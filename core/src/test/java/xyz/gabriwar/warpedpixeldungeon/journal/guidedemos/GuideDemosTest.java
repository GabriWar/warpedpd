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

package xyz.gabriwar.warpedpixeldungeon.journal.guidedemos;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.CiteMatch;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.DemoShot;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.DemoStage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.Reflection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The Descent Guide's acted-out scenes against the guide and the game: every demo plays a page of
 * the group that owns its chapter, names creatures, items, animations and words that exist, moves
 * its creatures only where they can go, and plays through and starts over headlessly (its stage is
 * built and run, its words left out: they need the platform's fonts). The group methods
 * (earlyBossesActOut...) are the coverage a content branch makes pass: every boss page has a demo,
 * and so has every page of the basics.
 */
public class GuideDemosTest {

	private static final String GUIDE = "src/main/assets/messages/guide/guide.properties";
	private static final String ASSETS = "src/main/assets/";
	private static final int MAX_DEMOS = 3, MAX_CAPTION = 48, MAX_STATUS = 16;

	//the basics chapter's pages, as the tome's merge promises them (they may not exist yet on a branch)
	static final List<String> BASICS = Arrays.asList(
			"basics_intro", "basics_examining", "basics_searching", "basics_surprise", "basics_positioning",
			"basics_hunger", "basics_identifying", "basics_strength", "basics_upgrades", "basics_levelling",
			"basics_skills", "basics_magic", "basics_looting", "basics_alchemy", "basics_enchanting",
			"basics_farming", "basics_defeat" );

	private static Properties guide;
	//each group's own words, by group
	private static final HashMap<String, Properties> words = new HashMap<>();

	@BeforeClass
	public static void boot() throws IOException {
		WarpedRoomsTest.boot();
		guide = load( GUIDE );
		for (int i = 0; i < GuideDemos.GROUPS.length; i++){
			words.put( GuideDemos.GROUPS[i][0], load( ASSETS + GuideDemos.BUNDLES[i] + ".properties" ) );
		}
	}

	private static Properties load( String path ) throws IOException {
		Properties p = new Properties();
		try (InputStreamReader r = new InputStreamReader( new FileInputStream( new File( path ) ), StandardCharsets.UTF_8 )){
			p.load( r );
		}
		return p;
	}

	private static void assertProblems( String what, List<String> problems ){
		if (problems.isEmpty()) return;
		StringBuilder sb = new StringBuilder( what + ": " + problems.size() + " problem(s)\n" );
		for (int i = 0; i < problems.size() && i < 60; i++) sb.append( problems.get( i ) ).append( '\n' );
		if (problems.size() > 60) sb.append( "...and " ).append( problems.size() - 60 ).append( " more\n" );
		fail( sb.toString() );
	}

	private static String where( Demo d ){
		return d.key + " (demo " + d.index + ", " + d.group + ")";
	}

	// ------------------------------------------------------------------ owners

	//the pages under a group's chapters that exist
	private static ArrayList<GuideGraph.Node> pagesOf( GuideGraph.Node root, String[] group ){
		ArrayList<GuideGraph.Node> out = new ArrayList<>();
		for (int i = 1; i < group.length; i++){
			GuideGraph.Node ch = GuideGraph.find( root, group[i] );
			if (ch == null) continue;
			for (GuideGraph.Node n : GuideGraph.all( ch )) if (n.key != null) out.add( n );
		}
		return out;
	}

	private static boolean basicsExist( GuideGraph.Node root ){
		return GuideGraph.find( root, "ch_basics" ) != null;
	}

	@Test
	public void everyDemoBelongsToItsGroup(){
		ArrayList<String> problems = new ArrayList<>();
		for (String k : GuideDemos.failures.keySet()) problems.add( "registering " + k + " failed: " + GuideDemos.failures.get( k ) );
		GuideGraph.Node root = GuideGraph.build();
		HashMap<String, String> owner = new HashMap<>();
		for (String[] g : GuideDemos.GROUPS){
			for (GuideGraph.Node n : pagesOf( root, g )) owner.put( n.key, g[0] );
		}
		//the basics as promised, while their chapter is still to come
		if (!basicsExist( root )) for (String k : BASICS) owner.put( k, "BasicsDemos" );

		HashMap<String, Integer> perPage = new HashMap<>();
		for (Demo d : GuideDemos.book().demos){
			String o = owner.get( d.key );
			if (o == null){
				problems.add( where( d ) + ": no page of the guide is called " + d.key + " (check the key)" );
			} else if (!o.equals( d.group )){
				problems.add( where( d ) + ": the page's chapter is " + o + "'s (write it there)" );
			}
			int n = perPage.containsKey( d.key ) ? perPage.get( d.key ) + 1 : 1;
			perPage.put( d.key, n );
			if (n == MAX_DEMOS + 1) problems.add( d.key + ": more than " + MAX_DEMOS + " demos on one page" );
		}
		assertProblems( "demos", problems );
	}

	// ------------------------------------------------------------------ what they name

	@Test
	public void everyDemoResolves(){
		ArrayList<String> problems = new ArrayList<>();
		for (Demo d : GuideDemos.book().demos) problems.addAll( demoProblems( d ) );
		assertProblems( "demos", problems );
	}

	//what is wrong with one demo: what it names, its words, where its creatures go
	private static ArrayList<String> demoProblems( Demo d ){
		ArrayList<String> out = new ArrayList<>();
		String at = where( d );
		if (d.map() == null){
			out.add( at + ": no stage( ... )" );
			return out;
		}
		if (d.steps().isEmpty()) out.add( at + ": nothing happens (no steps)" );

		//its words: the anchor in the page's text, every key its group's or the game's
		String text = guide.getProperty( "guide." + d.key );
		if (d.anchor() != null && text != null
				&& CiteMatch.find( CiteMatch.tokens( text ), CiteMatch.tokens( d.anchor() ) ) < 0){
			out.add( at + ": under( \"" + d.anchor() + "\" ) is not in the page's text (use its words as written)" );
		}

		//who is on it, and what they can play
		HashMap<String, CharSprite> sprites = new HashMap<>();
		for (Demo.Actor a : d.actors()){
			CharSprite s;
			if (a.mob != null){
				if (Modifier.isAbstract( a.mob.getModifiers() )){
					out.add( at + ": " + a.id + " is no concrete Mob" );
					continue;
				}
				s = GuidePuppet.make( a.mob );
			} else {
				s = GuidePuppet.hero( a.hero != null ? a.hero : HeroClass.WARRIOR, 1 );
			}
			if (s == null) out.add( at + ": " + a.id + "'s sprite cannot be made" );
			else sprites.put( a.id, s );
		}
		for (Demo.Prop p : d.props()){
			String wrong = propProblem( p );
			if (wrong != null) out.add( at + ": " + p.id + " " + wrong );
		}

		//the creatures' cells through the timeline, and the terrain under them
		int[] map = d.map();
		HashMap<String, Integer> cell = new HashMap<>();
		for (Demo.Actor a : d.actors()){
			cell.put( a.id, a.cell );
			if (!standable( map[a.cell] )) out.add( at + ": " + a.id + " starts on terrain it cannot stand on" );
		}
		for (Demo.Step s : d.steps()){
			String step = at + " at " + s.at + "s, " + s.op + ( s.who != null ? " " + s.who : "" );
			switch (s.op){
				case MOVE: {
					int from = cell.get( s.who );
					for (int c : s.cells){
						if (dist( d, from, c ) != 1) out.add( step + ": each step of a walk is to a neighbouring cell" );
						if (!standable( map[c] )) out.add( step + ": walks onto terrain it cannot stand on" );
						from = c;
					}
					cell.put( s.who, from );
					break;
				}
				case KNOCK: {
					int from = cell.get( s.who ), to = s.cells[0];
					int dx = to % d.width() - from % d.width(), dy = to / d.width() - from / d.width();
					if ((dx == 0 && dy == 0) || (dx != 0 && dy != 0 && Math.abs( dx ) != Math.abs( dy ))){
						out.add( step + ": a knockback runs along a row, a column or a diagonal" );
					}
					if (!standable( map[to] )) out.add( step + ": lands on terrain it cannot stand on" );
					cell.put( s.who, to );
					break;
				}
				case JUMP:
					if (!standable( map[s.cells[0]] )) out.add( step + ": lands on terrain it cannot stand on" );
					cell.put( s.who, s.cells[0] );
					break;
				case ANIM: {
					CharSprite sp = sprites.get( s.who );
					if (sp != null && GuidePuppet.named( sp, s.text ) == null){
						out.add( step + ": its sprite (" + sp.getClass().getSimpleName() + ") has no animation called " + s.text );
					}
					break;
				}
				case STATUS: {
					String w = words( d, s.text, out, step );
					if (w != null && w.length() > MAX_STATUS) out.add( step + ": \"" + w + "\" is longer than " + MAX_STATUS + " characters" );
					break;
				}
				case CAPTION:
					if (s.text != null){
						String w = words( d, s.text, out, step );
						if (w != null && w.length() > MAX_CAPTION) out.add( step + ": the caption is longer than " + MAX_CAPTION + " characters" );
					}
					break;
				case BUFF: {
					try {
						Buff b = (Buff) Reflection.newInstanceUnhandled( s.buff );
						if ((s.n >= 0 ? s.n : b.icon()) == BuffIndicator.NONE){
							out.add( step + ": " + s.buff.getSimpleName() + " shows no icon (pass a BuffIndicator icon)" );
						}
					} catch (Exception e){
						out.add( step + ": " + s.buff.getSimpleName() + " cannot be made: " + e );
					}
					break;
				}
				case TERRAIN:
					for (int c : s.cells) map[c] = s.n;
					break;
				case SHOOT:
					if (s.shot.item != null){
						String wrong = propProblem( new Demo.Prop( "thrown", Demo.Prop.Kind.ITEM, s.shot.item, -1, s.cells[0] ) );
						if (wrong != null) out.add( step + ": what it throws " + wrong );
					}
					break;
				default:
					break;
			}
		}
		return out;
	}

	//a step's words: its own group's (guidedemo.<page>.<key>, in its bundle) or the game's
	private static String words( Demo d, String key, ArrayList<String> out, String step ){
		if (Demo.absolute( key )){
			String w = Messages.get( key );
			if (w.equals( Messages.NO_TEXT_FOUND )){
				out.add( step + ": the game has no words called " + key );
				return null;
			}
			return w;
		}
		String w = words.get( d.group ).getProperty( d.localKey( key ) );
		if (w == null) out.add( step + ": " + d.localKey( key ) + " is not in " + d.group + "'s bundle" );
		return w;
	}

	private static boolean standable( int terrain ){
		return (Terrain.flags[terrain] & Terrain.PASSABLE) != 0 || terrain == Terrain.CHASM;
	}

	private static int dist( Demo d, int a, int b ){
		return Math.max( Math.abs( a % d.width() - b % d.width() ), Math.abs( a / d.width() - b / d.width() ) );
	}

	private static String propProblem( Demo.Prop p ){
		try {
			switch (p.kind){
				case ITEM: {
					if (!Item.class.isAssignableFrom( p.type ) || Modifier.isAbstract( p.type.getModifiers() )) return "is no concrete Item";
					Item item = (Item) Reflection.newInstanceUnhandled( p.type );
					if (item.icon != -1) return ItemSpriteSheet.Icons.film.get( item.icon ) != null ? null : "has no icon";
					return ItemSpriteSheet.film.get( item.image() ) != null ? null : "has no sprite";
				}
				case SPRITE:
					return ItemSpriteSheet.film.get( p.image ) != null ? null : "is no frame of the item sheet";
				default:
					Reflection.newInstanceUnhandled( p.type );
					return null;
			}
		} catch (Exception e){
			return "cannot be made: " + e;
		}
	}

	@Test
	public void demosOwnTheirWords(){
		ArrayList<String> problems = new ArrayList<>();
		for (String[] g : GuideDemos.GROUPS){
			HashSet<String> used = new HashSet<>();
			for (Demo d : GuideDemos.book().demos){
				if (!d.group.equals( g[0] )) continue;
				for (Demo.Step s : d.steps()){
					if ((s.op == Demo.Op.CAPTION || s.op == Demo.Op.STATUS) && s.text != null && !Demo.absolute( s.text )){
						used.add( d.localKey( s.text ) );
					}
				}
			}
			for (String k : words.get( g[0] ).stringPropertyNames()){
				if (!used.contains( k )) problems.add( g[0] + ": " + k + " is in its bundle but no demo of it says it (drop it)" );
			}
		}
		assertProblems( "words", problems );
	}

	// ------------------------------------------------------------------ playing

	//a stage with no words: a test has no fonts to draw them with
	private static DemoStage stage( Demo d ){
		return new DemoStage( d, 0x59B848, 120 ){
			@Override
			protected Words words( String text, int size ) {
				return null;
			}
		};
	}

	@Test
	public void everyDemoPlaysAndStartsOver(){
		ArrayList<String> problems = new ArrayList<>();
		int before = FxBudget.live( FxBudget.PARTICLES );
		for (Demo d : GuideDemos.book().demos){
			problems.addAll( plays( d ) );
		}
		if (FxBudget.live( FxBudget.PARTICLES ) != before){
			problems.add( "the stages left particles counted alive in the effects budget" );
		}
		assertProblems( "playing", problems );
	}

	//plays a demo through twice, frame by frame
	private static ArrayList<String> plays( Demo d ){
		ArrayList<String> out = new ArrayList<>();
		float elapsed = Game.elapsed;
		DemoStage s;
		try {
			s = stage( d );
			s.setPos( 0, 0 );
		} catch (Exception e){
			out.add( where( d ) + ": its stage cannot be built: " + e );
			return out;
		}
		try {
			Game.elapsed = 0.05f;
			float last = 0;
			int loops = 0;
			int frames = (int)Math.ceil( 2 * (d.length() + Demo.REST) / Game.elapsed ) + 4;
			for (int f = 0; f < frames; f++){
				s.update();
				if (s.played() < last) loops++;
				last = s.played();
			}
			if (loops < 2) out.add( where( d ) + ": it did not start over (" + loops + " loops in " + frames + " frames)" );
			s.tap();
			float held = s.played();
			for (int f = 0; f < 10; f++) s.update();
			if (s.played() != held) out.add( where( d ) + ": a tap does not hold it still" );
			s.tap();
		} catch (Exception e){
			out.add( where( d ) + ": playing it threw " + e );
		} finally {
			s.destroy();
			Game.elapsed = elapsed;
		}
		return out;
	}

	//every kind of step, on a stage of its own, plays and starts over
	@Test
	public void everyStepPlays(){
		DemoBook book = new DemoBook();
		book.group = "Fixture";
		Demo.Look half = ( s, on ) -> s.alpha( on ? 0.5f : 1f );
		Demo d = book.demo( "fixture" ).stage( Demo.Region.PRISON,
						"#######",
						"..~~...",
						".......",
						"..|...." )
				.mob( "rat", Rat.class, 1, 2 ).hero( "you", 4, 2 )
				.item( "food", Food.class, 5, 3 ).hide( "food" )
				.turn()
				.move( "you", 3, 2 ).beat().attack( "you", 2, 2 ).beat().hurt( "rat", 2 ).surprise( "rat" )
				.zap( "rat", 3, 2 ).operate( "you", 2, 2 ).idle( "you" ).face( "rat", 6, 2 )
				.bolt( "rat", MagicMissile.SHAMAN_RED, 3, 2 ).toss( "you", Food.class, 1, 2 )
				.ray( "rat", Effects.Type.DEATH_RAY, 6, 2 ).lightning( "you", 1, 2 ).chain( "rat", 3, 2 )
				.heal( "you", 3 ).status( "you", "actors.char.def_verb", CharSprite.NEUTRAL )
				.icon( "rat", Icons.SLEEP ).buff( "you", Hunger.class, BuffIndicator.HUNGER ).clear( "rat" )
				.freeze( "you" ).thaw( "you" ).look( "rat", half, true )
				.turn()
				.look( "rat", half, false ).show( "food" )
				.burst( GuideEffect.BLOOD, 2, 2 ).pour( GuideEffect.TOXIC_GAS, 3, 1, 4, 1 ).stop( 3, 1 )
				.mark( Demo.Mark.DANGER, 1, 1 ).mark( Demo.Mark.TARGET, 2, 1 ).mark( Demo.Mark.SAFE, 3, 3 )
				.mark( Demo.Mark.HINT, 4, 3 ).unmark( 1, 1 )
				.arrow( Demo.Mark.SAFE, 1, 2, 4, 2 ).arrow( Demo.Mark.HINT, 1, 1, 3, 3 )
				.terrain( Terrain.WATER, 5, 2 )
				.turn()
				.stop().unmark().unarrow()
				.knock( "rat", 1, 1 ).jump( "you", 5, 1 )
				.turn()
				.die( "rat" ).uncaption()
				.turns( 2 )
				.show( "rat" )
				.turn();
		assertProblems( "fixture", demoProblems( d ) );
		assertProblems( "fixture", plays( d ) );
	}

	//every kind of shot shows on its way and is gone once it is over, and none of it lands in the game's
	//scene (as under a page opened in play: its layers are there)
	@Test
	public void everyShotShowsOnItsStageAlone(){
		DemoBook book = new DemoBook();
		book.group = "Fixture";
		Demo d = book.demo( "fixture" ).stage( Demo.Region.SEWERS, "#####", ".....", "....." )
				.hero( "you", 0, 1 ).mob( "rat", Rat.class, 4, 1 )
				.bolt( "you", MagicMissile.MAGIC_MISSILE, 4, 1 ).bolt( "you", MagicMissile.SHADOW, 4, 1 )
				.toss( "you", Food.class, 4, 2 ).ray( "you", Effects.Type.DEATH_RAY, 4, 1 )
				.lightning( "you", 4, 1 ).chain( "you", 4, 1 )
				.turn();
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		float elapsed = Game.elapsed;
		GameScene.useLayersForTests( layers );
		try {
			Game.elapsed = 0.05f;
			for (Demo.Step st : d.steps()){
				DemoShot shot = DemoShot.make( st.shot, 100 );
				shot.fire( 8, 8, 72, 8, Demo.BEAT );
				for (int f = 0; f < 3; f++) shot.update();
				assertTrue( st.shot.kind + " shows nothing on its way", shown( shot ) > 0 );
				for (int f = 0; f < 40; f++) shot.update();
				assertEquals( st.shot.kind + " still shows once it is over", 0, shown( shot ) );
				shot.destroy();
				for (Group l : layers) assertEquals( st.shot.kind + " reached the game's scene", 0, l.length );
			}
		} finally {
			GameScene.useLayersForTests( null );
			Game.elapsed = elapsed;
		}
	}

	//an item lying hidden stays hidden until it is shown (an item's sprite with no heap shows itself on
	//every update)
	@Test
	public void aHiddenItemStaysHidden(){
		DemoBook book = new DemoBook();
		book.group = "Fixture";
		Demo d = book.demo( "fixture" ).stage( Demo.Region.SEWERS, "...", "..." )
				.item( "food", Food.class, 1, 1 ).hide( "food" )
				.turn()
				.show( "food" )
				.turn();
		float elapsed = Game.elapsed;
		DemoStage s = stage( d );
		try {
			s.setPos( 0, 0 );
			Game.elapsed = 0.05f;
			for (int f = 0; f < 10; f++) s.update();
			assertEquals( 0, shownItems( s ) );
			for (int f = 0; f < 15; f++) s.update();
			assertEquals( 1, shownItems( s ) );
		} finally {
			s.destroy();
			Game.elapsed = elapsed;
		}
	}

	private static int shownItems( Gizmo g ){
		if (!g.exists || !g.visible) return 0;
		if (g instanceof ItemSprite) return 1;
		int n = 0;
		if (g instanceof Group) for (Gizmo m : ((Group) g).membersView()) n += shownItems( m );
		return n;
	}

	//how many pictures of it show
	private static int shown( Gizmo g ){
		if (!g.exists || !g.visible) return 0;
		if (g instanceof Group){
			int n = 0;
			for (Gizmo m : ((Group) g).membersView()) n += shown( m );
			return n;
		}
		return g instanceof Visual && ((Visual) g).alpha() > 0 ? 1 : 0;
	}

	@Test
	public void everyTerrainDrawsInEveryRegion(){
		float elapsed = Game.elapsed;
		try {
			List<Integer> all = Demo.terrains();
			for (Demo.Region r : Demo.Region.values()){
				//every terrain of the legend side by side, under a row of wall
				DemoBook book = new DemoBook();
				book.group = "Fixture";
				StringBuilder legend = new StringBuilder();
				for (char c = 33; c < 127; c++){
					try {
						new DemoBook().demo( "x" ).stage( r, "" + c + c + c, "..." );
						legend.append( c );
					} catch (IllegalArgumentException e){
						//no terrain of the legend
					}
				}
				assertEquals( all.size(), legend.length() );
				ArrayList<String> rows = new ArrayList<>();
				rows.add( "#######" );
				for (int i = 0; i < legend.length(); i += 7){
					StringBuilder row = new StringBuilder( legend.substring( i, Math.min( legend.length(), i + 7 ) ) );
					while (row.length() < 7) row.append( '.' );
					rows.add( row.toString() );
				}
				while (rows.size() < 7) rows.add( "......." );
				Demo d = book.demo( "fixture" ).stage( r, rows.toArray( new String[0] ) )
						.terrain( Terrain.WATER, 0, 6, 1, 6 ).turn();
				DemoStage s = stage( d );
				s.setPos( 0, 0 );
				Game.elapsed = 0.05f;
				for (int f = 0; f < 60; f++) s.update();
				s.destroy();
			}
		} finally {
			Game.elapsed = elapsed;
		}
	}

	// ------------------------------------------------------------------ coverage

	@Test
	public void surfaceBossesActOut(){
		bossesActOut( 0 );
	}

	@Test
	public void earlyBossesActOut(){
		bossesActOut( 1 );
	}

	@Test
	public void lateBossesActOut(){
		bossesActOut( 2 );
	}

	@Test
	public void extraBossesActOut(){
		bossesActOut( 3 );
	}

	//every boss page of a group's chapters has a demo
	private void bossesActOut( int i ){
		String[] g = GuideDemos.GROUPS[i];
		ArrayList<String> problems = new ArrayList<>();
		for (GuideGraph.Node n : pagesOf( GuideGraph.build(), g )){
			if (n.mob != null && GuideDemos.of( n.key ).isEmpty()){
				problems.add( n.key + ": a boss page acts out how it fights (book.demo( \"" + n.key + "\" ) in " + g[0] + ")" );
			}
		}
		assertProblems( g[0], problems );
	}

	//every page of the basics has a demo: the chapter's own pages, or the ones promised while it is still to come
	@Test
	public void basicsPagesActOut(){
		GuideGraph.Node root = GuideGraph.build();
		ArrayList<String> keys = new ArrayList<>();
		if (basicsExist( root )){
			for (GuideGraph.Node n : pagesOf( root, GuideDemos.GROUPS[4] )) keys.add( n.key );
		} else {
			keys.addAll( BASICS );
		}
		ArrayList<String> problems = new ArrayList<>();
		for (String k : keys){
			if (GuideDemos.of( k ).isEmpty()) problems.add( k + ": a basics page acts out what it teaches (book.demo( \"" + k + "\" ) in BasicsDemos)" );
		}
		assertProblems( "BasicsDemos", problems );
	}

	// ------------------------------------------------------------------ the builder

	@Test
	public void aDemoRefusesWhatCannotPlay(){
		DemoBook book = new DemoBook();
		book.group = "Fixture";
		Demo d = book.demo( "fixture" );
		expectRefusal( () -> d.mob( "rat", Rat.class, 1, 1 ) );              //before its stage
		expectRefusal( () -> d.stage( Demo.Region.SEWERS, "..", ".." ) );    //too narrow
		expectRefusal( () -> d.stage( Demo.Region.SEWERS, "...", "..?" ) );  //no such terrain
		expectRefusal( () -> d.stage( Demo.Region.SEWERS, "...", "...." ) ); //ragged
		d.stage( Demo.Region.SEWERS, "#####", ".....", "....." );
		expectRefusal( () -> d.stage( Demo.Region.SEWERS, "...", "..." ) );  //a second stage
		expectRefusal( () -> d.mob( "rat", Rat.class, 1, 0 ) );              //its head over the top
		expectRefusal( () -> d.mob( "rat", Rat.class, 5, 1 ) );              //off the stage
		d.mob( "rat", Rat.class, 1, 1 ).hero( "you", 3, 1 ).item( "food", Food.class, 2, 2 );
		expectRefusal( () -> d.mob( "rat", Rat.class, 2, 1 ) );              //two called rat
		expectRefusal( () -> d.move( "nobody", 2, 1 ) );
		expectRefusal( () -> d.move( "rat", 2 ) );                           //half a pair
		expectRefusal( () -> d.attack( "food", 2, 1 ) );                     //an item does not strike
		expectRefusal( () -> d.hurt( "rat", 0 ) );
		expectRefusal( () -> d.turns( 0 ) );
		expectRefusal( () -> d.caption( "actors.char.def_verb" ) );          //a caption is the demo's own
		expectRefusal( () -> d.arrow( Demo.Mark.HINT, 0, 1, 2, 2 ) );         //neither straight nor diagonal
		expectRefusal( () -> d.burst( GuideEffect.FIRE ) );                   //on no cell
		expectRefusal( () -> d.terrain( Terrain.MINE_CRYSTAL, 1, 1 ) );      //no terrain a stage draws
		expectRefusal( () -> d.bolt( "rat", MagicMissile.FIRE_CONE, 3, 1 ) ); //a cone is no bolt
		expectRefusal( () -> d.ray( "rat", Effects.Type.CHAIN, 3, 1 ) );      //a chain is no ray
		expectRefusal( () -> d.toss( "food", Food.class, 3, 1 ) );           //an item throws nothing
		d.hide( "food" ).turn().show( "food" ).unmark().stop().buff( "you", Paralysis.class );
		assertEquals( 5, d.steps().size() );
		//the last step has a turn to be seen
		assertTrue( d.length() >= d.steps().get( 4 ).at + Demo.TURN );
		assertEquals( 1, book.demo( "fixture" ).index );
		//what a stage's legend draws is every terrain it accepts
		for (int t : Demo.terrains()) assertTrue( Demo.drawable( t ) );
		assertTrue( !Demo.drawable( Terrain.MINE_CRYSTAL ) );
		//a prop of every kind
		Demo props = book.demo( "fixture" ).stage( Demo.Region.CAVES, "...", "..." )
				.item( "potion", PotionOfHealing.class, 0, 0 ).sprite( "tomb", ItemSpriteSheet.TOMB, 1, 0 )
				.plant( "sungrass", Sungrass.class, 2, 0 ).trap( "dart", WornDartTrap.class, 0, 1 )
				.hero( "you", HeroClass.ROGUE, 1, 1 ).icon( "you", Icons.ALERT ).turn();
		for (Demo.Prop p : props.props()) assertEquals( p.id, null, propProblem( p ) );
		assertTrue( plays( props ).isEmpty() );
	}

	private interface Call { void run(); }

	private static void expectRefusal( Call c ){
		try {
			c.run();
			fail( "accepted" );
		} catch (IllegalArgumentException e){
			//refused, as it should be
		}
	}
}
