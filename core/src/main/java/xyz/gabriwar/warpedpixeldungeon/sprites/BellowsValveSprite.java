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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BellowsValve;
import com.watabou.noosa.TextureFilm;

/**
 * The Bellows' valve (tools/warped_rooms_art.py, 16x16): a riveted tank on saddles with
 * a brass wheel on its shoulder, its gauge in the green; and the same with the wheel
 * turned, the gauge dark and a wisp leaking from the union.
 */
public class BellowsValveSprite extends MobSprite {

	private final Animation ready, open;

	public BellowsValveSprite(){
		super();

		texture( Assets.Sprites.BELLOWS_VALVE );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		ready = new Animation( 1, true );
		ready.frames( frames, 0 );
		open = new Animation( 1, true );
		open.frames( frames, 1 );

		idle = ready;
		run = ready.clone();
		attack = ready.clone();
		die = ready.clone();

		play( idle );
	}

	public void show( boolean spent ){
		idle = spent ? open : ready;
		play( idle, true );
	}

	@Override
	public void link( Char ch ){
		super.link( ch );
		if (ch instanceof BellowsValve) show( ((BellowsValve) ch).spent() );
	}

	@Override
	public int blood(){
		return 0xFFC99A3E;
	}
}
