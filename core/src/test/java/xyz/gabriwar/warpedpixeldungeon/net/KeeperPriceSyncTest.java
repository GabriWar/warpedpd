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

package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A co-op guest is quoted the host's own prices: the host ships each keeper's price factor with
 * him ("pf": a rescued caravan's markdown, a saved village's, the travelling market's markup),
 * in the full state and in a delta whenever it changes where he stands; the guest's stand-in
 * keeps it, and a look at a shelf prices the ware by the keeper of that shelf as the host picks
 * him - not by a villager standing nearer, and at the plain price from a host too old to say.
 */
public class KeeperPriceSyncTest {

	private static final long SEED = 0x5EED0F7EA7L;

	private Level level;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		//pricing a ware can reach the badges
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void save(){
		level = Dungeon.level;
		hero = Dungeon.hero;
		Actor.clear();
	}

	@After
	public void restore() throws Exception {
		TestParty.leave();
		Actor.clear();
		StateSerializer.resetDeltaTracking();
		Dungeon.level = level;
		Dungeon.hero = hero;
	}

	private static JSONObject entry( JSONArray mobs, Mob m ) throws Exception {
		for (int i = 0; mobs != null && i < mobs.length(); i++){
			if (mobs.getJSONObject( i ).getInt( "id" ) == m.id()) return mobs.getJSONObject( i );
		}
		return null;
	}

	@Test
	public void theHostShipsEachKeepersPrices() throws Exception {
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		PathFinder.setMapSize( w, h );
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window win = WindowGenerator.generate( SEED, 0, 0, 0, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, 0, 0, shift, GameCalendar.season(), win.terrain, w, h );
		assertNotNull( ow );
		Dungeon.level = ow;
		Hero me = new Hero();
		me.pos = w / 2 + (h / 2) * w;
		Dungeon.hero = me;
		Caravaneer stall = new Caravaneer();
		stall.pos = me.pos + 2;
		stall.pitched = WorldClock.day();
		Villager folk = new Villager();
		folk.pos = me.pos - 2;
		ow.mobs.add( stall );
		ow.mobs.add( folk );

		StateSerializer.resetDeltaTracking();
		JSONObject full = StateSerializer.serializeFullState();
		assertNotNull( full );
		assertEquals( 1.0, entry( full.getJSONArray( "mobs" ), stall ).getDouble( "pf" ), 0 );
		assertFalse( "a villager keeps no shelf", entry( full.getJSONArray( "mobs" ), folk ).has( "pf" ) );

		//nothing changed: the keeper is not sent again
		JSONObject quiet = StateSerializer.serializeDelta();
		assertTrue( quiet == null || entry( quiet.optJSONArray( "mobs" ), stall ) == null );

		//saved from his outlaws where he stands: his markdown goes out at once
		stall.ambush = Caravaneer.AMBUSH_SAVED;
		JSONObject delta = StateSerializer.serializeDelta();
		assertNotNull( delta );
		assertEquals( Caravaneer.SAVED_DISCOUNT, entry( delta.getJSONArray( "mobs" ), stall ).getDouble( "pf" ), 1e-6 );
		assertNull( entry( delta.getJSONArray( "mobs" ), folk ) );
	}

	@Test
	public void theGuestIsQuotedTheKeepersPrice() throws Exception {
		Level l = bareLevel( 20, 20 );
		Dungeon.level = l;
		Hero me = new Hero();
		me.pos = 2 + 2 * 20;
		Dungeon.hero = me;
		int shelf = 10 + 10 * 20;
		Item ware = new Food();
		Heap heap = l.drop( ware, shelf );
		heap.type = Heap.Type.FOR_SALE;
		int full = Shopkeeper.sellPrice( ware, me );
		assertTrue( full > 1 );

		//the host's keeper two cells off, a villager standing right by the shelf
		SpectatorReceiver.SpectatorMob keeper = new SpectatorReceiver.SpectatorMob();
		keeper.pos = shelf + 2;
		keeper.hostId = 7;
		SpectatorReceiver.applySleep( keeper, new JSONObject( "{\"id\":7,\"pf\":" + Caravaneer.SAVED_DISCOUNT + "}" ) );
		SpectatorReceiver.SpectatorMob folk = new SpectatorReceiver.SpectatorMob();
		folk.pos = shelf + 1;
		folk.hostId = 3;
		SpectatorReceiver.applySleep( folk, new JSONObject( "{\"id\":3}" ) );
		l.mobs.add( keeper );
		l.mobs.add( folk );
		assertEquals( -1f, folk.priceFactor, 0f );
		assertEquals( Caravaneer.SAVED_DISCOUNT, SpectatorReceiver.priceFactorNear( l, shelf ), 1e-6f );

		//a game of one has real keepers only: the stand-ins are not asked
		assertEquals( full, Shopkeeper.shelfPrice( ware, me, shelf ) );

		TestParty.guest();
		int saved = Shopkeeper.discounted( full, Caravaneer.SAVED_DISCOUNT );
		assertTrue( saved < full );
		assertEquals( saved, Shopkeeper.shelfPrice( ware, me, shelf ) );
		assertEquals( Messages.get( Heap.class, "for_sale", saved, ware.title() ), heap.title() );

		//the markdown over (the delta brings his factor back to one), and a keeper too far off
		SpectatorReceiver.applySleep( keeper, new JSONObject( "{\"id\":7,\"pf\":1.0}" ) );
		assertEquals( full, Shopkeeper.shelfPrice( ware, me, shelf ) );
		SpectatorReceiver.applySleep( keeper, new JSONObject( "{\"id\":7,\"pf\":1.25}" ) );
		assertEquals( Shopkeeper.discounted( full, 1.25f ), Shopkeeper.shelfPrice( ware, me, shelf ) );
		keeper.pos = shelf + 4;
		assertEquals( full, Shopkeeper.shelfPrice( ware, me, shelf ) );
		//a host too old to ship prices: the plain price, as before
		keeper.pos = shelf + 2;
		SpectatorReceiver.applySleep( keeper, new JSONObject( "{\"id\":7}" ) );
		assertEquals( full, Shopkeeper.shelfPrice( ware, me, shelf ) );
	}

	private static Level bareLevel( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( w, h );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		Painter.fill( l, 1, 1, w - 2, h - 2, Terrain.EMPTY );
		l.buildFlagMaps();
		return l;
	}
}
