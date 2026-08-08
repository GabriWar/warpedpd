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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

public class LockSmith extends PassiveSkillA3 {

	{
		name = "Lock Smith";
		tier = 3;
		image = 51;
	}

	@Override
	public boolean disableTrap(){
		if (Random.Int(100) < 33 * level){
			castText = Messages.get(this, "cast");
			castTextYell();
			if (level >= Skill.MAX_LEVEL) stripForParts();
			return true;
		}
		castText = Messages.get(this, "cast_fail", name());
		castTextYell();
		return false;
	}

	//at mastery the jammed mechanism is not just neutralised, it is robbed:
	//the salvaged coin then runs through Bandit's and Master Thief's loot bonus
	private void stripForParts(){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		Dungeon.level.drop( new Gold( 8 + 4 * CurrentSkills.skillLevel(Bandit.class) ), hero.pos ).sprite.drop();
		hero.MP = Math.min( hero.MT, hero.MP + 2 );
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
