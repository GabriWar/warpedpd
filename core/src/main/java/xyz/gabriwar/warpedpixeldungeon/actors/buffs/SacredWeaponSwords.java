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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * Sacred Weapon: spectral copies of the cleric's weapon planted in the floor. Each one
 * cuts an enemy next to it every turn until it fades; at mastery a fading sword flies
 * at the nearest enemy in reach.
 *
 * Its look is blades of holy light, never the weapon lying on the floor or thrown: each falls as
 * a blade of light (SkillFX.Fall.HOLY_BLADE, the item's draws and time kept), stands in its cell
 * hovering over a pool of gold (Blades), cuts with a stroke and a crescent of gold, and at
 * mastery flies as a blade of light (SkillFX.Streak.HOLY_BLADE).
 */
public class SacredWeaponSwords extends Buff {

	/** turns a planted sword stands */
	public static final int DURATION = 3;
	/** share of a weapon roll each cut deals */
	public static final float CUT = 0.35f;
	/** share of a weapon roll the fading throw deals, and how far it flies */
	public static final float THROW = 0.5f;
	public static final int THROW_RANGE = 4;

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int[] cells = new int[0];
	private int[] left = new int[0];
	private int rank = 1;
	private int depth = -1, branch = -1;

	private Blades visual;
	//the cells planted by the last plant, whose blades wait for their fall to land
	private int[] fresh = new int[0];

	private boolean sameFloor(){
		return depth == Dungeon.depth && branch == Dungeon.branch;
	}

	/** drops a sword beside the struck enemy; false when the cap is reached or there is no room */
	public boolean plant( Char struck, int rank ){
		if (!sameFloor()){
			cells = new int[0];
			left = new int[0];
			depth = Dungeon.depth;
			branch = Dungeon.branch;
		}
		this.rank = rank;
		if (cells.length >= rank) return false;

		//the sword lands on the far side of the enemy when it can, flanking it
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = struck.pos + n;
			if (!SkillInteractions.valid( c ) || c == target.pos || !Dungeon.level.passable[c]
					|| Dungeon.level.pit[c] || indexOf( c ) >= 0) continue;
			if (best == -1 || Dungeon.level.trueDistance( target.pos, c ) > Dungeon.level.trueDistance( target.pos, best )) best = c;
		}
		if (best == -1) return false;

		int[] nc = java.util.Arrays.copyOf( cells, cells.length + 1 );
		int[] nl = java.util.Arrays.copyOf( left, left.length + 1 );
		nc[cells.length] = best;
		nl[left.length] = DURATION;
		cells = nc;
		left = nl;

		final int cell = best;
		KindOfWeapon look = target instanceof Hero ? ((Hero) target).belongings.weapon() : null;
		//a blade of light falls where the weapon fell (its draws, its early return with no weapon, its time)
		SkillFX.rain( cell, look, SkillFX.Fall.HOLY_BLADE, 1, () -> {
			CellEmitter.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
			Blades b = visual;
			if (b != null) b.landed( cell );
		} );
		SpatialSound.play( Assets.Sounds.HIT_MAGIC, cell, 0.9f, 0.9f );
		fresh = new int[]{ cell };
		fx( true );
		fresh = new int[0];
		//with no weapon nothing falls: the blade opens in its cell at once, never popping in
		if (look == null && visual != null) visual.landed( cell );
		return true;
	}

	private int indexOf( int cell ){
		for (int i = 0; i < cells.length; i++) if (cells[i] == cell) return i;
		return -1;
	}

	private static int roll( Hero hero ){
		KindOfWeapon w = hero.belongings.weapon();
		return w == null ? RingOfForce.damageRoll( hero ) : w.damageRoll( hero );
	}

	private static boolean awakeEnemy( Char ch ){
		return ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
				&& ((Mob) ch).state != ((Mob) ch).SLEEPING;
	}

	@Override
	public boolean act(){
		if (!(target instanceof Hero) || !sameFloor() || cells.length == 0){
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		int kept = 0;
		int[] nc = new int[cells.length];
		int[] nl = new int[cells.length];
		for (int i = 0; i < cells.length; i++){
			int c = cells[i];
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( c + n );
				if (!awakeEnemy( ch )) continue;
				SkillSpectacleFX.fly( SkillSpectacleFX.SABER, c, ch.pos, 0, .25f, Blades.GOLD );
				ch.damage( Math.max( 1, Math.round( roll( hero ) * CUT ) ), this );
				SkillFX.flash( ch );
				SpatialSound.play( Assets.Sounds.HIT_SLASH, ch, 0.8f, 1.2f );
				break;
			}
			if (--left[i] > 0){
				nc[kept] = c;
				nl[kept++] = left[i];
			} else {
				if (rank >= Skill.MAX_LEVEL) hurl( hero, c );
				if (Dungeon.level.heroFOV[c]) CellEmitter.center( c ).burst( Speck.factory( Speck.LIGHT ), 4 );
			}
		}
		boolean changed = kept != cells.length;
		cells = java.util.Arrays.copyOf( nc, kept );
		left = java.util.Arrays.copyOf( nl, kept );
		if (cells.length == 0){
			detach();
			return true;
		}
		if (changed) fx( true );
		spend( TICK );
		return true;
	}

	//at mastery the fading sword pulls itself out of the floor and flies at the nearest enemy
	private void hurl( Hero hero, int from ){
		Mob nearest = null;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!awakeEnemy( m ) || Dungeon.level.distance( from, m.pos ) > THROW_RANGE
					|| !SkillInteractions.clear( from, m.pos )) continue;
			if (nearest == null || Dungeon.level.trueDistance( from, m.pos ) < Dungeon.level.trueDistance( from, nearest.pos )) nearest = m;
		}
		if (nearest == null) return;
		final Mob struck = nearest;
		//the blade flies as light, as fast as the weapon would have (a guest is sent its flight)
		KindOfWeapon look = hero.belongings.weapon();
		SkillFX.streak( from, struck.pos, SkillFX.Streak.HOLY_BLADE, look, () -> SkillFX.flash( struck ) );
		SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 1.2f );
		struck.damage( Math.max( 1, Math.round( roll( hero ) * THROW ) ), this );
	}

	@Override
	public void fx( boolean on ){
		//blades of cells that went fade out (every one when the swords go)
		if (visual != null){
			visual.fadeExcept( on && sameFloor() ? cells : new int[0] );
			visual = null;
		}
		if (!on || target.sprite == null || target.sprite.parent == null || cells.length == 0 || !sameFloor()) return;
		visual = Blades.show( cells, fresh );
	}

	/**
	 * The planted swords as blades of holy light: per cell a LANCE_HEAD_9 turned point down into
	 * the floor, a CORE_BAND_1 guard across its socket and a HILT px CORE_BAND_1 grip above, lit
	 * GOLD in the light layer, hovering ALPHA +- BREATH and bobbing 1 px at BOB_HZ in a GLOW_11
	 * halo (GLOW_LEVEL) over a POOL_15 of gold (POOL_LEVEL). A blade still falling (SkillFX.Fall.HOLY_BLADE) stands up as it lands
	 * (or after WAIT s), opening over OPEN s; one that goes fades over FADE s. 3 quads, a glow and
	 * a pool a blade (at most 3 blades: 6 persistent lights); nothing allocated a frame; let go of with Fx.dispose. Made
	 * anywhere (asset keys), shown on the render thread.
	 */
	public static class Blades extends Group {

		public static final int GOLD = 0xF1D99C, CORE = 0xFFF4D0;
		public static final int HILT = 10, GUARD = 5, TIP = 5;
		public static final float ALPHA = 0.75f, BREATH = 0.1f, BOB_HZ = 0.8f, POOL_LEVEL = 0.3f, GLOW_LEVEL = 0.3f;
		public static final float OPEN = 0.12f, WAIT = 0.6f, FADE = 0.3f;

		final int[] cells;
		final FxPic[] heads, guards, hilts;
		final FxLight[] pools, glows;
		//per blade: its cell's middle in the world, where its head was last put (a slide of the
		//window moves it: the middle moves with it), seconds since it opened (-1: there already;
		//-2: still falling), the time it has waited for its fall, its fade left (-1: none)
		final float[] mx, my, hx, hy, open, waited, fade;
		private volatile boolean fading;
		private boolean ending;

		Blades( int[] cells, int[] fresh ){
			this.cells = cells.clone();
			int n = cells.length;
			heads = new FxPic[n];
			guards = new FxPic[n];
			hilts = new FxPic[n];
			pools = new FxLight[n];
			glows = new FxLight[n];
			mx = new float[n];
			my = new float[n];
			hx = new float[n];
			hy = new float[n];
			open = new float[n];
			waited = new float[n];
			fade = new float[n];
			for (int i = 0; i < n; i++){
				mx[i] = SkillVfx.cx( cells[i] );
				my[i] = SkillVfx.cy( cells[i] );
				//the blade: the lance head turned point down (+90: its +x down), its socket (x0) up
				//top; pivoting on its middle row's top edge, so the turned rows land on whole columns
				heads[i] = pic( FxFrames.LANCE_HEAD_9, SkillVfx.SHEET, 0, 2, 90, GOLD );
				//the guard across its socket, and the grip above it turned the same way down
				guards[i] = pic( FxFrames.Light.CORE_BAND_1, SkillVfx.LIGHT_SHEET, 0, 0, 0, CORE );
				guards[i].scale.set( GUARD / 16f, 1 );
				hilts[i] = pic( FxFrames.Light.CORE_BAND_1, SkillVfx.LIGHT_SHEET, 0, 0, 90, GOLD );
				hilts[i].scale.set( HILT / 16f, 1 );
				hx[i] = heads[i].x;
				hy[i] = heads[i].y;
				open[i] = -1;
				for (int f : fresh) if (f == cells[i]) open[i] = -2;
				fade[i] = -1;
			}
		}

		private FxPic pic( int[] frame, String sheet, float ox, float oy, float angle, int color ){
			FxPic p = new FxPic( sheet );
			p.show( frame );
			p.additive = true;
			p.lit( color );
			p.origin.set( ox, oy );
			p.angle = angle;
			p.visible = false;
			add( p );
			return p;
		}

		/** The blades for these cells into the light layer (posted from any thread). */
		public static Blades show( int[] cells, int[] fresh ){
			final Blades b = new Blades( cells, fresh );
			Fx.post( () -> {
				if (b.fading || GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
				GameScene.light( b );
				for (int i = 0; i < b.cells.length; i++){
					b.pools[i] = FxLight.pool( b.mx[i], b.my[i] + TIP, 15, GOLD, 0 ).persistent();
					b.glows[i] = FxLight.glow( b.mx[i], b.my[i] + TIP - 8, 11, GOLD, 0 ).persistent();
				}
			} );
			return b;
		}

		/** The fall onto this cell landed: its blade stands up (render thread, the fall's callback). */
		public void landed( final int cell ){
			Fx.post( () -> {
				for (int i = 0; i < cells.length; i++) if (cells[i] == cell && open[i] == -2) open[i] = 0;
			} );
		}

		/** Gives way to the next blades: those of cells it does not keep fade out, the rest go at
		 *  once, and then it is let go of (any thread). */
		public void fadeExcept( final int[] keep ){
			fading = true;
			Fx.post( () -> {
				ending = true;
				boolean any = false;
				for (int i = 0; i < cells.length; i++){
					boolean kept = false;
					for (int k : keep) if (k == cells[i]) kept = true;
					if (kept || parent == null || open[i] == -2) hide( i );
					else { fade[i] = FADE; any = true; }
				}
				if (!any) Fx.dispose( this );
			} );
		}

		private void hide( int i ){
			heads[i].visible = guards[i].visible = hilts[i].visible = false;
			if (pools[i] != null){
				pools[i].putOut( 0.1f );
				pools[i] = null;
			}
			if (glows[i] != null){
				glows[i].putOut( 0.1f );
				glows[i] = null;
			}
		}

		@Override
		public void update(){
			super.update();
			//time held still (the hourglass, swiftthistle): the blades' fall, opening and fading hold,
			//but they keep standing in their cells (a cast made then must not hide the ones kept)
			float dt = Emitter.freezeEmitters && Game.timeTotal > 1 ? 0 : Game.elapsed;
			//the scene's clock: blades made again when one goes keep their bob and breath
			float time = Game.timeTotal;
			boolean any = false;
			for (int i = 0; i < cells.length; i++){
				if (ending && fade[i] < 0) continue;
				//a slide of the window moves every picture: the middle moves with it
				FxPic h = heads[i];
				mx[i] += h.x - hx[i];
				my[i] += h.y - hy[i];
				float a = ALPHA + BREATH * (float) Math.sin( time * 2.1f + i * 1.7f );
				if (open[i] == -2){
					waited[i] += dt;
					if (waited[i] >= WAIT) open[i] = 0;
					else a = 0;
				}
				if (open[i] >= 0){
					open[i] += dt;
					a *= Math.min( 1f, open[i] / OPEN );
					if (open[i] >= OPEN) open[i] = -1;
				}
				if (fade[i] >= 0){
					fade[i] -= dt;
					a *= Math.max( 0f, fade[i] / FADE );
					if (fade[i] <= 0){ hide( i ); continue; }
					any = true;
				}
				boolean seen = a > 0.01f && SkillVfx.seen( Fx.cellAt( mx[i], my[i] ) );
				//bobbing a whole pixel up and down; its tip TIP px under the cell's middle, its socket 8
				//above the tip, the guard the row over the socket, the grip HILT px over that
				int bob = Math.sin( time * 2 * Math.PI * BOB_HZ + cells[i] ) > 0 ? -1 : 0;
				int col = Math.round( mx[i] ), socket = Math.round( my[i] ) + TIP - 8 + bob;
				h.x = col + 1;
				h.y = socket - 2;
				FxPic g = guards[i];
				g.x = col - GUARD / 2;
				g.y = socket - 1;
				FxPic l = hilts[i];
				l.x = col + 1;
				l.y = socket - 1 - HILT;
				hx[i] = h.x;
				hy[i] = h.y;
				h.am = a;
				g.am = a * 0.9f;
				l.am = a * 0.8f;
				h.visible = g.visible = l.visible = seen;
				if (pools[i] != null) pools[i].level( POOL_LEVEL * Math.min( 1f, a / ALPHA ) );
				if (glows[i] != null) glows[i].level( GLOW_LEVEL * Math.min( 1f, a / ALPHA ) );
			}
			if (ending && !any) Fx.dispose( this );
		}

		@Override
		public void destroy(){
			super.destroy();
			for (int i = 0; i < pools.length; i++) hide( i );
		}
	}

	private static final String CELLS = "cells", LEFT = "left", RANK = "rank", DEPTH = "depth", BRANCH = "branch";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CELLS, cells );
		bundle.put( LEFT, left );
		bundle.put( RANK, rank );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		cells = bundle.getIntArray( CELLS );
		left = bundle.getIntArray( LEFT );
		rank = bundle.getInt( RANK );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
		if (cells == null || left == null || cells.length != left.length){
			cells = new int[0];
			left = new int[0];
		}
	}
}
