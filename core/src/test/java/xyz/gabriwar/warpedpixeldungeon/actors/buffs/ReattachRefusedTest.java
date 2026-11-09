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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCleansing;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A fresh bleed onto one already running attaches that same bleed again. When the second attach
 * was refused (a cleanse, a skill that shrugs a debuff off) it cleared the target of a bleed still
 * on the hero and still in the turn loop, and its next tick crashed (depth 65, a Warlock).
 */
public class ReattachRefusedTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aRefusedReattachLeavesTheBuffOnItsChar(){
		Hero hero = new Hero();
		Bleeding bleed = Buff.affect( hero, Bleeding.class );
		bleed.set( 5 );
		Buff.affect( hero, PotionOfCleansing.Cleanse.class, 5f );

		bleed.attachTo( hero );

		assertSame( "still bleeding the hero", hero, bleed.target );
		assertTrue( hero.buffs().contains( bleed ) );
	}
}
