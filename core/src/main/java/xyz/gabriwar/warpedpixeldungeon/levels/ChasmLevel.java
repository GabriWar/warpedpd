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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahTranscend;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;

public class ChasmLevel extends Level {

	private static final int SIZE = 48;

	{
		color1 = 0x48763c;
		color2 = 0x59994a;
	}

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

		// Fill everything with wall
		for (int i = 0; i < length(); i++) {
			map[i] = Terrain.WALL;
		}

		// Generate random rooms to simulate RegularLevel room generation
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
		// Connect last to first for loop
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

		// Apply water patches (Sprouted: 0.45f fill, 5 smoothness)
		boolean[] water = Patch.generate(width(), height(), 0.45f, 5, true);
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && water[i]) {
				map[i] = Terrain.WATER;
			}
		}

		// Apply grass patches (Sprouted: 0.40f fill, 4 smoothness)
		boolean[] grass = Patch.generate(width(), height(), 0.40f, 4, true);
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && grass[i]) {
				map[i] = Terrain.GRASS;
			}
		}

		// --- Decoration (ported from Sprouted ChasmLevel.decorate()) ---

		// Wall deco near water at top
		for (int i = 0; i < width(); i++) {
			if (map[i] == Terrain.WALL && i + width() < length()
					&& map[i + width()] == Terrain.WATER
					&& Random.Int(4) == 0) {
				map[i] = Terrain.WALL_DECO;
			}
		}

		// Wall deco where wall above and water below
		for (int i = width(); i < length() - width(); i++) {
			if (map[i] == Terrain.WALL && map[i - width()] == Terrain.WALL
					&& map[i + width()] == Terrain.WATER && Random.Int(2) == 0) {
				map[i] = Terrain.WALL_DECO;
			}
		}

		// Empty deco near walls
		for (int i = width() + 1; i < length() - width() - 1; i++) {
			if (map[i] == Terrain.EMPTY) {
				int count = (map[i + 1] == Terrain.WALL ? 1 : 0)
						+ (map[i - 1] == Terrain.WALL ? 1 : 0)
						+ (map[i + width()] == Terrain.WALL ? 1 : 0)
						+ (map[i - width()] == Terrain.WALL ? 1 : 0);
				if (Random.Int(16) < count * count) {
					map[i] = Terrain.EMPTY_DECO;
				}
			}
		}

		// Place entrance (Sprouted replaces ENTRANCE with EMPTY in decorate)
		Rect entranceRoom = rooms.get(rooms.size() - 1);
		int entrancePos = (entranceRoom.left + entranceRoom.right) / 2
				+ ((entranceRoom.top + entranceRoom.bottom) / 2) * width();
		while (map[entrancePos] == Terrain.WALL) {
			entrancePos = entranceRoom.left + 1 + Random.Int(Math.max(1, entranceRoom.width() - 2))
					+ (entranceRoom.top + 1 + Random.Int(Math.max(1, entranceRoom.height() - 2))) * width();
		}
		// Sprouted: teleport-only level, no stairs back (entrance tile = EMPTY, not ENTRANCE)
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));
		map[entrancePos] = Terrain.EMPTY;

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

		// Sprouted: EMPTY_SP tiles become CHASM 25% of the time (chasm feeling)
		// Since we don't have CHASM feeling, convert some EMPTY tiles to CHASM
		// to give the level its characteristic chasm-filled nature
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && Random.Float() < 0.08f) {
				// Don't place chasms adjacent to entrance/exit
				if (i != entrancePos && i != exitPos) {
					map[i] = Terrain.CHASM;
				}
			}
		}

		// Drop SanChikarahTranscend on exit pedestal (if not already obtained)
		if (!Dungeon.sanchikarahtranscend) {
			drop(new SanChikarahTranscend(), exitPos);
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

		//Sprouted seeds the chasms with Sentinel guardians (5% of open floor; it
		//used EMPTY_SP tiles, which this level doesn't paint, so EMPTY stands in)
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && heaps.get(i) == null
					&& findMob(i) == null && Random.Float() < 0.05f) {
				Sentinel sentinel = new Sentinel();
				sentinel.pos = i;
				mobs.add(sentinel);
			}
		}
	}

	@Override
	protected void createItems() {
		scatterCaveLoot();

		// Sprouted drops a PotionOfLevitation near the entrance
		LevelTransition entrance = getTransition(null);
		if (entrance != null) {
			int pos = entrance.cell();
			for (int i = 0; i < length(); i++) {
				if (map[i] == Terrain.EMPTY && heaps.get(i) == null
						&& Math.abs(i % width() - pos % width()) < 5
						&& Math.abs(i / width() - pos / width()) < 5) {
					drop(new PotionOfLevitation(), i);
					break;
				}
			}
		}

		//Sprouted scatters phaseshift seeds across the chasm floor (2%)
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && heaps.get(i) == null
					&& Random.Float() < 0.02f) {
				drop(new Phaseshift.Seed(), i);
			}
		}
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return "Murky water";
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
				return "Wet yellowish moss covers the floor.";
			case Terrain.BOOKSHELF:
				return "The bookshelf is packed with cheap useless books. Might it burn?";
			default:
				return super.tileDesc( tile );
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		SewerLevel.addSewerVisuals( this, visuals );
		return visuals;
	}

}
