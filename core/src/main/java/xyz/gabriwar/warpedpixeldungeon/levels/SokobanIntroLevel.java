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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanBlack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ActivatePortalTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChangeSheepTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.HeapGenTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SokobanPortalTrap;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Towel;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.Collection;
import java.util.HashSet;

public class SokobanIntroLevel extends Level {

	private static final int SIZE = 48;

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
	}

	public HashSet<Item> heapstogen;
	public int[] heapgenspots;
	public int[] teleportspots;
	public int[] portswitchspots;
	public int[] teleportassign;
	public int[] destinationspots;
	public int[] destinationassign;
	public int prizeNo;

	private static final String HEAPSTOGEN = "heapstogen";
	private static final String HEAPGENSPOTS = "heapgenspots";
	private static final String TELEPORTSPOTS = "teleportspots";
	private static final String PORTSWITCHSPOTS = "portswitchspots";
	private static final String DESTINATIONSPOTS = "destinationspots";
	private static final String TELEPORTASSIGN = "teleportassign";
	private static final String DESTINATIONASSIGN = "destinationassign";
	private static final String PRIZENO = "prizeNo";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HEAPSTOGEN, heapstogen);
		bundle.put(HEAPGENSPOTS, heapgenspots);
		bundle.put(TELEPORTSPOTS, teleportspots);
		bundle.put(PORTSWITCHSPOTS, portswitchspots);
		bundle.put(DESTINATIONSPOTS, destinationspots);
		bundle.put(DESTINATIONASSIGN, destinationassign);
		bundle.put(TELEPORTASSIGN, teleportassign);
		bundle.put(PRIZENO, prizeNo);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);

		heapgenspots = bundle.getIntArray(HEAPGENSPOTS);
		teleportspots = bundle.getIntArray(TELEPORTSPOTS);
		portswitchspots = bundle.getIntArray(PORTSWITCHSPOTS);
		destinationspots = bundle.getIntArray(DESTINATIONSPOTS);
		destinationassign = bundle.getIntArray(DESTINATIONASSIGN);
		teleportassign = bundle.getIntArray(TELEPORTASSIGN);
		prizeNo = bundle.getInt(PRIZENO);

		heapstogen = new HashSet<>();

		Collection<Bundlable> collectionheap = bundle.getCollection(HEAPSTOGEN);
		for (Bundlable i : collectionheap) {
			Item item = (Item) i;
			if (item != null) {
				heapstogen.add(item);
			}
		}
	}

	@Override
	public void create() {
		heapstogen = new HashSet<>();
		heapgenspots = new int[10];
		teleportspots = new int[10];
		portswitchspots = new int[10];
		destinationspots = new int[10];
		destinationassign = new int[10];
		teleportassign = new int[10];
		super.create();
	}

	public void addItemToGen(Item item, int arraypos, int pos) {
		if (item != null) {
			heapstogen.add(item);
			heapgenspots[arraypos] = pos;
		}
	}

	public Item genPrizeItem() {
		return genPrizeItem(null);
	}

	public Item genPrizeItem(Class<? extends Item> match) {
		boolean keysLeft = false;

		if (heapstogen.size() == 0)
			return null;

		for (Item item : heapstogen) {
			if (match != null && match.isInstance(item)) {
				heapstogen.remove(item);
				keysLeft = true;
				return item;
			}
		}

		if (match == null || !keysLeft) {
			Item item = Random.element(heapstogen);
			heapstogen.remove(item);
			return item;
		}

		return null;
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

		map = SokobanLayouts.SOKOBAN_INTRO_LEVEL.clone();

		createSwitches();
		createSheep();

		int entrancePos = 7 + width() * 3;
		// Sprouted: journal-accessed level, no stairs back
		transitions.add(new LevelTransition(this, entrancePos, LevelTransition.Type.BRANCH_ENTRANCE));
		map[entrancePos] = Terrain.EMPTY;

		return true;
	}

	protected void createSheep() {
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.SOKOBAN_SHEEP) {
				SheepSokoban npc = new SheepSokoban();
				mobs.add(npc);
				npc.pos = i;
			} else if (map[i] == Terrain.CORNER_SOKOBAN_SHEEP) {
				SheepSokobanCorner npc = new SheepSokobanCorner();
				mobs.add(npc);
				npc.pos = i;
			} else if (map[i] == Terrain.SWITCH_SOKOBAN_SHEEP) {
				SheepSokobanSwitch npc = new SheepSokobanSwitch();
				mobs.add(npc);
				npc.pos = i;
			} else if (map[i] == Terrain.BLACK_SOKOBAN_SHEEP) {
				SheepSokobanBlack npc = new SheepSokobanBlack();
				mobs.add(npc);
				npc.pos = i;
			}
		}
	}

	protected void createSwitches() {
		//spots where your portals are
		teleportspots[0] = 7 + width() * 21;

		//spots where your portal switches are
		portswitchspots[0] = 8 + width() * 27;

		//assign each switch to a portal
		teleportassign[0] = 7 + width() * 21;

		//assign each switch to a destination spot
		destinationassign[0] = 34 + width() * 8;

		//set the original destination of portals
		destinationspots[0] = 0;
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		int goldmin = 1;
		int goldmax = 100;
		if (firstVisit) {
			goldmin = 300;
			goldmax = 500;
		}
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.SOKOBAN_HEAP) {
				if (firstVisit && Random.Int(5) == 0) {
					drop(new ScrollOfUpgrade(), i).type = Heap.Type.CHEST;
				} else {
					drop(new Gold(Random.Int(goldmin, goldmax)), i).type = Heap.Type.CHEST;
				}
			}
		}

		addItemToGen(new IronKey(Dungeon.depth), 0, 24 + width() * 21);

		if (firstVisit) {
			addItemToGen(new Towel(), 1, 37 + width() * 21);
		} else {
			addItemToGen(new Gold(1), 1, 37 + width() * 21);
		}
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
		boolean sheepEffect = false;

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

			case Terrain.SOKOBAN_PORT_SWITCH:
				if (isHero) {
					ActivatePortalTrap apt = new ActivatePortalTrap();
					apt.pos = cell;
					apt.activate();
				} else if (isSheep) {
					int arraypos = -1;
					int portpos = -1;
					int portarray = -1;
					int destpos = -1;

					for (int i = 0; i < portswitchspots.length; i++) {
						if (portswitchspots[i] == cell) {
							arraypos = i;
							break;
						}
					}

					if (arraypos != -1) {
						portpos = teleportassign[arraypos];
						destpos = destinationassign[arraypos];

						for (int i = 0; i < teleportspots.length; i++) {
							if (teleportspots[i] == portpos) {
								portarray = i;
								break;
							}
						}

						if (portarray != -1 && map[portpos] == Terrain.PORT_WELL) {
							destinationspots[portarray] = destpos;
							GLog.i("Click!");
						}
					}

					sheepEffect = true;
					trapTriggered = true;
				}
				break;

			case Terrain.PORT_WELL:
				if (isHero) {
					int portarray = -1;
					for (int i = 0; i < teleportspots.length; i++) {
						if (teleportspots[i] == cell) {
							portarray = i;
							break;
						}
					}
					if (portarray != -1) {
						int destpos = destinationspots[portarray];
						if (destpos > 0) {
							SokobanPortalTrap spt = new SokobanPortalTrap();
							spt.pos = cell;
							spt.destPos = destpos;
							spt.activate();
						}
					}
				}
				break;

			case Terrain.SOKOBAN_ITEM_REVEAL:
				if (isSheep) {
					HeapGenTrap hgt = new HeapGenTrap();
					hgt.pos = cell;
					hgt.activate();
					if (prizeNo < heapgenspots.length) {
						drop(genPrizeItem(IronKey.class), heapgenspots[prizeNo]);
						prizeNo++;
					}
					sheepEffect = true;
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
			} else if (sheepEffect) {
				set(cell, Terrain.EMPTY);
			} else {
				set(cell, Terrain.INACTIVE_TRAP);
			}
			GameScene.updateMap(cell);
		}

		super.occupyCell(ch);
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return -1;
	}
}
