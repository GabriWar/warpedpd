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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

public class Awareness extends PassiveSkillA2 {

	{
		name = "Awareness";
		image = 75;
		tier = 2;
	}

	@Override
	public boolean dodgeChance(){
		if (Random.Int(100) < 10 * level){
			castText = Messages.get(this, "cast");
			castTextYell();
			//slipping the shot starts the kite
			Buff.prolong( Dungeon.hero, Haste.class, 1 + level );
			return true;
		}
		castText = Messages.get(this, "cast_fail");
		castTextYell();
		return false;
	}

	//the shout swaps between hit and miss lines, so the live field wins over the bundle
	@Override
	public String castText(){
		return castText;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
