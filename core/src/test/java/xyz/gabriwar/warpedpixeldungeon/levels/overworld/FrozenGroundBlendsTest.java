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

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//the tundra's bare ground is drawn as snow while it is frozen (DungeonTerrainTilemap), so it must
//blend as snow too: an ice patch in it took a green rim of grass, and a boulder sat on a green
//square of bare ground, in the middle of the snow
public class FrozenGroundBlendsTest {

	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	//far out in the wilds, away from every village's fittings
	private static final int OX = 400_000, OY = 400_000;

	private final int ice = 40 + 30 * W, rock = 60 + 30 * W, mined = 80 + 30 * W, bare = 20 + 30 * W;

	private WindowGenerator.Window tundra( boolean frozen ){
		WindowGenerator.Window w = new WindowGenerator.Window();
		w.altitude = 0;
		w.terrain = new int[W * H];
		Arrays.fill( w.terrain, Terrain.EMPTY );
		w.terrain[ice] = Terrain.FROZEN_WATER;
		w.terrain[rock] = Terrain.BOULDER;
		w.terrain[mined] = Terrain.EMPTY_DECO;
		w.frozen = new boolean[W * H];
		Arrays.fill( w.frozen, frozen );
		return w;
	}

	private static boolean sideBlendOf( int row, int id ){
		for (int v = 0; v < OverworldDress.BIOME_VARIANTS; v++){
			if (id == OverworldDress.blend( row, 15, v )) return true;
		}
		return false;
	}

	@Test
	public void frozenBareGroundShowsAndBlendsAsSnow(){
		WindowGenerator.Window w = tundra( true );
		assertEquals( Terrain.SNOW, WindowGenerator.visualGround( w.terrain, w.frozen, bare ) );
		assertEquals( "mined floor in the frost", Terrain.SNOW, WindowGenerator.visualGround( w.terrain, w.frozen, mined ) );
		assertEquals( "a boulder lies in the snow around it", Terrain.SNOW, WindowGenerator.rockGround( w.terrain, w.frozen, rock ) );

		int[][] dress = WindowGenerator.dress( 7L, OX, OY, w.terrain, w, GameCalendar.Season.WINTER );
		assertTrue( "the snow laps over the ice patch, not grass: " + dress[0][ice],
				sideBlendOf( OverworldDress.BLEND_SNOW, dress[0][ice] ) );
	}

	@Test
	public void thawedBareGroundStillSpillsGreenOverIce(){
		WindowGenerator.Window w = tundra( false );
		assertEquals( Terrain.EMPTY, WindowGenerator.visualGround( w.terrain, w.frozen, bare ) );
		assertEquals( Terrain.EMPTY, WindowGenerator.rockGround( w.terrain, w.frozen, rock ) );

		int[][] dress = WindowGenerator.dress( 7L, OX, OY, w.terrain, w, GameCalendar.Season.SUMMER );
		assertTrue( "bare ground's green over the ice: " + dress[0][ice],
				sideBlendOf( OverworldDress.BLEND_GRASS, dress[0][ice] ) );
	}
}
