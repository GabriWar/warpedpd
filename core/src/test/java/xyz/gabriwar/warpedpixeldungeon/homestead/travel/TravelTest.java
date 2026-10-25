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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WellFed;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeActions;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeAnchors;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leashed;
import xyz.gabriwar.warpedpixeldungeon.homestead.pieces.TravelPieces;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The travel part (homestead/travel): the trip's cost and its cap, the waystones put down once per
 * big settlement and kept through window moves and saves, attunement kept with the run, three homes
 * at most, where a trip lands when the stone's side is taken, a trip to another slice landing beside
 * the right stone with no time passing and the hunger paid, an animal on a rope taken along, the
 * refusals, the roads' quicker going, the homes' names, the hearthstone's menu, the map's marks and
 * every string resolving. Every window is a real one of the world, derived from a seed.
 */
public class TravelTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, challenges, startDay, turn;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level savedLevel;
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
		challenges = Dungeon.challenges;
		startDay = Dungeon.calendarStartDay;
		turn = Dungeon.cycleTurn;
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 4242 );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
		HomesteadWorld.reset();
	}

	@After
	public void tearDown(){
		//an arrival a test set and no level took
		if (OverworldLevel.hasPendingArrival()) windowAt( 0, 0 ).getTransition( null );
		HomesteadWorld.reset();
		Random.popGenerator();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	//the live window of a slice round a world cell, built as a mirror's is and made the host's own
	private static OverworldLevel windowAt( int altitude, int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	private static OverworldLevel windowAt( int wx, int wy ){
		return windowAt( 0, wx, wy );
	}

	private static void set( Object o, String name, Object value ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, value );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//an open, unedited, unprotected cell with open ground all round it, nearest the window's middle
	private static int openCell( OverworldLevel ow ){
		int mid = W / 2 + (H / 2) * W;
		for (int r = 0; r < 70; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = mid + dx + dy * W;
					if (open3( ow, c )) return c;
				}
			}
		}
		throw new AssertionError( "no open ground in the window" );
	}

	//a surface window with open ground in it (openCell finds it)
	private static OverworldLevel openWindow(){
		for (int[] at : new int[][]{ { 600, -500 }, { 300, 300 }, { -400, 260 }, { -900, -700 }, { 1500, 900 } }){
			OverworldLevel ow = windowAt( at[0], at[1] );
			int mid = W / 2 + (H / 2) * W;
			for (int r = 0; r < 70; r++){
				for (int dy = -r; dy <= r; dy++){
					for (int dx = -r; dx <= r; dx++){
						if (Math.max( Math.abs( dx ), Math.abs( dy ) ) == r && open3( ow, mid + dx + dy * W )) return ow;
					}
				}
			}
		}
		throw new AssertionError( "no open ground in any test window" );
	}

	private static boolean open3( OverworldLevel ow, int c ){
		int x0 = c % W, y0 = c / W;
		for (int y = y0 - 2; y <= y0 + 2; y++){
			for (int x = x0 - 2; x <= x0 + 2; x++){
				if (x < 3 || y < 3 || x >= W - 3 || y >= H - 3) return false;
				int n = x + y * W;
				int t = ow.map[n];
				if (!ow.passable[n] || ow.pit[n] || (Terrain.flags[t] & Terrain.LIQUID) != 0 || t == Terrain.FROZEN_WATER
						|| t == Terrain.BRIDGE || Terrain.homestead( t ) || ow.protectedForBuilding( n )
						|| ow.heaps.get( n ) != null || ow.getTransition( n ) != null) return false;
				for (Mob m : ow.mobs) if (m.pos == n) return false;
			}
		}
		return true;
	}

	//the hero standing on a cell, seeing everything
	private void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
	}

	private void give( Class<? extends Item> cls, int n ){
		Item i = com.watabou.utils.Reflection.newInstance( cls );
		i.quantity( n );
		assertTrue( i.collect( hero.belongings.backpack ) );
	}

	private static BuildPiece hearthstone(){
		return BuildCatalog.byId( "hearthstone" );
	}

	private static List<Waystone> waystones( OverworldLevel ow, long key ){
		ArrayList<Waystone> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof Waystone && ((Waystone) m).key == key) out.add( (Waystone) m );
		return out;
	}

	private static TravelSlice slice( OverworldLevel ow ){
		TravelSlice s = ow.homestead().section( TravelSlice.class );
		assertNotNull( "the slice keeps the travel section", s );
		return s;
	}

	private static OverworldLevel saveAndLoad( OverworldLevel ow ) throws Exception {
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( read );
		Dungeon.level = back;
		return back;
	}

	//the run's homestead data through a real save and back
	private static void saveAndLoadWorld() throws Exception {
		Bundle g = new Bundle();
		HomesteadWorld.storeInBundle( g );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( g, out, false ) );
		HomesteadWorld.reset();
		HomesteadWorld.restoreFromBundle( Bundle.read( new ByteArrayInputStream( out.toByteArray() ) ) );
	}

	//the first big (a waystone's) settlement near the origin: {sx, sy, cx, cy, houses}
	private static int[] bigSettlement(){
		for (int[] s : WorldStructures.settlementsNear( SEED, 0, 0, null, Waystones.MIN_HOUSES, 8 )){
			if (Waystones.keepsOne( SEED, s[0], s[1] )) return s;
		}
		throw new AssertionError( "no big settlement near the origin of the test world" );
	}

	private static int[] smallSettlement(){
		for (int[] s : WorldStructures.settlementsNear( SEED, 0, 0, null, 0, 8 )){
			if (s[4] < Waystones.MIN_HOUSES) return s;
		}
		throw new AssertionError( "no small settlement near the origin of the test world" );
	}

	// ------------------------------------------------------------------ the cost

	@Test
	public void aTripCostsHalfAPointACellAndSixtyALevelUpToStarving(){
		assertEquals( 5, Travel.distance( 0, 0, 3, 4 ) );
		assertEquals( 0, Travel.hungerCost( 0, 0, 0 ) );
		assertEquals( 50, Travel.hungerCost( 100, 0, 0 ) );
		assertEquals( 50, Travel.hungerCost( 101, 0, 0 ) );
		assertEquals( "three levels down into the caves", 50 + 180, Travel.hungerCost( 100, 0, -3 ) );
		assertEquals( "the mountains to the caves", 60 * 4, Travel.hungerCost( 0, 2, -2 ) );
		assertEquals( "capped", Travel.MAX_HUNGER, Travel.hungerCost( 5000, 0, 0 ) );
		assertEquals( "capped", Travel.MAX_HUNGER, Travel.hungerCost( 0, 10, -12 ) );
		assertEquals( 450, Travel.MAX_HUNGER );
		assertEquals( (int)Hunger.STARVING, Travel.MAX_HUNGER );

		Travel.Place a = new Travel.Place( Travel.Kind.WAYSTONE, WorldLayers.SURFACE_DEPTH, 0, 0, 1, "a" );
		Travel.Place b = new Travel.Place( Travel.Kind.HOME, WorldLayers.depthOf( -2 ), 300, 400, 0, "b" );
		assertEquals( -2, b.altitude() );
		assertEquals( 500 / 2 + 120, Travel.cost( a, b ) );

		assertEquals( Messages.get( Travel.class, "meals_1" ), Travel.meals( 5 ) );
		assertEquals( Messages.get( Travel.class, "meals_2" ), Travel.meals( 150 ) );
		assertEquals( Messages.get( Travel.class, "meals_4" ), Travel.meals( 300 ) );
		assertEquals( Messages.get( Travel.class, "meals_6" ), Travel.meals( 450 ) );
		assertEquals( "the most a trip costs reads as the last of the meal counts",
				Messages.get( Travel.class, "meals_6" ), Travel.meals( Travel.hungerCost( 99999, 12, -12 ) ) );

		Hunger h = Buff.affect( hero, Hunger.class );
		h.setLevel( 0 );
		assertEquals( 0, Travel.leaves( hero, 200 ) );
		h.setLevel( 250 );
		assertEquals( "hungry", 1, Travel.leaves( hero, 60 ) );
		h.setLevel( 400 );
		assertEquals( "starving", 2, Travel.leaves( hero, 60 ) );
	}

	// ------------------------------------------------------------------ the waystones

	@Test
	public void aBigSettlementGetsOneWaystoneByItsWellOnceAndForAll() throws Exception {
		int[] s = bigSettlement();
		long key = WorldStructures.sectorOf( s[0], s[1] );
		OverworldLevel ow = windowAt( s[2], s[3] );
		slice( ow ).materialize( ow );
		slice( ow ).materialize( ow );
		List<Waystone> stones = waystones( ow, key );
		assertEquals( "one waystone", 1, stones.size() );
		Waystone w = stones.get( 0 );
		assertTrue( "beside the well", Math.max( Math.abs( w.wx - s[2] ), Math.abs( w.wy - s[3] ) ) <= 2 );
		assertEquals( ow.localCell( w.wx, w.wy ), w.pos );
		assertEquals( WorldStructures.villageName( SEED, s[0], s[1] ), w.place );
		assertTrue( Char.hasProp( w, Char.Property.IMMOVABLE ) );
		assertTrue( slice( ow ).placed( key ) );

		//the same world puts it on the same cell
		OverworldLevel again = windowAt( s[2], s[3] );
		slice( again ).materialize( again );
		assertEquals( w.wx, waystones( again, key ).get( 0 ).wx );
		assertEquals( w.wy, waystones( again, key ).get( 0 ).wy );

		//the window going far off and back: parked and put back, never a second
		Dungeon.level = ow;
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, s[2] + 900, s[3] );
		assertTrue( waystones( ow, key ).isEmpty() );
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, s[2] + 10, s[3] - 5 );
		assertEquals( 1, waystones( ow, key ).size() );
		assertEquals( ow.localCell( w.wx, w.wy ), waystones( ow, key ).get( 0 ).pos );

		//and a save
		OverworldLevel loaded = saveAndLoad( ow );
		assertEquals( 1, waystones( loaded, key ).size() );
		slice( loaded ).materialize( loaded );
		assertEquals( 1, waystones( loaded, key ).size() );
		assertEquals( w.wx, waystones( loaded, key ).get( 0 ).wx );
	}

	@Test
	public void onlyTownsAndBiggerKeepAWaystone(){
		int[] small = smallSettlement();
		assertFalse( Waystones.keepsOne( SEED, small[0], small[1] ) );
		OverworldLevel ow = windowAt( small[2], small[3] );
		slice( ow ).materialize( ow );
		assertTrue( waystones( ow, WorldStructures.sectorOf( small[0], small[1] ) ).isEmpty() );
		//a camp of outlaws as big as a town keeps one as well
		int camps = 0;
		for (int[] s : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.BANDIT, Waystones.MIN_HOUSES, 10 )){
			assertTrue( Waystones.keepsOne( SEED, s[0], s[1] ) );
			camps++;
		}
		assertTrue( "the test world has a big bandit camp near the origin", camps > 0 );
		//no slice but the surface has any
		OverworldLevel cave = windowAt( -1, small[2], small[3] );
		slice( cave ).materialize( cave );
		for (Mob m : cave.mobs) assertFalse( m instanceof Waystone );
	}

	@Test
	public void theTownKeepsItsWaystoneBesideThePlaza(){
		int tx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), ty = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		OverworldLevel ow = windowAt( tx, ty );
		slice( ow ).materialize( ow );
		List<Waystone> town = waystones( ow, Waystones.TOWN );
		assertEquals( 1, town.size() );
		assertTrue( ow.inTown( town.get( 0 ).pos ) );
		assertEquals( Waystones.townWX(), town.get( 0 ).wx );
		assertEquals( Waystones.townWY(), town.get( 0 ).wy );
		assertTrue( ow.distance( ow.localCell( tx, ty ), town.get( 0 ).pos ) <= 2 );
		assertEquals( Messages.get( Waystones.class, "town" ), town.get( 0 ).place );
	}

	@Test
	public void attunementIsKeptWithTheRunOnEverySlice() throws Exception {
		TravelWorld tw = TravelWorld.get();
		assertNotNull( tw );
		tw.see( 77L, 10, 20, "Seenford" );
		assertTrue( tw.attune( 42L, 300, -40, "Millbrook" ) );
		assertFalse( "only once", tw.attune( 42L, 300, -40, "Millbrook" ) );
		assertTrue( tw.attune( Waystones.TOWN, Waystones.townWX(), Waystones.townWY(), "Dolyahaven" ) );
		HomesteadWorld.addHome( new HomesteadWorld.Home( WorldLayers.depthOf( -3 ), 120, 80, "Deep hollow", 0 ) );

		saveAndLoadWorld();
		tw = TravelWorld.get();
		assertTrue( tw.isAttuned( 42L ) );
		assertTrue( tw.isAttuned( Waystones.TOWN ) );
		assertTrue( tw.isSeen( 77L ) );
		assertFalse( tw.isAttuned( 77L ) );
		assertEquals( "Millbrook", tw.stone( 42L ).name );
		assertEquals( 300, tw.stone( 42L ).wx );

		//from a home down in the caves both attuned stones of the surface can be reached, and back
		Travel.Place cave = Travel.Place.of( HomesteadWorld.homes().get( 0 ) );
		List<Travel.Place> there = Travel.destinations( cave );
		assertEquals( 2, there.size() );
		for (Travel.Place p : there){
			assertEquals( Travel.Kind.WAYSTONE, p.kind );
			assertEquals( WorldLayers.SURFACE_DEPTH, p.depth );
		}
		Travel.Place mill = Travel.Place.of( tw.stone( 42L ) );
		List<Travel.Place> back = Travel.destinations( mill );
		assertEquals( "the home first, then the town", Travel.Kind.HOME, back.get( 0 ).kind );
		assertEquals( 2, back.size() );
		for (Travel.Place p : back) assertFalse( "not the stone itself", p.same( mill ) );

		//a new run knows none
		HomesteadWorld.reset();
		assertFalse( TravelWorld.get().isAttuned( 42L ) );
	}

	@Test
	public void aStonePutDownLaterIsKnownByItsOwnCell(){
		TravelWorld tw = TravelWorld.get();
		tw.attune( 5L, 100, 100, "Farhold" );
		tw.placed( 5L, 101, 99 );
		assertEquals( 101, tw.stone( 5L ).wx );
		assertEquals( 99, tw.stone( 5L ).wy );
		assertTrue( tw.isAttuned( 5L ) );
	}

	// ------------------------------------------------------------------ homes

	@Test
	public void aHearthstoneFoundsAHomeAndThreeIsTheMost(){
		BuildPiece stone = hearthstone();
		assertNotNull( stone );
		assertEquals( BuildCategory.TRAVEL, stone.category );
		assertEquals( 200, stone.maxHp );
		assertEquals( RoughStone.class, stone.cost.get( 0 ).material );
		assertEquals( 10, stone.cost.get( 0 ).quantity );
		assertEquals( CutStone.class, stone.cost.get( 1 ).material );
		assertEquals( 2, stone.cost.get( 1 ).quantity );
		assertTrue( stone.confirmRemoval );
		assertFalse( stone.removalWarning().contains( "NO TEXT" ) );

		OverworldLevel ow = openWindow();
		int c = openCell( ow );
		stand( ow, c );
		new BuildersSatchel().collect( hero.belongings.backpack );
		give( RoughStone.class, 40 );
		give( CutStone.class, 8 );

		int[] spots = { c + 2, c - 2, c + 2 * W };
		for (int i = 0; i < spots.length; i++){
			assertEquals( Homestead.Reason.OK, Homestead.place( hero, spots[i], stone ) );
			assertEquals( i + 1, HomesteadWorld.homes().size() );
		}
		assertEquals( 40 - 30, Material.count( hero, RoughStone.class ) );
		HomesteadWorld.Home first = HomesteadWorld.homeAt( WorldLayers.SURFACE_DEPTH, ow.worldX + spots[0] % W, ow.worldY + spots[0] / W );
		assertNotNull( first );
		assertEquals( "visited as it went up", WorldClock.day(), first.lastVisitDay );
		assertTrue( "an anchor", HomeAnchors.near( ow, spots[0] ) );
		for (Mob m : ow.mobs){
			if (m.pos == spots[0]){
				assertTrue( m instanceof Hearthstone );
				assertEquals( first, ((Hearthstone) m).home() );
			}
		}

		//a fourth is refused
		assertEquals( Homestead.Reason.LIMIT, Homestead.canPlace( hero, c - 2 * W, stone ) );
		assertFalse( Homestead.Reason.LIMIT.message().contains( "NO TEXT" ) );
		assertEquals( TravelPieces.MAX_HOMES, HomesteadWorld.homes().size() );

		//one taken down: its home goes, and another may go up
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, spots[0] ) );
		assertNull( HomesteadWorld.homeAt( WorldLayers.SURFACE_DEPTH, first.wx, first.wy ) );
		assertEquals( 2, HomesteadWorld.homes().size() );
		assertEquals( Homestead.Reason.OK, Homestead.canPlace( hero, c - 2 * W, stone ) );

		//one wrecked goes as well
		assertTrue( Homestead.damage( ow, spots[1], 500, null ) );
		assertEquals( 1, HomesteadWorld.homes().size() );
	}

	@Test
	public void aHomeIsNamedAfterWhereItStands(){
		int[] s = bigSettlement();
		String village = WorldStructures.villageName( SEED, s[0], s[1] );
		String near = HomeNames.forNewHome( SEED, 0, s[2] + 20, s[3] );
		assertTrue( near, near.contains( village ) );
		assertTrue( HomeNames.forNewHome( SEED, -2, s[2] + 20, s[3] ).contains( village ) );
		assertTrue( HomeNames.forNewHome( SEED, 0, 3, 3 ).contains( Messages.get( Waystones.class, "town" ) ) );

		//far from any place, the land's name
		String wild = HomeNames.forNewHome( SEED, 0, 40000, 40000 );
		assertFalse( wild.contains( "NO TEXT" ) );
		assertEquals( Messages.get( HomeNames.class, "cave" ), HomeNames.forNewHome( SEED, -5, 40000, 40000 ) );

		//a name taken gets a number
		HomesteadWorld.addHome( new HomesteadWorld.Home( 97, s[2] + 20, s[3], near, 0 ) );
		String second = near + " 2";
		assertEquals( "numbered while it fits, else the short name",
				second.length() <= HomeActions.NAME_MAX ? second : Messages.get( HomeNames.class, "short_near", village ),
				HomeNames.forNewHome( SEED, 0, s[2] + 20, s[3] + 1 ) );

		//however many share a place, every name is its own and fits what Rename allows
		java.util.HashSet<String> names = new java.util.HashSet<>();
		names.add( near );
		for (int i = 1; i <= 14; i++){
			for (int alt : new int[]{ 0, -3, 2 }){
				String n = HomeNames.forNewHome( SEED, alt, s[2] + 20, s[3] + i );
				assertTrue( n, n.length() <= HomeActions.NAME_MAX );
				assertTrue( "unique: " + n, names.add( n ) );
				HomesteadWorld.addHome( new HomesteadWorld.Home( WorldLayers.depthOf( alt ), s[2] + 20, s[3] + i, n, 0 ) );
			}
		}
	}

	@Test
	public void theHearthstonesMenuTravelsFirstAndRenamesSecond(){
		HomeActions.Action ledger = new HomeActions.Action( "ledger", HomeActions.class, "ledger", () -> {} );
		HomeActions.Action rename = new HomeActions.Action( "rename", HomeActions.class, "rename", () -> {} );
		HomeActions.Action notice = new HomeActions.Action( "notice", HomeActions.class, "ledger", () -> {} );
		HomeActions.Action travel = new HomeActions.Action( TravelActions.TRAVEL, TravelActions.class, "travel", () -> {} );
		List<HomeActions.Action> menu = WndHome.ordered( Arrays.asList( ledger, rename, travel, notice ) );
		assertEquals( Arrays.asList( travel, rename, ledger, notice ), menu );

		//the travel part offers travel at every home, to the host's hero
		HomesteadWorld.Home home = new HomesteadWorld.Home( 97, 1, 2, "Here", 0 );
		boolean offered = false;
		for (HomeActions.Action a : HomeActions.forHome( home, hero )) if (a.id.equals( TravelActions.TRAVEL )) offered = true;
		assertTrue( offered );
		assertEquals( Messages.get( TravelActions.class, "travel" ), travel.label() );
	}

	// ------------------------------------------------------------------ going

	//a surface window with the town's waystone, the hero beside it and it attuned; returns the stone
	private Waystone atTheTownStone( OverworldLevel ow ){
		slice( ow ).materialize( ow );
		Waystone town = waystones( ow, Waystones.TOWN ).get( 0 );
		for (int n : PathFinder.NEIGHBOURS8){
			int c = town.pos + n;
			if (ow.passable[c] && !ow.solid[c]){
				boolean taken = false;
				for (Mob m : ow.mobs) if (m.pos == c) taken = true;
				if (!taken){
					stand( ow, c );
					break;
				}
			}
		}
		TravelWorld.get().attune( town.key, town.wx, town.wy, town.place );
		return town;
	}

	@Test
	public void aTripToACaveHomeLandsBesideItsStoneWithNoTimePassing(){
		int tx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), ty = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );

		//a home down in the caves, some way off
		OverworldLevel cave = windowAt( -2, tx + 150, ty + 60 );
		int hc = openCell( cave );
		Homestead.build( cave, hc, hearthstone(), null );
		HomesteadWorld.Home home = HomesteadWorld.homes().get( 0 );
		assertEquals( WorldLayers.depthOf( -2 ), home.depth );

		OverworldLevel ow = windowAt( tx, ty );
		Waystone town = atTheTownStone( ow );
		Travel.Place from = Travel.Place.of( town );
		Travel.Place to = Travel.Place.of( home );
		assertTrue( Travel.destinations( from ).get( 0 ).same( to ) );
		assertNull( Travel.check( hero, from, to ) );

		Hunger hunger = Buff.affect( hero, Hunger.class );
		hunger.setLevel( 10 );
		int cost = Travel.cost( from, to );
		assertEquals( Travel.distance( town.wx, town.wy, home.wx, home.wy ) / 2 + 2 * Travel.HUNGER_PER_LEVEL, cost );
		float now = Actor.now(), cooldown = hero.cooldown(), duration = Statistics.duration;
		int clock = WorldClock.turn();

		InterlevelScene.Mode mode = InterlevelScene.mode;
		int returnDepth = InterlevelScene.returnDepth, returnBranch = InterlevelScene.returnBranch;
		try {
			assertTrue( Travel.go( hero, from, to ) );
			//the scene that loads the cave slice, and lands him on the arrival
			assertEquals( InterlevelScene.Mode.RETURN, InterlevelScene.mode );
			assertEquals( home.depth, InterlevelScene.returnDepth );
			assertEquals( 0, InterlevelScene.returnBranch );
			assertTrue( OverworldLevel.hasPendingArrival() );
		} finally {
			InterlevelScene.mode = mode;
			InterlevelScene.returnDepth = returnDepth;
			InterlevelScene.returnBranch = returnBranch;
		}
		assertEquals( 10 + cost, hunger.hunger() );
		assertEquals( "no time passes", now, Actor.now(), 0f );
		assertEquals( cooldown, hero.cooldown(), 0f );
		assertEquals( duration, Statistics.duration, 0f );
		assertEquals( clock, WorldClock.turn() );
		assertNotNull( hero.buff( TravelArrival.class ) );

		//the cave slice takes the arrival: beside the hearthstone, never on it
		Dungeon.level = cave;
		int land = cave.getTransition( null ).cell();
		assertNotEquals( hc, land );
		assertTrue( cave.adjacent( land, hc ) );
		assertTrue( cave.passable[land] );
	}

	@Test
	public void anArrivalWhoseSideIsTakenLandsOnTheNearestFreeCellRoundTheStone(){
		OverworldLevel ow = openWindow();
		int c = openCell( ow );
		Waystone stone = new Waystone();
		stone.key = 9L;
		stone.pos = c;
		ow.mobs.add( stone );
		int wx = ow.worldX + c % W, wy = ow.worldY + c / W;

		OverworldLevel.arriveAt( wx, wy );
		int land = ow.getTransition( null ).cell();
		assertNotEquals( "never on the stone", c, land );
		assertTrue( ow.adjacent( land, c ) );

		//every side but one walled or taken
		int free = c + 1 + W;
		for (int n : PathFinder.NEIGHBOURS8){
			int s = c + n;
			if (s == free) continue;
			if (n == -1){
				Rat rat = new Rat();
				rat.pos = s;
				ow.mobs.add( rat );
			} else {
				Homestead.build( ow, s, BuildCatalog.byId( "wall_stone" ), null );
			}
		}
		OverworldLevel.arriveAt( wx, wy );
		assertEquals( free, ow.getTransition( null ).cell() );
	}

	//a beacon home from far off: the window jumps mid-trip (no level current) and a frozen mob
	//it parks lets its Frost go, which looks at the level's water
	@Test
	public void aFarArrivalMidTripParksAFrozenMob(){
		OverworldLevel ow = openWindow();
		Rat rat = new Rat();
		rat.pos = openCell( ow );
		ow.mobs.add( rat );
		Buff.affect( rat, Frost.class, Frost.DURATION );
		Dungeon.level = null;

		OverworldLevel.arriveAt( ow.worldX + 5000, ow.worldY );
		ow.getTransition( null );
		assertNull( "the trip's level is still unset", Dungeon.level );
		assertFalse( ow.mobs.contains( rat ) );
	}

	@Test
	public void anAnimalOnARopeComesAlong(){
		int tx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), ty = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		OverworldLevel far = windowAt( tx + 400, ty - 80 );
		int hc = openCell( far );
		Homestead.build( far, hc, hearthstone(), null );
		HomesteadWorld.Home home = HomesteadWorld.homes().get( 0 );

		OverworldLevel ow = windowAt( tx, ty );
		Waystone town = atTheTownStone( ow );
		Rat goat = new Rat();
		goat.alignment = Char.Alignment.NEUTRAL;
		Rat loose = new Rat();
		loose.alignment = Char.Alignment.NEUTRAL;
		Rat tied = new Rat();
		tied.alignment = Char.Alignment.NEUTRAL;
		Rat mute = new Rat();
		mute.alignment = Char.Alignment.NEUTRAL;
		Rat[] beside = { goat, loose, tied, mute };
		int placed = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			boolean taken = c == town.pos;
			for (Mob m : ow.mobs) if (m.pos == c) taken = true;
			if (taken || !ow.passable[c] || placed == beside.length) continue;
			beside[placed++].pos = c;
		}
		assertEquals( "room beside the hero for all four", beside.length, placed );
		for (Rat r : beside) ow.mobs.add( r );
		Buff.affect( goat, Leashed.class ).mode = Leashed.Mode.LEASHED;
		Buff.affect( tied, Leashed.class ).mode = Leashed.Mode.TIED;
		Buff.affect( mute, Leashed.class ).penned = true;
		assertTrue( Travel.leashed( goat, hero ) );
		assertFalse( Travel.leashed( loose, hero ) );
		assertFalse( "tied to a post", Travel.leashed( tied, hero ) );
		assertFalse( "held by someone else", Travel.leashed( goat, new Hero() ) );
		assertFalse( "only penned, on no rope", Travel.leashed( mute, hero ) );

		Travel.Place to = Travel.Place.of( home );
		Travel.depart( hero, Travel.Place.of( town ), to );
		assertFalse( "the roped one leaves with him", ow.mobs.contains( goat ) );
		assertTrue( "the loose one stays", ow.mobs.contains( loose ) );
		assertTrue( "the tied one stays at its post", ow.mobs.contains( tied ) );
		assertTrue( ow.mobs.contains( mute ) );
		TravelArrival arrival = hero.buff( TravelArrival.class );
		assertTrue( arrival.carried().contains( goat ) );

		//he lands beside the hearthstone, and the rope's animal is put down by him
		Dungeon.level = far;
		hero.pos = far.getTransition( null ).cell();
		arrival.act();
		assertTrue( far.mobs.contains( goat ) );
		assertTrue( far.adjacent( goat.pos, hero.pos ) );
		assertNull( hero.buff( TravelArrival.class ) );
	}

	@Test
	public void heMayNotTravelFloatingRootedAwayFromTheStoneOrWithEnemiesInSight(){
		int tx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), ty = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		OverworldLevel ow = windowAt( tx, ty );
		Waystone town = atTheTownStone( ow );
		TravelWorld.get().attune( 1234L, tx + 300, ty, "Elsewhere" );
		Travel.Place from = Travel.Place.of( town );
		Travel.Place to = Travel.Place.of( TravelWorld.get().stone( 1234L ) );
		assertNull( Travel.check( hero, from, to ) );

		int beside = hero.pos;
		stand( ow, town.pos + 3 );
		assertEquals( Travel.Refusal.NOT_BESIDE, Travel.check( hero, from, to ) );
		stand( ow, beside );

		hero.rooted = true;
		assertEquals( Travel.Refusal.ROOTED, Travel.check( hero, from, to ) );
		hero.rooted = false;

		hero.flying = true;
		assertEquals( Travel.Refusal.LEVITATING, Travel.check( hero, from, to ) );
		hero.flying = false;

		Rat rat = new Rat();
		rat.alignment = Char.Alignment.ENEMY;
		rat.pos = town.pos + 4 * W;
		ow.mobs.add( rat );
		assertEquals( Travel.Refusal.ENEMIES, Travel.check( hero, from, to ) );
		hero.fieldOfView[rat.pos] = false;
		assertNull( "out of sight", Travel.check( hero, from, to ) );

		assertEquals( Travel.Refusal.GONE, Travel.check( hero, from, Travel.Place.of( new HomesteadWorld.Home( 97, 5, 5, "gone", 0 ) ) ) );

		Hero guest = new Hero();
		guest.pos = hero.pos;
		assertEquals( Travel.Refusal.GUEST, Travel.check( guest, from, to ) );

		Dungeon.depth = WorldLayers.depthOf( -1 );
		assertEquals( Travel.Refusal.NOT_HERE, Travel.check( hero, from, to ) );
		Dungeon.depth = OverworldLevel.DEPTH;

		for (Travel.Refusal r : Travel.Refusal.values()) assertFalse( r.message().contains( "NO TEXT" ) );
	}

	@Test
	public void aTripThatWouldStarveHimToDeathIsRefusedAndOnePastStarvingHurts(){
		//a hero whose wounds are only counted (a real one's reach the game's screen)
		hero = new Hero(){
			@Override
			public void damage( int dmg, Object src ){
				HP -= dmg;
			}
		};
		hero.lvl = 10;
		Dungeon.hero = hero;
		int tx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), ty = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		OverworldLevel ow = windowAt( tx, ty );
		Waystone town = atTheTownStone( ow );
		//a stone a full trip away: 900 cells, 450 hunger
		TravelWorld.get().attune( 4321L, town.wx + 900, town.wy, "Faraway" );
		Travel.Place from = Travel.Place.of( town );
		Travel.Place to = Travel.Place.of( TravelWorld.get().stone( 4321L ) );
		int cost = Travel.cost( from, to );
		assertEquals( Travel.MAX_HUNGER, cost );

		Hunger hunger = Buff.affect( hero, Hunger.class );
		hero.HT = 100;
		hero.HP = 30;
		hunger.setLevel( Hunger.STARVING );
		//all 450 past starving: 45 of his 100 health, more than the 30 he has
		assertEquals( 45, Travel.hurt( hero, cost ) );
		assertTrue( Travel.deadly( hero, cost ) );
		assertEquals( Travel.Refusal.TOO_HUNGRY, Travel.check( hero, from, to ) );
		assertFalse( Travel.Refusal.TOO_HUNGRY.message().contains( "NO TEXT" ) );
		InterlevelScene.Mode mode = InterlevelScene.mode;
		try {
			InterlevelScene.mode = null;
			assertFalse( Travel.go( hero, from, to ) );
			assertNull( "no scene is asked for", InterlevelScene.mode );
		} finally {
			InterlevelScene.mode = mode;
		}
		assertEquals( 30, hero.HP );
		assertEquals( (int)Hunger.STARVING, hunger.hunger() );
		assertNull( hero.buff( TravelArrival.class ) );
		assertFalse( OverworldLevel.hasPendingArrival() );

		//with health to spare he goes, and pays it: what the window warned
		hero.HP = 100;
		assertNull( Travel.check( hero, from, to ) );
		int hurt = Travel.hurt( hero, cost );
		assertEquals( cost, Travel.depart( hero, from, to ) );
		assertTrue( hero.isAlive() );
		assertTrue( "no more than the warning said", 100 - hero.HP <= hurt );
		assertTrue( 100 - hero.HP >= hurt - 1 );

		//short of starving it costs no health; crossing into it, the one point starving costs
		hunger.setLevel( 0 );
		assertEquals( 0, Travel.hurt( hero, 300 ) );
		hunger.setLevel( 400 );
		assertEquals( 1, Travel.hurt( hero, 50 ) );
	}

	@Test
	public void aWellFedHeroPaysOutOfHisMealFirstAndTheRestOutOfHisStomach(){
		OverworldLevel ow = openWindow();
		stand( ow, openCell( ow ) );
		Travel.Place from = new Travel.Place( Travel.Kind.WAYSTONE, WorldLayers.SURFACE_DEPTH, 0, 0, 1, "a" );
		Travel.Place to = new Travel.Place( Travel.Kind.WAYSTONE, WorldLayers.SURFACE_DEPTH, 400, 0, 2, "b" );
		int cost = Travel.cost( from, to );
		assertEquals( 200, cost );

		Hunger hunger = Buff.affect( hero, Hunger.class );
		hunger.setLevel( 250 );
		WellFed fed = Buff.affect( hero, WellFed.class );
		fed.reset();
		//a meal with plenty left takes it all: he arrives as fed as he left
		assertEquals( cost, Travel.fromMeal( hero, cost ) );
		assertEquals( 0, Travel.leaves( hero, cost ) );

		//a meal nearly gone takes what it has, and the rest is hunger: no free trip
		hunger.affectHunger( -(fed.turnsLeft() - 5) );
		assertEquals( 5, fed.turnsLeft() );
		assertEquals( 250, hunger.hunger() );
		assertEquals( 5, Travel.fromMeal( hero, cost ) );
		assertEquals( "250 + 195: hungry", 1, Travel.leaves( hero, cost ) );

		assertEquals( cost, Travel.depart( hero, from, to ) );
		assertNull( "the meal is eaten up", hero.buff( WellFed.class ) );
		assertEquals( 250 + 195, hunger.hunger() );
	}

	@Test
	public void aHomeOutlastsTheWindowMovingAndASaveAndATripOnItsOwnSliceLandsByIt() throws Exception {
		OverworldLevel ow = openWindow();
		int c = openCell( ow );
		Homestead.build( ow, c, hearthstone(), null );
		HomesteadWorld.Home home = HomesteadWorld.homes().get( 0 );
		Travel.Place there = Travel.Place.of( home );
		assertEquals( c, Travel.stoneOf( ow, there ).pos );

		//the window goes far off: the stone with it, parked
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, home.wx + 900, home.wy );
		assertEquals( -1, ow.localCell( home.wx, home.wy ) );
		assertNull( Travel.stoneOf( ow, there ) );
		stand( ow, W / 2 + (H / 2) * W );

		//a trip on the same slice from out there, with a save on the way
		Hunger hunger = Buff.affect( hero, Hunger.class );
		hunger.setLevel( 0 );
		Travel.Place from = new Travel.Place( Travel.Kind.WAYSTONE, WorldLayers.SURFACE_DEPTH, home.wx + 900, home.wy, 7, "out there" );
		assertEquals( 450, Travel.depart( hero, from, there ) );
		OverworldLevel back = saveAndLoad( ow );
		saveAndLoadWorld();
		home = HomesteadWorld.homeAt( WorldLayers.SURFACE_DEPTH, home.wx, home.wy );
		assertNotNull( "the home is kept with the run", home );

		//the slice read back takes the arrival (the old one still on show, as when the scene loads)
		Dungeon.level = ow;
		int land = back.getTransition( null ).cell();
		Dungeon.level = back;
		int stone = back.localCell( home.wx, home.wy );
		assertNotEquals( -1, stone );
		Mob found = Travel.stoneOf( back, Travel.Place.of( home ) );
		assertNotNull( "its hearthstone is back where it stood", found );
		assertEquals( stone, found.pos );
		assertEquals( home.wx, ((Hearthstone) found).wx );
		assertEquals( home.wy, ((Hearthstone) found).wy );
		assertTrue( back.adjacent( land, stone ) );
	}

	@Test
	public void anArrivalNeverLandsOverAWallFromTheStone(){
		OverworldLevel ow = openWindow();
		int c = openCell( ow );
		Waystone stone = new Waystone();
		stone.key = 11L;
		stone.pos = c;
		ow.mobs.add( stone );
		//the stone in a closed one-cell room: every side walled but one, and that one taken
		int door = c + 1;
		for (int n : PathFinder.NEIGHBOURS8){
			if (c + n == door) continue;
			Homestead.build( ow, c + n, BuildCatalog.byId( "wall_stone" ), null );
		}
		Rat rat = new Rat();
		rat.pos = door;
		ow.mobs.add( rat );
		int wx = ow.worldX + c % W, wy = ow.worldY + c / W;
		OverworldLevel.arriveAt( wx, wy );
		int land = ow.getTransition( null ).cell();
		assertNotEquals( "never on the stone", c, land );
		assertTrue( "out past the one open side", ow.distance( land, door ) == 1 );
		assertFalse( ow.solid[land] );
	}

	// ------------------------------------------------------------------ roads, the map, the words

	@Test
	public void roadsAndPathsAreATenthQuicker(){
		OverworldLevel ow = openWindow();
		int c = openCell( ow );
		hero.pos = c;
		assertEquals( 1f, Paths.speedFactor( hero ), 0f );
		for (int t : new int[]{ Terrain.PATH_STONE, Terrain.BRIDGE_PLANK, Terrain.DIRT_PATH, Terrain.BRIDGE }){
			ow.map[c] = t;
			assertEquals( 1.1f, Paths.speedFactor( hero ), 0.0001f );
		}
		hero.flying = true;
		assertEquals( "not over the road", 1f, Paths.speedFactor( hero ), 0f );
		hero.flying = false;
		Dungeon.level = new xyz.gabriwar.warpedpixeldungeon.levels.DeadEndLevel();
		Dungeon.level.map = new int[ow.length()];
		Dungeon.level.map[c] = Terrain.DIRT_PATH;
		assertEquals( "the dungeon's paths are no roads", 1f, Paths.speedFactor( hero ), 0f );
	}

	@Test
	public void theMapPinsHomesAndTheWaystonesKnown(){
		HomesteadWorld.addHome( new HomesteadWorld.Home( 97, 10, 10, "Upland", 0 ) );
		HomesteadWorld.addHome( new HomesteadWorld.Home( WorldLayers.depthOf( -1 ), 30, 30, "Burrow", 0 ) );
		TravelWorld.get().attune( 1L, 50, 50, "Bright" );
		TravelWorld.get().see( 2L, 60, 60, "Dim" );
		List<MapMarks.Mark> marks = MapMarks.all( 97 );
		assertEquals( 4, marks.size() );
		MapMarks.Mark here = marks.get( 0 ), there = marks.get( 1 ), bright = marks.get( 2 ), dim = marks.get( 3 );
		assertEquals( MapMarks.Icon.HOME, here.icon );
		assertEquals( "Upland", here.name );
		assertEquals( 1f, here.alpha, 0f );
		assertTrue( there.name.contains( "Burrow" ) && there.name.contains( WndTravel.slice( there.depth ) ) );
		assertTrue( there.alpha < 1f );
		assertEquals( MapMarks.Icon.WAYSTONE, bright.icon );
		assertEquals( 1f, bright.alpha, 0f );
		assertFalse( "the village names it", bright.labelled );
		assertEquals( MapMarks.DIM, dim.alpha, 0f );

		//from the caves the waystones, all of the surface, are faded and say where they are
		List<MapMarks.Mark> below = MapMarks.all( WorldLayers.depthOf( -1 ) );
		MapMarks.Mark brightBelow = below.get( 2 );
		assertEquals( MapMarks.OTHER_SLICE, brightBelow.alpha, 0.0001f );
		assertTrue( brightBelow.labelled );
		assertTrue( brightBelow.name.contains( "Bright" ) && brightBelow.name.contains( WndTravel.slice( 97 ) ) );
		assertEquals( MapMarks.DIM * MapMarks.OTHER_SLICE, below.get( 3 ).alpha, 0.0001f );

		//another part's marks (the threats: a home under threat)
		MapMarks.Provider threat = ( d, out ) -> out.add( new MapMarks.Mark( MapMarks.Icon.DOT, 97, 10, 10, 0xFF3333, "under threat", true, 1f, true ) );
		MapMarks.register( threat );
		try {
			assertEquals( 5, MapMarks.all( 97 ).size() );
			assertTrue( MapMarks.all( 97 ).get( 4 ).blink );
		} finally {
			MapMarks.unregister( threat );
		}
	}

	@Test
	public void aCoopGuestsMapPinsTheHostsHomesAndWaystones() throws Exception {
		HomesteadWorld.addHome( new HomesteadWorld.Home( 97, 10, 10, "Upland", 0 ) );
		TravelWorld.get().attune( 1L, 50, 50, "Bright" );
		TravelWorld.get().see( 2L, 60, 60, "Dim" );
		long sig = TravelShare.sig();
		assertEquals( "the same record, the same fingerprint", sig, TravelShare.sig() );
		String shipped = TravelShare.text();

		//every change a guest's map shows moves the fingerprint
		HomesteadWorld.homes().get( 0 ).name = "Renamed";
		assertNotEquals( sig, TravelShare.sig() );
		HomesteadWorld.homes().get( 0 ).name = "Upland";
		assertEquals( sig, TravelShare.sig() );
		TravelWorld.get().attune( 2L, 60, 60, "Dim" );
		assertNotEquals( sig, TravelShare.sig() );

		//a guest whose own run knows nothing takes the host's over
		HomesteadWorld.reset();
		Field mode = xyz.gabriwar.warpedpixeldungeon.net.NetManager.class.getDeclaredField( "mode" );
		mode.setAccessible( true );
		Object was = mode.get( null );
		mode.set( null, xyz.gabriwar.warpedpixeldungeon.net.NetManager.NetMode.PLAYER );
		try {
			TravelShare.adopt( shipped );
			List<MapMarks.Mark> marks = MapMarks.all( 97 );
			assertEquals( 3, marks.size() );
			assertEquals( "Upland", marks.get( 0 ).name );
			assertEquals( MapMarks.Icon.WAYSTONE, marks.get( 1 ).icon );
			assertEquals( 1f, marks.get( 1 ).alpha, 0f );
			assertEquals( MapMarks.DIM, marks.get( 2 ).alpha, 0f );
			assertTrue( "the attuned one's mirror glows", TravelShare.attunedAt( 50, 50 ) );
			assertFalse( TravelShare.attunedAt( 60, 60 ) );
			//an empty payload changes nothing
			TravelShare.adopt( "" );
			assertEquals( 3, MapMarks.all( 97 ).size() );
		} finally {
			TravelShare.adopt( "{\"hdepth\":[],\"hx\":[],\"hy\":[],\"hname\":[],\"skey\":[],\"sx\":[],\"sy\":[],\"sname\":[],\"satt\":[]}" );
			mode.set( null, was );
		}
		assertTrue( "the host reads his own run again", MapMarks.all( 97 ).isEmpty() );
	}

	@Test
	public void everyWordOfTheTravelPartIsWritten(){
		String[][] keys = {
				{ "Travel", "meals_1", "meals_2", "meals_3", "meals_4", "meals_5", "meals_6", "travelled" },
				{ "WndTravel", "title", "nowhere", "slice_surface", "slice_up", "slice_down", "row_home", "row_waystone",
						"row_cost", "warn_hungry", "warn_starving", "warn_hurt", "warn_deadly", "confirm_title", "confirm_cost",
						"go", "stay" },
				{ "Waystone", "name", "name_attuned", "desc", "desc_attuned", "attuned", "guest" },
				{ "Hearthstone", "name", "name_of", "desc", "guest", "founded", "lost" },
				{ "HomeNames", "surface_near", "cave_near", "peak_near", "short_near", "surface", "cave", "peak" },
				{ "Waystones", "town" }, { "WndHome", "body" }, { "TravelActions", "travel" }, { "MapMarks", "elsewhere" }
		};
		for (String[] group : keys){
			Class<?> cls;
			try {
				cls = Class.forName( "xyz.gabriwar.warpedpixeldungeon.homestead.travel." + group[0] );
			} catch (ClassNotFoundException e){
				throw new AssertionError( e );
			}
			for (int i = 1; i < group.length; i++){
				String text = Messages.get( cls, group[i] );
				assertFalse( group[0] + "." + group[i], text.contains( "NO TEXT" ) );
			}
		}
		assertFalse( Messages.get( WndTravel.class, "intro", 1, 300, 450 ).contains( "NO TEXT" ) );
		assertFalse( hearthstone().name().contains( "NO TEXT" ) );
		assertFalse( hearthstone().desc().contains( "NO TEXT" ) );
	}
}
