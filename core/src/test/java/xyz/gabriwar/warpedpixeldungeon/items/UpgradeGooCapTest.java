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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfWealth;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.noosa.Group;
import com.watabou.utils.Signal;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The upgrade goos, as in Sprouted, raise gear no further than +15 unless it is reinforced, but
 * their descriptions promised +1, +3 or +5 outright, so a violet goo on gear past +10 gave less
 * than the five it promised. On gear already at +15 or past it the goo did nothing, was used up
 * all the same and still said the gear looked better: a +28 ring of wealth stayed +28 under a
 * yellow goo. A goo that can add nothing now stays in the pack and says why.
 */
public class UpgradeGooCapTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private Hero savedHero;
	private Hero hero;

	@Before
	public void setUp(){
		savedHero = Dungeon.hero;
		hero = new Hero();
		hero.sprite = stub();
		Dungeon.hero = hero;
		Item.curUser = hero;
	}

	@After
	public void tearDown(){
		Dungeon.hero = savedHero;
		Item.curUser = null;
	}

	//a sprite that draws nothing: its sparkle goes to a group of its own, there being no game scene
	private static MobSprite stub(){
		MobSprite sp = new MobSprite(){
			@Override public void operate( int cell ){ }
			@Override public void update(){ }
		};
		sp.visible = false;
		new Group().add( sp );
		return sp;
	}

	private static RingOfWealth ring( int level, boolean reinforced ){
		RingOfWealth ring = new RingOfWealth();
		ring.upgrade( level );
		ring.reinforced = reinforced;
		return ring;
	}

	//the goo in the pack, applied to the item as the bag window hands it over; returns the log
	private ArrayList<String> apply( Item goo, Item target ){
		hero.belongings.backpack.items.add( goo );
		final ArrayList<String> lines = new ArrayList<>();
		Signal.Listener<String> l = lines::add;
		GLog.update.add( l );
		try {
			Field f = goo.getClass().getDeclaredField( "itemSelector" );
			f.setAccessible( true );
			WndBag.ItemSelector selector = (WndBag.ItemSelector) f.get( goo );
			assertTrue( selector.itemSelectable( target ) );
			selector.onSelect( target );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		} finally {
			GLog.update.remove( l );
		}
		return lines;
	}

	private static boolean logged( ArrayList<String> lines, String text ){
		for (String line : lines) if (line.endsWith( text )) return true;
		return false;
	}

	@Test
	public void yellowGooOnARingPastFifteenIsKeptAndSaysWhy(){
		RingOfWealth ring = ring( 28, false );
		UpgradeBlobYellow goo = new UpgradeBlobYellow();
		ArrayList<String> log = apply( goo, ring );

		assertEquals( 28, ring.trueLevel() );
		assertTrue( "the goo is not spent", hero.belongings.backpack.items.contains( goo ) );
		assertFalse( "no word of an upgrade that did not happen",
				logged( log, Messages.get( UpgradeBlobYellow.class, "upgraded", ring.name() ) ) );
		assertTrue( logged( log, Messages.get( UpgradeBlobYellow.class, "capped", ring.name() ) ) );
	}

	@Test
	public void violetGooOnARingPastFifteenIsKeptAndSaysWhy(){
		RingOfWealth ring = ring( 20, false );
		UpgradeBlobViolet goo = new UpgradeBlobViolet();
		ArrayList<String> log = apply( goo, ring );

		assertEquals( 20, ring.trueLevel() );
		assertTrue( "the goo is not spent", hero.belongings.backpack.items.contains( goo ) );
		assertFalse( logged( log, Messages.get( UpgradeBlobViolet.class, "upgraded", ring.name() ) ) );
		assertTrue( logged( log, Messages.get( UpgradeBlobViolet.class, "capped", ring.name() ) ) );
	}

	@Test
	public void redGooOnARingAtFifteenIsKept(){
		RingOfWealth ring = ring( 15, false );
		UpgradeBlobRed goo = new UpgradeBlobRed();
		apply( goo, ring );

		assertEquals( 15, ring.trueLevel() );
		assertTrue( hero.belongings.backpack.items.contains( goo ) );
	}

	@Test
	public void violetGooRaisesGearUpToFifteen(){
		RingOfWealth ring = ring( 12, false );
		UpgradeBlobViolet goo = new UpgradeBlobViolet();
		ArrayList<String> log = apply( goo, ring );

		assertEquals( 15, ring.trueLevel() );
		assertFalse( "the goo is spent", hero.belongings.backpack.items.contains( goo ) );
		assertTrue( logged( log, Messages.get( UpgradeBlobViolet.class, "upgraded", ring.name() ) ) );
	}

	@Test
	public void violetGooRaisesReinforcedGearByFivePastFifteen(){
		RingOfWealth ring = ring( 20, true );
		UpgradeBlobViolet goo = new UpgradeBlobViolet();
		apply( goo, ring );

		assertEquals( 25, ring.trueLevel() );
		assertFalse( "the goo is spent", hero.belongings.backpack.items.contains( goo ) );
	}
}
