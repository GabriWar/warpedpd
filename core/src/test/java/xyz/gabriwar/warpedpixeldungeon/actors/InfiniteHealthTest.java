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

package xyz.gabriwar.warpedpixeldungeon.actors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;

import static org.junit.Assert.*;

/**
 * The debug window's infinite health. It used to snap HP back to full from inside isAlive(),
 * which the render thread calls every frame. Health now comes back on the actor thread, right
 * after the blow lands: the blow still shows its whole number, but nothing after it (the red
 * flash, the low-health sound, a Potion of Healing drunk by auto-heal) treats the hero as
 * nearly dead.
 */
public class InfiniteHealthTest {

	@BeforeClass public static void loadAssets(){
		AllItemsTest.titleScreen();
	}

	private Hero previousHero;
	private Hero hero;

	@Before public void setUp(){
		previousHero = Dungeon.hero;
		hero = new Hero();
		Dungeon.hero = hero;
	}

	@After public void tearDown(){
		Dungeon.hero = previousHero;
	}

	//the number shown is the blow's own, as before (a cap left it at HP - 1)
	@Test
	public void theBlowKeepsItsWholeNumber(){
		hero.debugInfiniteHealth = true;
		hero.HP = 10;
		assertEquals( 50, hero.applyHealthHold( 50 ) );
	}

	@Test
	public void withoutItBlowsAreWhole(){
		hero.HP = 10;
		assertEquals( 50, hero.applyHealthHold( 50 ) );
	}

	//the render thread asks every frame: asking must not change anything
	@Test
	public void askingWhetherTheHeroLivesWritesNothing(){
		hero.debugInfiniteHealth = true;
		hero.HP = 0;
		assertTrue( hero.isAlive() );
		assertEquals( 0, hero.HP );
		hero.HP = 7;
		assertTrue( hero.isAlive() );
		assertEquals( 7, hero.HP );
	}

	@Test
	public void aResolvedBlowLeavesFullHealth(){
		hero.debugInfiniteHealth = true;
		hero.damage( 1, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger() );
		assertEquals( hero.HT, hero.HP );
		assertTrue( hero.isAlive() );
	}

	//a blow ten times the hero's health: the health is back before the hero's own damage code
	//looks at it, so nothing flashes or interrupts for "serious damage" (either would fail here,
	//with no scene). The ordinary interrupt at the start of every blow is switched off for that
	@Test
	public void aHeavyBlowIsUndoneBeforeAnythingSeesIt(){
		hero.debugInfiniteHealth = true;
		hero.damageInterrupt = false;
		hero.damage( hero.HT * 10, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning() );
		assertEquals( hero.HT, hero.HP );
		assertTrue( hero.isAlive() );
	}

	@Test
	public void deathIsRefused(){
		hero.debugInfiniteHealth = true;
		hero.HP = 0;
		hero.die( null );
		assertTrue( hero.isAlive() );
		assertEquals( hero.HT, hero.HP );
	}
}
