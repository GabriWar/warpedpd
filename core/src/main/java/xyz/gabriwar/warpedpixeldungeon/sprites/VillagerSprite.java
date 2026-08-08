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

import com.watabou.noosa.TextureFilm;

/**
 * Overworld villager: one of three civilian looks (hooded thief, robed mage,
 * mushroom kid), each in eight colour variants generated from the base sheets.
 */
public class VillagerSprite extends MobSprite {

	public static final int THIEF  = 0;
	public static final int MAGE   = 1;
	public static final int SHROOM = 2;

	public VillagerSprite(){
		this( THIEF, 0 );
	}

	public VillagerSprite( int type, int color ){
		super();
		type = Math.floorMod( type, 3 );
		color = Math.floorMod( color, 8 );

		switch (type){
			case THIEF: default: {
				texture( "sprites/villager_thief_" + color + ".png" );
				TextureFilm film = new TextureFilm( texture, 12, 13 );
				idle = new Animation( 1, true );
				idle.frames( film, 0, 0, 0, 1, 0, 0, 0, 0, 1 );
				run = new Animation( 15, true );
				run.frames( film, 0, 0, 2, 3, 3, 4 );
				die = new Animation( 10, false );
				die.frames( film, 5, 6, 7, 8, 9 );
				attack = new Animation( 12, false );
				attack.frames( film, 10, 11, 12, 0 );
				break;
			}
			case MAGE: {
				texture( "sprites/villager_mage_" + color + ".png" );
				TextureFilm film = new TextureFilm( texture, 12, 14 );
				idle = new Animation( 10, true );
				idle.frames( film, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 3, 3, 3, 3, 3, 2, 1 );
				run = new Animation( 20, true );
				run.frames( film, 0 );
				die = new Animation( 20, false );
				die.frames( film, 0 );
				attack = idle.clone();
				break;
			}
			case SHROOM: {
				texture( "sprites/villager_shroom_" + color + ".png" );
				TextureFilm film = new TextureFilm( texture, 12, 14 );
				idle = new Animation( 10, true );
				idle.frames( film, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 3, 3, 3, 3, 3, 2, 1 );
				run = new Animation( 20, true );
				run.frames( film, 0 );
				die = new Animation( 20, false );
				die.frames( film, 0 );
				attack = idle.clone();
				break;
			}
		}

		play( idle );
	}
}
