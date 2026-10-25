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

/** A festival's cook (actors/mobs/npcs/FestivalCook, tools/assets/events_life_art.py): the village
 *  shopkeeper's two frames in a cook's white cap and apron, the second with the ladle raised. No
 *  coin flicks off his thumb: his trade is a hot meal. */
public class FestivalCookSprite extends MobSprite {

	public static final String TEXTURE = "sprites/festival_cook.png";

	public FestivalCookSprite(){
		super();
		texture( TEXTURE );
		TextureFilm film = new TextureFilm( texture, 14, 14 );

		idle = new Animation( 10, true );
		idle.frames( film, 1, 1, 1, 1, 1, 0, 0, 0, 0 );

		die = new Animation( 20, false );
		die.frames( film, 0 );

		//their own objects: an attack or a death aliased to the looped idle never completes
		run = idle.clone();
		attack = idle.clone();

		//texture() reset the frame to the whole sheet: start the idle again so it crops
		play( idle, true );
	}
}
