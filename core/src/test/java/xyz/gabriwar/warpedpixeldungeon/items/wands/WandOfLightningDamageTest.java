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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndResurrect;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Signal;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The wand of lightning's blow, pinned. Who its arcs reach (one cell, two from a target standing
 * in water, one more with the caster's charge, the caster only beside the one it arcs from, never
 * through walls, each once), who is spared (allies off the struck cell, the charged), the share
 * each takes (0.4 + 0.6 / hit, all of it when the struck cell is water) and the caster's half, and
 * its damage by level. Then its run over the water: from the struck cell if it is water and from
 * whoever it hit standing in water, three steps out over the water and no further, even in a
 * lake, into whoever stands in it and does not fly, the caster too (for his half, his death
 * reported as ever), allies still spared; a zap that never touches water is as it was. damageRoll()
 * stands at 100: the numbers are the multiplier's own.
 */
public class WandOfLightningDamageTest {

	private static final int W = 20;

	private Level savedLevel;
	private Hero savedHero, savedUser;
	private Camera savedCam;

	private final HashMap<Char, ArrayList<Integer>> took = new HashMap<>();
	private Hero caster;
	private Group stage;
	private Level level;
	//the caster's health goes down as he is hit (else it is only recorded)
	private boolean mortal;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedUser = user();
		savedCam = Camera.main;
		Actor.clear();
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		level = floor();
		caster = new Hero(){
			@Override
			public void damage( int dmg, Object src ){
				record( this, dmg );
				if (mortal) HP -= dmg;
			}
		};
		caster.pos = at( 3, 10 );
		caster.HP = caster.HT = 100;
		caster.sprite = sprite();
		stage = new Group();
		stage.add( caster.sprite );
		Actor.add( caster );
		Dungeon.hero = caster;
		user( caster );
	}

	@After
	public void tearDown() throws Exception {
		Actor.clear();
		user( savedUser );
		Dungeon.hero = savedHero;
		Dungeon.level = savedLevel;
		Camera.main = savedCam;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	private static Hero user() throws Exception {
		return (Hero) curUser().get( null );
	}

	private static void user( Hero h ) throws Exception {
		curUser().set( null, h );
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField( "curUser" );
		f.setAccessible( true );
		return f;
	}

	//an open floor walled round
	private static Level floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
		return l;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private void wet( int... cells ){
		for (int c : cells) Level.set( c, Terrain.WATER, level );
	}

	private void record( Char ch, int dmg ){
		took.computeIfAbsent( ch, k -> new ArrayList<>() ).add( dmg );
	}

	//what a char took, once each
	private int blow( Char ch ){
		ArrayList<Integer> got = took.get( ch );
		if (got == null) return 0;
		assertEquals( "hit once", 1, got.size() );
		return got.get( 0 );
	}

	//a sprite with nowhere to draw: the zap bursts its sparks off it all the same
	private static CharSprite sprite(){
		return new CharSprite(){
			@Override
			public Emitter centerEmitter(){
				return new Emitter();
			}
		};
	}

	private Char foe( int cell ){
		Char ch = new Char(){
			@Override
			protected boolean act(){
				return true;
			}
			@Override
			public void damage( int dmg, Object src ){
				record( this, dmg );
			}
		};
		ch.alignment = Char.Alignment.ENEMY;
		ch.pos = cell;
		ch.HP = ch.HT = 100;
		ch.sprite = sprite();
		Actor.add( ch );
		return ch;
	}

	private static WandOfLightning wand(){
		return new WandOfLightning(){
			@Override
			public int damageRoll(){
				return 100;
			}
		};
	}

	//a zap at the cell, as Wand's zapper runs it: the blow dealt in the effect's callback;
	//whether that callback ran before fx returned
	private boolean zap( WandOfLightning wand, int cell ){
		final Ballistica bolt = new Ballistica( caster.pos, cell, Ballistica.MAGIC_BOLT );
		final boolean[] done = { false };
		wand.fx( bolt, () -> {
			wand.onZap( bolt );
			done[0] = true;
		} );
		return done[0];
	}

	@Test
	public void aTargetAloneTakesItAll(){
		Char a = foe( at( 8, 10 ) );
		zap( wand(), a.pos );
		assertEquals( 100, blow( a ) );
		assertEquals( 0, blow( caster ) );
	}

	@Test
	public void onDryGroundItArcsOneCellAndTheHitShareIt(){
		Char a = foe( at( 8, 10 ) ), b = foe( at( 9, 11 ) ), far = foe( at( 11, 11 ) );
		zap( wand(), a.pos );
		//0.4 + 0.6 / 2
		assertEquals( 70, blow( a ) );
		assertEquals( 70, blow( b ) );
		assertEquals( "two from the nearest it hit", 0, blow( far ) );

		//three hit: 0.6 each
		took.clear();
		Char c = foe( at( 9, 9 ) );
		zap( wand(), a.pos );
		for (Char ch : new Char[]{ a, b, c }) assertEquals( 60, blow( ch ) );
		assertEquals( 0, blow( far ) );
	}

	@Test
	public void fromWaterItArcsTwoCellsAndHitsInFull(){
		wet( at( 8, 10 ) );
		Char a = foe( at( 8, 10 ) ), b = foe( at( 10, 10 ) ), far = foe( at( 12, 10 ) );
		zap( wand(), a.pos );
		assertEquals( 100, blow( a ) );
		assertEquals( 100, blow( b ) );
		assertEquals( "b stands dry: one cell on from it", 0, blow( far ) );

		//the wand looks at the water, not at whether its target flies over it
		took.clear();
		a.flying = true;
		zap( wand(), a.pos );
		assertEquals( 100, blow( a ) );
		assertEquals( 100, blow( b ) );
	}

	@Test
	public void theCastersChargeReachesOneFurther(){
		Char a = foe( at( 8, 10 ) ), b = foe( at( 10, 10 ) );
		Buff.affect( caster, WandOfLightning.LightningCharge.class, 10f );
		zap( wand(), a.pos );
		assertEquals( 70, blow( a ) );
		assertEquals( 70, blow( b ) );
	}

	@Test
	public void theCasterOnlyBesideWhatItArcsFromAndForHalf(){
		//beside the target: 0.7 for the two of them, the caster's halved
		Char a = foe( at( 4, 10 ) );
		zap( wand(), a.pos );
		assertEquals( 70, blow( a ) );
		assertEquals( 35, blow( caster ) );

		//two off a target in water: out of its reach for the caster, though not for a foe
		Actor.remove( a );
		took.clear();
		wet( at( 5, 10 ) );
		Char w = foe( at( 5, 10 ) ), beyond = foe( at( 7, 10 ) );
		zap( wand(), w.pos );
		assertEquals( 100, blow( w ) );
		assertEquals( 100, blow( beyond ) );
		assertEquals( 0, blow( caster ) );
	}

	@Test
	public void wallsStopTheArcs(){
		wet( at( 8, 10 ) );
		for (int y = 9; y <= 11; y++) Level.set( at( 9, y ), Terrain.WALL, level );
		Char a = foe( at( 8, 10 ) ), behind = foe( at( 10, 10 ) );
		zap( wand(), a.pos );
		assertEquals( 100, blow( a ) );
		assertEquals( 0, blow( behind ) );
	}

	@Test
	public void eachIsHitOnce(){
		wet( at( 8, 10 ), at( 9, 10 ), at( 9, 11 ), at( 10, 10 ) );
		Char[] all = { foe( at( 8, 10 ) ), foe( at( 9, 10 ) ), foe( at( 9, 11 ) ), foe( at( 10, 10 ) ) };
		zap( wand(), all[0].pos );
		for (Char ch : all) assertEquals( 100, blow( ch ) );
	}

	@Test
	public void alliesAndTheChargedAreSpared(){
		Char a = foe( at( 8, 10 ) );
		Char ally = foe( at( 9, 10 ) );
		ally.alignment = Char.Alignment.ALLY;
		Char charged = foe( at( 8, 11 ) );
		Buff.affect( charged, WandOfLightning.LightningCharge.class, 10f );
		zap( wand(), a.pos );
		//spared, and not counted in the share
		assertEquals( 100, blow( a ) );
		assertEquals( 0, blow( ally ) );
		assertEquals( 0, blow( charged ) );

		//an ally the bolt itself strikes is not spared
		took.clear();
		Actor.remove( a );
		Actor.remove( charged );
		zap( wand(), ally.pos );
		assertEquals( 100, blow( ally ) );
	}

	@Test
	public void theBlowIsDealtAtOnceUnderOneLightning(){
		Char a = foe( at( 8, 10 ) );
		foe( at( 9, 10 ) );
		assertTrue( "dealt before fx returns, not after the effect", zap( wand(), a.pos ) );
		int bolts = 0, arcs = 0;
		for (Gizmo g : stage.membersView()){
			if (!(g instanceof Lightning)) continue;
			bolts++;
			for (Gizmo arc : ((Lightning) g).membersView()) if (arc instanceof Lightning.Arc) arcs++;
		}
		assertEquals( 1, bolts );
		assertEquals( "the caster's arc and the one on", 2, arcs );
	}

	// ------------------------------------------------------------ its run over the water

	//the arcs of the one lightning the zap drew
	private int arcs(){
		int bolts = 0, arcs = 0;
		for (Gizmo g : stage.membersView()){
			if (!(g instanceof Lightning)) continue;
			bolts++;
			for (Gizmo arc : ((Lightning) g).membersView()) if (arc instanceof Lightning.Arc) arcs++;
		}
		assertEquals( "one lightning", 1, bolts );
		return arcs;
	}

	private void pool( int x0, int y0, int x1, int y1 ){
		for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) wet( at( x, y ) );
	}

	@Test
	public void itRunsThreeStepsOverTheWaterAndNoFurther(){
		//a strip of water from the target on: past its arcs' two cells, three steps out and not four
		pool( 6, 10, 16, 10 );
		Char t = foe( at( 6, 10 ) ), three = foe( at( 9, 10 ) ), four = foe( at( 10, 10 ) );
		zap( wand(), t.pos );
		assertEquals( 100, blow( t ) );
		assertEquals( "three steps out", 100, blow( three ) );
		assertEquals( "four, beside one it reached: it runs on from none", 0, blow( four ) );
		assertEquals( "the caster on dry ground", 0, blow( caster ) );
	}

	@Test
	public void itRunsFromTheStruckCellWhenItIsWaterThoughTheStruckFlies(){
		//a bat flying over a pool is struck: it sets nothing off itself, the water under it does
		pool( 6, 8, 11, 12 );
		Char bat = foe( at( 6, 10 ) ), one = foe( at( 9, 9 ) ), two = foe( at( 9, 12 ) ), four = foe( at( 10, 10 ) );
		bat.flying = true;
		zap( wand(), bat.pos );
		assertEquals( 100, blow( bat ) );
		assertEquals( "three steps out", 100, blow( one ) );
		assertEquals( 100, blow( two ) );
		assertEquals( "four", 0, blow( four ) );
		//the zap, and a step drawn for each cell of the pool within three of the struck one
		assertEquals( 1 + (4 * 5 - 1), arcs() );
	}

	@Test
	public void theCasterStandingInTheWaterItRunsOverTakesHalf(){
		//three steps through the water from the one struck: out of every arc's reach, not the run's
		pool( 3, 10, 10, 10 );
		Char t = foe( at( 6, 10 ) );
		zap( wand(), t.pos );
		assertEquals( 100, blow( t ) );
		assertEquals( 50, blow( caster ) );

		//from a target on dry ground beside the water, it never sets off
		took.clear();
		Actor.remove( t );
		Level.set( at( 6, 10 ), Terrain.EMPTY, level );
		Char dry = foe( at( 6, 10 ) );
		zap( wand(), dry.pos );
		assertEquals( 100, blow( dry ) );
		assertEquals( 0, blow( caster ) );
	}

	@Test
	public void fliersAndAlliesInTheWaterAreSpared(){
		//three steps out over a pool: the one standing in it is hit, the bat flying over it and the
		//ally standing in it are not
		pool( 6, 9, 12, 11 );
		Char t = foe( at( 6, 10 ) ), other = foe( at( 9, 9 ) ), bat = foe( at( 9, 11 ) ), ally = foe( at( 9, 10 ) );
		bat.flying = true;
		ally.alignment = Char.Alignment.ALLY;
		zap( wand(), t.pos );
		assertEquals( 100, blow( t ) );
		assertEquals( 100, blow( other ) );
		assertEquals( 0, blow( bat ) );
		assertEquals( 0, blow( ally ) );
	}

	@Test
	public void evenInALakeItRunsNoFurtherThanThreeSteps(){
		pool( 1, 1, W - 2, W - 2 );
		Char t = foe( at( 10, 10 ) ), three = foe( at( 13, 10 ) ), four = foe( at( 14, 10 ) ),
				below = foe( at( 10, 14 ) ), corner = foe( at( 6, 6 ) ), far = foe( at( 17, 17 ) );
		zap( wand(), t.pos );
		assertEquals( 100, blow( t ) );
		assertEquals( 100, blow( three ) );
		for (Char ch : new Char[]{ four, below, corner, far }) assertEquals( 0, blow( ch ) );
		//the caster stands in the lake too, seven steps off: out of it
		assertEquals( 0, blow( caster ) );
		//every cell of the 7x7 window round the struck one, once
		assertEquals( 1 + 48, arcs() );
	}

	@Test
	public void itsStepsDrawnAreOneWindowsWorthAtMost(){
		//two set off in a lake (the struck and the one its arc reached): the run covers both windows,
		//but draws one window's worth of its steps
		pool( 1, 1, W - 2, W - 2 );
		Char t = foe( at( 10, 10 ) ), b = foe( at( 12, 10 ) );
		zap( wand(), t.pos );
		assertEquals( 100, blow( t ) );
		assertEquals( 100, blow( b ) );
		assertEquals( "the zap, its arc on and " + WandOfLightning.DRAWN_STEPS + " steps", 2 + WandOfLightning.DRAWN_STEPS, arcs() );
	}

	@Test
	public void aZapThatNeverTouchesWaterIsAsItWas(){
		//water about, the caster standing in some, a foe in a pool: a dry target and its dry arc
		//share the blow as ever, and nothing runs
		wet( at( 3, 10 ) );
		pool( 12, 13, 14, 15 );
		Char a = foe( at( 8, 10 ) ), b = foe( at( 9, 11 ) ), inPool = foe( at( 13, 14 ) );
		zap( wand(), a.pos );
		assertEquals( 70, blow( a ) );
		assertEquals( 70, blow( b ) );
		assertEquals( 0, blow( inPool ) );
		assertEquals( 0, blow( caster ) );
		assertEquals( "the zap and its arc on, no steps", 2, arcs() );
	}

	@Test
	public void aCasterKilledByHisOwnWandInTheWaterIsReportedAsEver() throws Exception {
		//the badge, the run failed and the wand's own message, as when it arcs back into him. The
		//badges held as all earned and a seed set, so none is shown or saved; a resurrection window
		//standing, so failing the run writes no rankings in a test
		Field global = Badges.class.getDeclaredField( "global" ), local = Badges.class.getDeclaredField( "local" );
		global.setAccessible( true );
		local.setAccessible( true );
		Object savedGlobal = global.get( null ), savedLocal = local.get( null ), savedWnd = WndResurrect.instance;
		String savedSeed = Dungeon.customSeedText;
		ArrayList<String> log = new ArrayList<>();
		Signal.Listener<String> listen = text -> { log.add( text ); return false; };
		try {
			global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
			local.set( null, new HashSet<Badges.Badge>() );
			Dungeon.customSeedText = "test";
			WndResurrect.instance = new Object();
			GLog.update.add( listen );

			pool( 3, 10, 10, 10 );
			Char t = foe( at( 6, 10 ) );
			mortal = true;
			caster.HP = 40;
			zap( wand(), t.pos );
			assertEquals( 50, blow( caster ) );
			assertFalse( caster.isAlive() );
			@SuppressWarnings("unchecked") HashSet<Badges.Badge> earned = (HashSet<Badges.Badge>) local.get( null );
			assertTrue( earned.contains( Badges.Badge.DEATH_FROM_FRIENDLY_MAGIC ) );
			assertTrue( log.toString(), log.contains( GLog.NEGATIVE + Messages.get( WandOfLightning.class, "ondeath" ) ) );
		} finally {
			GLog.update.remove( listen );
			WndResurrect.instance = savedWnd;
			Dungeon.customSeedText = savedSeed;
			global.set( null, savedGlobal );
			local.set( null, savedLocal );
		}
	}

	@Test
	public void itsDamageByLevel(){
		WandOfLightning w = new WandOfLightning();
		assertEquals( 5, w.min( 0 ) );
		assertEquals( 10, w.max( 0 ) );
		assertEquals( 6, w.min( 1 ) );
		assertEquals( 15, w.max( 1 ) );
		assertEquals( 8, w.min( 3 ) );
		assertEquals( 25, w.max( 3 ) );
	}
}
