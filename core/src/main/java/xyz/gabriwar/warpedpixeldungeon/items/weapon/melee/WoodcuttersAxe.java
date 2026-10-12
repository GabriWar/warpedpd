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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * A felling axe: a tool first (it fells a tree in 3 turns, the quickest of any axe:
 * homestead/gathering/Gathering), a poor weapon after, as the pickaxe is - tier-2 blows for a
 * tier-3 arm. Sold at the town's general store.
 */
public class WoodcuttersAxe extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WOODCUTTERS_AXE;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		tier = 2;

		bones = false;
	}

	@Override
	public int STRReq( int lvl ){
		return super.STRReq( lvl ) + 2; //tier 3 strength requirement with tier 2 damage stats
	}

	//a tool, not a duelist's weapon: no ability was ever designed for it
	@Override
	public boolean hasDuelistAbility(){
		return false;
	}
}
