package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import com.watabou.utils.Reflection;

import java.util.LinkedHashMap;

/** Cheats for the current hero's tree, including its selected subclass and talents. */
public final class SkillDebug {
	public static void maxTree(Hero hero) {
		hero.debugAllSkillPaths = true;
		int points = hero.heroSkills.availableSkill;
		try {
			hero.heroSkills.availableSkill = Integer.MAX_VALUE;
			for (CurrentSkills.BRANCHES branch : CurrentSkills.BRANCHES.values()) {
				for (Skill skill : hero.heroSkills.branchSkills(branch)) {
					//one his calling took over from a borrowed skill is lent its levels, as the borrowed ones are
					hero.heroSkills.raiseForeign(skill, Skill.MAX_LEVEL);
					while (skill.level < Skill.MAX_LEVEL && skill.requestUpgrade()) { }
				}
			}
			//the skills taken from other classes are part of the tree too
			for (Skill skill : hero.heroSkills.foreignSkills()) {
				hero.heroSkills.raiseForeign(skill, Skill.MAX_LEVEL);
			}
			for (LinkedHashMap<Talent, Integer> tier : hero.talents) {
				for (Talent talent : tier.keySet()) {
					while (hero.pointsInTalent(talent) < talent.maxPoints()) hero.upgradeTalent(talent);
				}
			}
		} finally {
			hero.heroSkills.availableSkill = points;
		}
	}

	/** gives the hero a skill of another class (or calling) at level 1, without spending points;
	 *  false if he already has it */
	public static boolean grant(Hero hero, Class<? extends Skill> cls) {
		if (hero.heroSkills.holds(cls)) return false;
		Skill skill = Reflection.newInstance(cls);
		if (skill == null) return false;
		skill.level = 1;
		return hero.heroSkills.addForeign(skill);
	}

	/** takes a granted skill away again, handing back any points spent raising it; the hero's own skills cannot be */
	public static boolean revoke(Hero hero, Class<? extends Skill> cls) {
		return hero.heroSkills.removeForeign(cls);
	}

	/** the hero holds every skill of every class and calling */
	public static boolean allClassesOn(Hero hero) {
		for (CurrentSkills.Origin o : CurrentSkills.catalog()) {
			if (!hero.heroSkills.holds(o.cls)) return false;
		}
		return true;
	}

	/** on: every other class's and calling's skill, at its top level, without spending points.
	 *  off: back to the hero's own skills (every foreign one goes, those granted one by one too),
	 *  and those his calling took over from them keep only the levels bought for them */
	public static void allClasses(Hero hero, boolean on) {
		CurrentSkills hs = hero.heroSkills;
		if (!on) {
			hs.clearForeign();
			hs.takeBackLentLevels();
			return;
		}
		for (CurrentSkills.Origin o : CurrentSkills.catalog()) {
			if (hs.holds(o.cls)) continue;
			Skill skill = Reflection.newInstance(o.cls);
			if (skill != null) hs.addForeign(skill);
		}
		for (Skill skill : hs.foreignSkills()) hs.raiseForeign(skill, Skill.MAX_LEVEL);
	}
}
