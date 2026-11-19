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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.quest.vault.VaultDM100;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.quest.vault.VaultGhoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.quest.vault.VaultRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.quest.vault.VaultSkeleton;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The vault's guards drop their dwarf token whatever the hero's level: they are maxLvl 30, and
 * a hero past 32 got no token from them (Mob.rollToDropLoot), too few to open the token door.
 */
public class VaultTokensTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aGuardsTokenIsNeverOutLevelled(){
		for (Mob guard : new Mob[]{ new VaultRat(), new VaultGhoul(), new VaultSkeleton(), new VaultDM100() }){
			assertTrue( guard.getClass().getSimpleName(), guard.lootIgnoresLevel() );
		}
		assertFalse( "a plain rat's loot still is", new Rat().lootIgnoresLevel() );
	}
}
