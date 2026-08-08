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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BanditKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ThiefKing;
import xyz.gabriwar.warpedpixeldungeon.Bones;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class ThiefBossLevel extends Level {

	private static final int SIZE = 48;

	private static final int TOP = 2;
	private static final int HALL_WIDTH = 13;
	private static final int HALL_HEIGHT = 15;
	private static final int CHAMBER_HEIGHT = 3;

	{
		color1 = 0x4b6636;
		color2 = 0xf2f2f2;
	}

	private int LEFT;
	private int CENTER;

	private int arenaDoor;
	private boolean enteredArena = false;
	private boolean keyDropped = false;

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_VAULT;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_PRISON;
	}

	private static final String DOOR = "door";
	private static final String ENTERED = "entered";
	private static final String DROPPED = "droppped";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DOOR, arenaDoor);
		bundle.put(ENTERED, enteredArena);
		bundle.put(DROPPED, keyDropped);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		arenaDoor = bundle.getInt(DOOR);
		enteredArena = bundle.getBoolean(ENTERED);
		keyDropped = bundle.getBoolean(DROPPED);
	}

	@Override
	protected boolean build() {

		setSize(SIZE, SIZE);

		LEFT = (width() - HALL_WIDTH) / 2;
		CENTER = LEFT + HALL_WIDTH / 2;

		Painter.fill(this, LEFT, TOP, HALL_WIDTH, HALL_HEIGHT, Terrain.EMPTY);
		Painter.fill(this, CENTER, TOP, 1, HALL_HEIGHT, Terrain.EMPTY);

		int y = TOP + 1;
		while (y < TOP + HALL_HEIGHT) {
			map[y * width() + CENTER - 2] = Terrain.CHASM;
			map[y * width() + CENTER + 2] = Terrain.CHASM;
			y += 2;
		}

		int exitPos = (TOP - 1) * width() + CENTER;
		map[exitPos] = Terrain.LOCKED_EXIT;

		arenaDoor = (TOP + HALL_HEIGHT) * width() + CENTER;
		map[arenaDoor] = Terrain.DOOR;

		Painter.fill(this, LEFT, TOP + HALL_HEIGHT + 1, HALL_WIDTH,
				CHAMBER_HEIGHT, Terrain.EMPTY);
		Painter.fill(this, LEFT, TOP + HALL_HEIGHT + 1, 1, CHAMBER_HEIGHT,
				Terrain.WATER);
		Painter.fill(this, LEFT + HALL_WIDTH - 1, TOP + HALL_HEIGHT + 1, 1,
				CHAMBER_HEIGHT, Terrain.WATER);

		int entrancePos = (TOP + HALL_HEIGHT + 2 + Random.Int(CHAMBER_HEIGHT - 1))
				* width() + LEFT + Random.Int(HALL_WIDTH - 2);
		map[entrancePos] = Terrain.PEDESTAL;

		map[exitPos] = Terrain.WALL;

		// Sprouted: teleport-only level, no stairs back
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));

		// decorate
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && Random.Int(10) == 0) {
				map[i] = Terrain.EMPTY_DECO;
			} else if (map[i] == Terrain.WALL && Random.Int(8) == 0) {
				map[i] = Terrain.WALL_DECO;
			}
		}

		// Sprouted: entrance stays as PEDESTAL, no stairs visual

		return true;
	}

	public int pedestal(boolean left) {
		if (left) {
			return (TOP + HALL_HEIGHT / 2) * width() + CENTER - 2;
		} else {
			return (TOP + HALL_HEIGHT / 2) * width() + CENTER + 2;
		}
	}

	@Override
	protected void createMobs() {
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
				do {
					pos = Random.IntRange(LEFT + 1, LEFT + HALL_WIDTH - 2)
							+ Random.IntRange(TOP + HALL_HEIGHT + 1, TOP
									+ HALL_HEIGHT + CHAMBER_HEIGHT) * width();
				} while (pos == entrance());
				for (Item i : bonesItems) {
					drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	@Override
	public void occupyCell(Char ch) {

		super.occupyCell(ch);

		if (!enteredArena && outsideEntraceRoom(ch.pos) && ch == Dungeon.hero) {

			enteredArena = true;

			Mob boss = new ThiefKing();
			Mob bandit1 = new BanditKing();
			Mob bandit2 = new BanditKing();
			boss.state = boss.HUNTING;
			bandit1.state = bandit1.HUNTING;
			bandit2.state = bandit2.HUNTING;
			int count = 0;
			do {
				boss.pos = Random.Int(length());
			} while (!passable[boss.pos]
					|| !outsideEntraceRoom(boss.pos)
					|| (Dungeon.level.heroFOV[boss.pos] && count++ < 20));
			bandit1.pos = (TOP + 1) * width() + CENTER + 1;
			bandit2.pos = (TOP + 1) * width() + CENTER - 1;
			GameScene.add(boss);
			GameScene.add(bandit1);
			GameScene.add(bandit2);

			Dungeon.observe();
		}
	}

	@Override
	public Heap drop(Item item, int cell) {

		if (!keyDropped && item instanceof SkeletonKey) {

			keyDropped = true;
			locked = false;

			set(arenaDoor, Terrain.DOOR);
			GameScene.updateMap(arenaDoor);
			Dungeon.observe();
		}

		return super.drop(item, cell);
	}

	private boolean outsideEntraceRoom(int cell) {
		return cell / width() < arenaDoor / width();
	}

	@Override
	public String tileName(int tile) {
		switch (tile) {
		case Terrain.WATER:
			return "Suspiciously colored water";
		case Terrain.HIGH_GRASS:
			return "Flowers with little gem-shaped leaves";
		default:
			return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
		case Terrain.ENTRANCE:
			return "A ramp leads up to the upper depth.";
		case Terrain.EXIT:
			return "A ramp leads down to the lower depth.";
		case Terrain.WALL_DECO:
		case Terrain.EMPTY_DECO:
			return "Rough-hewn gold ore adorns the dungeon.";
		case Terrain.EMPTY_SP:
			return "Thick carpet covers the floor.";
		case Terrain.STATUE:
		case Terrain.STATUE_SP:
			return "The statue is made of solid gold.";
		case Terrain.BOOKSHELF:
			return "The rows of books on different disciplines fill the bookshelf.";
		default:
			return super.tileDesc(tile);
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		CityLevel.addCityVisuals(this, visuals);
		return visuals;
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return -1;
	}
}
