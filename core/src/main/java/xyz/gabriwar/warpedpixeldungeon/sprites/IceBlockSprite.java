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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock;
import com.watabou.noosa.TextureFilm;

/**
 * An ice block with something in it (tools/warped_rooms_art.py, 16x16). Frames 0-8 are
 * three silhouettes (long, round, small) at three stages of thaw, shape * 3 + stage;
 * 9-11 are the block going to pieces. Drawn in the ice of the sheet it stands on: each region's
 * copy is that region's frozen water (tools/wr_art/ice_block_regions.py).
 */
public class IceBlockSprite extends MobSprite {

	private final TextureFilm frames;

	public IceBlockSprite(){
		super();

		texture( ice() );
		frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 1, true );
		idle.frames( frames, 0 );

		run = idle.clone();
		attack = idle.clone();

		die = new Animation( 10, false );
		die.frames( frames, 9, 10, 11 );

		play( idle );
	}

	//the block in the ice of the floor's own frozen water
	private static String ice(){
		String tiles = Dungeon.level == null ? null : Dungeon.level.tilesTex();
		if (Assets.Environment.TILES_SEWERS.equals( tiles )) return Assets.Sprites.ICE_BLOCK_SEWERS;
		if (Assets.Environment.TILES_PRISON.equals( tiles )) return Assets.Sprites.ICE_BLOCK_PRISON;
		if (Assets.Environment.TILES_CAVES.equals( tiles )) return Assets.Sprites.ICE_BLOCK_CAVES;
		if (Assets.Environment.TILES_CITY.equals( tiles )) return Assets.Sprites.ICE_BLOCK_CITY;
		return Assets.Sprites.ICE_BLOCK;
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

	//chips off it: its pale ice (the first frame's frosted top), in this region's copy
	@Override
	public int blood(){
		return texture.getPixel( 3, 1 );
	}

	/** Its meltwater as it thaws away: its body's ice, in this region's copy. */
	public int meltwater(){
		return texture.getPixel( 2, 5 );
	}
}
