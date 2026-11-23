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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.levels.CityBossLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The Dwarf King met away from his arena (one put down on the world's surface by hand, ten at a
 * time): his summons cast the floor to his arena for its summoning spots and threw on his first
 * turn, every turn, and on every load of the save (6 reports, one player). His phase change took
 * him to the throne's cell of his arena's map, which on another floor is any cell, or none on a
 * small one. Off his arena he calls no subjects and keeps his place.
 */
public class DwarfKingOffArenaTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void up(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		Actor.clear();
		Dungeon.level.mobs = new LinkedHashSet<>();
		PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
	}

	@After
	public void down(){
		Actor.clear();
		rig.down();
	}

	private static void hero( int cell ){
		Hero h = new Hero();
		h.HP = h.HT = 9999;
		h.damageInterrupt = false;
		h.pos = cell;
		CharSprite s = new RatSprite();
		s.ch = h;
		h.sprite = s;
		new Group().add( s );
		s.place( cell );
		Dungeon.hero = h;
		Actor.add( h );
	}

	private DwarfKing king( int cell ){
		DwarfKing k = Reflection.newInstance( DwarfKing.class );
		k.pos = cell;
		k.state = k.WANDERING;
		CharSprite s = k.sprite();
		s.ch = k;
		k.sprite = s;
		rig.stage.add( s );
		s.place( cell );
		Actor.add( k );
		Dungeon.level.mobs.add( k );
		return k;
	}

	private static int phase( DwarfKing k ){
		try {
			Field f = DwarfKing.class.getDeclaredField( "phase" );
			f.setAccessible( true );
			return f.getInt( k );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private static void phase( DwarfKing k, int phase ){
		try {
			Field f = DwarfKing.class.getDeclaredField( "phase" );
			f.setAccessible( true );
			f.setInt( k, phase );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void offHisArenaHeTakesHisTurnsCallingNoOne(){
		hero( at( 2, 2 ) );
		DwarfKing k = king( at( 12, 12 ) );
		for (int i = 0; i < 5; i++) k.act();
		assertTrue( "no subject on its way", k.buffs( DwarfKing.Summoning.class ).isEmpty() );
	}

	//the reports' kings, saved on the throne in their second phase
	@Test
	public void offHisArenaHisSecondPhaseCallsNoOne(){
		hero( at( 2, 2 ) );
		DwarfKing k = king( at( 12, 12 ) );
		phase( k, 2 );
		for (int i = 0; i < 5; i++) k.act();
		assertTrue( "no subject on its way", k.buffs( DwarfKing.Summoning.class ).isEmpty() );
	}

	//this floor is 16 by 16: the arena's throne cell is not on it
	@Test
	public void offHisArenaHisPhaseChangeKeepsHisPlace(){
		assertTrue( CityBossLevel.throne >= Dungeon.level.length() );
		hero( at( 2, 2 ) );
		int cell = at( 12, 12 );
		DwarfKing k = king( cell );
		k.damage( k.HT - 40, Dungeon.hero );
		assertEquals( 2, phase( k ) );
		assertEquals( cell, k.pos );
	}
}
