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


import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SacredWeaponSwords;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

public class SacredWeapon extends PassiveSkillB3 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	//chance, per melee hit, that a spectral copy of the weapon falls and plants itself beside the target
	private static final int CHANCE = 20;

	{
		name = "Sacred Weapon";
		image = 110;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//the level caps how many swords stand at once: 1 / 2 / 3
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || ranged || enemy == null || hero == null || !enemy.isAlive() || Random.Int( 100 ) >= CHANCE)
			return damage;
		Buff.affect( hero, SacredWeaponSwords.class ).plant( enemy, level );
		return damage;
	}
}
