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
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * A snowflake, in one of four sizes from a speck to a crystal, wandering down on
 * its own wobble and carried by the wind; in a blizzard it is a white streak
 * driven sideways, or, in its thick, a flake blown along with the streaks.
 */
public class AmbientSnowParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AmbientSnowParticle) emitter.recycle(AmbientSnowParticle.class)).reset(x, y);
		}
	};

	/** loose snow the wind picks up off frozen ground; nothing when the air is still */
	public static final Emitter.Factory DRIFT = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			if (ClimateManager.localWindSpeed() < 7f) return;
			((AmbientSnowParticle) emitter.recycle(AmbientSnowParticle.class)).resetDrift(x, y);
		}
	};

	/** driven snow: streaks that cross the screen with the wind */
	public static final Emitter.Factory BLIZZARD = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AmbientSnowParticle) emitter.recycle(AmbientSnowParticle.class)).resetStorm(x, y);
		}
	};

	/** the thick of a blizzard (WeatherOverlay): a flake of any size blown along with the wind */
	public static final Emitter.Factory DRIVEN = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AmbientSnowParticle) emitter.recycle(AmbientSnowParticle.class)).resetDriven(x, y);
		}
	};

	/** and its fastest snow: a streak racing nearly level with the wind */
	public static final Emitter.Factory GALE = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((AmbientSnowParticle) emitter.recycle(AmbientSnowParticle.class)).resetGale(x, y);
		}
	};

	private float wobblePhase;
	private float windBias; // cached wind at spawn
	private float wobble;
	private boolean storm, settled;
	private static final float SETTLE = 0.7f;

	public AmbientSnowParticle() {
		super();
		color(0xFFFFFF);
		lifespan = Random.Float(3f, 6f);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float(3f, 6f);
		storm = false;
		settled = false;
		color(Random.Float() < 0.8f ? 0xFFFFFF : 0xE4EEFF);

		//most flakes are specks; the crystals are rare and fall slower
		float r = Random.Float();
		frame(r < 0.5f ? WeatherSprites.FLAKE_1 : (r < 0.78f ? WeatherSprites.FLAKE_2 : (r < 0.94f ? WeatherSprites.FLAKE_3 : WeatherSprites.FLAKE_5)));
		float weight = frame == WeatherSprites.FLAKE_5 ? 0.7f : (frame == WeatherSprites.FLAKE_3 ? 0.85f : 1f);

		//wind biases horizontal drift in actual wind direction - snow is light so wind has big effect
		float wind = ClimateManager.localWindSpeed() * (1f + 0.3f * WeatherSprites.gust());
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		windBias = (float) Math.sin(windRad) * wind * 0.8f;
		float windY  = -(float) Math.cos(windRad) * wind * 0.2f;
		speed.set(Random.Float(-2, 2) + windBias, (Random.Float(6, 14) + windY) * weight);
		wobble = Math.max(1f, 5f - Math.abs(windBias) * 0.25f);
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
		frame(Random.Float() < 0.7f ? WeatherSprites.FLAKE_1 : WeatherSprites.FLAKE_2);
		left = lifespan = Random.Float(1.5f, 3f);
	}

	/** a blizzard flake: a streak, fast, nearly level, gone in a second */
	public void resetStorm(float x, float y) {
		reset(x, y);
		storm = true;
		float wind = Math.max(8f, ClimateManager.localWindSpeed()) * (1f + 0.4f * WeatherSprites.gust());
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		float wx = (float) Math.sin(windRad) * wind * 3f;
		float wy = -(float) Math.cos(windRad) * wind * 0.8f + Random.Float(14, 26);
		speed.set(wx + Random.Float(-4, 4), wy);
		windBias = wx;
		wobble = 0.5f;
		frame(Random.Float() < 0.6f ? WeatherSprites.STREAK_3 : WeatherSprites.STREAK_4);
		angle = (float) Math.toDegrees(Math.atan2(speed.y, speed.x));
		left = lifespan = Random.Float(0.8f, 1.6f);
	}

	/** a flake, any of the four sizes, carried hard along the wind and falling slowly under it:
	 *  gone in a second or two */
	public void resetDriven(float x, float y) {
		reset(x, y);
		storm = true;
		float wind = Math.max(8f, ClimateManager.localWindSpeed()) * (1f + 0.4f * WeatherSprites.gust());
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		speed.set((float) Math.sin(windRad) * wind * 2f + Random.Float(-6, 6),
				-(float) Math.cos(windRad) * wind * 0.6f + Random.Float(8, 18));
		windBias = speed.x;
		left = lifespan = Random.Float(1.2f, 2.4f);
	}

	/** a blizzard streak driven half again as fast and flatter, the long one, gone sooner */
	public void resetGale(float x, float y) {
		resetStorm(x, y);
		speed.set(speed.x * 1.8f, speed.y * 1.2f);
		windBias = speed.x;
		frame(WeatherSprites.STREAK_4);
		angle = (float) Math.toDegrees(Math.atan2(speed.y, speed.x));
		left = lifespan = Random.Float(0.5f, 1f);
	}

	@Override
	public void update() {
		if (!storm && !settled && left <= Game.elapsed && onFrozenGround()) {
			//it lands on snow and lies there a moment before it is lost in the rest
			settled = true;
			left = lifespan = SETTLE;
			speed.set(0, 0);
			acc.set(0, 0);
		}
		super.update();
		if (settled) {
			am = 0.7f * (left / lifespan);
		} else {
			if (!storm) {
				//gentle drift + wind - strong wind suppresses wobble amplitude
				wobblePhase += Game.elapsed * 2f;
				speed.x = (float)Math.sin(wobblePhase) * wobble + windBias;
			}
			am = envelope(0.15f, 0.05f, storm ? 0.85f : 0.75f);
		}
		fov();
	}

	private boolean onFrozenGround() {
		if (!(Dungeon.level instanceof OverworldLevel) || x < 0 || y < 0) return false;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int cx = (int)(x / DungeonTilemap.SIZE), cy = (int)(y / DungeonTilemap.SIZE);
		if (cx >= ow.width() || cy >= ow.height()) return false;
		return ow.frozenAt(cx + cy * ow.width());
	}
}
