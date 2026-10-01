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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import org.junit.Test;

import java.util.EnumMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A village house interior picks its resident by the settlement its door belongs
 * to: every house door of every village must resolve back to that village.
 */
public class VillageHousesTest {

	private static final long SEED = 0x5EED0F7EA7L;

	@Test
	public void everyHouseDoorBelongsToItsOwnSettlement(){
		int doors = 0;
		EnumMap<WorldStructures.Faction, Integer> byFaction = new EnumMap<>( WorldStructures.Faction.class );
		for (int sy = -12; sy <= 12; sy++){
			for (int sx = -12; sx <= 12; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int cx = WorldStructures.siteX( SEED, sx, sy ), cy = WorldStructures.siteY( SEED, sx, sy );
				int[] layout = WorldStructures.settlementLayout( SEED, sx, sy );
				for (int i = 1; i + 1 < layout.length; i += 2){
					int wx = cx + WorldStructures.houseDoorDX( layout[i], layout[i+1] );
					int wy = cy + WorldStructures.houseDoorDY( layout[i], layout[i+1] );
					assertEquals( "door " + wx + "," + wy, WorldStructures.sectorOf( sx, sy ),
							WorldStructures.houseSector( SEED, wx, wy ) );
					doors++;
				}
				byFaction.merge( WorldStructures.faction( SEED, sx, sy ), 1, Integer::sum );
			}
		}
		assertTrue( "doors checked: " + doors, doors > 100 );
		assertTrue( "no gnoll village in the sample: " + byFaction, byFaction.getOrDefault( WorldStructures.Faction.GNOLL, 0 ) > 0 );
		assertEquals( Long.MIN_VALUE, WorldStructures.houseSector( SEED, 0, 0 ) );
	}
}
