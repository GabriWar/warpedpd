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
 * Hard ice pellets — falls fast with tumble, heavier than rain/snow.
 * Wind pushes them but they resist more due to mass.
 */
public class HailParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((HailParticle) emitter.recycle(HailParticle.class)).reset(x, y);
		}
	};

	private float tumblePhase;

	public HailParticle() {
		super();
		lifespan = Random.Float(0.5f, 0.9f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;

		// Chunky ice: bigger than rain, ice-blue/white
		size = Random.Float(1.5f, 3f);
		color(Random.Float() < 0.4f ? 0xCCDDEE : 0xEEF4FF);

		// Fast, heavy fall with wind drift — hail resists wind more than snow
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float windX = (float) Math.sin(windRad) * ClimateManager.localWindSpeed() * 0.5f;
		speed.set(Random.Float(-2, 2) + windX, Random.Float(60, 100));
		acc.set(0, 40); // heavy
		tumblePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;

		// Quick tumble (faster than snow wobble)
		tumblePhase += Game.elapsed * 8f;
		speed.x += (float) Math.sin(tumblePhase) * 2f * Game.elapsed;

		// Hard edges — mostly opaque, abrupt end
		if (p > 0.85f) {
			am = (1f - p) * 6.7f;
		} else if (p < 0.1f) {
			am = p * 10f; // quick fade at ground
		} else {
			am = 0.85f;
		}

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
