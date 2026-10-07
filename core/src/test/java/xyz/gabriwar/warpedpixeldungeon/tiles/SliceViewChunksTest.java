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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import com.watabou.noosa.Group;
import com.watabou.noosa.Tilemap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * The view down from a mountain is drawn by chunked tilemaps that a window slide shifts and
 * refills rather than rebuilds: after any run of slides, every chunk must draw exactly the
 * cells its data holds (a chunk left drawing nothing where the data has ground is a black hole
 * in the view, until something rebuilds the whole map).
 */
public class SliceViewChunksTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT, CHUNK = 16;

	@BeforeClass
	public static void boot() throws Exception {
		WindowSlideTest.boot();
	}

	@AfterClass
	public static void unboot(){
		WindowSlideTest.unboot();
	}

	private static Field chunksF, dirtyF, countF, dataF;
	private static Method rebuild;

	//what drawing every chunk does: the dirty ones are rebuilt from the tilemap's data
	private static void drawAll( Tilemap t ) throws Exception {
		Object[] chunks = (Object[]) chunksF.get( t );
		int cols = (W + CHUNK - 1) / CHUNK;
		java.lang.reflect.Method fold = Tilemap.class.getDeclaredMethod( "foldUpdatesIntoChunks" );
		fold.setAccessible( true );
		fold.invoke( t );
		chunks = (Object[]) chunksF.get( t );
		for (int i = 0; i < chunks.length; i++){
			if (dirtyF.getBoolean( chunks[i] )) rebuild.invoke( t, chunks[i], i % cols, i / cols );
		}
	}

	private static void check( String what, Tilemap t, int[] data ) throws Exception {
		Object[] chunks = (Object[]) chunksF.get( t );
		int cols = (W + CHUNK - 1) / CHUNK;
		for (int i = 0; i < chunks.length; i++){
			int x0 = (i % cols) * CHUNK, y0 = (i / cols) * CHUNK, n = 0;
			for (int y = y0; y < Math.min( H, y0 + CHUNK ); y++){
				for (int x = x0; x < Math.min( W, x0 + CHUNK ); x++) if (data[x + y * W] >= 0) n++;
			}
			assertEquals( what + ": chunk " + (i % cols) + "," + (i / cols), n, countF.getInt( chunks[i] ) );
		}
	}

	@Test
	public void slidesKeepEveryChunkTrue() throws Exception {
		chunksF = Tilemap.class.getDeclaredField( "chunks" );
		chunksF.setAccessible( true );
		Class<?> chunk = Class.forName( "com.watabou.noosa.Tilemap$Chunk" );
		dirtyF = chunk.getDeclaredField( "dirty" );
		dirtyF.setAccessible( true );
		countF = chunk.getDeclaredField( "count" );
		countF.setAccessible( true );
		rebuild = Tilemap.class.getDeclaredMethod( "rebuildChunk", chunk, int.class, int.class );
		rebuild.setAccessible( true );

		Group scene = new Group();
		SliceGroundLayer layer = new SliceGroundLayer( 0, SliceGroundLayer.TILES );
		int ox = -1200, oy = -800, alt = 2;
		int[][] moves = { {32,0}, {32,0}, {0,32}, {-32,0}, {0,-32}, {-32,-32}, {64,0}, {32,32}, {0,0}, {-64,32} };

		WindowGenerator.Views v = WindowGenerator.belowLayers( SEED, ox, oy, WindowGenerator.generate( SEED, alt, ox, oy, 0f ) );
		layer.setRect( 0, 0, W, H );
		layer.setData( v.depth[2].tiles );
		Tilemap t = layer.create();
		scene.add( t );
		drawAll( t );
		check( "first", t, v.depth[2].tiles );

		for (int[] m : moves){
			ox += m[0];
			oy += m[1];
			v = WindowGenerator.belowLayers( SEED, ox, oy, WindowGenerator.generate( SEED, alt, ox, oy, 0f ) );
			//OverworldLevel: adoptPrepared (setBelowData), then presentWindowArt (shift, add)
			layer.setRect( 0, 0, W, H );
			layer.setData( v.depth[2].tiles );
			if (m[0] != 0 || m[1] != 0) layer.shiftVisual( m[0], m[1] );
			Tilemap again = layer.create();
			assertEquals( t, again );
			drawAll( t );
			check( "after " + m[0] + "," + m[1] + " to " + ox + "," + oy, t, v.depth[2].tiles );
		}
	}

	//the actor thread can adopt a second slide before the render thread has presented the first
	//(OverworldLevel.shiftWindow waits for its render block only so long): the first block then
	//refills with the second window's data, and the second block's shift must not scramble it
	@Test
	public void twoSlidesBeforeOnePresentation() throws Exception {
		slidesKeepEveryChunkTrue();
		Group scene = new Group();
		SliceGroundLayer layer = new SliceGroundLayer( 0, SliceGroundLayer.TILES );
		int ox = 400, oy = -1600, alt = 3;
		WindowGenerator.Views v0 = WindowGenerator.belowLayers( SEED, ox, oy, WindowGenerator.generate( SEED, alt, ox, oy, 0f ) );
		layer.setRect( 0, 0, W, H );
		layer.setData( v0.depth[0].tiles );
		Tilemap t = layer.create();
		scene.add( t );
		drawAll( t );

		//slide one adopted, then slide two adopted, before either is on screen
		WindowGenerator.Views v1 = WindowGenerator.belowLayers( SEED, ox + 32, oy, WindowGenerator.generate( SEED, alt, ox + 32, oy, 0f ) );
		WindowGenerator.Views v2 = WindowGenerator.belowLayers( SEED, ox + 64, oy, WindowGenerator.generate( SEED, alt, ox + 64, oy, 0f ) );
		layer.setData( v1.depth[0].tiles );
		layer.setData( v2.depth[0].tiles );
		int[] expected = v2.depth[0].tiles.clone();
		//the first render block: its own shift, then the layer as it is now (slide two's)
		layer.shiftVisual( 32, 0 );
		layer.create();
		drawAll( t );
		//the second render block
		layer.shiftVisual( 32, 0 );
		layer.create();
		drawAll( t );

		for (int c = 0; c < expected.length; c++) assertEquals( "the layer's own data at " + c, expected[c], layer.at( c ) );
		check( "two slides, one late presentation", t, expected );
	}
}
