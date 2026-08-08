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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

public class Lunge extends ActiveSkill1 {

	{
		name = "Lunge";
		castText = "Lunge!";
		image = 97;
		mana = 3;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.active2.active = false;
			hero.heroSkills.active3.active = false;
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

	//damageModifier() runs once per swing, before attackProc: remember whether this
	//particular swing was actually paid for, so the opening below is never free
	private boolean paidForSwing = false;

	@Override
	public float damageModifier(){
		if (!active || Dungeon.hero.MP < getManaCost()){
			paidForSwing = false;
			return 1f;
		} else {
			castTextYell();
			Dungeon.hero.MP -= getManaCost();
			paidForSwing = true;
			return 1f + 0.12f * level;
		}
	}

	//a fully committed lunge leaves the target open
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (paidForSwing && !ranged && level == MAX_LEVEL
				&& enemy != null && enemy.isAlive()){
			Buff.prolong( enemy, Vulnerable.class, 3f );
		}
		return damage;
	}
}
