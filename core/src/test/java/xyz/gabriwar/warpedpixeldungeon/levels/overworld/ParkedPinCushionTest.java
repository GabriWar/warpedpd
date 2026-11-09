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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.PinCushion;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A mob leaving the window loses its buffs, and a PinCushion's detach dropped the darts stuck in
 * it on its cell: on a window jump no scene shows the level, the heap had no sprite and the
 * floor change crashed (4 reports on the heights). The darts are parked at the mob's world cell.
 */
public class ParkedPinCushionTest {

	private Level saved;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void theDartsInAParkedMobWaitAtItsWorldCell() throws Throwable {
		OverworldLevel ow = new OverworldLevel();
		ow.mobs = new HashSet<>();
		Dungeon.level = ow;
		Deer deer = new Deer();
		ow.mobs.add( deer );
		Buff.affect( deer, PinCushion.class ).stick( new Dart() );

		Method park = OverworldLevel.class.getDeclaredMethod( "park", Mob.class, int.class, int.class );
		park.setAccessible( true );
		try {
			park.invoke( ow, deer, 500, 600 );
		} catch (InvocationTargetException e){
			throw e.getCause();
		}

		assertNull( "the cushion went with the darts", deer.buff( PinCushion.class ) );
		assertTrue( "the deer itself waits in the store", ow.parked().contains( deer ) );

		Field f = OverworldLevel.class.getDeclaredField( "storedHeaps" );
		f.setAccessible( true );
		Heap parked = ((HashMap<Long, Heap>) f.get( ow )).get( OverworldLevel.worldKey( 500, 600 ) );
		assertNotNull( "a heap where the deer stood", parked );
		boolean dart = false;
		for (Item i : parked.items) dart |= i instanceof Dart;
		assertTrue( "the dart is in it", dart );
	}
}
