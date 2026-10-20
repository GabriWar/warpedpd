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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The wind in the world's trees (WindSway): how hard and which way it sways them, a tile stepping
 * through its sway without ever flickering, how a swaying tile is cut, and the dressing handing its
 * trees to the wind while it keeps drawing the rest - unmoved by a window slide.
 */
public class WindSwayTest {

	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT, S = DungeonTilemap.SIZE;

	@BeforeClass
	public static void boot() throws Exception {
		WindowSlideTest.boot();
	}

	@AfterClass
	public static void unboot(){
		WindowSlideTest.unboot();
	}

	private Level savedLevel;
	private float savedElapsed, savedTotal;

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedElapsed = Game.elapsed;
		savedTotal = Game.timeTotal;
	}

	@After
	public void tearDown(){
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugWindDirOverride = Float.NaN;
		ClimateManager.debugForceStorm = false;
		Emitter.freezeEmitters = false;
		//a still frame, so nothing is left swaying for whatever runs next
		Dungeon.level = null;
		Fx.frame();
		WindSway.frame();
		Dungeon.level = savedLevel;
		Game.elapsed = savedElapsed;
		Game.timeTotal = savedTotal;
	}

	// ------------------------------------------------------------------ the wind

	@Test
	public void stillInACalmAPixelInABreezeTwoInAGale(){
		assertEquals( 0, WindSway.strengthFor( 0, 1.5f, false ) );
		assertEquals( 1, WindSway.strengthFor( 0, 3f, false ) );
		assertEquals( 2, WindSway.strengthFor( 0, 12f, false ) );
		assertEquals( 2, WindSway.strengthFor( 0, 0f, true ) );
		assertEquals( 0, WindSway.strengthFor( 2, 0.5f, false ) );
	}

	@Test
	public void aWindHoveringOnAThresholdKeepsItsStrength(){
		//up only a little past 2 m/s, back down only a little short of it
		assertEquals( 0, WindSway.strengthFor( 0, 2.3f, false ) );
		assertEquals( 1, WindSway.strengthFor( 0, 2.5f, false ) );
		assertEquals( 1, WindSway.strengthFor( 1, 1.7f, false ) );
		assertEquals( 0, WindSway.strengthFor( 1, 1.5f, false ) );
		//and the same about 8
		assertEquals( 1, WindSway.strengthFor( 1, 8.3f, false ) );
		assertEquals( 2, WindSway.strengthFor( 1, 8.5f, false ) );
		assertEquals( 2, WindSway.strengthFor( 2, 7.7f, false ) );
		assertEquals( 1, WindSway.strengthFor( 2, 7.5f, false ) );
	}

	@Test
	public void theWavesRunAlongTheNearestCompassStepAndHoldIt(){
		assertEquals( 0, WindSway.stepFor( -1, 0f ) );
		assertEquals( 2, WindSway.stepFor( -1, 90f ) );
		assertEquals( 4, WindSway.stepFor( -1, 180f ) );
		assertEquals( 6, WindSway.stepFor( -1, 270f ) );
		assertEquals( 1, WindSway.stepFor( -1, 50f ) );
		assertEquals( 0, WindSway.stepFor( -1, 355f ) );
		//east until the wind is well into south-east, north until it is well into north-west
		assertEquals( 2, WindSway.stepFor( 2, 115f ) );
		assertEquals( 3, WindSway.stepFor( 2, 130f ) );
		assertEquals( 0, WindSway.stepFor( 0, 335f ) );
		assertEquals( 7, WindSway.stepFor( 0, 320f ) );
	}

	// ------------------------------------------------------------------ the sway

	@Test
	public void aBreezeSwaysAPixelEitherWayAndAGaleLeansDownwind(){
		TreeSet<Integer> breeze = new TreeSet<>(), east = new TreeSet<>(), west = new TreeSet<>(), north = new TreeSet<>();
		for (int wy = -40; wy < 40; wy++){
			for (int wx = -40; wx < 40; wx++){
				for (double t = 0; t < 2 * Math.PI; t += 0.37){
					assertEquals( 0, WindSway.offset( 0, 1, 0, t, wx, wy ) );
					breeze.add( WindSway.offset( 1, 1, 0, t, wx, wy ) );
					east.add( WindSway.offset( 2, 1, 0, t, wx, wy ) );
					west.add( WindSway.offset( 2, -1, 1, t, wx, wy ) );
					north.add( WindSway.offset( 2, 0, -1, t, wx, wy ) );
				}
			}
		}
		assertEquals( new TreeSet<>( Arrays.asList( -1, 0, 1 ) ), breeze );
		assertEquals( new TreeSet<>( Arrays.asList( 0, 1, 2 ) ), east );
		assertEquals( new TreeSet<>( Arrays.asList( -2, -1, 0 ) ), west );
		assertEquals( new TreeSet<>( Arrays.asList( -2, -1, 0, 1, 2 ) ), north );
	}

	@Test
	public void theCrestsRunDownwind(){
		//where the cells leaning east sit along an east wind's wavelength, as an angle round it: as the
		//waves run on, that place moves east by as much as they ran
		double before = crest( 0.0 ), after = crest( 0.3 );
		assertEquals( 0.3, Math.atan2( Math.sin( after - before ), Math.cos( after - before ) ), 0.1 );
	}

	//the mean place, as an angle round one wavelength, of the cells of 400 rows leaning east at this travel
	private static double crest( double travel ){
		double sx = 0, sy = 0;
		for (int wy = 0; wy < 400; wy++){
			for (int wx = 0; wx < WindSway.WAVE; wx++){
				if (WindSway.offset( 1, 1, 0, travel, wx, wy ) != 1) continue;
				double a = 2 * Math.PI * wx / WindSway.WAVE;
				sx += Math.cos( a );
				sy += Math.sin( a );
			}
		}
		return Math.atan2( sy, sx );
	}

	@Test
	public void aTileStepsThroughItsSwayInTurn(){
		//as the waves run on, a tile moves a pixel at a time, half a radian of their run apart at least,
		//and turns back only after holding its furthest for well over a radian: it never flickers
		for (int strength = 1; strength <= 2; strength++){
			for (int dx = -1; dx <= 1; dx++){
				for (int wx = 0; wx < 30; wx++){
					int last = WindSway.offset( strength, dx, 1, 0, wx, 7 ), dir = 0;
					double changed = Double.NaN;
					for (double t = 0.001; t < 4 * Math.PI; t += 0.001){
						int s = WindSway.offset( strength, dx, 1, t, wx, 7 );
						if (s == last) continue;
						assertEquals( 1, Math.abs( s - last ) );
						int d = Integer.signum( s - last );
						if (!Double.isNaN( changed )){
							assertTrue( t - changed >= 0.5 );
							if (d != dir) assertTrue( t - changed >= 1.4 );
						}
						dir = d;
						last = s;
						changed = t;
					}
				}
			}
		}
	}

	@Test
	public void underTheOpenSkyATileStepsAFewTimesASecond(){
		Dungeon.level = new OverworldLevel( 0 );
		//a breeze, a gale across the screen (leaning) and one up it (two pixels either way)
		for (float[] wind : new float[][]{ { 5f, 90f }, { 12f, 270f }, { 12f, 180f } }){
			float steps = stepsASecond( wind[0], wind[1] );
			assertTrue( "wind " + Arrays.toString( wind ) + ": " + steps, steps >= 1.5f && steps <= 5f );
		}
	}

	//how often a tile's offset changes in this wind, a second, over twenty seconds of frames: never on
	//two frames running, and a pixel at a time
	private static float stepsASecond( float wind, float dir ){
		ClimateManager.debugWindOverride = wind;
		ClimateManager.debugWindDirOverride = dir;
		run( 60 );
		int[] last = new int[20], since = new int[20];
		for (int i = 0; i < last.length; i++) last[i] = WindSway.offsetAt( 1000 + 3 * i, 2000 + i );
		//when each last changed before these frames is not known
		Arrays.fill( since, 1000 );
		int changes = 0;
		for (int f = 0; f < 60 * 20; f++){
			run( 1 );
			for (int i = 0; i < last.length; i++){
				int s = WindSway.offsetAt( 1000 + 3 * i, 2000 + i );
				since[i]++;
				if (s != last[i]){
					//a twentieth of a second apart at least, three frames
					assertTrue( since[i] >= 3 );
					assertEquals( 1, Math.abs( s - last[i] ) );
					changes++;
					since[i] = 0;
				}
				last[i] = s;
			}
		}
		return changes / (20f * last.length);
	}

	@Test
	public void nothingSwaysInACalmUnderTheRockOrWhileTimeStands(){
		ClimateManager.debugWindOverride = 1f;
		assertStill( new OverworldLevel( 0 ) );
		//the caves have no sky
		ClimateManager.debugWindOverride = 12f;
		assertStill( new OverworldLevel( -1 ) );

		//a gale, and then time stops: every tile holds where it was
		Dungeon.level = new OverworldLevel( 0 );
		run( 30 );
		int[] held = new int[20];
		for (int i = 0; i < held.length; i++) held[i] = WindSway.offsetAt( 500 + i, 700 );
		Emitter.freezeEmitters = true;
		run( 120 );
		for (int i = 0; i < held.length; i++) assertEquals( held[i], WindSway.offsetAt( 500 + i, 700 ) );
	}

	private void assertStill( Level level ){
		Dungeon.level = level;
		for (int f = 0; f < 120; f++){
			run( 1 );
			for (int i = 0; i < 20; i++) assertEquals( 0, WindSway.offsetAt( 500 + 7 * i, 700 + i ) );
		}
	}

	private static void run( int frames ){
		Game.elapsed = 1 / 60f;
		for (int f = 0; f < frames; f++){
			Game.timeTotal += Game.elapsed;
			Fx.frame();
			WindSway.frame();
		}
	}

	// ------------------------------------------------------------------ the cut

	@Test
	public void aSwayingTileIsCutIntoItsMovingTopAndItsStandingFoot(){
		//a frame of a 256x256 sheet
		RectF uv = new RectF( 0.25f, 0.5f, 0.3125f, 0.5625f );
		float[] out = new float[3 * 16];
		for (int rows : new int[]{ 8, 9, 16 }){
			for (int s = -2; s <= 2; s++){
				int n = WindSway.quads( out, 32, 48, uv, rows, s );
				//every row of the tile once, across its whole width, with its own texels
				boolean[] drawn = new boolean[S];
				for (int q = 0; q < n; q++){
					int i = q * 16;
					float x1 = out[i], y1 = out[i+1], x2 = out[i+4], y2 = out[i+9];
					assertEquals( S, x2 - x1, 0f );
					assertEquals( uv.left, out[i+2], 0f );
					assertEquals( uv.right, out[i+6], 0f );
					assertEquals( uv.top + (y1 - 48) / S * (uv.bottom - uv.top), out[i+3], 1e-6f );
					assertEquals( uv.top + (y2 - 48) / S * (uv.bottom - uv.top), out[i+11], 1e-6f );
					for (int r = (int)(y1 - 48); r < (int)(y2 - 48); r++){
						assertFalse( drawn[r] );
						drawn[r] = true;
						//the top s pixels across, at two the band under it one, the foot where it stands
						int want = r >= rows ? 0 : Math.abs( s ) == 2 && r >= rows / 2 ? Integer.signum( s ) : s;
						assertEquals( 32 + want, x1, 0f );
					}
				}
				for (boolean d : drawn) assertTrue( d );
				//still, a tile is one quad, as its tilemap would have drawn it
				if (s == 0) assertEquals( 1, n );
			}
		}
	}

	// ------------------------------------------------------------------ the dressing

	@Test
	public void theDressingHandsItsTreesToTheWindAndASlideMovesNone() throws Exception {
		OverworldLevel level = new OverworldLevel( 0 );
		level.setSize( W, H );
		//far from the town, which is never fogged
		level.worldX = 100000;
		level.worldY = 100000;
		Dungeon.level = level;
		int[] data = new int[W * H];
		Arrays.fill( data, -1 );
		int pine = cell( 50, 60 ), crown = cell( 70, 59 ), trunk = cell( 70, 60 ), rock = cell( 90, 59 ), unseen = cell( 110, 60 );
		data[pine] = OverworldDress.SUMMER_PINES[1];
		data[crown] = OverworldDress.OAK_SUMMER_TOP;
		data[trunk] = OverworldDress.OAK_SUMMER_TRUNK;
		data[rock] = OverworldDress.ROCK_TOP[0];
		data[unseen] = OverworldDress.WINTER_PINES[2];
		for (int c : new int[]{ pine, crown, trunk, rock }) level.visited[c] = true;

		OverworldDress.Layer layer = new OverworldDress.Layer();
		layer.setRect( 0, 0, W, H );
		layer.setData( data );
		Tilemap t = layer.create();

		//its own chunks keep the trunk and the rock; the pine and the crown are the wind's
		Method needs = Tilemap.class.getDeclaredMethod( "needsRender", int.class );
		needs.setAccessible( true );
		assertFalse( (boolean) needs.invoke( t, pine ) );
		assertFalse( (boolean) needs.invoke( t, crown ) );
		assertFalse( (boolean) needs.invoke( t, unseen ) );
		assertTrue( (boolean) needs.invoke( t, trunk ) );
		assertTrue( (boolean) needs.invoke( t, rock ) );
		//a swaying pine is still the picture of its cell (the cell's info window)
		assertNotNull( t.image( 50, 60 ) );

		Field swayF = t.getClass().getDeclaredField( "sway" );
		swayF.setAccessible( true );
		WindSway.Batch batch = (WindSway.Batch) swayF.get( t );
		Field filmF = Tilemap.class.getDeclaredField( "tileset" );
		filmF.setAccessible( true );
		TextureFilm film = (TextureFilm) filmF.get( t );

		//a breeze from the west, run on until the pine leans
		ClimateManager.debugWindOverride = 5f;
		ClimateManager.debugWindDirOverride = 90f;
		int f = 0;
		do {
			run( 1 );
		} while (WindSway.offsetAt( level.worldX + 50, level.worldY + 60 ) == 0 && ++f < 600);
		int s = WindSway.offsetAt( level.worldX + 50, level.worldY + 60 );
		int sc = WindSway.offsetAt( level.worldX + 70, level.worldY + 59 );
		assertNotEquals( 0, s );

		batch.build( level, film, data, W, H, 0, 0, W, H );
		float[] v = vertices( batch );
		//the pine: its top nine rows across, its foot where it stands
		ArrayList<float[]> p = quadsIn( v, 50, 60 );
		assertEquals( 2, p.size() );
		assertQuad( p, 50 * S + s, 60 * S, 60 * S + 9 );
		assertQuad( p, 50 * S, 60 * S + 9, 61 * S );
		//the crown, whole (at two pixels its lower half one)
		ArrayList<float[]> c = quadsIn( v, 70, 59 );
		if (sc == 0 || Math.abs( sc ) == 1){
			assertEquals( 1, c.size() );
			assertQuad( c, 70 * S + sc, 59 * S, 60 * S );
		} else {
			assertEquals( 2, c.size() );
		}
		//the pine nobody has seen stands as it is drawn under the fog; the trunk and the rock are the map's
		ArrayList<float[]> u = quadsIn( v, 110, 60 );
		assertEquals( 1, u.size() );
		assertQuad( u, 110 * S, 60 * S, 61 * S );
		assertEquals( 0, quadsIn( v, 70, 60 ).size() );
		assertEquals( 0, quadsIn( v, 90, 59 ).size() );
		assertEquals( p.size() + c.size() + u.size(), batch.count() );

		//the window slides 32 cells east and the exploration with it: the same pine leans the same
		//pixels, 32 cells further west in the window - on screen, where the camera moved too, nothing moved
		int[] slid = new int[W * H];
		Arrays.fill( slid, -1 );
		Tilemap.shiftCells( data, slid, W, H, 32, 0 );
		level.worldX += 32;
		Arrays.fill( level.visited, false );
		for (int cc : new int[]{ pine, crown, trunk, rock }) level.visited[cc - 32] = true;
		batch.build( level, film, slid, W, H, 0, 0, W, H );
		v = vertices( batch );
		p = quadsIn( v, 18, 60 );
		assertEquals( 2, p.size() );
		assertQuad( p, 18 * S + s, 60 * S, 60 * S + 9 );
		assertQuad( p, 18 * S, 60 * S + 9, 61 * S );
	}

	@Test
	public void theBushesAndTheTallGrassSwayInBothTheirLayersAlike() throws Exception {
		OverworldLevel level = new OverworldLevel( 0 );
		level.setSize( W, H );
		Arrays.fill( level.map, Terrain.GRASS );
		level.worldX = 100000;
		level.worldY = 100000;
		int bush = cell( 40, 40 ), tall = cell( 41, 40 ), flowers = cell( 42, 40 ), plain = cell( 43, 40 );
		level.map[bush] = Terrain.SHRUB;
		level.map[tall] = Terrain.HIGH_GRASS;
		level.map[flowers] = Terrain.FLOWER_PATCH;
		Arrays.fill( level.visited, true );
		Dungeon.level = level;
		DungeonTileSheet.setupVariance( level.length(), 1234L );
		TerrainFeaturesTilemap features = new TerrainFeaturesTilemap( new SparseArray<>(), new SparseArray<>() );
		RaisedTerrainTilemap raised = new RaisedTerrainTilemap();

		//the bush and the tall grass's blades are the wind's, in the layer under everyone and in the
		//tufts over them; the flowers and the short grass stand
		Method needs = Tilemap.class.getDeclaredMethod( "needsRender", int.class );
		needs.setAccessible( true );
		assertFalse( (boolean) needs.invoke( features, bush ) );
		assertFalse( (boolean) needs.invoke( features, tall ) );
		assertTrue( (boolean) needs.invoke( features, flowers ) );
		assertTrue( (boolean) needs.invoke( features, plain ) );
		assertFalse( (boolean) needs.invoke( raised, tall ) );

		//a breeze, run on until the tall grass leans: its blades and its tuft are cut and shifted alike,
		//or the blades under the tuft would show twice
		ClimateManager.debugWindOverride = 5f;
		ClimateManager.debugWindDirOverride = 90f;
		int f = 0;
		do {
			run( 1 );
		} while (WindSway.offsetAt( level.worldX + 41, level.worldY + 40 ) == 0 && ++f < 600);
		int s = WindSway.offsetAt( level.worldX + 41, level.worldY + 40 );
		assertNotEquals( 0, s );
		ArrayList<float[]> blades = built( features, level ), tuft = built( raised, level );
		ArrayList<float[]> b = quadsIn( flat( blades ), 41, 40 ), q = quadsIn( flat( tuft ), 41, 40 );
		assertEquals( 2, b.size() );
		assertQuad( b, 41 * S + s, 40 * S, 40 * S + 8 );
		assertQuad( b, 41 * S, 40 * S + 8, 41 * S );
		assertEquals( 2, q.size() );
		assertQuad( q, 41 * S + s, 40 * S, 40 * S + 8 );
		assertQuad( q, 41 * S, 40 * S + 8, 41 * S );

		//no sky in the caves: their tilemaps draw every tile themselves
		Dungeon.level = new OverworldLevel( -1 );
		Dungeon.level.setSize( W, H );
		System.arraycopy( level.map, 0, Dungeon.level.map, 0, level.length() );
		TerrainFeaturesTilemap below = new TerrainFeaturesTilemap( new SparseArray<>(), new SparseArray<>() );
		assertTrue( (boolean) needs.invoke( below, bush ) );
	}

	//the quads a DungeonTilemap's batch builds over the whole map, each as its 16 floats
	private static ArrayList<float[]> built( Tilemap map, OverworldLevel level ) throws Exception {
		Field swayF = map.getClass().getDeclaredField( "sway" );
		swayF.setAccessible( true );
		WindSway.Batch batch = (WindSway.Batch) swayF.get( map );
		Field filmF = Tilemap.class.getDeclaredField( "tileset" ), dataF = Tilemap.class.getDeclaredField( "data" );
		filmF.setAccessible( true );
		dataF.setAccessible( true );
		batch.build( level, (TextureFilm) filmF.get( map ), (int[]) dataF.get( map ), W, H, 0, 0, W, H );
		float[] v = vertices( batch );
		ArrayList<float[]> out = new ArrayList<>();
		for (int i = 0; i < v.length; i += 16) out.add( Arrays.copyOfRange( v, i, i + 16 ) );
		return out;
	}

	private static float[] flat( ArrayList<float[]> quads ){
		float[] v = new float[quads.size() * 16];
		for (int i = 0; i < quads.size(); i++) System.arraycopy( quads.get( i ), 0, v, i * 16, 16 );
		return v;
	}

	private static int cell( int x, int y ){
		return x + y * W;
	}

	private static float[] vertices( WindSway.Batch batch ){
		float[] v = new float[batch.count() * 16];
		((Buffer) batch.vertices()).position( 0 );
		batch.vertices().get( v );
		((Buffer) batch.vertices()).position( 0 );
		return v;
	}

	//the quads drawn on a cell, as (x, top, bottom): those starting on its rows within two pixels of it
	private static ArrayList<float[]> quadsIn( float[] v, int x, int y ){
		ArrayList<float[]> out = new ArrayList<>();
		for (int i = 0; i < v.length; i += 16){
			if (v[i+1] >= y * S && v[i+1] < (y + 1) * S && Math.abs( v[i] - x * S ) <= 2){
				out.add( new float[]{ v[i], v[i+1], v[i+9] } );
			}
		}
		return out;
	}

	private static void assertQuad( ArrayList<float[]> quads, float x, float top, float bottom ){
		for (float[] q : quads){
			if (q[0] == x && q[1] == top && q[2] == bottom) return;
		}
		StringBuilder got = new StringBuilder();
		for (float[] q : quads) got.append( Arrays.toString( q ) );
		throw new AssertionError( "no quad at x " + x + ", rows " + top + ".." + bottom + "; got " + got );
	}
}
