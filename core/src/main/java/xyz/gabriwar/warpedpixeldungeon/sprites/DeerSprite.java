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

/** A deer of the surface (tools/deer_gen.py): the stag, row 0 of the sheet; the doe, row 1, is
 *  DoeSprite - a class of its own, so a co-op guest, who rebuilds a sprite from its class alone,
 *  sees the same deer the host does. Frames: stand, graze, three strides of the run, the fall,
 *  the carcass. */
public class DeerSprite extends MobSprite {

	public DeerSprite(){
		this( false );
	}

	protected DeerSprite( boolean doe ){
		super();
		texture( Assets.Sprites.DEER );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );
		//row 1 of the sheet is the doe
		int o = doe ? 7 : 0;

		//head down to graze now and then
		idle = new Animation( 2, true );
		idle.frames( frames, o, o, o, o, o + 1, o + 1, o + 1, o );

		//gathered, thrown out, the bound, thrown out again
		run = new Animation( 14, true );
		run.frames( frames, o + 2, o + 3, o + 4, o + 3 );

		//its own object: a startled rear, never the looped idle (an attack that never ends)
		attack = new Animation( 12, false );
		attack.frames( frames, o, o + 2, o );

		die = new Animation( 10, false );
		die.frames( frames, o + 5, o + 6 );

		play( idle );
	}
}
