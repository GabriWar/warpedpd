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


import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;

public class Predation extends Skill {

	{
		tag = "D2";
		name = "Predation";
		image = 119;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && enemy != null && isBroken( enemy )){
			return damage + Math.round( damage * 0.05f * level );
		}
		return damage;
	}

	private static boolean isBroken( Char enemy ){
		if (enemy instanceof Mob){
			Mob mob = (Mob) enemy;
			if (mob.state == mob.SLEEPING || mob.state == mob.FLEEING) return true;
		}
		return enemy.buff( Terror.class ) != null
				|| enemy.buff( Amok.class ) != null
				|| enemy.buff( Charm.class ) != null
				|| enemy.buff( Blindness.class ) != null
				|| enemy.buff( Paralysis.class ) != null
				|| enemy.buff( MagicalSleep.class ) != null
				|| enemy.buff( Roots.class ) != null;
	}
}
