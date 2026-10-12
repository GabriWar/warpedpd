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
 * The forge: a brick hearth under a hood, with bellows. Lit, it works iron ore into fittings and,
 * when the woodcutter's and the mason's tools exist, forges them (StationRecipes). It never makes a
 * masterwork core: the smith's one crucible a day stays the only way to one (StationRecipes
 * refuses any recipe that would).
 */
public class Forge extends HeatStation {

	{
		spriteClass = StationSprite.Forge.class;
	}

	/** What a lit forge gives its room, C. */
	public static final float ROOM_HEAT = 8f;

	@Override
	public int frame(){
		return HomesteadSprites.FORGE;
	}

	@Override
	protected float cellHeat(){
		return 14f;
	}

	@Override
	protected float radiate(){
		return 5f;
	}

	@Override
	protected float litRoomHeat(){
		return ROOM_HEAT;
	}
}
