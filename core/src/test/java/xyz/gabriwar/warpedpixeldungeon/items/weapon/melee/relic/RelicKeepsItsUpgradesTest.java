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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A relic weapon refused every plain upgrade past +15, a cap Sprouted's relics never had, while
 * an upgrade that keeps the enchantment (Magical Infusion, a Scroll of Reforging, the troll
 * smith's reforge) went past it. A save rebuilds an item's level one plain upgrade at a time, so
 * the next load cut such a relic back to +15: a player's +40 Crom Cruach Axe came back +15.
 */
public class RelicKeepsItsUpgradesTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	//the relic as the game reads it back: written to a save file's bytes, then read again
	@SuppressWarnings("unchecked")
	private static <T extends Item> T reload( T relic ) throws Exception {
		Bundle file = new Bundle();
		file.put( "relic", relic );
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		assertTrue( Bundle.write( file, bytes ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( bytes.toByteArray() ) );
		return (T) read.get( "relic" );
	}

	@Test
	public void aScrollOfUpgradeRaisesARelicPastFifteen(){
		CromCruachAxe axe = new CromCruachAxe();
		for (int i = 0; i < 40; i++) axe.upgrade();
		assertEquals( 40, axe.trueLevel() );
	}

	@Test
	public void aRelicPastFifteenLoadsWithAllItsLevels() throws Exception {
		for (RelicMeleeWeapon relic : new RelicMeleeWeapon[]{
				new CromCruachAxe(), new AresSword(), new LokisFlail(), new NeptunusTrident() }){
			//as Magical Infusion raises it: an upgrade that keeps the enchantment
			for (int i = 0; i < 40; i++) relic.upgrade( true );
			assertEquals( 40, relic.trueLevel() );

			RelicMeleeWeapon back = reload( relic );
			assertEquals( relic.getClass(), back.getClass() );
			assertEquals( relic.getClass().getSimpleName(), 40, back.trueLevel() );
		}
	}

	//Sprouted's relics and Jupiter's Wrath are reinforced, so the goos and the smith take them
	//past +15; the port had dropped the flag, and an older save's relic comes back reinforced
	@Test
	public void relicsAreReinforcedEvenFromAnOlderSave() throws Exception {
		for (Item relic : new Item[]{ new CromCruachAxe(), new AresSword(), new LokisFlail(),
				new NeptunusTrident(), new JupitersWraith() }){
			assertTrue( relic.getClass().getSimpleName(), relic.isReinforced() );
			relic.reinforced = false;
			assertTrue( relic.getClass().getSimpleName() + " from an older save", reload( relic ).isReinforced() );
		}
	}
}
