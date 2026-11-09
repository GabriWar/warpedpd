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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

/**
 * A shocking weapon's lightning arcs through characters that have no sprite (one torn down,
 * given back on its next act) - the one struck too - and shocks them, drawing nothing for them.
 * It crashed on the first.
 */
public class ShockingNoSpriteTest {

	private Level savedLevel;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		Actor.clear();
		Level level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( 10, 10 );
		Dungeon.level = level;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
	}

	@Test
	public void theLightningShocksCharsWithNoSprite(){
		Rat attacker = new Rat(), struck = new Rat(), beside = new Rat();
		attacker.pos = 43;
		struck.pos = 44;
		beside.pos = 45;
		Actor.add( attacker );
		Actor.add( struck );
		Actor.add( beside );

		ArrayList<Char> affected = new ArrayList<>();
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		Shocking.arc( attacker, struck, 2, affected, arcs );
		assertTrue( affected.contains( beside ) );
		assertTrue( arcs.isEmpty() );
	}
}
