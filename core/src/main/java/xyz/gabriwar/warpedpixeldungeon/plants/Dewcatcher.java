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
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.VioletDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.YellowDewdrop;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Dewcatcher extends Plant {

	{
		image = 14;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -5f; }

	@Override
	public void activate( Char ch ) {
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]) {
				if (Random.Int(10) == 0) {
					Dungeon.level.drop(new VioletDewdrop(), c).sprite.drop();
				} else if (Random.Int(5) == 0) {
					Dungeon.level.drop(new RedDewdrop(), c).sprite.drop();
				} else if (Random.Int(3) == 0) {
					Dungeon.level.drop(new YellowDewdrop(), c).sprite.drop();
				}
			}
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
		if (Random.Int(10) == 0) {
			Dungeon.level.drop(new VioletDewdrop(), newpos).sprite.drop();
		} else if (Random.Int(5) == 0) {
			Dungeon.level.drop(new RedDewdrop(), newpos).sprite.drop();
		} else if (Random.Int(3) == 0) {
			Dungeon.level.drop(new YellowDewdrop(), newpos).sprite.drop();
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_DEWCATCHER;

			plantClass = Dewcatcher.class;
		}
	}
}
