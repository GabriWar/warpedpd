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

import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * An unidentified item shows its rarity and its type, but not what they do for it: the rolled
 * lines, their strength and the perk stay hidden until it is identified.
 */
public class QualityUnidentifiedTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	@Test
	public void theLinesAndThePerkWaitForIdentification(){
		Longsword sword = new Longsword();
		Quality.roll( sword, Rarity.RARE, ItemType.ALPHA, 0 );
		Quality q = sword.quality;
		String hidden = Messages.get( Quality.class, "unknown_lines" );
		String hiddenPerk = Messages.get( Quality.class, "unknown_perk" );

		assertFalse( sword.isIdentified() );
		String rarity = q.describeRarity( sword );
		assertTrue( rarity, rarity.contains( Rarity.RARE.title() ) );
		assertTrue( rarity, rarity.contains( hidden ) );
		assertFalse( rarity, rarity.contains( Messages.get( Quality.class, "potency", Math.round( q.potency() * 100f ) ) ) );
		String type = q.describeType( sword, ItemType.ALPHA );
		assertTrue( type, type.contains( ItemType.ALPHA.title() ) );
		assertTrue( type, type.contains( hiddenPerk ) );

		sword.identify();
		assertFalse( q.describeRarity( sword ).contains( hidden ) );
		assertFalse( q.describeType( sword, ItemType.ALPHA ).contains( hiddenPerk ) );
	}
}
