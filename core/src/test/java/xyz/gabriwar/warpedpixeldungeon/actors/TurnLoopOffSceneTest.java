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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The turn loop's thread outlives its game scene, waiting, and a co-op player's input or leaving
 * wakes it from the network (NetManager.wakeActorThread) whatever the host is looking at. Woken on
 * the title, a new game's hero select or a level loading, it played the game left behind: a host's
 * hero turn with Dungeon.hero already gone (Comfy.check) and mobs whose sprites went with the scene
 * (Mob.act), both crashes on the actor thread. It takes no turn until a game scene is up.
 */
public class TurnLoopOffSceneTest {

	private Thread loop;
	private Object savedThread;
	private Hero savedHero;
	private boolean savedSwitching, savedRunning;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		Actor.clear();
		savedHero = Dungeon.hero;
		Dungeon.hero = null;
		savedThread = field( GameScene.class, "actorThread" ).get( null );
		savedRunning = field( GameScene.class, "running" ).getBoolean( null );
		savedSwitching = field( Game.class, "requestedReset" ).getBoolean( Game.instance );
		//a game, no scene being switched to: only the scene being up or not tells the loop apart
		field( Game.class, "requestedReset" ).setBoolean( Game.instance, false );
		field( GameScene.class, "running" ).setBoolean( null, false );
	}

	@After
	public void tearDown() throws Exception {
		if (loop != null){
			Actor.keepActorThreadAlive = false;
			//an interrupt can land before the loop is back in its wait: it is sent until it is out
			for (int i = 0; i < 20 && loop.isAlive(); i++){
				loop.interrupt();
				loop.join( 100 );
			}
		}
		Actor.keepActorThreadAlive = true;
		Actor.clear();
		Dungeon.hero = savedHero;
		field( GameScene.class, "actorThread" ).set( null, savedThread );
		field( GameScene.class, "running" ).setBoolean( null, savedRunning );
		field( Game.class, "requestedReset" ).setBoolean( Game.instance, savedSwitching );
	}

	private static Field field( Class<?> c, String name ) throws Exception {
		Field f = c.getDeclaredField( name );
		f.setAccessible( true );
		return f;
	}

	@Test
	public void aWokenLoopTakesNoTurnWithNoGameSceneUp() throws Exception {
		final AtomicInteger turns = new AtomicInteger();
		Actor.add( new Actor(){
			@Override
			protected boolean act(){
				turns.incrementAndGet();
				spend( TICK );
				return true;
			}
		} );

		//the loop GameScene.update starts, left waiting once its scene is gone
		Actor.keepActorThreadAlive = true;
		loop = new Thread( Actor::process );
		loop.setDaemon( true );
		field( GameScene.class, "actorThread" ).set( null, loop );
		loop.start();

		//the network's wakes, as a co-op player's input or leaving sends them
		for (int i = 0; i < 10; i++){
			NetManager.wakeActorThread();
			Thread.sleep( 20 );
		}
		assertEquals( "no turn with no game scene up", 0, turns.get() );

		//a game scene up again: the same wake plays the turn
		field( GameScene.class, "running" ).setBoolean( null, true );
		long until = System.currentTimeMillis() + 3000;
		while (turns.get() == 0 && System.currentTimeMillis() < until){
			NetManager.wakeActorThread();
			Thread.sleep( 20 );
		}
		assertTrue( "a turn once a game scene is up", turns.get() > 0 );
	}
}
