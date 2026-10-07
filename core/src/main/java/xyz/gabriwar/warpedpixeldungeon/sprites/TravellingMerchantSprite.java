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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant;
import com.watabou.noosa.TextureFilm;

/** A travelling market's trader (tools/travelling_merchant_gen.py): the shopkeeper's two frames
 *  in a turban and a robe, one row per speciality - violet curios (this one), teal relics
 *  (RelicMerchantSprite), crimson exotics (ExoticMerchantSprite). A class per speciality, so a
 *  co-op guest, who rebuilds a sprite from its class alone, sees the same trader as the host. */
public class TravellingMerchantSprite extends ShopkeeperSprite {

	public TravellingMerchantSprite(){
		this( TravellingMerchant.CURIOS );
	}

	protected TravellingMerchantSprite( int speciality ){
		super();
		int f = 2 * Math.floorMod( speciality, TravellingMerchant.SPECIALITIES );
		texture( Assets.Sprites.TRAVELLING_MERCHANT );
		TextureFilm film = new TextureFilm( texture, 14, 14 );

		idle = new Animation( 10, true );
		idle.frames( film, f + 1, f + 1, f + 1, f + 1, f + 1, f, f, f, f );

		die = new Animation( 20, false );
		die.frames( film, f );

		//their own objects: an attack or a death aliased to the looped idle never completes
		run = idle.clone();
		attack = idle.clone();

		//texture() reset the frame to the whole sheet: start the idle again so it crops
		play( idle, true );
	}
}
