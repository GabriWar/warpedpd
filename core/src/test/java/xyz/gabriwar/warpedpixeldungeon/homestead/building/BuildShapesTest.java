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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The build bar's shapes (BuildShapes) and what the taps on the map mean (BuildSelection): lines
 * snapped to the eight directions and cut at the map's edge and at 64 cells, rectangles filled in
 * building order and their rims walked once round, the walls' area a hollow shell, and the taps
 * that start, end, move, build, pick and look.
 */
public class BuildShapesTest {

	private static final int W = 40, H = 30;

	private static int c( int x, int y ){
		return x + y * W;
	}

	// ------------------------------------------------------------------ lines

	@Test
	public void aLineSnapsToTheNearestOfTheEightDirections(){
		int from = c( 10, 10 );
		assertArrayEquals( "straight east", new int[]{ c( 10, 10 ), c( 11, 10 ), c( 12, 10 ), c( 13, 10 ) },
				BuildShapes.line( W, H, from, c( 13, 10 ) ) );
		assertArrayEquals( "a little off east is still east", new int[]{ c( 10, 10 ), c( 11, 10 ), c( 12, 10 ), c( 13, 10 ), c( 14, 10 ), c( 15, 10 ) },
				BuildShapes.line( W, H, from, c( 15, 12 ) ) );
		assertArrayEquals( "north", new int[]{ c( 10, 10 ), c( 10, 9 ), c( 10, 8 ), c( 10, 7 ) },
				BuildShapes.line( W, H, from, c( 11, 7 ) ) );
		//(1, -2) is 63 degrees off east, nearer the north-east than the north
		assertArrayEquals( "north-east", new int[]{ c( 10, 10 ), c( 11, 9 ), c( 12, 8 ) },
				BuildShapes.line( W, H, from, c( 11, 8 ) ) );
		//(5, 3) is 31 degrees off east: the diagonal, to the diagonal cell nearest it
		assertArrayEquals( "south-east", new int[]{ c( 10, 10 ), c( 11, 11 ), c( 12, 12 ), c( 13, 13 ), c( 14, 14 ) },
				BuildShapes.line( W, H, from, c( 15, 13 ) ) );
		assertArrayEquals( "north-west", new int[]{ c( 10, 10 ), c( 9, 9 ), c( 8, 8 ) },
				BuildShapes.line( W, H, from, c( 8, 8 ) ) );
		assertArrayEquals( "west", new int[]{ c( 10, 10 ), c( 9, 10 ), c( 8, 10 ) },
				BuildShapes.line( W, H, from, c( 8, 10 ) ) );
		assertArrayEquals( "onto itself", new int[]{ from }, BuildShapes.line( W, H, from, from ) );
	}

	@Test
	public void aLineStopsAtTheEdgeAndAtSixtyFourCells(){
		//south-east toward the diagonal cell nearest (39, 12), which is (40, 12): off the map
		assertArrayEquals( new int[]{ c( 38, 10 ), c( 39, 11 ) }, BuildShapes.line( W, H, c( 38, 10 ), c( 39, 12 ) ) );

		int wide = 200;
		int[] longLine = BuildShapes.line( wide, 10, 0, 150 );
		assertEquals( BuildShapes.MAX_CELLS, longLine.length );
		assertEquals( 0, longLine[0] );
		assertEquals( BuildShapes.MAX_CELLS - 1, longLine[BuildShapes.MAX_CELLS - 1] );
	}

	// ------------------------------------------------------------------ rectangles

	@Test
	public void aRectangleIsBuiltOutwardFromTheFirstCorner(){
		assertEquals( 6, BuildShapes.rectCount( W, c( 5, 5 ), c( 7, 6 ) ) );
		assertArrayEquals( new int[]{ c( 5, 5 ), c( 6, 5 ), c( 7, 5 ), c( 5, 6 ), c( 6, 6 ), c( 7, 6 ) },
				BuildShapes.rect( W, c( 5, 5 ), c( 7, 6 ) ) );
		//from the bottom-right corner it starts there and works back
		assertArrayEquals( new int[]{ c( 7, 6 ), c( 6, 6 ), c( 5, 6 ), c( 7, 5 ), c( 6, 5 ), c( 5, 5 ) },
				BuildShapes.rect( W, c( 7, 6 ), c( 5, 5 ) ) );
		assertArrayEquals( new int[]{ c( 3, 3 ) }, BuildShapes.rect( W, c( 3, 3 ), c( 3, 3 ) ) );
	}

	@Test
	public void anOutlineWalksTheRimOnceRound(){
		int[] rim = BuildShapes.outline( W, c( 2, 2 ), c( 5, 4 ) );
		assertEquals( 10, BuildShapes.outlineCount( W, c( 2, 2 ), c( 5, 4 ) ) );
		assertArrayEquals( new int[]{
				c( 2, 2 ), c( 3, 2 ), c( 4, 2 ),
				c( 5, 2 ), c( 5, 3 ),
				c( 5, 4 ), c( 4, 4 ), c( 3, 4 ),
				c( 2, 4 ), c( 2, 3 ) }, rim );
		//every rim cell once, no inside cell
		java.util.HashSet<Integer> seen = new java.util.HashSet<>();
		for (int cell : rim){
			assertTrue( seen.add( cell ) );
			int x = cell % W, y = cell / W;
			assertTrue( x == 2 || x == 5 || y == 2 || y == 4 );
		}
		//one cell thin, the outline is the line itself
		assertArrayEquals( BuildShapes.rect( W, c( 2, 2 ), c( 6, 2 ) ), BuildShapes.outline( W, c( 2, 2 ), c( 6, 2 ) ) );
		assertEquals( 5, BuildShapes.outlineCount( W, c( 2, 2 ), c( 6, 2 ) ) );
		//from the far corner, it starts there
		assertEquals( c( 5, 4 ), BuildShapes.outline( W, c( 5, 4 ), c( 2, 2 ) )[0] );
	}

	@Test
	public void wallsMakeAHollowShellFloorsAndRoofsFill(){
		assertTrue( BuildShapes.areaIsOutline( BuildCatalog.byId( "wall_stone" ) ) );
		assertTrue( BuildShapes.areaIsOutline( BuildCatalog.byId( "fence" ) ) );
		assertFalse( BuildShapes.areaIsOutline( BuildCatalog.byId( "floor_plank" ) ) );
		assertFalse( BuildShapes.areaIsOutline( BuildCatalog.byId( "path" ) ) );
		assertFalse( BuildShapes.areaIsOutline( BuildCatalog.byId( "roof_slate" ) ) );
		assertFalse( BuildShapes.areaIsOutline( null ) );
	}

	// ------------------------------------------------------------------ the taps

	@Test
	public void aLineIsStartedEndedMovedAndBuilt(){
		BuildSelection s = new BuildSelection();
		s.mode( BuildSelection.Mode.LINE );
		assertEquals( 0, s.cells( W, H ).length );
		assertEquals( BuildSelection.Step.ANCHOR, s.tap( c( 4, 4 ), W, H ) );
		assertFalse( s.previewing() );
		assertArrayEquals( "under the pointer before the second tap", new int[]{ c( 4, 4 ), c( 5, 4 ), c( 6, 4 ) }, s.cellsTo( c( 6, 4 ), W, H ) );
		assertEquals( BuildSelection.Step.PREVIEW, s.tap( c( 8, 4 ), W, H ) );
		assertTrue( s.previewing() );
		assertEquals( 5, s.cells( W, H ).length );
		//off the line: the end moves
		assertEquals( BuildSelection.Step.PREVIEW, s.tap( c( 4, 8 ), W, H ) );
		assertArrayEquals( new int[]{ c( 4, 4 ), c( 4, 5 ), c( 4, 6 ), c( 4, 7 ), c( 4, 8 ) }, s.cells( W, H ) );
		//on it: built
		assertEquals( BuildSelection.Step.CONFIRM, s.tap( c( 4, 6 ), W, H ) );
		s.clear();
		assertEquals( -1, s.anchor() );
		assertEquals( BuildSelection.Step.ANCHOR, s.tap( c( 1, 1 ), W, H ) );
		s.mode( BuildSelection.Mode.SINGLE );
		assertEquals( "a new mode forgets the picks", -1, s.anchor() );
		assertEquals( BuildSelection.Step.PLACE, s.tap( c( 1, 1 ), W, H ) );
	}

	@Test
	public void anAreaOfWallsIsItsRimAndTooBigIsNeverBuilt(){
		BuildSelection s = new BuildSelection();
		s.mode( BuildSelection.Mode.AREA );
		s.outline( true );
		s.tap( c( 2, 2 ), W, H );
		s.tap( c( 6, 5 ), W, H );
		assertEquals( 14, s.cells( W, H ).length );
		assertFalse( s.tooBig( W ) );
		assertEquals( "a tap inside the hollow shell builds it", BuildSelection.Step.CONFIRM, s.tap( c( 4, 3 ), W, H ) );
		assertEquals( "a tap outside it moves the corner", BuildSelection.Step.PREVIEW, s.tap( c( 7, 5 ), W, H ) );
		assertEquals( c( 7, 5 ), s.end() );

		BuildSelection f = new BuildSelection();
		f.mode( BuildSelection.Mode.AREA );
		f.tap( c( 0, 0 ), W, H );
		f.tap( c( 7, 7 ), W, H );
		assertEquals( 64, f.cells( W, H ).length );
		assertFalse( "8 by 8 is the most", f.tooBig( W ) );
		assertEquals( BuildSelection.Step.CONFIRM, f.tap( c( 3, 3 ), W, H ) );
		f.tap( c( 8, 7 ), W, H );
		assertTrue( "9 by 8 is too big", f.tooBig( W ) );
		assertEquals( 72, f.count( f.end(), W ) );
		//too big has no cells to build (the bar frames it whole), and a tap inside moves the corner
		assertEquals( 0, f.cells( W, H ).length );
		assertEquals( 0, f.cellsTo( c( 9, 9 ), W, H ).length );
		assertEquals( BuildSelection.Step.PREVIEW, f.tap( c( 3, 3 ), W, H ) );
		assertFalse( "moved back to 4 by 4", f.tooBig( W ) );
	}

	@Test
	public void demolishAndRepairPickThenAct(){
		BuildSelection s = new BuildSelection();
		s.mode( BuildSelection.Mode.DEMOLISH );
		assertEquals( BuildSelection.Step.TARGET, s.tap( c( 3, 3 ), W, H ) );
		assertEquals( c( 3, 3 ), s.target() );
		assertEquals( "another cell is picked instead", BuildSelection.Step.TARGET, s.tap( c( 4, 3 ), W, H ) );
		assertEquals( BuildSelection.Step.CONFIRM, s.tap( c( 4, 3 ), W, H ) );
		s.mode( BuildSelection.Mode.REPAIR );
		assertEquals( -1, s.target() );
		assertEquals( BuildSelection.Step.TARGET, s.tap( c( 1, 1 ), W, H ) );
		s.mode( BuildSelection.Mode.INSPECT );
		assertEquals( BuildSelection.Step.INSPECT, s.tap( c( 1, 1 ), W, H ) );
		assertEquals( BuildSelection.Step.INSPECT, s.tap( c( 1, 1 ), W, H ) );
	}
}
