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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Stack samples folded into a call tree, root first, so a report can show not only which
 * method was running but the whole chain that led there, with line numbers, like a text
 * flame graph. Also keeps self time per exact line.
 */
public class StackTree {

	private static class Node {
		final String label;
		int count = 0;
		final HashMap<String, Node> children = new HashMap<>();
		Node( String label ){ this.label = label; }
	}

	private final Node root = new Node( "" );
	private final HashMap<String, Integer> selfLines = new HashMap<>();
	private int samples = 0;

	/** the frames the render loop shares with every sample; folded away so the tree starts at the game */
	private static boolean boring( StackTraceElement e ){
		String c = e.getClassName();
		return c.startsWith( "com.badlogic.gdx.backends" ) || c.startsWith( "org.lwjgl" )
				|| c.startsWith( "java.lang.Thread" ) && e.getMethodName().equals( "run" );
	}

	public static String frame( StackTraceElement e ){
		String c = e.getClassName();
		String s = c.substring( c.lastIndexOf( '.' ) + 1 ) + "." + e.getMethodName();
		return e.getLineNumber() > 0 ? s + ":" + e.getLineNumber() : s;
	}

	public synchronized void add( StackTraceElement[] st ){
		if (st.length == 0) return;
		samples++;
		String top = frame( st[0] );
		Integer c = selfLines.get( top );
		selfLines.put( top, c == null ? 1 : c + 1 );
		Node n = root;
		n.count++;
		for (int i = st.length - 1; i >= 0; i--){
			if (boring( st[i] )) continue;
			String f = frame( st[i] );
			Node child = n.children.get( f );
			if (child == null) n.children.put( f, child = new Node( f ) );
			child.count++;
			n = child;
		}
	}

	public int samples(){ return samples; }

	/** the tree as indented text: every branch worth at least {@code minShare} of the samples,
	 *  hottest child first, deepest chains folded when a node has a single child */
	public synchronized String text( float minShare, int maxLines ){
		StringBuilder sb = new StringBuilder();
		if (samples == 0) return "  (no samples)\n";
		int[] lines = { 0 };
		for (Node child : sorted( root )) print( sb, child, 0, Math.max( 1, Math.round( minShare * samples ) ), maxLines, lines );
		if (lines[0] >= maxLines) sb.append( "  ...\n" );
		return sb.toString();
	}

	private void print( StringBuilder sb, Node n, int depth, int min, int maxLines, int[] lines ){
		if (n.count < min || lines[0] >= maxLines) return;
		StringBuilder label = new StringBuilder( n.label );
		//fold straight chains: a node whose only child carries all its samples
		while (n.children.size() == 1){
			Node only = n.children.values().iterator().next();
			if (only.count != n.count) break;
			label.append( " > " ).append( only.label );
			n = only;
		}
		for (int i = 0; i < depth; i++) sb.append( "  " );
		sb.append( String.format( Locale.ROOT, "%5.1f%%  %s\n", 100f * n.count / samples, label ) );
		lines[0]++;
		for (Node child : sorted( n )) print( sb, child, depth + 1, min, maxLines, lines );
	}

	private static ArrayList<Node> sorted( Node n ){
		ArrayList<Node> l = new ArrayList<>( n.children.values() );
		Collections.sort( l, ( a, b ) -> b.count - a.count );
		return l;
	}

	/** the exact lines that were executing, hottest first */
	public synchronized String selfText( int top ){
		StringBuilder sb = new StringBuilder();
		ArrayList<Map.Entry<String, Integer>> l = new ArrayList<>( selfLines.entrySet() );
		Collections.sort( l, ( a, b ) -> b.getValue() - a.getValue() );
		for (int i = 0; i < l.size() && i < top; i++){
			sb.append( String.format( Locale.ROOT, "  %5.1f%%  %s\n", 100f * l.get( i ).getValue() / Math.max( 1, samples ), l.get( i ).getKey() ) );
		}
		return sb.toString();
	}
}
