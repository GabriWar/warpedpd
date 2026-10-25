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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The desert's places (Drylands): where they stand, that they move nothing that stood before them,
 * that the world lays them the same every time, and what they are made of.
 */
public class DrylandsTest {

	private static final long[] SEEDS = { 0xBA77011L, 12345L, 987654321L };
	private static final int SPAN = 30;

	//every desert place of a seed over the sectors -SPAN..SPAN
	private static ArrayList<Drylands.Place> placesOf( long seed ){
		ArrayList<Drylands.Place> out = new ArrayList<>();
		for (int sy = -SPAN; sy <= SPAN; sy++){
			for (int sx = -SPAN; sx <= SPAN; sx++){
				WorldStructures.Site t = WorldStructures.siteType( seed, sx, sy );
				if (t == WorldStructures.Site.OASIS || t == WorldStructures.Site.BURIED_TEMPLE){
					out.add( new Drylands.Place( t, sx, sy, WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ) ) );
				}
			}
		}
		return out;
	}

	//a desert place only where the cascade left a sector empty; every site the cascade lays is kept
	@Test
	public void theyTakeOnlyWhatNothingElseClaimed(){
		int oases = 0, temples = 0;
		for (long seed : SEEDS){
			for (int sy = -SPAN; sy <= SPAN; sy++){
				for (int sx = -SPAN; sx <= SPAN; sx++){
					WorldStructures.Site base = WorldStructures.baseType( seed, sx, sy );
					WorldStructures.Site site = WorldStructures.siteType( seed, sx, sy );
					if (base != WorldStructures.Site.NONE){
						assertEquals( "sector " + sx + "," + sy, base, site );
						continue;
					}
					//the wetlands' places come before the desert's, the sea's after them
					if (WetlandSites.isWetland( site ) || WorldStructures.seaSite( site )) continue;
					if (site == WorldStructures.Site.OASIS) oases++;
					if (site == WorldStructures.Site.BURIED_TEMPLE) temples++;
					if (site != WorldStructures.Site.NONE){
						assertEquals( Drylands.kindRolled( seed, sx, sy ), site );
						assertEquals( WorldModel.Biome.DESERT, WorldModel.baseBiomeAt( seed,
								WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ) ) );
					}
				}
			}
		}
		assertTrue( "oases " + oases, oases >= 6 );
		assertTrue( "temples " + temples, temples >= 6 );
	}

	//the same seed lays the same places, cell for cell, whatever was asked before
	@Test
	public void theWorldLaysThemTheSameEveryTime(){
		HashMap<Long, ArrayList<String>> first = new HashMap<>();
		for (long seed : SEEDS) first.put( seed, describe( seed ) );
		//another seed asked in between empties every cache
		for (int i = SEEDS.length - 1; i >= 0; i--){
			WorldStructures.siteType( SEEDS[i] ^ 0x5A5AL, 0, 0 );
			assertEquals( first.get( SEEDS[i] ), describe( SEEDS[i] ) );
		}
	}

	private static ArrayList<String> describe( long seed ){
		ArrayList<String> out = new ArrayList<>();
		for (Drylands.Place p : placesOf( seed )){
			StringBuilder sb = new StringBuilder( p.type + "@" + p.cx + "," + p.cy + ":" );
			int r = Drylands.reach( p.type );
			for (int y = p.cy - r; y <= p.cy + r; y++){
				for (int x = p.cx - r; x <= p.cx + r; x++) sb.append( Drylands.terrain( seed, p.sx, p.sy, p.type, x, y ) ).append( ' ' );
			}
			out.add( sb.toString() );
		}
		return out;
	}

	//their reach and a ring round it are desert ground of the surface, clear of every road and every
	//cell a cave mouth would open on; their own cells are what the world lays there
	@Test
	public void theyStandOnOpenDesertAwayFromRoadsAndMouths(){
		for (long seed : SEEDS){
			WorldModel.Sample smp = new WorldModel.Sample();
			for (Drylands.Place p : placesOf( seed )){
				int r = Drylands.reach( p.type ) + 1;
				for (int y = p.cy - r; y <= p.cy + r; y++){
					for (int x = p.cx - r; x <= p.cx + r; x++){
						WorldModel.sample( seed, x, y, 0f, smp );
						assertEquals( WorldModel.Biome.DESERT, smp.biome );
						assertEquals( 0, WorldLayers.band( smp.elev ) );
						assertFalse( "a road through " + p.type + " at " + x + "," + y, WorldStructures.onRoad( seed, x, y ) );
						assertFalse( "a cave mouth under " + p.type + " at " + x + "," + y, WindowGenerator.mouthCandidate( seed, x, y ) );
						int t = Drylands.terrain( seed, p.sx, p.sy, p.type, x, y );
						if (t != -1) assertEquals( t, WorldModel.terrainAt( seed, x, y ) );
					}
				}
			}
		}
	}

	//an oasis: water at its heart, its rug on the ring, palms that never stand side by side, and a way
	//in from the desert to its water and its trader
	@Test
	public void anOasisHasWaterPalmsARugAndAWayIn(){
		int checked = 0;
		for (long seed : SEEDS){
			for (Drylands.Place p : placesOf( seed )){
				if (p.type != WorldStructures.Site.OASIS) continue;
				checked++;
				assertEquals( Terrain.WATER, Drylands.terrain( seed, p.sx, p.sy, p.type, p.cx, p.cy ) );
				int[] rug = Drylands.rugCell( seed, p.sx, p.sy );
				assertEquals( Terrain.CUSTOM_DECO_EMPTY, Drylands.terrain( seed, p.sx, p.sy, p.type, rug[0], rug[1] ) );
				int palms = 0;
				int r = Drylands.OASIS_REACH;
				for (int y = p.cy - r; y <= p.cy + r; y++){
					for (int x = p.cx - r; x <= p.cx + r; x++){
						if (Drylands.terrain( seed, p.sx, p.sy, p.type, x, y ) != Terrain.CUSTOM_DECO) continue;
						palms++;
						for (int dy = -1; dy <= 1; dy++){
							for (int dx = -1; dx <= 1; dx++){
								if (dx == 0 && dy == 0) continue;
								assertFalse( "palms side by side at " + x + "," + y,
										Drylands.terrain( seed, p.sx, p.sy, p.type, x + dx, y + dy ) == Terrain.CUSTOM_DECO );
							}
						}
					}
				}
				assertTrue( "an oasis with " + palms + " palms", palms >= 2 );
				//from the approach to the water's edge and to the rug's side, over open ground
				int[] from = Drylands.approach( seed, p );
				assertTrue( "the water of the oasis at " + p.cx + "," + p.cy, reachable( seed, p, from,
						(x, y) -> WorldModel.terrainAt( seed, x + 1, y ) == Terrain.WATER || WorldModel.terrainAt( seed, x - 1, y ) == Terrain.WATER
								|| WorldModel.terrainAt( seed, x, y + 1 ) == Terrain.WATER || WorldModel.terrainAt( seed, x, y - 1 ) == Terrain.WATER ) );
				assertTrue( reachable( seed, p, from, (x, y) -> Math.max( Math.abs( x - rug[0] ), Math.abs( y - rug[1] ) ) <= 1 ) );
				//the shade covers the water and the ring, not the sand past them
				assertTrue( Drylands.shadeOf( seed, p.sx, p.sy, p.cx, p.cy ) );
				assertTrue( Drylands.shadeOf( seed, p.sx, p.sy, rug[0], rug[1] ) );
				assertFalse( Drylands.shadeOf( seed, p.sx, p.sy, p.cx, p.cy + r + 2 ) );
			}
		}
		assertTrue( checked > 0 );
	}

	//a buried temple's top: its stairwell at its heart (its own way, no slice's), obelisks and statues
	//standing, the rest slabs to walk on, the stairwell reached from the desert
	@Test
	public void aTempleTopHasItsStairwellAndItsStones(){
		int checked = 0;
		for (long seed : SEEDS){
			for (Drylands.Place p : placesOf( seed )){
				if (p.type != WorldStructures.Site.BURIED_TEMPLE) continue;
				checked++;
				assertEquals( Terrain.EXIT, Drylands.terrain( seed, p.sx, p.sy, p.type, p.cx, p.cy ) );
				assertTrue( Drylands.templeGate( seed, p.cx, p.cy ) );
				assertFalse( Drylands.templeGate( seed, p.cx + 1, p.cy ) );
				assertFalse( WorldStructures.delveGate( seed, p.cx, p.cy ) );
				assertEquals( Terrain.CUSTOM_DECO, Drylands.terrain( seed, p.sx, p.sy, p.type, p.cx - 3, p.cy - 2 ) );
				assertEquals( Terrain.CUSTOM_DECO, Drylands.terrain( seed, p.sx, p.sy, p.type, p.cx + 2, p.cy + 2 ) );
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (dx == 0 && dy == 0) continue;
						assertEquals( Terrain.CUSTOM_DECO_EMPTY, Drylands.terrain( seed, p.sx, p.sy, p.type, p.cx + dx, p.cy + dy ) );
					}
				}
				assertTrue( reachable( seed, p, Drylands.approach( seed, p ), (x, y) -> x == p.cx && y == p.cy ) );
				assertNotNull( Drylands.placeAt( seed, p.cx, p.cy ) );
				assertNull( Drylands.placeAt( seed, p.cx, p.cy + Drylands.TEMPLE_REACH + 2 ) );
			}
		}
		assertTrue( checked > 0 );
	}

	//can a hero walk from a cell to one the goal likes, over the world's dry open ground round a place?
	private static boolean reachable( long seed, Drylands.Place p, int[] from, java.util.function.BiPredicate<Integer, Integer> goal ){
		int r = Drylands.reach( p.type ) + 3;
		ArrayDeque<int[]> q = new ArrayDeque<>();
		HashSet<Long> seen = new HashSet<>();
		q.add( from );
		seen.add( key( from[0], from[1] ) );
		while (!q.isEmpty()){
			int[] c = q.poll();
			if (goal.test( c[0], c[1] )) return true;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int x = c[0] + dx, y = c[1] + dy;
					if (Math.abs( x - p.cx ) > r || Math.abs( y - p.cy ) > r || !seen.add( key( x, y ) )) continue;
					int t = WorldModel.terrainAt( seed, x, y );
					if ((Terrain.flags[t] & Terrain.PASSABLE) == 0 || t == Terrain.WATER) continue;
					q.add( new int[]{ x, y } );
				}
			}
		}
		return false;
	}

	private static long key( int x, int y ){
		return (((long) x) << 32) | (y & 0xFFFFFFFFL);
	}

	//the props: a palm's trunk on its cell and its crown over the cell above, the rug, the stairwell;
	//a cell changed in play shows no prop, and a window holding no place has none
	@Test
	public void thePropsFollowTheCellsTheyStandOn(){
		long seed = SEEDS[0];
		Drylands.Place oasis = null, temple = null;
		for (Drylands.Place p : placesOf( seed )){
			if (p.type == WorldStructures.Site.OASIS && oasis == null) oasis = p;
			if (p.type == WorldStructures.Site.BURIED_TEMPLE && temple == null) temple = p;
		}
		assertNotNull( oasis );
		assertNotNull( temple );
		for (Drylands.Place p : new Drylands.Place[]{ oasis, temple }){
			int w = 40, h = 40, ox = p.cx - 20, oy = p.cy - 20;
			int[] map = new int[w * h];
			for (int y = 0; y < h; y++){
				for (int x = 0; x < w; x++) map[x + y * w] = WorldModel.terrainAt( seed, ox + x, oy + y );
			}
			int[][] art = Drylands.art( seed, ox, oy, map, w, h );
			assertNotNull( art );
			int heart = 20 + 20 * w;
			if (p.type == WorldStructures.Site.BURIED_TEMPLE){
				assertEquals( Drylands.ART_STAIR, art[0][heart] );
			} else {
				int[] rug = Drylands.rugCell( seed, p.sx, p.sy );
				assertEquals( Drylands.ART_RUG, art[0][(rug[0] - ox) + (rug[1] - oy) * w] );
				for (int c = 0; c < map.length; c++){
					if (map[c] != Terrain.CUSTOM_DECO) continue;
					assertTrue( art[0][c] >= Drylands.ART_PALM_TRUNK && art[0][c] < Drylands.ART_PALM_TRUNK + 3 );
					assertEquals( art[0][c] - Drylands.ART_PALM_TRUNK + Drylands.ART_PALM_CROWN, art[1][c - w] );
					//felled (a debug edit, a bomb of the future): its prop goes with it
					int[] changed = map.clone();
					changed[c] = Terrain.EMPTY_SP;
					assertEquals( -1, Drylands.art( seed, ox, oy, changed, w, h )[0][c] );
				}
			}
		}
		assertNull( Drylands.art( seed, 100000, 100000, new int[16 * 16], 16, 16 ) );
	}

	//a temple's name, level and floors are the world's: the same each time, its floors 2 to 4
	@Test
	public void aTemplesMeasureIsTheWorlds(){
		for (int sy = -20; sy <= 20; sy++){
			for (int sx = -20; sx <= 20; sx++){
				int l = xyz.gabriwar.warpedpixeldungeon.levels.Delves.templeLevel( SEEDS[0], sx, sy );
				assertTrue( l >= 1 );
				assertEquals( l, xyz.gabriwar.warpedpixeldungeon.levels.Delves.templeLevel( SEEDS[0], sx, sy ) );
				int f = xyz.gabriwar.warpedpixeldungeon.levels.Delves.templeFloors( SEEDS[0], sx, sy );
				assertTrue( f >= 2 && f <= 4 );
				int region = xyz.gabriwar.warpedpixeldungeon.levels.Delves.templeRegion( SEEDS[0], sx, sy );
				assertTrue( region >= 0 && region <= 4 );
			}
		}
		assertArrayEquals( new int[]{ 0, 0 }, new int[]{ Drylands.reach( WorldStructures.Site.VILLAGE ), Drylands.reach( WorldStructures.Site.NONE ) } );
	}
}
