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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

/**
 * Firedamp, seen (levels/overworld/HazardWatch): what the gas does as a flame licks at it and as
 * it goes up - visual only, its harm dealt already by the game.
 * - Arming (the turn the fuse burns): a warm glow creeping out along the gas cells from the flame,
 *   cell by cell (ARM_STEP s a step), rising to 0.25, two sparks spitting at each.
 * - Ignition: a front running out through the pocket breadth first (IGNITE_STEP s a step, the
 *   whole of it within IGNITE_MAX s), each cell flaring as it passes - a glow, three tongues, a
 *   puff of smoke - with one light riding the front, and the camera shaken (Fx.shake 3 at the
 *   hero, nothing past SHAKE_CELLS).
 * At most CELLS cells. From any thread (handed to the render thread); sent on to co-op guests
 * (FireNet), each shaken as far as his own hero stands from it.
 */
public class FiredampFront extends Gizmo {

	public static final int CELLS = 40;
	public static final float ARM_STEP = 0.08f, IGNITE_STEP = 0.035f, IGNITE_MAX = 0.5f;
	public static final int ARM_COLOR = 0xFFC060;
	public static final float ARM_LEVEL = 0.25f, CELL_GLOW = 0.6f, SHAKE = 3f, SHAKE_FOR = 0.6f, SHAKE_CELLS = 12f;

	private final int[] cells;
	private final int[] dist;
	private final boolean ignite;
	private final float step;
	private final boolean[] done;
	private int left;
	private float t;
	private final LightRef rider = new LightRef();

	FiredampFront( int[] cells, int[] dist, boolean ignite, float step ){
		this.cells = cells;
		this.dist = dist;
		this.ignite = ignite;
		this.step = step;
		done = new boolean[cells.length];
		left = cells.length;
	}

	/** A flame licking at the gas: the glow creeping along its cells from the first. */
	public static void arming( int[] pocket ){
		pocket = first( pocket );
		FireNet.send( FireNet.DAMP_ARMING, -1, 0, 0, pocket );
		start( pocket, false );
	}

	/** The gas going up: the front out from the first of its cells. */
	public static void ignition( int[] pocket ){
		pocket = first( pocket );
		FireNet.send( FireNet.DAMP_IGNITION, -1, 0, 0, pocket );
		start( pocket, true );
	}

	//the first CELLS of a pocket, all that is shown (and sent)
	private static int[] first( int[] pocket ){
		if (pocket == null || pocket.length <= CELLS) return pocket;
		int[] out = new int[CELLS];
		System.arraycopy( pocket, 0, out, 0, CELLS );
		return out;
	}

	static void start( final int[] pocket, final boolean ignite ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> start( pocket, ignite ) );
			return;
		}
		Level level = Dungeon.level;
		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		if (level == null || lights == null || pocket == null || pocket.length == 0) return;
		int n = Math.min( CELLS, pocket.length );
		int[] cells = new int[n];
		System.arraycopy( pocket, 0, cells, 0, n );
		int[] dist = distances( cells, level.width() );
		int far = 0;
		for (int d : dist) far = Math.max( far, d );
		float step = ignite ? (far * IGNITE_STEP > IGNITE_MAX ? IGNITE_MAX / far : IGNITE_STEP) : ARM_STEP;
		lights.add( new FiredampFront( cells, dist, ignite, step ) );
	}

	/** Steps from the first cell to each, through the others (8 ways), breadth first. */
	static int[] distances( int[] cells, int w ){
		int n = cells.length;
		int[] dist = new int[n];
		java.util.Arrays.fill( dist, -1 );
		int[] queue = new int[n];
		int head = 0, tail = 0;
		dist[0] = 0;
		queue[tail++] = 0;
		while (head < tail){
			int i = queue[head++];
			int c = cells[i];
			for (int j = 0; j < n; j++){
				if (dist[j] != -1) continue;
				int o = cells[j];
				if (Math.abs( o % w - c % w ) <= 1 && Math.abs( o / w - c / w ) <= 1){
					dist[j] = dist[i] + 1;
					queue[tail++] = j;
				}
			}
		}
		//a cell the pocket's own walk reached another way: as far as the farthest
		int far = 0;
		for (int d : dist) far = Math.max( far, d );
		for (int j = 0; j < n; j++) if (dist[j] == -1) dist[j] = far + 1;
		return dist;
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		Level level = Dungeon.level;
		if (level == null){
			killAndErase();
			return;
		}
		if (t == 0 && ignite) shake( level );
		t += Game.elapsed;
		int w = level.width();
		float sx = 0, sy = 0;
		int front = 0;
		for (int i = 0; i < cells.length; i++){
			if (done[i] || t < dist[i] * step) continue;
			done[i] = true;
			left--;
			float x = (cells[i] % w + 0.5f) * DungeonTilemap.SIZE, y = (cells[i] / w + 0.5f) * DungeonTilemap.SIZE;
			if (ignite) flare( x, y );
			else arm( x, y, dist[i] );
			sx += x;
			sy += y;
			front++;
		}
		if (ignite && front > 0) ride( sx / front, sy / front );
		if (left == 0){
			rider.release( 0.3f );
			killAndErase();
		}
	}

	//the creeping glow at a cell, and its sparks
	private void arm( float x, float y, int d ){
		if (parent == null) return;
		LightFrame g = new LightFrame();
		g.setup( FxFrames.Light.POOL_15, x, y + 3, ARM_COLOR, ARM_LEVEL ).life( 0.3f, 0.6f + 0.02f * d, 0.4f ).seenAt( x, y );
		parent.add( g );
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		for (int i = 0; i < 2; i++) FxFactories.spark( Element.FIRE ).emit( glow, i, x + FxRandom.Float( -4f, 4f ), y + FxRandom.Float( -2f, 4f ) );
	}

	//the front passing a cell: a glow, three tongues, a puff
	private void flare( float x, float y ){
		if (parent == null) return;
		LightFrame g = new LightFrame();
		g.setup( FxFrames.Light.GLOW_15, x, y - 2, Element.FIRE.light, CELL_GLOW ).life( 0, 0.05f, 0.3f ).seenAt( x, y );
		parent.add( g );
		FxEmitter glow = Fx.glowAir();
		if (glow != null){
			for (int i = 0; i < 3; i++){
				if (!FxBudget.keep( FxBudget.P1, x, y )) continue;
				Flames.tongue( Flames.make( glow, FxParticle.class, x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( -2f, 5f ) ),
						Element.FIRE, FxRandom.chance( 0.5f ) ? Flames.M : Flames.L ).source( x, y );
			}
		}
		FxEmitter air = Fx.air();
		if (air != null && FxBudget.keep( FxBudget.P2, x, y )){
			SmokeParticle.puff( Flames.make( air, SmokeParticle.class, x, y - 4 ), FxRandom.Float( -4f, 4f ), -FxRandom.Float( 8f, 12f ) );
		}
	}

	//one light riding the front
	private void ride( float x, float y ){
		if (!rider.held()){
			rider.set( FxLight.glow( x, y, 47, Element.FIRE.light, 0.9f ).flicker( FxLight.TORCH ) );
		} else {
			rider.place( x, y, 0.9f );
		}
	}

	private void shake( Level level ){
		if (Dungeon.hero == null || Dungeon.hero.pos < 0) return;
		int w = level.width(), h = Dungeon.hero.pos, c = cells[0];
		float d = (float)Math.sqrt( Math.pow( c % w - h % w, 2 ) + Math.pow( c / w - h / w, 2 ) );
		float k = 1f - d / SHAKE_CELLS;
		if (k > 0) Fx.shake( SHAKE * k, SHAKE_FOR );
	}
}
