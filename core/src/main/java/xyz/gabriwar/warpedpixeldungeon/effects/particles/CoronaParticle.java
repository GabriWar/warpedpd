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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * Faint white-gold ring glow for solar eclipse totality.
 * Large, slow-drifting particles with light blending that create
 * a corona halo in the sky during eclipses.
 */
public class CoronaParticle extends PixelParticle {

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
		left = lifespan;
		size = Random.Float(3f, 6f);

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
		float p = left / lifespan;

		// Slow pulse
		pulsePhase += Game.elapsed * 2f;
		float pulse = ((float) Math.sin(pulsePhase) + 1f) * 0.5f;

		float envelope;
		if (p > 0.8f) {
			envelope = (1f - p) * 5f;
		} else if (p < 0.2f) {
			envelope = p * 5f;
		} else {
			envelope = 1f;
		}

		am = pulse * envelope * 0.2f; // very subtle glow

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
