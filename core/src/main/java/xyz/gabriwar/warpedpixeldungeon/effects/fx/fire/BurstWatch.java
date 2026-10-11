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

import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

/**
 * A burst of an old factory read as one event: a factory that used to make n separate particles
 * (a bomb's 30 blast particles, a sacrifice's 20 blue flames) now opens a watch on its first
 * particle (index 0) and counts every other one the same emitter makes before the watch's first
 * frame - an emitter's burst makes all of its particles in one update, so by then the count is the
 * burst's size - and the watch then plays one event sized by it, at the middle of where they were
 * asked for. The trigger is the factory's own burst, so a co-op guest replaying it sees the same.
 *
 * A gizmo in the light layer; at most OPEN watches open at once. One more (a shrapnel bomb bursts
 * on every cell it reaches in one frame) plays the oldest open one at once to take its place: that
 * burst is whole already (an emitter makes all of a burst in one update, and a new one is starting),
 * so no burst is ever dropped. Render thread.
 */
public abstract class BurstWatch extends Gizmo {

	/** Watches open at once, at most. */
	public static final int OPEN = 16;
	private static final BurstWatch[] open = new BurstWatch[OPEN];

	//the order watches were opened in: the oldest makes way
	private static long opened;

	private Emitter from;
	/** Particles asked for, and the sums of where. */
	protected int n;
	protected float sumX, sumY;
	private boolean done;
	private long order;

	/** Opens a watch on an emitter's burst, its first particle at (x, y); false off the game's
	 *  scene. With OPEN open already, the oldest plays now and this one takes its place. */
	public static boolean open( BurstWatch w, Emitter from, float x, float y ){
		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		if (lights == null) return false;
		//a watch played early may open another as it plays (a blast's steam), so look again
		for (;;){
			int oldest = -1;
			for (int i = 0; i < OPEN; i++){
				if (open[i] == null){
					open[i] = w;
					w.from = from;
					w.n = 1;
					w.sumX = x;
					w.sumY = y;
					w.order = opened++;
					lights.add( w );
					return true;
				}
				if (oldest == -1 || open[i].order < open[oldest].order) oldest = i;
			}
			open[oldest].finish();
		}
	}

	/** A later particle of an emitter's burst, at (x, y): counted by its watch of this kind. True if
	 *  a watch of that kind was open for it. */
	public static boolean count( Class<? extends BurstWatch> kind, Emitter from, float x, float y ){
		for (int i = 0; i < OPEN; i++){
			BurstWatch w = open[i];
			if (w != null && w.from == from && w.getClass() == kind){
				w.n++;
				w.sumX += x;
				w.sumY += y;
				return true;
			}
		}
		return false;
	}

	/** A new scene: no watch left open. */
	public static void reset(){
		for (int i = 0; i < OPEN; i++) open[i] = null;
	}

	//off the open list: counted out
	private void close(){
		for (int i = 0; i < OPEN; i++) if (open[i] == this) open[i] = null;
		from = null;
	}

	@Override
	public void update(){
		super.update();
		finish();
	}

	//counted out: off the open list, played once, gone
	private void finish(){
		if (done) return;
		done = true;
		close();
		play( n, sumX / n, sumY / n );
		killAndErase();
	}

	/** The burst counted: n particles asked for about (x, y). */
	protected abstract void play( int n, float x, float y );
}
