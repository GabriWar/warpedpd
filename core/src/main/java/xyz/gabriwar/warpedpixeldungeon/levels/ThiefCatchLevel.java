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
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;

public class ThiefCatchLevel extends Level {

	private static final int SIZE = 48;

	private int stairs = 0;

	{
		color1 = 0x48763c;
		color2 = 0x59994a;
	}

	@Override
	public String tilesTex() {
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

		// Sprouted's ThiefCatchLevel is nearly identical to TenguDenLevel:
		// A boss mini-maze with entrance room, 4 standard rooms in a loop,
		// and a "king's room" (here just STANDARD type instead of RAT_KING2).

		// Create entrance room in the upper-middle area
		int entrRoomLeft = SIZE / 2 - 4;
		int entrRoomTop = Random.IntRange(4, 10);
		int entrRoomW = Random.IntRange(5, 8);
		int entrRoomH = Random.IntRange(5, 8);
		Painter.fill(this, entrRoomLeft, entrRoomTop, entrRoomW, entrRoomH, Terrain.EMPTY);

		// Create 4 standard rooms around the map forming a loop
		ArrayList<Rect> standardRooms = new ArrayList<>();
		int[][] roomPositions = {
			{ Random.IntRange(4, 10), Random.IntRange(SIZE / 2, SIZE / 2 + 6) },
			{ Random.IntRange(SIZE / 2 - 4, SIZE / 2 + 2), Random.IntRange(SIZE / 2, SIZE / 2 + 6) },
			{ Random.IntRange(SIZE / 2 + 2, SIZE - 14), Random.IntRange(SIZE / 4, SIZE / 2 - 2) },
			{ Random.IntRange(4, 10), Random.IntRange(SIZE / 4, SIZE / 2 - 4) },
		};

		for (int[] pos : roomPositions) {
			int rw = Random.IntRange(5, 8);
			int rh = Random.IntRange(5, 8);
			int rx = Math.min(pos[0], SIZE - rw - 2);
			int ry = Math.min(pos[1], SIZE - rh - 2);
			Rect room = new Rect(rx, ry, rx + rw, ry + rh);
			standardRooms.add(room);
			Painter.fill(this, rx, ry, rw, rh, Terrain.EMPTY);
		}

		// Create extra room adjacent to the last standard room (STANDARD, not RAT_KING2)
		Rect lastRoom = standardRooms.get(standardRooms.size() - 1);
		int extraRoomX = lastRoom.right + 1;
		int extraRoomY = lastRoom.top;
		int extraRoomW = Random.IntRange(4, 6);
		int extraRoomH = Random.IntRange(4, 6);
		if (extraRoomX + extraRoomW >= SIZE - 1) {
			extraRoomX = lastRoom.left - extraRoomW - 1;
		}
		if (extraRoomX < 2) extraRoomX = 2;
		Painter.fill(this, extraRoomX, extraRoomY, extraRoomW, extraRoomH, Terrain.EMPTY);
		// Connect extra room to last standard room
		int connY = (lastRoom.top + lastRoom.bottom) / 2;
		Painter.fill(this, Math.min(lastRoom.right, extraRoomX), connY,
				Math.abs(extraRoomX - lastRoom.right) + 1, 1, Terrain.EMPTY);

		// Connect rooms in a loop: entrance -> room0 -> room1 -> room2 -> room3 -> entrance
		Rect entranceRect = new Rect(entrRoomLeft, entrRoomTop,
				entrRoomLeft + entrRoomW, entrRoomTop + entrRoomH);

		ArrayList<Rect> allRooms = new ArrayList<>();
		allRooms.add(entranceRect);
		allRooms.addAll(standardRooms);
		allRooms.add(entranceRect); // close the loop

		for (int i = 0; i < allRooms.size() - 1; i++) {
			Rect from = allRooms.get(i);
			Rect to = allRooms.get(i + 1);
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

		// Apply water patches (Sprouted: 0.5f fill, 5 smoothness)
		boolean[] water = Patch.generate(width(), height(), 0.5f, 5, true);
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

		// --- Decoration (ported from Sprouted ThiefCatchLevel.decorate()) ---
		// Decorate top of entrance/exit room with WALL_DECO and WATER
		int decoStart = entrRoomTop * width() + entrRoomLeft + 1;
		int decoEnd = decoStart + entrRoomW - 1;
		for (int i = decoStart; i < decoEnd; i++) {
			if (i >= 0 && i < length() && i + width() < length()) {
				map[i] = Terrain.WALL_DECO;
				map[i + width()] = Terrain.WATER;
			}
		}

		// Place entrance
		int entrancePos = (entrRoomLeft + entrRoomW / 2)
				+ (entrRoomTop + entrRoomH / 2) * width();
		while (map[entrancePos] == Terrain.WALL) {
			entrancePos = entrRoomLeft + 1 + Random.Int(Math.max(1, entrRoomW - 2))
					+ (entrRoomTop + 1 + Random.Int(Math.max(1, entrRoomH - 2))) * width();
		}
		// Sprouted: teleport-only level, no stairs back (entrance tile = EMPTY, not ENTRANCE)
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));
		map[entrancePos] = Terrain.EMPTY;

		// Sprouted: exit position at top of entrance room, set to EMPTY_SP (not a real exit)
		int exitPos = entrRoomTop * width() + (entrRoomLeft + entrRoomW / 2);
		if (exitPos >= 0 && exitPos < length()) {
			map[exitPos] = Terrain.EMPTY_SP;
		}

		return true;
	}

	@Override
	protected void createMobs() {
		if (Dungeon.tengudenkilled) {
			return;
		}
		Mob mob = createMob();
		if (mob != null) {
			do {
				mob.pos = randomRespawnCell(mob);
			} while (mob.pos == -1);
			mobs.add(mob);
		}
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
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

	@Override
	public int randomRespawnCell( Char ch ) {
		return entrance() - width();
	}

	// seal() and unseal() are inherited from Level and work correctly.
	// Call seal() when the boss fight starts and unseal() when it ends.

	private static final String STAIRS = "stairs";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STAIRS, stairs);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stairs = bundle.getInt(STAIRS);
	}
}
