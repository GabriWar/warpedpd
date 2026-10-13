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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.levels.DeadEndLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import static org.junit.Assert.assertNotNull;

//a level a co-op guest builds from the host's word gets every collection Level.create() makes:
//GameScene walks them all as it opens, and a missing one crashed it
public class NetworkLevelCollectionsTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void aGuestsLevelHasEveryCollection(){
		Level l = new DeadEndLevel();
		OverworldLevel.initNetworkCollections( l );
		assertNotNull( l.transitions );
		assertNotNull( l.mobs );
		assertNotNull( l.heaps );
		assertNotNull( l.blobs );
		assertNotNull( l.plants );
		assertNotNull( l.traps );
		assertNotNull( l.customTiles );
		assertNotNull( "the terrain overlays GameScene walks", l.customTerrain );
		assertNotNull( l.customWalls );
	}
}
