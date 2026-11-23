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

package xyz.gabriwar.warpedpixeldungeon.levels.traps;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A sheep pushed onto a change-sheep trap is swapped for the next sheep of the cycle, and the
 * new sheep is set down on the same cell through the floor's occupyCell, which fires the trap
 * under it again. Since every sheep of the cycle can be changed again, that never ended: a
 * player's game crashed with the stack overflowed (SokobanCastle, depth 52). One press, one change.
 */
public class ChangeSheepTrapTest {

	private static final int W = 8;
	private static final int TRAP = 3 + 3 * W;

	private static int presses;

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
		Dungeon.depth = 52;
		floor();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 1 + W;
		presses = 0;
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
	public void aSheepPushedOntoTheTrapChangesOnce(){
		SheepSokoban sheep = new SheepSokoban();
		sheep.pos = TRAP;
		sheep.sprite = stub();
		Dungeon.level.mobs.add( sheep );
		Actor.add( sheep );

		Dungeon.level.occupyCell( sheep );

		Char now = Actor.findChar( TRAP );
		assertTrue( "the plain sheep becomes a corner sheep, and stays one: " + now, now instanceof SheepSokobanCorner );
		assertEquals( 2, presses ); //the push, then the new sheep set down, which does nothing
	}

	@Test
	public void eachSheepOfTheCycleChangesOnce(){
		SheepSokobanCorner corner = new SheepSokobanCorner();
		corner.pos = TRAP;
		corner.sprite = stub();
		Dungeon.level.mobs.add( corner );
		Actor.add( corner );

		Dungeon.level.occupyCell( corner );

		assertTrue( Actor.findChar( TRAP ) instanceof SheepSokobanSwitch );
	}

	private static MobSprite stub(){
		return new MobSprite(){
			@Override public void place( int cell ){ }
			@Override public void update(){ }
			@Override public void killAndErase(){ }
		};
	}

	//a floor that fires the trap as the sokoban floors do (SokobanCastle.occupyCell): under any
	//sheep that steps onto it, spending it only after the change is done
	private static void floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public void occupyCell( Char ch ){
				presses++;
				//a live game scene puts a mob it adds on the actor list (GameScene.add); a test has none
				if (!Actor.all().contains( ch )) Actor.add( ch );
				if (ch.sprite == null) ch.sprite = stub();
				if (map[ch.pos] == Terrain.CHANGE_SHEEP_TRAP){
					ChangeSheepTrap cst = new ChangeSheepTrap();
					cst.pos = ch.pos;
					cst.activate();
					set( ch.pos, Terrain.INACTIVE_TRAP );
				}
				super.occupyCell( ch );
			}
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
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (W - 1) * W] = Terrain.WALL;
		for (int y = 0; y < W; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.map[TRAP] = Terrain.CHANGE_SHEEP_TRAP;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
	}
}
