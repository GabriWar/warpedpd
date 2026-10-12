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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A homestead's spike trap: a boarded frame of iron spikes laid on the open ground (the build
 * menu's spike trap, DefencePieces), in plain sight. It is a real trap of the level's traps, drawn
 * over the ground it lies on (TerrainFeaturesTilemap, cell ARMED of terrain_features.png), but it
 * lies on walkable ground - an ordinary trap's cell is one monsters walk round - and Level.pressCell
 * springs it for whoever steps on it that it bites: a monster (ENEMY-aligned, on its feet), never
 * the hero, his allies or a neutral beast.
 *
 * It impales: NormalIntRange( 4 + d / 2, 8 + d ), less half the victim's armour, where d is the
 * deepest floor the hero has reached, and cripples it for a while. Then it lies sprung (drawn
 * darker, cell SPRUNG) and arms itself again REARM turns later. The world-keyed times it was sprung
 * are the slice's (ThreatSlice), so a trap carried through a window's move or a save keeps its wait.
 */
public class SpikeTrap extends Trap {

	/** Its pictures, cells of environment/terrain_features.png (tools/spike_trap_tile_gen.py):
	 *  a trap's cell there is color + shape * 16, the shape 0. */
	public static final int ARMED = 9, SPRUNG = 10;
	/** How long it lies sprung, in turns. */
	public static final float REARM = 20f;

	{
		color = ARMED;
		shape = 0;
		visible = true;
		canBeHidden = false;
		canBeSearched = false;
		disarmedByActivation = false;
	}

	/** When it was last sprung, on the world's clock (FarmCrop.clock); long ago when never. */
	public float sprungAt = -REARM;

	/** Is it armed now? */
	public boolean armed(){
		return FarmCrop.clock() - sprungAt >= REARM;
	}

	/** Does it bite whoever this is: a monster on its feet, never the hero, an ally or a neutral beast. */
	public static boolean springsFor( Char ch ){
		return ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && !ch.flying && ch.isAlive();
	}

	@Override
	public void trigger(){
		if (armed() && springsFor( Actor.findChar( pos ) )) super.trigger();
	}

	@Override
	public void activate(){
		Char ch = Actor.findChar( pos );
		sprungAt = FarmCrop.clock();
		if (Dungeon.level instanceof OverworldLevel){
			ThreatSlice slice = ThreatSlice.of( (OverworldLevel) Dungeon.level );
			if (slice != null) slice.sprung( (OverworldLevel) Dungeon.level, pos, sprungAt );
		}
		show( SPRUNG );
		if (!springsFor( ch )) return;
		Buff.prolong( ch, Trap.HazardAssistTracker.class, HazardAssistTracker.DURATION );
		int d = Math.max( 1, Statistics.deepestFloor );
		int damage = Math.max( 1, Random.NormalIntRange( 4 + d / 2, 8 + d ) - ch.drRoll() / 2 );
		ch.damage( damage, this );
		if (ch.isAlive()) Buff.prolong( ch, Cripple.class, Cripple.DURATION );
		if (ch.sprite != null) Wound.hit( ch );
	}

	/** Armed again (ThreatSlice, once its REARM turns are up). */
	void rearm(){
		show( ARMED );
	}

	//its picture: armed or sprung
	private void show( int look ){
		if (color == look) return;
		color = look;
		GameScene.updateMap( pos );
	}

	private static final String SPRUNG_AT = "sprung_at";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SPRUNG_AT, sprungAt );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		if (bundle.contains( SPRUNG_AT )) sprungAt = bundle.getFloat( SPRUNG_AT );
		color = armed() ? ARMED : SPRUNG;
	}
}
