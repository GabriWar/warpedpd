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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class GameCalendar {

	// Game lore dates: "85th of Autumn, 375", "1st of Summer, 308", etc.
	// Year offset: game year 376 = real year 2026, so gameYear = realYear - 1650

	public static final int YEAR_OFFSET = 1650;

	public enum Season {
		SPRING,  // Mar-May
		SUMMER,  // Jun-Aug
		AUTUMN,  // Sep-Nov
		WINTER;  // Dec-Feb

		public Season next() {
			switch (this) {
				case SPRING: return SUMMER;
				case SUMMER: return AUTUMN;
				case AUTUMN: return WINTER;
				case WINTER: return SPRING;
				default:     return SPRING;
			}
		}
	}

	// 7-day week mapped to real days
	public enum Weekday {
		STONESDAY,    // Monday
		FORGEDAY,     // Tuesday
		TIDEDAY,      // Wednesday
		STORMDAY,     // Thursday
		SHADOWDAY,    // Friday
		HARVESTDAY,   // Saturday
		LIGHTDAY;     // Sunday

		public static final int DAYS_PER_WEEK = 7;
	}

	// Moon phases
	public enum MoonPhase {
		NEW_MOON,
		WAXING_CRESCENT,
		FIRST_QUARTER,
		WAXING_GIBBOUS,
		FULL_MOON,
		WANING_GIBBOUS,
		LAST_QUARTER,
		WANING_CRESCENT;
	}

	// =========================================================================
	// CLOCK MODE — mirrors real-world calendar exactly
	// =========================================================================

	// Real season from current month
	private static Season realSeason() {
		int month = Calendar.getInstance().get(Calendar.MONTH);
		if (month >= Calendar.MARCH && month <= Calendar.MAY)          return Season.SPRING;
		if (month >= Calendar.JUNE && month <= Calendar.AUGUST)        return Season.SUMMER;
		if (month >= Calendar.SEPTEMBER && month <= Calendar.NOVEMBER) return Season.AUTUMN;
		return Season.WINTER;
	}

	// Real day within the current season (1-indexed)
	private static int realDayOfSeason() {
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		switch (realSeason()) {
			case SPRING:
				if (month == Calendar.MARCH) return day;
				if (month == Calendar.APRIL) return 31 + day;
				return 31 + 30 + day;
			case SUMMER:
				if (month == Calendar.JUNE) return day;
				if (month == Calendar.JULY) return 30 + day;
				return 30 + 31 + day;
			case AUTUMN:
				if (month == Calendar.SEPTEMBER) return day;
				if (month == Calendar.OCTOBER) return 30 + day;
				return 30 + 31 + day;
			case WINTER:
				if (month == Calendar.DECEMBER) return day;
				if (month == Calendar.JANUARY) return 31 + day;
				return 31 + 31 + day;
			default: return day;
		}
	}

	// Days in each season for the real calendar
	// Spring: 31+30+31 = 92, Summer: 30+31+31 = 92
	// Autumn: 30+31+30 = 91, Winter: 31+31+28/29 = 90 or 91
	private static int realDaysInSeason(Season s) {
		switch (s) {
			case SPRING: return 92;
			case SUMMER: return 92;
			case AUTUMN: return 91;
			case WINTER:
				Calendar cal = Calendar.getInstance();
				int year = cal.get(Calendar.YEAR);
				return new GregorianCalendar().isLeapYear(year) ? 91 : 90;
			default: return 91;
		}
	}

	// Real year mapped to game year
	private static int realYear() {
		return Calendar.getInstance().get(Calendar.YEAR) - YEAR_OFFSET;
	}

	// Real weekday → game weekday
	private static Weekday realWeekday() {
		int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		switch (dow) {
			case Calendar.MONDAY:    return Weekday.STONESDAY;
			case Calendar.TUESDAY:   return Weekday.FORGEDAY;
			case Calendar.WEDNESDAY: return Weekday.TIDEDAY;
			case Calendar.THURSDAY:  return Weekday.STORMDAY;
			case Calendar.FRIDAY:    return Weekday.SHADOWDAY;
			case Calendar.SATURDAY:  return Weekday.HARVESTDAY;
			case Calendar.SUNDAY:    return Weekday.LIGHTDAY;
			default:                 return Weekday.STONESDAY;
		}
	}

	// Real moon phase using synodic month (~29.53 days)
	// Reference new moon: Jan 6, 2000 18:14 UTC (JD 2451550.26)
	private static MoonPhase realMoonPhase() {
		Calendar cal = Calendar.getInstance();
		double jd = toJulianDay(cal);
		double daysSinceNewMoon = (jd - 2451550.1) % 29.530588;
		if (daysSinceNewMoon < 0) daysSinceNewMoon += 29.530588;
		int idx = (int)((daysSinceNewMoon / 29.530588) * 8) % 8;
		return MoonPhase.values()[idx];
	}

	private static double toJulianDay(Calendar cal) {
		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH) + 1;
		int d = cal.get(Calendar.DAY_OF_MONTH);
		int h = cal.get(Calendar.HOUR_OF_DAY);
		if (m <= 2) { y--; m += 12; }
		int A = y / 100;
		int B = 2 - A + (A / 4);
		return (int)(365.25 * (y + 4716)) + (int)(30.6001 * (m + 1))
				+ d + h / 24.0 + B - 1524.5;
	}

	// Sunrise/sunset in fractional hours for ~50°N latitude
	// Uses sine approximation of day length through the year
	public static float[] sunriseSunset() {
		int doy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		// Spring equinox ~day 80, summer solstice ~day 172
		double angle = (doy - 80) * 2 * Math.PI / 365.25;
		// Half-day ranges from ~3.75h (winter) to ~8.25h (summer), centered at 6h
		double halfDay = 6.0 + 2.25 * Math.sin(angle);
		float sunrise = (float)(12.0 - halfDay);
		float sunset = (float)(12.0 + halfDay);
		return new float[]{sunrise, sunset};
	}

	// =========================================================================
	// TURN-BASED MODE — game calendar with leap years
	// =========================================================================

	// Turn-based: 91 days per season, 364 per year
	// Leap year: every 4th year, Winter gets 92 days → 365 total
	// (Gregorian-like: skip century years except every 400th)

	private static final int BASE_DAYS_PER_SEASON = 91;
	private static final int BASE_DAYS_PER_YEAR = BASE_DAYS_PER_SEASON * 4; // 364

	// Base year for random start (seed picks a date near this)
	private static final int BASE_YEAR = 376;

	// Generate a starting absolute day from the dungeon seed.
	// Year: 374-378, any season, any day within season.
	public static int generateStartDay(long seed) {
		// Use seed bits deterministically
		java.util.Random rng = new java.util.Random(seed ^ 0xCA1E9DA7L);
		int year = BASE_YEAR + rng.nextInt(5) - 2; // 374..378
		int seasonOrd = rng.nextInt(4);             // 0..3
		int day = rng.nextInt(BASE_DAYS_PER_SEASON);// 0..90

		// Compute absolute day for this date
		int days = 0;
		for (int y = 0; y < year; y++) {
			days += daysInYear(y);
		}
		for (int s = 0; s < seasonOrd; s++) {
			days += daysInSeason(Season.values()[s], year);
		}
		days += day;
		return days;
	}

	public static boolean isLeapYear(int year) {
		if (year % 400 == 0) return true;
		if (year % 100 == 0) return false;
		return year % 4 == 0;
	}

	public static int daysInYear(int year) {
		return isLeapYear(year) ? 365 : 364;
	}

	public static int daysInSeason(Season s, int year) {
		if (s == Season.WINTER && isLeapYear(year)) return 92;
		return BASE_DAYS_PER_SEASON;
	}

	private static int gameDaysElapsed() {
		return Dungeon.cycleTurn / DayNightCycle.FULL_CYCLE;
	}

	// Resolve absolute day into year, season, dayOfSeason
	private static int[] resolveAbsoluteDay(int absDay) {
		int year = 0;
		int remaining = absDay;

		// Fast-forward through 400-year cycles (400 years = 364*400 + 97 leap days)
		int cycle400 = 364 * 400 + 97;
		if (remaining >= cycle400) {
			int periods = remaining / cycle400;
			year += periods * 400;
			remaining -= periods * cycle400;
		}

		while (remaining >= daysInYear(year)) {
			remaining -= daysInYear(year);
			year++;
		}

		int seasonOrd = 0;
		while (seasonOrd < 4) {
			int dInSeason = daysInSeason(Season.values()[seasonOrd], year);
			if (remaining < dInSeason) break;
			remaining -= dInSeason;
			seasonOrd++;
		}
		if (seasonOrd >= 4) seasonOrd = 3; // safety

		return new int[]{year, seasonOrd, remaining + 1}; // +1 for 1-indexed day
	}

	// Turn-based game day absolute
	private static int turnAbsoluteDay() {
		return Dungeon.calendarStartDay + gameDaysElapsed();
	}

	private static int[] turnDateComponents() {
		return resolveAbsoluteDay(turnAbsoluteDay());
	}

	private static Weekday turnWeekday() {
		return Weekday.values()[turnAbsoluteDay() % Weekday.DAYS_PER_WEEK];
	}

	// Turn-based moon phase: 29-day cycle anchored to calendarStartDay (seed-derived)
	private static MoonPhase turnMoonPhase() {
		int dayInCycle = turnAbsoluteDay() % 29;
		int idx = (dayInCycle * 8) / 29;
		return MoonPhase.values()[idx];
	}

	//continuous position in the lunar cycle, 0..1 with 0 = new moon. The sundial uses
	//it to interpolate the phase icon and pin instead of stepping through 8 buckets.
	public static float moonProgress() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) {
			double jd = System.currentTimeMillis() / 86400000.0 + 2440587.5;
			double days = (jd - 2451550.1) % 29.530588;
			if (days < 0) days += 29.530588;
			return (float)(days / 29.530588);
		}
		return (turnAbsoluteDay() % 29) / 29f;
	}

	// =========================================================================
	// PUBLIC API — delegates to clock or turn-based
	// =========================================================================

	public static int year() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return realYear();
		return turnDateComponents()[0];
	}

	public static Season season() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return realSeason();
		return Season.values()[turnDateComponents()[1]];
	}

	public static int dayOfSeason() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return realDayOfSeason();
		return turnDateComponents()[2];
	}

	public static int daysInCurrentSeason() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return realDaysInSeason(realSeason());
		int[] d = turnDateComponents();
		return daysInSeason(Season.values()[d[1]], d[0]);
	}

	public static boolean isCurrentYearLeap() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) {
			return new GregorianCalendar().isLeapYear(
					Calendar.getInstance().get(Calendar.YEAR));
		}
		return isLeapYear(turnDateComponents()[0]);
	}

	public static int weekOfSeason() {
		return ((dayOfSeason() - 1) / Weekday.DAYS_PER_WEEK) + 1;
	}

	public static Weekday weekday() {
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return realWeekday();
		return turnWeekday();
	}

	// Debug override: null = disabled
	public static MoonPhase debugMoonOverride = null;

	public static MoonPhase moonPhase() {
		if (debugMoonOverride != null) return debugMoonOverride;
		if (Dungeon.isChallenged(Challenges.REAL_CLOCK)) return realMoonPhase();
		return turnMoonPhase();
	}

	public static boolean isFullMoon() {
		return moonPhase() == MoonPhase.FULL_MOON;
	}

	public static boolean isNewMoon() {
		return moonPhase() == MoonPhase.NEW_MOON;
	}

	// =========================================================================
	// GAMEPLAY EFFECTS
	// =========================================================================

	public static float moonDamageMultiplier() {
		switch (moonPhase()) {
			case FULL_MOON:      return 1.15f;
			case NEW_MOON:       return 0.90f;
			case WAXING_GIBBOUS: return 1.05f;
			case WANING_GIBBOUS: return 1.05f;
			default:             return 1f;
		}
	}

	public static float moonSpawnMultiplier() {
		switch (moonPhase()) {
			case FULL_MOON:        return 0.50f;  // moonlight wards off creatures
			case WANING_GIBBOUS:   return 0.65f;
			case WAXING_GIBBOUS:   return 0.65f;
			case FIRST_QUARTER:    return 0.85f;
			case LAST_QUARTER:     return 0.85f;
			case WAXING_CRESCENT:  return 1.15f;
			case WANING_CRESCENT:  return 1.15f;
			case NEW_MOON:         return 1.50f;  // total darkness breeds horrors
			default:               return 1f;
		}
	}

	// ---- Season modifiers ----

	// Plant seed preservation chance bonus (added on top of existing chance)
	public static float seasonSeedBonus() {
		switch (season()) {
			case SPRING: return 0.25f;  // spring nurtures growth
			case SUMMER: return 0.10f;  // warmth helps
			case AUTUMN: return 0f;
			case WINTER: return -0.10f; // harsh winter kills seeds
			default:     return 0f;
		}
	}

	// Multiplier on loot drop chance
	public static float seasonLootMultiplier() {
		switch (season()) {
			case SPRING: return 1f;
			case SUMMER: return 0.80f;  // scorching heat saps bounty
			case AUTUMN: return 1.35f;  // harvest bounty
			case WINTER: return 1.10f;  // frozen caches preserved
			default:     return 1f;
		}
	}

	// Multiplier on gold amounts
	public static float seasonGoldMultiplier() {
		switch (season()) {
			case SPRING: return 1f;
			case SUMMER: return 0.85f;  // heat corrodes
			case AUTUMN: return 1.40f;  // harvest wealth
			case WINTER: return 1.15f;  // frozen hoards uncovered
			default:     return 1f;
		}
	}

	// Damage multiplier from season (affects all combatants)
	public static float seasonDamageMultiplier() {
		switch (season()) {
			case SPRING: return 1f;
			case SUMMER: return 1.15f;  // heat fuels aggression
			case AUTUMN: return 1f;
			case WINTER: return 0.85f;  // bitter cold numbs strikes
			default:     return 1f;
		}
	}

	// ---- Weekday modifiers ----

	// Damage multiplier from weekday
	public static float weekdayDamageMultiplier() {
		switch (weekday()) {
			case FORGEDAY:  return 1.20f;  // forge-tempered strikes
			case STORMDAY:  return 1.10f;  // thunderous blows
			default:        return 1f;
		}
	}

	// Loot multiplier from weekday
	public static float weekdayLootMultiplier() {
		switch (weekday()) {
			case HARVESTDAY: return 1.35f;  // bountiful harvest day
			default:         return 1f;
		}
	}

	// Evasion bonus from weekday
	public static int weekdayEvasionBonus() {
		switch (weekday()) {
			case SHADOWDAY: return 3;  // shadows cloak the agile
			default:        return 0;
		}
	}

	// Defense multiplier from weekday
	public static float weekdayDefenseMultiplier() {
		switch (weekday()) {
			case STONESDAY: return 1.20f;  // stone-hard defense
			case LIGHTDAY:  return 1.15f;  // light shields the faithful
			default:        return 1f;
		}
	}

	// Healing multiplier from weekday
	public static float weekdayHealingMultiplier() {
		switch (weekday()) {
			case TIDEDAY: return 1.25f;  // tides of restoration
			default:      return 1f;
		}
	}

	// =========================================================================
	// FORMATTING
	// =========================================================================

	/**
	 * Every calendar modifier that is not neutral today, as {label, value} pairs
	 * ("+35%" / "-15%", evasion as a flat "+3"). Read-only; for the hero stats and
	 * sundial guide "today" blocks.
	 */
	public static ArrayList<String[]> activeModifiers() {
		ArrayList<String[]> out = new ArrayList<>();
		addModifier(out, "mod_season_loot",      seasonLootMultiplier());
		addModifier(out, "mod_season_gold",      seasonGoldMultiplier());
		addModifier(out, "mod_season_damage",    seasonDamageMultiplier());
		addModifier(out, "mod_weekday_damage",   weekdayDamageMultiplier());
		addModifier(out, "mod_weekday_loot",     weekdayLootMultiplier());
		int evasion = weekdayEvasionBonus();
		if (evasion != 0) {
			out.add(new String[]{ Messages.get(GameCalendar.class, "mod_weekday_evasion"),
					(evasion > 0 ? "+" : "") + evasion });
		}
		addModifier(out, "mod_weekday_defense",  weekdayDefenseMultiplier());
		addModifier(out, "mod_weekday_healing",  weekdayHealingMultiplier());
		addModifier(out, "mod_moon_damage",      moonDamageMultiplier());
		addModifier(out, "mod_moon_spawn",       moonSpawnMultiplier());
		return out;
	}

	private static void addModifier(ArrayList<String[]> out, String key, float multiplier) {
		int pct = Math.round((multiplier - 1f) * 100f);
		if (pct == 0) return;
		out.add(new String[]{ Messages.get(GameCalendar.class, key), (pct > 0 ? "+" : "") + pct + "%" });
	}

	public static String dateString() {
		int day = dayOfSeason();
		String seasonName = Messages.get(GameCalendar.class, season().name().toLowerCase());
		String weekdayName = Messages.get(GameCalendar.class, "day_" + weekday().name().toLowerCase());
		return Messages.get(GameCalendar.class, "date_format",
				weekdayName, ordinal(day), seasonName, year());
	}

	public static String shortDateString() {
		int day = dayOfSeason();
		String seasonName = Messages.get(GameCalendar.class, season().name().toLowerCase());
		return Messages.get(GameCalendar.class, "date_short", ordinal(day), seasonName, year());
	}

	public static String weekdayString() {
		return Messages.get(GameCalendar.class, "day_" + weekday().name().toLowerCase());
	}

	public static String moonPhaseString() {
		return Messages.get(GameCalendar.class, "moon_" + moonPhase().name().toLowerCase());
	}

	private static String ordinal(int n) {
		if (n >= 11 && n <= 13) return n + "th";
		switch (n % 10) {
			case 1:  return n + "st";
			case 2:  return n + "nd";
			case 3:  return n + "rd";
			default: return n + "th";
		}
	}
}
