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

package xyz.gabriwar.warpedpixeldungeon.items.armor;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//follower's overcoat: taking a hit has a chance to heal the wearer
//for a quarter of the damage dealt
public class FollowerArmor extends Armor {

	{
		image = ItemSpriteSheet.FOLLOWER_ARMOR;
	}

	public FollowerArmor() {
		super( 3 );
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {

		int heal = damage / 4;

		damage = super.proc(attacker, defender, damage);

		if (Random.Int(8) == 0 && heal > 0) {
			defender.HP = Math.min(defender.HT, defender.HP + heal);
			if (defender.sprite != null) {
				defender.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
			}
		}

		return damage;
	}

	//a thick overcoat: warm and slow to give up its heat
	@Override
	public float thermalOffset() {
		return super.thermalOffset() + 1.5f;
	}

	@Override
	public float thermalMass() {
		return 0.85f;
	}
}
