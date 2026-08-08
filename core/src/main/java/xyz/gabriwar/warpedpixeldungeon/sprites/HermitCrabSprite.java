/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

public class HermitCrabSprite extends MobSprite {

	public HermitCrabSprite() {
		super();

		texture( Assets.Sprites.CRAB );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		int c = 16;

		idle = new Animation( 5, true );
		idle.frames( frames, 0+c, 1+c, 0+c, 2+c );

		run = new Animation( 10, true );
		run.frames( frames, 3+c, 4+c, 5+c, 6+c );

		attack = new Animation( 12, false );
		attack.frames( frames, 7+c, 8+c, 9+c );

		zap = attack.clone();

		die = new Animation( 12, false );
		die.frames( frames, 10+c, 11+c, 12+c, 13+c );

		play( idle );
	}

	public void zap( int pos ) {
		Char enemy = Actor.findChar(pos);

		PointF origin = center();
		if (enemy != null) {
			parent.add(new Lightning(origin, enemy.sprite.destinationCenter(), (Callback) ch));
		} else {
			parent.add(new Lightning(origin, pos, (Callback) ch));
		}
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );

		super.zap( ch.pos );
		flash();
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) {
			idle();
		}
		super.onComplete( anim );
	}

	@Override
	public int blood() {
		return 0xFFFFEA80;
	}

}
