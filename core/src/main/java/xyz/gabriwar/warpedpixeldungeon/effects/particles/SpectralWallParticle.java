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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.GameMath;

/**
 * The skeleton key's spectral wall: bricks of the region's stone fading in and shrinking away,
 * each a whole number of pixels on a side as it shrinks (never a fraction), shimmering faintly
 * (+-12% at about 1 Hz). The key's wall pours them every 0.15 s, about 20 a cell. Its randomness is
 * the effects' own.
 */
public class SpectralWallParticle extends PixelParticle {

	/** The shimmer: its depth and speed (rad/s). */
	public static final float SHIMMER = 0.12f, SHIMMER_RATE = 6f;

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			//scale frequency roughly to the size of the bricks used
			int type = 1 + (Dungeon.depth-1)/5;
			type = (int)GameMath.gate(1, type, 5);

			//some regions use fewer particles because they are bigger
			switch (type){
				case 2:
					//3/5 chance for a particle in prison
					if (FxRandom.Int(5) >= 3) return;
					break;
				case 5:
					//2/5 chance for a particle in halls
					if (FxRandom.Int(5) >= 2) return;
					break;
			}

			((SpectralWallParticle)emitter.recycle( SpectralWallParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return false;
		}
	};

	private int type = 0; //1-5 for sewers - demon halls
	private float xScale = 0, yScale = 0;
	private float phase;

	public SpectralWallParticle() {
		super();

		lifespan = 3f;

		am = 0.6f;
	}

	public void reset( float x, float y ) {
		revive();

		type = 1 + (Dungeon.depth-1)/5;
		type = (int)GameMath.gate(1, type, 5);

		this.x = x;
		this.y = y;

		left = lifespan;
		phase = FxRandom.angle();

		switch (type){
			case 1:
				this.x = Math.round(x/7)*7;
				this.y = Math.round(y/4)*4 - 6;
				this.x += Math.round(this.y % 8)/4f - 1;
				color(Ramp.lerp(0xD4D4D4, 0xABABAB, FxRandom.Float()));
				xScale = 6;
				yScale = 3;
				break;
			case 2:
				this.x = Math.round(x/7)*7;
				this.y = Math.round(y/6)*6 - 6;
				this.x += Math.round(this.y % 8)/4f - 1;
				color(Ramp.lerp(0xc4be9c, 0x9c927d, FxRandom.Float()));
				xScale = 6;
				yScale = 5;
				break;
			case 3:
				this.y -= 6;
				float colorScale = (this.x%16)/16 + (this.y%16)/16;
				if (colorScale > 1f) colorScale = 2f - colorScale;
				color(Ramp.lerp(0xb7b0a5, 0x6a6662, colorScale));
				xScale = 5;
				yScale = 5;
				break;
			case 4:
				this.x = Math.round(x/5)*5;
				this.y = Math.round(y/5)*5 - 6;
				this.x += Math.round(this.y % 16)/5f - 2;
				color(Ramp.lerp(0xd0bca3, 0xa38d81, FxRandom.Float()));
				xScale = 4;
				yScale = 4;
				break;
			case 5:
				this.x = Math.round(x/4)*4;
				this.y = Math.round((y+8)/16)*16 - 14;
				color(Ramp.lerp(0xa2947d, 0x594847, FxRandom.Float()));
				xScale = 4;
				yScale = 20;
				break;
		}
	}

	public void update() {
		super.update();
		float k = left / lifespan;
		//whole pixels as it shrinks
		int w = Math.round( k * xScale ), h = Math.round( k * yScale );
		visible = w > 0 && h > 0;
		scale.set( w, h );
		phase += Game.elapsed * SHIMMER_RATE;
		am = 0.6f * (1f - SHIMMER + SHIMMER * (float)Math.sin( phase ));
	}

}
