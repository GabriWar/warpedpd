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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//the crab king's minion keeps Sprouted's shelled crab, so it can't be mistaken for the sewer hermit crab
public class ShellCrabSprite extends MobSprite {

	public ShellCrabSprite() {
		super();

		texture( Assets.Sprites.SHELL_CRAB );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 1, 1 );

		run = new Animation( 12, true );
		run.frames( frames, 2, 3, 4, 5, 6 );

		attack = new Animation( 12, false );
		attack.frames( frames, 6, 7, 8, 9 );

		zap = attack.clone();

		die = new Animation( 12, false );
		die.frames( frames, 10, 11, 12, 13 );

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
