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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import com.watabou.noosa.MovieClip;
import com.watabou.utils.RectF;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

/**
 * Every creature's sprite, built the way the game builds one (its no-argument constructor), shows
 * a frame of its animation - never its whole sheet. A sprite that swapped its texture after
 * its parent started the idle (a variant in another pelt) used to show the full sheet until the
 * next frame tick, and in a still portrait (the creature's info window) for good.
 */
public class SpriteFramesTest {

	@BeforeClass
	public static void boot() throws Exception {
		WindowSlideTest.boot();
	}

	@AfterClass
	public static void unboot(){
		WindowSlideTest.unboot();
	}

	@Test
	public void everySpriteShowsItsCurrentFrame() throws Exception {
		Field animField = MovieClip.class.getDeclaredField( "curAnim" );
		animField.setAccessible( true );

		File dir = new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/sprites" );
		String[] files = dir.list();
		assertTrue( files != null && files.length > 100 );
		ArrayList<String> wrong = new ArrayList<>();
		int checked = 0;
		for (String f : files){
			if (!f.endsWith( ".java" )) continue;
			Class<?> cls = Class.forName( "xyz.gabriwar.warpedpixeldungeon.sprites." + f.replace( ".java", "" ) );
			if (!CharSprite.class.isAssignableFrom( cls ) || Modifier.isAbstract( cls.getModifiers() )) continue;
			CharSprite sprite;
			try {
				sprite = (CharSprite) cls.getConstructor().newInstance();
			} catch (NoSuchMethodException e){
				continue;
			} catch (Throwable t){
				//sprites that need a live hero or level to be built are not this test's business
				continue;
			}
			MovieClip.Animation anim = (MovieClip.Animation) animField.get( sprite );
			if (anim == null || anim.frames == null || anim.frames.length == 0) continue;
			checked++;
			//one of its animation's frames (a flock's idles start at a random point of theirs)
			RectF shown = sprite.frame();
			boolean ofItsAnimation = false;
			for (RectF fr : anim.frames){
				if (shown != null && shown.left == fr.left && shown.top == fr.top
						&& shown.right == fr.right && shown.bottom == fr.bottom) ofItsAnimation = true;
			}
			if (!ofItsAnimation) wrong.add( cls.getSimpleName() );
		}
		assertTrue( "only " + checked + " sprites built", checked > 100 );
		assertTrue( "showing more than their frame: " + wrong, wrong.isEmpty() );
	}
}
