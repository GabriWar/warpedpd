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

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrapMechanism;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

public class SecretSummoningRoom extends SecretRoom {
	
	//minimum of 3x3 traps, max of 6x6 traps
	
	@Override
	public int maxWidth() {
		return 8;
	}
	
	@Override
	public int maxHeight() {
		return 8;
	}
	
	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.SECRET_TRAP);
		
		Point center = center();
		//living-world twist: the circle draws on the moon it was found under.
		//A full moon empowers it (guaranteed ring or artifact); a new moon
		//leaves only common spoils. Other phases keep the default random roll.
		Item loot;
		if (GameCalendar.isFullMoon()){
			loot = Generator.random(Random.oneOf(Generator.Category.RING, Generator.Category.ARTIFACT));
		} else {
			loot = Generator.random();
		}
		level.drop(loot, level.pointToCell(center)).setHauntedIfCursed().type = Heap.Type.SKELETON;

		float revealedChance = TrapMechanism.revealHiddenTrapChance();
		float revealInc = 0;
		for (Point p : getPoints()){
			int cell = level.pointToCell(p);
			if (level.map[cell] == Terrain.SECRET_TRAP){
				revealInc += revealedChance;
				if (revealInc >= 1) {
					level.setTrap(new SummoningTrap().reveal(), cell);
					Painter.set(level, cell, Terrain.TRAP);
					revealInc--;
				} else {
					level.setTrap(new SummoningTrap().hide(), cell);
				}
			}
		}
		
		entrance().set(Door.Type.HIDDEN);
	}
	
}
