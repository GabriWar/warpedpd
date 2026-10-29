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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//the render thread updating a layer (holding its group) asks a pool for a picture: a goo drop
//landing lays its decal (FxDecal.at), while the actor thread, inside the scene's own lock, waits
//on that same layer for an emitter (a goo bursting through a door, Door.enter). Neither may wait
//on the other for good
public class PoolLockTest {

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
	}

	@Test
	public void aPoolAskedFromInsideALayerNeverWaitsOnTheScene() throws Exception {
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		final Group effects = layers[GameScene.Layer.EFFECTS.ordinal()];

		final CountDownLatch inLayer = new CountDownLatch( 1 ), inScene = new CountDownLatch( 1 );
		final CountDownLatch done = new CountDownLatch( 2 );

		Thread render = new Thread( () -> {
			synchronized (effects) {
				inLayer.countDown();
				try { inScene.await(); } catch (InterruptedException e){ return; }
				GameScene.recycle( GameScene.Layer.LIGHTS, Gizmo.class );
			}
			done.countDown();
		} );
		Thread actor = new Thread( () -> {
			try { inLayer.await(); } catch (InterruptedException e){ return; }
			synchronized (GameScene.class) {
				inScene.countDown();
				GameScene.recycle( GameScene.Layer.EFFECTS, Gizmo.class );
			}
			done.countDown();
		} );
		render.setDaemon( true );
		actor.setDaemon( true );
		render.start();
		actor.start();

		assertTrue( "both threads go on", done.await( 5, TimeUnit.SECONDS ) );
		assertFalse( render.isAlive() && actor.isAlive() );
	}
}
