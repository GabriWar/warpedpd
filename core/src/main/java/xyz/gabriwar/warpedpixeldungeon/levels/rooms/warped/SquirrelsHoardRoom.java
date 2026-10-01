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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HoardSquirrel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LivingPlant;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretRoom;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The Squirrel's Hoard: a hidden hollow where something small has been putting seeds by.
 * What is in it depends on when in the year the floor is made - read once, here, off the
 * calendar, and never explained:
 *
 *   autumn  the hoard at its fullest: eight seeds, and nobody home
 *   winter  half of it eaten, and its owner asleep on the rest (HoardSquirrel). Four seeds
 *           for a hero who can take them without waking it
 *   spring  nobody came back for it, and it all came up: the hollow is a thicket of grown
 *           plants, a few of them the kind that bite (LivingPlant), with two seeds left
 *           that did not take
 *   summer  this year's hoard barely begun: three seeds among last year's husks
 */
public class SquirrelsHoardRoom extends SecretRoom {

	/** set by the test floor to paint a given season's hoard; null reads the calendar */
	public GameCalendar.Season seasonOverride = null;

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.GRASS );

		GameCalendar.Season season = seasonOverride != null ? seasonOverride : GameCalendar.season();

		//the nest of bedding in the middle of it all
		Point c = center();
		int nest = level.pointToCell( c );
		Painter.set( level, nest, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, nest, WarpedRoomTiles.BEDDING, "bedding" );

		ArrayList<Integer> floor = new ArrayList<>();
		for (int x = left + 1; x < right; x++){
			for (int y = top + 1; y < bottom; y++){
				int cell = x + y * level.width();
				if (cell != nest) floor.add( cell );
			}
		}
		Random.shuffle( floor );

		switch (season){
			case AUTUMN:
				seeds( level, floor, 8 );
				husks( level, floor, 1 );
				break;
			case WINTER:
				seeds( level, floor, 4 );
				husks( level, floor, 2 );
				HoardSquirrel squirrel = new HoardSquirrel();
				squirrel.pos = nest;
				squirrel.state = squirrel.SLEEPING;
				level.mobs.add( squirrel );
				break;
			case SPRING:
				thicket( level, floor );
				seeds( level, floor, 2 );
				break;
			case SUMMER:
			default:
				seeds( level, floor, 3 );
				husks( level, floor, 3 );
				break;
		}

		entrance().set( Door.Type.HIDDEN );
	}

	private void seeds( Level level, ArrayList<Integer> floor, int count ){
		for (int i = 0; i < count && !floor.isEmpty(); i++){
			level.drop( Generator.random( Generator.Category.SEED ), floor.remove( 0 ) );
		}
	}

	private void husks( Level level, ArrayList<Integer> floor, int count ){
		for (int i = 0; i < count && !floor.isEmpty(); i++){
			int cell = floor.remove( 0 );
			Painter.set( level, cell, Terrain.CUSTOM_DECO_EMPTY );
			WarpedRoomTiles.place( level, cell, WarpedRoomTiles.ACORNS, "husks" );
		}
	}

	//spring: tall grass wall to wall, the hoard grown into plants, and among them two or
	//three that woke up hungry
	private void thicket( Level level, ArrayList<Integer> floor ){
		for (int cell : floor) Painter.set( level, cell, Terrain.HIGH_GRASS );

		int plants = Math.min( floor.size(), Random.IntRange( 5, 7 ) );
		for (int i = 0; i < plants; i++){
			Plant.Seed seed = (Plant.Seed) Generator.random( Generator.Category.SEED );
			level.plant( seed, floor.remove( 0 ) );
		}

		int biters = Math.min( floor.size(), Random.IntRange( 2, 3 ) );
		for (int i = 0; i < biters; i++){
			Plant.Seed seed = (Plant.Seed) Generator.random( Generator.Category.SEED );
			if (seed.getPlantClass() == null) continue;
			Plant kind = Reflection.newInstance( seed.getPlantClass() );
			if (kind == null) continue;
			LivingPlant biter = new LivingPlant().setPlantClass( kind );
			biter.pos = floor.remove( 0 );
			biter.state = biter.SLEEPING;
			level.mobs.add( biter );
		}
	}
}
