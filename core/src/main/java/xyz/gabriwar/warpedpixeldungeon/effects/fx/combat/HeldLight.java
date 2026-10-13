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
 * frame - still alive and never set up again since it was handed over (FxLight.setups) - and let
 * go of (never touched again) the moment it is not. Where the light stands is no test of whose it
 * is: a sprite can be far from its light for a frame (a teleport, the world's window sliding, a
 * long frame), and a light of its own let go of then would follow it, lit, for good. A held light is only ever asked for while the kit has room under
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

	private final boolean pool;
	private final int size;
	private int color;
	private final int flicker;
	private final boolean reflect;
	private final float dayShare;
	private FxLight light;
	//the light's setups as it was handed over: one set up afresh since is another owner's
	private int setups;
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
		if (light != null && !mine( light )){
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
			setups = light.setups();
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

	//still the light this holder was given: alive, never made persistent, never set up again (a
	//light taken up again is set up afresh by its new owner), wherever it stands now
	private boolean mine( FxLight l ){
		return l.alive && !l.isPersistent() && l.setups() == setups;
	}

	/** Whether it holds a light now. */
	boolean held(){
		return light != null && light.alive;
	}

	/** The light held, null for none (tests). */
	FxLight light(){
		return light;
	}

	/** Put out over `fade` s and let go of (one taken up by another owner since is theirs, left
	 *  alone). Any thread. */
	void putOut( float fade ){
		FxLight l = light;
		light = null;
		if (l != null && mine( l )) l.putOut( fade );
	}
}
