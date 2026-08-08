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

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/**
 * A weather front is a moving air mass that drives weather changes over time.
 * Fronts have a type, duration, intensity, and associated climate shifts.
 * They produce a bell-curve of cloud cover / precipitation as they pass through.
 */
public class WeatherFront implements Bundlable {

	public enum Type {
		WARM,        // gradual onset, steady rain, slow to clear
		COLD,        // sharp onset, intense but shorter, storms possible
		STATIONARY   // lingering overcast, drizzle, can last a long time
	}

	public Type type;
	public int arrivalTurn;    // cycleTurn when this front begins
	public int duration;       // how many turns the front lasts
	public float intensity;    // 0.0-1.0, peak cloud cover / precip strength
	public float tempShift;    // °C change during the front (+ = warm, - = cold)
	public float humidityBoost;// added to base humidity during the front
	public float windBase;     // base wind speed during the front

	public WeatherFront() {}

	public WeatherFront(Type type, int arrivalTurn, int duration,
	                     float intensity, float tempShift,
	                     float humidityBoost, float windBase) {
		this.type = type;
		this.arrivalTurn = arrivalTurn;
		this.duration = duration;
		this.intensity = intensity;
		this.tempShift = tempShift;
		this.humidityBoost = humidityBoost;
		this.windBase = windBase;
	}

	/**
	 * Returns a 0-1 progress curve value for the given number of turns into the front.
	 * Warm fronts ramp up slowly and linger; cold fronts spike fast then fade.
	 */
	public float progressCurve(int turnsInto) {
		if (duration <= 0) return 0f;
		float t = Math.max(0f, Math.min(1f, turnsInto / (float) duration));

		switch (type) {
			case WARM:
				// Slow ramp up (first 40%), sustained peak (40-70%), slow fade (70-100%)
				if (t < 0.4f) return (t / 0.4f) * (t / 0.4f); // quadratic ramp
				if (t < 0.7f) return 1f;                         // sustained peak
				return 1f - ((t - 0.7f) / 0.3f);                // linear fade

			case COLD:
				// Sharp ramp (first 15%), brief peak (15-35%), long fade (35-100%)
				if (t < 0.15f) return t / 0.15f;                // fast ramp
				if (t < 0.35f) return 1f;                        // peak
				return 1f - ((t - 0.35f) / 0.65f);              // gradual fade

			case STATIONARY:
				// Gentle sine-like envelope, never quite at full intensity
				return 0.7f * (float) Math.sin(Math.PI * t);

			default:
				return 0f;
		}
	}

	/** Whether the front has fully passed given the current cycleTurn. */
	public boolean isExpired(int cycleTurn) {
		return cycleTurn >= arrivalTurn + duration;
	}

	/** Whether the front is currently active. */
	public boolean isActive(int cycleTurn) {
		return cycleTurn >= arrivalTurn && cycleTurn < arrivalTurn + duration;
	}

	/** Turns elapsed since this front began (0 if not yet arrived). */
	public int turnsInto(int cycleTurn) {
		return Math.max(0, cycleTurn - arrivalTurn);
	}

	// ---- Bundle persistence ----

	private static final String TYPE_KEY      = "type";
	private static final String ARRIVAL       = "arrival";
	private static final String DURATION_KEY  = "duration";
	private static final String INTENSITY_KEY = "intensity";
	private static final String TEMP_SHIFT    = "tempShift";
	private static final String HUMIDITY_KEY  = "humidity";
	private static final String WIND_KEY      = "wind";

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(TYPE_KEY, type);
		bundle.put(ARRIVAL, arrivalTurn);
		bundle.put(DURATION_KEY, duration);
		bundle.put(INTENSITY_KEY, intensity);
		bundle.put(TEMP_SHIFT, tempShift);
		bundle.put(HUMIDITY_KEY, humidityBoost);
		bundle.put(WIND_KEY, windBase);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		type = bundle.getEnum(TYPE_KEY, Type.class);
		arrivalTurn = bundle.getInt(ARRIVAL);
		duration = bundle.getInt(DURATION_KEY);
		intensity = bundle.getFloat(INTENSITY_KEY);
		tempShift = bundle.getFloat(TEMP_SHIFT);
		humidityBoost = bundle.getFloat(HUMIDITY_KEY);
		windBase = bundle.getFloat(WIND_KEY);
	}
}
