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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A light a character wears while a state lasts (a burning creature's, a torch's, an aura's): a
 * kit light (FxLight) kept to the sprite, its level set every frame (0 while the sprite is
 * hidden), handed back when the state ends.
 *
 * The kit's lights are pooled and capped: past the cap the dimmest makes way, and a light that
 * made way may be taken up again at once by some other effect. So a held light is checked every
 * frame - still alive and still where this sprite would put it - and let go of (never touched
 * again) the moment it is not. A held light is only ever asked for while the kit has room under
 * its cap, so one never makes another (a held one, a fire's) make way; without room it waits,
 * asking again every RETRY seconds, and one taken up again after it made way fades back in over
 * BACK_IN s instead of popping on. Lights of states are transient to the kit (a sprite's light
 * shows only where the hero sees it) but keep a persistent light's share by day
 * (Fx.DAY_PERSISTENT), so they never take the fires' places.
 */
final class HeldLight {

	/** How long a light that made way (or found no room) waits before it is asked for again. */
	static final float RETRY = 0.5f;
	/** A light taken up again after it made way fades back in from BACK_FROM of its level over
	 *  BACK_IN s. */
	static final float BACK_IN = 0.2f, BACK_FROM = 0.2f;
	//how far a held light may stray from where this sprite puts it and still be ours, px: more
	//than a sprite moves in a slow frame, less than the cell a foreign light would stand on
	private static final float MINE = 24f;

	private final boolean pool;
	private final int size;
	private int color;
	private final int flicker;
	private final boolean reflect;
	private final float dayShare;
	private FxLight light;
	//its colour as it was made: a light set up afresh by another owner takes theirs
	private float madeR, madeG, madeB;
	private float dx, dy;
	private float retryIn;
	//1 once it is fully in; under 1 while one taken up again fades back in
	private float in = 1f;
	private boolean lost;

	HeldLight( boolean pool, int size, int color, int flicker, boolean reflect ){
		this( pool, size, color, flicker, reflect, Fx.DAY_PERSISTENT );
	}

	HeldLight( boolean pool, int size, int color, int flicker, boolean reflect, float dayShare ){
		this.pool = pool;
		this.size = size;
		this.color = color & 0xFFFFFF;
		this.flicker = flicker;
		this.reflect = reflect;
		this.dayShare = dayShare;
	}

	/**
	 * Once a frame (render thread): kept (dx, dy) off the sprite's middle at `level` (0 hides it
	 * without letting it go); made, or made again after it made way.
	 */
	void update( CharSprite s, float dx, float dy, float level, float dt ){
		this.dx = dx;
		this.dy = dy;
		if (light != null && !mine( s )){
			light = null;
			lost = true;
			retryIn = RETRY;
		}
		if (light == null){
			if ((retryIn -= dt) > 0 || level <= 0f) return;
			//only into room under the cap: a held light never makes another make way
			if (FxLight.live() >= FxBudget.lightCap()){
				retryIn = RETRY;
				return;
			}
			float cx = s.x + s.width() / 2f + dx, cy = s.y + s.height() / 2f + dy;
			in = lost ? BACK_FROM : 1f;
			lost = false;
			float l = level * in;
			light = pool ? FxLight.pool( cx, cy, size, color, l ) : FxLight.glow( cx, cy, size, color, l );
			light.flicker( flicker ).day( dayShare ).follow( s, dx, dy );
			if (reflect) light.reflect();
			madeR = light.rm;
			madeG = light.gm;
			madeB = light.bm;
		} else if (in < 1f){
			in = Math.min( 1f, in + dt * (1f - BACK_FROM) / BACK_IN );
		}
		light.follow( s, dx, dy );
		light.level( s.visible ? level * in : 0f );
	}

	/** Its colour from now on (the next light it is given takes it; an aura's changes rarely). */
	void color( int c ){
		color = c & 0xFFFFFF;
	}

	//still the light this holder was given: alive, never made persistent, in its colour and about
	//where the sprite would put it (a light taken up again is set up afresh by its new owner)
	private boolean mine( CharSprite s ){
		FxLight l = light;
		if (!l.alive || l.isPersistent()) return false;
		if (l.rm != madeR || l.gm != madeG || l.bm != madeB) return false;
		float ex = s.x + s.width() / 2f + dx, ey = s.y + s.height() / 2f + dy;
		return Math.abs( l.centerX() - ex ) <= MINE && Math.abs( l.centerY() - ey ) <= MINE;
	}

	/** Whether it holds a light now. */
	boolean held(){
		return light != null && light.alive;
	}

	/** The light held, null for none (tests). */
	FxLight light(){
		return light;
	}

	/** Put out over `fade` s and let go of. Any thread. */
	void putOut( float fade ){
		FxLight l = light;
		light = null;
		if (l != null && l.alive) l.putOut( fade );
	}
}
