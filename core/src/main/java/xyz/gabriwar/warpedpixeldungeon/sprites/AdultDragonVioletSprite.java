/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AdultDragonViolet;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class AdultDragonVioletSprite extends MobSprite {

	private Animation cast;

	public AdultDragonVioletSprite() {
		super();

		texture( Assets.Sprites.ADULT_DRAGON_VIOLET );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 1, 0, 1, 0, 1, 0 );

		run = new Animation( 6, false );
		run.frames( frames, 0, 2, 1 );

		attack = new Animation( 6, false );
		attack.frames( frames, 0, 2, 3, 4, 5 );

		cast = attack.clone();

		die = new Animation( 8, false );
		die.frames( frames, 0, 5, 6, 7, 8, 9, 9, 9 );

		play( run.clone() );
	}

	@Override
	public void zap( int cell ) {
		turnTo( ch.pos, cell );
		play( cast );

		MagicMissile.boltFromChar( parent, MagicMissile.POISON, this, cell, new Callback() {
			@Override
			public void call() {
				((AdultDragonViolet) ch).onZapComplete();
			}
		} );
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
	}
}
