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

public class AmbientSnowParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AmbientSnowParticle) emitter.recycle(AmbientSnowParticle.class)).reset(x, y);
		}
	};

	private float wobblePhase;
	private float windBias; // cached wind at spawn

	public AmbientSnowParticle() {
		super();
		color(0xFFFFFF);
		lifespan = Random.Float(3f, 6f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;
		size = Random.Float(1f, 2.5f);

		// Wind biases horizontal drift in actual wind direction — snow is light so wind has big effect
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		windBias = (float) Math.sin(windRad) * ClimateManager.localWindSpeed() * 0.8f;
		float windY  = -(float) Math.cos(windRad) * ClimateManager.localWindSpeed() * 0.2f;
		speed.set(Random.Float(-2, 2) + windBias, Random.Float(6, 14) + windY);
		wobblePhase = Random.Float((float)(Math.PI * 2));
	}

	/**
	 * Loose snow the wind lifts off frozen ground: the same flake, but blown
	 * along rather than falling - it skims the surface the way the wind goes.
	 */
	public void resetDrift(float x, float y) {
		reset(x, y);
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float wind = ClimateManager.surfaceWindSpeed();
		windBias = (float) Math.sin(windRad) * wind * 1.2f;
		float windY = -(float) Math.cos(windRad) * wind * 0.5f;
		speed.set(Random.Float(-2, 2) + windBias, Random.Float(-2, 2) + windY);
		size = Random.Float(0.8f, 1.6f);
		left = lifespan = Random.Float(1.5f, 3f);
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;

		// Gentle drift + wind — strong wind suppresses wobble amplitude
		wobblePhase += Game.elapsed * 2f;
		float wobbleAmt = Math.max(1f, 5f - Math.abs(windBias) * 0.25f);
		speed.x = (float)Math.sin(wobblePhase) * wobbleAmt + windBias;

		if (p > 0.9f) {
			am = (1f - p) * 10f;
		} else if (p < 0.15f) {
			am = p * 6.7f;
		} else {
			am = 0.7f;
		}

		int cell = (int)(this.x / DungeonTilemap.SIZE) + (int)(this.y / DungeonTilemap.SIZE) * Dungeon.level.width();
		if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
			am = 0;
		}
	}
}
