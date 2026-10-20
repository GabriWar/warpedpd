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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.PixelParticle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The weather's drifting specks go the way the wind blows (Fx.driftX): falling leaves, dust, ash
 * and fireflies drift east in an east wind as fast as they ever did, and in a west wind as fast
 * west, the mirror of it (the leaves' and the dust's gusts pushing them on downwind too); under a
 * roof, where the sky's wind is nothing, they keep the draught east they always had.
 */
public class AmbientWindTest {

	private static final float DT = 1f / 60f;
	private static final int N = 2000;

	private Level level;
	private float wind, dir, elapsed, time;
	private GL20 gl, gl20;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		level = Dungeon.level;
		wind = ClimateManager.debugWindOverride;
		dir = ClimateManager.debugWindDirOverride;
		elapsed = Game.elapsed;
		time = Game.timeTotal;
		gl = Gdx.gl;
		gl20 = Gdx.gl20;
		//the weather's sheet is painted through GL the first time a particle asks for it: a GL that
		//does nothing (as the nature tests' NatureTestBase.stubGl)
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
		//the gusting at the clock's start: a third of a swell, the same for every wind below
		Game.timeTotal = 0f;
	}

	@After
	public void restore(){
		Dungeon.level = level;
		ClimateManager.debugWindOverride = wind;
		ClimateManager.debugWindDirOverride = dir;
		Game.elapsed = elapsed;
		Game.timeTotal = time;
		Gdx.gl = gl;
		Gdx.gl20 = gl20;
		Fx.frame();
	}

	//a wind of 8 m/s blowing toward `dir` degrees, under the surface's open sky or under a roof
	private static void sky( boolean open, float dir ){
		Dungeon.level = open ? new OverworldLevel() : null;
		ClimateManager.debugWindOverride = 8f;
		ClimateManager.debugWindDirOverride = dir;
		Fx.frame();
	}

	private interface Kind {
		PixelParticle make();
	}

	//the mean of N fresh ones' speed across the screen, after `frames` of their own
	private static float meanSpeedX( Kind kind, int frames ){
		Game.elapsed = DT;
		float sum = 0f;
		for (int i = 0; i < N; i++){
			PixelParticle p = kind.make();
			for (int f = 0; f < frames; f++) p.update();
			sum += p.speed.x;
			p.kill();
		}
		return sum / N;
	}

	private static final Kind LEAF = () -> {
		FallingLeafParticle p = new FallingLeafParticle();
		p.leafColor( 0x5A9E3A );
		p.reset( 100f, 100f );
		return p;
	};

	private static final Kind DUST = () -> {
		DustParticle p = new DustParticle();
		p.reset( 100f, 100f );
		return p;
	};

	private static final Kind ASH = () -> {
		AshParticle p = new AshParticle();
		p.reset( 100f, 100f );
		return p;
	};

	private static final Kind FIREFLY = () -> {
		FireflyParticle p = new FireflyParticle();
		p.reset( 100f, 100f );
		return p;
	};

	//east in an east wind, west in a west wind, the mirror of it; east under a roof whichever way it blows
	private static void followsTheWind( String what, Kind kind, int frames, float least ){
		sky( true, 90f );
		float east = meanSpeedX( kind, frames );
		sky( true, 270f );
		float west = meanSpeedX( kind, frames );
		assertTrue( what + " in an east wind: " + east, east > least );
		assertTrue( what + " in a west wind: " + west, west < -least );
		assertEquals( what + ": the mirror of it", 0f, east + west, least * 0.4f );
		sky( false, 270f );
		float roof = meanSpeedX( kind, frames );
		assertTrue( what + " under a roof: " + roof, roof > least );
	}

	@Test
	public void aFallingLeafRidesTheWindItsWay(){
		followsTheWind( "a leaf as it falls", LEAF, 0, 4f );
		//and its swing, the gust's share with it
		followsTheWind( "a leaf swinging", LEAF, 1, 4f );
	}

	@Test
	public void dustSkatesTheWayTheWindBlows(){
		followsTheWind( "dust", DUST, 0, 6f );
		followsTheWind( "dust a second on", DUST, 60, 6f );
	}

	@Test
	public void ashDriftsTheWayTheWindBlows(){
		followsTheWind( "ash", ASH, 1, 3f );
	}

	@Test
	public void aFireflyLeansIntoTheWindItsWay(){
		followsTheWind( "a firefly", FIREFLY, 1, 1f );
	}

	@Test
	public void underAnOpenSkyTheDriftIsTheWindsWithoutItsLean(){
		sky( true, 90f );
		assertEquals( Fx.windX() / 1.2f, Fx.driftX(), 0.0001f );
		sky( true, 0f );
		assertEquals( "a north wind blows nothing across the screen", 0f, Fx.driftX(), 0.0001f );
		sky( false, 270f );
		assertEquals( 0f, Fx.windX(), 0f );
		assertEquals( "under a roof the draught east", ClimateManager.localWindSpeed(), Fx.driftX(), 0f );
	}
}
