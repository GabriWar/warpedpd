/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

/**
 * The Tinkerer's Dew Condenser, read off the live climate engine each turn:
 * the waterskin condenses dew out of humid air.
 */
public class WeatherAttunement {

	//humidity below this yields nothing; at 1.0 the skin gains a drop every 100 turns
	private static final float DRY_POINT = 0.35f;
	private static final float PEAK_RATE = 0.01f;

	/** drops per turn the waterskin condenses at the hero's cell, before charge bonuses */
	public static float condenseRate(){
		float h = ClimateManager.localHumidity();
		float rate = h <= DRY_POINT ? 0f : (h - DRY_POINT) / (1f - DRY_POINT) * PEAK_RATE;
		if (ClimateManager.isRaining() || ClimateManager.isSnowing()) rate += 0.004f;
		else if (ClimateManager.isFoggy()) rate += 0.002f;
		return rate;
	}

	/** turns per drop at the current rate, or 0 when the air is too dry */
	public static int turnsPerDrop( float rate ){
		return rate <= 0f ? 0 : Math.max( 1, Math.round( 1f / rate ) );
	}

}
