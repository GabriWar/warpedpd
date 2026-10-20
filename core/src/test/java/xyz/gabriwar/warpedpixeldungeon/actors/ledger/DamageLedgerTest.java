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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Preparation;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.TimeStasis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.MailArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Mace;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.TreeFall;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaveSites;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Group;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The damage ledger, headless, on real fights (the enchantments' stage: stand-in layers, a GPU that
 * says yes, the clock held): every record of some 250 seeded hits of every kind (the hero's sword
 * and thrown knife on a rat, a rat on the hero in armour and behind a shield, under Weakness,
 * Vulnerable and Doom, a wand on a rat that resists it) sums exactly to what it showed, names every
 * rule (no "Other rules"), and accounts for every point of health and shield the struck one lost; a
 * miss keeps its roll; a Blazing proc is its own record under its blow; bleeding, time stopped and
 * an invulnerable target close with their outcome; the ledger draws nothing of the game's Random;
 * a run of poison ticks is one record; the card a hit showed comes back to its record from either
 * thread; a co-op guest keeps nothing.
 */
public class DamageLedgerTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private LedgerStage stage;
	private EnchantFxTest.Rig rig;

	@Before
	public void setUp() throws Exception {
		stage = new LedgerStage();
		stage.up();
		rig = stage.rig;
	}

	@After
	public void tearDown() throws Exception {
		stage.down();
	}

	private Hero hero( int cell ){
		return stage.hero( cell );
	}

	private <M extends Mob> M body( M m, int cell, int hp ){
		return stage.body( m, cell, hp );
	}

	private Rat rat( int cell, int hp ){
		return stage.rat( cell, hp );
	}

	/** A rat that has seen the hero: it is not surprised, so it dodges. */
	public static class AlertRat extends Rat {
		{
			enemySeen = true;
		}
	}

	/** A rat that cannot miss. */
	public static class SureRat extends Rat {
		@Override
		public int attackSkill( Char target ){
			return INFINITE_ACCURACY;
		}
	}

	private static List<HitRecord> all(){
		List<HitRecord> l = new ArrayList<>( Dungeon.hero.damageLog.dealt() );
		l.addAll( Dungeon.hero.damageLog.taken() );
		return l;
	}

	//the records a run added (by identity), oldest first
	private static List<HitRecord> fresh( Runnable run ){
		IdentityHashMap<HitRecord, Boolean> before = new IdentityHashMap<>();
		for (HitRecord r : all()) before.put( r, Boolean.TRUE );
		run.run();
		List<HitRecord> out = new ArrayList<>();
		for (HitRecord r : all()) if (!before.containsKey( r )) out.add( r );
		return out;
	}

	private static String show( HitRecord r ){
		return r.sourceName + " outcome " + r.outcome + " shown " + r.shown + " hp " + r.hpLost + " shield " + r.shieldLost
				+ " overkill " + r.overkill + " steps " + Arrays.toString( r.steps );
	}

	/** One hit, checked: every record it made sums and names all its rules, and they account for
	 *  every point of health and shield the struck one lost. Returns its records. */
	private static List<HitRecord> strike( Char target, Runnable hit ){
		int hp0 = target.HP, sh0 = target.shielding();
		List<HitRecord> made = fresh( hit );
		int lost = 0, shield = 0;
		for (HitRecord r : made){
			assertTrue( "sums: " + show( r ), r.sums() );
			assertEquals( "every rule named: " + show( r ), 0, r.unnamed() );
			if (r.outcome == HitRecord.MISS){
				assertNotNull( "a miss keeps its roll", r.roll );
				assertFalse( r.roll.hit );
				assertEquals( 0, r.steps.length );
			}
			if (r.targetId == target.id()){
				lost += r.hpLost - r.overkill;
				shield += r.shieldLost;
			}
		}
		assertEquals( "health lost is what the records took", hp0 - Math.max( 0, target.HP ), lost );
		assertEquals( "shield lost is what the records took", sh0 - target.shielding(), shield );
		return made;
	}

	// ------------------------------------------------------------------ the invariant

	@Test
	public void everyHitOfEveryKindSumsToWhatItTook(){
		Random.pushGenerator( 9001 );
		try {
			Hero h = hero( at( 5, 6 ) );
			Sword sword = new Sword();
			sword.identify();
			h.belongings.weapon = sword;
			LeatherArmor armor = new LeatherArmor();
			armor.identify();
			h.belongings.armor = armor;
			Rat r = rat( at( 6, 6 ), 99999 );
			Rat other = rat( at( 6, 7 ), 99999 );
			int blows = 0, misses = 0, taken = 0;

			//the hero's sword, then his thrown knives, on rats in every state
			for (int i = 0; i < 120; i++){
				if (i == 20) Buff.affect( h, Weakness.class, 1000f );
				if (i == 40) Buff.affect( r, Vulnerable.class, 1000f );
				if (i == 60) Buff.affect( r, Doom.class );
				if (i == 80){
					Buff.detach( h, Weakness.class );
					h.belongings.thrownWeapon = new ThrowingKnife();
				}
				final Rat target = i % 3 == 0 ? other : r;
				for (HitRecord rec : strike( target, () -> h.attack( target ) )){
					if (rec.kind == HitRecord.BLOW) blows++;
					if (rec.outcome == HitRecord.MISS) misses++;
				}
			}
			h.belongings.thrownWeapon = null;

			//rats on the hero: in armour, then behind a shield
			for (int i = 0; i < 100; i++){
				if (i == 50) Buff.affect( h, Barrier.class ).setShield( 60 );
				final Rat by = i % 2 == 0 ? r : other;
				for (HitRecord rec : strike( h, () -> by.attack( h ) )){
					assertFalse( rec.dealt );
					taken++;
					if (rec.outcome == HitRecord.MISS) misses++;
				}
			}

			//a wand on a rat that resists it, by its roll
			addResistance( r, WandOfMagicMissile.class );
			WandOfMagicMissile wand = new WandOfMagicMissile();
			wand.level( 3 );
			for (int i = 0; i < 30; i++){
				List<HitRecord> made = strike( r, () -> r.damage( wand.damageRoll(), wand ) );
				assertEquals( 1, made.size() );
				HitRecord z = made.get( 0 );
				assertTrue( z.dealt );
				assertEquals( HitRecord.DIRECT, z.kind );
				assertTrue( "starts at the wand's roll: " + show( z ), z.steps[0].label.contains( " to " ) );
				assertTrue( "the resistance is a step: " + show( z ), has( z, DamageLedger.RESIST.text() ) );
			}

			assertTrue( "blows were kept: " + blows, blows >= 100 );
			assertTrue( "misses were kept: " + misses, misses > 0 );
			assertTrue( "hits taken were kept: " + taken, taken >= 100 );
			//the rules the setups put in are named on the hits they touched
			assertTrue( anyStep( Dungeon.hero.damageLog.dealt(), "Weakness" ) || anyStep( Dungeon.hero.damageLog.dealt(), "weak" ) );
			assertTrue( anyStep( Dungeon.hero.damageLog.taken(), DamageLedger.ARMOR.text() ) );
		} finally {
			Random.popGenerator();
		}
	}

	private static boolean has( HitRecord r, String label ){
		for (Step s : r.steps) if (s.label.equals( label )) return true;
		return false;
	}

	private static boolean anyStep( List<HitRecord> l, String part ){
		for (HitRecord r : l) for (Step s : r.steps) if (s.label.toLowerCase().contains( part.toLowerCase() )) return true;
		return false;
	}

	private static void addResistance( Char ch, Class<?> c ){
		try {
			Field f = Char.class.getDeclaredField( "resistances" );
			f.setAccessible( true );
			@SuppressWarnings("unchecked") HashSet<Class<?>> set = (HashSet<Class<?>>) f.get( ch );
			set.add( c );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void rarityGearIsNamedRuleByRule(){
		Random.pushGenerator( 6060 );
		try {
			Hero h = hero( at( 5, 6 ) );
			Rat r = rat( at( 6, 6 ), 999999 );
			//a rat that sees the hero coming dodges (a passive one is always surprised)
			Rat hunting = body( new AlertRat(), at( 6, 5 ), 999999 );
			int misses = 0, crits = 0;
			Weapon[] weapons = { new Dagger(), new Sword(), new Spear(), new Mace(), new Greataxe(), new Whip(), new ThrowingKnife() };
			Armor[] armors = { new LeatherArmor(), new MailArmor(), new PlateArmor() };
			for (int round = 0; round < 3; round++){
				for (Weapon w : weapons){
					Quality.roll( w, Rarity.LEGENDARY, round == 0 ? ItemType.ALPHA : round == 1 ? ItemType.BETA : ItemType.GAMMA, 0 );
					//its lines and perk, not a vestige (a Midas one drops gold, which wants a game scene)
					w.enchant( null );
					w.quality.vestige = null;
					w.identify();
					if (w instanceof ThrowingKnife){
						h.belongings.thrownWeapon = (ThrowingKnife) w;
					} else {
						h.belongings.thrownWeapon = null;
						h.belongings.weapon = w;
					}
					Armor a = armors[round];
					Quality.roll( a, Rarity.LEGENDARY, ItemType.ALPHA, 0 );
					a.inscribe( null );
					a.quality.vestige = null;
					a.identify();
					h.belongings.armor = a;
					for (int i = 0; i < 12; i++){
						final Rat target = i % 2 == 0 ? r : hunting;
						for (HitRecord rec : strike( target, () -> h.attack( target ) )){
							if (rec.outcome == HitRecord.MISS) misses++;
							if (rec.crit) crits++;
						}
						final Rat by = i % 2 == 0 ? r : hunting;
						strike( h, () -> by.attack( h ) );
					}
				}
			}
			h.belongings.thrownWeapon = null;
			assertTrue( "the hero missed sometimes: " + misses, misses > 0 );
			assertTrue( "crits were kept: " + crits, crits > 0 );
		} finally {
			Random.popGenerator();
		}
	}

	//a rogue's preparation, as if he had stood unseen for `turns` turns
	private static Preparation prepared( Hero h, int turns ){
		Preparation p = Buff.affect( h, Preparation.class );
		try {
			Field f = Preparation.class.getDeclaredField( "turnsInvis" );
			f.setAccessible( true );
			f.set( p, turns );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
		return p;
	}

	@Test
	public void preparationsBestRollAndKnockOutAreSteps(){
		Random.pushGenerator( 1313 );
		try {
			Hero h = hero( at( 5, 6 ) );
			h.belongings.weapon = new Sword();
			Rat r = rat( at( 6, 6 ), 99999 );
			String prep = null;
			for (int i = 0; i < 10; i++){
				prepared( h, 9 );
				for (HitRecord rec : strike( r, () -> h.attack( r ) )){
					if (rec.kind != HitRecord.BLOW || rec.outcome != HitRecord.HIT) continue;
					for (Step st : rec.steps) if (st.label.contains( "3" ) && st.label.startsWith( DamageLedger.text( new Preparation() ) )) prep = st.label;
				}
			}
			assertNotNull( "the best of three rolls, with its bonus, is a named step", prep );

			//a knock-out: whatever health was left is the blow's too
			HitRecord ko = null;
			for (int i = 0; i < 20 && ko == null; i++){
				Rat weak = rat( at( 6, 7 ), 1000 );
				weak.HP = 40;
				prepared( h, 9 );
				for (HitRecord rec : strike( weak, () -> h.attack( weak ) )){
					if (rec.kind == HitRecord.BLOW && rec.executed) ko = rec;
				}
				Dungeon.level.mobs.remove( weak );
				Actor.remove( weak );
			}
			assertNotNull( ko );
			assertTrue( ko.killed );
			assertEquals( 40, ko.hpLost - ko.overkill );
			assertEquals( DamageLedger.text( new Preparation() ), ko.steps[ko.steps.length - 1].label );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void aKillTakesWhatWasLeftAndKeepsTheOverkill(){
		Random.pushGenerator( 77 );
		try {
			Hero h = hero( at( 5, 6 ) );
			h.belongings.weapon = new Sword();
			HitRecord kill = null;
			for (int i = 0; i < 40 && kill == null; i++){
				Rat r = rat( at( 6, 6 ), 3 );
				for (HitRecord rec : strike( r, () -> h.attack( r ) )){
					if (rec.killed && rec.kind == HitRecord.BLOW) kill = rec;
				}
				Dungeon.level.mobs.remove( r );
				Actor.remove( r );
			}
			assertNotNull( kill );
			assertEquals( 3, kill.hpLost - kill.overkill );
			assertEquals( kill.shown, kill.hpLost );
		} finally {
			Random.popGenerator();
		}
	}

	// ------------------------------------------------------------------ side damage and outcomes

	@Test
	public void aBlazingProcIsItsOwnRecordUnderItsBlow(){
		Random.pushGenerator( 31 );
		try {
			Hero h = hero( at( 5, 6 ) );
			Sword s = new Sword();
			s.enchant( new Blazing() );
			s.level( 30 );
			s.identify();
			h.belongings.weapon = s;
			Rat r = rat( at( 6, 6 ), 99999 );
			HitRecord proc = null;
			for (int i = 0; i < 60 && proc == null; i++){
				for (HitRecord rec : strike( r, () -> h.attack( r ) )){
					if (rec.kind != HitRecord.BLOW && Blazing.class.getName().equals( rec.sourceClass )) proc = rec;
				}
			}
			assertNotNull( "a Blazing proc was kept", proc );
			assertTrue( proc.dealt );
			assertNotEquals( 0, proc.parent );
			boolean parentKept = false;
			for (HitRecord rec : Dungeon.hero.damageLog.dealt()) if (rec.kind == HitRecord.BLOW && rec.serial == proc.parent) parentKept = true;
			assertTrue( "its blow is kept, by its serial", parentKept );

			//the blow met the rat as the proc left it, and its card summed the two
			HitRecord blow = null;
			for (HitRecord rec : Dungeon.hero.damageLog.dealt()) if (rec.kind == HitRecord.BLOW && rec.serial == proc.parent) blow = rec;
			if (blow.outcome == HitRecord.HIT){
				assertEquals( "health as the damage met it", proc.defender.hp - (proc.hpLost - proc.overkill), blow.defender.hp );
				int withIt = blow.total();
				for (HitRecord rec : Dungeon.hero.damageLog.dealt()){
					if (rec != blow && rec.parent == blow.serial && rec.targetId == blow.targetId && rec.outcome == HitRecord.HIT) withIt += rec.total();
				}
				assertEquals( "its card's number: the blow and its proc", withIt, blow.cardValue );
			}
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void anExecuteInTheAttackEndsTheChainOnWhatItTook(){
		//Silent Death finishes a sleeper inside the hero's attack, before the blow's damage is asked
		//for: the record ends on the health it took, with no unnamed step to the request and back
		Hero h = hero( at( 5, 6 ) );
		Rat r = rat( at( 6, 6 ), 30 );
		DamageLedger.openBlow( h, r, 1f, 0f, 1f );
		try {
			DamageLedger.beginBaseRoll();
			DamageLedger.endBaseRoll( 8f );
			DamageLedger.execute( r, "Silent Death", r.HP );
			DamageLedger.expectMain( r, 8 );
		} finally {
			DamageLedger.closeBlow();
		}
		HitRecord rec = Dungeon.hero.damageLog.dealt().get( 0 );
		assertTrue( show( rec ), rec.sums() );
		assertEquals( show( rec ), 0, rec.unnamed() );
		assertTrue( rec.executed );
		assertEquals( 30, rec.shown );
		assertEquals( 8, rec.request );
		assertEquals( "Silent Death", rec.steps[rec.steps.length - 1].label );
	}

	@Test
	public void aGuardIsANamedStepOnTheEvasion(){
		Random.pushGenerator( 21 );
		try {
			Hero h = hero( at( 5, 6 ) );
			Rat r = rat( at( 6, 6 ), 99999 );
			xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield.GuardTracker guard =
					Buff.affect( h, xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield.GuardTracker.class, 50f );
			List<HitRecord> made = strike( h, () -> r.attack( h ) );
			assertEquals( 1, made.size() );
			HitRecord miss = made.get( 0 );
			assertEquals( HitRecord.MISS, miss.outcome );
			assertTrue( miss.roll.untouchable );
			Step[] def = miss.roll.defSteps;
			assertEquals( "the hero's own evasion first", Step.BASE, def[0].op );
			Step last = def[def.length - 1];
			assertEquals( Step.SET, last.op );
			assertEquals( "the guard names the step that made him untouchable", DamageLedger.text( guard ), last.label );
			assertEquals( Char.INFINITE_EVASION, last.after, 0f );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void bleedingTimeStoppedAndInvulnerableCloseWithTheirOutcome(){
		Random.pushGenerator( 5 );
		try {
			Hero h = hero( at( 5, 6 ) );
			h.belongings.weapon = new Sword();
			Rat r = rat( at( 6, 6 ), 99999 );

			//a harvest blow becomes bleeding: nothing taken now
			HitRecord bleed = null;
			for (int i = 0; i < 30 && bleed == null; i++){
				Buff.affect( h, Sickle.HarvestBleedTracker.class, 5f );
				for (HitRecord rec : strike( r, () -> h.attack( r ) )){
					if (rec.outcome == HitRecord.BLEED) bleed = rec;
				}
			}
			assertNotNull( bleed );
			assertEquals( 0, bleed.shown );
			assertEquals( Step.SET, bleed.steps[bleed.steps.length - 1].op );
			assertEquals( 0f, bleed.steps[bleed.steps.length - 1].after, 0f );
			assertNotNull( r.buff( Bleeding.class ) );
			Buff.detach( h, Sickle.HarvestBleedTracker.class );

			//time stopped: a sure rat's blow and a direct hit take nothing
			SureRat sure = body( new SureRat(), at( 4, 6 ), 50 );
			Buff.affect( h, TimeStasis.class );
			List<HitRecord> stopped = strike( h, () -> sure.attack( h ) );
			assertEquals( 1, stopped.size() );
			assertEquals( HitRecord.STASIS, stopped.get( 0 ).outcome );
			assertEquals( 0, stopped.get( 0 ).shown );
			stopped = strike( h, () -> h.damage( 7, sure ) );
			assertEquals( 1, stopped.size() );
			assertEquals( HitRecord.STASIS, stopped.get( 0 ).outcome );
			Buff.detach( h, TimeStasis.class );

			//an invulnerable rat: the blow is kept as immune, with no roll and no steps
			Buff.affect( r, Invulnerability.class, 10f );
			List<HitRecord> immune = strike( r, () -> h.attack( r ) );
			assertEquals( 1, immune.size() );
			assertEquals( HitRecord.IMMUNE, immune.get( 0 ).outcome );
			assertNull( immune.get( 0 ).roll );
			assertEquals( 0, immune.get( 0 ).steps.length );
		} finally {
			Random.popGenerator();
		}
	}

	// ------------------------------------------------------------------ draws nothing

	//a mixed seeded fight: the game's Random's next number after it and every health
	private long[] fight( boolean recording ){
		Actor.clear();
		Dungeon.level.mobs = new LinkedHashSet<>();
		DamageLedger.recording = recording;
		Random.pushGenerator( 2468 );
		try {
			Hero h = hero( at( 5, 6 ) );
			Sword s = new Sword();
			s.enchant( new Blazing() );
			s.level( 5 );
			h.belongings.weapon = s;
			h.belongings.armor = new LeatherArmor();
			Rat a = rat( at( 6, 6 ), 5000 ), b = rat( at( 4, 6 ), 5000 );
			Buff.affect( a, Vulnerable.class, 1000f );
			Buff.affect( h, Barrier.class ).setShield( 30 );
			WandOfMagicMissile wand = new WandOfMagicMissile();
			for (int i = 0; i < 40; i++){
				h.attack( i % 2 == 0 ? a : b );
				a.attack( h );
				b.attack( h );
				if (i % 5 == 0) b.damage( wand.damageRoll(), wand );
			}
			return new long[]{ Random.Long(), h.HP, h.shielding(), a.HP, b.HP };
		} finally {
			Random.popGenerator();
			DamageLedger.recording = true;
		}
	}

	@Test
	public void theLedgerDrawsNothingOfTheGamesRandom(){
		long[] off = fight( false );
		assertEquals( 0, Dungeon.hero.damageLog.dealt().size() );
		long[] on = fight( true );
		assertTrue( Dungeon.hero.damageLog.dealt().size() > 0 );
		assertEquals( Arrays.toString( off ), Arrays.toString( on ) );
	}

	// ------------------------------------------------------------------ the lists

	@Test
	public void eachListKeepsItsLastHundredNewestFirst(){
		Random.pushGenerator( 12 );
		try {
			Hero h = hero( at( 5, 6 ) );
			h.belongings.weapon = new Sword();
			Rat r = rat( at( 6, 6 ), 999999 );
			for (int i = 0; i < 130; i++) h.attack( r );
			for (int i = 0; i < 130; i++) r.attack( h );
			List<HitRecord> dealt = h.damageLog.dealt(), taken = h.damageLog.taken();
			assertEquals( DamageLog.KEEP, dealt.size() );
			assertEquals( DamageLog.KEEP, taken.size() );
			for (int i = 1; i < dealt.size(); i++) assertTrue( dealt.get( i - 1 ).serial > dealt.get( i ).serial );
			for (int i = 1; i < taken.size(); i++) assertTrue( taken.get( i - 1 ).serial > taken.get( i ).serial );
			for (HitRecord rec : dealt) assertTrue( rec.dealt );
			for (HitRecord rec : taken) assertFalse( rec.dealt );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void poisonTicksFoldIntoOneRunThatStillSums(){
		Hero h = hero( at( 5, 6 ) );
		//(the ticks' source, not attached: an attached poison's bubbles want a game scene)
		Poison p = new Poison();
		int hp0 = h.HP;
		for (int i = 0; i < 12; i++) h.damage( 1 + i % 3, p );
		List<HitRecord> taken = h.damageLog.taken();
		assertEquals( 1, taken.size() );
		HitRecord run = taken.get( 0 );
		assertEquals( HitRecord.DOT, run.kind );
		assertEquals( 12, run.ticks );
		assertEquals( 12, run.tickAmounts.length );
		assertEquals( hp0 - h.HP, run.total() );
		assertTrue( show( run ), run.sums() );
		assertEquals( 0, run.unnamed() );
		//a run is at most MAX_TICKS long; another source starts its own
		for (int i = 0; i < DamageLedger.MAX_TICKS; i++) h.damage( 1, p );
		assertEquals( 2, h.damageLog.taken().size() );
		assertEquals( DamageLedger.MAX_TICKS, h.damageLog.taken().get( 1 ).ticks );
		assertEquals( 12, h.damageLog.taken().get( 0 ).ticks );
		h.damage( 3, new Bleeding() );
		assertEquals( 3, h.damageLog.taken().size() );
		for (HitRecord rec : h.damageLog.taken()) assertTrue( show( rec ), rec.sums() );
	}

	@Test
	public void theWorldsOwnBlowsAreNamedAndExplained(){
		Hero h = hero( at( 5, 6 ) );
		Object[] sources = { new Chasm(), new Heap(), TreeFall.class, CaveSites.class };
		for (Object src : sources){
			h.damage( 5, src );
			HitRecord r = h.damageLog.taken().get( 0 );
			assertEquals( src.getClass() == Class.class ? ((Class<?>) src).getName() : src.getClass().getName(), r.sourceClass );
			String name = DamageLedger.sourceName( r );
			assertNotEquals( src.toString(), DamageLedger.SOMETHING.text(), name );
			assertEquals( name, r.sourceName );
			assertNotNull( src.toString(), DamageLedger.sourceDesc( r ) );
			assertTrue( show( r ), r.sums() );
		}
		List<HitRecord> taken = h.damageLog.taken();
		assertEquals( "Fall", DamageLedger.sourceName( taken.get( taken.size() - 1 ) ) );
		//a fall kept before falls had a name reads as one now
		HitRecord old = new HitRecord();
		old.sourceName = DamageLedger.SOMETHING.text();
		old.sourceClass = Chasm.class.getName();
		assertEquals( "Fall", DamageLedger.sourceName( old ) );
		//anything else has no explanation and keeps the name it was given
		old.sourceClass = Poison.class.getName();
		assertNull( DamageLedger.sourceDesc( old ) );
		assertEquals( DamageLedger.SOMETHING.text(), DamageLedger.sourceName( old ) );
	}

	// ------------------------------------------------------------------ the card it showed

	@Test
	public void theCardAHitShowedComesBackToItsRecordFromEitherThread() throws Exception {
		Random.pushGenerator( 404 );
		try {
			Hero h = hero( at( 5, 6 ) );
			h.belongings.weapon = new Sword();
			Rat r = rat( at( 6, 6 ), 99999 );
			//on the render thread: the presenter runs inside the blow's close
			HitRecord seen = null;
			for (int i = 0; i < 20 && seen == null; i++){
				for (HitRecord rec : fresh( () -> h.attack( r ) )){
					if (rec.kind == HitRecord.BLOW && rec.outcome == HitRecord.HIT) seen = rec;
				}
				rig.frames( 40, true );
			}
			assertNotNull( seen );
			assertNotEquals( "its card's icon", -1, seen.cardIcon );

			//on the actor thread: the presenter runs later, on the render thread
			HitRecord later = null;
			for (int i = 0; i < 20 && later == null; i++){
				final List<HitRecord> made = new ArrayList<>();
				List<Runnable> posted = onActorThread( () -> made.addAll( fresh( () -> h.attack( r ) ) ) );
				HitRecord hit = null;
				for (HitRecord rec : made) if (rec.kind == HitRecord.BLOW && rec.outcome == HitRecord.HIT) hit = rec;
				if (hit != null){
					assertEquals( "no card before the render thread draws it", -1, hit.cardIcon );
					for (Runnable p : posted) p.run();
					later = hit;
				}
				rig.frames( 40, true );
			}
			assertNotNull( later );
			assertNotEquals( -1, later.cardIcon );
		} finally {
			Random.popGenerator();
		}
	}

	//a run on another thread, what it posted to the render thread handed back
	private static List<Runnable> onActorThread( Runnable run ) throws Exception {
		final List<Runnable> posted = new ArrayList<>();
		Application saved = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				( p, m, args ) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) args[0] );
						}
						return null;
					}
					return saved != null ? m.invoke( saved, args ) : null;
				} );
		final Throwable[] failed = new Throwable[1];
		try {
			Thread t = new Thread( () -> {
				try {
					run.run();
				} catch (Throwable e){
					failed[0] = e;
				}
			} );
			t.start();
			t.join();
		} finally {
			Gdx.app = saved;
		}
		if (failed[0] != null) throw new AssertionError( "on the actor thread", failed[0] );
		return posted;
	}

	// ------------------------------------------------------------------ co-op

	@Test
	public void aCoopGuestKeepsNothing() throws Exception {
		Hero h = hero( at( 5, 6 ) );
		h.belongings.weapon = new Sword();
		Rat r = rat( at( 6, 6 ), 99999 );
		TestParty.guest();
		Random.pushGenerator( 8 );
		try {
			for (int i = 0; i < 20; i++){
				h.attack( r );
				r.attack( h );
			}
			h.damage( 3, r );
		} finally {
			Random.popGenerator();
			TestParty.leave();
		}
		assertEquals( 0, h.damageLog.dealt().size() );
		assertEquals( 0, h.damageLog.taken().size() );
	}
}
