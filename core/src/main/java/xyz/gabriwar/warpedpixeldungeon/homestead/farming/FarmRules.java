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

package xyz.gabriwar.warpedpixeldungeon.homestead.farming;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.FusedTomato;
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.food.Grape;
import xyz.gabriwar.warpedpixeldungeon.items.food.PlantEgg;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The numbers and the rules of the fields, pure: how fast a crop grows in each season, under
 * glass and by water, how long a cycle is and how likely it is to wither, when the frost takes it
 * while the hero is away, what the sickle reaps, and the crop's own dice. Farming applies them to
 * the world; the tests ask them directly.
 */
public final class FarmRules {

	private FarmRules(){}

	/** Growth a turn by season, against a summer turn's 1. Winter grows nothing in the open. */
	public static final float SPRING = 1.25f, SUMMER = 1f, AUTUMN = 0.75f, WINTER = 0f;
	/** A crop in the open dies when its cell's temperature falls below this, C. */
	public static final float FROST_C = -2f;
	/** A crop that has not been brought up to date for longer than this (turns of the clock) was
	 *  left alone: the hero was away, and the away rules settle the gap (frost by the days gone). */
	public static final int AWAY_TURNS = 20;
	/** Water: each water tile next to a crop counts, up to MAX_WATER; a well within WELL_REACH
	 *  cells counts as WELL_WATER of them. */
	public static final int MAX_WATER = 4, WELL_REACH = 2, WELL_WATER = 2;
	/** Each water tile shortens a water lover's cycle by this share and its wither chance by
	 *  WITHER_STEP points; any other crop gets half of both. */
	public static final float WATER_STEP = 0.10f;
	public static final int WITHER_STEP = 10, MIN_WITHER = 5;
	/** A dose of plant food gives this share of the growth left in the cycle. */
	public static final float FOOD_SHARE = 0.25f;

	/** Growth a turn in a season; a greenhouse grows at summer's pace all year. */
	public static float seasonRate( GameCalendar.Season season, boolean greenhouse ){
		if (greenhouse) return SUMMER;
		switch (season){
			case SPRING: return SPRING;
			case SUMMER: return SUMMER;
			case AUTUMN: return AUTUMN;
			default:     return WINTER;
		}
	}

	/** Does the plant love water (FarmCrop.WATER_LOVERS): it takes the whole water bonus. */
	public static boolean lover( Class<? extends Plant> plant ){
		return FarmCrop.lovesWater( plant );
	}

	/** The turns of growth a cycle of a plant takes with this much water (0..MAX_WATER). */
	public static int cycleTurns( Class<? extends Plant> plant, int water ){
		float step = lover( plant ) ? WATER_STEP : WATER_STEP / 2f;
		int w = Math.max( 0, Math.min( MAX_WATER, water ) );
		return Math.max( 1, Math.round( FarmCrop.profileFor( plant )[0] * (1f - step * w) ) );
	}

	/** Its chance in a hundred to wither at the end of a cycle after the first, with this much water. */
	public static int witherChance( Class<? extends Plant> plant, int water ){
		int step = lover( plant ) ? WITHER_STEP : WITHER_STEP / 2;
		int w = Math.max( 0, Math.min( MAX_WATER, water ) );
		return Math.max( MIN_WITHER, FarmCrop.profileFor( plant )[2] - step * w );
	}

	/** The most seeds it drops of its own before it is spent. */
	public static int maxSeeds( Class<? extends Plant> plant ){
		return FarmCrop.profileFor( plant )[1];
	}

	/**
	 * The water at a cell of a map: the water tiles (shallow or deep) among its eight neighbours,
	 * up to MAX_WATER, and at least WELL_WATER with a well within WELL_REACH cells. Ice is no water.
	 */
	public static int water( int[] map, int width, int cell ){
		int n = 0;
		for (int d : PathFinder.NEIGHBOURS8){
			int c = cell + d;
			if (c < 0 || c >= map.length || Math.abs( c % width - cell % width ) > 1) continue;
			if (map[c] == Terrain.WATER || map[c] == Terrain.DEEP_WATER) n++;
		}
		n = Math.min( MAX_WATER, n );
		if (n >= WELL_WATER) return n;
		int x = cell % width, y = cell / width, h = map.length / width;
		for (int dy = -WELL_REACH; dy <= WELL_REACH; dy++){
			for (int dx = -WELL_REACH; dx <= WELL_REACH; dx++){
				int nx = x + dx, ny = y + dy;
				if (nx < 0 || ny < 0 || nx >= width || ny >= h) continue;
				if (map[nx + ny * width] == Terrain.WELL) return WELL_WATER;
			}
		}
		return n;
	}

	/** Was any of the days after `fromDay`, up to and including `toDay`, a winter day? A year and
	 *  more always holds one. */
	public static boolean winterBetween( int fromDay, int toDay ){
		if (toDay - fromDay >= 364) return true;
		for (int d = fromDay + 1; d <= toDay; d++){
			if (GameCalendar.seasonOfDay( d ) == GameCalendar.Season.WINTER) return true;
		}
		return false;
	}

	/**
	 * The growth `dt` turns of the clock give, the last of them on `toDay`. Tended as it goes
	 * (not away) it is today's season's pace; across an absence the turns are spread evenly over
	 * the days from `fromDay` to `toDay` and each day grows at its own season's pace.
	 */
	public static float growth( float dt, int fromDay, int toDay, boolean away, boolean greenhouse ){
		if (greenhouse) return dt * SUMMER;
		if (!away || toDay <= fromDay) return dt * seasonRate( GameCalendar.seasonOfDay( toDay ), false );
		//past a year away the open field has had a winter (winterBetween): nothing outdoors lives
		//to grow, so the days are only walked for absences shorter than that
		int days = Math.min( toDay - fromDay + 1, 366 );
		float share = dt / days, out = 0f;
		for (int i = 0; i < days; i++){
			out += share * seasonRate( GameCalendar.seasonOfDay( toDay - i ), false );
		}
		return out;
	}

	/** The crop's own die, 0..99, for the end of a cycle: the same for the same crop and cycle,
	 *  so a reload or the settling of an absence comes out the same. */
	public static int roll( long seed, long key, float planted, int cycle ){
		return (int) Math.floorMod( hash( seed, key, Float.floatToIntBits( planted ), cycle ), 100L );
	}

	/** One of `n` (0..n-1) for a home's night: which of its open crops the pests take that night
	 *  while the hero is away (FarmSettler). The same for the same world, home and day. */
	public static int pick( long seed, long homeKey, int day, int n ){
		return (int) Math.floorMod( hash( seed, homeKey, day, -1 ), (long) Math.max( 1, n ) );
	}

	//a well-stirred mix of four numbers
	private static long hash( long a, long b, long c, long d ){
		long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L ^ d * 0x27D4EB2F165667C5L;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	/** Does the plant's crop give food to the sickle (the rest give their own seed)? */
	public static boolean food( Class<? extends Plant> plant ){
		return plant == BlandfruitBush.class || plant == Cornwheat.class || plant == Apricobush.class
				|| plant == Gobgrape.class || plant == Eggbloom.class || plant == Tomatobush.class;
	}

	/**
	 * What the sickle reaps from a ripe crop: Blandfruit 1-2 from a blandfruit bush, 2-3 cornwheat
	 * shafts, an aprico, two grapes, a plant egg, 1-2 fused tomatoes from a tomato bush; any other
	 * plant its own seed.
	 */
	public static ArrayList<Item> harvest( Class<? extends Plant> plant, Class<? extends Plant.Seed> seed ){
		ArrayList<Item> out = new ArrayList<>();
		if (plant == BlandfruitBush.class)     out.add( new Blandfruit().quantity( Random.IntRange( 1, 2 ) ) );
		else if (plant == Cornwheat.class)     out.add( new Cornwheatshaft().quantity( Random.IntRange( 2, 3 ) ) );
		else if (plant == Apricobush.class)    out.add( new Aprico() );
		else if (plant == Gobgrape.class)      out.add( new Grape().quantity( 2 ) );
		else if (plant == Eggbloom.class)      out.add( new PlantEgg() );
		else if (plant == Tomatobush.class)    out.add( new FusedTomato().quantity( Random.IntRange( 1, 2 ) ) );
		else if (seed != null){
			Item s = Reflection.newInstance( seed );
			if (s != null) out.add( s );
		}
		return out;
	}
}
