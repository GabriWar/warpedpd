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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * Soft fog/mist blobs — large, very translucent, drift slowly with wind.
 * Used for FOG weather state and sewer ambiance.
 */
public class MistParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((MistParticle) emitter.recycle(MistParticle.class)).reset(x, y);
		}
	};

	private float driftPhase;

	public MistParticle() {
		super();
		color(0xDDDDDD);
		lifespan = Random.Float(4f, 8f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;

		// Large soft blobs
		size = Random.Float(3f, 6f);

		// Mist drifts mostly horizontally, pushed by wind
		float wind = ClimateManager.localWindSpeed();
		speed.set(Random.Float(-2, 2) + wind * 0.5f, Random.Float(-1, 2));
		acc.set(0, 0);
		driftPhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;

		// Slow undulating drift
		driftPhase += Game.elapsed * 0.8f;
		speed.y = (float) Math.sin(driftPhase) * 1.5f;

		// Very transparent — fog is subtle
		float envelope;
		if (p > 0.8f) {
			envelope = (1f - p) * 5f;
		} else if (p < 0.2f) {
			envelope = p * 5f;
		} else {
			envelope = 1f;
		}
		am = envelope * 0.15f;

		// Grow slightly over lifetime (fog expands)
		size(size * (0.8f + 0.4f * (1f - p)));

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
