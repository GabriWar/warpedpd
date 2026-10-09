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

package xyz.gabriwar.warpedpixeldungeon.journal;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.journal.DescentPage;
import xyz.gabriwar.warpedpixeldungeon.levels.DeadEndLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The Descent Guide's tree against its words and its world: every node has a title and every
 * page its text, no string is left over, every page can actually be earned (a floor page lies on
 * a floor the game builds, a discovery is one something in the game makes, a boss is a boss), the
 * record of found and read pages survives a save, and nothing the player reads talks about
 * gambling.
 */
public class GuideGraphTest {

	private static final String GUIDE = "src/main/assets/messages/guide/guide.properties";
	private static final String SCENES = "src/main/assets/messages/scenes/scenes.properties";

	private static Properties guide;

	//badges this test marked as held, given back afterwards
	private static final HashSet<Badges.Badge> lent = new HashSet<>();

	@BeforeClass
	public static void boot() throws IOException, ReflectiveOperationException {
		WarpedRoomsTest.boot();
		com.watabou.utils.FileUtils.setDefaultFileProperties( com.badlogic.gdx.Files.FileType.Absolute,
				new java.io.File( "build/test-saves" ).getAbsolutePath() + "/" );
		guide = load( GUIDE );
		//building a floor can earn a badge, which goes on to tell a platform there is none of
		//here: every badge counts as held already while this test builds floors
		Badges.loadGlobal();
		java.lang.reflect.Field f = Badges.class.getDeclaredField( "global" );
		f.setAccessible( true );
		@SuppressWarnings("unchecked")
		HashSet<Badges.Badge> global = (HashSet<Badges.Badge>) f.get( null );
		for (Badges.Badge b : Badges.Badge.values()){
			if (global.add( b )) lent.add( b );
		}
	}

	@AfterClass
	public static void giveBack() throws ReflectiveOperationException {
		java.lang.reflect.Field f = Badges.class.getDeclaredField( "global" );
		f.setAccessible( true );
		@SuppressWarnings("unchecked")
		HashSet<Badges.Badge> global = (HashSet<Badges.Badge>) f.get( null );
		global.removeAll( lent );
		lent.clear();
	}

	private static Properties load( String path ) throws IOException {
		Properties p = new Properties();
		try (InputStreamReader r = new InputStreamReader( new FileInputStream( path ), StandardCharsets.UTF_8 )){
			p.load( r );
		}
		return p;
	}

	@Test
	public void everyNodeHasItsWords(){
		GuideGraph.Node root = GuideGraph.build();
		HashSet<String> ids = new HashSet<>(), keys = new HashSet<>();
		for (GuideGraph.Node n : GuideGraph.all( root )){
			assertTrue( "two nodes are " + n.id, ids.add( n.id ) );
			assertNotNull( n.id + " has no title", guide.getProperty( "guide.title." + n.id ) );
			for (GuideGraph.Node c : n.children) assertTrue( c.parent == n );
			if (n.key != null){
				assertTrue( "two pages are " + n.key, keys.add( n.key ) );
				assertTrue( n.id + ": a page's id is its key", n.id.equals( n.key ) );
				assertTrue( n.key + " is a page with children", n.children.isEmpty() );
				String text = guide.getProperty( "guide." + n.key );
				assertNotNull( n.key + " has no text", text );
				assertTrue( n.key + " is nearly empty", text.length() > 200 );
			} else {
				assertFalse( n.id + " is an empty chapter", n.children.isEmpty() );
			}
			if (n.mob != null){
				assertNotNull( n.id + ": a boss without a chapter", n.key );
				assertEquals( n.key, GuideGraph.keyForMob( n.mob ) );
			}
		}
		assertEquals( GuideGraph.allKeys().size(), keys.size() );
	}

	@Test
	public void noStringIsLeftOver(){
		GuideGraph.Node root = GuideGraph.build();
		HashSet<String> ids = new HashSet<>(), keys = new HashSet<>(), places = new HashSet<>(), tags = new HashSet<>();
		for (GuideGraph.Node n : GuideGraph.all( root )){
			ids.add( n.id );
			if (n.key != null) keys.add( n.key );
			if (n.place != null) places.add( n.place );
			if (n.tag != null) tags.add( n.tag );
		}
		for (String k : guide.stringPropertyNames()){
			assertTrue( k, k.startsWith( "guide." ) );
			String rest = k.substring( "guide.".length() );
			if (rest.startsWith( "title." ) || rest.startsWith( "sub." )){
				String id = rest.substring( rest.indexOf( '.' ) + 1 );
				assertTrue( k + " names no node", ids.contains( id ) );
			} else if (rest.startsWith( "hint." )){
				String h = rest.substring( "hint.".length() );
				boolean used = h.equals( "boss" ) || h.equals( "chapter" ) || h.equals( "floor" )
						|| (h.startsWith( "place_" ) && places.contains( h.substring( 6 ) ))
						|| (h.startsWith( "tag_" ) && tags.contains( h.substring( 4 ) ));
				assertTrue( k + " is never shown", used );
			} else {
				assertTrue( k + " is a page no node holds", keys.contains( rest ) );
			}
		}
		for (String p : places) assertNotNull( "no hint for " + p, guide.getProperty( "guide.hint.place_" + p ) );
		for (String t : tags) assertNotNull( "no hint for " + t, guide.getProperty( "guide.hint.tag_" + t ) );
	}

	@Test
	public void discoveriesAreOnesTheGameMakes() throws IOException {
		HashSet<String> made = new HashSet<>();
		int[][] places = { {97, 0}, {96, 0}, {90, 0}, {87, 0}, {101, 0}, {104, 0}, {112, 0}, {1, 6}, {1, 7}, {3, 100}, {5, 117} };
		for (int[] p : places) made.addAll( GuideGraph.arrivalTags( p[0], p[1] ) );
		//the rest are revealed by the game's code where the thing happens
		String code = sources();
		for (String tag : GuideGraph.ALL_TAGS){
			if (made.contains( tag )) continue;
			String constant = constantFor( tag );
			assertTrue( tag + " is never revealed", code.contains( "GuideGraph." + constant ) );
			made.add( tag );
		}
		for (GuideGraph.Node n : GuideGraph.all( GuideGraph.build() )){
			if (n.tag != null) assertTrue( n.id + ": " + n.tag, made.contains( n.tag ) );
		}
	}

	private static String constantFor( String tag ) {
		for (java.lang.reflect.Field f : GuideGraph.class.getFields()){
			try {
				if (f.getName().startsWith( "TAG_" ) && tag.equals( f.get( null ) )) return f.getName();
			} catch (IllegalAccessException e){
				throw new RuntimeException( e );
			}
		}
		fail( "no constant for " + tag );
		return null;
	}

	private static String sources() throws IOException {
		StringBuilder sb = new StringBuilder();
		Path root = Paths.get( "src/main/java" );
		try (Stream<Path> paths = Files.walk( root )){
			for (Path p : (Iterable<Path>) paths.filter( f -> f.toString().endsWith( ".java" ) )::iterator){
				if (p.toString().endsWith( "GuideGraph.java" )) continue;
				sb.append( new String( Files.readAllBytes( p ), StandardCharsets.UTF_8 ) );
			}
		}
		return sb.toString();
	}

	@Test
	public void arrivingSomewhereWritesItsPages(){
		assertTrue( GuideGraph.arrivalTags( 97, 0 ).contains( GuideGraph.TAG_SURFACE ) );
		assertTrue( GuideGraph.arrivalTags( 96, 0 ).contains( GuideGraph.TAG_MOUNTAINS ) );
		assertFalse( GuideGraph.arrivalTags( 91, 0 ).contains( GuideGraph.TAG_HEIGHTS ) );
		assertTrue( GuideGraph.arrivalTags( 90, 0 ).contains( GuideGraph.TAG_HEIGHTS ) );
		assertTrue( GuideGraph.arrivalTags( 101, 0 ).contains( GuideGraph.TAG_CAVES ) );
		assertFalse( GuideGraph.arrivalTags( 103, 0 ).contains( GuideGraph.TAG_CAVES_DEEP ) );
		assertTrue( GuideGraph.arrivalTags( 104, 0 ).contains( GuideGraph.TAG_CAVES_DEEP ) );
		assertTrue( GuideGraph.arrivalTags( 3, 6 ).contains( GuideGraph.TAG_TOWN_INSIDE ) );
		assertTrue( GuideGraph.arrivalTags( 1, 7 ).contains( GuideGraph.TAG_VILLAGE ) );
		assertTrue( GuideGraph.arrivalTags( 9, 104 ).contains( GuideGraph.TAG_BARROW ) );
		//the dungeon's own floors are earned page by page
		for (int d = 1; d <= 26; d++) assertTrue( GuideGraph.arrivalTags( d, 0 ).isEmpty() );
		//the save numbering is the game's
		assertEquals( xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers.SURFACE_DEPTH, GuideGraph.SURFACE_DEPTH );
		assertEquals( xyz.gabriwar.warpedpixeldungeon.levels.Delves.BRANCH_BASE, GuideGraph.BARROWS_FROM );
		assertEquals( xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel.BRANCH, GuideGraph.VILLAGE_HOUSE );

		GuideProgress.clear();
		ArrayList<String> first = GuideGraph.discover( GuideGraph.TAG_SURFACE );
		assertTrue( first.contains( "surface_overview" ) );
		assertTrue( first.contains( "town_overview" ) );
		assertTrue( GuideGraph.discover( GuideGraph.TAG_SURFACE ).isEmpty() );
		GuideProgress.clear();
	}

	//every page that lies on a floor is on that floor when the game builds it
	@Test
	public void everyFloorPageLiesOnItsFloor(){
		GuideProgress.clear();
		Dungeon.seed = 0x6E1DEL;
		xyz.gabriwar.warpedpixeldungeon.GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();

		java.util.HashMap<String, HashSet<String>> lying = new java.util.HashMap<>();
		for (GuideGraph.Node n : GuideGraph.all( GuideGraph.build() )){
			if (n.key == null || n.mob != null || n.tag != null) continue;
			if (n.depths == null) continue; //common knowledge
			int depth = GuideGraph.spawnDepth( n );
			assertTrue( n.key, depth > 0 );
			String where = n.branch + "/" + depth;
			if (!lying.containsKey( where )){
				Dungeon.depth = depth;
				Dungeon.branch = n.branch;
				Level level = Dungeon.newLevel();
				assertFalse( n.key + ": depth " + where + " is a dead end", level instanceof DeadEndLevel );
				HashSet<String> pages = new HashSet<>();
				for (Heap heap : level.heaps.valueList()){
					for (Item item : heap.items){
						if (item instanceof DescentPage) pages.add( ((DescentPage) item).page() );
					}
				}
				lying.put( where, pages );
			}
			assertTrue( n.key + ": no page on " + where, lying.get( where ).contains( n.key ) );
		}
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.level = null;
	}

	//a boss's chapter writes itself when its health bar comes up, the moment its fight begins:
	//fight advice read only after the win is no use, and the king's tomb, never an enemy, was
	//never counted as a kill at all
	@Test
	public void everyBossChapterWritesItselfWhenItsFightBegins() throws IOException {
		GuideProgress.clear();
		Dungeon.seed = 0xB055L;
		xyz.gabriwar.warpedpixeldungeon.GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();
		for (GuideGraph.Node n : GuideGraph.all( GuideGraph.build() )){
			if (n.mob == null) continue;
			Path src = Paths.get( "src/main/java/" + n.mob.getName().replace( '.', '/' ) + ".java" );
			String code = new String( Files.readAllBytes( src ), StandardCharsets.UTF_8 );
			assertTrue( n.id + ": " + n.mob.getSimpleName() + " never raises a health bar",
					code.contains( "BossHealthBar.assignBoss" ) );

			Mob boss = Reflection.newInstance( n.mob );
			assertNotNull( n.mob.getSimpleName(), boss );
			BossHealthBar.assignBoss( boss );
			assertTrue( n.id + " stays locked when its fight begins", GuideProgress.pageFound( n.key ) );
		}
		BossHealthBar.assignBoss( null );
		GuideProgress.clear();
	}

	//the lair dragon has a chapter of its own: the bestiary files it under the purple dragon
	//it is ported from, but its kill still writes the lair dragon's page
	@Test
	public void theLairDragonsKillWritesItsChapter(){
		GuideProgress.clear();
		Bestiary.setSeen( OverworldDragon.class );
		assertTrue( GuideProgress.pageFound( "boss_dragon" ) );
		GuideProgress.clear();
	}

	//a page found before anything loaded the journal from disk (a spectator goes from the lobby
	//straight into the game) must not leave the journal to be saved over with that page alone
	@Test
	public void findingAPageLoadsTheJournalFirst() throws ReflectiveOperationException {
		java.lang.reflect.Field loaded = Journal.class.getDeclaredField( "loaded" );
		loaded.setAccessible( true );
		boolean wasLoaded = loaded.getBoolean( null );
		try {
			GuideProgress.clear();
			GuideProgress.findPage( "on_disk" );
			Journal.saveGlobal( true );
			GuideProgress.clear();
			loaded.setBoolean( null, false );

			GuideProgress.findPage( "fresh" );
			assertTrue( GuideProgress.pageFound( "fresh" ) );
			assertTrue( "the journal on disk was never loaded", GuideProgress.pageFound( "on_disk" ) );
		} finally {
			loaded.setBoolean( null, wasLoaded );
			com.watabou.utils.FileUtils.deleteFile( Journal.JOURNAL_FILE );
			GuideProgress.clear();
		}
	}

	@Test
	public void foundAndReadPagesSurviveASave(){
		GuideProgress.clear();
		GuideProgress.findPage( "a" );
		GuideProgress.findPage( "b" );
		GuideProgress.findPage( "c" );
		assertFalse( GuideProgress.findPage( "b" ) );
		GuideProgress.markRead( "c" );
		assertEquals( "b", GuideProgress.newestUnread() );

		Bundle bundle = new Bundle();
		GuideProgress.store( bundle );
		GuideProgress.clear();
		assertNull( GuideProgress.newestUnread() );
		GuideProgress.restore( bundle );
		assertTrue( GuideProgress.pageFound( "a" ) );
		assertTrue( GuideProgress.isRead( "c" ) );
		assertFalse( GuideProgress.isRead( "a" ) );
		assertEquals( "b", GuideProgress.newestUnread() );

		//a journal from before pages were read: what was found counts as read
		Bundle old = new Bundle();
		old.put( "guide_pages", new String[]{ "x", "y" } );
		GuideProgress.restore( old );
		assertTrue( GuideProgress.isRead( "x" ) && GuideProgress.isRead( "y" ) );
		assertNull( GuideProgress.newestUnread() );
		GuideProgress.clear();
	}

	@Test
	public void lockingFollowsWhatWasFound(){
		GuideProgress.clear();
		GuideGraph.Node root = GuideGraph.build();
		GuideGraph.Node sewers = GuideGraph.find( root, "ch_sewers" );
		GuideGraph.Node f2 = GuideGraph.find( root, "sewers_f2" );
		assertFalse( GuideGraph.unlocked( sewers ) );
		assertFalse( GuideGraph.unlocked( f2 ) );
		assertTrue( GuideGraph.unlocked( root ) );
		assertTrue( GuideGraph.unlocked( GuideGraph.find( root, "topic_meta" ) ) );
		//the floor page of depth 2 lies on depth 3
		assertTrue( GuideGraph.pagesToSpawn( 3, 0 ).contains( "sewers_f2" ) );
		assertFalse( GuideGraph.pagesToSpawn( 2, 0 ).contains( "sewers_f2" ) );

		GuideProgress.findPage( "sewers_f2" );
		assertTrue( GuideGraph.unlocked( f2 ) );
		assertTrue( GuideGraph.unlocked( sewers ) );
		assertTrue( GuideGraph.isNew( f2 ) );
		int[] p = GuideGraph.progress( sewers );
		assertEquals( 1, p[0] );
		assertEquals( 7, p[1] );
		assertEquals( 1, p[2] );
		GuideProgress.markRead( "sewers_f2" );
		assertFalse( GuideGraph.isNew( f2 ) );
		assertFalse( GuideGraph.pagesToSpawn( 3, 0 ).contains( "sewers_f2" ) );
		GuideProgress.clear();
	}

	private static final Pattern GAMBLING = Pattern.compile(
			"(?i)\\b(gambl\\w*|bets?|betting|wager\\w*|roulette|casino\\w*|jackpot\\w*|lotter\\w*|stakes?|staking|odds)\\b" );

	@Test
	public void noPageTalksAboutGambling() throws IOException {
		for (String k : guide.stringPropertyNames()){
			assertFalse( k, GAMBLING.matcher( guide.getProperty( k ) ).find() );
		}
		Properties scenes = load( SCENES );
		for (String k : scenes.stringPropertyNames()){
			if (!k.startsWith( "scenes.guidescene." )) continue;
			assertFalse( k, GAMBLING.matcher( scenes.getProperty( k ) ).find() );
		}
	}
}
