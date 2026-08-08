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
 * Mixed rain-ice — falls like rain but with a slight wobble and icy tint.
 * Visually between rain and snow: faster than snow, wetter-looking than hail.
 */
public class SleetParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((SleetParticle) emitter.recycle(SleetParticle.class)).reset(x, y);
		}
	};

	private float wobblePhase;

	public SleetParticle() {
		super();
		lifespan = Random.Float(0.7f, 1.1f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;
		size = Random.Float(0.8f, 2.0f);

		// Blue-white translucent (wetter than hail)
		color(Random.Float() < 0.5f ? 0x99BBDD : 0xBBCCEE);

		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float windX = (float) Math.sin(windRad) * ClimateManager.localWindSpeed() * 0.7f;
		speed.set(Random.Float(-2, 2) + windX, Random.Float(40, 65));
		acc.set(0, 25);
		wobblePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;

		// Slight wobble — between rain's straight fall and snow's drift
		wobblePhase += Game.elapsed * 4f;
		speed.x += (float) Math.sin(wobblePhase) * 1.5f * Game.elapsed;

		if (p < 0.25f) {
			am = p * 4f * 0.55f;
		} else {
			am = 0.55f;
		}

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
