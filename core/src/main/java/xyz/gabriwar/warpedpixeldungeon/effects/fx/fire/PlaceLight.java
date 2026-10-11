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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import com.watabou.noosa.Game;

/**
 * The light of a place that burns for good (a prison's torch, the city's green flames and its
 * vents): a persistent glow in the air and, if asked, a pool on the ground under it (thrown down
 * any water below), flickering as told and shown as plainly as the hero sees the place (1 in
 * view, 0.8 explored). Only the places() places nearest the hero hold lights - PLACES, fewer on a
 * struggling phone; within NEAR cells, let go past FAR - so a level of forty torches never spends
 * the lights' budget, and a place lights only where the kit has room (LightRef.room), never
 * putting out another's light; a place's lights die by themselves within a second once it stops
 * being updated (out of sight), and its slot is free again.
 *
 * Its owner calls update() every frame it is shown. Render thread.
 */
public final class PlaceLight {

	/** Places holding lights at once, at most; within how many cells one takes them, past how
	 *  many it lets go. */
	public static final int PLACES = 6, NEAR = 7, FAR = 9;
	/** A place whose lights the kit took for newer ones waits this long before it asks again, s. */
	public static final float RETRY = 2f;

	private static final PlaceLight[] slots = new PlaceLight[PLACES];

	private final int cell;
	private final float gx, gy, px, py;
	private final int glowSize, poolSize, color, flicker;
	private final float level;
	private final LightRef glow = new LightRef(), pool = new LightRef();
	private float retryAt;

	/**
	 * A place's light: its cell, a glow `glowSize` px wide at (gx, gy) and (poolSize 0 for none) a
	 * pool at (px, py), in a colour at a level, flickering as FxLight's profile.
	 */
	public PlaceLight( int cell, float gx, float gy, int glowSize, float px, float py, int poolSize, int color, float level, int flicker ){
		this.cell = cell;
		this.gx = gx;
		this.gy = gy;
		this.px = px;
		this.py = py;
		this.glowSize = glowSize;
		this.poolSize = poolSize;
		this.color = color;
		this.level = level;
		this.flicker = flicker;
	}

	/** Places that may hold lights now: PLACES, or half the persistent lights when a struggling
	 *  phone has fewer (each place's glow is one). */
	public static int places(){
		return Math.min( PLACES, FxBudget.persistentLightCap() / 2 );
	}

	/** A new scene: no place holds a light. */
	public static void reset(){
		for (int i = 0; i < PLACES; i++) slots[i] = null;
	}

	/** Places holding lights now. */
	public static int holding(){
		int n = 0;
		for (PlaceLight p : slots) if (p != null && p.lit()) n++;
		return n;
	}

	/** Every frame its owner is shown: its lights taken, kept or let go by its distance. */
	public void update(){
		int d = distance();
		boolean lit = lit();
		if (lit){
			if (d > FAR || !slotted()){
				out();
				return;
			}
			glow.keep();
			pool.keep();
			return;
		}
		if (d > NEAR || Game.timeTotal < retryAt || !LightRef.room( poolSize > 0 ? 2 : 1, 1 ) || !claim()) return;
		retryAt = Game.timeTotal + RETRY;
		//the glow is the persistent one (seen as the place is); the pool, shown where the hero sees
		//its ground, keeps the persistent lights' count to one a place
		glow.set( FxLight.glow( gx, gy, glowSize, color, level ).flicker( flicker ).persistent() );
		if (poolSize > 0){
			pool.set( FxLight.pool( px, py, poolSize, color, level ).flicker( flicker ).day( Fx.DAY_PERSISTENT ).reflect() );
		}
	}

	/** Its lights put out (its place gone). */
	public void out(){
		glow.release( 0.3f );
		pool.release( 0.3f );
		for (int i = 0; i < PLACES; i++) if (slots[i] == this) slots[i] = null;
	}

	private boolean lit(){
		return glow.held() || pool.held();
	}

	private int distance(){
		if (Dungeon.hero == null || Dungeon.level == null || Dungeon.hero.pos < 0) return 0;
		int w = Dungeon.level.width(), h = Dungeon.hero.pos;
		return Math.max( Math.abs( cell % w - h % w ), Math.abs( cell / w - h / w ) );
	}

	//in one of the slots open now
	private boolean slotted(){
		int n = places();
		for (int i = 0; i < n; i++) if (slots[i] == this) return true;
		return false;
	}

	//a slot among those open now: its own, a free one, or one whose place's lights have died out
	private boolean claim(){
		int n = places();
		for (int i = 0; i < PLACES; i++){
			if (slots[i] != this) continue;
			if (i < n) return true;
			slots[i] = null;
		}
		for (int i = 0; i < n; i++){
			if (slots[i] == null || !slots[i].lit()){
				slots[i] = this;
				return true;
			}
		}
		return false;
	}
}
