package xyz.gabriwar.warpedpixeldungeon.actors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Demoralize;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Bloodthirst;

import static org.junit.Assert.*;

public class BloodthirstTest {
	@BeforeClass public static void loadAssets(){
		xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
	}

	private Hero previousHero;
	private Hero hero;

	@Before public void setUp(){
		previousHero = Dungeon.hero;
		hero = new Hero();
		Dungeon.hero = hero;
	}

	@After public void tearDown(){
		Dungeon.hero = previousHero;
	}

	@Test public void meleeHitsApplyRankScaledDamageReductionWithoutHealing(){
		Bloodthirst skill = new Bloodthirst();
		skill.level = Bloodthirst.MAX_LEVEL;
		hero.HP = hero.HT - 10;
		int hpBefore = hero.HP;
		Hero enemy = new Hero();
		skill.onHitProc(enemy, 20, false);
		Demoralize.Demoralized debuff = enemy.buff(Demoralize.Demoralized.class);
		assertNotNull(debuff);
		assertEquals(.80f, debuff.factor, .001f);
		assertEquals(hpBefore, hero.HP);
	}

	@Test public void rangedHitsDoNotApplyDemoralize(){
		Bloodthirst skill = new Bloodthirst();
		skill.level = Bloodthirst.MAX_LEVEL;
		skill.onHitProc(null, 20, true);
		assertNull(hero.buff(Demoralize.Demoralized.class));
	}

	@Test public void repeatedMeleeHitsBuildDemoralizeCounter(){
		Bloodthirst skill = new Bloodthirst();
		skill.level = 2;
		skill.onHitProc(null, 20, false);
		Hero enemy = new Hero();
		skill.onHitProc(enemy, 20, false);
		assertEquals(1, enemy.buff(Demoralize.Demoralized.class).hits);
		skill.onHitProc(enemy, 20, false);
		assertEquals(2, enemy.buff(Demoralize.Demoralized.class).hits);
	}

	@Test public void fiveConsecutiveHitsAtRankThreeTerrifyAndMissResets(){
		Bloodthirst skill = new Bloodthirst();
		skill.level = Bloodthirst.MAX_LEVEL;
		Hero enemy = new Hero();
		for (int i = 0; i < 4; i++) skill.onHitProc(enemy, 20, false);
		assertNull(enemy.buff(Terror.class));
		skill.onHitProc(enemy, 20, false);
		assertNotNull(enemy.buff(Terror.class));
		Demoralize.Demoralized debuff = enemy.buff(Demoralize.Demoralized.class);
		debuff.hits = 4;
		skill.onHeroAttackMiss(enemy, false);
		assertEquals(0, debuff.hits);
	}
}
