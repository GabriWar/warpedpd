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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
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
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The skills' shared kit (docs/fx/skills.md, the kit): SkillBolt.fly draws the game's Random once,
 * as the MagicMissile it replaces, and lands on its frame at 30, 60 and 144 fps, mote draws none;
 * the new rain draws four an item, returns early as the old one did and lands with the MissileSprite
 * it replaces, a streak with its item's; every SkillBursts factory is registered for co-op and its
 * emit draws none; LanceFX and ShoutFX draw none and keep their caps; a spectral lance keeps to its
 * own cell, a jaw's halves meet and never cross, a roar raises no dust over the one who shouts;
 * a loud moment (War Cry on
 * twelve, a reach-5 thrust through five) stays under the budget's soft cap, the flares' and the
 * afterimages'; nothing touches the GPU while made; and the package's skills no longer borrow the
 * alarm's brackets, a curse's shadow, the level-up's stars or the upright spear.
 */
public class SkillKitTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group mobs;
	private Level level;

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
		Hero hero = new Hero();
		hero.pos = at( 3, 4 );
		Dungeon.hero = hero;
		link( plain(), hero, hero.pos );
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
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
		SkillBolt.reset();
		RuneFX.reset();
		LinkFX.reset();
		SkillRay.reset();
	}

	//an open floor, all of it seen
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

	private CharSprite link( CharSprite s, xyz.gabriwar.warpedpixeldungeon.actors.Char ch, int cell ){
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
			Game.timeTotal += Game.elapsed;
			FxBudget.frame( Game.elapsed );
			WaterFX.frame();
			mobs.update();
			for (Group g : layers) g.update();
		}
	}

	//the frame (1 = the first update) a run's callback comes on, at most `max`
	private int arrival( java.util.function.Consumer<Runnable> start, int max ){
		final int[] at = { -1 }, frame = { 0 };
		start.accept( () -> { if (at[0] < 0) at[0] = frame[0]; } );
		for (frame[0] = 1; frame[0] <= max && at[0] < 0; frame[0]++) frames( 1 );
		return at[0];
	}

	// ------------------------------------------------------------------ the bolt

	@Test
	public void aBoltDrawsTheGamesRandomOnceAsTheMissileDid(){
		PointF from = new PointF( 40, 60 ), to = new PointF( 140, 90 );
		int missile = EnchantFxTest.draws( () -> ((MagicMissile) mobs.recycle( MagicMissile.class ))
				.reset( MagicMissile.SHADOW, new PointF( from ), new PointF( to ), null ) );
		assertEquals( 1, missile );
		for (int look = 0; look < SkillBolt.LOOKS; look++){
			final int l = look;
			assertEquals( "look " + look, missile, EnchantFxTest.draws( () -> {
				SkillBolt.fly( mobs, new PointF( from ), new PointF( to ), l, null );
				frames( 60 );
			} ) );
		}
		//a mote: none at all
		assertEquals( 0, EnchantFxTest.draws( () -> {
			for (int l = 0; l < SkillBolt.LOOKS; l++) SkillBolt.mote( new PointF( from ), new PointF( to ), l, null );
			frames( 60 );
		} ) );
		//nothing to fly in: the pour's one draw, and the callback at once
		final boolean[] called = { false };
		assertEquals( 1, EnchantFxTest.draws( () -> SkillBolt.fly( null, new PointF( from ), new PointF( to ), SkillBolt.HOLY,
				() -> called[0] = true ) ) );
		assertTrue( called[0] );
	}

	@Test
	public void aBoltLandsOnTheMissilesFrame(){
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			for (int look = 0; look < SkillBolt.LOOKS; look++){
				final int l = look;
				int missile = arrival( cb -> ((MagicMissile) mobs.recycle( MagicMissile.class ))
						.reset( MagicMissile.MAGIC_MISSILE, new PointF( 40, 60 ), new PointF( 151, 97 ), cb::run ), 400 );
				int bolt = arrival( cb -> SkillBolt.fly( mobs, new PointF( 40, 60 ), new PointF( 151, 97 ), l, cb::run ), 400 );
				int mote = arrival( cb -> SkillBolt.mote( new PointF( 40, 60 ), new PointF( 151, 97 ), l, cb::run ), 400 );
				assertTrue( missile > 0 );
				assertEquals( "dt " + dt + " look " + look, missile, bolt );
				assertEquals( "dt " + dt + " look " + look, missile, mote );
				frames( 30 );
			}
		}
	}

	// ------------------------------------------------------------------ the rain and the streak

	@Test
	public void theNewRainDrawsAsTheOldAndLandsWithIt(){
		//a MissileSprite's first flight in a run loads what draws once for good (a class's static
		//setup): taken out of the count, as it was long before any skill rained in a game
		((MissileSprite) new Group().recycle( MissileSprite.class )).reset( new PointF( 10, 10 ), new PointF( 60, 90 ), new Dart(), null );
		for (int fall = SkillFX.Fall.METEOR; fall <= SkillFX.Fall.SHOCK; fall++){
			final int f = fall;
			final Dart d = new Dart();
			int old = EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), d, 3, null ) );
			int now = EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), d, f, 3, null ) );
			assertEquals( 12, old );
			assertEquals( "fall " + fall, old, now );
			frames( 120 );
		}
		//the same early returns: nothing to draw as, or nowhere to draw
		final boolean[] called = { false };
		assertEquals( 0, EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), null, SkillFX.Fall.METEOR, 3, () -> called[0] = true ) ) );
		CharSprite hs = Dungeon.hero.sprite;
		mobs.remove( hs );
		final Dart d = new Dart();
		assertEquals( 0, EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), d, SkillFX.Fall.METEOR, 3, () -> called[0] = true ) ) );
		mobs.add( hs );
		assertFalse( called[0] );
		//the same path, the same frame: the same seed for both
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			for (int fall = SkillFX.Fall.METEOR; fall <= SkillFX.Fall.SHOCK; fall++){
				final int f = fall;
				Random.pushGenerator( 77 );
				int old = arrival( cb -> SkillFX.rain( at( 7, 6 ), new Dart(), 1, cb::run ), 400 );
				Random.popGenerator();
				Random.pushGenerator( 77 );
				int now = arrival( cb -> SkillFX.rain( at( 7, 6 ), new Dart(), f, 1, cb::run ), 400 );
				Random.popGenerator();
				assertTrue( old > 0 );
				assertEquals( "dt " + dt + " fall " + fall, old, now );
			}
		}
	}

	@Test
	public void aStreakLandsAsItsItemWould(){
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			for (int look : new int[]{ SkillFX.Streak.COIN, SkillFX.Streak.SPIRIT_ARROW, SkillFX.Streak.FEATHER_DART,
					SkillFX.Streak.PELLET, SkillFX.Streak.HOLY_BLADE, SkillFX.Streak.SNARE }){
				int old = arrival( cb -> SkillFX.streak( at( 2, 2 ), at( 9, 6 ), new Gold(), cb::run ), 400 );
				int now = arrival( cb -> SkillFX.streak( at( 2, 2 ), at( 9, 6 ), look, cb::run ), 400 );
				assertEquals( "dt " + dt + " look " + look, old, now );
				final CharSprite hs = Dungeon.hero.sprite;
				old = arrival( cb -> SkillFX.streak( hs, at( 9, 6 ), new Dart(), cb::run ), 400 );
				now = arrival( cb -> SkillFX.streak( hs, at( 9, 6 ), look, new Dart(), cb::run ), 400 );
				assertEquals( "dt " + dt + " look " + look + " from a sprite", old, now );
			}
		}
		//from a cell to itself: at once, as ever
		final boolean[] called = { false };
		SkillFX.streak( at( 2, 2 ), at( 2, 2 ), SkillFX.Streak.COIN, () -> called[0] = true );
		assertTrue( called[0] );
	}

	@Test
	public void theNewLooksAreNoItems(){
		SkillFX.rain( at( 7, 6 ), new Dart(), SkillFX.Fall.SPIRIT_ARROW, 3, null );
		SkillFX.streak( at( 2, 2 ), at( 9, 6 ), SkillFX.Streak.SNARE, null );
		frames( 3 );
		assertEquals( 0, count( mobs, MissileSprite.class ) );
	}

	// ------------------------------------------------------------------ the bursts

	@Test
	public void everyBurstIsRegisteredAndDrawsNothingOfTheGamesRandom() throws Exception {
		Field f = NetVisuals.class.getDeclaredField( "idByFactory" );
		f.setAccessible( true );
		@SuppressWarnings("unchecked")
		Map<Emitter.Factory, String> ids = (Map<Emitter.Factory, String>) f.get( null );
		assertEquals( 24, SkillBursts.ALL.length );
		HashSet<String> seen = new HashSet<>();
		for (Object[] b : SkillBursts.ALL){
			Emitter.Factory factory = (Emitter.Factory) b[1];
			assertEquals( b[0], ids.get( factory ) );
			assertTrue( seen.add( (String) b[0] ) );
			final Emitter e = new Emitter();
			layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
			assertEquals( (String) b[0], 0, EnchantFxTest.draws( () -> {
				for (int i = 0; i < 10; i++) factory.emit( e, i, 80 + i, 70 );
				frames( 40 );
			} ) );
			//two kit particles an emit at most
			int before = FxBudget.live( FxBudget.PARTICLES );
			factory.emit( e, 0, 80, 70 );
			assertTrue( (String) b[0], FxBudget.live( FxBudget.PARTICLES ) - before <= 2 );
			frames( 60 );
		}
	}

	// ------------------------------------------------------------------ the lance and the shout

	@Test
	public void theLanceAndTheShoutDrawNothingOfTheGamesRandom(){
		CharSprite a = rat( at( 5, 4 ) ), b = rat( at( 7, 4 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			LanceFX.thrust( Dungeon.hero.pos, at( 8, 4 ), 5, LanceFX.STEEL, Dungeon.hero.sprite );
			LanceFX.pierce( a, 0, LanceFX.STEEL );
			LanceFX.spectral( Dungeon.hero.pos, at( 6, 4 ), 0.04f );
			LanceFX.spectral( at( 6, 4 ), at( 6, 4 ), 0 );
			LanceFX.stab( 100, 60, 90, ShoutFX.DREAD, 12, 0.35f );
			ShoutFX.roar( Dungeon.hero.sprite, ShoutFX.MARTIAL, 72 );
			ShoutFX.flinch( a, 50, 70, ShoutFX.MARTIAL, 0.08f );
			ShoutFX.pull( b, 50, 70, ShoutFX.FURY, 0.1f );
			ShoutFX.dread( a, false, 0 );
			ShoutFX.dread( b, true, 0.08f );
			ShoutFX.creep( at( 5, 4 ), at( 7, 4 ), 0 );
			ShoutFX.howl( Dungeon.hero.sprite, ShoutFX.DREAD, 5, 0.06f );
			frames( 90 );
		} ) );
	}

	@Test
	public void lancesKeepToTheirCapAndGo(){
		for (int i = 0; i < 40; i++) LanceFX.thrust( Dungeon.hero.pos, at( 8, 4 ), 5, LanceFX.STEEL, Dungeon.hero.sprite );
		for (int i = 0; i < 40; i++) LanceFX.spectral( Dungeon.hero.pos, at( 4 + i % 5, 4 ), 0 );
		assertEquals( LanceFX.CAP, LanceFX.live() );
		assertEquals( LanceFX.CAP, count( layers[GameScene.Layer.LIGHTS.ordinal()], LanceFX.class ) );
		frames( 60 );
		assertEquals( 0, LanceFX.live() );
		//the timings the lane waits on: a thrust's whole
		assertEquals( 0.035f * 4 + 0.07f + 0.12f, LanceFX.STEP * 4 + LanceFX.HOLD + LanceFX.WITHDRAW, 1e-6f );
	}

	@Test
	public void aSpectralLanceKeepsToItsOwnCell(){
		//the lane's first cell, beside the one who cast it: never drawn over him, and never further
		//back than SPECTRAL_BACK behind its cell's middle (a full lane is a ripple of stabs, not a beam)
		CharSprite hs = Dungeon.hero.sprite;
		int cell = at( 4, 4 );
		float back = SkillVfx.cx( cell ) - LanceFX.SPECTRAL_BACK;
		LanceFX.spectral( Dungeon.hero.pos, cell, 0 );
		int shown = 0;
		for (int f = 0; f < 30; f++){
			frames( 1 );
			for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
				if (!(g instanceof LanceFX) || !g.exists) continue;
				for (Gizmo m : ((LanceFX) g).membersView()){
					if (!(m instanceof Image) || !m.visible || ((Image) m).am <= 0) continue;
					Image p = (Image) m;
					shown++;
					assertTrue( "over the caster at frame " + f, p.x >= hs.x + hs.width() );
					assertTrue( "behind its cell at frame " + f + ": " + p.x, p.x >= back - 0.5f );
				}
			}
		}
		assertTrue( shown > 0 );
	}

	@Test
	public void aJawsHalvesMeetAndNeverCross(){
		//its two crescents close until their tips touch: never past each other (one white blot)
		SkillSpectacleFX.show( SkillSpectacleFX.JAW, at( 6, 4 ) );
		int least = Integer.MAX_VALUE;
		for (int f = 0; f < 30; f++){
			frames( 1 );
			for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
				if (!(g instanceof SkillSpectacleFX) || !g.exists) continue;
				Image upper = (Image) ((SkillSpectacleFX) g).membersView().get( 0 );
				Image lower = (Image) ((SkillSpectacleFX) g).membersView().get( 1 );
				if (!upper.visible) continue;
				least = Math.min( least, Math.round( lower.y - upper.y ) );
			}
		}
		assertEquals( "shut, tip to tip", 2 * SkillSpectacleFX.JAW_SHUT, least );
	}

	@Test
	public void aRoarNeverCloudsTheOneWhoShouts(){
		//on a dry floor the rings are the ground's answer: no dust rising over him
		ShoutFX.roar( Dungeon.hero.sprite, ShoutFX.MARTIAL, 72 );
		int most = 0;
		for (int f = 0; f < 20; f++){
			frames( 1 );
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
		}
		assertEquals( 0, most );
		assertTrue( FxRing.live() > 0 );
	}

	@Test
	public void aShoutFlinchesItsFirstFewInFull(){
		CharSprite[] r = new CharSprite[12];
		for (int i = 0; i < r.length; i++) r[i] = rat( at( 5 + i % 4, 2 + i / 4 ) );
		ShoutFX.roar( Dungeon.hero.sprite, ShoutFX.MARTIAL, 72 );
		for (CharSprite c : r) ShoutFX.flinch( c, 50, 70, ShoutFX.MARTIAL, 0 );
		assertEquals( 2 * ShoutFX.FLINCH_RICH, Afterimage.live() );
		assertTrue( Afterimage.live() <= FxBudget.afterimageCap() );
	}

	// ------------------------------------------------------------------ a loud moment

	@Test
	public void aLoudMomentStaysUnderTheBudget(){
		//War Cry on twelve enemies, then a reach-5 thrust through five
		CharSprite[] r = new CharSprite[12];
		for (int i = 0; i < r.length; i++) r[i] = rat( at( 5 + i % 4, 1 + i / 4 ) );
		CharSprite[] lane = new CharSprite[5];
		for (int i = 0; i < 5; i++) lane[i] = rat( at( 4 + i, 6 ) );
		Dungeon.hero.pos = at( 3, 6 );
		Dungeon.hero.sprite.x = 3 * 16 + 2;
		Dungeon.hero.sprite.y = 6 * 16;
		int most = 0, flares = 0, ghosts = 0, lights = 0;
		for (CharSprite c : r){
			c.emitter().burst( SkillBursts.TREMBLE, 3 );
			ShoutFX.flinch( c, 50, 100, ShoutFX.MARTIAL, 0.05f );
		}
		Dungeon.hero.sprite.emitter().burst( SkillBursts.ROAR, 4 );
		ShoutFX.roar( Dungeon.hero.sprite, ShoutFX.MARTIAL, 72 );
		LanceFX.thrust( Dungeon.hero.pos, at( 8, 6 ), 5, LanceFX.STEEL, Dungeon.hero.sprite );
		for (int i = 0; i < 5; i++){
			Emitter grit = new Emitter();
			layers[GameScene.Layer.EFFECTS.ordinal()].add( grit );
			grit.pos( (4 + i) * 16 + 8, 6 * 16 + 8 );
			grit.burst( SkillBursts.GRIT, 2 );
			LanceFX.pierce( lane[i], 0, LanceFX.STEEL );
		}
		Dungeon.hero.sprite.emitter().burst( SkillBursts.FLOURISH, 3 );
		for (int f = 0; f < 60; f++){
			frames( 1 );
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
			flares = Math.max( flares, PixelFlare.live() );
			ghosts = Math.max( ghosts, Afterimage.live() );
			lights = Math.max( lights, FxLight.live() );
		}
		assertTrue( "particles " + most, most <= FxBudget.soft() );
		assertTrue( "flares " + flares, flares <= PixelFlare.CAP );
		assertTrue( "afterimages " + ghosts, ghosts <= FxBudget.afterimageCap() );
		assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
	}

	// ------------------------------------------------------------------ the GPU

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		CharSprite a = rat( at( 5, 4 ) );
		failOnGl();
		makeEverything( a );
		frames( 40 );
	}

	@Test
	public void noneOfTheNewPicturesDrawsTheGamesRandom(){
		CharSprite a = rat( at( 5, 4 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			makeEverything( a );
			frames( 60 );
		} ) );
	}

	//every new picture of the kit once (none of the game's bursts, none that draw)
	private void makeEverything( CharSprite a ){
		CharSprite hs = Dungeon.hero.sprite;
		LanceFX.thrust( Dungeon.hero.pos, at( 8, 4 ), 5, LanceFX.STEEL, hs );
		LanceFX.pierce( a, 0, LanceFX.STEEL );
		LanceFX.spectral( Dungeon.hero.pos, at( 6, 4 ), 0 );
		LanceFX.stab( 100, 60, -90, LanceFX.STEEL, 18, 0.3f );
		ShoutFX.roar( hs, ShoutFX.MARTIAL, 72 );
		ShoutFX.flinch( a, 50, 70, ShoutFX.MARTIAL, 0 );
		ShoutFX.pull( a, 50, 70, ShoutFX.FURY, 0 );
		ShoutFX.dread( a, false, 0 );
		ShoutFX.creep( at( 5, 4 ), at( 6, 5 ), 0 );
		ShoutFX.howl( hs, ShoutFX.WOLF, 3, 0.05f );
		for (int l = 0; l < SkillBolt.LOOKS; l++) SkillBolt.mote( hs, a, l, null );
		makeTheNewKit( a );
		for (int kind : new int[]{ SkillSpectacleFX.HAMMER, SkillSpectacleFX.SPIRE, SkillSpectacleFX.SABER, SkillSpectacleFX.SHADOW,
				SkillSpectacleFX.RAPTOR, SkillSpectacleFX.JAW, SkillSpectacleFX.WINGS, SkillSpectacleFX.THORN, SkillSpectacleFX.LANCE }){
			SkillSpectacleFX.fly( kind, Dungeon.hero.pos, at( 6, 5 ), 0, 0.5f );
		}
		WarriorImpactFX.show( at( 6, 6 ), true );
		for (int k : new int[]{ SkillField.ARROWS, SkillField.SOUL, SkillField.SMOKE }){
			SkillFieldFX.show( new SkillFieldFX( field( k, new int[]{ at( 2, 8 ), at( 3, 8 ) } ) ) );
		}
		SkillFX.streak( at( 2, 2 ), at( 8, 6 ), SkillFX.Streak.COIN, null );
		SkillFX.streak( hs, at( 8, 6 ), SkillFX.Streak.SNARE, null );
	}

	private static SkillField field( int kind, int[] cells ){
		Bundle b = new Bundle();
		b.put( "kind", kind );
		b.put( "rank", 1 );
		b.put( "remaining", 5 );
		b.put( "age", 0 );
		b.put( "cells", cells );
		b.put( "depth", Dungeon.depth );
		b.put( "branch", Dungeon.branch );
		b.put( "origin", cells[0] );
		SkillField f = new SkillField();
		f.restoreFromBundle( b );
		return f;
	}

	// ------------------------------------------------------------------ nothing borrowed

	@Test
	public void theSpectaclesNoLongerShowTheirIcons(){
		//the upright spear, the falling hammer, the wings and the jaw icons: none of
		//skill_spectacles.png's frames is on show; nor the hammer standing in the crack
		for (int kind = 0; kind <= SkillSpectacleFX.SLASH; kind++){
			if (kind == SkillSpectacleFX.ROCK || kind == SkillSpectacleFX.SHURIKEN) continue;
			SkillSpectacleFX.show( kind, at( 6, 5 ) );
		}
		WarriorImpactFX.show( at( 7, 6 ), true );
		for (int f = 0; f < 20; f++){
			frames( 1 );
			assertEquals( 0, textured( "effects/skill_spectacles.png" ) );
			assertEquals( 0, textured( "effects/warrior_impact.png" ) );
		}
		frames( 60 );
		assertEquals( 0, SkillSpectacleFX.live() );
		assertEquals( 0, LanceFX.live() );
	}

	@Test
	public void thePackagesSkillsNoLongerBorrowTheGamesLooks() throws Exception {
		String dir = "src/main/java/xyz/gabriwar/warpedpixeldungeon/actors/hero/skills/";
		String[][] banned = {
				{ "ImpalingThrust.java", "Speck.STAR", "SkillSpectacleFX.LANCE", "StreakFX" },
				{ "WarCry.java", "Speck.SCREAM" },
				{ "CreepingDread.java", "Speck.SCREAM", "ShadowParticle.CURSE" } };
		for (String[] b : banned){
			String src = new String( Files.readAllBytes( new File( dir + b[0] ).toPath() ), "UTF-8" );
			for (int i = 1; i < b.length; i++) assertFalse( b[0] + " uses " + b[i], src.contains( b[i] ) );
		}
		String seq = new String( Files.readAllBytes( new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/actors/buffs/SkillSequence.java" ).toPath() ), "UTF-8" );
		assertFalse( "the briar's jaw", seq.contains( "SkillSpectacleFX.JAW" ) );
		assertFalse( "the aura off the kit's post", seq.contains( "runOnRenderThread" ) );
		//a swapped burst keeps its quantity (a plain emitter draws two of the game's Random a particle):
		//the proc's tremor is master's one scream, the spill's too
		String dread = new String( Files.readAllBytes( new File( dir + "CreepingDread.java" ).toPath() ), "UTF-8" );
		assertFalse( "the proc's tremor", dread.contains( "SkillBursts.TREMBLE, 2" ) );
		assertEquals( 2, dread.split( "SkillBursts.TREMBLE, 1 \\)", -1 ).length - 1 );
	}

	// ------------------------------------------------------------------ the second pass's kit (rfx)

	//every new piece of the kit once: the motes, the runes' verbs, the links, the rays (none draws)
	private void makeTheNewKit( CharSprite a ){
		CharSprite hs = Dungeon.hero.sprite;
		MoteFX.crosses( hs, 3 );
		MoteFX.greens( 80, 70, 4 );
		MoteFX.gather( hs, 8, SkillPalette.MANA, 18, 0.4f );
		MoteFX.peel( a, 60, 60, 4, SkillPalette.SOUL, 0.3f );
		MoteFX.flick( 90, 70, 6, FxFrames.SPARK_2, SkillPalette.STEEL, 30f, 50f, 4f, 0f, 0.2f, 8f, FxBudget.P2 );
		MoteFX.flick( 90, 70, 3, FxFrames.DROP_2, 0x6CCC3C, (float) -Math.PI / 2, 1.4f, 10f, 20f, 0f, 160f, 0.8f, 8f, FxBudget.P2 );
		MoteFX.glint( 70, 60, FxFrames.STAR4_5, 0xE8F0FF, 0.2f, 30f, 0f );
		RuneFX.ring( hs, 6, SkillPalette.RUNE_ARCANE, 5, 2, 2.4f, 0.6f ).spin( 8f, 0.3f ).release( 40f, 0.25f );
		RuneFX.lock( a, 6, SkillPalette.RUNE_MANA, 8, 4, 0.08f, 0.3f ).lift( -12f, 0.3f );
		RuneFX.lock( 100, 60, 2, SkillPalette.RUNE_MANA, 8, 4, 0.08f, 0.3f, 0f, 1.2f );
		RuneFX.shut( a, 5, SkillPalette.RUNE_VOID, 9, 4.5f, 0.2f );
		RuneFX.scatter( 110, 80, 6, SkillPalette.RUNE_MANA, 10, 0.35f );
		RuneFX.rise( hs, 6, SkillPalette.RUNE_HOLY, 9, 3.5f, 18, 0.6f );
		RuneFX.stamp( 60, 50, FxFrames.RUNE_5[2], 0xF1D99C, 0.35f );
		RuneFX.shed( 70, 70, SkillPalette.RUNE_ARCANE );
		LinkFX.bind( mobs, hs.center(), a.center(), LinkFX.SHADOW, null );
		LinkFX.bind( mobs, hs.center(), a.center(), LinkFX.STEEL, null );
		Gizmo held = LinkFX.held( hs, a, LinkFX.SHADOW );
		LinkFX.drain( a, hs, LinkFX.SOUL_HEAL, 0.3f );
		SkillRay.show( hs, a, SkillRay.HOLY, 0.4f );
		SkillRay.show( hs, a, SkillRay.HEAL, 0.5f );
		SkillRay.stream( hs, a, SkillRay.STREAM_HOLY, 0.4f );
		SkillBolt.mote( a, hs, SkillBolt.SOUL, null ).peel( a, 6 );
		Fx.dispose( held );
	}

	@Test
	public void theNewKitDrawsNothingOfTheGamesRandom(){
		CharSprite a = rat( at( 7, 4 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			makeTheNewKit( a );
			frames( 120 );
		} ) );
	}

	@Test
	public void theNewKitsFilesHaveNoDrawSiteButTheBoltsOnePour() throws Exception {
		//the draw sites the skills' censuses count, in the kit's new files and the bolt's
		String base = "src/main/java/xyz/gabriwar/warpedpixeldungeon/effects/skillfx/";
		Object[][] census = {
				{ "SkillBolt.java", new int[]{ 0, 0, 0, 0, 1 } },
				{ "MoteFX.java", new int[]{ 0, 0, 0, 0, 0 } },
				{ "RuneFX.java", new int[]{ 0, 0, 0, 0, 0 } },
				{ "LinkFX.java", new int[]{ 0, 0, 0, 0, 0 } },
				{ "SkillRay.java", new int[]{ 0, 0, 0, 0, 0 } } };
		String[] sites = { "\\.burst\\s*\\(", "(?<!FxTimeline)\\.start\\s*\\(", "\\.startDelayed\\s*\\(", "\\.pour\\s*\\(",
				"(?<![A-Za-z])Random\\.(Int|Float|NormalIntRange|NormalFloat|IntRange|Long|chances|element|shuffle|oneOf|chance)\\b" };
		for (Object[] row : census){
			String src = code( new String( Files.readAllBytes( new File( base + row[0] ).toPath() ), "UTF-8" ) );
			for (int k = 0; k < sites.length; k++){
				Matcher m = Pattern.compile( sites[k] ).matcher( src );
				int n = 0;
				while (m.find()) n++;
				assertEquals( row[0] + " " + sites[k], ((int[]) row[1])[k], n );
			}
			if (row[0].equals( "SkillBolt.java" )){
				//its reset's one pour (MagicMissile's draw) and the quiet path's delayed start (none)
				assertEquals( 1, src.split( "(?<![A-Za-z.])pour\\s*\\(", -1 ).length - 1 );
				assertEquals( 1, src.split( "(?<![A-Za-z.])startDelayed\\s*\\(", -1 ).length - 1 );
			}
			//no verb the censuses would count as a draw
			assertFalse( row[0] + "", Pattern.compile( "public static \\S+ (burst|start|pour|rain)\\s*\\(" ).matcher( src ).find() );
		}
	}

	@Test
	public void aBoltKeepsItsTrailUnderItsMostAndItsFirstFourAreRich(){
		assertEquals( 48, SkillBolt.TRAIL_MAX );
		assertEquals( 10, SkillBolt.LOOKS );
		assertEquals( 9, SkillBolt.RUNE );
		assertEquals( 0.02f, SkillBolt.TRAIL_EVERY, 0f );
		//one bolt at a time down a long way: its particles never past its most (and its launch's sparks)
		for (int look = 0; look < SkillBolt.LOOKS; look++){
			if (look == SkillBolt.RUNE || look == SkillBolt.SPIRIT) continue;
			resetKit();
			final int l = look;
			int most = 0;
			SkillBolt b = SkillBolt.fly( mobs, new PointF( 20, 60 ), new PointF( 230, 70 ), l, null );
			for (int f = 0; f < 50; f++){
				frames( 1 );
				if (b.on) most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
			}
			assertTrue( "look " + look + ": " + most, most <= SkillBolt.TRAIL_MAX + SkillBolt.LAUNCH_SPARKS );
			assertTrue( "look " + look + " too thin: " + most, most >= 16 );
			frames( 60 );
		}
		//six at once: the first RICH in full, the rest lean
		resetKit();
		SkillBolt[] six = new SkillBolt[6];
		for (int i = 0; i < six.length; i++) six[i] = SkillBolt.fly( mobs, new PointF( 20, 30 + 10 * i ), new PointF( 220, 40 + 10 * i ), SkillBolt.HOLY, null );
		frames( 1 );
		for (int i = 0; i < six.length; i++) assertEquals( "bolt " + i, i < SkillBolt.RICH, six[i].rich() );
		assertEquals( SkillBolt.RICH, SkillBolt.richLive() );
		frames( 120 );
		assertEquals( 0, SkillBolt.richLive() );
	}

	@Test
	public void aCometsSquaresLieAlongTheWayItFlewThatFrame(){
		//at 30 fps a frame holds one or two of the core's emits; each lies where the bolt was at its own
		//moment, spread over the frame's way (never heaped where the frame began, which dotted the comet)
		Game.elapsed = 1 / 30f;
		SkillBolt b = SkillBolt.fly( mobs, new PointF( 20, 60 ), new PointF( 230, 60 ), SkillBolt.HOLY, null );
		java.util.Set<FxParticle> before = java.util.Collections.newSetFromMap( new IdentityHashMap<>() );
		float lead = 0, most = Float.NEGATIVE_INFINITY;
		int n = 0;
		for (int f = 0; f < 28 && b.on; f++){
			before.clear();
			before.addAll( particles() );
			float x0 = b.x, way = SkillBolt.SPEED * Game.elapsed;
			frames( 1 );
			for (FxParticle p : particles()){
				//the core's squares: born this frame, barely drifting (sheds rise, launch sparks fly)
				if (before.contains( p ) || Math.abs( p.vx ) > SkillBolt.CORE_DRIFT + 0.01f || Math.abs( p.vy ) > SkillBolt.CORE_DRIFT + 0.01f) continue;
				lead += p.px - x0;
				most = Math.max( most, p.px - x0 - way );
				n++;
			}
		}
		assertTrue( "core squares seen: " + n, n > 40 );
		//laid on the way flown: their mean lead past the frame's start well over the emit box's own 1 px
		assertTrue( "mean lead " + lead / n, lead / n > 3f );
		//and never past where the bolt's box reached by the frame's end
		assertTrue( "ahead of the head by " + most, most <= 4.5f );
	}

	@Test
	public void theWardBoltFliesAsSpiritVolleyDidAt72b() throws Exception {
		//RUNE pours the ward's own squares into itself (MagicMissile.WARD's interval) and lands with
		//BoltFx's ward: the same draw as the missile, the same frame, carried over the night's tint
		SkillBolt b = SkillBolt.fly( mobs, new PointF( 40, 60 ), new PointF( 150, 80 ), SkillBolt.RUNE, null );
		Field interval = Emitter.class.getDeclaredField( "interval" );
		interval.setAccessible( true );
		assertEquals( 0.015f, (float) interval.get( b ), 0f );
		frames( 10 );
		assertTrue( count( b, MagicMissile.WardParticle.class ) > 4 );
		Field carrier = SkillBolt.class.getDeclaredField( "carrier" );
		carrier.setAccessible( true );
		Gizmo c = (Gizmo) carrier.get( b );
		assertTrue( "carried over the tint", c != null && c.exists && c.parent == layers[GameScene.Layer.LIGHTS.ordinal()] );
		frames( 120 );
		assertFalse( "gone once its squares have", b.exists );
	}

	@Test
	public void aPeelLeavesTheBodyForTheHead(){
		CharSprite a = rat( at( 4, 4 ) );
		SkillBolt.mote( a, rat( at( 10, 4 ) ), SkillBolt.SOUL, null ).peel( a, 6 );
		int near = 0;
		for (int f = 0; f < 12; f++){
			frames( 1 );
			for (FxParticle p : particles()){
				if (Math.abs( p.px - SkillVfx.midX( a ) ) <= a.width() / 2f && p.vx > 0 && p.progress() < 0.3f) near++;
			}
		}
		assertTrue( "motes off the body: " + near, near > 0 );
	}

	// ------------------------------------------------------------------ links

	@Test
	public void aBindBitesOnTheChainsFrameEvenWithNoBite(){
		float[][][] ways = { { { 40, 60 }, { 56, 60 } }, { { 40, 60 }, { 110, 75 } } };
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			for (float[][] w : ways){
				for (int look = 0; look < LinkFX.LOOKS; look++){
					final int l = look;
					int chain = arrival( cb -> mobs.add( new Chains( new PointF( w[0][0], w[0][1] ), new PointF( w[1][0], w[1][1] ),
							Effects.Type.ETHEREAL_CHAIN, cb::run ) ), 600 );
					int bind = arrival( cb -> LinkFX.bind( mobs, new PointF( w[0][0], w[0][1] ), new PointF( w[1][0], w[1][1] ), l, cb::run ), 600 );
					assertTrue( chain > 0 );
					assertEquals( "dt " + dt + " look " + look, chain, bind );
					frames( 60 );
				}
			}
			//no bite at all: it still goes, and nothing breaks
			Gizmo g = LinkFX.bind( mobs, new PointF( 40, 60 ), new PointF( 90, 60 ), LinkFX.STEEL, null );
			frames( (int) (1.5f / dt) );
			assertFalse( g.exists );
		}
		assertEquals( 0, LinkFX.live() );
	}

	@Test
	public void aBindPullsMotesOutOfBothEndsByItsLength(){
		assertEquals( 3, LinkFX.motesASide( 16 ) );
		assertEquals( 7, LinkFX.motesASide( 40 ) );
		assertEquals( 12, LinkFX.motesASide( 70 ) );
		assertEquals( 12, LinkFX.motesASide( 300 ) );
		for (float len : new float[]{ 16, 40, 70 }){
			resetKit();
			float ax = 40, ay = 60, bx = 40 + len, by = 60;
			LinkFX.bind( mobs, new PointF( ax, ay ), new PointF( bx, by ), LinkFX.SHADOW, null );
			IdentityHashMap<FxParticle, Boolean> seen = new IdentityHashMap<>();
			int fromA = 0, fromB = 0;
			float mx = (ax + bx) / 2f;
			//the gather only, up to the bite (its race home and the motes it flicks are not the gather's)
			float bite = (float) Math.hypot( bx - ax, by - ay ) / 320f + 0.2f;
			for (int f = 0; f * Game.elapsed < bite - Game.elapsed; f++){
				frames( 1 );
				for (FxParticle p : particles()){
					if (seen.put( p, true ) != null) continue;
					//newborn: born at an end and bound for the middle (ink peeling off drifts slowly)
					if (Math.abs( p.px - ax ) <= 5 && p.vx > 10) fromA++;
					else if (Math.abs( p.px - bx ) <= 5 && p.vx < -10) fromB++;
				}
				seen.keySet().removeIf( p -> !p.alive );
			}
			int n = LinkFX.motesASide( len );
			assertEquals( "from a, " + len + " px", n, fromA );
			assertEquals( "from b, " + len + " px", n, fromB );
			assertTrue( mx > ax );
			//its bite's motes gone before the next
			frames( 60 );
		}
	}

	@Test
	public void theLinksKeepToTheirCap(){
		final int[] bites = { 0 };
		for (int i = 0; i < 10; i++) LinkFX.bind( mobs, new PointF( 40, 60 + i ), new PointF( 120, 70 ), LinkFX.SHADOW, () -> bites[0]++ );
		assertEquals( LinkFX.CAP, LinkFX.live() );
		frames( 120 );
		//past the cap a bind still bites on its frame
		assertEquals( 10, bites[0] );
		assertEquals( 0, LinkFX.live() );
		CharSprite a = rat( at( 5, 4 ) ), b = rat( at( 8, 5 ) );
		Gizmo held = LinkFX.held( a, b, LinkFX.SHADOW );
		frames( 30 );
		assertEquals( LinkFX.HELD_LINKS, count( layers[GameScene.Layer.LIGHTS.ordinal()], FxPic.class, held ) );
		Fx.dispose( held );
		frames( 2 );
		assertEquals( 0, LinkFX.live() );
	}

	/** A held made while the bonds are full is not dropped for the whole link: it waits and binds
	 *  the pair once a bond has gone (a pair linked in a crowded moment). */
	@Test
	public void aHeldMadeWithTheBondsFullBindsOnceThereIsRoom(){
		for (int i = 0; i < LinkFX.CAP; i++) LinkFX.bind( mobs, new PointF( 40, 60 + i ), new PointF( 120, 70 ), LinkFX.SHADOW, null );
		CharSprite a = rat( at( 5, 4 ) ), b = rat( at( 8, 5 ) );
		Gizmo held = LinkFX.held( a, b, LinkFX.SHADOW );
		frames( 2 );
		assertEquals( LinkFX.CAP, LinkFX.live() );
		assertEquals( "no links while the bonds are full", 0, count( layers[GameScene.Layer.LIGHTS.ordinal()], FxPic.class, held ) );
		//the binds bite and go within a second; the held tries again every HELD_RETRY s
		frames( 60 + Math.round( 60 * 2 * LinkFX.HELD_RETRY ) );
		assertEquals( 1, LinkFX.live() );
		assertEquals( LinkFX.HELD_LINKS, count( layers[GameScene.Layer.LIGHTS.ordinal()], FxPic.class, held ) );
		Fx.dispose( held );
		frames( 2 );
		assertEquals( 0, LinkFX.live() );
	}

	/** A shadow pair's shared cut glints as a cut across the thread, never a red plus: a small red
	 *  plus is the heal's cross, and two linked enemies are not healing each other. */
	@Test
	public void aShadowHeldsGlintIsACutNeverTheHealsPlus() throws Exception {
		CharSprite a = rat( at( 5, 4 ) ), b = rat( at( 9, 4 ) );
		Gizmo held = LinkFX.held( a, b, LinkFX.SHADOW );
		Field shown = FxParticle.class.getDeclaredField( "shown" );
		shown.setAccessible( true );
		int cuts = 0;
		for (int f = 0; f < 90; f++){
			frames( 1 );
			for (FxParticle p : particles()){
				if ((p.colorNow() & 0xFFFFFF) != SkillPalette.BLOOD_LIGHT) continue;
				int[] k = (int[]) shown.get( p );
				assertFalse( "a red plus between the pair", Arrays.equals( k, FxFrames.PLUS_3 ) );
				if (Arrays.equals( k, FxFrames.SPARK_3 )) cuts++;
			}
		}
		assertTrue( "the cut glints", cuts > 0 );
		Fx.dispose( held );
		frames( 30 );
	}

	// ------------------------------------------------------------------ runes, rays, motes

	@Test
	public void theRunesKeepToTheirCapAndGo(){
		RuneFX.Runes[] r = new RuneFX.Runes[12];
		for (int i = 0; i < r.length; i++) r[i] = RuneFX.ring( 80, 70, 20, SkillPalette.RUNE_ARCANE, 5, 2, 2.4f, 0.5f );
		assertEquals( RuneFX.CAP, RuneFX.live() );
		for (int i = 0; i < r.length; i++) assertEquals( i < RuneFX.CAP, r[i].shown() );
		frames( 2 );
		//at most RUNES a pattern
		assertTrue( count( layers[GameScene.Layer.LIGHTS.ordinal()], FxPic.class ) <= RuneFX.CAP * RuneFX.RUNES );
		frames( 60 );
		assertEquals( 0, RuneFX.live() );
		assertEquals( 0, count( layers[GameScene.Layer.LIGHTS.ordinal()], RuneFX.Runes.class ) );
	}

	@Test
	public void raysKeepToTheirFullAndGo(){
		CharSprite a = rat( at( 8, 4 ) );
		for (int i = 0; i < 5; i++) SkillRay.show( Dungeon.hero.sprite, a, SkillRay.HOLY, 0.4f );
		assertEquals( SkillRay.FULL, SkillRay.full() );
		assertEquals( 2 * SkillRay.FULL, FxLight.live() );
		frames( 60 );
		assertEquals( 0, SkillRay.full() );
	}

	@Test
	public void motesKeepToTheirCapsAndFallNoFurtherThanTheirFloor(){
		MoteFX.crosses( 80, 70, 40 );
		assertEquals( MoteFX.CROSSES, FxBudget.live( FxBudget.PARTICLES ) );
		frames( 90 );
		MoteFX.flick( 80, 70, 40, FxFrames.DROP_2, 0x6CCC3C, 40f, 90f, 0f, 300f, 2f, 8f, FxBudget.P2 );
		assertEquals( MoteFX.FLICK, FxBudget.live( FxBudget.PARTICLES ) );
		for (int f = 0; f < 120; f++){
			frames( 1 );
			for (FxParticle p : particles()) assertTrue( "past its floor: " + p.py, p.py <= 78.01f );
		}
		assertEquals( 0, FxBudget.live( FxBudget.PARTICLES ) );
	}

	// ------------------------------------------------------------------ the daze and the steel stay put

	@Test
	public void theDazeIsTheGamesStunAndNothingFlies() throws Exception {
		Field c = Class.forName( "xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Looks$Stunned" ).getDeclaredField( "COLOR" );
		c.setAccessible( true );
		assertEquals( c.getInt( null ), SkillPalette.DAZE );
		assertEquals( 0xE0C369, SkillPalette.DAZE );
		CharSprite a = rat( at( 6, 4 ) );
		Game.elapsed = 1 / 60f;
		for (Emitter.Factory f : new Emitter.Factory[]{ SkillBursts.DAZE, SkillBursts.METAL_SPARK, SkillBursts.STEEL_GLINT, SkillBursts.FLOURISH }){
			resetKit();
			Emitter e = new Emitter();
			layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
			e.pos( a );
			for (int i = 0; i < 6; i++) f.emit( e, i, SkillVfx.midX( a ), SkillVfx.midY( a ) );
			IdentityHashMap<FxParticle, float[]> born = new IdentityHashMap<>();
			for (FxParticle p : particles()) born.put( p, new float[]{ p.px, p.py } );
			assertTrue( born.size() > 0 );
			float most = 0;
			for (int k = 0; k < 72; k++){
				frames( 1 );
				for (FxParticle p : particles()){
					float[] b = born.get( p );
					if (b != null) most = Math.max( most, (float) Math.hypot( p.px - b[0], p.py - b[1] ) );
				}
			}
			assertTrue( f + ": " + most, most <= 16f );
			if (f == SkillBursts.DAZE) assertTrue( "a daze stays where it is: " + most, most <= 5.5f );
		}
	}

	// ------------------------------------------------------------------ colours by meaning

	@Test
	public void noRampStartsWhiteAndTheColoursFollowMeaning() throws Exception {
		for (Field f : SkillBursts.class.getFields()){
			if (f.getType() != Ramp.class) continue;
			assertTrue( "SkillBursts." + f.getName(), ((Ramp) f.get( null )).stop( 0 ) != 0xFFFFFF );
		}
		for (Object[] r : SkillPalette.RAMPS){
			Ramp ramp = (Ramp) r[1];
			assertTrue( (String) r[0], ramp.stop( 0 ) != 0xFFFFFF );
			//the palest stop is a flash: from a quarter of a life on, nearer the next stop
			if (ramp.size() >= 3) assertTrue( (String) r[0], dist( ramp.at( 0.26f ), ramp.stop( 1 ) ) <= dist( ramp.at( 0.26f ), ramp.stop( 0 ) ) );
		}
		//a heal never paler than A8FFC0; blood and the heart crimson, never pink
		assertEquals( 0xA8FFC0, SkillPalette.HEAL.stop( 0 ) );
		assertEquals( SkillPalette.HEAL, SkillBursts.HEAL );
		assertEquals( SkillPalette.BLOOD, SkillBursts.BLOOD );
		assertEquals( SkillPalette.FIRE, SkillBursts.FIRE );
		assertEquals( SkillPalette.LIGHTNING, SkillBursts.STORM );
		assertEquals( SkillPalette.HOLY, SkillBursts.HOLY );
		assertEquals( SkillPalette.SOUL, SkillBursts.SOUL );
		assertTrue( Arrays.equals( new int[]{ 0xFFE0D0, 0xFF5A4A, 0xCC1111, 0x6A0A0A }, ShoutFX.FURY ) );
		assertEquals( 0xFF8418, School.FIRE.color );
		assertEquals( 0x9070F0, School.ARCANE.color );
		assertEquals( 0x7FA6FF, School.STORM.color );
		assertEquals( 0xA8D880, School.NATURE.color );
		assertEquals( 0xB89CE4, School.SHADOW.color );
		assertTrue( Arrays.equals( new int[]{ 0xFFFFFF, 0xE4F0FF, 0x7FA6FF, 0x6A90FF, 0xA8C8FF }, Lightning.STORM ) );
		assertTrue( Arrays.equals( new int[]{ 0xFFFFFF, 0xE0D4FF, 0x9070F0, 0x5A38C0, 0xB8A0FF }, Lightning.ARCANE ) );
		//a pulse starts in its own colour's pale, never a white ping
		PulseRingFX.at( at( 6, 4 ), SkillPalette.HEAL_LIGHT, 20, 0.5f );
		frames( 1 );
		int rings = 0;
		for (Gizmo g : layers[GameScene.Layer.FLOOR.ordinal()].membersView()){
			if (!(g instanceof FxRing) || !g.alive) continue;
			FxRing r = (FxRing) g;
			assertFalse( "a white ring", r.rm > 0.99f && r.gm > 0.99f && r.bm > 0.99f && r.ra + r.ga + r.ba < 0.01f );
			rings++;
		}
		assertTrue( rings > 0 );
	}

	@Test
	public void theNewKitsLightsAreAtMostSixTenths() throws Exception {
		CharSprite a = rat( at( 8, 4 ) );
		Field level = FxLight.class.getDeclaredField( "level" ), core = FxLight.class.getDeclaredField( "core" );
		level.setAccessible( true );
		core.setAccessible( true );
		Runnable[] pieces = {
				() -> makeTheNewKit( a ),
				//every bolt but the ward's (BoltFx's own, kept exact) and the old spirit, up to its arrival
				() -> { for (int l = 0; l < SkillBolt.LOOKS; l++) if (l != SkillBolt.RUNE && l != SkillBolt.SPIRIT && l != SkillBolt.HOLY)
						SkillBolt.fly( mobs, new PointF( 20, 40 + 8 * l ), new PointF( 230, 60 ), l, null ); },
				() -> SkillBolt.fly( mobs, new PointF( 20, 40 ), new PointF( 230, 60 ), SkillBolt.HOLY, null ) };
		int[] upTo = { 90, 90, 50 };
		for (int k = 0; k < pieces.length; k++){
			resetKit();
			pieces[k].run();
			for (int f = 0; f < upTo[k]; f++){
				frames( 1 );
				for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
					if (!(g instanceof FxLight) || !g.alive || core.getBoolean( g )) continue;
					assertTrue( "piece " + k + ": " + level.getFloat( g ), level.getFloat( g ) <= 0.6f + 1e-4f );
				}
			}
			frames( 120 );
		}
	}

	// ------------------------------------------------------------------ the three restored

	/** f0783eacd's draws and next value (x 1e6) of the three restored skills, measured by RfxKitProcs
	 *  in a checkout of it (RFX_BASELINE=1). */
	static final Object[][] RESTORED_BASELINE = {
			{ "righteous-strikes", 81, 869072 }, { "retribution", 13, 636942 },
			{ "regeneration-growth", 7, 531647 }, { "regeneration-kill", 13, 636942 } };

	@Test
	public void theThreeRestoredDrawTheGamesRandomAsF0783Did() throws Exception {
		GameScene.useLayersForTests( null );
		LinkedHashMap<String, long[]> now = RfxKitProcs.measure();
		GameScene.useLayersForTests( layers );
		assertEquals( RESTORED_BASELINE.length, now.size() );
		for (Object[] row : RESTORED_BASELINE){
			long[] n = now.get( (String) row[0] );
			assertEquals( row[0] + ": draws", (long) (Integer) row[1], n[0] );
			assertEquals( row[0] + ": the next value", (long) (Integer) row[2], n[1] );
		}
		//and every draw site where f0783 had it: bursts only (quantities as they were)
		String dir = "src/main/java/xyz/gabriwar/warpedpixeldungeon/actors/hero/skills/";
		Object[][] census = { { "RighteousStrikes.java", 3 }, { "Retribution.java", 2 }, { "Regeneration.java", 2 } };
		for (Object[] row : census){
			String src = code( new String( Files.readAllBytes( new File( dir + row[0] ).toPath() ), "UTF-8" ) );
			Matcher m = Pattern.compile( "\\.burst\\s*\\(" ).matcher( src );
			int n = 0;
			while (m.find()) n++;
			assertEquals( row[0] + " bursts", (int) (Integer) row[1], n );
			for (String draw : new String[]{ "(?<!FxTimeline)\\.start\\s*\\(", "\\.pour\\s*\\(", "(?<![A-Za-z])Random\\.", "Splash\\.at", "SkillBolt\\.fly" }){
				assertFalse( row[0] + " " + draw, Pattern.compile( draw ).matcher( src ).find() );
			}
		}
		String rs = code( new String( Files.readAllBytes( new File( dir + "RighteousStrikes.java" ).toPath() ), "UTF-8" ) );
		String re = code( new String( Files.readAllBytes( new File( dir + "Retribution.java" ).toPath() ), "UTF-8" ) );
		String rg = code( new String( Files.readAllBytes( new File( dir + "Regeneration.java" ).toPath() ), "UTF-8" ) );
		//72b40617b's looks back: the light rays, the flares, the growth's crosses; never a star speck
		assertTrue( rs.contains( "new Flare( 6, 20 ).color( 0xFFEE88, true )" ) && rs.contains( "new Beam.LightRay(" ) );
		assertTrue( re.contains( "new Beam.LightRay(" ) );
		assertTrue( rg.contains( "new Flare( 5, 16 ).color( 0x66FF66, true )" ) && rg.contains( "Speck.factory( Speck.HEALING ), 3 )" ) );
		for (String src : new String[]{ rs, re, rg }){
			assertFalse( src.contains( "Speck.STAR" ) );
			assertFalse( src.contains( "PixelFlare" ) );
			assertFalse( src.contains( "StreakFX" ) );
		}
		//the flares and rays made on the render thread (the procs run on the actor thread)
		assertTrue( rs.contains( "Fx.post(" ) && re.contains( "Fx.post(" ) && rg.contains( "Fx.post(" ) );
	}

	// ------------------------------------------------------------------ rfx helpers

	//every live kit particle on the stand-in layers
	private List<FxParticle> particles(){
		List<FxParticle> out = new ArrayList<>();
		for (Group g : layers) collect( g, out );
		return out;
	}

	private static void collect( Group g, List<FxParticle> out ){
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof FxParticle) out.add( (FxParticle) m );
			else if (m instanceof Group) collect( (Group) m, out );
		}
	}

	private static int count( Group g, Class<?> c, Gizmo within ){
		for (Gizmo m : g.membersView()) if (m == within) return count( (Group) m, c );
		return 0;
	}

	private static float dist( int a, int b ){
		int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF), dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF), db = (a & 0xFF) - (b & 0xFF);
		return (float) Math.sqrt( dr * dr + dg * dg + db * db );
	}

	//a source without its comments
	private static String code( String src ){
		return src.replaceAll( "(?s)/\\*.*?\\*/", "" ).replaceAll( "//[^\n]*", "" );
	}

	// ------------------------------------------------------------------ helpers

	private static int count( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.exists && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += count( (Group) m, c );
		}
		return n;
	}

	private int textured( String asset ){
		if (!TextureCache.contains( asset )) return 0;
		int n = count( mobs, asset );
		for (Group g : layers) n += count( g, asset );
		return n;
	}

	private static int count( Group g, String asset ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive || !m.visible) continue;
			if (m instanceof Group) n += count( (Group) m, asset );
			else if (m instanceof Image && ((Image) m).texture == TextureCache.get( asset )) n++;
		}
		return n;
	}
}
