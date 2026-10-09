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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A blob's emitter: as many of the game's random numbers drawn as ever (the blob's own use(), once),
 * none as it emits; each cell handed an index of its own; a factory that asks told where the
 * cloud's edge lies, which way it drifts, which cells died away, when it wells up and who walked
 * through it.
 */
public class BlobEmitterTest {

	private static final int W = 12;

	private float savedElapsed, savedScale;
	private Level savedLevel;
	private GL20 savedGl, savedGl20;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		//the weather's sheet (a fire's flames) goes to the GPU when first made: a stand-in for it
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedScale = FxBudget.scale;
		FxBudget.scale = 1f;
		FxBudget.reset();
		FxBudget.resetGovernor();
		Emitter.freezeEmitters = false;
		Dungeon.level = floor();
		Game.elapsed = 0.05f;
	}

	@After
	public void tearDown(){
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		FxBudget.scale = savedScale;
		FxBudget.reset();
	}

	private static Level floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		l.mapped = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		return l;
	}

	//a blob whose emitter pours the factory it is handed, once a tick
	public static class Test1 extends Blob {
		static Emitter.Factory factory;
		@Override
		public void use( BlobEmitter emitter ){
			super.use( emitter );
			emitter.pour( factory, 0.1f );
		}
	}

	//a factory that writes down what it is asked for and what it hears
	private static class Notes extends Emitter.Factory implements BlobEmitter.Aftermath, BlobEmitter.Bloomable, BlobEmitter.Kickable {
		final ArrayList<int[]> emits = new ArrayList<>();
		final ArrayList<Integer> dying = new ArrayList<>(), blooms = new ArrayList<>(), kicks = new ArrayList<>();
		final HashMap<Integer, Boolean> edges = new HashMap<>();
		final HashMap<Integer, Float> drifts = new HashMap<>();

		@Override
		public void emit( Emitter e, int index, float x, float y ){
			BlobEmitter.Cell c = BlobEmitter.cell();
			emits.add( new int[]{ index, c.cell } );
			edges.put( c.cell, c.edge );
			drifts.put( c.cell, c.dx );
		}

		@Override public void dying( int cell ){ dying.add( cell ); }
		@Override public void bloom( int cell ){ blooms.add( cell ); }
		@Override public void kick( int cell, float dx, float dy ){ kicks.add( cell ); }
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private static Test1 cloud( Notes notes, int amount ){
		Test1.factory = notes;
		Test1 b = new Test1();
		for (int y = 4; y <= 6; y++) for (int x = 4; x <= 6; x++) b.seed( Dungeon.level, at( x, y ), amount );
		return b;
	}

	private static void tick( BlobEmitter e ){
		for (int i = 0; i < 2; i++) e.update();
	}

	@Test
	public void aFiresEmitterDrawsTheGamesRandomOnceAsEver(){
		Random.pushGenerator( 99 );
		Random.Float();
		float second = Random.Float();
		Random.popGenerator();

		Random.pushGenerator( 99 );
		Fire fire = new Fire();
		for (int y = 4; y <= 6; y++) for (int x = 4; x <= 6; x++) fire.seed( Dungeon.level, at( x, y ), 10 );
		new BlobEmitter( fire );
		assertEquals( "its use() alone drew one", second, Random.Float(), 0f );
		Random.popGenerator();
	}

	@Test
	public void emittingDrawsNoneOfTheGamesRandom(){
		Random.pushGenerator( 7 );
		float first = Random.Float();
		Random.popGenerator();

		Notes notes = new Notes();
		Test1 b = cloud( notes, 4 );
		BlobEmitter e = new BlobEmitter( b );
		//its use() drew its one before this
		Random.pushGenerator( 7 );
		for (int i = 0; i < 40; i++){
			FxBudget.frame( Game.elapsed );
			e.update();
		}
		assertTrue( notes.emits.size() > 0 );
		assertEquals( first, Random.Float(), 0f );
		Random.popGenerator();
	}

	@Test
	public void eachCellHasAnIndexOfItsOwnAndItsEdgeAndDrift(){
		Notes notes = new Notes();
		Test1 b = cloud( notes, 1000 );
		//thicker to the west: it drifts east
		b.cur[at( 4, 5 )] = 3000;
		BlobEmitter e = new BlobEmitter( b );
		tick( e );
		assertEquals( 9, notes.emits.size() );
		HashSet<Integer> offsets = new HashSet<>();
		for (int[] n : notes.emits) offsets.add( n[0] - n[1] );
		assertEquals( "one tick's index plus the cell", 1, offsets.size() );
		assertFalse( "the middle is inside", notes.edges.get( at( 5, 5 ) ) );
		assertTrue( notes.edges.get( at( 4, 4 ) ) );
		assertTrue( notes.edges.get( at( 6, 5 ) ) );
		assertTrue( "drifting away from the thick side", notes.drifts.get( at( 5, 5 ) ) > 0 );
		assertTrue( Math.abs( notes.drifts.get( at( 5, 5 ) ) ) <= BlobEmitter.DRIFT );
	}

	@Test
	public void itHearsOfCellsDyingAndOfTheCloudWellingUp(){
		Notes notes = new Notes();
		Test1 b = cloud( notes, 1000 );
		BlobEmitter e = new BlobEmitter( b );
		tick( e );
		assertEquals( "welling up from nothing, at its thickest", 1, notes.blooms.size() );
		//a cell gone between ticks
		b.volume -= b.cur[at( 6, 6 )];
		b.cur[at( 6, 6 )] = 0;
		tick( e );
		assertEquals( Arrays.asList( at( 6, 6 ) ), notes.dying );
		assertEquals( 1, notes.blooms.size() );
		//half again at once
		b.seed( Dungeon.level, at( 5, 5 ), 6000 );
		tick( e );
		assertEquals( 2, notes.blooms.size() );
		assertEquals( at( 5, 5 ), (int) notes.blooms.get( 1 ) );
		//all of it gone: every last cell dies
		notes.dying.clear();
		b.fullyClear();
		tick( e );
		assertEquals( 8, notes.dying.size() );
	}

	@Test
	public void itHearsOfSomeoneWalkingThroughIt(){
		Notes notes = new Notes();
		Test1 b = cloud( notes, 1000 );
		BlobEmitter e = new BlobEmitter( b );
		Steps.record( at( 3, 5 ), at( 4, 5 ), true );
		Steps.record( at( 1, 1 ), at( 2, 1 ), true );
		Steps.record( at( 5, 3 ), at( 5, 4 ), false );
		for (int i = 0; i < 6; i++) e.update();
		assertEquals( "only the seen step into it", Arrays.asList( at( 4, 5 ) ), notes.kicks );
		for (int i = 0; i < 6; i++) e.update();
		assertEquals( 1, notes.kicks.size() );
	}

	@Test
	public void thinCellsEmitLess(){
		Notes thick = new Notes(), thin = new Notes();
		BlobEmitter a = new BlobEmitter( cloud( thick, 1000 ) );
		BlobEmitter b = new BlobEmitter( cloud( thin, 6 ) );
		FxRandom.seedForTests( 3 );
		for (int i = 0; i < 100; i++){
			FxBudget.frame( 1 / 60f );
			tick( a );
			tick( b );
		}
		assertTrue( thin.emits.size() < thick.emits.size() * 0.6f );
		assertTrue( thin.emits.size() > thick.emits.size() * 0.3f );
	}
}
