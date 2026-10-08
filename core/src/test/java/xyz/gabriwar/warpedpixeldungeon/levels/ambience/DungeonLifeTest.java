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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSpriteTest;
import xyz.gabriwar.warpedpixeldungeon.effects.SliceCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Fauna.Kind;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The life of a floor, headless: a bare floor laid out with something of every place's ground
 * within the hero's reach (a pool and its banks, moss, a torch and a vent in the north wall, a
 * shelf, statues, a scaffold, grass, flowers, ice, embers, remains), the hero in its middle, the
 * scene's update stepped frame by frame - the pictures stepped too, as their layers would. Every
 * place below ground comes alive with its own kinds and only those, the caps hold every frame,
 * nothing turns up where the hero cannot see - his sight the one the dark leaves him -, a fight or
 * the hero's coming sends them off, a short sight lets him nearer, and the hour or their own while
 * sends them home.
 */
public class DungeonLifeTest {

	private static final int W = 40, H = 32;
	//the hero stands in the middle of the room, which runs seven cells every way from him
	private static final int HX = 20, HY = 14;
	private static final float DT = 1f / 30f;

	private float elapsed, clock, precip, sun, moon;
	private boolean freeze, fog;
	private Phase phase;
	private Object weather;
	private Camera camera;
	private Level levelWas;
	private Hero heroWas;
	private final ArrayList<DungeonLife> made = new ArrayList<>();

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
		DungeonCritterSpriteTest.sheet();
	}

	//the climate's sky, which the hero's sight reckons with (DayNightCycle.viewDistanceModifier):
	//never stepped headless, so set here by hand, and put back after
	private static Field sky( String name ) throws Exception {
		Field f = ClimateManager.class.getDeclaredField( name );
		f.setAccessible( true );
		return f;
	}

	//the sky's light: 1 a lit noon, his sight his own; 0 a moonless night, four cells the shorter
	private static void light( float sun ) throws Exception {
		sky( "sunLight" ).setFloat( null, sun );
		sky( "moonLight" ).setFloat( null, 0f );
	}

	@Before
	public void save() throws Exception {
		elapsed = Game.elapsed;
		clock = Game.timeTotal;
		freeze = Emitter.freezeEmitters;
		phase = DayNightCycle.debugPhaseOverride;
		camera = Camera.main;
		levelWas = Dungeon.level;
		heroWas = Dungeon.hero;
		sun = sky( "sunLight" ).getFloat( null );
		moon = sky( "moonLight" ).getFloat( null );
		weather = sky( "weatherState" ).get( null );
		fog = sky( "localFog" ).getBoolean( null );
		precip = ClimateManager.debugPrecipOverride;
		Emitter.freezeEmitters = false;
		Actor.clear();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = HX + HY * W;
		CharSprite hs = new CharSprite();
		hs.x = HX * 16 + 8;
		hs.y = HY * 16 + 14;
		Dungeon.hero.sprite = hs;
		Camera.main = new Camera( 0, 0, W * 16, H * 16, 1f );
		//a clear sky at a lit hour: the hero sees as far as his view distance
		light( 1f );
		sky( "weatherState" ).set( null, WeatherState.CLEAR );
		ClimateManager.setLocalFog( false );
		ClimateManager.debugPrecipOverride = 0f;
	}

	@After
	public void restore() throws Exception {
		for (DungeonLife l : made) l.destroy();
		made.clear();
		Game.elapsed = elapsed;
		//back before anything a critter said rang: nothing still rings into the next test
		Game.timeTotal = clock;
		Emitter.freezeEmitters = freeze;
		DayNightCycle.debugPhaseOverride = phase;
		Camera.main = camera;
		Dungeon.level = levelWas;
		Dungeon.hero = heroWas;
		sky( "sunLight" ).setFloat( null, sun );
		sky( "moonLight" ).setFloat( null, moon );
		sky( "weatherState" ).set( null, weather );
		ClimateManager.setLocalFog( fog );
		ClimateManager.debugPrecipOverride = precip;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	/** A floor of the place, every cell in the hero's sight. */
	private static Level floor( final Place place ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public Place ambience(){ return place; }
		};
		l.setSize( W, H );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		int[] map = l.map;
		Arrays.fill( map, Terrain.WALL );
		fill( map, 14, 8, 13, 13, Terrain.EMPTY );
		//the north wall: a torch (a vent, an ice seam) either side of a shelf, faces over row 8
		map[at( 17, 7 )] = Terrain.WALL_DECO;
		map[at( 23, 7 )] = Terrain.WALL_DECO;
		map[at( 20, 7 )] = Terrain.BOOKSHELF;
		//moss at the foot of the wall and about the room, a pool with its banks, grass and flowers
		map[at( 15, 8 )] = map[at( 16, 8 )] = Terrain.GRASS;
		map[at( 15, 10 )] = map[at( 16, 10 )] = map[at( 15, 11 )] = map[at( 17, 12 )] = Terrain.EMPTY_DECO;
		fill( map, 21, 15, 5, 5, Terrain.WATER );
		fill( map, 15, 15, 4, 3, Terrain.GRASS );
		fill( map, 15, 19, 3, 1, Terrain.HIGH_GRASS );
		fill( map, 15, 13, 3, 1, Terrain.FROZEN_WATER );
		//a scaffold (a flame, rubble), two statues with open air over them, embers
		map[at( 18, 10 )] = Terrain.REGION_DECO;
		map[at( 22, 10 )] = Terrain.STATUE;
		map[at( 24, 10 )] = Terrain.STATUE;
		map[at( 25, 12 )] = Terrain.EMBERS;
		//remains for the flies
		Heap bones = new Heap();
		bones.type = Heap.Type.REMAINS;
		bones.pos = at( 19, 18 );
		l.heaps.put( bones.pos, bones );
		l.buildFlagMaps();
		Arrays.fill( l.heroFOV, true );
		return l;
	}

	private static void fill( int[] map, int x, int y, int w, int h, int t ){
		for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) map[at( i, j )] = t;
	}

	private DungeonLife life( Place p ){
		Level l = floor( p );
		Dungeon.level = l;
		DungeonLife life = DungeonLife.forLevel( l );
		assertNotNull( p + " has no life", life );
		made.add( life );
		return life;
	}

	private interface Look {
		void at( DungeonLife life );
	}

	//seconds of the scene, its clock running on: the life's update, then every picture's own, as
	//their layers would
	private static void step( DungeonLife life, float seconds, Look look ){
		for (float t = 0f; t < seconds; t += DT){
			Game.elapsed = DT;
			Game.timeTotal += DT;
			life.update();
			for (DungeonLife.Critter c : new ArrayList<>( life.live )){
				if (c.sprite.exists) c.sprite.update();
			}
			if (look != null) look.at( life );
		}
	}

	//the caps hold: no kind over its own, and no more than the global cap out at all
	private static void capped( DungeonLife life ){
		for (Kind k : Kind.values()){
			assertTrue( life.place + ": " + k + " over its cap", life.census( k ) <= Fauna.cap( k, life.place ) );
		}
		assertTrue( life.place + ": " + life.total() + " out", life.total() <= Fauna.GLOBAL_CAP );
	}

	// ------------------------------------------------------------ who

	@Test
	public void onlyTheFloorsBelowGroundHaveLife(){
		for (Place p : Place.values()){
			Level l = floor( p );
			DungeonLife life = DungeonLife.forLevel( l );
			if (p.underground()){
				assertNotNull( p + " has no life", life );
				made.add( life );
			} else {
				assertNull( p + " is the overworld's", life );
			}
		}
		assertNull( DungeonLife.forLevel( floor( null ) ) );
		assertNull( DungeonLife.forLevel( null ) );
	}

	@Test
	public void everyPlaceBelowGroundComesAliveWithItsOwnKindsAndTheCapsHold(){
		for (Place p : Place.values()){
			if (!p.underground()) continue;
			final EnumSet<Kind> seen = EnumSet.noneOf( Kind.class );
			EnumSet<Kind> own = EnumSet.noneOf( Kind.class );
			own.addAll( Arrays.asList( Fauna.kinds( p ) ) );
			for (Phase ph : new Phase[]{ Phase.NIGHT, Phase.DAY }){
				DayNightCycle.debugPhaseOverride = ph;
				DungeonLife life = life( p );
				//a minute and a half of each hour at the least, and on while any of its own has yet
				//to show: a kind with little ground (the catacombs' mice have two corners, which the
				//roaches, spiders and moths along that wall often hold) may take a while
				for (float t = 0f; t < 240f && (t < 90f || !seen.containsAll( own )); t += 1f){
					step( life, 1f, l -> {
						capped( l );
						for (DungeonLife.Critter c : l.live) seen.add( c.kind );
					} );
				}
				life.destroy();
			}
			assertEquals( p + " came alive with " + seen, own, seen );
		}
	}

	@Test
	public void nothingTurnsUpWhereTheHeroCannotSee(){
		for (Place p : new Place[]{ Place.SEWERS, Place.PRISON, Place.CAVES, Place.CITY, Place.HALLS, Place.FROZEN,
				Place.NEST, Place.MEADOW, Place.SHORE, Place.CATACOMB }){
			//the field's and the shore's life is out by day
			DayNightCycle.debugPhaseOverride = p == Place.MEADOW || p == Place.SHORE ? Phase.DAY : Phase.NIGHT;
			DungeonLife life = life( p );
			//the west of the room is dark to him
			for (int c = 0; c < Dungeon.level.length(); c++) Dungeon.level.heroFOV[c] = c % W >= HX;
			final IdentityHashMap<CritterSprite, Boolean> known = new IdentityHashMap<>();
			final int[] out = { 0 };
			step( life, 45f, l -> {
				for (DungeonLife.Critter c : l.live){
					if (known.put( c.sprite, true ) != null) continue;
					out[0]++;
					assertTrue( l.place + ": a " + c.kind + " turned up in the dark at x " + c.sprite.ax, c.sprite.ax >= HX * 16 );
				}
			} );
			assertTrue( p + ": nothing turned up in the light", out[0] > 0 );
			life.destroy();
		}
	}

	@Test
	public void theHallsLifeTurnsUpCloseInTheirShortSight(){
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		Dungeon.hero.viewDistance = 3;
		DungeonLife life = life( Place.HALLS );
		final IdentityHashMap<CritterSprite, Boolean> known = new IdentityHashMap<>();
		step( life, 60f, l -> {
			for (DungeonLife.Critter c : l.live){
				if (known.put( c.sprite, true ) != null) continue;
				//where it turned up: within his three cells (and the few px a bank, a light or a
				//statue's top puts it off its cell's middle)
				float dx = Math.abs( c.sprite.ax - (HX * 16 + 8) ), dy = Math.abs( c.sprite.ay - (HY * 16 + 8) );
				assertTrue( "a " + c.kind + " turned up " + dx + "," + dy + "px off", Math.max( dx, dy ) <= 3 * 16 + 13 );
			}
		} );
		assertTrue( "nothing turned up", known.size() > 0 );
		assertEquals( 3, life.sight );
		//everything lets him nearer where he sees so little: a mouse, say, would bolt at three cells
		assertEquals( (3 - 0.75f) * 16, life.reach( Kind.MOUSE.scare ), 0.001f );
		assertEquals( Kind.SALAMANDER.scare, life.reach( Kind.SALAMANDER.scare ), 0f );
	}

	@Test
	public void theHerosSightIsWhatHisFieldOfViewHasNotHisViewDistanceAlone() throws Exception {
		//an open floor, the hero in its middle: the field of view the game casts for him there
		//reaches as far east as his sight
		Dungeon.level = DungeonCritterSpriteTest.floor( 41, 41, Terrain.EMPTY );
		Hero h = Dungeon.hero;
		h.pos = 20 + 20 * 41;
		boolean noFog = Dungeon.debugNoFog;
		Dungeon.debugNoFog = false;
		try {
			//a lit hour under a clear sky: as far as his view distance
			assertSight( 8, h );
			h.viewDistance = 3;
			assertSight( 3, h );
			//the fog takes two cells
			h.viewDistance = 8;
			ClimateManager.setLocalFog( true );
			assertSight( 6, h );
			ClimateManager.setLocalFog( false );
			//a moonless night four, never leaving him under two
			light( 0f );
			assertSight( 4, h );
			h.viewDistance = 4;
			assertSight( 2, h );
			h.viewDistance = 3;
			assertSight( 2, h );
			//the sun low: the dark takes a part of a cell, rounded as the game rounds it
			light( 0.2f );
			h.viewDistance = 8;
			assertSight( DungeonLife.heroSight( h ), h );
			assertTrue( DungeonLife.heroSight( h ) > 4 && DungeonLife.heroSight( h ) < 8 );
		} finally {
			Dungeon.debugNoFog = noFog;
		}
	}

	//his sight is this many cells, and his field of view reaches that far east of him, no farther
	private static void assertSight( int cells, Hero h ){
		assertEquals( cells, DungeonLife.heroSight( h ) );
		Level l = Dungeon.level;
		boolean[] fov = new boolean[l.length()];
		l.updateFieldOfView( h, fov );
		int far = 0;
		for (int d = 1; h.pos % l.width() + d < l.width(); d++) if (fov[h.pos + d]) far = d;
		assertEquals( "his field of view", cells, far );
	}

	@Test
	public void onADarkNightItsLifeTurnsUpInTheLittleHeSees() throws Exception {
		//the halls three floors down, four cells by a lit hour: the dark leaves him two. The life
		//goes by those two - by his view distance its reaches would all be past what he sees
		DayNightCycle.debugPhaseOverride = Phase.DUSK;
		light( 0f );
		Dungeon.hero.viewDistance = 4;
		DungeonLife life = life( Place.HALLS );
		//a skull pillar and a tongue of the cold lava within his two cells
		Level l = Dungeon.level;
		l.map[at( 22, 12 )] = Terrain.STATUE;
		l.map[at( 17, 14 )] = Terrain.WATER;
		l.buildFlagMaps();
		final IdentityHashMap<CritterSprite, Boolean> known = new IdentityHashMap<>();
		final EnumSet<Kind> seen = EnumSet.noneOf( Kind.class );
		step( life, 90f, x -> {
			for (DungeonLife.Critter c : x.live){
				if (known.put( c.sprite, true ) != null) continue;
				seen.add( c.kind );
				float dx = Math.abs( c.sprite.ax - (HX * 16 + 8) ), dy = Math.abs( c.sprite.ay - (HY * 16 + 8) );
				assertTrue( "a " + c.kind + " turned up " + dx + "," + dy + "px off", Math.max( dx, dy ) <= 2 * 16 + 13 );
			}
		} );
		assertEquals( 2, life.sight );
		assertTrue( "only " + seen + " came out in the dark",
				seen.containsAll( EnumSet.of( Kind.SALAMANDER, Kind.EMBER_BEETLE, Kind.CROW ) ) );
	}

	@Test
	public void inAShortSightTheLifeHeedsTheHeroOnlyNearer(){
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		DungeonLife life = life( Place.SEWERS );
		step( life, DT, null );
		//a lit floor's sight: as near as he is
		assertEquals( DungeonLife.FULL_SIGHT, life.sight );
		assertEquals( 56f, life.seems( 56f, Kind.FROG.scare ), 0f );
		//a frog three and a half cells off him crouches, and keeps quiet
		final int[] croaks = { 0, 0 };
		DungeonCritterSprite.Frog lit = frog( croaks, 0 );
		life.live.add( new DungeonLife.Critter( Kind.FROG, lit, null ) );
		step( life, 15f, null );
		assertEquals( "it croaked with him that near", 0, croaks[0] );
		lit.vanish();
		//four cells, the night's (the life looks round again while he stands): the edge of his
		//sight stands for a lit floor's edge, the reach is its own
		Dungeon.hero.viewDistance = 4;
		step( life, DungeonLife.RESCAN + 0.1f, null );
		assertEquals( 4, life.sight );
		float frog = Kind.FROG.scare;
		assertEquals( frog, life.seems( frog, frog ), 0.001f );
		assertEquals( DungeonLife.FULL_SIGHT * 16f, life.seems( 4 * 16f, frog ), 0.001f );
		assertTrue( life.seems( 56f, frog ) > 56f );
		//the same frog at the same three and a half cells sits up and croaks: he is at the edge
		//of what he sees
		DungeonCritterSprite.Frog dark = frog( croaks, 1 );
		life.live.add( new DungeonLife.Critter( Kind.FROG, dark, null ) );
		step( life, 15f, null );
		assertTrue( "it crouched the whole while", croaks[1] > 0 );
		//two cells: the reach cut short to them, and within it he is nearer still
		Dungeon.hero.viewDistance = 2;
		step( life, DungeonLife.RESCAN + 0.1f, null );
		assertEquals( 2, life.sight );
		float r = life.reach( frog );
		assertEquals( (2 - 0.75f) * 16, r, 0.001f );
		assertEquals( frog, life.seems( r, frog ), 0.001f );
		assertEquals( frog / 2f, life.seems( r / 2f, frog ), 0.001f );
		assertEquals( DungeonLife.FULL_SIGHT * 16f, life.seems( 2 * 16f, frog ), 0.001f );
	}

	//a frog on the floor east of the hero, 56px from his feet, counting its croaks in croaks[k]
	private static DungeonCritterSprite.Frog frog( final int[] croaks, final int k ){
		return new DungeonCritterSprite.Frog( HX * 16 + 8 + 56f, HY * 16 + 12f, 0f, 0f, false ){
			@Override protected void croaked(){ croaks[k]++; }
		};
	}

	// ------------------------------------------------------------ near

	@Test
	public void aFightInSightScattersTheLifeAroundIt(){
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		DungeonLife life = life( Place.SEWERS );
		DungeonLife.Critter target = null;
		for (float t = 0f; t < 60f && target == null; t += 1f){
			step( life, 1f, null );
			for (DungeonLife.Critter c : life.live){
				if (c.sprite instanceof DungeonCritterSprite && !(c.sprite instanceof DungeonCritterSprite.Snail)
						&& ((DungeonCritterSprite) c.sprite).calm() && c.sprite.alpha() >= 1f){
					target = c;
					break;
				}
			}
		}
		assertNotNull( "nothing came out", target );
		float fx = target.sprite.gx(), fy = target.sprite.gy();
		//everything calm within its kind's noise of the fight
		ArrayList<DungeonLife.Critter> near = new ArrayList<>();
		for (DungeonLife.Critter c : life.live){
			if (c.sprite instanceof DungeonCritterSprite && ((DungeonCritterSprite) c.sprite).calm()
					&& Math.max( Math.abs( c.sprite.gx() - fx ), Math.abs( c.sprite.gy() - fy ) ) <= c.kind.noise - 16f){
				near.add( c );
			}
		}
		DungeonLife.noise( at( (int)(fx / 16), (int)(fy / 16) ) );
		step( life, DT, null );
		for (DungeonLife.Critter c : near){
			DungeonCritterSprite s = (DungeonCritterSprite) c.sprite;
			if (s instanceof DungeonCritterSprite.Snail){
				assertTrue( "a snail out in the fight", ((DungeonCritterSprite.Snail) s).hidden() );
			} else {
				assertFalse( "a " + c.kind + " sat through the fight", s.exists && s.calm() );
			}
		}
	}

	@Test
	public void theHeroComingUpSendsThemOff(){
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		DungeonLife life = life( Place.PRISON );
		DungeonLife.Critter target = null;
		for (float t = 0f; t < 60f && target == null; t += 1f){
			step( life, 1f, null );
			for (DungeonLife.Critter c : life.live){
				if ((c.kind == Kind.MOUSE || c.kind == Kind.ROACH || c.kind == Kind.SPIDER)
						&& ((DungeonCritterSprite) c.sprite).calm() && c.sprite.alpha() >= 1f){
					target = c;
					break;
				}
			}
		}
		assertNotNull( "nothing came out", target );
		//he steps up beside it
		CharSprite hs = Dungeon.hero.sprite;
		hs.x = target.sprite.gx();
		hs.y = target.sprite.gy() + 2f;
		step( life, DT, null );
		DungeonCritterSprite s = (DungeonCritterSprite) target.sprite;
		assertFalse( "a " + target.kind + " let him walk up", s.exists && s.calm() );
	}

	@Test
	public void aSwiftOffItsStatueSetsTheRestOfTheRowGoing(){
		DayNightCycle.debugPhaseOverride = Phase.DAY;
		DungeonLife life = life( Place.CITY );
		ArrayList<DungeonCritterSprite.Percher> row = new ArrayList<>();
		for (float t = 0f; t < 120f && row.size() < 2; t += 1f){
			step( life, 1f, null );
			row.clear();
			for (DungeonLife.Critter c : life.live){
				if (c.sprite instanceof DungeonCritterSprite.Percher){
					DungeonCritterSprite.Percher p = (DungeonCritterSprite.Percher) c.sprite;
					if (p.calm() && !p.landing()) row.add( p );
				}
			}
		}
		assertTrue( "no row of swifts came down on the statues", row.size() >= 2 );
		//west to east: the floor's two statues, two cells apart
		Collections.sort( row, (a, b) -> Float.compare( a.gx(), b.gx() ) );
		DungeonCritterSprite.Percher first = row.get( 0 ), next = row.get( 1 );
		//a third statue on, four cells from the first: farther than a row reaches, its swift is no
		//part of the first's row
		Dungeon.level.map[at( 26, 10 )] = Terrain.STATUE;
		Dungeon.level.buildFlagMaps();
		DungeonCritterSprite.Percher far = new DungeonCritterSprite.Percher( false, 26 * 16 + 8, 10 * 16 - 3, 11f );
		life.live.add( new DungeonLife.Critter( Kind.SWIFT, far, null ) );
		//the hero comes up west of the first: near enough to send it off, too far off the next
		//for that one to mind him - only its row can send it after the first
		CharSprite hs = Dungeon.hero.sprite;
		hs.x = first.gx() - 40f;
		hs.y = first.gy() + 2f;
		assertTrue( next.gx() - hs.x > life.reach( Kind.SWIFT.scare ) );
		step( life, DT, null );
		assertFalse( "the first stayed on its statue", first.calm() );
		assertFalse( "the rest of its row stayed", next.calm() );
		assertTrue( "one off its row went with it", far.calm() );
	}

	@Test
	public void theHourSendsTheDaysLifeHome(){
		DayNightCycle.debugPhaseOverride = Phase.DAY;
		DungeonLife life = life( Place.MEADOW );
		step( life, 40f, null );
		boolean day = false;
		for (DungeonLife.Critter c : life.live){
			if (c.kind == Kind.BUTTERFLY || c.kind == Kind.FINCH) day = true;
		}
		assertTrue( "no butterflies nor finches came out by day", day );
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		step( life, 40f, null );
		for (DungeonLife.Critter c : life.live){
			assertFalse( "a " + c.kind + " stayed out at night", c.kind == Kind.BUTTERFLY || c.kind == Kind.FINCH );
		}
	}

	@Test
	public void aFlockOrAHareGoesItsWayWhenItsWhileIsUp(){
		//their hour all the while (the dawn: no butterflies out to fill the room), and nothing
		//comes near them: the hero stands as if fishing
		DayNightCycle.debugPhaseOverride = Phase.DAWN;
		DungeonLife life = life( Place.MEADOW );
		final ArrayList<CritterSprite> first = new ArrayList<>();
		boolean flock = false, hare = false;
		for (float t = 0f; t < 120f && !(flock && hare); t += 1f){
			step( life, 1f, null );
			for (DungeonLife.Critter c : life.live){
				if (!DungeonLife.calm( c )) continue;
				if (c.kind == Kind.FINCH && !flock){
					first.addAll( c.flock.birds );
					flock = true;
				} else if (c.kind == Kind.HARE && !hare){
					first.add( c.sprite );
					hare = true;
				}
			}
		}
		assertTrue( "no finches came down", flock );
		assertTrue( "no hare came out", hare );
		step( life, DungeonLife.STAY_MAX + 5f, null );
		for (CritterSprite s : first) assertFalse( "a " + s.getClass().getSimpleName() + " stayed on and on", s.exists );
	}

	@Test
	public void onlyTheCavesBlindFishGlideTheIceFishLeapSilver(){
		//by night: no flock of buntings in the ice taking up the room the fish need
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		for (Place p : new Place[]{ Place.CAVES, Place.FROZEN }){
			DungeonLife life = life( p );
			final int[] fish = { 0, 0 };
			final IdentityHashMap<CritterSprite, Boolean> known = new IdentityHashMap<>();
			step( life, 60f, x -> {
				for (DungeonLife.Critter c : x.live){
					if (c.kind != Kind.FISH || known.put( c.sprite, true ) != null) continue;
					fish[c.sprite instanceof SliceCritterSprite.Swimmer ? 1 : 0]++;
				}
			} );
			assertTrue( p + ": only " + (fish[0] + fish[1]) + " fish", fish[0] + fish[1] >= 6 );
			//the swimmer under the surface is drawn as the blind fish: never the ice fish
			if (p == Place.CAVES){
				assertTrue( "no blind fish glided", fish[1] > 0 );
			} else {
				assertEquals( "the ice fish glided as the caves' blind fish", 0, fish[1] );
			}
			life.destroy();
		}
	}

	@Test
	public void fliesComeToTheHallsPillarsOfSkulls(){
		DayNightCycle.debugPhaseOverride = Phase.DAY;
		DungeonLife life = life( Place.HALLS );
		//no remains lying about: only the pillars draw them
		Dungeon.level.heaps.clear();
		final HashSet<Integer> over = new HashSet<>();
		step( life, 40f, x -> {
			for (DungeonLife.Cloud c : x.clouds) if (c.air == Fauna.Air.FLIES) over.add( c.cell );
		} );
		assertFalse( "no flies came", over.isEmpty() );
		for (int cell : over) assertEquals( "flies over " + cell, Terrain.STATUE, Dungeon.level.map[cell] );
	}

	@Test
	public void timeStandingStillHoldsTheLife(){
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		DungeonLife life = life( Place.SEWERS );
		Emitter.freezeEmitters = true;
		step( life, 20f, null );
		assertEquals( 0, life.live.size() );
		Emitter.freezeEmitters = false;
		step( life, 20f, null );
		assertTrue( life.live.size() > 0 );
	}

	@Test
	public void itIsQuietWhenItsFloorIsNotTheOneShown(){
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		DungeonLife life = life( Place.SEWERS );
		Dungeon.level = floor( Place.SEWERS );
		step( life, 20f, null );
		assertEquals( 0, life.live.size() );
	}

	// ------------------------------------------------------------ gone

	@Test
	public void goneWithTheSceneItTakesItsPicturesAndHearsNoMore() throws Exception {
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		DungeonLife life = life( Place.CATACOMB );
		step( life, 30f, null );
		assertTrue( life.live.size() > 0 );
		//the layers they would be drawn in
		Group layer = new Group();
		ArrayList<CritterSprite> out = new ArrayList<>();
		for (DungeonLife.Critter c : life.live){
			if (!c.sprite.exists) continue;
			layer.add( c.sprite );
			out.add( c.sprite );
		}
		life.destroy();
		assertEquals( 0, life.live.size() );
		for (CritterSprite s : out) assertFalse( s.exists );
		for (Object m : layer.membersView()) assertNull( m );
		//no life on the floor: a fight is heard by none
		Field noises = DungeonLife.class.getDeclaredField( "NOISES" );
		noises.setAccessible( true );
		DungeonLife.noise( at( HX, HY ) );
		@SuppressWarnings("unchecked") ArrayList<Integer> heard = (ArrayList<Integer>) noises.get( null );
		synchronized (heard){
			assertEquals( 0, heard.size() );
		}
		//and a new one is the one that hears
		DungeonLife next = life( Place.CATACOMB );
		DungeonLife.noise( at( HX, HY ) );
		synchronized (heard){
			assertEquals( 1, heard.size() );
		}
		step( next, DT, null );
		synchronized (heard){
			assertEquals( 0, heard.size() );
		}
	}

	// ------------------------------------------------------------ the debug scenes

	@Test
	public void theScenesSetTheHeroDownInSightOfHisPlacesLife(){
		PathFinder.setMapSize( W, H );
		for (Place p : Place.values()){
			if (!p.underground()) continue;
			Level l = floor( p );
			//a passage out of the room's west side, turning north to a dead end out of its sight
			fill( l.map, 6, 14, 8, 1, Terrain.EMPTY );
			fill( l.map, 6, 9, 1, 5, Terrain.EMPTY );
			l.buildFlagMaps();
			Dungeon.level = l;
			int cell = DungeonLife.liveliest( l, at( 6, 9 ) );
			assertTrue( p + ": no cell", cell >= 0 );
			assertTrue( p + ": not open ground", l.passable[cell] && !l.pit[cell] );
			int x = cell % W, y = cell / W;
			assertTrue( p + ": set down at " + x + "," + y + ", not in the room", x >= 14 && x < 27 && y >= 8 && y < 21 );
		}
		assertEquals( "no place, no spot", -1, DungeonLife.liveliest( floor( null ), at( HX, HY ) ) );
	}

	@Test
	public void mothsCircleALightOnlyWithItsFaceInSight(){
		for (Place p : new Place[]{ Place.FROZEN, Place.PRISON }){
			DayNightCycle.debugPhaseOverride = Phase.NIGHT;
			DungeonLife life = life( p );
			Level l = Dungeon.level;
			//the hero sees the two seams (torches) from behind: their wall cells, never the open
			//cell before them, as from a room on the far side of that wall
			int[] lights = { at( 17, 7 ), at( 23, 7 ) };
			for (int c : lights) l.heroFOV[c + W] = false;
			final int[] most = { 0 };
			step( life, 90f, x -> most[0] = Math.max( most[0], x.census( Kind.MOTH ) ) );
			assertEquals( p + ": moths over the back of a wall", 0, most[0] );
			//the faces in sight: they come
			for (int c : lights) l.heroFOV[c + W] = true;
			step( life, 90f, x -> most[0] = Math.max( most[0], x.census( Kind.MOTH ) ) );
			assertTrue( p + ": no moths at a light in sight", most[0] > 0 );
			life.destroy();
		}
	}

	@Test
	public void theScenesLogTheKindsOfThePlace(){
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		String line = DungeonLife.showcase( floor( Place.SEWERS ) );
		for (Kind k : Fauna.kinds( Place.SEWERS )) assertTrue( line, line.contains( k.name().toLowerCase() ) );
		assertTrue( line, line.contains( "frog 100%" ) && line.contains( "gnats 100%" ) );
	}
}
