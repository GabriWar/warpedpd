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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MonkEnergy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.Camera;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
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
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A second look at the slices' creatures, beside LayerFaunaTest and GoatTest: every turn a goat
 * takes spends its time, whatever stands round it (a turn that spends nothing freezes the game);
 * the hero himself - not only a beast - is thrown over the edge when he strikes a goat with a
 * drop at his back, and a co-op guest's hero never is; the golems of the dungeon, the surface
 * and the mountains leave no ore; and a slice's count of the day's ore drops comes back from a
 * real save, and from an older one without it.
 */
public class FaunaChecksTest {

	private static final int S = 12;
	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, turn, challenges;
	private DayNightCycle.Phase override;
	private Level saved;
	private Hero savedHero;
	private SparseArray<ArrayList<xyz.gabriwar.warpedpixeldungeon.levels.features.FallenMob>> fallen;
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
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		turn = Dungeon.cycleTurn;
		challenges = Dungeon.challenges;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		fallen = Dungeon.fallenMobs;
		Dungeon.fallenMobs = new SparseArray<>();
		Actor.clear();
		Dungeon.depth = 6;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		hero = new Hero();
		hero.lvl = 12;
		hero.sprite = stub();
		hero.damageInterrupt = false;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.cycleTurn = turn;
		Dungeon.challenges = challenges;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		Dungeon.fallenMobs = fallen;
	}

	// ------------------------------------------------------------------ helpers

	private static MobSprite stub(){
		MobSprite sp = new MobSprite(){
			@Override public void fall( Callback landed ){ }
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void die(){ }
			@Override public void move( int from, int to ){ }
			@Override public void idle(){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showAlert(){ }
			@Override public void hideAlert(){ }
		};
		sp.visible = false;
		return sp;
	}

	//a 12x12 floor in a ring of rock, the column x = 8 of the given terrain
	private Level ledge( int column ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( S, S );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		Painter.fill( l, 0, 0, S, S, Terrain.WALL );
		Painter.fill( l, 1, 1, S - 2, S - 2, Terrain.EMPTY );
		for (int y = 1; y < S - 1; y++) l.map[8 + y * S] = column;
		l.buildFlagMaps();
		Dungeon.level = l;
		hero.pos = 1 + 10 * S;
		return l;
	}

	private static int at( int x, int y ){ return x + y * S; }

	private static <T extends Mob> T put( Level l, T m, int cell ){
		m.pos = cell;
		m.sprite = stub();
		l.mobs.add( m );
		Actor.add( m );
		return m;
	}

	private static Object call( Object o, Class<?> cls, String name ){
		try {
			Method m = cls.getDeclaredMethod( name );
			m.setAccessible( true );
			return m.invoke( o );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static boolean act( Actor a ){
		return (Boolean) call( a, Actor.class, "act" );
	}

	private static Object field( Object o, Class<?> cls, String name ) throws Exception {
		Field f = cls.getDeclaredField( name );
		f.setAccessible( true );
		return f.get( o );
	}

	//the push queued for this char, or null
	private static Pushing pushOf( Object ch ) throws Exception {
		for (Actor a : Actor.all()){
			if (a instanceof Pushing && field( a, Pushing.class, "ch" ) == ch) return (Pushing) a;
		}
		return null;
	}

	private static Pushing anyPush(){
		for (Actor a : Actor.all()) if (a instanceof Pushing) return (Pushing) a;
		return null;
	}

	// ------------------------------------------------------------------ the goat's turns

	@Test
	public void everyTurnAGoatTakesSpendsItsTime(){
		int[] columns = { Terrain.CHASM, Terrain.WALL, Terrain.EMPTY };
		int turns = 0;
		for (int seed = 0; seed < 300; seed++){
			Random.pushGenerator( 9000 + seed );
			try {
				Actor.clear();
				Dungeon.fallenMobs = new SparseArray<>();
				hero = new Hero();
				hero.lvl = 12;
				hero.sprite = stub();
				hero.damageInterrupt = false;
				Dungeon.hero = hero;
				Level l = ledge( columns[seed % 3] );
				//a few odd rocks, so it gets cornered now and then
				for (int i = Random.Int( 6 ); i > 0; i--) l.map[at( 1 + Random.Int( S - 2 ), 1 + Random.Int( S - 2 ) )] = Terrain.WALL;
				l.buildFlagMaps();
				int g0;
				do g0 = at( 1 + Random.Int( S - 2 ), 1 + Random.Int( S - 2 ) ); while (l.solid[g0] || l.pit[g0]);
				Goat g = put( l, new Goat(), g0 );
				//the hero near it, beside it, or far
				int h0 = -1;
				for (int tries = 0; tries < 50 && h0 == -1; tries++){
					int c = Random.Int( 3 ) == 0 ? g0 + PathFinder.NEIGHBOURS8[Random.Int( 8 )]
							: at( 1 + Random.Int( S - 2 ), 1 + Random.Int( S - 2 ) );
					if (c != g0 && !l.solid[c] && !l.pit[c] && Actor.findChar( c ) == null) h0 = c;
				}
				if (h0 != -1){
					hero.pos = h0;
					Actor.add( hero );
				}
				//a rat beside it that struck it, now and then
				if (Random.Int( 2 ) == 0){
					int r = g0 + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
					if (!l.solid[r] && !l.pit[r] && Actor.findChar( r ) == null){
						Rat rat = put( l, new Rat(), r );
						rat.HP = rat.HT = 500;
						g.damage( 0, rat );
					}
				}
				//a wolf about, now and then
				if (Random.Int( 3 ) == 0){
					int c = at( 1 + Random.Int( S - 2 ), 1 + Random.Int( S - 2 ) );
					if (!l.solid[c] && !l.pit[c] && Actor.findChar( c ) == null) put( l, new GrayWolf(), c );
				}
				if (Random.Int( 3 ) == 0 && hero.pos >= 0 && l.adjacent( hero.pos, g0 )) g.damage( 0, hero );
				if (Random.Int( 10 ) == 0) Buff.affect( g, Paralysis.class, 3f );

				for (int t = 0; t < 6; t++){
					//he sidesteps its horns (a struck hero needs a game scene to be interrupted in)
					Buff.affect( hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class );
					float before = g.cooldown();
					boolean done = act( g );
					//a stub sprite is never on screen: every blow lands at once, nothing is left pending
					assertTrue( "seed " + seed + " turn " + t + ": act left a turn pending", done );
					assertTrue( "seed " + seed + " turn " + t + ": a turn that spends no time", g.cooldown() > before );
					turns++;
					if (!g.isAlive()) break;
				}
			} finally {
				Random.popGenerator();
			}
		}
		assertTrue( turns > 1000 );
	}

	// ------------------------------------------------------------------ the hero at the edge

	@Test
	public void aHeroWhoStrikesAGoatWithADropAtHisBackIsThrownOverIt() throws Exception {
		//a push of this machine's hero pans the camera after him
		Camera cam = Camera.main;
		Camera.main = new Camera( 0, 0, 100, 100, 1 );
		try {
			heroThrown();
		} finally {
			Camera.main = cam;
		}
	}

	private void heroThrown() throws Exception {
		int hits = 0;
		for (int trial = 0; trial < 40 && hits < 3; trial++){
			Actor.clear();
			hero = new Hero();
			hero.lvl = 12;
			hero.sprite = stub();
			hero.damageInterrupt = false;
			hero.HP = hero.HT = 500;
			Dungeon.hero = hero;
			Level l = ledge( Terrain.CHASM );
			Goat g = put( l, new Goat(), at( 6, 5 ) );
			hero.pos = at( 7, 5 );
			Actor.add( hero );
			g.damage( 1, hero );
			assertTrue( act( g ) );
			assertEquals( "it stands its ground: he has the drop at his back", at( 6, 5 ), g.pos );
			if (hero.HP == hero.HT) continue;   //the butt missed
			hits++;
			Pushing p = pushOf( hero );
			assertTrue( "butted, he is thrown", p != null );
			assertEquals( "over the edge, straight away from it", at( 8, 5 ), (int) (Integer) field( p, Pushing.class, "to" ) );
			assertTrue( l.pit[at( 8, 5 )] );
		}
		assertTrue( "the butt lands now and then", hits > 0 );
	}

	@Test
	public void aCoopGuestsHeroIsNeverThrown() throws Exception {
		int hits = 0;
		for (int trial = 0; trial < 40 && hits < 3; trial++){
			Actor.clear();
			hero = new Hero();
			hero.lvl = 12;
			hero.sprite = stub();
			hero.damageInterrupt = false;
			Dungeon.hero = hero;
			Level l = ledge( Terrain.CHASM );
			Goat g = put( l, new Goat(), at( 6, 5 ) );
			//this machine's hero far off; a guest's at the goat's side, the drop at his back
			Actor.add( hero );
			Hero guest = new Hero();
			guest.lvl = 12;
			guest.sprite = stub();
			guest.damageInterrupt = false;
			guest.HP = guest.HT = 500;
			guest.pos = at( 7, 5 );
			Actor.add( guest );
			g.damage( 1, guest );
			assertTrue( act( g ) );
			if (guest.HP == guest.HT) continue;
			hits++;
			assertNull( "his own machine moves him, never this one", pushOf( guest ) );
			assertEquals( at( 7, 5 ), guest.pos );
		}
		assertTrue( "the butt lands now and then", hits > 0 );
	}

	// ------------------------------------------------------------------ golem ore

	@Test
	public void golemsOutsideTheWorldsCavesLeaveNoOre(){
		Level dungeon = ledge( Terrain.EMPTY );
		Golem g = put( dungeon, new Golem(), at( 4, 4 ) );
		for (int i = 0; i < 50; i++) call( g, Golem.class, "dropExtraLoot" );
		assertEquals( "a dungeon golem is as it was", 0, dungeon.heaps.size );

		for (int alt : new int[]{ 0, 2, 9 }){
			OverworldLevel ow = window( alt );
			int before = ow.heaps.size;
			Golem og = new Golem();
			og.pos = W / 2 + (H / 2) * W;
			for (int i = 0; i < 50; i++) call( og, Golem.class, "dropExtraLoot" );
			assertEquals( "no ore from a golem at altitude " + alt, before, ow.heaps.size );
		}
	}

	// ------------------------------------------------------------------ the day's ore count, saved

	private OverworldLevel window( int altitude ){
		PathFinder.setMapSize( W, H );
		int ox = -W / 2, oy = -H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.depth = WorldLayers.depthOf( altitude );
		Dungeon.level = ow;
		return ow;
	}

	private static Bundle written( OverworldLevel ow ) throws Exception {
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		//a headless Game carries no version code: stamp the save as a current one
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		return b;
	}

	private static OverworldLevel read( Bundle b ) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle in = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( in );
		Dungeon.level = back;
		return back;
	}

	@Test
	public void theDaysOreCountComesBackFromASave() throws Exception {
		Dungeon.cycleTurn = 7 * DayNightCycle.FULL_CYCLE + 50;
		OverworldLevel ow = window( -8 );
		assertEquals( 0.2f, ow.oreDropChance( 0.2f ), 1e-6f );
		ow.oreDropped();
		ow.oreDropped();
		ow.oreDropped();
		Bundle b = written( ow );

		OverworldLevel back = read( b );
		assertEquals( -8, back.altitude() );
		assertEquals( "three fell today: an eighth", 0.025f, back.oreDropChance( 0.2f ), 1e-6f );

		//a save from before the golems' ore: nothing fell yet today
		Bundle old = written( back );
		assertTrue( old.remove( "ore_drop_day" ) );
		assertTrue( old.remove( "ore_drops" ) );
		OverworldLevel older = read( old );
		assertEquals( 0.2f, older.oreDropChance( 0.2f ), 1e-6f );
	}
}
