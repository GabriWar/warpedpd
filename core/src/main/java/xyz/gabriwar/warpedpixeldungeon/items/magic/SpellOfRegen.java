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

package xyz.gabriwar.warpedpixeldungeon.items.magic;

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BerryRegeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

public class SpellOfRegen extends ManaSpell {

	{
		mpCost = 1;
		selfCast = true;
		magicType = MagicFamily.STATUS;
		spellNum = 11;
	}

	@Override
	protected void onZap(int cell) {
		Char ch = Actor.findChar(cell);
		if (ch != null) {
			//regenLeft is both the turn count AND the rate (it heals 1 + regenLeft/25 a turn),
			//so HT * spellLevel() was ~500 turns at up to 21 HP each: full permanent regen for 1 MP
			Buff.affect(ch, BerryRegeneration.class).set(Math.round(ch.HT / 2f));
			GLog.p(Messages.get(this, "cast"));
		}
	}

	@Override
	protected void fx(int cell, Callback callback) {
		callback.call();
	}
}
