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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//Ported from SPS-PD. A unique, unbreakable returning boomerang.
public class Boomerang extends HeavyBoomerang {

	{
		image = ItemSpriteSheet.BOOMERANG;

		tier = 2;

		unique = true;
		bones = false;

		//at 100+ uses per durability the weapon never breaks, like the SPS original
		baseUses = 100;
	}

	@Override
	public int defaultQuantity() {
		return 1;
	}

	//SPS stats: 5-10 base, +2 min / +4 max per upgrade
	@Override
	public int min(int lvl) {
		return 5 + 2*lvl;
	}

	@Override
	public int max(int lvl) {
		return 10 + 4*lvl;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//SPS: 1 in 50 hits shakes a dewdrop loose from the target
		if (Random.Int(50) == 0){
			Dungeon.level.drop(new Dewdrop(), defender.pos).sprite.drop();
		}
		return super.proc(attacker, defender, damage);
	}

}
