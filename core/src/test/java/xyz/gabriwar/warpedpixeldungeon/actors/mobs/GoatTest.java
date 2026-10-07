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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MonkEnergy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.GoatSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Signal;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
 * The mountain goat (Goat): cliff fauna of the high pastures that answers a blow. Whoever strikes
 * it with a drop at his back is butted over the edge - the push resolved here headless, the
 * striker landing in the chasm and falling through - on open ground it bolts instead; cornered
 * and never struck it lowers its horns a turn first and its butt throws no one.
 */
public class GoatTest {

	private static final int W = 12;

	private int depth, branch;
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
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		fallen = Dungeon.fallenMobs;
		Dungeon.fallenMobs = new SparseArray<>();
		Actor.clear();
		//a dungeon floor with a floor under it: a beast thrown into the chasm falls through alive
		Dungeon.depth = 6;
		Dungeon.branch = 0;
		hero = new Hero();
		hero.lvl = 12;
		hero.sprite = stub();
		//no game scene to hand an interrupted hero back to
		hero.damageInterrupt = false;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		Dungeon.fallenMobs = fallen;
	}

	// ------------------------------------------------------------------ helpers

	//a sprite that draws nothing and is never on screen: blows land at once, without the swing
	private static MobSprite stub(){
		MobSprite sp = new MobSprite(){
			@Override public void fall( Callback landed ){ }
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

	//a 12x12 floor in a ring of rock, the column x = 8 of the given terrain; the hero far off in a corner
	private Level ledge( int column ){
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
		l.transitions = new ArrayList<>();
		Painter.fill( l, 0, 0, W, W, Terrain.WALL );
		Painter.fill( l, 1, 1, W - 2, W - 2, Terrain.EMPTY );
		for (int y = 1; y < W - 1; y++) l.map[8 + y * W] = column;
		l.buildFlagMaps();
		Dungeon.level = l;
		hero.pos = 1 + 10 * W;
		return l;
	}

	private static int at( int x, int y ){ return x + y * W; }

	private Goat goat( Level l, int cell ){
		Goat g = new Goat();
		g.pos = cell;
		g.sprite = stub();
		l.mobs.add( g );
		Actor.add( g );
		return g;
	}

	private static Object invoke( Object o, Class<?> cls, String name ){
		try {
			Method m = cls.getDeclaredMethod( name );
			m.setAccessible( true );
			return m.invoke( o );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static boolean act( Actor a ){
		return (Boolean) invoke( a, Actor.class, "act" );
	}

	private static Pushing pushing(){
		for (Actor a : Actor.all()) if (a instanceof Pushing) return (Pushing) a;
		return null;
	}

	//the push the butt queued, played out as its animation would end it: the one thrown lands
	private static void land( Pushing p ) throws Exception {
		Field f = Pushing.class.getDeclaredField( "callback" );
		f.setAccessible( true );
		Actor.remove( p );
		((Callback) f.get( p )).call();
	}

	private static ArrayList<String> logged( Runnable run ){
		final ArrayList<String> lines = new ArrayList<>();
		Signal.Listener<String> l = lines::add;
		GLog.update.add( l );
		try {
			run.run();
		} finally {
			GLog.update.remove( l );
		}
		return lines;
	}

	// ------------------------------------------------------------------ the beast

	@Test
	public void aGoatIsCliffFauna() throws Exception {
		Goat g = new Goat();
		assertTrue( OverworldFauna.isFauna( g ) );
		assertEquals( Char.Alignment.NEUTRAL, g.alignment );
		assertEquals( Mob.Thermal.COLD_DWELLER, g.thermal );
		assertTrue( "no xp: fauna", g.maxLvl < 0 );
		assertSame( OverworldFauna.Habitat.CLIFF, OverworldFauna.home( Goat.class ) );
		//a co-op guest rebuilds it from the sprite's simple name in the sprites package
		assertSame( GoatSprite.class, g.spriteClass );
		assertEquals( "xyz.gabriwar.warpedpixeldungeon.sprites", GoatSprite.class.getPackage().getName() );
		assertNull( GoatSprite.class.getEnclosingClass() );
		assertTrue( Modifier.isPublic( GoatSprite.class.getConstructor().getModifiers() ) );
	}

	@Test
	public void theButtThrowsOverTheEdge(){
		Level l = ledge( Terrain.CHASM );
		int goatAt = at( 6, 5 ), victim = at( 7, 5 );
		assertEquals( at( 8, 5 ), Goat.shoveCell( l, goatAt, victim ) );
		assertTrue( l.pit[at( 8, 5 )] );
		assertTrue( "a drop at his back: it answers", Goat.answers( l, goatAt, victim ) );

		l = ledge( Terrain.WALL );
		assertEquals( "rock behind: nowhere to throw him", -1, Goat.shoveCell( l, goatAt, victim ) );
		assertFalse( Goat.answers( l, goatAt, victim ) );

		l = ledge( Terrain.EMPTY );
		assertEquals( at( 8, 5 ), Goat.shoveCell( l, goatAt, victim ) );
		assertFalse( "open ground behind: no edge to answer by", Goat.answers( l, goatAt, victim ) );
	}

	@Test
	public void aStruckGoatButtsItsStrikerOffTheCliff() throws Exception {
		Level l = ledge( Terrain.CHASM );
		Goat g = goat( l, at( 6, 5 ) );
		Rat rat = new Rat();
		rat.pos = at( 7, 5 );
		rat.sprite = stub();
		rat.HP = rat.HT = 200;      //it lives through the fall: thrown, it drops to the floor below
		rat.defenseSkill = 0;       //and the butt lands
		l.mobs.add( rat );
		Actor.add( rat );

		g.damage( 1, rat );
		assertTrue( act( g ) );
		assertEquals( "it stands its ground", at( 6, 5 ), g.pos );
		assertTrue( "the butt took its time", g.cooldown() > 0 );
		Pushing p = pushing();
		assertNotNull( "the butt throws the one who struck it", p );
		land( p );
		assertEquals( "over the edge", at( 8, 5 ), rat.pos );
		assertFalse( "and down: gone from this floor", l.mobs.contains( rat ) );
		assertNotNull( "landing on the one below", Dungeon.fallenMobs.get( Dungeon.depth + 1 ) );
	}

	@Test
	public void aStruckGoatBoltsWhenThereIsNoEdgeBehindHim(){
		Level l = ledge( Terrain.EMPTY );
		Goat g = goat( l, at( 6, 5 ) );
		hero.pos = at( 7, 5 );
		Actor.add( hero );
		g.damage( 1, hero );
		int before = l.distance( g.pos, hero.pos );
		assertTrue( act( g ) );
		assertTrue( "it bolts", l.distance( g.pos, hero.pos ) > before );
		assertNull( "and butts no one", pushing() );
	}

	@Test
	public void aCorneredGoatWarnsFirstAndThrowsNoOne(){
		Level l = ledge( Terrain.EMPTY );
		//a dead end of rock round the goat, the hero in its mouth
		for (int y = 3; y <= 7; y++) for (int x = 2; x <= 6; x++) l.map[at( x, y )] = Terrain.WALL;
		l.map[at( 4, 5 )] = Terrain.EMPTY;
		l.map[at( 5, 5 )] = Terrain.EMPTY;
		l.buildFlagMaps();
		Goat g = goat( l, at( 4, 5 ) );
		hero.pos = at( 5, 5 );
		Actor.add( hero );

		ArrayList<String> lines = logged( () -> assertTrue( act( g ) ) );
		boolean warned = false;
		for (String s : lines) warned |= s.contains( "lowers its horns at you" );
		assertTrue( "the horns go down first: " + lines, warned );
		assertEquals( at( 4, 5 ), g.pos );
		assertEquals( "a turn, and no blow yet", hero.HT, hero.HP );
		assertNull( pushing() );

		//he sidesteps it (a struck hero needs a game scene to be interrupted in)
		Buff.affect( hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class );
		float before = g.cooldown();
		assertTrue( act( g ) );
		assertTrue( "then the butt, its time spent", g.cooldown() > before );
		assertNull( "which throws no one: he never struck it", pushing() );
		assertEquals( at( 5, 5 ), hero.pos );
	}

	@Test
	public void aGoatBundles(){
		Level l = ledge( Terrain.CHASM );
		Goat g = goat( l, at( 6, 5 ) );
		g.HP = 30;
		g.damage( 0, hero );
		Bundle b = new Bundle();
		g.storeInBundle( b );
		Goat back = new Goat();
		back.restoreFromBundle( b );
		assertEquals( 30, back.HP );
		Bundle again = new Bundle();
		back.storeInBundle( again );
		assertEquals( hero.id(), again.getInt( "provoker" ) );

		//an older save has none of its keys
		b.remove( "provoker" );
		b.remove( "heading" );
		b.remove( "bracing" );
		Goat old = new Goat();
		old.restoreFromBundle( b );
		Bundle out = new Bundle();
		old.storeInBundle( out );
		assertEquals( -1, out.getInt( "provoker" ) );
		assertEquals( 0, out.getInt( "heading" ) );
		assertFalse( out.getBoolean( "bracing" ) );
	}

	@Test
	public void aGoatLeavesMeat(){
		Level l = ledge( Terrain.EMPTY );
		Goat g = goat( l, at( 6, 5 ) );
		g.die( null );
		Heap h = l.heaps.get( at( 6, 5 ) );
		assertNotNull( h );
		assertTrue( h.peek() instanceof MysteryMeat );
	}
}
