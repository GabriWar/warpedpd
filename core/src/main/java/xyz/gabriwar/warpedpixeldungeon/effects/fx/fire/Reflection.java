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
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import com.watabou.noosa.Game;

/**
 * A light of the world's own (a lit window or a hearth, a cave's glowing fungus or crystal)
 * thrown down the water below it: where water lies within ROWS rows under its ground, a small
 * pool of its light lies there too, its broken streak running down the water (FxLight.reflect),
 * as bright as the light itself is lit. Nothing where there is no water. Only the lit() nearest
 * the hero have one - SLOTS, fewer on a struggling phone; within NEAR cells, let go past FAR - and
 * one lights only where the kit has room (LightRef.room), so a lakeside town of lit windows never
 * passes the persistent lights round. Its owner updates it every frame; render thread.
 */
public final class Reflection {

	/** How far below its ground water is looked for, rows. */
	public static final int ROWS = 3;
	/** Reflections lit at once, at most; within how many cells of the hero one lights, past how
	 *  many it goes out. */
	public static final int SLOTS = 4, NEAR = 7, FAR = 9;
	/** A reflection the kit took for a newer light waits this long before it asks again, s. */
	public static final float RETRY = 2f;

	private static final Reflection[] slots = new Reflection[SLOTS];

	private final LightRef light = new LightRef();
	private int checked = -2;
	private boolean water;
	private float retryAt;

	/** Reflections that may be lit now: SLOTS, or a quarter of the persistent lights when a
	 *  struggling phone has fewer. */
	public static int lit(){
		return Math.min( SLOTS, FxBudget.persistentLightCap() / 4 );
	}

	/** A new scene: none lit. */
	public static void reset(){
		for (int i = 0; i < SLOTS; i++) slots[i] = null;
	}

	/** Every frame: its light at (x, groundY) in a colour, as lit as `level` (0 for out). */
	public void update( float x, float groundY, int color, float level ){
		int cell = Fx.cellAt( x, groundY );
		if (cell != checked){
			checked = cell;
			water = waterBelow( cell );
		}
		if (!water || level < 0.01f){
			out();
			return;
		}
		int d = distance( cell );
		if (light.held()){
			if (d > FAR || !slotted()){
				out();
				return;
			}
			light.place( x, groundY, level );
		} else if (Game.timeTotal >= retryAt && d <= NEAR && LightRef.room( 1, 1 ) && claim()){
			retryAt = Game.timeTotal + RETRY;
			light.set( FxLight.pool( x, groundY, 15, color, level ).persistent().reflect() );
		}
	}

	/** Its owner gone: its light out. */
	public void out(){
		light.release( 0.3f );
		for (int i = 0; i < SLOTS; i++) if (slots[i] == this) slots[i] = null;
	}

	private int distance( int cell ){
		if (Dungeon.hero == null || Dungeon.level == null || Dungeon.hero.pos < 0 || cell < 0) return 0;
		int w = Dungeon.level.width(), h = Dungeon.hero.pos;
		return Math.max( Math.abs( cell % w - h % w ), Math.abs( cell / w - h / w ) );
	}

	//in one of the slots open now
	private boolean slotted(){
		int n = lit();
		for (int i = 0; i < n; i++) if (slots[i] == this) return true;
		return false;
	}

	//a slot among those open now: its own, a free one, or one whose light has gone
	private boolean claim(){
		int n = lit();
		for (int i = 0; i < SLOTS; i++){
			if (slots[i] != this) continue;
			if (i < n) return true;
			slots[i] = null;
		}
		for (int i = 0; i < n; i++){
			if (slots[i] == null || !slots[i].light.held()){
				slots[i] = this;
				return true;
			}
		}
		return false;
	}

	/** Whether water lies within ROWS rows below a cell. */
	public static boolean waterBelow( int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || level.water == null) return false;
		int w = level.width();
		for (int k = 1; k <= ROWS; k++){
			int c = cell + k * w;
			if (c >= level.length()) break;
			if (level.water[c]) return true;
		}
		return false;
	}
}
