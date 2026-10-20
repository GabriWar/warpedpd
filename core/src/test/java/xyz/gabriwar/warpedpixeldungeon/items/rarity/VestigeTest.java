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
import com.watabou.noosa.Game;
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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Luck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.HERO;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.dummy;

/** The vestigial enchantment: who carries one, which, how often it fires, when it is silent,
 *  and that it survives saves, copies, splits, the forge and the smith's reroll. */
public class VestigeTest {

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

	//an enchantment that only counts its procs
	private static class Counting extends Weapon.Enchantment {
		int procs = 0;
		@Override public int proc( Weapon weapon, Char attacker, Char defender, int damage ){
			procs++;
			return damage;
		}
		@Override public ItemSprite.Glowing glowing(){ return null; }
	}

	private static Longsword legendary(){
		Longsword sword = new Longsword();
		Quality.roll( sword, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		return sword;
	}

	//a legendary sword with no enchantment and only its vestige, rolled to fire this often
	private static Longsword vestigeOnly( Weapon.Enchantment v, float value ){
		Longsword sword = new Longsword();
		Quality q = QualitySourceTest.only( sword, Rarity.LEGENDARY, value / RarityLine.VESTIGE.max(), RarityLine.VESTIGE );
		q.vestige = v;
		sword.identify();
		return sword;
	}

	private static void assertOneEligibleVestige( Item item ){
		String what = item.getClass().getSimpleName();
		Quality q = item.quality;
		assertEquals( what, RarityLine.VESTIGE, q.lines.get( 0 ) );
		assertEquals( what, 1, q.lines.lastIndexOf( RarityLine.VESTIGE ) + 1 - q.lines.indexOf( RarityLine.VESTIGE ) );
		assertEquals( what, q.lines.size(), q.rolls.size() );
		Weapon.Enchantment v = Quality.vestigeOf( item );
		assertNotNull( what, v );
		assertTrue( what + " " + v.getClass(), Quality.vestigePool().contains( v.getClass() ) );
		Weapon w = (Weapon) item;
		assertFalse( what + " echoes its own " + v.getClass(), Quality.kin( v, w.enchantment ) || Quality.kin( v, w.enchantment2 ) );
		assertEquals( 0, v.level() );
	}

	@Test
	public void everyLegendaryAndExoticWeaponCarriesOne(){
		Supplier<?>[] kinds = { Longsword::new, Bow::new, FT::new, Javelin::new };
		for (Rarity r : new Rarity[]{ Rarity.LEGENDARY, Rarity.EXOTIC }){
			for (Supplier<?> kind : kinds){
				for (int i = 0; i < 200; i++){
					Item item = (Item) kind.get();
					Quality.roll( item, r, ItemType.GAMMA, 0 );
					assertOneEligibleVestige( item );
					assertEquals( r.lines + 1, item.quality.lines.size() );
				}
			}
		}
		for (int i = 0; i < 200; i++){
			Longsword rare = new Longsword();
			Quality.roll( rare, Rarity.RARE, ItemType.GAMMA, 0 );
			assertFalse( rare.quality.has( RarityLine.VESTIGE ) );
			assertNull( Quality.vestigeOf( rare ) );
			//armor's vestige is a glyph (ArmorVestigeTest), never an enchantment
			PlateArmor armor = new PlateArmor();
			Quality.roll( armor, Rarity.EXOTIC, ItemType.GAMMA, 0 );
			assertNull( Quality.vestigeOf( armor ) );
		}
		//the 22: the good pools less Kinetic, Crystal, Projecting, Ancient, Unstable, Horror, Poison
		assertEquals( 22, Quality.vestigePool().size() );
	}

	@Test
	public void itNeverEchoesTheWeaponsOwnEnchantmentOrItsKin(){
		for (int i = 0; i < 300; i++){
			Longsword sword = new Longsword();
			sword.enchant( new Venomous() );
			sword.addEnchant( new Luck() );
			Quality.roll( sword, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
			Weapon.Enchantment v = Quality.vestigeOf( sword );
			assertFalse( v.getClass().getSimpleName(), v instanceof Venomous || v instanceof Poison );
			assertFalse( v.getClass().getSimpleName(), v instanceof Luck
					|| v instanceof xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing );
		}
	}

	@Test
	public void aLineFromANewerBuildIsDroppedOnLoadInsteadOfCrashing(){
		Longsword sword = legendary();
		Bundle b = new Bundle();
		sword.quality.storeInBundle( b );
		String[] names = b.getStringArray( "lines" );
		names[names.length - 1] = "A_LINE_FROM_A_NEWER_BUILD";
		b.put( "lines", names );
		Quality back = new Quality();
		back.restoreFromBundle( b );
		assertEquals( sword.quality.lines.subList( 0, names.length - 1 ), back.lines );
		assertEquals( back.lines.size(), back.rolls.size() );
	}

	@Test
	public void itIsSavedCopiedSplitAndCompared(){
		Longsword sword = legendary();
		Weapon.Enchantment v = Quality.vestigeOf( sword );
		Bundle b = new Bundle();
		sword.storeInBundle( b );
		Longsword back = new Longsword();
		back.restoreFromBundle( b );
		assertSame( v.getClass(), Quality.vestigeOf( back ).getClass() );
		assertEquals( sword.quality.lines, back.quality.lines );
		assertEquals( sword.quality.rolls, back.quality.rolls );

		Quality copy = sword.quality.copy();
		assertSame( v.getClass(), copy.vestige.getClass() );
		assertEquals( RarityLine.VESTIGE, copy.lines.get( 0 ) );

		Javelin stack = new Javelin();
		stack.quantity( 3 );
		Quality.roll( stack, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		MissileWeapon child = (MissileWeapon) stack.split( 1 );
		assertSame( Quality.vestigeOf( stack ).getClass(), Quality.vestigeOf( child ).getClass() );
		assertTrue( Quality.sameQuality( stack, child ) );
		assertTrue( stack.isSimilar( child ) );

		//two stacks alike in all but their vestige never merge
		Javelin twin = new Javelin();
		twin.quality = stack.quality.copy();
		assertTrue( Quality.sameQuality( stack, twin ) );
		twin.quality.vestige = Quality.vestigeOf( stack ) instanceof Blazing ? new Chilling() : new Blazing();
		assertFalse( Quality.sameQuality( stack, twin ) );
		assertFalse( stack.isSimilar( twin ) );
	}

	@Test
	public void aVestigeThatNoLongerExistsTakesItsLineAlong(){
		Longsword sword = legendary();
		Bundle b = new Bundle();
		sword.quality.storeInBundle( b );
		b.remove( "vestige" );
		Quality back = new Quality();
		back.restoreFromBundle( b );
		assertFalse( back.has( RarityLine.VESTIGE ) );
		assertEquals( 2, back.lines.size() );
		assertEquals( 2, back.rolls.size() );
		assertEquals( sword.quality.lines.subList( 1, 3 ), back.lines );
	}

	//a legendary weapon as a save from before vestiges wrote it
	private static Bundle oldSave( Weapon w ){
		Quality q = w.quality;
		q.lines.remove( 0 );
		q.rolls.remove( 0 );
		q.vestige = null;
		Bundle b = new Bundle();
		w.storeInBundle( b );
		Bundle qb = b.getBundle( "quality" );
		assertTrue( qb.remove( "vestige_rolled" ) );
		assertFalse( qb.contains( "vestige" ) );
		return b;
	}

	@Test
	public void anOlderSaveGainsTheSameVestigeOnEveryLoad(){
		for (int i = 0; i < 50; i++){
			Bundle old = oldSave( legendary() );
			long untouched = firstLong( 0x01DL + i );
			Random.pushGenerator( 0x01DL + i );
			Longsword a = new Longsword();
			try {
				a.restoreFromBundle( old );
				//the grant draws from its own seed, never the game's
				assertEquals( untouched, Random.Long() );
			} finally {
				Random.popGenerator();
			}
			Longsword b = new Longsword();
			b.restoreFromBundle( old );
			assertOneEligibleVestige( a );
			assertSame( Quality.vestigeOf( a ).getClass(), Quality.vestigeOf( b ).getClass() );
			assertEquals( a.quality.rolls, b.quality.rolls );

			//once granted it is saved, and a scorched one never grows back
			a.quality.lines.remove( 0 );
			a.quality.rolls.remove( 0 );
			Bundle now = new Bundle();
			a.storeInBundle( now );
			Longsword c = new Longsword();
			c.restoreFromBundle( now );
			assertNull( Quality.vestigeOf( c ) );
		}

		//a thrown stack and its child saved apart gain the same one, and still merge
		Javelin stack = new Javelin();
		stack.quantity( 3 );
		Quality.roll( stack, Rarity.EXOTIC, ItemType.GAMMA, 0 );
		Bundle old = oldSave( stack );
		Javelin x = new Javelin(), y = new Javelin();
		x.restoreFromBundle( old );
		y.restoreFromBundle( old );
		assertTrue( Quality.sameQuality( x, y ) );
		assertTrue( x.isSimilar( y ) );

		//an older rare weapon gains nothing
		Longsword rare = new Longsword();
		Quality.roll( rare, Rarity.RARE, ItemType.GAMMA, 0 );
		Bundle b = new Bundle();
		rare.storeInBundle( b );
		b.getBundle( "quality" ).remove( "vestige_rolled" );
		Longsword r = new Longsword();
		r.restoreFromBundle( b );
		assertNull( Quality.vestigeOf( r ) );
	}

	@Test
	public void theForgeBurnsItLast(){
		Longsword sword = legendary();
		Quality q = sword.quality;
		Weapon.Enchantment v = Quality.vestigeOf( sword );
		assertTrue( q.scorch( sword ) );
		assertSame( v, Quality.vestigeOf( sword ) );
		assertTrue( q.scorch( sword ) );
		assertSame( v, Quality.vestigeOf( sword ) );
		assertEquals( 1, q.lines.size() );
		assertTrue( q.scorch( sword ) );
		assertNull( Quality.vestigeOf( sword ) );
	}

	@Test
	public void aRerollGivesOrClearsIt(){
		Longsword sword = legendary();
		Quality q = sword.quality;
		q.rerollRarity( sword, Rarity.RARE );
		assertNull( Quality.vestigeOf( sword ) );
		assertFalse( q.has( RarityLine.VESTIGE ) );
		assertNull( q.vestige );
		q.rerollRarity( sword, Rarity.EXOTIC );
		assertOneEligibleVestige( sword );
		assertEquals( 3, q.lines.size() );
	}

	@Test
	public void itFiresAtItsValue(){
		Longsword sword = vestigeOnly( new Luck(), 0.6f );
		assertEquals( 0.6f, Quality.value( sword, RarityLine.VESTIGE ), 1e-5f );
		//Luck takes the wielder's best roll: from a blow of 0 it shows when it acted
		Char wielder = new Char(){
			@Override public int damageRoll(){ return 50; }
		};
		wielder.alignment = Char.Alignment.ENEMY;
		wielder.pos = HERO + 19;
		Char target = dummy( HERO + 20, Char.Alignment.ALLY, 1000000 );
		int acted = 0, blows = 20000;
		Random.pushGenerator( 0x7E57L );
		try {
			for (int i = 0; i < blows; i++){
				if (sword.proc( wielder, target, 0 ) > 0) acted++;
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.6f, acted / (float) blows, 0.02f );
	}

	@Test
	public void theSameEnchantmentOrItsKinSilencesIt(){
		Longsword sword = vestigeOnly( new Blazing(), 3f );
		assertNotNull( Quality.vestigeFor( sword, null ) );
		//Trinity's borrowed enchantment silences it too
		assertNull( Quality.vestigeFor( sword, new Blazing() ) );
		sword.enchant( new Blazing() );
		long untouched = firstLong( 0x511EL );
		Random.pushGenerator( 0x511EL );
		try {
			assertNull( Quality.vestigeFor( sword, null ) );
			//silent draws nothing
			assertEquals( untouched, Random.Long() );
		} finally {
			Random.popGenerator();
		}
		sword.identify();
		assertTrue( RarityLine.VESTIGE.describe( sword, sword.quality.roll( RarityLine.VESTIGE ) ).contains( "silent" ) );

		//kin under another name says which enchantment hushes it
		Longsword lucky = vestigeOnly( new Luck(), 3f );
		lucky.enchant( new Slashing() );
		assertNull( Quality.vestigeFor( lucky, null ) );
		String kinLine = RarityLine.VESTIGE.describe( lucky, lucky.quality.roll( RarityLine.VESTIGE ) );
		assertTrue( kinLine, kinLine.contains( "vestigial " + new Luck().name() ) );
		assertTrue( kinLine, kinLine.contains( "its twin, the " + new Slashing().name() ) );
		assertFalse( kinLine, kinLine.contains( "the real one" ) );
		//Venomous and Poison share a name: the plain line stands
		Longsword venomous = vestigeOnly( new Venomous(), 3f );
		venomous.enchant( new Poison() );
		assertTrue( RarityLine.VESTIGE.describe( venomous, venomous.quality.roll( RarityLine.VESTIGE ) ).contains( "the real one" ) );

		Longsword venom = vestigeOnly( new Venomous(), 3f );
		venom.enchant( new Poison() );
		assertNull( Quality.vestigeFor( venom, null ) );
		//"unweave all" keeps the vestige, and it speaks again
		venom.enchant( null );
		assertNotNull( Quality.vestigeFor( venom, null ) );
	}

	@Test
	public void magicImmunityAndAnotherClassesHolyWeaponSilenceIt(){
		Counting counter = new Counting();
		Longsword sword = vestigeOnly( counter, 3f );
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 1000000 );

		Hero hero = Dungeon.hero;
		hero.belongings.weapon = sword;
		Buff.affect( hero, MagicImmune.class, 100f );
		for (int i = 0; i < 50; i++) sword.proc( hero, target, 10 );
		assertEquals( 0, counter.procs );
		hero.buff( MagicImmune.class ).detach();

		Buff.affect( hero, HolyWeapon.HolyWepBuff.class, 100f );
		hero.subClass = HeroSubClass.NONE;
		for (int i = 0; i < 50; i++) sword.proc( hero, target, 10 );
		assertEquals( 0, counter.procs );

		//a Paladin's holy weapon keeps its good magic, the vestige too
		hero.subClass = HeroSubClass.PALADIN;
		for (int i = 0; i < 50; i++) sword.proc( hero, target, 10 );
		assertEquals( 50, counter.procs );
	}

	@Test
	public void onlyTheVestigeItselfIsTheVestige(){
		Longsword sword = legendary();
		Weapon.Enchantment v = Quality.vestigeOf( sword );
		assertTrue( Quality.isVestige( sword, v ) );
		assertFalse( Quality.isVestige( sword, sword.enchantment ) );
		//an Unstable roll of the same enchantment is another instance
		assertFalse( Quality.isVestige( sword, (Weapon.Enchantment) Reflection.newInstance( v.getClass() ) ) );
		assertFalse( Quality.isVestige( sword, null ) );

		//a bow's shared enchantment on a thrown weapon is no vestige of the thrown weapon
		SpiritBow spirit = new SpiritBow();
		Quality.roll( spirit, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		Javelin javelin = new Javelin();
		Quality.roll( javelin, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		assertFalse( Quality.isVestige( javelin, spirit.enchantment ) );
		assertFalse( Quality.isVestige( javelin, Quality.vestigeOf( spirit ) ) );
		assertTrue( Quality.isVestige( javelin, Quality.vestigeOf( javelin ) ) );

		//an arrow fires its bow's vestige
		Bow bow = new Bow();
		Quality.roll( bow, Rarity.EXOTIC, ItemType.GAMMA, 0 );
		assertTrue( Quality.isVestige( bow.knockArrow(), Quality.vestigeOf( bow ) ) );
	}

	@Test
	public void itStaysHiddenUntilIdentified(){
		Longsword sword = legendary();
		Weapon.Enchantment v = Quality.vestigeOf( sword );
		String hidden = sword.quality.describeRarity( sword );
		assertTrue( hidden, hidden.contains( Messages.get( Quality.class, "unknown_lines" ) ) );
		assertFalse( hidden, hidden.contains( "vestigial" ) );
		assertFalse( sword.quality.describe( sword ).contains( "vestigial" ) );

		sword.identify();
		String shown = sword.quality.describeRarity( sword );
		assertTrue( shown, shown.contains( Messages.get( Quality.class, "tab_vestige" ) ) );
		assertTrue( shown, shown.contains( "vestigial " + v.name() ) );
		assertTrue( shown, shown.contains( v.desc() ) );
		assertFalse( shown, shown.contains( Messages.NO_TEXT_FOUND ) );
		assertTrue( sword.quality.describe( sword ).contains( "vestigial " + v.name() ) );
		//it is in no slot: the enchantment checks leave it out
		for (Weapon.Enchantment e : sword.enchantments()) assertFalse( e == v );
		assertFalse( sword.hasEnchant( v.getClass(), Dungeon.hero ) );

		//the tier tabs say legendary and exotic weapons carry one; rare ones do not, and armor's
		//tab speaks of its own (ArmorVestigeTest)
		String other = "Their weapons also carry";
		String exotic = sword.quality.describeRarityTier( sword, Rarity.EXOTIC );
		assertTrue( exotic, exotic.contains( "_. " + other ) );
		assertFalse( sword.quality.describeRarityTier( sword, Rarity.RARE ).contains( other ) );
		PlateArmor armor = new PlateArmor();
		Quality.roll( armor, Rarity.RARE, ItemType.GAMMA, 0 );
		assertFalse( armor.quality.describeRarityTier( armor, Rarity.LEGENDARY ).contains( other ) );
	}

	@Test
	public void theMasterworkSentenceUnderTheVestigeIsFilledIn(){
		Longsword sword = legendary();
		sword.quality.masterwork = 2;
		sword.quality.kills = 7;
		sword.identify();
		for (String tab : new String[]{ sword.quality.describeRarity( sword ), sword.quality.describe( sword ) }){
			assertTrue( tab, tab.contains( "Masterwork _2/" + Quality.MASTERWORK_MAX + "_" ) );
			assertTrue( tab, tab.contains( "lifted by 15% a step" ) );
			assertTrue( tab, tab.contains( "slain _7_ enemies" ) );
			assertFalse( tab, tab.contains( "%1$d" ) );
		}
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
