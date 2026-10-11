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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

/**
 * A picture's painted pixels as the places things leave from or gather to (an item dissolving
 * into motes, re-forming out of them): BitmaskEmitter's pick, on the effects' own random numbers
 * and without allocating, its frame looked up once by its caller. Read only: the picture's
 * bitmap (no GL).
 */
public final class ArcBitmask {

	private ArcBitmask(){}

	/** Tries at a painted pixel before settling for the picture's middle. */
	public static final int TRIES = 16;

	/**
	 * A world point on one of `img`'s painted (non-transparent) pixels within its frame `uv`
	 * (Image.frame(), looked up once), into out[0], out[1]; its middle if none is found in TRIES.
	 */
	public static void pick( Image img, RectF uv, float[] out ){
		Pixmap bm = img.texture != null ? img.texture.bitmap : null;
		if (bm != null && uv != null){
			int w = bm.getWidth(), h = bm.getHeight();
			float ox = uv.left * w, oy = uv.top * h;
			float fw = uv.width() * w, fh = uv.height() * h;
			for (int i = 0; i < TRIES; i++){
				float x = FxRandom.Float( fw ), y = FxRandom.Float( fh );
				if ((bm.getPixel( (int)(x + ox), (int)(y + oy) ) & 0xFF) != 0){
					out[0] = img.x + x * img.scale.x;
					out[1] = img.y + y * img.scale.y;
					return;
				}
			}
		}
		out[0] = img.x + img.width() / 2f;
		out[1] = img.y + img.height() / 2f;
	}
}
