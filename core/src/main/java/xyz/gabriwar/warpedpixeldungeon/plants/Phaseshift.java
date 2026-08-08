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

package xyz.gabriwar.warpedpixeldungeon.plants;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfTransmutation;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WellWater;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Phaseshift extends Plant {

	{
		image = 15;
		seedClass = Seed.class;
	}

	@Override
	public void activate(Char ch) {
		if (ch == null) {
			WellWater.affectCellPlant(pos);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		int newpos;
		int trys = 8;
		do {
			newpos = ch.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			trys--;
			if (trys <= 0) return;
		} while (!Dungeon.level.passable[newpos]);
		GameScene.add(Blob.seed(newpos, 1, WaterOfTransmutation.class));
	}

	public static boolean checkWater() {
		WellWater water = (WellWater) Dungeon.level.blobs.get(WaterOfTransmutation.class);
		return water != null && water.volume > 0;
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_PHASEPITCHER;

			plantClass = Phaseshift.class;
		}

		@Override
		public Plant couch(int pos, Level level) {
			//seed into the given level: during worldgen Dungeon.level is null/stale
			Blob water = Blob.seed(pos, 1, WaterOfTransmutation.class, level);
			if (level == Dungeon.level) GameScene.add(water);
			return super.couch(pos, level);
		}
	}
}
