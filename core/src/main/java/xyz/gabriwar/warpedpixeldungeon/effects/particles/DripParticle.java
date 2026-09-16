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

/** Water dripping from a cave roof: a drop, then the small splash of it landing. */
public class DripParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((DripParticle) emitter.recycle(DripParticle.class)).reset(x, y);
		}
	};

	private int tint;

	public DripParticle() {
		super();
		lifespan = Random.Float(0.4f, 0.8f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(0.4f, 0.8f);
		tint = Random.Float() < 0.5f ? 0x7A9CCC : 0x6688BB;
		color(tint);
		frame(WeatherSprites.DROP);

		float wind = ClimateManager.localWindSpeed();
		speed.set(Random.Float(-1f, 1f) + wind * 0.1f, Random.Float(25, 45));
		acc.set(0, 40);
	}

	@Override
	public void update() {
		boolean falling = left > 0;
		super.update();
		if (falling && left <= 0) {
			if (WeatherSprites.visible(x, y) && parent instanceof Group) {
				SplashParticle.splash((Group) parent, x, y, tint, 0.55f);
			}
			return;
		}
		am = envelope(0.2f, 0.05f, 0.6f);
		fov();
	}
}
