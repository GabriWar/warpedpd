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

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ElfBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;

import static org.junit.Assert.assertEquals;

//a strength line takes off what it says, never 0, on every weapon and armor
public class StrengthLineTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void theCutIsOneOrTwoByTheRoll(){
		float max = RarityLine.STR_REQ.max();
		assertEquals( 0, Quality.strReqStep( 0f ) );
		assertEquals( 1, Quality.strReqStep( max * 0.01f ) );
		assertEquals( 1, Quality.strReqStep( max * 0.49f ) );
		assertEquals( 2, Quality.strReqStep( max * 0.5f ) );
		assertEquals( 2, Quality.strReqStep( max ) );
	}

	@Test
	public void everyPieceTakesOffWhatItsLineSays(){
		for (Weapon w : new Weapon[]{ new Longsword(), new Greataxe(), new ElfBow() }){
			check( w, () -> w.STRReq() );
		}
		PlateArmor plate = new PlateArmor();
		check( plate, () -> plate.STRReq() );
	}

	private static void check( xyz.gabriwar.warpedpixeldungeon.items.Item item, java.util.function.IntSupplier req ){
		Quality.roll( item, Rarity.RARE, ItemType.GAMMA, 0 );
		Quality q = item.quality;
		q.lines.clear();
		q.rolls.clear();
		int base = req.getAsInt();
		q.lines.add( RarityLine.STR_REQ );
		for (float roll : new float[]{ 0.01f, 0.3f, 0.6f, 1f }){
			q.rolls.clear();
			q.rolls.add( roll );
			int cut = roll >= 0.5f ? 2 : 1;
			String name = item.getClass().getSimpleName() + " at " + roll;
			assertEquals( name, base - cut, req.getAsInt() );
			assertEquals( name, "-" + cut + " strength required", RarityLine.STR_REQ.describe( item, q.roll( RarityLine.STR_REQ ) ) );
		}
	}
}
