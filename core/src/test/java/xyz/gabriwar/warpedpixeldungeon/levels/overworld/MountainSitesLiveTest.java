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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ThermalVent;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpringSoak;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Hermit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.EagleEgg;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfWarmth;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CairnSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.EagleSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HermitSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The places on the mountains, live on a real peak window (built as a mirror's is, then made the
 * host's own): a hot spring warms through, hoards are laid once through a save, the eagles are
 * placed once and keep to their nest, the tower's lair fills at most once a week, the hermit looks
 * at an item for nothing once a day, the hut, the shelter and the springs smoke, glow and steam,
 * every string resolves and old saves load.
 */
public class MountainSitesLiveTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, turn, startDay, gold;
	private DayNightCycle.Phase override;
	private float temp;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		gold = Dungeon.gold;
		override = DayNightCycle.debugPhaseOverride;
		temp = ClimateManager.debugTempOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.calendarStartDay = 0;
		hero = new Hero();
		hero.lvl = 20;
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.gold = gold;
		DayNightCycle.debugPhaseOverride = override;
		ClimateManager.debugTempOverride = temp;
		WorldModel.releaseSeasonShift();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void die(){ }
			@Override public void move( int from, int to ){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showAlert(){ }
			@Override public void hideAlert(){ }
			@Override public void operate( int cell ){ }
		};
		sp.visible = false;
		return sp;
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

	private static boolean act( Actor a ){
		try {
			Method m = Actor.class.getDeclaredMethod( "act" );
			m.setAccessible( true );
			return (Boolean) m.invoke( a );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//the live window of a site's slice, centred on it, the hero standing on (wx, wy) or the nearest open cell
	private OverworldLevel windowAt( MountainSites.Site s, int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = s.wx - W / 2, oy = s.wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, ox, oy, 0f );
		OverworldLevel ow = OverworldLevel.forNetwork( s.altitude, SEED, ox, oy, 0f, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.level = ow;
		Dungeon.depth = WorldLayers.depthOf( s.altitude );
		hero.pos = -1;
		for (int r = 0; r <= 8 && hero.pos == -1; r++){
			for (int dy = -r; dy <= r && hero.pos == -1; dy++){
				for (int dx = -r; dx <= r && hero.pos == -1; dx++){
					int cell = ow.localCell( wx + dx, wy + dy );
					if (cell != -1 && ow.passable[cell] && !ow.occupied( cell )) hero.pos = cell;
				}
			}
		}
		assertTrue( hero.pos != -1 );
		assertTrue( ow.mountainSites().contains( s ) );
		return ow;
	}

	private static MountainSites.Site nearest( MountainSites.Kind k ){
		MountainSites.Site s = MountainSites.nearest( SEED, k, 0, 0, 48 );
		assertNotNull( s );
		return s;
	}

	private static ArrayList<Eagle> eagles( OverworldLevel ow, long key ){
		ArrayList<Eagle> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof Eagle && ((Eagle) m).eyrieKey == key) out.add( (Eagle) m );
		for (Mob m : ow.parked()) if (m instanceof Eagle && ((Eagle) m).eyrieKey == key) out.add( (Eagle) m );
		return out;
	}

	private static OverworldLevel saveAndLoad( OverworldLevel ow ){
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		//a headless Game carries no version code: stamp the save as a current one
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( b );
		Dungeon.level = back;
		return back;
	}

	// ------------------------------------------------------------------ the climate

	@Test
	public void springWarmsAndCuresTheCold(){
		MountainSites.Site s = nearest( MountainSites.Kind.SPRINGS );
		OverworldLevel ow = windowAt( s, s.wx, s.wy + 9 );
		ClimateManager.debugTempOverride = -25f;
		int pool = ow.localCell( s.wx, s.wy );
		assertTrue( ow.hotSpring( pool ) );
		assertTrue( ow.water[pool] );
		int far = ow.localCell( s.wx, s.wy + 9 );
		assertFalse( ow.hotSpring( far ) );
		hero.pos = pool;
		assertTrue( TileTemperature.feelsLikeAt( pool, hero ) >= MountainSites.POOL_C );
		assertTrue( ThermalVent.bathing( hero ) );
		//the warm ground round it
		int beside = -1;
		for (int c = 0; c < ow.length() && beside == -1; c++){
			if (ow.springWarmth( c ) == MountainSites.STEAM_C && !ow.water[c] && ow.passable[c]) beside = c;
		}
		assertTrue( beside != -1 );
		assertTrue( TileTemperature.feelsLikeAt( beside, hero ) >= MountainSites.STEAM_C );
		assertTrue( TileTemperature.feelsLikeAt( pool, hero ) > TileTemperature.feelsLikeAt( far, hero ) + 20 );
		//the freezing air never closes the pool
		if (ow.tileHeat == null) ow.tileHeat = new float[ow.length()];
		if (ow.waterPhaseProgress == null) ow.waterPhaseProgress = new float[ow.length()];
		for (int i = 0; i < 20; i++) TileTemperature.stepDiffusion( ow );
		assertEquals( Terrain.WATER, ow.map[pool] );
		//stepping into it soaks the hero
		ow.occupyCell( hero );
		assertNotNull( hero.buff( SpringSoak.class ) );
	}

	// ------------------------------------------------------------------ once only

	@Test
	public void hoardsAreLaidOnceThroughASave(){
		MountainSites.Site e = nearest( MountainSites.Kind.EYRIE );
		OverworldLevel ow = windowAt( e, e.wx - MountainSites.DX[e.dir] * 8, e.wy - MountainSites.DY[e.dir] * 8 );
		ow.populateMountainSites();
		ow.populateMountainSites();
		int nest = ow.localCell( e.wx, e.wy );
		Heap heap = ow.heaps.get( nest );
		assertNotNull( heap );
		assertTrue( "the eggs lie on top", heap.peek() instanceof EagleEgg );
		//gold and eggs, and in one nest of three a gem
		int hoard = Math.floorMod( OverworldLevel.structHash( e.key, 3 ), 3L ) == 0 ? 3 : 2;
		assertEquals( hoard, heap.items.size() );
		assertEquals( 2, eagles( ow, e.key ).size() );
		OverworldLevel back = saveAndLoad( ow );
		back.populateMountainSites();
		assertEquals( hoard, back.heaps.get( nest ).items.size() );
		assertEquals( 2, eagles( back, e.key ).size() );
		assertFalse( back.claimHoard( e.key ) );

		MountainSites.Site c = nearest( MountainSites.Kind.CLIMBER );
		ow = windowAt( c, c.wx, c.wy + 3 );
		ow.populateMountainSites();
		ow.populateMountainSites();
		Heap remains = ow.heaps.get( ow.localCell( c.wx, c.wy ) );
		assertNotNull( remains );
		assertEquals( Heap.Type.SKELETON, remains.type );
		boolean warm = false;
		for (Item it : remains.items) warm |= it instanceof ElixirOfWarmth;
		assertTrue( "the climber's warmth", warm );
		int items = remains.items.size();
		back = saveAndLoad( ow );
		back.populateMountainSites();
		assertEquals( items, back.heaps.get( back.localCell( c.wx, c.wy ) ).items.size() );
		assertFalse( back.claimHoard( c.key ) );

		//a cairn whose stone is added stays topped, whatever becomes of the pile
		MountainSites.Site top = nearest( MountainSites.Kind.CAIRN );
		ow = windowAt( top, top.wx, top.wy + 3 );
		assertTrue( ow.claimHoard( top.key ) );
		ow.populateMountainSites();
		ow.populateMountainSites();
		int cairns = 0;
		for (Mob m : ow.mobs){
			if (m instanceof SummitCairn && ((SummitCairn) m).siteKey == top.key){
				cairns++;
				assertTrue( ((SummitCairn) m).topped );
				assertEquals( top.feet(), ((SummitCairn) m).feet );
			}
		}
		assertEquals( 1, cairns );
	}

	// ------------------------------------------------------------------ the eagles

	@Test
	public void eaglesArePlacedOnceAndGuardTheirNest(){
		assertTrue( Eagle.nearNest( 0, 0, 3, -3, Eagle.AGGRO ) );
		assertFalse( Eagle.nearNest( 0, 0, 4, 0, Eagle.AGGRO ) );
		assertTrue( "no nest: no leash", Eagle.nearNest( Integer.MIN_VALUE, 0, 400, 0, 1 ) );

		MountainSites.Site e = nearest( MountainSites.Kind.EYRIE );
		OverworldLevel ow = windowAt( e, e.wx - MountainSites.DX[e.dir] * 8, e.wy - MountainSites.DY[e.dir] * 8 );
		ow.populateMountainSites();
		ArrayList<Eagle> pair = eagles( ow, e.key );
		assertEquals( 2, pair.size() );
		for (Eagle g : pair){
			g.sprite = stub();
			Actor.add( g );
			assertTrue( g.guards( e.wx, e.wy ) );
			assertFalse( g.angered );
		}
		Actor.add( hero );

		//the eggs taken off the nest set both on the thief; taken anywhere else, nobody minds
		int nest = ow.localCell( e.wx, e.wy );
		new EagleEgg().doPickUp( hero, hero.pos );
		for (Eagle g : pair) assertFalse( g.angered );
		new EagleEgg().doPickUp( hero, nest );
		for (Eagle g : pair){
			assertTrue( g.angered );
			assertTrue( g.state == g.HUNTING );
		}

		//one that is not robbed does not chase a trespasser off its ground: back to circling, time spent
		Eagle g = pair.get( 0 );
		g.angered = false;
		g.aggro( hero );
		int sevenOff = -1;
		for (int c = 0; c < ow.length() && sevenOff == -1; c++){
			if (ow.passable[c] && ow.distance( c, nest ) == 7 && !ow.occupied( c )) sevenOff = c;
		}
		hero.pos = sevenOff;
		float before = g.cooldown();
		assertTrue( act( g ) );
		assertTrue( g.state == g.WANDERING );
		assertTrue( "it spent its turn", g.cooldown() > before );

		//a robbed one lured twenty cells off gives up and flies home
		Eagle h = pair.get( 1 );
		int lured = -1;
		for (int c = 0; c < ow.length() && lured == -1; c++){
			if (!ow.solid[c] && ow.distance( c, nest ) == 20 && !ow.occupied( c )) lured = c;
		}
		h.pos = lured;
		before = h.cooldown();
		assertTrue( act( h ) );
		assertFalse( h.angered );
		assertTrue( h.state == h.WANDERING );
		assertTrue( h.cooldown() > before );

		//however far they roam, the eyrie never grows another pair
		ow.populateMountainSites();
		ow.populateMountainSites();
		assertEquals( 2, eagles( ow, e.key ).size() );
		assertEquals( 2, ow.eyrieGuards( e.key ) );

		//when both are dead the eyrie is cleared for good (a hero far past them rolls no meat for
		//them: a dropped heap has no sprite headless)
		Actor.clear();
		hero.lvl = 99;
		for (Eagle d : eagles( ow, e.key )){
			d.sprite = stub();
			d.die( null );
		}
		assertEquals( 0, ow.eyrieGuards( e.key ) );
		assertTrue( ow.clearedSites().contains( e.key ) );
	}

	private static Object call( OverworldLevel ow, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( ow, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	@Test
	public void anEagleParkedOverTheDropComesBack(){
		MountainSites.Site e = nearest( MountainSites.Kind.EYRIE );
		OverworldLevel ow = windowAt( e, e.wx - MountainSites.DX[e.dir] * 8, e.wy - MountainSites.DY[e.dir] * 8 );
		ow.populateMountainSites();
		ArrayList<Eagle> pair = eagles( ow, e.key );
		assertEquals( 2, pair.size() );
		int nest = ow.localCell( e.wx, e.wy );
		//open air over the drop, with no ground within two cells to set anything down on
		int air = -1;
		for (int c = 0; c < ow.length() && air == -1; c++){
			int x = c % W, y = c / W;
			if (x < 4 || y < 4 || x > W - 5 || y > H - 5 || ow.solid[c] || ow.passable[c]) continue;
			if (ow.freeSpotWithin( c, 2 ) == -1) air = c;
		}
		assertTrue( "no open air over a drop in the eyrie's window", air != -1 );
		for (Eagle g : pair){
			g.pos = air;
			ow.mobs.remove( g );
			call( ow, "park", new Class<?>[]{ Mob.class, int.class, int.class }, g, ow.worldX() + air % W, ow.worldY() + air / W );
		}
		assertEquals( 2, ow.eyrieGuards( e.key ) );
		call( ow, "unparkMobs", new Class<?>[0] );
		int back = 0;
		for (Mob m : ow.mobs){
			if (m instanceof Eagle && ((Eagle) m).eyrieKey == e.key){
				back++;
				assertTrue( "home at its nest", ow.distance( m.pos, nest ) <= 3 );
			}
		}
		assertEquals( "both eagles are back over their nest", 2, back );
	}

	@Test
	public void aCrowdedNestStillGetsItsPairOnceItClears(){
		MountainSites.Site e = nearest( MountainSites.Kind.EYRIE );
		OverworldLevel ow = windowAt( e, e.wx - MountainSites.DX[e.dir] * 8, e.wy - MountainSites.DY[e.dir] * 8 );
		int nest = ow.localCell( e.wx, e.wy );
		//every cell round the nest is taken: no eagle can be set down, and none is spent
		ArrayList<Mob> crowd = new ArrayList<>();
		Mob onNest = null;
		for (int c = 0; c < ow.length(); c++){
			if (ow.distance( c, nest ) > 4 || !ow.passable[c] || ow.occupied( c )) continue;
			Mob m = new Yeti();
			m.pos = c;
			ow.mobs.add( m );
			crowd.add( m );
			if (c == nest) onNest = m;
		}
		assertNotNull( onNest );
		ow.populateMountainSites();
		assertEquals( 0, eagles( ow, e.key ).size() );
		//the crowd moves on but for one beast on the nest itself: the pair comes, beside it
		for (Mob m : crowd) if (m != onNest) ow.mobs.remove( m );
		ow.populateMountainSites();
		ArrayList<Eagle> pair = eagles( ow, e.key );
		assertEquals( 2, pair.size() );
		for (Eagle g : pair){
			assertTrue( g.pos != nest );
			assertTrue( ow.distance( g.pos, nest ) <= 3 );
		}
		ow.populateMountainSites();
		assertEquals( "still the one pair", 2, eagles( ow, e.key ).size() );
	}

	@Test
	public void aCirclingEagleStoopsOnlyNearItsNestOrWhenStruck() throws Exception {
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		//a bright noon (the sky's light is the climate's, never stepped headless): full sight
		Field sun = ClimateManager.class.getDeclaredField( "sunLight" );
		sun.setAccessible( true );
		float wasSun = sun.getFloat( null );
		sun.setFloat( null, 1f );
		try {
			circleAndStoop();
		} finally {
			sun.setFloat( null, wasSun );
		}
	}

	private void circleAndStoop(){
		MountainSites.Site e = nearest( MountainSites.Kind.EYRIE );
		OverworldLevel ow = windowAt( e, e.wx - MountainSites.DX[e.dir] * 12, e.wy - MountainSites.DY[e.dir] * 12 );
		ow.populateMountainSites();
		ArrayList<Eagle> pair = eagles( ow, e.key );
		Eagle g = pair.get( 0 );
		ow.mobs.remove( pair.get( 1 ) );
		g.sprite = stub();
		Actor.add( g );
		Actor.add( hero );
		int nest = ow.localCell( e.wx, e.wy );
		//what it sees from the nest, the hero well off
		g.pos = nest;
		act( g );
		boolean[] seen = g.fieldOfView.clone();
		int[] at = new int[7];
		for (int c = 0; c < ow.length(); c++){
			int d = ow.distance( c, nest );
			if (d < at.length && at[d] == 0 && seen[c] && ow.passable[c] && !ow.occupied( c ) && c != nest) at[d] = c;
		}
		assertTrue( "cells in its sight 3, 5 and 6 from the nest", at[3] != 0 && at[5] != 0 && at[6] != 0 );

		//in plain sight five cells off the nest: the eagle lets the hero pass
		g.state = g.WANDERING;
		g.pos = nest;
		hero.pos = at[5];
		act( g );
		assertTrue( g.state == g.WANDERING );
		assertFalse( g.angered );

		//three cells off: it stoops
		g.pos = nest;
		hero.pos = at[3];
		act( g );
		assertTrue( g.state == g.HUNTING );

		//struck from six cells off: it remembers who did it and stoops all the same
		g.state = g.WANDERING;
		g.pos = nest;
		hero.pos = at[6];
		g.damage( 5, hero );
		act( g );
		assertTrue( g.angered );
		assertTrue( g.state == g.HUNTING );
	}

	@Test
	public void theTowerLairFillsAtMostOnceAWeek(){
		MountainSites.Site t = null;
		for (int r = 0; r < 48 && t == null; r++){
			MountainSites.Site s = MountainSites.nearest( SEED, MountainSites.Kind.TOWER, r * 64 * 3, 0, 48 );
			if (s != null && Math.floorMod( OverworldLevel.structHash( s.key, 0x7011L ), 3L ) != 2) t = s;
		}
		assertNotNull( "no tower with a lair", t );
		OverworldLevel ow = windowAt( t, t.wx, t.wy + 6 );
		int occupants = 0;
		for (int i = 0; i < 3; i++){
			ow.populateMountainSites();
			//the lot wanders off, out of the tower's census
			for (Mob m : ow.mobs){
				if (m instanceof Yeti || m instanceof OverworldBandit){
					if (i == 0) occupants++;
					m.pos = ow.localCell( t.wx + 15, t.wy + 15 );
				}
			}
		}
		int total = 0;
		for (Mob m : ow.mobs) if (m instanceof Yeti || m instanceof OverworldBandit) total++;
		assertTrue( occupants > 0 );
		assertEquals( "one lot a week", occupants, total );
	}

	// ------------------------------------------------------------------ the hermit

	@Test
	public void hermitLooksAtAnItemForNothingOnceADay(){
		Hermit h = Hermit.of( 77L, 10, -20, 3 );
		int day = WorldClock.day();
		assertEquals( 0, h.idFee( day, hero ) );
		h.freeDay = day;
		assertTrue( h.idFee( day, hero ) > 0 );
		assertEquals( 0, h.idFee( day + 1, hero ) );
		Bundle b = new Bundle();
		h.storeInBundle( b );
		Hermit back = new Hermit();
		back.restoreFromBundle( b );
		assertEquals( 77L, back.siteKey );
		assertEquals( 10, back.homeX );
		assertEquals( -20, back.homeY );
		assertEquals( 3, back.altitude );
		assertEquals( day, back.freeDay );
	}

	@Test
	public void theHermitIsAtHomeAndTheShelterIsWarm(){
		MountainSites.Site s = nearest( MountainSites.Kind.HERMIT );
		OverworldLevel ow = windowAt( s, s.wx, s.wy + 5 );
		ow.populateMountainSites();
		ow.populateMountainSites();
		int hermits = 0;
		for (Mob m : ow.mobs) if (m instanceof Hermit && ((Hermit) m).siteKey == s.key) hermits++;
		assertEquals( 1, hermits );
		assertTrue( ow.shelterAt( ow.localCell( s.wx, s.wy ) ) );
		assertFalse( ow.shelterAt( ow.localCell( s.wx, s.wy + 5 ) ) );

		MountainSites.Site p = nearest( MountainSites.Kind.PASS );
		ow = windowAt( p, p.wx, p.wy );
		ow.populateMountainSites();
		ow.populateMountainSites();
		int[] bed = MountainSites.bedrollCell( p );
		int beds = 0;
		for (Mob m : ow.mobs){
			if (m instanceof Bedroll){
				beds++;
				assertTrue( ((Bedroll) m).shelter );
				assertEquals( ow.localCell( bed[0], bed[1] ), m.pos );
			}
		}
		assertEquals( 1, beds );
		int[] hearth = MountainSites.hearthCell( SEED, p );
		assertEquals( Terrain.EMBERS, ow.map[ow.localCell( hearth[0], hearth[1] )] );
		assertTrue( ow.shelterAt( ow.localCell( p.wx, p.wy ) ) );
		assertTrue( TileTemperature.nearFire( ow.localCell( bed[0], bed[1] ) ) );
	}

	// ------------------------------------------------------------------ what burns

	@Test
	public void thePeaksSmokeGlowAndSteam(){
		for (MountainSites.Kind k : new MountainSites.Kind[]{ MountainSites.Kind.HERMIT, MountainSites.Kind.PASS, MountainSites.Kind.SPRINGS }){
			MountainSites.Site s = nearest( k );
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, ox, oy, 0f );
			ArrayList<SettlementAmbience.Light> lights = new ArrayList<>();
			ArrayList<SettlementAmbience.Smoke> smoke = new ArrayList<>();
			DayNightCycle.Phase phase = k == MountainSites.Kind.PASS ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DUSK;
			SettlementAmbience.collectPeaks( SEED, MountainSitesVerifyTest.peaks( w ), w.terrain, W, H, ox, oy, s.wx, s.wy + 4,
					phase, 0.9f, GameCalendar.Season.WINTER, -10f, false, new HashSet<>(), lights, smoke, new HashSet<>() );
			int px = 16;
			switch (k){
				case HERMIT:
					assertEquals( "both windows lit", 2, lights.size() );
					assertEquals( 1, smoke.size() );
					//out of the stack above the back wall, never in the room
					assertTrue( smoke.get( 0 ).y < (s.wy - 2 - oy) * px );
					assertTrue( smoke.get( 0 ).gated );
					break;
				case PASS: {
					int[] hearth = MountainSites.hearthCell( SEED, s );
					assertEquals( 1, lights.size() );
					assertEquals( SettlementLights.KIND_HEARTH, lights.get( 0 ).kind );
					assertEquals( 1, smoke.size() );
					assertNotNull( smoke.get( 0 ).flame );
					assertEquals( (hearth[0] - ox) * px + SettlementLights.HEARTH_MOUTH[0], smoke.get( 0 ).flame[0], 0f );
					//out of the stack straight over the fire, above the north wall
					assertTrue( smoke.get( 0 ).y < (s.wy - 2 - oy) * px );
					assertEquals( (hearth[0] - ox) * px + SettlementLights.CHIMNEY_MOUTH[0], smoke.get( 0 ).x, 0f );
					break;
				}
				default:
					assertEquals( 1, smoke.size() );
					assertTrue( smoke.get( 0 ).steam );
					assertTrue( smoke.get( 0 ).gated );
			}
		}
	}

	// ------------------------------------------------------------------ strings, sprites, saves

	@Test
	public void everyStringResolves(){
		ArrayList<String> keys = new ArrayList<>();
		for (MountainSites.Kind k : MountainSites.Kind.values()){
			keys.add( "found_" + k.lower() );
			keys.add( "rumour_" + k.lower() );
			if (k != MountainSites.Kind.PASS) keys.add( "map_" + k.lower() );
		}
		for (String d : new String[]{ "n", "ne", "e", "se", "s", "sw", "w", "nw" }) keys.add( "dir_" + d );
		for (String k : new String[]{ "height_same", "height_up_1", "height_up", "height_down_1", "height_down",
				"summit", "summit_mood", "summit_again", "tower_view", "journal_title", "journal_0", "journal_1",
				"journal_2", "journal_3", "pass_sign", "nest", "nest_desc", "hearth", "hearth_desc", "spring",
				"spring_desc", "tower_top", "tower_top_desc", "place_site", "rumour_cave", "rumour_vein", "rumour_none" }){
			keys.add( k );
		}
		for (String k : keys) assertResolves( MountainSites.class, k );
		assertResolves( OverworldLevel.class, "tower_wall" );
		assertResolves( OverworldLevel.class, "tower_wall_desc" );
		assertResolves( OverworldLevel.class, "hut_wall" );
		for (String k : new String[]{ "name", "desc", "robbed" }) assertResolves( Eagle.class, k );
		for (String k : new String[]{ "name", "desc", "opt_trade", "opt_identify", "opt_identify_free", "opt_rumour",
				"identify_prompt", "no_gold", "identified", "warn", "flee", "talk_0", "talk_1", "talk_2", "talk_3", "talk_4" }){
			assertResolves( Hermit.class, k );
		}
		assertResolves( SummitCairn.class, "name" );
		assertResolves( SummitCairn.class, "desc" );
		for (String k : new String[]{ "name", "desc", "eat_msg" }) assertResolves( EagleEgg.class, k );
		assertFalse( new EagleEgg().name().contains( "!!!" ) );
	}

	private static void assertResolves( Class<?> cls, String key ){
		String s = Messages.get( cls, key );
		assertFalse( cls.getSimpleName() + "." + key, s.contains( Messages.NO_TEXT_FOUND ) );
	}

	@Test
	public void spritesAreGuestBuildable() throws Exception {
		for (Class<?> c : new Class<?>[]{ HermitSprite.class, EagleSprite.class, CairnSprite.class }){
			assertEquals( "xyz.gabriwar.warpedpixeldungeon.sprites", c.getPackage().getName() );
			assertTrue( Modifier.isPublic( c.getConstructor().getModifiers() ) );
		}
		assertEquals( HermitSprite.class, new Hermit().spriteClass );
		assertEquals( EagleSprite.class, new Eagle().spriteClass );
		assertEquals( CairnSprite.class, new SummitCairn().spriteClass );
	}

	@Test
	public void oldSavesLoad(){
		//every new field falls back when its key is missing
		Eagle e = Eagle.of( 9L, 4, 5 );
		e.angered = true;
		Bundle b = new Bundle();
		e.storeInBundle( b );
		for (String k : new String[]{ "eyrie_key", "nest_x", "nest_y", "angered" }) b.remove( k );
		Eagle old = new Eagle();
		old.restoreFromBundle( b );
		assertEquals( 0L, old.eyrieKey );
		assertEquals( Integer.MIN_VALUE, old.nestX );
		assertFalse( old.angered );

		Bedroll bed = new Bedroll();
		b = new Bundle();
		bed.storeInBundle( b );
		b.remove( "shelter" );
		Bedroll oldBed = new Bedroll();
		oldBed.restoreFromBundle( b );
		assertFalse( oldBed.shelter );

		Hermit h = Hermit.of( 3L, 1, 2, 4 );
		b = new Bundle();
		h.storeInBundle( b );
		for (String k : new String[]{ "hermit_site", "home_x", "home_y", "hermit_alt", "free_day", "first_stock" }) b.remove( k );
		Hermit oldH = new Hermit();
		oldH.restoreFromBundle( b );
		assertEquals( 0L, oldH.siteKey );
		assertEquals( Integer.MIN_VALUE, oldH.homeX );
		assertEquals( 1, oldH.altitude );
		assertEquals( Integer.MIN_VALUE, oldH.freeDay );

		SummitCairn c = new SummitCairn();
		c.topped = true;
		b = new Bundle();
		c.storeInBundle( b );
		for (String k : new String[]{ "cairn_site", "summit_x", "summit_y", "feet", "topped" }) b.remove( k );
		SummitCairn oldC = new SummitCairn();
		oldC.restoreFromBundle( b );
		assertFalse( oldC.topped );
		assertEquals( 0, oldC.feet );

		//a peak's level saved before the places existed loads them anew
		MountainSites.Site s = nearest( MountainSites.Kind.HERMIT );
		OverworldLevel ow = windowAt( s, s.wx, s.wy + 5 );
		OverworldLevel back = saveAndLoad( ow );
		assertTrue( back.mountainSites().contains( s ) );
	}
}
