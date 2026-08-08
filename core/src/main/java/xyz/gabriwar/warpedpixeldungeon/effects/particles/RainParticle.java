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

public class RainParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((RainParticle) emitter.recycle(RainParticle.class)).reset(x, y);
		}
	};

	public RainParticle() {
		super();
		color(Random.Float() < 0.3f ? 0x8899BB : 0x6688AA);
		lifespan = Random.Float(0.6f, 1.0f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;
		size = Random.Float(0.5f, 1.5f);

		// Wind pushes rain in the actual wind direction
		float wind = ClimateManager.localWindSpeed();
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float windX = (float) Math.sin(windRad) * wind * 1.2f;
		float windY = -(float) Math.cos(windRad) * wind * 0.35f; // headwind slows fall, tailwind hastens
		speed.set(Random.Float(windX - 3f, windX + 1f), Random.Float(50, 80) + windY);
		acc.set(0, 30);
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		am = p < 0.3f ? p * 3.3f : 0.6f;

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
