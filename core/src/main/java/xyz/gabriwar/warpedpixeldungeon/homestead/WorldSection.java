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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import com.watabou.utils.Bundlable;

/**
 * A part of the homesteads' run-wide data that a feature keeps for itself, across every slice:
 * the attuned waystones, the raids' ledgers. One instance per run, made by HomesteadWorld from
 * the fixed list HomesteadWorld.SECTIONS (looked up by class name, a missing one skipped), saved
 * with the game under its class name, made afresh for a new run. A public class with a public
 * no-argument constructor; host-owned state, as everything of the homesteads is.
 */
public abstract class WorldSection implements Bundlable {
}
