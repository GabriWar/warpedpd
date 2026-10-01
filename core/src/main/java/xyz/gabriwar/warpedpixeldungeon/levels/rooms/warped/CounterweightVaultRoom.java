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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CounterweightPlates;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

/**
 * The Counterweight Vault: a barred vault at the back of a hall, and two pressure plates
 * near the front of it, too far apart for one pair of feet and too far from the bars to
 * hold one and walk through. Both must be held at once (CounterweightPlates).
 *
 * In company that is two people. Alone it is whatever else the hero has that weighs
 * something: a pet, a summoned creature, a shadow clone, a mirror image, a Sokoban sheep,
 * an enemy lured onto it - or a spare sword dropped on one plate and a spare cuirass on
 * the other. The bars are bars: the prize can be seen through them from the door.
 */
public class CounterweightVaultRoom extends SpecialRoom {

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
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );

		RoomFrame f = new RoomFrame( this, entrance() );
		int mid = f.span / 2;
		int back = f.depth - 1;

		//the vault: three by two against the far wall, its gateway in the middle
		int wallRow = back - 2;
		int[] inside = new int[6];
		int n = 0;
		for (int row = wallRow; row <= back; row++){
			for (int s = 0; s < f.span; s++){
				boolean in = row > wallRow && Math.abs( s - mid ) <= 1;
				Painter.set( level, f.at( row, s ), in ? Terrain.EMPTY_SP : Terrain.WALL );
				if (in) inside[n++] = f.cell( level, row, s );
			}
		}
		int gate = f.cell( level, wallRow, mid );
		Painter.set( level, gate, Terrain.CUSTOM_DECO );
		WarpedRoomTiles.placeSwitch( level, gate,
				WarpedRoomTiles.PORTCULLIS, WarpedRoomTiles.PORTCULLIS_OPEN, "portcullis" );

		//the plates: second row in, as far apart as the hall lets them be
		CounterweightPlates plates = null;
		for (int s : new int[]{ 0, f.span - 1 }){
			int cell = f.cell( level, 1, s );
			Painter.set( level, cell, Terrain.CUSTOM_DECO_EMPTY );
			WarpedRoomTiles.placeSwitch( level, cell,
					WarpedRoomTiles.PLATE_UP, WarpedRoomTiles.PLATE_DOWN, "plate" );
			plates = Blob.seed( cell, 1, CounterweightPlates.class, level );
		}
		plates.gate = gate;
		plates.vault = inside;

		Heap chest = level.drop( prize(), f.cell( level, back, mid ) );
		chest.type = Heap.Type.CHEST;
		level.drop( new Gold().random(), f.cell( level, back, mid - 1 ) );
		level.drop( new Gold().random(), f.cell( level, back, mid + 1 ) );

		entrance().set( Door.Type.REGULAR );
	}

	private static Item prize(){
		Item item = Generator.random( Random.oneOf(
				Generator.Category.WEAPON, Generator.Category.ARMOR,
				Generator.Category.RING, Generator.Category.ARTIFACT ) );
		item.cursed = false;
		return WarpedRooms.forceQuality( item,
				Random.Float() < 0.25f ? Rarity.LEGENDARY : Rarity.RARE, null );
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
