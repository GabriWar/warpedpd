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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownsfolkSilent;
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

/**
 * Fallen leaves stopped the hero's walk on every turn that any mob in his sight stood on them:
 * a townsperson at their post, a monster asleep, his own pet at his heels. In the town, where the
 * folk stand all day where autumn's leaves drift, a tap to walk across it went one tile at a time.
 * The leaves rustle when a monster walks into them, and only then is the hero stopped.
 */
public class FallenLeavesRustleTest {

	private static final int W = 16;
	private static final int HERO = 2 + 2 * W;
	//a strip of leaves in the hero's sight, well away from him
	private static final int LEAVES = 8 + 8 * W;

	private static int interrupts;

	private int depth, branch;
	private Level savedLevel;
	private Hero savedHero;
	private FallenLeaves leaves;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = 97;
		Dungeon.branch = 0;
		floor();
		Hero hero = new Hero(){
			//what stops his walk; the real one wants a game scene's cell selector
			@Override public void interrupt(){ interrupts++; }
		};
		hero.pos = HERO;
		Dungeon.hero = hero;
		Actor.add( hero );
		interrupts = 0;
		for (int i = 0; i < 3; i++) leaves = Blob.seed( LEAVES + i, 100, FallenLeaves.class, Dungeon.level );
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void aTownspersonStandingInLeavesLetsTheHeroWalk(){
		stand( new TownsfolkSilent(), LEAVES );
		turns( 10 );
		assertEquals( 0, interrupts );
	}

	@Test
	public void aMonsterAsleepInLeavesLetsTheHeroWalk(){
		Rat rat = new Rat();
		rat.state = rat.SLEEPING;
		stand( rat, LEAVES );
		turns( 10 );
		assertEquals( 0, interrupts );
	}

	@Test
	public void hisOwnPetFollowingIntoLeavesLetsTheHeroWalk(){
		Rat pet = new Rat();
		pet.alignment = Char.Alignment.ALLY;
		stand( pet, LEAVES - 1 );
		pet.move( LEAVES, true );
		turns( 1 );
		pet.move( LEAVES + 1, true );
		turns( 1 );
		assertEquals( 0, interrupts );
	}

	@Test
	public void aTownspersonWalkingThroughLeavesLetsTheHeroWalk(){
		TownsfolkSilent folk = new TownsfolkSilent();
		stand( folk, LEAVES - 1 );
		folk.move( LEAVES, true );
		turns( 1 );
		assertEquals( 0, interrupts );
	}

	@Test
	public void aMonsterWalkingIntoLeavesIsHeard(){
		Rat rat = new Rat();
		stand( rat, LEAVES - 1 );
		turns( 1 );
		assertEquals( "off the leaves it makes no sound", 0, interrupts );
		rat.move( LEAVES, true );
		turns( 1 );
		assertEquals( "the step in rustles", 1, interrupts );
		turns( 5 );
		assertEquals( "standing there it is quiet again", 1, interrupts );
		rat.move( LEAVES + 1, true );
		turns( 1 );
		assertEquals( "and the next step rustles", 2, interrupts );
	}

	// ------------------------------------------------------------------ helpers

	//a mob that has looked about once (its field of view built), as any that has acted has
	private static void stand( Mob m, int cell ){
		m.pos = cell;
		m.fieldOfView = new boolean[Dungeon.level.length()];
		m.sprite = stub();
		Dungeon.level.mobs.add( m );
		Actor.add( m );
	}

	//the leaves' own turns
	private void turns( int n ){
		for (int i = 0; i < n; i++) leaves.act();
	}

	//a sprite that draws nothing
	private static MobSprite stub(){
		MobSprite sp = new MobSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void move( int from, int to ){ }
		};
		sp.visible = false;
		return sp;
	}

	//open floor walled round, all of it in the hero's sight
	private static void floor(){
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
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (W - 1) * W] = Terrain.WALL;
		for (int y = 0; y < W; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.buildFlagMaps();
		Arrays.fill( l.heroFOV, true );
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
	}
}
