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
import com.watabou.noosa.TextureFilm;

/** The bar's roulette table: its little wheel keeps turning (tools/roulette_table.py). */
public class RouletteTableSprite extends MobSprite {

	public RouletteTableSprite(){
		super();

		texture( Assets.Sprites.ROULETTE_TABLE );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 6, true );
		idle.frames( frames, 0, 1, 2, 3 );

		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}
}
