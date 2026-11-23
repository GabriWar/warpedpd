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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
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

import static org.junit.Assert.assertEquals;

/**
 * A cursed wand's random wand that came up a wand of blink crashed the game (index -1) when its
 * bolt stopped on the caster's own cell: zapped straight into a wall beside him, a magic bolt
 * collides before it leaves his cell (Ballistica dist 0), and the blink backed off "one cell" from
 * the char it found there, himself, to path index -1. Such a blink leaves him where he stands.
 */
public class WandOfBlinkIntoWallTest {

	private static final int W = 8;
	//beside the west wall
	private static final int CELL = 1 + 3 * W;

	private int depth;
	private Level savedLevel;
	private Hero savedHero, savedUser;
	private Camera savedCam;
	private Hero caster;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		depth = Dungeon.depth;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedUser = (Hero) curUser().get( null );
		savedCam = Camera.main;
		Actor.clear();
		Dungeon.depth = 13;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		floor();
		caster = new Hero();
		caster.pos = CELL;
		//invisible: no fade-in on a sprite with no scene to tween in
		caster.invisible = 1;
		caster.sprite = new CharSprite(){
			@Override
			public Emitter emitter(){
				return new Emitter();
			}
		};
		new Group().add( caster.sprite );
		Actor.add( caster );
		Dungeon.hero = caster;
		curUser().set( null, caster );
	}

	@After
	public void tearDown() throws Exception {
		Actor.clear();
		curUser().set( null, savedUser );
		Dungeon.depth = depth;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Camera.main = savedCam;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void aBlinkIntoTheWallBesideHimLeavesHimWhereHeStands(){
		//as CursedWand.cursedZap builds it, aimed at the wall west of him
		Ballistica bolt = new Ballistica( CELL, CELL - 1, Ballistica.MAGIC_BOLT );
		assertEquals( "the bolt stops on his own cell", 0, (int) bolt.dist );

		new WandOfBlink().onZap( bolt );

		assertEquals( CELL, caster.pos );
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField( "curUser" );
		f.setAccessible( true );
		return f;
	}

	//an open floor walled round
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
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
	}
}
