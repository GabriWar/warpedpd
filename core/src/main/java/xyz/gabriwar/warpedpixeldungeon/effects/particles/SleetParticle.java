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
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/** Half-frozen rain: a slushy drop, slower than rain, that splats where it lands. */
public class SleetParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((SleetParticle) emitter.recycle(SleetParticle.class)).reset(x, y);
		}
	};

	private float wobblePhase;
	private int tint;

	public SleetParticle() {
		super();
		lifespan = Random.Float(0.7f, 1.1f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(0.7f, 1.1f);

		tint = Random.Float() < 0.5f ? 0xA8C4E0 : 0xC8D8F0;
		color(tint);
		frame(Random.Float() < 0.5f ? WeatherSprites.RAIN_V : WeatherSprites.FLAKE_2);

		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float windX = (float) Math.sin(windRad) * ClimateManager.localWindSpeed() * 0.7f;
		speed.set(Random.Float(-2, 2) + windX, Random.Float(40, 65));
		acc.set(0, 25);
		wobblePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		boolean falling = left > 0;
		super.update();
		if (falling && left <= 0) {
			if (Random.Float() < 0.3f && WeatherSprites.visible(x, y) && parent instanceof Group) {
				SplashParticle.splash((Group) parent, x, y, tint, 0.5f);
			}
			return;
		}
		wobblePhase += Game.elapsed * 4f;
		speed.x += (float) Math.sin(wobblePhase) * 1.5f * Game.elapsed;
		am = envelope(0.25f, 0.02f, 0.6f);
		fov();
	}
}
