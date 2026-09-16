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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.Group;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** Where a drop lands: a crown of water that opens and is gone in a blink, or on
 *  water a ring that spreads. */
public class SplashParticle extends WeatherParticle {

	private static final float LIFE = 0.28f;

	private int shown = -1;
	private float peak;
	private int[][] frames;

	/** a splash at a landing point, in the drop's colour, if the group has room */
	public static void splash( Group in, float x, float y, int color, float alpha ) {
		if (in == null) return;
		SplashParticle s = (SplashParticle) in.recycle( SplashParticle.class );
		s.revive();
		s.x = x;
		s.y = y;
		s.color( color );
		s.peak = alpha;
		s.speed.set( 0, 0 );
		s.acc.set( 0, 0 );
		s.frames = onWater( x, y ) ? WeatherSprites.RIPPLE : WeatherSprites.SPLASH;
		s.left = s.lifespan = s.frames == WeatherSprites.RIPPLE ? LIFE * 1.6f : LIFE;
		s.shown = -1;
		s.show( 0 );
	}

	private static boolean onWater( float x, float y ) {
		if (Dungeon.level == null || Dungeon.level.water == null || x < 0 || y < 0) return false;
		int cx = (int)(x / DungeonTilemap.SIZE), cy = (int)(y / DungeonTilemap.SIZE);
		if (cx >= Dungeon.level.width() || cy >= Dungeon.level.height()) return false;
		return Dungeon.level.water[cx + cy * Dungeon.level.width()];
	}

	private void show( int f ) {
		if (f != shown) {
			shown = f;
			frame( frames[f] );
			//the crown stands on the landing point; the ring lies round it
			if (frames == WeatherSprites.RIPPLE) origin.set( frame[2] / 2f, frame[3] / 2f );
			else origin.set( frame[2] / 2f, frame[3] );
		}
	}

	@Override
	public void update() {
		super.update();
		float p = 1f - left / lifespan;
		show( p < 0.35f ? 0 : (p < 0.7f ? 1 : 2) );
		am = peak * (1f - p * 0.6f);
		fov();
	}
}
