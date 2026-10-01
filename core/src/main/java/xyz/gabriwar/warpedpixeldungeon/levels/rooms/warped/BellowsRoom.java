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

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BellowsDraft;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MobSpawner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BellowsValve;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The Bellows: a ventilation gallery with a wind that never drops, blowing from the
 * door's wall to the far one (BellowsDraft). Against that far wall something has made
 * its den around a chest, and is asleep in it.
 *
 * The room's answer is in the wind. Anything released at the door end is carried down
 * the gallery and packed against the far wall, and none of it comes back - the valve by
 * the door holds one canister of bad air for exactly that. The same wind adds to every
 * throw made with it and takes from every throw made into it, which matters when what
 * was asleep wakes up and comes up the gallery at whoever is standing in the doorway.
 */
public class BellowsRoom extends SpecialRoom {

	@Override
	public int minWidth(){ return 9; }
	@Override
	public int maxWidth(){ return 11; }
	@Override
	public int minHeight(){ return 9; }
	@Override
	public int maxHeight(){ return 11; }

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );

		RoomFrame f = new RoomFrame( this, entrance() );
		int mid = f.span / 2;
		int back = f.depth - 1;
		int heading = f.heading();

		//the draught fills the whole room
		BellowsDraft draft = (BellowsDraft) level.blobs.get( BellowsDraft.class );
		if (draft == null){
			draft = new BellowsDraft();
			level.blobs.put( BellowsDraft.class, draft );
		}
		int[] gallery = new int[f.depth * f.span];
		int marked = 0;
		for (int row = 0; row < f.depth; row++){
			for (int s = 0; s < f.span; s++){
				int cell = f.cell( level, row, s );
				draft.mark( level, cell, heading );
				gallery[marked++] = cell;
			}
		}

		//by the door: the gas canister on one side of it, letting out downwind, and on the
		//other a painted board that says, without a word, what becomes of anything let go here
		int valveSide = f.doorSide > 0 ? f.doorSide - 1 : f.doorSide + 1;
		int signSide = valveSide < f.doorSide ? f.doorSide + 1 : f.doorSide - 1;
		if (signSide < 0 || signSide >= f.span) signSide = valveSide < f.doorSide ? valveSide - 1 : valveSide + 1;

		//intake grates under the door wall, outlet grates under the far one
		for (int s = 0; s < f.span; s++){
			if (s != f.doorSide && s != valveSide && s != signSide){
				floor( level, f.cell( level, 0, s ), WarpedRoomTiles.GRILLE_IN, "grille_in" );
			}
			floor( level, f.cell( level, back, s ), WarpedRoomTiles.GRILLE_OUT, "grille_out" );
		}
		//chalked arrows down the middle of the gallery, for anyone who cannot feel a wind
		for (int row = 2; row < back - 2; row += 2){
			floor( level, f.cell( level, row, mid ), WarpedRoomTiles.WIND_EAST + heading, "wind_mark" );
		}

		BellowsValve valve = new BellowsValve();
		valve.pos = f.cell( level, 0, valveSide );
		valve.outlet = f.cell( level, 1, valveSide );
		valve.gallery = gallery;
		level.mobs.add( valve );

		if (signSide >= 0 && signSide < f.span){
			int sign = f.cell( level, 0, signSide );
			Painter.set( level, sign, Terrain.CUSTOM_DECO );
			WarpedRoomTiles.place( level, sign, WarpedRoomTiles.WARNING_SIGN, "warning_sign" );
		}

		//the den against the far wall: the chest, its gold, and what sleeps on them
		int chestCell = f.cell( level, back, mid );
		Heap chest = level.drop( Generator.random( Random.oneOf(
				Generator.Category.WEAPON, Generator.Category.ARMOR,
				Generator.Category.RING, Generator.Category.WAND, Generator.Category.ARTIFACT ) ), chestCell );
		chest.type = Heap.Type.CHEST;
		level.drop( new Gold().random(), f.cell( level, back, mid + 1 ) );

		ArrayList<Class<? extends Mob>> rotation = MobSpawner.getMobRotation( WarpedRooms.threat() );
		int sleepers = f.span >= 9 ? 4 : 3;
		ArrayList<Point> beds = new ArrayList<>();
		for (int row = back - 1; row <= back; row++){
			for (int s = 0; s < f.span; s++){
				Point p = f.at( row, s );
				int cell = level.pointToCell( p );
				if (cell != chestCell && level.heaps.get( cell ) == null) beds.add( p );
			}
		}
		Random.shuffle( beds );
		for (int i = 0; i < sleepers && i < beds.size() && !rotation.isEmpty(); i++){
			Mob mob = Reflection.newInstance( rotation.get( i % rotation.size() ) );
			if (mob == null) continue;
			mob.pos = level.pointToCell( beds.get( i ) );
			mob.state = mob.SLEEPING;
			level.mobs.add( mob );
		}

		entrance().set( Door.Type.REGULAR );
	}

	private void floor( Level level, int cell, int art, String key ){
		Painter.set( level, cell, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, cell, art, key );
	}

	@Override
	public boolean canPlaceWater( Point p ){
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ){
		return false;
	}

	//bare stone is where the floor's painter lays its traps: not in the gallery
	@Override
	public boolean canPlaceTrap( Point p ){
		return false;
	}
}
