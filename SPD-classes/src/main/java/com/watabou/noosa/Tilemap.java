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

package com.watabou.noosa;

import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.utils.Rect;
import com.watabou.utils.RectF;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A grid of same-sized tiles drawn from one texture.
 *
 * The map is laid out as 16x16-cell chunks, each with its own vertex buffer. Only the chunks
 * the camera can see are drawn, and a chunk that changed is rebuilt only when it is about to
 * be drawn, so work that lands off screen costs nothing until the player looks at it. A
 * chunk-aligned content shift (a streaming level sliding its window) re-labels the chunk
 * objects instead of rebuilding them: the overlap keeps its buffers untouched.
 *
 * The single-buffer path is kept behind {@link #chunked} so the two can be compared in play;
 * the flag is read when a map is laid out.
 */
public class Tilemap extends Visual {

	/** chunked rendering on new tilemaps; off restores the single-buffer path */
	public static boolean chunked = true;

	public static final int CHUNK = 16;
	//how far past the camera edge a chunk still counts as visible: the camera shake never
	//exceeds ten units, and a tile is at most sixteen
	private static final float CULL_PAD = 32f;

	protected SmartTexture texture;
	protected TextureFilm tileset;

	protected int[] data;
	protected int mapWidth;
	protected int mapHeight;
	protected int size;

	private float cellW;
	private float cellH;

	protected float[] vertices;
	protected FloatBuffer quads;
	protected Vertexbuffer buffer;

	private volatile Rect updated;
	private boolean fullUpdate;
	private Rect updating;
	private int topLeftUpdating;
	private int bottomRightUpdating;

	// ---------------------------------------------------------------- chunks

	private static class Chunk {
		FloatBuffer quads;
		Vertexbuffer buffer;
		boolean dirty = true;
		int count = 0;   //renderable quads after the last rebuild
	}

	private boolean useChunks;
	private Chunk[] chunks;
	private int chunkCols, chunkRows;
	//the model matrix of the chunk being drawn: the map's, translated to the chunk's slot
	private final float[] chunkMatrix = new float[16];

	public Tilemap( Object tx, TextureFilm tileset ) {

		super( 0, 0, 0, 0 );

		this.texture = TextureCache.get( tx );
		this.tileset = tileset;

		RectF r = tileset.get( 0 );
		cellW = tileset.width( r );
		cellH = tileset.height( r );

		vertices = new float[16];

		updated = new Rect();
	}

	public synchronized void map( int[] data, int cols ) {

		int rows = data.length / cols;
		boolean sameShape = this.data != null && cols == mapWidth && rows == mapHeight;

		this.data = data;

		mapWidth = cols;
		mapHeight = rows;
		size = mapWidth * mapHeight;

		width = cellW * mapWidth;
		height = cellH * mapHeight;

		if (sameShape && useChunks && chunks != null){
			//the same grid again: keep the chunk objects, everything is simply dirty
			updateMap();
			return;
		}

		useChunks = chunked;
		if (useChunks){
			releaseChunks();
			chunkCols = (mapWidth + CHUNK - 1) / CHUNK;
			chunkRows = (mapHeight + CHUNK - 1) / CHUNK;
			chunks = new Chunk[chunkCols * chunkRows];
			for (int i = 0; i < chunks.length; i++) chunks[i] = new Chunk();
			quads = null;
			if (buffer != null){
				buffer.delete();
				buffer = null;
			}
		} else {
			quads = Quad.createSet( size );
		}

		updateMap();
	}

	/**
	 * Swaps in a new data array of the same shape and marks only the chunks whose content
	 * changed. For a layer recomputed from scratch (a pure function of the world) this leaves
	 * every unchanged region's buffer alone. Falls back to {@link #map} when the shape differs
	 * or the map is not chunked.
	 */
	public synchronized void refill( int[] data, int cols ){
		if (!useChunks || chunks == null || this.data == null
				|| cols != mapWidth || data.length / cols != mapHeight){
			map( data, cols );
			return;
		}
		int[] old = this.data;
		this.data = data;
		if (old == data) return;
		for (int cy = 0; cy < chunkRows; cy++){
			int y0 = cy * CHUNK, y1 = Math.min( mapHeight, y0 + CHUNK );
			for (int cx = 0; cx < chunkCols; cx++){
				Chunk c = chunks[cy * chunkCols + cx];
				if (c.dirty) continue;
				int x0 = cx * CHUNK, x1 = Math.min( mapWidth, x0 + CHUNK );
				scan:
				for (int y = y0; y < y1; y++){
					int base = y * mapWidth;
					for (int x = x0; x < x1; x++){
						if (old[base + x] != data[base + x]){
							c.dirty = true;
							break scan;
						}
					}
				}
			}
		}
	}

	public Image image(int x, int y){
		if (!needsRender(x + mapWidth*y)){
			return null;
		} else {
			Image img = new Image(texture);
			img.frame(tileset.get(data[x + mapWidth * y]));
			return img;
		}
	}

	//forces a full update, including new buffer
	public synchronized void updateMap(){
		updated.set( 0, 0, mapWidth, mapHeight );
		fullUpdate = true;
	}

	public synchronized void updateMapCell(int cell){
		updated.union( cell % mapWidth, cell / mapWidth );
	}

	/** marks a rectangle of cells (right and bottom exclusive) for rebuilding */
	public synchronized void updateMapRect( Rect r ){
		if (r == null || r.isEmpty()) return;
		if (useChunks && chunks != null){
			//straight onto the chunks: several far-apart rects must not widen into one
			//bounding rect that covers the whole map
			markChunks( r );
			return;
		}
		if (updated.isEmpty()){
			updated.set( r.left, r.top, r.right, r.bottom );
		} else {
			updated.set( Math.min( updated.left, r.left ), Math.min( updated.top, r.top ),
					Math.max( updated.right, r.right ), Math.max( updated.bottom, r.bottom ) );
		}
	}

	private synchronized void moveToUpdating(){
		updating = new Rect(updated);
		updated.setEmpty();
	}

	// ---------------------------------------------------------------- content shifts

	/**
	 * Moves cells by (dcx, dcy): the cell at (x, y) takes the value that was at (x+dcx, y+dcy).
	 * Cells that had no source keep whatever was in the destination. Safe when src and dst are
	 * the same array.
	 */
	public static void shiftCells( int[] src, int[] dst, int w, int h, int dcx, int dcy ){
		int yFrom = dcy >= 0 ? 0 : h - 1;
		int yTo   = dcy >= 0 ? h : -1;
		int yStep = dcy >= 0 ? 1 : -1;
		int len = w - Math.abs( dcx );
		if (len <= 0) return;
		for (int y = yFrom; y != yTo; y += yStep){
			int sy = y + dcy;
			if (sy < 0 || sy >= h) continue;
			System.arraycopy( src, sy * w + Math.max( 0, dcx ), dst, y * w + Math.max( 0, -dcx ), len );
		}
	}

	/**
	 * After the data moved by a chunk-aligned (dcx, dcy), re-labels the chunk objects so each
	 * keeps drawing the content it already holds; the chunks that now cover fresh cells are
	 * marked dirty. Returns false when the shift is not chunk aligned (or the map is not
	 * chunked), in which case the caller has to rebuild everything.
	 */
	protected synchronized boolean relabelChunks( int dcx, int dcy ){
		if (!useChunks || chunks == null || dcx % CHUNK != 0 || dcy % CHUNK != 0) return false;
		int[] source = relabelSources( chunkCols, chunkRows, dcx / CHUNK, dcy / CHUNK );
		Chunk[] fresh = new Chunk[chunks.length];
		boolean[] moved = new boolean[chunks.length];
		for (int i = 0; i < fresh.length; i++){
			if (source[i] >= 0){
				fresh[i] = chunks[source[i]];
				moved[source[i]] = true;
			}
		}
		ArrayList<Chunk> spare = new ArrayList<>();
		for (int i = 0; i < chunks.length; i++) if (!moved[i]) spare.add( chunks[i] );
		for (int i = 0; i < fresh.length; i++){
			if (fresh[i] == null){
				Chunk c = spare.isEmpty() ? new Chunk() : spare.remove( spare.size() - 1 );
				c.dirty = true;
				fresh[i] = c;
			}
		}
		chunks = fresh;
		return true;
	}

	/**
	 * For a content shift of (ddx, ddy) whole chunks: which old chunk each new grid slot
	 * takes its content from, or -1 when the slot now covers cells that were outside. Pure,
	 * so the relabel can be checked on its own: the same mapping {@link #shiftCells} applies
	 * to cells, at chunk granularity.
	 */
	public static int[] relabelSources( int cols, int rows, int ddx, int ddy ){
		int[] source = new int[cols * rows];
		for (int cy = 0; cy < rows; cy++){
			for (int cx = 0; cx < cols; cx++){
				int sx = cx + ddx, sy = cy + ddy;
				source[cy * cols + cx] = sx >= 0 && sy >= 0 && sx < cols && sy < rows ? sy * cols + sx : -1;
			}
		}
		return source;
	}

	/** shifts this map's own content in place (see {@link #shiftCells}); the exposed cells
	 *  are left for a following {@link #refill} or {@link #updateMapRect} to fill */
	public synchronized void shiftContent( int dcx, int dcy ){
		if (data == null) return;
		shiftCells( data, data, mapWidth, mapHeight, dcx, dcy );
		if (!relabelChunks( dcx, dcy )) updateMap();
	}

	/**
	 * The same content shift, on a copy of the data: for a map whose data array its owner may
	 * already have handed back as the NEXT window's (a refill with the very array this map
	 * holds). Shifting that in place would scramble the owner's copy too, and the refill that
	 * follows, seeing its own array again, would mark nothing to rebuild.
	 */
	public synchronized void shiftContentCopy( int dcx, int dcy ){
		if (data == null) return;
		data = data.clone();
		shiftContent( dcx, dcy );
	}

	// ---------------------------------------------------------------- drawing

	protected void updateVertices() {

		moveToUpdating();
		
		float x1, y1, x2, y2;
		int pos;
		RectF uv;

		y1 = cellH * updating.top;
		y2 = y1 + cellH;

		for (int i=updating.top; i < updating.bottom; i++) {

			x1 = cellW * updating.left;
			x2 = x1 + cellW;

			pos = i * mapWidth + updating.left;

			for (int j=updating.left; j < updating.right; j++) {

				if (topLeftUpdating == -1)
					topLeftUpdating = pos;

				bottomRightUpdating = pos + 1;

				((Buffer)quads).position(pos*16);
				
				uv = tileset.get(data[pos]);
				
				if (needsRender(pos) && uv != null) {

					vertices[0] = x1;
					vertices[1] = y1;

					vertices[2] = uv.left;
					vertices[3] = uv.top;

					vertices[4] = x2;
					vertices[5] = y1;

					vertices[6] = uv.right;
					vertices[7] = uv.top;

					vertices[8] = x2;
					vertices[9] = y2;

					vertices[10] = uv.right;
					vertices[11] = uv.bottom;

					vertices[12] = x1;
					vertices[13] = y2;

					vertices[14] = uv.left;
					vertices[15] = uv.bottom;

				} else {

					//If we don't need to draw this tile simply set the quad to size 0 at 0, 0.
					// This does result in the quad being drawn, but we are skipping all
					// pixel-filling. This is better than fully skipping rendering as we
					// don't need to manage a buffer of drawable tiles with insertions/deletions.
					Arrays.fill(vertices, 0);
				}

				quads.put(vertices);

				pos++;
				x1 = x2;
				x2 += cellW;

			}

			y1 = y2;
			y2 += cellH;
		}

	}

	private synchronized void foldUpdatesIntoChunks(){
		if (updated.isEmpty()) return;
		moveToUpdating();
		markChunks( updating );
		updating.setEmpty();
		fullUpdate = false;
	}

	private void markChunks( Rect r ){
		int cx0 = Math.max( 0, r.left / CHUNK );
		int cy0 = Math.max( 0, r.top / CHUNK );
		int cx1 = Math.min( chunkCols - 1, (r.right - 1) / CHUNK );
		int cy1 = Math.min( chunkRows - 1, (r.bottom - 1) / CHUNK );
		for (int cy = cy0; cy <= cy1; cy++){
			for (int cx = cx0; cx <= cx1; cx++){
				chunks[cy * chunkCols + cx].dirty = true;
			}
		}
	}

	//a chunk's vertices are in the chunk's OWN space (0..CHUNK cells): the slot it draws at
	//comes from the model matrix at draw time, so a relabel never has to touch a buffer
	private void rebuildChunk( Chunk c, int cx, int cy ){
		if (c.quads == null) c.quads = Quad.createSet( CHUNK * CHUNK );
		((Buffer)c.quads).position( 0 );
		int count = 0;
		int x0 = cx * CHUNK, y0 = cy * CHUNK;
		for (int ly = 0; ly < CHUNK; ly++){
			int y = y0 + ly;
			float y1 = cellH * ly, y2 = y1 + cellH;
			for (int lx = 0; lx < CHUNK; lx++){
				int x = x0 + lx;
				RectF uv = null;
				int pos = -1;
				if (x < mapWidth && y < mapHeight){
					pos = y * mapWidth + x;
					uv = tileset.get( data[pos] );
				}
				if (pos >= 0 && uv != null && needsRender( pos )){
					float x1 = cellW * lx, x2 = x1 + cellW;
					vertices[0] = x1;  vertices[1] = y1;  vertices[2] = uv.left;   vertices[3] = uv.top;
					vertices[4] = x2;  vertices[5] = y1;  vertices[6] = uv.right;  vertices[7] = uv.top;
					vertices[8] = x2;  vertices[9] = y2;  vertices[10] = uv.right; vertices[11] = uv.bottom;
					vertices[12] = x1; vertices[13] = y2; vertices[14] = uv.left;  vertices[15] = uv.bottom;
					count++;
				} else {
					Arrays.fill( vertices, 0 );
				}
				c.quads.put( vertices );
			}
		}
		c.count = count;
		c.dirty = false;
		//a chunk with nothing to draw never gets a GL buffer
		if (count > 0 || c.buffer != null){
			if (c.buffer == null) c.buffer = new Vertexbuffer( c.quads );
			else c.buffer.updateVertices( c.quads );
		}
	}

	@Override
	public void draw() {

		super.draw();

		if (useChunks){
			drawChunked();
			return;
		}

		if (!updated.isEmpty()) {
			updateVertices();
			if (buffer == null)
				buffer = new Vertexbuffer(quads);
			else {
				if (fullUpdate) {
					buffer.updateVertices(quads);
					fullUpdate = false;
				} else {
					buffer.updateVertices(quads,
							topLeftUpdating * 16,
							bottomRightUpdating * 16);
				}
			}
			topLeftUpdating = -1;
			updating.setEmpty();
		}

		NoosaScript script = script();

		texture.bind();

		script.uModel.valueM4( matrix );
		script.lighting(
				rm, gm, bm, am,
				ra, ga, ba, aa );

		script.camera( camera );

		script.drawQuadSetLarge( buffer, size );

	}

	private void drawChunked(){
		if (chunks == null) return;

		foldUpdatesIntoChunks();

		//which chunks the camera can see; a rotated or scaled map is not culled
		int cx0 = 0, cy0 = 0, cx1 = chunkCols - 1, cy1 = chunkRows - 1;
		Camera c = camera();
		if (c != null && angle == 0 && scale.x == 1f && scale.y == 1f){
			float cw = cellW * CHUNK, ch = cellH * CHUNK;
			float vx0 = c.scroll.x - x - CULL_PAD, vy0 = c.scroll.y - y - CULL_PAD;
			float vx1 = vx0 + c.width + 2 * CULL_PAD, vy1 = vy0 + c.height + 2 * CULL_PAD;
			cx0 = Math.max( 0, (int) Math.floor( vx0 / cw ) );
			cy0 = Math.max( 0, (int) Math.floor( vy0 / ch ) );
			cx1 = Math.min( chunkCols - 1, (int) Math.floor( vx1 / cw ) );
			cy1 = Math.min( chunkRows - 1, (int) Math.floor( vy1 / ch ) );
			if (cx0 > cx1 || cy0 > cy1) return;
		}

		NoosaScript script = script();

		texture.bind();

		script.lighting(
				rm, gm, bm, am,
				ra, ga, ba, aa );

		script.camera( camera );

		float cw = cellW * CHUNK, ch = cellH * CHUNK;
		for (int cy = cy0; cy <= cy1; cy++){
			for (int cx = cx0; cx <= cx1; cx++){
				Chunk chunk = chunks[cy * chunkCols + cx];
				if (chunk.dirty) rebuildChunk( chunk, cx, cy );
				if (chunk.buffer == null || chunk.count == 0) continue;
				com.watabou.glwrap.Matrix.copy( matrix, chunkMatrix );
				com.watabou.glwrap.Matrix.translate( chunkMatrix, cx * cw, cy * ch );
				script.uModel.valueM4( chunkMatrix );
				script.drawQuadSet( chunk.buffer, CHUNK * CHUNK, 0 );
			}
		}
	}
	
	protected NoosaScript script(){
		return NoosaScriptNoLighting.get();
	}

	private void releaseChunks(){
		if (chunks == null) return;
		for (Chunk c : chunks){
			if (c.buffer != null){
				c.buffer.delete();
				c.buffer = null;
			}
		}
		chunks = null;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (buffer != null) {
			buffer.delete();
			//destroy can reach the same tilemap twice (its group destroys it, then
			//whoever owns it replaces it): deleting a GL buffer id twice frees an
			//id the driver may have handed to someone else in between
			buffer = null;
		}
		releaseChunks();
	}

	protected boolean needsRender(int pos){
		return data[pos] >= 0;
	}
}
