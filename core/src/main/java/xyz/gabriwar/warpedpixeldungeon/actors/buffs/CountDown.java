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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BanditKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

public class CountDown extends Buff {

	{
		type = buffType.NEGATIVE;
	}

	private int ticks = 0;

	private static final String TICKS = "ticks";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TICKS, ticks);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		ticks = bundle.getInt(TICKS);
	}

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(6 - ticks);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", 6 - ticks);
	}

	@Override
	public boolean act() {
		if (target.isAlive()) {
			ticks++;
			GLog.w( Messages.get(this, "tick", 6 - ticks) );

			if (ticks > 5) {
				GLog.w( Messages.get(this, "up") );
				target.sprite.emitter().burst(ShadowParticle.CURSE, 6);
				target.damage(Math.round(target.HT / 4f), this);
				for (Mob mob : Dungeon.level.mobs) {
					if (mob instanceof BanditKing) {
						mob.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
						mob.HP = Math.min(mob.HP + (Math.round(target.HT / 8f)), mob.HT);
					}
				}
				GLog.w( Messages.get(this, "feed") );
				detach();
			}
		}

		if (!target.isAlive() && target == Dungeon.hero) {
			Dungeon.fail(this);
			GLog.n( Messages.get(this, "ondeath") );
		}

		spend(TICK);
		return true;
	}
}
