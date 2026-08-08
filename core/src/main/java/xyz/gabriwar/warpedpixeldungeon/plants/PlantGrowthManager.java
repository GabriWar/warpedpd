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

package xyz.gabriwar.warpedpixeldungeon.plants;

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * Drives passive (ambient) plant growth on levels.
 *
 * Called each hero turn from DayNightCycle and on level entry from Dungeon.switchLevel.
 * Follows the same static-utility pattern as WeatherBlobSpawner.
 */
public class PlantGrowthManager {

	// How many hero turns between plant growth ticks at 1.0× rate (SUMMER baseline)
	private static final int BASE_GROWTH_INTERVAL = 250;

	// How many hero turns between grass regrowth ticks (season-independent base)
	private static final int GRASS_REGROWTH_INTERVAL = 8;

	// Hard cap on total plants (player-planted + natural)
	private static final int TOTAL_CAP = 50;

	// Maximum catch-up cycles when entering a level that has been unvisited
	private static final int MAX_CATCHUP_CYCLES = 8;

	// How many hero turns between freeze/thaw checks
	private static final int FREEZE_THAW_INTERVAL = 5;
	// Temperature thresholds for water ↔ ice transitions
	private static final float FREEZE_TEMP = 0f;   // water freezes below this
	private static final float THAW_TEMP   = 5f;   // ice melts above this (hysteresis prevents flickering)

	// Valid terrain types for natural plant placement
	private static final int[] VALID_TERRAIN = {
			Terrain.GRASS,
			Terrain.HIGH_GRASS,
			Terrain.FURROWED_GRASS,
			Terrain.EMPTY,
			Terrain.EMBERS,
			Terrain.EMPTY_DECO
	};

	// -------------------------------------------------------------------------

	/** Called every hero turn from DayNightCycle.onHeroTurn(). */
	public static void onHeroTurn(Level level) {
		if (level == null) return;

		// Freeze/thaw runs regardless of herbalism challenge
		processFreezeThaw(level);

		if (Dungeon.isChallenged(Challenges.NO_HERBALISM)) return;

		GameCalendar.Season season = GameCalendar.season();

		// Plant growth
		int plantInterval = Math.round(BASE_GROWTH_INTERVAL * intervalMultiplier(season));
		if (Dungeon.cycleTurn - level.lastGrowthTurn >= plantInterval) {
			processGrowth(level, 1);
			level.lastGrowthTurn = Dungeon.cycleTurn;
		}

		// Grass regrowth (faster, season gated)
		int grassTiles = grassTilesPerCycle(season);
		if (grassTiles > 0 && Dungeon.cycleTurn - level.lastGrassRegrowthTurn >= GRASS_REGROWTH_INTERVAL) {
			processGrassRegrowth(level, grassTiles, true);
			level.lastGrassRegrowthTurn = Dungeon.cycleTurn;
		}
	}

	/** Called when the player enters a level, catching up missed growth ticks. */
	public static void catchUpGrowth(Level level) {
		if (level == null) return;
		if (Dungeon.isChallenged(Challenges.NO_HERBALISM)) return;

		GameCalendar.Season season = GameCalendar.season();

		// Plant catch-up
		if (level.lastGrowthTurn < 0) {
			level.lastGrowthTurn = Dungeon.cycleTurn;
		} else {
			int plantInterval = Math.round(BASE_GROWTH_INTERVAL * intervalMultiplier(season));
			if (plantInterval <= 0) plantInterval = 1;
			int elapsed = Dungeon.cycleTurn - level.lastGrowthTurn;
			int cycles = Math.min(elapsed / plantInterval, MAX_CATCHUP_CYCLES);
			if (cycles > 0) {
				processGrowth(level, cycles);
				level.lastGrowthTurn = Dungeon.cycleTurn;
			}
		}

		// Grass catch-up
		if (level.lastGrassRegrowthTurn < 0) {
			level.lastGrassRegrowthTurn = Dungeon.cycleTurn;
		} else {
			int grassTiles = grassTilesPerCycle(season);
			if (grassTiles > 0) {
				int elapsed = Dungeon.cycleTurn - level.lastGrassRegrowthTurn;
				int cycles = Math.min(elapsed / GRASS_REGROWTH_INTERVAL, MAX_CATCHUP_CYCLES * 10);
				if (cycles > 0) {
					processGrassRegrowth(level, cycles * grassTiles, false);
					level.lastGrassRegrowthTurn = Dungeon.cycleTurn;
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	private static void processGrowth(Level level, int cycles) {
		GameCalendar.Season season = GameCalendar.season();
		int naturalCap = naturalCap(season);

		for (int c = 0; c < cycles; c++) {
			// Winter kill: 10% chance per cycle to remove a warm/non-cold natural plant
			if (season == GameCalendar.Season.WINTER) {
				winterKill(level);
			}

			// Hard cap check
			if (level.plants.size >= TOTAL_CAP) continue;

			// Count natural plants
			int naturalCount = countNaturalPlants(level);

			// Evict oldest natural plant if at natural cap
			if (naturalCount >= naturalCap) {
				evictOldestNatural(level);
				naturalCount--;
			}

			// Re-check hard cap after possible eviction
			if (level.plants.size >= TOTAL_CAP) continue;

			// Find a valid position
			int pos = randomValidPlantPos(level);
			if (pos == -1) continue;

			// Select and create the plant
			Class<? extends Plant.Seed> seedClass = selectPlantClass(level, season);
			if (seedClass == null) continue;

			@SuppressWarnings("unchecked")
			Class<? extends Plant> plantClass =
					(Class<? extends Plant>) seedClass.getEnclosingClass();
			if (plantClass == null) continue;

			Plant plant = Reflection.newInstance(plantClass);
			if (plant == null) continue;

			level.plantNatural(plant, pos);
		}
	}

	// -------------------------------------------------------------------------

	/** Possibly kill one warm natural plant (10% chance). */
	private static void winterKill(Level level) {
		if (Random.Float() > 0.10f) return;
		if (level.naturalPlantOrder.isEmpty()) return;

		// Prefer warm plants; fall back to any natural plant
		ArrayList<Integer> warmNatural = new ArrayList<>();
		for (int pos : level.naturalPlantOrder) {
			Plant p = level.plants.get(pos);
			if (p != null && PlantWeights.WARM_PLANTS.contains(p.seedClass())) {
				warmNatural.add(pos);
			}
		}

		int victim;
		if (!warmNatural.isEmpty()) {
			victim = warmNatural.get(Random.Int(warmNatural.size()));
		} else {
			victim = level.naturalPlantOrder.get(Random.Int(level.naturalPlantOrder.size()));
		}
		level.uproot(victim);
	}

	private static void evictOldestNatural(Level level) {
		if (level.naturalPlantOrder.isEmpty()) return;
		int pos = level.naturalPlantOrder.get(0);
		level.uproot(pos);
	}

	private static int countNaturalPlants(Level level) {
		return level.naturalPlantOrder.size();
	}

	/**
	 * Finds a random valid position for a plant on the level.
	 * Up to 30 attempts: must be passable, valid terrain, no existing plant, no character.
	 */
	private static int randomValidPlantPos(Level level) {
		for (int tries = 0; tries < 30; tries++) {
			int pos = Random.Int(level.length());
			if (!level.passable[pos]) continue;
			if (!isValidTerrain(level.map[pos])) continue;
			if (level.plants.get(pos) != null) continue;
			if (Actor.findChar(pos) != null) continue;
			return pos;
		}
		return -1;
	}

	private static boolean isValidTerrain(int terrain) {
		for (int t : VALID_TERRAIN) {
			if (terrain == t) return true;
		}
		return false;
	}

	/**
	 * Selects a plant seed class using biome weights × season modifiers.
	 */
	private static Class<? extends Plant.Seed> selectPlantClass(Level level, GameCalendar.Season season) {
		float[] weights = PlantWeights.levelWeights(level);
		// Apply season modifiers
		for (int i = 0; i < weights.length; i++) {
			weights[i] *= PlantWeights.seasonModifier(PlantWeights.ALL_SEEDS[i], season);
		}
		int idx = Random.chances(weights);
		if (idx < 0 || idx >= PlantWeights.ALL_SEEDS.length) return null;
		return PlantWeights.ALL_SEEDS[idx];
	}

	// -------------------------------------------------------------------------

	/** Returns the natural plant cap for the current season. */
	private static int naturalCap(GameCalendar.Season season) {
		switch (season) {
			case SPRING: return 20;
			case SUMMER: return 12;
			case WINTER: return 6;
			case AUTUMN:
			default:     return 10;
		}
	}

	/** Returns the interval multiplier for the current season (lower = faster growth). */
	private static float intervalMultiplier(GameCalendar.Season season) {
		switch (season) {
			case SPRING: return 0.5f;
			case SUMMER: return 1.0f;
			case AUTUMN: return 1.25f;
			case WINTER: return 2.0f;
			default:     return 1.0f;
		}
	}

	/** How many GRASS→HIGH_GRASS conversions to do per grass regrowth tick. */
	private static int grassTilesPerCycle(GameCalendar.Season season) {
		switch (season) {
			case SPRING: return 3;
			case SUMMER: return 2;
			case AUTUMN: return 1;
			case WINTER: return 0; // frozen ground, no regrowth
			default:     return 1;
		}
	}

	/**
	 * Converts EMBERS → GRASS and GRASS → HIGH_GRASS over time.
	 * Skips tiles in the player's FOV to avoid pop-in.
	 */
	private static void processGrassRegrowth(Level level, int tiles, boolean visuals) {
		int grown = 0;
		for (int tries = 0; tries < tiles * 10 && grown < tiles; tries++) {
			int pos = Random.Int(level.length());
			if (level.plants.get(pos) != null) continue;
			if (Actor.findChar(pos) != null) continue;

			if (level.map[pos] == Terrain.EMBERS) {
				// Embers cool down into grass first
				level.set(pos, Terrain.GRASS, level);
				GameScene.updateMap(pos);
				grown++;
			} else if (level.map[pos] == Terrain.GRASS) {
				// Grass grows back into high grass
				level.set(pos, Terrain.HIGH_GRASS, level);
				GameScene.updateMap(pos);
				if (visuals) CellEmitter.get(pos).burst(LeafParticle.GENERAL, 4);
				grown++;
			}
		}
	}

	// -------------------------------------------------------------------------
	// Water freeze / thaw
	// -------------------------------------------------------------------------

	private static int freezeThawCounter = 0;

	/**
	 * Checks a batch of water/ice tiles each cycle and freezes or thaws them
	 * based on per-tile temperature. Uses hysteresis (freeze < 0°C, thaw > 5°C)
	 * to prevent flickering at the boundary.
	 * Only affects levels where waterCanFreeze() is true (not lava levels).
	 */
	private static void processFreezeThaw(Level level) {
		if (!level.waterCanFreeze()) return;

		freezeThawCounter++;
		if (freezeThawCounter < FREEZE_THAW_INTERVAL) return;
		freezeThawCounter = 0;

		int len = level.length();
		// Check a random sample of tiles each cycle for performance
		int checks = Math.max(10, len / 50);

		for (int i = 0; i < checks; i++) {
			int pos = Random.Int(len);
			int terrain = level.map[pos];

			if (terrain == Terrain.WATER) {
				float temp = TileTemperature.tileTemp(pos);
				if (temp < FREEZE_TEMP) {
					// Water freezes into ice
					Level.set(pos, Terrain.FROZEN_WATER, level);
					level.water[pos] = false; // no longer liquid
					GameScene.updateMap(pos);
					CellEmitter.get(pos).burst(SnowParticle.RISING_FACTORY, 6);
				}

			} else if (terrain == Terrain.FROZEN_WATER) {
				float temp = TileTemperature.tileTemp(pos);
				if (temp > THAW_TEMP) {
					// Ice melts back into water
					Level.set(pos, Terrain.WATER, level);
					level.water[pos] = true;
					GameScene.updateMap(pos);
					CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 4);
				}
			}
		}
	}
}
