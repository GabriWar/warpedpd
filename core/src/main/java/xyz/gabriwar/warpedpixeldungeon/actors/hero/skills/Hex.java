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

public class Hex extends SubSkill2 {

	{
		name = "Hex";
		castText = "Suffer";
		image = 59;
		mana = 8;
		tier = 2;
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
			xyz.gabriwar.warpedpixeldungeon.actors.Char nearest = null;
			for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob : Dungeon.level.mobs.toArray(new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0])){
				if (Dungeon.level.heroFOV[mob.pos] && mob.alignment == xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY){
					if (nearest == null || Dungeon.level.distance(hero.pos, mob.pos) < Dungeon.level.distance(hero.pos, nearest.pos)){
						nearest = mob;
					}
				}
			}
			if (nearest == null){
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( "No target in sight." );
				return;
			}
			Buff.prolong( nearest, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness.class, 3 + 2 * level );
			Buff.prolong( nearest, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable.class, 3 + 2 * level );
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
