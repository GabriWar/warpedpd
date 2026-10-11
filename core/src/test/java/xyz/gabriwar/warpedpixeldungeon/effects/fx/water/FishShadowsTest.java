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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The fish shadows of the open water: up to three come on the shore's water once there is water
 * enough in view, none where the water is not theirs, none with too little in view; they keep to
 * open water; a stir within 48 px scatters one and it is gone, and another comes back later.
 */
public class FishShadowsTest {

	private static final int W = 30;

	private float savedElapsed, savedTime;
	private Camera savedCam;
	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedElapsed = Game.elapsed;
		savedTime = Game.timeTotal;
		savedCam = Camera.main;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Camera.main = null;
		Emitter.freezeEmitters = false;
		WaterFX.sceneCreated();
		WaterLive.sceneCreated();
		FxRandom.seedForTests( 17 );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 1 + W;
		Game.elapsed = 1 / 30f;
		Game.timeTotal = 1f;
	}

	@After
	public void tearDown(){
		WaterFX.sceneCreated();
		WaterLive.sceneCreated();
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Game.timeTotal = savedTime;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a lake on the shore (or on a place without shadows): water in columns 4-25, rows 4-25
	private static Level lake( final Place place ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public Place ambience(){ return place; }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int y = 4; y < 26; y++) for (int x = 4; x < 26; x++) l.map[x + y * W] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		return l;
	}

	private static void run( FishShadows s, float seconds ){
		for (float t = 0; t < seconds; t += Game.elapsed){
			Game.timeTotal += Game.elapsed;
			WaterFX.frame();
			s.update();
		}
	}

	private static FishShadows.Shadow first( FishShadows s ){
		for (Gizmo g : s.membersView()) if (g instanceof FishShadows.Shadow && g.alive) return (FishShadows.Shadow) g;
		return null;
	}

	@Test
	public void upToThreeComeOnTheShoresOpenWaterAndKeepToIt(){
		Dungeon.level = lake( Place.SHORE );
		WaterAmbience.seen = 200;
		FishShadows s = new FishShadows();
		int most = 0;
		for (int i = 0; i < 600; i++){
			run( s, 1 / 30f );
			most = Math.max( most, s.swimming() );
			for (Gizmo g : s.membersView()){
				if (!(g instanceof FishShadows.Shadow) || !g.alive) continue;
				FishShadows.Shadow f = (FishShadows.Shadow) g;
				int cell = (int)(f.px / 16) + (int)(f.py / 16) * W;
				assertTrue( "on the water at " + f.px + "," + f.py, Dungeon.level.water[cell] );
			}
		}
		assertTrue( "some came", most >= 1 );
		assertTrue( "three at most: " + most, most <= FishShadows.MOST );
	}

	@Test
	public void noneWithoutWaterEnoughInViewOrWhereTheWaterIsNotTheirs(){
		Dungeon.level = lake( Place.SHORE );
		WaterAmbience.seen = 8;
		FishShadows s = new FishShadows();
		run( s, 10f );
		assertEquals( 0, s.swimming() );
		Dungeon.level = lake( Place.SEWERS );
		WaterAmbience.seen = 200;
		s = new FishShadows();
		run( s, 10f );
		assertEquals( 0, s.swimming() );
	}

	@Test
	public void aStirWithin48PxScattersOneAndAnotherComesLater(){
		Dungeon.level = lake( Place.SHORE );
		WaterAmbience.seen = 200;
		FishShadows s = new FishShadows();
		run( s, 4f );
		FishShadows.Shadow f = first( s );
		assertTrue( f != null );
		//a stone 40 px off: it is gone inside 0.6 s
		WaterFX.disturb( f.px + 40f, f.py, 24f, false );
		run( s, 0.7f );
		assertFalse( f.alive );
		//and another comes 4 to 8 s later
		int before = s.swimming();
		run( s, 9f );
		assertTrue( s.swimming() >= before );
	}

	@Test
	public void outOfTheHerosSightTheyLeaveAndTheyNeverLingerFarBehindHim(){
		Dungeon.level = lake( Place.SHORE );
		WaterAmbience.seen = 200;
		Dungeon.hero.pos = 6 + 6 * W;
		FishShadows s = new FishShadows();
		run( s, 12f );
		assertTrue( s.swimming() >= 1 );
		//all of them within reach of him
		for (Gizmo g : s.membersView()){
			if (!(g instanceof FishShadows.Shadow) || !g.alive) continue;
			assertFalse( FishShadows.farFromHero( Dungeon.level, ((FishShadows.Shadow) g).px, ((FishShadows.Shadow) g).py ) );
		}
		//he walks off down the shore: the ones behind slip away, the new ones come near him
		Dungeon.hero.pos = 24 + 24 * W;
		run( s, 1.5f );
		for (Gizmo g : s.membersView()){
			if (!(g instanceof FishShadows.Shadow) || !g.alive) continue;
			FishShadows.Shadow f = (FishShadows.Shadow) g;
			assertFalse( "lingering at " + f.px + "," + f.py, FishShadows.farFromHero( Dungeon.level, f.px, f.py ) );
		}
		run( s, 10f );
		assertTrue( "others came near him", s.swimming() >= 1 );
		//the water goes out of his view: gone inside 2 s and their fade, and none come
		Arrays.fill( Dungeon.level.heroFOV, false );
		run( s, FishShadows.UNSEEN_FOR + 1.2f );
		assertEquals( 0, s.swimming() );
		run( s, 10f );
		assertEquals( 0, s.swimming() );
	}
}
