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
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
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
 *   <li>Veil — the faint wash heavy weather lays over everything</li>
 *   <li>Ambient — fireflies, falling leaves, petals (flavor, non-precipitation)</li>
 *   <li>Precipitation — rain, snow, hail, sleet driven by climate conditions</li>
 *   <li>Lightning — the bolt, the flash, and thunder that arrives late from afar</li>
 * </ol>
 */
public class WeatherOverlay extends Group {

	// Margin around viewport for particle spawning (pixels).
	// Prevents pop-in at edges as camera scrolls.
	private static final float MARGIN = 32f;

	// ---- Emitter layers ----
	private ColorBlock veil;
	private float veilAlpha = 0f;
	private Emitter ambientEmitter;
	private Emitter ambientEmitter2;
	private Emitter precipEmitter;
	private Emitter sleetSnowEmitter; // secondary emitter for sleet's snow component
	private Emitter driftEmitter;     // snow the wind lifts off frozen ground

	// ---- Current state ----
	private PrecipType activePrecipType = PrecipType.NONE;
	private float activePrecipRate = 0f;
	private AmbientType activeAmbient = AmbientType.NONE;
	private float lightningTimer = 0f; // countdown to next lightning flash
	private float heatRayPour = -1f;   // the interval the heat rays are pouring at

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
		DRIP,
		HEAT_RAYS
	}

	public WeatherOverlay() {
		super();
	}

	/** Call once after adding to the scene. */
	public void setup() {
		clearAll();
		if (veil == null) {
			veil = new ColorBlock(1, 1, 0xFFFFFFFF);
			veil.am = 0;
			add(veil);
		}
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
		retireEmitter(driftEmitter);
		precipEmitter = null;
		sleetSnowEmitter = null;
		driftEmitter = null;

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
				driftEmitter = createViewportEmitter(true, 0f);
				driftEmitter.pour(AmbientSnowParticle.DRIFT, 0.08f);
				add(driftEmitter);
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
				//driven streaks, and under them the flakes that still manage to fall
				precipEmitter = createViewportEmitter(true, SNOW_FALL);
				precipEmitter.pour(AmbientSnowParticle.BLIZZARD, interval);
				sleetSnowEmitter = createViewportEmitter(true, SNOW_FALL);
				sleetSnowEmitter.pour(AmbientSnowParticle.FACTORY, interval * 2.5f);
				add(sleetSnowEmitter);
				driftEmitter = createViewportEmitter(true, 0f);
				driftEmitter.pour(AmbientSnowParticle.DRIFT, 0.04f);
				add(driftEmitter);
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

		ambientEmitter = type == AmbientType.FIREFLIES ? createGrassEmitter() : createViewportEmitter(null, 0f);
		switch (type) {
			case FIREFLIES:
				ambientEmitter.pour(FireflyParticle.FACTORY, 0.5f);
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
				//the embers' glow, additive, in a layer of its own
				ambientEmitter2 = createViewportEmitter(null, 0f);
				ambientEmitter2.pour(AshParticle.EMBER_GLOW, 0.7f);
				add(ambientEmitter2);
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
			case HEAT_RAYS:
				//the hotter it feels, the thicker the air stands: 0.22s of gap at the
				//threshold down to 0.05s when it is truly baking
				heatRayPour = heatRayInterval();
				ambientEmitter.pour(xyz.gabriwar.warpedpixeldungeon.effects.WeatherBlobFX.HEAT_RAYS, heatRayPour);
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
		repositionToViewport(driftEmitter);
		repositionToViewport(ambientEmitter);
		repositionToViewport(ambientEmitter2);

		// Suppress ambient during heavy precipitation
		if (activePrecipRate > 0.5f) {
			if (ambientEmitter != null && ambientEmitter.on) ambientEmitter.on = false;
			if (ambientEmitter2 != null && ambientEmitter2.on) ambientEmitter2.on = false;
		}

		//the rays thicken and thin with the heat while they are running
		if (activeAmbient == AmbientType.HEAT_RAYS && ambientEmitter != null) {
			float want = heatRayInterval();
			if (Math.abs(want - heatRayPour) > 0.02f) {
				heatRayPour = want;
				ambientEmitter.pour(xyz.gabriwar.warpedpixeldungeon.effects.WeatherBlobFX.HEAT_RAYS, want);
			}
		}

		// Lightning during storms: a flash, most times a bolt, and thunder after it
		boolean storming = netStormingOverride != null ? netStormingOverride : ClimateManager.isStorming();
		if (storming) {
			lightningTimer -= Game.elapsed;
			if (lightningTimer <= 0f) {
				add(LightningFlash.flash());
				strike();
					//a real storm keeps flashing: the harder the rain and wind, the less
				//time between bolts, and every few of them come in a quick double
				float fury = Math.min(1f, Math.max(ClimateManager.localPrecipRate(),
						ClimateManager.localWindSpeed() / 18f));
				float gap = Random.Float(2.6f, 7f) - fury * Random.Float(1.4f, 4f);
				if (Random.Float() < 0.28f) gap = Random.Float(0.25f, 0.5f);
				lightningTimer = Math.max(0.2f, gap);
			}
		} else {
			lightningTimer = 0f;
		}

		updateVeil(storming);
	}

	/**
	 * The wash heavy weather lays over the world: rain greys it a little, a
	 * blizzard whitens it, a storm darkens it. Faint, and eased in and out.
	 */
	private void updateVeil(boolean storming) {
		if (veil == null) return;
		float target = 0f;
		int color = 0xFFFFFF;
		switch (activePrecipType) {
			case RAIN:     target = 0.10f * activePrecipRate; color = 0x98AEC8; break;
			case SLEET:    target = 0.08f * activePrecipRate; color = 0xB4C0CC; break;
			case SNOW:     target = 0.06f * activePrecipRate; color = 0xF0F4FF; break;
			case BLIZZARD: target = 0.22f * activePrecipRate; color = 0xF4F8FF; break;
			case HAIL:     target = 0.06f * activePrecipRate; color = 0xC0C8D0; break;
			default:       target = 0f;
		}
		if (storming) { target += 0.05f; color = activePrecipType == PrecipType.NONE ? 0x404858 : color; }
		veilAlpha += (target - veilAlpha) * Math.min(1f, Game.elapsed * 1.5f);
		Camera cam = Camera.main;
		if (cam != null) {
			veil.size(cam.width + MARGIN * 2f, cam.height + MARGIN * 2f);
			veil.x = cam.scroll.x - MARGIN;
			veil.y = cam.scroll.y - MARGIN;
		}
		veil.hardlight(color);
		veil.am = veilAlpha;
		veil.visible = veilAlpha > 0.002f;
	}

	/**
	 * A bolt comes down on a spot the hero can see, leaning with the wind, and the
	 * thunder follows as late and as quiet as the strike is far. When no spot is in
	 * view the flash was sheet lightning behind the clouds, and only a rumble comes.
	 */
	/** how fast the heat rays pour, by how hot it feels */
	private static float heatRayInterval() {
		float over = ClimateManager.feelsLikeTemp() - ClimateManager.HEAT_RAY_TEMP;
		float t = Math.min(1f, Math.max(0f, over / 14f));
		return 0.22f - 0.17f * t;
	}

	private void strike() {
		Camera cam = Camera.main;
		if (cam == null || Dungeon.level == null || Dungeon.level.heroFOV == null) return;
		int w = Dungeon.level.width(), h = Dungeon.level.height();
		int cell = -1;
		if (Random.Float() < 0.7f) {
			for (int i = 0; i < 14; i++) {
				int cx = (int)((cam.scroll.x + Random.Float(cam.width)) / DungeonTilemap.SIZE);
				int cy = (int)((cam.scroll.y + Random.Float(cam.height)) / DungeonTilemap.SIZE);
				if (cx < 0 || cy < 0 || cx >= w || cy >= h) continue;
				int c = cx + cy * w;
				if (Dungeon.level.heroFOV[c] && !Dungeon.level.solid[c]) { cell = c; break; }
			}
		}
		float tiles;
		if (cell >= 0) {
			PointF to = DungeonTilemap.tileCenterToWorld(cell);
			float lean = (float)Math.sin(Math.toRadians(ClimateManager.surfaceWindDir())) * ClimateManager.localWindSpeed() * 1.5f;
			PointF from = new PointF(to.x - lean, cam.scroll.y - 12f);
			//the channel: a zigzag of five or six arcs, and a fork off one of the
			//upper joints that dies out short of the ground
			java.util.ArrayList<Lightning.Arc> arcs = new java.util.ArrayList<>();
			int steps = 5 + Random.Int(2);
			PointF prev = from;
			PointF forkFrom = null;
			for (int i = 1; i <= steps; i++) {
				float t = i / (float)steps;
				PointF next = i == steps ? to : new PointF(
						from.x + (to.x - from.x) * t + Random.Float(-12f, 12f),
						from.y + (to.y - from.y) * t + Random.Float(-4f, 4f));
				arcs.add(new Lightning.Arc(prev, next));
				if (i == 2) forkFrom = next;
				prev = next;
			}
			if (forkFrom != null && Random.Float() < 0.7f) {
				float side = Random.Int(2) == 0 ? -1 : 1;
				PointF mid = new PointF(forkFrom.x + side * Random.Float(10f, 18f), forkFrom.y + Random.Float(14f, 24f));
				PointF end = new PointF(mid.x + side * Random.Float(6f, 14f), mid.y + Random.Float(10f, 20f));
				arcs.add(new Lightning.Arc(forkFrom, mid));
				arcs.add(new Lightning.Arc(mid, end));
			}
			add(new Lightning(arcs, null));
			add(new StrikeGlow(to.x, to.y));
			tiles = Dungeon.hero != null ? Dungeon.level.distance(Dungeon.hero.pos, cell) : 6f;
		} else {
			tiles = Random.Float(10f, 24f);
		}
		//sound takes its time: about a fifth of a second per tile, and it dulls with distance
		float delay = Math.min(3f, 0.12f + tiles * 0.18f);
		float volume = Math.max(0.25f, 1f - tiles * 0.035f);
		float pitch = cell >= 0 ? Random.Float(0.85f, 1.05f) : Random.Float(0.6f, 0.75f);
		Sample.INSTANCE.playDelayed(Assets.Sounds.LIGHTNING, delay, volume, pitch);
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
		private float baseInterval = -1;

		GroundEmitter(Boolean frozen, float fall) {
			this.frozen = frozen;
			this.fall = fall;
		}

		@Override
		public void update() {
			//the fall comes in gusts: thicker for a few seconds, then thinner
			if (baseInterval < 0) baseInterval = interval;
			if (baseInterval > 0) interval = baseInterval / Math.max(0.5f, 1f + 0.45f * WeatherSprites.gust());
			super.update();
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

	/** A viewport emitter that favours grass and water, where fireflies gather:
	 *  elsewhere most of its spawns are skipped. */
	private Emitter createGrassEmitter() {
		Emitter e = new Emitter() {
			@Override
			protected void emit(int index) {
				float px = x + Random.Float(width), py = y + Random.Float(height);
				if (!fireflyGround(px, py) && Random.Float() < 0.8f) return;
				factory.emit(this, index, px, py);
			}
		};
		e.autoKill = false;
		repositionToViewport(e);
		return e;
	}

	private static boolean fireflyGround(float px, float py) {
		if (Dungeon.level == null || px < 0 || py < 0) return false;
		int cx = (int)(px / DungeonTilemap.SIZE), cy = (int)(py / DungeonTilemap.SIZE);
		if (cx >= Dungeon.level.width() || cy >= Dungeon.level.height()) return false;
		int cell = cx + cy * Dungeon.level.width();
		int t = Dungeon.level.map[cell];
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS
				|| (Dungeon.level.water != null && Dungeon.level.water[cell]);
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

	/** the light a strike throws on the ground where it lands: a broad soft glow,
	 *  additive, gone in a third of a second */
	private static class StrikeGlow extends Group {
		private static final float LIFE = 0.35f;
		private float left = LIFE;
		private final com.watabou.noosa.Image glow;

		StrikeGlow(float x, float y) {
			super();
			glow = new com.watabou.noosa.Image(WeatherSprites.get());
			int[] f = WeatherSprites.GLOW_5;
			glow.frame(f[0], f[1], f[2], f[3]);
			glow.origin.set(f[2] / 2f, f[3] / 2f);
			glow.scale.set(6);
			glow.hardlight(0xD8E4FF);
			glow.x = x - f[2] / 2f;
			glow.y = y - f[3] / 2f;
			add(glow);
		}

		@Override
		public void update() {
			super.update();
			left -= Game.elapsed;
			if (left <= 0) { killAndErase(); return; }
			glow.am = 0.9f * (left / LIFE);
		}

		@Override
		public void draw() {
			com.watabou.glwrap.Blending.setLightMode();
			super.draw();
			com.watabou.glwrap.Blending.setNormalMode();
		}
	}

	/** Kill all emitters and reset state. */
	private void clearAll() {
		killEmitter(ambientEmitter);  ambientEmitter = null;
		killEmitter(ambientEmitter2); ambientEmitter2 = null;
		killEmitter(precipEmitter);   precipEmitter = null;
		killEmitter(sleetSnowEmitter); sleetSnowEmitter = null;
		killEmitter(driftEmitter);    driftEmitter = null;
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

