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

package xyz.gabriwar.warpedpixeldungeon.actors;

import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Berserk;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ElementalOrbit;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FletchingFeathers;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MasterThiefCoins;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.AimedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BloodRush;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Carnage;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CinderTrail;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CrusadersZeal;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Fletching;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.IronStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MasterThief;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.NinjaBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.RecklessFury;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Smash;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Spark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpiritArmor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Stealth;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SummonRat;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WeaponBond;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.SkillDebug;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** Skills of other classes given to a hero by the debug window: they work, save, and go again. */
public class ForeignSkillsTest {

	@BeforeClass public static void loadAssets(){
		xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
	}

	private Hero previousHero;
	private Level previousLevel;
	private String previousVersion;
	private Hero hero;

	@Before public void setUp(){
		previousHero = Dungeon.hero;
		previousLevel = Dungeon.level;
		previousVersion = Game.version;
		Game.version = "test";
		hero = heroOf( HeroClass.MAGE );
	}

	@After public void tearDown(){
		Dungeon.hero = previousHero;
		Dungeon.level = previousLevel;
		Game.version = previousVersion;
		if (previousLevel != null) PathFinder.setMapSize( previousLevel.width(), previousLevel.height() );
	}

	private static Hero heroOf( HeroClass cls ){
		Hero h = new Hero();
		Dungeon.hero = h;
		h.heroClass = cls;
		h.heroSkills = CurrentSkills.forHero( h );
		h.heroSkills.init( h );
		return h;
	}

	//the tree as a save file would bring it back
	private static Bundle saved( CurrentSkills hs ) throws Exception {
		Bundle out = new Bundle();
		hs.storeInBundle( out );
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Bundle.write( out, bytes );
		return Bundle.read( new ByteArrayInputStream( bytes.toByteArray() ) );
	}

	//a sprite with nowhere to draw: the bow stances burst particles off it unchecked
	private static class QuietSprite extends CharSprite {
		@Override public Emitter emitter(){ return new Emitter(); }
	}

	@Test public void aForeignSkillIsLearnedFreeAtLevelOne(){
		hero.heroSkills.availableSkill = 5;
		assertNull( hero.heroSkills.get( Smash.class ) );
		assertTrue( SkillDebug.grant( hero, Smash.class ) );
		Smash smash = hero.heroSkills.get( Smash.class );
		assertNotNull( smash );
		assertEquals( 1, smash.level );
		assertEquals( 5, hero.heroSkills.availableSkill );
		assertTrue( hero.heroSkills.isForeign( smash ) );
		//"A1", Summon Rat's own tag, is not taken
		assertEquals( CurrentSkills.FOREIGN_TAG + "Smash", smash.tag );
		assertFalse( SkillDebug.grant( hero, Smash.class ) );
		assertEquals( 1, hero.heroSkills.foreignSkills().size() );
		//the hero's own skills are not granted over
		assertFalse( SkillDebug.grant( hero, SummonRat.class ) );
		assertTrue( hero.heroSkills.usableNow( hero ).contains( smash ) );
	}

	@Test public void aForeignPassiveCountsInTheHooks(){
		int before = hero.heroSkills.allStealth();
		SkillDebug.grant( hero, Stealth.class );
		assertEquals( before + 1, hero.heroSkills.allStealth() );
		SkillDebug.revoke( hero, Stealth.class );
		assertEquals( before, hero.heroSkills.allStealth() );
	}

	@Test public void foreignSkillsAndTheirSlotsSurviveASaveAndLoad() throws Exception {
		CurrentSkills hs = hero.heroSkills;
		hs.get( SummonRat.class ).level = 2;
		SkillDebug.grant( hero, Smash.class );
		hs.get( Smash.class ).level = 3;
		SkillDebug.grant( hero, IronStance.class );
		hs.get( IronStance.class ).active = true;
		hs.quickslot( 0, hs.get( Smash.class ) );
		hs.quickslot( 1, hs.get( SummonRat.class ) );

		Bundle out = new Bundle();
		hs.storeInBundle( out );
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Bundle.write( out, bytes );
		Bundle in = Bundle.read( new ByteArrayInputStream( bytes.toByteArray() ) );

		//the load path of Hero.restoreFromBundle
		hs.init( hero );
		assertTrue( hs.foreignSkills().isEmpty() );
		hs.restoreSkillsFromBundle( in );

		assertEquals( 2, hs.get( SummonRat.class ).level );
		Smash smash = hs.get( Smash.class );
		assertNotNull( smash );
		assertTrue( hs.isForeign( smash ) );
		assertEquals( 3, smash.level );
		assertTrue( hs.get( IronStance.class ).active );
		assertSame( smash, hs.quickslot( 0 ) );
		assertSame( hs.get( SummonRat.class ), hs.quickslot( 1 ) );
	}

	@Test public void anOldSaveLoadsWithNoForeignSkills(){
		Bundle old = new Bundle();
		hero.heroSkills.get( SummonRat.class ).level = 1;
		hero.heroSkills.storeInBundle( old );
		old.remove( "foreign_skills" );
		hero.heroSkills.init( hero );
		hero.heroSkills.restoreSkillsFromBundle( old );
		assertTrue( hero.heroSkills.foreignSkills().isEmpty() );
		assertEquals( 1, hero.heroSkills.get( SummonRat.class ).level );
	}

	@Test public void aNewRunStartsWithoutThem(){
		SkillDebug.grant( hero, Smash.class );
		hero.heroSkills.init( hero );
		assertTrue( hero.heroSkills.foreignSkills().isEmpty() );
		assertNull( hero.heroSkills.get( Smash.class ) );
	}

	@Test public void forgettingOneClearsItsSlot(){
		CurrentSkills hs = hero.heroSkills;
		SkillDebug.grant( hero, Smash.class );
		hs.quickslot( 2, hs.get( Smash.class ) );
		hs.lastUsed = hs.get( Smash.class );
		assertTrue( SkillDebug.revoke( hero, Smash.class ) );
		assertNull( hs.get( Smash.class ) );
		assertNull( hs.quickslot( 2 ) );
		assertNull( hs.lastUsed );
		//the hero's own skills cannot be taken away
		assertFalse( SkillDebug.revoke( hero, SummonRat.class ) );
		assertNotNull( hs.get( SummonRat.class ) );
	}

	@Test public void takingTheCallingMakesItsForeignSkillsHisOwn(){
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		SkillDebug.grant( hero, Carnage.class );
		SkillDebug.grant( hero, Spark.class );
		hs.get( Carnage.class ).level = 2;
		hs.quickslot( 0, hs.get( Carnage.class ) );

		hero.subClass = HeroSubClass.BERSERKER;
		hs.initSubclassBranch( hero );

		Carnage carnage = hs.get( Carnage.class );
		assertFalse( hs.isForeign( carnage ) );
		assertTrue( hs.subSkills.contains( carnage ) );
		assertEquals( 2, carnage.level );
		assertSame( carnage, hs.quickslot( 0 ) );
		assertNotNull( hs.get( Spark.class ) );
		assertEquals( 1, hs.foreignSkills().size() );
		//once only: its hooks would otherwise run twice
		int copies = 0;
		for (CurrentSkills.Origin o : CurrentSkills.catalog()) if (o.cls == Carnage.class && hs.holds( o.cls )) copies++;
		assertEquals( 1, copies );
	}

	//Hero.restoreFromBundle's order: a fresh tree, the calling's branch, then what the save says
	private static void reload( Hero h, Bundle in ){
		h.heroSkills.init( h );
		if (h.subClass != HeroSubClass.NONE) h.heroSkills.initSubclassBranch( h );
		h.heroSkills.restoreSkillsFromBundle( in );
	}

	//"All skills, all classes" lends a calling's skills for nothing and taking the calling makes them his
	//own; turning it off takes back what was lent: the levels nobody paid for, the stance and the slot
	@Test public void aCallingTakenWithEverySkillOnKeepsNoneOfTheLentLevels(){
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		hero.heroSkills.availableSkill = 4;
		SkillDebug.allClasses( hero, true );
		hs.get( Carnage.class ).active = true;
		hs.quickslot( 0, hs.get( Carnage.class ) );
		hero.subClass = HeroSubClass.BERSERKER;
		hs.initSubclassBranch( hero );
		assertFalse( hs.isForeign( hs.get( Carnage.class ) ) );
		assertEquals( Skill.MAX_LEVEL, hs.get( Carnage.class ).level );

		SkillDebug.allClasses( hero, false );
		assertTrue( hs.foreignSkills().isEmpty() );
		assertEquals( 3, hs.subSkills.size() );
		for (Skill s : hs.subSkills){
			assertEquals( s.getClass().getSimpleName(), 0, s.level );
			assertFalse( s.getClass().getSimpleName(), s.active );
			assertFalse( s.getClass().getSimpleName(), hs.usableNow( hero ).contains( s ) );
		}
		assertNull( hs.quickslot( 0 ) );
		//nothing was paid, so nothing comes back
		assertEquals( 4, hero.heroSkills.availableSkill );
		//and at nothing it does nothing: a kill stokes no rage
		hs.onKill( new Rat(), false );
		assertNull( hero.buff( Berserk.class ) );
	}

	@Test public void theLevelsBoughtForATakenOverSkillStay(){
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		hero.heroSkills.availableSkill = 10;
		SkillDebug.grant( hero, Carnage.class );
		assertTrue( hs.get( Carnage.class ).requestUpgrade() );
		hs.quickslot( 0, hs.get( Carnage.class ) );
		SkillDebug.allClasses( hero, true );
		assertEquals( Skill.MAX_LEVEL, hs.get( Carnage.class ).level );
		hero.subClass = HeroSubClass.BERSERKER;
		hs.initSubclassBranch( hero );
		int left = hero.heroSkills.availableSkill;

		SkillDebug.allClasses( hero, false );
		Carnage carnage = hs.get( Carnage.class );
		assertEquals( 1, carnage.level );
		assertSame( carnage, hs.quickslot( 0 ) );
		//his own now: the level bought stays bought
		assertEquals( left, hero.heroSkills.availableSkill );
		for (Skill s : hs.subSkills) if (s != carnage) assertEquals( s.getClass().getSimpleName(), 0, s.level );
	}

	@Test public void whatATakenOverSkillWasLentIsRememberedAcrossSaves() throws Exception {
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		hero.heroSkills.availableSkill = 10;
		SkillDebug.grant( hero, Carnage.class );
		assertTrue( hs.get( Carnage.class ).requestUpgrade() );
		SkillDebug.allClasses( hero, true );
		reload( hero, saved( hs ) );
		hero.subClass = HeroSubClass.BERSERKER;
		hs.initSubclassBranch( hero );
		reload( hero, saved( hs ) );
		assertFalse( hs.isForeign( hs.get( Carnage.class ) ) );
		assertEquals( Skill.MAX_LEVEL, hs.get( Carnage.class ).level );
		assertEquals( Skill.MAX_LEVEL, hs.get( BloodRush.class ).level );

		SkillDebug.allClasses( hero, false );
		assertEquals( 1, hs.get( Carnage.class ).level );
		assertEquals( 0, hs.get( BloodRush.class ).level );
		//and once given back, a save no longer owes anything
		reload( hero, saved( hs ) );
		SkillDebug.allClasses( hero, true );
		SkillDebug.allClasses( hero, false );
		assertEquals( 1, hs.get( Carnage.class ).level );
	}

	//what a taken-over skill left on the hero goes with its lent levels: Mana Shield's charged shards
	@Test public void aTakenOverSkillsShardsGoWithItsLentLevels(){
		hero = heroOf( HeroClass.MAGE );
		SkillDebug.allClasses( hero, true );
		hero.subClass = HeroSubClass.BATTLEMAGE;
		hero.heroSkills.initSubclassBranch( hero );
		ManaShield.Charged shards = Buff.affect( hero, ManaShield.Charged.class );
		shards.set( 2 );
		shards.act();
		assertNotNull( hero.buff( ManaShield.Charged.class ) );
		SkillDebug.allClasses( hero, false );
		shards.act();
		assertNull( hero.buff( ManaShield.Charged.class ) );
	}

	//taught at level 1 in the picker, then taken over by the calling: off takes that level back too
	@Test public void aSkillTaughtAloneThenTakenOverLosesItsLentLevelToo(){
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		SkillDebug.grant( hero, Carnage.class );
		hero.subClass = HeroSubClass.BERSERKER;
		hs.initSubclassBranch( hero );
		assertEquals( 1, hs.get( Carnage.class ).level );
		SkillDebug.allClasses( hero, true );
		SkillDebug.allClasses( hero, false );
		assertEquals( 0, hs.get( Carnage.class ).level );
	}

	//Max skill tree lends a taken-over skill its levels as it does a borrowed one; the hero's own, never
	//borrowed, keep what it gave them
	@Test public void maxSkillTreeOnATakenOverSkillIsLentToo(){
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		hero.heroSkills.availableSkill = 5;
		SkillDebug.grant( hero, Carnage.class );
		hero.subClass = HeroSubClass.BERSERKER;
		hs.initSubclassBranch( hero );
		SkillDebug.maxTree( hero );
		assertEquals( Skill.MAX_LEVEL, hs.get( Carnage.class ).level );

		SkillDebug.allClasses( hero, true );
		SkillDebug.allClasses( hero, false );
		assertEquals( 0, hs.get( Carnage.class ).level );
		assertEquals( Skill.MAX_LEVEL, hs.get( BloodRush.class ).level );
		assertEquals( Skill.MAX_LEVEL, hs.get( Smash.class ).level );
		assertEquals( 5, hero.heroSkills.availableSkill );
	}

	@Test public void theCatalogHoldsEverySkillOfEveryClassOnce(){
		Set<Class<?>> seen = new HashSet<>();
		for (CurrentSkills.Origin o : CurrentSkills.catalog()) assertTrue( o.cls.getSimpleName(), seen.add( o.cls ) );
		//20 core skills a class, 3 a calling
		assertEquals( 6 * 20 + 12 * 3, seen.size() );
	}

	@Test public void allClassesOnAndOffForEveryClass(){
		for (HeroClass cls : HeroClass.values()){
			hero = heroOf( cls );
			CurrentSkills hs = hero.heroSkills;
			hero.heroSkills.availableSkill = 4;
			Skill own = hs.activeSkills.get( 0 );
			own.level = 1;
			assertFalse( SkillDebug.allClassesOn( hero ) );

			SkillDebug.allClasses( hero, true );
			assertTrue( cls.name(), SkillDebug.allClassesOn( hero ) );
			assertEquals( 4, hero.heroSkills.availableSkill );
			for (CurrentSkills.Origin o : CurrentSkills.catalog()){
				Skill s = hs.exact( o.cls );
				assertNotNull( o.cls.getSimpleName(), s );
				if (hs.isForeign( s )) assertEquals( o.cls.getSimpleName(), Skill.MAX_LEVEL, s.level );
			}
			//the hero's own tree is left as it was
			assertEquals( 1, own.level );

			SkillDebug.allClasses( hero, false );
			assertFalse( SkillDebug.allClassesOn( hero ) );
			assertTrue( hs.foreignSkills().isEmpty() );
			assertSame( own, hs.activeSkills.get( 0 ) );
			assertEquals( 1, own.level );
			for (CurrentSkills.Origin o : CurrentSkills.catalog()){
				if (o.heroClass != cls) assertFalse( o.cls.getSimpleName(), hs.holds( o.cls ) );
			}
		}
	}

	@Test public void maxTreeTakesTheForeignSkillsToTheTop(){
		hero.heroSkills.availableSkill = 7;
		SkillDebug.grant( hero, Smash.class );
		SkillDebug.maxTree( hero );
		assertEquals( Skill.MAX_LEVEL, hero.heroSkills.get( Smash.class ).level );
		assertEquals( 7, hero.heroSkills.availableSkill );
	}

	//the points put into a borrowed skill come back when it goes; the levels it was given for nothing do not
	@Test public void pointsSpentOnABorrowedSkillComeBackWhenItGoes(){
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		hero.heroSkills.availableSkill = 20;
		SkillDebug.grant( hero, Spark.class );
		Spark spark = hs.get( Spark.class );
		int cost = spark.upgradeCost();
		assertTrue( spark.requestUpgrade() );
		assertTrue( spark.requestUpgrade() );
		assertEquals( 20 - 2 * cost, hero.heroSkills.availableSkill );
		assertTrue( SkillDebug.revoke( hero, Spark.class ) );
		assertEquals( 20, hero.heroSkills.availableSkill );

		//one level bought, the rest given by Max skill tree: only the bought one comes back
		SkillDebug.grant( hero, Spark.class );
		assertTrue( hs.get( Spark.class ).requestUpgrade() );
		SkillDebug.maxTree( hero );
		assertEquals( Skill.MAX_LEVEL, hs.get( Spark.class ).level );
		assertEquals( 20 - cost, hero.heroSkills.availableSkill );
		SkillDebug.revoke( hero, Spark.class );
		assertEquals( 20, hero.heroSkills.availableSkill );

		//one bought, then "All skills, all classes" on and off: that one comes back, nothing more
		SkillDebug.grant( hero, Spark.class );
		assertTrue( hs.get( Spark.class ).requestUpgrade() );
		SkillDebug.allClasses( hero, true );
		SkillDebug.allClasses( hero, false );
		assertEquals( 20, hero.heroSkills.availableSkill );
	}

	@Test public void whatWasPaidForABorrowedSkillIsRememberedAcrossASave() throws Exception {
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		hero.heroSkills.availableSkill = 10;
		SkillDebug.grant( hero, Spark.class );
		assertTrue( hs.get( Spark.class ).requestUpgrade() );
		Bundle in = saved( hs );
		hs.init( hero );
		hs.restoreSkillsFromBundle( in );
		assertEquals( 2, hs.get( Spark.class ).level );
		SkillDebug.revoke( hero, Spark.class );
		assertEquals( 10, hero.heroSkills.availableSkill );
	}

	//only a skill's own code takes these off the hero: once it is forgotten they have to let go themselves
	@Test public void aForgottenSkillsBuffsLetGo(){
		hero = heroOf( HeroClass.WARRIOR );
		Object[][] owned = {
				{ SpiritArmor.class, SpiritArmorMotes.class },
				{ MasterThief.class, MasterThiefCoins.class },
				{ CrusadersZeal.class, CrusadersZeal.Zeal.class },
				{ Fletching.class, FletchingFeathers.class },
				{ WeaponBond.class, WeaponBond.Bonded.class },
				{ CinderTrail.class, ElementalOrbit.class } };
		for (Object[] pair : owned){
			@SuppressWarnings("unchecked") Class<? extends Skill> skill = (Class<? extends Skill>) pair[0];
			@SuppressWarnings("unchecked") Class<? extends Buff> buff = (Class<? extends Buff>) pair[1];
			SkillDebug.grant( hero, skill );
			Buff b = Buff.affect( hero, buff );
			if (b instanceof ElementalOrbit) ((ElementalOrbit) b).set( 1 );
			b.act();
			assertNotNull( buff.getSimpleName() + " while " + skill.getSimpleName() + " is held", hero.buff( buff ) );
			SkillDebug.revoke( hero, skill );
			b.act();
			assertNull( buff.getSimpleName() + " once " + skill.getSimpleName() + " is gone", hero.buff( buff ) );
		}
	}

	//its rage is the Berserker's; borrowed by anyone else, Carnage starts a rage of his own
	@Test public void aBorrowedCarnageStokesARageAwayFromTheBerserker(){
		hero = heroOf( HeroClass.MAGE );
		SkillDebug.grant( hero, Carnage.class );
		hero.heroSkills.onKill( new Rat(), false );
		assertNotNull( hero.buff( Berserk.class ) );
	}

	//Ninja Bomb taken away (debug) while its target was being picked: the pick throws nothing
	@Test public void aNinjaBombForgottenMidCastThrowsNothing() throws Exception {
		hero = heroOf( HeroClass.WARRIOR );
		hero.MP = 50;
		java.lang.reflect.Field thrower = NinjaBomb.class.getDeclaredField( "thrower" );
		thrower.setAccessible( true );
		((CellSelector.Listener) thrower.get( null )).onSelect( hero.pos + 1 );
		assertEquals( 50, hero.MP );
	}

	//the load path re-learns the saved foreign skills, even over a tree that still holds them
	@Test public void loadingOverALiveTreeKeepsItsForeignSkills() throws Exception {
		CurrentSkills hs = hero.heroSkills;
		SkillDebug.grant( hero, Smash.class );
		hs.restoreSkillsFromBundle( saved( hs ) );
		Smash smash = hs.get( Smash.class );
		assertNotNull( smash );
		assertTrue( hs.isForeign( smash ) );
		assertEquals( 1, hs.foreignSkills().size() );
	}

	private static Level smallLevel(){
		Level floor = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		floor.setSize( 9, 9 );
		floor.traps = new com.watabou.utils.SparseArray<>();
		floor.plants = new com.watabou.utils.SparseArray<>();
		floor.heaps = new com.watabou.utils.SparseArray<>();
		floor.blobs = new java.util.HashMap<>();
		floor.mobs = new java.util.HashSet<>();
		java.util.Arrays.fill( floor.solid, true );
		for (int y = 1; y < 8; y++) for (int x = 1; x < 8; x++) Level.set( y * 9 + x, Terrain.EMPTY, floor );
		java.util.Arrays.fill( floor.heroFOV, true );
		Dungeon.level = floor;
		return floor;
	}

	//what the skill windows and the quick panel ask of each of them. A description that fails to
	//format is only reported (and shown raw), so the report is what is looked for
	@Test public void everyOtherClassesSkillDescribesItselfOnAWarriorAndAMage(){
		smallLevel();
		PrintStream err = System.err;
		ByteArrayOutputStream reported = new ByteArrayOutputStream();
		System.setErr( new PrintStream( reported ) );
		try {
			for (HeroClass cls : new HeroClass[]{ HeroClass.WARRIOR, HeroClass.MAGE }){
				hero = heroOf( cls );
				SkillDebug.allClasses( hero, true );
				for (Skill s : hero.heroSkills.foreignSkills()){
					assertNotNull( s.name() );
					assertFalse( s.info().isEmpty() );
					assertNotNull( s.actions( hero ) );
				}
				assertFalse( hero.heroSkills.usableNow( hero ).isEmpty() );
			}
		} finally {
			System.setErr( err );
		}
		assertFalse( reported.toString(), reported.toString().contains( "formatting error" ) );
	}

	//the hooks a fight runs, with one skill at its top level, the hero's own at nothing
	private Throwable fightWith( HeroClass cls, HeroSubClass sub, Class<? extends Skill> skill, long seed ){
		Level floor = smallLevel();
		hero = heroOf( cls );
		hero.pos = 4 * 9 + 4;
		CurrentSkills hs = hero.heroSkills;
		if (sub != HeroSubClass.NONE){
			hero.subClass = sub;
			hs.initSubclassBranch( hero );
		}
		SkillDebug.grant( hero, skill );
		hs.exact( skill ).level = Skill.MAX_LEVEL;
		Mob rat = new Rat();
		rat.pos = hero.pos + 1;
		floor.mobs.add( rat );
		Actor.add( rat );
		com.watabou.utils.Random.pushGenerator( seed );
		try {
			hs.allDamageModifier( false );
			hs.allDamageModifier( true );
			hs.allIncomingDamage( 10, rat );
			hs.allOnHit( rat, 10, false );
			hs.allOnHit( rat, 10, true );
			hs.allOnDefend( rat, 10 );
			hs.allToHitBonus();
			hs.allToHitModifier();
			hs.allHealthRegen();
			hs.allManaRegen();
			hs.allStealth();
			hs.allWeaponLevelBonus();
			hs.allVenomBonus();
			hs.allSummonLimit();
			hs.allWandDamage();
			hs.allWandRecharge();
			hs.allLootBonus( 10 );
			hs.allFletching();
			hs.anyKnocksBack();
			hs.anyAoEDamage();
			hs.anyDodge( rat );
			hs.anySureHit( rat );
			hs.anyIgnoresArmor( rat );
			hs.rollVenomous();
			hs.rollCripple();
			hs.rollPassThrough();
			hs.anyDisableTrap();
			hs.anyAimedShot();
			hs.anyDoubleShot();
			hs.anyDoubleStab();
			hs.anyArrowToBomb();
			hs.onHeroMissed( rat, true );
			hs.onHeroAttackMiss( rat, false );
			hs.onEnemyStepsAdjacent( rat, rat.pos + 1 );
			hs.onDamageTaken( 1, 0, rat );
			hs.anyShrugsOffDebuff( new Weakness() );
			hs.anyPreventsWaking( rat );
			return null;
		} catch (RuntimeException e){
			return e;
		} finally {
			com.watabou.utils.Random.popGenerator();
			for (Buff b : rat.buffs()) b.detach();
			Actor.remove( rat );
			for (Buff b : hero.buffs()) b.detach();
		}
	}

	//a skill that gets through a fight on its own class's hero gets through it on a Warrior and a Mage.
	//Some cannot be checked here at all: their visuals need a game scene to draw in, even at home
	@Test public void noSkillBreaksOnAHeroOfAnotherClass(){
		int checked = 0;
		for (CurrentSkills.Origin o : CurrentSkills.catalog()){
			for (long seed = 1; seed <= 3; seed++){
				if (fightWith( o.heroClass, o.subClass, o.cls, seed ) != null) continue;
				checked++;
				for (HeroClass other : new HeroClass[]{ HeroClass.WARRIOR, HeroClass.MAGE }){
					if (other == o.heroClass) continue;
					Throwable away = fightWith( other, HeroSubClass.NONE, o.cls, seed );
					if (away != null) throw new AssertionError( o.cls.getSimpleName() + " on a " + other + ": " + away, away );
				}
			}
		}
		assertTrue( "only " + checked + " runs could be checked", checked > CurrentSkills.catalog().size() * 3 / 2 );
	}

	@Test public void aForeignBowStanceLeavesAWarriorsOwnStancesAlone(){
		hero = heroOf( HeroClass.WARRIOR );
		CurrentSkills hs = hero.heroSkills;
		hero.sprite = new QuietSprite();
		for (Skill s : hs.activeSkills){ s.level = 1; s.active = true; }
		SkillDebug.grant( hero, AimedShot.class );
		hs.get( AimedShot.class ).execute( hero, Skill.AC_ACTIVATE );
		assertTrue( hs.get( AimedShot.class ).active );
		//it used to switch off whatever stood second and third in the hero's own column
		assertTrue( hs.activeSkills.get( 1 ).active );
		assertTrue( hs.activeSkills.get( 2 ).active );
	}

	@Test public void theBowStancesStillTakeTurnsOnAHuntress(){
		hero = heroOf( HeroClass.HUNTRESS );
		CurrentSkills hs = hero.heroSkills;
		hero.sprite = new QuietSprite();
		for (Skill s : hs.activeSkills) s.level = 1;
		hs.get( DoubleShot.class ).active = true;
		hs.get( AimedShot.class ).execute( hero, Skill.AC_ACTIVATE );
		assertTrue( hs.get( AimedShot.class ).active );
		assertFalse( hs.get( DoubleShot.class ).active );
	}

	@Test public void foreignStancesAndTheHerosOwnSwitchEachOtherOff(){
		//a Huntress's own bow stance, and a Warrior's stance taken up over it
		hero = heroOf( HeroClass.HUNTRESS );
		CurrentSkills hs = hero.heroSkills;
		hs.get( AimedShot.class ).level = 1;
		hs.get( AimedShot.class ).active = true;
		SkillDebug.grant( hero, IronStance.class );
		hs.get( IronStance.class ).execute( hero, Skill.AC_ACTIVATE );
		assertTrue( hs.get( IronStance.class ).active );
		assertFalse( hs.get( AimedShot.class ).active );

		//and a foreign one switched off by another foreign one
		SkillDebug.grant( hero, RecklessFury.class );
		hs.get( RecklessFury.class ).execute( hero, Skill.AC_ACTIVATE );
		assertTrue( hs.get( RecklessFury.class ).active );
		assertFalse( hs.get( IronStance.class ).active );

		//the arrow tips, both foreign on a Warrior, still one at a time
		hero = heroOf( HeroClass.WARRIOR );
		hs = hero.heroSkills;
		hero.sprite = new QuietSprite();
		SkillDebug.grant( hero, FrostArrows.class );
		SkillDebug.grant( hero, EmberArrows.class );
		hs.get( FrostArrows.class ).execute( hero, Skill.AC_ACTIVATE );
		hs.get( EmberArrows.class ).execute( hero, Skill.AC_ACTIVATE );
		assertTrue( hs.get( EmberArrows.class ).active );
		assertFalse( hs.get( FrostArrows.class ).active );
	}
}
