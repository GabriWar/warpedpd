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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class BottleFire extends MissileWeapon {

	{
		image = ItemSpriteSheet.BOTTLE_FIRE;
		hitSound = Assets.Sounds.SHATTER;
		hitSoundPitch = 1f;

		bones = false;

		tier = 1;
		baseUses = 1;
		sticky = false;
	}

	@Override
	protected void onThrow(int cell) {
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == curUser) {
			//shatters on the ground, igniting the surrounding tiles
			for (int offset : PathFinder.NEIGHBOURS9) {
				int c = cell + offset;
				if (Dungeon.level.flamable[c]
						|| Actor.findChar(c) != null
						|| Dungeon.level.heaps.get(c) != null) {
					GameScene.add(Blob.seed(c, 5, Fire.class));
				}
			}
			if (Dungeon.level.heroFOV[cell]) {
				Sample.INSTANCE.play(Assets.Sounds.SHATTER);
			}
		} else {
			super.onThrow(cell);
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		Buff.affect(defender, Burning.class).reignite(defender, 10f);
		return super.proc(attacker, defender, damage);
	}
}
