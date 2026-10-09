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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WeatherParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The fixes the effects kit made to what everyone shares: a speck's factory as light or plain
 * whichever was asked for first; a weather particle out of the hero's sight not drawn at all, and
 * drawn again once he sees it; one gradient texture for every flare.
 */
public class SharedFixesTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aSpecksFactoryIsLightOrPlainAsAskedWhicheverCameFirst(){
		//plain first, then light: each its own
		Emitter.Factory plain = Speck.factory( Speck.STEAM );
		Emitter.Factory light = Speck.factory( Speck.STEAM, true );
		assertNotSame( plain, light );
		assertFalse( plain.lightMode() );
		assertTrue( light.lightMode() );
		//light first, then plain
		Emitter.Factory light2 = Speck.factory( Speck.BUBBLE, true );
		Emitter.Factory plain2 = Speck.factory( Speck.BUBBLE, false );
		assertTrue( light2.lightMode() );
		assertFalse( plain2.lightMode() );
		//the same object every time, and its type found back from it (co-op's recording)
		assertSame( plain, Speck.factory( Speck.STEAM ) );
		assertSame( light, Speck.factory( Speck.STEAM, true ) );
		assertEquals( Speck.STEAM, Speck.typeForFactory( plain ) );
		assertEquals( Speck.STEAM, Speck.typeForFactory( light ) );
		assertEquals( Speck.BUBBLE, Speck.typeForFactory( light2 ) );
		assertEquals( -1, Speck.typeForFactory( FxFactories.steam() ) );
	}

	//a particle of the weather that checks its sight each frame, as they all do
	private static class Drop extends WeatherParticle {
		Drop(){
			frame( WeatherSprites.DROP );
		}

		@Override
		public void update(){
			super.update();
			am = 0.6f;
			fov();
		}
	}

	@Test
	public void aWeatherParticleOutOfSightIsNotDrawnAtAll(){
		GL20 savedGl = Gdx.gl, savedGl20 = Gdx.gl20;
		Level savedLevel = Dungeon.level;
		try {
			//the weather's sheet goes to the GPU when first made: a stand-in for it
			Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
				Class<?> r = m.getReturnType();
				if (r == int.class) return 1;
				if (r == boolean.class) return false;
				if (r == float.class) return 0f;
				return null;
			} );
			Level l = new Level(){
				@Override protected boolean build(){ return true; }
				@Override protected void createMobs(){ }
				@Override protected void createItems(){ }
			};
			l.setSize( 8, 8 );
			l.mobs = new HashSet<>();
			l.heaps = new SparseArray<>();
			l.blobs = new HashMap<>();
			l.plants = new SparseArray<>();
			l.traps = new SparseArray<>();
			Arrays.fill( l.map, Terrain.EMPTY );
			l.buildFlagMaps();
			l.heroFOV = new boolean[l.length()];
			Dungeon.level = l;

			Drop d = new Drop();
			d.reset( 20, 20, 0xFFFFFF, 1, 5f );
			d.update();
			assertFalse( "out of sight: not drawn", d.visible );
			assertEquals( "its own alpha untouched", 0.6f, d.am, 0f );
			Arrays.fill( l.heroFOV, true );
			d.update();
			assertTrue( "seen again: drawn", d.visible );
			//a new life starts seen
			Arrays.fill( l.heroFOV, false );
			d.update();
			assertFalse( d.visible );
			d.kill();
			d.reset( 20, 20, 0xFFFFFF, 1, 5f );
			assertTrue( d.visible );
		} finally {
			Gdx.gl = savedGl;
			Gdx.gl20 = savedGl20;
			Dungeon.level = savedLevel;
		}
	}

	@Test
	public void everyFlareSharesOneGradient() throws Exception {
		java.lang.reflect.Field tex = Flare.class.getDeclaredField( "texture" );
		tex.setAccessible( true );
		Object first = tex.get( new Flare( 6, 16 ) );
		for (int i = 0; i < 100; i++) assertSame( first, tex.get( new Flare( 4 + i % 8, 8 + i ) ) );
		//its edge fades out in white, not cyan
		java.lang.reflect.Field g = Flare.class.getDeclaredField( "GRADIENT" );
		g.setAccessible( true );
		int[] gradient = (int[]) g.get( null );
		for (int c : gradient) assertEquals( 0xFFFFFF, c & 0xFFFFFF );
	}
}
