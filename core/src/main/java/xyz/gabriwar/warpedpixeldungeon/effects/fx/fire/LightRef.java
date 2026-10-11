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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

/**
 * A hold on a lasting light (a fire's, a torch's, a coal bed's) that cannot leak it: its holder
 * keeps it (keep, place) every frame or scan it is shown, and a watch in the light layer puts out
 * any light not kept for HOLD s (its holder gone, or out of sight) over FADE s; and a light the kit
 * took away (its cap gives a newer light the place of the dimmest) is noticed and let go, never
 * steered once it lights something else (a recycled light comes back in another colour or size,
 * or not alive).
 *
 * The light itself is given no life: renewing an FxLight's life restarts its flicker's clock, so a
 * light renewed every frame would never flicker (kit request: a renewal that keeps the clock).
 *
 * Render thread.
 */
public final class LightRef {

	/** How long a light lasts past its last keep, s, and how long it then takes to go. */
	public static final float HOLD = 0.6f, FADE = 0.3f;
	/** Holds watched at once, at most (a light past them is held as setLasting holds it). */
	public static final int WATCHED = 96;

	private static final LightRef[] watched = new LightRef[WATCHED];
	private static int watching;
	private static Watch watch;

	private FxLight light;
	private float rm, gm, bm, w, h;
	private boolean lasting;
	private float keptAt;
	private int slot = -1;

	/** A new scene: nothing watched (the last scene's lights went with it). */
	public static void reset(){
		for (int i = 0; i < WATCHED; i++){
			if (watched[i] != null) watched[i].slot = -1;
			watched[i] = null;
		}
		watching = 0;
		watch = null;
	}

	/** Holds watched now. */
	public static int watchedCount(){
		return watching;
	}

	/** Whether `lights` more lights, `persistent` of them persistent, fit under the kit's caps now
	 *  (FxLight.room) without taking another's place. A holder that asks
	 *  only then never puts out another holder's light to light its own, so a crowded scene on a
	 *  struggling phone keeps the lights it has instead of passing them round every few seconds. */
	public static boolean room( int lights, int persistent ){
		return FxLight.room( lights, persistent );
	}

	/** Holds `l` (null for none), kept from now: out HOLD s after its last keep. */
	public LightRef set( FxLight l ){
		take( l );
		lasting = false;
		if (l != null) watchMe();
		return this;
	}

	/** Holds `l` as it is, lasting until released (for a holder that is not updated every frame
	 *  but always puts its light out). */
	public LightRef setLasting( FxLight l ){
		take( l );
		lasting = true;
		unwatch();
		return this;
	}

	private void take( FxLight l ){
		light = l;
		if (l != null){
			rm = l.rm;
			gm = l.gm;
			bm = l.bm;
			w = l.width();
			h = l.height();
		}
		keptAt = Watch.clock;
	}

	/** The light held, or null if there is none or it is no longer this holder's. */
	public FxLight get(){
		FxLight l = light;
		if (l == null) return null;
		if (!l.alive || l.rm != rm || l.gm != gm || l.bm != bm || l.width() != w || l.height() != h){
			light = null;
			unwatch();
			return null;
		}
		return l;
	}

	public boolean held(){
		return get() != null;
	}

	/** Kept: it lasts another HOLD s. Returns whether it is still held. */
	public boolean keep(){
		if (get() == null) return false;
		keptAt = Watch.clock;
		return true;
	}

	/** Its middle moved to (x, y), its level set; kept. */
	public void place( float x, float y, float level ){
		FxLight l = get();
		if (l == null) return;
		//a move from outside is taken as a slide: its middle follows
		l.x += x - l.centerX();
		l.y += y - l.centerY();
		l.level( level );
		keptAt = Watch.clock;
	}

	/** Put out over `fade` s and let go. */
	public void release( float fade ){
		FxLight l = get();
		if (l != null) l.putOut( fade );
		light = null;
		unwatch();
	}

	// ------------------------------------------------------------------ the watch

	private void watchMe(){
		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		if (lights != null && (watch == null || watch.parent != lights || !watch.alive)){
			watch = new Watch();
			lights.add( watch );
		}
		if (slot != -1) return;
		for (int i = 0; i < WATCHED; i++){
			if (watched[i] == null){
				watched[i] = this;
				slot = i;
				watching++;
				return;
			}
		}
		//past the watch's room: held as a lasting light (its holders all put theirs out)
		lasting = true;
	}

	private void unwatch(){
		if (slot == -1) return;
		if (watched[slot] == this){
			watched[slot] = null;
			watching--;
		}
		slot = -1;
	}

	/** Puts out every held light not kept for HOLD s. Its clock stands while the game is frozen
	 *  (a window open), as its holders do. */
	static final class Watch extends Gizmo {

		static float clock;

		@Override
		public void update(){
			super.update();
			if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
			clock += Game.elapsed;
			for (int i = 0; i < WATCHED; i++){
				LightRef r = watched[i];
				if (r == null || r.lasting) continue;
				if (r.get() == null) continue;
				if (clock - r.keptAt > HOLD) r.release( FADE );
			}
		}
	}
}
