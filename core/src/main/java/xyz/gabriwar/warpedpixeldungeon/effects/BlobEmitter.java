/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Steps;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * A blob's particles: each tick, one from its factory for every cell of it the hero sees (or has
 * seen, for one always shown), thinning away as a cell runs out (under FADE_VOLUME) and where it
 * is thin (FxBudget.Prioritized.density: 0.3 of a cell of 1, all of one of 1000), thinned again by
 * the effects' budget (its factory's priority, P1 unless it says), placed by the effects' own
 * random numbers - the blob's own use() is called once, as ever, so the game's draws are unchanged.
 *
 * A factory sees more than where to put its particle if it asks: the cell it is for (cell(): its
 * volume, whether it lies at the cloud's edge, the drift down its volume, whether it lived last
 * tick; and the index it is handed is the tick's plus the cell, so a detail every few particles
 * no longer pulses through the whole cloud at once). It may answer to the cloud's life: a cell
 * dying away (Aftermath), the cloud welling up (Bloomable, at its thickest cell) and someone
 * walking through it (Kickable, the steps the sprites took, at most KICKS a check).
 *
 * A blob that is light (Blob.emissive) is drawn in the light layer; what it sheds as matter (its
 * smoke) goes to its companion among the gases (matter()).
 */
public class BlobEmitter extends Emitter {

	/** A factory whose cells' deaths it hears of: the cell held blob last tick and holds none now. */
	public interface Aftermath {
		void dying( int cell );
	}

	/** A factory that answers to the cloud welling up (from nothing, or by half again in a tick),
	 *  at its thickest cell. */
	public interface Bloomable {
		void bloom( int cell );
	}

	/** A factory that answers to someone walking through the cloud: the cell he stepped into and
	 *  his step, px. */
	public interface Kickable {
		void kick( int cell, float dx, float dy );
	}

	/** The cell a particle is being made for, as its factory may read it (cell()) during emit. */
	public static final class Cell {
		/** The cell, and its volume. */
		public int cell, cur;
		/** Whether one of its four neighbours holds none: the cloud's edge. */
		public boolean edge;
		/** The drift down its volume to its neighbours, px/s, within DRIFT each way. */
		public float dx, dy;
		/** Whether it held blob last tick. */
		public boolean wasActive;
	}

	//below this much blob left in a cell, its particles start to thin out
	private static final int FADE_VOLUME = 5;
	/** The most a cell's drift may be, px/s; the walk-through's check, s; its most kicks a check. */
	public static final float DRIFT = 3f, KICK_EVERY = 0.25f;
	public static final int KICKS = 6;

	//the one cell being emitted for (render thread)
	private static final Cell CELL = new Cell();

	private Blob blob;

	public RectF bound = new RectF(0, 0, 1, 1);

	//which cells held blob last tick (allocated once), and the area they lay in
	private byte[] was;
	private int wasLeft, wasTop, wasRight, wasBottom;
	//the volume last tick, for its welling up
	private int lastVolume;
	//the walk-through's clock, the last step it read, and the steps read into
	private float kickIn = KICK_EVERY;
	private long lastStep = Steps.last();
	private final int[] steps = new int[2 * KICKS];
	//its matter's companion among the gases, for a blob drawn as light
	private FxEmitter matter;

	public BlobEmitter( Blob blob ) {

		super();

		this.blob = blob;
		blob.use( this );
	}

	/** The cell being emitted for, read by a factory during its emit. Render thread. */
	public static Cell cell(){
		return CELL;
	}

	/** Where its matter goes: itself, or for a blob drawn as light (Blob.emissive) its companion
	 *  among the gases, made the first time it is asked for. */
	public Emitter matter(){
		if (!blob.emissive()) return this;
		if (matter == null){
			matter = new FxEmitter();
			matter.on = false;
			matter.autoKill = false;
		}
		return matter;
	}

	@Override
	protected void emit( int index ) {

		Level level = Dungeon.level;
		if (level == null || blob.cur == null) return;
		int w = level.width();

		if (was == null || was.length != level.length()) was = new byte[level.length()];
		dyingOutside( level );

		if (blob.volume <= 0) {
			lastVolume = 0;
			return;
		}

		if (blob.area.isEmpty())
			blob.setupArea();

		int[] map = blob.cur;
		float size = DungeonTilemap.SIZE;
		FxBudget.Prioritized prioritized = factory instanceof FxBudget.Prioritized ? (FxBudget.Prioritized) factory : null;
		int priority = prioritized != null ? prioritized.priority() : FxBudget.P1;
		Aftermath aftermath = factory instanceof Aftermath ? (Aftermath) factory : null;

		//welling up: from nothing, or by half again since the last tick
		if (factory instanceof Bloomable && (lastVolume == 0 || blob.volume >= lastVolume * 1.5f)) {
			int thickest = thickest( level );
			if (thickest != -1) ((Bloomable) factory).bloom( thickest );
		}
		lastVolume = blob.volume;

		int cell;
		for (int i = blob.area.left; i < blob.area.right; i++) {
			for (int j = blob.area.top; j < blob.area.bottom; j++) {
				cell = i + j*w;
				if (cell < 0 || cell >= map.length) continue;
				boolean visible = cell < level.heroFOV.length && level.heroFOV[cell];
				if (blob.alwaysVisible && cell < level.length()) {
					visible = visible || level.mapped[cell] || level.visited[cell];
				}
				int cur = map[cell];
				boolean wasActive = was[cell] != 0;
				was[cell] = (byte)(cur > 0 ? 1 : 0);
				if (cur <= 0) {
					if (wasActive && visible && aftermath != null) aftermath.dying( cell );
					continue;
				}
				if (!visible) continue;
				//a cell running out of blob emits less and less, so fire, frost
				//and the rest thin away instead of stopping between two frames
				if (cur < FADE_VOLUME && FxRandom.Float() * FADE_VOLUME > cur) {
					continue;
				}
				//and a thin one less than a thick one
				float density = prioritized != null ? prioritized.density( cur ) : FxBudget.blobDensity( cur );
				if (density < 1f && FxRandom.Float() >= density) {
					continue;
				}
				float x = (i + FxRandom.Float(bound.left, bound.right)) * size;
				float y = (j + FxRandom.Float(bound.top, bound.bottom)) * size;
				if (!FxBudget.keep( priority, x, y )) continue;
				describe( level, map, cell, cur, wasActive );
				factory.emit(this, index + cell, x, y);
			}
		}
		wasLeft = blob.area.left;
		wasTop = blob.area.top;
		wasRight = blob.area.right;
		wasBottom = blob.area.bottom;
	}

	//cells that held blob last tick outside the area it covers now: dying, and forgotten
	private void dyingOutside( Level level ){
		if (wasRight <= wasLeft || wasBottom <= wasTop) return;
		int w = level.width();
		Aftermath aftermath = factory instanceof Aftermath ? (Aftermath) factory : null;
		boolean empty = blob.volume <= 0 || blob.area.isEmpty();
		for (int i = wasLeft; i < wasRight; i++) {
			for (int j = wasTop; j < wasBottom; j++) {
				if (!empty && i >= blob.area.left && i < blob.area.right && j >= blob.area.top && j < blob.area.bottom) continue;
				int cell = i + j * w;
				if (cell < 0 || cell >= was.length || was[cell] == 0) continue;
				was[cell] = 0;
				if (aftermath != null && cell < level.heroFOV.length && level.heroFOV[cell]) aftermath.dying( cell );
			}
		}
		if (empty) wasRight = wasLeft;
	}

	//the thickest cell the hero sees, -1 for none
	private int thickest( Level level ){
		int w = level.width(), best = -1;
		int[] map = blob.cur;
		for (int i = blob.area.left; i < blob.area.right; i++) {
			for (int j = blob.area.top; j < blob.area.bottom; j++) {
				int cell = i + j * w;
				if (cell < 0 || cell >= map.length || map[cell] <= 0) continue;
				if (cell >= level.heroFOV.length || !level.heroFOV[cell]) continue;
				if (best == -1 || map[cell] > map[best]) best = cell;
			}
		}
		return best;
	}

	//the cell's context, for its factory to read
	private static void describe( Level level, int[] map, int cell, int cur, boolean wasActive ){
		int w = level.width();
		int l = side( level, map, cell, cell - 1, cur ), r = side( level, map, cell, cell + 1, cur );
		int u = side( level, map, cell, cell - w, cur ), d = side( level, map, cell, cell + w, cur );
		CELL.cell = cell;
		CELL.cur = cur;
		CELL.wasActive = wasActive;
		CELL.edge = l == 0 || r == 0 || u == 0 || d == 0;
		CELL.dx = drift( l, r );
		CELL.dy = drift( u, d );
	}

	//a neighbour's volume; a wall's (or the map's edge) counts as the cell's own, so nothing
	//drifts into it
	private static int side( Level level, int[] map, int cell, int n, int cur ){
		if (n < 0 || n >= map.length || level.solid == null || level.solid[n]) return cur;
		if (Math.abs( (n % level.width()) - (cell % level.width()) ) > 1) return cur;
		return map[n];
	}

	//down the volume from one side to the other, px/s
	private static float drift( int from, int to ){
		float sum = from + to;
		if (sum <= 0) return 0;
		return Math.max( -DRIFT, Math.min( DRIFT, DRIFT * (from - to) / sum ) );
	}

	@Override
	public void update() {
		super.update();
		if (!(factory instanceof Kickable) || isFrozen()) return;
		if ((kickIn -= Game.elapsed) > 0) return;
		kickIn = KICK_EVERY;
		Level level = Dungeon.level;
		if (level == null || blob.cur == null || blob.volume <= 0) {
			lastStep = Steps.last();
			return;
		}
		int n = Steps.seenAfter( lastStep, steps );
		lastStep = Steps.last();
		int w = level.width();
		for (int k = 0; k < n; k++) {
			int from = steps[2 * k], to = steps[2 * k + 1];
			if (to < 0 || to >= blob.cur.length || blob.cur[to] <= 0) continue;
			((Kickable) factory).kick( to,
					((to % w) - (from % w)) * DungeonTilemap.SIZE,
					((to / w) - (from / w)) * DungeonTilemap.SIZE );
		}
	}
}
