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

package xyz.gabriwar.warpedpixeldungeon.items.bombs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * A tomato off a farm's tomato bush (homestead/farming), its fuse already smouldering: thrown, it
 * bursts where it lands with the tomato bush's own blast (Tomatobush.explode) - the cell it lands
 * on and the eight round it, wooden things there set alight. Its damage scales to the depth as the
 * plant's does; on the world's slices that is the deepest floor reached (Dungeon.weatherDepth).
 */
public class FusedTomato extends Item {

	{
		image = ItemSpriteSheet.FUSED_TOMATO;

		defaultAction = AC_THROW;
		usesTargeting = true;

		stackable = true;
		bones = false;
	}

	@Override
	protected void onThrow( int cell ){
		//into a chasm it falls away like anything else
		if (Dungeon.level.pit[cell]){
			super.onThrow( cell );
			return;
		}
		SpatialSound.play( Assets.Sounds.BLAST, cell );
		new Tomatobush().explode( cell, null, Dungeon.weatherDepth( Dungeon.depth ) );
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
		return 15 * quantity;
	}
}
