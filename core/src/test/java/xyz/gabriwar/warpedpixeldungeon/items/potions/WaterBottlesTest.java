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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.EmptyBottle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.PotionBandolier;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Water bottles: water poured or thrown on one cell puts its fire out, drenches whoever stands
 * there and leaves a puddle only on bare ground; a drenched character's first flame only dries
 * it off; a potion from seeds takes a bottle from the pack, an empty one before a full one, and
 * none is brewed without one.
 */
public class WaterBottlesTest {

	private static final int W = 9, CENTER = 4 + 4 * W;

	private Level savedLevel;
	private Hero savedHero;
	private Level level;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( W, W );
		level.mobs = new HashSet<>();
		level.heaps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		Arrays.fill( level.map, Terrain.EMPTY );
		level.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = level;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = CENTER;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void waterLeavesAPuddleOnlyOnBareGround(){
		int grass = CENTER + 1, farm = CENTER - 1, door = CENTER + W;
		Level.set( grass, Terrain.GRASS, level );
		Level.set( farm, Terrain.FARMLAND, level );
		Level.set( door, Terrain.DOOR, level );

		PotionOfWater.douse( grass );
		PotionOfWater.douse( farm );
		PotionOfWater.douse( door );

		assertEquals( Terrain.WATER, level.map[grass] );
		assertTrue( level.water[grass] );
		assertEquals( "farmland keeps its soil", Terrain.FARMLAND, level.map[farm] );
		assertEquals( Terrain.DOOR, level.map[door] );
	}

	@Test
	public void waterPutsOutFireAndDrenches(){
		int cell = CENTER + 1;
		Blob fire = Blob.seed( cell, 5, Fire.class, level );
		level.blobs.put( Fire.class, fire );
		Hero hero = Dungeon.hero;
		hero.pos = cell;
		Actor.add( hero );
		Buff.affect( hero, Burning.class ).reignite( hero );
		assertNotNull( hero.buff( Burning.class ) );

		assertTrue( "a fire went out", PotionOfWater.douse( cell ) );
		assertEquals( 0, fire.cur[cell] );
		assertNotNull( hero.buff( Drenched.class ) );
		assertNull( "the soaking put him out", hero.buff( Burning.class ) );
	}

	@Test
	public void theFirstFlameOnTheDrenchedOnlyDriesThem(){
		Hero hero = Dungeon.hero;
		Buff.prolong( hero, Drenched.class, Drenched.DURATION );

		Buff.affect( hero, Burning.class ).reignite( hero );
		assertNull( hero.buff( Burning.class ) );
		assertNull( "dried off", hero.buff( Drenched.class ) );

		Buff.affect( hero, Burning.class ).reignite( hero );
		assertNotNull( "dry, the next one catches", hero.buff( Burning.class ) );
	}

	private static ArrayList<Item> seeds(){
		return new ArrayList<>( Arrays.asList( new Firebloom.Seed(), new Icecap.Seed(), new Sungrass.Seed() ) );
	}

	@Test
	public void aPotionFromSeedsTakesABottle(){
		Hero hero = Dungeon.hero;
		Potion.SeedToPotion recipe = new Potion.SeedToPotion();

		assertTrue( "the pot still shows the recipe", recipe.testIngredients( seeds() ) );
		assertNull( "no bottle, no potion", recipe.brew( seeds() ) );

		new PotionOfWater().collect( hero.belongings.backpack );
		new EmptyBottle().collect( hero.belongings.backpack );
		assertTrue( recipe.brew( seeds() ) instanceof Potion );
		assertNull( "the empty bottle went first", hero.belongings.getItem( EmptyBottle.class ) );
		assertNotNull( hero.belongings.getItem( PotionOfWater.class ) );

		assertTrue( recipe.brew( seeds() ) instanceof Potion );
		assertNull( "then the bottle of water", hero.belongings.getItem( PotionOfWater.class ) );

		PotionBandolier bandolier = new PotionBandolier();
		assertTrue( "the bandolier holds bottles", bandolier.canHold( new EmptyBottle() ) && bandolier.canHold( new PotionOfWater() ) );

		EmptyBottle.AnyBottle any = new EmptyBottle.AnyBottle();
		assertTrue( any.isSimilar( new EmptyBottle() ) && any.isSimilar( new PotionOfWater() ) );
	}
}
