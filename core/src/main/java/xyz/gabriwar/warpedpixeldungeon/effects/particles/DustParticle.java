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
 * Tiny tan/brown motes blown around by wind.
 * Used in dry/warm areas and prison corridors.
 */
public class DustParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((DustParticle) emitter.recycle(DustParticle.class)).reset(x, y);
		}
	};

	private static final int[] COLORS = { 0xBB9966, 0xAA8855, 0xCC9977, 0x998866 };

	private float wobblePhase;

	public DustParticle() {
		super();
		lifespan = Random.Float(2f, 5f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;

		size = Random.Float(0.5f, 1.5f);
		color(COLORS[Random.Int(COLORS.length)]);

		// Dust is very light — wind dominates movement
		float wind = ClimateManager.localWindSpeed();
		speed.set(Random.Float(-2, 2) + wind * 1.2f, Random.Float(-2, 4));
		acc.set(0, Random.Float(0.5f, 2f)); // barely falls
		wobblePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;

		// Erratic drift
		wobblePhase += Game.elapsed * Random.Float(3f, 6f);
		speed.x += (float) Math.sin(wobblePhase) * 3f * Game.elapsed;

		// Very subtle — dust is barely visible
		float envelope;
		if (p > 0.85f) {
			envelope = (1f - p) * 6.7f;
		} else if (p < 0.15f) {
			envelope = p * 6.7f;
		} else {
			envelope = 1f;
		}
		am = envelope * 0.35f;

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
