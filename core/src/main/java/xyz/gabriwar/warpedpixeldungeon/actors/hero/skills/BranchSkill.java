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

	/** the shared "points invested / next advancement" footer every branch header shows */
	protected String branchProgressInfo(){
		return Messages.get(BranchSkill.class, "invested", totalSpent()) + "\n"
				+ (canUpgrade() ? Messages.get(BranchSkill.class, "next_cost", nextUpgradeCost()) + "\n"
								: Messages.get(BranchSkill.class, "branch_maxed"));
	}
}
