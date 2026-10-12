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

import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * One crop in the ground: what was sown, when, how far its current cycle has grown and what it
 * has given. On a world slice this record IS the crop - the slice's FarmSlice keeps it by world
 * cell and the FarmCrop standing on the cell is only how it is shown and touched; harvesting it,
 * its withering, frost and pests take the record away. Growth is counted in turns of the global
 * clock (FarmCrop.clock) at the rate the season, the water and the roof allow (Farming.tend), so
 * a crop the hero is away from catches up on everything when he is back.
 */
public class Crop implements Bundlable {

	/** Its world cell (OverworldLevel.worldKey's halves); unused off the world's slices. */
	public int wx, wy;
	/** What was sown. */
	public Class<? extends Plant.Seed> seed;
	/** FarmCrop.clock() when it was sown: the crop's own name for its dice (FarmRules.roll). */
	public float planted;
	/** Turns of growth into the current cycle, and how many of those plant food gave. */
	public float growth, fed;
	/** FarmCrop.clock() and WorldClock.day() when it was last brought up to date. */
	public float clock;
	public int day;
	/** Whole cycles grown: the first ripens it, each after that drops a seed or withers it. */
	public int cycles;
	/** Seeds it has dropped of its own. */
	public int seeds;

	public Crop(){
	}

	public Crop( Class<? extends Plant.Seed> seed, float now, int today ){
		this.seed = seed;
		planted = now;
		clock = now;
		day = today;
	}

	/** Has it grown a whole cycle: ready for the sickle. */
	public boolean ripe(){
		return cycles >= 1;
	}

	//the plant its seed grows, worked out once (the sprite asks every frame)
	private Class<? extends Plant.Seed> plantOf;
	private Class<? extends Plant> plant;

	/** The plant it grows, null when its seed is no longer in the game. */
	public Class<? extends Plant> plant(){
		if (seed == null) return null;
		if (plantOf != seed){
			plantOf = seed;
			try {
				plant = seed.getConstructor().newInstance().getPlantClass();
			} catch (ReflectiveOperationException | RuntimeException e){
				plant = null;
			}
		}
		return plant;
	}

	private static final String WX = "wx", WY = "wy", SEED = "seed", PLANTED = "planted", GROWTH = "growth",
			FED = "fed", CLOCK = "clock", DAY = "day", CYCLES = "cycles", SEEDS = "seeds";

	@Override
	public void storeInBundle( Bundle bundle ){
		bundle.put( WX, wx );
		bundle.put( WY, wy );
		if (seed != null) bundle.put( SEED, seed );
		bundle.put( PLANTED, planted );
		bundle.put( GROWTH, growth );
		bundle.put( FED, fed );
		bundle.put( CLOCK, clock );
		bundle.put( DAY, day );
		bundle.put( CYCLES, cycles );
		bundle.put( SEEDS, seeds );
	}

	@SuppressWarnings("unchecked")
	@Override
	public void restoreFromBundle( Bundle bundle ){
		wx = bundle.getInt( WX );
		wy = bundle.getInt( WY );
		Class<?> cls = bundle.contains( SEED ) ? bundle.getClass( SEED ) : null;
		seed = cls != null && Plant.Seed.class.isAssignableFrom( cls ) ? (Class<? extends Plant.Seed>) cls : null;
		planted = bundle.getFloat( PLANTED );
		growth = bundle.getFloat( GROWTH );
		fed = bundle.getFloat( FED );
		clock = bundle.getFloat( CLOCK );
		day = bundle.getInt( DAY );
		cycles = bundle.getInt( CYCLES );
		seeds = bundle.getInt( SEEDS );
	}
}
