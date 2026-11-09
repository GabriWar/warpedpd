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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfHaste;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Two rings of haste far upgraded (a report's overworld rogue wore +20 and +27, out on the roads)
 * made a step cost under a thousandth of a turn, which the clock rounds back to the whole turn it
 * started from: he walked
 * and marched to his waypoints with the game time stuck, nothing else ever got a turn, and the
 * freeze watchdog closed the game. However fast he is, every step now moves the clock.
 */
public class HasteClockTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private Hero hero;

	@Before
	public void up(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		hero = new Hero();
		hero.HP = hero.HT = 300;
		hero.pos = EnchantFxTest.at( 5, 6 );
		Dungeon.hero = hero;
		new HeroSprite( hero );
	}

	@After
	public void down(){
		rig.down();
	}

	private void wear( int level ){
		RingOfHaste ring = new RingOfHaste();
		ring.level( level );
		ring.activate( hero );
	}

	@Test
	public void everyStepOfAVeryFastHeroMovesTheClock(){
		wear( 30 );
		wear( 30 );
		float before = hero.cooldown();
		//what Hero.getCloser spends on each step
		for (int step = 0; step < 10; step++) hero.spend( 1f / hero.speed() );
		assertTrue( "ten steps from a whole turn: " + before + " -> " + hero.cooldown(), hero.cooldown() > before );
	}

	@Test
	public void aFastHeroIsStillFast(){
		wear( 5 );
		assertTrue( "+5 haste: " + hero.speed(), hero.speed() > 1.5f );
	}
}
