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

import xyz.gabriwar.warpedpixeldungeon.items.magic.ManaSpell;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

public class MagicSurge extends FlavourBuff {

	public static final float DURATION = 25f;

	private String flavor = ManaSpell.MagicFamily.GENERAL.name();

	private static final String FLAVOR = "flavor";

	public void setFlavor(String flv) {
		this.flavor = flv;
	}

	public ManaSpell.MagicFamily buffFlavor() {
		for (ManaSpell.MagicFamily c : ManaSpell.MagicFamily.values()) {
			if (c.name().equals(flavor)) {
				return c;
			}
		}
		return ManaSpell.MagicFamily.GENERAL;
	}

	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(FLAVOR, flavor);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		flavor = bundle.getString(FLAVOR);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", flavor);
	}
}
