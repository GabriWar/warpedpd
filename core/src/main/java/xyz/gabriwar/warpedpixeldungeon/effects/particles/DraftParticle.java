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

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * A streak of moving air in The Bellows: a pale line, drawn out along the wind, that
 * crosses a tile or two in the direction the draught blows and fades at both ends.
 */
public class DraftParticle extends PixelParticle {

	//a gust across a ridge of the heights (levels/overworld/HazardWatch): the same streaks, blowing
	//north, east, south or west, but mostly slate (the ridges are snow, where the Bellows' pale air
	//cannot be seen) with a pale one in three for the drop's black
	public static final Emitter.Factory[] GUSTS = new Emitter.Factory[4];
	private static final int AIR = 0xDDEEF2, SLATE = 0x5A6E86, SLATE_LIGHT = 0x7A8DA6;
	static {
		final int[] gx = { 0, 1, 0, -1 }, gy = { -1, 0, 1, 0 };
		for (int i = 0; i < 4; i++){
			final int dx = gx[i], dy = gy[i];
			GUSTS[i] = new Emitter.Factory(){
				@Override
				public void emit( Emitter emitter, int index, float x, float y ){
					((DraftParticle) emitter.recycle( DraftParticle.class )).reset( x, y, dx, dy,
							index % 3 == 2 ? AIR : (index % 2 == 0 ? SLATE : SLATE_LIGHT) );
				}
			};
		}
	}

	public DraftParticle(){
		super();
		color( AIR );
		lifespan = 0.55f;
	}

	public void reset( float x, float y, int dx, int dy ){
		reset( x, y, dx, dy, AIR );
	}

	public void reset( float x, float y, int dx, int dy, int color ){
		revive();
		//a recycled streak may have been a gust's
		color( color );
		left = lifespan = Random.Float( 0.5f, 0.9f );
		this.x = x;
		this.y = y;
		float pace = Random.Float( 30f, 48f );
		speed.set( dx * pace, dy * pace );
		//drawn out along the wind: a line of air, four to seven pixels long and one thick
		float length = Random.Float( 4f, 7f );
		scale.set( dx != 0 ? length : 1f, dy != 0 ? length : 1f );
		am = 0;
	}

	@Override
	public void update(){
		super.update();
		float p = left / lifespan;
		am = (p < 0.5f ? p : 1f - p) * 1.6f;
	}
}
