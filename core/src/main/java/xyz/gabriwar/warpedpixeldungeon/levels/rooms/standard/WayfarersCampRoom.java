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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

/**
 * A Wayfarer's Camp: somebody's old camp in an ordinary chamber. A ring of stones round a
 * fire pit (CampFire - cold when found, and lit by any flame), a bedroll beside it
 * (Bedroll), a log to sit on and a crate with what the last traveller left: something to
 * eat and a firebloom seed to get the fire going.
 *
 * It is a standard room, so it costs the special-room queues nothing. What it gives is
 * time: sleeping here moves the clock, which is how the rooms and rites that wait on a
 * sky can be waited for.
 */
public class WayfarersCampRoom extends StandardRoom {

	@Override
	public int minWidth(){ return Math.max( 7, super.minWidth() ); }
	@Override
	public int minHeight(){ return Math.max( 7, super.minHeight() ); }

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );

		for (Door door : connected.values()){
			door.set( Door.Type.REGULAR );
		}

		Point c = new Point( (left + right) / 2, (top + bottom) / 2 );
		int w = level.width();
		int pit = level.pointToCell( c );

		Painter.set( level, pit, Terrain.EMBERS );
		WarpedRoomTiles.placeSwitch( level, pit,
				WarpedRoomTiles.FIRE_RING, WarpedRoomTiles.FIRE_RING_LIT, "camp_fire" );
		Blob.seed( pit, 1, CampFire.class, level );

		Bedroll bedroll = new Bedroll();
		bedroll.pos = pit - 1;
		bedroll.firePit = pit;
		level.mobs.add( bedroll );
		Painter.set( level, bedroll.pos, Terrain.EMPTY_SP );

		int seat = pit + 1;
		Painter.set( level, seat, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, seat, WarpedRoomTiles.LOG_SEAT, "log_seat" );

		//the ground the fire has been burning over: soot, a burnt stick, an ember. One of
		//three, so no two camps leave the same mark
		int ash = pit + w;
		Painter.set( level, ash, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, ash,
				WarpedRoomTiles.ASH_SPILL + Random.Int( WarpedRoomTiles.ASH_SPILLS ), "ash_spill" );

		int crate = pit + w + 1;
		Painter.set( level, crate, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, crate, WarpedRoomTiles.SUPPLIES, "supplies" );
		level.drop( Generator.random( Generator.Category.FOOD ), crate );
		level.drop( new Firebloom.Seed(), crate );
	}

	//no pond in the middle of the camp, and no trap beside the bed
	private boolean campGround( Point p ){
		Point c = new Point( (left + right) / 2, (top + bottom) / 2 );
		return Math.abs( p.x - c.x ) <= 2 && Math.abs( p.y - c.y ) <= 2;
	}

	@Override
	public boolean canPlaceWater( Point p ){
		return !campGround( p );
	}

	@Override
	public boolean canPlaceTrap( Point p ){
		return !campGround( p );
	}
}
