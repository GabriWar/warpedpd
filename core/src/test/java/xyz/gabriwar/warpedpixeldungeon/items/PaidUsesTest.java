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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//what a player pays to use a thing: the spider charm's health never the last of it, the
//minigun's and the technology shotgun's rounds a bullet each, and both fired only in hand
public class PaidUsesTest {

	private static boolean standIn;
	private EnchantFxTest.Rig rig;
	private int bullets;

	@BeforeClass
	public static void boot() throws Exception {
		EnchantFxTest.boot();
		//an item's action cancels the scene's cell selector first: headless a bare one stands in
		//(as PieceFxTest's)
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		if (sel.get( null ) == null){
			sel.set( null, bare( CellSelector.class ) );
			standIn = true;
		}
	}

	@AfterClass
	public static void unboot() throws Exception {
		if (!standIn) return;
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		sel.set( null, null );
	}

	@SuppressWarnings("unchecked")
	private static <T> T bare( Class<T> c ){
		try {
			Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
			u.setAccessible( true );
			Object unsafe = u.get( null );
			return (T) unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, c );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Before
	public void setUp(){
		bullets = Dungeon.bullet;
		rig = new EnchantFxTest.Rig();
		rig.up();
		Actor.clear();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.hero = null;
		Dungeon.bullet = bullets;
		rig.down();
	}

	private Hero hero(){
		Hero h = new Hero();
		h.HP = h.HT = 100;
		h.STR = 20;
		h.damageInterrupt = false;
		h.pos = Dungeon.level.width() + 1;
		CharSprite s = new RatSprite();
		s.ch = h;
		h.sprite = s;
		rig.stage.add( s );
		Dungeon.hero = h;
		Actor.add( h );
		return h;
	}

	@Test
	public void theSpiderCharmNeverTakesTheLastOfALife(){
		Hero h = hero();
		h.HP = h.HT / 4;
		new SpiderCharm().execute( h, SpiderCharm.AC_USE );
		assertTrue( h.isAlive() );
		assertEquals( "nothing paid", h.HT / 4, h.HP );

		h.HP = h.HT;
		new SpiderCharm().execute( h, SpiderCharm.AC_USE );
		assertEquals( "a quarter paid when it can be", h.HT - h.HT / 4, h.HP );
	}

	@Test
	public void theMinigunReloadsABulletARoundAndOnlyInHand(){
		Hero h = hero();
		MiniGun gun = new MiniGun();
		Dungeon.bullet = 10;
		gun.execute( h, MiniGun.AC_RELOAD );
		assertEquals( "not in hand: nothing loaded", 0, gun.charge );
		assertEquals( 10, Dungeon.bullet );
		assertFalse( gun.actions( h ).contains( MiniGun.AC_SHOOT ) );

		h.belongings.weapon = gun;
		assertTrue( gun.actions( h ).contains( MiniGun.AC_SHOOT ) );
		Dungeon.bullet = 2;
		gun.execute( h, MiniGun.AC_RELOAD );
		assertEquals( "as many rounds as there are bullets", 2, gun.charge );
		assertEquals( 0, Dungeon.bullet );

		gun.execute( h, MiniGun.AC_RELOAD );
		assertEquals( "none left: nothing loaded", 2, gun.charge );
	}

	@Test
	public void theTechnologyShotgunReloadsABulletARoundAndOnlyInHand(){
		Hero h = hero();
		ShootGun gun = new ShootGun();
		Dungeon.bullet = 10;
		gun.execute( h, ShootGun.AC_SHOOT );
		assertEquals( "not in hand: nothing loaded", 0, gun.charge );
		assertEquals( 10, Dungeon.bullet );

		h.belongings.weapon = gun;
		gun.execute( h, ShootGun.AC_SHOOT );
		assertEquals( "an empty magazine reloads in full", gun.fullcharge, gun.charge );
		assertEquals( 10 - gun.fullcharge, Dungeon.bullet );
	}
}
