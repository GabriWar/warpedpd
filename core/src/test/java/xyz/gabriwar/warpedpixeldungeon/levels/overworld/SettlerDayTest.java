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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Role;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Settlement;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Slot;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Weather;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The settlements' day as the live game runs it: Settler.act turn by turn on a real
 * generated window (near the hero they walk, far off they step straight to their spot, and
 * every turn takes time), the level's own entry points (unparkMobs, resettleFolk on a load,
 * populateSettlement on a spawn), the sky and the path budget as the turns read them, and
 * a save through the real Bundle text format.
 */
public class SettlerDayTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private static final Method ACT;
	static {
		try {
			ACT = Settler.class.getDeclaredMethod( "act" );
			ACT.setAccessible( true );
		} catch (NoSuchMethodException e){
			throw new AssertionError( e );
		}
	}

	private int turn, startDay, challenges;
	private float duration, precip, wind;
	private PrecipType precipType;
	private Phase override;
	private Level level;
	private Hero hero;

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
		wind = ClimateManager.debugWindOverride;
		precipType = ClimateManager.debugPrecipTypeOverride;
		override = DayNightCycle.debugPhaseOverride;
		level = Dungeon.level;
		hero = Dungeon.hero;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = null;
		Actor.clear();
	}

	@After
	public void restore(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		Statistics.duration = duration;
		ClimateManager.debugPrecipOverride = precip;
		ClimateManager.debugWindOverride = wind;
		ClimateManager.debugPrecipTypeOverride = precipType;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = level;
		Dungeon.hero = hero;
	}

	// ---------------------------------------------------------------- helpers

	private static Settlement first( WorldStructures.Faction faction, int minHouses, int sectors ){
		ArrayList<int[]> near = WorldStructures.settlementsNear( SEED, 0, 0, faction, minHouses, sectors );
		assertFalse( "no " + faction + " settlement of " + minHouses + "+ houses", near.isEmpty() );
		int[] v = near.get( 0 );
		Settlement s = VillageRoutine.settlement( SEED, WorldStructures.sectorOf( v[0], v[1] ) );
		assertNotNull( s );
		return s;
	}

	private static Settlement human(){
		return first( WorldStructures.Faction.HUMAN, 6, 12 );
	}

	private static int ring( Settlement s, int wx, int wy ){
		return Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) );
	}

	private static int wx( OverworldLevel ow, int cell ){ return ow.worldX() + cell % W; }
	private static int wy( OverworldLevel ow, int cell ){ return ow.worldY() + cell / W; }

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

	//one game turn
	private static void tick(){
		Dungeon.cycleTurn++;
		Statistics.duration += 1;
	}

	private static OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
		OverworldLevel l = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( l );
		//an ordinary stretch of days: any raid on the window's villages about now was beaten off,
		//so nobody keeps in mending after one (RaidEvent.recovering) - the raids have tests of their own
		int day = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		for (int sy = Math.floorDiv( oy, WorldStructures.SECTOR ) - 1; sy <= Math.floorDiv( oy + H, WorldStructures.SECTOR ) + 1; sy++){
			for (int sx = Math.floorDiv( ox, WorldStructures.SECTOR ) - 1; sx <= Math.floorDiv( ox + W, WorldStructures.SECTOR ) + 1; sx++){
				for (int d = day - 3; d <= day + 3; d++){
					l.resolveEvent( WorldEvents.idOf( WorldEvents.Type.RAID, sx, sy, d ), Integer.MAX_VALUE );
				}
			}
		}
		return l;
	}

	//one of the family of house h, as populateSettlement makes it
	private static Settler member( Settlement s, int h, int m ){
		long fh = VillageRoutine.familyHash( s.sx, s.sy, h );
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
		return f;
	}

	//every family of the settlement, each set down where its arrival says
	private static ArrayList<Settler> village( OverworldLevel ow, Settlement s ){
		ArrayList<Settler> out = new ArrayList<>();
		for (int h = s.firstFamily(); h < s.lastFamily(); h++){
			int members = 1 + (int) Math.floorMod( VillageRoutine.familyHash( s.sx, s.sy, h ), 3 );
			for (int m = 0; m < members; m++){
				Settler f = member( s, h, m );
				int at = VillageRoutine.arrivalCell( ow, f, f.homeX, f.homeY );
				if (at == -1) continue;
				f.pos = at;
				ow.mobs.add( f );
				out.add( f );
			}
		}
		return out;
	}

	//a sprite that draws nothing: the act loop only places and turns it
	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
		};
		sp.visible = false;
		return sp;
	}

	//the level live, its folk on the scheduler with sprites, the hero on a cell near (wx, wy)
	private static void liven( OverworldLevel ow, ArrayList<Settler> folk, int wx, int wy ){
		Dungeon.level = ow;
		java.util.Arrays.fill( ow.heroFOV, false );   //no sounds, no particles: nobody watching
		Hero h = new Hero();
		h.pos = -1;
		for (int r = 0; r <= 4 && h.pos == -1; r++){
			for (int dy = -r; dy <= r && h.pos == -1; dy++){
				for (int dx = -r; dx <= r && h.pos == -1; dx++){
					int c = ow.localCell( wx + dx, wy + dy );
					if (c != -1 && ow.passable[c] && !ow.occupied( c )) h.pos = c;
				}
			}
		}
		assertNotEquals( -1, h.pos );
		Dungeon.hero = h;
		Actor.add( h );
		for (Settler f : folk){
			f.sprite = stub();
			Actor.add( f );
		}
	}

	private static boolean act( Settler f ){
		try {
			return (Boolean) ACT.invoke( f );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static boolean inOwnHouse( OverworldLevel ow, Settler f ){
		return Math.abs( wx( ow, f.pos ) - f.homeX ) <= 1 && Math.abs( wy( ow, f.pos ) - f.homeY ) <= 1;
	}

	//is it where its trade has it at work? (the band a spot keeps to, the bank, the door)
	private static boolean atWork( OverworldLevel ow, Settlement s, Settler f ){
		int x = wx( ow, f.pos ), y = wy( ow, f.pos ), r = ring( s, x, y );
		switch (Role.values()[f.role]){
			case FISHER:
				//a bank it can walk to from home, if there is one
				if (VillageRoutine.shores( ow, s, VillageRoutine.homeArea( ow, s, f.homeX, f.homeY ) ).length > 0){
					return VillageRoutine.waterBeside( ow, f.pos );
				}
			case FARMER:
				//at the edge of one of the village's fields, outside the fence; with none, about the yard
				if (!s.farms()) return Math.max( Math.abs( x - f.homeX ), Math.abs( y - f.homeY ) ) <= 6;
				return r >= s.radius + WorldStructures.FIELD_RING0 - 1 && r <= s.radius + WorldStructures.FIELD_REACH + 1
						&& VillageRoutine.besideField( ow, f.pos ) != -1;
			case GATHERER:
				return r >= s.radius - 2 && r <= s.radius + 1;
			case HUNTER:
				return r >= s.radius && r <= s.radius + 1;
			case CRAFTER: case ELDER:
				return Math.max( Math.abs( x - f.homeX ), Math.abs( y - f.homeY ) ) <= 6;
			default:   //LOAFER
				return r >= 1 && r <= 5;
		}
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Long, Mob> parked( OverworldLevel ow ) throws Exception {
		Field f = OverworldLevel.class.getDeclaredField( "parkedMobs" );
		f.setAccessible( true );
		return (HashMap<Long, Mob>) f.get( ow );
	}

	private static void call( OverworldLevel ow, String name ) throws Exception {
		Method m = OverworldLevel.class.getDeclaredMethod( name );
		m.setAccessible( true );
		m.invoke( ow );
	}

	// ---------------------------------------------------------------- the act loop

	//far from any hero, a new part of the day puts each one straight on its spot: one turn
	//(a couple when a neighbour still stands on it), never a walk
	@Test
	public void farFolkStepStraightToTheirNewSpot(){
		Settlement s = human();
		at( Season.SUMMER, Phase.NIGHT, 0.95f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		ArrayList<Settler> folk = village( ow, s );
		assertTrue( folk.size() >= 4 );
		//the hero in the far corner of the window
		liven( ow, folk, ow.worldX() + 4, ow.worldY() + 4 );
		assertTrue( OverworldLevel.heroDistance( ow, ow.localCell( s.cx, s.cy ) ) > VillageRoutine.NEAR + s.radius + 8 );

		at( Season.SUMMER, Phase.DAY, 0.3f );
		for (int round = 0; round < 3; round++){
			for (Settler f : folk){
				float before = f.cooldown();
				assertTrue( act( f ) );
				assertTrue( "every turn takes time", f.cooldown() > before );
			}
			tick();
		}
		int working = 0, there = 0;
		StringBuilder off = new StringBuilder();
		for (Settler f : folk){
			assertEquals( "up and about", Slot.WORK.ordinal(), f.slot );
			assertNotEquals( f.SLEEPING, f.state );
			working++;
			if (atWork( ow, s, f )) there++;
			else off.append( Role.values()[f.role] ).append( '@' ).append( ring( s, wx( ow, f.pos ), wy( ow, f.pos ) ) ).append( ' ' );
		}
		assertTrue( "at work: " + there + " of " + working + " (off: " + off + ")", there >= working * 9 / 10 );
	}

	//a whole day with the hero by the well: they walk (one step a turn), never share a cell
	//or tread the grass flat, every turn takes time - at mid-morning they are at work, at
	//dusk in a crowd at the well, and by midnight everyone is asleep in their own house
	@Test
	public void aWholeDayNearTheHero(){
		wholeDay( human(), Season.SUMMER );
	}

	@Test
	public void aWholeDayInAGnollCamp(){
		wholeDay( first( WorldStructures.Faction.GNOLL, 4, 12 ), Season.SUMMER );
	}

	//a bigger place, in winter: the farmers sweep their doorsteps
	@Test
	public void aWholeWinterDayInATown(){
		wholeDay( first( WorldStructures.Faction.HUMAN, 18, 16 ), Season.WINTER );
	}

	//one turn of every settler, checking what holds on every turn
	private static void turn( OverworldLevel ow, ArrayList<Settler> folk, String when ){
		tick();
		for (Settler f : folk){
			int was = f.pos;
			float before = f.cooldown();
			assertTrue( act( f ) );
			assertTrue( "every turn takes time", f.cooldown() > before );
			assertTrue( "one step a turn at most (" + when + ")", ow.distance( was, f.pos ) <= 1 );
			assertFalse( "never treads the crop or the tall grass flat: " + ow.map[f.pos], VillageRoutine.trample( ow.map[f.pos] ) );
		}
		HashSet<Integer> cells = new HashSet<>();
		cells.add( Dungeon.hero.pos );
		for (Settler f : folk) assertTrue( "one to a cell (" + when + ")", cells.add( f.pos ) );
	}

	private static void wholeDay( Settlement s, Season season ){
		at( season, Phase.NIGHT, 0.95f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		ArrayList<Settler> folk = village( ow, s );
		assertTrue( folk.size() >= 2 );
		liven( ow, folk, s.cx, s.cy + 1 );
		assertTrue( OverworldLevel.heroDistance( ow, ow.localCell( s.cx, s.cy ) ) <= 2 );

		boolean checkedWork = false, checkedDusk = false;
		int turns = 0, beatTurns = 0, onFence = 0;
		long t0 = System.nanoTime();
		while (true){
			turn( ow, folk, "turn " + turns );
			turns++;
			Phase p = DayNightCycle.phase();
			float t = DayNightCycle.phaseProgress();

			//a hunter's round moves on a few cells every HUNT_LEG turns and he walks the leg along
			//the fence, so he is on it most of his day (some 80%; crossing the camp, as all of
			//them once did on the same turn, kept them there 71%)
			if (p == Phase.DAY){
				for (Settler f : folk){
					if (f.role != Role.HUNTER.ordinal() || f.slot != Slot.WORK.ordinal()) continue;
					beatTurns++;
					if (atWork( ow, s, f )) onFence++;
				}
			}

			if (!checkedWork && p == Phase.DAY && t >= 0.3f){
				checkedWork = true;
				int working = 0, there = 0;
				StringBuilder off = new StringBuilder();
				for (Settler f : folk){
					if (f.slot != Slot.WORK.ordinal() || f.role == Role.HUNTER.ordinal()) continue;
					working++;
					boolean ok = season == Season.WINTER && f.role == Role.FARMER.ordinal()
							? Math.max( Math.abs( wx( ow, f.pos ) - f.homeX ), Math.abs( wy( ow, f.pos ) - f.homeY ) ) <= 6
							: atWork( ow, s, f );
					if (ok) there++;
					else off.append( Role.values()[f.role] ).append( '@' ).append( ring( s, wx( ow, f.pos ), wy( ow, f.pos ) ) ).append( ' ' );
				}
				assertTrue( "mid-morning at work: " + there + " of " + working + " (off: " + off + ")",
						there >= working * 8 / 10 );
			}
			if (!checkedDusk && p == Phase.DUSK && t >= 0.6f){
				checkedDusk = true;
				int gathering = 0, there = 0;
				for (Settler f : folk){
					if (f.slot != Slot.GATHER.ordinal()) continue;
					gathering++;
					if (ring( s, wx( ow, f.pos ), wy( ow, f.pos ) ) <= 5) there++;
				}
				assertTrue( "a crowd at the well: " + there + " of " + gathering, there >= gathering * 8 / 10 );
			}
			if (checkedDusk && p == Phase.NIGHT && t >= 0.6f) break;
			assertTrue( "the day ends", turns < 2 * DayNightCycle.FULL_CYCLE );
		}
		long ms = (System.nanoTime() - t0) / 1_000_000;
		System.out.println( "SettlerDayTest: " + (s.gnoll ? "gnoll " : "human ") + s.houses() + " houses (radius " + s.radius + "), "
				+ folk.size() + " folk, " + season + ", " + turns + " turns in " + ms + " ms"
				+ (beatTurns > 0 ? ", hunters on the fence " + (100 * onFence / beatTurns) + "% of their day" : "") );
		if (beatTurns > 0) assertTrue( "hunters on the fence " + onFence + " of " + beatTurns, onFence * 10 >= beatTurns * 7 );

		StringBuilder out = new StringBuilder();
		for (Settler f : folk){
			assertEquals( Slot.BED.ordinal(), f.slot );
			if (!inOwnHouse( ow, f ) || f.state != f.SLEEPING){
				out.append( Role.values()[f.role] ).append( " house " ).append( f.house ).append( " at ring " )
						.append( ring( s, wx( ow, f.pos ), wy( ow, f.pos ) ) ).append( f.state == f.SLEEPING ? " asleep" : " awake" ).append( "; " );
			}
		}
		assertEquals( "everyone abed in their own house by midnight: " + out, 0, out.length() );
	}

	//a shower that holds sends the workers indoors a few at a time, and the fair weather after
	//it sends them back out
	@Test
	public void rainSendsThemInAndFairWeatherOut(){
		Settlement s = human();
		at( Season.SUMMER, Phase.DAY, 0.05f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		ArrayList<Settler> folk = village( ow, s );
		liven( ow, folk, s.cx, s.cy + 1 );
		for (int i = 0; i < 150; i++) turn( ow, folk, "fair" );

		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.6f;
		int firstIn = -1;
		for (int i = 0; i < 200; i++){
			turn( ow, folk, "rain " + i );
			int in = 0;
			for (Settler f : folk) if (f.slot == Slot.INDOORS.ordinal()) in++;
			if (in > 0 && firstIn == -1) firstIn = i;
		}
		assertTrue( "nobody went in before the rain held: " + firstIn, firstIn >= VillageRoutine.WEATHER_HOLD - 1 );
		StringBuilder out = new StringBuilder();
		for (Settler f : folk){
			assertEquals( Slot.INDOORS.ordinal(), f.slot );
			if (!inOwnHouse( ow, f )) out.append( Role.values()[f.role] ).append( "; " );
		}
		assertEquals( "all of them indoors in their own house: " + out, 0, out.length() );

		ClimateManager.debugPrecipOverride = 0f;
		for (int i = 0; i < 200; i++) turn( ow, folk, "after " + i );
		int working = 0, there = 0;
		for (Settler f : folk){
			if (f.slot != Slot.WORK.ordinal()) continue;
			working++;
			if (atWork( ow, s, f )) there++;
		}
		assertTrue( "back at work: " + there + " of " + working, working > 0 && there >= working * 8 / 10 );
	}

	//the window slides by a rebase while the folk are on their way: their walks and spots
	//slide with it (the same world cells), nobody jumps, and they still get there
	@Test
	public void aRebaseMidWalkKeepsThemOnTheirWay() throws Exception {
		Settlement s = human();
		at( Season.SUMMER, Phase.NIGHT, 0.95f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		ArrayList<Settler> folk = village( ow, s );
		liven( ow, folk, s.cx, s.cy + 1 );
		//into the dawn, until most of them are on the move
		while (DayNightCycle.phase() != Phase.DAWN || DayNightCycle.phaseProgress() < 0.45f) turn( ow, folk, "dawn" );

		//the rebase: the same world, its window 32 cells further east
		int dx = 32;
		OverworldLevel next = windowAt( s.cx + dx, s.cy );
		assertEquals( ow.worldX() + dx, next.worldX() );
		Field ver = OverworldLevel.class.getDeclaredField( "windowVersion" );
		ver.setAccessible( true );
		ver.setInt( next, ow.windowVersion() + 1 );
		HashMap<Settler, Long> world = new HashMap<>();
		for (Settler f : folk){
			world.put( f, OverworldLevel.worldKey( wx( ow, f.pos ), wy( ow, f.pos ) ) );
			f.pos -= dx;
			next.mobs.add( f );
		}
		java.util.Arrays.fill( next.heroFOV, false );
		Dungeon.hero.pos -= dx;
		Dungeon.level = next;
		for (Settler f : folk){
			assertEquals( (long) world.get( f ), OverworldLevel.worldKey( wx( next, f.pos ), wy( next, f.pos ) ) );
		}
		for (int i = 0; i < 600; i++) turn( next, folk, "after the slide " + i );

		int working = 0, there = 0;
		StringBuilder off = new StringBuilder();
		for (Settler f : folk){
			if (f.slot != Slot.WORK.ordinal()) continue;
			working++;
			if (atWork( next, s, f )) there++;
			else off.append( Role.values()[f.role] ).append( '@' ).append( ring( s, wx( next, f.pos ), wy( next, f.pos ) ) ).append( ' ' );
		}
		assertTrue( "at work after the slide: " + there + " of " + working + " (off: " + off + ")",
				working > 0 && there >= working * 8 / 10 );
	}

	//a river through the village with a single plank crossing, and two of the folk meeting on
	//it head on at bedtime: they step past each other and both get to bed, rather than
	//standing there face to face all night
	@Test
	public void twoMeetingOnABridgeStepPastEachOther(){
		Settlement s = human();
		at( Season.SUMMER, Phase.NIGHT, 0.5f );
		//three rows of water right across the window (whatever they flood), one bridge over them
		//where no house stands, and a family whose house is whole on either bank, each with a
		//way from the bridge to its door
		OverworldLevel ow = null;
		ArrayList<Settler> folk = null;
		Settler up = null, down = null;
		int y = 0, bx = 0;
		search:
		for (int dy = 4; dy <= s.radius; dy++){
			for (int ox = 0; ox <= s.radius; ox++){
				y = s.cy + dy;
				bx = s.cx + ox;
				boolean clear = true;
				for (int h = 0; h < s.houses(); h++){
					if (Math.abs( s.houseX( h ) - bx ) <= 3 && Math.abs( s.houseY( h ) - y ) <= 5) clear = false;
				}
				if (!clear) continue;
				ow = windowAt( s.cx, s.cy );
				for (int x = ow.worldX() + 1; x < ow.worldX() + W - 1; x++){   //the window's solid rim stays
					for (int ry = y - 1; ry <= y + 1; ry++) ow.map[ow.localCell( x, ry )] = x == bx ? Terrain.BRIDGE : Terrain.WATER;
				}
				//firm ground at either end of the planks
				for (int dx = -1; dx <= 1; dx++){
					ow.map[ow.localCell( bx + dx, y - 2 )] = Terrain.EMPTY;
					ow.map[ow.localCell( bx + dx, y + 2 )] = Terrain.EMPTY;
				}
				ow.buildFlagMaps();
				folk = village( ow, s );
				up = down = null;
				int mid = ow.localCell( bx, y ), northEnd = ow.localCell( bx, y - 1 );
				for (Settler f : folk){
					if (Math.abs( f.homeY - y ) <= 3) continue;   //a flooded house
					int door = ow.localCell( VillageRoutine.doorX( s, f.homeX, f.homeY ), VillageRoutine.doorY( s, f.homeX, f.homeY ) );
					if (f.homeY < y && up == null && VillageRoutine.reachable( ow, mid, door )) up = f;
					if (f.homeY > y && down == null && VillageRoutine.reachable( ow, northEnd, door )) down = f;
				}
				if (up != null && down != null) break search;
			}
		}
		assertNotNull( "a family on the north bank", up );
		assertNotNull( "a family on the south bank", down );

		//one of the north bank's folk on the middle of the bridge, one of the south's on its
		//north end: the only way on for each is through the other
		up.pos = ow.localCell( bx, y );
		down.pos = ow.localCell( bx, y - 1 );
		liven( ow, folk, s.cx, s.cy + 1 );

		int passed = -1;
		for (int i = 0; i < 300; i++){
			turn( ow, folk, "on the bridge " + i );
			if (passed == -1 && wy( ow, up.pos ) < y - 1 && wy( ow, down.pos ) > y + 1) passed = i;
		}
		assertTrue( "past each other within a few turns: " + passed, passed != -1 && passed <= 10 );
		for (Settler f : new Settler[]{ up, down }){
			assertTrue( "home over the river", inOwnHouse( ow, f ) );
			assertEquals( f.SLEEPING, f.state );
		}
	}

	//one left in a doorway beside its spot (stepped past there by another) does not idle in
	//it, though the doorway is within its wander of the spot: it goes on to the spot itself
	@Test
	public void nobodyIdlesInADoorway(){
		Settlement s = human();
		at( Season.SUMMER, Phase.DAY, 0.3f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		ArrayList<Settler> folk = village( ow, s );
		Settler f = null;
		int door = -1;
		for (Settler g : folk){
			if (f != null || g.slot != Slot.WORK.ordinal() || g.role == Role.ELDER.ordinal()) continue;   //an elder's bench allows no wander
			for (int n : PathFinder.NEIGHBOURS8){
				if (ow.map[g.pos + n] == Terrain.DOOR && !ow.occupied( g.pos + n )){
					f = g;
					door = g.pos + n;
					break;
				}
			}
		}
		assertNotNull( "one at work beside a door", f );
		int spot = f.pos;
		f.pos = door;
		liven( ow, folk, s.cx, s.cy + 1 );
		tick();
		assertTrue( act( f ) );
		assertEquals( "straight back to its spot", spot, f.pos );
	}

	//one set down in a pocket of ground it cannot walk out of (here: walled in) searches for no
	//way that is not there: while a hero sees it, it stays; once nobody does, it is simply at
	//its spot
	@Test
	public void cutOffItWaitsTillUnseenThenIsThere(){
		Settlement s = human();
		at( Season.SUMMER, Phase.DAY, 0.3f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		//a cell a few steps from the well, walled in on every side
		int pocket = -1;
		for (int k = 0; k < 8 * 6 && pocket == -1; k++){
			int c = ow.localCell( s.cx + VillageRoutine.ringX( 6, k ), s.cy + VillageRoutine.ringY( 6, k ) );
			if (VillageRoutine.standable( ow, c )) pocket = c;
		}
		assertNotEquals( -1, pocket );
		for (int n : PathFinder.NEIGHBOURS8) ow.map[pocket + n] = Terrain.WALL;
		ow.buildFlagMaps();
		ArrayList<Settler> folk = village( ow, s );
		Settler f = null;
		for (Settler g : folk) if (f == null && g.slot == Slot.WORK.ordinal() && g.pos != pocket) f = g;
		assertNotNull( f );
		f.pos = pocket;
		liven( ow, folk, s.cx, s.cy + 1 );
		assertFalse( VillageRoutine.reachable( ow, pocket, ow.localCell( s.cx + s.radius, s.cy ) ) );

		ow.heroFOV[pocket] = true;
		for (int i = 0; i < 30; i++){
			tick();
			float before = f.cooldown();
			assertTrue( act( f ) );
			assertTrue( "every turn takes time", f.cooldown() > before );
			assertEquals( "it stays where it is seen", pocket, f.pos );
			//and spent none of the turn's searches on it
			for (int k = 0; k < VillageRoutine.PATHS_PER_TURN; k++) assertTrue( VillageRoutine.takePath() );
		}
		ow.heroFOV[pocket] = false;
		tick();
		assertTrue( act( f ) );
		assertNotEquals( "nobody sees it go", pocket, f.pos );
		assertTrue( "at its work: " + Role.values()[f.role] + " at ring " + ring( s, wx( ow, f.pos ), wy( ow, f.pos ) ), atWork( ow, s, f ) );
	}

	// ---------------------------------------------------------------- the level's entry points

	//a parked family comes back into the window straight to where the day has got to, and a
	//parked guard to his post
	@Test
	public void unparkedFolkComeBackToTheirSpot() throws Exception {
		Settlement s = first( WorldStructures.Faction.HUMAN, 9, 12 );
		at( Season.SUMMER, Phase.NIGHT, 0.6f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		HashMap<Long, Mob> store = parked( ow );
		ArrayList<Settler> folk = new ArrayList<>();
		int h = s.firstFamily();
		int members = 1 + (int) Math.floorMod( VillageRoutine.familyHash( s.sx, s.sy, h ), 3 );
		for (int m = 0; m < members; m++){
			Settler f = member( s, h, m );
			f.slot = Slot.WORK.ordinal();   //parked at its work in the afternoon
			f.state = f.PASSIVE;
			folk.add( f );
			store.put( OverworldLevel.worldKey( s.cx + s.radius, s.cy + m ), f );
		}
		OverworldGuard g = OverworldGuard.random( s.key );
		g.post = 1;
		store.put( OverworldLevel.worldKey( s.cx + s.radius - 1, s.cy - 3 ), g );

		call( ow, "unparkMobs" );

		for (Settler f : folk){
			assertTrue( "back in the window", ow.mobs.contains( f ) );
			assertTrue( "in their own house", inOwnHouse( ow, f ) );
			assertEquals( Slot.BED.ordinal(), f.slot );
			assertEquals( f.SLEEPING, f.state );
		}
		assertTrue( ow.mobs.contains( g ) );
		int gx = wx( ow, g.pos ), gy = wy( ow, g.pos );
		if (VillageRoutine.gates( ow, s ).length > 0){
			assertTrue( "at a gate by night: ring " + ring( s, gx, gy ), ring( s, gx, gy ) >= s.radius - 2 && ring( s, gx, gy ) <= s.radius + 1 );
		} else {
			assertTrue( ring( s, gx, gy ) <= 3 );
		}
		assertTrue( store.isEmpty() );
	}

	//a save from before the routine: on load the folk take a home, a place in the family and
	//a trade from where they stand, and go where the day has got to; one saved under the
	//routine, whose part of the day has not moved on, is left to walk on
	@Test
	public void aLoadSettlesOldAndNewFolk() throws Exception {
		Settlement s = human();
		at( Season.SUMMER, Phase.NIGHT, 0.6f );
		OverworldLevel ow = windowAt( s.cx, s.cy );

		//an old save's villager standing by the well
		Villager fresh = new Villager();
		fresh.homeSector = s.key;
		Bundle b = new Bundle();
		fresh.storeInBundle( b );
		for (String k : new String[]{ "routine_house", "routine_member", "routine_role", "routine_slot",
				"routine_home_x", "routine_home_y" }) assertTrue( b.remove( k ) );
		Villager old = new Villager();
		old.restoreFromBundle( b );
		int standX = s.cx + 2, standY = s.cy + 3;
		old.pos = ow.localCell( standX, standY );
		ow.mobs.add( old );

		//one saved under the routine, abed and still in the night it went to bed in
		Settler abed = member( s, s.firstFamily(), 0 );
		abed.slot = Slot.BED.ordinal();
		int abedAt = ow.localCell( s.cx - 2, s.cy - 3 );   //wherever it was on its way home
		abed.pos = abedAt;
		ow.mobs.add( abed );

		call( ow, "resettleFolk" );

		int house = VillageRoutine.nearestHouse( s, standX, standY, ow );
		assertEquals( house, old.house );
		assertEquals( s.houseX( house ), old.homeX );
		assertEquals( s.houseY( house ), old.homeY );
		assertTrue( old.member >= 0 && old.member < 3 );
		assertEquals( VillageRoutine.roleOf( VillageRoutine.familyHash( s.sx, s.sy, house ), old.member, false ).ordinal(), old.role );
		assertEquals( Slot.BED.ordinal(), old.slot );
		assertTrue( "in the house it took", inOwnHouse( ow, old ) );
		assertEquals( old.SLEEPING, old.state );

		assertEquals( "on its way, left to walk on", abedAt, abed.pos );
	}

	//the real spawn: populateSettlement gives each family its house, place and trade, and
	//sets them down abed at night
	@Test
	public void theSpawnPutsEveryFamilyToBed() throws Exception {
		Settlement s = human();
		at( Season.SUMMER, Phase.NIGHT, 0.6f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		Method pop = OverworldLevel.class.getDeclaredMethod( "populateSettlement", int.class, int.class, long.class, int.class );
		pop.setAccessible( true );
		pop.invoke( ow, s.sx, s.sy, s.key, ow.localCell( s.cx, s.cy ) );

		int folk = 0, keepers = 0;
		HashSet<Integer> cells = new HashSet<>();
		for (Mob m : ow.mobs){
			assertTrue( "one to a cell", cells.add( m.pos ) );
			if (m instanceof OverworldShopkeeper) keepers++;
			if (!(m instanceof Settler)) continue;
			Settler f = (Settler) m;
			folk++;
			assertTrue( f.house >= 0 && f.member >= 0 );
			assertEquals( s.houseX( f.house ), f.homeX );
			assertEquals( s.houseY( f.house ), f.homeY );
			assertEquals( VillageRoutine.roleOf( VillageRoutine.familyHash( s.sx, s.sy, f.house ), f.member, false ).ordinal(), f.role );
			assertTrue( "abed in their own house", inOwnHouse( ow, f ) );
			assertEquals( f.SLEEPING, f.state );
		}
		assertTrue( folk >= 4 );
		assertEquals( VillageRoutine.vendorHouses( s.houses() ), keepers );
	}

	//nobody is moved when the day has nowhere for them: a settlement whose spots all lie off
	//the window, or a villager of no settlement, is left where it stood
	@Test
	public void noSpotLeavesThemWhereTheyStood(){
		Settlement s = human();
		at( Season.SUMMER, Phase.DAY, 0.3f );
		OverworldLevel ow = windowAt( s.cx + 400, s.cy + 400 );
		Settler f = member( s, s.firstFamily(), 0 );
		f.pos = 90 + 90 * W;
		assertEquals( -1, VillageRoutine.arrivalCell( ow, f, s.cx, s.cy ) );
		assertEquals( 90 + 90 * W, f.pos );

		Villager stray = new Villager();
		stray.pos = 91 + 90 * W;
		assertEquals( -1, VillageRoutine.arrivalCell( ow, stray, ow.worldX() + 91, ow.worldY() + 90 ) );
		assertEquals( 91 + 90 * W, stray.pos );
	}

	//a gnoll clan: abed in its own tents by night, by day the hunters on the fence and the
	//gatherers in the brush
	@Test
	public void aGnollClanByNightAndDay(){
		Settlement s = first( WorldStructures.Faction.GNOLL, 2, 12 );
		at( Season.SUMMER, Phase.NIGHT, 0.6f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		ArrayList<Settler> night = village( ow, s );
		assertTrue( night.size() >= 2 );
		HashSet<Integer> cells = new HashSet<>();
		for (Settler f : night){
			assertTrue( cells.add( f.pos ) );
			assertTrue( "in their own house", inOwnHouse( ow, f ) );
			assertEquals( f.SLEEPING, f.state );
		}

		at( Season.SUMMER, Phase.DAY, 0.3f );
		ow = windowAt( s.cx, s.cy );
		ArrayList<Settler> day = village( ow, s );
		int working = 0, there = 0;
		for (Settler f : day){
			if (f.slot != Slot.WORK.ordinal()) continue;
			working++;
			if (atWork( ow, s, f )) there++;
		}
		assertTrue( "at work: " + there + " of " + working, working > 0 && there >= working * 9 / 10 );
	}

	//the biggest place around, at night: every one of its families finds a bed in its own
	//house, one to a cell
	@Test
	public void aMetropolisSleepsInItsOwnHouses(){
		Settlement s = first( WorldStructures.Faction.HUMAN, 45, 24 );
		at( Season.SUMMER, Phase.NIGHT, 0.6f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		int expected = 0;
		for (int h = s.firstFamily(); h < s.lastFamily(); h++){
			expected += 1 + (int) Math.floorMod( VillageRoutine.familyHash( s.sx, s.sy, h ), 3 );
		}
		long t0 = System.nanoTime();
		ArrayList<Settler> folk = village( ow, s );
		long ms = (System.nanoTime() - t0) / 1_000_000;
		System.out.println( "SettlerDayTest: " + folk.size() + " metropolis folk arrived in " + ms + " ms" );
		assertEquals( "nobody without a bed", expected, folk.size() );
		HashSet<Integer> cells = new HashSet<>();
		for (Settler f : folk){
			assertTrue( cells.add( f.pos ) );
			assertTrue( inOwnHouse( ow, f ) );
		}
	}

	//a guard never walks his watch: with a hero near he keeps the post he has, and he takes
	//the hour's post (the gate by night, his torch lit; the well by day) once nobody is near
	@Test
	public void guardsChangePostOnlyWhereNobodySees() throws Exception {
		Settlement s = null;
		for (int[] v : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 9, 12 )){
			Settlement c = VillageRoutine.settlement( SEED, WorldStructures.sectorOf( v[0], v[1] ) );
			if (VillageRoutine.gates( windowAt( c.cx, c.cy ), c ).length > 0){
				s = c;
				break;
			}
		}
		assertNotNull( "a walled town with a road in", s );
		at( Season.SUMMER, Phase.DAY, 0.5f );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		OverworldGuard g = OverworldGuard.random( s.key );
		g.post = 0;
		g.pos = VillageRoutine.arrivalCell( ow, g, s.cx, s.cy );
		assertNotEquals( -1, g.pos );
		ow.mobs.add( g );
		liven( ow, new ArrayList<>(), s.cx, s.cy + 1 );
		g.sprite = stub();
		Actor.add( g );
		Method watch = OverworldGuard.class.getDeclaredMethod( "keepWatch", OverworldLevel.class );
		watch.setAccessible( true );
		Field lit = OverworldGuard.class.getDeclaredField( "lit" );
		lit.setAccessible( true );

		int wellPost = g.pos;
		assertTrue( ring( s, wx( ow, wellPost ), wy( ow, wellPost ) ) <= 3 );

		//nightfall with the hero at the well: he stays, no torch
		at( Season.SUMMER, Phase.NIGHT, 0.3f );
		watch.invoke( g, ow );
		assertEquals( wellPost, g.pos );
		assertFalse( (Boolean) lit.get( g ) );

		//the hero walks off: the gate, with a torch
		Dungeon.hero.pos = 4 + 4 * W;
		watch.invoke( g, ow );
		int r = ring( s, wx( ow, g.pos ), wy( ow, g.pos ) );
		assertTrue( "at a gate: ring " + r, r >= s.radius - 2 && r <= s.radius + 1 );
		assertTrue( (Boolean) lit.get( g ) );

		//day again, still nobody near: back by the well, the torch out
		at( Season.SUMMER, Phase.DAY, 0.3f );
		watch.invoke( g, ow );
		assertTrue( ring( s, wx( ow, g.pos ), wy( ow, g.pos ) ) <= 3 );
		assertFalse( (Boolean) lit.get( g ) );
	}

	//what they say fits what they do: a sleeper only grumbles, a fisher by the water talks of
	//fish, one with no water to fish talks of the fields, a loafer by day keeps the old lines
	@Test
	public void talkFitsTheHourAndTheTrade(){
		Settlement wet = null, dry = null;
		for (int[] v : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 12 )){
			Settlement c = VillageRoutine.settlement( SEED, WorldStructures.sectorOf( v[0], v[1] ) );
			//the banks the first family can walk to (its member below is the one spoken to)
			OverworldLevel ow = windowAt( c.cx, c.cy );
			int h = c.firstFamily();
			int banks = VillageRoutine.shores( ow, c, VillageRoutine.homeArea( ow, c, c.houseX( h ), c.houseY( h ) ) ).length;
			if (banks > 0 && wet == null) wet = c;
			if (banks == 0 && dry == null) dry = c;
			if (wet != null && dry != null) break;
		}
		assertNotNull( wet );
		assertNotNull( dry );
		final ArrayList<String> said = new ArrayList<>();
		com.watabou.utils.Signal.Listener<String> ear = t -> { said.add( t ); return false; };
		xyz.gabriwar.warpedpixeldungeon.utils.GLog.update.add( ear );
		try {
			at( Season.SUMMER, Phase.DAY, 0.3f );
			String[][] cases = {
					{ "wet", "FISHER", "fisher_day" },
					{ "dry", "FISHER", "farmer_day" },
					{ "dry", "LOAFER", "line" },
			};
			for (String[] k : cases){
				Settlement s = k[0].equals( "wet" ) ? wet : dry;
				OverworldLevel ow = windowAt( s.cx, s.cy );
				Settler f = member( s, s.firstFamily(), 0 );
				f.role = Role.valueOf( k[1] ).ordinal();
				f.pos = VillageRoutine.arrivalCell( ow, f, f.homeX, f.homeY );
				assertNotEquals( -1, f.pos );
				ow.mobs.add( f );
				liven( ow, new ArrayList<>(), s.cx, s.cy + 1 );
				f.sprite = stub();
				said.clear();
				f.interact( Dungeon.hero );
				String line = String.join( "", said );
				boolean fits = false;
				int n = k[2].equals( "line" ) ? 6 : 3;
				for (int i = 0; i < n; i++) fits |= line.contains( Messages.get( Villager.class, k[2] + "_" + i ) );
				assertTrue( k[0] + " " + k[1] + " said: " + line, fits );
				Actor.clear();
			}

			//abed: the old grumble
			at( Season.SUMMER, Phase.NIGHT, 0.6f );
			OverworldLevel ow = windowAt( dry.cx, dry.cy );
			Settler f = member( dry, dry.firstFamily(), 0 );
			f.pos = VillageRoutine.arrivalCell( ow, f, f.homeX, f.homeY );
			ow.mobs.add( f );
			liven( ow, new ArrayList<>(), dry.cx, dry.cy + 1 );
			f.sprite = stub();
			said.clear();
			f.interact( Dungeon.hero );
			assertTrue( String.join( "", said ).contains( Messages.get( Villager.class, "asleep" ) ) );
		} finally {
			xyz.gabriwar.warpedpixeldungeon.utils.GLog.update.remove( ear );
		}
	}

	// ---------------------------------------------------------------- the clock as turns read it

	//rain and sleet are rain, snow is snow until the wind drives it, a blizzard, hail or a
	//storm is a storm, and nothing falling is fair whatever the sky
	@Test
	public void theSkySortsIntoTheSchedulesWeather(){
		ClimateManager.debugWindOverride = 0f;
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		assertEquals( Weather.FAIR, VillageRoutine.weather() );
		ClimateManager.debugPrecipOverride = 0.5f;
		assertEquals( Weather.RAIN, VillageRoutine.weather() );
		ClimateManager.debugPrecipTypeOverride = PrecipType.SLEET;
		assertEquals( Weather.RAIN, VillageRoutine.weather() );
		ClimateManager.debugPrecipTypeOverride = PrecipType.SNOW;
		assertEquals( Weather.SNOW, VillageRoutine.weather() );
		ClimateManager.debugWindOverride = 20f;
		assertEquals( Weather.STORM, VillageRoutine.weather() );
		ClimateManager.debugWindOverride = 0f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.BLIZZARD;
		assertEquals( Weather.STORM, VillageRoutine.weather() );
		ClimateManager.debugPrecipTypeOverride = PrecipType.HAIL;
		assertEquals( Weather.STORM, VillageRoutine.weather() );
		ClimateManager.debugPrecipTypeOverride = PrecipType.NONE;
		assertEquals( Weather.FAIR, VillageRoutine.weather() );
	}

	//the clock is read once a turn, and a shower is believed only once it has held
	@Test
	public void theClockBelievesTheRainOnceItHolds(){
		at( Season.SUMMER, Phase.DAY, 0.3f );
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		VillageRoutine.Clock c = VillageRoutine.clock();
		assertEquals( Weather.FAIR, c.weather );
		assertTrue( "the same reading all turn", c == VillageRoutine.clock() );
		ClimateManager.debugPrecipOverride = 0.5f;
		int held = 0;
		for (int i = 0; i < 3 * VillageRoutine.WEATHER_HOLD && VillageRoutine.clock().weather == Weather.FAIR; i++){
			tick();
			held++;
		}
		assertEquals( Weather.RAIN, VillageRoutine.clock().weather );
		assertTrue( "held " + held, held >= VillageRoutine.WEATHER_HOLD && held <= VillageRoutine.WEATHER_HOLD + 2 );
		//nobody went in on the turn it was believed; all of them within 32 turns
		VillageRoutine.Clock now = VillageRoutine.clock();
		int p0 = VillageRoutine.personal( 1L, 0 ) & ~(31 << 24);
		assertEquals( Slot.WORK, VillageRoutine.slotFor( Role.FARMER, now, p0 | (31 << 24) ) );
		//read every turn, as the settlers do: a gap past WEATHER_HOLD would read as a time jump
		for (int i = 0; i < 32; i++){
			tick();
			VillageRoutine.clock();
		}
		assertEquals( Slot.INDOORS, VillageRoutine.slotFor( Role.FARMER, VillageRoutine.clock(), p0 | (31 << 24) ) );
	}

	//three paths a turn across the window, and a fresh three the next
	@Test
	public void pathsAreRationedPerTurn(){
		at( Season.SUMMER, Phase.DAY, 0.3f );
		for (int i = 0; i < VillageRoutine.PATHS_PER_TURN; i++) assertTrue( VillageRoutine.takePath() );
		assertFalse( VillageRoutine.takePath() );
		assertFalse( VillageRoutine.takePath() );
		tick();
		assertTrue( VillageRoutine.takePath() );
	}

	// ---------------------------------------------------------------- saves

	//through the real save text, an unknown home included
	@Test
	public void theRoutineSurvivesTheSaveFile() throws Exception {
		Villager v = new Villager();
		v.homeSector = WorldStructures.sectorOf( -3, 8 );
		v.house = 7;
		v.member = 1;
		v.role = Role.ELDER.ordinal();
		v.slot = Slot.INDOORS.ordinal();
		Villager unknown = new Villager();
		Bundle b = new Bundle();
		b.put( "known", v );
		b.put( "unknown", unknown );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out ) );
		Bundle back = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		Villager w = (Villager) back.get( "known" );
		assertEquals( 7, w.house );
		assertEquals( 1, w.member );
		assertEquals( Role.ELDER.ordinal(), w.role );
		assertEquals( Slot.INDOORS.ordinal(), w.slot );
		assertEquals( Integer.MIN_VALUE, w.homeX );
		assertEquals( v.homeSector, w.homeSector );
		Villager u = (Villager) back.get( "unknown" );
		assertEquals( -1, u.house );
		assertEquals( -1, u.role );
		assertEquals( Integer.MIN_VALUE, u.homeY );
		assertEquals( Long.MIN_VALUE, u.homeSector );
	}
}
