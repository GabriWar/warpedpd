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

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The dangers of the slices on paper (LayerHazards): firedamp only deep, on the damp low floor
 * of shut-in tunnels, never near a way between slices, the same in every window that sees it;
 * loose rock more common deeper; the roof's support and the collapse odds; thin ice only on
 * lakes, at their shore or on a mild day, never in deep cold; ridges, and a gust that never
 * pushes anyone into a drop; the numbers of thin air; and the finders the scenes use.
 */
public class LayerHazardsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	// ------------------------------------------------------------- firedamp

	@Test
	public void firedampIsPureAndWindowsAgree(){
		for (int alt : new int[]{ -6, -12 }){
			WindowGenerator.Window a = WindowGenerator.generate( SEED, alt, 0, 0, 0f );
			WindowGenerator.Window b = WindowGenerator.generate( SEED, alt, 0, 0, 0f );
			assertArrayEquals( a.firedamp, b.firedamp );
			WindowGenerator.Window c = WindowGenerator.generate( SEED, alt, 32, -32, 0f );
			int compared = 0;
			//the cells both windows hold away from their rings (a way just outside a window
			//is not seen by it: the hero never comes within the margin of the ring)
			for (int y = 6; y < H - 6; y++){
				for (int x = 6; x < W - 6; x++){
					int cx = x - 32, cy = y + 32;
					if (cx < 6 || cy < 6 || cx >= W - 6 || cy >= H - 6) continue;
					assertEquals( "cell " + x + "," + y + " at " + alt, a.firedamp[x + y * W], c.firedamp[cx + cy * W] );
					compared++;
				}
			}
			assertTrue( compared > 10000 );
		}
	}

	@Test
	public void firedampOnlyDeepOnFloorAwayFromWays(){
		for (int alt = -1; alt > LayerHazards.FIREDAMP_TOP; alt--){
			assertNull( WindowGenerator.generate( SEED, alt, 0, 0, 0f ).firedamp );
		}
		int pockets = 0, open = 0, chambers = 0;
		for (int alt = LayerHazards.FIREDAMP_TOP; alt >= -12; alt--){
			for (int i = 0; i < (alt == -8 ? 6 : 1); i++){
				int ox = i * 400 - 1000, oy = i * 300 - 700;
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, ox, oy, 0f );
				assertNotNull( w.firedamp );
				for (int y = 1; y < H - 1; y++){
					for (int x = 1; x < W - 1; x++){
						int c = x + y * W;
						if (alt == -8 && (Terrain.flags[w.terrain[c]] & Terrain.PASSABLE) != 0) open++;
						if (!w.firedamp[c]) continue;
						int t = w.terrain[c];
						assertTrue( "gas on " + t, t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.MUSHROOM_PATCH );
						assertEquals( WindowGenerator.LINK_NONE, w.link[c] );
						if (alt == -8) pockets++;
						//no way within the clearance, wherever the window can see it
						for (int dy = -LayerHazards.WAY_CLEARANCE; dy <= LayerHazards.WAY_CLEARANCE; dy++){
							for (int dx = -LayerHazards.WAY_CLEARANCE; dx <= LayerHazards.WAY_CLEARANCE; dx++){
								int nx = x + dx, ny = y + dy;
								if (nx < 1 || ny < 1 || nx >= W - 1 || ny >= H - 1) continue;
								int n = nx + ny * W;
								assertTrue( "way beside gas", w.terrain[n] != Terrain.CHASM && w.link[n] == WindowGenerator.LINK_NONE );
							}
						}
						WorldModel.CaveSample cs = WorldModel.caveSample( SEED, ox + x, oy + y, alt, null );
						if (cs.chamber > WorldModel.chamberLevel( alt )){
							//a chamber's: in a dip of its floor
							assertTrue( cs.floor < LayerHazards.LOW_CHAMBER );
							assertTrue( LayerHazards.hollow( SEED, ox + x, oy + y, alt, cs.floor ) );
							chambers++;
							continue;
						}
						//a tunnel's: shut in, few of the cells two steps round it are open
						int ringOpen = 0;
						for (int dy = -2; dy <= 2; dy++){
							for (int dx = -2; dx <= 2; dx++){
								if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != 2) continue;
								if (WorldModel.caveOpen( SEED, ox + x + dx, oy + y + dy, alt )) ringOpen++;
							}
						}
						assertTrue( "open ring " + ringOpen, ringOpen <= LayerHazards.ENCLOSED_MAX );
					}
				}
			}
		}
		System.out.println( "[hazards] firedamp at -8 over 6 windows: " + pockets + " gas cells of " + open + " open; "
				+ chambers + " gas cells in chambers on all deep slices" );
		assertTrue( "no firedamp in a chamber's dip on any deep slice", chambers > 0 );
		assertTrue( "no firedamp in six windows at -8", pockets > 0 );
		float share = pockets / (float) open;
		assertTrue( "share " + share, share > 0.0005f && share < 0.05f );
	}

	@Test
	public void firedampGrowsDeeper(){
		int shallow = 0, deep = 0;
		for (int by = -32; by < 32; by++){
			for (int bx = -32; bx < 32; bx++){
				if (LayerHazards.pocketBlock( SEED, bx, by, -4 )) shallow++;
				if (LayerHazards.pocketBlock( SEED, bx, by, -12 )) deep++;
				assertFalse( LayerHazards.pocketBlock( SEED, bx, by, -3 ) );
				assertFalse( LayerHazards.pocketBlock( SEED, bx, by, 2 ) );
			}
		}
		assertTrue( shallow + " vs " + deep, deep > shallow );
	}

	// ------------------------------------------------------------ loose rock

	@Test
	public void looseRockScalesWithDepth(){
		int top = 0, bottom = 0, n = 0;
		for (int y = -512; y < 512; y += 4){
			for (int x = -512; x < 512; x += 4){
				n++;
				if (LayerHazards.looseRock( SEED, x, y, -1 )) top++;
				if (LayerHazards.looseRock( SEED, x, y, -12 )) bottom++;
				assertFalse( LayerHazards.looseRock( SEED, x, y, 0 ) );
				assertFalse( LayerHazards.looseRock( SEED, x, y, 5 ) );
			}
		}
		float a = top / (float) n, b = bottom / (float) n;
		System.out.println( "[hazards] loose rock -1: " + a + ", -12: " + b );
		assertTrue( "at -1: " + a, a >= 0.02f && a <= 0.25f );
		assertTrue( "at -12: " + b, b >= 0.15f && b <= 0.65f );
		assertTrue( b > a );
	}

	@Test
	public void collapseChanceRules(){
		for (int alt : new int[]{ 0, 1, 10 }){
			assertEquals( 0f, LayerHazards.collapseChance( true, true, alt ), 0f );
		}
		for (int alt = -1; alt >= -12; alt--){
			assertEquals( 0f, LayerHazards.collapseChance( false, false, alt ), 0f );
			assertTrue( LayerHazards.collapseChance( false, true, alt ) > LayerHazards.collapseChance( true, false, alt ) );
			assertTrue( LayerHazards.collapseChance( true, true, alt ) <= 0.6f );
			if (alt < -1) assertTrue( LayerHazards.collapseChance( true, true, alt ) >= LayerHazards.collapseChance( true, true, alt + 1 ) );
		}
	}

	//a 7x7 map from rows of '#' (WALL) and '.' (EMPTY), the cell at its centre
	private static int[] map( String... rows ){
		int[] m = new int[49];
		for (int y = 0; y < 7; y++){
			for (int x = 0; x < 7; x++) m[x + y * 7] = rows[y].charAt( x ) == '#' ? Terrain.WALL : Terrain.EMPTY;
		}
		return m;
	}

	@Test
	public void unsupportedShapes(){
		//a pillar mined out in the middle of a hall
		assertTrue( LayerHazards.unsupported( map( "#######", "#.....#", "#.....#", "#.....#", "#.....#", "#.....#", "#######" ), 7, 24 ) );
		//a thin wall between two ways, mined through
		assertTrue( LayerHazards.unsupported( map( "#######", "##.#.##", "##.#.##", "##...##", "##.#.##", "##.#.##", "#######" ), 7, 24 ) );
		//a dead-end face in solid rock
		assertFalse( LayerHazards.unsupported( map( "#######", "#######", "#######", "###..##", "#######", "#######", "#######" ), 7, 24 ) );
		//rock on three sides: open only on the way in and out of a bend
		assertFalse( LayerHazards.unsupported( map( "#######", "#######", "###.###", "##...##", "#######", "#######", "#######" ), 7, 24 ) );
	}

	// ------------------------------------------------------------- thin ice

	//a 9x9 map of grass with a 5x5 tarn of ice in the middle
	private static int[] tarn(){
		int[] m = new int[81];
		Arrays.fill( m, Terrain.GRASS );
		for (int y = 2; y <= 6; y++) for (int x = 2; x <= 6; x++) m[x + y * 9] = Terrain.FROZEN_WATER;
		return m;
	}

	@Test
	public void thinIceRules(){
		int[] m = tarn();
		int edge = 2 + 4 * 9, centre = 4 + 4 * 9, corner = 2 + 2 * 9;
		assertTrue( LayerHazards.thinIce( m, 9, edge, -5f ) );
		assertTrue( LayerHazards.thinIce( m, 9, corner, -5f ) );
		assertFalse( LayerHazards.thinIce( m, 9, centre, -5f ) );
		assertFalse( LayerHazards.thinIce( m, 9, 3 + 4 * 9, -5f ) );
		for (int c = 0; c < 81; c++){
			int x = c % 9, y = c / 9;
			if (x == 0 || y == 0 || x == 8 || y == 8) continue;
			assertFalse( LayerHazards.thinIce( m, 9, c, -12f ) );
			assertEquals( m[c] == Terrain.FROZEN_WATER, LayerHazards.thinIce( m, 9, c, 0f ) );
		}
		//a frozen puddle bears anyone
		int[] p = new int[81];
		Arrays.fill( p, Terrain.GRASS );
		p[40] = Terrain.FROZEN_WATER;
		p[41] = Terrain.FROZEN_WATER;
		assertFalse( LayerHazards.thinIce( p, 9, 40, 0f ) );
		assertFalse( LayerHazards.thinIce( m, 9, 0 + 4 * 9 + 1, 0f ) );
		//a wide lake keeps its core: a cell with a single land cell by it is no shore
		int[] lake = new int[81];
		Arrays.fill( lake, Terrain.FROZEN_WATER );
		lake[0] = Terrain.GRASS;
		assertFalse( LayerHazards.thinIce( lake, 9, 10, -5f ) );
	}

	// ------------------------------------------------------- ridges and gusts

	//a 5x5 map from rows of '.' EMPTY, 'v' CHASM, '#' WALL, the hero at its centre
	private static int[] ground( String... rows ){
		int[] m = new int[25];
		for (int y = 0; y < 5; y++){
			for (int x = 0; x < 5; x++){
				char ch = rows[y].charAt( x );
				m[x + y * 5] = ch == 'v' ? Terrain.CHASM : ch == '#' ? Terrain.WALL : Terrain.EMPTY;
			}
		}
		return m;
	}

	@Test
	public void ridgeRules(){
		assertTrue( LayerHazards.ridgeCell( ground( ".....", ".....", ".v.v.", ".....", "....." ), 5, 12 ) );
		assertTrue( LayerHazards.ridgeCell( ground( ".....", "..v..", ".....", "..v..", "....." ), 5, 12 ) );
		assertFalse( LayerHazards.ridgeCell( ground( ".....", "..#..", ".v.v.", ".....", "....." ), 5, 12 ) );
		assertFalse( LayerHazards.ridgeCell( ground( ".....", ".....", ".v...", ".....", "....." ), 5, 12 ) );
		assertFalse( LayerHazards.ridgeCell( ground( ".....", ".....", ".vvv.", ".....", "....." ), 5, 12 ) );
		assertTrue( LayerHazards.ridgeCell( ground( ".....", ".vv..", ".v...", ".v...", "....." ), 5, 12 ) );
	}

	@Test
	public void gustNeverIntoAPit(){
		int[] kinds = { Terrain.EMPTY, Terrain.CHASM, Terrain.WALL };
		int[] m = new int[25];
		int pushed = 0, staggered = 0;
		for (int pattern = 0; pattern < 6561; pattern++){
			Arrays.fill( m, Terrain.EMPTY );
			int p = pattern;
			for (int i = 0; i < 8; i++){
				m[12 + LayerHazards.DX[i] + LayerHazards.DY[i] * 5] = kinds[p % 3];
				p /= 3;
			}
			for (int dir = 0; dir < 8; dir++){
				int to = LayerHazards.gustTarget( m, 5, 12, dir, c -> false );
				if (to == -1){ staggered++; continue; }
				pushed++;
				assertTrue( (Terrain.flags[m[to]] & Terrain.PIT) == 0 );
				assertTrue( (Terrain.flags[m[to]] & Terrain.PASSABLE) != 0 );
				boolean cone = false;
				for (int k : new int[]{ dir, (dir + 1) & 7, (dir + 7) & 7 }){
					if (to == 12 + LayerHazards.DX[k] + LayerHazards.DY[k] * 5) cone = true;
				}
				assertTrue( cone );
			}
		}
		assertTrue( pushed > 0 && staggered > 0 );
		//a cell the caller refuses (someone standing there, cracked ice) is never the target
		Arrays.fill( m, Terrain.EMPTY );
		assertEquals( -1, LayerHazards.gustTarget( m, 5, 12, 2, c -> true ) );
		assertEquals( 7, LayerHazards.windDir( 315f ) );
		assertEquals( 0, LayerHazards.windDir( 359f ) );
		assertEquals( 2, LayerHazards.windDir( 90f ) );
	}

	@Test
	public void gustChanceBounds(){
		assertEquals( 0f, LayerHazards.gustChance( 6, 30f ), 0f );
		assertEquals( 0f, LayerHazards.gustChance( 9, 5f ), 0f );
		float last = 0f;
		for (int alt = LayerHazards.HIGH; alt <= 10; alt++){
			float prevWind = 0f;
			for (float wind = 6f; wind <= 30f; wind += 2f){
				float c = LayerHazards.gustChance( alt, wind );
				assertTrue( c > 0f && c <= 0.35f );
				assertTrue( c >= prevWind );
				prevWind = c;
			}
			float at10 = LayerHazards.gustChance( alt, 10f );
			assertTrue( at10 >= last );
			last = at10;
		}
	}

	@Test
	public void breathlessNumbers(){
		assertEquals( 0, LayerHazards.maxStacks( 6 ) );
		assertEquals( 2, LayerHazards.maxStacks( 7 ) );
		assertEquals( 3, LayerHazards.maxStacks( 8 ) );
		assertEquals( 4, LayerHazards.maxStacks( 9 ) );
		assertEquals( 5, LayerHazards.maxStacks( 10 ) );
		assertEquals( 40, LayerHazards.stackEvery( 7 ) );
		assertEquals( 25, LayerHazards.stackEvery( 10 ) );
	}

	// ------------------------------------------------------------- finders

	@Test
	public void finders(){
		long t0 = System.currentTimeMillis();
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		assertNotNull( f );
		WorldModel.CaveSample cs = WorldModel.caveSample( SEED, f[2], f[3], -6, null );
		assertTrue( LayerHazards.gassy( SEED, f[2], f[3], -6, cs, WorldModel.caveTerrain( SEED, f[2], f[3], -6, cs ) ) );
		assertEquals( 1, Math.abs( f[0] - f[2] ) + Math.abs( f[1] - f[3] ) );
		long t1 = System.currentTimeMillis();
		int[] l = LayerHazards.findLooseWall( SEED, -8, 200 );
		assertNotNull( l );
		assertTrue( LayerHazards.looseRock( SEED, l[0] + 1, l[1], -8 ) );
		assertFalse( WorldModel.caveOpen( SEED, l[0] + 1, l[1], -8 ) );
		assertTrue( WorldModel.caveOpen( SEED, l[0], l[1], -8 ) && WorldModel.caveOpen( SEED, l[0] + 2, l[1], -8 ) );
		long t2 = System.currentTimeMillis();
		System.out.println( "[hazards] firedamp at " + Arrays.toString( f ) + " in " + (t1 - t0) + "ms, loose wall at "
				+ Arrays.toString( l ) + " in " + (t2 - t1) + "ms" );
	}

	@Test
	public void heightsFinders(){
		float winter = WorldModel.calendarShift( xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.WINTER, 0.5f );
		long t0 = System.currentTimeMillis();
		int[] ice = LayerHazards.findThinIce( SEED, 2, winter, -4f, 30, 8 );
		assertNotNull( ice );
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 2, ice[0] - W / 2, ice[1] - H / 2, winter );
		int c = W / 2 + (H / 2) * W;
		boolean beside = false;
		for (int n : new int[]{ c - W, c + 1, c + W, c - 1 }) beside |= LayerHazards.thinIce( w.terrain, W, n, -4f );
		assertTrue( beside );
		long t1 = System.currentTimeMillis();
		int[] ridge = LayerHazards.findRidge( SEED, 9, winter, 30, 8 );
		assertNotNull( ridge );
		w = WindowGenerator.generate( SEED, 9, ridge[0] - W / 2, ridge[1] - H / 2, winter );
		assertTrue( LayerHazards.ridgeCell( w.terrain, W, c ) );
		long t2 = System.currentTimeMillis();
		System.out.println( "[hazards] thin ice by " + Arrays.toString( ice ) + " in " + (t1 - t0) + "ms, ridge at "
				+ Arrays.toString( ridge ) + " in " + (t2 - t1) + "ms" );
	}
}
