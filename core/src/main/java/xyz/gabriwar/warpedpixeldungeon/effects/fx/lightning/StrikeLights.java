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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;

/**
 * The pools of light lightning throws on the ground (FxLight, in the light layer), and who drives
 * each: a lightning sets its pool's level every frame from its halo. A creature's zap striking
 * within NEAR px of another's in the last FOR s lights that pool again, at AGAIN of its level,
 * rather than laying a second one beside it (a DM-100 zapping three times a second is one
 * flickering pool, not a smear). Render thread.
 */
public final class StrikeLights {

	private StrikeLights(){}

	public static final float NEAR = 24f, FOR = 0.35f, AGAIN = 0.7f;
	//the pools remembered: one rich major lays up to 14 (6 where it strikes, 8 along its joints),
	//and a pool lit again must stay remembered while both its lightnings last, or both drive it
	static final int RECENT = 24;

	private static final FxLight[] lights = new FxLight[RECENT];
	private static final Object[] owners = new Object[RECENT];
	private static final float[] xs = new float[RECENT], ys = new float[RECENT], ats = new float[RECENT];
	private static int next;

	/** A new scene: none remembered. */
	public static synchronized void reset(){
		for (int i = 0; i < RECENT; i++){
			lights[i] = null;
			owners[i] = null;
		}
		next = 0;
	}

	/** Whether a pool of light can be laid now: on the game's scene. */
	public static boolean canLight(){
		return GameScene.layer( GameScene.Layer.LIGHTS ) != null;
	}

	/**
	 * A minor strike's pool at (x, y), `size` px wide: a recent one within NEAR px lit again (its
	 * life restarted) or a new one at `level`, and from now on driven by `owner`; null off the
	 * game's scene. Whether it was lit again: relit(owner).
	 */
	public static synchronized FxLight minor( Object owner, float x, float y, int size, int color, float level, float life, float release ){
		relit = null;
		if (!canLight()) return null;
		float now = Game.timeTotal;
		for (int i = 0; i < RECENT; i++){
			if (!still( i ) || now - ats[i] > FOR) continue;
			FxLight l = lights[i];
			if (Math.abs( xs[i] - x ) <= NEAR && Math.abs( ys[i] - y ) <= NEAR){
				l.life( 0f, life, release );
				owners[i] = owner;
				ats[i] = now;
				relit = owner;
				return l;
			}
		}
		FxLight l = FxLight.pool( x, y, size, color, level ).life( 0f, life, release );
		remember( owner, l, x, y, now );
		return l;
	}

	//whether the pool remembered at i is still that pool: alive, where it was laid, not made since
	//into another light (one that made way for another may be handed out again as it)
	private static boolean still( int i ){
		FxLight l = lights[i];
		return l != null && l.alive && l.parent != null && !l.isPersistent()
				&& l.centerX() == xs[i] && l.centerY() == ys[i];
	}

	//the last owner to light a pool again, for relit()
	private static Object relit;

	/** Whether the pool `owner` last asked for was one lit again. */
	public static synchronized boolean relit( Object owner ){
		return relit == owner;
	}

	/** A pool any lightning drives on its own (a major's, a storm's), at `level`: remembered for
	 *  the zaps that strike near it after. */
	public static synchronized FxLight pool( Object owner, float x, float y, int size, int color, float level, float life, float release ){
		if (!canLight()) return null;
		FxLight l = FxLight.pool( x, y, size, color, level ).life( 0f, life, release );
		remember( owner, l, x, y, Game.timeTotal );
		return l;
	}

	//a light handed out anew is its new pool's alone: what was remembered of it before is forgotten
	private static void remember( Object owner, FxLight l, float x, float y, float now ){
		for (int i = 0; i < RECENT; i++){
			if (lights[i] == l){
				lights[i] = null;
				owners[i] = null;
			}
		}
		lights[next] = l;
		owners[next] = owner;
		xs[next] = x;
		ys[next] = y;
		ats[next] = now;
		next = (next + 1) % RECENT;
	}

	/** Whether `owner` still drives this pool (none lit it again since). */
	public static synchronized boolean drives( Object owner, FxLight l ){
		for (int i = 0; i < RECENT; i++){
			if (lights[i] == l) return owners[i] == owner;
		}
		return true;
	}
}
