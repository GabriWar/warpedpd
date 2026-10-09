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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Every lightning in the game has the storm's look (Lightning.Arc), drawn as light: a bright core
 * on a soft halo along each leg. An arc of a cell or a diagonal is the old two halves meeting at a
 * middle; a longer one (from 30 px) a zigzag of a joint every 16 px thrown wider the longer it is
 * up to the storm bolt's own at 60 px (half of it at least), forked from 40 px, with twigs from 40
 * px and a bloom and a glow at its ends. It strikes, dips, strikes again and fades in its 0.3 s,
 * its channel formed anew as it re-strikes. Built anywhere, on the actor thread too, without
 * touching the GPU (its glows are made on the first frame); as long-lived as ever, its callback as
 * late, and destroyed once done.
 */
public class LightningTest {

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Random.pushGenerator( 1234 );
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//textures without a GPU
	private static void stubGl(){
		gl( (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
	}

	private static void gl( java.lang.reflect.InvocationHandler h ){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, h );
	}

	private static PointF down( float length ){
		return new PointF( 40f, 30f + length );
	}

	private static final PointF TOP = new PointF( 40f, 30f );

	//how far a point lies across the line from a to b, and along it from where it would be
	//evenly spaced as the i-th of n
	private static float[] offset( PointF p, PointF a, PointF b, int i, int n ){
		float dx = b.x - a.x, dy = b.y - a.y, len = (float)Math.sqrt( dx * dx + dy * dy );
		float ux = dx / len, uy = dy / len;
		float bx = a.x + dx * i / n, by = a.y + dy * i / n;
		float ox = p.x - bx, oy = p.y - by;
		return new float[]{ ox * uy - oy * ux, ox * ux + oy * uy };
	}

	//where a line image's line starts and ends: its origin (its middle row) is laid on the line
	private static PointF start( Image img ){
		return new PointF( img.x + img.origin.x, img.y + img.origin.y );
	}

	private static PointF end( Image img ){
		double r = Math.toRadians( img.angle );
		float len = img.scale.x * img.width;
		PointF s = start( img );
		return new PointF( s.x + (float)Math.cos( r ) * len, s.y + (float)Math.sin( r ) * len );
	}

	//the arcs' expected numbers by length: one leg under 30 px, else a joint every 16 px (two to
	//eight legs); thrown by its length over 60 px, half of it at least
	private static int joints( float len ){
		return len < 30f ? 1 : Math.max( 2, Math.min( 8, Math.round( len / 16f ) ) );
	}

	private static float k( float len ){
		return Math.max( 0.5f, Math.min( 1f, len / 60f ) );
	}

	@Test
	public void anArcOfACellIsTheOldTwoHalves(){
		for (PointF to : new PointF[]{ new PointF( 56f, 30f ), new PointF( 56f, 46f ) }){
			PointF from = new PointF( 40f, 30f );
			Lightning.Arc arc = new Lightning.Arc( from, to );
			assertEquals( 2, arc.path.length );
			assertNull( arc.fork );
			assertFalse( "a hop strikes nothing", arc.glows );
			assertEquals( 2, arc.membersView().size() );
			Image a = (Image) arc.membersView().get( 0 ), b = (Image) arc.membersView().get( 1 );
			//the first from where it starts, the second from its middle thrown up to 4 px each way,
			//on to where it ends: each drawn along its middle row, laid on the line
			assertEquals( from.x, start( a ).x, 1e-4f );
			assertEquals( from.y, start( a ).y, 1e-4f );
			PointF mid = start( b );
			assertTrue( Math.abs( mid.x - (from.x + to.x) / 2 ) <= 4f && Math.abs( mid.y - (from.y + to.y) / 2 ) <= 4f );
			assertEquals( to.x, end( b ).x, 1e-3f );
			assertEquals( to.y, end( b ).y, 1e-3f );
			assertEquals( mid.x, end( a ).x, 1e-3f );
			assertEquals( mid.y, end( a ).y, 1e-3f );
			assertEquals( 0f, a.origin.x, 0f );
			assertEquals( a.height / 2f, a.origin.y, 0f );
		}
	}

	@Test
	public void aLongArcIsAChannelOfJoints(){
		for (int t = 0; t < 200; t++){
			for (PointF[] ends : new PointF[][]{
					{ TOP, down( 160f ) },
					{ TOP, down( 48f ) },
					{ new PointF( 10f, 10f ), new PointF( 120f, 90f ) },
					{ new PointF( 200f, 50f ), new PointF( 20f, 60f ) } }){
				PointF from = ends[0], to = ends[1];
				float len = PointF.distance( from, to );
				Lightning.Arc arc = new Lightning.Arc( from, to );
				int n = arc.path.length - 1;
				assertEquals( joints( len ), n );
				assertSame( from, arc.path[0] );
				assertSame( "ends exactly where it strikes", to, arc.path[n] );
				float k = k( len );
				for (int i = 1; i < n; i++){
					float[] o = offset( arc.path[i], from, to, i, n );
					assertTrue( "across " + o[0], Math.abs( o[0] ) <= 12f * k + 1e-3f );
					assertTrue( "along " + o[1], Math.abs( o[1] ) <= 4f * k + 1e-3f );
				}
			}
		}
	}

	@Test
	public void longerArcsAreThrownWiderUpToTheStormsAtSixtyPixels(){
		float[] lengths = { 30f, 45f, 160f };
		float[] widest = new float[lengths.length];
		for (int t = 0; t < 400; t++){
			for (int l = 0; l < lengths.length; l++){
				Lightning.Arc arc = new Lightning.Arc( TOP, down( lengths[l] ) );
				int n = arc.path.length - 1;
				for (int i = 1; i < n; i++){
					widest[l] = Math.max( widest[l], Math.abs( offset( arc.path[i], TOP, down( lengths[l] ), i, n )[0] ) );
				}
			}
		}
		//by its length up to the storm's, 12 px each way, from 60 px on; never under half of it
		assertTrue( "two cells less a bit: " + widest[0], widest[0] <= 6f + 1e-3f && widest[0] > 5f );
		assertTrue( "three: " + widest[1], widest[1] <= 9f + 1e-3f && widest[1] > 7.5f );
		assertTrue( "ten: " + widest[2], widest[2] <= 12f + 1e-3f && widest[2] > 10f );
	}

	@Test
	public void itForksSevenTimesInTenFromFortyPixelsShortOfWhereItStrikes(){
		int forks = 0, runs = 1000;
		for (int t = 0; t < runs; t++){
			Lightning.Arc arc = new Lightning.Arc( TOP, down( 160f ) );
			if (arc.fork != null){
				forks++;
				//off one of its upper joints, its two legs going on the way it goes
				assertEquals( 3, arc.fork.length );
				assertSame( arc.path[3], arc.fork[0] );
				assertTrue( arc.fork[1].y > arc.fork[0].y && arc.fork[2].y > arc.fork[1].y );
				float out = Math.abs( arc.fork[2].x - arc.fork[0].x );
				assertTrue( "out " + out, out >= 16f - 1e-3f && out <= 32f + 1e-3f );
			}
			assertNull( "never under 40 px", new Lightning.Arc( TOP, down( 39f ) ).fork );
			//a short one's fork runs on no further than 0.6 of the way left to where it strikes
			Lightning.Arc near = new Lightning.Arc( TOP, down( 48f ) );
			if (near.fork != null){
				PointF at = near.fork[0];
				assertSame( near.path[1], at );
				float on = near.fork[2].y - at.y, room = PointF.distance( at, down( 48f ) );
				assertTrue( "on " + on + " of " + room, on <= 0.6f * room + 1e-3f );
			}
		}
		assertTrue( "forked " + forks + " of " + runs, forks > runs * 0.64f && forks < runs * 0.76f );
		int short40 = 0;
		for (int t = 0; t < runs; t++) if (new Lightning.Arc( TOP, down( 40f ) ).fork != null) short40++;
		assertTrue( "from 40 px: " + short40, short40 > runs * 0.64f && short40 < runs * 0.76f );
	}

	@Test
	public void theStormsChannelIsAsItsNumbersSay(){
		//the overlay's: five joints, its zigzag 0.4 as wide (a storm cloud's under a roof), no fork,
		//its glow its own, and no twigs: the storm's shape
		stubGl();
		for (int t = 0; t < 100; t++){
			PointF to = down( 45f );
			Lightning.Arc arc = new Lightning.Arc( TOP, to, 5, 0.4f, 0f, false );
			new Lightning( Arrays.asList( arc ), null );
			assertEquals( 6, arc.path.length );
			assertNull( arc.fork );
			assertNull( arc.twigs );
			assertFalse( arc.glows );
			for (int i = 1; i < 5; i++){
				float[] o = offset( arc.path[i], TOP, to, i, 5 );
				assertTrue( Math.abs( o[0] ) <= 12f * 0.4f + 1e-3f && Math.abs( o[1] ) <= 4f * 0.4f + 1e-3f );
			}
		}
	}

	@Test
	public void itTwigsFromFortyPixelsOffItsJoints(){
		stubGl();
		int[] counts = new int[4];
		for (int t = 0; t < 300; t++){
			PointF to = new PointF( 140f, 70f );
			Lightning.Arc arc = new Lightning.Arc( TOP, to );
			new Lightning( Arrays.asList( arc ), null );
			assertNotNull( arc.twigs );
			counts[arc.twigs.length]++;
			float way = (float)Math.atan2( to.y - TOP.y, to.x - TOP.x );
			for (PointF[] twig : arc.twigs){
				//off a joint between its ends, 5-12 px in all, 25-70 degrees off its way
				boolean onJoint = false;
				for (int j = 1; j + 1 < arc.path.length; j++) onJoint |= twig[0] == arc.path[j];
				assertTrue( "off a joint", onJoint );
				float len = PointF.distance( twig[0], twig[1] ) + PointF.distance( twig[1], twig[2] );
				assertTrue( "length " + len, len >= 5f - 1e-3f && len <= 12f + 1e-3f );
				double off = Math.abs( Math.toDegrees( Math.atan2( twig[1].y - twig[0].y, twig[1].x - twig[0].x ) - way ) );
				off = Math.min( off, 360 - off );
				assertTrue( "off its way " + off, off >= 25 - 1e-2 && off <= 70 + 1e-2 );
			}
			//none under 40 px
			Lightning.Arc shorter = new Lightning.Arc( TOP, down( 39f ) );
			new Lightning( Arrays.asList( shorter ), null );
			assertNull( shorter.twigs );
		}
		assertEquals( "one to three", 0, counts[0] );
		for (int c = 1; c <= 3; c++) assertTrue( c + " twigs: " + counts[c], counts[c] > 60 );
	}

	@Test
	public void anArcOfNoLengthIsSafe(){
		stubGl();
		PointF p = new PointF( 40f, 40f );
		Lightning l = new Lightning( p, new PointF( p.x, p.y ), null );
		Group stage = new Group();
		stage.add( l );
		Game.elapsed = 0.05f;
		stage.update();
		Lightning.Arc arc = (Lightning.Arc) l.membersView().get( 0 );
		assertEquals( 2, arc.path.length );
		assertNull( arc.fork );
		assertNull( arc.glow );
		assertNull( arc.source );
		//its two halves and its halo
		assertEquals( 3, arc.membersView().size() );
		for (Gizmo g : arc.membersView()) assertFalse( Float.isNaN( ((Image) g).scale.x ) );
	}

	@Test
	public void itIsBuiltWithoutTheGpuAndGlowsOnItsFirstFrame(){
		//any GL call made while it is put together fails the test: it may be built on the
		//actor thread, where one crashed the game
		gl( (p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
		//24.1: a cell's diagonal hop from a golem (19 px high) down to a rat (15), centre to centre
		float[] lengths = { 0f, 10f, 16f, 22.6f, 23.9f, 24f, 24.1f, 29.9f, 30f, 32f, 45f, 50f, 60f, 64f, 110f, 160f, 400f };
		Lightning[] bolts = new Lightning[lengths.length];
		for (int i = 0; i < lengths.length; i++){
			bolts[i] = new Lightning( TOP, down( lengths[i] ), null );
			Lightning.Arc arc = (Lightning.Arc) bolts[i].membersView().get( 0 );
			assertNull( arc.glow );
			assertNull( arc.source );
		}
		Lightning crossed = new Lightning( TOP, down( 45f ), null ).noGlow();

		//its first frame, on the render thread
		stubGl();
		Game.elapsed = 0.05f;
		Group stage = new Group();
		for (Lightning l : bolts) stage.add( l );
		stage.add( crossed );
		stage.update();
		for (int i = 0; i < lengths.length; i++){
			Lightning.Arc arc = (Lightning.Arc) bolts[i].membersView().get( 0 );
			//only where it zigzags: one of a single leg is the old arc, with no glow at either end
			boolean strikes = lengths[i] >= 30f;
			assertEquals( "strikes at " + lengths[i], strikes, arc.glows );
			assertEquals( "glow at " + lengths[i], strikes, arc.glow != null );
			assertEquals( "source at " + lengths[i], strikes, arc.source != null );
			assertEquals( "one leg at " + lengths[i], lengths[i] < 30f, arc.path.length == 2 );
			//two halves to a leg, a halo along each, two lines to a twig, and its glows
			int legs = arc.path.length - 1 + (arc.fork == null ? 0 : 2);
			int twigs = arc.twigs == null ? 0 : arc.twigs.length;
			assertEquals( 3 * legs + 2 * twigs + (strikes ? 2 : 0), arc.membersView().size() );
			for (Gizmo g : arc.membersView()) assertTrue( g instanceof Image );
			if (strikes){
				//on where it strikes, as wide as the storm's (30 px) from 60 px, 15 px at least
				Image glow = arc.glow;
				PointF to = down( lengths[i] );
				assertEquals( to.x, glow.x + glow.origin.x, 1e-3f );
				assertEquals( to.y, glow.y + glow.origin.y, 1e-3f );
				float wide = glow.width * glow.scale.x;
				assertEquals( lengths[i] >= 60f ? 30f : Math.max( 15f, 30f * lengths[i] / 60f ), wide, 1e-3f );
				//and a small one where it sets off
				assertEquals( TOP.x, arc.source.x + arc.source.origin.x, 1e-3f );
				assertEquals( TOP.y, arc.source.y + arc.source.origin.y, 1e-3f );
				assertEquals( 11f, arc.source.width * arc.source.scale.x, 1e-3f );
				assertTrue( arc.source.am < arc.glow.am );
			}
		}
		Lightning.Arc cross = (Lightning.Arc) crossed.membersView().get( 0 );
		assertNull( cross.glow );
		assertNull( cross.source );
		assertEquals( 0, cross.sparks );
	}

	@Test
	public void aChainsArcsShareTheirGlows(){
		//from the caster to one and on from him to the next: a glow where it sets off, a bloom on
		//each struck, none twice in a place
		stubGl();
		PointF caster = new PointF( 20f, 40f ), a = new PointF( 80f, 40f ), b = new PointF( 80f, 100f );
		Lightning.Arc first = new Lightning.Arc( caster, a ), second = new Lightning.Arc( a, b ),
				again = new Lightning.Arc( caster, new PointF( 20f, 100f ) );
		Lightning l = new Lightning( Arrays.asList( first, second, again ), null );
		Group stage = new Group();
		stage.add( l );
		Game.elapsed = 0.05f;
		stage.update();
		assertNotNull( first.glow );
		assertNotNull( second.glow );
		assertNotNull( again.glow );
		assertNotNull( "the caster's", first.source );
		assertNull( "where the first struck: its bloom", second.source );
		assertNull( "the caster's already", again.source );
	}

	@Test
	public void itStrikesDipsStrikesAgainAndFades(){
		//the strike, the dip, the re-strike with the flash's second pulse, the last waver, the end
		assertEquals( 1f, Lightning.flicker( 0f ), 1e-6f );
		assertEquals( 0.25f, Lightning.flicker( 0.08f ), 1e-6f );
		assertEquals( 1f, Lightning.flicker( 0.14f ), 1e-6f );
		assertEquals( 0.4f, Lightning.flicker( 0.2f ), 1e-6f );
		assertEquals( 0f, Lightning.flicker( 0.3f ), 1e-6f );
		assertEquals( 0f, Lightning.flicker( 0.5f ), 0f );
		//dark in the dip, bright either side of it
		for (float t = 0.07f; t <= 0.09f; t += 0.005f) assertTrue( "dip at " + t, Lightning.flicker( t ) < 0.45f );
		assertTrue( Lightning.flicker( 0.02f ) > 0.9f && Lightning.flicker( 0.135f ) > 0.9f );
		//fading down from the last waver
		float last = 1f;
		for (float t = 0.221f; t < 0.3f; t += 0.01f){
			float b = Lightning.flicker( t );
			assertTrue( b < last );
			last = b;
		}
		//its halo steadier: lingering through the dip, as bright at the strike, out at the end
		assertEquals( 1f, Lightning.halo( 0f ), 1e-6f );
		assertEquals( 0f, Lightning.halo( 0.3f ), 1e-6f );
		for (float t = 0.06f; t <= 0.1f; t += 0.01f) assertTrue( "at " + t, Lightning.halo( t ) > Lightning.flicker( t ) );
		assertTrue( Lightning.halo( 0.08f ) > Lightning.flicker( 0.08f ) + 0.2f );
	}

	@Test
	public void itLastsAsLongAndCallsBackOnceAsLate(){
		stubGl();
		final int[] calls = { 0 };
		Lightning l = new Lightning( TOP, down( 64f ), () -> calls[0]++ );
		Lightning.Arc arc = (Lightning.Arc) l.membersView().get( 0 );
		Group stage = new Group();
		stage.add( l );

		Game.elapsed = 0.1f;
		stage.update();
		Image half = (Image) arc.membersView().get( 0 );
		assertEquals( Lightning.flicker( 0.1f ), half.am, 1e-3f );
		assertNotNull( arc.glow );
		assertEquals( 0.9f * Lightning.flicker( 0.1f ), arc.glow.am, 1e-3f );
		Game.elapsed = 0.19f;
		stage.update();
		assertEquals( 0, calls[0] );
		assertEquals( Lightning.flicker( 0.29f ), half.am, 1e-3f );
		assertEquals( 0.9f * Lightning.flicker( 0.29f ), arc.glow.am, 1e-3f );

		Game.elapsed = 0.02f;
		stage.update();
		assertEquals( 1, calls[0] );
		assertFalse( l.alive );
		stage.update();
		stage.update();
		assertEquals( 1, calls[0] );
	}

	@Test
	public void itsChannelFormsAnewAsItReStrikesBetweenTheSameEnds(){
		stubGl();
		int seconds = 0;
		for (int t = 0; t < 100; t++){
			Lightning l = new Lightning( TOP, down( 160f ), null );
			Lightning.Arc arc = (Lightning.Arc) l.membersView().get( 0 );
			Group stage = new Group();
			stage.add( l );
			Game.elapsed = 0.02f;
			int formed = 0;
			PointF[] path = arc.path;
			for (int f = 0; f < 15; f++){
				stage.update();
				if (arc.path != path){
					formed++;
					float at = (f + 1) * 0.02f;
					//at the dip, or at the last waver: the first frame past either
					assertTrue( "formed at " + at, Math.abs( at - 0.09f ) <= 0.011f || Math.abs( at - 0.21f ) <= 0.011f );
					assertSame( TOP, arc.path[0] );
					assertEquals( down( 160f ).y, arc.path[arc.path.length - 1].y, 0f );
					assertEquals( path.length, arc.path.length );
					path = arc.path;
				}
			}
			assertTrue( "once or twice: " + formed, formed == 1 || formed == 2 );
			if (formed == 2) seconds++;
		}
		assertTrue( "a second time about half the time: " + seconds, seconds > 30 && seconds < 70 );
	}

	@Test
	public void itsMiddlesShimmerAPixelAFrame(){
		//the halves' meeting point moves no more than the jitter from frame to frame, and stays
		//within 4 px of the leg's middle
		stubGl();
		Lightning l = new Lightning( TOP, new PointF( 56f, 30f ), null );
		Lightning.Arc arc = (Lightning.Arc) l.membersView().get( 0 );
		Group stage = new Group();
		stage.add( l );
		Game.elapsed = 0.01f;
		stage.update();
		Image b = (Image) arc.membersView().get( 1 );
		PointF was = start( b );
		for (int f = 0; f < 6; f++){
			stage.update();
			PointF now = start( b );
			assertTrue( Math.abs( now.x - was.x ) <= 2f + 1e-3f && Math.abs( now.y - was.y ) <= 2f + 1e-3f );
			assertTrue( Math.abs( now.x - 48f ) <= 4f && Math.abs( now.y - 30f ) <= 4f );
			was = now;
		}
	}

	@Test
	public void aRunOverTheWaterIsAFaintCrackleLitAsTheRunGetsThere(){
		stubGl();
		PointF a = new PointF( 40f, 40f ), b = new PointF( 56f, 56f );
		Lightning.Arc step = new Lightning.Arc( a, b ).crawl( 2 );
		Lightning.Arc main = new Lightning.Arc( new PointF( 0f, 40f ), a );
		assertFalse( step.glows );
		assertEquals( 1, step.sparks );
		assertEquals( 2 * Lightning.Arc.STEP_LAG, step.lag, 1e-6f );
		Lightning l = new Lightning( Arrays.asList( main, step ), null );
		Group stage = new Group();
		stage.add( l );
		Image core = (Image) step.membersView().get( 0 );
		Image mainCore = (Image) main.membersView().get( 0 );
		assertTrue( "thinner", core.scale.y < mainCore.scale.y );
		//dark till the run gets there, then a flash of its own over the dip
		Game.elapsed = 0.03f;
		stage.update();
		assertEquals( 0f, core.am, 0f );
		stage.update();
		assertTrue( core.am > 0f );
		assertTrue( "flashing as it lights", core.am > Lightning.flicker( 0.06f ) * 0.8f );
		assertNull( step.glow );
		assertNull( step.source );
	}

	@Test
	public void aLightningLaysItsHalosAndTwigsOnItsFirstLegsOnly(){
		//an arc to every char on the level: past RICH_LEGS legs the rest are the bare stroke
		stubGl();
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		for (int i = 0; i < 20; i++) arcs.add( new Lightning.Arc( TOP, new PointF( 40f + 300f, 30f + i * 20f ) ) );
		new Lightning( arcs, null );
		int legs = 0, rich = 0;
		for (Lightning.Arc arc : arcs){
			legs += arc.legs;
			int bare = 2 * arc.legs;
			boolean laid = arc.membersView().size() > bare;
			if (laid) rich++;
			assertEquals( "laid only within the first " + Lightning.RICH_LEGS + " legs", legs <= Lightning.RICH_LEGS, laid );
		}
		assertTrue( rich > 0 && rich < arcs.size() );
	}

	@Test
	public void aFinishedLightningLetsGoOfItsImages(){
		//each image drawn holds a vertex buffer until it is destroyed (or the scene changes): one
		//only killed and erased kept them all, and the overworld's scene can stay up for hours
		stubGl();
		Lightning l = new Lightning( TOP, down( 160f ), null );
		Lightning.Arc arc = (Lightning.Arc) l.membersView().get( 0 );
		Group stage = new Group();
		stage.add( l );
		Game.elapsed = 0.1f;
		stage.update();
		java.util.List<Gizmo> images = arc.membersView();
		assertNotNull( arc.glow );
		assertTrue( images.contains( arc.glow ) );
		assertTrue( images.contains( arc.source ) );

		Game.elapsed = 0.25f;
		stage.update();
		assertFalse( l.alive );
		assertEquals( "off the stage", -1, stage.indexOf( l ) );
		assertEquals( "destroyed", 0, l.length );
		assertEquals( 0, arc.length );
		assertNull( arc.parent );
		for (Gizmo g : images) assertNull( "every line, halo and glow destroyed", g.parent );
	}

	// ------------------------------------------------------------ where it strikes in sight

	private static final int W = 12;

	private static Level floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		return l;
	}

	@Test
	public void itStrikesInSightWhereTheNearestStrikeTheHeroSeesLands(){
		Level l = floor();
		Dungeon.level = l;
		Hero hero = new Hero();
		hero.pos = 1 + 6 * W;
		int near = 4 + 6 * W, far = 9 + 6 * W, hop = 2 + 6 * W;
		List<Lightning.Arc> arcs = Arrays.asList(
				new Lightning.Arc( hero.pos, far ),
				new Lightning.Arc( hero.pos, near ),
				//a hop of a cell strikes nothing
				new Lightning.Arc( hero.pos, hop ) );
		assertEquals( "nothing seen", -1, Lightning.struckInSight( arcs, l, hero ) );
		l.heroFOV[far] = l.heroFOV[hop] = true;
		assertEquals( far, Lightning.struckInSight( arcs, l, hero ) );
		l.heroFOV[near] = true;
		assertEquals( "the nearest", near, Lightning.struckInSight( arcs, l, hero ) );
		//a cross through a cell strikes nothing
		Lightning.Arc cross = new Lightning.Arc( near - 1, near + 1 );
		cross.noGlow();
		assertEquals( -1, Lightning.struckInSight( Arrays.asList( cross ), l, hero ) );
		//nor one that ends off the map
		Lightning.Arc off = new Lightning.Arc( DungeonTilemap.tileCenterToWorld( near ), new PointF( W * 16f + 40f, 20f ) );
		assertEquals( -1, Lightning.struckInSight( Arrays.asList( off ), l, hero ) );
		assertEquals( -1, Lightning.struckInSight( arcs, null, hero ) );
	}
}
