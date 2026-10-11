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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LightParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SunlightParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The area's particles at work, headless (stand-in layers for the game's scene): the leaves' and
 * the lights' factories are the objects co-op knows them by; a leaf pops, flutters and settles a
 * few px below where it rose, on whole pixels, floats on water with a ring and fades into high
 * grass; shafts are painted frames never scaled and hidden while they wait; every cap holds;
 * nothing draws the game's Random and nothing touches the GPU while it is made.
 */
public class NatureFxTest extends NatureTestBase {

	// ------------------------------------------------------------------ identities

	@Test
	public void theFactoriesKeepTheIdentitiesCoopKnowsThemBy() throws Exception {
		Method resolve = NetVisuals.class.getDeclaredMethod( "resolveFactory", String.class );
		resolve.setAccessible( true );
		assertSame( LeafParticle.GENERAL, resolve.invoke( null, "leaf" ) );
		assertSame( LeafParticle.LEVEL_SPECIFIC, resolve.invoke( null, "leaf_level" ) );
		assertSame( ShaftParticle.FACTORY, resolve.invoke( null, "shaft" ) );
		assertSame( SunlightParticle.FACTORY, resolve.invoke( null, "sunlight" ) );
		assertSame( FireflyParticle.FACTORY, resolve.invoke( null, "firefly" ) );
		//and the new ones bursting through cell emitters are known too
		assertSame( HolyFx.SPIRIT_WISP, resolve.invoke( null, "spirit_wisp" ) );
		assertSame( ShaftParticle.HEAL, resolve.invoke( null, "shaft_heal" ) );
		assertSame( PlantFx.SUNGRASS_SHAFT, resolve.invoke( null, "sungrass_shaft" ) );
		//the light ones say so: their emitters draw them added
		assertTrue( ShaftParticle.FACTORY.lightMode() && SunlightParticle.FACTORY.lightMode()
				&& FireflyParticle.FACTORY.lightMode() && LightParticle.FACTORY.lightMode() );
		assertFalse( LeafParticle.GENERAL.lightMode() );
	}

	// ------------------------------------------------------------------ leaves

	@Test
	public void aLeafPopsFluttersAndSettlesAFewPixelsBelowWhereItRose(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		int cell = 3 + 6 * W;
		float x = cx( cell ), y = cy( cell );
		LeafParticle p = ((LeafParticle) e.recycle( LeafParticle.class )).leaf( x, y, 0x4E9A30 );
		float top = y, flips = 0;
		boolean wasFlipped = p.flipHorizontal;
		boolean settled = false;
		Game.elapsed = 1 / 60f;
		for (int i = 0; i < 300 && p.alive; i++){
			e.update();
			top = Math.min( top, p.py );
			if (p.flipHorizontal != wasFlipped){
				flips++;
				wasFlipped = p.flipHorizontal;
			}
			if (p.vy == 0 && p.vx == 0 && p.py > y){
				settled = true;
				//lying: on whole pixels, 2-6 px below where it rose
				assertEquals( Math.round( p.x ), p.x, 0f );
				assertTrue( "settled at " + (p.py - y), p.py - y >= LeafParticle.SETTLE_MIN - 0.01f
						&& p.py - y <= LeafParticle.SETTLE_MAX + 0.01f );
			}
			//never past its ground, never a fractional scale
			assertEquals( 1f, Math.abs( p.scale.x ), 0f );
		}
		assertTrue( "it popped up", y - top >= 4f );
		assertTrue( "it turned over as it fluttered", flips >= 2 );
		assertTrue( "it settled", settled );
		assertFalse( "and faded", p.alive );
	}

	@Test
	public void aLeafOnTheWaterFloatsWithARingAndOneOnHighGrassFadesAtOnce(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		int water = 10 + 7 * W, grass = 3 + W;
		LeafParticle onWater = ((LeafParticle) e.recycle( LeafParticle.class )).leaf( cx( water ), cy( water ) - 4, 0x4E9A30 );
		LeafParticle onGrass = ((LeafParticle) e.recycle( LeafParticle.class )).leaf( cx( grass ), cy( grass ) - 4, 0x4E9A30 );
		Game.elapsed = 1 / 60f;
		Group surface = layers[GameScene.Layer.SURFACE.ordinal()];
		float grassGoneAt = -1, waterLandedAt = -1, waterGoneAt = -1;
		for (int i = 1; i <= 300; i++){
			FxBudget.frame( Game.elapsed );
			WaterFX.frame();
			for (Group g : layers) g.update();
			//its touching the water: a ring there
			if (waterLandedAt < 0 && living( surface, Ripple.class ) > 0) waterLandedAt = i / 60f;
			if (grassGoneAt < 0 && !onGrass.alive) grassGoneAt = i / 60f;
			if (waterGoneAt < 0 && !onWater.alive) waterGoneAt = i / 60f;
		}
		assertTrue( "a ring where it touched the water", waterLandedAt > 0 );
		//it floats about a second, then fades
		assertTrue( "floated " + (waterGoneAt - waterLandedAt), waterGoneAt - waterLandedAt >= LeafParticle.FLOAT - 0.05f );
		//on the high grass it was gone as it touched
		assertTrue( grassGoneAt > 0 && grassGoneAt < waterGoneAt );
	}

	@Test
	public void noMoreLeavesThanTheCapAndNoMoreFirefliesThanTheirs(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		int leaves = LeafParticle.alive();
		for (int i = 0; i < 40; i++){
			fullBudget();
			for (int k = 0; k < 10; k++) LeafParticle.GENERAL.emit( e, k, 30, 30 );
		}
		assertTrue( "leaves " + LeafParticle.alive(), LeafParticle.alive() <= LeafParticle.CAP );
		assertTrue( LeafParticle.alive() > leaves );
		for (Gizmo g : e.membersView()) if (g != null) g.kill();
		assertEquals( leaves, LeafParticle.alive() );

		int flies = FireflyParticle.alive();
		Emitter caller = new Emitter();
		for (int i = 0; i < 60; i++) FireflyParticle.FACTORY.emit( caller, i, 40, 40 );
		assertTrue( "fireflies " + FireflyParticle.alive(), FireflyParticle.alive() <= FireflyParticle.CAP );
		//sent to the light layer whoever asked
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		assertTrue( living( lights, FireflyParticle.class ) > 0 );
		assertEquals( 0, living( caller, FireflyParticle.class ) );
		for (Gizmo g : lights.membersView()){
			if (g instanceof Group) for (Gizmo m : ((Group) g).membersView()) if (m instanceof FireflyParticle) m.kill();
		}
		assertEquals( flies, FireflyParticle.alive() );
	}

	@Test
	public void noMoreShaftsThanTheCapAndAGoneScenesParticlesAreNoLongerCounted(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.LIGHTS.ordinal()].add( e );
		int shafts = ShaftParticle.alive(), leaves = LeafParticle.alive(), flies = FireflyParticle.alive();
		for (int i = 0; i < 30; i++){
			fullBudget();
			for (int k = 0; k < 4; k++){
				ShaftParticle.FACTORY.emit( e, k, 30, 30 );
				SunlightParticle.FACTORY.emit( e, k, 30, 30 );
			}
		}
		assertTrue( "shafts " + ShaftParticle.alive(), ShaftParticle.alive() > shafts && ShaftParticle.alive() <= ShaftParticle.CAP );
		fullBudget();
		for (int k = 0; k < 10; k++) LeafParticle.GENERAL.emit( e, k, 30, 30 );
		for (int k = 0; k < 5; k++) FireflyParticle.FACTORY.emit( e, k, 30, 30 );
		assertTrue( LeafParticle.alive() > leaves );
		assertTrue( FireflyParticle.alive() > flies );

		//the scene goes (destroyed, never killed): its particles let go of their places
		for (Group g : layers) g.destroy();
		assertEquals( shafts, ShaftParticle.alive() );
		assertEquals( leaves, LeafParticle.alive() );
		assertEquals( flies, FireflyParticle.alive() );
	}

	// ------------------------------------------------------------------ shafts

	@Test
	public void ambientShaftsLeaveRoomForACastsAndAWallsShafts(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.LIGHTS.ordinal()].add( e );
		int start = ShaftParticle.alive();
		//a big patch of sunlight and foliage: as many shafts as they may have
		for (int i = 0; i < 400; i++){
			fullBudget();
			SunlightParticle.FACTORY.emit( e, i, 30, 30 );
			if (ShaftParticle.allowed( FxBudget.P2, 30, 30 )){
				((ShaftParticle) e.recycle( ShaftParticle.class )).shaft( 30, 30, FxFrames.Light.SHAFT_32, Nature.FLECK, 0.35f, 9f );
			}
		}
		assertEquals( "ambient shafts stop short of the cap", Math.max( start, ShaftParticle.AMBIENT ), ShaftParticle.alive() );
		assertFalse( ShaftParticle.allowed( FxBudget.P2, 30, 30 ) );
		//a cast's shafts and a wall's still stand, up to the cap
		fullBudget();
		assertTrue( ShaftParticle.allowed( FxBudget.P1, 30, 30 ) );
		for (int i = 0; i < 100; i++){
			fullBudget();
			ShaftParticle.FACTORY.emit( e, i, 30, 30 );
		}
		assertEquals( ShaftParticle.CAP, ShaftParticle.alive() );
		for (Group g : layers) g.destroy();
		assertEquals( start, ShaftParticle.alive() );
	}

	@Test
	public void aShaftIsAPaintedFrameNeverScaledHiddenWhileItWaits(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.LIGHTS.ordinal()].add( e );
		ShaftParticle s = (ShaftParticle) e.recycle( ShaftParticle.class );
		s.shaft( 40, 40, FxFrames.Light.SHAFT_32, Nature.HOLY, ShaftParticle.PEAK, ShaftParticle.LIFE ).delay( 0.3f );
		Game.elapsed = 1 / 30f;
		float peak = 0;
		int motes = 0;
		for (int i = 0; i < 60 && s.alive; i++){
			boolean waiting = s.waiting();
			e.update();
			if (waiting && s.waiting()) assertFalse( "hidden while it waits", s.visible );
			assertEquals( 1f, s.scale.x, 0f );
			assertEquals( 1f, s.scale.y, 0f );
			assertEquals( 7f, s.width, 0f );
			assertEquals( 32f, s.height, 0f );
			if (s.visible) peak = Math.max( peak, s.am );
			motes = Math.max( motes, living( e, FxParticle.class ) - 1 );
		}
		assertTrue( "up to its peak and no more: " + peak, peak > 0.3f && peak <= ShaftParticle.PEAK + 0.001f );
		//its motes rose with it, in the same light layer
		assertTrue( "motes " + motes, motes >= 1 );
	}

	@Test
	public void theSunlightIsOneColourNoLongerARandomOne(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.LIGHTS.ordinal()].add( e );
		for (int i = 0; i < 6; i++){
			SunlightParticle s = (SunlightParticle) e.recycle( SunlightParticle.class );
			s.reset( 20 + i, 20 );
			assertEquals( SunlightParticle.COLOR, s.colorNow() );
			assertEquals( 40f, s.height, 0f );
			s.kill();
		}
	}

	// ------------------------------------------------------------------ rules

	@Test
	public void theAreasParticlesDrawNoGameRandom(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		e.pos( 40, 40, 16, 16 );
		Emitter.Factory[] all = { LeafParticle.GENERAL, LeafParticle.LEVEL_SPECIFIC, ShaftParticle.FACTORY, ShaftParticle.HEAL,
				SunlightParticle.FACTORY, LightParticle.FACTORY, FireflyParticle.FACTORY, HolyFx.SPIRIT_WISP, HolyFx.LANCE_TRAIL,
				HolyFx.AURA, BlobFx.FOLIAGE, BlobFx.SUN, BlobFx.WALL, BlobFx.WRATH, BlobFx.HALLOWED, BlobFx.growth( false ),
				BlobFx.growth( true ), PlantFx.SUNGRASS_SHAFT };
		assertNoGameRandom( () -> {
			for (Emitter.Factory f : all){
				fullBudget();
				e.burst( f, 4 );
				frames( 40 );
			}
			PlantFx.sprung( 3 + 6 * W );
			PlantFx.bark( 3 + 6 * W );
			PlantFx.blind( 3 + 6 * W );
			PlantFx.fade( 3 + 6 * W, 5 + 6 * W );
			Trample.at( 3 + W );
			HealFx.heal( 40, 30, 2 + 2 * W, 20 );
			HealFx.shield( 40, 30, 2 + 2 * W );
			SeasonSweep.sweep( 2 + 2 * W, new int[]{ 3 + W, 5 + W }, 0xFFC8DC );
			HolyFx.sparkles( new int[]{ 1, 2, 3 }, 3 );
			frames( 60 );
		} );
	}

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		//the weather's sheet (the firefly's and half the leaves') painted first, as the game has
		WeatherSprites.get();
		failOnGl();
		FxEmitter e = new FxEmitter();
		assertNotNull( ((LeafParticle) e.recycle( LeafParticle.class )).leaf( 10, 10, 0x4E9A30 ) );
		assertNotNull( ((ShaftParticle) e.recycle( ShaftParticle.class )).reset( 10, 10 ) );
		assertNotNull( ((SunlightParticle) e.recycle( SunlightParticle.class )).reset( 10, 10 ) );
		assertNotNull( ((LightParticle) e.recycle( LightParticle.class )).reset( 10, 10 ) );
		assertNotNull( ((FireflyParticle) e.recycle( FireflyParticle.class )) );
		assertNotNull( Nature.lit( e, 10, 10 ).look( FxFrames.Light.POOL_15 ) );
		assertNotNull( ((EllipseMote) e.recycle( EllipseMote.class )).round( 10, 10, 5, 2, 0, 1 ) );
		assertNotNull( ((Sprout) e.recycle( Sprout.class )).sprout( 10, 10, 0x4E9A30, Sprout.LIFE ) );
		HealFx.heal( 40, 30, 2 + 2 * W, 20 );
		HealFx.shield( 40, 30, 2 + 2 * W );
		Trample.at( 3 + W );
		GrowthPulse.changed( 5 + 2 * W, Terrain.EMPTY, Terrain.GRASS );
		frames( 3 );
		e.update();
	}

	@Test
	public void healingMotesGrowWithTheAmountToSixAndNoMore(){
		assertEquals( 2, HealFx.motes( 0 ) );
		assertEquals( 3, HealFx.motes( 5 ) );
		assertEquals( 6, HealFx.motes( 20 ) );
		assertEquals( 6, HealFx.motes( 500 ) );
		assertEquals( 12, HealFx.amount( "+12" ) );
		assertEquals( 7, HealFx.amount( "7 HP" ) );
		assertEquals( 0, HealFx.amount( "full" ) );
	}

	@Test
	public void aHaloPutOutBeforeItsLightCameNeverLightsAndTheLastOneGoesOut() throws Exception {
		Group stage = new Group();
		final CharSprite s = new RatSprite();
		stage.add( s );
		final java.util.List<Runnable> posted = new java.util.ArrayList<>();
		Application savedApp = Gdx.app;
		final Application app = savedApp;
		//the render thread's queue, run when the test says
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				(p, m, a) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) a[0] );
						}
						return null;
					}
					return m.invoke( app, a );
				} );
		int before = FxLight.livePersistent();
		try {
			//the avatar's buff attached and set on the actor thread: lit, put out, lit again
			final HolyFx.Halo[] last = new HolyFx.Halo[1];
			Thread actor = new Thread( () -> {
				HolyFx.Halo first = HolyFx.halo( s, 1 );
				first.out();
				last[0] = HolyFx.halo( s, 1 );
			} );
			actor.start();
			actor.join();
			synchronized (posted){
				for (Runnable r : posted) r.run();
				posted.clear();
			}
			assertEquals( "the last halo's glow and pool alone", before + 2, FxLight.livePersistent() );
			last[0].out();
			frames( 30 );
			assertEquals( before, FxLight.livePersistent() );
		} finally {
			Gdx.app = savedApp;
		}
	}

	@Test
	public void theWellsAndTheHaloDoNotAllocateLightsPastTheirShare(){
		int before = FxLight.live();
		HealFx.drink( Dungeon.hero, 2 + 2 * W );
		//off a scene the hero has no sprite: nothing, and no error
		assertEquals( before, FxLight.live() );
		HolyFx.Halo h = HolyFx.halo( null, 2 );
		assertNotNull( h );
		h.out();
		assertEquals( before, FxLight.live() );
		assertTrue( Element.HOLY.light != 0 );
	}
}
