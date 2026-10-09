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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SteamParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientPlayer;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherSounds;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Manages all weather-related particle layers in the game scene.
 * <p>
 * Emitters are constrained to the camera viewport plus a small margin,
 * and repositioned every frame. This means particles only spawn in the
 * visible area — FogOfWar on top handles masking non-FOV tiles.
 * <p>
 * Layers (bottom to top):
 * <ol>
 *   <li>Veil — the faint wash heavy weather lays over everything, and a sandstorm's haze</li>
 *   <li>Ambient — fireflies, falling leaves, petals (flavor, non-precipitation)</li>
 *   <li>Precipitation — rain, snow, hail, sleet driven by climate conditions</li>
 *   <li>Sand — a sandstorm's dust puffs and driven sand, where its sound plays</li>
 *   <li>Blizzard — where its sound plays: driven snow over the snowfall, a white vignette closing in
 *   from the screen's edges and whiteouts in the strong gusts</li>
 *   <li>Lightning — the bolt (a storm's or a storm cloud's), its arcs, the flash, and its thunder;
 *   and every other lightning of the game (Lightning), drawn here over the day/night tint as light
 *   is, with the storm's flash and crack where it strikes in the hero's sight (struck)</li>
 * </ol>
 * Ambient, Precipitation, Sand and Blizzard lie in the order they began: each new layer goes on top.
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
	private ColorBlock haze;           // the sandstorm's tan haze
	private GroundEmitter sandEmitter; // the sandstorm's driven sand
	private GroundEmitter puffEmitter; // and its dust puffs
	private float sandAmount = 0f;     // the sandstorm on screen, 0..1, eased toward sandstorm()
	private float sandTarget = 0f, sandCheckIn = 0f; // sandstorm() as last looked at, and when next
	private boolean stilled;           // the ambient flavour stilled by the sand or the blizzard
	private Image vignette;            // the blizzard's white closing in from the screen's edges
	private ColorBlock whiteout;       // and its whiteouts in the strong gusts
	private GroundEmitter blizStreaks; // its streaks driven fastest
	private GroundEmitter blizFlakes;  // and its flakes blown along with them
	private float blizAmount = 0f;     // the blizzard on screen, 0..1, eased toward blizzard()
	private float blizTarget = 0f, blizCheckIn = 0f; // blizzard() as last looked at, and when next
	private float blizLight = 1f;      // the light on the ground, its whites darkened by it
	private float vignetteSpread = 1f; // how far past the screen the vignette is stretched
	private float whiteoutLeft = 0f, whiteoutLife = 1f, whiteoutWait = 0f; // the whiteout's clock
	private float lastGust = Float.NaN; // the gusting a frame ago: a whiteout rises with it

	// ---- Current state ----
	private PrecipType activePrecipType = PrecipType.NONE;
	private float activePrecipRate = 0f;
	private AmbientType activeAmbient = AmbientType.NONE;
	private float lightningTimer = 0f; // countdown to next lightning flash
	private float clock = 0f;          // its own frames' clock, real seconds, for the gates below
	private Group sparks;              // the sparks where the bolts' arcs land, made with the first
	private int stepsLeft;             // the steps over the water a frame's bolts may still draw

	// The turns run many a second (resting, a long walk), and a storm's bolts with them: the
	// sky flashes and shakes no more often than this, in real seconds, whatever flashes it (a
	// storm's bolt, sheet lightning, or any other lightning struck in sight: struck)
	static final float FLASH_GAP = 0.7f;
	final Gate flashes = new Gate(FLASH_GAP);
	// ...and the crack of a lightning of the game struck in sight no more often than this: each
	// rolls on for 6.4 s, and a wand zapped every turn must not pile them up
	static final float THUNDER_GAP = 2.5f;
	final Gate cracks = new Gate(THUNDER_GAP);
	// ...and their crawls over the water (StormStrikes.discharge) draw no more steps in a frame,
	// all of them together, than one crawl's whole 7x7 window: beside a storm cloud a frame may
	// bring many bolts
	private static final int STEPS_A_FRAME = 48;
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
		if (haze == null) {
			haze = new ColorBlock(1, 1, 0xFFFFFFFF);
			haze.hardlight(SAND_HAZE_COLOR);
			haze.am = 0;
			add(haze);
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

	/**
	 * How hard the sandstorm blows on screen, 0..1: nothing but where its sound plays
	 * (WeatherSounds.sandstorm: the desert's open sky, 10 m/s and more), from SAND_FLOOR at
	 * 10 m/s to full by SAND_FULL.
	 */
	public static float sandstorm() {
		if (!WeatherSounds.sandstorm(Dungeon.level)) return 0f;
		float t = (ClimateManager.localWindSpeed() - SAND_WIND) / (SAND_FULL - SAND_WIND);
		return SAND_FLOOR + (1f - SAND_FLOOR) * Math.max(0f, Math.min(1f, t));
	}

	/**
	 * How hard the blizzard blows on screen, 0..1: nothing but where its sound plays
	 * (WeatherSounds.blizzard: its bed laid on snowed-under ground beside the hero), from BLIZ_FLOOR
	 * up by its rate (full at BLIZ_RATE) and its wind (BLIZ_WIND to BLIZ_WIND_FULL) alike.
	 */
	public static float blizzard() {
		if (!WeatherSounds.blizzard(Dungeon.level)) return 0f;
		float rate = Math.max(0f, Math.min(1f, ClimateManager.localPrecipRate() / BLIZ_RATE));
		float wind = Math.max(0f, Math.min(1f, (ClimateManager.localWindSpeed() - BLIZ_WIND) / (BLIZ_WIND_FULL - BLIZ_WIND)));
		return BLIZ_FLOOR + (1f - BLIZ_FLOOR) * (rate + wind) / 2f;
	}

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
		repositionToViewport(sandEmitter);
		repositionToViewport(puffEmitter);
		repositionToViewport(blizStreaks);
		repositionToViewport(blizFlakes);

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

		// Lightning during storms, and out of the storm clouds. The ground bolts come down on the
		// game's turns (StormStrikes and StormCloud, or the host's through NetVisuals on a guest)
		// and are only drawn here: each one in sight and its arcs where they are seen, but the
		// flash and the thunder, of the nearest, once in FLASH_GAP at most. A storm cloud's out of
		// sight goes unseen and unheard, but for its arcs in sight
		clock += Game.elapsed;
		stepsLeft = STEPS_A_FRAME;
		int nearest = -1;
		StormStrikes.Bolt bolt;
		while ((bolt = StormStrikes.next()) != null) {
			if (bolt.level != Dungeon.level || Dungeon.hero == null) continue;
			if (!strike(bolt) && bolt.cloud) continue;
			if (nearest == -1 || Dungeon.level.distance(Dungeon.hero.pos, bolt.cell)
					< Dungeon.level.distance(Dungeon.hero.pos, nearest)) nearest = bolt.cell;
		}
		if (nearest != -1 && flashes.pass(clock)) {
			add(LightningFlash.flash());
			thunder(nearest, Dungeon.level.distance(Dungeon.hero.pos, nearest));
		}
		// Between them the sky flashes on its own, sheet lightning behind the clouds with only
		// a far roll after it: it touches nothing, so it keeps the frames' clock
		boolean storming = netStormingOverride != null ? netStormingOverride : ClimateManager.isStorming();
		if (storming) {
			lightningTimer -= Game.elapsed;
			if (lightningTimer <= 0f) {
				add(LightningFlash.flash());
				thunder(-1, Random.Float(10f, 24f));
				flashes.close(clock);
				//the harder the rain and wind, the less time between flashes, and now
				//and then they come in a quick double
				float fury = StormStrikes.fury();
				float gap = Random.Float(4f, 10f) - fury * Random.Float(1.5f, 4f);
				if (Random.Float() < 0.2f) gap = Random.Float(0.25f, 0.5f);
				lightningTimer = Math.max(0.2f, gap);
			}
		} else {
			lightningTimer = 0f;
		}

		updateVeil(storming);
		updateSand();
		updateBlizzard();
		still(sandAmount > 0.05f || blizAmount > 0.05f);
	}

	/**
	 * The sandstorm: sand driven low along the ground with the wind, puffs of dust rolling
	 * after it, and a tan haze over everything, thicker the harder it blows and surging with
	 * the gusts. Eased in and out; its emitters are made the first time it blows and kept.
	 */
	private void updateSand() {
		//looked at twice a second (the biome under the hero is a sample of the world model)
		if ((sandCheckIn -= Game.elapsed) <= 0f) {
			sandCheckIn = SAND_CHECK;
			//not while the overworld's window moves: its map and the hero's cell are not in one
			//frame (WeatherSounds holds too), what blows holds
			if (Dungeon.level == null || !Dungeon.level.fogHeld()) sandTarget = sandstorm();
			if (haze != null) lightHaze();
		}
		sandAmount += (sandTarget - sandAmount) * Math.min(1f, Game.elapsed * 0.8f);
		if (sandTarget == 0f && sandAmount < 0.005f) sandAmount = 0f;
		boolean blowing = sandAmount > 0.05f;
		if (blowing && sandEmitter == null) {
			puffEmitter = (GroundEmitter) createViewportEmitter(null, 0f);
			puffEmitter.pour(DustParticle.PUFF, SAND_PUFF_MAX);
			add(puffEmitter);
			sandEmitter = (GroundEmitter) createViewportEmitter(null, 0f);
			sandEmitter.pour(DustParticle.DRIVEN, SAND_GRAIN_MAX);
			add(sandEmitter);
		}
		if (sandEmitter != null) {
			sandEmitter.on = puffEmitter.on = blowing;
			sandEmitter.base(SAND_GRAIN_MAX + (SAND_GRAIN_MIN - SAND_GRAIN_MAX) * sandAmount);
			puffEmitter.base(SAND_PUFF_MAX + (SAND_PUFF_MIN - SAND_PUFF_MAX) * sandAmount);
		}
		if (haze == null) return;
		float alpha = sandAmount * (SAND_HAZE + SAND_HAZE_GUST * WeatherSprites.gust());
		Camera cam = Camera.main;
		if (cam != null) {
			haze.size(cam.width + MARGIN * 2f, cam.height + MARGIN * 2f);
			haze.x = cam.scroll.x - MARGIN;
			haze.y = cam.scroll.y - MARGIN;
		}
		haze.am = alpha;
		haze.visible = alpha > 0.002f;
	}

	/**
	 * The calm weather's flavour (motes, petals, fireflies) is stilled while the sand or the
	 * blizzard blows and comes back after, unless heavy precipitation still holds it; heat that
	 * hurts still shows.
	 */
	private void still(boolean blowing) {
		if (blowing && activeAmbient != AmbientType.HEAT_RAYS) {
			if (ambientEmitter != null) ambientEmitter.on = false;
			if (ambientEmitter2 != null) ambientEmitter2.on = false;
			stilled = true;
		} else if (stilled) {
			stilled = false;
			if (activePrecipRate <= 0.5f) {
				if (ambientEmitter != null) ambientEmitter.on = true;
				if (ambientEmitter2 != null) ambientEmitter2.on = true;
			}
		}
	}

	/**
	 * The blizzard, where it is heard: streaks and flakes driven along the wind over the snowfall,
	 * a white vignette closing in from the screen's edges as it thickens and pulsing thicker in the
	 * gusts, and a brief whiteout when a strong gust comes. Its whites are lit as the ground under
	 * them. Eased in and out; made the first time it blows and kept.
	 */
	private void updateBlizzard() {
		//looked at twice a second, as the sand is, and held while the overworld's window moves
		if ((blizCheckIn -= Game.elapsed) <= 0f) {
			blizCheckIn = SAND_CHECK;
			if (Dungeon.level == null || !Dungeon.level.fogHeld()) blizTarget = blizzard();
			blizLight = groundLight();
		}
		blizAmount += (blizTarget - blizAmount) * Math.min(1f, Game.elapsed * 0.8f);
		if (blizTarget == 0f && blizAmount < 0.005f) blizAmount = 0f;
		boolean blowing = blizAmount > 0.05f;
		if (blowing && blizStreaks == null) {
			blizFlakes = (GroundEmitter) createViewportEmitter(true, SNOW_FALL);
			blizFlakes.pour(AmbientSnowParticle.DRIVEN, BLIZ_FLAKE_MAX);
			add(blizFlakes);
			blizStreaks = (GroundEmitter) createViewportEmitter(true, SNOW_FALL);
			blizStreaks.pour(AmbientSnowParticle.GALE, BLIZ_STREAK_MAX);
			add(blizStreaks);
			vignette = new Image(WeatherSprites.vignette());
			add(vignette);
			whiteout = new ColorBlock(1, 1, 0xFFFFFFFF);
			add(whiteout);
		}
		if (blizStreaks == null) return;
		blizStreaks.on = blizFlakes.on = blowing;
		blizStreaks.base(BLIZ_STREAK_MAX + (BLIZ_STREAK_MIN - BLIZ_STREAK_MAX) * blizAmount);
		blizFlakes.base(BLIZ_FLAKE_MAX + (BLIZ_FLAKE_MIN - BLIZ_FLAKE_MAX) * blizAmount);

		//a whiteout as a strong gust rises, the longer the thicker the blizzard, one at a time
		float gust = WeatherSprites.gust();
		whiteoutWait -= Game.elapsed;
		whiteoutLeft -= Game.elapsed;
		if (blizAmount >= WHITEOUT_FROM && lastGust < WHITEOUT_GUST && gust >= WHITEOUT_GUST && whiteoutWait <= 0f) {
			whiteoutLife = whiteoutLeft = WHITEOUT_MIN + (WHITEOUT_MAX - WHITEOUT_MIN) * blizAmount;
			whiteoutWait = WHITEOUT_EVERY;
		}
		lastGust = gust;
		float white = whiteoutLeft > 0f
				? blizAmount * WHITEOUT_ALPHA * (float) Math.sin(Math.PI * (1f - whiteoutLeft / whiteoutLife)) : 0f;

		//the vignette: stretched past the screen while the blizzard is light, closing in to its edges
		//as it thickens, sooner in the gusts (placed as it is drawn: placeVignette)
		float close = Math.max(0f, Math.min(1f, blizAmount * (1f + VIGNETTE_GUST * gust)));
		vignetteSpread = 1f + VIGNETTE_SPREAD * (1f - close);
		Camera cam = Camera.main;
		if (cam != null) {
			whiteout.size(cam.width + MARGIN * 2f, cam.height + MARGIN * 2f);
			whiteout.x = cam.scroll.x - MARGIN;
			whiteout.y = cam.scroll.y - MARGIN;
		}
		vignette.hardlight(BLIZ_WHITE_R * blizLight, BLIZ_WHITE_G * blizLight, blizLight);
		whiteout.hardlight(BLIZ_WHITE_R * blizLight, BLIZ_WHITE_G * blizLight, blizLight);
		vignette.am = blizAmount * VIGNETTE_ALPHA;
		vignette.visible = vignette.am > 0.002f;
		whiteout.am = white;
		whiteout.visible = white > 0.002f;
	}

	@Override
	public void draw() {
		placeVignette();
		super.draw();
	}

	//the vignette on the camera as it is drawn: the camera follows and pans after update()
	//(Game.update), and unlike the washes the vignette has no margin to hide that frame's lag,
	//so it would bare a strip at its leading edge. It overhangs the screen by the flash's shake,
	//which only the camera's matrix knows
	private void placeVignette() {
		Camera cam = Camera.main;
		if (vignette == null || cam == null) return;
		float w = cam.width * vignetteSpread + VIGNETTE_SHAKE * 2f, h = cam.height * vignetteSpread + VIGNETTE_SHAKE * 2f;
		vignette.scale.set(w / vignette.width, h / vignette.height);
		vignette.x = cam.scroll.x + (cam.width - w) / 2f;
		vignette.y = cam.scroll.y + (cam.height - h) / 2f;
	}

	/**
	 * The light on the ground, 0..1: the haze and the blizzard's whites are drawn over the
	 * day/night tint, so they are darkened as GameScene.updateDayNightTint darkens the world (the
	 * dark of a low light and the sky tint's alpha), never a pale wash over a dark scene.
	 */
	private static float groundLight() {
		float dark = 1f - DayNightCycle.brightnessMult() + DayNightCycle.phaseTintSmooth()[0];
		return 1f - Math.max(0f, Math.min(1f, dark));
	}

	/** The haze is lit as the ground under it is (groundLight): the sand's tan, darkened. */
	private void lightHaze() {
		float l = groundLight();
		haze.hardlight(((SAND_HAZE_COLOR >> 16) & 0xFF) / 255f * l,
				((SAND_HAZE_COLOR >> 8) & 0xFF) / 255f * l, (SAND_HAZE_COLOR & 0xFF) / 255f * l);
	}

	/**
	 * The wash heavy weather lays over the world: rain greys it a little, a
	 * blizzard whitens it (lit as the ground under it, whiter where it is heard,
	 * the vignette's and the whiteouts' white over it), a storm darkens it. Faint,
	 * and eased in and out.
	 */
	private void updateVeil(boolean storming) {
		if (veil == null) return;
		float target = 0f;
		int color = 0xFFFFFF;
		switch (activePrecipType) {
			case RAIN:     target = 0.10f * activePrecipRate; color = 0x98AEC8; break;
			case SLEET:    target = 0.08f * activePrecipRate; color = 0xB4C0CC; break;
			case SNOW:     target = 0.06f * activePrecipRate; color = 0xF0F4FF; break;
			case BLIZZARD: target = 0.14f * activePrecipRate + 0.08f * blizAmount; color = blizzardWhite(); break;
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

	//the blizzard's white (F4F8FF) darkened by the light on the ground
	private int blizzardWhite() {
		int r = Math.round(BLIZ_WHITE_R * blizLight * 255), g = Math.round(BLIZ_WHITE_G * blizLight * 255), b = Math.round(blizLight * 255);
		return (r << 16) | (g << 8) | b;
	}

	/** how fast the heat rays pour, by how hot it feels */
	private static float heatRayInterval() {
		float over = ClimateManager.feelsLikeTemp() - ClimateManager.HEAT_RAY_TEMP;
		float t = Math.min(1f, Math.max(0f, over / 14f));
		return 0.22f - 0.17f * t;
	}

	/**
	 * A turn's bolt (StormStrikes, StormCloud: `cloud`) on its cell: where the hero can see it, it
	 * comes down from the top of the view leaning with the wind, or, a storm cloud's where the sky
	 * is not open overhead, out of the cloud deck it draws two cells up (not through the walls),
	 * as short in its zigzag and fork; whether it did. Where it ran (StormStrikes.discharge) is
	 * drawn with it, or alone, wherever the hero sees it, its crawl over the water as a faint
	 * crackle (Lightning.Arc.crawl). The flash and the thunder are update()'s, and the sparks are
	 * the overlay's own: its Lightning makes none of the three (stormsOwn).
	 */
	private boolean strike(StormStrikes.Bolt bolt) {
		Camera cam = Camera.main;
		boolean[] fov = Dungeon.level.heroFOV;
		if (cam == null || fov == null) return false;
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		boolean seen = fov[bolt.cell];
		PointF to = DungeonTilemap.tileCenterToWorld(bolt.cell);
		if (seen) {
			boolean low = bolt.cloud && !StormStrikes.underOpenSky(Dungeon.level);
			float k = low ? CLOUD_BOLT : 1f;
			float lean = low ? Random.Float(-10f, 10f)
					: (float)Math.sin(Math.toRadians(ClimateManager.surfaceWindDir())) * ClimateManager.localWindSpeed() * 1.5f;
			PointF from = new PointF(to.x - lean, low ? to.y - Random.Float(34f, 52f) : cam.scroll.y - 12f);
			//the channel: a zigzag of five or six legs, and a fork off one of the upper joints
			//that dies out short of the ground (Lightning.Arc: every lightning's look); its glow
			//the strike's own
			arcs.add(new Lightning.Arc(from, to, 5 + Random.Int(2), k, 0.7f, false));
		}
		//its arcs from one it hit to the next, with three sparks on each, and its crawl over the
		//water, a spark on each cell, as many steps of it as the frame has left (nearest first)
		for (int i = 0; i + 1 < bolt.arcs.length; i += 2) arc(bolt.arcs[i], bolt.arcs[i + 1], 3, false, fov, arcs);
		for (int i = 0; i + 1 < bolt.water.length && stepsLeft > 0; i += 2) {
			if (arc(bolt.water[i], bolt.water[i + 1], 1, true, fov, arcs)) stepsLeft--;
		}
		if (!arcs.isEmpty()) add(new Lightning(arcs, null).stormsOwn());
		if (seen) add(new StrikeGlow(to.x, to.y));
		return seen;
	}

	//one of a bolt's arcs, where the hero sees either end (a step of its crawl over the water a
	//faint crackle, lit with the strike), and n sparks where he sees it land; whether it was drawn
	private boolean arc(int from, int to, int n, boolean water, boolean[] fov, ArrayList<Lightning.Arc> into) {
		if (!fov[from] && !fov[to]) return false;
		Lightning.Arc arc = new Lightning.Arc(from, to);
		into.add(water ? arc.crawl(0) : arc);
		if (!fov[to]) return true;
		PointF p = DungeonTilemap.tileCenterToWorld(to);
		spark(p.x, p.y, n);
		return true;
	}

	/**
	 * n sparks at a point of the world, from the overlay's own emitters: over the day/night tint
	 * with the lightning that throws them, and never sent on to a co-op guest as one of
	 * CellEmitter's would be (a guest draws a storm bolt's sparks from its arcs himself).
	 */
	void spark(float x, float y, int n) {
		if (sparks == null) {
			sparks = new Group();
			add(sparks);
		}
		Emitter e = (Emitter) sparks.recycle(Emitter.class);
		e.revive();
		e.pos(x, y);
		e.burst(SparkParticle.FACTORY, n);
	}

	/**
	 * A lightning of the game (Lightning: a wand's, a spell's, a monster's) struck the cell in the
	 * hero's sight, on its first frame: the storm's flash over the view, unless the sky flashed in
	 * the last FLASH_GAP (a storm's bolts and these alike), and from the cell the storm's crack for a
	 * bolt in sight, unless one of these cracked in the last THUNDER_GAP. Render thread.
	 */
	void struck(int cell) {
		if (flashes.pass(clock)) add(LightningFlash.flash());
		if (cracks.pass(clock)) crack(cell, CRACK);
	}

	/**
	 * A wand of lightning's zap struck the cell (Lightning.thunderous): the storm's crack from it
	 * every time, seen or not and whatever cracked before, closing the gate on the others' for
	 * THUNDER_GAP; the flash where the hero sees it, under the flashes' gate as ever. Render thread.
	 */
	void thunder(int cell) {
		boolean[] fov = Dungeon.level == null ? null : Dungeon.level.heroFOV;
		if (fov != null && cell >= 0 && cell < fov.length && fov[cell] && flashes.pass(clock)) {
			add(LightningFlash.flash());
		}
		cracks.close(clock);
		crack(cell, WAND_CRACK);
	}

	/**
	 * The storm's crack for a bolt in sight (WeatherSounds: a take of THUNDER_NEAR, as dull as it is
	 * far, starting with its flash, from its side), for a lightning of the game that struck `cell`,
	 * at `loud` close by: on the effects' channel, so it is heard where the weather is not, over the
	 * caller's own zap.
	 */
	private void crack(int cell, float loud) {
		if (Dungeon.level == null || Dungeon.hero == null) return;
		int w = Dungeon.level.width();
		float tiles = Dungeon.level.distance(Dungeon.hero.pos, cell);
		AmbientSound near = AmbientSound.THUNDER_NEAR;
		SpatialSound.playPanned(near.takes[Random.Int(near.takes.length)],
				WeatherSounds.thunderDelay(tiles, false, true), crackVolume(tiles, loud),
				1f + Random.Float(-AmbientPlayer.PITCH_SPREAD, AmbientPlayer.PITCH_SPREAD),
				SpatialSound.pan(cell % w - Dungeon.hero.pos % w));
		cracked++;
	}

	//how loud a lightning of the game cracks close by, and a wand of lightning's own zap: on the
	//effects' channel the storm's mix (THUNDER_NEAR's 0.5, a crack peaking near -25 LUFS) sat under
	//a sword's hit (-21) and far under a bomb (-13); 0.85 brings it to a heavy hit's, a wand's to -19
	static final float CRACK = 0.85f, WAND_CRACK = 1f;

	//the cracks played, for the tests
	int cracked;

	/** The crack's volume `tiles` away, `loud` close by: as dull as it is far (thunderLevel). */
	static float crackVolume(float tiles, float loud) {
		return loud * thunderLevel(tiles);
	}

	//a strike's thunder, as dull as it is far (WeatherScape.thunderLevel)
	private static float thunderLevel(float tiles) {
		return Math.max(0.25f, 1f - tiles * 0.035f);
	}

	/**
	 * Draws a lightning of the game here, with the storm's bolts over the day/night tint: its light
	 * is not darkened by the night or the depths, and it lies over the flash it brought. Its frames
	 * (and its callback) stay with whoever added it; the stand-in goes when it does. Render thread.
	 */
	Gizmo carry(Lightning l) {
		return add(new Carried(l));
	}

	//a lightning's stand-in on this layer: it draws it, nothing more
	private static final class Carried extends Gizmo {
		private final Lightning l;

		Carried(Lightning l) {
			this.l = l;
		}

		@Override
		public void update() {
			//one its parent let go of before it ended goes with it
			if (!l.exists || l.parent == null) killAndErase();
		}

		@Override
		public void draw() {
			if (l.exists && l.parent != null && l.isVisible()) l.drawLit();
		}
	}

	/**
	 * A rate limit on a clock handed in (the overlay's own, real seconds as its frames run): it lets
	 * one through, then none until `gap` has gone by.
	 */
	static final class Gate {
		private final float gap;
		private float last = Float.NEGATIVE_INFINITY;

		Gate(float gap) {
			this.gap = gap;
		}

		/** Whether one may go through at `now`. */
		boolean open(float now) {
			return now - last >= gap;
		}

		/** One went through at `now`, whether or not it was asked: none more until the gap is gone. */
		void close(float now) {
			last = now;
		}

		/** Lets one through at `now` if it is open; whether it did. */
		boolean pass(float now) {
			if (!open(now)) return false;
			close(now);
			return true;
		}
	}

	/**
	 * The thunder of a strike `tiles` away, or of sheet lightning (cell -1). It is the
	 * weather's own (WeatherSounds): a crack and its roll from the strike's side with the
	 * flash for a bolt in sight or near, or a far roll within half a second. Where the weather
	 * is not heard now, or its thunder would not ring with a bolt in sight (in its first quiet,
	 * over a crack still rolling), the old crack plays instead, as soon after the flash.
	 */
	private void thunder(int cell, float tiles) {
		float dx = cell >= 0 ? cell % Dungeon.level.width() - Dungeon.hero.pos % Dungeon.level.width() : 0f;
		boolean seen = cell >= 0 && Dungeon.level.heroFOV != null && Dungeon.level.heroFOV[cell];
		if (WeatherSounds.strike(dx, tiles, cell < 0, seen)) return;
		//it dulls with distance
		float delay = WeatherSounds.thunderDelay(tiles, cell < 0, seen);
		float volume = thunderLevel(tiles);
		float pitch = cell >= 0 ? Random.Float(0.85f, 1.05f) : Random.Float(0.6f, 0.75f);
		//it already dulls with distance its own way: spatial sound only gives it its side
		SpatialSound.playPanned(Assets.Sounds.LIGHTNING, delay, volume, pitch, SpatialSound.pan(dx));
	}

	// =====================================================================
	// INTERNALS
	// =====================================================================

	// The sandstorm: from SAND_FLOOR of its full at 10 m/s (where its sound starts) to full at
	// SAND_FULL; the sand's and the puffs' intervals (s) from barely blowing to full, before the
	// gusts; the haze's alpha at full, give or take SAND_HAZE_GUST with the gusting
	private static final float SAND_WIND = 10f, SAND_FULL = 20f, SAND_FLOOR = 0.3f;
	private static final float SAND_GRAIN_MAX = 0.03f, SAND_GRAIN_MIN = 0.006f;
	private static final float SAND_PUFF_MAX = 0.9f, SAND_PUFF_MIN = 0.3f;
	private static final float SAND_HAZE = 0.24f, SAND_HAZE_GUST = 0.05f;
	private static final int SAND_HAZE_COLOR = 0xC8A064;
	private static final float SAND_CHECK = 0.5f;

	// The blizzard: from BLIZ_FLOOR of its full where it is heard, up by its rate (full at BLIZ_RATE)
	// and its wind (BLIZ_WIND to BLIZ_WIND_FULL m/s) alike. The driven streaks' and flakes'
	// intervals (s) from the lightest to full, before the gusts: at most about 290 a second over
	// the snowfall's own (capped at 220 streaks and 90 flakes a second at its heaviest)
	private static final float BLIZ_FLOOR = 0.35f, BLIZ_RATE = 0.6f, BLIZ_WIND = 10f, BLIZ_WIND_FULL = 20f;
	private static final float BLIZ_STREAK_MAX = 0.016f, BLIZ_STREAK_MIN = 0.006f;
	private static final float BLIZ_FLAKE_MAX = 0.02f, BLIZ_FLAKE_MIN = 0.008f;
	// its white, F4F8FF, as the red and green shares of the blue
	private static final float BLIZ_WHITE_R = 0xF4 / 255f, BLIZ_WHITE_G = 0xF8 / 255f;
	// the vignette: its alpha at full; stretched this much past the screen at nothing, at its edges
	// at full; the gusts close it in by this share more, to the edges at most; past them by the
	// flash's shake (LightningFlash: 2 px)
	private static final float VIGNETTE_ALPHA = 0.9f, VIGNETTE_SPREAD = 0.6f, VIGNETTE_GUST = 0.3f;
	private static final float VIGNETTE_SHAKE = 2f;
	// a storm cloud's bolt under a roof, out of its cloud deck: its zigzag and fork this share of
	// the sky's
	private static final float CLOUD_BOLT = 0.4f;
	// a whiteout: from a blizzard this thick, as the gusting rises through WHITEOUT_GUST, at most
	// one in WHITEOUT_EVERY s; WHITEOUT_MIN to WHITEOUT_MAX s long by how thick, its peak alpha
	// WHITEOUT_ALPHA at full
	private static final float WHITEOUT_FROM = 0.4f, WHITEOUT_GUST = 0.75f, WHITEOUT_EVERY = 5f;
	private static final float WHITEOUT_MIN = 0.3f, WHITEOUT_MAX = 0.8f, WHITEOUT_ALPHA = 0.85f;

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

		/** a new interval to pour at, the gusts still on top of it */
		void base(float interval) {
			baseInterval = interval;
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
			case BLIZZARD: minInterval = 0.0045f; maxInterval = 0.011f;  break;
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
			//its image lets go of its vertex buffer now, not when the scene changes
			if (left <= 0) { killAndErase(); destroy(); return; }
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
		killEmitter(sandEmitter);     sandEmitter = null;
		killEmitter(puffEmitter);     puffEmitter = null;
		killEmitter(blizStreaks);     blizStreaks = null;
		killEmitter(blizFlakes);      blizFlakes = null;
		sandAmount = sandTarget = sandCheckIn = 0f;
		blizAmount = blizTarget = blizCheckIn = 0f;
		stilled = false;
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

