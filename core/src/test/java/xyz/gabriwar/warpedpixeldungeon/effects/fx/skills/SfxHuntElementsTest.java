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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArrowRain;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChargedShotDraw;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FletchingFeathers;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeartseekerArrow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaShieldWard;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MeteorFall;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.PoacherBait;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ArcaneEcho;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ArrowStorm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blink;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CinderTrail;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Fletching;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MeteorCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sorcerer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Spark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.StormCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WildCall;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import org.json.JSONArray;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The hunt-elements pass (huntress, mage, fire, frost, storm and arcane skills, docs/fx/skills.md):
 * every changed proc and cast draws the game's Random as master did (the draw sites of every file
 * against master's, each visual helper's draws pinned, a meteor's real impact as master's ten
 * bursts); an untinted lightning draws and shows exactly as before and a tinted one is in its own
 * colours, rim and all; the storm skills' chains (Spark's hero.next, Blink's jumps) and the bolts,
 * falls and throws that replaced wands' bolts and items land on their old frames; Charged Shot's aim
 * line and the meteor's heat are let go of with Fx.dispose; caps hold in a loud moment; nothing is
 * made on the GPU; and none of the borrowed looks is left (hearts, wool, the raptor, darts, the
 * trap mechanism, crosshairs, stench, rocks, red light, the level-up's stars, wands' bolts, rays,
 * the old flares, the low-health and bomb sounds).
 */
public class SfxHuntElementsTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group mobs;
	private Level level;
	private Hero hero;
	//the frame the hero was let go (hero.next), -1 for not yet
	private int nextAt = -1, frame;

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
		stubGl();
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		resetKit();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 11 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		mobs = new Group();
		level = room();
		Dungeon.level = level;
		hero = new Hero(){
			@Override
			public void next(){
				if (nextAt < 0) nextAt = frame;
			}
		};
		hero.pos = at( 3, 4 );
		Dungeon.hero = hero;
		link( plain(), hero, hero.pos );
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown() throws Exception {
		sceneField().set( null, null );
		GameScene.useLayersForTests( null );
		resetKit();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	private static void resetKit(){
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		PixelFlare.reset();
		LanceFX.reset();
		SkillSpectacleFX.reset();
		ShoutFX.reset();
		SkillFieldFX.clearCap();
		WaterFX.sceneCreated();
	}

	private static Level room(){
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
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private static CharSprite plain(){
		return new CharSprite(){
			{
				texture( xyz.gabriwar.warpedpixeldungeon.Assets.Effects.FX_SHEET );
				frame( 0, 0, 12, 15 );
			}
		};
	}

	private CharSprite link( CharSprite s, Char ch, int cell ){
		if (ch instanceof Mob) ((Mob) ch).state = ((Mob) ch).PASSIVE;
		ch.pos = cell;
		s.ch = ch;
		ch.sprite = s;
		mobs.add( s );
		s.x = (cell % W) * 16 + 2;
		s.y = (cell / W) * 16;
		s.visible = true;
		return s;
	}

	private CharSprite rat( int cell ){
		Rat r = new Rat();
		r.HP = r.HT = 50;
		return link( new RatSprite(), r, cell );
	}

	private static void stubGl(){
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

	private static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
	}

	private void frames( int n ){
		for (int i = 0; i < n; i++){
			frame++;
			Game.timeTotal += Game.elapsed;
			FxBudget.frame( Game.elapsed );
			WaterFX.frame();
			mobs.update();
			for (Group g : layers) g.update();
		}
	}

	//the frame (1 = the first update) a run's callback comes on, at most `max`
	private int arrival( java.util.function.Consumer<Runnable> start, int max ){
		final int[] at = { -1 }, f = { 0 };
		start.accept( () -> { if (at[0] < 0) at[0] = f[0]; } );
		for (f[0] = 1; f[0] <= max && at[0] < 0; f[0]++) frames( 1 );
		return at[0];
	}

	// ------------------------------------------------------------------ a scene for CellEmitter

	private static Field sceneField() throws Exception {
		Field f = GameScene.class.getDeclaredField( "scene" );
		f.setAccessible( true );
		return f;
	}

	//a bare game scene (no constructor run) holding the stand-in layers, so the skills' own
	//CellEmitter bursts work headless (ElementProcsTest's)
	private void bareScene() throws Exception {
		Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
		u.setAccessible( true );
		Object unsafe = u.get( null );
		Object scene = unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, GameScene.class );
		set( scene, "floorEmitters", layers[GameScene.Layer.FLOOR.ordinal()] );
		set( scene, "ripples", layers[GameScene.Layer.SURFACE.ordinal()] );
		set( scene, "subsurface", layers[GameScene.Layer.SUBSURFACE.ordinal()] );
		set( scene, "sheen", layers[GameScene.Layer.SHEEN.ordinal()] );
		set( scene, "emitters", layers[GameScene.Layer.EFFECTS.ordinal()] );
		set( scene, "lights", layers[GameScene.Layer.LIGHTS.ordinal()] );
		set( scene, "effects", new Group() );
		set( scene, "gases", new Group() );
		sceneField().set( null, scene );
	}

	private static void set( Object o, String name, Object v ) throws Exception {
		Field f = GameScene.class.getDeclaredField( name );
		f.setAccessible( true );
		f.set( o, v );
	}

	// ------------------------------------------------------------------ lightning: the tint

	private static int lightningColor( String method, float t ) throws Exception {
		Method m = Lightning.class.getDeclaredMethod( method, float.class );
		m.setAccessible( true );
		return (Integer) m.invoke( null, t );
	}

	//every image of a lightning by its texture: cores (effects.png), halos, glows
	private static void images( Group g, String asset, List<Image> out ){
		for (Gizmo m : g.membersView()){
			if (m instanceof Group) images( (Group) m, asset, out );
			else if (m instanceof Image && ((Image) m).texture == TextureCache.get( asset )) out.add( (Image) m );
		}
	}

	private static float r( int c ){ return ((c >> 16) & 0xFF) / 255f; }
	private static float g( int c ){ return ((c >> 8) & 0xFF) / 255f; }
	private static float b( int c ){ return (c & 0xFF) / 255f; }

	@Test
	public void anUntintedLightningDrawsAndShowsAsBefore() throws Exception {
		//the same draws of the game's Random, tinted or not (the tint is colour alone)
		//(the game's next number after making one: a long arc draws past what draws() counts)
		for (int[] tint : new int[][]{ Lightning.STORM, Lightning.ARCANE }){
			Random.pushGenerator( 5 );
			float before = Random.Float();
			Random.popGenerator();
			Random.pushGenerator( 5 );
			new Lightning( new PointF( 40, 60 ), new PointF( 150, 95 ), null );
			float plain = Random.Float();
			Random.popGenerator();
			Random.pushGenerator( 5 );
			new Lightning( new PointF( 40, 60 ), new PointF( 150, 95 ), null ).tint( tint );
			float tinted = Random.Float();
			Random.popGenerator();
			assertNotEquals( before, plain, 0f );
			assertEquals( plain, tinted, 0f );
		}
		//an untinted one in the storm's blue-white, frame by frame, as the static colours say
		Lightning l = new Lightning( new PointF( 40, 60 ), new PointF( 150, 95 ), null );
		assertFalse( l.tinted() );
		mobs.add( l );
		for (int f = 1; f <= 16; f++){
			frames( 1 );
			if (!l.alive) break;
			float t = f * Game.elapsed;
			int core = lightningColor( "coreColor", t ), halo = lightningColor( "haloColor", t );
			ArrayList<Image> cores = new ArrayList<>(), halos = new ArrayList<>();
			images( l, xyz.gabriwar.warpedpixeldungeon.Assets.Effects.EFFECTS, cores );
			images( l, xyz.gabriwar.warpedpixeldungeon.Assets.Effects.LIGHTNING_HALO, halos );
			assertFalse( cores.isEmpty() );
			for (Image c : cores){
				assertEquals( r( core ), c.rm, 1e-4f );
				assertEquals( g( core ), c.gm, 1e-4f );
				assertEquals( b( core ), c.bm, 1e-4f );
				assertEquals( 0f, c.ra, 0f );
			}
			for (Image h : halos){
				assertEquals( r( halo ), h.rm, 1e-4f );
				assertEquals( b( halo ), h.bm, 1e-4f );
			}
		}
	}

	@Test
	public void aTintedLightningIsInItsOwnColours() throws Exception {
		Lightning l = new Lightning( new PointF( 40, 60 ), new PointF( 150, 95 ), null ).tint( Lightning.ARCANE );
		assertTrue( l.tinted() );
		mobs.add( l );
		frames( 1 );
		int haloHot = Lightning.ARCANE[2];
		ArrayList<Image> cores = new ArrayList<>(), halos = new ArrayList<>(), glows = new ArrayList<>();
		images( l, xyz.gabriwar.warpedpixeldungeon.Assets.Effects.EFFECTS, cores );
		images( l, xyz.gabriwar.warpedpixeldungeon.Assets.Effects.LIGHTNING_HALO, halos );
		images( l, xyz.gabriwar.warpedpixeldungeon.Assets.Effects.FX_LIGHT, glows );
		assertFalse( halos.isEmpty() );
		for (Image h : halos){
			assertEquals( r( haloHot ), h.rm, 1e-4f );
			assertEquals( g( haloHot ), h.gm, 1e-4f );
			assertEquals( b( haloHot ), h.bm, 1e-4f );
		}
		//the stroke: its white middle at the core's colour, its painted blue rim (4488FF) turned the
		//halo's violet, never left blue. A white middle bounds the rim from below (a multiply and an
		//offset that keep 255 at 255 cannot take the rim's 44 red or 88 green any lower), so the rim
		//reaches the halo's colour as far as that allows (the arcane halo's 70 green: 88)
		for (Image c : cores){
			assertEquals( 1f, c.rm + c.ra, 0.01f );
			float rimR = 0x44 / 255f * c.rm + c.ra, rimG = 0x88 / 255f * c.gm + c.ga, rimB = c.bm + c.ba;
			assertEquals( Math.max( r( haloHot ), 0x44 / 255f ), rimR, 0.02f );
			assertEquals( Math.max( g( haloHot ), 0x88 / 255f ), rimG, 0.02f );
			assertTrue( "rim still blue", rimR > rimG );
			assertTrue( rimB <= 1.001f );
		}
		//its glow where it strikes: its halo toward white, not the storm's D8E4FF
		assertFalse( glows.isEmpty() );
		int bloom = Ramp.lerp( haloHot, 0xFFFFFF, 0.6f );
		boolean found = false;
		for (Image gl : glows) found |= Math.abs( gl.rm - r( bloom ) ) < 0.01f && Math.abs( gl.gm - g( bloom ) ) < 0.01f;
		assertTrue( found );
		//its pools' colour
		Field pool = Lightning.class.getDeclaredField( "poolColor" );
		pool.setAccessible( true );
		assertEquals( Lightning.ARCANE[4], pool.getInt( l ) );
		assertNotEquals( Lightning.ARCANE[4], pool.getInt( new Lightning( new PointF( 1, 1 ), new PointF( 90, 90 ), null ) ) );
	}

	@Test
	public void aTintedLightningCallsBackOnItsFrame(){
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			int plain = arrival( cb -> mobs.add( new Lightning( at( 2, 2 ), at( 8, 5 ), cb::run ) ), 400 );
			int storm = arrival( cb -> mobs.add( new Lightning( at( 2, 2 ), at( 8, 5 ), cb::run ).tint( Lightning.STORM ) ), 400 );
			int arcane = arrival( cb -> mobs.add( new Lightning( at( 2, 2 ), at( 8, 5 ), cb::run ).major().tint( Lightning.ARCANE ) ), 400 );
			assertTrue( plain > 0 );
			assertEquals( "dt " + dt, plain, storm );
			assertEquals( "dt " + dt, plain, arcane );
		}
	}

	// ------------------------------------------------------------------ the chains the game waits on

	@Test
	public void sparksChainLetsTheHeroGoOnItsFrame() throws Exception {
		bareScene();
		CharSprite a = rat( at( 5, 4 ) ), c = rat( at( 7, 4 ) );
		Method arc = Spark.class.getDeclaredMethod( "arc", Hero.class, int.class, ArrayList.class, int.class, int.class );
		arc.setAccessible( true );
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			//two untinted lightnings, one after the other: the frame master's chain let the hero go
			final int[] want = { -1 };
			final int start = frame;
			mobs.add( new Lightning( hero.pos, a.ch.pos, () -> mobs.add( new Lightning( a.ch.pos, c.ch.pos, () -> want[0] = frame - start ) ) ) );
			for (int f = 0; f < 200 && want[0] < 0; f++) frames( 1 );
			nextAt = -1;
			int from = frame;
			arc.invoke( null, hero, hero.pos, new ArrayList<>( Arrays.asList( a.ch.pos, c.ch.pos ) ), 0, c.ch.pos );
			for (int f = 0; f < 200 && nextAt < 0; f++) frames( 1 );
			assertTrue( want[0] > 0 );
			assertEquals( "dt " + dt, want[0], nextAt - from );
		}
	}

	@Test
	public void blinksChainJumpsOnItsFrames() throws Exception {
		CharSprite a = rat( at( 4, 4 ) ), c = rat( at( 6, 5 ) );
		//strikes that land on nothing: the chain's pictures and time alone
		a.ch.HP = 0;
		c.ch.HP = 0;
		Class<?> jumpClass = Class.forName( Blink.class.getName() + "$Jump" );
		Constructor<?> jump = jumpClass.getDeclaredConstructor( int.class, int.class, Char.class, int.class );
		jump.setAccessible( true );
		Method play = Blink.class.getDeclaredMethod( "playJump", Blink.class, Hero.class, ArrayList.class, int.class );
		play.setAccessible( true );
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			//master's chain as it lays its pictures in the group (each target's sparks, then its arc)
			final int[] want = { -1 };
			final int start = frame;
			a.emitter().burst( SkillBursts.STORM_SPARK, 1 );
			mobs.add( new Lightning( at( 3, 4 ), at( 5, 4 ), () -> {
				c.emitter().burst( SkillBursts.STORM_SPARK, 1 );
				mobs.add( new Lightning( at( 5, 4 ), at( 7, 6 ), () -> want[0] = frame - start ) );
			} ) );
			for (int f = 0; f < 200 && want[0] < 0; f++) frames( 1 );
			ArrayList<Object> jumps = new ArrayList<>();
			jumps.add( jump.newInstance( at( 3, 4 ), at( 5, 4 ), a.ch, 0 ) );
			jumps.add( jump.newInstance( at( 5, 4 ), at( 7, 6 ), c.ch, 0 ) );
			nextAt = -1;
			int from = frame;
			play.invoke( null, new Blink(), hero, jumps, 0 );
			//his shape left where he set off
			assertTrue( Afterimage.live() > 0 );
			for (int f = 0; f < 200 && nextAt < 0; f++) frames( 1 );
			assertTrue( want[0] > 0 );
			assertEquals( "dt " + dt, want[0], nextAt - from );
		}
	}

	// ------------------------------------------------------------------ bolts, falls and throws

	@Test
	public void theBoltsFallsAndThrowsLandAsTheirOldOnesDid(){
		CharSprite hs = hero.sprite;
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			//Wizard's and Pyromancy's (cell to cell), Spark's (from a sprite), the ember leap's and the
			//spirit volley's (point to point)
			for (int look : new int[]{ SkillBolt.ARCANE, SkillBolt.EMBER }){
				int old = arrival( cb -> ((MagicMissile) mobs.recycle( MagicMissile.class )).reset( MagicMissile.FIRE, at( 2, 2 ), at( 9, 7 ), cb::run ), 400 );
				int now = arrival( cb -> SkillBolt.fly( mobs, at( 2, 2 ), at( 9, 7 ), look, cb::run ), 400 );
				assertEquals( old, now );
			}
			int old = arrival( cb -> MagicMissile.boltFromChar( mobs, MagicMissile.MAGIC_MISSILE, hs, at( 9, 6 ), cb::run ), 400 );
			int now = arrival( cb -> Spark.bolt( hs, at( 9, 6 ), cb::run ), 400 );
			assertEquals( old, now );
			for (int look : new int[]{ SkillBolt.SPIRIT, SkillBolt.EMBER }){
				old = arrival( cb -> ((MagicMissile) mobs.recycle( MagicMissile.class )).reset( MagicMissile.WARD, new PointF( 50, 70 ), new PointF( 150, 40 ), cb::run ), 400 );
				now = arrival( cb -> SkillBolt.fly( mobs, new PointF( 50, 70 ), new PointF( 150, 40 ), look, cb::run ), 400 );
				assertEquals( old, now );
			}
			//the meteor and the spirit arrows: the bomb's and the darts' draws and frames
			for (Object[] f : new Object[][]{ { new Bomb(), SkillFX.Fall.METEOR }, { new Dart(), SkillFX.Fall.SPIRIT_ARROW } }){
				final Item drawsAs = (Item) f[0];
				final int fall = (Integer) f[1];
				Random.pushGenerator( 31 );
				old = arrival( cb -> SkillFX.rain( at( 7, 6 ), drawsAs, 1, cb::run ), 400 );
				Random.popGenerator();
				Random.pushGenerator( 31 );
				now = arrival( cb -> SkillFX.rain( at( 7, 6 ), drawsAs, fall, 1, cb::run ), 400 );
				Random.popGenerator();
				assertTrue( old > 0 );
				assertEquals( old, now );
				assertEquals( 8, EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), drawsAs, fall, 2, null ) ) );
				frames( 60 );
			}
			//Heartseeker's and Fletching's throws timed as the dart, Bear Trap's snare as the mechanism
			for (int look : new int[]{ SkillFX.Streak.SPIRIT_ARROW, SkillFX.Streak.FEATHER_DART }){
				old = arrival( cb -> SkillFX.streak( at( 3, 3 ), at( 9, 6 ), new Dart(), cb::run ), 400 );
				now = arrival( cb -> SkillFX.streak( at( 3, 3 ), at( 9, 6 ), look, new Dart(), cb::run ), 400 );
				assertEquals( old, now );
			}
			Item mechanism = new Item(){ { image = ItemSpriteSheet.TRAP_MECHANISM; } };
			old = arrival( cb -> SkillFX.streak( hs, at( 8, 4 ), mechanism, cb::run ), 400 );
			now = arrival( cb -> SkillFX.streak( hs, at( 8, 4 ), SkillFX.Streak.SNARE, cb::run ), 400 );
			assertEquals( old, now );
		}
	}

	// ------------------------------------------------------------------ the game's Random

	@Test
	public void theNewPicturesDrawTheGamesRandomAsTheOldOnesDid() throws Exception {
		CharSprite a = rat( at( 5, 4 ) ), c = rat( at( 7, 5 ) );
		CharSprite hs = hero.sprite;
		//one burst of the sprite's emitter each, as master's hearts, wool, light, sparks, flames
		assertEquals( 1, EnchantFxTest.draws( () -> HeartseekerArrow.lodged( a, 3 ) ) );
		assertEquals( 1, EnchantFxTest.draws( () -> HeartseekerArrow.beat( a, 1 ) ) );
		assertEquals( 1, EnchantFxTest.draws( () -> HeartseekerArrow.bursting( a ) ) );
		assertEquals( 1, EnchantFxTest.draws( () -> Fletching.burst( a ) ) );
		assertEquals( 1, EnchantFxTest.draws( () -> ChargedShotDraw.drawing( hs, 3 ) ) );
		assertEquals( 1, EnchantFxTest.draws( () -> Sorcerer.overloaded( a ) ) );
		assertEquals( 2, EnchantFxTest.draws( () -> CinderTrail.gather( hs, 4 ) ) );
		frames( 60 );
		//the pictures of their own: none
		assertEquals( 0, EnchantFxTest.draws( () -> {
			Fletching.splintered( a );
			EmberArrows.heatRing( a.ch.pos );
			EmberArrows.kindled( hs, EmberArrows.HEAT, 0xFFC070 );
			WildCall.marked( a );
			WildCall.bite( a.ch.pos, hero.pos );
			PoacherBait.noose( a );
			ArrowRain.stormLands( a.ch.pos );
			ArrowStorm.loose( mobs, 50, 70, 120, 2, 0 );
			ChargedShot.release( hs, at( 10, 4 ) );
			ChargedShotDraw.AimLine.show( Arrays.asList( at( 4, 4 ), at( 5, 4 ), at( 6, 4 ) ) );
			MeteorCall.marked( hs, c.ch.pos );
			MeteorFall.impact( c.ch.pos );
			MeteorFall.HeatMark.at( c.ch.pos );
			ArcaneEcho.echo( a.ch.pos, hero.pos );
			Blink.jumped( hs, at( 3, 4 ), at( 5, 4 ) );
			StormCall.struck( a.ch.pos );
			ManaShieldWard.broke( hs );
			frames( 90 );
		} ) );
		//the pluck: one burst, as the wool
		FletchingFeathers ring = Buff.affect( hero, FletchingFeathers.class );
		assertEquals( 1, EnchantFxTest.draws( ring::pluck ) );
	}

	@Test
	public void aMeteorStrikesWithMastersTenBursts() throws Exception {
		//master's impact: the blast's burst and a flame burst on each of the nine cells (Explosion.meteor
		//drew none); now the stone's chips and an ember or flame burst on each of the nine
		bareScene();
		CharSprite r = rat( at( 6, 6 ) );
		MeteorFall fall = new MeteorFall();
		fall.set( at( 6, 6 ), 3 );
		Method impact = MeteorFall.class.getDeclaredMethod( "impactFX" );
		impact.setAccessible( true );
		assertEquals( 10, EnchantFxTest.draws( () -> {
			try {
				impact.invoke( fall );
			} catch (ReflectiveOperationException e){
				throw new AssertionError( e );
			}
		} ) );
		//and their emits over the frames after: a plain emitter draws two for each particle's spawn
		//point, so the chips keep the blast's 30 and each cell its 4 (2 x 30 + 9 x 2 x 4, as master)
		Random.pushGenerator( 1234 );
		frames( 60 );
		float next = Random.Float();
		Random.popGenerator();
		Random.pushGenerator( 1234 );
		for (int i = 0; i < 2 * 30 + 9 * 2 * 4; i++) Random.Float();
		assertEquals( "the emits' draws", Random.Float(), next, 0f );
		Random.popGenerator();
		assertNotNull( r );
	}

	/**
	 * Every draw site of the game's Random in the package's files, by kind, as master (72b40617b)
	 * has them: bursts, starts, delayed starts, pours, lightnings and their arcs, bolts (a wand's
	 * MagicMissile then, SkillBolt.fly now: one draw each), rains, Random calls, splashes. Two moves
	 * are equal draws: Heartseeker's hit burst is HeartseekerArrow.lodged's now (one burst, called
	 * where it was), and Spark's bolt is chosen by an if (one SkillBolt.fly runs, as
	 * boltFromChar's one reset did).
	 */
	@Test
	public void everyDrawSiteIsWhereMasterHadIt() throws Exception {
		String base = "src/main/java/xyz/gabriwar/warpedpixeldungeon/";
		Object[][] census = {
				{ "actors/buffs/ArrowRain.java", new int[]{ 0, 0, 0, 0, 0, 0, 0, 1, 2, 0 } },
				{ "actors/buffs/ChargedShotDraw.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/buffs/FletchingFeathers.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/buffs/HeartseekerArrow.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/buffs/ManaShieldWard.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/buffs/MeteorFall.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 1, 1, 0 } },
				{ "actors/buffs/PoacherBait.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/buffs/SlipstreamWind.java", new int[]{ 5, 0, 0, 1, 0, 0, 0, 0, 0, 0 } },
				{ "actors/buffs/SpiritArmorMotes.java", new int[]{ 0, 0, 0, 0, 0, 0, 1, 0, 1, 0 } },
				{ "actors/hero/skills/ArcaneEcho.java", new int[]{ 2, 0, 0, 0, 2, 0, 0, 0, 2, 0 } },
				{ "actors/hero/skills/ArrowStorm.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/BearTrap.java", new int[]{ 5, 1, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/Blink.java", new int[]{ 3, 0, 0, 0, 1, 3, 0, 0, 2, 0 } },
				{ "actors/hero/skills/ChargedShot.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/CinderTrail.java", new int[]{ 4, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
				{ "actors/hero/skills/EmberArrows.java", new int[]{ 5, 1, 1, 0, 0, 0, 1, 0, 0, 0 } },
				{ "actors/hero/skills/Fletching.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/FrostArrows.java", new int[]{ 4, 0, 2, 0, 0, 0, 0, 0, 1, 0 } },
				//master's one hit burst: HeartseekerArrow.lodged's now
				{ "actors/hero/skills/Heartseeker.java", new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/MeteorCall.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/Poacher.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/PyreAffinity.java", new int[]{ 3, 0, 0, 0, 0, 0, 1, 0, 1, 0 } },
				{ "actors/hero/skills/SixthSense.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/Sorcerer.java", new int[]{ 3, 0, 0, 0, 1, 0, 0, 0, 1, 0 } },
				//master's one boltFromChar: one of an if's two SkillBolt.fly now
				{ "actors/hero/skills/Spark.java", new int[]{ 3, 0, 0, 0, 2, 0, 2, 0, 1, 0 } },
				{ "actors/hero/skills/SpellBlade.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
				{ "actors/hero/skills/StormCall.java", new int[]{ 1, 0, 0, 0, 2, 0, 0, 0, 1, 0 } },
				{ "actors/hero/skills/Thorns.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/WildCall.java", new int[]{ 4, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
				{ "actors/hero/skills/Wizard.java", new int[]{ 2, 0, 0, 0, 0, 0, 1, 0, 1, 0 } },
				{ "effects/Lightning.java", new int[]{ 0, 0, 0, 0, 1, 0, 0, 0, 4, 0 } } };
		String[] sites = { "\\.burst\\s*\\(", "(?<!FxTimeline)\\.start\\s*\\(", "\\.startDelayed\\s*\\(", "\\.pour\\s*\\(",
				"new\\s+Lightning\\s*\\(", "new\\s+Lightning\\.Arc\\s*\\(",
				"(MagicMissile\\.boltFromChar|\\(\\s*\\(\\s*MagicMissile\\s*\\)[^;]*?\\.reset\\s*\\(|SkillBolt\\.fly\\s*\\()",
				"SkillFX\\.rain\\s*\\(",
				"(?<![A-Za-z])Random\\.(Int|Float|NormalIntRange|NormalFloat|IntRange|Long|chances|element|shuffle|oneOf|chance)\\b",
				"Splash\\.at\\s*\\(" };
		for (Object[] row : census){
			String src = source( base + row[0] );
			int[] want = (int[]) row[1];
			for (int k = 0; k < sites.length; k++){
				Matcher m = Pattern.compile( sites[k], Pattern.DOTALL ).matcher( src );
				int n = 0;
				while (m.find()) n++;
				assertEquals( row[0] + " " + sites[k], want[k], n );
			}
		}
	}

	//a source file without its comments
	private static String source( String path ) throws Exception {
		String s = new String( Files.readAllBytes( new File( path ).toPath() ), "UTF-8" );
		s = s.replaceAll( "(?s)/\\*.*?\\*/", "" );
		return s.replaceAll( "//[^\\n]*", "" );
	}

	// ------------------------------------------------------------------ held visuals

	private int count( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.exists && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += count( (Group) m, c );
		}
		return n;
	}

	@Test
	public void theAimLineAndTheHeatAreLetGo(){
		Group floor = layers[GameScene.Layer.FLOOR.ordinal()];
		//the draw: its aim line made anew each turn, one at a time, let go of as it is loosed
		ChargedShotDraw draw = Buff.append( hero, ChargedShotDraw.class );
		draw.set( at( 10, 4 ), 3, 5 );
		frames( 2 );
		assertEquals( 1, count( floor, ChargedShotDraw.AimLine.class ) );
		draw.fx( true );
		draw.fx( true );
		frames( 2 );
		assertEquals( 1, count( floor, ChargedShotDraw.AimLine.class ) );
		draw.detach();
		frames( 2 );
		assertEquals( 0, count( floor, ChargedShotDraw.AimLine.class ) );
		//one let go of before it was ever shown never shows
		ChargedShotDraw.AimLine early = ChargedShotDraw.AimLine.show( Arrays.asList( at( 4, 4 ), at( 5, 4 ) ) );
		early.release();
		frames( 2 );
		assertEquals( 0, count( floor, ChargedShotDraw.AimLine.class ) );

		//the meteor's mark: its heat from the cast till it falls, its pool put out with it
		int lights = FxLight.live();
		MeteorFall fall = Buff.append( hero, MeteorFall.class );
		fall.set( at( 6, 4 ), 1 );
		frames( 3 );
		assertEquals( 1, count( floor, MeteorFall.HeatMark.class ) );
		assertTrue( FxLight.live() > lights );
		fall.detach();
		frames( 30 );
		assertEquals( 0, count( floor, MeteorFall.HeatMark.class ) );
		assertEquals( lights, FxLight.live() );
	}

	@Test
	public void theMeteorsHeatGathersOnlyOnItsOwnFloor() throws Exception {
		//a meteor called on another floor (the hero took the stairs before it fell): no heat on this
		//floor's cell of the same number while the buff waits to let itself go
		Group floor = layers[GameScene.Layer.FLOOR.ordinal()];
		//out of sight: no emitter bursts in this bare scene (the heat checks sight itself, each frame)
		level.heroFOV[at( 6, 4 )] = false;
		MeteorFall fall = new MeteorFall();
		fall.set( at( 6, 4 ), 1 );
		Field depth = MeteorFall.class.getDeclaredField( "floorDepth" );
		depth.setAccessible( true );
		depth.setInt( fall, Dungeon.depth + 1 );
		fall.fx( true );
		frames( 3 );
		assertEquals( 0, count( floor, MeteorFall.HeatMark.class ) );
		//on its own floor it gathers
		depth.setInt( fall, Dungeon.depth );
		fall.fx( true );
		frames( 3 );
		assertEquals( 1, count( floor, MeteorFall.HeatMark.class ) );
		fall.fx( false );
		frames( 3 );
		assertEquals( 0, count( floor, MeteorFall.HeatMark.class ) );
	}

	// ------------------------------------------------------------------ co-op

	//the missiles sent to guests since the last drain
	private static int missilesSent() throws Exception {
		JSONArray events = NetVisuals.drainEvents();
		int n = 0;
		for (int i = 0; events != null && i < events.length(); i++){
			if (events.getJSONObject( i ).getString( "t" ).equals( "ms" )) n++;
		}
		return n;
	}

	@Test
	public void aGuestStillSeesTheVolleyLoosedAndTheShotWithoutABow() throws Exception {
		//master sent guests each arrow of Arrow Storm's volley (a MissileSprite) and Charged Shot's
		//arrow without a bow (a Dart's): they are still sent, as the items they stand for
		Hero guest = new Hero();
		guest.pos = at( 1, 1 );
		TestParty.join( guest );
		try {
			NetVisuals.clear();
			ArrowStorm.loose( mobs, 50, 70, 120, 2, 0 );
			assertEquals( 1, missilesSent() );
			ChargedShot.fire( hero, at( 10, 4 ), 1 );
			assertEquals( 1, missilesSent() );
		} finally {
			TestParty.leave();
			NetVisuals.clear();
		}
	}

	@Test
	public void theSlipstreamsSwirlsAreDisposed() throws Exception {
		String src = source( "src/main/java/xyz/gabriwar/warpedpixeldungeon/actors/buffs/SlipstreamWind.java" );
		assertFalse( src.contains( "killAndErase" ) );
		assertTrue( src.contains( "Fx.dispose( w )" ) );
	}

	// ------------------------------------------------------------------ caps

	@Test
	public void theCapsHoldInALoudMoment(){
		//an aim line down a lane longer than any reach: MOST dots
		ArrayList<Integer> lane = new ArrayList<>();
		for (int i = 0; i < 40; i++) lane.add( at( i % W, 2 + i / W ) );
		ChargedShotDraw.AimLine line = ChargedShotDraw.AimLine.show( lane );
		assertEquals( ChargedShotDraw.AimLine.MOST, line.dots() );
		line.release();
		//a meteor striking, an arrow storm's heart, a storm call over five, an echo on six, wolves on four
		CharSprite[] r = new CharSprite[8];
		for (int i = 0; i < r.length; i++) r[i] = rat( at( 5 + i % 4, 3 + i / 4 ) );
		int most = 0, flares = 0, ghosts = 0, lights = 0, rings = 0;
		MeteorFall.impact( at( 6, 6 ) );
		MeteorFall.HeatMark.at( at( 9, 6 ) );
		ArrowRain.stormLands( at( 4, 6 ) );
		EmberArrows.heatRing( at( 5, 7 ) );
		for (int i = 0; i < 5; i++){
			StormCall.struck( r[i].ch.pos );
			mobs.add( new Lightning( hero.pos, r[i].ch.pos, null ).major().tint( Lightning.STORM ) );
		}
		for (int i = 0; i < 6; i++) ArcaneEcho.echo( r[i].ch.pos, hero.pos );
		for (int i = 0; i < 4; i++) WildCall.bite( r[i].ch.pos, hero.pos );
		for (int i = 0; i < 8; i++) HeartseekerArrow.beat( r[i], 2 );
		for (int f = 0; f < 60; f++){
			frames( 1 );
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
			flares = Math.max( flares, PixelFlare.live() );
			ghosts = Math.max( ghosts, Afterimage.live() );
			lights = Math.max( lights, FxLight.live() );
			rings = Math.max( rings, FxRing.live() );
		}
		assertTrue( "particles " + most, most <= FxBudget.soft() );
		assertTrue( "flares " + flares, flares <= PixelFlare.CAP );
		assertTrue( "afterimages " + ghosts, ghosts <= FxBudget.afterimageCap() );
		assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
		assertTrue( "rings " + rings, rings <= FxBudget.ringCap() );
		assertTrue( SkillSpectacleFX.live() <= SkillSpectacleFX.CAP );
	}

	// ------------------------------------------------------------------ the GPU

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		CharSprite a = rat( at( 5, 4 ) );
		CharSprite hs = hero.sprite;
		failOnGl();
		new Lightning( at( 2, 2 ), at( 8, 5 ), null ).tint( Lightning.STORM );
		ChargedShotDraw.AimLine.show( Arrays.asList( at( 4, 4 ), at( 5, 4 ), at( 6, 4 ) ) );
		MeteorFall.HeatMark.at( at( 7, 6 ) );
		MeteorFall.impact( at( 7, 6 ) );
		HeartseekerArrow.beat( a, 1 );
		HeartseekerArrow.bursting( a );
		Fletching.burst( a );
		WildCall.bite( a.ch.pos, hero.pos );
		ArcaneEcho.echo( a.ch.pos, hero.pos );
		Blink.jumped( hs, at( 3, 4 ), at( 5, 4 ) );
		ChargedShot.release( hs, at( 10, 4 ) );
		EmberArrows.heatRing( at( 6, 6 ) );
		ManaShieldWard.broke( hs );
		frames( 40 );
	}

	// ------------------------------------------------------------------ nothing borrowed

	@Test
	public void noneOfTheBorrowedLooksIsLeft() throws Exception {
		String dir = "src/main/java/xyz/gabriwar/warpedpixeldungeon/actors/";
		String[][] banned = {
				{ "hero/skills/Heartseeker.java", "Speck.HEART" },
				{ "buffs/HeartseekerArrow.java", "Speck.HEART", "HEALTH_WARN", "new Flare", "new Dart() )" },
				{ "hero/skills/EmberArrows.java", "MagicMissile", "hero.sprite.emitter().burst( FlameParticle" },
				{ "hero/skills/FrostArrows.java", "hero.sprite.emitter().burst( SnowParticle" },
				{ "hero/skills/Fletching.java", "Speck.WOOL", "SkillFX.streak( enemy.pos, victim.pos, new " },
				{ "buffs/FletchingFeathers.java", "Speck.WOOL" },
				{ "hero/skills/ArrowStorm.java", "MissileSprite", "recycle(" },
				{ "buffs/ArrowRain.java", "RAPTOR", "look, 1 + Random" },
				{ "hero/skills/ChargedShot.java", "Beam", "new Flare", "far, new Dart" },
				{ "buffs/ChargedShotDraw.java", "Icons.TARGET", "new Flare", "Speck.LIGHT", "killAndErase" },
				{ "hero/skills/BearTrap.java", "TRAP_MECHANISM", "SparkParticle", "Speck.STAR" },
				{ "hero/skills/WildCall.java", "Speck.STAR" },
				{ "hero/skills/Poacher.java", "Speck.STENCH" },
				{ "buffs/PoacherBait.java", "Speck.STENCH", "Speck.ROCK", "Sounds.TRAP" },
				{ "hero/skills/MeteorCall.java", "Speck.RED_LIGHT", "new Flare" },
				{ "buffs/MeteorFall.java", "Speck.RED_LIGHT", "BlastParticle", "Explosion", "Sounds.BLAST", "Sounds.ROCKS", "new Bomb(), 1," },
				{ "hero/skills/ArcaneEcho.java", "SparkParticle" },
				{ "hero/skills/Blink.java", "SparkParticle" },
				{ "hero/skills/Spark.java", "SparkParticle", "MagicMissile.MAGIC_MISSILE" },
				{ "hero/skills/Sorcerer.java", "SparkParticle" },
				//the leap's arrival in the bolt's arcane (the snap-back's energy stays: the wand is recharged)
				{ "hero/skills/Wizard.java", "MagicMissile", "next.sprite.centerEmitter().burst( EnergyParticle" },
				{ "hero/skills/PyreAffinity.java", "MagicMissile", "new Flare" },
				{ "hero/skills/CinderTrail.java", "new Flare", "SnowParticle.RISING_FACTORY" },
				{ "hero/skills/SpellBlade.java", "EnergyParticle" },
				{ "hero/skills/StormCall.java", "SparkParticle" },
				{ "hero/skills/SixthSense.java", "hero.sprite.emitter().burst( Speck.factory( Speck.STAR )" },
				{ "buffs/SpiritArmorMotes.java", "MagicMissile" },
				{ "buffs/ManaShieldWard.java", "new Flare" } };
		for (String[] b : banned){
			String src = new String( Files.readAllBytes( new File( dir + b[0] ).toPath() ), "UTF-8" );
			for (int i = 1; i < b.length; i++) assertFalse( b[0] + " uses " + b[i], src.contains( b[i] ) );
		}
		//every storm and arcane lightning tinted
		for (String f : new String[]{ "hero/skills/ArcaneEcho.java", "hero/skills/Blink.java", "hero/skills/Spark.java",
				"hero/skills/Sorcerer.java", "hero/skills/StormCall.java" }){
			String src = source( dir + f );
			Matcher m = Pattern.compile( "new Lightning\\(.*?\\);", Pattern.DOTALL ).matcher( src );
			int n = 0;
			while (m.find()){
				n++;
				assertTrue( f + ": " + m.group(), m.group().contains( ".tint( Lightning." ) );
			}
			assertTrue( f, n > 0 );
		}
		//thrown and fallen at runtime: no item flies
		SkillFX.rain( at( 7, 6 ), new Dart(), SkillFX.Fall.SPIRIT_ARROW, 2, null );
		SkillFX.rain( at( 7, 6 ), new Bomb(), SkillFX.Fall.METEOR, 1, null );
		SkillFX.streak( at( 2, 2 ), at( 9, 6 ), SkillFX.Streak.FEATHER_DART, new Dart(), null );
		SkillFX.streak( hero.sprite, at( 9, 6 ), SkillFX.Streak.SNARE, null );
		ArrowStorm.loose( mobs, 50, 70, 120, 2, 0 );
		frames( 3 );
		assertEquals( 0, count( mobs, MissileSprite.class ) );
		assertEquals( 0, count( mobs, MagicMissile.class ) );
	}
}
