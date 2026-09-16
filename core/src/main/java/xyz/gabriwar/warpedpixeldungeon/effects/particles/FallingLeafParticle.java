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
 * A leaf, or in spring a petal, tumbling down: it turns over as it falls, swings
 * on its wobble and rides the wind. The colours are the season's.
 */
public class FallingLeafParticle extends WeatherParticle {

	private static final int[] AUTUMN_COLORS = {
			0xCC6622, 0xDD8833, 0xBB4411, 0xEEAA22, 0x995511
	};

	private static final int[] SPRING_COLORS = {
			0x44BB44, 0x88CC44, 0xFFAABB, 0x66DD66, 0xFF88AA
	};

	private static final int[] SUMMER_COLORS = {
			0x3E7D2E, 0x5A9E3A, 0x78B24A, 0x2F6B25, 0x8FC05A
	};

	/** The leaf colours a season's canopy sheds. */
	public static int[] seasonColors(xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season season) {
		switch (season) {
			case AUTUMN: return AUTUMN_COLORS;
			case SPRING: return SPRING_COLORS;
			default:     return SUMMER_COLORS;
		}
	}

	public static final Emitter.Factory AUTUMN = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			FallingLeafParticle p = (FallingLeafParticle) emitter.recycle(FallingLeafParticle.class);
			p.petal = false;
			p.color(AUTUMN_COLORS[Random.Int(AUTUMN_COLORS.length)]);
			p.reset(x, y);
		}
	};

	public static final Emitter.Factory SPRING = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			FallingLeafParticle p = (FallingLeafParticle) emitter.recycle(FallingLeafParticle.class);
			int c = SPRING_COLORS[Random.Int(SPRING_COLORS.length)];
			//the pinks are blossom, the greens young leaves
			p.petal = (c & 0xFF0000) == 0xFF0000;
			p.color(c);
			p.reset(x, y);
		}
	};

	private float wobblePhase;
	private float windBias;
	private float tumble;
	private int face;
	private boolean petal;

	public FallingLeafParticle() {
		super();
		lifespan = Random.Float(3f, 5f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(3f, 5f);
		face = Random.Int(WeatherSprites.LEAVES.length);
		frame(petal ? WeatherSprites.PETAL : WeatherSprites.LEAVES[face]);
		tumble = Random.Float(0.12f, 0.3f);

		windBias = ClimateManager.localWindSpeed() * 1.0f;
		speed.set(Random.Float(-6, 6) + windBias, petal ? Random.Float(5, 10) : Random.Float(8, 16));
		acc.set(0, Random.Float(4, 8));
		wobblePhase = Random.Float((float)(Math.PI * 2));
	}

	/**
	 * A leaf torn off a canopy by the wind: carried the way the wind blows
	 * (surfaceWindDir, compass degrees, the direction it blows TOWARDS),
	 * with only a lazy fall of its own.
	 */
	public void resetWind(float x, float y) {
		reset(x, y);
		float wind = ClimateManager.surfaceWindSpeed();
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		windBias = (float) Math.sin(windRad) * wind * 1.5f;
		float windY = -(float) Math.cos(windRad) * wind * 0.6f;
		speed.set(Random.Float(-4, 4) + windBias, Random.Float(3, 9) + windY);
		acc.set(0, Random.Float(1, 3));
	}

	@Override
	public void update() {
		super.update();
		wobblePhase += Game.elapsed * 3f;
		float gust = 1f + 0.4f * WeatherSprites.gust();
		speed.x = (float) Math.sin(wobblePhase) * 10f + windBias * gust;
		//it turns over as it swings: another face, or the same one mirrored
		tumble -= Game.elapsed;
		if (tumble <= 0) {
			tumble = Random.Float(0.12f, 0.3f);
			if (!petal && Random.Float() < 0.5f) {
				face = (face + 1 + Random.Int(WeatherSprites.LEAVES.length - 1)) % WeatherSprites.LEAVES.length;
				frame(WeatherSprites.LEAVES[face]);
			}
			scale.x = -scale.x;
		}
		am = envelope(0.2f, 0.1f, 1f);
		fov();
	}
}
