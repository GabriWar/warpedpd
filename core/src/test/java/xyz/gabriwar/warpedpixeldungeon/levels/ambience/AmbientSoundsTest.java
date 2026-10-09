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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretGardenRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Every place's voices (AmbientSounds.voices) against docs/ambience.md's table, the hours,
 * seasons and weather they keep (night voices never by day, winter hushing the surface's frogs
 * and crickets, storms and rain the birds, the wind only on open ground when it blows, birdsong
 * through the grates only under the town), the vault the city's hushed, the floors that get a
 * sounder, and a sounder silent off its floor, with no hero, with the channel off.
 */
public class AmbientSoundsTest {

	private Level savedLevel;
	private Hero savedHero;
	private float savedElapsed;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedElapsed = Game.elapsed;
	}

	@After
	public void tearDown(){
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.elapsed = savedElapsed;
		Sample.INSTANCE.ambientEnable( true );
		Emitter.freezeEmitters = false;
		held = false;
	}

	//what a place's voices sound: their AmbientSounds (a bed's other sound too), CHAINS for the
	//prison's chains
	private static Set<Object> sounds( Place p ){
		HashSet<Object> s = new HashSet<>();
		for (Voice v : AmbientSounds.voices( p )){
			s.add( v.sound != null ? v.sound : v.takes[0] );
			if (v.alt != null) s.add( v.alt.sound );
		}
		return s;
	}

	//every voice of a place, a bed's other sound among them
	private static ArrayList<Voice> all( Place p ){
		ArrayList<Voice> all = new ArrayList<>();
		for (Voice v : AmbientSounds.voices( p )){
			all.add( v );
			if (v.alt != null) all.add( v.alt );
		}
		return all;
	}

	private static Voice voice( Place p, AmbientSound s ){
		for (Voice v : AmbientSounds.voices( p )) if (v.sound == s) return v;
		throw new AssertionError( p + " has no " + s );
	}

	private static Voice bed( Place p, AmbientSound s ){
		for (Voice v : AmbientSounds.voices( p )) if (v.sound == s && v.bed) return v;
		throw new AssertionError( p + " has no bed of " + s );
	}

	private static Soundscape.Air air( Phase phase, Season season ){
		Soundscape.Air a = new Soundscape.Air();
		a.phase = phase;
		a.season = season;
		return a;
	}

	private static Soundscape.Air outside( Phase phase, Season season, Biome biome ){
		Soundscape.Air a = air( phase, season );
		a.biome = biome;
		return a;
	}

	// ------------------------------------------------------------ the places

	@Test
	public void everyPlaceButTheCaveSlicesHasItsVoices(){
		for (Place p : Place.values()){
			assertEquals( p.toString(), p != Place.CAVE_SLICES, AmbientSounds.voices( p ).length > 0 );
		}
	}

	@Test
	public void eachPlaceHasTheVoicesTheTableLists(){
		HashMap<Place, Object[]> table = new HashMap<>();
		table.put( Place.SEWERS, new Object[]{ AmbientSound.DRIP, AmbientSound.DRIP_METAL, AmbientSound.TRICKLE,
				AmbientSound.POUR, AmbientSound.GURGLE, AmbientSound.FROG, AmbientSound.CRICKET, AmbientSound.RAT,
				AmbientSound.FLY, AmbientSound.BIRD } );
		table.put( Place.PRISON, new Object[]{ AmbientSound.DRIP, AmbientSound.CRACKLE, AmbientSound.CREAK,
				Assets.Sounds.CHAINS, AmbientSound.MOAN, AmbientSound.WIND, AmbientSound.CRICKET, AmbientSound.RAT,
				AmbientSound.SKITTER } );
		table.put( Place.CAVES, new Object[]{ AmbientSound.DRIP, AmbientSound.PEBBLES, AmbientSound.RUMBLE,
				AmbientSound.BAT, AmbientSound.TRICKLE, AmbientSound.CRICKET, AmbientSound.CREAK, AmbientSound.WIND } );
		table.put( Place.CITY, new Object[]{ AmbientSound.ANVIL, AmbientSound.CHIME, AmbientSound.CRACKLE,
				AmbientSound.WIND, AmbientSound.TRICKLE, AmbientSound.COO, AmbientSound.CRICKET, AmbientSound.DRIP } );
		table.put( Place.HALLS, new Object[]{ AmbientSound.LAVA, AmbientSound.CRACKLE, AmbientSound.WHISPER,
				AmbientSound.MOAN, AmbientSound.RUMBLE, AmbientSound.WIND } );
		table.put( Place.FROZEN, new Object[]{ AmbientSound.WIND, AmbientSound.ICE, AmbientSound.DRIP, AmbientSound.BIRD } );
		table.put( Place.NEST, new Object[]{ AmbientSound.SKITTER, AmbientSound.FLY, AmbientSound.DRIP } );
		table.put( Place.VAULT, table.get( Place.CITY ) );
		table.put( Place.TEMPLE, new Object[]{ AmbientSound.CHIME, AmbientSound.DRIP, AmbientSound.WIND } );
		table.put( Place.MINES, new Object[]{ AmbientSound.DRIP, AmbientSound.PEBBLES, AmbientSound.RUMBLE, AmbientSound.BAT } );
		table.put( Place.MEADOW, new Object[]{ AmbientSound.BIRD, AmbientSound.CRICKET, AmbientSound.FLY, AmbientSound.WIND } );
		table.put( Place.SHORE, new Object[]{ AmbientSound.SPLASH, AmbientSound.LAP, AmbientSound.WIND, AmbientSound.BIRD } );
		table.put( Place.CATACOMB, new Object[]{ AmbientSound.DRIP, AmbientSound.MOAN, AmbientSound.WHISPER,
				AmbientSound.SKITTER, AmbientSound.RAT } );
		table.put( Place.SURFACE, new Object[]{ AmbientSound.BIRD, AmbientSound.COO, AmbientSound.CRICKET,
				AmbientSound.FROG, AmbientSound.LAP, AmbientSound.WIND } );
		table.put( Place.PEAKS, new Object[]{ AmbientSound.WIND, AmbientSound.PEBBLES, AmbientSound.BIRD, AmbientSound.ICE } );
		for (Place p : Place.values()){
			if (p == Place.CAVE_SLICES) continue;
			Object[] want = table.get( p );
			assertNotNull( "the test knows " + p, want );
			assertEquals( p + " has exactly its table's", new HashSet<>( Arrays.asList( want ) ), sounds( p ) );
		}
	}

	@Test
	public void theBedsAreWhereTheTableSays(){
		//the water spilling from the sewers' pipes, within four cells: one bed, the sewers' only
		//one, trickling from a pipe or pouring from those that pour
		Voice spill = bed( Place.SEWERS, AmbientSound.TRICKLE );
		assertEquals( 4f, spill.radius, 0f );
		assertArrayEquals( new Source[]{ Source.PIPE }, spill.from );
		assertSame( AmbientSound.POUR, spill.alt.sound );
		assertArrayEquals( new Source[]{ Source.PIPE_POUR }, spill.alt.from );
		int beds = 0;
		for (Voice v : AmbientSounds.voices( Place.SEWERS )) if (v.bed) beds++;
		assertEquals( 1, beds );
		//the water lapping at a bank laps, it never splashes
		assertSame( Source.LAPPING, voice( Place.SHORE, AmbientSound.LAP ).from[0] );
		assertSame( Source.LAPPING, voice( Place.SURFACE, AmbientSound.LAP ).from[0] );
		for (Place p : Place.values()){
			for (Voice v : AmbientSounds.voices( p )) assertFalse( v.sound == AmbientSound.SPLASH && v.from[0] == Source.LAPPING );
		}
		assertSame( Source.TORCH, bed( Place.PRISON, AmbientSound.CRACKLE ).from[0] );
		assertSame( Source.BIG_WATER, bed( Place.CAVES, AmbientSound.TRICKLE ).from[0] );
		assertSame( Source.FLAME, bed( Place.CITY, AmbientSound.CRACKLE ).from[0] );
		assertSame( Source.BIG_WATER, bed( Place.CITY, AmbientSound.TRICKLE ).from[0] );
		assertSame( Source.LAVA, bed( Place.HALLS, AmbientSound.LAVA ).from[0] );
		assertSame( Source.EMBERS, bed( Place.HALLS, AmbientSound.CRACKLE ).from[0] );
		//and where it says they come from
		assertSame( Source.PIPE, voice( Place.SEWERS, AmbientSound.DRIP_METAL ).from[0] );
		assertSame( Source.SHORE, voice( Place.SEWERS, AmbientSound.FROG ).from[0] );
		assertTrue( voice( Place.SEWERS, AmbientSound.FROG ).chorus );
		assertSame( Source.GRASS, voice( Place.SEWERS, AmbientSound.CRICKET ).from[0] );
		assertTrue( voice( Place.SEWERS, AmbientSound.RAT ).unseen );
		assertSame( Source.CAGE, voice( Place.PRISON, AmbientSound.CREAK ).from[0] );
		assertSame( Source.CHASM, voice( Place.PRISON, AmbientSound.WIND ).from[0] );
		assertSame( Source.SCAFFOLD, voice( Place.CAVES, AmbientSound.CREAK ).from[0] );
		assertSame( Source.CHASM, voice( Place.CAVES, AmbientSound.WIND ).from[0] );
		assertSame( Source.STATUE, voice( Place.CITY, AmbientSound.COO ).from[0] );
		assertSame( Source.VENT, voice( Place.CITY, AmbientSound.WIND ).from[0] );
		assertSame( Source.ICE, voice( Place.FROZEN, AmbientSound.ICE ).from[0] );
		assertSame( Source.MELT, voice( Place.FROZEN, AmbientSound.DRIP ).from[0] );
		assertSame( Source.WEB, voice( Place.NEST, AmbientSound.FLY ).from[0] );
		assertSame( Source.FROST, voice( Place.PEAKS, AmbientSound.ICE ).from[0] );
		//the echoing caves and mines
		for (AmbientSound s : new AmbientSound[]{ AmbientSound.DRIP, AmbientSound.PEBBLES, AmbientSound.BAT }){
			assertTrue( voice( Place.CAVES, s ).echo );
			assertTrue( voice( Place.MINES, s ).echo );
		}
		//the air's own, heard centred: the rumble, the far smith and chimes, the halls' hot wind
		assertSame( Source.AIR, voice( Place.CAVES, AmbientSound.RUMBLE ).from[0] );
		assertSame( Source.AIR, voice( Place.CITY, AmbientSound.ANVIL ).from[0] );
		assertSame( Source.AIR, voice( Place.CITY, AmbientSound.CHIME ).from[0] );
		assertSame( Source.AIR, voice( Place.HALLS, AmbientSound.WIND ).from[0] );
	}

	@Test
	public void thePrisonsChainsAreTheGamesOwnLowAndSlow(){
		Voice chains = null;
		for (Voice v : AmbientSounds.voices( Place.PRISON )) if (v.sound == null) chains = v;
		assertNotNull( chains );
		assertEquals( Assets.Sounds.CHAINS, chains.takes[0] );
		assertTrue( Arrays.asList( Assets.Sounds.all ).contains( Assets.Sounds.CHAINS ) );
		assertTrue( "slowed", chains.pitchLo >= 0.6f - 1e-6f && chains.pitchHi <= 0.8f + 1e-6f );
		assertTrue( "low", chains.gain * chains.level < AmbientSound.CREAK.gain );
		assertSame( "the hanging cages first", Source.HANGING, chains.from[0] );
	}

	@Test
	public void everyVoiceIsWellFormed(){
		for (Place p : Place.values()){
			for (Voice v : all( p )){
				String what = p + " " + (v.sound != null ? v.sound : v.takes[0]);
				assertTrue( what, v.level > 0f && v.level <= 1f );
				assertTrue( what, v.pitchLo <= v.pitchHi && v.pitchLo >= 0.5f && v.pitchHi <= 2f );
				assertTrue( what, v.hours != 0 && v.seasons != 0 );
				assertTrue( what, v.from.length > 0 );
				for (int i = 0; i < v.from.length - 1; i++) assertTrue( what + ": the air only last", v.from[i] != Source.AIR );
				if (v.bed){
					assertTrue( what, v.radius > 0f && v.radius < AmbientPlayer.HEARD_CELLS );
					assertTrue( what + ": a bed starts before the last play ends",
							v.overlap > 0f && v.overlap < v.length / 2f );
					assertFalse( what + ": a bed has a source", v.airy() );
				} else {
					assertTrue( what, v.every > 0f );
					assertTrue( what, v.near >= 0f && v.near < v.far && v.far < AmbientPlayer.HEARD_CELLS );
				}
			}
		}
	}

	@Test
	public void rareThingsStayRare(){
		assertTrue( voice( Place.PRISON, AmbientSound.MOAN ).every >= 120f );
		assertTrue( voice( Place.CAVES, AmbientSound.RUMBLE ).every >= 60f );
		assertTrue( voice( Place.CITY, AmbientSound.CHIME ).every >= 60f );
		assertTrue( voice( Place.CITY, AmbientSound.ANVIL ).every >= 60f );
		//and the drips are the most often heard in the sewers
		float drip = voice( Place.SEWERS, AmbientSound.DRIP ).every;
		for (Voice v : AmbientSounds.voices( Place.SEWERS )) if (!v.bed) assertTrue( drip <= v.every );
	}

	@Test
	public void theVaultIsTheCityHushed(){
		Voice[] city = AmbientSounds.voices( Place.CITY ), vault = AmbientSounds.voices( Place.VAULT );
		assertEquals( city.length, vault.length );
		for (int i = 0; i < city.length; i++){
			assertSame( city[i].sound, vault[i].sound );
			assertEquals( city[i].bed, vault[i].bed );
			assertEquals( "half as often", city[i].every * 2f, vault[i].every, 1e-4f );
			assertTrue( "quieter", vault[i].level < city[i].level );
			assertEquals( city[i].hours, vault[i].hours );
			assertEquals( city[i].radius, vault[i].radius, 0f );
			assertArrayEquals( city[i].from, vault[i].from );
		}
	}

	// ------------------------------------------------------------ the hours, seasons, weather

	@Test
	public void nightVoicesNeverSoundByDay(){
		int checked = 0;
		for (Place p : Place.values()){
			for (Voice v : AmbientSounds.voices( p )){
				if ((v.hours & (1 << Phase.DAY.ordinal())) != 0) continue;
				checked++;
				for (Season s : Season.values()){
					for (Biome b : new Biome[]{ null, Biome.PLAINS, Biome.FOREST }){
						Soundscape.Air a = outside( Phase.DAY, s, b );
						assertFalse( p + " " + v.sound + " by day", v.allowed( a ) );
						a.phase = Phase.DAWN;
						if ((v.hours & (1 << Phase.DAWN.ordinal())) == 0) assertFalse( v.allowed( a ) );
					}
				}
			}
		}
		assertTrue( "frogs, crickets", checked >= 8 );
		//the sewers' frogs at dusk and by night; their crickets only by night
		Voice frog = voice( Place.SEWERS, AmbientSound.FROG ), cricket = voice( Place.SEWERS, AmbientSound.CRICKET );
		assertTrue( frog.allowed( air( Phase.DUSK, Season.SUMMER ) ) );
		assertTrue( frog.allowed( air( Phase.NIGHT, Season.WINTER ) ) );
		assertFalse( cricket.allowed( air( Phase.DUSK, Season.SUMMER ) ) );
		assertTrue( cricket.allowed( air( Phase.NIGHT, Season.SUMMER ) ) );
		//and the fly keeps the day
		Voice fly = voice( Place.SEWERS, AmbientSound.FLY );
		assertTrue( fly.allowed( air( Phase.DAY, Season.SUMMER ) ) );
		assertFalse( fly.allowed( air( Phase.NIGHT, Season.SUMMER ) ) );
		//the drips know no hour
		Voice drip = voice( Place.SEWERS, AmbientSound.DRIP );
		for (Phase ph : Phase.values()) assertTrue( drip.allowed( air( ph, Season.WINTER ) ) );
	}

	@Test
	public void winterHushesTheSurfacesFrogsAndCrickets(){
		Voice frog = voice( Place.SURFACE, AmbientSound.FROG ), cricket = voice( Place.SURFACE, AmbientSound.CRICKET );
		for (Season s : Season.values()){
			Soundscape.Air a = outside( Phase.NIGHT, s, Biome.MEADOW );
			a.temp = 18f;
			assertEquals( "frogs in " + s, s != Season.WINTER, frog.allowed( a ) );
			assertEquals( "crickets in " + s, s != Season.WINTER, cricket.allowed( a ) );
		}
		//and so does a cold night in a warm season
		Soundscape.Air cold = outside( Phase.NIGHT, Season.AUTUMN, Biome.MEADOW );
		cold.temp = 3f;
		assertFalse( frog.allowed( cold ) );
		assertFalse( cricket.allowed( cold ) );
		cold.temp = 8f;
		assertTrue( "frogs from five degrees", frog.allowed( cold ) );
		assertFalse( "crickets from ten", cricket.allowed( cold ) );
	}

	@Test
	public void stormsAndRainHushTheBirdsAndCrickets(){
		Voice bird = voice( Place.SURFACE, AmbientSound.BIRD ), dove = voice( Place.SURFACE, AmbientSound.COO ),
				cricket = voice( Place.SURFACE, AmbientSound.CRICKET ), peak = voice( Place.PEAKS, AmbientSound.BIRD );
		Soundscape.Air day = outside( Phase.DAY, Season.SPRING, Biome.FOREST );
		Soundscape.Air night = outside( Phase.NIGHT, Season.SUMMER, Biome.PLAINS );
		night.temp = 20f;
		assertTrue( bird.allowed( day ) && dove.allowed( day ) && peak.allowed( day ) );
		assertTrue( cricket.allowed( night ) );
		day.storm = night.storm = true;
		assertFalse( bird.allowed( day ) || dove.allowed( day ) || peak.allowed( day ) );
		assertFalse( cricket.allowed( night ) );
		day.storm = night.storm = false;
		day.rain = night.rain = 0.3f;
		assertFalse( "the rain too", bird.allowed( day ) || dove.allowed( day ) || peak.allowed( day ) );
		assertFalse( cricket.allowed( night ) );
		//the water's lapping and the wind are not hushed by the weather
		Voice lap = voice( Place.SURFACE, AmbientSound.LAP );
		assertTrue( lap.allowed( day ) );
	}

	@Test
	public void birdsKeepTheWoodsTheMeadowsAndThePlainsByDay(){
		Voice bird = voice( Place.SURFACE, AmbientSound.BIRD ), dove = voice( Place.SURFACE, AmbientSound.COO );
		EnumSet<Biome> home = EnumSet.of( Biome.FOREST, Biome.MEADOW, Biome.PLAINS );
		for (Biome b : Biome.values()){
			for (Phase p : Phase.values()){
				boolean want = home.contains( b ) && (p == Phase.DAWN || p == Phase.DAY);
				assertEquals( b + " " + p, want, bird.allowed( outside( p, Season.SUMMER, b ) ) );
				assertEquals( b + " " + p, want, dove.allowed( outside( p, Season.SUMMER, b ) ) );
			}
		}
	}

	@Test
	public void frogsKeepTheNightButInTheSwampsAndTheRain(){
		Voice frog = voice( Place.SURFACE, AmbientSound.FROG );
		assertFalse( "not by day on the plains", frog.allowed( outside( Phase.DAY, Season.SUMMER, Biome.PLAINS ) ) );
		assertTrue( "by night", frog.allowed( outside( Phase.NIGHT, Season.SUMMER, Biome.PLAINS ) ) );
		assertTrue( "day and night in the swamps", frog.allowed( outside( Phase.DAY, Season.SUMMER, Biome.SWAMP ) ) );
		Soundscape.Air rain = outside( Phase.DAY, Season.SPRING, Biome.FOREST );
		rain.rain = 0.2f;
		assertTrue( "out in the rain", frog.allowed( rain ) );
		rain.storm = true;
		assertTrue( "a storm is rain too", frog.allowed( rain ) );
		assertFalse( "but never in winter", frog.allowed( outside( Phase.DAY, Season.WINTER, Biome.SWAMP ) ) );
	}

	@Test
	public void theWindBlowsOnOpenGroundWhenItBlows(){
		Voice wind = voice( Place.SURFACE, AmbientSound.WIND );
		EnumSet<Biome> open = EnumSet.of( Biome.PLAINS, Biome.TUNDRA, Biome.SNOWFIELD, Biome.DESERT, Biome.BEACH );
		for (Biome b : Biome.values()){
			Soundscape.Air a = outside( Phase.DAY, Season.AUTUMN, b );
			a.wind = 2f;
			assertFalse( "a calm in " + b, wind.allowed( a ) );
			a.wind = 6f;
			assertEquals( b.toString(), open.contains( b ), wind.allowed( a ) );
			//from a gust's wind the weather's own wind takes over (WeatherSounds: the gusts and
			//the gale, from the wind's side), never the place's from the air as well
			a.wind = WeatherScape.GUST_WIND;
			assertFalse( "the weather's wind in " + b, wind.allowed( a ) );
		}
		//louder and oftener the harder it blows, and bounded
		float lastLevel = 0f, lastRate = 0f;
		for (float s = 0f; s <= 30f; s += 0.5f){
			float l = Voice.windLevel( s ), r = Voice.windRate( s );
			assertTrue( l >= lastLevel && r >= lastRate );
			assertTrue( l > 0f && l <= 1f && r >= 1f && r <= 3f );
			lastLevel = l;
			lastRate = r;
		}
		assertEquals( 1f, Voice.windLevel( Voice.GALE ), 1e-6f );
		//the peaks' wind never stops, a breeze there still half heard
		Voice peaks = voice( Place.PEAKS, AmbientSound.WIND );
		Soundscape.Air still = air( Phase.NIGHT, Season.WINTER );
		assertTrue( peaks.allowed( still ) );
		assertEquals( 0.5f, peaks.loudness( still ), 1e-6f );
		still.wind = 6.5f;
		assertTrue( peaks.allowed( still ) );
		still.wind = 20f;
		assertEquals( 1f, peaks.loudness( still ), 1e-6f );
		assertEquals( 3f, peaks.rate( still ), 1e-6f );
		//but from a gust's wind it is the weather's
		assertFalse( peaks.allowed( still ) );
		still.wind = WeatherScape.GUST_WIND;
		assertFalse( peaks.allowed( still ) );
	}

	@Test
	public void birdsongThroughTheGratesOnlyUnderTheTownByDay(){
		Voice bird = voice( Place.SEWERS, AmbientSound.BIRD );
		Soundscape.Air a = air( Phase.DAY, Season.SUMMER );
		for (int depth = 1; depth <= 4; depth++){
			a.depth = depth;
			a.branch = 0;
			assertEquals( "floor " + depth, depth <= 2, bird.allowed( a ) );
			//a barrow's sewers lie under no town
			a.branch = 100;
			assertFalse( bird.allowed( a ) );
		}
		a.depth = 1;
		a.branch = 0;
		a.phase = Phase.NIGHT;
		assertFalse( bird.allowed( a ) );
	}

	// ------------------------------------------------------------ the sounder

	private static boolean held;

	//a bare floor of a place: rock round the edge, a pool, a wall over it with a pipe
	private static Level floor( final Place place ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public Place ambience(){ return place; }
			@Override public boolean fogHeld(){ return held; }
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
			l.map[i + 10 * 30] = Terrain.WALL;
		}
		l.map[12 + 10 * 30] = Terrain.WALL_DECO;
		for (int y = 11; y <= 14; y++) for (int x = 1; x < 29; x++) l.map[x + y * 30] = Terrain.WATER;
		return l;
	}

	@Test
	public void theFloorsWithAPlaceGetASounder(){
		assertNull( AmbientSounds.forLevel( null ) );
		assertNull( "a boss floor is quiet", AmbientSounds.forLevel( floor( null ) ) );
		assertNull( "the cave slices are CaveLife's", AmbientSounds.forLevel( floor( Place.CAVE_SLICES ) ) );
		for (Place p : Place.values()){
			if (p == Place.CAVE_SLICES || p == Place.SURFACE || p == Place.PEAKS) continue;
			AmbientSounds s = AmbientSounds.forLevel( floor( p ) );
			assertNotNull( p.toString(), s );
			assertEquals( AmbientSounds.voices( p ).length, s.scape.voices.length );
		}
	}

	@Test
	public void silentOffItsFloorWithoutAHeroOrTheChannel(){
		held = false;
		Level sewer = floor( Place.SEWERS );
		Dungeon.level = sewer;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 12 + 16 * 30;
		AmbientSounds sounds = AmbientSounds.forLevel( sewer );
		final ArrayList<Voice> heard = new ArrayList<>();
		sounds.out = (v, take, cell, level, pitch, echo) -> heard.add( v );
		Game.elapsed = 0.05f;

		for (int f = 0; f < 1200; f++) sounds.update();
		assertFalse( "heard on its floor", heard.isEmpty() );

		//another floor is current (the scene is changing): nothing, not even its clock
		Dungeon.level = floor( Place.SEWERS );
		heard.clear();
		float clock = sounds.scape.now;
		for (int f = 0; f < 1200; f++) sounds.update();
		assertTrue( heard.isEmpty() );
		assertEquals( clock, sounds.scape.now, 0f );
		Dungeon.level = sewer;

		//the hero dead
		Dungeon.hero.HP = 0;
		for (int f = 0; f < 1200; f++) sounds.update();
		assertTrue( heard.isEmpty() );
		Dungeon.hero.HP = Dungeon.hero.HT;

		//the ambience off
		Sample.INSTANCE.ambientEnable( false );
		for (int f = 0; f < 1200; f++) sounds.update();
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
		for (int f = 0; f < 1200; f++) sounds.update();
		assertFalse( heard.isEmpty() );
	}

	@Test
	public void theSecretRoomsAreMaskedRimAndAll(){
		Room secret = new SecretGardenRoom();
		secret.set( 2, 3, 6, 7 );
		Room plain = new EmptyRoom();
		plain.set( 8, 3, 12, 7 );
		boolean[] mask = AmbientSounds.secretCells( Arrays.asList( plain, secret ), 20, 12 );
		assertNotNull( mask );
		for (int y = 0; y < 12; y++){
			for (int x = 0; x < 20; x++){
				assertEquals( x + "," + y, x >= 2 && x <= 6 && y >= 3 && y <= 7, mask[x + y * 20] );
			}
		}
		assertNull( "no secret room, no mask", AmbientSounds.secretCells( Arrays.<Room>asList( plain ), 20, 12 ) );
	}
}
