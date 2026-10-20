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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.DivineWrathGround;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MeteorFall;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SacredWeaponSwords;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaShieldWard;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aegis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Condemn;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DragonKick;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostNova;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Grace;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HeadShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HealingPrayer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LastStand;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Litany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MassHeal;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Purify;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sorcerer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpellBlade;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WeaponBond;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillFlight;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ArcaneEcho;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BloodDance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WildCall;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.BannerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ConeAOE;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The skill FX batch together (docs/fx/skills.md): the five packages' loudest pictures in one fight
 * at once (a roar over ten with its flinches and a reach-5 thrust, a howl maddening four, Deadfall's
 * nine blows with a standard planted and Blood Dance's eight steps, a hallowed ground with three
 * blades and four pillars, a meteor's strike with its heat and its chips, an arrow storm landing, an
 * echo and a wolf's bite) stay under every one of the kit's caps, frame by frame, and once the held
 * pictures are let go of nothing of them is left.
 *
 * The second pass (rfx) together: the first pass's fight, the kit's loud moment and the four
 * packages' own (the runes' busy arcane moment, Shadow Link's bonds and currents with held pairs,
 * rays, peels, a volley of spirit arrows and strokes with their extras, the holy-heal moment played
 * for real, the martial moment with its dazes and Blade Bind's pull) all at once on a phone's
 * numbers: every counted cap holds frame by frame, the particles never pass HARD, no emitter is
 * left emitting forever, and once it is over nothing is left.
 */
public class SfxBatchTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Object savedScene;
	private Group[] layers;
	private Group mobs;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedCam = Camera.main;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedScene = sceneField().get( null );
		stubGl();
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		resetKit();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 17 );
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
		bareScene();
	}

	@After
	public void tearDown() throws Exception {
		sceneField().set( null, savedScene );
		GameScene.useLayersForTests( null );
		resetKit();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	@Test
	public void theWholeBatchInOneFightStaysUnderTheKitsCaps(){
		CharSprite hs = Dungeon.hero.sprite;
		CharSprite[] r = new CharSprite[10];
		for (int i = 0; i < r.length; i++) r[i] = rat( at( 5 + i % 5, 2 + i / 5 * 4 ) );
		float hx = SkillVfx.midX( hs ), hy = SkillVfx.midY( hs );

		//kit-lance: War Cry over all ten, Impaling Thrust down a lane of five, through three
		ShoutFX.roar( hs, ShoutFX.MARTIAL, 72 );
		for (int i = 0; i < r.length; i++){
			r[i].emitter().burst( SkillBursts.TREMBLE, 1 );
			ShoutFX.flinch( r[i], hx, hy, ShoutFX.MARTIAL, 0.02f * i );
		}
		LanceFX.thrust( Dungeon.hero.pos, at( 8, 4 ), 5, LanceFX.STEEL, hs );
		for (int i = 0; i < 3; i++) LanceFX.pierce( r[i], 0, LanceFX.STEEL );

		//dread-shadow: Dread Howl maddening four, frightening the rest; an Ambush stake
		ShoutFX.howl( hs, ShoutFX.DREAD, 5, 0.06f );
		for (int i = 0; i < r.length; i++){
			CellEmitter.get( r[i].ch.pos ).burst( SkillBursts.DREAD, 3 );
			if (i < 4) DreadFX.madness( r[i] );
			else ShoutFX.flinch( r[i], hx, hy, DreadFX.DREAD_FLINCH, 0.06f * i );
		}
		DreadFX.stake( r[4] );

		//martial-steel: Deadfall's nine blows, a standard planted, Blood Dance's eight steps,
		//Blade Bind's two tethers
		int center = at( 6, 4 );
		for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++){
			WarriorImpactFX.show( center + dx + dy * W, dx == 0 && dy == 0 );
		}
		BannerFX banner = BannerFX.plant( at( 2, 6 ), true );
		for (int k = 0; k < 8; k++) BloodDance.step( hs, k );
		SkillMarkFX.tether( r[0].ch, r[1].ch, 0xB9DEEE, 0.6f, -2 );
		SkillMarkFX.tether( r[0].ch, r[1].ch, 0xB9DEEE, 0.6f, 2 );

		//holy-light: Divine Wrath's hallowed ground of nine, three blades of light, four pillars
		int[] ground = new int[9];
		for (int i = 0; i < 9; i++) ground[i] = at( 6 + i % 3, 5 + i / 3 );
		DivineWrathGround.Hallow hallow = DivineWrathGround.Hallow.show( ground, ground );
		SacredWeaponSwords.Blades blades = SacredWeaponSwords.Blades.show( new int[]{ at( 2, 2 ), at( 2, 3 ), at( 1, 5 ) }, new int[0] );
		for (int i = 5; i < 9; i++) PillarRiseFX.show( r[i].ch.pos, 0xF1D99C, null );
		PixelFlare.show( hx, hy, 0xF1D99C, PixelFlare.L, 0.8f );

		//hunt-elements: a meteor striking with its chips and embers, another's heat gathering,
		//Ember Arrows' kill, an arrow storm landing, Arcane Echo, Wild Call's bite
		int stone = at( 9, 4 );
		MeteorFall.impact( stone );
		CellEmitter.center( stone ).burst( SkillBursts.STONE_CHIP, 30 );
		for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++){
			CellEmitter.get( stone + dx + dy * W ).burst( SkillBursts.EMBER_MOTE, 4 );
		}
		MeteorFall.HeatMark heat = MeteorFall.HeatMark.at( at( 11, 6 ) );
		EmberArrows.heatRing( r[7].ch.pos );
		ArrowRain.stormLands( at( 10, 2 ) );
		ArcaneEcho.echo( r[3].ch.pos, Dungeon.hero.pos );
		WildCall.bite( r[2].ch.pos, Dungeon.hero.pos );

		//the second pass's kit (rfx): a ward volley, Scouring Flame's cone, shadow binds, a held link,
		//holy rays, heal flows, a ward locking and breaking
		Gizmo held = kitMoment( r );

		int particles = 0, flares = 0, lights = 0, persistent = 0, ghosts = 0, rings = 0, decals = 0, lances = 0, spectacles = 0;
		int runes = 0, links = 0, rays = 0;
		for (int f = 0; f < 120; f++){
			frames( 1 );
			particles = Math.max( particles, FxBudget.live( FxBudget.PARTICLES ) );
			flares = Math.max( flares, PixelFlare.live() );
			lights = Math.max( lights, FxLight.live() );
			persistent = Math.max( persistent, FxLight.livePersistent() );
			ghosts = Math.max( ghosts, Afterimage.live() );
			rings = Math.max( rings, FxRing.live() );
			decals = Math.max( decals, FxDecal.live() );
			lances = Math.max( lances, LanceFX.live() );
			spectacles = Math.max( spectacles, SkillSpectacleFX.live() );
			runes = Math.max( runes, RuneFX.live() );
			links = Math.max( links, LinkFX.live() );
			rays = Math.max( rays, SkillRay.full() );
		}
		assertTrue( "runes " + runes, runes <= RuneFX.CAP );
		assertTrue( "links " + links, links <= LinkFX.CAP );
		assertTrue( "rays " + rays, rays <= SkillRay.FULL );
		assertTrue( "particles " + particles, particles <= FxBudget.soft() );
		assertTrue( "flares " + flares, flares <= PixelFlare.CAP );
		assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
		assertTrue( "persistent lights " + persistent, persistent <= FxBudget.persistentLightCap() );
		assertTrue( "afterimages " + ghosts, ghosts <= FxBudget.afterimageCap() );
		assertTrue( "rings " + rings, rings <= FxBudget.ringCap() );
		assertTrue( "decals " + decals, decals <= FxBudget.decalCap() );
		assertTrue( "lances " + lances, lances <= LanceFX.CAP );
		assertTrue( "spectacles " + spectacles, spectacles <= SkillSpectacleFX.CAP );

		//the held pictures let go of as their buffs would: nothing of the fight is left
		Fx.dispose( banner );
		hallow.fadeExcept( new int[0] );
		blades.fadeExcept( new int[0] );
		heat.release();
		Fx.dispose( held );
		frames( 600 );
		assertEquals( "runes", 0, RuneFX.live() );
		assertEquals( "links", 0, LinkFX.live() );
		assertEquals( "rays", 0, SkillRay.full() );
		assertEquals( "rich bolts", 0, SkillBolt.richLive() );
		assertEquals( "particles", 0, FxBudget.live( FxBudget.PARTICLES ) );
		assertEquals( "flares", 0, PixelFlare.live() );
		assertEquals( "lights", 0, FxLight.live() );
		assertEquals( "afterimages", 0, Afterimage.live() );
		assertEquals( "rings", 0, FxRing.live() );
		assertEquals( "lances", 0, LanceFX.live() );
		assertEquals( "spectacles", 0, SkillSpectacleFX.live() );
	}

	//the kit's loud moment: Spirit Volley's four ward bolts, Scouring Flame's cone at reach 5 (its
	//real ConeAOE: a holy bolt down each of its rays, on one frame), two shadow binds, a held link,
	//two holy rays, three heal flows, a ward locking on and one breaking; returns the held link
	private Gizmo kitMoment( CharSprite[] r ){
		CharSprite hs = Dungeon.hero.sprite;
		for (int i = 0; i < 4; i++) SkillBolt.fly( mobs, hs.center(), r[i].destinationCenter(), SkillBolt.RUNE, null );
		cone();
		LinkFX.bind( mobs, r[0].center(), r[1].center(), LinkFX.SHADOW, null );
		LinkFX.bind( mobs, r[2].center(), r[5].center(), LinkFX.SHADOW, null );
		Gizmo held = LinkFX.held( r[3], r[4], LinkFX.SHADOW );
		SkillRay.show( hs, r[6], SkillRay.HOLY, 0.5f );
		SkillRay.show( hs, r[7], SkillRay.HOLY, 0.5f );
		for (int i = 0; i < 3; i++) SkillRay.show( hs, r[i], SkillRay.HEAL, 0.6f );
		RuneFX.lock( r[8], 6, SkillPalette.RUNE_MANA, 8, 4, 0.08f, 0.3f );
		RuneFX.scatter( r[9], 6, SkillPalette.RUNE_MANA, 10, 0.35f );
		return held;
	}

	//Scouring Flame at +3 (reach 5, 50 degrees) toward the east: a holy bolt down each ray of the
	//cone, as ScouringFlame flies them; how many
	private int cone(){
		PathFinder.setMapSize( W, W );
		Ballistica aim = new Ballistica( Dungeon.hero.pos, at( 9, 4 ), Ballistica.STOP_SOLID );
		ConeAOE cone = new ConeAOE( aim, 5, 50, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET );
		for (Ballistica ray : cone.rays){
			int end = ray.path.get( ray.dist );
			SkillBolt.fly( mobs, Dungeon.hero.sprite.center(), DungeonTilemap.raisedTileCenterToWorld( end ), SkillBolt.HOLY, null );
		}
		return cone.rays.size();
	}

	@Test
	public void theKitsLoudMomentStaysUnderEveryCapAndItsLoadRefusesNone() throws Exception {
		CharSprite[] r = new CharSprite[10];
		for (int i = 0; i < r.length; i++) r[i] = rat( at( 5 + i % 4, 1 + i / 4 * 3 ) );
		//a phone's numbers (SOFT 360, lights 24), not the desktop's half again
		Field scale = FxBudget.class.getDeclaredField( "scale" );
		scale.setAccessible( true );
		float desktop = scale.getFloat( null );
		scale.setFloat( null, 1f );
		try {
			phoneMoment( r );
		} finally {
			scale.setFloat( null, desktop );
		}
	}

	private void phoneMoment( CharSprite[] r ){
		assertTrue( "the cone's rays at reach 5", new ConeAOE( new Ballistica( Dungeon.hero.pos, at( 9, 4 ), Ballistica.STOP_SOLID ), 5, 50,
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET ).rays.size() >= 10 );
		Gizmo held = kitMoment( r );
		int most = 0, lights = 0, crowded = 0, starved = 0;
		for (int f = 0; f < 120; f++){
			//the load before each frame's spawns: past 80% of SOFT the body (P2) would thin
			if (FxBudget.factor( FxBudget.P2 ) < 1f) crowded++;
			frames( 1 );
			if (FxBudget.spawnsLeft() < 0) starved++;
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
			lights = Math.max( lights, FxLight.live() );
			assertTrue( "runes", RuneFX.live() <= RuneFX.CAP );
			assertTrue( "links", LinkFX.live() <= LinkFX.CAP );
			assertTrue( "rays", SkillRay.full() <= SkillRay.FULL );
		}
		assertTrue( "particles " + most, most <= FxBudget.soft() );
		assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
		System.out.println( "rfx kit moment: peak particles " + most + ", lights " + lights + ", frames crowded " + crowded
				+ ", frames out of spawns " + starved );
		Fx.dispose( held );
		frames( 600 );
		assertEquals( 0, FxBudget.live( FxBudget.PARTICLES ) );
		assertEquals( 0, RuneFX.live() );
		assertEquals( 0, LinkFX.live() );
		assertEquals( 0, SkillRay.full() );
		assertEquals( 0, SkillBolt.richLive() );
	}

	@Test
	public void scouringFlamesConeAtReachFiveRefusesNoBodyParticle() throws Exception {
		//the worst bolt moment (plan 3.2): ten or more holy bolts on one frame, four rich, the rest lean,
		//never loud enough that the budget thins their bodies (P0 to P2), on a phone's numbers
		Field scale = FxBudget.class.getDeclaredField( "scale" );
		scale.setAccessible( true );
		float desktop = scale.getFloat( null );
		scale.setFloat( null, 1f );
		try {
			int rays = cone();
			assertTrue( "rays " + rays, rays >= 10 );
			int most = 0, lights = 0;
			for (int f = 0; f < 90; f++){
				assertEquals( "frame " + f + ": the body thinned at " + FxBudget.live( FxBudget.PARTICLES ), 1f, FxBudget.factor( FxBudget.P2 ), 0f );
				frames( 1 );
				assertTrue( "frame " + f + ": out of spawns", FxBudget.spawnsLeft() >= 0 );
				most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
				lights = Math.max( lights, FxLight.live() );
			}
			System.out.println( "rfx scouring flame at reach 5: " + rays + " bolts, peak particles " + most + ", lights " + lights );
			assertTrue( lights <= FxBudget.lightCap() );
		} finally {
			scale.setFloat( null, desktop );
		}
		frames( 120 );
		assertEquals( 0, SkillBolt.richLive() );
	}

	@Test
	public void anImpactWithTheBudgetFullStartsNoEndlessBurst() throws Exception {
		//the budget full, an impact is allowed no sparks: it must not start its emitter with none
		//(no interval and no quantity emits forever and hangs the frame; a meteor struck in a busy
		//fight did)
		int full = FxBudget.hard();
		FxBudget.add( FxBudget.PARTICLES, full );
		try {
			assertEquals( 0, FxBudget.allow( FxRecipes.BURST, FxBudget.P2 ) );
			FxRecipes.impact( 80, 70, Element.EARTH, 1.2f );
			FxRecipes.impact( 80, 70, Element.FIRE, 1f );
			MeteorFall.impact( at( 6, 4 ) );
			for (Group g : layers) assertEquals( 0, endless( g ) );
		} finally {
			FxBudget.add( FxBudget.PARTICLES, -full );
		}
		frames( 30 );
	}

	/** Every picture of both passes at once on a phone's numbers (far louder than any real fight):
	 *  the budget thins what it must, but every counted cap holds frame by frame, the particles never
	 *  pass HARD, no emitter emits forever, and once the held pictures are let go of nothing is left. */
	@Test
	public void bothPassesAllAtOnceKeepEveryCapOnAPhone() throws Exception {
		Field scale = FxBudget.class.getDeclaredField( "scale" );
		scale.setAccessible( true );
		float desktop = scale.getFloat( null );
		scale.setFloat( null, 1f );
		EnchantFxTest.boot();
		DreadShadowProcs t = new DreadShadowProcs();
		t.up();
		try {
			resetKit();
			Game.elapsed = 1 / 60f;
			everythingAtOnce( t );
		} finally {
			t.down();
			scale.setFloat( null, desktop );
		}
	}

	private void everythingAtOnce( DreadShadowProcs t ) throws Exception {
		Hero hero = t.hero;
		hero.HP = 40;
		hero.HT = 100;
		hero.MP = hero.MT = 500;
		Group stage = t.rig.stage;
		CharSprite hs = hero.sprite;
		float hx = SkillVfx.midX( hs ), hy = SkillVfx.midY( hs );

		//the martial moment's places by the hero (3, 4): Dragon Kick into a wall with two beside where
		//it lands, Blade Bind's bound foe; the holy-heal moment's four allies round him
		Rat victim = t.rat( t.at( 4, 4 ) );
		t.rat( t.at( 5, 3 ) );
		t.rat( t.at( 5, 5 ) );
		int wall = t.at( 6, 4 );
		Dungeon.level.solid[wall] = true;
		Dungeon.level.passable[wall] = false;
		Rat bound = t.rat( t.at( 3, 3 ) );
		for (int[] c : new int[][]{ { 4, 3 }, { 2, 3 }, { 4, 5 }, { 2, 5 } }) RfxMeaningProcs.ally( t, c[0], c[1] );
		//Head Shot's ring-out on three
		Rat struck = t.rat( t.at( 9, 2 ) );
		t.rat( t.at( 8, 2 ) );
		t.rat( t.at( 10, 2 ) );
		t.rat( t.at( 9, 1 ) );
		//everyone else, east of the hero
		java.util.List<Rat> crowd = new java.util.ArrayList<>();
		for (int y = 3; y <= 14 && crowd.size() < 34; y++) for (int x = 7; x <= 14 && crowd.size() < 34; x++){
			if (y == 4 && x < 8) continue;
			crowd.add( t.rat( t.at( x, y ) ) );
		}
		CharSprite[] r = new CharSprite[crowd.size()];
		for (int i = 0; i < r.length; i++) r[i] = crowd.get( i ).sprite;

		//the first pass: War Cry over ten, a reach-5 thrust through three, Dread Howl maddening four,
		//Deadfall's nine blows, a standard, Blood Dance's steps, a hallowed ground of nine, three
		//blades, four pillars, a meteor's strike with its chips and embers, another's heat, an arrow
		//storm, an echo, a wolf's bite
		ShoutFX.roar( hs, ShoutFX.MARTIAL, 72 );
		for (int i = 0; i < 10; i++){
			r[i].emitter().burst( SkillBursts.TREMBLE, 1 );
			ShoutFX.flinch( r[i], hx, hy, ShoutFX.MARTIAL, 0.02f * i );
		}
		LanceFX.thrust( hero.pos, t.at( 8, 4 ), 5, LanceFX.STEEL, hs );
		for (int i = 0; i < 3; i++) LanceFX.pierce( r[i], 0, LanceFX.STEEL );
		ShoutFX.howl( hs, ShoutFX.DREAD, 5, 0.06f );
		for (int i = 0; i < 10; i++){
			CellEmitter.get( r[i].ch.pos ).burst( SkillBursts.DREAD, 3 );
			if (i < 4) DreadFX.madness( r[i] );
		}
		DreadFX.stake( r[4] );
		int center = t.at( 11, 9 );
		for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) WarriorImpactFX.show( center + dx + dy * W, dx == 0 && dy == 0 );
		BannerFX banner = BannerFX.plant( t.at( 1, 7 ), true );
		for (int k = 0; k < 8; k++) BloodDance.step( hs, k );
		int[] ground = new int[9];
		for (int i = 0; i < 9; i++) ground[i] = t.at( 8 + i % 3, 11 + i / 3 );
		DivineWrathGround.Hallow hallow = DivineWrathGround.Hallow.show( ground, ground );
		SacredWeaponSwords.Blades blades = SacredWeaponSwords.Blades.show( new int[]{ t.at( 1, 9 ), t.at( 2, 9 ), t.at( 1, 10 ) }, new int[0] );
		for (int i = 5; i < 9; i++) PillarRiseFX.show( r[i].ch.pos, 0xF1D99C, null );
		PixelFlare.show( hx, hy, 0xF1D99C, PixelFlare.L, 0.8f );
		int stone = t.at( 12, 6 );
		MeteorFall.impact( stone );
		CellEmitter.center( stone ).burst( SkillBursts.STONE_CHIP, 30 );
		for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) CellEmitter.get( stone + dx + dy * W ).burst( SkillBursts.EMBER_MOTE, 4 );
		MeteorFall.HeatMark heat = MeteorFall.HeatMark.at( t.at( 13, 12 ) );
		EmberArrows.heatRing( r[7].ch.pos );
		ArrowRain.stormLands( t.at( 10, 13 ) );
		ArcaneEcho.echo( r[3].ch.pos, hero.pos );
		WildCall.bite( r[2].ch.pos, hero.pos );

		//the kit: four ward bolts, Scouring Flame's cone, two binds, a held link, two holy rays, three
		//heal flows, a ward locking, one breaking
		for (int i = 0; i < 4; i++) SkillBolt.fly( stage, hs.center(), r[10 + i].destinationCenter(), SkillBolt.RUNE, null );
		PathFinder.setMapSize( W, W );
		ConeAOE cone = new ConeAOE( new Ballistica( hero.pos, t.at( 9, 4 ), Ballistica.STOP_SOLID ), 5, 50,
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET );
		for (Ballistica ray : cone.rays){
			SkillBolt.fly( stage, hs.center(), DungeonTilemap.raisedTileCenterToWorld( ray.path.get( ray.dist ) ), SkillBolt.HOLY, null );
		}
		LinkFX.bind( stage, r[0].center(), r[1].center(), LinkFX.SHADOW, null );
		LinkFX.bind( stage, r[2].center(), r[5].center(), LinkFX.SHADOW, null );
		Gizmo held = LinkFX.held( r[3], r[4], LinkFX.SHADOW );
		SkillRay.show( hs, r[6], SkillRay.HOLY, 0.5f );
		SkillRay.show( hs, r[7], SkillRay.HOLY, 0.5f );
		for (int i = 0; i < 3; i++) SkillRay.show( hs, r[i], SkillRay.HEAL, 0.6f );
		RuneFX.lock( r[8], 6, SkillPalette.RUNE_MANA, 8, 4, 0.08f, 0.3f );
		RuneFX.scatter( r[9], 6, SkillPalette.RUNE_MANA, 10, 0.35f );

		//runes: the volley's spent motes, an echo with its chain, Mana Shield breaking and raised,
		//Spell Blade, an overload winding up, a hex, a brand, Sanctuary, Aegis, Weapon Bond, the ward cast
		for (int i = 0; i < 4; i++) SpiritArmorFX.spend( hero, r[10 + i].ch );
		ArcaneEcho.echo( r[14].ch.pos, r[3].ch.pos );
		ManaShieldWard.broke( hs );
		ManaShield.raised( hs );
		ManaShieldWard.absorbed( hs, r[0].ch.pos );
		SpellBlade.inscribed( hs, r[15] );
		RuneFX.Runes charging = Sorcerer.charging( r[16] );
		Hex.spoken( r[17] );
		Condemn.branded( r[18] );
		Sanctuary.raised( hs );
		Aegis.warded( hs, r[0].ch.pos );
		WeaponBond.bonded( hs );
		ManaShield ward = new ManaShield();
		ward.level = 1;
		SkillCastFX.play( ward, hero );

		//links: two Shadow Links with their currents, a linked pair and a cut pair held, three rays,
		//souls and fear peeled, Charged Shot's release, strokes with their extras, a volley of
		//spirit arrows
		DreadFX.tether( stage, r[19].center(), r[22].center(), null );
		DreadFX.tether( stage, r[23].center(), r[26].center(), null );
		DoubleStab.link( r[19].ch, r[20].ch, 3 );
		SkillInteractions.mark( r[21].ch, SkillInteractions.Mark.CUT, 3, 8 ).other = r[22].ch.id();
		SkillInteractions.mark( r[22].ch, SkillInteractions.Mark.CUT, 3, 8 ).other = r[21].ch.id();
		for (int i = 0; i < 3; i++) SkillRay.show( hs, r[23 + i], SkillRay.HOLY, 0.4f );
		SkillBolt.fly( stage, r[24].ch.pos, hero.pos, SkillBolt.SOUL, null ).peel( r[24], 6 );
		SkillBolt.fly( stage, r[25].center(), hs.center(), SkillBolt.MANA, null ).peel( r[25], 4 );
		SkillBolt.mote( r[20], hs, SkillBolt.SOUL, null ).peel( r[20], 6 );
		ChargedShot.release( hs, t.at( 14, 4 ) );
		StreakFX.show( hero.pos, t.at( 14, 6 ), 0xF1D99C, 0.45f, 0.25f, StreakFX.MOTES );
		StreakFX.show( hero.pos, t.at( 14, 8 ), 0xB9DEEE, 0.45f, 0.25f, StreakFX.SPARKS );
		for (int i = 0; i < 8; i++) SkillFlight.fly( stage, 120 + 12 * i, 0, 120 + 12 * i, 200, SkillFlight.SPIRIT_ARROW, 400, null );

		//meaning: Mass Heal mending four, a verse of Litany, Healing Prayer, Purify, played for real;
		//a mana gain, Grace's bloom, Frost Nova's shards and rime, a dawn's mend
		MassHeal m = DreadShadowProcs.skill( new MassHeal() );
		Litany l = DreadShadowProcs.skill( new Litany() );
		HealingPrayer p = DreadShadowProcs.skill( new HealingPrayer() );
		Purify u = DreadShadowProcs.skill( new Purify() );
		m.execute( hero, Skill.AC_CAST );
		hero.buff( MassHeal.Wings.class ).act();
		l.onSkillCast( l );
		p.execute( hero, Skill.AC_CAST );
		u.execute( hero, Skill.AC_CAST );
		MoteFX.gather( hs, 8, SkillPalette.MANA, 18f, 0.4f );
		Grace.manaBloom( hs );
		FrostNova.shards( hs );
		for (int i = 0; i < 9; i++) FrostNova.rime( r[i].ch.pos );
		SkillSequence.mended( victim );

		//stars: Dragon Kick into the wall, Head Shot's ring-out, Last Stand, Blade Bind's pull,
		//Pirouette's landing dazing four
		Method kick = DragonKick.class.getDeclaredMethod( "kick", Hero.class, Char.class );
		kick.setAccessible( true );
		kick.invoke( DreadShadowProcs.skill( new DragonKick() ), hero, victim );
		Method ring = HeadShot.class.getDeclaredMethod( "ringOut", Char.class );
		ring.setAccessible( true );
		struck.sprite.emitter().burst( SkillBursts.METAL_SPARK, 6 );
		ring.invoke( null, struck );
		DreadShadowProcs.skill( new LastStand() ).incomingDamageReduction( 90, bound );
		Method bind = BladeBind.class.getDeclaredMethod( "bind", Hero.class, int.class );
		bind.setAccessible( true );
		bind.invoke( DreadShadowProcs.skill( new BladeBind() ), hero, bound.pos );
		ArcSpinFX.around( hs, 0xCCE0FF, 10, 0.5f, 0, 1080, 0.5f, true );
		for (int i = 27; i < 31; i++) r[i].emitter().burst( SkillBursts.DAZE, 3 );

		int particles = 0, flares = 0, lights = 0, persistent = 0, ghosts = 0, rings = 0, decals = 0, lances = 0, spectacles = 0;
		int runes = 0, links = 0, rays = 0, rich = 0, cored = 0, endless = 0;
		for (int f = 0; f < 150; f++){
			if (f == 30) Sorcerer.discharged( charging, r[16].ch.pos );
			t.rig.frames( 1, true );
			particles = Math.max( particles, FxBudget.live( FxBudget.PARTICLES ) );
			flares = Math.max( flares, PixelFlare.live() );
			lights = Math.max( lights, FxLight.live() );
			persistent = Math.max( persistent, FxLight.livePersistent() );
			ghosts = Math.max( ghosts, Afterimage.live() );
			rings = Math.max( rings, FxRing.live() );
			decals = Math.max( decals, FxDecal.live() );
			lances = Math.max( lances, LanceFX.live() );
			spectacles = Math.max( spectacles, SkillSpectacleFX.live() );
			runes = Math.max( runes, RuneFX.live() );
			links = Math.max( links, LinkFX.live() );
			rays = Math.max( rays, SkillRay.full() );
			rich = Math.max( rich, SkillBolt.richLive() );
			cored = Math.max( cored, SkillFlight.cored() );
			if (f % 10 == 0){
				for (Group g : t.rig.layers) endless += endless( g );
				endless += endless( stage );
			}
		}
		System.out.println( "rfx both passes at once (phone): peak particles " + particles + " (SOFT " + FxBudget.soft() + ", HARD "
				+ FxBudget.hard() + "), lights " + lights + ", persistent " + persistent + ", flares " + flares + ", rings " + rings
				+ ", decals " + decals + ", afterimages " + ghosts + ", runes " + runes + ", links " + links + ", rays " + rays
				+ ", rich bolts " + rich + ", cored lights " + cored );
		assertEquals( "emitters left emitting forever", 0, endless );
		assertTrue( "particles " + particles, particles <= FxBudget.hard() );
		assertTrue( "runes " + runes, runes > 0 && runes <= RuneFX.CAP );
		assertTrue( "links " + links, links > 0 && links <= LinkFX.CAP );
		assertTrue( "rays " + rays, rays > 0 && rays <= SkillRay.FULL );
		assertTrue( "rich bolts " + rich, rich <= SkillBolt.RICH );
		assertTrue( "cored lights " + cored, cored <= SkillFlight.CORE_RICH );
		assertTrue( "flares " + flares, flares <= PixelFlare.CAP );
		assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
		assertTrue( "persistent lights " + persistent, persistent <= FxBudget.persistentLightCap() );
		assertTrue( "afterimages " + ghosts, ghosts <= FxBudget.afterimageCap() );
		assertTrue( "rings " + rings, rings <= FxBudget.ringCap() );
		assertTrue( "decals " + decals, decals <= FxBudget.decalCap() );
		assertTrue( "lances " + lances, lances <= LanceFX.CAP );
		assertTrue( "spectacles " + spectacles, spectacles <= SkillSpectacleFX.CAP );

		//let go of as their buffs would: the marks, the links, the standard, the ground, the blades,
		//the heat, the held bond; then everything runs out
		DoubleStab.clearLinks();
		for (Rat x : crowd) for (SkillInteractions.Mark k : x.buffs( SkillInteractions.Mark.class )) k.detach();
		Fx.dispose( banner );
		hallow.fadeExcept( new int[0] );
		blades.fadeExcept( new int[0] );
		heat.release();
		Fx.dispose( held );
		t.rig.frames( 600, true );
		assertEquals( "runes", 0, RuneFX.live() );
		assertEquals( "links", 0, LinkFX.live() );
		assertEquals( "rays", 0, SkillRay.full() );
		assertEquals( "rich bolts", 0, SkillBolt.richLive() );
		assertEquals( "cored lights", 0, SkillFlight.cored() );
		assertEquals( "particles", 0, FxBudget.live( FxBudget.PARTICLES ) );
		assertEquals( "flares", 0, PixelFlare.live() );
		assertEquals( "afterimages", 0, Afterimage.live() );
		assertEquals( "lances", 0, LanceFX.live() );
		assertEquals( "spectacles", 0, SkillSpectacleFX.live() );
	}

	// ------------------------------------------------------------------ helpers

	//emitters started with no interval and no quantity: they would emit forever
	private static int endless( Group g ) throws Exception {
		Field interval = Emitter.class.getDeclaredField( "interval" ), quantity = Emitter.class.getDeclaredField( "quantity" );
		interval.setAccessible( true );
		quantity.setAccessible( true );
		int n = 0;
		for (com.watabou.noosa.Gizmo m : g.membersView()){
			if (m instanceof Emitter && ((Emitter) m).on && (float) interval.get( m ) <= 0 && (int) quantity.get( m ) <= 0) n++;
			if (m instanceof Group) n += endless( (Group) m );
		}
		return n;
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
			Class<?> t = m.getReturnType();
			if (t == int.class) return 1;
			if (t == boolean.class) return false;
			if (t == float.class) return 0f;
			if (t == String.class) return "";
			return null;
		} );
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

	// ------------------------------------------------------------------ a scene for CellEmitter

	private static Field sceneField() throws Exception {
		Field f = GameScene.class.getDeclaredField( "scene" );
		f.setAccessible( true );
		return f;
	}

	//a bare game scene (no constructor run) holding the stand-in layers, so the skills' own
	//CellEmitter bursts work headless (SfxHuntElementsTest's)
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
}
