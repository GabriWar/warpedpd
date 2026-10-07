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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Breathless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CaveIn;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A second look at the slices' dangers, written apart from HazardWatchTest: the firedamp mask is
 * the field's own on every deep slice and in every window (no window decides it), keeps off the
 * ways and does not turn with the seasons; a spent pocket and a crack stay put through a window
 * slide; a cave-in, wherever it is set off, keeps off pits, water, ice and the ways and leaves
 * the miner a cell to step to; a gust never puts anyone in a drop or moves him more than a cell;
 * the new buffs come back whole from a save.
 */
public class HazardsVerifyTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, depth, challenges;
	private float shift, temp, wind, windDir;
	private boolean shiftHeld;
	private Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		HazardWatchTest.boot();
	}

	@AfterClass
	public static void unboot() throws Exception {
		HazardWatchTest.unboot();
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		depth = Dungeon.depth;
		challenges = Dungeon.challenges;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		temp = ClimateManager.debugTempOverride;
		wind = ClimateManager.debugWindOverride;
		windDir = ClimateManager.debugWindDirOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.challenges = 0;
		hero = new Hero();
		hero.lvl = 12;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.depth = depth;
		Dungeon.challenges = challenges;
		ClimateManager.debugTempOverride = temp;
		ClimateManager.debugWindOverride = wind;
		ClimateManager.debugWindDirOverride = windDir;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	//as HazardWatchTest.windowAt: a mirror's window made the host's own
	private static OverworldLevel windowAt( int altitude, int wx, int wy, float s ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		host( ow );
		Dungeon.level = ow;
		Dungeon.depth = WorldLayers.depthOf( altitude );
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	private static void host( OverworldLevel ow ){
		set( ow, "network", false );
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( "currentStamp" );
			m.setAccessible( true );
			set( ow, "seasonStamp", m.invoke( ow ) );
		} catch (Exception e){
			throw new AssertionError( e );
		}
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

	//the window slid by (dx, dy) as a rebase leaves it: the same level and its HazardWatch, the
	//ground of the new origin (re-derived through the mirror path, then made the host's again)
	private static void slide( OverworldLevel ow, int dx, int dy, float s ){
		int nx = ow.worldX() + dx, ny = ow.worldY() + dy;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, ow.altitude(), nx, ny, s );
		assertTrue( ow.applyNetworkWindow( SEED, nx, ny, s, GameCalendar.season(), w.terrain ) );
		host( ow );
	}

	private void place( Hero h, int cell ){
		h.pos = cell;
		Actor.add( h );
	}

	private static boolean plainFloor( int t ){
		return t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.MUSHROOM_PATCH;
	}

	// ----------------------------------------------------------------- firedamp

	@Test
	public void firedampMaskIsTheFieldsOwnOnEveryDeepSlice(){
		WorldModel.CaveSample cs = new WorldModel.CaveSample();
		int total = 0;
		for (int alt = LayerHazards.FIREDAMP_TOP; WorldLayers.exists( alt ); alt--){
			for (int[] o : new int[][]{ { -88, -88 }, { 140, -260 } }){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				assertNotNull( "a mask at " + alt, w.firedamp );
				ArrayList<Integer> ways = new ArrayList<>();
				for (int c = 0; c < W * H; c++){
					if (w.link[c] != WindowGenerator.LINK_NONE || w.terrain[c] == Terrain.CHASM) ways.add( c );
				}
				for (int c = 0; c < W * H; c++){
					int wx = o[0] + c % W, wy = o[1] + c / W;
					WorldModel.caveSample( SEED, wx, wy, alt, cs );
					//the window's rim is walled after the mask is laid: gas there is never stood in
					//(gasAt asks passable), so only the inside has to be the pure rule
					boolean rim = c % W == 0 || c % W == W - 1 || c / W == 0 || c / W == H - 1;
					if (rim){
						assertTrue( !w.firedamp[c] || (Terrain.flags[w.terrain[c]] & Terrain.PASSABLE) == 0
								|| LayerHazards.gassy( SEED, wx, wy, alt, cs, w.terrain[c] ) );
						continue;
					}
					//the window's mask is the pure rule, cell for cell, but in and about a place (CaveSites)
					assertEquals( "cell " + wx + "," + wy + " at " + alt + " local " + (c % W) + "," + (c / W) + " t=" + w.terrain[c] + " field=" + WorldModel.caveTerrain( SEED, wx, wy, alt, cs ),
							LayerHazards.gassy( SEED, wx, wy, alt, cs, w.terrain[c] ) && !nearPlace( alt, wx, wy ), w.firedamp[c] );
					if (!w.firedamp[c]) continue;
					total++;
					assertTrue( "gas on plain floor only, not " + w.terrain[c], plainFloor( w.terrain[c] ) );
					for (int way : ways){
						int d = Math.max( Math.abs( way % W - c % W ), Math.abs( way / W - c / W ) );
						assertTrue( "gas " + d + " from a way at " + alt, d > LayerHazards.WAY_CLEARANCE );
					}
				}
			}
		}
		assertTrue( "some firedamp on the deep slices: " + total, total > 0 );
	}

	//within SITE_CLEARANCE of the reach of a place, or WAY_CLEARANCE of a mine's shaft, top or
	//foot: asked of the world, wherever the window stands
	private static boolean nearPlace( int alt, int wx, int wy ){
		int r = LayerHazards.SITE_CLEARANCE, d = LayerHazards.WAY_CLEARANCE;
		for (CaveSites.Site s : CaveSites.inRect( SEED, alt, wx - d, wy - d, wx + d, wy + d )){
			if (wx >= s.x0 - r && wx <= s.x1 + r && wy >= s.y0 - r && wy <= s.y1 + r) return true;
			if (s.hasShaft() && Math.abs( wx - s.shaftX ) <= d && Math.abs( wy - s.shaftY ) <= d) return true;
		}
		for (CaveSites.Site m : CaveSites.inRect( SEED, alt + 1, wx - d, wy - d, wx + d, wy + d )){
			if (m.hasShaft() && Math.abs( wx - m.shaftX ) <= d && Math.abs( wy - m.shaftY ) <= d) return true;
		}
		return false;
	}

	//the same world cell has the same gas in every window: a place or a shaft just past the rim
	//keeps its clearance inside it, as it does once the window has slid over it
	@Test
	public void firedampIsTheSameInEveryWindow(){
		int edges = 0;
		for (int alt = LayerHazards.FIREDAMP_TOP; WorldLayers.exists( alt ); alt--){
			int here = 0;
			for (CaveSites.Site s : CaveSites.inRect( SEED, alt, -600, -600, 600, 600 )){
				//one window with the place a cell past its west rim, one holding it whole
				int oy = s.y0 - 40;
				WindowGenerator.Window past = WindowGenerator.generate( SEED, alt, s.x1 + 1, oy, 0f );
				WindowGenerator.Window over = WindowGenerator.generate( SEED, alt, s.x1 + 1 - 64, oy, 0f );
				for (int y = 1; y < H - 1; y++){
					for (int x = 1; x <= LayerHazards.WAY_CLEARANCE + 1; x++){
						assertEquals( s.type + " at " + alt + " cell " + x + "," + y, over.firedamp[x + 64 + y * W], past.firedamp[x + y * W] );
					}
				}
				edges++;
				if (++here == 3) break;
			}
		}
		assertTrue( "places looked at: " + edges, edges > 0 );
	}

	//a fire anywhere on a pocket hisses to every hero within 12 (HazardWatch.arm), and the blast
	//takes the pocket and the cells beside it: no one the blast reaches may be out of earshot
	@Test
	public void everyoneTheBlastReachesHearsTheHiss(){
		int pockets = 0, widest = 0;
		for (int alt = LayerHazards.FIREDAMP_TOP; WorldLayers.exists( alt ); alt--){
			for (int[] o : new int[][]{ { -88, -88 }, { 140, -260 }, { -400, 300 } }){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				boolean[] seen = new boolean[W * H];
				for (int s = 0; s < W * H; s++){
					if (!w.firedamp[s] || seen[s]) continue;
					pockets++;
					int x0 = s % W, x1 = x0, y0 = s / W, y1 = y0;
					java.util.ArrayDeque<Integer> todo = new java.util.ArrayDeque<>();
					todo.add( s );
					seen[s] = true;
					while (!todo.isEmpty()){
						int c = todo.poll();
						x0 = Math.min( x0, c % W ); x1 = Math.max( x1, c % W );
						y0 = Math.min( y0, c / W ); y1 = Math.max( y1, c / W );
						for (int dy = -1; dy <= 1; dy++){
							for (int dx = -1; dx <= 1; dx++){
								int x = c % W + dx, y = c / W + dy;
								if (x < 0 || y < 0 || x >= W || y >= H) continue;
								int n = x + y * W;
								if (w.firedamp[n] && !seen[n]){
									seen[n] = true;
									todo.add( n );
								}
							}
						}
					}
					//from the farthest gas cell to the farthest cell beside the gas
					int reach = Math.max( x1 - x0, y1 - y0 ) + 1;
					widest = Math.max( widest, reach );
					assertTrue( "a pocket at " + alt + " near " + (o[0] + x0) + "," + (o[1] + y0) + " spans " + reach + " cells", reach <= 12 );
				}
			}
		}
		assertTrue( pockets > 0 );
	}

	@Test
	public void firedampDoesNotTurnWithTheSeasons(){
		float winter = WorldModel.calendarShift( GameCalendar.Season.WINTER, 0.5f );
		float summer = WorldModel.calendarShift( GameCalendar.Season.SUMMER, 0.5f );
		for (int alt : new int[]{ LayerHazards.FIREDAMP_TOP, -9 }){
			WindowGenerator.Window a = WindowGenerator.generate( SEED, alt, -88, -88, winter );
			WindowGenerator.Window b = WindowGenerator.generate( SEED, alt, -88, -88, summer );
			assertArrayEquals( "the caves keep no seasons at " + alt, a.firedamp, b.firedamp );
		}
	}

	@Test
	public void aSpentPocketStaysSpentThroughASlide(){
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		assertNotNull( f );
		OverworldLevel ow = windowAt( -6, f[0], f[1], 0f );
		int gas = ow.localCell( f[2], f[3] );
		assertTrue( ow.hazards().gasAt( gas ) );
		place( hero, ow.localCell( f[0], f[1] ) );
		ow.hazards().ignite( f[2], f[3] );
		assertFalse( ow.hazards().gasAt( gas ) );

		slide( ow, 32, -32, 0f );
		int moved = ow.localCell( f[2], f[3] );
		assertEquals( gas - 32 + 32 * W, moved );
		assertFalse( "spent where it lies, not where it lay", ow.hazards().gasAt( moved ) );
		//and the gas cell the old index names now is whatever the field says it is
		WorldModel.CaveSample cs = new WorldModel.CaveSample();
		int wx = ow.worldX() + gas % W, wy = ow.worldY() + gas / W;
		WorldModel.caveSample( SEED, wx, wy, -6, cs );
		boolean pure = LayerHazards.gassy( SEED, wx, wy, -6, cs, ow.map[gas] );
		boolean spentBlock = LayerHazards.pocketKey( wx, wy ) == LayerHazards.pocketKey( f[2], f[3] );
		if (!spentBlock) assertEquals( pure, ow.hazards().gasAt( gas ) );
	}

	// ----------------------------------------------------------------- thin ice

	@Test
	public void aCrackStaysPutThroughASlide(){
		float winter = WorldModel.calendarShift( GameCalendar.Season.WINTER, 0.5f );
		ClimateManager.debugTempOverride = -4f;
		int[] at = LayerHazards.findThinIce( SEED, 2, winter, -4f, 30, 8 );
		assertNotNull( at );
		OverworldLevel ow = windowAt( 2, at[0], at[1], winter );
		int land = W / 2 + (H / 2) * W, ice = -1;
		for (int n : new int[]{ land - W, land + 1, land + W, land - 1 }){
			if (LayerHazards.thinIce( ow.map, W, n, -4f )) ice = n;
		}
		assertTrue( ice != -1 );
		place( hero, ice );
		ow.hazards().stepped( hero );
		assertTrue( ow.hazards().crackedAt( ice ) );
		int wx = ow.worldX() + ice % W, wy = ow.worldY() + ice / W;
		hero.pos = land;

		slide( ow, -32, 32, winter );
		int c = ow.localCell( wx, wy );
		assertEquals( ice + 32 - 32 * W, c );
		assertTrue( ow.hazards().crackedAt( c ) );
		assertFalse( "the old index is other ice now", ow.hazards().crackedAt( ice ) && ice != c );
		//stepping on it there, after the slide, breaks it
		hero.pos = c;
		ow.hazards().stepped( hero );
		assertEquals( Terrain.WATER, ow.map[c] );
	}

	// ----------------------------------------------------------------- rockfall

	@Test
	public void aCaveInAnywhereKeepsOffTheWaysAndLeavesAWayOut(){
		Random rnd = new Random( 77 );
		int tried = 0, fell = 0;
		for (int alt : new int[]{ -2, -7, -12 }){
			OverworldLevel ow = windowAt( alt, 0, 0, 0f );
			for (int k = 0; k < 400; k++){
				int dst = (8 + rnd.nextInt( W - 16 )) + (8 + rnd.nextInt( H - 16 )) * W;
				if (!ow.passable[dst] || ow.pit[dst]) continue;
				//the miner beside the cell he mined, on ground he can stand on
				int stand = dst + PathFinder.NEIGHBOURS8[rnd.nextInt( 8 )];
				if (!ow.passable[stand] || ow.pit[stand]) continue;
				Actor.clear();
				for (CaveIn old : hero.buffs( CaveIn.class )) old.detach();
				place( hero, stand );
				ow.hazards().collapse( hero, dst, rnd.nextBoolean(), 1f );
				tried++;
				CaveIn fall = hero.buff( CaveIn.class );
				if (fall == null) continue;
				fell++;
				assertTrue( "his action comes first", fall.cooldown() + fall.rest >= 1f );
				int[] rocks = fall.rockPositions();
				for (int r : rocks){
					assertTrue( ow.passable[r] );
					assertFalse( "never on a pit", ow.pit[r] );
					assertFalse( "never on water", ow.water[r] );
					assertTrue( ow.map[r] != Terrain.FROZEN_WATER && ow.map[r] != Terrain.ENTRANCE && ow.map[r] != Terrain.EXIT );
					for (int n : new int[]{ r, r - W, r + 1, r + W, r - 1 }){
						assertEquals( "never on or beside a way", WindowGenerator.LINK_NONE, ow.linkAt( n ) );
					}
				}
				boolean out = false;
				for (int d : PathFinder.NEIGHBOURS8){
					int c = stand + d;
					boolean in = false;
					for (int r : rocks) in |= r == c;
					if (ow.passable[c] && !ow.pit[c] && !in) out = true;
				}
				boolean onHim = false;
				for (int r : rocks) onHim |= r == stand;
				assertTrue( "a cell to step to, or nothing on him at " + alt + " cell " + stand, out || !onHim );
			}
		}
		assertTrue( tried > 200 && fell > 100 );
	}

	@Test
	public void noRoofComesDownOffTheCaves(){
		for (int alt : new int[]{ 0, 3 }){
			OverworldLevel ow = windowAt( alt, 0, 0, 0f );
			int c = W / 2 + (H / 2) * W;
			place( hero, c - 1 );
			for (int i = 0; i < 200; i++) ow.hazards().mined( hero, c, Terrain.WALL );
			assertTrue( "no cave-in at " + alt, hero.buffs( CaveIn.class ).isEmpty() );
			Actor.clear();
		}
		assertEquals( 0f, LayerHazards.collapseChance( true, true, 0 ), 0f );
		for (int alt = -1; WorldLayers.exists( alt ); alt--){
			assertTrue( LayerHazards.collapseChance( true, true, alt ) <= 0.6f );
			assertTrue( LayerHazards.collapseChance( true, true, alt ) >= LayerHazards.collapseChance( true, true, alt + 1 ) );
		}
	}

	// ----------------------------------------------------------------- gusts

	@Test
	public void aGustNeverPutsAnyoneInADrop(){
		float winter = WorldModel.calendarShift( GameCalendar.Season.WINTER, 0.5f );
		ClimateManager.debugTempOverride = 0f;
		int[] r = LayerHazards.findRidge( SEED, 9, winter, 30, 8 );
		assertNotNull( r );
		OverworldLevel ow = windowAt( 9, r[0], r[1], winter );
		int c = W / 2 + (H / 2) * W;
		Random rnd = new Random( 9 );
		int ridges = 0, pushed = 0;
		for (int k = 0; k < 600; k++){
			for (int dy = -2; dy <= 2; dy++){
				for (int dx = -2; dx <= 2; dx++) Level.set( c + dx + dy * W, Terrain.EMPTY, ow );
			}
			for (int d : PathFinder.NEIGHBOURS8){
				if (rnd.nextInt( 3 ) != 0) Level.set( c + d, Terrain.CHASM, ow );
			}
			if (!LayerHazards.ridgeCell( ow.map, W, c )) continue;
			ridges++;
			Actor.clear();
			for (Buff b : hero.buffs()) b.detach();
			place( hero, c );
			ow.hazards().gustStrike( hero, rnd.nextInt( 8 ) );
			assertFalse( "into a drop", ow.pit[hero.pos] );
			assertTrue( ow.passable[hero.pos] );
			int moved = Math.max( Math.abs( hero.pos % W - c % W ), Math.abs( hero.pos / W - c / W ) );
			assertTrue( moved <= 1 );
			if (moved == 1) pushed++;
		}
		assertTrue( ridges > 100 && pushed > 10 );
	}

	// ----------------------------------------------------------------- thin air

	//resting beside a fire up high eases the breath; it must not flap between "the air is thin"
	//and "you can breathe freely" every couple of turns while he sits there
	@Test
	public void restingByAFireUpHighDoesNotFlapTheLog(){
		OverworldLevel high = windowAt( 9, 0, 0, 0f );
		int c = W / 2 + (H / 2) * W;
		place( hero, c );
		Level.set( c + 1, Terrain.EMBERS, high );
		hero.resting = true;
		final int[] onset = { 0 }, better = { 0 };
		String on = xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Breathless.class, "onset" );
		String off = xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Breathless.class, "better" );
		com.watabou.utils.Signal.Listener<String> l = line -> {
			if (line.contains( on )) onset[0]++;
			if (line.contains( off )) better[0]++;
			return false;
		};
		xyz.gabriwar.warpedpixeldungeon.utils.GLog.update.add( l );
		try {
			for (int t = 1; t <= 30; t++){
				high.hazards().heroTurnAt( hero, t, 0.99f );
				Breathless b = hero.buff( Breathless.class );
				if (b != null) b.act();
			}
		} finally {
			xyz.gabriwar.warpedpixeldungeon.utils.GLog.update.remove( l );
		}
		assertTrue( "30 turns resting by embers at +9: onset logged " + onset[0] + " times, better " + better[0],
				onset[0] <= 1 && better[0] <= 1 );
	}

	// ----------------------------------------------------------------- saves

	@Test
	public void theNewBuffsComeBackWholeFromASave() throws Exception {
		OverworldLevel ow = windowAt( 9, 0, 0, 0f );
		place( hero, W / 2 + (H / 2) * W );
		Breathless b = Buff.affect( hero, Breathless.class );
		for (int i = 0; i < LayerHazards.stackEvery( 9 ) * 2 + 3; i++) b.act();
		Bundle bb = new Bundle();
		b.storeInBundle( bb );
		Breathless back = new Breathless();
		back.restoreFromBundle( Bundle.read( new java.io.ByteArrayInputStream( bytes( bb ) ) ) );
		assertEquals( b.stacks, back.stacks );
		assertTrue( back.stacks >= 3 );

		CaveIn fall = new CaveIn();
		fall.depth = 104;
		fall.deep = 7;
		fall.rest = 0.5f;
		ArrayList<Integer> cells = new ArrayList<>( Arrays.asList( 500, 501, 677 ) );
		fall.setRockPositions( cells );
		Bundle fb = new Bundle();
		fall.storeInBundle( fb );
		CaveIn read = new CaveIn();
		read.restoreFromBundle( Bundle.read( new java.io.ByteArrayInputStream( bytes( fb ) ) ) );
		assertEquals( 104, read.depth );
		assertEquals( 7, read.deep );
		assertEquals( 0.5f, read.rest, 0f );
		assertArrayEquals( new int[]{ 500, 501, 677 }, read.rockPositions() );
	}

	private static byte[] bytes( Bundle b ){
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		return out.toByteArray();
	}
}
