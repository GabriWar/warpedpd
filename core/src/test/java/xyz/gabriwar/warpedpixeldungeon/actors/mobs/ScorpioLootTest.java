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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * A scorpio's drop cast its loot, the class PotionOfHealing, to an Item: every drop it rolled
 * and every bandit's snatch from one threw. The first rolls give the potion itself.
 */
public class ScorpioLootTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void theFirstDropIsAHealingPotion(){
		Dungeon.LimitedDrops.SCORPIO_HP.count = 0;
		boolean potion = false;
		for (int i = 0; i < 20 && !potion; i++){
			Dungeon.LimitedDrops.SCORPIO_HP.count = 0;
			Item loot = new Scorpio().createLoot();
			potion = loot instanceof PotionOfHealing;
		}
		assertTrue( "a healing potion, not a crash", potion );
		Dungeon.LimitedDrops.SCORPIO_HP.count = 0;
	}
}
