/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

public class PlantMobSprite extends MobSprite {

	// Each plant occupies 9 frames in the sheet (idle×2, run×6, die×3), 16×16 each.
	// id corresponds to Plant.image — same values used in OV's LivingPlantSprite.
	public PlantMobSprite( int id ) {
		super();

		int offset = id * 9;

		texture( Assets.Sprites.LIVING_PLANTS );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 5, true );
		idle.frames( frames, offset, offset + 1 );

		run = new Animation( 15, true );
		run.frames( frames, offset, offset + 1, offset + 2, offset + 3, offset + 4, offset + 5 );

		attack = new Animation( 12, false );
		attack.frames( frames, offset, offset + 2, offset + 3, offset + 4, offset );

		zap = attack.clone();

		die = new Animation( 5, false );
		die.frames( frames, offset + 6, offset + 7, offset + 8 );

		play( idle );
	}

	// Default constructor uses row 0 (first plant in the sheet)
	public PlantMobSprite() {
		this( 0 );
	}
}
