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
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.AssassinsBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.FightGloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Glaive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Gloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Mace;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shortsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ChainFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.LokisFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Boomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForestDart;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Kunai;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** A weapon's, thrown weapon's or armor's lines fill slots: a common piece a plain bonus, an
 *  uncommon one a trait, a rare or better one a trait then a plain bonus; and the roll draws one
 *  pick and one strength per line, whatever the size of the pool it picks from. */
public class LineSlotsTest {

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;

	@BeforeClass
	public static void boot() throws Exception {
		QualitySourceTest.boot();
	}

	//a thrown weapon's enchantment reaches the hero, so there is one, on a floor of his own
	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
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

	/** the game's Random with a count of the ints and floats drawn from it */
	static final class Counter extends java.util.Random {
		int ints, floats;

		Counter( long seed ){
			super( seed );
		}

		@Override public int nextInt( int bound ){ ints++; return super.nextInt( bound ); }
		@Override public float nextFloat(){ floats++; return super.nextFloat(); }

		/** puts a counter on top of the game's Random; take it off with Random.popGenerator() */
		@SuppressWarnings("unchecked")
		static Counter push( long seed ){
			Counter c = new Counter( seed );
			try {
				Field f = Random.class.getDeclaredField( "generators" );
				f.setAccessible( true );
				((ArrayDeque<java.util.Random>) f.get( null )).push( c );
			} catch (ReflectiveOperationException e){
				throw new IllegalStateException( e );
			}
			return c;
		}
	}

	private static final Supplier<?>[] SLOTTED = {
			Longsword::new, FT::new, Javelin::new, ClothArmor::new, PlateArmor::new
	};

	@Test
	public void eachTierFillsItsSlots(){
		Random.pushGenerator( 0x5107L );
		try {
			for (int i = 0; i < 200; i++){
				for (Supplier<?> kind : SLOTTED){
					for (Rarity r : Rarity.values()){
						Item item = (Item) kind.get();
						Quality.roll( item, r, ItemType.GAMMA, 0 );
						Quality q = item.quality;
						String what = item.getClass().getSimpleName() + " " + r + " " + q.lines;
						int at = 0;
						if (r.ordinal() >= Rarity.LEGENDARY.ordinal()){
							assertEquals( what, RarityLine.VESTIGE, q.lines.get( 0 ) );
							at = 1;
						}
						if (r == Rarity.COMMON){
							assertEquals( what, 1, q.lines.size() );
							assertFalse( what, q.lines.get( 0 ).trait() );
						} else if (r == Rarity.UNCOMMON){
							assertEquals( what, 1, q.lines.size() );
							assertTrue( what, q.lines.get( 0 ).trait() );
						} else {
							assertEquals( what, at + 2, q.lines.size() );
							assertTrue( what, q.lines.get( at ).trait() );
							assertFalse( what, q.lines.get( at + 1 ).trait() );
						}
						Quality.Family fam = Quality.family( item );
						for (RarityLine l : q.lines){
							if (l != RarityLine.VESTIGE) assertTrue( what, RarityLine.pool( fam, item ).contains( l ) );
						}
					}
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void aRollDrawsOncePerLineWhateverThePool(){
		//a longsword's pool and a flamethrower's (three launcher traits more) draw alike
		for (Supplier<?> kind : SLOTTED){
			assertDraws( (Item) kind.get(), Rarity.COMMON, 1 );
			assertDraws( (Item) kind.get(), Rarity.UNCOMMON, 1 );
			assertDraws( (Item) kind.get(), Rarity.RARE, 2 );
		}
		//a wand fills no slots: its tier's count from the whole pool, one pick a line
		assertDraws( new WandOfMagicMissile(), Rarity.COMMON, 1 );
		assertDraws( new WandOfMagicMissile(), Rarity.RARE, 2 );
	}

	@Test
	public void aSlayerDrawsItsFamily(){
		//an uncommon longsword's trait is one of four; seeds until one rolls SLAYER, and its family
		//is the one more Int
		int slayers = 0;
		for (long seed = 1; seed < 400; seed++){
			Longsword sword = new Longsword();
			Counter c = Counter.push( seed );
			try {
				Quality.roll( sword, Rarity.UNCOMMON, ItemType.GAMMA, 0 );
			} finally {
				Random.popGenerator();
			}
			Quality q = sword.quality;
			assertEquals( 1, c.floats );
			if (q.has( RarityLine.SLAYER )){
				slayers++;
				assertEquals( 2, c.ints );
				assertTrue( q.slayer != null );
			} else {
				assertEquals( 1, c.ints );
				assertEquals( null, q.slayer );
			}
		}
		assertTrue( "slayers " + slayers, slayers > 50 );
	}

	private static void assertDraws( Item item, Rarity r, int lines ){
		Counter c = Counter.push( 0xD2A3L );
		try {
			Quality.roll( item, r, ItemType.GAMMA, 0 );
		} finally {
			Random.popGenerator();
		}
		String what = item.getClass().getSimpleName() + " " + r + " " + item.quality.lines;
		assertEquals( what, lines, item.quality.lines.size() );
		//a SLAYER line draws its family too (aSlayerDrawsItsFamily)
		assertEquals( what, lines + (item.quality.has( RarityLine.SLAYER ) ? 1 : 0), c.ints );
		assertEquals( what, lines, c.floats );
	}

	//the spec's table of what each kind of weapon offers beside crit and lifesteal
	private static final EnumMap<WeaponPerk, EnumSet<RarityLine>> KIND = new EnumMap<>( WeaponPerk.class );
	static {
		KIND.put( WeaponPerk.MOMENTUM,  EnumSet.of( RarityLine.SLAYER, RarityLine.EXECUTIONER ) );
		KIND.put( WeaponPerk.KEEN,      EnumSet.of( RarityLine.MOONLIT, RarityLine.HUNTER ) );
		KIND.put( WeaponPerk.SWIFT,     EnumSet.of( RarityLine.MOONLIT, RarityLine.WARM_GRIP ) );
		KIND.put( WeaponPerk.CRUSH,     EnumSet.of( RarityLine.SLAYER, RarityLine.DEFIANT ) );
		KIND.put( WeaponPerk.WILD,      EnumSet.of( RarityLine.DEFIANT, RarityLine.STORMBORN ) );
		KIND.put( WeaponPerk.REACH,     EnumSet.of( RarityLine.STORMBORN, RarityLine.HUNTER ) );
		KIND.put( WeaponPerk.GUARD,     EnumSet.of( RarityLine.DEFIANT, RarityLine.SLAYER ) );
		KIND.put( WeaponPerk.SIPHON,    EnumSet.of( RarityLine.STORMBORN, RarityLine.WARM_GRIP ) );
		KIND.put( WeaponPerk.SCYTHE,    EnumSet.of( RarityLine.EXECUTIONER, RarityLine.MOONLIT ) );
		KIND.put( WeaponPerk.SHOT,      EnumSet.of( RarityLine.HUNTER, RarityLine.STEADY ) );
		KIND.put( WeaponPerk.GUN,       EnumSet.of( RarityLine.STEADY ) );
		KIND.put( WeaponPerk.DART,      EnumSet.of( RarityLine.HUNTER, RarityLine.STEADY ) );
		KIND.put( WeaponPerk.THROW,     EnumSet.of( RarityLine.STEADY, RarityLine.EXECUTIONER ) );
		KIND.put( WeaponPerk.IMPACT,    EnumSet.of( RarityLine.SLAYER, RarityLine.HUNTER ) );
		KIND.put( WeaponPerk.BOOMERANG, EnumSet.of( RarityLine.STEADY, RarityLine.SLAYER ) );
		KIND.put( WeaponPerk.WOODSMAN,  EnumSet.of( RarityLine.EXECUTIONER, RarityLine.WARM_GRIP ) );
		KIND.put( WeaponPerk.QUARRY,    EnumSet.of( RarityLine.SLAYER, RarityLine.WARM_GRIP ) );
		KIND.put( WeaponPerk.BARD,      EnumSet.of( RarityLine.MOONLIT, RarityLine.STORMBORN ) );
		KIND.put( WeaponPerk.LASH,      EnumSet.of( RarityLine.HUNTER, RarityLine.EXECUTIONER ) );
	}

	//weapons whose kind no regrouping moves
	private static final Supplier<?>[] STABLE = {
			Longsword::new, Mace::new, Sword::new, Glaive::new, Spear::new, Bow::new, SpiritBow::new, FT::new,
			MiniGun::new, Javelin::new, Dart::new, Boomerang::new, Sickle::new, MagesStaff::new, RoundShield::new,
			Gloves::new, Shortsword::new, Flail::new
	};

	//a weapon of each kind the rt2 pass added, and the weapons it moved to another kind: the
	//flails to the wild blows, the assassin's blade to Ambush, the fight gloves to Flurry, the
	//forest dart to the darts and the kunai to the light throws
	private static final Supplier<?>[] REGROUPED = {
			HandAxe::new, WarHammer::new, Flute::new, Whip::new,
			ChainFlail::new, LokisFlail::new, AssassinsBlade::new, FightGloves::new, ForestDart::new, Kunai::new
	};

	@Test
	public void everyKindRollsItsOwnTraitsAndNoOther(){
		Random.pushGenerator( 0x7EA1L );
		try {
			for (Supplier<?>[] group : new Supplier<?>[][]{ STABLE, REGROUPED }) for (Supplier<?> kind : group){
				Item sample = (Item) kind.get();
				Quality.Family fam = Quality.family( sample );
				EnumSet<RarityLine> want = EnumSet.of( RarityLine.CRIT, RarityLine.LIFESTEAL );
				want.addAll( KIND.get( WeaponPerk.of( sample ) ) );
				//the launcher lines, where a launcher fits them (RarityLineWeaponTest)
				for (RarityLine l : new RarityLine[]{ RarityLine.AMMO, RarityLine.RELOAD, RarityLine.RANGE }){
					if (l.fits( fam ) && l.fits( sample )) want.add( l );
				}
				String what = sample.getClass().getSimpleName() + " (" + WeaponPerk.of( sample ) + ")";
				assertEquals( what, want, EnumSet.copyOf( RarityLine.traits( fam, sample ) ) );
				EnumSet<RarityLine> seen = EnumSet.noneOf( RarityLine.class );
				for (int i = 0; i < 300; i++){
					Item item = (Item) kind.get();
					Quality.roll( item, i % 2 == 0 ? Rarity.UNCOMMON : Rarity.RARE, ItemType.GAMMA, 0 );
					for (RarityLine l : item.quality.lines){
						if (!l.trait()) continue;
						assertTrue( what + " rolled " + l, want.contains( l ) );
						seen.add( l );
					}
				}
				assertEquals( what, want, seen );
			}
		} finally {
			Random.popGenerator();
		}
	}

	private static final EnumSet<RarityLine> LIGHT = EnumSet.of( RarityLine.WARMTH, RarityLine.COOLING,
			RarityLine.RAINPROOF, RarityLine.FLEET, RarityLine.HUSHED );
	private static final EnumSet<RarityLine> HEAVY = EnumSet.of( RarityLine.WARMTH, RarityLine.WINDBREAK,
			RarityLine.SUREFOOT, RarityLine.RESOLVE );

	@Test
	public void everyArmorRollsItsWeightsTraitsAndNoOther(){
		int light = 0, heavy = 0;
		Random.pushGenerator( 0xA2A0L );
		try {
			for (Class<?> cls : Catalog.ARMOR.items()){
				Item sample = (Item) Reflection.newInstance( cls );
				GearPerk p = GearPerk.of( sample );
				boolean isHeavy = p == GearPerk.BULWARK || p == GearPerk.STALWART;
				if (isHeavy) heavy++; else light++;
				EnumSet<RarityLine> want = isHeavy ? HEAVY : LIGHT;
				String what = cls.getSimpleName() + " (" + p + ")";
				assertEquals( what, want, EnumSet.copyOf( RarityLine.traits( Quality.Family.ARMOR, sample ) ) );
				assertEquals( what, isHeavy, RarityLine.heavy( (xyz.gabriwar.warpedpixeldungeon.items.armor.Armor) sample ) );
				for (int i = 0; i < 60; i++){
					Item item = (Item) Reflection.newInstance( cls );
					Quality.roll( item, Rarity.UNCOMMON, ItemType.GAMMA, 0 );
					for (RarityLine l : item.quality.lines) assertTrue( what + " rolled " + l, want.contains( l ) );
				}
			}
		} finally {
			Random.popGenerator();
		}
		assertTrue( light > 3 && heavy > 3 );
	}

	@Test
	public void theCatalogNamesEachKindsTraits(){
		Item sword = Quality.sample( new Longsword() );
		assertTrue( sword.quality.describeRarityTier( sword, Rarity.COMMON ).contains( " Its line is a plain bonus." ) );
		assertTrue( sword.quality.describeRarityTier( sword, Rarity.UNCOMMON )
				.contains( " Its line is a trait: critical hits, lifesteal, slayer or executioner." ) );
		for (Rarity r : new Rarity[]{ Rarity.RARE, Rarity.LEGENDARY, Rarity.EXOTIC }){
			assertTrue( r.toString(), sword.quality.describeRarityTier( sword, r )
					.contains( " One line is a trait (critical hits, lifesteal, slayer or executioner), the other a plain bonus." ) );
		}
		Item plate = Quality.sample( new PlateArmor() );
		assertTrue( plate.quality.describeRarityTier( plate, Rarity.UNCOMMON )
				.contains( "trait: warmth, resolve, windbreak or sure footing." ) );
		Item gun = Quality.sample( new FT() );
		assertTrue( gun.quality.describeRarityTier( gun, Rarity.UNCOMMON )
				.contains( "trait: critical hits, lifesteal, spared ammunition, free reloads, range or steady aim." ) );
		//a wand fills no slots
		Item wand = Quality.sample( new WandOfMagicMissile() );
		assertFalse( wand.quality.describeRarityTier( wand, Rarity.RARE ).contains( "plain bonus" ) );
	}

	@Test
	public void aScorchBurnsThePlainLineFirst(){
		Random.pushGenerator( 0x5C02L );
		try {
			for (int i = 0; i < 50; i++){
				Longsword sword = new Longsword();
				Quality.roll( sword, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
				Quality q = sword.quality;
				assertEquals( 3, q.lines.size() );
				q.scorch( sword );
				assertEquals( RarityLine.VESTIGE, q.lines.get( 0 ) );
				assertTrue( q.lines.toString(), q.lines.get( 1 ).trait() );
				q.scorch( sword );
				assertEquals( 1, q.lines.size() );
				assertEquals( RarityLine.VESTIGE, q.lines.get( 0 ) );
			}
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void theFallbacksGiveAPlainLine(){
		Random.pushGenerator( 0xFA11L );
		try {
			for (int i = 0; i < 100; i++){
				for (Supplier<?> kind : SLOTTED){
					Item item = (Item) kind.get();
					Quality q = QualitySourceTest.only( item, Rarity.COMMON, 0f );
					q.masterworkStep( item );
					assertEquals( 1, q.lines.size() );
					assertFalse( q.lines.toString(), q.lines.get( 0 ).trait() );
				}
			}
		} finally {
			Random.popGenerator();
		}
	}
}
