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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

//a crash report carries the run as last saved, and the desktop loads it back into a free slot;
//a player's zip gives up plain files into that slot and nothing anywhere else
public class CrashSaveTest {

	private int slot;
	private Hero hero;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level level;
	private int depth, branch, gold, deepest, startDay;
	private long seed;
	private float duration;
	private File tmp;

	@BeforeClass
	public static void boot() throws Exception {
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		slot = GamesInProgress.curSlot;
		hero = Dungeon.hero;
		level = Dungeon.level;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		//Dungeon.init below resets the whole run; the tests after this one share it
		gold = Dungeon.gold;
		seed = Dungeon.seed;
		startDay = Dungeon.calendarStartDay;
		deepest = xyz.gabriwar.warpedpixeldungeon.Statistics.deepestFloor;
		duration = xyz.gabriwar.warpedpixeldungeon.Statistics.duration;
		tmp = File.createTempFile( "crash-save", ".zip" );
		for (int s = 1; s <= GamesInProgress.MAX_SLOTS; s++) folder( s ).deleteDirectory();
	}

	@After
	public void tearDown(){
		for (int s = 1; s <= GamesInProgress.MAX_SLOTS; s++) folder( s ).deleteDirectory();
		GamesInProgress.curSlot = slot;
		Dungeon.hero = hero;
		Dungeon.level = level;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.gold = gold;
		Dungeon.seed = seed;
		Dungeon.calendarStartDay = startDay;
		xyz.gabriwar.warpedpixeldungeon.Statistics.deepestFloor = deepest;
		xyz.gabriwar.warpedpixeldungeon.Statistics.duration = duration;
		xyz.gabriwar.warpedpixeldungeon.actors.Actor.clear();
		tmp.delete();
	}

	private static FileHandle folder( int s ){
		return FileUtils.getFileHandle( GamesInProgress.gameFolder( s ) );
	}

	@Test
	public void theRunGoesWithTheReportAndComesBackInAFreeSlot() throws Exception {
		GamesInProgress.curSlot = 1;
		Dungeon.hero = new Hero();
		folder( 1 ).child( "game.dat" ).writeString( "the run", false );
		folder( 1 ).child( "depth14.dat" ).writeString( "the temple", false );

		assertTrue( CrashSave.zip( tmp ) );
		int into = CrashSave.unpack( tmp );
		assertNotEquals( "never over the run it came from", 1, into );
		assertEquals( "the temple", folder( into ).child( "depth14.dat" ).readString() );
		//a bare hero with no run around him does not build: the run goes as it was last saved
		assertEquals( "the run", folder( into ).child( "game.dat" ).readString() );
		assertFalse( folder( into ).child( "lastsave-game.dat" ).exists() );
	}

	@Test
	public void theRunGoesAsItDiedBesideItsLastSave() throws Exception {
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 0xC4A5L;
		Dungeon.init();
		GamesInProgress.curSlot = 1;
		folder( 1 ).child( "game.dat" ).writeString( "the run", false );

		assertTrue( CrashSave.zip( tmp ) );
		int into = CrashSave.unpack( tmp );
		assertEquals( "the run", folder( into ).child( "lastsave-game.dat" ).readString() );
		Bundle live = FileUtils.bundleFromFile( GamesInProgress.gameFile( into ) );
		assertTrue( "the hero as he died", live.contains( "hero" ) );
		assertEquals( "the player's own slot is never written", "the run", folder( 1 ).child( "game.dat" ).readString() );
	}

	@Test
	public void noRunNoSave(){
		Dungeon.hero = null;
		assertFalse( CrashSave.zip( tmp ) );
	}

	@Test
	public void aZipGivesUpPlainFilesIntoTheSlotOnly() throws Exception {
		try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( tmp ) )) {
			for (String name : new String[]{ "game.dat", "../escaped.dat", "sub/inner.dat", "/abs.dat" }){
				zip.putNextEntry( new ZipEntry( name ) );
				zip.write( name.getBytes( StandardCharsets.UTF_8 ) );
				zip.closeEntry();
			}
		}
		int into = CrashSave.unpack( tmp );
		FileHandle dir = folder( into );
		assertEquals( 1, dir.list().length );
		assertTrue( dir.child( "game.dat" ).exists() );
		assertFalse( dir.parent().child( "escaped.dat" ).exists() );
	}
}
