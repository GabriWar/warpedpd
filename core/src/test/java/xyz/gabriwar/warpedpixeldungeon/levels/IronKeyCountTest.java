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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** A generated floor holds exactly one iron key per locked door, on the floor or inside a mimic. */
public class IronKeyCountTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
		com.watabou.utils.FileUtils.setDefaultFileProperties( com.badlogic.gdx.Files.FileType.Absolute,
				new java.io.File( "build/test-saves" ).getAbsolutePath() + "/" );
	}

	@Test
	public void oneKeyPerLockedDoor(){
		StringBuilder bad = new StringBuilder();
		for (long seed = 1; seed <= 6; seed++){
			Dungeon.seed = seed * 7919L;
			GamesInProgress.selectedClass = HeroClass.WARRIOR;
			Dungeon.init();
			for (int depth = 1; depth <= 24; depth++){
				if (depth % 5 == 0) continue;
				Dungeon.branch = 0;
				Dungeon.depth = depth;
				Level level = Dungeon.newLevel();
				int doors = 0;
				for (int c = 0; c < level.length(); c++) if (level.map[c] == Terrain.LOCKED_DOOR) doors++;
				int keys = 0;
				StringBuilder where = new StringBuilder();
				for (Heap h : level.heaps.valueList()){
					for (Item i : h.items){
						if (i instanceof IronKey && ((IronKey) i).depth == depth){
							keys += i.quantity();
							where.append( ' ' ).append( h.type );
						}
					}
				}
				for (Mob m : level.mobs){
					if (m instanceof Mimic && ((Mimic) m).items != null){
						for (Item i : ((Mimic) m).items){
							if (i instanceof IronKey && ((IronKey) i).depth == depth){ keys += i.quantity(); where.append( " mimic" ); }
						}
					}
				}
				if (keys != doors && level instanceof RegularLevel){
					for (Room r : ((RegularLevel) level).rooms()){
						for (Room.Door d : r.connected.values()){
							if (d != null && level.map[level.pointToCell( d )] == Terrain.LOCKED_DOOR) where.append( " locked:" ).append( r.getClass().getSimpleName() );
						}
					}
					for (Mob m : level.mobs) where.append( " " ).append( m.getClass().getSimpleName() );
				}
				if (keys != doors){
					bad.append( "seed " ).append( seed ).append( " depth " ).append( depth ).append( ": " )
							.append( doors ).append( " locked doors, " ).append( keys ).append( " keys" ).append( where ).append( '\n' );
				}
			}
		}
		assertEquals( "", bad.toString() );
	}
}
