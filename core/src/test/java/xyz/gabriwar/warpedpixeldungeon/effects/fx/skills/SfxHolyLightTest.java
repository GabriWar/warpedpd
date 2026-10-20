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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.DivineWrathGround;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SacredWeaponSwords;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ShieldOfTheFaithfulWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CrusadersZeal;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Purify;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.BlobFx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.AegisArcFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.HolyFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The cleric's holy light (docs/fx/skills.md, holy-light): every swap keeps the game's Random as the
 * call it replaced drew it (a burst, a start or a pour its factory swapped; a wand's bolt a holy
 * SkillBolt; the white splash its one bare draw; the weapon's fall a blade of light), the bolts and
 * the falls land on the frames the old ones did, the held pictures (the hallowed ground's halos, the
 * aegis arc, the planted blades) are let go of with Fx.dispose and leave nothing behind, a lance out of
 * a body never reaches the floor, the aegis faces the blow and dims as it cracks, Crusader's Zeal
 * wears its own icon, a busy holy moment stays under the budget, nothing touches the GPU while made,
 * and none of the package's skills borrows a game look any more (fire on the hero, a wand's bolt, a
 * beam between characters, a shield item or bubble, the weapon item, notes, an upgrade's arrows).
 */
public class SfxHolyLightTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group mobs;

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
		Dungeon.level = room();
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
		return link( new RatSprite(), new Rat(), cell );
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
	private int arrival( Consumer<Runnable> start, int max ){
		final int[] at = { -1 }, frame = { 0 };
		start.accept( () -> { if (at[0] < 0) at[0] = frame[0]; } );
		for (frame[0] = 1; frame[0] <= max && at[0] < 0; frame[0]++) frames( 1 );
		return at[0];
	}

	private Emitter emitter(){
		Emitter e = new Emitter();
		e.pos( 80, 70, 16, 16 );
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		return e;
	}

	// ------------------------------------------------------------------ the game's Random

	@Test
	public void everySwappedBurstDrawsAsTheOneItReplaced(){
		//{ what it was, what it is, how many }: each skill's very call, its factory swapped
		Object[][] bursts = {
				{ FlameParticle.FACTORY, SkillBursts.HOLY_MOTE, 8 },             //Divine Wrath's kindling, Fervor's (10)
				{ FlameParticle.FACTORY, SkillBursts.HOLY_MOTE, 2 },             //Divine Wrath's hallowed cells
				{ FlameParticle.FACTORY, HolyFlameParticle.FACTORY, 12 },        //Fervor's eruption (and its +3's 6)
				{ ShaftParticle.FACTORY, SkillBursts.HOLY_MOTE, 8 },             //Avatar, Last Rites, Purify, Mass Heal, Healing Prayer
				{ Speck.factory( Speck.STAR ), SkillBursts.HOLY_MOTE, 8 },       //Holy Charge's struck
				{ Speck.factory( Speck.NOTE ), SkillBursts.VERSE, 2 },           //Litany
				{ Speck.factory( Speck.UP ), SkillBursts.BREATH, 5 },            //Second Wind
				{ Speck.factory( Speck.STEAM ), SkillBursts.BREATH, 5 } };       //Inner Peace
		//(Regeneration's growth bursts Speck.HEALING again, the owner's restored look: no swap left)
		for (Object[] b : bursts){
			int old = EnchantFxTest.draws( () -> { emitter().burst( (Emitter.Factory) b[0], (Integer) b[2] ); frames( 40 ); } );
			int now = EnchantFxTest.draws( () -> { emitter().burst( (Emitter.Factory) b[1], (Integer) b[2] ); frames( 40 ); } );
			assertEquals( b[1] + " for " + b[0], old, now );
			frames( 60 );
		}
		//the streams: Avatar's (LIGHT) and Reckoning's (STAR) starts, the hallowed ground's pour, the
		//stances' accents (a pour each)
		Object[][] streams = {
				{ Speck.factory( Speck.LIGHT ), SkillBursts.HOLY_MOTE, 12 },
				{ Speck.factory( Speck.STAR ), SkillBursts.HOLY_MOTE, 10 } };
		for (Object[] s : streams){
			int old = EnchantFxTest.draws( () -> { emitter().start( (Emitter.Factory) s[0], 0.3f, (Integer) s[2] ); frames( 240 ); } );
			int now = EnchantFxTest.draws( () -> { emitter().start( (Emitter.Factory) s[1], 0.3f, (Integer) s[2] ); frames( 240 ); } );
			assertEquals( s[1] + " for " + s[0], old, now );
		}
		for (Object[] p : new Object[][]{ { BlobFx.WRATH, SkillBursts.HOLY_MOTE }, { FlameParticle.FACTORY, SkillBursts.HOLY_MOTE },
				{ ShaftParticle.FACTORY, SkillBursts.HOLY_MOTE } }){
			int old = EnchantFxTest.draws( () -> { Emitter e = emitter(); e.pour( (Emitter.Factory) p[0], 0.3f ); frames( 90 ); e.on = false; } );
			int now = EnchantFxTest.draws( () -> { Emitter e = emitter(); e.pour( (Emitter.Factory) p[1], 0.3f ); frames( 90 ); e.on = false; } );
			assertEquals( p[1] + " for " + p[0], old, now );
		}
	}

	@Test
	public void theWhiteSplashIsLeftAsItsOneDraw(){
		//Purify: Splash.around drew once when the scene had its effects, never without
		CharSprite hs = Dungeon.hero.sprite;
		assertEquals( 1, EnchantFxTest.draws( () -> Splash.around( hs, 0xFFFFFF, 6 ) ) );
		assertEquals( 1, EnchantFxTest.draws( Purify::splashDraw ) );
		GameScene.useLayersForTests( null );
		assertEquals( 0, EnchantFxTest.draws( () -> Splash.around( hs, 0xFFFFFF, 6 ) ) );
		assertEquals( 0, EnchantFxTest.draws( Purify::splashDraw ) );
		GameScene.useLayersForTests( layers );
	}

	@Test
	public void theHolyBoltsAreTheMissilesTheyReplace(){
		//Grace, Scouring Flame and the ward's reflection: MagicMissile.boltFromChar(LIGHT_MISSILE) to a
		//cell nobody stands on is SkillBolt.fly( group, sprite, cell ): one draw, the same frame
		CharSprite hs = Dungeon.hero.sprite;
		int to = at( 9, 6 );
		assertEquals( EnchantFxTest.draws( () -> MagicMissile.boltFromChar( mobs, MagicMissile.LIGHT_MISSILE, hs, to, null ) ),
				EnchantFxTest.draws( () -> SkillBolt.fly( mobs, hs, to, SkillBolt.HOLY, null ) ) );
		frames( 120 );
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			int old = arrival( cb -> MagicMissile.boltFromChar( mobs, MagicMissile.LIGHT_MISSILE, hs, to, cb::run ), 400 );
			int now = arrival( cb -> SkillBolt.fly( mobs, hs, to, SkillBolt.HOLY, cb::run ), 400 );
			assertTrue( old > 0 );
			assertEquals( "dt " + dt, old, now );
			//to a body: its middle (destinationCenter), as boltFromChar aimed
			CharSprite r = rat( at( 8, 2 ) );
			old = arrival( cb -> ((MagicMissile) mobs.recycle( MagicMissile.class ))
					.reset( MagicMissile.LIGHT_MISSILE, hs.center(), r.destinationCenter(), cb::run ), 400 );
			now = arrival( cb -> SkillBolt.fly( mobs, hs.center(), r.destinationCenter(), SkillBolt.HOLY, cb::run ), 400 );
			assertEquals( "dt " + dt + " to a body", old, now );
			mobs.remove( r );
			frames( 30 );
		}
		//the mends and the beams' places: no draw at all
		CharSprite r = rat( at( 6, 2 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			SkillBolt.mote( hs, r, SkillBolt.HEAL, null );
			SkillBolt.mote( r, hs, SkillBolt.HOLY, () -> FxRecipes.castBloom( 50, 70, Element.HOLY ) );
			frames( 90 );
		} ) );
	}

	@Test
	public void aBladeOfLightFallsAndFliesAsTheSwordDid(){
		//a MissileSprite's first flight loads what draws once for good: taken out of the count
		((MissileSprite) new Group().recycle( MissileSprite.class )).reset( new PointF( 10, 10 ), new PointF( 60, 90 ), new Sword(), null );
		final Sword sword = new Sword();
		int old = EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), sword, 1, null ) );
		int now = EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), sword, SkillFX.Fall.HOLY_BLADE, 1, null ) );
		assertEquals( 4, old );
		assertEquals( old, now );
		frames( 120 );
		//no weapon: nothing falls, nothing is drawn, the landing never comes (as ever)
		final boolean[] called = { false };
		assertEquals( 0, EnchantFxTest.draws( () -> SkillFX.rain( at( 7, 6 ), null, SkillFX.Fall.HOLY_BLADE, 1, () -> called[0] = true ) ) );
		frames( 60 );
		assertFalse( called[0] );
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			Random.pushGenerator( 5 );
			int fell = arrival( cb -> SkillFX.rain( at( 7, 6 ), new Sword(), 1, cb::run ), 400 );
			Random.popGenerator();
			Random.pushGenerator( 5 );
			int falls = arrival( cb -> SkillFX.rain( at( 7, 6 ), new Sword(), SkillFX.Fall.HOLY_BLADE, 1, cb::run ), 400 );
			Random.popGenerator();
			assertEquals( "dt " + dt, fell, falls );
			//the mastery's throw: as fast as the sword flew
			int threw = arrival( cb -> SkillFX.streak( at( 6, 4 ), at( 9, 2 ), new Sword(), cb::run ), 400 );
			int throwsNow = arrival( cb -> SkillFX.streak( at( 6, 4 ), at( 9, 2 ), SkillFX.Streak.HOLY_BLADE, new Sword(), cb::run ), 400 );
			assertEquals( "dt " + dt, threw, throwsNow );
		}
		//and neither is the sword item
		SkillFX.rain( at( 7, 6 ), new Sword(), SkillFX.Fall.HOLY_BLADE, 1, null );
		SkillFX.streak( at( 6, 4 ), at( 9, 2 ), SkillFX.Streak.HOLY_BLADE, new Sword(), null );
		frames( 3 );
		assertEquals( 0, count( mobs, MissileSprite.class ) + count( mobs, ItemSprite.class ) );
	}

	@Test
	public void noneOfTheNewPicturesDrawsTheGamesRandom(){
		CharSprite a = rat( at( 5, 4 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			makeEverything( a );
			frames( 90 );
		} ) );
	}

	// ------------------------------------------------------------------ the held pictures

	@Test
	public void theHallowedGroundIsItsOwnLightAndComesAndGoes(){
		int[] cells = { at( 5, 3 ), at( 5, 4 ), at( 6, 4 ) };
		Group floor = layers[GameScene.Layer.FLOOR.ordinal()];
		DivineWrathGround.Hallow g = DivineWrathGround.Hallow.show( cells, new int[]{ at( 6, 4 ) } );
		assertNotNull( g );
		assertEquals( 1, count( floor, DivineWrathGround.Hallow.class ) );
		//members: a pool, then a halo, a cell. The new cell's halo opens from the smallest ring,
		//stepping (never scaled)
		frames( 1 );
		assertEquals( FxFrames.RING_5[2], ((Image) g.membersView().get( 5 )).width, 0 );
		frames( 30 );
		//a halo lying on the floor over a pool of light a cell: never Sanctuary's glyph circle (a
		//16 px glyph of skill_fields.png, turning), never standing up out of the floor
		Object sanctuarys = com.watabou.gltextures.TextureCache.get( "effects/skill_fields.png" );
		for (Gizmo m : g.membersView()){
			Image p = (Image) m;
			assertTrue( p.visible );
			assertNotEquals( sanctuarys, p.texture );
			assertEquals( 0, p.angle, 0 );
			assertTrue( "lying flat: " + p.width + "x" + p.height, p.height * 2 <= p.width + 1 );
			assertTrue( "about its cell (a crest swells it a step): " + p.width, p.width <= FxFrames.RING_17[2] );
		}
		//one ground: a crest of light rolls out across its halos, so they are not all alike every frame
		boolean rolled = false;
		for (int f = 0; f < 120 && !rolled; f++){
			frames( 1 );
			float a0 = ((Image) g.membersView().get( 1 )).am;
			for (int i = 3; i < 6; i += 2) if (Math.abs( ((Image) g.membersView().get( i )).am - a0 ) > 0.15f) rolled = true;
		}
		assertTrue( rolled );
		//a burning cell flares to full
		g.flare( at( 5, 4 ) );
		frames( 1 );
		assertEquals( 1f, ((Image) g.membersView().get( 3 )).am, 0 );
		//the ground changes: the cell that went fades, then the old light is let go of
		DivineWrathGround.Hallow next = DivineWrathGround.Hallow.show( new int[]{ at( 5, 3 ), at( 5, 4 ) }, new int[0] );
		g.fadeExcept( new int[]{ at( 5, 3 ), at( 5, 4 ) } );
		frames( 2 );
		assertFalse( ((Image) g.membersView().get( 1 )).visible );
		assertTrue( ((Image) g.membersView().get( 5 )).visible );
		frames( Math.round( DivineWrathGround.Hallow.FADE * 60 ) + 2 );
		assertEquals( 1, count( floor, DivineWrathGround.Hallow.class ) );
		next.fadeExcept( new int[0] );
		frames( 30 );
		assertEquals( 0, count( floor, DivineWrathGround.Hallow.class ) );
		//nothing to show: nothing made
		assertEquals( null, DivineWrathGround.Hallow.show( new int[0], new int[0] ) );
	}

	@Test
	public void theAegisFacesTheBlowAndCracks(){
		CharSprite hs = Dungeon.hero.sprite;
		hs.flipHorizontal = false;
		assertEquals( 0, AegisArcFX.facing( hs, at( 6, 4 ) ) );
		assertEquals( 4, AegisArcFX.facing( hs, at( 1, 4 ) ) );
		assertEquals( 2, AegisArcFX.facing( hs, at( 3, 7 ) ) );
		assertEquals( 6, AegisArcFX.facing( hs, at( 3, 1 ) ) );
		assertEquals( 7, AegisArcFX.facing( hs, at( 5, 2 ) ) );
		//nothing to face: his front
		assertEquals( 0, AegisArcFX.facing( hs, -1 ) );
		hs.flipHorizontal = true;
		assertEquals( 4, AegisArcFX.facing( hs, -1 ) );
		hs.flipHorizontal = false;
		//it rests a step dimmer with each crack, never under 0.4
		assertEquals( 0.7f, ShieldOfTheFaithfulWard.rest( 0 ), 1e-6f );
		assertEquals( 0.55f, ShieldOfTheFaithfulWard.rest( 1 ), 1e-6f );
		assertEquals( 0.4f, ShieldOfTheFaithfulWard.rest( 2 ), 1e-6f );
		assertEquals( 0.4f, ShieldOfTheFaithfulWard.rest( 5 ), 1e-6f );

		//a held arc sweeps round to what it guards, one direction at a time
		final int[] facing = { at( 6, 4 ) };
		final float[] rest = { 0.7f };
		AegisArcFX arc = AegisArcFX.hold( hs, new AegisArcFX.Ward(){
			@Override public int facing(){ return facing[0]; }
			@Override public float rest(){ return rest[0]; }
		}, AegisArcFX.GOLD );
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		assertEquals( 1, count( lights, AegisArcFX.class ) );
		frames( 60 );
		assertTrue( arc.visible );
		assertTrue( "breathing under its rest: " + arc.am, arc.am <= 0.7f + 1e-4f && arc.am >= 0.7f * (1 - AegisArcFX.BREATH_DIP) - 0.02f );
		assertTrue( shows( arc, 0 ) );
		facing[0] = at( 1, 4 );
		frames( 1 );
		assertFalse( "a step, not a jump", shows( arc, 4 ) );
		frames( Math.round( 4 * AegisArcFX.SWEEP * 60 ) + 4 );
		assertTrue( shows( arc, 4 ) );
		//a catch flares it to full
		arc.caught();
		frames( 1 );
		assertEquals( 1f, arc.am, 0 );
		//broken: it goes from sight, and comes back when it reforms
		rest[0] = 0;
		frames( 60 );
		assertFalse( arc.visible );
		//let go of: off the scene
		Fx.dispose( arc );
		frames( 1 );
		assertEquals( 0, count( lights, AegisArcFX.class ) );

		//Aegis's flash: by itself, for its life, then gone
		AegisArcFX.flash( hs, at( 4, 4 ), AegisArcFX.GOLD, 0.3f );
		assertEquals( 1, count( lights, AegisArcFX.class ) );
		frames( 2 );
		assertEquals( 1, count( lights, AegisArcFX.class ) );
		frames( 30 );
		assertEquals( 0, count( lights, AegisArcFX.class ) );
	}

	@Test
	public void thePlantedBladesStandInTheirCellsAndGo(){
		int cell = at( 6, 4 );
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		int lightsBefore = FxLight.live();
		SacredWeaponSwords.Blades b = SacredWeaponSwords.Blades.show( new int[]{ cell }, new int[]{ cell } );
		assertEquals( 1, count( lights, SacredWeaponSwords.Blades.class ) );
		//still falling: nothing stands there yet
		frames( 5 );
		for (Gizmo m : b.membersView()) assertFalse( m.visible );
		b.landed( cell );
		frames( 20 );
		//its tip in its cell (never below it), its grip no taller than a body standing there
		float cellBottom = (cell / W + 1) * 16f, cellTop = (cell / W) * 16f - 8;
		for (Gizmo m : b.membersView()){
			Image p = (Image) m;
			assertTrue( p.visible );
			float[] box = box( p );
			assertTrue( "within its cell: " + Arrays.toString( box ), box[1] >= cellTop && box[3] <= cellBottom );
			assertTrue( "on its column: " + Arrays.toString( box ), box[0] >= (cell % W) * 16 && box[2] <= (cell % W + 1) * 16 );
		}
		assertTrue( FxLight.live() > lightsBefore );
		assertEquals( 0, count( mobs, ItemSprite.class ) + count( lights, ItemSprite.class ) );
		//gone: faded, let go of, its light put out
		b.fadeExcept( new int[0] );
		frames( 60 );
		assertEquals( 0, count( lights, SacredWeaponSwords.Blades.class ) );
		assertEquals( lightsBefore, FxLight.live() );
		//one still falling when the swords go is let go of at once
		SacredWeaponSwords.Blades c = SacredWeaponSwords.Blades.show( new int[]{ cell }, new int[]{ cell } );
		c.fadeExcept( new int[0] );
		frames( 2 );
		assertEquals( 0, count( lights, SacredWeaponSwords.Blades.class ) );
	}

	@Test
	public void theHeldPicturesStayWithTheirOwnersWhileTimeIsStill(){
		float savedTotal = Game.timeTotal;
		Game.timeTotal = 10;
		try {
			CharSprite hs = Dungeon.hero.sprite;
			AegisArcFX arc = AegisArcFX.hold( hs, new AegisArcFX.Ward(){
				@Override public int facing(){ return -1; }
				@Override public float rest(){ return 0.7f; }
			}, AegisArcFX.GOLD );
			int cell = at( 6, 4 );
			SacredWeaponSwords.Blades b = SacredWeaponSwords.Blades.show( new int[]{ cell }, new int[0] );
			frames( 30 );
			assertTrue( arc.visible );
			//the hourglass: the cleric walks a cell, his ward at his side
			Emitter.freezeEmitters = true;
			float x = arc.x;
			hs.x += 16;
			frames( 1 );
			assertEquals( x + 16, arc.x, 0.5f );
			assertTrue( arc.visible );
			//a cast then remakes the blades: the one kept still stands
			SacredWeaponSwords.Blades next = SacredWeaponSwords.Blades.show( new int[]{ cell }, new int[0] );
			b.fadeExcept( new int[]{ cell } );
			frames( 1 );
			for (Gizmo m : next.membersView()) assertTrue( m.visible );
			Emitter.freezeEmitters = false;
			next.fadeExcept( new int[0] );
			Fx.dispose( arc );
			frames( 30 );
		} finally {
			Emitter.freezeEmitters = false;
			Game.timeTotal = savedTotal;
		}
	}

	@Test
	public void theLanceOutOfABodyNeverReachesTheFloor(){
		CharSprite r = rat( at( 6, 4 ) );
		float feet = r.y + r.height();
		LanceFX.stab( SkillVfx.midX( r ), r.y - Transcendence.LANCE_OVER, -90, Transcendence.HOLY, Transcendence.LANCE, Transcendence.LANCE_LIFE );
		int shown = 0;
		for (int f = 0; f < 30; f++){
			frames( 1 );
			for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
				if (!(g instanceof LanceFX) || !g.exists) continue;
				for (Gizmo m : ((LanceFX) g).membersView()){
					if (!(m instanceof Image) || !m.visible || ((Image) m).am <= 0) continue;
					shown++;
					float[] box = box( (Image) m );
					assertTrue( "into the floor at frame " + f + ": " + Arrays.toString( box ), box[3] <= feet - 2 );
				}
			}
		}
		assertTrue( shown > 0 );
		assertEquals( 0, LanceFX.live() );
	}

	// ------------------------------------------------------------------ caps and the GPU

	@Test
	public void aBusyHolyMomentStaysUnderTheBudget(){
		//Reckoning on eight, a hallowed ground of nine cells under them, a Scouring Flame of nine
		//bolts, a pass through two bodies with its three strokes, the ward and three blades held
		CharSprite[] r = new CharSprite[8];
		for (int i = 0; i < r.length; i++) r[i] = rat( at( 5 + i % 4, 2 + i / 4 * 3 ) );
		CharSprite hs = Dungeon.hero.sprite;
		int most = 0, flares = 0, lights = 0, ghosts = 0;
		hs.emitter().start( SkillBursts.HOLY_MOTE, 0.3f, 10 );
		PixelFlare.show( SkillVfx.midX( hs ), SkillVfx.midY( hs ), 0xF1D99C, PixelFlare.L, 0.8f );
		for (CharSprite c : r){
			PillarRiseFX.show( c.ch.pos, 0xF1D99C, null );
			c.emitter().burst( HolyFlameParticle.FACTORY, 7 );
		}
		int[] ground = new int[9];
		for (int i = 0; i < 9; i++) ground[i] = at( 6 + i % 3, 3 + i / 3 );
		DivineWrathGround.Hallow.show( ground, ground );
		for (int c : ground) emitter().pour( SkillBursts.HOLY_MOTE, 0.3f );
		for (int i = 0; i < 9; i++) SkillBolt.fly( mobs, hs, at( 9, i + 1 ), SkillBolt.HOLY, null );
		StreakFX.dash( Dungeon.hero.pos, at( 6, 4 ), 0xF1D99C, 0.45f, 0.25f, true );
		for (int i = 0; i < 3; i++) StreakFX.show( at( 6, 4 ), at( 8, 2 + i * 2 ), 0xF1D99C, 0.35f, 0.2f );
		LanceFX.stab( SkillVfx.midX( r[0] ), r[0].y - Transcendence.LANCE_OVER, -90, Transcendence.HOLY, Transcendence.LANCE, Transcendence.LANCE_LIFE );
		LanceFX.stab( SkillVfx.midX( r[1] ), r[1].y - Transcendence.LANCE_OVER, -90, Transcendence.HOLY, Transcendence.LANCE, Transcendence.LANCE_LIFE );
		AegisArcFX.hold( hs, new AegisArcFX.Ward(){
			@Override public int facing(){ return at( 5, 2 ); }
			@Override public float rest(){ return 0.7f; }
		}, AegisArcFX.GOLD ).caught();
		SacredWeaponSwords.Blades.show( new int[]{ at( 2, 3 ), at( 2, 5 ), at( 4, 6 ) }, new int[0] );
		for (int f = 0; f < 90; f++){
			frames( 1 );
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
			flares = Math.max( flares, PixelFlare.live() );
			lights = Math.max( lights, FxLight.live() );
			ghosts = Math.max( ghosts, Afterimage.live() );
		}
		assertTrue( "particles " + most, most <= FxBudget.soft() );
		assertTrue( "flares " + flares, flares <= PixelFlare.CAP );
		assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
		assertTrue( "afterimages " + ghosts, ghosts <= FxBudget.afterimageCap() );
		assertTrue( "persistent lights " + FxLight.livePersistent(), FxLight.livePersistent() <= FxBudget.persistentLightCap() );
	}

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		CharSprite a = rat( at( 5, 4 ) );
		failOnGl();
		makeEverything( a );
		frames( 40 );
	}

	//every new picture of the package once (none of the game's bursts, none that draw)
	private void makeEverything( CharSprite a ){
		CharSprite hs = Dungeon.hero.sprite;
		int[] ground = { at( 5, 3 ), at( 5, 4 ), at( 6, 4 ) };
		DivineWrathGround.Hallow g = DivineWrathGround.Hallow.show( ground, ground );
		g.flare( at( 5, 4 ) );
		AegisArcFX.flash( hs, a.ch.pos, AegisArcFX.GOLD, 0.3f );
		AegisArcFX arc = AegisArcFX.hold( hs, new AegisArcFX.Ward(){
			@Override public int facing(){ return at( 5, 4 ); }
			@Override public float rest(){ return 0.55f; }
		}, AegisArcFX.GOLD );
		arc.caught();
		SacredWeaponSwords.Blades b = SacredWeaponSwords.Blades.show( new int[]{ at( 6, 5 ) }, new int[]{ at( 6, 5 ) } );
		b.landed( at( 6, 5 ) );
		SkillSpectacleFX.fly( SkillSpectacleFX.SABER, at( 6, 5 ), a.ch.pos, 0, .25f, SacredWeaponSwords.Blades.GOLD );
		SkillSpectacleFX.fly( SkillSpectacleFX.WINGS, Dungeon.hero.pos, Dungeon.hero.pos, 0, .65f, 0xB3D699 );
		StreakFX.show( Dungeon.hero.pos, a.ch.pos, 0xF1D99C, 0.3f, 0.15f );
		StreakFX.dash( Dungeon.hero.pos, at( 6, 4 ), 0xF1D99C, 0.45f, 0.3f, true );
		LanceFX.stab( SkillVfx.midX( a ), a.y - Transcendence.LANCE_OVER, -90, Transcendence.HOLY, Transcendence.LANCE, Transcendence.LANCE_LIFE );
		PillarRiseFX.show( Dungeon.hero.pos, 0xF1D99C, null );
		PulseRingFX.around( hs, 0xF1D99C, 24, 0.5f );
		PixelFlare.show( SkillVfx.midX( a ), SkillVfx.midY( a ), 0xF1D99C, PixelFlare.M, 0.5f );
		FxRecipes.embers( SkillVfx.midX( a ), SkillVfx.midY( a ), Element.HOLY, 4 );
		SkillBolt.mote( hs, a, SkillBolt.HEAL, null );
		g.fadeExcept( new int[0] );
		b.fadeExcept( new int[0] );
	}

	// ------------------------------------------------------------------ nothing borrowed

	@Test
	public void crusadersZealWearsItsOwnIcon() throws Exception {
		assertEquals( BuffIndicator.ZEAL, new CrusadersZeal.Zeal().icon() );
		assertNotEquals( BuffIndicator.FIRE, BuffIndicator.ZEAL );
		BufferedImage small = ImageIO.read( new File( "src/main/assets/interfaces/buffs.png" ) );
		BufferedImage large = ImageIO.read( new File( "src/main/assets/interfaces/large_buffs.png" ) );
		assertTrue( "small", opaque( small, BuffIndicator.ZEAL, BuffIndicator.SIZE_SMALL ) > 10 );
		assertTrue( "large", opaque( large, BuffIndicator.ZEAL, BuffIndicator.SIZE_LARGE ) > 30 );
	}

	private static int opaque( BufferedImage im, int cell, int size ){
		int cols = im.getWidth() / size, x0 = (cell % cols) * size, y0 = (cell / cols) * size, n = 0;
		for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) if ((im.getRGB( x0 + x, y0 + y ) >>> 24) != 0) n++;
		return n;
	}

	@Test
	public void theClericsSkillsNoLongerBorrowTheGamesLooks() throws Exception {
		String sk = "src/main/java/xyz/gabriwar/warpedpixeldungeon/actors/hero/skills/";
		String bf = "src/main/java/xyz/gabriwar/warpedpixeldungeon/actors/buffs/";
		//fire on the hero or the floor, a wand's bolt, a beam between characters, a shield item or the
		//SHIELDED bubble, the weapon item, a lullaby's notes, an upgrade's arrows, steam, a level-up's
		//stars, the legacy fans of rays, a splash, a second landing, a dropped cross, a curse's and a
		//fire's sounds; and Zeal's flame icon
		String[] everywhere = { "Beam.", "MagicMissile", "new Flare(", "effects.Flare;", "ShaftParticle", "Speck.NOTE", "Speck.UP",
				"Speck.STEAM", "Speck.SCREAM", "ItemSprite", "Splash.", "Sounds.CURSED" };
		String[][] files = {
				{ sk + "DivineWrath.java", "FlameParticle", "Sounds.BURNING" },
				{ bf + "DivineWrathGround.java", "BlobFx.WRATH", "Sounds.BURNING" },
				{ sk + "CrusadersZeal.java", "BuffIndicator.FIRE" },
				{ sk + "AvatarOfLight.java", "ShieldHalo" },
				{ bf + "AvatarOfLightHalo.java" },
				{ sk + "Aegis.java", "ShieldHalo" },
				{ bf + "AegisRecharge.java" },
				{ sk + "HolySmite.java", "SkillFX.land(" },
				{ sk + "HolyCharge.java", "Speck.STAR" },
				{ sk + "Litany.java" },
				{ sk + "LastRites.java" },
				{ sk + "Grace.java" },
				{ sk + "PillarOfLight.java", "SkillSpectacleFX.SPIRE" },
				//(Righteous Strikes and Retribution: their LightRays and Righteous Strikes' Flare back on
				//the owner's word, the second pass; RfxKitProcs and SkillKitTest hold them)
				{ sk + "Purify.java" },
				{ sk + "MassHeal.java" },
				{ sk + "ScouringFlame.java" },
				{ sk + "Sanctuary.java", "ShieldHalo" },
				{ sk + "Reckoning.java", "Speck.STAR" },
				{ sk + "ZealPassive.java", "FlameParticle", "Sounds.BURNING", "Sounds.BLAST" },
				{ bf + "ShieldOfTheFaithfulWard.java", "ShieldHalo", "ROUND_SHIELD" },
				{ sk + "Transcendence.java" },
				{ bf + "SacredWeaponSwords.java", "ItemSpriteSheet", "SkillFX.rain( cell, look, 1" },
				{ sk + "HealingPrayer.java" },
				{ sk + "Condemn.java" },
				{ sk + "SecondWind.java" },
				{ sk + "InnerPeace.java" } };
		for (String[] f : files){
			String src = code( new String( Files.readAllBytes( new File( f[0] ).toPath() ), "UTF-8" ) );
			String name = f[0].substring( f[0].lastIndexOf( '/' ) + 1 );
			for (String b : everywhere) assertFalse( name + " uses " + b, uses( src, b ) );
			for (int i = 1; i < f.length; i++) assertFalse( name + " uses " + f[i], uses( src, f[i] ) );
		}
		//Regeneration's growth shows the heal's crosses again (72b40617b's look, restored on the owner's
		//word: an exception to "crosses only for a real heal"), never the breath
		String regen = new String( Files.readAllBytes( new File( sk + "Regeneration.java" ).toPath() ), "UTF-8" );
		String growth = regen.substring( regen.indexOf( "protected boolean upgrade()" ), regen.indexOf( "return true;" ) );
		assertTrue( growth.contains( "Speck.factory( Speck.HEALING ), 3" ) );
		assertFalse( growth.contains( "SkillBursts.BREATH" ) );
		//the one shield's halo kept: Healing Prayer's +3 grants a real Barrier
		String prayer = new String( Files.readAllBytes( new File( sk + "HealingPrayer.java" ).toPath() ), "UTF-8" );
		assertTrue( prayer.contains( "Barrier.class ).incShield" ) && prayer.contains( "ShieldHalo" ) );
	}

	// ------------------------------------------------------------------ helpers

	//whether code names a thing (a whole name: HolyFlameParticle is not FlameParticle)
	private static boolean uses( String code, String name ){
		return java.util.regex.Pattern.compile( "(?<![A-Za-z])" + java.util.regex.Pattern.quote( name ) ).matcher( code ).find();
	}

	//a source without its comments (they may name what the code no longer does)
	private static String code( String src ){
		return src.replaceAll( "(?s)/\\*.*?\\*/", "" ).replaceAll( "//[^\n]*", "" );
	}

	//whether a held arc shows the frame of direction k
	private static boolean shows( Image i, int k ){
		int[] f = FxFrames.Light.RIM_ARC_23[k];
		RectF uv = i.frame();
		float tw = i.texture.width, th = i.texture.height;
		return Math.abs( uv.left * tw - f[0] ) < 0.5f && Math.abs( uv.top * th - f[1] ) < 0.5f;
	}

	//a quad's box in the world: { left, top, right, bottom }, its turn and scale about its origin taken in
	private static float[] box( Image i ){
		float w = i.width, h = i.height;
		double a = Math.toRadians( i.angle ), c = Math.cos( a ), s = Math.sin( a );
		float[] out = { Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
		for (float[] p : new float[][]{ { 0, 0 }, { w, 0 }, { 0, h }, { w, h } }){
			float dx = (p[0] - i.origin.x) * i.scale.x, dy = (p[1] - i.origin.y) * i.scale.y;
			float x = (float) (i.x + i.origin.x + dx * c - dy * s), y = (float) (i.y + i.origin.y + dx * s + dy * c);
			out[0] = Math.min( out[0], x );
			out[1] = Math.min( out[1], y );
			out[2] = Math.max( out[2], x );
			out[3] = Math.max( out[3], y );
		}
		return out;
	}

	private static int count( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.exists && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += count( (Group) m, c );
		}
		return n;
	}
}
