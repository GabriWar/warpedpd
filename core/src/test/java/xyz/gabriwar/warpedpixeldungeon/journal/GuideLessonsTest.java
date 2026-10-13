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

package xyz.gabriwar.warpedpixeldungeon.journal;

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

//what an item picked up first teaches: its page of the basics writes itself
public class GuideLessonsTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void pickingUpTeaches(){
		assertEquals( GuideGraph.BASICS_MAGIC, GuideGraph.lessonFor( new WandOfMagicMissile() ) );
		assertEquals( GuideGraph.BASICS_FARMING, GuideGraph.lessonFor( new Sungrass.Seed() ) );
		assertEquals( GuideGraph.BASICS_LOOTING, GuideGraph.lessonFor( new IronKey( 1 ) ) );
		assertNull( GuideGraph.lessonFor( new Food() ) );
	}
}
