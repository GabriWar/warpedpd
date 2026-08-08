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
import xyz.gabriwar.warpedpixeldungeon.Bones;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.StoneOre;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;

public class MineLevel extends Level {

	private static final int SIZE = 32;

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
		//Sprouted set 6 and then divided it by 3 down here: the mines are meant to be dark
		viewDistance = 2;
	}

	@Override
	public String tilesTex() {
		// Sprouted also uses TILES_CAVES
		return Assets.Environment.TILES_CAVES;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CAVES;
	}

	@Override
	protected boolean build() {

		setSize(SIZE, SIZE);

		// Fill everything with wall
		for (int i = 0; i < length(); i++) {
			map[i] = Terrain.WALL;
		}

		// Generate random rooms - cave style with more organic shapes
		ArrayList<Rect> rooms = new ArrayList<>();
		int roomCount = Random.IntRange(10, 16);

		for (int i = 0; i < roomCount; i++) {
			int rw = Random.IntRange(4, 8);
			int rh = Random.IntRange(4, 8);
			int rx = Random.IntRange(2, SIZE - rw - 2);
			int ry = Random.IntRange(2, SIZE - rh - 2);
			Rect room = new Rect(rx, ry, rx + rw, ry + rh);
			rooms.add(room);
			Painter.fill(this, rx, ry, rw, rh, Terrain.EMPTY);
		}

		// Connect rooms with tunnels
		for (int i = 1; i < rooms.size(); i++) {
			Rect from = rooms.get(i - 1);
			Rect to = rooms.get(i);
			int fx = (from.left + from.right) / 2;
			int fy = (from.top + from.bottom) / 2;
			int tx = (to.left + to.right) / 2;
			int ty = (to.top + to.bottom) / 2;

			if (Random.Int(2) == 0) {
				Painter.fill(this, Math.min(fx, tx), fy, Math.abs(tx - fx) + 1, 1, Terrain.EMPTY);
				Painter.fill(this, tx, Math.min(fy, ty), 1, Math.abs(ty - fy) + 1, Terrain.EMPTY);
			} else {
				Painter.fill(this, fx, Math.min(fy, ty), 1, Math.abs(ty - fy) + 1, Terrain.EMPTY);
				Painter.fill(this, Math.min(fx, tx), ty, Math.abs(tx - fx) + 1, 1, Terrain.EMPTY);
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
			Painter.fill(this, Math.min(fx, tx), fy, Math.abs(tx - fx) + 1, 1, Terrain.EMPTY);
			Painter.fill(this, tx, Math.min(fy, ty), 1, Math.abs(ty - fy) + 1, Terrain.EMPTY);
		}

		// Apply water patches (Sprouted: 0.45f fill, 6 smoothness)
		boolean[] water = Patch.generate(width(), height(), 0.45f, 6, true);
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && water[i]) {
				map[i] = Terrain.WATER;
			}
		}

		// Apply grass patches (Sprouted: 0.35f fill, 3 smoothness)
		boolean[] grass = Patch.generate(width(), height(), 0.35f, 3, true);
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && grass[i]) {
				map[i] = Terrain.GRASS;
			}
		}

		// --- Decoration (ported from Sprouted MineLevel.decorate()) ---

		// Corner-filling in larger rooms (Sprouted: adds wall corners in standard rooms)
		for (Rect room : rooms) {
			if (room.width() <= 3 || room.height() <= 3) {
				continue;
			}
			int s = room.width() * room.height();

			if (Random.Int(s) > 8) {
				int corner = (room.left + 1) + (room.top + 1) * width();
				if (corner - 1 >= 0 && corner - width() >= 0
						&& map[corner - 1] == Terrain.WALL
						&& map[corner - width()] == Terrain.WALL) {
					map[corner] = Terrain.WALL;
				}
			}

			if (Random.Int(s) > 8) {
				int corner = (room.right - 1) + (room.top + 1) * width();
				if (corner + 1 < length() && corner - width() >= 0
						&& map[corner + 1] == Terrain.WALL
						&& map[corner - width()] == Terrain.WALL) {
					map[corner] = Terrain.WALL;
				}
			}

			if (Random.Int(s) > 8) {
				int corner = (room.left + 1) + (room.bottom - 1) * width();
				if (corner - 1 >= 0 && corner + width() < length()
						&& map[corner - 1] == Terrain.WALL
						&& map[corner + width()] == Terrain.WALL) {
					map[corner] = Terrain.WALL;
				}
			}

			if (Random.Int(s) > 8) {
				int corner = (room.right - 1) + (room.bottom - 1) * width();
				if (corner + 1 < length() && corner + width() < length()
						&& map[corner + 1] == Terrain.WALL
						&& map[corner + width()] == Terrain.WALL) {
					map[corner] = Terrain.WALL;
				}
			}
		}

		// Empty deco near walls
		for (int i = width() + 1; i < length() - width(); i++) {
			if (map[i] == Terrain.EMPTY) {
				int n = 0;
				if (map[i + 1] == Terrain.WALL) n++;
				if (map[i - 1] == Terrain.WALL) n++;
				if (map[i + width()] == Terrain.WALL) n++;
				if (map[i - width()] == Terrain.WALL) n++;
				if (Random.Int(6) <= n) {
					map[i] = Terrain.EMPTY_DECO;
				}
			}
		}

		// Wall deco (ore veins)
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.WALL && Random.Int(8) == 0) {
				map[i] = Terrain.WALL_DECO;
			}
		}

		// Barricades between adjacent rooms (ported from Sprouted)
		for (int ri = 0; ri < rooms.size(); ri++) {
			Rect r = rooms.get(ri);
			for (int rj = ri + 1; rj < rooms.size(); rj++) {
				Rect n = rooms.get(rj);
				Rect w = r.intersect(n);
				if (w.left == w.right && w.bottom - w.top >= 5) {
					int bTop = w.top + 2;
					int bHeight = w.bottom - w.top - 3;
					if (bHeight > 0) {
						Painter.fill(this, w.left, bTop, 1, bHeight, Terrain.BARRICADE);
					}
				} else if (w.top == w.bottom && w.right - w.left >= 5) {
					int bLeft = w.left + 2;
					int bWidth = w.right - w.left - 3;
					if (bWidth > 0) {
						Painter.fill(this, bLeft, w.top, bWidth, 1, Terrain.BARRICADE);
					}
				}
			}
		}

		// Place entrance
		Rect entranceRoom = rooms.get(rooms.size() - 1);
		int entrancePos = (entranceRoom.left + entranceRoom.right) / 2
				+ ((entranceRoom.top + entranceRoom.bottom) / 2) * width();
		while (map[entrancePos] == Terrain.WALL) {
			entrancePos = entranceRoom.left + 1 + Random.Int(Math.max(1, entranceRoom.width() - 2))
					+ (entranceRoom.top + 1 + Random.Int(Math.max(1, entranceRoom.height() - 2))) * width();
		}
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.REGULAR_ENTRANCE));
		map[entrancePos] = Terrain.ENTRANCE;

		// Place exit
		Rect exitRoom = rooms.get(0);
		int exitPos = (exitRoom.left + exitRoom.right) / 2
				+ ((exitRoom.top + exitRoom.bottom) / 2) * width();
		while (map[exitPos] == Terrain.WALL || exitPos == entrancePos) {
			exitPos = exitRoom.left + 1 + Random.Int(Math.max(1, exitRoom.width() - 2))
					+ (exitRoom.top + 1 + Random.Int(Math.max(1, exitRoom.height() - 2))) * width();
		}
		transitions.add(new LevelTransition(this, exitPos, LevelTransition.Type.REGULAR_EXIT));
		map[exitPos] = Terrain.EXIT;

		return true;
	}

	//the first mine floor's up-ladder surfaces beside the plaza staircase
	//that leads down here (the old separate town level is gone)
	@Override
	public boolean activateTransition( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero,
			LevelTransition transition ) {
		if (Dungeon.depth == 56 && transition.type == LevelTransition.Type.REGULAR_ENTRANCE) {
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.arriveInTown(
					xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures.TOWN_STAIRS - 32 );
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.travelToSurface();
			return true;
		}
		return super.activateTransition( hero, transition );
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

		//Sprouted gave the Rogue a free bomb here
		if (Dungeon.hero.heroClass == HeroClass.ROGUE && Random.Int(3) == 0) {
			drop(new Bomb(), randomDestination(null));
		}

		//the mines are lined with ore - it is the act's whole gold economy
		for (int i = 0; i < 9; i++) {
			drop(new StoneOre(), randomDestination(null));
		}
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.GRASS:
				return "Fluorescent moss";
			case Terrain.HIGH_GRASS:
				return "Fluorescent mushrooms";
			case Terrain.WATER:
				return "Freezing cold water.";
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return "The ladder leads up to the upper depth.";
			case Terrain.EXIT:
				return "The ladder leads down to the lower depth.";
			case Terrain.HIGH_GRASS:
				return "Huge mushrooms block the view.";
			case Terrain.WALL_DECO:
				return "A vein of some ore is visible on the wall. Gold?";
			case Terrain.BOOKSHELF:
				return "Who would need a bookshelf in a cave?";
			default:
				return super.tileDesc( tile );
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		CavesLevel.addCavesVisuals( this, visuals );
		return visuals;
	}

}
