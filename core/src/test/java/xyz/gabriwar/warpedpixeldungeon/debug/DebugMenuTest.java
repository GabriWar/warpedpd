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

package xyz.gabriwar.warpedpixeldungeon.debug;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * The debug window's contents. Its remake kept every control the old window had: this lists
 * them all (by tab, kind and label), so a control dropped or folded away by a later change
 * fails here. It also pins the fixed behaviour of the controls that were broken.
 */
public class DebugMenuTest {

	@BeforeClass public static void loadAssets(){
		AllItemsTest.titleScreen();
	}

	private Hero previousHero;
	private Level previousLevel;

	@Before public void setUp(){
		previousHero = Dungeon.hero;
		previousLevel = Dungeon.level;
		Dungeon.hero = new Hero();
	}

	@After public void tearDown(){
		Dungeon.hero = previousHero;
		Dungeon.level = previousLevel;
		Dungeon.debugNoFog = false;
		ClimateManager.debugTempOverride = Float.NaN;
		ClimateManager.debugCloudOverride = Float.NaN;
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugWindDirOverride = Float.NaN;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugPrecipTypeOverride = null;
		DayNightCycle.debugPhaseOverride = null;
		GameCalendar.debugMoonOverride = null;
		setAurora( false );
		Dungeon.debugOneHitKill = false;
	}

	//every control of the old window, where it lives now (tab | kind | label)
	private static final String[] EVERY_CONTROL = {
			"Climate|HEADER|Here and now",
			"Climate|READOUT|Temperature, clouds and wind",
			"Climate|READOUT|Day phase, moon and precipitation",
			"Climate|HEADER|Map overlay",
			"Climate|TOGGLE|Heat overlay",
			"Climate|TEXT|Tile temperatures in C: blue = cold, orange/red = hot. Bars show freezing (blue) or melting (gold).",
			"Climate|HEADER|Weather overrides",
			"Climate|OVERRIDE|Temperature",
			"Climate|OVERRIDE|Clouds",
			"Climate|OVERRIDE|Wind",
			"Climate|OVERRIDE|Wind direction",
			"Climate|OVERRIDE|Precipitation",
			"Climate|OVERRIDE|Precipitation type",
			"Climate|TOGGLE|Force storm",
			"Climate|HEADER|Time overrides",
			"Climate|OVERRIDE|Day phase",
			"Climate|OVERRIDE|Moon",
			"Climate|HEADER|Special events",
			"Climate|TOGGLE|Aurora",
			"Climate|TOGGLE|Rainbow",
			"Climate|TOGGLE|Solar eclipse",
			"Climate|TOGGLE|Lunar eclipse",

			"Items|ACTION|All items",
			"Items|HEADER|By category",
			"Items|ACTION|Weapon", "Items|ACTION|Armor", "Items|ACTION|Wand", "Items|ACTION|Ring",
			"Items|ACTION|Artifact", "Items|ACTION|Potion", "Items|ACTION|Scroll", "Items|ACTION|Seed",
			"Items|ACTION|Stone", "Items|ACTION|Food", "Items|ACTION|Missile", "Items|ACTION|Trinket",
			"Items|HEADER|Exotic and spells",
			"Items|ACTION|Exotic potions",
			"Items|ACTION|Exotic scrolls",
			"Items|ACTION|Spells",
			"Items|HEADER|What you carry",
			"Items|ACTION|Enchant weapon / armor",
			"Items|ACTION|Identify all",

			"Mobs|ACTION|All mobs",
			"Mobs|ACTION|Weather / blobs",
			"Mobs|HEADER|Spawn by bestiary",
			"Mobs|ACTION|Regional", "Mobs|ACTION|Bosses", "Mobs|ACTION|Universal",
			"Mobs|ACTION|Rare", "Mobs|ACTION|Quest", "Mobs|ACTION|Neutral",

			"Travel|READOUT|Where the hero is",
			"Travel|HEADER|Go somewhere",
			"Travel|ACTION|Scenes...",
			"Travel|ACTION|Teleport to a tile",
			"Travel|ACTION|All levels",
			"Travel|HEADER|Debug levels",
			"Travel|TEXT|The rooms showcase holds every room, each with its sign; the overworld is the infinite world (a work in progress).",
			"Travel|ACTION|Rooms showcase",
			"Travel|ACTION|Overworld",
			"Travel|HEADER|World slices",
			"Travel|ACTION|Slice up (+1)",
			"Travel|ACTION|Slice down (-1)",

			"Hero|HEADER|Hero stats",
			"Hero|READOUT|Hero stats",
			"Hero|HEADER|Cheats",
			"Hero|TOGGLE|Infinite health",
			"Hero|TOGGLE|Invisibility (infinite)",
			"Hero|TOGGLE|One hit kill",
			"Hero|TOGGLE|Infinite mana",
			"Hero|HEADER|Map",
			"Hero|TOGGLE|No fog of war",
			"Hero|ACTION|Reveal level",
			"Hero|HEADER|Body",
			"Hero|ACTION|Full heal",
			"Hero|ACTION|+5 strength",
			"Hero|ACTION|+5 levels",
			"Hero|ACTION|Satisfy hunger",
			"Hero|HEADER|Sleep",
			"Hero|ACTION|Max sleepiness",
			"Hero|ACTION|Make drowsy",
			"Hero|ACTION|Clear sleepiness",
			"Hero|HEADER|Resources",
			"Hero|VALUE|Gold",
			"Hero|VALUE|Alchemy energy",
			"Hero|VALUE|Skill points",
			"Hero|HEADER|Skills",
			"Hero|ACTION|Max skill tree",
			"Hero|TOGGLE|All skill paths",
			"Hero|ACTION|Skills of any class...",
			"Hero|TOGGLE|All skills, all classes",

			"Performance|HEADER|Profiler",
			"Performance|TEXT|The profiler samples the render and actor threads every 4 ms while it runs: play normally, then stop it and read where the time went. "
					+ "Frames, GL draw calls, heap, hot paths and slowest actors are in the same report.",
			"Performance|TOGGLE|Profiler",
			"Performance|ACTION|Show last profile",
			"Performance|ACTION|Save last profile",
			"Performance|TOGGLE|Flight recording (JFR)",
			"Performance|HEADER|Lag detector",
			"Performance|TOGGLE|Lag detector",
			"Performance|ACTION|Lag snapshot now",
			"Performance|ACTION|Save last snapshot",
			"Performance|HEADER|Renderer",
			"Performance|TOGGLE|Chunked tilemaps (next level)",

			"Journal and account|HEADER|Journal",
			"Journal and account|ACTION|Fill journal",
			"Journal and account|ACTION|Clear journal",
			"Journal and account|ACTION|Fill mob drops",
			"Journal and account|HEADER|Guide",
			"Journal and account|ACTION|Fill guide",
			"Journal and account|ACTION|Clear guide",
			"Journal and account|ACTION|Reset tutorial",
			"Journal and account|HEADER|Account",
			"Journal and account|ACTION|Relay dev token",
			"Journal and account|HEADER|Supporter thanks",
			"Journal and account|ACTION|Thanks T1",
			"Journal and account|ACTION|Thanks T2",
			"Journal and account|ACTION|Thanks T3",
			"Journal and account|ACTION|T3 + scene reset",
	};

	private static List<String> listed( List<DebugMenu.Tab> tabs ){
		ArrayList<String> out = new ArrayList<>();
		for (DebugMenu.Tab t : tabs){
			for (DebugMenu.Control c : t.controls) out.add( t.title + "|" + c.kind + "|" + c.label );
		}
		return out;
	}

	@Test
	public void everyControlOfTheOldWindowIsStillThere(){
		List<String> now = listed( DebugMenu.tabs() );
		assertEquals( Arrays.asList( EVERY_CONTROL ), now );
	}

	@Test
	public void theTabsKeepTheirOrderSoTheRememberedTabStillPointsAtTheSameOne(){
		List<DebugMenu.Tab> tabs = DebugMenu.tabs();
		String[] titles = new String[tabs.size()];
		for (int i = 0; i < titles.length; i++) titles[i] = tabs.get( i ).title;
		assertArrayEquals( new String[]{ "Climate", "Items", "Mobs", "Travel", "Hero", "Performance", "Journal and account" }, titles );
	}

	@Test
	public void everyControlIsWiredUp(){
		for (DebugMenu.Tab t : DebugMenu.tabs()){
			for (DebugMenu.Control c : t.controls){
				switch (c.kind){
					case ACTION:
						assertNotNull( c.toString(), c.action );
						break;
					case TOGGLE:
						assertNotNull( c.toString(), c.action );
						assertNotNull( c.toString(), c.on );
						break;
					case OVERRIDE:
						assertNotNull( c.toString(), c.on );
						assertNotNull( c.toString(), c.clear );
						//fall through: an override is a value slider too
					case VALUE:
						assertNotNull( c.toString(), c.set );
						assertNotNull( c.toString(), c.value );
						assertNotNull( c.toString(), c.text );
						assertTrue( c.toString(), c.max > c.min );
						break;
					case READOUT:
						assertNotNull( c.toString(), c.text );
						assertNotNull( c.toString(), c.color );
						break;
					default:
						break;
				}
			}
		}
	}

	@Test
	public void theHeroTabSaysSoWhenThereIsNoHero(){
		Dungeon.hero = null;
		for (DebugMenu.Tab t : DebugMenu.tabs()){
			if (!t.title.equals( "Hero" )) continue;
			assertEquals( 1, t.controls.size() );
			assertEquals( DebugMenu.Kind.TEXT, t.controls.get( 0 ).kind );
		}
	}

	private static DebugMenu.Control find( String label ){
		for (DebugMenu.Tab t : DebugMenu.tabs()){
			for (DebugMenu.Control c : t.controls) if (c.label.equals( label )) return c;
		}
		throw new AssertionError( "no control " + label );
	}

	//it used to hold 0 C (and 0% cloud, no wind, north, rain, dawn, new moon) whatever the slider said
	@Test
	public void tickingAnOverrideHoldsTheSlidersValue(){
		DebugMenu.Control temp = find( "Temperature" );
		assertFalse( temp.isOn() );
		temp.tick( 25 );
		assertTrue( temp.isOn() );
		assertEquals( 25f, ClimateManager.debugTempOverride, 0f );
		assertEquals( 25, temp.value.getAsInt() );
		assertEquals( "Temperature: 25C", temp.text.get() );
		temp.tick( 25 );
		assertFalse( temp.isOn() );
		assertTrue( Float.isNaN( ClimateManager.debugTempOverride ) );
	}

	//moving the slider holds the value, and the tick (read from the same state) comes on with it
	@Test
	public void movingAnOverridesSliderTurnsItOn(){
		DebugMenu.Control dir = find( "Wind direction" );
		dir.slide( 3 );
		assertTrue( dir.isOn() );
		assertEquals( 135f, ClimateManager.debugWindDirOverride, 0f );
		assertEquals( "Wind dir: SE", dir.text.get() );

		DebugMenu.Control type = find( "Precipitation type" );
		type.slide( 4 );
		assertEquals( PrecipType.BLIZZARD, ClimateManager.debugPrecipTypeOverride );
		assertEquals( "Precip type: blizzard", type.text.get() );

		DebugMenu.Control moon = find( "Moon" );
		moon.slide( 4 );
		assertEquals( GameCalendar.MoonPhase.values()[4], GameCalendar.debugMoonOverride );
		assertTrue( moon.text.get().startsWith( "Moon: " ) );
		assertFalse( moon.text.get().contains( "_" ) );

		DebugMenu.Control clouds = find( "Clouds" );
		clouds.slide( 99 );
		assertEquals( 1f, ClimateManager.debugCloudOverride, 0f );
	}

	@Test
	public void compassIndexWrapsBothWays(){
		assertEquals( 0, DebugMenu.compassIndex( 0f ) );
		assertEquals( 0, DebugMenu.compassIndex( 359f ) );
		assertEquals( 7, DebugMenu.compassIndex( -45f ) );
		assertEquals( 2, DebugMenu.compassIndex( 90f ) );
	}

	@Test
	public void noPrecipitationStillPutsTheSliderSomewhere(){
		assertEquals( 0, DebugMenu.precipIndex( PrecipType.NONE ) );
		assertEquals( 3, DebugMenu.precipIndex( PrecipType.SLEET ) );
	}

	//the toggle logs (GLog), which a test cannot: the event's own flag is set directly
	private static void setAurora( boolean on ){
		try {
			Field f = ClimateManager.class.getDeclaredField( "auroraActive" );
			f.setAccessible( true );
			f.setBoolean( null, on );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	//it read isAurora(), which the overworld's sky gates: on there, it still said off
	@Test
	public void theAuroraToggleShowsTheEventItself(){
		DebugMenu.Control aurora = find( "Aurora" );
		assertFalse( aurora.isOn() );
		setAurora( true );
		assertTrue( aurora.isOn() );
		assertEquals( "", aurora.text.get() );

		//an overworld sky that cannot show it: still on, and the label says why it is not seen
		Dungeon.level = new OverworldLevel( 0 ){
			@Override public boolean auroraPossible(){ return false; }
		};
		assertFalse( ClimateManager.isAurora() );
		assertTrue( aurora.isOn() );
		assertEquals( " (hidden)", aurora.text.get() );

		setAurora( false );
		assertFalse( aurora.isOn() );
		assertEquals( "", aurora.text.get() );
	}

	//the state lived in a flag that outlived the hero: a new game showed it off while a potion's
	//invisibility could still never be broken
	@Test
	public void invisibilityIsTheBuffItself(){
		Hero h = Dungeon.hero;
		DebugMenu.Control invisible = find( "Invisibility (infinite)" );
		assertFalse( invisible.isOn() );

		invisible.action.run();
		assertTrue( invisible.isOn() );
		Invisibility.dispel( h );
		assertNotNull( "the debug invisibility is never broken", h.buff( Invisibility.class ) );

		invisible.action.run();
		assertFalse( invisible.isOn() );
		assertNull( h.buff( Invisibility.class ) );

		//an ordinary invisibility breaks as usual, and does not read as the debug one
		Buff.affect( h, Invisibility.class, 10f );
		assertFalse( invisible.isOn() );
		Invisibility.dispel( h );
		assertNull( h.buff( Invisibility.class ) );

		//left on, then a new game: the new hero is not invisible, and his potions wear off
		invisible.action.run();
		Hero next = new Hero();
		Dungeon.hero = next;
		assertFalse( invisible.isOn() );
		Buff.affect( next, Invisibility.class, 10f );
		Invisibility.dispel( next );
		assertNull( next.buff( Invisibility.class ) );
	}

	private static Level smallLevel(){
		Level level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( 6, 6 );
		Dungeon.level = level;
		return level;
	}

	//no-fog marks every cell seen (Dungeon.observe); stand-in for that here, the hero is nowhere
	private static void liftFog( Level level ){
		Arrays.fill( level.visited, true );
	}

	@Test
	public void noFogOffBringsBackTheFogOfThatLevel(){
		Level level = smallLevel();
		level.visited[7] = true;
		DebugMenu.Control noFog = find( "No fog of war" );
		noFog.action.run();
		assertTrue( noFog.isOn() );
		liftFog( level );
		noFog.action.run();
		assertFalse( noFog.isOn() );
		assertTrue( level.visited[7] );
		assertFalse( level.visited[8] );
	}

	//the overworld swaps its exploration array as its window moves: the old one is not pasted back
	@Test
	public void noFogOffLeavesAMovedExplorationAlone(){
		Level level = smallLevel();
		DebugMenu.Control noFog = find( "No fog of war" );
		noFog.action.run();
		level.visited = new boolean[level.length()];
		liftFog( level );
		noFog.action.run();
		for (boolean seen : level.visited) assertTrue( seen );
	}

	@Test
	public void aRevealLevelOutlastsNoFog(){
		Level level = smallLevel();
		DebugMenu.Control noFog = find( "No fog of war" );
		noFog.action.run();
		liftFog( level );
		find( "Reveal level" ).action.run();
		noFog.action.run();
		for (boolean seen : level.visited) assertTrue( seen );
		for (boolean seen : level.mapped) assertTrue( seen );
	}

	@Test
	public void togglesFlipTheirStateInPlace(){
		DebugMenu.Control ohk = find( "One hit kill" );
		assertFalse( ohk.isOn() );
		ohk.action.run();
		assertTrue( ohk.isOn() );
		assertTrue( Dungeon.debugOneHitKill );
		ohk.action.run();
		assertFalse( ohk.isOn() );

		Hero h = Dungeon.hero;
		h.HP = 3;
		DebugMenu.Control health = find( "Infinite health" );
		health.action.run();
		assertTrue( health.isOn() );
		assertEquals( h.HT, h.HP );
		health.action.run();
		assertFalse( h.debugInfiniteHealth );
	}

	@Test
	public void allSkillsAllClassesTurnsOnAndBackOff(){
		Hero h = Dungeon.hero;
		h.heroSkills.init( h );
		try {
			DebugMenu.Control all = find( "All skills, all classes" );
			assertFalse( all.isOn() );
			all.action.run();
			assertTrue( all.isOn() );
			assertFalse( h.heroSkills.foreignSkills().isEmpty() );
			all.action.run();
			assertFalse( all.isOn() );
			assertTrue( h.heroSkills.foreignSkills().isEmpty() );
		} finally {
			h.heroSkills.init( h );
		}
	}

	@Test
	public void valueSlidersStepThroughTheirAmounts(){
		assertEquals( 0, DebugMenu.stepOf( 0 ) );
		assertEquals( 5, DebugMenu.stepOf( 1234 ) );
		assertEquals( DebugMenu.VALUE_STEPS.length - 1, DebugMenu.stepOf( Integer.MAX_VALUE ) );
		int before = Dungeon.gold;
		try {
			DebugMenu.Control gold = find( "Gold" );
			gold.set.accept( 3 );
			assertEquals( 100, Dungeon.gold );
			assertEquals( "Gold: 100", gold.text.get() );
			assertEquals( 3, gold.value.getAsInt() );
		} finally {
			Dungeon.gold = before;
		}
	}

	@Test
	public void readoutsPrintTheSameNumbersOnEveryDevice(){
		Locale before = Locale.getDefault();
		try {
			Locale.setDefault( new Locale( "pt", "BR" ) );
			assertEquals( "12.3", DebugMenu.fmt( 12.34f ) );
		} finally {
			Locale.setDefault( before );
		}
	}

	@Test
	public void labelsAreUniqueWithinATab(){
		for (DebugMenu.Tab t : DebugMenu.tabs()){
			Set<String> seen = new LinkedHashSet<>();
			for (DebugMenu.Control c : t.controls){
				assertTrue( t.title + ": " + c, seen.add( c.kind + "|" + c.label ) );
			}
		}
	}
}
