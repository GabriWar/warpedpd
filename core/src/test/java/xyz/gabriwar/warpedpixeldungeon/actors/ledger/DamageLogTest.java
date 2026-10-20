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

package xyz.gabriwar.warpedpixeldungeon.actors.ledger;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The hero's two lists: each keeps its last hundred, newest first; a full log (two hundred real
 * records: blows, misses, procs, wand hits, both sides' buffs) goes into the hero's save and comes
 * back the same, in well under 256 KB of the save's text; a hero saved before the ledger loads with
 * both lists empty.
 */
public class DamageLogTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private LedgerStage stage;

	@Before
	public void setUp() throws Exception {
		stage = new LedgerStage();
		stage.up();
	}

	@After
	public void tearDown() throws Exception {
		stage.down();
	}

	@Test
	public void eachListKeepsItsLastHundredNewestFirst(){
		DamageLog log = new DamageLog();
		for (int i = 1; i <= 260; i++){
			HitRecord r = new HitRecord();
			r.serial = i;
			r.dealt = i % 2 == 0;
			log.add( r );
		}
		List<HitRecord> dealt = log.dealt(), taken = log.taken();
		assertEquals( DamageLog.KEEP, dealt.size() );
		assertEquals( DamageLog.KEEP, taken.size() );
		assertEquals( 260, dealt.get( 0 ).serial );
		assertEquals( 259, taken.get( 0 ).serial );
		assertEquals( 62, dealt.get( DamageLog.KEEP - 1 ).serial );
		for (HitRecord r : dealt) assertTrue( r.dealt );
		//a copy: changing it changes nothing kept
		dealt.clear();
		assertEquals( DamageLog.KEEP, log.dealt().size() );
		log.clear();
		assertEquals( 0, log.dealt().size() );
		assertEquals( 0, log.taken().size() );
	}

	//a full log of real hits: both lists at a hundred
	private Hero fullLog(){
		Hero h = stage.hero( at( 5, 6 ) );
		//(a hero's save writes his talents)
		Talent.initClassTalents( h );
		Sword s = new Sword();
		s.enchant( new Blazing() );
		s.level( 6 );
		s.identify();
		h.belongings.weapon = s;
		LeatherArmor a = new LeatherArmor();
		a.identify();
		h.belongings.armor = a;
		Buff.affect( h, Bless.class, 1000f );
		Buff.affect( h, Haste.class, 1000f );
		Buff.affect( h, Barrier.class ).setShield( 200 );
		Rat r = stage.rat( at( 6, 6 ), 99999 );
		Buff.affect( r, Vulnerable.class, 1000f );
		Buff.affect( r, Weakness.class, 1000f );
		WandOfMagicMissile wand = new WandOfMagicMissile();
		Random.pushGenerator( 777 );
		try {
			for (int i = 0; i < 140; i++){
				h.attack( r );
				r.attack( h );
				if (i % 4 == 0) r.damage( wand.damageRoll(), wand );
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( DamageLog.KEEP, h.damageLog.dealt().size() );
		assertEquals( DamageLog.KEEP, h.damageLog.taken().size() );
		return h;
	}

	@Test
	public void aFullLogRoundTripsInTheSaveAndStaysSmall() throws Exception {
		Hero h = fullLog();
		Bundle saved = new Bundle();
		h.damageLog.storeInBundle( saved );
		String json = saved.toString();
		assertTrue( "a full log is " + json.length() + " characters", json.length() < 256 * 1024 );

		Bundle read = Bundle.read( new ByteArrayInputStream( json.getBytes( StandardCharsets.UTF_8 ) ) );
		DamageLog back = new DamageLog();
		back.restoreFromBundle( read );
		same( h.damageLog.dealt(), back.dealt(), true );
		same( h.damageLog.taken(), back.taken(), false );
	}

	private static void same( List<HitRecord> a, List<HitRecord> b, boolean dealt ){
		assertEquals( a.size(), b.size() );
		for (int i = 0; i < a.size(); i++){
			HitRecord x = a.get( i ), y = b.get( i );
			assertEquals( dealt, y.dealt );
			assertTrue( y.sums() );
			assertEquals( x.serial, y.serial );
			assertEquals( x.parent, y.parent );
			assertEquals( x.kind, y.kind );
			assertEquals( x.outcome, y.outcome );
			assertEquals( x.turn, y.turn, 0f );
			assertEquals( x.depth, y.depth );
			assertEquals( x.sourceName, y.sourceName );
			assertEquals( x.sourceClass, y.sourceClass );
			assertEquals( x.sourceIcon, y.sourceIcon );
			assertEquals( x.sourceItem, y.sourceItem );
			assertEquals( x.targetId, y.targetId );
			assertEquals( x.dr, y.dr );
			assertEquals( x.drRaw, y.drRaw );
			assertEquals( x.request, y.request );
			assertEquals( x.shown, y.shown );
			assertEquals( x.hpLost, y.hpLost );
			assertEquals( x.shieldLost, y.shieldLost );
			assertEquals( x.overkill, y.overkill );
			assertEquals( x.crit, y.crit );
			assertEquals( x.killed, y.killed );
			assertEquals( x.executed, y.executed );
			assertEquals( x.cardIcon, y.cardIcon );
			assertEquals( x.cardColor, y.cardColor );
			assertArrayEquals( x.chips, y.chips );
			assertArrayEquals( x.chipTints, y.chipTints );
			assertEquals( x.ticks, y.ticks );
			steps( x.steps, y.steps );
			side( x.attacker, y.attacker );
			side( x.defender, y.defender );
			if (x.roll == null){
				assertNull( y.roll );
			} else {
				assertNotNull( y.roll );
				assertEquals( x.roll.hit, y.roll.hit );
				assertEquals( x.roll.sure, y.roll.sure );
				assertEquals( x.roll.untouchable, y.roll.untouchable );
				assertEquals( x.roll.reasonIcon, y.roll.reasonIcon );
				assertEquals( x.roll.accStat, y.roll.accStat, 0f );
				assertEquals( x.roll.defStat, y.roll.defStat, 0f );
				assertEquals( x.roll.accRoll, y.roll.accRoll, 0f );
				assertEquals( x.roll.defRoll, y.roll.defRoll, 0f );
				assertEquals( x.roll.chance(), y.roll.chance(), 0f );
				steps( x.roll.accSteps, y.roll.accSteps );
				steps( x.roll.defSteps, y.roll.defSteps );
				steps( x.roll.accRollSteps, y.roll.accRollSteps );
				steps( x.roll.defRollSteps, y.roll.defRollSteps );
			}
		}
	}

	private static void steps( Step[] a, Step[] b ){
		assertEquals( a.length, b.length );
		for (int i = 0; i < a.length; i++){
			assertEquals( a[i].op, b[i].op );
			assertEquals( a[i].side, b[i].side );
			assertEquals( a[i].icon, b[i].icon );
			assertEquals( a[i].tint, b[i].tint );
			assertEquals( a[i].label, b[i].label );
			assertEquals( a[i].before, b[i].before, 0f );
			assertEquals( a[i].after, b[i].after, 0f );
		}
	}

	private static void side( Side a, Side b ){
		if (a == null){
			assertNull( b );
			return;
		}
		assertNotNull( b );
		assertEquals( a.name, b.name );
		assertEquals( a.cls, b.cls );
		assertEquals( a.hero, b.hero );
		assertEquals( a.heroClass, b.heroClass );
		assertEquals( a.heroTier, b.heroTier );
		assertEquals( a.hp, b.hp );
		assertEquals( a.ht, b.ht );
		assertEquals( a.shield, b.shield );
		assertArrayEquals( a.buffIcons, b.buffIcons );
		assertArrayEquals( a.buffTints, b.buffTints );
		assertArrayEquals( a.buffNames, b.buffNames );
		assertArrayEquals( a.buffClasses, b.buffClasses );
		assertEquals( a.weapon, b.weapon );
		assertEquals( a.armor, b.armor );
		assertEquals( a.weaponRarity, b.weaponRarity );
		assertEquals( a.armorRarity, b.armorRarity );
		assertEquals( a.weaponColor, b.weaponColor );
		assertEquals( a.armorColor, b.armorColor );
	}

	@Test
	public void theHerosSaveKeepsItsLogAndAnOldOneLoadsEmpty() throws Exception {
		Hero h = fullLog();
		Bundle saved = new Bundle();
		saved.put( "hero", h );
		Bundle withLog = Bundle.read( new ByteArrayInputStream( saved.toString().getBytes( StandardCharsets.UTF_8 ) ) );
		Hero back = (Hero) withLog.get( "hero" );
		assertEquals( DamageLog.KEEP, back.damageLog.dealt().size() );
		assertEquals( DamageLog.KEEP, back.damageLog.taken().size() );
		assertEquals( h.damageLog.dealt().get( 0 ).serial, back.damageLog.dealt().get( 0 ).serial );

		//a save from before the ledger: no log in it
		Bundle old = Bundle.read( new ByteArrayInputStream( saved.toString().getBytes( StandardCharsets.UTF_8 ) ) );
		assertTrue( old.getBundle( "hero" ).remove( "damage_log" ) );
		assertFalse( old.getBundle( "hero" ).contains( "damage_log" ) );
		Hero before = (Hero) old.get( "hero" );
		assertEquals( 0, before.damageLog.dealt().size() );
		assertEquals( 0, before.damageLog.taken().size() );
	}

	@Test
	public void aRankingKeepsTheHeroWithoutHisLog(){
		Hero h = fullLog();
		assertEquals( DamageLog.KEEP, h.damageLog.dealt().size() );
		//(a ranking writes which scrolls, potions and rings were known)
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		xyz.gabriwar.warpedpixeldungeon.items.rings.Ring.initGems();
		xyz.gabriwar.warpedpixeldungeon.Rankings.Record rec = new xyz.gabriwar.warpedpixeldungeon.Rankings.Record();
		xyz.gabriwar.warpedpixeldungeon.Rankings.INSTANCE.saveGameData( rec );
		Bundle ranked = rec.gameData.getBundle( xyz.gabriwar.warpedpixeldungeon.Rankings.HERO );
		assertFalse( ranked.isNull() );
		assertTrue( "the ranked hero is kept", ranked.contains( "lvl" ) || ranked.getKeys().size() > 5 );
		assertFalse( "but not his 200 hits", ranked.contains( Hero.DAMAGE_LOG ) );
		//the game's own hero keeps them
		assertEquals( DamageLog.KEEP, h.damageLog.dealt().size() );
	}
}
