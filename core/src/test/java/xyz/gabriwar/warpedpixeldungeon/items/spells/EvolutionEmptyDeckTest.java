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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Reflection;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Evolution picked the new weapon from its tier's deck as the floors have left it: once every card
 * of a tier had dropped (the deck refills only on the generator's next draw) there was no weapon to
 * pick and the spell threw (1 report, tier 5's deck spent after 22 drops). It picks from the tier's
 * full list now, as transmutation does, and leaves the deck as it was.
 */
public class EvolutionEmptyDeckTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aSpentTierDeckStillGivesAWeapon(){
		Generator.Category c = Generator.Category.WEP_T5;
		float[] savedProbs = c.probs;
		Hero savedHero = Dungeon.hero;
		try {
			Dungeon.hero = new Hero();
			c.probs = new float[c.classes.length];

			MeleeWeapon w = (MeleeWeapon) Reflection.newInstance( c.classes[0] );
			w.upgrade( 3 );
			for (int i = 0; i < 20; i++){
				Item n = Evolution.changeItem( w );
				assertTrue( n instanceof MeleeWeapon );
				assertEquals( 5, ((MeleeWeapon) n).tier );
				assertNotEquals( w.getClass(), n.getClass() );
				assertEquals( 3, ((MeleeWeapon) n).trueLevel() );
			}
			assertArrayEquals( "the deck is left as it was", new float[c.classes.length], c.probs, 0f );
		} finally {
			c.probs = savedProbs;
			Dungeon.hero = savedHero;
		}
	}
}
