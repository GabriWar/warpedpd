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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertFalse;

/**
 * A floor saved with a dead mirage on it (HP 0, its time long run out) brought it back into the
 * turn loop, where its act() found nothing to dissipate and returned true without spending, a
 * million times a second: the game hung the moment the hero got back to the floor. It now leaves.
 */
public class DeadMirageTest {

	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void up(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 8, 8 );
		l.mobs = new HashSet<>();
		Dungeon.level = l;
		Hero hero = new Hero();
		hero.HP = hero.HT = 20;
		Dungeon.hero = hero;
	}

	@After
	public void down(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//the mirage of the report's save: HP 0, ttl -848201, still on depth 4's mobs
	private static Mirage deadMirage(){
		Mirage m = new Mirage();
		m.pos = 9;
		Bundle b = new Bundle();
		m.storeInBundle( b );
		b.put( "HP", 0 );
		b.put( "ttl", -848201 );
		Mirage loaded = new Mirage();
		loaded.restoreFromBundle( b );
		return loaded;
	}

	@Test
	public void aDeadMirageLeavesTheFloorOnItsTurn(){
		Mirage m = deadMirage();
		Dungeon.level.mobs.add( m );
		Actor.add( m );

		m.act();

		assertFalse( "off the turn loop", Actor.all().contains( m ) );
		assertFalse( "off the floor", Dungeon.level.mobs.contains( m ) );
	}

	@Test
	public void aDeadMirageWithTimeLeftLeavesToo(){
		Mirage m = deadMirage();
		Bundle b = new Bundle();
		m.storeInBundle( b );
		b.put( "ttl", 30 );
		m = new Mirage();
		m.restoreFromBundle( b );
		Dungeon.level.mobs.add( m );
		Actor.add( m );

		m.act();

		assertFalse( Actor.all().contains( m ) );
		assertFalse( Dungeon.level.mobs.contains( m ) );
	}
}
