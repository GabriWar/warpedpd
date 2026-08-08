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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Water;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ScrollOfRegrowth extends Scroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_REGROWTH;
	}

	{
		bones = true;
	}

	@Override
	public void doRead() {

		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean[] mapped = Dungeon.level.mapped;
		boolean[] discoverable = Dungeon.level.discoverable;

		boolean noticed = false;

		for (int i = 0; i < length; i++) {

			GameScene.add(Blob.seed(i, 40, Water.class));

			int terr = map[i];

			if (discoverable[i]) {

				mapped[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {

					Dungeon.level.discover(i);

					if (Dungeon.level.heroFOV[i]) {
						GameScene.discoverTile(i, terr);
						CellEmitter.get(i).start(Speck.factory(Speck.DISCOVER), 0.1f, 4);
						noticed = true;
					}
				}
			}
		}
		Dungeon.observe();

		GLog.i(Messages.get(this, "layout"));
		if (noticed) {
			Sample.INSTANCE.play(Assets.Sounds.SECRET);
		}

		SpellSprite.show(curUser, SpellSprite.MAP);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		Invisibility.dispel();

		identify();

		curUser.spendAndNext(TIME_TO_READ);
		readAnimation();
	}

	@Override
	public int value() {
		return isKnown() ? 25 * quantity : super.value();
	}
}
