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

package xyz.gabriwar.warpedpixeldungeon.windows;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * The Damage tab's words and its icon legend's are in English and in Portuguese, key for key, with
 * the same format codes in both (a string read with arguments goes through String.format), and no
 * em dash.
 */
public class DamageTabStringsTest {

	private static final String DIR = "src/main/assets/messages/";

	private static Properties load( String file ) throws Exception {
		Properties p = new Properties();
		try (Reader r = new InputStreamReader( new FileInputStream( DIR + file ), StandardCharsets.UTF_8 )){
			p.load( r );
		}
		return p;
	}

	//the format codes of a string, in order: %d, %s, %1$s, %%...
	private static ArrayList<String> codes( String s ){
		ArrayList<String> out = new ArrayList<>();
		Matcher m = Pattern.compile( "%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]" ).matcher( s );
		while (m.find()) out.add( m.group() );
		java.util.Collections.sort( out );
		return out;
	}

	private static void same( String base, String pt, String prefix, String... alsoKeys ) throws Exception {
		Properties en = load( base ), br = load( pt );
		TreeSet<String> keys = new TreeSet<>();
		for (String k : en.stringPropertyNames()) if (k.startsWith( prefix )) keys.add( k );
		for (String k : br.stringPropertyNames()) if (k.startsWith( prefix )) keys.add( k );
		for (String k : alsoKeys) keys.add( k );
		ArrayList<String> bad = new ArrayList<>();
		for (String k : keys){
			String e = en.getProperty( k ), b = br.getProperty( k );
			if (e == null || e.trim().isEmpty()) bad.add( k + ": no English" );
			if (b == null || b.trim().isEmpty()) bad.add( k + ": no Portuguese" );
			if (e == null || b == null) continue;
			if (!codes( e ).equals( codes( b ) )) bad.add( k + ": format codes differ: " + codes( e ) + " / " + codes( b ) );
			if (e.contains( "—" ) || b.contains( "—" )) bad.add( k + ": an em dash" );
		}
		assertTrue( prefix + " " + bad, bad.isEmpty() );
		assertTrue( "no keys under " + prefix, !keys.isEmpty() );
	}

	@Test
	public void theTabsWordsAreInBothLanguages() throws Exception {
		same( "windows/windows.properties", "windows/windows_pt.properties", "windows.wndhero$damagetab.",
				"windows.wndhero.damage" );
	}

	@Test
	public void theIconLegendIsInBothLanguages() throws Exception {
		same( "ui/ui.properties", "ui/ui_pt.properties", "ui.chiplegend." );
	}

	@Test
	public void everyKeyTheTabReadsExists() throws Exception {
		//the keys DamageTab.txt is called with, read from its source
		String src = new String( java.nio.file.Files.readAllBytes( java.nio.file.Paths.get(
				"src/main/java/xyz/gabriwar/warpedpixeldungeon/windows/DamageTab.java" ) ), StandardCharsets.UTF_8 );
		Properties en = load( "windows/windows.properties" ), br = load( "windows/windows_pt.properties" );
		ArrayList<String> missing = new ArrayList<>();
		int seen = 0;
		Pattern literal = Pattern.compile( "\"([a-z_]+)\"" );
		for (String line : src.split( "\n" )){
			if (!line.contains( "txt(" ) || line.contains( "static String txt(" )) continue;
			Matcher m = literal.matcher( line );
			while (m.find()){
				seen++;
				String k = "windows.wndhero$damagetab." + m.group( 1 );
				if (en.getProperty( k ) == null) missing.add( k + " (en)" );
				if (br.getProperty( k ) == null) missing.add( k + " (pt)" );
			}
		}
		assertTrue( "found the calls", seen > 20 );
		assertTrue( "missing: " + missing, missing.isEmpty() );
	}
}
