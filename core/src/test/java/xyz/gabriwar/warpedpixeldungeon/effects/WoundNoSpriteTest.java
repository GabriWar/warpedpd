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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * A wound opened on a char with no sprite draws nothing. The blow that opens it can have sent the
 * char through a chasm first: a Fight Gloves stun grounded a bat flying over one, Chasm.fallThrough
 * took it off the floor and let its sprite go, and the same blow's Predation then opened a wound
 * on it and crashed. Every skill opens its wounds through here.
 */
public class WoundNoSpriteTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aWoundOnACharWithNoSpriteDrawsNothing(){
		Bat bat = new Bat();
		assertNull( bat.sprite );
		Wound.hit( bat );
		Wound.hit( bat, 45f );
	}
}
