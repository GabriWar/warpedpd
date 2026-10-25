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
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import com.watabou.noosa.TextureFilm;

/** A camel of the desert (tools/ow_fauna_gen.py, 18x18 frames): stand, chewing, two of the stride,
 *  the head thrown back and the spit, the fall onto its knees and the carcass. Its one blow is a
 *  spit, near or far: the glob flies from its mouth (a missile, which a co-op guest is sent as the
 *  host throws it) and the blow lands when it arrives (Camel.attackProc). */
public class CamelSprite extends WildSprite {

	//the cell the spit is aimed at
	private int spitAt;

	public CamelSprite(){
		super();
		texture( Assets.Sprites.CAMEL );
		TextureFilm frames = new TextureFilm( texture, 18, 18 );

		//chewing the cud
		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 1, 0, 0 );

		run = new Animation( 8, true );
		run.frames( frames, 2, 0, 3, 0 );

		//the head thrown back, then thrust out
		zap = new Animation( 12, false );
		zap.frames( frames, 4, 4, 5, 5 );
		attack = zap.clone();

		die = new Animation( 10, false );
		die.frames( frames, 6, 7 );

		play( idle );
	}

	@Override
	public void attack( int cell ){
		spitAt = cell;
		zap( cell );
	}

	@Override
	public void onComplete( Animation anim ){
		if (anim == zap){
			idle();
			//a guest is sent the glob as the host throws it: it throws none of its own
			if (NetManager.isNetClient()) return;
			if (parent == null){
				ch.onAttackComplete();
				return;
			}
			((MissileSprite) parent.recycle( MissileSprite.class )).reset( this, spitAt, new Spit(), () -> ch.onAttackComplete() );
		} else {
			super.onComplete( anim );
		}
	}

	/** The glob in flight (no item: nothing of it lands). */
	public static class Spit extends Item {
		{
			image = ItemSpriteSheet.CAMEL_SPIT;
		}
	}
}
