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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * A frame of the light sheet flashing once where it is put, added in the light layer over the
 * night's tint, fading from its alpha to nothing over its life: a shield's arc glinting as a blow
 * glances off it. Pooled in the light layer; counted as a kit particle while it shows. Render
 * thread.
 */
public class LightFlash extends Image {

	private float peak, life, age;
	private boolean counted;
	private final RectF uv = new RectF();

	public LightFlash(){
		super( Assets.Effects.FX_LIGHT );
	}

	/** A frame (FxFrames.Light) with its middle at (x, y) in a colour, from `alpha` to nothing over
	 *  `life` s. Null off the game's scene. */
	public static LightFlash at( float x, float y, int[] frame, int color, float alpha, float life ){
		LightFlash f = GameScene.recycle( GameScene.Layer.LIGHTS, LightFlash.class );
		if (f == null) return null;
		f.setup( x, y, frame, color, alpha, life );
		return f;
	}

	private void setup( float x, float y, int[] f, int color, float alpha, float life ){
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
		this.x = Math.round( x - f[2] / 2f );
		this.y = Math.round( y - f[3] / 2f );
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		peak = alpha;
		am = alpha;
		this.life = Math.max( 0.01f, life );
		age = 0;
		visible = true;
		revive();
		if (!counted){
			counted = true;
			FxBudget.add( FxBudget.PARTICLES, 1 );
		}
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		age += Game.elapsed;
		if (age >= life){
			kill();
			return;
		}
		am = peak * (1f - age / life);
	}

	@Override
	public void kill(){
		super.kill();
		if (counted){
			counted = false;
			FxBudget.add( FxBudget.PARTICLES, -1 );
		}
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
