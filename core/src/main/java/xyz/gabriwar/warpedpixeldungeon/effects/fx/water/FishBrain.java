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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;

/**
 * How the fish under the water make up their minds (docs/fx/water-life.md, "Fish and water life"):
 * what startles one and what draws it, how fast it bursts away and how it slows, where it turns
 * back from the bank, how it tilts. Pure, but for the water's disturbances (WaterFX), read once a
 * frame for every fish (disturbances).
 *
 * Startled by a disturbance within that disturbance's own reach (a ring's 24, 40 or 64 px), by the
 * hero within 32 px or anyone else within 24: it turns away (+-0.4 rad of straight away) and bursts
 * off at 50-60 px/s, slowing as exp(-2.5 t) to its own pace. Drawn by food (a drop, a leaf, an
 * insect, an item) within 40 px: it swims to it at half again its pace.
 */
public final class FishBrain {

	private FishBrain(){}

	/** How near the hero, or anyone else, may come before a fish is off, px. */
	public static final float HERO_PX = 32f, OTHERS_PX = 24f;
	/** Food draws it from this far, px; it has arrived within ARRIVED px. */
	public static final float FOOD_PX = 40f, ARRIVED = 3f;
	/** Its burst: how fast, how it dies away, how far from straight away it turns. */
	public static final float BURST_MIN = 50f, BURST_MAX = 60f, BURST_DECAY = 2.5f, AWAY_JITTER = 0.4f;
	/** It looks this far ahead for the bank, this often, px and s. */
	public static final float AHEAD = 8f, LOOK_EVERY = 0.1f;
	/** Its tilt, swimming up or down the screen: level, or this many degrees. */
	public static final int TILT = 20;

	/** Whether a disturbance of `radius` at (sx, sy) startles a fish at (x, y): food never does. */
	public static boolean startles( float x, float y, float sx, float sy, float radius, boolean food ){
		return !food && dist( x, y, sx, sy ) <= radius;
	}

	/** Whether food at (sx, sy) draws a fish at (x, y). */
	public static boolean draws( float x, float y, float sx, float sy, boolean food ){
		return food && dist( x, y, sx, sy ) <= FOOD_PX;
	}

	/** Whether someone at (px, py) is too near a fish at (x, y): the hero within 32, anyone 24. */
	public static boolean tooNear( float x, float y, float px, float py, boolean hero ){
		return dist( x, y, px, py ) <= (hero ? HERO_PX : OTHERS_PX);
	}

	/** Its burst's speed `t` s into it from `v0`, never under its own pace. */
	public static float burst( float v0, float t, float pace ){
		return Math.max( pace, v0 * (float)Math.exp( -BURST_DECAY * t ) );
	}

	/** Straight away from (sx, sy), radians, from (x, y); a way at random from right on top of it. */
	public static float away( float x, float y, float sx, float sy, float random ){
		float dx = x - sx, dy = y - sy;
		if (Math.abs( dx ) + Math.abs( dy ) < 0.5f) return random;
		return (float)Math.atan2( dy, dx );
	}

	/** Its tilt in degrees for a heading: level going across, TILT nose up or down going up or
	 *  down the screen, mirrored as it faces (on whole steps, never a smooth turn). */
	public static int tilt( float heading ){
		float s = (float)Math.sin( heading );
		if (Math.abs( s ) < 0.38f) return 0;
		boolean left = Math.cos( heading ) < 0;
		int t = s > 0 ? TILT : -TILT;
		return left ? -t : t;
	}

	/** Water whose body's middle is darker than this (luma, 0-255) is dark: the caves', the city's. */
	public static final int DARK_WATER = 60;
	/** What the dungeon's fish shows as under water with no deep of its own. */
	static final int DEEP = 0x2A4C3C;

	/** Whether a fish under this water must show pale: its deep would sink into a water this dark. */
	public static boolean darkWater( Element.Liquid l ){
		int c = l.body.stop( l.body.size() / 2 );
		int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
		return 0.2126f * r + 0.7152f * g + 0.0722f * b < DARK_WATER;
	}

	/** The colour a fish under this water shows as: on dark water a pale ghost in its glint (the
	 *  caves' C0ECE4, the city's D8C0C0), on lighter water a silhouette of its deep. */
	public static int shade( Element.Liquid l ){
		if (darkWater( l )) return l.glint;
		return l.deep != Element.NONE ? l.deep : DEEP;
	}

	public static float dist( float ax, float ay, float bx, float by ){
		float dx = ax - bx, dy = ay - by;
		return (float)Math.sqrt( dx * dx + dy * dy );
	}

	/** Whether a world point lies over liquid of the level (deep water too), the map's edge dry. */
	public static boolean wet( float x, float y ){
		Level level = Dungeon.level;
		if (level == null || level.water == null || x < 0 || y < 0) return false;
		int cx = (int)(x / DungeonTilemap.SIZE), cy = (int)(y / DungeonTilemap.SIZE);
		if (cx >= level.width() || cy >= level.height()) return false;
		return level.water[cx + cy * level.width()];
	}

	/** Open water a fish keeps to: liquid, and five of its eight neighbours too (no puddles, no
	 *  narrow edges); never the map's outer ring. */
	public static boolean openWater( Level level, int cell ){
		int w = level.width(), h = level.height();
		int x = cell % w, y = cell / w;
		if (x < 1 || y < 1 || x > w - 2 || y > h - 2 || !level.water[cell]) return false;
		int n = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && level.water[cell + dx + dy * w]) n++;
			}
		}
		return n >= 5;
	}

	// ------------------------------------------------------------------ the frame's disturbances

	private static final float[] bus = new float[WaterFX.BUS * 4];
	private static int busN;
	private static float busAt = -1f;

	/** This frame's disturbances (x, y, radius, food) for every fish, read from the water once a
	 *  frame; how many. The array is shared: read it, never keep it. */
	public static float[] disturbances(){
		if (Game.timeTotal != busAt){
			busAt = Game.timeTotal;
			busN = WaterFX.disturbances( bus );
		}
		return bus;
	}

	/** How many disturbances disturbances() holds this frame. */
	public static int disturbed(){
		disturbances();
		return busN;
	}
}
