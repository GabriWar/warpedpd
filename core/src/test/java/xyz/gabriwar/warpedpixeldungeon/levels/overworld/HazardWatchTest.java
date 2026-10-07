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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Breathless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CaveIn;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.StateSerializer;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Signal;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The dangers of the slices acting on real windows (HazardWatch): a firedamp pocket goes up
 * only after a warning and the hero's action, spends itself and stays spent through a save until
 * it fills again, never kills a hero who is not near death; a cave-in is marked and warned of a
 * turn before the rocks fall, follows a rebase, keeps off the ways and heaps only plain floor;
 * thin ice cracks, groans and gives, the crack surviving a save and healing with time; a gust is
 * warned of before it pushes and never pushes into a drop; Breathless stacks and clears; every
 * string resolves; an old save loads; a mirror changes nothing and draws its host's cracks.
 */
public class HazardWatchTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, depth, challenges;
	private float shift, temp, wind, windDir;
	private boolean shiftHeld;
	private DayNightCycle.Phase override;
	private Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
		//a badly hurt hero is interrupted whatever (Hero.damage), which resets the scene's key hold:
		//headless there is no scene, so a bare selector stands in for its one (resetKeyHold only
		//clears three fields)
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		if (sel.get( null ) == null){
			Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
			u.setAccessible( true );
			Object unsafe = u.get( null );
			Object bare = unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, CellSelector.class );
			sel.set( null, bare );
			standIn = true;
		}
	}

	private static boolean standIn;

	@AfterClass
	public static void unboot() throws Exception {
		if (!standIn) return;
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		sel.set( null, null );
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		depth = Dungeon.depth;
		challenges = Dungeon.challenges;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		override = DayNightCycle.debugPhaseOverride;
		temp = ClimateManager.debugTempOverride;
		wind = ClimateManager.debugWindOverride;
		windDir = ClimateManager.debugWindDirOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.challenges = 0;
		hero = new Hero();
		hero.lvl = 12;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.depth = depth;
		Dungeon.challenges = challenges;
		DayNightCycle.debugPhaseOverride = override;
		ClimateManager.debugTempOverride = temp;
		ClimateManager.debugWindOverride = wind;
		ClimateManager.debugWindDirOverride = windDir;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	//the live window of a slice centred on a world cell, built as a mirror's is and made the host's own
	private static OverworldLevel windowAt( int altitude, int wx, int wy, float s ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( "currentStamp" );
			m.setAccessible( true );
			set( ow, "seasonStamp", m.invoke( ow ) );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.level = ow;
		Dungeon.depth = WorldLayers.depthOf( altitude );
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	private static void set( Object o, String name, Object v ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private void place( Hero h, int cell ){
		h.pos = cell;
		Actor.add( h );
	}

	//the level as a real save has it: written to bytes, read back into a fresh level
	private static OverworldLevel saveAndLoad( OverworldLevel ow ) throws Exception {
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		Bundle read = Bundle.read( new ByteArrayInputStream( bytes( b ) ) );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( read );
		Dungeon.level = back;
		return back;
	}

	private static byte[] bytes( Bundle b ){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		return out.toByteArray();
	}

	//the log's lines while `run` runs
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

	private static boolean said( ArrayList<String> lines, Class<?> owner, String key ){
		String text = Messages.get( owner, key );
		for (String s : lines) if (s.contains( text )) return true;
		return false;
	}

	private static ArrayList<HazardWatch.Strike> strikes(){
		ArrayList<HazardWatch.Strike> out = new ArrayList<>();
		for (Actor a : Actor.all()) if (a instanceof HazardWatch.Strike) out.add( (HazardWatch.Strike) a );
		return out;
	}

	private static void runStrikes(){
		for (HazardWatch.Strike s : strikes()) s.act();
	}

	// ----------------------------------------------------------------- firedamp

	private OverworldLevel firedampWindow( int[] f ){
		OverworldLevel ow = windowAt( -6, f[0], f[1], 0f );
		assertTrue( ow.hazards().gasAt( ow.localCell( f[2], f[3] ) ) );
		return ow;
	}

	@Test
	public void igniteSpendsThePocketAndPersists() throws Exception {
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		assertNotNull( f );
		OverworldLevel ow = firedampWindow( f );
		int gas = ow.localCell( f[2], f[3] );
		//a hero in the gas with 40% of his health left
		place( hero, gas );
		hero.HP = Math.round( hero.HT * 0.4f );
		int before = hero.HP;
		ow.hazards().ignite( f[2], f[3] );
		assertFalse( ow.hazards().gasAt( gas ) );
		assertTrue( "he lives", hero.isAlive() );
		assertTrue( before - hero.HP <= Math.round( hero.HT * 0.35f ) );
		Blob fire = ow.blobs.get( Fire.class );
		assertNotNull( fire );
		assertTrue( fire.volume > 0 );
		assertEquals( "no fire under him", 0, fire.cur[gas] );

		OverworldLevel back = saveAndLoad( ow );
		assertFalse( "spent through a save", back.hazards().gasAt( back.localCell( f[2], f[3] ) ) );
		Dungeon.cycleTurn += LayerHazards.REFILL;
		assertTrue( "filled again", back.hazards().gasAt( back.localCell( f[2], f[3] ) ) );
	}

	@Test
	public void torchNeedsAWarningTurn(){
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		OverworldLevel ow = firedampWindow( f );
		int gas = ow.localCell( f[2], f[3] );
		place( hero, gas );
		Light.burn( hero, 100f );
		ArrayList<String> first = logged( () -> ow.hazards().heroTurnAt( hero, 1f, 0.99f ) );
		assertTrue( said( first, HazardWatch.class, "firedamp_smell" ) );
		assertTrue( said( first, HazardWatch.class, "firedamp_flame" ) );
		assertTrue( "nothing armed on the warning turn", strikes().isEmpty() );
		assertTrue( ow.hazards().gasAt( gas ) );
		//a turn on, still in the gas with his torch: it goes up after his action
		ow.hazards().heroTurnAt( hero, 2f, 0.99f );
		assertEquals( 1, strikes().size() );
		assertTrue( "nothing yet before he acts", ow.hazards().gasAt( gas ) );
		runStrikes();
		assertFalse( ow.hazards().gasAt( gas ) );
	}

	@Test
	public void aTorchCarriedOutInTimeSetsNothingOff(){
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		OverworldLevel ow = firedampWindow( f );
		int gas = ow.localCell( f[2], f[3] );
		place( hero, gas );
		Light.burn( hero, 100f );
		ow.hazards().heroTurnAt( hero, 1f, 0.99f );
		ow.hazards().heroTurnAt( hero, 2f, 0.99f );
		//his action that turn takes him out of the gas
		hero.pos = ow.localCell( f[0], f[1] );
		runStrikes();
		assertTrue( ow.hazards().gasAt( gas ) );
	}

	@Test
	public void heldFastNeitherFlameNorGustTakesHim(){
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		OverworldLevel ow = firedampWindow( f );
		int gas = ow.localCell( f[2], f[3] );
		place( hero, gas );
		Light.burn( hero, 100f );
		Buff.affect( hero, Roots.class, 10f );
		for (int t = 1; t <= 5; t++) ow.hazards().heroTurnAt( hero, t, 0.99f );
		assertTrue( "rooted with his torch, nothing is armed", strikes().isEmpty() );
		assertTrue( ow.hazards().gasAt( gas ) );

		OverworldLevel high = crest();
		int c = W / 2 + (H / 2) * W;
		Actor.clear();
		place( hero, c );
		Level.set( c + W + 1, Terrain.EMPTY, high );
		Buff.affect( hero, Roots.class, 10f );
		high.hazards().gustStrike( hero, 2 );
		assertEquals( "rooted, the gust does not move him", c, hero.pos );
		assertNotNull( hero.buff( Daze.class ) );
	}

	@Test
	public void lingeringMakesTheHeadSwim(){
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		OverworldLevel ow = firedampWindow( f );
		place( hero, ow.localCell( f[2], f[3] ) );
		ArrayList<String> lines = new ArrayList<>();
		for (int t = 1; t <= LayerHazards.DIZZY_TURNS + 1; t++){
			final float now = t;
			lines.addAll( logged( () -> ow.hazards().heroTurnAt( hero, now, 0.99f ) ) );
		}
		assertNotNull( hero.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo.class ) );
		assertTrue( said( lines, HazardWatch.class, "firedamp_dizzy" ) );
		assertTrue( strikes().isEmpty() );
	}

	@Test
	public void externalHeatHissesBeforeItBlows(){
		int[] f = LayerHazards.findFiredamp( SEED, -6, 40 );
		OverworldLevel ow = firedampWindow( f );
		int gas = ow.localCell( f[2], f[3] );
		//beside the gas, never in it: he has smelled nothing
		place( hero, ow.localCell( f[0], f[1] ) );
		hero.HP = Math.round( hero.HT * 0.4f );
		int before = hero.HP;
		ow.tileHeat[gas] = 60f;
		ArrayList<String> lines = logged( () -> ow.hazards().worldTurn() );
		assertTrue( said( lines, HazardWatch.class, "firedamp_hiss" ) );
		assertEquals( before, hero.HP );
		assertTrue( ow.hazards().gasAt( gas ) );
		ArrayList<HazardWatch.Strike> s = strikes();
		assertEquals( 1, s.size() );
		assertTrue( "after his next action", s.get( 0 ).cooldown() >= 1f );
		//armed once, not again while it burns down
		ow.hazards().worldTurn();
		assertEquals( 1, strikes().size() );
		runStrikes();
		assertFalse( ow.hazards().gasAt( gas ) );
		assertTrue( hero.isAlive() );
		assertTrue( before - hero.HP <= Math.round( hero.HT * 0.35f ) );
	}

	// ----------------------------------------------------------------- rockfall

	@Test
	public void rockfallTelegraphPrecedesDamage(){
		int[] l = LayerHazards.findLooseWall( SEED, -8, 200 );
		assertNotNull( l );
		OverworldLevel ow = windowAt( -8, l[0], l[1], 0f );
		int stand = ow.localCell( l[0], l[1] ), wall = stand + 1;
		assertEquals( Terrain.WALL, ow.map[wall] );
		place( hero, stand );
		Level.set( wall, Terrain.EMPTY_DECO, ow );
		int hp = hero.HP;
		ArrayList<String> lines = logged( () -> ow.hazards().collapse( hero, wall, true, 1f ) );
		assertTrue( said( lines, HazardWatch.class, "ceiling_groans" ) );
		assertEquals( "nothing has fallen yet", hp, hero.HP );
		CaveIn fall = hero.buff( CaveIn.class );
		assertNotNull( fall );
		assertTrue( "his action comes first", fall.cooldown() + fall.rest >= 1f );
		int[] rocks = fall.rockPositions();
		ArrayList<Integer> cells = new ArrayList<>();
		for (int r : rocks) cells.add( r );
		assertTrue( cells.contains( wall ) );
		boolean free = false;
		for (int d : PathFinder.NEIGHBOURS8){
			int c = hero.pos + d;
			if (ow.passable[c] && !ow.pit[c] && !cells.contains( c )) free = true;
		}
		assertTrue( "a cell to step to", free );
		for (int r : rocks){
			assertTrue( ow.passable[r] && !ow.pit[r] && !ow.water[r] );
		}

		//the rocks follow the ground through a rebase
		fall.translate( 32, 0, W, H );
		for (int i = 0; i < rocks.length; i++){
			if (rocks[i] % W - 32 > 0) assertTrue( Arrays.toString( fall.rockPositions() ), contains( fall.rockPositions(), rocks[i] - 32 ) );
		}

		//when they fall, a hero is hurt by a third of his health at most
		fall.affectChar( hero );
		assertTrue( hp - hero.HP <= Math.max( 1, hero.HT / 3 ) );
		assertTrue( hero.isAlive() );

		//the miner gone from the slice: nothing falls
		int hpNow = hero.HP;
		Dungeon.depth = WorldLayers.depthOf( -7 );
		fall.rest = 0f;
		fall.act();
		assertEquals( hpNow, hero.HP );
		assertNull( hero.buff( CaveIn.class ) );
	}

	private static boolean contains( int[] a, int v ){
		for (int x : a) if (x == v) return true;
		return false;
	}

	//a slowed or chilled miner's blow costs him more than a turn (Char.spend): the rocks wait until
	//his next turn has begun, so he always has it to step away in
	@Test
	public void aSlowMinerGetsHisTurnBeforeTheRocks(){
		for (Class<? extends Buff> slow : java.util.Arrays.<Class<? extends Buff>>asList(
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow.class,
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia.class )){
			Actor.clear();
			hero = new Hero();
			hero.lvl = 12;
			Dungeon.hero = hero;
			int[] l = LayerHazards.findLooseWall( SEED, -8, 200 );
			OverworldLevel ow = windowAt( -8, l[0], l[1], 0f );
			int stand = ow.localCell( l[0], l[1] ), wall = stand + 1;
			place( hero, stand );
			if (slow == xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow.class) Buff.affect( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow.class, 20f );
			else Buff.affect( hero, slow );
			Level.set( wall, Terrain.EMPTY_DECO, ow );
			ow.hazards().heroTurnAt( hero, 0f, 0.99f );
			//the blow, then its cost as Hero.actMine spends it
			ow.hazards().collapse( hero, wall, true, 1f );
			hero.spendAndNext( Actor.TICK );
			assertTrue( slow.getSimpleName() + " makes the blow dear", hero.cooldown() > 1f );
			CaveIn fall = hero.buff( CaveIn.class );
			assertNotNull( fall );
			int hp = hero.HP;
			//the halfway dust, then the end of the wait: he has not had his turn, so nothing falls
			fall.act();
			fall.act();
			assertSame( fall, hero.buff( CaveIn.class ) );
			assertEquals( hp, hero.HP );
			assertTrue( slow.getSimpleName() + ": not before his turn", fall.cooldown() >= hero.cooldown() );
			//his turn begins: now they come down after it
			ow.hazards().heroTurnAt( hero, hero.cooldown(), 0.99f );
			assertFalse( fall.waiting );
		}
	}

	@Test
	public void anImmobileMinerIsNeverUnderTheRocks(){
		int[] l = LayerHazards.findLooseWall( SEED, -8, 200 );
		OverworldLevel ow = windowAt( -8, l[0], l[1], 0f );
		int stand = ow.localCell( l[0], l[1] ), wall = stand + 1;
		place( hero, stand );
		Buff.affect( hero, Roots.class, 5f );
		Level.set( wall, Terrain.EMPTY_DECO, ow );
		ow.hazards().collapse( hero, wall, true, 1f );
		CaveIn fall = hero.buff( CaveIn.class );
		assertNotNull( fall );
		assertFalse( contains( fall.rockPositions(), stand ) );
	}

	@Test
	public void aGuestUnderTheRocksIsWarned() throws Exception {
		int[] l = LayerHazards.findLooseWall( SEED, -8, 200 );
		OverworldLevel ow = windowAt( -8, l[0], l[1], 0f );
		int stand = ow.localCell( l[0], l[1] ), wall = stand + 1;
		place( hero, stand );
		Level.set( wall, Terrain.EMPTY_DECO, ow );
		Hero guest = new Hero();
		int beyond = wall + 1;
		assertTrue( ow.passable[beyond] );
		guest.pos = beyond;
		Field q = StateSerializer.class.getDeclaredField( "pendingHeroLogMessages" );
		q.setAccessible( true );
		Collection<?> queue = (Collection<?>) q.get( null );
		TestParty.join( guest );
		try {
			queue.clear();
			ow.hazards().collapse( hero, wall, true, 1f );
			String text = Messages.get( HazardWatch.class, "ceiling_groans" );
			boolean told = false;
			for (Object m : queue){
				Field id = m.getClass().getDeclaredField( "heroId" ), t = m.getClass().getDeclaredField( "text" );
				id.setAccessible( true );
				t.setAccessible( true );
				if ((Integer) id.get( m ) == guest.id() && ((String) t.get( m )).contains( text )) told = true;
			}
			assertTrue( "the guest by the wall hears it", told );
		} finally {
			queue.clear();
			TestParty.leave();
		}
	}

	@Test
	public void rubbleOnlyOnPlainFloorAwayFromTheWays(){
		OverworldLevel ow = windowAt( -8, 0, 0, 0f );
		CaveIn fall = new CaveIn();
		int floors = 0, rubble = 0;
		for (int c = W + 1; c < ow.length() - W - 1; c++){
			int t = ow.map[c];
			boolean plain = t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.MUSHROOM_PATCH;
			if (!plain && t != Terrain.CHASM && t != Terrain.ENTRANCE && t != Terrain.EXIT && t != Terrain.WATER) continue;
			for (int i = 0; i < 6; i++) fall.affectCell( c );
			if (!plain) assertEquals( "rubble on " + t, t, ow.map[c] );
			else {
				floors++;
				if (ow.map[c] == Terrain.MINE_BOULDER) rubble++;
			}
		}
		assertTrue( rubble > 0 && rubble < floors );

		//a cave-in beside a way keeps off it and its sides
		OverworldLevel fresh = windowAt( -8, 0, 0, 0f );
		int way = -1;
		for (int c = 2 * W; c < fresh.length() - 2 * W && way == -1; c++){
			if (fresh.linkAt( c ) != WindowGenerator.LINK_NONE && c % W > 4 && c % W < W - 4) way = c;
		}
		assertNotEquals( "a ladder in the window", -1, way );
		Hero far = new Hero();
		place( far, fresh.localCell( fresh.worldX() + 20, fresh.worldY() + 20 ) );
		for (int d : PathFinder.NEIGHBOURS8){
			if (!fresh.passable[way + d]) continue;
			Actor.clear();
			far.buffs( CaveIn.class ).forEach( Buff::detach );
			place( far, far.pos );
			fresh.hazards().collapse( far, way + d, true, 1f );
			CaveIn ci = far.buff( CaveIn.class );
			if (ci == null) continue;
			for (int r : ci.rockPositions()){
				assertEquals( WindowGenerator.LINK_NONE, fresh.linkAt( r ) );
				for (int n : new int[]{ r - W, r + 1, r + W, r - 1 }) assertEquals( WindowGenerator.LINK_NONE, fresh.linkAt( n ) );
			}
		}
	}

	// ----------------------------------------------------------------- thin ice

	private static final float WINTER = WorldModel.calendarShift( GameCalendar.Season.WINTER, 0.5f );

	//a +2 window over a frozen tarn at -4C: the land cell beside thin ice at its centre
	private OverworldLevel tarnWindow(){
		ClimateManager.debugTempOverride = -4f;
		int[] at = LayerHazards.findThinIce( SEED, 2, WINTER, -4f, 30, 8 );
		assertNotNull( at );
		return windowAt( 2, at[0], at[1], WINTER );
	}

	private static int thinBeside( OverworldLevel ow, int land ){
		for (int n : new int[]{ land - W, land + 1, land + W, land - 1 }){
			if (LayerHazards.thinIce( ow.map, W, n, -4f )) return n;
		}
		throw new AssertionError( "no thin ice beside " + land );
	}

	@Test
	public void thinIceCracksThenBreaks() throws Exception {
		OverworldLevel ow = tarnWindow();
		int land = W / 2 + (H / 2) * W, ice = thinBeside( ow, land );
		place( hero, ice );
		hero.bodyTemp = 10f;
		ArrayList<String> lines = logged( () -> ow.hazards().stepped( hero ) );
		assertTrue( ow.hazards().crackedAt( ice ) );
		assertTrue( said( lines, HazardWatch.class, "ice_crack" ) );
		assertEquals( Messages.get( HazardWatch.class, "cracked_name" ), ow.tileNameAt( ice ) );
		ow.hazards().heroTurnAt( hero, 1f, 0.99f );
		lines = logged( () -> ow.hazards().heroTurnAt( hero, 2f, 0.99f ) );
		assertTrue( said( lines, HazardWatch.class, "ice_groan" ) );
		assertEquals( 2, ow.hazards().cracks().stageOf( OverworldLevel.worldKey( ow.worldX() + ice % W, ow.worldY() + ice / W ) ) );
		assertTrue( strikes().isEmpty() );
		assertEquals( Terrain.FROZEN_WATER, ow.map[ice] );
		ow.hazards().heroTurnAt( hero, 3f, 0.99f );
		assertEquals( 1, strikes().size() );
		runStrikes();
		assertEquals( Terrain.WATER, ow.map[ice] );
		assertNotNull( hero.buff( Drenched.class ) );
		assertNotNull( hero.buff( SoakedShoes.class ) );
		assertTrue( hero.bodyTemp <= 10f - LayerHazards.PLUNGE_CHILL );
		assertFalse( ow.hazards().crackedAt( ice ) );

		//the hole is the player's doing: it is in the save
		int wx = ow.worldX() + ice % W, wy = ow.worldY() + ice / W;
		OverworldLevel back = saveAndLoad( ow );
		assertEquals( Terrain.WATER, back.map[back.localCell( wx, wy )] );
	}

	@Test
	public void stepBackOnACrackAndItGives(){
		OverworldLevel ow = tarnWindow();
		int land = W / 2 + (H / 2) * W, ice = thinBeside( ow, land );
		place( hero, ice );
		ow.hazards().stepped( hero );
		hero.pos = land;
		hero.pos = ice;
		ow.hazards().stepped( hero );
		assertEquals( Terrain.WATER, ow.map[ice] );
	}

	@Test
	public void heldFastTheIceWaits(){
		OverworldLevel ow = tarnWindow();
		int land = W / 2 + (H / 2) * W, ice = thinBeside( ow, land );
		place( hero, ice );
		ow.hazards().stepped( hero );
		Buff.affect( hero, Roots.class, 10f );
		for (int t = 1; t <= 6; t++) ow.hazards().heroTurnAt( hero, t, 0.99f );
		assertTrue( strikes().isEmpty() );
		assertEquals( Terrain.FROZEN_WATER, ow.map[ice] );
	}

	//held on groaning ice, he is told once, and the guests are not sent the cracks every turn
	@Test
	public void heldOnGroaningIceItGroansOnce(){
		OverworldLevel ow = tarnWindow();
		int land = W / 2 + (H / 2) * W, ice = thinBeside( ow, land );
		place( hero, ice );
		ow.hazards().stepped( hero );
		ow.hazards().heroTurnAt( hero, 1f, 0.99f );
		ArrayList<String> lines = logged( () -> ow.hazards().heroTurnAt( hero, 2f, 0.99f ) );
		assertTrue( said( lines, HazardWatch.class, "ice_groan" ) );
		long sig = ow.sharedHazardsSig();
		Buff.affect( hero, Roots.class, 10f );
		lines = logged( () -> {
			for (int t = 3; t <= 8; t++) ow.hazards().heroTurnAt( hero, t, 0.99f );
		} );
		assertFalse( said( lines, HazardWatch.class, "ice_groan" ) );
		assertEquals( sig, ow.sharedHazardsSig() );
		assertTrue( strikes().isEmpty() );
		assertEquals( Terrain.FROZEN_WATER, ow.map[ice] );
	}

	//the ice is judged by the air over it, in its own biome, not by the host hero's
	@Test
	public void theIceFeelsItsOwnBiomesAir(){
		ClimateManager.debugTempOverride = Float.NaN;
		int cold = -1, warm = -1;
		OverworldLevel ow = null;
		for (int[] o : new int[][]{ { 0, 0 }, { 600, 0 }, { 0, 600 }, { -600, -600 }, { 1200, 300 }, { -900, 900 } }){
			ow = windowAt( 0, o[0], o[1], WINTER );
			cold = warm = -1;
			for (int c = W + 1; c < ow.length() - W - 1; c += 7){
				WorldModel.Biome b = ow.biomeAtCell( c );
				if (b == WorldModel.Biome.SNOWFIELD || b == WorldModel.Biome.TUNDRA) cold = c;
				if (b == WorldModel.Biome.DESERT || b == WorldModel.Biome.SWAMP || b == WorldModel.Biome.BEACH) warm = c;
			}
			if (cold != -1 && warm != -1) break;
		}
		assertTrue( "a window with a cold and a warm biome", cold != -1 && warm != -1 );
		assertTrue( ClimateManager.airAt( ow, cold ) + 10f < ClimateManager.airAt( ow, warm ) );
		//caves have no biome weather
		OverworldLevel cave = windowAt( -3, 0, 0, 0f );
		assertEquals( ClimateManager.airAt( cave, W + 1 ), ClimateManager.airAt( cave, cave.length() - W - 2 ), 0f );
		ClimateManager.debugTempOverride = -7f;
		assertEquals( -7f, ClimateManager.airAt( ow, warm ), 0f );
	}

	@Test
	public void deepColdIceBearsAnyone(){
		OverworldLevel ow = tarnWindow();
		ClimateManager.debugTempOverride = -12f;
		int land = W / 2 + (H / 2) * W, ice = thinBeside( ow, land );
		place( hero, ice );
		ow.hazards().stepped( hero );
		assertFalse( ow.hazards().crackedAt( ice ) );
	}

	//thin ice is a danger of the slices: the surface's winter rivers bear anyone, however mild
	@Test
	public void theSurfacesIceNeverCracks(){
		ClimateManager.debugTempOverride = -3f;
		int[] at = LayerHazards.findThinIce( SEED, 0, WINTER, -3f, 30, 8 );
		assertNotNull( at );
		OverworldLevel ow = windowAt( 0, at[0], at[1], WINTER );
		int land = W / 2 + (H / 2) * W, ice = -1;
		for (int n : new int[]{ land - W, land + 1, land + W, land - 1 }){
			if (LayerHazards.thinIce( ow.map, W, n, -3f ) && !ow.inTown( n ) && !ow.hotSpring( n )) ice = n;
		}
		assertTrue( "ice a slice would call thin", ice != -1 );
		place( hero, ice );
		ow.hazards().stepped( hero );
		ow.hazards().heroTurnAt( hero, 1f, 0.99f );
		ow.hazards().stepped( hero );
		for (int t = 2; t <= 5; t++) ow.hazards().heroTurnAt( hero, t, 0.99f );
		assertFalse( ow.hazards().crackedAt( ice ) );
		assertTrue( strikes().isEmpty() );
		assertEquals( Terrain.FROZEN_WATER, ow.map[ice] );
	}

	@Test
	public void cracksPersistAndHeal() throws Exception {
		OverworldLevel ow = tarnWindow();
		int land = W / 2 + (H / 2) * W, ice = thinBeside( ow, land );
		place( hero, ice );
		ow.hazards().stepped( hero );
		int wx = ow.worldX() + ice % W, wy = ow.worldY() + ice / W;
		hero.pos = land;
		OverworldLevel back = saveAndLoad( ow );
		int c = back.localCell( wx, wy );
		assertTrue( "through a save", back.hazards().crackedAt( c ) );
		Dungeon.cycleTurn += LayerHazards.CRACK_HEAL;
		for (int i = 0; i < 64; i++) back.hazards().worldTurn();
		assertFalse( "frozen over again", back.hazards().crackedAt( c ) );
	}

	// ----------------------------------------------------------------- the heights

	//a +9 window with a crest laid round its centre: drops west, east and to the north-east and
	//south-east, ground north, south and on the western diagonals
	private OverworldLevel crest(){
		ClimateManager.debugTempOverride = 0f;
		int[] r = LayerHazards.findRidge( SEED, 9, WINTER, 30, 8 );
		assertNotNull( r );
		OverworldLevel ow = windowAt( 9, r[0], r[1], WINTER );
		int c = W / 2 + (H / 2) * W;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++) Level.set( c + dx + dy * W, Terrain.EMPTY, ow );
		}
		for (int n : new int[]{ c - 1, c + 1, c - W + 1, c + W + 1 }) Level.set( n, Terrain.CHASM, ow );
		assertTrue( LayerHazards.ridgeCell( ow.map, W, c ) );
		return ow;
	}

	@Test
	public void gustWarnsBeforeItPushes(){
		OverworldLevel ow = crest();
		int c = W / 2 + (H / 2) * W;
		place( hero, c );
		ClimateManager.debugWindOverride = 20f;
		ClimateManager.debugWindDirOverride = 90f;
		ArrayList<String> lines = logged( () -> ow.hazards().heroTurnAt( hero, 1f, 0f ) );
		assertTrue( said( lines, HazardWatch.class, "gust_warn" ) );
		assertEquals( "the warning moves nobody", c, hero.pos );
		assertEquals( 1, strikes().size() );
		//east, and both ways 45 degrees off it, are the drop: he staggers where he stands
		lines = logged( () -> ow.hazards().gustStrike( hero, 2 ) );
		assertTrue( said( lines, HazardWatch.class, "gust_brace" ) );
		assertEquals( c, hero.pos );
		assertNotNull( hero.buff( Daze.class ) );
		//quiet for a few of his turns after a gust, counted in turns: a save's Actor.fixTime turning
		//the clock back does not stretch it
		Actor.clear();
		place( hero, c );
		for (int t = 0; t < HazardWatch.GUST_QUIET; t++){
			final float now = 0.5f + t;
			ow.hazards().heroTurnAt( hero, now, 0f );
			assertTrue( strikes().isEmpty() );
		}
		ow.hazards().heroTurnAt( hero, 0.5f + HazardWatch.GUST_QUIET, 0f );
		assertEquals( 1, strikes().size() );
		//with ground to the south-east he is pushed there, along the ridge
		Level.set( c + W + 1, Terrain.EMPTY, ow );
		ow.hazards().gustStrike( hero, 2 );
		assertEquals( c + W + 1, hero.pos );
		assertFalse( ow.pit[hero.pos] );
	}

	@Test
	public void noGustBelowTheHeightsOrOffTheRidge(){
		OverworldLevel ow = crest();
		int c = W / 2 + (H / 2) * W;
		place( hero, c + 2 * W );
		ClimateManager.debugWindOverride = 30f;
		for (int t = 1; t <= 20; t++) ow.hazards().heroTurnAt( hero, t, 0f );
		assertTrue( strikes().isEmpty() );
	}

	@Test
	public void breathlessStacksAndClears(){
		OverworldLevel high = windowAt( 9, 0, 0, 0f );
		Buff.affect( hero, Hunger.class );
		place( hero, W / 2 + (H / 2) * W );
		ArrayList<String> lines = logged( () -> {
			high.hazards().heroTurnAt( hero, 1f, 0.99f );
			high.hazards().heroTurnAt( hero, 2f, 0.99f );
		} );
		assertTrue( said( lines, Breathless.class, "onset" ) );
		assertEquals( 1, hero.buffs( Breathless.class ).size() );
		Breathless b = hero.buff( Breathless.class );
		assertEquals( 1, b.stacks );
		int hunger = hero.buff( Hunger.class ).hunger();
		for (int i = 0; i < LayerHazards.stackEvery( 9 ); i++) b.act();
		assertEquals( 2, b.stacks );
		for (int i = 0; i < 400; i++) b.act();
		assertEquals( LayerHazards.maxStacks( 9 ), b.stacks );
		assertEquals( 0.4f, b.regenFactor(), 0.001f );
		assertTrue( hero.buff( Hunger.class ).hunger() > hunger );
		assertFalse( b.desc().contains( Messages.NO_TEXT_FOUND ) );

		//resting by a fire up here eases it two turns a stack
		Level.set( hero.pos + 1, Terrain.EMBERS, high );
		hero.resting = true;
		for (int i = 0; i < 4; i++) b.act();
		assertEquals( LayerHazards.maxStacks( 9 ) - 2, b.stacks );
		hero.resting = false;

		//down below, a stack every eight turns, and gone at none
		windowAt( 3, 0, 0, 0f );
		lines = logged( () -> {
			for (int i = 0; i < 8 * LayerHazards.maxStacks( 9 ); i++) if (hero.buff( Breathless.class ) != null) b.act();
		} );
		assertNull( hero.buff( Breathless.class ) );
		assertTrue( said( lines, Breathless.class, "better" ) );
	}

	// ----------------------------------------------------------------- the rest

	@Test
	public void stringsResolve(){
		for (String k : new String[]{ "firedamp_smell", "firedamp_flame", "firedamp_dizzy", "firedamp_hiss", "firedamp_ignite",
				"ice_crack", "ice_groan", "ice_break", "cracked_name", "cracked_desc", "ceiling_groans",
				"gust_warn", "gust_hit", "gust_brace" }){
			assertFalse( k, Messages.get( HazardWatch.class, k ).contains( Messages.NO_TEXT_FOUND ) );
		}
		for (String k : new String[]{ "name", "ondeath", "rankings_desc" }){
			assertFalse( k, Messages.get( Firedamp.class, k ).contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( k, Messages.get( CaveIn.class, k ).contains( Messages.NO_TEXT_FOUND ) );
		}
		for (String k : new String[]{ "name", "onset", "worse", "better" }){
			assertFalse( k, Messages.get( Breathless.class, k ).contains( Messages.NO_TEXT_FOUND ) );
		}
		assertFalse( Messages.get( Breathless.class, "desc", 30, 2, 4 ).contains( Messages.NO_TEXT_FOUND ) );
	}

	@Test
	public void oldSavesLoad() throws Exception {
		OverworldLevel ow = tarnWindow();
		int land = W / 2 + (H / 2) * W, ice = thinBeside( ow, land );
		place( hero, ice );
		ow.hazards().stepped( hero );
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		for (String k : new String[]{ "hz_spent_keys", "hz_spent_until", "hz_crack_keys", "hz_crack_at" }){
			assertTrue( k, b.remove( k ) );
		}
		Bundle read = Bundle.read( new ByteArrayInputStream( bytes( b ) ) );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( read );
		Dungeon.level = back;
		assertEquals( 0, back.hazards().cracks().keys.length );

		//the new buffs read from a bundle with none of their own keys
		Bundle empty = new Bundle();
		Breathless br = new Breathless();
		br.restoreFromBundle( empty );
		assertEquals( 1, br.stacks );
		CaveIn ci = new CaveIn();
		ci.restoreFromBundle( empty );
		assertEquals( -1, ci.depth );
		assertEquals( 0, ci.deep );
	}

	@Test
	public void aMirrorChangesNothingAndDrawsItsHostsCracks(){
		OverworldLevel host = tarnWindow();
		int land = W / 2 + (H / 2) * W, ice = thinBeside( host, land );
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 2, host.worldX(), host.worldY(), WINTER );
		OverworldLevel mirror = OverworldLevel.forNetwork( 2, SEED, host.worldX(), host.worldY(), WINTER, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( mirror );
		assertTrue( mirror.isMirror() );
		Dungeon.level = mirror;
		place( hero, ice );
		mirror.hazards().stepped( hero );
		mirror.hazards().heroTurnAt( hero, 1f, 0f );
		mirror.hazards().worldTurn();
		mirror.hazards().mined( hero, ice, Terrain.WALL );
		assertFalse( mirror.hazards().crackedAt( ice ) );
		assertEquals( Terrain.FROZEN_WATER, mirror.map[ice] );
		assertTrue( strikes().isEmpty() );
		assertNull( hero.buff( CaveIn.class ) );

		Dungeon.level = host;
		host.hazards().stepped( hero );
		assertTrue( host.hazards().crackedAt( ice ) );
		mirror.adoptSharedHazards( host.sharedHazards() );
		assertTrue( mirror.hazards().crackedAt( ice ) );
		assertEquals( host.sharedHazardsSig(), mirror.sharedHazardsSig() );
	}
}
