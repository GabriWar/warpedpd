/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.potions.exotic;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.butters.Butter;
import xyz.gabriwar.warpedpixeldungeon.tiles.butters.PotionButterVariant1;
import xyz.gabriwar.warpedpixeldungeon.tiles.butters.PotionButterVariant2;
import xyz.gabriwar.warpedpixeldungeon.tiles.butters.PotionButterVariant3;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfSlime extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_SLIME;
	}
	@Override
	public void apply(Hero hero) {
		identify();
		int count = 0;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			for (int offset : PathFinder.NEIGHBOURS9) {
				int cell = mob.pos + offset;
				if (Dungeon.level.passable[cell]
						&& (Dungeon.level.map[cell] == Terrain.EMPTY
						|| Dungeon.level.map[cell] == Terrain.EMPTY_DECO
						|| Dungeon.level.map[cell] == Terrain.EMPTY_SP)
						&& Dungeon.level.plants.get(cell) == null) {
					int choice = Random.Int(3);
					Butter variant = choice == 0 ? new PotionButterVariant1()
							: choice == 1 ? new PotionButterVariant2()
							: new PotionButterVariant3();
					Dungeon.level.setButters(variant, cell);
					GameScene.updateMap(cell);
				}
			}
			count++;
		}
		if (count > 0) GLog.i(Messages.get(this, "mobs_effected", count));
		else GLog.i(Messages.get(this, "no_targets"));
	}
}
