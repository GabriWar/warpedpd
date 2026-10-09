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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.utils.Random;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The kit's pure parts: its random numbers never touch the game's, its curves and ramps have the
 * shapes the effects' language gives them, and every element and liquid has its palette.
 */
public class FxCoreTest {

	// ------------------------------------------------------------------ FxRandom

	@Test
	public void theEffectsRandomNumbersNeverMoveTheGames(){
		//the game's seeded generator, drawn from once as it is...
		Random.pushGenerator( 4242 );
		float a = Random.Float();
		int b = Random.Int( 1000 );
		Random.popGenerator();
		//...and again with a thousand effect draws of every kind between
		Random.pushGenerator( 4242 );
		for (int i = 0; i < 1000; i++){
			FxRandom.Float();
			FxRandom.Float( 3f );
			FxRandom.Float( -2f, 5f );
			FxRandom.NormalFloat( 0, 1 );
			FxRandom.Int( 7 );
			FxRandom.IntRange( 2, 4 );
			FxRandom.chance( 0.5f );
			FxRandom.sign();
			FxRandom.angle();
			FxRandom.element( new int[]{ 1, 2, 3 } );
			FxRandom.element( new String[]{ "a", "b" } );
		}
		assertEquals( a, Random.Float(), 0f );
		assertEquals( b, Random.Int( 1000 ) );
		Random.popGenerator();
	}

	@Test
	public void itsNumbersFallWhereTheyShould(){
		FxRandom.seedForTests( 7 );
		HashSet<Integer> seen = new HashSet<>();
		float sum = 0;
		int n = 20000, ups = 0;
		for (int i = 0; i < n; i++){
			float f = FxRandom.Float( -2f, 5f );
			assertTrue( f >= -2f && f < 5f );
			int r = FxRandom.IntRange( 2, 4 );
			assertTrue( r >= 2 && r <= 4 );
			seen.add( r );
			assertEquals( 0, FxRandom.Int( 0 ) );
			sum += FxRandom.NormalFloat( 0, 1 );
			float s = FxRandom.sign();
			assertTrue( s == 1f || s == -1f );
			if (s > 0) ups++;
			float t = FxRandom.angle();
			assertTrue( t >= 0 && t < 2 * Math.PI + 1e-4 );
		}
		assertEquals( new HashSet<>( Arrays.asList( 2, 3, 4 ) ), seen );
		assertEquals( 0.5f, sum / n, 0.02f );
		assertEquals( 0.5f, ups / (float)n, 0.03f );
		//seeded for a test, a thread sees the same numbers again
		FxRandom.seedForTests( 99 );
		float x = FxRandom.Float();
		FxRandom.seedForTests( 99 );
		assertEquals( x, FxRandom.Float(), 0f );
	}

	// ------------------------------------------------------------------ FxCurves

	@Test
	public void theCurvesHaveTheirShapes(){
		for (float t = 0; t <= 1.001f; t += 0.05f){
			assertTrue( FxCurves.easeOut( t ) >= t - 1e-6 );
			assertTrue( FxCurves.easeOut3( t ) >= FxCurves.easeOut( t ) - 1e-6 );
			assertTrue( FxCurves.easeIn( t ) <= t + 1e-6 );
		}
		assertEquals( 0f, FxCurves.easeOut( 0 ), 0 );
		assertEquals( 1f, FxCurves.easeOut( 1 ), 0 );
		assertEquals( 1f, FxCurves.easeOut3( 2 ), 0 );
		assertEquals( 0.5f, FxCurves.smooth( 0.5f ), 1e-6 );
		assertEquals( 0f, FxCurves.tri( 0 ), 0 );
		assertEquals( 1f, FxCurves.tri( 0.5f ), 1e-6 );
		assertEquals( 0f, FxCurves.tri( 1 ), 1e-6 );
		//the kit's pop overshoots by a twentieth or so and settles on 1
		float peak = 0;
		for (float t = 0; t <= 1f; t += 0.01f) peak = Math.max( peak, FxCurves.backOut( t, 1.15f ) );
		assertTrue( "peak " + peak, peak > 1.03f && peak < 1.08f );
		assertEquals( 1f, FxCurves.backOut( 1, 1.15f ), 1e-6 );
		//up over the attack, down over the release
		assertEquals( 0.5f, FxCurves.attackRelease( 0.05f, 0.1f, 0.4f ), 1e-6 );
		assertEquals( 1f, FxCurves.attackRelease( 0.1f, 0.1f, 0.4f ), 1e-6 );
		assertEquals( 0.5f, FxCurves.attackRelease( 0.3f, 0.1f, 0.4f ), 1e-6 );
		assertEquals( 0f, FxCurves.attackRelease( 0.6f, 0.1f, 0.4f ), 0 );
		//painted steps
		assertEquals( 0f, FxCurves.step( 0.24f, 4 ), 0 );
		assertEquals( 0.25f, FxCurves.step( 0.26f, 4 ), 0 );
		assertEquals( 1f, FxCurves.step( 1f, 4 ), 0 );
	}

	@Test
	public void aKeyedTableIsTheLightningsFlicker(){
		float[] keys = { 0f, 1f, 0.04f, 0.85f, 0.08f, 0.25f, 0.3f, 0f };
		assertEquals( 1f, FxCurves.keyed( keys, -1f ), 0 );
		assertEquals( 1f, FxCurves.keyed( keys, 0f ), 0 );
		assertEquals( 0.925f, FxCurves.keyed( keys, 0.02f ), 1e-5 );
		assertEquals( 0.25f, FxCurves.keyed( keys, 0.08f ), 1e-6 );
		assertEquals( 0f, FxCurves.keyed( keys, 0.5f ), 0 );
	}

	@Test
	public void theHearthAndTheNoiseStayInTheirBounds(){
		float lo = 1, hi = -1;
		for (float t = 0; t < 60; t += 0.013f){
			float h = FxCurves.hearth( t, 0.3f, 1.1f, 2.4f );
			assertTrue( Math.abs( h ) <= 0.075f + 1e-6 );
			float n = FxCurves.noise1( 17, t );
			assertTrue( n >= -1.0001f && n <= 1.0001f );
			lo = Math.min( lo, n );
			hi = Math.max( hi, n );
			//smooth: no jump between near times
			assertTrue( Math.abs( FxCurves.noise1( 17, t + 0.001f ) - n ) < 0.01f );
		}
		assertTrue( "it wanders", hi - lo > 1f );
		assertEquals( FxCurves.noise1( 3, 4.5f ), FxCurves.noise1( 3, 4.5f ), 0 );
		assertNotEquals( FxCurves.noise1( 3, 4.5f ), FxCurves.noise1( 4, 4.5f ), 0 );
	}

	// ------------------------------------------------------------------ Ramp

	@Test
	public void lightFlowsAlongItsRampAndMatterSteps(){
		Ramp r = new Ramp( 0xFFFFFF, 0x808080, 0x000000 );
		assertEquals( 0xFFFFFF, r.at( 0 ) );
		assertEquals( 0x000000, r.at( 1 ) );
		assertEquals( 0x808080, r.at( 0.5f ) );
		assertEquals( Ramp.lerp( 0xFFFFFF, 0x808080, 0.5f ), r.at( 0.25f ) );
		//matter keeps to its stops
		assertEquals( 0xFFFFFF, r.step( 0.2f ) );
		assertEquals( 0x808080, r.step( 0.5f ) );
		assertEquals( 0x000000, r.step( 0.9f ) );
		assertEquals( 3, r.size() );
		assertEquals( 0x808080, r.stop( 1 ) );
		//a flame holds its first stops longer
		Ramp fire = Element.FIRE.ramp.keyed( 0, .18f, .45f, .7f, 1 );
		assertEquals( 5, fire.size() );
		assertEquals( Element.FIRE.ramp.stop( 1 ), fire.at( 0.18f ) );
		assertEquals( Element.FIRE.ramp.stop( 4 ), fire.at( 1f ) );
		assertEquals( Element.FIRE.ramp.stop( 2 ), fire.step( 0.5f ) );
		assertEquals( 0x102030, Ramp.lerp( 0x102030, 0xFFFFFF, 0 ) );
	}

	@Test
	public void everyElementAndLiquidHasItsPalette(){
		for (Element e : Element.values()){
			assertTrue( e.name(), e.ramp.size() >= 4 );
			assertNotNull( e.ramp );
		}
		assertTrue( Element.FIRE.emissive && !Element.SMOKE.emissive && !Element.SHADOW.emissive );
		assertTrue( Element.ELECTRIC.emissive && !Element.FROST.emissive );
		assertEquals( 0xFF9A48, Element.FIRE.light );
		assertEquals( 7, Element.PRISM.ramp.size() );
		assertEquals( null, Element.MAGIC_FIRE.smoke );

		assertEquals( Element.Liquid.SEWER, Element.Liquid.of( Assets.Environment.WATER_SEWERS ) );
		assertEquals( Element.Liquid.PRISON, Element.Liquid.of( Assets.Environment.WATER_PRISON ) );
		assertEquals( Element.Liquid.CAVE, Element.Liquid.of( Assets.Environment.WATER_CAVES ) );
		assertEquals( Element.Liquid.CITY, Element.Liquid.of( Assets.Environment.WATER_CITY ) );
		assertEquals( Element.Liquid.LAVA, Element.Liquid.of( Assets.Environment.WATER_HALLS ) );
		assertEquals( Element.Liquid.FROZEN, Element.Liquid.of( Assets.Environment.WATER_FROZEN ) );
		assertEquals( Element.Liquid.TEMPLE, Element.Liquid.of( Assets.Environment.WATER_TEMPLE ) );
		assertEquals( Element.Liquid.SEWER, Element.Liquid.of( null ) );
		assertEquals( Element.Liquid.SEWER, Element.Liquid.of( "environment/elsewhere.png" ) );
		//only lava's rings are light; only it scrolls at 2, the city's oil at 3.5
		for (Element.Liquid l : Element.Liquid.values()){
			assertEquals( l.name(), l == Element.Liquid.LAVA, l.ringAdditive );
		}
		assertEquals( 2f, Element.Liquid.LAVA.scroll, 0 );
		assertEquals( 3.5f, Element.Liquid.CITY.scroll, 0 );
		assertEquals( 0, Element.Liquid.FROZEN.cycleFps );
		assertEquals( Element.NONE, Element.Liquid.LAVA.deep );
		assertFalse( Element.Liquid.CITY.sheenTintA == Element.NONE );
	}
}
