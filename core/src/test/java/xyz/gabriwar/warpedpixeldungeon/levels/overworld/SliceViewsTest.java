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

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.debug.ViewScenes;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The views of the other bands: what a cave's pit shows of the slices below, and the shores the
 * open water of every view wears (WindowGenerator.belowLayers, viewShores, the rock's tops).
 */
public class SliceViewsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	private static final float SUMMER = WorldModel.calendarShift( GameCalendar.Season.SUMMER, 0.5f );
	private static final int[][] ORIGINS = { {-1200, 800}, {-2000, -2000}, {400, -1600}, {1600, 400} };

	private static boolean water( int t ){
		return t == Terrain.WATER || t == Terrain.DEEP_WATER;
	}

	private static boolean interior( int c ){
		int x = c % W, y = c / W;
		return x > 0 && y > 0 && x < W - 1 && y < H - 1;
	}

	private static boolean way( int t ){
		return t == Terrain.ENTRANCE || t == Terrain.EXIT;
	}

	// ---------------------------------------------------------------- the pits

	//every pit shows the floor of the slice below as that slice lies there (a ladder's cell its own
	//ground), through a pit of that slice too one more slice down; nothing else in a cave looks down
	@Test
	public void aPitShowsTheSliceBelowAsItLies(){
		int pits = 0, deeper = 0, pools = 0, boulders = 0;
		for (int[] o : ORIGINS){
			WindowGenerator.Window[] slice = new WindowGenerator.Window[WorldLayers.MAX_BELOW + 1];
			for (int k = 1; k <= WorldLayers.MAX_BELOW; k++) slice[k] = WindowGenerator.generate( SEED, -k, o[0], o[1], 0f );
			for (int k = 1; k <= WorldLayers.MAX_BELOW; k++){
				WindowGenerator.Window w = slice[k];
				assertNotNull( "slice -" + k + " has no view down", w.below );
				for (int c = 0; c < W * H; c++){
					boolean pit = interior( c ) && w.terrain[c] == Terrain.CHASM;
					if (!pit){
						assertEquals( "a view down off a pit at -" + k + " cell " + c, -1, w.below[c] );
						continue;
					}
					pits++;
					int down = w.belowDepth[c];
					assertTrue( "a pit at the deepest slice", k < WorldLayers.MAX_BELOW );
					assertTrue( down >= 1 && k + down <= WorldLayers.MAX_BELOW );
					//what it shows lies `down` slices below, and every slice between has a pit there too
					for (int j = 1; j < down; j++) assertEquals( Terrain.CHASM, slice[k + j].terrain[c] );
					int t = slice[k + down].terrain[c];
					assertFalse( "a pit over rock at -" + k + " cell " + c, t == Terrain.WALL || t == Terrain.CHASM );
					if (down > 1) deeper++;
					if (water( t )) pools++;
					if (t == Terrain.BOULDER) boulders++;
					if (way( t )) continue;
					//seen from afar, the caves' floor whatever lies on it (a boulder's too), or its water
					assertEquals( "the floor under the pit at -" + k + " cell " + c,
							water( t ) ? DungeonTileSheet.BELOW_WATER : DungeonTileSheet.FLOOR, w.below[c] );
					//(the window below tiers its pools into deep water; seen from above, water is water)
					assertEquals( water( t ) ? Terrain.WATER : t, w.belowTerrain[c] );
				}
			}
		}
		System.out.println( "[views] " + pits + " pits, " + deeper + " over a pit of the slice below, " + pools + " over a pool, "
				+ boulders + " over a boulder" );
		assertTrue( "no pits in the sample", pits > 20 );
	}

	//pitAt is caves()'s second pass for one cell: it says pit exactly where the window has one
	@Test
	public void pitAtAgreesWithTheWindow(){
		for (int k = 1; k <= WorldLayers.MAX_BELOW; k++){
			WindowGenerator.Window w = WindowGenerator.generate( SEED, -k, ORIGINS[0][0], ORIGINS[0][1], 0f );
			for (int c = 0; c < W * H; c++){
				if (!interior( c )) continue;
				int wx = ORIGINS[0][0] + c % W, wy = ORIGINS[0][1] + c / W;
				assertEquals( "slice -" + k + " cell " + c, w.terrain[c] == Terrain.CHASM, WindowGenerator.pitAt( SEED, -k, wx, wy ) );
			}
		}
	}

	//a pit's view lies in the layer of its depth, shaded from every rim round it
	@Test
	public void aPitIsShadedFromItsRims(){
		int pits = 0;
		for (int k = 1; k < WorldLayers.MAX_BELOW; k++){
			WindowGenerator.Window w = WindowGenerator.generate( SEED, -k, ORIGINS[0][0], ORIGINS[0][1], 0f );
			WindowGenerator.Views v = WindowGenerator.belowLayers( SEED, ORIGINS[0][0], ORIGINS[0][1], w );
			assertNotNull( v );
			for (int c = 0; c < W * H; c++){
				int g = w.below[c] == -1 ? -1 : Math.min( WindowGenerator.BELOW_LAYERS, w.belowDepth[c] ) - 1;
				for (int d = 0; d < v.depth.length; d++){
					assertEquals( "cell " + c, d == g ? w.below[c] : -1, v.depth[d].tiles[c] );
				}
				if (g < 0){
					assertEquals( -1, v.shade[c] );
					continue;
				}
				pits++;
				int mask = 0, x = c % W, y = c / W;
				int[] dx = { 0, 1, 1, 1, 0, -1, -1, -1 }, dy = { -1, -1, 0, 1, 1, 1, 0, -1 };
				for (int i = 0; i < 8; i++){
					int n = x + dx[i] + (y + dy[i]) * W;
					if (w.below[n] == -1 || Math.min( WindowGenerator.BELOW_LAYERS, w.belowDepth[n] ) - 1 < g) mask |= 1 << i;
				}
				assertEquals( DungeonTileSheet.belowShade( mask ), v.shade[c] );
			}
		}
		assertTrue( pits > 0 );
	}

	// ---------------------------------------------------------------- the shores

	private static int ground( int viewTile ){
		return viewTile == DungeonTileSheet.DIRT_PATH_TILE ? 1 : viewTile == DungeonTileSheet.FLOOR_SP ? 2
				: viewTile == DungeonTileSheet.SNOW_TILE ? 3 : 0;
	}

	//what a water cell of a view must wear: its banks are the land of the view on its sides, each in
	//the ground the view shows there
	private static int expectedShore( int c, int[] terrain, int[] shown, boolean caves ){
		int[] side = { c - W, c + 1, c + W, c - 1 };
		int mask = 0;
		for (int i = 0; i < 4; i++){
			if (terrain[side[i]] >= 0 && DungeonTileSheet.worldBank( terrain[side[i]] )) mask |= 1 << i;
		}
		if (mask == 0) return -1;
		if (caves) return DungeonTileSheet.WATER + mask;
		return DungeonTileSheet.overworldShore( mask, ground( shown[side[0]] ), ground( shown[side[1]] ),
				ground( shown[side[2]] ), ground( shown[side[3]] ) );
	}

	//the rule on a hand-laid view: every bank in its own ground, nothing toward ice or no view, and the
	//caves' own stitch below the surface
	@Test
	public void viewShoresFollowTheBanks(){
		int n = W * H, c = 40 + 40 * W;
		int[] at = new int[n], shown = new int[n], out = new int[n];
		Arrays.fill( at, -1 );
		Arrays.fill( shown, -1 );
		Arrays.fill( out, -1 );
		at[c] = Terrain.WATER;                 shown[c] = DungeonTileSheet.BELOW_WATER;
		at[c - W] = Terrain.SNOW;              shown[c - W] = DungeonTileSheet.SNOW_TILE;
		at[c + 1] = Terrain.EMPTY_SP;          shown[c + 1] = DungeonTileSheet.FLOOR_SP;
		at[c + W] = Terrain.GRASS;             shown[c + W] = DungeonTileSheet.SNOW_TILE;   //frozen grass shows snow
		at[c - 1] = Terrain.FROZEN_WATER;      shown[c - 1] = DungeonTileSheet.FROZEN_WATER;
		WindowGenerator.viewShores( at, at, shown, false, out );
		assertEquals( DungeonTileSheet.overworldShore( 1 | 2 | 4, 3, 2, 3, 0 ), out[c] );
		for (int i = 0; i < n; i++) if (i != c) assertEquals( -1, out[i] );

		//the ice gone from the view: no bank there either; in the caves the caves' water stitch
		at[c - 1] = -1;
		Arrays.fill( out, -1 );
		WindowGenerator.viewShores( at, at, shown, true, out );
		assertEquals( DungeonTileSheet.WATER + (1 | 2 | 4), out[c] );

		//open water all round wears nothing
		for (int s : new int[]{ c - W, c + 1, c + W, c - 1 }) at[s] = Terrain.WATER;
		Arrays.fill( out, -1 );
		WindowGenerator.viewShores( at, at, shown, false, out );
		assertEquals( -1, out[c] );
	}

	//the view down from a mountain: its water wears the surface's shores against the land of its own
	//depth, never against a nearer ledge or a deeper drop
	@Test
	public void waterBelowAMountainHasShores(){
		int shores = 0, water = 0;
		int[] spot = ViewScenes.waterBelow( SEED, 1, SUMMER );
		assertNotNull( spot );
		int[][] origins = { { spot[0] - W / 2, spot[1] - H / 2 }, ORIGINS[1], ORIGINS[2] };
		for (int alt = 1; alt <= 3; alt++){
			for (int[] o : origins){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], SUMMER );
				WindowGenerator.Views v = WindowGenerator.belowLayers( SEED, o[0], o[1], w );
				for (WindowGenerator.View layer : v.depth){
					int[] vt = new int[W * H];
					Arrays.fill( vt, -1 );
					for (int c = 0; c < W * H; c++) if (layer.tiles[c] != -1) vt[c] = w.belowTerrain[c];
					for (int c = 0; c < W * H; c++){
						if (!interior( c ) || !water( vt[c] )){
							assertEquals( "a shore off the water at " + c, -1, layer.shores[c] );
							continue;
						}
						water++;
						int want = expectedShore( c, vt, layer.tiles, false );
						assertEquals( "alt " + alt + " cell " + c, want, layer.shores[c] );
						if (want != -1) shores++;
					}
				}
			}
		}
		System.out.println( "[views] water below the mountains: " + water + " cells, " + shores + " with a shore" );
		assertTrue( "no shores below the mountains", shores > 50 );
	}

	//a tarn on the rock's top wears the surface's shores too, against every top round it (the front
	//row's ground as well); only the tops that are drawn wear them
	@Test
	public void tarnsOnTheTopsHaveShores(){
		int shores = 0;
		int[] spot = ViewScenes.tarnAbove( SEED, 0, SUMMER );
		assertNotNull( spot );
		int[][] cases = { { 0, spot[0] - W / 2, spot[1] - H / 2 }, { 1, ORIGINS[0][0], ORIGINS[0][1] },
				{ 2, ORIGINS[2][0], ORIGINS[2][1] }, { 1, ORIGINS[3][0], ORIGINS[3][1] } };
		for (int[] k : cases){
			WindowGenerator.Window w = WindowGenerator.generate( SEED, k[0], k[1], k[2], SUMMER );
			//(the row over the window's ring is left out: the ring turns to wall after the tops are dressed)
			for (int c = 0; c < W * (H - 2); c++){
				boolean drawn = interior( c ) && w.top[c] != -1 && DungeonTileSheet.wallStitcheable( w.terrain[c + W] );
				if (!drawn || !water( w.topTerrain[c] )){
					assertEquals( "a shore on no tarn at " + c, -1, w.topShore[c] );
					continue;
				}
				int want = expectedShore( c, w.topTerrain, w.top, false );
				assertEquals( "slice " + k[0] + " cell " + c, want, w.topShore[c] );
				if (want != -1) shores++;
			}
		}
		assertTrue( "no tarn's shore on the tops", shores > 10 );
	}

	// ---------------------------------------------------------------- one world

	//two windows over the same ground draw the same views on it: every part is a pure function of
	//the world cell and the cells round it
	@Test
	public void overlappingWindowsAgree(){
		int[][] cases = { { 2, ORIGINS[1][0], ORIGINS[1][1] }, { -3, ORIGINS[0][0], ORIGINS[0][1] }, { 0, ORIGINS[2][0], ORIGINS[2][1] } };
		for (int[] k : cases){
			int alt = k[0], ox = k[1], oy = k[2], dx = 32, dy = 64;
			WindowGenerator.Window a = WindowGenerator.generate( SEED, alt, ox, oy, SUMMER );
			WindowGenerator.Window b = WindowGenerator.generate( SEED, alt, ox + dx, oy + dy, SUMMER );
			WindowGenerator.Views va = WindowGenerator.belowLayers( SEED, ox, oy, a );
			WindowGenerator.Views vb = WindowGenerator.belowLayers( SEED, ox + dx, oy + dy, b );
			for (int y = dy + 3; y < H - 3; y++){
				for (int x = dx + 3; x < W - 3; x++){
					int ca = x + y * W, cb = (x - dx) + (y - dy) * W;
					String at = "slice " + alt + " world " + (ox + x) + "," + (oy + y);
					if (va != null){
						for (int d = 0; d < va.depth.length; d++){
							assertEquals( at, va.depth[d].tiles[ca], vb.depth[d].tiles[cb] );
							assertEquals( at, va.depth[d].shores[ca], vb.depth[d].shores[cb] );
							assertEquals( at, va.depth[d].blends[ca], vb.depth[d].blends[cb] );
							assertEquals( at, va.depth[d].corners[ca], vb.depth[d].corners[cb] );
						}
						assertEquals( at, va.shade[ca], vb.shade[cb] );
					}
					//the surface's tops (its structures are the world's, read cell by cell)
					if (alt == 0){
						assertEquals( at, a.topShore[ca], b.topShore[cb] );
					}
				}
			}
			//and the same window twice is the same window
			WindowGenerator.Window again = WindowGenerator.generate( SEED, alt, ox, oy, SUMMER );
			WindowGenerator.Views vAgain = WindowGenerator.belowLayers( SEED, ox, oy, again );
			if (va != null){
				for (int d = 0; d < va.depth.length; d++) assertTrue( Arrays.equals( va.depth[d].shores, vAgain.depth[d].shores ) );
			}
			if (a.top != null) assertTrue( Arrays.equals( a.topShore, again.topShore ) );
		}
	}

	// ---------------------------------------------------------------- the layer

	//a part of a view with nothing to draw says so (GameScene puts up no tilemap for it until it has)
	@Test
	public void anEmptyPartIsKnownAsItIsHandedOver(){
		SliceGroundLayer l = new SliceGroundLayer( 0, SliceGroundLayer.SHORES, xyz.gabriwar.warpedpixeldungeon.Assets.Environment.TILES_CAVES );
		assertTrue( l.empty() );
		int[] data = new int[W * H];
		Arrays.fill( data, -1 );
		l.setData( data );
		assertTrue( l.empty() );
		int[] some = data.clone();
		some[W * H - 1] = DungeonTileSheet.WATER + 5;
		l.setData( some );
		assertFalse( l.empty() );
		l.setData( data );
		assertTrue( l.empty() );
		l.setCell( 10, DungeonTileSheet.WATER );
		assertFalse( l.empty() );
	}
}
