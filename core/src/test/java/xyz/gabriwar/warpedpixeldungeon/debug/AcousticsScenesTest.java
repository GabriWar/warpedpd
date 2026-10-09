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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.audio.Earshot;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaveLife;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import com.watabou.noosa.Game;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The room acoustics scenes (DebugScenes: acoustics-*): all ten are listed, and each finds the
 * spot it promises - the nearest room, the middle of the longest corridor, the biggest space, and
 * for the cavern and the fight a cavern of the world's caves.
 */
public class AcousticsScenesTest {

	private static final String[] IDS = { "acoustics-room", "acoustics-corridor", "acoustics-sewers", "acoustics-hall",
			"acoustics-cave", "acoustics-cavern", "acoustics-peaks", "acoustics-field", "acoustics-church", "acoustics-busy" };

	private String wasVersion;

	@BeforeClass
	public static void boot(){
		AllItemsTest.titleScreen();
	}

	@Before
	public void setUp(){
		wasVersion = Game.version;
		Game.version = "test";
	}

	@After
	public void tearDown(){
		Game.version = wasVersion;
	}

	@Test
	public void theScenesAreListedOnce(){
		HashSet<String> seen = new HashSet<>();
		int acoustics = 0;
		for (DebugScenes.Scene s : DebugScenes.SCENES){
			assertTrue( "one scene for " + s.id(), seen.add( s.id() ) );
			if (s instanceof DebugScenes.AcousticsScene) acoustics++;
		}
		for (String id : IDS){
			assertNotNull( id + " is listed", DebugScenes.byId( id ) );
			assertTrue( id + " is a room acoustics scene", DebugScenes.byId( id ) instanceof DebugScenes.AcousticsScene );
		}
		assertEquals( IDS.length, acoustics );
	}

	//a floor of rock with the rectangles given carved out of it, (x0, y0, x1, y1) each, both ends in
	private static Level floor( int[]... open ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		l.setSize( 50, 50 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.blobs = new HashMap<>();
		for (int[] r : open){
			for (int y = r[1]; y <= r[3]; y++) for (int x = r[0]; x <= r[2]; x++) l.map[x + y * 50] = Terrain.EMPTY;
		}
		l.buildFlagMaps();
		return l;
	}

	@Test
	public void theCorridorSceneStandsInTheMiddleOfTheLongestCorridor(){
		//a room, a corridor of 9 leaving it, and one of 21 across the floor
		Level l = floor( new int[]{ 5, 5, 11, 11 }, new int[]{ 12, 8, 20, 8 }, new int[]{ 10, 30, 30, 30 } );
		assertEquals( 20 + 30 * 50, DebugScenes.AcousticsScene.corridor( l ) );
		assertEquals( Earshot.Space.TIGHT, Earshot.of( l, 20 + 30 * 50 ).space );
	}

	@Test
	public void theLargestSceneStandsInTheBiggestSpace(){
		Level l = floor( new int[]{ 3, 3, 9, 9 }, new int[]{ 20, 20, 38, 38 } );
		int c = DebugScenes.AcousticsScene.largest( l, 6 + 6 * 50, 50 );
		assertTrue( "in the big room: " + c, c % 50 >= 20 && c % 50 <= 38 && c / 50 >= 20 && c / 50 <= 38 );
		assertEquals( Earshot.Space.HUGE, Earshot.of( l, c ).space );
	}

	@Test
	public void theCavernScenesStandInACavernOfTheWorldsCaves(){
		for (long dungeonSeed : new long[]{ 12345L, 777L, 4242L }){
			long seed = OverworldLevel.worldSeedOf( dungeonSeed );
			for (String id : new String[]{ "acoustics-cavern", "acoustics-busy" }){
				DebugScenes.AcousticsScene scene = (DebugScenes.AcousticsScene) DebugScenes.byId( id );
				//the window round the chamber the scene sends the hero to, as the world builds it
				int[] at = CaveLife.findChamber( seed, scene.altitude );
				assertNotNull( id + ": a chamber", at );
				int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
				WindowGenerator.Window win = WindowGenerator.generate( seed, scene.altitude, at[0] - w / 2, at[1] - h / 2, 0f );
				OverworldLevel l = new OverworldLevel( scene.altitude ){
					@Override public boolean frozenAt( int cell ){ return win.frozen[cell]; }
				};
				l.setSize( w, h );
				l.mobs = new HashSet<>();
				l.heaps = new SparseArray<>();
				l.plants = new SparseArray<>();
				l.traps = new SparseArray<>();
				l.blobs = new HashMap<>();
				System.arraycopy( win.terrain, 0, l.map, 0, w * h );
				l.buildFlagMaps();
				int c = scene.pick( l, w / 2 + h / 2 * w );
				assertTrue( id + ": a cell", c >= 0 );
				Earshot.Snapshot s = Earshot.of( l, c );
				assertEquals( id + " (" + dungeonSeed + "): " + s.describe(), Earshot.Tail.CAVERN, s.tail );
			}
		}
	}

	@Test
	public void theRoomSceneFindsTheNearestRoom(){
		//standing at the end of a long corridor that leads through a door to a room of 49 cells
		Level l = floor( new int[]{ 30, 30, 36, 36 }, new int[]{ 5, 33, 28, 33 } );
		l.map[29 + 33 * 50] = Terrain.DOOR;
		l.buildFlagMaps();
		assertEquals( Earshot.Space.TIGHT, Earshot.of( l, 5 + 33 * 50 ).space );
		DebugScenes.AcousticsScene room = (DebugScenes.AcousticsScene) DebugScenes.byId( "acoustics-room" );
		int c = room.pick( l, 5 + 33 * 50 );
		assertTrue( "a cell: " + c, c >= 0 );
		assertEquals( Earshot.Space.ROOM, Earshot.of( l, c ).space );
	}
}
