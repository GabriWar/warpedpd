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

public class IronStance extends ActiveSkill {

	{
		tag = "CC";
		name = "Iron Stance";
		castText = "Hold!";
		image = 13;
		tier = 4;
		mana = 3;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			// only one stance or attack toggle at a time
			hero.heroSkills.deactivateOtherToggles( this );
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

	@Override
	public float damageModifier(){
		if (!active || level <= 0)
			return 1f;
		return 0.85f;
	}

	@Override
	public int onDefendProc( Char enemy, int damage ){
		if (!active || level <= 0 || Dungeon.hero.MP < getManaCost())
			return damage;
		castTextYell();
		Dungeon.hero.MP -= getManaCost();
		return Math.round( damage * (1f - 0.12f * level) );
	}
}
