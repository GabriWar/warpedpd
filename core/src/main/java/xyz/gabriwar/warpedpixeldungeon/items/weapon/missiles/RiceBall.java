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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drowsy;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class RiceBall extends MissileWeapon {

	{
		image = ItemSpriteSheet.RICE_BALL;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.0f;

		bones = false;

		tier = 1;
		baseUses = 5;
	}

	@Override
	public int min(int lvl) {
		return 1;
	}

	@Override
	public int max(int lvl) {
		return 2 + lvl;
	}

	@Override
	public float delayFactor(Char owner) {
		return super.delayFactor(owner) * 0.25f;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//only lands on an unhurt target - the source gated this on HP/HT > 0.01f, which is
		//integer division, so it only ever passed at full health. Without the gate the ball
		//teleports anything away, including the wounded thing you were about to finish off.
		//The undead and the inorganic are unmoved by it, as in the source.
		if (defender.HP == defender.HT
				&& !Char.hasProp(defender, Char.Property.BOSS)
				&& !Char.hasProp(defender, Char.Property.MINIBOSS)
				&& !Char.hasProp(defender, Char.Property.IMMOVABLE)
				&& !Char.hasProp(defender, Char.Property.UNDEAD)
				&& !Char.hasProp(defender, Char.Property.INORGANIC)) {

			Buff.affect(defender, Drowsy.class);
			defender.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);

			ScrollOfTeleportation.teleportChar(defender);
		}

		return super.proc(attacker, defender, damage);
	}
}
