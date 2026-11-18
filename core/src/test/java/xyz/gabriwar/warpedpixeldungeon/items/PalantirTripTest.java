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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.ZotBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The Palantir makes one round trip: to Zot's floor (99) and back to where it was used, and Zot's
 * floor stays sealed until he dies, so the Palantir only opens there once he is dead. The forged
 * SanChikarah, kept for good since it stopped dropping itself on its way back from the Shadow
 * Yogs, took the hero off that sealed floor (once the Shadow Yogs were dead), back to where it had
 * last been used: the Palantir, spent and only good on 99, never opened again, and Zot could not
 * be reached for the rest of the run. The SanChikarah refuses a sealed floor now, as the Palantir
 * and the town beacon do.
 */
public class PalantirTripTest {

	private static boolean standIn;

	private long seed;
	private int depth, branch;
	private Level level;
	private boolean shadowYogKilled;
	private InterlevelScene.Mode mode;
	private int returnDepth, returnBranch, returnPos;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
		//an item's action cancels the scene's cell selector first: headless a bare one stands in
		//(as SewersKeyReturnTest's)
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
		seed = Dungeon.seed; depth = Dungeon.depth; branch = Dungeon.branch; level = Dungeon.level;
		shadowYogKilled = Dungeon.shadowyogkilled;
		mode = InterlevelScene.mode;
		returnDepth = InterlevelScene.returnDepth;
		returnBranch = InterlevelScene.returnBranch;
		returnPos = InterlevelScene.returnPos;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 0x9A1A47L;
		Dungeon.init();
	}

	@After
	public void tearDown(){
		Dungeon.seed = seed; Dungeon.depth = depth; Dungeon.branch = branch; Dungeon.level = level;
		Dungeon.shadowyogkilled = shadowYogKilled;
		InterlevelScene.mode = mode;
		InterlevelScene.returnDepth = returnDepth;
		InterlevelScene.returnBranch = returnBranch;
		InterlevelScene.returnPos = returnPos;
		Actor.clear();
	}

	//where the hero stands when he uses an item: only its depth and branch matter
	private static Level standingIn( int d, int b ){
		Dungeon.depth = d; Dungeon.branch = b;
		Actor.clear();
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 32, 32 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		Dungeon.level = l;
		return l;
	}

	//Zot's floor as the Palantir's trip builds it: sealed until he dies
	private static Level zotsFloor(){
		Dungeon.depth = 99; Dungeon.branch = 0;
		Actor.clear();
		Dungeon.level = Dungeon.newLevel();
		return Dungeon.level;
	}

	@Test
	public void theSanChikarahCannotTakeTheHeroOffZotsSealedFloor(){
		Hero hero = Dungeon.hero;

		//the Shadow Yogs are dead and the SanChikarah, kept for good, remembers the floor it last
		//left from (depth 13), as it does after its trip back from them
		SanChikarah san = new SanChikarah();
		standingIn( 13, 0 );
		hero.pos = 200;
		san.execute( hero, SanChikarah.AC_PORT );
		assertEquals( InterlevelScene.Mode.PORT4, InterlevelScene.mode );
		Dungeon.shadowyogkilled = true;

		//the Palantir, used on depth 12, takes him to Zot's floor
		Palantir palantir = new Palantir();
		standingIn( 12, 0 );
		int stood = 300;
		hero.pos = stood;
		palantir.execute( hero, Palantir.AC_PORT );
		assertEquals( InterlevelScene.Mode.PALANTIR, InterlevelScene.mode );

		Level zot = zotsFloor();
		assertTrue( "Zot's floor is sealed until he dies", zot.locked );
		assertFalse( "so the Palantir cannot take him back yet",
				palantir.actions( hero ).contains( Palantir.AC_PORT ) );

		//the SanChikarah, turned there at once, leaves him where he is
		InterlevelScene.mode = InterlevelScene.Mode.NONE;
		san.execute( hero, SanChikarah.AC_PORT );
		assertEquals( "no trip off the sealed floor", InterlevelScene.Mode.NONE, InterlevelScene.mode );
		assertEquals( 99, Dungeon.depth );

		//Zot dies and the Palantir takes him back to where he used it
		zot.locked = false;
		assertTrue( palantir.actions( hero ).contains( Palantir.AC_PORT ) );
		palantir.execute( hero, Palantir.AC_PORT );
		assertEquals( InterlevelScene.Mode.RETURN, InterlevelScene.mode );
		assertEquals( 12, InterlevelScene.returnDepth );
		assertEquals( 0, InterlevelScene.returnBranch );
		assertEquals( stood, InterlevelScene.returnPos );
	}

	@Test
	public void oneTripOnlyAsInSprouted(){
		Hero hero = Dungeon.hero;
		Palantir palantir = new Palantir();
		standingIn( 12, 0 );
		assertTrue( palantir.actions( hero ).contains( Palantir.AC_PORT ) );
		palantir.execute( hero, Palantir.AC_PORT );

		//back from Zot's floor, the Palantir is spent: it only opens on 99 now
		standingIn( 12, 0 );
		assertFalse( palantir.actions( hero ).contains( Palantir.AC_PORT ) );
	}
}
