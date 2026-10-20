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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BloodDance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hamstring;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LockSmith;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.StoneSkin;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SummonCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Warbreaker;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.BannerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
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
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The warrior's, duelist's, steel and earth skills' own looks (package martial-steel): the new
 * pictures draw nothing of the game's Random; a splash taken out still draws its one; Deadfall's
 * weight and shock draw as the item's rain did; Blade Bind's bite comes on the frame the chain's
 * did; the banner stands as two quads and a light and is let go of with them; the stagger draws no
 * stun stars and rocks within its pixels; a tether keeps to its links and goes; the bound mark
 * circles an ally only and goes with its sprite; nothing touches the GPU while made; and the
 * package's skills no longer borrow the alarm, the rockfall, the stun's stars, forge sparks, the
 * Ethereal Chains, a beam, a spear item or the old flares.
 */
public class SfxMartialSteelTest {

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
		Dungeon.hero = hero;
		link( plain(), hero, at( 3, 4 ) );
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
				texture( Assets.Effects.FX_SHEET );
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

	// ------------------------------------------------------------------ the game's Random

	@Test
	public void theNewPicturesDrawNothingOfTheGamesRandom(){
		CharSprite a = rat( at( 5, 4 ) );
		SummonedPet pet = pet( at( 2, 2 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			makeEverything( a );
			pet.updateSpriteState();
			frames( 90 );
		} ) );
	}

	@Test
	public void aSplashTakenOutStillDrawsItsOne() throws Exception {
		//Blood Dance's ring: each splash it no longer makes stands as the one draw it always made
		assertEquals( 1, EnchantFxTest.draws( () -> Splash.at( at( 4, 4 ), 0xCC1111, 3 ) ) );
		assertEquals( 1, EnchantFxTest.draws( () -> {
			if (GameScene.layer( GameScene.Layer.EFFECTS ) != null) com.watabou.utils.Random.Float();
			for (int k = 0; k < 8; k++) BloodDance.step( Dungeon.hero.sprite, k );
		} ) );
		//and the waltz still makes it, once a step, in its timeline
		String src = source( "actors/hero/skills/BloodDance.java" );
		int at = src.indexOf( "t.at( 0.05f * i" );
		assertTrue( at >= 0 );
		assertTrue( src.indexOf( "if (GameScene.layer( GameScene.Layer.EFFECTS ) != null) Random.Float();", at ) > at );
	}

	@Test
	public void theWaltzLeavesNoBloodWhereNoneWasShed(){
		//Blood Dance costs mana, not blood: its steps are light circling him, never splats on the floor
		for (int k = 0; k < 8; k++) BloodDance.step( Dungeon.hero.sprite, k );
		frames( 1 );
		assertEquals( 0, FxDecal.live() );
		assertTrue( FxBudget.live( FxBudget.PARTICLES ) >= 8 );
		frames( 60 );
		assertEquals( 0, FxDecal.live() );
	}

	@Test
	public void deadfallsWeightDrawsAsTheItemsRainDid(){
		//a MissileSprite's first flight in a run loads what draws once for good (a class's static
		//setup): taken out of the count, as SkillKitTest does
		((xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite) new Group().recycle( xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite.class ))
				.reset( new PointF( 10, 10 ), new PointF( 60, 90 ), new StoneOfBlast(), null );
		StoneOfBlast look = new StoneOfBlast();
		final int cell = at( 6, 4 );
		for (int n = 1; n <= 2; n++){
			final int k = n;
			int old = EnchantFxTest.draws( () -> SkillFX.rain( cell, look, k, null ) );
			assertEquals( 4 * n, old );
			//the middle: one weight, any second fall its shock; the rest: shock
			assertEquals( old, EnchantFxTest.draws( () -> {
				SkillFX.rain( cell, look, SkillFX.Fall.WEIGHT, 1, () -> {} );
				SkillFX.rain( cell, look, SkillFX.Fall.SHOCK, k - 1, null );
			} ) );
			assertEquals( old, EnchantFxTest.draws( () -> SkillFX.rain( cell, look, SkillFX.Fall.SHOCK, k, () -> {} ) ) );
			//and the same numbers in the same order: what comes after them is the same
			com.watabou.utils.Random.pushGenerator( 99 );
			SkillFX.rain( cell, look, k, null );
			float a = com.watabou.utils.Random.Float();
			com.watabou.utils.Random.popGenerator();
			com.watabou.utils.Random.pushGenerator( 99 );
			SkillFX.rain( cell, look, SkillFX.Fall.WEIGHT, 1, null );
			SkillFX.rain( cell, look, SkillFX.Fall.SHOCK, k - 1, null );
			float b = com.watabou.utils.Random.Float();
			com.watabou.utils.Random.popGenerator();
			assertEquals( a, b, 0f );
			frames( 60 );
		}
	}

	// ------------------------------------------------------------------ timing the game waits on

	@Test
	public void aBindBitesOnTheChainsFrame(){
		PointF from = new PointF( 50, 70 ), to = new PointF( 90, 74 );
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			final int[] chain = { -1 }, bite = { -1 };
			final int[] frame = { 0 };
			mobs.add( new Chains( new PointF( from.x, from.y ), new PointF( to.x, to.y ), Effects.Type.ETHEREAL_CHAIN,
					() -> chain[0] = frame[0] ) );
			mobs.add( new BladeBind.Bite( new PointF( from.x, from.y ), new PointF( to.x, to.y ), () -> bite[0] = frame[0] ) );
			for (frame[0] = 1; frame[0] <= 200 && (chain[0] < 0 || bite[0] < 0); frame[0]++) frames( 1 );
			assertTrue( chain[0] > 0 );
			assertEquals( "at " + dt, chain[0], bite[0] );
		}
	}

	// ------------------------------------------------------------------ the banner

	@Test
	public void theBannerStandsAsTwoQuadsAndALightAndGoesWithThem(){
		int cell = at( 6, 4 );
		BannerFX b = BannerFX.plant( cell, true );
		assertNotNull( b.parent );
		frames( 30 );
		assertEquals( 2, visible( b ) );
		assertTrue( b.lit() );
		assertEquals( 1, FxLight.livePersistent() );
		//it flutters A B C B
		assertEquals( 0, BannerFX.clothFrame( 0 ) );
		assertEquals( 1, BannerFX.clothFrame( 1 / 6f ) );
		assertEquals( 2, BannerFX.clothFrame( 2 / 6f ) );
		assertEquals( 1, BannerFX.clothFrame( 3 / 6f ) );
		//it rises from its foot
		assertTrue( BannerFX.poleRows( 0 ) < FxFrames.BANNER_POLE[3] );
		assertEquals( FxFrames.BANNER_POLE[3], BannerFX.poleRows( BannerFX.RISE ) );
		//its breath within ALPHA +- BREATH
		for (float t = 0; t < 3; t += 0.05f){
			float a = BannerFX.alphaAt( t );
			assertTrue( a >= BannerFX.ALPHA - BannerFX.BREATH - 1e-4f && a <= BannerFX.ALPHA + BannerFX.BREATH + 1e-4f );
		}
		Fx.dispose( b );
		assertFalse( b.alive );
		assertNull( b.parent );
		frames( 30 );
		assertEquals( 0, FxLight.live() );
	}

	@Test
	public void theStandardLetsGoOfItsBannerByTheKit() throws Exception {
		String src = source( "actors/buffs/BannerStandard.java" );
		assertTrue( src.contains( "Fx.dispose( standard )" ) );
		assertFalse( "a spear lying there", src.contains( "ItemSprite" ) );
		assertFalse( src.contains( "YELLOW_LIGHT" ) );
	}

	// ------------------------------------------------------------------ the stagger

	@Test
	public void theStaggerIsFootingLostNeverAStunsStars(){
		CharSprite a = rat( at( 5, 4 ) );
		SkillInteractions.Mark m = new SkillInteractions.Mark();
		m.kind = SkillInteractions.Mark.STAGGER;
		m.rank = 2;
		m.target = a.ch;
		SkillMarkFX fx = new SkillMarkFX( m );
		mobs.add( fx );
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		for (int f = 0; f < 150; f++){
			frames( 1 );
			assertEquals( "a stun's stars", 0, framed( FxFrames.STAR4_5 ) );
			int rings = 0;
			for (Gizmo g : floor.membersView()){
				if (g instanceof FxPic && g.visible && sameFrame( (Image) g, FxFrames.RING_13 )){
					rings++;
					float x = ((Image) g).x;
					assertEquals( "whole pixels", Math.round( x ), x, 0f );
				}
			}
			assertEquals( 1, rings );
			assertTrue( "at most two skids", framed( FxFrames.PUFF_S[0] ) + framed( FxFrames.PUFF_S[1] ) <= SkillMarkFX.SKID_MOST );
		}
		for (float t = 0; t < 3; t += 0.01f) assertTrue( Math.abs( SkillMarkFX.rock( t ) ) <= SkillMarkFX.STAGGER_ROCK );
		//it rocks: both ways
		boolean left = false, right = false;
		for (float t = 0; t < 1; t += 0.01f){
			left |= SkillMarkFX.rock( t ) < 0;
			right |= SkillMarkFX.rock( t ) > 0;
		}
		assertTrue( left && right );
		Fx.dispose( fx );
		frames( 2 );
		assertEquals( 0, framed( FxFrames.RING_13 ) );
	}

	@Test
	public void aTetherKeepsToItsLinksAndGoes(){
		CharSprite a = rat( at( 7, 4 ) );
		SkillMarkFX.tether( Dungeon.hero, a.ch, BladeBind.STEEL, BladeBind.TETHER_LIFE, -2 );
		SkillMarkFX.tether( Dungeon.hero, a.ch, BladeBind.STEEL, BladeBind.TETHER_LIFE, 2 );
		int most = 0;
		for (int f = 0; f < 40; f++){
			frames( 1 );
			most = Math.max( most, framed( FxFrames.SPARK_3 ) );
		}
		assertTrue( most > 4 );
		assertTrue( most <= 2 * SkillMarkFX.TETHER_LINKS );
		assertEquals( 0, framed( FxFrames.SPARK_3 ) );
		//thrown out, then held
		assertTrue( SkillMarkFX.tetherOut( 0 ) == 0 && SkillMarkFX.tetherOut( SkillMarkFX.TETHER_OUT ) == 1f );
	}

	// ------------------------------------------------------------------ the bound mark

	@Test
	public void theBoundMarkCirclesAnAllyOnlyAndGoesWithItsSprite(){
		SummonedPet pet = pet( at( 4, 6 ) );
		pet.updateSpriteState();
		frames( 5 );
		FxOrbit.Ring r = FxOrbit.get( pet.sprite, FxOrbit.Slot.MARK );
		assertNotNull( r );
		assertEquals( 1, FxOrbit.count( pet.sprite ) );
		//turned: no mark
		pet.alignment = Char.Alignment.ENEMY;
		frames( 2 );
		assertEquals( 0, FxOrbit.count( pet.sprite ) );
		pet.alignment = Char.Alignment.ALLY;
		frames( 2 );
		assertEquals( 1, FxOrbit.count( pet.sprite ) );
		//relinked: one mark, never two keepers
		pet.updateSpriteState();
		frames( 2 );
		assertEquals( 1, count( mobs, SummonedPet.BoundMark.class ) );
		//its sprite gone: the mark goes
		pet.sprite.killAndErase();
		frames( 2 );
		assertEquals( 0, count( mobs, SummonedPet.BoundMark.class ) );
		assertNull( FxOrbit.get( pet.sprite, FxOrbit.Slot.MARK ) );
	}

	// ------------------------------------------------------------------ the GPU

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		CharSprite a = rat( at( 5, 4 ) );
		failOnGl();
		makeEverything( a );
		frames( 40 );
	}

	//every new picture of the package once
	private void makeEverything( CharSprite a ){
		CharSprite h = Dungeon.hero.sprite;
		StoneSkin.crack( h );
		StoneSkin.regrow( h );
		Warbreaker.shatter( a );
		Hamstring.legs( a );
		LockSmith.found( at( 6, 6 ) );
		SummonCrab.arrival( at( 2, 5 ) );
		for (int k = 0; k < 8; k++) BloodDance.step( h, k );
		SkillMarkFX.tether( Dungeon.hero, a.ch, BladeBind.STEEL, BladeBind.TETHER_LIFE );
		BannerFX.plant( at( 7, 6 ), true );
		SkillInteractions.Mark m = new SkillInteractions.Mark();
		m.kind = SkillInteractions.Mark.STAGGER;
		m.target = a.ch;
		mobs.add( new SkillMarkFX( m ) );
	}

	// ------------------------------------------------------------------ nothing borrowed

	@Test
	public void thePackagesSkillsNoLongerBorrowTheGamesLooks() throws Exception {
		String[][] banned = {
				{ "actors/buffs/UndyingWillWard.java", "Speck.SCREAM" },
				{ "actors/hero/skills/UndyingWill.java", "new Flare(" },
				{ "actors/hero/skills/StoneSkin.java", "Speck.ROCK" },
				{ "actors/hero/skills/Endurance.java", "Speck.ROCK", "Speck.FORGE" },
				{ "actors/hero/skills/Groundwork.java", "Speck.ROCK", "SkillSpectacleFX" },
				{ "actors/hero/skills/KnockBack.java", "Speck.ROCK", "Speck.FORGE" },
				{ "effects/skillfx/StanceAura.java", "Speck.FORGE", "Speck.STAR" },
				{ "actors/hero/skills/FirmHand.java", "Speck.FORGE" },
				{ "actors/hero/skills/Shieldbearer.java", "Speck.ROCK", "new Flare(" },
				{ "actors/hero/skills/Toughness.java", "Speck.FORGE", "BloodParticle" },
				{ "actors/hero/skills/Warbreaker.java", "Speck.STAR" },
				{ "actors/hero/skills/Challenge.java", "Speck.STAR", "new Flare(" },
				{ "actors/hero/skills/ComboOpener.java", "Sounds.BLAST", "new Flare(" },
				{ "actors/hero/skills/Aplomb.java", "Speck.FORGE", "new Flare(" },
				{ "actors/hero/skills/BladeBind.java", "new Chains(", "ETHEREAL_CHAIN", "SparkParticle", "Speck.STAR" },
				{ "actors/hero/skills/IronTip.java", "SparkParticle" },
				{ "actors/hero/skills/LockSmith.java", "SparkParticle" },
				{ "actors/hero/skills/Lunge.java", "SkillSpectacleFX" },
				{ "actors/hero/skills/TrueEdge.java", "Beam." },
				{ "actors/hero/skills/Hamstring.java", "Speck.BONE" },
				{ "actors/hero/skills/Deadfall.java", "Speck.ROCK", "Sounds.ROCKS, cell, 1f", "rain( at, look, 1 + Random" },
				{ "actors/hero/skills/Fleche.java", "Speck.STAR" },
				{ "actors/hero/skills/Hurl.java", "Speck.STAR", "new Flare(" },
				{ "actors/hero/skills/DragonKick.java", "Speck.ROCK", "new Flare(" },
				{ "actors/hero/skills/Earthshatter.java", "Speck.ROCK" },
				{ "actors/hero/skills/Overwatch.java", "Speck.ROCK" },
				{ "actors/hero/skills/Accuracy.java", "new Flare(" },
				{ "actors/hero/skills/WeaponBond.java", "new Flare(" },
				{ "actors/hero/skills/BloodDance.java", "new Flare(", "Splash.at( c", "FxDecal" } };
		for (String[] b : banned){
			String src = source( b[0] );
			for (int i = 1; i < b.length; i++) assertFalse( b[0] + " uses " + b[i], src.contains( b[i] ) );
		}
		//the honest ones stay: the bite's sound, Blood Dance's real cuts
		assertTrue( source( "actors/hero/skills/BloodDance.java" ).contains( "Splash.at( enemy.pos" ) );
		//no star speck anywhere in the package's skills any more (it fell off the screen): a body
		//really dazed twinkles in place in the stun's amber (SkillBursts.DAZE)
		for (String f : new String[]{ "Aplomb", "DragonKick", "Lunge", "IronStance", "LastStand", "Pirouette", "Aggression",
				"Mastery", "ParryStance", "RiposteStance", "CounterTime", "BladeMastery", "Precision", "Rampage", "SecondWind",
				"Accuracy", "BladeBind", "WeaponBond", "Warbreaker", "Challenge", "Fleche", "Hurl" }){
			assertFalse( f + " bursts the falling star", source( "actors/hero/skills/" + f + ".java" ).contains( "Speck.STAR" ) );
		}
		assertTrue( source( "actors/hero/skills/Aplomb.java" ).contains( "burst( SkillBursts.DAZE, 4 )" ) );
		assertTrue( source( "actors/hero/skills/DragonKick.java" ).contains( "burst( SkillBursts.DAZE, 4 )" ) );
		assertTrue( source( "actors/hero/skills/Lunge.java" ).contains( "?SkillBursts.DAZE:SkillBursts.METAL_SPARK,4)" ) );
		//a swapped burst keeps its quantity (a plain emitter draws two of the game's Random a
		//particle): the held pain's throb is master's three drops of blood
		assertTrue( source( "actors/hero/skills/Toughness.java" ).contains( "burst( SkillBursts.HEARTBEAT, 3 )" ) );
	}

	// ------------------------------------------------------------------ helpers

	private SummonedPet pet( int cell ){
		SummonedPet pet = new SummonedPet( SummonedPet.PET_TYPES.CRAB );
		CharSprite s = pet.sprite();
		link( s, pet, cell );
		return pet;
	}

	private static String source( String path ) throws Exception {
		return new String( Files.readAllBytes( new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/" + path ).toPath() ), "UTF-8" );
	}

	private static int visible( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()) if (m != null && m.exists && m.visible && m instanceof Image) n++;
		return n;
	}

	private static int count( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.exists && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += count( (Group) m, c );
		}
		return n;
	}

	//pictures of the kit's sheet showing this frame, anywhere
	private int framed( int[] frame ){
		int n = count( mobs, frame );
		for (Group g : layers) n += count( g, frame );
		return n;
	}

	private static int count( Group g, int[] frame ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive || !m.visible) continue;
			if (m instanceof Group) n += count( (Group) m, frame );
			else if (m instanceof Image && sameFrame( (Image) m, frame )) n++;
		}
		return n;
	}

	private static boolean sameFrame( Image i, int[] f ){
		if (i.texture == null || !TextureCache.contains( Assets.Effects.FX_SHEET ) || i.texture != TextureCache.get( Assets.Effects.FX_SHEET )) return false;
		RectF r = i.frame();
		return Math.round( r.left * i.texture.width ) == f[0] && Math.round( r.top * i.texture.height ) == f[1]
				&& Math.round( (r.right - r.left) * i.texture.width ) == f[2] && Math.round( (r.bottom - r.top) * i.texture.height ) == f[3];
	}
}
