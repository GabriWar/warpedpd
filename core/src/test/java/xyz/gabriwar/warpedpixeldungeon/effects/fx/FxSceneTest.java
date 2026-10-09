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

import xyz.gabriwar.warpedpixeldungeon.effects.LightningFlash;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The kit's ground in the game's scene: the camera's shake as the player set it, a light's
 * strength by day and night, emitters that never draw the game's random numbers, and the storm's
 * flash refused by the gate spent unseen.
 */
public class FxSceneTest {

	private float savedElapsed;
	private LongSupplier savedClock;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedElapsed = Game.elapsed;
		savedClock = FlashGate.clock;
		Emitter.freezeEmitters = false;
	}

	@After
	public void tearDown(){
		Game.elapsed = savedElapsed;
		FlashGate.clock = savedClock;
		FlashGate.reset();
	}

	@Test
	public void theShakeFollowsTheSetting(){
		//the default setting (2) looks as a raw shake always did; off shakes nothing
		assertEquals( 3f, Fx.shakePx( 3f, 2, false ), 1e-6 );
		assertEquals( 0f, Fx.shakePx( 3f, 0, false ), 0 );
		assertEquals( 1.5f, Fx.shakePx( 3f, 1, false ), 1e-6 );
		//never past 4 px, nor 2 while a blizzard falls
		assertEquals( 4f, Fx.shakePx( 3f, 4, false ), 0 );
		assertEquals( 4f, Fx.shakePx( 20f, 2, false ), 0 );
		assertEquals( 2f, Fx.shakePx( 3f, 2, true ), 0 );
		assertEquals( 1f, Fx.shakePx( 1f, 2, true ), 1e-6 );
	}

	@Test
	public void aLightIsFullAtNightAndDimmerByDay(){
		//the star crater's rule: by day (no tint) 0.3 of what the day share leaves, full by night
		assertEquals( 0.35f + 0.65f * 0.3f, Fx.nightMul( Fx.DAY_PERSISTENT, 0f ), 1e-5 );
		assertEquals( 0.75f + 0.25f * 0.3f, Fx.nightMul( Fx.DAY_TRANSIENT, 0f ), 1e-5 );
		assertEquals( 1f, Fx.nightMul( Fx.DAY_PERSISTENT, 0.54f ), 1e-6 );
		assertEquals( 1f, Fx.nightMul( 1f, 0f ), 1e-6 );
		//it rises with the tint
		assertTrue( Fx.nightMul( 0.35f, 0.2f ) > Fx.nightMul( 0.35f, 0.1f ) );
	}

	@Test
	public void theKitsEmittersNeverDrawTheGamesRandomNumbers(){
		Random.pushGenerator( 777 );
		float expected = Random.Float();
		Random.popGenerator();

		Random.pushGenerator( 777 );
		FxEmitter e = new FxEmitter();
		e.pos( 10, 10, 16, 16 );
		final int[] made = { 0 };
		Emitter.Factory f = new Emitter.Factory(){
			@Override
			public void emit( Emitter emitter, int index, float x, float y ){
				assertTrue( x >= 10 && x < 26 && y >= 10 && y < 26 );
				made[0]++;
			}
		};
		e.burst( f, 5 );
		e.pour( f, 0.05f );
		e.start( f, 0.1f, 3 );
		Game.elapsed = 0.07f;
		for (int i = 0; i < 10; i++) e.update();
		assertTrue( made[0] > 0 );
		assertEquals( expected, Random.Float(), 0f );
		Random.popGenerator();
		//never tagged for a co-op guest
		assertEquals( -1, e.netCell );
	}

	@Test
	public void offTheGamesSceneThereIsNoLightLayer(){
		assertTrue( Fx.onRenderThread() );
		assertNull( Fx.lightCarrier( PixelParticle.class ) );
		assertNull( GameScene.lightEmitter() );
		assertNull( GameScene.carryLight( new Lit(){
			@Override public void drawLit(){}
			@Override public boolean isVisible(){ return true; }
		} ) );
		//posted work runs at once on the render thread
		final boolean[] ran = { false };
		Fx.post( () -> ran[0] = true );
		assertTrue( ran[0] );
	}

	@Test
	public void aStormFlashTheGateRefusesRunsItsCourseUnseen(){
		final double[] now = { 500 };
		FlashGate.clock = () -> (long)(now[0] * 1e9);
		FlashGate.reset();
		Group sky = new Group();
		LightningFlash first = LightningFlash.flash();
		assertTrue( first.visible );
		LightningFlash second = LightningFlash.flash();
		assertFalse( "0.7 s apart at least", second.visible );
		sky.add( first );
		sky.add( second );
		Game.elapsed = 0.1f;
		sky.update();
		assertTrue( first.exists && second.exists );
		assertFalse( second.visible );
		for (int i = 0; i < 4; i++) sky.update();
		assertFalse( "both gone after their 0.35 s", first.exists || second.exists );
		now[0] += 0.8;
		assertTrue( LightningFlash.flash().visible );
	}
}
