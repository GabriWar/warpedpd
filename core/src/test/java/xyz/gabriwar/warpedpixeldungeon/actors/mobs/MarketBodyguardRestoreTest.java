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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A black market guard is read back with his floor (Dungeon.loadLevel), while Dungeon.level is
 * still null, and every buff he carries is put back on him then: one he took in a fight (a
 * fire, a corruption, a bleed) asked the market's cell for gas and threw, and the floor never
 * loaded again (players stuck on depths 2, 18 and 24). A buff he wears again on loading is no
 * new blow from the hero, and a turned guard comes back turned before his buffs do: a corrupted
 * one on the hero's side, and with the heat he wore.
 */
public class MarketBodyguardRestoreTest {

	private Hero hero;
	private Level level;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		hero = Dungeon.hero;
		level = Dungeon.level;
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		Dungeon.hero = hero;
		Dungeon.level = level;
		Actor.clear();
	}

	//the guard as Dungeon.loadLevel reads him: written to a save file's bytes, then read back
	//with no floor set yet
	private static MarketBodyguard reload( Bundle guard ) throws Exception {
		Bundle file = new Bundle();
		file.put( "guard", guard );
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		assertTrue( Bundle.write( file, bytes ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( bytes.toByteArray() ) );
		Dungeon.level = null;
		Actor.clear();
		return (MarketBodyguard) read.get( "guard" );
	}

	private static Bundle saved( MarketBodyguard guard ){
		Bundle b = new Bundle();
		b.put( "guard", guard );
		return b.getBundle( "guard" );
	}

	private static MarketBodyguard turned(){
		MarketBodyguard guard = new MarketBodyguard();
		guard.pos = 40;
		guard.turnHostile();
		return guard;
	}

	@Test
	public void aBurningGuardLoadsWithHisFloor() throws Exception {
		MarketBodyguard guard = turned();
		Buff.affect( guard, Burning.class ).reignite( guard, 8f );
		Buff.affect( guard, Heatstroke.class );
		assertNotNull( guard.buff( Burning.class ) );

		MarketBodyguard back = reload( saved( guard ) );
		assertNotNull( "still burning", back.buff( Burning.class ) );
		assertNotNull( "a turned guard's heat is his own, loaded or not", back.buff( Heatstroke.class ) );
		assertEquals( Char.Alignment.ENEMY, back.alignment );
	}

	@Test
	public void aGuardAtPeaceKeepsThePeaceWhenHisDebuffComesBack() throws Exception {
		//a gas laid it on him at his post and the market let it pass: he is at peace, paralysed
		MarketBodyguard guard = turned();
		Buff.affect( guard, Paralysis.class, 5f );
		Bundle b = saved( guard );
		b.put( "hostile", false );

		MarketBodyguard back = reload( b );
		assertNotNull( back.buff( Paralysis.class ) );
		assertEquals( "loading is not the hero's steel", Char.Alignment.NEUTRAL, back.alignment );
		Bundle again = new Bundle();
		back.storeInBundle( again );
		assertFalse( again.getBoolean( "hostile" ) );
	}

	@Test
	public void aCorruptedGuardComesBackOnTheHerosSide() throws Exception {
		MarketBodyguard guard = turned();
		Buff.affect( guard, Corruption.class );
		assertEquals( Char.Alignment.ALLY, guard.alignment );

		MarketBodyguard back = reload( saved( guard ) );
		assertNotNull( back.buff( Corruption.class ) );
		assertEquals( "the corruption still holds him", Char.Alignment.ALLY, back.alignment );
	}
}
