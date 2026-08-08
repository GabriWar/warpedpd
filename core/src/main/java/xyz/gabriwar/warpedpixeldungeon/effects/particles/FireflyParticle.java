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

public class FireflyParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((FireflyParticle) emitter.recycle(FireflyParticle.class)).reset(x, y);
		}
	};

	private float blinkPhase;
	private float blinkSpeed;

	public FireflyParticle() {
		super();
		lifespan = Random.Float(4f, 8f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;
		size = Random.Float(1f, 2f);
		color(Random.Float() < 0.7f ? 0xFFEE66 : 0xAAFF44);

		// Wind gently pushes fireflies — they're light but resist
		float wind = ClimateManager.localWindSpeed() * 0.3f;
		speed.set(Random.Float(-4, 4) + wind, Random.Float(-4, 4));
		acc.set(0, 0);
		blinkPhase = Random.Float((float)(Math.PI * 2));
		blinkSpeed = Random.Float(2f, 5f);
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;

		blinkPhase += Game.elapsed * blinkSpeed;
		float glow = ((float) Math.sin(blinkPhase) + 1f) * 0.5f;

		float envelope;
		if (p > 0.85f) {
			envelope = (1f - p) * 6.7f;
		} else if (p < 0.15f) {
			envelope = p * 6.7f;
		} else {
			envelope = 1f;
		}

		am = glow * envelope * 0.8f;

		// Random direction changes, biased by wind
		if (Random.Float() < Game.elapsed * 0.5f) {
			float wind = ClimateManager.localWindSpeed() * 0.3f;
			speed.set(Random.Float(-4, 4) + wind, Random.Float(-4, 4));
		}

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
