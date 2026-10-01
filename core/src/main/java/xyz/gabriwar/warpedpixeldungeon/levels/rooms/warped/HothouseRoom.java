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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HothouseGlass;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.PlantGrowthManager;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The Hothouse: planting beds under glass (HothouseGlass), an aisle up the middle and
 * one across the front. It opens with a few plants already up and a couple of seeds on
 * the potting bench; after that it is a garden the hero can use - whatever is planted
 * in a bed spreads along it in its own time, all year, and goes to seed generously when
 * it is harvested. In winter it is the only ground on the floor where anything grows.
 */
public class HothouseRoom extends SpecialRoom {

	@Override
	public int minWidth(){ return 7; }
	@Override
	public int minHeight(){ return 7; }

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );

		RoomFrame f = new RoomFrame( this, entrance() );
		int mid = f.span / 2;

		ArrayList<Integer> beds = new ArrayList<>();
		for (int row = 1; row < f.depth; row++){
			for (int s = 0; s < f.span; s++){
				if (s == mid) continue;
				int cell = f.cell( level, row, s );
				Painter.set( level, cell, Terrain.CUSTOM_DECO_EMPTY );
				WarpedRoomTiles.place( level, cell, WarpedRoomTiles.PLANTER, "planter" );
				Blob.seed( cell, 1, HothouseGlass.class, level );
				beds.add( cell );
			}
		}

		//what was growing when the gardener left: spring's flora, whatever the season
		Random.shuffle( beds );
		int grown = Math.min( beds.size(), Random.IntRange( 3, 4 ) );
		for (int i = 0; i < grown; i++){
			Class<? extends Plant.Seed> kind = PlantGrowthManager.selectPlantClass(
					level, GameCalendar.Season.SPRING );
			if (kind == null) continue;
			Plant.Seed seed = Reflection.newInstance( kind );
			if (seed != null) level.plant( seed, beds.get( i ) );
		}

		//the potting bench: the front corner away from the door
		int benchSide = f.doorSide <= mid ? f.span - 1 : 0;
		int bench = f.cell( level, 0, benchSide );
		for (int i = 0; i < 2; i++){
			level.drop( Generator.random( Generator.Category.SEED ), bench );
		}

		entrance().set( Door.Type.REGULAR );
	}

	@Override
	public boolean canPlaceWater( Point p ){
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ){
		return false;
	}
}
