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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class CounterTime extends Skill {

	{
		tag = "D3";
		name = "Counter-Time";
		image = 139;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onDefendProc( Char enemy, int damage ){
		if (level <= 0 || enemy == null || !enemy.isAlive() || enemy == Dungeon.hero){
			return damage;
		}
		if (!Dungeon.level.adjacent( enemy.pos, Dungeon.hero.pos )){
			return damage;
		}
		if (Random.Int( 100 ) < 10 * level){
			enemy.damage( Math.round( weaponRoll() * 0.5f ), this );
			if (Dungeon.hero.sprite != null){
				Dungeon.hero.sprite.showStatus( CharSprite.POSITIVE, "counter!" );
			}
			return Math.round( damage * 0.5f );
		}
		return damage;
	}

	//a raw roll of whatever is in hand. Deliberately not Hero.damageRoll(): that path runs the
	//whole skill tree's damage modifiers, so an active toggle like Lunge would spend mana and
	//yell its cast text, and one-shot buffs (PhysicalEmpower, moon fury) would be eaten - none
	//of which a passive counter is allowed to do.
	private static int weaponRoll(){
		Hero hero = Dungeon.hero;
		KindOfWeapon wep = hero.belongings.weapon();
		return wep != null ? wep.damageRoll( hero ) : RingOfForce.damageRoll( hero );
	}
}
