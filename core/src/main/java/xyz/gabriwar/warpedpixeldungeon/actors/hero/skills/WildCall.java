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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

import java.util.ArrayList;

public class WildCall extends SubSkill3 {

	{
		name = "Wild Call";
		castText = "To me!";
		image = 42;
		mana = 10;
		tier = 3;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			if (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet.summonedPets >= 3 + hero.heroSkills.allSummonLimit()){
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( "You cannot control more summons." );
				return;
			}
			java.util.ArrayList<Integer> candidates = new java.util.ArrayList<>();
			for (int n : com.watabou.utils.PathFinder.NEIGHBOURS4){
				int c = hero.pos + n;
				if (c < 0 || c >= Dungeon.level.length()) continue;
				if (Dungeon.level.passable[c] && xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar(c) == null){
					candidates.add(c);
				}
			}
			if (candidates.isEmpty()) return;
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet beast =
					new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet(
							xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet.PET_TYPES.CRAB );
			beast.name = "Wild Beast";
			beast.spawn( 2 * level );
			beast.pos = com.watabou.utils.Random.element(candidates);
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add(beast);
			hero.MP -= getManaCost();
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
