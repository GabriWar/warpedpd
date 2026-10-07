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
 * The slices' critters drawn the right way round, headless: the circling eagle banks into
 * the turn it is flying (its foreshortened wing on the inside of the circle), and a marmot
 * dives head first toward the rock its burrow is under, whichever way it was looking.
 */
public class SliceCritterSpriteTest {

	private static final float DT = 1f / 60f;
	private float elapsed;
	private boolean freeze;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		elapsed = Game.elapsed;
		freeze = Emitter.freezeEmitters;
		Emitter.freezeEmitters = false;
	}

	@After
	public void restore(){
		Game.elapsed = elapsed;
		Emitter.freezeEmitters = freeze;
	}

	private static void frame( CritterSprite c ){
		Game.elapsed = DT;
		c.update();
	}

	@Test
	public void theEagleBanksIntoItsTurn(){
		int right = 0, left = 0;
		for (int i = 0; i < 40; i++){
			SliceCritterSprite.Eagle e = new SliceCritterSprite.Eagle( 400f, 400f );
			frame( e );
			float x0 = e.gx(), y0 = e.gy();
			frame( e );
			float x1 = e.gx(), y1 = e.gy();
			frame( e );
			float x2 = e.gx(), y2 = e.gy();
			//y down: a positive cross product is a clockwise circle on screen, a right turn
			float cross = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
			assertTrue( cross != 0f );
			//the bank frame is drawn turning left: a right turn is the mirrored frame
			assertEquals( "mirrored for a right turn", cross > 0f, e.flipHorizontal );
			if (cross > 0f) right++;
			else left++;
		}
		assertTrue( "both ways round", right > 0 && left > 0 );
	}

	@Test
	public void aMarmotDivesHeadFirstToItsRock(){
		for (int bx : new int[]{ 1, -1 }){
			SliceCritterSprite.Marmot m = new SliceCritterSprite.Marmot( 300f, 300f, bx, 0 );
			//it comes up looking out, away from its rock
			assertEquals( bx > 0, m.flipHorizontal );
			m.peek();
			for (int k = 0; k < 45; k++) frame( m );
			assertTrue( m.up() );
			//the hero on its open side: it turns to him, then dives
			m.alert( true, 300f - bx * 60f );
			assertEquals( bx > 0, m.flipHorizontal );
			m.dive();
			assertFalse( m.up() );
			assertEquals( "head first toward the rock at " + bx, bx < 0, m.flipHorizontal );
			float x = m.gx();
			for (int k = 0; k < 12; k++) frame( m );
			assertEquals( "and it hops that way", bx > 0, m.gx() > x );
		}
	}
}
