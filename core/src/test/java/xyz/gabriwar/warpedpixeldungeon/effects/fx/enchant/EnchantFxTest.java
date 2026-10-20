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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.ItemGhost;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HolyFx;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SummonElemental;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Dazzling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The enchantments' pictures, headless (stand-in layers for the game's scene, a GPU that says yes):
 * every family at every tier, plain, as an echo and hexed, draws nothing of the game's Random and
 * keeps to its caps (its own quads, two lights, one decal; an echo seven, a join five); a fourth rich
 * proc in a quarter second plays minimal; a proc from the actor thread posts itself and stamps the
 * blow before the blow's star reads it; a blow ending in its proc draws the stamped star and no
 * blood burst; one sting a blow, an echo's at three tenths; the glow profiles bounded and pure; the
 * enchantments' states and every charm's hearts.
 */
public class EnchantFxTest {

	static final int W = 16;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	/**
	 * A headless stage the enchantments' tests share (EnchantNetTest, EnchantPreviewDump): stand-in
	 * layers, a GPU that says yes, a room of floor with a pool (columns 9-12, rows 4-8) all seen,
	 * the governor at FULL, the kit's counts at zero, the clock held where the test puts it.
	 */
	public static final class Rig {

		private GL20 savedGl, savedGl20;
		private Camera savedCam;
		private float savedElapsed, savedTime;
		private Level savedLevel;
		private int savedDepth;
		private Hero savedHero;
		public Group[] layers;
		public Group stage;

		public void up(){
			savedGl = Gdx.gl;
			savedGl20 = Gdx.gl20;
			savedCam = Camera.main;
			savedElapsed = Game.elapsed;
			savedTime = Game.timeTotal;
			savedLevel = Dungeon.level;
			savedDepth = Dungeon.depth;
			//the rats here are depth 1's: an earlier test may leave a deep depth, whose rats hit too
			//surely for Stone to turn a point
			Dungeon.depth = 1;
			savedHero = Dungeon.hero;
			yesGl();
			Camera.main = new Camera( 0, 0, 320, 180, 1 );
			Camera.main.fullScreen = true;
			Emitter.freezeEmitters = false;
			Game.elapsed = 1 / 30f;
			Game.timeTotal = 100f;
			layers( true );
			Dungeon.level = room();
			Dungeon.hero = null;
			stage = new Group();
		}

		/** Fresh layers (or none), the kit's counts and statics as a new scene has them. */
		public void layers( boolean on ){
			FxBudget.reset();
			fullTier();
			FxLight.reset();
			FxDecal.reset();
			FxRing.reset();
			Afterimage.reset();
			ItemGhost.reset();
			WaterFX.sceneCreated();
			EnchantFx.reset();
			FxRandom.seedForTests( 7 );
			if (on){
				layers = new Group[GameScene.Layer.values().length];
				for (int i = 0; i < layers.length; i++) layers[i] = new Group();
				GameScene.useLayersForTests( layers );
			} else {
				layers = null;
				GameScene.useLayersForTests( null );
			}
			Fx.sceneCreated();
		}

		public void down(){
			GameScene.useLayersForTests( null );
			//forget the carriers made on the stand-in layers: a later test's world burst (a surprise's
			//ring) would go into an emitter no one updates and stay counted alive in the budget
			Fx.sceneCreated();
			FxBudget.reset();
			FxLight.reset();
			FxDecal.reset();
			FxRing.reset();
			Afterimage.reset();
			ItemGhost.reset();
			WaterFX.sceneCreated();
			EnchantFx.reset();
			Sting.out = xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound::play;
			Gdx.gl = savedGl;
			Gdx.gl20 = savedGl20;
			Camera.main = savedCam;
			Game.elapsed = savedElapsed;
			Game.timeTotal = savedTime;
			Dungeon.level = savedLevel;
			Dungeon.depth = savedDepth;
			Dungeon.hero = savedHero;
		}

		/** A rat's sprite on the stage at a cell, linked by hand to its character. */
		public CharSprite rat( int cell ){
			Rat ch = new Rat();
			ch.state = ch.PASSIVE;
			ch.HP = ch.HT = 50;
			ch.pos = cell;
			CharSprite s = new RatSprite();
			s.ch = ch;
			ch.sprite = s;
			stage.add( s );
			s.place( cell );
			s.visible = true;
			return s;
		}

		/** n frames of the stage and every layer; the clock moves only if `clock`. */
		public void frames( int n, boolean clock ){
			for (int i = 0; i < n; i++){
				FxBudget.frame( 1 / 60f );
				WaterFX.frame();
				if (clock) Game.timeTotal += Game.elapsed;
				stage.update();
				if (layers != null) for (Group g : layers) g.update();
			}
		}

		/** The pictures alive now on the layers and the stage (not the sprites), but lights and decals. */
		public void alive( IdentityHashMap<Gizmo, Boolean> into ){
			if (layers != null) for (Group g : layers) collect( g, into );
			collect( stage, into );
		}

		private static void collect( Group g, IdentityHashMap<Gizmo, Boolean> into ){
			for (Gizmo m : g.membersView()){
				if (m == null || !m.exists || !m.alive) continue;
				if (m instanceof Group){
					collect( (Group) m, into );
				} else if (m instanceof Visual && !(m instanceof CharSprite) && !(m instanceof FxLight) && !(m instanceof FxDecal)){
					into.put( m, Boolean.TRUE );
				}
			}
		}

		/**
		 * What a run makes: every picture that comes alive (a quad) over `frames` frames after it,
		 * and the most lights and decals over the ones before it at once. Run at a held clock.
		 */
		public int[] made( Runnable run, int frames ){
			IdentityHashMap<Gizmo, Boolean> before = new IdentityHashMap<>(), now = new IdentityHashMap<>();
			alive( before );
			int lights0 = FxLight.live(), decals0 = FxDecal.live();
			run.run();
			int quads = 0, lights = 0, decals = 0;
			for (int f = 0; f <= frames; f++){
				now.clear();
				alive( now );
				for (Gizmo g : now.keySet()) if (!before.containsKey( g )) quads++;
				lights = Math.max( lights, FxLight.live() - lights0 );
				decals = Math.max( decals, FxDecal.live() - decals0 );
				before.clear();
				before.putAll( now );
				frames( 1, false );
			}
			return new int[]{ quads, lights, decals };
		}
	}

	static void fullTier(){
		try {
			Method m = FxBudget.class.getDeclaredMethod( "resetGovernor" );
			m.setAccessible( true );
			m.invoke( null );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	//a room with a pool (columns 9-12, rows 4-8); all of it seen
	static Level room(){
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
		for (int y = 4; y <= 8; y++) for (int x = 9; x <= 12; x++) l.map[x + y * W] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	public static int at( int x, int y ){
		return x + y * W;
	}

	static void yesGl(){
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

	/** How many of the game's Random a run draws (the next number after it, against a fresh
	 *  generator of the same seed drawn k times). */
	public static int draws( Runnable run ){
		Random.pushGenerator( 1234 );
		run.run();
		float next = Random.Float();
		Random.popGenerator();
		for (int k = 0; k < 64; k++){
			Random.pushGenerator( 1234 );
			for (int i = 0; i < k; i++) Random.Float();
			float f = Random.Float();
			Random.popGenerator();
			if (f == next) return k;
		}
		return -1;
	}

	/** The first look of a family, curses aside. */
	public static EnchantLook face( Family f ){
		for (EnchantLook l : EnchantLooks.all()) if (l.fam == f && !l.curse) return l;
		throw new AssertionError( f );
	}

	/** A test's ear: every sting played. */
	static final class Ear implements Sting.Out {
		final List<Object[]> heard = new ArrayList<>();

		@Override
		public void play( String sample, Char at, float volume, float pitch ){
			heard.add( new Object[]{ sample, at, volume, pitch } );
		}
	}

	private Rig rig;
	private Ear ear;

	@Before
	public void setUp(){
		rig = new Rig();
		rig.up();
		ear = new Ear();
		Sting.out = ear;
	}

	@After
	public void tearDown(){
		rig.down();
	}

	private static final int[] FLAGS = { 0, EnchantFx.ECHO, EnchantFx.CURSE };

	//a proc of a look at a tier, as the game calls it: the defender dead first for a kill
	private static void proc( EnchantLook l, CharSprite a, CharSprite d, int tier, int flags ){
		d.ch.HP = tier == EnchantFx.FINISHER ? 0 : d.ch.HT;
		EnchantFx.proc( l, a.ch, d.ch, EnchantFx.strengthOf( tier ), 1, flags );
	}

	// ------------------------------------------------------------------ the game's Random

	@Test
	public void everyFamilyAtEveryTierDrawsNothingOfTheGamesRandom(){
		for (boolean layers : new boolean[]{ true, false }){
			rig.layers( layers );
			CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), w = rig.rat( at( 10, 6 ) );
			for (Family f : Family.values()){
				EnchantLook l = face( f );
				for (int tier = EnchantFx.WEAK; tier <= EnchantFx.FINISHER; tier++){
					for (int flags : FLAGS){
						final int t = tier, fl = flags;
						assertEquals( f + " tier " + tier + " flags " + flags + (layers ? "" : " (no layers)"), 0, draws( () -> {
							proc( l, a, d, t, fl );
							proc( l, a, w, t, fl );
							EnchantFx.ward( l, d.ch, a.ch, EnchantFx.strengthOf( t ), 1, fl );
							EnchantFx.passive( l, d.ch, EnchantFx.DODGE, 0.5f );
							EnchantFx.passive( l, d.ch, EnchantFx.STEP, 0.5f );
							EnchantFx.blowEnded( a.ch, d.ch );
							EnchantFx.gather( l, 40, 40, 1f );
							EnchantFx.finale( l, 40, 40, 0.5f );
							rig.frames( 30, true );
						} ) );
					}
				}
			}
			for (EnchantLook l : EnchantLooks.all()){
				assertEquals( l.toString(), 0, draws( () -> {
					EnchantFx.proc( l, a.ch, d.ch, 0.7f, 2, l.curse ? EnchantFx.CURSE : 0 );
					rig.frames( 10, true );
				} ) );
			}
			a.ch.HP = d.ch.HP = 50;
		}
	}

	@Test
	public void aSwingDrawsNothingOfTheGamesRandom(){
		Hero h = new Hero();
		Dungeon.hero = h;
		Sword sword = new Sword();
		sword.enchant( new Blazing() );
		h.belongings.weapon = sword;
		h.pos = at( 5, 6 );
		//(a mob's sprite stands in for his, off the stage: only where it stands is read)
		CharSprite hs = new RatSprite();
		hs.ch = h;
		h.sprite = hs;
		hs.place( h.pos );
		hs.visible = true;
		CharSprite d = rig.rat( at( 6, 6 ) );
		assertEquals( 0, draws( () -> {
			EnchantFx.swing( h, d.ch, true );
			EnchantFx.swing( h, d.ch, false );
			rig.frames( 20, true );
		} ) );
		//a landed swing leaves its tint for the blow's star; its trail is its motes
		int[] m = rig.made( () -> EnchantFx.swing( h, d.ch, true ), 15 );
		assertTrue( "a trail: " + m[0], m[0] >= EnchantFx.SWING && m[0] <= EnchantFx.SWING_MOST + 2 );
		assertSame( EnchantLooks.of( Blazing.class ), EnchantState.get( d ).swingLook );
		//an unenchanted weapon: no trail and no tint
		h.belongings.weapon = new Sword();
		EnchantState.get( d ).swingLook = null;
		m = rig.made( () -> EnchantFx.swing( h, d.ch, true ), 15 );
		assertEquals( 0, m[0] );
		assertNull( EnchantState.get( d ).swingLook );
		Dungeon.hero = null;
	}

	// ------------------------------------------------------------------ caps

	@Test
	public void everyFamilyKeepsToItsCaps(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), wet = rig.rat( at( 10, 6 ) );
		for (Family f : Family.values()){
			EnchantLook l = face( f );
			for (CharSprite def : new CharSprite[]{ d, wet }){
				for (int tier = EnchantFx.WEAK; tier <= EnchantFx.FINISHER; tier++){
					for (int flags : FLAGS){
						Game.timeTotal += 2f;
						final int t = tier, fl = flags;
						int[] m = rig.made( () -> proc( l, a, def, t, fl ), 75 );
						int cap = (fl & EnchantFx.ECHO) != 0 ? EnchantFx.ECHO_CAP : EnchantFx.CAP[t];
						String what = f + " tier " + t + " flags " + fl + (def == wet ? " on water" : "");
						assertTrue( what + ": " + m[0] + " quads", m[0] <= cap );
						assertTrue( what + ": something shows", m[0] > 0 );
						assertTrue( what + ": " + m[1] + " lights", m[1] <= EnchantFx.LIGHTS );
						assertTrue( what + ": " + m[2] + " decals", m[2] <= EnchantFx.DECALS );
						rig.frames( 90, false );
					}
				}
			}
		}
		d.ch.HP = wet.ch.HP = 50;
	}

	@Test
	public void aThirdProcOnABlowJoinsAndAFourthRichStartPlaysMinimal(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		EnchantLook blazing = EnchantLooks.of( Blazing.class );
		//the clock held: one blow; the first two play, their pictures gone, then the third joins
		Game.timeTotal += 5f;
		proc( blazing, a, d, EnchantFx.STRONG, 0 );
		proc( face( Family.STORM ), a, d, EnchantFx.STRONG, 0 );
		assertSame( "the first owns the stamp", blazing, d.blowLook );
		rig.frames( 90, false );
		int[] join = rig.made( () -> proc( face( Family.VENOM ), a, d, EnchantFx.STRONG, 0 ), 75 );
		assertTrue( "a join: " + join[0], join[0] > 0 && join[0] <= EnchantFx.JOIN_CAP );
		assertEquals( "a join throws no light", 0, join[1] );
		assertSame( blazing, d.blowLook );

		//three rich starts in the window, then a fourth on another body: minimal
		Game.timeTotal += 5f;
		CharSprite[] bodies = { rig.rat( at( 3, 3 ) ), rig.rat( at( 5, 3 ) ), rig.rat( at( 7, 3 ) ), rig.rat( at( 3, 10 ) ) };
		int[] rich = rig.made( () -> proc( face( Family.EMBER ), a, bodies[0], EnchantFx.STRONG, 0 ), 60 );
		assertTrue( "a rich one: " + rich[0], rich[0] > EnchantFx.MINIMAL_CAP );
		proc( face( Family.EMBER ), a, bodies[1], EnchantFx.STRONG, 0 );
		proc( face( Family.EMBER ), a, bodies[2], EnchantFx.STRONG, 0 );
		rig.frames( 90, false );
		int[] fourth = rig.made( () -> proc( face( Family.EMBER ), a, bodies[3], EnchantFx.STRONG, 0 ), 60 );
		assertTrue( "the fourth: " + fourth[0], fourth[0] > 0 && fourth[0] <= EnchantFx.MINIMAL_CAP );
		//past the window, rich again
		Game.timeTotal += EnchantFx.RICH_WINDOW + 0.01f;
		int[] again = rig.made( () -> proc( face( Family.EMBER ), a, bodies[3], EnchantFx.STRONG, 0 ), 60 );
		assertTrue( "rich again: " + again[0], again[0] > EnchantFx.MINIMAL_CAP );
		//a multi-target proc's fourth target: minimal
		Game.timeTotal += 5f;
		int[] multi = rig.made( () -> proc( face( Family.STORM ), a, bodies[0], EnchantFx.MID, EnchantFx.multi( 3 ) ), 60 );
		assertTrue( "a fourth target: " + multi[0], multi[0] <= EnchantFx.MINIMAL_CAP );
	}

	@Test
	public void quietMomentsTakeNoneOfTheRichStarts(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), w = rig.rat( at( 3, 3 ) );
		EnchantLook thorns = EnchantLooks.of( Thorns.class );
		Game.timeTotal += 5f;
		//a wearer walking and ticking all through the window
		for (int i = 0; i < 5; i++){
			EnchantFx.passive( thorns, w.ch, EnchantFx.TICK, 0.5f );
			EnchantFx.passive( thorns, w.ch, EnchantFx.STEP, 0.5f );
		}
		int[] proc = rig.made( () -> proc( face( Family.EMBER ), a, d, EnchantFx.STRONG, 0 ), 60 );
		assertTrue( "the proc still rich: " + proc[0], proc[0] > EnchantFx.MINIMAL_CAP );
	}

	@Test
	public void aCursesBlowIsDrawnInVoidAndThrowsNoBrightLight(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		EnchantLook dazzling = EnchantLooks.of( Dazzling.class );
		assertSame( Element.VOID, dazzling.strike );
		assertEquals( Element.VOID.glint, dazzling.star.stop( 0 ) );
		assertEquals( Element.VOID.light, dazzling.star.stop( dazzling.star.size() - 1 ) );
		assertSame( "a good enchantment's own", EnchantLooks.of( Blazing.class ).el, EnchantLooks.of( Blazing.class ).strike );
		Game.timeTotal += 5f;
		rig.made( () -> {
			proc( dazzling, a, d, EnchantFx.STRONG, EnchantFx.CURSE );
			d.hitFx( a.x + 8, a.y + 8, 4, HitFx.SLASH, false );
		}, 1 );
		assertSame( dazzling, d.blowLook );
		assertTrue( "its star in VOID", starIn( dazzling.star ) );
		//a hex of a family that throws no light of its own (Annoying's FORCE): its dim glow and the
		//blow's bloom, no pool under it
		rig.frames( 90, false );
		Game.timeTotal += 5f;
		EnchantLook annoying = EnchantLooks.of( Annoying.class );
		int[] m = rig.made( () -> {
			proc( annoying, a, d, EnchantFx.STRONG, EnchantFx.CURSE );
			d.hitFx( a.x + 8, a.y + 8, 4, HitFx.SLASH, false );
		}, 3 );
		assertEquals( "the hex's glow and the bloom", 2, m[1] );
	}

	@Test
	public void itemGhostsAndAfterimagesShareOneCap(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		for (int i = 0; i < ItemGhost.MOST; i++){
			assertNotNull( ItemGhost.of( ItemSpriteSheet.SWORD, 40, 40, 0, 0xFFFFFF, 0.5f, 5f, 0, 0 ) );
		}
		for (int i = 0; i < 40; i++) Afterimage.of( s, 0xFFFFFF, 0.5f, 5f, 0, 0 );
		assertEquals( ItemGhost.MOST, ItemGhost.live() );
		assertTrue( Afterimage.live() + " + " + ItemGhost.live(), Afterimage.live() + ItemGhost.live() <= FxBudget.afterimageCap() );
		assertTrue( "the characters' still come", Afterimage.live() > 0 );
	}

	// ------------------------------------------------------------------ threads, the stamp, the end

	@Test
	public void aProcFromTheActorThreadPostsAndStampsBeforeTheStarReadsIt() throws Exception {
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		EnchantLook blazing = EnchantLooks.of( Blazing.class );
		final List<Runnable> posted = new ArrayList<>();
		Application saved = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				(p, m, args) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) args[0] );
						}
						return null;
					}
					return saved != null ? m.invoke( saved, args ) : null;
				} );
		try {
			Thread actor = new Thread( () -> {
				EnchantFx.proc( blazing, a.ch, d.ch, 0.5f, 0, 0 );
				d.hitFx( a.x + 8, a.y + 8, 4, HitFx.SLASH, false );
			} );
			actor.start();
			actor.join();
			assertNull( "nothing drawn off the render thread", d.blowLook );
			assertEquals( "the proc and the star, posted", 2, posted.size() );
			for (Runnable r : posted) r.run();
			posted.clear();
		} finally {
			Gdx.app = saved;
		}
		assertSame( blazing, d.blowLook );
		assertTrue( d.blowUntil > Game.timeTotal );
		rig.frames( 1, false );
		assertTrue( "the star white to the enchantment's light", starIn( blazing.star ) );
		assertFalse( starIn( new Ramp( 0xFFFFFF, d.blood() & 0xFFFFFF ) ) );
	}

	//a living light particle of the stand-in layers coloured along this ramp now
	private boolean starIn( Ramp ramp ){
		for (Gizmo g : rig.layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (!(g instanceof Group)) continue;
			for (Gizmo m : ((Group) g).membersView()){
				if (!(m instanceof FxParticle) || !m.alive) continue;
				FxParticle p = (FxParticle) m;
				if (p.priority() == FxBudget.P0 && p.colorNow() == ramp.at( p.progress() )) return true;
			}
		}
		return false;
	}

	@Test
	public void aBlowEndingInItsProcDrawsTheStampedStarAndNoBloodBurst(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		EnchantLook venom = EnchantLooks.of( Venomous.class );
		assertEquals( "no blood burst (Splash draws the game's Random)", 0, draws( () -> {
			proc( venom, a, d, EnchantFx.FINISHER, 0 );
			EnchantFx.blowEnded( a.ch, d.ch );
			rig.frames( 1, false );
		} ) );
		assertTrue( "its star in the venom's light", starIn( venom.star ) );
		rig.frames( 60, false );
		//no proc stamped it (a defence's self-kill): the star alone, as any blow's
		Game.timeTotal += 1f;
		CharSprite e = rig.rat( at( 3, 3 ) );
		e.ch.HP = 0;
		int[] m = rig.made( () -> EnchantFx.blowEnded( a.ch, e.ch ), 20 );
		assertTrue( "the star: " + m[0], m[0] >= 1 && m[0] <= 1 + HitFx.MOST );
		d.ch.HP = 50;
	}

	@Test
	public void aStampedOrTintedBlowCostsNoQuadMore(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		int[] plain = rig.made( () -> d.hitFx( a.x + 8, a.y + 8, 4, HitFx.SLASH, false ), 0 );
		Game.timeTotal += 1f;
		d.blowLook = EnchantLooks.of( Blazing.class );
		d.blowUntil = Game.timeTotal + EnchantFx.STAMP_FOR;
		int[] stamped = rig.made( () -> d.hitFx( a.x + 8, a.y + 8, 4, HitFx.SLASH, false ), 0 );
		Game.timeTotal += 1f;
		EnchantState.of( d ).swingLook = EnchantLooks.of( Blazing.class );
		EnchantState.of( d ).swingUntil = Game.timeTotal + EnchantFx.STAMP_FOR;
		int[] tinted = rig.made( () -> d.hitFx( a.x + 8, a.y + 8, 4, HitFx.SLASH, false ), 0 );
		assertTrue( "a star and its crescent: " + plain[0], plain[0] >= 2 );
		assertEquals( plain[0], stamped[0] );
		assertEquals( plain[0], tinted[0] );
		assertEquals( 0, draws( () -> {
			d.hitFx( a.x + 8, a.y + 8, 4, HitFx.STAB, false );
			rig.frames( 5, false );
		} ) );
	}

	// ------------------------------------------------------------------ stings

	@Test
	public void oneStingABlowTheStrongestAndAnEchoAtThreeTenths(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		EnchantLook blazing = EnchantLooks.of( Blazing.class ), venom = EnchantLooks.of( Venomous.class );
		proc( blazing, a, d, EnchantFx.MID, 0 );
		proc( venom, a, d, EnchantFx.STRONG, 0 );
		rig.frames( 3, true );
		assertEquals( "one sting", 1, ear.heard.size() );
		assertEquals( "the strongest's", venom.sound, ear.heard.get( 0 )[0] );
		float v = (Float) ear.heard.get( 0 )[2];
		assertTrue( v >= Sting.QUIETEST && v <= Sting.LOUDEST );

		//the same enchantment again within its gap: none
		ear.heard.clear();
		proc( venom, a, d, EnchantFx.STRONG, 0 );
		rig.frames( 2, false );
		assertEquals( 0, ear.heard.size() );

		//an echo alone, later: a muffled memory of it
		Game.timeTotal += 1f;
		proc( blazing, a, d, EnchantFx.MID, EnchantFx.ECHO );
		rig.frames( 3, false );
		assertEquals( 1, ear.heard.size() );
		Object[] e = ear.heard.get( 0 );
		assertEquals( blazing.sound, e[0] );
		assertEquals( Sting.volume( blazing, EnchantFx.MID, false ) * Sting.ECHO_VOL, (Float) e[2], 1e-4f );
		float p = (Float) e[3];
		assertTrue( "pitch " + p, p >= blazing.pitch * Sting.ECHO_PITCH * 0.949f && p <= blazing.pitch * Sting.ECHO_PITCH * 1.051f );

		//an echo and the real thing on one blow: the real one's
		ear.heard.clear();
		Game.timeTotal += 1f;
		proc( venom, a, d, EnchantFx.MID, EnchantFx.ECHO );
		proc( blazing, a, d, EnchantFx.WEAK, 0 );
		rig.frames( 3, false );
		assertEquals( 1, ear.heard.size() );
		assertEquals( blazing.sound, ear.heard.get( 0 )[0] );

		//a blow the hero does not see: no sting
		ear.heard.clear();
		Game.timeTotal += 1f;
		Arrays.fill( Dungeon.level.heroFOV, false );
		proc( venom, a, d, EnchantFx.STRONG, 0 );
		rig.frames( 3, false );
		assertEquals( 0, ear.heard.size() );
		Arrays.fill( Dungeon.level.heroFOV, true );

		//a sound the enchantment's own code already plays (Shocking's lightning): none on the host
		Game.timeTotal += 1f;
		proc( face( Family.STORM ), a, d, EnchantFx.STRONG, 0 );
		rig.frames( 3, false );
		assertEquals( 0, ear.heard.size() );
	}

	// ------------------------------------------------------------------ profiles

	@Test
	public void theGlowProfilesAreBoundedAndPure(){
		FxRandom.seedForTests( 3 );
		float fx = FxRandom.Float();
		FxRandom.seedForTests( 3 );
		assertEquals( 0, draws( () -> {
			for (Profile p : Profile.values()) for (int i = 0; i < 600; i++) p.value( i / 60f, 0.37f );
		} ) );
		assertEquals( "no FxRandom either", fx, FxRandom.Float(), 0f );
		for (Profile p : Profile.values()){
			for (float phase : new float[]{ 0f, 1.234f, 17.5f }){
				int blips = 0;
				float last = p.value( 0, phase );
				int[] window = new int[61 * 120];
				for (int i = 0; i < 60 * 120; i++){
					float t = i / 120f, v = p.value( t, phase );
					assertEquals( p + " pure", v, p.value( t, phase ), 0f );
					assertTrue( p + " " + v, v >= 0f && v <= 0.6f );
					if (p.lasting) assertTrue( p + " at " + t + ": " + v, Math.abs( v - p.base ) <= Profile.MOST_SWING + 1e-4f );
					if (v > last + 0.05f){
						blips++;
						window[i] = 1;
					}
					last = v;
				}
				//no more than 3 rises a second, in any second
				for (int i = 0; i + 120 <= 60 * 120; i += 6){
					int n = 0;
					for (int k = i; k < i + 120; k++) n += window[k];
					assertTrue( p + ": " + n + " rises in a second", n <= 3 );
				}
				if (p == Profile.FLICKER) assertTrue( "it blips: " + blips, blips >= 40 );
			}
		}
		assertEquals( Profile.SMOULDER.base, Profile.SMOULDER.value( 0, 0 ), Profile.MOST_SWING );
		assertEquals( 0xFF0000, Profile.cycleColor( new int[]{ 0xFF0000, 0x00FF00 }, 0.1f, 1f ) );
		assertEquals( 0x00FF00, Profile.cycleColor( new int[]{ 0xFF0000, 0x00FF00 }, 1.1f, 1f ) );
	}

	// ------------------------------------------------------------------ states

	@Test
	public void stunnedSeededSurgingAndEveryCharmsHearts(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		Fx.air();
		Fx.glowAir();
		//the guiding light's mark already circles it: a stun's stars must leave its ring be
		HolyFx.mark( s, true );
		FxOrbit.Ring mark = FxOrbit.get( s, FxOrbit.Slot.MARK );
		assertNotNull( mark );
		assertEquals( 0, draws( () -> {
			Buff.affect( s.ch, Paralysis.class, 5f );
			s.add( CharSprite.State.SEEDED );
			s.add( CharSprite.State.SURGING );
			rig.frames( 30, true );
		} ) );
		assertTrue( "still: a stun pauses it", s.paused );
		assertEquals( 3, s.looksCount() );
		assertTrue( "the stun stars", s.stunStars() );
		assertSame( "the mark's ring untouched", mark, FxOrbit.get( s, FxOrbit.Slot.MARK ) );
		assertNotNull( "a surge's motes", FxOrbit.get( s, FxOrbit.Slot.ELEMENTAL ) );
		Buff.detach( s.ch, Paralysis.class );
		s.remove( CharSprite.State.SEEDED );
		s.remove( CharSprite.State.SURGING );
		rig.frames( 30, true );
		assertFalse( s.paused );
		assertEquals( 0, s.looksCount() );
		assertFalse( s.stunStars() );
		assertSame( "the stun's end leaves the mark", mark, FxOrbit.get( s, FxOrbit.Slot.MARK ) );
		HolyFx.mark( s, false );
		rig.frames( 2, true );

		//a pause that is no stun (a time freeze's, a duel's): still, no stars
		CharSprite still = rig.rat( at( 8, 2 ) );
		still.add( CharSprite.State.PARALYSED );
		rig.frames( 5, true );
		assertTrue( still.paused );
		assertFalse( "a time freeze shows no stun stars", still.stunStars() );
		//a stun under ice (frozen solid): the ice, no stars
		CharSprite iced = rig.rat( at( 2, 8 ) );
		Buff.affect( iced.ch, Paralysis.class, 5f );
		rig.frames( 5, true );
		assertTrue( iced.stunStars() );
		iced.add( CharSprite.State.FROZEN );
		rig.frames( 5, true );
		assertFalse( "no stars over the ice", iced.stunStars() );
		iced.remove( CharSprite.State.FROZEN );
		rig.frames( 5, true );
		assertTrue( "the stars again as it thaws", iced.stunStars() );

		//a stun landing on the blow that kills: the death plays, no stars on the corpse
		CharSprite slain = rig.rat( at( 12, 12 ) );
		slain.ch.HP = 0;
		slain.add( CharSprite.State.PARALYSED );
		slain.die();
		rig.frames( 2, true );
		assertFalse( "the death is not paused", slain.paused );
		assertFalse( slain.stunStars() );
		assertEquals( 0, slain.looksCount() );

		//a sprite of no character (the surface's ally, held still): no stars
		CharSprite bare = new RatSprite();
		rig.stage.add( bare );
		bare.add( CharSprite.State.PARALYSED );
		rig.frames( 5, true );
		assertTrue( bare.paused );
		assertFalse( bare.stunStars() );

		//every charm wears its hearts, whatever charmed it
		CharSprite c = rig.rat( at( 3, 3 ) );
		Buff.affect( c.ch, Charm.class, 5f );
		rig.frames( 3, true );
		assertEquals( 1, c.looksCount() );
		Buff.detach( c.ch, Charm.class );
		rig.frames( 30, true );
		assertEquals( 0, c.looksCount() );
		//a charm wearing off a summoned ally (whose bond wears hearts too): they stay
		Buff.affect( c.ch, SummonElemental.InvisAlly.class );
		Buff.affect( c.ch, Charm.class, 5f );
		rig.frames( 3, true );
		Buff.detach( c.ch, Charm.class );
		rig.frames( 30, true );
		assertEquals( "the ally's hearts", 1, c.looksCount() );
	}

	@Test
	public void anAfterglowWaitsForTheFlashAndGoes(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		s.flash();
		s.afterglow( 0xFF8418, 0.2f );
		rig.frames( 1, false );
		assertEquals( "held while it flashes", 0f, s.afterglowNow(), 0.11f );
		rig.frames( 3, false );
		assertTrue( s.afterglowNow() > 0f );
		rig.frames( 10, false );
		assertEquals( 0f, s.afterglowNow(), 0f );
		assertEquals( 1f, s.rm, 0f );
	}

	@Test
	public void markedGainedItemsAreFoundForASecond(){
		Weapon w = new Sword(), v = new Sword();
		EnchantFx.markGained( w );
		assertEquals( 0f, EnchantFx.gainedAgo( w ), 0f );
		assertEquals( Float.MAX_VALUE, EnchantFx.gainedAgo( v ), 0f );
		Game.timeTotal += 0.5f;
		assertEquals( 0.5f, EnchantFx.gainedAgo( w ), 1e-4f );
	}
}
