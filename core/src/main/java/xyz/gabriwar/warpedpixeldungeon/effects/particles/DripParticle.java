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
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * Water drips falling from the sewer ceiling.
 * Sparse, slow, small blue-ish droplets with a brief fall and fade.
 * Slightly wind-reactive but mostly vertical.
 */
public class DripParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((DripParticle) emitter.recycle(DripParticle.class)).reset(x, y);
		}
	};

	public DripParticle() {
		super();
		color(Random.Float() < 0.5f ? 0x6688BB : 0x5577AA);
		lifespan = Random.Float(0.4f, 0.8f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;
		size = Random.Float(0.5f, 1.0f);

		// Mostly vertical with tiny wind bias
		float wind = ClimateManager.localWindSpeed();
		speed.set(Random.Float(-1f, 1f) + wind * 0.1f, Random.Float(25, 45));
		acc.set(0, 40);
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		// Fade in quickly, fade out at end
		am = p < 0.2f ? p * 5f : (p > 0.8f ? (1f - p) * 5f : 0.5f);

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
