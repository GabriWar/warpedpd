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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ElementalLock;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Waterweed;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

/**
 * The Elemental Lock Vault: an antechamber with three rune plots in its floor, and at
 * the back a small vault behind a gate of light that has no keyhole. Three of the four
 * runes (ember, rime, bloom, tide) are drawn for each vault; what each wants is said
 * only by its picture and a line of verse, and is checked only against the world
 * (ElementalLock). One seed that answers one of the three is left lying in the room, as
 * the hint that seeds answer runes at all.
 *
 * The gate is solid but not opaque: the prize - gear of rare quality or better - can be
 * seen from the first step in.
 */
public class ElementalLockRoom extends SpecialRoom {

	@Override
	public int minWidth(){ return 9; }
	@Override
	public int maxWidth(){ return 11; }
	@Override
	public int minHeight(){ return 9; }
	@Override
	public int maxHeight(){ return 11; }

	private static final String[] KEYS = { "", "rune_ember", "rune_rime", "rune_bloom", "rune_tide" };
	private static final int[] ART = { 0, WarpedRoomTiles.RUNE_EMBER, WarpedRoomTiles.RUNE_RIME,
			WarpedRoomTiles.RUNE_BLOOM, WarpedRoomTiles.RUNE_TIDE };

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		//plain stone, not flagstones: the tide rune is answered by water, and the things
		//that make water only make it out of bare floor
		Painter.fill( level, this, 1, Terrain.EMPTY );

		RoomFrame f = new RoomFrame( this, entrance() );
		int mid = f.span / 2;
		int back = f.depth - 1;

		//the vault: a three-by-two cell against the far wall, walled in all round, with
		//the gate in the middle of its front
		int wallRow = back - 2;
		for (int row = wallRow; row <= back; row++){
			for (int s = 0; s < f.span; s++){
				boolean inside = row > wallRow && Math.abs( s - mid ) <= 1;
				Painter.set( level, f.at( row, s ), inside ? Terrain.EMPTY_SP : Terrain.WALL );
			}
		}
		int gate = f.cell( level, wallRow, mid );
		Painter.set( level, gate, Terrain.CUSTOM_DECO );
		WarpedRoomTiles.place( level, gate, WarpedRoomTiles.RUNE_GATE, "rune_gate" );

		//three of the four runes, in a row across the antechamber
		int[] kinds = { ElementalLock.EMBER, ElementalLock.RIME, ElementalLock.BLOOM, ElementalLock.TIDE };
		shuffle( kinds );
		int runeRow = Math.max( 1, wallRow - 2 );
		int[] sides = { mid - 2, mid, mid + 2 };
		ElementalLock lock = null;
		for (int i = 0; i < 3; i++){
			int cell = f.cell( level, runeRow, sides[i] );
			Painter.set( level, cell, Terrain.CUSTOM_DECO_EMPTY );
			WarpedRoomTiles.placeSwitch( level, cell, ART[kinds[i]],
					ART[kinds[i]] + WarpedRoomTiles.RUNE_LIT_OFFSET, KEYS[kinds[i]] );
			lock = Blob.seed( cell, kinds[i], ElementalLock.class, level );
		}
		lock.gate = gate;

		//the prize, in plain sight through the gate
		Heap chest = level.drop( prize(), f.cell( level, back, mid ) );
		chest.type = Heap.Type.CHEST;
		level.drop( new Gold().random(), f.cell( level, back, mid - 1 ) );
		level.drop( new Gold().random(), f.cell( level, back, mid + 1 ) );

		//the hint: a seed that answers one of this vault's runes, left by the way in
		int hintSide = f.doorSide <= mid ? f.span - 1 : 0;
		level.drop( hint( kinds[Random.Int( 3 )] ), f.cell( level, 0, hintSide ) );

		entrance().set( Door.Type.REGULAR );
	}

	private static void shuffle( int[] a ){
		for (int i = a.length - 1; i > 0; i--){
			int j = Random.Int( i + 1 );
			int t = a[i]; a[i] = a[j]; a[j] = t;
		}
	}

	private static Item prize(){
		Item item = Generator.random( Random.oneOf(
				Generator.Category.WEAPON, Generator.Category.ARMOR, Generator.Category.WAND ) );
		item.cursed = false;
		return WarpedRooms.forceQuality( item,
				Random.Float() < 0.3f ? Rarity.LEGENDARY : Rarity.RARE, null );
	}

	private static Item hint( int kind ){
		switch (kind){
			case ElementalLock.EMBER: return new Firebloom.Seed();
			case ElementalLock.RIME:  return new Icecap.Seed();
			case ElementalLock.TIDE:  return new Waterweed.Seed();
			default:                  return Generator.random( Generator.Category.SEED );
		}
	}

	//the runes are read off the floor itself: nothing may be poured or sown over them
	@Override
	public boolean canPlaceWater( Point p ){
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ){
		return false;
	}

	//bare stone is where the floor's painter lays its traps: not among the runes
	@Override
	public boolean canPlaceTrap( Point p ){
		return false;
	}
}
