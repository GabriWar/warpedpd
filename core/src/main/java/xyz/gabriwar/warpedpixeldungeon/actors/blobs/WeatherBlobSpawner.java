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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * Spawns tile-level weather blobs driven by {@link ClimateManager} state.
 * Instead of pure random chance, blob types are selected based on the
 * actual simulated climate: temperature, precipitation, wind, humidity,
 * moon light, and weather state.
 */
public class WeatherBlobSpawner {

	// Spawn attempt each turn, gated by cooldown + climate conditions
	private static final float BASE_CHANCE = 0.06f;

	private static int cooldown = 0;
	private static final int MIN_COOLDOWN = 40;

	// How far a patch reaches from its seed cell: enough to fill a small room
	private static final int PATCH_RADIUS = 3;

	public static void onHeroTurn() {
		if (Dungeon.level == null) return;

		if (cooldown > 0) {
			cooldown--;
			return;
		}

		if (Random.Float() > BASE_CHANCE) return;

		Class<? extends Blob> blobType = pickWeatherBlob();
		if (blobType == null) return;

		int cell = findSpawnCell();
		if (cell == -1) return;

		int amount = blobAmount(blobType);

		// The patch: the open cells within a few steps of the seed, as many as the
		// weather calls for, so a cloud fills a small room rather than a corner of it
		int wanted = patchCells(blobType);
		java.util.ArrayList<Integer> patch = new java.util.ArrayList<>();
		boolean[] open = new boolean[Dungeon.level.length()];
		for (int i = 0; i < open.length; i++) {
			open[i] = Dungeon.level.passable[i] && !Dungeon.level.solid[i] && !Dungeon.level.pit[i];
		}
		PathFinder.buildDistanceMap(cell, open, PATCH_RADIUS);
		for (int i = 0; i < open.length; i++) {
			if (PathFinder.distance[i] <= PATCH_RADIUS) patch.add(i);
		}
		java.util.Collections.shuffle(patch);
		//the seed cell always, the rest nearest first so the patch stays in one piece
		java.util.Collections.sort(patch, (a, b) -> PathFinder.distance[a] - PathFinder.distance[b]);
		int placed = 0;
		for (int c : patch) {
			if (placed >= wanted) break;
			//thinner at the edge, thick in the middle
			int here = PathFinder.distance[c] >= PATCH_RADIUS ? Math.max(1, amount / 2) : amount;
			GameScene.add(Blob.seed(c, here, blobType));
			placed++;
		}

		// Cooldown scales inversely with weather intensity — storms spawn blobs faster
		float intensity = Math.max(ClimateManager.localPrecipRate(),
				ClimateManager.localWindSpeed() / 20f);
		int cooldownRange = Math.round(MIN_COOLDOWN * (1f - intensity * 0.5f));
		cooldown = cooldownRange + Random.IntRange(0, 30);
	}

	/** how many cells a patch of this weather covers */
	private static int patchCells(Class<? extends Blob> type) {
		float rate = ClimateManager.localPrecipRate();
		if (type == HeatHaze.class)     return Random.IntRange(12, 20);
		if (type == FallenLeaves.class)  return Random.IntRange(12, 20);
		if (type == Moonbeam.class)      return Random.IntRange(9, 15);
		if (type == MistCloud.class)     return Random.IntRange(18, 28);
		if (type == Blizzard.class)      return Math.round(Random.IntRange(14, 22) * (0.8f + 0.4f * rate));
		if (type == StormCloud.class)    return Math.round(Random.IntRange(16, 24) * (0.8f + 0.4f * rate));
		return 12;
	}

	/**
	 * Select a blob type based on current ClimateManager state.
	 * Priority: precipitation → special conditions → seasonal ambient.
	 */
	private static Class<? extends Blob> pickWeatherBlob() {
		DayNightCycle.Phase phase = DayNightCycle.phase();

		// --- Moonbeam: night + moon visible + not heavy clouds ---
		if (DayNightCycle.isNight()
				&& ClimateManager.moonLight() > 0.03f
				&& ClimateManager.cloudCover() < 0.7f) {
			// Chance proportional to moonlight strength
			if (Random.Float() < ClimateManager.moonLight() * 2f) {
				return Moonbeam.class;
			}
		}

		// --- Precipitation-driven blobs ---
		PrecipType precip = ClimateManager.localPrecipType();
		float precipRate = ClimateManager.localPrecipRate();

		if (precipRate > 0.1f) {
			switch (precip) {
				case BLIZZARD:
				case SNOW:
					if (ClimateManager.localTemp() < 2f) {
						return Blizzard.class;
					}
					break;
				case RAIN:
				case SLEET:
					// Storm clouds when raining; heavier rain = more likely
					if (Random.Float() < precipRate) {
						return StormCloud.class;
					}
					break;
				case HAIL:
					// Hail can freeze tiles too
					if (Random.Float() < 0.4f) return Blizzard.class;
					if (Random.Float() < 0.6f) return StormCloud.class;
					break;
			}
		}

		// --- Fog → MistCloud ---
		if (ClimateManager.isFoggy()) {
			return MistCloud.class;
		}

		// --- Mist at dawn/dusk when humidity is high ---
		if ((phase == DayNightCycle.Phase.DAWN || phase == DayNightCycle.Phase.DUSK)
				&& ClimateManager.localHumidity() > 0.55f
				&& ClimateManager.cloudCover() > 0.2f) {
			if (Random.Float() < ClimateManager.localHumidity()) {
				return MistCloud.class;
			}
		}

		// --- Heat haze: hot + clear/fair ---
		if (ClimateManager.localTemp() > 28f
				&& ClimateManager.isClear()
				&& precipRate < 0.05f) {
			// Stronger at higher temps
			float heatChance = (ClimateManager.localTemp() - 28f) / 15f; // 0-1 over 28-43°C
			if (Random.Float() < heatChance) {
				return HeatHaze.class;
			}
		}

		// --- Fallen leaves: autumn + wind ---
		if (GameCalendar.season() == GameCalendar.Season.AUTUMN
				&& ClimateManager.localWindSpeed() > 3f
				&& precipRate < 0.3f) {
			return FallenLeaves.class;
		}

		return null;
	}

	private static int blobAmount(Class<? extends Blob> type) {
		// Scale amounts by weather intensity for precipitation blobs
		float rate = ClimateManager.localPrecipRate();
		float mult = 0.7f + rate * 0.6f; // 0.7x at light, 1.3x at heavy

		//per cell; the spreading kinds lose one a turn per cell as they even out, the
		//fixed kinds one a turn where they lie, so these are roughly turns of life.
		//Doubled once more: weather that arrives is worth walking around in.
		if (type == HeatHaze.class)     return Random.IntRange(48, 80);
		if (type == FallenLeaves.class)  return Random.IntRange(80, 140);
		if (type == Moonbeam.class)      return Math.round(Random.IntRange(60, 100) * (0.5f + ClimateManager.moonLight() * 3f));
		if (type == MistCloud.class)     return Math.round(Random.IntRange(60, 100) * mult);
		if (type == Blizzard.class)      return Math.round(Random.IntRange(48, 80) * mult);
		if (type == StormCloud.class)    return Math.round(Random.IntRange(56, 90) * mult);
		return 60;
	}

	private static int findSpawnCell() {
		for (int tries = 0; tries < 20; tries++) {
			int cell = Random.Int(Dungeon.level.length());
			if (Dungeon.level.passable[cell]
					&& !Dungeon.level.solid[cell]
					&& !Dungeon.level.pit[cell]
					&& Dungeon.level.distance(cell, Dungeon.hero.pos) >= 3
					&& Dungeon.level.distance(cell, Dungeon.hero.pos) <= 12) {
				return cell;
			}
		}
		return -1;
	}

	public static void reset() {
		cooldown = 0;
	}
}
