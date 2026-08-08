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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class WandOfAvalanche extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_BLAST_WAVE;

		//no STOP_TARGET: an avalanche flies over whoever is in the way and comes down on the
		//wall behind them. Stopping at the first target turned it into a single-target bolt.
		collisionProperties = Ballistica.STOP_SOLID;
	}

	@Override
	public int min(int lvl) {
		return 2 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 8 + 3 * lvl;
	}

	@Override
	public void onZap(Ballistica bolt) {

		Sample.INSTANCE.play(Assets.Sounds.ROCKS);

		int level = buffedLvl();
		int size = 1 + level / 3;

		//the blast radius grows with the wand. It used to be a hardcoded 3x3 - `size` was
		//computed and then only spent on the particle count - so upgrading widened nothing.
		PathFinder.buildDistanceMap(bolt.collisionPos, BArray.not(Dungeon.level.solid, null), size);

		for (int c = 0; c < Dungeon.level.length(); c++) {
			int d = PathFinder.distance[c];
			if (d == Integer.MAX_VALUE) continue;

			Char ch = Actor.findChar(c);
			if (ch != null) {
				//and it falls off with distance from the impact, rather than hitting the far
				//edge of the blast as hard as the centre
				ch.damage(damageRoll() + (size - d) * 2, this);
				wandProc(ch, chargesPerCast());

				if (ch.isAlive() && Random.Int(2 + d) == 0) {
					Buff.prolong(ch, Paralysis.class, Random.IntRange(2, 4));
				}
			}

			if (Dungeon.level.heroFOV[c]) {
				CellEmitter.get(c).start(Speck.factory(Speck.ROCK), 0.07f, 3 + size - d);
			}
		}

		Camera.main.shake(3, 0.3f);

		if (!curUser.isAlive()) {
			Dungeon.fail(this);
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(
				curUser.sprite.parent,
				MagicMissile.EARTH,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		int level = Math.max(0, buffedLvl());
		float procChance = (level + 1f) / (level + 3f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {
			Buff.prolong(defender, Paralysis.class, Random.IntRange(1, 3));
		}
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x664400);
		particle.am = 0.6f;
		particle.setLifespan(1f);
		particle.acc.set(0, 30);
		particle.setSize(1f, 3f);
		particle.shuffleXY(1f);
	}
}
