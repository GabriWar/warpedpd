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

package xyz.gabriwar.warpedpixeldungeon.journal.guidecites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownBat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RatBoss;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenKey;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.CiteArt;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.CiteChip;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.LiveCrop;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;
import com.watabou.utils.Reflection;
import com.watabou.utils.SparseArray;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The Descent Guide's cites against its words and the game: every highlighted word on a page names
 * a cite (or is plainly no thing), every cite names something the page says and a thing the game
 * has, every page is cited by the one group that owns its chapter, and the matcher reduces words
 * as its rules say. The group methods (surfacePagesAreCited...) each check one group's chapters:
 * a content branch makes its own pass.
 *
 * The pictures are built here too, headlessly (textures load lazily, nothing reaches the GPU): every
 * kind's art (CiteArt.icon) and plate (CiteChip.Plate) is made and run for a while, every effect
 * pours or bursts, and a plate takes its particles with it. A whole CiteChip is not built here: its
 * caption needs the platform's fonts, which a test has none of.
 */
public class GuideCitesTest {

	private static final String GUIDE = "src/main/assets/messages/guide/guide.properties";
	private static final String ASSETS = "src/main/assets/";
	private static final int MAX_CITES = 24, MAX_PLAIN = 4, MAX_CAPTION = 28;

	private static Properties guide;
	//every thing's name in the English bundles, reduced: a plain word must be none of them
	private static HashSet<String> thingNames;

	@BeforeClass
	public static void boot() throws IOException {
		WarpedRoomsTest.boot();
		guide = new Properties();
		try (InputStreamReader r = new InputStreamReader( new FileInputStream( GUIDE ), StandardCharsets.UTF_8 )){
			guide.load( r );
		}
		thingNames = new HashSet<>();
		for (String dir : new String[]{ "actors", "items", "plants", "levels", "homestead" }){
			File[] files = new File( ASSETS + "messages/" + dir ).listFiles();
			if (files == null) continue;
			for (File f : files){
				if (!f.getName().matches( "^[a-z]+\\.properties$" )) continue;
				Properties p = new Properties();
				try (InputStreamReader r = new InputStreamReader( new FileInputStream( f ), StandardCharsets.UTF_8 )){
					p.load( r );
				}
				for (String k : p.stringPropertyNames()){
					if (k.endsWith( ".name" )) thingNames.add( reduced( p.getProperty( k ) ) );
				}
			}
		}
	}

	private static String reduced( String phrase ){
		return String.join( " ", CiteMatch.tokens( phrase ) );
	}

	// ------------------------------------------------------------------ the matcher

	@Test
	public void citeMatchFollowsItsRules(){
		//the spec's examples
		assertTrue( CiteMatch.same( "Potions of Strength", "Potion of Strength" ) );
		assertTrue( CiteMatch.same( "Dew Condenser:", "dew condenser" ) );
		assertTrue( CiteMatch.same( "Miners' camp", "miner camp" ) );
		assertTrue( CiteMatch.same( "The build bar", "build bar" ) );
		assertFalse( CiteMatch.same( "Tengu Ancient Key", "Tengu" ) );
		assertEquals( 0, CiteMatch.find( CiteMatch.tokens( "Tengu Ancient Key" ), CiteMatch.tokens( "Tengu" ) ) );

		//1: lower case, curly apostrophes made straight
		assertArrayEquals( new String[]{ "goo" }, CiteMatch.tokens( "GOO’s" ) );
		//2: runs of letters, digits, ' and -; everything else parts them and goes
		assertArrayEquals( new String[]{ "grey", "rat", "tier", "5", "half-health" },
				CiteMatch.tokens( "_grey rat_ (tier 5, half-health):" ) );
		assertArrayEquals( new String[0], CiteMatch.tokens( " - _ ' " ) );
		//3: a trailing 's, then a trailing ', and an opening quote
		assertArrayEquals( new String[]{ "otiluke", "journal" }, CiteMatch.tokens( "Otiluke's Journal" ) );
		assertArrayEquals( new String[]{ "miner" }, CiteMatch.tokens( "miners'" ) );
		assertArrayEquals( new String[]{ "word" }, CiteMatch.tokens( "'word'" ) );
		//4: plurals
		assertArrayEquals( new String[]{ "berry" }, CiteMatch.tokens( "berries" ) );
		assertArrayEquals( new String[]{ "tie" }, CiteMatch.tokens( "ties" ) );
		assertArrayEquals( new String[]{ "torch", "bush", "box", "glass", "buzz" },
				CiteMatch.tokens( "torches bushes boxes glasses buzzes" ) );
		assertArrayEquals( new String[]{ "rat", "gas", "moss", "fungus", "iris" },
				CiteMatch.tokens( "rats gas moss fungus iris" ) );
		//5: one leading article goes, never a lone word
		assertArrayEquals( new String[]{ "rat" }, CiteMatch.tokens( "a rat" ) );
		assertArrayEquals( new String[]{ "ankh" }, CiteMatch.tokens( "an Ankh" ) );
		assertArrayEquals( new String[]{ "the", "rat" }, CiteMatch.tokens( "the the rat" ) );
		assertArrayEquals( new String[]{ "a" }, CiteMatch.tokens( "A" ) );
		//irregular plurals stay apart: they need a word of their own
		assertFalse( CiteMatch.same( "succubi", "succubus" ) );
		assertFalse( CiteMatch.same( "", "" ) );

		assertEquals( Arrays.asList( "b c", "e" ), CiteMatch.highlights( "a _b c_ d _e_ f _g" ) );
		assertEquals( 3, CiteMatch.find( CiteMatch.tokens( "it calls the rat boss" ), CiteMatch.tokens( "the rat bosses" ) ) );
		assertEquals( -1, CiteMatch.find( CiteMatch.tokens( "a rat" ), CiteMatch.tokens( "bat" ) ) );
		assertEquals( -1, CiteMatch.find( CiteMatch.tokens( "a rat" ), new String[0] ) );
	}

	// ------------------------------------------------------------------ the groups

	//the chapters a group owns that exist, and every page under them
	private static ArrayList<GuideGraph.Node> pagesOf( GuideGraph.Node root, String[] group, ArrayList<String> problems ){
		ArrayList<GuideGraph.Node> out = new ArrayList<>();
		for (int i = 1; i < group.length; i++){
			GuideGraph.Node ch = GuideGraph.find( root, group[i] );
			if (ch == null){
				if (problems != null) problems.add( group[0] + ": chapter " + group[i] + " does not exist" );
				continue;
			}
			for (GuideGraph.Node n : GuideGraph.all( ch )){
				if (n.key != null) out.add( n );
			}
		}
		return out;
	}

	@Test
	public void everyPageBelongsToOneGroup(){
		GuideGraph.Node root = GuideGraph.build();
		HashMap<String, String> owner = new HashMap<>();
		for (String[] g : GuideCites.GROUPS){
			for (GuideGraph.Node n : pagesOf( root, g, null )){
				String before = owner.put( n.key, g[0] );
				assertNull( n.key + " lies under chapters of both " + before + " and " + g[0], before );
			}
		}
		for (GuideGraph.Node n : GuideGraph.all( root )){
			if (n.key != null) assertNotNull( n.key + " lies under no group's chapters (add its chapter to GuideCites.GROUPS)",
					owner.get( n.key ) );
		}
		CiteBook book = GuideCites.book();
		for (CiteBook.Page p : book.pages.values()){
			assertTrue( p.group + " cites " + p.key + ", which is no page of the guide (check the key)",
					owner.containsKey( p.key ) );
			assertEquals( p.key + " was cited by " + p.group + ", but its chapter is " + owner.get( p.key ) + "'s",
					owner.get( p.key ), p.group );
		}
		for (String k : GuideCites.failures.keySet()){
			fail( "registering " + k + " failed: " + GuideCites.failures.get( k ) );
		}
	}

	@Test
	public void citesNameRealThings(){
		assertProblems( "", realThingProblems( GuideCites.book() ) );
	}

	@Test
	public void surfacePagesAreCited(){
		groupIsCited( 0 );
	}

	@Test
	public void earlyPagesAreCited(){
		groupIsCited( 1 );
	}

	@Test
	public void latePagesAreCited(){
		groupIsCited( 2 );
	}

	@Test
	public void extraPagesAreCited(){
		groupIsCited( 3 );
	}

	@Test
	public void basicsPagesAreCited(){
		groupIsCited( 4 );
	}

	private void groupIsCited( int i ){
		String[] group = GuideCites.GROUPS[i];
		ArrayList<String> problems = new ArrayList<>();
		for (String k : GuideCites.failures.keySet()){
			if (k.startsWith( group[0] )) problems.add( "registering " + k + " failed: " + GuideCites.failures.get( k ) );
		}
		GuideGraph.Node root = GuideGraph.build();
		CiteBook book = GuideCites.book();
		for (GuideGraph.Node n : pagesOf( root, group, problems )){
			problems.addAll( pageProblems( n, book.pages.get( n.key ) ) );
		}
		assertProblems( group[0], problems );
	}

	private static void assertProblems( String what, List<String> problems ){
		if (problems.isEmpty()) return;
		StringBuilder sb = new StringBuilder( what + ": " + problems.size() + " problem(s)\n" );
		for (int i = 0; i < problems.size() && i < 60; i++) sb.append( problems.get( i ) ).append( '\n' );
		if (problems.size() > 60) sb.append( "...and " ).append( problems.size() - 60 ).append( " more\n" );
		fail( sb.toString() );
	}

	//what is wrong with one page's cites against its text
	private static ArrayList<String> pageProblems( GuideGraph.Node node, CiteBook.Page page ){
		ArrayList<String> out = new ArrayList<>();
		String key = node.key;
		String text = guide.getProperty( "guide." + key );
		if (text == null){
			out.add( key + ": no text in guide.properties" );
			return out;
		}
		int n = page == null ? 0 : page.cites.size();
		if (n < 1 || n > MAX_CITES){
			out.add( key + ": " + n + " cites (a page has 1 to " + MAX_CITES + "; book.page( \"" + key + "\" ) in your group's file)" );
		}
		if (page == null) return out;

		ArrayList<String> words = new ArrayList<>( page.plain );
		for (Cite c : page.cites) words.addAll( Arrays.asList( c.words ) );
		ArrayList<String> highlights = CiteMatch.highlights( text );
		for (String h : highlights){
			boolean named = false;
			for (String w : words) named |= CiteMatch.same( h, w );
			if (!named) out.add( key + ": highlight \"" + h + "\" has no cite (add the word to a cite, or plain() if it names nothing)" );
		}

		String[] all = CiteMatch.tokens( text );
		for (Cite c : page.cites){
			boolean said = false;
			for (String w : c.words) said |= CiteMatch.find( all, CiteMatch.tokens( w ) ) >= 0;
			if (!said) out.add( key + ": cite \"" + c.caption() + "\" names nothing the page says (one of its words must appear in the text, as written)" );
		}

		if (page.plain.size() > MAX_PLAIN){
			out.add( key + ": " + page.plain.size() + " plain words (at most " + MAX_PLAIN + ": cite the rest)" );
		}
		for (String p : page.plain){
			boolean highlighted = false;
			for (String h : highlights) highlighted |= CiteMatch.same( h, p );
			if (!highlighted) out.add( key + ": plain word \"" + p + "\" is no highlight of the page (drop it)" );
			if (thingNames.contains( reduced( p ) )){
				out.add( key + ": plain word \"" + p + "\" is the name of a game thing (cite it instead)" );
			}
		}

		if (node.mob != null){
			boolean own = false;
			for (Cite c : page.cites) own |= c.kind == Cite.Kind.MOB && c.type == node.mob;
			if (!own) out.add( key + ": a boss page cites its own boss (.mob( " + node.mob.getSimpleName() + ".class, ... ))" );
		}
		return out;
	}

	//what is wrong with the cites themselves: things the game has, short captions, one word per thing
	private static ArrayList<String> realThingProblems( CiteBook book ){
		ArrayList<String> out = new ArrayList<>();
		GuideGraph.Node root = GuideGraph.build();
		for (CiteBook.Page p : book.pages.values()){
			GuideGraph.Node node = GuideGraph.find( root, p.key );
			HashMap<String, String> seen = new HashMap<>();
			int primaries = 0;
			for (Cite c : p.cites){
				String where = p.key + ": " + c.kind + " \"" + c.caption() + "\"";
				String wrong = thingProblem( c );
				if (wrong != null) out.add( where + " " + wrong );
				if (c.caption().length() > MAX_CAPTION){
					out.add( where + ": its caption is longer than " + MAX_CAPTION + " characters (shorten its first word)" );
				}
				//the same word twice on one cite is harmless; two cites sharing one is not
				HashSet<String> own = new HashSet<>();
				for (String w : c.words){
					String r = reduced( w );
					if (!own.add( r )) continue;
					String other = seen.put( r, c.caption() );
					if (other != null){
						out.add( where + ": the word \"" + w + "\" is also on the cite \"" + other + "\" (a word names one thing)" );
					}
				}
				if (c.primary){
					primaries++;
					if (c.kind == Cite.Kind.EFFECT) out.add( where + ": an effect cannot be the page's icon (drop primary())" );
					if (node != null && node.mob != null){
						out.add( where + ": a boss page's card already shows its boss (drop primary())" );
					}
				}
			}
			if (primaries > 1) out.add( p.key + ": " + primaries + " primaries (one at most)" );
		}
		return out;
	}

	//why a cite names nothing the game has, or null
	private static String thingProblem( Cite c ){
		try {
			switch (c.kind){
				case MOB: {
					if (!Mob.class.isAssignableFrom( c.type ) || Modifier.isAbstract( c.type.getModifiers() )){
						return "is no concrete Mob";
					}
					if (GuidePuppet.unmade( c.type ) != null){
						//never made (it would change the run): its sprite class must be what the game draws
						if (c.type.getMethod( "sprite" ).getDeclaringClass() != Mob.class) return "draws its own sprite()";
						Reflection.newInstanceUnhandled( GuidePuppet.unmade( c.type ) );
						return null;
					}
					Mob m = (Mob) Reflection.newInstanceUnhandled( c.type );
					boolean drawn = m.spriteClass != null || c.type.getMethod( "sprite" ).getDeclaringClass() != Mob.class;
					return drawn ? null : "has no sprite";
				}
				case ITEM: {
					if (!Item.class.isAssignableFrom( c.type ) || Modifier.isAbstract( c.type.getModifiers() )){
						return "is no concrete Item";
					}
					Item item = (Item) Reflection.newInstanceUnhandled( c.type );
					if (item.icon != -1){
						return ItemSpriteSheet.Icons.film.get( item.icon ) != null ? null : "has no icon";
					}
					return ItemSpriteSheet.film.get( item.image() ) != null ? null : "has no sprite";
				}
				case SPRITE:
					return ItemSpriteSheet.film.get( c.image ) != null ? null : "is no frame of the item sheet";
				case BUFF: {
					Buff b = (Buff) Reflection.newInstanceUnhandled( c.type );
					int icon = c.image >= 0 ? c.image : b.icon();
					return icon != BuffIndicator.NONE ? null
							: "shows no icon (pass a BuffIndicator icon: buff( " + c.type.getSimpleName() + ".class, BuffIndicator.X, ... ))";
				}
				case TRAP: case PLANT:
					Reflection.newInstanceUnhandled( c.type );
					return null;
				case TERRAIN: {
					File f = new File( ASSETS + c.asset );
					if (!f.exists()) return "names a tileset that does not exist (" + c.asset + ")";
					BufferedImage img = ImageIO.read( f );
					int tiles = (img.getWidth() / 16) * (img.getHeight() / 16);
					return c.image >= 0 && c.image < tiles ? null : "names tile " + c.image + ", outside its tileset";
				}
				case WATER:
					return new File( ASSETS + c.asset ).exists() ? null : "names a water texture that does not exist (" + c.asset + ")";
				default:
					return null;
			}
		} catch (Exception e){
			return "cannot be made: " + e;
		}
	}

	// ------------------------------------------------------------------ registering

	//the guide's own sewers_f1 page, cited completely: the checks above find nothing wrong with it
	private static CiteBook sewersFixture( boolean withKey ){
		CiteBook book = new CiteBook();
		book.group = "EarlyCites";
		CiteBook.Page p = book.page( "sewers_f1" )
				.mob( Snake.class, "sewer snake" ).primary()
				.mob( BrownBat.class, "brown bat" )
				.mob( GreyRat.class, "grey rat" )
				.mob( RatBoss.class, "rat boss" )
				.mob( Rat.class, "rat" )
				.trap( WornDartTrap.class, "worn dart trap" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" );
		if (withKey) p.item( GoldenKey.class, "golden key" );
		return book;
	}

	@Test
	public void aFullyCitedPagePassesAndAGapIsNamed(){
		GuideGraph.Node node = GuideGraph.find( GuideGraph.build(), "sewers_f1" );
		CiteBook full = sewersFixture( true );
		assertProblems( "fixture", pageProblems( node, full.pages.get( "sewers_f1" ) ) );
		assertProblems( "fixture", realThingProblems( full ) );

		List<String> gap = pageProblems( node, sewersFixture( false ).pages.get( "sewers_f1" ) );
		assertEquals( gap.toString(), 1, gap.size() );
		assertTrue( gap.get( 0 ), gap.get( 0 ).contains( "\"golden key\"" ) );

		//a plain word that names a thing is caught, and so is a cite of something the page never says
		CiteBook wrong = sewersFixture( true );
		wrong.page( "sewers_f1" ).plain( "golden key" ).item( Food.class, "pasty" );
		List<String> bad = pageProblems( node, wrong.pages.get( "sewers_f1" ) );
		assertTrue( bad.toString(), bad.toString().contains( "is the name of a game thing" ) );
		assertTrue( bad.toString(), bad.toString().contains( "\"pasty\" names nothing" ) );

		//two cites sharing a word, a boss page with a primary, a tile outside its sheet
		CiteBook twice = new CiteBook();
		twice.group = "EarlyCites";
		twice.page( "sewers_f1" ).mob( Rat.class, "rat" ).mob( GreyRat.class, "grey rat", "rats" );
		twice.page( "boss_goo" ).sprite( ItemSpriteSheet.TOMB, "tomb" ).primary()
				.terrain( Assets.Environment.TILES_SEWERS, 100000, "floor" );
		String all = realThingProblems( twice ).toString();
		assertTrue( all, all.contains( "is also on the cite \"rat\"" ) );
		assertTrue( all, all.contains( "a boss page's card already shows its boss" ) );
		assertTrue( all, all.contains( "outside its tileset" ) );

		//the boss page must cite its boss
		GuideGraph.Node goo = GuideGraph.find( GuideGraph.build(), "boss_goo" );
		assertTrue( pageProblems( goo, twice.pages.get( "boss_goo" ) ).toString().contains( "cites its own boss" ) );
	}

	@Test
	public void theBookRefusesWhatCannotBeCited(){
		CiteBook book = new CiteBook();
		book.group = "Fixture";
		CiteBook.Page p = book.page( "sewers_f1" );
		expectRefusal( () -> p.mob( Rat.class ) );
		expectRefusal( () -> p.mob( Rat.class, "rat", " " ) );
		expectRefusal( () -> p.mob( null, "rat" ) );
		expectRefusal( () -> p.icon( null, "search" ) );
		expectRefusal( () -> p.buff( Hunger.class, -1, "hunger" ) );
		expectRefusal( p::primary );
		p.mob( Rat.class, "rat" ).primary();
		expectRefusal( p::primary );
		//a page goes on where it was left
		assertSame( p, book.page( "sewers_f1" ) );
		assertEquals( 1, p.cites.size() );
		assertTrue( p.cites.get( 0 ).isPrimary() );
	}

	private interface Call { void run(); }

	private static void expectRefusal( Call c ){
		try {
			c.run();
			fail( "accepted" );
		} catch (IllegalArgumentException | IllegalStateException e){
			//refused, as it should be
		}
	}

	// ------------------------------------------------------------------ the pictures

	private static CiteBook everyKind(){
		CiteBook book = new CiteBook();
		book.group = "Fixture";
		book.page( "fixture" )
				.mob( Rat.class, "rat" )
				.hero( HeroClass.WARRIOR, "warrior" )
				.item( Food.class, "ration" )
				.item( PotionOfHealing.class, "potion of healing" )
				.sprite( ItemSpriteSheet.TOMB, "tomb" )
				.icon( Icons.MAGNIFY, "search" )
				.buff( Hunger.class, BuffIndicator.HUNGER, "hunger" )
				.buff( Poison.class, "poison" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.GRASS, "grass" )
				.water( Assets.Environment.WATER_SEWERS, "water" )
				.trap( WornDartTrap.class, "worn dart trap" )
				.plant( Sungrass.class, "sungrass" )
				.effect( GuideEffect.FIRE, "fire" )
				.custom( () -> {
					Image i = new Image( Assets.Environment.BRANCH_STAIRS );
					i.frame( i.texture.uvRectBySize( 16, 0, 16, 16 ) );
					return i;
				}, "stairs" );
		return book;
	}

	@Test
	public void everyKindCanBeDrawn(){
		float elapsed = Game.elapsed;
		try {
			List<Cite> cites = everyKind().pages.get( "fixture" ).cites;
			HashSet<Cite.Kind> kinds = new HashSet<>();
			for (Cite c : cites) kinds.add( c.kind );
			assertEquals( Cite.Kind.values().length, kinds.size() );
			assertProblems( "fixture", realThingProblems( everyKind() ) );

			for (Cite c : cites){
				Image art = CiteArt.icon( c, 18, 18, 3.5f, 7f );
				if (!c.still()){
					assertNull( art );
				} else {
					assertNotNull( c.caption(), art );
					assertTrue( c.caption() + " is wider than its box", art.width() <= 18 && art.height() <= 18 );
					assertFalse( c.caption() + " fell back to the unknown item", art instanceof ItemSprite
							&& sameFrame( art.frame(), ItemSpriteSheet.film.get( ItemSpriteSheet.SOMETHING ) ) );
				}

				CiteChip.Plate plate = new CiteChip.Plate( c, 0x59B848 );
				plate.setPos( 0, 0 );
				Game.elapsed = 0.05f;
				for (int f = 0; f < 120; f++) plate.update();
				plate.tap();
				for (int f = 0; f < 10; f++) plate.update();
				plate.destroy();
				if (art != null) art.destroy();
			}

			//a creature lives in a crop of its own; a potion shows its kind, never this run's colour
			assertTrue( CiteArt.icon( cites.get( 0 ), 18, 18, 0, 0 ) instanceof LiveCrop );
			Image potion = CiteArt.icon( cites.get( 3 ), 18, 18, 0, 0 );
			assertTrue( sameFrame( potion.frame(), ItemSpriteSheet.film.get( ItemSpriteSheet.POTION_HOLDER ) ) );
		} finally {
			Game.elapsed = elapsed;
		}
	}

	@Test
	public void everyEffectPoursAndTakesItsParticlesAlong(){
		float elapsed = Game.elapsed;
		try {
			int before = FxBudget.live( FxBudget.PARTICLES );
			for (GuideEffect e : GuideEffect.values()){
				CiteBook book = new CiteBook();
				book.group = "Fixture";
				book.page( "fixture" ).effect( e, e.name() );
				CiteChip.Plate plate = new CiteChip.Plate( book.pages.get( "fixture" ).cites.get( 0 ), 0xC04848 );
				plate.setPos( 0, 0 );
				Game.elapsed = 0.05f;
				int most = 0;
				for (int f = 0; f < 80; f++){
					plate.update();
					most = Math.max( most, living( plate ) );
				}
				assertTrue( e + " made no particles", most > 0 );
				plate.destroy();
				assertEquals( e + " left particles counted alive", before, FxBudget.live( FxBudget.PARTICLES ) );
			}
		} finally {
			Game.elapsed = elapsed;
		}
	}

	//every effect shows on its plate, opened over the game (its scene's layers there, the hero seeing
	//nothing of the level where the plate lies), and none of it lands in the game's scene
	@Test
	public void everyEffectShowsOnItsPlateAlone(){
		float elapsed = Game.elapsed;
		Level level = Dungeon.level;
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		try {
			Level dark = new Level(){
				@Override protected boolean build(){ return true; }
				@Override protected void createMobs(){ }
				@Override protected void createItems(){ }
			};
			dark.setSize( 16, 16 );
			dark.heroFOV = new boolean[dark.length()];
			Dungeon.level = dark;
			for (GuideEffect e : GuideEffect.values()){
				CiteBook book = new CiteBook();
				book.group = "Fixture";
				book.page( "fixture" ).effect( e, e.name() );
				CiteChip.Plate plate = new CiteChip.Plate( book.pages.get( "fixture" ).cites.get( 0 ), 0xC04848 );
				//a window's camera, never the game's (Camera.main): what it draws is the window's
				plate.camera = new Camera( 0, 0, 64, 64, 1 );
				plate.setPos( 8, 8 );
				Game.elapsed = 0.05f;
				int most = 0;
				for (int f = 0; f < 80; f++){
					plate.update();
					most = Math.max( most, shown( plate ) );
				}
				assertTrue( e + " showed nothing", most > 0 );
				plate.destroy();
				for (Group l : layers) assertEquals( e + " reached the game's scene", 0, l.length );
			}
		} finally {
			GameScene.useLayersForTests( null );
			Dungeon.level = level;
			Game.elapsed = elapsed;
		}
	}

	//how many particles of its effect show
	private static int shown( CiteChip.Plate plate ){
		int n = 0;
		for (Gizmo g : plate.membersView()){
			if (!(g instanceof Emitter)) continue;
			for (Gizmo p : ((Emitter) g).membersView()){
				if (p.exists && p.visible && p instanceof Visual && ((Visual) p).alpha() > 0) n++;
			}
		}
		return n;
	}

	private static int living( CiteChip.Plate plate ){
		for (Gizmo g : plate.membersView()){
			if (g instanceof Emitter) return ((Emitter) g).countLiving();
		}
		return 0;
	}

	//A trap's and a plant's picture is cut from the sheet the texture cache holds now. The features map
	//a game scene left behind holds the texture the next scene's cache clear deleted (PixelScene.create:
	//the guide's scene clears it on its way in), and that texture's GL name then drew whatever texture
	//was made next under it: every trap and plant plate a solid square
	@Test
	public void trapsAndPlantsDrawFromTheLiveSheet() throws Exception {
		Field instance = TerrainFeaturesTilemap.class.getDeclaredField( "instance" );
		instance.setAccessible( true );
		Object map = instance.get( null );
		Level level = Dungeon.level;
		GL20 gl = Gdx.gl, gl20 = Gdx.gl20;
		//deleting a texture reaches the GL: one that does nothing
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
		try {
			//the game scene's map, then the guide's scene clearing the cache
			Dungeon.level = null;
			new TerrainFeaturesTilemap( new SparseArray<>(), new SparseArray<>() );
			TextureCache.remove( Assets.Environment.TERRAIN_FEATURES );

			SmartTexture live = TextureCache.get( Assets.Environment.TERRAIN_FEATURES );
			TextureFilm tiles = new TextureFilm( live, 16, 16 );
			Trap trap = new WornDartTrap();
			Image t = TerrainFeaturesTilemap.getTrapVisual( trap );
			assertSame( live, t.texture );
			assertTrue( nearFrame( t.frame(), tiles.get( (trap.active ? trap.color : Trap.BLACK) + trap.shape * 16 ) ) );
			//the plants and the homestead's crops (hs_farming), all cut from it
			for (Class<?> c : new Class<?>[]{ Sungrass.class, BlandfruitBush.class, Cornwheat.class, Apricobush.class,
					Gobgrape.class, Eggbloom.class, Tomatobush.class }){
				Plant plant = (Plant) Reflection.newInstance( c );
				Image p = TerrainFeaturesTilemap.getPlantVisual( plant );
				assertNotNull( c.getSimpleName(), p );
				assertSame( c.getSimpleName(), live, p.texture );
				assertTrue( c.getSimpleName(), nearFrame( p.frame(), tiles.get( plant.image + 7 * 16 ) ) );
			}
		} finally {
			Gdx.gl = gl;
			Gdx.gl20 = gl20;
			Dungeon.level = level;
			instance.set( null, map );
		}
	}

	//the same tile, to a float's rounding
	private static boolean nearFrame( RectF a, RectF b ){
		return a != null && b != null && Math.abs( a.left - b.left ) < 1e-6f && Math.abs( a.top - b.top ) < 1e-6f
				&& Math.abs( a.right - b.right ) < 1e-6f && Math.abs( a.bottom - b.bottom ) < 1e-6f;
	}

	private static boolean sameFrame( RectF a, RectF b ){
		return a != null && b != null && a.left == b.left && a.top == b.top && a.right == b.right && a.bottom == b.bottom;
	}

	@Test
	public void aCreatureStrikesAndGoesBackToIdling() throws Exception {
		Field curAnim = MovieClip.class.getDeclaredField( "curAnim" );
		curAnim.setAccessible( true );
		float elapsed = Game.elapsed;
		try {
			CharSprite rat = GuidePuppet.make( Rat.class );
			assertNotNull( rat );
			assertNull( "a puppet has no creature", rat.ch );
			Object idle = curAnim.get( rat );
			assertTrue( GuidePuppet.act( rat ) );
			assertTrue( curAnim.get( rat ) != idle );
			Game.elapsed = 0.05f;
			for (int f = 0; f < 60; f++) rat.update();
			assertSame( "back to idling once the strike ends", idle, curAnim.get( rat ) );

			//an attack slot that is the looped idle is never played: it would never end
			Field attack = CharSprite.class.getDeclaredField( "attack" );
			Field zap = CharSprite.class.getDeclaredField( "zap" );
			Field operate = CharSprite.class.getDeclaredField( "operate" );
			attack.setAccessible( true );
			zap.setAccessible( true );
			operate.setAccessible( true );
			attack.set( rat, idle );
			zap.set( rat, null );
			operate.set( rat, null );
			assertFalse( GuidePuppet.act( rat ) );

			//a crop keeps its footprint whatever the frame, and a giant fits the box
			LiveCrop crop = new LiveCrop( GuidePuppet.make( Rat.class ), 20, 20, 0, 0 );
			float w = crop.width(), h = crop.height();
			crop.act();
			for (int f = 0; f < 30; f++){
				crop.update();
				assertEquals( w, crop.width(), 0 );
				assertEquals( h, crop.height(), 0 );
			}
			crop.destroy();
			CharSprite yog = GuidePuppet.make( YogDzewa.class );
			assertNotNull( yog );
			LiveCrop giant = new LiveCrop( yog, 20, 20, 6, 10 );
			assertTrue( giant.width() <= 20 && giant.height() <= 20 );
			for (int f = 0; f < 30; f++) giant.update();
			giant.destroy();
		} finally {
			Game.elapsed = elapsed;
		}
	}
}
