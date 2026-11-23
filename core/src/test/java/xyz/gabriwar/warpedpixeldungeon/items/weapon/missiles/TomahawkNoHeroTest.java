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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A tomahawk's bleed read the hero's Ring of Sharpshooting with no hero to read it from: its page in
 * the journal's catalog, opened from the title screen, crashed the game (1 report). With no hero it
 * counts no ring, as every other thrown weapon, bow and gun already did.
 */
public class TomahawkNoHeroTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void itsPageReadsWithNoHero(){
		Hero saved = Dungeon.hero;
		try {
			Dungeon.hero = null;
			Tomahawk t = new Tomahawk();
			t.identify( false );
			assertTrue( t.isIdentified() );
			assertTrue( t.info().length() > 0 );
			assertEquals( t.minBleed( t.buffedLvl() ), t.minBleed(), 0f );
			assertEquals( t.maxBleed( t.buffedLvl() ), t.maxBleed(), 0f );
		} finally {
			Dungeon.hero = saved;
		}
	}
}
