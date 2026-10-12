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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

/**
 * What the taps on the map mean while the build bar is up (BuildPlacer): the mode, and the cells
 * picked so far. Pure, so the tests walk it tap by tap.
 *
 * - SINGLE: every tap builds one piece there.
 * - LINE and AREA: the first tap sets the start (the first corner), the second the end (the other
 *   corner) and shows the whole shape; a tap on any cell of that shape builds it (anywhere in an
 *   area's rectangle, the hollow inside of a wall's shell too), a tap anywhere else moves the end
 *   there. A rectangle too big to build is never built: a tap in it moves the corner.
 * - DEMOLISH and REPAIR: the first tap on a cell picks it and shows what it gives back or costs,
 *   a second tap on the same cell does it; a tap elsewhere picks that cell instead.
 * - INSPECT: every tap looks at the cell.
 */
public final class BuildSelection {

	public enum Mode {
		SINGLE, LINE, AREA, DEMOLISH, REPAIR, INSPECT;

		/** Laid along a shape between two taps. */
		public boolean shaped(){
			return this == LINE || this == AREA;
		}

		/** Picks a cell first and acts on a second tap on it. */
		public boolean targeted(){
			return this == DEMOLISH || this == REPAIR;
		}

		/** Puts a piece up (needs one chosen). */
		public boolean places(){
			return this == SINGLE || shaped();
		}
	}

	/** What a tap asks for. */
	public enum Step {
		/** build one piece on the cell (SINGLE) */
		PLACE,
		/** the shape's start is set: show it */
		ANCHOR,
		/** the shape's end is set (or moved): show the whole shape */
		PREVIEW,
		/** build the shape shown, or take down / mend the cell picked */
		CONFIRM,
		/** a cell is picked to take down or mend: show what that gives back or costs */
		TARGET,
		/** look at the cell */
		INSPECT
	}

	private Mode mode = Mode.SINGLE;
	private int anchor = -1, end = -1, target = -1;
	//an area of the piece is only the rim of its rectangle (BuildShapes.areaIsOutline)
	private boolean outline;

	public Mode mode(){
		return mode;
	}

	/** Switches mode, forgetting the cells picked. */
	public void mode( Mode mode ){
		this.mode = mode;
		clear();
	}

	/** Whether an AREA lays only its rim (a wall's hollow shell). */
	public void outline( boolean outline ){
		this.outline = outline;
	}

	public boolean outline(){
		return outline;
	}

	/** Forgets the cells picked: the start, the end and the target. */
	public void clear(){
		anchor = end = target = -1;
	}

	public int anchor(){
		return anchor;
	}

	public int end(){
		return end;
	}

	public int target(){
		return target;
	}

	/** Is a whole shape shown, waiting for the tap that builds it. */
	public boolean previewing(){
		return mode.shaped() && anchor != -1 && end != -1;
	}

	/** What a tap on a cell of a map `w` wide and `h` high means, the picks moved on with it. */
	public Step tap( int cell, int w, int h ){
		switch (mode){
			case SINGLE:
				return Step.PLACE;
			case INSPECT:
				return Step.INSPECT;
			case DEMOLISH:
			case REPAIR:
				if (cell == target) return Step.CONFIRM;
				target = cell;
				return Step.TARGET;
			default:
				if (anchor == -1){
					anchor = cell;
					return Step.ANCHOR;
				}
				if (end != -1 && !tooBig( w ) && onShape( cell, w, h )) return Step.CONFIRM;
				end = cell;
				return Step.PREVIEW;
		}
	}

	/** The shape from the start to the end (to `to` while no end is set: the shape under the
	 *  pointer before the second tap). Empty with no start. */
	public int[] cells( int w, int h ){
		return cellsTo( end != -1 ? end : anchor, w, h );
	}

	/** The shape from the start to a cell; empty for a rectangle too big to build (tooBigTo),
	 *  which the bar frames whole instead. */
	public int[] cellsTo( int to, int w, int h ){
		if (anchor == -1 || !mode.shaped()) return new int[0];
		if (to == -1) to = anchor;
		if (mode == Mode.LINE) return BuildShapes.line( w, h, anchor, to );
		if (count( to, w ) > BuildShapes.MAX_CELLS) return new int[0];
		return outline ? BuildShapes.outline( w, anchor, to ) : BuildShapes.rect( w, anchor, to );
	}

	/** Is the rectangle shown more than one go may build (a line is cut instead). */
	public boolean tooBig( int w ){
		return tooBigTo( end != -1 ? end : anchor, w );
	}

	/** As tooBig, for the shape to a cell. */
	public boolean tooBigTo( int to, int w ){
		return mode == Mode.AREA && anchor != -1 && to != -1 && count( to, w ) > BuildShapes.MAX_CELLS;
	}

	/** How many cells the rectangle to a cell would build. */
	public int count( int to, int w ){
		return outline ? BuildShapes.outlineCount( w, anchor, to ) : BuildShapes.rectCount( w, anchor, to );
	}

	//is a cell on the shape shown: one of a line's cells, anywhere in an area's rectangle
	private boolean onShape( int cell, int w, int h ){
		if (mode == Mode.AREA){
			int x = cell % w, y = cell / w;
			int ax = anchor % w, ay = anchor / w, ex = end % w, ey = end / w;
			return x >= Math.min( ax, ex ) && x <= Math.max( ax, ex ) && y >= Math.min( ay, ey ) && y <= Math.max( ay, ey );
		}
		return contains( cells( w, h ), cell );
	}

	private static boolean contains( int[] cells, int cell ){
		for (int c : cells) if (c == cell) return true;
		return false;
	}
}
