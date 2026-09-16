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

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** A hailstone: a small hard ball that drops fast and bounces once where it lands. */
public class HailParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((HailParticle) emitter.recycle(HailParticle.class)).reset(x, y);
		}
	};

	private static final float BOUNCE = 0.28f;

	private float tumblePhase;
	private boolean landed;

	public HailParticle() {
		super();
		lifespan = Random.Float(0.5f, 0.9f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(0.5f, 0.9f);
		landed = false;

		frame(Random.Float() < 0.7f ? WeatherSprites.HAIL_2 : WeatherSprites.HAIL_1);
		color(Random.Float() < 0.4f ? 0xCCDDEE : 0xF4F8FF);

		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float windX = (float) Math.sin(windRad) * ClimateManager.localWindSpeed() * 0.5f;
		speed.set(Random.Float(-2, 2) + windX, Random.Float(60, 100));
		acc.set(0, 40); // heavy
		tumblePhase = Random.Float((float)(Math.PI * 2));
	}

	@Override
	public void update() {
		if (!landed && left <= Game.elapsed && onWater()) {
			//into water: a ring, and gone
			landed = true;
			if (WeatherSprites.visible(x, y) && parent instanceof Group) SplashParticle.splash((Group) parent, x, y, 0xD8E8F8, 0.7f);
			kill();
			return;
		}
		if (!landed && left <= Game.elapsed) {
			//it hits and hops: back up a little, then down for good
			landed = true;
			left += BOUNCE;
			lifespan = BOUNCE;
			speed.set(speed.x * 0.4f + Random.Float(-6, 6), -Random.Float(22, 38));
			acc.set(0, 220);
		}
		super.update();
		if (!landed) {
			tumblePhase += Game.elapsed * 8f;
			speed.x += (float) Math.sin(tumblePhase) * 2f * Game.elapsed;
			am = envelope(0.15f, 0.02f, 0.9f);
		} else {
			am = 0.9f * (left / lifespan);
		}
		fov();
	}

	private boolean onWater() {
		if (Dungeon.level == null || Dungeon.level.water == null || x < 0 || y < 0) return false;
		int cx = (int)(x / DungeonTilemap.SIZE), cy = (int)(y / DungeonTilemap.SIZE);
		if (cx >= Dungeon.level.width() || cy >= Dungeon.level.height()) return false;
		return Dungeon.level.water[cx + cy * Dungeon.level.width()];
	}
}
