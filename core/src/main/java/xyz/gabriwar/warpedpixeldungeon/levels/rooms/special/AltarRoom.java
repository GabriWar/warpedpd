/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2024-2026 Gabriwar
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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.special;

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.AltarShrine;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

/**
 * A shrine room containing a single artifact resting on a sacred pedestal.
 * The interior is filled with a chasm (or overgrown grass before boss floors),
 * with a narrow stone path leading from the entrance to the altar.
 * Ported and expanded from Overgrown Pixel Dungeon.
 */
public class AltarRoom extends SpecialRoom {

	@Override
	public int minWidth()  { return 7; }
	@Override
	public int minHeight() { return 7; }

	@Override
	public void paint(Level level) {

		Painter.fill(level, this, Terrain.WALL);
		// Before a boss floor: let grass reclaim the shrine; otherwise a chasm surrounds the altar
		Painter.fill(level, this, 1, Dungeon.bossLevel(Dungeon.depth + 1) ? Terrain.HIGH_GRASS : Terrain.CHASM);

		Point c = center();
		Door door = entrance();

		// Draw the approach path from the entrance to the pedestal
		if (door.x == left || door.x == right) {
			Point p = Painter.drawInside(level, this, door, Math.abs(door.x - c.x) - 2, Terrain.EMPTY_SP);
			for (; p.y != c.y; p.y += p.y < c.y ? +1 : -1) {
				Painter.set(level, p, Terrain.EMPTY_SP);
			}
		} else {
			Point p = Painter.drawInside(level, this, door, Math.abs(door.y - c.y) - 2, Terrain.EMPTY_SP);
			for (; p.x != c.x; p.x += p.x < c.x ? +1 : -1) {
				Painter.set(level, p, Terrain.EMPTY_SP);
			}
		}

		// Central altar: ring of embers with the pedestal at the heart
		Painter.fill(level, c.x - 1, c.y - 1, 3, 3, Terrain.EMBERS);
		Painter.set(level, c, Terrain.PEDESTAL);

		level.drop(prize(level), level.pointToCell(c));

		// Seed the shrine marker blob so the journal registers this landmark
		Blob.seed(level.pointToCell(c), 1, AltarShrine.class, level);

		door.set(Door.Type.EMPTY);
	}

	private static Item prize(Level level) {
		//living-world twist: seven weekday gods, seven gifts. The altar's offering
		//reflects whoever holds the day it was found. Light's day yields the artifact
		//(the richest prize); every other day answers with its own domain.
		Item prize;
		switch (GameCalendar.weekday()) {
			case STONESDAY:  prize = Generator.random(Generator.Category.ARMOR);  break;
			case FORGEDAY:   prize = Generator.random(Generator.Category.WEAPON); break;
			case TIDEDAY:    prize = Generator.random(Generator.Category.POTION); break;
			case STORMDAY:   prize = Generator.random(Generator.Category.WAND);   break;
			case SHADOWDAY:  prize = Generator.random(Generator.Category.RING);   break;
			case HARVESTDAY: prize = Generator.random(Generator.Category.SEED);   break;
			case LIGHTDAY:
			default:         prize = Generator.randomArtifact();                  break;
		}
		if (prize == null || Challenges.isItemBlocked(prize)) {
			prize = new Gold().random();
		}
		return prize;
	}
}
