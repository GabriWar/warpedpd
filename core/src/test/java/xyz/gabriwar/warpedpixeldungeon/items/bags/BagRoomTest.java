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

package xyz.gabriwar.warpedpixeldungeon.items.bags;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import xyz.gabriwar.warpedpixeldungeon.items.StoneOre;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.BlueNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.GreenNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.NornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.OrangeNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.PurpleNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.YellowNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.TypeShifter;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Ores;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** How much each bag holds, what the builder's satchel takes, and how many slots the bag window lays out. */
public class BagRoomTest {

	private Hero savedHero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.hero = savedHero;
	}

	@Test
	public void everyBagHoldsFiveMore(){
		assertEquals( 49, new Bag().capacity() );
		Belongings.Backpack pack = Dungeon.hero.belongings.backpack;
		assertEquals( 49, pack.capacity() );
		for (Bag b : new Bag[]{ new VelvetPouch(), new ScrollHolder(), new PotionBandolier(),
				new MagicalHolster(), new FoodPouch(), new AnkhChain(), new KeyRing() }){
			assertEquals( b.getClass().getSimpleName(), 48, b.capacity() );
		}
		assertEquals( 30, new BuildersSatchel().capacity() );

		//a bag in the backpack takes a physical slot, and the backpack makes room for it
		assertTrue( new VelvetPouch().collect( pack ) );
		assertTrue( new BuildersSatchel().collect( pack ) );
		assertEquals( 51, pack.capacity() );
	}

	@Test
	public void theSatchelTakesWhatTheRockGives(){
		BuildersSatchel satchel = new BuildersSatchel();
		for (Ores.Kind k : Ores.Kind.values()){
			assertTrue( k.name(), satchel.canHold( Reflection.newInstance( k.item ) ) );
		}
		for (Ores.GemKind g : Ores.GemKind.values()){
			assertTrue( g.name(), satchel.canHold( Reflection.newInstance( g.item ) ) );
		}
		for (Item it : new Item[]{ new StoneOre(), new DarkGold(), new MasterworkCore(), new NornStone(),
				new BlueNornStone(), new GreenNornStone(), new OrangeNornStone(),
				new PurpleNornStone(), new YellowNornStone() }){
			assertTrue( it.getClass().getSimpleName(), satchel.canHold( it ) );
		}
		//what it held before still goes in
		assertTrue( satchel.canHold( new Log() ) );
		assertTrue( satchel.canHold( new PortableChest() ) );
		assertTrue( satchel.canHold( new BuildersTool() ) );
		//the type shifter is spent like a spell, and stays out
		assertFalse( satchel.canHold( new TypeShifter() ) );
		assertFalse( satchel.canHold( new Food() ) );
	}

	@Test
	public void oreGoesInTheSatchelAndSmeltsFromIt(){
		Hero hero = Dungeon.hero;
		BuildersSatchel satchel = new BuildersSatchel();
		assertTrue( satchel.collect( hero.belongings.backpack ) );

		assertTrue( new IronOre().quantity( Ores.Kind.IRON.batch ).collect( hero.belongings.backpack ) );
		IronOre ore = hero.belongings.getItem( IronOre.class );
		assertNotNull( ore );
		assertTrue( "in the satchel", satchel.items.contains( ore ) );
		assertFalse( "not loose in the pack", hero.belongings.backpack.items.contains( ore ) );

		assertTrue( Blacksmith2.smeltInto( hero, ore ) );
		assertEquals( null, hero.belongings.getItem( IronOre.class ) );
		MasterworkCore core = hero.belongings.getItem( MasterworkCore.class );
		assertNotNull( core );
		assertTrue( "the core joins the ore's bag", satchel.items.contains( core ) );
	}

	@Test
	public void anOldSaveLoadsItsOreIntoTheSatchel(){
		//a save from before the satchel took ore: the ore lies loose in the pack, after the satchel
		Belongings.Backpack old = new Belongings.Backpack();
		BuildersSatchel satchel = new BuildersSatchel();
		satchel.items.add( new Log().quantity( 4 ) );
		old.items.add( satchel );
		old.items.add( new IronOre().quantity( 7 ) );
		old.items.add( new Food() );

		Bundle b = new Bundle();
		b.put( "pack", old );
		Belongings.Backpack back = (Belongings.Backpack) b.get( "pack" );

		BuildersSatchel loaded = null;
		for (Item it : back.items) if (it instanceof BuildersSatchel) loaded = (BuildersSatchel) it;
		assertNotNull( loaded );
		//the satchel and the food in the pack; the logs and the ore in the satchel
		assertEquals( 2, back.items.size() );
		assertEquals( 2, loaded.items.size() );
		boolean oreInSatchel = false;
		for (Item it : loaded.items) if (it instanceof IronOre && it.quantity() == 7) oreInSatchel = true;
		assertTrue( oreInSatchel );
		for (Item it : back.items) assertFalse( it instanceof IronOre );
	}

	@Test
	public void theBagWindowLaysOutEverySlot(){
		Belongings stuff = Dungeon.hero.belongings;
		//the five worn items, then the backpack's 49
		assertEquals( 5 + 49, WndBag.gridSlots( stuff.backpack, stuff ) );

		VelvetPouch pouch = new VelvetPouch();
		BuildersSatchel satchel = new BuildersSatchel();
		assertTrue( pouch.collect( stuff.backpack ) );
		assertTrue( satchel.collect( stuff.backpack ) );
		//bags are tabs, not grid slots: the backpack's room does not change
		assertEquals( 5 + 49, WndBag.gridSlots( stuff.backpack, stuff ) );
		//a sub-bag shows itself first, then its room
		assertEquals( 5 + 1 + 48, WndBag.gridSlots( pouch, stuff ) );
		assertEquals( 5 + 1 + 30, WndBag.gridSlots( satchel, stuff ) );

		//the champion's second weapon leads the backpack's grid
		stuff.secondWep = new Dagger();
		assertEquals( 5 + 1 + 49, WndBag.gridSlots( stuff.backpack, stuff ) );
		stuff.secondWep = null;

		//an overfull bag (forced past its room on load) still shows all it holds
		for (int i = 0; i < 31; i++) satchel.items.add( new Log() );
		assertEquals( 5 + 1 + 31, WndBag.gridSlots( satchel, stuff ) );
		assertSame( satchel, stuff.getItem( BuildersSatchel.class ) );
	}
}
