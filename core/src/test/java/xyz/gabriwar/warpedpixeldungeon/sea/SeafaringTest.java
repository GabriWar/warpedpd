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

package xyz.gabriwar.warpedpixeldungeon.sea;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shark;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Mounts;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Taming;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Forge;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishingRod;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishingWaters;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Raft;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Sailboat;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SeaSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SeaWorldTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The rules of the water (sea/Seafaring), on a real island of the surface: a boat launched from the
 * shore takes its sailor aboard, a step ashore moors it and a step onto it boards it again, it is
 * hauled back into the pack with its hull, it sails over the deep and lands nearest a target ashore,
 * its pace, the weather's waves wearing a raft until it capsizes into a swim, a swimmer tiring, a boat
 * afloat through a save, the stations' recipes and what a shark may bite.
 */
public class SeafaringTest {

	private int depth, branch, startDay, turn;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level savedLevel;
	private Hero savedHero;
	private Hero hero;

	//headless, a bare cell selector stands in for the scene's: an item's action cancels it, and a
	//hurt hero's interrupt resets its key hold (as WeaponPerkTest's)
	private static boolean standIn;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		if (sel.get( null ) == null){
			sel.set( null, bare( CellSelector.class ) );
			standIn = true;
		}
	}

	@AfterClass
	public static void unboot() throws Exception {
		if (!standIn) return;
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		sel.set( null, null );
	}

	@SuppressWarnings("unchecked")
	private static <T> T bare( Class<T> c ){
		try {
			Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
			u.setAccessible( true );
			Object unsafe = u.get( null );
			return (T) unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, c );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		startDay = Dungeon.calendarStartDay;
		turn = Dungeon.cycleTurn;
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 4244 );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.calendarStartDay = 0;
		Dungeon.cycleTurn = 100;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		HomesteadWorld.reset();
		calm();
		hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		ClimateManager.debugPrecipTypeOverride = null;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugWindDirOverride = Float.NaN;
		ClimateManager.debugForceStorm = false;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		HomesteadWorld.reset();
	}

	// ------------------------------------------------------------------ helpers

	//no rain, no wind, no storm
	private static void calm(){
		ClimateManager.debugPrecipTypeOverride = PrecipType.NONE;
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugWindOverride = 0f;
		ClimateManager.debugWindDirOverride = Float.NaN;
		ClimateManager.debugForceStorm = false;
	}

	//an island's window, the level of the game, every cell known
	private static OverworldLevel island(){
		SeaSites.Island isl = SeaWorldTest.island( i -> i.radius >= 6 );
		OverworldLevel ow = SeaWorldTest.windowAt( isl.cx, isl.cy );
		Arrays.fill( ow.visited, true );
		return ow;
	}

	private static boolean water( OverworldLevel ow, int c ){
		return ow.map[c] == Terrain.WATER || ow.map[c] == Terrain.DEEP_WATER;
	}

	//open ground with open water beside it, nearest the window's middle; -1 for none
	private static int shore( OverworldLevel ow ){
		int w = ow.width(), mid = w / 2 + (ow.height() / 2) * w;
		for (int r = 0; r < w / 2 - 4; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = mid + dx + dy * w;
					if (!ow.passable[c] || water( ow, c ) || ow.pit[c]) continue;
					for (int n : PathFinder.NEIGHBOURS8) if (water( ow, c + n )) return c;
				}
			}
		}
		return -1;
	}

	//the hero standing on a cell, seeing everything, on the actors' roll
	private void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
		if (!Actor.all().contains( hero )) Actor.add( hero );
	}

	//the hero aboard a boat of a kind on a water cell
	private Afloat aboardAt( OverworldLevel ow, int cell, Boat.Kind kind ){
		stand( ow, cell );
		Afloat a = Buff.affect( hero, Afloat.class );
		a.kind = kind;
		a.hull = kind.hull;
		a.wx = ow.worldX + cell % ow.width();
		a.wy = ow.worldY + cell / ow.width();
		a.aboard = true;
		return a;
	}

	//a deep water cell with deep water all round it, nearest the window's middle (well inside it: no
	//step from there slides the window)
	private static int openDeep( OverworldLevel ow ){
		int w = ow.width(), mid = w / 2 + (ow.height() / 2) * w;
		for (int r = 0; r < 40; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = mid + dx + dy * w;
					if (ow.map[c] != Terrain.DEEP_WATER) continue;
					boolean open = true;
					for (int n : PathFinder.NEIGHBOURS8) open &= ow.map[c + n] == Terrain.DEEP_WATER;
					if (open) return c;
				}
			}
		}
		return -1;
	}

	//a deep water cell with open ground a few cells off in a straight line: where a wreck's timbers
	//drift ashore
	private static int deepByTheShore( OverworldLevel ow ){
		for (int c = 0; c < ow.length(); c++){
			if (!ow.insideMap( c ) || ow.map[c] != Terrain.DEEP_WATER) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				for (int r = 2; r <= 4; r++){
					int at = c + n * r;
					if (at >= 0 && at < ow.length() && ow.insideMap( at ) && ow.passable[at] && !water( ow, at )) return c;
				}
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------ the tests

	@Test
	public void aBoatIsLaunchedFromTheShoreAndTakesHimAboard(){
		OverworldLevel ow = island();
		int shore = shore( ow );
		assertNotEquals( -1, shore );
		stand( ow, shore );
		Raft raft = new Raft();
		assertTrue( raft.collect( hero.belongings.backpack ) );
		assertNull( Seafaring.launchRefusal( hero ) );
		int onto = Seafaring.launchCell( hero );
		assertTrue( water( ow, onto ) );
		Seafaring.launch( hero, raft );
		assertEquals( onto, hero.pos );
		assertTrue( Seafaring.aboard( hero ) );
		assertNull( hero.belongings.getItem( Raft.class ) );
		assertEquals( Boat.Kind.RAFT, Seafaring.boat( hero ).kind );
		//aboard, a second boat is refused
		Sailboat sail = new Sailboat();
		sail.collect( hero.belongings.backpack );
		assertNotNull( Seafaring.launchRefusal( hero ) );
		//no boat goes off the surface's water: none from a dry inland cell
		hero.buff( Afloat.class ).detach();
		int dry = -1;
		for (int c = 0; c < ow.length() && dry == -1; c++){
			if (!ow.insideMap( c ) || !ow.passable[c] || water( ow, c )) continue;
			boolean inland = true;
			for (int n : PathFinder.NEIGHBOURS8) inland &= !water( ow, c + n );
			if (inland) dry = c;
		}
		stand( ow, dry );
		assertNotNull( Seafaring.launchRefusal( hero ) );
	}

	@Test
	public void aStepAshoreMoorsItAndAStepOntoItBoardsAgain(){
		OverworldLevel ow = island();
		int shore = shore( ow );
		stand( ow, shore );
		Raft raft = new Raft();
		raft.collect( hero.belongings.backpack );
		Seafaring.launch( hero, raft );
		int boat = hero.pos;
		hero.move( shore );
		assertEquals( shore, hero.pos );
		Afloat a = Seafaring.boat( hero );
		assertFalse( a.aboard );
		assertEquals( ow.worldX + boat % ow.width(), a.wx );
		assertEquals( ow.worldY + boat / ow.width(), a.wy );
		assertEquals( boat, Seafaring.boatCell( hero ) );
		assertEquals( 1, ow.sea().moorings().length );
		//a walk back to it: Hero.getCloser hands the step to the sea
		assertTrue( Seafaring.steers( hero, boat ) );
		assertEquals( boat, Seafaring.nextStep( hero, boat ) );
		hero.move( boat );
		assertTrue( Seafaring.aboard( hero ) );
		assertEquals( 0, ow.sea().moorings().length );
	}

	@Test
	public void aLaunchFromThePackHandsTheTurnOn(){
		//LAUNCH runs from the pack while the game waits on him (the actors' current one): no animation
		//of his ends it, so unless the launch hands the turn on nothing wakes the actors again
		OverworldLevel ow = island();
		stand( ow, shore( ow ) );
		Raft raft = new Raft();
		raft.collect( hero.belongings.backpack );
		current( hero );
		try {
			Seafaring.launch( hero, raft );
			assertTrue( Seafaring.aboard( hero ) );
			assertNull( "the turn handed on", Actor.current() );
		} finally {
			current( null );
		}
	}

	@Test
	public void aBoatOutOfReachIsLeftToTheSeaForAnother(){
		OverworldLevel ow = island();
		int shore = shore( ow );
		stand( ow, shore );
		//his sailboat afloat out on the open deep, where no walk reaches (an island left by a scroll)
		int deep = openDeep( ow );
		Afloat old = Buff.affect( hero, Afloat.class );
		old.kind = Boat.Kind.SAILBOAT;
		old.hull = old.max();
		old.wx = ow.worldX + deep % ow.width();
		old.wy = ow.worldY + deep / ow.width();
		assertFalse( Seafaring.withinReach( hero, ow, old ) );
		Raft raft = new Raft();
		raft.collect( hero.belongings.backpack );
		assertNull( Seafaring.launchRefusal( hero ) );
		Seafaring.launch( hero, raft );
		assertTrue( Seafaring.aboard( hero ) );
		assertSame( Boat.Kind.RAFT, Seafaring.boat( hero ).kind );
		//moored where he can walk to it, a boat keeps him from launching another until it is hauled out
		hero.move( shore );
		assertTrue( Seafaring.withinReach( hero, ow, Seafaring.boat( hero ) ) );
		assertEquals( 1, ow.sea().moorings().length );
		new Sailboat().collect( hero.belongings.backpack );
		assertNotNull( Seafaring.launchRefusal( hero ) );
	}

	//the actor the game waits on (Actor.current), as when the hero's input is awaited
	private static void current( Actor a ){
		try {
			Field f = Actor.class.getDeclaredField( "current" );
			f.setAccessible( true );
			f.set( null, a );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void itIsHauledOutWithItsHull(){
		OverworldLevel ow = island();
		int shore = shore( ow );
		stand( ow, shore );
		Raft raft = new Raft();
		raft.collect( hero.belongings.backpack );
		Seafaring.launch( hero, raft );
		hero.move( shore );
		Seafaring.boat( hero ).hull = 25;
		assertTrue( Seafaring.canHaul( hero ) );
		Seafaring.haul( hero );
		assertNull( Seafaring.boat( hero ) );
		Raft back = hero.belongings.getItem( Raft.class );
		assertNotNull( back );
		assertEquals( 25, back.hull );
		//worth less the more it needs mending, and planks mend it
		assertTrue( back.value() < new Raft().value() );
		new Plank().quantity( 5 ).collect( hero.belongings.backpack );
		back.execute( hero, Boat.AC_REPAIR );
		assertEquals( Boat.Kind.RAFT.hull, back.hull );
		Plank left = hero.belongings.getItem( Plank.class );
		assertEquals( 3, left == null ? 0 : left.quantity() );
	}

	@Test
	public void itSailsTheDeepAndLandsNearestATargetAshore(){
		OverworldLevel ow = island();
		int deep = openDeep( ow );
		assertNotEquals( -1, deep );
		aboardAt( ow, deep, Boat.Kind.RAFT );
		//a target out on the water: the first step is water beside him
		int w = ow.width();
		int far = -1;
		for (int c = 0; c < ow.length() && far == -1; c++){
			if (water( ow, c ) && ow.insideMap( c ) && ow.distance( deep, c ) >= 6 && ow.distance( deep, c ) <= 10) far = c;
		}
		assertNotEquals( -1, far );
		int step = Seafaring.nextStep( hero, far );
		assertNotEquals( -1, step );
		assertTrue( ow.adjacent( deep, step ) );
		assertTrue( water( ow, step ) );
		//a target ashore: he sails on, and the first step ashore is beside the water
		int land = shore( ow );
		int guard = 0;
		while (Seafaring.aboard( hero ) && guard++ < 400){
			int s = Seafaring.nextStep( hero, land );
			assertNotEquals( "a step toward the land", -1, s );
			assertTrue( ow.adjacent( hero.pos, s ) );
			hero.move( s );
		}
		assertFalse( "he landed", Seafaring.aboard( hero ) );
		assertFalse( water( ow, hero.pos ) );
		boolean byWater = false;
		for (int n : PathFinder.NEIGHBOURS8) byWater |= water( ow, hero.pos + n );
		assertTrue( byWater );
		assertEquals( 1, ow.sea().moorings().length );
	}

	@Test
	public void theBoatStaysUnderHimAsTheWindowSlides(){
		OverworldLevel ow = island();
		int w = ow.width(), y = ow.height() / 2;
		//a step west onto the window's last stretch: the window slides at once (OverworldLevel's urgent rebase)
		int start = -1;
		for (int x = 31; x >= 3 && start == -1; x--){
			int c = x + y * w;
			if (ow.map[c] == Terrain.DEEP_WATER && ow.map[c - 1] == Terrain.DEEP_WATER) start = c;
		}
		assertNotEquals( -1, start );
		Afloat a = aboardAt( ow, start, Boat.Kind.RAFT );
		int[] before = Seafaring.worldCell( start );
		int originBefore = ow.worldX;
		hero.move( start - 1 );
		assertNotEquals( "the window slid", originBefore, ow.worldX );
		assertTrue( "still aboard", Seafaring.aboard( hero ) );
		assertEquals( before[0] - 1, a.wx );
		assertEquals( before[1], a.wy );
		assertEquals( -1, a.headX );
		assertEquals( 0, a.headY );
	}

	@Test
	public void theBoatsPaces(){
		OverworldLevel ow = island();
		int deep = openDeep( ow );
		Afloat a = aboardAt( ow, deep, Boat.Kind.RAFT );
		assertEquals( 1f, Seafaring.speedFactor( hero ), 0.001f );
		a.kind = Boat.Kind.SAILBOAT;
		assertEquals( 1.5f, Seafaring.speedFactor( hero ), 0.001f );
		//heading east, the wind blowing east: twice a walker's
		a.headX = 1;
		a.headY = 0;
		ClimateManager.debugWindOverride = 8f;
		ClimateManager.debugWindDirOverride = 90f;
		assertEquals( Seafaring.SAIL_BEFORE_WIND, Seafaring.speedFactor( hero ), 0.001f );
		//into the wind: half again
		ClimateManager.debugWindDirOverride = 270f;
		assertEquals( 1.5f, Seafaring.speedFactor( hero ), 0.001f );
		//swimming: half
		a.detach();
		assertTrue( Seafaring.swimming( hero ) );
		assertEquals( Seafaring.SWIM_SPEED, Seafaring.speedFactor( hero ), 0.001f );
	}

	@Test
	public void calmWaterWearsNothingAndAStormTearsARaftApart(){
		OverworldLevel ow = island();
		int deep = deepByTheShore( ow );
		assertNotEquals( -1, deep );
		Afloat a = aboardAt( ow, deep, Boat.Kind.RAFT );
		for (int i = 0; i < 200; i++) Seafaring.boatTurn( hero, a );
		assertEquals( Boat.Kind.RAFT.hull, a.hull );

		ClimateManager.debugForceStorm = true;
		int told = 0, turns = 0;
		while (Seafaring.boat( hero ) != null && turns++ < 2000){
			Seafaring.boatTurn( hero, a );
			told = Math.max( told, a.told );
		}
		assertNull( "the raft went down", Seafaring.boat( hero ) );
		assertEquals( "warned at half and a quarter", 2, told );
		assertNotNull( "in the water", hero.buff( Swimming.class ) );
		//its timbers drifted off: a plank or two somewhere near
		int planks = 0;
		for (Heap h : ow.heaps.valueList()){
			for (Item i : h.items) if (i instanceof Plank) planks += i.quantity();
		}
		assertTrue( "planks " + planks, planks >= 1 && planks <= 2 );
	}

	@Test
	public void aSailboatRidesOutTheRain(){
		OverworldLevel ow = island();
		Afloat a = aboardAt( ow, openDeep( ow ), Boat.Kind.SAILBOAT );
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.5f;
		assertEquals( 1, Seafaring.roughness() );
		for (int i = 0; i < 500; i++) Seafaring.boatTurn( hero, a );
		assertEquals( Boat.Kind.SAILBOAT.hull, a.hull );
	}

	@Test
	public void aSwimmerTiresAfterHisGrace(){
		OverworldLevel ow = island();
		stand( ow, openDeep( ow ) );
		hero.HP = hero.HT = 100;
		Seafaring.startSwimming( hero );
		Swimming s = hero.buff( Swimming.class );
		assertNotNull( s );
		for (int i = 0; i < Swimming.GRACE; i++) s.act();
		assertEquals( 100, hero.HP );
		s.act();
		s.act();
		assertEquals( 100 - 100 / Swimming.SHARE, hero.HP );
		//out of the deep, it ends
		hero.pos = shore( ow );
		s.act();
		assertNull( hero.buff( Swimming.class ) );
	}

	@Test
	public void aBoatAfloatComesBackThroughASave(){
		Afloat a = new Afloat();
		a.kind = Boat.Kind.SAILBOAT;
		a.hull = 77;
		a.wx = -1234;
		a.wy = 5678;
		a.aboard = true;
		a.headX = -1;
		a.headY = 1;
		a.told = 1;
		Bundle b = new Bundle();
		a.storeInBundle( b );
		Afloat back = new Afloat();
		back.restoreFromBundle( b );
		assertSame( Boat.Kind.SAILBOAT, back.kind );
		assertEquals( 77, back.hull );
		assertEquals( -1234, back.wx );
		assertEquals( 5678, back.wy );
		assertTrue( back.aboard );
		assertEquals( -1, back.headX );
		assertEquals( 1, back.headY );
		assertEquals( 1, back.told );

		Raft r = new Raft();
		r.hull = 12;
		Bundle rb = new Bundle();
		r.storeInBundle( rb );
		Raft rback = new Raft();
		rback.restoreFromBundle( rb );
		assertEquals( 12, rback.hull );
		//a raft from before its hull was kept is whole
		Raft old = new Raft();
		old.restoreFromBundle( new Bundle() );
		assertEquals( Boat.Kind.RAFT.hull, old.hull );
	}

	@Test
	public void theStationsBuildBoats(){
		StationRecipe raft = StationRecipes.byId( "raft" );
		assertNotNull( raft );
		assertSame( Workbench.class, raft.station );
		assertTrue( raft.output.sample() instanceof Raft );
		StationRecipe sail = StationRecipes.byId( "sailboat" );
		assertNotNull( sail );
		assertSame( Forge.class, sail.station );
		assertTrue( sail.output.sample() instanceof Sailboat );
	}

	@Test
	public void aSharkBitesOnlyWhatIsInTheWater(){
		OverworldLevel ow = island();
		int deep = openDeep( ow );
		stand( ow, deep );
		assertTrue( Shark.prey( hero ) );
		Buff.affect( hero, LanternGlare.class, 10f );
		assertFalse( Shark.prey( hero ) );
		hero.buff( LanternGlare.class ).detach();
		hero.pos = shore( ow );
		assertFalse( Shark.prey( hero ) );
		aboardAt( ow, deep, Boat.Kind.RAFT );
		assertTrue( Shark.prey( hero ) );
	}

	// ------------------------------------------------------------------ the other parts at sea

	//open ground nobody stands on with open water beside it, beside the cell given; -1 for none
	private static int shoreBeside( OverworldLevel ow, int cell ){
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (!ow.passable[c] || water( ow, c ) || ow.pit[c] || Actor.findChar( c ) != null) continue;
			for (int m : PathFinder.NEIGHBOURS8) if (water( ow, c + m )) return c;
		}
		return -1;
	}

	@Test
	public void aRiderGetsDownAtTheWaterAndNoHorseGoesAboard(){
		OverworldLevel ow = island();
		//a shore cell with another beside it: the hero on the one, his saddled horse on the other
		int horseAt = -1, heroAt = -1;
		for (int c = 0; c < ow.length() && horseAt == -1; c++){
			if (!ow.insideMap( c ) || !ow.passable[c] || water( ow, c ) || ow.pit[c]) continue;
			boolean wet = false;
			for (int n : PathFinder.NEIGHBOURS8) wet |= water( ow, c + n );
			if (!wet) continue;
			int b = shoreBeside( ow, c );
			if (b != -1){ heroAt = c; horseAt = b; }
		}
		assertNotEquals( -1, horseAt );
		stand( ow, heroAt );
		Horse h = new Horse();
		h.tame = true;
		h.trust = Taming.TAME_TRUST;
		h.saddled = true;
		h.setLook();
		h.pos = horseAt;
		ow.mobs.add( h );
		Actor.add( h );
		Mounts.mount( hero, h );
		assertTrue( Mounts.rides( hero ) );
		assertEquals( horseAt, hero.pos );

		//his boat moored beside him: walking aboard it, he gets down and the horse waits ashore
		int moor = Seafaring.launchCell( hero );
		assertNotEquals( -1, moor );
		Afloat a = Buff.affect( hero, Afloat.class );
		a.kind = Boat.Kind.RAFT;
		a.hull = a.kind.hull;
		a.wx = ow.worldX + moor % ow.width();
		a.wy = ow.worldY + moor / ow.width();
		a.aboard = false;
		assertTrue( Mounts.getsDownBefore( hero, moor ) );
		int from = hero.pos;
		hero.move( moor );
		Mounts.stepped( hero, from, true );
		assertTrue( Seafaring.aboard( hero ) );
		assertFalse( Mounts.rides( hero ) );
		assertSame( h, Actor.findChar( from ) );
		//and from the boat no horse is mounted
		assertNotNull( Mounts.mountBlock( hero, h ) );

		//launched from the pack while riding: the same, the horse left on the shore he leaves
		a.detach();
		stand( ow, from );
		ow.mobs.remove( h );
		Actor.remove( h );
		int other = shoreBeside( ow, from );
		assertNotEquals( -1, other );
		h.pos = other;
		ow.mobs.add( h );
		Actor.add( h );
		Mounts.mount( hero, h );
		assertTrue( Mounts.rides( hero ) );
		int shore = hero.pos;
		Raft raft = new Raft();
		assertTrue( raft.collect( hero.belongings.backpack ) );
		assertNull( Seafaring.launchRefusal( hero ) );
		Seafaring.launch( hero, raft );
		assertTrue( Seafaring.aboard( hero ) );
		assertFalse( Mounts.rides( hero ) );
		assertSame( h, Actor.findChar( shore ) );
	}

	@Test
	public void aLineGoesOutFromABoat(){
		OverworldLevel ow = island();
		int deep = openDeep( ow );
		assertNotEquals( -1, deep );
		aboardAt( ow, deep, Boat.Kind.SAILBOAT );
		Arrays.fill( ow.heroFOV, true );
		//open water two cells off the boat takes a line, and it is the sea's
		int cell = deep + 2;
		assertTrue( water( ow, cell ) );
		assertNull( FishingRod.refusal( hero, cell ) );
		assertEquals( FishingWaters.Kind.SEA, FishingWaters.kindAt( ow, cell ) );
	}
}
