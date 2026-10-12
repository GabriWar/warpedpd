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

package xyz.gabriwar.warpedpixeldungeon.homestead.animals;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundle;

/**
 * The animals' section of a world slice (HomesteadStore): nothing of its own to keep - a roped,
 * tied or penned animal carries its Leashed, and the level keeps it as one of the homestead's
 * (Leadable.homesteadKept) - but before the window moves every beast looks again whether it is
 * penned, so one shut in a moment ago is kept and not parked off as wildlife.
 */
public class AnimalSlice extends StoreSection {

	@Override
	public void dematerialize( OverworldLevel ow ){
		for (Mob m : ow.mobs.toArray( new Mob[0] )){
			if (m instanceof Leadable) Roping.lookAtPen( ow, m, true );
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ){
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
	}
}
