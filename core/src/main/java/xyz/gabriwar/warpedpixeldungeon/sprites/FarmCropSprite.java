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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import com.watabou.noosa.TextureFilm;

/**
 * A growing crop: renders its plant's art from the terrain features sheet,
 * translucent while growing and solid once mature.
 */
public class FarmCropSprite extends MobSprite {

	public FarmCropSprite() {
		super();

		texture( Assets.Environment.TERRAIN_FEATURES );

		TextureFilm film = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 1, true );
		idle.frames( film, 0 );

		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		if (ch instanceof FarmCrop){
			TextureFilm film = new TextureFilm( texture, 16, 16 );
			//plant art lives on the plant rows of the features sheet
			int frame = ((FarmCrop) ch).plantImage() + 7*16;
			idle = new Animation( 1, true );
			idle.frames( film, frame );
			run = idle.clone();
			attack = idle.clone();
			die = idle.clone();
			play( idle );
		}
	}

	@Override
	public void update() {
		super.update();
		if (ch instanceof FarmCrop){
			alpha( ((FarmCrop) ch).mature() ? 1f : 0.6f );
		}
	}
}
