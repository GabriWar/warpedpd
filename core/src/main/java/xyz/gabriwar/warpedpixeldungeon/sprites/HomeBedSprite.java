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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.homestead.rooms.HomeBed;
import com.watabou.noosa.TextureFilm;

/**
 * A bed the hero built (homestead/rooms/HomeBed): one still frame of sprites/homestead.png by its
 * kind - straw, wood or fur (HomesteadSprites.BED_*), head to the north.
 */
public class HomeBedSprite extends MobSprite {

	public HomeBedSprite(){
		super();
		texture( HomesteadSprites.TEXTURE );
		show( HomesteadSprites.BED_STRAW );
	}

	@Override
	public void link( Char ch ){
		super.link( ch );
		if (ch instanceof HomeBed) show( ((HomeBed) ch).kind.frame );
	}

	//a single frame; the other slots are clones, never the idle itself (an aliased looped idle
	//would never finish an attack or a death)
	private void show( int frame ){
		TextureFilm film = HomesteadSprites.film();
		idle = new Animation( 1, true );
		idle.frames( film, frame );
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
