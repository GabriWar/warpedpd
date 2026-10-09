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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * A mark left on the floor: a scorch, a splat, a wet patch, an etch, rime, a crack, a web. Matter
 * (grey frames lit in its colour, drawn normally), on the floor under everyone, world-anchored
 * and on whole pixels; it holds, then fades. At most FxBudget.decalCap() at once (32, fewer on a
 * struggling phone): past it the oldest fades away over OLDEST_FADE s. Rime, vines and cracks can
 * grow into place (revealCrop).
 */
public class FxDecal extends Image {

	/** Which way a mark grows into place: from its left edge, its right, its bottom up, its top
	 *  down, or out from its middle. */
	public static final int FROM_LEFT = 0, FROM_RIGHT = 1, FROM_BOTTOM = 2, FROM_TOP = 3, FROM_MIDDLE = 4;
	public static final float OLDEST_FADE = 0.3f;
	/** A wet mark's: its colour, alpha, hold and fade (gone in 0.8 s). */
	public static final int WET_COLOR = 0x1A2430;
	public static final float WET_ALPHA = 0.35f, WET_HOLD = 0.3f, WET_FADE = 0.5f;

	//the live ones, for the cap
	private static final FxDecal[] live = new FxDecal[96];
	private static int count;
	private static long serials;

	private int[] frame;
	private float peak, hold, fade, age;
	private float cutLeft = -1;
	private int reveal = -1;
	private float revealFor;
	private float cx, cy;
	//where it was last put: a move from outside since (a window's slide) moves its middle too
	private float placedX, placedY;
	private long serial;
	private final RectF uv = new RectF();

	public FxDecal(){
		super( Assets.Effects.FX_SHEET );
	}

	/** A new scene: none of the last one's left. */
	public static synchronized void reset(){
		for (int i = 0; i < count; i++) live[i] = null;
		count = 0;
	}

	/** How many are on the floor. */
	public static synchronized int live(){
		return count;
	}

	/**
	 * A mark at (x, y), its middle there: a frame of FxFrames lit in `color` at `alpha`, held for
	 * `hold` s then faded over `fade` s, mirrored or not. Null off the game's scene.
	 */
	public static FxDecal at( float x, float y, int[] frame, int color, float alpha, float hold, float fade, boolean mirror ){
		FxDecal d = GameScene.recycle( GameScene.Layer.FLOOR, FxDecal.class );
		if (d == null) return null;
		d.setup( x, y, frame, color, alpha, hold, fade, mirror );
		enter( d );
		return d;
	}

	/** A liquid's splat, in its colour: one of three sizes, held 2.5 s and faded over 1. */
	public static FxDecal splat( float x, float y, int color ){
		int[][] s = FxFrames.SPLAT;
		return at( x, y, s[FxRandom.Int( s.length )], color, 0.8f, 2.5f, 1f, FxRandom.chance( 0.5f ) );
	}

	/** A drop's wet mark on the floor, gone in 0.8 s. */
	public static FxDecal wet( float x, float y ){
		return at( x, y, FxFrames.WET_5, WET_COLOR, WET_ALPHA, WET_HOLD, WET_FADE, false );
	}

	/** It grows into place from one side over `seconds`. */
	public FxDecal revealCrop( int from, float seconds ){
		reveal = from;
		revealFor = Math.max( 0.01f, seconds );
		crop( 0f );
		return this;
	}

	private void setup( float x, float y, int[] frame, int color, float alpha, float hold, float fade, boolean mirror ){
		this.frame = frame;
		peak = alpha;
		this.hold = hold;
		this.fade = Math.max( 0.01f, fade );
		age = 0;
		cutLeft = -1;
		reveal = -1;
		cx = x;
		cy = y;
		flipHorizontal = mirror;
		flipVertical = false;
		angle = 0;
		scale.set( 1 );
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		am = alpha;
		crop( 1f );
		visible = true;
		revive();
	}

	//shows the share p of its frame, grown from its reveal side (all of it without one), placed
	//on whole pixels about its middle
	private void crop( float p ){
		int fx = frame[0], fy = frame[1], fw = frame[2], fh = frame[3];
		int l = fx, t = fy, r = fx + fw, b = fy + fh;
		if (reveal >= 0 && p < 1f){
			int w = Math.max( 1, Math.round( fw * p ) ), h = Math.max( 1, Math.round( fh * p ) );
			switch (reveal){
				case FROM_LEFT:   r = fx + w; break;
				case FROM_RIGHT:  l = fx + fw - w; break;
				case FROM_BOTTOM: t = fy + fh - h; break;
				case FROM_TOP:    b = fy + h; break;
				default:
					l = fx + (fw - w) / 2; r = l + w;
					t = fy + (fh - h) / 2; b = t + h;
			}
		}
		float tw = texture.width, th = texture.height;
		uv.set( l / tw, t / th, r / tw, b / th );
		frame( uv );
		//the shown part stays where it lies within the whole frame
		x = Math.round( cx - fw / 2f ) + (l - fx);
		y = Math.round( cy - fh / 2f ) + (t - fy);
		if (flipHorizontal) x = Math.round( cx - fw / 2f ) + (fx + fw - r);
		placedX = x;
		placedY = y;
	}

	private static synchronized void enter( FxDecal d ){
		d.serial = ++serials;
		//over the cap: the oldest still holding fades away now
		int cap = FxBudget.decalCap();
		int holding = 0;
		FxDecal oldest = null;
		for (int i = 0; i < count; i++){
			FxDecal o = live[i];
			if (o.cutLeft >= 0) continue;
			holding++;
			if (oldest == null || o.serial < oldest.serial) oldest = o;
		}
		if (holding >= cap && oldest != null) oldest.cutLeft = OLDEST_FADE;
		if (count < live.length){
			live[count++] = d;
			FxBudget.add( FxBudget.DECALS, 1 );
		}
	}

	private static synchronized void leave( FxDecal d ){
		for (int i = 0; i < count; i++){
			if (live[i] == d){
				live[i] = live[--count];
				live[count] = null;
				FxBudget.add( FxBudget.DECALS, -1 );
				return;
			}
		}
	}

	@Override
	public void kill(){
		super.kill();
		leave( this );
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		cx += x - placedX;
		cy += y - placedY;
		placedX = x;
		placedY = y;
		float dt = Game.elapsed;
		age += dt;
		if (reveal >= 0){
			float p = Math.min( 1f, age / revealFor );
			crop( p );
			if (p >= 1f) reveal = -1;
		}
		float a = peak;
		if (cutLeft >= 0){
			cutLeft -= dt;
			a *= Math.max( 0f, cutLeft / OLDEST_FADE ) * Math.min( 1f, Math.max( 0f, 1f - (age - hold) / fade ) );
			if (cutLeft <= 0){
				kill();
				return;
			}
		} else if (age > hold){
			float k = 1f - (age - hold) / fade;
			if (k <= 0){
				kill();
				return;
			}
			a *= k;
		}
		am = a;
	}
}
