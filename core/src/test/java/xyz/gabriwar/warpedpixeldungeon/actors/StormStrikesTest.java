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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.StormCloud;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.TownChurchLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
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
 * A storm's ground bolts (StormStrikes): decided on the turn, only in a storm and only under the
 * open sky, around the hero rather than on him; a hit shocks and stuns what is not immune, and
 * runs on as the wand of lightning's zap does (StormStrikes.discharge: a cell from whoever it hit,
 * two from one in water, a hero only from beside, the blow shared as the wand's, whole only in
 * standing water; out over the water a splash), the ground it strikes is burnt at once and the
 * grass round it burns on through the storm's heavy rain; each bolt is handed to the overlay to
 * draw with where it ran, the host's to a guest's through NetVisuals.
 */
public class StormStrikesTest {

	private static final int W = 30, CENTER = 15 + 15 * W;

	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		drain();
		ClimateManager.reset();
		Random.pushGenerator( 4242 );
		//a storm's heavy rain
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.6f;
		ClimateManager.debugWindOverride = 15f;
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		drain();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
		ClimateManager.debugPrecipTypeOverride = null;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugForceStorm = false;
		ClimateManager.reset();
	}

	private static int drain(){
		int n = 0;
		while (StormStrikes.next() != null) n++;
		return n;
	}

	//a level of grass with rock round the edge, the hero in the middle of it
	private static <L extends Level> L grass( L l ){
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.GRASS );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = CENTER;
		return l;
	}

	//bolts brought down over this many turns on the level
	private static int bolts( Level l, int turns ){
		grass( l );
		int n = 0;
		for (int t = 0; t < turns; t++){
			StormStrikes.onHeroTurn();
			StormStrikes.Bolt b;
			while ((b = StormStrikes.next()) != null){
				assertSame( l, b.level );
				assertTrue( "within reach of the hero", l.distance( CENTER, b.cell ) <= StormStrikes.REACH );
				n++;
			}
			//the fires the bolts lit are not this test's business
			l.blobs.clear();
			Actor.clear();
		}
		return n;
	}

	@Test
	public void boltsComeOnlyInAStormUnderTheOpenSky(){
		assertEquals( "no storm, no bolts", 0, bolts( new OverworldLevel( 0 ), 300 ) );

		ClimateManager.debugForceStorm = true;
		//a full storm (fury 1): a bolt every third turn or so
		int surface = bolts( new OverworldLevel( 0 ), 300 );
		assertTrue( "the surface in a storm: " + surface, surface > 70 && surface < 140 );
		assertTrue( "the peaks", bolts( new OverworldLevel( 4 ), 300 ) > 70 );
		assertEquals( "the overworld's caves", 0, bolts( new OverworldLevel( -3 ), 300 ) );
		assertEquals( "indoors", 0, bolts( new TownChurchLevel(), 300 ) );
		assertEquals( "a dungeon floor the rain reaches", 0, bolts( new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public int climateDepth(){ return 2; }
		}, 300 ) );

		//a mild storm strikes less often
		ClimateManager.debugPrecipOverride = 0.1f;
		ClimateManager.debugWindOverride = 2f;
		int mild = bolts( new OverworldLevel( 0 ), 300 );
		assertTrue( "a mild storm: " + mild, mild > 15 && mild < surface - 30 );
	}

	@Test
	public void aBoltLandsAroundTheHeroAndRarelyOnHim(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		//a tree beside him and a house's doorway: never struck, the ground is
		Level.set( CENTER + 1, Terrain.TREE_OAK, l );
		Level.set( CENTER - 1, Terrain.OPEN_DOOR, l );
		int onHim = 0, tries = 20000;
		for (int i = 0; i < tries; i++){
			int c = StormStrikes.pick( l, CENTER );
			assertTrue( c != -1 );
			assertTrue( !l.solid[c] && c != CENTER - 1 );
			assertTrue( l.distance( CENTER, c ) <= StormStrikes.REACH );
			if (c == CENTER) onHim++;
		}
		assertTrue( "on the hero " + onHim + " times in " + tries, onHim < tries / 200 );
	}

	private static Char target( final boolean immune ){
		Char ch = new Char(){
			{
				if (immune) immunities.add( StormCloud.class );
			}
			@Override
			protected boolean act(){
				return true;
			}
		};
		ch.HP = ch.HT = 60;
		ch.pos = CENTER + 3;
		Actor.add( ch );
		return ch;
	}

	@Test
	public void aHitShocksAndStunsWhatIsNotImmune(){
		grass( new OverworldLevel( 0 ) );
		Char ch = target( false );
		StormStrikes.strike( ch.pos );
		assertTrue( "a fifth to a third of its health: " + ch.HP, ch.HP <= 60 - 12 && ch.HP >= 60 - 20 );
		assertNotNull( ch.buff( Paralysis.class ) );

		Actor.clear();
		grass( new OverworldLevel( 0 ) );
		Char spared = target( true );
		StormStrikes.strike( spared.pos );
		assertEquals( 60, spared.HP );
		assertNull( spared.buff( Paralysis.class ) );
	}

	@Test
	public void theBoltArcsOnToWhoeverStandsBesideWhatItStrikes(){
		grass( new OverworldLevel( 0 ) );
		Char struck = target( false ), near = target( false );
		near.pos = struck.pos + 1;
		StormStrikes.strike( struck.pos );
		assertTrue( "the one beside: " + near.HP, near.HP < 60 );
		assertTrue( struck.HP < 60 );
		assertNotNull( struck.buff( Paralysis.class ) );
		assertNull( "only the struck one is stunned", near.buff( Paralysis.class ) );
	}

	@Test
	public void inWaterTheBoltRunsOverItWithTheSplashTheWaterAlwaysCarried(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		int cell = CENTER + 3;
		for (int x = 0; x <= 3; x++) Level.set( cell + x, Terrain.WATER, l );
		//three steps out over the water from where it struck, nobody there: 1-3, as whoever
		//stood in the water beside a bolt always took, not the blow
		Char far = target( false );
		far.pos = cell + 3;
		StormStrikes.strike( cell );
		assertTrue( "a splash: " + far.HP, far.HP <= 60 - 1 && far.HP >= 60 - 3 );
		assertNull( far.buff( Paralysis.class ) );
	}

	@Test
	public void theBoltScorchesAndTheGrassBurnsOnThroughTheRain(){
		ClimateManager.debugForceStorm = true;
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		int cell = CENTER + 3, door = cell - W;
		//a house's open doorway beside it
		Level.set( door, Terrain.OPEN_DOOR, l );
		StormStrikes.strike( cell );
		assertEquals( "burnt at once", Terrain.EMBERS, l.map[cell] );
		Fire fire = (Fire) l.blobs.get( Fire.class );
		assertNotNull( fire );
		for (int n : PathFinder.NEIGHBOURS4){
			if (cell + n == door) assertEquals( "never the doorway", 0, fire.cur[door] );
			else assertTrue( "the grass round it catches", fire.cur[cell + n] > 0 );
		}
		//the turns go by in the storm's heavy rain: the fire burns down to embers
		for (int t = 0; t < 4; t++) fire.act();
		for (int n : PathFinder.NEIGHBOURS4){
			if (cell + n != door) assertEquals( "the grass beside burnt", Terrain.EMBERS, l.map[cell + n] );
		}

		//handed to the overlay to draw
		StormStrikes.Bolt b = StormStrikes.next();
		assertNotNull( b );
		assertSame( l, b.level );
		assertEquals( cell, b.cell );
		assertFalse( "the sky's", b.cloud );
		assertNull( StormStrikes.next() );
	}

	@Test
	public void aStormCloudsBoltIsHandedToTheOverlayItsBlowDealt() throws Exception {
		//a dungeon floor: no sprite is made on the actor thread, the overlay draws it (there is no
		//hero sprite here to hang one on)
		Level l = grass( new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		} );
		Char ch = target( false );
		Method strike = StormCloud.class.getDeclaredMethod( "strike", int.class );
		strike.setAccessible( true );
		strike.invoke( new StormCloud(), ch.pos );
		//the blow as it always was: a shock and a stun, and the grass under it alight
		assertTrue( "2-4 and a third of the depth: " + ch.HP, ch.HP < 60 && ch.HP >= 60 - 4 - Dungeon.scalingDepth() / 3 );
		assertNotNull( ch.buff( Paralysis.class ) );
		Fire fire = (Fire) l.blobs.get( Fire.class );
		assertNotNull( fire );
		assertTrue( fire.cur[ch.pos] > 0 );
		//and handed over to be drawn as the storm's own
		StormStrikes.Bolt b = StormStrikes.next();
		assertNotNull( b );
		assertSame( l, b.level );
		assertEquals( ch.pos, b.cell );
		assertTrue( "a cloud's", b.cloud );
		assertNull( StormStrikes.next() );
	}

	@Test
	public void aStormCloudsBoltOnTheWorldHitsByShareAsTheSkys() throws Exception {
		//the world's slices are numbered past the dungeon's floors (97 the surface, 96-87 the
		//peaks, 101-112 the caves): by depth a cloud's bolt there was a blow of 31-41
		Method strike = StormCloud.class.getDeclaredMethod( "strike", int.class );
		strike.setAccessible( true );
		int depth = Dungeon.depth;
		try {
			for (int altitude : new int[]{ 0, 4, -3 }){
				Actor.clear();
				grass( new OverworldLevel( altitude ) );
				Dungeon.depth = WorldLayers.depthOf( altitude );
				Char ch = target( false );
				strike.invoke( new StormCloud(), ch.pos );
				assertTrue( "altitude " + altitude + ", a fifth to a third of its health: " + ch.HP,
						ch.HP <= 60 - 12 && ch.HP >= 60 - 20 );
				assertNotNull( ch.buff( Paralysis.class ) );
				drain();
			}
		} finally {
			Dungeon.depth = depth;
		}
	}

	@Test
	public void aBoltSetsNoOneAlight(){
		//the surface's burning (harmDepth 97) would be a killing one: a bolt on someone lights
		//nothing under him or round him...
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		int cell = CENTER + 3;
		Char struck = target( false );
		StormStrikes.strike( cell );
		Fire fire = (Fire) l.blobs.get( Fire.class );
		if (fire != null){
			for (int n : PathFinder.NEIGHBOURS9) assertEquals( 0, fire.cur[cell + n] );
			fire.act();
		}
		assertNull( struck.buff( Burning.class ) );

		//...nor beside one standing near
		Actor.clear();
		l = grass( new OverworldLevel( 0 ) );
		Char near = target( false );
		near.pos = cell + 2;
		StormStrikes.strike( cell );
		fire = (Fire) l.blobs.get( Fire.class );
		assertNotNull( fire );
		assertEquals( 0, fire.cur[cell + 1] );
		assertTrue( "the grass round it with no one by catches", fire.cur[cell - 1] > 0 );
		fire.act();
		assertNull( near.buff( Burning.class ) );
	}

	@Test
	public void theFireItLightsKeepsOffTheDoors(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		int cell = CENTER + 3, door = cell + W + 1;
		Level.set( door, Terrain.DOOR, l );
		StormStrikes.strike( cell );
		Fire fire = (Fire) l.blobs.get( Fire.class );
		assertNotNull( fire );
		//the fire spreads by the four sides: none lit where it could reach a door
		assertEquals( 0, fire.cur[cell + 1] );
		assertEquals( 0, fire.cur[cell + W] );
		assertTrue( fire.cur[cell - 1] > 0 && fire.cur[cell - W] > 0 );
	}

	@Test
	public void nothingBurnsInTheTown(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		//the window over the town's middle
		l.worldX = WorldStructures.townWorldX( 0 ) + 1;
		l.worldY = WorldStructures.townWorldY( 0 ) + 1;
		int cell = CENTER + 3;
		for (int n : PathFinder.NEIGHBOURS9) assertTrue( l.inTown( cell + n ) );
		Char ch = target( false );
		StormStrikes.strike( cell );
		assertTrue( "the bolt still shocks", ch.HP < 60 );
		assertEquals( "not scorched", Terrain.GRASS, l.map[cell] );
		Fire fire = (Fire) l.blobs.get( Fire.class );
		if (fire != null) for (int n : PathFinder.NEIGHBOURS9) assertEquals( 0, fire.cur[cell + n] );
	}

	@Test
	public void boltsFallAroundAGuestToo() throws Exception {
		ClimateManager.debugForceStorm = true;
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		//the host in one corner, a co-op guest in the far one: out of each other's reach
		Dungeon.hero.pos = 1 + W;
		Hero guest = new Hero();
		guest.pos = W - 2 + (W - 2) * W;
		assertTrue( l.distance( Dungeon.hero.pos, guest.pos ) > 2 * StormStrikes.REACH );
		TestParty.join( guest );
		try {
			int byGuest = 0;
			for (int t = 0; t < 300; t++){
				StormStrikes.onHeroTurn();
				StormStrikes.Bolt b;
				while ((b = StormStrikes.next()) != null){
					if (l.distance( guest.pos, b.cell ) <= StormStrikes.REACH) byGuest++;
				}
				l.blobs.clear();
			}
			assertTrue( "round the guest: " + byGuest, byGuest > 30 );
		} finally {
			TestParty.leave();
			NetVisuals.clear();
		}
	}

	@Test
	public void aGuestDrawsTheHostsBolts() throws Exception {
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		JSONObject evt = new JSONObject();
		evt.put( "t", "sk" );
		evt.put( "c", CENTER + 2 );
		JSONObject outside = new JSONObject();
		outside.put( "t", "sk" );
		outside.put( "c", W * W + 5 );
		//and a storm cloud's, drawn the same way
		JSONObject cloud = new JSONObject();
		cloud.put( "t", "sk" );
		cloud.put( "c", CENTER - 2 );
		cloud.put( "k", true );
		NetVisuals.replay( new JSONArray().put( evt ).put( outside ).put( cloud ) );
		StormStrikes.Bolt b = StormStrikes.next();
		assertNotNull( b );
		assertSame( l, b.level );
		assertEquals( CENTER + 2, b.cell );
		assertFalse( b.cloud );
		b = StormStrikes.next();
		//a cell off the level is dropped: the next is the cloud's
		assertNotNull( b );
		assertEquals( CENTER - 2, b.cell );
		assertTrue( b.cloud );
		assertNull( StormStrikes.next() );
		//and the guest's game strikes nothing of its own: only where the turns run
		assertEquals( l.map[CENTER + 2], Terrain.GRASS );
	}

	// ------------------------------------------------------------ where a bolt runs

	private static int at( int x, int y ){
		return x + y * W;
	}

	private static void wet( Level l, int... cells ){
		for (int c : cells) Level.set( c, Terrain.WATER, l );
	}

	//one who tells what he took, and how many times
	private static class Taker extends Char {
		int took, hits;

		Taker( int pos ){
			this.pos = pos;
			HP = HT = 60;
			Actor.add( this );
		}

		@Override
		protected boolean act(){
			return true;
		}

		@Override
		public void damage( int dmg, Object src ){
			took += dmg;
			hits++;
		}
	}

	//a hero who tells what he took
	private static class HeroTaker extends Hero {
		int took;

		HeroTaker( int pos ){
			this.pos = pos;
			Actor.add( this );
		}

		@Override
		public void damage( int dmg, Object src ){
			took += dmg;
		}
	}

	//a bolt's blow of 100 on the cell (StormCloud.jolt), shared out among whom it hits
	private static StormStrikes.Discharge jolt( int cell ){
		return StormCloud.jolt( cell, new StormCloud(), ch -> 100 );
	}

	@Test
	public void theBlowIsSharedAsTheWandsAndOnlyTheStruckOneIsStunned(){
		grass( new OverworldLevel( 0 ) );
		Taker alone = new Taker( at( 18, 15 ) );
		jolt( alone.pos );
		assertEquals( "all of it", 100, alone.took );
		assertNotNull( alone.buff( Paralysis.class ) );

		Actor.clear();
		Taker struck = new Taker( at( 18, 15 ) ), beside = new Taker( at( 19, 15 ) ), far = new Taker( at( 21, 15 ) );
		StormStrikes.Discharge d = jolt( struck.pos );
		//0.4 + 0.6 / 2
		assertEquals( 70, struck.took );
		assertEquals( 70, beside.took );
		assertEquals( "a cell from the nearest it hit, dry", 0, far.took );
		assertNotNull( struck.buff( Paralysis.class ) );
		assertNull( beside.buff( Paralysis.class ) );
		assertEquals( Arrays.asList( struck, beside ), d.hit );
		assertTrue( Arrays.equals( new int[]{ struck.pos, beside.pos }, d.arcs ) );
		assertEquals( 0, d.water.length );

		Actor.clear();
		struck = new Taker( at( 18, 15 ) );
		beside = new Taker( at( 19, 15 ) );
		Taker under = new Taker( at( 18, 16 ) );
		jolt( struck.pos );
		for (Taker t : new Taker[]{ struck, beside, under }) assertEquals( 60, t.took );
	}

	@Test
	public void fromWaterItArcsTwoCellsAndAllTakeTheWhole(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		wet( l, at( 18, 15 ) );
		Taker struck = new Taker( at( 18, 15 ) ), two = new Taker( at( 20, 15 ) ), far = new Taker( at( 22, 15 ) );
		jolt( struck.pos );
		assertEquals( 100, struck.took );
		assertEquals( 100, two.took );
		assertEquals( "two stands dry: one cell on from him", 0, far.took );

		//one flying over the water is not in it: a cell's reach, though it struck water
		Actor.clear();
		struck = new Taker( at( 18, 15 ) );
		struck.flying = true;
		two = new Taker( at( 20, 15 ) );
		jolt( struck.pos );
		assertEquals( 100, struck.took );
		assertEquals( 0, two.took );
	}

	@Test
	public void itRunsOverTheWaterThreeStepsIntoWhoeverStandsInIt(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		for (int x = 16; x <= 24; x++) wet( l, at( x, 15 ) );
		int cell = at( 16, 15 );

		//nobody where it struck: three steps out he is hit, by the water's splash of 1-3, none of
		//the blow (it is no arc of the wand's, sharing it)
		Taker three = new Taker( at( 19, 15 ) );
		StormStrikes.Discharge d = jolt( cell );
		assertEquals( 1, three.hits );
		assertTrue( "a splash: " + three.took, three.took >= 1 && three.took <= 3 );
		assertTrue( d.hit.isEmpty() );
		assertEquals( Arrays.asList( three ), d.wet );
		assertTrue( Arrays.equals( new int[]{ at( 18, 15 ), at( 19, 15 ) }, d.arcs ) );
		assertTrue( Arrays.equals( new int[]{ cell, at( 17, 15 ), at( 17, 15 ), at( 18, 15 ) }, d.water ) );

		//four steps out, not
		Actor.clear();
		Taker four = new Taker( at( 20, 15 ) );
		jolt( cell );
		assertEquals( 0, four.took );

		//one flying over it is not in it: it runs on under him
		Actor.clear();
		Taker flier = new Taker( at( 18, 15 ) );
		flier.flying = true;
		d = jolt( cell );
		assertEquals( 0, flier.took );
		assertEquals( 0, d.hit.size() + d.wet.size() );
		assertTrue( Arrays.equals( new int[]{ cell, at( 17, 15 ), at( 17, 15 ), at( 18, 15 ), at( 18, 15 ), at( 19, 15 ) }, d.water ) );
	}

	@Test
	public void aBoltBesideTheWaterRunsIntoItWithTheSplashOfOneToThree(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		wet( l, at( 19, 15 ) );
		Taker wading = new Taker( at( 19, 15 ) );
		//struck dry ground, nobody there
		jolt( at( 18, 15 ) );
		assertTrue( "1-3 as ever, not the blow: " + wading.took, wading.took >= 1 && wading.took <= 3 );
		assertNull( wading.buff( Paralysis.class ) );

		//and one struck beside him takes the whole of it, alone in the share: the water's splash
		//takes none of it
		Actor.clear();
		Taker struck = new Taker( at( 18, 15 ) );
		wading = new Taker( at( 20, 15 ) );
		wet( l, at( 20, 15 ) );
		jolt( struck.pos );
		assertEquals( 100, struck.took );
		assertTrue( wading.took >= 1 && wading.took <= 3 );

		//and with nobody by, dry, it runs nowhere
		Actor.clear();
		StormStrikes.Discharge d = jolt( at( 12, 15 ) );
		assertTrue( d.hit.isEmpty() );
		assertEquals( 0, d.arcs.length + d.water.length );
	}

	@Test
	public void theCrawlIsBoundedEvenInALake(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		for (int y = 1; y < W - 1; y++) for (int x = 1; x < W - 1; x++) l.map[at( x, y )] = Terrain.WATER;
		l.buildFlagMaps();
		StormStrikes.Discharge d = jolt( CENTER );
		assertTrue( d.full );
		assertEquals( 0, d.arcs.length );
		//every cell of its 7x7 window, each once, each from one a step nearer
		assertEquals( 2 * 48, d.water.length );
		HashSet<Integer> reached = new HashSet<>();
		for (int i = 0; i < d.water.length; i += 2){
			int from = d.water[i], to = d.water[i + 1];
			assertTrue( l.water[to] );
			assertTrue( reached.add( to ) );
			assertEquals( 1, l.distance( from, to ) );
			assertEquals( l.distance( CENTER, to ) - 1, l.distance( CENTER, from ) );
			assertTrue( l.distance( CENTER, to ) <= 3 );
		}
	}

	@Test
	public void theHerosSideIsOnlyReachedFromBesideOneItHit() throws Exception {
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		wet( l, at( 18, 15 ) );
		Taker struck = new Taker( at( 18, 15 ) );
		HeroTaker hero = new HeroTaker( at( 20, 15 ) );
		Dungeon.hero = hero;
		Hero guest = new HeroTaker( at( 18, 17 ) );
		TestParty.join( guest );
		Taker pet = new Taker( at( 16, 15 ) );
		pet.alignment = Char.Alignment.ALLY;
		try {
			//two from one in water: a foe would be hit, the host, his guest and his pet are not
			jolt( struck.pos );
			assertEquals( 100, struck.took );
			assertEquals( 0, hero.took );
			assertEquals( 0, ((HeroTaker) guest).took );
			assertEquals( 0, pet.took );
			//beside him, they are
			hero.pos = at( 19, 15 );
			pet.pos = at( 17, 15 );
			jolt( struck.pos );
			assertEquals( 100, hero.took );
			assertEquals( 100, pet.took );

			//the water never runs into them: it flows round them, though a foe there is hit
			Actor.clear();
			for (int x = 16; x <= 22; x++) wet( l, at( x, 15 ) );
			hero = new HeroTaker( at( 17, 15 ) );
			Dungeon.hero = hero;
			Taker foe = new Taker( at( 19, 15 ) );
			StormStrikes.Discharge d = jolt( at( 16, 15 ) );
			assertEquals( 0, hero.took );
			assertEquals( "round him, not through: he stands in a channel one cell wide", 0, foe.took );
			for (int i = 1; i < d.water.length; i += 2) assertTrue( d.water[i] != hero.pos );
			hero.pos = at( 20, 14 );
			jolt( at( 16, 15 ) );
			assertTrue( "the water's splash: " + foe.took, foe.took >= 1 && foe.took <= 3 );
			assertEquals( 0, hero.took );
		} finally {
			TestParty.leave();
			NetVisuals.clear();
		}
	}

	@Test
	public void whomTheWaterReachesPassItOnNoFurther(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		for (int x = 16; x <= 19; x++) wet( l, at( x, 15 ) );
		//nobody where it struck: the water runs into one two steps out, who passes it on to no one
		Taker wading = new Taker( at( 18, 15 ) ), dry = new Taker( at( 20, 16 ) );
		jolt( at( 16, 15 ) );
		assertTrue( wading.took >= 1 && wading.took <= 3 );
		assertEquals( 0, dry.took );

		//one struck there arcs to him, two cells through the water, and he on to the next
		Actor.clear();
		Taker struck = new Taker( at( 16, 15 ) );
		wading = new Taker( at( 18, 15 ) );
		dry = new Taker( at( 20, 16 ) );
		StormStrikes.Discharge d = jolt( struck.pos );
		for (Taker t : new Taker[]{ struck, wading, dry }) assertEquals( 100, t.took );
		assertTrue( Arrays.equals( new int[]{ struck.pos, wading.pos, wading.pos, dry.pos }, d.arcs ) );
	}

	@Test
	public void theImmuneAndTheTownsPeopleNeitherTakeNorPassIt(){
		grass( new OverworldLevel( 0 ) );
		Taker struck = new Taker( at( 18, 15 ) ), past = new Taker( at( 20, 15 ) );
		Char immune = target( true );
		immune.pos = at( 19, 15 );
		jolt( struck.pos );
		assertEquals( "alone: the immune one is not counted", 100, struck.took );
		assertEquals( 60, immune.HP );
		assertEquals( 0, past.took );

		Actor.clear();
		struck = new Taker( at( 18, 15 ) );
		final int[] npcTook = { 0 };
		NPC villager = new NPC(){
			@Override
			public void damage( int dmg, Object src ){
				npcTook[0] += dmg;
			}
		};
		villager.pos = at( 19, 15 );
		Actor.add( villager );
		past = new Taker( at( 20, 15 ) );
		jolt( struck.pos );
		assertEquals( 100, struck.took );
		assertEquals( 0, npcTook[0] );
		assertEquals( 0, past.took );

		//nor an immune one struck: on a dry cell it runs nowhere
		Actor.clear();
		immune = target( true );
		Taker beside = new Taker( immune.pos + 1 );
		StormStrikes.Discharge d = jolt( immune.pos );
		assertEquals( 60, immune.HP );
		assertEquals( 0, beside.took );
		assertTrue( d.hit.isEmpty() );
	}

	@Test
	public void eachIsHitOnce(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		wet( l, at( 18, 15 ), at( 19, 15 ), at( 18, 16 ), at( 19, 16 ) );
		Taker[] all = { new Taker( at( 18, 15 ) ), new Taker( at( 19, 15 ) ), new Taker( at( 18, 16 ) ), new Taker( at( 19, 16 ) ) };
		StormStrikes.Discharge d = jolt( all[0].pos );
		for (Taker t : all){
			assertEquals( 1, t.hits );
			assertEquals( 100, t.took );
		}
		assertEquals( 4, d.hit.size() );
	}

	@Test
	public void theSkysBoltCarriesWhereItRan(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		Taker struck = new Taker( at( 18, 15 ) ), beside = new Taker( at( 19, 15 ) );
		StormStrikes.strike( struck.pos );
		assertTrue( "a fifth to a third, 0.7 of it: " + struck.took, struck.took >= 8 && struck.took <= 14 );
		assertEquals( struck.took > 0, beside.took > 0 );
		StormStrikes.Bolt b = StormStrikes.next();
		assertNotNull( b );
		assertTrue( Arrays.equals( new int[]{ struck.pos, beside.pos }, b.arcs ) );
		assertEquals( 0, b.water.length );
		assertNull( StormStrikes.next() );

		//over the water, its crawl
		Actor.clear();
		wet( l, at( 10, 10 ), at( 11, 10 ) );
		StormStrikes.strike( at( 10, 10 ) );
		b = StormStrikes.next();
		assertEquals( 0, b.arcs.length );
		assertTrue( Arrays.equals( new int[]{ at( 10, 10 ), at( 11, 10 ) }, b.water ) );

		//and a bolt that ran nowhere carries nothing
		StormStrikes.strike( at( 5, 20 ) );
		b = StormStrikes.next();
		assertEquals( 0, b.arcs.length + b.water.length );
	}

	@Test
	public void aStormCloudsBoltArcsToo() throws Exception {
		Level l = grass( new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		} );
		Taker struck = new Taker( at( 18, 15 ) ), beside = new Taker( at( 19, 15 ) );
		Method strike = StormCloud.class.getDeclaredMethod( "strike", int.class );
		strike.setAccessible( true );
		strike.invoke( new StormCloud(), struck.pos );
		//the floor's 2-4 + a third of the depth, 0.7 of it each
		int most = Math.round( (4 + Dungeon.scalingDepth() / 3) * 0.7f );
		for (Taker t : new Taker[]{ struck, beside }) assertTrue( "took " + t.took, t.took >= 1 && t.took <= most );
		assertNotNull( struck.buff( Paralysis.class ) );
		assertNull( beside.buff( Paralysis.class ) );
		StormStrikes.Bolt b = StormStrikes.next();
		assertTrue( b.cloud );
		assertTrue( Arrays.equals( new int[]{ struck.pos, beside.pos }, b.arcs ) );
		assertNotNull( (Fire) l.blobs.get( Fire.class ) );
	}

	@Test
	public void theRainAStormCloudLaysIsNoWaterForTheWholeBlow(){
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		//a cloud over the cell, which its rain has made water (StormCloud.evolve does so to every
		//cell of it before it strikes one): no bolt into standing water, the blow is shared out
		int cell = at( 18, 15 );
		Blob.seed( cell, 50, StormCloud.class );
		wet( l, cell );
		Taker struck = new Taker( cell ), beside = new Taker( at( 19, 15 ) );
		StormStrikes.Discharge d = jolt( cell );
		assertFalse( d.full );
		assertEquals( 70, struck.took );
		assertEquals( 70, beside.took );
		//the sky's bolt there alike; standing water out of the cloud is still the whole of it
		assertFalse( StormStrikes.discharge( l, cell, StormCloud.class ).full );
		wet( l, at( 10, 10 ) );
		assertTrue( StormStrikes.discharge( l, at( 10, 10 ), StormCloud.class ).full );
	}

	@Test
	public void aStormCloudsOwnTurnOnTheWorldSharesItsBlowAndOnlySplashesTheWater(){
		//as in play: the cloud's turn makes its cell water and strikes it (StormCloud.act), on the
		//world by share. A wisp of a cloud, fading, strikes what it owes at once on its one cell
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		l.customTiles = new ArrayList<>();
		int cell = at( 18, 15 );
		//of health 1 the share is 3 each: the whole of it 3, shared between two 2
		Taker struck = new Taker( cell ), beside = new Taker( at( 19, 15 ) );
		struck.HT = beside.HT = 1;
		Blob.seed( cell, 5, StormCloud.class ).act();
		assertTrue( l.water[cell] );
		assertTrue( struck.hits >= 1 );
		assertEquals( "shared, though it struck the water it rained", 2 * struck.hits, struck.took );
		assertEquals( struck.hits, beside.hits );
		assertEquals( 2 * beside.hits, beside.took );

		//and one it runs into through the water, two steps out, nobody where it struck: a splash
		//of 1-3 a bolt, not the fifth to a third of his health he would take struck
		Actor.clear();
		l = grass( new OverworldLevel( 0 ) );
		l.customTiles = new ArrayList<>();
		wet( l, at( 19, 15 ), at( 20, 15 ) );
		Taker wading = new Taker( at( 20, 15 ) );
		Blob.seed( cell, 5, StormCloud.class ).act();
		assertTrue( l.water[cell] );
		assertTrue( wading.hits >= 1 );
		assertTrue( "a splash a bolt: " + wading.took + " in " + wading.hits,
				wading.took >= wading.hits && wading.took <= 3 * wading.hits );
		assertNull( wading.buff( Paralysis.class ) );
	}

	@Test
	public void aGuestDrawsTheHostsArcs() throws Exception {
		OverworldLevel l = grass( new OverworldLevel( 0 ) );
		wet( l, at( 10, 10 ), at( 11, 10 ) );
		Hero guest = new Hero();
		guest.pos = at( 2, 2 );
		TestParty.join( guest );
		try {
			NetVisuals.clear();
			Taker struck = new Taker( at( 18, 15 ) ), beside = new Taker( at( 19, 15 ) );
			StormStrikes.strike( struck.pos );
			StormStrikes.strike( at( 10, 10 ) );
			StormStrikes.strike( at( 5, 20 ) );
			StormStrikes.Bolt[] host = { StormStrikes.next(), StormStrikes.next(), StormStrikes.next() };
			assertNull( StormStrikes.next() );
			JSONArray sent = new JSONArray();
			JSONArray events = NetVisuals.drainEvents();
			for (int i = 0; i < events.length(); i++){
				if (events.getJSONObject( i ).getString( "t" ).equals( "sk" )) sent.put( events.getJSONObject( i ) );
			}
			assertEquals( 3, sent.length() );
			assertFalse( "nothing sent where it ran nowhere", sent.getJSONObject( 2 ).has( "a" ) || sent.getJSONObject( 2 ).has( "w" ) );

			//a pair off the level is dropped, and an event from before the arcs has none
			JSONObject bad = new JSONObject();
			bad.put( "t", "sk" );
			bad.put( "c", CENTER );
			bad.put( "a", new JSONArray().put( CENTER ).put( W * W + 3 ).put( CENTER ).put( CENTER + 1 ) );
			bad.put( "w", new JSONArray().put( -1 ).put( CENTER ) );
			JSONObject old = new JSONObject();
			old.put( "t", "sk" );
			old.put( "c", CENTER - 2 );
			sent.put( bad ).put( old );

			NetVisuals.replay( sent );
			for (StormStrikes.Bolt h : host){
				StormStrikes.Bolt b = StormStrikes.next();
				assertEquals( h.cell, b.cell );
				assertTrue( Arrays.equals( h.arcs, b.arcs ) );
				assertTrue( Arrays.equals( h.water, b.water ) );
			}
			StormStrikes.Bolt b = StormStrikes.next();
			assertTrue( Arrays.equals( new int[]{ CENTER, CENTER + 1 }, b.arcs ) );
			assertEquals( 0, b.water.length );
			b = StormStrikes.next();
			assertEquals( CENTER - 2, b.cell );
			assertEquals( 0, b.arcs.length + b.water.length );
			assertNull( StormStrikes.next() );
		} finally {
			TestParty.leave();
			NetVisuals.clear();
		}
	}
}
