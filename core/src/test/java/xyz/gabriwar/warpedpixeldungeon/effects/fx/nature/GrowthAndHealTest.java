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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Sunlight;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.UnfilteredSunlight;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.WallOfLight;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.TerrainWatch;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.LotusSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.watabou.noosa.Camera;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Growth, the blobs, the plants, the lotus and healing, headless: grass growing is told to the
 * area once per change, draws nothing of the game's Random and, after a wand's cast, waits by its
 * distance from the caster; the growth's light reveals its tile from the foot up in four steps and
 * trampled grass parts in three; sunlight pours while it lasts with the one draw it always made;
 * a growth blob keeps to its cap; the lotus at +10 pours from 24 cells at most; at most twelve
 * plants breathe; and a healing number, the host's or a guest's replayed one, plays its hook.
 */
public class GrowthAndHealTest extends NatureTestBase {

	private static final int W = NatureTestBase.W;

	// ------------------------------------------------------------------ growth

	@Test
	public void growthIsToldOnceAChangeAndDrawsNoGameRandom(){
		Level l = Dungeon.level;
		TerrainWatch.resync();
		int cell = 5 + 2 * W, trampled = 3 + W;
		assertNoGameRandom( () -> {
			Level.set( cell, Terrain.GRASS );
			TerrainWatch.changed( cell );
			TerrainWatch.changed( cell );
			//high grass trampled: the trample's, not a growth
			Level.set( trampled, Terrain.GRASS );
			TerrainWatch.changed( trampled );
			TerrainWatch.drain();
			frames( 30 );
		} );
		assertEquals( 1, GrowthPulse.played() );
		//said changed again, unchanged: nothing more
		TerrainWatch.changed( cell );
		TerrainWatch.drain();
		assertEquals( 1, GrowthPulse.played() );
		//grass to high grass grows too; high grass to grass does not
		assertTrue( GrowthPulse.grew( Terrain.GRASS, Terrain.HIGH_GRASS ) );
		assertTrue( GrowthPulse.grew( Terrain.EMBERS, Terrain.GRASS ) );
		assertFalse( GrowthPulse.grew( Terrain.HIGH_GRASS, Terrain.GRASS ) );
		assertTrue( Trample.trampled( Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS ) );
		assertEquals( l, Dungeon.level );
	}

	@Test
	public void afterACastTheFarGrowthsWaitByTheirDistance(){
		float saved = Game.timeTotal;
		try {
			Game.timeTotal = 100f;
			int caster = 2 + 2 * W, near = 2 + 2 * W, far = 6 + 2 * W;
			GrowthPulse.waveFrom( caster );
			Level.set( near, Terrain.GRASS );
			Level.set( far, Terrain.GRASS );
			GrowthPulse.changed( near, Terrain.EMPTY, Terrain.GRASS );
			GrowthPulse.changed( far, Terrain.EMPTY, Terrain.GRASS );
			assertEquals( "the caster's own cell at once", 1, GrowthPulse.played() );
			assertEquals( 1, GrowthPulse.waiting() );
			//four cells off: 64 px at 160 px/s, 0.4 s
			Game.timeTotal = 100.3f;
			GrowthPulse.tick();
			assertEquals( 1, GrowthPulse.played() );
			Game.timeTotal = 100.45f;
			GrowthPulse.tick();
			assertEquals( 2, GrowthPulse.played() );
			assertEquals( 0, GrowthPulse.waiting() );

			//never more waiting than its queue
			GrowthPulse.waveFrom( caster );
			for (int i = 0; i < 200; i++) GrowthPulse.changed( 14 + 14 * W, Terrain.EMPTY, Terrain.GRASS );
			assertTrue( GrowthPulse.waiting() <= GrowthPulse.QUEUE );
		} finally {
			Game.timeTotal = saved;
		}
	}

	@Test
	public void theGrowthsLightRisesFromItsFootAndTrampledGrassPartsFromItsTop(){
		Image src = new Image( Assets.Effects.FX_SHEET, 0, 0, 16, 16 );
		final int[] ended = { 0 };
		TileCrop up = new TileCrop( src, 32, 48, TileCrop.REVEAL, 0.35f, true ).then( () -> ended[0]++ );
		Group g = new Group();
		g.add( up );
		Game.elapsed = 0.05f;
		int[] heights = new int[4];
		heights[0] = Math.round( up.height );
		for (int i = 1; i < 4; i++){
			g.update();
			heights[i] = Math.round( up.height );
			//its foot stays on its cell's floor
			assertEquals( 48 + 16, up.y + up.height, 0.001f );
			assertEquals( 1f, up.scale.y, 0f );
		}
		assertEquals( 4, heights[0] );
		assertEquals( 8, heights[1] );
		assertEquals( 12, heights[2] );
		assertEquals( 16, heights[3] );
		for (int i = 0; i < 20; i++) g.update();
		assertEquals( "its end told once", 1, ended[0] );

		TileCrop part = new TileCrop( src, 32, 48, TileCrop.PART, 1f, false );
		g.add( part );
		Game.elapsed = 0.05f;
		assertEquals( 16, Math.round( part.height ) );
		part.update();
		assertEquals( 12, Math.round( part.height ) );
		assertTrue( part.am < 1f );
		part.update();
		assertEquals( 8, Math.round( part.height ) );
		assertEquals( 48 + 16, part.y + part.height, 0.001f );
	}

	// ------------------------------------------------------------------ blobs

	@Test
	public void sunlightPoursWhileItLastsWithTheOneDrawItAlwaysMade(){
		for (Class<? extends Blob> kind : new Class[]{ Sunlight.class, UnfilteredSunlight.class }){
			final Blob sun = Blob.seed( 4 + 4 * W, 30, kind );
			sun.volume = 30;
			Random.pushGenerator( 77 );
			Random.Float();
			float second = Random.Float();
			Random.popGenerator();
			final BlobEmitter[] e = new BlobEmitter[1];
			assertEquals( "one draw of the game's Random, as ever", second,
					nextGameRandom( 77, () -> e[0] = new BlobEmitter( sun ) ), 0f );
			Game.elapsed = 1 / 30f;
			for (int i = 0; i < 300; i++){
				fullBudget();
				e[0].update();
			}
			assertTrue( "still pouring after 10 s", e[0].on );
			Dungeon.level.blobs.remove( kind );
		}
	}

	@Test
	public void aOneUnitWellOrGardenKeepsItsLifeThoughItEmitsOnATickInFive(){
		//the steady blobs are never thinned by their volume as a cloud is
		for (com.watabou.noosa.particles.Emitter.Factory f : new com.watabou.noosa.particles.Emitter.Factory[]{
				BlobFx.WELL, BlobFx.PORTAL, BlobFx.FOLIAGE, BlobFx.SUN, BlobFx.HALLOWED, BlobFx.WALL }){
			assertTrue( f instanceof FxBudget.Prioritized );
			assertEquals( 1f, ((FxBudget.Prioritized) f).density( 1 ), 0f );
		}
		//an emit of a one-unit cell stands for five ticks, of a thick one for one
		BlobEmitter.Cell c = new BlobEmitter.Cell();
		c.cur = 1;
		int sum = 0;
		for (int i = 0; i < 1000; i++) sum += BlobFx.times( c );
		assertEquals( 5f, sum / 1000f, 0.01f );
		c.cur = 2;
		sum = 0;
		for (int i = 0; i < 4000; i++) sum += BlobFx.times( c );
		assertEquals( 2.5f, sum / 4000f, 0.05f );
		c.cur = 9;
		assertEquals( 1, BlobFx.times( c ) );

		//a well of one unit, emitting every two seconds: its glints and its shaft keep coming
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		FxEmitter e = new FxEmitter();
		lights.add( e );
		float saved = Game.timeTotal;
		try {
			Game.elapsed = 1 / 30f;
			int most = 0, shafts = 0;
			for (int f = 0; f < 30 * 12; f++){
				Game.timeTotal = 100 + f / 30f;
				fullBudget();
				if (f % 60 == 0) BlobFx.WELL.emit( e, f, 40, 40 );
				lights.update();
				most = Math.max( most, living( lights, EllipseMote.class ) );
				shafts = Math.max( shafts, living( lights, xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle.class ) );
			}
			assertTrue( "glints at once " + most, most >= 3 && most <= 4 );
			assertTrue( "its shaft", shafts >= 1 );
		} finally {
			Game.timeTotal = saved;
		}
	}

	@Test
	public void aWallOfLightInItsLastTurnsStaysWholeAndGoesWithIt(){
		//a wall three cells tall with one turn left: its emitter skips four ticks in five there
		int[] cells = { 5 + 3 * W, 5 + 4 * W, 5 + 5 * W };
		Blob wall = null;
		for (int c : cells) wall = Blob.seed( c, 1, WallOfLight.LightWall.class );
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		BlobEmitter e = new BlobEmitter( wall );
		lights.add( e );
		float saved = Game.timeTotal;
		try {
			Game.elapsed = 1 / 30f;
			int frames = 0, whole = 0;
			for (int f = 0; f < 30 * 10; f++){
				Game.timeTotal = 200 + f / 30f;
				fullBudget();
				lights.update();
				if (f < 30) continue;
				frames++;
				if (curtains( lights ) >= cells.length) whole++;
			}
			assertTrue( "a curtain over every cell on " + whole + " of " + frames + " frames", whole >= frames * 0.97f );

			//a long frame (a phone stalling): still one curtain a cell, never a second over it
			Game.elapsed = 0.6f;
			Game.timeTotal += 0.6f;
			fullBudget();
			lights.update();
			Game.elapsed = 1 / 30f;
			for (int f = 0; f < 30; f++){
				Game.timeTotal += 1 / 30f;
				fullBudget();
				lights.update();
			}
			assertEquals( cells.length, curtains( lights ) );

			//the wall gone: its curtains with it, within their fade
			for (int c : cells) wall.cur[c] = 0;
			wall.volume = 0;
			for (int f = 0; f < 15; f++){
				Game.timeTotal += 1 / 30f;
				lights.update();
			}
			assertEquals( 0, curtains( lights ) );
		} finally {
			Game.timeTotal = saved;
			Dungeon.level.blobs.remove( WallOfLight.LightWall.class );
		}
	}

	//a wall's curtains shown now (its edge line not counted)
	private static int curtains( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m instanceof BlobFx.WallSheet && m.alive && ((BlobFx.WallSheet) m).visible
					&& !((BlobFx.WallSheet) m).edge() && ((BlobFx.WallSheet) m).am > BlobFx.WALL_SHEET * 0.5f) n++;
			if (m instanceof Group) n += curtains( (Group) m );
		}
		return n;
	}

	@Test
	public void aGrowthBlobKeepsToItsCap(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		float saved = Game.timeTotal;
		try {
			for (int k = 0; k < 4; k++){
				Game.timeTotal = 50 + k;
				com.watabou.noosa.particles.Emitter.Factory f = BlobFx.growth( true );
				for (int i = 0; i < 2000; i++){
					if (i % 40 == 0) fullBudget();
					f.emit( e, i, 20 + i % 30, 20 + i % 20 );
				}
				assertTrue( "alive " + e.countLiving(), e.countLiving() <= BlobFx.GROWTH_CAP );
				for (Gizmo g : e.membersView()) if (g != null) g.kill();
			}
		} finally {
			Game.timeTotal = saved;
		}
	}

	// ------------------------------------------------------------------ plants

	@Test
	public void theLotusAtTenPoursFromTwentyFourCellsAtMost() throws Exception {
		WandOfRegrowth.Lotus lotus = new WandOfRegrowth.Lotus();
		Method set = WandOfRegrowth.Lotus.class.getDeclaredMethod( "setLevel", int.class );
		set.setAccessible( true );
		set.invoke( lotus, 10 );
		lotus.pos = 8 + 8 * W;
		assertEquals( 10, lotus.range() );
		Camera saved = Camera.main;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			Group stage = new Group();
			LotusSprite s = new LotusSprite();
			stage.add( s );
			s.link( lotus );
			assertTrue( "it pours from " + s.grassEmitters(), s.grassEmitters() > 0 && s.grassEmitters() <= LotusSprite.GRASS_EMITTERS );
			s.kill();
			assertEquals( 0, s.grassEmitters() );
		} finally {
			Camera.main = saved;
		}
	}

	// ------------------------------------------------------------------ healing

	@Test
	public void aHealingNumberPlaysItsHookTheHostsAndAGuestsReplayedOne(){
		Application savedApp = Gdx.app;
		final Application app = savedApp;
		//the render thread's queue run at once
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				(p, m, a) -> {
					if (m.getName().equals( "postRunnable" )){
						((Runnable) a[0]).run();
						return null;
					}
					return m.invoke( app, a );
				} );
		try {
			HealFx.reset();
			int cell = 2 + 2 * W;
			//showStatusWithIcon's call and NetVisuals' replay both come through here
			FloatingText.show( 40, 30, cell, "12", 0x00FF00, FloatingText.HEALING, true );
			assertEquals( 1, HealFx.heals() );
			FloatingText.show( 40, 30, cell, "5", 0x00FF00, FloatingText.SHIELDING, true );
			assertEquals( 1, HealFx.shields() );
			FloatingText.show( 40, 30, cell, "-3", 0xFF0000, FloatingText.PHYS_DMG, true );
			assertEquals( 1, HealFx.heals() );
			assertEquals( 1, HealFx.shields() );
			//its ring on the floor and its motes in the light
			assertTrue( living( layers[GameScene.Layer.FLOOR.ordinal()], Gizmo.class ) >= 1 );
		} finally {
			Gdx.app = savedApp;
		}
	}
}
