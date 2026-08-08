/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class Spork extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SPORK;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.3f;

		tier = 3;
		DLY = 0.5f; //2x speed
		ACC = 1.2f; //20% more accurate
		reinforced = true; //can be pushed past +15 in the upgrade blobs
	}

	@Override
	public int max(int lvl) {
		return  3*(tier+1) +    //12 base, down from 20 (balanced by speed)
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (defender instanceof Gullin || defender instanceof Kupua
				|| defender instanceof MineSentinel || defender instanceof Otiluke
				|| defender instanceof Zot || defender instanceof ZotPhase) {
			defender.damage(Random.Int(damage, damage * 4), this);
		}
		return super.proc(attacker, defender, damage);
	}


	//Sprouted weapon: no Duelist ability was ever designed for it
	@Override
	public boolean hasDuelistAbility() {
		return false;
	}
}
