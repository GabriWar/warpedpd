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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Texture;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.noosa.Game;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * The sea of clouds below a high peak (levels/overworld/PeakLife): standing on the summit
 * you look down onto cloud tops drifting with the wind. Drawn on the ground layer under
 * everyone (GameScene.floorEffect) as one quad per cell of chasm in view - never the ground -
 * each showing its 16x16 of a seamless tile (effects/cloud_sea.png), so the cloud slips behind
 * the mountain as it drifts. Under the ground's edge it starts below what that row draws (a
 * lip, a wall's face) and the ground's shadow; wherever it meets something standing in front
 * of it its last pixels come from the frayed copy of the tile (effects/cloud_sea_rim.png, every
 * other texel), so it thins into the drop instead of ending on a ruler line. Two layers of the
 * same tile, the deeper one further along it, darker and slower, so the tile's repeat is lost
 * and the sea has depth.
 *
 * Its cells are the window's, set by PeakLife whenever the window and the scene agree; a
 * rebase slides it with the floor layer until then. The tile is sampled in world pixels, so
 * nothing jumps when the window moves. Render thread only.
 */
public class CloudSea extends Visual {

	/** The sides of a cell where the cloud meets the mountain (PeakLife.cloudEdge); the low bits
	 *  are the rows it keeps clear at the top. */
	public static final int CUT_TOP = 32, CUT_LEFT = 64, CUT_RIGHT = 128;
	private static final int LIP = 31;
	//how deep the cloud frays where it is cut, pixels
	private static final int RIM = 3;

	//the deck's side in pixels (CloudDeck): power of two, so it can repeat
	private static final int T = CloudDeck.SIZE;
	//the deeper layer's place along the tile and its pace against the near one
	private static final int DEEP_U = 131, DEEP_V = 77;
	private static final float DEEP_PACE = 0.6f;

	//the deck for the game's seed, once CloudDeck has painted it (null till then: nothing drawn)
	private SmartTexture texture, rim;
	private final long seed;
	private int[] cells = new int[0], edges = new int[0];
	private int count, rims, w, originX, originY;
	private float driftX, driftY;
	//the wind's drift, scene pixels a second
	public float vx, vy;
	private float alpha, shown, deep;
	private float tr = 1f, tg = 1f, tb = 1f;

	private float[] vertices = new float[16];
	private FloatBuffer quads;
	private Vertexbuffer buffer;
	private int capacity, built, builtRims;
	private boolean dirty;
	private int du = Integer.MIN_VALUE, dv, deepU, deepV;

	public CloudSea(){
		this( Dungeon.seed );
	}

	public CloudSea( long seed ){
		super( 0, 0, 0, 0 );
		this.seed = seed;
	}

	//no size of its own (the cells it covers are laid where the scene draws them), so the
	//visual's own culling has to be bypassed: it would see a point at the window's origin and
	//skip the whole sea whenever the camera lay past it
	@Override
	public boolean isVisible(){
		return visible && camera() != null;
	}

	/**
	 * The window cells it covers (cell indices of a window `w` wide whose cell (0,0) is world
	 * cell (originX, originY)) and how it lies on each (PeakLife.cloudEdge), laid where the
	 * scene draws that window now.
	 */
	public void cover( int[] cells, int[] edges, int count, int w, int originX, int originY ){
		if (this.cells.length < count){
			this.cells = new int[Math.max( count, this.cells.length * 2 )];
			this.edges = new int[this.cells.length];
		}
		System.arraycopy( cells, 0, this.cells, 0, count );
		System.arraycopy( edges, 0, this.edges, 0, count );
		this.count = count;
		rims = 0;
		for (int i = 0; i < count; i++) rims += Integer.bitCount( edges[i] & (CUT_TOP | CUT_LEFT | CUT_RIGHT) );
		this.w = w;
		this.originX = originX;
		this.originY = originY;
		x = y = 0f;
		dirty = true;
	}

	/** How it looks: the near layer's alpha, the deep layer's against it, and the light's tint. */
	public void look( float alpha, float deep, int tint ){
		this.alpha = alpha;
		this.deep = deep;
		tr = ((tint >> 16) & 0xFF) / 255f;
		tg = ((tint >> 8) & 0xFF) / 255f;
		tb = (tint & 0xFF) / 255f;
	}

	/** Moves it by (dx, dy) scene pixels: a network mirror's window was re-labelled under it. */
	public void slide( float dx, float dy ){
		x += dx;
		y += dy;
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters) return;
		float dt = Game.elapsed;
		driftX += vx * dt;
		driftY += vy * dt;
		//fades in on arrival and with the hour, a second or so either way
		shown += Math.max( -dt, Math.min( dt, alpha - shown ) );
		//whole texels only: the art stays pixel art, and the buffer is rebuilt a few times a second
		int nu = Math.floorMod( (int)Math.floor( driftX ), T ), nv = Math.floorMod( (int)Math.floor( driftY ), T );
		int ndu = Math.floorMod( (int)Math.floor( driftX * DEEP_PACE ), T ), ndv = Math.floorMod( (int)Math.floor( driftY * DEEP_PACE ), T );
		if (nu != du || nv != dv || ndu != deepU || ndv != deepV){
			du = nu;
			dv = nv;
			deepU = ndu;
			deepV = ndv;
			dirty = true;
		}
	}

	private void build(){
		int quadsNeeded = 2 * (count + rims);
		if (capacity < quadsNeeded){
			capacity = Math.max( 64, quadsNeeded + quadsNeeded / 4 );
			quads = Quad.createSet( capacity );
			if (buffer != null){
				buffer.delete();
				buffer = null;
			}
		}
		((Buffer)quads).position( 0 );
		final int S = DungeonTilemap.SIZE;
		//per layer: the cells' bodies (the whole tile), then their frayed edges (the rim tile)
		for (int layer = 0; layer < 2; layer++){
			//the deep layer first, drawn first
			int u0 = layer == 0 ? DEEP_U - deepU : -du, v0 = layer == 0 ? DEEP_V - deepV : -dv;
			for (int i = 0; i < count; i++){
				int c = cells[i], e = edges[i], x1 = (c % w) * S, y1 = (c / w) * S;
				int top = (e & CUT_TOP) != 0 ? (e & LIP) + RIM : 0;
				quad( x1 + ((e & CUT_LEFT) != 0 ? RIM : 0), y1 + top,
						x1 + S - ((e & CUT_RIGHT) != 0 ? RIM : 0), y1 + S, u0, v0 );
			}
			for (int i = 0; i < count; i++){
				int c = cells[i], e = edges[i], x1 = (c % w) * S, y1 = (c / w) * S;
				int top = 0;
				if ((e & CUT_TOP) != 0){
					top = (e & LIP) + RIM;
					quad( x1, y1 + (e & LIP), x1 + S, y1 + top, u0, v0 );
				}
				if ((e & CUT_LEFT) != 0) quad( x1, y1 + top, x1 + RIM, y1 + S, u0, v0 );
				if ((e & CUT_RIGHT) != 0) quad( x1 + S - RIM, y1 + top, x1 + S, y1 + S, u0, v0 );
			}
		}
		((Buffer)quads).position( 0 );
		if (buffer == null) buffer = new Vertexbuffer( quads );
		else buffer.updateVertices( quads, 0, quadsNeeded * 16 );
		built = count;
		builtRims = rims;
		dirty = false;
	}

	//the window-pixel rectangle (x1, y1)-(x2, y2), showing the tile at its world pixels moved by (u0, v0):
	//the same cloud over the same air in every window
	private void quad( int x1, int y1, int x2, int y2, int u0, int v0 ){
		float u = Math.floorMod( originX * DungeonTilemap.SIZE + x1 + u0, T ) / (float)T;
		float v = Math.floorMod( originY * DungeonTilemap.SIZE + y1 + v0, T ) / (float)T;
		float u2 = u + (x2 - x1) / (float)T, v2 = v + (y2 - y1) / (float)T;
		vertices[0] = x1;  vertices[1] = y1;  vertices[2] = u;   vertices[3] = v;
		vertices[4] = x2;  vertices[5] = y1;  vertices[6] = u2;  vertices[7] = v;
		vertices[8] = x2;  vertices[9] = y2;  vertices[10] = u2; vertices[11] = v2;
		vertices[12] = x1; vertices[13] = y2; vertices[14] = u;  vertices[15] = v2;
		quads.put( vertices );
	}

	@Override
	public void draw(){
		if (count == 0 || shown <= 0.005f) return;
		if (texture == null){
			SmartTexture[] deck = CloudDeck.get( seed );
			if (deck == null) return;
			texture = deck[0];
			rim = deck[1];
		}
		super.draw();
		if (dirty) build();
		NoosaScript script = NoosaScript.get();
		texture.bind();
		script.camera( camera() );
		script.uModel.valueM4( matrix );
		//the deep layer, darker, then the near one: each its bodies, then its frayed edges
		int layer = built + builtRims;
		script.lighting( tr * 0.82f, tg * 0.82f, tb * 0.82f, shown * deep, 0, 0, 0, 0 );
		script.drawQuadSet( buffer, built, 0 );
		rim.bind();
		script.drawQuadSet( buffer, builtRims, built );
		script.lighting( tr, tg, tb, shown, 0, 0, 0, 0 );
		texture.bind();
		script.drawQuadSet( buffer, built, layer );
		rim.bind();
		script.drawQuadSet( buffer, builtRims, layer + built );
	}

	@Override
	public void destroy(){
		super.destroy();
		if (buffer != null){
			buffer.delete();
			buffer = null;
		}
	}
}
