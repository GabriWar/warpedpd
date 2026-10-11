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
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A firefly: a bright heart in a soft glow, drawn as light over the night's tint (every firefly,
 * the weather's, the dungeon's and the surface's, in the one light layer), that wanders on a slow
 * meandering path and lights in pulses, a flash or two and then dark for a while, the way real
 * ones signal. While it flashes it lights a small pool on the ground under it, and over water its
 * glow shows in it, wobbling. Now and then it falls in with the others' beat; it shies from the
 * hero; it rests a while on the grass. At most CAP at once.
 */
public class FireflyParticle extends WeatherParticle {

	/** The most fireflies at once. */
	public static final int CAP = 20;
	/** Its pool: colour, alpha at a full flash, px below it. */
	public static final int POOL_COLOR = 0xF8F060;
	public static final float POOL_ALPHA = 0.07f, POOL_BELOW = 7f;
	/** Its glow in the water: share of the flash, px below, its wobble's px and Hz. */
	public static final float REFLECT_SHARE = 0.3f, REFLECT_BELOW = 12f, WOBBLE = 1f, WOBBLE_HZ = 3f;
	/** One cycle in this many falls in with the others' beat, every BEAT s. */
	public static final int ENTRAIN = 20;
	public static final float BEAT = 2f;
	/** It shies from the hero within this many px; rests on grass 1-2 s. */
	public static final float SHY = 24f, REST_MIN = 1f, REST_MAX = 2f, REST_CHANCE = 0.15f;

	private static final AtomicInteger living = new AtomicInteger();

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			if (living.get() >= CAP) return;
			Emitter carrier = Fx.lightCarrier(FireflyParticle.class);
			((FireflyParticle) (carrier != null ? carrier : emitter).recycle(FireflyParticle.class)).reset(x, y);
		}

		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** Fireflies alive now. */
	public static int alive() {
		return living.get();
	}

	private float heading, pace, bobPhase;
	private float pulse, pulseLeft, dark, rest;
	private int flashes;
	private float glow;
	private boolean counted;

	//the pool under it and its glow in the water: its own two quads, drawn with it
	private final Image pool, reflection;
	private boolean reflecting;

	public FireflyParticle() {
		super();
		lifespan = FxRandom.Float(5f, 10f);
		pool = lightFrame(FxFrames.Light.POOL_15);
		reflection = lightFrame(FxFrames.Light.GLOW_7);
	}

	private static Image lightFrame(int[] f) {
		Image i = new Image(Assets.Effects.FX_LIGHT);
		float tw = i.texture.width, th = i.texture.height;
		i.frame(new RectF(f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th));
		i.hardlight(POOL_COLOR);
		return i;
	}

	@Override
	public void revive() {
		super.revive();
		if (!counted) {
			counted = true;
			living.incrementAndGet();
		}
	}

	@Override
	public void kill() {
		super.kill();
		if (counted) {
			counted = false;
			living.decrementAndGet();
		}
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = FxRandom.Float(5f, 10f);

		frame(FxRandom.Float() < 0.75f ? WeatherSprites.GLOW_5 : WeatherSprites.GLOW_3);
		color(FxRandom.Float() < 0.7f ? 0xF8F060 : 0xC8FF60);

		heading = FxRandom.Float((float)(Math.PI * 2));
		pace = FxRandom.Float(3f, 7f);
		bobPhase = FxRandom.Float((float)(Math.PI * 2));
		acc.set(0, 0);
		//start somewhere in its cycle so a fresh swarm does not blink in step
		dark = FxRandom.Float(0f, 1.5f);
		pulseLeft = 0;
		flashes = 0;
		rest = 0;
		glow = 0;
		am = 0;
	}

	@Override
	public void update() {
		super.update();
		float dt = Game.elapsed;

		//the path: a heading that drifts, a gentle bob, the wind leaned into; still while it rests
		if (rest > 0) {
			rest -= dt;
			speed.set(0, 0);
		} else {
			heading += FxRandom.Float(-1f, 1f) * 2.2f * dt;
			bobPhase += dt * 2f;
			float run = pace;
			CharSprite hero = Dungeon.hero != null ? Dungeon.hero.sprite : null;
			if (hero != null && hero.visible) {
				float dx = x - (hero.x + hero.width() / 2f), dy = y - (hero.y + hero.height() / 2f);
				if (dx * dx + dy * dy < SHY * SHY) {
					//away from him, quicker
					heading = (float) Math.atan2(dy, dx);
					run = 9f;
				}
			}
			float wind = ClimateManager.localWindSpeed() * 0.25f;
			speed.set((float) Math.cos(heading) * run + wind, (float) Math.sin(heading) * run * 0.6f + (float) Math.sin(bobPhase) * 2.5f);
		}

		//the light: a pulse of a third of a second, one to three of them, then dark
		if (pulseLeft > 0) {
			pulseLeft -= dt;
			float t = 1f - pulseLeft / pulse;
			glow = (float) Math.sin(t * Math.PI);
			if (pulseLeft <= 0) {
				if (--flashes > 0) { pulse = FxRandom.Float(0.25f, 0.4f); pulseLeft = pulse; dark = 0; }
				else darkSpell();
			}
		} else {
			dark -= dt;
			glow = 0;
			if (dark <= 0) {
				flashes = FxRandom.Float() < 0.6f ? 1 : (FxRandom.Float() < 0.7f ? 2 : 3);
				pulse = FxRandom.Float(0.3f, 0.5f);
				pulseLeft = pulse;
			}
		}
		am = glow * envelope(0.1f, 0.15f, 0.95f);
		fov();
		placeLights();
	}

	//dark between its flashes: now and then until the others' beat, and now and then at rest
	private void darkSpell() {
		if (FxRandom.Int(ENTRAIN) == 0) {
			float t = Game.timeTotal;
			dark = (float) (Math.floor(t / BEAT) + 1) * BEAT - t;
		} else {
			dark = FxRandom.Float(0.9f, 2.6f);
		}
		if (rest <= 0 && onGrass() && FxRandom.chance(REST_CHANCE)) {
			rest = FxRandom.Float(REST_MIN, REST_MAX);
		}
	}

	private boolean onGrass() {
		Level level = Dungeon.level;
		int c = Fx.cellAt(x, y + height);
		if (level == null || c < 0 || c >= level.length()) return false;
		int t = level.map[c];
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
	}

	//its pool on the ground and its glow in the water, where it is now
	private void placeLights() {
		float cx = x + width / 2f, cy = y + height / 2f;
		pool.x = Math.round(cx - pool.width / 2f);
		pool.y = Math.round(cy + POOL_BELOW - pool.height / 2f);
		pool.am = POOL_ALPHA * am;
		reflecting = visible && am > 0.01f && WaterFX.onWater(cx, cy + REFLECT_BELOW);
		if (reflecting) {
			float wob = Math.round(WOBBLE * (float) Math.sin(Game.timeTotal * 2 * Math.PI * WOBBLE_HZ + heading));
			reflection.x = Math.round(cx - reflection.width / 2f + wob);
			reflection.y = Math.round(cy + REFLECT_BELOW - reflection.height / 2f);
			reflection.am = REFLECT_SHARE * am;
			reflection.flipVertical = true;
		}
	}

	@Override
	public void draw() {
		if (visible && am > 0.01f) {
			pool.draw();
			if (reflecting) reflection.draw();
		}
		super.draw();
	}

	@Override
	public void destroy() {
		super.destroy();
		//gone with its scene (never killed): no longer counted
		if (counted) {
			counted = false;
			living.decrementAndGet();
		}
		pool.destroy();
		reflection.destroy();
	}
}
