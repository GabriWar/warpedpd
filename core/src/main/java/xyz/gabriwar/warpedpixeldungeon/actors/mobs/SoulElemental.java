/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfChilli;
import xyz.gabriwar.warpedpixeldungeon.sprites.ElementalSprite;

public class SoulElemental extends Elemental {

	{
		spriteClass = ElementalSprite.NewbornFire.class;

		loot = new PotionOfChilli();
		lootChance = 0.01f;

		alignment = Alignment.ALLY;
	}

	@Override
	protected void meleeProc( Char enemy, int damage ) {
		// soul elemental has no melee effect
	}

	@Override
	protected void rangedProc( Char enemy ) {
		// soul elemental has no ranged attack
	}

}
