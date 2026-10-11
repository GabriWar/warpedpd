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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

/**
 * One painted frame of a sheet ({ x, y, w, h } as FxFrames gives it), drawn at x1 and, when
 * `additive`, added as light. Its uv rectangle is its own and reused, so stepping it to another
 * frame (a slash's next direction, a coin's turn) or cropping it (a column revealed, a jaw's half)
 * allocates nothing. Made anywhere: its texture is fetched through the cache and uploads when it
 * is first drawn. Positions are whole pixels unless the caller says otherwise (place()).
 */
public class FxPic extends Image {

	private final RectF uv = new RectF();
	/** Added (light) rather than blended (matter). */
	public boolean additive;
	//the frame shown, for crops
	private int fx, fy, fw, fh;
	//its middle as last placed, and where that put it (a slide of the window moves x and y)
	private float mx, my, lx, ly;

	public FxPic( Object texture ){
		super( texture );
	}

	/** Shows this frame whole. */
	public FxPic show( int[] f ){
		return crop( f, 0, 0, f[2], f[3] );
	}

	/** Shows the part of frame f from (left, top), w x h px of it. */
	public FxPic crop( int[] f, int left, int top, int w, int h ){
		fx = f[0] + left;
		fy = f[1] + top;
		fw = Math.max( 0, w );
		fh = Math.max( 0, h );
		float tw = texture.width, th = texture.height;
		uv.set( fx / tw, fy / th, (fx + fw) / tw, (fy + fh) / th );
		frame( uv );
		return this;
	}

	/** Its middle at (cx, cy), on whole pixels. */
	public FxPic place( float cx, float cy ){
		mx = cx;
		my = cy;
		x = lx = Math.round( cx - width / 2f );
		y = ly = Math.round( cy - height / 2f );
		return this;
	}

	/** Its middle as last placed, moved by any slide since (the overworld window's rebase moves
	 *  every picture's x and y): call before placing it again from its own middle. */
	public FxPic takeSlide(){
		if (x != lx || y != ly){
			mx += x - lx;
			my += y - ly;
			lx = x;
			ly = y;
		}
		return this;
	}

	/** Moved off its middle by (dx, dy) px for now (a twitch, a bob): the next place() puts it
	 *  back, and takeSlide() does not mistake it for a slide. */
	public FxPic nudge( float dx, float dy ){
		x += dx;
		y += dy;
		lx += dx;
		ly += dy;
		return this;
	}

	/** Its middle as last placed (slides taken in by takeSlide). */
	public float midX(){
		return mx;
	}

	public float midY(){
		return my;
	}

	/** Lit in a colour (light frames are white; matter frames grey-shaded: hardlight keeps their shading). */
	public FxPic lit( int color ){
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		return this;
	}

	@Override
	public void draw(){
		if (additive){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		} else {
			super.draw();
		}
	}
}
