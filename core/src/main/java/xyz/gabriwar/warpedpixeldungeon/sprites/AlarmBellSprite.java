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

/**
 * A homestead's alarm bell (sprites/homestead.png): hanging still (HomesteadSprites.ALARM_BELL),
 * and rung - swung left, then right, with sound marks (ALARM_BELL_RINGING, two frames at 10 fps,
 * played twice through) before it hangs still again.
 */
public class AlarmBellSprite extends MobSprite {

	private final Animation ringing;

	public AlarmBellSprite(){
		super();

		texture( HomesteadSprites.TEXTURE );
		TextureFilm film = HomesteadSprites.film();

		idle = new Animation( 1, true );
		idle.frames( film, HomesteadSprites.ALARM_BELL );

		int r = HomesteadSprites.ALARM_BELL_RINGING;
		ringing = new Animation( 10, false );
		ringing.frames( film, r, r + 1, r, r + 1 );

		//never the idle instance itself (see TemperingAnvilSprite)
		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	/** Swings the bell: twice to and fro, then still. */
	public void ring(){
		play( ringing, true );
	}

	@Override
	public void onComplete( Animation anim ){
		if (anim == ringing){
			play( idle );
			return;
		}
		super.onComplete( anim );
	}

	@Override
	public int blood(){
		return 0xFF6E6E78;
	}
}
