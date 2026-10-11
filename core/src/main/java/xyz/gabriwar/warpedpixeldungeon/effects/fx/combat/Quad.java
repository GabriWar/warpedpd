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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.RectF;

/**
 * One painted frame of the kit's sheets drawn by hand right before or after a sprite (in its
 * draw): a glow behind it, frost on it, vines at its feet. Its picture is made the first time it
 * is drawn (the render thread) and let go of on the render thread (release); setting it up
 * allocates nothing. Placed on whole pixels about a middle, at a whole scale, mirrored or not.
 */
final class Quad {

	private final boolean lightSheet;
	private Image image;
	private final RectF uv = new RectF();
	private int[] shown;
	//a crop of the frame: its share shown from the bottom up (1 all of it)
	private float cropUp = 1f;
	private int fx, fy, fw, fh;

	/** A frame of the sheet (FxFrames) or of the light sheet (FxFrames.Light). */
	Quad( boolean lightSheet ){
		this.lightSheet = lightSheet;
	}

	/** The frame it shows. */
	Quad show( int[] frame ){
		if (frame != shown){
			shown = frame;
			fx = frame[0];
			fy = frame[1];
			fw = frame[2];
			fh = frame[3];
			if (image != null) apply();
		}
		return this;
	}

	/** Only the share p of its frame shows, from its bottom up (vines growing, retracting). */
	Quad cropUp( float p ){
		p = Math.max( 0f, Math.min( 1f, p ) );
		if (p != cropUp){
			cropUp = p;
			if (image != null) apply();
		}
		return this;
	}

	int width(){
		return fw;
	}

	int height(){
		return fh;
	}

	private void apply(){
		int h = Math.max( 0, Math.round( fh * cropUp ) );
		float tw = image.texture.width, th = image.texture.height;
		uv.set( fx / tw, (fy + fh - h) / th, (fx + fw) / tw, (fy + fh) / th );
		image.frame( uv );
	}

	/**
	 * Drawn now with its middle at (cx, cy) (its bottom edge at cy + h/2 when cropped), in a colour
	 * at an alpha, added or not, at a whole scale, mirrored or not; through the sprite's camera.
	 * Render thread.
	 */
	void draw( Visual over, float cx, float cy, int color, float alpha, boolean light, int scale, boolean mirror ){
		if (shown == null || alpha <= 0.004f) return;
		if (image == null){
			image = new Image( lightSheet ? Assets.Effects.FX_LIGHT : Assets.Effects.FX_SHEET );
			apply();
		}
		if (cropUp <= 0f) return;
		image.camera = over.camera();
		if (image.flipHorizontal != mirror){
			image.flipHorizontal = mirror;
			apply();
		}
		image.scale.set( scale );
		image.resetColor();
		image.hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		image.am = Math.min( 1f, alpha );
		int h = Math.round( fh * cropUp );
		image.x = Math.round( cx - fw * scale / 2f );
		image.y = Math.round( cy - fh * scale / 2f ) + (fh - h) * scale;
		if (light) Blending.setLightMode();
		image.draw();
		if (light) Blending.setNormalMode();
	}

	/** Its picture let go of (its buffer freed on the render thread). Any thread. */
	void release(){
		Image gone = image;
		image = null;
		if (gone != null) Fx.dispose( gone );
	}
}
