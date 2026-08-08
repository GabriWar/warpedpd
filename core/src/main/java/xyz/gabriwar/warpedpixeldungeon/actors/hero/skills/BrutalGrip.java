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
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.Random;

public class BrutalGrip extends Skill {

	{
		tag = "CB";
		name = "Brutal Grip";
		image = 138;
		tier = 4;
	}

	//live only while holding a heavy (tier 4+) melee weapon
	private boolean qualifies(){
		if (level <= 0 || Dungeon.hero == null) return false;
		KindOfWeapon w = Dungeon.hero.belongings.weapon();
		return w instanceof MeleeWeapon && ((MeleeWeapon)w).tier >= 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public float damageModifier(){
		return qualifies() ? 1f + 0.07f * level : 1f;
	}

	//you commit to the swing, so you eat more of what comes back
	@Override
	public float incomingDamageModifier(){
		return qualifies() ? 1f + 0.03f * level : 1f;
	}

	//the melee cripple is rolled here rather than through cripple(), which only fires on ranged attacks
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged && enemy != null && enemy.isAlive() && qualifies()
				&& Random.Int( 100 ) < 8 * level){
			Buff.prolong( enemy, Cripple.class, 3 + level );
		}
		return damage;
	}
}
