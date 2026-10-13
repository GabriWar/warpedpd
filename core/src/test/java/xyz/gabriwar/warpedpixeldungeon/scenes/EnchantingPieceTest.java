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
	public void aRevealedTrioWaitsOnItsSocket(){
		Longsword sword = new Longsword();
		EnchantingScene.WeaponPiece p = new EnchantingScene.WeaponPiece( sword );
		assertNull( p.at( 0 ) );
		assertFalse( p.waiting( -1 ) );

		Weapon.Enchantment[] opts = p.roll();
		assertEquals( 3, opts.length );
		assertNotEquals( opts[0].getClass(), opts[1].getClass() );
		assertNotEquals( opts[0].getClass(), opts[2].getClass() );
		assertNotEquals( opts[1].getClass(), opts[2].getClass() );
		p.pending( -1, opts );
		assertTrue( p.waiting( -1 ) );
		//a reroll trio is another socket's: the free socket's stays apart
		assertFalse( p.waiting( 0 ) );

		//taking one fills the first socket, and the trio no longer fits the piece
		p.pending( -1, null );
		p.weave( opts[1] );
		assertNotNull( p.at( 0 ) );
		assertEquals( 1, p.filled() );
		p.pending( -1, opts );
		assertFalse( "a trio holding what the piece has is stale", p.waiting( -1 ) );

		//a reroll trio waits on its own socket only
		Weapon.Enchantment[] re = p.roll();
		p.pending( 0, re );
		assertTrue( p.waiting( 0 ) );
		assertFalse( p.waiting( 1 ) );
		p.pending( 0, null );
		assertFalse( p.waiting( 0 ) );
		assertEquals( -1, sword.pendingRerollSlot );
	}

	@Test
	public void armorReadsTheSameWay(){
		LeatherArmor leather = new LeatherArmor();
		EnchantingScene.ArmorPiece p = new EnchantingScene.ArmorPiece( leather );
		Armor.Glyph[] opts = p.roll();
		p.pending( -1, opts );
		assertTrue( p.waiting( -1 ) );
		p.weave( opts[0] );
		assertEquals( opts[0], p.at( 0 ) );
		assertTrue( p.hasGood() );
		p.unweave();
		assertNull( p.at( 0 ) );
		assertNull( leather.pendingWeaveOpts );
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
