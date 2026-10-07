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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Breathless;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LostSoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShrinePiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaveMiner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Hermit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.EagleEgg;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Gem;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetSprites;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * The world's slices as one: the places of the caves and the heights, their ore, their creatures
 * and their dangers built apart and wired together. Each test is one meeting of two of them -
 * the trade of the ore, the beasts round the places, the hazards kept out of them, the rumours
 * told of them, the cells no two of them share.
 */
public class LayersAliveTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, depth, branch;
	private long dungeonSeed;
	private float temp, precip;
	private DayNightCycle.Phase override;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero, hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		dungeonSeed = Dungeon.seed;
		temp = ClimateManager.debugTempOverride;
		precip = ClimateManager.debugPrecipOverride;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.seed = SEED;
		ClimateManager.debugPrecipOverride = 0f;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		hero = new Hero();
		hero.lvl = 20;
		hero.sprite = new CharSprite(){
			@Override public void place( int cell ){}
			@Override public void showStatus( int color, String text, Object... args ){}
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){}
		};
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.seed = dungeonSeed;
		ClimateManager.debugTempOverride = temp;
		ClimateManager.debugPrecipOverride = precip;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------ helpers

	private static final EnumMap<CaveSites.Type, CaveSites.Site> CAVES = new EnumMap<>( CaveSites.Type.class );

	//one place of each kind of the caves near the origin (the debug scenes' own search)
	private static CaveSites.Site cave( CaveSites.Type t ){
		CaveSites.Site s = CAVES.get( t );
		if (s == null){
			int pref = t == CaveSites.Type.TOMB ? -9 : t == CaveSites.Type.RIFT ? -11 : -4;
			if (!t.at( pref )) pref = t.shallowest;
			s = DebugScenes.caveSite( SEED, t, pref );
			assertNotNull( "no " + t.key() + " near the origin", s );
			CAVES.put( t, s );
		}
		return s;
	}

	private static MountainSites.Site peak( MountainSites.Kind k ){
		MountainSites.Site s = MountainSites.nearest( SEED, k, 0, 0, 48 );
		assertNotNull( "no " + k.lower() + " near the origin", s );
		return s;
	}

	//the live window of a slice centred on a world cell, built as a mirror's is and made the host's own
	private OverworldLevel windowAt( int altitude, int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, 0f );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, 0f, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		Dungeon.depth = WorldLayers.depthOf( altitude );
		Dungeon.level = ow;
		hero.pos = -1;
		return ow;
	}

	//the hero on the nearest open cell to a world cell
	private void standNear( OverworldLevel ow, int wx, int wy ){
		for (int r = 0; r <= 12; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					int c = ow.localCell( wx + dx, wy + dy );
					if (c != -1 && ow.passable[c] && !ow.occupied( c )){
						hero.pos = c;
						return;
					}
				}
			}
		}
		throw new AssertionError( "nowhere to stand near " + wx + "," + wy );
	}

	private static void set( Object o, String name, Object v ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = o.getClass().getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static boolean wild( OverworldLevel ow, Mob m ){
		return OverworldFauna.isFauna( m ) && !(boolean) call( ow, "inSiteBerth", new Class<?>[]{ int.class }, m.pos );
	}

	// ------------------------------------------------------------ one economy

	//the contract's table, sink by sink: shops pay value(), the miner half again, the troll a core
	//a crucible and the filings, the rift's forge a core a smaller crucible and no filings
	@Test
	public void oneEconomyWithNoLoop() throws Exception {
		int[] price = { 5, 10, 20, 35, 100, 100 }, batch = { 100, 50, 30, 20, 10, 10 }, forge = { 100, 50, 30, 20, 7, 7 };
		for (Ores.Kind k : Ores.Kind.values()){
			Ore one = Reflection.newInstance( k.item );
			assertEquals( k.name(), price[k.ordinal()], one.value() );
			assertEquals( k.name(), batch[k.ordinal()], k.batch );
			assertEquals( k.name(), forge[k.ordinal()], k.forgeBatch() );
			Ore crucible = (Ore) Reflection.newInstance( k.item ).quantity( k.batch );
			assertEquals( "the miner pays half again for " + k.name(), Math.round( crucible.value() * CaveMiner.ORE_RATE ), CaveMiner.orePrice( crucible ) );
			assertTrue( CaveMiner.minersGoods( crucible ) );
			//what a crucible's ore fetches at the miner never buys back the core it would have made
			assertTrue( k.name(), CaveMiner.orePrice( crucible ) < 2 * new MasterworkCore().value() );
		}
		int[] gem = { 15, 20, 35, 35, 80 };
		for (Ores.GemKind g : Ores.GemKind.values()){
			Gem one = Reflection.newInstance( g.item );
			assertEquals( g.name(), gem[g.ordinal()], one.value() );
			assertEquals( Math.round( gem[g.ordinal()] * CaveMiner.ORE_RATE ), CaveMiner.orePrice( one ) );
		}
		//ten deepsilver: a core and 150 at the troll, 1500 at the miner, 1000 at a shop
		assertEquals( 1000, new MasterworkCore().value() );
		assertEquals( 150, Ores.Kind.DEEPSILVER.bonus );
		assertEquals( 1500, CaveMiner.orePrice( Reflection.newInstance( Ores.Kind.DEEPSILVER.item ).quantity( 10 ) ) );

		//nothing in the world sells ore or gems: not the loot tables, not the slices' keepers
		for (Generator.Category cat : Generator.Category.values()){
			if (cat.classes == null) continue;
			for (Class<?> c : cat.classes){
				assertFalse( cat + " holds " + c.getSimpleName(), Ore.class.isAssignableFrom( c ) || Gem.class.isAssignableFrom( c ) );
			}
		}
		for (Shopkeeper keeper : new Shopkeeper[]{ new CaveMiner(), Hermit.of( 1L, 0, 0, 3 ) }){
			Method stock = keeper.getClass().getDeclaredMethod( "stockSource" );
			stock.setAccessible( true );
			@SuppressWarnings("unchecked")
			ArrayList<Item> items = (ArrayList<Item>) stock.invoke( keeper );
			for (Item it : items){
				assertFalse( keeper.getClass().getSimpleName() + " sells " + it.getClass().getSimpleName(),
						it instanceof Ore || it instanceof Gem || it instanceof MasterworkCore );
			}
		}
		//and the smith's crucible takes no more than one batch, the forge's neither
		Ore lots = (Ore) Reflection.newInstance( Ores.Kind.IRON.item ).quantity( 150 );
		assertTrue( lots.collect( hero.belongings.backpack ) );
		Ore held = hero.belongings.getItem( Ores.Kind.IRON.item );
		assertTrue( Blacksmith2.smeltInto( hero, held ) );
		assertEquals( 100, held.quantity() );
		assertTrue( Blacksmith2.smeltInto( hero, held, true ) );
		assertEquals( 50, held.quantity() );
		assertEquals( 2, hero.belongings.getItem( MasterworkCore.class ).quantity() );
	}

	// ------------------------------------------------------------ the beasts round the places

	//no wildlife in any place's berth, on either side of the surface; and a place's guards (of
	//the wildlife's own kinds) never fill the slice's cap of twelve
	@Test
	public void wildlifeKeepsOutOfThePlacesAndGuardsLeaveRoomForIt(){
		Random.pushGenerator( 11 );
		try {
			//the heights: an eyrie and a watchtower
			for (MountainSites.Kind k : new MountainSites.Kind[]{ MountainSites.Kind.EYRIE, MountainSites.Kind.TOWER, MountainSites.Kind.HERMIT }){
				MountainSites.Site s = peak( k );
				OverworldLevel ow = windowAt( s.altitude, s.wx, s.wy );
				hero.pos = W / 2 + 30 + (H / 2) * W;
				ow.populateLayerSites();
				for (int i = 0; i < 6; i++) call( ow, "populateFauna", new Class<?>[0] );
				for (Mob m : ow.mobs){
					if (!OverworldFauna.isFauna( m ) || m instanceof Yeti && k == MountainSites.Kind.TOWER) continue;
					int wx = ow.worldX() + m.pos % W, wy = ow.worldY() + m.pos / W;
					for (MountainSites.Site t : ow.mountainSites()){
						assertFalse( m.getClass().getSimpleName() + " in the berth of a " + t.kind.lower(), MountainSites.inBerth( t, wx, wy ) );
					}
				}
			}

			//the caves: a crystal cavern full of its own guards
			CaveSites.Site c = cave( CaveSites.Type.CRYSTAL );
			OverworldLevel ow = windowAt( c.altitude, c.cx, c.cy );
			hero.pos = ow.localCell( c.standX, c.standY );
			ow.populateLayerSites();
			int posted = 0;
			for (int y = c.y0 - 4; y <= c.y1 + 4 && posted < 12; y++){
				for (int x = c.x0 - 4; x <= c.x1 + 4 && posted < 12; x++){
					int cell = ow.localCell( x, y );
					if (cell != -1 && ow.passable[cell] && !ow.occupied( cell ) && cell != hero.pos
							&& ow.addMob( new FossilSkeleton(), cell )) posted++;
				}
			}
			assertEquals( "twelve guards round the cavern", 12, posted );
			for (int i = 0; i < 4; i++) call( ow, "populateFauna", new Class<?>[0] );
			HashSet<Class<?>> guards = new HashSet<>();
			for (CaveSites.Guard g : CaveSites.Guard.values()) guards.add( g.cls );
			int wildlife = 0;
			for (Mob m : ow.mobs){
				if (wild( ow, m )) wildlife++;
				//every beast in a berth is a guard, posted here or by a place of the window
				else if (OverworldFauna.isFauna( m )) assertTrue( m.getClass().getSimpleName(), guards.contains( m.getClass() ) );
			}
			assertTrue( "the guards left the wildlife its twelve: " + wildlife, wildlife > 0 && wildlife <= 12 );
		} finally {
			Random.popGenerator();
		}
	}

	//the fire's creatures of the abyss come out on the rift's hot ground (pristine embers within
	//five), just outside its reach, and nowhere else is hot
	@Test
	public void theRiftIsTheAbyssHotGround(){
		CaveSites.Site rift = cave( CaveSites.Type.RIFT );
		assumeTrue( "the rift is in the abyss", rift.altitude <= -10 );
		OverworldLevel ow = windowAt( rift.altitude, rift.cx, rift.cy );
		hero.pos = ow.localCell( rift.standX, rift.standY );
		int hot = 0;
		for (int cell = 0; cell < ow.length(); cell++){
			if (!ow.passable[cell] || (OverworldFauna.habitat( ow, cell ) & OverworldFauna.Habitat.HEAT.bit) == 0) continue;
			int wx = ow.worldX() + cell % W, wy = ow.worldY() + cell / W;
			boolean nearFissure = false;
			for (long k : rift.fissure){
				int fx = (int)(k & 0xFFFFFFFFL), fy = (int)(k >> 32);
				nearFissure |= Math.max( Math.abs( fx - wx ), Math.abs( fy - wy ) ) <= 5;
			}
			boolean nearCamp = false;
			for (CaveSites.Site s : ow.caveSites()){
				if (s.type == CaveSites.Type.CAMP) nearCamp |= Math.max( Math.abs( s.cx - wx ), Math.abs( s.cy - wy ) ) <= 12;
			}
			assertTrue( "hot ground away from the rift at " + wx + "," + wy, nearFissure || nearCamp );
			if (!(boolean) call( ow, "inSiteBerth", new Class<?>[]{ int.class }, cell )) hot++;
		}
		assertTrue( "the rift's heat reaches past its berth", hot > 0 );

		Random.pushGenerator( 4 );
		try {
			ow.mobs.clear();
			Actor.clear();
			for (int round = 0; round < 30; round++) call( ow, "populateFauna", new Class<?>[0] );
		} finally {
			Random.popGenerator();
		}
		for (Mob m : ow.mobs){
			if (!(m instanceof Elemental.FireElemental) && !(m instanceof LostSoul)) continue;
			assertTrue( m.getClass().getSimpleName() + " off the hot ground",
					(OverworldFauna.habitat( ow, m.pos ) & OverworldFauna.Habitat.HEAT.bit) != 0 );
			assertFalse( (boolean) call( ow, "inSiteBerth", new Class<?>[]{ int.class }, m.pos ) );
		}
	}

	// ------------------------------------------------------------ the dangers kept out

	//no roof comes down on a place of the caves, nor on a tomb's stone
	@Test
	public void rockfallSparesThePlaces(){
		for (CaveSites.Type t : new CaveSites.Type[]{ CaveSites.Type.TOMB, CaveSites.Type.CAMP, CaveSites.Type.RIFT }){
			CaveSites.Site s = cave( t );
			OverworldLevel ow = windowAt( s.altitude, s.cx, s.cy );
			HazardWatch hz = ow.hazards();
			int open = 0;
			for (int cell = 0; cell < ow.length(); cell++){
				boolean falls = (boolean) call( hz, "canFall", new Class<?>[]{ int.class }, cell );
				if (ow.inSiteReach( cell ) || ow.unbreakable( cell )) assertFalse( t.key() + " cell " + cell, falls );
				else if (falls) open++;
			}
			assertTrue( "rock still falls elsewhere", open > 0 );
		}
	}

	//a hot spring's pool never freezes thin, nor ice on a place of the heights' ground; the same ice
	//outside them cracks
	@Test
	public void thinIceSparesSpringsAndPlaces(){
		ClimateManager.debugTempOverride = 0f;
		MountainSites.Site s = peak( MountainSites.Kind.SPRINGS );
		OverworldLevel ow = windowAt( s.altitude, s.wx, s.wy );
		int pool = ow.localCell( s.wx, s.wy );
		assertTrue( ow.hotSpring( pool ) );
		ow.map[pool] = Terrain.FROZEN_WATER;
		assertTrue( "it would read as thin", LayerHazards.thinIce( ow.map, W, pool, 0f ) );
		hero.pos = pool;
		ow.hazards().stepped( hero );
		assertFalse( "a spring's ice", ow.hazards().crackedAt( pool ) );

		//a frozen puddle laid on a place's own ground (inside its reach), and one well away
		int inside = -1, outside = -1;
		for (int cell = W * 2; cell < ow.length() - W * 2 && (inside == -1 || outside == -1); cell++){
			int x = cell % W;
			if (x < 2 || x > W - 3 || ow.hotSpring( cell ) || !ow.passable[cell]) continue;
			if (ow.inSiteReach( cell ) && inside == -1) inside = cell;
			else if (!ow.inSiteReach( cell ) && outside == -1 && ow.distance( cell, pool ) > 20) outside = cell;
		}
		assertNotEquals( -1, inside );
		assertNotEquals( -1, outside );
		for (int c : new int[]{ inside, outside }){
			ow.map[c] = Terrain.FROZEN_WATER;
			ow.map[c + 1] = ow.map[c - 1] = ow.map[c - W] = Terrain.WATER;
			assertTrue( LayerHazards.thinIce( ow.map, W, c, 0f ) );
			hero.pos = c;
			ow.hazards().stepped( hero );
		}
		assertFalse( "ice on a place's ground", ow.hazards().crackedAt( inside ) );
		assertTrue( "the same ice outside it", ow.hazards().crackedAt( outside ) );
	}

	//thin air eases under a roof as beside a fire: the hermit's hut and a waystation's shelter
	@Test
	public void aRoofEasesThinAir(){
		for (MountainSites.Kind k : new MountainSites.Kind[]{ MountainSites.Kind.HERMIT, MountainSites.Kind.PASS }){
			MountainSites.Site s = peak( k );
			OverworldLevel ow = windowAt( s.altitude, s.wx, s.wy );
			hero.pos = ow.localCell( s.wx, s.wy );
			assertTrue( ow.shelterAt( hero.pos ) );
			hero.resting = true;
			assertTrue( k.lower() + ": resting under its roof", Breathless.sheltered( hero ) );
			hero.resting = false;
			assertFalse( "standing is no rest", Breathless.sheltered( hero ) );
			//well outside, in the open, no fire near: no shelter
			hero.resting = true;
			hero.pos = ow.localCell( s.x1 + 6, s.y1 + 6 );
			assertFalse( ow.shelterAt( hero.pos ) );
			assertFalse( Breathless.sheltered( hero ) );
			hero.resting = false;
		}
	}

	// ------------------------------------------------------------ the places themselves

	//an eyrie's nest keeps its gold, now and then a stone from the shallow caves, and the eggs on top;
	//once ever
	@Test
	public void anEyriesNestHoldsItsHoard(){
		MountainSites.Site s = peak( MountainSites.Kind.EYRIE );
		OverworldLevel ow = windowAt( s.altitude, s.wx, s.wy );
		standNear( ow, s.wx - MountainSites.DX[s.dir] * 6, s.wy - MountainSites.DY[s.dir] * 6 );
		ow.populateLayerSites();
		Heap nest = ow.heaps.get( ow.localCell( s.wx, s.wy ) );
		assertNotNull( nest );
		assertTrue( "the eggs on top", nest.peek() instanceof EagleEgg );
		Gem gem = null;
		for (Item it : nest.items) if (it instanceof Gem) gem = (Gem) it;
		boolean stone = Math.floorMod( OverworldLevel.structHash( s.key, 3 ), 3L ) == 0;
		assertEquals( "a stone in one nest of three", stone, gem != null );
		if (gem != null){
			assertEquals( Ores.rollGem( -1, (int) Math.floorMod( OverworldLevel.structHash( s.key, 4 ), 100L ) ), gem.kind() );
		}
		int items = nest.items.size();
		ow.populateLayerSites();
		assertEquals( "once ever", items, ow.heaps.get( ow.localCell( s.wx, s.wy ) ).items.size() );
	}

	//something lairs in a watchtower at most once a week, kept by the periodic keys (never a
	//new once-key every week)
	@Test
	public void aTowersLairComesWeekly(){
		//the nearest tower something lairs in, looked for from a few points round the origin
		MountainSites.Site s = null;
		for (int[] from : new int[][]{ { 0, 0 }, { 1500, 0 }, { 0, 1500 }, { -1500, 0 }, { 0, -1500 } }){
			MountainSites.Site t = MountainSites.nearest( SEED, MountainSites.Kind.TOWER, from[0], from[1], 48 );
			if (t != null && Math.floorMod( OverworldLevel.structHash( t.key, 0x7011L ), 3L ) != 2){
				s = t;
				break;
			}
		}
		assertNotNull( "no tower with a lair round the origin", s );
		OverworldLevel ow = windowAt( s.altitude, s.wx, s.wy );
		standNear( ow, s.wx + MountainSites.DX[s.dir] * 6, s.wy + MountainSites.DY[s.dir] * 6 );
		ow.populateLayerSites();
		assertTrue( lairers( ow ) > 0 );
		//gone, the same week: nothing new
		ow.mobs.clear();
		Actor.clear();
		Dungeon.cycleTurn += 3 * DayNightCycle.FULL_CYCLE;
		ow.populateLayerSites();
		assertEquals( 0, lairers( ow ) );
		//a week on, back
		Dungeon.cycleTurn += 4 * DayNightCycle.FULL_CYCLE;
		ow.populateLayerSites();
		assertTrue( lairers( ow ) > 0 );
		//a mirror never peoples it
		ow.mobs.clear();
		Actor.clear();
		Dungeon.cycleTurn += 7 * DayNightCycle.FULL_CYCLE;
		set( ow, "network", true );
		ow.populateMountainSites();
		assertEquals( 0, lairers( ow ) );
	}

	private static int lairers( OverworldLevel ow ){
		int n = 0;
		for (Mob m : ow.mobs) if (m instanceof Yeti || m instanceof OverworldBandit) n++;
		return n;
	}

	//every rumour a hermit tells is one of the world's own: a place of the heights, a place of the
	//caves under his mountain, the richest rock near his home - each where it is said to be
	@Test
	public void aHermitsRumoursAreTrue(){
		MountainSites.Site home = peak( MountainSites.Kind.HERMIT );
		int a = home.altitude, hx = home.wx, hy = home.wy;
		HashSet<String> told = new HashSet<>();
		for (MountainSites.Site t : MountainSites.rumourCandidates( SEED, a, hx, hy, home.key )){
			told.add( MountainSites.rumourOf( SEED, t, a, hx, hy ) );
		}
		HashSet<String> caves = new HashSet<>();
		for (CaveSites.Site c : MountainSites.caveCandidates( SEED, hx, hy )){
			assertEquals( -1, c.altitude );
			assertEquals( "the world's own place", c, CaveSites.site( SEED, -1, c.sx, c.sy ) );
			assertTrue( Math.abs( c.sx - Math.floorDiv( hx, CaveSites.SECTOR ) ) <= 1 && Math.abs( c.sy - Math.floorDiv( hy, CaveSites.SECTOR ) ) <= 1 );
			int dx = c.cx - hx, dy = c.cy - hy;
			caves.add( Messages.get( MountainSites.class, "rumour_cave", Messages.get( MountainSites.class, "dir_" + MountainSites.compass( dx, dy ) ),
					MountainSites.paces( dx, dy ), CaveSites.phrase( c.type ) ) );
		}
		String vein = null;
		int[] v = Ores.notableVein( SEED, a, hx, hy, MountainSites.VEIN_REACH );
		if (v != null){
			Ores.Kind k = Ores.Kind.values()[v[2]];
			assertEquals( "the rock holds it", k, Ores.oreAt( SEED, a, v[0], v[1] ) );
			assertTrue( Ores.naturalRock( SEED, a, v[0], v[1] ) );
			int dx = v[0] - hx, dy = v[1] - hy;
			vein = Messages.get( MountainSites.class, "rumour_vein", Messages.get( MountainSites.class, "dir_" + MountainSites.compass( dx, dy ) ),
					MountainSites.paces( dx, dy ), Messages.get( k.item, "name" ) );
		}
		HashSet<String> heard = new HashSet<>();
		for (int day = 0; day < 400; day++){
			String r = MountainSites.rumour( SEED, a, hx, hy, home.key, day );
			assertFalse( r, r.contains( Messages.NO_TEXT_FOUND ) );
			assertEquals( "the same all day", r, MountainSites.rumour( SEED, a, hx, hy, home.key, day ) );
			assertTrue( "a rumour of nothing real: " + r, told.contains( r ) || caves.contains( r ) || r.equals( vein ) );
			heard.add( r );
		}
		if (!caves.isEmpty()) assertFalse( "the caves are told of", java.util.Collections.disjoint( heard, caves ) );
		if (vein != null) assertTrue( "the rock is told of", heard.contains( vein ) );
		System.out.println( "[layers] hermit at +" + a + ": " + told.size() + " heights, " + caves.size() + " caves, vein " + (vein != null) );
	}

	//no two features on one cell: no ore in a place's masonry, no way inside a place of the
	//heights, no two places' reaches overlapping
	@Test
	public void noTwoFeaturesShareACell(){
		for (CaveSites.Type t : CaveSites.Type.values()){
			CaveSites.Site s = cave( t );
			int ox = s.cx - W / 2, oy = s.cy - H / 2;
			WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, ox, oy, 0f );
			ArrayList<CaveSites.Site> laid = new ArrayList<>();
			for (Object o : w.sites) laid.add( (CaveSites.Site) o );
			assertTrue( laid.contains( s ) );
			for (CaveSites.Site p : laid){
				for (int i = 0; i < p.cells.length; i++){
					int x = (int)(p.cells[i] & 0xFFFFFFFFL) - ox, y = (int)(p.cells[i] >> 32) - oy;
					if (x < 1 || y < 1 || x > W - 2 || y > H - 2) continue;
					int c = x + y * W;
					if (p.terrain[i] == Terrain.WALL) assertEquals( t.key() + " masonry holds ore", 0, w.veins[c] );
					if (w.firedamp != null) assertFalse( t.key() + " gas on a place", w.firedamp[c] );
				}
				for (CaveSites.Site q : laid){
					if (q == p) continue;
					assertFalse( p.type.key() + " meets " + q.type.key(), p.x1 >= q.x0 && p.x0 <= q.x1 && p.y1 >= q.y0 && p.y0 <= q.y1 );
				}
			}
		}
		for (MountainSites.Kind k : MountainSites.Kind.values()){
			MountainSites.Site s = peak( k );
			int ox = s.wx - W / 2, oy = s.wy - H / 2;
			WindowGenerator.Window w = WindowGenerator.generate( SEED, s.altitude, ox, oy, 0f );
			List<MountainSites.Site> laid = MountainSitesVerifyTest.peaks( w );
			assertTrue( laid.contains( s ) );
			for (MountainSites.Site p : laid){
				for (int wy = p.y0; wy <= p.y1; wy++){
					for (int wx = p.x0; wx <= p.x1; wx++){
						int x = wx - ox, y = wy - oy;
						if (x < 1 || y < 1 || x > W - 2 || y > H - 2) continue;
						int c = x + y * W;
						assertEquals( p.kind.lower() + " over a way", WindowGenerator.LINK_NONE, w.link[c] );
						if (MountainSites.solidAt( SEED, p, wx, wy ) != -1) assertEquals( p.kind.lower() + " masonry holds ore", 0, w.veins[c] );
					}
				}
				for (MountainSites.Site q : laid){
					if (q == p) continue;
					assertFalse( p.kind.lower() + " meets " + q.kind.lower(), p.x1 >= q.x0 && p.x0 <= q.x1 && p.y1 >= q.y0 && p.y0 <= q.y1 );
				}
			}
		}
	}

	//the soaring eagle is a picture of the sky: never over a nest whose real eagles it could pass for
	@Test
	public void noPictureOfAnEagleOverAnEyrie(){
		MountainSites.Site s = peak( MountainSites.Kind.EYRIE );
		List<MountainSites.Site> one = java.util.Collections.singletonList( s );
		assertTrue( PeakLife.byEyrie( one, s.wx, s.wy ) );
		assertTrue( PeakLife.byEyrie( one, s.wx + PeakLife.EYRIE_CLEAR, s.wy - PeakLife.EYRIE_CLEAR ) );
		assertFalse( PeakLife.byEyrie( one, s.wx + PeakLife.EYRIE_CLEAR + 1, s.wy ) );
		assertFalse( "only an eyrie's", PeakLife.byEyrie( java.util.Collections.singletonList( peak( MountainSites.Kind.HERMIT ) ),
				peak( MountainSites.Kind.HERMIT ).wx, peak( MountainSites.Kind.HERMIT ).wy ) );
		//the level's own list is the window's: the eyrie is in it
		OverworldLevel ow = windowAt( s.altitude, s.wx, s.wy );
		assertTrue( PeakLife.byEyrie( ow.mountainSites(), s.wx, s.wy ) );
		assertTrue( ow.layerSites().contains( s ) );
	}

	//a shrine's fish keeps the strength of its slice over a save, not the hundred-odd depth number
	@Test
	public void aShrinesFishKeepsItsSliceOverASave(){
		Dungeon.depth = WorldLayers.depthOf( -6 );
		ShrinePiranha fish = new ShrinePiranha();
		int evasion = fish.defenseSkill;
		assertEquals( 10 + 2 * 6, evasion );
		Bundle b = new Bundle();
		fish.storeInBundle( b );
		ShrinePiranha back = new ShrinePiranha();
		back.restoreFromBundle( b );
		assertEquals( evasion, back.defenseSkill );
	}

	//a shrine's fish is a fish to the slices: coming back into the window it never lands on dry ground
	@Test
	public void aShrinesFishNeverComesBackOntoLand(){
		assertEquals( OverworldFauna.Habitat.POOL, OverworldFauna.home( ShrinePiranha.class ) );
		OverworldLevel ow = windowAt( -6, 0, 0 );
		ShrinePiranha fish = new ShrinePiranha();
		int dry = -1, wet = -1;
		for (int c = 0; c < ow.length() && (dry == -1 || wet == -1); c++){
			if (ow.passable[c] && !ow.water[c] && dry == -1) dry = c;
			if (ow.water[c] && wet == -1) wet = c;
		}
		assertTrue( dry != -1 );
		assertFalse( OverworldFauna.standsAt( ow, fish, dry ) );
		if (wet != -1) assertTrue( OverworldFauna.standsAt( ow, fish, wet ) );
	}

	// ------------------------------------------------------------ the guests' view

	//everyone the heights' places set down reaches a co-op guest: his sprite goes over the wire
	//under the name the caves' places taught it (NetSprites.wireName) and comes back the same
	@Test
	public void thePeaksFolkReachAGuest() throws Exception {
		ArrayList<Mob> made = new ArrayList<>();
		HashSet<MountainSites.Kind> peopled = new HashSet<>();
		for (MountainSites.Kind k : MountainSites.Kind.values()){
			MountainSites.Site s = MountainSites.nearest( SEED, k, 0, 0, 48 );
			if (s == null) continue;
			OverworldLevel ow = windowAt( s.altitude, s.wx, s.wy );
			ow.populateLayerSites();
			if (!ow.mobs.isEmpty()) peopled.add( k );
			made.addAll( ow.mobs );
		}
		for (MountainSites.Kind k : new MountainSites.Kind[]{ MountainSites.Kind.HERMIT, MountainSites.Kind.EYRIE,
				MountainSites.Kind.CAIRN, MountainSites.Kind.PASS }){
			assertTrue( "nobody at the " + k.lower(), peopled.contains( k ) );
		}
		//the tower's lair, whichever lot it is this week
		made.add( new Yeti() );
		made.add( new OverworldBandit() );
		for (Mob mob : made){
			Class<?> back = Class.forName( NetSprites.PACKAGE + NetSprites.wireName( mob.spriteClass ) );
			assertEquals( mob.getClass().getSimpleName(), mob.spriteClass, back );
			assertTrue( CharSprite.class.isAssignableFrom( back ) );
			assertNotNull( back.getConstructor() );
		}
	}

	//a wisp's gem is the world's caves' own: none from a wisp on the surface or the heights
	@Test
	public void wispsLeaveNoGemOutsideTheCaves(){
		for (int alt : new int[]{ 0, 2, 9 }){
			MountainSites.Site any = peak( MountainSites.Kind.HERMIT );
			OverworldLevel ow = windowAt( alt, any.wx, any.wy );
			int before = ow.heaps.size;
			CrystalWisp wisp = new CrystalWisp();
			wisp.pos = W / 2 + (H / 2) * W;
			for (int i = 0; i < 60; i++) call( wisp, "dropExtraLoot", new Class<?>[0] );
			assertEquals( "no gem from a wisp at altitude " + alt, before, ow.heaps.size );
			assertEquals( "and none counted against the day", 0.12f, ow.oreDropChance( 0.12f ), 1e-6f );
		}
	}
}
