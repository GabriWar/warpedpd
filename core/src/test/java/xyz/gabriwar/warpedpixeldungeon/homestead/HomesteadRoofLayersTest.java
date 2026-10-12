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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofLayers;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The roofs on screen (tiles/RoofLayers; here for the store's package-private setRoof): each cell's tile by the 4-neighbour mask of the same roof and
 * its slope by the ridge row, the ridge caps' open ends, the building over the hero moved to the
 * faded layers with its own ridge caps, the roofs joined to a cell, and the fade itself: in over
 * 0.2 s to 15%, out again before another building takes its place, never again within one.
 */
public class HomesteadRoofLayersTest {

	private static final int OX = 300, OY = -40, W = 16, H = 12;

	private static int c( int x, int y ){
		return x + y * W;
	}

	//a 3x3 slate roof at (5..7, 5..7) and one thatch cell east of its middle
	private static HomesteadStore store(){
		HomesteadStore s = new HomesteadStore();
		for (int y = 5; y <= 7; y++) for (int x = 5; x <= 7; x++) s.setRoof( OX + x, OY + y, RoofTiles.SLATE, 60 );
		s.setRoof( OX + 8, OY + 6, RoofTiles.THATCH, 20 );
		return s;
	}

	@Test
	public void eachCellTakesItsTileByTheMaskOfItsOwnRoof(){
		HomesteadStore s = store();
		int n = W * H;
		int[] slopes = new int[n], ridges = new int[n], faded = new int[n], fadedRidges = new int[n];
		RoofLayers.compute( s, OX, OY, W, H, (boolean[]) null, slopes, ridges, faded, fadedRidges );
		int all = RoofTiles.N | RoofTiles.E | RoofTiles.S | RoofTiles.W;
		assertEquals( "the middle, roof all round", RoofTiles.index( RoofTiles.SLATE, all, false ), slopes[c( 6, 6 )] );
		assertEquals( "the north-west corner, on the shaded slope", RoofTiles.index( RoofTiles.SLATE, RoofTiles.E | RoofTiles.S, true ), slopes[c( 5, 5 )] );
		assertEquals( "the south edge", RoofTiles.index( RoofTiles.SLATE, RoofTiles.N | RoofTiles.E | RoofTiles.W, false ), slopes[c( 6, 7 )] );
		assertEquals( "thatch beside it joins nothing", RoofTiles.index( RoofTiles.SLATE, RoofTiles.N | RoofTiles.S | RoofTiles.W, false ), slopes[c( 7, 6 )] );
		assertEquals( RoofTiles.index( RoofTiles.THATCH, 0, false ), slopes[c( 8, 6 )] );
		//the ridge on the middle row, open at both its ends
		assertEquals( RoofTiles.ridge( RoofTiles.SLATE, RoofTiles.RIDGE_OPEN_W ), ridges[c( 5, 6 )] );
		assertEquals( RoofTiles.ridge( RoofTiles.SLATE, 0 ), ridges[c( 6, 6 )] );
		assertEquals( RoofTiles.ridge( RoofTiles.SLATE, RoofTiles.RIDGE_OPEN_E ), ridges[c( 7, 6 )] );
		assertEquals( -1, ridges[c( 6, 5 )] );
		assertEquals( -1, slopes[c( 1, 1 )] );
		for (int i = 0; i < n; i++){
			assertEquals( -1, faded[i] );
			assertEquals( -1, fadedRidges[i] );
		}
	}

	@Test
	public void aRoofIsDrawnInsetWithinItsHousesWalls(){
		//a 5x5 stone house at (4..8, 3..7) roofed over its walls too, as an area roof lays it
		HomesteadStore s = new HomesteadStore();
		int n = W * H;
		int[] map = new int[n];
		java.util.Arrays.fill( map, Terrain.GRASS );
		for (int y = 3; y <= 7; y++) for (int x = 4; x <= 8; x++){
			boolean edge = x == 4 || x == 8 || y == 3 || y == 7;
			map[c( x, y )] = edge ? Terrain.BUILT_WALL_STONE : Terrain.FLOOR_STONE;
			s.setRoof( OX + x, OY + y, RoofTiles.SLATE, 60 );
		}
		map[c( 6, 7 )] = Terrain.BUILT_DOOR_BARRED;
		map[c( 4, 5 )] = Terrain.BUILT_WINDOW_STONE;
		int[] slopes = new int[n], ridges = new int[n], faded = new int[n], fadedRidges = new int[n];
		RoofLayers.compute( s, OX, OY, W, H, null, map, slopes, ridges, faded, fadedRidges );
		//no roof over the walls, the window or the door: their tops show
		for (int y = 3; y <= 7; y++) for (int x = 4; x <= 8; x++){
			if (x == 4 || x == 8 || y == 3 || y == 7){
				assertEquals( "wall at " + x + "," + y, -1, slopes[c( x, y )] );
				assertEquals( -1, ridges[c( x, y )] );
			}
		}
		//the inner 3x3 is the roof, with its eaves against the walls
		assertEquals( RoofTiles.index( RoofTiles.SLATE, RoofTiles.E | RoofTiles.S, true ), slopes[c( 5, 4 )] );
		int all = RoofTiles.N | RoofTiles.E | RoofTiles.S | RoofTiles.W;
		assertEquals( RoofTiles.index( RoofTiles.SLATE, all, false ), slopes[c( 6, 5 )] );
		assertEquals( RoofTiles.ridge( RoofTiles.SLATE, 0 ), ridges[c( 6, 5 )] );
		//without the map (as before) the walls carry the roof too
		RoofLayers.compute( s, OX, OY, W, H, (boolean[]) null, slopes, ridges, faded, fadedRidges );
		assertTrue( slopes[c( 4, 3 )] != -1 );
	}

	@Test
	public void theBuildingOverTheHeroMovesToTheFadedLayersRidgeAndAll(){
		HomesteadStore s = store();
		int n = W * H;
		boolean[] joined = RoofLayers.connected( s, OX, OY, W, H, c( 6, 6 ) );
		assertNotNull( joined );
		assertTrue( "joined through to the thatch", joined[c( 8, 6 )] );
		assertFalse( joined[c( 4, 6 )] );
		assertNull( "no roof, nothing to fade", RoofLayers.connected( s, OX, OY, W, H, c( 1, 1 ) ) );

		//only the west column of the slate faded: the ridge breaks where the fading starts
		boolean[] hidden = new boolean[n];
		for (int y = 5; y <= 7; y++) hidden[c( 5, y )] = true;
		int[] slopes = new int[n], ridges = new int[n], faded = new int[n], fadedRidges = new int[n];
		RoofLayers.compute( s, OX, OY, W, H, hidden, slopes, ridges, faded, fadedRidges );
		assertEquals( -1, slopes[c( 5, 5 )] );
		assertEquals( RoofTiles.index( RoofTiles.SLATE, RoofTiles.E | RoofTiles.S, true ), faded[c( 5, 5 )] );
		assertEquals( -1, ridges[c( 5, 6 )] );
		assertEquals( RoofTiles.ridge( RoofTiles.SLATE, RoofTiles.RIDGE_OPEN_W | RoofTiles.RIDGE_OPEN_E ), fadedRidges[c( 5, 6 )] );
		assertEquals( RoofTiles.ridge( RoofTiles.SLATE, RoofTiles.RIDGE_OPEN_W ), ridges[c( 6, 6 )] );
		assertEquals( -1, faded[c( 6, 6 )] );
	}

	@Test
	public void aRoofFadesInOutAndWaitsForTheOldOne(){
		int n = W * H;
		boolean[] a = new boolean[n], a2 = new boolean[n], b = new boolean[n];
		a[3] = a[4] = true;
		a2[4] = a2[5] = true;
		b[40] = true;
		RoofLayers.Fade f = new RoofLayers.Fade();
		assertEquals( 1f, f.alpha, 0f );

		//nothing over the hero and nothing faded: nothing to lay again
		assertFalse( f.want( null ) );
		assertFalse( f.want( new boolean[n] ) );

		//stepping in: taken at once, still in full, then down to 15% in 0.2 s
		assertTrue( f.want( a ) );
		assertEquals( 1f, f.alpha, 0f );
		assertFalse( f.step( 0.1f ) );
		assertEquals( 1f - (1f - RoofLayers.FADE) / 2f, f.alpha, 0.001f );
		f.step( 0.15f );
		assertEquals( RoofLayers.FADE, f.alpha, 0f );

		//a step within the same building: nothing to lay again
		assertFalse( f.want( a.clone() ) );

		//the same building, a roof cell more: taken at once, still faded
		assertTrue( f.want( a2 ) );
		assertEquals( RoofLayers.FADE, f.alpha, 0f );

		//stepping out: it comes back in full before it leaves the faded layers
		assertFalse( f.want( null ) );
		assertFalse( f.step( 0.1f ) );
		assertTrue( f.shown == a2 );
		assertTrue( "back in full: the swap", f.step( 0.15f ) );
		assertNull( f.shown );
		assertEquals( 1f, f.alpha, 0f );
		f.step( 1f );
		assertEquals( "nothing faded stays in full", 1f, f.alpha, 0f );

		//straight from one building to another: the first comes back, then the second fades
		assertTrue( f.want( a ) );
		f.step( 1f );
		assertFalse( f.want( b ) );
		assertTrue( f.step( 1f ) );
		assertTrue( f.shown == b );
		f.step( 1f );
		assertEquals( RoofLayers.FADE, f.alpha, 0f );

		//a new window shows its building already faded
		f.reset( a );
		assertEquals( RoofLayers.FADE, f.alpha, 0f );
		f.reset( new boolean[n] );
		assertNull( f.shown );
		assertEquals( 1f, f.alpha, 0f );
	}
}
