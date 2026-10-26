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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.FallenMob;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import com.watabou.utils.Callback;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

//a summoning trap sets its monsters down with ScrollOfTeleportation.appear, and one set down
//over a chasm falls through to the floor below as it lands, sprite and all
public class AppearOverAChasmTest {

	private static final int W = 12;

	private int depth, branch;
	private Level saved;
	private Hero savedHero;
	private SparseArray<ArrayList<FallenMob>> fallen;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		fallen = Dungeon.fallenMobs;
		Dungeon.fallenMobs = new SparseArray<>();
		Actor.clear();
		//a dungeon floor with a floor under it
		Dungeon.depth = 2;
		Dungeon.branch = 0;
		Hero hero = new Hero();
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		Dungeon.fallenMobs = fallen;
	}

	private static MobSprite stub(){
		MobSprite sp = new MobSprite(){
			@Override public void fall( Callback landed ){ }
			@Override public void place( int cell ){ }
			@Override public void update(){ }
			@Override public void interruptMotion(){ }
		};
		sp.visible = false;
		return sp;
	}

	//a 12x12 floor in a ring of rock, the column x = 8 a chasm; the hero far off in a corner
	private static Level ledge(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
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
		Painter.fill( l, 0, 0, W, W, Terrain.WALL );
		Painter.fill( l, 1, 1, W - 2, W - 2, Terrain.EMPTY );
		for (int y = 1; y < W - 1; y++) l.map[8 + y * W] = Terrain.CHASM;
		l.buildFlagMaps();
		Dungeon.level = l;
		Dungeon.hero.pos = 1 + 10 * W;
		return l;
	}

	@Test
	public void aMonsterSetDownOverAChasmFallsThroughOnce(){
		Level l = ledge();
		Rat rat = new Rat();
		int pit = 8 + 5 * W;
		rat.pos = pit;
		rat.sprite = stub();
		l.mobs.add( rat );
		Actor.add( rat );

		ScrollOfTeleportation.appear( rat, pit );
		assertNull( "gone below, sprite and all", rat.sprite );
		assertFalse( l.mobs.contains( rat ) );
		assertEquals( "it lands on the floor below once", 1, Dungeon.fallenMobs.get( Dungeon.chasmKey( 3 ) ).size() );
	}
}
