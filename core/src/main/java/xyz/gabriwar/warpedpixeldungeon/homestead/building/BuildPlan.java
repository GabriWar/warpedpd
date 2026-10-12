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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A piece laid over a shape's cells, worked out before it is built: which cells take it (and why
 * the others don't), how many of those the hero's materials stretch to, and what it all costs.
 * The build bar shows it as the shape's ghosts and its summary ("can afford 5 of 7"), and builds
 * exactly the cells it calls buildable, in order.
 *
 * The cells are asked through a Site (Homestead.canPlaceSite in play: everything but the cost) and
 * the materials through a Stock (Material.count), so a test hands in its own.
 *
 * A roof holds itself up as it goes: a cell that only stands on a roof planned beside it is taken
 * once that one is (Chain), and the cells are built in the order they were taken, from the walls
 * inward, so each finds its neighbour already roofed when its turn comes.
 */
public final class BuildPlan {

	/** Whether one cell would take the piece, leaving the cost aside. */
	public interface Site {
		Homestead.Reason at( int cell );
	}

	/** As Site, the cells already taken into the plan (to be built before this one) counting as
	 *  built: what a roof needs, which rests on the roof beside it. */
	public interface Chain {
		Homestead.Reason at( int cell, Set<Integer> planned );
	}

	/** How many of a material the builder has. */
	public interface Stock {
		int have( Class<? extends Item> material );
	}

	/** Every cell of the shape, in building order: the shape's own, except that the cells taking
	 *  the piece fill their places in the order they were taken (Chain: a roof from its walls in). */
	public final int[] cells;
	/** Why each cell would (OK) or would not take the piece. */
	public final Homestead.Reason[] reasons;
	/** How many cells take it. */
	public final int valid;
	/** How many of those the materials are enough for: the first ones, in order. */
	public final int affordable;

	private BuildPlan( int[] cells, Homestead.Reason[] reasons, int valid, int affordable ){
		this.cells = cells;
		this.reasons = reasons;
		this.valid = valid;
		this.affordable = affordable;
	}

	/** The plan of a piece over cells: each one's verdict, and how many the stock pays for. */
	public static BuildPlan of( BuildPiece piece, int[] cells, Site site, Stock stock ){
		return plan( piece, cells, (c, planned) -> site.at( c ), stock, false );
	}

	/**
	 * The plan of a piece over cells whose verdicts may rest on each other: the shape is gone
	 * through in its order, again and again, each cell that takes the piece (the ones taken before
	 * it counted as built) joining the plan, until a pass takes none. The cells taken fill their
	 * places in the shape in the order they were taken; the others keep their places and their
	 * last verdict. With no cell resting on another this is one pass and the shape's own order.
	 */
	public static BuildPlan of( BuildPiece piece, int[] cells, Chain site, Stock stock ){
		return plan( piece, cells, site, stock, true );
	}

	//chains: go through the shape again while a pass takes any cell (one pass when no cell rests
	//on another, the plain Site's)
	private static BuildPlan plan( BuildPiece piece, int[] cells, Chain site, Stock stock, boolean chains ){
		int n = cells.length;
		int[] order = cells.clone();
		Homestead.Reason[] reasons = new Homestead.Reason[n];
		int[] takenInOrder = new int[n];
		Set<Integer> planned = new HashSet<>();
		int valid = 0;
		boolean more = true;
		while (more){
			more = false;
			for (int i = 0; i < n; i++){
				if (reasons[i] != null && reasons[i].ok()) continue;
				reasons[i] = site.at( cells[i], planned );
				if (reasons[i].ok()){
					planned.add( cells[i] );
					takenInOrder[valid++] = cells[i];
					more = chains;
				}
			}
		}
		//the places of the cells taken, in the shape's order, get them in the order they were taken
		for (int i = 0, j = 0; i < n; i++){
			if (reasons[i].ok()) order[i] = takenInOrder[j++];
		}
		return new BuildPlan( order, reasons, valid, Math.min( valid, affordableCount( piece, stock ) ) );
	}

	/** The plan for the hero on the level shown: the cells as Homestead.canPlaceSite has them, a
	 *  roof resting on the roof planned beside it. */
	public static BuildPlan of( Hero hero, BuildPiece piece, int[] cells ){
		//only a roof rests on what is planned beside it
		return plan( piece, cells, (c, planned) -> Homestead.canPlaceSite( hero, c, piece, planned ),
				m -> Material.count( hero, m ), piece.kind == BuildPiece.Kind.ROOF );
	}

	/** Is the cell at this place in the order one that gets built: it takes the piece, and it is
	 *  among the first `affordable` that do. */
	public boolean buildable( int index ){
		if (!reasons[index].ok()) return false;
		int before = 0;
		for (int i = 0; i < index; i++) if (reasons[i].ok()) before++;
		return before < affordable;
	}

	/** The cells that get built, in order. */
	public int[] buildableCells(){
		int[] out = new int[affordable];
		int n = 0;
		for (int i = 0; i < cells.length && n < affordable; i++){
			if (reasons[i].ok()) out[n++] = cells[i];
		}
		return out;
	}

	/** How many cells won't be built because they don't take the piece. */
	public int skipped(){
		return cells.length - valid;
	}

	/** How many pieces a stock pays for: the fewest any of its materials stretches to, each
	 *  material's quantities summed first. Integer.MAX_VALUE for a piece that costs nothing. */
	public static int affordableCount( BuildPiece piece, Stock stock ){
		int n = Integer.MAX_VALUE;
		for (BuildPiece.Cost c : aggregate( piece.cost, 1 )){
			if (c.quantity <= 0) continue;
			n = Math.min( n, stock.have( c.material ) / c.quantity );
		}
		return n;
	}

	/** A cost `times` over, each material once with its quantities summed, in the order the
	 *  materials first come in the cost. What is summed is what is paid (Cost.amount: gold at the
	 *  town's rate), so the quantity of each cost it returns is what is paid, gold included. */
	public static List<BuildPiece.Cost> aggregate( List<BuildPiece.Cost> cost, int times ){
		LinkedHashMap<Class<? extends Item>, Integer> sum = new LinkedHashMap<>();
		for (BuildPiece.Cost c : cost){
			Integer had = sum.get( c.material );
			sum.put( c.material, (had == null ? 0 : had) + c.amount() * times );
		}
		ArrayList<BuildPiece.Cost> out = new ArrayList<>();
		for (Map.Entry<Class<? extends Item>, Integer> e : sum.entrySet()) out.add( new BuildPiece.Cost( e.getKey(), e.getValue() ) );
		return out;
	}

	/** What is short for building a piece `times` over: each material and how many more are
	 *  needed. Empty when the stock is enough. */
	public static List<BuildPiece.Cost> missing( BuildPiece piece, int times, Stock stock ){
		ArrayList<BuildPiece.Cost> out = new ArrayList<>();
		for (BuildPiece.Cost c : aggregate( piece.cost, times )){
			int short_ = c.quantity - stock.have( c.material );
			if (short_ > 0) out.add( new BuildPiece.Cost( c.material, short_ ) );
		}
		return out;
	}

	/**
	 * The cells within Homestead.RANGE of the hero where the bar's mode would act now: where the
	 * piece goes up as Homestead.canPlace has it (SINGLE, LINE, AREA, materials and all), where
	 * something can come down (DEMOLISH, Homestead.canRemove) or be mended (REPAIR,
	 * Homestead.canRepair). None for INSPECT, or for a placing mode with no piece.
	 */
	public static int[] validAround( Hero hero, Level level, BuildSelection.Mode mode, BuildPiece piece ){
		if (mode == BuildSelection.Mode.INSPECT || (mode.places() && piece == null)) return new int[0];
		int w = level.width(), hx = hero.pos % w, hy = hero.pos / w, r = Homestead.RANGE;
		int[] out = new int[(2 * r + 1) * (2 * r + 1)];
		int n = 0;
		for (int y = Math.max( 0, hy - r ); y <= Math.min( level.height() - 1, hy + r ); y++){
			for (int x = Math.max( 0, hx - r ); x <= Math.min( w - 1, hx + r ); x++){
				int c = x + y * w;
				Homestead.Reason why;
				switch (mode){
					case DEMOLISH: why = Homestead.canRemove( hero, c ); break;
					case REPAIR:   why = Homestead.canRepair( hero, c ); break;
					default:       why = Homestead.canPlace( hero, c, piece ); break;
				}
				if (why.ok()) out[n++] = c;
			}
		}
		return java.util.Arrays.copyOf( out, n );
	}
}
