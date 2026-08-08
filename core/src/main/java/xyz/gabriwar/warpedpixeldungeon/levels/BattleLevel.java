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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.HolyHandGrenade;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;

public class BattleLevel extends Level {

	private static final int SIZE = 48;

	{
		color1 = 0x6a723d;
		color2 = 0x88924c;
		viewDistance = 8;
	}

	private static final int ROOM_LEFT   = SIZE / 2 - 2;
	private static final int ROOM_RIGHT  = SIZE / 2 + 2;
	private static final int ROOM_TOP    = SIZE / 2 - 2;
	private static final int ROOM_BOTTOM = SIZE / 2 + 2;

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_PRISON;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_PRISON;
	}

	@Override
	protected boolean build() {

		setSize(SIZE, SIZE);

		int topMost = Integer.MAX_VALUE;
		int exitPos = -1;

		for (int i = 0; i < 8; i++) {
			int left, right, top, bottom;
			if (Random.Int(2) == 0) {
				left = Random.Int(1, ROOM_LEFT - 3);
				right = ROOM_RIGHT + 3;
			} else {
				left = ROOM_LEFT - 3;
				right = Random.Int(ROOM_RIGHT + 3, width() - 1);
			}
			if (Random.Int(2) == 0) {
				top = Random.Int(2, ROOM_TOP - 3);
				bottom = ROOM_BOTTOM + 3;
			} else {
				top = ROOM_LEFT - 3;
				bottom = Random.Int(ROOM_TOP + 3, height() - 1);
			}

			Painter.fill(this, left, top, right - left + 1, bottom - top + 1,
					Terrain.EMPTY);

			if (top < topMost) {
				topMost = top;
				exitPos = Random.Int(left, right) + (top - 1) * width();
			}
		}

		if (exitPos != -1) {
			map[exitPos] = Terrain.WALL;
		}

		Painter.fill(this, ROOM_LEFT, ROOM_TOP + 1, ROOM_RIGHT - ROOM_LEFT + 1,
				ROOM_BOTTOM - ROOM_TOP, Terrain.EMPTY);

		int entrancePos = Random.Int(ROOM_LEFT + 1, ROOM_RIGHT - 1)
				+ Random.Int(ROOM_TOP + 1, ROOM_BOTTOM - 1) * width();
		// Sprouted: teleport-only level, no stairs back (entrance tile = EMPTY, not ENTRANCE)
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));
		map[entrancePos] = Terrain.EMPTY;


		// Decoration
		for (int i = width() + 1; i < length() - width(); i++) {
			if (map[i] == Terrain.EMPTY) {
				int n = 0;
				if (map[i + 1] == Terrain.WALL) n++;
				if (map[i - 1] == Terrain.WALL) n++;
				if (map[i + width()] == Terrain.WALL) n++;
				if (map[i - width()] == Terrain.WALL) n++;
				if (Random.Int(8) <= n) {
					map[i] = Terrain.EMPTY_DECO;
				}
			}
		}

		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.WALL && Random.Int(8) == 0) {
				map[i] = Terrain.WALL_DECO;
			}

			if (map[i] == Terrain.EMPTY && heaps.get(i) == null && Random.Float() < .20f) {
				map[i] = Terrain.HIGH_GRASS;
			}
			if (map[i] == Terrain.EMPTY && heaps.get(i) == null && Random.Float() < .25f) {
				map[i] = Terrain.GRASS;
			}
			if (map[i] == Terrain.EMPTY && heaps.get(i) == null && Random.Float() < .30f) {
				map[i] = Terrain.SHRUB;
			}
		}

		return true;
	}

	@Override
	protected void createItems() {
		int pos = randomRespawnCell(null);
		if (pos != -1) {
			HolyHandGrenade grenades = new HolyHandGrenade();
			grenades.quantity(200);
			drop(grenades, pos).type = Heap.Type.CHEST;
		}
	}

	@Override
	public int mobLimit() {
		return 16;
	}

	@Override
	public float respawnCooldown() {
		return 30f;
	}

	@Override
	protected void createMobs() {
		for (int i = 0; i < mobLimit(); i++) {
			Mob mob = createMob();
			if (mob == null) continue;
			do { mob.pos = randomRespawnCell(mob); } while (mob.pos == -1);
			mobs.add(mob);
		}
	}

	@Override
	public String tileName(int tile) {
		switch (tile) {
		case Terrain.WATER:
			return "Dark cold water.";
		default:
			return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
		case Terrain.EMPTY_DECO:
			return "There are old blood stains on the floor.";
		case Terrain.BOOKSHELF:
			return "The bookshelf is packed with cheap useless books. Might it burn?";
		default:
			return super.tileDesc(tile);
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		CavesLevel.addCavesVisuals(this, visuals);
		return visuals;
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		int cell;
		do {
			cell = Random.Int(length());
		} while (!passable[cell] || map[cell] == Terrain.ENTRANCE || Actor.findChar(cell) != null);
		return cell;
	}
}
