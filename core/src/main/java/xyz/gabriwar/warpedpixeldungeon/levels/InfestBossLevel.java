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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class InfestBossLevel extends Level {

	private static final int SIZE = 48;

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;

		viewDistance = 6;
	}

	private int ROOM_LEFT;
	private int ROOM_RIGHT;
	private int ROOM_TOP;
	private int ROOM_BOTTOM;

	private int arenaDoor;
	private boolean enteredArena = false;
	private boolean keyDropped = false;

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CAVES;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CAVES;
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

		ROOM_LEFT = width() / 2 - 2;
		ROOM_RIGHT = width() / 2 + 2;
		ROOM_TOP = height() / 2 - 2;
		ROOM_BOTTOM = height() / 2 + 2;

		int topMost = Integer.MAX_VALUE;
		int exitPos = 0;

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

		map[exitPos] = Terrain.WALL;

		// scatter hidden summoning traps in open areas
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && Random.Int(20) == 0) {
				setTrap(new SummoningTrap().hide(), i);
				map[i] = Terrain.SECRET_TRAP;
			}
		}

		Painter.fill(this, ROOM_LEFT - 1, ROOM_TOP - 1, ROOM_RIGHT - ROOM_LEFT
				+ 3, ROOM_BOTTOM - ROOM_TOP + 3, Terrain.WALL);
		Painter.fill(this, ROOM_LEFT, ROOM_TOP + 1, ROOM_RIGHT - ROOM_LEFT + 1,
				ROOM_BOTTOM - ROOM_TOP, Terrain.EMPTY);

		// Place visible summoning traps along the top of the arena room
		for (int x = ROOM_LEFT; x <= ROOM_RIGHT; x++) {
			int cell = x + ROOM_TOP * width();
			setTrap(new SummoningTrap().reveal(), cell);
			map[cell] = Terrain.TRAP;
		}

		arenaDoor = Random.Int(ROOM_LEFT, ROOM_RIGHT) + (ROOM_BOTTOM + 1)
				* width();
		map[arenaDoor] = Terrain.DOOR;

		int entrancePos = Random.Int(ROOM_LEFT + 1, ROOM_RIGHT - 1)
				+ Random.Int(ROOM_TOP + 1, ROOM_BOTTOM - 1) * width();
		map[entrancePos] = Terrain.ENTRANCE;

		// Sprouted: teleport-only level, no stairs back
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));

		// decorate
		for (int i = width() + 1; i < length() - width(); i++) {
			if (map[i] == Terrain.EMPTY) {
				int n = 0;
				if (map[i + 1] == Terrain.WALL) {
					n++;
				}
				if (map[i - 1] == Terrain.WALL) {
					n++;
				}
				if (map[i + width()] == Terrain.WALL) {
					n++;
				}
				if (map[i - width()] == Terrain.WALL) {
					n++;
				}
				if (Random.Int(8) <= n) {
					map[i] = Terrain.EMPTY_DECO;
				}
			}
		}

		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.WALL && Random.Int(8) == 0) {
				map[i] = Terrain.WALL_DECO;
			}
			if (map[i] == Terrain.ENTRANCE) {
				map[i] = Terrain.PEDESTAL;
			}
		}

		// Sprouted: entrance stays as PEDESTAL (set in decorate loop above), no stairs visual

		return true;
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
	public int mobLimit() {
		return 20;
	}

	@Override
	protected void createItems() {
	}

	@Override
	public void occupyCell(Char ch) {

		super.occupyCell(ch);

		if (!enteredArena && outsideEntraceRoom(ch.pos) && ch == Dungeon.hero) {

			enteredArena = true;

			for (int i = 0; i < 10; i++) {
				ShadowYog boss = new ShadowYog();
				boss.state = boss.SLEEPING;
				do {
					boss.pos = Random.Int(length());
				} while (!passable[boss.pos] || !outsideEntraceRoom(boss.pos)
						|| Dungeon.level.heroFOV[boss.pos]);
				GameScene.add(boss);
			}
			GLog.n("we are legion");

			GameScene.updateMap(arenaDoor);
			Dungeon.observe();
		}
	}

	private boolean outsideEntraceRoom(int cell) {
		int cx = cell % width();
		int cy = cell / width();
		return cx < ROOM_LEFT - 1 || cx > ROOM_RIGHT + 1 || cy < ROOM_TOP - 1
				|| cy > ROOM_BOTTOM + 1;
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

	@Override
	public int randomRespawnCell( Char ch ) {
		int cell;
		do {
			cell = Random.Int(length());
		} while (!passable[cell] || Actor.findChar(cell) != null);
		return cell;
	}
}
