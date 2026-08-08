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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class DivineWrath extends Skill {

	{
		name = "Divine Wrath";
		tag = "PB4";
		image = 153;
		tier = 4;
		mana = 3;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0)
			return damage;

		Hero hero = Dungeon.hero;
		if (hero == null || hero.MP < getManaCost())
			return damage;

		hero.MP -= getManaCost();

		int bonus = 2 + 2 * level;
		if (Char.hasProp( enemy, Char.Property.UNDEAD ) || Char.hasProp( enemy, Char.Property.DEMONIC ))
			bonus *= 2;
		if (ranged)
			bonus /= 2;

		return damage + bonus;
	}

	@Override
	public String info(){
		return Messages.get(this, "desc", getManaCost()) + "\n"
				+ costUpgradeInfo();
	}
}
