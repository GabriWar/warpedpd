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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * An independent check of the places on the mountains: a window's sites are the pure ones whatever
 * resolved them first, whichever thread and whatever the season; none lie off the peaks; built
 * walls hold no ore; a hot spring feels warmer than the snow five cells off; the cairn's view is
 * granted once through a save; the tower's view every time; an eagle never acts on without spending.
 */
public class MountainSitesVerifyTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, turn, startDay;
	private DayNightCycle.Phase override;
	private float temp;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		override = DayNightCycle.debugPhaseOverride;
		temp = ClimateManager.debugTempOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.calendarStartDay = 0;
		hero = new Hero();
		hero.lvl = 20;
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		DayNightCycle.debugPhaseOverride = override;
		ClimateManager.debugTempOverride = temp;
		WorldModel.releaseSeasonShift();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void die(){ }
			@Override public void move( int from, int to ){ }
			@Override public void attack( int cell ){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showAlert(){ }
			@Override public void hideAlert(){ }
			@Override public void operate( int cell ){ }
		};
		sp.visible = false;
		return sp;
	}

	private static void setField( Object o, String name, Object v ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static boolean act( Actor a ){
		try {
			Method m = Actor.class.getDeclaredMethod( "act" );
			m.setAccessible( true );
			return (Boolean) m.invoke( a );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//drops every resolved site: the next resolution is a cold one
	private static void coldCaches(){
		MountainSites.checkSeed( SEED + 1 );
		MountainSites.checkSeed( SEED );
	}

	private static MountainSites.Site nearest( MountainSites.Kind k ){
		MountainSites.Site s = MountainSites.nearest( SEED, k, 0, 0, 48 );
		assertNotNull( "no " + k, s );
		return s;
	}

	private static List<MountainSites.Site> oneOfEach(){
		ArrayList<MountainSites.Site> out = new ArrayList<>();
		for (MountainSites.Kind k : MountainSites.Kind.values()) out.add( nearest( k ) );
		return out;
	}

	private OverworldLevel windowAt( MountainSites.Site s, int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = s.wx - W / 2, oy = s.wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, ox, oy, 0f );
		OverworldLevel ow = OverworldLevel.forNetwork( s.altitude, SEED, ox, oy, 0f, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.level = ow;
		Dungeon.depth = WorldLayers.depthOf( s.altitude );
		hero.pos = -1;
		for (int r = 0; r <= 8 && hero.pos == -1; r++){
			for (int dy = -r; dy <= r && hero.pos == -1; dy++){
				for (int dx = -r; dx <= r && hero.pos == -1; dx++){
					int cell = ow.localCell( wx + dx, wy + dy );
					if (cell != -1 && ow.passable[cell] && !ow.occupied( cell )) hero.pos = cell;
				}
			}
		}
		assertTrue( hero.pos != -1 );
		assertTrue( ow.mountainSites().contains( s ) );
		return ow;
	}

	private static OverworldLevel saveAndLoad( OverworldLevel ow ){
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( b );
		Dungeon.level = back;
		return back;
	}

	private static int mappedCount( OverworldLevel ow ){
		int n = 0;
		for (boolean m : ow.mapped) if (m) n++;
		return n;
	}

	// ------------------------------------------------------------------ purity

	@Test
	public void aWindowsSitesAreThePureOnesWhateverResolvedThemFirst(){
		List<MountainSites.Site> probes = oneOfEach();
		//windows first, cold: the resolvers read the window's own bands and elevations
		coldCaches();
		ArrayList<List<MountainSites.Site>> fromWindows = new ArrayList<>();
		for (MountainSites.Site s : probes){
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			fromWindows.add( peaks( WindowGenerator.generate( SEED, s.altitude, ox, oy, 0f ) ) );
		}
		//then the pure functions, cold: everything sampled off the world
		coldCaches();
		for (int i = 0; i < probes.size(); i++){
			MountainSites.Site s = probes.get( i );
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			List<MountainSites.Site> pure = MountainSites.sitesIn( SEED, s.altitude, ox + 1, oy + 1, ox + W - 2, oy + H - 2 );
			assertEquals( s.kind + " at +" + s.altitude, pure, fromWindows.get( i ) );
			assertTrue( pure.contains( s ) );
		}
	}

	@Test
	public void sitesAndTheirMasonryDoNotChangeWithTheSeason(){
		float[] shifts = { WorldModel.WINTER_SHIFT, 0f, WorldModel.SUMMER_SHIFT };
		for (MountainSites.Site s : oneOfEach()){
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			WindowGenerator.Window[] ws = new WindowGenerator.Window[shifts.length];
			for (int i = 0; i < shifts.length; i++) ws[i] = WindowGenerator.generate( SEED, s.altitude, ox, oy, shifts[i] );
			for (int i = 1; i < shifts.length; i++){
				assertEquals( s.kind + ": the same sites all year", peaks( ws[0] ), peaks( ws[i] ) );
			}
			for (MountainSites.Site t : peaks( ws[0] )){
				for (int wy = t.y0; wy <= t.y1; wy++){
					for (int wx = t.x0; wx <= t.x1; wx++){
						int x = wx - ox, y = wy - oy;
						if (x < 1 || y < 1 || x > W - 2 || y > H - 2) continue;
						int solid = MountainSites.solidAt( SEED, t, wx, wy );
						if (solid == -1) continue;
						for (WindowGenerator.Window w : ws){
							assertEquals( t.kind + " masonry at " + wx + "," + wy, solid, w.terrain[x + y * W] );
						}
					}
				}
			}
		}
	}

	@Test
	public void threadsResolvingAtOnceAgreeWithOneAlone() throws Exception {
		final int S0 = -2, S1 = 1;
		coldCaches();
		final ArrayList<MountainSites.Site> reference = new ArrayList<>();
		for (int sy = S0; sy <= S1; sy++){
			for (int sx = S0; sx <= S1; sx++){
				for (int a = 1; a <= WorldLayers.MAX_ABOVE; a++) reference.add( MountainSites.siteOf( SEED, sx, sy, a ) );
				reference.add( MountainSites.summitOf( SEED, sx, sy ) );
			}
		}
		coldCaches();
		ExecutorService pool = Executors.newFixedThreadPool( 4 );
		try {
			ArrayList<Future<List<MountainSites.Site>>> runs = new ArrayList<>();
			for (int t = 0; t < 4; t++){
				final long order = t;
				runs.add( pool.submit( () -> {
					ArrayList<int[]> jobs = new ArrayList<>();
					for (int sy = S0; sy <= S1; sy++){
						for (int sx = S0; sx <= S1; sx++){
							for (int a = 1; a <= WorldLayers.MAX_ABOVE + 1; a++) jobs.add( new int[]{ sx, sy, a } );
						}
					}
					Collections.shuffle( jobs, new Random( order ) );
					MountainSites.Site[] got = new MountainSites.Site[jobs.size()];
					for (int[] j : jobs){
						int idx = ((j[1] - S0) * (S1 - S0 + 1) + (j[0] - S0)) * (WorldLayers.MAX_ABOVE + 1) + (j[2] - 1);
						got[idx] = j[2] <= WorldLayers.MAX_ABOVE
								? MountainSites.siteOf( SEED, j[0], j[1], j[2] )
								: MountainSites.summitOf( SEED, j[0], j[1] );
					}
					ArrayList<MountainSites.Site> l = new ArrayList<>();
					Collections.addAll( l, got );
					return l;
				} ) );
			}
			for (Future<List<MountainSites.Site>> f : runs) assertEquals( reference, f.get() );
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void noPlacesOffThePeaks(){
		for (int sy = -3; sy <= 3; sy++){
			for (int sx = -3; sx <= 3; sx++){
				assertNull( MountainSites.siteOf( SEED, sx, sy, 0 ) );
				assertNull( MountainSites.siteOf( SEED, sx, sy, -1 ) );
				assertNull( MountainSites.siteOf( SEED, sx, sy, WorldLayers.MAX_ABOVE + 1 ) );
			}
		}
		assertTrue( MountainSites.sitesIn( SEED, 0, -200, -200, 200, 200 ).isEmpty() );
		assertTrue( MountainSites.sitesIn( SEED, -3, -200, -200, 200, 200 ).isEmpty() );
		assertTrue( WindowGenerator.generate( SEED, 0, -88, -88, 0f ).sites.isEmpty() );
		assertTrue( peaks( WindowGenerator.generate( SEED, -2, -88, -88, 0f ) ).isEmpty() );
	}

	// ------------------------------------------------------------------ ore

	@Test
	public void builtWallsHoldNoOreAndTheWindowAgreesWithTheWorld(){
		for (MountainSites.Kind k : new MountainSites.Kind[]{ MountainSites.Kind.HERMIT, MountainSites.Kind.TOWER, MountainSites.Kind.PASS }){
			MountainSites.Site s = nearest( k );
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, ox, oy, 0f );
			byte[] veins = w.veins;
			int walls = 0;
			for (int wy = s.y0 - 1; wy <= s.y1 + 1; wy++){
				for (int wx = s.x0 - 1; wx <= s.x1 + 1; wx++){
					int c = (wx - ox) + (wy - oy) * W;
					Ores.Kind pure = Ores.oreAt( SEED, s.altitude, wx, wy );
					Ores.Kind memo = veins[c] == 0 ? null : Ores.Kind.values()[veins[c] - 1];
					assertEquals( k + " cell " + wx + "," + wy + ": the window's veins are the world's", pure, memo );
					if (MountainSites.solidAt( SEED, s, wx, wy ) == Terrain.WALL){
						walls++;
						assertEquals( Terrain.WALL, w.terrain[c] );
						assertNull( k + " wall " + wx + "," + wy + " holds ore", memo );
					}
				}
			}
			assertTrue( k + " built no walls", walls > 0 );
		}
	}

	// ------------------------------------------------------------------ climate

	@Test
	public void aSpringFeelsWarmerThanTheSnowFiveCellsOff(){
		MountainSites.Site s = nearest( MountainSites.Kind.SPRINGS );
		OverworldLevel ow = windowAt( s, s.wx, s.wy + 9 );
		ClimateManager.debugTempOverride = -25f;
		int pool = ow.localCell( s.wx, s.wy );
		hero.pos = pool;
		float atPool = TileTemperature.feelsLikeAt( pool, hero );
		int checked = 0;
		for (int d = 0; d < 4; d++){
			int c = ow.localCell( s.wx + 5 * MountainSites.DX[d], s.wy + 5 * MountainSites.DY[d] );
			if (c == -1 || !ow.passable[c] || ow.water[c]) continue;
			checked++;
			assertEquals( Float.NEGATIVE_INFINITY, ow.springWarmth( c ), 0f );
			float off = TileTemperature.feelsLikeAt( c, hero );
			assertTrue( "pool " + atPool + " vs five off " + off, atPool > off + 20f );
			assertTrue( "the snow five off is still cold: " + off, off < 0f );
		}
		assertTrue( checked > 0 );
	}

	// ------------------------------------------------------------------ the views

	@Test
	public void theCairnShowsTheLandOnceThroughASave(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		MountainSites.Site top = nearest( MountainSites.Kind.CAIRN );
		OverworldLevel ow = windowAt( top, top.wx, top.wy + 1 );
		ow.populateMountainSites();
		SummitCairn cairn = null;
		for (Mob m : ow.mobs) if (m instanceof SummitCairn && ((SummitCairn) m).siteKey == top.key) cairn = (SummitCairn) m;
		assertNotNull( cairn );
		assertEquals( ow.localCell( top.wx, top.wy ), cairn.pos );
		assertFalse( cairn.topped );
		cairn.sprite = stub();
		Actor.add( hero );

		int before = mappedCount( ow );
		assertTrue( cairn.interact( hero ) );
		assertTrue( cairn.topped );
		assertNotNull( "the view lifts his spirits", hero.buff( Bless.class ) );
		assertTrue( "the land shows", mappedCount( ow ) > before + 1000 );
		int far = ow.localCell( top.wx + MountainSites.SUMMIT_VIEW - 2, top.wy );
		if (far != -1 && ow.discoverable[far]) assertTrue( ow.mapped[far] );

		//a second stone: nothing more
		Buff.detach( hero, Bless.class );
		cairn.interact( hero );
		assertNull( hero.buff( Bless.class ) );

		//nor after a save and a load, the cairn put back as it was
		OverworldLevel back = saveAndLoad( ow );
		Dungeon.hero = hero;
		back.populateMountainSites();
		int cairns = 0;
		for (Mob m : back.mobs){
			if (m instanceof SummitCairn && ((SummitCairn) m).siteKey == top.key){
				cairns++;
				SummitCairn c = (SummitCairn) m;
				assertTrue( c.topped );
				c.sprite = stub();
				c.interact( hero );
			}
		}
		assertEquals( 1, cairns );
		assertNull( hero.buff( Bless.class ) );
		assertFalse( back.claimHoard( top.key ) );
	}

	@Test
	public void theTowerTopShowsTheLandEveryTime(){
		MountainSites.Site t = nearest( MountainSites.Kind.TOWER );
		OverworldLevel ow = windowAt( t, t.wx, t.wy );
		int top = ow.localCell( t.wx, t.wy );
		assertTrue( ow.passable[top] );
		hero.pos = top;
		for (int i = 0; i < 2; i++){
			java.util.Arrays.fill( ow.mapped, false );
			ow.occupyCell( hero );
			assertTrue( "visit " + i + ": the land shows", mappedCount( ow ) > 500 );
		}
		//a step off the top shows nothing
		java.util.Arrays.fill( ow.mapped, false );
		int off = ow.localCell( t.wx + 1, t.wy );
		if (ow.passable[off]){
			hero.pos = off;
			ow.occupyCell( hero );
			assertEquals( 0, mappedCount( ow ) );
		}
	}

	// ------------------------------------------------------------------ the eagles' time

	@Test
	public void anEagleSpendsTimeOnEveryAct(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		//a hero struck headless has no cell selector to reset: the strike itself is not on trial
		hero = new Hero(){
			@Override public void interrupt(){ }
		};
		hero.lvl = 20;
		hero.sprite = stub();
		Dungeon.hero = hero;
		MountainSites.Site e = nearest( MountainSites.Kind.EYRIE );
		OverworldLevel ow = windowAt( e, e.wx - MountainSites.DX[e.dir] * 2, e.wy - MountainSites.DY[e.dir] * 2 );
		ow.populateMountainSites();
		hero.HP = hero.HT = 1_000_000;
		Actor.add( hero );
		ArrayList<Eagle> pair = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof Eagle && ((Eagle) m).eyrieKey == e.key) pair.add( (Eagle) m );
		assertEquals( 2, pair.size() );
		for (Eagle g : pair){
			g.sprite = stub();
			Actor.add( g );
		}
		//circling, stooping on a trespasser by the nest, then hurt and fleeing, then robbed
		for (int phase = 0; phase < 3; phase++){
			for (Eagle g : pair){
				if (phase == 1) g.damage( (int) (g.HT * 0.7f), hero );
				if (phase == 2){
					g.HP = g.HT;
					g.enrage( hero );
				}
			}
			//a change of mind may act again at once (Wandering.noticeEnemy, as every mob): never twice running
			for (int i = 0; i < 40; i++){
				for (Eagle g : pair){
					if (!g.isAlive()) continue;
					int free = 0;
					while (true){
						float before = g.cooldown();
						boolean next = act( g );
						if (!next || g.cooldown() > before) break;
						assertTrue( "phase " + phase + " step " + i + " state " + g.state + ": acts on and on without spending",
								++free < 3 );
					}
				}
			}
		}
	}

	//the places on the mountains a window laid (its sites list, in their own order)
	static List<MountainSites.Site> peaks( WindowGenerator.Window w ){
		List<MountainSites.Site> out = new ArrayList<>();
		for (Object o : w.sites) if (o instanceof MountainSites.Site) out.add( (MountainSites.Site) o );
		return out;
	}
}
