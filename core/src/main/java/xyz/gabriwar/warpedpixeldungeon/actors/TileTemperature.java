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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import com.watabou.utils.PathFinder;

import java.util.Arrays;

/**
 * Per-tile temperature system with persistent heat that decays and diffuses.
 * <p>
 * Blobs deposit heat/cold into Level.tileHeat[] each turn via depositHeat().
 * stepDiffusion() decays heat toward zero and spreads it to neighbors.
 * tileTemp() reads from tileHeat[] for the thermal contribution.
 */
public final class TileTemperature {

    private static final float DECAY_AIR   = 0.85f;  // half-life ~4.3 turns
    private static final float DECAY_WATER = 0.92f;  // half-life ~8.5 turns
    private static final float DIFFUSE_RATE = 0.08f;  // 8% to each non-solid neighbor

    // Water baseline temp (°C). Real groundwater/lakes sit around 12-15°C.
    private static final float WATER_BASELINE  = 13f;
    private static final float WATER_INERTIA   = 0.7f;
    private static final float EMBERS_BONUS    = 25f;
    private static final float ICE_PENALTY     = 8f;
    private static final float WOOL_RUG_BONUS  = 2f;
    // Sitting beside a campfire (embers or open fire within one cell) counts as warm:
    // a wild camp's fire pit keeps a sleeping hero out of hypothermia.
    public static final float CAMPFIRE_WARMTH  = 10f;
    // A torch burning in your hand is not a campfire, but it is a flame you
    // carry: it takes the edge off the cold wherever you walk.
    public static final float TORCH_WARMTH     = 6f;

    // Reusable diffusion buffer — avoids GC pressure from per-turn allocation.
    // Lazily sized to match level.length().
    private static float[] diffBuf = null;
    private static int diffBufLen = 0;

    // Dirty flag: set true by depositHeat, cleared by stepDiffusion.
    // When false and no residual heat exists, stepDiffusion is a no-op.
    private static boolean dirty = false;

    /**
     * Deposits heat (positive) or cold (negative) into a cell's tileHeat.
     */
    public static void depositHeat(int cell, float amount) {
        if (Dungeon.level == null || Dungeon.level.tileHeat == null) return;
        if (cell < 0 || cell >= Dungeon.level.tileHeat.length) return;
        Dungeon.level.tileHeat[cell] += amount;
        dirty = true;
    }

    /**
     * In-place decay + neighbor diffusion. Called once per hero turn.
     * Only processes cells with non-zero heat and their immediate neighbors.
     */
    public static void stepDiffusion(Level level) {
        if (level == null || level.tileHeat == null) return;

        int len = level.length();
        int w = level.width();
        float[] heat = level.tileHeat;

        // Fast exit: if nothing was deposited since last step,
        // scan is still needed for residual heat decay, but only if dirty or
        // we know there's residual heat. We use the dirty flag plus a quick check.
        if (!dirty) {
            // No deposits this turn. Check if any residual heat needs processing.
            // This scan is O(len) but exits early on first non-zero cell.
            boolean anyHeat = false;
            for (int i = 0; i < len; i++) {
                if (heat[i] != 0) { anyHeat = true; break; }
            }
            if (!anyHeat) return;
        }
        dirty = false;

        // Ensure diffusion buffer is correctly sized
        if (diffBuf == null || diffBufLen != len) {
            diffBuf = new float[len];
            diffBufLen = len;
        } else {
            Arrays.fill(diffBuf, 0f);
        }

        // Single pass: decay each non-zero cell and diffuse to neighbors
        int h = level.height();
        for (int i = 0; i < len; i++) {
            if (heat[i] == 0) continue;

            // Decay
            float val = heat[i] * (level.water[i] ? DECAY_WATER : DECAY_AIR);

            // Clamp near-zero
            if (val > -0.1f && val < 0.1f) {
                // Decayed to nothing, don't diffuse
                continue;
            }

            // Diffuse: send 8% to each non-solid neighbor
            int x = i % w;
            int y = i / w;
            float sent = 0;
            for (int offset : PathFinder.NEIGHBOURS8) {
                int n = i + offset;
                int nx = n % w;
                // Bounds check: skip if wrapping around edges or out of map
                if (n < 0 || n >= len) continue;
                if (Math.abs(nx - x) > 1) continue; // prevent row wrapping
                if (!level.solid[n]) {
                    float share = val * DIFFUSE_RATE;
                    diffBuf[n] += share;
                    sent += share;
                }
            }

            // Keep remainder
            diffBuf[i] += val - sent;
        }

        // Swap results back into tileHeat
        System.arraycopy(diffBuf, 0, heat, 0, len);
    }

    /**
     * Returns the temperature in °C at the given cell, incorporating:
     * - ClimateManager ambient (depth-adjusted, seasonal, diurnal)
     * - Water/embers/wool-rug terrain modifiers
     * - Persistent tileHeat (deposited by blobs, decayed by diffusion)
     * - Plant contributions
     */
    public static float tileTemp(int cell) {
        if (Dungeon.level == null) return ClimateManager.localTemp();

        float t = ClimateManager.localTemp();

        int terrain = Dungeon.level.map[cell];

        if (Dungeon.level.water[cell]) {
            float waterTemp = WATER_BASELINE + (t - WATER_BASELINE) * WATER_INERTIA;
            t = waterTemp;
        }

        if (terrain == Terrain.EMBERS) t += EMBERS_BONUS;
        if (terrain == Terrain.FROZEN_WATER) t -= ICE_PENALTY;
        if (terrain == Terrain.WOOL_RUG) t += WOOL_RUG_BONUS;

        // Plant contributions
        Plant plant = Dungeon.level.plants.get(cell);
        if (plant != null) t += plant.temperatureBonus();
        for (int offset : PathFinder.NEIGHBOURS8) {
            int n = cell + offset;
            if (n >= 0 && n < Dungeon.level.map.length) {
                Plant adj = Dungeon.level.plants.get(n);
                if (adj != null) t += adj.temperatureBonus() * 0.8f;
            }
        }

        // Persistent tile heat from blobs
        if (Dungeon.level.tileHeat != null && cell < Dungeon.level.tileHeat.length) {
            t += Dungeon.level.tileHeat[cell];
        }

        return t;
    }

    /**
     * True when the cell or one of its 8 neighbours is a bed of embers or holds
     * open fire. Used for campfire warmth and for drying soaked shoes.
     */
    public static boolean nearFire(int cell) {
        if (Dungeon.level == null) return false;
        int[] map = Dungeon.level.map;
        int w = Dungeon.level.width();
        Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
        boolean hasFire = fire != null && fire.volume > 0 && fire.cur != null;
        int x = cell % w;
        for (int offset : PathFinder.NEIGHBOURS9) {
            int n = cell + offset;
            if (n < 0 || n >= map.length) continue;
            if (Math.abs(n % w - x) > 1) continue; // prevent row wrapping
            if (map[n] == Terrain.EMBERS) return true;
            if (hasFire && fire.cur[n] > 0) return true;
        }
        return false;
    }

    /**
     * Like tileTemp() but also applies wind chill, campfire warmth, the given
     * character's Drenched state (wetness makes cold feel worse) and, for the hero
     * on their own cell, the warmth of the armor they wear.
     */
    public static float feelsLikeAt(int cell, Char ch) {
        float t = tileTemp(cell);
        float wind = ClimateManager.localWindSpeed();
        if (t < 10f && wind > 3f) {
            t -= wind * 0.5f;
        }
        // the embers cell itself already carries EMBERS_BONUS via tileTemp()
        if (Dungeon.level != null && Dungeon.level.map[cell] != Terrain.EMBERS && nearFire(cell)) {
            t += CAMPFIRE_WARMTH;
        }
        if (ch != null && ch.buff(Drenched.class) != null) {
            t -= 5f;
        }
        if (ch != null && ch.buff(SoakedShoes.class) != null) {
            t += SoakedShoes.COLD_PENALTY;
        }
        if (ch instanceof Hero && cell == ch.pos && ((Hero) ch).belongings.armor != null) {
            t += ((Hero) ch).belongings.armor.thermalOffset();
        }
        // the flame you carry warms the cell you stand in - a magical glow does not
        if (ch != null && cell == ch.pos) {
            Light light = ch.buff(Light.class);
            if (light != null && light.flame) t += TORCH_WARMTH;
        }
        return t;
    }

    /** Convenience overload that checks the hero's Drenched state. */
    public static float feelsLikeAt(int cell) {
        return feelsLikeAt(cell, Dungeon.hero);
    }
}
