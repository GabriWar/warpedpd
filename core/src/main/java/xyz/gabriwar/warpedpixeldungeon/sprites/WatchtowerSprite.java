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
 * A homestead's watchtower (sprites/homestead.png, HomesteadSprites.WATCHTOWER): its one 16x32
 * frame, bottom-aligned on its cell (CharSprite.worldToCamera), so its top half stands over the
 * cell above.
 */
public class WatchtowerSprite extends MobSprite {

	public WatchtowerSprite(){
		super();

		texture( HomesteadSprites.TEXTURE );
		TextureFilm film = HomesteadSprites.film();

		idle = new Animation( 1, true );
		idle.frames( film, HomesteadSprites.WATCHTOWER );

		//never the idle instance itself (see TemperingAnvilSprite)
		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	@Override
	public int blood(){
		return 0xFF8A6B4A;
	}
}
