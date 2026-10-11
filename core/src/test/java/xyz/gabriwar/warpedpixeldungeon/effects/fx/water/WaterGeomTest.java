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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The live water's pure geometry and timing (WaterGeom). */
public class WaterGeomTest {

	@Test
	public void theWaterBoxHoldsTheWaterInViewAndNothingElse(){
		int w = 10, h = 8;
		boolean[] water = new boolean[w * h];
		water[3 + 2 * w] = water[6 + 5 * w] = water[8 + 1 * w] = true;
		int[] box = new int[4];
		assertTrue( WaterGeom.waterBox( water, w, h, 0, 0, 9, 7, box ) );
		assertArrayEquals( new int[]{ 3, 1, 8, 5 }, box );
		//a view that leaves the far cell out
		assertTrue( WaterGeom.waterBox( water, w, h, 0, 2, 7, 7, box ) );
		assertArrayEquals( new int[]{ 3, 2, 6, 5 }, box );
		//a view past the map's edges is cut to it
		assertTrue( WaterGeom.waterBox( water, w, h, -5, -5, 40, 40, box ) );
		assertArrayEquals( new int[]{ 3, 1, 8, 5 }, box );
		//none in view: false, the box untouched
		assertFalse( WaterGeom.waterBox( water, w, h, 0, 6, 9, 7, box ) );
		assertArrayEquals( new int[]{ 3, 1, 8, 5 }, box );
		assertFalse( WaterGeom.waterBox( null, w, h, 0, 0, 9, 7, box ) );
	}

	@Test
	public void anAnchoredTextureShowsTheSameTexelAtAWorldPointWhereverItIsCut(){
		for (float scroll : new float[]{ 0f, 13.5f, -70f }){
			for (float x : new float[]{ 0f, 48f, 512f, -16f }){
				float start = WaterGeom.anchor( x, scroll, 64 );
				assertTrue( start >= 0 && start < 64 );
				float texel = (start + (300 - x)) % 64;
				assertEquals( ((300 + scroll) % 64 + 64) % 64, (texel + 64) % 64, 1e-3f );
			}
		}
		//a slide of the overworld's window (512 px) changes nothing: 64 divides it
		assertEquals( WaterGeom.anchor( 96, 5, 64 ), WaterGeom.anchor( 96 + 512, 5, 64 ), 1e-4f );
	}

	@Test
	public void theCycleStepsAtItsPaceAndWrapsAfterEight(){
		assertEquals( 0, WaterGeom.cycleFrame( 0f, 6 ) );
		assertEquals( 0, WaterGeom.cycleFrame( 0.16f, 6 ) );
		assertEquals( 1, WaterGeom.cycleFrame( 0.17f, 6 ) );
		assertEquals( 7, WaterGeom.cycleFrame( 1.33f, 6 ) );
		assertEquals( 0, WaterGeom.cycleFrame( 1.34f, 6 ) );
		assertEquals( "the ice keeps still", 0, WaterGeom.cycleFrame( 5f, 0 ) );
	}

	@Test
	public void aSheenDimsWithCloudsUndergroundAndAtNightAndBreathesWithin12Percent(){
		float max = 0, min = 10;
		for (float t = 0; t < 8; t += 0.05f){
			float s = WaterGeom.sheenShare( true, 0f, 0f, t, 0f );
			max = Math.max( max, s );
			min = Math.min( min, s );
		}
		assertEquals( 1.12f, max, 0.01f );
		assertEquals( 0.88f, min, 0.01f );
		//a quarter of the 0.25 Hz breath: one second
		assertEquals( 1.12f, WaterGeom.sheenShare( true, 0f, 0f, 1f, 0f ), 1e-3f );
		//overcast: half; underground: 0.8; night: 0.6 of that
		assertEquals( 0.5f, WaterGeom.sheenShare( true, 0.9f, 0f, 0f, 0f ), 1e-3f );
		assertEquals( 0.8f, WaterGeom.sheenShare( false, 0f, 0f, 0f, 0f ), 1e-3f );
		assertEquals( 0.48f, WaterGeom.sheenShare( false, 0f, 0.54f, 0f, 0f ), 1e-3f );
		assertEquals( 1f, WaterGeom.night( 0.6f ), 0 );
		assertEquals( 0f, WaterGeom.night( 0f ), 0 );
	}

	@Test
	public void theRainBreaksTheShineAndItsMarksShowAtNight(){
		//a light rain leaves the sheens be; a downpour leaves 0.3 of them, easing down between
		assertEquals( 1f, WaterGeom.rainDim( 0f ), 0 );
		assertEquals( 1f, WaterGeom.rainDim( 0.2f ), 0 );
		assertEquals( 0.3f, WaterGeom.rainDim( 1f ), 1e-4f );
		float last = 1f;
		for (float r = 0f; r <= 1f; r += 0.05f){
			assertTrue( WaterGeom.rainDim( r ) <= last + 1e-6f );
			last = WaterGeom.rainDim( r );
		}
		//the pocks: 0.15 from 0.4 of rain, 0.45 in a downpour
		assertEquals( 0f, WaterGeom.pockAlpha( 0.39f ), 0 );
		assertEquals( 0.15f, WaterGeom.pockAlpha( 0.4f ), 1e-4f );
		assertEquals( 0.45f, WaterGeom.pockAlpha( 1f ), 1e-4f );
		//lifted at night, not by day
		assertEquals( 1f, WaterGeom.nightLift( 0f, 0.4f ), 0 );
		assertEquals( 1.4f, WaterGeom.nightLift( 0.54f, 0.4f ), 1e-4f );
	}
}
