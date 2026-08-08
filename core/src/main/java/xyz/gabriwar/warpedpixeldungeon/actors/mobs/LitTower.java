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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ConfusionGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.LitTowerSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class LitTower extends Mob implements Callback {

	private static final float TIME_TO_ZAP = 2f;

	{
		spriteClass = LitTowerSprite.class;

		HP = HT = 600;
		defenseSkill = 1000;

		EXP = 25;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		loot = new RedDewdrop();
		lootChance = 1f;

		properties.add(Property.IMMOVABLE);

		immunities.add(ToxicGas.class);
		immunities.add(Terror.class);
		immunities.add(ConfusionGas.class);

		resistances.add(Electricity.class);
		resistances.add(ScrollOfPsionicBlast.class);
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public void beckon(int cell) {
		// Do nothing
	}

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill( Char target ) {
		return 100;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1000);
	}

	@Override
	public void damage(int dmg, Object src) {
		// LitTower is invulnerable
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	protected boolean act() {
		if (Dungeon.level.distance(pos, Dungeon.hero.pos) < 5
				&& Dungeon.hero.isAlive()
				&& checkOtiluke()) {
			zapAll(Dungeon.hero.pos);
		}
		return super.act();
	}

	protected boolean checkOtiluke() {
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof Otiluke) {
				return true;
			}
		}
		return false;
	}

	public void zapAll(int loc) {
		yell(Messages.get(this, "zap"));

		Char hero = Dungeon.hero;
		int mobDmg = Random.Int(300, 600);

		boolean visible = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[loc];

		if (visible) {
			((LitTowerSprite) sprite).zap(loc);
		}

		hero.damage(mobDmg, this);

		hero.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
		hero.sprite.flash();

		Camera.main.shake(2, 0.3f);

		if (!hero.isAlive()) {
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "lightning_kill"));
		}
	}

	@Override
	protected boolean doAttack(Char enemy) {
		return false;
	}

	@Override
	public void call() {
		next();
	}
}
