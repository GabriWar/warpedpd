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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Rect;

import java.util.ArrayList;
import com.watabou.utils.PointF;

public abstract class DungeonTilemap extends Tilemap {

	public static final int SIZE = 16;

	protected int[] map;

	public DungeonTilemap(String tex) {
		super(tex, new TextureFilm( tex, SIZE, SIZE ) );
	}

	@Override
	//we need to retain two arrays, map is the dungeon tilemap which we can reference.
	// Data is our own internal image representation of the tiles, which may differ.
	public void map(int[] data, int cols) {
		map = data;
		super.map(new int[data.length], cols);
	}

	@Override
	public synchronized void updateMap() {
		for (int i = 0; i < data.length; i++)
			data[i] = getTileVisual(i ,map[i], false);
		super.updateMap();
	}

	/**
	 * A content shift computed ahead of time: the visual array as it will be after the
	 * window moved by (dcx, dcy), and the rectangles of cells whose visuals changed. Built on
	 * the actor thread without touching the array the render thread is drawing, then swapped
	 * in by {@link #applyShift} inside the render thread's atomic block.
	 */
	public static class ShiftPlan {
		public final int[] data;
		public final ArrayList<Rect> dirty = new ArrayList<>();
		public final int dcx, dcy;

		ShiftPlan( int[] data, int dcx, int dcy ){
			this.data = data;
			this.dcx = dcx;
			this.dcy = dcy;
		}
	}

	/**
	 * Sliding-window rebase: the window shifted by (dcx, dcy) cells, so for the overlap
	 * every cell shows the SAME world content it already computed - the cached visual ids
	 * move with arraycopy and only the newly exposed strips, a two-cell stitching fringe and
	 * the trailing border ring run getTileVisual. Reads the level as it is now (the new
	 * window) and this map's current visuals; writes only into the plan.
	 */
	public ShiftPlan prepareShift( int dcx, int dcy ){
		int w = mapWidth, h = mapHeight;
		int[] nd = new int[size];
		System.arraycopy( data, 0, nd, 0, size );
		shiftCells( data, nd, w, h, dcx, dcy );
		ShiftPlan plan = new ShiftPlan( nd, dcx, dcy );

		//the exposed strips: everything there is new
		if (dcx > 0)      recompute( plan, w - dcx, 0, w, h, false );
		else if (dcx < 0) recompute( plan, 0, 0, -dcx, h, false );
		if (dcy > 0)      recompute( plan, 0, h - dcy, w, h, false );
		else if (dcy < 0) recompute( plan, 0, 0, w, -dcy, false );

		//a two-cell fringe inside the overlap: stitched visuals there were computed against
		//what used to be the solid border
		final int f = 2;
		if (dcx > 0)      recompute( plan, w - dcx - f, 0, w - dcx, h, true );
		else if (dcx < 0) recompute( plan, -dcx, 0, -dcx + f, h, true );
		if (dcy > 0)      recompute( plan, 0, h - dcy - f, w, h - dcy, true );
		else if (dcy < 0) recompute( plan, 0, -dcy, w, -dcy + f, true );

		//the trailing border ring: interior content slid onto it, and the ring is always
		//window-authored (solid)
		if (dcx > 0)      recompute( plan, 0, 0, 1, h, true );
		else if (dcx < 0) recompute( plan, w - 1, 0, w, h, true );
		if (dcy > 0)      recompute( plan, 0, 0, w, 1, true );
		else if (dcy < 0) recompute( plan, 0, h - 1, w, h, true );

		return plan;
	}

	private void recompute( ShiftPlan plan, int l, int t, int r, int b, boolean onlyChanged ){
		l = Math.max( 0, l ); t = Math.max( 0, t );
		r = Math.min( mapWidth, r ); b = Math.min( mapHeight, b );
		if (l >= r || t >= b) return;
		Rect changed = new Rect();
		for (int y = t; y < b; y++){
			for (int x = l; x < r; x++){
				int c = x + y * mapWidth;
				int v = getTileVisual( c, map[c], false );
				if (plan.data[c] != v){
					plan.data[c] = v;
					if (onlyChanged) changed.union( x, y );
				}
			}
		}
		if (!onlyChanged) plan.dirty.add( new Rect( l, t, r, b ) );
		else if (!changed.isEmpty()) plan.dirty.add( changed );
	}

	/** swaps a prepared shift in: a reference swap and a chunk relabel, nothing recomputed */
	public synchronized void applyShift( ShiftPlan plan ){
		data = plan.data;
		if (!relabelChunks( plan.dcx, plan.dcy )){
			//not chunked: one full vertex rebuild, the visuals are already right
			super.updateMap();
			return;
		}
		for (Rect r : plan.dirty) updateMapRect( r );
	}

	/** prepare and apply on the calling thread, for callers that already run where they draw */
	public void shiftAndUpdate( int dcx, int dcy ){
		applyShift( prepareShift( dcx, dcy ) );
	}

	@Override
	public synchronized void updateMapCell(int cell) {
		//update in a 3x3 grid to account for neighbours which might also be affected
		if (Dungeon.level.insideMap(cell)) {
			for (int i : PathFinder.NEIGHBOURS9) {
				data[cell + i] = getTileVisual(cell + i, map[cell + i], false);
			}
			super.updateMapCell(cell - mapWidth - 1);
			super.updateMapCell(cell + mapWidth + 1);

		//unless we're at the level's edge, then just do the one tile.
		} else {
			data[cell] = getTileVisual(cell, map[cell], false);
			super.updateMapCell(cell);
		}
	}

	protected abstract int getTileVisual(int pos, int tile, boolean flat);

	public int screenToTile(int x, int y ){
		return screenToTile(x, y, false);
	}

	public int screenToTile(int x, int y, boolean wallAssist ) {
		PointF p = camera().screenToCamera( x, y ).
			offset( this.point().negate() ).
			invScale( SIZE );
		
		//snap to the edges of the tilemap
		p.x = GameMath.gate(0, p.x, Dungeon.level.width()-0.001f);
		p.y = GameMath.gate(0, p.y, Dungeon.level.height()-0.001f);

		int cell = (int)p.x + (int)p.y * Dungeon.level.width();

		//wall assist is used to make raised perspective tapping a bit easier.
		// If the pressed tile is a wall tile, the tap can be 'bumped' down into a none-wall tile.
		// currently this happens if the bottom 1/4 of the wall tile is pressed.
		if (wallAssist
				&& map != null
				&& isWallAssistable(cell)){

			if (cell + mapWidth < size
					&& p.y % 1 >= 0.75f
					&& !isWallAssistable(cell + mapWidth)){
				cell += mapWidth;
			}

		}

		return cell;
	}

	private boolean isWallAssistable(int cell){
		if (map == null || cell >= size){
			return false;
		}

		if (DungeonTileSheet.wallStitcheable(map[cell])){
			return true;
		}

		//caves region deco is very wall-like, so it counts
		if (Dungeon.depth >= 10 && Dungeon.depth <= 15
				&& (map[cell] == Terrain.REGION_DECO || map[cell] == Terrain.REGION_DECO_ALT)) {
			return true;
		}

		return false;
	}
	
	@Override
	public boolean overlapsPoint( float x, float y ) {
		return true;
	}
	
	public void discover( int pos, int oldValue ) {
		
		int visual = getTileVisual( pos, oldValue, false);
		if (visual < 0) return;
		
		final Image tile = new Image( texture );
		tile.frame( tileset.get( getTileVisual( pos, oldValue, false)));
		tile.point( tileToWorld( pos ) );

		parent.add( tile );
		
		parent.add( new AlphaTweener( tile, 0, 0.6f ) {
			protected void onComplete() {
				tile.killAndErase();
				killAndErase();
			}
		} );
	}
	
	public static PointF tileToWorld( int pos ) {
		return new PointF( pos % Dungeon.level.width(), pos / Dungeon.level.width()  ).scale( SIZE );
	}
	
	public static PointF tileCenterToWorld( int pos ) {
		return new PointF(
			(pos % Dungeon.level.width() + 0.5f) * SIZE,
			(pos / Dungeon.level.width() + 0.5f) * SIZE );
	}

	public static PointF raisedTileCenterToWorld( int pos ) {
		return new PointF(
				(pos % Dungeon.level.width() + 0.5f) * SIZE,
				(pos / Dungeon.level.width() + 0.1f) * SIZE );
	}

	public static int worldToTile( float x, float y, int width){
		return (int)(x / SIZE) + ((int)(y / SIZE) * width);
	}
	
	@Override
	public boolean overlapsScreenPoint( int x, int y ) {
		return true;
	}

}
