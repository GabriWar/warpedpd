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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ElderYeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GiantScorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Roaming;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.RoamerTrophy;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The roaming great beasts (RoamerEvent) on paper: a pure function of the seed, the region and
 * the period, one period in two, eight days within its period; the beast's kind its lair's
 * ground's, its haunts a day's walk apart on its kind's ground inside its region; where it is at
 * any turn following its days (resting, then on its way); its tracks keeping their cells as the
 * turns go by; the map pinning the sector it haunts;
 * met about as often as meant; its names, its beasts and its trophy; and what a save keeps of it
 * (its mark, a trophy, the wounds of one that got away).
 */
public class RoamerEventTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int FC = WorldEvents.FC;
	private static final List<WorldEvents.Event> NONE = Collections.emptyList();

	private int startDay, challenges;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
	}

	@After
	public void tearDown(){
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
	}

	//every beast of the regions within four of the origin's over twenty periods
	private static ArrayList<WorldEvents.Event> beasts(){
		ArrayList<WorldEvents.Event> out = new ArrayList<>();
		for (int ry = -4; ry <= 4; ry++){
			for (int rx = -4; rx <= 4; rx++){
				for (int p = 0; p < 20; p++){
					WorldEvents.Event e = RoamerEvent.roamer( SEED, rx, ry, p );
					if (e != null) out.add( e );
				}
			}
		}
		return out;
	}

	@Test
	public void beastsKeepTheirPeriodsAndTheirGround(){
		ArrayList<WorldEvents.Event> beasts = beasts();
		assertTrue( "only " + beasts.size() + " beasts", beasts.size() >= 40 );
		HashSet<RoamerEvent.Kind> kinds = new HashSet<>();
		for (WorldEvents.Event e : beasts){
			int period = Math.floorDiv( e.startDay, RoamerEvent.PERIOD );
			assertTrue( "inside its period", e.startDay + RoamerEvent.DAYS <= (period + 1) * RoamerEvent.PERIOD );
			assertEquals( RoamerEvent.DAYS * FC, e.endTurn - e.startTurn );
			assertEquals( e.startDay * FC, e.startTurn );
			assertEquals( WorldEvents.idOf( WorldEvents.Type.ROAMER, e.sx, e.sy, e.startDay ), e.id );
			RoamerEvent.Haunts h = RoamerEvent.haunts( SEED, e );
			kinds.add( h.kind );
			//its kind is its lair's ground's
			WorldModel.Biome lair = WorldModel.baseBiomeAt( SEED, e.wx, e.wy );
			assertTrue( h.kind.keepsTo( lair ) );
			assertTrue( Math.max( Math.abs( e.wx ), Math.abs( e.wy ) ) >= RoamerEvent.TOWN_CLEAR );
			assertFalse( OverworldFauna.nearSettlement( SEED, e.wx, e.wy ) );
			int half = RoamerEvent.REGION / 2;
			for (int i = 0; i < RoamerEvent.DAYS; i++){
				//inside its region, on its kind's ground, a day's walk from the day before's
				String at = "haunt " + i + " " + h.x[i] + "," + h.y[i] + " of the beast anchored " + e.sx + "," + e.sy
						+ " from its lair " + e.wx + "," + e.wy;
				assertTrue( at, Math.floorDiv( h.x[i], WorldStructures.SECTOR ) >= e.sx - half );
				assertTrue( at, Math.floorDiv( h.x[i], WorldStructures.SECTOR ) <= e.sx + half );
				assertTrue( at, Math.floorDiv( h.y[i], WorldStructures.SECTOR ) >= e.sy - half );
				assertTrue( at, Math.floorDiv( h.y[i], WorldStructures.SECTOR ) <= e.sy + half );
				assertTrue( h.kind.keepsTo( WorldModel.baseBiomeAt( SEED, h.x[i], h.y[i] ) ) );
				if (i > 0) assertTrue( Math.hypot( h.x[i] - h.x[i - 1], h.y[i] - h.y[i - 1] ) <= RoamerEvent.STEP + 2 );
			}
			//pure: asked again, the same
			WorldEvents.Event again = RoamerEvent.roamer( SEED, Math.floorDiv( e.sx, RoamerEvent.REGION ),
					Math.floorDiv( e.sy, RoamerEvent.REGION ), Math.floorDiv( e.startDay, RoamerEvent.PERIOD ) );
			assertEquals( e.id, again.id );
			assertEquals( e.wx, again.wx );
		}
		System.out.println( "[roamers] " + beasts.size() + " beasts in 81 regions over 20 periods: " + kinds );
		assertTrue( kinds.contains( RoamerEvent.Kind.WOLF ) );
	}

	@Test
	public void itRestsThenRoamsFromHauntToHaunt(){
		WorldEvents.Event e = beasts().get( 0 );
		RoamerEvent.Haunts h = RoamerEvent.haunts( SEED, e );
		//its first day at its lair
		for (int t = e.startTurn; t < e.startTurn + FC; t += 250){
			int[] p = RoamerEvent.where( h, e, t );
			assertEquals( h.x[0], p[0] );
			assertEquals( h.y[0], p[1] );
		}
		for (int i = 1; i < RoamerEvent.DAYS; i++){
			int day = e.startTurn + i * FC;
			int[] rest = RoamerEvent.where( h, e, day + Math.round( RoamerEvent.LEAVE * FC ) - 1 );
			int[] there = RoamerEvent.where( h, e, day + Math.round( RoamerEvent.ARRIVE * FC ) + 1 );
			assertEquals( h.x[i - 1], rest[0] );
			assertEquals( h.y[i - 1], rest[1] );
			assertEquals( h.x[i], there[0] );
			assertEquals( h.y[i], there[1] );
			//on its way, never a jump of more than a cell from one turn to the next
			int[] prev = rest;
			for (int t = day + Math.round( RoamerEvent.LEAVE * FC ); t <= day + Math.round( RoamerEvent.ARRIVE * FC ); t++){
				int[] p = RoamerEvent.where( h, e, t );
				assertTrue( Math.max( Math.abs( p[0] - prev[0] ), Math.abs( p[1] - prev[1] ) ) <= 1 );
				prev = p;
			}
		}
		//the map pins the middle of the sector it haunts now
		WorldEvents.Event m = WorldEvents.onMap( SEED, e, e.startTurn + 3 * FC );
		int[] p = RoamerEvent.where( h, e, e.startTurn + 3 * FC );
		assertEquals( Math.floorDiv( p[0], WorldStructures.SECTOR ) * WorldStructures.SECTOR + WorldStructures.SECTOR / 2, m.wx );
		assertEquals( Math.floorDiv( p[1], WorldStructures.SECTOR ) * WorldStructures.SECTOR + WorldStructures.SECTOR / 2, m.wy );
		assertEquals( e.id, m.id );
	}

	@Test
	public void itsTracksStayWhereTheyWerePressed(){
		//a beast with a good day's walk from its lair
		WorldEvents.Event e = null;
		RoamerEvent.Haunts h = null;
		for (WorldEvents.Event b : beasts()){
			RoamerEvent.Haunts bh = RoamerEvent.haunts( SEED, b );
			if (Math.max( Math.abs( bh.x[1] - bh.x[0] ), Math.abs( bh.y[1] - bh.y[0] ) ) >= 40){
				e = b;
				h = bh;
				break;
			}
		}
		assertNotNull( e );
		//a window of open ground over that walk
		OverworldLevel ow = new OverworldLevel();
		ow.setSize( OverworldLevel.WIDTH, OverworldLevel.HEIGHT );
		java.util.Arrays.fill( ow.map, Terrain.GRASS );
		java.util.Arrays.fill( ow.passable, true );
		ow.worldX = (h.x[0] + h.x[1]) / 2 - OverworldLevel.WIDTH / 2;
		ow.worldY = (h.y[0] + h.y[1]) / 2 - OverworldLevel.HEIGHT / 2;
		//from one turn to the next a print keeps its cell and its way: at most the oldest goes
		//and at most a new one comes at the far end
		ArrayList<Integer> before = null;
		int most = 0;
		for (int turn = e.startTurn + FC; turn < e.startTurn + 3 * FC; turn++){
			ArrayList<Integer> now = new ArrayList<>();
			for (int[] p : RoamerEvent.trail( ow, e, h, turn )) now.add( p[0] * 16 + p[1] );
			if (before != null){
				int gone = !before.isEmpty() && (now.isEmpty() || !before.get( 0 ).equals( now.get( 0 ) )) ? 1 : 0;
				int kept = before.size() - gone;
				assertTrue( "turn " + turn, kept <= now.size() && now.size() - kept <= 1 );
				assertEquals( "turn " + turn, before.subList( gone, before.size() ), now.subList( 0, kept ) );
			}
			most = Math.max( most, now.size() );
			before = now;
		}
		assertTrue( "only " + most + " prints", most >= 10 );
	}

	@Test
	public void aHeroHearsOfOneNowAndThen(){
		//the share of days a beast is abroad within RUMOUR sectors of a spot on land
		java.util.Random rng = new java.util.Random( 3 );
		int samples = 0, near = 0;
		for (int k = 0; k < 120; k++){
			int sx = rng.nextInt( 31 ) - 15, sy = rng.nextInt( 31 ) - 15;
			WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, sx * WorldStructures.SECTOR + 48, sy * WorldStructures.SECTOR + 48 );
			if (b == WorldModel.Biome.OCEAN || b == WorldModel.Biome.MOUNTAIN) continue;
			for (int d = 0; d < 240; d += 4){
				samples++;
				int turn = d * FC + 1200;
				for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, sx, sy, RoamerEvent.RUMOUR + RoamerEvent.REGION / 2 + 1, d, NONE )){
					if (e.type != WorldEvents.Type.ROAMER || !e.activeAt( turn )) continue;
					int[] p = RoamerEvent.where( RoamerEvent.haunts( SEED, e ), e, turn );
					if (Math.max( Math.abs( Math.floorDiv( p[0], WorldStructures.SECTOR ) - sx ),
							Math.abs( Math.floorDiv( p[1], WorldStructures.SECTOR ) - sy ) ) <= RoamerEvent.RUMOUR){
						near++;
						break;
					}
				}
			}
		}
		double share = near / (double) Math.max( 1, samples );
		System.out.println( "[roamers] a beast abroad within " + RoamerEvent.RUMOUR + " sectors on " + Math.round( share * 100 ) + "% of days" );
		assertTrue( "too seldom: " + share, share >= 0.05 );
		assertTrue( "too often: " + share, share <= 0.6 );
	}

	@Test
	public void namesBeastsAndTrophies(){
		for (RoamerEvent.Kind k : RoamerEvent.Kind.values()){
			HashSet<String> names = new HashSet<>();
			for (int n = 0; n < RoamerEvent.NAMES; n++){
				String t = RoamerEvent.title( k, n );
				assertFalse( k + " " + n, t.contains( Messages.NO_TEXT_FOUND ) );
				names.add( t );
			}
			assertEquals( RoamerEvent.NAMES, names.size() );
			for (String key : new String[]{ "rumour_" + k.key() + "_0", "rumour_" + k.key() + "_1", "tracks_" + k.key(), "seen_" + k.key() }){
				assertNotEquals( key, Messages.NO_TEXT_FOUND, Messages.get( RoamerEvent.class, key ) );
			}
			Mob m = k.make();
			assertSame( k, RoamerEvent.kindOf( m ) );
			//four times its kin's health
			assertTrue( m.HT >= 400 );
			Roaming r = Buff.affect( m, Roaming.class );
			r.event = 99L;
			r.name = 3;
			assertEquals( RoamerEvent.title( k, 3 ), m.name() );
			RoamerTrophy t = RoamerTrophy.of( k, 3, 99L );
			assertEquals( k.trophy, t.image() );
			assertTrue( t.name().contains( RoamerEvent.title( k, 3 ) ) );
			assertNotNull( ItemSpriteSheet.film.get( k.trophy ) );
			Bundle b = new Bundle();
			b.put( "trophy", t );
			RoamerTrophy back = (RoamerTrophy) b.get( "trophy" );
			assertEquals( 99L, back.event );
			assertEquals( k.ordinal(), back.kind );
			assertEquals( t.name(), back.name() );
		}
		assertTrue( new GreatWolf().HT > new xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf().HT * 3 );
		assertTrue( new ElderYeti().HT > new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti().HT * 3 );
		assertTrue( new GiantScorpio().HT > new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio().HT * 3 );
		for (String key : new String[]{ "land_woods", "land_fields", "land_hills", "land_snow", "land_desert", "close_by", "slain" }){
			assertNotEquals( key, Messages.NO_TEXT_FOUND, Messages.get( RoamerEvent.class, key ) );
		}
		assertTrue( Messages.get( RoamerEvent.class, "toward", "north" ).contains( "north" ) );
		//a beast's kind by its lair's ground
		assertSame( RoamerEvent.Kind.SCORPION, RoamerEvent.Kind.of( WorldModel.Biome.DESERT, 0 ) );
		assertSame( RoamerEvent.Kind.YETI, RoamerEvent.Kind.of( WorldModel.Biome.SNOWFIELD, 1 ) );
		assertSame( RoamerEvent.Kind.WOLF, RoamerEvent.Kind.of( WorldModel.Biome.FOREST, 0 ) );
		assertEquals( null, RoamerEvent.Kind.of( WorldModel.Biome.OCEAN, 0 ) );
	}

	@Test
	public void aBeastsMarkAndItsWoundsComeBackFromASave(){
		Roaming r = new Roaming();
		r.event = 7777L;
		r.name = 5;
		r.endTurn = 123456;
		Bundle b = new Bundle();
		b.put( "mark", r );
		Roaming back = (Roaming) b.get( "mark" );
		assertEquals( 7777L, back.event );
		assertEquals( 5, back.name );
		assertEquals( 123456, back.endTurn );

		WorldEventLog log = new WorldEventLog();
		log.wounds.put( 7777L, 210 );
		log.woundEnds.put( 7777L, 123456 );
		log.wounds.put( 8888L, 90 );
		log.woundEnds.put( 8888L, 500 );
		Bundle lb = new Bundle();
		log.storeInBundle( lb );
		WorldEventLog read = new WorldEventLog();
		read.restoreFromBundle( lb );
		assertEquals( Integer.valueOf( 210 ), read.wounds.get( 7777L ) );
		assertEquals( Integer.valueOf( 500 ), read.woundEnds.get( 8888L ) );
		//forgotten once its days are over
		read.prune( 600 );
		assertFalse( read.wounds.containsKey( 8888L ) );
		assertTrue( read.wounds.containsKey( 7777L ) );
		//a save from before the beasts reads none
		WorldEventLog old = new WorldEventLog();
		old.restoreFromBundle( new Bundle() );
		assertTrue( old.wounds.isEmpty() );
	}

	@Test
	public void aForcedBeastHasAnIdOfItsOwn(){
		WorldEvents.Event natural = beasts().get( 0 );
		int[] lair = RoamerEvent.debugLair( SEED, natural.wx, natural.wy, 6 );
		assertNotNull( lair );
		WorldEvents.Event e = RoamerEvent.forcedRoamer( lair[0], lair[1], 40 * FC + 300 );
		assertNotEquals( WorldEvents.idOf( WorldEvents.Type.ROAMER, e.sx, e.sy, e.startDay ), e.id );
		assertEquals( RoamerEvent.DAYS * FC, e.endTurn - e.startTurn );
		RoamerEvent.Haunts h = RoamerEvent.haunts( SEED, e );
		assertEquals( lair[0], h.x[0] );
		assertEquals( lair[1], h.y[0] );
		ArrayList<WorldEvents.Event> forced = new ArrayList<>();
		forced.add( e );
		boolean listed = false;
		for (WorldEvents.Event o : WorldEvents.eventsNear( SEED, e.sx, e.sy, 1, 41, forced )) listed |= o.id == e.id;
		assertTrue( listed );
	}
}
