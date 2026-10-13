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

package xyz.gabriwar.warpedpixeldungeon.actors.hero;

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTransmutation;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//a searched bookshelf gives a scroll 8% of the time, and only one of five kinds
public class ShelfScrollTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void onlyTheScholarsScrolls(){
		assertEquals( 0.08f, Hero.SHELF_SCROLL_CHANCE, 0f );
		List<Class<?>> allowed = Arrays.asList( ScrollOfIdentify.class, ScrollOfRemoveCurse.class,
				ScrollOfRecharging.class, ScrollOfTransmutation.class, ScrollOfMagicMapping.class );
		HashSet<Class<?>> seen = new HashSet<>();
		for (int i = 0; i < 2000; i++){
			Item s = Hero.shelfScroll();
			assertTrue( s.getClass().getSimpleName(), allowed.contains( s.getClass() ) );
			seen.add( s.getClass() );
		}
		assertEquals( "every kind turns up", allowed.size(), seen.size() );
	}
}
