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

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.watabou.utils.RectF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import org.junit.BeforeClass;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Every concrete Item in the game, built with no run loaded (the title screen:
 * Dungeon.hero is null). Each one has its text built the way the journal
 * builds it, and its sprite frame looked up.
 *
 * Fails only on what can crash a player: a catalog item (the journal lists
 * those from the title screen) whose text throws. Everything else lands in
 * core/build/reports/items/: crashes.txt (items that throw outside a run -
 * usually harmless, they only exist inside one) and sprites.tsv, which
 * tools/item_sprites.py turns into a sheet of missing, empty and torn frames.
 */
public class AllItemsTest {

	private static final File REPORT = new File( "build/reports/items" );

	//just enough of Gdx.files for the message bundles: every path resolves under the assets
	private static final class AssetFiles implements com.badlogic.gdx.Files {
		private final File root = new File( "src/main/assets" );
		private FileHandle at( String path ){ return new FileHandle( new File( root, path ) ); }
		@Override public FileHandle getFileHandle( String path, FileType type ){ return at( path ); }
		@Override public FileHandle classpath( String path ){ return at( path ); }
		@Override public FileHandle internal( String path ){ return at( path ); }
		@Override public FileHandle external( String path ){ return at( path ); }
		@Override public FileHandle absolute( String path ){ return new FileHandle( new File( path ) ); }
		@Override public FileHandle local( String path ){ return at( path ); }
		@Override public String getExternalStoragePath(){ return root.getAbsolutePath(); }
		@Override public boolean isExternalStorageAvailable(){ return false; }
		@Override public String getLocalStoragePath(){ return root.getAbsolutePath(); }
		@Override public boolean isLocalStorageAvailable(){ return false; }
	}

	//in-memory preferences: every get answers the stored value or the caller's default
	private static Preferences memoryPrefs(){
		HashMap<String, Object> store = new HashMap<>();
		return (Preferences) Proxy.newProxyInstance( Preferences.class.getClassLoader(),
				new Class<?>[]{ Preferences.class }, (proxy, m, args) -> {
					String n = m.getName();
					if (n.startsWith( "put" )){
						if (args.length == 2) store.put( (String) args[0], args[1] );
						return proxy;
					}
					if (n.equals( "contains" )) return store.containsKey( (String) args[0] );
					if (n.startsWith( "get" ) && args != null && args.length >= 1){
						Object v = store.get( (String) args[0] );
						if (v != null) return v;
						if (args.length == 2) return args[1];
						Class<?> r = m.getReturnType();
						if (r == boolean.class) return false;
						if (r == int.class) return 0;
						if (r == long.class) return 0L;
						if (r == float.class) return 0f;
						return r == String.class ? "" : null;
					}
					return null;
				} );
	}

	@BeforeClass
	public static void titleScreen(){
		GdxNativesLoader.load();
		Gdx.files = new AssetFiles();
		final Preferences prefs = memoryPrefs();
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(),
				new Class<?>[]{ Application.class }, (proxy, m, args) -> {
					if (m.getName().equals( "getPreferences" )) return prefs;
					if (m.getName().equals( "getType" )) return Application.ApplicationType.Desktop;
					Class<?> r = m.getReturnType();
					if (r == int.class || r == long.class) return 0;
					if (r == boolean.class) return false;
					return null;
				} );
		Messages.setup( Languages.ENGLISH );
		Dungeon.hero = null;
	}

	/**
	 * Anything the generator can make can end up on a shop counter, and a shop's price
	 * is the item's value times a depth factor: an item worth nothing is handed over for
	 * free (a technology shotgun was, in the town's shop). Every generated item must be
	 * worth at least a coin.
	 */
	@Test
	public void everyGeneratedItemHasAPrice(){
		TreeSet<String> free = new TreeSet<>();
		for (Generator.Category cat : Generator.Category.values()){
			//gold is money, and trinkets only come from the catalyst and transmutation,
			//never a shop roll (they are worth nothing in Shattered too)
			if (cat == Generator.Category.GOLD || cat == Generator.Category.TRINKET || cat.classes == null) continue;
			for (int i = 0; i < cat.classes.length; i++){
				Class<?> cls = cat.classes[i];
				//listed but never rolled (the class items sit in their lists at zero)
				if (chance( cat.defaultProbs, i ) <= 0 && chance( cat.defaultProbs2, i ) <= 0
						&& chance( cat.probs, i ) <= 0) continue;
				Item item;
				try {
					item = (Item) cls.getConstructor().newInstance();
				} catch (Throwable t){
					continue;
				}
				if (item.value() <= 0) free.add( cat + " " + cls.getName().replace( "xyz.gabriwar.warpedpixeldungeon.items.", "" ) );
			}
		}
		if (!free.isEmpty()) fail( free.size() + " generated items are worth nothing (free in any shop):\n" + String.join( "\n", free ) );
	}

	private static float chance( float[] probs, int i ){
		return probs == null || i >= probs.length ? 0 : probs[i];
	}

	//every concrete Item with a no-argument constructor, from the compiled classes
	private static List<Class<? extends Item>> allItems() throws IOException {
		Path root = new File( "build/classes/java/main" ).toPath();
		List<Class<? extends Item>> out = new ArrayList<>();
		try (Stream<Path> files = java.nio.file.Files.walk( root )){
			for (Path p : files.filter( f -> f.toString().endsWith( ".class" ) ).collect( Collectors.toList() )){
				String name = root.relativize( p ).toString().replace( File.separatorChar, '.' );
				name = name.substring( 0, name.length() - ".class".length() );
				Class<?> cls;
				try {
					cls = Class.forName( name, false, AllItemsTest.class.getClassLoader() );
				} catch (Throwable t){
					continue;
				}
				if (!Item.class.isAssignableFrom( cls ) || Modifier.isAbstract( cls.getModifiers() )) continue;
				if (cls.isAnonymousClass() || cls.isLocalClass()) continue;
				if (cls.getEnclosingClass() != null && !Modifier.isStatic( cls.getModifiers() )) continue;
				try {
					if (!Modifier.isPublic( cls.getDeclaredConstructor().getModifiers() )) continue;
				} catch (NoSuchMethodException e){
					continue;
				}
				@SuppressWarnings("unchecked") Class<? extends Item> item = (Class<? extends Item>) cls;
				out.add( item );
			}
		}
		out.sort( (a, b) -> a.getName().compareTo( b.getName() ) );
		return out;
	}

	private static String trace( Class<?> cls, Throwable t ){
		StringBuilder sb = new StringBuilder( cls.getName() + ": " + t );
		for (StackTraceElement e : t.getStackTrace()){
			if (!e.getClassName().startsWith( "xyz.gabriwar" ) || e.getClassName().startsWith( AllItemsTest.class.getName() )) continue;
			sb.append( "\n    at " ).append( e );
			if (sb.length() > 900) break;
		}
		return sb.toString();
	}

	@Test
	public void everyItemDescribesItselfOutsideARun() throws IOException {
		HashSet<Class<?>> listed = new HashSet<>();
		for (Catalog c : Catalog.values()) listed.addAll( c.items() );

		List<Class<? extends Item>> items = allItems();
		ArrayList<String> catalogCrashes = new ArrayList<>(), otherCrashes = new ArrayList<>();
		REPORT.mkdirs();
		try (PrintWriter sprites = new PrintWriter( new File( REPORT, "sprites.tsv" ) )){
			sprites.println( "class\timage\tleft\ttop\tright\tbottom\tcatalog" );
			for (Class<? extends Item> cls : items){
				boolean inCatalog = listed.contains( cls );
				Item item;
				try {
					item = cls.getDeclaredConstructor().newInstance();
				} catch (Throwable t){
					(inCatalog ? catalogCrashes : otherCrashes).add( trace( cls, t.getCause() != null ? t.getCause() : t ) );
					continue;
				}
				try {
					item.name();
					//what WndJournal.addGridItems reads for a seen item
					if (item instanceof ClassArmor || item instanceof SpiritBow){
						item.desc();
					} else if (!(item instanceof DriedRose)){
						//DriedRose.desc asks the running Game for its scene, which a test has not got
						item.info();
					}
					item.glowing();
					item.status();
				} catch (Throwable t){
					(inCatalog ? catalogCrashes : otherCrashes).add( trace( cls, t ) );
				}
				int image;
				try {
					image = item.image();
				} catch (Throwable t){
					otherCrashes.add( trace( cls, t ) );
					continue;
				}
				RectF f = ItemSpriteSheet.film.get( image );
				if (f == null){
					sprites.println( cls.getName() + "\t" + image + "\t-1\t-1\t-1\t-1\t" + inCatalog );
				} else {
					sprites.println( cls.getName() + "\t" + image + "\t" + f.left + "\t" + f.top + "\t" + f.right + "\t" + f.bottom + "\t" + inCatalog );
				}
			}
		}
		try (PrintWriter out = new PrintWriter( new File( REPORT, "crashes.txt" ) )){
			out.println( "== catalog items (the journal shows these from the title screen: a crash here is a real crash)" );
			for (String s : catalogCrashes) out.println( s );
			out.println( "\n== other items (only built inside a run; listed for review)" );
			for (String s : otherCrashes) out.println( s );
		}
		for (String s : catalogCrashes) System.out.println( s );
		System.out.println( items.size() + " items, " + catalogCrashes.size() + " catalog crashes, "
				+ otherCrashes.size() + " other crashes (see build/reports/items/crashes.txt)" );
		assertTrue( catalogCrashes.size() + " catalog items crash outside a run (see output)", catalogCrashes.isEmpty() );
	}
}
