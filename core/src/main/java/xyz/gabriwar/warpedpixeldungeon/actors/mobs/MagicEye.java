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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfDisintegration;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.MagicEyeSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class MagicEye extends Mob implements Callback {

	{
		spriteClass = MagicEyeSprite.class;

		HP = HT = 400;
		defenseSkill = 40;

		EXP = 16;

		flying = true;

		loot = new PotionOfMending();
		lootChance = 0.05f;

		properties.add(Property.DEMONIC);

		immunities.add(Terror.class);

		resistances.add(WandOfDisintegration.class);
		resistances.add(Grim.class);
		resistances.add(Vampiric.class);
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public float attackDelay() {
		return 1.6f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(20, 40);
	}

	@Override
	public int attackSkill( Char target ) {
		return 30;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 70);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent(pos, enemy.pos)) {
			return super.doAttack(enemy);
		} else {
			boolean visible = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos];
			if (visible) {
				sprite.zap(enemy.pos);
			}
			spend(attackDelay());

			Ballistica beam = new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID);
			for (int cell : beam.subPath(1, beam.dist)) {
				Char ch = Actor.findChar(cell);
				if (ch != null) {
					if (hit(this, ch, true)) {
						int dmg = Random.NormalIntRange(50, 100);
						ch.damage(dmg, this);
						ch.sprite.flash();
					}
				}
			}

			if (enemy == Dungeon.hero && !enemy.isAlive()) {
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "deathgaze_kill"));
			}
			return !visible;
		}
	}

	@Override
	public void call() {
		next();
	}
}
