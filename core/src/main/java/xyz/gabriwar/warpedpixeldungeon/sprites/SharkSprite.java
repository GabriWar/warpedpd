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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterDrops;
import com.watabou.noosa.TextureFilm;

/** A shark of the open sea (actors/mobs/Shark), 16x16 (tools/assets/sea_art.py): its fin cutting the
 *  water with a small bow wave as it cruises (four frames, its wake trailing), quicker as it closes in;
 *  the bite breaks the surface, jaws open; dead, it rolls belly up and sinks. */
public class SharkSprite extends MobSprite {

	//the cell it last lunged at: the spray flies that way
	private int bitten = -1;

	public SharkSprite(){
		super();

		renderShadow = false;
		perspectiveRaise = 0.2f;

		texture( "sprites/shark.png" );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 5, true );
		idle.frames( frames, 0, 1, 2, 3 );

		run = new Animation( 10, true );
		run.frames( frames, 4, 5, 6, 7 );

		attack = new Animation( 14, false );
		attack.frames( frames, 8, 9, 10, 11 );

		die = new Animation( 6, false );
		die.frames( frames, 12, 13, 14 );

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
		if (anim == attack){
			//out of the water and back in: a crown, spray flung at what it bit, a ring
			WaterDrops.lunge( ch.pos, bitten );
		}
	}
}
