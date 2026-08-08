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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer3;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.items.Rice;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahLife;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;

public class FortressLevel extends Level {

	private static final int SIZE = 48;

	{
		color1 = 0x4b6636;
		color2 = 0xf2f2f2;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CITY;
	}

	@Override
	protected boolean build() {

		setSize(SIZE, SIZE);

		// Fill everything with wall
		for (int i = 0; i < length(); i++) {
			map[i] = Terrain.WALL;
		}

		// Generate random rooms to simulate RegularLevel room generation
		// Fortress has special rooms and standard rooms, city-themed
		ArrayList<Rect> rooms = new ArrayList<>();
		int roomCount = Random.IntRange(14, 20);

		for (int i = 0; i < roomCount; i++) {
			int rw = Random.IntRange(4, 10);
			int rh = Random.IntRange(4, 10);
			int rx = Random.IntRange(2, SIZE - rw - 2);
			int ry = Random.IntRange(2, SIZE - rh - 2);
			rooms.add(new Rect(rx, ry, rx + rw, ry + rh));
			Painter.fill(this, rx, ry, rw, rh, Terrain.EMPTY);
		}

		// Connect rooms with tunnels (passage-style, as Sprouted converts tunnels to passages)
		for (int i = 1; i < rooms.size(); i++) {
			Rect from = rooms.get(i - 1);
			Rect to = rooms.get(i);
			int fx = (from.left + from.right) / 2;
			int fy = (from.top + from.bottom) / 2;
			int tx = (to.left + to.right) / 2;
			int ty = (to.top + to.bottom) / 2;

			// Wider corridors for passage feel (width 2)
			if (Random.Int(2) == 0) {
				Painter.fill(this, Math.min(fx, tx), fy, Math.abs(tx - fx) + 1, 2, Terrain.EMPTY);
				Painter.fill(this, tx, Math.min(fy, ty), 2, Math.abs(ty - fy) + 1, Terrain.EMPTY);
			} else {
				Painter.fill(this, fx, Math.min(fy, ty), 2, Math.abs(ty - fy) + 1, Terrain.EMPTY);
				Painter.fill(this, Math.min(fx, tx), ty, Math.abs(tx - fx) + 1, 2, Terrain.EMPTY);
			}
		}
		// Connect last to first
		{
			Rect from = rooms.get(rooms.size() - 1);
			Rect to = rooms.get(0);
			int fx = (from.left + from.right) / 2;
			int fy = (from.top + from.bottom) / 2;
			int tx = (to.left + to.right) / 2;
			int ty = (to.top + to.bottom) / 2;
			Painter.fill(this, Math.min(fx, tx), fy, Math.abs(tx - fx) + 1, 2, Terrain.EMPTY);
			Painter.fill(this, tx, Math.min(fy, ty), 2, Math.abs(ty - fy) + 1, Terrain.EMPTY);
		}

		// Apply water patches (Sprouted: 0.45f fill, 4 smoothness)
		boolean[] water = Patch.generate(width(), height(), 0.45f, 4, true);
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && water[i]) {
				map[i] = Terrain.WATER;
			}
		}

		// Apply grass patches (Sprouted: 0.40f fill, 3 smoothness)
		boolean[] grass = Patch.generate(width(), height(), 0.40f, 3, true);
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && grass[i]) {
				map[i] = Terrain.GRASS;
			}
		}

		// --- Decoration (ported from Sprouted FortressLevel.decorate()) ---

		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && Random.Int(10) == 0) {
				map[i] = Terrain.EMPTY_DECO;
			} else if (map[i] == Terrain.WALL && Random.Int(8) == 0) {
				map[i] = Terrain.WALL_DECO;
			}
		}

		// Place entrance as PEDESTAL (Sprouted replaces ENTRANCE with PEDESTAL in decorate)
		Rect entranceRoom = rooms.get(rooms.size() - 1);
		int entrancePos = (entranceRoom.left + entranceRoom.right) / 2
				+ ((entranceRoom.top + entranceRoom.bottom) / 2) * width();
		while (map[entrancePos] == Terrain.WALL) {
			entrancePos = entranceRoom.left + 1 + Random.Int(Math.max(1, entranceRoom.width() - 2))
					+ (entranceRoom.top + 1 + Random.Int(Math.max(1, entranceRoom.height() - 2))) * width();
		}
		// Sprouted: teleport-only level, no stairs back
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));
		map[entrancePos] = Terrain.PEDESTAL;

		// Place exit as PEDESTAL (Sprouted replaces EXIT with PEDESTAL in decorate)
		Rect exitRoom = rooms.get(0);
		int exitPos = (exitRoom.left + exitRoom.right) / 2
				+ ((exitRoom.top + exitRoom.bottom) / 2) * width();
		while (map[exitPos] == Terrain.WALL || exitPos == entrancePos) {
			exitPos = exitRoom.left + 1 + Random.Int(Math.max(1, exitRoom.width() - 2))
					+ (exitRoom.top + 1 + Random.Int(Math.max(1, exitRoom.height() - 2))) * width();
		}
		// Sprouted: exit is just a pedestal with item, not functional stairs.
		// Registered as a transition anyway so that Level.exit() resolves to it: the
		// Book of Life/Dead/Transcendence only lets you read - and leave - while standing
		// on exit(), and without this it falls back to the entrance, so you could read the
		// book the instant you land and skip the floor. It never acts as stairs; Hero.handle
		// refuses to activate a REGULAR_EXIT this deep.
		map[exitPos] = Terrain.PEDESTAL;
		transitions.add(new LevelTransition(this, exitPos, LevelTransition.Type.REGULAR_EXIT));

		// Sprouted: replace any CHASM with EMPTY
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.CHASM) {
				map[i] = Terrain.EMPTY;
			}
		}

		// Drop SanChikarahLife on exit pedestal (if not already obtained)
		if (!Dungeon.sanchikarahlife) {
			drop(new SanChikarahLife(), exitPos);
		}

		return true;
	}

	@Override
	public int mobLimit() {
		return 10;
	}

	@Override
	protected void createMobs() {
		for (int i = 0; i < mobLimit(); i++) {
			Mob mob = createMob();
			if (mob == null) continue;
			do {
				mob.pos = randomRespawnCell(mob);
			} while (mob.pos == -1);
			mobs.add(mob);
		}
	}

	@Override
	protected void createItems() {
		scatterCaveLoot();

		// Spawn Tinkerer3 NPC
		Tinkerer3 npc = new Tinkerer3();
		do {
			npc.pos = randomRespawnCell(npc);
		} while (npc.pos == -1);
		mobs.add(npc);

		// Sprouted added a Mushroom for the Tinkerer quest. This level never flushes
		// itemsToSpawn, so addItemToSpawn was dead — drop it directly instead.
		int mushPos;
		int mushTries = 30;
		do { mushPos = randomRespawnCell(null); } while (mushPos == -1 && mushTries-- > 0);
		if (mushPos != -1) drop(new Mushroom(), mushPos);

		// Drop Rice near entrance
		LevelTransition entrance = getTransition(null);
		if (entrance != null) {
			int ricePos = entrance.cell();
			// Find a nearby passable cell
			for (int i = 0; i < length(); i++) {
				if (map[i] == Terrain.EMPTY && heaps.get(i) == null
						&& Math.abs(i % width() - ricePos % width()) < 5
						&& Math.abs(i / width() - ricePos / width()) < 5) {
					drop(new Rice(), i);
					break;
				}
			}
		}
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return "Suspiciously colored water";
			case Terrain.HIGH_GRASS:
				return "High blooming flowers";
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return "A ramp leads up to the upper depth.";
			case Terrain.EXIT:
				return "A ramp leads down to the lower depth.";
			case Terrain.WALL_DECO:
			case Terrain.EMPTY_DECO:
				return "Several tiles are missing here.";
			case Terrain.EMPTY_SP:
				return "Thick carpet covers the floor.";
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return "The statue depicts some dwarf standing in a heroic stance.";
			case Terrain.BOOKSHELF:
				return "The rows of books on different disciplines fill the bookshelf.";
			default:
				return super.tileDesc( tile );
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		CityLevel.addCityVisuals( this, visuals );
		return visuals;
	}

}
