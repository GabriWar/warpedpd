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

/**
 * The hoard's squirrel (tools/warped_rooms_art.py, 16x16): russet, with the tail doing
 * most of the talking. 0-1 sitting up, 2-5 bounding, 6-7 biting, 8-10 dying, 11-12
 * curled up asleep with the tail over its nose.
 */
public class HoardSquirrelSprite extends MobSprite {

	private final Animation asleep;

	public HoardSquirrelSprite(){
		super();

		texture( Assets.Sprites.HOARD_SQUIRREL );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 3, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 1, 1 );

		run = new Animation( 14, true );
		run.frames( frames, 2, 3, 4, 5 );

		attack = new Animation( 12, false );
		attack.frames( frames, 6, 7, 0 );

		die = new Animation( 10, false );
		die.frames( frames, 8, 9, 10 );

		asleep = new Animation( 1, true );
		asleep.frames( frames, 11, 11, 12, 12 );

		play( idle );
	}

	//asleep it is a ball of fur, not a squirrel standing with a Z over it
	@Override
	public void idle(){
		if (ch != null && ch.isAlive()
				&& ch instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob
				&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) ch).state
					== ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) ch).SLEEPING){
			play( asleep );
		} else {
			super.idle();
		}
	}

	@Override
	public void link( xyz.gabriwar.warpedpixeldungeon.actors.Char ch ){
		super.link( ch );
		idle();
	}

	@Override
	public int blood(){
		return 0xFFB5522A;
	}
}
