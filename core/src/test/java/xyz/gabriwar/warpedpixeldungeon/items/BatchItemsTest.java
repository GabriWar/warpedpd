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

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWater;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BatchItemsTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void classArmorKeepsTheOldArmorsRarityAndType(){
		PlateArmor plate = new PlateArmor();
		Quality.roll( plate, Rarity.values()[Rarity.values().length - 1], ItemType.ALPHA, 0 );
		Hero hero = new Hero();
		hero.heroClass = HeroClass.ROGUE;
		ClassArmor garb = ClassArmor.upgrade( hero, plate );
		assertEquals( Quality.Family.ARMOR, Quality.family( garb ) );
		assertNotNull( garb.quality );
		assertTrue( garb.quality != plate.quality );
		assertEquals( plate.quality.rarity, garb.quality.rarity );
		assertEquals( ItemType.ALPHA, garb.quality.type );
		assertEquals( plate.quality.lines, garb.quality.lines );
	}

	@Test
	public void theBottleOfWaterIsAlwaysKnownInItsOwnBottle(){
		PotionOfWater water = new PotionOfWater();
		assertTrue( water.isKnown() );
		assertTrue( water.isIdentified() );
		assertEquals( ItemSpriteSheet.BOTTLE_OF_WATER, water.image() );
		assertEquals( "bottle of water", water.name() );
	}

	@Test
	public void theAlchemyPotFillsAnEmptyBottle(){
		ArrayList<Item> in = new ArrayList<>();
		in.add( new EmptyBottle() );
		ArrayList<Recipe> found = Recipe.findRecipes( in );
		assertEquals( 1, found.size() );
		Recipe r = found.get( 0 );
		assertTrue( r.sampleOutput( in ) instanceof PotionOfWater );
		assertEquals( 0, r.cost( in ) );
	}

	@Test
	public void theSatchelTakesTheBuildersTool(){
		assertTrue( new BuildersSatchel().canHold( new BuildersTool() ) );
	}
}
