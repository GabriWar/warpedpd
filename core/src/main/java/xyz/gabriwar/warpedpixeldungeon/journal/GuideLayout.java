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

import java.util.ArrayList;

//The Descent Guide's geometry, kept apart from the scene so it can be tested headlessly.
//Everything is in whole graph pixels: the guide's camera only ever zooms by whole steps,
//so a card, an icon and a connector all land on one pixel grid.
//
//Two layouts of the same tree:
//  - TREE: a column per level, leaves stacked, every parent centred on its children,
//    joined by right-angled connectors (wide screens);
//  - OUTLINE: an indented list, one card per row, joined by an elbow from the parent's
//    trunk (narrow screens: a phone held upright cannot fit two columns).
public final class GuideLayout {

	private GuideLayout(){}

	public static final int GAP_X  = 18;  //between tree columns
	public static final int GAP_Y  = 5;   //between stacked tree leaves
	public static final int INDENT = 9;   //per outline level
	public static final int ROW_GAP = 4;  //between outline rows
	public static final int TRUNK  = 4;   //an outline trunk's distance from its parent's left edge

	//--- placing the cards ---

	/** Lays out the visible part of the tree (expanded nodes' children) from each node's card
	 *  size (lw, lh), writing lx, ly and level. The root's top-left is (0, 0). */
	public static void layout( GuideGraph.Node root, boolean outline ){
		if (outline){
			int[] y = { 0 };
			outline( root, 0, y );
		} else {
			ArrayList<Integer> colW = new ArrayList<>();
			measureColumns( root, 0, colW );
			int[] colX = new int[colW.size()];
			for (int i = 1; i < colX.length; i++) colX[i] = colX[i-1] + colW.get( i-1 ) + GAP_X;
			int[] y = { 0 };
			tree( root, 0, colX, y );
			int top = root.ly;
			shift( root, -top );
		}
	}

	public static boolean showsChildren( GuideGraph.Node n ){
		return n.expanded && !n.children.isEmpty();
	}

	private static void outline( GuideGraph.Node n, int level, int[] y ){
		n.level = level;
		n.lx = level * INDENT;
		n.ly = y[0];
		y[0] += n.lh + ROW_GAP;
		if (showsChildren( n )){
			for (GuideGraph.Node c : n.children) outline( c, level + 1, y );
		}
	}

	private static void measureColumns( GuideGraph.Node n, int level, ArrayList<Integer> colW ){
		while (colW.size() <= level) colW.add( 0 );
		colW.set( level, Math.max( colW.get( level ), n.lw ) );
		if (showsChildren( n )){
			for (GuideGraph.Node c : n.children) measureColumns( c, level + 1, colW );
		}
	}

	//y[0] is the first free row; returns nothing, the subtree is placed below it
	private static void tree( GuideGraph.Node n, int level, int[] colX, int[] y ){
		n.level = level;
		n.lx = colX[level];
		int start = y[0];
		if (!showsChildren( n )){
			n.ly = start;
			y[0] = start + n.lh + GAP_Y;
			return;
		}
		for (GuideGraph.Node c : n.children) tree( c, level + 1, colX, y );
		GuideGraph.Node first = n.children.get( 0 ), last = n.children.get( n.children.size() - 1 );
		int mid = (first.ly + first.lh / 2 + last.ly + last.lh / 2) / 2;
		n.ly = mid - n.lh / 2;
		//a parent taller than its children's span must not poke into its neighbours
		if (n.ly < start){
			int dy = start - n.ly;
			shift( n, dy );
			y[0] += dy;
		}
		y[0] = Math.max( y[0], n.ly + n.lh + GAP_Y );
	}

	private static void shift( GuideGraph.Node n, int dy ){
		n.ly += dy;
		if (showsChildren( n )){
			for (GuideGraph.Node c : n.children) shift( c, dy );
		}
	}

	//--- connectors ---

	/** The corner points of the connector from a parent's card to a child's, in path order
	 *  (x0, y0, x1, y1, ...): every leg is straight along one axis. */
	public static int[] connector( int px, int py, int pw, int ph, int cx, int cy, int cw, int ch, boolean outline ){
		if (outline){
			int tx = px + TRUNK;
			int ty = cy + ch / 2;
			return new int[]{ tx, py + ph, tx, ty, cx - 1, ty };
		} else {
			int x0 = px + pw;
			int y0 = py + ph / 2;
			int x1 = cx - 1;
			int y1 = cy + ch / 2;
			int mx = x0 + Math.max( 1, (x1 - x0) / 2 );
			return new int[]{ x0, y0, mx, y0, mx, y1, x1, y1 };
		}
	}

	public static int length( int[] path ){
		int len = 0;
		for (int i = 2; i < path.length; i += 2){
			len += Math.abs( path[i] - path[i-2] ) + Math.abs( path[i+1] - path[i-1] );
		}
		return len;
	}

	/** The 1-pixel-thick rectangles {x, y, w, h} covering the first `drawn` pixels of a path:
	 *  one per leg, empty (w or h 0) for a leg not reached yet. */
	public static int[][] legs( int[] path, int drawn ){
		int n = path.length / 2 - 1;
		int[][] out = new int[n][4];
		int left = Math.max( 0, drawn );
		for (int i = 0; i < n; i++){
			int ax = path[2*i], ay = path[2*i+1], bx = path[2*i+2], by = path[2*i+3];
			int len = Math.abs( bx - ax ) + Math.abs( by - ay );
			int take = Math.min( len, left );
			left -= take;
			int[] r = out[i];
			if (take <= 0){
				r[0] = ax; r[1] = ay; r[2] = 0; r[3] = 0;
				continue;
			}
			if (ay == by){
				int ex = ax + Integer.signum( bx - ax ) * take;
				r[0] = Math.min( ax, ex ); r[1] = ay;
				r[2] = Math.abs( ex - ax ) + 1; r[3] = 1;
			} else {
				int ey = ay + Integer.signum( by - ay ) * take;
				r[0] = ax; r[1] = Math.min( ay, ey );
				r[2] = 1; r[3] = Math.abs( ey - ay ) + 1;
			}
		}
		return out;
	}

	/** The pixel at a distance along a path (clamped to its ends), into out {x, y}. */
	public static void pointAt( int[] path, int dist, int[] out ){
		int left = Math.max( 0, dist );
		for (int i = 2; i < path.length; i += 2){
			int ax = path[i-2], ay = path[i-1], bx = path[i], by = path[i+1];
			int len = Math.abs( bx - ax ) + Math.abs( by - ay );
			if (left <= len){
				out[0] = ax + Integer.signum( bx - ax ) * left;
				out[1] = ay + Integer.signum( by - ay ) * left;
				return;
			}
			left -= len;
		}
		out[0] = path[path.length - 2];
		out[1] = path[path.length - 1];
	}

	//--- the camera ---

	/** The whole zoom steps the guide offers: from half the interface's zoom (rounded up) to
	 *  twice it, about a third apart, always including the interface's own. Every step is a
	 *  whole number of screen pixels per graph pixel, and there are few of them: card text is
	 *  rendered anew for each step it is shown at. */
	public static int[] zoomLevels( int defaultZoom ){
		int dz = Math.max( 1, defaultZoom );
		int lo = Math.max( 1, (dz + 1) / 2 );
		int hi = Math.max( lo + 1, dz * 2 );
		ArrayList<Integer> out = new ArrayList<>();
		int z = lo;
		out.add( z );
		while (z < hi){
			int next = Math.min( hi, Math.max( z + 1, Math.round( z * 1.3f ) ) );
			if (z < dz && next > dz) next = dz;
			out.add( next );
			z = next;
		}
		int[] arr = new int[out.size()];
		for (int i = 0; i < arr.length; i++) arr[i] = out.get( i );
		return arr;
	}

	/** The next zoom step in a direction (+1 in, -1 out) from a (possibly fractional) zoom. */
	public static int step( int[] levels, float zoom, int dir ){
		if (dir > 0){
			for (int z : levels) if (z > zoom + 0.01f) return z;
			return levels[levels.length - 1];
		} else {
			for (int i = levels.length - 1; i >= 0; i--) if (levels[i] < zoom - 0.01f) return levels[i];
			return levels[0];
		}
	}

	/** The zoom step nearest a fractional zoom (a pinch let go). */
	public static int nearest( int[] levels, float zoom ){
		int best = levels[0];
		for (int z : levels) if (Math.abs( z - zoom ) < Math.abs( best - zoom )) best = z;
		return best;
	}

	/** The largest step, at most `cap`, at which a box of graph pixels (plus a margin round
	 *  it) fits a screen of device pixels; the smallest step when none does. */
	public static int fit( int[] levels, int boxW, int boxH, int margin, int screenW, int screenH, int cap ){
		int best = levels[0];
		for (int z : levels){
			if (z > cap) break;
			if ((boxW + 2 * margin) * z <= screenW && (boxH + 2 * margin) * z <= screenH) best = z;
		}
		return best;
	}

	/** The zoom steps (+ in, - out) a mouse-wheel scroll of `amount` makes (+ is down). A
	 *  smooth wheel or a trackpad sends many small amounts: they add up in acc[0] to whole
	 *  notches, turning back drops what was left over, and a scroll with nothing up or down in
	 *  it (a sideways swipe) makes none. */
	public static int wheelSteps( float[] acc, float amount ){
		if (amount == 0) return 0;
		if (acc[0] != 0 && Math.signum( acc[0] ) != Math.signum( amount )) acc[0] = 0;
		acc[0] += amount;
		int notches = (int) acc[0];
		acc[0] -= notches;
		return -notches;
	}

	/** Keeps a scroll (the view's left or top edge, graph pixels) such that at least `margin`
	 *  of the content [lo, hi] stays inside a view `view` wide. */
	public static float clamp( float scroll, float view, int lo, int hi, int margin ){
		float min = lo - view + margin;
		float max = hi - margin;
		if (min > max) return (min + max) / 2f;
		return Math.max( min, Math.min( max, scroll ) );
	}

	public static float ease( float t ){
		t = Math.max( 0f, Math.min( 1f, t ) );
		return t * t * (3f - 2f * t);
	}

	/** Ease-out with a small overshoot, for things popping into place. */
	public static float easeOutBack( float t ){
		t = Math.max( 0f, Math.min( 1f, t ) );
		float c = 1.6f;
		float u = t - 1f;
		return 1f + (c + 1f) * u * u * u + c * u * u;
	}
}
