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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//the dots over a creature's head show exactly when the hero's next blow would surprise it
public class UnawareDotsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private static Level level(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 9, 9 );
		Dungeon.level = l;
		Dungeon.depth = 3;
		Dungeon.hero = new Hero();
		Dungeon.hero.HP = Dungeon.hero.HT = 20;
		Dungeon.hero.pos = 4 + 4 * 9;
		return l;
	}

	private static <T extends Mob> T at( T m, Level l, boolean seesHero, boolean hasSeenHero ){
		m.pos = 2 + 4 * 9;
		m.fieldOfView = new boolean[l.length()];
		if (seesHero) Arrays.fill( m.fieldOfView, true );
		m.enemySeen = hasSeenHero;
		return m;
	}

	@Test
	public void aNeutralBeastThatHasNotSeenTheHeroShowsDots(){
		Level l = level();
		Bunny b = at( new Bunny(), l, true, false );
		assertTrue( b.showsUnaware() );
	}

	@Test
	public void aHunterThatLostSightOfTheHeroShowsDots(){
		Level l = level();
		Rat r = at( new Rat(), l, false, true );
		r.state = r.HUNTING;
		assertTrue( "behind a door it cannot see him", r.showsUnaware() );
		r.fieldOfView[Dungeon.hero.pos] = true;
		assertFalse( "seen and watched: no surprise, no dots", r.showsUnaware() );
	}

	@Test
	public void sleepersAlliesTownsfolkAndMimicsShowNone(){
		Level l = level();
		Rat asleep = at( new Rat(), l, false, false );
		asleep.state = asleep.SLEEPING;
		assertFalse( "asleep shows its zzz", asleep.showsUnaware() );

		Rat ally = at( new Rat(), l, false, false );
		ally.alignment = Char.Alignment.ALLY;
		assertFalse( ally.showsUnaware() );

		Villager v = at( new Villager(), l, false, false );
		assertFalse( "townsfolk never notice anyone", v.showsUnaware() );

		Mimic m = at( new Mimic(), l, false, false );
		assertFalse( "a disguised mimic is not given away", m.showsUnaware() );
	}
}
