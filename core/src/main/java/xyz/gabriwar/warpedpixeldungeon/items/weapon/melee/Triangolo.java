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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//SPS-PD's triangolo: resonates on-hit, striking everything adjacent to the target
public class Triangolo extends MeleeWeapon {

	{
		image = ItemSpriteSheet.TRIANGOLO;
		hitSound = Assets.Sounds.HIT_PARRY;
		hitSoundPitch = 1.3f;

		tier = 1;
	}

	//SPS-PD grew reach by 1 per upgrade, to a max of 4
	@Override
	public int reachFactor(Char owner) {
		int reach = super.reachFactor(owner);
		if (!(owner instanceof Hero && RingOfForce.fightingUnarmed((Hero) owner))){
			reach += Math.min(3, Math.max(0, buffedLvl()));
		}
		return reach;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//resonance: full weapon damage to every other char adjacent to the target
		int p = defender.pos;
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(p + n);
			if (ch != null && ch != defender && ch != attacker && ch.isAlive()) {
				int dmg = Math.max(Random.NormalIntRange(min(), max()) - Random.IntRange(0, 1), 0);
				ch.damage(dmg, this);
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
