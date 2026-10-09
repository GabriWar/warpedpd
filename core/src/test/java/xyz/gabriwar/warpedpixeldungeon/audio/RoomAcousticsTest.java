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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place;
import com.badlogic.gdx.audio.Sound;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Room acoustics as it reaches the device (through SpatialSound.out): off, every effect plays as
 * spatial sound alone plays it; on, an effect of its table rings with its space's tail, echoes off
 * far rock, comes muffled through a door or rock, and never takes more voices than its budget.
 */
public class RoomAcousticsTest {

	private static final String TAIL_HALL_HIT = "sounds/tail_hall_hit.mp3";
	private static final String TAIL_ROOM_HIT = "sounds/tail_room_hit.mp3";
	private static final String TAIL_CAVERN_HIT = "sounds/tail_cavern_hit.mp3";
	private static final String MUFFLED_HIT = "sounds/muffled_hit.mp3";
	//a HALL tail at -8 dB, from a file stored 6 dB under its dry sound
	private static final float HALL_8 = (float) Math.pow( 10, -2 / 20.0 );
	//a CAVERN tail at -9 dB, the caves' (2 dB under a hall's -8, their own +1)
	private static final float CAVERN_9 = (float) Math.pow( 10, -3 / 20.0 );

	private static final class Played {
		final Object id;
		final float delay, volume, pitch, pan;
		Played( Object id, float delay, float volume, float pitch, float pan ){
			this.id = id; this.delay = delay; this.volume = volume; this.pitch = pitch; this.pan = pan;
		}
		@Override public String toString(){
			return id + " delay " + delay + " volume " + volume + " pitch " + pitch + " pan " + pan;
		}
	}

	private final ArrayList<Played> played = new ArrayList<>();
	private final double[] now = { 1000.0 };

	private boolean wasSpatial, wasDesktop, wasOn;
	private Earshot.Clock wasEarshotClock, wasClock;
	private RoomAcoustics.Loaded wasLoaded;
	private String previousVersion;
	private Level previousLevel;
	private Hero previousHero;

	@BeforeClass
	public static void boot(){
		AllItemsTest.titleScreen();
	}

	@Before
	public void setUp(){
		wasSpatial = SpatialSound.on;
		wasDesktop = SpatialSound.desktop;
		wasOn = RoomAcoustics.on;
		wasEarshotClock = Earshot.clock;
		wasClock = RoomAcoustics.clock;
		wasLoaded = RoomAcoustics.loaded;
		previousVersion = Game.version;
		Game.version = "test";
		previousLevel = Dungeon.level;
		previousHero = Dungeon.hero;

		SpatialSound.on = true;
		SpatialSound.desktop = false;
		SpatialSound.out = ( id, delay, volume, pitch, pan ) -> played.add( new Played( id, delay, volume, pitch, pan ) );
		RoomAcoustics.on = true;
		Earshot.clock = () -> now[0];
		RoomAcoustics.clock = () -> now[0];
		RoomAcoustics.loaded = id -> false;
		RoomAcoustics.reset();
	}

	@After
	public void tearDown(){
		SpatialSound.on = wasSpatial;
		SpatialSound.desktop = wasDesktop;
		SpatialSound.out = SpatialSound.SAMPLE;
		RoomAcoustics.on = wasOn;
		Earshot.clock = wasEarshotClock;
		RoomAcoustics.clock = wasClock;
		RoomAcoustics.loaded = wasLoaded;
		RoomAcoustics.reset();
		Actor.clear();
		Dungeon.level = previousLevel;
		Dungeon.hero = previousHero;
		Game.version = previousVersion;
		if (previousLevel != null) PathFinder.setMapSize( previousLevel.width(), previousLevel.height() );
	}

	private Hero listen( EarshotTest.Floor f, int x, int y ){
		Dungeon.level = f;
		Hero hero = new Hero();
		hero.pos = f.at( x, y );
		Dungeon.hero = hero;
		return hero;
	}

	/** a stone hall of 21 by 21 (HUGE: a HALL tail at -8 dB), the hero in its middle at (20, 20) */
	private EarshotTest.Floor hall(){
		EarshotTest.Floor f = EarshotTest.room( 10, 10, 21, 21 );
		listen( f, 20, 20 );
		return f;
	}

	private ArrayList<Played> play( Object id, int cell, float volume ){
		played.clear();
		SpatialSound.play( id, cell, volume );
		return new ArrayList<>( played );
	}

	private static int extras( ArrayList<Played> plays ){
		int n = 0;
		for (Played p : plays) if (p.id.toString().startsWith( "sounds/tail_" ) || p.delay > 0.05f) n++;
		return n;
	}

	// ----------------------------------------------------------------------- off

	@Test
	public void offEveryEffectPlaysAsSpatialSoundAlonePlaysIt(){
		RoomAcoustics.on = false;
		//the hall, and a sealed room east of it behind the rock
		EarshotTest.Floor f = hall().fill( 33, 12, 40, 28, Terrain.EMPTY );
		int hero = Dungeon.hero.pos;
		for (Object id : new Object[]{ Assets.Sounds.HIT, Assets.Sounds.BLAST, Assets.Sounds.STEP, Assets.Sounds.CHALLENGE }){
			for (int y = 8; y <= 32; y += 3){
				for (int x = 8; x <= 40; x += 3){
					int cell = f.at( x, y );
					played.clear();
					SpatialSound.play( id, cell, 0.8f, 1.1f );
					assertEquals( 1, played.size() );
					Played p = played.get( 0 );
					float dx = x - hero % f.width(), dy = y - hero / f.width();
					assertEquals( id, p.id );
					assertEquals( 0.8f * SpatialSound.level( dx, dy ), p.volume, 0f );
					assertEquals( SpatialSound.pan( dx ), p.pan, 0f );
					assertEquals( 1.1f, p.pitch, 0f );
					assertEquals( 0f, p.delay, 0f );
				}
			}
		}
	}

	@Test
	public void aSoundOutOfItsTablePlaysAsToday(){
		EarshotTest.Floor f = hall();
		ArrayList<Played> plays = play( Assets.Sounds.CHALLENGE, f.at( 23, 20 ), 1f );
		assertEquals( 1, plays.size() );
		assertEquals( Assets.Sounds.CHALLENGE, plays.get( 0 ).id );
		assertEquals( SpatialSound.level( 3f, 0f ), plays.get( 0 ).volume, 0f );
		assertEquals( 0.6f, plays.get( 0 ).pan, 1e-6f );
	}

	// --------------------------------------------------------------------- tails

	@Test
	public void aHitInAHugeHallRingsWithItsHallTail(){
		hall();
		played.clear();
		SpatialSound.playDelayed( Assets.Sounds.HIT, 0.2f, Dungeon.hero.pos, 1f, 1.1f );
		assertEquals( played.toString(), 2, played.size() );
		Played dry = played.get( 0 ), tail = played.get( 1 );
		assertEquals( Assets.Sounds.HIT, dry.id );
		assertEquals( 1f, dry.volume, 0f );
		assertEquals( 0f, dry.pan, 0f );
		assertEquals( TAIL_HALL_HIT, tail.id );
		assertEquals( 0.794f, tail.volume, 0.001f );
		assertEquals( HALL_8, tail.volume, 1e-6f );
		assertEquals( 0f, tail.pan, 0f );
		assertEquals( 0.2f, tail.delay, 0f );
		assertEquals( 1.1f, tail.pitch, 0f );
	}

	@Test
	public void theTailFadesSlowerThanItsSoundAndNeverComesWithinFiveDecibels(){
		EarshotTest.Floor f = hall();
		float lastRatio = 0;
		for (int d : new int[]{ 0, 3, 6, 10 }){
			now[0] += 2;
			ArrayList<Played> plays = play( Assets.Sounds.HIT, f.at( 20 + d, 20 ), 1f );
			assertEquals( plays.toString(), 2, plays.size() );
			Played dry = plays.get( 0 ), tail = plays.get( 1 );
			float lvl = SpatialSound.level( d, 0 );
			assertEquals( lvl, dry.volume, 1e-6f );
			assertEquals( Math.min( HALL_8 * (0.55f + 0.45f * lvl), 1.12f * dry.volume ), tail.volume, 1e-6f );
			assertEquals( "the tail is panned a little the other way", -0.3f * dry.pan, tail.pan, 1e-6f );
			float ratio = tail.volume / dry.volume;
			assertTrue( "wetter further off, at " + d, ratio > lastRatio );
			assertTrue( ratio <= 1.12f + 1e-6f );
			lastRatio = ratio;
		}
		assertEquals( "held at 10 cells", 1.12f, lastRatio, 1e-5f );
	}

	// ------------------------------------------------------------------ muffling

	/** the hall, a room north of it through a door at (17, 9), and a room east of it behind the rock */
	private EarshotTest.Floor hallWithNeighbours(){
		EarshotTest.Floor f = EarshotTest.room( 10, 10, 13, 13 )
				.fill( 14, 2, 20, 8, Terrain.EMPTY ).fill( 17, 9, 17, 9, Terrain.DOOR )
				.fill( 24, 12, 30, 20, Terrain.EMPTY );
		listen( f, 16, 16 );
		return f;
	}

	@Test
	public void throughADoorItIsMuffledHeardFromTheDoorAndRingsLess(){
		RoomAcoustics.loaded = id -> true;
		EarshotTest.Floor f = hallWithNeighbours();
		int cell = f.at( 18, 7 );
		float lvl = SpatialSound.level( 2, -9 );

		//the copies have only just loaded: the dry sound, as quiet as the copy would be
		ArrayList<Played> plays = play( Assets.Sounds.HIT, cell, 1f );
		assertEquals( plays.toString(), 2, plays.size() );
		assertEquals( Assets.Sounds.HIT, plays.get( 0 ).id );
		assertEquals( lvl * 0.8f, plays.get( 0 ).volume, 1e-6f );

		now[0] += 10;
		plays = play( Assets.Sounds.HIT, cell, 1f );
		assertEquals( plays.toString(), 2, plays.size() );
		Played dry = plays.get( 0 ), tail = plays.get( 1 );
		assertEquals( MUFFLED_HIT, dry.id );
		assertEquals( lvl * 0.8f, dry.volume, 1e-6f );
		assertEquals( "from the door, a cell to the right", SpatialSound.pan( 1 ), dry.pan, 1e-6f );
		assertEquals( TAIL_HALL_HIT, tail.id );
		assertEquals( HALL_8 * (0.55f + 0.45f * lvl) * 0.7f, tail.volume, 1e-6f );
		assertEquals( -0.3f * dry.pan, tail.pan, 1e-6f );
	}

	@Test
	public void behindRockOnlyAQuieterMuffledCopy(){
		EarshotTest.Floor f = hallWithNeighbours();
		int cell = f.at( 27, 16 );
		float lvl = SpatialSound.level( 11, 0 );

		ArrayList<Played> plays = play( Assets.Sounds.HIT, cell, 1f );
		assertEquals( plays.toString(), 1, plays.size() );
		assertEquals( "not ready: the dry sound", Assets.Sounds.HIT, plays.get( 0 ).id );
		assertEquals( lvl * 0.6f, plays.get( 0 ).volume, 1e-6f );

		RoomAcoustics.loaded = id -> true;
		play( Assets.Sounds.HIT, cell, 1f );
		now[0] += 10;
		plays = play( Assets.Sounds.HIT, cell, 1f );
		assertEquals( plays.toString(), 1, plays.size() );
		assertEquals( MUFFLED_HIT, plays.get( 0 ).id );
		assertEquals( lvl * 0.6f, plays.get( 0 ).volume, 1e-6f );
		assertEquals( SpatialSound.pan( 11 ), plays.get( 0 ).pan, 1e-6f );

		//a sound with no muffled copy: the dry sound, as quiet
		plays = play( Assets.Sounds.STEP, cell, 1f );
		assertEquals( 1, plays.size() );
		assertEquals( Assets.Sounds.STEP, plays.get( 0 ).id );
		assertEquals( lvl * 0.6f, plays.get( 0 ).volume, 1e-6f );
	}

	@Test
	public void theCopiesAreReadyTenSecondsAfterTheyLoadWhateverPlaysFirst(){
		RoomAcoustics.loaded = id -> true;
		EarshotTest.Floor f = hallWithNeighbours();
		//a blow in the open, every copy loaded: their ten seconds start now, not at the first sound
		//heard through rock
		play( Assets.Sounds.HIT, Dungeon.hero.pos, 1f );
		now[0] += 10;
		ArrayList<Played> plays = play( Assets.Sounds.HIT, f.at( 27, 16 ), 1f );
		assertEquals( plays.toString(), MUFFLED_HIT, plays.get( 0 ).id );
	}

	@Test
	public void aCopyThatDidNotLoadLeavesTheSoundItself(){
		//every copy loaded but the blow's (its file failed, or the switch went off as it played)
		RoomAcoustics.loaded = id -> !id.equals( MUFFLED_HIT );
		EarshotTest.Floor f = hallWithNeighbours();
		int cell = f.at( 27, 16 );
		play( Assets.Sounds.HIT, cell, 1f );
		now[0] += 10;
		ArrayList<Played> plays = play( Assets.Sounds.HIT, cell, 1f );
		assertEquals( plays.toString(), Assets.Sounds.HIT, plays.get( 0 ).id );
		assertEquals( SpatialSound.level( 11, 0 ) * 0.6f, plays.get( 0 ).volume, 1e-6f );
		//another's is there
		assertEquals( "sounds/muffled_hit_crush.mp3", play( Assets.Sounds.HIT_CRUSH, cell, 1f ).get( 0 ).id );
	}

	// ---------------------------------------------------------------------- echo

	private EarshotTest.Floor cave(){
		EarshotTest.Floor f = EarshotTest.room( 10, 10, 25, 25 );
		f.place = Place.CAVES;
		listen( f, 22, 22 );
		return f;
	}

	private static Played echo( ArrayList<Played> plays ){
		for (Played p : plays) if (p.id == Assets.Sounds.BLAST && p.delay > 0.05f) return p;
		return null;
	}

	@Test
	public void aHitInACavernRingsWithItsCavernTailAndEchoes(){
		EarshotTest.Floor f = cave();
		Earshot.Snapshot s = Earshot.of( f, Dungeon.hero.pos );
		played.clear();
		SpatialSound.playDelayed( Assets.Sounds.HIT, 0.2f, Dungeon.hero.pos, 1f, 1.1f );
		assertEquals( played.toString(), 3, played.size() );
		Played dry = played.get( 0 ), tail = played.get( 1 ), echo = played.get( 2 );
		assertEquals( Assets.Sounds.HIT, dry.id );
		assertEquals( 1f, dry.volume, 0f );
		assertEquals( TAIL_CAVERN_HIT, tail.id );
		assertEquals( 0.708f, tail.volume, 0.001f );
		assertEquals( CAVERN_9, tail.volume, 1e-6f );
		assertEquals( 0f, tail.pan, 0f );
		assertEquals( 0.2f, tail.delay, 0f );
		assertEquals( 1.1f, tail.pitch, 0f );
		//and the far wall gives it back
		assertEquals( Assets.Sounds.HIT, echo.id );
		assertTrue( "delay " + echo.delay, echo.delay >= 0.2f + s.echoDelay * 0.94f - 1e-6f
				&& echo.delay <= 0.2f + s.echoDelay * 1.06f + 1e-6f );

		//four cells east: wetter than its sound, panned a little the other way
		now[0] += 2;
		ArrayList<Played> plays = play( Assets.Sounds.HIT, f.at( 26, 22 ), 1f );
		float lvl = SpatialSound.level( 4, 0 );
		Played t = plays.get( 1 );
		assertEquals( TAIL_CAVERN_HIT, t.id );
		assertEquals( Math.min( CAVERN_9 * (0.55f + 0.45f * lvl), 1.12f * lvl ), t.volume, 1e-6f );
		assertEquals( -0.3f * SpatialSound.pan( 4 ), t.pan, 1e-6f );
		assertEquals( 0f, t.delay, 0f );
	}

	@Test
	public void aBlastInACaveEchoesBack(){
		EarshotTest.Floor f = cave();
		Earshot.Snapshot s = Earshot.of( f, Dungeon.hero.pos );
		ArrayList<Played> plays = play( Assets.Sounds.BLAST, f.at( 26, 22 ), 1f );
		assertEquals( plays.toString(), 3, plays.size() );
		Played e = echo( plays );
		assertTrue( plays.toString(), e != null );
		assertTrue( "delay " + e.delay, e.delay >= s.echoDelay * 0.94f - 1e-6f && e.delay <= s.echoDelay * 1.06f + 1e-6f );
		assertEquals( SpatialSound.level( 4, 0 ) * s.echoLevel, e.volume, 1e-6f );
		assertEquals( s.echoPan, e.pan, 1e-6f );

		//too far, too quiet, too soon after the last
		now[0] += 3;
		assertEquals( null, echo( play( Assets.Sounds.BLAST, f.at( 31, 22 ), 1f ) ) );
		now[0] += 3;
		assertEquals( null, echo( play( Assets.Sounds.BLAST, f.at( 26, 22 ), 0.4f ) ) );
		now[0] += 3;
		assertTrue( echo( play( Assets.Sounds.BLAST, f.at( 26, 22 ), 1f ) ) != null );
		now[0] += 0.2;
		assertEquals( null, echo( play( Assets.Sounds.BLAST, f.at( 26, 22 ), 1f ) ) );
		//and never a step
		now[0] += 3;
		assertEquals( 2, play( Assets.Sounds.STEP, f.at( 23, 22 ), 1f ).size() );
	}

	// -------------------------------------------------------------------- budget

	private static final Object[] HITS = { Assets.Sounds.HIT, Assets.Sounds.HIT_SLASH, Assets.Sounds.HIT_STAB,
			Assets.Sounds.HIT_CRUSH, Assets.Sounds.HIT_STRONG, Assets.Sounds.HIT_PARRY, Assets.Sounds.HIT_ARROW,
			Assets.Sounds.HIT_MAGIC, Assets.Sounds.ATK_CROSSBOW, Assets.Sounds.ATK_SPIRITBOW };

	@Test
	public void tenBlowsAtOnceTakeThreeExtrasAtMost(){
		hall();
		played.clear();
		for (Object id : HITS) SpatialSound.play( id, Dungeon.hero.pos );
		assertEquals( played.toString(), 13, played.size() );
		assertEquals( 3, extras( played ) );
	}

	@Test
	public void noMoreThanThreeExtrasSoundAtOnceForAsLongAsTheirFilesPlay() throws IOException {
		//long sounds 0.35 s apart in a big hall (tails only) and in a cave (tails and echoes): an extra
		//keeps a device voice for as long as its file plays, a rumble of rocks' hall tail 2.3 s and
		//its cavern tail 3.4 s
		Object[] ids = { Assets.Sounds.HIT_STRONG, Assets.Sounds.MINE, Assets.Sounds.ROCKS, Assets.Sounds.BLAST,
				Assets.Sounds.BONES, Assets.Sounds.ZAP };
		for (boolean inCave : new boolean[]{ false, true }){
			RoomAcoustics.reset();
			int cell = inCave ? cave().at( 25, 22 ) : hall().at( 20, 20 );
			ArrayList<double[]> sounding = new ArrayList<>();
			double start = now[0] += 10;
			for (int i = 0; i < 40; i++){
				now[0] = start + i * 0.35;
				for (Played p : play( ids[i % ids.length], cell, 1f )){
					boolean tail = p.id.toString().startsWith( "sounds/tail_" ), echo = !tail && p.delay > 0.05f;
					if (!tail && !echo) continue;
					double from = now[0] + p.delay;
					sounding.add( new double[]{ from,
							from + RoomAcousticsAssetsTest.playLength( new File( "src/main/assets", p.id.toString() ) ) / p.pitch } );
				}
			}
			assertTrue( "extras played", sounding.size() >= 10 );
			for (double[] a : sounding){
				int at = 0;
				for (double[] b : sounding) if (b[0] <= a[0] && a[0] < b[1]) at++;
				assertTrue( (inCave ? "cave" : "hall") + ": " + at + " extras at once, " + (a[0] - start) + " s in", at <= 3 );
			}
		}
	}

	@Test
	public void footstepsShareOneTail(){
		hall();
		double start = now[0];
		ArrayList<Integer> tails = new ArrayList<>();
		for (int k = 0; k < 20; k++){
			now[0] = start + k / 10.0;
			if (extras( play( Assets.Sounds.STEP, Dungeon.hero.pos, 1f ) ) > 0) tails.add( k );
		}
		assertEquals( "at 0 s and 1.0 s", "[0, 10]", tails.toString() );
	}

	//the most of these [from, to) spans sounding at once
	private static int mostAtOnce( ArrayList<double[]> spans ){
		int most = 0;
		for (double[] a : spans){
			int at = 0;
			for (double[] b : spans) if (b[0] <= a[0] && a[0] < b[1]) at++;
			most = Math.max( most, at );
		}
		return most;
	}

	//how long a play keeps its voice, s: its file as long as it plays, over its pitch
	private static double length( Played p ) throws IOException {
		return RoomAcousticsAssetsTest.playLength( new File( "src/main/assets", p.id.toString() ) ) / p.pitch;
	}

	@Test
	public void aCavernsFootstepsShareOneTailAndNeverHoldMoreThanTwoVoices() throws IOException {
		cave();
		double start = now[0];
		ArrayList<Integer> tails = new ArrayList<>();
		ArrayList<double[]> sounding = new ArrayList<>();
		for (int k = 0; k < 40; k++){
			now[0] = start + k / 10.0;
			for (Played p : play( Assets.Sounds.STEP, Dungeon.hero.pos, 1f )){
				if (!p.id.toString().startsWith( "sounds/tail_" )) continue;
				assertEquals( "sounds/tail_cavern_step.mp3", p.id );
				tails.add( k );
				sounding.add( new double[]{ now[0], now[0] + length( p ) } );
			}
		}
		assertEquals( "at 0 s, 1.6 s and 3.2 s", "[0, 16, 32]", tails.toString() );
		assertEquals( 2, mostAtOnce( sounding ) );
	}

	/** a cavern of 25 by 25 (the caves: a CAVERN tail at -9 dB, an echo off its far wall), the hero in
	 *  its middle at (22, 22); a room north of it through a door at (22, 9), one east of it behind the rock */
	private EarshotTest.Floor cavernWithNeighbours(){
		EarshotTest.Floor f = EarshotTest.room( 10, 10, 25, 25 )
				.fill( 19, 2, 25, 8, Terrain.EMPTY ).fill( 22, 9, 22, 9, Terrain.DOOR )
				.fill( 37, 14, 43, 30, Terrain.EMPTY );
		f.place = Place.CAVES;
		listen( f, 22, 22 );
		return f;
	}

	@Test
	public void aBusyFightInACavernKeepsToThreeExtras() throws IOException {
		//the acoustics-busy scene in a cavern, the muffled copies in use: the hero walks in, four foes
		//set on him, a blow a second each, a quarter of a second apart, and he strikes back; and the
		//scene's three rounds 1.5 s apart, a heavy blow on him, a blast six cells off, a blow behind
		//the door and a mine behind the rock
		RoomAcoustics.loaded = id -> true;
		EarshotTest.Floor f = cavernWithNeighbours();
		int hero = Dungeon.hero.pos;
		play( Assets.Sounds.HIT, hero, 1f );
		double start = now[0] += 10;
		ArrayList<Object[]> script = new ArrayList<>();
		for (int k = 0; k < 6; k++) script.add( new Object[]{ 0.3 * k, Assets.Sounds.STEP, hero } );
		int[] foes = { f.at( 21, 22 ), f.at( 23, 22 ), f.at( 22, 21 ), f.at( 22, 23 ) };
		Object[] blows = { Assets.Sounds.HIT, Assets.Sounds.HIT_SLASH, Assets.Sounds.HIT_CRUSH, Assets.Sounds.MISS };
		for (int t = 2; t < 8; t++){
			for (int i = 0; i < 4; i++) script.add( new Object[]{ t + 0.25 * i, blows[i], foes[i] } );
			script.add( new Object[]{ t + 0.6, Assets.Sounds.HIT_STRONG, foes[t % 4] } );
		}
		for (int r = 0; r < 3; r++){
			double at = 2.5 + 1.5 * r;
			script.add( new Object[]{ at, Assets.Sounds.HIT_STRONG, hero } );
			script.add( new Object[]{ at + 0.3, Assets.Sounds.BLAST, f.at( 28, 22 ) } );
			script.add( new Object[]{ at + 0.6, Assets.Sounds.HIT, f.at( 22, 5 ) } );
			script.add( new Object[]{ at + 0.9, Assets.Sounds.MINE, f.at( 40, 22 ) } );
		}
		script.sort( ( a, b ) -> Double.compare( (double) a[0], (double) b[0] ) );

		ArrayList<double[]> extras = new ArrayList<>();
		int caverns = 0;
		for (Object[] e : script){
			now[0] = start + (double) e[0];
			for (Played p : play( e[1], (int) e[2], 1f )){
				boolean tail = p.id.toString().startsWith( "sounds/tail_" ), echo = !tail && p.delay > 0.05f;
				if (!tail && !echo) continue;
				double from = now[0] + p.delay;
				extras.add( new double[]{ from, from + length( p ) } );
				if (p.id.toString().startsWith( "sounds/tail_cavern_" )) caverns++;
			}
		}
		assertTrue( "extras played: " + extras.size(), extras.size() >= 6 );
		assertTrue( "cavern tails: " + caverns, caverns >= 3 );
		assertTrue( mostAtOnce( extras ) + " extras at once", mostAtOnce( extras ) <= 3 );
	}

	@Test
	public void extrasComeBackAfterASecond(){
		listen( EarshotTest.room( 10, 10, 7, 7 ), 13, 13 );
		played.clear();
		for (Object id : HITS) SpatialSound.play( id, Dungeon.hero.pos );
		assertEquals( 3, extras( played ) );
		now[0] += 1;
		ArrayList<Played> plays = play( Assets.Sounds.HIT, Dungeon.hero.pos, 1f );
		assertEquals( plays.toString(), 2, plays.size() );
		assertEquals( TAIL_ROOM_HIT, plays.get( 1 ).id );
		assertTrue( RoomAcoustics.skipped() > 0 );
		assertTrue( RoomAcoustics.scheduled() >= 4 );
	}

	// --------------------------------------------------------------- the switches

	@Test
	public void onItQueuesItsFilesAndOffItLetsThemGoQueuedOrLoaded() throws Exception {
		Field queueField = Sample.class.getDeclaredField( "loadingQueue" ), idsField = Sample.class.getDeclaredField( "ids" );
		queueField.setAccessible( true );
		idsField.setAccessible( true );
		@SuppressWarnings("unchecked") LinkedList<String> queue = (LinkedList<String>) queueField.get( null );
		@SuppressWarnings("unchecked") HashMap<Object, Sound> ids = (HashMap<Object, Sound>) idsField.get( Sample.INSTANCE );
		List<String> assets = Arrays.asList( RoomAcoustics.assets() );
		//one of them loaded already: a stand-in that tells when it is let go
		String loaded = assets.get( 5 );
		boolean[] disposed = { false };
		Sound sound = (Sound) Proxy.newProxyInstance( Sound.class.getClassLoader(), new Class<?>[]{ Sound.class }, ( proxy, m, args ) -> {
			if (m.getName().equals( "dispose" )) disposed[0] = true;
			return m.getReturnType() == long.class ? 0L : null;
		} );
		synchronized (Sample.INSTANCE){
			ids.put( loaded, sound );
		}
		try {
			RoomAcoustics.enable( true );
			assertTrue( RoomAcoustics.on );
			ArrayList<String> queued = new ArrayList<>();
			synchronized (queue){
				for (String a : queue) if (assets.contains( a )) queued.add( a );
			}
			assertEquals( "the preview's tail first", assets.get( 0 ), queued.get( 0 ) );
			assertEquals( "all but the one loaded", assets.size() - 1, queued.size() );

			RoomAcoustics.enable( false );
			assertFalse( RoomAcoustics.on );
			synchronized (queue){
				for (String a : assets) assertFalse( a + " is still queued", queue.contains( a ) );
			}
			assertTrue( "the loaded one is let go", disposed[0] );
			assertFalse( Sample.INSTANCE.isLoaded( loaded ) );
		} finally {
			synchronized (queue){
				queue.removeAll( assets );
			}
			synchronized (Sample.INSTANCE){
				ids.remove( loaded );
			}
		}
	}

	@Test
	public void withSpatialSoundOffTheExtrasAreCentredAndAsWetNearAndFar(){
		SpatialSound.on = false;
		EarshotTest.Floor f = hall();
		ArrayList<Played> plays = play( Assets.Sounds.HIT, f.at( 26, 20 ), 1f );
		assertEquals( 2, plays.size() );
		assertEquals( 1f, plays.get( 0 ).volume, 0f );
		assertEquals( 0f, plays.get( 0 ).pan, 0f );
		assertEquals( HALL_8, plays.get( 1 ).volume, 1e-6f );
		assertEquals( 0f, plays.get( 1 ).pan, 0f );

		//nor are echoes or doors given a side
		EarshotTest.Floor c = cave();
		Played e = echo( play( Assets.Sounds.BLAST, c.at( 26, 22 ), 1f ) );
		assertTrue( e != null );
		assertEquals( 0f, e.pan, 0f );
	}

	@Test
	public void aGuestHearsFromHisOwnHero(){
		//two rooms the rock keeps apart: the host in one, a guest in the other
		EarshotTest.Floor f = EarshotTest.room( 10, 10, 7, 7 ).fill( 30, 10, 36, 16, Terrain.EMPTY );
		int cell = f.at( 34, 13 );
		listen( f, 33, 13 );
		ArrayList<Played> plays = play( Assets.Sounds.HIT, cell, 1f );
		assertEquals( plays.toString(), 2, plays.size() );
		assertEquals( Assets.Sounds.HIT, plays.get( 0 ).id );
		assertEquals( TAIL_ROOM_HIT, plays.get( 1 ).id );

		now[0] += 1;
		listen( f, 13, 13 );
		plays = play( Assets.Sounds.HIT, cell, 1f );
		assertEquals( plays.toString(), 1, plays.size() );
		assertEquals( SpatialSound.level( 21, 0 ) * 0.6f, plays.get( 0 ).volume, 1e-6f );
	}

	@Test
	public void theDungeonsSeededRandomIsNeverTouched(){
		EarshotTest.Floor f = cave();
		Random.pushGenerator( 99 );
		int[] expected = new int[10];
		for (int i = 0; i < expected.length; i++) expected[i] = Random.Int( 1000000 );
		Random.popGenerator();

		Random.pushGenerator( 99 );
		for (int i = 0; i < 100; i++){
			now[0] += 0.6;
			SpatialSound.play( Assets.Sounds.BLAST, f.at( 23 + i % 4, 22 ) );
		}
		int[] got = new int[10];
		for (int i = 0; i < got.length; i++) got[i] = Random.Int( 1000000 );
		Random.popGenerator();
		assertArrayEquals( expected, got );
	}
}
