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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * A living plant kept hunting an enemy that had left the floor alive (a mob that falls down a
 * chasm and lives is taken off it, its sprite with it) and struck at it: Mob.doAttack read the
 * missing sprite and crashed. It looks for another, as every other mob does (Mob.chooseEnemy).
 */
public class LivingPlantEnemyTest {

	private Level saved;
	private Hero savedHero;
	private int depth;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		depth = Dungeon.depth;
		Actor.clear();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		Dungeon.depth = depth;
	}

	@Test
	public void anEnemyGoneFromTheFloorIsNotHuntedOn(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 9, 9 );
		l.mobs = new HashSet<>();
		Dungeon.level = l;
		Dungeon.depth = 3;
		Hero hero = new Hero();
		hero.HP = hero.HT = 20;
		hero.pos = 4 + 4 * 9;
		Dungeon.hero = hero;
		Actor.add( hero );

		LivingPlant plant = new LivingPlant();
		plant.pos = 3 + 4 * 9;
		plant.state = plant.HUNTING;
		l.mobs.add( plant );
		Actor.add( plant );

		//alive and a step away, but off the floor and the actor list: it fell and lived below
		Rat gone = new Rat();
		gone.pos = 2 + 4 * 9;
		plant.enemy = gone;

		Char chosen = plant.chooseEnemy();
		assertNotSame( "not the rat that left", gone, chosen );
		assertSame( "the hero beside it instead", hero, chosen );
	}
}
