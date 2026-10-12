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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * What the fields' and the animals' tests share (HomesteadFarmTest, HomesteadAnimalsTest): a real
 * window of the surface derived from a seed and made the host's own, open ground in it, the hero
 * standing somewhere seeing everything, the calendar set to a day of a season (turn-counted, the
 * year starting on the first of spring), the global clock moved on, a save and a load, and the
 * window jumping away and back. Everything they touch is put back after each test.
 */
public abstract class FarmTestBase {

	static final long SEED = 0x5EED0F7EA7L;
	static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	/** Days of each season, year 0 (spring 0-90, summer 91-181, autumn 182-272, winter 273-364). */
	static final int SPRING_DAY = 20, SUMMER_DAY = 120, AUTUMN_DAY = 250, WINTER_DAY = 300;

	private int depth, branch, challenges, startDay, turn;
	private DayNightCycle.Phase override;
	private float shift, duration, temp;
	private boolean shiftHeld;
	private InterlevelScene.Mode mode;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level savedLevel;
	private Hero savedHero;
	Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		challenges = Dungeon.challenges;
		startDay = Dungeon.calendarStartDay;
		turn = Dungeon.cycleTurn;
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		duration = Statistics.duration;
		temp = ClimateManager.debugTempOverride;
		mode = InterlevelScene.mode;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 4242 );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		ClimateManager.debugTempOverride = 15f;
		setDay( SUMMER_DAY );
		HomesteadWorld.reset();
		hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		HomesteadWorld.reset();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		DayNightCycle.debugPhaseOverride = override;
		Statistics.duration = duration;
		ClimateManager.debugTempOverride = temp;
		InterlevelScene.mode = mode;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ time

	/** The world's clock at noon of a day (WorldClock.day()). */
	static void setDay( int day ){
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + 1000;
	}

	static GameCalendar.Season season(){
		return GameCalendar.season();
	}

	/** The global clock (FarmCrop.clock) moved on. */
	static void advance( float turns ){
		Statistics.duration += turns;
	}

	/** The actors' clock (Actor.now) moved on: what a level's own timers count by. */
	static void advanceNow( float turns ){
		try {
			Field now = Actor.class.getDeclaredField( "now" );
			now.setAccessible( true );
			now.setFloat( null, now.getFloat( null ) + turns );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	// ------------------------------------------------------------------ the world

	//the live surface window round a world cell, built as a mirror's is and made the host's own
	static OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	static void set( Object o, String name, Object value ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, value );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	/** The window jumps to be centred on a world cell (OverworldLevel.jumpWindowTo). */
	static void jump( OverworldLevel ow, int wx, int wy ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( "jumpWindowTo", int.class, int.class );
			m.setAccessible( true );
			m.invoke( ow, wx, wy );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//the top-left cell of a w x h stretch of open, unedited, unprotected ground (and a cell round
	//it) in the window, nearest its middle
	static int openGround( OverworldLevel ow, int w, int h ){
		int mid = W / 2 + (H / 2) * W;
		for (int r = 0; r < 70; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int o = mid + dx + dy * W;
					if (fits( ow, o, w, h )) return o;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int o, int w, int h ){
		int ox = o % W, oy = o / W;
		for (int y = oy - 1; y <= oy + h; y++){
			for (int x = ox - 1; x <= ox + w; x++){
				if (x < 3 || y < 3 || x >= W - 3 || y >= H - 3) return false;
				int c = x + y * W;
				int t = ow.map[c];
				if (!ow.passable[c] || ow.pit[c] || ow.avoid[c] || (Terrain.flags[t] & Terrain.LIQUID) != 0
						|| t == Terrain.FROZEN_WATER || t == Terrain.BRIDGE || t == Terrain.WELL || Terrain.homestead( t )
						|| ow.protectedForBuilding( c ) || ow.pristineAt( c ) != t || ow.heaps.get( c ) != null) return false;
				for (Mob m : ow.mobs) if (m.pos == c) return false;
			}
		}
		//no water near it either: the fields' water must be the test's own
		for (int y = oy - 3; y <= oy + h + 2; y++){
			for (int x = ox - 3; x <= ox + w + 2; x++){
				int t = ow.map[x + y * W];
				if (t == Terrain.WATER || t == Terrain.DEEP_WATER || t == Terrain.WELL) return false;
			}
		}
		return true;
	}

	/** A window with a w x h stretch of open ground in it: {wx, wy, top-left cell}. */
	int[] site( int w, int h ){
		for (int[] at : new int[][]{ { 300, 300 }, { -400, 260 }, { 600, -500 }, { -900, -700 }, { 1500, 900 }, { 2400, -1800 } }){
			OverworldLevel ow = windowAt( at[0], at[1] );
			int o = openGround( ow, w, h );
			if (o != -1) return new int[]{ at[0], at[1], o };
		}
		throw new AssertionError( "no open ground in any test window" );
	}

	/** The window of a site, with every wild mob taken off it (the tests place their own). */
	static OverworldLevel quiet( int[] at ){
		OverworldLevel ow = windowAt( at[0], at[1] );
		ow.mobs.clear();
		return ow;
	}

	//the hero standing on a cell, seeing everything, on the actors' list
	void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
		if (!Actor.all().contains( hero )) Actor.add( hero );
	}

	void give( Item item ){
		assertTrue( item.collect( hero.belongings.backpack ) );
	}

	static void build( OverworldLevel ow, int cell, String id ){
		Homestead.build( ow, cell, BuildCatalog.byId( id ), null );
	}

	/** A w x h ring of a piece (fence) with its top-left at o: the cells of the ring. */
	static ArrayList<Integer> ring( OverworldLevel ow, int o, int w, int h, String id ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				if (x != 0 && y != 0 && x != w - 1 && y != h - 1) continue;
				build( ow, o + x + y * W, id );
				out.add( o + x + y * W );
			}
		}
		return out;
	}

	/** A mob put down on a cell, on the level and the actors' list. */
	static <T extends Mob> T put( OverworldLevel ow, T m, int cell ){
		m.pos = cell;
		ow.mobs.add( m );
		Actor.add( m );
		return m;
	}

	/** Every mob of the level on the actors' list (they are put there by a scene in play). */
	static void actorsOn( OverworldLevel ow ){
		for (Mob m : ow.mobs) if (!Actor.all().contains( m )) Actor.add( m );
	}

	static OverworldLevel saveAndLoad( OverworldLevel ow ) throws Exception {
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( read );
		Dungeon.level = back;
		return back;
	}
}
