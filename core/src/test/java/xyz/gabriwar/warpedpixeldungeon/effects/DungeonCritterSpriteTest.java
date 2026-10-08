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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Basker;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Beetle;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Fly;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Frog;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Moth;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Mouse;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Percher;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Scuttler;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Snail;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Spider;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite.Strider;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientPlayer;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The dungeon's critters on their own, headless (no GL is touched until a sprite is drawn): a
 * scared roach is gone in a moment, a frog croaks no oftener than it should and dives into the
 * water with a ring - never into ice -, a snail draws in while the hero is beside it, a moth keeps
 * to its light, a spider climbs its thread, a strider never sits on ice, a beetle takes wing, a
 * fly in the web only struggles, and one critter is heard at a time. The sheet they are drawn
 * from is made apart from the code: where it is not in the assets yet, a blank sheet of its size
 * stands in (DungeonCritterFramesTest checks the art).
 */
public class DungeonCritterSpriteTest {

	private static final float DT = 1f / 60f;
	private float elapsed, clock, wind;
	private boolean freeze;
	private Level was;
	private Hero heroWas;

	/** A blank sheet of the critters' size where tools/dungeon_critters_gen.py has not drawn the real one into the assets yet. */
	public static void sheet(){
		if (!Gdx.files.internal( Assets.Effects.DUNGEON_CRITTERS ).exists()){
			TextureCache.create( Assets.Effects.DUNGEON_CRITTERS, 128, 240 );
		}
	}

	/** A bare floor all of one terrain, its flag maps built: what a critter reads under it. */
	public static Level floor( int w, int h, int terrain ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( w, h );
		Arrays.fill( l.map, terrain );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		l.buildFlagMaps();
		return l;
	}

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
		sheet();
	}

	@Before
	public void save(){
		elapsed = Game.elapsed;
		clock = Game.timeTotal;
		wind = ClimateManager.debugWindOverride;
		freeze = Emitter.freezeEmitters;
		was = Dungeon.level;
		heroWas = Dungeon.hero;
		ClimateManager.debugWindOverride = 0f;
		Emitter.freezeEmitters = false;
		//no floor: everything is in sight (the water's tests lay their own pools); and no hero to
		//hear a sound but where a test puts one
		Dungeon.level = null;
		Dungeon.hero = null;
	}

	@After
	public void restore(){
		Game.elapsed = elapsed;
		Game.timeTotal = clock;
		ClimateManager.debugWindOverride = wind;
		Emitter.freezeEmitters = freeze;
		Dungeon.level = was;
		Dungeon.hero = heroWas;
	}

	private static void frame( CritterSprite c, float dt ){
		Game.elapsed = dt;
		c.update();
	}

	private static void run( CritterSprite c, float seconds ){
		for (float t = 0f; t < seconds && c.exists; t += DT) frame( c, DT );
	}

	//frames until it is gone (or `limit` seconds): the seconds it took
	private static float runOut( CritterSprite c, float limit ){
		float t = 0f;
		while (c.exists && t < limit){
			frame( c, DT );
			t += DT;
		}
		return t;
	}

	// ------------------------------------------------------------ the sheet

	@Test
	public void theCreaturesPoseOnlyTheSheetsFrames(){
		int[] frames = DungeonCritterSprite.frames();
		//the docs' table: every row's frames, 62 in all
		assertEquals( 62, frames.length );
		HashSet<Integer> seen = new HashSet<>();
		for (int f : frames){
			assertTrue( "frame " + f + " is off the sheet", f >= 0 && f < 15 * 8 );
			assertTrue( "frame " + f + " twice", seen.add( f ) );
		}
		//the rows the docs give each creature
		assertTrue( seen.contains( DungeonCritterSprite.ROW_FROG * 8 + 5 ) );
		assertTrue( seen.contains( DungeonCritterSprite.ROW_MOUSE * 8 + 5 ) );
		assertTrue( seen.contains( DungeonCritterSprite.ROW_SALAMANDER * 8 + 4 ) );
		assertTrue( seen.contains( DungeonCritterSprite.ROW_EMBER_BEETLE * 8 + 4 ) );
		assertTrue( seen.contains( DungeonCritterSprite.ROW_FLY * 8 + 3 ) );
		assertFalse( seen.contains( DungeonCritterSprite.ROW_SILVERFISH * 8 + 2 ) );
	}

	@Test
	public void everyCreatureIsMadeOutRatherThanPoppingIn(){
		CritterSprite[] all = {
				new Frog( 100f, 100f, 0f, 0f, false ),
				new Scuttler( Scuttler.Look.ROACH, 100f, 100f, null ),
				new Mouse( 100f, 100f, null ),
				new Basker( Basker.Look.LIZARD, 100f, 100f, null, 0f, 0f, false ),
				new Beetle( false, 100f, 100f, null ),
				new Snail( 100f, 100f ),
				new Moth( 100f, 100f, 0f, 0xF2E8C8, true, true ),
				new Spider( 100f, 100f, 19f, 12f ),
				new Fly( 100f, 100f ),
		};
		for (CritterSprite c : all){
			frame( c, DT );
			assertTrue( c.getClass().getSimpleName() + " popped in", c.alpha() < 0.2f );
			run( c, 1.5f );
			assertTrue( c.getClass().getSimpleName() + " gone", c.exists );
			assertEquals( c.getClass().getSimpleName() + " not made out", 1f, c.alpha(), 0.001f );
		}
	}

	@Test
	public void oneScaredBeforeItWasMadeOutWasNeverThere(){
		DungeonCritterSprite[] all = {
				new Frog( 100f, 100f, 116f, 100f, true ),
				new Scuttler( Scuttler.Look.CENTIPEDE, 100f, 100f, null ),
				new Moth( 100f, 100f, 0f, 0xF2E8C8, true, true ),
				new Beetle( true, 100f, 100f, null ),
		};
		for (DungeonCritterSprite c : all){
			c.scare( 90f, 100f );
			assertFalse( c.getClass().getSimpleName() + " still there", c.exists );
		}
	}

	// ------------------------------------------------------------ what scuttles

	@Test
	public void aScaredRoachDashesForTheDarkAndIsGoneInUnderTwoSeconds(){
		for (int n = 0; n < 20; n++){
			Scuttler r = new Scuttler( Scuttler.Look.ROACH, 200f, 196f, null );
			run( r, 1f );
			assertTrue( r.calm() );
			//from the west
			r.scare( 176f, 196f );
			assertFalse( r.calm() );
			float far = r.gx(), t = 0f, lastAlpha = 1f;
			while (r.exists && t < 3f){
				frame( r, DT );
				t += DT;
				if (!r.exists) break;
				far = Math.max( far, r.gx() );
				assertTrue( "it brightened again", r.alpha() <= lastAlpha + 0.001f );
				lastAlpha = r.alpha();
			}
			assertFalse( "still about after " + t + "s", r.exists );
			assertTrue( "took " + t + "s", t < 2f );
			assertTrue( "ran " + (far - 200f) + "px east", far - 200f > 20f );
		}
	}

	@Test
	public void aRoachRunsTheWayItIsGivenAndPottersBetween(){
		final float[] wander = { 230f, 196f };
		final float[] away = { 240f, 196f, 270f, 196f };
		DungeonCritterSprite.Ground ground = new DungeonCritterSprite.Ground(){
			@Override public float[] wander( float x, float y ){ return x < 229f ? wander : null; }
			@Override public float[] flee( float x, float y, float sx, float sy ){ return away; }
		};
		Scuttler r = new Scuttler( Scuttler.Look.ROACH, 200f, 196f, ground );
		//its first rest is five seconds at most, then it runs its little way and is still again
		run( r, 6.5f );
		assertTrue( r.exists );
		assertEquals( 230f, r.ax, 0.001f );
		assertEquals( 196f, r.ay, 0.001f );
		assertTrue( r.calm() );
		r.scare( 200f, 196f );
		float t = 0f, maxX = r.ax;
		while (r.exists && t < 3f){
			frame( r, DT );
			t += DT;
			if (r.exists) maxX = Math.max( maxX, r.ax );
			assertEquals( "it keeps to the wall's foot", 196f, r.ay, 0.001f );
		}
		assertFalse( r.exists );
		//40px at 78px a second
		assertTrue( "took " + t + "s", t <= 40f / 78f + 3 * DT );
		assertTrue( "reached " + maxX, maxX > 260f );
	}

	@Test
	public void aMouseSitsUpWhenTheHeroIsAboutAndRunsWhenHeComes(){
		Mouse m = new Mouse( 200f, 196f, null );
		run( m, 1f );
		m.heed( 50f );
		frame( m, DT );
		assertTrue( "nose up", m.posed( DungeonCritterSprite.ROW_MOUSE, 1 ) );
		m.scare( 220f, 196f );
		assertFalse( m.calm() );
		assertTrue( runOut( m, 3f ) < 2f );
		assertFalse( m.exists );
	}

	// ------------------------------------------------------------ the frog

	@Test
	public void aFrogCroaksNowAndThenButNeverMoreThanOnceInSixSeconds(){
		final ArrayList<Float> croaks = new ArrayList<>();
		final float[] clock = { 0f };
		Frog f = new Frog( 200f, 200f, 216f, 200f, true ){
			@Override protected void croaked(){ croaks.add( clock[0] ); }
		};
		//its while on the bank is forty seconds at the least
		for (; clock[0] < 39f; clock[0] += DT) frame( f, DT );
		assertTrue( f.exists );
		assertTrue( "only " + croaks.size() + " croaks in 39s", croaks.size() >= 3 );
		for (int i = 1; i < croaks.size(); i++){
			assertTrue( "croaked again after " + (croaks.get( i ) - croaks.get( i - 1 )) + "s",
					croaks.get( i ) - croaks.get( i - 1 ) >= 6f );
		}
	}

	@Test
	public void aFrogCrouchedByTheHeroKeepsQuiet(){
		final int[] croaks = { 0 };
		Frog f = new Frog( 200f, 200f, 216f, 200f, true ){
			@Override protected void croaked(){ croaks[0]++; }
		};
		for (float t = 0f; t < 30f; t += DT){
			f.heed( 40f );
			frame( f, DT );
		}
		assertEquals( 0, croaks[0] );
		assertTrue( f.posed( DungeonCritterSprite.ROW_FROG, 2 ) );
	}

	//a floor in sight with a pool over these cells (x0, y0 to x1, y1), of water or, iced over, of ice
	private static Level pool( int x0, int y0, int x1, int y1, int terrain ){
		Level l = floor( 20, 20, Terrain.EMPTY );
		for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) l.map[x + y * 20] = terrain;
		l.buildFlagMaps();
		Arrays.fill( l.heroFOV, true );
		Dungeon.level = l;
		return l;
	}

	@Test
	public void aScaredFrogDivesIntoTheWaterWithARingAndIsGone(){
		pool( 14, 11, 16, 13, Terrain.WATER );
		final ArrayList<float[]> plunges = new ArrayList<>();
		//the water is the cell to the east: it dives into its middle
		Frog f = new Frog( 204f, 200f, 232f, 202f, true ){
			@Override protected void plunge( float x, float y ){ plunges.add( new float[]{ x, y } ); }
		};
		run( f, 1f );
		f.scare( 180f, 200f );
		assertFalse( f.calm() );
		boolean swam = false;
		float t = 0f;
		while (f.exists && t < 3f){
			frame( f, DT );
			t += DT;
			if (f.exists && f.posed( DungeonCritterSprite.ROW_FROG, 5 )) swam = true;
		}
		assertFalse( f.exists );
		assertTrue( "took " + t + "s", t < 2f );
		assertEquals( "one splash", 1, plunges.size() );
		assertEquals( 232f, plunges.get( 0 )[0], 0.01f );
		assertEquals( 202f, plunges.get( 0 )[1], 0.01f );
		assertTrue( "its head was never seen above the water", swam );
	}

	@Test
	public void aFrogWhosePoolHasIcedOverHopsOffOverTheBankInstead(){
		//it sat down by the water; the pool froze while it sat
		Level l = pool( 14, 11, 16, 13, Terrain.WATER );
		final ArrayList<float[]> plunges = new ArrayList<>();
		Frog f = new Frog( 204f, 200f, 232f, 202f, true ){
			@Override protected void plunge( float x, float y ){ plunges.add( new float[]{ x, y } ); }
		};
		run( f, 1f );
		for (int y = 11; y <= 13; y++) for (int x = 14; x <= 16; x++) l.map[x + y * 20] = Terrain.FROZEN_WATER;
		l.buildFlagMaps();
		//from the west: off east in two hops over the ice, never into it
		f.scare( 180f, 200f );
		float t = 0f;
		boolean swam = false;
		while (f.exists && t < 3f){
			frame( f, DT );
			t += DT;
			if (f.exists && f.posed( DungeonCritterSprite.ROW_FROG, 5 )) swam = true;
		}
		assertFalse( f.exists );
		assertTrue( "took " + t + "s", t < 1f );
		assertEquals( "it dived into the ice", 0, plunges.size() );
		assertFalse( "it swam in the ice", swam );
	}

	@Test
	public void aFrogWithNoWaterHopsOffIntoTheDark(){
		Frog f = new Frog( 200f, 200f, 0f, 0f, false );
		run( f, 1f );
		f.scare( 190f, 200f );
		float maxX = f.gx(), t = 0f;
		while (f.exists && t < 2f){
			frame( f, DT );
			t += DT;
			if (f.exists) maxX = Math.max( maxX, f.gx() );
		}
		assertFalse( f.exists );
		assertTrue( "took " + t + "s", t < 1f );
		assertTrue( "hopped " + (maxX - 200f) + "px off", maxX - 200f > 12f );
	}

	// ------------------------------------------------------------ their sounds

	@Test
	public void oneCritterIsHeardAtATimeEachHoldingTheVoiceTillItHasRungOut(){
		pool( 14, 11, 16, 13, Terrain.WATER );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 10 + 10 * 20;
		Sample.INSTANCE.ambientEnable( true );
		float x = 10 * 16 + 24f, y = 10 * 16 + 8f;
		//well past whatever an earlier critter held the voice for
		float t0 = Game.timeTotal + 100f;
		Game.timeTotal = t0;
		assertFalse( DungeonCritterSprite.voiceBusy() );
		//a croak by the hero: the voice is held the whole of the longest take, at the lowest pitch
		//the player may nudge it to, and nothing else is heard over it
		assertTrue( DungeonCritterSprite.voiceAt( AmbientSound.FROG, x, y, 0.7f, 1f ) );
		float rung = AmbientSound.FROG.length / (1f - AmbientPlayer.PITCH_SPREAD);
		Game.timeTotal = t0 + rung - 0.02f;
		assertTrue( DungeonCritterSprite.voiceBusy() );
		assertFalse( "two critters at once", DungeonCritterSprite.voiceAt( AmbientSound.SKITTER, x, y, 0.45f, 1.2f ) );
		Game.timeTotal = t0 + rung + 0.02f;
		assertFalse( DungeonCritterSprite.voiceBusy() );
		//pitched up, a squeak rings out the sooner
		float t1 = Game.timeTotal;
		assertTrue( DungeonCritterSprite.voiceAt( AmbientSound.RAT, x, y, 0.4f, 1.5f ) );
		float squeak = AmbientSound.RAT.length / (1.5f * (1f - AmbientPlayer.PITCH_SPREAD));
		assertTrue( squeak < AmbientSound.RAT.length );
		Game.timeTotal = t1 + squeak - 0.02f;
		assertTrue( DungeonCritterSprite.voiceBusy() );
		Game.timeTotal = t1 + squeak + 0.02f;
		assertFalse( DungeonCritterSprite.voiceBusy() );
		//one too far off to be heard holds nothing
		assertFalse( DungeonCritterSprite.voiceAt( AmbientSound.FROG, x + 20 * 16f, y, 0.7f, 1f ) );
		assertFalse( DungeonCritterSprite.voiceBusy() );
		//nor does a sound late in one floor's hour hush the next floor's: its scene's clock starts
		//again at nought
		assertTrue( DungeonCritterSprite.voiceAt( AmbientSound.FLY, x, y, 0.3f, 1f ) );
		assertTrue( DungeonCritterSprite.voiceBusy() );
		Game.timeTotal = 0f;
		assertFalse( DungeonCritterSprite.voiceBusy() );
	}

	// ------------------------------------------------------------ what basks

	@Test
	public void aSalamanderDivesIntoTheLavaInABurstOfSparks(){
		//the halls' cold lava is water to their map
		pool( 13, 12, 15, 13, Terrain.WATER );
		final ArrayList<float[]> dives = new ArrayList<>();
		Basker s = new Basker( Basker.Look.SALAMANDER, 200f, 200f, null, 222f, 201f, true ){
			@Override protected void slipIn( float x, float y ){ dives.add( new float[]{ x, y } ); }
		};
		run( s, 1f );
		s.scare( 180f, 200f );
		float t = 0f;
		while (s.exists && t < 2f){
			frame( s, DT );
			t += DT;
			//it slips under, it does not fade on the bank
			if (s.exists) assertEquals( 1f, s.alpha(), 0.001f );
		}
		assertFalse( s.exists );
		assertEquals( 1, dives.size() );
		assertEquals( 222f, dives.get( 0 )[0], 0.01f );
		assertEquals( 201f, dives.get( 0 )[1], 0.01f );
	}

	@Test
	public void aNewtWhoseWaterHasIcedOverRunsForACrackInstead(){
		Level l = pool( 13, 12, 15, 13, Terrain.WATER );
		final ArrayList<float[]> dives = new ArrayList<>();
		Basker n = new Basker( Basker.Look.NEWT, 200f, 200f, null, 222f, 201f, true ){
			@Override protected void slipIn( float x, float y ){ dives.add( new float[]{ x, y } ); }
		};
		run( n, 1f );
		for (int y = 12; y <= 13; y++) for (int x = 13; x <= 15; x++) l.map[x + y * 20] = Terrain.FROZEN_WATER;
		l.buildFlagMaps();
		n.scare( 180f, 200f );
		assertFalse( n.calm() );
		boolean faded = false;
		float t = 0f;
		while (n.exists && t < 3f){
			frame( n, DT );
			t += DT;
			if (n.exists && n.alpha() < 1f) faded = true;
		}
		assertFalse( n.exists );
		assertEquals( "it slipped into the ice", 0, dives.size() );
		assertTrue( "it never ran off into the dark", faded );
	}

	@Test
	public void aSalamandersSpotsGlowAndANewtLooksUpAtTheHero(){
		Basker s = new Basker( Basker.Look.SALAMANDER, 200f, 200f, null, 0f, 0f, false );
		boolean glowed = false;
		for (float t = 0f; t < 20f; t += DT){
			frame( s, DT );
			if (s.posed( DungeonCritterSprite.ROW_SALAMANDER, 4 )) glowed = true;
		}
		assertTrue( glowed );
		Basker n = new Basker( Basker.Look.NEWT, 200f, 200f, null, 0f, 0f, false );
		run( n, 1f );
		n.heed( 48f );
		frame( n, DT );
		assertTrue( n.posed( DungeonCritterSprite.ROW_NEWT, 3 ) );
		//no water beside it: off for a crack
		n.scare( 180f, 200f );
		assertTrue( runOut( n, 3f ) < 2f );
		assertFalse( n.exists );
	}

	// ------------------------------------------------------------ the beetles

	@Test
	public void aScaredBeetleOpensItsWingCasesAndIsUpAndAway(){
		Beetle b = new Beetle( false, 200f, 200f, null );
		run( b, 1f );
		assertFalse( b.wantsAir() );
		b.scare( 180f, 200f );
		assertFalse( b.calm() );
		assertTrue( "it takes wing", b.wantsAir() );
		b.lifted();
		assertFalse( b.wantsAir() );
		frame( b, DT );
		assertTrue( "wing cases open", b.posed( DungeonCritterSprite.ROW_BEETLE, 2 ) );
		float maxZ = 0f, t = DT;
		while (b.exists && t < 3f){
			frame( b, DT );
			t += DT;
			if (b.exists) maxZ = Math.max( maxZ, b.z );
		}
		assertFalse( b.exists );
		assertTrue( "took " + t + "s", t <= 1.2f + 2 * DT );
		assertTrue( "rose " + maxZ + "px", maxZ > 8f );
	}

	@Test
	public void anEmberBeetlesBackGlows(){
		Beetle b = new Beetle( true, 200f, 200f, null );
		boolean glowed = false;
		for (float t = 0f; t < 15f; t += DT){
			frame( b, DT );
			if (b.posed( DungeonCritterSprite.ROW_EMBER_BEETLE, 4 )) glowed = true;
		}
		assertTrue( glowed );
	}

	// ------------------------------------------------------------ the snail

	@Test
	public void aSnailDrawsInWhileTheHeroIsBesideItAndComesOutWhenHeIsGone(){
		Snail s = new Snail( 200f, 200f );
		run( s, 1f );
		assertFalse( s.hidden() );
		for (float t = 0f; t < 5f; t += DT){
			s.heed( 16f );
			frame( s, DT );
		}
		assertTrue( s.hidden() );
		assertTrue( s.posed( DungeonCritterSprite.ROW_SNAIL, 2 ) );
		//it never runs
		assertTrue( s.calm() );
		float x = s.gx();
		for (float t = 0f; t < 4f; t += DT){
			s.heed( 100f );
			frame( s, DT );
		}
		assertFalse( "still in its shell", s.hidden() );
		float crept = 0f;
		for (float t = 0f; t < 4f; t += DT){
			s.heed( 100f );
			frame( s, DT );
			crept = Math.max( crept, Math.abs( s.gx() - x ) );
		}
		assertTrue( "it never crept on", crept > 1f );
	}

	@Test
	public void aSnailKeepsToItsPatchOfMossAndIsLostInItAtLast(){
		Snail s = new Snail( 200f, 200f );
		float t = 0f;
		for (; s.exists && t < 130f; t += DT){
			frame( s, DT );
			assertTrue( Math.abs( s.gx() - 200f ) <= 5.001f && Math.abs( s.gy() - 200f ) <= 2.001f );
		}
		assertFalse( s.exists );
		assertTrue( "gone after " + t + "s", t >= 60f );
	}

	@Test
	public void aWalkerGoesItsWayIntoTheDarkAfterAWhile(){
		Scuttler r = new Scuttler( Scuttler.Look.CENTIPEDE, 200f, 196f, null );
		float t = 0f;
		for (; r.exists && t < 100f; t += DT) frame( r, DT );
		assertFalse( r.exists );
		assertTrue( "gone after " + t + "s", t >= 40f );
		Frog f = new Frog( 200f, 200f, 0f, 0f, false );
		t = 0f;
		for (; f.exists && t < 100f; t += DT) frame( f, DT );
		assertFalse( f.exists );
		assertTrue( "gone after " + t + "s", t >= 40f );
	}

	// ------------------------------------------------------------ the moths

	@Test
	public void aMothKeepsToItsLightAndFliesOffWhenStartled(){
		for (int n = 0; n < 10; n++){
			Moth m = new Moth( 300f, 300f, 0f, 0xF09848, true, true );
			for (float t = 0f; t < 20f; t += DT){
				frame( m, DT );
				assertTrue( "strayed from its light", Math.max( Math.abs( m.gx() - 300f ), Math.abs( m.gy() - 300f ) ) <= 16f );
			}
			assertTrue( m.calm() );
			m.scare( 300f, 330f );
			assertFalse( m.calm() );
			float t = runOut( m, 3f );
			assertFalse( m.exists );
			assertTrue( "took " + t + "s", t <= 1.5f + 2 * DT );
		}
	}

	@Test
	public void aMothOverTheLavaNeverComesDown(){
		Moth m = new Moth( 300f, 300f, 5f, 0xF09848, true, false );
		for (float t = 0f; t < 22f; t += DT){
			frame( m, DT );
			//wings folded flat: down on something
			assertFalse( m.posed( DungeonCritterSprite.ROW_MOTH, 3 ) );
		}
	}

	@Test
	public void aMothWithNoLightRestsOnTheWallAndFlitsAboutItsSpot(){
		Moth m = new Moth( 300f, 300f, 20f, 0xF2E8C8, false, false );
		frame( m, DT );
		assertTrue( m.posed( DungeonCritterSprite.ROW_MOTH, 3 ) );
		boolean flew = false;
		for (float t = 0f; t < 20f; t += DT){
			frame( m, DT );
			if (!m.posed( DungeonCritterSprite.ROW_MOTH, 3 )) flew = true;
			assertTrue( "strayed from its wall", Math.max( Math.abs( m.gx() - 300f ), Math.abs( m.gy() - 300f ) ) <= 24f );
		}
		assertTrue( flew );
	}

	// ------------------------------------------------------------ the spider

	@Test
	public void aSpiderLetsItselfDownAndClimbsBackWhenApproached(){
		Spider s = new Spider( 300f, 200f, 19f, 14f );
		run( s, 3f );
		assertTrue( s.calm() );
		//how far down its thread it hangs
		assertEquals( 14f, s.oy, 1.3f );
		//its thread runs from the face's top down to it
		ColorBlock silk = (ColorBlock) s.under();
		assertEquals( 300f, silk.x, 0.001f );
		assertEquals( 200f, silk.y, 0.001f );
		assertEquals( s.oy, silk.height(), 0.001f );
		assertTrue( silk.visible );
		s.scare( 300f, 230f );
		assertFalse( s.calm() );
		float last = s.oy, t = 0f;
		while (s.exists && t < 3f){
			frame( s, DT );
			t += DT;
			if (!s.exists) break;
			assertTrue( "it went down again", s.oy <= last + 0.001f );
			last = s.oy;
		}
		assertFalse( s.exists );
		assertTrue( "took " + t + "s", t < 1.2f );
		assertFalse( "its thread lingers", silk.exists );
	}

	// ------------------------------------------------------------ on the water

	@Test
	public void aStriderGlidesOnTheWaterAndIsGoneWhenItFreezes(){
		Level l = floor( 20, 20, Terrain.EMPTY );
		for (int y = 4; y < 16; y++) for (int x = 4; x < 16; x++) l.map[x + y * 20] = Terrain.WATER;
		l.buildFlagMaps();
		Dungeon.level = l;
		Strider s = new Strider( 160f, 160f );
		for (float t = 0f; t < 18f; t += DT){
			frame( s, DT );
			assertTrue( s.exists );
			int c = (int)(s.gx() / 16) + (int)(s.gy() / 16) * 20;
			assertTrue( "out of the water at " + s.gx() + "," + s.gy(), l.water[c] );
		}
		//the pool ices over under it
		for (int y = 4; y < 16; y++) for (int x = 4; x < 16; x++) l.map[x + y * 20] = Terrain.FROZEN_WATER;
		l.buildFlagMaps();
		assertTrue( runOut( s, 2f ) < 1f );
		assertFalse( s.exists );
	}

	@Test
	public void aScaredStriderDartsAcrossTheWater(){
		Level l = floor( 20, 20, Terrain.WATER );
		Dungeon.level = l;
		Strider s = new Strider( 160f, 160f );
		run( s, 1f );
		s.scare( 140f, 160f );
		assertFalse( s.calm() );
		assertTrue( runOut( s, 2f ) <= 0.7f + 2 * DT );
		assertFalse( s.exists );
	}

	// ------------------------------------------------------------ the statues

	@Test
	public void aBirdOnAStatueSettlesAndNeverHopsOffItsTop(){
		for (boolean crow : new boolean[]{ false, true }){
			Percher b = new Percher( crow, 200f, 196f, 12f );
			b.land( 0.6f, 0f );
			assertTrue( b.landing() );
			run( b, 1.5f );
			assertFalse( b.landing() );
			assertTrue( b.calm() );
			int row = crow ? 0 : 2;
			//well inside its while on the statue (45s at the least)
			for (float t = 0f; t < 30f; t += DT){
				frame( b, DT );
				assertEquals( 200f, b.gx(), 0f );
				assertEquals( 196f, b.gy(), 0f );
				assertEquals( 0f, b.z, 0f );
				assertTrue( "posed off its perch", b.posed( row, 0 ) || b.posed( row, 1 ) || b.posed( row, 2 ) );
			}
			b.heed( 60f );
			frame( b, DT );
			assertTrue( "it does not look up at the hero", b.posed( row, 2 ) );
		}
	}

	@Test
	public void aScaredBirdLeavesItsStatueClimbingAndIsGoneInUnderTwoSeconds(){
		Percher b = new Percher( false, 200f, 196f, 12f );
		b.land( 0f, 0f );
		run( b, 1.5f );
		b.scare( 200f, 240f );
		assertFalse( b.calm() );
		float t = 0f, lastZ = -1f;
		while (b.exists && t < 3f){
			frame( b, DT );
			t += DT;
			if (!b.exists) break;
			assertTrue( "it came down again", b.z >= lastZ );
			lastZ = b.z;
		}
		assertFalse( b.exists );
		assertTrue( "took " + t + "s", t <= 1.8f + 2 * DT );
		//up and away from below it
		assertTrue( lastZ > 30f );
	}

	@Test
	public void aBirdNotYetInSightOfItsStatueNeverArrives(){
		Percher b = new Percher( true, 200f, 196f, 12f );
		b.land( 0f, 0.5f );
		frame( b, DT );
		b.scare( 200f, 240f );
		assertFalse( b.exists );
	}

	// ------------------------------------------------------------ the web

	@Test
	public void aFlyStuckInTheWebStrugglesButNeverFlees(){
		Fly f = new Fly( 200f, 200f );
		run( f, 1f );
		int blurs = 0;
		for (float t = 0f; t < 8f; t += DT){
			f.scare( 205f, 200f );
			frame( f, DT );
			assertTrue( f.calm() );
			//its wings a blur, tugging at the threads where it hangs
			if (f.posed( DungeonCritterSprite.ROW_FLY, 1 )) blurs++;
			assertTrue( Math.abs( f.gx() - 200f ) <= 1.001f && f.gy() == 200f );
		}
		//a struggle the whole while: every other frame or so a blur
		assertTrue( "only " + blurs + " blurs", blurs > 8f / DT / 3f );
		assertFalse( f.wantsAir() );
	}

	@Test
	public void nowAndThenAFlyTearsItselfFree(){
		Fly f = new Fly( 200f, 200f );
		float t = 0f;
		boolean tookWing = false;
		while (f.exists && t < 240f){
			frame( f, DT );
			t += DT;
			if (f.wantsAir()){
				tookWing = true;
				f.lifted();
			}
		}
		assertTrue( tookWing );
		assertFalse( "it never got away", f.exists );
	}
}
