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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

public class LastStand extends Skill {

	{
		tag = "PA4";
		name = "Last Stand";
		image = 14;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int incomingDamageReduction( int damage ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null) return 0;

		//this runs inside Hero.damage, so the blow has already lost the armour's dr:
		//the barrier answers the damage the hero is really about to take
		if (hero.HP - damage <= hero.HT * 0.25f){
			Barrier barrier = Buff.affect( hero, Barrier.class );
			if (barrier.shielding() == 0){
				barrier.setShield( 4 * level );
			}
		}

		if (hero.HP <= hero.HT / 3){
			return Math.round( damage * 0.05f * level );
		}
		return 0;
	}
}
