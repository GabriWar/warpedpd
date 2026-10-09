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
import com.watabou.utils.Bundle;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.IronStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.KnockBack;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Smash;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Spark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Stealth;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SummonRat;
import xyz.gabriwar.warpedpixeldungeon.debug.SkillDebug;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/** Every hero holds a skill tree of his own: two of one class (a host and his guest in co-op) never
 *  share levels, stances, slots, borrowed skills or points. */
public class HeroSkillTreesTest {

	@BeforeClass public static void loadAssets(){
		xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
		//a class's starting kit is identified as it is handed out
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		xyz.gabriwar.warpedpixeldungeon.items.rings.Ring.initGems();
	}

	private Hero previousHero;
	private String previousVersion;

	@Before public void setUp(){
		previousHero = Dungeon.hero;
		previousVersion = Game.version;
		Game.version = "test";
	}

	@After public void tearDown(){
		Dungeon.hero = previousHero;
		Game.version = previousVersion;
		//what a class's starting kit leaves behind
		Dungeon.quickslot.reset();
		Dungeon.LimitedDrops.reset();
	}

	private static Hero heroOf( HeroClass cls ){
		Hero h = new Hero();
		h.heroClass = cls;
		Talent.initClassTalents( h );
		h.heroSkills = CurrentSkills.forHero( h );
		h.heroSkills.init( h );
		return h;
	}

	//the points a hero has to spend
	private static int points( Hero h ){
		return h.heroSkills.availableSkill;
	}

	private static void setPoints( Hero h, int points ){
		h.heroSkills.availableSkill = points;
	}

	//the hero as a save file (or a guest's snapshot) brings him back: Hero.restoreFromBundle
	private static Hero reloaded( Hero h ) throws Exception {
		Bundle out = new Bundle();
		out.put( "hero", h );
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Bundle.write( out, bytes );
		return (Hero) Bundle.read( new ByteArrayInputStream( bytes.toByteArray() ) ).get( "hero" );
	}

	//what the host learns: a level bought, a slot, a stance switched on, a skill borrowed
	private static void playAsHost( Hero host ){
		Dungeon.hero = host;
		CurrentSkills hs = host.heroSkills;
		setPoints( host, 10 );
		assertTrue( hs.get( Smash.class ).requestUpgrade() );
		hs.quickslot( 0, hs.get( Smash.class ) );
		hs.get( IronStance.class ).level = 1;
		hs.get( IronStance.class ).active = true;
		assertTrue( SkillDebug.grant( host, Spark.class ) );
	}

	private static void assertHostsTree( Hero host ){
		CurrentSkills hs = host.heroSkills;
		assertEquals( 1, hs.get( Smash.class ).level );
		assertSame( hs.get( Smash.class ), hs.quickslot( 0 ) );
		assertEquals( 1, hs.get( IronStance.class ).level );
		assertTrue( hs.get( IronStance.class ).active );
		assertNotNull( hs.get( Spark.class ) );
		assertTrue( hs.isForeign( hs.get( Spark.class ) ) );
		assertEquals( 1, hs.foreignSkills().size() );
		assertEquals( 0, hs.get( KnockBack.class ).level );
		assertNull( hs.get( Stealth.class ) );
	}

	private static void assertUntouched( Hero h ){
		CurrentSkills hs = h.heroSkills;
		assertEquals( 0, hs.get( Smash.class ).level );
		assertNull( hs.quickslot( 0 ) );
		assertFalse( hs.get( IronStance.class ).active );
		assertNull( hs.get( Spark.class ) );
		assertTrue( hs.foreignSkills().isEmpty() );
	}

	@Test public void twoHeroesOfOneClassHoldTwoTrees(){
		Hero host = heroOf( HeroClass.WARRIOR );
		Hero guest = heroOf( HeroClass.WARRIOR );
		assertNotSame( host.heroSkills, guest.heroSkills );

		playAsHost( host );
		assertUntouched( guest );
		//nothing of the host's can be taken away through the guest
		assertFalse( SkillDebug.revoke( guest, Spark.class ) );

		//and the other way round
		Dungeon.hero = guest;
		setPoints( guest, 10 );
		CurrentSkills gs = guest.heroSkills;
		assertTrue( gs.get( KnockBack.class ).requestUpgrade() );
		gs.quickslot( 1, gs.get( KnockBack.class ) );
		assertTrue( SkillDebug.grant( guest, Stealth.class ) );
		SkillDebug.maxTree( guest );
		assertHostsTree( host );
		assertNull( host.heroSkills.quickslot( 1 ) );
	}

	//a guest's hero is made with a fresh tree of his class (HeroClass.initHero): the host's stays as it was
	@Test public void aNewTreeForOneLeavesTheOthersAlone(){
		Hero host = heroOf( HeroClass.WARRIOR );
		playAsHost( host );
		Hero guest = heroOf( HeroClass.WARRIOR );
		assertHostsTree( host );
		assertUntouched( guest );
	}

	//a guest's hero restored from its bundle (a host's save, the snapshot a player brings) leaves the host's alone
	@Test public void restoringOneFromABundleLeavesTheOthersAlone() throws Exception {
		Hero host = heroOf( HeroClass.WARRIOR );
		Hero guest = heroOf( HeroClass.WARRIOR );
		Dungeon.hero = guest;
		setPoints( guest, 10 );
		assertTrue( guest.heroSkills.get( KnockBack.class ).requestUpgrade() );

		playAsHost( host );
		Hero back = reloaded( guest );
		assertHostsTree( host );
		assertEquals( 1, back.heroSkills.get( KnockBack.class ).level );
		assertEquals( 0, back.heroSkills.get( Smash.class ).level );
		assertNull( back.heroSkills.get( Spark.class ) );
	}

	@Test public void eachHeroComesBackFromASaveWithHisOwnTree() throws Exception {
		Hero host = heroOf( HeroClass.WARRIOR );
		Hero guest = heroOf( HeroClass.WARRIOR );
		playAsHost( host );
		Dungeon.hero = guest;
		setPoints( guest, 10 );
		assertTrue( guest.heroSkills.get( KnockBack.class ).requestUpgrade() );
		assertTrue( SkillDebug.grant( guest, Stealth.class ) );

		Hero hostBack = reloaded( host );
		Hero guestBack = reloaded( guest );
		assertHostsTree( hostBack );
		CurrentSkills gs = guestBack.heroSkills;
		assertEquals( 1, gs.get( KnockBack.class ).level );
		assertEquals( 0, gs.get( Smash.class ).level );
		assertNotNull( gs.get( Stealth.class ) );
		assertNull( gs.get( Spark.class ) );
	}

	//a save from before the borrowed skills, the slots and the stances: the class and the levels
	@Test public void anOldSaveWithOnlyItsTypeAndLevelsStillLoads(){
		Bundle old = new Bundle();
		old.put( CurrentSkills.TYPE, "MAGE" );
		old.put( Skill.SKILL_LEVEL + " " + new SummonRat().tag, 2 );
		CurrentSkills tree = CurrentSkills.restoreFromBundle( old );
		assertEquals( HeroClass.MAGE, tree.heroClass );
		Hero mage = new Hero();
		mage.heroClass = HeroClass.MAGE;
		tree.init( mage );
		tree.restoreSkillsFromBundle( old );
		assertSame( tree, mage.heroSkills );
		assertEquals( 2, tree.get( SummonRat.class ).level );
		assertFalse( tree.get( SummonRat.class ).active );
		assertTrue( tree.foreignSkills().isEmpty() );
		assertNull( tree.quickslot( 0 ) );

		//and a tree saved now names its class as the old ones did
		Bundle now = new Bundle();
		tree.storeInBundle( now );
		assertEquals( "MAGE", now.getString( CurrentSkills.TYPE ) );
	}

	//a hero saved before his points were: they are granted again for his level
	@Test public void aHeroFromBeforeThePointsWereSavedLoads(){
		Hero mage = heroOf( HeroClass.MAGE );
		mage.lvl = 3;
		mage.heroSkills.get( SummonRat.class ).level = 2;
		Bundle out = new Bundle();
		out.put( "hero", mage );
		Bundle saved = out.getBundle( "hero" );
		for (String key : saved.getKeys()){
			if (key.equals( "skillsavailable" ) || key.equals( "foreign_skills" )
					|| key.startsWith( "skill_quickslot_" ) || key.startsWith( Skill.SKILL_ACTIVE + " " )) saved.remove( key );
		}
		Hero back = (Hero) out.get( "hero" );
		assertEquals( HeroClass.MAGE, back.heroSkills.heroClass );
		assertEquals( 2, back.heroSkills.get( SummonRat.class ).level );
		assertEquals( Skill.STARTING_SKILL + 2 * 3, points( back ) );
		assertTrue( back.heroSkills.foreignSkills().isEmpty() );
		assertNull( back.heroSkills.quickslot( 0 ) );
	}

	//NetManager's fresh spawn of a joining player: Dungeon.hero is the guest's while his class sets him up
	private static Hero joinAs( HeroClass cls, Hero host ){
		Hero netHero = new Hero();
		netHero.heroClass = cls;
		netHero.isRemote = true;
		try {
			Dungeon.hero = netHero;
			Dungeon.quickslot.reset();
			cls.initHero( netHero );
		} finally {
			Dungeon.hero = host;
			Dungeon.quickslot.reset();
		}
		return netHero;
	}

	@Test public void aGuestOfTheHostsClassJoiningLeavesTheHostsSkillsAlone() throws Exception {
		Hero host = new Hero();
		Dungeon.hero = host;
		Dungeon.quickslot.reset();
		HeroClass.WARRIOR.initHero( host );
		playAsHost( host );
		setPoints( host, 7 );

		Hero guest = joinAs( HeroClass.WARRIOR, host );
		assertHostsTree( host );
		assertEquals( 7, points( host ) );
		assertUntouched( guest );
		assertEquals( Skill.STARTING_SKILL, points( guest ) );

		//the guest's level-up pays him, not the host
		guest.earnExp( guest.maxExp(), HeroSkillTreesTest.class );
		assertEquals( 7, points( host ) );
		assertEquals( Skill.STARTING_SKILL + 3, points( guest ) );

		//nor does the host's save of the guest, read back, touch the host
		Hero guestBack = reloaded( guest );
		assertHostsTree( host );
		assertEquals( 7, points( host ) );
		assertEquals( Skill.STARTING_SKILL + 3, points( guestBack ) );
	}

	//the copy of his hero a player brings to a host (NetManager.restoreClientHero): his points and
	//levels come with him, and the host's stay his
	@Test public void aHeroBroughtByAPlayerKeepsHisOwnPoints() throws Exception {
		Hero host = heroOf( HeroClass.WARRIOR );
		playAsHost( host );
		setPoints( host, 7 );
		Hero guest = heroOf( HeroClass.WARRIOR );
		Dungeon.hero = guest;
		setPoints( guest, 10 );
		assertTrue( guest.heroSkills.get( KnockBack.class ).requestUpgrade() );
		int guestsPoints = points( guest );
		Dungeon.hero = host;

		Method snapshot = NetManager.class.getDeclaredMethod( "snapshotHero", Hero.class );
		snapshot.setAccessible( true );
		Method restore = NetManager.class.getDeclaredMethod( "restoreClientHero", JSONObject.class, String.class );
		restore.setAccessible( true );
		JSONObject snap = (JSONObject) snapshot.invoke( null, guest );
		Hero brought = (Hero) restore.invoke( null, snap.getJSONObject( "hero" ), "guest" );

		assertNotNull( brought );
		assertEquals( guestsPoints, points( brought ) );
		assertEquals( 1, brought.heroSkills.get( KnockBack.class ).level );
		assertNull( brought.heroSkills.get( Spark.class ) );
		assertHostsTree( host );
		assertEquals( 7, points( host ) );
	}
}
