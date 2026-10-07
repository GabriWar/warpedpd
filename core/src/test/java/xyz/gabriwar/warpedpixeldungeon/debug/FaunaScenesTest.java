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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The fauna scenes (DebugScenes: layers-fauna-caves, layers-fauna-peaks, layers-goat-cliff): they
 * are listed, their finders find the ground they promise near the origin, the goat's cliff lip
 * stands in the real window of +2 with the drop behind it, and the catalogue holds every kind of
 * the bands at its furthest band.
 */
public class FaunaScenesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private Level saved;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
	}

	@After
	public void tearDown(){
		Dungeon.level = saved;
	}

	@Test
	public void theScenesAreListed(){
		assertEquals( "Caves: a creature of every band", DebugScenes.byId( "layers-fauna-caves" ).title() );
		assertEquals( "Peaks: a creature of every band", DebugScenes.byId( "layers-fauna-peaks" ).title() );
		assertEquals( "Peaks: a goat at a cliff edge", DebugScenes.byId( "layers-goat-cliff" ).title() );
	}

	@Test
	public void theFindersFindTheirGround(){
		int[] chamber = DebugScenes.faunaChamber( SEED, -6 );
		assertNotNull( chamber );
		for (int y = -4; y <= 4; y++){
			for (int x = -4; x <= 4; x++){
				assertTrue( WorldModel.caveOpen( SEED, chamber[0] + x, chamber[1] + y, -6 ) );
				assertFalse( WorldModel.caveWet( SEED, chamber[0] + x, chamber[1] + y, -6 ) );
			}
		}
		for (int a : new int[]{ 2, 5 }){
			long t = System.currentTimeMillis();
			int[] peak = DebugScenes.faunaPeak( SEED, a );
			System.out.println( "[fauna scenes] +" + a + " found in " + (System.currentTimeMillis() - t) + " ms" );
			assertNotNull( peak );
			assertEquals( a, WorldLayers.band( WorldModel.elevation( SEED, peak[0], peak[1] ) ) );
		}
	}

	@Test
	public void theGoatsLipHasTheDropBehindIt() throws Exception {
		int[] peak = DebugScenes.faunaPeak( SEED, 2 );
		assertNotNull( peak );
		PathFinder.setMapSize( W, H );
		int ox = peak[0] - W / 2, oy = peak[1] - H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 2, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 2, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		Field net = OverworldLevel.class.getDeclaredField( "network" );
		net.setAccessible( true );
		net.set( ow, false );
		Dungeon.level = ow;
		int centre = W / 2 + (H / 2) * W;
		int[] lip = DebugScenes.cliffLip( ow, centre, 40 );
		assertNotNull( "no cliff edge within 40 cells", lip );
		int d = lip[0] - lip[1];
		assertTrue( "the goat stands beside the hero", ow.adjacent( lip[0], lip[1] ) );
		assertTrue( "and the drop is at the hero's back", ow.pit[lip[0] + d] );
		assertTrue( ow.passable[lip[0]] && ow.passable[lip[1]] );
	}

	@Test
	public void theCatalogueHoldsEveryKindAtItsFurthestBand(){
		for (boolean caves : new boolean[]{ true, false }){
			ArrayList<DebugScenes.Kind> kinds = DebugScenes.faunaKinds( caves );
			HashSet<Class<? extends Mob>> all = new HashSet<>();
			for (int i = 1; WorldLayers.exists( caves ? -i : i ); i++) all.addAll( OverworldFauna.kindsAt( caves ? -i : i ) );
			assertEquals( all.size(), kinds.size() );
			for (DebugScenes.Kind k : kinds){
				assertTrue( all.contains( k.cls ) );
				assertTrue( OverworldFauna.kindsAt( k.altitude ).contains( k.cls ) );
				//no band further out has it
				for (int a = k.altitude + (caves ? -1 : 1); WorldLayers.exists( a ); a += caves ? -1 : 1){
					assertFalse( k.cls.getSimpleName() + " further out at " + a, OverworldFauna.kindsAt( a ).contains( k.cls ) );
				}
			}
		}
	}
}
