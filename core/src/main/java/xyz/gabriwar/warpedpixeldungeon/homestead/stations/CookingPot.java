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

import xyz.gabriwar.warpedpixeldungeon.homestead.rooms.RoomTypes;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.sprites.StationSprite;

/**
 * The cooking pot: a cauldron on a tripod over embers that keep themselves. It stews meat, cooks
 * a blandfruit with a seed and bakes a meat pie: the alchemy pot's food, made without alchemical
 * energy (StationRecipes). It steams while it cooks.
 */
public class CookingPot extends HomesteadStation {

	{
		spriteClass = StationSprite.CookingPot.class;
	}

	@Override
	public int frame(){
		return HomesteadSprites.COOKING_POT;
	}

	//a meal cooked in a kitchen keeps the hero well fed for longer (rooms/RoomTypes.cookedAt): each
	//food the pot makes is told where it was cooked
	@Override
	protected void made( OverworldLevel level, Item item ){
		if (item instanceof Food) RoomTypes.cookedAt( (Food) item, level, pos );
	}
}
