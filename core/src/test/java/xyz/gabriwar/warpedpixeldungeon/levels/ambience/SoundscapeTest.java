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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The scheduler of the voices (Soundscape), driven frame by frame on hand-built floors with its
 * own dice: never more than three voices at once, a bed played on while its source is near, on
 * its beat through a source lost and found, and stopped as the hero walks off, one pipe bed that
 * trickles or pours by its pipe, the quiet before the first sound, waits that are never a
 * metronome, a frogs' chorus from several banks at several pitches, echoes that are the very
 * sound thrown back, the dark's voices only from the dark, a source in a wall heard from its
 * face, and the cells carried along when the overworld's window moves.
 */
public class SoundscapeTest {

	private static final float DT = 1f / 30f;

	/** One sound as the scheduler sent it out. */
	private static final class Heard {
		final Voice v;
		final int take, cell;
		final float at, level, pitch;
		final boolean echo;

		Heard( Voice v, int take, int cell, float at, float level, float pitch, boolean echo ){
			this.v = v;
			this.take = take;
			this.cell = cell;
			this.at = at;
			this.level = level;
			this.pitch = pitch;
			this.echo = echo;
		}

		//how long it rings: the pitch is the one it is played at, the nudge in it
		float end(){
			return at + v.length / pitch;
		}
	}

	private static final class Rec implements Soundscape.Out {
		Soundscape s;
		final ArrayList<Heard> heard = new ArrayList<>();

		@Override
		public void play( Voice v, int take, int cell, float level, float pitch, boolean echo ){
			assertTrue( "one of its takes", take >= 0 && take < v.takes.length );
			heard.add( new Heard( v, take, cell, s.now, level, pitch, echo ) );
		}
	}

	private static Voice voice( Place p, AmbientSound s ){
		for (Voice v : AmbientSounds.voices( p )) if (v.sound == s) return v;
		throw new AssertionError( p + " has no " + s );
	}

	//the voice a sound was played for: a bed for its other sound (a pipe that pours), else its own
	private static Voice owner( Voice[] voices, Voice played ){
		for (Voice v : voices) if (v.alt == played) return v;
		return played;
	}

	private static Soundscape scape( Voice[] voices, long seed, Rec rec ){
		Soundscape s = new Soundscape( voices, new Random( seed ) );
		rec.s = s;
		return s;
	}

	private static boolean[] seen( int n, boolean v ){
		boolean[] b = new boolean[n];
		Arrays.fill( b, v );
		return b;
	}

	/**
	 * A sewer floor 40 x 40 for everything at once: rock round the edge, a channel of water
	 * across with a wall over it that has drain pipes in it, banks of moss, grass, cells of floor.
	 */
	private static int[] sewer(){
		int w = 40;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int i = 0; i < w; i++){
			m[i] = m[i + (w - 1) * w] = m[i * w] = m[w - 1 + i * w] = Terrain.WALL;
		}
		for (int x = 1; x < w - 1; x++){
			m[x + 14 * w] = x % 6 == 3 ? Terrain.WALL_DECO : Terrain.WALL;
			for (int y = 15; y <= 18; y++) m[x + y * w] = Terrain.WATER;
		}
		for (int y = 22; y <= 30; y++) for (int x = 4; x <= 14; x++) m[x + y * w] = Terrain.GRASS;
		for (int y = 4; y <= 10; y++) m[20 + y * w] = Terrain.WALL;
		return m;
	}

	//what the hero sees: the cells within `r` of him
	private static void see( boolean[] sight, int w, int hero, int r ){
		Arrays.fill( sight, false );
		int hx = hero % w, hy = hero / w;
		for (int y = Math.max( 0, hy - r ); y <= Math.min( sight.length / w - 1, hy + r ); y++){
			for (int x = Math.max( 0, hx - r ); x <= Math.min( w - 1, hx + r ); x++) sight[x + y * w] = true;
		}
	}

	private static Soundscape.Air night(){
		Soundscape.Air a = new Soundscape.Air();
		a.phase = Phase.NIGHT;
		return a;
	}

	// ------------------------------------------------------------ the rules

	@Test
	public void theWaitsAreNeverAMetronome(){
		Random r = new Random( 7 );
		float mean = 10f, sum = 0f, sq = 0f, lo = Float.MAX_VALUE, hi = 0f;
		HashSet<Float> distinct = new HashSet<>();
		int n = 20000;
		for (int i = 0; i < n; i++){
			float t = Soundscape.nextInterval( mean, r.nextFloat() );
			sum += t;
			sq += t * t;
			lo = Math.min( lo, t );
			hi = Math.max( hi, t );
			distinct.add( t );
		}
		float avg = sum / n, sd = (float)Math.sqrt( sq / n - avg * avg );
		assertEquals( "about its mean", mean, avg, 0.4f );
		assertTrue( "spread like rain, not a clock: sd " + sd, sd > 0.45f * mean );
		assertTrue( "never bunched up", lo >= 0.35f * mean - 1e-4f );
		assertTrue( "never lost", hi <= 4f * mean + 1e-4f );
		assertTrue( distinct.size() > n / 2 );
		//the dice's ends
		assertEquals( 0.35f * mean, Soundscape.nextInterval( mean, 0f ), 1e-4f );
		assertEquals( 4f * mean, Soundscape.nextInterval( mean, 1f ), 1e-4f );
	}

	@Test
	public void aBedFadesToItsReachAndNoFurther(){
		float last = 1f;
		for (float d = 0f; d <= 9f; d += 0.25f){
			float r = Soundscape.reach( d, 6f );
			assertTrue( r >= 0f && r <= 1f );
			assertTrue( "never louder further off", r <= last );
			last = r;
		}
		assertEquals( 1f, Soundscape.reach( 5f, 6f ), 1e-6f );
		assertEquals( 0.5f, Soundscape.reach( 6f, 6f ), 1e-6f );
		assertEquals( 0f, Soundscape.reach( 7f, 6f ), 1e-6f );
	}

	@Test
	public void probesStayInTheRingAndOnTheMap(){
		int w = 30;
		int[] m = new int[w * w];
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 2 + 2 * w, null );
		Random r = new Random( 3 );
		int inside = 0;
		for (int i = 0; i < 5000; i++){
			int c = Soundscape.probe( r, g, 3f, 8f, i % 2 == 0 );
			if (c < 0) continue;
			inside++;
			assertTrue( "inside the outer ring", g.inner( c ) );
			float d = g.away( c );
			assertTrue( "near the ring: " + d, d >= 3f - 0.75f && d <= 8f + 0.75f );
		}
		assertTrue( inside > 500 );
	}

	@Test
	public void rockBetweenMufflesTheDarkDoesNot(){
		int w = 12;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		int hero = 2 + 5 * w;
		assertFalse( "open floor", Soundscape.behindRock( m, w, hero, 9 + 5 * w ) );
		assertFalse( "open floor, aslant", Soundscape.behindRock( m, w, hero, 9 + 9 * w ) );
		assertFalse( "the hero's own cell", Soundscape.behindRock( m, w, hero, hero ) );
		m[5 + 5 * w] = Terrain.WALL;
		assertTrue( "a wall between", Soundscape.behindRock( m, w, hero, 9 + 5 * w ) );
		assertTrue( "either way", Soundscape.behindRock( m, w, 9 + 5 * w, hero ) );
		assertFalse( "a wall to one side", Soundscape.behindRock( m, w, hero, 2 + 9 * w ) );
		m[5 + 5 * w] = Terrain.DOOR;
		assertTrue( "a shut door", Soundscape.behindRock( m, w, hero, 9 + 5 * w ) );
		m[5 + 5 * w] = Terrain.OPEN_DOOR;
		assertFalse( "an open one", Soundscape.behindRock( m, w, hero, 9 + 5 * w ) );
		for (int t : new int[]{ Terrain.HIGH_GRASS, Terrain.DEEP_WATER, Terrain.STATUE, Terrain.CHASM }){
			m[5 + 5 * w] = t;
			assertFalse( "a sound carries over " + t, Soundscape.behindRock( m, w, hero, 9 + 5 * w ) );
		}
		//a source in the rock itself (a pipe, a torch) is not behind it
		m[5 + 5 * w] = Terrain.WALL_DECO;
		assertFalse( Soundscape.behindRock( m, w, hero, 5 + 5 * w ) );
		assertFalse( "next to the hero", Soundscape.behindRock( m, w, 4 + 5 * w, 5 + 5 * w ) );
	}

	@Test
	public void cellsGoWithTheWindow(){
		int w = 10, h = 8;
		assertEquals( 3 + 2 * w, Soundscape.moved( 5 + 4 * w, 2, 2, w, h ) );
		assertEquals( 7 + 6 * w, Soundscape.moved( 5 + 4 * w, -2, -2, w, h ) );
		assertEquals( "left behind", -1, Soundscape.moved( 1 + 4 * w, 2, 0, w, h ) );
		assertEquals( -1, Soundscape.moved( 5 + 7 * w, 0, -1, w, h ) );
		assertEquals( "no cell stays none", -1, Soundscape.moved( -1, 0, 0, w, h ) );
		//a bed's source goes with it
		Voice bed = new Voice( AmbientSound.TRICKLE ).bed( 6f, 1.45f ).from( Source.PIPE );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ bed }, 1, rec );
		s.bedAt[0] = 5 + 4 * w;
		s.shift( 2, 1, w, h );
		assertEquals( 3 + 3 * w, s.bedAt[0] );
		s.shift( 4, 0, w, h );
		assertEquals( -1, s.bedAt[0] );
	}

	// ------------------------------------------------------------ the scheduler

	@Test
	public void neverMoreThanThreeVoicesAtOnce(){
		int w = 40;
		int[] m = sewer();
		Rec rec = new Rec();
		Voice[] voices = AmbientSounds.voices( Place.SEWERS );
		Soundscape s = scape( voices, 11, rec );
		boolean[] sight = new boolean[m.length];
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 9 + 20 * w, sight );
		Soundscape.Air a = night();
		int most = 0;
		for (int f = 0; f < 30 * 60 * 20; f++){
			//the hero walks up and down the bank, by the pipes and away, seeing six cells round
			int x = 3 + (f / 90) % 34;
			int at = x + ((f / 3000) % 2 == 0 ? 20 : 12) * w;
			if (at != g.hero) see( sight, w, at, 6 );
			g.hero = at;
			s.tick( DT, g, a, rec );
			assertTrue( "sounding " + s.sounding(), s.sounding() <= Soundscape.MAX_VOICES );
			most = Math.max( most, s.sounding() );
		}
		assertEquals( "the floor is busy enough to fill every voice", Soundscape.MAX_VOICES, most );
		//and by what was heard: no moment with more than three voices ringing
		for (float t = 0f; t < s.now; t += 0.05f){
			HashSet<Voice> ringing = new HashSet<>();
			for (Heard h : rec.heard) if (h.at <= t && t < h.end()) ringing.add( owner( voices, h.v ) );
			assertTrue( ringing.size() + " voices at " + t, ringing.size() <= Soundscape.MAX_VOICES );
		}
		HashSet<AmbientSound> sounds = new HashSet<>();
		for (Heard h : rec.heard) sounds.add( h.v.sound );
		for (AmbientSound want : new AmbientSound[]{ AmbientSound.DRIP, AmbientSound.DRIP_METAL, AmbientSound.GURGLE,
				AmbientSound.FROG, AmbientSound.CRICKET, AmbientSound.RAT }){
			assertTrue( want + " was heard", sounds.contains( want ) );
		}
		assertTrue( "a pipe was heard spilling",
				sounds.contains( AmbientSound.TRICKLE ) || sounds.contains( AmbientSound.POUR ) );
		assertFalse( "no fly by night", sounds.contains( AmbientSound.FLY ) );
		assertFalse( "no birds by night", sounds.contains( AmbientSound.BIRD ) );
	}

	@Test
	public void aRingingCritterTakesOneOfTheVoices(){
		int w = 40;
		int[] m = sewer();
		Rec rec = new Rec();
		Voice[] voices = AmbientSounds.voices( Place.SEWERS );
		Soundscape s = scape( voices, 11, rec );
		boolean[] sight = new boolean[m.length];
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 9 + 20 * w, sight );
		Soundscape.Air a = night();
		//a critter croaking or squeaking the whole while (AmbientSounds sets it from DungeonCritterSprite)
		s.others = 1;
		int most = 0;
		for (int f = 0; f < 30 * 60 * 20; f++){
			int x = 3 + (f / 90) % 34;
			int at = x + ((f / 3000) % 2 == 0 ? 20 : 12) * w;
			if (at != g.hero) see( sight, w, at, 6 );
			g.hero = at;
			s.tick( DT, g, a, rec );
			assertTrue( "sounding " + s.sounding(), s.sounding() <= Soundscape.MAX_VOICES );
			most = Math.max( most, s.sounding() - s.others );
		}
		assertEquals( "the place's own fill the voices the critter leaves", Soundscape.MAX_VOICES - 1, most );
		for (float t = 0f; t < s.now; t += 0.05f){
			HashSet<Voice> ringing = new HashSet<>();
			for (Heard h : rec.heard) if (h.at <= t && t < h.end()) ringing.add( owner( voices, h.v ) );
			assertTrue( ringing.size() + " voices at " + t, ringing.size() <= Soundscape.MAX_VOICES - 1 );
		}
	}

	@Test
	public void aCritterWaitsWhileThePlaceHoldsEveryVoice(){
		int w = 40;
		int[] m = sewer();
		Rec rec = new Rec();
		Voice[] voices = AmbientSounds.voices( Place.SEWERS );
		Soundscape s = scape( voices, 7, rec );
		boolean[] sight = new boolean[m.length];
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 9 + 20 * w, sight );
		Soundscape.Air a = night();
		//a frog on the bank that tries a croak every few seconds, as AmbientSounds and the critters
		//take their turns in a frame: the place ticks counting a croak still ringing, then the frog
		//asks whether the place is full (DungeonCritterSprite.voiceAt, AmbientSounds.full)
		ArrayList<float[]> croaks = new ArrayList<>();
		float croakEnds = -1f, nextTry = 0f;
		int refused = 0;
		for (int f = 0; f < 20 * 60 * 30; f++){
			int x = 3 + (f / 90) % 34;
			int at = x + ((f / 3000) % 2 == 0 ? 20 : 12) * w;
			if (at != g.hero) see( sight, w, at, 6 );
			g.hero = at;
			s.others = s.now < croakEnds ? 1 : 0;
			s.tick( DT, g, a, rec );
			if (s.now >= nextTry){
				if (s.full()){
					refused++;
				} else {
					croakEnds = s.now + AmbientSound.FROG.length;
					croaks.add( new float[]{ s.now, croakEnds } );
				}
				nextTry = s.now + 3f;
			}
		}
		assertTrue( "the frog croaked", croaks.size() > 50 );
		assertTrue( "and was kept waiting while the place was full", refused > 0 );
		for (float t = 0f; t < s.now; t += 0.05f){
			HashSet<Voice> ringing = new HashSet<>();
			for (Heard h : rec.heard) if (h.at <= t && t < h.end()) ringing.add( owner( voices, h.v ) );
			int n = ringing.size();
			for (float[] c : croaks) if (c[0] <= t && t < c[1]) n++;
			assertTrue( n + " voices at " + t, n <= Soundscape.MAX_VOICES );
		}
	}

	@Test
	public void nothingIsHeardFromASecretRoomUntilItIsFound(){
		int w = 30;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 10 * w] = Terrain.WALL;
		for (int y = 11; y <= 13; y++) for (int x = 1; x < w - 1; x++) m[x + y * w] = Terrain.WATER;
		int pipe = 12 + 10 * w;
		m[pipe] = Terrain.WALL_DECO;
		Voice bed = new Voice( AmbientSound.TRICKLE ).bed( 6f, 1.45f ).from( Source.PIPE );
		Voice drip = new Voice( AmbientSound.DRIP_METAL ).every( 1f ).from( Source.PIPE );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ bed, drip }, 12, rec );
		//the hero north of the wall, four cells from the pipe; under the wall a secret room he has
		//not found, its rim (the wall, the pipe in it) and all
		boolean[] sight = new boolean[m.length];
		for (int c = 0; c < 11 * w; c++) sight[c] = true;
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 12 + 6 * w, sight );
		g.secret = new boolean[m.length];
		for (int c = 10 * w; c < 15 * w; c++) g.secret[c] = true;
		g.visited = new boolean[m.length];
		g.mapped = new boolean[m.length];
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 30f) s.tick( DT, g, a, rec );
		assertTrue( rec.heard.size() + " heard from the secret room", rec.heard.isEmpty() );
		//mapped (a scroll of magic mapping shows the secrets): heard
		for (int c = 10 * w; c < 15 * w; c++) g.mapped[c] = true;
		while (s.now < 60f) s.tick( DT, g, a, rec );
		assertFalse( "heard once mapped", rec.heard.isEmpty() );
		for (Heard h : rec.heard) assertEquals( pipe, h.cell );
		//and once explored, mapped or not
		Arrays.fill( g.mapped, false );
		for (int c = 10 * w; c < 15 * w; c++) g.visited[c] = true;
		int before = rec.heard.size();
		while (s.now < 90f) s.tick( DT, g, a, rec );
		assertTrue( "heard once explored", rec.heard.size() > before );
	}

	@Test
	public void theFirstSoundWaitsForTheQuiet(){
		int w = 40;
		int[] m = sewer();
		for (long seed = 1; seed <= 20; seed++){
			Rec rec = new Rec();
			Soundscape s = scape( AmbientSounds.voices( Place.SEWERS ), seed, rec );
			Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 9 + 20 * w, seen( m.length, true ) );
			Soundscape.Air a = night();
			while (rec.heard.isEmpty() && s.now < 30f) s.tick( DT, g, a, rec );
			float first = rec.heard.get( 0 ).at;
			assertTrue( "not before the quiet: " + first, first >= Soundscape.QUIET_MIN );
			assertTrue( "but soon after: " + first, first <= Soundscape.QUIET_MAX + 3 * DT );
		}
	}

	@Test
	public void aBedPlaysOnWhileItsSourceIsNearAndStopsAfter(){
		int w = 30;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 10 * w] = Terrain.WALL;
		for (int y = 11; y <= 13; y++) for (int x = 1; x < w - 1; x++) m[x + y * w] = Terrain.WATER;
		int pipe = 10 + 10 * w;
		m[pipe] = Terrain.WALL_DECO;
		Voice bed = new Voice( AmbientSound.TRICKLE ).bed( 6f, 1.45f ).pitch( 0.95f, 1.08f ).from( Source.PIPE );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ bed }, 5, rec );
		boolean[] sight = seen( m.length, true );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 12 + 15 * w, sight );
		Soundscape.Air a = new Soundscape.Air();

		//near: on and on, from the pipe, each play a little before the last ends
		while (s.now < 30f) s.tick( DT, g, a, rec );
		assertFalse( rec.heard.isEmpty() );
		assertTrue( "after the quiet", rec.heard.get( 0 ).at <= Soundscape.QUIET_MAX + 0.5f );
		float beatLo = (bed.length - bed.overlap) / (bed.pitchHi * (1f + AmbientPlayer.PITCH_SPREAD)) * (1f - Soundscape.BED_JITTER);
		float beatHi = (bed.length - bed.overlap) / (bed.pitchLo * (1f - AmbientPlayer.PITCH_SPREAD)) * (1f + Soundscape.BED_JITTER);
		for (int i = 0; i < rec.heard.size(); i++){
			Heard h = rec.heard.get( i );
			assertEquals( pipe, h.cell );
			if (i == 0) continue;
			float gap = h.at - rec.heard.get( i - 1 ).at;
			assertTrue( "the beat " + gap, gap >= beatLo - DT && gap <= beatHi + DT );
			assertTrue( "it starts before the last one ends", h.at < rec.heard.get( i - 1 ).end() );
		}
		assertTrue( s.on[0] );
		assertEquals( 1, s.sounding() );
		int played = rec.heard.size();

		//away: it stops, and its last play rings out
		g.hero = 25 + 25 * w;
		while (s.now < 50f) s.tick( DT, g, a, rec );
		assertFalse( s.on[0] );
		assertEquals( -1, s.bedAt[0] );
		assertEquals( 0, s.sounding() );
		for (int i = played; i < rec.heard.size(); i++){
			assertTrue( "nothing once away", rec.heard.get( i ).at <= 30f + 2 * DT );
		}
		int after = rec.heard.size();

		//back: it starts again soon, quieter the further; in the dark just as loud, but muffled
		//through the rock once a wall stands between
		g.hero = 10 + 15 * w;
		while (s.now < 52f) s.tick( DT, g, a, rec );
		assertTrue( "found again within two seconds", rec.heard.size() > after );
		Heard back = rec.heard.get( after );
		assertEquals( Soundscape.reach( 5f, 6f ), back.level, 1e-4f );
		Arrays.fill( sight, false );
		while (s.now < 56f) s.tick( DT, g, a, rec );
		assertEquals( Soundscape.reach( 5f, 6f ), rec.heard.get( rec.heard.size() - 1 ).level, 1e-4f );
		m[10 + 14 * w] = Terrain.WALL;
		while (s.now < 62f) s.tick( DT, g, a, rec );
		Heard walled = rec.heard.get( rec.heard.size() - 1 );
		assertEquals( Soundscape.MUFFLED * Soundscape.reach( 5f, 6f ), walled.level, 1e-4f );
	}

	@Test
	public void aBedLostAndFoundAgainKeepsItsBeat(){
		int w = 30;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 10 * w] = Terrain.WALL;
		for (int y = 11; y <= 13; y++) for (int x = 1; x < w - 1; x++) m[x + y * w] = Terrain.WATER;
		int pipe = 10 + 10 * w;
		m[pipe] = Terrain.WALL_DECO;
		Voice bed = new Voice( AmbientSound.TRICKLE ).bed( 6f, 1.45f ).pitch( 0.95f, 1.08f ).from( Source.PIPE );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ bed }, 6, rec );
		//the hero steps to and fro over the edge of its reach, a step every 0.2 s: just in, just out
		int in = 13 + 15 * w, out = 14 + 15 * w;
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, in, seen( m.length, true ) );
		assertTrue( g.away( pipe ) <= bed.radius );
		g.hero = out;
		assertTrue( g.away( pipe ) > bed.radius );
		Soundscape.Air a = new Soundscape.Air();
		int lost = 0;
		boolean was = false;
		while (s.now < 300f){
			g.hero = (int)(s.now / 0.2f) % 2 == 0 ? in : out;
			s.tick( DT, g, a, rec );
			if (was && !s.on[0]) lost++;
			was = s.on[0];
		}
		assertTrue( "it lost its pipe over and over: " + lost, lost > 20 );
		assertTrue( rec.heard.size() > 20 );
		//found again, it waits for its beat: never a second take started over the one still playing
		float beatLo = (bed.length - bed.overlap) / (bed.pitchHi * (1f + AmbientPlayer.PITCH_SPREAD)) * (1f - Soundscape.BED_JITTER);
		for (int i = 1; i < rec.heard.size(); i++){
			float gap = rec.heard.get( i ).at - rec.heard.get( i - 1 ).at;
			assertTrue( "on its beat: " + gap, gap >= beatLo - DT );
		}
	}

	@Test
	public void onePipeBedTricklesOrPoursByThePipeItIsNear(){
		int w = 40;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 10 * w] = Terrain.WALL;
		for (int y = 11; y <= 13; y++) for (int x = 1; x < w - 1; x++) m[x + y * w] = Terrain.WATER;
		//a pipe that trickles at one end of the channel and one that pours at the other
		int trickle = -1, pour = -1;
		for (int x = 2; x < 12 && trickle < 0; x++) if (!Source.pours( x + 10 * w )) trickle = x + 10 * w;
		for (int x = 28; x < 38 && pour < 0; x++) if (Source.pours( x + 10 * w )) pour = x + 10 * w;
		assertTrue( trickle >= 0 && pour >= 0 );
		m[trickle] = m[pour] = Terrain.WALL_DECO;
		Voice[] voices = { voice( Place.SEWERS, AmbientSound.TRICKLE ) };
		Voice bed = voices[0];
		assertSame( AmbientSound.POUR, bed.alt.sound );
		Rec rec = new Rec();
		Soundscape s = scape( voices, 14, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, trickle % w + 15 * w, seen( m.length, true ) );
		Soundscape.Air a = new Soundscape.Air();
		//by the first pipe, then along the bank to the other, a cell every half second, and there
		while (s.now < 15f) s.tick( DT, g, a, rec );
		for (int x = trickle % w; x <= pour % w; x++){
			g.hero = x + 15 * w;
			for (float until = s.now + 0.5f; s.now < until; ) s.tick( DT, g, a, rec );
		}
		for (float until = s.now + 15f; s.now < until; ) s.tick( DT, g, a, rec );
		boolean trickled = false, poured = false;
		for (int i = 0; i < rec.heard.size(); i++){
			Heard h = rec.heard.get( i );
			assertTrue( h.cell == trickle || h.cell == pour );
			//each pipe its own sound, its lead and its beat
			assertSame( h.cell == pour ? bed.alt : bed, h.v );
			if (h.cell == pour) poured = true;
			else trickled = true;
			if (i == 0) continue;
			Heard last = rec.heard.get( i - 1 );
			float beat = (last.v.length - last.v.overlap) / last.pitch * (1f - Soundscape.BED_JITTER);
			assertTrue( "never over the last play's beat: " + (h.at - last.at), h.at - last.at >= beat - DT );
		}
		assertTrue( trickled && poured );
		assertEquals( pour, rec.heard.get( rec.heard.size() - 1 ).cell );
	}

	@Test
	public void aSourceInTheRockIsHeardFromItsFaceAndWhatIsSeenClearly(){
		int w = 12;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 5 * w] = Terrain.WALL;
		int torch = 6 + 5 * w;
		m[torch] = Terrain.WALL_DECO;
		boolean[] sight = seen( m.length, false );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 9 + 6 * w, sight );
		assertEquals( "the air", 1f, Soundscape.muffle( g, -1 ), 0f );
		//along its wall, in the dark: clear, though a straight line to it clips the wall beside it
		assertTrue( Soundscape.behindRock( m, w, g.hero, torch ) );
		assertEquals( 1f, Soundscape.muffle( g, torch ), 0f );
		g.hero = 6 + 9 * w;
		assertEquals( "before it", 1f, Soundscape.muffle( g, torch ), 0f );
		//behind the wall it is in: muffled, nothing else between, even with the wall in sight
		g.hero = 6 + 2 * w;
		assertFalse( Soundscape.behindRock( m, w, g.hero, torch ) );
		assertEquals( Soundscape.MUFFLED, Soundscape.muffle( g, torch ), 0f );
		for (int c = 0; c < 6 * w; c++) sight[c] = true;
		assertEquals( Soundscape.MUFFLED, Soundscape.muffle( g, torch ), 0f );
		//what the hero sees he hears clearly, whatever a straight line to it clips; unseen, muffled
		Arrays.fill( sight, false );
		g.hero = 2 + 7 * w;
		m[3 + 8 * w] = Terrain.WALL;
		int mouse = 5 + 10 * w;
		assertTrue( Soundscape.behindRock( m, w, g.hero, mouse ) );
		assertEquals( Soundscape.MUFFLED, Soundscape.muffle( g, mouse ), 0f );
		sight[mouse] = true;
		assertEquals( 1f, Soundscape.muffle( g, mouse ), 0f );
	}

	@Test
	public void aTorchInSightIsHeardClearAlongItsWall(){
		//a corridor a cell wide, a torch in its north wall
		int w = 30;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.WALL );
		for (int x = 1; x < w - 1; x++) m[x + 11 * w] = Terrain.EMPTY;
		int torch = 10 + 10 * w;
		m[torch] = Terrain.WALL_DECO;
		Voice crackle = new Voice( AmbientSound.CRACKLE ).bed( 4f, 1.05f ).from( Source.TORCH );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ crackle }, 3, rec );
		//the hero three cells along, the torch in sight: a straight line to it clips the wall beside it
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 13 + 11 * w, seen( m.length, true ) );
		assertTrue( Soundscape.behindRock( m, w, g.hero, torch ) );
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 12f) s.tick( DT, g, a, rec );
		assertFalse( rec.heard.isEmpty() );
		for (Heard h : rec.heard){
			assertEquals( torch, h.cell );
			assertEquals( "clear", Soundscape.reach( g.away( torch ), 4f ), h.level, 1e-4f );
		}
	}

	@Test
	public void aPipeIsMuffledFromTheRoomBehindItsWall(){
		int w = 30;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 10 * w] = Terrain.WALL;
		for (int y = 11; y <= 13; y++) for (int x = 1; x < w - 1; x++) m[x + y * w] = Terrain.WATER;
		int pipe = 12 + 10 * w;
		m[pipe] = Terrain.WALL_DECO;
		Voice bed = new Voice( AmbientSound.TRICKLE ).bed( 6f, 1.45f ).from( Source.PIPE );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ bed }, 12, rec );
		//the hero in the room behind the wall, four cells north of the pipe: he sees his room and
		//the wall, never the water under the pipe
		boolean[] sight = new boolean[m.length];
		for (int c = 0; c < 11 * w; c++) sight[c] = true;
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 12 + 6 * w, sight );
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 12f) s.tick( DT, g, a, rec );
		assertFalse( rec.heard.isEmpty() );
		for (Heard h : rec.heard){
			assertEquals( pipe, h.cell );
			assertEquals( "through the wall", Soundscape.MUFFLED * Soundscape.reach( 4f, 6f ), h.level, 1e-4f );
		}
	}

	@Test
	public void aBedFollowsTheNearestSource(){
		int w = 30;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 10 * w] = Terrain.WALL;
		for (int y = 11; y <= 13; y++) for (int x = 1; x < w - 1; x++) m[x + y * w] = Terrain.WATER;
		//both in reach of the hero, one nearer
		int far = 7 + 10 * w, near = 12 + 10 * w;
		m[far] = m[near] = Terrain.WALL_DECO;
		Voice bed = new Voice( AmbientSound.POUR ).bed( 6f, 1.75f ).from( Source.PIPE );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ bed }, 9, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 11 + 14 * w, seen( m.length, true ) );
		assertTrue( g.away( far ) <= 6f && g.away( near ) < g.away( far ) );
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 12f) s.tick( DT, g, a, rec );
		assertEquals( "the nearer pipe", near, s.bedAt[0] );
		assertEquals( near, rec.heard.get( rec.heard.size() - 1 ).cell );
	}

	@Test
	public void aBedWaitsForAVoiceAndHoldsTheOthersBack(){
		int w = 30;
		int[] m = new int[w * w];
		Arrays.fill( m, Terrain.EMPTY );
		for (int x = 0; x < w; x++) m[x + 10 * w] = Terrain.WALL;
		for (int y = 11; y <= 13; y++) for (int x = 1; x < w - 1; x++) m[x + y * w] = Terrain.WATER;
		int pipe = 10 + 10 * w;
		m[pipe] = Terrain.WALL_DECO;
		//three loud far-off voices from the air, always due, and a bed far from its pipe
		Voice[] voices = {
				new Voice( AmbientSound.WIND ).every( 0.5f ),
				new Voice( AmbientSound.RUMBLE ).every( 0.5f ),
				new Voice( AmbientSound.MOAN ).every( 0.5f ),
				new Voice( AmbientSound.TRICKLE ).bed( 6f, 1.45f ).from( Source.PIPE )
		};
		Rec rec = new Rec();
		Soundscape s = scape( voices, 4, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 25 + 25 * w, seen( m.length, true ) );
		Soundscape.Air a = new Soundscape.Air();
		while ((s.now < 10f || s.sounding() < Soundscape.MAX_VOICES) && s.now < 60f) s.tick( DT, g, a, rec );
		assertEquals( Soundscape.MAX_VOICES, s.sounding() );
		//the hero comes to the pipe: once the bed has found it, it waits for a voice to fall
		//silent, and nothing else starts in the meantime
		g.hero = 10 + 14 * w;
		float came = s.now;
		while (s.bedAt[3] < 0 && s.now < came + 5f) s.tick( DT, g, a, rec );
		assertEquals( pipe, s.bedAt[3] );
		float found = s.now;
		int before = rec.heard.size();
		while (!s.on[3] && s.now < found + 10f){
			s.tick( DT, g, a, rec );
			for (int i = before; i < rec.heard.size(); i++) assertTrue( rec.heard.get( i ).v.bed );
		}
		assertTrue( "the bed got a voice", s.on[3] );
		assertTrue( s.sounding() <= Soundscape.MAX_VOICES );
	}

	@Test
	public void aFrogsChorusSingsFromSeveralBanksAtSeveralPitches(){
		int w = 40;
		int[] m = sewer();
		Voice frog = voice( Place.SEWERS, AmbientSound.FROG );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ frog }, 21, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 20 + 20 * w, seen( m.length, true ) );
		Soundscape.Air a = night();
		while (s.now < 1800f) s.tick( DT, g, a, rec );
		//a sound and those a breath after it are one song: a chorus's frogs come under a second
		//apart, the next song not before the last has rung out
		ArrayList<ArrayList<Heard>> songs = new ArrayList<>();
		for (Heard h : rec.heard){
			ArrayList<Heard> last = songs.isEmpty() ? null : songs.get( songs.size() - 1 );
			if (last != null && h.at - last.get( last.size() - 1 ).at < 1.2f) last.add( h );
			else {
				ArrayList<Heard> song = new ArrayList<>();
				song.add( h );
				songs.add( song );
			}
		}
		int choruses = 0;
		for (ArrayList<Heard> song : songs){
			assertTrue( "three frogs at most", song.size() <= 3 );
			if (song.size() < 2) continue;
			choruses++;
			HashSet<Integer> cells = new HashSet<>();
			HashSet<Float> pitches = new HashSet<>();
			for (int i = 0; i < song.size(); i++){
				Heard h = song.get( i );
				assertTrue( "on a bank", Source.shore( m, w, h.cell ) );
				cells.add( h.cell );
				pitches.add( h.pitch );
				if (i > 0) assertTrue( "staggered", h.at - song.get( i - 1 ).at >= Soundscape.CHORUS_GAP_MIN - 1e-4f );
			}
			assertEquals( "each from its own bank", song.size(), cells.size() );
			assertEquals( "each at its own pitch", song.size(), pitches.size() );
		}
		assertTrue( "now and then a chorus: " + choruses + " of " + songs.size(),
				choruses > songs.size() / 8 && choruses < songs.size() / 2 );
	}

	@Test
	public void theCavesThrowEveryDropBack(){
		int w = 40;
		int[] m = sewer();
		Voice drip = voice( Place.CAVES, AmbientSound.DRIP );
		assertTrue( drip.echo );
		assertTrue( "takes to tell apart", drip.takes.length > 1 );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ drip }, 2, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 20 + 20 * w, seen( m.length, true ) );
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 300f) s.tick( DT, g, a, rec );
		int drops = 0;
		HashSet<Integer> takes = new HashSet<>();
		for (int i = 0; i + 1 < rec.heard.size(); i++){
			Heard h = rec.heard.get( i );
			if (h.echo) continue;
			drops++;
			takes.add( h.take );
			Heard e = rec.heard.get( i + 1 );
			assertTrue( "its echo follows", e.echo );
			assertEquals( h.cell, e.cell );
			//the very drop thrown back: its take, quieter and a touch lower, nothing picked anew
			assertEquals( "the same take", h.take, e.take );
			assertEquals( h.level * Soundscape.ECHO_LEVEL, e.level, 1e-4f );
			assertEquals( h.pitch * Soundscape.ECHO_PITCH, e.pitch, 1e-4f );
			float lag = e.at - h.at;
			assertTrue( "a moment later: " + lag, lag >= Soundscape.ECHO_MIN - 1e-4f && lag <= Soundscape.ECHO_MAX + DT );
		}
		assertTrue( drops > 20 );
		assertEquals( "every take is heard", drip.takes.length, takes.size() );
	}

	@Test
	public void theDarksVoicesComeOnlyFromTheDark(){
		int w = 40;
		int[] m = sewer();
		Voice rat = voice( Place.SEWERS, AmbientSound.RAT );
		assertTrue( rat.unseen );
		boolean[] sight = seen( m.length, true );
		Rec rec = new Rec();
		Soundscape s = scape( new Voice[]{ rat }, 8, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 20 + 21 * w, sight );
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 400f) s.tick( DT, g, a, rec );
		assertTrue( "everything in sight: no rat", rec.heard.isEmpty() );
		//the hero, below the channel, sees only round himself: the rats are heard from out
		//there, muffled when there is rock between (the channel's wall)
		Arrays.fill( sight, false );
		for (int y = 18; y <= 24; y++) for (int x = 17; x <= 23; x++) sight[x + y * w] = true;
		while (s.now < 1200f) s.tick( DT, g, a, rec );
		assertTrue( rec.heard.size() > 5 );
		boolean clear = false, walled = false;
		for (Heard h : rec.heard){
			assertFalse( sight[h.cell] );
			float d = g.away( h.cell );
			assertTrue( "far off: " + d, d >= rat.near - 0.75f && d <= rat.far + 0.75f );
			boolean rock = Soundscape.behindRock( m, w, g.hero, h.cell );
			assertEquals( rat.level * (rock ? Soundscape.MUFFLED : 1f), h.level, 1e-4f );
			if (rock) walled = true;
			else clear = true;
		}
		assertTrue( "from the open floor and from behind the channel's wall", clear && walled );
	}

	@Test
	public void theAirIsHeardCentredAndASourceFromItsCell(){
		int w = 40;
		int[] m = sewer();
		Voice[] caves = AmbientSounds.voices( Place.CAVES );
		Rec rec = new Rec();
		Soundscape s = scape( caves, 13, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 20 + 20 * w, seen( m.length, true ) );
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 1500f) s.tick( DT, g, a, rec );
		boolean rumbled = false;
		for (Heard h : rec.heard){
			if (h.v.sound == AmbientSound.RUMBLE){
				rumbled = true;
				assertEquals( "the rumble is the air's", -1, h.cell );
			} else if (h.v.sound == AmbientSound.DRIP){
				assertTrue( Source.drip( m, w, h.cell ) );
			} else if (h.v.sound == AmbientSound.PEBBLES || h.v.sound == AmbientSound.BAT){
				assertTrue( Source.rockFace( m, w, h.cell ) );
			}
		}
		assertTrue( rumbled );
	}

	@Test
	public void aVoiceKeepsItsHours(){
		int w = 40;
		int[] m = sewer();
		Voice[] sewers = AmbientSounds.voices( Place.SEWERS );
		Rec rec = new Rec();
		Soundscape s = scape( sewers, 17, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 9 + 20 * w, seen( m.length, true ) );
		Soundscape.Air a = new Soundscape.Air();
		a.phase = Phase.DAY;
		a.depth = 3;
		while (s.now < 900f) s.tick( DT, g, a, rec );
		HashSet<AmbientSound> sounds = new HashSet<>();
		for (Heard h : rec.heard) sounds.add( h.v.sound );
		assertFalse( "no frogs by day", sounds.contains( AmbientSound.FROG ) );
		assertFalse( "no crickets by day", sounds.contains( AmbientSound.CRICKET ) );
		assertTrue( "a fly by day", sounds.contains( AmbientSound.FLY ) );
		assertFalse( "no birdsong on the third floor", sounds.contains( AmbientSound.BIRD ) );
		a.depth = 1;
		while (s.now < 2400f) s.tick( DT, g, a, rec );
		sounds.clear();
		for (Heard h : rec.heard) sounds.add( h.v.sound );
		assertTrue( "birdsong through the grates of the first floor", sounds.contains( AmbientSound.BIRD ) );
	}

	@Test
	public void aSilenceStopsEverythingAndStartsOverAfterAQuiet(){
		int w = 40;
		int[] m = sewer();
		Rec rec = new Rec();
		Soundscape s = scape( AmbientSounds.voices( Place.CAVES ), 23, rec );
		Soundscape.Ground g = new Soundscape.Ground().set( m, w, w, 20 + 16 * w, seen( m.length, true ) );
		Soundscape.Air a = new Soundscape.Air();
		while (s.now < 60f) s.tick( DT, g, a, rec );
		assertTrue( "the stream by the channel", s.on[4] );
		for (int f = 0; f < 300; f++) s.hush( DT );
		assertFalse( "the beds stop", s.on[4] );
		int before = rec.heard.size();
		float back = s.now;
		while (s.now < back + Soundscape.QUIET_MIN - DT) s.tick( DT, g, a, rec );
		assertEquals( "nothing in the quiet, the echoes to come dropped", before, rec.heard.size() );
		while (s.now < back + 30f) s.tick( DT, g, a, rec );
		assertTrue( rec.heard.size() > before );
		assertNotEquals( 0, s.sounding() );
	}
}
