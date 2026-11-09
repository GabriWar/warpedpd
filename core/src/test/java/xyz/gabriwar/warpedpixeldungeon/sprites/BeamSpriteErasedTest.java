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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import com.watabou.noosa.Group;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNull;

/**
 * A broken robot's and a magic eye's beam is drawn when their strike's animation ends, after the
 * strike landed - and the strike can take the sprite down: a mirage in the robot's shape dissolves
 * on its own blow (Mirage.attackProc). The beam then had no scene to go in and the game crashed.
 */
public class BeamSpriteErasedTest {

	private Level savedLevel;

	@BeforeClass
	public static void boot() throws Exception {
		WindowSlideTest.boot();
	}

	@AfterClass
	public static void unboot(){
		WindowSlideTest.unboot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		Level level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( 10, 10 );
		Arrays.fill( level.heroFOV, true );
		Dungeon.level = level;
	}

	@After
	public void tearDown(){
		Dungeon.level = savedLevel;
	}

	//a mob gone with its strike, as a mirage is: its sprite erased from the scene
	private static class Vanishing extends Mob {
		@Override
		public void onAttackComplete(){
			sprite.killAndErase();
		}
	}

	private static void strikeAndVanish( CharSprite sprite ){
		Vanishing mob = new Vanishing();
		mob.pos = 11;
		mob.sprite = sprite;
		sprite.ch = mob;
		Group scene = new Group();
		scene.add( sprite );
		sprite.onComplete( sprite.attack );
		assertNull( sprite.parent );
	}

	@Test
	public void aBrokenRobotGoneWithItsStrikeFiresNoBeam(){
		strikeAndVanish( new BrokenRobotSprite() );
	}

	@Test
	public void aMagicEyeGoneWithItsStrikeFiresNoBeam(){
		strikeAndVanish( new MagicEyeSprite() );
	}
}
