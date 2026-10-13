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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

//How a cite's words are found in a page's text, the same for the page reader and for
//GuideCitesTest. A phrase is reduced to tokens, the same way on both sides:
//  1. lower case (English), a curly apostrophe made straight;
//  2. tokens are runs of [a-z0-9'-], anything else parts them (':' ',' '.' '(' ')' '_' too);
//  3. per token a trailing 's, then a trailing ', goes (and leading 's: a quote opening);
//  4. plurals: "ies" becomes "y" (longer than 4), "ches" "shes" "xes" "sses" "zes" lose "es",
//     else a trailing s goes on a token longer than 3 not ending "ss", "us" or "is";
//  5. a single leading "the", "a" or "an" goes.
//Over-stemming does no harm, as both sides stem alike; irregular plurals (succubi, wolves)
//need a word of their own.
public final class CiteMatch {

	private CiteMatch(){}

	/** The highlighted spans of a text, as written: between pairs of '_' (RenderedTextBlock's toggle). */
	public static ArrayList<String> highlights( String text ){
		ArrayList<String> out = new ArrayList<>();
		if (text == null) return out;
		int open = -1;
		for (int i = 0; i < text.length(); i++){
			if (text.charAt( i ) != '_') continue;
			if (open < 0){
				open = i + 1;
			} else {
				String span = text.substring( open, i ).trim();
				if (!span.isEmpty()) out.add( span );
				open = -1;
			}
		}
		return out;
	}

	/** A phrase reduced for matching (rules above), as its tokens. */
	public static String[] tokens( String phrase ){
		ArrayList<String> out = new ArrayList<>();
		if (phrase == null) return new String[0];
		String s = phrase.toLowerCase( Locale.ENGLISH ).replace( '’', '\'' );
		int i = 0;
		while (i < s.length()){
			if (!part( s.charAt( i ) )){
				i++;
				continue;
			}
			int j = i;
			while (j < s.length() && part( s.charAt( j ) )) j++;
			String t = stem( s.substring( i, j ) );
			if (t != null) out.add( t );
			i = j;
		}
		if (out.size() > 1 && (out.get( 0 ).equals( "the" ) || out.get( 0 ).equals( "a" ) || out.get( 0 ).equals( "an" ))){
			out.remove( 0 );
		}
		return out.toArray( new String[0] );
	}

	/** Whether two phrases are the same after reduction. */
	public static boolean same( String a, String b ){
		String[] ta = tokens( a ), tb = tokens( b );
		return ta.length > 0 && Arrays.equals( ta, tb );
	}

	/** First index where `run` occurs as consecutive tokens in `text`, -1 if none. */
	public static int find( String[] text, String[] run ){
		if (run.length == 0 || run.length > text.length) return -1;
		for (int i = 0; i + run.length <= text.length; i++){
			boolean at = true;
			for (int k = 0; k < run.length && at; k++){
				at = text[i + k].equals( run[k] );
			}
			if (at) return i;
		}
		return -1;
	}

	private static boolean part( char c ){
		return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '\'' || c == '-';
	}

	//one token stemmed, null when nothing of a word is left (a lone dash or quote)
	private static String stem( String t ){
		while (t.startsWith( "'" )) t = t.substring( 1 );
		if (t.endsWith( "'s" )) t = t.substring( 0, t.length() - 2 );
		if (t.endsWith( "'" )) t = t.substring( 0, t.length() - 1 );
		boolean word = false;
		for (int i = 0; i < t.length() && !word; i++){
			char c = t.charAt( i );
			word = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
		}
		if (!word) return null;

		if (t.endsWith( "ies" ) && t.length() > 4){
			return t.substring( 0, t.length() - 3 ) + "y";
		}
		if (t.endsWith( "ches" ) || t.endsWith( "shes" ) || t.endsWith( "xes" ) || t.endsWith( "sses" ) || t.endsWith( "zes" )){
			return t.substring( 0, t.length() - 2 );
		}
		if (t.endsWith( "s" ) && t.length() > 3 && !t.endsWith( "ss" ) && !t.endsWith( "us" ) && !t.endsWith( "is" )){
			return t.substring( 0, t.length() - 1 );
		}
		return t;
	}
}
