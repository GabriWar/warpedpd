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
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A piece over a shape's cells before it is built (BuildPlan): the cost summed per material, how
 * many pieces the materials pay for, which cells get built and in what order when some don't take
 * the piece and the materials run short, what is missing, and the Recent list.
 */
public class BuildPlanTest {

	//a stock of materials, as many as put in
	private static BuildPlan.Stock stock( Object... pairs ){
		HashMap<Class<?>, Integer> have = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) have.put( (Class<?>) pairs[i], (Integer) pairs[i + 1] );
		return m -> have.containsKey( m ) ? have.get( m ) : 0;
	}

	@Test
	public void costsAreSummedPerMaterialAndTimesOver(){
		BuildPiece twice = BuildPiece.terrain( "test_twice", BuildCategory.WALLS, Terrain.BUILT_WALL_PLANK )
				.cost( Plank.class, 2 ).cost( Rope.class, 1 ).cost( Plank.class, 1 ).done();
		List<BuildPiece.Cost> sum = BuildPlan.aggregate( twice.cost, 4 );
		assertEquals( 2, sum.size() );
		assertEquals( Plank.class, sum.get( 0 ).material );
		assertEquals( 12, sum.get( 0 ).quantity );
		assertEquals( Rope.class, sum.get( 1 ).material );
		assertEquals( 4, sum.get( 1 ).quantity );

		//the planks count once, three a piece: 10 planks pay for three
		assertEquals( 3, BuildPlan.affordableCount( twice, stock( Plank.class, 10, Rope.class, 9 ) ) );
		assertEquals( 0, BuildPlan.affordableCount( twice, stock( Plank.class, 10 ) ) );
		BuildPiece free = BuildPiece.terrain( "test_free", BuildCategory.FLOORS, Terrain.FLOOR_PLANK ).done();
		assertEquals( Integer.MAX_VALUE, BuildPlan.affordableCount( free, stock() ) );
	}

	@Test
	public void whatIsMissingIsListed(){
		BuildPiece brick = BuildCatalog.byId( "wall_brick" );
		List<BuildPiece.Cost> miss = BuildPlan.missing( brick, 3, stock( FiredBrick.class, 5, Clay.class, 3 ) );
		assertEquals( 1, miss.size() );
		assertEquals( FiredBrick.class, miss.get( 0 ).material );
		assertEquals( 1, miss.get( 0 ).quantity );
		assertTrue( BuildPlan.missing( brick, 2, stock( FiredBrick.class, 5, Clay.class, 3 ) ).isEmpty() );
		assertEquals( 2, BuildPlan.missing( brick, 1, stock() ).size() );
	}

	@Test
	public void theFirstCellsThatTakeThePieceAreBuiltWhileTheMaterialsLast(){
		BuildPiece brick = BuildCatalog.byId( "wall_brick" );
		int[] cells = { 10, 11, 12, 13, 14, 15, 16 };
		//12 and 14 don't take it; bricks for three
		BuildPlan p = BuildPlan.of( brick, cells,
				c -> c == 12 ? Homestead.Reason.OCCUPIED : c == 14 ? Homestead.Reason.WRONG_GROUND : Homestead.Reason.OK,
				stock( FiredBrick.class, 7, Clay.class, 10 ) );
		assertEquals( 5, p.valid );
		assertEquals( 3, p.affordable );
		assertEquals( 2, p.skipped() );
		assertArrayEquals( new int[]{ 10, 11, 13 }, p.buildableCells() );
		assertTrue( p.buildable( 0 ) );
		assertFalse( "it doesn't take the piece", p.buildable( 2 ) );
		assertTrue( p.buildable( 3 ) );
		assertFalse( "valid, but the bricks ran out", p.buildable( 5 ) );
		assertEquals( Homestead.Reason.WRONG_GROUND, p.reasons[4] );

		BuildPlan none = BuildPlan.of( brick, cells, c -> Homestead.Reason.OK, stock() );
		assertEquals( 7, none.valid );
		assertEquals( 0, none.affordable );
		assertEquals( 0, none.buildableCells().length );
	}

	@Test
	public void recentPiecesComeNewestFirstAndKeepEight(){
		ArrayList<String> ids = new ArrayList<>();
		BuildRecent.use( ids, "wall_log" );
		BuildRecent.use( ids, "door" );
		BuildRecent.use( ids, "wall_log" );
		assertEquals( "wall_log", ids.get( 0 ) );
		assertEquals( "door", ids.get( 1 ) );
		assertEquals( 2, ids.size() );
		for (String id : new String[]{ "fence", "gate", "path", "floor_plank", "floor_stone", "roof_slate", "roof_tile", "bridge" }){
			BuildRecent.use( ids, id );
		}
		assertEquals( BuildRecent.MAX, ids.size() );
		assertEquals( "bridge", ids.get( 0 ) );
		assertFalse( "the oldest fell off", ids.contains( "wall_log" ) );
		ids.add( "no_such_piece" );
		List<BuildPiece> pieces = BuildRecent.pieces( ids );
		assertEquals( BuildRecent.MAX, pieces.size() );
		assertEquals( BuildCatalog.byId( "bridge" ), pieces.get( 0 ) );
	}

	@Test
	public void everyMaterialHasAColourOfItsOwn(){
		java.util.HashSet<Integer> colours = new java.util.HashSet<>();
		for (Class<? extends Item> m : xyz.gabriwar.warpedpixeldungeon.items.materials.Material.ALL){
			int c = MaterialColors.of( m );
			assertTrue( m.getSimpleName(), c != MaterialColors.OTHER );
			colours.add( c );
		}
		assertEquals( xyz.gabriwar.warpedpixeldungeon.items.materials.Material.ALL.length, colours.size() );
	}
}
