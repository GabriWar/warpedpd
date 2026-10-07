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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Clock;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Kind;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Role;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Settlement;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Slot;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Target;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Weather;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The settlements' day (VillageRoutine, Settler, the guards' watch): the trades rolled from
 * the family hash, the part of the day each is in by the hour and the weather, the spots
 * those parts put them on, what they say, what their saves carry - and, on a real generated
 * window, that everyone coming into it lands where the day says, one to a cell.
 */
public class VillageRoutineTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, startDay, challenges;
	private float duration, precip;
	private Phase override;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		duration = Statistics.duration;
		precip = ClimateManager.debugPrecipOverride;
		override = DayNightCycle.debugPhaseOverride;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = null;
	}

	@After
	public void restore(){
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		Statistics.duration = duration;
		ClimateManager.debugPrecipOverride = precip;
		DayNightCycle.debugPhaseOverride = override;
	}

	// ---------------------------------------------------------------- helpers

	private static Clock clock( Phase p, float t ){
		return new Clock( p, t, Weather.FAIR, false, 0, 0 );
	}

	private static Clock clock( Phase p, float t, Weather w ){
		return new Clock( p, t, w, false, 0, 0 );
	}

	//a member's timing bits, as VillageRoutine.personal lays them out
	private static int personal( int wake, int linger, int breakBits, int quit, int breakOff ){
		return (wake & 0xFF) | ((linger & 0xFF) << 8) | ((breakBits & 3) << 16) | ((quit & 0xFF) << 20) | ((breakOff & 15) << 28);
	}

	private static Settlement first( WorldStructures.Faction faction, int minHouses ){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, faction, minHouses, 12 ).get( 0 );
		Settlement s = VillageRoutine.settlement( SEED, WorldStructures.sectorOf( v[0], v[1] ) );
		assertNotNull( s );
		return s;
	}

	private static Settlement human(){
		return first( WorldStructures.Faction.HUMAN, 6 );
	}

	private static int ring( Settlement s, int wx, int wy ){
		return Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) );
	}

	//the world's clock set to a point of the day in a season, the rain held off; a jump, so
	//the villages take the sky as it is
	private static void at( Season season, Phase phase, float into ){
		Dungeon.cycleTurn = 0;
		for (int d = 0; d < 400 && GameCalendar.season() != season; d++) Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		int start = 0;
		for (Phase p = Phase.DAWN; p != phase; p = p.next()) start += DayNightCycle.phaseDuration( p );
		Dungeon.cycleTurn += start + Math.round( into * DayNightCycle.phaseDuration( phase ) );
		Statistics.duration += 1000;
		ClimateManager.debugPrecipOverride = 0f;
		assertEquals( phase, DayNightCycle.phase() );
	}

	//a real window of the world centred on a cell: a network mirror of it, which needs no
	//scene and runs nothing - just its terrain and flag maps
	private static OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
		OverworldLevel l = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( l );
		return l;
	}

	//the families of a settlement as populateSettlement makes them, each set down where its
	//arrival says (and kept off the level when there is none)
	private static ArrayList<Settler> peopleArrive( OverworldLevel ow, Settlement s, HashMap<Settler, Integer> cells ){
		ArrayList<Settler> out = new ArrayList<>();
		for (int h = s.firstFamily(); h < s.lastFamily(); h++){
			long fh = VillageRoutine.familyHash( s.sx, s.sy, h );
			int members = 1 + (int) Math.floorMod( fh, 3 );
			for (int m = 0; m < members; m++){
				Settler f;
				if (s.gnoll){
					GnollVillager g = new GnollVillager();
					g.homeSector = s.key;
					f = g;
				} else {
					Villager v = new Villager();
					v.homeSector = s.key;
					f = v;
				}
				f.house = h;
				f.member = m;
				f.role = VillageRoutine.roleOf( fh, m, s.gnoll ).ordinal();
				f.homeX = s.houseX( h );
				f.homeY = s.houseY( h );
				int at = VillageRoutine.arrivalCell( ow, f, f.homeX, f.homeY );
				cells.put( f, at );
				if (at != -1){
					f.pos = at;
					ow.mobs.add( f );
				}
				out.add( f );
			}
		}
		return out;
	}

	private static Level bareLevel( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( w, h );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		return l;
	}

	// ---------------------------------------------------------------- trades

	@Test
	public void rolesFollowTheirShares(){
		java.util.Random rnd = new java.util.Random( 7 );
		int[] human = new int[Role.values().length];
		int hunters = 0, n = 0;
		for (int i = 0; i < 10000; i++){
			long h = rnd.nextLong();
			for (int m = 0; m < 3; m++){
				human[VillageRoutine.roleOf( h, m, false ).ordinal()]++;
				if (VillageRoutine.roleOf( h, m, true ) == Role.HUNTER) hunters++;
				n++;
			}
		}
		float[] want = { 0.35f, 0.15f, 0.15f, 0.10f, 0.25f };
		for (int r = 0; r < want.length; r++){
			assertEquals( Role.values()[r].name(), want[r], human[r] / (float) n, 0.02f );
		}
		assertEquals( 0, human[Role.HUNTER.ordinal()] + human[Role.GATHERER.ordinal()] );
		assertEquals( 0.5f, hunters / (float) n, 0.02f );
	}

	@Test
	public void rolesAreDeterministicAndFactionBound(){
		java.util.Random rnd = new java.util.Random( 11 );
		for (int i = 0; i < 1000; i++){
			long h = rnd.nextLong();
			int m = rnd.nextInt( 3 );
			assertEquals( VillageRoutine.roleOf( h, m, false ), VillageRoutine.roleOf( h, m, false ) );
			assertTrue( VillageRoutine.belongs( VillageRoutine.roleOf( h, m, false ), false ) );
			assertTrue( VillageRoutine.belongs( VillageRoutine.roleOf( h, m, true ), true ) );
			assertFalse( VillageRoutine.belongs( VillageRoutine.roleOf( h, m, true ), false ) );
		}
	}

	// ---------------------------------------------------------------- the hours

	@Test
	public void nightIsForSleeping(){
		java.util.Random rnd = new java.util.Random( 3 );
		for (Role r : Role.values()){
			for (int i = 0; i < 50; i++){
				assertEquals( r.name(), Slot.BED, VillageRoutine.slotFor( r, clock( Phase.NIGHT, 0.3f ), rnd.nextInt() ) );
			}
		}
		int lingers = personal( 0, 255, 1, 0, 0 );
		assertEquals( Slot.GATHER, VillageRoutine.slotFor( Role.LOAFER, clock( Phase.NIGHT, 0f ), lingers ) );
		assertEquals( Slot.GATHER, VillageRoutine.slotFor( Role.FARMER, clock( Phase.NIGHT, 0f ), lingers ) );
		assertEquals( Slot.BED, VillageRoutine.slotFor( Role.ELDER, clock( Phase.NIGHT, 0f ), lingers ) );
	}

	@Test
	public void dawnEmptiesTheHousesGradually(){
		for (Role r : Role.values()){
			assertEquals( r.name(), Slot.BED, VillageRoutine.slotFor( r, clock( Phase.DAWN, 0.02f ), personal( 0, 0, 1, 0, 0 ) ) );
			assertEquals( r.name(), Slot.WORK, VillageRoutine.slotFor( r, clock( Phase.DAWN, 0.95f ), personal( 255, 0, 1, 0, 0 ) ) );
		}
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DAWN, 0.25f ), personal( 0, 0, 1, 0, 0 ) ) );
		assertEquals( Slot.BED, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DAWN, 0.25f ), personal( 255, 0, 1, 0, 0 ) ) );
		for (int wake = 0; wake < 256; wake++){
			assertEquals( Slot.BED, VillageRoutine.slotFor( Role.LOAFER, clock( Phase.DAWN, 0.5f ), personal( wake, 0, 1, 0, 0 ) ) );
		}
	}

	@Test
	public void dayIsForWorkWithABreakForSome(){
		int taker = personal( 0, 0, 0, 0, 0 );
		assertEquals( Slot.BREAK, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DAY, 0.45f ), taker ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DAY, 0.3f ), taker ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.ELDER, clock( Phase.DAY, 0.45f ), taker ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.LOAFER, clock( Phase.DAY, 0.45f ), taker ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DAY, 0.45f ), personal( 0, 0, 1, 0, 0 ) ) );
	}

	@Test
	public void duskGathersAtTheWell(){
		int early = personal( 0, 0, 1, 0, 0 );
		assertEquals( Slot.GATHER, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DUSK, 0.1f ), early ) );
		assertEquals( Slot.GATHER, VillageRoutine.slotFor( Role.ELDER, clock( Phase.DUSK, 0.1f ), early ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.FISHER, clock( Phase.DUSK, 0.1f ), early ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.HUNTER, clock( Phase.DUSK, 0.1f ), early ) );
		assertEquals( Slot.GATHER, VillageRoutine.slotFor( Role.GATHERER, clock( Phase.DUSK, 0.1f ), early ) );
		assertEquals( Slot.BED, VillageRoutine.slotFor( Role.ELDER, clock( Phase.DUSK, 0.6f ), early ) );
		assertEquals( Slot.GATHER, VillageRoutine.slotFor( Role.FISHER, clock( Phase.DUSK, 0.6f ), early ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DUSK, 0.1f ), personal( 0, 0, 1, 255, 0 ) ) );
	}

	// ---------------------------------------------------------------- the weather

	//a travelling market in the village (WorldEvents): by mid-morning, when nobody is at the well
	//on a plain day, the loafers, the elders and the craftsmen are among the stalls; a break is
	//spent there too, while the fields, the banks, the weather and the night keep their own
	@Test
	public void aMarketDrawsTheVillageToTheWell(){
		java.util.Random rnd = new java.util.Random( 13 );
		int plain = 0, market = 0, n = 0;
		for (int i = 0; i < 3000; i++){
			long h = rnd.nextLong();
			for (int m = 0; m < 3; m++){
				Role r = VillageRoutine.roleOf( h, m, false );
				Slot s = VillageRoutine.slotFor( r, clock( Phase.DAY, 0.3f ), VillageRoutine.personal( h, m ) );
				if (s == Slot.GATHER) plain++;
				if (VillageRoutine.marketSlot( r, s ) == Slot.GATHER) market++;
				n++;
			}
		}
		assertEquals( 0, plain );
		assertEquals( 0.5f, market / (float) n, 0.03f );
		for (Role r : new Role[]{ Role.LOAFER, Role.ELDER, Role.CRAFTER }){
			assertEquals( Slot.GATHER, VillageRoutine.marketSlot( r, Slot.WORK ) );
		}
		for (Role r : Role.values()){
			assertEquals( Slot.GATHER, VillageRoutine.marketSlot( r, Slot.BREAK ) );
			assertEquals( Slot.BED, VillageRoutine.marketSlot( r, Slot.BED ) );
			assertEquals( Slot.INDOORS, VillageRoutine.marketSlot( r, Slot.INDOORS ) );
		}
		assertEquals( Slot.WORK, VillageRoutine.marketSlot( Role.FARMER, Slot.WORK ) );
		assertEquals( Slot.WORK, VillageRoutine.marketSlot( Role.FISHER, Slot.WORK ) );
	}

	@Test
	public void weatherSendsPeopleIndoors(){
		int p = personal( 0, 0, 1, 0, 0 );
		for (Role r : Role.values()){
			assertEquals( r.name(), Slot.INDOORS, VillageRoutine.slotFor( r, clock( Phase.DAY, 0.3f, Weather.STORM ), p ) );
			assertEquals( r.name(), Slot.INDOORS, VillageRoutine.slotFor( r, clock( Phase.DAY, 0.3f, Weather.RAIN ), p ) );
			assertEquals( r.name(), Slot.BED, VillageRoutine.slotFor( r, clock( Phase.NIGHT, 0.5f, Weather.STORM ), p ) );
		}
		assertEquals( Slot.INDOORS, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DUSK, 0.6f, Weather.RAIN ), p ) );
		assertEquals( Slot.INDOORS, VillageRoutine.slotFor( Role.FARMER, clock( Phase.DAY, 0.3f, Weather.SNOW ), p ) );
		assertEquals( Slot.INDOORS, VillageRoutine.slotFor( Role.GATHERER, clock( Phase.DAY, 0.3f, Weather.SNOW ), p ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.CRAFTER, clock( Phase.DAY, 0.3f, Weather.SNOW ), p ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.FISHER, clock( Phase.DAY, 0.3f, Weather.SNOW ), p ) );
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.LOAFER, clock( Phase.DAY, 0.3f, Weather.SNOW ), p ) );
	}

	//a shower does not empty the fields in one turn: each farmer goes in its own number of
	//turns after the villages took the rain in
	@Test
	public void aChangeInTheSkyIsTakenInOneByOne(){
		java.util.Random rnd = new java.util.Random( 5 );
		HashSet<Integer> delays = new HashSet<>();
		for (int i = 0; i < 200; i++){
			int p = personal( 0, 0, 1, rnd.nextInt( 256 ), rnd.nextInt( 16 ) );
			int d = VillageRoutine.weatherDelay( p );
			assertTrue( d >= 0 && d < 32 );
			delays.add( d );
			for (int since = 0; since < 40; since++){
				Clock c = new Clock( Phase.DAY, 0.3f, Weather.RAIN, Weather.FAIR, Weather.FAIR, 1000, 900, 1000 + since, false, 0 );
				assertEquals( "delay " + d + ", " + since + " turns on", since >= d ? Slot.INDOORS : Slot.WORK,
						VillageRoutine.slotFor( Role.FARMER, c, p ) );
			}
		}
		assertTrue( "delays " + delays, delays.size() > 16 );
	}

	//a shower that is over before the slowest have taken it in is still lived whole and in
	//order by each of them: every member acts on the sky as the villages believed it its own
	//few turns ago, so each is indoors exactly as long as the rain was believed, a little
	//after the next - never sent in once it has cleared without having seen it come
	@Test
	public void aShortShowerIsLivedInOrder(){
		VillageRoutine.Latch l = new VillageRoutine.Latch();
		int start = 1000, end = 1021;   //raw rain over [start, end), believed from start + WEATHER_HOLD
		int believed = start + VillageRoutine.WEATHER_HOLD;
		int over = -1;
		HashMap<Integer, Integer> inFrom = new HashMap<>(), inTurns = new HashMap<>();
		for (int t = start - 50; t < start + 200; t++){
			l.read( t >= start && t < end ? Weather.RAIN : Weather.FAIR, t );
			if (over == -1 && t > believed && l.weather == Weather.FAIR) over = t;
			Clock c = new Clock( Phase.DAY, 0.3f, l.weather, l.before, l.earlier, l.changed, l.prevChanged, t, false, 0 );
			for (int d = 0; d < 32; d++){
				int p = personal( 0, 0, 1, 0, 0 ) | (d << 24);
				assertEquals( d, VillageRoutine.weatherDelay( p ) );
				//the sky as believed d turns ago
				int at = t - d;
				Weather then = at >= believed && (over == -1 || at < over) ? Weather.RAIN : Weather.FAIR;
				assertEquals( "delay " + d + ", turn " + t, then, c.weatherFor( p ) );
				if (VillageRoutine.slotFor( Role.FARMER, c, p ) == Slot.INDOORS){
					if (!inFrom.containsKey( d )) inFrom.put( d, t );
					inTurns.merge( d, 1, Integer::sum );
				}
			}
		}
		assertTrue( "the rain was believed, then the fair weather: " + over, over > believed );
		for (int d = 0; d < 32; d++){
			assertEquals( "delay " + d + " goes in its own turns after the rain is believed",
					believed + d, (int) inFrom.get( d ) );
			assertEquals( "delay " + d + " is in as long as the rain was believed",
					over - believed, (int) inTurns.get( d ) );
		}
	}

	//the villages believe a change only once it has held a while; a flicker is ignored, and a
	//jump in time takes the sky as it is
	@Test
	public void theSkyIsBelievedOnceItHolds(){
		VillageRoutine.Latch l = new VillageRoutine.Latch();
		l.read( Weather.FAIR, 100 );
		assertEquals( Weather.FAIR, l.weather );
		for (int t = 101; t < 101 + VillageRoutine.WEATHER_HOLD; t++){
			l.read( Weather.RAIN, t );
			assertEquals( "turn " + t, Weather.FAIR, l.weather );
		}
		l.read( Weather.RAIN, 101 + VillageRoutine.WEATHER_HOLD );
		assertEquals( Weather.RAIN, l.weather );
		assertEquals( Weather.FAIR, l.before );
		assertEquals( 101 + VillageRoutine.WEATHER_HOLD, l.changed );

		//a flicker: fair for a few turns, rain again - still rain all along
		for (int t = 130; t < 140; t++) l.read( t % 2 == 0 ? Weather.FAIR : Weather.RAIN, t );
		assertEquals( Weather.RAIN, l.weather );

		//time jumped: whatever it is now, everyone already knows
		l.read( Weather.SNOW, 5000 );
		assertEquals( Weather.SNOW, l.weather );
		assertEquals( Weather.SNOW, l.before );
		assertTrue( 5000 - l.changed >= 32 );
	}

	// ---------------------------------------------------------------- spots

	@Test
	public void targetsSitWhereTheRoleWorks(){
		Settlement s = human();
		assertTrue( "the test's village has no fields", s.farms() );
		Clock summer = clock( Phase.DAY, 0.3f );
		Clock winter = new Clock( Phase.DAY, 0.3f, Weather.FAIR, true, 0, 0 );
		int R = s.radius;
		for (int h = s.firstFamily(); h < s.lastFamily(); h++){
			int hx = s.houseX( h ), hy = s.houseY( h );
			int ix = VillageRoutine.inX( s, hx, hy ), iy = VillageRoutine.inY( s, hx, hy );
			assertEquals( 1, Math.abs( ix ) + Math.abs( iy ) );
			for (int m = 0; m < 3; m++){
				Target bed = VillageRoutine.target( s, h, hx, hy, m, Role.FARMER, Slot.BED, summer, new long[0] );
				assertEquals( Kind.HOME, bed.kind );
				assertTrue( Math.abs( bed.wx - hx ) <= 1 && Math.abs( bed.wy - hy ) <= 1 );
				assertEquals( 1, (bed.wx - hx) * ix + (bed.wy - hy) * iy );   //the back row
				assertEquals( Terrain.EMPTY_SP, WorldStructures.terrainAt( SEED, bed.wx, bed.wy ) );

				Target in = VillageRoutine.target( s, h, hx, hy, m, Role.FARMER, Slot.INDOORS, summer, new long[0] );
				assertTrue( Math.abs( in.wx - hx ) <= 1 && Math.abs( in.wy - hy ) <= 1 );
				assertEquals( 0, (in.wx - hx) * ix + (in.wy - hy) * iy );     //the middle row
				assertEquals( Terrain.EMPTY_SP, WorldStructures.terrainAt( SEED, in.wx, in.wy ) );

				Target craft = VillageRoutine.target( s, h, hx, hy, m, Role.CRAFTER, Slot.WORK, summer, new long[0] );
				assertEquals( Kind.STREET, craft.kind );
				assertEquals( 3, Math.max( Math.abs( craft.wx - hx ), Math.abs( craft.wy - hy ) ) );
				Target elder = VillageRoutine.target( s, h, hx, hy, m, Role.ELDER, Slot.WORK, summer, new long[0] );
				assertEquals( 4, Math.max( Math.abs( elder.wx - hx ), Math.abs( elder.wy - hy ) ) );

				//a place at the edge of one of the village's fields, outside the fence
				Target field = VillageRoutine.target( s, h, hx, hy, m, Role.FARMER, Slot.WORK, summer, new long[0] );
				assertEquals( Kind.FIELD, field.kind );
				int[] plots = WorldStructures.fieldPlots( SEED, s.sx, s.sy );
				assertFalse( "in the field itself", WorldStructures.inField( plots, field.wx, field.wy ) );
				boolean beside = false;
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++) beside |= WorldStructures.inField( plots, field.wx + dx, field.wy + dy );
				}
				assertTrue( "not beside a field", beside );
				int fr = ring( s, field.wx, field.wy );
				assertTrue( "ring " + fr, fr >= R + WorldStructures.FIELD_RING0 - 1 && fr <= R + WorldStructures.FIELD_REACH + 1 );
				assertTrue( fr >= field.ringMin && fr <= field.ringMax );
				Target snowbound = VillageRoutine.target( s, h, hx, hy, m, Role.FARMER, Slot.WORK, winter, new long[0] );
				Target craftWinter = VillageRoutine.target( s, h, hx, hy, m, Role.CRAFTER, Slot.WORK, winter, new long[0] );
				assertEquals( craftWinter.wx, snowbound.wx );
				assertEquals( craftWinter.wy, snowbound.wy );

				for (Slot slot : new Slot[]{ Slot.GATHER, Slot.BREAK }){
					Target well = VillageRoutine.target( s, h, hx, hy, m, Role.FARMER, slot, summer, new long[0] );
					assertEquals( Kind.WELL, well.kind );
					assertEquals( 3, ring( s, well.wx, well.wy ) );
				}
				Target loaf = VillageRoutine.target( s, h, hx, hy, m, Role.LOAFER, Slot.WORK, summer, new long[0] );
				assertEquals( Kind.WELL, loaf.kind );
				assertEquals( 3, ring( s, loaf.wx, loaf.wy ) );
			}
		}
	}

	@Test
	public void theFamilysFirstTwoTalkSideBySide(){
		Settlement s = human();
		for (int day = 0; day < 7; day++){
			Clock dusk = new Clock( Phase.DUSK, 0.5f, Weather.FAIR, false, day, 0 );
			for (int h = s.firstFamily(); h < s.lastFamily(); h++){
				Target a = VillageRoutine.target( s, h, s.houseX( h ), s.houseY( h ), 0, Role.FARMER, Slot.GATHER, dusk, new long[0] );
				Target b = VillageRoutine.target( s, h, s.houseX( h ), s.houseY( h ), 1, Role.LOAFER, Slot.GATHER, dusk, new long[0] );
				assertEquals( a.wy, b.wy );
				assertEquals( 3, Math.abs( a.wy - s.cy ) );
				assertEquals( 1, Math.abs( a.wx - b.wx ) );
			}
		}
	}

	@Test
	public void fisherWithoutWaterWorksTheFields(){
		Settlement s = human();
		Clock c = clock( Phase.DAY, 0.3f );
		int h = s.firstFamily(), hx = s.houseX( h ), hy = s.houseY( h );
		Target dry = VillageRoutine.target( s, h, hx, hy, 0, Role.FISHER, Slot.WORK, c, new long[0] );
		Target farm = VillageRoutine.target( s, h, hx, hy, 0, Role.FARMER, Slot.WORK, c, new long[0] );
		assertEquals( Kind.FIELD, dry.kind );
		assertEquals( farm.wx, dry.wx );
		assertEquals( farm.wy, dry.wy );

		long a = OverworldLevel.worldKey( s.cx + 20, s.cy + 3 ), b = OverworldLevel.worldKey( s.cx - 17, s.cy );
		Target bank = VillageRoutine.target( s, h, hx, hy, 0, Role.FISHER, Slot.WORK, c, new long[]{ a, b } );
		assertEquals( Kind.SHORE, bank.kind );
		long got = OverworldLevel.worldKey( bank.wx, bank.wy );
		assertTrue( got == a || got == b );

		//many banks: only the six nearest (the list comes nearest first) are worked
		long[] banks = new long[20];
		for (int i = 0; i < banks.length; i++) banks[i] = OverworldLevel.worldKey( s.cx + 10 + i, s.cy );
		for (int house = s.firstFamily(); house < s.lastFamily(); house++){
			for (int m = 0; m < 3; m++){
				Target t = VillageRoutine.target( s, house, s.houseX( house ), s.houseY( house ), m, Role.FISHER, Slot.WORK, c, banks );
				assertTrue( t.wx - s.cx - 10 < 6 );
			}
		}
	}

	//a hunter's spot moves HUNT_STRIDE cells on round the fence every HUNT_LEG turns, a
	//gatherer's FORAGE_STRIDE along the brush every FORAGE_LEG, each leg at its own turn
	@Test
	public void hunterWalksTheFence(){
		Settlement s = first( WorldStructures.Faction.GNOLL, 2 );
		int R = s.radius + 1, h = s.firstFamily();
		for (int m = 0; m < 3; m++){
			long spot = VillageRoutine.spotHash( VillageRoutine.familyHash( s.sx, s.sy, h ), m );
			for (Role r : new Role[]{ Role.HUNTER, Role.GATHERER }){
				boolean hunter = r == Role.HUNTER;
				int leg = hunter ? VillageRoutine.HUNT_LEG : VillageRoutine.FORAGE_LEG;
				int stride = hunter ? VillageRoutine.HUNT_STRIDE : VillageRoutine.FORAGE_STRIDE;
				int rr = hunter ? R : s.radius;
				//the turn this member's leg turns over, somewhere in the first leg
				int turn = 5000, b0 = VillageRoutine.beatOf( r, Slot.WORK, clockAt( turn ), spot );
				while (VillageRoutine.beatOf( r, Slot.WORK, clockAt( turn ), spot ) == b0) turn++;
				assertTrue( turn - 5000 <= leg );
				Target t0 = VillageRoutine.target( s, h, s.houseX( h ), s.houseY( h ), m, r, Slot.WORK, clockAt( turn - 1 ), new long[0] );
				Target t1 = VillageRoutine.target( s, h, s.houseX( h ), s.houseY( h ), m, r, Slot.WORK, clockAt( turn ), new long[0] );
				Target t2 = VillageRoutine.target( s, h, s.houseX( h ), s.houseY( h ), m, r, Slot.WORK, clockAt( turn + leg - 1 ), new long[0] );
				assertEquals( hunter ? Kind.RING : Kind.BRUSH, t0.kind );
				assertEquals( rr, ring( s, t0.wx, t0.wy ) );
				assertEquals( rr, ring( s, t1.wx, t1.wy ) );
				assertEquals( stride, Math.floorMod( ringIndex( rr, t1.wx - s.cx, t1.wy - s.cy ) - ringIndex( rr, t0.wx - s.cx, t0.wy - s.cy ), 8 * rr ) );
				assertEquals( t0.beat + 1, t1.beat );
				assertEquals( "the spot holds the whole leg", t1.wx, t2.wx );
				assertEquals( t1.wy, t2.wy );
				assertEquals( t1.beat, t2.beat );
			}
			//nobody else's spot moves with the turns
			assertEquals( 0, VillageRoutine.beatOf( Role.FARMER, Slot.WORK, clockAt( 777 ), spot ) );
			assertEquals( 0, VillageRoutine.beatOf( Role.HUNTER, Slot.GATHER, clockAt( 777 ), spot ) );
		}
	}

	private static Clock clockAt( int turn ){
		return new Clock( Phase.DAY, 0.3f, Weather.FAIR, false, 0, turn );
	}

	//a clan does not set off as one: its hunters' legs turn over on many different turns
	@Test
	public void aClanSetsOffOnTurnsOfItsOwn(){
		Settlement s = first( WorldStructures.Faction.GNOLL, 2 );
		HashSet<Integer> turns = new HashSet<>();
		int hunters = 0;
		for (int h = s.firstFamily(); h < s.lastFamily(); h++){
			long fh = VillageRoutine.familyHash( s.sx, s.sy, h );
			for (int m = 0; m < 3; m++){
				long spot = VillageRoutine.spotHash( fh, m );
				int turn = 9000, b0 = VillageRoutine.beatOf( Role.HUNTER, Slot.WORK, clockAt( turn ), spot );
				while (VillageRoutine.beatOf( Role.HUNTER, Slot.WORK, clockAt( turn ), spot ) == b0) turn++;
				turns.add( turn );
				hunters++;
			}
		}
		assertTrue( hunters + " hunters set off on " + turns.size() + " turns", turns.size() * 2 >= hunters );
	}

	//a leg of a hunter's round is short enough that the shortest way along it - corners and
	//all - keeps to the fence's band (rings radius..radius+1), never across the camp
	@Test
	public void aLegKeepsToTheFence(){
		int size = 80, c0 = size / 2;
		PathFinder.setMapSize( size, size );
		boolean[] open = new boolean[size * size];
		java.util.Arrays.fill( open, true );
		for (int R = 4; R <= 30; R++){
			for (int k = 0; k < 8 * R; k++){
				int k1 = (k + VillageRoutine.HUNT_STRIDE) % (8 * R);
				int from = c0 + VillageRoutine.ringX( R, k ) + (c0 + VillageRoutine.ringY( R, k )) * size;
				int to = c0 + VillageRoutine.ringX( R, k1 ) + (c0 + VillageRoutine.ringY( R, k1 )) * size;
				PathFinder.Path path = PathFinder.find( from, to, open );
				assertNotNull( path );
				for (int c : path){
					int r = Math.max( Math.abs( c % size - c0 ), Math.abs( c / size - c0 ) );
					assertTrue( "ring " + R + " leg from " + k + " dips to ring " + r, r >= R - 1 && r <= R );
				}
			}
		}
	}

	private static int ringIndex( int r, int dx, int dy ){
		for (int k = 0; k < 8 * r; k++){
			if (VillageRoutine.ringX( r, k ) == dx && VillageRoutine.ringY( r, k ) == dy) return k;
		}
		throw new AssertionError( "not on ring " + r + ": " + dx + "," + dy );
	}

	@Test
	public void ringsAreWalkedRoundOnceEach(){
		for (int r = 1; r < 40; r++){
			HashSet<Long> seen = new HashSet<>();
			for (int k = 0; k < 8 * r; k++){
				int x = VillageRoutine.ringX( r, k ), y = VillageRoutine.ringY( r, k );
				assertEquals( r, Math.max( Math.abs( x ), Math.abs( y ) ) );
				assertTrue( seen.add( OverworldLevel.worldKey( x, y ) ) );
				int k2 = (k + 1) % (8 * r);
				assertEquals( 1, Math.max( Math.abs( VillageRoutine.ringX( r, k2 ) - x ), Math.abs( VillageRoutine.ringY( r, k2 ) - y ) ) );
			}
		}
	}

	@Test
	public void familiesHaveHouses(){
		for (int n = 2; n <= 90; n++){
			assertTrue( "houses " + n, VillageRoutine.vendorHouses( n ) < WorldStructures.populatedHouses( n ) );
		}
	}

	@Test
	public void guardPostsMatchTheSpawn(){
		PathFinder.setMapSize( W, H );
		int centre = 88 + 88 * W;
		for (int g = 0; g < 4; g++){
			int[] o = VillageRoutine.GUARD_POSTS[g];
			assertEquals( centre + PathFinder.NEIGHBOURS8[(g * 2) % 8] * 2, centre + o[0] + o[1] * W );
		}
		Settlement s = human();
		assertEquals( 2, VillageRoutine.nearestPost( s, s.cx + 2, s.cy ) );
		assertEquals( 3, VillageRoutine.nearestPost( s, s.cx, s.cy + 3 ) );
	}

	@Test
	public void noSettlementForBanditsOrNoKey(){
		assertEquals( null, VillageRoutine.settlement( SEED, Long.MIN_VALUE ) );
		int[] camp = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.BANDIT, 1, 12 ).get( 0 );
		assertEquals( null, VillageRoutine.settlement( SEED, WorldStructures.sectorOf( camp[0], camp[1] ) ) );
		assertTrue( first( WorldStructures.Faction.GNOLL, 2 ).gnoll );
		assertFalse( human().gnoll );
	}

	// ---------------------------------------------------------------- chatter

	@Test
	public void everyChatLineResolves(){
		Class<?>[] kinds = { Villager.class, GnollVillager.class };
		for (Class<?> cls : kinds){
			boolean gnoll = cls == GnollVillager.class;
			for (Role r : Role.values()){
				if (!VillageRoutine.belongs( r, gnoll )) continue;
				for (Slot s : Slot.values()){
					for (Phase p : Phase.values()){
						for (boolean winter : new boolean[]{ false, true }){
							for (boolean farms : new boolean[]{ false, true }){
								String stem = VillageRoutine.chatStem( r, s, new Clock( p, 0.5f, Weather.FAIR, winter, 0, 0 ), farms );
								for (int i = 0; i < VillageRoutine.chatLines( stem ); i++){
									String key = stem + "_" + i;
									assertNotEquals( cls.getSimpleName() + "." + key, Messages.NO_TEXT_FOUND, Messages.get( cls, key ) );
								}
							}
						}
					}
				}
			}
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( cls, "asleep" ) );
		}
	}

	// ---------------------------------------------------------------- saves

	@Test
	public void villagerRoundTripsItsRoutine(){
		Villager v = new Villager();
		v.look = 2;
		v.tint = 5;
		v.homeSector = WorldStructures.sectorOf( 3, -4 );
		v.house = 4;
		v.member = 2;
		v.role = Role.FISHER.ordinal();
		v.slot = Slot.GATHER.ordinal();
		v.homeX = 301;
		v.homeY = -377;
		Bundle b = new Bundle();
		v.storeInBundle( b );
		Villager w = new Villager();
		w.restoreFromBundle( b );
		assertEquals( 2, w.look );
		assertEquals( 5, w.tint );
		assertEquals( v.homeSector, w.homeSector );
		assertEquals( 4, w.house );
		assertEquals( 2, w.member );
		assertEquals( Role.FISHER.ordinal(), w.role );
		assertEquals( Slot.GATHER.ordinal(), w.slot );
		assertEquals( 301, w.homeX );
		assertEquals( -377, w.homeY );
	}

	@Test
	public void gnollRoundTripsItsRoutine(){
		GnollVillager g = new GnollVillager();
		g.tint = 6;
		g.homeSector = WorldStructures.sectorOf( -7, 2 );
		g.house = 1;
		g.member = 0;
		g.role = Role.GATHERER.ordinal();
		g.slot = Slot.WORK.ordinal();
		g.homeX = -660;
		g.homeY = 210;
		Bundle b = new Bundle();
		g.storeInBundle( b );
		GnollVillager h = new GnollVillager();
		h.restoreFromBundle( b );
		assertEquals( 6, h.tint );
		assertEquals( g.homeSector, h.homeSector );
		assertEquals( 1, h.house );
		assertEquals( 0, h.member );
		assertEquals( Role.GATHERER.ordinal(), h.role );
		assertEquals( Slot.WORK.ordinal(), h.slot );
		assertEquals( -660, h.homeX );
		assertEquals( 210, h.homeY );
	}

	private static final String[] ROUTINE_KEYS = { "routine_house", "routine_member", "routine_role",
			"routine_slot", "routine_home_x", "routine_home_y" };

	@Test
	public void oldVillagerSaveAdoptsAHome(){
		Settlement s = human();
		Villager v = new Villager();
		v.homeSector = s.key;
		Bundle b = new Bundle();
		v.storeInBundle( b );
		for (String k : ROUTINE_KEYS) assertTrue( k, b.remove( k ) );

		Villager old = new Villager();
		old.restoreFromBundle( b );
		assertEquals( -1, old.house );
		assertEquals( -1, old.member );
		assertEquals( -1, old.role );
		assertEquals( -1, old.slot );
		assertEquals( Integer.MIN_VALUE, old.homeX );
		assertEquals( Integer.MIN_VALUE, old.homeY );

		int f = s.firstFamily();
		old.adopt( s, s.houseX( f ) + 1, s.houseY( f ), null );
		assertEquals( f, old.house );
		assertEquals( s.houseX( f ), old.homeX );
		assertEquals( s.houseY( f ), old.homeY );
		assertTrue( old.member >= 0 && old.member < 3 );
		assertEquals( VillageRoutine.roleOf( VillageRoutine.familyHash( s.sx, s.sy, f ), old.member, false ).ordinal(), old.role );

		Bundle again = new Bundle();
		old.storeInBundle( again );
		for (String k : ROUTINE_KEYS) assertTrue( k, again.contains( k ) );

		//a gnoll's trade in a villager's save (or the reverse) is rolled again
		Villager mixed = new Villager();
		mixed.role = Role.HUNTER.ordinal();
		mixed.adopt( s, s.houseX( f ), s.houseY( f ), null );
		assertTrue( VillageRoutine.belongs( Role.values()[mixed.role], false ) );
	}

	@Test
	public void oldGuardSaveKeepsAPost(){
		OverworldGuard g = OverworldGuard.random( WorldStructures.sectorOf( 1, 1 ) );
		g.post = 2;
		Bundle b = new Bundle();
		g.storeInBundle( b );
		OverworldGuard back = new OverworldGuard();
		back.restoreFromBundle( b );
		assertEquals( 2, back.post );
		assertTrue( b.remove( "routine_post" ) );
		OverworldGuard old = new OverworldGuard();
		old.restoreFromBundle( b );
		assertEquals( -1, old.post );
	}

	// ---------------------------------------------------------------- the work, seen

	@Test
	public void flourishWithNothingBesideIsANoOp(){
		Level l = bareLevel( 12, 12 );
		PathFinder.setMapSize( 12, 12 );
		java.util.Arrays.fill( l.map, Terrain.EMPTY );
		int pos = 5 + 5 * 12;
		l.map[pos] = Terrain.GRASS;
		l.buildFlagMaps();
		for (Kind k : Kind.values()){
			assertEquals( k.name(), -1, VillageRoutine.flourishCell( l, pos, k ) );
		}
		l.map[pos + 1] = Terrain.WATER;
		l.map[pos + 12 - 1] = Terrain.FLOWER_PATCH;
		l.map[pos - 12] = Terrain.FURROWED_GRASS;
		l.buildFlagMaps();
		assertEquals( pos + 1, VillageRoutine.flourishCell( l, pos, Kind.SHORE ) );
		//a farmer stoops over a village's field alone: flowers, or furrowed grass off the surface, are none
		assertEquals( -1, VillageRoutine.flourishCell( l, pos, Kind.FIELD ) );
		assertEquals( pos + 12 - 1, VillageRoutine.flourishCell( l, pos, Kind.BRUSH ) );
	}

	// ---------------------------------------------------------------- on a real window

	//at night every family is abed in its own house, one to a cell, the doorway left clear
	@Test
	public void atNightEveryoneSleepsInTheirOwnHouse(){
		Settlement s = human();
		at( Season.SUMMER, Phase.NIGHT, 0.5f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		HashMap<Settler, Integer> cells = new HashMap<>();
		ArrayList<Settler> folk = peopleArrive( ow, s, cells );
		assertTrue( folk.size() >= 4 );
		HashSet<Integer> taken = new HashSet<>();
		for (Settler f : folk){
			int c = cells.get( f );
			assertNotEquals( "nobody without a bed", -1, c );
			assertTrue( taken.add( c ) );
			int wx = ow.worldX() + c % W, wy = ow.worldY() + c / W;
			assertTrue( "in their own house", Math.abs( wx - f.homeX ) <= 1 && Math.abs( wy - f.homeY ) <= 1 );
			int ix = VillageRoutine.inX( s, f.homeX, f.homeY ), iy = VillageRoutine.inY( s, f.homeX, f.homeY );
			assertFalse( "the doorway is clear", wx == f.homeX - ix && wy == f.homeY - iy );
			assertEquals( f.SLEEPING, f.state );
			assertEquals( Slot.BED.ordinal(), f.slot );
		}
	}

	//mid-morning in summer: the farmers at the edges of the village's fields outside the fence,
	//the fishers on a bank (or the fields), the crafters and elders at their doors, the loafers
	//about the well
	@Test
	public void byDayTheyAreAtTheirWork(){
		Settlement s = human();
		at( Season.SUMMER, Phase.DAY, 0.25f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		HashMap<Settler, Integer> cells = new HashMap<>();
		ArrayList<Settler> folk = peopleArrive( ow, s, cells );
		HashSet<Integer> taken = new HashSet<>();
		int placed = 0;
		for (Settler f : folk){
			int c = cells.get( f );
			if (c == -1) continue;
			placed++;
			assertTrue( taken.add( c ) );
			assertTrue( VillageRoutine.standable( ow, c ) );
			int wx = ow.worldX() + c % W, wy = ow.worldY() + c / W;
			Role r = Role.values()[f.role];
			Slot slot = Slot.values()[f.slot];
			assertNotEquals( f.SLEEPING, f.state );
			if (slot != Slot.WORK) continue;   //a break at the well
			switch (r){
				case FISHER:
					//the banks it can walk to from home
					if (VillageRoutine.shores( ow, s, VillageRoutine.homeArea( ow, s, f.homeX, f.homeY ) ).length > 0){
						assertTrue( "on the bank", VillageRoutine.waterBeside( ow, c ) );
						break;
					}
				case FARMER:
					//beside the village's own field, standing on its edge: never in the crop, never on the road
					assertTrue( "at the edge of the fields' band: " + ring( s, wx, wy ),
							ring( s, wx, wy ) >= s.radius + WorldStructures.FIELD_RING0 - 1
							&& ring( s, wx, wy ) <= s.radius + WorldStructures.FIELD_REACH + 1 );
					assertNotEquals( "beside no field", -1, VillageRoutine.besideField( ow, c ) );
					assertFalse( ow.fieldAt( c ) );
					assertNotEquals( Terrain.DIRT_PATH, ow.map[c] );
					break;
				case CRAFTER: case ELDER:
					assertTrue( "by the door", Math.max( Math.abs( wx - f.homeX ), Math.abs( wy - f.homeY ) ) <= 6 );
					break;
				case LOAFER:
					assertTrue( "about the well", ring( s, wx, wy ) >= 1 && ring( s, wx, wy ) <= 5 );
					break;
				default:
					break;
			}
		}
		assertTrue( "placed " + placed + " of " + folk.size(), placed >= folk.size() * 9 / 10 );
	}

	//at dusk the village gathers at the well
	@Test
	public void atDuskTheyGatherAtTheWell(){
		Settlement s = human();
		at( Season.SUMMER, Phase.DUSK, 0.45f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		HashMap<Settler, Integer> cells = new HashMap<>();
		ArrayList<Settler> folk = peopleArrive( ow, s, cells );
		int gathered = 0;
		HashSet<Integer> taken = new HashSet<>();
		for (Settler f : folk){
			int c = cells.get( f );
			if (c == -1 || f.slot != Slot.GATHER.ordinal()) continue;
			assertTrue( taken.add( c ) );
			int wx = ow.worldX() + c % W, wy = ow.worldY() + c / W;
			assertTrue( ring( s, wx, wy ) >= 1 && ring( s, wx, wy ) <= 5 );
			gathered++;
		}
		assertTrue( "a crowd: " + gathered + " of " + folk.size(), gathered >= folk.size() / 2 );
	}

	//the guards flank the well by day and keep the gates by night
	@Test
	public void guardsKeepTheWellByDayAndTheGatesByNight(){
		Settlement s = first( WorldStructures.Faction.HUMAN, 9 );
		for (Phase phase : new Phase[]{ Phase.DAY, Phase.NIGHT }){
			at( Season.SUMMER, phase, 0.5f );
			OverworldLevel ow = windowAt( s.cx, s.cy );
			HashSet<Integer> taken = new HashSet<>();
			for (int g = 0; g < 2; g++){
				OverworldGuard guard = OverworldGuard.random( s.key );
				guard.post = g;
				int c = VillageRoutine.arrivalCell( ow, guard, s.cx, s.cy );
				assertNotEquals( -1, c );
				guard.pos = c;
				ow.mobs.add( guard );
				assertTrue( taken.add( c ) );
				int wx = ow.worldX() + c % W, wy = ow.worldY() + c / W;
				if (phase == Phase.DAY || VillageRoutine.gates( ow, s ).length == 0){
					assertTrue( phase + ": by the well", ring( s, wx, wy ) <= 3 );
				} else {
					assertTrue( "at a gate: ring " + ring( s, wx, wy ), ring( s, wx, wy ) >= s.radius - 2 && ring( s, wx, wy ) <= s.radius + 1 );
				}
			}
		}
	}

	//whether a way exists is a lookup that answers exactly what a search would: between cells
	//on the walking ground or off it (a settler may stand on what grew or was dropped there
	//since), side by side or across the window
	@Test
	public void reachableIsWhatThePathFinderFinds(){
		for (WorldStructures.Faction faction : new WorldStructures.Faction[]{ WorldStructures.Faction.HUMAN, WorldStructures.Faction.GNOLL }){
			Settlement s = first( faction, 4 );
			at( Season.SUMMER, Phase.DAY, 0.3f );
			OverworldLevel ow = windowAt( s.cx, s.cy );
			boolean[] mask = VillageRoutine.walkMask( ow );
			java.util.Random rnd = new java.util.Random( 11 );
			int ways = 0, none = 0, offMask = 0;
			for (int i = 0; i < 600; i++){
				int a = (2 + rnd.nextInt( W - 4 )) + (2 + rnd.nextInt( H - 4 )) * W;
				int reach = i % 3 == 0 ? 2 : i % 3 == 1 ? 12 : W;
				int bx = Math.max( 2, Math.min( W - 3, a % W + rnd.nextInt( 2 * reach + 1 ) - reach ) );
				int by = Math.max( 2, Math.min( H - 3, a / W + rnd.nextInt( 2 * reach + 1 ) - reach ) );
				int b = bx + by * W;
				if (a == b) continue;
				boolean found = PathFinder.find( a, b, mask ) != null;
				assertEquals( "from " + a + " to " + b, found, VillageRoutine.reachable( ow, a, b ) );
				if (found) ways++;
				else none++;
				if (!mask[a] || !mask[b]) offMask++;
			}
			assertTrue( faction + ": " + ways + " ways, " + none + " none", ways > 50 && none > 20 );
			assertTrue( offMask > 50 );
		}
	}

	//every spot a member is set down on is one it can walk home from, where its house's
	//ground allows it: a work spot is looked for in its home's area first
	@Test
	public void arrivalsCanWalkHome(){
		int checked = 0;
		for (Settlement s : new Settlement[]{ human(), first( WorldStructures.Faction.GNOLL, 2 ), first( WorldStructures.Faction.HUMAN, 18 ) }){
			for (Phase phase : new Phase[]{ Phase.DAY, Phase.DUSK }){
				at( Season.SUMMER, phase, 0.3f );
				OverworldLevel ow = windowAt( s.cx, s.cy );
				HashMap<Settler, Integer> cells = new HashMap<>();
				for (Settler f : peopleArrive( ow, s, cells )){
					int c = cells.get( f );
					int area = VillageRoutine.homeArea( ow, s, f.homeX, f.homeY );
					if (c == -1 || area == 0) continue;
					int door = ow.localCell( VillageRoutine.doorX( s, f.homeX, f.homeY ), VillageRoutine.doorY( s, f.homeX, f.homeY ) );
					assertTrue( Role.values()[f.role] + " " + Slot.values()[f.slot] + " at ring " + ring( s, ow.worldX() + c % W, ow.worldY() + c / W )
							+ " cannot walk home", VillageRoutine.reachable( ow, c, door ) );
					checked++;
				}
			}
		}
		assertTrue( checked > 30 );
	}

	//the banks are dry cells by open water, nearest the well first
	@Test
	public void banksComeNearestFirst(){
		Settlement shore = null;
		for (int[] v : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 )){
			if (VillageRoutine.shoreNear( SEED, v[0], v[1] )){
				shore = VillageRoutine.settlement( SEED, WorldStructures.sectorOf( v[0], v[1] ) );
				break;
			}
		}
		assertNotNull( shore );
		at( Season.SUMMER, Phase.DAY, 0.25f );
		OverworldLevel ow = windowAt( shore.cx, shore.cy );
		long[] banks = VillageRoutine.shores( ow, shore );
		assertTrue( banks.length > 0 );
		int last = -1;
		for (long k : banks){
			int wx = (int)(k & 0xFFFFFFFFL), wy = (int)(k >> 32);
			int c = ow.localCell( wx, wy );
			assertTrue( VillageRoutine.standable( ow, c ) );
			assertTrue( VillageRoutine.waterBeside( ow, c ) );
			int r = ring( shore, wx, wy );
			assertTrue( r >= last );
			assertTrue( r <= shore.radius + 6 );
			last = r;
		}
	}
}
