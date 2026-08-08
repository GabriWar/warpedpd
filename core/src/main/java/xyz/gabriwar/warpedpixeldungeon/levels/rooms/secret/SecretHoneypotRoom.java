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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bee;
import xyz.gabriwar.warpedpixeldungeon.items.Honeypot;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.SteelHoneypot;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

public class SecretHoneypotRoom extends SecretRoom {

	@Override
	public void paint(Level level) {
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill(level, this, 1, Terrain.EMPTY );

		Point brokenPotPos = center();

		brokenPotPos.x = (brokenPotPos.x + entrance().x) / 2;
		brokenPotPos.y = (brokenPotPos.y + entrance().y) / 2;

		Honeypot.ShatteredPot pot = new Honeypot.ShatteredPot();
		level.drop(pot, level.pointToCell(brokenPotPos));

		//living-world twist: the hive follows the seasons
		GameCalendar.Season season = GameCalendar.season();

		//in winter the guard bee is dormant — the hive can be robbed freely
		if (season != GameCalendar.Season.WINTER) {
			Bee bee = new Bee();
			bee.spawn( Dungeon.depth );
			bee.HP = bee.HT;
			bee.pos = level.pointToCell(brokenPotPos);
			level.mobs.add( bee );

			bee.setPotInfo(level.pointToCell(brokenPotPos), null);
		}

		placeItem(new Honeypot(), level);

		placeItem( new Bomb().random(), level);

		//summer: the hive is thriving — an extra pot
		if (season == GameCalendar.Season.SUMMER) {
			placeItem(new Honeypot(), level);
		}
		//winter: a frozen-preserved steel pot where the lost guard would be
		if (season == GameCalendar.Season.WINTER) {
			placeItem(new SteelHoneypot(), level);
		}

		entrance().set(Door.Type.HIDDEN);
	}
	
	private void placeItem(Item item, Level level){
		int itemPos;
		do {
			itemPos = level.pointToCell(random());
		} while (level.heaps.get(itemPos) != null);
		
		level.drop(item, itemPos);
	}
}
