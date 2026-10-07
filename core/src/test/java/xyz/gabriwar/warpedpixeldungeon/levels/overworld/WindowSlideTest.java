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

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.FogOfWar;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A rebase seen from the render thread, which keeps drawing while the actor thread moves the
 * window (OverworldLevel.rebase): the window's origin, map and exploration change one after
 * another on the actor thread, then one render block moves the scene. The fog's texture is
 * painted from the level's arrays, so a frame painted in between used to mix the two windows
 * and leave explored ground drawn black (or unexplored ground drawn seen) once the texture was
 * blitted. Here the window walks across real slices, a real FogOfWar is painted by a stand-in
 * render thread, and the result must equal a fog painted whole from the final state.
 */
public class WindowSlideTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	//a meadow of the first mountain slice, with rock and drops round it (PrepBench's search)
	private static final int PEAK_X = 1816, PEAK_Y = -1128;

	private static Application app;
	private static GL20 gl, gl20;
	//what the stand-in render thread does with a block the rebase posts to it
	private static volatile PostHandler post;

	private interface PostHandler {
		void posted( Runnable block );
	}

	private OverworldLevel ow;
	private Hero hero;
	private FogOfWar fog;
	private int prev = -1;
	//the origin before the step that may rebase
	private volatile int lastWX, lastWY;

	private Level savedLevel;
	private Hero savedHero;
	private int savedDepth;
	private DayNightCycle.Phase savedPhase;
	private Camera savedCamera;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
		app = Gdx.app;
		gl = Gdx.gl;
		gl20 = Gdx.gl20;
		//textures without a GPU: the fog's pixels live in its Pixmap, which is all this reads
		GL20 none = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
		Gdx.gl = Gdx.gl20 = none;
		final Application base = app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class }, (p, m, a) -> {
			if (m.getName().equals( "postRunnable" )){
				post.posted( (Runnable) a[0] );
				return null;
			}
			return m.invoke( base, a );
		} );
	}

	@AfterClass
	public static void unboot(){
		Gdx.app = app;
		Gdx.gl = gl;
		Gdx.gl20 = gl20;
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedDepth = Dungeon.depth;
		savedPhase = DayNightCycle.debugPhaseOverride;
		savedCamera = Camera.main;
		Actor.clear();
		Camera.main = new Camera( 0, 0, 100, 100, 1 );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
	}

	@After
	public void tearDown(){
		Actor.clear();
		WorldModel.releaseSeasonShift();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Dungeon.depth = savedDepth;
		DayNightCycle.debugPhaseOverride = savedPhase;
		Camera.main = savedCamera;
	}

	// ------------------------------------------------------------------ the harness

	//the live window of a slice centred on a world cell, the hero on the open cell nearest it
	private void liveWindow( int altitude, int wx, int wy ) throws Exception {
		hero = new Hero(){
			@Override public void interrupt(){ }
		};
		hero.lvl = 20;
		hero.sprite = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void update(){ }
			@Override public void move( int from, int to ){ }
			@Override public void turnTo( int from, int to ){ }
		};
		hero.sprite.visible = false;
		Dungeon.hero = hero;
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, 0f );
		ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, 0f, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		Field net = OverworldLevel.class.getDeclaredField( "network" );
		net.setAccessible( true );
		net.set( ow, false );
		Dungeon.level = ow;
		Dungeon.depth = WorldLayers.depthOf( altitude );
		hero.viewDistance = ow.viewDistance;
		hero.pos = -1;
		int centre = W / 2 + (H / 2) * W;
		for (int r = 0; r < 40 && hero.pos == -1; r++){
			for (int dy = -r; dy <= r && hero.pos == -1; dy++){
				for (int dx = -r; dx <= r && hero.pos == -1; dx++){
					int c = centre + dx + dy * W;
					if (walkable( c )) hero.pos = c;
				}
			}
		}
		assertTrue( hero.pos != -1 );
		forgetTextures();
		fog = new FogOfWar( W, H );
	}

	//ground a walk may step on: open, no drop, nothing that needs a scene to press it
	private boolean walkable( int c ){
		if (c <= W || c >= W * (H - 1) || !ow.passable[c] || ow.avoid[c] || ow.pit[c]) return false;
		int t = ow.map[c];
		return t != Terrain.DOOR && t != Terrain.HIGH_GRASS && t != Terrain.FURROWED_GRASS
				&& t != Terrain.ENTRANCE && t != Terrain.EXIT;
	}

	//the open neighbour furthest along (dx, dy), never back where he came from; -1 when none is ahead
	private int step( int from, int dx, int dy ){
		int best = -1, score = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = from + n;
			if (c == prev || !walkable( c )) continue;
			int nx = Math.floorMod( n + 1, W ) - 1, ny = (n - nx) / W;
			int sc = nx * dx + ny * dy;
			if (sc > score){
				score = sc;
				best = c;
			}
		}
		return best;
	}

	//one step of the hero's: his turn begins (Dungeon.observe, the fog queued as GameScene does),
	//then he moves; true when the window moved
	private boolean walk( int to ){
		Dungeon.observe();
		queueObserved();
		lastWX = ow.worldX;
		lastWY = ow.worldY;
		prev = hero.pos;
		hero.pos = to;
		ow.occupyCell( hero );
		return ow.worldX != lastWX || ow.worldY != lastWY;
	}

	//what Dungeon.observe hands GameScene.updateFog: the square round the hero
	private void queueObserved(){
		int dist = Math.max( hero.viewDistance, 8 ) + 1;
		int x = hero.pos % W, y = hero.pos / W;
		int l = Math.max( 0, x - dist ), r = Math.min( x + dist, W - 1 );
		int t = Math.max( 0, y - dist ), b = Math.min( y + dist, H - 1 );
		fog.updateFogArea( l, t, r - l + 1, b - t + 1 );
	}

	//the rebase's render block as the scene runs it: GameScene.applyMapShift blits the fog first,
	//then the block itself (its Dungeon.observe), whose square GameScene queues
	private Runnable block( Runnable posted ){
		final int dx = ow.worldX - lastWX, dy = ow.worldY - lastWY;
		return () -> {
			fog.shiftContent( dx, dy );
			posted.run();
			queueObserved();
		};
	}

	//waits out the background preparation, as a hero walking at a player's pace does
	private void settle() throws Exception {
		Field l = OverworldLevel.class.getDeclaredField( "pregenLock" );
		Field pr = OverworldLevel.class.getDeclaredField( "preps" );
		l.setAccessible( true );
		pr.setAccessible( true );
		for (int i = 0; i < 2000; i++){
			boolean busy = false;
			synchronized (l.get( ow )){
				for (Object o : (List<?>) pr.get( ow )){
					Field r = o.getClass().getDeclaredField( "running" );
					r.setAccessible( true );
					busy |= (Boolean) r.get( o );
				}
			}
			if (!busy) return;
			Thread.sleep( 2 );
		}
	}

	private static void forgetTextures() throws Exception {
		//a second fog gets a texture of its own (TextureCache keys them by size)
		Field all = TextureCache.class.getDeclaredField( "all" );
		all.setAccessible( true );
		((Map<?, ?>) all.get( null )).clear();
	}

	//cells of the window's interior whose fog differs from a fog painted whole from the same state
	//(the outer two rings are left out: a wall beside the border reads the border, which a shift
	//repaints alone, and the border is 64 cells off screen)
	private String fogErrors() throws Exception {
		fog.refresh();
		forgetTextures();
		FogOfWar whole = new FogOfWar( W, H );
		whole.refresh();
		int wrong = 0, black = 0;
		StringBuilder at = new StringBuilder();
		for (int y = 2; y < H - 2; y++){
			for (int x = 2; x < W - 2; x++){
				for (int k = 0; k < 4; k++){
					int px = 2 * x + (k & 1), py = 2 * y + (k >> 1);
					int a = fog.texture.bitmap.getPixel( px, py ), b = whole.texture.bitmap.getPixel( px, py );
					if (a == b) continue;
					wrong++;
					int c = x + y * W;
					if (a == 0x000000FF && (ow.visited[c] || ow.mapped[c])) black++;
					if (at.length() < 200) at.append( x ).append( ',' ).append( y ).append( ' ' );
					break;
				}
			}
		}
		return wrong == 0 ? "" : wrong + " cells wrong, " + black + " of them explored ground drawn black: " + at;
	}

	//east, back west over the explored ground, then a diagonal out and back: rebases on both axes
	private static final int[][] LEGS = { { 1, 0, 150 }, { -1, 0, 150 }, { 1, -1, 110 }, { -1, 1, 110 } };

	// ------------------------------------------------------------------ the tests

	//the worst case made certain: a frame lands after the actor's half of every rebase, before its
	//render block. the level is held then, and the frame paints nothing
	@Test
	public void aFrameBetweenTheHalvesOfARebasePaintsNoFog() throws Exception {
		for (int[] at : new int[][]{ { 1, PEAK_X, PEAK_Y }, { 0, 0, 0 } }){
			liveWindow( at[0], at[1], at[2] );
			final int[] held = { 0 };
			post = posted -> {
				if (ow.fogHeld()) held[0]++;
				fog.refresh();
				block( posted ).run();
			};
			int rebases = 0;
			for (int[] leg : LEGS){
				for (int i = 0; i < leg[2]; i++){
					int to = step( hero.pos, leg[0], leg[1] );
					if (to == -1) break;
					if (walk( to )) rebases++;
					assertFalse( "the fog is let go once the block has run", ow.fogHeld() );
					fog.refresh();
					settle();
				}
			}
			assertTrue( "slice " + at[0] + ": rebases " + rebases, rebases >= 6 );
			assertEquals( "slice " + at[0] + ": the fog is held through every rebase", rebases, held[0] );
			assertEquals( "slice " + at[0], "", fogErrors() );
		}
	}

	//the race as it runs: a render thread drawing frames while the actor thread walks, the posted
	//blocks run at the start of its next frame, the actor waiting for them as the game's does
	@Test
	public void framesDrawnThroughARunLeaveTheFogAsSeen() throws Exception {
		liveWindow( 1, PEAK_X, PEAK_Y );
		final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
		post = posted -> queue.add( block( posted ) );
		final Throwable[] failed = { null };
		final int[] rebases = { 0 };
		final boolean[] done = { false };
		Thread actor = new Thread( () -> {
			try {
				for (int[] leg : LEGS){
					for (int i = 0; i < leg[2]; i++){
						int to = step( hero.pos, leg[0], leg[1] );
						if (to == -1) break;
						if (walk( to )) rebases[0]++;
						settle();
					}
				}
			} catch (Throwable t){
				failed[0] = t;
			}
			done[0] = true;
		}, "actor" );
		actor.start();
		while (!done[0] || !queue.isEmpty()){
			Runnable r;
			while ((r = queue.poll()) != null) r.run();
			fog.refresh();
			Thread.sleep( 1 );
		}
		actor.join();
		if (failed[0] != null) throw new AssertionError( failed[0] );
		assertTrue( "rebases " + rebases[0], rebases[0] >= 6 );
		assertEquals( "", fogErrors() );
	}

	//what the hero explored stays explored, cell for cell in the world, as the window slides under
	//it: on a peak and in the caves
	@Test
	public void explorationSlidesWithTheWindow() throws Exception {
		post = posted -> block( posted ).run();
		for (int[] at : new int[][]{ { 1, PEAK_X, PEAK_Y }, { -6, 0, 0 } }){
			liveWindow( at[0], at[1], at[2] );
			int rebases = 0;
			for (int[] leg : LEGS){
				for (int i = 0; i < leg[2]; i++){
					int to = step( hero.pos, leg[0], leg[1] );
					if (to == -1) break;
					HashSet<Long> visited = explored( ow.visited ), mapped = explored( ow.mapped );
					if (walk( to )) rebases++;
					assertKept( "slice " + at[0] + " step " + i + ": visited", visited, ow.visited );
					assertKept( "slice " + at[0] + " step " + i + ": mapped", mapped, ow.mapped );
					settle();
				}
			}
			assertTrue( "slice " + at[0] + ": rebases " + rebases, rebases >= 2 );
		}
	}

	private HashSet<Long> explored( boolean[] set ){
		HashSet<Long> out = new HashSet<>();
		for (int c = 0; c < set.length; c++){
			if (set[c]) out.add( OverworldLevel.worldKey( ow.worldX + c % W, ow.worldY + c / W ) );
		}
		return out;
	}

	private void assertKept( String what, HashSet<Long> before, boolean[] now ){
		for (long k : before){
			int c = ow.localCell( (int) (k & 0xFFFFFFFFL), (int) (k >> 32) );
			if (c != -1) assertTrue( what + " at " + (k & 0xFFFFFFFFL) + "," + (k >> 32), now[c] );
		}
	}

	//the ground a camp's fire lights for the hero from afar is kept as a window rect: the rebase
	//moves it with the window, so the next observe repaints the ground it lit and not ground 32 off
	@Test
	public void litGroundMovesWithTheWindow() throws Exception {
		CaveSites.Site camp = CaveSites.nearest( SEED, CaveSites.Type.CAMP, -4, 0, 0, 12 );
		assertNotNull( camp );
		assertTrue( camp.lights.length >= 4 );
		//the camp's stand cell sits on the window's eastern margin: his next step east slides it
		liveWindow( -4, camp.standX - (W - 64) + W / 2, camp.standY );
		hero.pos = ow.localCell( camp.standX, camp.standY );
		assertTrue( ow.passable[hero.pos] );
		post = posted -> block( posted ).run();
		Field nowF = OverworldLevel.class.getDeclaredField( "litNow" );
		nowF.setAccessible( true );
		//the first step in the margin starts the preparation; once it is ready, a step east rebases
		int to = step( hero.pos, 1, 0 );
		assertTrue( to != -1 );
		assertFalse( walk( to ) );
		settle();
		final int[][] seen = new int[2][];
		post = posted -> {
			try {
				seen[1] = (int[]) nowF.get( ow );
			} catch (IllegalAccessException e){
				throw new AssertionError( e );
			}
			block( posted ).run();
		};
		to = step( hero.pos, 1, 0 );
		assertTrue( to != -1 );
		Dungeon.observe();
		seen[0] = (int[]) nowF.get( ow );
		assertNotNull( "the camp's light shows him ground beyond his own sight", seen[0] );
		prev = hero.pos;
		hero.pos = to;
		lastWX = ow.worldX;
		lastWY = ow.worldY;
		ow.occupyCell( hero );
		int dx = ow.worldX - lastWX, dy = ow.worldY - lastWY;
		assertTrue( "the window moved", dx != 0 || dy != 0 );
		assertArrayEquals( new int[]{ seen[0][0] - dx, seen[0][1] - dy, seen[0][2] - dx, seen[0][3] - dy }, seen[1] );
	}

	//a path zig-zagging round rocks flips the guess at the next window step after step: each guess
	//is prepared once, not a whole preparation thrown away and started again at every step
	@Test
	public void aZigZagWalkPreparesEachGuessOnce() throws Exception {
		post = posted -> block( posted ).run();
		liveWindow( 1, PEAK_X, PEAK_Y );
		Field started = OverworldLevel.class.getDeclaredField( "prepsStarted" );
		started.setAccessible( true );
		int rebases = 0, steps = 0;
		for (int i = 0; i < 260; i++){
			int to = step( hero.pos, 1, -(i & 1) );
			if (to == -1) to = step( hero.pos, 1, 0 );
			if (to == -1) break;
			steps++;
			if (walk( to )) rebases++;
			//a step takes a while in play; the preparation is not waited for
			Thread.sleep( 3 );
		}
		settle();
		int preps = (Integer) started.get( ow );
		assertTrue( "steps " + steps + ", rebases " + rebases, steps > 150 && rebases >= 3 );
		assertTrue( "preparations " + preps + " for " + rebases + " rebases", preps <= 3 * rebases + 2 );
	}
}
