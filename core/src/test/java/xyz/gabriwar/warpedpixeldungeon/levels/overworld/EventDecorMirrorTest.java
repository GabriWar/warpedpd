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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant;
import xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver;
import xyz.gabriwar.warpedpixeldungeon.sprites.VillagerSprite;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A co-op guest sees the world's timed events on the ground as his host does: his mirror
 * (EventDecorMirror) works out the hot craters and the markets' bunting from the world and the
 * log his host shipped, the traders being the host's stand-ins round the well, and comes to the
 * very lists the host's step shows - the same craters as hot, the same markets strung to the
 * same stalls at the same places, a villager's stand-in by the well never taken for a trader.
 * A market the host settled is taken down on the guest's screen as soon as the log says so.
 */
public class EventDecorMirrorTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, startDay, challenges, depth, branch;
	private Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		xyz.gabriwar.warpedpixeldungeon.items.rings.Ring.initGems();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		hero = new Hero();
		hero.lvl = 12;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	private static void setField( Object o, String name, Object v ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Object read( Object o, String name ){
		try {
			Field f = o.getClass().getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//a market near the origin in a village of four houses or more, a day or two in
	private static WorldEvents.Event marketDue(){
		for (int p = 0; p < 40; p++){
			for (int ry = -3; ry <= 3; ry++){
				for (int rx = -3; rx <= 3; rx++){
					WorldEvents.Event e = WorldEvents.market( SEED, rx, ry, p );
					if (e != null && e.startDay >= 2 && (WorldStructures.settlementLayout( SEED, e.sx, e.sy ).length - 1) / 2 >= 4) return e;
				}
			}
		}
		throw new AssertionError( "no market near the origin" );
	}

	@Test
	public void theGuestsMirrorShowsWhatTheHostsStepDoes(){
		WorldEvents.Event market = marketDue();
		Dungeon.cycleTurn = market.startTurn + 400;
		float shift = WorldModel.calendarShift();
		int ox = market.wx - W / 2, oy = market.wy + 3 - H / 2;
		PathFinder.setMapSize( W, H );
		WindowGenerator.Window win = WindowGenerator.generate( SEED, 0, ox, oy, shift );

		//the host: his window round the market's well, a star come down twelve cells off it
		OverworldLevel host = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), win.terrain, W, H );
		assertNotNull( host );
		setField( host, "network", false );
		Dungeon.level = host;
		hero.pos = host.localCell( market.wx, market.wy + 3 );
		Arrays.fill( host.heroFOV, false );
		int now = host.eventTurn();
		host.forceEvent( WorldEvents.forcedStar( market.wx + 12, market.wy - 20, now - WorldEvents.STAR_HOT / 2 ) );
		host.pollEvents();
		ArrayList<Mob> traders = new ArrayList<>();
		for (Mob m : host.mobs) if (m instanceof TravellingMerchant) traders.add( m );
		assertTrue( "no traders: " + traders.size(), traders.size() >= 2 );
		ArrayList<WorldEventDecor.Crater> hc = new ArrayList<>();
		ArrayList<WorldEventDecor.Fair> hf = new ArrayList<>();
		host.eventDecor( host.nearEvents(), now, OverworldLevel::tradesFor, hc, hf );
		assertEquals( 1, hc.size() );
		assertEquals( 1, hf.size() );
		assertEquals( 2, read( hc.get( 0 ), "heat" ) );

		//the guest: the same window as a mirror, the host's mobs as his stand-ins, a villager's by
		//the well among them, and the log his host shipped
		OverworldLevel guest = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), win.terrain, W, H );
		assertNotNull( guest );
		for (Mob m : host.mobs){
			SpectatorReceiver.SpectatorMob s = new SpectatorReceiver.SpectatorMob();
			s.pos = m.pos;
			s.hostId = m.id();
			s.spriteClass = m.spriteClass;
			guest.mobs.add( s );
		}
		SpectatorReceiver.SpectatorMob folk = new SpectatorReceiver.SpectatorMob();
		folk.pos = host.localCell( market.wx + 1, market.wy + 1 );
		folk.spriteClass = VillagerSprite.class;
		guest.mobs.add( folk );
		guest.adoptSharedEvents( host.sharedEvents() );
		EventDecorMirror mirror = new EventDecorMirror( guest );
		ArrayList<WorldEventDecor.Crater> gc = new ArrayList<>();
		ArrayList<WorldEventDecor.Fair> gf = new ArrayList<>();
		mirror.gather( now, gc, gf );

		assertTrue( "the guest's lists differ from the host's", WorldEventDecor.same( gc, gf, hc, hf ) );
		for (int i = 0; i < hc.size(); i++){
			assertEquals( read( hc.get( i ), "x" ), read( gc.get( i ), "x" ) );
			assertEquals( read( hc.get( i ), "y" ), read( gc.get( i ), "y" ) );
		}
		for (int i = 0; i < hf.size(); i++){
			assertEquals( read( hf.get( i ), "wellX" ), read( gf.get( i ), "wellX" ) );
			assertEquals( read( hf.get( i ), "wellY" ), read( gf.get( i ), "wellY" ) );
			assertArrayEquals( (float[]) read( hf.get( i ), "stallXY" ), (float[]) read( gf.get( i ), "stallXY" ), 0f );
			assertEquals( traders.size(), ((long[]) read( gf.get( i ), "stalls" )).length );
		}

		//the host chases the market off: once his log comes, the guest's bunting comes down with
		//it, though the stand-ins are still where they stood
		host.resolveEvent( market.id, market.endTurn );
		hc.clear();
		hf.clear();
		host.eventDecor( host.nearEvents(), now, OverworldLevel::tradesFor, hc, hf );
		assertTrue( hf.isEmpty() );
		guest.adoptSharedEvents( host.sharedEvents() );
		gc.clear();
		gf.clear();
		mirror.gather( now, gc, gf );
		assertTrue( gf.isEmpty() );
		assertEquals( 1, gc.size() );

		//and the crater cools on both clocks alike
		int later = now + WorldEvents.STAR_HOT / 2 - 1;
		List<WorldEventDecor.Crater> hot = new ArrayList<>(), mirrored = new ArrayList<>();
		host.eventDecor( host.nearEvents(), later, OverworldLevel::tradesFor, hot, new ArrayList<>() );
		mirror.gather( later, mirrored, new ArrayList<>() );
		assertEquals( 1, hot.size() );
		assertEquals( 1, read( hot.get( 0 ), "heat" ) );
		assertTrue( WorldEventDecor.same( mirrored, new ArrayList<>(), hot, new ArrayList<>() ) );
		assertFalse( mirrored.isEmpty() );
	}
}
