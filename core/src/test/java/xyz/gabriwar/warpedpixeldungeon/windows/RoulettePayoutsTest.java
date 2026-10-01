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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.ui.RouletteWheel;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The bar's roulette (WndRoulette) must be the real European game: a wheel of the 37
 * numbers in their true order with the true reds, and every one of the table's 49 bets
 * paying so that, over all 37 numbers, a stake comes back as exactly 36/37 of itself -
 * the single zero's house edge, no more and no less.
 */
public class RoulettePayoutsTest {

	@Test
	public void theWheelHoldsEachNumberOnce(){
		int[] sorted = RouletteWheel.EUROPEAN.clone();
		Arrays.sort( sorted );
		for (int i = 0; i < 37; i++) assertEquals( i, sorted[i] );
		int reds = 0;
		for (int n = 1; n <= 36; n++) if (RouletteWheel.isRed( n )) reds++;
		assertEquals( 18, reds );
		assertTrue( !RouletteWheel.isRed( 0 ) );
		//red and black alternate round the wheel after the zero
		for (int i = 1; i + 1 < 37; i++){
			assertTrue( RouletteWheel.isRed( RouletteWheel.EUROPEAN[i] ) != RouletteWheel.isRed( RouletteWheel.EUROPEAN[i + 1] ) );
		}
	}

	@Test
	public void everyBetKeepsTheSingleZeroEdge(){
		for (int bet = 0; bet <= 48; bet++){
			int returned = 0, winners = 0;
			for (int n = 0; n <= 36; n++){
				if (WndRoulette.wins( bet, n )){
					winners++;
					returned += WndRoulette.odds( bet ) + 1;
				}
			}
			assertTrue( "bet " + bet + " never wins", winners > 0 );
			assertEquals( "bet " + bet + ": stake back over 37 spins", 36, returned );
		}
	}

	@Test
	public void theZeroLosesEveryOutsideBet(){
		for (int bet = 37; bet <= 48; bet++){
			assertTrue( "bet " + bet, !WndRoulette.wins( bet, 0 ) );
		}
		assertTrue( WndRoulette.wins( 0, 0 ) );
	}
}
