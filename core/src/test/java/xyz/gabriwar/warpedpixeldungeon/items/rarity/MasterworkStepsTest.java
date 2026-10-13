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

import com.watabou.utils.Bundle;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//three masterwork steps of a core each: a quarter, another quarter, then every line doubled
public class MasterworkStepsTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	private static Quality line( Rarity rarity ){
		Longsword sword = new Longsword();
		Quality.roll( sword, rarity, ItemType.GAMMA, 0 );
		Quality q = sword.quality;
		q.lines.clear();
		q.rolls.clear();
		q.lines.add( RarityLine.ACCURACY );
		q.rolls.add( 0.6f );
		return q;
	}

	@Test
	public void threeStepsOfOneCoreAndTheLastDoubles(){
		Longsword sword = new Longsword();
		Quality.roll( sword, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		Quality q = sword.quality;
		q.lines.clear();
		q.rolls.clear();
		q.lines.add( RarityLine.ACCURACY );
		q.rolls.add( 0.6f );
		float base = RarityLine.ACCURACY.max() * 0.6f;
		float[] scale = { 1f, 1.25f, 1.5f, 2f };
		for (int step = 0; step <= 3; step++){
			assertEquals( "step " + step, base * scale[step], q.value( RarityLine.ACCURACY ), 1e-5f );
			if (step < 3){
				assertEquals( 1, q.masterworkCost() );
				q.masterworkStep( sword );
			}
		}
		assertTrue( q.fullyMasterworked() );
		assertEquals( 0, q.masterworkCost() );
	}

	@Test
	public void theThirdStepTakesALegendaryBase(){
		Longsword sword = new Longsword();
		Quality.roll( sword, Rarity.RARE, ItemType.GAMMA, 3 );
		assertEquals( 2, sword.quality.masterwork );
		assertFalse( sword.quality.fullyMasterworked() );
		assertEquals( 0, sword.quality.masterworkCost() );
	}

	@Test
	public void aTenStepSaveComesBackInThree(){
		int[][] cases = { {0, 0}, {1, 1}, {5, 1}, {6, 2}, {9, 2}, {10, 3} };
		for (int[] c : cases){
			Quality q = line( Rarity.LEGENDARY );
			Bundle old = new Bundle();
			q.storeInBundle( old );
			//as an old save wrote it: ten steps, no mark of the three
			old.remove( "masterwork_steps" );
			old.put( "masterwork", c[0] );
			Quality back = new Quality();
			back.restoreFromBundle( old );
			assertEquals( "old step " + c[0], c[1], back.masterwork );
		}
		//a save of today keeps its step
		Quality q = line( Rarity.LEGENDARY );
		q.masterwork = 1;
		Bundle b = new Bundle();
		q.storeInBundle( b );
		Quality back = new Quality();
		back.restoreFromBundle( b );
		assertEquals( 1, back.masterwork );
	}
}
