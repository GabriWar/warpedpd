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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A storage chest's take-out window shows the hero's gear and the chest's belly beside what it
 * holds; only what it holds can be taken. Taking the belly put it in the backpack and then inside
 * itself (a crash on every save after), taking worn gear copied it.
 */
public class StorageChestWithdrawTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void onlyWhatTheChestHoldsIsTakenOut(){
		StorageChest chest = new StorageChest();
		Food food = new Food();
		chest.storage.items.add( food );

		assertTrue( "what lies in the chest", chest.holds( food ) );
		assertFalse( "the chest's own belly", chest.holds( chest.storage ) );
		assertFalse( "the hero's gear shown above it", chest.holds( new Dagger() ) );
	}
}
