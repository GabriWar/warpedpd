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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The budget a phone's frame keeps (FxBudget): everything but P0 thins as the live count passes
 * 80% of SOFT, dressing first, nothing past HARD however many bursts a frame brings; the governor
 * steps its tiers down after a second of slow frames and back up after three of fast ones, each
 * tier taking lights, decals, sheens, glints and reflections away.
 */
public class FxBudgetTest {

	private float savedScale;

	@Before
	public void setUp(){
		savedScale = FxBudget.scale;
		//a phone's numbers
		FxBudget.scale = 1f;
		FxBudget.reset();
		FxBudget.resetGovernor();
		FxRandom.seedForTests( 11 );
		Dungeon.hero = null;
	}

	@After
	public void tearDown(){
		FxBudget.reset();
		FxBudget.resetGovernor();
		FxBudget.scale = savedScale;
	}

	private static void live( int n ){
		FxBudget.add( FxBudget.PARTICLES, n - FxBudget.live( FxBudget.PARTICLES ) );
	}

	@Test
	public void itThinsByLoadDressingFirst(){
		live( 0 );
		for (int p = FxBudget.P0; p <= FxBudget.P3; p++) assertEquals( 1f, FxBudget.factor( p ), 1e-6 );
		//load 1.05 of SOFT: 0.7 of the body and the life, 0.49 of the dressing
		live( Math.round( 1.05f * FxBudget.SOFT ) );
		assertEquals( 0.7f, FxBudget.factor( FxBudget.P2 ), 0.01f );
		assertEquals( 0.7f, FxBudget.factor( FxBudget.P1 ), 0.01f );
		assertEquals( 0.49f, FxBudget.factor( FxBudget.P3 ), 0.01f );
		//1.3: the body never under 0.6
		live( Math.round( 1.3f * FxBudget.SOFT ) );
		assertEquals( 0.4f, FxBudget.factor( FxBudget.P2 ), 0.01f );
		assertEquals( 0.6f, FxBudget.factor( FxBudget.P1 ), 1e-6 );
		assertEquals( 0.16f, FxBudget.factor( FxBudget.P3 ), 0.01f );
		//its floor
		live( FxBudget.HARD - 1 );
		assertEquals( 0.25f, FxBudget.factor( FxBudget.P2 ), 1e-6 );
		//past HARD only P0
		live( FxBudget.HARD );
		assertEquals( 0f, FxBudget.factor( FxBudget.P1 ), 0 );
		assertEquals( 1f, FxBudget.factor( FxBudget.P0 ), 0 );
		assertTrue( FxBudget.keep( FxBudget.P0 ) );
		assertFalse( FxBudget.keep( FxBudget.P1 ) );
		assertEquals( 0, FxBudget.allow( 10, FxBudget.P1 ) );
		assertEquals( 10, FxBudget.allow( 10, FxBudget.P0 ) );
	}

	@Test
	public void aThousandBurstsInAFrameKeepTheLiveCountUnderHard(){
		for (int frame = 0; frame < 5; frame++){
			FxBudget.frame( 1 / 60f );
			for (int i = 0; i < 1000; i++){
				int p = FxBudget.P1 + i % 3;
				int k = FxBudget.allow( 12, p );
				assertTrue( k >= 0 && k <= 12 );
				FxBudget.add( FxBudget.PARTICLES, k );
				for (int j = 0; j < 3; j++){
					if (FxBudget.keep( FxBudget.P2 )) FxBudget.add( FxBudget.PARTICLES, 1 );
				}
				assertTrue( "live " + FxBudget.live( FxBudget.PARTICLES ), FxBudget.live( FxBudget.PARTICLES ) <= FxBudget.HARD );
			}
		}
		//a burst's body keeps one at least while there is room
		live( 0 );
		FxBudget.frame( 1 / 60f );
		for (int i = 0; i < FxBudget.SPAWNS + 10; i++) FxBudget.keep( FxBudget.P0 );
		assertEquals( 1, FxBudget.allow( 1, FxBudget.P1 ) );
		assertEquals( 0, FxBudget.allow( 5, FxBudget.P3 ) );
	}

	@Test
	public void theFramesSpawnsRunOutAndComeBackNextFrame(){
		FxBudget.frame( 1 / 60f );
		int kept = 0;
		for (int i = 0; i < 500; i++) if (FxBudget.keep( FxBudget.P2 )) kept++;
		assertEquals( FxBudget.SPAWNS, kept );
		FxBudget.frame( 1 / 60f );
		assertTrue( FxBudget.keep( FxBudget.P2 ) );
	}

	@Test
	public void theGovernorStepsDownOnSlowFramesAndBackUpOnFastOnes(){
		assertEquals( FxBudget.FULL, FxBudget.tier() );
		float t = 0;
		//30 fps: about a second and a half to step down (its average has to catch up first)
		while (FxBudget.tier() == FxBudget.FULL && t < 5){
			FxBudget.frame( 1 / 30f );
			t += 1 / 30f;
		}
		assertEquals( FxBudget.REDUCED, FxBudget.tier() );
		assertTrue( "after " + t, t >= 1f && t < 2f );
		assertEquals( 16, FxBudget.lightCap() );
		assertEquals( 20, FxBudget.decalCap() );
		assertFalse( FxBudget.sheenB() );
		assertTrue( FxBudget.sheenA() );
		assertEquals( 0.5f, FxBudget.glints(), 0 );
		//a second more of it: MINIMAL, and the dressing gone
		for (int i = 0; i < 31; i++) FxBudget.frame( 1 / 30f );
		assertEquals( FxBudget.MINIMAL, FxBudget.tier() );
		assertEquals( 8, FxBudget.lightCap() );
		assertEquals( 12, FxBudget.decalCap() );
		assertFalse( FxBudget.sheenA() );
		assertFalse( FxBudget.reflections() );
		assertEquals( 0f, FxBudget.factor( FxBudget.P3 ), 0 );
		assertEquals( 0.6f, FxBudget.factor( FxBudget.P1 ), 1e-6 );
		assertEquals( 0.35f, FxBudget.factor( FxBudget.P2 ), 1e-6 );
		//never below MINIMAL
		for (int i = 0; i < 300; i++) FxBudget.frame( 1 / 10f );
		assertEquals( FxBudget.MINIMAL, FxBudget.tier() );
		//60 fps again: one tier up after three seconds, the next three later
		t = 0;
		while (FxBudget.tier() == FxBudget.MINIMAL && t < 10){
			FxBudget.frame( 1 / 60f );
			t += 1 / 60f;
		}
		assertEquals( FxBudget.REDUCED, FxBudget.tier() );
		assertTrue( "after " + t, t >= 3f && t < 5f );
		for (int i = 0; i < 3 * 60 + 2; i++) FxBudget.frame( 1 / 60f );
		assertEquals( FxBudget.FULL, FxBudget.tier() );
		assertEquals( 24, FxBudget.lightCap() );
		assertEquals( 12, FxBudget.persistentLightCap() );
	}

	@Test
	public void desktopCountsHalfAsManyAgain(){
		FxBudget.scale = 1.5f;
		assertEquals( 540, FxBudget.soft() );
		assertEquals( 780, FxBudget.hard() );
		assertEquals( 36, FxBudget.lightCap() );
		assertEquals( 60, FxBudget.ringCap() );
	}

	@Test
	public void aBlobCellsShareGrowsWithItsVolume(){
		assertEquals( 0.3f, FxBudget.blobDensity( 0 ), 0 );
		assertEquals( 0.3f, FxBudget.blobDensity( 1 ), 0 );
		assertEquals( 0.55f, FxBudget.blobDensity( 10 ), 1e-5 );
		assertEquals( 0.8f, FxBudget.blobDensity( 100 ), 1e-5 );
		assertEquals( 1f, FxBudget.blobDensity( 5000 ), 0 );
		FxBudget.Prioritized p = () -> FxBudget.P2;
		assertEquals( FxBudget.blobDensity( 10 ), p.density( 10 ), 0 );
	}
}
