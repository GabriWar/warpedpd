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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

public class Bombvoyage extends ActiveSkill3 {

	{
		name = "Bombvoyage";
		castText = "Bombvoyage";
		image = 91;
		tier = 3;
		mana = 15;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.active2.active = false; // Disable Double Shot
		}
	}

	@Override
	public boolean arrowToBomb(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return false;
		else {
			castTextYell();
			Dungeon.hero.MP -= getManaCost();
			return true;
		}
	}

	//runs just before Hero detonates the bomb, so anything that survives the blast
	//is already limping - the MP check mirrors the one arrowToBomb() is about to make
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged && active && level >= 3 && enemy != null && enemy.isAlive()
				&& Dungeon.hero.MP >= getManaCost()){
			Buff.prolong( enemy, Cripple.class, 4f );
		}
		return damage;
	}

	@Override
	public int getManaCost(){
		return mana - level * 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
