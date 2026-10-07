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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Well fed, the mana pool fills half again as fast (ManaRegen.WELL_FED_BOOST). */
public class WellFedManaTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	//the mana a resting hero regains over these turns
	private static int regained( boolean wellFed ){
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.HP = hero.HT = 20;
		hero.MT = 100000;
		hero.MP = 0;
		hero.resting = true;
		if (wellFed) Buff.affect( hero, WellFed.class ).reset();
		ManaRegen regen = new ManaRegen();
		regen.attachTo( hero );
		for (int i = 0; i < 20; i++) regen.act();
		return hero.MP;
	}

	@Test
	public void wellFedRegeneratesHalfAgainAsFast(){
		Hero previous = Dungeon.hero;
		try {
			int normal = regained( false ), fed = regained( true );
			assertEquals( Math.round( normal * ManaRegen.WELL_FED_BOOST ), fed, 1 );
		} finally {
			Dungeon.hero = previous;
		}
	}
}
