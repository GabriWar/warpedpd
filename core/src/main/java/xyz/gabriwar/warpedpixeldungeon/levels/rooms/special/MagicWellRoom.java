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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.special;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfAwareness;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfHealth;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfTransmutation;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WellWater;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

public class MagicWellRoom extends SpecialRoom {

	private static final Class<?>[] WATERS =
		{WaterOfAwareness.class, WaterOfHealth.class};
	
	public Class<?extends WellWater> overrideWater = null;
	
	public void paint( Level level ) {

		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );
		
		Point c = center();
		Painter.set( level, c.x, c.y, Terrain.WELL );
		
		//living-world twist: the well mirrors the sky — its water is set by the
		//moon phase the room was found under. Full moon heals, new moon reveals,
		//the quarters transmute; other phases keep the default random draw.
		@SuppressWarnings("unchecked")
		Class<? extends WellWater> waterClass;
		if (overrideWater != null) {
			waterClass = overrideWater;
		} else {
			switch (GameCalendar.moonPhase()) {
				case FULL_MOON:
					waterClass = WaterOfHealth.class; break;
				case NEW_MOON:
					waterClass = WaterOfAwareness.class; break;
				case FIRST_QUARTER: case LAST_QUARTER:
					waterClass = WaterOfTransmutation.class; break;
				default:
					waterClass = (Class<? extends WellWater>) Random.element( WATERS );
			}
		}

		WellWater.seed(c.x + level.width() * c.y, 1, waterClass, level);
		
		entrance().set( Door.Type.LOCKED );
		level.addItemToSpawn( new IronKey( Dungeon.depth ) );
	}
}
