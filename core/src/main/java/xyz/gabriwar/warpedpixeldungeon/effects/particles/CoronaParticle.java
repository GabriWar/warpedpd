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

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/** The motes of an eclipse's corona: pale gold glows that hang and pulse. */
public class CoronaParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((CoronaParticle) emitter.recycle(CoronaParticle.class)).reset(x, y);
		}

		@Override
		public boolean lightMode() {
			return true; // additive blending for glow
		}
	};

	private float pulsePhase;

	public CoronaParticle() {
		super();
		lifespan = Random.Float(2f, 4f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(2f, 4f);
		frame(Random.Float() < 0.6f ? WeatherSprites.GLOW_5 : WeatherSprites.CORONA);

		// White-gold glow
		color(Random.Float() < 0.6f ? 0xFFEECC : 0xFFFFDD);

		// Very slow drift
		speed.set(Random.Float(-1, 1), Random.Float(-1, 1));
		acc.set(0, 0);
		pulsePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		pulsePhase += Game.elapsed * 2f;
		float pulse = ((float) Math.sin(pulsePhase) + 1f) * 0.5f;
		am = envelope(0.2f, 0.2f, 0.7f) * (0.4f + 0.6f * pulse);
		fov();
	}
}
