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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock;
import com.watabou.noosa.TextureFilm;

/**
 * An ice block with something in it (tools/warped_rooms_art.py, 16x16). Frames 0-8 are
 * three silhouettes (long, round, small) at three stages of thaw, shape * 3 + stage;
 * 9-11 are the block going to pieces.
 */
public class IceBlockSprite extends MobSprite {

	private final TextureFilm frames;

	public IceBlockSprite(){
		super();

		texture( Assets.Sprites.ICE_BLOCK );
		frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 1, true );
		idle.frames( frames, 0 );

		run = idle.clone();
		attack = idle.clone();

		die = new Animation( 10, false );
		die.frames( frames, 9, 10, 11 );

		play( idle );
	}

	public void show( int shape, int stage ){
		idle = new Animation( 1, true );
		idle.frames( frames, shape * 3 + stage );
		play( idle, true );
	}

	@Override
	public void link( Char ch ){
		super.link( ch );
		if (ch instanceof IceBlock) show( ((IceBlock) ch).shape(), ((IceBlock) ch).stage() );
	}

	@Override
	public int blood(){
		return 0xFFBFE6F5;
	}
}
