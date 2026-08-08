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

//frames taken from Remixed PD's spritesDesc/IceGuardian.json
public class IceGuardianSprite extends MobSprite {

	public IceGuardianSprite() {
		super();

		texture( Assets.Sprites.RM_ICE_GUARDIAN );
		TextureFilm frames = new TextureFilm( texture, 22, 20 );

		idle = new Animation( 4, true );
		idle.frames( frames, 0, 1, 2, 3 );

		run = new Animation( 6, true );
		run.frames( frames, 15, 16, 17, 18, 19, 20 );

		attack = new Animation( 10, false );
		attack.frames( frames, 4, 5, 6, 7 );

		die = new Animation( 6, false );
		die.frames( frames, 8, 9, 10, 11, 12, 13, 14 );

		play( idle );
	}
}
