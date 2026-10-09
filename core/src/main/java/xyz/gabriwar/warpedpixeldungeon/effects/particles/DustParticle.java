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
 * Wind-borne dust: tan specks that skate along low and stretch when the wind is up. In a
 * sandstorm the sand is driven: fast grains and streaks along the ground with the wind, and
 * puffs of dust rolling after them.
 */
public class DustParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((DustParticle) emitter.recycle(DustParticle.class)).reset(x, y);
		}
	};

	/** the sandstorm's driven sand: grains and streaks, fast and low, with the wind */
	public static final Emitter.Factory DRIVEN = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((DustParticle) emitter.recycle(DustParticle.class)).resetDriven(x, y);
		}
	};

	/** the sandstorm's dust: a puff rolling along with the wind, swelling as it goes */
	public static final Emitter.Factory PUFF = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((DustParticle) emitter.recycle(DustParticle.class)).resetPuff(x, y);
		}
	};

	private static final int[] COLORS = { 0xBB9966, 0xAA8855, 0xCC9977, 0x998866 };
	private static final int[] SAND = { 0xE0C890, 0xD8B878, 0xC8A060, 0xB89058 };

	private float wobblePhase, wobbleRate;
	//a sandstorm's: driven sand, or a puff (its size at the start, and at the end)
	private boolean driven, puff;
	private float grow0, grow1;

	public DustParticle() {
		super();
		lifespan = Random.Float(2f, 5f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(2f, 5f);
		driven = puff = false;

		float wind = ClimateManager.localWindSpeed();
		//a fast wind draws the specks out into short streaks
		frame(wind > 6f && Random.Float() < 0.5f ? WeatherSprites.SPECK_2 : WeatherSprites.SPECK_1);
		color(COLORS[Random.Int(COLORS.length)]);

		speed.set(Random.Float(-2, 2) + wind * 1.2f, Random.Float(-2, 4));
		acc.set(0, Random.Float(0.5f, 2f)); // barely falls
		wobblePhase = Random.Float((float)(Math.PI * 2));
		wobbleRate = Random.Float(3f, 6f);
	}

	/** a grain or a streak of sand, driven along the ground the way the wind blows (0 up the
	 *  screen, 90 right), faster in a gust, gone in about a second */
	public void resetDriven(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		driven = true;
		puff = false;
		float r = Random.Float();
		frame(r < 0.3f ? WeatherSprites.STREAK_4 : (r < 0.6f ? WeatherSprites.STREAK_3
				: (r < 0.8f ? WeatherSprites.SPECK_2 : WeatherSprites.SPECK_1)));
		color(SAND[Random.Int(SAND.length)]);
		float wind = Math.max(10f, ClimateManager.localWindSpeed()) * (1f + 0.4f * WeatherSprites.gust());
		float rad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float along = wind * Random.Float(5f, 8f);
		//low: it skims the ground, only a little settling as it goes
		speed.set((float) Math.sin(rad) * along + Random.Float(-3, 3),
				-(float) Math.cos(rad) * along + Random.Float(1, 6));
		acc.set(0, 0);
		angle = (float) Math.toDegrees(Math.atan2(speed.y, speed.x));
		left = lifespan = Random.Float(0.5f, 1.1f);
	}

	/** a puff of dust: a wisp scaled up, slower than the sand and swelling as it rolls along */
	public void resetPuff(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		driven = false;
		puff = true;
		frame(Random.Float() < 0.5f ? WeatherSprites.WISP_L : WeatherSprites.WISP_XL);
		color(SAND[2 + Random.Int(2)]);
		float wind = Math.max(10f, ClimateManager.localWindSpeed());
		float rad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float along = wind * Random.Float(2f, 3.5f);
		speed.set((float) Math.sin(rad) * along, -(float) Math.cos(rad) * along);
		acc.set(0, 0);
		grow0 = Random.Float(1.5f, 2.2f);
		grow1 = grow0 * Random.Float(1.4f, 1.8f);
		left = lifespan = Random.Float(2f, 3.5f);
	}

	@Override
	public void update() {
		if (driven || puff) {
			super.update();
			if (puff) {
				scale.set(grow0 + (grow1 - grow0) * (1f - left / lifespan));
				am = envelope(0.3f, 0.4f, 0.22f);
			} else {
				am = envelope(0.1f, 0.3f, 0.8f);
			}
			fov();
			return;
		}
		super.update();
		wobblePhase += Game.elapsed * wobbleRate;
		speed.x += ((float) Math.sin(wobblePhase) * 3f + 4f * WeatherSprites.gust()) * Game.elapsed;
		am = envelope(0.15f, 0.15f, 0.4f);
		fov();
	}
}
