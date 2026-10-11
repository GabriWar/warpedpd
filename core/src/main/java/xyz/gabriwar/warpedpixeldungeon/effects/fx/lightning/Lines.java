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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning;

import com.watabou.noosa.Image;

/**
 * Line quads, as the lightning lays them: an image's middle row (its origin) laid from one point
 * to another, turned and stretched about it. Only lines turn freely (the pixel rules); nothing
 * here allocates.
 */
public final class Lines {

	private Lines(){}

	private static final double DEG = 180 / Math.PI;

	/** A line image (its origin already at its middle row's start) from (x, y) to (toX, toY). */
	public static void place( Image line, float x, float y, float toX, float toY ){
		float dx = toX - x;
		float dy = toY - y;
		line.angle = (float)(Math.atan2( dy, dx ) * DEG);
		line.scale.x = (float)Math.sqrt( dx * dx + dy * dy ) / line.width;
		line.x = x - line.origin.x;
		line.y = y - line.origin.y;
	}

	/** Shown at `am`, or hidden (visible false: no draw spent) when there is nothing to show. */
	public static void shade( Image img, float am ){
		img.am = am;
		img.visible = am > 0.004f;
	}
}
