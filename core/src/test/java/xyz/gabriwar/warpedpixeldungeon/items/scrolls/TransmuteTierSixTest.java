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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.DualGreatSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.SharpKatana;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Transmuting one of Warped's tier 6 weapons (the alchemy ones, the relics) crashed the game: the
 * generator has five tiers of weapons and the scroll asked for the sixth. It now gives a tier 5
 * weapon, as Evolution does, keeping the old one's upgrades.
 */
public class TransmuteTierSixTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aTierSixWeaponBecomesATierFiveOne(){
		Hero saved = Dungeon.hero;
		try {
			Dungeon.hero = new Hero();
			for (MeleeWeapon w : new MeleeWeapon[]{ new DualGreatSword(), new SharpKatana() }){
				assertEquals( 6, w.tier );
				w.upgrade( 2 );
				Item n = ScrollOfTransmutation.changeItem( w );
				assertNotNull( w.getClass().getSimpleName(), n );
				assertTrue( n instanceof MeleeWeapon );
				assertEquals( 5, ((MeleeWeapon) n).tier );
				assertEquals( 2, ((MeleeWeapon) n).trueLevel() );
			}
		} finally {
			Dungeon.hero = saved;
		}
	}
}
