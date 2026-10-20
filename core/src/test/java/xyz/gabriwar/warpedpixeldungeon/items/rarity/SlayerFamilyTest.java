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

import com.watabou.utils.Bundle;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FetidRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.lang.reflect.Field;
import java.util.EnumMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** The family a SLAYER line is sharpened against: who belongs to it, and how it is kept. */
public class SlayerFamilyTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	@Test
	public void everyFamilyHasMonstersInTheBestiary() throws Exception {
		Class<?> registry = Class.forName( "xyz.gabriwar.warpedpixeldungeon.journal.MobRegistry" );
		Field all = registry.getDeclaredField( "ALL" );
		all.setAccessible( true );
		EnumMap<SlayerFamily, Integer> members = new EnumMap<>( SlayerFamily.class );
		for (Class<?> cls : (Class<?>[]) all.get( null )){
			Object o;
			try {
				o = cls.getConstructor().newInstance();
			} catch (Throwable t){
				continue;
			}
			if (!(o instanceof Char)) continue;
			for (SlayerFamily f : SlayerFamily.values()){
				if (f.has( (Char) o )) members.merge( f, 1, Integer::sum );
			}
		}
		for (SlayerFamily f : SlayerFamily.values()){
			assertTrue( f + " has " + members.get( f ), members.getOrDefault( f, 0 ) >= 3 );
			assertFalse( f.title(), f.title().contains( Messages.NO_TEXT_FOUND ) );
		}
	}

	@Test
	public void theFamiliesSortTheUsualFoes(){
		assertTrue( SlayerFamily.BEASTS.has( new Rat() ) );
		assertFalse( SlayerFamily.HUMANOIDS.has( new Rat() ) );
		assertTrue( SlayerFamily.HUMANOIDS.has( new Gnoll() ) );
		assertFalse( SlayerFamily.BEASTS.has( new Gnoll() ) );
		assertTrue( SlayerFamily.UNDEAD.has( new Skeleton() ) );
		//bones are inorganic, but the undead are not built
		assertFalse( SlayerFamily.CONSTRUCTS.has( new Skeleton() ) );
		assertTrue( SlayerFamily.CONSTRUCTS.has( new ClayGolem() ) );
		//a rat (by its class) and a demon (by its nature) at once
		assertTrue( SlayerFamily.BEASTS.has( new FetidRat() ) );
		assertTrue( SlayerFamily.DEMONS.has( new FetidRat() ) );
		assertFalse( SlayerFamily.UNDEAD.has( null ) );
	}

	private static Quality slayer( Longsword sword, SlayerFamily f ){
		Quality q = new Quality();
		q.rarity = Rarity.RARE;
		q.lines.add( RarityLine.SLAYER );
		q.rolls.add( 0.7f );
		q.lines.add( RarityLine.ACCURACY );
		q.rolls.add( 0.6f );
		q.slayer = f;
		sword.quality = q;
		return q;
	}

	@Test
	public void aSlayerLineSavesItsFamilyAndDropsAnUnknownOne(){
		Longsword sword = new Longsword();
		Quality q = slayer( sword, SlayerFamily.HUMANOIDS );
		Quality copy = q.copy();
		assertEquals( q.lines, copy.lines );
		assertEquals( SlayerFamily.HUMANOIDS, copy.slayer );
		assertTrue( RarityLine.SLAYER.describe( sword, q.roll( RarityLine.SLAYER ) ).endsWith( "against humanoids" ) );

		//a family this build does not know: the line and its roll go, the rest stays
		Bundle b = new Bundle();
		q.storeInBundle( b );
		b.put( "slayer", "DRAGONS" );
		Quality loaded = new Quality();
		loaded.restoreFromBundle( b );
		assertEquals( 1, loaded.lines.size() );
		assertEquals( RarityLine.ACCURACY, loaded.lines.get( 0 ) );
		assertEquals( 0.6f, loaded.rolls.get( 0 ), 0f );
		assertNull( loaded.slayer );

		//no family at all: the same
		Quality bare = new Quality();
		bare.lines.add( RarityLine.SLAYER );
		bare.rolls.add( 0.5f );
		Bundle nb = new Bundle();
		bare.storeInBundle( nb );
		assertFalse( nb.contains( "slayer" ) );
		Quality none = new Quality();
		none.restoreFromBundle( nb );
		assertTrue( none.lines.isEmpty() );

		//a family is written only beside its line
		q.lines.remove( 0 );
		q.rolls.remove( 0 );
		Bundle without = new Bundle();
		q.storeInBundle( without );
		assertFalse( without.contains( "slayer" ) );
	}

	@Test
	public void sameQualityComparesTheFamily(){
		Longsword a = new Longsword(), b = new Longsword();
		slayer( a, SlayerFamily.UNDEAD );
		Quality qb = slayer( b, SlayerFamily.UNDEAD );
		assertTrue( Quality.sameQuality( a, b ) );
		qb.slayer = SlayerFamily.BEASTS;
		assertFalse( Quality.sameQuality( a, b ) );
		//a leftover family on a quality without the line counts for nothing
		a.quality.lines.remove( 0 );
		a.quality.rolls.remove( 0 );
		qb.lines.remove( 0 );
		qb.rolls.remove( 0 );
		assertTrue( Quality.sameQuality( a, b ) );
	}
}
