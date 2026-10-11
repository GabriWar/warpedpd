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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * A glow lying on the floor under something that burns (a fire's cell, a coal bed, the rift's
 * crack, a vent): a frame of the light sheet (a 2:1 pool, or a small round glow at a waterline)
 * added to the floor it lies on, on the floor's layer - under the night's tint, so it dims after
 * dark where the lights of the light layer take over. It eases to the level it is told (0.25 s
 * either way), breathes or flickers like a hearth (within +-12%), and fades out before it goes.
 * Its frame sits on whole pixels.
 *
 * Its owner keeps it in a group (recycled from there) and steers it: level, out. No GL until it
 * is first drawn.
 */
public class FloorGlow extends Image {

	/** How fast it eases to its level, per s. */
	public static final float EASE = 4f;

	private float level, target, t, a, b, c;
	private int flicker;
	private boolean out;
	private float cx, cy;
	private final RectF uv = new RectF();

	public FloorGlow(){
		super( Assets.Effects.FX_LIGHT );
	}

	/** Lit afresh at (x, y) (its middle): a light-sheet frame in a colour, easing up to `level`,
	 *  flickering as FxLight's STEADY, BREATH or HEARTH. */
	public FloorGlow reset( float x, float y, int[] frame, int color, float level, int flicker ){
		revive();
		float tw = texture.width, th = texture.height;
		uv.set( frame[0] / tw, frame[1] / th, (frame[0] + frame[2]) / tw, (frame[1] + frame[3]) / th );
		frame( uv );
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		this.level = 0;
		target = level;
		this.flicker = flicker;
		out = false;
		t = 0;
		a = FxRandom.angle();
		b = FxRandom.angle();
		c = FxRandom.angle();
		am = 0;
		visible = false;
		moveTo( x, y );
		return this;
	}

	/** Its middle to (x, y), on whole pixels. */
	public void moveTo( float x, float y ){
		cx = x;
		cy = y;
		this.x = Math.round( x - width / 2f );
		this.y = Math.round( y - height / 2f );
	}

	public float centerX(){
		return cx;
	}

	public float centerY(){
		return cy;
	}

	/** The level it eases to. */
	public void level( float l ){
		target = l;
		out = false;
	}

	/** Fades away, then dies (and is recycled). */
	public void out(){
		target = 0;
		out = true;
	}

	public boolean isOut(){
		return out;
	}

	/** How bright it shows now (its alpha). */
	public float shown(){
		return am;
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		float dt = Game.elapsed;
		t += dt;
		float k = Math.min( 1f, dt * EASE );
		level += (target - level) * k;
		if (out && level < 0.01f){
			kill();
			return;
		}
		float f;
		switch (flicker){
			case FxLight.BREATH:
				f = 1f + 0.08f * (float)Math.sin( 2 * Math.PI * 0.45f * t + a );
				break;
			case FxLight.HEARTH:
			case FxLight.TORCH:
				f = 1f + 0.6f * 2.2f * FxCurves.hearth( t, a, b, c ) * (flicker == FxLight.TORCH ? 1.4f : 1f);
				f = Math.max( 0.88f, Math.min( 1.12f, f ) );
				break;
			default:
				f = 1f;
		}
		am = Math.max( 0f, Math.min( 1f, level * f ) );
		visible = am > 0.004f;
		//slid with its layer: its middle follows
		cx = x + width / 2f;
		cy = y + height / 2f;
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
