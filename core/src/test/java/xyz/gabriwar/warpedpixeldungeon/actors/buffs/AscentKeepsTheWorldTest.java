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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The amulet's ascent in a run that goes on past it: the troll blacksmith, a storage chest and
 * the rest of a floor's folk outlive the climb, and the curse bites on the dungeon's floors only.
 */
public class AscentKeepsTheWorldTest {

	private long seed;
	private int depth, branch, highest;
	private Level level;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		seed = Dungeon.seed; depth = Dungeon.depth; branch = Dungeon.branch;
		highest = Statistics.highestAscent; level = Dungeon.level;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 0xA5CE7L;
		Dungeon.init();
	}

	@After
	public void tearDown(){
		Dungeon.seed = seed; Dungeon.depth = depth; Dungeon.branch = branch;
		Statistics.highestAscent = highest; Dungeon.level = level;
		Actor.clear();
	}

	private AscensionChallenge curse( float stacks ){
		AscensionChallenge c = Buff.affect( Dungeon.hero, AscensionChallenge.class );
		Bundle b = new Bundle();
		c.storeInBundle( b );
		b.put( AscensionChallenge.STACKS, stacks );
		c.restoreFromBundle( b );
		return c;
	}

	private Level floor( int d ){
		Dungeon.depth = d; Dungeon.branch = 0;
		Actor.clear();
		Dungeon.level = Dungeon.newLevel();
		return Dungeon.level;
	}

	private int freeCell( Level l ){
		for (int i = 0; i < l.length(); i++) if (l.passable[i] && Actor.findChar( i ) == null && l.findMob( i ) == null) return i;
		throw new IllegalStateException();
	}

	@Test
	public void theSmithAndTheChestOutliveTheClimb(){
		Level l = floor( 13 );
		Blacksmith smith = new Blacksmith(); smith.pos = freeCell( l ); l.mobs.add( smith );
		StorageChest chest = new StorageChest(); chest.pos = freeCell( l ); l.mobs.add( chest );
		Rat rat = new Rat(); rat.pos = freeCell( l ); l.mobs.add( rat );
		Statistics.highestAscent = 14;
		AscensionChallenge c = curse( 0 );

		c.onLevelSwitch();

		assertEquals( "a step of the ascent", 13, Statistics.highestAscent );
		assertTrue( "the troll blacksmith is still there", l.mobs.contains( smith ) );
		assertTrue( "the storage chest is still there", l.mobs.contains( chest ) );
		assertFalse( "the floor's monsters are reset, as before", l.mobs.contains( rat ) );
	}

	@Test
	public void theCurseWaitsOffTheClimb(){
		Level l = floor( 97 );
		AscensionChallenge c = curse( 10 );
		int hp = Dungeon.hero.HP;
		for (int i = 0; i < 20; i++) c.act();
		assertEquals( "no harm on the surface", hp, Dungeon.hero.HP );
		assertFalse( AscensionChallenge.climbing() );
		Rat rat = new Rat();
		assertEquals( "monsters out here are not boosted", 1f, AscensionChallenge.statModifier( rat ), 0.001f );
		assertEquals( "nor is the world's scaling", 97 == Dungeon.depth ? Dungeon.depth : -1, Dungeon.scalingDepth() );
	}

	@Test
	public void theCurseStillBitesOnTheClimb(){
		floor( 8 ).mobs.clear();   //no monsters to call (they have no sprites to show it here)
		curse( 10 );
		assertTrue( AscensionChallenge.climbing() );
		assertEquals( "monsters on the climb are boosted", 10f, AscensionChallenge.statModifier( new Rat() ), 0.001f );
		assertEquals( 26, Dungeon.scalingDepth() );
		//its bite, on a char the headless test can hurt (the hero's hurt reaches for the screen)
		Rat bitten = new Rat();
		bitten.HP = bitten.HT = 1000;
		AscensionChallenge c = new AscensionChallenge();
		c.attachTo( bitten );
		Bundle b = new Bundle(); c.storeInBundle( b ); b.put( AscensionChallenge.STACKS, 10f ); c.restoreFromBundle( b );
		for (int i = 0; i < 20; i++) c.act();
		assertTrue( "the climb still hurts at 8+ stacks", bitten.HP < 1000 );
	}
}
