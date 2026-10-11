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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BatSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.particles.Emitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Walking in the water headless (stand-in layers, a GPU that answers yes): a wader stepping in
 * throws up a crown, droplets and a ring, a wake follows one through it facing its way, one
 * stepping out sheds droplets; a flier makes nothing; the eight waders nearest the hero get a
 * waterline and the rest none; one standing rings the water now and then; grass is flicked back
 * by a footstep; and a gallery's puppet (a dummy on no level) wades as a real one does.
 */
public class WaterWalkTest {

	static final int W = WaterLiveTest.W;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed, savedTime;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group stage;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedCam = Camera.main;
		savedElapsed = Game.elapsed;
		savedTime = Game.timeTotal;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//a GPU that answers yes: shaders compile and link, so a waterline can be drawn
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			String n = m.getName();
			if ((n.equals( "glGetShaderiv" ) || n.equals( "glGetProgramiv" )) && a[2] instanceof IntBuffer){
				((IntBuffer) a[2]).put( 0, 1 );
				return null;
			}
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			if (r == String.class) return "";
			return null;
		} );
		Camera.main = new Camera( 0, 0, 480, 320, 1 );
		Camera.main.fullScreen = true;
		Emitter.freezeEmitters = false;
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.sceneCreated();
		WaterFX.sceneCreated();
		WaterLiveTest.fullTier();
		FxRing.reset();
		WaterLive.sceneCreated();
		FxRandom.seedForTests( 21 );
		Dungeon.level = WaterLiveTest.pond( Assets.Environment.WATER_SEWERS );
		//grass to the west of the pool's bank
		for (int y = 4; y < 10; y++) Dungeon.level.map[3 + y * W] = Terrain.GRASS;
		Dungeon.level.buildFlagMaps();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 5 + 6 * W;
		stage = new Group();
		Game.elapsed = 1 / 60f;
		Game.timeTotal = 5f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		WaterLive.planeForTests = null;
		WaterFX.sceneCreated();
		FxRing.reset();
		WaterLive.sceneCreated();
		FxBudget.reset();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Game.timeTotal = savedTime;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	//a sprite on the stage linked by hand to a character on no level, as the gallery's puppets are
	private CharSprite sprite( CharSprite s, Mob ch, int cell ){
		ch.state = ch.PASSIVE;
		ch.pos = cell;
		s.ch = ch;
		ch.sprite = s;
		stage.add( s );
		s.place( cell );
		s.visible = true;
		return s;
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

	private void frames( int n ){
		for (int i = 0; i < n; i++){
			Game.timeTotal += Game.elapsed;
			FxBudget.frame( 1 / 60f );
			WaterFX.frame();
			for (Group g : layers) g.update();
			stage.update();
		}
	}

	@Test
	public void aWaderSteppingInThrowsUpACrownDropletsAndARing(){
		CharSprite s = sprite( new RatSprite(), new Rat(), at( 5, 5 ) );
		s.ch.pos = at( 6, 5 );
		s.move( at( 5, 5 ), at( 6, 5 ) );
		assertEquals( "a middling ring and its second", 2, rings() );
		assertTrue( "droplets: " + FxBudget.live( FxBudget.DROPLETS ), FxBudget.live( FxBudget.DROPLETS ) >= 3 );
		//the crown and the droplets in the air over everyone
		assertTrue( living( GameScene.Layer.EFFECTS, FxParticle.class ) >= 4 );
		//the ring waits 0.06 s, then spreads; the droplets land and ring the water
		int most = 0;
		for (int i = 0; i < 40; i++){
			frames( 1 );
			most = Math.max( most, rings() );
		}
		assertTrue( "the droplets' rings too: " + most, most >= 3 );
		frames( 60 );
		assertEquals( "all gone in a second and a half", 0, rings() );
	}

	@Test
	public void aWakeFollowsAWaderThroughTheWaterFacingItsWay(){
		CharSprite s = sprite( new RatSprite(), new Rat(), at( 9, 6 ) );
		s.ch.pos = at( 8, 6 );
		s.move( at( 9, 6 ), at( 8, 6 ) );
		assertEquals( 1, Marks.live( Marks.WAKE ) );
		Marks.Mark wake = null;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()){
			if (g instanceof Group) for (Gizmo k : ((Group) g).membersView()) if (k instanceof Marks.Mark && k.alive) wake = (Marks.Mark) k;
		}
		//heading west: mirrored, behind (east of) its feet
		assertTrue( wake.flipHorizontal );
		assertTrue( wake.px > 8 * 16 + 8 );
		assertEquals( "a small ring where it stepped from", 1, rings() );
		//the wake is gone in 0.6 s
		frames( 40 );
		assertEquals( 0, Marks.live( Marks.WAKE ) );
	}

	@Test
	public void aWaderSteppingOutShedsDropletsAndAFlierMakesNothing(){
		CharSprite s = sprite( new RatSprite(), new Rat(), at( 6, 5 ) );
		s.ch.pos = at( 5, 5 );
		s.move( at( 6, 5 ), at( 5, 5 ) );
		assertEquals( 1, rings() );
		assertTrue( FxBudget.live( FxBudget.DROPLETS ) >= 2 );

		WaterFX.frame();
		int before = rings(), particles = living( GameScene.Layer.EFFECTS, FxParticle.class );
		CharSprite b = sprite( new BatSprite(), new Bat(), at( 7, 6 ) );
		b.ch.pos = at( 8, 6 );
		b.move( at( 7, 6 ), at( 8, 6 ) );
		b.ch.pos = at( 5, 6 );
		b.move( at( 4, 6 ), at( 5, 6 ) );
		assertEquals( before, rings() );
		assertEquals( particles, living( GameScene.Layer.EFFECTS, FxParticle.class ) );
		assertEquals( 0, Marks.live( Marks.WAKE ) );
	}

	@Test
	public void theEightWadersNearestTheHeroGetAWaterlineAndStandersRingTheWater(){
		ArrayList<CharSprite> waders = new ArrayList<>();
		for (int i = 0; i < 12; i++){
			int x = 6 + i % 10, y = 4 + i / 10 * 3;
			waders.add( sprite( new RatSprite(), new Rat(), at( x, y ) ) );
		}
		WaterLive.planeForTests = new SkinnedBlock( W * 16, WaterLiveTest.H * 16, Assets.Environment.WATER_SEWERS );
		WaterLive live = new WaterLive();
		layers[GameScene.Layer.SHEEN.ordinal()].add( live );
		//drawn (their waterlines asked for), then looked over
		for (CharSprite s : waders) WaterWalk.afterDraw( s );
		live.update();
		assertEquals( WaterWalk.WATERLINES, WaterWalk.lined() );
		//the nearest are the ones: the hero stands at column 5, row 6
		for (CharSprite s : waders){
			WaterWalk.Wader w = (WaterWalk.Wader) s.fxSlots[0];
			int x = (int)((s.x + s.width() / 2) / 16);
			boolean near = x <= 11 || s.y > 6 * 16;
			if (w.active) assertTrue( "a far one lined at column " + x, near );
		}
		//drawn again: their pictures made and drawn over them
		for (CharSprite s : waders) WaterWalk.afterDraw( s );
		int drawn = 0;
		for (CharSprite s : waders){
			WaterWalk.Wader w = (WaterWalk.Wader) s.fxSlots[0];
			if (w.band != null) drawn++;
		}
		assertEquals( WaterWalk.WATERLINES, drawn );
		//standing still they ring the water every 1.4 to 2.6 s
		int before = rings();
		for (int i = 0; i < 180; i++){
			for (CharSprite s : waders) WaterWalk.afterDraw( s );
			Game.timeTotal += Game.elapsed;
			FxBudget.frame( 1 / 60f );
			WaterFX.frame();
			live.update();
		}
		assertTrue( rings() > before );
		//a killed one lets its pictures go
		CharSprite gone = null;
		for (CharSprite s : waders) if (((WaterWalk.Wader) s.fxSlots[0]).band != null) gone = s;
		WaterWalk.gone( gone );
		assertEquals( null, gone.fxSlots[0] );
	}

	@Test
	public void aWadersWaterlineStaysOnTheWaterWhileItJumps(){
		CharSprite s = sprite( new RatSprite(), new Rat(), at( 7, 6 ) );
		WaterLive.planeForTests = new SkinnedBlock( W * 16, WaterLiveTest.H * 16, Assets.Environment.WATER_SEWERS );
		WaterLive live = new WaterLive();
		layers[GameScene.Layer.SHEEN.ordinal()].add( live );
		WaterWalk.afterDraw( s );
		live.update();
		WaterWalk.afterDraw( s );
		WaterWalk.Wader w = (WaterWalk.Wader) s.fxSlots[0];
		assertTrue( w.active && w.band != null );
		float groundY = w.band.y;
		//a jump over the pool, 12 px high at its top
		s.ch.pos = at( 13, 6 );
		s.jump( at( 7, 6 ), at( 13, 6 ), 12f, 0.6f, null );
		float y0 = s.y, highest = s.y;
		for (int i = 0; i < 18; i++){
			Game.timeTotal += Game.elapsed;
			stage.update();
			highest = Math.min( highest, s.y );
			WaterWalk.afterDraw( s );
			assertTrue( "the waterline went up with it", w.band.y >= groundY - 1 );
		}
		assertTrue( "it is in the air: " + (y0 - highest), highest < y0 - 8 );
	}

	@Test
	public void aFootstepFlicksTheGrassBack(){
		CharSprite s = sprite( new RatSprite(), new Rat(), at( 3, 5 ) );
		s.ch.pos = at( 4, 5 );
		int before = living( GameScene.Layer.EFFECTS, FxParticle.class );
		s.move( at( 3, 5 ), at( 4, 5 ) );
		int blades = living( GameScene.Layer.EFFECTS, FxParticle.class ) - before;
		assertTrue( "one or two blades: " + blades, blades >= 1 && blades <= 2 );
		//never more than one a 0.2 s
		s.ch.pos = at( 3, 5 );
		s.move( at( 4, 5 ), at( 3, 5 ) );
		s.ch.pos = at( 4, 5 );
		s.move( at( 3, 5 ), at( 4, 5 ) );
		assertEquals( blades, living( GameScene.Layer.EFFECTS, FxParticle.class ) - before );
	}
}
