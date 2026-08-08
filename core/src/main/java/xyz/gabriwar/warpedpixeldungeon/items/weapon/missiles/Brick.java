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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class Brick extends MissileWeapon {

	{
		image = ItemSpriteSheet.BRICK;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.1f;

		tier = 1;
		baseUses = 5;
		sticky = false;
	}

	@Override
	public int min(int lvl) {
		return  4 * tier + 	                //4 base, up from 2, brick hits are very consistent
				lvl;                        //scaling unchanged
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//40% chance to briefly stun the target
		if (Random.Int(5) < 2) {
			Buff.prolong(defender, Paralysis.class, 2f);
		}

		//1/80 chance to shatter, spilling the mason's hidden coin stash
		if (Random.Int(80) == 0) {
			Dungeon.level.drop(new Gold(Random.IntRange(30, 100)), defender.pos).sprite.drop();
			durability = 0;
			parent = null;
		}

		return super.proc(attacker, defender, damage);
	}

}
