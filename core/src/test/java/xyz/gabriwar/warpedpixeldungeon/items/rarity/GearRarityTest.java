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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CapeOfThorns;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfAccuracy;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.EyeOfNewt;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Rings, trinkets and artifacts roll a rarity with hero-wide lines, and descriptions colour
 *  the rarity, type and enchantment names. */
public class GearRarityTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	@Test
	public void ringsTrinketsAndArtifactsRollLinesFromTheirOwnPools(){
		for (Item item : new Item[]{ new RingOfAccuracy(), new EyeOfNewt(), new CapeOfThorns() }){
			Quality.roll( item, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
			Quality q = item.quality;
			String what = item.getClass().getSimpleName();
			assertTrue( what, Quality.hasRarity( item ) );
			assertFalse( what + " has no number of its own", Quality.hasPotency( item ) );
			assertEquals( what, Rarity.LEGENDARY, q.rarity );
			assertEquals( what, 2, q.lines.size() );
			for (RarityLine l : q.lines){
				assertTrue( what + " rolled " + l, l.fits( Quality.family( item ) ) );
				assertFalse( what + ": " + l, l.describe( item, 1f ).contains( Messages.NO_TEXT_FOUND ) );
			}
			assertEquals( what, 0f, Quality.potency( item ), 0f );
		}
	}

	@Test
	public void aWornRingsLinesAddToTheHerosStats(){
		Hero hero = new Hero();
		Dungeon.hero = hero;
		RingOfAccuracy ring = new RingOfAccuracy();
		Quality.roll( ring, Rarity.EXOTIC, ItemType.GAMMA, 0 );
		hero.belongings.ring = ring;
		Quality q = ring.quality;
		for (GearPerk.Stat stat : GearPerk.Stat.values()){
			float with = GearPerk.heroBonus( hero, stat );
			float lines = 0f;
			for (RarityLine l : q.lines) if (l.feeds( stat )) lines += q.value( l );
			ArrayList<RarityLine> kept = new ArrayList<>( q.lines );
			ArrayList<Float> rolls = new ArrayList<>( q.rolls );
			q.lines.clear();
			q.rolls.clear();
			float without = GearPerk.heroBonus( hero, stat );
			q.lines.addAll( kept );
			q.rolls.addAll( rolls );
			assertEquals( stat.name(), lines, with - without, 0.0001f );
		}
	}

	@Test
	public void descriptionsColourTheirNames(){
		RingOfAccuracy ring = new RingOfAccuracy();
		Quality.roll( ring, Rarity.RARE, ItemType.BETA, 0 );
		String text = ring.quality.describe( ring );
		assertTrue( text, text.contains( Messages.tint( Rarity.RARE.color, Rarity.RARE.title() ) ) );
		assertTrue( text, text.contains( Messages.tint( ItemType.BETA.color, ItemType.BETA.title() ) ) );

		Blazing blazing = new Blazing();
		assertEquals( Messages.tint( blazing.glowing().color, blazing.name() ), blazing.coloredName() );
		//a curse glows black: its name takes a readable dark red instead
		Annoying curse = new Annoying();
		assertEquals( Messages.tint( 0xB0405A, curse.name() ), curse.coloredName() );
	}
}
