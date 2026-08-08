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
 * Dark grey flakes that glow faintly orange at the edges — volcanic ash.
 * Replaces all surface precipitation in Demon Halls.
 * Slow descent, fluttering drift, wind-reactive.
 */
public class AshParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AshParticle) emitter.recycle(AshParticle.class)).reset(x, y);
		}
	};

	private float wobblePhase;
	private float windBias;
	private boolean embers; // some particles glow orange

	public AshParticle() {
		super();
		lifespan = Random.Float(3f, 6f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;
		size = Random.Float(1f, 2.5f);

		// Mostly dark grey, some glow like embers
		embers = Random.Float() < 0.15f;
		color(embers ? 0xFF6633 : (Random.Float() < 0.5f ? 0x555555 : 0x444444));

		windBias = ClimateManager.localWindSpeed() * 0.7f;
		speed.set(Random.Float(-4, 4) + windBias, Random.Float(4, 10));
		acc.set(0, Random.Float(1f, 3f));
		wobblePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;

		// Flutter
		wobblePhase += Game.elapsed * 2.5f;
		speed.x = (float) Math.sin(wobblePhase) * 6f + windBias;

		float envelope;
		if (p > 0.85f) {
			envelope = (1f - p) * 6.7f;
		} else if (p < 0.1f) {
			envelope = p * 10f;
		} else {
			envelope = 1f;
		}

		// Embers pulse faintly
		if (embers) {
			float pulse = ((float) Math.sin(wobblePhase * 3f) + 1f) * 0.25f + 0.5f;
			am = envelope * pulse * 0.7f;
		} else {
			am = envelope * 0.5f;
		}

		// Shrink as it cools
		size(size * (0.6f + 0.4f * p));

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
