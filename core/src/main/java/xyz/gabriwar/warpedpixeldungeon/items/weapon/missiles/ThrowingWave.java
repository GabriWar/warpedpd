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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ThrowingWave extends MissileWeapon {

	{
		image = ItemSpriteSheet.THROWING_WAVE;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.3f;

		bones = false;

		tier = 1;
		baseUses = 1;
		sticky = false;

		DLY = 0.5f; //2x speed
	}

	@Override
	public int defaultQuantity() {
		return 4;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		Buff.affect(defender, Amok.class, 10f);
		Buff.affect(defender, Charm.class, 5f).object = attacker.id();
		defender.damage(defender.HP / 4, this);
		return super.proc(attacker, defender, damage);
	}
}
