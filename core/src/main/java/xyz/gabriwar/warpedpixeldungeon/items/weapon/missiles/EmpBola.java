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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class EmpBola extends MissileWeapon {

	{
		image = ItemSpriteSheet.EMP_BOLA;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		bones = false;

		tier = 2;
		baseUses = 1;
		sticky = false;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		Buff.prolong(defender, Cripple.class, Cripple.DURATION);
		Buff.prolong(defender, Paralysis.class, 3f);
		if (Char.hasProp(defender, Char.Property.INORGANIC)) {
			defender.damage(defender.HT / 3, this);
		}
		return super.proc(attacker, defender, damage);
	}
}
