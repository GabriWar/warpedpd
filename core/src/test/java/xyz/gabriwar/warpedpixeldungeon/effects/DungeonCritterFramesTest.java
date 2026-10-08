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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The critter sheet against the code that poses it (docs/ambience.md, "The critter sheet"): the
 * art is drawn apart (tools/dungeon_critters_gen.py), so this reads the real file - never the
 * blank stand-in the behaviour tests use - and checks the sheet is the size the film is cut for,
 * that every frame a creature poses has something drawn in it, and that the poses stand where
 * their origins expect: ground poses on row 14, a hanging spider's thread meeting its body at
 * (8, 2), a swimming frog's head clear of the water line.
 */
public class DungeonCritterFramesTest {

	private static Pixmap sheet;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
		assertTrue( Assets.Effects.DUNGEON_CRITTERS + " is not in the assets",
				Gdx.files.internal( Assets.Effects.DUNGEON_CRITTERS ).exists() );
		sheet = new Pixmap( Gdx.files.internal( Assets.Effects.DUNGEON_CRITTERS ) );
	}

	@AfterClass
	public static void down(){
		if (sheet != null) sheet.dispose();
	}

	private static boolean opaque( int frame, int x, int y ){
		int fx = (frame % 8) * 16, fy = (frame / 8) * 16;
		return (sheet.getPixel( fx + x, fy + y ) & 0xFF) != 0;
	}

	//the lowest row of a frame with anything drawn in it, or -1 for an empty frame
	private static int lowest( int frame ){
		for (int y = 15; y >= 0; y--){
			for (int x = 0; x < 16; x++) if (opaque( frame, x, y )) return y;
		}
		return -1;
	}

	private static int highest( int frame ){
		for (int y = 0; y < 16; y++){
			for (int x = 0; x < 16; x++) if (opaque( frame, x, y )) return y;
		}
		return -1;
	}

	@Test
	public void theSheetIsCutForTheFilm(){
		assertEquals( 128, sheet.getWidth() );
		assertEquals( 240, sheet.getHeight() );
	}

	@Test
	public void everyFrameACreaturePosesIsDrawn(){
		for (int f : DungeonCritterSprite.frames()){
			assertTrue( "frame " + f + " (row " + f / 8 + ", col " + f % 8 + ") is empty", lowest( f ) >= 0 );
		}
	}

	@Test
	public void theGroundPosesStandOnRowFourteen(){
		int[][] ground = {
				{ DungeonCritterSprite.ROW_FROG, 0, 1, 2, 4 },
				{ DungeonCritterSprite.ROW_ROACH, 0, 1, 2, 3, 4 },
				{ DungeonCritterSprite.ROW_MOUSE, 0, 1, 2, 3, 4, 5 },
				{ DungeonCritterSprite.ROW_SPIDER, 2, 3 },
				{ DungeonCritterSprite.ROW_NEWT, 0, 1, 2, 3 },
				{ DungeonCritterSprite.ROW_SALAMANDER, 0, 1, 2, 3, 4 },
				{ DungeonCritterSprite.ROW_LIZARD, 0, 1, 2, 3 },
				{ DungeonCritterSprite.ROW_SNAIL, 0, 1, 2 },
				{ DungeonCritterSprite.ROW_BEETLE, 0, 1, 3 },
				{ DungeonCritterSprite.ROW_EMBER_BEETLE, 0, 1, 3, 4 },
				{ DungeonCritterSprite.ROW_CENTIPEDE, 0, 1, 2 },
				{ DungeonCritterSprite.ROW_SILVERFISH, 0, 1 },
		};
		for (int[] row : ground){
			for (int i = 1; i < row.length; i++){
				int f = row[0] * 8 + row[i];
				assertEquals( "the feet of row " + row[0] + ", col " + row[i], 14, lowest( f ) );
			}
		}
	}

	@Test
	public void theHangingSpidersThreadMeetsItsBodyAndTheSwimmingFrogsHeadIsClear(){
		for (int col : new int[]{ 0, 1 }){
			int f = DungeonCritterSprite.ROW_SPIDER * 8 + col;
			assertEquals( "the top of the hanging spider, col " + col, 2, highest( f ) );
			assertTrue( "the thread meets the spider at (8, 2), col " + col, opaque( f, 8, 2 ) );
		}
		int swim = DungeonCritterSprite.ROW_FROG * 8 + 5;
		assertTrue( "the swimming frog shows below the water line", lowest( swim ) <= 12 );
	}
}
