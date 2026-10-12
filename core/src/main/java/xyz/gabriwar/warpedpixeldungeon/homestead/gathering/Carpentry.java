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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * The village carpenter (OverworldShopkeeper.CARPENTER): every human settlement big enough for a
 * trader (a village or larger, VillageRoutine.vendorHouses) has one, with a stall a few steps
 * from its well, besides its usual traders. He sells what a builder cannot gather - planks, rope,
 * thatch, iron fittings and glass panes - at twice their worth (Wares.MATERIAL_MARKUP), and buys
 * any material at its worth, as every trader does.
 *
 * His shelf is all five, always (lay): each restock lays out whichever of them is not still on
 * it, on the free cells round his stall - beside him first, then a step further out - so a
 * builder never finds the fittings or the glass missing. He sells no world map: the village's
 * other traders carry it.
 *
 * Set up once per village, kept in the run's record (GatheringWorld): a village the hero knew
 * before the homesteads gets its carpenter the next time the window lays it.
 */
public final class Carpentry {

	private Carpentry(){}

	/** How far from the well the stall stands, each way, in cells. */
	public static final int STALL_REACH = 3;
	/** The goods on his shelf: planks, rope, thatch, iron fittings, glass panes. */
	public static final int GOODS = 5;

	/**
	 * Called as the window populates a settlement (OverworldLevel.populateSettlement): sets the
	 * carpenter's stall up when this village should have one and never had it. Host only.
	 */
	public static void settle( OverworldLevel ow, long key ){
		if (ow.altitude() != 0 || ow.isMirror()) return;
		GatheringWorld world = HomesteadWorld.section( GatheringWorld.class );
		if (world == null || world.hasCarpenter( key )) return;
		VillageRoutine.Settlement s = VillageRoutine.settlement( ow.worldSeed(), key );
		if (s == null || s.gnoll || VillageRoutine.vendorHouses( s.houses() ) < 1) return;
		//a corner of the square by the well, the same one every time: the first, from the village's
		//own, with room round it for his five goods (else the roomiest)
		int first = (int) Math.floorMod( (key * 0x9E3779B97F4A7C15L) >>> 29, 4L );
		int at = -1, room = -1;
		for (int i = 0; i < 4 && room < GOODS; i++){
			int corner = (first + i) % 4;
			int c = ow.localCell( s.cx + (corner % 2 == 0 ? -STALL_REACH : STALL_REACH),
					s.cy + (corner < 2 ? -STALL_REACH : STALL_REACH) );
			if (c == -1 || !ow.passable[c]) continue;
			//by the window's own width: on the window's first build the path finder may still be
			//sized for the level left
			int free = 0, w = ow.width();
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					if ((dx != 0 || dy != 0) && shelfCell( ow, c + dx + dy * w )) free++;
				}
			}
			if (free > room){
				room = free;
				at = c;
			}
		}
		if (at == -1) return;
		OverworldShopkeeper keeper = OverworldShopkeeper.of( OverworldShopkeeper.CARPENTER, key );
		if (ow.addMob( keeper, at )){
			keeper.openShop();
			world.carpenterSet( key );
		}
	}

	/** A fresh shelf of the carpenter's goods: planks, rope, thatch, iron fittings, glass panes. */
	public static ArrayList<Item> stock(){
		ArrayList<Item> stock = new ArrayList<>();
		stock.add( new Plank().quantity( Random.IntRange( 8, 12 ) ) );
		stock.add( new Rope().quantity( Random.IntRange( 3, 6 ) ) );
		stock.add( new Thatch().quantity( Random.IntRange( 8, 12 ) ) );
		stock.add( new IronFittings().quantity( Random.IntRange( 1, 3 ) ) );
		stock.add( new GlassPane().quantity( Random.IntRange( 2, 4 ) ) );
		return stock;
	}

	/**
	 * The carpenter restocks (OverworldShopkeeper.restock): each of his goods not still on his
	 * shelf - a ware for sale within two cells of him - is laid out on a free cell beside him, or
	 * a step further out when those are taken. How many were laid.
	 */
	public static int lay( Mob keeper ){
		Level level = Dungeon.level;
		int w = level.width(), kx = keeper.pos % w, ky = keeper.pos / w;
		HashSet<Class<?>> shown = new HashSet<>();
		ArrayList<Integer> near = new ArrayList<>(), far = new ArrayList<>();
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int x = kx + dx, y = ky + dy;
				if ((dx == 0 && dy == 0) || x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				int c = x + y * w;
				Heap h = level.heaps.get( c );
				if (h != null && h.type == Heap.Type.FOR_SALE && !h.isEmpty()) shown.add( h.peek().getClass() );
				if (shelfCell( level, c )) (Math.max( Math.abs( dx ), Math.abs( dy ) ) == 1 ? near : far).add( c );
			}
		}
		near.addAll( far );
		int placed = 0;
		for (Item it : stock()){
			if (near.isEmpty()) break;
			if (shown.contains( it.getClass() )) continue;
			level.drop( it, near.remove( 0 ) ).type = Heap.Type.FOR_SALE;
			placed++;
		}
		return placed;
	}

	//a cell a ware may be laid on: open floor with nothing lying there
	private static boolean shelfCell( Level level, int c ){
		return c >= 0 && c < level.length() && level.insideMap( c ) && level.passable[c] && !level.pit[c]
				&& level.heaps.get( c ) == null;
	}
}
