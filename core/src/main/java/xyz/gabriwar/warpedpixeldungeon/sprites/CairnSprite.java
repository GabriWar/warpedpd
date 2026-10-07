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

/** A summit's cairn (actors/mobs/npcs/SummitCairn): a pile of stones, with the hero's pale new
 *  one on top once it is added (tools/mountain_sites_art.py). A co-op guest, who rebuilds a
 *  sprite from its class alone, sees the pile as the climbers left it. */
public class CairnSprite extends MobSprite {

	private final TextureFilm film;

	public CairnSprite(){
		super();

		texture( Assets.Sprites.CAIRN );
		film = new TextureFilm( texture, 16, 16 );
		topped( false );
	}

	/** Shows the pile with the hero's stone on it, or without. */
	public void topped( boolean t ){
		int f = t ? 1 : 0;
		idle = new Animation( 1, true );
		idle.frames( film, f );
		run = new Animation( 1, true );
		run.frames( film, f );
		attack = new Animation( 1, false );
		attack.frames( film, f );
		die = new Animation( 1, false );
		die.frames( film, f );
		play( idle );
	}
}
