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

/** A lake shrine's idol (actors/mobs/npcs/LakeShrine): the caves' dwarf statue cooled to wet
 *  stone, coins on its plinth and a candle; now and then the coins glint and the flame leans
 *  (tools/cave_sites_gen.py). */
public class CaveShrineSprite extends MobSprite {

	public CaveShrineSprite(){
		super();

		texture( Assets.Sprites.CAVE_SHRINE );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 0, 1 );

		run = new Animation( 1, false );
		run.frames( frames, 0 );
		attack = new Animation( 1, false );
		attack.frames( frames, 0 );
		die = new Animation( 1, false );
		die.frames( frames, 0 );

		play( idle );
	}

	@Override
	public int blood(){
		return 0xFF7A8090;
	}
}
