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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Reflection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Every enchantment's, glyph's and curse's look (EnchantLooks): each class of the game's lists and
 * the eight specials has exactly one row; the 78 codes are the ones pinned here (append only:
 * EnchantNet sends them, a guest of another build reads the same number); the Glowing each class
 * shows finds its row; every sting is one of the game's sounds.
 */
public class EnchantLooksTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	/** The codes, pinned: change a row's code and a guest of another build reads another look. */
	static final Object[][] CODES = {
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class, 0 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling.class, 1 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking.class, 2 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic.class, 3 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous.class, 4 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blocking.class, 5 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming.class, 6 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic.class, 7 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch.class, 8 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky.class, 9 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting.class, 10 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable.class, 11 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vorpal.class, 12 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting.class, 13 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Crystal.class, 14 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class, 15 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class, 16 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis.class, 17 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging.class, 18 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Parasitic.class, 19 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Horror.class, 20 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting.class, 21 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class, 22 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy.class, 23 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious.class, 24 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient.class, 25 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Luck.class, 26 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing.class, 27 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck.class, 28 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech.class, 29 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw.class, 30 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror.class, 31 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison.class, 32 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock.class, 33 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom.class, 34 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose.class, 35 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Poison.class, 36 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Adaptation.class, 37 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Affection.class, 38 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic.class, 39 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage.class, 40 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Bounce.class, 41 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone.class, 42 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage.class, 43 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal.class, 44 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Entanglement.class, 45 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Flow.class, 46 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirrorimage.class, 47 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirroring.class, 48 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Obfuscation.class, 49 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Phasing.class, 50 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Potential.class, 51 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Purity.class, 52 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Repulsion.class, 53 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Satisfying.class, 54 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Siphoning.class, 55 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone.class, 56 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Swiftness.class, 57 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns.class, 58 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity.class, 59 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying.class, 60 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Dazzling.class, 61 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing.class, 62 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Explosive.class, 63 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Friendly.class, 64 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized.class, 65 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Pressurized.class, 66 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial.class, 67 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wayward.class, 68 },
			{ xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wondrous.class, 69 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.AntiEntropy.class, 70 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Bulk.class, 71 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion.class, 72 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Displacement.class, 73 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Metabolism.class, 74 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Multiplicity.class, 75 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Overgrowth.class, 76 },
			{ xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Stench.class, 77 },
	};

	static final Class<?>[] SPECIALS = {
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech.class,
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw.class,
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck.class,
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror.class,
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison.class,
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock.class,
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom.class,
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose.class };

	//every class the game lists, the specials with them
	static List<Class<?>> every(){
		List<Class<?>> out = new ArrayList<>();
		for (Class<?>[] l : new Class<?>[][]{ Weapon.Enchantment.common, Weapon.Enchantment.uncommon, Weapon.Enchantment.rare,
				Weapon.Enchantment.curses, SPECIALS, Armor.Glyph.common, Armor.Glyph.uncommon, Armor.Glyph.rare, Armor.Glyph.curses }){
			for (Class<?> c : l) out.add( c );
		}
		return out;
	}

	@Test
	public void everyListedClassHasExactlyOneRow(){
		List<Class<?>> all = every();
		assertEquals( "the game lists 78", EnchantLooks.COUNT, new HashSet<>( all ).size() );
		assertEquals( "and none twice", all.size(), new HashSet<>( all ).size() );
		HashSet<Class<?>> rows = new HashSet<>();
		for (EnchantLook l : EnchantLooks.all()){
			assertNotNull( l );
			assertTrue( "a class twice: " + l, rows.add( l.of ) );
		}
		for (Class<?> c : all){
			EnchantLook l = EnchantLooks.of( c );
			assertNotNull( "no look for " + c.getSimpleName(), l );
			assertSame( c, l.of );
			assertTrue( c.getSimpleName() + " in the table", rows.contains( c ) );
		}
		assertEquals( EnchantLooks.COUNT, EnchantLooks.all().size() );
		assertNull( EnchantLooks.of( String.class ) );
		assertNull( EnchantLooks.byCode( -1 ) );
		assertNull( EnchantLooks.byCode( EnchantLooks.COUNT ) );
	}

	@Test
	public void theCodesArePinned(){
		assertEquals( EnchantLooks.COUNT, CODES.length );
		HashSet<Integer> codes = new HashSet<>();
		for (Object[] row : CODES){
			Class<?> c = (Class<?>) row[0];
			int code = (Integer) row[1];
			assertTrue( "code twice: " + code, codes.add( code ) );
			assertEquals( c.getSimpleName(), code, EnchantLooks.of( c ).code );
			assertSame( c, EnchantLooks.byCode( code ).of );
		}
		for (int i = 0; i < EnchantLooks.COUNT; i++) assertEquals( i, EnchantLooks.byCode( i ).code );
		//the lists' ranges
		for (EnchantLook l : EnchantLooks.all()){
			if (l.code < EnchantLooks.GLYPHS){
				assertFalse( l.toString(), l.glyph || l.curse );
			} else if (l.code < EnchantLooks.WEAPON_CURSES){
				assertTrue( l.toString(), l.glyph && !l.curse );
			} else if (l.code < EnchantLooks.ARMOR_CURSES){
				assertTrue( l.toString(), l.curse && !l.glyph );
			} else {
				assertTrue( l.toString(), l.curse && l.glyph );
			}
		}
	}

	@Test
	public void theGlowEveryClassShowsFindsItsRow(){
		for (Class<?> c : every()){
			Object made = Reflection.newInstance( c );
			ItemSprite.Glowing g = made instanceof Weapon.Enchantment ? ((Weapon.Enchantment) made).glowing()
					: ((Armor.Glyph) made).glowing();
			assertNotNull( c.getSimpleName() + " shows no glow", g );
			assertSame( c.getSimpleName(), EnchantLooks.of( c ), EnchantLooks.forGlowing( g ) );
		}
		//every glow a class keeps (a crystal's flaws, an explosive's fuse) finds it too
		for (Class<?> c : new Class<?>[]{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Crystal.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Explosive.class }){
			int found = 0;
			for (Field f : c.getDeclaredFields()){
				if (!Modifier.isStatic( f.getModifiers() ) || f.getType() != ItemSprite.Glowing.class) continue;
				try {
					f.setAccessible( true );
					assertSame( c.getSimpleName() + "." + f.getName(), EnchantLooks.of( c ), EnchantLooks.forGlowing( (ItemSprite.Glowing) f.get( null ) ) );
					found++;
				} catch (IllegalAccessException e){
					throw new AssertionError( e );
				}
			}
			assertEquals( c.getSimpleName() + "'s three glows", 3, found );
		}
		//a glow of no enchantment finds none
		assertNull( EnchantLooks.forGlowing( new ItemSprite.Glowing( 0xFF4400 ) ) );
		assertNull( EnchantLooks.forGlowing( null ) );
	}

	@Test
	public void everyStingIsOneOfTheGamesSounds() throws IllegalAccessException {
		HashSet<String> sounds = new HashSet<>();
		for (Field f : Assets.Sounds.class.getFields()){
			if (Modifier.isStatic( f.getModifiers() ) && f.getType() == String.class) sounds.add( (String) f.get( null ) );
		}
		int with = 0;
		for (EnchantLook l : EnchantLooks.all()){
			if (l.sound == null){
				assertFalse( l + ": its own sound and none", l.ownSound );
				continue;
			}
			with++;
			assertTrue( l + ": " + l.sound, sounds.contains( l.sound ) );
			assertTrue( l + " volume", l.vol > 0 && l.vol <= 1f );
			assertTrue( l + " pitch", l.pitch >= 0.4f && l.pitch <= 2f );
		}
		assertTrue( with > 70 );
	}

	@Test
	public void theRowsAreWhole(){
		for (EnchantLook l : EnchantLooks.all()){
			assertNotNull( l + " family", l.fam );
			assertNotNull( l + " element", l.el );
			assertNotNull( l + " motif", l.motif );
			assertNotNull( l + " profile", l.profile );
			assertEquals( l + " emissive", l.el.emissive, l.emissive );
			assertEquals( l.el.ramp.size(), l.pale.size() );
			if (l.motif == Motif.CYCLE) assertTrue( l + " walks colours", l.cycle != null && l.cycle.length >= 2 );
			if (l.fam != Family.MIRAGE) assertNotNull( l + " rune", l.rune );
			if (l.curse) assertTrue( l + ": a curse's glow is its fuse or the curse's", l.profile == Profile.CURSE || l.profile == Profile.PULSE );
		}
	}
}
