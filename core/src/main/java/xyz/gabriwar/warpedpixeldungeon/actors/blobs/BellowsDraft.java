/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Draught;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DraftParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

/**
 * The wind of The Bellows: a draught that fills the room and blows one way, from the
 * door's wall to the far one. Each marked cell holds MARK + heading (0 east, 1 south,
 * 2 west, 3 north), so the direction is the cell's and needs nothing else to be saved.
 * (MARK keeps every cell well over the volume under which BlobEmitter thins a cell's
 * particles out: a draught marked 1-4 showed a fifth of its wind.)
 *
 * It does two things. Every turn it takes SHARE of each airborne cloud on its cells and
 * carries it one cell downwind - a gas let go at the door end arrives at the far end as
 * a front and piles against the wall there, and never comes back. And it bends anything
 * thrown through it (projectileFactor, read by ClimateManager.windProjectileFactor):
 * a fifth more likely to land and nearly a third heavier with the wind behind it, as
 * much worse into it.
 */
public class BellowsDraft extends Blob {

	private static final float SHARE = 0.75f;
	private static final int MARK = 10;

	//what the wind can carry: everything that hangs in the air. fire, frost, sparks and
	//water sit on the ground and stay where they are
	private static final Class<?>[] AIRBORNE = {
			ToxicGas.class, ConfusionGas.class, ParalyticGas.class, CorrosiveGas.class,
			CorruptGas.class, StenchGas.class, PoisonGas.class, OozeGas.class, GlowingGas.class,
			BallGas.class, PopGas.class, GrassSmokeGas.class, Smoke.class, SmokeScreen.class,
			Miasma.class, MistCloud.class, Steam.class, ChargedSteam.class, Dirtcloud.class,
			Slownesscloud.class, Withercloud.class, CloudOfCorruption.class,
			DigestingAcidCloud.class
	};

	//the heading of the last cell marked: what the particles follow
	private int heading = 0;

	private static final int[] DX = { 1, 0, -1, 0 };
	private static final int[] DY = { 0, 1, 0, -1 };

	/** marks a cell of the draught; heading is 0 east, 1 south, 2 west, 3 north */
	public void mark( xyz.gabriwar.warpedpixeldungeon.levels.Level level, int cell, int heading ){
		this.heading = heading;
		if (cur == null || cur[cell] == 0) seed( level, cell, MARK + heading );
	}

	@Override
	protected void evolve(){
		int w = Dungeon.level.width();
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * w;
				if (!Dungeon.level.insideMap( cell )) continue;
				off[cell] = cur[cell];
				volume += off[cell];
			}
		}

		//the hero feels it: an icon for as long as they stand in it, and a line the moment
		//they first do. The icon's description is where the room's rules are written down
		Hero hero = Dungeon.hero;
		if (hero != null && hero.isAlive() && hero.pos >= 0 && hero.pos < cur.length && cur[hero.pos] > 0){
			boolean entering = hero.buff( Draught.class ) == null;
			Draught felt = Buff.prolong( hero, Draught.class, Draught.DURATION );
			felt.heading = cur[hero.pos] - MARK;
			if (entering) GLog.i( Messages.get( this, "enter" ) );
		}

		for (Class<?> type : AIRBORNE){
			Blob gas = Dungeon.level.blobs.get( (Class<? extends Blob>) type );
			if (gas == null || gas.volume <= 0 || gas.cur == null) continue;
			carry( gas, w );
		}
	}

	//moves a share of the cloud one cell downwind on every cell of the draught. The
	//shares are all measured before any is moved, so nothing is carried twice in a turn
	private void carry( Blob gas, int w ){
		int[] moved = null;
		for (int y = area.top; y < area.bottom; y++){
			for (int x = area.left; x < area.right; x++){
				int cell = x + y * w;
				if (cur[cell] <= 0 || gas.cur[cell] <= 0) continue;
				int dir = cur[cell] - MARK;
				int to = cell + DX[dir] + DY[dir] * w;
				if (!Dungeon.level.insideMap( to ) || Dungeon.level.solid[to]) continue;
				int share = Math.round( gas.cur[cell] * SHARE );
				if (share <= 0) continue;
				if (moved == null) moved = new int[gas.cur.length];
				moved[cell] -= share;
				moved[to] += share;
			}
		}
		if (moved == null) return;
		for (int cell = 0; cell < moved.length; cell++){
			if (moved[cell] == 0) continue;
			gas.cur[cell] += moved[cell];
			if (moved[cell] > 0) gas.area.union( cell % w, cell / w );
		}
	}

	/** the heading of the draught on a cell, or -1 where the air is still */
	public static int headingAt( int cell ){
		if (Dungeon.level == null) return -1;
		BellowsDraft draft = (BellowsDraft) Dungeon.level.blobs.get( BellowsDraft.class );
		if (draft == null || draft.volume <= 0 || draft.cur == null
				|| cell < 0 || cell >= draft.cur.length || draft.cur[cell] <= 0) return -1;
		return draft.cur[cell] - MARK;
	}

	/** What the draught does to a throw from one cell to another: a multiplier for its
	 *  damage or its accuracy, or NaN when neither end is in a draught and the weather's
	 *  own wind should be asked instead. */
	public static float projectileFactor( int fromCell, int toCell, boolean forDamage ){
		int dir = headingAt( fromCell );
		if (dir == -1) dir = headingAt( toCell );
		if (dir == -1 || fromCell == toCell) return Float.NaN;

		int w = Dungeon.level.width();
		float dx = toCell % w - fromCell % w;
		float dy = toCell / w - fromCell / w;
		float len = (float) Math.sqrt( dx * dx + dy * dy );
		//+1 straight downwind, -1 straight into it, 0 across
		float alignment = (dx * DX[dir] + dy * DY[dir]) / len;
		return 1f + alignment * (forDamage ? 0.30f : 0.20f);
	}

	@Override
	public void use( BlobEmitter emitter ){
		super.use( emitter );
		emitter.pour( new Emitter.Factory(){
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				((DraftParticle) e.recycle( DraftParticle.class )).reset( x, y, DX[heading], DY[heading] );
			}
		}, 0.16f );
	}

	@Override
	public String tileDesc(){
		return Messages.get( this, "desc" );
	}

	private static final String HEADING = "heading";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HEADING, heading );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		heading = bundle.getInt( HEADING );
	}
}
