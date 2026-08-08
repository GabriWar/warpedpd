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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SummonSkeleton extends ActiveSkill3 {

	{
		name = "Summon Skeleton";
		castText = "The dead shall obey!";
		image = 43;
		mana = 3;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_SUMMON);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_SUMMON)){

			if (SummonedPet.summonedPets >= 3 + hero.heroSkills.allSummonLimit()){
				GLog.w( "You cannot control more summons." );
				return;
			}

			ArrayList<Integer> candidates = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS4){
				int c = hero.pos + n;
				if (c < 0 || c >= Dungeon.level.length())
					continue;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					candidates.add(c);
				}
			}
			int newPos = candidates.size() > 0 ? Random.element(candidates) : -1;
			if (newPos != -1){
				SummonedPet pet = new SummonedPet(SummonedPet.PET_TYPES.SKELETON);
				pet.spawn(level + Summoner.markBonus());
				pet.pos = newPos;
				GameScene.add(pet);
				Actor.addDelayed(new Pushing(pet, hero.pos, newPos), -1);
				pet.sprite.alpha(0);
				pet.sprite.parent.add(new AlphaTweener(pet.sprite, 1, 0.15f));

				hero.MP -= getManaCost();
				castTextYell();
				hero.spend( TIME_TO_USE );
				hero.busy();
				hero.sprite.operate( hero.pos );
			}
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
