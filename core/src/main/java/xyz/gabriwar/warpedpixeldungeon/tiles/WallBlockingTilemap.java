/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.HallsBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;


public class WallBlockingTilemap extends Tilemap {

	public static final int SIZE = 16;

	private static final int CLEARED        = -2;
	private static final int BLOCK_NONE     = -1;
	private static final int BLOCK_RIGHT    = 0;
	private static final int BLOCK_LEFT     = 1;
	private static final int BLOCK_ALL      = 2;
	private static final int BLOCK_BELOW    = 3;

	public WallBlockingTilemap() {
		super(Assets.Environment.WALL_BLOCKING, new TextureFilm( Assets.Environment.WALL_BLOCKING, SIZE, SIZE ) );
		map( new int[Dungeon.level.length()], Dungeon.level.width());
	}

	@Override
	public synchronized void updateMap() {
		for (int cell = 0; cell < data.length; cell++) {
			//force all top/bottom row, and none-discoverable cells to cleared
			if (!Dungeon.level.discoverable[cell]
					|| (cell - mapWidth) <= 0
					|| (cell + mapWidth) >= size){
				data[cell] = CLEARED;
			} else {
				updateMapCell(cell);
			}
		}

		super.updateMap();
	}

	private int curr;

	@Override
	public synchronized void updateMapCell(int cell) {
		int c = compute( cell );
		if (data[cell] != c){
			data[cell] = c;
			super.updateMapCell(cell);
		}
	}

	//what updateMap() would put in a cell: the row and discoverability guard, then the blocker
	private int computeForMap( int cell ){
		if (!Dungeon.level.discoverable[cell]
				|| (cell - mapWidth) <= 0
				|| (cell + mapWidth) >= size){
			return CLEARED;
		}
		return compute( cell );
	}

	//the blocker for a cell, from the level's map and exploration memory; no state touched
	private int compute( int cell ){

		//FIXME this is to address the wall blocking looking odd on the new yog floor.
		// The true solution is to improve the fog of war so the blockers aren't necessary.
		if (Dungeon.level instanceof HallsBossLevel
				//always-lit hand-painted levels (the town buildings): the wall
				//blackout would paint their art over with black squares
				|| Dungeon.level.noFogOfWar()
				//the top of the overworld's rock is ground seen from below, fogged as such
				|| Dungeon.level.rockTopAt(cell)){
			return CLEARED;
		}

		//non-wall tiles
		if (!wall(cell)) {

			//clear empty floor tiles and cells which are visible, and the ground north of a
			//rock top, which casts no lip to hide
			if (!fogHidden(cell) || !wall(cell + mapWidth) || Dungeon.level.rockTopAt(cell + mapWidth)) {
				curr = CLEARED;

			//block wall overhang if:
			//- There are cells 2x below
			//- The cell below is a wall and visible
			//- All of left, below-left, right, below-right is either a wall or hidden
			} else if ( !fogHidden(cell + mapWidth)
					&& (fogHidden(cell - 1) || wall(cell - 1))
					&& (fogHidden(cell + 1) || wall(cell + 1))
					&& (fogHidden(cell - 1 + mapWidth) || wall(cell - 1 + mapWidth))
					&& (fogHidden(cell + 1 + mapWidth) || wall(cell + 1 + mapWidth))) {
				curr = BLOCK_BELOW;

			} else {
				curr = BLOCK_NONE;
			}

		//wall tiles
		} else {

			//camera-facing wall tiles
			if (!wall(cell + mapWidth)) {

				//Block a camera-facing wall if:
				//- the cell above, above-left, or above-right is not a wall, visible, and has a wall below
				//- none of the remaining 5 neighbour cells are both not a wall and visible
				
				//if all 3 above are wall we can shortcut and just clear the cell
				//unless one or more is a shelf, or we can mine, then we have to just block none
				if (wall(cell - 1 - mapWidth) && wall(cell - mapWidth) && wall(cell + 1 - mapWidth)){
					if (shelf(cell - 1 - mapWidth) || shelf(cell - mapWidth)
							|| shelf(cell + 1 - mapWidth) || Dungeon.level instanceof MiningLevel){
						curr = BLOCK_NONE;
					} else {
						curr = CLEARED;
					}
					
				} else if ((!wall(cell - 1 - mapWidth) && !fogHidden(cell - 1 - mapWidth) && wall(cell - 1)) ||
						(!wall(cell - mapWidth) && !fogHidden(cell - mapWidth)) ||
						(!wall(cell + 1 - mapWidth) && !fogHidden(cell + 1 - mapWidth) && wall(cell+1))){
					
					if ( !fogHidden( cell + mapWidth) ||
							(!wall(cell - 1) && !fogHidden(cell - 1)) ||
							(!wall(cell - 1 + mapWidth) && !fogHidden(cell - 1 + mapWidth)) ||
							(!wall(cell + 1) && !fogHidden(cell + 1)) ||
							(!wall(cell + 1 + mapWidth) && !fogHidden(cell + 1 + mapWidth))){
						curr = CLEARED;
					} else {
						curr = BLOCK_ALL;
					}
					
				} else {
					curr = BLOCK_NONE;
				}

			//internal wall tiles
			} else {
				
				//Block the side of an internal wall if:
				//- any cells above, the one directly below, or the cell itself is visible
				//and all of the following are NOT true:
				//- the cell has no neighbours on that side
				//- the top-side neighbour is visible and the side neighbour isn't a wall.
				//- the side neighbour is both not a wall and visible
				//- the bottom-side neighbour is both not a wall and visible

				curr = BLOCK_NONE;
				
				if (!fogHidden(cell - mapWidth)
						|| !fogHidden(cell - mapWidth - 1)
						|| !fogHidden(cell - mapWidth + 1)
						|| !fogHidden(cell)
						|| !fogHidden(cell + mapWidth)) {
					
					//right side
					if ( ((cell + 1) % mapWidth == 0) ||
							(!wall(cell + 1) && !fogHidden(cell + 1 - mapWidth)) ||
							(!wall(cell + 1) && !fogHidden(cell + 1)) ||
							(!wall(cell + 1 + mapWidth) && !fogHidden(cell + 1 + mapWidth))
							){
						//do nothing
					} else {
						curr += 1;
					}
					
					//left side
					if ( (cell  % mapWidth == 0) ||
							(!wall(cell - 1) && !fogHidden(cell - 1 - mapWidth)) ||
							(!wall(cell - 1) && !fogHidden(cell - 1)) ||
							(!wall(cell - 1 + mapWidth) && !fogHidden(cell - 1 + mapWidth))
							){
						//do nothing
					} else {
						curr += 2;
					}
					
					if (curr == BLOCK_NONE) {
						curr = CLEARED;
					}
				}

			}

		}

		return curr;
	}

	/**
	 * Sliding-window rebase: the blockers of the overlap move with arraycopy; the exposed
	 * strips, a two-cell fringe and the trailing border ring are recomputed against the
	 * translated exploration memory. Built off the render thread, swapped in by applyShift.
	 */
	public DungeonTilemap.ShiftPlan prepareShift( int dcx, int dcy ){
		int w = mapWidth, h = mapHeight;
		int[] nd = new int[size];
		System.arraycopy( data, 0, nd, 0, size );
		shiftCells( data, nd, w, h, dcx, dcy );
		DungeonTilemap.ShiftPlan plan = new DungeonTilemap.ShiftPlan( nd, dcx, dcy );
		if (dcx > 0)      recompute( plan, w - dcx, 0, w, h, false );
		else if (dcx < 0) recompute( plan, 0, 0, -dcx, h, false );
		if (dcy > 0)      recompute( plan, 0, h - dcy, w, h, false );
		else if (dcy < 0) recompute( plan, 0, 0, w, -dcy, false );
		final int f = 2;
		if (dcx > 0)      recompute( plan, w - dcx - f, 0, w - dcx, h, true );
		else if (dcx < 0) recompute( plan, -dcx, 0, -dcx + f, h, true );
		if (dcy > 0)      recompute( plan, 0, h - dcy - f, w, h - dcy, true );
		else if (dcy < 0) recompute( plan, 0, -dcy, w, -dcy + f, true );
		if (dcx > 0)      recompute( plan, 0, 0, 1, h, true );
		else if (dcx < 0) recompute( plan, w - 1, 0, w, h, true );
		if (dcy > 0)      recompute( plan, 0, 0, w, 1, true );
		else if (dcy < 0) recompute( plan, 0, h - 1, w, h, true );
		return plan;
	}

	private void recompute( DungeonTilemap.ShiftPlan plan, int l, int t, int r, int b, boolean onlyChanged ){
		l = Math.max( 0, l ); t = Math.max( 0, t );
		r = Math.min( mapWidth, r ); b = Math.min( mapHeight, b );
		if (l >= r || t >= b) return;
		com.watabou.utils.Rect changed = new com.watabou.utils.Rect();
		for (int y = t; y < b; y++){
			for (int x = l; x < r; x++){
				int c = x + y * mapWidth;
				int v = computeForMap( c );
				if (plan.data[c] != v){
					plan.data[c] = v;
					if (onlyChanged) changed.union( x, y );
				}
			}
		}
		if (!onlyChanged) plan.dirty.add( new com.watabou.utils.Rect( l, t, r, b ) );
		else if (!changed.isEmpty()) plan.dirty.add( changed );
	}

	public synchronized void applyShift( DungeonTilemap.ShiftPlan plan ){
		data = plan.data;
		if (!relabelChunks( plan.dcx, plan.dcy )){
			super.updateMap();
			return;
		}
		for (com.watabou.utils.Rect r : plan.dirty) updateMapRect( r );
	}

	private boolean fogHidden(int cell){
		if (!Dungeon.level.visited[cell] && !Dungeon.level.mapped[cell]) {
			return true;
		} else if (wall(cell) && cell + mapWidth < size && !wall(cell + mapWidth) &&
				!Dungeon.level.visited[cell + mapWidth] && !Dungeon.level.mapped[cell + mapWidth]) {
			return true;
		}
		return false;
	}

	private boolean wall(int cell) {
		return DungeonTileSheet.wallStitcheable(Dungeon.level.map[cell]);
	}

	private boolean shelf(int cell) {
		return Dungeon.level.map[cell] == Terrain.BOOKSHELF;
	}

	private boolean door(int cell) {
		return DungeonTileSheet.doorTile(Dungeon.level.map[cell]);
	}
	
	public synchronized void updateArea(int cell, int radius){
		int l = cell%mapWidth - radius;
		int t = cell/mapWidth - radius;
		int r = cell%mapWidth - radius + 1 + 2*radius;
		int b = cell/mapWidth - radius + 1 + 2*radius;
		updateArea(
				Math.max(0, l),
				Math.max(0, t),
				Math.min(mapWidth-1, r - l),
				Math.min(mapHeight-1, b - t)
		);
	}

	public synchronized void updateArea(int x, int y, int w, int h) {
		int cell;
		for (int i = x; i <= x+w; i++){
			for (int j = y; j <= y+h; j++){
				cell = i + j*mapWidth;
				if (cell < data.length && data[cell] != CLEARED)
					updateMapCell(cell);
			}
		}
	}
	
}
