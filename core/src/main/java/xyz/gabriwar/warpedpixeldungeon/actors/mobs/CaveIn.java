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


package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A cave's roof coming down after a careless blow of the pick (levels/overworld/HazardWatch):
 * the marked cells take the rocks once the wait is out and the miner has had a turn to step
 * away in (slowed or chilled, his turn comes later than the wait), the dust pouring on them meanwhile and
 * bursting again halfway (bursts reach a co-op guest, the poured dust does not). It hurts more
 * deeper and cripples, a hero never losing more than a third of his health to it; where no one
 * stands, some cells are left heaped with rubble (MINE_BOULDER, which a pick clears and which
 * holds nothing).
 */
public class CaveIn extends DelayedRockFall implements Hero.Doom {

	//the slice it was set off on (Dungeon.depth) and how deep that is (0 at -1, 11 at -12)
	public int depth = -1, deep = 0;
	//the second half of the wait, still to come after the halfway burst
	public float rest = 0f;
	//the miner has yet to begin a turn since the marks: a slowed or chilled one (Char.spend) may
	//not get it within the wait, and the rocks hold off until he has (HazardWatch.heroTurnAt)
	public boolean waiting = true;

	//plain floor a fall may heap with rubble
	private static boolean floor( int t ){
		return t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMPTY_SP
				|| t == Terrain.MUSHROOM_PATCH || t == Terrain.GRASS;
	}

	/** The dust bursting on every marked cell: seen by everyone, co-op guests included. */
	public void dust(){
		if (!(com.watabou.noosa.Game.scene() instanceof GameScene)) return;
		for (int c : rockPositions()) CellEmitter.get( c ).burst( EarthParticle.FALLING, 4 );
	}

	@Override
	public boolean act(){
		//the miner left the slice: nothing falls on a level that is not there
		if (Dungeon.depth != depth || !(Dungeon.level instanceof OverworldLevel)){
			detach();
			return true;
		}
		if (rest > 0f){
			float r = rest;
			rest = 0f;
			dust();
			spend( r );
			return true;
		}
		if (waiting && target.isAlive()){
			float wait = target.cooldown() - cooldown();
			if (wait > 0f){
				//the marks stay up until they fall
				if (com.watabou.noosa.Game.scene() instanceof GameScene){
					for (int c : rockPositions()) GameScene.targetedCell( c, wait );
				}
				spend( wait );
				return true;
			}
		}
		return super.act();
	}

	@Override
	public void affectChar( Char ch ){
		int dmg = Random.NormalIntRange( 4 + deep, 8 + 2 * deep ) - ch.drRoll();
		if (ch instanceof Hero) dmg = Math.min( dmg, Math.max( 1, ch.HT / 3 ) );
		if (dmg > 0) ch.damage( dmg, this );
		if (ch.isAlive()) Buff.prolong( ch, Cripple.class, 3f );
	}

	@Override
	public void affectCell( int cell ){
		Level level = Dungeon.level;
		if (Random.Int( 3 ) != 0 || !floor( level.map[cell] ) || level.heaps.get( cell ) != null) return;
		Level.set( cell, Terrain.MINE_BOULDER );
		GameScene.updateMap( cell );
	}

	@Override
	public void onDeath(){
		Dungeon.fail( CaveIn.class );
		GLog.n( Messages.get( CaveIn.class, "ondeath" ) );
	}

	private static final String DEPTH = "depth", DEEP = "deep", REST = "rest", WAITING = "waiting";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( DEPTH, depth );
		bundle.put( DEEP, deep );
		bundle.put( REST, rest );
		bundle.put( WAITING, waiting );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		depth = bundle.contains( DEPTH ) ? bundle.getInt( DEPTH ) : -1;
		deep = bundle.contains( DEEP ) ? bundle.getInt( DEEP ) : 0;
		rest = bundle.contains( REST ) ? bundle.getFloat( REST ) : 0f;
		waiting = bundle.contains( WAITING ) && bundle.getBoolean( WAITING );
	}
}
