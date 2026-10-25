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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * Earthworms: a fishing rod's plainest bait (Bait.WORM), the one most fish of the still waters take.
 * Turned up by the shovel tilling grass or bare earth (homestead/gathering/Gathering, more of them
 * in the rain) and sold by the villages' fishers (FisherTrade).
 */
public class Earthworm extends Item {

	/** The chance a spadeful of tilled ground turns worms up, and in the rain; how many. */
	public static final float CHANCE = 0.3f, RAIN_CHANCE = 0.6f;
	public static final int MIN = 1, MAX = 3;

	{
		image = ItemSpriteSheet.EARTHWORMS;
		stackable = true;
		bones = false;
	}

	/** The worms a spadeful of grass or bare earth turns up as it is tilled (Gathering.dug), or null. */
	public static Earthworm turnedUp(){
		if (Random.Float() >= (ClimateManager.isRaining() ? RAIN_CHANCE : CHANCE)) return null;
		return (Earthworm) new Earthworm().quantity( Random.IntRange( MIN, MAX ) );
	}

	@Override
	public boolean collect( Bag container ){
		if (!super.collect( container )) return false;
		Catching.carried( this, container );
		return true;
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	@Override
	public int value(){
		return quantity;
	}
}
