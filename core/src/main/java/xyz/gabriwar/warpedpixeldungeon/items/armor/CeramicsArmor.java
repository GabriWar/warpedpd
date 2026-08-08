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

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

//fired clay armor: high base defense, but brittle - it gains
//less maximum defense from upgrades than other armors
public class CeramicsArmor extends Armor {

	{
		image = ItemSpriteSheet.CERAMICS_ARMOR;
	}

	public CeramicsArmor() {
		super( 3 );
	}

	@Override
	public int DRMax(int lvl) {
		//+3 base defense, but upgrades give 1 less max defense than usual
		return Math.max(super.DRMax(lvl) + 3 - lvl, lvl);
	}

	//fired clay: mild insulation, and ceramic holds its temperature strongly
	@Override
	public float thermalOffset() {
		return super.thermalOffset() + 0.5f;
	}

	@Override
	public float thermalMass() {
		return 0.55f;
	}
}
