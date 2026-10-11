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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * One painted frame of light that is not a glow or a pool (a column of light, a star's head, a
 * rim of heat): added to what lies under it, its middle at a point and on whole pixels, rising
 * to its alpha over `attack` s, held, gone over `release` s - or held until put out when it is
 * given no life - shown only while the hero sees its point when it is told to (seenAt). Made with
 * a frame of a sheet (the light sheet by default); no GL until it is drawn.
 */
public class LightFrame extends Image {

	private float peak, attack, hold, release, age;
	private boolean lasting, out, sourced;
	private float outFor, outLeft, delay;
	private float breathHz, breathAmp, breathPhase;
	private float cx, cy, srcX, srcY;
	private final RectF uv = new RectF();

	public LightFrame(){
		super( Assets.Effects.FX_LIGHT );
	}

	public LightFrame( Object texture ){
		super( texture );
	}

	/** Its frame ({x, y, w, h} of its sheet), its middle at (x, y), in a colour at `alpha`. */
	public LightFrame setup( int[] frame, float x, float y, int color, float alpha ){
		revive();
		float tw = texture.width, th = texture.height;
		uv.set( frame[0] / tw, frame[1] / th, (frame[0] + frame[2]) / tw, (frame[1] + frame[3]) / th );
		frame( uv );
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		peak = alpha;
		attack = hold = 0;
		release = 0.01f;
		lasting = true;
		out = sourced = false;
		age = 0;
		delay = 0;
		breathAmp = 0;
		am = 0;
		visible = false;
		moveTo( x, y );
		return this;
	}

	/** Another frame of its sheet in its place (a twinkle's turn), its middle kept. */
	public LightFrame show( int[] frame ){
		float tw = texture.width, th = texture.height;
		uv.set( frame[0] / tw, frame[1] / th, (frame[0] + frame[2]) / tw, (frame[1] + frame[3]) / th );
		frame( uv );
		moveTo( cx, cy );
		return this;
	}

	/** A life: up over `attack`, held `hold`, down over `release`, then gone. */
	public LightFrame life( float attack, float hold, float release ){
		this.attack = attack;
		this.hold = hold;
		this.release = Math.max( 0.01f, release );
		lasting = false;
		age = 0;
		return this;
	}

	/** Breathing: its alpha swells and ebbs by `amp` (within 0.12) `hz` times a second. */
	public LightFrame breathe( float hz, float amp ){
		breathHz = hz;
		breathAmp = Math.min( 0.12f, amp );
		breathPhase = xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.angle();
		return this;
	}

	/** Nothing shown for `seconds`; its life starts after. */
	public LightFrame delay( float seconds ){
		delay = seconds;
		return this;
	}

	/** Shown only where the hero sees (x, y). */
	public LightFrame seenAt( float x, float y ){
		sourced = true;
		srcX = x;
		srcY = y;
		return this;
	}

	/** Its peak alpha (from now). */
	public LightFrame peak( float a ){
		peak = a;
		return this;
	}

	public void moveTo( float x, float y ){
		cx = x;
		cy = y;
		this.x = Math.round( x - width / 2f );
		this.y = Math.round( y - height / 2f );
	}

	/** Fades out over `fade` s, then gone. */
	public void putOut( float fade ){
		if (out) return;
		out = true;
		outFor = outLeft = Math.max( 0.01f, fade );
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		float dt = Game.elapsed;
		if (delay > 0){
			delay -= dt;
			visible = false;
			return;
		}
		age += dt;
		float env = 1f;
		if (!lasting){
			if (age < attack) env = age / attack;
			else if (age > attack + hold){
				float r = (age - attack - hold) / release;
				if (r >= 1f){
					gone();
					return;
				}
				env = 1f - r;
			}
		}
		if (out){
			outLeft -= dt;
			if (outLeft <= 0){
				gone();
				return;
			}
			env *= outLeft / outFor;
		}
		if (breathAmp > 0) env *= 1f + breathAmp * (float)Math.sin( 2 * Math.PI * breathHz * age + breathPhase );
		am = peak * env;
		visible = am > 0.004f && (!sourced || Fx.seen( srcX, srcY ));
	}

	//off its group and its buffers freed (an erased picture keeps them till the scene changes,
	//and the surface is one long scene)
	private void gone(){
		killAndErase();
		destroy();
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
