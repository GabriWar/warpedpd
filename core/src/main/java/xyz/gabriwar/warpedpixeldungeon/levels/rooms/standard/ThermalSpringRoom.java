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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard;

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ThermalVent;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

/**
 * A Thermal Spring: an ordinary chamber with a pool in the middle of it that something
 * under the floor keeps hot (ThermalVent). It is a standard room, so it costs the
 * special-room queues nothing, and it has no reward in it but what the water is: the one
 * warm place on a winter floor, a cure for the cold, and a slow way to cook yourself if
 * you fall asleep in it.
 */
public class ThermalSpringRoom extends StandardRoom {

	@Override
	public int minWidth(){ return Math.max( 7, super.minWidth() ); }
	@Override
	public int minHeight(){ return Math.max( 7, super.minHeight() ); }

	@Override
	public float[] sizeCatProbs(){
		return new float[]{ 3, 1, 0 };
	}

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );
		Painter.fillEllipse( level, this, 2, Terrain.WATER );

		for (Door door : connected.values()){
			door.set( Door.Type.REGULAR );
		}

		//the vent is the pool's middle; every other cell of open water is just warm
		int vent = level.pointToCell( new Point( (left + right) / 2, (top + bottom) / 2 ) );
		for (int x = left + 1; x < right; x++){
			for (int y = top + 1; y < bottom; y++){
				int cell = x + y * level.width();
				if (level.map[cell] == Terrain.WATER){
					Blob.seed( cell, cell == vent ? ThermalVent.VENT : ThermalVent.WATER, ThermalVent.class, level );
				}
			}
		}
	}

	//the pool is the painter's to leave alone: water poured over the rim would be cold
	@Override
	public boolean canPlaceWater( Point p ){
		return false;
	}
}
