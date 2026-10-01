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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Collector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.RegularLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The Warped room slot: a queue of special rooms of its own, so that the rooms Warped
 * adds never thin out the vanilla special rooms (SpecialRoom's run queue is fixed-rate:
 * every class added there makes every other one rarer). One of these is added to a
 * regular floor every two or three floors, each seen once before any repeats, and the
 * black market is pinned to one floor of every chapter the way the laboratory is.
 *
 * The run-wide standing with the black market lives here too, because it outlasts any
 * one floor: the heat the hero has drawn, the gold the collector is carrying back, and
 * whether the market will still deal with them at all.
 */
public class WarpedRooms {

	private static final ArrayList<Class<? extends Room>> SLOT_ROOMS = new ArrayList<>( Arrays.asList(
			RivalGalleryRoom.class, TemperingForgeRoom.class, BreakersBenchRoom.class,
			PedigreeHallRoom.class, FrozenCacheRoom.class, BellowsRoom.class,
			ElementalLockRoom.class, HothouseRoom.class, IncubatorNestRoom.class,
			CounterweightVaultRoom.class
	) );

	public static ArrayList<Class<? extends Room>> runQueue = new ArrayList<>();

	//the first floor deep enough for the next slot room
	private static int nextSlotDepth = 2;
	//a floor that fails to build is built again from scratch, initRooms and all: the
	//choice made for a depth is remembered so the retry gets the same room back instead
	//of finding the slot already spent
	private static int lastSlotDepth = -1;
	private static Class<? extends Room> lastSlotRoom = null;

	//the one floor of each chapter that hides the black market (floor 2, 3 or 4 of five)
	private static int[] marketDepth = new int[5];

	// ---------------------------------------------------------------- black market standing

	/** purchases, contracts, fenced goods and spins all draw heat; at this much the
	 *  collector comes for the hero on every floor they reach, until it is killed */
	public static final int HEAT_LIMIT = 3;
	public static int heat = 0;
	//what the hero has paid the market this run: the collector carries a share of it
	public static int goldSpent = 0;
	//the hero drew steel in a market: no dealer sets up for them again this run
	public static boolean banned = false;

	public static void initForRun(){
		runQueue = new ArrayList<>( SLOT_ROOMS );
		Random.shuffle( runQueue );
		nextSlotDepth = Random.IntRange( 2, 3 );
		lastSlotDepth = -1;
		lastSlotRoom = null;
		for (int region = 0; region < marketDepth.length; region++){
			marketDepth[region] = region * 5 + Random.IntRange( 2, 4 );
		}
		heat = 0;
		goldSpent = 0;
		banned = false;
	}

	/** Called at the end of RegularLevel.initRooms: the slot room and the market, when
	 *  this floor is due one. Only the main dungeon's regular floors have a slot. */
	public static void addRooms( Level level, ArrayList<Room> rooms ){
		int depth = Dungeon.depth;
		if (Dungeon.branch != 0 || depth < 1 || depth > 24 || Dungeon.bossLevel( depth )) return;

		int region = (depth - 1) / 5;
		if (region < marketDepth.length && marketDepth[region] == depth){
			rooms.add( new BlackMarketRoom() );
		}

		if (lastSlotDepth == depth && lastSlotRoom != null){
			rooms.add( Reflection.newInstance( lastSlotRoom ) );
			return;
		}
		if (depth < nextSlotDepth) return;

		for (int i = 0; i < runQueue.size(); i++){
			Class<? extends Room> type = runQueue.get( i );
			if (!allowed( type, level )) continue;
			runQueue.remove( i );
			runQueue.add( type );
			lastSlotDepth = depth;
			lastSlotRoom = type;
			nextSlotDepth = depth + Random.IntRange( 2, 3 );
			rooms.add( Reflection.newInstance( type ) );
			return;
		}
	}

	//the frozen cache is a sheet of ice: it needs a floor whose water can freeze at
	//all (the city and the halls run with lava, which never does)
	private static boolean allowed( Class<? extends Room> type, Level level ){
		if (type == FrozenCacheRoom.class) return level.waterCanFreeze();
		return true;
	}

	// ---------------------------------------------------------------- shared helpers

	/** the depth the rooms' own creatures scale with: the floor's, or on the far-off
	 *  special floors (the test floor sits at 86) the deepest real floor reached */
	public static int threat(){
		if (Dungeon.depth >= 1 && Dungeon.depth <= 26) return Dungeon.depth;
		return Math.max( 1, Math.min( 26, Statistics.deepestFloor ) );
	}

	/** re-rolls the item's quality with the rarity and type forced (null = rolled) */
	public static <T extends Item> T forceQuality( T item, Rarity rarity, ItemType type ){
		if (!Quality.eligible( item )) return item;
		item.quality = null;
		Quality.roll( item, rarity, type, 0 );
		return item;
	}

	/** the rarity one step above, stopping at legendary: exotic is never handed out */
	public static Rarity stepAbove( Rarity from ){
		int next = Math.min( Rarity.LEGENDARY.ordinal(), from.ordinal() + 1 );
		return Rarity.values()[next];
	}

	// ---------------------------------------------------------------- the collector

	public static void addHeat( int amount ){
		heat = Math.max( 0, heat + amount );
	}

	public static void paid( int gold ){
		goldSpent += Math.max( 0, gold );
	}

	/** the collector is dead: the debt is settled, and what it carried is on the floor */
	public static void settle(){
		heat = 0;
		goldSpent = 0;
	}

	/** Called from Dungeon.switchLevel before the actors are initialised, as fallen
	 *  monsters are: a hero carrying too much heat finds the collector already on the
	 *  floor they walk onto. One per floor, and only where a hunt can be run at all. */
	public static void onArrive( Level level ){
		if (heat < HEAT_LIMIT || Dungeon.branch != 0 || !(level instanceof RegularLevel)) return;
		for (Mob m : level.mobs){
			if (m instanceof Collector) return;
		}
		Collector collector = new Collector();
		int cell = level.randomRespawnCell( collector );
		if (cell == -1) return;
		collector.pos = cell;
		level.mobs.add( collector );
	}

	// ---------------------------------------------------------------- save

	private static final String QUEUE       = "warped_rooms";
	private static final String NEXT_SLOT   = "warped_next_slot";
	private static final String LAST_DEPTH  = "warped_last_slot_depth";
	private static final String LAST_ROOM   = "warped_last_slot_room";
	private static final String MARKETS     = "warped_market_depths";
	private static final String HEAT        = "warped_market_heat";
	private static final String SPENT       = "warped_market_spent";
	private static final String BANNED      = "warped_market_banned";

	public static void storeInBundle( Bundle bundle ){
		bundle.put( QUEUE, runQueue.toArray( new Class[0] ) );
		bundle.put( NEXT_SLOT, nextSlotDepth );
		bundle.put( LAST_DEPTH, lastSlotDepth );
		if (lastSlotRoom != null) bundle.put( LAST_ROOM, new Class[]{ lastSlotRoom } );
		bundle.put( MARKETS, marketDepth );
		bundle.put( HEAT, heat );
		bundle.put( SPENT, goldSpent );
		bundle.put( BANNED, banned );
	}

	@SuppressWarnings("unchecked")
	public static void restoreFromBundle( Bundle bundle ){
		//a run saved before the slot existed simply starts its queue now
		if (!bundle.contains( QUEUE )){
			initForRun();
			return;
		}
		runQueue = new ArrayList<>();
		for (Class<?> type : bundle.getClassArray( QUEUE )){
			if (type != null) runQueue.add( (Class<? extends Room>) type );
		}
		//rooms added to the slot after this run began join the back of its queue
		for (Class<? extends Room> type : SLOT_ROOMS){
			if (!runQueue.contains( type )) runQueue.add( type );
		}
		nextSlotDepth = bundle.getInt( NEXT_SLOT );
		lastSlotDepth = bundle.getInt( LAST_DEPTH );
		lastSlotRoom = null;
		if (bundle.contains( LAST_ROOM )){
			Class<?>[] last = bundle.getClassArray( LAST_ROOM );
			if (last.length > 0 && last[0] != null) lastSlotRoom = (Class<? extends Room>) last[0];
		}
		int[] markets = bundle.getIntArray( MARKETS );
		if (markets != null && markets.length == marketDepth.length) marketDepth = markets;
		heat = bundle.getInt( HEAT );
		goldSpent = bundle.getInt( SPENT );
		banned = bundle.getBoolean( BANNED );
	}
}
