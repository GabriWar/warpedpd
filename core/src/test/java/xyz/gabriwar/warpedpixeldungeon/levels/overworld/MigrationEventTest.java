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
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Migrating;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The deer herds' migrations (MigrationEvent) on paper: only in spring and autumn, north in spring
 * and south in autumn, four days on a road of the deer's ground a pure function of the seed, the
 * region and the season, met often enough in those seasons; every beast's place a pure function
 * of the herd and the turn, the deer in a column and the wolves behind it, coming onto the road
 * and leaving it in turn; the map pinning the herd's middle as it moves; and what a herd's beast
 * keeps through a save.
 */
public class MigrationEventTest {

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

	//every herd of the regions within six of the origin's over two years, each once
	private static ArrayList<WorldEvents.Event> herds(){
		ArrayList<WorldEvents.Event> out = new ArrayList<>();
		HashSet<Long> seen = new HashSet<>();
		for (int ry = -6; ry <= 6; ry++){
			for (int rx = -6; rx <= 6; rx++){
				for (int d = 0; d < 2 * 364; d++){
					WorldEvents.Event e = MigrationEvent.migration( SEED, rx, ry, d );
					if (e != null && seen.add( e.id )) out.add( e );
				}
			}
		}
		return out;
	}

	@Test
	public void herdsTakeTheirSeasonsRoadOverFourDays(){
		ArrayList<WorldEvents.Event> herds = herds();
		assertTrue( "only " + herds.size() + " herds", herds.size() >= 20 );
		for (WorldEvents.Event e : herds){
			EventSeasons.Span s = EventSeasons.of( e.startDay );
			assertTrue( s.season == GameCalendar.Season.SPRING || s.season == GameCalendar.Season.AUTUMN );
			//its days all inside its season
			assertTrue( e.startDay >= s.first + MigrationEvent.EDGE );
			assertTrue( e.startDay + MigrationEvent.DAYS < s.end() );
			assertEquals( MigrationEvent.DAYS * FC, e.endTurn - e.startTurn );
			assertEquals( e.startDay * FC + MigrationEvent.SET_OFF, e.startTurn );
			assertEquals( WorldEvents.idOf( WorldEvents.Type.MIGRATION, e.sx, e.sy, e.startDay ), e.id );
			MigrationEvent.Road r = MigrationEvent.roadOf( SEED, e );
			assertNotNull( r );
			assertEquals( r.x0, e.wx );
			assertEquals( r.y0, e.wy );
			assertEquals( MigrationEvent.LENGTH, Math.hypot( r.x1 - r.x0, r.y1 - r.y0 ), 2 );
			//north in spring, south in autumn, never more than SWING off
			double ang = Math.atan2( r.y1 - r.y0, r.x1 - r.x0 );
			double due = s.season == GameCalendar.Season.SPRING ? -Math.PI / 2 : Math.PI / 2;
			assertTrue( Math.abs( ang - due ) <= MigrationEvent.SWING + 0.02 );
			assertTrue( MigrationEvent.deerRoad( SEED, r.x0, r.y0, r.x1, r.y1 ) );
			//through its own region
			int mx = (r.x0 + r.x1) / 2, my = (r.y0 + r.y1) / 2;
			assertEquals( Math.floorDiv( e.sx, MigrationEvent.REGION ), Math.floorDiv( Math.floorDiv( mx, WorldStructures.SECTOR ), MigrationEvent.REGION ), 1 );
			assertEquals( Math.floorDiv( e.sy, MigrationEvent.REGION ), Math.floorDiv( Math.floorDiv( my, WorldStructures.SECTOR ), MigrationEvent.REGION ), 1 );
			//listed among the world's events on each of its days
			for (int d = e.startDay; d <= e.startDay + MigrationEvent.DAYS; d++){
				boolean listed = false;
				for (WorldEvents.Event o : WorldEvents.eventsNear( SEED, e.sx, e.sy, 1, d, NONE )) listed |= o.id == e.id;
				assertTrue( "day " + d, listed );
			}
		}
		//and at most one a region a season
		HashSet<String> seasons = new HashSet<>();
		for (WorldEvents.Event e : herds) assertTrue( seasons.add( e.sx + "," + e.sy + "@" + EventSeasons.of( e.startDay ).first ) );
	}

	@Test
	public void noHerdInSummerOrWinter(){
		for (int d = 0; d < 364; d++){
			GameCalendar.Season s = EventSeasons.of( d ).season;
			if (s == GameCalendar.Season.SPRING || s == GameCalendar.Season.AUTUMN) continue;
			for (int ry = -4; ry <= 4; ry++){
				for (int rx = -4; rx <= 4; rx++) assertNull( MigrationEvent.migration( SEED, rx, ry, d ) );
			}
		}
	}

	@Test
	public void theHerdIsPureAndShapedAsAColumn(){
		WorldEvents.Event e = herds().get( 0 );
		int deer = MigrationEvent.herdSize( e.id ), wolves = MigrationEvent.packSize( e.id );
		assertTrue( deer >= MigrationEvent.MIN_HERD && deer <= MigrationEvent.MAX_HERD );
		assertTrue( wolves >= MigrationEvent.MIN_PACK && wolves <= MigrationEvent.MAX_PACK );
		int last = 0;
		for (int k = 0; k < deer; k++){
			int[] p = MigrationEvent.place( e.id, k );
			assertTrue( p[0] >= 0 && p[0] <= MigrationEvent.COLUMN );
			assertTrue( Math.abs( p[1] ) <= MigrationEvent.SPREAD + 1 );
			assertTrue( "down the column", p[0] >= last - 2 );
			last = Math.max( last, p[0] );
			assertEquals( p[0], MigrationEvent.place( e.id, k )[0] );
		}
		for (int j = 0; j < wolves; j++){
			int[] p = MigrationEvent.place( e.id, MigrationEvent.WOLVES + j );
			assertTrue( "the wolves keep behind the last deer", p[0] >= last + MigrationEvent.TRAIL - 2 );
		}
		//a place comes onto the road, moves along it and leaves it: nowhere before, nowhere after
		MigrationEvent.Road r = MigrationEvent.roadOf( SEED, e );
		int[] p = MigrationEvent.place( e.id, deer - 1 );
		assertNull( MigrationEvent.placeAt( r.x0, r.y0, r.x1, r.y1, e.startTurn, e.endTurn, p[0], p[1], e.startTurn ) );
		assertNull( MigrationEvent.placeAt( r.x0, r.y0, r.x1, r.y1, e.startTurn, e.endTurn, p[0], p[1], e.endTurn ) );
		double prev = -1;
		int on = 0;
		for (int t = e.startTurn; t < e.endTurn; t += 100){
			int[] at = MigrationEvent.placeAt( r.x0, r.y0, r.x1, r.y1, e.startTurn, e.endTurn, p[0], p[1], t );
			if (at == null) continue;
			on++;
			double along = (at[0] - r.x0) * (r.x1 - r.x0) + (at[1] - r.y0) * (r.y1 - r.y0);
			assertTrue( "it only goes forward", along >= prev - MigrationEvent.LENGTH );
			prev = along;
		}
		assertTrue( on > 50 );
		//the whole herd has left the road by its last turn
		for (int k = 0; k < deer; k++){
			int[] q = MigrationEvent.place( e.id, k );
			assertNull( MigrationEvent.placeAt( r.x0, r.y0, r.x1, r.y1, e.startTurn, e.endTurn, q[0], q[1], e.endTurn - 1 ) );
		}
	}

	@Test
	public void theMapFollowsTheHerdsMiddle(){
		WorldEvents.Event e = herds().get( 1 );
		WorldEvents.Event a = WorldEvents.onMap( SEED, e, e.startTurn + FC );
		WorldEvents.Event b = WorldEvents.onMap( SEED, e, e.startTurn + 3 * FC );
		assertEquals( e.id, a.id );
		assertEquals( e.endTurn, a.endTurn );
		assertTrue( "it moved on", a.wx != b.wx || a.wy != b.wy );
		MigrationEvent.Road r = MigrationEvent.roadOf( SEED, e );
		//its middle is on its road
		for (WorldEvents.Event m : new WorldEvents.Event[]{ a, b }){
			double len = Math.hypot( r.x1 - r.x0, r.y1 - r.y0 );
			double cross = Math.abs( (m.wx - r.x0) * (r.y1 - r.y0) - (m.wy - r.y0) * (r.x1 - r.x0) ) / len;
			assertTrue( cross <= 1.5 );
		}
	}

	@Test
	public void aHeroMeetsHerdsInTheirSeasons(){
		//the share of spring and autumn days a herd's middle is within HEAR sectors of a spot on land
		java.util.Random rng = new java.util.Random( 5 );
		int samples = 0, near = 0;
		for (int k = 0; k < 160; k++){
			int sx = rng.nextInt( 31 ) - 15, sy = rng.nextInt( 31 ) - 15;
			WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, sx * WorldStructures.SECTOR + 48, sy * WorldStructures.SECTOR + 48 );
			if (!MigrationEvent.deerGround( b )) continue;
			for (int d = 0; d < 364; d += 3){
				GameCalendar.Season s = EventSeasons.of( d ).season;
				if (s != GameCalendar.Season.SPRING && s != GameCalendar.Season.AUTUMN) continue;
				samples++;
				int turn = d * FC + 1500;
				for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, sx, sy, MigrationEvent.HEAR + 2, d, NONE )){
					if (e.type != WorldEvents.Type.MIGRATION || !e.activeAt( turn )) continue;
					WorldEvents.Event m = WorldEvents.onMap( SEED, e, turn );
					if (Math.max( Math.abs( Math.floorDiv( m.wx, WorldStructures.SECTOR ) - sx ),
							Math.abs( Math.floorDiv( m.wy, WorldStructures.SECTOR ) - sy ) ) <= MigrationEvent.HEAR){
						near++;
						break;
					}
				}
			}
		}
		double share = near / (double) Math.max( 1, samples );
		System.out.println( "[migrations] a herd within " + MigrationEvent.HEAR + " sectors on " + Math.round( share * 100 )
				+ "% of spring and autumn days on the deer's ground" );
		assertTrue( "too seldom: " + share, share >= 0.03 );
		assertTrue( "too often: " + share, share <= 0.5 );
	}

	@Test
	public void aForcedHerdGoesTheSeasonsWay(){
		int spring = -1, autumn = -1;
		for (int d = 0; d < 364 && (spring == -1 || autumn == -1); d++){
			GameCalendar.Season s = EventSeasons.of( d ).season;
			if (s == GameCalendar.Season.SPRING && spring == -1) spring = d;
			if (s == GameCalendar.Season.AUTUMN && autumn == -1) autumn = d;
		}
		WorldEvents.Event n = MigrationEvent.forcedMigration( 500, 500, spring * FC + 900 );
		WorldEvents.Event s = MigrationEvent.forcedMigration( 500, 500, autumn * FC + 900 );
		assertNotEquals( WorldEvents.idOf( WorldEvents.Type.MIGRATION, n.sx, n.sy, n.startDay ), n.id );
		assertTrue( MigrationEvent.roadOf( SEED, n ).y1 < 500 - MigrationEvent.LENGTH + 2 );
		assertTrue( MigrationEvent.roadOf( SEED, s ).y1 > 500 + MigrationEvent.LENGTH - 2 );
		assertEquals( MigrationEvent.DAYS * FC, n.endTurn - n.startTurn );
		//its middle is where the debug scenes put it
		int now = spring * FC + 2000;
		WorldEvents.Event m = MigrationEvent.forcedMigration( 500, 500, MigrationEvent.debugSetOff( now, 30 ) );
		int[] mid = MigrationEvent.middleNow( SEED, m, now );
		assertEquals( 500, mid[0], 2 );
		assertEquals( 470, mid[1], 2 );
	}

	@Test
	public void aHerdsBeastKeepsItsPlaceThroughASave(){
		Migrating g = new Migrating();
		g.herd = 4242L;
		g.slot = MigrationEvent.WOLVES + 1;
		g.x0 = 10;
		g.y0 = -20;
		g.x1 = 10;
		g.y1 = -260;
		g.startTurn = 100;
		g.endTurn = 100 + 4 * FC;
		g.offset = 60;
		g.lateral = -2;
		g.huntedNight = 33;
		g.defending = true;
		Bundle b = new Bundle();
		b.put( "tag", g );
		Migrating back = (Migrating) b.get( "tag" );
		assertEquals( 4242L, back.herd );
		assertTrue( back.wolf() );
		assertEquals( -260, back.y1 );
		assertEquals( 100 + 4 * FC, back.endTurn );
		assertEquals( 60, back.offset );
		assertEquals( -2, back.lateral );
		assertEquals( 33, back.huntedNight );
		assertTrue( back.defending );
		//an older tag without its keys reads as nobody's
		Migrating empty = new Migrating();
		empty.restoreFromBundle( new Bundle() );
		assertEquals( Long.MIN_VALUE, empty.herd );
		assertFalse( empty.defending );
		for (String k : new String[]{ "heard", "heard_here", "hunt" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( MigrationEvent.class, k ) );
		}
	}
}
