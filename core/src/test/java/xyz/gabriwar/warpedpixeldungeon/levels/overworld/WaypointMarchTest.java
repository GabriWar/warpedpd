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

import com.watabou.utils.PathFinder;
import com.watabou.utils.Signal;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sea.Afloat;
import xyz.gabriwar.warpedpixeldungeon.sea.Seafaring;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The magic map's march on a live window (OverworldLevel.marchDestination): the hero walks the legs it
 * hands him, a step a turn, by his own pathfinding over the ground he has seen, as Hero.act and getCloser
 * do. It goes round a lake its old legs paced in front of, stops at the nearest shore of an island with
 * its word for it, shuts a gap a monster stands in and goes round by another, sails a boat round a
 * spit of land and puts him ashore for the waypoint, and never marches a mirror.
 */
public class WaypointMarchTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = OverworldLevel.WIDTH, H = OverworldLevel.HEIGHT;
	//how far the stand-in for the hero's sight reaches (the surface's view distance)
	private static final int SIGHT = 20;

	private Level savedLevel;
	private Hero savedHero;
	private int savedDepth;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedDepth = Dungeon.depth;
		Actor.clear();
		hero = new Hero();
		hero.lvl = 12;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Dungeon.depth = savedDepth;
	}

	// ------------------------------------------------------------------ helpers

	//a window of open ground at world (ox, oy) with the walls of a window round it; painted by the caller
	private static int[] ground(){
		int[] map = new int[W * H];
		Arrays.fill( map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) map[x] = map[x + (H - 1) * W] = Terrain.WALL;
		for (int y = 0; y < H; y++) map[y * W] = map[W - 1 + y * W] = Terrain.WALL;
		return map;
	}

	private static void rect( int[] map, int x0, int y0, int x1, int y1, int terrain ){
		for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) map[x + y * W] = terrain;
	}

	//the live window over that map, the host's own (a mirror built from it, made the host's)
	private OverworldLevel window( int ox, int oy, int[] map, int heroCell ){
		PathFinder.setMapSize( W, H );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, 0f, GameCalendar.season(), map, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		Dungeon.level = ow;
		Dungeon.depth = WorldLayers.depthOf( 0 );
		hero.pos = heroCell;
		hero.fieldOfView = ow.heroFOV;
		Actor.add( hero );
		look( ow );
		return ow;
	}

	private static void set( Object o, String name, Object v ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//the hero's sight, as far as the surface's: everything within SIGHT seen
	private void look( OverworldLevel ow ){
		Arrays.fill( ow.heroFOV, false );
		int hx = hero.pos % W, hy = hero.pos / W;
		for (int y = Math.max( 0, hy - SIGHT ); y <= Math.min( H - 1, hy + SIGHT ); y++){
			for (int x = Math.max( 0, hx - SIGHT ); x <= Math.min( W - 1, hx + SIGHT ); x++){
				ow.heroFOV[x + y * W] = true;
				ow.visited[x + y * W] = true;
			}
		}
	}

	//a step toward the leg, as Hero.getCloser takes it: over the ground he knows, round whom he sees; aboard
	//his boat, the sea's own steering
	private int stepToward( OverworldLevel ow, int dst ){
		if (Seafaring.steers( hero, dst )) return Seafaring.nextStep( hero, dst );
		boolean[] known = new boolean[ow.length()];
		for (int i = 0; i < known.length; i++){
			known[i] = ow.passable[i] && (ow.visited[i] || ow.mapped[i]) && ow.plants.get( i ) == null;
		}
		PathFinder.Path p = Dungeon.findPath( hero, dst, known, ow.heroFOV, true );
		return p == null || p.isEmpty() ? -1 : p.getFirst();
	}

	//the march, act by act, until it stops or the hero comes within a step of the waypoint: the cells he
	//stood on (the hook's own cell back is a turn waited out)
	private ArrayList<Integer> march( OverworldLevel ow, int limit ){
		ArrayList<Integer> trail = new ArrayList<>();
		trail.add( hero.pos );
		for (int act = 0; act < limit; act++){
			int x = ow.worldX + hero.pos % W, y = ow.worldY + hero.pos / W;
			if (Math.max( Math.abs( x - ow.waypointX ), Math.abs( y - ow.waypointY ) ) <= 1) break;
			int dst = ow.marchDestination();
			if (dst == -1) break;
			if (dst == hero.pos) continue;
			int step = stepToward( ow, dst );
			if (step == -1) continue;
			int[] from = Seafaring.worldCell( hero.pos );
			hero.pos = step;
			Seafaring.moved( hero, from, step );
			look( ow );
			trail.add( step );
		}
		return trail;
	}

	private static int mostVisits( ArrayList<Integer> trail ){
		HashMap<Integer, Integer> seen = new HashMap<>();
		int most = 0;
		for (int c : trail) most = Math.max( most, seen.merge( c, 1, Integer::sum ) );
		return most;
	}

	//the log's lines while `run` runs
	private static ArrayList<String> logged( Runnable run ){
		final ArrayList<String> lines = new ArrayList<>();
		Signal.Listener<String> l = lines::add;
		GLog.update.add( l );
		try {
			run.run();
		} finally {
			GLog.update.remove( l );
		}
		return lines;
	}

	private static boolean said( ArrayList<String> lines, String key ){
		String text = Messages.get( OverworldLevel.class, key );
		for (String s : lines) if (s.contains( text )) return true;
		return false;
	}

	// ------------------------------------------------------------------ the tests

	@Test
	public void theMarchGoesRoundALakeItsOldLegsPacedBefore(){
		//a U of deep water: the hero in its cup, the waypoint past its bottom. Every old leg turned no more
		//than 90 degrees off the waypoint's bearing and paced along the cup's bottom (MarchPlannerTest)
		int[] map = ground();
		rect( map, 64, 50, 69, 106, Terrain.DEEP_WATER );
		rect( map, 107, 50, 112, 106, Terrain.DEEP_WATER );
		rect( map, 64, 101, 112, 106, Terrain.DEEP_WATER );
		OverworldLevel ow = window( 1200, 1200, map, 88 + 92 * W );
		ow.setWaypoint( 1200 + 88, 1200 + 125 );
		assertTrue( ow.waypointMarching );

		ArrayList<Integer> trail = march( ow, 600 );
		int last = trail.get( trail.size() - 1 );
		assertTrue( "the hero reached the waypoint (he stopped at " + last % W + "," + last / W + ")",
				Math.max( Math.abs( last % W - 88 ), Math.abs( last / W - 125 ) ) <= 1 );
		assertTrue( "no pacing (most visits " + mostVisits( trail ) + ")", mostVisits( trail ) <= 2 );
		//out of the cup's mouth and round an arm: about 160 cells, nowhere near the 600 turns allowed
		assertTrue( trail.size() + " steps", trail.size() < 220 );
		assertTrue( ow.waypointMarching );
	}

	@Test
	public void anIslandWaypointStopsTheMarchAtTheNearestShoreWithItsWord(){
		int[] map = ground();
		rect( map, 70, 60, 120, 110, Terrain.DEEP_WATER );
		rect( map, 90, 80, 100, 90, Terrain.EMPTY );
		OverworldLevel ow = window( 3000, -2000, map, 40 + 85 * W );
		ow.setWaypoint( 3000 + 95, -2000 + 85 );

		final ArrayList<Integer>[] trail = new ArrayList[1];
		ArrayList<String> lines = logged( () -> trail[0] = march( ow, 400 ) );
		int last = trail[0].get( trail[0].size() - 1 );
		assertTrue( "the march stopped with its word: " + lines, said( lines, "waypoint_unreachable" ) );
		assertFalse( ow.waypointMarching );
		//the waypoint stays, for the hud tag to try again
		assertTrue( ow.waypointActive );
		//at the shore nearest the island: 26 cells of water off it, on the side he came from
		assertEquals( 69, last % W );
		assertEquals( 26, Math.max( Math.abs( last % W - 95 ), Math.abs( last / W - 85 ) ) );
		assertTrue( "no pacing (most visits " + mostVisits( trail[0] ) + ")", mostVisits( trail[0] ) <= 2 );
	}

	@Test
	public void aboardTheMarchSailsRoundASpitOfLandAndPutsHimAshore(){
		//a lake with a spit of land down its middle from the north shore: the hero aboard his raft west of
		//it, the waypoint on the ground past the lake's east shore
		int[] map = ground();
		rect( map, 40, 40, 140, 140, Terrain.DEEP_WATER );
		rect( map, 88, 40, 92, 120, Terrain.EMPTY );
		OverworldLevel ow = window( 5000, 5000, map, 60 + 60 * W );
		Afloat a = Buff.affect( hero, Afloat.class );
		a.kind = Boat.Kind.RAFT;
		a.hull = Boat.Kind.RAFT.hull;
		a.wx = 5000 + 60;
		a.wy = 5000 + 60;
		a.aboard = true;
		ow.setWaypoint( 5000 + 146, 5000 + 60 );
		assertTrue( ow.waypointMarching );

		ArrayList<Integer> trail = march( ow, 600 );
		int last = trail.get( trail.size() - 1 );
		assertTrue( "the hero reached the waypoint (he stopped at " + last % W + "," + last / W + ")",
				Math.max( Math.abs( last % W - 146 ), Math.abs( last / W - 60 ) ) <= 1 );
		assertFalse( "he came ashore", Seafaring.aboard( hero ) );
		//the spit was sailed round, south of its tip
		boolean round = false;
		for (int c : trail) round |= c % W >= 88 && c % W <= 92 && c / W > 120;
		assertTrue( round );
		assertTrue( "no pacing (most visits " + mostVisits( trail ) + ")", mostVisits( trail ) <= 2 );
		assertTrue( trail.size() + " steps", trail.size() < 260 );
	}

	@Test
	public void aGapAMonsterStandsInIsShutAndGoneRound(){
		//a wall across the window with two gaps: the near one, a monster standing in it, and one far off
		int[] map = ground();
		rect( map, 1, 90, W - 2, 91, Terrain.WALL );
		rect( map, 88, 90, 88, 91, Terrain.EMPTY );
		rect( map, 150, 90, 150, 91, Terrain.EMPTY );
		OverworldLevel ow = window( -800, 400, map, 88 + 70 * W );
		Rat rat = new Rat();
		rat.pos = 88 + 90 * W;
		Actor.add( rat );
		ow.setWaypoint( -800 + 88, 400 + 110 );

		ArrayList<Integer> trail = march( ow, 600 );
		int last = trail.get( trail.size() - 1 );
		assertTrue( "the hero reached the waypoint (he stopped at " + last % W + "," + last / W + ")",
				Math.max( Math.abs( last % W - 88 ), Math.abs( last / W - 110 ) ) <= 1 );
		assertTrue( "by the far gap", trail.contains( 150 + 90 * W ) || trail.contains( 150 + 91 * W ) );
		assertTrue( ow.waypointMarching );
	}

	@Test
	public void aMarchThatPacesIsStoppedWithItsWord(){
		//a hero who steps back and forth whatever leg he is handed (as a walk that flips between two ways
		//would): no headway, so the march plans again from where he stands, and after STUCK_PLANS plans
		//that get him no further it gives up with the existing word
		int[] map = ground();
		OverworldLevel ow = window( 600, 600, map, 88 + 88 * W );
		ow.setWaypoint( 600 + 88, 600 + 150 );
		final int a = 88 + 88 * W, b = 89 + 88 * W;
		final int[] acts = new int[1];
		ArrayList<String> lines = logged( () -> {
			for (; acts[0] < 200; acts[0]++){
				if (ow.marchDestination() == -1) break;
				hero.pos = hero.pos == a ? b : a;
				look( ow );
			}
		} );
		assertTrue( "the march stopped with its word: " + lines, said( lines, "waypoint_blocked" ) );
		assertFalse( ow.waypointMarching );
		assertTrue( ow.waypointActive );
		//four rounds of twelve acts and a plan: no sooner (a march that gets on is never stopped) and no later
		assertTrue( acts[0] + " acts", acts[0] >= 48 && acts[0] < 70 );
	}

	@Test
	public void aMirrorNeverMarches(){
		int[] map = ground();
		PathFinder.setMapSize( W, H );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, 0, 0, 0f, GameCalendar.season(), map, W, H );
		assertNotNull( ow );
		Dungeon.level = ow;
		hero.pos = 88 + 88 * W;
		ow.setWaypoint( 120, 120 );
		assertFalse( ow.waypointActive );
		//a waypoint it holds all the same (read from a bundle) walks nobody on a mirror
		ow.waypointActive = true;
		ow.waypointMarching = true;
		ow.waypointX = 120;
		ow.waypointY = 120;
		assertEquals( -1, ow.marchDestination() );
	}
}
