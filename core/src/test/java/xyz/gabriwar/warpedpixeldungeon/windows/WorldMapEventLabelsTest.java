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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The world map's event names (WndWorldMap.labelCorner): a market's or a raid's hangs off the
 * corner of its village's dot that the village's own name leaves free, and when a market and
 * a raid are on one village's well the same day the second takes the other corner on that
 * side - three names round one dot, none over another.
 */
public class WorldMapEventLabelsTest {

	//every sector parity, the negative sectors' too
	@Test
	public void aMarketAndARaidOnOneWellNameThemselvesApart(){
		for (int sy = -2; sy <= 1; sy++){
			for (int sx = -2; sx <= 1; sx++){
				int wx = sx * 64 + 30, wy = sy * 64 + 30;
				WorldEvents.Event market = new WorldEvents.Event( WorldEvents.Type.MARKET, sx, sy, 5, 0, 100, wx, wy, 1L );
				WorldEvents.Event raid = new WorldEvents.Event( WorldEvents.Type.RAID, sx, sy, 5, 0, 100, wx, wy, 2L );
				WorldEvents.Event star = new WorldEvents.Event( WorldEvents.Type.FALLEN_STAR, sx, sy, 5, 0, 100, wx + 9, wy, 3L );
				//the village's own name, as labelSites hangs it
				boolean[] village = { (sy & 1) == 0, (sx & 1) == 0 };
				for (List<WorldEvents.Event> l : Arrays.asList( Arrays.asList( market, raid, star ), Arrays.asList( raid, market, star ) )){
					assertFalse( WndWorldMap.stacked( l, 0 ) );
					assertTrue( WndWorldMap.stacked( l, 1 ) );
					assertFalse( WndWorldMap.stacked( l, 2 ) );
					boolean[] first = WndWorldMap.labelCorner( l, 0 ), second = WndWorldMap.labelCorner( l, 1 );
					String at = "sector " + sx + "," + sy;
					assertFalse( at + ": the event names on one corner", Arrays.equals( first, second ) );
					assertFalse( at + ": over the village's name", Arrays.equals( first, village ) );
					assertFalse( at + ": over the village's name", Arrays.equals( second, village ) );
					//the side the village's name leaves free, one name above the other
					assertEquals( first[0], second[0] );
					//a star alone keeps the upper right
					assertArrayEquals( new boolean[]{ true, false }, WndWorldMap.labelCorner( l, 2 ) );
				}
				//alone, a market keeps the corner it always had
				assertArrayEquals( new boolean[]{ !village[0], !village[1] },
						WndWorldMap.labelCorner( Arrays.asList( market ), 0 ) );
			}
		}
	}
}
