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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RivalStatue;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;

/**
 * The Rival's Gallery: a locked hall of ordinary statues with one at the far end that
 * is not. The floor is made as the hero arrives, so the room can read the weapon in
 * their hand: the rival is armed with one of the same perk group, a rarity above it and
 * already alpha (RivalStatue.armAgainst). Beat the twist you have not unlocked yet, and
 * it is yours.
 */
public class RivalGalleryRoom extends SpecialRoom {

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

		//the gallery: a statue down each side wall on every other row, and a bare aisle
		//up the middle to the plinth
		for (int row = 1; row < back; row += 2){
			Painter.set( level, f.at( row, 0 ), Terrain.STATUE_SP );
			Painter.set( level, f.at( row, f.span - 1 ), Terrain.STATUE_SP );
		}

		int plinth = f.cell( level, back, mid );
		Painter.set( level, plinth, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, plinth, WarpedRoomTiles.PLINTH, "rival_plinth" );
		//braziers either side of it, so the far end reads as the point of the room
		if (mid - 1 >= 1){
			brazier( level, f.cell( level, back, mid - 1 ) );
			brazier( level, f.cell( level, back, mid + 1 ) );
		}

		RivalStatue rival = new RivalStatue();
		rival.armAgainst( Dungeon.hero );
		rival.pos = plinth;
		level.mobs.add( rival );

		entrance().set( Door.Type.LOCKED );
		level.addItemToSpawn( new IronKey( Dungeon.depth ) );
	}

	private void brazier( Level level, int cell ){
		Painter.set( level, cell, Terrain.CUSTOM_DECO );
		WarpedRoomTiles.place( level, cell, WarpedRoomTiles.BRAZIER, "brazier" );
	}
}
