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

public class DragonKick extends SubSkill3 {

	{
		name = "Dragon Kick";
		castText = "HWAAA!";
		image = 18;
		mana = 12;
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
			boolean hit = false;
			for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
				xyz.gabriwar.warpedpixeldungeon.actors.Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( hero.pos + n );
				if (ch != null && ch.alignment == xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY){
					ch.damage( com.watabou.utils.Random.NormalIntRange( 3 + 2 * level, 8 + 4 * level ), this );
					if (ch.isAlive() && ch.pos != hero.pos){
						int opposite = ch.pos + (ch.pos - hero.pos);
						xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica traj =
								new xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica( ch.pos, opposite,
										xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica.MAGIC_BOLT );
						xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave.throwChar( ch, traj, 1, true, false, this );
					}
					hit = true;
				}
			}
			if (!hit){
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( "No one in reach." );
				return;
			}
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
