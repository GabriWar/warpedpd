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

import com.watabou.noosa.TextureFilm;

/** A village's festival champion (actors/mobs/npcs/FestivalChampion, tools/assets/events_life_art.py):
 *  the watchman's frames out of his mail, in a wrestler's crimson tunic with a gold sash, bare arms
 *  and a laurel in his hair. He yields, never falls: his last frames are a bow. */
public class FestivalChampionSprite extends MobSprite {

	public static final String TEXTURE = "sprites/festival_champion.png";

	public FestivalChampionSprite(){
		super();
		texture( TEXTURE );
		TextureFilm frames = new TextureFilm( texture, 12, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 1, 1 );

		run = new Animation( 15, true );
		run.frames( frames, 2, 3, 4, 5, 6, 7 );

		attack = new Animation( 12, false );
		attack.frames( frames, 8, 9, 10 );

		die = new Animation( 8, false );
		die.frames( frames, 11, 12, 13, 14 );

		play( idle, true );
	}
}
