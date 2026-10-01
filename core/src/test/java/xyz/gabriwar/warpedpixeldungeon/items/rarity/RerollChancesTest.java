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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The smith's rarity wheel (WndRarityWheel) is a 37-pocket roulette whose pockets the
 * rarities share: every stake and depth must fill exactly 37 pockets, a tier with any
 * chance keeps one, ten cores give exotic its fixed long shot, and adding cores never
 * makes the rarer tiers less likely.
 */
public class RerollChancesTest {

	private static final int[] DEPTHS = { 1, 4, 5, 14, 15, 25, 97 };

	@Test
	public void pocketsAlwaysFillTheWheel(){
		for (int depth : DEPTHS){
			for (int cores = 0; cores <= Rarity.MAX_STAKE + 3; cores++){
				int[] p = Rarity.rerollPockets( depth, cores );
				int sum = 0;
				for (int v : p){
					assertTrue( "negative pockets at depth " + depth + ", " + cores + " cores", v >= 0 );
					sum += v;
				}
				assertEquals( "depth " + depth + ", " + cores + " cores", Rarity.POCKETS, sum );
				Rarity[] wheel = Rarity.rerollWheel( depth, cores );
				int[] seen = new int[p.length];
				for (Rarity r : wheel){ assertNotNull( r ); seen[r.ordinal()]++; }
				for (int i = 0; i < p.length; i++) assertEquals( p[i], seen[i] );
			}
		}
	}

	@Test
	public void oneCoreCutsTheWheelLikeAFreshDrop(){
		for (int depth : new int[]{ 1, 10, 25 }){
			int[] p = Rarity.rerollPockets( depth, 1 );
			float total = 0;
			for (Rarity r : Rarity.values()) total += r.weight( depth );
			for (Rarity r : Rarity.values()){
				float want = r.weight( depth ) / total * Rarity.POCKETS;
				assertTrue( r + " at depth " + depth + ": " + p[r.ordinal()] + " pockets for " + want,
						Math.abs( p[r.ordinal()] - want ) <= 1.01f );
				if (r.weight( depth ) == 0) assertEquals( 0, p[r.ordinal()] );
			}
		}
	}

	@Test
	public void tenCoresGiveExoticItsLongShot(){
		for (int depth : new int[]{ 1, 10, 25 }){
			assertEquals( Rarity.MAX_EXOTIC_POCKETS, Rarity.rerollPockets( depth, Rarity.MAX_STAKE )[Rarity.EXOTIC.ordinal()] );
		}
		//a gamble still: exotic never passes one in four, at any stake or depth
		for (int depth : new int[]{ 1, 10, 25, 97 }){
			for (int cores = 1; cores <= Rarity.MAX_STAKE; cores++){
				int ex = Rarity.rerollPockets( depth, cores )[Rarity.EXOTIC.ordinal()];
				assertTrue( depth + "/" + cores + ": " + ex, ex / (float)Rarity.POCKETS <= 0.25f );
			}
		}
	}

	@Test
	public void moreCoresNeverHurtTheRarerTiers(){
		for (int depth : new int[]{ 1, 10, 25 }){
			int[] prev = Rarity.rerollPockets( depth, 1 );
			for (int cores = 2; cores <= Rarity.MAX_STAKE; cores++){
				int[] p = Rarity.rerollPockets( depth, cores );
				int prevUp = 0, up = 0;
				for (int i = Rarity.RARE.ordinal(); i < p.length; i++){ prevUp += prev[i]; up += p[i]; }
				assertTrue( "depth " + depth + ", " + cores + " cores", up >= prevUp );
				assertTrue( p[Rarity.COMMON.ordinal()] <= prev[Rarity.COMMON.ordinal()] );
				prev = p;
			}
		}
	}
}
