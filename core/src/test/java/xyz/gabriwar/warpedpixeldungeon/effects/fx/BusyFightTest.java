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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;

/**
 * Every area's effects at once, as the kit's busy fight: 15 cells of fire, 20 of toxic gas, two
 * burning rats taking four blows a second, a fire bolt every 1.5 s, a wand's lightning every 2 s
 * and rain on a pond. At each of the governor's tiers the kit's particles stay under its hard cap,
 * the lights under theirs, and the pictures drawn come down tier by tier.
 */
public class BusyFightTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group gases, world;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedCam = Camera.main;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		yesGl();
		Camera.main = new Camera( 0, 0, 256, 256, 1 );
		Camera.main.fullScreen = true;
		Emitter.freezeEmitters = false;
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
		clear();
		FxBudget.resetGovernor();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	private void clear(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		WaterFX.sceneCreated();
	}

	//a fresh scene: the kit's counts, the areas' statics, the layers, the floor and the hero
	private void scene(){
		clear();
		FxRandom.seedForTests( 5 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		for (int k : new int[]{ FxModules.LIGHTNING, FxModules.FIRE_HEAT, FxModules.GASES_COLD, FxModules.SKILLS }){
			FxModules.ALL[k].sceneCreated( null );
		}
		gases = new Group();
		world = new Group();
		//drawn by the main camera, as the game's world is
		world.camera = Camera.main;
		Dungeon.level = floor();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = at( 8, 8 );
	}

	//a bare floor seen whole, with a pond in its south-east corner (columns 11-14, rows 11-14)
	private static Level floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public String waterTex(){ return Assets.Environment.WATER_CAVES; }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int y = 11; y < 15; y++) for (int x = 11; x < 15; x++) l.map[x + y * W] = Terrain.WATER;
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

	//a GPU that answers yes: shaders compile and link, pictures can be drawn
	private static void yesGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			String n = m.getName();
			if ((n.equals( "glGetShaderiv" ) || n.equals( "glGetProgramiv" )) && a[2] instanceof IntBuffer){
				((IntBuffer) a[2]).put( 0, 1 );
				return null;
			}
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			if (r == String.class) return "";
			return null;
		} );
	}

	//a blob on the floor and its emitter where the game's scene puts it
	private <T extends Blob> void blob( Class<T> kind, int amount, int x0, int y0, int w, int h ){
		T b = com.watabou.utils.Reflection.newInstance( kind );
		for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) b.seed( Dungeon.level, at( x, y ), amount );
		Dungeon.level.blobs.put( kind, b );
		BlobEmitter e = new BlobEmitter( b );
		if (b.emissive()){
			layers[GameScene.Layer.LIGHTS.ordinal()].add( e );
			gases.add( e.matter() );
		} else {
			gases.add( e );
		}
	}

	private CharSprite rat( int cell ){
		Rat ch = new Rat();
		ch.state = ch.PASSIVE;
		ch.pos = cell;
		CharSprite s = new RatSprite();
		s.ch = ch;
		ch.sprite = s;
		world.add( s );
		s.place( cell );
		s.visible = true;
		s.add( CharSprite.State.BURNING );
		return s;
	}

	//pictures that would be drawn now: every visible Visual on the layers and the world
	private static int drawn( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.visible) continue;
			if (m instanceof Group) n += drawn( (Group) m );
			else if (m instanceof Visual && m.alive) n++;
		}
		return n;
	}

	private int drawn(){
		int n = drawn( gases ) + drawn( world );
		for (Group g : layers) n += drawn( g );
		return n;
	}

	/** What a tier's busy fight came to: the peaks over its last seconds and the mean drawn. */
	static final class Peaks {
		int particles, lights, persistent, drawn, frames;
		long drawnSum;

		float meanDrawn(){
			return frames == 0 ? 0 : drawnSum / (float)frames;
		}

		@Override
		public String toString(){
			return "particles " + particles + ", lights " + lights + " (persistent " + persistent + "), drawn peak "
					+ drawn + " mean " + Math.round( meanDrawn() );
		}
	}

	//the busy fight for `seconds` at a frame time that holds the governor's tier
	private Peaks fight( float frameTime, float seconds ){
		blob( Fire.class, 8, 2, 2, 3, 5 );
		blob( ToxicGas.class, 300, 6, 2, 5, 4 );
		CharSprite[] rats = { rat( at( 6, 9 ) ), rat( at( 9, 9 ) ) };
		PointF hero = DungeonTilemap.tileCenterToWorld( Dungeon.hero.pos );

		Peaks p = new Peaks();
		int frames = Math.round( seconds * 60 );
		float nextHit = 0, nextBolt = 0.3f, nextZap = 0.6f, nextDrop = 0;
		int blows = 0;
		for (int f = 0; f < frames; f++){
			float t = f / 60f;
			Game.timeTotal = 300 + t;
			FxBudget.frame( frameTime );
			WaterFX.frame();
			if (t >= nextHit){
				CharSprite r = rats[blows++ % 2];
				r.hitFx( hero.x, hero.y, 6, (blows & 1) == 0 ? HitFx.SLASH : HitFx.STRONG, true );
				nextHit += 0.25f;
			}
			if (t >= nextBolt){
				MagicMissile m = (MagicMissile) world.recycle( MagicMissile.class );
				m.reset( MagicMissile.FIRE, hero, rats[1].center(), null );
				nextBolt += 1.5f;
			}
			if (t >= nextZap){
				world.add( new Lightning( Dungeon.hero.pos, rats[0].ch.pos, null ).major() );
				nextZap += 2f;
			}
			//rain of 0.6 on the pond: about six drops on it a second
			if (t >= nextDrop){
				float x = (11 + FxRandom.Float( 4 )) * DungeonTilemap.SIZE, y = (11 + FxRandom.Float( 4 )) * DungeonTilemap.SIZE;
				WaterFX.drop( x, y, FxRandom.Float( 0.2f, 0.5f ), 0xFFFFFF );
				nextDrop += 1 / 6f;
			}
			gases.update();
			world.update();
			for (Group g : layers) g.update();

			//the first two seconds fill the scene; then it is measured
			if (t < 2f) continue;
			p.particles = Math.max( p.particles, FxBudget.live( FxBudget.PARTICLES ) );
			p.lights = Math.max( p.lights, FxLight.live() );
			p.persistent = Math.max( p.persistent, FxLight.livePersistent() );
			int d = drawn();
			p.drawn = Math.max( p.drawn, d );
			p.drawnSum += d;
			p.frames++;
		}
		return p;
	}

	//the governor at `tier`, by the frame times a phone that slow reports
	private static void tier( int tier ){
		FxBudget.resetGovernor();
		while (FxBudget.tier() < tier) FxBudget.frame( 1 / 10f );
	}

	@Test
	public void lastingLightsAskForRoomAndPutNoneOut(){
		scene();
		FxBudget.resetGovernor();
		int cap = FxBudget.persistentLightCap();
		FxLight[] held = new FxLight[cap];
		for (int i = 0; i < cap; i++){
			assertTrue( FxLight.room( 1, 1 ) );
			held[i] = FxLight.pool( 16 + 16 * (i % 8), 16 + 16 * (i / 8), 23, 0xFF9A48, 0.5f ).persistent();
		}
		//the persistent share full: no room for another lasting one, still room for a passing one
		assertTrue( !FxLight.room( 1, 1 ) );
		assertTrue( FxLight.room( 1, 0 ) );
		//an aura coming into view waits rather than putting one out
		com.watabou.noosa.Image sprite = new com.watabou.noosa.Image();
		sprite.x = sprite.y = 120;
		world.add( sprite );
		Flare aura = new Flare( 5, 16 ).color( 0xFFE9A8, true ).show( sprite, 0 );
		for (int f = 0; f < 10; f++) world.update();
		for (FxLight l : held) assertTrue( "a lasting light kept", l.alive && l.isPersistent() );
		assertTrue( FxLight.livePersistent() == cap );
		//one going out makes room, and the aura lays its pool then
		held[0].kill();
		world.update();
		assertTrue( "the aura's pool laid", FxLight.livePersistent() == cap );
		aura.killAndErase();
	}

	@Test
	public void everyAreaAtOnceStaysWithinTheBudgetAndScalesDownTierByTier(){
		Peaks[] at = new Peaks[3];
		for (int tier = FxBudget.FULL; tier <= FxBudget.MINIMAL; tier++){
			scene();
			tier( tier );
			//1/50 s: neither slow enough to step down nor fast enough to step up
			at[tier] = fight( 1 / 50f, 8f );
			assertTrue( "tier held at " + tier, FxBudget.tier() == tier );
			System.out.println( "busy fight, tier " + tier + ": " + at[tier] );

			assertTrue( "tier " + tier + " " + at[tier], at[tier].particles <= FxBudget.hard() );
			assertTrue( "tier " + tier + " " + at[tier], at[tier].lights <= FxBudget.lightCap() );
			assertTrue( "tier " + tier + " " + at[tier], at[tier].persistent <= FxBudget.persistentLightCap() );
			assertTrue( "something showed at tier " + tier, at[tier].particles > 0 && at[tier].lights > 0 );
		}
		assertTrue( "reduced draws less than full: " + at[0] + " / " + at[1], at[1].meanDrawn() < at[0].meanDrawn() );
		assertTrue( "minimal draws less than reduced: " + at[1] + " / " + at[2], at[2].meanDrawn() < at[1].meanDrawn() );
	}
}
