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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** The journal's catalog shows every rarity and type a kind of item can have (WndInfoItem's
 *  browsing window): every kind in it with a rarity can be described at every tier and type,
 *  from the title screen too, without touching the game's Random. */
public class CatalogQualityTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	@Test
	public void everyCatalogKindDescribesEveryTierAndType(){
		Dungeon.hero = null;
		ArrayList<Catalog> catalogs = new ArrayList<>( Catalog.equipmentCatalogs );
		catalogs.addAll( Catalog.consumableCatalogs );
		int kinds = 0;
		for (Catalog c : catalogs){
			for (Class<?> cls : c.items()){
				if (!Item.class.isAssignableFrom( cls )) continue;
				Item item = (Item) Reflection.newInstance( cls );
				if (item == null || !Quality.eligible( item )) continue;

				Random.pushGenerator( 1234 );
				int before = Random.Int( 1000 );
				Random.popGenerator();
				Random.pushGenerator( 1234 );
				Quality.sample( item );
				String what = cls.getSimpleName();
				assertNotNull( what, item.quality );
				assertEquals( what, Rarity.COMMON, item.quality.rarity );
				assertEquals( what, ItemType.GAMMA, item.quality.type );

				ArrayList<String> texts = new ArrayList<>();
				if (Quality.hasRarity( item )){
					for (Rarity r : Rarity.values()) texts.add( item.quality.describeRarityTier( item, r ) );
				}
				for (ItemType t : ItemType.values()) texts.add( Quality.perkText( item, t ) );
				assertEquals( what + " drew from the game's Random", before, Random.Int( 1000 ) );
				Random.popGenerator();
				for (String s : texts){
					assertFalse( what + ": " + s, s.contains( Messages.NO_TEXT_FOUND ) );
					assertFalse( what + " shows a raw placeholder: " + s, s.matches( "(?s).*%\\d?\\$?[ds].*" ) );
				}
				kinds++;
			}
		}
		assertTrue( "the catalog has kinds with a rarity", kinds > 50 );
	}

	@Test
	public void aKindOutsideTheSystemIsLeftAlone(){
		Item gold = new xyz.gabriwar.warpedpixeldungeon.items.Gold();
		Quality.sample( gold );
		assertEquals( null, gold.quality );
	}
}
