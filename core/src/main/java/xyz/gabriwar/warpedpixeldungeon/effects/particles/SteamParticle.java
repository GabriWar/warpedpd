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

/** Steam: wisps that rise, spread as they climb, and thin into nothing. */
public class SteamParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((SteamParticle) emitter.recycle(SteamParticle.class)).reset(x, y);
		}
	};

	private int stage;

	public SteamParticle() {
		super();
		color(0xEEEEEE);
		lifespan = Random.Float(2f, 4f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(2f, 4f);
		stage = -1;
		grow(0);

		float wind = ClimateManager.localWindSpeed() * 0.4f;
		speed.set(Random.Float(-1, 1) + wind, Random.Float(-12, -6)); // rises
		acc.set(0, -2); // gentle upward acceleration (buoyancy)
	}

	private void grow(int s) {
		if (s != stage) {
			stage = s;
			frame(s == 0 ? WeatherSprites.WISP_S : (s == 1 ? WeatherSprites.WISP_L : WeatherSprites.WISP_XL));
		}
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		float age = 1f - p;
		grow(age < 0.35f ? 0 : (age < 0.7f ? 1 : 2));
		float wind = ClimateManager.localWindSpeed() * 0.4f;
		speed.x += wind * Game.elapsed * 0.5f;
		am = p > 0.85f ? (1f - p) * 6.7f * 0.3f : p * 0.3f;
		fov();
	}
}
