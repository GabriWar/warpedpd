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

import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/** Ground mist: long soft wisps that creep along low, swelling as they go. */
public class MistParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((MistParticle) emitter.recycle(MistParticle.class)).reset(x, y);
		}
	};

	private float driftPhase;
	private boolean swollen;

	public MistParticle() {
		super();
		color(0xDDDDDD);
		lifespan = Random.Float(4f, 8f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(4f, 8f);
		swollen = false;
		frame(Random.Float() < 0.5f ? WeatherSprites.WISP_L : WeatherSprites.WISP_XL);
		color(Random.Float() < 0.5f ? 0xDDDDE4 : 0xC8D0D8);

		// Mist drifts mostly horizontally, pushed by wind
		float wind = ClimateManager.localWindSpeed();
		speed.set(Random.Float(-2, 2) + wind * 0.5f, Random.Float(-1, 2));
		acc.set(0, 0);
		driftPhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		// Slow undulating drift
		driftPhase += Game.elapsed * 0.8f;
		speed.y = (float) Math.sin(driftPhase) * 1.5f;
		//it spreads: a small wisp becomes a long one halfway through
		if (!swollen && p < 0.55f && frame == WeatherSprites.WISP_L) {
			swollen = true;
			frame(WeatherSprites.WISP_XL);
		}
		am = envelope(0.2f, 0.2f, 0.3f);
		fov();
	}
}
