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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The world's barrows (Delves, WorldStructures.Site.DUNGEON): where they stand, how high they
 * are, how their floors scale and what they never hold.
 */
public class DelvesTest {

	private static final long SEED = 0xBA77011L;

	//items, mobs and the hero need the assets and the message bundles: the item test's harness
	@BeforeClass
	public static void assets(){
		xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
	}

	private int depth, branch;
	private Hero hero;

	@Before
	public void keep(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		hero = Dungeon.hero;
		Delves.reset();
	}

	@After
	public void restore(){
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.hero = hero;
		Delves.reset();
	}

	//barrows are found over the world, on dry land, away from the town, their stairway in a
	//dressed stone square open to the south
	@Test
	public void barrowsStandOnDryLandWithAStairway(){
		int found = 0;
		for (int sy = -20; sy <= 20; sy++){
			for (int sx = -20; sx <= 20; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.DUNGEON) continue;
				found++;
				int wx = WorldStructures.siteX( SEED, sx, sy ), wy = WorldStructures.siteY( SEED, sx, sy );
				assertFalse( "near the town", Math.abs( wx ) < WorldModel.TOWN_IN + 8 && Math.abs( wy ) < WorldModel.TOWN_IN + 8 );
				WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, wx, wy );
				assertTrue( "on " + b, b != WorldModel.Biome.OCEAN && b != WorldModel.Biome.RIVER
						&& b != WorldModel.Biome.BEACH && b != WorldModel.Biome.SWAMP && b != WorldModel.Biome.MOUNTAIN );
				assertEquals( Terrain.EXIT, WorldStructures.terrainAt( SEED, wx, wy, Terrain.EMPTY ) );
				assertTrue( WorldStructures.delveGate( SEED, wx, wy ) );
				assertFalse( WorldStructures.delveGate( SEED, wx + 1, wy ) );
				assertEquals( Terrain.EMPTY_SP, WorldStructures.terrainAt( SEED, wx, wy + 3, Terrain.EMPTY ) );
				assertEquals( Terrain.WALL_DECO, WorldStructures.terrainAt( SEED, wx + 3, wy, Terrain.EMPTY ) );
			}
		}
		//one sector in six of the unclaimed land: dozens over 41 x 41 sectors
		assertTrue( "found " + found, found >= 20 && found <= 400 );
	}

	//a barrow's level never drops below 1, is the same every time it is asked, and runs higher
	//the farther from the town
	@Test
	public void levelsRiseWithDistance(){
		long near = 0, far = 0;
		int nNear = 0, nFar = 0;
		for (int sy = -60; sy <= 60; sy++){
			for (int sx = -60; sx <= 60; sx++){
				int l = Delves.level( SEED, sx, sy );
				assertTrue( l >= 1 );
				assertEquals( l, Delves.level( SEED, sx, sy ) );
				int d = Math.max( Math.abs( sx ), Math.abs( sy ) );
				if (d <= 5){ near += l; nNear++; }
				if (d >= 50){ far += l; nFar++; }
				int r = Delves.region( SEED, sx, sy );
				assertTrue( r >= 0 && r <= 4 );
				if (l <= 25) assertEquals( Math.min( 4, (l - 1) / 5 ), r );
			}
		}
		assertTrue( far / (float)nFar > 3 * near / (float)nNear );
	}

	//at or under the depth a mob is native to nothing changes; past it the factors only grow,
	//and past the dungeon's bottom they keep growing with no ceiling
	@Test
	public void scalingOnlyGrowsAndNeverStops(){
		assertEquals( 1f, Delves.powerAt( 5, 5, 10f ), 0f );
		assertEquals( 1f, Delves.skillAt( 3, 5, 10f ), 0f );
		float lastP = 0, lastS = 0;
		for (int e = 1; e <= 400; e++){
			float p = Delves.powerAt( e, 4, 10f ), s = Delves.skillAt( e, 4, 10f );
			assertTrue( "power at " + e, p >= lastP );
			assertTrue( "skill at " + e, s >= lastS );
			assertTrue( "skill outpacing power at " + e, s <= p );
			lastP = p;
			lastS = s;
		}
		assertTrue( Delves.powerAt( 400, 4, 1f ) > 200 );
		//a weak mob is lifted to the bottom's strength first: a rat past 26 outscales an eye
		assertTrue( Delves.powerAt( 30, 4, 10f ) > Delves.powerAt( 30, 24, 1.1f ) );
	}

	//the first barrow opened takes the first branch, the next the next; reopening one finds it,
	//and the registry survives a save
	@Test
	public void barrowsGetTheirOwnBranchesAndAreSaved(){
		Delves.Entry a = Delves.open( SEED, 3, 4 );
		Delves.Entry b = Delves.open( SEED, -7, 2 );
		assertEquals( Delves.BRANCH_BASE, a.branch );
		assertEquals( Delves.BRANCH_BASE + 1, b.branch );
		assertSame( a, Delves.open( SEED, 3, 4 ) );
		assertEquals( Delves.level( SEED, 3, 4 ), a.level );
		assertEquals( Delves.floors( SEED, 3, 4 ), a.floors );
		assertTrue( a.lastDepth() <= 24 );
		b.cleared = true;

		Bundle bundle = new Bundle();
		Delves.storeInBundle( bundle );
		Delves.reset();
		assertFalse( Delves.cleared( -7, 2 ) );
		Delves.restoreFromBundle( bundle );
		assertTrue( Delves.cleared( -7, 2 ) );
		assertEquals( b.branch, Delves.bySector( -7, 2 ).branch );
		assertEquals( a.region, Delves.byBranch( a.branch ).region );
		//a third barrow never reuses a branch
		assertEquals( Delves.BRANCH_BASE + 2, Delves.open( SEED, 9, 9 ).branch );
	}

	//nothing that grows the hero for good is let into a barrow; elsewhere nothing is touched
	@Test
	public void noPermanentUpgradesInABarrow(){
		Delves.Entry e = Delves.open( SEED, 3, 4 );
		Dungeon.branch = 0;
		assertFalse( Delves.banned( new PotionOfStrength() ) );
		Dungeon.branch = e.branch;
		Dungeon.depth = e.firstDepth();
		assertTrue( Delves.banned( new PotionOfStrength() ) );
		assertTrue( Delves.banned( new ScrollOfUpgrade() ) );
		assertFalse( Delves.banned( new PotionOfHealing() ) );
		assertTrue( Delves.substitute( new ScrollOfUpgrade() ).quantity() > 0 );
	}

	//in a barrow an enemy's stats carry its scaling through the one modifier every stat reads;
	//the hero is never scaled
	@Test
	public void enemiesInABarrowScaleThroughTheStatModifier(){
		Delves.Entry opened = Delves.open( SEED, 3, 4 );
		Dungeon.hero = new Hero();
		Dungeon.branch = opened.branch;
		opened.level = 60;
		opened.region = 0;
		Dungeon.depth = opened.lastDepth();
		Rat rat = new Rat();
		float p = AscensionChallenge.statModifier( rat );
		assertEquals( Delves.powerAt( 60 + opened.floors - 1, Dungeon.depth, 10f ), p, 0.001f );
		assertTrue( p > 10 );
		assertTrue( AscensionChallenge.skillModifier( rat ) < p );
		assertEquals( 1f, AscensionChallenge.statModifier( Dungeon.hero ), 0f );

		Dungeon.branch = 0;
		assertEquals( 1f, AscensionChallenge.statModifier( rat ), 0f );
	}

	//leaving a barrow lands on its own stairway: the world's sites are laid by the world seed,
	//not the run's, and the way out has to find the very cell the way in stands on
	@Test
	public void theWayOutIsTheBarrowsOwnStairway(){
		long run = Dungeon.seed;
		Dungeon.seed = 0x1234ABCDL;
		long world = Delves.worldSeed();
		int found = 0;
		for (int sy = -12; sy <= 12 && found < 5; sy++){
			for (int sx = -12; sx <= 12 && found < 5; sx++){
				if (WorldStructures.siteType( world, sx, sy ) != WorldStructures.Site.DUNGEON) continue;
				Delves.Entry e = Delves.open( world, sx, sy );
				int[] stairs = Delves.stairs( e );
				assertTrue( WorldStructures.delveGate( world, stairs[0], stairs[1] ) );
				found++;
			}
		}
		assertTrue( found > 0 );
		Dungeon.seed = run;
	}

	//a barrow runs 4 to 15 floors on the dungeon's regular depths, never a boss depth nor past
	//24, in order, starting at its region (or higher, when it would run past the halls); its
	//level is spread wide round its distance's base
	@Test
	public void barrowsRunFourToFifteenRegularFloors(){
		boolean[] seenLength = new boolean[Delves.MAX_FLOORS + 1];
		int easy = 0, hard = 0;
		for (int sy = -40; sy <= 40; sy++){
			for (int sx = -40; sx <= 40; sx++){
				Delves.Entry e = new Delves.Entry();
				e.region = Delves.region( SEED, sx, sy );
				e.floors = Delves.floors( SEED, sx, sy );
				e.level = Delves.level( SEED, sx, sy );
				assertTrue( e.floors >= Delves.MIN_FLOORS && e.floors <= Delves.MAX_FLOORS );
				seenLength[e.floors] = true;
				int last = 0;
				for (int f = 0; f < e.floors; f++){
					int d = e.depthOf( f );
					assertTrue( d > last && d <= 24 && d % 5 != 0 );
					assertEquals( f, e.floorOf( d ) );
					last = d;
				}
				assertEquals( -1, e.floorOf( 5 ) );
				if (e.region * 4 + e.floors <= Delves.DEPTHS.length) assertEquals( e.region * 5 + 1, e.firstDepth() );
				if (Math.abs( sx ) >= 30 || Math.abs( sy ) >= 30){
					if (e.level < 15) easy++;
					if (e.level > 60) hard++;
				}
			}
		}
		for (int n = Delves.MIN_FLOORS; n <= Delves.MAX_FLOORS; n++) assertTrue( "no barrow of " + n + " floors", seenLength[n] );
		//far out, both gentle barrows and brutal ones are common
		assertTrue( "easy " + easy, easy > 100 );
		assertTrue( "hard " + hard, hard > 100 );
	}
}
