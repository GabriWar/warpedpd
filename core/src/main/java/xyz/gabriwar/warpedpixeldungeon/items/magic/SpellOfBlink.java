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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Callback;

import java.util.List;

public class SpellOfBlink extends ManaSpell {

	{
		mpCost = 1;
		selfCast = false;
		magicType = MagicFamily.GENERAL;
		spellNum = 1;
	}

	@Override
	protected void onZap(int cell) {
		int depth = Dungeon.depth;
		if ((depth >= 50 && depth <= 54) || depth == 66) {
			GLog.w(Messages.get(this, "no_blink"));
			return;
		}

		Ballistica route = new Ballistica(curUser.pos, cell, Ballistica.MAGIC_BOLT);
		List<Integer> path = route.subPath(1, spellLevel() + 4);

		int targetPos = curUser.pos;
		for (int pos : path) {
			if (Actor.findChar(pos) == null && Dungeon.level.passable[pos]) {
				targetPos = pos;
			} else {
				break;
			}
		}

		if (targetPos != curUser.pos) {
			appear(curUser, targetPos);
			Dungeon.level.occupyCell(curUser);
			Dungeon.observe();
			GLog.p(Messages.get(this, "cast"));
		} else {
			GLog.w(Messages.get(this, "no_blink"));
		}
	}

	public static void appear(Char ch, int pos) {
		ch.move(pos, false);
		if (ch.pos == pos) {
			ch.sprite.interruptMotion();
			ch.sprite.place(pos);
		}

		if (ch.invisible == 0) {
			ch.sprite.alpha(0);
			ch.sprite.parent.add(new AlphaTweener(ch.sprite, 1, 0.4f));
		}

		ch.sprite.emitter().start(Speck.factory(Speck.LIGHT), 0.2f, 3);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
	}

	@Override
	protected void fx(int cell, Callback callback) {
		callback.call();
	}
}
