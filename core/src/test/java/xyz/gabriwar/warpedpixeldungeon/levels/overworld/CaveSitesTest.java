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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RiftElemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShrinePiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaveMiner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.DwarvenForge;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.LakeShrine;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Deepsilver;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Gem;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetSprites;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The places of the caves (CaveSites): pure and identical everywhere, spaced, kept to their
 * depths, never on a way between slices, a mine's shaft that lands in a real chamber, a tomb that
 * neither pick nor bomb opens, caches laid once through a save, guards that come back while
 * their cache lies there, the miner's prices and true rumours, the shrine's once-a-day rule, the
 * rift's bounded harm, and every string and sprite a place uses.
 */
public class CaveSitesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		Ring.initGems();
		//caches are identified as they are laid, and identifying reaches the badges
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	private int turn, depth, branch;
	private long dungeonSeed;
	private DayNightCycle.Phase override;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero, hero;
	private int gold;

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		dungeonSeed = Dungeon.seed;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		gold = Dungeon.gold;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.seed = SEED;
		hero = new Hero();
		hero.lvl = 15;
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.seed = dungeonSeed;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		Dungeon.gold = gold;
	}

	// ------------------------------------------------------------ helpers

	private static CharSprite stub(){
		return new CharSprite(){
			@Override public void place( int cell ){}
			@Override public void showStatus( int color, String text, Object... args ){}
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){}
		};
	}

	//one of every kind, the nearest the origin to its favourite slice (DebugScenes' own search)
	private static final EnumMap<CaveSites.Type, CaveSites.Site> ONE = new EnumMap<>( CaveSites.Type.class );

	private static CaveSites.Site one( CaveSites.Type t ){
		CaveSites.Site s = ONE.get( t );
		if (s == null){
			int pref = t == CaveSites.Type.TOMB ? -9 : t == CaveSites.Type.RIFT ? -7 : -4;
			if (!t.at( pref )) pref = t.shallowest;
			s = xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes.caveSite( SEED, t, pref );
			assertNotNull( "no " + t.key() + " near the origin for the test seed", s );
			ONE.put( t, s );
		}
		return s;
	}

	//every place of 12x12 sectors round the origin, slice by slice
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

	//a window holding the place, with it a little off its middle (as a walk there would have it)
	private static int[] originFor( CaveSites.Site s ){
		return new int[]{ s.cx - W / 2 + 7, s.cy - H / 2 - 5 };
	}

	private static Object field( Object o, String name ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
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

	private static Object invoke( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//the live window of a slice holding a place, built as a mirror's is and made the host's own;
	//the hero on the place's stand cell
	private OverworldLevel levelAt( CaveSites.Site s ){
		PathFinder.setMapSize( W, H );
		Dungeon.depth = WorldLayers.depthOf( s.altitude );
		int[] o = originFor( s );
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, o[0], o[1], shift );
		OverworldLevel ow = OverworldLevel.forNetwork( s.altitude, SEED, o[0], o[1], shift,
				xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.level = ow;
		hero.pos = ow.localCell( s.standX, s.standY );
		assertNotEquals( -1, hero.pos );
		return ow;
	}

	private static OverworldLevel saveAndLoad( OverworldLevel ow ) throws Exception {
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

	private static int count( OverworldLevel ow, Class<?> cls ){
		int n = 0;
		for (Mob m : ow.mobs) if (m.getClass() == cls) n++;
		for (Mob m : ow.parked()) if (m.getClass() == cls) n++;
		return n;
	}

	// ------------------------------------------------------------ the world

	@Test
	public void naturalHelpersMatchTheGenerator(){
		for (int a : new int[]{ -1, -4, -9 }){
			int ox = -1200, oy = 800;
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, ox, oy, 0f );
			int checked = 0, ways = 0;
			for (int y = 1; y < H - 1; y += 2){
				for (int x = 1; x < W - 1; x += 2){
					int c = x + y * W, wx = ox + x, wy = oy + y;
					boolean laid = false;
					for (Object o : w.sites) laid |= ((CaveSites.Site) o).holds( wx, wy );
					//a mine's shaft from above lands with its rock broken round it
					laid |= CaveSites.reservedLanding( SEED, a, wx, wy );
					if (laid) continue;
					boolean way = WindowGenerator.wayAt( SEED, a, wx, wy );
					assertEquals( "slice " + a + " at " + wx + "," + wy, way,
							w.link[c] != WindowGenerator.LINK_NONE || w.terrain[c] == Terrain.CHASM );
					if (way){
						ways++;
						continue;
					}
					int t = w.terrain[c] == Terrain.DEEP_WATER ? Terrain.WATER : w.terrain[c];
					assertEquals( "slice " + a + " at " + wx + "," + wy, WindowGenerator.naturalCaveTerrain( SEED, a, wx, wy ), t );
					checked++;
				}
			}
			assertTrue( checked > 5000 );
			assertTrue( "no ways on slice " + a, ways >= 0 );
		}
	}

	@Test
	public void sitesAreDeterministic(){
		ArrayList<CaveSites.Site> first = new ArrayList<>();
		for (int a : new int[]{ -1, -5, -10 }) first.addAll( sample( a ) );
		CaveSites.checkSeed( SEED ^ 1 );
		CaveSites.checkSeed( SEED );
		for (CaveSites.Site s : first){
			CaveSites.Site again = CaveSites.site( SEED, s.altitude, s.sx, s.sy );
			assertNotNull( again );
			assertTrue( s != again );
			assertEquals( s.type, again.type );
			assertEquals( s.key, again.key );
			assertTrue( Arrays.equals( s.cells, again.cells ) );
			assertTrue( Arrays.equals( s.terrain, again.terrain ) );
			assertTrue( Arrays.equals( s.guards, again.guards ) );
			assertTrue( Arrays.equals( s.lights, again.lights ) );
			assertEquals( s.props, again.props );
			assertEquals( s.shaftX, again.shaftX );
			assertEquals( s.x0 + "," + s.y0 + "," + s.x1 + "," + s.y1, again.x0 + "," + again.y0 + "," + again.x1 + "," + again.y1 );
		}
	}

	@Test
	public void overlappingWindowsAgreeWithSites(){
		for (CaveSites.Type t : CaveSites.Type.values()){
			CaveSites.Site s = one( t );
			int[] o = originFor( s );
			WindowGenerator.Window a = WindowGenerator.generate( SEED, s.altitude, o[0], o[1], 0f );
			WindowGenerator.Window b = WindowGenerator.generate( SEED, s.altitude, o[0] + 32, o[1] - 32, 0f );
			boolean seen = false;
			for (Object x : a.sites) seen |= ((CaveSites.Site) x).key == s.key;
			assertTrue( t.key(), seen );
			//the interiors of both, one cell in from each ring (the water by a ring is its shelf)
			for (int y = 2; y < H - 2; y++){
				for (int x = 2; x < W - 2; x++){
					int bx = x - 32, by = y + 32;
					if (bx < 2 || by < 2 || bx >= W - 2 || by >= H - 2) continue;
					assertEquals( t.key() + " at " + x + "," + y, a.terrain[x + y * W], b.terrain[bx + by * W] );
					assertEquals( a.link[x + y * W], b.link[bx + by * W] );
				}
			}
			//and the dressing over them
			int[][] da = WindowGenerator.dress( SEED, o[0], o[1], a.terrain, a, xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.SUMMER );
			int[][] db = WindowGenerator.dress( SEED, o[0] + 32, o[1] - 32, b.terrain, b, xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.SUMMER );
			for (int y = s.y0 - 1; y <= s.y1 + 1; y++){
				for (int x = s.x0; x <= s.x1; x++){
					int ca = (x - o[0]) + (y - o[1]) * W, cb = (x - o[0] - 32) + (y - o[1] + 32) * W;
					for (int l = 0; l < 3; l++) assertEquals( t.key() + " dress " + l, da[l][ca], db[l][cb] );
				}
			}
		}
	}

	@Test
	public void spacingContainmentDepthsAndDensity(){
		EnumMap<CaveSites.Type, Integer> seen = new EnumMap<>( CaveSites.Type.class );
		for (int a = -1; a >= -WorldLayers.MAX_BELOW; a--){
			ArrayList<CaveSites.Site> all = sample( a );
			float density = all.size() / 144f;
			assertTrue( "slice " + a + " density " + density, density >= 0.12f && density <= 0.6f );
			boolean nearOrigin = false;
			for (CaveSites.Site s : all){
				//inside its own sector, clear of its edge
				int lx0 = s.x0 - s.sx * CaveSites.SECTOR, lx1 = s.x1 - s.sx * CaveSites.SECTOR;
				int ly0 = s.y0 - s.sy * CaveSites.SECTOR, ly1 = s.y1 - s.sy * CaveSites.SECTOR;
				assertTrue( s.type + " " + lx0 + ".." + lx1, lx0 >= 1 && lx1 <= CaveSites.SECTOR - 2 );
				assertTrue( s.type + " " + ly0 + ".." + ly1, ly0 >= 1 && ly1 <= CaveSites.SECTOR - 2 );
				assertTrue( s.type + " on " + a, s.type.at( a ) );
				seen.merge( s.type, 1, Integer::sum );
				nearOrigin |= Math.abs( s.sx ) <= 4 && Math.abs( s.sy ) <= 4;
				for (CaveSites.Site o : all){
					if (o == s) continue;
					assertTrue( Math.max( Math.abs( o.cx - s.cx ), Math.abs( o.cy - s.cy ) ) >= 32 );
					assertTrue( "reaches overlap", o.x1 < s.x0 || o.x0 > s.x1 || o.y1 < s.y0 || o.y0 > s.y1 );
				}
			}
			assertTrue( "nothing near the origin on " + a, nearOrigin );
		}
		for (CaveSites.Type t : CaveSites.Type.values()) assertTrue( "no " + t.key() + " anywhere", seen.containsKey( t ) );
	}

	@Test
	public void neverOnWaysOrWater(){
		for (int a = -1; a >= -WorldLayers.MAX_BELOW; a -= 1){
			for (CaveSites.Site s : sample( a )){
				for (int i = 0; i < s.cells.length; i++){
					int wx = (int)(s.cells[i] & 0xFFFFFFFFL), wy = (int)(s.cells[i] >> 32);
					String at = s.type + " on " + a + " at " + wx + "," + wy;
					assertFalse( at, WindowGenerator.wayAt( SEED, a, wx, wy ) );
					assertFalse( at, WindowGenerator.pitFromAbove( SEED, a, wx, wy ) );
					assertFalse( at, CaveSites.reservedLanding( SEED, a, wx, wy ) );
					assertFalse( at, WorldModel.caveWet( SEED, wx, wy, a ) );
					//nor under a lake of the surface, which floods the slices under it
					if (-a < WorldModel.WATER_TIERS) assertFalse( at + " under a lake", WindowGenerator.surfaceWaterTier( SEED, wx, wy ) > -a );
					//and nothing solid beside a way
					if ((Terrain.flags[s.terrain[i]] & Terrain.SOLID) != 0){
						for (int[] d : new int[][]{ { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }){
							assertFalse( at, WindowGenerator.wayAt( SEED, a, wx + d[0], wy + d[1] ) );
						}
					}
				}
			}
		}
		//in a real window, every natural way keeps its way
		CaveSites.Site s = one( CaveSites.Type.GROTTO );
		int[] o = originFor( s );
		WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, o[0], o[1], 0f );
		for (int y = 1; y < H - 1; y++){
			for (int x = 1; x < W - 1; x++){
				int c = x + y * W;
				if (WindowGenerator.wayAt( SEED, s.altitude, o[0] + x, o[1] + y )){
					assertTrue( w.link[c] != WindowGenerator.LINK_NONE || w.terrain[c] == Terrain.CHASM );
				}
			}
		}
	}

	//the first mine with a shaft near the origin
	private static CaveSites.Site shaftMine(){
		for (int a = -1; a >= -6; a--){
			for (CaveSites.Site s : sample( a )) if (s.type == CaveSites.Type.MINE && s.hasShaft()) return s;
		}
		fail( "no mine with a shaft near the origin" );
		return null;
	}

	@Test
	public void mineShaftLandsInAChamber(){
		CaveSites.Site m = shaftMine();
		int[] o = originFor( m );
		WindowGenerator.Window top = WindowGenerator.generate( SEED, m.altitude, o[0], o[1], 0f );
		WindowGenerator.Window bottom = WindowGenerator.generate( SEED, m.altitude - 1, o[0], o[1], 0f );
		int c = (m.shaftX - o[0]) + (m.shaftY - o[1]) * W;
		assertEquals( Terrain.EXIT, top.terrain[c] );
		assertEquals( WindowGenerator.LINK_LADDER_DOWN, top.link[c] );
		assertEquals( Terrain.ENTRANCE, bottom.terrain[c] );
		assertEquals( WindowGenerator.LINK_LADDER_UP, bottom.link[c] );
		for (int n : PathFinder.NEIGHBOURS8){
			int t = bottom.terrain[c + n];
			assertTrue( "rock beside the landing", t != Terrain.WALL && t != Terrain.MINE_CRYSTAL && t != Terrain.MINE_BOULDER && t != Terrain.BOULDER );
		}
		//it opens onto a chamber of forty cells at least
		boolean[] seen = new boolean[W * H];
		ArrayDeque<Integer> q = new ArrayDeque<>();
		q.add( c );
		seen[c] = true;
		int reached = 0;
		while (!q.isEmpty()){
			int x = q.poll();
			reached++;
			for (int n : PathFinder.NEIGHBOURS4){
				int y = x + n;
				if (y < 0 || y >= W * H || seen[y] || (Terrain.flags[bottom.terrain[y]] & Terrain.PASSABLE) == 0) continue;
				seen[y] = true;
				q.add( y );
			}
		}
		assertTrue( "the shaft opens onto " + reached + " cells", reached >= 40 );
		//the ways of the two slices still match, the shaft's among them
		for (int y = 1; y < H - 1; y++){
			for (int x = 1; x < W - 1; x++){
				int i = x + y * W;
				if (bottom.link[i] == WindowGenerator.LINK_LADDER_UP){
					assertEquals( Terrain.EXIT, top.terrain[i] );
					assertEquals( WindowGenerator.LINK_LADDER_DOWN, top.link[i] );
				}
				if (top.link[i] == WindowGenerator.LINK_LADDER_DOWN){
					assertEquals( WindowGenerator.LINK_LADDER_UP, bottom.link[i] );
				}
			}
		}
	}

	@Test
	public void sitesKeepTheirChambersConnected(){
		int n = 0;
		for (int a = -1; a >= -WorldLayers.MAX_BELOW && n < 30; a -= 2){
			for (CaveSites.Site s : sample( a )){
				if (n++ >= 30) break;
				int[] o = originFor( s );
				WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, o[0], o[1], 0f );
				int bx = s.x0 - 2 - o[0], by = s.y0 - 2 - o[1], bw = s.x1 - s.x0 + 5, bh = s.y1 - s.y0 + 5;
				boolean[] before = new boolean[bw * bh], after = new boolean[bw * bh];
				flood( before, bx, by, bw, bh, s, o, null );
				flood( after, bx, by, bw, bh, s, o, w.terrain );
				for (int y = 0; y < bh; y++){
					for (int x = 0; x < bw; x++){
						int wx = o[0] + bx + x, wy = o[1] + by + y;
						if (s.type == CaveSites.Type.TOMB && Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ) <= 3) continue;
						int t = w.terrain[(bx + x) + (by + y) * W];
						if (before[x + y * bw] && (Terrain.flags[t] & Terrain.PASSABLE) != 0){
							assertTrue( s.type + " cut " + wx + "," + wy + " off", after[x + y * bw] );
						}
					}
				}
			}
		}
		assertTrue( n >= 20 );
	}

	//what the stand cell reaches within the box: by the natural cave (map null) or the window
	private static void flood( boolean[] r, int bx, int by, int bw, int bh, CaveSites.Site s, int[] o, int[] map ){
		ArrayDeque<int[]> q = new ArrayDeque<>();
		q.add( new int[]{ s.standX - o[0] - bx, s.standY - o[1] - by } );
		r[q.peek()[0] + q.peek()[1] * bw] = true;
		while (!q.isEmpty()){
			int[] p = q.poll();
			for (int[] d : new int[][]{ { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }){
				int x = p[0] + d[0], y = p[1] + d[1];
				if (x < 0 || y < 0 || x >= bw || y >= bh || r[x + y * bw]) continue;
				int wx = o[0] + bx + x, wy = o[1] + by + y;
				boolean ok;
				if (map == null){
					int t = WindowGenerator.naturalCaveTerrain( SEED, s.altitude, wx, wy );
					ok = (Terrain.flags[t] & Terrain.PASSABLE) != 0 && t != Terrain.CHASM;
				} else {
					ok = (Terrain.flags[map[(bx + x) + (by + y) * W]] & Terrain.PASSABLE) != 0;
				}
				if (!ok) continue;
				r[x + y * bw] = true;
				q.add( new int[]{ x, y } );
			}
		}
	}

	@Test
	public void coldCacheTimingStaysStreamable(){
		for (int a : new int[]{ -1, -6, -12 }){
			int ox = 15000 + 72 * 5 * a, oy = -15000;
			//everything warm but the places: the window once, then the places forgotten
			WindowGenerator.generate( SEED, a, ox, oy, 0f );
			long cold = Long.MAX_VALUE, warm = Long.MAX_VALUE;
			for (int i = 0; i < 3; i++){
				CaveSites.checkSeed( SEED ^ 1 );
				CaveSites.checkSeed( SEED );
				long t0 = System.nanoTime();
				WindowGenerator.generate( SEED, a, ox, oy, 0f );
				cold = Math.min( cold, System.nanoTime() - t0 );
				t0 = System.nanoTime();
				WindowGenerator.generate( SEED, a, ox, oy, 0f );
				warm = Math.min( warm, System.nanoTime() - t0 );
			}
			System.out.println( "slice " + a + ": places cold " + cold / 1000000 + " ms, warm " + warm / 1000000 + " ms" );
			assertTrue( "slice " + a + ": cold " + cold / 1000000 + " ms against " + warm / 1000000,
					cold <= 1.5 * warm + 5_000_000L );
		}
	}

	// ------------------------------------------------------------ the tomb

	@Test
	public void tombIsSealed(){
		CaveSites.Site t = one( CaveSites.Type.TOMB );
		assertTrue( CaveSites.tombWall( SEED, t.altitude, t.cx + 3, t.cy ) );
		assertTrue( CaveSites.tombWall( SEED, t.altitude, t.cx, t.cy + 3 ) );
		assertFalse( CaveSites.tombWall( SEED, t.altitude, t.cx, t.cy ) );
		assertTrue( CaveSites.tombBox( SEED, t.altitude, t.cx + 2, t.cy - 2 ) );
		assertFalse( CaveSites.tombBox( SEED, t.altitude, t.cx + 4, t.cy ) );
		//nothing comes down into it from above: no pit, no ladder, no shaft lands near it
		for (int dy = -6; dy <= 6; dy++){
			for (int dx = -6; dx <= 6; dx++){
				assertFalse( WindowGenerator.wayAt( SEED, t.altitude, t.cx + dx, t.cy + dy ) );
				assertFalse( WindowGenerator.pitFromAbove( SEED, t.altitude, t.cx + dx, t.cy + dy ) );
			}
		}
		OverworldLevel ow = levelAt( t );
		int door = ow.localCell( t.cx, t.cy + 3 ), ring = ow.localCell( t.cx - 3, t.cy + 3 );
		assertEquals( Terrain.LOCKED_DOOR, ow.map[door] );
		assertEquals( Terrain.WALL, ow.map[ring] );
		assertEquals( Terrain.EMPTY_SP, ow.map[ow.localCell( t.cx + 1, t.cy + 1 )] );
		assertTrue( ow.unbreakable( ring ) );
		assertTrue( ow.unbreakable( door ) );
		assertFalse( ow.unbreakable( ow.localCell( t.cx, t.cy ) ) );
		//an ordinary rock wall of the slice still breaks
		int rock = -1;
		for (int c = W + 1; c < W * (H - 1) && rock == -1; c++){
			if (ow.map[c] == Terrain.WALL && !ow.unbreakable( c )) rock = c;
		}
		assertNotEquals( -1, rock );
		assertFalse( ow.unbreakable( rock ) );
		//a bomb beside the wall and beside the door leaves both standing
		//(nobody sees it go off: a test has no scene to draw the blast on)
		for (int at : new int[]{ ow.localCell( t.cx - 2, t.cy + 4 ), ow.localCell( t.cx, t.cy + 4 ) }){
			Arrays.fill( ow.heroFOV, false );
			hero.pos = ow.localCell( t.cx + 5, t.cy + 5 );
			new Bomb().explode( at );
		}
		assertEquals( Terrain.LOCKED_DOOR, ow.map[door] );
		assertEquals( Terrain.WALL, ow.map[ring] );
		assertEquals( Terrain.WALL, ow.map[ow.localCell( t.cx - 1, t.cy + 3 )] );
		//and the slice above will not dig down onto it
		Dungeon.depth = WorldLayers.depthOf( t.altitude + 1 );
		int[] o = originFor( t );
		WindowGenerator.Window up = WindowGenerator.generate( SEED, t.altitude + 1, o[0], o[1], 0f );
		OverworldLevel above = OverworldLevel.forNetwork( t.altitude + 1, SEED, o[0], o[1], 0f,
				xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.season(), up.terrain, W, H );
		assertTrue( above.sealedBelow( above.localCell( t.cx, t.cy ) ) );
		assertTrue( above.sealedBelow( above.localCell( t.cx + 3, t.cy - 3 ) ) );
		assertFalse( above.sealedBelow( above.localCell( t.cx + 4, t.cy ) ) );
	}

	// ------------------------------------------------------------ peopling

	@Test
	public void onceOnlyLootSurvivesASave() throws Exception {
		CaveSites.Site m = one( CaveSites.Type.MINE );
		OverworldLevel ow = levelAt( m );
		Dungeon.cycleTurn = 10 * DayNightCycle.FULL_CYCLE + 100;
		ow.populateLayerSites();
		Heap loot = ow.heaps.get( ow.localCell( m.lootX, m.lootY ) );
		assertNotNull( "the mine's cache", loot );
		boolean ore = false;
		for (Item it : loot.items) ore |= it instanceof Ore;
		assertTrue( ore );
		assertTrue( count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton.class ) >= 1 );
		assertEquals( 1, count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class ) );

		OverworldLevel back = saveAndLoad( ow );
		back.heaps.clear();
		back.mobs.clear();
		back.populateLayerSites();
		assertNull( "the cache is laid once ever", back.heaps.get( back.localCell( m.lootX, m.lootY ) ) );
		//nor do the guards come back with no cache to keep, not even the next day
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		back.populateLayerSites();
		assertEquals( 0, count( back, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class ) );

		//the tomb's key and chest, likewise
		CaveSites.Site t = one( CaveSites.Type.TOMB );
		OverworldLevel tomb = levelAt( t );
		tomb.populateLayerSites();
		Heap key = tomb.heaps.get( tomb.localCell( t.keyX, t.keyY ) );
		Heap chest = tomb.heaps.get( tomb.localCell( t.lootX, t.lootY ) );
		assertNotNull( key );
		assertNotNull( chest );
		assertEquals( Heap.Type.SKELETON, key.type );
		assertEquals( Heap.Type.CHEST, chest.type );
		boolean iron = false;
		for (Item it : key.items) iron |= it instanceof xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey
				&& ((xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey) it).depth == WorldLayers.depthOf( t.altitude );
		assertTrue( "the key opens this slice's doors", iron );
		assertEquals( 1, count( tomb, xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich.class ) );
		OverworldLevel tombBack = saveAndLoad( tomb );
		tombBack.heaps.clear();
		tombBack.populateLayerSites();
		assertNull( tombBack.heaps.get( tombBack.localCell( t.keyX, t.keyY ) ) );
		assertNull( tombBack.heaps.get( tombBack.localCell( t.lootX, t.lootY ) ) );
	}

	@Test
	public void guardsComeBackWhileTheirCacheLies() throws Exception {
		CaveSites.Site m = one( CaveSites.Type.MINE );
		OverworldLevel ow = levelAt( m );
		Dungeon.cycleTurn = 20 * DayNightCycle.FULL_CYCLE + 100;
		ow.populateLayerSites();
		int skeletons = count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class );
		assertEquals( 1, skeletons );
		//the same day nothing more comes
		ow.populateLayerSites();
		assertEquals( 1, count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class ) );
		//the guards are parked as the window leaves them, and pruned as stale wildlife
		for (Mob g : ow.mobs.toArray( new Mob[0] )){
			invoke( ow, "park", new Class<?>[]{ Mob.class, int.class, int.class }, g,
					ow.worldX() + g.pos % W, ow.worldY() + g.pos / W );
			ow.mobs.remove( g );
		}
		xyz.gabriwar.warpedpixeldungeon.Statistics.duration += 3 * DayNightCycle.FULL_CYCLE;
		invoke( ow, "pruneParked", new Class<?>[]{ int.class, int.class }, m.cx, m.cy );
		assertEquals( 0, count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class ) );
		//...and the next day, the cache still lying there, they are back
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		ow.populateLayerSites();
		assertEquals( 1, count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class ) );
		//once the cache is taken they come back no more
		ow.heaps.remove( ow.localCell( m.lootX, m.lootY ) );
		for (Mob g : ow.mobs.toArray( new Mob[0] )) ow.mobs.remove( g );
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		ow.populateLayerSites();
		assertEquals( 0, count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class ) );
	}

	//a shrine's fish is set in its pool once: parked, it outlasts any trip, however long or far
	@Test
	public void aShrinesFishWaitsOutAnyTrip(){
		CaveSites.Site shrine = one( CaveSites.Type.SHRINE );
		OverworldLevel ow = levelAt( shrine );
		ShrinePiranha fish = new ShrinePiranha();
		invoke( ow, "park", new Class<?>[]{ Mob.class, int.class, int.class }, fish, shrine.cx, shrine.cy );
		xyz.gabriwar.warpedpixeldungeon.Statistics.duration += 3 * DayNightCycle.FULL_CYCLE;
		invoke( ow, "pruneParked", new Class<?>[]{ int.class, int.class }, shrine.cx + 4 * W, shrine.cy - 4 * H );
		@SuppressWarnings("unchecked")
		java.util.HashMap<Long, Mob> parked = (java.util.HashMap<Long, Mob>) field( ow, "parkedMobs" );
		assertTrue( parked.containsValue( fish ) );
	}

	@Test
	public void keepersStandAtTheirPlaces(){
		CaveSites.Site camp = one( CaveSites.Type.CAMP );
		OverworldLevel ow = levelAt( camp );
		ow.populateLayerSites();
		assertEquals( 1, count( ow, CaveMiner.class ) );
		ow.populateLayerSites();
		assertEquals( "one trader a camp", 1, count( ow, CaveMiner.class ) );
		//the camp's fire smokes north, and nobody's post is north of it
		assertTrue( camp.npcY >= camp.cy || camp.npcX != camp.cx );

		CaveSites.Site shrine = one( CaveSites.Type.SHRINE );
		ow = levelAt( shrine );
		ow.populateLayerSites();
		assertEquals( 1, count( ow, LakeShrine.class ) );
		assertTrue( count( ow, ShrinePiranha.class ) <= 1 );

		CaveSites.Site rift = one( CaveSites.Type.RIFT );
		ow = levelAt( rift );
		ow.populateLayerSites();
		assertEquals( 1, count( ow, DwarvenForge.class ) );
		assertTrue( count( ow, RiftElemental.class ) >= 2 );
		//no wildlife within a place's berth (a place's own guards are of the wildlife's kinds:
		//a grotto's spinners keep it)
		HashSet<Class<?>> guards = new HashSet<>();
		for (CaveSites.Guard g : CaveSites.Guard.values()) guards.add( g.cls );
		for (Mob mob : ow.mobs){
			if (OverworldFauna.isFauna( mob ) && !guards.contains( mob.getClass() )){
				assertFalse( CaveSites.inBerth( ow.layerSites(), ow.worldX() + mob.pos % W, ow.worldY() + mob.pos / W ) );
			}
		}
	}

	@Test
	public void everySpriteReachesAGuest() throws Exception {
		ArrayList<Mob> made = new ArrayList<>();
		for (CaveSites.Type t : CaveSites.Type.values()){
			OverworldLevel ow = levelAt( one( t ) );
			Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
			ow.populateLayerSites();
			made.addAll( ow.mobs );
		}
		for (CaveSites.Guard g : CaveSites.Guard.values()) made.add( (Mob) g.cls.getConstructor().newInstance() );
		//and the wildlife the slices roll, on every kind of ground
		int every = 0;
		for (OverworldFauna.Habitat h : OverworldFauna.Habitat.values()) every |= h.bit;
		for (int a : new int[]{ 0, 3, -1, -6, -12 }){
			for (WorldModel.Biome b : WorldModel.Biome.values()){
				for (DayNightCycle.Phase ph : DayNightCycle.Phase.values()){
					for (int i = 0; i < 20; i++) made.addAll( OverworldFauna.roll( a, b, ph, every ) );
				}
			}
		}
		for (Mob mob : made){
			String wire = NetSprites.wireName( mob.spriteClass );
			Class<?> back = Class.forName( NetSprites.PACKAGE + wire );
			assertEquals( mob.spriteClass, back );
			assertTrue( CharSprite.class.isAssignableFrom( back ) );
			assertNotNull( back.getConstructor() );
		}
		//the rift's forge glows from its sprite's own constructor, the one a guest builds by name
		assertEquals( xyz.gabriwar.warpedpixeldungeon.sprites.DwarvenForgeSprite.class, new DwarvenForge().spriteClass );
		assertTrue( xyz.gabriwar.warpedpixeldungeon.sprites.TemperingAnvilSprite.class.isAssignableFrom( new DwarvenForge().spriteClass ) );
		for (Method m : DwarvenForge.class.getDeclaredMethods()) assertNotEquals( "the forge sets no face of its own", "sprite", m.getName() );
		//a top-level sprite's name is as it always was
		assertEquals( "RatSprite", NetSprites.wireName( xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite.class ) );
		assertEquals( "CrystalWispSprite$Blue", NetSprites.wireName( xyz.gabriwar.warpedpixeldungeon.sprites.CrystalWispSprite.Blue.class ) );
	}

	// ------------------------------------------------------------ the trade

	@Test
	public void minerPaysMoreThanAShop(){
		for (Item it : new Item[]{ new IronOre().quantity( 7 ), new Deepsilver().quantity( 3 ), new xyz.gabriwar.warpedpixeldungeon.items.ore.Diamond() }){
			assertEquals( Math.round( it.value() * 1.5f ), CaveMiner.orePrice( it ) );
			assertTrue( CaveMiner.orePrice( it ) > it.value() );
			assertTrue( CaveMiner.minersGoods( it ) );
		}
		assertFalse( CaveMiner.minersGoods( new Pickaxe() ) );
		//the caves never close for the night; the surface does
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		Dungeon.depth = WorldLayers.depthOf( -5 );
		assertFalse( Shopkeeper.closedForNight() );
		Dungeon.depth = OverworldLevel.DEPTH;
		assertTrue( Shopkeeper.closedForNight() );
	}

	@Test
	public void minerSellsAndStocksSupplies() throws Exception {
		CaveSites.Site camp = one( CaveSites.Type.CAMP );
		OverworldLevel ow = levelAt( camp );
		CaveMiner miner = new CaveMiner();
		Method src = CaveMiner.class.getDeclaredMethod( "stockSource" );
		src.setAccessible( true );
		@SuppressWarnings("unchecked") ArrayList<Item> stock = (ArrayList<Item>) src.invoke( miner );
		assertTrue( stock.size() <= 4 );
		boolean pick = false;
		for (Item it : stock){
			assertFalse( it instanceof Ore || it instanceof Gem );
			pick |= it instanceof Pickaxe;
		}
		assertTrue( "a pick for a hero without one", pick );
		new Pickaxe().collect( hero.belongings.backpack );
		@SuppressWarnings("unchecked") ArrayList<Item> again = (ArrayList<Item>) src.invoke( miner );
		for (Item it : again) assertFalse( it instanceof Pickaxe );
		//a sale pays his price into the purse and keeps nothing to buy back
		Item ore = new IronOre().quantity( 6 );
		assertTrue( ore.collect( hero.belongings.backpack ) );
		Item held = hero.belongings.getItem( IronOre.class );
		Dungeon.gold = 0;
		int one = miner.sell( hero, held, false );
		assertEquals( Math.round( 10 * 1.5f ), one );
		assertEquals( 5, hero.belongings.getItem( IronOre.class ).quantity() );
		int rest = miner.sell( hero, hero.belongings.getItem( IronOre.class ), true );
		assertEquals( Math.round( 50 * 1.5f ), rest );
		assertNull( hero.belongings.getItem( IronOre.class ) );
		assertEquals( one + rest, Dungeon.gold );
		assertTrue( miner.buybackItems.isEmpty() );
		//"first_stock" round-trips, and its absence is false
		Bundle b = new Bundle();
		miner.openShop();
		miner.storeInBundle( b );
		CaveMiner back = new CaveMiner();
		back.restoreFromBundle( b );
		Field f = CaveMiner.class.getDeclaredField( "firstStock" );
		f.setAccessible( true );
		assertTrue( (Boolean) f.get( back ) );
		assertFalse( ow.mobs.contains( back ) );
	}

	@Test
	public void rumoursAreTrue(){
		CaveSites.Site camp = one( CaveSites.Type.CAMP );
		OverworldLevel ow = levelAt( camp );
		int cell = ow.localCell( camp.npcX, camp.npcY );
		String news = ow.caveRumours( cell );
		assertNotNull( news );
		assertFalse( news.contains( Messages.NO_TEXT_FOUND ) );
		//every place he names stands where he says, his own camp is not among them, and he names
		//the nearest of his slice first
		List<CaveSites.Site> named = CaveSites.rumoured( ow, cell );
		assertFalse( "he knows of something", named.isEmpty() );
		long last = -1;
		for (CaveSites.Site s : named){
			assertTrue( s != camp );
			assertTrue( s == CaveSites.site( SEED, s.altitude, s.sx, s.sy ) );
			assertTrue( s.altitude == camp.altitude || s.altitude == camp.altitude - 1 );
			String line = Messages.get( CaveMiner.class, s.altitude == camp.altitude ? "rumour_line" : "rumour_below",
					CaveSites.phrase( s.type ), CaveSites.direction( s.cx - camp.npcX, s.cy - camp.npcY ),
					CaveSites.distance( s.cx - camp.npcX, s.cy - camp.npcY ) );
			assertTrue( line, news.contains( line ) );
			if (s.altitude == camp.altitude){
				long d = (long)(s.cx - camp.npcX) * (s.cx - camp.npcX) + (long)(s.cy - camp.npcY) * (s.cy - camp.npcY);
				assertTrue( d >= last );
				last = d;
			}
		}
		//the compass and the distances
		assertEquals( Messages.get( CaveSites.class, "dir_n" ), CaveSites.direction( 0, -10 ) );
		assertEquals( Messages.get( CaveSites.class, "dir_se" ), CaveSites.direction( 10, 10 ) );
		assertEquals( Messages.get( CaveSites.class, "dir_w" ), CaveSites.direction( -10, 1 ) );
		assertEquals( Messages.get( CaveSites.class, "dist_near" ), CaveSites.distance( 47, 0 ) );
		assertEquals( Messages.get( CaveSites.class, "dist_mid" ), CaveSites.distance( 0, 48 ) );
		assertEquals( Messages.get( CaveSites.class, "dist_far" ), CaveSites.distance( 200, 3 ) );
		//asked again in the same window, the same answer without working it out again
		assertTrue( news == ow.caveRumours( cell ) );
	}

	@Test
	public void shrineTakesOneOfferingADay(){
		Dungeon.depth = WorldLayers.depthOf( -4 );
		Dungeon.cycleTurn = 30 * DayNightCycle.FULL_CYCLE + 10;
		LakeShrine shrine = new LakeShrine();
		assertFalse( shrine.blessedToday() );
		int price = LakeShrine.baseOffer( -4 ) * LakeShrine.TIERS[1];
		Dungeon.gold = price - 1;
		assertFalse( "too poor", shrine.pay( hero, 1 ) );
		assertEquals( price - 1, Dungeon.gold );
		Dungeon.gold = price + 50;
		assertTrue( shrine.pay( hero, 1 ) );
		assertEquals( 50, Dungeon.gold );
		assertNotNull( hero.buff( Bless.class ) );
		assertEquals( LakeShrine.TURNS[1], hero.buff( Bless.class ).cooldown(), 0.01f );
		assertTrue( shrine.blessedToday() );
		Dungeon.gold = 10000;
		assertFalse( "once a day", shrine.pay( hero, 0 ) );
		assertEquals( 10000, Dungeon.gold );
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		assertFalse( shrine.blessedToday() );
		assertTrue( shrine.pay( hero, 0 ) );
		//"last_offer" round-trips, and its absence is no offering ever
		Bundle b = new Bundle();
		shrine.storeInBundle( b );
		LakeShrine back = new LakeShrine();
		back.restoreFromBundle( b );
		assertEquals( WorldClock.day(), back.lastOffer );
		LakeShrine fresh = new LakeShrine();
		fresh.restoreFromBundle( new Bundle() );
		assertEquals( Integer.MIN_VALUE, fresh.lastOffer );
		//deeper pools ask more
		assertTrue( LakeShrine.baseOffer( -10 ) > LakeShrine.baseOffer( -2 ) );
	}

	@Test
	public void deepForgeSmeltsTheRarestInSmallerCrucibles(){
		Dungeon.level = levelAt( one( CaveSites.Type.RIFT ) );
		Dungeon.gold = 0;
		Ore ds = (Ore) new Deepsilver().quantity( 7 );
		assertTrue( ds.collect( hero.belongings.backpack ) );
		Ore held = hero.belongings.getItem( Deepsilver.class );
		assertTrue( DwarvenForge.canSmelt( hero ) );
		assertTrue( Blacksmith2.smeltInto( hero, held, true ) );
		assertNull( hero.belongings.getItem( Deepsilver.class ) );
		assertNotNull( hero.belongings.getItem( MasterworkCore.class ) );
		assertEquals( "no filings bought at the rift", 0, Dungeon.gold );
		//the troll still takes ten
		Ore more = (Ore) new Deepsilver().quantity( 7 );
		more.collect( hero.belongings.backpack );
		assertFalse( Blacksmith2.smeltInto( hero, hero.belongings.getItem( Deepsilver.class ) ) );
		//iron goes fifty to a core at both
		assertEquals( 50, Ores.Kind.IRON.forgeBatch() );
		Ore iron = (Ore) new IronOre().quantity( 10 );
		iron.collect( hero.belongings.backpack );
		assertFalse( Blacksmith2.smeltInto( hero, hero.belongings.getItem( IronOre.class ), true ) );
	}

	//the deep forges are no core faucet: one crucible a stack, one a day for the hero whichever
	//rift's forge he walks to - every rift has its own, and a walk from one to the next is no new day
	@Test
	public void deepForgeFiresOnceADay(){
		Dungeon.level = levelAt( one( CaveSites.Type.RIFT ) );
		hero.smeltDay = Integer.MIN_VALUE;
		Ore ds = (Ore) new Deepsilver().quantity( 21 );
		assertTrue( ds.collect( hero.belongings.backpack ) );
		assertTrue( Blacksmith2.pourCrucible( hero, hero.belongings.getItem( Deepsilver.class ), true ) );
		assertEquals( "one crucible, not three", 14, hero.belongings.getItem( Deepsilver.class ).quantity() );
		assertEquals( 1, hero.belongings.getItem( MasterworkCore.class ).quantity() );
		assertTrue( Blacksmith2.firedToday( hero ) );
		assertFalse( "cold till tomorrow", Blacksmith2.pourCrucible( hero, hero.belongings.getItem( Deepsilver.class ), true ) );
		assertEquals( 14, hero.belongings.getItem( Deepsilver.class ).quantity() );
		//the next world day it pours again
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		assertTrue( Blacksmith2.pourCrucible( hero, hero.belongings.getItem( Deepsilver.class ), true ) );
		assertEquals( 2, hero.belongings.getItem( MasterworkCore.class ).quantity() );
	}

	@Test
	public void oresSharesAreTheSlices(){
		assertEquals( Ores.Kind.COPPER, Ores.commonKind( -1 ) );
		assertEquals( Ores.Kind.IRON, Ores.commonKind( -5 ) );
		assertEquals( Ores.Kind.SILVER, Ores.commonKind( -11 ) );
		assertEquals( Ores.Kind.COPPER, Ores.richestKind( -1 ) );
		assertEquals( Ores.Kind.GOLD, Ores.richestKind( -6 ) );
		assertEquals( Ores.Kind.DEEPSILVER, Ores.richestKind( -12 ) );
		assertEquals( Ores.GemKind.AMETHYST, Ores.rollGem( -1, 0 ) );
		assertEquals( Ores.GemKind.DIAMOND, Ores.rollGem( -12, 99 ) );
		for (int a = -1; a >= -12; a--){
			for (int r = 0; r < 100; r += 7){
				Ores.GemKind g = Ores.rollGem( a, r );
				assertTrue( a > -4 ? g != Ores.GemKind.DIAMOND : true );
			}
		}
		Ore lumps = Ores.lumps( Ores.Kind.SILVER, 5 );
		assertEquals( 5, lumps.quantity() );
		assertEquals( Ores.Kind.SILVER, lumps.kind() );
		//the vein a miner names is there, and the richest within reach
		int[] v = Ores.notableVein( SEED, -6, 0, 0, 48 );
		assertNotNull( v );
		assertEquals( Ores.Kind.values()[v[2]], Ores.oreAt( SEED, -6, v[0], v[1] ) );
		assertTrue( Math.max( Math.abs( v[0] ), Math.abs( v[1] ) ) <= 48 );
	}

	// ------------------------------------------------------------ the rift

	@Test
	public void riftHarmStaysBounded(){
		CaveSites.Site rift = one( CaveSites.Type.RIFT );
		OverworldLevel ow = levelAt( rift );
		Dungeon.depth = WorldLayers.depthOf( -12 );
		//burning and heat on a slice are scaled by the slice, at most the halls' 30
		assertTrue( Dungeon.harmDepth() <= 30 );
		assertEquals( WorldLayers.statDepth( rift.altitude ), Dungeon.harmDepth() );
		Dungeon.level = null;
		assertEquals( Dungeon.scalingDepth(), Dungeon.harmDepth() );
		Dungeon.level = ow;
		//a burn there is at most 3 + 30/4, a flare at most 8: a hero on the crack takes no more
		//than 8 + 10 a turn from the rift itself
		assertTrue( 3 + Dungeon.harmDepth() / 4 + 8 <= 18 );
		//the heat it gives the air: the cells beside the crack and the forge stay under 45
		ArrayList<Integer> crack = new ArrayList<>();
		for (long k : rift.fissure){
			int c = ow.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) );
			if (c != -1) crack.add( c );
		}
		assertTrue( crack.size() >= 5 );
		for (int i = 0; i < 300; i++){
			for (int c : crack) xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature.depositHeat( c, CaveSites.RIFT_HEAT );
			xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature.stepDiffusion( ow );
		}
		int forge = ow.localCell( rift.npcX, rift.npcY );
		float hottest = xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature.tileTemp( forge );
		for (int c : crack){
			for (int n : PathFinder.NEIGHBOURS8){
				if (crack.contains( c + n ) || !ow.passable[c + n]) continue;
				hottest = Math.max( hottest, xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature.tileTemp( c + n ) );
			}
		}
		assertTrue( "beside the crack " + hottest + " C", hottest < 45f );
		//the guards keep the burning, whose damage now reads the slice: fixed strength regardless
		Dungeon.depth = WorldLayers.depthOf( -10 );
		RiftElemental e = new RiftElemental();
		assertEquals( 83, e.HT );
		for (int i = 0; i < 200; i++) assertTrue( e.damageRoll() <= 34 );
		ShrinePiranha p = new ShrinePiranha();
		assertEquals( 30 + 5 * 10, p.HT );
		for (int i = 0; i < 200; i++) assertTrue( p.damageRoll() <= 10 + 2 * 10 );
	}

	// ------------------------------------------------------------ saves and names

	@Test
	public void foundPlacesAreKeptAndOldSavesLoad() throws Exception {
		CaveSites.Site g = one( CaveSites.Type.GROTTO );
		OverworldLevel ow = levelAt( g );
		int see = ow.localCell( g.standX, g.standY );
		ow.visited[see] = true;
		ow.discover( g.key, g.type.ordinal(), g.standX, g.standY, new int[]{ g.x0, g.y0, g.x1, g.y1 }, CaveSites.foundLine( g ) );
		assertEquals( 1, ow.foundSites().length );
		assertEquals( g.key, ow.foundSites()[0].key );
		String place = ow.placeNameAt( ow.localCell( g.cx, g.cy ) );
		assertEquals( Messages.get( CaveSites.class, "place_site", CaveSites.mapName( g.type ), -g.altitude ), place );
		//and a cell far outside it is just the slice
		assertEquals( Messages.get( OverworldLevel.class, "place_below", -g.altitude ), ow.placeNameAt( W + 2 ) );
		Dungeon.cycleTurn = 40 * DayNightCycle.FULL_CYCLE;
		assertTrue( ow.dueOnce( 77L, 1 ) );
		assertFalse( ow.dueOnce( 77L, 1 ) );
		OverworldLevel back = saveAndLoad( ow );
		assertEquals( 1, back.foundSites().length );
		assertEquals( g.type.ordinal(), back.foundSites()[0].kind );
		assertFalse( "the due record came back", back.dueOnce( 77L, 1 ) );
		//a bundle with none of the keys (an older save) loads with nothing found and nothing due
		Bundle b = new Bundle();
		ow.customTerrain = new ArrayList<>();
		ow.storeInBundle( b );
		b.remove( "layer_sites_found_at" );
		b.remove( "layer_sites_found_kind" );
		b.remove( "layer_due_keys" );
		b.remove( "layer_due_days" );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Actor.clear();
		OverworldLevel old = new OverworldLevel();
		old.restoreFromBundle( Bundle.read( new ByteArrayInputStream( out.toByteArray() ) ) );
		assertEquals( 0, old.foundSites().length );
		assertTrue( old.dueOnce( 77L, 1 ) );
		//a hero saved where a place's prop now stands is set down beside it
		Dungeon.level = null;
		CaveSites.Site camp = one( CaveSites.Type.CAMP );
		OverworldLevel c = levelAt( camp );
		int crates = -1;
		for (int i = 0; i < c.length(); i++) if (c.map[i] == Terrain.CUSTOM_DECO) crates = i;
		assertNotEquals( -1, crates );
		hero.pos = crates;
		Dungeon.depth = WorldLayers.depthOf( camp.altitude );
		OverworldLevel cBack = saveAndLoad( c );
		assertTrue( cBack.passable[hero.pos] );
	}

	@Test
	public void everyStringResolves(){
		ArrayList<String> keys = new ArrayList<>( Arrays.asList( "place_site", "dir_n", "dir_ne", "dir_e", "dir_se",
				"dir_s", "dir_sw", "dir_w", "dir_nw", "dist_near", "dist_mid", "dist_far", "tomb_unbreakable",
				"rift_flare", "rift_death", "rankings_desc" ) );
		for (CaveSites.Type t : CaveSites.Type.values()){
			keys.add( "place_" + t.key() );
			keys.add( "found_" + t.key() );
			keys.add( "rumour_" + t.key() );
		}
		for (CaveSites.Prop p : CaveSites.Prop.values()){
			keys.add( p.name );
			keys.add( p.name + "_desc" );
		}
		for (String k : keys) assertResolves( CaveSites.class, k );
		for (String k : new String[]{ "name", "desc", "greeting", "sell_ore", "sell_other", "news", "buyback", "ore_prompt",
				"ore_offer", "ore_all", "ore_one", "ore_cancel", "sold", "news_intro", "rumour_line", "rumour_below",
				"rumour_vein", "rumour_none", "talk" }) assertResolves( CaveMiner.class, k );
		for (String k : new String[]{ "name", "desc", "prompt", "offer", "leave", "blessed_small", "blessed_mid",
				"blessed_large", "already", "poor", "net_host_only" }) assertResolves( LakeShrine.class, k );
		for (String k : new String[]{ "name", "desc", "prompt", "smelt", "leave", "smelt_prompt", "short", "smelted",
				"cold", "nothing", "net_host_only" }) assertResolves( DwarvenForge.class, k );
		//the place's creatures borrow their kin's names
		assertResolves( RiftElemental.class, "name" );
		assertResolves( ShrinePiranha.class, "name" );
	}

	private static void assertResolves( Class<?> cls, String key ){
		String s = Messages.get( cls, key, 1, 2, 3 );
		assertFalse( cls.getSimpleName() + "." + key, s.contains( Messages.NO_TEXT_FOUND ) );
	}

	@Test
	public void lightsBurnOnlyWhereThePropStands(){
		CaveSites.Site camp = one( CaveSites.Type.CAMP );
		OverworldLevel ow = levelAt( camp );
		ArrayList<SettlementAmbience.Light> lights = new ArrayList<>();
		ArrayList<SettlementAmbience.Smoke> smoke = new ArrayList<>();
		CaveSites.collectLights( ow, hero.pos, new HashSet<>(), lights, smoke, new HashSet<>() );
		assertEquals( 3, lights.size() );
		assertEquals( 1, smoke.size() );
		for (SettlementAmbience.Light l : lights) assertEquals( 1f, l.level, 0f );
		//the fire put out (its embers dug up), its light and smoke go
		ow.map[ow.localCell( camp.cx, camp.cy )] = Terrain.EMPTY;
		lights.clear();
		smoke.clear();
		CaveSites.collectLights( ow, hero.pos, new HashSet<>(), lights, smoke, new HashSet<>() );
		assertEquals( 2, lights.size() );
		assertEquals( 0, smoke.size() );
	}

	@Test
	public void litPlacesAreSeenFromAfar(){
		CaveSites.Site camp = one( CaveSites.Type.CAMP );
		OverworldLevel ow = levelAt( camp );
		//a hero twelve cells off, beyond his eight cells of sight, with a clear line to the fire
		int fire = ow.localCell( camp.cx, camp.cy );
		int from = -1;
		for (int d = 12; d >= 10 && from == -1; d--){
			for (int[] o : new int[][]{ { 0, d }, { d, 0 }, { -d, 0 }, { 0, -d } }){
				int c = ow.localCell( camp.cx + o[0], camp.cy + o[1] );
				if (c != -1 && ow.passable[c]){
					boolean[] fov = new boolean[ow.length()];
					hero.pos = c;
					hero.viewDistance = ow.viewDistance;
					ow.updateFieldOfView( hero, fov );
					if (fov[fire]){
						from = c;
						break;
					}
				}
			}
		}
		assertNotEquals( "the camp's fire is seen from beyond eight cells", -1, from );
		//...and Dungeon.observe writes that far ground into the explored map, which the fog is
		//painted from - not only into his sight
		hero.pos = from;
		java.util.Arrays.fill( ow.visited, false );
		Dungeon.observe();
		assertTrue( "the lit camp is explored from afar", ow.visited[fire] );
		assertTrue( ow.heroFOV[fire] );
	}

	//Ores' rock memo reads the natural rock the window's row pass recorded (Window.rock), never the
	//terrain a place laid: inside a mine's gallery, a tomb, a crystal cavern and round a shaft's
	//bottom it is still the world's rock
	@Test
	public void oreMemoAgreesInsidePlaces(){
		CaveSites.Site m = shaftMine();
		ArrayList<int[]> windows = new ArrayList<>();
		int[] o = originFor( m );
		windows.add( new int[]{ m.altitude, o[0], o[1] } );
		windows.add( new int[]{ m.altitude - 1, o[0], o[1] } );
		for (CaveSites.Type t : new CaveSites.Type[]{ CaveSites.Type.TOMB, CaveSites.Type.CRYSTAL, CaveSites.Type.RIFT }){
			CaveSites.Site s = one( t );
			int[] so = originFor( s );
			windows.add( new int[]{ s.altitude, so[0], so[1] } );
		}
		for (int[] win : windows){
			WindowGenerator.Window w = WindowGenerator.generate( SEED, win[0], win[1], win[2], 0f );
			Ores.Rock hinted = new Ores.Rock( SEED, win[0], win[1], win[2], W, H ).over( w.rock, win[1], win[2] );
			for (int y = 0; y < H; y++){
				for (int x = 0; x < W; x++){
					assertEquals( "slice " + win[0] + " at " + x + "," + y, Ores.naturalRock( SEED, win[0], win[1] + x, win[2] + y ),
							hinted.at( win[1] + x, win[2] + y ) );
				}
			}
		}
	}

	@Test
	public void timbersNeverHangOverTheLane(){
		for (int a = -1; a >= -6; a--){
			for (CaveSites.Site s : sample( a )){
				if (s.type != CaveSites.Type.MINE) continue;
				for (java.util.Map.Entry<Long, CaveSites.Prop> e : s.props.entrySet()){
					if (e.getValue() != CaveSites.Prop.TIMBER) continue;
					long above = e.getKey() - (1L << 32);
					CaveSites.Prop under = s.props.get( above );
					assertTrue( "a timber's top over the rails", under == null || under == CaveSites.Prop.TIMBER );
				}
			}
		}
	}

	//the cave field's own terrain of a cell, as the places are judged against it
	private static int natural( int a, int wx, int wy ){
		return WorldModel.caveTerrain( SEED, wx, wy, a, WorldModel.caveSample( SEED, wx, wy, a, null ) );
	}

	//a timber holds up a roof: beyond it, away from the rails, the natural rock it props
	@Test
	public void timbersStandUnderTheRock(){
		int timbers = 0;
		for (int a = -1; a >= -6; a--){
			for (CaveSites.Site s : sample( a )){
				if (s.type != CaveSites.Type.MINE) continue;
				for (java.util.Map.Entry<Long, CaveSites.Prop> e : s.props.entrySet()){
					if (e.getValue() != CaveSites.Prop.TIMBER) continue;
					int tx = (int)(e.getKey() & 0xFFFFFFFFL), ty = (int)(e.getKey() >> 32);
					int[] rail = null;
					for (int[] d : new int[][]{ { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }){
						CaveSites.Prop p = s.props.get( OverworldLevel.worldKey( tx + d[0], ty + d[1] ) );
						if (p != null && p.name.equals( "prop_rails" )) rail = d;
					}
					assertNotNull( "a timber beside its gallery's rails", rail );
					int bx = tx - rail[0], by = ty - rail[1];
					assertEquals( "rock beyond the timber at " + tx + "," + ty + " on " + a, Terrain.WALL, natural( a, bx, by ) );
					assertFalse( WindowGenerator.wayAt( SEED, a, bx, by ) );
					timbers++;
				}
			}
		}
		assertTrue( "timbers stand somewhere: " + timbers, timbers >= 5 );
	}

	//a rift's crack runs straight across its chamber, east-west or north-south, into the rock at
	//both ends, every cell of it drawn with its axis' own tiles (they join edge to edge)
	@Test
	public void riftCracksRunWallToWall(){
		int rifts = 0;
		for (int a = OverworldFauna.DEEP_CAVES; a >= -WorldLayers.MAX_BELOW; a--){
			for (CaveSites.Site s : sample( a )){
				if (s.type != CaveSites.Type.RIFT) continue;
				rifts++;
				ArrayList<int[]> line = new ArrayList<>();
				Boolean ew = null;
				for (java.util.Map.Entry<Long, CaveSites.Prop> e : s.props.entrySet()){
					CaveSites.Prop p = e.getValue();
					if (!p.name.equals( "prop_rift" )) continue;
					boolean thisEw = p == CaveSites.Prop.LAVA_EW_0 || p == CaveSites.Prop.LAVA_EW_1;
					if (ew == null) ew = thisEw;
					assertEquals( "one axis a crack", ew, thisEw );
					line.add( new int[]{ (int)(e.getKey() & 0xFFFFFFFFL), (int)(e.getKey() >> 32) } );
				}
				assertNotNull( ew );
				int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
				for (int[] c : line){
					assertEquals( "a straight crack", ew ? s.cy : s.cx, ew ? c[1] : c[0] );
					lo = Math.min( lo, ew ? c[0] : c[1] );
					hi = Math.max( hi, ew ? c[0] : c[1] );
				}
				assertEquals( "an unbroken crack", hi - lo + 1, line.size() );
				assertTrue( "a crack across a chamber: " + line.size(), line.size() >= 9 );
				int[] before = ew ? new int[]{ lo - 1, s.cy } : new int[]{ s.cx, lo - 1 };
				int[] after = ew ? new int[]{ hi + 1, s.cy } : new int[]{ s.cx, hi + 1 };
				for (int[] r : new int[][]{ before, after }){
					int t = natural( a, r[0], r[1] );
					assertTrue( "the crack ends in the rock on " + a, t == Terrain.WALL || t == Terrain.MINE_CRYSTAL );
				}
				//every cell of the crack and its scorched heart burns
				for (long k : s.fissure){
					int i = Arrays.binarySearch( sortedCells( s ), k );
					assertTrue( i >= 0 );
				}
			}
		}
		assertTrue( "rifts near the origin: " + rifts, rifts >= 5 );
	}

	private static long[] sortedCells( CaveSites.Site s ){
		long[] c = s.cells.clone();
		Arrays.sort( c );
		return c;
	}

	//a crystal cavern is a place of its own: shards on its floor glowing cold, lights on them, and
	//deep down a crystal guardian asleep by its cache
	@Test
	public void cavernsGlowAndDeepOnesAreGuarded(){
		boolean deep = false;
		for (int a = -3; a >= -WorldLayers.MAX_BELOW; a--){
			for (CaveSites.Site s : sample( a )){
				if (s.type != CaveSites.Type.CRYSTAL) continue;
				int shards = 0;
				for (CaveSites.Prop p : s.props.values()) if (p == CaveSites.Prop.SHARDS_0 || p == CaveSites.Prop.SHARDS_1) shards++;
				assertTrue( "shards in a cavern on " + a, shards >= 3 );
				int lit = 0;
				for (int i = 0; i < s.lights.length; i += 4){
					assertEquals( SettlementLights.KIND_SHARDS, s.lights[i + 2] );
					CaveSites.Prop p = s.props.get( OverworldLevel.worldKey( s.lights[i], s.lights[i + 1] ) );
					assertTrue( "a light on its shards", p == CaveSites.Prop.SHARDS_0 || p == CaveSites.Prop.SHARDS_1 );
					lit++;
				}
				assertTrue( lit >= 1 && lit <= 3 );
				boolean guardian = false;
				for (int i = 0; i < s.guards.length; i += 3) guardian |= s.guards[i + 2] == CaveSites.Guard.GUARDIAN.ordinal();
				assertEquals( "a guardian from " + CaveSites.GUARDIAN_DEPTH + " down, on " + a, a <= CaveSites.GUARDIAN_DEPTH, guardian );
				deep |= guardian;
			}
		}
		assertTrue( deep );
		CaveSites.Site s = null;
		for (int a = CaveSites.GUARDIAN_DEPTH; a >= -WorldLayers.MAX_BELOW && s == null; a--){
			for (CaveSites.Site c : sample( a )) if (c.type == CaveSites.Type.CRYSTAL){ s = c; break; }
		}
		OverworldLevel ow = levelAt( s );
		ow.populateLayerSites();
		assertEquals( 1, count( ow, xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalGuardian.class ) );
	}

	//every shrine has the deep water its tale promises, and its pale thing in it; a shrine whose
	//pool has no deep water today keeps the piranha's once-only key unspent
	@Test
	public void everyShrineHasItsPiranha(){
		int checked = 0;
		for (int a = -2; a >= -10 && checked < 3; a -= 3){
			for (CaveSites.Site s : sample( a )){
				if (s.type != CaveSites.Type.SHRINE) continue;
				OverworldLevel ow = levelAt( s );
				ow.populateLayerSites();
				assertEquals( "the shrine on " + a + " at " + s.cx + "," + s.cy, 1, count( ow, ShrinePiranha.class ) );
				checked++;
				break;
			}
		}
		assertTrue( checked >= 2 );
		CaveSites.Site s = one( CaveSites.Type.SHRINE );
		OverworldLevel ow = levelAt( s );
		Arrays.fill( ow.water, false );
		ow.populateLayerSites();
		assertEquals( 0, count( ow, ShrinePiranha.class ) );
		assertTrue( "the piranha's key is still unspent", ow.claimHoard( CaveSites.lootKey( s, CaveSites.PIRANHA ) ) );
	}

	//the surface's lakes flood the slices under them: no place lays a cell there, on any seed
	@Test
	public void placesStayOutOfTheLakes(){
		for (long seed : new long[]{ 777L, 12345L }){
			for (int a = -1; a >= -WorldModel.WATER_TIERS + 1; a--){
				for (int sy = -8; sy < 8; sy++){
					for (int sx = -12; sx < 4; sx++){
						CaveSites.Site s = CaveSites.site( seed, a, sx, sy );
						if (s == null) continue;
						for (long k : s.cells){
							int wx = (int)(k & 0xFFFFFFFFL), wy = (int)(k >> 32);
							assertFalse( s.type + " on " + a + " at " + wx + "," + wy + " (seed " + seed + ") under a lake",
									WindowGenerator.surfaceWaterTier( seed, wx, wy ) > -a );
						}
					}
				}
			}
		}
	}

	//an older save's edits never open a tomb: a ring cell dug out before the tomb stood there is
	//its wall again, a hero saved inside it is set down outside, and a heap or a mob left where a
	//prop or a wall stands now comes out onto open ground
	//a hero saved floating over a drop (levitating) is not a hero inside a wall: he stays over it
	@Test
	public void aHeroSavedOverADropStaysThere() throws Exception {
		CaveSites.Site t = one( CaveSites.Type.TOMB );
		OverworldLevel ow = levelAt( t );
		int over = hero.pos;
		ow.map[over] = Terrain.CHASM;
		OverworldLevel back = saveAndLoad( ow );
		assertEquals( Terrain.CHASM, back.map[over] );
		assertFalse( back.passable[over] );
		assertEquals( over, hero.pos );
	}

	@Test
	public void oldEditsNeverOpenATomb() throws Exception {
		CaveSites.Site t = one( CaveSites.Type.TOMB );
		OverworldLevel ow = levelAt( t );
		Dungeon.depth = WorldLayers.depthOf( t.altitude );
		int ring = ow.localCell( t.cx - 3, t.cy );
		int inside = ow.localCell( t.cx, t.cy );
		int coffin = ow.localCell( t.cx, t.cy - 1 );
		assertEquals( Terrain.WALL, ow.map[ring] );
		assertEquals( Terrain.CUSTOM_DECO, ow.map[coffin] );
		ow.map[ring] = Terrain.EMPTY_DECO;
		hero.pos = inside;
		Heap h = new Heap();
		h.pos = coffin;
		h.drop( new xyz.gabriwar.warpedpixeldungeon.items.Gold( 7 ) );
		ow.heaps.put( coffin, h );
		Mob rat = new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat();
		rat.pos = coffin;
		ow.mobs.add( rat );
		OverworldLevel back = saveAndLoad( ow );
		assertEquals( "the wall stands again", Terrain.WALL, back.map[ring] );
		@SuppressWarnings("unchecked")
		java.util.HashMap<Long, Integer> diffs = (java.util.HashMap<Long, Integer>) field( back, "diffs" );
		assertFalse( diffs.containsKey( OverworldLevel.worldKey( t.cx - 3, t.cy ) ) );
		assertFalse( "the hero is set down outside", Math.max( Math.abs( back.worldX() + hero.pos % W - t.cx ),
				Math.abs( back.worldY() + hero.pos / W - t.cy ) ) <= 3 );
		assertTrue( back.passable[hero.pos] );
		assertNull( back.heaps.get( coffin ) );
		boolean gold = false;
		for (Heap b : back.heaps.valueList()){
			assertFalse( back.solid[b.pos] );
			for (Item i : b.items) gold |= i instanceof xyz.gabriwar.warpedpixeldungeon.items.Gold && i.quantity() == 7;
		}
		assertTrue( "the heap came out", gold );
		for (Mob m : back.mobs) assertFalse( m.getClass().getSimpleName() + " in a solid cell", back.solid[m.pos] );

		//a door opened with its key stays open
		OverworldLevel o2 = levelAt( t );
		int door = o2.localCell( t.cx, t.cy + 3 );
		o2.map[door] = Terrain.DOOR;
		OverworldLevel b2 = saveAndLoad( o2 );
		assertEquals( Terrain.DOOR, b2.map[door] );
		hero.pos = b2.localCell( t.cx, t.cy );
		OverworldLevel b3 = saveAndLoad( b2 );
		assertEquals( "an open tomb keeps its visitor", b3.localCell( t.cx, t.cy ), hero.pos );

		//a parked heap coming back over a place's prop is set down beside it
		OverworldLevel o4 = levelAt( t );
		Heap parked = new Heap();
		parked.drop( new xyz.gabriwar.warpedpixeldungeon.items.Gold( 9 ) );
		@SuppressWarnings("unchecked")
		java.util.HashMap<Long, Heap> stored = (java.util.HashMap<Long, Heap>) field( o4, "storedHeaps" );
		stored.put( OverworldLevel.worldKey( t.cx, t.cy - 1 ), parked );
		invoke( o4, "restoreHeapsInWindow", new Class<?>[0] );
		assertTrue( stored.isEmpty() );
		assertFalse( o4.solid[parked.pos] );
		assertEquals( parked, o4.heaps.get( parked.pos ) );
	}
}
