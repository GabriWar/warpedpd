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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Stalwart, heavy armor's perk, keeps the armor's minimum block at a share of its maximum. Under
 * Faith is my Armor (the NO_ARMOR challenge) Armor.DRMin gave 0 before the perk was added, so it
 * did nothing and the armor's block range showed no number for it. The challenge lowers the
 * maximum; Stalwart now holds its share of that maximum, and the armor's text shows it.
 */
public class StalwartFaithTest {

	private int savedChallenges;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		savedChallenges = Dungeon.challenges;
		Dungeon.challenges = Challenges.NO_ARMOR;
	}

	@After
	public void tearDown(){
		Dungeon.challenges = savedChallenges;
	}

	private static PlateArmor plate( ItemType type ){
		PlateArmor plate = new PlateArmor();
		Quality.roll( plate, Rarity.RARE, type, 0 );
		plate.quality.lines.clear();
		plate.quality.rolls.clear();
		assertEquals( GearPerk.STALWART, GearPerk.of( plate ) );
		return plate;
	}

	@Test
	public void stalwartHoldsItsShareOfTheLoweredMaximum(){
		for (ItemType type : ItemType.values()){
			PlateArmor plate = plate( type );
			float share = 0.10f + 0.10f * type.rank;
			for (int lvl : new int[]{ 0, 3, 10 }){
				int max = plate.DRMax( lvl );
				String name = type + " plate +" + lvl;
				assertEquals( name, Math.round( max * share ), plate.DRMin( lvl ) );
				assertTrue( name + " blocks at least 1", plate.DRMin( lvl ) > 0 );
			}
		}
	}

	@Test
	public void theArmorsTextShowsTheMinimum(){
		PlateArmor plate = plate( ItemType.ALPHA );
		plate.levelKnown = true;
		String shown = plate.DRMin() + "-" + plate.DRMax() + " damage";
		assertTrue( shown + " in: " + plate.info(), plate.info().contains( shown ) );
		assertTrue( plate.DRMin() > 0 );
	}

	@Test
	public void plainArmorStillBlocksFromNothing(){
		PlateArmor plain = new PlateArmor();
		assertEquals( 0, plain.DRMin( 0 ) );
		assertEquals( 0, plain.DRMin( 10 ) );
	}
}
