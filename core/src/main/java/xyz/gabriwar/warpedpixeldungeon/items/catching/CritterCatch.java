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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.CatchFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.DungeonLife;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;

/**
 * The bug net's side of the small life (BugNet): what it may catch on a cell, and how often.
 *
 * The critters are pictures on the render thread, never saved (OverworldCritters on the surface,
 * DungeonLife below; the fireflies are particles of the light layer): at the tap the net asks each
 * for the small critter drawn on the cell (critterAt, a Target), and when the swing lands it is taken
 * into the net or it gets away. A critter is on a cell when the point it is drawn at (its feet, or
 * where it flies) lies in the cell's square, SLACK px either side.
 *
 * The thinning (thinned): in any one stretch of land (area: a square of AREA cells of a slice of the
 * world, or a whole floor of the dungeon) the first FREE critters netted in a day come at their own
 * chance; each one after is harder, and from the CAP-th on the rest keep clear of the net until the
 * next day (CatchWorld counts them).
 */
public final class CritterCatch {

	private CritterCatch(){}

	/** How far round a cell's square a critter still counts as on it, px. */
	public static final float SLACK = 3f;
	/** The stretch of land the thinning counts in, cells square (on the world's slices). */
	public static final int AREA = 64;
	/** Netted at their own chance; none at all from. */
	public static final int FREE = 3, CAP = 6;

	/** A small critter seen on a cell, as the net's target. Render thread. */
	public static abstract class Target {

		public final Critter critter;

		protected Target( Critter critter ){
			this.critter = critter;
		}

		/** Where it is drawn now, scene px. */
		public abstract float x();
		public abstract float y();

		/** Still there to be caught, and still on this cell? */
		public abstract boolean on( int cell );

		/** Caught: out of the picture, into the net (CatchFx.netted). */
		public abstract void take();

		/** Missed: off it goes, away from (sx, sy) scene px. */
		public abstract void spook( float sx, float sy );
	}

	/** Is the scene point (px, py) on the cell, SLACK px either side of its square? */
	public static boolean on( int cell, int width, float px, float py ){
		float x0 = (cell % width) * DungeonTilemap.SIZE, y0 = (cell / width) * DungeonTilemap.SIZE;
		return px >= x0 - SLACK && px < x0 + DungeonTilemap.SIZE + SLACK
				&& py >= y0 - SLACK && py < y0 + DungeonTilemap.SIZE + SLACK;
	}

	/** The small critter on a cell, the nearest its middle: the surface's, the dungeon's, or a firefly. Render thread. */
	public static Target at( int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return null;
		Target t = OverworldCritters.critterAt( cell );
		if (t == null) t = DungeonLife.critterAt( cell );
		if (t == null) t = firefly( level, cell );
		return t;
	}

	/** The chance to net a critter of `chance` after `netted` caught today in its stretch of land. */
	public static float thinned( float chance, int netted ){
		if (netted < FREE) return chance;
		if (netted >= CAP) return 0f;
		return chance * (CAP - netted) / (float)(CAP - FREE + 1);
	}

	/** The stretch of land a cell of the level is in, for the thinning. */
	public static long areaKey( Level level, int cell ){
		if (level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) level;
			int w = ow.width();
			return area( Dungeon.depth, 0, Math.floorDiv( ow.worldX() + cell % w, AREA ), Math.floorDiv( ow.worldY() + cell / w, AREA ) );
		}
		return area( Dungeon.depth, Dungeon.branch, 0, 0 );
	}

	/** A stretch of land as a key: a slice's square, or a floor (its depth and branch). */
	public static long area( int depth, int branch, int ax, int ay ){
		return ((long)(depth & 0xFFF) << 52) | ((long)(branch & 0xFFF) << 40)
				| ((long)(ax & 0xFFFFF) << 20) | (ay & 0xFFFFFL);
	}

	// ------------------------------------------------------------ the fireflies

	//a firefly lit or between its flashes on the cell, nearest its middle
	private static Target firefly( Level level, int cell ){
		Emitter carrier = Fx.lightCarrier( FireflyParticle.class );
		if (carrier == null) return null;
		int w = level.width();
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		FireflyParticle best = null;
		float bestD = Float.MAX_VALUE;
		for (Gizmo g : carrier.membersView()){
			if (!(g instanceof FireflyParticle) || !g.alive || !g.exists) continue;
			FireflyParticle p = (FireflyParticle) g;
			float px = p.x + p.origin.x, py = p.y + p.origin.y;
			if (!on( cell, w, px, py )) continue;
			float d = Math.max( Math.abs( px - cx ), Math.abs( py - cy ) );
			if (d < bestD){
				bestD = d;
				best = p;
			}
		}
		if (best == null) return null;
		final FireflyParticle fly = best;
		return new Target( Critter.FIREFLY ){
			@Override public float x(){ return fly.x + fly.origin.x; }
			@Override public float y(){ return fly.y + fly.origin.y; }
			@Override public boolean on( int c ){
				Level l = Dungeon.level;
				return fly.alive && fly.exists && l != null && CritterCatch.on( c, l.width(), x(), y() );
			}
			@Override public void take(){
				CatchFx.netted( x(), y(), critter );
				fly.kill();
			}
			//a firefly minds no net: it drifts on as it was
			@Override public void spook( float sx, float sy ){
			}
		};
	}
}
