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
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The magic map's route planner (MarchPlanner) on synthetic ground - a window known exactly, the
 * untouched world beyond it - and on a real world: it goes round what the old greedy legs paced in
 * front of, stops at the nearest shore of an island, takes roads where they pay, plans the same way
 * every time, and plans 600 cells of a real world in milliseconds.
 */
public class MarchPlannerTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = OverworldLevel.WIDTH, H = OverworldLevel.HEIGHT;

	// ------------------------------------------------------------------ synthetic ground

	//a window's worth of ground at world (ox, oy), known exactly as the live window is; beyond it the
	//untouched world, read as the planner reads it (on its lattice) from `beyond`
	private static final class Grid {
		final int ox, oy;
		final int[] t = new int[W * H];
		java.util.function.IntBinaryOperator beyond = (x, y) -> Terrain.EMPTY;

		Grid( int ox, int oy, int fill ){
			this.ox = ox;
			this.oy = oy;
			Arrays.fill( t, fill );
			for (int x = 0; x < W; x++) t[x] = t[x + (H - 1) * W] = Terrain.WALL;
			for (int y = 0; y < H; y++) t[y * W] = t[W - 1 + y * W] = Terrain.WALL;
		}

		//cells x0..x1, y0..y1 of the window
		Grid rect( int x0, int y0, int x1, int y1, int terrain ){
			for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) t[x + y * W] = terrain;
			return this;
		}

		boolean open( int cell ){
			return MarchPlanner.LAND.cost( t[cell] ) > 0;
		}

		MarchPlanner.LongLongMap real(){
			MarchPlanner.LongLongMap m = new MarchPlanner.LongLongMap();
			int B = MarchPlanner.B;
			float[] c = new float[B * B];
			//the blocks whole inside the window's ring, as MarchPlanner.readWindow reads them
			int bx0 = Math.floorDiv( ox + B, B ), bx1 = Math.floorDiv( ox + W - 1, B ) - 1;
			int by0 = Math.floorDiv( oy + B, B ), by1 = Math.floorDiv( oy + H - 1, B ) - 1;
			for (int by = by0; by <= by1; by++){
				for (int bx = bx0; bx <= bx1; bx++){
					for (int i = 0; i < B * B; i++){
						c[i] = MarchPlanner.LAND.cost( t[(bx * B + i % B - ox) + (by * B + i / B - oy) * W] );
					}
					m.put( MarchPlanner.blockKey( bx, by ), MarchPlanner.realWord( c ) );
				}
			}
			return m;
		}

		MarchPlanner.Ground ground(){
			return new MarchPlanner.Ground(){
				@Override
				protected int terrain( int wx, int wy, Object scratch ){
					int x = wx - ox, y = wy - oy;
					if (x >= 0 && y >= 0 && x < W && y < H) return t[x + y * W];
					return beyond.applyAsInt( wx, wy );
				}
			};
		}

		MarchPlanner.Route plan( int sx, int sy, int gx, int gy ){
			return plan( ground(), sx, sy, gx, gy, new MarchPlanner.LongLongMap() );
		}

		MarchPlanner.Route plan( MarchPlanner.Ground g, int sx, int sy, int gx, int gy, MarchPlanner.LongLongMap shut ){
			return MarchPlanner.plan( new MarchPlanner.Request( g, MarchPlanner.LAND,
					ox + sx, oy + sy, ox + gx, oy + gy, real(), shut, MarchPlanner.BUDGET ) );
		}

		//steps over open cells from a window cell, Integer.MAX_VALUE where it does not reach
		int[] steps( int from ){
			int[] d = new int[W * H];
			Arrays.fill( d, Integer.MAX_VALUE );
			ArrayDeque<Integer> q = new ArrayDeque<>();
			d[from] = 0;
			q.add( from );
			while (!q.isEmpty()){
				int c = q.poll();
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int n = c + dx + dy * W;
						if (n < 0 || n >= W * H || Math.abs( n % W - c % W ) > 1 || !open( n ) || d[n] != Integer.MAX_VALUE) continue;
						d[n] = d[c] + 1;
						q.add( n );
					}
				}
			}
			return d;
		}

		//the step from `at` toward `to` along the shortest way (the hero's own pathfinding), -1 when none
		int stepToward( int at, int to ){
			int[] d = steps( to );
			int best = -1;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int n = at + dx + dy * W;
					if (n == at || n < 0 || n >= W * H || d[n] == Integer.MAX_VALUE) continue;
					if (best == -1 || d[n] < d[best]) best = n;
				}
			}
			return best != -1 && d[best] < d[at] ? best : -1;
		}
	}

	//walks a route as the march does (MarchPlanner.advance, leg), knowing the whole window, a step at a
	//time along his own shortest way to each leg; the cells he stood on, ending within a step of the goal
	//or after `limit` steps
	private static int[] follow( Grid g, MarchPlanner.Route r, int sx, int sy, int gx, int gy, int limit ){
		int at = sx + sy * W, along = 0;
		int[] trail = new int[limit + 1];
		int n = 0;
		trail[n++] = at;
		while (n <= limit){
			int x = at % W, y = at / W;
			if (Math.max( Math.abs( x - gx ), Math.abs( y - gy ) ) <= 1) break;
			along = MarchPlanner.advance( r, along, g.ox + x, g.oy + y );
			int leg = MarchPlanner.leg( r, along, g.ox + x, g.oy + y, g.steps( at ), g.ox, g.oy, W, H );
			if (leg == -1) break;
			int step = g.stepToward( at, (r.xs[leg] - g.ox) + (r.ys[leg] - g.oy) * W );
			if (step == -1) break;
			at = step;
			trail[n++] = at;
		}
		return Arrays.copyOf( trail, n );
	}

	//the march before the planner (the old OverworldLevel.navigableLeg): a leg of 10 cells toward the
	//waypoint's bearing, else turned 45 then 90 degrees either side, else 5 long, walked to its end before
	//the next is picked; the cells he stood on, ending within a step of the goal or after `limit` steps
	private static int[] followGreedy( Grid g, int sx, int sy, int gx, int gy, int limit ){
		int[][] octants = { {1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1} };
		int at = sx + sy * W;
		int[] trail = new int[limit + 1];
		int n = 0;
		trail[n++] = at;
		while (n <= limit){
			int x = at % W, y = at / W;
			if (Math.max( Math.abs( x - gx ), Math.abs( y - gy ) ) <= 1) break;
			int octant = Math.floorMod( Math.round( (float)(Math.atan2( gy - y, gx - x ) / (Math.PI / 4)) ), 8 );
			int[] reach = g.steps( at );
			int leg = -1;
			legs:
			for (int len : new int[]{ 10, 5 }){
				for (int rot : new int[]{ 0, 1, -1, 2, -2 }){
					int[] dir = octants[Math.floorMod( octant + rot, 8 )];
					int tx = Math.max( 2, Math.min( W - 3, x + dir[0] * len ) ), ty = Math.max( 2, Math.min( H - 3, y + dir[1] * len ) );
					int target = tx + ty * W;
					if (!g.open( target )){
						for (int dy = -1; dy <= 1 && !g.open( target ); dy++){
							for (int dx = -1; dx <= 1; dx++){
								if (g.open( target + dx + dy * W ) && target + dx + dy * W != at){
									target = target + dx + dy * W;
									break;
								}
							}
						}
						if (!g.open( target )) continue;
					}
					if (target != at && reach[target] != Integer.MAX_VALUE){
						leg = target;
						break legs;
					}
				}
			}
			if (leg == -1) break;
			while (at != leg && n <= limit){
				at = g.stepToward( at, leg );
				trail[n++] = at;
			}
		}
		return Arrays.copyOf( trail, n );
	}

	private static boolean arrived( int[] trail, int gx, int gy ){
		int last = trail[trail.length - 1];
		return Math.max( Math.abs( last % W - gx ), Math.abs( last / W - gy ) ) <= 1;
	}

	//the most times any one cell was stood on
	private static int mostVisits( int[] trail ){
		HashMap<Integer, Integer> seen = new HashMap<>();
		int most = 0;
		for (int c : trail) most = Math.max( most, seen.merge( c, 1, Integer::sum ) );
		return most;
	}

	//a U of deep water whose cup holds the hero and opens away from the waypoint, which lies past its bottom
	private static Grid uLake(){
		return new Grid( 1200, 1200, Terrain.EMPTY )
				.rect( 64, 50, 69, 106, Terrain.DEEP_WATER )
				.rect( 107, 50, 112, 106, Terrain.DEEP_WATER )
				.rect( 64, 101, 112, 106, Terrain.DEEP_WATER );
	}

	// ------------------------------------------------------------------ the tests

	@Test
	public void aUShapedLakeIsWalkedRoundWhereTheGreedyLegsPaced(){
		Grid g = uLake();
		int sx = 88, sy = 92, gx = 88, gy = 125;

		//the old march: every leg turned no more than 90 degrees off the waypoint's bearing, so out of the
		//cup's mouth (away from it) was never tried - it paced along the cup's bottom
		int[] greedy = followGreedy( g, sx, sy, gx, gy, 400 );
		assertFalse( "the greedy legs got there", arrived( greedy, gx, gy ) );
		assertTrue( "the greedy legs paced (most visits " + mostVisits( greedy ) + ")", mostVisits( greedy ) >= 10 );

		MarchPlanner.Route r = g.plan( sx, sy, gx, gy );
		assertEquals( MarchPlanner.Status.REACHED, r.status );
		assertTrue( r.endsAtGoal );
		//out of the mouth, round an arm
		int north = Integer.MAX_VALUE;
		for (int i = 0; i < r.size(); i++){
			int x = r.xs[i] - g.ox, y = r.ys[i] - g.oy;
			assertTrue( "route point " + x + "," + y + " on " + g.t[x + y * W], g.open( x + y * W ) );
			north = Math.min( north, y );
		}
		assertTrue( "the route leaves the cup by its mouth", north < 50 );

		int[] walked = follow( g, r, sx, sy, gx, gy, 400 );
		assertTrue( "the march gets there", arrived( walked, gx, gy ) );
		assertTrue( "no pacing (most visits " + mostVisits( walked ) + ")", mostVisits( walked ) <= 1 );
		assertTrue( "about the route's length: " + walked.length + " steps for " + r.along[r.last()],
				walked.length <= r.along[r.last()] * 1.1f + 5 );
	}

	@Test
	public void anIslandIsWalkedToItsNearestShore(){
		Grid g = new Grid( 3000, -2000, Terrain.EMPTY )
				.rect( 70, 60, 120, 110, Terrain.DEEP_WATER )
				.rect( 90, 80, 100, 90, Terrain.EMPTY );
		int sx = 40, sy = 85, gx = 95, gy = 85;
		MarchPlanner.Route r = g.plan( sx, sy, gx, gy );
		assertEquals( MarchPlanner.Status.NEAREST, r.status );
		assertFalse( r.endsAtGoal );
		int ex = r.xs[r.last()] - g.ox, ey = r.ys[r.last()] - g.oy;
		assertTrue( g.open( ex + ey * W ) );
		//on the shore: as near the island as the mainland goes (26 cells of water, on every side), the
		//shore nearest the hero of those
		assertEquals( 26, Math.max( Math.abs( ex - gx ), Math.abs( ey - gy ) ) );
		assertEquals( 69, ex );
		//planned again from there it gets no further: the march's word that it cannot be reached
		MarchPlanner.Route again = g.plan( ex, ey, gx, gy );
		assertEquals( MarchPlanner.Status.NEAREST, again.status );
		int ax = again.xs[again.last()] - g.ox, ay = again.ys[again.last()] - g.oy;
		assertTrue( Math.max( Math.abs( ax - ex ), Math.abs( ay - ey ) ) <= 2 );
	}

	@Test
	public void aFarIslandIsToldOutOfReachWithoutSearchingTheWholeMainland(){
		//open land every way and, 400 cells east, a lake with an island in it: the search from the hero
		//would cover the whole region before it ran out of land. The look from the island's side, after
		//PROBE nodes, finds its piece of the world closed and stops it there, at the shore nearest it
		Grid g = new Grid( 0, 0, Terrain.EMPTY );
		g.beyond = (x, y) -> x >= 490 && x <= 510 && y >= 80 && y <= 100 ? Terrain.EMPTY
				: x >= 440 && x <= 560 && y >= 30 && y <= 150 ? Terrain.DEEP_WATER : Terrain.EMPTY;
		MarchPlanner.Route r = g.plan( 88, 88, 500, 90 );
		assertEquals( MarchPlanner.Status.NEAREST, r.status );
		assertEquals( MarchPlanner.PROBE, r.expanded );
		int ex = r.xs[r.last()], ey = r.ys[r.last()];
		assertTrue( "the west shore, the hero's side: " + ex + "," + ey, ex >= 430 && ex < 440 && Math.abs( ey - 90 ) <= 4 );
	}

	@Test
	public void roadsAreTakenWhereTheyPay(){
		//open ground, and a road three cells off the straight way: a tenth quicker, it pays over 128 cells
		Grid g = new Grid( -500, 700, Terrain.EMPTY ).rect( 16, 91, 160, 92, Terrain.DIRT_PATH );
		MarchPlanner.Route r = g.plan( 24, 88, 152, 88 );
		assertEquals( MarchPlanner.Status.REACHED, r.status );
		assertTrue( "on the road: " + onRoad( g, r ) + " of " + r.size(), onRoad( g, r ) >= r.size() * 0.7f );

		//32 cells off it does not
		Grid far = new Grid( -500, 700, Terrain.EMPTY ).rect( 16, 120, 160, 121, Terrain.DIRT_PATH );
		MarchPlanner.Route straight = far.plan( 24, 88, 152, 88 );
		assertEquals( 0, onRoad( far, straight ) );
		assertTrue( straight.along[straight.last()] < 132 );
	}

	private static int onRoad( Grid g, MarchPlanner.Route r ){
		int n = 0;
		for (int i = 0; i < r.size(); i++){
			if (g.t[(r.xs[i] - g.ox) + (r.ys[i] - g.oy) * W] == Terrain.DIRT_PATH) n++;
		}
		return n;
	}

	@Test
	public void plansAreTheSameEveryTime(){
		Grid g = uLake();
		g.beyond = (x, y) -> (x / 37 + y / 23) % 5 == 0 ? Terrain.DEEP_WATER : Terrain.EMPTY;
		MarchPlanner.Route a = g.plan( 88, 92, 160, 400 );
		//a fresh ground (sampled cold), the same one again (warm), and on the planner's own thread
		MarchPlanner.Route b = g.plan( 88, 92, 160, 400 );
		MarchPlanner.Ground warm = g.ground();
		g.plan( warm, 88, 92, 160, 400, new MarchPlanner.LongLongMap() );
		MarchPlanner.Route c = g.plan( warm, 88, 92, 160, 400, new MarchPlanner.LongLongMap() );
		MarchPlanner.Route d = MarchPlanner.submit( new MarchPlanner.Request( g.ground(), MarchPlanner.LAND,
				g.ox + 88, g.oy + 92, g.ox + 160, g.oy + 400, g.real(), new MarchPlanner.LongLongMap(), MarchPlanner.BUDGET ),
				true, false ).await( 10_000 );
		for (MarchPlanner.Route o : new MarchPlanner.Route[]{ b, c, d }){
			assertEquals( a.status, o.status );
			assertArrayEquals( a.xs, o.xs );
			assertArrayEquals( a.ys, o.ys );
		}
	}

	@Test
	public void theUntouchedWorldBeyondTheWindowIsWalkedRound(){
		//past the window's east edge a lake 200 cells high stands across the way: read from the untouched
		//world, sampled, it is gone round, never waded
		Grid g = new Grid( 0, 0, Terrain.EMPTY );
		g.beyond = (x, y) -> x >= 260 && x < 330 && y >= -60 && y < 140 ? Terrain.DEEP_WATER : Terrain.EMPTY;
		MarchPlanner.Route r = g.plan( 88, 88, 420, 88 );
		assertEquals( MarchPlanner.Status.REACHED, r.status );
		for (int i = 0; i < r.size(); i++){
			assertFalse( "route point " + r.xs[i] + "," + r.ys[i] + " in the lake",
					r.xs[i] > 262 && r.xs[i] < 328 && r.ys[i] > -58 && r.ys[i] < 138 );
		}
		assertTrue( r.along[r.last()] > 360 );
	}

	@Test
	public void aBlockFoundShutIsGoneRound(){
		//a corridor through a wall of rock; with its block shut (a mob stood in it) the way round is taken
		Grid g = new Grid( 400, 400, Terrain.EMPTY ).rect( 20, 86, 156, 89, Terrain.WALL ).rect( 88, 86, 91, 89, Terrain.EMPTY );
		MarchPlanner.Route through = g.plan( 89, 70, 89, 110 );
		assertEquals( MarchPlanner.Status.REACHED, through.status );
		assertTrue( through.along[through.last()] < 45 );
		MarchPlanner.LongLongMap shut = new MarchPlanner.LongLongMap();
		shut.put( MarchPlanner.blockKey( Math.floorDiv( g.ox + 89, MarchPlanner.B ), Math.floorDiv( g.oy + 87, MarchPlanner.B ) ), 1 );
		MarchPlanner.Route round = g.plan( g.ground(), 89, 70, 89, 110, shut );
		assertEquals( MarchPlanner.Status.REACHED, round.status );
		assertTrue( round.along[round.last()] > 120 );
	}

	// ------------------------------------------------------------------ the real world

	//the exact blocks of a generated window, as the live one is read
	private static MarchPlanner.LongLongMap realOf( WindowGenerator.Window win, int ox, int oy ){
		Grid g = new Grid( ox, oy, Terrain.EMPTY );
		System.arraycopy( win.terrain, 0, g.t, 0, W * H );
		return g.real();
	}

	@Test
	public void sixHundredCellsOfARealWorldPlanInMilliseconds(){
		//three walks of some 600 cells on the test seed, from a window generated round the hero as the game
		//has it. Budget: a cold plan (the world beyond the window sampled from nothing) under a second, a warm
		//one (sampled already: the plans every window after the first) under 100 ms. Measured on a desktop:
		//about 20-50 ms cold, under 3 ms warm
		int[][] walks = { { 1208, -435, 640, -245 }, { 2272, -2695, 2469, -2129 }, { 2325, -3120, 1766, -2904 } };
		for (int[] w : walks){
			int ox = w[0] - W / 2, oy = w[1] - H / 2;
			MarchPlanner.LongLongMap real = realOf( WindowGenerator.generate( SEED, 0, ox, oy, 0f ), ox, oy );
			MarchPlanner.Ground ground = new MarchPlanner.WorldGround( SEED, 0, 0f );
			long t0 = System.nanoTime();
			MarchPlanner.Route cold = MarchPlanner.plan( new MarchPlanner.Request( ground, MarchPlanner.LAND,
					w[0], w[1], w[2], w[3], real, new MarchPlanner.LongLongMap(), MarchPlanner.BUDGET ) );
			long t1 = System.nanoTime();
			MarchPlanner.Route warm = MarchPlanner.plan( new MarchPlanner.Request( ground, MarchPlanner.LAND,
					w[0], w[1], w[2], w[3], real, new MarchPlanner.LongLongMap(), MarchPlanner.BUDGET ) );
			long t2 = System.nanoTime();
			System.out.println( "[march] " + w[0] + "," + w[1] + " -> " + w[2] + "," + w[3] + ": " + cold.status
					+ ", " + Math.round( cold.along[cold.last()] ) + " cells of route, " + cold.expanded + " nodes, "
					+ ground.sampled() + " chunks sampled; cold " + (t1 - t0) / 1_000_000 + " ms, warm " + (t2 - t1) / 1000 + " us" );
			assertEquals( MarchPlanner.Status.REACHED, cold.status );
			assertTrue( cold.endsAtGoal );
			assertArrayEquals( cold.xs, warm.xs );
			assertTrue( "cold plan " + (t1 - t0) / 1_000_000 + " ms", t1 - t0 < 1_000_000_000L );
			assertTrue( "warm plan " + (t2 - t1) / 1_000_000 + " ms", t2 - t1 < 100_000_000L );
		}
	}

	@Test
	public void aRealIslandIsFoundOutOfReachQuickly(){
		//an island of the test seed, 300 cells west of the hero across 60 cells of water every way: the
		//nearest shore to it, and planned again from there no nearer. Budget: under two seconds cold (the
		//planner proves it by running out of land; measured on a desktop about 0.3 s)
		MarchPlanner.Ground ground = new MarchPlanner.WorldGround( SEED, 0, 0f );
		long t0 = System.nanoTime();
		MarchPlanner.Route r = MarchPlanner.plan( new MarchPlanner.Request( ground, MarchPlanner.LAND,
				-2012, -1368, -1712, -1368, new MarchPlanner.LongLongMap(), new MarchPlanner.LongLongMap(), MarchPlanner.BUDGET ) );
		long t1 = System.nanoTime();
		int ex = r.xs[r.last()], ey = r.ys[r.last()];
		System.out.println( "[march] island: " + r.status + ", " + r.expanded + " nodes, " + ground.sampled() + " chunks, "
				+ (t1 - t0) / 1_000_000 + " ms cold; the nearest shore " + Math.max( Math.abs( ex + 1712 ), Math.abs( ey + 1368 ) ) + " cells off" );
		assertEquals( MarchPlanner.Status.NEAREST, r.status );
		assertFalse( r.endsAtGoal );
		assertTrue( Math.max( Math.abs( ex + 1712 ), Math.abs( ey + 1368 ) ) > 20 );
		assertTrue( "island plan " + (t1 - t0) / 1_000_000 + " ms", t1 - t0 < 2_000_000_000L );
		MarchPlanner.Route again = MarchPlanner.plan( new MarchPlanner.Request( ground, MarchPlanner.LAND,
				ex, ey, -1712, -1368, new MarchPlanner.LongLongMap(), new MarchPlanner.LongLongMap(), MarchPlanner.BUDGET ) );
		int ax = again.xs[again.last()], ay = again.ys[again.last()];
		assertTrue( MarchPlanner.octile( Math.abs( ax + 1712 ), Math.abs( ay + 1368 ) )
				> MarchPlanner.octile( Math.abs( ex + 1712 ), Math.abs( ey + 1368 ) ) - 1f );
	}

	@Test
	public void theSampledWorldAgreesWithTheGeneratedWindows(){
		//every block of sixteen generated windows: the untouched world's lattice reading against the window's
		//own cells. A block is open when half its cells or more are; a barrier when half of them are deep
		//water (a lake, a river's core) - those the lattice must shut, or the route would wade into them
		int blocks = 0, agree = 0, barriers = 0, caught = 0;
		MarchPlanner.Ground ground = new MarchPlanner.WorldGround( SEED, 0, 0f );
		for (int oy = -4000; oy <= 3500; oy += 2500){
			for (int ox = -4000; ox <= 3500; ox += 2500){
				WindowGenerator.Window win = WindowGenerator.generate( SEED, 0, ox, oy, 0f );
				MarchPlanner.LongLongMap real = realOf( win, ox, oy );
				for (int by = Math.floorDiv( oy + 4, 4 ); by * 4 + 3 <= oy + H - 2; by++){
					for (int bx = Math.floorDiv( ox + 4, 4 ); bx * 4 + 3 <= ox + W - 2; bx++){
						long word = real.get( MarchPlanner.blockKey( bx, by ), -1 );
						if (word == -1) continue;
						int deep = 0;
						for (int i = 0; i < 16; i++){
							if (win.terrain[(bx * 4 + i % 4 - ox) + (by * 4 + i / 4 - oy) * W] == Terrain.DEEP_WATER) deep++;
						}
						boolean open = Long.bitCount( word & 0xFFFF ) >= 8;
						boolean sampled = (ground.word( bx, by, MarchPlanner.LAND ) & 0xFFFF) != 0;
						blocks++;
						if (open == sampled) agree++;
						if (deep >= 8){
							barriers++;
							if (!sampled) caught++;
						}
					}
				}
			}
		}
		System.out.println( "[march] the lattice agrees with the windows on " + agree + " of " + blocks + " blocks ("
				+ 100f * agree / blocks + "%); it shuts " + caught + " of " + barriers + " deep-water blocks ("
				+ 100f * caught / barriers + "%)" );
		assertTrue( agree >= blocks * 0.95f );
		assertTrue( barriers > 1000 );
		assertTrue( caught >= barriers * 0.95f );
	}
}
