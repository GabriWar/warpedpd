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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

/**
 * A hearthstone (homestead/travel/Hearthstone): a low round stone whose carved rune lies dormant by
 * day and glows warm after dark - at night, and always down in the caves, where no daylight comes
 * (HEARTHSTONE_LIT, with a small warm light over it).
 */
public class HearthstoneSprite extends GlowStoneSprite {

	/** The warm light of its rune. */
	public static final int GLOW = 0xFFB565;

	public HearthstoneSprite(){
		super( HomesteadSprites.HEARTHSTONE, HomesteadSprites.HEARTHSTONE_LIT, 1, 1 );
	}

	@Override
	protected boolean lit(){
		if (Dungeon.level instanceof OverworldLevel && !((OverworldLevel) Dungeon.level).openSky()) return true;
		return DayNightCycle.isNight();
	}

	@Override
	protected int glowColor(){
		return GLOW;
	}

	@Override
	protected int glowSize(){
		return 11;
	}

	@Override
	protected float glowLevel(){
		return 0.5f;
	}
}
