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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

public class RecklessFury extends ActiveSkill {

	{
		tag = "CD";
		name = "Reckless Fury";
		castText = "No guard!";
		image = 15;
		tier = 4;
		mana = 4;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			// only one stance or attack toggle at a time
			for (Skill s : hero.heroSkills.activeSkills){
				if (s != this) s.active = false;
			}
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//on-hit rather than damageModifier(): Hero.damageRoll() also rolls for mirror images and the
	//like, and those swings must not spend the hero's mana or shout in his name
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!active || level <= 0 || ranged || Dungeon.hero.MP < getManaCost())
			return damage;
		castTextYell();
		Dungeon.hero.MP -= getManaCost();
		return Math.round( damage * (1f + 0.15f * level) );
	}

	@Override
	public float incomingDamageModifier(){
		if (!active || level <= 0)
			return 1f;
		return 1.10f;
	}
}
