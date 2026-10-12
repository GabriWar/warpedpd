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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Only identified gear can be enchanted or inscribed. */
public class EnchantIdentifiedTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	@Test
	public void unidentifiedGearCannotBeEnchanted(){
		for (Item item : new Item[]{ new Longsword(), new LeatherArmor() }){
			String what = item.getClass().getSimpleName();
			assertFalse( what + " is unidentified", item.isIdentified() );
			assertFalse( what, ScrollOfEnchantment.enchantable( item ) );
			item.identify();
			assertTrue( what, ScrollOfEnchantment.enchantable( item ) );
		}
	}
}
