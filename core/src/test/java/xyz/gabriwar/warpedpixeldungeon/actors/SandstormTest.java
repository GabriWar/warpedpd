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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sandblown;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.items.misc.DesertScarf;
import xyz.gabriwar.warpedpixeldungeon.items.misc.SandGoggles;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The desert's sandstorm (actors/Sandstorm): where it blows, how hard, what keeps it off, what it
 * does to the exposed, and the wear that keeps it off being made at a workbench.
 */
public class SandstormTest {

	private static final long SEED = 3L;

	@BeforeClass
	public static void assets(){
		//the items' and the heroes' harness, a game with a version (the net layer reads it)
		xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest.boot();
	}

	private Hero hero;
	private Level level;

	@Before
	public void keep(){
		hero = Dungeon.hero;
		level = Dungeon.level;
	}

	@After
	public void restore(){
		Dungeon.hero = hero;
		Dungeon.level = level;
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugWindDirOverride = Float.NaN;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugPrecipTypeOverride = null;
	}

	//a 64x64 slice of the world round a cell, its ground plain and open
	private static OverworldLevel slice( int altitude, int wx, int wy ){
		OverworldLevel ow = new OverworldLevel( altitude );
		ow.setSize( 64, 64 );
		ow.mobs = new HashSet<>();
		ow.worldSeed = SEED;
		ow.worldX = wx - 32;
		ow.worldY = wy - 32;
		Arrays.fill( ow.map, Terrain.EMPTY_SP );
		Arrays.fill( ow.passable, true );
		return ow;
	}

	//the first world cell (on a coarse grid out from the origin) whose biome is or is not the desert
	private static int[] cell( boolean desert ){
		for (int r = 0; r < 200; r++){
			for (int i = -r; i <= r; i++){
				for (int j = -r; j <= r; j++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int x = i * 32, y = j * 32;
					if ((WorldModel.biomeAt( SEED, x, y ) == WorldModel.Biome.DESERT) == desert) return new int[]{ x, y };
				}
			}
		}
		throw new AssertionError( "no " + (desert ? "desert" : "other ground") );
	}

	private static final int MIDDLE = 32 + 32 * 64;

	//its strength: none under 10 m/s, its floor at 10, all of it from 20, growing in between
	@Test
	public void itBlowsHarderTheHarderTheWind(){
		assertEquals( 0f, Sandstorm.strength( 9.9f ), 0f );
		assertEquals( Sandstorm.FLOOR, Sandstorm.strength( 10f ), 1e-6f );
		assertEquals( 1f, Sandstorm.strength( 20f ), 1e-6f );
		assertEquals( 1f, Sandstorm.strength( 30f ), 1e-6f );
		float last = 0f;
		for (float w = 10f; w <= 20f; w += 0.5f){
			assertTrue( Sandstorm.strength( w ) >= last );
			last = Sandstorm.strength( w );
		}
		//out of it nothing changes; in it the sight, the speed and the eyes, more the harder it blows
		assertEquals( 1f, Sandstorm.sightFactor( 0f ), 0f );
		assertEquals( 1f, Sandstorm.speedFactor( 0f ), 0f );
		assertEquals( 0f, Sandstorm.blindChance( 0f ), 0f );
		assertEquals( Sandstorm.SIGHT_START, Sandstorm.sightFactor( Sandstorm.FLOOR ), 1e-6f );
		assertEquals( Sandstorm.SIGHT_LEAST, Sandstorm.sightFactor( 1f ), 1e-6f );
		assertEquals( Sandstorm.SLOW_LEAST, Sandstorm.speedFactor( 1f ), 1e-6f );
		assertEquals( Sandstorm.BLIND_BASE + Sandstorm.BLIND_RISE, Sandstorm.blindChance( 1f ), 1e-6f );
		assertTrue( Sandstorm.sightFactor( 0.6f ) < Sandstorm.sightFactor( Sandstorm.FLOOR ) );
	}

	//the sand comes from where the wind blows from: the cell upwind of a hero
	@Test
	public void theSandComesFromUpwind(){
		assertArrayEquals( new int[]{ 0, 1 }, Sandstorm.upwind( 0f ) );       //blowing north: from the south
		assertArrayEquals( new int[]{ -1, 0 }, Sandstorm.upwind( 90f ) );     //blowing east: from the west
		assertArrayEquals( new int[]{ 0, -1 }, Sandstorm.upwind( 180f ) );    //blowing south: from the north
		assertArrayEquals( new int[]{ 1, 0 }, Sandstorm.upwind( 270f ) );     //blowing west: from the east
		assertArrayEquals( new int[]{ -1, 1 }, Sandstorm.upwind( 45f ) );     //blowing north-east: from the south-west
	}

	//over the desert's surface alone, in a wind of 10 m/s and more, never in a blizzard
	@Test
	public void itBlowsOnlyOverTheDesertsSurface(){
		int[] sand = cell( true ), grass = cell( false );
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugWindOverride = 15f;
		OverworldLevel desert = slice( 0, sand[0], sand[1] );
		assertTrue( desert.desertAt( MIDDLE ) );
		assertEquals( Sandstorm.strength( 15f ), Sandstorm.strengthAt( desert, MIDDLE ), 1e-6f );
		assertEquals( 0f, Sandstorm.strengthAt( slice( 0, grass[0], grass[1] ), MIDDLE ), 0f );
		assertEquals( "the peaks", 0f, Sandstorm.strengthAt( slice( 4, sand[0], sand[1] ), MIDDLE ), 0f );
		assertEquals( "the caves", 0f, Sandstorm.strengthAt( slice( -3, sand[0], sand[1] ), MIDDLE ), 0f );
		ClimateManager.debugWindOverride = 8f;
		assertEquals( 0f, Sandstorm.strengthAt( desert, MIDDLE ), 0f );
		ClimateManager.debugWindOverride = 15f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.BLIZZARD;
		ClimateManager.debugPrecipOverride = 0.4f;
		assertEquals( "a blizzard is its own wind", 0f, Sandstorm.strengthAt( desert, MIDDLE ), 0f );
	}

	//something solid one or two cells upwind keeps it off; water, a pit or open ground do not, nor
	//something three cells off; goggles or a scarf keep it off anywhere
	@Test
	public void theLeeAndTheWearKeepItOff(){
		int[] sand = cell( true );
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugWindOverride = 18f;
		ClimateManager.debugWindDirOverride = 270f;   //from the east
		OverworldLevel desert = slice( 0, sand[0], sand[1] );
		Dungeon.level = desert;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = MIDDLE;
		assertFalse( Sandstorm.sheltered( desert, MIDDLE ) );
		assertTrue( Sandstorm.exposure( Dungeon.hero ) > 0f );

		desert.solid[MIDDLE + 2] = true;
		assertTrue( "a rock two cells upwind", Sandstorm.sheltered( desert, MIDDLE ) );
		assertEquals( 0f, Sandstorm.exposure( Dungeon.hero ), 0f );
		assertEquals( 1f, Sandstorm.sightFactor( Dungeon.hero ), 0f );
		desert.water[MIDDLE + 2] = true;
		assertFalse( "deep water is no shelter", Sandstorm.sheltered( desert, MIDDLE ) );
		desert.water[MIDDLE + 2] = false;
		desert.solid[MIDDLE + 2] = false;
		desert.solid[MIDDLE + 3] = true;
		assertFalse( "three cells off", Sandstorm.sheltered( desert, MIDDLE ) );
		desert.solid[MIDDLE - 1] = true;
		assertFalse( "downwind", Sandstorm.sheltered( desert, MIDDLE ) );

		assertTrue( new SandGoggles().collect( Dungeon.hero.belongings.backpack ) );
		assertTrue( Sandstorm.wears( Dungeon.hero ) );
		assertEquals( 0f, Sandstorm.exposure( Dungeon.hero ), 0f );
		assertEquals( 1f, Sandstorm.speedFactor( Dungeon.hero ), 0f );
		Dungeon.hero.belongings.getItem( SandGoggles.class ).detach( Dungeon.hero.belongings.backpack );
		assertFalse( Sandstorm.wears( Dungeon.hero ) );
		assertTrue( new DesertScarf().collect( Dungeon.hero.belongings.backpack ) );
		assertTrue( Sandstorm.wears( Dungeon.hero ) );
	}

	//caught in it the hero is Sandblown, and slower and shorter-sighted; out of it, he is not
	@Test
	public void aHeroInItIsSandblownUntilItDrops(){
		int[] sand = cell( true );
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugWindOverride = 20f;
		ClimateManager.debugWindDirOverride = 270f;
		OverworldLevel desert = slice( 0, sand[0], sand[1] );
		Dungeon.level = desert;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = MIDDLE;
		Sandstorm.onHeroTurn( Dungeon.hero );
		Sandblown blown = Dungeon.hero.buff( Sandblown.class );
		assertNotNull( blown );
		assertEquals( Sandstorm.SLOW_LEAST, Sandstorm.speedFactor( Dungeon.hero ), 1e-6f );
		assertEquals( Sandstorm.SIGHT_LEAST, Sandstorm.sightFactor( Dungeon.hero ), 1e-6f );
		assertTrue( blown.desc().length() > 20 );

		//a game saved in it loads in the same storm: the numbers it shows, the rest after a blinding
		blown.blinded();
		com.watabou.utils.Bundle bundle = new com.watabou.utils.Bundle();
		blown.storeInBundle( bundle );
		Sandblown loaded = new Sandblown();
		loaded.restoreFromBundle( bundle );
		assertEquals( blown.desc(), loaded.desc() );
		assertEquals( blown.type, loaded.type );
		assertFalse( loaded.mayBlind() );
		//and one from before its fields were saved loads as a fresh one
		Sandblown old = new Sandblown();
		old.restoreFromBundle( new com.watabou.utils.Bundle() );
		assertTrue( old.mayBlind() );

		ClimateManager.debugWindOverride = 4f;
		Sandstorm.onHeroTurn( Dungeon.hero );
		assertNull( Dungeon.hero.buff( Sandblown.class ) );
	}

	//the workbench makes both: goggles from a glass pane and leather, the scarf from thatch
	@Test
	public void theWorkbenchMakesTheWear(){
		StationRecipe goggles = StationRecipes.byId( "sand_goggles" ), scarf = StationRecipes.byId( "desert_scarf" );
		assertNotNull( goggles );
		assertNotNull( scarf );
		assertEquals( Workbench.class, goggles.station );
		assertEquals( Workbench.class, scarf.station );
		assertTrue( goggles.output.sample() instanceof SandGoggles );
		assertTrue( scarf.output.sample() instanceof DesertScarf );
	}
}
