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

package xyz.gabriwar.warpedpixeldungeon.messages;

import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Every language can be chosen: a family of texts the language has no file for (Warped's guide,
 * skills...) is read in English. Indonesian used to crash the settings, and every start after,
 * on the first family with no "_in" file.
 */
public class LanguageFallbackTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@After
	public void english(){
		Messages.setup( Languages.ENGLISH );
	}

	@Test
	public void everyLanguageLoads(){
		for (Languages lang : Languages.values()){
			Messages.setup( lang );
			assertEquals( lang, Messages.lang() );
		}
	}

	@Test
	public void indonesianKeepsItsOwnTextsAndReadsTheRestInEnglish(){
		Messages.setup( Languages.INDONESIAN );
		assertEquals( "kamu", Messages.get( "actors.hero.hero.name" ) );
		assertEquals( "%1$s is maxed out.", Messages.get( "actors.hero.skills.skill.maxed" ) );
	}
}
