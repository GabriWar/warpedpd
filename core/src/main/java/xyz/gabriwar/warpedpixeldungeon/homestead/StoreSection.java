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

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundlable;

/**
 * A part of a world slice's homestead data that a feature keeps for itself: stumps growing back,
 * crops, pens, a raid's ledger. One instance per slice, made by HomesteadStore from the fixed list
 * HomesteadStore.SECTIONS (looked up by class name, a missing one skipped), saved with the slice
 * under its class name. A section is a public class with a public no-argument constructor.
 *
 * Keep everything in WORLD coordinates (OverworldLevel.worldKey, SectorMap for an index): the
 * window moves under it, and a world-keyed section needs nothing translated. Every hook runs on
 * the host only, never on a co-op guest's mirror. Things that live as actors (a station, a crop, a
 * penned animal) are mobs implementing HomesteadActor: the level parks them and puts them back
 * with the window, so a section never spawns one that is merely parked.
 */
public abstract class StoreSection implements Bundlable {

	/** The window was just laid at a new origin, or the slice was made or loaded: lay this
	 *  section's things on the window's cells (OverworldLevel.localCell finds them). */
	public void materialize( OverworldLevel level ){}

	/** The window is about to move off its cells (a rebase, a jump, the season turning): take
	 *  back what lives only on the window. */
	public void dematerialize( OverworldLevel level ){}

	/** A cell changed through the Homestead facade: a piece went up, came down or was wrecked. */
	public void cellChanged( OverworldLevel level, int cell ){}

	/** Once a world turn while the hero is on this slice (OverworldLevel's turn, as a hero's turn
	 *  begins). */
	public void worldTurn( OverworldLevel level ){}
}
