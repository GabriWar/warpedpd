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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Central climate simulation engine. Computes surface weather state each hero
 * turn based on seasonal baselines, diurnal cycles, and weather fronts.
 * <p>
 * All methods are static. State is persisted via Bundle with the Dungeon save.
 */
public class ClimateManager {

	// =====================================================================
	// SURFACE STATE (sky-level values)
	// =====================================================================

	private static float surfaceTemp;       // °C
	private static float surfaceHumidity;   // 0.0-1.0
	private static float surfacePressure;   // hPa
	private static float surfaceWindSpeed;  // 0-25
	private static float surfaceWindDir;    // degrees 0-360
	private static float cloudCover;        // 0.0-1.0
	private static float precipRate;        // 0.0-1.0
	private static PrecipType precipType = PrecipType.NONE;
	private static WeatherState weatherState = WeatherState.CLEAR;

	// Previous state for transition detection
	private static WeatherState prevWeatherState = WeatherState.CLEAR;
	private static PrecipType prevPrecipType = PrecipType.NONE;

	// =====================================================================
	// WEATHER FRONT QUEUE
	// =====================================================================

	private static ArrayList<WeatherFront> frontQueue = new ArrayList<>();
	private static int frontIndex = 0;       // index of current/next front in queue
	private static long frontGenSeed = 0;    // RNG seed for front generation
	private static int lastFrontGenTurn = 0; // last cycleTurn we generated fronts for

	// =====================================================================
	// DEBUG OVERRIDES — bypass climate simulation (null = disabled)
	// =====================================================================

	/** When non-null, overrides localPrecipType() and localPrecipRate() entirely. */
	public static PrecipType debugForcePrecipType = null;
	public static float      debugForcePrecipRate = 1f;

	// =====================================================================
	// LOCAL (depth-adjusted) VALUES — recomputed on level change
	// =====================================================================

	private static int currentDepth = 1;
	private static float localTemp;
	private static float localHumidity;
	private static float localWindSpeed;
	private static float localPrecipRate;
	private static PrecipType localPrecipType = PrecipType.NONE;

	// =====================================================================
	// LIGHT STATE (surface values, computed per turn)
	// =====================================================================

	private static float solarElevation;     // degrees, negative = below horizon
	private static float sunLight;           // 0.0-1.0, cloud-attenuated solar intensity
	private static float moonLight;          // 0.0-1.0, cloud + phase + visibility
	private static float surfaceLight;       // sunLight + moonLight, clamped 0-1
	private static float moonVisibility;     // 0.0-1.0, is moon above horizon right now?
	private static float moonElevation;      // degrees above horizon
	private static float localAmbientLight;  // depth-attenuated + torch base

	// Solar color: warm at low elevation (dawn/dusk), neutral at high (noon)
	private static float[] solarColorRGB = {1f, 1f, 1f};
	// Lunar color: blue-silver normally, amber for harvest moon
	private static float[] moonColorRGB = {0.65f, 0.70f, 0.90f};

	// Special events
	private static boolean solarEclipseActive = false;
	private static int solarEclipseRemaining = 0;
	private static boolean lunarEclipseActive = false;
	private static int lunarEclipseRemaining = 0;
	private static boolean auroraActive = false;
	private static int auroraRemaining = 0;
	private static boolean rainbowActive = false;
	private static int rainbowRemaining = 0;

	// =====================================================================
	// CONSTANTS
	// =====================================================================

	private static final float DUNGEON_BASELINE_TEMP = 15f;  // °C deep-dungeon baseline
	private static final float DUNGEON_BASELINE_HUMIDITY = 0.50f;
	private static final float TORCH_BASE_LIGHT = 0.25f;  // hero torch minimum

	// Peak solar elevation by season (degrees)
	private static final float SUMMER_PEAK_ELEV = 63f;
	private static final float WINTER_PEAK_ELEV = 23f;
	private static final float EQUINOX_PEAK_ELEV = 45f;

	// Moon brightness by phase (max moonLight before cloud attenuation)
	private static final float[] MOON_BRIGHTNESS = {
			0.00f,  // NEW_MOON
			0.03f,  // WAXING_CRESCENT
			0.07f,  // FIRST_QUARTER
			0.11f,  // WAXING_GIBBOUS
			0.15f,  // FULL_MOON
			0.11f,  // WANING_GIBBOUS
			0.07f,  // LAST_QUARTER
			0.03f   // WANING_CRESCENT
	};

	// Moon visibility window as fraction of night (how long moon is above horizon)
	private static final float[] MOON_VISIBILITY_WINDOW = {
			0.00f,  // NEW_MOON (not visible at night)
			0.20f,  // WAXING_CRESCENT (early evening only)
			0.50f,  // FIRST_QUARTER (first half of night)
			0.75f,  // WAXING_GIBBOUS (most of night)
			1.00f,  // FULL_MOON (all night)
			0.75f,  // WANING_GIBBOUS (most of night, rises late)
			0.50f,  // LAST_QUARTER (second half of night)
			0.20f   // WANING_CRESCENT (pre-dawn only)
	};

	// =====================================================================
	// PUBLIC API — Surface values
	// =====================================================================

	public static float surfaceTemp()      { return surfaceTemp; }
	public static float surfaceHumidity()  { return surfaceHumidity; }
	public static float surfacePressure()  { return surfacePressure; }
	public static float surfaceWindSpeed() { return surfaceWindSpeed; }
	public static float surfaceWindDir()   { return Float.isNaN(debugWindDirOverride) ? surfaceWindDir : debugWindDirOverride; }
	public static float cloudCover()       { return Float.isNaN(debugCloudOverride) ? cloudCover : debugCloudOverride; }
	public static float precipRate()       { return precipRate; }
	public static PrecipType precipType()  { return precipType; }
	public static WeatherState weatherState() { return weatherState; }

	// =====================================================================
	// PUBLIC API — Local (depth-adjusted) values
	// =====================================================================

	// Debug overrides: NaN = disabled, any value = forced
	public static float debugTempOverride = Float.NaN;
	public static float debugCloudOverride = Float.NaN;   // 0.0 - 1.0
	public static float debugWindOverride = Float.NaN;    // 0 - 30
	public static float debugPrecipOverride = Float.NaN;  // 0.0 - 1.0
	public static PrecipType debugPrecipTypeOverride = null; // null = disabled
	public static float debugWindDirOverride = Float.NaN; // degrees 0-359, NaN = disabled

	public static float localTemp() {
		return Float.isNaN(debugTempOverride) ? localTemp : debugTempOverride;
	}
	public static float localHumidity()    { return localHumidity; }
	public static float localWindSpeed()   { return Float.isNaN(debugWindOverride) ? localWindSpeed : debugWindOverride; }
	public static float localPrecipRate()  { return Float.isNaN(debugPrecipOverride) ? localPrecipRate : debugPrecipOverride; }
	public static PrecipType localPrecipType() {
		// Explicit type override takes priority
		if (debugPrecipTypeOverride != null) return debugPrecipTypeOverride;
		// When the debug rate override forces precipitation but the real type is NONE,
		// derive a sensible type from the current temperature so particles appear.
		if (!Float.isNaN(debugPrecipOverride) && debugPrecipOverride > 0f
				&& localPrecipType == PrecipType.NONE) {
			float t = localTemp();
			if (t < -5f) return PrecipType.SNOW;
			if (t <= 2f) return PrecipType.SLEET;
			return PrecipType.RAIN;
		}
		return localPrecipType;
	}

	// =====================================================================
	// PUBLIC API — Convenience
	// =====================================================================

	public static boolean isRaining() {
		return localPrecipType() == PrecipType.RAIN && localPrecipRate() > 0f;
	}

	public static boolean isSnowing() {
		return (localPrecipType() == PrecipType.SNOW || localPrecipType() == PrecipType.BLIZZARD)
				&& localPrecipRate() > 0f;
	}

	public static boolean isStorming() {
		return weatherState == WeatherState.STORM;
	}

	public static boolean isFoggy() {
		return weatherState == WeatherState.FOG || localFog;
	}

	// Local fog: fog the PLACE makes rather than the fronts (the overworld's
	// swamps at dawn). The scene pushes the level's answer here every frame
	// (GameScene.updateWeather <- OverworldLevel.localFog); every fog reader
	// goes through isFoggy()/ambientType() and sees it. Not bundled: it is
	// re-derived from where the hero stands.
	private static boolean localFog = false;
	public static void setLocalFog(boolean fog) { localFog = fog; }

	// The special skies are the level's to allow: the overworld shows an
	// aurora only over its far north at night and a rainbow only by day after
	// the rain; everywhere else the climate's own word stands.
	public static boolean isAurora() {
		if (!auroraActive) return false;
		return !(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)
				|| ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).auroraPossible();
	}
	public static boolean isRainbow() {
		if (!rainbowActive) return false;
		return !(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)
				|| ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).rainbowPossible();
	}

	public static boolean isClear() {
		return weatherState == WeatherState.CLEAR || weatherState == WeatherState.FAIR;
	}

	public static float feelsLikeTemp() {
		float t = localTemp;
		float w = localWindSpeed;
		// Wind chill (simplified): only when cold and windy
		if (t < 10f && w > 3f) {
			t -= w * 0.5f;
		}
		// Being wet makes it feel much colder
		if (xyz.gabriwar.warpedpixeldungeon.Dungeon.hero != null
				&& xyz.gabriwar.warpedpixeldungeon.Dungeon.hero.buff(
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched.class) != null) {
			t -= 5f;
		}
		return t;
	}

	/**
	 * Returns a damage / accuracy multiplier for projectiles based on wind.
	 * Tailwind (throwing in the direction the wind blows) → slight bonus;
	 * headwind → slight penalty.  Effect is intentionally very subtle:
	 * at max wind (25 m/s) perfectly aligned it is ±8 % damage / ±5 % accuracy.
	 *
	 * @param fromCell  thrower position
	 * @param toCell    target position
	 * @param forDamage true → ±8 % range; false → ±5 % range (accuracy)
	 */
	public static float windProjectileFactor(int fromCell, int toCell, boolean forDamage) {
		if (Dungeon.level == null || fromCell == toCell) return 1f;

		int w = Dungeon.level.width();
		int fromCol = fromCell % w, fromRow = fromCell / w;
		int toCol   = toCell   % w, toRow   = toCell   / w;

		float dx = toCol - fromCol;
		float dy = fromRow - toRow; // screen-Y is inverted vs. math-Y
		// Throw angle: 0° = north, 90° = east  (standard compass)
		float throwDeg = (float) Math.toDegrees(Math.atan2(dx, dy));
		if (throwDeg < 0) throwDeg += 360f;

		// surfaceWindDir = direction the wind blows TOWARDS
		float diff = Math.abs(throwDeg - surfaceWindDir);
		if (diff > 180f) diff = 360f - diff;

		float alignment = (float) Math.cos(Math.toRadians(diff)); // +1 tail, -1 head
		float strength  = Math.min(1f, localWindSpeed / 25f);

		float maxBonus = forDamage ? 0.08f : 0.05f;
		return 1f + alignment * strength * maxBonus;
	}

	/** Cloud cover effect on brightness. 1.0 = no dimming, <1.0 = dimmed. */
	public static float brightnessModifier() {
		float exposure = depthExposure(currentDepth);
		return 1f - (cloudCover * 0.25f * exposure);
	}

	// =====================================================================
	// PUBLIC API — Light values
	// =====================================================================

	public static float sunLight()          { return sunLight; }
	public static float moonLight()         { return moonLight; }
	public static float surfaceLight()      { return surfaceLight; }
	public static float solarElevation()    { return solarElevation; }
	public static float moonVisibility()    { return moonVisibility; }
	public static float moonElevation()     { return moonElevation; }
	public static float localAmbientLight() { return localAmbientLight; }
	public static boolean isSolarEclipse()  { return solarEclipseActive; }
	public static boolean isLunarEclipse()  { return lunarEclipseActive; }

	// Debug toggles for special events
	public static void debugToggleAurora() {
		auroraActive = !auroraActive;
		auroraRemaining = auroraActive ? 500 : 0;
		if (auroraActive) logWeather("aurora_appears");
		else logWeather("aurora_fades");
	}
	public static void debugToggleRainbow() {
		rainbowActive = !rainbowActive;
		rainbowRemaining = rainbowActive ? 200 : 0;
		if (rainbowActive) logWeather("rainbow_appears");
		else logWeather("rainbow_fades");
	}
	public static void debugToggleSolarEclipse() {
		solarEclipseActive = !solarEclipseActive;
		solarEclipseRemaining = solarEclipseActive ? 150 : 0;
		if (solarEclipseActive) logWeather("eclipse_sun");
		else logWeather("eclipse_sun_end");
	}
	public static void debugToggleLunarEclipse() {
		lunarEclipseActive = !lunarEclipseActive;
		lunarEclipseRemaining = lunarEclipseActive ? 300 : 0;
		if (lunarEclipseActive) logWeather("eclipse_moon");
		else logWeather("eclipse_moon_end");
	}

	/** Light exposure factor for current depth (0.02–1.0). */
	public static float lightExposure() {
		return Math.max(0.02f, 1f - currentDepth * 0.045f);
	}

	/** Solar color as RGB (warm at low elevation, neutral at high). */
	public static float[] solarColor() {
		return solarColorRGB;
	}

	/** Moonlight color as RGB (blue-silver, or amber for harvest moon). */
	public static float[] moonColor() {
		return moonColorRGB;
	}

	/**
	 * Returns the blended sky tint as {A, R, G, B} for the DayNightCycle overlay.
	 * Incorporates solar color, lunar color, cloud dimming, and special events.
	 */
	public static float[] computeSkyTint() {
		float totalLight = clamp(sunLight + moonLight, 0f, 1f);

		// Blend solar and lunar colors by relative contribution
		float sunWeight = (totalLight > 0.001f) ? sunLight / totalLight : 0f;
		float moonWeight = 1f - sunWeight;

		float r = solarColorRGB[0] * sunWeight + moonColorRGB[0] * moonWeight;
		float g = solarColorRGB[1] * sunWeight + moonColorRGB[1] * moonWeight;
		float b = solarColorRGB[2] * sunWeight + moonColorRGB[2] * moonWeight;

		// Overlay alpha: darker = more opaque tint
		// At full daylight → no tint; at full darkness → heavy tint
		float darkness = 1f - totalLight;

		// Blood moon tint override
		if (lunarEclipseActive) {
			r = 0.60f; g = 0.05f; b = 0.05f;
			darkness = Math.max(darkness, 0.15f);
		}

		// Aurora: faint green tint addition
		if (auroraActive) {
			float auroraPulse = 0.5f + 0.5f * (float) Math.sin(
					System.currentTimeMillis() * 0.001 * 0.3);
			r = r * 0.85f + 0.15f * auroraPulse * 0.2f;
			g = g * 0.85f + 0.15f * auroraPulse * 0.8f;
			b = b * 0.85f + 0.15f * auroraPulse * 0.4f;
		}

		// Night base tint (blue-ish darkness)
		float nightR = 0.067f, nightG = 0.133f, nightB = 0.267f;
		float tintR = nightR * darkness + r * (1f - darkness) * 0.3f;
		float tintG = nightG * darkness + g * (1f - darkness) * 0.3f;
		float tintB = nightB * darkness + b * (1f - darkness) * 0.3f;
		// Keep tint alpha subtle — the overlay darkening is primarily driven
		// by 1−brightness in GameScene.updateDayNightTint(); this only adds colour.
		float tintA = darkness * 0.20f;

		return new float[]{ tintA, tintR, tintG, tintB };
	}

	/**
	 * Determines what ambient (non-precipitation) particle effect should play.
	 * Based on season, time of day, weather state, and depth.
	 */
	public static WeatherOverlayAmbient ambientType() {
		// Special events take priority
		if (solarEclipseActive && currentDepth < 16) {
			return WeatherOverlayAmbient.CORONA;
		}
		if (isAurora() && currentDepth < 16) {
			return WeatherOverlayAmbient.AURORA;
		}
		if (isRainbow() && currentDepth < 16) {
			return WeatherOverlayAmbient.RAINBOW;
		}

		// Depth-specific ambient (underground regions)
		if (currentDepth >= 21) {
			// Demon Halls: ash always
			return WeatherOverlayAmbient.ASH;
		}
		if (currentDepth >= 16) {
			// Dwarven City: steam vents
			return WeatherOverlayAmbient.STEAM;
		}
		if (currentDepth >= 11) {
			// Caves: dust motes
			if (localPrecipRate < 0.3f) return WeatherOverlayAmbient.DUST;
			return WeatherOverlayAmbient.NONE;
		}
		if (currentDepth >= 6) {
			// Prison: wind-blown dust
			if (localWindSpeed > 3f && localPrecipRate < 0.3f) return WeatherOverlayAmbient.DUST;
		}
		if (currentDepth >= 1 && currentDepth <= 5) {
			// Sewers: dripping water always, mist when foggy
			if (isFoggy()) return WeatherOverlayAmbient.MIST;
			return WeatherOverlayAmbient.DRIP;
		}

		// Surface / shallow depths — weather-driven
		if (localPrecipRate > 0.5f) {
			return WeatherOverlayAmbient.NONE;
		}

		// Fog (the fronts', or the place's own - see setLocalFog)
		if (isFoggy()) {
			return WeatherOverlayAmbient.MIST;
		}

		GameCalendar.Season season = GameCalendar.season();
		DayNightCycle.Phase phase = DayNightCycle.phase();
		boolean isNight = (phase == DayNightCycle.Phase.NIGHT || phase == DayNightCycle.Phase.DUSK);
		boolean isClearish = (weatherState == WeatherState.CLEAR
				|| weatherState == WeatherState.FAIR
				|| weatherState == WeatherState.PARTLY_CLOUDY);

		switch (season) {
			case SUMMER:
			case SPRING:
				if (isNight && isClearish) return WeatherOverlayAmbient.FIREFLIES;
				if (!isNight && isClearish && season == GameCalendar.Season.SPRING) {
					return WeatherOverlayAmbient.SPRING_PETALS;
				}
				// Hot clear summer days: dust shimmer
				if (!isNight && localTemp > 28f && isClearish) {
					return WeatherOverlayAmbient.DUST;
				}
				break;
			case AUTUMN:
				if (surfaceWindSpeed > 5f && localPrecipRate < 0.2f) {
					return WeatherOverlayAmbient.AUTUMN_LEAVES;
				}
				break;
			case WINTER:
				// No ambient in winter — snow handled by precipitation
				break;
		}
		return WeatherOverlayAmbient.NONE;
	}

	/**
	 * Mirror of WeatherOverlay.AmbientType to avoid circular dependency.
	 * GameScene maps this to the overlay's enum.
	 */
	public enum WeatherOverlayAmbient {
		NONE, FIREFLIES, AUTUMN_LEAVES, SPRING_PETALS, MIST, DUST, ASH, STEAM, AURORA, CORONA, DRIP, RAINBOW
	}

	// =====================================================================
	// LIFECYCLE
	// =====================================================================

	/** Called when a new game starts. Generates initial weather state and front queue. */
	public static void onNewGame(long seed) {
		frontGenSeed = seed ^ 0xC11CA7EL;
		frontQueue.clear();
		frontIndex = 0;
		lastFrontGenTurn = 0;

		// Initial conditions based on season — wind seeded for determinism
		surfacePressure = 1013f;
		java.util.Random windRng = new java.util.Random(seed ^ 0xB1EEB1EEL);
		surfaceWindDir = windRng.nextFloat() * 360f;
		surfaceWindSpeed = 1f + windRng.nextFloat() * 9f;  // 1–10 m/s
		cloudCover = 0f;
		precipRate = 0f;
		precipType = PrecipType.NONE;
		weatherState = WeatherState.CLEAR;
		prevWeatherState = WeatherState.CLEAR;
		prevPrecipType = PrecipType.NONE;

		// Reset light and special events
		solarEclipseActive = false; solarEclipseRemaining = 0;
		lunarEclipseActive = false; lunarEclipseRemaining = 0;
		auroraActive = false; auroraRemaining = 0;
		rainbowActive = false; rainbowRemaining = 0;

		// Compute initial temperature
		surfaceTemp = seasonalBaselineTemp();
		surfaceHumidity = seasonalBaselineHumidity();

		// Generate first batch of fronts
		generateFronts(0);

		// Compute initial light state
		computeLight(0);

		// Set local values
		currentDepth = 1;
		computeLocalValues();
	}

	/**
	 * Called once per hero turn (from DayNightCycle.onHeroTurn).
	 * Advances the climate simulation by one step.
	 */
	public static void onHeroTurn() {
		int turn = Dungeon.cycleTurn;

		// 1. Seasonal baseline
		float baseTemp = seasonalBaselineTemp();
		float baseHumidity = seasonalBaselineHumidity();

		// 2. Diurnal temperature shift from day/night phase
		float diurnalShift = computeDiurnalShift();
		surfaceTemp = baseTemp + diurnalShift;

		// 3. Process weather fronts
		float frontCloud = 0f;
		float frontHumidity = 0f;
		float frontTempShift = 0f;
		float frontWind = 0f;
		boolean frontActive = false;

		// Ensure we have fronts generated ahead
		if (turn > lastFrontGenTurn - 2000) {
			generateFronts(turn);
		}

		// Find and process active front
		WeatherFront activeFront = null;
		for (int i = frontIndex; i < frontQueue.size(); i++) {
			WeatherFront f = frontQueue.get(i);
			if (f.isExpired(turn)) {
				frontIndex = i + 1;
				continue;
			}
			if (f.isActive(turn)) {
				activeFront = f;
				break;
			}
			break; // front hasn't arrived yet
		}

		if (activeFront != null) {
			frontActive = true;
			float curve = activeFront.progressCurve(activeFront.turnsInto(turn));
			frontCloud = activeFront.intensity * curve;
			frontHumidity = activeFront.humidityBoost * curve;
			frontTempShift = activeFront.tempShift * curve;
			frontWind = activeFront.windBase * curve;
		}

		// 4. Apply front effects
		surfaceTemp += frontTempShift;
		cloudCover = frontActive ? frontCloud : drift(cloudCover, 0f, 0.002f);
		surfaceHumidity = clamp(baseHumidity + frontHumidity, 0f, 1f);
		surfaceWindSpeed = frontActive
				? clamp(frontWind + wobble(turn, 2f), 0f, 25f)
				: drift(surfaceWindSpeed, 3f, 0.05f);
		surfacePressure = frontActive
				? clamp(1013f - activeFront.intensity * 20f, 950f, 1050f)
				: drift(surfacePressure, 1013f, 0.1f);

		// Wind direction drifts slowly
		surfaceWindDir = (surfaceWindDir + wobble(turn, 1.5f) + 360f) % 360f;

		// 5. Clamp
		surfaceTemp = clamp(surfaceTemp, -20f, 60f);
		cloudCover = clamp(cloudCover, 0f, 1f);
		surfaceHumidity = clamp(surfaceHumidity, 0f, 1f);

		// 6. Compute precipitation
		computePrecipitation();

		// 7. Determine weather state
		prevWeatherState = weatherState;
		prevPrecipType = precipType;
		weatherState = determineWeatherState();

		// 7b. Compute light pipeline (needs weather state for cloud/precip attenuation)
		computeLight(turn);

		// 8. Compute local values for current depth
		computeLocalValues();

		// 9. Announce transitions
		announceWeatherChanges();

		// 10. Clean old fronts from queue (memory hygiene)
		while (frontIndex > 0 && frontQueue.size() > 0) {
			frontQueue.remove(0);
			frontIndex--;
		}
	}

	/** Called when the hero changes dungeon depth. */
	public static void onLevelChange(int depth) {
		currentDepth = depth;
		computeLocalValues();
	}

	// =====================================================================
	// SIMULATION INTERNALS
	// =====================================================================

	/** Seasonal mean temperature based on calendar position. */
	private static float seasonalBaselineTemp() {
		GameCalendar.Season season = GameCalendar.season();
		int day = GameCalendar.dayOfSeason();
		int daysInSeason = GameCalendar.daysInCurrentSeason();
		float t = (float) day / Math.max(1, daysInSeason);

		switch (season) {
			case SPRING: return 5f + 13f * t;                         // 5→18°C
			case SUMMER: return 18f + 14f * (float) Math.sin(Math.PI * t); // peaks ~32°C
			case AUTUMN: return 18f - 13f * t;                         // 18→5°C
			case WINTER: return 5f - 12f * (float) Math.sin(Math.PI * t);  // dips ~-7°C
			default:     return 12f;
		}
	}

	/** Seasonal baseline humidity. */
	private static float seasonalBaselineHumidity() {
		switch (GameCalendar.season()) {
			case SPRING: return 0.65f;
			case SUMMER: return 0.45f;
			case AUTUMN: return 0.55f;
			case WINTER: return 0.40f;
			default:     return 0.50f;
		}
	}

	/** Diurnal temperature swing from day/night phase. */
	private static float computeDiurnalShift() {
		DayNightCycle.Phase phase = DayNightCycle.phase();
		float progress = DayNightCycle.phaseProgress();

		// Amplitude varies by season
		float amplitude;
		switch (GameCalendar.season()) {
			case SUMMER: amplitude = 7f; break;
			case WINTER: amplitude = 3f; break;
			default:     amplitude = 5f; break;
		}

		// Map phase + progress to a sinusoidal curve
		// Dawn: rising from cold. Day: warm peak. Dusk: cooling. Night: cold trough.
		float phaseAngle;
		switch (phase) {
			case DAWN:  phaseAngle = -0.5f + progress * 0.5f; break;  // -0.5 → 0.0
			case DAY:   phaseAngle = progress;                 break;  //  0.0 → 1.0
			case DUSK:  phaseAngle = 1f + progress * 0.5f;     break;  //  1.0 → 1.5
			case NIGHT: phaseAngle = 1.5f + progress * 0.5f;   break;  //  1.5 → 2.0
			default:    phaseAngle = 0f;
		}

		// sin curve: peak at 0.5 (mid-day), trough at 1.5 (mid-night)
		return amplitude * (float) Math.sin(Math.PI * phaseAngle);
	}

	// =====================================================================
	// LIGHT COMPUTATION
	// =====================================================================

	/** Compute all light values for the current turn. Call after weather state is set. */
	private static void computeLight(int turn) {
		DayNightCycle.Phase phase = DayNightCycle.phase();
		float progress = DayNightCycle.phaseProgress();
		GameCalendar.Season season = GameCalendar.season();

		// ---- Solar ----
		solarElevation = computeSolarElevation(phase, progress, season);
		float rawSun = solarIntensity(solarElevation);
		computeSolarColor(solarElevation);

		// Cloud attenuation of sunlight
		float sunCloudFactor = 1f - (cloudCover * 0.70f);
		if (weatherState == WeatherState.STORM) sunCloudFactor *= 0.6f;
		sunLight = rawSun * Math.max(0f, sunCloudFactor);

		// ---- Lunar ----
		GameCalendar.MoonPhase moonPhase = GameCalendar.moonPhase();
		int phaseOrd = moonPhase.ordinal();
		float maxMoonBright = MOON_BRIGHTNESS[phaseOrd];
		float moonWindow = MOON_VISIBILITY_WINDOW[phaseOrd];

		moonVisibility = computeMoonVisibility(phase, progress, moonPhase, moonWindow);
		moonElevation = computeMoonElevation(moonVisibility, season);
		computeMoonColor(moonPhase, season);

		float rawMoon = maxMoonBright * moonVisibility;

		// Cloud attenuation of moonlight (moon is more easily blocked)
		float moonCloudFactor = 1f - (cloudCover * 0.90f);
		if (weatherState == WeatherState.STORM) moonCloudFactor *= 0.3f;
		moonLight = rawMoon * Math.max(0f, moonCloudFactor);

		// ---- Precipitation scattering ----
		if (precipRate > 0f) {
			float scatterDim = 1f - precipRate * 0.3f;
			sunLight *= scatterDim;
			moonLight *= scatterDim;
		}

		// ---- Special events ----
		checkSpecialEvents(turn, phase, progress, moonPhase, season);

		// Eclipse overrides
		if (solarEclipseActive) {
			sunLight *= 0.02f;
		}
		if (lunarEclipseActive) {
			moonLight *= 0.25f;
		}

		// ---- Combine ----
		surfaceLight = clamp(sunLight + moonLight, 0f, 1f);

		// Aurora adds a tiny bit of ambient light
		if (auroraActive) {
			surfaceLight = clamp(surfaceLight + 0.03f, 0f, 1f);
		}
	}

	/**
	 * Solar elevation from phase, progress, and season.
	 * Positive = above horizon, negative = below.
	 */
	private static float computeSolarElevation(DayNightCycle.Phase phase,
	                                            float progress,
	                                            GameCalendar.Season season) {
		float peakElev;
		switch (season) {
			case SUMMER: peakElev = SUMMER_PEAK_ELEV; break;
			case WINTER: peakElev = WINTER_PEAK_ELEV; break;
			case SPRING:
			case AUTUMN:
			default:     peakElev = EQUINOX_PEAK_ELEV; break;
		}

		// Map phase+progress to a continuous angle for the sun's arc
		switch (phase) {
			case DAWN:
				// Sun rises from -10° to ~5°
				return -10f + progress * 15f;
			case DAY:
				// Sun arcs from ~5° to peakElev and back down to ~5°
				return 5f + (peakElev - 5f) * (float) Math.sin(Math.PI * progress);
			case DUSK:
				// Sun descends from ~5° to -10°
				return 5f - progress * 15f;
			case NIGHT:
			default:
				// Sun is well below horizon
				return -10f - progress * 30f;
		}
	}

	/** Raw solar intensity from elevation (0 when below horizon). */
	private static float solarIntensity(float elevation) {
		if (elevation <= 0f) return 0f;
		if (elevation >= 15f) return 1f;
		// Smooth ramp from 0° to 15°
		return elevation / 15f;
	}

	/** Compute solar color temperature from elevation. */
	private static void computeSolarColor(float elevation) {
		if (elevation <= 0f) {
			solarColorRGB[0] = 0f; solarColorRGB[1] = 0f; solarColorRGB[2] = 0f;
			return;
		}

		// Low elevation: warm orange/red (dawn/dusk). High: neutral white.
		float t = clamp(elevation / 30f, 0f, 1f); // 0=horizon, 1=high noon
		// Warm → neutral
		solarColorRGB[0] = 1.0f;                          // R stays high
		solarColorRGB[1] = 0.6f + 0.4f * t;               // G: 0.6 → 1.0
		solarColorRGB[2] = 0.3f + 0.7f * t;               // B: 0.3 → 1.0
	}

	/**
	 * Moon visibility: 0-1 based on whether the moon is above the horizon
	 * during the current night phase. Different moon phases are visible at
	 * different times of night.
	 */
	private static float computeMoonVisibility(DayNightCycle.Phase phase,
	                                            float progress,
	                                            GameCalendar.MoonPhase moonPhase,
	                                            float window) {
		if (window <= 0f) return 0f;

		// Moon is only visible during night and partially during dusk/dawn
		float nightProgress; // 0=start of night, 1=end of night
		switch (phase) {
			case DUSK:
				nightProgress = -0.5f + progress * 0.5f; // -0.5 to 0.0
				break;
			case NIGHT:
				nightProgress = progress; // 0.0 to 1.0
				break;
			case DAWN:
				nightProgress = 1f + progress * 0.5f; // 1.0 to 1.5
				break;
			default:
				return 0f; // no moon during day
		}

		// Which part of night the moon is visible depends on phase
		// Waxing moons: visible in first part of night (set early)
		// Full moon: all night
		// Waning moons: visible in second part (rise late)
		int ord = moonPhase.ordinal();
		float windowStart, windowEnd;

		if (ord <= 4) {
			// NEW→FULL: rises earlier, visible from start of night
			windowStart = 0f;
			windowEnd = window;
		} else {
			// FULL→NEW: rises later, visible in second half
			windowStart = 1f - window;
			windowEnd = 1f;
		}

		if (nightProgress < windowStart || nightProgress > windowEnd) return 0f;

		// Smooth ramp at edges (rise/set)
		float edgeFade = 0.1f;
		float vis = 1f;
		if (nightProgress < windowStart + edgeFade) {
			vis = (nightProgress - windowStart) / edgeFade;
		} else if (nightProgress > windowEnd - edgeFade) {
			vis = (windowEnd - nightProgress) / edgeFade;
		}
		return clamp(vis, 0f, 1f);
	}

	/** Moon elevation: simplified arc during visibility window. */
	private static float computeMoonElevation(float visibility, GameCalendar.Season season) {
		if (visibility <= 0f) return 0f;
		float peakElev;
		switch (season) {
			case WINTER: peakElev = 65f; break; // high when sun is low
			case SUMMER: peakElev = 25f; break; // low when sun is high
			default:     peakElev = 45f; break;
		}
		return peakElev * visibility; // simplified: peaks when most visible
	}

	/** Compute moon color based on phase and season (harvest moon detection). */
	private static void computeMoonColor(GameCalendar.MoonPhase moonPhase,
	                                      GameCalendar.Season season) {
		// Harvest moon: first full moon of autumn
		boolean harvestMoon = (moonPhase == GameCalendar.MoonPhase.FULL_MOON
				&& season == GameCalendar.Season.AUTUMN
				&& GameCalendar.dayOfSeason() < 30);

		if (harvestMoon) {
			moonColorRGB[0] = 0.95f; moonColorRGB[1] = 0.75f; moonColorRGB[2] = 0.45f;
		} else {
			// Standard blue-silver moonlight
			moonColorRGB[0] = 0.65f; moonColorRGB[1] = 0.70f; moonColorRGB[2] = 0.90f;
		}
	}

	/** Check and tick special light events (eclipses, aurora). */
	private static void checkSpecialEvents(int turn, DayNightCycle.Phase phase,
	                                        float progress,
	                                        GameCalendar.MoonPhase moonPhase,
	                                        GameCalendar.Season season) {
		// Tick active events
		if (solarEclipseActive) {
			solarEclipseRemaining--;
			if (solarEclipseRemaining <= 0) {
				solarEclipseActive = false;
				logWeather("eclipse_sun_end");
			}
		}
		if (lunarEclipseActive) {
			lunarEclipseRemaining--;
			if (lunarEclipseRemaining <= 0) {
				lunarEclipseActive = false;
				logWeather("eclipse_moon_end");
			}
		}
		if (auroraActive) {
			auroraRemaining--;
			if (auroraRemaining <= 0) {
				auroraActive = false;
				logWeather("aurora_fades");
			}
		}
		if (rainbowActive) {
			rainbowRemaining--;
			if (rainbowRemaining <= 0) {
				rainbowActive = false;
				logWeather("rainbow_fades");
			}
		}

		// Only check for new events every ~50 turns to save work
		if (turn % 50 != 0) return;

		// Deterministic RNG from seed + day
		int absDay = Dungeon.calendarStartDay + (Dungeon.cycleTurn / DayNightCycle.FULL_CYCLE);
		long eventSeed = Dungeon.seed ^ ((long) absDay * 0x517CC1B727220A95L);
		java.util.Random rng = new java.util.Random(eventSeed);

		// Solar eclipse: new moon, midday, ~0.1% per year (~0.027% per day)
		if (!solarEclipseActive
				&& moonPhase == GameCalendar.MoonPhase.NEW_MOON
				&& phase == DayNightCycle.Phase.DAY
				&& progress > 0.4f && progress < 0.6f
				&& rng.nextFloat() < 0.00027f) {
			solarEclipseActive = true;
			solarEclipseRemaining = 80 + rng.nextInt(70); // 80-150 turns
			logWeather("eclipse_sun");
		}

		// Lunar eclipse: full moon, mid-night, ~0.5% per year
		if (!lunarEclipseActive
				&& moonPhase == GameCalendar.MoonPhase.FULL_MOON
				&& phase == DayNightCycle.Phase.NIGHT
				&& progress > 0.3f && progress < 0.7f
				&& rng.nextFloat() < 0.005f) {
			lunarEclipseActive = true;
			lunarEclipseRemaining = 200 + rng.nextInt(200); // 200-400 turns
			logWeather("eclipse_moon");
		}

		// Aurora: winter clear night, ~2% per day
		if (!auroraActive
				&& season == GameCalendar.Season.WINTER
				&& phase == DayNightCycle.Phase.NIGHT
				&& cloudCover < 0.2f
				&& rng.nextFloat() < 0.02f) {
			auroraActive = true;
			auroraRemaining = 300 + rng.nextInt(400); // 300-700 turns
			logWeather("aurora_appears");
		}

		// Rainbow: rain just cleared + daytime + low sun angle (refraction)
		if (!rainbowActive
				&& weatherState == WeatherState.CLEARING
				&& (phase == DayNightCycle.Phase.DAY || phase == DayNightCycle.Phase.DAWN)
				&& solarElevation > 5f && solarElevation < 42f
				&& surfaceHumidity > 0.5f
				&& rng.nextFloat() < 0.15f) {
			rainbowActive = true;
			rainbowRemaining = 100 + rng.nextInt(200); // 100-300 turns
			logWeather("rainbow_appears");
		}
	}

	/** Derive precipitation from cloud cover, humidity, and temperature. */
	private static void computePrecipitation() {
		if (cloudCover < 0.5f || surfaceHumidity < 0.6f) {
			precipRate = 0f;
			precipType = PrecipType.NONE;
			return;
		}

		// Find active front intensity (or default)
		float frontIntensity = 0.5f;
		for (int i = frontIndex; i < frontQueue.size(); i++) {
			WeatherFront f = frontQueue.get(i);
			if (f.isActive(Dungeon.cycleTurn)) {
				frontIntensity = f.intensity;
				break;
			}
		}

		precipRate = clamp((cloudCover - 0.4f) * surfaceHumidity * frontIntensity, 0f, 1f);

		if (precipRate <= 0.01f) {
			precipRate = 0f;
			precipType = PrecipType.NONE;
			return;
		}

		// Determine type from temperature
		if (weatherState == WeatherState.STORM && surfaceTemp < 5f && surfaceHumidity > 0.8f) {
			precipType = PrecipType.HAIL;
		} else if (surfaceTemp < -5f && surfaceWindSpeed > 10f) {
			precipType = PrecipType.BLIZZARD;
		} else if (surfaceTemp < 0f) {
			precipType = PrecipType.SNOW;
		} else if (surfaceTemp <= 2f) {
			precipType = PrecipType.SLEET;
		} else {
			precipType = PrecipType.RAIN;
		}
	}

	/** Determine overall weather state from current conditions. */
	private static WeatherState determineWeatherState() {
		// Fog: low wind, high humidity, near dew point, low cloud cover
		float dewPoint = surfaceTemp - ((1f - surfaceHumidity) * 20f);
		if (surfaceWindSpeed < 3f && surfaceHumidity > 0.85f
				&& surfaceTemp <= dewPoint + 2f && cloudCover < 0.5f) {
			return WeatherState.FOG;
		}

		// Storm: high wind + heavy precip
		if (precipRate > 0.6f && surfaceWindSpeed > 12f) {
			return WeatherState.STORM;
		}

		// Precipitation states
		if (precipRate > 0.4f) return WeatherState.HEAVY_PRECIP;
		if (precipRate > 0.05f) return WeatherState.LIGHT_PRECIP;

		// Clearing: was precipitating, now stopped, but clouds still > 0.3
		if (prevWeatherState == WeatherState.LIGHT_PRECIP
				|| prevWeatherState == WeatherState.HEAVY_PRECIP
				|| prevWeatherState == WeatherState.STORM) {
			if (precipRate <= 0.05f && cloudCover > 0.3f) {
				return WeatherState.CLEARING;
			}
		}
		// Stay in clearing while clouds are fading
		if (prevWeatherState == WeatherState.CLEARING && cloudCover > 0.15f) {
			return WeatherState.CLEARING;
		}

		// Cloud states
		if (cloudCover > 0.6f)  return WeatherState.OVERCAST;
		if (cloudCover > 0.3f)  return WeatherState.PARTLY_CLOUDY;
		if (cloudCover > 0.1f)  return WeatherState.FAIR;

		return WeatherState.CLEAR;
	}

	// =====================================================================
	// DEPTH / LOCAL
	// =====================================================================

	/** Exposure factor: how much surface weather reaches this depth (0.05–1.0). */
	private static float depthExposure(int depth) {
		return Math.max(0.05f, 1f - depth * 0.035f);
	}

	/** Regional temperature bias based on dungeon region. */
	private static float regionalTempBias(int depth) {
		if (depth <= 5)  return -2f;   // Sewers: cool
		if (depth <= 10) return -5f;   // Prison: cold
		if (depth <= 15) return 0f;    // Caves: stable
		if (depth <= 20) return 8f;    // Dwarven City: heated
		return 20f;                     // Demon Halls: volcanic
	}

	/** Regional humidity bias. */
	private static float regionalHumidityBias(int depth) {
		if (depth <= 5)  return 0.20f;
		if (depth <= 10) return -0.10f;
		if (depth <= 15) return 0.05f;
		if (depth <= 20) return -0.15f;
		return -0.30f;
	}

	/** Recompute local (depth-adjusted) climate values. */
	private static void computeLocalValues() {
		float exposure = depthExposure(currentDepth);
		float tempBias = regionalTempBias(currentDepth);
		float humBias = regionalHumidityBias(currentDepth);

		localTemp = DUNGEON_BASELINE_TEMP
				+ (surfaceTemp - DUNGEON_BASELINE_TEMP) * exposure
				+ tempBias
				+ overworldBiomeTempBias();
		localHumidity = clamp(
				DUNGEON_BASELINE_HUMIDITY
						+ (surfaceHumidity - DUNGEON_BASELINE_HUMIDITY) * exposure
						+ humBias
						+ overworldBiomeHumidityBias(),
				0f, 1f);
		localWindSpeed = surfaceWindSpeed * exposure;
		localPrecipRate = precipRate * exposure;

		// No surface precipitation deep underground
		if (currentDepth >= 16 || localPrecipRate < 0.01f) {
			localPrecipRate = 0f;
			localPrecipType = PrecipType.NONE;
		} else {
			localPrecipType = precipType;
		}

		// Light: depth-attenuated + torch baseline
		float lExp = lightExposure();
		localAmbientLight = clamp(surfaceLight * lExp + TORCH_BASE_LIGHT, 0f, 1f);
	}

	//the hero's overworld biome pushes the local climate around: deserts
	//scorch, snowfields and tundra freeze, swamps drip
	private static float overworldBiomeTempBias(){
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome b = heroOverworldBiome();
		if (b == null) return 0f;
		switch (b){
			case DESERT:    return 12f;
			case SNOWFIELD: return -20f;
			case TUNDRA:    return -15f;
			case MOUNTAIN:
			case FOOTHILLS: return -8f;
			case SWAMP:     return 3f;
			case BEACH:     return 2f;
			default:        return 0f;
		}
	}

	private static float overworldBiomeHumidityBias(){
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome b = heroOverworldBiome();
		if (b == null) return 0f;
		switch (b){
			case SWAMP:     return 0.25f;
			case RIVER:
			case OCEAN:     return 0.15f;
			case DESERT:    return -0.30f;
			case TUNDRA:    return -0.10f;
			default:        return 0f;
		}
	}

	private static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome heroOverworldBiome(){
		if (Dungeon.hero == null
				|| !(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)){
			return null;
		}
		return ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level)
				.biomeAtCell( Dungeon.hero.pos );
	}

	// =====================================================================
	// FRONT GENERATION
	// =====================================================================

	/** Generate weather fronts ahead of the given turn so the queue never runs dry. */
	private static void generateFronts(int currentTurn) {
		java.util.Random rng = new java.util.Random(
				frontGenSeed ^ ((long) currentTurn / 2000) * 0x9E3779B97F4A7C15L);

		// Generate enough fronts to cover ~5000 turns ahead
		int horizon = currentTurn + 5000;

		// Find the end of the last scheduled front
		int cursor;
		if (frontQueue.isEmpty()) {
			cursor = currentTurn;
		} else {
			WeatherFront last = frontQueue.get(frontQueue.size() - 1);
			cursor = last.arrivalTurn + last.duration;
		}

		GameCalendar.Season season = GameCalendar.season();

		while (cursor < horizon) {
			// Gap between fronts
			int gap = 200 + rng.nextInt(400); // 200-600 turns
			cursor += gap;

			// Front type weighted by season
			WeatherFront.Type type = pickFrontType(season, rng);

			// Duration by type
			int duration;
			switch (type) {
				case WARM:       duration = 300 + rng.nextInt(500); break;  // 300-800
				case COLD:       duration = 150 + rng.nextInt(350); break;  // 150-500
				case STATIONARY: duration = 400 + rng.nextInt(400); break;  // 400-800
				default:         duration = 300; break;
			}

			// Intensity by season
			float baseIntensity = 0.3f + rng.nextFloat() * 0.7f; // 0.3-1.0
			switch (season) {
				case SPRING: baseIntensity *= 0.75f; break; // spring: gentler
				case SUMMER: baseIntensity *= 1.1f;  break; // summer: intense but brief
				case AUTUMN: baseIntensity *= 0.85f; break;
				case WINTER: baseIntensity *= 0.90f; break;
			}
			float intensity = clamp(baseIntensity, 0.2f, 1f);

			// Temperature shift
			float tempShift;
			switch (type) {
				case WARM:       tempShift = 2f + rng.nextFloat() * 5f;   break; // +2 to +7
				case COLD:       tempShift = -(3f + rng.nextFloat() * 8f); break; // -3 to -11
				case STATIONARY: tempShift = -1f + rng.nextFloat() * 2f;   break; // -1 to +1
				default:         tempShift = 0f;
			}

			// Humidity boost
			float humBoost = 0.1f + rng.nextFloat() * 0.3f; // 0.1-0.4

			// Wind
			float wind;
			switch (type) {
				case COLD:  wind = 8f + rng.nextFloat() * 12f;  break; // 8-20
				case WARM:  wind = 4f + rng.nextFloat() * 8f;   break; // 4-12
				default:    wind = 2f + rng.nextFloat() * 6f;   break; // 2-8
			}

			frontQueue.add(new WeatherFront(
					type, cursor, duration, intensity, tempShift, humBoost, wind));
		}

		lastFrontGenTurn = horizon;
	}

	/** Pick a front type weighted by season. */
	private static WeatherFront.Type pickFrontType(GameCalendar.Season season, java.util.Random rng) {
		float roll = rng.nextFloat();
		switch (season) {
			case SPRING:
				// Lots of warm fronts (steady rain)
				if (roll < 0.50f) return WeatherFront.Type.WARM;
				if (roll < 0.80f) return WeatherFront.Type.COLD;
				return WeatherFront.Type.STATIONARY;
			case SUMMER:
				// Cold fronts = thunderstorms
				if (roll < 0.20f) return WeatherFront.Type.WARM;
				if (roll < 0.70f) return WeatherFront.Type.COLD;
				return WeatherFront.Type.STATIONARY;
			case AUTUMN:
				// Mix of everything, more stationary
				if (roll < 0.30f) return WeatherFront.Type.WARM;
				if (roll < 0.60f) return WeatherFront.Type.COLD;
				return WeatherFront.Type.STATIONARY;
			case WINTER:
				// Mostly cold fronts and stationary
				if (roll < 0.15f) return WeatherFront.Type.WARM;
				if (roll < 0.55f) return WeatherFront.Type.COLD;
				return WeatherFront.Type.STATIONARY;
			default:
				return WeatherFront.Type.COLD;
		}
	}

	// =====================================================================
	// WEATHER CHANGE ANNOUNCEMENTS
	// =====================================================================

	private static void announceWeatherChanges() {
		if (weatherState == prevWeatherState && precipType == prevPrecipType) {
			return;
		}

		// Precipitation started
		if (prevPrecipType == PrecipType.NONE && precipType != PrecipType.NONE) {
			String key;
			switch (precipType) {
				case RAIN:     key = "rain_starts"; break;
				case SNOW:     key = "snow_starts"; break;
				case HAIL:     key = "hail_starts"; break;
				case SLEET:    key = "sleet_starts"; break;
				case BLIZZARD: key = "blizzard_starts"; break;
				default:       key = null;
			}
			if (key != null) {
				logWeather(key);
			}
		}

		// Precipitation stopped
		if (prevPrecipType != PrecipType.NONE && precipType == PrecipType.NONE) {
			logWeather("precip_stops");
		}

		// Precip type changed (e.g. rain → snow)
		if (prevPrecipType != PrecipType.NONE && precipType != PrecipType.NONE
				&& prevPrecipType != precipType) {
			String key;
			switch (precipType) {
				case RAIN:     key = "turns_to_rain"; break;
				case SNOW:     key = "turns_to_snow"; break;
				case HAIL:     key = "turns_to_hail"; break;
				case SLEET:    key = "turns_to_sleet"; break;
				case BLIZZARD: key = "turns_to_blizzard"; break;
				default:       key = null;
			}
			if (key != null) {
				logWeather(key);
			}
		}

		// State transitions (non-precip)
		if (prevWeatherState != weatherState) {
			switch (weatherState) {
				case OVERCAST:
					if (prevWeatherState == WeatherState.CLEAR
							|| prevWeatherState == WeatherState.FAIR
							|| prevWeatherState == WeatherState.PARTLY_CLOUDY) {
						logWeather("clouds_gather");
					}
					break;
				case PARTLY_CLOUDY:
					if (prevWeatherState == WeatherState.CLEAR
							|| prevWeatherState == WeatherState.FAIR) {
						logWeather("clouds_drift");
					}
					break;
				case STORM:
					logWeather("storm_begins");
					break;
				case FOG:
					logWeather("fog_rolls_in");
					break;
				case CLEARING:
					logWeather("clouds_breaking");
					break;
				case CLEAR:
					if (prevWeatherState != WeatherState.FAIR) {
						logWeather("sky_clears");
					}
					break;
			}
		}
	}

	private static void logWeather(String key) {
		String msg = Messages.get(ClimateManager.class, key);
		if (msg != null && !msg.isEmpty() && !msg.startsWith("!!!")) {
			GLog.i(msg);
		}
	}

	// =====================================================================
	// UTILITY
	// =====================================================================

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}

	/** Drift a value toward a target at the given rate per turn. */
	private static float drift(float current, float target, float rate) {
		if (Math.abs(current - target) < rate) return target;
		return current + (target > current ? rate : -rate);
	}

	/** Deterministic wobble based on turn and dungeon seed (small pseudo-random perturbation). */
	private static float wobble(int turn, float amplitude) {
		// XOR with dungeon seed so wind drift is unique per seed
		long h = (turn ^ Dungeon.seed) * 0x9E3779B97F4A7C15L;
		h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
		float normalized = ((h >>> 40) & 0xFFFFF) / (float) 0xFFFFF; // 0..1
		return (normalized - 0.5f) * 2f * amplitude;
	}

	// =====================================================================
	// BUNDLE PERSISTENCE
	// =====================================================================

	private static final String CLIMATE             = "climate";
	private static final String B_SURFACE_TEMP      = "sTemp";
	private static final String B_SURFACE_HUMIDITY  = "sHum";
	private static final String B_SURFACE_PRESSURE  = "sPres";
	private static final String B_WIND_SPEED        = "wSpd";
	private static final String B_WIND_DIR          = "wDir";
	private static final String B_CLOUD_COVER       = "cloud";
	private static final String B_PRECIP_RATE       = "pRate";
	private static final String B_PRECIP_TYPE       = "pType";
	private static final String B_WEATHER_STATE     = "wState";
	private static final String B_PREV_WEATHER      = "prevW";
	private static final String B_PREV_PRECIP       = "prevP";
	private static final String B_FRONT_QUEUE       = "fronts";
	private static final String B_FRONT_INDEX       = "fIdx";
	private static final String B_FRONT_GEN_SEED    = "fSeed";
	private static final String B_LAST_FRONT_GEN    = "fGenT";
	private static final String B_DEPTH             = "depth";
	private static final String B_SOLAR_ECLIPSE     = "sEcl";
	private static final String B_SOLAR_ECL_REM     = "sEclR";
	private static final String B_LUNAR_ECLIPSE     = "lEcl";
	private static final String B_LUNAR_ECL_REM     = "lEclR";
	private static final String B_AURORA            = "aurora";
	private static final String B_AURORA_REM        = "auroraR";
	private static final String B_RAINBOW           = "rainbow";
	private static final String B_RAINBOW_REM       = "rainbowR";

	public static void storeInBundle(Bundle parent) {
		Bundle b = new Bundle();

		b.put(B_SURFACE_TEMP, surfaceTemp);
		b.put(B_SURFACE_HUMIDITY, surfaceHumidity);
		b.put(B_SURFACE_PRESSURE, surfacePressure);
		b.put(B_WIND_SPEED, surfaceWindSpeed);
		b.put(B_WIND_DIR, surfaceWindDir);
		b.put(B_CLOUD_COVER, cloudCover);
		b.put(B_PRECIP_RATE, precipRate);
		b.put(B_PRECIP_TYPE, precipType);
		b.put(B_WEATHER_STATE, weatherState);
		b.put(B_PREV_WEATHER, prevWeatherState);
		b.put(B_PREV_PRECIP, prevPrecipType);
		b.put(B_FRONT_QUEUE, frontQueue);
		b.put(B_FRONT_INDEX, frontIndex);
		b.put(B_FRONT_GEN_SEED, frontGenSeed);
		b.put(B_LAST_FRONT_GEN, lastFrontGenTurn);
		b.put(B_DEPTH, currentDepth);
		b.put(B_SOLAR_ECLIPSE, solarEclipseActive);
		b.put(B_SOLAR_ECL_REM, solarEclipseRemaining);
		b.put(B_LUNAR_ECLIPSE, lunarEclipseActive);
		b.put(B_LUNAR_ECL_REM, lunarEclipseRemaining);
		b.put(B_AURORA, auroraActive);
		b.put(B_AURORA_REM, auroraRemaining);
		b.put(B_RAINBOW, rainbowActive);
		b.put(B_RAINBOW_REM, rainbowRemaining);

		parent.put(CLIMATE, b);
	}

	public static void restoreFromBundle(Bundle parent) {
		Bundle b = parent.getBundle(CLIMATE);
		if (b == null || b.isNull()) {
			onNewGame(Dungeon.seed);
			return;
		}

		surfaceTemp = b.getFloat(B_SURFACE_TEMP);
		surfaceHumidity = b.getFloat(B_SURFACE_HUMIDITY);
		surfacePressure = b.getFloat(B_SURFACE_PRESSURE);
		surfaceWindSpeed = b.getFloat(B_WIND_SPEED);
		surfaceWindDir = b.getFloat(B_WIND_DIR);
		cloudCover = b.getFloat(B_CLOUD_COVER);
		precipRate = b.getFloat(B_PRECIP_RATE);
		precipType = b.getEnum(B_PRECIP_TYPE, PrecipType.class);
		weatherState = b.getEnum(B_WEATHER_STATE, WeatherState.class);
		prevWeatherState = b.getEnum(B_PREV_WEATHER, WeatherState.class);
		prevPrecipType = b.getEnum(B_PREV_PRECIP, PrecipType.class);
		frontIndex = b.getInt(B_FRONT_INDEX);
		frontGenSeed = b.getLong(B_FRONT_GEN_SEED);
		lastFrontGenTurn = b.getInt(B_LAST_FRONT_GEN);
		currentDepth = b.getInt(B_DEPTH);
		solarEclipseActive = b.getBoolean(B_SOLAR_ECLIPSE);
		solarEclipseRemaining = b.getInt(B_SOLAR_ECL_REM);
		lunarEclipseActive = b.getBoolean(B_LUNAR_ECLIPSE);
		lunarEclipseRemaining = b.getInt(B_LUNAR_ECL_REM);
		auroraActive = b.getBoolean(B_AURORA);
		auroraRemaining = b.getInt(B_AURORA_REM);
		rainbowActive = b.getBoolean(B_RAINBOW);
		rainbowRemaining = b.getInt(B_RAINBOW_REM);

		frontQueue.clear();
		Collection<Bundlable> fronts = b.getCollection(B_FRONT_QUEUE);
		if (fronts != null) {
			for (Bundlable item : fronts) {
				if (item instanceof WeatherFront) {
					frontQueue.add((WeatherFront) item);
				}
			}
		}

		if (precipType == null) precipType = PrecipType.NONE;
		if (weatherState == null) weatherState = WeatherState.CLEAR;
		if (prevWeatherState == null) prevWeatherState = WeatherState.CLEAR;
		if (prevPrecipType == null) prevPrecipType = PrecipType.NONE;

		// Recompute light from restored weather state
		computeLight(Dungeon.cycleTurn);
		computeLocalValues();
	}

	/** Restore climate state from network data (spectator). */
	public static void restoreFromNetwork(
			float temp, float humidity, float pressure,
			float windSpeed, float windDir, float cloud,
			float pRate, String pType, String wState,
			boolean storming, boolean aurora, boolean rainbow,
			boolean solarEcl, boolean lunarEcl) {
		surfaceTemp = temp;
		surfaceHumidity = humidity;
		surfacePressure = pressure;
		surfaceWindSpeed = windSpeed;
		surfaceWindDir = windDir;
		cloudCover = cloud;
		precipRate = pRate;
		try { precipType = PrecipType.valueOf(pType); } catch (Exception e) { precipType = PrecipType.NONE; }
		try { weatherState = WeatherState.valueOf(wState); } catch (Exception e) { weatherState = WeatherState.CLEAR; }
		solarEclipseActive = solarEcl;
		lunarEclipseActive = lunarEcl;
		auroraActive = aurora;
		rainbowActive = rainbow;
		if (Dungeon.cycleTurn > 0) computeLight(Dungeon.cycleTurn);
		computeLocalValues();
	}

	/** Reset all state (for safety / testing). */
	public static void reset() {
		surfaceTemp = 15f;
		surfaceHumidity = 0.50f;
		surfacePressure = 1013f;
		surfaceWindSpeed = 3f;
		surfaceWindDir = 180f;
		cloudCover = 0f;
		precipRate = 0f;
		precipType = PrecipType.NONE;
		weatherState = WeatherState.CLEAR;
		prevWeatherState = WeatherState.CLEAR;
		prevPrecipType = PrecipType.NONE;
		frontQueue.clear();
		frontIndex = 0;
		frontGenSeed = 0;
		lastFrontGenTurn = 0;
		currentDepth = 1;
		// Light state
		solarElevation = 0f;
		sunLight = 0f;
		moonLight = 0f;
		surfaceLight = 0f;
		moonVisibility = 0f;
		moonElevation = 0f;
		localAmbientLight = TORCH_BASE_LIGHT;
		solarColorRGB = new float[]{1f, 1f, 1f};
		moonColorRGB = new float[]{0.65f, 0.70f, 0.90f};
		solarEclipseActive = false; solarEclipseRemaining = 0;
		lunarEclipseActive = false; lunarEclipseRemaining = 0;
		auroraActive = false; auroraRemaining = 0;
		rainbowActive = false; rainbowRemaining = 0;
		computeLocalValues();
	}
}
