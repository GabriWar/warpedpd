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
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Fauna.Air;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Fauna.Kind;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The dungeon's small life is pictures, but where it turns up is a set of pure rules over the
 * floor's map: wall bases, corners, banks and open water, lights, perches, shelves, moss and
 * webbing - with nothing ever drawn over a critter of the floor from the cell below - and the
 * ways a creature runs, which never go through rock or water or cut a corner.
 */
public class HabitatTest {

	private static final int W = 16, H = 12;
	private int[] map;

	@Before
	public void rock(){
		map = new int[W * H];
		Arrays.fill( map, Terrain.WALL );
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private void set( int x, int y, int t ){
		map[at( x, y )] = t;
	}

	private void fill( int x, int y, int w, int h, int t ){
		for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) set( i, j, t );
	}

	// ------------------------------------------------------------ the floor

	@Test
	public void groundIsDryFloorACritterIsSeenOn(){
		//a hidden trap is drawn as the floor: shying from it would give it away
		for (int t : new int[]{ Terrain.EMPTY, Terrain.EMPTY_SP, Terrain.EMPTY_DECO, Terrain.GRASS, Terrain.EMBERS,
				Terrain.INACTIVE_TRAP, Terrain.SECRET_TRAP }){
			assertTrue( "terrain " + t, Habitat.ground( t ) );
		}
		for (int t : new int[]{ Terrain.WATER, Terrain.FROZEN_WATER, Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS, Terrain.DOOR,
				Terrain.OPEN_DOOR, Terrain.ENTRANCE, Terrain.EXIT, Terrain.TRAP, Terrain.PEDESTAL, Terrain.CHASM, Terrain.WALL,
				Terrain.CUSTOM_DECO_EMPTY, Terrain.WELL }){
			assertFalse( "terrain " + t, Habitat.ground( t ) );
		}
	}

	@Test
	public void nothingIsDrawnOverACritterFromTheCellBelow(){
		fill( 2, 2, 8, 6, Terrain.EMPTY );
		int c = at( 4, 4 );
		assertTrue( Habitat.clearBelow( map, W, c ) );
		assertTrue( Habitat.stand( map, W, c ) );
		//a wall's lip, a statue's top, a tuft, a door's frame, a shelf, a silk pillar: all drawn over it
		for (int t : new int[]{ Terrain.WALL, Terrain.WALL_DECO, Terrain.STATUE, Terrain.STATUE_SP, Terrain.REGION_DECO,
				Terrain.HIGH_GRASS, Terrain.DOOR, Terrain.OPEN_DOOR, Terrain.BOOKSHELF, Terrain.BARRICADE, Terrain.CUSTOM_DECO,
				Terrain.MINE_CRYSTAL, Terrain.ALCHEMY }){
			set( 4, 5, t );
			assertFalse( "under terrain " + t, Habitat.clearBelow( map, W, c ) );
			assertFalse( Habitat.stand( map, W, c ) );
		}
		for (int t : new int[]{ Terrain.WATER, Terrain.CHASM, Terrain.GRASS, Terrain.FROZEN_WATER, Terrain.EMPTY_DECO }){
			set( 4, 5, t );
			assertTrue( "over terrain " + t, Habitat.clearBelow( map, W, c ) );
		}
		//the bottom row has nothing below it on the map
		assertFalse( Habitat.clearBelow( map, W, at( 4, H - 1 ) ) );
	}

	@Test
	public void wallBasesSidesAndCorners(){
		fill( 2, 2, 6, 5, Terrain.EMPTY );
		assertTrue( Habitat.wallBase( map, W, at( 4, 2 ) ) );
		assertFalse( Habitat.wallBase( map, W, at( 4, 3 ) ) );
		//a decorated face and a hidden door are rock too; a shelf is a shelf
		set( 5, 1, Terrain.WALL_DECO );
		assertTrue( Habitat.wallBase( map, W, at( 5, 2 ) ) );
		set( 6, 1, Terrain.SECRET_DOOR );
		assertTrue( Habitat.wallBase( map, W, at( 6, 2 ) ) );
		set( 3, 1, Terrain.BOOKSHELF );
		assertFalse( Habitat.wallBase( map, W, at( 3, 2 ) ) );
		assertTrue( Habitat.shelfBase( map, W, at( 3, 2 ) ) );

		assertTrue( Habitat.wallSide( map, W, at( 2, 4 ) ) );
		assertTrue( Habitat.wallSide( map, W, at( 7, 4 ) ) );
		assertFalse( Habitat.wallSide( map, W, at( 4, 4 ) ) );

		//the four corners of the room, and nowhere else
		assertTrue( Habitat.corner( map, W, at( 2, 2 ) ) );
		assertTrue( Habitat.corner( map, W, at( 7, 2 ) ) );
		assertTrue( Habitat.corner( map, W, at( 2, 6 ) ) );
		assertTrue( Habitat.corner( map, W, at( 7, 6 ) ) );
		assertFalse( Habitat.corner( map, W, at( 4, 2 ) ) );
		assertFalse( Habitat.corner( map, W, at( 2, 4 ) ) );
		assertFalse( Habitat.corner( map, W, at( 4, 4 ) ) );
	}

	@Test
	public void banksAndOpenWater(){
		fill( 1, 1, 12, 9, Terrain.EMPTY );
		fill( 4, 3, 3, 3, Terrain.WATER );
		//beside the water, north, east, south and west of it
		assertEquals( at( 4, 4 ), Habitat.waterBeside( map, W, at( 3, 4 ) ) );
		assertEquals( at( 6, 4 ), Habitat.waterBeside( map, W, at( 7, 4 ) ) );
		assertEquals( at( 5, 3 ), Habitat.waterBeside( map, W, at( 5, 2 ) ) );
		assertEquals( at( 5, 5 ), Habitat.waterBeside( map, W, at( 5, 6 ) ) );
		assertTrue( Habitat.waterEdge( map, W, at( 3, 4 ) ) );
		//only across a corner is no bank; the water itself is none
		assertFalse( Habitat.waterEdge( map, W, at( 3, 2 ) ) );
		assertFalse( Habitat.waterEdge( map, W, at( 5, 4 ) ) );
		//the pool's middle and its sides are open (five of eight neighbours wet), its corners not
		assertTrue( Habitat.openWater( map, W, H, at( 5, 4 ) ) );
		assertTrue( Habitat.openWater( map, W, H, at( 4, 4 ) ) );
		assertFalse( Habitat.openWater( map, W, H, at( 4, 3 ) ) );
		assertFalse( Habitat.openWater( map, W, H, at( 3, 4 ) ) );
		//a one-wide channel is no open water
		fill( 1, 8, 10, 1, Terrain.WATER );
		assertFalse( Habitat.openWater( map, W, H, at( 5, 8 ) ) );
		//ice is neither water nor a bank
		fill( 4, 3, 3, 3, Terrain.FROZEN_WATER );
		assertFalse( Habitat.openWater( map, W, H, at( 5, 4 ) ) );
		assertFalse( Habitat.waterEdge( map, W, at( 3, 4 ) ) );
		assertEquals( -1, Habitat.waterBeside( map, W, at( 3, 4 ) ) );
	}

	@Test
	public void theColdLavasEdgeIsTheWatersEdge(){
		//the halls' lava is water to their map
		fill( 1, 1, 10, 8, Terrain.EMPTY );
		fill( 5, 3, 2, 2, Terrain.WATER );
		assertTrue( Habitat.lavaEdge( map, W, at( 4, 3 ) ) );
		assertTrue( Habitat.groundFor( Kind.SALAMANDER, Place.HALLS, map, W, H, at( 4, 3 ) ) );
		assertFalse( Habitat.groundFor( Kind.SALAMANDER, Place.HALLS, map, W, H, at( 2, 6 ) ) );
		//lava (and embers) are the halls' moths' light
		assertTrue( Habitat.mothLight( Place.HALLS, map, W, H, at( 5, 3 ) ) );
		set( 2, 6, Terrain.EMBERS );
		assertTrue( Habitat.mothLight( Place.HALLS, map, W, H, at( 2, 6 ) ) );
		assertFalse( Habitat.mothLight( Place.HALLS, map, W, H, at( 3, 6 ) ) );
	}

	@Test
	public void pipesTorchesFlamesAndVents(){
		fill( 2, 2, 8, 5, Terrain.EMPTY );
		set( 4, 2, Terrain.WATER );
		set( 4, 1, Terrain.WALL_DECO );
		set( 6, 1, Terrain.WALL_DECO );
		//over the water it is a pipe; over the floor a torch (in the city, a vent)
		assertTrue( Habitat.pipe( map, W, at( 4, 1 ) ) );
		assertFalse( Habitat.torch( map, W, at( 4, 1 ) ) );
		assertTrue( Habitat.torch( map, W, at( 6, 1 ) ) );
		assertFalse( Habitat.pipe( map, W, at( 6, 1 ) ) );
		assertFalse( Habitat.torch( map, W, at( 5, 1 ) ) );
		assertTrue( Habitat.ventBase( map, W, at( 6, 2 ) ) );
		assertFalse( Habitat.ventBase( map, W, at( 5, 2 ) ) );
		assertTrue( Habitat.flame( Terrain.REGION_DECO ) );
		assertTrue( Habitat.flame( Terrain.REGION_DECO_ALT ) );
		assertFalse( Habitat.flame( Terrain.STATUE ) );
		//the prison's moths circle the torch, the frozen branch's the seam, the city's the flame
		assertTrue( Habitat.mothLight( Place.PRISON, map, W, H, at( 6, 1 ) ) );
		assertTrue( Habitat.mothLight( Place.FROZEN, map, W, H, at( 6, 1 ) ) );
		assertFalse( Habitat.mothLight( Place.CITY, map, W, H, at( 6, 1 ) ) );
		set( 8, 4, Terrain.REGION_DECO );
		assertTrue( Habitat.mothLight( Place.CITY, map, W, H, at( 8, 4 ) ) );
		assertTrue( Habitat.mothLight( Place.VAULT, map, W, H, at( 8, 4 ) ) );
		assertFalse( Habitat.mothLight( Place.PRISON, map, W, H, at( 8, 4 ) ) );
	}

	@Test
	public void statuesBirdsPerchOn(){
		fill( 2, 2, 8, 6, Terrain.EMPTY );
		set( 4, 4, Terrain.STATUE );
		set( 6, 4, Terrain.STATUE_SP );
		set( 5, 2, Terrain.STATUE );
		assertTrue( Habitat.perch( map, W, at( 4, 4 ) ) );
		assertTrue( Habitat.perch( map, W, at( 6, 4 ) ) );
		//against the wall its top is hidden in the rock
		assertFalse( Habitat.perch( map, W, at( 5, 2 ) ) );
		assertFalse( Habitat.perch( map, W, at( 4, 5 ) ) );
		assertTrue( Habitat.groundFor( Kind.SWIFT, Place.CITY, map, W, H, at( 4, 4 ) ) );
		assertTrue( Habitat.groundFor( Kind.CROW, Place.HALLS, map, W, H, at( 6, 4 ) ) );
	}

	@Test
	public void mossWebbingScaffoldsAndGlowWalls(){
		assertTrue( Habitat.moss( Place.SEWERS, Terrain.EMPTY_DECO ) );
		assertTrue( Habitat.moss( Place.CATACOMB, Terrain.EMPTY_DECO ) );
		assertFalse( Habitat.moss( Place.SEWERS, Terrain.GRASS ) );
		assertTrue( Habitat.moss( Place.CAVES, Terrain.GRASS ) );
		assertTrue( Habitat.moss( Place.HALLS, Terrain.GRASS ) );
		//the prison's floor deco is blood, not moss
		assertFalse( Habitat.moss( Place.PRISON, Terrain.EMPTY_DECO ) );
		assertTrue( Habitat.webbing( Terrain.GRASS ) );
		assertTrue( Habitat.webbing( Terrain.HIGH_GRASS ) );
		assertFalse( Habitat.webbing( Terrain.EMPTY ) );

		fill( 2, 2, 8, 6, Terrain.EMPTY );
		set( 6, 5, Terrain.REGION_DECO );
		assertTrue( Habitat.byScaffold( map, W, H, at( 5, 4 ) ) );
		assertTrue( Habitat.byScaffold( map, W, H, at( 7, 6 ) ) );
		assertFalse( Habitat.byScaffold( map, W, H, at( 3, 3 ) ) );
		//the ALT is the same metal structure, and the one most of the caves' rooms lay
		set( 6, 5, Terrain.REGION_DECO_ALT );
		assertTrue( Habitat.byScaffold( map, W, H, at( 5, 4 ) ) );
		assertTrue( Habitat.byScaffold( map, W, H, at( 7, 6 ) ) );
		assertFalse( Habitat.byScaffold( map, W, H, at( 3, 3 ) ) );
		//itself it is not beside
		assertFalse( Habitat.byScaffold( map, W, H, at( 6, 5 ) ) );

		assertFalse( Habitat.glowWall( map, W, H, at( 3, 1 ) ) );
		set( 2, 2, Terrain.GRASS );
		assertTrue( Habitat.glowWall( map, W, H, at( 3, 1 ) ) );
		assertTrue( Habitat.glowWall( map, W, H, at( 2, 1 ) ) );
		assertFalse( Habitat.glowWall( map, W, H, at( 5, 1 ) ) );
		//rock over rock has no face to cling to
		assertFalse( Habitat.glowWall( map, W, H, at( 3, 0 ) ) );
	}

	// ------------------------------------------------------------ who lives where

	@Test
	public void eachKindOnItsOwnGround(){
		fill( 1, 1, 14, 10, Terrain.EMPTY );
		//a stretch of wall across the room, a torch (or a vent) in it, its faces over row 4
		fill( 3, 3, 5, 1, Terrain.WALL );
		set( 6, 3, Terrain.WALL_DECO );
		fill( 9, 5, 4, 4, Terrain.WATER );
		set( 2, 8, Terrain.EMPTY_DECO );

		//frogs on a bank or the moss; roaches and centipedes at a wall's foot; mice in a corner
		assertTrue( Habitat.groundFor( Kind.FROG, Place.SEWERS, map, W, H, at( 8, 6 ) ) );
		assertTrue( Habitat.groundFor( Kind.FROG, Place.SEWERS, map, W, H, at( 2, 8 ) ) );
		assertFalse( Habitat.groundFor( Kind.FROG, Place.SEWERS, map, W, H, at( 5, 7 ) ) );
		assertTrue( Habitat.groundFor( Kind.ROACH, Place.SEWERS, map, W, H, at( 4, 4 ) ) );
		assertFalse( Habitat.groundFor( Kind.ROACH, Place.SEWERS, map, W, H, at( 4, 6 ) ) );
		assertTrue( Habitat.groundFor( Kind.CENTIPEDE, Place.MINES, map, W, H, at( 5, 4 ) ) );
		assertTrue( Habitat.groundFor( Kind.MOUSE, Place.PRISON, map, W, H, at( 1, 1 ) ) );
		assertFalse( Habitat.groundFor( Kind.MOUSE, Place.PRISON, map, W, H, at( 5, 4 ) ) );
		//a bank over a wall's lip is no bank to sit on
		set( 14, 10, Terrain.WATER );
		assertTrue( Habitat.waterEdge( map, W, at( 13, 10 ) ) );
		assertFalse( Habitat.groundFor( Kind.FROG, Place.SEWERS, map, W, H, at( 13, 10 ) ) );

		//the newts at the water, the striders and the fish out on it; never at its corner
		assertTrue( Habitat.groundFor( Kind.NEWT, Place.CAVES, map, W, H, at( 9, 4 ) ) );
		assertTrue( Habitat.groundFor( Kind.STRIDER, Place.SEWERS, map, W, H, at( 10, 6 ) ) );
		assertTrue( Habitat.groundFor( Kind.FISH, Place.CAVES, map, W, H, at( 11, 7 ) ) );
		assertFalse( Habitat.groundFor( Kind.STRIDER, Place.SEWERS, map, W, H, at( 9, 5 ) ) );

		//snails on the sewers' moss only
		assertTrue( Habitat.groundFor( Kind.SNAIL, Place.SEWERS, map, W, H, at( 2, 8 ) ) );
		assertFalse( Habitat.groundFor( Kind.SNAIL, Place.SEWERS, map, W, H, at( 3, 8 ) ) );

		//a moth in a place with no light rests on a plain face; where there are lights it keeps to them
		assertTrue( Habitat.groundFor( Kind.MOTH, Place.CATACOMB, map, W, H, at( 4, 4 ) ) );
		assertFalse( Habitat.groundFor( Kind.MOTH, Place.CATACOMB, map, W, H, at( 6, 4 ) ) );
		assertFalse( Habitat.groundFor( Kind.MOTH, Place.CATACOMB, map, W, H, at( 4, 6 ) ) );
		assertFalse( Habitat.groundFor( Kind.MOTH, Place.PRISON, map, W, H, at( 4, 4 ) ) );
		assertTrue( Habitat.groundFor( Kind.MOTH, Place.PRISON, map, W, H, at( 6, 3 ) ) );

		//the spider hangs over a cell under a plain face, never a torch's, never a doorway
		assertTrue( Habitat.groundFor( Kind.SPIDER, Place.PRISON, map, W, H, at( 4, 4 ) ) );
		assertFalse( Habitat.groundFor( Kind.SPIDER, Place.PRISON, map, W, H, at( 4, 5 ) ) );
		assertFalse( Habitat.groundFor( Kind.SPIDER, Place.PRISON, map, W, H, at( 6, 4 ) ) );
		set( 7, 4, Terrain.DOOR );
		assertFalse( Habitat.groundFor( Kind.SPIDER, Place.PRISON, map, W, H, at( 7, 4 ) ) );
		//over water at the wall's foot it may
		set( 5, 4, Terrain.WATER );
		assertTrue( Habitat.groundFor( Kind.SPIDER, Place.NEST, map, W, H, at( 5, 4 ) ) );
	}

	@Test
	public void theCitysTheHallsAndTheNestsOwn(){
		fill( 1, 1, 14, 10, Terrain.EMPTY );
		set( 3, 0, Terrain.WALL_DECO );
		set( 6, 0, Terrain.BOOKSHELF );
		set( 9, 5, Terrain.HIGH_GRASS );
		set( 9, 7, Terrain.GRASS );
		//lizards under the vents, silverfish under the shelves, butterflies over the flowers
		assertTrue( Habitat.groundFor( Kind.LIZARD, Place.CITY, map, W, H, at( 3, 1 ) ) );
		assertFalse( Habitat.groundFor( Kind.LIZARD, Place.CITY, map, W, H, at( 4, 1 ) ) );
		assertTrue( Habitat.groundFor( Kind.SILVERFISH, Place.CITY, map, W, H, at( 6, 1 ) ) );
		assertFalse( Habitat.groundFor( Kind.SILVERFISH, Place.CITY, map, W, H, at( 4, 1 ) ) );
		assertTrue( Habitat.groundFor( Kind.BUTTERFLY, Place.CITY, map, W, H, at( 9, 5 ) ) );
		assertFalse( Habitat.groundFor( Kind.BUTTERFLY, Place.CITY, map, W, H, at( 9, 7 ) ) );
		assertTrue( Habitat.groundFor( Kind.BUTTERFLY, Place.MEADOW, map, W, H, at( 9, 7 ) ) );
		//the halls' ember beetles on the embermoss; the nest's spiderlings and flies on its webbing
		assertTrue( Habitat.groundFor( Kind.EMBER_BEETLE, Place.HALLS, map, W, H, at( 9, 7 ) ) );
		assertFalse( Habitat.groundFor( Kind.EMBER_BEETLE, Place.HALLS, map, W, H, at( 8, 7 ) ) );
		assertTrue( Habitat.groundFor( Kind.SPIDERLING, Place.NEST, map, W, H, at( 9, 7 ) ) );
		assertTrue( Habitat.groundFor( Kind.FLY, Place.NEST, map, W, H, at( 9, 7 ) ) );
		assertFalse( Habitat.groundFor( Kind.FLY, Place.NEST, map, W, H, at( 9, 6 ) ) );
		//the caves' beetles by the scaffolds (either of the two), the mines' by the rock
		set( 12, 8, Terrain.REGION_DECO );
		assertTrue( Habitat.groundFor( Kind.BEETLE, Place.CAVES, map, W, H, at( 11, 8 ) ) );
		assertFalse( Habitat.groundFor( Kind.BEETLE, Place.CAVES, map, W, H, at( 4, 1 ) ) );
		set( 3, 6, Terrain.REGION_DECO_ALT );
		assertTrue( Habitat.groundFor( Kind.BEETLE, Place.CAVES, map, W, H, at( 4, 6 ) ) );
		assertTrue( Habitat.groundFor( Kind.BEETLE, Place.MINES, map, W, H, at( 4, 1 ) ) );
		assertFalse( Habitat.groundFor( Kind.BEETLE, Place.MINES, map, W, H, at( 5, 5 ) ) );
	}

	@Test
	public void flocksComeDownInTheOpenAndHaresSitAwayFromTheWalls(){
		fill( 1, 1, 14, 10, Terrain.EMPTY );
		fill( 6, 6, 3, 3, Terrain.FROZEN_WATER );
		//open ground: nothing that stands as a wall in the three by three round it
		assertTrue( Habitat.groundFor( Kind.FINCH, Place.MEADOW, map, W, H, at( 4, 4 ) ) );
		assertFalse( Habitat.groundFor( Kind.FINCH, Place.MEADOW, map, W, H, at( 1, 2 ) ) );
		assertFalse( Habitat.groundFor( Kind.GULL, Place.SHORE, map, W, H, at( 1, 5 ) ) );
		//buntings come down on the ice; finches do not
		assertTrue( Habitat.groundFor( Kind.BUNTING, Place.FROZEN, map, W, H, at( 7, 7 ) ) );
		assertFalse( Habitat.groundFor( Kind.FINCH, Place.MEADOW, map, W, H, at( 7, 7 ) ) );
		//a hare sits on the floor or the ice, not under a wall's face
		assertTrue( Habitat.groundFor( Kind.HARE, Place.FROZEN, map, W, H, at( 7, 7 ) ) );
		assertTrue( Habitat.groundFor( Kind.HARE, Place.MEADOW, map, W, H, at( 4, 4 ) ) );
		assertFalse( Habitat.groundFor( Kind.HARE, Place.MEADOW, map, W, H, at( 4, 1 ) ) );
	}

	@Test
	public void theAirOfEachPlace(){
		fill( 1, 1, 14, 10, Terrain.EMPTY );
		fill( 8, 4, 3, 3, Terrain.WATER );
		set( 2, 5, Terrain.EMPTY_DECO );
		set( 4, 6, Terrain.HIGH_GRASS );
		set( 5, 6, Terrain.GRASS );
		set( 6, 6, Terrain.EMBERS );
		assertTrue( Habitat.airFor( Air.GNATS, Place.SEWERS, map, W, H, at( 9, 5 ) ) );
		assertFalse( Habitat.airFor( Air.GNATS, Place.SEWERS, map, W, H, at( 3, 5 ) ) );
		//the sewers drip into their water only; the caves into their pools and by their rock
		assertTrue( Habitat.airFor( Air.DRIPS, Place.SEWERS, map, W, H, at( 9, 5 ) ) );
		assertFalse( Habitat.airFor( Air.DRIPS, Place.SEWERS, map, W, H, at( 4, 1 ) ) );
		assertTrue( Habitat.airFor( Air.DRIPS, Place.CAVES, map, W, H, at( 4, 1 ) ) );
		assertFalse( Habitat.airFor( Air.DRIPS, Place.CAVES, map, W, H, at( 4, 3 ) ) );
		//flies over the prison's stains and the catacombs' moss; the halls' about their pillars of
		//skulls (and the remains lying about, heaps)
		assertTrue( Habitat.airFor( Air.FLIES, Place.PRISON, map, W, H, at( 2, 5 ) ) );
		assertTrue( Habitat.airFor( Air.FLIES, Place.CATACOMB, map, W, H, at( 2, 5 ) ) );
		assertFalse( Habitat.airFor( Air.FLIES, Place.HALLS, map, W, H, at( 2, 5 ) ) );
		set( 12, 3, Terrain.STATUE );
		set( 13, 3, Terrain.STATUE_SP );
		assertTrue( Habitat.airFor( Air.FLIES, Place.HALLS, map, W, H, at( 12, 3 ) ) );
		assertTrue( Habitat.airFor( Air.FLIES, Place.HALLS, map, W, H, at( 13, 3 ) ) );
		//a statue elsewhere is only stone
		assertFalse( Habitat.airFor( Air.FLIES, Place.PRISON, map, W, H, at( 12, 3 ) ) );
		assertFalse( Habitat.airFor( Air.FLIES, Place.CATACOMB, map, W, H, at( 12, 3 ) ) );
		assertTrue( Habitat.airFor( Air.SPORES, Place.CAVES, map, W, H, at( 4, 6 ) ) );
		assertFalse( Habitat.airFor( Air.SPORES, Place.CAVES, map, W, H, at( 5, 6 ) ) );
		assertTrue( Habitat.airFor( Air.EMBERS, Place.HALLS, map, W, H, at( 5, 6 ) ) );
		assertTrue( Habitat.airFor( Air.EMBERS, Place.HALLS, map, W, H, at( 6, 6 ) ) );
		assertFalse( Habitat.airFor( Air.EMBERS, Place.HALLS, map, W, H, at( 7, 6 ) ) );
		assertTrue( Habitat.airFor( Air.SILK, Place.NEST, map, W, H, at( 4, 6 ) ) );
		assertTrue( Habitat.airFor( Air.SILK, Place.NEST, map, W, H, at( 2, 5 ) ) );
		assertTrue( Habitat.airFor( Air.FIREFLIES, Place.MEADOW, map, W, H, at( 5, 6 ) ) );
		assertFalse( Habitat.airFor( Air.FIREFLIES, Place.MEADOW, map, W, H, at( 7, 6 ) ) );
		//dust hangs in a room's still air, not in a passage
		assertTrue( Habitat.airFor( Air.DUST, Place.TEMPLE, map, W, H, at( 5, 3 ) ) );
		fill( 1, 1, 14, 10, Terrain.WALL );
		fill( 2, 5, 10, 1, Terrain.EMPTY );
		assertFalse( Habitat.airFor( Air.DUST, Place.TEMPLE, map, W, H, at( 5, 5 ) ) );
		//glow-worms on a face over the caves' moss
		set( 5, 5, Terrain.GRASS );
		assertTrue( Habitat.airFor( Air.GLOW_WORMS, Place.CAVES, map, W, H, at( 5, 4 ) ) );
		assertFalse( Habitat.airFor( Air.GLOW_WORMS, Place.CAVES, map, W, H, at( 9, 4 ) ) );
	}

	@Test
	public void nothingOnTheMapsRim(){
		Arrays.fill( map, Terrain.EMPTY );
		for (Kind k : Kind.values()){
			for (Place p : Place.values()){
				assertFalse( Habitat.groundFor( k, p, map, W, H, at( 0, 5 ) ) );
				assertFalse( Habitat.groundFor( k, p, map, W, H, at( 5, 0 ) ) );
				assertFalse( Habitat.groundFor( k, p, map, W, H, at( W - 1, 5 ) ) );
				assertFalse( Habitat.groundFor( k, p, map, W, H, at( 5, H - 1 ) ) );
			}
		}
	}

	// ------------------------------------------------------------ the ways

	@Test
	public void theLaneOfAWallsFoot(){
		fill( 2, 2, 6, 5, Terrain.EMPTY );
		assertEquals( 2 * 16 + 4f, Habitat.laneY( map, W, at( 4, 2 ) ), 0f );
		assertEquals( 4 * 16 + 11f, Habitat.laneY( map, W, at( 4, 4 ) ), 0f );
	}

	@Test
	public void aScaredRoachRunsAlongTheWallAwayFromTheFright(){
		fill( 1, 1, 14, 8, Terrain.EMPTY );
		//from two cells to its west
		int[] way = Habitat.flee( Kind.ROACH, Place.SEWERS, map, W, H, at( 6, 1 ), 4.5f, 1.5f );
		assertEquals( 4, way.length );
		int last = at( 6, 1 );
		for (int c : way){
			assertTrue( "it went back", c % W > last % W );
			assertEquals( "it left the wall's foot", 1, c / W );
			last = c;
		}
	}

	@Test
	public void aFlightNeverCrossesRockOrWaterNorCutsACorner(){
		Random rng = new Random( 7 );
		for (int n = 0; n < 400; n++){
			Arrays.fill( map, Terrain.WALL );
			fill( 1, 1, W - 2, H - 2, Terrain.EMPTY );
			for (int k = 0; k < 30; k++){
				int x = 1 + rng.nextInt( W - 2 ), y = 1 + rng.nextInt( H - 2 );
				set( x, y, rng.nextBoolean() ? Terrain.WALL : Terrain.WATER );
			}
			int start = at( 1 + rng.nextInt( W - 2 ), 1 + rng.nextInt( H - 2 ) );
			if (!Habitat.stand( map, W, start )) continue;
			float sx = 1 + rng.nextFloat() * (W - 2), sy = 1 + rng.nextFloat() * (H - 2);
			for (Kind k : new Kind[]{ Kind.ROACH, Kind.MOUSE, Kind.NEWT, Kind.SPIDERLING, Kind.LIZARD }){
				int[] way = Habitat.flee( k, Place.SEWERS, map, W, H, start, sx, sy );
				int from = start;
				float before = dist2( from, sx, sy );
				for (int c : way){
					assertTrue( "onto rock or water", Habitat.stand( map, W, c ) );
					int dx = c % W - from % W, dy = c / W - from / W;
					assertTrue( "not a step", Math.abs( dx ) <= 1 && Math.abs( dy ) <= 1 && (dx != 0 || dy != 0) );
					if (dx != 0 && dy != 0){
						assertTrue( "cut a corner", Habitat.stand( map, W, from + dx ) && Habitat.stand( map, W, from + dy * W ) );
					}
					float after = dist2( c, sx, sy );
					assertTrue( "it ran toward the fright", after > before );
					before = after;
					from = c;
				}
			}
		}
	}

	private static float dist2( int cell, float sx, float sy ){
		float x = cell % W + 0.5f - sx, y = cell / W + 0.5f - sy;
		return x * x + y * y;
	}

	@Test
	public void hemmedInItGoesNowhere(){
		set( 5, 5, Terrain.EMPTY );
		assertEquals( 0, Habitat.flee( Kind.MOUSE, Place.PRISON, map, W, H, at( 5, 5 ), 4.5f, 5.5f ).length );
		assertEquals( 0, Habitat.wander( Kind.MOUSE, Place.PRISON, map, W, H, at( 5, 5 ), new Random( 1 ) ).length );
	}

	@Test
	public void aWandererKeepsToItsHaunt(){
		fill( 1, 1, 14, 8, Terrain.EMPTY );
		fill( 3, 5, 4, 3, Terrain.GRASS );
		Random rng = new Random( 3 );
		for (int n = 0; n < 200; n++){
			for (int c : Habitat.wander( Kind.ROACH, Place.PRISON, map, W, H, at( 6, 1 ), rng )){
				assertEquals( "off the wall's foot", 1, c / W );
			}
			for (int c : Habitat.wander( Kind.SPIDERLING, Place.NEST, map, W, H, at( 4, 6 ), rng )){
				assertEquals( "off the webbing", Terrain.GRASS, map[c] );
			}
			for (int c : Habitat.wander( Kind.MOUSE, Place.PRISON, map, W, H, at( 1, 1 ), rng )){
				assertTrue( "away from the walls", Habitat.wallBase( map, W, c ) || Habitat.wallSide( map, W, c ) );
			}
		}
		//a roach's wander runs the row, never down it
		int[] way = Habitat.along( at( 6, 1 ), 1, 3, c -> Habitat.stand( map, W, c ) && Habitat.wallBase( map, W, c ) );
		assertEquals( 3, way.length );
		assertEquals( at( 9, 1 ), way[2] );
	}

	@Test
	public void aHaresDashNeverCrossesWallsOrWaterAndEndsInTheCoverItRanFor(){
		Random rng = new Random( 11 );
		int intoCover = 0;
		for (int n = 0; n < 600; n++){
			Arrays.fill( map, Terrain.WALL );
			fill( 1, 1, W - 2, H - 2, Terrain.EMPTY );
			for (int k = 0; k < 25; k++){
				int x = 1 + rng.nextInt( W - 2 ), y = 1 + rng.nextInt( H - 2 );
				int r = rng.nextInt( 4 );
				set( x, y, r == 0 ? Terrain.WALL : r == 1 ? Terrain.WATER : r == 2 ? Terrain.HIGH_GRASS : Terrain.FROZEN_WATER );
			}
			int start = at( 1 + rng.nextInt( W - 2 ), 1 + rng.nextInt( H - 2 ) );
			if (!Habitat.runnable( map[start] )) continue;
			float sx = 1 + rng.nextFloat() * (W - 2), sy = 1 + rng.nextFloat() * (H - 2);
			Habitat.Dash d = Habitat.dash( map, W, H, start, sx, sy, rng );
			assertTrue( d.pts.length >= 2 && d.pts.length % 2 == 0 );
			float lx = start % W + 0.5f, ly = start / W + 0.5f;
			if (d.pts.length > 2 || Habitat.legRunnable( map, W, H, lx, ly, d.pts[0], d.pts[1] )){
				for (int i = 0; i + 1 < d.pts.length; i += 2){
					assertTrue( "a leg crossed something", Habitat.legRunnable( map, W, H, lx, ly, d.pts[i], d.pts[i+1] ) );
					lx = d.pts[i];
					ly = d.pts[i+1];
				}
			}
			if (d.cover){
				intoCover++;
				int end = (int)Math.floor( lx ) + (int)Math.floor( ly ) * W;
				assertTrue( "ran for cover and ended in the open", Habitat.cover( map[end] ) );
			}
		}
		assertTrue( "never once ran for cover", intoCover > 0 );
	}

	@Test
	public void onTheLineHeWalks(){
		assertTrue( Habitat.nearSegment( 5f, 5.5f, 0f, 5f, 10f, 5f, 1f ) );
		assertFalse( Habitat.nearSegment( 5f, 7f, 0f, 5f, 10f, 5f, 1f ) );
		//past the segment's end it is measured from the end
		assertFalse( Habitat.nearSegment( 12f, 5f, 0f, 5f, 10f, 5f, 1.5f ) );
		assertTrue( Habitat.nearSegment( 0.5f, 0.5f, 0f, 0f, 0f, 0f, 1f ) );
	}
}
