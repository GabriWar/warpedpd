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

import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterDrops;

/**
 * Water dripping from a cave roof, the trees after rain: a drop, then the small splash of it
 * landing - on the water a small ring in its surface (and a plink now and then). Its randomness
 * is the effects' own (FxRandom).
 */
public class DripParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((DripParticle) emitter.recycle(DripParticle.class)).reset(x, y);
		}
	};

	/** A drop let go from the roof over the emitter's point, timed to land on it exactly: the
	 *  world's caves drip into their pools and by their walls (levels/overworld/CaveLife), and the
	 *  dungeon's (levels/ambience/DungeonLife). It gathers on the roof as a bead first, then falls,
	 *  and crowns and rings the water where it lands (WaterDrops.fall). */
	public static final Emitter.Factory FALL = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			WaterDrops.fall(emitter, x, y, FxRandom.chance(0.5f) ? 0x7A9CCC : 0x6688BB);
		}
	};

	private int tint;

	public DripParticle() {
		super();
		lifespan = FxRandom.Float(0.4f, 0.8f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = FxRandom.Float(0.4f, 0.8f);
		tint = FxRandom.Float() < 0.5f ? 0x7A9CCC : 0x6688BB;
		color(tint);
		frame(WeatherSprites.DROP);

		float wind = ClimateManager.localWindSpeed();
		speed.set(FxRandom.Float(-1f, 1f) + wind * 0.1f, FxRandom.Float(25, 45));
		acc.set(0, 40);
	}

	@Override
	public void update() {
		boolean falling = left > 0;
		super.update();
		if (falling && left <= 0) {
			if (WeatherSprites.visible(x, y)) {
				//on the water a small ring in its own surface, else the splash of it
				if (WaterFX.onWater(x, y)) WaterDrops.drip(x, y);
				else if (parent instanceof Group) SplashParticle.splash((Group) parent, x, y, tint, 0.55f);
			}
			return;
		}
		am = envelope(0.2f, 0.05f, 0.6f);
		fov();
	}
}
