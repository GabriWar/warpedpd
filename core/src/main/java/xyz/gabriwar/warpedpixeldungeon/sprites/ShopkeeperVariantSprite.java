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

/** A colour variant of the shopkeeper sheet. */
public class ShopkeeperVariantSprite extends ShopkeeperSprite {

	public ShopkeeperVariantSprite(){
		this( 0 );
	}

	public ShopkeeperVariantSprite( int color ){
		super();
		color = Math.floorMod( color, 6 );   //heals any bad tint from old saves
		texture( "sprites/shopkeeper_v" + color + ".png" );
		//Image.texture() resets the frame to the FULL sheet; a single-frame
		//idle never re-applies its crop, leaving the whole spritesheet on
		//screen - force the animation to restart and re-frame immediately
		play( idle, true );
	}
}
