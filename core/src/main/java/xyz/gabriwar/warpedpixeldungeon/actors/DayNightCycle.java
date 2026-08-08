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
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WeatherBlobSpawner;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.plants.PlantGrowthManager;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.Calendar;

public class DayNightCycle {

	public enum Phase {
		DAWN,
		DAY,
		DUSK,
		NIGHT;

		public Phase next() {
			switch (this) {
				case DAWN:  return DAY;
				case DAY:   return DUSK;
				case DUSK:  return NIGHT;
				case NIGHT: return DAWN;
				default:    return DAY;
			}
		}
	}

	// Total turns per day/night cycle (constant regardless of season)
	public static final int FULL_CYCLE = 2500;

	// =========================================================================
	// TURN-BASED: seasonal phase durations
	// =========================================================================
	// Total always = 2500. Summer = long days, Winter = short days.
	// Dawn and Dusk stay fixed at 250 each; Day+Night = 2000.

	private static final int DAWN_TURNS = 250;
	private static final int DUSK_TURNS = 250;

	// Day turns by season (Night = 2000 - dayTurns)
	private static int dayTurnsBySeason() {
		GameCalendar.Season s = GameCalendar.season();
		switch (s) {
			case SUMMER: return 1400;  // long summer days
			case SPRING: return 1100;  // slightly longer than average
			case AUTUMN: return 900;   // slightly shorter than average
			case WINTER: return 600;   // short winter days
			default:     return 1000;
		}
	}

	private static int nightTurnsBySeason() {
		return 2000 - dayTurnsBySeason();
	}

	// Phase durations for the current season
	public static int phaseDuration(Phase phase) {
		switch (phase) {
			case DAWN:  return DAWN_TURNS;
			case DAY:   return dayTurnsBySeason();
			case DUSK:  return DUSK_TURNS;
			case NIGHT: return nightTurnsBySeason();
			default:    return 200;
		}
	}

	// =========================================================================
	// CLOCK MODE: real sunrise/sunset
	// =========================================================================

	// Returns current time of day as fractional hours (e.g. 14.5 = 2:30 PM)
	private static float currentHourFloat() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.HOUR_OF_DAY)
				+ cal.get(Calendar.MINUTE) / 60f
				+ cal.get(Calendar.SECOND) / 3600f;
	}

	private static Phase clockPhase() {
		float[] sr = GameCalendar.sunriseSunset();
		float sunrise = sr[0];
		float sunset = sr[1];
		float dawnStart = sunrise - 0.75f;  // 45 min before sunrise
		float duskEnd = sunset + 0.75f;     // 45 min after sunset
		float now = currentHourFloat();

		if (now >= dawnStart && now < sunrise) return Phase.DAWN;
		if (now >= sunrise && now < sunset)    return Phase.DAY;
		if (now >= sunset && now < duskEnd)    return Phase.DUSK;
		return Phase.NIGHT;
	}

	// =========================================================================
	// PHASE DETERMINATION
	// =========================================================================

	// Debug override: null = disabled
	public static Phase debugPhaseOverride = null;

	public static Phase phase() {
		if (debugPhaseOverride != null) return debugPhaseOverride;
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) {
			return clockPhase();
		}
		return turnPhase();
	}

	// Turn-based: determine phase from cycleTurn using seasonal durations
	private static Phase turnPhase() {
		int pos = Dungeon.cycleTurn % FULL_CYCLE;
		int dawnLen = DAWN_TURNS;
		int dayLen = dayTurnsBySeason();
		int duskLen = DUSK_TURNS;

		if (pos < dawnLen) return Phase.DAWN;
		pos -= dawnLen;
		if (pos < dayLen) return Phase.DAY;
		pos -= dayLen;
		if (pos < duskLen) return Phase.DUSK;
		return Phase.NIGHT;
	}

	public static int turnsIntoPhase() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return 0;
		int pos = Dungeon.cycleTurn % FULL_CYCLE;
		int dawnLen = DAWN_TURNS;
		int dayLen = dayTurnsBySeason();
		int duskLen = DUSK_TURNS;

		if (pos < dawnLen) return pos;
		pos -= dawnLen;
		if (pos < dayLen) return pos;
		pos -= dayLen;
		if (pos < duskLen) return pos;
		pos -= duskLen;
		return pos;
	}

	public static int turnsUntilPhaseChange() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return -1;
		return phaseDuration(phase()) - turnsIntoPhase();
	}

	// =========================================================================
	// BOOLEAN QUERIES
	// =========================================================================

	public static boolean isNight() {
		return phase() == Phase.NIGHT;
	}

	public static boolean isDark() {
		Phase p = phase();
		return p == Phase.NIGHT || p == Phase.DUSK;
	}

	public static boolean isBright() {
		Phase p = phase();
		return p == Phase.DAY || p == Phase.DAWN;
	}

	// =========================================================================
	// GAMEPLAY MODIFIERS
	// =========================================================================

	public static float viewDistanceModifier() {
		// Derive darkness from actual solar + lunar intensity instead of discrete phase steps.
		// This makes the penalty grow gradually as the sun sets, with moonlight softening nights.
		float sun  = ClimateManager.sunLight();
		float moon = ClimateManager.moonLight();

		// Combine sky sources; moonlight counts for half (dimmer, blue-shifted)
		float skyLight = Math.min(1f, sun + moon * 0.5f);

		// Penalty is zero when skyLight >= threshold (bright day / full moon).
		// Below the threshold it grows quadratically — sharp at full dark, gentle at twilight.
		final float THRESHOLD = 0.35f; // sun is fully up and strong → no penalty at all
		float darkFactor = Math.max(0f, 1f - skyLight / THRESHOLD);
		darkFactor = darkFactor * darkFactor; // square: stays near 0 at dusk, peaks at dark

		float mod = -4f * darkFactor;

		// Weather reduces visibility proportionally to precipitation rate
		if (Dungeon.level != null) {
			float precipRate = ClimateManager.localPrecipRate();
			PrecipType precipType = ClimateManager.localPrecipType();
			if (precipType == PrecipType.BLIZZARD) {
				mod -= precipRate * 4f;
			} else if (precipRate > 0f) {
				mod -= precipRate * 2f;
			}
			if (ClimateManager.isFoggy()) {
				mod -= 2f;
			}
		}

		// Tiredness narrows the hero's focus: up to -2 sight as drowsiness deepens
		if (Dungeon.hero != null) {
			Sleepiness tired = Dungeon.hero.buff(Sleepiness.class);
			if (tired != null && tired.level() >= Sleepiness.DROWSY) {
				float progress = (tired.level() - Sleepiness.DROWSY) / (Sleepiness.COMATOSE - Sleepiness.DROWSY);
				mod -= 2f * Math.min(1f, progress);
			}
		}

		return mod;
	}

	public static float spawnRateMultiplier() {
		float base;
		switch (phase()) {
			case NIGHT: base = 0.5f;  break;
			case DUSK:  base = 0.75f; break;
			default:    base = 1f;    break;
		}
		float mult = base * GameCalendar.moonSpawnMultiplier();

		// Eclipses dramatically increase spawn rates
		if (ClimateManager.isSolarEclipse()) {
			mult *= 0.35f; // solar eclipse: darkness mid-day, creatures swarm
		}
		if (ClimateManager.isLunarEclipse()) {
			mult *= 0.25f; // blood moon: strongest spawn pressure
		}
		return mult;
	}

	public static float undeadDamageMultiplier() {
		float base = isNight() ? 1.25f : 1f;
		float mult = base * GameCalendar.moonDamageMultiplier();

		// Solar eclipse: undead empowered as if deepest night
		if (ClimateManager.isSolarEclipse()) {
			mult *= 1.5f;
		}
		// Blood moon: massive undead/demonic power surge
		if (ClimateManager.isLunarEclipse()) {
			mult *= 2f;
		}
		return mult;
	}

	public static int stealthBonus() {
		int bonus = isNight() ? 2 : 0;
		if (GameCalendar.isNewMoon()) bonus += 1;
		if (Dungeon.hero != null) {
			if (Dungeon.hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.FireflyGlow.class) != null) {
				bonus += xyz.gabriwar.warpedpixeldungeon.actors.buffs.FireflyGlow.STEALTH_BONUS;
			}
			if (Dungeon.hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.SolarEclipseBuff.class) != null) {
				bonus += xyz.gabriwar.warpedpixeldungeon.actors.buffs.SolarEclipseBuff.STEALTH_BONUS;
			}
		}
		return bonus;
	}

	// =========================================================================
	// VISUAL — smooth continuous interpolation across the full cycle
	// =========================================================================

	// Returns 0.0–1.0 progress within the current phase.
	// For turn-based: turnsIntoPhase / phaseDuration.
	// For clock-based: fractional progress through the clock phase window.
	public static float phaseProgress() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) {
			return clockPhaseProgress();
		}
		Phase p = phase();
		int dur = phaseDuration(p);
		if (dur <= 0) return 0f;
		return Math.min(1f, turnsIntoPhase() / (float) dur);
	}

	private static float clockPhaseProgress() {
		float[] sr = GameCalendar.sunriseSunset();
		float sunrise = sr[0];
		float sunset  = sr[1];
		float dawnStart = sunrise - 0.75f;
		float duskEnd   = sunset  + 0.75f;
		float now = currentHourFloat();

		Phase p = clockPhase();
		switch (p) {
			case DAWN:  return (now - dawnStart) / (sunrise - dawnStart);
			case DAY:   return (now - sunrise) / (sunset - sunrise);
			case DUSK:  return (now - sunset) / (duskEnd - sunset);
			case NIGHT:
			default:
				// Night wraps around midnight
				float nightLen = 24f - (duskEnd - dawnStart);
				float intoNight;
				if (now >= duskEnd) {
					intoNight = now - duskEnd;
				} else {
					intoNight = (24f - duskEnd) + now;
				}
				return nightLen > 0 ? Math.min(1f, intoNight / nightLen) : 0f;
		}
	}

	// Tint color endpoints (ARGB as floats)
	//                              A      R      G      B
	private static final float[] NIGHT_TINT = {0.133f, 0.067f, 0.133f, 0.267f}; // 0x22112244
	private static final float[] DAWN_TINT  = {0.067f, 1.000f, 0.533f, 0.267f}; // 0x11FF8844
	private static final float[] DAY_TINT   = {0.000f, 0.000f, 0.000f, 0.000f}; // 0x00000000
	private static final float[] DUSK_TINT  = {0.067f, 0.533f, 0.267f, 0.400f}; // 0x11884466

	// Brightness endpoints
	private static final float NIGHT_BRIGHT = 0.65f;
	private static final float DAWN_BRIGHT  = 1.05f;
	private static final float DAY_BRIGHT   = 1.00f;
	private static final float DUSK_BRIGHT  = 0.85f;

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	// Returns ARGB tint as 4-float array [A,R,G,B], smoothly interpolated.
	// During transitions (DAWN/DUSK) we interpolate from the previous phase
	// through the peak tint and into the next phase.
	// During steady phases (DAY/NIGHT) values are constant.
	public static float[] phaseTintSmooth() {
		// Delegate to ClimateManager for weather-aware sky tint when in-game
		if (Dungeon.level != null) {
			return ClimateManager.computeSkyTint();
		}
		return fallbackTint();
	}

	/** Original hardcoded tint logic — used outside GameScene (menus, etc). */
	private static float[] fallbackTint() {
		Phase p = phase();
		float t = phaseProgress();
		float[] result = new float[4];

		switch (p) {
			case DAWN:
				if (t < 0.5f) {
					float st = t * 2f;
					for (int i = 0; i < 4; i++) result[i] = lerp(NIGHT_TINT[i], DAWN_TINT[i], st);
				} else {
					float st = (t - 0.5f) * 2f;
					for (int i = 0; i < 4; i++) result[i] = lerp(DAWN_TINT[i], DAY_TINT[i], st);
				}
				break;
			case DAY:
				System.arraycopy(DAY_TINT, 0, result, 0, 4);
				break;
			case DUSK:
				if (t < 0.5f) {
					float st = t * 2f;
					for (int i = 0; i < 4; i++) result[i] = lerp(DAY_TINT[i], DUSK_TINT[i], st);
				} else {
					float st = (t - 0.5f) * 2f;
					for (int i = 0; i < 4; i++) result[i] = lerp(DUSK_TINT[i], NIGHT_TINT[i], st);
				}
				break;
			case NIGHT:
			default:
				System.arraycopy(NIGHT_TINT, 0, result, 0, 4);
				break;
		}
		return result;
	}

	public static float brightnessMult() {
		// Delegate to ClimateManager for weather-aware brightness when in-game.
		// localAmbientLight() returns 0.25–1.0 (torch base → full sun).
		// The overlay formula (darkA = 1−brightness) was designed for the old
		// range 0.65–1.0, so we remap to keep nights dark but playable.
		if (Dungeon.level != null) {
			float raw = ClimateManager.localAmbientLight(); // 0.25 – 1.0
			return Math.min(1f, 0.55f + raw * 0.45f);       // 0.66 – 1.0
		}
		return fallbackBrightness();
	}

	/** Original hardcoded brightness logic — used outside GameScene. */
	private static float fallbackBrightness() {
		Phase p = phase();
		float t = phaseProgress();

		float base;
		switch (p) {
			case DAWN:
				if (t < 0.5f) {
					base = lerp(NIGHT_BRIGHT, DAWN_BRIGHT, t * 2f);
				} else {
					base = lerp(DAWN_BRIGHT, DAY_BRIGHT, (t - 0.5f) * 2f);
				}
				break;
			case DAY:
				base = DAY_BRIGHT;
				break;
			case DUSK:
				if (t < 0.5f) {
					base = lerp(DAY_BRIGHT, DUSK_BRIGHT, t * 2f);
				} else {
					base = lerp(DUSK_BRIGHT, NIGHT_BRIGHT, (t - 0.5f) * 2f);
				}
				break;
			case NIGHT:
			default:
				base = NIGHT_BRIGHT;
				break;
		}

		if (GameCalendar.isFullMoon() && isNight()) {
			base += 0.10f;
		}
		return base;
	}

	// Legacy integer tint for anything that still needs it
	public static int phaseTint() {
		float[] t = phaseTintSmooth();
		int a = Math.round(t[0] * 255f);
		int r = Math.round(t[1] * 255f);
		int g = Math.round(t[2] * 255f);
		int b = Math.round(t[3] * 255f);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// =========================================================================
	// TURN PROCESSING
	// =========================================================================

	private static Phase lastPhase = null;

	public static void onHeroTurn() {
		Phase before = lastPhase;

		if (!Dungeon.isChallenged(Challenges.REAL_CLOCK)) {
			Dungeon.cycleTurn++;
		}

		Phase after = phase();

		if (before != null && before != after) {
			onPhaseChange(before, after);
		}
		lastPhase = after;

		ClimateManager.onHeroTurn();
		WeatherBlobSpawner.onHeroTurn();
		PlantGrowthManager.onHeroTurn(Dungeon.level);
	}

	public static void syncPhase() {
		lastPhase = phase();
	}

	private static void onPhaseChange(Phase from, Phase to) {
		String msg = Messages.get(DayNightCycle.class, to.name().toLowerCase());
		if (msg != null && !msg.isEmpty() && !msg.startsWith("!!!")) {
			GLog.h(msg);
		}

		if (to == Phase.NIGHT) {
			String moonMsg = Messages.get(DayNightCycle.class, "moon_rises",
					GameCalendar.moonPhaseString());
			if (moonMsg != null && !moonMsg.startsWith("!!!")) {
				GLog.i(moonMsg);
			}
		}

		// Log date on dawn (start of new day)
		if (to == Phase.DAWN) {
			String dateMsg = Messages.get(DayNightCycle.class, "new_day",
					GameCalendar.dateString());
			if (dateMsg != null && !dateMsg.startsWith("!!!")) {
				GLog.i(dateMsg);
			}
		}

		if (Dungeon.level != null) {
			Dungeon.observe();
			GameScene.updateFog();
		}
	}
}
