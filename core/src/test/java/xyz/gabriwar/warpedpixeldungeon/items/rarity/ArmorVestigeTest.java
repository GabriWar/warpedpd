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

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicImmune;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.ItemLooks;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Entanglement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Flow;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Obfuscation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Purity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Swiftness;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.HERO;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.dummy;

/** The vestigial glyph: which armor carries one, which glyph, how often it answers, when it is
 *  silent, and that it is saved, copied, scorched, rerolled, granted to older saves, described
 *  and shown as the weapon's vestige is. */
public class ArmorVestigeTest {

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;

	@BeforeClass
	public static void boot() throws Exception {
		QualitySourceTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//a level's cells reach the co-op layer, which reads the game's version as it starts
		savedVersion = Game.version;
		Game.version = "test";
		QualitySourceTest.arena();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.version = savedVersion;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//a glyph that only counts its procs
	private static class Counting extends Armor.Glyph {
		int procs = 0;
		@Override public int proc( Armor armor, Char attacker, Char defender, int damage ){
			procs++;
			return damage;
		}
		@Override public ItemSprite.Glowing glowing(){ return null; }
	}

	private static ClothArmor legendary(){
		ClothArmor armor = new ClothArmor();
		Quality.roll( armor, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		return armor;
	}

	//a legendary cloth armor with no glyph and only its vestige, rolled to answer this often
	private static ClothArmor vestigeOnly( Armor.Glyph v, float value ){
		ClothArmor armor = new ClothArmor();
		Quality q = QualitySourceTest.only( armor, Rarity.LEGENDARY, value / RarityLine.VESTIGE.max(), RarityLine.VESTIGE );
		armor.inscribe( null );
		q.vestige = v;
		armor.identify();
		return armor;
	}

	private static void assertOneEligibleVestige( Armor armor ){
		String what = armor.getClass().getSimpleName();
		Quality q = armor.quality;
		assertEquals( what, RarityLine.VESTIGE, q.lines.get( 0 ) );
		assertEquals( what, 1, q.lines.lastIndexOf( RarityLine.VESTIGE ) + 1 - q.lines.indexOf( RarityLine.VESTIGE ) );
		assertEquals( what, q.lines.size(), q.rolls.size() );
		Armor.Glyph v = Quality.glyphVestigeOf( armor );
		assertNotNull( what, v );
		assertNull( "no enchantment on armor", Quality.vestigeOf( armor ) );
		assertTrue( what + " " + v.getClass(), Quality.glyphVestigePool().contains( v.getClass() ) );
		for (Armor.Glyph g : armor.glyphs()) assertFalse( what + " echoes its own " + v.getClass(), g.getClass() == v.getClass() );
		assertEquals( 0, v.level() );
	}

	@Test
	public void everyLegendaryAndExoticArmorCarriesOne(){
		Supplier<?>[] kinds = { ClothArmor::new, LeatherArmor::new, PlateArmor::new };
		for (Rarity r : new Rarity[]{ Rarity.LEGENDARY, Rarity.EXOTIC }){
			for (Supplier<?> kind : kinds){
				for (int i = 0; i < 200; i++){
					Armor armor = (Armor) kind.get();
					Quality.roll( armor, r, ItemType.GAMMA, 0 );
					assertOneEligibleVestige( armor );
					assertEquals( r.lines + 1, armor.quality.lines.size() );
					//its tier's own glyph is there beside it
					assertNotNull( armor.glyph );
				}
			}
		}
		for (Rarity r : new Rarity[]{ Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE }){
			for (int i = 0; i < 100; i++){
				PlateArmor armor = new PlateArmor();
				Quality.roll( armor, r, ItemType.GAMMA, 0 );
				assertFalse( armor.quality.has( RarityLine.VESTIGE ) );
				assertNull( Quality.vestigeMagic( armor ) );
			}
		}
	}

	@Test
	public void theGlyphsItCanBeAreTheOnesWhoseProcActsAlone(){
		List<Class<?>> pool = Quality.glyphVestigePool();
		//the 23 good glyphs less the seven that act only where the game asks for them, and Stone
		assertEquals( 15, pool.size() );
		for (Class<?> c : new Class<?>[]{ Obfuscation.class, Swiftness.class, Brimstone.class, Camouflage.class,
				Flow.class, AntiMagic.class, Afterimage.class, Stone.class }){
			assertFalse( c.getSimpleName(), pool.contains( c ) );
		}
		List<Class<?>> good = new ArrayList<>();
		good.addAll( Arrays.asList( Armor.Glyph.common ) );
		good.addAll( Arrays.asList( Armor.Glyph.uncommon ) );
		good.addAll( Arrays.asList( Armor.Glyph.rare ) );
		assertTrue( good.containsAll( pool ) );
		for (Class<?> c : Armor.Glyph.curses) assertFalse( c.getSimpleName(), pool.contains( c ) );
		//a weapon's pool is its own
		assertEquals( 22, Quality.vestigePool().size() );
	}

	@Test
	public void itNeverEchoesTheArmorsOwnGlyphs(){
		for (int i = 0; i < 300; i++){
			ClothArmor armor = new ClothArmor();
			armor.inscribe( new Thorns() );
			armor.addGlyph( new Entanglement() );
			Quality.roll( armor, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
			Armor.Glyph v = Quality.glyphVestigeOf( armor );
			assertFalse( v.getClass().getSimpleName(), v instanceof Thorns || v instanceof Entanglement );
		}
	}

	@Test
	public void itIsSavedCopiedAndCompared(){
		ClothArmor armor = legendary();
		Armor.Glyph v = Quality.glyphVestigeOf( armor );
		Bundle b = new Bundle();
		armor.storeInBundle( b );
		ClothArmor back = new ClothArmor();
		back.restoreFromBundle( b );
		assertSame( v.getClass(), Quality.glyphVestigeOf( back ).getClass() );
		assertEquals( armor.quality.lines, back.quality.lines );
		assertEquals( armor.quality.rolls, back.quality.rolls );

		Quality copy = armor.quality.copy();
		assertSame( v.getClass(), copy.vestige.getClass() );
		assertEquals( RarityLine.VESTIGE, copy.lines.get( 0 ) );

		ClothArmor twin = new ClothArmor();
		twin.quality = armor.quality.copy();
		assertTrue( Quality.sameQuality( armor, twin ) );
		twin.quality.vestige = v instanceof Thorns ? new Purity() : new Thorns();
		assertFalse( Quality.sameQuality( armor, twin ) );
	}

	@Test
	public void aVestigeThatNoLongerExistsTakesItsLineAlong(){
		ClothArmor armor = legendary();
		Bundle b = new Bundle();
		armor.quality.storeInBundle( b );
		b.remove( "vestige" );
		Quality back = new Quality();
		back.restoreFromBundle( b );
		assertFalse( back.has( RarityLine.VESTIGE ) );
		assertEquals( armor.quality.lines.subList( 1, 3 ), back.lines );
		assertEquals( 2, back.rolls.size() );
	}

	//a legendary armor as a save from before armor vestiges wrote it: from before any vestige, or
	//from when weapons alone had them (vestige_rolled written, glyph_vestige_rolled not)
	private static Bundle oldSave( Armor a, boolean weaponsHadThem ){
		Quality q = a.quality;
		q.lines.remove( 0 );
		q.rolls.remove( 0 );
		q.vestige = null;
		Bundle b = new Bundle();
		a.storeInBundle( b );
		Bundle qb = b.getBundle( "quality" );
		assertTrue( qb.remove( "glyph_vestige_rolled" ) );
		if (!weaponsHadThem) assertTrue( qb.remove( "vestige_rolled" ) );
		assertFalse( qb.contains( "vestige" ) );
		return b;
	}

	@Test
	public void anOlderSaveGainsTheSameVestigeOnEveryLoad(){
		for (boolean weaponsHadThem : new boolean[]{ false, true }){
			for (int i = 0; i < 50; i++){
				Bundle old = oldSave( legendary(), weaponsHadThem );
				long untouched = firstLong( 0xA2L + i );
				Random.pushGenerator( 0xA2L + i );
				ClothArmor a = new ClothArmor();
				try {
					a.restoreFromBundle( old );
					//the grant draws from its own seed, never the game's
					assertEquals( untouched, Random.Long() );
				} finally {
					Random.popGenerator();
				}
				ClothArmor b = new ClothArmor();
				b.restoreFromBundle( old );
				assertOneEligibleVestige( a );
				assertSame( Quality.glyphVestigeOf( a ).getClass(), Quality.glyphVestigeOf( b ).getClass() );
				assertEquals( a.quality.rolls, b.quality.rolls );

				//once granted it is saved, and a scorched one never grows back
				a.quality.lines.remove( 0 );
				a.quality.rolls.remove( 0 );
				Bundle now = new Bundle();
				a.storeInBundle( now );
				ClothArmor c = new ClothArmor();
				c.restoreFromBundle( now );
				assertNull( Quality.glyphVestigeOf( c ) );
			}
		}

		//an older rare armor gains nothing
		PlateArmor rare = new PlateArmor();
		Quality.roll( rare, Rarity.RARE, ItemType.GAMMA, 0 );
		Bundle b = new Bundle();
		rare.storeInBundle( b );
		b.getBundle( "quality" ).remove( "glyph_vestige_rolled" );
		PlateArmor r = new PlateArmor();
		r.restoreFromBundle( b );
		assertNull( Quality.vestigeMagic( r ) );

		//a weapon saved when only weapons had vestiges keeps exactly what it had
		Longsword sword = new Longsword();
		Quality.roll( sword, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		Bundle sb = new Bundle();
		sword.storeInBundle( sb );
		sb.getBundle( "quality" ).remove( "glyph_vestige_rolled" );
		Longsword s = new Longsword();
		s.restoreFromBundle( sb );
		assertEquals( sword.quality.lines, s.quality.lines );
		assertEquals( sword.quality.rolls, s.quality.rolls );
		assertSame( Quality.vestigeOf( sword ).getClass(), Quality.vestigeOf( s ).getClass() );
	}

	@Test
	public void theForgeBurnsItLastAndARerollGivesOrClearsIt(){
		ClothArmor armor = legendary();
		Quality q = armor.quality;
		Armor.Glyph v = Quality.glyphVestigeOf( armor );
		assertTrue( q.scorch( armor ) );
		assertTrue( q.scorch( armor ) );
		assertSame( v, Quality.glyphVestigeOf( armor ) );
		assertTrue( q.scorch( armor ) );
		assertNull( Quality.glyphVestigeOf( armor ) );

		ClothArmor other = legendary();
		other.quality.rerollRarity( other, Rarity.RARE );
		assertNull( Quality.vestigeMagic( other ) );
		assertNull( other.quality.vestige );
		other.quality.rerollRarity( other, Rarity.EXOTIC );
		assertOneEligibleVestige( other );
		assertEquals( 3, other.quality.lines.size() );
	}

	@Test
	public void itAnswersAtItsValueAndMasterworkRaisesIt(){
		Counting counter = new Counting();
		ClothArmor armor = vestigeOnly( counter, 0.6f );
		assertEquals( 0.6f, Quality.value( armor, RarityLine.VESTIGE ), 1e-5f );
		Char attacker = dummy( HERO + 1, Char.Alignment.ENEMY, 1000000 );
		Char wearer = dummy( HERO + 2, Char.Alignment.ALLY, 1000000 );
		int blows = 20000;
		Random.pushGenerator( 0xA4L );
		try {
			for (int i = 0; i < blows; i++) armor.proc( attacker, wearer, 10 );
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.6f, counter.procs / (float) blows, 0.02f );

		//a legendary roll answers 21-30% as often as the glyph would, twice that fully masterworked
		ClothArmor rolled = legendary();
		float k = Quality.value( rolled, RarityLine.VESTIGE );
		assertTrue( "" + k, k >= 0.21f - 1e-4f && k <= 0.30f + 1e-4f );
		rolled.quality.masterwork = Quality.MASTERWORK_MAX;
		assertEquals( 2f * k, Quality.value( rolled, RarityLine.VESTIGE ), 1e-5f );
	}

	@Test
	public void theSameGlyphSilencesIt(){
		ClothArmor armor = vestigeOnly( new Thorns(), 3f );
		assertNotNull( Quality.vestigeFor( armor, null ) );
		//Trinity's borrowed glyph silences it too
		assertNull( Quality.vestigeFor( armor, new Thorns() ) );
		armor.inscribe( new Thorns() );
		long untouched = firstLong( 0x511EL );
		Random.pushGenerator( 0x511EL );
		try {
			assertNull( Quality.vestigeFor( armor, null ) );
			//silent draws nothing
			assertEquals( untouched, Random.Long() );
		} finally {
			Random.popGenerator();
		}
		String line = RarityLine.VESTIGE.describe( armor, armor.quality.roll( RarityLine.VESTIGE ) );
		assertTrue( line, line.contains( "silent while the armor carries the real one" ) );
		assertTrue( line, line.contains( "vestigial " + new Thorns().name() ) );

		//in the second slot too
		ClothArmor second = vestigeOnly( new Thorns(), 3f );
		second.inscribe( new Purity() );
		second.addGlyph( new Thorns() );
		assertNull( Quality.vestigeFor( second, null ) );

		//"unweave all" keeps the vestige, and it speaks again
		armor.inscribe( null );
		assertNotNull( Quality.vestigeFor( armor, null ) );
	}

	@Test
	public void magicImmunityAndAnotherClassesHolyWardSilenceIt(){
		Counting counter = new Counting();
		ClothArmor armor = vestigeOnly( counter, 3f );
		Char attacker = dummy( HERO + 1, Char.Alignment.ENEMY, 1000000 );

		Hero hero = Dungeon.hero;
		hero.belongings.armor = armor;
		Buff.affect( hero, MagicImmune.class, 100f );
		for (int i = 0; i < 50; i++) armor.proc( attacker, hero, 10 );
		assertEquals( 0, counter.procs );
		hero.buff( MagicImmune.class ).detach();

		Buff.affect( hero, HolyWard.HolyArmBuff.class, 100f );
		hero.subClass = HeroSubClass.NONE;
		for (int i = 0; i < 50; i++) armor.proc( attacker, hero, 10 );
		assertEquals( 0, counter.procs );

		//a Paladin's holy ward keeps its good magic, the vestige too
		hero.subClass = HeroSubClass.PALADIN;
		for (int i = 0; i < 50; i++) armor.proc( attacker, hero, 10 );
		assertEquals( 50, counter.procs );
	}

	@Test
	public void onlyTheVestigeItselfIsTheVestige(){
		ClothArmor armor = legendary();
		Armor.Glyph v = Quality.glyphVestigeOf( armor );
		assertTrue( Quality.isVestige( armor, v ) );
		assertFalse( Quality.isVestige( armor, armor.glyph ) );
		assertFalse( Quality.isVestige( armor, (Armor.Glyph) Reflection.newInstance( v.getClass() ) ) );
		assertFalse( Quality.isVestige( armor, (Armor.Glyph) null ) );
		//another armor's vestige is not this one's
		ClothArmor other = legendary();
		assertFalse( Quality.isVestige( other, v ) );
	}

	@Test
	public void itStaysHiddenUntilIdentifiedThenIsDescribedAndShown(){
		ClothArmor armor = legendary();
		armor.levelKnown = false;
		Armor.Glyph v = Quality.glyphVestigeOf( armor );
		String hidden = armor.quality.describeRarity( armor );
		assertTrue( hidden, hidden.contains( Messages.get( Quality.class, "unknown_lines" ) ) );
		assertFalse( hidden, hidden.contains( "vestigial" ) );
		assertNull( "no echo unidentified", ItemLooks.of( armor ).echo );

		armor.identify();
		for (String shown : new String[]{ armor.quality.describeRarity( armor ), armor.quality.describe( armor ) }){
			assertTrue( shown, shown.contains( Messages.get( Quality.class, "tab_vestige_glyph" ) ) );
			assertFalse( shown, shown.contains( Messages.get( Quality.class, "tab_vestige" ) ) );
			assertTrue( shown, shown.contains( "vestigial " + v.name() + ": triggers " ) );
			assertTrue( shown, shown.contains( v.desc() ) );
			assertFalse( shown, shown.contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( shown, shown.contains( "%1$" ) );
		}
		//it is in no slot: the glyph checks leave it out
		for (Armor.Glyph g : armor.glyphs()) assertFalse( g == v );
		assertFalse( armor.hasGlyph( v.getClass(), Dungeon.hero ) );
		//its echo shows on the item once identified
		assertSame( EnchantLooks.of( v.getClass() ), ItemLooks.of( armor ).echo );

		//the tier tabs say legendary and exotic armor carries one, rare armor does not
		String other = "Their armor also carries a _vestigial glyph_";
		String exotic = armor.quality.describeRarityTier( armor, Rarity.EXOTIC );
		assertTrue( exotic, exotic.contains( "_. " + other ) );
		assertTrue( armor.quality.describeRarityTier( armor, Rarity.LEGENDARY ).contains( other ) );
		assertFalse( armor.quality.describeRarityTier( armor, Rarity.RARE ).contains( other ) );
		assertFalse( exotic, exotic.contains( "Their weapons" ) );
		Longsword sword = new Longsword();
		Quality.roll( sword, Rarity.RARE, ItemType.GAMMA, 0 );
		assertFalse( sword.quality.describeRarityTier( sword, Rarity.EXOTIC ).contains( other ) );
	}

	private static long firstLong( long seed ){
		Random.pushGenerator( seed );
		try {
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}
}
