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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import com.watabou.noosa.Game;

/**
 * A mote going round a 2:1 ellipse lying on the ground (a portal's ring, a well's caustics):
 * its middle, its half-width and half-height, its turn in rad/s; on whole pixels. Everything else
 * (its look, colour, life, alpha) as FxParticle.
 */
public class EllipseMote extends FxParticle {

	private float cx, cy, rx, ry, a, spin;
	//where it was put, for a slide of its layer
	private float laidX, laidY;

	/** Round the ellipse about (cx, cy), `rx` by `ry` px, from angle `a`, at `spin` rad/s. */
	public EllipseMote round( float cx, float cy, float rx, float ry, float a, float spin ){
		reset( cx + (float)Math.cos( a ) * rx, cy + (float)Math.sin( a ) * ry );
		this.cx = cx;
		this.cy = cy;
		this.rx = rx;
		this.ry = ry;
		this.a = a;
		this.spin = spin;
		snap();
		laidX = x;
		laidY = y;
		return this;
	}

	@Override
	public void update(){
		//slid with its layer: its ring goes too (FxParticle adds the slide to its place itself)
		float sx = x - laidX, sy = y - laidY;
		cx += sx;
		cy += sy;
		a += spin * Game.elapsed;
		px = cx + (float)Math.cos( a ) * rx - sx;
		py = cy + (float)Math.sin( a ) * ry - sy;
		vx = vy = 0;
		super.update();
		laidX = x;
		laidY = y;
	}
}
