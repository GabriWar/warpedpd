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

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.NestBrood;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.NestCoals;
import xyz.gabriwar.warpedpixeldungeon.items.Egg;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;

/**
 * The Incubator Nest: a round brooding chamber. In the middle is a straw nest with an
 * egg in it (NestBrood), and at the four points around it four beds of banked coals
 * (NestCoals) - light one with any flame and the fire creeps round the ring, and all
 * four together hold the nest well past brooding heat for most of a hundred turns. The
 * same nest broods just as steadily on frost, on a poisonous cloud, or on live sparks,
 * for a keeper who would rather hatch something else; nothing in the room says so.
 */
public class IncubatorNestRoom extends SpecialRoom {

	@Override
	public int minWidth(){ return 7; }
	@Override
	public int minHeight(){ return 7; }

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fillEllipse( level, this, 1, Terrain.EMPTY_SP );
		//a round room can round its own doorway shut: cut straight in from the door
		Painter.drawInside( level, this, entrance(), Math.max( width(), height() ) / 2 - 1, Terrain.EMPTY_SP );

		Point c = new Point( (left + right) / 2, (top + bottom) / 2 );
		int nest = level.pointToCell( c );
		Painter.set( level, nest, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, nest, WarpedRoomTiles.NEST, "nest" );
		Blob.seed( nest, 1, NestBrood.class, level );
		level.drop( new Egg(), nest );

		int w = level.width();
		for (int coal : new int[]{ nest - w, nest + w, nest - 1, nest + 1 }){
			Painter.set( level, coal, Terrain.EMBERS );
			WarpedRoomTiles.placeSwitch( level, coal,
					WarpedRoomTiles.COALS_COLD, WarpedRoomTiles.COALS_LIT, "coals" );
			Blob.seed( coal, 1, NestCoals.class, level );
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
