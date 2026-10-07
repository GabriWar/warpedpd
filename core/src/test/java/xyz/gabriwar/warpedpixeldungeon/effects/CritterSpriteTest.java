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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The critters' own motion, headless (no GL is touched until a sprite is drawn): a scared
 * bird is up and gone inside two seconds, a hare runs its dash and is gone, a butterfly keeps
 * to its patch, and - the rebase contract - a picture slid with the effects group stays
 * exactly where the slide put it, however many times follow() is called before its update.
 */
public class CritterSpriteTest {

	private static final float DT = 1f / 60f;
	private float elapsed, wind;
	private boolean freeze;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		elapsed = Game.elapsed;
		wind = ClimateManager.debugWindOverride;
		freeze = Emitter.freezeEmitters;
		ClimateManager.debugWindOverride = 0f;
		Emitter.freezeEmitters = false;
	}

	@After
	public void restore(){
		Game.elapsed = elapsed;
		ClimateManager.debugWindOverride = wind;
		Emitter.freezeEmitters = freeze;
	}

	private static void frame( CritterSprite c, float dt ){
		Game.elapsed = dt;
		c.update();
	}

	//frames until it is gone (or `limit` seconds), the seconds it took
	private static float runOut( CritterSprite c, float limit ){
		float t = 0f;
		while (c.exists && t < limit){
			frame( c, DT );
			t += DT;
		}
		return t;
	}

	@Test
	public void aScaredBirdClimbsAwayAndIsGoneInUnderTwoSeconds(){
		for (CritterSprite.Bird.Kind k : new CritterSprite.Bird.Kind[]{
				CritterSprite.Bird.Kind.CROW, CritterSprite.Bird.Kind.GULL, CritterSprite.Bird.Kind.FINCH }){
			CritterSprite.Bird b = new CritterSprite.Bird( k, 200f, 200f, 0f );
			for (int i = 0; i < 30; i++) frame( b, DT );
			assertTrue( b.grounded() );
			//east, after a 0.08s stagger
			b.takeOff( 0f, 0.08f );
			assertFalse( b.grounded() );
			float lastZ = -1f, lastX = b.gx();
			float t = 0f;
			while (b.exists && t < 5f){
				frame( b, DT );
				t += DT;
				if (!b.exists) break;
				assertTrue( k + ": climbs", b.z >= lastZ );
				assertTrue( k + ": never turns back", b.gx() >= lastX - 0.001f );
				lastZ = b.z;
				lastX = b.gx();
			}
			assertFalse( k + " still about after " + t + "s", b.exists );
			assertTrue( k + " gone too soon: " + t + "s", t >= 1.5f );
			assertTrue( k + " gone too late: " + t + "s", t <= 0.08f + 1.8f + 2 * DT );
			//it went the way it was sent, well away from where it stood
			assertTrue( k + " flew " + (lastX - 200f) + "px", lastX - 200f > 80f );
		}
	}

	@Test
	public void theOwlLeavesWithinTwoSeconds(){
		CritterSprite.Bird owl = new CritterSprite.Bird( CritterSprite.Bird.Kind.OWL, 200f, 200f, 13f );
		owl.land( 1f, 0f );
		runOut( owl, 2f );
		assertTrue( owl.exists );
		assertTrue( owl.grounded() );
		assertFalse( owl.landing() );
		owl.takeOff( 3f, 0f );
		float t = runOut( owl, 5f );
		assertFalse( owl.exists );
		assertTrue( "owl took " + t + "s", t <= 2f + 2 * DT );
	}

	@Test
	public void aBirdNotYetInSightNeverArrives(){
		CritterSprite.Bird b = new CritterSprite.Bird( CritterSprite.Bird.Kind.CROW, 200f, 200f, 0f );
		b.land( 0f, 0.5f );
		frame( b, DT );
		assertEquals( 0f, b.shown, 0f );
		b.takeOff( 2f, 0f );
		assertFalse( b.exists );
	}

	@Test
	public void aBirdScaredWhileLandingKeepsItsPlaceAndAlpha(){
		CritterSprite.Bird b = new CritterSprite.Bird( CritterSprite.Bird.Kind.CROW, 200f, 200f, 0f );
		b.land( 0f, 0f );
		for (int i = 0; i < 9; i++) frame( b, DT );
		assertTrue( b.landing() );
		float x = b.x, y = b.y, a = b.alpha();
		assertTrue( a > 0f && a < 1f );
		b.takeOff( 2f, 0f );
		frame( b, DT );
		//one frame on: no jump, no flash to full alpha
		assertTrue( Math.abs( b.x - x ) < 4f && Math.abs( b.y - y ) < 4f );
		assertTrue( b.alpha() <= a + 0.001f );
	}

	//what GameScene.shiftWorldVisuals does to every member of the effects group
	private static void slide( CritterSprite c, float sx, float sy ){
		c.x += sx;
		c.y += sy;
	}

	private static void assertStaysWhereSlid( String what, CritterSprite c ){
		for (int i = 0; i < 20; i++) frame( c, DT );
		assertTrue( what, c.exists );
		float x = c.x, y = c.y, ax = c.ax, ay = c.ay;
		slide( c, -512f, 512f );
		//the Field carries the slide first, then the sprite's own update does it again
		c.follow();
		c.follow();
		frame( c, 0f );
		assertEquals( what + " x", x - 512f, c.x, 0.001f );
		assertEquals( what + " y", y + 512f, c.y, 0.001f );
		assertEquals( what + " ax", ax - 512f, c.ax, 0.001f );
		assertEquals( what + " ay", ay + 512f, c.ay, 0.001f );
		//and once more without the Field: the sprite's update alone catches it
		slide( c, 512f, 0f );
		frame( c, 0f );
		assertEquals( what + " x again", x, c.x, 0.001f );
		assertEquals( what + " ax again", ax, c.ax, 0.001f );
	}

	@Test
	public void aRebaseSlideNeverMovesACritter(){
		assertStaysWhereSlid( "perched crow", new CritterSprite.Bird( CritterSprite.Bird.Kind.CROW, 300f, 300f, 0f ) );
		assertStaysWhereSlid( "sitting hare", new CritterSprite.Hare( false, 300f, 300f ) );
		assertStaysWhereSlid( "butterfly", new CritterSprite.Butterfly( 300f, 300f ) );

		//in flight: the slide is carried and the flight goes on from where it was put
		CritterSprite.Bird b = new CritterSprite.Bird( CritterSprite.Bird.Kind.GULL, 300f, 300f, 0f );
		frame( b, DT );
		b.takeOff( 0f, 0f );
		for (int i = 0; i < 20; i++) frame( b, DT );
		float x = b.x, y = b.y;
		slide( b, -512f, 0f );
		b.follow();
		frame( b, DT );
		assertTrue( "flying gull jumped " + (b.x - (x - 512f)), Math.abs( b.x - (x - 512f) ) < 4f );
		assertTrue( Math.abs( b.y - y ) < 4f );
	}

	@Test
	public void aNetworkMirrorsHandMoveIsNotReadAsASlide(){
		CritterSprite.Hare h = new CritterSprite.Hare( false, 300f, 300f );
		for (int i = 0; i < 5; i++) frame( h, DT );
		float x = h.x;
		//the Field moves the ground point by hand and forgets the drawn marks
		h.ax -= 512f;
		h.resync();
		frame( h, 0f );
		assertEquals( x - 512f, h.x, 0.001f );
	}

	@Test
	public void aHareRunsItsDashAndIsGone(){
		CritterSprite.Hare h = new CritterSprite.Hare( false, 300f, 300f );
		for (int i = 0; i < 60; i++) frame( h, DT );
		assertTrue( h.sitting() );
		assertEquals( 1f, h.shown, 0.001f );
		float[] route = { 16f, 0f, 32f, 10f, 64f, 0f };
		h.bolt( route, true );
		assertFalse( h.sitting() );
		float t = 0f, lastX = h.gx();
		float minAlpha = 1f;
		while (h.exists && t < 3f){
			frame( h, DT );
			t += DT;
			if (!h.exists) break;
			assertTrue( "runs on, never back", h.gx() >= lastX - 0.001f );
			lastX = h.gx();
			minAlpha = Math.min( minAlpha, h.alpha() );
		}
		assertFalse( h.exists );
		float len = 16f + (float)Math.hypot( 16, 10 ) + (float)Math.hypot( 32, 10 );
		//a startled beat, then 120 px a second
		assertTrue( "took " + t + "s", t <= 0.12f + len / 120f + 3 * DT );
		assertTrue( "reached " + (lastX - 300f), lastX - 300f > 55f );
		//faded into the cover rather than blinking out
		assertTrue( minAlpha < 0.25f );
	}

	@Test
	public void aButterflyKeepsToItsPatchAndLeaves(){
		for (int n = 0; n < 20; n++){
			CritterSprite.Butterfly b = new CritterSprite.Butterfly( 300f, 300f );
			float t = 0f, far = 0f;
			while (b.exists && t < 60f){
				frame( b, DT );
				t += DT;
				if (!b.exists) break;
				far = Math.max( far, (float)Math.hypot( b.gx() - 300f, b.gy() - 300f ) );
				assertTrue( "height " + b.z, b.z >= 2f && b.z <= 19f );
			}
			assertFalse( "still about after " + t + "s", b.exists );
			assertTrue( "gone after " + t + "s", t >= 18f && t <= 36f + 2 * DT );
			assertTrue( "strayed " + far + "px from its patch", far <= 64f );
		}
	}

	@Test
	public void aStartledButterflyDartsAway(){
		for (int n = 0; n < 20; n++){
			CritterSprite.Butterfly b = new CritterSprite.Butterfly( 300f, 300f );
			for (int i = 0; i < 120; i++) frame( b, DT );
			//someone brushes past four pixels west of it
			float sx = b.gx() - 4f, sy = b.gy();
			b.startle( sx, sy );
			float d0 = (float)Math.hypot( b.gx() - sx, b.gy() - sy );
			for (int i = 0; i < 60; i++) frame( b, DT );
			float d1 = (float)Math.hypot( b.gx() - sx, b.gy() - sy );
			assertTrue( "only " + (d1 - d0) + "px further off", d1 - d0 > 15f );
		}
	}
}
