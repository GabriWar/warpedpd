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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Where the voices come from (Source): each rule on small maps drawn by hand - a drain pipe
 * over the water and one over the floor, a torch, a pool and a puddle, the cold lava, the
 * embermoss, the ice and the frozen ground, the cages, the webbing; and the open face a source
 * set in the rock is heard from.
 */
public class SourceTest {

	private static final int W = 9, H = 9;

	//a W x H map of `fill`
	private static int[] map( int fill ){
		int[] m = new int[W * H];
		Arrays.fill( m, fill );
		return m;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private static Soundscape.Ground ground( int[] map ){
		return new Soundscape.Ground().set( map, W, H, at( 4, 4 ), null );
	}

	private static boolean fits( Source s, int[] map, int x, int y ){
		return s.test( ground( map ), at( x, y ) );
	}

	@Test
	public void aPipeIsAWallDecorationOverTheWater(){
		int[] m = map( Terrain.EMPTY );
		for (int x = 0; x < W; x++) m[at( x, 3 )] = Terrain.WALL;
		for (int x = 0; x < W; x++) m[at( x, 4 )] = Terrain.WATER;
		m[at( 2, 3 )] = Terrain.WALL_DECO;
		m[at( 6, 3 )] = Terrain.WALL_DECO;
		m[at( 6, 4 )] = Terrain.EMPTY;
		assertTrue( "over the water", fits( Source.PIPE, m, 2, 3 ) );
		assertFalse( "over the floor: no water to spill into", fits( Source.PIPE, m, 6, 3 ) );
		assertFalse( "a plain wall", fits( Source.PIPE, m, 4, 3 ) );
		m[at( 2, 4 )] = Terrain.FROZEN_WATER;
		assertFalse( "over ice: nothing to spill into", fits( Source.PIPE, m, 2, 3 ) );
		m[at( 2, 4 )] = Terrain.WATER;
		//the drains: the pipe, and water against the wall; not water out in the open
		assertTrue( fits( Source.DRAIN, m, 2, 3 ) );
		assertTrue( fits( Source.DRAIN, m, 3, 4 ) );
		m[at( 4, 6 )] = Terrain.WATER;
		assertFalse( fits( Source.DRAIN, m, 4, 6 ) );
	}

	@Test
	public void onePipeInThreePoursAndAlwaysTheSameOnes(){
		int pours = 0;
		for (int c = 0; c < 3000; c++){
			if (Source.pours( c )) pours++;
			assertEquals( Source.pours( c ), Source.pours( c ) );
		}
		assertTrue( "about a third: " + pours, pours > 900 && pours < 1100 );
		//every pipe is a pipe, and those that pour pour; a wall that is no pipe pours nothing
		int[] m = map( Terrain.EMPTY );
		for (int x = 0; x < W; x++){
			m[at( x, 3 )] = Terrain.WALL_DECO;
			m[at( x, 4 )] = Terrain.WATER;
		}
		for (int x = 1; x < W - 1; x++){
			assertTrue( fits( Source.PIPE, m, x, 3 ) );
			assertEquals( Source.pours( at( x, 3 ) ), fits( Source.PIPE_POUR, m, x, 3 ) );
			m[at( x, 4 )] = Terrain.EMPTY;
			assertFalse( fits( Source.PIPE_POUR, m, x, 3 ) );
			m[at( x, 4 )] = Terrain.WATER;
		}
	}

	@Test
	public void aSourceInTheRockFacesTheOpenCellBeforeIt(){
		int[] m = map( Terrain.EMPTY );
		for (int x = 0; x < W; x++) m[at( x, 3 )] = Terrain.WALL;
		m[at( 2, 3 )] = Terrain.WALL_DECO;
		m[at( 6, 6 )] = Terrain.BOULDER;
		//a torch, a pipe, a rock face: the cell below, where its sound comes out
		assertEquals( at( 2, 4 ), Source.face( m, W, at( 2, 3 ) ) );
		assertEquals( at( 5, 4 ), Source.face( m, W, at( 5, 3 ) ) );
		m[at( 2, 4 )] = Terrain.WATER;
		assertEquals( "over water too", at( 2, 4 ), Source.face( m, W, at( 2, 3 ) ) );
		//rock with rock below shows no face; a boulder, the floor, the water are their own
		for (int x = 0; x < W; x++) m[at( x, 2 )] = Terrain.WALL;
		assertEquals( at( 4, 2 ), Source.face( m, W, at( 4, 2 ) ) );
		assertEquals( at( 6, 6 ), Source.face( m, W, at( 6, 6 ) ) );
		assertEquals( at( 4, 6 ), Source.face( m, W, at( 4, 6 ) ) );
		assertEquals( at( 2, 4 ), Source.face( m, W, at( 2, 4 ) ) );
	}

	@Test
	public void torchesVentsAndSeamsAreTheWallDecorations(){
		int[] m = map( Terrain.EMPTY );
		m[at( 4, 3 )] = Terrain.WALL_DECO;
		m[at( 5, 3 )] = Terrain.WALL;
		assertTrue( fits( Source.TORCH, m, 4, 3 ) );
		assertTrue( fits( Source.VENT, m, 4, 3 ) );
		assertTrue( fits( Source.ICE, m, 4, 3 ) );
		assertFalse( fits( Source.TORCH, m, 5, 3 ) );
		assertFalse( fits( Source.TORCH, m, 4, 4 ) );
		//meltwater falls right under a seam
		assertTrue( fits( Source.MELT, m, 4, 4 ) );
		assertFalse( fits( Source.MELT, m, 5, 4 ) );
		assertFalse( fits( Source.MELT, m, 4, 5 ) );
	}

	@Test
	public void aDropLandsInWaterOrByTheRock(){
		int[] m = map( Terrain.EMPTY );
		m[at( 2, 2 )] = Terrain.WALL;
		m[at( 6, 6 )] = Terrain.WATER;
		assertTrue( "in the water", fits( Source.DRIP, m, 6, 6 ) );
		assertTrue( "by the rock", fits( Source.DRIP, m, 3, 3 ) );
		assertFalse( "open floor, no rock above it", fits( Source.DRIP, m, 4, 6 ) );
		assertFalse( "the rock itself", fits( Source.DRIP, m, 2, 2 ) );
	}

	@Test
	public void openWaterIsAPoolNeverAPuddle(){
		int[] m = map( Terrain.EMPTY );
		for (int y = 2; y <= 6; y++) for (int x = 2; x <= 6; x++) m[at( x, y )] = Terrain.WATER;
		assertTrue( "the middle of a pool", fits( Source.BIG_WATER, m, 4, 4 ) );
		assertTrue( "five of eight neighbours", fits( Source.BIG_WATER, m, 2, 4 ) );
		assertFalse( "a corner: three of eight", fits( Source.BIG_WATER, m, 2, 2 ) );
		//the pool's edge laps, its middle (no bank beside it) does not
		assertTrue( fits( Source.LAPPING, m, 2, 4 ) );
		assertTrue( fits( Source.LAPPING, m, 2, 2 ) );
		assertFalse( fits( Source.LAPPING, m, 4, 4 ) );
		//a puddle neither laps nor is open water
		int[] p = map( Terrain.EMPTY );
		p[at( 4, 4 )] = Terrain.WATER;
		p[at( 5, 4 )] = Terrain.WATER;
		assertFalse( fits( Source.BIG_WATER, p, 4, 4 ) );
		assertFalse( fits( Source.LAPPING, p, 4, 4 ) );
		//ice is no water
		for (int i = 0; i < m.length; i++) if (m[i] == Terrain.WATER) m[i] = Terrain.FROZEN_WATER;
		assertFalse( fits( Source.BIG_WATER, m, 4, 4 ) );
		assertFalse( fits( Source.LAPPING, m, 2, 4 ) );
	}

	@Test
	public void theShoreIsBothSidesOfTheWatersEdge(){
		int[] m = map( Terrain.EMPTY );
		for (int y = 0; y < H; y++) for (int x = 5; x < W; x++) m[at( x, y )] = Terrain.WATER;
		assertTrue( "in the shallows by the bank", fits( Source.SHORE, m, 5, 4 ) );
		assertTrue( "on the bank", fits( Source.SHORE, m, 4, 4 ) );
		assertFalse( "out in the water", fits( Source.SHORE, m, 7, 4 ) );
		assertFalse( "back from the bank", fits( Source.SHORE, m, 2, 4 ) );
		//deep water is no place to sit, but a bank beside it is still a bank
		m[at( 5, 4 )] = Terrain.DEEP_WATER;
		assertFalse( fits( Source.SHORE, m, 5, 4 ) );
		assertTrue( fits( Source.SHORE, m, 4, 4 ) );
	}

	@Test
	public void theHallsLavaAndEmbers(){
		int[] m = map( Terrain.EMPTY );
		m[at( 3, 3 )] = Terrain.WATER;
		m[at( 4, 4 )] = Terrain.GRASS;
		m[at( 5, 4 )] = Terrain.HIGH_GRASS;
		m[at( 6, 4 )] = Terrain.EMBERS;
		m[at( 4, 5 )] = Terrain.BOOKSHELF;
		assertTrue( "cold lava is the halls' water", fits( Source.LAVA, m, 3, 3 ) );
		assertFalse( fits( Source.LAVA, m, 4, 3 ) );
		assertTrue( "embermoss", fits( Source.EMBERS, m, 4, 4 ) );
		assertTrue( "emberfungi", fits( Source.EMBERS, m, 5, 4 ) );
		assertTrue( "embers", fits( Source.EMBERS, m, 6, 4 ) );
		assertTrue( "a smouldering shelf", fits( Source.EMBERS, m, 4, 5 ) );
		assertFalse( fits( Source.EMBERS, m, 2, 2 ) );
		m[at( 3, 3 )] = Terrain.FROZEN_WATER;
		assertFalse( fits( Source.LAVA, m, 3, 3 ) );
	}

	@Test
	public void wallsFacesAndChasms(){
		int[] m = map( Terrain.EMPTY );
		for (int x = 0; x < W; x++){
			m[at( x, 1 )] = Terrain.WALL;
			m[at( x, 2 )] = Terrain.WALL;
		}
		m[at( 6, 6 )] = Terrain.BOULDER;
		m[at( 2, 6 )] = Terrain.CHASM;
		assertTrue( "rock over the floor", fits( Source.ROCK_FACE, m, 4, 2 ) );
		assertFalse( "rock over rock", fits( Source.ROCK_FACE, m, 4, 1 ) );
		assertTrue( "a boulder", fits( Source.ROCK_FACE, m, 6, 6 ) );
		assertTrue( "the foot of the wall", fits( Source.WALL_BASE, m, 4, 3 ) );
		assertFalse( "out on the floor", fits( Source.WALL_BASE, m, 4, 5 ) );
		assertTrue( fits( Source.CHASM, m, 2, 6 ) );
		assertFalse( fits( Source.CHASM, m, 3, 6 ) );
		assertTrue( fits( Source.FLOOR, m, 4, 5 ) );
		assertFalse( fits( Source.FLOOR, m, 2, 6 ) );
		m[at( 4, 3 )] = Terrain.WATER;
		assertFalse( "water is not floor", fits( Source.WALL_BASE, m, 4, 3 ) );
		assertFalse( fits( Source.FLOOR, m, 4, 3 ) );
	}

	@Test
	public void theRegionsDecorations(){
		int[] m = map( Terrain.EMPTY );
		m[at( 2, 2 )] = Terrain.REGION_DECO;
		m[at( 3, 2 )] = Terrain.REGION_DECO_ALT;
		m[at( 4, 2 )] = Terrain.DOOR;
		m[at( 5, 2 )] = Terrain.STATUE;
		m[at( 6, 2 )] = Terrain.STATUE_SP;
		//the prison's cages, standing and hanging, and its cell doors
		assertTrue( fits( Source.CAGE, m, 2, 2 ) );
		assertTrue( fits( Source.CAGE, m, 3, 2 ) );
		assertTrue( fits( Source.CAGE, m, 4, 2 ) );
		assertFalse( fits( Source.HANGING, m, 2, 2 ) );
		assertTrue( fits( Source.HANGING, m, 3, 2 ) );
		//the caves' scaffolds, the city's flames, the frozen branch's ice clusters
		assertTrue( fits( Source.SCAFFOLD, m, 2, 2 ) && fits( Source.SCAFFOLD, m, 3, 2 ) );
		assertTrue( fits( Source.FLAME, m, 2, 2 ) && fits( Source.FLAME, m, 3, 2 ) );
		assertTrue( fits( Source.ICE, m, 2, 2 ) );
		assertFalse( fits( Source.FLAME, m, 4, 2 ) );
		assertTrue( fits( Source.STATUE, m, 5, 2 ) && fits( Source.STATUE, m, 6, 2 ) );
		assertFalse( fits( Source.STATUE, m, 2, 2 ) );
	}

	@Test
	public void grassWebbingAndTrees(){
		int[] m = map( Terrain.EMPTY );
		m[at( 2, 2 )] = Terrain.GRASS;
		m[at( 3, 2 )] = Terrain.HIGH_GRASS;
		m[at( 4, 2 )] = Terrain.FLOWER_PATCH;
		m[at( 5, 2 )] = Terrain.EMPTY_DECO;
		m[at( 2, 4 )] = Terrain.TREE_OAK;
		m[at( 3, 4 )] = Terrain.TREE_PINE;
		m[at( 4, 4 )] = Terrain.SHRUB;
		assertTrue( fits( Source.GRASS, m, 2, 2 ) && fits( Source.GRASS, m, 3, 2 ) && fits( Source.GRASS, m, 4, 2 ) );
		assertFalse( fits( Source.GRASS, m, 5, 2 ) );
		assertTrue( "webbing and the husks in it", fits( Source.WEB, m, 2, 2 ) && fits( Source.WEB, m, 5, 2 ) );
		assertFalse( fits( Source.WEB, m, 6, 2 ) );
		assertTrue( fits( Source.TREE, m, 2, 4 ) && fits( Source.TREE, m, 3, 4 ) && fits( Source.TREE, m, 4, 4 ) );
		assertFalse( fits( Source.TREE, m, 2, 2 ) );
	}

	@Test
	public void frostIsIceOrGroundFrozenOver(){
		assertTrue( Source.frost( Terrain.FROZEN_WATER, false ) );
		assertTrue( Source.frost( Terrain.SNOW, true ) );
		assertTrue( Source.frost( Terrain.EMPTY, true ) );
		assertFalse( "thawed", Source.frost( Terrain.SNOW, false ) );
		assertFalse( "rock is no ground", Source.frost( Terrain.WALL, true ) );
		assertFalse( Source.frost( Terrain.WATER, false ) );
		//off the overworld nothing is frozen over but frozen water
		int[] m = map( Terrain.EMPTY );
		m[at( 3, 3 )] = Terrain.FROZEN_WATER;
		assertTrue( fits( Source.FROST, m, 3, 3 ) );
		assertFalse( fits( Source.FROST, m, 4, 4 ) );
	}

	@Test
	public void theAirIsNoCell(){
		int[] m = map( Terrain.EMPTY );
		for (int c = 0; c < m.length; c++){
			int x = c % W, y = c / W;
			if (x < 1 || y < 1 || x > W - 2 || y > H - 2) continue;
			assertFalse( Source.AIR.test( ground( m ), c ) );
		}
	}
}
