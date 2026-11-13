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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChampionEnemy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;

//an albino piranha out of water died through damage( HT ), which a champion's damage reduction
//(a giant takes a fifth) let it live through: it then acted on land for free, turn after turn,
//holding the game's clock still forever. Out of water it dies, champion or not, as Shattered's
//piranha does
public class PiranhaOnLandTest {

	private static final int W = 8, H = 8;

	private int depth;
	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = 10;
		Dungeon.hero = new Hero();
		floor();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void aGiantPiranhaOnLandDies(){
		diesOnLand( new AlbinoPiranha(), ChampionEnemy.Giant.class );
	}

	@Test
	public void aBlessedShrinePiranhaOnLandDies(){
		diesOnLand( new ShrinePiranha(), ChampionEnemy.Blessed.class );
	}

	private static void diesOnLand( AlbinoPiranha fish, Class<? extends ChampionEnemy> champion ){
		fish.pos = 3 + 3 * W;
		Dungeon.level.mobs.add( fish );
		Actor.add( fish );
		Buff.affect( fish, champion );
		fish.act();
		assertFalse( "out of water it dies, " + champion.getSimpleName() + " or not", fish.isAlive() );
	}

	//dry floor walled round
	private static void floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, H );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (H - 1) * W] = Terrain.WALL;
		for (int y = 0; y < H; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, H );
		Dungeon.level = l;
	}
}
