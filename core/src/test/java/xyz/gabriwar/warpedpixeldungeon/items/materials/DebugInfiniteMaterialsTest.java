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

package xyz.gabriwar.warpedpixeldungeon.items.materials;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The debug menu's infinite building materials: every cost is met and nothing is taken. */
public class DebugInfiniteMaterialsTest {

	@After
	public void off(){
		Dungeon.debugInfiniteMaterials = false;
	}

	@Test
	public void costsAreAlwaysMetAndNothingIsSpent(){
		//the toggle answers before the hero's bags are looked at, so no hero is needed
		Hero hero = null;
		Dungeon.gold = 10;
		assertFalse( "gold short without the toggle", Material.spend( hero, Gold.class, 800 ) );

		Dungeon.debugInfiniteMaterials = true;
		assertEquals( Material.INFINITE, Material.count( hero, Plank.class ) );
		assertEquals( Material.INFINITE, Material.count( hero, Gold.class ) );
		assertTrue( Material.spend( hero, Plank.class, 3 ) );
		assertTrue( Material.spend( hero, Gold.class, 800 ) );
		assertEquals( "no gold taken", 10, Dungeon.gold );
	}
}
