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
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The ore of the world's rock (Ores): pure and the same through every window, tiered by depth
 * and height with the extreme metals kept to their extremes, laid in runs along the rock faces,
 * drawn only where a face shows, seen often enough to be worth walking for, and priced so the
 * gems never out-earn the ore.
 */
public class OresTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	@BeforeClass
	public static void boot(){
		//the item sheet's frames (ItemSpriteSheet) and the tile sheet's stitching need the assets
		WarpedRoomsTest.boot();
	}

	//a window centre on slice a (> 0) whose own ground and the rock above it both fill a good
	//part of the view: the nearest to the origin, searched ring by ring
	static int[] rockyPeak( long seed, int a ){
		for (int r = 0; r <= 4000; r += 64){
			for (int i = -r; i <= r; i += 64){
				for (int[] c : new int[][]{ { i, -r }, { i, r }, { -r, i }, { r, i } }){
					if (WorldLayers.band( WorldModel.elevation( seed, c[0], c[1] ) ) != a) continue;
					int ground = 0, rock = 0;
					for (int dy = -40; dy <= 40; dy += 10){
						for (int dx = -40; dx <= 40; dx += 10){
							int b = WorldLayers.band( WorldModel.elevation( seed, c[0] + dx, c[1] + dy ) );
							if (b == a) ground++;
							else if (b > a) rock++;
						}
					}
					if (ground >= 20 && rock >= 20) return c;
				}
			}
		}
		return null;
	}

	//a window's origin centred on a slice's typical country: the caves anywhere, the peaks on rocky ground
	private static int[] origin( int a ){
		int[] c = a > 0 ? rockyPeak( SEED, a ) : new int[]{ 300, -200 };
		assertNotNull( "no rocky ground on slice " + a, c );
		return new int[]{ c[0] - W / 2, c[1] - H / 2 };
	}

	private static int[][] dressed( WindowGenerator.Window w, int ox, int oy ){
		return WindowGenerator.dress( SEED, ox, oy, w.terrain, w, GameCalendar.Season.SUMMER );
	}

	@Test
	public void veinsAreDeterministic(){
		for (int a : new int[]{ -6, 4 }){
			int[] o = origin( a );
			byte[] one = new byte[W * H], two = new byte[W * H];
			Ores.veins( SEED, a, o[0], o[1], W, H, null, one );
			Ores.veins( SEED, a, o[0], o[1], W, H, null, two );
			assertArrayEquals( one, two );
			for (int c = 0; c < one.length; c += 37){
				assertEquals( Ores.oreAt( SEED, a, o[0] + c % W, o[1] + c / W ), Ores.oreAt( SEED, a, o[0] + c % W, o[1] + c / W ) );
			}
		}
	}

	//the window at once gives what the cell function gives; a window offset by a slide agrees on the overlap
	@Test
	public void windowMapMatchesCellFunction(){
		for (int a : new int[]{ -6, 4 }){
			int[] o = origin( a );
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], 0f );
			byte[] map = w.veins;
			int veins = 0;
			for (int c = 0; c < map.length; c++){
				Ores.Kind k = Ores.oreAt( SEED, a, o[0] + c % W, o[1] + c / W );
				assertEquals( "slice " + a + " cell " + c, k == null ? 0 : k.ordinal() + 1, map[c] );
				if (k != null) veins++;
			}
			assertTrue( "no veins on slice " + a, veins > 50 );
			int sx = o[0] + 32, sy = o[1] - 32;
			byte[] slid = WindowGenerator.generate( SEED, a, sx, sy, 0f ).veins;
			for (int y = 0; y < H; y++){
				for (int x = 0; x < W; x++){
					int lx = x + 32, ly = y - 32;
					if (lx >= W || ly < 0) continue;
					assertEquals( map[lx + ly * W], slid[x + y * W] );
				}
			}
		}
	}

	//the generator's record of the rock (Window.rock), which the window's veins are walked over, is
	//the world's natural rock at every cell, in any season - ring, ways and water included
	@Test
	public void rockBitsAreTheWorldsRock(){
		for (int a : new int[]{ -1, -6, -12, 3, 8 }){
			int[] o = origin( a );
			for (float shift : new float[]{ 0f, WorldModel.calendarShift( GameCalendar.Season.WINTER, 0.5f ) }){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], shift );
				for (int y = 0; y < H; y++){
					for (int x = 0; x < W; x++){
						assertEquals( "slice " + a + " at " + x + "," + y + " (" + w.terrain[x + y * W] + ")",
								Ores.naturalRock( SEED, a, o[0] + x, o[1] + y ), w.rock[x + y * W] );
					}
				}
				byte[] asked = new byte[W * H];
				Ores.veins( SEED, a, o[0], o[1], W, H, null, asked );
				assertArrayEquals( "slice " + a, asked, w.veins );
			}
		}
	}

	//masonry raised on open ground never holds ore and never glints, and floor cut into the rock
	//moves no vein: what a site builds into a window's terrain (a tomb's ring in the caves, a hut's
	//walls on a peak's own ground, a shaft's floor) leaves the window's veins as they were
	@Test
	public void builtWallsHoldNoOre(){
		for (int a : new int[]{ -6, 3 }){
			int[] o = origin( a );
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], 0f );
			byte[] before = w.veins.clone();
			int built = 0, cut = 0;
			//every 7x7 of the slice's own ground (no natural rock in it) on a 9-cell grid gets a 5x5
			//ring of wall in it; some vein faces get the rock behind them cut to floor
			for (int y = 8; y + 5 < H - 8; y += 9){
				for (int x = 8; x + 5 < W - 8; x += 9){
					boolean open = true;
					for (int dy = -1; dy <= 5 && open; dy++){
						for (int dx = -1; dx <= 5; dx++){
							if (w.rock[x + dx + (y + dy) * W]){ open = false; break; }
						}
					}
					if (!open) continue;
					for (int dy = 0; dy < 5; dy++){
						for (int dx = 0; dx < 5; dx++){
							if (dy == 0 || dy == 4 || dx == 0 || dx == 4){
								w.terrain[x + dx + (y + dy) * W] = Terrain.WALL;
								built++;
							}
						}
					}
				}
			}
			for (int c = W; c < W * (H - 1); c++){
				if (before[c] != 0 && w.rock[c - W] && Ores.showsFace( w.terrain, c ) && c % 13 == 0){
					w.terrain[c - W] = Terrain.EMPTY;
					cut++;
				}
			}
			assertTrue( "slice " + a + ": nothing built", built > 50 );
			assertTrue( "slice " + a + ": nothing cut", cut > 0 );
			byte[] again = new byte[W * H];
			Ores.veins( SEED, a, o[0], o[1], W, H, w.rock, again );
			assertArrayEquals( "the veins moved with the building on " + a, before, again );
			int[][] d = dressed( w, o[0], o[1] );
			for (int c = 0; c < W * H; c++){
				if (w.rock[c]) continue;
				assertEquals( "ore in masonry on " + a, 0, w.veins[c] );
				assertNull( "a glint on masonry on " + a, Ores.faceKind( d[0][c] ) );
			}
		}
	}

	@Test
	public void surfaceHasNoVeins(){
		assertEquals( 0, Ores.density( 0 ) );
		assertEquals( "nothing rises above the top slice", 0, Ores.density( WorldLayers.MAX_ABOVE ) );
		for (int i = 0; i < 10000; i++){
			assertNull( Ores.oreAt( SEED, 0, i % 100, i / 100 ) );
			assertTrue( !Ores.naturalRock( SEED, 0, i % 100, i / 100 ) );
		}
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, 0, 0, 0f );
		assertNull( w.rock );
		assertNull( w.veins );
		int[] ground = new int[W * H], canopy = new int[W * H];
		Arrays.fill( ground, -1 );
		Arrays.fill( canopy, -1 );
		Ores.dressVeins( SEED, 0, 0, w.terrain, w.veins, ground, canopy );
		for (int g : ground) assertEquals( -1, g );
	}

	//every vein's metal belongs to its slice (caves) or to the band of its rock (peaks); the
	//extremes stay at the extremes; copper stays shallow
	@Test
	public void tierTable(){
		for (int a = -WorldLayers.MAX_BELOW; a <= WorldLayers.MAX_ABOVE; a++){
			if (a == 0) continue;
			int sum = 0;
			for (Ores.Kind k : Ores.Kind.values()) sum += Ores.weight( a, k );
			assertEquals( "row " + a, 100, sum );
		}
		assertEquals( 0, Ores.weight( -12, Ores.Kind.SKYIRON ) );
		assertEquals( 0, Ores.weight( 10, Ores.Kind.DEEPSILVER ) );

		int[] xs = new int[Ores.BLOCK], ys = new int[Ores.BLOCK];
		Ores.Kind[] kind = new Ores.Kind[1];
		int[] seen = new int[Ores.Kind.values().length];
		for (int a : new int[]{ -1, -3, -5, -8, -9, -12, 2, 4, 6, 7, 8 }){
			int[] o = origin( a );
			Ores.Rock rock = new Ores.Rock( SEED, a, o[0] - 16, o[1] - 16, 600, 600 );
			for (int by = Math.floorDiv( o[1], 8 ); by < Math.floorDiv( o[1], 8 ) + 70; by++){
				for (int bx = Math.floorDiv( o[0], 8 ); bx < Math.floorDiv( o[0], 8 ) + 70; bx++){
					int n = Ores.vein( SEED, a, bx, by, xs, ys, kind, rock );
					if (n == 0) continue;
					Ores.Kind k = kind[0];
					seen[k.ordinal()]++;
					int row = a;
					if (a > 0){
						int band = WorldLayers.band( WorldModel.elevation( SEED, xs[0], ys[0] ) );
						assertTrue( "a peak vein starts in rock", band > a );
						row = Math.min( WorldLayers.MAX_ABOVE, band );
						if (k == Ores.Kind.SKYIRON) assertTrue( "skyiron in band " + band + " rock", band >= 8 );
						if (k == Ores.Kind.COPPER) assertTrue( "copper in band " + band + " rock", band <= 4 );
					} else {
						if (k == Ores.Kind.DEEPSILVER) assertTrue( "deepsilver at " + a, a <= -9 );
						if (k == Ores.Kind.COPPER) assertTrue( "copper at " + a, a >= -5 );
					}
					assertTrue( k + " on row " + row, Ores.weight( row, k ) > 0 );
					assertTrue( a > 0 ? k != Ores.Kind.DEEPSILVER : k != Ores.Kind.SKYIRON );
				}
			}
		}
		assertTrue( "no deepsilver anywhere", seen[Ores.Kind.DEEPSILVER.ordinal()] > 0 );
		for (Ores.Kind k : new Ores.Kind[]{ Ores.Kind.COPPER, Ores.Kind.IRON, Ores.Kind.SILVER, Ores.Kind.GOLD }){
			assertTrue( "no " + k, seen[k.ordinal()] > 0 );
		}
	}

	//the deeper and higher the slice, the more of its faces are veined
	@Test
	public void densityRisesWithDepthAndHeight(){
		for (int a = 1; a < WorldLayers.MAX_BELOW; a++){
			assertTrue( Ores.density( -a - 1 ) >= Ores.density( -a ) );
		}
		for (int a = 1; a < WorldLayers.MAX_ABOVE - 1; a++){
			assertTrue( Ores.density( a + 1 ) >= Ores.density( a ) );
		}
		assertTrue( Ores.density( -12 ) > Ores.density( -1 ) );
		float shallow = veinedFaces( -1 ), deep = veinedFaces( -12 );
		System.out.println( "[ores] veined faces -1 " + shallow + " -12 " + deep );
		assertTrue( deep > shallow );
	}

	//the share of a slice's faces that carry a vein, over a few windows
	private static float veinedFaces( int a ){
		int faces = 0, veined = 0;
		for (int i = 0; i < 3; i++){
			int ox = -500 + 700 * i, oy = 400 - 300 * i;
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, ox, oy, 0f );
			byte[] v = w.veins;
			for (int c = W; c < W * (H - 1); c++){
				if (!Ores.showsFace( w.terrain, c )) continue;
				faces++;
				if (v[c] != 0) veined++;
			}
		}
		return veined / (float) faces;
	}

	//a vein is a run along the rock, never a speck
	@Test
	public void veinsAreRuns(){
		int[] xs = new int[Ores.BLOCK], ys = new int[Ores.BLOCK];
		Ores.Kind[] kind = new Ores.Kind[1];
		for (int a : new int[]{ -4, -10, 5 }){
			int[] o = origin( a );
			int size = 512;
			Ores.Rock rock = new Ores.Rock( SEED, a, o[0] - 16, o[1] - 16, size + 32, size + 33 );
			int veins = 0;
			for (int by = Math.floorDiv( o[1], 8 ); by < Math.floorDiv( o[1] + size, 8 ); by++){
				for (int bx = Math.floorDiv( o[0], 8 ); bx < Math.floorDiv( o[0] + size, 8 ); bx++){
					int n = Ores.vein( SEED, a, bx, by, xs, ys, kind, rock );
					if (n == 0) continue;
					veins++;
					assertTrue( "length " + n, n >= 3 && n <= 8 );
					for (int i = 0; i < n; i++){
						assertTrue( "out of reach", Math.abs( Math.floorDiv( xs[i], 8 ) - bx ) <= 1 && Math.abs( Math.floorDiv( ys[i], 8 ) - by ) <= 1 );
						if (i > 0) assertTrue( "a step of one", Math.abs( xs[i] - xs[i-1] ) <= 1 && Math.abs( ys[i] - ys[i-1] ) <= 1 );
						assertTrue( "ore in natural rock only", Ores.naturalRock( SEED, a, xs[i], ys[i] ) );
					}
				}
			}
			//(the peaks are open country with outcrops: far fewer faces to start on)
			assertTrue( "slice " + a + ": " + veins + " veins", veins > (a < 0 ? 100 : 10) );
			byte[] map = new byte[size * size];
			Ores.veins( SEED, a, o[0], o[1], size, size, null, map );
			int cells = 0, friendly = 0;
			boolean[] done = new boolean[map.length];
			for (int c = 0; c < map.length; c++){
				if (map[c] == 0) continue;
				cells++;
				int x = c % size, y = c / size;
				boolean same = false;
				for (int dy = -1; dy <= 1 && !same; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if ((dx | dy) == 0 || x + dx < 0 || y + dy < 0 || x + dx >= size || y + dy >= size) continue;
						if (map[c + dx + dy * size] == map[c]){ same = true; break; }
					}
				}
				if (same) friendly++;
				//a seam cut by the rect's edge may be short: count only the ones well inside
				if (done[c] || x < 8 || y < 8 || x >= size - 8 || y >= size - 8) continue;
				int comp = 0;
				ArrayDeque<Integer> q = new ArrayDeque<>();
				q.add( c );
				done[c] = true;
				while (!q.isEmpty()){
					int p = q.poll();
					comp++;
					for (int dy = -1; dy <= 1; dy++){
						for (int dx = -1; dx <= 1; dx++){
							int px = p % size + dx, py = p / size + dy;
							if (px < 0 || py < 0 || px >= size || py >= size) continue;
							int n = px + py * size;
							if (!done[n] && map[n] != 0){ done[n] = true; q.add( n ); }
						}
					}
				}
				assertTrue( "a vein of " + comp, comp >= 3 );
			}
			assertTrue( "slice " + a + ": " + friendly + " of " + cells, friendly >= cells * 0.95f );
		}
	}

	//every glint is on a WALL face with a vein in it, every such face glints, and the cell rule agrees
	@Test
	public void glintsOnlyOnShownFaces(){
		for (int a : new int[]{ -6, 8 }){
			int[] o = origin( a );
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], 0f );
			int[][] d = dressed( w, o[0], o[1] );
			byte[] vein = w.veins;
			int glints = 0;
			for (int c = 0; c < W * H; c++){
				int x = c % W, y = c / W;
				Ores.Kind k = Ores.faceKind( d[0][c] );
				if (k != null){
					glints++;
					assertEquals( Terrain.WALL, w.terrain[c] );
					assertTrue( Ores.showsFace( w.terrain, c ) );
					assertEquals( k.ordinal() + 1, vein[c] );
					assertEquals( -1, d[2][c] );
				} else if (x > 0 && y > 0 && x < W - 1 && y < H - 1 && vein[c] != 0 && Ores.showsFace( w.terrain, c )){
					assertTrue( "an unglinting vein face", d[0][c] != -1 || d[2][c] != -1 );
				}
				if (d[0][c] == -1 || k != null){
					assertEquals( d[0][c], Ores.faceTileAt( SEED, o[0], o[1], w.terrain, w.veins, d[2], c ) );
				}
			}
			assertTrue( "no glints on slice " + a, glints > 5 );
		}
	}

	//walking the caves at -5 the rock glints often; on the peaks, wherever rock is in sight
	@Test
	public void glintsAreSeen(){
		for (int a : new int[]{ -5, 5 }){
			int[] o = origin( a );
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], 0f );
			int[][] d = dressed( w, o[0], o[1] );
			int faces = 0, glints = 0;
			for (int c = W; c < W * (H - 1); c++){
				if (!Ores.showsFace( w.terrain, c )) continue;
				faces++;
				if (Ores.faceKind( d[0][c] ) != null) glints++;
			}
			Random rnd = new Random( 7 );
			int samples = 0;
			long seen = 0;
			for (int tries = 0; tries < 200000 && samples < 200; tries++){
				int c = 9 + rnd.nextInt( W - 18 ) + (9 + rnd.nextInt( H - 18 )) * W;
				if ((Terrain.flags[w.terrain[c]] & Terrain.PASSABLE) == 0) continue;
				int here = 0, rock = 0;
				for (int dy = -8; dy <= 8; dy++){
					for (int dx = -8; dx <= 8; dx++){
						int n = c + dx + dy * W;
						if (Ores.showsFace( w.terrain, n )) rock++;
						if (Ores.faceKind( d[0][n] ) != null) here++;
					}
				}
				//the peaks are mostly open country: count the places a face is in sight
				if (a > 0 && rock == 0) continue;
				samples++;
				seen += here;
			}
			float share = glints / (float) faces, view = seen / (float) samples;
			System.out.printf( "[ores] slice %d: %d of %d faces glint (%.1f%%), %.2f glints in view%n", a, glints, faces, 100 * share, view );
			assertTrue( share >= 0.08f );
			assertTrue( view >= 2.5f );
		}
	}

	@Test
	public void gemOdds(){
		for (int a = 0; a <= WorldLayers.MAX_ABOVE; a++) assertNull( Ores.gemAt( SEED, a, 5, 5 ) );
		int[][] counts = new int[13][Ores.GemKind.values().length];
		int[] gems = new int[13];
		int n = 20000;
		for (int d = 1; d <= 12; d++){
			for (int i = 0; i < n; i++){
				Ores.GemKind g = Ores.gemAt( SEED, -d, i % 211, i / 211 );
				if (g == null) continue;
				gems[d]++;
				counts[d][g.ordinal()]++;
			}
			if (d <= 3) assertEquals( "a diamond at -" + d, 0, counts[d][Ores.GemKind.DIAMOND.ordinal()] );
		}
		assertEquals( 0.06f, gems[1] / (float) n, 0.01f );
		assertEquals( 0.17f, gems[12] / (float) n, 0.015f );
		float d6 = counts[6][Ores.GemKind.DIAMOND.ordinal()] / (float) gems[6];
		float d12 = counts[12][Ores.GemKind.DIAMOND.ordinal()] / (float) gems[12];
		assertTrue( d12 > d6 );
	}

	@Test
	public void yields(){
		int doubles = 0, n = 20000;
		for (int i = 0; i < n; i++){
			for (int a = -5; a <= -1; a++) assertEquals( 1, Ores.yield( SEED, a, i % 157, i / 157 ) );
			int y = Ores.yield( SEED, -12, i % 157, i / 157 );
			assertTrue( y == 1 || y == 2 );
			if (y == 2) doubles++;
		}
		assertEquals( 0.20f, doubles / (float) n, 0.03f );
	}

	//the veins are walked once per window on the generating thread (a worker, or a mirror's network
	//thread); the dress, which a mirror runs on the render thread, only reads them
	@Test
	public void dressCostIsSmall(){
		long walk = 0, dress = 0;
		int runs = 0;
		for (int pass = 0; pass < 2; pass++){
			for (int a : new int[]{ -2, -7, -12, 3, 6, 8 }){
				int[] o = origin( a );
				WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], 0f );
				int[] ground = new int[W * H], canopy = new int[W * H];
				Arrays.fill( ground, -1 );
				Arrays.fill( canopy, -1 );
				byte[] v = new byte[W * H];
				long t = System.nanoTime();
				Ores.veins( SEED, a, o[0], o[1], W, H, w.rock, v );
				long t2 = System.nanoTime();
				Ores.dressVeins( SEED, o[0], o[1], w.terrain, w.veins, ground, canopy );
				long t3 = System.nanoTime();
				//the first pass warms the JIT up
				if (pass == 0) continue;
				walk += t2 - t;
				dress += t3 - t2;
				runs++;
			}
		}
		float walkMs = walk / 1e6f / runs, dressMs = dress / 1e6f / runs;
		System.out.println( "[ores] vein walk " + walkMs + " ms, dressVeins " + dressMs + " ms a window" );
		assertTrue( walkMs < 20f );
		assertTrue( dressMs < 1f );
	}

	//a gem is worth no more gold per hunger than the ore beside it (a vein cell costs 4, a crystal 1)
	@Test
	public void oreAndGemEconomy(){
		for (int a : new int[]{ -3, -6, -12 }){
			int crystals = 0, veinFaces = 0;
			float ore = 0;
			for (int i = 0; i < 2; i++){
				int ox = -300 + 600 * i, oy = 200 - 500 * i;
				WindowGenerator.Window w = WindowGenerator.generate( SEED, a, ox, oy, 0f );
				int[][] d = dressed( w, ox, oy );
				for (int c = W; c < W * (H - 1); c++){
					if (w.terrain[c] == Terrain.MINE_CRYSTAL){
						for (int n : new int[]{ c - 1, c + 1, c - W, c + W }){
							if ((Terrain.flags[w.terrain[n]] & Terrain.PASSABLE) != 0){ crystals++; break; }
						}
					}
					Ores.Kind k = Ores.faceKind( d[0][c] );
					if (k != null){
						veinFaces++;
						ore += k.price * Ores.yield( SEED, a, ox + c % W, oy + c / W );
					}
				}
			}
			float gem = 0;
			for (int i = 0; i < 20000; i++){
				Ores.GemKind g = Ores.gemAt( SEED, a, i % 199, i / 199 );
				if (g != null) gem += g.price;
			}
			float gemPerHunger = gem / 20000f;
			float orePerHunger = ore / veinFaces / 4f;
			System.out.printf( "[ores] slice %d: %d reachable crystals, %d glinting faces; gold a hunger: gems %.2f, ore %.2f%n",
					a, crystals, veinFaces, gemPerHunger, orePerHunger );
			assertTrue( crystals > 0 && veinFaces > 0 );
			assertTrue( gemPerHunger <= orePerHunger );
		}
	}

	/**
	 * A greedy miner on a real window: from open ground near the middle he walks (one turn a cell,
	 * diagonals too) to the nearest floor beside a glinting face, breaks it (one turn), and goes on
	 * - a broken face shows the rock behind it, which glints in turn if the vein runs on. Returns
	 * the lumps of each metal (yield included), then the vein cells broken and the turns spent: at
	 * most `turns`, fewer when no glint is left within his reach.
	 */
	static int[] mine( long seed, int a, int ox, int oy, int turns ){
		WindowGenerator.Window w = WindowGenerator.generate( seed, a, ox, oy, 0f );
		int[] map = w.terrain.clone();
		int[] got = new int[Ores.Kind.values().length + 2];
		int pos = -1;
		for (int r = 0; r < W / 2 && pos < 0; r++){
			for (int c : new int[]{ W / 2 + r + (H / 2) * W, W / 2 - r + (H / 2) * W, W / 2 + (H / 2 + r) * W, W / 2 + (H / 2 - r) * W }){
				if ((Terrain.flags[map[c]] & Terrain.PASSABLE) != 0){ pos = c; break; }
			}
		}
		int[] dist = new int[W * H];
		ArrayDeque<Integer> q = new ArrayDeque<>();
		int spent = 0;
		while (spent < turns){
			Arrays.fill( dist, -1 );
			dist[pos] = 0;
			q.clear();
			q.add( pos );
			int stand = -1, target = -1;
			while (!q.isEmpty() && target < 0){
				int c = q.poll();
				for (int dy = -1; dy <= 1 && target < 0; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int n = c + dx + dy * W;
						int nx = n % W, ny = n / W;
						if (nx < 1 || ny < 1 || nx >= W - 1 || ny >= H - 1) continue;
						if (w.veins[n] != 0 && map[n] == Terrain.WALL && Ores.showsFace( map, n )){
							stand = c;
							target = n;
							break;
						}
						if (dist[n] < 0 && (Terrain.flags[map[n]] & Terrain.PASSABLE) != 0){
							dist[n] = dist[c] + 1;
							q.add( n );
						}
					}
				}
			}
			if (target < 0) break;
			spent += dist[stand] + 1;
			pos = stand;
			map[target] = Terrain.EMPTY_DECO;
			got[w.veins[target] - 1] += Ores.yield( seed, a, ox + target % W, oy + target / W );
			got[got.length - 2]++;
		}
		got[got.length - 1] = spent;
		return got;
	}

	//how fast a miner who does nothing else digs, per thousand turns, walking included (the numbers
	//the exchange rates in Ores are set by): in the caves of -5 and -12 he never runs short of
	//glints, and fills more than the troll's one crucible a day well within the day
	@Test
	public void minerRate(){
		for (int a : new int[]{ -3, -5, -8, -12, 5, 8 }){
			int[] o = origin( a );
			int[] got = mine( SEED, a, o[0], o[1], 1000 );
			int spent = got[got.length - 1];
			float cores = 0, gold = 0;
			StringBuilder sb = new StringBuilder();
			for (Ores.Kind k : Ores.Kind.values()){
				int n = got[k.ordinal()];
				if (n == 0) continue;
				cores += n / (float) k.batch;
				gold += n * k.price;
				sb.append( ' ' ).append( n ).append( ' ' ).append( k.name().toLowerCase() );
			}
			System.out.printf( "[ores] miner on %d, %d turns: %d cells,%s; %.2f cores at the troll, %.0f gold at a shop%n",
					a, spent, got[got.length - 2], sb, cores, gold );
			if (a == -5 || a == -12){
				assertTrue( "slice " + a + ": out of glints after " + spent, spent >= 1000 );
				assertTrue( "slice " + a + ": no crucible's worth", cores >= 2f );
			}
		}
	}

	//the art the ids point at is on the sheets, inside the rects the game crops
	@Test
	public void sheetsHoldTheArt() throws Exception {
		BufferedImage dress = ImageIO.read( new File( "src/main/assets/environment/overworld_dress.png" ) );
		for (int id = OverworldDress.ORE_FACE; id < OverworldDress.ORE_FACE + OverworldDress.ORE_VERSIONS * Ores.Kind.values().length; id++){
			assertTrue( "dress " + id, opaque( dress, (id % 16) * 16, (id / 16) * 16, 16, 16 ) > 10 );
		}
		BufferedImage items = ImageIO.read( new File( "src/main/assets/sprites/items.png" ) );
		int[][] rects = { { ItemSpriteSheet.ORE_COPPER, 11, 10 }, { ItemSpriteSheet.ORE_IRON, 12, 10 },
				{ ItemSpriteSheet.ORE_SILVER, 12, 10 }, { ItemSpriteSheet.ORE_GOLD, 12, 10 },
				{ ItemSpriteSheet.ORE_DEEPSILVER, 12, 10 }, { ItemSpriteSheet.ORE_SKYIRON, 12, 10 },
				{ ItemSpriteSheet.GEM_AMETHYST, 11, 11 }, { ItemSpriteSheet.GEM_GARNET, 10, 9 },
				{ ItemSpriteSheet.GEM_EMERALD, 8, 10 }, { ItemSpriteSheet.GEM_SAPPHIRE, 9, 11 },
				{ ItemSpriteSheet.GEM_DIAMOND, 10, 10 } };
		for (int[] r : rects){
			int x = (r[0] % 16) * 16, y = (r[0] / 16) * 16;
			int inside = opaque( items, x, y, r[1], r[2] );
			assertTrue( "item " + r[0], inside > 30 );
			assertEquals( "item " + r[0] + " spills out of its rect", inside, opaque( items, x, y, 16, 16 ) );
		}
	}

	private static int opaque( BufferedImage im, int x0, int y0, int w, int h ){
		int n = 0;
		for (int y = y0; y < y0 + h; y++){
			for (int x = x0; x < x0 + w; x++){
				if ((im.getRGB( x, y ) >>> 24) != 0) n++;
			}
		}
		return n;
	}
}
