/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;

public class PitcherPlantVariant3Sprite extends MobSprite {

	private Emitter cloud;

	public PitcherPlantVariant3Sprite() {
		super();

		texture( Assets.Sprites.PITCHER_PLANT );

		TextureFilm frames = new TextureFilm( texture, 16, 15 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1 );

		run = new Animation( 10, true );
		run.frames( frames, 6, 7, 8, 9, 10 );

		attack = new Animation( 15, false );
		attack.frames( frames, 2, 3, 4, 5, 0 );

		die = new Animation( 10, false );
		die.frames( frames, 11, 12, 13, 14 );

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );

		if (cloud == null) {
			cloud = emitter();
			cloud.pour( Speck.factory( Speck.TOXIC ), 0.7f );
		}
	}

	@Override
	public void update() {

		super.update();

		if (cloud != null) {
			cloud.visible = visible;
		}
	}

	@Override
	public void kill() {
		super.kill();

		if (cloud != null) {
			cloud.on = false;
		}
	}
}
