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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanBlack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ActivatePortalTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChangeSheepTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.HeapGenTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SokobanPortalTrap;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.Collection;
import java.util.HashSet;

public class SafeLevel extends Level {

	private static final int SIZE = 96;

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
	}

	@Override
	public com.watabou.noosa.Group addVisuals() {
		super.addVisuals();
		for (int i = 0; i < length(); i++){
			if (map[i] == Terrain.FURROWED_GRASS){
				visuals.add( new TilledSoil( i ) );
			}
		}
		return visuals;
	}

	//a very subtle drift of tiny motes rising off tilled planting soil
	private static class TilledSoil extends com.watabou.noosa.Group {

		private int pos;
		private com.watabou.noosa.particles.Emitter emitter;

		public TilledSoil( int pos ){
			super();
			this.pos = pos;

			com.watabou.utils.PointF p = xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld( pos );
			emitter = new com.watabou.noosa.particles.Emitter();
			emitter.pos( p.x - 5, p.y - 4, 10, 8 );
			emitter.pour( SoilMote.FACTORY, 0.9f );  //sparse - barely-there glimmer
			add( emitter );
		}

		@Override
		public void update() {
			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])){
				super.update();
				if (Dungeon.level.map[pos] != Terrain.FURROWED_GRASS){
					kill();
				}
			}
		}
	}

	public static class SoilMote extends com.watabou.noosa.particles.PixelParticle {

		public static final com.watabou.noosa.particles.Emitter.Factory FACTORY
				= new com.watabou.noosa.particles.Emitter.Factory() {
			@Override
			public void emit( com.watabou.noosa.particles.Emitter emitter, int index, float x, float y ){
				((SoilMote) emitter.recycle( SoilMote.class )).reset( x, y );
			}
		};

		public SoilMote(){
			super();
			lifespan = 1.4f;
			acc.set( 0, -6 );  //drift gently upward
		}

		public void reset( float x, float y ){
			revive();
			this.x = x;
			this.y = y;
			left = lifespan;
			//soft earthy green
			color( com.watabou.utils.Random.Int(2) == 0 ? 0x8ba84f : 0x9c7a4a );
			size( 1 );
			speed.set( 0, -3 );
		}

		@Override
		public void update() {
			super.update();
			//fade in then out
			float p = left / lifespan;
			am = p < 0.5f ? (p * 2f) * 0.5f : (1 - p) * 2f * 0.5f;
		}
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

		firstVisit = true;
		setSize(SIZE, SIZE);

		// Fill everything with walls, then copy the 48x48 default layout into the top-left corner
		java.util.Arrays.fill(map, Terrain.WALL);
		int[] defaultLayout = Layouts.SAFE_ROOM_DEFAULT;
		int layoutSize = 48;
		for (int row = 0; row < layoutSize; row++) {
			for (int col = 0; col < layoutSize; col++) {
				map[col + row * SIZE] = defaultLayout[col + row * layoutSize];
			}
		}

		// Reveal the whole level from the start
		for (int i = 0; i < length(); i++) {
			mapped[i] = true;
			visited[i] = true;
		}

		createSwitches();
		createSheep();

		int entrancePos = 23 + width() * 15;
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
		//spots where your portal switches are


		//spots where your portals are


		//assign each switch to a portal


		//assign each switch to a destination spot


		//set the original destination of portals

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
		for (int i = 0; i < length(); i++) {
			if (firstVisit && map[i] == Terrain.SOKOBAN_HEAP) {
				if (Random.Int(5) == 0) {
					drop(new ScrollOfUpgrade(), i).type = Heap.Type.CHEST;
				} else {
					drop(new Gold(Random.Int(500, 1500)), i).type = Heap.Type.CHEST;
				}
			}
		}
		if (firstVisit) {
			addItemToGen(new IronKey(Dungeon.depth), 0, 23 + width() * 17);
			drop(new BuildersTool(), 23 + width() * 15);
		} else {
			addItemToGen(new Food(), 0, 23 + width() * 17);
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
	public void updateFieldOfView(xyz.gabriwar.warpedpixeldungeon.actors.Char c, boolean[] fieldOfView) {
		java.util.Arrays.fill(fieldOfView, true);
		if (c == Dungeon.hero) {
			java.util.Arrays.fill(visited, true);
			java.util.Arrays.fill(mapped, true);
			GameScene.updateFog();
		}
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return -1;
	}
}
