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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RivalStatue;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import com.watabou.utils.Callback;

/**
 * The Rival's Gallery statue: the ordinary statue cast in dark bronze with gilt edges
 * (tools/warped_rooms_art.py recolours the statue sheet, frame for frame). A marksman's
 * rival looses its shots as a bolt across the room; every other rival swings, even when
 * its reach lets the swing land from two tiles off.
 */
public class RivalStatueSprite extends StatueSprite {

	private Animation shoot;

	public RivalStatueSprite(){
		super();
		//same sheet layout, so the frames the parent cut still fit - but swapping the
		//texture resets the picture to the whole sheet, so the idle frame is put back
		texture( Assets.Sprites.RIVAL_STATUE );
		shoot = attack.clone();
		play( idle, true );
	}

	@Override
	public void attack( int cell ){
		boolean ranged = ch instanceof RivalStatue
				&& ((RivalStatue) ch).perk() == WeaponPerk.SHOT
				&& !Dungeon.level.adjacent( cell, ch.pos );
		if (!ranged){
			super.attack( cell );
			return;
		}
		((MissileSprite) parent.recycle( MissileSprite.class ))
				.reset( this, cell, new Dart(), new Callback(){
					@Override
					public void call(){
						ch.onAttackComplete();
					}
				} );
		play( shoot );
		turnTo( ch.pos, cell );
	}

	@Override
	public int blood(){
		return 0xFFB8935A;
	}
}
