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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ForgeCoals;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;

/**
 * The Tempering Forge: a smithy with nobody in it. The back wall holds the furnace, its
 * coal bed set into a niche so that nearly all of its heat goes one way - onto the anvil
 * standing in front of it. Down one side runs the quench trough. The work is the hero's:
 * lay a piece on the anvil, light the coals with any fire at all, wait for white heat,
 * quench. How hard each half of that is belongs to the weather, not to this room.
 *
 * A striker and a quench are left on the bench by the door - one firebloom seed, one
 * icecap seed - enough to do it once without having brought anything.
 */
public class TemperingForgeRoom extends SpecialRoom {

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
		int back = f.depth - 1;

		//the back row is the furnace wall: solid, but for the coal niche in its middle
		for (int s = 0; s < f.span; s++){
			int cell = f.cell( level, back, s );
			if (s == mid){
				Painter.set( level, cell, Terrain.EMBERS );
				WarpedRoomTiles.placeSwitch( level, cell,
						WarpedRoomTiles.COALS_COLD, WarpedRoomTiles.COALS_LIT, "coals" );
				Blob.seed( cell, 1, ForgeCoals.class, level );
			} else if (Math.abs( s - mid ) == 1){
				Painter.set( level, cell, Terrain.CUSTOM_DECO );
				WarpedRoomTiles.place( level, cell, WarpedRoomTiles.FURNACE, "furnace" );
			} else {
				Painter.set( level, cell, Terrain.WALL );
			}
		}

		TemperingAnvil anvil = new TemperingAnvil();
		anvil.pos = f.cell( level, back - 1, mid );
		level.mobs.add( anvil );

		//the trough: water down the side away from the door, two or three cells of it
		int troughSide = f.doorSide <= mid ? f.span - 1 : 0;
		int from = Math.max( 1, back - 3 );
		int[] trough = new int[back - from];
		for (int row = from; row < back; row++){
			int cell = f.cell( level, row, troughSide );
			Painter.set( level, cell, Terrain.WATER );
			trough[row - from] = cell;
		}
		anvil.trough = trough;

		//a rack of tools on the other side wall, for the look of the place
		int rackSide = troughSide == 0 ? f.span - 1 : 0;
		int rack = f.cell( level, back - 1, rackSide );
		Painter.set( level, rack, Terrain.CUSTOM_DECO );
		WarpedRoomTiles.place( level, rack, WarpedRoomTiles.TOOL_RACK, "tool_rack" );

		//the smith's leavings, by the door and well clear of the heat
		Point bench = f.at( 0, f.doorSide == rackSide ? f.clampSide( rackSide + (rackSide == 0 ? 1 : -1) ) : rackSide );
		int benchCell = level.pointToCell( bench );
		level.drop( new Firebloom.Seed(), benchCell );
		level.drop( new Icecap.Seed(), benchCell );

		entrance().set( Door.Type.REGULAR );
	}

	//water or grass laid over the smithy by the floor's painter would drown the coals
	@Override
	public boolean canPlaceWater( Point p ){
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ){
		return false;
	}
}
