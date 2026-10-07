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

package xyz.gabriwar.warpedpixeldungeon.items.ore;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Ores;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The ore and gem items: one class per metal and stone, stacking only with their own kind and
 * saving as nothing but their class and count; priced and energized from the tables in Ores;
 * smelted at the troll's forge one crucible a day, his filings' gold on top for the rarest;
 * every line they or the forge say exists.
 */
public class OreItemsTest {

	private Hero savedHero;
	private int gold;

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
		gold = Dungeon.gold;
		Actor.clear();
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.hero = savedHero;
		Dungeon.gold = gold;
	}

	private static ArrayList<Item> all(){
		ArrayList<Item> out = new ArrayList<>();
		for (Ores.Kind k : Ores.Kind.values()) out.add( Reflection.newInstance( k.item ) );
		for (Ores.GemKind g : Ores.GemKind.values()) out.add( Reflection.newInstance( g.item ) );
		return out;
	}

	@Test
	public void kindsAndClassesMatch(){
		for (Ores.Kind k : Ores.Kind.values()) assertSame( k, Reflection.newInstance( k.item ).kind() );
		for (Ores.GemKind g : Ores.GemKind.values()) assertSame( g, Reflection.newInstance( g.item ).kind() );
	}

	@Test
	public void bundleRoundTrip() throws Exception {
		for (Item item : all()){
			item.quantity( 7 );
			Bundle b = new Bundle();
			b.put( "i", item );
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			assertTrue( Bundle.write( b, out ) );
			Item back = (Item) Bundle.read( new ByteArrayInputStream( out.toByteArray() ) ).get( "i" );
			assertSame( item.getClass(), back.getClass() );
			assertEquals( 7, back.quantity() );
			assertTrue( back.isIdentified() );
			assertFalse( back.isUpgradable() );
			assertTrue( back.stackable );
		}
		assertTrue( new IronOre().isSimilar( new IronOre() ) );
		assertFalse( new IronOre().isSimilar( new SilverOre() ) );
		assertFalse( new Garnet().isSimilar( new Amethyst() ) );
	}

	@Test
	public void pricesAndEnergy(){
		for (Ores.Kind k : Ores.Kind.values()){
			Ore o = Reflection.newInstance( k.item );
			o.quantity( 3 );
			assertEquals( 3 * k.price, o.value() );
			boolean extreme = k == Ores.Kind.DEEPSILVER || k == Ores.Kind.SKYIRON;
			assertEquals( extreme, o.energyVal() > 0 );
			//a whole batch never sells for more than a core does
			assertTrue( k.batch * k.price <= new MasterworkCore().value() );
		}
		for (Ores.GemKind g : Ores.GemKind.values()){
			Gem gem = Reflection.newInstance( g.item );
			gem.quantity( 2 );
			assertEquals( 2 * g.price, gem.value() );
			assertTrue( gem.energyVal() > 0 );
		}
		assertTrue( Ores.Kind.COPPER.price < Ores.Kind.IRON.price && Ores.Kind.IRON.price < Ores.Kind.SILVER.price
				&& Ores.Kind.SILVER.price < Ores.Kind.GOLD.price && Ores.Kind.GOLD.price < Ores.Kind.DEEPSILVER.price );
	}

	//a batch makes a core and the rarest pay their filings on top; the troll fires one crucible a
	//world day, and still knows it after a save
	@Test
	public void smeltOneCrucibleADay(){
		Hero hero = Dungeon.hero;
		Dungeon.gold = 0;
		int turn = Dungeon.cycleTurn;
		try {
			new IronOre().quantity( 107 ).collect( hero.belongings.backpack );
			assertTrue( Blacksmith2.smeltInto( hero, hero.belongings.getItem( IronOre.class ) ) );
			assertEquals( 57, hero.belongings.getItem( IronOre.class ).quantity() );
			assertEquals( 1, hero.belongings.getItem( MasterworkCore.class ).quantity() );
			assertEquals( "iron pays no filings", 0, Dungeon.gold );

			Ore deep = (Ore) new Deepsilver().quantity( 9 );
			deep.collect( hero.belongings.backpack );
			assertFalse( "short of a batch", Blacksmith2.smeltInto( hero, deep ) );
			assertEquals( 9, hero.belongings.getItem( Deepsilver.class ).quantity() );
			assertEquals( 1, hero.belongings.getItem( MasterworkCore.class ).quantity() );

			new Skyiron().quantity( 23 ).collect( hero.belongings.backpack );
			assertTrue( Blacksmith2.smeltInto( hero, hero.belongings.getItem( Skyiron.class ) ) );
			assertEquals( 13, hero.belongings.getItem( Skyiron.class ).quantity() );
			assertEquals( 2, hero.belongings.getItem( MasterworkCore.class ).quantity() );
			assertEquals( Ores.Kind.SKYIRON.bonus, Dungeon.gold );

			assertTrue( Blacksmith2.smeltInto( hero, hero.belongings.getItem( IronOre.class ) ) );
			new IronOre().quantity( 43 ).collect( hero.belongings.backpack );
			assertEquals( 50, hero.belongings.getItem( IronOre.class ).quantity() );
			assertTrue( Blacksmith2.smeltInto( hero, hero.belongings.getItem( IronOre.class ) ) );
			assertNull( "a stack used up is gone", hero.belongings.getItem( IronOre.class ) );
			assertEquals( 4, hero.belongings.getItem( MasterworkCore.class ).quantity() );

			Dungeon.cycleTurn = 3 * DayNightCycle.FULL_CYCLE + 10;
			hero.smeltDay = Integer.MIN_VALUE;
			new IronOre().quantity( 120 ).collect( hero.belongings.backpack );
			assertFalse( Blacksmith2.firedToday( hero ) );
			assertTrue( Blacksmith2.pourCrucible( hero, hero.belongings.getItem( IronOre.class ), false ) );
			assertTrue( Blacksmith2.firedToday( hero ) );
			assertFalse( "twice in a day", Blacksmith2.pourCrucible( hero, hero.belongings.getItem( IronOre.class ), false ) );
			assertEquals( 70, hero.belongings.getItem( IronOre.class ).quantity() );

			//the day is the hero's, not the smith's: the troll his commute makes anew each
			//morning (TownCommute), or a deep forge, is no second crucible
			assertFalse( "nor at a deep forge", Blacksmith2.pourCrucible( hero, hero.belongings.getItem( IronOre.class ), true ) );
			assertEquals( 70, hero.belongings.getItem( IronOre.class ).quantity() );
			Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
			assertFalse( "the next day", Blacksmith2.firedToday( hero ) );
			assertTrue( Blacksmith2.pourCrucible( hero, hero.belongings.getItem( IronOre.class ), false ) );
			assertEquals( 20, hero.belongings.getItem( IronOre.class ).quantity() );
			assertEquals( 6, hero.belongings.getItem( MasterworkCore.class ).quantity() );
		} finally {
			Dungeon.cycleTurn = turn;
		}
	}

	@Test
	public void stringsResolve(){
		ArrayList<String> lines = new ArrayList<>();
		for (Item item : all()){
			lines.add( item.name() );
			lines.add( item.desc() );
		}
		for (String k : new String[]{ "prised", "prised_two" }) lines.add( Messages.get( Ore.class, k, "iron ore" ) );
		lines.add( Messages.get( Ore.class, "you_now_have", 10, "iron ore" ) );
		lines.add( Messages.get( Ore.class, "smith", 50, 0 ) );
		lines.add( Messages.get( Ore.class, "smith_bonus", 10, 150 ) );
		lines.add( Messages.get( Ore.class, "energy", 5 ) );
		lines.add( Messages.get( Gem.class, "found", "garnet" ) );
		lines.add( Messages.get( Gem.class, "shattered" ) );
		lines.add( Messages.get( Gem.class, "uses", 2 ) );
		for (String k : new String[]{ "opt_smelt", "no_ore", "smelt_prompt" }) lines.add( Messages.get( Blacksmith2.class, k ) );
		lines.add( Messages.get( Blacksmith2.class, "smelt_short", 50, "iron ore", 12 ) );
		lines.add( Messages.get( Blacksmith2.class, "smelted", 50, "iron ore" ) );
		lines.add( Messages.get( Blacksmith2.class, "smelt_tomorrow" ) );
		lines.add( Messages.get( Blacksmith2.class, "smelted_bonus", 150 ) );
		lines.add( Messages.get( OverworldLevel.class, "vein", "iron ore" ) );
		lines.add( Messages.get( OverworldLevel.class, "vein_desc", "Iron ore" ) );
		lines.add( Messages.get( OverworldLevel.class, "crystal_desc" ) );
		for (String l : lines){
			assertFalse( l, l.contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( l, l.contains( "%" ) );
		}
	}
}
