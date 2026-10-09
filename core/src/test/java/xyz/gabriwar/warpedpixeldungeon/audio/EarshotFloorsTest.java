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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * The rock's echo (Earshot) on floors as the game builds them: the dungeon's caves and the mines
 * are tunnels and chambers whose walls are mostly near, and still have far walls to answer from in
 * their big spaces.
 */
public class EarshotFloorsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
		//Dungeon.init reads the global badges: keep the files a run touches in the build folder
		com.watabou.utils.FileUtils.setDefaultFileProperties( com.badlogic.gdx.Files.FileType.Absolute,
				new java.io.File( "build/test-saves" ).getAbsolutePath() + "/" );
	}

	@Test
	public void theCavesAndTheMinesEcho(){
		Dungeon.seed = 12345L;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();
		//a floor of the caves and one of the mines
		for (int depth : new int[]{ 12, 56 }){
			Dungeon.branch = 0;
			Dungeon.depth = depth;
			Level level = Dungeon.newLevel();
			int rock = 0, echo = 0;
			for (int c = 0; c < level.length(); c++){
				if (!level.passable[c] || level.solid[c]) continue;
				Earshot.Snapshot s = Earshot.of( level, c );
				if (s == null || s.space != Earshot.Space.HUGE || !s.rock) continue;
				rock++;
				if (s.echoDelay > 0f) echo++;
			}
			assertTrue( "depth " + depth + ": " + rock + " cells of a big rock space", rock > 50 );
			assertTrue( "depth " + depth + ": " + echo + " of " + rock + " cells echo", echo >= rock / 10 );
		}
	}
}
