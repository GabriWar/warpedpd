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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class BranchSkill extends Skill {

	@Override
	public float getAlpha(){
		return 1f;
	}

	@Override
	public String info(){
		return Messages.get(this, "desc") + "\n" + branchProgressInfo();
	}

	/** the class emblem at the top of the tree: everything hangs off it, so it
	 *  answers for the whole tree rather than one column */
	private boolean trunk(){
		return Dungeon.hero != null && Dungeon.hero.heroSkills != null && Dungeon.hero.heroSkills.branchPA == this;
	}

	private int skillsAll(){
		return Dungeon.hero != null && Dungeon.hero.heroSkills != null ? Dungeon.hero.heroSkills.totalSpentAll() : totalSpent();
	}

	/** the points the emblem wears: the whole tree for the trunk (skills and
	 *  talents both draw on the same pool), its own column for the other headers */
	public int spent(){
		return trunk() ? skillsAll() + CurrentSkills.talentPointsSpent() : totalSpent();
	}

	/** one line per column of the tree, each with the points sunk into it, so the
	 *  trunk also answers for the passive column that has no emblem of its own */
	private String columns(){
		CurrentSkills hs = Dungeon.hero.heroSkills;
		StringBuilder sb = new StringBuilder();
		Object[][] cols = {
				{ hs.branchPA, CurrentSkills.BRANCHES.PASSIVEA },
				{ hs.branchA,  CurrentSkills.BRANCHES.ACTIVE },
				{ hs.branchPB, CurrentSkills.BRANCHES.PASSIVEB },
				{ hs.branchD,  CurrentSkills.BRANCHES.FOURTH },
				{ hs.branchS,  CurrentSkills.BRANCHES.SUBCLASS } };
		for (Object[] c : cols){
			if (c[0] == null) continue;
			if (sb.length() > 0) sb.append("\n");
			sb.append(Messages.get(BranchSkill.class, "column", ((Skill) c[0]).name(), hs.totalSpent((CurrentSkills.BRANCHES) c[1])));
		}
		return sb.toString();
	}

	/** the shared "points invested / next advancement" footer every branch header shows */
	protected String branchProgressInfo(){
		int skills = skillsAll(), talents = CurrentSkills.talentPointsSpent();
		String invested = trunk()
				? Messages.get(BranchSkill.class, "invested_tree", skills + talents, skills, talents) + "\n" + columns()
				: Messages.get(BranchSkill.class, "invested", totalSpent()) + "\n"
					+ Messages.get(BranchSkill.class, "invested_all", skills + talents);
		return invested + "\n"
				+ (canUpgrade() ? Messages.get(BranchSkill.class, "next_cost", nextUpgradeCost()) + "\n"
								: Messages.get(BranchSkill.class, "branch_maxed"));
	}
}
