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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LitTower;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanBlack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Palantir;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChangeSheepTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOtilukeMessage;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class MinesBossLevel extends Level {

	private static final int SIZE = 48;

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
	}

	private boolean entered = false;
	private static final String ENTERED = "entered";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ENTERED, entered);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		entered = bundle.getBoolean(ENTERED);
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

		map = Layouts.MINE_BOSS.clone();

		buildFlagMaps();
		cleanWalls();

		int entrancePos = 17 + width() * 44;
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.REGULAR_ENTRANCE));
		map[entrancePos] = Terrain.ENTRANCE;

		// exit = 0 in original (no exit)

		return true;
	}

	@Override
	protected void createMobs() {
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.SOKOBAN_SHEEP) {
				MineSentinel npc = new MineSentinel();
				mobs.add(npc);
				npc.pos = i;
			} else if (map[i] == Terrain.CORNER_SOKOBAN_SHEEP) {
				LitTower npc = new LitTower();
				mobs.add(npc);
				npc.pos = i;
			}
		}

		Otiluke mob = new Otiluke();
		mobs.add(mob);
		mob.pos = 33 + width() * 10;
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
		drop(new IronKey(Dungeon.depth), 30 + width() * 44).type = Heap.Type.CHEST;
		drop(new Palantir(), 14 + width() * 10);
	}

	@Override
	public void occupyCell(Char ch) {
		int cell = ch.pos;

		boolean isSheep = ch instanceof SheepSokoban || ch instanceof SheepSokobanSwitch
				|| ch instanceof SheepSokobanCorner || ch instanceof SheepSokobanBlack
				|| ch instanceof Sheep;
		boolean isHero = ch == Dungeon.hero;

		boolean trapTriggered = false;
		boolean fleeced = false;

		switch (map[cell]) {
			case Terrain.FLEECING_TRAP:
				if (isHero) {
					FleecingTrap ft = new FleecingTrap();
					ft.pos = cell;
					ft.activate();
					trapTriggered = true;
				} else if (isSheep) {
					FleecingTrap ft = new FleecingTrap();
					ft.pos = cell;
					ft.activate();
					fleeced = true;
					trapTriggered = true;
				}
				break;

			case Terrain.CHANGE_SHEEP_TRAP:
				if (isSheep) {
					ChangeSheepTrap cst = new ChangeSheepTrap();
					cst.pos = cell;
					cst.activate();
					trapTriggered = true;
				}
				break;
		}

		if (trapTriggered) {
			if (Dungeon.level.heroFOV[cell]) {
				Sample.INSTANCE.play(Assets.Sounds.TRAP);
			}
			if (fleeced) {
				set(cell, Terrain.WOOL_RUG);
			} else {
				set(cell, Terrain.INACTIVE_TRAP);
			}
			GameScene.updateMap(cell);
		}

		super.occupyCell(ch);

		if (isHero && !entered) {
			entered = true;
			locked = true;
			GameScene.show(new WndOtilukeMessage());
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
			case Terrain.EMPTY_DECO:
				return "There are old blood stains on the floor.";
			default:
				return super.tileDesc( tile );
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		PrisonLevel.addPrisonVisuals( this, visuals );
		return visuals;
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return -1;
	}
}
