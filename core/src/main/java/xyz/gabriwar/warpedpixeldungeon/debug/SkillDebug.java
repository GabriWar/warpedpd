package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;

import java.util.LinkedHashMap;

/** Cheats for the current hero's tree, including its selected subclass and talents. */
public final class SkillDebug {
	public static void maxTree(Hero hero) {
		hero.debugAllSkillPaths = true;
		int points = Skill.availableSkill;
		try {
			Skill.availableSkill = Integer.MAX_VALUE;
			for (CurrentSkills.BRANCHES branch : CurrentSkills.BRANCHES.values()) {
				for (Skill skill : hero.heroSkills.branchSkills(branch)) {
					while (skill.level < Skill.MAX_LEVEL && skill.requestUpgrade()) { }
				}
			}
			for (LinkedHashMap<Talent, Integer> tier : hero.talents) {
				for (Talent talent : tier.keySet()) {
					while (hero.pointsInTalent(talent) < talent.maxPoints()) hero.upgradeTalent(talent);
				}
			}
		} finally {
			Skill.availableSkill = points;
		}
	}
}
