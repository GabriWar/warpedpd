/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class SnowedIn extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static final float DURATION = 10f;

	@Override
	public boolean attachTo(Char target) {
		if (!target.flying && super.attachTo(target)) {
			target.rooted = true;
			//being buried in snow leaves the victim bitterly cold: heavy chill and a
			//body temperature dump well below the hypothermia threshold
			Buff.prolong( target, Chill.class, Chill.DURATION );
			target.bodyTemp = Float.isNaN(target.bodyTemp) ? -15f : Math.min( target.bodyTemp, -15f );
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void detach() {
		if (target.buff(Roots.class) == null) {
			target.rooted = false;
		}
		super.detach();
	}

	@Override
	public int icon() {
		return BuffIndicator.FROST;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.6f, 0.8f, 1.0f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String toString() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}
}
