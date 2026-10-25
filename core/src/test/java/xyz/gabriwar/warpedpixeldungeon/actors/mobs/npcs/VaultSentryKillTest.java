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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The vault's scanning sentry and laser, spawned bare as the debug menu's mob picker makes them (no
 * vault room handing them a pattern): a hero beside one, one hit kill on, swinging and throwing at
 * it, the sentry taking its turns and the level saved, crashed the game on the sentry's first turn
 * (its scan pattern was null). Now a bare one scans or fires as the vault's cross room sets them.
 */
public class VaultSentryKillTest {

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
		Dungeon.debugOneHitKill = false;
		Actor.clear();
		rig.down();
	}

	private Hero hero( int cell ){
		Hero h = new Hero();
		h.HP = h.HT = 9999;
		h.STR = 20;
		//(an interrupt wants a game scene's cell selector)
		h.damageInterrupt = false;
		h.pos = cell;
		CharSprite s = new RatSprite();
		s.ch = h;
		h.sprite = s;
		new Group().add( s );
		s.place( cell );
		Dungeon.hero = h;
		Actor.add( h );
		return h;
	}

	//as WndDebugPicker.selectCellForMob makes it: a bare instance, a cell, its debug state
	private <M extends Mob> M spawned( Class<M> cls, int cell ){
		M m = Reflection.newInstance( cls );
		m.pos = cell;
		m.state = m.debugSpawnState();
		CharSprite s = m.sprite();
		s.ch = m;
		m.sprite = s;
		rig.stage.add( s );
		s.place( cell );
		Actor.add( m );
		Dungeon.level.mobs.add( m );
		return m;
	}

	private static Bundle saved( Mob m ){
		Bundle b = new Bundle();
		m.storeInBundle( b );
		return b;
	}

	@Test
	public void aSpawnedScanningSentryTakesItsTurnsUnderOneHitKill(){
		Dungeon.debugOneHitKill = true;
		Hero h = hero( at( 5, 6 ) );
		VaultSentry v = spawned( VaultSentry.class, at( 6, 6 ) );

		//the hero's blows and throws find it invulnerable, as the vault's sentries are
		assertFalse( h.attack( v ) );
		h.belongings.thrownWeapon = new ThrowingKnife();
		assertFalse( h.attack( v ) );
		h.belongings.thrownWeapon = null;
		assertTrue( v.isAlive() );

		//its turns: a scan every few, the hero beside it on the west zapped by the first one
		for (int i = 0; i < 8; i++) v.act();
		assertNotNull( v.scanDirs );
		assertEquals( 4, v.scanDirs.length );
		assertTrue( "the scan reached the hero beside it", h.HP < h.HT );

		VaultSentry back = new VaultSentry();
		back.restoreFromBundle( saved( v ) );
		assertEquals( 4, back.scanDirs.length );
		assertEquals( v.scanDirIdx, back.scanDirIdx );
	}

	@Test
	public void aSpawnedSentrySavesBeforeItsFirstTurn(){
		hero( at( 5, 6 ) );
		VaultSentry v = spawned( VaultSentry.class, at( 6, 6 ) );
		VaultSentry back = new VaultSentry();
		back.restoreFromBundle( saved( v ) );
		assertEquals( 4, back.scanDirs.length );
	}

	@Test
	public void aSpawnedLaserSentryTakesItsTurns(){
		Dungeon.debugOneHitKill = true;
		hero( at( 2, 2 ) );
		VaultLaser l = spawned( VaultLaser.class, at( 6, 6 ) );
		for (int i = 0; i < 12; i++) l.act();
		assertNotNull( l.laserDirs );
		assertEquals( 4, l.laserDirs.length );

		VaultLaser back = new VaultLaser();
		back.restoreFromBundle( saved( l ) );
		assertEquals( 4, back.laserDirs.length );
	}
}
