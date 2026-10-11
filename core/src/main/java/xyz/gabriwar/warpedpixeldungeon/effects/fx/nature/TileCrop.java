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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

/**
 * A copy of a tile's picture shown a row at a time, never scaled: grown up from its foot in four
 * steps (a growth's light running up the blades, a plant coming up), or flattened from its top in
 * steps as it fades (high grass parting under a step). Gone (its buffers freed) when it is done.
 */
public class TileCrop extends Image {

	/** Its shows: a light running up a tile; high grass parting; a plant coming up. */
	public static final int REVEAL = 0, PART = 1, GROW = 2;
	/** The reveal: four steps over REVEAL_FOR s at its alpha, then fading over REVEAL_FADE s. */
	public static final float REVEAL_FOR = 0.2f, REVEAL_FADE = 0.3f;
	/** The parting: 16, 12, then 8 rows over PART_FOR s, fading from 1 to 0. */
	public static final float PART_FOR = 0.15f;
	/** The growth: four steps over GROW_FOR s, a pixel's bounce for BOUNCE_FOR s. */
	public static final float GROW_FOR = 0.2f, BOUNCE_FOR = 0.06f;

	private static final int SIZE = DungeonTilemap.SIZE;

	private final RectF uv = new RectF();
	private float u0, v0, u1, v1;
	private float baseX, baseY, placedX, placedY;
	private int mode, rows = -1;
	private float t, alpha0;
	private boolean additive;
	private Runnable then;

	/** A copy of `src`'s frame laid on the cell whose top-left is (x, y), to show as `mode`. */
	public TileCrop( Image src, float x, float y, int mode, float alpha, boolean additive ){
		super();
		texture = src.texture;
		RectF f = src.frame();
		u0 = f.left;
		v0 = f.top;
		u1 = f.right;
		v1 = f.bottom;
		baseX = x;
		baseY = y;
		this.mode = mode;
		alpha0 = alpha;
		this.additive = additive;
		this.x = placedX = x;
		rows( mode == PART ? SIZE : SIZE / 4 );
		placedY = this.y;
		am = mode == PART || mode == GROW ? 1f : alpha;
	}

	/** Runs `r` as it ends (the growth: the real plant shown in its place). */
	public TileCrop then( Runnable r ){
		then = r;
		return this;
	}

	//only its bottom `r` rows
	private void rows( int r ){
		if (r != rows){
			rows = r;
			float k = r / (float)SIZE;
			uv.set( u0, v1 - (v1 - v0) * k, u1, v1 );
			frame( uv );
		}
		y = baseY + (SIZE - r);
	}

	@Override
	public void update(){
		super.update();
		//slid with its layer (the overworld's window): its place goes too
		baseX += x - placedX;
		baseY += y - placedY;
		t += Game.elapsed;
		switch (mode){
			case REVEAL:
				rows( SIZE / 4 * Math.min( 4, 1 + (int)(t / (REVEAL_FOR / 4)) ) );
				am = t < REVEAL_FOR ? alpha0 : alpha0 * Math.max( 0f, 1f - (t - REVEAL_FOR) / REVEAL_FADE );
				if (t >= REVEAL_FOR + REVEAL_FADE) end();
				break;
			case PART:
				rows( t < PART_FOR / 3 ? SIZE : t < 2 * PART_FOR / 3 ? SIZE * 3 / 4 : SIZE / 2 );
				am = Math.max( 0f, 1f - t / PART_FOR );
				if (t >= PART_FOR) end();
				break;
			case GROW:
			default:
				rows( SIZE / 4 * Math.min( 4, 1 + (int)(t / (GROW_FOR / 4)) ) );
				//a pixel's bounce as it reaches its height, then the real thing
				if (t >= GROW_FOR) y = baseY - 1;
				if (t >= GROW_FOR + BOUNCE_FOR) end();
				break;
		}
		x = placedX = baseX;
		placedY = y;
	}

	private void end(){
		if (then != null){
			Runnable r = then;
			then = null;
			r.run();
		}
		Fx.dispose( this );
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
