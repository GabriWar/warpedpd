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

/**
 * A firefly: a bright heart in a soft glow, drawn additively, that wanders on a
 * slow meandering path and lights in pulses, a flash or two and then dark for a
 * while, the way real ones signal.
 */
public class FireflyParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((FireflyParticle) emitter.recycle(FireflyParticle.class)).reset(x, y);
		}

		@Override
		public boolean lightMode() {
			return true;
		}
	};

	private float heading, pace, bobPhase;
	private float pulse, pulseLeft, dark;
	private int flashes;

	public FireflyParticle() {
		super();
		lifespan = Random.Float(5f, 10f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(5f, 10f);

		frame(Random.Float() < 0.75f ? WeatherSprites.GLOW_5 : WeatherSprites.GLOW_3);
		color(Random.Float() < 0.7f ? 0xF8F060 : 0xC8FF60);

		heading = Random.Float((float)(Math.PI * 2));
		pace = Random.Float(3f, 7f);
		bobPhase = Random.Float((float)(Math.PI * 2));
		acc.set(0, 0);
		//start somewhere in its cycle so a fresh swarm does not blink in step
		dark = Random.Float(0f, 1.5f);
		pulseLeft = 0;
		flashes = 0;
		am = 0;
	}

	@Override
	public void update() {
		super.update();
		float dt = Game.elapsed;

		//the path: a heading that drifts, a gentle bob, the wind leaned into
		heading += Random.Float(-1f, 1f) * 2.2f * dt;
		bobPhase += dt * 2f;
		float wind = ClimateManager.localWindSpeed() * 0.25f;
		speed.set((float) Math.cos(heading) * pace + wind, (float) Math.sin(heading) * pace * 0.6f + (float) Math.sin(bobPhase) * 2.5f);

		//the light: a pulse of a third of a second, one to three of them, then dark
		float glow;
		if (pulseLeft > 0) {
			pulseLeft -= dt;
			float t = 1f - pulseLeft / pulse;
			glow = (float) Math.sin(t * Math.PI);
			if (pulseLeft <= 0) {
				if (--flashes > 0) { pulse = Random.Float(0.25f, 0.4f); pulseLeft = pulse; dark = 0; }
				else dark = Random.Float(0.9f, 2.6f);
			}
		} else {
			dark -= dt;
			glow = 0;
			if (dark <= 0) {
				flashes = Random.Float() < 0.6f ? 1 : (Random.Float() < 0.7f ? 2 : 3);
				pulse = Random.Float(0.3f, 0.5f);
				pulseLeft = pulse;
			}
		}
		am = glow * envelope(0.1f, 0.15f, 0.95f);
		fov();
	}
}
