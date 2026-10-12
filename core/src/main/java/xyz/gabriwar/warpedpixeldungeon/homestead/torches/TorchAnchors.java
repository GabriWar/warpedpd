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

package xyz.gabriwar.warpedpixeldungeon.homestead.torches;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeAnchors;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import java.util.function.IntConsumer;

/** A standing torch anchors a home as a lit hearth does (HomeAnchors' provider): no wild animal
 *  is set down within HomeAnchors.RADIUS cells of it. */
public class TorchAnchors implements HomeAnchors.Provider {

	@Override
	public void anchors( OverworldLevel level, IntConsumer cells ){
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (m instanceof StandingTorch && m.pos >= 0) cells.accept( m.pos );
		}
	}
}
