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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.MagicFire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A tree on fire (BurningTree, through its fire's FireField): its crown is alight while it burns -
 * flames over the crown, not its foot, shown only where the hero sees it - and goes out once it has
 * burnt down; only an open fire's (or an inferno's) trees catch so, the nearest CROWNS of them; a
 * crown follows its tree across the window's slide; a storm's bolt strikes a tree's crown; and none
 * of it draws the game's Random.
 */
public class BurningTreeTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Level level;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//a GPU that answers: the weather's sheet (the shimmer's rays) goes to it when first drawn on
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
		for (int i = 0; i < 1200; i++) FxBudget.frame( 1 / 120f );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		FireField.sceneCreated();
		LightRef.reset();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 13 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		level = floor();
		Dungeon.level = level;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = at( 8, 8 );
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		FireField.sceneCreated();
		LightRef.reset();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a bare floor, all of it seen
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
		Arrays.fill( l.visited, true );
		return l;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private void frames( int n ){
		for (int i = 0; i < n; i++){
			FxBudget.frame( 1 / 120f );
			for (Group g : layers) g.update();
		}
	}

	private static int living( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += living( (Group) m, c );
		}
		return n;
	}

	//a fire of a kind burning on cells, its emitter in the light layer as the scene puts it
	private <T extends Blob> T burning( Class<T> kind, int amount, int... cells ){
		T b = com.watabou.utils.Reflection.newInstance( kind );
		for (int c : cells) b.seed( level, c, amount );
		level.blobs.put( kind, b );
		layers[GameScene.Layer.LIGHTS.ordinal()].add( new BlobEmitter( b ) );
		return b;
	}

	//the crown's flames over a pine's cell, or null: a kit emitter in the light layer on its crown
	private FxEmitter crown( int pine ){
		float x = (pine % W) * 16 + 4, y = (pine / W) * 16 + 4;
		for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (g instanceof FxEmitter && g.alive && ((FxEmitter) g).x == x && ((FxEmitter) g).y == y) return (FxEmitter) g;
		}
		return null;
	}

	@Test
	public void aBoltStrikesATreesCrown(){
		int pine = at( 5, 5 ), oak = at( 9, 5 );
		Level.set( pine, Terrain.TREE_PINE, level );
		Level.set( oak, Terrain.TREE_OAK, level );
		PointF p = new PointF();
		assertTrue( BurningTree.struck( level, pine, p ) );
		assertEquals( "over its middle", 5 * 16 + 8, p.x, 0f );
		assertTrue( "into its crown, high on its cell: " + p.y, p.y >= 5 * 16 && p.y < 5 * 16 + 8 );
		//an oak with no crown drawn over the cell above it: its crown tops its own cell
		assertTrue( BurningTree.struck( level, oak, p ) );
		assertTrue( p.y >= 5 * 16 && p.y < 5 * 16 + 8 );
		//no tree, no crown
		p.set( -1, -1 );
		assertFalse( BurningTree.struck( level, at( 2, 2 ), p ) );
		assertEquals( -1, p.x, 0f );
	}

	@Test
	public void aBurningTreesCrownIsAlightUntilItBurnsDown(){
		int pine = at( 5, 5 );
		Level.set( pine, Terrain.TREE_PINE, level );
		burning( Fire.class, 8, pine, pine + 1 );
		frames( 15 );
		FxEmitter crown = crown( pine );
		assertNotNull( "its crown alight", crown );
		assertTrue( crown.on );
		assertTrue( "flames on it", living( crown, FxParticle.class ) > 0 );
		//its tongues are the crown's flames: FFF4B0 at their core, FFB040, FF5A20 at their tips
		assertEquals( 0xFFF4B0, BurningTree.FLAME.at( 0 ) );
		assertEquals( 0xFFB040, BurningTree.FLAME.at( 0.4f ) );
		assertEquals( 0xFF5A20, BurningTree.FLAME.at( 1 ) );
		int tongues = 0;
		for (Gizmo g : crown.membersView()){
			//a tongue's frame is 3 px wide and more, an ember's 2 at most
			if (!(g instanceof FxParticle) || !g.alive || ((FxParticle) g).width() < 3) continue;
			FxParticle p = (FxParticle) g;
			assertEquals( BurningTree.FLAME.at( p.progress() ), p.colorNow() );
			tongues++;
		}
		assertTrue( tongues > 0 );

		//burnt down to embers: the crown goes out, its last flames die down, its emitter is let go
		Level.set( pine, Terrain.EMBERS, level );
		frames( 10 );
		assertFalse( crown.on );
		frames( 90 );
		assertFalse( crown.alive );
	}

	@Test
	public void aCrownOutOfSightShowsNoFlames(){
		int pine = at( 5, 5 );
		Level.set( pine, Terrain.TREE_PINE, level );
		level.heroFOV[pine] = false;
		//a fire in sight beside it, so its field is made
		burning( Fire.class, 8, pine, at( 10, 10 ) );
		frames( 30 );
		FxEmitter crown = crown( pine );
		assertNotNull( crown );
		assertFalse( crown.on );
		assertEquals( 0, living( crown, FxParticle.class ) );
	}

	@Test
	public void aMagicFireTakesNoTreeAsWood(){
		int pine = at( 5, 5 );
		Level.set( pine, Terrain.TREE_PINE, level );
		burning( MagicFire.class, 8, pine );
		frames( 15 );
		assertNull( crown( pine ) );
	}

	@Test
	public void theTreesNearestTheHeroHaveTheirCrownsAlight(){
		//a fire through a wood: the CROWNS nearest the hero alight, the farther ones not
		int[] wood = { at( 7, 8 ), at( 9, 8 ), at( 8, 6 ), at( 8, 10 ), at( 1, 1 ), at( 14, 14 ) };
		for (int c : wood) Level.set( c, Terrain.TREE_PINE, level );
		burning( Fire.class, 8, wood );
		frames( 15 );
		for (int i = 0; i < wood.length; i++){
			if (i < FireField.CROWNS) assertNotNull( "a near tree's crown", crown( wood[i] ) );
			else assertNull( "a far one's", crown( wood[i] ) );
		}
	}

	@Test
	public void aCrownFollowsItsTreeAcrossTheWindowsSlide(){
		int pine = at( 5, 5 );
		Level.set( pine, Terrain.TREE_PINE, level );
		BurningTree t = BurningTree.light( level, pine, 8, true );
		assertNotNull( t );
		assertEquals( pine, t.cell() );
		//the scene slides its light layer with the world: a cell east
		FxEmitter crown = crown( pine );
		crown.x += 16;
		assertEquals( pine + 1, t.cell() );
		t.out();
	}

	@Test
	public void aBurningTreeDrawsNoneOfTheGamesRandom(){
		int pine = at( 5, 5 );
		Level.set( pine, Terrain.TREE_PINE, level );
		burning( Fire.class, 8, pine );
		Random.pushGenerator( 31 );
		float first = Random.Float();
		Random.popGenerator();
		Random.pushGenerator( 31 );
		frames( 90 );
		Level.set( pine, Terrain.EMBERS, level );
		frames( 30 );
		assertEquals( first, Random.Float(), 0f );
		Random.popGenerator();
	}
}
