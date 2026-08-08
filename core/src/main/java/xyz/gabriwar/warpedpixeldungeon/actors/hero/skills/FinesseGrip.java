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
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.Random;

public class FinesseGrip extends Skill {

	{
		tag = "CA";
		name = "Finesse Grip";
		image = 141;
		tier = 4;
	}

	//live while unarmed or holding a light (tier 1-3) melee weapon
	private boolean qualifies(){
		if (level <= 0 || Dungeon.hero == null) return false;
		KindOfWeapon w = Dungeon.hero.belongings.weapon();
		if (w == null) return true;
		if (!(w instanceof MeleeWeapon)) return false;
		return ((MeleeWeapon)w).tier <= 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public float damageModifier(){
		return qualifies() ? 1f + 0.05f * level : 1f;
	}

	@Override
	public int toHitBonus(){
		return qualifies() ? 3 * level : 0;
	}

	@Override
	public boolean dodgeChance(){
		return qualifies() && Random.Int( 100 ) < 3 * level;
	}
}
