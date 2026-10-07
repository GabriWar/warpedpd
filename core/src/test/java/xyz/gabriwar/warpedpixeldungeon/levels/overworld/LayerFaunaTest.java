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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bananaspider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ColdSpirit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSpinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyOni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.KoboldIcemancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LostSoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Minotaur;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.WildCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna.Habitat;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetSprites;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * The slices' creatures (OverworldFauna's bands): every band of the caves and the mountains has
 * its own beasts and none of the other side's; a row rolls only on its own ground (a fish on a
 * deep pool's shelf, a wisp by its seam, a goat by a drop, the icy ones on the snow); the windows
 * the game builds are peopled that way and keep the hero's ground clear; the depth-scaled beasts
 * are tuned to their band, not to the slices' depth numbers, and the danger rises the further
 * the slice; the hour cull spares the caves; and the drops of ore the caves' beasts give run dry
 * day by day.
 */
public class LayerFaunaTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	private static final int[] CAVE_BANDS = { -1, -4, -7, -10 }, PEAK_BANDS = { 1, 4, 8 };

	private int turn, depth, branch, challenges;
	private float precip;
	private DayNightCycle.Phase override;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		challenges = Dungeon.challenges;
		precip = ClimateManager.debugPrecipOverride;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		DayNightCycle.debugPhaseOverride = null;
		ClimateManager.debugPrecipOverride = 0f;
		hero = new Hero();
		hero.lvl = 30;
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		ClimateManager.debugPrecipOverride = precip;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void update(){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){ }
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

	private static Object invoke( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//the window of a slice round a world cell, built as a mirror's is and made the host's own
	private OverworldLevel windowAt( int altitude, int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.depth = WorldLayers.depthOf( altitude );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	//a world cell of the mountain slice's own ground, the 5x5 round it too, near the origin
	private static int[] peakGround( int altitude ){
		for (int r = 0; r <= 3200; r += 32){
			for (int dy = -r; dy <= r; dy += 32){
				for (int dx = -r; dx <= r; dx += 32){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (WorldLayers.band( WorldModel.elevation( SEED, dx, dy ) ) != altitude) continue;
					boolean all = true;
					for (int y = -2; y <= 2 && all; y++){
						for (int x = -2; x <= 2 && all; x++){
							all = WorldLayers.band( WorldModel.elevation( SEED, dx + x, dy + y ) ) == altitude;
						}
					}
					if (all) return new int[]{ dx, dy };
				}
			}
		}
		return null;
	}

	private static int allBits(){
		int m = 0;
		for (Habitat h : Habitat.values()) m |= h.bit;
		return m;
	}

	private static Mob tuned( Class<? extends Mob> cls, int altitude ){
		Mob m = com.watabou.utils.Reflection.newInstance( cls );
		OverworldFauna.prepare( m, altitude );
		return m;
	}

	//the hardest blow seen in many rolls
	private static int maxHit( Mob m ){
		int best = 0;
		for (int i = 0; i < 5000; i++) best = Math.max( best, m.damageRoll() );
		return best;
	}

	// ------------------------------------------------------------------ the tables

	@Test
	public void everySliceHasItsBand(){
		assertEquals( -1, OverworldFauna.statDepth( 0 ) );
		assertTrue( OverworldFauna.kindsAt( 0 ).isEmpty() );
		for (int a = -WorldLayers.MAX_BELOW; a <= WorldLayers.MAX_ABOVE; a++){
			if (a == 0) continue;
			assertTrue( "slice " + a + " is tuned", OverworldFauna.statDepth( a ) > 0 );
			assertFalse( "slice " + a + " has beasts", OverworldFauna.kindsAt( a ).isEmpty() );
		}
		//the deeper and the higher, the stronger; neighbouring bands differ
		for (int[] bands : new int[][]{ CAVE_BANDS, PEAK_BANDS }){
			for (int i = 1; i < bands.length; i++){
				assertTrue( OverworldFauna.statDepth( bands[i] ) > OverworldFauna.statDepth( bands[i - 1] ) );
				assertNotEquals( OverworldFauna.kindsAt( bands[i] ), OverworldFauna.kindsAt( bands[i - 1] ) );
			}
		}
		//the shared tuning table (contracts: 14/19/24/30 below, 14/20/28 above)
		assertEquals( 14, OverworldFauna.statDepth( -3 ) );
		assertEquals( 19, OverworldFauna.statDepth( -6 ) );
		assertEquals( 24, OverworldFauna.statDepth( -9 ) );
		assertEquals( 30, OverworldFauna.statDepth( -12 ) );
		assertEquals( 14, OverworldFauna.statDepth( 3 ) );
		assertEquals( 20, OverworldFauna.statDepth( 7 ) );
		assertEquals( 28, OverworldFauna.statDepth( 10 ) );
	}

	@Test
	public void cavesKeepToTheDarkPeaksToTheSky() throws Exception {
		List<Class<?>> surface = Arrays.asList( Deer.class, Bunny.class, Goat.class, GrayWolf.class, BrownWolf.class,
				Crab.class, WildCrab.class, Scorpio.class, Bananaspider.class, Yeti.class );
		List<Class<?>> under = Arrays.asList( Golem.class, AlbinoPiranha.class, CrystalWisp.class, FungalSpinner.class,
				GreyOni.class, Minotaur.class, Wraith.class, Brute.class, Bat.class, LostSoul.class, Elemental.FireElemental.class );
		for (int a = -WorldLayers.MAX_BELOW; a <= WorldLayers.MAX_ABOVE; a++){
			if (a == 0) continue;
			for (Class<? extends Mob> cls : OverworldFauna.kindsAt( a )){
				if (a < 0) assertFalse( cls.getSimpleName() + " in the caves at " + a, surface.contains( cls ) );
				else assertFalse( cls.getSimpleName() + " on the mountains at " + a, under.contains( cls ) );
				Mob m = cls.newInstance();
				assertFalse( cls.getSimpleName() + " is a boss", Char.hasProp( m, Char.Property.BOSS ) || Char.hasProp( m, Char.Property.MINIBOSS ) );
				//a co-op guest rebuilds the sprite from its class: one of the sprites package, made bare
				assertNotNull( cls.getSimpleName(), m.spriteClass );
				assertTrue( m.spriteClass.getName().startsWith( "xyz.gabriwar.warpedpixeldungeon.sprites." ) );
				assertTrue( Modifier.isPublic( m.spriteClass.getConstructor().getModifiers() ) );
				assertTrue( cls.getSimpleName() + " counts as fauna", OverworldFauna.isFauna( m ) );
				//...under the name the host sends (NetSprites.wireName: a nested sprite, a wisp's
				//colour or a shaman's, keeps its outer class), for every look the kind is born with
				for (int i = 0; i < 6; i++){
					Class<?> sprite = cls.newInstance().spriteClass;
					assertEquals( cls.getSimpleName() + " at " + a, sprite,
							Class.forName( NetSprites.PACKAGE + NetSprites.wireName( sprite ) ) );
				}
			}
		}
	}

	@Test
	public void rollsNeverLeaveTheirBand(){
		Random.pushGenerator( 7 );
		try {
			for (int a : new int[]{ -2, -5, -8, -11, 2, 5, 9 }){
				HashSet<Class<?>> seen = new HashSet<>();
				for (DayNightCycle.Phase p : DayNightCycle.Phase.values()){
					for (int i = 0; i < 1500; i++){
						for (Mob m : OverworldFauna.roll( a, WorldModel.Biome.MEADOW, p, allBits() )){
							assertTrue( m.getClass().getSimpleName() + " at " + a, OverworldFauna.kindsAt( a ).contains( m.getClass() ) );
							assertEquals( OverworldFauna.statDepth( a ), m.statDepth() );
							seen.add( m.getClass() );
						}
					}
				}
				assertEquals( "every kind of " + a + " turns up", new HashSet<>( OverworldFauna.kindsAt( a ) ), seen );
			}
		} finally {
			Random.popGenerator();
		}
	}

	// ------------------------------------------------------------------ the ground

	//a 24x24 cave floor in a ring of rock, one feature set at (x, y)
	private static int[] floor( int x, int y, int t ){
		int[] map = new int[24 * 24];
		for (int c = 0; c < map.length; c++){
			int cx = c % 24, cy = c / 24;
			map[c] = cx == 0 || cy == 0 || cx == 23 || cy == 23 ? Terrain.WALL : Terrain.EMPTY;
		}
		if (x >= 0) map[x + y * 24] = t;
		return map;
	}

	private static int hab( int[] map, int[] world, int x, int y, boolean frozen, int altitude ){
		return OverworldFauna.habitat( map, world, 24, 24, x + y * 24, frozen, altitude );
	}

	@Test
	public void habitatsGateTheRows(){
		int dry = Habitat.DRY.bit;
		int[] pool = floor( 11, 10, Terrain.DEEP_WATER );
		pool[10 + 10 * 24] = Terrain.WATER;
		pool[5 + 5 * 24] = Terrain.WATER;
		assertEquals( "the shelf of a deep pool", Habitat.POOL.bit, hab( pool, null, 10, 10, false, -5 ) );
		assertEquals( "a puddle with no deep water by it", 0, hab( pool, null, 5, 5, false, -5 ) );
		assertEquals( "the surface's water is no one's", 0, hab( pool, null, 10, 10, false, 0 ) );
		assertEquals( "rock", 0, hab( pool, null, 0, 0, false, -5 ) );

		int[] seam = floor( 11, 10, Terrain.MINE_CRYSTAL );
		assertEquals( dry | Habitat.CRYSTAL.bit, hab( seam, null, 10, 10, false, -5 ) );
		assertEquals( dry, hab( seam, null, 9, 10, false, -5 ) );

		int[] grove = floor( 12, 10, Terrain.MUSHROOM_PATCH );
		assertEquals( dry | Habitat.FUNGUS.bit, hab( grove, null, 10, 10, false, -8 ) );
		assertEquals( dry, hab( grove, null, 9, 10, false, -8 ) );

		int[] hot = floor( -1, -1, 0 );
		int[] world = floor( 15, 10, Terrain.EMBERS );
		assertEquals( "embers the world laid", dry | Habitat.HEAT.bit, hab( hot, world, 10, 10, false, -11 ) );
		assertEquals( dry, hab( hot, world, 9, 10, false, -11 ) );
		assertEquals( "embers the hero burned draw nothing", dry, hab( world, hot, 10, 10, false, -11 ) );

		int[] lip = floor( 12, 10, Terrain.CHASM );
		assertEquals( dry | Habitat.CLIFF.bit, hab( lip, null, 10, 10, false, 2 ) );
		assertEquals( dry, hab( lip, null, 9, 10, false, 2 ) );

		assertEquals( dry | Habitat.FROZEN.bit, hab( floor( -1, -1, 0 ), null, 10, 10, true, 6 ) );
		assertEquals( "the surface's land is dry land", dry, hab( seam, null, 10, 10, true, 0 ) );

		Random.pushGenerator( 11 );
		try {
			assertOnly( -5, DayNightCycle.Phase.DAY, Habitat.POOL.bit, AlbinoPiranha.class );
			assertNever( -5, dry, AlbinoPiranha.class, CrystalWisp.class );
			assertSome( -5, dry | Habitat.CRYSTAL.bit, CrystalWisp.class );
			assertNever( -8, dry, FungalSpinner.class );
			assertSome( -8, dry | Habitat.FUNGUS.bit, FungalSpinner.class );
			assertNever( -11, dry, Elemental.FireElemental.class, LostSoul.class );
			assertSome( -11, dry | Habitat.HEAT.bit, Elemental.FireElemental.class, LostSoul.class );
			assertNever( 2, dry, Goat.class );
			assertSome( 2, dry | Habitat.CLIFF.bit, Goat.class );
			assertNever( 6, dry, ColdSpirit.class, KoboldIcemancer.class );
			assertSome( 6, dry | Habitat.FROZEN.bit, ColdSpirit.class, KoboldIcemancer.class );
			assertNever( 9, dry, Elemental.FrostElemental.class, ColdSpirit.class, IceDemon.class );
			assertSome( 9, dry | Habitat.FROZEN.bit, Elemental.FrostElemental.class, ColdSpirit.class, IceDemon.class );
			//the high pastures' day beasts are gone by night
			for (int i = 0; i < 800; i++){
				for (Mob m : OverworldFauna.roll( 2, WorldModel.Biome.MEADOW, DayNightCycle.Phase.NIGHT, allBits() )){
					assertFalse( m instanceof Goat || m instanceof Bunny );
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	private static ArrayList<Class<?>> rolled( int a, DayNightCycle.Phase p, int hab ){
		ArrayList<Class<?>> out = new ArrayList<>();
		for (int i = 0; i < 800; i++){
			for (Mob m : OverworldFauna.roll( a, WorldModel.Biome.MEADOW, p, hab )) out.add( m.getClass() );
		}
		return out;
	}

	private static void assertOnly( int a, DayNightCycle.Phase p, int hab, Class<?> only ){
		ArrayList<Class<?>> got = rolled( a, p, hab );
		assertFalse( got.isEmpty() );
		for (Class<?> c : got) assertEquals( only, c );
	}

	private static void assertNever( int a, int hab, Class<?>... never ){
		for (DayNightCycle.Phase p : DayNightCycle.Phase.values()){
			for (Class<?> c : rolled( a, p, hab )) assertFalse( c.getSimpleName() + " at " + a, Arrays.asList( never ).contains( c ) );
		}
	}

	private static void assertSome( int a, int hab, Class<?>... some ){
		ArrayList<Class<?>> got = rolled( a, DayNightCycle.Phase.DAY, hab );
		got.addAll( rolled( a, DayNightCycle.Phase.NIGHT, hab ) );
		for (Class<?> c : some) assertTrue( c.getSimpleName() + " at " + a, got.contains( c ) );
	}

	@Test
	public void placementHoldsInRealWindows(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		Random.pushGenerator( 3 );
		try {
			for (int a : new int[]{ -5, -8, -11, 2, 6 }){
				int[] at = a < 0 ? new int[]{ 0, 0 } : peakGround( a );
				assumeTrue( "no ground of slice " + a + " near the origin", at != null );
				OverworldLevel ow = windowAt( a, at[0], at[1] );
				hero.pos = W / 2 + (H / 2) * W;
				for (int i = 0; i < 4; i++) invoke( ow, "populateFauna", new Class<?>[0] );
				int fauna = 0;
				for (Mob m : ow.mobs){
					if (!OverworldFauna.isFauna( m )) continue;
					fauna++;
					assertTrue( m.getClass().getSimpleName() + " off its ground at " + a, OverworldFauna.livesAt( ow, m, m.pos ) );
					assertTrue( "too near the hero", ow.distance( m.pos, hero.pos ) >= 16 );
					if (m instanceof AlbinoPiranha) assertEquals( Terrain.WATER, ow.map[m.pos] );
				}
				System.out.println( "[layer fauna] slice " + a + ": " + fauna + " beasts" );
				assertTrue( "the cap", fauna <= 12 );
				assertTrue( "slice " + a + " is peopled", fauna > 0 );

				//a slice's first build, the hero not yet there: his arrival is kept clear
				OverworldLevel fresh = windowAt( a, at[0], at[1] );
				Dungeon.level = null;
				fresh.arrivalX = at[0];
				fresh.arrivalY = at[1];
				invoke( fresh, "populateFauna", new Class<?>[0] );
				int arrival = W / 2 + (H / 2) * W;
				for (Mob m : fresh.mobs) assertTrue( "on the arrival", fresh.distance( m.pos, arrival ) >= 16 );
			}
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void aGuestsHeroIsKeptClearToo() throws Exception {
		//a co-op guest far from the host on an abyss slice: nothing is set down at his side either
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		Hero guest = new Hero();
		TestParty.join( guest );
		Random.pushGenerator( 5 );
		try {
			OverworldLevel ow = windowAt( -11, 0, 0 );
			hero.pos = W / 2 + (H / 2) * W;
			guest.pos = hero.pos + 60;
			for (int round = 0; round < 10; round++){
				Actor.clear();
				ow.mobs.clear();
				invoke( ow, "populateFauna", new Class<?>[0] );
				for (Mob m : ow.mobs){
					assertTrue( "beside the guest", ow.distance( m.pos, guest.pos ) >= 16 );
				}
			}
		} finally {
			Random.popGenerator();
			TestParty.leave();
		}
	}

	@Test
	public void aFishWhosePoolCellIsTakenStaysParked(){
		//a deep pool's shelf with dry land beside it, in one of the windows round the origin
		OverworldLevel ow = null;
		int shelf = -1;
		for (int k = 0; k < 9 && shelf == -1; k++){
			ow = windowAt( -5, (k % 3 - 1) * W, (k / 3 - 1) * H );
			for (int c = 3 * W + 3; c < ow.length() - 3 * W - 3 && shelf == -1; c++){
				if (c % W < 3 || c % W > W - 4 || (OverworldFauna.habitat( ow, c ) & Habitat.POOL.bit) == 0) continue;
				for (int dy = -2; dy <= 2; dy++){
					for (int dx = -2; dx <= 2; dx++){
						int n = c + dx + dy * W;
						if (ow.passable[n] && !ow.water[n]) shelf = c;
					}
				}
			}
		}
		assertTrue( "no deep pool by dry land round the origin", shelf != -1 );
		AlbinoPiranha fish = new AlbinoPiranha();
		int wx = ow.worldX() + shelf % W, wy = ow.worldY() + shelf / W;
		invoke( ow, "park", new Class<?>[]{ Mob.class, int.class, int.class }, fish, wx, wy );
		//every water cell it could come back to taken: the only free cells near it are dry land
		boolean land = false;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int c = shelf + dx + dy * W;
				if (ow.water[c] && ow.passable[c]){
					Bat b = new Bat();
					b.pos = c;
					ow.mobs.add( b );
				} else if (ow.passable[c]) land = true;
			}
		}
		assertTrue( land );
		invoke( ow, "unparkMobs", new Class<?>[0] );
		assertFalse( "it waits rather than coming back onto dry land", ow.mobs.contains( fish ) );
		ow.mobs.clear();
		invoke( ow, "unparkMobs", new Class<?>[0] );
		assertTrue( "back in its pool once the water is clear", ow.mobs.contains( fish ) );
		assertEquals( shelf, fish.pos );
	}

	// ------------------------------------------------------------------ the strength

	@Test
	public void theDeepAndTheHighAreTunedNotAbsurd(){
		//per band: the greatest health, blow and accuracy of any of its beasts (docs/world-layers.md)
		int[][] caps = {
				//alt  HT   hit  acc
				{  -1, 110,  26,  22 },
				{  -4, 135,  42,  58 },
				{  -7, 160,  60,  68 },
				{ -10, 500, 115,  80 },
				{   1, 110,  35,  40 },
				{   4, 120,  45,  40 },
				{   8, 120,  48,  40 },
		};
		Random.pushGenerator( 5 );
		try {
			for (int[] cap : caps){
				int a = cap[0];
				Dungeon.depth = WorldLayers.depthOf( a );
				for (Class<? extends Mob> cls : OverworldFauna.kindsAt( a )){
					Mob m = tuned( cls, a );
					String what = cls.getSimpleName() + " at " + a;
					assertTrue( what + " health " + m.HT, m.HT <= cap[1] );
					assertTrue( what + " blow", maxHit( m ) <= cap[2] );
					assertTrue( what + " accuracy " + m.attackSkill( hero ), m.attackSkill( hero ) <= cap[3] );
				}
			}
			//...and the tuning is what holds them there: a piranha of the raw depth hits like a dragon
			Dungeon.depth = WorldLayers.depthOf( -11 );
			assertTrue( maxHit( new AlbinoPiranha() ) > 115 );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void theDangerRisesTheFurtherTheSlice(){
		Random.pushGenerator( 9 );
		try {
			int[] hits = new int[CAVE_BANDS.length], peak = new int[PEAK_BANDS.length];
			for (int i = 0; i < CAVE_BANDS.length; i++) hits[i] = bandMaxHit( CAVE_BANDS[i] );
			for (int i = 0; i < PEAK_BANDS.length; i++) peak[i] = bandMaxHit( PEAK_BANDS[i] );
			System.out.println( "[layer fauna] hardest blows: caves " + Arrays.toString( hits ) + ", peaks " + Arrays.toString( peak ) );
			for (int i = 1; i < hits.length; i++) assertTrue( hits[i] > hits[i - 1] );
			for (int i = 1; i < peak.length; i++) assertTrue( peak[i] > peak[i - 1] );

			//and the share of rolls that bring out something hostile, over every hour and ground
			float r1 = hostile( 1 ), r4 = hostile( 4 ), r8 = hostile( 8 ), c1 = hostile( -1 ), c10 = hostile( -10 );
			System.out.println( "[layer fauna] hostile share: +1 " + r1 + ", +4 " + r4 + ", +8 " + r8 + ", -1 " + c1 + ", -10 " + c10 );
			assertTrue( r4 > r1 );
			assertTrue( r8 > r4 );
			assertTrue( c10 > c1 );

			//the abyss's wraiths: a hero of level 30 (accuracy 10, and 1 a level gained) clears a whole
			//pack in a dozen swings or fewer
			Wraith w = (Wraith) tuned( Wraith.class, -10 );
			float p = (10 + 29) / (2f * w.defenseSkill( hero ));
			float swings = 2 / Math.min( 1f, p );
			System.out.println( "[layer fauna] abyss wraith: def " + w.defenseSkill( hero ) + ", hp " + w.HT + ", a pair takes " + swings + " swings" );
			assertTrue( swings <= 12 );
		} finally {
			Random.popGenerator();
		}
	}

	private int bandMaxHit( int a ){
		Dungeon.depth = WorldLayers.depthOf( a );
		int best = 0;
		for (Class<? extends Mob> cls : OverworldFauna.kindsAt( a )) best = Math.max( best, maxHit( tuned( cls, a ) ) );
		return best;
	}

	private static float hostile( int a ){
		int rolls = 0, foes = 0;
		for (DayNightCycle.Phase p : DayNightCycle.Phase.values()){
			for (int i = 0; i < 4000; i++){
				ArrayList<Mob> got = OverworldFauna.roll( a, WorldModel.Biome.MEADOW, p, allBits() );
				rolls++;
				if (!got.isEmpty() && got.get( 0 ).alignment == Char.Alignment.ENEMY) foes++;
			}
		}
		return foes / (float) rolls;
	}

	@Test
	public void theIcyOnesStillFindSnowInSummer(){
		//midsummer, the warmest the mountains get: the frozen rows must still have ground to roll on
		float shift = WorldModel.calendarShift( GameCalendar.Season.SUMMER, 0.5f );
		for (int a : new int[]{ 5, 8, 9, 10 }){
			int[] at = peakGround( a );
			assertNotNull( "no ground of +" + a, at );
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, at[0] - W / 2, at[1] - H / 2, shift );
			int open = 0, frozen = 0;
			for (int c = 0; c < w.terrain.length; c++){
				if ((Terrain.flags[w.terrain[c]] & Terrain.PASSABLE) == 0) continue;
				open++;
				if (w.frozen[c]) frozen++;
			}
			float share = frozen / (float) Math.max( 1, open );
			System.out.println( "[layer fauna] +" + a + " midsummer: " + Math.round( share * 100 ) + "% of the open ground frozen" );
			assertTrue( "+" + a + " has no snow in summer", share > 0.05f );
		}
	}

	@Test
	public void aWorldSlicesBrutesCarryPursesOfTheirBand(){
		Random.pushGenerator( 13 );
		try {
			for (int a : CAVE_BANDS){
				Dungeon.depth = WorldLayers.depthOf( a );
				for (int i = 0; i < 200; i++){
					Item purse = tuned( Brute.class, a ).createLoot();
					assertTrue( purse instanceof Gold );
					assertTrue( "a purse of " + purse.quantity() + " at " + a, purse.quantity() <= 80 );
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void theCavesBeastsOreRunsDryByTheDay(){
		OverworldLevel ow = windowAt( -8, 0, 0 );
		Dungeon.cycleTurn = 10 * DayNightCycle.FULL_CYCLE + 100;
		assertEquals( 0.2f, ow.oreDropChance( 0.2f ), 1e-6f );
		for (int i = 0; i < 19; i++) ow.oreDropped();
		assertTrue( "the twentieth of a day", ow.oreDropChance( 0.2f ) < 1e-4f );

		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		assertEquals( 19, b.getInt( "ore_drops" ) );
		assertEquals( 10, b.getInt( "ore_drop_day" ) );

		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		assertEquals( "a new day", 0.2f, ow.oreDropChance( 0.2f ), 1e-6f );
	}

	@Test
	public void tunedStrengthSurvivesASave(){
		Dungeon.depth = 108;
		AlbinoPiranha fish = new AlbinoPiranha();
		fish.setStatDepth( 24 );
		Bundle b = new Bundle();
		fish.storeInBundle( b );
		AlbinoPiranha back = new AlbinoPiranha();
		back.restoreFromBundle( b );
		assertEquals( 24, back.statDepth() );
		assertEquals( 58, back.defenseSkill );
		assertEquals( 130, back.HT );
		Random.pushGenerator( 1 );
		try {
			assertTrue( maxHit( back ) <= 52 );
		} finally {
			Random.popGenerator();
		}
		//an older save knows nothing of it: the beast is the floor's, as it always was
		b.remove( "stat_depth" );
		AlbinoPiranha old = new AlbinoPiranha();
		old.restoreFromBundle( b );
		assertEquals( 108, old.statDepth() );
		assertFalse( old.statTuned() );

		Wraith w = new Wraith();
		w.setStatDepth( 30 );
		Bundle wb = new Bundle();
		w.storeInBundle( wb );
		Wraith wback = new Wraith();
		wback.restoreFromBundle( wb );
		assertEquals( w.HT, wback.HT );
		assertEquals( 30, wback.statDepth() );

		Bunny hare = new Bunny();
		hare.setAlpine();
		Bundle hb = new Bundle();
		hare.storeInBundle( hb );
		Bunny hback = new Bunny();
		hback.restoreFromBundle( hb );
		assertEquals( Mob.Thermal.COLD_DWELLER, hback.thermal );
		assertEquals( Messages.get( Bunny.class, "alpine_desc" ), hback.description() );
		hb.remove( "alpine" );
		Bunny hold = new Bunny();
		hold.restoreFromBundle( hb );
		assertEquals( Mob.Thermal.NORMAL, hold.thermal );
	}

	@Test
	public void theHourCullsTheSkyNotTheDark(){
		OverworldLevel sky = new OverworldLevel( 2 );
		sky.setSize( 64, 64 );
		sky.mobs = new HashSet<>();
		Dungeon.level = sky;
		hero.pos = 32 + 32 * 64;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		Goat goat = new Goat();
		goat.pos = 2 + 2 * 64;
		GrayWolf wolf = new GrayWolf();
		wolf.pos = 60 + 60 * 64;
		sky.mobs.add( goat );
		sky.mobs.add( wolf );
		OverworldFauna.cull( sky );
		assertFalse( "a goat out at night is gone", sky.mobs.contains( goat ) );
		assertTrue( sky.mobs.contains( wolf ) );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		Goat day = new Goat();
		day.pos = 2 + 2 * 64;
		sky.mobs.add( day );
		OverworldFauna.cull( sky );
		assertFalse( "the night's wolves by day", sky.mobs.contains( wolf ) );
		assertTrue( sky.mobs.contains( day ) );

		OverworldLevel cave = new OverworldLevel( -6 );
		cave.setSize( 64, 64 );
		cave.mobs = new HashSet<>();
		Dungeon.level = cave;
		Bat bat = new Bat();
		bat.pos = 60 + 60 * 64;
		cave.mobs.add( bat );
		OverworldFauna.cull( cave );
		assertTrue( "no sky, no hours: the caves are never culled", cave.mobs.contains( bat ) );
	}

	@Test
	public void theNewWordsAreWritten(){
		for (String key : new String[]{ "name", "desc", "butt", "brace" }){
			String s = Messages.get( Goat.class, key );
			assertNotEquals( key, Messages.NO_TEXT_FOUND, s );
			assertFalse( s.isEmpty() );
		}
		assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( Bunny.class, "alpine_desc" ) );
		assertTrue( Messages.get( Golem.class, "desc" ).contains( "caves" ) );
	}
}
