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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts;

import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.plants.Frostcorn;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.utils.Reflection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tipping darts offers only the seeds a dart can be tipped with. Every seed was offered, and
 * picking one of Warped's own (a frostcorn, a crop's) crashed: no tipped dart is made from it.
 */
public class DartTipSeedsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private static WndBag.ItemSelector selector() throws Exception {
		Field f = Dart.class.getDeclaredField( "itemSelector" );
		f.setAccessible( true );
		return (WndBag.ItemSelector) f.get( new Dart() );
	}

	@Test
	public void onlySeedsWithADartAreOffered() throws Exception {
		WndBag.ItemSelector tip = selector();
		assertTrue( tip.itemSelectable( new Sungrass.Seed() ) );
		assertFalse( tip.itemSelectable( new Frostcorn.Seed() ) );

		for (Class<?> cls : Generator.Category.SEED.classes){
			Item seed = (Item) Reflection.newInstance( cls );
			boolean offered = tip.itemSelectable( seed );
			assertEquals( cls.getName(), offered, TippedDart.types.containsKey( cls ) );
			if (offered) TippedDart.getTipped( (Plant.Seed) seed, 1 ).name();
		}
	}
}
