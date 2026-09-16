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
 * Falling ash: grey flakes that drift and wobble down, and among them embers that
 * glow orange, brighten in the gusts and go out as they cool.
 */
public class AshParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AshParticle) emitter.recycle(AshParticle.class)).reset(x, y, false);
		}
	};

	/** the glow round the embers, drawn additively in its own layer */
	public static final Emitter.Factory EMBER_GLOW = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AshParticle) emitter.recycle(AshParticle.class)).reset(x, y, true);
		}

		@Override
		public boolean lightMode() {
			return true;
		}
	};

	private float wobblePhase;
	private float windBias;
	private boolean embers; // some particles glow orange
	private boolean glow;

	public AshParticle() {
		super();
		lifespan = Random.Float(3f, 6f);
	}

	public void reset(float x, float y, boolean glowLayer) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(3f, 6f);
		glow = glowLayer;

		embers = glowLayer || Random.Float() < 0.15f;
		if (glowLayer) {
			frame(WeatherSprites.GLOW_3);
			color(0xFF6A28);
		} else if (embers) {
			frame(WeatherSprites.EMBER);
			color(0xFF8040);
		} else {
			frame(Random.Float() < 0.6f ? WeatherSprites.ASH_2 : WeatherSprites.SPECK_1);
			color(Random.Float() < 0.5f ? 0x666068 : 0x4E4A50);
		}

		windBias = ClimateManager.localWindSpeed() * 0.7f;
		speed.set(Random.Float(-4, 4) + windBias, Random.Float(4, 10));
		acc.set(0, Random.Float(1f, 3f));
		wobblePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		wobblePhase += Game.elapsed * 2.5f;
		speed.x = (float) Math.sin(wobblePhase) * 6f + windBias;

		if (embers) {
			//the pulse, brighter in a gust, dying as the ember cools
			float pulse = ((float) Math.sin(wobblePhase * 3f) + 1f) * 0.25f + 0.5f;
			pulse *= 1f + 0.3f * WeatherSprites.gust();
			am = envelope(0.1f, 0.5f, glow ? 0.55f : 0.95f) * Math.min(1f, pulse);
		} else {
			am = envelope(0.1f, 0.15f, 0.55f);
		}
		fov();
	}
}
