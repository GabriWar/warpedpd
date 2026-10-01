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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil;
import com.watabou.noosa.TextureFilm;

/**
 * The Tempering Forge's anvil (tools/warped_rooms_art.py, 16x16): bare, with a cold bar
 * of metal across it, with the bar glowing and pulsing, and cracked through once its one
 * good temper is spent. The anvil tells the sprite which to show.
 */
public class TemperingAnvilSprite extends MobSprite {

	private final Animation[] faces = new Animation[4];

	public TemperingAnvilSprite(){
		super();

		texture( Assets.Sprites.TEMPERING_ANVIL );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		faces[0] = new Animation( 1, true );
		faces[0].frames( frames, 0 );
		faces[1] = new Animation( 1, true );
		faces[1].frames( frames, 1 );
		faces[2] = new Animation( 5, true );
		faces[2].frames( frames, 2, 3 );
		faces[3] = new Animation( 1, true );
		faces[3].frames( frames, 4 );

		idle = faces[0];
		//never the idle instance itself: a looped idle that IS attack fires
		//onAttackComplete every cycle (see PylonSprite)
		run = faces[0].clone();
		attack = faces[0].clone();
		die = faces[0].clone();

		play( idle );
	}

	public void show( int face ){
		idle = faces[Math.max( 0, Math.min( faces.length - 1, face ) )];
		play( idle, true );
	}

	@Override
	public void link( Char ch ){
		super.link( ch );
		if (ch instanceof TemperingAnvil) show( ((TemperingAnvil) ch).visualState() );
	}

	@Override
	public int blood(){
		return 0xFF6E6E78;
	}
}
