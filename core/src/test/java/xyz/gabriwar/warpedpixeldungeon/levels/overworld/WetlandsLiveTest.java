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
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BogBlessing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MarshLight;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ParasiticInfection;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Wading;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DrownedAcolyte;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.PearlOyster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SunkenIdol;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SwampWitch;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.WitchCauldron;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfAccuracy;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.BogLantern;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.Pearl;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.Seashell;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.WadingBalm;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.WitchsRemedy;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DrownedAcolyteSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PearlOysterSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SunkenIdolSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SwampWitchSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.WitchCauldronSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The wetlands' places live on a real surface window (built as a mirror's is, then made the host's
 * own): the witch, her cauldron and the idol set down once through a save, and the witch never again
 * once she fled; the idol's offering once in a few days and its blessing; the bog dragging at a
 * wader and the mist thickening the fog; the acolytes rising once a night and sinking by day; the
 * tide laying the pools' finds and taking them back, their stock kept through a save; the places
 * found when seen; the witch's pot; every string; old saves.
 */
public class WetlandsLiveTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, turn, startDay, gold;
	private DayNightCycle.Phase override;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		gold = Dungeon.gold;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = null;
		ClimateManager.setLocalFog( false );
		hero = new Hero();
		hero.lvl = 20;
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.gold = gold;
		DayNightCycle.debugPhaseOverride = override;
		ClimateManager.setLocalFog( false );
		WorldModel.releaseSeasonShift();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

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
			@Override public void operate( int cell ){ }
		};
		sp.visible = false;
		return sp;
	}

	private static void setField( Object o, String name, Object v ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
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

	private static WetlandSites.Place nearest( WorldStructures.Site kind ){
		WetlandSites.Place p = WetlandSites.nearest( SEED, kind, 0, 0, 40 );
		assertNotNull( kind.name(), p );
		return p;
	}

	//the live surface window centred on a place, the hero on (wx, wy) or the nearest open cell
	private OverworldLevel windowAt( WetlandSites.Place p, int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = p.cx - W / 2, oy = p.cy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, 0f );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, 0f, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.level = ow;
		Dungeon.depth = WorldLayers.depthOf( 0 );
		hero.pos = -1;
		for (int r = 0; r <= 8 && hero.pos == -1; r++){
			for (int dy = -r; dy <= r && hero.pos == -1; dy++){
				for (int dx = -r; dx <= r && hero.pos == -1; dx++){
					int cell = ow.localCell( wx + dx, wy + dy );
					if (cell != -1 && ow.passable[cell] && !ow.water[cell] && !ow.occupied( cell )) hero.pos = cell;
				}
			}
		}
		assertTrue( hero.pos != -1 );
		return ow;
	}

	private static OverworldLevel saveAndLoad( OverworldLevel ow ){
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		//a headless Game carries no version code: stamp the save as a current one
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( b );
		Dungeon.level = back;
		return back;
	}

	private static long keyOf( Mob m ){
		if (m instanceof SwampWitch) return ((SwampWitch) m).siteKey;
		if (m instanceof WitchCauldron) return ((WitchCauldron) m).siteKey;
		if (m instanceof SunkenIdol) return ((SunkenIdol) m).siteKey;
		if (m instanceof DrownedAcolyte) return ((DrownedAcolyte) m).shrineKey;
		if (m instanceof PearlOyster) return ((PearlOyster) m).poolsKey;
		return Long.MIN_VALUE;
	}

	//the place's mobs of a kind, standing in the window or parked
	private static <T extends Mob> ArrayList<T> of( OverworldLevel ow, Class<T> kind, long key ){
		ArrayList<T> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (kind.isInstance( m ) && keyOf( m ) == key) out.add( kind.cast( m ) );
		for (Mob m : ow.parked()) if (kind.isInstance( m ) && keyOf( m ) == key) out.add( kind.cast( m ) );
		return out;
	}

	private static int cell( OverworldLevel ow, int[] wxy ){
		return ow.localCell( wxy[0], wxy[1] );
	}

	//the first turn from `from` on with the tide out (or in)
	private static int tideFrom( int from, boolean out ){
		int t = from;
		while (WetlandSites.tideOut( t ) != out) t++;
		return t;
	}

	// ------------------------------------------------------------------ their folk

	@Test
	public void theFolkAreSetDownOnceThroughASave(){
		WetlandSites.Place hut = nearest( WorldStructures.Site.WITCH_HUT );
		OverworldLevel ow = windowAt( hut, hut.cx, hut.cy + WetlandSites.WALK + 2 );
		ow.wetlands().populate();
		ow.wetlands().populate();
		assertEquals( 1, of( ow, SwampWitch.class, hut.key() ).size() );
		assertEquals( 1, of( ow, WitchCauldron.class, hut.key() ).size() );
		assertEquals( cell( ow, WetlandSites.witchCell( hut ) ), of( ow, SwampWitch.class, hut.key() ).get( 0 ).pos );
		assertEquals( cell( ow, WetlandSites.cauldronCell( hut ) ), of( ow, WitchCauldron.class, hut.key() ).get( 0 ).pos );
		//her house is the villages' kind: its door, its walls, the boardwalk out
		assertEquals( Terrain.DOOR, ow.map[ow.localCell( hut.cx, hut.cy + 2 )] );
		assertEquals( Terrain.BRIDGE, ow.map[ow.localCell( hut.cx, hut.cy + 4 )] );

		OverworldLevel back = saveAndLoad( ow );
		back.wetlands().populate();
		assertEquals( 1, of( back, SwampWitch.class, hut.key() ).size() );
		assertEquals( 1, of( back, WitchCauldron.class, hut.key() ).size() );

		//driven off, she never comes back to it; her cauldron stays
		SwampWitch witch = of( back, SwampWitch.class, hut.key() ).get( 0 );
		witch.flee();
		assertTrue( back.clearedSites().contains( hut.key() ) );
		back.wetlands().populate();
		assertEquals( 0, of( back, SwampWitch.class, hut.key() ).size() );
		assertEquals( 1, of( back, WitchCauldron.class, hut.key() ).size() );

		WetlandSites.Place shrine = nearest( WorldStructures.Site.SUNKEN_SHRINE );
		ow = windowAt( shrine, shrine.cx, shrine.cy + 6 );
		ow.wetlands().populate();
		ow.wetlands().populate();
		assertEquals( 1, of( ow, SunkenIdol.class, shrine.key() ).size() );
		assertEquals( cell( ow, WetlandSites.idolCell( shrine ) ), of( ow, SunkenIdol.class, shrine.key() ).get( 0 ).pos );
		back = saveAndLoad( ow );
		back.wetlands().populate();
		assertEquals( 1, of( back, SunkenIdol.class, shrine.key() ).size() );

		//nothing of the wild is set down in a hut or a shrine; a beach's pools are open ground
		assertTrue( back.wetlands().inBerth( back.localCell( shrine.cx + 2, shrine.cy ) ) );
		assertFalse( back.wetlands().inBerth( back.localCell( shrine.cx + WetlandSites.REACH + 1, shrine.cy ) ) );
	}

	// ------------------------------------------------------------------ the shrine

	@Test
	public void theIdolTakesAnOfferingOnceInAFewDays(){
		Dungeon.cycleTurn = 10 * DayNightCycle.FULL_CYCLE + 600;
		SunkenIdol idol = SunkenIdol.of( 7L );
		assertEquals( 0, idol.waitDays() );

		//too poor: nothing taken
		Dungeon.gold = WetlandSites.OFFER_GOLD - 1;
		assertFalse( idol.offerGold( hero ) );
		assertEquals( WetlandSites.OFFER_GOLD - 1, Dungeon.gold );
		assertNull( hero.buff( BogBlessing.class ) );

		Dungeon.gold = 500;
		assertTrue( idol.offerGold( hero ) );
		assertEquals( 500 - WetlandSites.OFFER_GOLD, Dungeon.gold );
		BogBlessing blessing = hero.buff( BogBlessing.class );
		assertNotNull( blessing );
		assertEquals( WetlandSites.BLESS_TURNS, blessing.cooldown(), 1f );
		assertEquals( WetlandSites.SHRINE_DAYS, idol.waitDays() );

		//once in a few days
		assertFalse( idol.offerGold( hero ) );
		assertEquals( 500 - WetlandSites.OFFER_GOLD, Dungeon.gold );
		Dungeon.cycleTurn += (WetlandSites.SHRINE_DAYS - 1) * DayNightCycle.FULL_CYCLE;
		assertEquals( 1, idol.waitDays() );
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		assertEquals( 0, idol.waitDays() );

		//an item: a shell is too little, a pearl is enough and buys a longer blessing
		Seashell shell = new Seashell();
		Pearl pearl = new Pearl();
		assertTrue( shell.collect( hero.belongings.backpack ) );
		assertTrue( pearl.collect( hero.belongings.backpack ) );
		assertFalse( SunkenIdol.offerable( shell, hero ) );
		assertTrue( SunkenIdol.offerable( pearl, hero ) );
		assertFalse( idol.offerItem( hero, shell ) );
		assertTrue( hero.belongings.contains( shell ) );
		Buff.detach( hero, BogBlessing.class );
		assertTrue( idol.offerItem( hero, pearl ) );
		assertFalse( hero.belongings.contains( pearl ) );
		blessing = hero.buff( BogBlessing.class );
		assertNotNull( blessing );
		assertEquals( WetlandSites.blessTurns( new Pearl().value() ), blessing.cooldown(), 1f );
		assertTrue( blessing.cooldown() > WetlandSites.BLESS_TURNS + 1 );
		assertEquals( WetlandSites.SHRINE_DAYS, idol.waitDays() );

		//kept through a save
		Bundle b = new Bundle();
		idol.storeInBundle( b );
		SunkenIdol again = new SunkenIdol();
		again.restoreFromBundle( b );
		assertEquals( 7L, again.siteKey );
		assertEquals( idol.lastOffer, again.lastOffer );
		assertEquals( WetlandSites.SHRINE_DAYS, again.waitDays() );
	}

	@Test
	public void theBlessingWashesTheBogOut() throws Exception {
		//(poison puffs on a live scene's cell as it takes: headless, the blessing's ward against it is
		//read off the hero's immunities)
		Buff.prolong( hero, ParasiticInfection.class, ParasiticInfection.DURATION );
		Buff.affect( hero, Ooze.class ).set( 20f );
		assertNotNull( hero.buff( ParasiticInfection.class ) );
		assertNotNull( hero.buff( Ooze.class ) );
		assertFalse( hero.isImmune( Poison.class ) );
		Buff.prolong( hero, BogBlessing.class, 100f ).laid( 100f );
		assertNull( hero.buff( ParasiticInfection.class ) );
		assertNull( "the parasite's ooze never takes", hero.buff( Ooze.class ) );
		//and none takes hold while it lasts
		for (Class<?> c : new Class<?>[]{ Poison.class, Ooze.class, ParasiticInfection.class }){
			assertTrue( c.getSimpleName(), hero.isImmune( c ) );
		}
		Buff.affect( hero, Ooze.class ).set( 20f );
		Buff.prolong( hero, ParasiticInfection.class, ParasiticInfection.DURATION );
		assertNull( hero.buff( Ooze.class ) );
		assertNull( hero.buff( ParasiticInfection.class ) );
		Buff.detach( hero, BogBlessing.class );
		assertFalse( hero.isImmune( Poison.class ) );

		//its length kept through a save, for the icon's fade
		BogBlessing b = new BogBlessing();
		b.laid( 800f );
		Bundle bundle = new Bundle();
		b.storeInBundle( bundle );
		BogBlessing again = new BogBlessing();
		again.restoreFromBundle( bundle );
		Field laid = BogBlessing.class.getDeclaredField( "laid" );
		laid.setAccessible( true );
		assertEquals( 800f, laid.getFloat( again ), 0.001f );
		bundle.remove( "laid" );
		BogBlessing old = new BogBlessing();
		old.restoreFromBundle( bundle );
		assertEquals( WetlandSites.BLESS_TURNS, laid.getFloat( old ), 0.001f );
	}

	@Test
	public void acolytesRiseOnceANightAndSinkByDay(){
		WetlandSites.Place shrine = nearest( WorldStructures.Site.SUNKEN_SHRINE );
		OverworldLevel ow = windowAt( shrine, shrine.cx, shrine.cy + 6 );
		long key = shrine.key();
		Dungeon.cycleTurn = 20 * DayNightCycle.FULL_CYCLE + 2400;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		ow.wetlands().tick( hero.pos );
		assertEquals( "none by day", 0, of( ow, DrownedAcolyte.class, key ).size() );

		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		ow.wetlands().tick( hero.pos );
		ArrayList<DrownedAcolyte> risen = of( ow, DrownedAcolyte.class, key );
		assertEquals( WetlandSites.guardCount( key, WorldClock.night() ), risen.size() );
		int home = ow.localCell( shrine.cx, shrine.cy );
		for (DrownedAcolyte a : risen){
			int r = ow.distance( a.pos, home );
			assertTrue( r >= 2 && r <= 3 );
			assertEquals( Terrain.WATER, ow.map[a.pos] );
			assertTrue( ow.distance( a.pos, hero.pos ) > 1 );
			assertEquals( shrine.cx, a.homeX );
			assertEquals( shrine.cy, a.homeY );
		}
		//once a night, through a save too
		ow.wetlands().tick( hero.pos );
		assertEquals( risen.size(), of( ow, DrownedAcolyte.class, key ).size() );
		OverworldLevel back = saveAndLoad( ow );
		back.wetlands().tick( hero.pos );
		ArrayList<DrownedAcolyte> kept = of( back, DrownedAcolyte.class, key );
		assertEquals( risen.size(), kept.size() );
		for (DrownedAcolyte a : kept){
			assertEquals( shrine.cx, a.homeX );
			assertEquals( shrine.cy, a.homeY );
		}
		//a real-clock night runs on past midnight: the next day of the same night raises none
		int night = WorldClock.night();
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE - Dungeon.cycleTurn % DayNightCycle.FULL_CYCLE + 100;
		assertEquals( night, WorldClock.night() );
		back.wetlands().tick( hero.pos );
		assertEquals( risen.size(), of( back, DrownedAcolyte.class, key ).size() );

		//sunrise: back under the water
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		for (DrownedAcolyte a : kept){
			a.sprite = stub();
			assertTrue( act( a ) );
		}
		assertEquals( 0, of( back, DrownedAcolyte.class, key ).size() );

		//a hero far off raises none on the next night
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int far = -1;
		for (int c = 0; c < back.length() && far == -1; c++){
			if (back.passable[c] && !back.water[c] && back.distance( c, home ) > Wetlands.GUARD_NEAR + 2
					&& back.distance( c, home ) < 40) far = c;
		}
		assertTrue( far != -1 );
		hero.pos = far;
		back.wetlands().tick( hero.pos );
		assertEquals( 0, of( back, DrownedAcolyte.class, key ).size() );
	}

	@Test
	public void anAcolytePastItsLeashWadesHomeAndSpendsItsTurn() throws Exception {
		WetlandSites.Place shrine = nearest( WorldStructures.Site.SUNKEN_SHRINE );
		OverworldLevel ow = windowAt( shrine, shrine.cx, shrine.cy + 6 );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int home = ow.localCell( shrine.cx, shrine.cy );
		//an open cell just past the leash, the hero on an open cell beside it further out still
		int at = -1, by = -1;
		for (int c = 0; c < ow.length() && at == -1; c++){
			int d = ow.distance( c, home );
			if (d <= DrownedAcolyte.LEASH || d > DrownedAcolyte.LEASH + 4 || !ow.passable[c] || ow.occupied( c )) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int m = c + n;
				if (m >= 0 && m < ow.length() && ow.passable[m] && !ow.occupied( m ) && ow.distance( m, home ) > d){
					at = c;
					by = m;
					break;
				}
			}
		}
		assertTrue( at != -1 );
		hero.pos = by;
		DrownedAcolyte a = DrownedAcolyte.of( shrine.key(), shrine.cx, shrine.cy );
		a.pos = at;
		a.sprite = stub();
		ow.mobs.add( a );
		a.state = a.HUNTING;
		//alerted, as noticing the hero leaves one: that used to turn it straight back to the hunt
		//without spending a moment, and the leash back to wandering, for ever
		Field alerted = Mob.class.getDeclaredField( "alerted" );
		alerted.setAccessible( true );
		alerted.setBoolean( a, true );
		assertTrue( act( a ) );
		assertEquals( a.WANDERING, a.state );
		assertTrue( "its turn spent", a.cooldown() > 0 );
		assertTrue( "it wades home", ow.distance( a.pos, home ) <= ow.distance( at, home ) );
	}

	// ------------------------------------------------------------------ the bog and the mist

	@Test
	public void theBogDragsAtWadersAndTheMistThickensTheFog(){
		WetlandSites.Place hut = nearest( WorldStructures.Site.WITCH_HUT );
		OverworldLevel ow = windowAt( hut, hut.cx, hut.cy + WetlandSites.WALK );
		int bog = -1, dry = -1;
		for (int dy = -12; dy <= 12; dy++){
			for (int dx = -12; dx <= 12; dx++){
				int c = ow.localCell( hut.cx + dx, hut.cy + dy );
				if (c == -1 || !ow.swampAt( c ) || ow.occupied( c )) continue;
				if (bog == -1 && ow.map[c] == Terrain.WATER) bog = c;
				if (dry == -1 && ow.passable[c] && !ow.water[c]) dry = c;
			}
		}
		assertTrue( "a swamp's water by the hut", bog != -1 );
		assertTrue( "a swamp's dry ground by the hut", dry != -1 );

		hero.pos = bog;
		assertEquals( WetlandSites.BOG_SPEED, WetlandSites.bogFactor( hero ), 0.0001f );
		Buff.affect( hero, Wading.class, 10f );
		assertEquals( 1f, WetlandSites.bogFactor( hero ), 0.0001f );
		Buff.detach( hero, Wading.class );
		Buff.affect( hero, BogBlessing.class, 10f );
		assertEquals( 1f, WetlandSites.bogFactor( hero ), 0.0001f );
		Buff.detach( hero, BogBlessing.class );
		hero.flying = true;
		assertEquals( 1f, WetlandSites.bogFactor( hero ), 0.0001f );
		hero.flying = false;
		hero.pos = dry;
		assertEquals( 1f, WetlandSites.bogFactor( hero ), 0.0001f );

		//the mist: a haze by day, fog of the place at dawn
		hero.pos = bog;
		Dungeon.cycleTurn = 30 * DayNightCycle.FULL_CYCLE + 100;
		assertEquals( DayNightCycle.Phase.DAWN, DayNightCycle.phase() );
		assertTrue( ow.localFog() );
		assertEquals( 3, WetlandSites.mistPenalty( ow, hero ) );
		//the fog it makes takes its two cells (DayNightCycle.viewDistanceModifier): never twice
		ClimateManager.setLocalFog( true );
		assertEquals( 1, WetlandSites.mistPenalty( ow, hero ) );
		//a bog lantern burns through the swamp's own fog
		Buff.affect( hero, MarshLight.class, 10f );
		assertEquals( -2, WetlandSites.mistPenalty( ow, hero ) );
		Buff.detach( hero, MarshLight.class );
		ClimateManager.setLocalFog( false );

		Dungeon.cycleTurn = 30 * DayNightCycle.FULL_CYCLE + 600;
		assertEquals( DayNightCycle.Phase.DAY, DayNightCycle.phase() );
		assertFalse( ow.localFog() );
		assertEquals( 1, WetlandSites.mistPenalty( ow, hero ) );
		//off the swamp, nothing
		int off = -1;
		for (int c = 0; c < ow.length() && off == -1; c++){
			if (ow.passable[c] && !ow.swampAt( c )) off = c;
		}
		assertTrue( off != -1 );
		hero.pos = off;
		assertEquals( 0, WetlandSites.mistPenalty( ow, hero ) );
		Dungeon.cycleTurn = 30 * DayNightCycle.FULL_CYCLE + 100;
		assertFalse( ow.localFog() );
	}

	// ------------------------------------------------------------------ the tide

	@Test
	public void theTideLaysThePoolsFindsAndTakesThemBack(){
		WetlandSites.Place p = nearest( WorldStructures.Site.TIDE_POOLS );
		OverworldLevel ow = windowAt( p, p.cx, p.cy + WetlandSites.REACH + 2 );
		long key = p.key();
		ArrayList<Integer> cells = new ArrayList<>();
		int[] xy = p.pools();
		for (int i = 0; i < xy.length; i += 2){
			int c = ow.localCell( xy[i], xy[i + 1] );
			assertEquals( Terrain.WATER, ow.map[c] );
			cells.add( c );
		}
		//by night (no crabs then), on a day whose low water, high water and low water again all fall in it
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int day = 30, out1, in1, out2, in2;
		while (true){
			out1 = tideFrom( day * DayNightCycle.FULL_CYCLE, true );
			in1 = tideFrom( out1, false );
			out2 = tideFrom( in1, true );
			in2 = tideFrom( out2, false );
			if (in2 < (day + 1) * DayNightCycle.FULL_CYCLE) break;
			day++;
		}

		Dungeon.cycleTurn = out1;
		ow.wetlands().tick( hero.pos );
		int laid = finds( ow, cells );
		assertEquals( Math.min( WetlandSites.POOL_CAP, cells.size() ), laid );
		assertEquals( WetlandSites.POOL_CAP - laid, ow.wetlands().stock( key )[0] );
		//a pearl oyster settles in a pool left free
		ArrayList<PearlOyster> oysters = of( ow, PearlOyster.class, key );
		assertEquals( cells.size() > laid ? 1 : 0, oysters.size() );
		for (PearlOyster o : oysters) assertTrue( cells.contains( o.pos ) );
		//the same low water lays nothing more
		ow.wetlands().tick( hero.pos );
		assertEquals( laid, finds( ow, cells ) );

		//the tide comes in and takes back what lies in the pools
		Dungeon.cycleTurn = in1;
		ow.wetlands().tick( hero.pos );
		assertEquals( 0, finds( ow, cells ) );
		assertEquals( WetlandSites.POOL_CAP, ow.wetlands().stock( key )[0] );

		//out again: what is picked up is gone from the pools till the sea brings more
		Dungeon.cycleTurn = out2;
		ow.wetlands().tick( hero.pos );
		int laid2 = finds( ow, cells );
		assertTrue( laid2 >= 1 );
		Heap picked = null;
		for (int c : cells) if (ow.heaps.get( c ) != null && picked == null) picked = ow.heaps.get( c );
		assertNotNull( picked );
		Item took = picked.pickUp();
		assertTrue( took instanceof Seashell || took instanceof Pearl );
		Dungeon.cycleTurn = in2;
		ow.wetlands().tick( hero.pos );
		assertEquals( 0, finds( ow, cells ) );
		assertEquals( WetlandSites.POOL_CAP - 1, ow.wetlands().stock( key )[0] );

		//kept through a save, and refilled the next day
		OverworldLevel back = saveAndLoad( ow );
		assertEquals( WetlandSites.POOL_CAP - 1, back.wetlands().stock( key )[0] );
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		assertEquals( WetlandSites.POOL_CAP, back.wetlands().stock( key )[0] );
	}

	//the heaps of nothing but the pools' finds on these cells
	private static int finds( OverworldLevel ow, ArrayList<Integer> cells ){
		int n = 0;
		for (int c : cells){
			Heap h = ow.heaps.get( c );
			if (h == null) continue;
			boolean only = !h.items.isEmpty();
			for (Item i : h.items) only &= i instanceof Seashell || i instanceof Pearl;
			if (only) n++;
		}
		return n;
	}

	@Test
	public void anOysterGivesUpItsPearlOrSnaps(){
		WetlandSites.Place p = nearest( WorldStructures.Site.TIDE_POOLS );
		OverworldLevel ow = windowAt( p, p.cx, p.cy + WetlandSites.REACH + 2 );
		int[] xy = p.pools();
		int pool = ow.localCell( xy[0], xy[1] );
		//one try in PINCH it snaps shut on the fingers (a hurt hero: no scene for that headless), the
		//rest it gives up its pearl; the first roll of the try decides
		int opened = 0, snaps = 0;
		for (int seed = 1; seed <= 60; seed++){
			Random.pushGenerator( seed );
			boolean snap = Random.Int( PearlOyster.PINCH ) == 0;
			Random.popGenerator();
			if (snap){
				snaps++;
				continue;
			}
			PearlOyster o = PearlOyster.of( p.key() );
			o.pos = pool;
			o.sprite = stub();
			ow.mobs.add( o );
			Random.pushGenerator( seed );
			try {
				assertTrue( o.interact( hero ) );
			} finally {
				Random.popGenerator();
			}
			assertFalse( ow.mobs.contains( o ) );
			Heap h = ow.heaps.get( pool );
			assertNotNull( h );
			assertTrue( h.peek() instanceof Pearl );
			h.destroy();
			opened++;
		}
		assertTrue( opened > snaps && snaps > 0 );
		//killed, it leaves its pearl too
		PearlOyster o = PearlOyster.of( p.key() );
		o.pos = pool;
		o.sprite = stub();
		ow.mobs.add( o );
		hero.lvl = 99;
		o.die( null );
		assertNotNull( ow.heaps.get( pool ) );
		assertTrue( ow.heaps.get( pool ).peek() instanceof Pearl );
	}

	// ------------------------------------------------------------------ found

	@Test
	public void placesAreFoundWhenSeenAndKeptThroughASave(){
		WetlandSites.Place hut = nearest( WorldStructures.Site.WITCH_HUT );
		OverworldLevel ow = windowAt( hut, hut.cx, hut.cy + WetlandSites.WALK + 2 );
		java.util.Arrays.fill( ow.visited, false );
		//pages write themselves once a hero has been down (GuideProgress.findPage)
		GuideProgress.clear();
		GuideProgress.descend();
		ow.wetlands().tick( hero.pos );
		assertFalse( ow.wetlands().found( hut.key() ) );
		ow.visited[ow.localCell( hut.cx, hut.cy + 2 )] = true;
		ow.wetlands().tick( hero.pos );
		assertTrue( ow.wetlands().found( hut.key() ) );
		assertTrue( GuideProgress.pageFound( "wet_witch" ) );
		//a hero standing in a swamp writes the swamps' page
		int swamp = -1;
		for (int c = 0; c < ow.length() && swamp == -1; c++) if (ow.passable[c] && ow.swampAt( c )) swamp = c;
		assertTrue( swamp != -1 );
		ow.wetlands().tick( swamp );
		assertTrue( GuideProgress.pageFound( Wetlands.SWAMP_PAGE ) );

		OverworldLevel back = saveAndLoad( ow );
		assertTrue( back.wetlands().found( hut.key() ) );
		GuideProgress.clear();
		//its name on the HUD, on its grounds
		assertEquals( WetlandSites.mapName( WorldStructures.Site.WITCH_HUT ), back.placeNameAt( back.localCell( hut.cx, hut.cy + 3 ) ) );

		//an older save: nothing found, every beach's pools full on the first visit
		Wetlands old = new Wetlands( back );
		old.restore( new Bundle() );
		assertFalse( old.found( hut.key() ) );
		assertEquals( WetlandSites.POOL_CAP, old.stock( 12345L )[0] );
	}

	// ------------------------------------------------------------------ the witch

	@Test
	public void theWitchsPot(){
		//what goes in: herbs, mushrooms, critter parts
		assertTrue( SwampWitch.herb( new Sungrass.Seed() ) );
		assertTrue( SwampWitch.mushroom( new xyz.gabriwar.warpedpixeldungeon.items.food.PixieParasol() ) );
		assertTrue( SwampWitch.critterPart( new MysteryMeat() ) );
		assertFalse( SwampWitch.ingredient( new Gold() ) );
		assertFalse( SwampWitch.ingredient( new Seashell() ) );
		assertEquals( SwampWitch.BREW_FEE, SwampWitch.brewFee( 0 ) );
		assertEquals( SwampWitch.BREW_FEE - SwampWitch.CRITTER_OFF, SwampWitch.brewFee( 1 ) );
		assertEquals( 0, SwampWitch.brewFee( 3 ) );

		Potion.initColors();
		Random.pushGenerator( 4321 );
		try {
			Class<? extends Potion> fade = Potion.SeedToPotion.types.get( Fadeleaf.Seed.class );
			List<Class<? extends Plant.Seed>> mixed = Arrays.asList( Sungrass.Seed.class, Fadeleaf.Seed.class, Fadeleaf.Seed.class );
			boolean healing = false, faded = false;
			for (int i = 0; i < 60; i++){
				Potion m = SwampWitch.brewKind( mixed, 0 );
				assertTrue( m instanceof PotionOfHealing || m.getClass() == fade );
				healing |= m instanceof PotionOfHealing;
				faded |= m.getClass() == fade;
				assertFalse( "a mixed pot is not named", m.isKnown() );
			}
			assertTrue( healing && faded );
			//three of one kind: that kind's, named
			List<Class<? extends Plant.Seed>> pure = Arrays.asList( Sungrass.Seed.class, Sungrass.Seed.class, Sungrass.Seed.class );
			Potion p = SwampWitch.brewKind( pure, 0 );
			assertTrue( p instanceof PotionOfHealing );
			assertTrue( p.isKnown() );
			for (int i = 0; i < 60; i++) assertFalse( SwampWitch.brewKind( pure, 0 ) instanceof ExoticPotion );
			//mushrooms make it strong now and then
			boolean exotic = false;
			for (int i = 0; i < 60; i++) exotic |= SwampWitch.brewKind( pure, 3 ) instanceof ExoticPotion;
			assertTrue( exotic );
			//nothing of a herb: any potion
			assertNotNull( SwampWitch.brewKind( new ArrayList<>(), 0 ) );
		} finally {
			Random.popGenerator();
		}

		//curses
		RingOfAccuracy ring = new RingOfAccuracy();
		ring.cursed = true;
		assertTrue( SwampWitch.cursed( ring ) );
		ring.cursed = false;
		assertFalse( SwampWitch.cursed( ring ) );

		//her shelf
		SwampWitch w = SwampWitch.of( 5L );
		assertEquals( SwampWitch.MARK_UP, w.priceFactor(), 0.0001f );
		Bundle b = new Bundle();
		w.storeInBundle( b );
		SwampWitch again = new SwampWitch();
		again.restoreFromBundle( b );
		assertEquals( 5L, again.siteKey );
	}

	@Test
	public void theWitchsRemedyDrawsTheBogOut(){
		WetlandSites.Place hut = nearest( WorldStructures.Site.WITCH_HUT );
		windowAt( hut, hut.cx, hut.cy + WetlandSites.WALK + 2 );
		assertFalse( WitchsRemedy.cure( hero ) );
		//the parasite first, then the ooze it leaves as it goes
		Buff.prolong( hero, ParasiticInfection.class, ParasiticInfection.DURATION );
		assertTrue( WitchsRemedy.cure( hero ) );
		assertNull( hero.buff( ParasiticInfection.class ) );
		assertNull( hero.buff( Ooze.class ) );
		Buff.affect( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding.class ).set( 6f );
		assertTrue( WitchsRemedy.cure( hero ) );
		assertNull( hero.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding.class ) );

	}

	// ------------------------------------------------------------------ the rest

	@Test
	public void tilesTellWhatTheyAre(){
		WetlandSites.Place pools = nearest( WorldStructures.Site.TIDE_POOLS );
		OverworldLevel ow = windowAt( pools, pools.cx, pools.cy + WetlandSites.REACH + 2 );
		int[] xy = pools.pools();
		int pool = ow.localCell( xy[0], xy[1] );
		assertEquals( Messages.get( WetlandSites.class, "pool" ), ow.tileNameAt( pool ) );
		Dungeon.cycleTurn = tideFrom( 40 * DayNightCycle.FULL_CYCLE, true );
		assertTrue( ow.tileDescAt( pool ).contains( String.valueOf( Math.max( 1, Math.round(
				24f * WetlandSites.turnsToTurn( Dungeon.cycleTurn ) / DayNightCycle.FULL_CYCLE ) ) ) ) );

		WetlandSites.Place shrine = nearest( WorldStructures.Site.SUNKEN_SHRINE );
		ow = windowAt( shrine, shrine.cx, shrine.cy + 6 );
		assertEquals( Messages.get( WetlandSites.class, "statue" ), ow.tileNameAt( ow.localCell( shrine.cx - 2, shrine.cy - 2 ) ) );
		assertEquals( Messages.get( WetlandSites.class, "statue_desc" ), ow.tileDescAt( ow.localCell( shrine.cx - 2, shrine.cy - 2 ) ) );
		int wall = -1;
		for (int dx = -1; dx <= 1 && wall == -1; dx++){
			int c = ow.localCell( shrine.cx + dx, shrine.cy - 2 );
			if (ow.map[c] == Terrain.WALL) wall = c;
		}
		for (int dy = -1; dy <= 1 && wall == -1; dy++){
			for (int dx : new int[]{ -2, 2 }){
				int c = ow.localCell( shrine.cx + dx, shrine.cy + dy );
				if (wall == -1 && ow.map[c] == Terrain.WALL) wall = c;
			}
		}
		assertTrue( "a wall of the shrine's ring standing", wall != -1 );
		assertEquals( Messages.get( WetlandSites.class, "shrine_wall" ), ow.tileNameAt( wall ) );
		assertTrue( ow.protectedForBuilding( wall ) );

		WetlandSites.Place hut = nearest( WorldStructures.Site.WITCH_HUT );
		ow = windowAt( hut, hut.cx, hut.cy + WetlandSites.WALK + 2 );
		assertEquals( Messages.get( WetlandSites.class, "boardwalk" ), ow.tileNameAt( ow.localCell( hut.cx, hut.cy + 4 ) ) );
		//the rest of the world keeps its own names
		assertFalse( Messages.get( WetlandSites.class, "boardwalk" ).equals( ow.tileNameAt( ow.localCell( hut.cx + 9, hut.cy + 9 ) ) ) );
	}

	@Test
	public void everyStringResolves(){
		for (WorldStructures.Site k : new WorldStructures.Site[]{ WorldStructures.Site.WITCH_HUT,
				WorldStructures.Site.SUNKEN_SHRINE, WorldStructures.Site.TIDE_POOLS }){
			String low = k.name().toLowerCase();
			assertResolves( WetlandSites.class, "found_" + low );
			assertResolves( WetlandSites.class, "map_" + low );
		}
		for (String k : new String[]{ "acolytes_rise", "tide_out", "tide_in", "pool", "pool_desc_out", "pool_desc_in",
				"pool_desc_iced", "statue", "statue_desc", "shrine_wall", "shrine_wall_desc", "boardwalk", "boardwalk_desc" }){
			assertResolves( WetlandSites.class, k );
		}
		for (String k : new String[]{ "name", "desc", "opt_trade", "opt_brew", "opt_curse", "brew_rules", "brew_start",
				"leave", "brew_prompt", "brew_cancel", "poor", "brewed", "brewed_strong", "curse_prompt", "curse_none",
				"curse_lifted", "night", "flee", "talk_0", "talk_1", "talk_2", "talk_3", "talk_4" }){
			assertResolves( SwampWitch.class, k );
		}
		for (String k : new String[]{ "name", "desc", "look" }) assertResolves( WitchCauldron.class, k );
		for (String k : new String[]{ "name", "desc", "prompt", "offer_gold", "offer_item", "leave", "gift_prompt", "poor",
				"still", "still_one", "blessed_gold", "blessed_item", "net_host_only" }){
			assertResolves( SunkenIdol.class, k );
		}
		for (String k : new String[]{ "name", "desc" }) assertResolves( DrownedAcolyte.class, k );
		for (String k : new String[]{ "name", "desc", "snap", "opened" }) assertResolves( PearlOyster.class, k );
		for (Class<?> c : new Class<?>[]{ BogBlessing.class, MarshLight.class, Wading.class }){
			assertResolves( c, "name" );
			assertResolves( c, "desc" );
		}
		for (Item i : new Item[]{ new Seashell(), new Pearl(), new WitchsRemedy(), new BogLantern(), new WadingBalm() }){
			assertFalse( i.getClass().getSimpleName(), i.name().contains( "!!!" ) );
			assertFalse( i.getClass().getSimpleName(), i.desc().contains( "!!!" ) );
		}
		assertResolves( WitchsRemedy.class, "cured" );
		assertResolves( WitchsRemedy.class, "nothing" );
		assertResolves( BogLantern.class, "lit" );
		assertResolves( WadingBalm.class, "rubbed" );
	}

	private static void assertResolves( Class<?> cls, String key ){
		String s = Messages.get( cls, key );
		assertFalse( cls.getSimpleName() + "." + key, s.contains( Messages.NO_TEXT_FOUND ) );
	}

	@Test
	public void spritesAreGuestBuildable() throws Exception {
		for (Class<?> c : new Class<?>[]{ SwampWitchSprite.class, WitchCauldronSprite.class, SunkenIdolSprite.class,
				DrownedAcolyteSprite.class, PearlOysterSprite.class }){
			assertEquals( "xyz.gabriwar.warpedpixeldungeon.sprites", c.getPackage().getName() );
			assertTrue( Modifier.isPublic( c.getConstructor().getModifiers() ) );
		}
		assertEquals( SwampWitchSprite.class, new SwampWitch().spriteClass );
		assertEquals( WitchCauldronSprite.class, new WitchCauldron().spriteClass );
		assertEquals( SunkenIdolSprite.class, new SunkenIdol().spriteClass );
		assertEquals( DrownedAcolyteSprite.class, new DrownedAcolyte().spriteClass );
		assertEquals( PearlOysterSprite.class, new PearlOyster().spriteClass );
	}

	@Test
	public void oldSavesLoad(){
		//every new field falls back when its key is missing
		SunkenIdol idol = SunkenIdol.of( 9L );
		idol.lastOffer = 4;
		Bundle b = new Bundle();
		idol.storeInBundle( b );
		b.remove( "idol_site" );
		b.remove( "last_offer" );
		SunkenIdol oldIdol = new SunkenIdol();
		oldIdol.restoreFromBundle( b );
		assertEquals( 0L, oldIdol.siteKey );
		assertEquals( Integer.MIN_VALUE, oldIdol.lastOffer );

		DrownedAcolyte a = DrownedAcolyte.of( 3L, 10, 20 );
		b = new Bundle();
		a.storeInBundle( b );
		for (String k : new String[]{ "acolyte_shrine", "home_x", "home_y" }) b.remove( k );
		DrownedAcolyte oldA = new DrownedAcolyte();
		oldA.restoreFromBundle( b );
		assertEquals( 0L, oldA.shrineKey );
		assertEquals( Integer.MIN_VALUE, oldA.homeX );
		assertEquals( Integer.MIN_VALUE, oldA.homeY );
		b = new Bundle();
		a.storeInBundle( b );
		DrownedAcolyte backA = new DrownedAcolyte();
		backA.restoreFromBundle( b );
		assertEquals( 3L, backA.shrineKey );
		assertEquals( 10, backA.homeX );
		assertEquals( 20, backA.homeY );

		PearlOyster o = PearlOyster.of( 6L );
		b = new Bundle();
		o.storeInBundle( b );
		PearlOyster backO = new PearlOyster();
		backO.restoreFromBundle( b );
		assertEquals( 6L, backO.poolsKey );
		b.remove( "oyster_pools" );
		PearlOyster oldO = new PearlOyster();
		oldO.restoreFromBundle( b );
		assertEquals( 0L, oldO.poolsKey );

		WitchCauldron pot = WitchCauldron.of( 8L );
		b = new Bundle();
		pot.storeInBundle( b );
		WitchCauldron backPot = new WitchCauldron();
		backPot.restoreFromBundle( b );
		assertEquals( 8L, backPot.siteKey );

		SwampWitch w = SwampWitch.of( 2L );
		b = new Bundle();
		w.storeInBundle( b );
		b.remove( "witch_site" );
		b.remove( "first_stock" );
		SwampWitch oldW = new SwampWitch();
		oldW.restoreFromBundle( b );
		assertEquals( 0L, oldW.siteKey );
	}
}
