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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.treasure;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.VaultSentry;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

import java.util.ArrayList;

public class VaultManyScansRoom extends VaultTreasureRoom {

	@Override
	public void paint(Level level) {
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );

		ArrayList<Integer> corners = new ArrayList<>();
		int w = level.width();
		Point c = center();
		corners.add(left+1  + w*(top+1));
		corners.add(c.x     + w*(top+1));
		corners.add(right-1 + w*(top+1));
		corners.add(left+1  + w*(c.y));
		//skip center
		corners.add(right-1 + w*(c.y));
		corners.add(left+1  + w*(bottom-1));
		corners.add(c.x     +  w*(bottom-1));
		corners.add(right-1 +  w*(bottom-1));

		Door entrance = entrance();
		entrance.set( Door.Type.REGULAR );

		for (int cell : corners){
			if (level.trueDistance(cell, level.pointToCell(entrance)) >= 2){
				VaultSentry sentry = new VaultSentry();
				sentry.pos = cell;
				sentry.scanLength = 7;
				sentry.scanWidth = 70;
				sentry.scanDirs = new int[][]{
						new int[]{c.x + w*c.y}
				};
				level.mobs.add(sentry);
			}
		}

		Painter.set(level, c, Terrain.PEDESTAL);
		Item treasureItem = Generator.randomUsingDefaults(Generator.Category.WEP_T5);
		if (treasureItem.cursed){
			treasureItem.cursed = false;
			if (((MeleeWeapon) treasureItem).hasCurseEnchant()){
				((MeleeWeapon) treasureItem).enchant(null);
			}
		}
		//not true ID
		treasureItem.levelKnown = treasureItem.cursedKnown = true;
		level.drop(treasureItem, c.x + w*c.y).type = Heap.Type.CHEST;

		level.addItemToSpawn(new PotionOfInvisibility());

	}

}
