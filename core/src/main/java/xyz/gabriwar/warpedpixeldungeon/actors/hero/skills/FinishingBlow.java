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


public class FinishingBlow extends SubSkill3 {

	{
		name = "Finishing Blow";
		castText = "Finish him!";
		image = 11;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && enemy.isAlive()
				&& !enemy.properties().contains(xyz.gabriwar.warpedpixeldungeon.actors.Char.Property.BOSS)
				&& !enemy.properties().contains(xyz.gabriwar.warpedpixeldungeon.actors.Char.Property.MINIBOSS)
				&& enemy.HP - damage > 0
				&& enemy.HP - damage <= enemy.HT * 0.05f * level){
			castTextYell();
			return enemy.HP;
		}
		return damage;
	}
}
