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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterDrops;
import com.watabou.noosa.TextureFilm;

/** A pike of the rivers (tools/ow_fauna_gen.py, 16x16 frames): lurking, a dark shape under the water
 *  with its eye catching the light; swimming; the strike up out of the water and the jaws shut; belly
 *  up. Low in the water and shadowless, as the piranha is. */
public class PikeSprite extends WildSprite {

	//the cell it last struck at: the spray flies that way
	private int bitten = -1;

	public PikeSprite(){
		super();
		renderShadow = false;
		perspectiveRaise = 0.2f;

		texture( Assets.Sprites.PIKE );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 3, true );
		idle.frames( frames, 0, 1 );

		run = new Animation( 8, true );
		run.frames( frames, 2, 3 );

		attack = new Animation( 12, false );
		attack.frames( frames, 4, 5, 5, 2 );

		die = new Animation( 8, false );
		die.frames( frames, 6, 7 );

		play( idle );
	}

	@Override
	public void link( Char ch ){
		super.link( ch );
		renderShadow = false;
	}

	@Override
	public void attack( int cell ){
		bitten = cell;
		super.attack( cell );
	}

	@Override
	public void onComplete( Animation anim ){
		super.onComplete( anim );
		//out of the water and back: a crown, droplets flung at what it bit, a ring
		if (anim == attack) WaterDrops.lunge( ch.pos, bitten );
	}
}
