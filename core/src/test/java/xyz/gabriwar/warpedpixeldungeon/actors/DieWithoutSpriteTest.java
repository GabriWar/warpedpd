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

package xyz.gabriwar.warpedpixeldungeon.actors;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * A char with no sprite yet (an overworld creature not on screen) can be killed by another's
 * blow: the crash report had a living plant's hit end in Char.die calling sprite.die() on null.
 */
public class DieWithoutSpriteTest {

	@Test
	public void aCharWithNoSpriteDiesQuietly(){
		Char ch = new Char(){
			@Override
			protected boolean act(){
				return true;
			}
		};
		ch.HP = ch.HT = 5;
		assertNull( ch.sprite );
		ch.HP = 0;
		ch.die( null );
		assertFalse( ch.isAlive() );
	}
}
