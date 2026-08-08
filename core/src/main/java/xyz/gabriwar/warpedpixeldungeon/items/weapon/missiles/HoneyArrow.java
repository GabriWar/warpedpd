/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bee;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class HoneyArrow extends MissileWeapon {

	{
		image = ItemSpriteSheet.HONEY_ARROW;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.2f;

		bones = false;

		tier = 1;
		baseUses = 1;
		sticky = false;
	}

	@Override
	public int defaultQuantity() {
		return 2;
	}

	@Override
	protected void onThrow(int cell) {
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == curUser) {
			shatter(null, cell);
		} else {
			super.onThrow(cell);
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		shatter(null, defender.pos);
		return super.proc(attacker, defender, damage);
	}

	//spawns an angry bee on or next to the given position, similar to Honeypot.shatter
	public void shatter(Char owner, int pos) {

		if (Dungeon.level.heroFOV[pos]) {
			Sample.INSTANCE.play(Assets.Sounds.SHATTER);
			Splash.at(pos, 0xffd500, 5);
		}

		int newPos = pos;
		if (Actor.findChar(pos) != null) {
			ArrayList<Integer> candidates = new ArrayList<>();

			for (int n : PathFinder.NEIGHBOURS4) {
				int c = pos + n;
				if (!Dungeon.level.solid[c] && Actor.findChar(c) == null) {
					candidates.add(c);
				}
			}

			newPos = candidates.size() > 0 ? Random.element(candidates) : -1;
		}

		if (newPos != -1) {
			Bee bee = new Bee();
			bee.spawn(Dungeon.scalingDepth());
			bee.setPotInfo(pos, owner);
			bee.HP = bee.HT;
			bee.pos = newPos;

			GameScene.add(bee);
			if (newPos != pos) Actor.add(new Pushing(bee, pos, newPos));

			bee.sprite.alpha(0);
			bee.sprite.parent.add(new AlphaTweener(bee.sprite, 1, 0.15f));

			Sample.INSTANCE.play(Assets.Sounds.BEE);
		}
	}
}
