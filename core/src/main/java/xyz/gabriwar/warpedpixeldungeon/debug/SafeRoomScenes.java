/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.legacy.SafeRoomMigration;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.FadedSafeSpotPage;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The safe room's removal (homestead.legacy.SafeRoomMigration), seen from a save:
 *
 * homestead-migration-check: the hero is taken to the town's plaza on the surface (from anywhere),
 * a made-up old safe room is written as this slot's depth-50 file - a heap of a weapon, a potion
 * and 250 gold, a golden storage chest holding three things, a sungrass crop, an alchemy pot and
 * an enchanting pedestal - and the load's migration runs on it: the chest of all it held is laid
 * beside the plaza (the kit packed with its contents, the crop's seed, 2300 gold for the pot and
 * the pedestal), and the file goes with the next save. A faded safe spot page lands in the pack:
 * read it for a builder's tool (or its worth in gold when there is one already).
 */
final class SafeRoomScenes {

	private SafeRoomScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new MigrationCheck()
	};

	private static final class MigrationCheck implements DebugScenes.Scene {
		//set while the hero is on his way to the plaza
		boolean travelling;

		@Override public String id(){ return "homestead-migration-check"; }
		@Override public String title(){ return "Homestead: the old safe room's belongings carried to the town"; }

		@Override public void apply( Hero hero ){
			OverworldLevel ow = Dungeon.level instanceof OverworldLevel ? (OverworldLevel) Dungeon.level : null;
			if (ow == null || ow.altitude() != 0 || SafeRoomMigration.chestCell( ow ) == -1){
				if (travelling){
					travelling = false;
					GLog.w( "Scene: could not reach the town's plaza" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
						WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) );
				return;
			}
			travelling = false;
			if (ow.isMirror()){
				GLog.w( "Scene: only the host runs the migration" );
				return;
			}

			String file = GamesInProgress.depthFile( GamesInProgress.curSlot, SafeRoomMigration.DEPTH, 0 );
			try {
				FileUtils.bundleToFile( file, oldRoom() );
			} catch (IOException e){
				GLog.w( "Scene: could not write the old room's file: " + e.getMessage() );
				return;
			}
			if (!Dungeon.generatedLevels.contains( SafeRoomMigration.DEPTH )){
				Dungeon.generatedLevels.add( SafeRoomMigration.DEPTH );
			}
			SafeRoomMigration.rearm();
			SafeRoomMigration.migrate( GamesInProgress.curSlot );
			int cell = SafeRoomMigration.chestCell( ow );
			SafeRoomMigration.deliver( ow );

			if (!new FadedSafeSpotPage().collect( hero.belongings.backpack )){
				GLog.w( "Scene: the pack is full, no faded page this time" );
			}
			Heap laid = cell == -1 ? null : ow.heaps.get( cell );
			GLog.i( "Scene: the old room's chest holds " + (laid == null ? "nothing (not laid)" : laid.items.size() + " things")
					+ ", beside the plaza."
					+ " Read the faded safe spot page in your pack: a builder's tool, or gold if you have one."
					+ " The depth-50 file is deleted with the next save." );
		}

		//a safe room as an old save left it: a heap, a chest with things in it, a crop, the two
		//amenities the builder's tool sold
		private static com.watabou.utils.Bundle oldRoom(){
			Heap heap = new Heap();
			heap.pos = 23 + 96 * 16;
			heap.drop( Generator.random( Generator.Category.WEAPON ) );
			heap.drop( Generator.random( Generator.Category.POTION ) );
			heap.drop( new Gold( 250 ) );

			StorageChest chest = new StorageChest();
			chest.pos = 20 + 96 * 12;
			chest.type = PortableChest.GOLDEN;
			chest.storage.cap = PortableChest.capacityFor( PortableChest.GOLDEN );
			chest.storage.items.add( Generator.random( Generator.Category.RING ) );
			chest.storage.items.add( Generator.random( Generator.Category.SCROLL ) );
			chest.storage.items.add( Generator.random( Generator.Category.FOOD ) );

			FarmCrop crop = new FarmCrop();
			crop.pos = 30 + 96 * 20;
			crop.sow( new Sungrass.Seed() );

			int[] map = new int[96 * 96];
			Arrays.fill( map, Terrain.EMPTY );
			map[25 + 96 * 10] = Terrain.ALCHEMY;
			map[27 + 96 * 10] = Terrain.PEDESTAL;

			ArrayList<Heap> heaps = new ArrayList<>();
			heaps.add( heap );
			ArrayList<Mob> mobs = new ArrayList<>();
			mobs.add( chest );
			mobs.add( crop );
			return SafeRoomMigration.roomFile( heaps, mobs, map );
		}
	}
}
