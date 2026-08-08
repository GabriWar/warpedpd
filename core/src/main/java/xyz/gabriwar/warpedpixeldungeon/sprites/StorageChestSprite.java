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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import com.watabou.noosa.TextureFilm;

/**
 * A placed storage chest: renders the matching chest art from the item sheet
 * (wooden, golden or crystal) as a static one-frame "mob".
 */
public class StorageChestSprite extends MobSprite {

	public StorageChestSprite() {
		super();

		texture( Assets.Sprites.ITEMS );

		TextureFilm film = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 1, true );
		idle.frames( film, ItemSpriteSheet.CHEST );

		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		if (ch instanceof StorageChest){
			int frame;
			switch (((StorageChest) ch).type){
				case PortableChest.GOLDEN:  frame = ItemSpriteSheet.LOCKED_CHEST;  break;
				case PortableChest.CRYSTAL: frame = ItemSpriteSheet.CRYSTAL_CHEST; break;
				default:                    frame = ItemSpriteSheet.CHEST;         break;
			}
			TextureFilm film = new TextureFilm( texture, 16, 16 );
			idle = new Animation( 1, true );
			idle.frames( film, frame );
			run = idle.clone();
			attack = idle.clone();
			die = idle.clone();
			play( idle );
		}
	}
}
