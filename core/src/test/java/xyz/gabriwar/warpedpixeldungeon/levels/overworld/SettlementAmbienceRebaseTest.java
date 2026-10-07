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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SettlementAmbience across a window slide, in a big settlement and away from any: what
 * burns is keyed by world cell and placed in window pixels, and the live visuals are only
 * re-placed when the list changes (sameLights / sameSmoke ignore positions), so a rebase
 * must give the very same list with every position moved by the slide - or a lamp jumps.
 */
public class SettlementAmbienceRebaseTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	//OverworldLevel.SHIFT_Q (private): one rebase step
	private static final int Q = 32;
	private static final int S = 16;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	private static final class Burning {
		final ArrayList<SettlementAmbience.Light> lights = new ArrayList<>();
		final ArrayList<SettlementAmbience.Smoke> smoke = new ArrayList<>();
		final HashSet<Long> keys = new HashSet<>();
	}

	private static Burning collect( int[] map, int ox, int oy, int hwx, int hwy, Phase phase, float p, int night ){
		Burning b = new Burning();
		SettlementAmbience.collect( SEED, map, map, W, H, ox, oy, hwx, hwy, phase, p, Season.WINTER, biome -> -5f, false, night,
				new HashSet<>(), b.lights, b.smoke, b.keys );
		return b;
	}

	private static WindowGenerator.Window gen( int ox, int oy ){
		return WindowGenerator.generate( SEED, 0, ox, oy, WorldModel.calendarShift( Season.WINTER, 0.5f ) );
	}

	private static boolean fitting( int id ){
		return id == OverworldDress.VILLAGE_WINDOWS[0] || id == OverworldDress.VILLAGE_WINDOWS[1]
				|| id == OverworldDress.VILLAGE_CHIMNEY || id == OverworldDress.GNOLL_HEARTH;
	}

	//the same list, in the same order, every position moved by the slide (the origin moved by dx, dy)
	private static void assertSlid( String where, Burning a, Burning b, int dx, int dy ){
		assertEquals( where + ": lights", a.lights.size(), b.lights.size() );
		assertEquals( where + ": smoke", a.smoke.size(), b.smoke.size() );
		assertEquals( where + ": keys", a.keys, b.keys );
		for (int i = 0; i < a.lights.size(); i++){
			SettlementAmbience.Light x = a.lights.get( i ), y = b.lights.get( i );
			assertEquals( where, x.key, y.key );
			assertEquals( where, x.kind, y.kind );
			assertEquals( where, x.level, y.level, 0f );
			assertEquals( where + ": light x", x.x - dx * S, y.x, 0f );
			assertEquals( where + ": light y", x.y - dy * S, y.y, 0f );
		}
		for (int i = 0; i < a.smoke.size(); i++){
			SettlementAmbience.Smoke x = a.smoke.get( i ), y = b.smoke.get( i );
			assertEquals( where, x.key, y.key );
			assertEquals( where, x.interval, y.interval, 0f );
			assertEquals( where, x.gated, y.gated );
			assertEquals( where, x.gateDX, y.gateDX );
			assertEquals( where, x.gateDY, y.gateDY );
			assertEquals( where, x.w, y.w, 0f );
			assertEquals( where, x.h, y.h, 0f );
			assertEquals( where + ": smoke x", x.x - dx * S, y.x, 0f );
			assertEquals( where + ": smoke y", x.y - dy * S, y.y, 0f );
		}
	}

	//a rebase moves the origin one step while the hero stays on his world cell: the lamps and
	//the smoke he sees (dusk and after midnight), and the fittings the dressing lays, are the same
	@Test
	public void aSlideMovesEveryLightWithTheWorld(){
		int[] human = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 ).get( 0 );
		int[] gnoll = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.GNOLL, 4, 12 ).get( 0 );
		int[] stand = SettlementLights.standCell( SEED, human[0], human[1] );
		int[][] heroes = {
				stand,
				{ gnoll[2], gnoll[3] },
				{ WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) } };
		String[] names = { "human village", "gnoll camp", "town" };
		int[][] slides = { { Q, 0 }, { -Q, 0 }, { 0, Q }, { 0, -Q } };
		int night = 11;

		for (int k = 0; k < heroes.length; k++){
			int hwx = heroes[k][0], hwy = heroes[k][1];
			int ox = hwx - W / 2, oy = hwy - H / 2;
			WindowGenerator.Window base = gen( ox, oy );
			Burning dusk = collect( base.terrain, ox, oy, hwx, hwy, Phase.DUSK, 0.61f, night );
			Burning late = collect( base.terrain, ox, oy, hwx, hwy, Phase.NIGHT, 0.9f, night );
			assertTrue( names[k] + ": lights at dusk " + dusk.lights.size(), dusk.lights.size() >= 4 );
			assertTrue( names[k] + ": smoke at dusk " + dusk.smoke.size(), dusk.smoke.size() >= 2 );
			int[][] dressA = WindowGenerator.dress( SEED, ox, oy, base.terrain, base, Season.WINTER );

			for (int[] s : slides){
				int nx = ox + s[0], ny = oy + s[1];
				WindowGenerator.Window moved = gen( nx, ny );
				String where = names[k] + " slid " + s[0] + "," + s[1];
				assertSlid( where + " at dusk", dusk, collect( moved.terrain, nx, ny, hwx, hwy, Phase.DUSK, 0.61f, night ), s[0], s[1] );
				assertSlid( where + " after midnight", late, collect( moved.terrain, nx, ny, hwx, hwy, Phase.NIGHT, 0.9f, night ), s[0], s[1] );

				//the dressing lays the same fittings on the same world cells wherever both windows
				//see them clear of their edges
				int[][] dressB = WindowGenerator.dress( SEED, nx, ny, moved.terrain, moved, Season.WINTER );
				int compared = 0;
				for (int wy = Math.max( oy, ny ) + 3; wy <= Math.min( oy, ny ) + H - 4; wy++){
					for (int wx = Math.max( ox, nx ) + 3; wx <= Math.min( ox, nx ) + W - 4; wx++){
						int ca = (wx - ox) + (wy - oy) * W, cb = (wx - nx) + (wy - ny) * W;
						for (int layer = 0; layer < 3; layer++){
							int a = dressA[layer][ca], b = dressB[layer][cb];
							if (!fitting( a ) && !fitting( b )) continue;
							assertEquals( where + ": fitting at " + wx + "," + wy + " layer " + layer, a, b );
							compared++;
						}
					}
				}
				//the town's glass and stacks are its own art, not the dressing's
				if (k < 2) assertTrue( where + ": fittings compared " + compared, compared >= 4 );
			}
		}
	}

	//a settlement bigger than ten houses: only the ten lived-in ones burn, each with at most its
	//two windows and one stack (or its one fire), wherever the hero walks through it
	@Test
	public void onlyTheLivedInHousesBurn(){
		int checkedEmpty = 0, settlements = 0;
		for (WorldStructures.Faction faction : new WorldStructures.Faction[]{ WorldStructures.Faction.HUMAN, WorldStructures.Faction.GNOLL }){
			ArrayList<int[]> big = WorldStructures.settlementsNear( SEED, 0, 0, faction, 11, 30 );
			if (big.isEmpty()) continue;
			int[] v = big.get( 0 );
			settlements++;
			int ox = v[2] - W / 2, oy = v[3] - H / 2;
			int[] map = gen( ox, oy ).terrain;
			int r = WorldStructures.settlementLayout( SEED, v[0], v[1] )[0];

			//every fitting cell in the window, by the house it belongs to
			HashMap<Long, SettlementLights.House> owner = new HashMap<>();
			ArrayList<long[]> emptyGlass = new ArrayList<>();
			for (SettlementLights.House h : SettlementLights.housesIn( SEED, ox, oy, ox + W - 1, oy + H - 1 )){
				ArrayList<Integer> cells = new ArrayList<>();
				if (h.faction == WorldStructures.Faction.HUMAN){
					for (int[] o : SettlementLights.HOUSE_WINDOWS){
						cells.add( SettlementLights.glassCell( SEED, map, W, H, ox, oy, h.wx + o[0], h.wy + o[1] ) );
					}
					cells.add( SettlementLights.chimneyCell( SEED, map, W, H, ox, oy, h.wx, h.wy ) );
				} else {
					cells.add( SettlementLights.hearthCell( SEED, map, W, H, ox, oy, h ) );
				}
				for (int c : cells){
					if (c == -1) continue;
					long key = OverworldLevel.worldKey( ox + c % W, oy + c / W );
					owner.put( key, h );
					if (!h.inhabited()) emptyGlass.add( new long[]{ key, ox + c % W, oy + c / W } );
				}
			}
			assertTrue( faction + ": empty houses with fittings " + emptyGlass.size(), !emptyGlass.isEmpty() );

			for (int dy = -r; dy <= r; dy += 4){
				for (int dx = -r; dx <= r; dx += 4){
					int hwx = v[2] + dx, hwy = v[3] + dy;
					Burning b = collect( map, ox, oy, hwx, hwy, Phase.DUSK, 0.61f, 5 );
					HashMap<SettlementLights.House, int[]> perHouse = new HashMap<>();
					HashSet<Long> seenLights = new HashSet<>(), seenSmoke = new HashSet<>();
					for (SettlementAmbience.Light l : b.lights){
						assertTrue( "a light listed twice", seenLights.add( l.key ) );
						SettlementLights.House h = owner.get( l.key );
						assertTrue( faction + ": a light on no fitting at " + hwx + "," + hwy, h != null );
						assertTrue( faction + ": an empty house lit, index " + h.index + " of " + h.count, h.inhabited() );
						perHouse.computeIfAbsent( h, x -> new int[2] )[0]++;
					}
					for (SettlementAmbience.Smoke sm : b.smoke){
						assertTrue( "a column listed twice", seenSmoke.add( sm.key ) );
						SettlementLights.House h = owner.get( sm.key );
						assertTrue( faction + ": smoke from no fitting at " + hwx + "," + hwy, h != null );
						assertTrue( faction + ": an empty house smoking, index " + h.index + " of " + h.count, h.inhabited() );
						perHouse.computeIfAbsent( h, x -> new int[2] )[1]++;
					}
					for (java.util.Map.Entry<SettlementLights.House, int[]> e : perHouse.entrySet()){
						int maxLights = e.getKey().faction == WorldStructures.Faction.HUMAN ? 2 : 1;
						assertTrue( "lights per house " + e.getValue()[0], e.getValue()[0] <= maxLights );
						assertTrue( "columns per house " + e.getValue()[1], e.getValue()[1] <= 1 );
					}
					for (long[] g : emptyGlass){
						if (Math.max( Math.abs( g[1] - hwx ), Math.abs( g[2] - hwy ) ) <= SettlementAmbience.RADIUS) checkedEmpty++;
					}
				}
			}
		}
		assertTrue( "big settlements found: " + settlements, settlements >= 1 );
		assertTrue( "empty houses' fittings in reach: " + checkedEmpty, checkedEmpty > 0 );
	}

	//out in the wild there is nothing to light, so the hero's turn posts nothing to the render
	//thread (onHeroTurn returns on an unchanged empty list), and finding that out is cheap
	@Test
	public void theWildCostsNothing(){
		int hwx = -1, hwy = 0;
		for (int x = 200; x < 20000 && hwx == -1; x += 7){
			if (SettlementLights.housesIn( SEED, x - SettlementAmbience.KEEP, -SettlementAmbience.KEEP,
					x + SettlementAmbience.KEEP, SettlementAmbience.KEEP ).isEmpty()) hwx = x;
		}
		assertTrue( "a stretch of wild", hwx != -1 );
		int ox = hwx - W / 2, oy = hwy - H / 2;
		int[] map = gen( ox, oy ).terrain;
		for (Phase p : Phase.values()){
			Burning b = collect( map, ox, oy, hwx, hwy, p, 0.5f, 3 );
			assertTrue( b.lights.isEmpty() );
			assertTrue( b.smoke.isEmpty() );
			assertTrue( b.keys.isEmpty() );
		}
		for (int i = 0; i < 2000; i++) collect( map, ox, oy, hwx, hwy, Phase.NIGHT, 0.5f, 3 );
		long t0 = System.nanoTime();
		int n = 5000;
		for (int i = 0; i < n; i++) collect( map, ox, oy, hwx + (i & 7), hwy, Phase.NIGHT, 0.5f, 3 );
		double us = (System.nanoTime() - t0) / 1000.0 / n;
		System.out.println( "[ambience] collect in the wild: " + us + " us/turn" );
		assertTrue( "collect in the wild " + us + " us", us < 500 );

		//and in the thick of the biggest settlement near the origin
		ArrayList<int[]> all = WorldStructures.settlementsNear( SEED, 0, 0, null, 2, 30 );
		int[] big = all.get( 0 );
		for (int[] v : all) if (v[4] > big[4] && WorldStructures.faction( SEED, v[0], v[1] ) != WorldStructures.Faction.BANDIT) big = v;
		ox = big[2] - W / 2; oy = big[3] - H / 2;
		map = gen( ox, oy ).terrain;
		for (int i = 0; i < 2000; i++) collect( map, ox, oy, big[2], big[3], Phase.DUSK, 0.61f, 3 );
		t0 = System.nanoTime();
		for (int i = 0; i < n; i++) collect( map, ox, oy, big[2] + (i & 7), big[3], Phase.DUSK, 0.61f, 3 );
		us = (System.nanoTime() - t0) / 1000.0 / n;
		System.out.println( "[ambience] collect in a " + big[4] + "-house settlement: " + us + " us/turn" );
		assertTrue( "collect in a settlement " + us + " us", us < 2000 );
	}

	//the night turns over in the middle of the DAY phase in every season of two years, so a
	//night's owls keep their lamps from dusk through the dawn after it
	@Test
	public void theNightTurnsOverByDayAllYear(){
		int turn = Dungeon.cycleTurn, challenges = Dungeon.challenges, start = Dungeon.calendarStartDay;
		Phase override = DayNightCycle.debugPhaseOverride;
		try {
			Dungeon.challenges = 0;
			Dungeon.calendarStartDay = 0;
			DayNightCycle.debugPhaseOverride = null;
			HashSet<Season> seasons = new HashSet<>();
			for (int day = 1; day < 730; day++){
				int t0 = day * DayNightCycle.FULL_CYCLE;
				Dungeon.cycleTurn = t0;
				seasons.add( GameCalendar.season() );
				assertEquals( Phase.DAWN, DayNightCycle.phase() );
				int dawnNight = WorldClock.night();
				//the turn the night changes is a day's turn, with day on either side of it
				for (int t = t0 + 499; t <= t0 + 500; t++){
					Dungeon.cycleTurn = t;
					assertEquals( "day " + day + " turn " + t, Phase.DAY, DayNightCycle.phase() );
				}
				Dungeon.cycleTurn = t0 + 499;
				assertEquals( dawnNight, WorldClock.night() );
				Dungeon.cycleTurn = t0 + 500;
				assertEquals( dawnNight + 1, WorldClock.night() );
				//that night holds through the dusk, the dark and the next dawn
				for (int t = t0 + 500; t < t0 + DayNightCycle.FULL_CYCLE + 250; t += 50){
					Dungeon.cycleTurn = t;
					assertEquals( "day " + day + " turn " + t, dawnNight + 1, WorldClock.night() );
				}
				Dungeon.cycleTurn = t0;
				assertEquals( day, WorldClock.day() );
			}
			assertEquals( 4, seasons.size() );
		} finally {
			Dungeon.cycleTurn = turn;
			Dungeon.challenges = challenges;
			Dungeon.calendarStartDay = start;
			DayNightCycle.debugPhaseOverride = override;
		}
	}
}
