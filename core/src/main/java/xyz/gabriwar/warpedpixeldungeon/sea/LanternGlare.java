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

package xyz.gabriwar.warpedpixeldungeon.sea;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;

/**
 * A lit sea lantern's glare and the bitter smoke of its keeper's oil (items/sea/SeaLantern): while it
 * burns, sharks will not go for its bearer (actors/mobs/Shark.prey). It burns as long as the lantern's
 * light (the Light buff it comes with, whose icon counts it down), so it shows no icon of its own.
 */
public class LanternGlare extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}
}
