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

/** A tie post (homestead/animals/TiePost), a short post with an iron ring: one still frame of
 *  sprites/homestead.png. */
public class TiePostSprite extends MobSprite {

	public TiePostSprite(){
		super();

		texture( HomesteadSprites.TEXTURE );
		TextureFilm film = HomesteadSprites.film();

		idle = new Animation( 1, true );
		idle.frames( film, HomesteadSprites.TIE_POST );
		//never the idle instance itself: a looped idle that IS attack fires onAttackComplete every
		//cycle (see TemperingAnvilSprite)
		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	@Override
	public int blood(){
		return 0xFF7A5A36;
	}
}
