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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Every effect the game names is one it loads at boot (Assets.Sounds.all, which
 * WarpedPixelDungeon.create hands to Sample.load) and is there in the assets: one left out of the
 * list is not loaded, and plays as silence. And a run of walls broken never hears the same take
 * twice in a row (WallBreak).
 */
public class SoundAssetsTest {

	private static final File ASSETS = new File( "src/main/assets" );

	@Test
	public void everySoundIsLoadedAtBootAndThere() throws IllegalAccessException {
		List<String> all = Arrays.asList( Assets.Sounds.all );
		int sounds = 0;
		for (Field f : Assets.Sounds.class.getFields()){
			if (f.getType() != String.class || !Modifier.isStatic( f.getModifiers() )) continue;
			String path = (String) f.get( null );
			assertTrue( "Assets.Sounds." + f.getName() + " is not in Assets.Sounds.all", all.contains( path ) );
			assertTrue( "no " + path + " in the assets", new File( ASSETS, path ).isFile() );
			sounds++;
		}
		assertEquals( "Assets.Sounds.all holds every sound once, and nothing else", sounds, all.size() );
		assertEquals( sounds, new HashSet<>( all ).size() );
	}

	@Test
	public void aWallNeverBreaksTheSameWayTwiceRunning(){
		HashSet<String> heard = new HashSet<>();
		String last = null;
		for (int i = 0; i < 300; i++){
			String take = WallBreak.take();
			assertNotEquals( "take " + i, last, take );
			heard.add( take );
			last = take;
		}
		assertEquals( "every take comes up", new HashSet<>( Arrays.asList( WallBreak.TAKES ) ), heard );
	}
}
