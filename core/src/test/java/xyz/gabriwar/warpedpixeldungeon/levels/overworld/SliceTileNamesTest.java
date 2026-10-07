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

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;

//every terrain a slice of the world can show - generated, or left by the hero and the world
//(a mined face, fallen rock, burnt ground, broken ice) - has a real name and a description
//when examined: "???" was what a cave's crystal seam answered
public class SliceTileNamesTest {

	private static final long SEED = 0x51CE7E57L;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void everyTerrainOfEverySliceHasANameAndADescription(){
		ArrayList<String> bad = new ArrayList<>();
		for (int alt = -12; alt <= 10; alt++){
			if (!WorldLayers.exists( alt )) continue;
			TreeSet<Integer> seen = new TreeSet<>();
			for (int k = 0; k < 4; k++){
				int ox = -88 + k * 700, oy = -88 - k * 500;
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, ox, oy, 0f );
				for (int t : w.terrain) seen.add( t );
			}
			//what play leaves on any slice: a mined face, rubble, fire, broken ice, a dug shaft
			int[] left = { Terrain.EMPTY_DECO, Terrain.EMPTY, Terrain.MINE_BOULDER, Terrain.EMBERS,
					Terrain.WATER, Terrain.EXIT, Terrain.ENTRANCE, Terrain.CHASM };
			for (int t : left) seen.add( t );
			OverworldLevel level = new OverworldLevel( alt );
			//a site's prop is named by its cell (tileNameAt, CaveSites.cellKind), not by its terrain
			seen.remove( Terrain.CUSTOM_DECO );
			for (int t : seen){
				String name = level.tileName( t );
				String desc = level.tileDesc( t );
				if (name == null || name.equals( "???" ) || name.contains( Messages.NO_TEXT_FOUND )){
					bad.add( "altitude " + alt + " terrain " + t + ": name '" + name + "'" );
				}
				//bare ground, grass, a door and a pedestal say nothing more than their name, as everywhere
				boolean plain = t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO
						|| t == Terrain.CUSTOM_DECO_EMPTY || t == Terrain.GRASS || t == Terrain.DOOR
						|| t == Terrain.PEDESTAL;
				if (!plain && (desc == null || desc.trim().isEmpty() || desc.contains( Messages.NO_TEXT_FOUND ))){
					bad.add( "altitude " + alt + " terrain " + t + " (" + name + "): no description" );
				}
			}
		}
		assertTrue( String.join( "\n", bad ), bad.isEmpty() );
	}
}
