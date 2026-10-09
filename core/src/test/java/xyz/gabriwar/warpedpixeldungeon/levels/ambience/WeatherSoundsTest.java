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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.LightningFlash;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherOverlay;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.TownChurchLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.WeatherScape.Where;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The weather's sounder (WeatherSounds) on a level: one on every level, what reaches the hero
 * by the level's kind, the climate read off ClimateManager (the debug storm among it), silent
 * off its level, with the hero dead or the channel off, its clock held while time is frozen or
 * the window moves, a storm's strikes handed to it only while it is heard, and the sandstorm drawn
 * by the overlay just where its bed is laid.
 */
public class WeatherSoundsTest {

	private Level savedLevel;
	private Hero savedHero;
	private float savedElapsed;
	private GL20 savedGl, savedGl20;
	private static boolean held;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedElapsed = Game.elapsed;
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
	}

	@After
	public void tearDown(){
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.elapsed = savedElapsed;
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Sample.INSTANCE.ambientEnable( true );
		Emitter.freezeEmitters = false;
		held = false;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugPrecipTypeOverride = null;
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugForceStorm = false;
	}

	//a bare floor at a climate depth: rock round the edge, a pool
	private static Level floor( final int climateDepth ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public boolean fogHeld(){ return held; }
			@Override public int climateDepth(){ return climateDepth; }
		};
		l.setSize( 30, 30 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < 30; i++){
			l.map[i] = l.map[i + 29 * 30] = l.map[i * 30] = l.map[29 + i * 30] = Terrain.WALL;
		}
		for (int y = 11; y <= 14; y++) for (int x = 1; x < 29; x++) l.map[x + y * 30] = Terrain.WATER;
		return l;
	}

	@Test
	public void everyLevelGetsOneHearingWhatReachesIt(){
		assertSame( Where.EXPOSED, WeatherSounds.where( floor( 1 ) ) );
		assertSame( Where.EXPOSED, WeatherSounds.where( floor( 15 ) ) );
		assertSame( "the climate reaches nothing from the city down", Where.SILENT, WeatherSounds.where( floor( 16 ) ) );
		assertSame( Where.SILENT, WeatherSounds.where( floor( 27 ) ) );
		assertSame( Where.OPEN, WeatherSounds.where( new OverworldLevel( 0 ) ) );
		assertSame( "the peaks", Where.OPEN, WeatherSounds.where( new OverworldLevel( 4 ) ) );
		assertSame( Where.CAVE, WeatherSounds.where( new OverworldLevel( -3 ) ) );
		assertSame( Where.INDOORS, WeatherSounds.where( new TownChurchLevel() ) );
		//a boss floor, with no place of its own, still hears the weather
		Level boss = floor( 5 );
		assertNotNull( WeatherSounds.forLevel( boss ) );
		assertSame( Where.EXPOSED, WeatherSounds.forLevel( boss ).scape.sky.where );
	}

	@Test
	public void theDebugStormIsAStorm(){
		assertFalse( ClimateManager.isStorming() );
		ClimateManager.debugForceStorm = true;
		assertTrue( ClimateManager.isStorming() );
		ClimateManager.debugForceStorm = false;
		assertFalse( ClimateManager.isStorming() );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void theDebugStormEarnsNoBadge() throws Exception {
		Field global = Badges.class.getDeclaredField( "global" ), local = Badges.class.getDeclaredField( "local" );
		global.setAccessible( true );
		local.setAccessible( true );
		Object savedGlobal = global.get( null ), savedLocal = local.get( null );
		String savedSeed = Dungeon.customSeedText;
		try {
			//every badge already in the profile (nothing to unlock), and a seeded run (nothing shown)
			global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
			local.set( null, new HashSet<Badges.Badge>() );
			Dungeon.customSeedText = "debug";
			Dungeon.level = new OverworldLevel( 0 );
			ClimateManager.debugForceStorm = true;
			Badges.validateWorldTurn();
			assertFalse( "out in a storm of the debug menu's",
					((HashSet<Badges.Badge>) local.get( null )).contains( Badges.Badge.STORM_CHASER ) );
		} finally {
			global.set( null, savedGlobal );
			local.set( null, savedLocal );
			Dungeon.customSeedText = savedSeed;
		}
	}

	@Test
	public void silentOffItsLevelWithoutAHeroOrTheChannelAndHeldWithTime(){
		held = false;
		Level wet = floor( 1 );
		Dungeon.level = wet;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 15 + 20 * 30;
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.3f;
		WeatherSounds sounds = WeatherSounds.forLevel( wet );
		final ArrayList<AmbientSound> heard = new ArrayList<>();
		sounds.out = (bed, s, take, level, pitch, pan) -> heard.add( s );
		Game.elapsed = 0.05f;

		for (int f = 0; f < 600; f++) sounds.update();
		assertFalse( "heard on its level", heard.isEmpty() );
		//read off the climate: the rain on a dungeon floor, and no wind there however it blows
		assertTrue( heard.contains( AmbientSound.RAIN ) );
		ClimateManager.debugWindOverride = 25f;
		heard.clear();
		for (int f = 0; f < 600; f++) sounds.update();
		assertEquals( new HashSet<>( Arrays.asList( AmbientSound.RAIN ) ), new HashSet<>( heard ) );

		//another level is current (the scene is changing): nothing, not even its clock
		Dungeon.level = floor( 1 );
		heard.clear();
		float clock = sounds.scape.now;
		for (int f = 0; f < 600; f++) sounds.update();
		assertTrue( heard.isEmpty() );
		assertEquals( clock, sounds.scape.now, 0f );
		Dungeon.level = wet;

		//the hero dead
		Dungeon.hero.HP = 0;
		for (int f = 0; f < 600; f++) sounds.update();
		assertTrue( heard.isEmpty() );
		Dungeon.hero.HP = Dungeon.hero.HT;

		//the ambience off
		Sample.INSTANCE.ambientEnable( false );
		for (int f = 0; f < 600; f++) sounds.update();
		assertTrue( heard.isEmpty() );
		Sample.INSTANCE.ambientEnable( true );

		//time frozen, and the overworld's window moving: the clock stands still too
		Emitter.freezeEmitters = true;
		clock = sounds.scape.now;
		for (int f = 0; f < 600; f++) sounds.update();
		assertTrue( heard.isEmpty() );
		assertEquals( clock, sounds.scape.now, 0f );
		Emitter.freezeEmitters = false;
		held = true;
		for (int f = 0; f < 600; f++) sounds.update();
		assertTrue( heard.isEmpty() );
		assertEquals( clock, sounds.scape.now, 0f );
		held = false;

		//heard again, after a quiet
		for (int f = 0; f < 600; f++) sounds.update();
		assertFalse( heard.isEmpty() );
		sounds.destroy();
	}

	@Test
	public void aStormsStrikeIsHandedOnOnlyWhileTheWeatherIsHeard(){
		held = false;
		Level wet = floor( 1 );
		Dungeon.level = wet;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 15 + 20 * 30;
		//dry: only the thunder
		ClimateManager.debugPrecipOverride = 0f;
		WeatherSounds sounds = WeatherSounds.forLevel( wet );
		final ArrayList<AmbientSound> heard = new ArrayList<>();
		sounds.out = (bed, s, take, level, pitch, pan) -> heard.add( s );
		Game.elapsed = 0.05f;

		//not heard yet: the overlay's own crack plays
		sounds.destroy();
		assertFalse( WeatherSounds.strike( 2f, 2f, false, false ) );
		sounds = WeatherSounds.forLevel( wet );
		sounds.out = (bed, s, take, level, pitch, pan) -> heard.add( s );
		//in its first quiet: a bolt in sight is handed back for the crack with its flash, one out of
		//sight is taken and lost with the quiet's other sounds
		sounds.update();
		assertFalse( WeatherSounds.strike( 2f, 2f, false, true ) );
		assertTrue( WeatherSounds.strike( 2f, 2f, false, false ) );
		for (int f = 0; f < 100; f++) sounds.update();
		assertTrue( WeatherSounds.strike( 2f, 2f, false, false ) );
		for (int f = 0; f < 40; f++) sounds.update();
		assertEquals( Arrays.asList( AmbientSound.THUNDER_NEAR ), heard );

		//the channel off, or the hero dead: back to the crack
		Sample.INSTANCE.ambientEnable( false );
		assertFalse( WeatherSounds.strike( 2f, 2f, false, false ) );
		Sample.INSTANCE.ambientEnable( true );
		Dungeon.hero.HP = 0;
		assertFalse( WeatherSounds.strike( 2f, 2f, false, false ) );
		Dungeon.hero.HP = Dungeon.hero.HT;
		//another level shown
		Dungeon.level = floor( 1 );
		assertFalse( WeatherSounds.strike( 2f, 2f, false, false ) );
		Dungeon.level = wet;
		assertTrue( WeatherSounds.strike( 2f, 2f, false, false ) );
		//time frozen, or the window moving: the weather's clock holds, so its thunder would come
		//all at once when it runs again; the crack plays with the flash instead
		for (int f = 0; f < 200; f++) sounds.update();
		heard.clear();
		Emitter.freezeEmitters = true;
		for (int k = 0; k < 10; k++){
			assertFalse( WeatherSounds.strike( 2f, 2f, false, false ) );
			for (int f = 0; f < 40; f++) sounds.update();
		}
		Emitter.freezeEmitters = false;
		held = true;
		assertFalse( WeatherSounds.strike( 2f, 2f, false, false ) );
		held = false;
		for (int f = 0; f < 200; f++) sounds.update();
		assertTrue( "nothing kept for later", heard.isEmpty() );
		assertTrue( WeatherSounds.strike( 2f, 2f, false, false ) );

		//a level the weather never reaches takes the strike and stays quiet
		Level deep = floor( 20 );
		Dungeon.level = deep;
		WeatherSounds quiet = WeatherSounds.forLevel( deep );
		heard.clear();
		quiet.out = (bed, s, take, level, pitch, pan) -> heard.add( s );
		quiet.update();
		assertTrue( WeatherSounds.strike( 2f, 2f, false, false ) );
		for (int f = 0; f < 200; f++) quiet.update();
		assertTrue( heard.isEmpty() );

		//gone with its scene
		quiet.destroy();
		assertFalse( WeatherSounds.strike( 2f, 2f, false, false ) );
		sounds.destroy();
	}

	//an overworld slice at an altitude, its window placed so the hero, in its middle, stands on the
	//world cell (wx, wy); its window moving while held
	private static OverworldLevel slice( int altitude, long seed, int wx, int wy ){
		return slice( altitude, seed, wx, wy, false );
	}

	//the same, all of its ground snowed under or none of it
	private static OverworldLevel slice( int altitude, long seed, int wx, int wy, final boolean frozen ){
		OverworldLevel ow = new OverworldLevel( altitude ){
			@Override public boolean fogHeld(){ return held; }
			@Override public boolean frozenAt( int cell ){ return frozen; }
		};
		ow.setSize( 64, 64 );
		ow.mobs = new HashSet<>();
		ow.worldSeed = seed;
		ow.worldX = wx - 32;
		ow.worldY = wy - 32;
		Arrays.fill( ow.map, Terrain.EMPTY );
		return ow;
	}

	//the first world cell (on a coarse grid out from the origin) whose biome is or is not the desert
	private static int[] cell( long seed, boolean desert ){
		for (int r = 0; r < 200; r++){
			for (int i = -r; i <= r; i++){
				for (int j = -r; j <= r; j++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int x = i * 32, y = j * 32;
					if ((WorldModel.biomeAt( seed, x, y ) == WorldModel.Biome.DESERT) == desert) return new int[]{ x, y };
				}
			}
		}
		throw new AssertionError( "no " + (desert ? "desert" : "other ground") + " for seed " + seed );
	}

	@Test
	public void theSandIsDrawnJustWhereTheSandstormIsHeard(){
		held = false;
		final long seed = 3L;
		int[] sand = cell( seed, true ), grass = cell( seed, false );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 32 + 32 * 64;
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugWindOverride = 15f;

		//the desert's surface, 10 m/s and more: the sand blows
		Level desert = slice( 0, seed, sand[0], sand[1] );
		Dungeon.level = desert;
		assertTrue( WeatherSounds.sandstorm( desert ) );
		//not on other ground, nor on the peaks or in the caves over the same cell, nor indoors or on a
		//dungeon floor, nor under 10 m/s
		assertFalse( WeatherSounds.sandstorm( slice( 0, seed, grass[0], grass[1] ) ) );
		assertFalse( "the peaks", WeatherSounds.sandstorm( slice( 4, seed, sand[0], sand[1] ) ) );
		assertFalse( "the caves", WeatherSounds.sandstorm( slice( -3, seed, sand[0], sand[1] ) ) );
		assertFalse( WeatherSounds.sandstorm( new TownChurchLevel() ) );
		assertFalse( WeatherSounds.sandstorm( floor( 1 ) ) );
		assertFalse( WeatherSounds.sandstorm( null ) );
		ClimateManager.debugWindOverride = 9.9f;
		assertFalse( WeatherSounds.sandstorm( desert ) );
		ClimateManager.debugWindOverride = 10f;
		assertTrue( WeatherSounds.sandstorm( desert ) );
		//rain does not lay the sand; a blizzard is its own wind where it lands, on snowed-under ground,
		//but on thawed sand it is not heard (WeatherScape.tick) and the sand blows
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.3f;
		assertTrue( WeatherSounds.sandstorm( desert ) );
		ClimateManager.debugPrecipTypeOverride = PrecipType.BLIZZARD;
		assertFalse( WeatherSounds.sandstorm( slice( 0, seed, sand[0], sand[1], true ) ) );
		assertTrue( "a blizzard on thawed sand", WeatherSounds.sandstorm( desert ) );
		ClimateManager.debugPrecipTypeOverride = null;
		ClimateManager.debugPrecipOverride = 0f;

		//the overlay's sand: none where there is no sandstorm, from a floor at 10 m/s to full by 20
		ClimateManager.debugWindOverride = 9f;
		assertEquals( 0f, WeatherOverlay.sandstorm(), 0f );
		ClimateManager.debugWindOverride = 10f;
		float low = WeatherOverlay.sandstorm();
		assertTrue( low > 0f && low < 1f );
		ClimateManager.debugWindOverride = 15f;
		assertTrue( WeatherOverlay.sandstorm() > low );
		ClimateManager.debugWindOverride = 20f;
		assertEquals( 1f, WeatherOverlay.sandstorm(), 1e-6f );
		ClimateManager.debugWindOverride = 30f;
		assertEquals( 1f, WeatherOverlay.sandstorm(), 1e-6f );
		Dungeon.level = slice( 0, seed, grass[0], grass[1] );
		assertEquals( 0f, WeatherOverlay.sandstorm(), 0f );
		Dungeon.level = desert;

		//and the sounds agree: the sandstorm bed is laid where the sand is drawn, the gale elsewhere,
		//under a clear sky and under a blizzard, on thawed sand and on snowed-under sand
		for (PrecipType fall : new PrecipType[]{ null, PrecipType.BLIZZARD }){
			ClimateManager.debugPrecipTypeOverride = fall;
			ClimateManager.debugPrecipOverride = fall == null ? 0f : 0.5f;
			for (float wind : new float[]{ 15f, 9f }){
				for (Level l : new Level[]{ desert, slice( 0, seed, sand[0], sand[1], true ),
						slice( 0, seed, grass[0], grass[1] ), slice( 4, seed, sand[0], sand[1] ) }){
					ClimateManager.debugWindOverride = wind;
					Dungeon.level = l;
					WeatherSounds sounds = WeatherSounds.forLevel( l );
					final ArrayList<AmbientSound> heard = new ArrayList<>();
					sounds.out = (bed, s, take, level, pitch, pan) -> heard.add( s );
					Game.elapsed = 0.05f;
					for (int f = 0; f < 600; f++) sounds.update();
					String at = fall + " at " + wind + " m/s";
					assertEquals( at, WeatherSounds.sandstorm( l ), heard.contains( AmbientSound.SANDSTORM ) );
					assertEquals( at, WeatherSounds.sandstorm( l ), WeatherOverlay.sandstorm() > 0f );
					sounds.destroy();
				}
			}
		}
	}

	//the overlay's frames, at 20 a second
	private static void run( WeatherOverlay o, float seconds ){
		Game.elapsed = 0.05f;
		for (float t = 0f; t < seconds; t += 0.05f) o.update();
	}

	private static ArrayList<Emitter> emitters( WeatherOverlay o ){
		ArrayList<Emitter> all = new ArrayList<>();
		for (Gizmo g : o.membersView()) if (g instanceof Emitter) all.add( (Emitter) g );
		return all;
	}

	//the sandstorm's haze: the overlay's second wash, after the veil
	private static ColorBlock haze( WeatherOverlay o ){
		int n = 0;
		for (Gizmo g : o.membersView()) if (g instanceof ColorBlock && n++ == 1) return (ColorBlock) g;
		throw new AssertionError( "no haze" );
	}

	//the overlay over the desert at this wind, its haze and its emitters made: textures without a
	//GPU (the particles' sheet is painted into its Pixmap, nothing is drawn)
	private static WeatherOverlay overDesert( float wind ){
		stubGl();
		int[] sand = cell( 3L, true );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 32 + 32 * 64;
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugWindOverride = wind;
		Dungeon.level = slice( 0, 3L, sand[0], sand[1] );
		WeatherOverlay o = new WeatherOverlay();
		o.setup();
		return o;
	}

	//textures without a GPU
	private static void stubGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
	}

	@Test
	public void theOverlayDrawsTheSandAndTheFlavourLiesStillUnderIt(){
		held = false;
		WeatherOverlay o = overDesert( 15f );
		o.setAmbient( WeatherOverlay.AmbientType.DUST );
		Emitter dust = emitters( o ).get( 0 );
		ColorBlock haze = haze( o );

		//the sand driven, the dust puffing, the haze up; the dust motes of a calm day lie still
		run( o, 5f );
		assertTrue( "the haze: " + haze.am, haze.visible && haze.am > 0.05f );
		ArrayList<Emitter> sand = emitters( o );
		sand.remove( dust );
		assertEquals( 2, sand.size() );
		for (Emitter e : sand) assertTrue( e.on && e.countLiving() > 0 );
		assertFalse( "the calm day's dust in a sandstorm", dust.on );

		//the window moving: the ground under the hero is not read, and what blows holds
		held = true;
		ClimateManager.debugWindOverride = 0f;
		run( o, 3f );
		assertTrue( "the haze held: " + haze.am, haze.am > 0.05f );
		for (Emitter e : sand) assertTrue( e.on );

		//the wind drops: the sand is laid, the haze clears, the dust comes back
		held = false;
		run( o, 10f );
		assertFalse( haze.visible );
		for (Emitter e : sand) assertFalse( e.on );
		assertTrue( dust.on );

		//but heat that hurts still stands in the air in a sandstorm
		ClimateManager.debugWindOverride = 15f;
		o.setAmbient( WeatherOverlay.AmbientType.HEAT_RAYS );
		ArrayList<Emitter> heat = emitters( o );
		heat.removeAll( sand );
		heat.remove( dust );
		assertEquals( 1, heat.size() );
		run( o, 5f );
		assertTrue( haze.visible );
		assertTrue( heat.get( 0 ).on );
	}

	@Test
	public void theHazeIsLitAsTheGroundUnderIt() throws Exception {
		held = false;
		Field sun = ClimateManager.class.getDeclaredField( "sunLight" );
		Field moon = ClimateManager.class.getDeclaredField( "moonLight" );
		Field ambient = ClimateManager.class.getDeclaredField( "localAmbientLight" );
		for (Field f : new Field[]{ sun, moon, ambient }) f.setAccessible( true );
		float s = sun.getFloat( null ), m = moon.getFloat( null ), a = ambient.getFloat( null );
		try {
			float[] day = new float[2], night = new float[2];
			for (float[] at : new float[][]{ day, night }){
				float light = at == day ? 1f : 0f;
				sun.setFloat( null, light );
				moon.setFloat( null, 0f );
				ambient.setFloat( null, light );
				WeatherOverlay o = overDesert( 20f );
				run( o, 4f );
				ColorBlock haze = haze( o );
				at[0] = haze.rm;
				at[1] = haze.am;
			}
			//by day the haze is the sand's own tan; by night as dark as the ground under it, never
			//a pale wash over a dark scene
			assertEquals( 0xC8 / 255f, day[0], 0.01f );
			assertTrue( "night " + night[0] + ", day " + day[0], night[0] < day[0] * 0.5f );
			assertEquals( "as thick", day[1], night[1], 0.08f );
		} finally {
			sun.setFloat( null, s );
			moon.setFloat( null, m );
			ambient.setFloat( null, a );
		}
	}

	// ------------------------------------------------------------ the blizzard

	//the hero in the middle of a level
	private static void heroAmid( Level l ){
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = l.width() / 2 + l.height() / 2 * l.width();
		Dungeon.level = l;
	}

	//a blizzard at this rate in this wind
	private static void blizzard( float rate, float wind ){
		ClimateManager.debugPrecipTypeOverride = PrecipType.BLIZZARD;
		ClimateManager.debugPrecipOverride = rate;
		ClimateManager.debugWindOverride = wind;
	}

	//the overlay over snowed-under ground in a blizzard
	private static WeatherOverlay overSnow( float rate, float wind ){
		stubGl();
		heroAmid( slice( 0, 3L, 0, 0, true ) );
		blizzard( rate, wind );
		WeatherOverlay o = new WeatherOverlay();
		o.setup();
		return o;
	}

	//the blizzard's vignette: the overlay's one plain image
	private static Image vignette( WeatherOverlay o ){
		for (Gizmo g : o.membersView()) if (g instanceof Image && !(g instanceof ColorBlock)) return (Image) g;
		throw new AssertionError( "no vignette" );
	}

	//the vignette placed on the camera as it is drawn (WeatherOverlay.draw)
	private static void place( WeatherOverlay o ) throws Exception {
		Method m = WeatherOverlay.class.getDeclaredMethod( "placeVignette" );
		m.setAccessible( true );
		m.invoke( o );
	}

	//the veil: the overlay's first wash
	private static ColorBlock veil( WeatherOverlay o ){
		for (Gizmo g : o.membersView()) if (g instanceof ColorBlock) return (ColorBlock) g;
		throw new AssertionError( "no veil" );
	}

	//its whiteout: the overlay's third wash, after the veil and the haze
	private static ColorBlock whiteout( WeatherOverlay o ){
		int n = 0;
		for (Gizmo g : o.membersView()) if (g instanceof ColorBlock && n++ == 2) return (ColorBlock) g;
		throw new AssertionError( "no whiteout" );
	}

	@Test
	public void theBlizzardIsDrawnJustWhereItIsHeard(){
		held = false;
		//on snowed-under ground under the open sky and on the peaks, and on a floor the climate
		//reaches; not on thawed ground, in the caves, deep down or indoors, nor in a snowfall
		Object[][] places = {
				{ slice( 0, 3L, 0, 0, true ), true }, { slice( 4, 3L, 0, 0, true ), true }, { floor( 1 ), true },
				{ slice( 0, 3L, 0, 0, false ), false }, { slice( -3, 3L, 0, 0, true ), false },
				{ floor( 20 ), false }, { new TownChurchLevel(), false },
		};
		for (PrecipType fall : new PrecipType[]{ PrecipType.BLIZZARD, PrecipType.SNOW }){
			for (Object[] p : places){
				Level l = (Level) p[0];
				heroAmid( l );
				blizzard( 0.4f, 14f );
				ClimateManager.debugPrecipTypeOverride = fall;
				boolean drawn = fall == PrecipType.BLIZZARD && (Boolean) p[1];
				String at = fall + " on " + l.getClass().getSimpleName() + " " + l.climateDepth();
				assertEquals( at, drawn, WeatherSounds.blizzard( l ) );
				assertEquals( at, drawn, WeatherOverlay.blizzard() > 0f );
				if (l instanceof TownChurchLevel) continue;
				//and the sound agrees: the blizzard bed is laid just where it is drawn
				WeatherSounds sounds = WeatherSounds.forLevel( l );
				final ArrayList<AmbientSound> heard = new ArrayList<>();
				sounds.out = (bed, s, take, level, pitch, pan) -> heard.add( s );
				Game.elapsed = 0.05f;
				for (int f = 0; f < 600; f++) sounds.update();
				assertEquals( at, drawn, heard.contains( AmbientSound.BLIZZARD ) );
				sounds.destroy();
			}
		}

		//the overlay's blizzard: thicker by its rate and its wind, from a floor to full
		heroAmid( slice( 0, 3L, 0, 0, true ) );
		blizzard( 0.02f, 10f );
		float light = WeatherOverlay.blizzard();
		assertTrue( "the lightest: " + light, light > 0.3f && light < 0.4f );
		blizzard( 0.3f, 10f );
		float wetter = WeatherOverlay.blizzard();
		blizzard( 0.3f, 15f );
		float windier = WeatherOverlay.blizzard();
		assertTrue( light < wetter && wetter < windier );
		blizzard( 0.6f, 20f );
		assertEquals( 1f, WeatherOverlay.blizzard(), 1e-6f );
		blizzard( 1f, 30f );
		assertEquals( 1f, WeatherOverlay.blizzard(), 1e-6f );
	}

	@Test
	public void theBlizzardsWhitesAndDrivenSnowComeAndGoWithIt() throws Exception {
		held = false;
		Camera savedCam = Camera.main;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			WeatherOverlay o = overSnow( 0.05f, 10.5f );
			o.setAmbient( WeatherOverlay.AmbientType.DUST );
			Emitter dust = emitters( o ).get( 0 );
			run( o, 6f );
			//the driven streaks and flakes, the vignette up; the calm day's dust lies still
			ArrayList<Emitter> snow = emitters( o );
			snow.remove( dust );
			assertEquals( 2, snow.size() );
			for (Emitter e : snow) assertTrue( e.on && e.countLiving() > 0 );
			assertFalse( "the calm day's dust in a blizzard", dust.on );
			Image vignette = vignette( o );
			assertTrue( vignette.visible );
			place( o );
			float lightAlpha = vignette.am, lightWidth = vignette.width * vignette.scale.x;
			//never short of the screen: the middle clear, the edges white
			assertTrue( lightWidth >= 320f - 1e-3f && vignette.height * vignette.scale.y >= 180f - 1e-3f );
			assertEquals( 160f, vignette.x + lightWidth / 2f, 1e-3f );

			//it thickens: the white closes in to the edges and deepens, more snow is driven
			int lightSnow = snow.get( 0 ).countLiving() + snow.get( 1 ).countLiving();
			blizzard( 0.6f, 20f );
			run( o, 6f );
			place( o );
			assertTrue( vignette.am > lightAlpha );
			assertTrue( "closing in", vignette.width * vignette.scale.x < lightWidth );
			//at its thickest still over the screen's edges, past the flash's shake of it, and placed
			//on the camera as it is drawn: it follows (and shakes) after the overlay's update, and a
			//vignette left where the view was would bare a strip at its leading edge
			Camera.main.scroll.offset( 7f, -5f );
			place( o );
			float w = vignette.width * vignette.scale.x, h = vignette.height * vignette.scale.y;
			assertTrue( "past the shake: " + w, w >= 320f + 4f - 1e-3f && h >= 180f + 4f - 1e-3f );
			assertEquals( 167f, vignette.x + w / 2f, 1e-3f );
			assertEquals( 85f, vignette.y + h / 2f, 1e-3f );
			Camera.main.scroll.set( 0f, 0f );
			int fullSnow = snow.get( 0 ).countLiving() + snow.get( 1 ).countLiving();
			assertTrue( "driven snow: " + lightSnow + " then " + fullSnow, fullSnow > lightSnow * 3 / 2 );

			//the snow stops: the white fades out (eased, not cut), the driven snow stops, the dust comes back
			ClimateManager.debugPrecipTypeOverride = null;
			ClimateManager.debugPrecipOverride = 0f;
			float full = vignette.am;
			run( o, 1f );
			assertTrue( "fading: " + vignette.am, vignette.am > 0.05f && vignette.am < full );
			run( o, 10f );
			assertFalse( vignette.visible );
			for (Emitter e : snow) assertFalse( e.on );
			assertTrue( dust.on );
		} finally {
			Camera.main = savedCam;
		}
	}

	@Test
	public void theBlizzardsVeilIsLaidJustWhereItIsHeard(){
		held = false;
		//a faint blizzard on thawed ground (not heard): only the faint wash its rate lays; on
		//snowed-under ground its own white on top
		float[] laid = new float[2];
		for (int k = 0; k < 2; k++){
			WeatherOverlay o = overSnow( 0.05f, 10.5f );
			if (k == 0) heroAmid( slice( 0, 3L, 0, 0, false ) );
			o.setPrecipitation( PrecipType.BLIZZARD, 0.05f );
			run( o, 6f );
			laid[k] = veil( o ).am;
		}
		assertTrue( "thawed: " + laid[0], laid[0] < 0.015f );
		assertTrue( "snowed under: " + laid[1], laid[1] > 0.03f );
	}

	@Test
	public void aStrongGustWhitesTheBlizzardOutBriefly(){
		held = false;
		float savedTime = Game.timeTotal;
		try {
			for (float rate : new float[]{ 0.6f, 0.01f }){
				WeatherOverlay o = overSnow( rate, rate > 0.1f ? 20f : 10f );
				Game.timeTotal = 0f;
				Game.elapsed = 0.05f;
				float most = 0f, whiteFor = 0f, longest = 0f;
				int whiteouts = 0;
				boolean was = false;
				//a minute of frames, the gusting as the clock runs (it rises through a strong gust
				//every 17 s or so)
				for (int f = 0; f < 1200; f++){
					Game.timeTotal += 0.05f;
					o.update();
					ColorBlock white = f > 10 ? whiteout( o ) : null;
					boolean is = white != null && white.visible;
					if (is){
						most = Math.max( most, white.am );
						whiteFor += 0.05f;
					} else if (was){
						longest = Math.max( longest, whiteFor );
						whiteFor = 0f;
						whiteouts++;
					}
					was = is;
				}
				if (rate > 0.1f){
					assertTrue( "whiteouts: " + whiteouts, whiteouts >= 3 );
					assertTrue( "mostly white: " + most, most > 0.7f );
					assertTrue( "brief: " + longest, longest <= 0.8f + 0.1f );
				} else {
					assertEquals( "none in the lightest", 0, whiteouts );
				}
			}
		} finally {
			Game.timeTotal = savedTime;
		}
	}

	@Test
	public void theBlizzardsWhitesAreLitAsTheGroundUnderThem() throws Exception {
		held = false;
		Field sun = ClimateManager.class.getDeclaredField( "sunLight" );
		Field moon = ClimateManager.class.getDeclaredField( "moonLight" );
		Field ambient = ClimateManager.class.getDeclaredField( "localAmbientLight" );
		for (Field f : new Field[]{ sun, moon, ambient }) f.setAccessible( true );
		float s = sun.getFloat( null ), m = moon.getFloat( null ), a = ambient.getFloat( null );
		try {
			float[] day = new float[2], night = new float[2];
			for (float[] at : new float[][]{ day, night }){
				float light = at == day ? 1f : 0f;
				sun.setFloat( null, light );
				moon.setFloat( null, 0f );
				ambient.setFloat( null, light );
				WeatherOverlay o = overSnow( 0.5f, 15f );
				run( o, 4f );
				Image vignette = vignette( o );
				at[0] = vignette.bm;
				at[1] = vignette.am;
			}
			assertEquals( "white by day", 1f, day[0], 0.01f );
			assertTrue( "night " + night[0] + ", day " + day[0], night[0] < day[0] * 0.5f );
			assertEquals( "as thick", day[1], night[1], 0.05f );
		} finally {
			sun.setFloat( null, s );
			moon.setFloat( null, m );
			ambient.setFloat( null, a );
		}
	}

	// ------------------------------------------------------------ the lightning

	private static int count( WeatherOverlay o, Class<?> c ){
		int n = 0;
		for (Gizmo g : o.membersView()) if (g != null && g.alive && c.isInstance( g )) n++;
		return n;
	}

	//how high the bolt standing on the overlay starts: its arcs' highest point, each image's
	//corners turned and stretched about its origin as it is drawn (a stroke's halo is laid on the
	//line by its middle row, so its corner is no measure)
	private static float top( WeatherOverlay o ){
		float top = Float.MAX_VALUE;
		for (Gizmo b : o.membersView()){
			if (!(b instanceof Lightning) || !b.alive) continue;
			for (Gizmo arc : ((Group) b).membersView()){
				for (Gizmo g : ((Group) arc).membersView()){
					Image img = (Image) g;
					double a = Math.toRadians( img.angle );
					for (float[] c : new float[][]{ { 0, 0 }, { img.width, 0 }, { 0, img.height }, { img.width, img.height } }){
						float lx = (c[0] - img.origin.x) * img.scale.x, ly = (c[1] - img.origin.y) * img.scale.y;
						top = Math.min( top, img.y + img.origin.y + (float)(lx * Math.sin( a ) + ly * Math.cos( a )) );
					}
				}
			}
		}
		return top;
	}

	@Test
	public void aStormCloudsBoltIsDrawnAsTheStormsOwn(){
		held = false;
		Camera savedCam = Camera.main;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			stubGl();
			Level l = floor( 20 );
			heroAmid( l );
			ClimateManager.debugPrecipOverride = 0f;
			l.heroFOV = new boolean[l.length()];
			Arrays.fill( l.heroFOV, true );
			int seen = Dungeon.hero.pos + 3, hidden = Dungeon.hero.pos - 4;
			l.heroFOV[hidden] = false;
			WeatherOverlay o = new WeatherOverlay();
			o.setup();
			Game.elapsed = 0.05f;

			//a cloud's bolt in sight: the storm's bolt, its glow, the flash; under a roof out of the
			//cloud deck it draws two cells up, not from the top of the view through the walls
			StormStrikes.show( l, seen, true );
			o.update();
			assertEquals( 1, count( o, Lightning.class ) );
			float ground = DungeonTilemap.tileCenterToWorld( seen ).y;
			float top = top( o );
			assertTrue( "out of the cloud: " + top + " over " + ground, top > ground - 60f && top < ground - 20f );
			assertEquals( 1, count( o, LightningFlash.class ) );
			int glows = 0;
			for (Gizmo g : o.membersView()) if (g != null && g.alive && g.getClass().getSimpleName().equals( "StrikeGlow" )) glows++;
			assertEquals( 1, glows );
			assertNull( StormStrikes.next() );

			//another at once: drawn, but no second flash inside the gap
			StormStrikes.show( l, seen, true );
			o.update();
			assertEquals( 2, count( o, Lightning.class ) );
			assertEquals( 1, count( o, LightningFlash.class ) );

			//the gap past and the first gone: a cloud's bolt out of sight goes unseen and unheard...
			run( o, 1f );
			assertEquals( 0, count( o, Lightning.class ) + count( o, LightningFlash.class ) );
			StormStrikes.show( l, hidden, true );
			o.update();
			assertEquals( 0, count( o, Lightning.class ) + count( o, LightningFlash.class ) );
			//...while the sky's out of sight still lights the clouds
			StormStrikes.show( l, hidden, false );
			o.update();
			assertEquals( 0, count( o, Lightning.class ) );
			assertEquals( 1, count( o, LightningFlash.class ) );

			//under the open sky a cloud's bolt comes down from the top of the view, as the sky's
			OverworldLevel open = slice( 0, 3L, 0, 0, true );
			heroAmid( open );
			open.heroFOV = new boolean[open.length()];
			Arrays.fill( open.heroFOV, true );
			run( o, 1f );
			StormStrikes.show( open, Dungeon.hero.pos + 3, true );
			o.update();
			assertEquals( 1, count( o, Lightning.class ) );
			assertTrue( "from the top of the view: " + top( o ), top( o ) < 0f );
		} finally {
			Camera.main = savedCam;
		}
	}

	//the arcs of the overlay's living bolts
	private static int arcs( WeatherOverlay o ){
		int n = 0;
		for (Gizmo b : o.membersView()){
			if (!(b instanceof Lightning) || !b.alive) continue;
			for (Gizmo arc : ((Group) b).membersView()) if (arc instanceof Lightning.Arc) n++;
		}
		return n;
	}

	private static int glows( WeatherOverlay o ){
		int n = 0;
		for (Gizmo g : o.membersView()) if (g != null && g.alive && g.getClass().getSimpleName().equals( "StrikeGlow" )) n++;
		return n;
	}

	//the sparks living where the bolts' arcs landed: the overlay's own emitters, in a group of their own
	private static int sparks( WeatherOverlay o ){
		int n = 0;
		for (Gizmo g : o.membersView()){
			if (g == null || g.getClass() != Group.class) continue;
			for (Gizmo e : ((Group) g).membersView()) if (e instanceof Emitter) n += ((Emitter) e).countLiving();
		}
		return n;
	}

	@Test
	public void aBoltsArcsAreDrawnWhereTheHeroSeesThem(){
		held = false;
		Camera savedCam = Camera.main;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			stubGl();
			Level l = floor( 20 );
			heroAmid( l );
			ClimateManager.debugPrecipOverride = 0f;
			l.heroFOV = new boolean[l.length()];
			Arrays.fill( l.heroFOV, true );
			int w = l.width(), cell = Dungeon.hero.pos + 3, hidden = cell + 2 * w;
			l.heroFOV[hidden] = l.heroFOV[hidden + 1] = false;
			WeatherOverlay o = new WeatherOverlay();
			o.setup();
			Game.elapsed = 0.05f;

			//the sky's bolt in sight, on to one beside it and from him to one out of sight (drawn,
			//but no sparks where it lands unseen), from there to another (unseen at both ends); over
			//the water a step seen and one not
			StormStrikes.show( l, cell, false,
					new int[]{ cell, cell + 1, cell + 1, hidden, hidden, hidden + 1 },
					new int[]{ cell, cell - 1, hidden, hidden + 1 } );
			o.update();
			assertEquals( "one bolt, its arcs in it", 1, count( o, Lightning.class ) );
			assertEquals( "the channel and the three arcs seen", 4, arcs( o ) );
			assertEquals( 1, glows( o ) );
			assertEquals( "the arcs flash nothing of their own", 1, count( o, LightningFlash.class ) );
			o.update();
			assertEquals( "three where it ran on to him, one on the water", 4, sparks( o ) );

			//a storm cloud's out of sight: unseen and unheard but for its arc in sight
			run( o, 1f );
			assertEquals( 0, count( o, Lightning.class ) + count( o, LightningFlash.class ) );
			StormStrikes.show( l, hidden, true, new int[]{ hidden, cell }, new int[0] );
			o.update();
			assertEquals( 1, count( o, Lightning.class ) );
			assertEquals( 1, arcs( o ) );
			assertEquals( 0, glows( o ) + count( o, LightningFlash.class ) );
		} finally {
			Camera.main = savedCam;
		}
	}

	@Test
	public void aFramesStepsOverTheWaterAreOneWindowsWorthAndTheBoltsAreLetGo(){
		held = false;
		Camera savedCam = Camera.main;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			stubGl();
			Level l = floor( 20 );
			heroAmid( l );
			ClimateManager.debugPrecipOverride = 0f;
			l.heroFOV = new boolean[l.length()];
			Arrays.fill( l.heroFOV, true );
			int cell = Dungeon.hero.pos + 3;
			//a run over a lake: every cell of its 7x7 window, 48 steps
			int[] lake = new int[2 * 48];
			for (int i = 0; i < lake.length; i += 2){
				lake[i] = cell;
				lake[i + 1] = cell + 1;
			}
			WeatherOverlay o = new WeatherOverlay();
			o.setup();
			Game.elapsed = 0.05f;

			//two such bolts in a frame (resting by a storm cloud runs about a turn a frame): both
			//channels, but one lake's steps between them
			StormStrikes.show( l, cell, false, new int[0], lake );
			StormStrikes.show( l, cell, false, new int[0], lake );
			o.update();
			assertEquals( 2 + 48, arcs( o ) );
			//the next frame's bolt has its own
			StormStrikes.show( l, cell, false, new int[0], lake );
			o.update();
			assertEquals( 3 + 2 * 48, arcs( o ) );

			//once done, the bolts and their glows are destroyed: their images let go of their
			//vertex buffers, which killing and erasing them kept till the scene changed
			ArrayList<Group> done = new ArrayList<>();
			for (Gizmo g : o.membersView()){
				if (g instanceof Lightning || (g != null && g.getClass().getSimpleName().equals( "StrikeGlow" ))) done.add( (Group) g );
			}
			assertEquals( 3 + 3, done.size() );
			run( o, 1f );
			for (Group g : done){
				assertEquals( -1, o.indexOf( g ) );
				assertEquals( 0, g.length );
			}
		} finally {
			Camera.main = savedCam;
		}
	}
}
