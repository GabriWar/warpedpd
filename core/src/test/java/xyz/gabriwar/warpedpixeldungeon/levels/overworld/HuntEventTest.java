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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.badlogic.gdx.Gdx;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Signal;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The wolf packs' night hunts (HuntEvent) and the deer (Deer): on paper - a pure function of
 * the seed, the sector and the night, on wild open ground, rare, saying itself and never
 * listed for the world's own announcement - and on the ground: a hunt starts at dusk when the
 * hero comes near its ground, once, and winds down at dawn; the cull spares its beasts and the
 * parked store drops them, ending the hunt; a deer with no pack behind it makes good its escape
 * and is gone; a kill leaves meat and the pack feeding; and the deer graze the woods and
 * meadows by day besides.
 */
public class HuntEventTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int FC = WorldEvents.FC;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, startDay, challenges, depth, branch;
	private float precip;
	private DayNightCycle.Phase override;
	private Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		precip = ClimateManager.debugPrecipOverride;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = null;
		ClimateManager.debugPrecipOverride = 0f;
		hero = new Hero();
		hero.lvl = 12;
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		ClimateManager.debugPrecipOverride = precip;
		DayNightCycle.debugPhaseOverride = override;
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
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showAlert(){ }
			@Override public void hideAlert(){ }
		};
		sp.visible = false;
		return sp;
	}

	private static Level bareLevel( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public boolean waterCanFreeze(){ return true; }
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

	//an open 64x64 field, the hero in the middle of it
	private Level field(){
		Level l = bareLevel( 64, 64 );
		Painter.fill( l, 1, 1, 62, 62, Terrain.EMPTY );
		l.buildFlagMaps();
		Dungeon.level = l;
		hero.pos = 32 + 32 * 64;
		return l;
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

	private static Object invoke( Object o, Class<?> cls, String name, Class<?>[] types, Object... args ){
		try {
			Method m = cls.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static boolean act( Actor a ){
		return (Boolean) invoke( a, Actor.class, "act", new Class<?>[0] );
	}

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

	private static boolean said( ArrayList<String> lines, String text ){
		for (String l : lines) if (l.contains( text )) return true;
		return false;
	}

	//the live surface window round a world cell, built as a mirror's is (no populator) and made
	//the host's own, the hero seeing nothing
	private OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	private static ArrayList<Mob> tagged( Level l, long id ){
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : l.mobs){
			HuntPack p = m.buff( HuntPack.class );
			if (p != null && p.hunt == id) out.add( m );
		}
		return out;
	}

	// ------------------------------------------------------------------ the schedule

	@Test
	public void huntsArePureAndRare(){
		ArrayList<String> first = new ArrayList<>(), second = new ArrayList<>();
		int grounds = 0, groundNights = 0, hunts = 0;
		for (int sy = -10; sy <= 10; sy++){
			for (int sx = -10; sx <= 10; sx++){
				if (HuntEvent.ground( SEED, sx, sy )) grounds++;
				for (int n = 0; n < 200; n++){
					if (HuntEvent.ground( SEED, sx, sy )) groundNights++;
					if (HuntEvent.scheduled( SEED, sx, sy, n )){
						hunts++;
						first.add( sx + "," + sy + "@" + n );
					}
				}
			}
		}
		//another world in between: the grounds are worked out again
		HuntEvent.ground( SEED ^ 1, 0, 0 );
		WorldStructures.siteType( SEED ^ 1, 0, 0 );
		for (int n = 199; n >= 0; n--){
			for (int sy = 10; sy >= -10; sy--){
				for (int sx = 10; sx >= -10; sx--){
					if (HuntEvent.scheduled( SEED, sx, sy, n )) second.add( sx + "," + sy + "@" + n );
				}
			}
		}
		Collections.sort( first );
		Collections.sort( second );
		assertEquals( first, second );
		double rate = hunts / (double) groundNights;
		System.out.println( "[hunts] " + grounds + " hunting grounds in 441 sectors; " + hunts + " hunts in "
				+ groundNights + " ground-nights: one in " + Math.round( 1 / rate ) );
		assertTrue( "no hunting grounds", grounds > 0 );
		assertTrue( "hunts too common: " + rate, rate <= 1.0 / 6 );
		assertTrue( "hunts too rare: " + rate, rate >= 1.0 / 24 );

		//from a hero's side: a night's walk of six hundred cells, straight across the wilds at
		//a random bearing - how often does it pass close enough to a ground to set a hunt off?
		java.util.Random rng = new java.util.Random( 7 );
		int met = 0, walks = 2000;
		for (int k = 0; k < walks; k++){
			int n = 10 + rng.nextInt( 300 );
			double x = rng.nextInt( 2000 ) - 1000, y = rng.nextInt( 2000 ) - 1000, a = rng.nextDouble() * Math.PI * 2;
			boolean hit = false;
			for (int step = 0; step <= 600 && !hit; step += 4){
				int wx = (int) Math.round( x + Math.cos( a ) * step ), wy = (int) Math.round( y + Math.sin( a ) * step );
				int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
				for (int dy = -1; dy <= 1 && !hit; dy++){
					for (int dx = -1; dx <= 1 && !hit; dx++){
						if (!HuntEvent.scheduled( SEED, sx + dx, sy + dy, n )) continue;
						hit = Math.max( Math.abs( HuntEvent.anchorX( SEED, sx + dx, sy + dy ) - wx ),
								Math.abs( HuntEvent.anchorY( SEED, sx + dx, sy + dy ) - wy ) ) <= HuntEvent.TRIGGER;
					}
				}
			}
			if (hit) met++;
		}
		double every = walks / (double) met;
		System.out.println( "[hunts] a night walker meets one every " + Math.round( every * 10 ) / 10.0 + " nights" );
		assertTrue( "met too often: every " + every, every >= 4 );
		assertTrue( "met too seldom: every " + every, every <= 16 );
	}

	@Test
	public void huntingGroundsAreWildOpenGround(){
		int grounds = 0;
		for (int sy = -10; sy <= 10; sy++){
			for (int sx = -10; sx <= 10; sx++){
				if (!HuntEvent.ground( SEED, sx, sy )) continue;
				grounds++;
				int ax = HuntEvent.anchorX( SEED, sx, sy ), ay = HuntEvent.anchorY( SEED, sx, sy );
				assertTrue( HuntEvent.GROUNDS.contains( WorldModel.baseBiomeAt( SEED, ax, ay ) ) );
				assertFalse( "in a settlement's streets", OverworldFauna.nearSettlement( SEED, ax, ay ) );
				assertEquals( "outside its own sector", sx, Math.floorDiv( ax, WorldStructures.SECTOR ) );
				assertEquals( "outside its own sector", sy, Math.floorDiv( ay, WorldStructures.SECTOR ) );
				assertTrue( "on the town's doorstep", Math.max( Math.abs( ax ), Math.abs( ay ) ) >= HuntEvent.TOWN_CLEAR );
			}
		}
		assertTrue( grounds > 0 );
	}

	@Test
	public void aHuntSpansItsNightAndSaysItself(){
		int sx = 0, sy = 0, n = -1;
		for (int s = 0; s < 400 && n == -1; s++){
			int x = s % 20 - 10, y = s / 20 - 10;
			for (int k = 5; k < 300 && n == -1; k++){
				if (HuntEvent.scheduled( SEED, x, y, k )){
					sx = x;
					sy = y;
					n = k;
				}
			}
		}
		assertNotEquals( -1, n );
		WorldEvents.Event e = HuntEvent.huntEvent( SEED, sx, sy, n );
		assertNotNull( e );
		assertEquals( WorldEvents.Type.HUNT, e.type );
		assertEquals( WorldClock.nightStart( n ), e.startTurn );
		assertEquals( WorldClock.nightStart( n + 1 ), e.endTurn );
		assertEquals( n * FC + 500, e.startTurn );
		assertEquals( WorldEvents.idOf( WorldEvents.Type.HUNT, sx, sy, n ), e.id );
		assertEquals( HuntEvent.anchorX( SEED, sx, sy ), e.wx );
		//listed on its own day and into the next morning
		for (int d = n; d <= n + 1; d++){
			boolean listed = false;
			for (WorldEvents.Event o : WorldEvents.eventsNear( SEED, sx, sy, 1, d, null )) listed |= o.id == e.id;
			assertTrue( "day " + d, listed );
		}
		//it says itself, as a raid does: never one of the world's own announcements
		ArrayList<WorldEvents.Event> near = new ArrayList<>();
		near.add( e );
		WorldEvents.Event raid = WorldEvents.forcedRaid( SEED, sx, sy, n * FC + 900 );
		near.add( raid );
		assertTrue( WorldEvents.toAnnounce( near, new HashMap<>(), new HashMap<>(), sx, sy, n * FC + 2000,
				sx * WorldStructures.SECTOR - 40, sy * WorldStructures.SECTOR - 40, W, H ).isEmpty() );
		//a forced one has an id of its own
		WorldEvents.Event forced = WorldEvents.forcedHunt( e.wx, e.wy, n * FC + 1800, e.endTurn );
		assertNotEquals( e.id, forced.id );
		assertEquals( WorldEvents.idOf( WorldEvents.Type.RAID, sx, sy, n ), raid.id );
	}

	// ------------------------------------------------------------------ on the ground

	@Test
	public void aHuntStartsAtDuskAndEndsAtDawn(){
		//the hunts of the test seed, nearest the origin first
		ArrayList<int[]> candidates = new ArrayList<>();
		for (int r = 1; r <= 8 && candidates.size() < 12; r++){
			for (int sy = -r; sy <= r; sy++){
				for (int sx = -r; sx <= r; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r || !HuntEvent.ground( SEED, sx, sy )) continue;
					for (int n = 20; n < 400; n++){
						if (HuntEvent.scheduled( SEED, sx, sy, n )){
							candidates.add( new int[]{ sx, sy, n } );
							break;
						}
					}
				}
			}
		}
		assertFalse( candidates.isEmpty() );
		for (int[] c : candidates){
			WorldEvents.Event e = HuntEvent.huntEvent( SEED, c[0], c[1], c[2] );
			Dungeon.cycleTurn = c[2] * FC + 2000;
			DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DUSK;
			OverworldLevel ow = windowAt( e.wx - 12, e.wy );
			hero.pos = -1;
			for (int dx = -12; dx <= -6 && hero.pos == -1; dx++){
				int cell = ow.localCell( e.wx + dx, e.wy );
				if (cell != -1 && ow.passable[cell]) hero.pos = cell;
			}
			if (hero.pos == -1) continue;
			ArrayList<String> lines = logged( () -> HuntEvent.onHeroStep( ow ) );
			ArrayList<Mob> beasts = tagged( ow, e.id );
			//no open ground for it here: the night passes quietly, settled
			if (beasts.isEmpty()){
				assertTrue( ow.eventResolved( e.id ) );
				continue;
			}
			int deer = 0, wolves = 0;
			for (Mob m : beasts){
				HuntPack p = m.buff( HuntPack.class );
				assertEquals( WorldClock.night(), p.day );
				assertEquals( e.wx, p.ax );
				if (m instanceof Deer) deer++;
				else if (m instanceof GrayWolf || m instanceof BrownWolf) wolves++;
			}
			assertTrue( "deer: " + deer, deer >= 1 && deer <= 2 );
			assertTrue( "wolves: " + wolves, wolves >= 2 && wolves <= 3 );
			assertTrue( "pinned: heard of", ow.eventAnnounced( e.id ) );
			assertFalse( ow.eventResolved( e.id ) );
			assertTrue( said( lines, Messages.get( HuntEvent.class, "start_dusk" ) ) );

			//the next step: still one hunt
			int near = hero.pos;
			HuntEvent.onHeroStep( ow );
			assertEquals( beasts.size(), tagged( ow, e.id ).size() );

			//the hero walks off far from it: unseen and out of reach, the beasts slip away, and the
			//hunt is over for good
			hero.pos = -1;
			for (int cell = 0; cell < ow.length() && hero.pos == -1; cell++){
				int d = Math.max( Math.abs( ow.worldX() + cell % W - e.wx ), Math.abs( ow.worldY() + cell / W - e.wy ) );
				if (d > HuntEvent.FAR + 2 && ow.localCell( ow.worldX() + cell % W, ow.worldY() + cell / W ) != -1 && ow.passable[cell]){
					hero.pos = cell;
				}
			}
			assertNotEquals( -1, hero.pos );
			HuntEvent.onHeroStep( ow );
			assertTrue( tagged( ow, e.id ).isEmpty() );
			for (Mob m : beasts) assertFalse( "slipped away", ow.mobs.contains( m ) );
			assertTrue( ow.eventResolved( e.id ) );
			hero.pos = near;
			HuntEvent.onHeroStep( ow );
			assertTrue( "never run again", tagged( ow, e.id ).isEmpty() );

			//another, forced on the same ground: the dawn ends it, and what is close by the hero is
			//let go as ordinary beasts
			WorldEvents.Event f = WorldEvents.forcedHunt( e.wx, e.wy, ow.eventTurn(), e.endTurn );
			ow.forceEvent( f );
			HuntEvent.onHeroStep( ow );
			ArrayList<Mob> second = tagged( ow, f.id );
			assertFalse( "the forced hunt starts", second.isEmpty() );
			DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAWN;
			HuntEvent.onHeroStep( ow );
			assertTrue( tagged( ow, f.id ).isEmpty() );
			for (Mob m : second){
				boolean close = OverworldLevel.heroDistance( ow, m.pos ) <= HuntEvent.UNSEEN;
				assertEquals( "let go when close, gone when not", close, ow.mobs.contains( m ) );
			}
			assertTrue( ow.eventResolved( f.id ) );
			return;
		}
		throw new AssertionError( "no hunt of the test seed found room to start" );
	}

	private static boolean pinned( OverworldLevel ow, WorldEvents.Event e ){
		for (WorldEvents.Event p : ow.mapEvents( e.wx - 100, e.wy - 100, 200 )) if (p.id == e.id) return true;
		return false;
	}

	//an open cell of the window `dist` cells (Chebyshev) west or east of a world cell, or -1
	private static int openAt( OverworldLevel ow, int wx, int wy, int dist ){
		for (int d = dist; d <= dist + 6; d++){
			for (int side : new int[]{ -1, 1 }){
				for (int dy = -3; dy <= 3; dy++){
					int cell = ow.localCell( wx + side * d, wy + dy );
					if (cell != -1 && ow.passable[cell] && !ow.occupied( cell ) && !ow.water[cell]) return cell;
				}
			}
		}
		return -1;
	}

	//a hunt within a sector of the hero is heard of as night falls, long before it begins: pinned
	//on the chart while he is still far off, a distant howl told him once the way it lies, and
	//never over for want of beasts before it began. It begins as he comes near its ground (told
	//again only to who sees it start), and is over once its beasts are. One heard of that never
	//began is over at dawn; one a sector and more off is never heard of at all
	@Test
	public void aFarHuntIsHeardOfAtNightfallAndPinnedTillItIsOver(){
		for (int r = 1; r <= 8; r++){
			for (int sy = -r; sy <= r; sy++){
				for (int sx = -r; sx <= r; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r || !HuntEvent.ground( SEED, sx, sy )) continue;
					for (int n = 20; n < 200; n++){
						if (HuntEvent.scheduled( SEED, sx, sy, n ) && heardThenBegun( HuntEvent.huntEvent( SEED, sx, sy, n ) )) return;
					}
				}
			}
		}
		throw new AssertionError( "no hunt of the test seed could be heard of and then begin" );
	}

	//the test above on one hunt; false when the ground leaves no room for it here (try another)
	private boolean heardThenBegun( WorldEvents.Event e ){
		Dungeon.cycleTurn = e.startDay * FC + 2000;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DUSK;
		OverworldLevel ow = windowAt( e.wx, e.wy );
		//the hero forty cells and more off the ground, his sector within a sector of its
		int far = openAt( ow, e.wx, e.wy, 40 );
		if (far == -1) return false;
		int hwx = ow.worldX() + far % W, hwy = ow.worldY() + far / W;
		if (!WorldEvents.within( e, Math.floorDiv( hwx, WorldStructures.SECTOR ), Math.floorDiv( hwy, WorldStructures.SECTOR ), HuntEvent.HEAR )) return false;
		hero.pos = far;
		String howl = Messages.get( HuntEvent.class, "start_far",
				Messages.get( WorldEvents.class, "dir_" + WorldEvents.direction( e.wx - hwx, e.wy - hwy ) ) );

		assertFalse( "pinned before night fell", pinned( ow, e ) );
		ArrayList<String> lines = logged( () -> HuntEvent.onHeroStep( ow ) );
		//another hunt of the world's own got going by the hero: not this test's case
		for (Mob m : ow.mobs) if (m.buff( HuntPack.class ) != null) return false;
		assertTrue( "not heard of from " + Math.max( Math.abs( e.wx - hwx ), Math.abs( e.wy - hwy ) ) + " cells", ow.eventAnnounced( e.id ) );
		assertTrue( "not pinned from afar", pinned( ow, e ) );
		assertFalse( ow.eventBegun( e.id ) );
		assertTrue( tagged( ow, e.id ).isEmpty() );
		assertTrue( "no distant howl: " + lines, said( lines, howl ) );

		//the next steps: told once, never over for want of beasts that were never out
		for (int i = 0; i < 3; i++){
			ArrayList<String> again = logged( () -> HuntEvent.onHeroStep( ow ) );
			assertFalse( "told twice", said( again, howl ) );
			assertFalse( "over before it began", ow.eventResolved( e.id ) );
			assertTrue( pinned( ow, e ) );
		}

		//he comes near: the pack is loosed, told only to who sees it
		int near = openAt( ow, e.wx, e.wy, 10 );
		assertNotEquals( -1, near );
		hero.pos = near;
		ArrayList<String> start = logged( () -> HuntEvent.onHeroStep( ow ) );
		ArrayList<Mob> beasts = tagged( ow, e.id );
		if (beasts.isEmpty()){
			assertTrue( "no room, and not settled", ow.eventResolved( e.id ) );
			return false;
		}
		assertTrue( ow.eventBegun( e.id ) );
		assertFalse( ow.eventResolved( e.id ) );
		assertTrue( "unpinned while it runs", pinned( ow, e ) );
		assertFalse( "heard of twice", said( start, howl ) );

		//every beast of it gone: over, and its pin with it
		for (Mob m : beasts) OverworldLevel.vanish( ow, m );
		HuntEvent.onHeroStep( ow );
		assertTrue( ow.eventResolved( e.id ) );
		assertFalse( "still pinned once over", pinned( ow, e ) );

		//one forced on the ground, heard of from afar, that never begins: over with the night
		hero.pos = far;
		WorldEvents.Event f = WorldEvents.forcedHunt( e.wx, e.wy, ow.eventTurn(), e.endTurn );
		ow.forceEvent( f );
		HuntEvent.onHeroStep( ow );
		assertTrue( ow.eventAnnounced( f.id ) );
		assertFalse( ow.eventBegun( f.id ) );
		assertTrue( pinned( ow, f ) );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAWN;
		HuntEvent.onHeroStep( ow );
		assertTrue( "heard of and never begun, still on at dawn", ow.eventResolved( f.id ) );
		assertFalse( pinned( ow, f ) );
		assertTrue( tagged( ow, f.id ).isEmpty() );

		//and one past a sector off is never heard of
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DUSK;
		WorldEvents.Event g = WorldEvents.forcedHunt( e.wx + 3 * WorldStructures.SECTOR, e.wy, ow.eventTurn(), e.endTurn );
		ow.forceEvent( g );
		HuntEvent.onHeroStep( ow );
		assertFalse( "heard of three sectors off", ow.eventAnnounced( g.id ) );
		return true;
	}

	//the log keeps which hunts began through a save; one from before hunts were heard of from afar
	//(no record of it) counts every hunt it heard of as begun, as it then was
	@Test
	public void theBegunHuntsSurviveASaveAndOldSavesCountTheHeardAsBegun(){
		WorldEventLog log = new WorldEventLog();
		log.announced.put( 11L, 5000 );
		log.announced.put( 12L, 5000 );
		log.begun.put( 11L, 5000 );
		Bundle b = new Bundle();
		log.storeInBundle( b );
		WorldEventLog back = new WorldEventLog();
		back.restoreFromBundle( b );
		assertEquals( Integer.valueOf( 5000 ), back.begun.get( 11L ) );
		assertFalse( "heard of from afar is not begun", back.begun.containsKey( 12L ) );
		assertTrue( b.remove( "event_begun_ids" ) );
		assertTrue( b.remove( "event_begun_ends" ) );
		WorldEventLog old = new WorldEventLog();
		old.restoreFromBundle( b );
		assertTrue( old.begun.containsKey( 11L ) );
		assertTrue( old.begun.containsKey( 12L ) );
		//and the begun are forgotten with the rest once over
		back.prune( 5000 );
		assertTrue( back.begun.isEmpty() );
	}

	@Test
	public void huntBeastsAreSparedTheCull(){
		OverworldLevel ow = new OverworldLevel();
		ow.setSize( 64, 64 );
		ow.mobs = new HashSet<>();
		Dungeon.level = ow;
		hero.pos = 32 + 32 * 64;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DUSK;
		Deer plain = new Deer();
		plain.pos = 2 + 2 * 64;
		Deer hunted = new Deer();
		hunted.pos = 60 + 60 * 64;
		Buff.affect( hunted, HuntPack.class ).hunt = 1L;
		ow.mobs.add( plain );
		ow.mobs.add( hunted );
		OverworldFauna.cull( ow );
		assertFalse( "a grazer out after dusk is culled", ow.mobs.contains( plain ) );
		assertTrue( "the hunt's own is the hunt's to end", ow.mobs.contains( hunted ) );
	}

	@Test
	public void deerAreDaytimeFauna(){
		assertTrue( OverworldFauna.isFauna( new Deer() ) );
		Random.pushGenerator( 42 );
		try {
			boolean meadow = false, forest = false, night = false;
			for (int i = 0; i < 3000; i++){
				for (Mob m : OverworldFauna.roll( WorldModel.Biome.MEADOW, DayNightCycle.Phase.DAY )) meadow |= m instanceof Deer;
				for (Mob m : OverworldFauna.roll( WorldModel.Biome.FOREST, DayNightCycle.Phase.DAWN )) forest |= m instanceof Deer;
				for (Mob m : OverworldFauna.roll( WorldModel.Biome.FOREST, DayNightCycle.Phase.NIGHT )) night |= m instanceof Deer;
				for (Mob m : OverworldFauna.roll( WorldModel.Biome.PLAINS, DayNightCycle.Phase.DAY )) night |= m instanceof Deer;
			}
			assertTrue( "deer graze the meadows by day", meadow );
			assertTrue( "and the woods", forest );
			assertFalse( "but never by night, nor on the plains", night );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void parkedHuntBeastIsDroppedAndItsHuntEnded(){
		OverworldLevel ow = new OverworldLevel();
		ow.mobs = new HashSet<>();
		Dungeon.level = ow;
		Deer hunted = new Deer();
		HuntPack p = Buff.affect( hunted, HuntPack.class );
		p.hunt = 77L;
		p.day = 30;
		ow.mobs.add( hunted );
		Deer grazer = new Deer();
		ow.mobs.add( grazer );
		Class<?>[] types = { Mob.class, int.class, int.class };
		invoke( ow, OverworldLevel.class, "park", types, hunted, 500, 600 );
		invoke( ow, OverworldLevel.class, "park", types, grazer, 510, 600 );
		assertFalse( "never stored", ow.parked().contains( hunted ) );
		assertFalse( ow.mobs.contains( hunted ) );
		assertTrue( "its hunt is over", ow.eventResolved( 77L ) );
		assertTrue( "an ordinary deer waits in the store", ow.parked().contains( grazer ) );
	}

	@Test
	public void theDeerGetsAwayOnceThePackIsBroken(){
		Level l = field();
		Deer d = new Deer();
		d.pos = 36 + 32 * 64;
		d.sprite = stub();
		Buff.affect( d, HuntPack.class ).hunt = 5L;
		l.mobs.add( d );
		Actor.add( d );
		//no wolf after it any more, the hero in sight: it bolts away from him
		Arrays.fill( l.heroFOV, true );
		int before = l.distance( d.pos, hero.pos );
		ArrayList<String> lines = logged( () -> act( d ) );
		assertTrue( said( lines, Messages.get( HuntEvent.class, "escape" ) ) );
		assertTrue( "away from the hero", l.distance( d.pos, hero.pos ) > before );
		assertTrue( l.mobs.contains( d ) );
		//out of everyone's sight and well off: it is gone
		Arrays.fill( l.heroFOV, false );
		hero.pos = 5 + 5 * 64;
		act( d );
		assertFalse( l.mobs.contains( d ) );
		//a pack still on its heels: it runs from the wolves and stays
		Deer quarry = new Deer();
		quarry.pos = 20 + 40 * 64;
		quarry.sprite = stub();
		Buff.affect( quarry, HuntPack.class ).hunt = 6L;
		GrayWolf w = new GrayWolf();
		w.pos = 24 + 40 * 64;
		Buff.affect( w, HuntPack.class ).hunt = 6L;
		l.mobs.add( quarry );
		l.mobs.add( w );
		Actor.add( quarry );
		Actor.add( w );
		assertTrue( HuntEvent.packHunting( 6L ) );
		act( quarry );
		assertTrue( l.mobs.contains( quarry ) );
		assertTrue( "away from the wolf", quarry.pos % 64 < 20 );
		//the wolf turns on the hero instead: the deer is free
		w.buff( HuntPack.class ).defending = true;
		assertFalse( HuntEvent.packHunting( 6L ) );
	}

	@Test
	public void theKillLeavesMeatAndThePackFeeding(){
		Level l = field();
		GrayWolf w = new GrayWolf();
		w.pos = 20 + 20 * 64;
		w.sprite = stub();
		HuntPack pack = Buff.affect( w, HuntPack.class );
		pack.hunt = 9L;
		GrayWolf turned = new GrayWolf();
		turned.pos = 22 + 22 * 64;
		turned.sprite = stub();
		Buff.affect( turned, HuntPack.class ).hunt = 9L;
		turned.buff( HuntPack.class ).defending = true;
		Deer d = new Deer();
		d.pos = 21 + 20 * 64;
		d.sprite = stub();
		Buff.affect( d, HuntPack.class ).hunt = 9L;
		l.mobs.add( w );
		l.mobs.add( turned );
		l.mobs.add( d );
		Arrays.fill( l.heroFOV, true );
		int at = d.pos;
		ArrayList<String> lines = logged( () -> d.die( w ) );
		assertTrue( said( lines, Messages.get( HuntEvent.class, "kill" ) ) );
		Heap h = l.heaps.get( at );
		assertNotNull( "the carcass", h );
		int meat = 0;
		for (Item i : h.items) if (i instanceof MysteryMeat) meat += i.quantity();
		assertEquals( 2, meat );
		assertTrue( "the pack feeds", pack.feeding );
		assertEquals( 21, pack.feedX );
		assertEquals( 20, pack.feedY );
		assertEquals( HuntPack.FEED_TURNS, pack.feedLeft );
		assertFalse( "the one fighting the hero fights on", turned.buff( HuntPack.class ).feeding );
	}

	@Test
	public void everyHuntStringResolves(){
		for (String k : new String[]{ "start_dusk", "start_night", "start_far", "heard_near", "kill", "escape" }){
			String s = Messages.get( HuntEvent.class, k, "north" );
			assertFalse( k, s.contains( Messages.NO_TEXT_FOUND ) );
		}
		assertFalse( Messages.get( WorldEvents.class, "map_hunt" ).contains( Messages.NO_TEXT_FOUND ) );
		assertEquals( "deer", new Deer().name() );
		assertFalse( new Deer().description().contains( Messages.NO_TEXT_FOUND ) );
		assertFalse( new HuntPack().name().contains( Messages.NO_TEXT_FOUND ) );
		assertFalse( new HuntPack().desc().contains( Messages.NO_TEXT_FOUND ) );
	}

	//the howl a hunt starts with is a sound the game loads with the rest (Sample.load at startup)
	@Test
	public void theHowlIsAmongTheGamesSounds(){
		assertTrue( Arrays.asList( Assets.Sounds.all ).contains( Assets.Sounds.HOWL ) );
		assertTrue( "no " + Assets.Sounds.HOWL + " in the assets", Gdx.files.internal( Assets.Sounds.HOWL ).exists() );
		assertTrue( Gdx.files.internal( Assets.Sounds.HOWL ).length() > 1000 );
	}
}
