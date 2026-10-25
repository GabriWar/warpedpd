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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import com.watabou.utils.Random;

/**
 * The rules of fishing (FishingRod, FishingLine), pure so the tests pin them:
 *
 * - how readily a fish takes a hook (weight): where it lives, times the bait, the hour, the season and
 *   the weather it likes (FishSpecies' tables). In a cave's pool only the water and the bait count;
 * - what comes up (roll): now and then not a fish at all - an old boot, anywhere but the caves; a
 *   bottle with a message in it, mostly out of the sea and the rivers; a sodden purse - else a fish
 *   drawn by weight;
 * - how long the line waits for a bite (waitTurns): four to eight turns over the square root of how
 *   keen the fish here are now (the weights summed, over KEEN), never under WAIT_FLOOR nor over
 *   WAIT_CAP. A good bait at a good hour bites in two or three turns, the wrong one in a storm takes
 *   the most;
 * - how long a bite holds (BITE_TURNS): reel in within it or the fish is gone with the bait.
 */
public final class FishingRules {

	private FishingRules(){}

	/** What comes up instead of a fish. */
	public enum Junk { BOOT, BOTTLE, PURSE }

	/** How far a line is cast (cells, any way); a hole in the ice is cut beside the hero. */
	public static final int CAST_RANGE = 4, ICE_RANGE = 1;
	/** Turns a bite holds on. */
	public static final int BITE_TURNS = 3;
	/** The wait's spread before the fish's keenness, and its bounds. */
	public static final int WAIT_MIN = 4, WAIT_MAX = 8, WAIT_FLOOR = 2, WAIT_CAP = 18;
	/** The summed weights that make an ordinary wait. */
	public static final float KEEN = 25f;

	/** How readily a fish takes this bait in these waters at this hour, season and weather (0: never). */
	public static float weight( FishSpecies f, FishingWaters.Kind water, Bait bait,
			DayNightCycle.Phase phase, GameCalendar.Season season, VillageRoutine.Weather sky ){
		if (!f.livesIn( water )) return 0f;
		float w = f.waters[water.ordinal()] * f.baits[bait.ordinal()];
		//down in the rock there is no hour, no season and no weather
		if (water != FishingWaters.Kind.CAVE){
			w *= f.hours[phase.ordinal()] * f.seasons[season.ordinal()] * f.skies[sky.ordinal()];
		}
		return w;
	}

	/** Every fish's weight, in FishSpecies' order. */
	public static float[] weights( FishingWaters.Kind water, Bait bait,
			DayNightCycle.Phase phase, GameCalendar.Season season, VillageRoutine.Weather sky ){
		FishSpecies[] all = FishSpecies.values();
		float[] out = new float[all.length];
		for (int i = 0; i < all.length; i++) out[i] = weight( all[i], water, bait, phase, season, sky );
		return out;
	}

	/** How keen the fish here are now: their weights summed. */
	public static float keenness( FishingWaters.Kind water, Bait bait,
			DayNightCycle.Phase phase, GameCalendar.Season season, VillageRoutine.Weather sky ){
		float sum = 0f;
		for (float w : weights( water, bait, phase, season, sky )) sum += w;
		return sum;
	}

	/** The chance a catch is this piece of junk instead of a fish. */
	public static float junkChance( Junk junk, FishingWaters.Kind water ){
		switch (junk){
			case BOOT:
				//nobody loses a boot down in the rock
				return water == FishingWaters.Kind.CAVE ? 0f : water == FishingWaters.Kind.ICE ? 0.015f : 0.03f;
			case BOTTLE:
				return water == FishingWaters.Kind.SEA ? 0.025f
						: water == FishingWaters.Kind.RIVER ? 0.01f
						: water == FishingWaters.Kind.LAKE ? 0.008f : 0f;
			case PURSE: default:
				return water == FishingWaters.Kind.CAVE ? 0.004f : 0.008f;
		}
	}

	/**
	 * The turns until a bite for the fish's keenness here (keenness) and a roll in [0, 1): four to
	 * eight turns over the square root of keenness / KEEN, between WAIT_FLOOR and WAIT_CAP.
	 */
	public static int waitTurns( float keenness, float roll ){
		float base = WAIT_MIN + roll * (WAIT_MAX - WAIT_MIN + 1);
		float k = Math.max( 0.01f, keenness / KEEN );
		int turns = Math.round( (float)Math.floor( base ) / (float)Math.sqrt( k ) );
		return Math.max( WAIT_FLOOR, Math.min( WAIT_CAP, turns ) );
	}

	/** What a bite brings up: a fish or a piece of junk. */
	public static final class Catch {
		public final FishSpecies fish;
		public final Junk junk;

		private Catch( FishSpecies fish, Junk junk ){
			this.fish = fish;
			this.junk = junk;
		}

		public static Catch of( FishSpecies fish ){
			return new Catch( fish, null );
		}

		public static Catch of( Junk junk ){
			return new Catch( null, junk );
		}
	}

	/**
	 * What comes up, from two rolls in [0, 1): the first against the junk's chances (boot, bottle,
	 * purse in turn), the second among the fish by weight. Null only where nothing lives at all.
	 */
	public static Catch pick( FishingWaters.Kind water, Bait bait, DayNightCycle.Phase phase,
			GameCalendar.Season season, VillageRoutine.Weather sky, float junkRoll, float fishRoll ){
		float edge = 0f;
		for (Junk j : Junk.values()){
			edge += junkChance( j, water );
			if (junkRoll < edge) return Catch.of( j );
		}
		float[] w = weights( water, bait, phase, season, sky );
		float sum = 0f;
		for (float x : w) sum += x;
		if (sum <= 0f) return null;
		float at = fishRoll * sum;
		FishSpecies[] all = FishSpecies.values();
		for (int i = 0; i < all.length; i++){
			if (w[i] <= 0f) continue;
			at -= w[i];
			if (at < 0f) return Catch.of( all[i] );
		}
		//the roll's rounding at the very top: the last that lives here
		for (int i = all.length - 1; i >= 0; i--) if (w[i] > 0f) return Catch.of( all[i] );
		return null;
	}

	/** What comes up, on the game's own dice. */
	public static Catch roll( FishingWaters.Kind water, Bait bait, DayNightCycle.Phase phase,
			GameCalendar.Season season, VillageRoutine.Weather sky ){
		return pick( water, bait, phase, season, sky, Random.Float(), Random.Float() );
	}
}
