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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RatKingDen;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Egg;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.SeekingClusterBombItem;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift;
import xyz.gabriwar.warpedpixeldungeon.plants.Starflower;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;

// Ported from Sprouted's TenguDenLevel (originally extends RegularLevel).
// Boss-style mini-maze level with a loop of 4 rooms + entrance + a "king's room".
// Entrance also doubles as exit room. Exit is blocked with WALL until unseal.
public class TenguDenLevel extends Level {

	private static final int SIZE = 32;

	private int stairs = 0;

	// Track entrance room bounds for decoration and seal/unseal
	private int entrRoomLeft, entrRoomTop, entrRoomW, entrRoomH;
	// Track king's room bounds for item placement
	private int kingRoomLeft, kingRoomTop, kingRoomRight, kingRoomBottom;
	// Track standard room centers for mob placement
	private ArrayList<int[]> standardRoomCenters = new ArrayList<>();

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

		// Sprouted's TenguDenLevel creates a boss mini-maze:
		// - An entrance room (also exit room) near the top half
		// - 4 standard rooms connected in a loop around the entrance
		// - A "king's room" off the last standard room

		// Create entrance room in the upper-middle area
		entrRoomLeft = SIZE / 2 - Random.IntRange(2, 4);
		entrRoomTop = Random.IntRange(2, 6);
		entrRoomW = Random.IntRange(5, 8);
		entrRoomH = Random.IntRange(5, 8);
		// Clamp to map bounds
		if (entrRoomLeft + entrRoomW >= SIZE - 1) entrRoomW = SIZE - 2 - entrRoomLeft;
		if (entrRoomTop + entrRoomH >= SIZE / 2) entrRoomH = SIZE / 2 - entrRoomTop;
		Painter.fill(this, entrRoomLeft, entrRoomTop, entrRoomW, entrRoomH, Terrain.EMPTY);

		Rect entranceRect = new Rect(entrRoomLeft, entrRoomTop,
				entrRoomLeft + entrRoomW, entrRoomTop + entrRoomH);

		// Create 4 standard rooms around the map forming a loop
		ArrayList<Rect> standardRooms = new ArrayList<>();
		standardRoomCenters.clear();

		// Room positions: bottom-left, bottom-right, right, left (forming a ring)
		int[][] roomPositions = {
			{ Random.IntRange(2, 6), Random.IntRange(SIZE / 2, SIZE / 2 + 4) },
			{ Random.IntRange(SIZE / 2 - 2, SIZE / 2 + 2), Random.IntRange(SIZE / 2, SIZE / 2 + 4) },
			{ Random.IntRange(SIZE / 2 + 2, SIZE - 10), Random.IntRange(SIZE / 4, SIZE / 2 - 2) },
			{ Random.IntRange(2, 6), Random.IntRange(SIZE / 4, SIZE / 2 - 2) },
		};

		for (int[] pos : roomPositions) {
			int rw = Random.IntRange(5, 8);
			int rh = Random.IntRange(5, 8);
			int rx = Math.max(1, Math.min(pos[0], SIZE - rw - 2));
			int ry = Math.max(1, Math.min(pos[1], SIZE - rh - 2));
			Rect room = new Rect(rx, ry, rx + rw, ry + rh);
			standardRooms.add(room);
			standardRoomCenters.add(new int[]{ (rx + rx + rw) / 2, (ry + ry + rh) / 2 });
			Painter.fill(this, rx, ry, rw, rh, Terrain.EMPTY);
		}

		// Create "king's room" - adjacent to the last standard room (Sprouted: RAT_KING2)
		Rect lastRoom = standardRooms.get(standardRooms.size() - 1);
		int kingX = lastRoom.right + 1;
		int kingY = lastRoom.top;
		int kingW = Random.IntRange(4, 6);
		int kingH = Random.IntRange(4, 6);
		if (kingX + kingW >= SIZE - 1) {
			kingX = lastRoom.left - kingW - 1;
		}
		if (kingX < 1) kingX = 1;
		if (kingY + kingH >= SIZE - 1) kingH = SIZE - 2 - kingY;

		kingRoomLeft = kingX;
		kingRoomTop = kingY;
		kingRoomRight = kingX + kingW;
		kingRoomBottom = kingY + kingH;

		// Fill king's room with EMPTY_SP (Sprouted: RatKingPainter2 fills with EMPTY_SP)
		Painter.fill(this, kingX, kingY, kingW, kingH, Terrain.WALL);
		Painter.fill(this, kingX + 1, kingY + 1, kingW - 2, kingH - 2, Terrain.EMPTY_SP);

		// Connect king's room to last standard room with a hidden-door-width corridor
		int connY = (lastRoom.top + lastRoom.bottom) / 2;
		int connMinX = Math.min(lastRoom.right - 1, kingX);
		int connMaxX = Math.max(lastRoom.right - 1, kingX);
		Painter.fill(this, connMinX, connY, connMaxX - connMinX + 2, 1, Terrain.EMPTY);
		// Place a secret door at the connection point (Sprouted: entrance.set(Door.Type.HIDDEN))
		int hiddenDoorPos;
		if (kingX > lastRoom.right) {
			hiddenDoorPos = connY * width() + kingX;
		} else {
			hiddenDoorPos = connY * width() + kingX + kingW - 1;
		}
		if (hiddenDoorPos >= 0 && hiddenDoorPos < length()) {
			map[hiddenDoorPos] = Terrain.SECRET_DOOR;
		}

		// Connect rooms in a loop: entrance -> room0 -> room1 -> room2 -> room3 -> entrance
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

			// L-shaped corridor
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

		// --- Decoration (ported from Sprouted TenguDenLevel.decorate()) ---
		// Decorate top of entrance room with WALL_DECO and WATER
		int exitPos = entrRoomTop * width() + (entrRoomLeft + entrRoomLeft + entrRoomW) / 2;
		int decoStart = entrRoomTop * width() + entrRoomLeft + 1;
		int decoEnd = decoStart + entrRoomW - 1;
		for (int i = decoStart; i < decoEnd; i++) {
			if (i >= 0 && i < length() && i + width() < length()) {
				if (i != exitPos) {
					map[i] = Terrain.WALL_DECO;
					map[i + width()] = Terrain.WATER;
				} else {
					map[i + width()] = Terrain.EMPTY;
				}
			}
		}

		// Exit position at top of entrance room — blocked with WALL (Sprouted: map[exit] = Terrain.WALL)
		if (exitPos >= 0 && exitPos < length()) {
			map[exitPos] = Terrain.WALL;
		}

		// Place entrance
		int entrancePos = (entrRoomLeft + entrRoomW / 2)
				+ (entrRoomTop + entrRoomH / 2) * width();
		// Ensure entrance is on a passable tile
		int tries = 0;
		while ((map[entrancePos] == Terrain.WALL || map[entrancePos] == Terrain.WALL_DECO) && tries++ < 100) {
			entrancePos = entrRoomLeft + 1 + Random.Int(Math.max(1, entrRoomW - 2))
					+ (entrRoomTop + 1 + Random.Int(Math.max(1, entrRoomH - 2))) * width();
		}
		// Sprouted: teleport-only level, no stairs back
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));
		map[entrancePos] = Terrain.PEDESTAL;

		// Place king's room contents (Sprouted: RatKingPainter2)
		paintKingsRoom();

		return true;
	}

	private void paintKingsRoom() {
		// Place chests around the perimeter of the king's room (Sprouted: RatKingPainter2)
		int doorPos = -1;
		// Find the secret door position
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.SECRET_DOOR) {
				doorPos = i;
				break;
			}
		}

		for (int x = kingRoomLeft + 1; x < kingRoomRight - 1; x++) {
			addKingsChest((kingRoomTop + 1) * width() + x, doorPos);
			addKingsChest((kingRoomBottom - 2) * width() + x, doorPos);
		}
		for (int y = kingRoomTop + 2; y < kingRoomBottom - 2; y++) {
			addKingsChest(y * width() + kingRoomLeft + 1, doorPos);
			addKingsChest(y * width() + kingRoomRight - 2, doorPos);
		}

		// Turn one random chest into a mimic
		ArrayList<Integer> chestPositions = new ArrayList<>();
		for (int i = 0; i < length(); i++) {
			Heap h = heaps.get(i);
			if (h != null && h.type == Heap.Type.CHEST) {
				chestPositions.add(i);
			}
		}
		if (!chestPositions.isEmpty()) {
			int mimicPos = Random.element(chestPositions);
			Heap h = heaps.get(mimicPos);
			if (h != null && !h.items.isEmpty()) {
				//Heap.pickUp() routes through Dungeon.level, which is null
				//during level creation - take the item out directly
				Item mimicItem = h.items.removeFirst();
				heaps.remove(mimicPos);
				Mimic.spawnAt(mimicPos, mimicItem);
			}
		}

		// Place RatKingDen NPC
		RatKingDen king = new RatKingDen();
		int kingPos;
		int tries = 0;
		do {
			kingPos = Random.IntRange(kingRoomLeft + 2, Math.max(kingRoomLeft + 2, kingRoomRight - 3))
					+ Random.IntRange(kingRoomTop + 2, Math.max(kingRoomTop + 2, kingRoomBottom - 3)) * width();
			tries++;
		} while ((kingPos < 0 || kingPos >= length() || map[kingPos] != Terrain.EMPTY_SP || heaps.get(kingPos) != null) && tries < 100);
		if (kingPos >= 0 && kingPos < length()) {
			king.pos = kingPos;
			mobs.add(king);
		}
	}

	private void addKingsChest(int pos, int doorPos) {
		if (pos < 0 || pos >= length()) return;

		// Don't place chests adjacent to the door (Sprouted: RatKingPainter2)
		if (doorPos != -1) {
			if (pos == doorPos - 1 || pos == doorPos + 1
					|| pos == doorPos - width() || pos == doorPos + width()) {
				return;
			}
		}

		Item prize;
		switch (Random.Int(10)) {
		case 0:
			prize = new Egg();
			break;
		case 1:
			prize = new Phaseshift.Seed();
			break;
		case 2:
			prize = Generator.randomUsingDefaults(Generator.Category.FOOD);
			break;
		case 3:
			prize = new Starflower.Seed();
			break;
		case 5:
			prize = new ActiveMrDestructo();
			break;
		case 6:
			prize = new SeekingClusterBombItem();
			break;
		default:
			prize = new Gold(Random.IntRange(1, 5));
			break;
		}

		drop(prize, pos).type = Heap.Type.CHEST;
	}

	@Override
	protected void createMobs() {
		// Sprouted: spawn one depth-appropriate mob in a standard room, unless already killed
		if (Dungeon.tengudenkilled) {
			return;
		}
		if (!standardRoomCenters.isEmpty()) {
			//the merged Tengu serves as the den boss (its damage()/die()/drops are
			//level-aware); it sets tengudenkilled on death, unlocking the TenguKey.
			Tengu mob = new Tengu();
			mob.makeDenBoss();
			mob.state = mob.HUNTING;
			int[] center = Random.element(standardRoomCenters);
			mob.pos = center[0] + center[1] * width();
			if (mob.pos >= 0 && mob.pos < length() && passable[mob.pos]) {
				mobs.add(mob);
			}
		}
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				int pos;
				int tries = 0;
				do {
					pos = entrRoomLeft + 1 + Random.Int(Math.max(1, entrRoomW - 2))
							+ (entrRoomTop + 1 + Random.Int(Math.max(1, entrRoomH - 2))) * width();
					tries++;
				} while ((pos == entrance() || map[pos] == Terrain.WALL) && tries < 100);
				for (Item i : bonesItems) {
					drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return -1;
	}

	// --- Seal/unseal logic ported from Sprouted ---

	public void seal() {
		if (!locked) {

			super.seal();

			int entrancePos = entrance();
			if (entrancePos != 0) {
				set(entrancePos, Terrain.WATER);
				GameScene.updateMap(entrancePos);
				GameScene.ripple(entrancePos);

				stairs = entrancePos;
			}
		}
	}

	public void unseal() {
		if (locked) {

			super.unseal();

			if (stairs != 0) {
				set(stairs, Terrain.ENTRANCE);
				GameScene.updateMap(stairs);
				stairs = 0;
			}
		}
	}

	private static final String STAIRS       = "stairs";
	private static final String ENTR_LEFT    = "entrRoomLeft";
	private static final String ENTR_TOP     = "entrRoomTop";
	private static final String ENTR_W       = "entrRoomW";
	private static final String ENTR_H       = "entrRoomH";
	private static final String KING_LEFT    = "kingRoomLeft";
	private static final String KING_TOP     = "kingRoomTop";
	private static final String KING_RIGHT   = "kingRoomRight";
	private static final String KING_BOTTOM  = "kingRoomBottom";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STAIRS, stairs);
		bundle.put(ENTR_LEFT, entrRoomLeft);
		bundle.put(ENTR_TOP, entrRoomTop);
		bundle.put(ENTR_W, entrRoomW);
		bundle.put(ENTR_H, entrRoomH);
		bundle.put(KING_LEFT, kingRoomLeft);
		bundle.put(KING_TOP, kingRoomTop);
		bundle.put(KING_RIGHT, kingRoomRight);
		bundle.put(KING_BOTTOM, kingRoomBottom);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stairs = bundle.getInt(STAIRS);
		entrRoomLeft = bundle.getInt(ENTR_LEFT);
		entrRoomTop = bundle.getInt(ENTR_TOP);
		entrRoomW = bundle.getInt(ENTR_W);
		entrRoomH = bundle.getInt(ENTR_H);
		kingRoomLeft = bundle.getInt(KING_LEFT);
		kingRoomTop = bundle.getInt(KING_TOP);
		kingRoomRight = bundle.getInt(KING_RIGHT);
		kingRoomBottom = bundle.getInt(KING_BOTTOM);
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		SewerLevel.addSewerVisuals(this, visuals);
		return visuals;
	}

	@Override
	public String tileName(int tile) {
		switch (tile) {
		case Terrain.WATER:
			return "Murky water";
		default:
			return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
		case Terrain.EMPTY_DECO:
			return "Wet yellowish moss covers the floor.";
		default:
			return super.tileDesc(tile);
		}
	}
}
