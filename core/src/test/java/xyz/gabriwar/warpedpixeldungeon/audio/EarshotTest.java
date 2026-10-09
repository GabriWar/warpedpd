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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.TownChurchLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInnLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Where the hero hears from (Earshot): the space around him from the walls in reach of a sound -
 * a corridor, a room, a hall, a cave or the open sky - and how its tail is set; whether a sound
 * reaches him clear, through a door or through rock; and the echo off far walls and rock faces.
 */
public class EarshotTest {

	private Earshot.Clock wasClock;
	private String wasVersion;
	private final double[] now = { 100.0 };

	@BeforeClass
	public static void boot(){
		AllItemsTest.titleScreen();
	}

	@Before
	public void setUp(){
		wasClock = Earshot.clock;
		Earshot.clock = () -> now[0];
		//Level.set asks NetManager, whose first use reads the version
		wasVersion = Game.version;
		Game.version = "test";
	}

	@After
	public void tearDown(){
		Earshot.clock = wasClock;
		Game.version = wasVersion;
	}

	/** A floor of rock to carve spaces out of, with the place and the tiles a test gives it. */
	static final class Floor extends Level {
		Place place;
		String tiles;
		boolean held;

		Floor( int w, int h ){
			setSize( w, h );
			mobs = new HashSet<>();
			heaps = new SparseArray<>();
			plants = new SparseArray<>();
			traps = new SparseArray<>();
			blobs = new HashMap<>();
		}

		@Override protected boolean build(){ return true; }
		@Override protected void createMobs(){}
		@Override protected void createItems(){}
		@Override public Place ambience(){ return place; }
		@Override public String tilesTex(){ return tiles; }
		@Override public boolean fogHeld(){ return held; }

		int at( int x, int y ){
			return x + y * width();
		}

		/** Sets the rectangle from (x0, y0) to (x1, y1), both included, to a terrain. */
		Floor fill( int x0, int y0, int x1, int y1, int terrain ){
			for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) map[at( x, y )] = terrain;
			return this;
		}
	}

	/** A rock floor with one room of plain floor, from (x0, y0), w by h cells. */
	static Floor room( int x0, int y0, int w, int h ){
		return new Floor( 60, 60 ).fill( x0, y0, x0 + w - 1, y0 + h - 1, Terrain.EMPTY );
	}

	private Earshot.Snapshot at( Level l, int x, int y ){
		Earshot.Snapshot s = Earshot.of( l, x + y * l.width() );
		assertNotNull( "a snapshot", s );
		return s;
	}

	// ------------------------------------------------------------------ spaces

	@Test
	public void aCellOrAClosetIsTight(){
		Earshot.Snapshot s = at( room( 10, 10, 3, 3 ), 11, 11 );
		assertEquals( Earshot.Space.TIGHT, s.space );
		assertEquals( Earshot.Tail.ROOM, s.tail );
		assertEquals( -12f, s.level, 1e-4f );
		assertEquals( 9, s.area );
		assertEquals( 12, s.faces );
	}

	@Test
	public void aCorridorAndABentTunnelNetworkAreTight(){
		Earshot.Snapshot corridor = at( room( 10, 10, 14, 1 ), 15, 10 );
		assertEquals( Earshot.Space.TIGHT, corridor.space );
		assertEquals( -12f, corridor.level, 1e-4f );

		//120 cells of 1-wide tunnels, bends and junctions: far over the 20 cells of a closet, but
		//thin as a corridor
		Floor net = new Floor( 60, 60 )
				.fill( 5, 20, 35, 20, Terrain.EMPTY )
				.fill( 5, 5, 5, 35, Terrain.EMPTY )
				.fill( 35, 5, 35, 35, Terrain.EMPTY )
				.fill( 6, 5, 34, 5, Terrain.EMPTY );
		Earshot.Snapshot s = at( net, 20, 20 );
		assertEquals( 120, s.area );
		assertTrue( "thin: " + s.area / (float) s.faces, s.area / (float) s.faces < 0.9f );
		assertEquals( Earshot.Space.TIGHT, s.space );
		assertEquals( Earshot.Tail.ROOM, s.tail );
	}

	@Test
	public void sevenBySevenIsARoom(){
		Earshot.Snapshot s = at( room( 10, 10, 7, 7 ), 13, 13 );
		assertEquals( Earshot.Space.ROOM, s.space );
		assertEquals( Earshot.Tail.ROOM, s.tail );
		assertEquals( -11f, s.level, 1e-4f );
		assertEquals( 49, s.area );
		assertEquals( 28, s.faces );
	}

	@Test
	public void tenByTenIsLarge(){
		Earshot.Snapshot s = at( room( 10, 10, 10, 10 ), 14, 14 );
		assertEquals( Earshot.Space.LARGE, s.space );
		assertEquals( Earshot.Tail.HALL, s.tail );
		assertEquals( -11f, s.level, 1e-4f );
	}

	@Test
	public void thirteenByThirteenIsHuge(){
		Earshot.Snapshot s = at( room( 10, 10, 13, 13 ), 16, 16 );
		assertEquals( Earshot.Space.HUGE, s.space );
		assertEquals( Earshot.Tail.HALL, s.tail );
		assertEquals( -8f, s.level, 1e-4f );
		assertFalse( s.rock );
		assertEquals( "a stone hall has no echo", 0f, s.echoDelay, 0f );
	}

	@Test
	public void pillarsKeepTwelveByTwelveLarge(){
		Floor f = room( 10, 10, 12, 12 )
				.fill( 12, 12, 13, 13, Terrain.WALL ).fill( 18, 12, 19, 13, Terrain.WALL )
				.fill( 12, 18, 13, 19, Terrain.WALL ).fill( 18, 18, 19, 19, Terrain.WALL );
		Earshot.Snapshot s = at( f, 15, 15 );
		assertEquals( 128, s.area );
		assertEquals( 80, s.faces );
		assertEquals( Earshot.Space.LARGE, s.space );
	}

	// ------------------------------------------------------------------ caverns

	@Test
	public void aBigCaveRingsAsACavernAndAStoneHallAsBigAsAHall(){
		Floor cave = room( 10, 10, 25, 25 );
		cave.place = Place.CAVES;
		Earshot.Snapshot s = at( cave, 22, 22 );
		assertEquals( Earshot.Space.HUGE, s.space );
		assertTrue( s.rock );
		assertEquals( Earshot.Tail.CAVERN, s.tail );
		//2 dB under a hall's -8, with the caves' +1
		assertEquals( -10f + 1f, s.level, 1e-4f );
		assertTrue( s.describe(), s.describe().contains( "CAVERN tail at -9.0 dB" ) );

		Earshot.Snapshot stone = at( room( 10, 10, 25, 25 ), 22, 22 );
		assertEquals( 625, stone.area );
		assertEquals( Earshot.Space.HUGE, stone.space );
		assertEquals( Earshot.Tail.HALL, stone.tail );
		assertEquals( -8f, stone.level, 1e-4f );
	}

	//a floor of rock with one n by n space at (10, 10): 0 a cave, 1 a mine, 2 the world's caves at -3,
	//3 a floor with no place and the caves' tiles (a boss floor, a guest's copy of a floor)
	private static Level rock( int kind, int n ){
		if (kind == 2){
			OverworldLevel ow = slice( -3, false, false );
			carve( ow, 10, 10, n, n, Terrain.EMPTY );
			return ow;
		}
		Floor f = room( 10, 10, n, n );
		if (kind == 0) f.place = Place.CAVES;
		else if (kind == 1) f.place = Place.MINES;
		else f.tiles = Assets.Environment.TILES_CAVES_GNOLL;
		return f;
	}

	@Test
	public void rockRingsAsACavernFromWhereItIsHuge(){
		for (int kind = 0; kind < 4; kind++){
			//12 by 12, 144 cells: large, a hall's tail
			Earshot.Snapshot s = at( rock( kind, 12 ), 16, 16 );
			assertTrue( "rock " + kind, s.rock );
			assertEquals( "rock " + kind, Earshot.Space.LARGE, s.space );
			assertEquals( "rock " + kind, Earshot.Tail.HALL, s.tail );
			assertEquals( "rock " + kind, -11f + 1f, s.level, 1e-4f );
			//13 by 13, 169 cells: huge, a cavern's
			s = at( rock( kind, 13 ), 16, 16 );
			assertEquals( "rock " + kind, Earshot.Space.HUGE, s.space );
			assertEquals( "rock " + kind, Earshot.Tail.CAVERN, s.tail );
			assertEquals( "rock " + kind, -10f + 1f, s.level, 1e-4f );
		}
	}

	@Test
	public void stoneRingsAsACavernOnceItFillsMostOfTheWindow(){
		//26 by 26, 676 cells: a hall
		Earshot.Snapshot s = at( room( 10, 10, 26, 26 ), 23, 23 );
		assertEquals( 676, s.area );
		assertEquals( Earshot.Tail.HALL, s.tail );
		assertEquals( -8f, s.level, 1e-4f );
		//27 by 27, 729 cells (two thirds of the 33 by 33 a sound is followed over): a cavern, 2 dB
		//under the hall, and no echo off stone
		s = at( room( 10, 10, 27, 27 ), 23, 23 );
		assertEquals( 729, s.area );
		assertEquals( Earshot.Space.HUGE, s.space );
		assertFalse( s.rock );
		assertEquals( Earshot.Tail.CAVERN, s.tail );
		assertEquals( -10f, s.level, 1e-4f );
		assertEquals( 0f, s.echoDelay, 0f );
		//one wider than the window all round
		s = at( room( 5, 5, 50, 50 ), 30, 30 );
		assertEquals( Earshot.CELLS, s.area );
		assertEquals( Earshot.Tail.CAVERN, s.tail );
		//with its place's colour, as a hall's
		Floor halls = room( 10, 10, 27, 27 );
		halls.place = Place.HALLS;
		assertEquals( -10f + 1f, at( halls, 23, 23 ).level, 1e-4f );
	}

	// ------------------------------------------------------------- level changes

	@Test
	public void bookshelvesSoakUpTheTail(){
		Floor f = room( 10, 10, 7, 7 );
		f.fill( 9, 9, 17, 9, Terrain.BOOKSHELF ).fill( 9, 17, 17, 17, Terrain.BOOKSHELF )
				.fill( 9, 9, 9, 17, Terrain.BOOKSHELF ).fill( 17, 9, 17, 17, Terrain.EMPTY_BOOKSHELF );
		Earshot.Snapshot s = at( f, 13, 13 );
		assertEquals( 1f, s.soft, 1e-6f );
		assertEquals( -11f - 6f, s.level, 1e-4f );
	}

	@Test
	public void snowSoaksUpTheTail(){
		Floor f = room( 10, 10, 13, 13 ).fill( 10, 10, 22, 22, Terrain.SNOW );
		Earshot.Snapshot s = at( f, 16, 16 );
		assertEquals( 1f, s.absorb, 1e-6f );
		assertEquals( -8f - 8f, s.level, 1e-4f );
	}

	@Test
	public void waterOverMuchOfTheFloorBrightensIt(){
		//68 of 169 cells: 40%
		Floor f = room( 10, 10, 13, 13 ).fill( 10, 10, 22, 14, Terrain.WATER ).fill( 10, 15, 12, 15, Terrain.WATER );
		Earshot.Snapshot s = at( f, 16, 18 );
		assertEquals( 68f / 169f, s.water, 1e-6f );
		assertEquals( -8f + 1f, s.level, 1e-4f );
	}

	@Test
	public void rockTurnsARoomIntoAHallAndEchoes(){
		Floor cave = room( 10, 10, 7, 7 );
		cave.place = Place.CAVES;
		Earshot.Snapshot s = at( cave, 13, 13 );
		assertTrue( s.rock );
		assertEquals( Earshot.Space.ROOM, s.space );
		assertEquals( "a cave room rings like a hall", Earshot.Tail.HALL, s.tail );
		assertEquals( -12f + 1f, s.level, 1e-4f );

		//a boss floor or a guest's copy of a floor has no place: the caves' tiles tell rock
		Floor boss = room( 10, 10, 7, 7 );
		boss.tiles = Assets.Environment.TILES_CAVES_CRYSTAL;
		assertTrue( at( boss, 13, 13 ).rock );
		assertEquals( -11f, at( boss, 13, 13 ).level, 1e-4f );
	}

	@Test
	public void theFrozenBranchRingsAndTheNestIsHushed(){
		Floor frozen = room( 10, 10, 13, 13 );
		frozen.place = Place.FROZEN;
		assertEquals( -8f + 2f, at( frozen, 16, 16 ).level, 1e-4f );

		Floor nest = room( 10, 10, 7, 7 );
		nest.place = Place.NEST;
		assertEquals( -11f - 3f, at( nest, 13, 13 ).level, 1e-4f );
		nest.place = null;
		nest.tiles = Assets.Environment.TILES_SPIDERNEST;
		assertEquals( -11f - 3f, at( nest, 13, 13 ).level, 1e-4f );
	}

	@Test
	public void theSewersHaveARoofAndTheMeadowHasNone(){
		Floor sewers = room( 10, 10, 7, 7 );
		sewers.place = Place.SEWERS;
		Earshot.Snapshot s = at( sewers, 13, 13 );
		assertFalse( s.openAir );
		assertEquals( Earshot.Tail.ROOM, s.tail );
		assertEquals( -11f + 1f, s.level, 1e-4f );

		//the sewers' tiles are also the forest's and the beach's (Assets): no place, no say
		Floor tiles = room( 10, 10, 7, 7 );
		tiles.tiles = Assets.Environment.TILES_SEWERS;
		s = at( tiles, 13, 13 );
		assertFalse( s.openAir );
		assertEquals( Earshot.Tail.ROOM, s.tail );
		assertEquals( -11f, s.level, 1e-4f );

		Floor meadow = room( 10, 10, 7, 7 );
		meadow.place = Place.MEADOW;
		s = at( meadow, 13, 13 );
		assertTrue( s.openAir );
		assertEquals( Earshot.Space.OPEN, s.space );
		assertEquals( Earshot.Tail.NONE, s.tail );
		meadow.place = Place.SHORE;
		assertEquals( Earshot.Space.OPEN, at( meadow, 12, 13 ).space );
	}

	@Test
	public void theChurchRingsLikeAHallAndAHouseHardlyAtAll(){
		TownChurchLevel church = new TownChurchLevel();
		church.setSize( 40, 40 );
		carve( church, 10, 10, 7, 7, Terrain.EMPTY );
		Earshot.Snapshot s = at( church, 13, 13 );
		assertEquals( Earshot.Space.CHURCH, s.space );
		assertEquals( Earshot.Tail.HALL, s.tail );
		assertEquals( -9f, s.level, 1e-4f );

		TownInnLevel inn = new TownInnLevel();
		inn.setSize( 40, 40 );
		carve( inn, 10, 10, 13, 13, Terrain.EMPTY );
		s = at( inn, 16, 16 );
		assertEquals( "however big the room", Earshot.Space.HOUSE, s.space );
		assertEquals( Earshot.Tail.ROOM, s.tail );
		assertEquals( -17f, s.level, 1e-4f );
		//rugs over all of it: nothing left
		carve( inn, 10, 10, 13, 13, Terrain.WOOL_RUG );
		now[0] += 1;
		s = at( inn, 16, 16 );
		assertEquals( -17f - 8f * 0.7f, s.level, 1e-4f );
		assertEquals( Earshot.Tail.NONE, s.tail );
	}

	private static void carve( Level l, int x0, int y0, int w, int h, int terrain ){
		for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) l.map[x + y * l.width()] = terrain;
	}

	@Test
	public void aTailUnderTwentyDecibelsIsNone(){
		//a closet of snow behind bookshelves: -12 - 6 - 8
		Floor f = room( 10, 10, 3, 3 ).fill( 10, 10, 12, 12, Terrain.SNOW );
		f.fill( 9, 9, 13, 9, Terrain.BOOKSHELF ).fill( 9, 13, 13, 13, Terrain.BOOKSHELF )
				.fill( 9, 9, 9, 13, Terrain.BOOKSHELF ).fill( 13, 9, 13, 13, Terrain.BOOKSHELF );
		Earshot.Snapshot s = at( f, 11, 11 );
		assertEquals( -24f, s.level, 1e-4f );
		assertEquals( Earshot.Tail.NONE, s.tail );
	}

	// ------------------------------------------------------------------ relation

	@Test
	public void theOpenDiagonalOfAHallIsClear(){
		Floor f = room( 10, 10, 17, 17 );
		Earshot.Snapshot s = at( f, 18, 18 );
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 24, 24 ) ) );
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 18, 18 ) ) );
		assertEquals( -1, s.door( f.at( 24, 24 ) ) );
	}

	@Test
	public void aPillarDoesNotHideASound(){
		Floor f = room( 10, 10, 17, 17 ).fill( 20, 17, 22, 19, Terrain.WALL );
		assertEquals( Earshot.Relation.CLEAR, at( f, 18, 18 ).relation( f.at( 24, 18 ) ) );
	}

	@Test
	public void aClosedDoorMufflesAndAnOpenOneDoesNot(){
		Floor f = room( 10, 10, 7, 7 ).fill( 18, 10, 24, 16, Terrain.EMPTY ).fill( 17, 13, 17, 13, Terrain.DOOR );
		Earshot.Snapshot s = at( f, 13, 13 );
		assertEquals( Earshot.Relation.DOOR, s.relation( f.at( 21, 12 ) ) );
		assertEquals( "heard through the door", f.at( 17, 13 ), s.door( f.at( 21, 12 ) ) );

		f.fill( 17, 13, 17, 13, Terrain.OPEN_DOOR );
		now[0] += 1;
		s = at( f, 13, 13 );
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 21, 12 ) ) );
		assertEquals( -1, s.door( f.at( 21, 12 ) ) );

		//every kind of closed door, and a barricade
		for (int t : new int[]{ Terrain.LOCKED_DOOR, Terrain.HERO_LKD_DR, Terrain.CRYSTAL_DOOR, Terrain.BARRICADE, Terrain.LOCKED_EXIT }){
			f.fill( 17, 13, 17, 13, t );
			now[0] += 1;
			assertEquals( "through " + t, Earshot.Relation.DOOR, at( f, 13, 13 ).relation( f.at( 21, 12 ) ) );
		}
		//a hidden door is a wall till it is found: never given away
		f.fill( 17, 13, 17, 13, Terrain.SECRET_DOOR );
		now[0] += 1;
		assertEquals( Earshot.Relation.WALL, at( f, 13, 13 ).relation( f.at( 21, 12 ) ) );
	}

	@Test
	public void aLongWayRoundIsAWall(){
		//a wall between the hero and the sound, its end ten rows down
		Floor f = room( 10, 10, 30, 30 ).fill( 22, 10, 22, 30, Terrain.WALL );
		Earshot.Snapshot s = at( f, 20, 20 );
		assertEquals( Earshot.Relation.WALL, s.relation( f.at( 24, 20 ) ) );
		//by its end, the way round is short
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 24, 33 ) ) );
	}

	@Test
	public void throughTheDoorBeatsALongWayRound(){
		//the room next door through a closed door, and also by a loop of open ground round the top
		Floor f = room( 10, 10, 7, 7 ).fill( 18, 10, 24, 16, Terrain.EMPTY ).fill( 17, 13, 17, 13, Terrain.DOOR )
				.fill( 13, 5, 13, 9, Terrain.EMPTY ).fill( 13, 5, 21, 5, Terrain.EMPTY ).fill( 21, 5, 21, 9, Terrain.EMPTY );
		Earshot.Snapshot s = at( f, 13, 13 );
		assertEquals( Earshot.Relation.DOOR, s.relation( f.at( 21, 12 ) ) );
		assertEquals( "heard through the door", f.at( 17, 13 ), s.door( f.at( 21, 12 ) ) );
		//up the loop, the way round is the short one
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 16, 5 ) ) );

		//a door that leads nowhere near gives nothing round a long wall: the way through it comes
		//back out the same side
		Floor g = room( 10, 10, 30, 30 ).fill( 22, 10, 22, 30, Terrain.WALL )
				.fill( 15, 9, 15, 9, Terrain.DOOR ).fill( 14, 6, 16, 8, Terrain.EMPTY );
		Earshot.Snapshot t = at( g, 20, 20 );
		assertEquals( Earshot.Relation.WALL, t.relation( g.at( 24, 20 ) ) );
		assertEquals( -1, t.door( g.at( 24, 20 ) ) );
	}

	@Test
	public void twoDoorsAreAWall(){
		Floor f = room( 10, 10, 5, 5 ).fill( 16, 10, 20, 14, Terrain.EMPTY ).fill( 22, 10, 26, 14, Terrain.EMPTY )
				.fill( 15, 12, 15, 12, Terrain.DOOR ).fill( 21, 12, 21, 12, Terrain.DOOR );
		Earshot.Snapshot s = at( f, 12, 12 );
		assertEquals( Earshot.Relation.DOOR, s.relation( f.at( 18, 12 ) ) );
		assertEquals( Earshot.Relation.WALL, s.relation( f.at( 24, 12 ) ) );
		assertEquals( -1, s.door( f.at( 24, 12 ) ) );
	}

	@Test
	public void noSqueezingBetweenWallsThatTouchAtACorner(){
		//two rooms that meet only corner to corner, at (12, 12) and (13, 13)
		Floor f = room( 10, 10, 3, 3 ).fill( 13, 13, 16, 16, Terrain.EMPTY );
		Earshot.Snapshot s = at( f, 11, 11 );
		assertEquals( Earshot.Relation.WALL, s.relation( f.at( 14, 14 ) ) );
		//a step across a corner with one side open is fine
		f.fill( 13, 12, 13, 12, Terrain.EMPTY );
		now[0] += 1;
		assertEquals( Earshot.Relation.CLEAR, at( f, 11, 11 ).relation( f.at( 14, 14 ) ) );
	}

	@Test
	public void aSoundInAWallIsHeardFromItsSide(){
		Floor f = room( 10, 10, 7, 7 );
		Earshot.Snapshot s = at( f, 13, 13 );
		//digging into the room's wall, and a door on its edge
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 17, 13 ) ) );
		f.fill( 9, 13, 9, 13, Terrain.DOOR ).fill( 4, 10, 8, 16, Terrain.EMPTY );
		now[0] += 1;
		s = at( f, 13, 13 );
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 9, 13 ) ) );
		//deep in the rock, nowhere near anything open
		assertEquals( Earshot.Relation.WALL, s.relation( f.at( 13, 30 ) ) );
	}

	@Test
	public void beyondTheWindowTheStraightLineDecides(){
		Floor f = new Floor( 60, 9 ).fill( 1, 4, 58, 4, Terrain.EMPTY );
		Earshot.Snapshot s = at( f, 5, 4 );
		assertEquals( Earshot.Relation.CLEAR, s.relation( f.at( 35, 4 ) ) );
		f.fill( 20, 4, 20, 4, Terrain.DOOR );
		now[0] += 1;
		s = at( f, 5, 4 );
		assertEquals( Earshot.Relation.DOOR, s.relation( f.at( 35, 4 ) ) );
		f.fill( 25, 4, 25, 4, Terrain.WALL );
		now[0] += 1;
		assertEquals( Earshot.Relation.WALL, at( f, 5, 4 ).relation( f.at( 35, 4 ) ) );
		//either way along the line: the better one counts
		assertEquals( Earshot.Relation.WALL, Earshot.line( f.map, f.width(), f.at( 35, 4 ), f.at( 5, 4 ) ) );
		assertEquals( Earshot.Relation.CLEAR, Earshot.line( f.map, f.width(), f.at( 30, 4 ), f.at( 26, 4 ) ) );
	}

	// ---------------------------------------------------------------------- echo

	/** The open sky over a field of the given ground, a rock face `east` cells east of the hero, 41 rows tall. */
	private static Floor field( int east, int ground ){
		Floor f = new Floor( 100, 100 ).fill( 0, 0, 99, 99, ground ).fill( 40 + east, 30, 40 + east, 70, Terrain.WALL );
		f.place = Place.MEADOW;
		return f;
	}

	@Test
	public void aRockFaceAcrossTheFieldEchoes(){
		Earshot.Snapshot s = at( field( 25, Terrain.EMPTY ), 40, 50 );
		assertEquals( Earshot.Space.OPEN, s.space );
		assertEquals( 0.00875f * 25, s.echoDelay, 1e-4f );
		assertEquals( 0.219f, s.echoDelay, 0.001f );
		assertEquals( 0.25f * 0.95f, s.echoLevel, 1e-4f );
		assertTrue( "from the east", s.echoPan > 0 );

		//too near to be told from the sound itself
		assertEquals( 0f, at( field( 8, Terrain.EMPTY ), 40, 50 ).echoDelay, 0f );

		//snow takes most of it
		Earshot.Snapshot snow = at( field( 25, Terrain.SNOW ), 40, 50 );
		assertEquals( 0.4f * s.echoLevel, snow.echoLevel, 1e-4f );
		assertEquals( s.echoDelay, snow.echoDelay, 1e-6f );
	}

	@Test
	public void aRockHallEchoesAndAStoneOneDoesNot(){
		Floor cave = room( 10, 10, 25, 25 );
		cave.place = Place.CAVES;
		Earshot.Snapshot s = at( cave, 22, 22 );
		assertEquals( Earshot.Space.HUGE, s.space );
		assertTrue( s.rock );
		assertTrue( "delay " + s.echoDelay, s.echoDelay >= 0.10f && s.echoDelay <= 0.25f );
		assertEquals( 0.16f, s.echoLevel, 1e-4f );

		Floor stone = room( 10, 10, 25, 25 );
		assertEquals( 0f, at( stone, 22, 22 ).echoDelay, 0f );
	}

	@Test
	public void aCavesFarWallEchoesWhileItsNearOnesCloseIn(){
		//a chamber of 15 by 15, the hero two cells in from its west wall: most of its walls are near
		//(the middle of the rays' lengths under 8 cells), the east one 13 off
		Floor cave = room( 10, 10, 15, 15 );
		cave.place = Place.CAVES;
		Earshot.Snapshot s = at( cave, 12, 17 );
		assertEquals( Earshot.Space.HUGE, s.space );
		assertTrue( "delay " + s.echoDelay, s.echoDelay >= 0.10f && s.echoDelay <= 0.25f );
		assertEquals( 0.16f, s.echoLevel, 1e-4f );
		assertTrue( "from the east", s.echoPan > 0 );

		//in the middle of one of 13 by 13 every wall is under 11 cells off: its cavern tail, no echo
		Floor small = room( 10, 10, 13, 13 );
		small.place = Place.CAVES;
		s = at( small, 16, 16 );
		assertEquals( Earshot.Space.HUGE, s.space );
		assertEquals( Earshot.Tail.CAVERN, s.tail );
		assertEquals( 0f, s.echoDelay, 0f );
	}

	// ---------------------------------------------------------------- the world

	/** A slice of the world all rock, at an altitude, under a shelter or snowed under everywhere or nowhere. */
	private static OverworldLevel slice( int altitude, final boolean shelter, final boolean snowed ){
		OverworldLevel ow = new OverworldLevel( altitude ){
			@Override public boolean shelterAt( int cell ){ return shelter; }
			@Override public boolean frozenAt( int cell ){ return snowed; }
			@Override public boolean fogHeld(){ return false; }
		};
		ow.setSize( 60, 60 );
		ow.mobs = new HashSet<>();
		Arrays.fill( ow.map, Terrain.WALL );
		carve( ow, 10, 10, 7, 7, Terrain.EMPTY );
		return ow;
	}

	@Test
	public void theWorldHasItsSkyItsCavesAndItsShelters(){
		//the surface: under the open sky, walls round the hero or not
		Earshot.Snapshot s = at( slice( 0, false, false ), 13, 13 );
		assertTrue( s.openAir );
		assertEquals( Earshot.Space.OPEN, s.space );
		assertEquals( Earshot.Tail.NONE, s.tail );

		//the caves under it: a roof, and rock
		s = at( slice( -3, false, false ), 13, 13 );
		assertFalse( s.openAir );
		assertTrue( s.rock );
		assertEquals( Earshot.Tail.HALL, s.tail );
		assertEquals( -12f + 1f, s.level, 1e-4f );
		//ground the world calls snowed under soaks a tail up as snow does
		assertEquals( -12f + 1f - 8f, at( slice( -3, false, true ), 13, 13 ).level, 1e-4f );

		//a shelter on the peaks: a house
		s = at( slice( 4, true, false ), 13, 13 );
		assertFalse( s.openAir );
		assertEquals( Earshot.Space.HOUSE, s.space );
		assertEquals( -17f, s.level, 1e-4f );
	}

	// ----------------------------------------------------------- when there is none

	@Test
	public void nothingWhileTheMapIsBetweenFramesOrOffIt(){
		Floor f = room( 10, 10, 7, 7 );
		f.held = true;
		assertNull( Earshot.of( f, f.at( 13, 13 ) ) );
		f.held = false;
		assertNull( Earshot.of( f, -1 ) );
		assertNull( Earshot.of( f, f.length() ) );
		assertNull( Earshot.of( null, 3 ) );
	}

	// ------------------------------------------------------------------- caching

	@Test
	public void aSnapshotLastsHalfASecondAndThenSeesTheMapAgain(){
		Floor f = room( 10, 10, 7, 7 ).fill( 18, 10, 24, 16, Terrain.EMPTY ).fill( 17, 13, 17, 13, Terrain.OPEN_DOOR );
		int hero = f.at( 13, 13 ), far = f.at( 21, 13 );
		Earshot.Snapshot s = Earshot.of( f, hero );
		assertEquals( Earshot.Relation.CLEAR, s.relation( far ) );
		now[0] += 0.4;
		assertSame( s, Earshot.of( f, hero ) );

		//the door shut straight in the map, as a dozen places do without Level.set
		f.map[f.at( 17, 13 )] = Terrain.DOOR;
		now[0] += 0.05;
		assertEquals( Earshot.Relation.CLEAR, Earshot.of( f, hero ).relation( far ) );
		now[0] += 0.6;
		assertEquals( Earshot.Relation.DOOR, Earshot.of( f, hero ).relation( far ) );

		//another level, or the hero a step on: at once
		Earshot.Snapshot again = Earshot.of( f, hero );
		Floor other = room( 10, 10, 13, 13 );
		assertNotSame( again, Earshot.of( other, hero ) );
		assertEquals( Earshot.Space.HUGE, Earshot.of( other, hero ).space );
		assertNotSame( Earshot.of( other, hero ), Earshot.of( other, hero + 1 ) );
	}

	@Test
	public void threadsMayAskWhileTheMapChanges() throws InterruptedException {
		Earshot.clock = wasClock;
		Floor f = room( 10, 10, 7, 7 ).fill( 18, 10, 24, 16, Terrain.EMPTY ).fill( 17, 13, 17, 13, Terrain.DOOR );
		int door = f.at( 17, 13 );
		AtomicReference<Throwable> failed = new AtomicReference<>();
		Thread[] askers = new Thread[2];
		for (int t = 0; t < 2; t++){
			final int[] heroes = t == 0 ? new int[]{ f.at( 12, 12 ), f.at( 13, 14 ) } : new int[]{ f.at( 20, 12 ), f.at( 21, 14 ) };
			askers[t] = new Thread( () -> {
				try {
					for (int i = 0; i < 5000; i++){
						Earshot.Snapshot s = Earshot.of( f, heroes[i % 2] );
						s.relation( f.at( 21, 13 ) );
						s.door( f.at( 13, 13 ) );
						s.describe();
					}
				} catch (Throwable e){
					failed.compareAndSet( null, e );
				}
			} );
		}
		Thread flipper = new Thread( () -> {
			try {
				for (int i = 0; i < 2000; i++) Level.set( door, i % 2 == 0 ? Terrain.OPEN_DOOR : Terrain.DOOR, f );
			} catch (Throwable e){
				failed.compareAndSet( null, e );
			}
		} );
		for (Thread t : askers) t.start();
		flipper.start();
		for (Thread t : askers) t.join();
		flipper.join();
		if (failed.get() != null) throw new AssertionError( failed.get() );
	}

	@Test
	public void theDungeonsSeededRandomIsNeverTouched(){
		Floor f = room( 10, 10, 13, 13 );
		f.place = Place.CAVES;
		Random.pushGenerator( 1234 );
		int[] expected = new int[10];
		for (int i = 0; i < expected.length; i++) expected[i] = Random.Int( 1000000 );
		Random.popGenerator();

		Random.pushGenerator( 1234 );
		for (int i = 0; i < 100; i++){
			now[0] += 0.3;
			Earshot.Snapshot s = Earshot.of( f, f.at( 12 + i % 9, 16 ) );
			s.relation( f.at( 20, 20 ) );
			s.describe();
		}
		int[] got = new int[10];
		for (int i = 0; i < got.length; i++) got[i] = Random.Int( 1000000 );
		Random.popGenerator();
		assertArrayEquals( expected, got );
	}
}
