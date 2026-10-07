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
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The places on the mountains (MountainSites) are pure functions of the world: these pin that
 * every one is found near the origin, sits on its own slice's ground clear of every way between
 * the slices, is laid the same through every window, that a cairn stands on a true summit, and
 * that resolving them costs a window little.
 */
public class MountainSitesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private static final EnumMap<MountainSites.Kind, MountainSites.Site> NEAREST = new EnumMap<>( MountainSites.Kind.class );

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		for (MountainSites.Kind k : MountainSites.Kind.values()){
			MountainSites.Site s = MountainSites.nearest( SEED, k, 0, 0, 48 );
			assertNotNull( "no " + k + " within 48 sectors of the origin", s );
			NEAREST.put( k, s );
		}
	}

	private static WindowGenerator.Window gen( int altitude, int ox, int oy ){
		return WindowGenerator.generate( SEED, altitude, ox, oy, 0f );
	}

	//every site of every slice in the sectors round the nearest of each kind
	private static ArrayList<MountainSites.Site> around(){
		ArrayList<MountainSites.Site> out = new ArrayList<>();
		for (MountainSites.Site n : NEAREST.values()){
			int sx0 = Math.floorDiv( n.wx, MountainSites.SECTOR ), sy0 = Math.floorDiv( n.wy, MountainSites.SECTOR );
			for (int sy = sy0 - 2; sy <= sy0 + 2; sy++){
				for (int sx = sx0 - 2; sx <= sx0 + 2; sx++){
					for (int a = 1; a <= WorldLayers.MAX_ABOVE; a++){
						MountainSites.Site s = MountainSites.siteOf( SEED, sx, sy, a );
						if (s != null && !out.contains( s )) out.add( s );
					}
					MountainSites.Site top = MountainSites.summitOf( SEED, sx, sy );
					if (top != null && !out.contains( top )) out.add( top );
				}
			}
		}
		return out;
	}

	@Test
	public void everyKindExistsNearTheOriginAndIsDeterministic(){
		ArrayList<MountainSites.Site> before = around();
		//a new world, and back: everything is resolved again from nothing
		MountainSites.checkSeed( SEED + 1 );
		MountainSites.checkSeed( SEED );
		for (MountainSites.Kind k : MountainSites.Kind.values()){
			assertEquals( NEAREST.get( k ), MountainSites.nearest( SEED, k, 0, 0, 48 ) );
		}
		assertEquals( before, around() );
	}

	@Test
	public void altitudesAndClimatesHold(){
		MountainSites.Probe p = new MountainSites.Probe( SEED );
		for (MountainSites.Site s : around()){
			int a = s.altitude;
			assertTrue( s.kind + " at " + a, s.kind.lo <= a && a <= s.kind.hi );
			switch (s.kind){
				case HERMIT:
					assertFalse( "a hut stands below the snow line", p.frozenMean( s.wx, s.wy, a ) );
					break;
				case SPRINGS:
					assertTrue( s.kind + " lies in the snows", p.frozenMean( s.wx, s.wy, a ) );
					break;
				case CLIMBER:
					//his pack's drift is drawn in: the snow on him and on it never melts
					assertTrue( "a climber lies in the snows all year",
							p.frozenAllYear( s.wx, s.wy, a ) && p.frozenAllYear( s.wx + 1, s.wy, a ) );
					break;
				case TOWER:
					assertTrue( "a tower stands on a ridge",
							(p.band( s.wx + 9, s.wy ) < a && p.band( s.wx - 9, s.wy ) < a)
							|| (p.band( s.wx, s.wy + 9 ) < a && p.band( s.wx, s.wy - 9 ) < a) );
					break;
				case PASS: {
					assertEquals( a, p.band( s.ax, s.ay ) );
					boolean tunnel = false;
					for (int d = 0; d < 4; d++) tunnel |= p.tunnel( s.ax + MountainSites.DX[d], s.ay + MountainSites.DY[d], a );
					assertTrue( "a waystation's path ends at a tunnel", tunnel );
					break;
				}
				case EYRIE: {
					boolean lip = false;
					for (int d = 0; d < 4; d++) lip |= p.band( s.wx + MountainSites.DX[d], s.wy + MountainSites.DY[d] ) < a;
					assertTrue( "a nest is on the lip of a drop", lip );
					break;
				}
				default:
			}
		}
	}

	@Test
	public void footprintsSitOnTheirOwnGroundClearOfTheWays(){
		MountainSites.Probe p = new MountainSites.Probe( SEED );
		for (MountainSites.Site s : around()){
			int a = s.altitude;
			//a waystation's reach runs out along its path: its shell and yard are the footprint
			int x0 = s.kind == MountainSites.Kind.PASS ? s.wx - 3 : s.x0, x1 = s.kind == MountainSites.Kind.PASS ? s.wx + 3 : s.x1;
			int y0 = s.kind == MountainSites.Kind.PASS ? s.wy - 3 : s.y0, y1 = s.kind == MountainSites.Kind.PASS ? s.wy + 3 : s.y1;
			for (int y = y0; y <= y1; y++){
				for (int x = x0; x <= x1; x++){
					if (s.kind == MountainSites.Kind.CAIRN && x != s.wx && y != s.wy) continue;
					assertEquals( s.kind + " on its own band", a, p.band( x, y ) );
					assertFalse( s.kind + " on a tarn", WorldModel.tarnAt( SEED, x, y, a ) );
				}
			}
			for (int y = y0 - MountainSites.MARGIN; y <= y1 + MountainSites.MARGIN; y++){
				for (int x = x0 - MountainSites.MARGIN; x <= x1 + MountainSites.MARGIN; x++){
					if (s.kind == MountainSites.Kind.CAIRN && Math.max( Math.abs( x - s.wx ), Math.abs( y - s.wy ) ) > MountainSites.MARGIN) continue;
					assertFalse( s.kind + " beside a way at " + x + "," + y, p.way( x, y, a ) );
				}
			}
		}
	}

	@Test
	public void cairnIsATrueLocalSummit(){
		int cairns = 0;
		for (MountainSites.Site s : around()){
			if (s.kind != MountainSites.Kind.CAIRN) continue;
			cairns++;
			float top = WorldModel.elevation( SEED, s.wx, s.wy );
			long h = WindowGenerator.dressHash( SEED, s.wx, s.wy, 0xCA1A5L );
			for (int dy = -MountainSites.SUMMIT_R; dy <= MountainSites.SUMMIT_R; dy++){
				for (int dx = -MountainSites.SUMMIT_R; dx <= MountainSites.SUMMIT_R; dx++){
					if (dx == 0 && dy == 0) continue;
					float e = WorldModel.elevation( SEED, s.wx + dx, s.wy + dy );
					long eh = WindowGenerator.dressHash( SEED, s.wx + dx, s.wy + dy, 0xCA1A5L );
					assertTrue( "higher ground beside a summit", e < top || (e == top && eh < h) );
				}
			}
			assertEquals( WorldLayers.band( top ), s.altitude );
			assertTrue( s.altitude >= 2 );
			assertEquals( MountainSites.feet( top ), s.feet() );
			assertEquals( Math.floorDiv( s.wx, MountainSites.SECTOR ), Math.floorDiv( s.wx, 64 ) );
		}
		assertTrue( cairns > 0 );
		assertEquals( 12000, MountainSites.feet( 1f ) );
	}

	@Test
	public void aClampedTopHasOneCairn(){
		//a top whose elevation clamps at 1: every cell of it ties, and the hash alone picks the summit
		int[] flat = null;
		for (int r = 0; r <= 4000 && flat == null; r += 16){
			for (int i = -r; i <= r && flat == null; i += 16){
				int[][] ring = { { i, -r }, { i, r }, { -r, i }, { r, i } };
				for (int[] c : ring){
					if (WorldModel.elevation( SEED, c[0], c[1] ) == 1f){ flat = c; break; }
				}
			}
		}
		assertNotNull( "no clamped top near the origin", flat );
		//the clamped top round it
		ArrayList<int[]> top = new ArrayList<>();
		java.util.HashSet<Long> seen = new java.util.HashSet<>();
		java.util.ArrayDeque<int[]> open = new java.util.ArrayDeque<>();
		open.add( flat );
		seen.add( OverworldLevel.worldKey( flat[0], flat[1] ) );
		while (!open.isEmpty() && top.size() < 4000){
			int[] c = open.poll();
			top.add( c );
			for (int d = 0; d < 4; d++){
				int x = c[0] + MountainSites.DX[d], y = c[1] + MountainSites.DY[d];
				if (seen.add( OverworldLevel.worldKey( x, y ) ) && WorldModel.elevation( SEED, x, y ) == 1f) open.add( new int[]{ x, y } );
			}
		}
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
		for (int[] c : top){
			x0 = Math.min( x0, c[0] ); x1 = Math.max( x1, c[0] );
			y0 = Math.min( y0, c[1] ); y1 = Math.max( y1, c[1] );
		}
		ArrayList<MountainSites.Site> on = new ArrayList<>();
		for (int sy = Math.floorDiv( y0, 64 ) - 1; sy <= Math.floorDiv( y1, 64 ) + 1; sy++){
			for (int sx = Math.floorDiv( x0, 64 ) - 1; sx <= Math.floorDiv( x1, 64 ) + 1; sx++){
				MountainSites.Site s = MountainSites.summitOf( SEED, sx, sy );
				if (s != null && seen.contains( OverworldLevel.worldKey( s.wx, s.wy ) )
						&& WorldModel.elevation( SEED, s.wx, s.wy ) == 1f) on.add( s );
			}
		}
		assertTrue( "the clamped top at " + flat[0] + "," + flat[1] + " (" + top.size() + " cells) has no cairn", on.size() >= 1 );
		//two cairns on one top only where it is too wide for one summit's reach
		for (int i = 0; i < on.size(); i++){
			for (int j = i + 1; j < on.size(); j++){
				assertTrue( Math.max( Math.abs( on.get( i ).wx - on.get( j ).wx ), Math.abs( on.get( i ).wy - on.get( j ).wy ) )
						> MountainSites.SUMMIT_R );
			}
		}
		if (x1 - x0 <= MountainSites.SUMMIT_R && y1 - y0 <= MountainSites.SUMMIT_R) assertEquals( 1, on.size() );
	}

	@Test
	public void sitesNeverTouchAWayAndLayAsBuilt(){
		for (MountainSites.Site s : NEAREST.values()){
			int a = s.altitude;
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			WindowGenerator.Window w = gen( a, ox, oy );
			WindowGenerator.Window below = gen( a - 1, ox, oy );
			WindowGenerator.Window above = WorldLayers.exists( a + 1 ) ? gen( a + 1, ox, oy ) : null;
			assertTrue( s.kind + " is laid in its window", w.sites.contains( s ) );
			int built = 0;
			for (int y = 1; y < H - 1; y++){
				for (int x = 1; x < W - 1; x++){
					int c = x + y * W;
					boolean b = MountainSites.builtAt( SEED, a, ox + x, oy + y );
					if (b){
						built++;
						int t = w.terrain[c];
						assertTrue( "built " + t, t == Terrain.WALL || t == Terrain.BOULDER || t == Terrain.DOOR || t == Terrain.SIGN );
						//nothing built stands on or beside a way
						for (int dy = -1; dy <= 1; dy++){
							for (int dx = -1; dx <= 1; dx++){
								assertEquals( WindowGenerator.LINK_NONE, w.link[c + dx + dy * W] );
							}
						}
					} else if (w.terrain[c] == Terrain.WALL){
						assertTrue( "a wall of the slice's own ground that no site built", w.band[c] > a );
					}
					//the ways still match the slices either side
					if (w.link[c] == WindowGenerator.LINK_STAIR_DOWN){
						assertEquals( Terrain.EXIT, w.terrain[c] );
						assertEquals( WindowGenerator.LINK_STAIR_UP, below.link[c] );
					}
					if (w.link[c] == WindowGenerator.LINK_STAIR_UP){
						assertEquals( Terrain.ENTRANCE, w.terrain[c] );
						if (above != null) assertEquals( WindowGenerator.LINK_STAIR_DOWN, above.link[c] );
					}
				}
			}
			boolean walls = s.kind == MountainSites.Kind.HERMIT || s.kind == MountainSites.Kind.TOWER || s.kind == MountainSites.Kind.PASS;
			if (walls) assertTrue( s.kind + " builds", built > 0 );
		}
	}

	@Test
	public void theRuinTheShelterAndTheClimberLookWhatTheyAre(){
		int towers = 0, snowed = 0, passes = 0, climbers = 0;
		for (MountainSites.Site s : around()){
			int a = s.altitude;
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			int mid = W / 2 + (H / 2) * W;
			switch (s.kind){
				case TOWER: {
					towers++;
					//roofless: winter's snow lies in the ring as round it, and the stump of its stair
					//marks the top cell
					WindowGenerator.Window w = WindowGenerator.generate( SEED, a, ox, oy, WorldModel.WINTER_SHIFT );
					int[][] dress = WindowGenerator.dress( SEED, ox, oy, w.terrain, w, GameCalendar.Season.WINTER );
					assertEquals( OverworldDress.TOWER_STAIR, dress[1][mid] );
					for (int dy = -2; dy <= 2; dy++){
						for (int dx = -2; dx <= 2; dx++){
							int c = mid + dx + dy * W;
							if ((Terrain.flags[w.terrain[c]] & Terrain.SOLID) != 0) continue;
							assertEquals( "the tower's floor at " + dx + "," + dy,
									w.frozen[c] ? Terrain.SNOW : Terrain.EMPTY_DECO, w.terrain[c] );
							if (w.frozen[c]) snowed++;
						}
					}
					break;
				}
				case PASS: {
					passes++;
					//the hearth stands under the north wall, its chimney straight above it, its smoke
					//over its flames (SettlementAmbience.collectPeaks reads the same cell)
					WindowGenerator.Window w = gen( a, ox, oy );
					int[] hearth = MountainSites.hearthCell( SEED, s );
					int hc = (hearth[0] - ox) + (hearth[1] - oy) * W;
					assertEquals( Terrain.EMBERS, w.terrain[hc] );
					assertEquals( "a wall behind the fire", Terrain.WALL, w.terrain[hc - W] );
					int[][] dress = WindowGenerator.dress( SEED, ox, oy, w.terrain, w, GameCalendar.Season.WINTER );
					int chimney = MountainSites.chimneyCell( SEED, s, w.terrain, W, H, ox, oy );
					if (chimney != -1){
						assertEquals( hc - 2 * W, chimney );
						assertEquals( OverworldDress.VILLAGE_CHIMNEY, dress[2][chimney] );
					}
					for (int c = 0; c < W * H; c++){
						if (c != chimney && dress[2][c] == OverworldDress.VILLAGE_CHIMNEY){
							assertTrue( "no stack off the hearth", Math.abs( (c % W) - W / 2 ) > 3 || Math.abs( (c / W) - H / 2 ) > 3 );
						}
					}
					break;
				}
				case CLIMBER: {
					climbers++;
					//at midsummer he and his pack still lie in snow
					WindowGenerator.Window w = WindowGenerator.generate( SEED, a, ox, oy, WorldModel.SUMMER_SHIFT );
					assertTrue( w.frozen[mid] && w.frozen[mid + 1] );
					assertEquals( Terrain.SNOW, w.terrain[mid] );
					break;
				}
				default:
			}
		}
		assertTrue( towers > 0 && passes > 0 && climbers > 0 );
		assertTrue( "some tower is snowed in in winter", snowed > 0 );

		//every way a shelter can face: the fire in a corner under the north wall, never in the
		//doorway's way nor on the bedroll
		for (int dir = 0; dir < 4; dir++){
			MountainSites.Site s = new MountainSites.Site( MountainSites.Kind.PASS, 2, 40, 40, dir, 40, 50, 0, 37, 37, 43, 50 );
			int[] hearth = MountainSites.hearthCell( SEED, s );
			int[] bed = MountainSites.bedrollCell( s );
			assertEquals( s.wy - 1, hearth[1] );
			assertEquals( 1, Math.max( Math.abs( hearth[0] - s.wx ), Math.abs( hearth[1] - s.wy ) ) );
			assertFalse( hearth[0] == s.wx + MountainSites.DX[dir] && hearth[1] == s.wy + MountainSites.DY[dir] );
			assertFalse( hearth[0] == bed[0] && hearth[1] == bed[1] );
		}
	}

	@Test
	public void overlappingWindowsAgreeOnSites(){
		for (MountainSites.Site s : NEAREST.values()){
			int a = s.altitude;
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			WindowGenerator.Window one = gen( a, ox, oy );
			WindowGenerator.Window two = gen( a, ox + 32, oy - 32 );
			for (int y = 1; y < H - 1; y++){
				for (int x = 1; x < W - 1; x++){
					int x2 = x - 32, y2 = y + 32;
					if (x2 < 1 || y2 < 1 || x2 >= W - 1 || y2 >= H - 1) continue;
					int c = x + y * W, c2 = x2 + y2 * W;
					assertEquals( one.terrain[c], two.terrain[c2] );
					assertEquals( one.field[c], two.field[c2] );
					assertEquals( one.frozen[c], two.frozen[c2] );
					assertEquals( one.waterDepth[c], two.waterDepth[c2] );
				}
			}
		}
	}

	@Test
	public void rumoursPointAtRealPlaces(){
		MountainSites.Site h = NEAREST.get( MountainSites.Kind.HERMIT );
		ArrayList<MountainSites.Site> told = MountainSites.rumourCandidates( SEED, h.altitude, h.wx, h.wy, h.key );
		assertFalse( told.isEmpty() );
		for (MountainSites.Site s : told){
			assertFalse( s.key == h.key );
			MountainSites.Site real = s.kind == MountainSites.Kind.CAIRN
					? MountainSites.summitOf( SEED, Math.floorDiv( s.wx, 64 ), Math.floorDiv( s.wy, 64 ) )
					: MountainSites.siteOf( SEED, Math.floorDiv( s.wx, 64 ), Math.floorDiv( s.wy, 64 ), s.altitude );
			assertEquals( s, real );
			String text = MountainSites.rumourOf( SEED, s, h.altitude, h.wx, h.wy );
			int dx = s.seeX - h.wx, dy = s.seeY - h.wy;
			assertTrue( text, text.contains( " " + MountainSites.paces( dx, dy ) + " paces" ) );
			assertTrue( text, text.contains( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( MountainSites.class,
					"dir_" + MountainSites.compass( dx, dy ) ) ) );
		}
		assertEquals( "n", MountainSites.compass( 0, -10 ) );
		assertEquals( "se", MountainSites.compass( 7, 7 ) );
		assertEquals( "w", MountainSites.compass( -10, 1 ) );
		assertEquals( 10, MountainSites.paces( 1, 0 ) );
		assertEquals( 140, MountainSites.paces( 100, 100 ) );
		//the same rumour all day, from the same hermit
		assertEquals( MountainSites.rumour( SEED, h.altitude, h.wx, h.wy, h.key, 5 ),
				MountainSites.rumour( SEED, h.altitude, h.wx, h.wy, h.key, 5 ) );
	}

	@Test
	public void elevationAloneIsTheSampledOne(){
		WorldModel.Sample a = new WorldModel.Sample(), b = new WorldModel.Sample();
		for (int i = 0; i < 10000; i++){
			int x = Math.floorMod( i * 7919, 6000 ) - 3000, y = Math.floorMod( i * 104729, 6000 ) - 3000;
			assertEquals( WorldModel.sample( SEED, x, y, 0f, a ).elev, WorldModel.elevationOf( SEED, x, y, b ), 0f );
			assertEquals( WorldModel.sample( SEED, x, y, -0.18f, a ).elev, WorldModel.elevationOf( SEED, x, y, b ), 0f );
		}
	}

	@Test
	public void resolvingTheSitesCostsAWindowLittle(){
		//a window across sector borders on a low and a high slice, its sites resolved from nothing
		//against the same window with every site already known
		for (int a : new int[]{ 2, 8 }){
			MountainSites.Site c = NEAREST.get( MountainSites.Kind.CAIRN );
			int ox = c.wx - W / 2 + 37, oy = c.wy - H / 2 - 21;
			gen( a, ox, oy );
			long warm = Long.MAX_VALUE, cold = Long.MAX_VALUE;
			for (int i = 0; i < 5; i++){
				long t = System.nanoTime();
				gen( a, ox, oy );
				warm = Math.min( warm, System.nanoTime() - t );
				MountainSites.checkSeed( SEED + 1 );
				MountainSites.checkSeed( SEED );
				t = System.nanoTime();
				gen( a, ox, oy );
				cold = Math.min( cold, System.nanoTime() - t );
			}
			System.out.println( "mountain window +" + a + ": " + warm / 1000000 + " ms known, " + cold / 1000000 + " ms resolving" );
			assertTrue( "+" + a + ": " + cold / 1000000 + " ms against " + warm / 1000000, cold <= warm * 3 / 2 );
		}
		//a hermit's rumour, from nothing
		MountainSites.Site h = NEAREST.get( MountainSites.Kind.HERMIT );
		MountainSites.checkSeed( SEED + 1 );
		MountainSites.checkSeed( SEED );
		long t = System.nanoTime();
		MountainSites.rumour( SEED, h.altitude, h.wx, h.wy, h.key, 3 );
		long ms = (System.nanoTime() - t) / 1000000;
		System.out.println( "a hermit's rumour from nothing: " + ms + " ms" );
		assertTrue( ms + " ms", ms < 1500 );
	}
}
