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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * FloatingText's stack on a crowded cell (headless on EnchantFxTest's stage: the texts are empty, as
 * no fonts are loaded, so every one overlaps the next and is nudged): a text pushed up loses time as
 * in Shattered, the fifth one down to nothing, but a damage number keeps NUMBER_FLOOR s; what counts
 * as a damage number.
 */
public class FloatingTextStackTest {

	private static final int KEY = 4242;

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final List<FloatingText> made = new ArrayList<>();

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
	}

	@After
	public void tearDown(){
		for (FloatingText t : made) t.kill();
		made.clear();
		rig.down();
	}

	//a text on the cell, a damage number or a word, pushed onto its stack
	private FloatingText text( boolean number ){
		FloatingText t = new FloatingText();
		t.reset( 40, 40, "", CharSprite.NEGATIVE, FloatingText.NO_ICON, false );
		t.damageNumber = number;
		made.add( t );
		FloatingText.push( t, KEY );
		return t;
	}

	@Test
	public void theOldestNumberOfSixKeepsItsFloorAndAWordIsStillCut(){
		FloatingText number = text( true );
		FloatingText word = text( false );
		for (int i = 0; i < 4; i++) text( false );
		//five texts came after the number, four after the word
		assertTrue( "the number keeps " + FloatingText.NUMBER_FLOOR + " s: " + number.timeLeft(),
				number.timeLeft() >= FloatingText.NUMBER_FLOOR - 1e-4f );
		assertEquals( "the word under it is cut as in Shattered", 1f - 4 / 5f, word.timeLeft(), 1e-4f );
		text( false );
		assertEquals( "a sixth below cuts a word to nothing", 0f, word.timeLeft(), 1e-4f );
		assertTrue( "and the number still keeps its floor", number.timeLeft() >= FloatingText.NUMBER_FLOOR - 1e-4f );
	}

	@Test
	public void theFloorNeverLengthensANumbersTime(){
		assertEquals( 0.1f, FloatingText.nudged( 0.1f, 6, true ), 1e-4f );
		assertEquals( FloatingText.NUMBER_FLOOR, FloatingText.nudged( 0.9f, 6, true ), 1e-4f );
		assertEquals( 0.8f, FloatingText.nudged( 0.9f, 1, true ), 1e-4f );
		assertEquals( 0f, FloatingText.nudged( 0.9f, 6, false ), 1e-4f );
	}

	@Test
	public void aDamageNumberIsAWholeNumberWithADamageIcon(){
		assertTrue( FloatingText.isDamageNumber( "12", FloatingText.PHYS_DMG ) );
		assertTrue( FloatingText.isDamageNumber( "-3", FloatingText.POISON ) );
		assertTrue( FloatingText.isDamageNumber( "7", FloatingText.HIT_WEP ) );
		assertFalse( "a word", FloatingText.isDamageNumber( "terrified", FloatingText.PHYS_DMG ) );
		assertFalse( "a heal", FloatingText.isDamageNumber( "5", FloatingText.HEALING ) );
		assertFalse( "no icon", FloatingText.isDamageNumber( "5", FloatingText.NO_ICON ) );
		assertFalse( "a sign alone", FloatingText.isDamageNumber( "+", FloatingText.PHYS_DMG ) );
		assertFalse( FloatingText.isDamageNumber( null, FloatingText.PHYS_DMG ) );
	}
}
