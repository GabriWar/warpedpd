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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.WeatherOverlayAmbient;
import xyz.gabriwar.warpedpixeldungeon.effects.CloudSea;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The slices' small life on the world as it is generated, not on hand-made maps: on real cave
 * and mountain windows every cell the life would use is the ground it is meant for (bats on a
 * face over open ground, fish in deep pools, drops in pools or by the rock, clouds over open
 * air and never a way between slices, marmots on thawed meadow by a boulder, plumes off
 * frozen rock); the glows picked round the hero are the same world cells from two windows
 * (nothing jumps on a rebase); every slice of the world gets a life and the surface keeps its
 * own; the world's caves turn the city's ambient overlay off.
 */
public class SliceLifeWorldTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private Level saved;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		PathFinder.setMapSize( W, H );
	}

	@After
	public void tearDown(){
		WorldModel.releaseSeasonShift();
		Dungeon.level = saved;
	}

	private static boolean water( int t ){
		return t == Terrain.WATER || t == Terrain.DEEP_WATER;
	}

	private static boolean open( int t ){
		return (Terrain.flags[t] & (Terrain.PASSABLE | Terrain.LIQUID)) != 0;
	}

	@Test
	public void caveLifeKeepsToItsGroundOnRealWindows(){
		int[] alts = { -1, -3, -5, -9, -12 };
		int[][] origins = { { -88, -88 }, { 400, -300 }, { -1200, 900 } };
		int fishTotal = 0;
		for (int a : alts){
			int batWalls = 0, fish = 0, pools = 0, floors = 0, chambers = 0, fungus = 0, crystal = 0;
			for (int[] o : origins){
				int[] m = WindowGenerator.generate( SEED, a, o[0], o[1], 0f ).terrain;
				for (int y = 1; y < H - 1; y++){
					for (int x = 1; x < W - 1; x++){
						int c = x + y * W, t = m[c];
						if (t == Terrain.MUSHROOM_PATCH) fungus++;
						if (t == Terrain.MINE_CRYSTAL) crystal++;
						if (CaveLife.batWall( m, W, c )){
							batWalls++;
							assertTrue( "a bat hangs on rock at " + a, t == Terrain.WALL || t == Terrain.WALL_DECO );
							assertTrue( "over open ground at " + a, open( m[c + W] ) );
						}
						if (CaveLife.caveFishWater( m, W, H, c )){
							fish++;
							assertTrue( "fish swim in pools at " + a + " (" + t + ")", water( t ) );
							boolean deep = false;
							for (int dy = -2; dy <= 2; dy++){
								for (int dx = -2; dx <= 2; dx++){
									int cx = x + dx, cy = y + dy;
									if (cx >= 0 && cy >= 0 && cx < W && cy < H && m[cx + cy * W] == Terrain.DEEP_WATER) deep = true;
								}
							}
							assertTrue( "never the wadeable shelf alone at " + a, deep );
						}
						int k = CaveLife.dripKind( m, W, H, c );
						if (k == CaveLife.DRIP_POOL){
							pools++;
							assertTrue( water( t ) );
						} else if (k == CaveLife.DRIP_FLOOR){
							floors++;
							assertTrue( "dry floor", (Terrain.flags[t] & Terrain.PASSABLE) != 0 && (Terrain.flags[t] & Terrain.LIQUID) == 0 );
						}
						if (CaveLife.chamberCell( m, W, c )){
							chambers++;
							assertTrue( "motes over dry floor", (Terrain.flags[t] & Terrain.PASSABLE) != 0 && (Terrain.flags[t] & Terrain.LIQUID) == 0 );
						}
					}
				}
			}
			System.out.println( "caves " + a + ": batWalls " + batWalls + ", fish cells " + fish + ", pool drips " + pools
					+ ", floor drips " + floors + ", chamber " + chambers + ", fungus " + fungus + ", crystal " + crystal );
			//the life has somewhere to be on every band of the caves
			assertTrue( "bats have faces at " + a, batWalls > 0 );
			assertTrue( "drops have floor by the rock at " + a, floors > 0 );
			assertTrue( "motes have chambers at " + a, chambers > 0 );
			assertTrue( "fungus to glow at " + a, fungus > 0 );
			fishTotal += fish;
		}
		assertTrue( "the blind fish have deep pools somewhere in the caves", fishTotal > 0 );
	}

	@Test
	public void peakLifeKeepsToItsGroundOnRealWindows(){
		float shift = WorldModel.calendarShift( GameCalendar.Season.SUMMER, 0.5f );
		int[] peak = PeakLife.findPeak( SEED, 7, shift );
		assertNotNull( peak );
		int[] meadow = PeakLife.findMeadowEdge( SEED, 3, shift );
		assertNotNull( meadow );
		int[][] spots = {
				{ peak[2], peak[0] - 88, peak[1] - 88 },
				{ peak[2], peak[0] - 40, peak[1] - 130 },
				{ 3, meadow[0] - 88, meadow[1] - 88 },
				{ 4, peak[0] - 88, peak[1] - 88 },
				{ 1, -88, -88 } };
		int seaTotal = 0, marmotTotal = 0;
		for (int[] s : spots){
			int a = s[0];
			WindowGenerator.Window win = WindowGenerator.generate( SEED, a, s[1], s[2], shift );
			int[] m = win.terrain;
			int[] buf = new int[W * H], edges = new int[W * H];
			int n = PeakLife.seaCells( m, W, H, 0, 0, W - 1, H - 1, buf, edges );
			for (int i = 0; i < n; i++){
				int c = buf[i];
				assertEquals( "clouds lie over air at +" + a, Terrain.CHASM, m[c] );
				if (m[c - W] != Terrain.CHASM){
					assertTrue( "below the cliff face's lip at +" + a, (edges[i] & CloudSea.CUT_TOP) != 0
							&& (edges[i] & 31) >= (m[c - W] == Terrain.WALL ? 14 : 4) );
				}
				assertEquals( "never over a way between slices at +" + a, WindowGenerator.LINK_NONE, win.link[c] );
			}
			if (a >= PeakLife.CLOUD_ALTITUDE) seaTotal += n;
			int down = PeakLife.downwind( W, 90f );
			int marmots = 0, ridges = 0;
			for (int y = 1; y < H - 1; y++){
				for (int x = 1; x < W - 1; x++){
					int c = x + y * W;
					if (PeakLife.marmotGround( m, W, c, win.frozen[c] )){
						marmots++;
						assertTrue( "thawed", !win.frozen[c] );
						int t = m[c];
						assertTrue( "meadow", t == Terrain.GRASS || t == Terrain.EMPTY || t == Terrain.FLOWER_PATCH );
						assertTrue( "a boulder beside", PeakLife.boulderBeside( m, W, c ) >= 0 );
						assertTrue( "nothing drawn over it from below", m[c + W] != Terrain.BOULDER && m[c + W] != Terrain.WALL );
					}
					if (PeakLife.ridge( m, W, H, c, win.frozen[c], down )){
						ridges++;
						assertEquals( Terrain.WALL, m[c] );
						assertTrue( win.frozen[c] );
					}
				}
			}
			marmotTotal += marmots;
			System.out.println( "peaks +" + a + " at " + s[1] + "," + s[2] + ": sea cells " + n + ", marmot cells " + marmots + ", ridges " + ridges );
		}
		assertTrue( "from a high peak there is air below for the clouds", seaTotal > 0 );
		assertTrue( "the +3 meadow has marmot ground", marmotTotal > 0 );
	}

	//a rebase moves the window 32 cells; the glows lit are keyed by world cell, and the pick
	//round the hero must be the same world cells from either window, or they would switch
	@Test
	public void glowsPickTheSameWorldCellsFromAnyWindow(){
		for (int a : new int[]{ -3, -9 }){
			int[] spot = CaveLife.findChamber( SEED, a );
			assertNotNull( spot );
			int[][] origins = { { spot[0] - 88, spot[1] - 88 }, { spot[0] - 56, spot[1] - 120 }, { spot[0] - 120, spot[1] - 56 } };
			ArrayList<long[]> picks = new ArrayList<>();
			for (int[] o : origins){
				final int ox = o[0], oy = o[1];
				final int[] m = WindowGenerator.generate( SEED, a, ox, oy, 0f ).terrain;
				boolean[] seen = new boolean[W * H];
				Arrays.fill( seen, true );
				int hcell = (spot[0] - ox) + (spot[1] - oy) * W;
				IntPredicate eligible = c -> CaveLife.glows( SEED, ox + c % W, oy + c / W );
				IntUnaryOperator glint = c -> m[c] == Terrain.MINE_CRYSTAL ? CaveLife.glintColour( a ) : 0;
				int[] pick = CaveLife.pickGlows( m, seen, W, H, hcell, eligible, glint, c -> false );
				long[] world = new long[pick.length];
				for (int i = 0; i < pick.length; i++) world[i] = OverworldLevel.worldKey( ox + pick[i] % W, oy + pick[i] / W );
				Arrays.sort( world );
				picks.add( world );
			}
			assertTrue( "something glows by the chamber at " + a, picks.get( 0 ).length > 0 );
			for (int i = 1; i < picks.size(); i++){
				assertTrue( "the same glows from window " + i + " at " + a, Arrays.equals( picks.get( 0 ), picks.get( i ) ) );
			}
		}
	}

	@Test
	public void everySliceHasALifeAndTheSurfaceKeepsItsOwn(){
		for (int a = -WorldLayers.MAX_BELOW; a <= WorldLayers.MAX_ABOVE; a++){
			OverworldLevel l = new OverworldLevel( a );
			assertTrue( "the field is put on slice " + a, l.hasCritters() );
		}
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, 100, 100, 0f );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, 100, 100, 0f, GameCalendar.Season.SUMMER, w.terrain, W, H );
		assertNotNull( ow );
		Dungeon.level = ow;
		OverworldCritters.Field f = new OverworldCritters.Field( ow );
		try {
			assertNull( "the surface runs its own birds and hares", f.life );
		} finally {
			f.destroy();
		}
	}

	@Test
	public void theWorldsCavesHaveNoCityOverlay(){
		for (int a : new int[]{ -1, -6, -12 }){
			Dungeon.level = new OverworldLevel( a );
			if (ClimateManager.feelsLikeTemp() >= ClimateManager.HEAT_RAY_TEMP) continue;
			assertEquals( "slice " + a, WeatherOverlayAmbient.NONE, ClimateManager.ambientType() );
		}
	}
}
