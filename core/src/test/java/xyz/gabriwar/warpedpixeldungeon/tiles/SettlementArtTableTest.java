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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SettlementLights;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The art table in SettlementLights against the art itself: the town's windows and
 * chimneys are where its authored layers put them, each light mask lies on glass (or on
 * the fire ring's logs), and the smoke leaves from a chimney's dark mouth.
 */
public class SettlementArtTableTest {

	private static final int[] WINDOW_TILES = { 7, 23, 82, 164 };

	@Test
	public void townWindowsAreTheArtsWindows(){
		HashSet<Integer> listed = new HashSet<>();
		for (int[] w : SettlementLights.TOWN_WINDOWS){
			int cell = w[0], kind = w[1];
			assertEquals( "cell " + cell, SettlementLights.KIND_SOURCE_TILE[kind], TownRemixedTiles.DECO[cell] );
			assertFalse( TownRemixedTiles.outdoor( cell ) );
			assertFalse( TownRemixedTiles.brokenNearMine( cell ) );
			//its building's door: a doorway of the town, a couple of cells off
			boolean door = false;
			for (int d : WorldStructures.TOWN_DOORS) door |= d == w[2];
			assertTrue( "door " + w[2], door );
			assertTrue( Math.abs( w[2] % 32 - cell % 32 ) <= 2 && Math.abs( w[2] / 32 - cell / 32 ) <= 2 );
			listed.add( cell );
		}
		for (int cell = 0; cell < TownRemixedTiles.DECO.length; cell++){
			for (int t : WINDOW_TILES){
				if (TownRemixedTiles.DECO[cell] == t) assertTrue( "unlisted window at " + cell, listed.contains( cell ) );
			}
		}
		assertTrue( listed.contains( SettlementLights.TOWN_INN_WINDOW ) );
		assertEquals( 275, SettlementLights.TOWN_INN_WINDOW );
		for (int[] w : SettlementLights.TOWN_WINDOWS){
			//the inn's window is the one whose door is the inn's
			assertEquals( w[0] == SettlementLights.TOWN_INN_WINDOW, w[2] == WorldStructures.TOWN_DOORS[6] );
		}
	}

	@Test
	public void townChimneysAreTheArtsChimneys(){
		HashSet<Integer> art = new HashSet<>(), table = new HashSet<>();
		for (int cell = 0; cell < TownRemixedTiles.ROOF_DECO.length; cell++){
			int t = TownRemixedTiles.ROOF_DECO[cell];
			if (t == 102 || t == 103) art.add( cell );
		}
		for (int c : SettlementLights.TOWN_CHIMNEYS) table.add( c );
		assertEquals( art, table );
		assertEquals( SettlementLights.TOWN_CHIMNEYS.length, table.size() );
		assertEquals( 212, SettlementLights.TOWN_INN_CHIMNEY );
		assertEquals( 103, TownRemixedTiles.ROOF_DECO[SettlementLights.TOWN_INN_CHIMNEY] );
	}

	@Test
	public void oneVariantPerWindowStyle(){
		assertEquals( 2, OverworldDress.VILLAGE_WINDOWS.length );
		assertEquals( OverworldDress.VILLAGE_WINDOWS.length, SettlementLights.VILLAGE_WINDOW_KINDS.length );
		assertEquals( SettlementLights.KIND_HEARTH + 1, SettlementLights.KIND_SOURCE_TILE.length );
		//the village windows are copies of the town's 7 and 23
		assertEquals( 7, SettlementLights.KIND_SOURCE_TILE[SettlementLights.VILLAGE_WINDOW_KINDS[0]] );
		assertEquals( 23, SettlementLights.KIND_SOURCE_TILE[SettlementLights.VILLAGE_WINDOW_KINDS[1]] );
	}

	private static int px( Pixmap p, int tile, int x, int y ){
		return p.getPixel( (tile % 16) * 16 + x, (tile / 16) * 16 + y );
	}

	private static int alpha( int rgba ){ return rgba & 0xFF; }

	private static float lum( int rgba ){
		return 0.299f * (rgba >>> 24) + 0.587f * ((rgba >>> 16) & 0xFF) + 0.114f * ((rgba >>> 8) & 0xFF);
	}

	//the caves' places (levels/overworld/CaveSites, tools/cave_sites_gen.py): every prop tile is
	//drawn, and each light kind 7-11 lies on the art it lights - the lantern's glass, the fire's
	//flames, the furnace's mouth, both giant mushrooms' spots, both shard tiles' shared chips -
	//with the camp's smoke rising off the flames; and a rift's crack tiles meet edge to edge
	@Test
	public void caveSitesArtIsDrawnAndLit(){
		GdxNativesLoader.load();
		File env = new File( "src/main/assets/environment" );
		Pixmap dress = new Pixmap( new FileHandle( new File( env, "overworld_dress.png" ) ) );
		Pixmap lights = new Pixmap( new FileHandle( new File( "src/main/assets/effects/settlement_lights.png" ) ) );
		try {
			assertTrue( dress.getHeight() >= 1248 );
			assertEquals( 48, lights.getHeight() );
			for (int id = OverworldDress.CAVE_SITES_BASE; id <= OverworldDress.CAVE_SITES_LAST; id++){
				int opaque = 0;
				for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (alpha( px( dress, id, x, y ) ) == 255) opaque++;
				assertTrue( "dress " + id + " is empty", opaque >= 8 );
			}
			int[][] lit = {
					{ SettlementLights.KIND_LANTERN, OverworldDress.CAVE_LANTERN },
					{ SettlementLights.KIND_CAMPFIRE, OverworldDress.CAVE_CAMPFIRE },
					{ SettlementLights.KIND_FURNACE, OverworldDress.CAVE_FURNACE },
					{ SettlementLights.KIND_SPORES, OverworldDress.CAVE_MUSH_CAP[0] },
					{ SettlementLights.KIND_SPORES, OverworldDress.CAVE_MUSH_CAP[1] },
					{ SettlementLights.KIND_SHARDS, OverworldDress.CAVE_SHARDS[0] },
					{ SettlementLights.KIND_SHARDS, OverworldDress.CAVE_SHARDS[1] } };
			boolean[] flames = new boolean[256];
			for (int[] k : lit){
				int fx = k[0] < 8 ? k[0] * 16 : (k[0] - 8) * 16, fy = k[0] < 8 ? 0 : 32;
				int n = 0;
				for (int y = 0; y < 16; y++){
					for (int x = 0; x < 16; x++){
						if (alpha( lights.getPixel( fx + x, fy + y ) ) == 0) continue;
						n++;
						assertEquals( "kind " + k[0] + " at " + x + "," + y, 255, alpha( px( dress, k[1], x, y ) ) );
						if (k[0] == SettlementLights.KIND_CAMPFIRE) flames[x + y * 16] = true;
					}
				}
				assertTrue( "kind " + k[0] + " lights " + n, n >= 4 );
			}
			int[] hm = SettlementLights.HEARTH_MOUTH;
			int onFlames = 0;
			for (int x = hm[0]; x < hm[0] + hm[2]; x++){
				for (int y = hm[1]; y < hm[1] + hm[3]; y++) if (flames[x + y * 16]) onFlames++;
			}
			assertTrue( "the camp's smoke rises off its flames: " + onFlames, onFlames >= 3 );
			//the crack: every version's west and east columns (north and south rows) are the same
			//pixels, molten in the middle, so any two cells of a crack side by side are one line
			for (int y = 0; y < 16; y++){
				int west = px( dress, OverworldDress.CAVE_LAVA_EW[0], 0, y );
				int north = px( dress, OverworldDress.CAVE_LAVA_NS[0], y, 0 );
				for (int v = 0; v < 2; v++){
					assertEquals( "E-W crack " + v + " row " + y, west, px( dress, OverworldDress.CAVE_LAVA_EW[v], 0, y ) );
					assertEquals( "E-W crack " + v + " row " + y, west, px( dress, OverworldDress.CAVE_LAVA_EW[v], 15, y ) );
					assertEquals( "N-S crack " + v + " column " + y, north, px( dress, OverworldDress.CAVE_LAVA_NS[v], y, 0 ) );
					assertEquals( "N-S crack " + v + " column " + y, north, px( dress, OverworldDress.CAVE_LAVA_NS[v], y, 15 ) );
				}
			}
			for (int y = 6; y <= 9; y++){
				assertTrue( "molten at the edge, row " + y, lum( px( dress, OverworldDress.CAVE_LAVA_EW[0], 0, y ) ) > 120 );
				assertTrue( "molten at the edge, column " + y, lum( px( dress, OverworldDress.CAVE_LAVA_NS[0], y, 0 ) ) > 120 );
			}
		} finally {
			lights.dispose();
			dress.dispose();
		}
	}

	@Test
	public void masksSitOnTheGlass(){
		GdxNativesLoader.load();
		File env = new File( "src/main/assets/environment" );
		Pixmap town = new Pixmap( new FileHandle( new File( env, "tiles_town_remixed.png" ) ) );
		Pixmap dress = new Pixmap( new FileHandle( new File( env, "overworld_dress.png" ) ) );
		Pixmap lights = new Pixmap( new FileHandle( new File( "src/main/assets/effects/settlement_lights.png" ) ) );
		Pixmap preview = new Pixmap( 7 * 16, 2 * 16, Pixmap.Format.RGBA8888 );
		try {
			int[] dressOf = { OverworldDress.VILLAGE_WINDOWS[0], OverworldDress.VILLAGE_WINDOWS[1], -1, -1, -1, -1, OverworldDress.GNOLL_HEARTH };
			int logs = 0;
			boolean[] logMask = new boolean[256];
			for (int kind = 0; kind <= SettlementLights.KIND_HEARTH; kind++){
				Pixmap src = dressOf[kind] != -1 ? dress : town;
				int tile = dressOf[kind] != -1 ? dressOf[kind] : SettlementLights.KIND_SOURCE_TILE[kind];
				int lit = 0;
				for (int y = 0; y < 16; y++){
					for (int x = 0; x < 16; x++){
						if (alpha( lights.getPixel( kind * 16 + x, y ) ) == 0) continue;
						lit++;
						int under = px( src, tile, x, y );
						assertEquals( "kind " + kind + " at " + x + "," + y, 255, alpha( under ) );
						if (kind == SettlementLights.KIND_HEARTH){
							//the logs, in the town's ring and in the dress copy alike
							assertEquals( under, px( town, 188, x, y ) );
							assertTrue( "not a log at " + x + "," + y, ((under >>> 8) & 0xFFFFFF) == 0x806135
									|| ((under >>> 8) & 0xFFFFFF) == 0x42321C || ((under >>> 8) & 0xFFFFFF) == 0x59421F );
							logMask[x + y * 16] = true;
							logs++;
						} else {
							assertTrue( "kind " + kind + " at " + x + "," + y + " is not glass", lum( under ) < 100 );
						}
					}
				}
				assertTrue( "kind " + kind + " lights nothing", lit >= 8 );
				//the contact sheet: the tile, and the tile with its glass lit
				preview.drawPixmap( src, kind * 16, 0, (tile % 16) * 16, (tile / 16) * 16, 16, 16 );
				preview.drawPixmap( src, kind * 16, 16, (tile % 16) * 16, (tile / 16) * 16, 16, 16 );
				for (int y = 0; y < 16; y++){
					for (int x = 0; x < 16; x++){
						if (alpha( lights.getPixel( kind * 16 + x, y ) ) == 0) continue;
						preview.drawPixel( kind * 16 + x, 16 + y, kind == SettlementLights.KIND_HEARTH ? 0xFF7A30FF : 0xFFD47AFF );
					}
				}
			}
			assertEquals( 27, logs );
			//the smoke leaves from the stack's dark mouth: the town's two stacks and the village copy
			int[] m = SettlementLights.CHIMNEY_MOUTH;
			for (int x = m[0]; x < m[0] + m[2]; x++){
				for (int y = m[1]; y < m[1] + m[3]; y++){
					for (int under : new int[]{ px( town, 102, x, y ), px( town, 103, x, y ), px( dress, OverworldDress.VILLAGE_CHIMNEY, x, y ) }){
						assertEquals( 255, alpha( under ) );
						assertTrue( "mouth " + x + "," + y, lum( under ) < 60 );
					}
				}
			}
			//and from the fire ring's logs
			int[] hm = SettlementLights.HEARTH_MOUTH;
			int onLogs = 0;
			for (int x = hm[0]; x < hm[0] + hm[2]; x++){
				for (int y = hm[1]; y < hm[1] + hm[3]; y++) if (logMask[x + y * 16]) onLogs++;
			}
			assertTrue( "hearth mouth on the logs: " + onLogs, onLogs >= 9 );

			Pixmap big = new Pixmap( preview.getWidth() * 4, preview.getHeight() * 4, Pixmap.Format.RGBA8888 );
			try {
				big.setFilter( Pixmap.Filter.NearestNeighbour );
				big.drawPixmap( preview, 0, 0, preview.getWidth(), preview.getHeight(), 0, 0, big.getWidth(), big.getHeight() );
				PixmapIO.writePNG( new FileHandle( "build/reports/settlement-lights-preview.png" ), big );
			} finally {
				big.dispose();
			}
		} finally {
			preview.dispose();
			lights.dispose();
			dress.dispose();
			town.dispose();
		}
	}
}
