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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.AmbientSnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.AshParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CoronaParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DustParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FallingLeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.HailParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.MistParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.RainParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SleetParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SteamParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

/**
 * Manages all weather-related particle layers in the game scene.
 * <p>
 * Emitters are constrained to the camera viewport plus a small margin,
 * and repositioned every frame. This means particles only spawn in the
 * visible area — FogOfWar on top handles masking non-FOV tiles.
 * <p>
 * Layers (bottom to top):
 * <ol>
 *   <li>Ambient — fireflies, falling leaves, petals (flavor, non-precipitation)</li>
 *   <li>Precipitation — rain, snow, hail, sleet driven by climate conditions</li>
 * </ol>
 */
public class WeatherOverlay extends Group {

	// Margin around viewport for particle spawning (pixels).
	// Prevents pop-in at edges as camera scrolls.
	private static final float MARGIN = 32f;

	// ---- Emitter layers ----
	private Emitter ambientEmitter;
	private Emitter ambientEmitter2;
	private Emitter precipEmitter;
	private Emitter sleetSnowEmitter; // secondary emitter for sleet's snow component

	// ---- Current state ----
	private PrecipType activePrecipType = PrecipType.NONE;
	private float activePrecipRate = 0f;
	private AmbientType activeAmbient = AmbientType.NONE;
	private float lightningTimer = 0f; // countdown to next lightning flash

	// Network: when non-null, overrides ClimateManager.isStorming() for spectator
	public static Boolean netStormingOverride = null;

	/** Ambient particle flavors — non-precipitation atmospheric effects. */
	public enum AmbientType {
		NONE,
		FIREFLIES,
		AUTUMN_LEAVES,
		SPRING_PETALS,
		MIST,
		DUST,
		ASH,
		STEAM,
		CORONA,
		DRIP
	}

	public WeatherOverlay() {
		super();
	}

	/** Call once after adding to the scene. */
	public void setup() {
		clearAll();
	}

	// =====================================================================
	// PUBLIC API
	// =====================================================================

	/**
	 * Set the precipitation effect. Pass {@link PrecipType#NONE} / rate 0 to stop.
	 * Smooth transitions: old emitter drains naturally while new one starts.
	 */
	public void setPrecipitation(PrecipType type, float rate) {
		rate = Math.max(0f, Math.min(1f, rate));

		if (type == activePrecipType && Math.abs(rate - activePrecipRate) < 0.01f) {
			return;
		}

		// Drain old emitters
		retireEmitter(precipEmitter);
		retireEmitter(sleetSnowEmitter);
		precipEmitter = null;
		sleetSnowEmitter = null;

		activePrecipType = type;
		activePrecipRate = rate;

		if (type == PrecipType.NONE || rate <= 0f) {
			return;
		}

		float interval = precipInterval(type, rate);

		// On the surface each kind falls where it belongs: snow over frozen
		// ground, rain (and sleet's wet half) over the rest, hail anywhere
		switch (type) {
			case RAIN:
				precipEmitter = createViewportEmitter(false, RAIN_FALL);
				precipEmitter.pour(RainParticle.FACTORY, interval);
				break;
			case SNOW:
				precipEmitter = createViewportEmitter(true, SNOW_FALL);
				precipEmitter.pour(AmbientSnowParticle.FACTORY, interval);
				break;
			case SLEET:
				precipEmitter = createViewportEmitter(false, RAIN_FALL);
				precipEmitter.pour(SleetParticle.FACTORY, interval);
				// Secondary snow component for mixed look
				sleetSnowEmitter = createViewportEmitter(true, SNOW_FALL);
				sleetSnowEmitter.pour(AmbientSnowParticle.FACTORY, interval * 3f);
				add(sleetSnowEmitter);
				break;
			case HAIL:
				precipEmitter = createViewportEmitter(null, 0f);
				precipEmitter.pour(HailParticle.FACTORY, interval);
				break;
			case BLIZZARD:
				precipEmitter = createViewportEmitter(true, SNOW_FALL);
				precipEmitter.pour(AmbientSnowParticle.FACTORY, interval);
				break;
		}
		add(precipEmitter);
	}

	/**
	 * Set the ambient (non-precipitation) particle effect.
	 * Pass {@link AmbientType#NONE} to clear.
	 */
	public void setAmbient(AmbientType type) {
		if (type == activeAmbient) {
			return;
		}

		retireEmitter(ambientEmitter);
		retireEmitter(ambientEmitter2);
		ambientEmitter = null;
		ambientEmitter2 = null;
		activeAmbient = type;

		if (type == AmbientType.NONE) {
			return;
		}

		ambientEmitter = createViewportEmitter(null, 0f);
		switch (type) {
			case FIREFLIES:
				ambientEmitter.pour(FireflyParticle.FACTORY, 0.8f);
				break;
			case AUTUMN_LEAVES:
				ambientEmitter.pour(FallingLeafParticle.AUTUMN, 0.15f);
				break;
			case SPRING_PETALS:
				ambientEmitter.pour(FallingLeafParticle.SPRING, 0.5f);
				break;
			case MIST:
				ambientEmitter.pour(MistParticle.FACTORY, 0.25f);
				break;
			case DUST:
				ambientEmitter.pour(DustParticle.FACTORY, 0.2f);
				break;
			case ASH:
				ambientEmitter.pour(AshParticle.FACTORY, 0.12f);
				break;
			case STEAM:
				ambientEmitter.pour(SteamParticle.FACTORY, 0.3f);
				break;
			case CORONA:
				ambientEmitter.pour(CoronaParticle.FACTORY, 0.5f);
				break;
			case DRIP:
				ambientEmitter.pour(DripParticle.FACTORY, 0.6f);
				break;
		}
		add(ambientEmitter);
	}

	// =====================================================================
	// QUERIES
	// =====================================================================

	public PrecipType activePrecipType()  { return activePrecipType; }
	public float      activePrecipRate()  { return activePrecipRate; }
	public AmbientType activeAmbient()    { return activeAmbient; }

	// =====================================================================
	// FRAME UPDATE
	// =====================================================================

	@Override
	public void update() {
		super.update();

		// Reposition all active emitters to follow camera viewport
		repositionToViewport(precipEmitter);
		repositionToViewport(sleetSnowEmitter);
		repositionToViewport(ambientEmitter);
		repositionToViewport(ambientEmitter2);

		// Suppress ambient during heavy precipitation
		if (activePrecipRate > 0.5f) {
			if (ambientEmitter != null && ambientEmitter.on) ambientEmitter.on = false;
			if (ambientEmitter2 != null && ambientEmitter2.on) ambientEmitter2.on = false;
		}

		// Lightning flashes during storms
		boolean storming = netStormingOverride != null ? netStormingOverride : ClimateManager.isStorming();
		if (storming) {
			lightningTimer -= com.watabou.noosa.Game.elapsed;
			if (lightningTimer <= 0f) {
				add(LightningFlash.flash());
				// Random interval between flashes: 3-12 seconds
				lightningTimer = Random.Float(3f, 12f);
			}
		} else {
			lightningTimer = 0f;
		}
	}

	// =====================================================================
	// INTERNALS
	// =====================================================================

	// How far below its spawn point a drop is tested against the ground (px):
	// roughly where it lands, so the frozen/unfrozen edge reads at the ground
	private static final float RAIN_FALL = 48f;
	private static final float SNOW_FALL = 24f;

	/**
	 * A viewport emitter that, on the surface, only spawns over the ground
	 * its kind belongs to: frozen (snow) or not (rain). Null = anywhere.
	 * Off the surface every spawn goes through.
	 */
	private static class GroundEmitter extends Emitter {
		private final Boolean frozen;
		private final float fall;

		GroundEmitter(Boolean frozen, float fall) {
			this.frozen = frozen;
			this.fall = fall;
		}

		@Override
		protected void emit(int index) {
			float px = x + Random.Float(width), py = y + Random.Float(height);
			if (frozen != null && !groundMatches(px, py + fall, frozen)) return;
			factory.emit(this, index, px, py);
		}
	}

	private static boolean groundMatches(float px, float py, boolean frozen) {
		if (!(Dungeon.level instanceof OverworldLevel)) return true;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int cx = (int) (px / DungeonTilemap.SIZE), cy = (int) (py / DungeonTilemap.SIZE);
		if (px < 0 || py < 0 || cx >= ow.width() || cy >= ow.height()) return false;
		return ow.frozenAt(cx + cy * ow.width()) == frozen;
	}

	/** Create a new emitter sized to the current camera viewport + margin. */
	private Emitter createViewportEmitter(Boolean frozen, float fall) {
		Emitter e = new GroundEmitter(frozen, fall);
		e.autoKill = false; // we manage lifecycle ourselves
		repositionToViewport(e);
		return e;
	}

	/** Reposition an emitter to cover the camera viewport + margin. */
	private void repositionToViewport(Emitter e) {
		if (e == null) return;
		Camera cam = Camera.main;
		if (cam == null) return;
		e.pos(
			cam.scroll.x - MARGIN,
			cam.scroll.y - MARGIN,
			cam.width + MARGIN * 2f,
			cam.height + MARGIN * 2f
		);
	}

	/** Stop an emitter from producing new particles but let existing ones drain. */
	private void retireEmitter(Emitter e) {
		if (e == null) return;
		e.on = false;
		e.autoKill = true; // self-destruct once particles expire
	}

	/** Convert precipitation rate (0–1) to emitter interval (seconds). */
	private float precipInterval(PrecipType type, float rate) {
		float minInterval, maxInterval;
		switch (type) {
			case RAIN:     minInterval = 0.003f;  maxInterval = 0.045f;  break;
			case SNOW:     minInterval = 0.0075f; maxInterval = 0.055f;  break;
			case HAIL:     minInterval = 0.0055f; maxInterval = 0.0375f; break;
			case SLEET:    minInterval = 0.00375f;maxInterval = 0.045f;  break;
			case BLIZZARD: minInterval = 0.002f;  maxInterval = 0.011f;  break;
			default:       minInterval = 0.025f;  maxInterval = 0.075f;
		}
		return maxInterval + (minInterval - maxInterval) * rate;
	}

	/** Kill all emitters and reset state. */
	private void clearAll() {
		killEmitter(ambientEmitter);  ambientEmitter = null;
		killEmitter(ambientEmitter2); ambientEmitter2 = null;
		killEmitter(precipEmitter);   precipEmitter = null;
		killEmitter(sleetSnowEmitter); sleetSnowEmitter = null;
		activePrecipType = PrecipType.NONE;
		activePrecipRate = 0f;
		activeAmbient = AmbientType.NONE;
	}

	private void killEmitter(Emitter e) {
		if (e == null) return;
		e.on = false;
		e.killAndErase();
	}
}

