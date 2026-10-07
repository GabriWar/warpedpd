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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The hunt's pack mind (HuntPack, steered from GrayWolf/BrownWolf.chooseEnemy) and its quarry
 * (Deer): a pack wolf goes for the deer before the hero, turns on him only when he crowds it
 * or his side draws blood - and then the whole pack does; a feeding pack stays by the kill at
 * ease until crowded, and is a pack of ordinary wolves once fed; fear, sleep and charm are
 * never overridden; a wolf off the hunt chooses as it always did; and a deer bolts away from
 * what hunts it.
 */
public class HuntPackTest {

	private static final int S = 40;

	private Level level;
	private Hero hero;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private int depth;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private static Level bareLevel( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public boolean waterCanFreeze(){ return true; }
		};
		l.setSize( w, h );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		return l;
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		depth = Dungeon.depth;
		Actor.clear();
		Dungeon.depth = 3;
		hero = new Hero();
		hero.sprite = stub();
		Dungeon.hero = hero;
		level = bareLevel( S, S );
		Painter.fill( level, 1, 1, S - 2, S - 2, Terrain.EMPTY );
		level.buildFlagMaps();
		Dungeon.level = level;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		Dungeon.depth = depth;
	}

	private static int c( int x, int y ){
		return x + y * S;
	}

	//a sprite that draws nothing
	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void die(){ }
			@Override public void move( int from, int to ){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showAlert(){ }
			@Override public void hideAlert(){ }
		};
		sp.visible = false;
		return sp;
	}

	//a grey wolf on hunt `hunt`, seeing everything, loosed at the hunt
	private GrayWolf wolf( int x, int y, long hunt ){
		GrayWolf w = new GrayWolf();
		w.pos = c( x, y );
		w.sprite = stub();
		w.fieldOfView = new boolean[level.length()];
		Arrays.fill( w.fieldOfView, true );
		w.state = w.HUNTING;
		level.mobs.add( w );
		HuntPack p = Buff.affect( w, HuntPack.class );
		p.hunt = hunt;
		p.day = 0;
		return w;
	}

	private Deer deer( int x, int y, long hunt ){
		Deer d = new Deer();
		d.pos = c( x, y );
		d.sprite = stub();
		level.mobs.add( d );
		if (hunt != Long.MIN_VALUE) Buff.affect( d, HuntPack.class ).hunt = hunt;
		return d;
	}

	@Test
	public void packWolfGoesForTheDeerBeforeTheHero(){
		GrayWolf w = wolf( 10, 10, 7 );
		Deer d = deer( 16, 10, 7 );
		hero.pos = c( 10, 14 );
		assertSame( d, w.chooseEnemy() );
		assertSame( w.HUNTING, w.state );
		assertFalse( w.buff( HuntPack.class ).defending );
		//a deer of another hunt is none of this pack's
		Deer other = deer( 11, 10, 8 );
		assertSame( d, w.chooseEnemy() );
		assertNotSame( other, w.chooseEnemy() );
	}

	@Test
	public void itTurnsOnTheHeroWhenCrowded(){
		GrayWolf w = wolf( 10, 10, 7 );
		deer( 16, 10, 7 );
		hero.pos = c( 10, 12 );
		assertSame( hero, w.chooseEnemy() );
		assertTrue( w.buff( HuntPack.class ).defending );
		assertSame( w.HUNTING, w.state );
		//and stays turned on him: a wolf like any other from then on
		hero.pos = c( 10, 20 );
		assertSame( hero, w.chooseEnemy() );
	}

	//a co-op guest is as much one of the party as the host: crowding a pack wolf turns it on him,
	//on the trail and at the kill alike (where he would otherwise walk off with the meat)
	@Test
	public void aGuestCrowdingThePackIsTurnedOn() throws Exception {
		GrayWolf w = wolf( 10, 10, 7 );
		deer( 16, 10, 7 );
		GrayWolf feeder = wolf( 30, 10, 7 );
		feeder.buff( HuntPack.class ).feedAt( 30, 11 );
		hero.pos = c( 20, 35 );
		Hero guest = new Hero();
		guest.pos = c( 10, 12 );
		Hero other = new Hero();
		other.pos = c( 31, 11 );
		TestParty.join( guest );
		TestParty.join( other );
		try {
			assertSame( guest, w.chooseEnemy() );
			assertTrue( w.buff( HuntPack.class ).defending );
			assertSame( "a guest at the kill left alone", other, feeder.chooseEnemy() );
			assertTrue( feeder.buff( HuntPack.class ).defending );
		} finally {
			TestParty.leave();
		}
	}

	@Test
	public void bloodDrawnByTheHerosSideTurnsTheWholePack(){
		GrayWolf w = wolf( 10, 10, 7 );
		GrayWolf mate = wolf( 12, 8, 7 );
		GrayWolf stranger = wolf( 30, 30, 8 );
		deer( 16, 10, 7 );
		deer( 34, 30, 8 );
		hero.pos = c( 10, 15 );
		//a blow that is nobody's doing (the heat, another beast) is no provocation
		assertFalse( HuntPack.byHero( new Bat() ) );
		assertFalse( HuntPack.byHero( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke.class ) );
		assertTrue( HuntPack.byHero( hero ) );
		assertTrue( HuntPack.byHero( new WandOfMagicMissile() ) );
		w.damage( 5, new Bat() );
		assertFalse( w.buff( HuntPack.class ).defending );
		w.damage( 5, hero );
		assertTrue( w.buff( HuntPack.class ).defending );
		assertTrue( "the pack rallies", mate.buff( HuntPack.class ).defending );
		assertFalse( "another hunt's pack does not", stranger.buff( HuntPack.class ).defending );
		assertSame( hero, w.chooseEnemy() );
	}

	@Test
	public void feedingPackIgnoresTheHeroUntilHeComesClose(){
		GrayWolf w = wolf( 10, 10, 7 );
		HuntPack p = w.buff( HuntPack.class );
		//off a level of the world, the world's cells are the level's own
		p.feedAt( 14, 10 );
		hero.pos = c( 10, 16 );
		assertNull( w.chooseEnemy() );
		assertSame( w.WANDERING, w.state );
		assertEquals( c( 14, 10 ), w.target );
		w.pos = c( 13, 10 );
		assertNull( w.chooseEnemy() );
		assertEquals( "at the kill it stays put", w.pos, w.target );
		hero.pos = c( 13, 12 );
		assertSame( hero, w.chooseEnemy() );
		assertTrue( p.defending );
	}

	@Test
	public void fedWolfIsAWolfAgain(){
		GrayWolf w = wolf( 10, 10, 7 );
		HuntPack p = w.buff( HuntPack.class );
		p.feedAt( 14, 10 );
		p.feedLeft = 1;
		hero.pos = c( 10, 30 );
		w.chooseEnemy();
		assertNull( w.buff( HuntPack.class ) );
	}

	@Test
	public void untaggedWolfIsUnchanged(){
		GrayWolf w = new GrayWolf();
		w.pos = c( 10, 10 );
		w.sprite = stub();
		w.fieldOfView = new boolean[level.length()];
		Arrays.fill( w.fieldOfView, true );
		w.state = w.WANDERING;
		level.mobs.add( w );
		Deer d = deer( 11, 10, Long.MIN_VALUE );
		hero.pos = c( 10, 20 );
		Char e = w.chooseEnemy();
		assertNotSame( "an ordinary wolf hunts what it always did", d, e );
		assertSame( hero, e );
	}

	@Test
	public void fearSleepAndCharmAreNeverOverridden(){
		GrayWolf w = wolf( 10, 10, 7 );
		deer( 16, 10, 7 );
		hero.pos = c( 10, 11 );
		Buff.affect( w, Terror.class, 10f );
		w.state = w.FLEEING;
		w.chooseEnemy();
		assertSame( "still running", w.FLEEING, w.state );
		assertFalse( w.buff( HuntPack.class ).defending );

		GrayWolf sleeper = wolf( 20, 20, 7 );
		sleeper.state = sleeper.SLEEPING;
		hero.pos = c( 20, 21 );
		sleeper.chooseEnemy();
		assertSame( sleeper.SLEEPING, sleeper.state );
	}

	@Test
	public void deerBoltsAwayFromTheWolf(){
		Deer d = deer( 20, 20, Long.MIN_VALUE );
		ArrayList<Integer> threats = new ArrayList<>();
		threats.add( c( 23, 20 ) );
		int step = d.bolt( threats );
		assertTrue( "a step", step != -1 );
		assertTrue( "away from the wolf", step % S < 20 );
		//walled in, it freezes
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (dx != 0 || dy != 0) Level.set( c( 20 + dx, 20 + dy ), Terrain.WALL, level );
			}
		}
		level.buildFlagMaps();
		assertEquals( -1, d.bolt( threats ) );
	}

	//a deer bolts from a co-op guest coming up on it as from the host
	@Test
	public void aDeerShiesFromAGuest() throws Exception {
		Deer d = deer( 20, 20, Long.MIN_VALUE );
		hero.pos = c( 2, 2 );
		Hero guest = new Hero();
		guest.pos = c( 23, 20 );
		TestParty.join( guest );
		try {
			assertTrue( d.act() );
			assertSame( "it grazed on with a guest three cells off", d.FLEEING, d.state );
			assertTrue( "away from the guest", d.pos % S < 20 );
		} finally {
			TestParty.leave();
		}
	}

	@Test
	public void deerIsFairGameAndNeverFights(){
		Deer d = deer( 20, 20, Long.MIN_VALUE );
		assertEquals( Char.Alignment.NEUTRAL, d.alignment );
		assertFalse( "a tap goes for it", d.heroShouldInteract() );
		assertNull( d.chooseEnemy() );
		assertEquals( 0, d.attackSkill( hero ) );
	}

	@Test
	public void theTagAndTheDeerSurviveABundleAndOlderOnes(){
		GrayWolf w = wolf( 10, 10, 77 );
		HuntPack p = w.buff( HuntPack.class );
		p.day = 12;
		p.ax = -300;
		p.ay = 410;
		p.feedAt( -290, 405 );
		p.feedLeft = 42;
		p.defending = true;
		Bundle b = new Bundle();
		b.put( "p", p );
		HuntPack back = (HuntPack) b.get( "p" );
		assertEquals( 77, back.hunt );
		assertEquals( 12, back.day );
		assertEquals( -300, back.ax );
		assertEquals( 410, back.ay );
		assertTrue( back.feeding );
		assertEquals( -290, back.feedX );
		assertEquals( 405, back.feedY );
		assertEquals( 42, back.feedLeft );
		assertTrue( back.defending );
		HuntPack bare = new HuntPack();
		bare.restoreFromBundle( new Bundle() );
		assertEquals( Long.MIN_VALUE, bare.hunt );
		assertEquals( Integer.MIN_VALUE, bare.day );
		assertFalse( bare.feeding || bare.defending );

		Deer doe = new Deer();
		doe.setDoe( true );
		Bundle db = new Bundle();
		db.put( "d", doe );
		Deer dback = (Deer) db.get( "d" );
		assertTrue( dback.doe );
		assertEquals( xyz.gabriwar.warpedpixeldungeon.sprites.DoeSprite.class, dback.spriteClass );
		//a mob saved before the deer had a doe of its own
		Bundle ob = new Bundle();
		new Bunny().storeInBundle( ob );
		Deer old = new Deer();
		old.restoreFromBundle( ob );
		assertFalse( "an older deer is a stag", old.doe );
		assertEquals( xyz.gabriwar.warpedpixeldungeon.sprites.DeerSprite.class, old.spriteClass );
		assertNotNull( new Deer().name() );
	}
}
