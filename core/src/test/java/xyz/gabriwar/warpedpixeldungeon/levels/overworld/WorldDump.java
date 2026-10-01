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
import org.junit.Assume;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Dumps every slice of one window as JSON for tools/world3d.py, which turns it
 * into a 3D page of the world. Runs only when asked:
 *
 *   ./gradlew :core:test --tests '*WorldDump' -Dworld.dump=/tmp/world.json [-Dworld.seed=N] [-Dworld.x=X -Dworld.y=Y]
 *
 * Without an origin, a window holding both mountains and open water is searched
 * for. One character per cell (see code()).
 */
public class WorldDump {

	private static char code( int t, int tier, byte link ){
		if (link == WindowGenerator.LINK_MOUTH || link == WindowGenerator.LINK_CAVE_EXIT) return 'm';
		switch (t){
			case Terrain.CHASM: return '.';
			case Terrain.WALL: case Terrain.WALL_DECO: return '#';
			case Terrain.MINE_CRYSTAL: return 'c';
			case Terrain.MINE_BOULDER: case Terrain.BOULDER: return 'b';
			case Terrain.WATER: return 'w';
			case Terrain.DEEP_WATER: return tier >= 4 ? 'V' : 'W';
			case Terrain.FROZEN_WATER: return 'i';
			case Terrain.SNOW: return 's';
			case Terrain.TREE_PINE: case Terrain.TREE_OAK: return 't';
			case Terrain.EMPTY_SP: return 'a';   //sand
			case Terrain.ENTRANCE: return 'u';
			case Terrain.EXIT: return 'd';
			case Terrain.DIRT_PATH: case Terrain.BRIDGE: return 'r';
			case Terrain.TOWN_SOLID: case Terrain.DOOR: case Terrain.BARRICADE: return 'h';
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FLOWER_PATCH: case Terrain.SHRUB: return 'g';
			case Terrain.MUSHROOM_PATCH: return 'f';
			default: return 'e';   //bare ground
		}
	}

	@Test
	public void dump() throws IOException {
		String path = System.getProperty( "world.dump" );
		Assume.assumeTrue( "set -Dworld.dump=<file> to dump the world", path != null && !path.isEmpty() );
		long seed = Long.getLong( "world.seed", 0x5EED0F7EA7L );
		int ox, oy;
		if (System.getProperty( "world.x" ) != null){
			ox = Integer.getInteger( "world.x" );
			oy = Integer.getInteger( "world.y" );
		} else {
			int[] o = findOrigin( seed );
			ox = o[0]; oy = o[1];
		}
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		StringBuilder sb = new StringBuilder();
		sb.append( "{\"seed\":" ).append( seed ).append( ",\"ox\":" ).append( ox ).append( ",\"oy\":" ).append( oy )
				.append( ",\"w\":" ).append( w ).append( ",\"h\":" ).append( h )
				.append( ",\"minAlt\":" ).append( -WorldLayers.MAX_BELOW ).append( ",\"maxAlt\":" ).append( WorldLayers.MAX_ABOVE )
				.append( ",\"slices\":[" );
		for (int a = -WorldLayers.MAX_BELOW; a <= WorldLayers.MAX_ABOVE; a++){
			WindowGenerator.Window win = WindowGenerator.generate( seed, a, ox, oy, 0f );
			if (a > -WorldLayers.MAX_BELOW) sb.append( ',' );
			sb.append( '"' );
			for (int i = 0; i < w * h; i++) sb.append( code( win.terrain[i], win.waterDepth[i], win.link[i] ) );
			sb.append( '"' );
		}
		sb.append( "]}" );
		try (FileWriter out = new FileWriter( path )){
			out.write( sb.toString() );
		}
		System.out.println( "world dumped to " + path + " (origin " + ox + "," + oy + ")" );
	}

	//a window with mountains AND open water in it, so the page shows every kind of slice
	private static int[] findOrigin( long seed ){
		int[] best = null;
		int bestScore = Integer.MIN_VALUE;
		for (int oy = -3000; oy < 3000; oy += 300){
			for (int ox = -3000; ox < 3000; ox += 300){
				int high = 0, wet = 0;
				for (int y = 8; y < WindowGenerator.HEIGHT; y += 16){
					for (int x = 8; x < WindowGenerator.WIDTH; x += 16){
						float e = WorldModel.elevation( seed, ox + x, oy + y );
						if (WorldLayers.band( e ) >= 2) high++;
						if (e < WorldModel.SEA) wet++;
					}
				}
				//a few peaks and a shore, the rest walkable land
				int score = Math.min( high, 12 ) + Math.min( wet, 12 ) - Math.max( 0, high - 30 ) - Math.max( 0, wet - 40 );
				if (high > 4 && wet > 4 && score > bestScore){
					bestScore = score;
					best = new int[]{ ox, oy };
				}
			}
		}
		return best != null ? best : new int[]{ -WindowGenerator.WIDTH/2, -WindowGenerator.HEIGHT/2 };
	}
}
