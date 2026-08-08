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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Callback;

public class WandOfBlink extends Wand {

	{
		image = ItemSpriteSheet.WAND_PRISMATIC_LIGHT;

		collisionProperties = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID;
	}

	@Override
	public void onZap(Ballistica bolt) {

		//no blinking on the deep journal/sokoban floors — you could hop the gaps and
		//walk off with the prizes without solving the puzzles (same gate as Telekinesis).
		//fx() already hid the hero's sprite by the time we get here, so put it back —
		//bailing out without this leaves the hero invisible for the rest of the floor.
		if (Dungeon.depth > 50) {
			curUser.sprite.visible = true;
			GLog.w(Messages.get(this, "no_blink"));
			return;
		}

		int maxDist = buffedLvl() + 4;
		int cell;

		if (bolt.dist > maxDist) {
			cell = bolt.path.get(maxDist);
		} else if (Actor.findChar(bolt.collisionPos) != null && bolt.dist > 1) {
			cell = bolt.path.get(bolt.dist - 1);
		} else {
			cell = bolt.collisionPos;
		}

		if (Actor.findChar(cell) != null) {
			//back off one tile but never past the max blink range
			cell = bolt.path.get(Math.min(bolt.dist - 1, maxDist));
		}

		curUser.sprite.visible = true;

		curUser.sprite.interruptMotion();
		curUser.move(cell);
		curUser.sprite.place(cell);

		if (curUser.invisible == 0) {
			curUser.sprite.alpha(0);
			curUser.sprite.parent.add(new AlphaTweener(curUser.sprite, 1, 0.4f));
		}

		curUser.sprite.emitter().start(Speck.factory(Speck.LIGHT), 0.2f, 3);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);

		Dungeon.level.occupyCell(curUser);
		Dungeon.observe();

		wandProc(curUser, chargesPerCast());
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		curUser.sprite.visible = false;
		MagicMissile.boltFromChar(
				curUser.sprite.parent,
				MagicMissile.BEACON,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//no on-hit effect
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xFFFFFF);
		particle.am = 0.5f;
		particle.setLifespan(0.6f);
		particle.setSize(1f, 2f);
		particle.shuffleXY(0.5f);
	}
}
