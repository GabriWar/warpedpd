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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leashed;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Hides;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GatorTooth;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.VultureFeather;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.FishingSpear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.AlligatorSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.VultureSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
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
 * The surface's beasts of the open lands, the swamp and the rivers, on a hand-made floor: a struck
 * bison sets its herd within reach on the striker and calms once he is gone, its charge's blow
 * lands harder and throws him; a struck camel spits once and the spit dazes; a vulture dives only
 * on the weak, gives them up once they mend, comes down to carrion and eats it a piece at a time;
 * an alligator lies in wait, bursts out at whoever steps beside it and drags him into the water,
 * and goes back to the water when its prey gets away; a pike dies out of the water and goes under
 * the ice of a frozen river; what each leaves and what the workbench makes of it; every beast's own
 * state survives a save, an older save loading clean; and the pictures: an alligator set down in its
 * pool shows only its eyes at once, a vulture is drawn aloft whatever last moved it.
 */
public class SurfaceBeastsTest {

	private static final int W = 24, H = 16;

	private Level level;
	private Hero hero;
	private Level saved;
	private Hero savedHero;
	private int depth;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		depth = Dungeon.depth;
		Actor.clear();
		Dungeon.depth = 97;
		hero = new Hero();
		hero.lvl = 20;
		hero.HP = hero.HT = 2000;
		hero.sprite = stub();
		//no game scene to hand an interrupted hero back to
		hero.damageInterrupt = false;
		Dungeon.hero = hero;
		level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( W, H );
		level.mobs = new HashSet<>();
		level.heaps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		level.customTiles = new ArrayList<>();
		level.customTerrain = new ArrayList<>();
		level.customWalls = new ArrayList<>();
		level.transitions = new ArrayList<>();
		Painter.fill( level, 0, 0, W, H, Terrain.WALL );
		Painter.fill( level, 1, 1, W - 2, H - 2, Terrain.EMPTY );
		level.buildFlagMaps();
		Dungeon.level = level;
		hero.pos = at( 1, H - 2 );
		Actor.add( hero );
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		Dungeon.depth = depth;
	}

	// ------------------------------------------------------------------ helpers

	private static int at( int x, int y ){
		return x + y * W;
	}

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
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){ }
			@Override public void showAlert(){ }
			@Override public void hideAlert(){ }
		};
		sp.visible = false;
		return sp;
	}

	private <T extends Mob> T put( T m, int cell ){
		m.pos = cell;
		m.sprite = stub();
		m.fieldOfView = new boolean[level.length()];
		Arrays.fill( m.fieldOfView, true );
		level.mobs.add( m );
		Actor.add( m );
		return m;
	}

	//water on the cells given, the flag maps rebuilt
	private void water( int... cells ){
		for (int c : cells) level.map[c] = Terrain.WATER;
		level.buildFlagMaps();
	}

	private static boolean act( Actor a ){
		try {
			Method m = Actor.class.getDeclaredMethod( "act" );
			m.setAccessible( true );
			return (Boolean) m.invoke( a );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static Object get( Object o, String name ){
		try {
			Field f = o.getClass().getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static void set( Object o, String name, Object v ){
		try {
			Field f = o.getClass().getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Pushing pushing(){
		for (Actor a : Actor.all()) if (a instanceof Pushing) return (Pushing) a;
		return null;
	}

	//a push played out as its animation would end it: the one pushed lands
	private static void land( Pushing p ) throws Exception {
		Field f = Pushing.class.getDeclaredField( "callback" );
		f.setAccessible( true );
		Actor.remove( p );
		((Callback) f.get( p )).call();
	}

	//everything lying on a cell, by class
	private int count( int cell, Class<? extends Item> cls ){
		Heap h = level.heaps.get( cell );
		int n = 0;
		if (h != null) for (Item i : h.items) if (cls.isInstance( i )) n += i.quantity();
		return n;
	}

	// ------------------------------------------------------------------ the bison

	@Test
	public void aStruckBisonSetsItsHerdOnTheStriker(){
		Bison a = put( new Bison(), at( 4, 4 ) );
		Bison b = put( new Bison(), at( 4 + Bison.HERD, 4 ) );
		Bison far = put( new Bison(), at( 4 + Bison.HERD + 1, 6 ) );
		assertEquals( Char.Alignment.NEUTRAL, a.alignment );
		a.damage( 5, hero );
		assertTrue( a.charging() );
		assertSame( hero, a.quarry() );
		assertEquals( Char.Alignment.ENEMY, a.alignment );
		assertTrue( "the herd within reach turns too", b.charging() );
		assertSame( hero, b.quarry() );
		assertFalse( "one further off does not", far.charging() );
		assertEquals( Char.Alignment.NEUTRAL, far.alignment );
		//what hurts it on its own stirs nothing
		Bison c = put( new Bison(), at( 6, 10 ) );
		c.damage( 3, Hides.class );
		assertFalse( c.charging() );
	}

	@Test
	public void aBisonTheHomesteadHoldsStirsNoHerd(){
		Bison held = put( new Bison(), at( 4, 4 ) );
		Bison wild = put( new Bison(), at( 6, 4 ) );
		Buff.affect( held, Leashed.class ).penned = true;
		assertTrue( Roping.held( held ) );
		held.damage( 5, hero );
		assertFalse( held.charging() );
		assertFalse( wild.charging() );
		//and a wild one's anger passes the held one by
		wild.damage( 5, hero );
		assertTrue( wild.charging() );
		assertFalse( held.charging() );
		assertTrue( "it can be roped while calm", Roping.leadable( new Bison() ) );
		assertFalse( "not while it charges", Roping.leadable( wild ) );
	}

	@Test
	public void theHerdCalmsOnceItsQuarryIsGone(){
		Bison b = put( new Bison(), at( 4, 4 ) );
		b.damage( 5, hero );
		assertTrue( b.charging() );
		Actor.remove( hero );
		assertTrue( act( b ) );
		assertFalse( b.charging() );
		assertEquals( Char.Alignment.NEUTRAL, b.alignment );
		assertSame( b.WANDERING, b.state );
	}

	@Test
	public void theHerdCalmsWhenItsQuarryStaysOutOfSight(){
		Bison b = put( new Bison(), at( 4, 4 ) );
		b.damage( 5, hero );
		Arrays.fill( b.fieldOfView, false );
		//Char.act would see him again: the anger is judged before the hunt runs, on what it saw last
		for (int i = 0; i < Bison.CALM; i++){
			set( b, "unseen", i );
			assertTrue( (Boolean) invokeStillAngry( b ) );
		}
		set( b, "unseen", Bison.CALM );
		assertFalse( (Boolean) invokeStillAngry( b ) );
	}

	private static Object invokeStillAngry( Bison b ){
		try {
			Method m = Bison.class.getDeclaredMethod( "stillAngry" );
			m.setAccessible( true );
			return m.invoke( b );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//a big rat for the beasts to throw about (the hero's own push pans a camera a test has not got)
	private Rat victim( int cell, Char.Alignment side ){
		Rat r = put( new Rat(), cell );
		r.HP = r.HT = 500;
		r.alignment = side;
		return r;
	}

	@Test
	public void aChargesBlowLandsHarderAndThrowsBack() throws Exception {
		Bison b = put( new Bison(), at( 6, 6 ) );
		Rat rat = victim( at( 7, 6 ), Char.Alignment.ENEMY );
		set( b, "charging", true );
		assertEquals( 30, b.attackProc( rat, 20 ) );
		Pushing p = pushing();
		assertNotNull( "the charge throws him", p );
		land( p );
		assertEquals( at( 8, 6 ), rat.pos );
		//a blow struck standing does neither
		assertEquals( 20, b.attackProc( rat, 20 ) );
		assertNull( pushing() );
	}

	@Test
	public void aChargeNeverThrowsAGuestsHero() throws Exception {
		Hero guest = new Hero();
		guest.HP = guest.HT = 500;
		guest.pos = at( 7, 6 );
		TestParty.join( guest );
		try {
			Bison b = put( new Bison(), at( 6, 6 ) );
			set( b, "charging", true );
			assertEquals( 30, b.attackProc( guest, 20 ) );
			assertNull( "his own machine moves him", pushing() );
		} finally {
			TestParty.leave();
		}
	}

	@Test
	public void aGrazingBisonKeepsToItsHerd(){
		Bison a = put( new Bison(), at( 4, 4 ) );
		put( new Bison(), at( 8, 4 ) );
		put( new Bison(), at( 8, 8 ) );
		assertEquals( "the middle of the others", at( 8, 6 ), a.herdCentre( level ) );
		Bison alone = put( new Bison(), at( 21, 13 ) );
		assertEquals( "none within reach: it grazes alone", -1, alone.herdCentre( level ) );
	}

	@Test
	public void aBisonLeavesItsHideAndMeat(){
		Bison b = put( new Bison(), at( 5, 5 ) );
		b.die( null );
		assertEquals( 2, count( at( 5, 5 ), MysteryMeat.class ) );
		assertEquals( "always its hide", 1, count( at( 5, 5 ), Hide.class ) );
		assertEquals( 2, Roping.meatOf( new Bison() ) );
	}

	@Test
	public void aBisonBundles(){
		Bison b = put( new Bison(), at( 4, 4 ) );
		b.damage( 5, hero );
		Bundle bundle = new Bundle();
		b.storeInBundle( bundle );
		Bison back = new Bison();
		back.restoreFromBundle( bundle );
		assertTrue( back.charging() );
		assertEquals( Char.Alignment.ENEMY, back.alignment );
		assertSame( hero, back.quarry() );

		//an older save has none of its keys: a calm bison
		bundle.remove( "quarry" );
		bundle.remove( "unseen" );
		bundle.remove( "moved" );
		bundle.put( "state", "WANDERING" );
		Bison old = new Bison();
		old.restoreFromBundle( bundle );
		assertFalse( old.charging() );
		assertEquals( Char.Alignment.NEUTRAL, old.alignment );
	}

	// ------------------------------------------------------------------ the camel

	@Test
	public void aStruckCamelSpitsOnceAndMakesOff(){
		Camel c = put( new Camel(), at( 6, 6 ) );
		hero.pos = at( 8, 6 );
		c.damage( 1, hero );
		assertEquals( hero.id(), get( c, "spitAt" ) );
		float before = c.cooldown();
		assertTrue( act( c ) );
		assertTrue( "the spit took its time", c.cooldown() > before );
		assertEquals( "one spit only", -1, get( c, "spitAt" ) );
		assertEquals( at( 6, 6 ), c.pos );
		//then it makes off from him
		int d = level.distance( c.pos, hero.pos );
		assertTrue( act( c ) );
		assertTrue( "it makes off", level.distance( c.pos, hero.pos ) > d );
		assertSame( c.FLEEING, c.state );
	}

	@Test
	public void theSpitDazes(){
		Camel c = put( new Camel(), at( 6, 6 ) );
		hero.pos = at( 8, 6 );
		c.attackProc( hero, 2 );
		assertNotNull( hero.buff( Daze.class ) );
		assertEquals( Camel.DAZE, hero.buff( Daze.class ).cooldown(), 0.01f );
	}

	@Test
	public void noSpitOutOfReachOrSight(){
		Camel c = put( new Camel(), at( 4, 6 ) );
		hero.pos = at( 4 + Camel.SPIT_RANGE + 1, 6 );
		c.damage( 1, hero );
		float before = c.cooldown();
		assertTrue( act( c ) );
		assertEquals( "it only makes off", before + 1f / c.speed(), c.cooldown(), 0.01f );
		assertEquals( -1, get( c, "spitAt" ) );
		//rock between them: no line for a spit
		Camel walled = put( new Camel(), at( 12, 10 ) );
		hero.pos = at( 14, 10 );
		level.map[at( 13, 10 )] = Terrain.WALL;
		level.buildFlagMaps();
		walled.damage( 1, hero );
		before = walled.cooldown();
		assertTrue( act( walled ) );
		assertEquals( before + 1f / walled.speed(), walled.cooldown(), 0.01f );
		assertNull( hero.buff( Daze.class ) );
	}

	@Test
	public void aCamelAmblesOffFromAHeroTooNear(){
		Camel c = put( new Camel(), at( 10, 8 ) );
		hero.pos = at( 11, 8 );
		assertTrue( act( c ) );
		assertTrue( level.distance( c.pos, hero.pos ) > 1 );
		assertSame( c.FLEEING, c.state );
	}

	@Test
	public void aCamelLeavesMeatAndBundles(){
		Camel c = put( new Camel(), at( 5, 5 ) );
		assertEquals( Mob.Thermal.HEAT_DWELLER, c.thermal );
		assertEquals( 2, Roping.meatOf( c ) );
		c.damage( 1, hero );
		Bundle bundle = new Bundle();
		c.storeInBundle( bundle );
		Camel back = new Camel();
		back.restoreFromBundle( bundle );
		assertEquals( hero.id(), get( back, "spitAt" ) );
		assertEquals( Camel.FRIGHT, get( back, "fright" ) );
		bundle.remove( "spit_at" );
		bundle.remove( "fright" );
		bundle.remove( "heading" );
		Camel old = new Camel();
		old.restoreFromBundle( bundle );
		assertEquals( -1, get( old, "spitAt" ) );
		assertEquals( 0, get( old, "fright" ) );

		c.die( null );
		assertEquals( 2, count( at( 5, 5 ), MysteryMeat.class ) );
	}

	// ------------------------------------------------------------------ the vulture

	@Test
	public void aVultureDivesOnlyOnTheWeak(){
		Vulture v = put( new Vulture(), at( 6, 6 ) );
		hero.pos = at( 10, 6 );
		assertTrue( v.state.act( false, false ) );
		assertEquals( "the hale are left alone", Char.Alignment.NEUTRAL, v.alignment );
		assertSame( v.WANDERING, v.state );

		hero.HP = (int) (hero.HT * Vulture.WEAK) - 1;
		v.state.act( false, false );
		assertEquals( "the weak are dived on", Char.Alignment.ENEMY, v.alignment );
		assertSame( v.HUNTING, v.state );
		assertSame( hero, v.chooseEnemy() );

		//still weak enough at 35%: it keeps on
		hero.HP = (int) (hero.HT * 0.35f);
		v.state.act( false, false );
		assertSame( v.HUNTING, v.state );
		//mended past 40%: it gives him up
		hero.HP = (int) (hero.HT * Vulture.GIVE_UP) + 1;
		v.state.act( false, false );
		assertEquals( Char.Alignment.NEUTRAL, v.alignment );
		assertSame( v.WANDERING, v.state );
	}

	@Test
	public void aVultureStruckByTheHaleFliesOff(){
		Vulture v = put( new Vulture(), at( 6, 6 ) );
		hero.pos = at( 8, 6 );
		v.damage( 3, hero );
		assertSame( v.FLEEING, v.state );
		assertEquals( Char.Alignment.NEUTRAL, v.alignment );
		assertSame( "it flies off from him", hero, v.chooseEnemy() );
		//struck by the weak, it dives on him instead
		Vulture w = put( new Vulture(), at( 12, 6 ) );
		hero.HP = hero.HT / 5;
		w.damage( 3, hero );
		assertSame( w.HUNTING, w.state );
		assertEquals( Char.Alignment.ENEMY, w.alignment );
	}

	@Test
	public void aVultureComesDownToCarrionAndEatsIt(){
		Vulture v = put( new Vulture(), at( 4, 4 ) );
		hero.pos = at( 20, 12 );
		int carcass = at( 7, 4 );
		level.drop( new MysteryMeat().quantity( 2 ), carcass );
		assertTrue( Vulture.carrionAt( level, carcass ) );
		for (int i = 0; i < 6 && v.pos != carcass; i++) v.state.act( false, false );
		assertEquals( "down on the carcass", carcass, v.pos );
		for (int i = 0; i < Vulture.EAT; i++) v.state.act( false, false );
		assertEquals( "a piece eaten", 1, count( carcass, MysteryMeat.class ) );
		for (int i = 0; i < Vulture.EAT; i++) v.state.act( false, false );
		assertNull( "all of it", level.heaps.get( carcass ) );
		//cooked meat is no carrion
		assertFalse( Vulture.isCarrion( new xyz.gabriwar.warpedpixeldungeon.items.food.ChargrilledMeat() ) );
	}

	@Test
	public void aHeroNearbySendsItUpOffTheCarrion(){
		Vulture v = put( new Vulture(), at( 7, 4 ) );
		level.drop( new MysteryMeat().quantity( 2 ), at( 7, 4 ) );
		hero.pos = at( 7 + Vulture.SHY, 4 );
		v.state.act( false, false );
		assertTrue( "it takes off", v.pos != at( 7, 4 ) );
		assertEquals( 2, count( at( 7, 4 ), MysteryMeat.class ) );
	}

	@Test
	public void aVultureCirclesOnItsRing(){
		Vulture v = put( new Vulture(), at( 10 + Vulture.RADIUS, 8 ) );
		int anchor = at( 10, 8 );
		int step = v.circleStep( anchor );
		assertTrue( step != -1 );
		float dx = step % W - 10, dy = step / W - 8;
		float d = (float) Math.sqrt( dx * dx + dy * dy );
		assertTrue( "it keeps to the ring: " + d, Math.abs( d - Vulture.RADIUS ) < 1f );
		assertEquals( "it goes round: a row up or down, not along the radius", 1, Math.abs( step / W - 8 ) );
	}

	@Test
	public void aVultureBackToCirclingIsNobodysEnemy(){
		Vulture v = put( new Vulture(), at( 6, 6 ) );
		hero.pos = at( 10, 6 );
		hero.HP = hero.HT / 5;
		v.state.act( false, false );
		assertSame( v.HUNTING, v.state );
		//mended a little, then out of its sight: Mob's hunt gives up and hands it back to its circling
		//while it still dives on him
		hero.HP = (int) (hero.HT * 0.35f);
		v.state = v.WANDERING;
		v.state.act( false, false );
		assertEquals( Char.Alignment.NEUTRAL, v.alignment );
		assertEquals( -1, get( v, "prey" ) );
		assertNull( v.chooseEnemy() );
	}

	//a vulture's picture as the game draws it: its place (x, y) is what its cell or a mover wrote
	//last, its body is drawn aloft over it whatever wrote it, and its shadow stays on the ground
	@Test
	public void aVultureIsDrawnAloftWhateverLastWroteItsPlace() throws Exception {
		Camera camera = Camera.main;
		float elapsed = Game.elapsed;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			Vulture v = new Vulture();
			v.pos = at( 6, 6 );
			//no scene here to hold the mark over a beast unaware of him
			v.state = v.PASSIVE;
			VultureSprite s = new VultureSprite();
			s.link( v );
			float ground = s.worldToCamera( v.pos ).y;
			float shadow = drawn( s, "shadowMatrix" );
			Game.elapsed = 1 / 60f;
			for (int i = 0; i < 120; i++) s.update();
			assertEquals( "its place is its cell's", ground, s.y, 0.001f );
			assertAloft( s, ground );
			assertEquals( "its shadow stays on the ground", shadow, drawn( s, "shadowMatrix" ), 0.001f );

			//a move's tween writing its place after the sprite's own frame
			s.point( new PointF( s.x, ground ) );
			assertAloft( s, ground );

			//the window shifting under it: it goes with the window and stays there
			s.shiftWorld( 0, 32 );
			for (int i = 0; i < 10; i++) s.update();
			assertEquals( ground + 32, s.y, 0.001f );
			assertAloft( s, ground + 32 );
		} finally {
			Camera.main = camera;
			Game.elapsed = elapsed;
		}
	}

	//its body drawn LIFT over the place given, give or take its bob
	private static void assertAloft( CharSprite s, float place ) throws Exception {
		float lift = place - drawn( s, "matrix" );
		assertTrue( "drawn aloft by " + lift, lift >= 2.4f && lift <= 3.6f );
	}

	//the y a sprite's body ("matrix") or shadow ("shadowMatrix") is drawn at, made as a frame makes it
	private static float drawn( CharSprite s, String matrix ) throws Exception {
		Method m = Visual.class.getDeclaredMethod( "updateMatrix" );
		m.setAccessible( true );
		m.invoke( s );
		return ((float[]) inherited( s, matrix ))[13];
	}

	//a field of the object's class or of any class above it
	private static Object inherited( Object o, String name ){
		for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()){
			try {
				Field f = c.getDeclaredField( name );
				f.setAccessible( true );
				return f.get( o );
			} catch (NoSuchFieldException e){
				//it is further up
			} catch (IllegalAccessException e){
				throw new AssertionError( e );
			}
		}
		throw new AssertionError( "no field " + name );
	}

	@Test
	public void aVultureLeavesFeathersAndBundles(){
		Vulture v = put( new Vulture(), at( 6, 6 ) );
		assertTrue( v.flying );
		hero.HP = hero.HT / 5;
		v.state.act( false, false );
		Bundle bundle = new Bundle();
		v.storeInBundle( bundle );
		Vulture back = new Vulture();
		back.restoreFromBundle( bundle );
		assertEquals( Char.Alignment.ENEMY, back.alignment );
		assertEquals( hero.id(), get( back, "prey" ) );
		for (String k : new String[]{ "home_x", "home_y", "prey", "scared", "eating", "fleeing", "climb", "clockwise" }){
			bundle.remove( k );
		}
		bundle.put( "state", "WANDERING" );
		Vulture old = new Vulture();
		old.restoreFromBundle( bundle );
		assertEquals( Char.Alignment.NEUTRAL, old.alignment );
		assertEquals( -1, get( old, "prey" ) );

		Random.pushGenerator( 1 );
		try {
			v.die( null );
		} finally {
			Random.popGenerator();
		}
		int feathers = 0;
		for (Heap h : level.heaps.valueList()) for (Item i : h.items) if (i instanceof VultureFeather) feathers += i.quantity();
		assertTrue( "one or two feathers: " + feathers, feathers >= 1 && feathers <= 2 );
	}

	// ------------------------------------------------------------------ the alligator

	@Test
	public void anAlligatorBurstsOutAndDragsWhoStepsBesideIt() throws Exception {
		//a pool two cells long, the hero's ally on the bank at its west end, the hero far off
		water( at( 8, 6 ), at( 9, 6 ) );
		Alligator g = put( new Alligator(), at( 8, 6 ) );
		assertTrue( g.lurking() );
		assertEquals( Char.Alignment.NEUTRAL, g.alignment );
		Rat ally = victim( at( 7, 6 ), Char.Alignment.ALLY );
		ally.defenseSkill = 1000;
		assertTrue( act( g ) );
		assertTrue( "the bite never misses, however nimble", ally.HP < ally.HT );
		assertEquals( Char.Alignment.ENEMY, g.alignment );
		assertSame( g.HUNTING, g.state );
		assertEquals( "it backs into the water behind it", at( 9, 6 ), g.pos );
		Pushing p = pushing();
		assertNotNull( "and drags him", p );
		land( p );
		assertEquals( "into the water", at( 8, 6 ), ally.pos );
		assertTrue( level.water[ally.pos] );
		//its bites from then on are no ambush: they can miss
		assertTrue( g.attackSkill( ally ) < Char.INFINITE_ACCURACY );
	}

	@Test
	public void noDragWithNoWaterBehindItOrForAGuest() throws Exception {
		water( at( 8, 6 ) );
		Alligator g = put( new Alligator(), at( 8, 6 ) );
		Rat ally = victim( at( 7, 6 ), Char.Alignment.ALLY );
		assertEquals( -1, Alligator.backInto( level, at( 8, 6 ), ally.pos ) );
		assertTrue( act( g ) );
		assertEquals( at( 8, 6 ), g.pos );
		assertNull( pushing() );

		//a guest's hero beside a pool with room behind it: bitten, never dragged by the host
		water( at( 14, 6 ), at( 15, 6 ) );
		Hero guest = new Hero();
		guest.HP = guest.HT = 500;
		guest.pos = at( 13, 6 );
		guest.sprite = stub();
		guest.damageInterrupt = false;
		Alligator h = put( new Alligator(), at( 14, 6 ) );
		TestParty.join( guest );
		try {
			hero.pos = at( 20, 12 );
			assertTrue( act( h ) );
			assertTrue( guest.HP < 500 );
			assertEquals( at( 14, 6 ), h.pos );
			assertNull( pushing() );
		} finally {
			TestParty.leave();
		}
	}

	@Test
	public void theBackwaterIsTheOneFurthestFromThePrey(){
		water( at( 8, 6 ), at( 9, 6 ), at( 9, 5 ), at( 7, 7 ) );
		assertEquals( "the water furthest from him", at( 9, 5 ), Alligator.backInto( level, at( 8, 6 ), at( 7, 6 ) ) );
		assertEquals( at( 8, 6 ), Alligator.nearestWater( level, at( 8, 6 ), 3 ) );
		assertEquals( -1, Alligator.nearestWater( level, at( 20, 12 ), 3 ) );
	}

	@Test
	public void itGoesBackToTheWaterWhenItsPreyGetsAway(){
		water( at( 4, 4 ) );
		Alligator g = put( new Alligator(), at( 6, 4 ) );
		g.damage( 1, hero );
		assertSame( "struck while it lurks, it hunts its striker", g.HUNTING, g.state );
		hero.pos = at( 6 + Alligator.CHASE + 1, 4 );
		g.enemy = hero;
		for (int i = 0; i < 6 && !g.lurking(); i++) g.state.act( true, false );
		assertTrue( "back in the water, lurking", g.lurking() );
		assertEquals( at( 4, 4 ), g.pos );
		assertEquals( Char.Alignment.NEUTRAL, g.alignment );
	}

	@Test
	public void itLiesInWaitAsNobodysEnemyHoweverItCameToIt(){
		water( at( 8, 6 ), at( 9, 6 ) );
		Alligator g = put( new Alligator(), at( 8, 6 ) );
		g.damage( 1, hero );
		assertEquals( Char.Alignment.ENEMY, g.alignment );
		//Mob's hunt giving up on prey it cannot see or reach hands it back to its waiting, still his enemy
		hero.pos = at( 20, 12 );
		g.state = g.WANDERING;
		assertTrue( act( g ) );
		assertTrue( g.lurking() );
		assertEquals( Char.Alignment.NEUTRAL, g.alignment );
	}

	//its picture, set down as the game sets one down: the look of its ground from the first frame
	@Test
	public void anAlligatorSetDownInItsPoolShowsOnlyItsEyes(){
		Camera camera = Camera.main;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			water( at( 8, 6 ) );
			Alligator wet = new Alligator();
			wet.pos = at( 8, 6 );
			AlligatorSprite s = new AlligatorSprite();
			s.link( wet );
			assertSame( "lurking", inherited( s, "lurk" ), inherited( s, "curAnim" ) );
			assertFalse( "no shadow under the water", (Boolean) inherited( s, "renderShadow" ) );

			Alligator dry = new Alligator();
			dry.pos = at( 4, 6 );
			AlligatorSprite b = new AlligatorSprite();
			b.link( dry );
			assertSame( "basking on the bank", inherited( b, "bask" ), inherited( b, "curAnim" ) );
			assertTrue( (Boolean) inherited( b, "renderShadow" ) );
		} finally {
			Camera.main = camera;
		}
	}

	@Test
	public void anAlligatorLeavesMeatAndBundles(){
		water( at( 6, 6 ) );
		Alligator g = put( new Alligator(), at( 6, 6 ) );
		g.damage( 1, hero );
		Bundle bundle = new Bundle();
		g.storeInBundle( bundle );
		Alligator back = new Alligator();
		back.restoreFromBundle( bundle );
		assertSame( back.HUNTING, back.state );
		assertEquals( Char.Alignment.ENEMY, back.alignment );
		bundle.remove( "lost" );
		bundle.put( "state", "WANDERING" );
		Alligator old = new Alligator();
		old.restoreFromBundle( bundle );
		assertTrue( old.lurking() );
		assertEquals( Char.Alignment.NEUTRAL, old.alignment );

		g.die( null );
		int meat = 0;
		for (Heap h : level.heaps.valueList()) for (Item i : h.items) if (i instanceof MysteryMeat) meat += i.quantity();
		assertEquals( 1, meat );
		assertEquals( Hides.GATOR, Hides.chance( Alligator.class ), 0f );
	}

	// ------------------------------------------------------------------ the pike

	@Test
	public void aPikeDiesOutOfTheWaterAndGoesUnderTheIce(){
		//out-levelled: its meat is no prize for him
		hero.lvl = 40;
		Pike dry = put( new Pike(), at( 5, 5 ) );
		assertTrue( act( dry ) );
		assertFalse( "out of the water it dies", dry.isAlive() );

		water( at( 10, 5 ) );
		Pike under = put( new Pike(), at( 10, 5 ) );
		level.map[at( 10, 5 )] = Terrain.FROZEN_WATER;
		level.buildFlagMaps();
		assertTrue( act( under ) );
		assertFalse( "gone under the ice", level.mobs.contains( under ) );
		assertNull( "nothing left of it", level.heaps.get( at( 10, 5 ) ) );
	}

	@Test
	public void aPikeSwimsOnlyThroughWater(){
		water( at( 5, 5 ), at( 6, 5 ), at( 7, 5 ) );
		Pike p = put( new Pike(), at( 5, 5 ) );
		assertTrue( p.getCloser( at( 7, 5 ) ) );
		assertEquals( at( 6, 5 ), p.pos );
		assertTrue( p.getCloser( at( 7, 5 ) ) );
		assertEquals( at( 7, 5 ), p.pos );
		assertFalse( "no water on to the land", p.getCloser( at( 10, 5 ) ) );
		assertEquals( at( 7, 5 ), p.pos );
		//a fishing spear takes half of it at least
		FishingSpear spear = new FishingSpear();
		assertEquals( p.HP / 2, spear.proc( hero, p, 1 ) );
	}

	// ------------------------------------------------------------------ what they leave

	@Test
	public void theWorkbenchMakesTheBeastsPartsUseful(){
		StationRecipe darts = StationRecipes.byId( "darts" );
		assertNotNull( darts );
		assertSame( Workbench.class, darts.station );
		assertTrue( darts.output.sample() instanceof Dart );
		assertEquals( 2, darts.inputs.get( 0 ).quantity );
		assertTrue( darts.inputs.get( 0 ).kinds().contains( VultureFeather.class ) );
		assertTrue( darts.inputs.get( 1 ).kinds().contains( Plank.class ) );
		StationRecipe spears = StationRecipes.byId( "fishing_spears" );
		assertNotNull( spears );
		assertTrue( spears.output.sample() instanceof FishingSpear );
		assertTrue( spears.inputs.get( 0 ).kinds().contains( GatorTooth.class ) );

		assertEquals( Hides.BISON, Hides.chance( Bison.class ), 0f );
		assertEquals( Hides.CAMEL, Hides.chance( Camel.class ), 0f );
		assertEquals( 0f, Hides.chance( Vulture.class ), 0f );
	}
}
