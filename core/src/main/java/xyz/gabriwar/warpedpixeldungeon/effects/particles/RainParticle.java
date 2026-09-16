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

import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/**
 * A raindrop: a short streak leaning with the wind, brighter toward its falling
 * end, that breaks into a small splash where it lands. Gusts lean it harder and
 * hurry it down.
 */
public class RainParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((RainParticle) emitter.recycle(RainParticle.class)).reset(x, y);
		}
	};

	private int tint;
	private float peak;
	private boolean splashes;

	public RainParticle() {
		super();
		lifespan = Random.Float(0.6f, 1.0f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(0.6f, 1.0f);

		float rate = ClimateManager.localPrecipRate();
		tint = Random.Float() < 0.35f ? 0xC8DCF4 : 0x9CB8DC;
		color(tint);
		//heavy rain is denser, so each drop can be a touch fainter
		peak = 0.55f + 0.25f * (1f - rate);
		splashes = Random.Float() < 0.35f + 0.25f * rate;

		//wind pushes rain in the actual wind direction, a gust more so
		float wind = ClimateManager.localWindSpeed() * (1f + 0.35f * WeatherSprites.gust());
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float windX = (float) Math.sin(windRad) * wind * 1.2f;
		float windY = -(float) Math.cos(windRad) * wind * 0.35f; // headwind slows fall, tailwind hastens
		speed.set(Random.Float(windX - 3f, windX + 1f), Random.Float(50, 80) + windY + 6f * WeatherSprites.gust());
		acc.set(0, 30);

		//the streak leans as far as it flies sideways, and draws out with speed
		float lean = Math.abs(speed.x) / Math.max(1f, speed.y);
		boolean fast = speed.y > 72f || (rate > 0.6f && Random.Float() < 0.6f);
		if (fast) frame(lean < 0.12f ? WeatherSprites.RAIN_VL : (lean < 0.4f ? WeatherSprites.RAIN_S1L : WeatherSprites.RAIN_S2L));
		else frame(lean < 0.12f ? WeatherSprites.RAIN_V : (lean < 0.4f ? WeatherSprites.RAIN_S1 : WeatherSprites.RAIN_S2));
		scale.x = speed.x < 0 ? -1 : 1;
	}

	@Override
	public void update() {
		boolean falling = left > 0;
		super.update();
		if (falling && left <= 0) {
			//landed: a splash where it hit, if anyone is there to see it
			if (splashes && WeatherSprites.visible(x, y) && parent instanceof Group) {
				SplashParticle.splash((Group) parent, x, y, tint, peak);
			}
			return;
		}
		am = envelope(0.3f, 0.05f, peak);
		fov();
	}
}
