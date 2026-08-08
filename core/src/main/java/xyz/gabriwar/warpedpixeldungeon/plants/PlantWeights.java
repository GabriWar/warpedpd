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

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.CavesBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CavesLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CityBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CityLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.FieldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.HallsBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.HallsLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.MineLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.MinesBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.PrisonBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.PrisonLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SewerBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SewerLevel;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Static weight tables for the passive plant growth system.
 * Index-stable: do not reorder ALL_SEEDS between releases.
 */
public class PlantWeights {

	// Index-stable array of all plantable seed classes (alphabetical).
	// Do NOT reorder entries — indices are stored in save-compatible weight arrays.
	@SuppressWarnings("unchecked")
	public static final Class<? extends Plant.Seed>[] ALL_SEEDS = new Class[]{
			Apricobush.Seed.class,       // 0
			Ballcrop.Seed.class,         // 1
			Bananabean.Seed.class,       // 2
			Blackholeflower.Seed.class,  // 3
			BlandfruitBush.Seed.class,   // 4
			Blindweed.Seed.class,        // 5
			Blueeyedsusan.Seed.class,    // 6
			Butterlion.Seed.class,       // 7
			Chandaliertail.Seed.class,   // 8
			Chillisnapper.Seed.class,    // 9
			Clitbalm.Seed.class,         // 10
			Clockcypress.Seed.class,     // 11
			Cocostuft.Seed.class,        // 12
			Combflower.Seed.class,       // 13
			Cornwheat.Seed.class,        // 14
			Crimsoncrown.Seed.class,     // 15
			Crimsonpepper.Seed.class,    // 16
			Dewcatcher.Seed.class,       // 17
			Dirtdaisy.Seed.class,        // 18
			Dreamfoil.Seed.class,        // 19
			Earthroot.Seed.class,        // 20
			Eggbloom.Seed.class,         // 21
			Eyeeuonymus.Seed.class,      // 22
			Fadeleaf.Seed.class,         // 23
			Feelerfern.Seed.class,       // 24
			Firebloom.Seed.class,        // 25
			Firefoxglove.Seed.class,     // 26
			Flowertree.Seed.class,       // 27
			Flytrap.Seed.class,          // 28
			Frostcorn.Seed.class,        // 29
			Gobgrape.Seed.class,         // 30
			Goograss.Seed.class,         // 31
			Grasslilly.Seed.class,       // 32
			Grassvine.Seed.class,        // 33
			Hypnohemp.Seed.class,        // 34
			Icecap.Seed.class,           // 35
			Kiwivetch.Seed.class,        // 36
			Larvaleaf.Seed.class,        // 37
			Lavenderlantern.Seed.class,  // 38
			Lightninglily.Seed.class,    // 39
			Mageroyal.Seed.class,        // 40
			Musclemoss.Seed.class,       // 41
			Nightshadeonion.Seed.class,  // 42
			Parasiteshrub.Seed.class,    // 43
			Peanutpetal.Seed.class,      // 44
			Phaseshift.Seed.class,       // 45
			Poppoplar.Seed.class,        // 46
			Rose.Seed.class,             // 47
			Rotberry.Seed.class,         // 48
			Shadowbloom.Seed.class,      // 49
			Snowhedge.Seed.class,        // 50
			Sorrowmoss.Seed.class,       // 51
			Sourpitcher.Seed.class,      // 52
			Starflower.Seed.class,       // 53
			Steamweed.Seed.class,        // 54
			Stormvine.Seed.class,        // 55
			Sunbloom.Seed.class,         // 56
			Suncarnivore.Seed.class,     // 57
			Sungrass.Seed.class,         // 58
			Swiftthistle.Seed.class,     // 59
			Tankcabbage.Seed.class,      // 60
			Tomatobush.Seed.class,       // 61
			Venusflytrap.Seed.class,     // 62
			Waterweed.Seed.class,        // 63
			Willowcane.Seed.class,       // 64
			Witherfennel.Seed.class      // 65
	};

	// Reverse lookup: seed class → index in ALL_SEEDS
	private static final HashMap<Class<? extends Plant.Seed>, Integer> SEED_INDEX = new HashMap<>();

	static {
		for (int i = 0; i < ALL_SEEDS.length; i++) {
			SEED_INDEX.put(ALL_SEEDS[i], i);
		}
	}

	// ----- Seasonal category sets -----

	public static final HashSet<Class<? extends Plant.Seed>> COLD_PLANTS = new HashSet<>();
	public static final HashSet<Class<? extends Plant.Seed>> WARM_PLANTS = new HashSet<>();
	public static final HashSet<Class<? extends Plant.Seed>> AQUATIC_PLANTS = new HashSet<>();

	static {
		// Cold-affinity plants (thrive in winter)
		COLD_PLANTS.add(Icecap.Seed.class);
		COLD_PLANTS.add(Frostcorn.Seed.class);
		COLD_PLANTS.add(Snowhedge.Seed.class);

		// Warm-affinity plants (thrive in summer, wither in winter)
		WARM_PLANTS.add(Firebloom.Seed.class);
		WARM_PLANTS.add(Sungrass.Seed.class);
		WARM_PLANTS.add(Sunbloom.Seed.class);
		WARM_PLANTS.add(Suncarnivore.Seed.class);
		WARM_PLANTS.add(Crimsonpepper.Seed.class);
		WARM_PLANTS.add(Chillisnapper.Seed.class);
		WARM_PLANTS.add(Firefoxglove.Seed.class);

		// Aquatic plants (thrive in spring / wet conditions)
		AQUATIC_PLANTS.add(Waterweed.Seed.class);
		AQUATIC_PLANTS.add(Steamweed.Seed.class);
		AQUATIC_PLANTS.add(Dewcatcher.Seed.class);
	}

	// ----- Biome weight overrides -----
	// Each entry maps seed class → weight multiplier for that biome.
	// Seeds not present use the default weight (1.0 for most, 0.1 for Rotberry/BlandfruitBush).

	private static final HashMap<Class<? extends Level>, HashMap<Class<? extends Plant.Seed>, Float>> BIOME_OVERRIDES = new HashMap<>();

	static {
		// Sewer: wet, mossy
		HashMap<Class<? extends Plant.Seed>, Float> sewer = new HashMap<>();
		sewer.put(Waterweed.Seed.class,  4.0f);
		sewer.put(Dreamfoil.Seed.class,  2.5f);
		sewer.put(Sorrowmoss.Seed.class, 2.0f);
		BIOME_OVERRIDES.put(SewerLevel.class,     sewer);
		BIOME_OVERRIDES.put(SewerBossLevel.class, sewer);

		// Caves/Mines: cold, rocky
		HashMap<Class<? extends Plant.Seed>, Float> caves = new HashMap<>();
		caves.put(Icecap.Seed.class,    4.0f);
		caves.put(Frostcorn.Seed.class, 3.0f);
		caves.put(Earthroot.Seed.class, 2.5f);
		caves.put(Snowhedge.Seed.class, 2.0f);
		BIOME_OVERRIDES.put(CavesLevel.class,     caves);
		BIOME_OVERRIDES.put(CavesBossLevel.class, caves);
		BIOME_OVERRIDES.put(MineLevel.class,      caves);
		BIOME_OVERRIDES.put(MinesBossLevel.class, caves);

		// Halls: fiery, demonic
		HashMap<Class<? extends Plant.Seed>, Float> halls = new HashMap<>();
		halls.put(Firebloom.Seed.class, 4.0f);
		halls.put(Sungrass.Seed.class,  3.0f);
		halls.put(Fadeleaf.Seed.class,  2.0f);
		BIOME_OVERRIDES.put(HallsLevel.class,     halls);
		BIOME_OVERRIDES.put(HallsBossLevel.class, halls);

		// Prison: dark, poisonous
		HashMap<Class<? extends Plant.Seed>, Float> prison = new HashMap<>();
		prison.put(Blindweed.Seed.class,  3.0f);
		prison.put(Sorrowmoss.Seed.class, 2.5f);
		prison.put(Mageroyal.Seed.class,  2.0f);
		BIOME_OVERRIDES.put(PrisonLevel.class,     prison);
		BIOME_OVERRIDES.put(PrisonBossLevel.class, prison);

		// City: arcane, urban
		HashMap<Class<? extends Plant.Seed>, Float> city = new HashMap<>();
		city.put(Mageroyal.Seed.class,   4.0f);
		city.put(Stormvine.Seed.class,   2.5f);
		city.put(Swiftthistle.Seed.class, 2.0f);
		BIOME_OVERRIDES.put(CityLevel.class,     city);
		BIOME_OVERRIDES.put(CityBossLevel.class, city);

		// Field: open, sunny
		HashMap<Class<? extends Plant.Seed>, Float> field = new HashMap<>();
		field.put(Sungrass.Seed.class,   4.0f);
		field.put(Earthroot.Seed.class,  3.0f);
		field.put(Starflower.Seed.class, 2.5f);
		BIOME_OVERRIDES.put(FieldLevel.class, field);
	}

	/** Returns the default weight array (all 1.0, Rotberry/BlandfruitBush at 0.1). */
	public static float[] defaults() {
		float[] w = new float[ALL_SEEDS.length];
		for (int i = 0; i < w.length; i++) w[i] = 1.0f;
		// Rotberry and BlandfruitBush should be very rare naturally
		w[SEED_INDEX.get(Rotberry.Seed.class)]      = 0.1f;
		w[SEED_INDEX.get(BlandfruitBush.Seed.class)] = 0.1f;
		return w;
	}

	/**
	 * Builds the combined weight array for a given level, applying biome overrides
	 * to the default weights.
	 */
	public static float[] levelWeights(Level level) {
		float[] w = defaults();
		HashMap<Class<? extends Plant.Seed>, Float> overrides = BIOME_OVERRIDES.get(level.getClass());
		if (overrides != null) {
			for (HashMap.Entry<Class<? extends Plant.Seed>, Float> entry : overrides.entrySet()) {
				Integer idx = SEED_INDEX.get(entry.getKey());
				if (idx != null) w[idx] = entry.getValue();
			}
		}
		return w;
	}

	/**
	 * Season modifier for a given seed class.
	 * Returns a float multiplier applied on top of base/biome weights.
	 */
	public static float seasonModifier(Class<? extends Plant.Seed> seedClass, GameCalendar.Season season) {
		switch (season) {
			case WINTER:
				if (COLD_PLANTS.contains(seedClass)) return 3.0f;
				if (WARM_PLANTS.contains(seedClass)) return 0.2f;
				return 1.0f;
			case SPRING:
				if (AQUATIC_PLANTS.contains(seedClass)) return 2.5f;
				return 1.2f;
			case SUMMER:
				if (WARM_PLANTS.contains(seedClass)) return 1.5f;
				if (COLD_PLANTS.contains(seedClass)) return 0.5f;
				return 1.0f;
			case AUTUMN:
			default:
				return 1.0f;
		}
	}
}
