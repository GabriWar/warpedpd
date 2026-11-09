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

package xyz.gabriwar.warpedpixeldungeon.actors.hero;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSearching;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;

/**
 * A ring of searching widens a deliberate search by a cell every two levels: past +40 the radius
 * went beyond ShadowCaster's rounding table (20 cells) and the search button threw. The radius
 * stops at the table's edge.
 */
public class SearchRadiusTest {

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
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void update(){ }
			@Override public void operate( int cell ){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
		};
		sp.visible = false;
		return sp;
	}

	@Test
	public void aHugeRingOfSearchingStillSearches(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 64, 64 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		Painter.fill( l, 1, 1, 62, 62, Terrain.EMPTY );
		l.buildFlagMaps();
		Dungeon.level = l;

		Hero hero = new Hero();
		hero.HP = hero.HT = 20;
		hero.pos = 32 + 32 * 64;
		hero.sprite = stub();
		hero.fieldOfView = new boolean[l.length()];
		Dungeon.hero = hero;
		Actor.add( hero );

		RingOfSearching ring = new RingOfSearching();
		ring.level( 60 );
		ring.activate( hero );

		assertFalse( "nothing hidden here, and no crash", hero.search( true ) );
	}
}
