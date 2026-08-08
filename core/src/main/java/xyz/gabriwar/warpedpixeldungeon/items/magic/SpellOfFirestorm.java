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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class SpellOfFirestorm extends ManaSpell {

	{
		mpCost = 1;
		selfCast = false;
		magicType = MagicFamily.FIRE;
		spellNum = 15;
	}

	@Override
	protected void onZap(int cell) {
		int level = spellLevel();

		Ballistica bolt = new Ballistica(curUser.pos, cell, Ballistica.MAGIC_BOLT);
		for (int c : bolt.subPath(1, bolt.dist - 1)) {
			if (Dungeon.level.flamable[c]) {
				GameScene.add(Blob.seed(c, 1, Fire.class));
			}
		}

		GameScene.add(Blob.seed(cell, 1, Fire.class));

		Char ch = Actor.findChar(cell);
		if (ch != null) {
			int damage = Random.Int(level * level, level * level * 10);
			if (checkFam(curUser)) {
				damage *= 4;
			}
			ch.damage(damage, this);
			Buff.affect(ch, Burning.class).reignite(ch);
		}
	}

	@Override
	protected void fx(int cell, Callback callback) {
		MagicMissile.boltFromChar(curUser.sprite.parent, MagicMissile.FIRE, curUser.sprite, cell, callback);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	@Override
	protected int magicDivisor() {
		return 3;
	}
}
