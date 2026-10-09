package xyz.gabriwar.warpedpixeldungeon.actors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.debug.SkillDebug;
import xyz.gabriwar.warpedpixeldungeon.ui.SkillTreePane;

import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class SkillDebugTest {
	@org.junit.BeforeClass public static void loadAssets() {
		xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
	}
	private Hero previousHero;
	private Hero hero;

	@Before public void setUp() {
		previousHero = Dungeon.hero;
		hero = new Hero();
		Dungeon.hero = hero;
		hero.heroClass = HeroClass.MAGE;
		hero.heroSkills = CurrentSkills.forHero(hero);
		hero.heroSkills.init(hero);
	}

	@After public void tearDown() {
		Dungeon.hero = previousHero;
	}

	@Test public void oldGodmodeConvertsButNormalProtectionRemains(){
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability protection =
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect(hero,
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability.class, 10f);
		hero.migrateDebugGodmode();
		assertFalse(hero.debugInfiniteHealth);
		assertNotNull(hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability.class));
		protection.detach();
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect(hero,
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability.class, 999f);
		hero.HP = 1;
		hero.migrateDebugGodmode();
		assertTrue(hero.debugInfiniteHealth);
		assertEquals(hero.HT, hero.HP);
		assertNull(hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability.class));
	}

	@Test public void infiniteHealthLocksHpWithoutInvulnerability() {
		hero.debugInfiniteHealth = true;
		hero.HP = 1;
		assertTrue(hero.isAlive());
		//isAlive() runs on the render thread every frame: it no longer writes HP
		assertEquals(1, hero.HP);
		hero.HP = hero.HT;
		assertFalse(hero.isInvulnerable(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger.class));
		hero.damage(hero.HT * 10, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger());
		assertEquals(hero.HT, hero.HP);
		hero.HP = 0;
		hero.die(this);
		assertEquals(hero.HT, hero.HP);
		hero.lvl++;
		hero.updateHT(false);
		assertEquals(hero.HT, hero.HP);
		hero.debugInfiniteHealth = false;
		hero.HP = 5;
		assertTrue(hero.isAlive());
		assertEquals(5, hero.HP);
	}

	@Test public void unlockBypassesExclusiveChoiceInUiAndUpgradeLogic() {
		Skill first = new Skill() { @Override protected boolean upgrade() { return true; } };
		Skill sibling = new Skill();
		first.exclusiveWith = sibling;
		sibling.level = 1;
		SkillTreePane.Node node = SkillTreePane.Node.skill(first, 0, 0, null);
		assertTrue(first.pathLocked());
		assertFalse(node.unlocked());
		hero.debugAllSkillPaths = true;
		hero.heroSkills.availableSkill = 1;
		assertTrue(node.canSpend());
		assertTrue(first.requestUpgrade());
		assertEquals(1, first.level);
		assertEquals(0, hero.heroSkills.availableSkill);
		hero.debugAllSkillPaths = false;
		assertTrue(first.pathLocked());
	}

	@Test public void maxTreeIncludesBothForksAndTalentsWithoutSpendingWallet() {
		hero.talents.clear();
		LinkedHashMap<Talent, Integer> tier = new LinkedHashMap<>();
		tier.put(Talent.HEARTY_MEAL, 0);
		hero.talents.add(tier);
		hero.heroSkills.availableSkill = 7;
		SkillDebug.maxTree(hero);
		assertTrue(hero.debugAllSkillPaths);
		int forks = 0;
		for (CurrentSkills.BRANCHES branch : CurrentSkills.BRANCHES.values()) {
			for (Skill skill : hero.heroSkills.branchSkills(branch)) {
				assertEquals(skill.getClass().getSimpleName(), Skill.MAX_LEVEL, skill.level);
				if (skill.exclusiveWith != null) forks++;
			}
		}
		assertTrue(forks > 0);
		assertEquals(Talent.HEARTY_MEAL.maxPoints(), hero.pointsInTalent(Talent.HEARTY_MEAL));
		assertEquals(7, hero.heroSkills.availableSkill);
		SkillDebug.maxTree(hero);
		assertEquals(7, hero.heroSkills.availableSkill);
	}
}
