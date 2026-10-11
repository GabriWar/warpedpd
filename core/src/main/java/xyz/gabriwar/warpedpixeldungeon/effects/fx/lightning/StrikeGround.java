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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * How the ground answers a lightning (the surfaces' rule): a dry floor keeps a faint scorch that
 * fades over SCORCH_FADE s (a major's or a storm's); the water lights up round where it struck
 * (washes of WASH_COLOR through its cells within one, a major's two, the nearest WASHES of them),
 * rings out (an M ring) and its fish dart off (a disturbance of DISTURB px); and each step of a
 * charge running over the water lights a cyan ring and a glint as the run gets there. Where it
 * lands is its cell's ground: the cell's middle GROUND px down, whatever it struck in the air
 * over it (a sprite's middle). Render thread; nothing off the game's scene.
 */
public final class StrikeGround {

	private StrikeGround(){}

	public static final int WASH_COLOR = 0x9CC8FF, WASHES = 12;
	public static final float WASH_ALPHA = 0.3f, WASH_LIFE = 0.4f, DISTURB = 96f;
	public static final float SCORCH_ALPHA = 0.4f, SCORCH_FADE = 3f;
	/** The run's step: its ring's colour and alpha (added: light on the water), its glint's. */
	public static final int STEP_RING = 0x9CE0FF, STEP_GLINT = 0xBFE6FF;
	public static final float STEP_RING_ALPHA = 0.6f, STEP_DISTURB = 24f;
	/** How far under a cell's middle its ground lies, px. */
	public static final float GROUND = 2f;

	/** The cell a point of the world strikes (-1 off the level). */
	public static int cell( float x, float y ){
		return Fx.cellAt( x, y );
	}

	/** Its cell's ground, world y (the point's own y off the level). */
	public static float groundY( float x, float y ){
		int c = cell( x, y );
		Level level = Dungeon.level;
		if (c < 0 || level == null) return y + GROUND;
		return (c / level.width() + 0.5f) * DungeonTilemap.SIZE + GROUND;
	}

	/** Whether the water lies under a world point. */
	public static boolean water( float x, float y ){
		Level level = Dungeon.level;
		int c = cell( x, y );
		return level != null && c >= 0 && c < level.length() && level.water != null && level.water[c];
	}

	/**
	 * The ground's answer where a lightning struck (x, y in the air over it): on the water its
	 * washes, its ring and its fish darting off; on a dry floor a scorch, if `scorch`. Whether it
	 * struck the water.
	 */
	public static boolean struck( float x, float y, boolean major, boolean scorch ){
		Level level = Dungeon.level;
		int c = cell( x, y );
		if (level == null || c < 0 || c >= level.length() || GameScene.layer( GameScene.Layer.FLOOR ) == null) return false;
		float gy = groundY( x, y );
		if (level.water[c]){
			washes( level, c, major ? 2 : 1 );
			WaterFX.ring( x, gy, WaterFX.M );
			WaterFX.disturb( x, gy, DISTURB, false );
			return true;
		}
		if (scorch && !level.solid[c] && !level.pit[c]){
			FxDecal.at( Math.round( x ), Math.round( gy ), FxFrames.SCORCH_S, FxRecipes.SCORCH_COLOR, SCORCH_ALPHA,
					0f, SCORCH_FADE, FxRandom.chance( 0.5f ) );
		}
		return false;
	}

	//washes through the water cells within r of c, nearest first, WASHES at most
	private static void washes( Level level, int c, int r ){
		int w = level.width(), cx = c % w, cy = c / w, n = 0;
		for (int ring = 0; ring <= r && n < WASHES; ring++){
			for (int dy = -ring; dy <= ring && n < WASHES; dy++){
				for (int dx = -ring; dx <= ring && n < WASHES; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != ring) continue;
					int x = cx + dx, y = cy + dy;
					if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
					int cell = x + y * w;
					if (!level.water[cell]) continue;
					WaterFX.wash( cell, WASH_COLOR, WASH_ALPHA, WASH_LIFE );
					n++;
				}
			}
		}
	}

	/** A step of a charge running over the water lit at (x, y): a cyan ring of light on the
	 *  surface and a glint over it, one each. Nothing off the water. */
	public static void step( float x, float y ){
		if (!water( x, y )) return;
		float gy = groundY( x, y );
		if (FxRing.live() < FxBudget.ringCap()){
			Ripple r = GameScene.recycle( GameScene.Layer.SURFACE, Ripple.class );
			if (r != null) r.reset( x, gy, WaterFX.S, STEP_RING, STEP_RING_ALPHA, true );
		}
		WaterFX.disturb( x, gy, STEP_DISTURB, false );
		FxEmitter glow = Fx.glowAir();
		if (glow != null) FxFactories.glint( STEP_GLINT ).emit( glow, 0, x, gy - 1f );
	}
}
