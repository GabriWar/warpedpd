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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Harp extends MeleeWeapon {

	{
		image = ItemSpriteSheet.HARP;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.3f;

		tier = 5;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  4*(tier+1) +    //24 base, down from 30
				lvl*(tier);     //+5 per level, down from +6
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//the harp's resonance strikes everyone else adjacent to the target
		//for a quarter of a regular damage roll
		int p = defender.pos;
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(p + n);
			if (ch != null && ch != attacker && ch != defender && ch.isAlive()) {
				ch.damage(augment.damageFactor(Random.NormalIntRange(min(), max())) / 4, this);
			}
		}

		return super.proc(attacker, defender, damage);
	}

	//SPS-PD weapon: no Duelist ability was ever designed for it
	@Override
	public boolean hasDuelistAbility() {
		return false;
	}
}
