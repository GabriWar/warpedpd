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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CreepingDread;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.HitRecord;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.Side;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Reflection;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The Damage tab names every icon a hit card can show (ChipLegend), in English and in Portuguese:
 * every text cell the cards use, a family mark as its very enchantment, a buff by the snapshot that
 * wore it, a skill with its shout.
 */
public class ChipLegendTest {

	private EnchantFxTest.Rig rig;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
	}

	@After
	public void tearDown(){
		Messages.setup( Languages.ENGLISH );
		rig.down();
	}

	//every text cell a card can carry: FloatingText's icon constants, the families' hollow twins, the
	//hit reasons' no-block row, and the cells the words package adds
	private static TreeSet<Integer> cardCells() throws Exception {
		TreeSet<Integer> cells = new TreeSet<>();
		for (Field f : FloatingText.class.getFields()){
			int mod = f.getModifiers();
			if (f.getType() != int.class || !Modifier.isStatic( mod )) continue;
			String n = f.getName();
			if (n.equals( "NO_ICON" ) || n.equals( "ICON_WIDTH" ) || n.equals( "ICON_HEIGHT" ) || n.equals( "HOLLOW" )) continue;
			cells.add( f.getInt( null ) );
		}
		for (int c = FloatingText.F_EMBER; c <= FloatingText.F_HEX; c++) cells.add( FloatingText.hollow( c ) );
		for (int c = FloatingText.HIT_WEP; c <= FloatingText.HIT_MOMEN; c++) cells.add( c + ChipLegend.NO_BLOCK_ROW );
		cells.addAll( Arrays.asList( ChipLegend.MISS_DODGE, ChipLegend.MISS_BLOCK, ChipLegend.GUARDED, ChipLegend.IMMUNE,
				ChipLegend.RESISTED, ChipLegend.RAGE, ChipLegend.FUSE, ChipLegend.RELOAD ) );
		return cells;
	}

	private static void allNamed( String lang ) throws Exception {
		String general = Messages.get( ChipLegend.class, "mark" );
		ArrayList<String> bad = new ArrayList<>();
		for (int cell : cardCells()){
			String s = ChipLegend.name( cell, 0, null, null );
			if (s == null || s.trim().isEmpty() || s.contains( Messages.NO_TEXT_FOUND ) || s.equals( general )
					|| s.contains( "%" )){
				bad.add( cell + ": " + s );
			}
		}
		assertTrue( lang + ": cells with no name of their own: " + bad, bad.isEmpty() );
	}

	@Test
	public void everyCardCellHasANameInEnglishAndPortuguese() throws Exception {
		Messages.setup( Languages.ENGLISH );
		allNamed( "en" );
		Messages.setup( Languages.PORTUGUESE );
		allNamed( "pt" );
	}

	@Test
	public void namesDifferBetweenTheLanguages() throws Exception {
		Messages.setup( Languages.ENGLISH );
		String en = ChipLegend.name( FloatingText.MAGIC_DMG, 0, null, null );
		Messages.setup( Languages.PORTUGUESE );
		String pt = ChipLegend.name( FloatingText.MAGIC_DMG, 0, null, null );
		assertEquals( "Magic damage", en );
		assertEquals( "Dano mágico", pt );
	}

	@Test
	public void aFamilyMarkNamesTheEnchantmentOfItsColour(){
		Messages.setup( Languages.ENGLISH );
		int tint = EnchantFx.chipTintOf( EnchantLooks.of( Blazing.class ), false );
		int cell = EnchantFx.chipOf( EnchantLooks.of( Blazing.class ), false );
		String blazing = Messages.titleCase( new Blazing().name() );
		assertEquals( blazing, ChipLegend.name( cell, tint, null, null ) );
		//its echo
		int echoTint = EnchantFx.chipTintOf( EnchantLooks.of( Blazing.class ), true );
		int echo = EnchantFx.chipOf( EnchantLooks.of( Blazing.class ), true );
		assertEquals( Messages.get( ChipLegend.class, "echo", blazing ), ChipLegend.name( echo, echoTint, null, null ) );
		//with no colour, the family
		assertEquals( Messages.get( ChipLegend.class, "f_ember" ), ChipLegend.name( cell, 0, null, null ) );
	}

	@Test
	public void aBuffIsNamedByWhoWoreIt(){
		Messages.setup( Languages.ENGLISH );
		int terror = BuffIndicator.TERROR;
		int code = -(terror + 1);

		HitRecord r = new HitRecord();
		r.defender = new Side();
		r.defender.buffIcons = new int[]{ terror };
		r.defender.buffTints = new int[]{ 0 };
		r.defender.buffNames = new String[]{ "Scared Stiff" };
		r.defender.buffClasses = new String[]{ "x" };
		assertEquals( "Scared Stiff", ChipLegend.name( code, 0, r, null ) );

		//laid on by the hit: the next hit's snapshot has it
		HitRecord bare = new HitRecord();
		assertEquals( "Scared Stiff", ChipLegend.name( code, 0, bare, Collections.singletonList( r ) ) );

		//nobody's: the buff its BuffIndicator constant names
		assertEquals( Messages.titleCase( Messages.get( "actors.buffs.terror.name" ) ), ChipLegend.name( code, 0, bare, null ) );

		//no buff at all: the general word
		assertEquals( Messages.get( ChipLegend.class, "effect" ), ChipLegend.name( -(BuffIndicator.NONE + 1), 0, null, null ) );
	}

	@Test
	public void aSkillIsNamedWithItsShout(){
		Messages.setup( Languages.ENGLISH );
		Skill dread = new CreepingDread();
		int image = dread.image();
		int same = 0;
		for (CurrentSkills.Origin o : CurrentSkills.catalog()){
			Skill s = (Skill) Reflection.newInstance( o.cls );
			if (s != null && s.image() == image) same++;
		}
		String named = ChipLegend.name( ChipArt.SKILL_BASE + image, 0, null, null );
		assertFalse( named.isEmpty() );
		if (same == 1){
			assertEquals( dread.name() + ": " + dread.castText(), named );
		}
		assertTrue( named, named.contains( ": " ) || same > 1 );
		//an image no skill has: the general word
		assertEquals( Messages.get( ChipLegend.class, "skill" ), ChipLegend.name( ChipArt.SKILL_BASE + 100000, 0, null, null ) );
	}

	@Test
	public void anUnusedCellReadsAsAMark(){
		Messages.setup( Languages.ENGLISH );
		assertEquals( Messages.get( ChipLegend.class, "mark" ), ChipLegend.name( 140, 0, null, null ) );
	}
}
