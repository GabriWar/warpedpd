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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class IncendiaryShuriken extends MissileWeapon {

	{
		image = ItemSpriteSheet.SHURIKEN;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.2f;

		tier = 2;
		baseUses = 5;
		DLY = 0.5f; //2x throw speed - the whole point of the thing
	}

	@Override
	public int min(int lvl) {
		return 2 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 6 + 2 * lvl;
	}

	@Override
	protected void onThrow(int cell) {
		Char enemy = Actor.findChar(cell);
		if ((enemy == null || enemy == curUser) && Dungeon.level.flamable[cell]) {
			GameScene.add(Blob.seed(cell, 4, Fire.class));
		} else {
			super.onThrow(cell);
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		Buff.affect(defender, Burning.class).reignite(defender);
		return super.proc(attacker, defender, damage);
	}
}
