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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;

/**
 * A painted picture that comes and goes on its own: a star snapping on, a shaft of light, a
 * chromatic ghost, a dark mark. It lives `life` s, its alpha up over `in` s and down over its
 * last `out` s (light fades smoothly; matter in the painted steps 1, 0.6, 0.3, 0), may swap
 * between two frames (a star's A and B: its spin) and drift, and shows only where the hero sees
 * the point it belongs to. Pooled in the light layer (light) or among the effects (matter); counted
 * as a particle in FxBudget while it lives.
 */
public class ArcFlash extends ArcImage {

	private float life, age, peak, in, out;
	private int[] a, b;
	private float swapEvery;
	private float cx, cy, vx, vy;
	private float srcX, srcY;
	private int step;
	private boolean counted;
	private float delay;

	public ArcFlash(){
		super();
	}

	/**
	 * A picture of a frame of the light sheet (light) or the plain sheet, its middle at (x, y), in
	 * `color` up to `alpha`, for `life` s; added to the light layer (light) or the effects' (matter).
	 * Null off the scene. Render thread.
	 */
	public static ArcFlash at( boolean lightSheet, boolean additive, float x, float y, int[] frame, int color, float alpha, float life ){
		GameScene.Layer layer = additive ? GameScene.Layer.LIGHTS : GameScene.Layer.EFFECTS;
		if (GameScene.layer( layer ) == null) return null;
		ArcFlash f = GameScene.recycle( layer, ArcFlash.class );
		if (f == null) return null;
		f.renew( lightSheet, additive );
		f.setup( x, y, frame, color, alpha, life );
		return f;
	}

	/** A frame of the light sheet, added. */
	public static ArcFlash light( float x, float y, int[] frame, int color, float alpha, float life ){
		return at( true, true, x, y, frame, color, alpha, life );
	}

	private void setup( float x, float y, int[] frame, int color, float alpha, float life ){
		a = frame;
		b = null;
		swapEvery = 0;
		step = 0;
		cx = srcX = x;
		cy = srcY = y;
		vx = vy = 0;
		this.life = Math.max( 0.01f, life );
		age = 0;
		delay = 0;
		peak = alpha;
		in = 0;
		out = life;
		show( frame );
		rgb( color );
		am = alpha;
		centre( cx, cy );
		if (!counted){
			counted = true;
			FxBudget.add( FxBudget.PARTICLES, 1 );
		}
	}

	/** Swapping to a second frame and back every `seconds` (a star's spin). */
	public ArcFlash swap( int[] other, float seconds ){
		b = other;
		swapEvery = seconds;
		return this;
	}

	/** Up over `in` s, down over the last `out` s. */
	public ArcFlash fade( float in, float out ){
		this.in = in;
		this.out = out;
		return this;
	}

	/** Drawn at a whole scale (1, 2 or 3). */
	public ArcFlash times( int k ){
		scale.set( k );
		centre( cx, cy );
		return this;
	}

	/** Drifting at (vx, vy) px/s. */
	public ArcFlash drift( float vx, float vy ){
		this.vx = vx;
		this.vy = vy;
		return this;
	}

	/** Shown only after `seconds`. */
	public ArcFlash after( float seconds ){
		delay = seconds;
		visible = false;
		return this;
	}

	/** Shown where the hero sees this point (its own middle by default). */
	public ArcFlash seenAt( float x, float y ){
		srcX = x;
		srcY = y;
		return this;
	}

	public float progress(){
		return Math.min( 1f, age / life );
	}

	@Override
	public void update(){
		super.update();
		float dt = Game.elapsed;
		if (delay > 0){
			delay -= dt;
			visible = false;
			return;
		}
		age += dt;
		if (age >= life){
			kill();
			return;
		}
		cx += vx * dt;
		cy += vy * dt;
		if (b != null && swapEvery > 0){
			int s = (int)(age / swapEvery);
			if (s != step){
				step = s;
				show( (s & 1) == 0 ? a : b );
			}
		}
		float k = peak;
		if (in > 0 && age < in) k *= age / in;
		float left = life - age;
		if (out > 0 && left < out){
			float f = left / out;
			k *= additive ? f : (f > 0.75f ? 1f : f > 0.5f ? 0.6f : f > 0.25f ? 0.3f : 0f);
		}
		am = k;
		centre( cx, cy );
		visible = k > 0.004f && Fx.seen( srcX, srcY + Arc.GROUND );
	}

	@Override
	public void kill(){
		super.kill();
		if (counted){
			counted = false;
			FxBudget.add( FxBudget.PARTICLES, -1 );
		}
	}
}
