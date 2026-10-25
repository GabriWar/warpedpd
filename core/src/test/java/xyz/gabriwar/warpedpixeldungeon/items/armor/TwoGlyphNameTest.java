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

package xyz.gabriwar.warpedpixeldungeon.items.armor;

import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Potential;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Satisfying;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** An armor with two glyphs reads "armor of brimstone and stone", not "armor of brimstone of stone". */
public class TwoGlyphNameTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	@After
	public void english(){
		Messages.setup( Languages.ENGLISH );
	}

	//the name with the two glyphs, its plain name standing as "~"
	private static String named( Armor.Glyph first, Armor.Glyph second ){
		LeatherArmor a = new LeatherArmor();
		a.identify();
		String plain = a.name();
		a.glyph = first;
		a.glyph2 = second;
		return a.name().replace( plain, "~" );
	}

	@Test
	public void twoGlyphsAfterTheNameAreJoined(){
		Messages.setup( Languages.ENGLISH );
		assertEquals( "~ of brimstone and stone", named( new Brimstone(), new Stone() ) );
		assertEquals( "~ of stone", named( new Stone(), null ) );
		//a glyph named before the armor chains as before
		assertEquals( "satisfying ~ of stone", named( new Satisfying(), new Stone() ) );
	}

	@Test
	public void inPortugueseTheSharedPrepositionIsSaidOnce(){
		Messages.setup( Languages.PORTUGUESE );
		assertEquals( "~ de enxofre e pedra", named( new Brimstone(), new Stone() ) );
		assertEquals( "~ do potencial e da viscosidade", named( new Potential(), new Viscosity() ) );
	}
}
