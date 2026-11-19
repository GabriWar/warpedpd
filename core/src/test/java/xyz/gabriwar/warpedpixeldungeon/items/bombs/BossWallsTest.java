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

package xyz.gabriwar.warpedpixeldungeon.items.bombs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A boss floor's walls and doors stand any blast while its boss lives: bombs were a way round
 * an arena to the stairs down without the fight. Once the boss is dead they break as anywhere.
 */
public class BossWallsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
		com.watabou.utils.FileUtils.setDefaultFileProperties( com.badlogic.gdx.Files.FileType.Absolute,
				new java.io.File( "build/test-saves" ).getAbsolutePath() + "/" );
	}

	@Test
	public void wallsStandWhileTheBossLives(){
		Dungeon.seed = 4242;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();
		Dungeon.branch = 0;
		Dungeon.depth = 5;
		Dungeon.level = Dungeon.newLevel();
		assertTrue( "Goo is on his floor", Bomb.bossAlive() );

		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m.properties().contains( Char.Property.BOSS )) m.HP = 0;
		}
		assertFalse( "and once he is dead the walls break", Bomb.bossAlive() );
	}
}
