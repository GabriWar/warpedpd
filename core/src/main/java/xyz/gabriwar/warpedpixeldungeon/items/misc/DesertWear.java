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

package xyz.gabriwar.warpedpixeldungeon.items.misc;

import xyz.gabriwar.warpedpixeldungeon.items.Item;

/**
 * What keeps a sandstorm off a hero (actors/Sandstorm): sand goggles or a desert scarf, carried.
 * Pulled on when the sand comes, so there is nothing to put on or forget: one in the pack and the
 * storm neither blinds nor slows him, and he sees as far as he would without it. Sold at the oases
 * (OasisTrader) and made at a homestead's workbench (homestead/stations/StationRecipes).
 */
public abstract class DesertWear extends Item {

	{
		unique = true;
		bones = false;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}
}
