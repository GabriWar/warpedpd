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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * An enchantment's proc on a struck char with no sprite: one the same blow already sent through a
 * chasm (a stun or a frost grounding a flier over one, a knockback onto one), which takes it off
 * the floor and lets its sprite go (Chasm.fallThrough) while the blow's later procs still run on
 * it. Chilling's splash crashed on a bat a Spirit Bow shot dropped into the caves; Blazing's and
 * Parasitic's bursts read the same sprite. Each still does what it does, drawing nothing for it.
 */
public class EnchantmentsNoSpriteTest {

	//enough strikes that each one procs (the rarest, Parasitic at +0, one in nine)
	private static final int STRIKES = 200;

	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		//a burn's damage is told against the hero's side (Char.damage)
		Dungeon.hero = new Hero();
		Level level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( 10, 10 );
		level.mobs = new HashSet<>();
		level.heaps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		Arrays.fill( level.map, Terrain.EMPTY );
		level.buildFlagMaps();
		Dungeon.level = level;
		Random.pushGenerator( 1234 );
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a struck rat off the floor with no sprite, sturdy enough to take every strike
	private static Rat struck(){
		Rat r = new Rat();
		r.pos = 44;
		r.HP = r.HT = 100_000;
		assertNull( r.sprite );
		return r;
	}

	private static void strike( Weapon.Enchantment e, Rat attacker, Rat defender ){
		Sword sword = new Sword();
		for (int i = 0; i < STRIKES; i++) e.proc( sword, attacker, defender, 10 );
	}

	@Test
	public void chillingChillsACharWithNoSprite(){
		Rat defender = struck();
		strike( new Chilling(), new Rat(), defender );
		assertNotNull( defender.buff( Chill.class ) );
	}

	@Test
	public void blazingBurnsACharWithNoSprite(){
		Rat defender = struck();
		strike( new Blazing(), new Rat(), defender );
		assertNotNull( defender.buff( Burning.class ) );
	}

	@Test
	public void parasiticSeedsACharWithNoSprite(){
		Rat defender = struck();
		strike( new Parasitic(), new Rat(), defender );
		assertNotNull( defender.buff( Parasitic.ParasiticSeed.class ) );
	}
}
