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

import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherScape.Where;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;

import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound.*;
import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherScape.LEFT;
import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherScape.NEAR;
import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherScape.ONE_SHOT;
import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherScape.RIGHT;
import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherScape.WIND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The weather's scheduler (WeatherScape), driven frame by frame on hand-built ground with its own
 * dice: the bed of what falls by its type and rate, rising through its band, laid without a gap
 * by two emitters on their own beats with different takes, eased through the climate's steps;
 * the rain on the nearest water over a tree, following the hero; the gale and its gusts from the
 * wind's side; thunder one roll at a time, a near crack breaking in over a far roll; indoors only
 * the roof, the caves only a far roll, the deep nothing; never more than six ringing; the hush.
 */
public class WeatherScapeTest {

	private boolean wasOn;

	//the classic ambience curve, the one every pan here is bounded by (spatial sound off)
	@Before
	public void classicCurve(){
		wasOn = SpatialSound.on;
		SpatialSound.on = false;
	}

	@After
	public void restore(){
		SpatialSound.on = wasOn;
	}

	private static final float DT = 1f / 30f;
	private static final int W = 40;

	/** One sound as the scheduler sent it out. */
	private static final class Heard {
		final int bed;
		final AmbientSound s;
		final int take;
		final float at, level, pitch, pan;

		Heard( int bed, AmbientSound s, int take, float at, float level, float pitch, float pan ){
			this.bed = bed;
			this.s = s;
			this.take = take;
			this.at = at;
			this.level = level;
			this.pitch = pitch;
			this.pan = pan;
		}

		float end(){
			return at + s.length / pitch;
		}

		//when its bed's next play is due at the latest, its beat jittered as far as it goes
		float latestNext(){
			return at + (s.length - s.lead) / pitch * (1f + Soundscape.BED_JITTER);
		}

		float earliestNext(){
			return at + (s.length - s.lead) / pitch * (1f - Soundscape.BED_JITTER);
		}
	}

	private static final class Rec implements WeatherScape.Out {
		WeatherScape s;
		final ArrayList<Heard> heard = new ArrayList<>();

		@Override
		public void play( int bed, AmbientSound sound, int take, float level, float pitch, float pan ){
			assertTrue( "one of its takes", take >= 0 && take < sound.takes.length );
			assertTrue( "a level", level > 0f && level <= 1f );
			assertTrue( "a pan", Math.abs( pan ) <= AmbientPlayer.MAX_PAN );
			heard.add( new Heard( bed, sound, take, s.now, level, pitch, pan ) );
		}

		ArrayList<Heard> of( int bed ){
			ArrayList<Heard> out = new ArrayList<>();
			for (Heard h : heard) if (h.bed == bed) out.add( h );
			return out;
		}

		ArrayList<Heard> of( AmbientSound s ){
			ArrayList<Heard> out = new ArrayList<>();
			for (Heard h : heard) if (h.s == s) out.add( h );
			return out;
		}

		HashSet<AmbientSound> sounds(){
			HashSet<AmbientSound> out = new HashSet<>();
			for (Heard h : heard) out.add( h.s );
			return out;
		}
	}

	private static WeatherScape scape( long seed, Rec rec ){
		WeatherScape s = new WeatherScape( new Random( seed ) );
		rec.s = s;
		return s;
	}

	private static void sky( WeatherScape s, Where where, PrecipType type, float rate, float wind, float dir ){
		s.sky.where = where;
		s.sky.type = type;
		s.sky.rate = rate;
		s.sky.wind = wind;
		s.sky.windDir = dir;
	}

	//an open field of grass, W x W
	private static int[] field(){
		int[] m = new int[W * W];
		Arrays.fill( m, Terrain.GRASS );
		return m;
	}

	private static Soundscape.Ground ground( int[] map, int hero ){
		return new Soundscape.Ground().set( map, W, W, hero, null );
	}

	//the visuals' gusting (WeatherSprites.gust) on the scape's clock
	private static float gust( float t ){
		return (float)(0.6 * Math.sin( t * 0.37 ) + 0.3 * Math.sin( t * 1.13 + 1.7 ) + 0.1 * Math.sin( t * 3.1 + 0.4 ));
	}

	private static void run( WeatherScape s, Soundscape.Ground g, Rec rec, float secs ){
		for (float t = 0f; t < secs; t += DT) s.tick( DT, g, gust( s.now ), rec );
	}

	// ------------------------------------------------------------ the rules

	@Test
	public void theBedOfWhatFallsByItsTypeAndRate(){
		for (Where w : new Where[]{ Where.OPEN, Where.EXPOSED }){
			assertNull( "under a hundredth, nothing", WeatherScape.fall( w, PrecipType.RAIN, 0.009f ) );
			assertSame( RAIN_LIGHT, WeatherScape.fall( w, PrecipType.RAIN, 0.01f ) );
			assertSame( RAIN_LIGHT, WeatherScape.fall( w, PrecipType.RAIN, 0.149f ) );
			assertSame( RAIN, WeatherScape.fall( w, PrecipType.RAIN, 0.15f ) );
			assertSame( RAIN, WeatherScape.fall( w, PrecipType.RAIN, 0.349f ) );
			assertSame( RAIN_HEAVY, WeatherScape.fall( w, PrecipType.RAIN, 0.35f ) );
			assertSame( RAIN_HEAVY, WeatherScape.fall( w, PrecipType.RAIN, 1f ) );
			assertSame( SLEET, WeatherScape.fall( w, PrecipType.SLEET, 0.2f ) );
			assertSame( HAIL, WeatherScape.fall( w, PrecipType.HAIL, 0.2f ) );
			assertSame( SNOW, WeatherScape.fall( w, PrecipType.SNOW, 0.2f ) );
			assertSame( BLIZZARD, WeatherScape.fall( w, PrecipType.BLIZZARD, 0.2f ) );
			assertNull( WeatherScape.fall( w, PrecipType.NONE, 0.5f ) );
		}
		//indoors the rain, the sleet and the hail drum on the roof; the snow lands without a sound
		assertSame( RAIN_ROOF, WeatherScape.fall( Where.INDOORS, PrecipType.RAIN, 0.05f ) );
		assertSame( RAIN_ROOF, WeatherScape.fall( Where.INDOORS, PrecipType.SLEET, 0.05f ) );
		assertSame( RAIN_ROOF, WeatherScape.fall( Where.INDOORS, PrecipType.HAIL, 0.05f ) );
		assertNull( WeatherScape.fall( Where.INDOORS, PrecipType.SNOW, 0.5f ) );
		assertNull( WeatherScape.fall( Where.INDOORS, PrecipType.BLIZZARD, 0.5f ) );
		assertNull( WeatherScape.fall( Where.INDOORS, PrecipType.RAIN, 0.005f ) );
		for (PrecipType t : PrecipType.values()){
			assertNull( "nothing falls in the caves", WeatherScape.fall( Where.CAVE, t, 1f ) );
			assertNull( "nor down deep", WeatherScape.fall( Where.SILENT, t, 1f ) );
		}
		//every one of them is a bed, with a lead
		for (Where w : Where.values()){
			for (PrecipType t : PrecipType.values()){
				AmbientSound s = WeatherScape.fall( w, t, 0.3f );
				assertTrue( s == null || s.lead > 0f );
			}
		}
	}

	@Test
	public void aBedRisesThroughItsBandNeverSteppingUpIntoTheNext(){
		assertEquals( "about half in a drizzle", 0.45f, WeatherScape.intensity( RAIN_LIGHT, 0.01f ), 1e-4f );
		assertEquals( 1f, WeatherScape.intensity( RAIN_LIGHT, WeatherScape.LIGHT_TOP ), 1e-4f );
		assertEquals( 1f, WeatherScape.intensity( RAIN, WeatherScape.STEADY_TOP ), 1e-4f );
		assertEquals( 1f, WeatherScape.intensity( RAIN_HEAVY, WeatherScape.HEAVY_TOP ), 1e-4f );
		assertEquals( 1f, WeatherScape.intensity( RAIN_HEAVY, 1f ), 1e-4f );
		//a heavier rain comes in 3 dB under its full: where the lighter one was at its full, its
		//gain 3 dB under the heavier's (docs/weather-sounds-research.md 6.3)
		assertEquals( WeatherScape.NEXT_LEVEL, WeatherScape.intensity( RAIN, WeatherScape.LIGHT_TOP ), 1e-4f );
		assertEquals( WeatherScape.NEXT_LEVEL, WeatherScape.intensity( RAIN_HEAVY, WeatherScape.STEADY_TOP ), 1e-4f );
		assertEquals( -3f, 20f * (float)Math.log10( WeatherScape.NEXT_LEVEL ), 0.05f );
		for (AmbientSound s : new AmbientSound[]{ RAIN_LIGHT, RAIN, RAIN_HEAVY, SNOW, SLEET, HAIL, BLIZZARD, RAIN_ROOF }){
			float last = 0f;
			for (float r = 0.01f; r <= 1f; r += 0.01f){
				float l = WeatherScape.intensity( s, r );
				assertTrue( s + " at " + r, l >= last && l <= 1f && l >= WeatherScape.FIRST_LEVEL );
				last = l;
			}
		}
	}

	@Test
	public void theWindsSideIsWhereItComesFrom(){
		//blowing toward the east (90), it comes from the west: the left
		assertEquals( -1f, WeatherScape.side( 90f ), 1e-5f );
		assertEquals( 1f, WeatherScape.side( 270f ), 1e-5f );
		assertEquals( 0f, WeatherScape.side( 0f ), 1e-5f );
		assertEquals( 0f, WeatherScape.side( 180f ), 1e-5f );
		assertEquals( -WeatherScape.WIND_PAN, WeatherScape.windPan( WeatherScape.side( 90f ) ), 1e-5f );
		assertEquals( 0.4f, WeatherScape.galeLevel( 10f, false ), 1e-5f );
		assertEquals( 1f, WeatherScape.galeLevel( 18f, false ), 1e-5f );
		assertEquals( 1f, WeatherScape.galeLevel( 30f, false ), 1e-5f );
		assertEquals( 0.4f * 1.15f, WeatherScape.galeLevel( 10f, true ), 1e-5f );
		assertEquals( "harder on the peaks, never over full", 1f, WeatherScape.galeLevel( 17f, true ), 1e-5f );
		assertEquals( 0.5f, WeatherScape.gustLevel( 7f ), 1e-5f );
		assertEquals( 1f, WeatherScape.gustLevel( 16f ), 1e-5f );
		assertNull( "too calm for a gale", WeatherScape.wind( Where.OPEN, null, 9.9f, false ) );
		assertSame( GALE, WeatherScape.wind( Where.OPEN, null, 10f, false ) );
		assertSame( GALE, WeatherScape.wind( Where.OPEN, RAIN_HEAVY, 20f, false ) );
		assertSame( "the desert's wind is sand", SANDSTORM, WeatherScape.wind( Where.OPEN, null, 12f, true ) );
		assertNull( "a blizzard is its own wind", WeatherScape.wind( Where.OPEN, BLIZZARD, 20f, false ) );
		for (Where w : new Where[]{ Where.EXPOSED, Where.INDOORS, Where.CAVE, Where.SILENT }){
			assertNull( "no wind out of the open: " + w, WeatherScape.wind( w, null, 25f, false ) );
		}
	}

	// ------------------------------------------------------------ the beds

	@Test
	public void eachEmitterLaysItsBedWithoutAGapAndCrossesIntoANewKindOnItsBeat(){
		Rec rec = new Rec();
		WeatherScape s = scape( 1, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.25f, 2f, 0f );
		run( s, g, rec, 300f );
		//the rain gets heavier: each emitter changes over at its next play
		s.sky.rate = 0.5f;
		run( s, g, rec, 300f );
		for (int bed : new int[]{ LEFT, RIGHT }){
			ArrayList<Heard> plays = rec.of( bed );
			assertTrue( plays.size() > 100 );
			for (int i = 1; i < plays.size(); i++){
				Heard last = plays.get( i - 1 ), next = plays.get( i );
				assertTrue( "no gap: " + next.at + " after " + last.at, next.at <= last.latestNext() + DT + 1e-4f );
				//the right's second play comes half a step after the left's first: its first play
				//came in just after the left's, so the two are never one ear alone
				if (bed != RIGHT || i > 1) assertTrue( "on its beat, never an extra play", next.at >= last.earliestNext() - 1e-4f );
				//the next starts well before the last ends: they cross over its fade
				assertTrue( last.end() - next.at > 0.7f );
				assertEquals( bed == LEFT ? -WeatherScape.FALL_PAN : WeatherScape.FALL_PAN, next.pan, 0f );
			}
			assertSame( RAIN, plays.get( 0 ).s );
			assertSame( RAIN_HEAVY, plays.get( plays.size() - 1 ).s );
		}
	}

	@Test
	public void theTwoSidesPlayDifferentTakesHalfAStepApart(){
		for (AmbientSound bed : new AmbientSound[]{ RAIN_LIGHT, RAIN, RAIN_HEAVY, BLIZZARD, SNOW, HAIL }){
			PrecipType type = bed == SNOW ? PrecipType.SNOW : bed == HAIL ? PrecipType.HAIL
					: bed == BLIZZARD ? PrecipType.BLIZZARD : PrecipType.RAIN;
			float rate = bed == RAIN_LIGHT ? 0.08f : bed == RAIN ? 0.25f : 0.45f;
			Rec rec = new Rec();
			WeatherScape s = scape( 2, rec );
			Soundscape.Ground g = ground( field(), 20 + 20 * W );
			sky( s, Where.OPEN, type, rate, 0f, 0f );
			run( s, g, rec, 240f );
			ArrayList<Heard> left = rec.of( LEFT ), right = rec.of( RIGHT );
			assertTrue( left.size() > 30 && right.size() > 30 );
			//the right comes in a moment after the left, both sides at once (never one ear
			//alone), and again half a step after the left's start: the pair never runs in line
			float step = bed.length - bed.lead;
			assertEquals( bed.toString(), WeatherScape.JOIN_LAG, right.get( 0 ).at - left.get( 0 ).at, DT + 1e-4f );
			assertEquals( bed.toString(), step / 2f, right.get( 1 ).at - left.get( 0 ).at, 0.1f * step );
			for (Heard h : rec.heard){
				//a far roll now and then in the heavy rain aside
				if (h.bed == ONE_SHOT) continue;
				assertSame( bed, h.s );
				if (bed.takes.length == 1) continue;
				//never the take the other side is playing
				Heard other = null;
				for (Heard o : rec.heard) if (o.bed != h.bed && o.bed != ONE_SHOT && o.at <= h.at && h.at < o.end()) other = o;
				if (other != null) assertNotEquals( bed + " at " + h.at, other.take, h.take );
			}
			if (bed.takes.length > 2){
				HashSet<Integer> takes = new HashSet<>();
				for (Heard h : left) takes.add( h.take );
				assertEquals( "every take comes up", bed.takes.length, takes.size() );
			}
		}
	}

	@Test
	public void theTwoSidesStayHalfAStepApartForGood(){
		//one take on both sides: were they to drift into line, the same file twice would phase
		for (long seed = 30; seed < 35; seed++){
			Rec rec = new Rec();
			WeatherScape s = scape( seed, rec );
			sky( s, Where.OPEN, PrecipType.SNOW, 0.3f, 0f, 0f );
			run( s, ground( field(), 20 + 20 * W ), rec, 1800f );
			ArrayList<Heard> left = rec.of( LEFT ), right = rec.of( RIGHT );
			assertTrue( right.size() > 400 );
			int j = 0;
			for (int i = 2; i < right.size(); i++){
				float t = right.get( i ).at;
				while (j + 1 < left.size() && left.get( j + 1 ).at <= t) j++;
				if (j + 1 >= left.size()) break;
				float phase = (t - left.get( j ).at) / (left.get( j + 1 ).at - left.get( j ).at);
				assertTrue( "seed " + seed + " at " + t + ": " + phase, phase >= 0.25f && phase <= 0.75f );
			}
		}
	}

	@Test
	public void nothingUnderAHundredthAndNothingBeforeTheQuietEnds(){
		Rec rec = new Rec();
		WeatherScape s = scape( 3, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.009f, 0f, 0f );
		run( s, g, rec, 60f );
		assertTrue( rec.heard.isEmpty() );

		rec = new Rec();
		s = scape( 3, rec );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.3f, 0f, 0f );
		run( s, g, rec, 10f );
		float first = rec.heard.get( 0 ).at;
		assertTrue( "after a quiet: " + first, first >= Soundscape.QUIET_MIN && first <= Soundscape.QUIET_MAX + DT );
	}

	@Test
	public void theClimatesStepsAreEased(){
		Rec rec = new Rec();
		WeatherScape s = scape( 4, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.1f, 4f, 90f );
		run( s, g, rec, 5f );
		//taken as it is on a fresh start
		assertEquals( 0.1f, s.rate, 1e-4f );
		assertEquals( 4f, s.wind, 1e-4f );
		assertEquals( -1f, s.side, 1e-4f );
		s.sky.rate = 0.5f;
		s.sky.wind = 20f;
		s.sky.windDir = 270f;
		run( s, g, rec, 1f );
		assertTrue( "on its way: " + s.rate, s.rate > 0.25f && s.rate < 0.35f );
		assertTrue( s.wind > 9f && s.wind < 13f );
		assertTrue( "turning through the middle: " + s.side, s.side > -0.3f && s.side < 0.3f );
		run( s, g, rec, 12f );
		assertEquals( 0.5f, s.rate, 0.01f );
		assertEquals( 20f, s.wind, 0.2f );
		assertEquals( 1f, s.side, 0.01f );
	}

	@Test
	public void aSnowFlickeringIntoABlizzardAndBackKeepsOneBedAndItsGale(){
		Rec rec = new Rec();
		WeatherScape s = scape( 20, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.SNOW, 0.2f, 10.5f, 90f );
		//the climate's coin toss around 10 m/s: SNOW and BLIZZARD a second each, five minutes,
		//the last second a SNOW
		for (int sec = 0; sec <= 300; sec++){
			s.sky.type = sec % 2 == 0 ? PrecipType.SNOW : PrecipType.BLIZZARD;
			run( s, g, rec, 1f );
		}
		for (int bed : new int[]{ LEFT, RIGHT }){
			for (Heard h : rec.of( bed )) assertSame( "one bed on both sides", SNOW, h.s );
		}
		//and the gale under it never stops
		ArrayList<Heard> gale = rec.of( WIND );
		assertTrue( gale.size() > 60 );
		for (int i = 1; i < gale.size(); i++){
			assertTrue( "no gap in the gale", gale.get( i ).at <= gale.get( i - 1 ).latestNext() + DT + 1e-4f );
		}

		//a blizzard that holds is taken, at the next plays, and the gale gives way to it
		rec.heard.clear();
		s.sky.type = PrecipType.BLIZZARD;
		float from = s.now;
		run( s, g, rec, 30f );
		for (Heard h : rec.heard){
			if (h.bed == ONE_SHOT) continue;
			if (h.at < from + WeatherScape.TYPE_HOLD) assertNotEquals( BLIZZARD, h.s );
		}
		assertSame( BLIZZARD, rec.of( LEFT ).get( rec.of( LEFT ).size() - 1 ).s );
		assertSame( BLIZZARD, rec.of( RIGHT ).get( rec.of( RIGHT ).size() - 1 ).s );
		for (Heard h : rec.of( WIND )) assertTrue( h.at < from + WeatherScape.TYPE_HOLD + DT );

		//what starts to fall is taken at once (its rate eases in from nothing)
		rec = new Rec();
		s = scape( 21, rec );
		sky( s, Where.OPEN, PrecipType.NONE, 0f, 0f, 0f );
		run( s, g, rec, 10f );
		assertTrue( rec.heard.isEmpty() );
		s.sky.type = PrecipType.RAIN;
		s.sky.rate = 0.3f;
		from = s.now;
		run( s, g, rec, 2f );
		assertFalse( rec.of( LEFT ).isEmpty() );
		assertTrue( rec.of( LEFT ).get( 0 ).at < from + 0.5f );
	}

	@Test
	public void whatFallsIsHeardFromTheGroundItFallsOn(){
		//the overworld snowed under left of column 20, thawed from it on
		final int edge = 20;
		OverworldLevel world = new OverworldLevel( 0 ){
			@Override public boolean frozenAt( int cell ){
				return cell % W < edge;
			}
		};
		Object[][] kinds = {
				//what falls, its bed, whether it falls on the left, on the right (the hero stands
				//on the edge: his own column, thawed, is heard on both sides)
				{ PrecipType.RAIN, RAIN, false, true },
				{ PrecipType.SNOW, SNOW, true, false },
				{ PrecipType.SLEET, SLEET, true, true },
				{ PrecipType.HAIL, HAIL, true, true },
		};
		for (Object[] k : kinds){
			Rec rec = new Rec();
			WeatherScape s = scape( 24, rec );
			Soundscape.Ground g = ground( field(), edge + 20 * W );
			g.world = world;
			sky( s, Where.OPEN, (PrecipType) k[0], 0.3f, 0f, 0f );
			run( s, g, rec, 60f );
			AmbientSound bed = (AmbientSound) k[1];
			float full = WeatherScape.intensity( bed, 0.3f );
			for (int side : new int[]{ LEFT, RIGHT }){
				boolean falls = (Boolean) k[side == LEFT ? 2 : 3];
				ArrayList<Heard> plays = rec.of( side );
				if (falls) assertFalse( bed + " on " + side, plays.isEmpty() );
				for (Heard h : plays){
					assertSame( bed, h.s );
					assertTrue( bed + " on " + side + ": " + h.level, falls
							? h.level <= full + 1e-5f && h.level >= 0.75f * full
							: h.level <= 0.2f * full );
				}
			}
		}

		//the hero well out on the snow: no rain at all; a blizzard over thawed ground is no
		//blizzard, but the wind blows, the gale's
		Rec rec = new Rec();
		WeatherScape s = scape( 25, rec );
		Soundscape.Ground g = ground( field(), 8 + 20 * W );
		g.world = world;
		sky( s, Where.OPEN, PrecipType.RAIN, 0.3f, 0f, 0f );
		run( s, g, rec, 60f );
		assertTrue( rec.heard.isEmpty() );
		g.hero = 32 + 20 * W;
		s.sky.type = PrecipType.BLIZZARD;
		s.sky.wind = 14f;
		run( s, g, rec, 60f );
		assertTrue( rec.of( BLIZZARD ).isEmpty() );
		assertTrue( rec.of( WIND ).size() > 5 );
		//walking onto the snow, the blizzard is heard and the gale gives way to it
		rec.heard.clear();
		g.hero = 8 + 20 * W;
		run( s, g, rec, 60f );
		assertFalse( rec.of( BLIZZARD ).isEmpty() );
		assertTrue( rec.of( WIND ).size() <= 1 );
	}

	// ------------------------------------------------------------ the rain near

	@Test
	public void theRainOnTheNearestWaterOverATreeFollowingTheHero(){
		int[] map = field();
		int hero = 20 + 20 * W;
		map[hero + 2] = Terrain.TREE_OAK;
		map[hero - 5] = Terrain.WATER;
		map[hero - 5 + W] = Terrain.WATER;
		Rec rec = new Rec();
		WeatherScape s = scape( 5, rec );
		Soundscape.Ground g = ground( map, hero );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.3f, 0f, 0f );
		run( s, g, rec, 30f );
		ArrayList<Heard> near = rec.of( NEAR );
		assertTrue( near.size() > 5 );
		float rain = WeatherScape.intensity( RAIN, 0.3f );
		for (Heard h : near){
			//the water five cells to the left, though the tree is nearer
			assertSame( RAIN_WATER, h.s );
			assertEquals( AmbientPlayer.pan( -5f ), h.pan, 1e-5f );
			assertEquals( AmbientPlayer.falloff( 5f ) * rain, h.level, 1e-4f );
		}
		assertEquals( hero - 5, s.beds[NEAR].cell );

		//the hero walks off right: the water is out of reach, the leaves are heard
		rec.heard.clear();
		g.hero = hero + 5;
		run( s, g, rec, 30f );
		near = rec.of( NEAR );
		assertTrue( near.size() > 5 );
		Heard last = near.get( near.size() - 1 );
		assertSame( RAIN_LEAVES, last.s );
		assertEquals( AmbientPlayer.pan( -3f ), last.pan, 1e-5f );
		assertEquals( AmbientPlayer.falloff( 3f ) * rain, last.level, 1e-4f );
		//one play, at most, still came off the water as he walked
		assertTrue( rec.of( RAIN_WATER ).size() <= 1 );

		//on to where there is nothing within reach: it stops, and what fell rings out
		rec.heard.clear();
		g.hero = hero + 15;
		run( s, g, rec, 30f );
		assertTrue( rec.of( NEAR ).size() <= 1 );
		assertFalse( "the rain itself goes on", rec.of( LEFT ).isEmpty() );

		//not for a drizzle, nor for snow, nor off the open sky
		for (int k = 0; k < 3; k++){
			rec = new Rec();
			s = scape( 6, rec );
			g = ground( map, hero );
			if (k == 0) sky( s, Where.OPEN, PrecipType.RAIN, 0.04f, 0f, 0f );
			if (k == 1) sky( s, Where.OPEN, PrecipType.SNOW, 0.3f, 0f, 0f );
			if (k == 2) sky( s, Where.EXPOSED, PrecipType.RAIN, 0.3f, 0f, 0f );
			run( s, g, rec, 30f );
			assertFalse( rec.heard.isEmpty() );
			assertTrue( rec.of( NEAR ).isEmpty() );
		}
	}

	@Test
	public void theWindowCarriesTheCellTheRainIsHeardOn(){
		int[] map = field();
		int hero = 20 + 20 * W;
		map[hero - 3] = Terrain.WATER;
		Rec rec = new Rec();
		WeatherScape s = scape( 7, rec );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.3f, 0f, 0f );
		run( s, ground( map, hero ), rec, 10f );
		assertEquals( hero - 3, s.beds[NEAR].cell );
		//the window moves 2 right and 1 down under him
		s.shift( 2, 1, W, W );
		assertEquals( hero - 3 - 2 - W, s.beds[NEAR].cell );
		s.shift( -40, 0, W, W );
		assertEquals( "left behind", -1, s.beds[NEAR].cell );
	}

	// ------------------------------------------------------------ the wind

	@Test
	public void theGaleAndItsGustsComeFromTheWindsSide(){
		for (float dir : new float[]{ 90f, 270f }){
			Rec rec = new Rec();
			WeatherScape s = scape( 8, rec );
			Soundscape.Ground g = ground( field(), 20 + 20 * W );
			sky( s, Where.OPEN, PrecipType.NONE, 0f, 14f, dir );
			run( s, g, rec, 240f );
			//blowing to the east it comes from the west: the left
			float sign = dir == 90f ? -1f : 1f;
			ArrayList<Heard> gale = rec.of( WIND );
			assertTrue( gale.size() > 30 );
			for (Heard h : gale){
				assertSame( GALE, h.s );
				assertEquals( sign * WeatherScape.WIND_PAN, h.pan, 1e-4f );
				assertEquals( WeatherScape.galeLevel( 14f, false ), h.level, 1e-4f );
			}
			ArrayList<Heard> gusts = rec.of( GUST );
			assertTrue( "gusts: " + gusts.size(), gusts.size() >= 10 );
			float last = -100f;
			for (Heard h : gusts){
				assertEquals( ONE_SHOT, h.bed );
				assertTrue( "from the wind's side: " + h.pan, Math.signum( h.pan ) == sign
						&& Math.abs( Math.abs( h.pan ) - WeatherScape.WIND_PAN ) <= WeatherScape.GUST_JITTER + 1e-5f );
				assertEquals( WeatherScape.gustLevel( 14f ), h.level, 1e-4f );
				assertTrue( "as the rain leans over", gust( h.at - DT ) >= WeatherScape.GUST_AT - 0.1f );
				assertTrue( "one at a time: " + (h.at - last), h.at - last >= WeatherScape.GUST_EVERY - 1e-4f );
				last = h.at;
			}
		}

		//too calm for gusts and gale; the peaks blow harder; the desert's wind is sand
		Rec rec = new Rec();
		WeatherScape s = scape( 9, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.NONE, 0f, 6.5f, 90f );
		run( s, g, rec, 120f );
		assertTrue( rec.heard.isEmpty() );
		s.sky.wind = 12f;
		s.sky.peaks = true;
		run( s, g, rec, 60f );
		assertEquals( WeatherScape.galeLevel( 12f, true ), rec.of( WIND ).get( rec.of( WIND ).size() - 1 ).level, 0.01f );
		rec.heard.clear();
		s.sky.peaks = false;
		s.sky.desert = true;
		run( s, g, rec, 30f );
		assertSame( SANDSTORM, rec.of( WIND ).get( rec.of( WIND ).size() - 1 ).s );
		//a blizzard is the wind itself: the gale gives way to it
		rec.heard.clear();
		s.sky.desert = false;
		s.sky.type = PrecipType.BLIZZARD;
		s.sky.rate = 0.3f;
		run( s, g, rec, 30f );
		assertFalse( rec.of( BLIZZARD ).isEmpty() );
		assertTrue( rec.of( WIND ).size() <= 1 );
		//on a dungeon floor what falls is heard, never the wind
		rec = new Rec();
		s = scape( 10, rec );
		sky( s, Where.EXPOSED, PrecipType.RAIN, 0.3f, 25f, 90f );
		run( s, g, rec, 120f );
		assertEquals( EnumSet.of( RAIN ), EnumSet.copyOf( rec.sounds() ) );
	}

	// ------------------------------------------------------------ the thunder

	@Test
	public void thunderOneRollAtATimeANearCrackBreakingInOverAFarRoll(){
		Rec rec = new Rec();
		WeatherScape s = scape( 11, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.NONE, 0f, 0f, 0f );
		s.sky.storm = true;
		run( s, g, rec, 5f );
		assertTrue( rec.heard.isEmpty() );

		//a bolt three tiles to the right: the crack, a moment later, from the right
		float at = s.now;
		s.strike( 3f, 3f, false, false );
		run( s, g, rec, 1.5f );
		assertEquals( 1, rec.heard.size() );
		Heard crack = rec.heard.get( 0 );
		assertSame( THUNDER_NEAR, crack.s );
		assertEquals( WeatherScape.delay( 3f, true ), crack.at - at, DT + 1e-4f );
		assertEquals( WeatherScape.thunderLevel( 3f ), crack.level, 1e-5f );
		assertEquals( AmbientPlayer.pan( 3f ), crack.pan, 1e-5f );
		//while it rolls on, the next strikes' thunder is lost in it, near or far
		s.strike( -2f, 2f, false, false );
		s.strike( 0f, 15f, true, false );
		run( s, g, rec, 4f );
		assertEquals( 1, rec.heard.size() );

		//rung out: sheet lightning's far roll, centred, dull
		run( s, g, rec, 3f );
		s.strike( 0f, 15f, true, false );
		run( s, g, rec, 3.5f );
		assertEquals( 2, rec.heard.size() );
		Heard far = rec.heard.get( 1 );
		assertSame( THUNDER_FAR, far.s );
		assertEquals( 0f, far.pan, 0f );
		assertEquals( WeatherScape.thunderLevel( 15f ), far.level, 1e-5f );
		//a bolt beyond eight tiles rolls far too: lost in the roll still ringing
		s.strike( 9f, 9f, false, false );
		run( s, g, rec, 2f );
		assertEquals( 2, rec.heard.size() );
		//but a near one breaks in over the far roll
		s.strike( -4f, 4f, false, false );
		run( s, g, rec, 1.5f );
		assertEquals( 3, rec.heard.size() );
		assertSame( THUNDER_NEAR, rec.heard.get( 2 ).s );
		assertTrue( rec.heard.get( 2 ).pan < 0f );
		assertTrue( "over the far roll", rec.heard.get( 2 ).at < far.end() );

		//a storm's strikes every second or two: never two rolls at once but a crack over a far one
		rec.heard.clear();
		Random dice = new Random( 1 );
		for (int k = 0; k < 200; k++){
			float tiles = 1f + dice.nextFloat() * 20f;
			s.strike( (dice.nextFloat() * 2f - 1f) * tiles, tiles, dice.nextFloat() < 0.3f, false );
			run( s, g, rec, 0.5f + dice.nextFloat() * 2f );
		}
		ArrayList<Heard> rolls = rec.heard;
		assertTrue( rolls.size() > 20 );
		for (int i = 1; i < rolls.size(); i++){
			Heard last = rolls.get( i - 1 ), next = rolls.get( i );
			if (next.at < last.end()){
				assertSame( "only a crack breaks in", THUNDER_NEAR, next.s );
				assertSame( "and only over a far roll", THUNDER_FAR, last.s );
			}
		}
	}

	@Test
	public void theThunderComesWithItsFlash(){
		//a strike at each distance, alone (the last roll rung out): when its thunder starts, what it
		//is, how loud and from where
		Object[][] cases = {
				//tiles, sheet, seen, crack
				{ 0.5f, false, false, true }, { 4f, false, false, true }, { 8f, false, false, true },
				{ 12f, false, true, true }, { 14f, false, true, true },
				{ 9f, false, false, false }, { 14f, false, false, false }, { 24f, false, false, false },
				{ 10f, true, false, false }, { 24f, true, false, false },
		};
		Rec rec = new Rec();
		WeatherScape s = scape( 31, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.NONE, 0f, 0f, 0f );
		s.sky.storm = true;
		run( s, g, rec, 5f );
		float lastCrack = -1f, lastRoll = -1f;
		for (Object[] c : cases){
			float tiles = (Float) c[0];
			boolean sheet = (Boolean) c[1], seen = (Boolean) c[2], crack = (Boolean) c[3];
			String what = tiles + " tiles" + (sheet ? ", sheet" : "") + (seen ? ", seen" : "");
			assertEquals( what, crack, WeatherScape.cracks( tiles, sheet, seen ) );
			rec.heard.clear();
			float at = s.now;
			float dx = sheet ? 0f : -tiles;
			s.strike( dx, tiles, sheet, seen );
			run( s, g, rec, 8f );
			assertEquals( what, 1, rec.heard.size() );
			Heard h = rec.heard.get( 0 );
			assertSame( what, crack ? THUNDER_NEAR : THUNDER_FAR, h.s );
			float late = h.at - at;
			assertEquals( what, WeatherScape.delay( tiles, crack ), late, DT + 1e-4f );
			//a crack with its flash, a far roll within half a second of it
			assertTrue( what + ": " + late, late <= (crack ? 0.15f : 0.5f) + DT + 1e-4f );
			//and still the further the later, the duller, and from its side
			float due = WeatherScape.delay( tiles, crack );
			if (crack){
				assertTrue( what, due >= lastCrack );
				lastCrack = due;
			} else if (!sheet){
				assertTrue( what, due >= lastRoll );
				lastRoll = due;
			}
			assertEquals( what, WeatherScape.thunderLevel( tiles ), h.level, 1e-5f );
			assertEquals( what, AmbientPlayer.pan( dx ), h.pan, 1e-5f );
		}
		//a crack is never later than a roll
		assertTrue( WeatherScape.delay( 24f, true ) <= WeatherScape.delay( 0f, false ) );
		//the overlay's own crack, where the weather is not heard, keeps the same time
		assertEquals( WeatherScape.delay( 12f, true ), WeatherSounds.thunderDelay( 12f, false, true ), 0f );
		assertEquals( WeatherScape.delay( 12f, false ), WeatherSounds.thunderDelay( 12f, false, false ), 0f );
		assertEquals( WeatherScape.delay( 5f, false ), WeatherSounds.thunderDelay( 5f, true, false ), 0f );
	}

	@Test
	public void aBoltInSightRingsInTheHerosOwnPlaceAndIsNeverSilent(){
		//a storm cloud's bolt seen in a cave, indoors, on a floor the weather never reaches: the
		//crack at its full, with the flash
		for (Where w : new Where[]{ Where.CAVE, Where.INDOORS, Where.SILENT }){
			Rec rec = new Rec();
			WeatherScape s = scape( 32, rec );
			Soundscape.Ground g = ground( field(), 20 + 20 * W );
			sky( s, w, PrecipType.NONE, 0f, 0f, 0f );
			run( s, g, rec, 5f );
			float at = s.now;
			assertTrue( s.strike( 2f, 3f, false, true ) );
			run( s, g, rec, 2f );
			assertEquals( w.name(), 1, rec.heard.size() );
			assertSame( w.name(), THUNDER_NEAR, rec.heard.get( 0 ).s );
			assertEquals( w.name(), WeatherScape.thunderLevel( 3f ), rec.heard.get( 0 ).level, 1e-5f );
			assertTrue( w.name(), rec.heard.get( 0 ).at - at <= 0.15f + DT );
		}
		//a strike in the quiet a fresh start keeps: its thunder would come long after its flash. One
		//out of sight is lost with the quiet's other sounds; one in sight is handed back, for the
		//overlay's own crack with its flash
		Rec rec = new Rec();
		WeatherScape s = scape( 33, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.NONE, 0f, 0f, 0f );
		s.sky.storm = true;
		s.tick( DT, g, 0f, rec );
		assertTrue( s.strike( 0f, 3f, false, false ) );
		assertFalse( "in sight, in the quiet", s.strike( 0f, 3f, false, true ) );
		run( s, g, rec, 6f );
		assertTrue( "dropped, not heard late", rec.heard.isEmpty() );

		//a bolt in sight while a crack still rolls is handed back too, never lost in it (one out of
		//sight still is); over a far roll it breaks in
		assertTrue( s.strike( 1f, 2f, false, true ) );
		run( s, g, rec, 1f );
		assertEquals( 1, rec.heard.size() );
		assertFalse( "in sight, over a crack rolling", s.strike( 1f, 2f, false, true ) );
		assertTrue( s.strike( 1f, 2f, false, false ) );
		run( s, g, rec, 1f );
		assertEquals( 1, rec.heard.size() );
		run( s, g, rec, 8f );
		assertTrue( s.strike( 0f, 15f, true, false ) );
		run( s, g, rec, 1f );
		assertSame( THUNDER_FAR, rec.heard.get( 1 ).s );
		assertTrue( "in sight, over a far roll", s.strike( 2f, 3f, false, true ) );
		run( s, g, rec, 1f );
		assertEquals( 3, rec.heard.size() );
		assertSame( THUNDER_NEAR, rec.heard.get( 2 ).s );
	}

	@Test
	public void aFarRollNowAndThenInTheHeavyRain(){
		for (Where w : new Where[]{ Where.OPEN, Where.EXPOSED }){
			Rec rec = new Rec();
			WeatherScape s = scape( 12, rec );
			Soundscape.Ground g = ground( field(), 20 + 20 * W );
			sky( s, w, PrecipType.RAIN, 0.4f, 0f, 0f );
			run( s, g, rec, 1200f );
			ArrayList<Heard> rolls = rec.of( THUNDER_FAR );
			assertTrue( rolls.size() >= 12 );
			assertTrue( rolls.get( 0 ).at <= Soundscape.QUIET_MAX + WeatherScape.ROLL_MAX + DT );
			for (int i = 1; i < rolls.size(); i++){
				float gap = rolls.get( i ).at - rolls.get( i - 1 ).at;
				assertTrue( "every 25-70 s: " + gap, gap >= WeatherScape.ROLL_MIN - 1e-3f && gap <= WeatherScape.ROLL_MAX + 2f );
			}
			for (Heard h : rolls) assertTrue( Math.abs( h.pan ) <= WeatherScape.ROLL_PAN );
			assertTrue( rec.of( THUNDER_NEAR ).isEmpty() );
		}
		//none in a lighter rain, indoors, or in a storm (its strikes bring their own)
		for (int k = 0; k < 3; k++){
			Rec rec = new Rec();
			WeatherScape s = scape( 13, rec );
			sky( s, k == 1 ? Where.INDOORS : Where.OPEN, PrecipType.RAIN, k == 0 ? 0.3f : 0.5f, 0f, 0f );
			s.sky.storm = k == 2;
			run( s, ground( field(), 20 + 20 * W ), rec, 300f );
			assertFalse( rec.heard.isEmpty() );
			assertTrue( rec.of( THUNDER_FAR ).isEmpty() );
		}
	}

	// ------------------------------------------------------------ where it is heard

	@Test
	public void indoorsOnlyTheRainOnTheRoofAndTheThunderHalfAsLoud(){
		int[] map = field();
		int hero = 20 + 20 * W;
		map[hero - 2] = Terrain.WATER;
		map[hero + 2] = Terrain.TREE_PINE;
		Rec rec = new Rec();
		WeatherScape s = scape( 14, rec );
		Soundscape.Ground g = ground( map, hero );
		sky( s, Where.INDOORS, PrecipType.RAIN, 0.5f, 25f, 90f );
		s.sky.dripping = true;
		run( s, g, rec, 120f );
		assertEquals( EnumSet.of( RAIN_ROOF ), EnumSet.copyOf( rec.sounds() ) );
		for (Heard h : rec.heard) assertTrue( h.bed == LEFT || h.bed == RIGHT );
		assertEquals( WeatherScape.intensity( RAIN_ROOF, 0.5f ), rec.heard.get( 0 ).level, 1e-4f );

		rec.heard.clear();
		s.sky.type = PrecipType.SNOW;
		run( s, g, rec, 30f );
		//the last play of the rain rings out, then nothing
		assertTrue( rec.heard.size() <= 2 );
		rec.heard.clear();
		s.strike( 2f, 2f, false, false );
		run( s, g, rec, 2f );
		assertEquals( 1, rec.heard.size() );
		assertSame( THUNDER_NEAR, rec.heard.get( 0 ).s );
		assertEquals( WeatherScape.thunderLevel( 2f ) * WeatherScape.INDOORS_THUNDER, rec.heard.get( 0 ).level, 1e-5f );
	}

	@Test
	public void theCavesHearOnlyAStormFarOffAndTheDeepNothing(){
		int[] map = field();
		int hero = 20 + 20 * W;
		map[hero + 2] = Terrain.TREE_PINE;
		for (Where w : new Where[]{ Where.CAVE, Where.SILENT }){
			Rec rec = new Rec();
			WeatherScape s = scape( 15, rec );
			Soundscape.Ground g = ground( map, hero );
			sky( s, w, PrecipType.RAIN, 0.6f, 25f, 90f );
			s.sky.dripping = true;
			s.sky.storm = true;
			run( s, g, rec, 120f );
			assertTrue( rec.heard.isEmpty() );
			s.strike( 2f, 2f, false, false );
			run( s, g, rec, 2f );
			if (w == Where.SILENT){
				assertTrue( rec.heard.isEmpty() );
				continue;
			}
			//the rock passes the roll, never the crack
			assertEquals( 1, rec.heard.size() );
			assertSame( THUNDER_FAR, rec.heard.get( 0 ).s );
			assertEquals( WeatherScape.thunderLevel( 2f ) * WeatherScape.CAVE_THUNDER, rec.heard.get( 0 ).level, 1e-5f );
		}
	}

	@Test
	public void dripsOffTheTreesInTheFogOnTheSurface(){
		int[] map = field();
		int hero = 20 + 20 * W;
		int[] trees = { hero + 3, hero - 4 + 2 * W, hero - 5 * W };
		for (int t : trees) map[t] = Terrain.TREE_OAK;
		map[hero + 9] = Terrain.TREE_OAK;
		Rec rec = new Rec();
		WeatherScape s = scape( 16, rec );
		Soundscape.Ground g = ground( map, hero );
		sky( s, Where.OPEN, PrecipType.NONE, 0f, 1f, 0f );
		s.sky.dripping = true;
		run( s, g, rec, 300f );
		ArrayList<Heard> drips = rec.of( DRIP_LEAF );
		assertTrue( drips.size() > 40 );
		assertEquals( drips.size(), rec.heard.size() );
		HashSet<Float> pans = new HashSet<>();
		for (int i = 0; i < drips.size(); i++){
			Heard h = drips.get( i );
			boolean fromATree = false;
			for (int t : trees){
				if (Math.abs( h.pan - AmbientPlayer.pan( t % W - hero % W ) ) < 1e-5f
						&& Math.abs( h.level - AmbientPlayer.falloff( g.away( t ) ) ) < 1e-5f) fromATree = true;
			}
			assertTrue( "off a tree in reach", fromATree );
			pans.add( h.pan );
			if (i > 0){
				float gap = h.at - drips.get( i - 1 ).at;
				assertTrue( "every 2-6 s: " + gap, gap >= WeatherScape.DRIP_MIN - 1e-3f && gap <= WeatherScape.DRIP_MAX + DT + 1e-3f );
			}
		}
		assertEquals( "off every one of them", trees.length, pans.size() );

		//not on the peaks, not off the open sky, not once the fog lifts, and none with no tree
		for (int k = 0; k < 4; k++){
			rec = new Rec();
			s = scape( 17, rec );
			sky( s, k == 1 ? Where.EXPOSED : Where.OPEN, PrecipType.NONE, 0f, 1f, 0f );
			s.sky.peaks = k == 0;
			s.sky.dripping = k != 2;
			run( s, ground( k == 3 ? field() : map, hero ), rec, 120f );
			assertTrue( rec.heard.isEmpty() );
		}
	}

	// ------------------------------------------------------------ the budget and the hush

	@Test
	public void neverMoreThanSixRingingTheBedsAlwaysTheirs(){
		int[] map = field();
		int hero = 20 + 20 * W;
		map[hero - 2] = Terrain.WATER;
		for (int t : new int[]{ hero + 3, hero - 3 * W, hero + 4 * W }) map[t] = Terrain.TREE_OAK;
		Rec rec = new Rec();
		WeatherScape s = scape( 18, rec );
		Soundscape.Ground g = ground( map, hero );
		//everything at once: a storm, heavy rain on the water, a gale, the trees dripping
		sky( s, Where.OPEN, PrecipType.RAIN, 0.6f, 22f, 90f );
		s.sky.storm = true;
		s.sky.dripping = true;
		Random dice = new Random( 2 );
		float nextStrike = 3f;
		int oneShotsMost = 0;
		for (float t = 0f; t < 900f; t += DT){
			if (s.now >= nextStrike){
				float tiles = 1f + dice.nextFloat() * 20f;
				s.strike( (dice.nextFloat() * 2f - 1f) * tiles, tiles, dice.nextFloat() < 0.3f, false );
				nextStrike = s.now + 0.25f + dice.nextFloat() * 4f;
			}
			//midway the storm passes: the rain eases, the fog comes, the gusts are free
			if (t > 450f){
				s.sky.storm = false;
				s.sky.rate = 0.3f;
				nextStrike = Float.MAX_VALUE;
			}
			s.tick( DT, g, gust( s.now ), rec );
			assertTrue( "ringing: " + s.ringing(), s.ringing() <= WeatherScape.MAX_PLAYS );
			//counted from what was played: each bed emitter's newest play, the thunder as one
			//while any of it rolls, and every gust and drip
			int beds = 0, oneShots = 0;
			boolean thunder = false;
			for (int bed : new int[]{ LEFT, RIGHT, NEAR, WIND }){
				ArrayList<Heard> plays = rec.of( bed );
				if (!plays.isEmpty() && plays.get( plays.size() - 1 ).end() > s.now) beds++;
			}
			for (Heard h : rec.heard){
				if (h.bed != ONE_SHOT || h.end() <= s.now) continue;
				if (h.s == THUNDER_FAR || h.s == THUNDER_NEAR) thunder = true;
				else oneShots++;
			}
			if (thunder) oneShots++;
			assertTrue( beds + oneShots <= WeatherScape.MAX_PLAYS );
			oneShotsMost = Math.max( oneShotsMost, oneShots );
		}
		assertEquals( 2, oneShotsMost );
		//the beds were laid all along, and the one-shots had their turns
		for (int bed : new int[]{ LEFT, RIGHT, NEAR, WIND }){
			ArrayList<Heard> plays = rec.of( bed );
			for (int i = 1; i < plays.size(); i++) assertTrue( plays.get( i ).at <= plays.get( i - 1 ).latestNext() + DT + 1e-4f );
		}
		for (AmbientSound one : new AmbientSound[]{ GUST, DRIP_LEAF, THUNDER_FAR, THUNDER_NEAR }){
			assertFalse( one + " was heard", rec.of( one ).isEmpty() );
		}
	}

	@Test
	public void inAStormACrackIsNeverLostToAGustNorAGustToAFarRoll(){
		int[] map = field();
		int hero = 20 + 20 * W;
		map[hero - 2] = Terrain.WATER;
		Rec rec = new Rec();
		WeatherScape s = scape( 22, rec );
		Soundscape.Ground g = ground( map, hero );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.6f, 14f, 90f );
		s.sky.storm = true;
		//the four beds laid, the rain upright
		for (float t = 0f; t < 10f; t += DT) s.tick( DT, g, -1f, rec );
		assertEquals( 4, s.bedsHolding() );
		//the rain leans over: a gust
		s.tick( DT, g, 0.9f, rec );
		assertEquals( 1, rec.of( GUST ).size() );
		//sheet lightning's far roll, then a bolt two tiles off while the gust and the roll ring
		s.strike( 0f, 15f, true, false );
		for (float t = 0f; t < 3.5f; t += DT) s.tick( DT, g, 0.9f, rec );
		assertEquals( 1, rec.of( THUNDER_FAR ).size() );
		s.strike( -2f, 2f, false, false );
		for (float t = 0f; t < 1f; t += DT) s.tick( DT, g, 0.9f, rec );
		assertEquals( "the crack breaks in", 1, rec.of( THUNDER_NEAR ).size() );
		assertTrue( s.ringing() <= WeatherScape.MAX_PLAYS );

		//all rung out; a far roll, and the rain leans over while it rolls: the gust is heard
		for (float t = 0f; t < 8f; t += DT) s.tick( DT, g, -1f, rec );
		s.strike( 0f, 15f, true, false );
		for (float t = 0f; t < 3.5f; t += DT) s.tick( DT, g, -1f, rec );
		assertEquals( 2, rec.of( THUNDER_FAR ).size() );
		s.tick( DT, g, 0.9f, rec );
		assertEquals( "a gust over the roll", 2, rec.of( GUST ).size() );
	}

	@Test
	public void aStormsGustsStillComeAndItsCracksAreHeard(){
		int[] map = field();
		int hero = 20 + 20 * W;
		map[hero - 2] = Terrain.WATER;
		int[] gusts = new int[2];
		for (int storm = 0; storm < 2; storm++){
			Rec rec = new Rec();
			WeatherScape s = scape( 23, rec );
			Soundscape.Ground g = ground( map, hero );
			sky( s, Where.OPEN, PrecipType.RAIN, 0.6f, 14f, 90f );
			s.sky.storm = storm == 1;
			Random dice = new Random( 3 );
			//the strikes as the overlay throws them: when each one's thunder is due, and if a crack
			ArrayList<float[]> due = new ArrayList<>();
			float nextStrike = 3f;
			for (float t = 0f; t < 600f; t += DT){
				if (storm == 1 && s.now >= nextStrike){
					float tiles = 1f + dice.nextFloat() * 20f;
					boolean sheet = dice.nextFloat() < 0.3f;
					s.strike( (dice.nextFloat() * 2f - 1f) * tiles, tiles, sheet, false );
					boolean crack = WeatherScape.cracks( tiles, sheet, false );
					due.add( new float[]{ s.now + WeatherScape.delay( tiles, crack ), crack ? 1 : 0 } );
					nextStrike = s.now + 1.7f + dice.nextFloat() * 0.6f;
				}
				s.tick( DT, g, gust( s.now ), rec );
				assertTrue( s.ringing() <= WeatherScape.MAX_PLAYS );
			}
			gusts[storm] = rec.of( GUST ).size();
			//every crack is heard but one that comes over a crack still ringing
			ArrayList<Heard> cracks = rec.of( THUNDER_NEAR );
			int heard = 0;
			for (float[] d : due){
				if (d[1] == 0 || d[0] > s.now - 1f) continue;
				boolean over = false, unsure = false, played = false;
				for (Heard c : cracks){
					if (c.at >= d[0] - 1e-4f && c.at <= d[0] + DT + 1e-3f) played = true;
					else if (c.at < d[0] && c.end() > d[0] + DT) over = true;
					else if (c.at < d[0] && c.end() > d[0] - 1e-4f) unsure = true;
				}
				if (over || unsure) continue;
				assertTrue( "the crack due at " + d[0], played );
				heard++;
			}
			if (storm == 1) assertTrue( heard > 20 );
		}
		assertTrue( "gusts in the storm: " + gusts[1] + " of " + gusts[0], gusts[1] * 2 >= gusts[0] );
	}

	@Test
	public void aHushStopsTheBedsDropsWhatWasToComeAndStartsOverAfterAQuiet(){
		Rec rec = new Rec();
		WeatherScape s = scape( 19, rec );
		Soundscape.Ground g = ground( field(), 20 + 20 * W );
		sky( s, Where.OPEN, PrecipType.RAIN, 0.3f, 0f, 0f );
		s.sky.storm = true;
		run( s, g, rec, 30f );
		assertFalse( rec.heard.isEmpty() );
		//a strike, then silence before its thunder comes
		s.strike( 1f, 10f, false, false );
		rec.heard.clear();
		for (float t = 0f; t < 5f; t += DT) s.hush( DT );
		assertTrue( rec.heard.isEmpty() );
		for (WeatherScape.Emitter e : s.beds) assertFalse( e.on );
		float back = s.now;
		run( s, g, rec, 10f );
		float first = rec.heard.get( 0 ).at - back;
		assertTrue( "after a quiet: " + first, first >= Soundscape.QUIET_MIN && first <= Soundscape.QUIET_MAX + DT );
		assertTrue( "the strike's thunder was dropped", rec.of( THUNDER_FAR ).isEmpty() && rec.of( THUNDER_NEAR ).isEmpty() );
	}
}
