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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.ToDoubleFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The settlements' lights (SettlementLights): who lives where, the hours the lamps and
 * hearths keep, and the dressing that puts the windows, chimneys and fire rings on the
 * very cells the lights later burn on.
 */
public class SettlementLightsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	@Test
	public void populatedHousesCapsAtTen(){
		for (int n = 2; n <= 90; n++) assertEquals( Math.min( n, 10 ), WorldStructures.populatedHouses( n ) );
		int checked = 0;
		for (int sy = -6; sy <= 6; sy++){
			for (int sx = -6; sx <= 6; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int cx = WorldStructures.siteX( SEED, sx, sy ), cy = WorldStructures.siteY( SEED, sx, sy );
				int r = WorldStructures.settlementLayout( SEED, sx, sy )[0];
				for (SettlementLights.House h : SettlementLights.housesIn( SEED, cx - r, cy - r, cx + r, cy + r )){
					assertNotEquals( WorldStructures.Faction.BANDIT, h.faction );
					assertEquals( h.index < Math.min( h.count, 10 ), h.inhabited() );
					checked++;
				}
			}
		}
		assertTrue( "houses checked: " + checked, checked > 100 );
	}

	@Test
	public void housesInMatchesBruteForce(){
		List<int[]> villages = WorldStructures.settlementsNear( SEED, 0, 0, null, 2, 12 );
		int rects = 0;
		for (int[] v : villages){
			if (WorldStructures.faction( SEED, v[0], v[1] ) == WorldStructures.Faction.BANDIT) continue;
			int x0 = v[2] - 22, y0 = v[3] - 22, x1 = v[2] + 21, y1 = v[3] + 21;
			HashSet<String> got = new HashSet<>();
			for (SettlementLights.House h : SettlementLights.housesIn( SEED, x0, y0, x1, y1 )){
				assertTrue( "listed twice", got.add( h.sx + "," + h.sy + "," + h.index ) );
			}
			HashSet<String> want = new HashSet<>();
			for (int sy = -12; sy <= 12; sy++){
				for (int sx = -12; sx <= 12; sx++){
					if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
					if (WorldStructures.faction( SEED, sx, sy ) == WorldStructures.Faction.BANDIT) continue;
					int cx = WorldStructures.siteX( SEED, sx, sy ), cy = WorldStructures.siteY( SEED, sx, sy );
					int[] layout = WorldStructures.settlementLayout( SEED, sx, sy );
					for (int i = 0; 1 + 2*i + 1 < layout.length; i++){
						int hx = cx + layout[1 + 2*i], hy = cy + layout[2 + 2*i];
						if (hx + 3 >= x0 && hx - 3 <= x1 && hy + 3 >= y0 && hy - 3 <= y1) want.add( sx + "," + sy + "," + i );
					}
				}
			}
			assertEquals( want, got );
			if (++rects == 5) break;
		}
		assertEquals( 5, rects );
	}

	@Test
	public void duskLightsComeOnOneByOne(){
		int last = -1;
		for (int step = 0; step <= 20; step++){
			float p = step * 0.05f;
			int lit = 0;
			for (int i = 0; i < 200; i++) if (SettlementLights.glowLevel( Phase.DUSK, p, i / 200f, false ) > 0) lit++;
			assertTrue( "dusk " + p + ": " + lit + " after " + last, lit >= last );
			last = lit;
		}
		int lit = 0, all = 0;
		for (int i = 0; i < 200; i++){
			if (SettlementLights.glowLevel( Phase.DUSK, 0.04f, i / 200f, false ) > 0) lit++;
			if (SettlementLights.glowLevel( Phase.DUSK, 0.61f, i / 200f, false ) > 0) all++;
		}
		assertEquals( 0, lit );
		assertEquals( 200, all );
	}

	@Test
	public void lateNightEmbersExceptOwls(){
		for (int i = 0; i < 100; i++){
			float h = i / 100f;
			assertEquals( 1f, SettlementLights.glowLevel( Phase.NIGHT, 0.3f, h, false ), 0f );
			assertEquals( SettlementLights.EMBER, SettlementLights.glowLevel( Phase.NIGHT, 0.7f, h, false ), 0f );
			assertEquals( 1f, SettlementLights.glowLevel( Phase.NIGHT, 0.9f, h, true ), 0f );
			assertEquals( 0f, SettlementLights.glowLevel( Phase.DAY, 0.5f, h, true ), 0f );
			assertEquals( 0f, SettlementLights.glowLevel( Phase.DAWN, 0.6f, h, false ), 0f );
		}
		assertEquals( SettlementLights.DAWN_LIGHT, SettlementLights.glowLevel( Phase.DAWN, 0.13f, 0.5f, false ), 0f );
	}

	@Test
	public void townSleepsAtTheInn(){
		for (int i = 0; i < 100; i++){
			float h = i / 100f;
			assertEquals( 0f, SettlementLights.townGlowLevel( Phase.NIGHT, 0.5f, h, false ), 0f );
			assertEquals( 1f, SettlementLights.townGlowLevel( Phase.NIGHT, 0.95f, h, true ), 0f );
			assertEquals( 1f, SettlementLights.townGlowLevel( Phase.DUSK, 0.9f, h, false ), 0f );
			assertEquals( 0f, SettlementLights.townGlowLevel( Phase.DAWN, 0.1f, h, false ), 0f );
		}
	}

	@Test
	public void smokeFollowsTheColdAndTheMeals(){
		assertEquals( 1f, SettlementLights.smokeLevel( Season.WINTER, 20f, Phase.DAY, 0.5f, 0.5f, false ), 0f );
		assertEquals( 0f, SettlementLights.smokeLevel( Season.SUMMER, 25f, Phase.DAY, 0.5f, 0.5f, false ), 0f );
		assertEquals( 1f, SettlementLights.smokeLevel( Season.SUMMER, 25f, Phase.DUSK, 0.5f, 0.5f, false ), 0f );
		assertEquals( 1f, SettlementLights.smokeLevel( Season.SUMMER, 25f, Phase.DAWN, 0.9f, 0.5f, false ), 0f );
		assertEquals( 0f, SettlementLights.smokeLevel( Season.SUMMER, 25f, Phase.NIGHT, 0.5f, 0.5f, false ), 0f );
		assertEquals( 1f, SettlementLights.smokeLevel( Season.SPRING, 6f, Phase.DAY, 0.5f, 0.5f, false ), 0f );
		assertEquals( SettlementLights.BANKED, SettlementLights.smokeLevel( Season.WINTER, -5f, Phase.NIGHT, 0.9f, 0.5f, false ), 0f );
		assertEquals( 1f, SettlementLights.smokeLevel( Season.WINTER, -5f, Phase.NIGHT, 0.9f, 0.5f, true ), 0f );
	}

	@Test
	public void rainThinsTheSmoke(){
		assertEquals( 0f, SettlementLights.puffInterval( 0f, false ), 0f );
		assertEquals( 0f, SettlementLights.puffInterval( 0f, true ), 0f );
		assertEquals( 0.3f, SettlementLights.puffInterval( 1f, false ), 1e-6f );
		assertTrue( SettlementLights.puffInterval( 1f, true ) > SettlementLights.puffInterval( 1f, false ) );
		assertTrue( SettlementLights.puffInterval( SettlementLights.BANKED, false ) > SettlementLights.puffInterval( 1f, false ) );
	}

	@Test
	public void oneOrTwoNightOwls(){
		for (int n = 2; n <= 10; n++){
			HashSet<String> sets = new HashSet<>();
			for (long night = 0; night <= 30; night++){
				int owls = 0;
				StringBuilder set = new StringBuilder();
				for (int i = 0; i < n; i++){
					boolean owl = SettlementLights.nightOwl( SEED, 3, -2, i, n, night );
					assertEquals( owl, SettlementLights.nightOwl( SEED, 3, -2, i, n, night ) );
					if (owl){ owls++; set.append( i ).append( ' ' ); }
				}
				assertEquals( "n " + n + " night " + night, n >= 6 ? 2 : 1, owls );
				sets.add( set.toString() );
			}
			if (n == 10) assertTrue( "the same owls every night", sets.size() > 1 );
		}
	}

	//the dawn belongs to the night before it: the owls who kept their lamps lit till late keep
	//them lit into the grey, and nobody else's lamp takes over
	@Test
	public void dawnKeepsLastNightsOwls(){
		WarpedRoomsTest.boot();
		int turn = Dungeon.cycleTurn, challenges = Dungeon.challenges, start = Dungeon.calendarStartDay;
		DayNightCycle.Phase override = DayNightCycle.debugPhaseOverride;
		try {
			Dungeon.challenges = 0;
			Dungeon.calendarStartDay = 0;
			DayNightCycle.debugPhaseOverride = null;
			for (int day = 3; day < 40; day += 7){
				//NIGHT 0.9 of the night that falls on `day`, then 0.05 into the dawn after it
				Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE;
				int night = 0;
				for (Phase p = Phase.DAWN; p != Phase.NIGHT; p = p.next()) night += DayNightCycle.phaseDuration( p );
				Dungeon.cycleTurn += night + Math.round( 0.9f * DayNightCycle.phaseDuration( Phase.NIGHT ) );
				assertEquals( Phase.NIGHT, DayNightCycle.phase() );
				int late = WorldClock.night();
				assertEquals( day, late );
				Dungeon.cycleTurn = (day + 1) * DayNightCycle.FULL_CYCLE + Math.round( 0.05f * DayNightCycle.phaseDuration( Phase.DAWN ) );
				assertEquals( Phase.DAWN, DayNightCycle.phase() );
				assertEquals( late, WorldClock.night() );
				assertEquals( day + 1, WorldClock.day() );
				for (int i = 0; i < 10; i++){
					boolean owlLate = SettlementLights.nightOwl( SEED, 1, 1, i, 10, late );
					assertEquals( owlLate, SettlementLights.nightOwl( SEED, 1, 1, i, 10, WorldClock.night() ) );
					float h = (i + 1) / 11f;
					//an owl's lamp burns on, a sleeper's stays an ember
					assertEquals( SettlementLights.glowLevel( Phase.NIGHT, 0.9f, h, owlLate ),
							SettlementLights.glowLevel( Phase.DAWN, 0.05f * h, h, owlLate ), 0f );
				}
				//and the next evening is a new night
				Dungeon.cycleTurn = (day + 1) * DayNightCycle.FULL_CYCLE + 600;
				assertEquals( late + 1, WorldClock.night() );
			}
		} finally {
			Dungeon.cycleTurn = turn;
			Dungeon.challenges = challenges;
			Dungeon.calendarStartDay = start;
			DayNightCycle.debugPhaseOverride = override;
		}
	}

	@Test
	public void settlementsNearAreNearestFirst(){
		ArrayList<int[]> all = WorldStructures.settlementsNear( SEED, 0, 0, null, 2, 12 );
		assertTrue( all.size() > 20 );
		long last = -1;
		for (int[] v : all){
			assertEquals( WorldStructures.Site.VILLAGE, WorldStructures.siteType( SEED, v[0], v[1] ) );
			assertEquals( (WorldStructures.settlementLayout( SEED, v[0], v[1] ).length - 1) / 2, v[4] );
			long d = (long) v[2] * v[2] + (long) v[3] * v[3];
			assertTrue( d >= last );
			last = d;
		}
		for (int[] v : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.GNOLL, 6, 12 )){
			assertEquals( WorldStructures.Faction.GNOLL, WorldStructures.faction( SEED, v[0], v[1] ) );
			assertTrue( v[4] >= 6 );
		}
	}

	private static boolean fitting( int id ){
		return id == OverworldDress.VILLAGE_WINDOWS[0] || id == OverworldDress.VILLAGE_WINDOWS[1]
				|| id == OverworldDress.VILLAGE_CHIMNEY || id == OverworldDress.GNOLL_HEARTH;
	}

	//a window dressed around a settlement, for WINTER (the debug scenes' season)
	private static int[][] window( int[] v, int[] origin, int[][] terrainOut ){
		int ox = v[2] - W / 2, oy = v[3] - H / 2;
		origin[0] = ox; origin[1] = oy;
		WindowGenerator.Window win = WindowGenerator.generate( SEED, 0, ox, oy, WorldModel.calendarShift( Season.WINTER, 0.5f ) );
		terrainOut[0] = win.terrain;
		return WindowGenerator.dress( SEED, ox, oy, win.terrain, win, Season.WINTER );
	}

	private static boolean inAnyHouseBox( List<SettlementLights.House> houses, int wx, int wy, int reach, long notSector ){
		for (SettlementLights.House h : houses){
			if (WorldStructures.sectorOf( h.sx, h.sy ) == notSector) continue;
			if (Math.abs( wx - h.wx ) <= reach && Math.abs( wy - h.wy ) <= reach) return true;
		}
		return false;
	}

	@Test
	public void dressPutsTheFittings(){
		// --- a human village: windows on the fronts, stacks over the backs, nothing stray
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 ).get( 0 );
		int[] o = new int[2];
		int[][] t = new int[1][];
		int[][] dress = window( v, o, t );
		int ox = o[0], oy = o[1];
		int[] map = t[0];
		ArrayList<SettlementLights.House> houses = SettlementLights.housesIn( SEED, ox, oy, ox + W - 1, oy + H - 1 );
		HashSet<Integer> expected = new HashSet<>();
		int inner = 0, windows = 0, chimneys = 0;
		for (SettlementLights.House h : houses){
			boolean whole = h.wx - 3 >= ox + 2 && h.wx + 3 <= ox + W - 3 && h.wy - 3 >= oy + 2 && h.wy + 3 <= oy + H - 3;
			if (whole && h.faction == WorldStructures.Faction.HUMAN) inner++;
			if (h.faction == WorldStructures.Faction.HUMAN){
				int style = OverworldDress.VILLAGE_WINDOWS[SettlementLights.windowStyle( SEED, h.wx, h.wy )];
				for (int[] off : SettlementLights.HOUSE_WINDOWS){
					int c = SettlementLights.glassCell( SEED, map, W, H, ox, oy, h.wx + off[0], h.wy + off[1] );
					if (c == -1) continue;
					assertEquals( style, dress[0][c] );
					expected.add( c );
					if (whole) windows++;
				}
				int c = SettlementLights.chimneyCell( SEED, map, W, H, ox, oy, h.wx, h.wy );
				if (c != -1){
					assertEquals( OverworldDress.VILLAGE_CHIMNEY, dress[2][c] );
					//on the cell above a back corner of its own house
					assertEquals( h.wy + SettlementLights.CHIMNEY_DY, oy + c / W );
					assertEquals( 1, Math.abs( ox + c % W - h.wx ) );
					expected.add( c + W * H * 2 );
					if (whole) chimneys++;
				}
			} else {
				int c = SettlementLights.hearthCell( SEED, map, W, H, ox, oy, h );
				if (c != -1){
					assertEquals( OverworldDress.GNOLL_HEARTH, dress[1][c] );
					expected.add( c + W * H );
				}
			}
		}
		assertTrue( "human houses wholly inside: " + inner, inner >= 4 );
		assertTrue( "windows " + windows + " of " + 2 * inner, windows >= 0.9f * 2 * inner );
		assertTrue( "chimneys " + chimneys + " of " + inner, chimneys * 2 >= inner );
		HashSet<Integer> placed = new HashSet<>();
		for (int layer = 0; layer < 3; layer++){
			for (int c = 0; c < W * H; c++) if (fitting( dress[layer][c] )) placed.add( c + W * H * layer );
		}
		assertEquals( "stray fittings", expected, placed );

		// --- a gnoll village: a fire ring by every hut, out in the open, and no window anywhere
		v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.GNOLL, 4, 12 ).get( 0 );
		dress = window( v, o, t );
		ox = o[0]; oy = o[1];
		map = t[0];
		houses = SettlementLights.housesIn( SEED, ox, oy, ox + W - 1, oy + H - 1 );
		long own = WorldStructures.sectorOf( v[0], v[1] );
		int huts = 0, rings = 0;
		for (SettlementLights.House h : houses){
			if (WorldStructures.sectorOf( h.sx, h.sy ) != own) continue;
			huts++;
			int c = SettlementLights.hearthCell( SEED, map, W, H, ox, oy, h );
			if (c == -1) continue;
			rings++;
			assertEquals( OverworldDress.GNOLL_HEARTH, dress[1][c] );
			int wx = ox + c % W, wy = oy + c / W;
			for (SettlementLights.House other : houses){
				assertFalse( "a ring inside a hut", Math.abs( wx - other.wx ) <= 2 && Math.abs( wy - other.wy ) <= 2 );
			}
			//beside its own door, outside (a step further out by a north door), never on a lip
			int d = Math.max( Math.abs( wx - h.wx ), Math.abs( wy - h.wy ) );
			assertTrue( "ring " + d + " from its hut", d == 3 || d == 4 );
			assertFalse( "a ring on a wall's lip", SettlementLights.underLip( map[c + W] ) );
		}
		assertTrue( "huts " + huts, huts >= 4 );
		assertTrue( "rings " + rings + " of " + huts, rings * 10 >= huts * 9 );
		int r = WorldStructures.settlementLayout( SEED, v[0], v[1] )[0] + 3;
		for (int wy = v[3] - r; wy <= v[3] + r; wy++){
			for (int wx = v[2] - r; wx <= v[2] + r; wx++){
				int x = wx - ox, y = wy - oy;
				if (x < 0 || y < 0 || x >= W || y >= H || inAnyHouseBox( houses, wx, wy, 3, own )) continue;
				int c = x + y * W;
				assertFalse( "a window in a gnoll camp", dress[0][c] == OverworldDress.VILLAGE_WINDOWS[0]
						|| dress[0][c] == OverworldDress.VILLAGE_WINDOWS[1] || dress[2][c] == OverworldDress.VILLAGE_CHIMNEY );
			}
		}

		// --- a bandit camp: nobody lives there, nothing is lit
		v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.BANDIT, 2, 12 ).get( 0 );
		dress = window( v, o, t );
		ox = o[0]; oy = o[1];
		houses = SettlementLights.housesIn( SEED, ox, oy, ox + W - 1, oy + H - 1 );
		own = WorldStructures.sectorOf( v[0], v[1] );
		r = WorldStructures.settlementLayout( SEED, v[0], v[1] )[0] + 3;
		int seen = 0;
		for (int wy = v[3] - r; wy <= v[3] + r; wy++){
			for (int wx = v[2] - r; wx <= v[2] + r; wx++){
				int x = wx - ox, y = wy - oy;
				if (x < 0 || y < 0 || x >= W || y >= H || inAnyHouseBox( houses, wx, wy, 3, own )) continue;
				int c = x + y * W;
				seen++;
				for (int layer = 0; layer < 3; layer++) assertFalse( "a fitting in a bandit camp", fitting( dress[layer][c] ) );
			}
		}
		assertTrue( seen > 400 );
	}

	// ------------------------------------------------- SettlementAmbience.collect

	private static final class Burning {
		final ArrayList<SettlementAmbience.Light> lights = new ArrayList<>();
		final ArrayList<SettlementAmbience.Smoke> smoke = new ArrayList<>();
		final HashSet<Long> keys = new HashSet<>();
	}

	//the same air everywhere, the terrain untouched since the window was made
	private static Burning collect( int[] map, int ox, int oy, int hwx, int hwy, Phase phase, float p,
			Season season, float temp, int night, HashSet<Long> live ){
		return collect( map, map, ox, oy, hwx, hwy, phase, p, season, biome -> temp, night, live );
	}

	private static Burning collect( int[] map, int[] pristine, int ox, int oy, int hwx, int hwy, Phase phase, float p,
			Season season, ToDoubleFunction<WorldModel.Biome> tempIn, int night, HashSet<Long> live ){
		Burning b = new Burning();
		SettlementAmbience.collect( SEED, map, pristine, W, H, ox, oy, hwx, hwy, phase, p, season, tempIn, false, night,
				live, b.lights, b.smoke, b.keys );
		return b;
	}

	private static int cheb( int ax, int ay, int bx, int by ){
		return Math.max( Math.abs( ax - bx ), Math.abs( ay - by ) );
	}

	@Test
	public void ambienceLightsTheVillageAtDusk(){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 ).get( 0 );
		int[] o = new int[2];
		int[][] t = new int[1][];
		window( v, o, t );
		int ox = o[0], oy = o[1];
		int[] map = t[0];
		int[] stand = SettlementLights.standCell( SEED, v[0], v[1] );
		Burning b = collect( map, ox, oy, stand[0], stand[1], Phase.DUSK, 0.61f, Season.WINTER, -5f, 10, new HashSet<>() );

		HashSet<Long> wantLights = new HashSet<>(), wantSmoke = new HashSet<>();
		for (SettlementLights.House h : SettlementLights.housesIn( SEED, ox, oy, ox + W - 1, oy + H - 1 )){
			if (!h.inhabited() || h.faction != WorldStructures.Faction.HUMAN) continue;
			for (int[] off : SettlementLights.HOUSE_WINDOWS){
				int c = SettlementLights.glassCell( SEED, map, W, H, ox, oy, h.wx + off[0], h.wy + off[1] );
				if (c != -1 && cheb( ox + c % W, oy + c / W, stand[0], stand[1] ) <= SettlementAmbience.RADIUS){
					wantLights.add( OverworldLevel.worldKey( ox + c % W, oy + c / W ) );
					SettlementAmbience.Light l = find( b.lights, OverworldLevel.worldKey( ox + c % W, oy + c / W ) );
					assertNotNull( "an unlit window", l );
					assertEquals( SettlementLights.VILLAGE_WINDOW_KINDS[SettlementLights.windowStyle( SEED, h.wx, h.wy )], l.kind );
					assertEquals( 1f, l.level, 0f );
					assertEquals( (c % W) * 16, l.x, 0f );
					assertEquals( (c / W) * 16, l.y, 0f );
				}
			}
			int c = SettlementLights.chimneyCell( SEED, map, W, H, ox, oy, h.wx, h.wy );
			if (c != -1 && cheb( ox + c % W, oy + c / W, stand[0], stand[1] ) <= SettlementAmbience.RADIUS){
				long key = OverworldLevel.worldKey( ox + c % W, oy + c / W );
				wantSmoke.add( key );
				SettlementAmbience.Smoke sm = null;
				for (SettlementAmbience.Smoke x : b.smoke) if (x.key == key) sm = x;
				assertNotNull( "a cold stack", sm );
				assertEquals( SettlementLights.PUFF_INTERVAL, sm.interval, 1e-6f );
				assertTrue( sm.gated );
				//out of the stack's mouth, gated on the middle of the house's front wall
				assertEquals( (c % W) * 16 + SettlementLights.CHIMNEY_MOUTH[0], sm.x, 0f );
				assertEquals( (c / W) * 16 + SettlementLights.CHIMNEY_MOUTH[1], sm.y, 0f );
				int gx = (int)((sm.x + 8) / 16) + sm.gateDX, gy = (int)((sm.y + 8) / 16) + sm.gateDY;
				assertEquals( h.wx, ox + gx );
				assertEquals( h.wy + 2, oy + gy );
			}
		}
		assertTrue( "lights " + wantLights.size(), wantLights.size() >= 4 );
		assertTrue( "smoke " + wantSmoke.size(), wantSmoke.size() >= 2 );
		//nothing else: no light for an empty house, nothing out of reach
		HashSet<Long> gotLights = new HashSet<>(), gotSmoke = new HashSet<>();
		for (SettlementAmbience.Light l : b.lights) assertTrue( "listed twice", gotLights.add( l.key ) );
		for (SettlementAmbience.Smoke x : b.smoke) assertTrue( "listed twice", gotSmoke.add( x.key ) );
		assertEquals( wantLights, gotLights );
		assertEquals( wantSmoke, gotSmoke );
		HashSet<Long> all = new HashSet<>( wantLights );
		all.addAll( wantSmoke );
		assertEquals( all, b.keys );
	}

	private static SettlementAmbience.Light find( List<SettlementAmbience.Light> lights, long key ){
		for (SettlementAmbience.Light l : lights) if (l.key == key) return l;
		return null;
	}

	@Test
	public void ambienceSinksToEmbersAfterMidnight(){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 ).get( 0 );
		int[] o = new int[2];
		int[][] t = new int[1][];
		window( v, o, t );
		int[] stand = SettlementLights.standCell( SEED, v[0], v[1] );
		int night = 17;
		Burning b = collect( t[0], o[0], o[1], stand[0], stand[1], Phase.NIGHT, 0.9f, Season.WINTER, -5f, night, new HashSet<>() );
		int bright = 0, embers = 0;
		for (SettlementAmbience.Light l : b.lights){
			if (l.level == 1f) bright++;
			else {
				assertEquals( SettlementLights.EMBER, l.level, 0f );
				embers++;
			}
		}
		//a night owl's two windows burn, everyone else's are embers (the village is small
		//enough that every house is in reach)
		int inhabited = WorldStructures.populatedHouses( v[4] ), owls = 0;
		for (int i = 0; i < inhabited; i++) if (SettlementLights.nightOwl( SEED, v[0], v[1], i, inhabited, night )) owls++;
		assertTrue( "bright " + bright + " embers " + embers, embers > bright );
		assertTrue( bright <= 2 * owls );
		for (SettlementAmbience.Smoke sm : b.smoke){
			assertTrue( sm.interval == SettlementLights.PUFF_INTERVAL
					|| Math.abs( sm.interval - SettlementLights.PUFF_INTERVAL / SettlementLights.BANKED ) < 1e-5f );
		}
		//and a warm summer's day leaves every hearth and lamp out
		Burning day = collect( t[0], o[0], o[1], stand[0], stand[1], Phase.DAY, 0.5f, Season.SUMMER, 24f, night, new HashSet<>() );
		assertTrue( day.lights.isEmpty() );
		assertTrue( day.smoke.isEmpty() );
	}

	@Test
	public void ambienceKeepsWhatBurnsTillFarther(){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 ).get( 0 );
		int[] o = new int[2];
		int[][] t = new int[1][];
		window( v, o, t );
		int[] stand = SettlementLights.standCell( SEED, v[0], v[1] );
		Burning b = collect( t[0], o[0], o[1], stand[0], stand[1], Phase.DUSK, 0.61f, Season.WINTER, -5f, 3, new HashSet<>() );
		SettlementAmbience.Light l = b.lights.get( 0 );
		int lx = o[0] + (int)(l.x / 16), ly = o[1] + (int)(l.y / 16);
		HashSet<Long> live = new HashSet<>();
		live.add( l.key );
		for (int d = SettlementAmbience.RADIUS + 1; d <= SettlementAmbience.KEEP + 2; d++){
			//the hero walks off west, the window does not follow (collect reads the hero's cell only)
			boolean fresh = find( collect( t[0], o[0], o[1], lx - d, ly, Phase.DUSK, 0.61f, Season.WINTER, -5f, 3, new HashSet<>() ).lights, l.key ) != null;
			boolean kept = find( collect( t[0], o[0], o[1], lx - d, ly, Phase.DUSK, 0.61f, Season.WINTER, -5f, 3, live ).lights, l.key ) != null;
			assertFalse( "made new at " + d, fresh );
			assertEquals( "kept at " + d, d <= SettlementAmbience.KEEP, kept );
		}
	}

	@Test
	public void ambienceLightsTheGnollFires(){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.GNOLL, 4, 12 ).get( 0 );
		int[] o = new int[2];
		int[][] t = new int[1][];
		window( v, o, t );
		Burning b = collect( t[0], o[0], o[1], v[2], v[3], Phase.DUSK, 0.61f, Season.WINTER, -5f, 3, new HashSet<>() );
		int fires = 0;
		for (SettlementLights.House h : SettlementLights.housesIn( SEED, o[0], o[1], o[0] + W - 1, o[1] + H - 1 )){
			if (h.faction != WorldStructures.Faction.GNOLL || !h.inhabited()) continue;
			int c = SettlementLights.hearthCell( SEED, t[0], W, H, o[0], o[1], h );
			if (c == -1 || cheb( o[0] + c % W, o[1] + c / W, v[2], v[3] ) > SettlementAmbience.RADIUS) continue;
			long key = OverworldLevel.worldKey( o[0] + c % W, o[1] + c / W );
			SettlementAmbience.Light l = find( b.lights, key );
			assertNotNull( l );
			assertEquals( SettlementLights.KIND_HEARTH, l.kind );
			SettlementAmbience.Smoke sm = null;
			for (SettlementAmbience.Smoke x : b.smoke) if (x.key == key) sm = x;
			assertNotNull( sm );
			//gated on the ring's own open ground, from among its logs
			assertTrue( sm.gated );
			assertEquals( 0, sm.gateDX );
			assertEquals( 0, sm.gateDY );
			assertEquals( (c % W) * 16 + SettlementLights.HEARTH_MOUTH[0], sm.x, 0f );
			assertEquals( c / W, (int)((sm.y + 8) / 16) );
			assertFalse( "a fire on a wall's lip", SettlementLights.underLip( t[0][c + W] ) );
			fires++;
		}
		assertTrue( "fires " + fires, fires >= 4 );
		for (SettlementAmbience.Light l : b.lights) assertEquals( SettlementLights.KIND_HEARTH, l.kind );
	}

	@Test
	public void ambienceLightsTheTown(){
		int ox = -W / 2, oy = -H / 2;
		WindowGenerator.Window win = WindowGenerator.generate( SEED, 0, ox, oy, WorldModel.calendarShift( Season.WINTER, 0.5f ) );
		int hx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), hy = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		Burning dusk = collect( win.terrain, ox, oy, hx, hy, Phase.DUSK, 0.9f, Season.WINTER, -8f, 3, new HashSet<>() );
		assertEquals( SettlementLights.TOWN_WINDOWS.length, dusk.lights.size() );
		assertEquals( SettlementLights.TOWN_CHIMNEYS.length, dusk.smoke.size() );
		for (SettlementAmbience.Smoke sm : dusk.smoke) assertFalse( "the town is never fogged", sm.gated );
		for (int i = 0; i < SettlementLights.TOWN_WINDOWS.length; i++){
			int[] tw = SettlementLights.TOWN_WINDOWS[i];
			SettlementAmbience.Light l = dusk.lights.get( i );
			assertEquals( tw[1], l.kind );
			assertEquals( (WorldStructures.townWorldX( tw[0] ) - ox) * 16, l.x, 0f );
			assertEquals( (WorldStructures.townWorldY( tw[0] ) - oy) * 16, l.y, 0f );
		}
		//late at night the town sleeps at the inn: its window alone, its hearth kept up
		Burning late = collect( win.terrain, ox, oy, hx, hy, Phase.NIGHT, 0.9f, Season.WINTER, -8f, 3, new HashSet<>() );
		assertEquals( 1, late.lights.size() );
		assertEquals( OverworldLevel.worldKey( WorldStructures.townWorldX( SettlementLights.TOWN_INN_WINDOW ),
				WorldStructures.townWorldY( SettlementLights.TOWN_INN_WINDOW ) ), late.lights.get( 0 ).key );
		int full = 0;
		for (SettlementAmbience.Smoke sm : late.smoke) if (sm.interval == SettlementLights.PUFF_INTERVAL) full++;
		assertEquals( SettlementLights.TOWN_CHIMNEYS.length, late.smoke.size() );
		assertEquals( 1, full );
	}

	@Test
	public void sceneVillageStandsClear(){
		ArrayList<int[]> found = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 );
		assertFalse( found.isEmpty() );
		for (int k = 0; k < Math.min( 8, found.size() ); k++){
			int[] v = found.get( k );
			assertEquals( WorldStructures.Faction.HUMAN, WorldStructures.faction( SEED, v[0], v[1] ) );
			assertTrue( v[4] >= 4 );
			int[] s = SettlementLights.standCell( SEED, v[0], v[1] );
			assertNotNull( s );
			int[] layout = WorldStructures.settlementLayout( SEED, v[0], v[1] );
			for (int i = 1; i + 1 < layout.length; i += 2){
				assertFalse( "stands in a house", Math.abs( s[0] - (v[2] + layout[i]) ) <= 3 && Math.abs( s[1] - (v[3] + layout[i+1]) ) <= 3 );
			}
			assertNotEquals( "stands on the fence", layout[0] + 2, Math.max( Math.abs( s[0] - v[2] ), Math.abs( s[1] - v[3] ) ) );
		}
	}

	//a hut whose door faces north has its own front wall right under the two cells beside its
	//doorstep, and the walls layer draws that wall's lip over them - above the edges layer the
	//ring lies on, so a ring there burned on top of the wall. it goes a step further out there,
	//and no ring anywhere stands on a lip
	@Test
	public void ringsNeverBurnOnAWallLip(){
		int camps = 0, huts = 0, rings = 0, northDoors = 0, northRings = 0;
		for (int[] v : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.GNOLL, 4, 12 )){
			int ox = v[2] - W / 2, oy = v[3] - H / 2;
			int[] map = WindowGenerator.generate( SEED, 0, ox, oy, WorldModel.calendarShift( Season.SUMMER, 0.5f ) ).terrain;
			long own = WorldStructures.sectorOf( v[0], v[1] );
			for (SettlementLights.House h : SettlementLights.housesIn( SEED, ox, oy, ox + W - 1, oy + H - 1 )){
				if (WorldStructures.sectorOf( h.sx, h.sy ) != own) continue;
				huts++;
				boolean north = WorldStructures.houseDoorDY( h.dx, h.dy ) < h.dy;
				if (north) northDoors++;
				int c = SettlementLights.hearthCell( SEED, map, W, H, ox, oy, h );
				if (c == -1) continue;
				rings++;
				if (north) northRings++;
				assertFalse( "a ring on a lip at " + (ox + c % W) + "," + (oy + c / W), SettlementLights.underLip( map[c + W] ) );
			}
			if (++camps == 6) break;
		}
		assertEquals( 6, camps );
		assertTrue( "north doors " + northDoors, northDoors >= 4 );
		assertTrue( "rings by north doors " + northRings + " of " + northDoors, northRings * 10 >= northDoors * 9 );
		assertTrue( "rings " + rings + " of " + huts, rings * 10 >= huts * 9 );
	}

	//the live map's grass grows tall and is trampled flat, and a fire leaves embers: the rings
	//the dressing lays and the fires that burn in them keep to the cells the window's untouched
	//terrain gave them. most rings lie on a footpath; the camps are walked until some lie on
	//the wild grass the old way (by the live map) would have moved
	@Test
	public void ringsStayPutWhileTheGrassChanges(){
		int camps = 0, moved = 0;
		for (int[] v : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.GNOLL, 2, 20 )){
			int ox = v[2] - W / 2, oy = v[3] - H / 2;
			WindowGenerator.Window win = WindowGenerator.generate( SEED, 0, ox, oy, WorldModel.calendarShift( Season.SUMMER, 0.5f ) );
			int[] pristine = win.terrain;
			int[] live = pristine.clone();
			for (int c = 0; c < live.length; c++){
				if (live[c] == Terrain.GRASS) live[c] = Terrain.HIGH_GRASS;
				else if (live[c] == Terrain.HIGH_GRASS) live[c] = Terrain.GRASS;
				else if (live[c] == Terrain.FURROWED_GRASS) live[c] = Terrain.EMBERS;
			}
			long own = WorldStructures.sectorOf( v[0], v[1] );
			for (SettlementLights.House h : SettlementLights.housesIn( SEED, ox, oy, ox + W - 1, oy + H - 1 )){
				if (WorldStructures.sectorOf( h.sx, h.sy ) != own) continue;
				if (SettlementLights.hearthCell( SEED, pristine, W, H, ox, oy, h ) != SettlementLights.hearthCell( SEED, live, W, H, ox, oy, h )) moved++;
			}

			int[][] calm = WindowGenerator.dress( SEED, ox, oy, pristine, win, Season.SUMMER );
			int[][] stirred = WindowGenerator.dress( SEED, ox, oy, live, win, Season.SUMMER );
			for (int c = 0; c < W * H; c++){
				assertEquals( "a ring moved at " + (ox + c % W) + "," + (oy + c / W),
						calm[1][c] == OverworldDress.GNOLL_HEARTH, stirred[1][c] == OverworldDress.GNOLL_HEARTH );
			}

			Burning a = collect( pristine, pristine, ox, oy, v[2], v[3], Phase.DUSK, 0.61f, Season.SUMMER, biome -> 20f, 3, new HashSet<>() );
			Burning b = collect( live, pristine, ox, oy, v[2], v[3], Phase.DUSK, 0.61f, Season.SUMMER, biome -> 20f, 3, new HashSet<>() );
			assertFalse( "no fire at " + v[0] + "," + v[1], a.lights.isEmpty() );
			assertEquals( a.keys, b.keys );
			assertEquals( a.lights.size(), b.lights.size() );
			assertEquals( a.smoke.size(), b.smoke.size() );
			for (int i = 0; i < a.lights.size(); i++){
				assertEquals( a.lights.get( i ).key, b.lights.get( i ).key );
				assertEquals( a.lights.get( i ).x, b.lights.get( i ).x, 0f );
				assertEquals( a.lights.get( i ).y, b.lights.get( i ).y, 0f );
				int c = (int)(b.lights.get( i ).x / 16) + (int)(b.lights.get( i ).y / 16) * W;
				assertEquals( "a fire with no ring under it", OverworldDress.GNOLL_HEARTH, stirred[1][c] );
			}
			if (++camps >= 3 && moved > 0) break;
		}
		assertTrue( "rings the grass would have moved " + moved + " in " + camps + " camps", moved > 0 );
	}

	//a spring day, mild where the hero stands and cold at a village's well (or the other way
	//round): the village smokes by the air at its own well. the hero's own reading follows the
	//biome under his feet, and every chimney in reach used to start and stop together as he
	//crossed a biome line
	@Test
	public void smokeKeepsTheVillagesOwnAir(){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 ).get( 0 );
		int ox = v[2] - W / 2, oy = v[3] - H / 2;
		int[] map = WindowGenerator.generate( SEED, 0, ox, oy, WorldModel.calendarShift( Season.SPRING, 0.5f ) ).terrain;
		WorldModel.Biome home = WorldModel.biomeAt( SEED, v[2], v[3] );
		int[] stand = SettlementLights.standCell( SEED, v[0], v[1] );
		Burning cold = collect( map, map, ox, oy, stand[0], stand[1], Phase.DAY, 0.5f, Season.SPRING,
				biome -> biome == home ? 4f : 16f, 3, new HashSet<>() );
		assertTrue( "stacks smoking in the cold " + cold.smoke.size(), cold.smoke.size() >= 2 );
		Burning mild = collect( map, map, ox, oy, stand[0], stand[1], Phase.DAY, 0.5f, Season.SPRING,
				biome -> biome == home ? 16f : 4f, 3, new HashSet<>() );
		assertTrue( "stacks smoking in the mild " + mild.smoke.size(), mild.smoke.isEmpty() );

		//the town, by its plaza's air
		int tox = -W / 2, toy = -H / 2;
		int[] town = WindowGenerator.generate( SEED, 0, tox, toy, WorldModel.calendarShift( Season.SPRING, 0.5f ) ).terrain;
		int px = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), py = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		WorldModel.Biome plaza = WorldModel.biomeAt( SEED, px, py );
		assertEquals( SettlementLights.TOWN_CHIMNEYS.length, collect( town, town, tox, toy, px, py, Phase.DAY, 0.5f, Season.SPRING,
				biome -> biome == plaza ? 4f : 16f, 3, new HashSet<>() ).smoke.size() );
		assertTrue( collect( town, town, tox, toy, px, py, Phase.DAY, 0.5f, Season.SPRING,
				biome -> biome == plaza ? 16f : 4f, 3, new HashSet<>() ).smoke.isEmpty() );
	}

	//the air a settlement smokes by is the hero's own reading, had he stood out there: the
	//surface's weather, the surface's bias and the place's biome (a debug override forces it)
	@Test
	public void surfaceTempInIsTheOpenAirOfThePlace(){
		WarpedRoomsTest.boot();
		Bundle snap = new Bundle();
		ClimateManager.storeInBundle( snap );
		float override = ClimateManager.debugTempOverride;
		Level level = Dungeon.level;
		try {
			Dungeon.level = null;
			ClimateManager.debugTempOverride = Float.NaN;
			ClimateManager.restoreFromNetwork( 12f, 0.5f, 1013f, 3f, 180f, 0f, 0f, "NONE", "CLEAR",
					false, false, false, false, false );
			ClimateManager.onLevelChange( 0 );
			//no biome under the hero (no surface level): his reading is the plain ground's
			assertEquals( ClimateManager.localTemp(), ClimateManager.surfaceTempIn( WorldModel.Biome.PLAINS ), 1e-4f );
			assertEquals( 10f, ClimateManager.surfaceTempIn( WorldModel.Biome.PLAINS ), 1e-4f );
			assertEquals( 2f, ClimateManager.surfaceTempIn( WorldModel.Biome.FOOTHILLS ), 1e-4f );
			assertEquals( 13f, ClimateManager.surfaceTempIn( WorldModel.Biome.SWAMP ), 1e-4f );
			assertEquals( 22f, ClimateManager.surfaceTempIn( WorldModel.Biome.DESERT ), 1e-4f );
			assertEquals( -10f, ClimateManager.surfaceTempIn( WorldModel.Biome.SNOWFIELD ), 1e-4f );
			ClimateManager.debugTempOverride = 3f;
			for (WorldModel.Biome b : WorldModel.Biome.values()) assertEquals( 3f, ClimateManager.surfaceTempIn( b ), 0f );
		} finally {
			ClimateManager.debugTempOverride = override;
			ClimateManager.restoreFromBundle( snap );
			Dungeon.level = level;
		}
	}
}
