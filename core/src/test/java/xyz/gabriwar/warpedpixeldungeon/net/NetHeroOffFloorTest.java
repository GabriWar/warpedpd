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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A co-op game crashed host and guest alike (3 reports) on a guest's hero standing on a cell of
 * another floor: cell 16631, on the 176x176 surface, read against a 1560-cell floor 1. A floor's
 * file is written as the party lands on it (Dungeon.switchLevel), before NetManager has moved the
 * guests over from the floor they left, so it holds them on that floor's cells until the next save;
 * a game loaded from it put them back on those cells: the host's own path-finding read the floor
 * at the guest's cell, and the guest's screen read the temperature there. A loaded hero holding a
 * cell its floor does not have is put beside the host; and a copy of a hero the host already has
 * in play is not another actor on the floor.
 */
public class NetHeroOffFloorTest {

	private static final int W = 10;
	private static final int HOST = 4 + 4 * W;
	//a surface cell
	private static final int OFF_FLOOR = 16631;

	/** A floor with nothing on it but what a test puts there. */
	public static class Floor extends Level {
		@Override protected boolean build(){ return true; }
		@Override protected void createMobs(){ }
		@Override protected void createItems(){ }
	}

	private Level savedLevel;
	private Hero savedHero;
	private int versionCode;
	private Hero host;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		versionCode = Game.versionCode;
		//a save this version writes
		Game.versionCode = WarpedPixelDungeon.v4_0_0;
		Actor.clear();
		host = new Hero();
		host.pos = HOST;
		Dungeon.hero = host;
	}

	@After
	public void tearDown() throws Exception {
		TestParty.leave();
		stashed().clear();
		Actor.clear();
		Game.versionCode = versionCode;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//the host's game, restarted, loads a floor saved with a guest still on a surface cell
	@Test
	public void aGuestLoadedOnAnotherFloorsCellStandsBesideTheHost() throws Exception {
		Hero guest = guest();
		Bundle save = savedWith( guest );
		TestParty.leave();
		mode().set( null, NetManager.NetMode.HOST );

		Level floor = load( save );
		NetManager.onGameLoaded();

		assertOnFloor( floor );
		Hero loaded = (Hero) stashed().get( "Zincero" );
		assertTrue( "beside the host", floor.adjacent( loaded.pos, HOST ) );
		//the host's own walk reads the floor at every char's cell
		Dungeon.findPassable( host, floor.passable, all( floor ), true );
	}

	//the guest still in play when the floor's file is loaded again
	@Test
	public void aCopyOfAGuestInPlayIsNoActor() throws Exception {
		Hero guest = guest();
		Bundle save = savedWith( guest );
		guest.pos = HOST + 1;

		Level floor = load( save );
		NetManager.onGameLoaded();

		assertOnFloor( floor );
		int heroes = 0;
		for (Char ch : Actor.chars()) if (ch instanceof Hero) heroes++;
		assertEquals( "the host and the guest in play", 2, heroes );
		assertTrue( Actor.chars().contains( guest ) );
		assertEquals( HOST + 1, guest.pos );
		Dungeon.findPassable( host, floor.passable, all( floor ), true );
	}

	private static Hero guest(){
		Hero guest = new Hero();
		guest.netOwnerName = "Zincero";
		Talent.initClassTalents( guest );
		return guest;
	}

	//the floor's file as Dungeon.switchLevel writes it: the guest in play, still on a surface cell
	private static Bundle savedWith( Hero guest ) throws Exception {
		guest.pos = OFF_FLOOR;
		TestParty.join( guest );
		Bundle save = new Bundle();
		save.put( "level", floor() );
		return save;
	}

	//loaded as Dungeon.loadLevel and switchLevel load it
	private Level load( Bundle save ){
		Actor.clear();
		Level floor = (Level) save.get( "level" );
		Dungeon.level = floor;
		PathFinder.setMapSize( W, W );
		Actor.add( host );
		return floor;
	}

	private static void assertOnFloor( Level floor ){
		for (Char ch : Actor.chars()){
			assertTrue( ch + " at " + ch.pos, ch.pos >= 0 && ch.pos < floor.length() );
		}
	}

	private static boolean[] all( Level floor ){
		boolean[] seen = new boolean[floor.length()];
		Arrays.fill( seen, true );
		return seen;
	}

	//an open floor walled round
	private static Level floor(){
		Level l = new Floor();
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		return l;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Hero> stashed() throws Exception {
		Field f = NetManager.class.getDeclaredField( "unclaimedHeroes" );
		f.setAccessible( true );
		return (Map<String, Hero>) f.get( null );
	}

	private static Field mode() throws Exception {
		Field f = NetManager.class.getDeclaredField( "mode" );
		f.setAccessible( true );
		return f;
	}
}
