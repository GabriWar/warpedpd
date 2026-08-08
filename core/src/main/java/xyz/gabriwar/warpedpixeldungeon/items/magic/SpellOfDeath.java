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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class SpellOfDeath extends ManaSpell {

	{
		mpCost = 1;
		selfCast = false;
		magicType = MagicFamily.STATUS;
		spellNum = 28;
	}

	@Override
	protected void onZap(int cell) {
		Char ch = Actor.findChar(cell);
		if (ch != null) {
			//Random.Int(10) < spellLevel() made this a certain kill from magicLevel 48 up,
			//on anything, for 1 MP. Sprouted's roll is asymptotic - it approaches ~5% and
			//never reaches certainty however high the level goes. Bosses are never executed.
			boolean canExecute = !Char.hasProp(ch, Char.Property.BOSS)
					&& !Char.hasProp(ch, Char.Property.MINIBOSS);

			if (canExecute && Random.Int(spellLevel() + 200) < spellLevel()) {
				ch.damage(ch.HP, this);
				ch.sprite.emitter().burst(ShadowParticle.CURSE, 6);
			} else {
				Terror t = Buff.affect(ch, Terror.class, 5f);
				t.object = curUser.id();
			}
		}
	}

	@Override
	protected void fx(int cell, Callback callback) {
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		MagicMissile.boltFromChar(curUser.sprite.parent, MagicMissile.SHADOW, curUser.sprite, cell, callback);
	}
}
