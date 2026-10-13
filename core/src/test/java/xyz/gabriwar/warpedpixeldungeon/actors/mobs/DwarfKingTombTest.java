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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DwarfKingTombTest {

	@Test
	public void theTombIsATargetNotSomeoneToTalkTo(){
		DwarfKingTomb tomb = new DwarfKingTomb();
		assertEquals( Char.Alignment.ENEMY, tomb.alignment );
		assertFalse( "a tap attacks it", tomb.heroShouldInteract() );
		assertEquals( 0, tomb.damageRoll() );
	}
}
