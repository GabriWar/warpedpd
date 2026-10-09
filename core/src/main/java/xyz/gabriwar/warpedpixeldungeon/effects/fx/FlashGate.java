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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import java.util.function.LongSupplier;

/**
 * Every flash of the whole screen asks here first, so the screen never strobes: a storm's sky
 * flashes at most once in STORM_GAP seconds, the game's own flashes (a scroll, a bomb, a wand's
 * lightning struck in sight) once in GAME_GAP, the red of a heavy blow once in DAMAGE_GAP, and
 * the storm's and the game's together no more than WINDOW_MAX times in any WINDOW seconds. A
 * flash refused is simply not drawn; whatever local light the effect has still is.
 *
 * Real seconds (System.nanoTime), so neither the time scale nor a slow frame changes them. Any
 * thread: the actor thread asks for the damage flash.
 */
public final class FlashGate {

	private FlashGate(){}

	public static final float STORM_GAP = 0.7f, GAME_GAP = 2.0f, DAMAGE_GAP = 0.35f;
	public static final int WINDOW_MAX = 3;
	public static final float WINDOW = 10f;
	/** How bright a passed flash may be: a storm's, and the game's softer one. */
	public static final float PEAK_STORM = 0.6f, PEAK_GAME = 0.35f;

	//the clock, in nanoseconds; a test hands its own
	static LongSupplier clock = System::nanoTime;

	private static double lastStorm = Double.NEGATIVE_INFINITY, lastGame = Double.NEGATIVE_INFINITY,
			lastDamage = Double.NEGATIVE_INFINITY;
	//the last WINDOW_MAX storm and game flashes, oldest first
	private static final double[] recent = new double[WINDOW_MAX];
	private static int recentCount = 0;

	private static double now(){
		return clock.getAsLong() / 1e9;
	}

	/** A storm's flash may go now (and is counted as gone). */
	public static synchronized boolean storm(){
		double t = now();
		if (t - lastStorm < STORM_GAP || !windowOpen( t )) return false;
		lastStorm = t;
		remember( t );
		return true;
	}

	/** One of the game's own full-screen flashes may go now (and is counted as gone). */
	public static synchronized boolean game(){
		double t = now();
		if (t - lastGame < GAME_GAP || !windowOpen( t )) return false;
		lastGame = t;
		remember( t );
		return true;
	}

	/** The hero's red flash for a heavy blow may go now (and is counted as gone). */
	public static synchronized boolean damage(){
		double t = now();
		if (t - lastDamage < DAMAGE_GAP) return false;
		lastDamage = t;
		return true;
	}

	/** Forgets every flash: a new scene starts with the gates open. */
	public static synchronized void reset(){
		lastStorm = lastGame = lastDamage = Double.NEGATIVE_INFINITY;
		recentCount = 0;
	}

	//fewer than WINDOW_MAX storm or game flashes in the last WINDOW seconds
	private static boolean windowOpen( double t ){
		return recentCount < WINDOW_MAX || t - recent[0] >= WINDOW;
	}

	private static void remember( double t ){
		if (recentCount < WINDOW_MAX){
			recent[recentCount++] = t;
		} else {
			System.arraycopy( recent, 1, recent, 0, WINDOW_MAX - 1 );
			recent[WINDOW_MAX - 1] = t;
		}
	}
}
