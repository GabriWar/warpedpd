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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PathFinder;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A second look at the places of the caves (CaveSites), from outside the unit: the sealed tomb's
 * chest is reached through its door alone, a rift's reach holds nothing that burns, a network
 * mirror never peoples a place, places resolved on several worker threads at once are the very
 * places one thread resolves, every kind lies on every slice it may, and a tomb's guards stop
 * coming once it is opened and its key taken.
 */
public class CaveSitesVerifyTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		Ring.initGems();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	private int turn, depth, branch;
	private long dungeonSeed;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero, hero;

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		dungeonSeed = Dungeon.seed;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.seed = SEED;
		hero = new Hero();
		hero.lvl = 15;
		hero.sprite = new CharSprite(){
			@Override public void place( int cell ){}
			@Override public void showStatus( int color, String text, Object... args ){}
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){}
		};
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.seed = dungeonSeed;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------ helpers

	private static ArrayList<CaveSites.Site> sample( int altitude ){
		ArrayList<CaveSites.Site> out = new ArrayList<>();
		for (int sy = -6; sy < 6; sy++){
			for (int sx = -6; sx < 6; sx++){
				CaveSites.Site s = CaveSites.site( SEED, altitude, sx, sy );
				if (s != null) out.add( s );
			}
		}
		return out;
	}

	private static int[] originFor( CaveSites.Site s ){
		return new int[]{ s.cx - W / 2 + 7, s.cy - H / 2 - 5 };
	}

	private static CaveSites.Site first( CaveSites.Type t, int altitude ){
		for (CaveSites.Site s : sample( altitude )) if (s.type == t) return s;
		return null;
	}

	//the slice's live window round a place: a mirror's, made the host's unless `mirror`
	private OverworldLevel levelAt( CaveSites.Site s, boolean mirror ) throws Exception {
		PathFinder.setMapSize( W, H );
		Dungeon.depth = WorldLayers.depthOf( s.altitude );
		int[] o = originFor( s );
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, o[0], o[1], shift );
		OverworldLevel ow = OverworldLevel.forNetwork( s.altitude, SEED, o[0], o[1], shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		if (!mirror){
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		}
		Dungeon.level = ow;
		hero.pos = ow.localCell( s.standX, s.standY );
		return ow;
	}

	private static int count( OverworldLevel ow, Class<?> cls ){
		int n = 0;
		for (Mob m : ow.mobs) if (m.getClass() == cls) n++;
		for (Mob m : ow.parked()) if (m.getClass() == cls) n++;
		return n;
	}

	//every cell the stand cell walks to in a window's terrain
	private static boolean[] reach( int[] map, int from ){
		boolean[] seen = new boolean[W * H];
		ArrayDeque<Integer> q = new ArrayDeque<>();
		seen[from] = true;
		q.add( from );
		while (!q.isEmpty()){
			int c = q.poll();
			for (int n : new int[]{ 1, -1, W, -W }){
				int d = c + n;
				if (d < 0 || d >= W * H || seen[d] || (Terrain.flags[map[d]] & Terrain.PASSABLE) == 0) continue;
				seen[d] = true;
				q.add( d );
			}
		}
		return seen;
	}

	// ------------------------------------------------------------ the tomb

	@Test
	public void tombChestIsReachedThroughItsDoorAlone(){
		int checked = 0;
		for (int a = -8; a >= -WorldLayers.MAX_BELOW; a--){
			for (CaveSites.Site t : sample( a )){
				if (t.type != CaveSites.Type.TOMB) continue;
				int[] o = originFor( t );
				WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], 0f );
				int[] map = w.terrain.clone();
				//every cell of its ring stands in the window: wall, or the iron door
				for (int dy = -3; dy <= 3; dy++){
					for (int dx = -3; dx <= 3; dx++){
						if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != 3) continue;
						int c = (t.cx + dx - o[0]) + (t.cy + dy - o[1]) * W;
						int want = dx == 0 && dy == 3 ? Terrain.LOCKED_DOOR : Terrain.WALL;
						assertEquals( "tomb on " + a + " ring " + dx + "," + dy, want, map[c] );
					}
				}
				int stand = (t.standX - o[0]) + (t.standY - o[1]) * W;
				int loot = (t.lootX - o[0]) + (t.lootY - o[1]) * W;
				int key = (t.keyX - o[0]) + (t.keyY - o[1]) * W;
				int door = (t.cx - o[0]) + (t.cy + 3 - o[1]) * W;
				boolean[] shut = reach( map, stand );
				assertFalse( "tomb on " + a + ": its chest is reached with the door shut", shut[loot] );
				assertTrue( "tomb on " + a + ": its key is out of reach", shut[key] );
				map[door] = Terrain.DOOR;
				assertTrue( "tomb on " + a + ": the open door does not reach the chest", reach( map, stand )[loot] );
				checked++;
			}
		}
		assertTrue( "only " + checked + " tombs near the origin", checked >= 3 );
	}

	@Test
	public void tombGuardsStopOnceItIsOpenedAndItsKeyTaken() throws Exception {
		CaveSites.Site t = null;
		for (int a = -8; a >= -WorldLayers.MAX_BELOW && t == null; a--) t = first( CaveSites.Type.TOMB, a );
		assertNotNull( t );
		OverworldLevel ow = levelAt( t, false );
		Dungeon.cycleTurn = 40 * DayNightCycle.FULL_CYCLE + 50;
		ow.populateLayerSites();
		assertEquals( 1, count( ow, DwarfLich.class ) );
		assertEquals( 2, count( ow, FossilSkeleton.class ) );
		int door = ow.localCell( t.cx, t.cy + 3 );
		//the key taken, the door still locked: the dead still keep it, day after day
		ow.heaps.remove( ow.localCell( t.keyX, t.keyY ) );
		ow.mobs.clear();
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		ow.populateLayerSites();
		assertEquals( 1, count( ow, DwarfLich.class ) );
		//opened: they come back no more
		ow.map[door] = Terrain.DOOR;
		ow.mobs.clear();
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		ow.populateLayerSites();
		assertEquals( 0, count( ow, DwarfLich.class ) );
		assertEquals( 0, count( ow, FossilSkeleton.class ) );
	}

	// ------------------------------------------------------------ the rift

	@Test
	public void riftReachHoldsNothingThatBurns(){
		int checked = 0;
		for (int a = OverworldFauna.DEEP_CAVES; a >= -WorldLayers.MAX_BELOW; a--){
			for (CaveSites.Site s : sample( a )){
				if (s.type != CaveSites.Type.RIFT || checked >= 12) continue;
				int[] o = originFor( s );
				WindowGenerator.Window w = WindowGenerator.generate( SEED, a, o[0], o[1], 0f );
				for (int dy = -9; dy <= 9; dy++){
					for (int dx = -9; dx <= 9; dx++){
						if (dx * dx + dy * dy > 8.5 * 8.5) continue;
						int c = (s.cx + dx - o[0]) + (s.cy + dy - o[1]) * W;
						assertEquals( "rift on " + a + " burns at " + dx + "," + dy, 0, Terrain.flags[w.terrain[c]] & Terrain.FLAMABLE );
					}
				}
				for (long k : s.fissure){
					int c = ((int)(k & 0xFFFFFFFFL) - o[0]) + ((int)(k >> 32) - o[1]) * W;
					assertEquals( Terrain.EMBERS, w.terrain[c] );
				}
				assertTrue( s.fissure.length >= 5 );
				checked++;
			}
		}
		assertTrue( "only " + checked + " rifts near the origin", checked >= 4 );
	}

	// ------------------------------------------------------------ mirrors

	@Test
	public void aMirrorNeverPeoplesAPlace() throws Exception {
		Field laid = OverworldLevel.class.getDeclaredField( "hoardLaid" );
		laid.setAccessible( true );
		for (CaveSites.Type type : CaveSites.Type.values()){
			CaveSites.Site s = null;
			for (int a = type.shallowest; a >= type.deepest && s == null; a--) s = first( type, a );
			assertNotNull( type.key(), s );
			OverworldLevel ow = levelAt( s, true );
			assertFalse( type.key() + " not adopted by the mirror", ow.caveSites().isEmpty() );
			Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
			ow.populateLayerSites();
			assertTrue( type.key() + " peopled on a mirror", ow.mobs.isEmpty() );
			assertEquals( type.key() + " laid loot on a mirror", 0, ow.heaps.valueList().size() );
			assertTrue( type.key() + " claimed a hoard on a mirror", ((java.util.Set<?>) laid.get( ow )).isEmpty() );
		}
	}

	// ------------------------------------------------------------ the world

	@Test
	public void workerThreadsResolveTheSamePlaces() throws Exception {
		final List<int[]> sectors = new ArrayList<>();
		for (int a : new int[]{ -1, -3, -6, -9, -12 }){
			for (int sy = -3; sy < 3; sy++) for (int sx = -3; sx < 3; sx++) sectors.add( new int[]{ a, sx, sy } );
		}
		//one thread, from a cold cache
		CaveSites.checkSeed( SEED ^ 7 );
		CaveSites.checkSeed( SEED );
		final ArrayList<CaveSites.Site> serial = new ArrayList<>();
		for (int[] k : sectors) serial.add( CaveSites.site( SEED, k[0], k[1], k[2] ) );
		//four at once, each its own order, from a cold cache again
		CaveSites.checkSeed( SEED ^ 7 );
		CaveSites.checkSeed( SEED );
		ExecutorService pool = Executors.newFixedThreadPool( 4 );
		try {
			ArrayList<Future<CaveSites.Site[]>> runs = new ArrayList<>();
			for (int r = 0; r < 4; r++){
				final long shuffle = r;
				runs.add( pool.submit( () -> {
					ArrayList<Integer> order = new ArrayList<>();
					for (int i = 0; i < sectors.size(); i++) order.add( i );
					Collections.shuffle( order, new java.util.Random( shuffle ) );
					CaveSites.Site[] got = new CaveSites.Site[sectors.size()];
					for (int i : order){
						int[] k = sectors.get( i );
						got[i] = CaveSites.site( SEED, k[0], k[1], k[2] );
					}
					return got;
				} ) );
			}
			for (Future<CaveSites.Site[]> f : runs){
				CaveSites.Site[] got = f.get();
				for (int i = 0; i < got.length; i++){
					CaveSites.Site a = serial.get( i ), b = got[i];
					String at = Arrays.toString( sectors.get( i ) );
					assertEquals( at, a == null, b == null );
					if (a == null) continue;
					assertEquals( at, a.type, b.type );
					assertEquals( at, a.key, b.key );
					assertTrue( at, Arrays.equals( a.cells, b.cells ) );
					assertTrue( at, Arrays.equals( a.terrain, b.terrain ) );
					assertTrue( at, Arrays.equals( a.guards, b.guards ) );
					assertEquals( at, a.shaftX, b.shaftX );
					assertEquals( at, a.props, b.props );
				}
			}
			//and windows prepared side by side, as the streaming workers do, are one window
			CaveSites.Site m = null;
			for (CaveSites.Site s : serial) if (s != null && s.type == CaveSites.Type.MINE) m = s;
			assertNotNull( m );
			final int[] o = originFor( m );
			final int alt = m.altitude;
			CaveSites.checkSeed( SEED ^ 7 );
			CaveSites.checkSeed( SEED );
			ArrayList<Future<WindowGenerator.Window>> wins = new ArrayList<>();
			for (int r = 0; r < 3; r++) wins.add( pool.submit( () -> WindowGenerator.generate( SEED, alt, o[0], o[1], 0f ) ) );
			WindowGenerator.Window w0 = wins.get( 0 ).get();
			for (Future<WindowGenerator.Window> f : wins){
				WindowGenerator.Window w = f.get();
				assertTrue( Arrays.equals( w0.terrain, w.terrain ) );
				assertTrue( Arrays.equals( w0.link, w.link ) );
				assertEquals( w0.sites.size(), w.sites.size() );
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void everyKindLiesOnEverySliceItMay(){
		//144 sectors, about 864 by 864 cells: a slice explored a while shows a few of each
		StringBuilder rare = new StringBuilder();
		for (CaveSites.Type t : CaveSites.Type.values()){
			for (int a = t.shallowest; a >= t.deepest; a--){
				int n = 0, wanted = 0;
				for (CaveSites.Site s : sample( a )) if (s.type == t) n++;
				for (int sy = -6; sy < 6; sy++) for (int sx = -6; sx < 6; sx++) if (CaveSites.typeOf( SEED, a, sx, sy ) == t) wanted++;
				if (n < 2) rare.append( "\n" ).append( t.key() ).append( " on " ).append( a ).append( ": " ).append( n )
						.append( " laid of " ).append( wanted ).append( " looked for" );
			}
		}
		assertTrue( "kinds too rare to find on their slices:" + rare, rare.length() == 0 );
	}
}
