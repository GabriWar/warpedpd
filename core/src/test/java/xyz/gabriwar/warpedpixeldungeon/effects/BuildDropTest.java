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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A built piece settling into place (BuildDrop): its picture falls DROP px onto its spot in whole
 * pixels, easing out, never rising again; squashes a pixel or two only once it has landed, and is
 * back to its own shape before it fades; whole until its last FADE s, gone at TIME (0.18 s).
 */
public class BuildDropTest {

	private static final float STEP = 1 / 240f;

	@Test
	public void itFallsOntoItsSpotEasingOut(){
		assertEquals( Math.round( BuildDrop.DROP ), BuildDrop.lift( 0 ) );
		assertEquals( 0, BuildDrop.lift( BuildDrop.FALL ) );
		int last = BuildDrop.lift( 0 );
		for (float t = 0; t <= BuildDrop.TIME; t += STEP){
			int l = BuildDrop.lift( t );
			assertTrue( "never back up: " + t, l <= last );
			last = l;
		}
		//easing out: past half its fall in its first third
		assertTrue( BuildDrop.lift( BuildDrop.FALL / 3 ) <= BuildDrop.DROP / 2 );
		assertEquals( 0, BuildDrop.lift( BuildDrop.TIME ) );
	}

	@Test
	public void itSquashesOnlyAsItLands(){
		int deepest = 0;
		for (float t = 0; t <= BuildDrop.TIME; t += STEP){
			int k = BuildDrop.squash( t );
			if (t <= BuildDrop.FALL || t >= BuildDrop.FALL + BuildDrop.SQUASH) assertEquals( "only landed: " + t, 0, k );
			assertTrue( k >= 0 && k <= BuildDrop.DEEPEST );
			deepest = Math.max( deepest, k );
		}
		assertEquals( BuildDrop.DEEPEST, deepest );
		//its own shape again before it fades
		assertTrue( BuildDrop.FALL + BuildDrop.SQUASH <= BuildDrop.TIME - BuildDrop.FADE );
	}

	@Test
	public void itIsWholeUntilItGoes(){
		for (float t = 0; t < BuildDrop.TIME - BuildDrop.FADE; t += STEP) assertEquals( 1f, BuildDrop.fade( t ), 0f );
		assertEquals( 0f, BuildDrop.fade( BuildDrop.TIME ), 0f );
		assertEquals( 0.18f, BuildDrop.TIME, 1e-6f );
	}
}
