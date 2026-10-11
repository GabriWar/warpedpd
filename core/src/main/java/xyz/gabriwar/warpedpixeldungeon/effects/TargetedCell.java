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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;

//a cell about to be struck (a boss's telegraph): its red reticle as ever, and under it a pool of
//red light breathing between POOL_LOW and POOL_HIGH once a second, and in the second half of every
//beat a ring closing on the cell from 23 px to 9 (painted ground rings, added) - the shape and the
//colour both say it. Drawn above the fog and the night with the reticle
public class TargetedCell extends Image implements Bundlable {

	public static final int TELEGRAPH = 0xFF3030;
	public static final float POOL_LOW = 0.15f, POOL_HIGH = 0.3f, HZ = 1f, RING_ALPHA = 0.6f;
	//the rings it closes through, widest first (RING_23, 17, 13, 9)
	private static final int[][] CLOSING = { FxFrames.RING_23, FxFrames.RING_17, FxFrames.RING_13, FxFrames.RING_9 };

	public int pos;
	public float time;

	private float alpha;
	private float beat;

	//the pool (light sheet) and the closing ring (sheet), made when first drawn
	private Image pool, ring;
	private final RectF poolUv = new RectF(), ringUv = new RectF();

	public static SparseArray<TargetedCell> cells = new SparseArray<>();

	public void reset( int pos, float time ){
		copy(Icons.get(Icons.TARGET));
		origin.set( width/2f );
		camera = null;

		this.pos = pos;
		point( DungeonTilemap.tileToWorld( pos ) );

		hardlight(0xFF0000);

		alpha = 1f;
		this.time = time;
		beat = 0;

		alpha(1f);
		scale.set(1f);

		synchronized (cells) {
			cells.put(pos, this);
		}
	}

	@Override
	public void update() {
		alpha -= Game.elapsed;
		beat += Game.elapsed;
		if (time >= Actor.now()){
			alpha = Math.max(alpha, 0.6f);
		}
		if (alpha > 0) {
			alpha( alpha );
			scale.set( (float)Math.pow(alpha, 0.33f) );
		} else {
			time = 0;
			synchronized (cells) {
				cells.remove(pos);
			}
			killAndErase();
		}
	}

	/** The pool's alpha `t` s into its beating. */
	public static float poolAlpha( float t ){
		return POOL_LOW + (POOL_HIGH - POOL_LOW) * (0.5f + 0.5f * (float)Math.sin( 2 * Math.PI * HZ * t ));
	}

	/** Which ring closes `t` s in (0: 23 px .. 3: 9 px), -1 in the first half of a beat. */
	public static int closing( float t ){
		float in = (t * HZ) % 1f;
		if (in < 0.5f) return -1;
		return Math.min( CLOSING.length - 1, (int)((in - 0.5f) / 0.5f * CLOSING.length) );
	}

	@Override
	public void draw() {
		//how much of its full self it shows (it fades once the blow has come)
		float k = Math.min( 1f, alpha / 0.6f );
		float cx = x + width / 2f, cy = y + height / 2f;
		if (pool == null) pool = new Image( Assets.Effects.FX_LIGHT );
		if (ring == null) ring = new Image( Assets.Effects.FX_SHEET );
		Blending.setLightMode();
		frame( pool, poolUv, FxFrames.Light.POOL_15 );
		lit( pool, cx, cy + 3, poolAlpha( beat ) * k );
		int r = time >= Actor.now() ? closing( beat ) : -1;
		if (r >= 0){
			frame( ring, ringUv, CLOSING[r] );
			lit( ring, cx, cy + 2, RING_ALPHA * k );
		}
		Blending.setNormalMode();
		super.draw();
	}

	private static void frame( Image img, RectF uv, int[] f ){
		float tw = img.texture.width, th = img.texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		img.frame( uv );
	}

	private void lit( Image img, float cx, float cy, float a ){
		img.camera = camera();
		img.resetColor();
		img.hardlight( TELEGRAPH );
		img.am = a;
		img.x = Math.round( cx - img.width / 2f );
		img.y = Math.round( cy - img.height / 2f );
		img.draw();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (pool != null) pool.destroy();
		if (ring != null) ring.destroy();
	}

	/** A sliding-window level moved (OverworldLevel.rebase, render thread): every mark follows
	 *  its cell, and the ones that left the window go. */
	public static void shiftAll( int dx, int dy, int width, int height ){
		synchronized (cells){
			java.util.List<TargetedCell> all = cells.valueList();
			cells.clear();
			for (TargetedCell c : all){
				int x = c.pos % width - dx, y = c.pos / width - dy;
				if (x <= 0 || y <= 0 || x >= width - 1 || y >= height - 1){
					c.killAndErase();
					continue;
				}
				c.pos = x + y * width;
				c.point( DungeonTilemap.tileToWorld( c.pos ) );
				cells.put( c.pos, c );
			}
		}
	}

	public static void fixTime(float min){
		synchronized (cells){
			for (TargetedCell c : cells.valueList()){
				c.time -= min;
			}
		}
	}

	private static final String POS = "pos";
	private static final String COLOR = "color";
	private static final String TIME = "time";

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(POS, pos);
		bundle.put(TIME, time);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		pos = bundle.getInt(POS);
		time = bundle.getInt(TIME);
	}

}
