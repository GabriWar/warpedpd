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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

//the pedestal's bookkeeping: a paid trio waits on its socket until taken or made stale
public class EnchantingPieceTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void threeRevealedFitThePieceUntilItChanges(){
		Longsword sword = new Longsword();
		EnchantingScene.WeaponPiece p = new EnchantingScene.WeaponPiece( sword );
		assertNull( p.at( 0 ) );

		Weapon.Enchantment[] opts = p.roll();
		assertEquals( 3, opts.length );
		assertNotEquals( opts[0].getClass(), opts[1].getClass() );
		assertNotEquals( opts[0].getClass(), opts[2].getClass() );
		assertNotEquals( opts[1].getClass(), opts[2].getClass() );
		assertTrue( p.valid( opts ) );

		//taking one fills the first socket, and the three no longer fit the piece
		p.weave( opts[1] );
		assertNotNull( p.at( 0 ) );
		assertEquals( 1, p.filled() );
		assertFalse( "three holding what the piece has are refused", p.valid( opts ) );
		//and a new reveal never offers what the piece already holds
		for (Weapon.Enchantment o : p.roll()) assertNotEquals( opts[1].getClass(), o.getClass() );
	}

	@Test
	public void armorReadsTheSameWay(){
		LeatherArmor leather = new LeatherArmor();
		EnchantingScene.ArmorPiece p = new EnchantingScene.ArmorPiece( leather );
		Armor.Glyph[] opts = p.roll();
		assertTrue( p.valid( opts ) );
		p.weave( opts[0] );
		assertEquals( opts[0], p.at( 0 ) );
		assertTrue( p.hasGood() );
		p.unweave();
		assertNull( p.at( 0 ) );
	}

	@Test
	public void aCardReadsTheFirstSentence(){
		assertEquals( "One.", EnchantingScene.gist( "One. Two." ) );
		assertEquals( "One", EnchantingScene.gist( "One\n\nTwo." ) );
		assertEquals( "Hits _hard_.", EnchantingScene.gist( "Hits _hard_. Then more." ) );
		//a highlight cut in two is closed
		assertEquals( "A _b.c._", EnchantingScene.gist( "A _b.c. d_ e." ) );
	}
}
