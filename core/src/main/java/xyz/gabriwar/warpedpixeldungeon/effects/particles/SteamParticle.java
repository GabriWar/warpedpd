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
 * Rising white wisps that expand and fade — hot steam from vents.
 * Used in Dwarven City depth ambiance.
 */
public class SteamParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((SteamParticle) emitter.recycle(SteamParticle.class)).reset(x, y);
		}
	};

	private float initialSize;

	public SteamParticle() {
		super();
		color(0xEEEEEE);
		lifespan = Random.Float(2f, 4f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;

		initialSize = Random.Float(1.5f, 3f);
		size = initialSize;

		// Rises and drifts with wind
		float wind = ClimateManager.localWindSpeed() * 0.4f;
		speed.set(Random.Float(-1, 1) + wind, Random.Float(-12, -6)); // rises
		acc.set(0, -2); // gentle upward acceleration (buoyancy)
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		float age = 1f - p; // 0 → 1

		// Expand as it rises (steam disperses)
		size(initialSize * (1f + age * 2f));

		// Wind pushes it sideways over time
		float wind = ClimateManager.localWindSpeed() * 0.4f;
		speed.x += wind * Game.elapsed * 0.5f;

		// Fade out as it expands
		if (p > 0.85f) {
			am = (1f - p) * 6.7f * 0.25f;
		} else {
			am = p * 0.25f;
		}

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
