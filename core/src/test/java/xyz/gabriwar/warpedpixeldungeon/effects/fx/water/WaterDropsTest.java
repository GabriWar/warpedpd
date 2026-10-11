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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * What drops into the water: a roof's drop beads, falls and crowns the water exactly where it was
 * aimed; an item rings it and stirs it as food; a lunge flings its droplets toward what it bit;
 * a blink rings it; the lava takes none of the droplets.
 */
public class WaterDropsTest {

	static final int W = WaterLiveTest.W;

	private float savedElapsed;
	private Group[] layers;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedElapsed = Game.elapsed;
		Emitter.freezeEmitters = false;
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		WaterFX.sceneCreated();
		WaterLiveTest.fullTier();
		FxRing.reset();
		WaterLive.sceneCreated();
		FxRandom.seedForTests( 9 );
		Dungeon.level = WaterLiveTest.pond( Assets.Environment.WATER_SEWERS );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 10 + 3 * W;
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		WaterFX.sceneCreated();
		FxRing.reset();
		WaterLive.sceneCreated();
		FxBudget.reset();
		Game.elapsed = savedElapsed;
		Dungeon.level = null;
		Dungeon.hero = null;
	}

	private int living( GameScene.Layer layer, Class<?> c ){
		int n = 0;
		for (Gizmo g : layers[layer.ordinal()].membersView()){
			if (g != null && g.alive && c.isInstance( g )) n++;
			if (g instanceof Group) for (Gizmo k : ((Group) g).membersView()) if (k != null && k.alive && c.isInstance( k )) n++;
		}
		return n;
	}

	private int rings(){
		return living( GameScene.Layer.SURFACE, Ripple.class );
	}

	private void frame(){
		FxBudget.frame( 1 / 60f );
		WaterFX.frame();
		for (Group g : layers) g.update();
	}

	@Test
	public void aRoofsDropBeadsFallsAndCrownsTheWaterWhereItWasAimed(){
		float x = 10 * 16 + 8, y = 6 * 16 + 9;
		Emitter e = new Emitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		e.pos( x, y );
		e.burst( DripParticle.FALL, 1 );
		frame();
		//the bead on the roof, high over its point
		WaterDrops.Bead bead = null;
		for (Gizmo g : e.membersView()) if (g instanceof WaterDrops.Bead && g.alive) bead = (WaterDrops.Bead) g;
		assertTrue( bead != null );
		assertTrue( "on the roof: " + bead.py, bead.py <= y - WaterDrops.fallHeight( 0.45f ) + 0.5f );
		assertEquals( x, bead.px, 0.5f );
		float t = 0;
		while (rings() == 0 && t < 1.5f){
			frame();
			t += Game.elapsed;
		}
		//0.2 s gathering, 0.45-0.6 falling
		assertTrue( "landed at " + t, t >= 0.6f && t <= 0.85f );
		assertEquals( "a middling ring and its second", 2, rings() );
		boolean crowned = false;
		for (Gizmo g : layers[GameScene.Layer.EFFECTS.ordinal()].membersView()){
			if (g instanceof Group) for (Gizmo k : ((Group) g).membersView()){
				if (k instanceof FxParticle && k.alive && ((FxParticle) k).height >= 4) crowned = true;
			}
		}
		assertTrue( "a crown thrown up", crowned );
	}

	@Test
	public void anItemRingsTheWaterStartlesFishRightByItAndDrawsTheRest(){
		float x = 9 * 16 + 8, y = 6 * 16 + 10;
		WaterFX.frame();
		WaterDrops.item( x, y );
		assertEquals( 2, rings() );
		assertTrue( FxBudget.live( FxBudget.DROPLETS ) >= 2 );
		WaterFX.frame();
		float[] out = new float[WaterFX.BUS * 4];
		int n = WaterFX.disturbances( out );
		boolean food = false, near = false, wide = false;
		for (int i = 0; i < n; i++){
			if (out[i * 4] != x) continue;
			if (out[i * 4 + 3] == 1f) food = true;
			else if (out[i * 4 + 2] <= WaterDrops.ITEM_STARTLES) near = true;
			else wide = true;
		}
		assertTrue( food );
		//a fish right by it is startled; one 24-40 px off only comes to look
		assertTrue( near );
		assertFalse( wide );
	}

	@Test
	public void aLungeFlingsItsDropletsTowardWhatItBit(){
		int fish = 8 + 6 * W, bitten = 7 + 6 * W;
		WaterDrops.lunge( fish, bitten );
		assertEquals( 2, rings() );
		int west = 0, east = 0;
		for (Gizmo g : layers[GameScene.Layer.EFFECTS.ordinal()].membersView()){
			if (g instanceof Group) for (Gizmo k : ((Group) g).membersView()){
				if (k instanceof WaterFX.Droplet && k.alive){
					if (((FxParticle) k).vx < 0) west++;
					else east++;
				}
			}
		}
		assertTrue( west + east >= 1 );
		assertTrue( "toward the bitten cell, west: " + west + " " + east, west >= east );
	}

	@Test
	public void aBlinkRingsTheWaterAndTheLavaTakesNoDroplets(){
		WaterDrops.arrive( 9 + 6 * W );
		assertEquals( 2, rings() );
		assertTrue( FxBudget.live( FxBudget.DROPLETS ) >= 1 );
		//on the lava: the ring alone
		Dungeon.level = WaterLiveTest.pond( Assets.Environment.WATER_HALLS );
		WaterFX.sceneCreated();
		FxBudget.reset();
		WaterDrops.arrive( 9 + 6 * W );
		assertEquals( 0, FxBudget.live( FxBudget.DROPLETS ) );
		//off the water: nothing
		WaterFX.frame();
		int before = rings();
		WaterDrops.arrive( 1 + 1 * W );
		assertEquals( before, rings() );
		assertFalse( WaterFX.onWater( 24, 24 ) );
	}

	@Test
	public void aThousandDropsNeverKeepMoreThanFortyRingsAndReuseThem(){
		int most = 0;
		for (int i = 0; i < 1000; i++){
			//a cloudburst: twenty a frame, every size, all over the pool
			float x = (6 + FxRandom.Int( 10 )) * 16 + FxRandom.Float( 16f ), y = (4 + FxRandom.Int( 6 )) * 16 + FxRandom.Float( 16f );
			WaterFX.drop( x, y, FxRandom.Float(), xyz.gabriwar.warpedpixeldungeon.effects.fx.Element.NONE );
			if (i % 20 == 19) frame();
			most = Math.max( most, rings() );
		}
		assertTrue( "rings at once: " + most, most > 0 && most <= FxBudget.ringCap() );
		//pooled: the surface never held many more ring objects than the cap
		int made = 0;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()) if (g instanceof Ripple) made++;
		assertTrue( "ring objects: " + made, made <= FxBudget.ringCap() + 2 * WaterFX.RINGS_A_FRAME );
	}
}
