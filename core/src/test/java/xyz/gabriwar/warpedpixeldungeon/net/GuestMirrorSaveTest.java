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

package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A co-op guest stands in the host's game, mirrored from the packets: it is never his to save.
 * When his connection dropped he was no longer a client, and the main menu (or the app going to
 * the background) saved that mirror into the slot he last played: the host's game over his own
 * run, and then a crash, as a SpectatorLevel has no water-phase array to write.
 */
public class GuestMirrorSaveTest {

	private static final int SLOT = 1;

	private int slot, depth, branch, gold, deepest, startDay;
	private long seed;
	private float duration;
	private Hero hero;
	private Level level;

	@BeforeClass
	public static void boot() throws Exception {
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		slot = GamesInProgress.curSlot;
		hero = Dungeon.hero;
		level = Dungeon.level;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		//Dungeon.init below resets the whole run; the tests after this one share it
		gold = Dungeon.gold;
		seed = Dungeon.seed;
		startDay = Dungeon.calendarStartDay;
		deepest = Statistics.deepestFloor;
		duration = Statistics.duration;
		folder().deleteDirectory();
	}

	@After
	public void tearDown(){
		folder().deleteDirectory();
		GamesInProgress.curSlot = slot;
		Dungeon.hero = hero;
		Dungeon.level = level;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.gold = gold;
		Dungeon.seed = seed;
		Dungeon.calendarStartDay = startDay;
		Statistics.deepestFloor = deepest;
		Statistics.duration = duration;
		Actor.clear();
	}

	private static FileHandle folder(){
		return FileUtils.getFileHandle( GamesInProgress.gameFolder( SLOT ) );
	}

	@Test
	public void aGuestWhoseConnectionDroppedSavesNothing() throws Exception {
		GamesInProgress.selectedClass = HeroClass.CLERIC;
		Dungeon.seed = 0xC0DEL;
		Dungeon.init();
		GamesInProgress.curSlot = SLOT;
		folder().child( "game.dat" ).writeString( "his own run", false );

		//the host's floor as the guest's device builds it from the packets
		int[] map = new int[16 * 16];
		Arrays.fill( map, Terrain.EMPTY );
		Dungeon.level = NetLevels.create( null, 0, map, 16, 16, null, null );
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		//the connection dropped: NetManager.handleDisconnect left him no client
		assertFalse( NetManager.isNetClient() );
		assertTrue( Dungeon.hero.isAlive() );

		Dungeon.saveAll();

		assertEquals( "his own run stays his", "his own run", folder().child( "game.dat" ).readString() );
		assertFalse( "and no floor of the host's lands beside it",
				folder().child( "depth1.dat" ).exists() );
	}
}
