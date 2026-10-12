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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.sprites.StationSprite;

/**
 * The kiln: a domed clay oven. Lit, it fires clay into bricks, melts sand into glass and chars wood
 * into charcoal (StationRecipes), and warms the room it stands in a little.
 */
public class Kiln extends HeatStation {

	{
		spriteClass = StationSprite.Kiln.class;
	}

	/** What a lit kiln gives its room, C. */
	public static final float ROOM_HEAT = 6f;

	@Override
	public int frame(){
		return HomesteadSprites.KILN;
	}

	@Override
	protected float cellHeat(){
		return 10f;
	}

	@Override
	protected float radiate(){
		return 3f;
	}

	@Override
	protected float litRoomHeat(){
		return ROOM_HEAT;
	}
}
