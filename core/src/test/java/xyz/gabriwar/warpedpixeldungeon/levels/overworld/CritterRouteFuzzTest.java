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
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters.Route;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A hare's dash over thousands of random patches of country: whatever the ground, wherever
 * it sits (the window's very edge included) and wherever the fright comes from, the dash
 * stays on the map, every leg crosses only ground a hare can cross, a dash that claims cover
 * ends in it, and a hare that got anywhere got further from what frightened it.
 */
public class CritterRouteFuzzTest {

	private static final int W = 40, H = 40;
	private static final int[] GROUND = { Terrain.GRASS, Terrain.GRASS, Terrain.GRASS, Terrain.EMPTY,
			Terrain.FLOWER_PATCH, Terrain.HIGH_GRASS, Terrain.TREE_OAK, Terrain.TREE_PINE, Terrain.BOULDER,
			Terrain.SHRUB, Terrain.WALL, Terrain.WATER, Terrain.DEEP_WATER, Terrain.DIRT_PATH, Terrain.SNOW };

	private static boolean legClear( int[] map, float ax, float ay, float bx, float by ){
		int steps = Math.max( 1, (int)Math.ceil( Math.hypot( bx - ax, by - ay ) * 2 ) );
		for (int i = 1; i <= steps; i++){
			float f = i / (float)steps;
			int cx = (int)Math.floor( ax + (bx - ax) * f ), cy = (int)Math.floor( ay + (by - ay) * f );
			if (cx < 1 || cy < 1 || cx > W - 2 || cy > H - 2) return false;
			if (!OverworldCritters.runnable( map[cx + cy * W] )) return false;
		}
		return true;
	}

	@Test
	public void everyDashStaysOnGroundAHareCanCross(){
		Random dice = new Random( 0xC0FFEE );
		int dashes = 0, covered = 0;
		for (int trial = 0; trial < 4000; trial++){
			int[] map = new int[W * H];
			//patches rather than noise: a cell takes its neighbour's ground two times in three
			for (int i = 0; i < map.length; i++){
				map[i] = i > 0 && dice.nextInt( 3 ) > 0 ? map[i - 1] : GROUND[dice.nextInt( GROUND.length )];
				if (i >= W && dice.nextInt( 3 ) == 0) map[i] = map[i - W];
			}
			int sx = 1 + dice.nextInt( W - 2 ), sy = 1 + dice.nextInt( H - 2 );
			int start = sx + sy * W;
			//it sits where a hare may sit
			map[start] = Terrain.GRASS;
			float srcX = sx + 0.5f + dice.nextInt( 11 ) - 5, srcY = sy + 0.5f + dice.nextInt( 11 ) - 5;
			if (trial % 50 == 0){
				//right on top of it: any way at all
				srcX = sx + 0.5f;
				srcY = sy + 0.5f;
			}
			Route r = OverworldCritters.boltRoute( map, W, H, start, srcX, srcY, new Random( trial ) );
			dashes++;
			String at = "trial " + trial + " from " + sx + "," + sy;
			assertTrue( at, r.pts.length >= 2 && r.pts.length % 2 == 0 );
			float lx = sx + 0.5f, ly = sy + 0.5f;
			for (int i = 0; i + 1 < r.pts.length; i += 2){
				float px = r.pts[i], py = r.pts[i + 1];
				assertTrue( at + ": waypoint " + px + "," + py + " off the map",
						px >= 1f && py >= 1f && px < W - 1 && py < H - 1 );
				assertTrue( at + ": waypoint on " + map[(int)px + (int)py * W],
						OverworldCritters.runnable( map[(int)px + (int)py * W] ) );
				//the hemmed-in hop stays in its own cell, every other leg is checked
				if (r.pts.length > 2 || (int)px != sx || (int)py != sy){
					assertTrue( at + ": leg to " + px + "," + py + " crosses something", legClear( map, lx, ly, px, py ) );
				}
				lx = px;
				ly = py;
			}
			if (r.cover){
				covered++;
				assertTrue( at + ": claims cover, ends on " + map[(int)lx + (int)ly * W],
						OverworldCritters.isCover( map[(int)lx + (int)ly * W] ) );
			}
			//anywhere it got to is further from the fright than where it sat
			if (srcX != sx + 0.5f || srcY != sy + 0.5f){
				double d0 = Math.hypot( sx + 0.5f - srcX, sy + 0.5f - srcY );
				double d1 = Math.hypot( lx - srcX, ly - srcY );
				assertTrue( at + ": ran toward the fright (" + d0 + " -> " + d1 + ")", d1 >= d0 - 0.01 );
			}
		}
		assertEquals( 4000, dashes );
		//the country is mixed enough that plenty of dashes find cover, and plenty do not
		assertTrue( "only " + covered + " found cover", covered > 400 && covered < 3600 );
	}
}
