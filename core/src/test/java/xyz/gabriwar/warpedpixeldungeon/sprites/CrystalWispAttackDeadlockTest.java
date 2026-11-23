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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Group;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;

/**
 * A crystal wisp's strike (on the actor thread) took the sprite's lock and then its layer's, to add
 * the light's flare; the render thread updating that layer holds the layer's lock and then takes the
 * sprite's to step its animation. Each waited on the other for good and the game froze (FREEZE:
 * render thread stalled, the actor thread in Group.add from CrystalWispSprite.attack).
 */
public class CrystalWispAttackDeadlockTest {

	private Level savedLevel;
	private Camera savedCamera;
	private Hero savedHero;

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
		savedCamera = Camera.main;
		savedHero = Dungeon.hero;
		Camera.main = new Camera( 0, 0, 100, 100, 1 );
		Level level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( 10, 10 );
		Arrays.fill( level.heroFOV, true );
		Dungeon.level = level;
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		Dungeon.level = savedLevel;
		Camera.main = savedCamera;
		Dungeon.hero = savedHero;
	}

	@Test
	public void aStrikeWhileItsLayerUpdatesDoesNotLockUp() throws Exception {
		CrystalWisp wisp = new CrystalWisp();
		wisp.pos = 11;
		wisp.state = wisp.PASSIVE; //no sleep or unaware mark over it: those go in a game scene
		CrystalWispSprite sprite = new CrystalWispSprite.Blue();
		sprite.link( wisp );
		Group layer = new Group();
		layer.add( sprite );

		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread actor = new Thread( () -> {
			try {
				sprite.attack( 12 );
			} catch (Throwable t){
				failure.set( t );
			}
		}, "actor" );
		actor.setDaemon( true );

		//the render thread: the layer's lock held (Group.update), the strike started meanwhile and
		//waiting on that lock, then the sprite's animation stepped (MovieClip.updateAnimation, under
		//the sprite's lock)
		Thread render = new Thread( () -> {
			try {
				synchronized (layer){
					actor.start();
					while (actor.getState() != Thread.State.BLOCKED && actor.isAlive()) Thread.yield();
					layer.update();
				}
			} catch (Throwable t){
				failure.set( t );
			}
		}, "render" );
		render.setDaemon( true );
		render.start();

		render.join( 5000 );
		actor.join( 5000 );
		assertFalse( "the render thread stuck on the sprite the strike holds", render.isAlive() );
		assertFalse( "the strike stuck on the layer the render thread holds", actor.isAlive() );
		if (failure.get() != null) throw new AssertionError( failure.get() );
	}
}
