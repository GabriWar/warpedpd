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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

/**
 * The arcane area's small vocabulary on the kit: lights given as the alpha they show (rather
 * than a share of their cap), particles made in a holder, cores, motes, glints and rings that
 * know whether they land on water. Render thread; everything here is a no-op off the game's
 * scene (a window, a test without layers) rather than a picture in no group.
 */
public final class Arc {

	private Arc(){}

	/** How far below a bolt's middle (raisedTileCenterToWorld) its ground lies, px. */
	public static final float GROUND = 6f;

	/** Whether the game's scene (or a test's layers) is there to draw in. */
	public static boolean onScene(){
		return GameScene.layer( GameScene.Layer.LIGHTS ) != null;
	}

	/** A round glow showing `alpha` at most (its cap 0.55), transient; null off the scene. */
	public static FxLight glow( float x, float y, int size, int color, float alpha ){
		if (!onScene()) return null;
		return FxLight.glow( x, y, size, color, Math.min( 1f, alpha / FxLight.GLOW_CAP ) );
	}

	/** A 2:1 pool on the ground showing `alpha` at most (its cap 0.35), transient; null off the scene. */
	public static FxLight pool( float x, float y, int size, int color, float alpha ){
		if (!onScene()) return null;
		return FxLight.pool( x, y, size, color, Math.min( 1f, alpha / FxLight.POOL_CAP ) );
	}

	/** A kit particle from an emitter's pool, at (x, y). */
	public static FxParticle make( Emitter e, float x, float y ){
		return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y );
	}

	/** A loose particle of light (Fx.glowAir), or null off the scene or past the budget. */
	public static FxParticle light( float x, float y, int priority ){
		FxEmitter e = Fx.glowAir();
		if (e == null || !FxBudget.keep( priority, x, y )) return null;
		return make( e, x, y ).light( true ).priority( priority );
	}

	/** A loose particle of matter (Fx.air), or null off the scene or past the budget. */
	public static FxParticle matter( float x, float y, int priority ){
		FxEmitter e = Fx.air();
		if (e == null || !FxBudget.keep( priority, x, y )) return null;
		return make( e, x, y ).priority( priority );
	}

	/** The brightest frame of an impact: a flash core for 0.05 s, P0. */
	public static void core( float x, float y, int color ){
		FxParticle p = light( x, y, FxBudget.P0 );
		if (p != null){
			p.look( FxFrames.CORE_FLASH ).paint( color ).life( 0.1f ).alpha( 1f, 0, 0.05f ).snap();
		}
	}

	/** n motes of light thrown out from (x, y) at speedMin..speedMax px/s, slowing, rising a
	 *  little, living life..1.5 life. P2. */
	public static void motes( float x, float y, int n, int color, float speedMin, float speedMax, float life ){
		FxEmitter e = Fx.glowAir();
		if (e == null) return;
		n = FxBudget.allow( n, FxBudget.P2 );
		for (int i = 0; i < n; i++){
			FxParticle p = make( e, x, y ).light( true );
			float a = (float)(2 * Math.PI * i / Math.max( 1, n )) + FxRandom.Float( -0.3f, 0.3f );
			p.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( color )
					.polar( a, FxRandom.Float( speedMin, speedMax ) ).gravity( -6f ).drag( 3f )
					.life( life * FxRandom.Float( 1f, 1.5f ) ).alpha( 1f, 0, life * 0.5f ).priority( FxBudget.P2 );
		}
	}

	/** A glint (a dot, a plus, a star, a plus, a dot over 0.38 s) at (x, y). P3. */
	public static void glint( float x, float y, int color ){
		FxParticle p = light( x, y, FxBudget.P3 );
		if (p != null) p.look( GLINT, 0 ).paint( color ).life( 0.38f ).alpha( 1f, 0, 0.05f ).snap();
	}

	static final int[][] GLINT = { FxFrames.MOTE_1, FxFrames.PLUS_3, FxFrames.STAR4_5, FxFrames.PLUS_3, FxFrames.MOTE_1 };

	/**
	 * A ring where something lands at (x, groundY): on water one of the water's (S, M or L by
	 * width, mirrored, its far crest bright), else a ground ring from `fromW` to `toW` px wide over
	 * `life` s, added (light) or plain, its peak `alpha`.
	 */
	public static void ring( float x, float groundY, int fromW, int toW, float life, int color, float alpha, boolean light ){
		if (WaterFX.onWater( x, groundY )){
			WaterFX.ring( x, groundY, toW >= 31 ? WaterFX.L : (toW >= 17 ? WaterFX.M : WaterFX.S), color, Math.min( 0.8f, alpha + 0.2f ) );
			return;
		}
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		if (floor != null) FxRing.ground( floor, x, groundY, fromW, toW, life, color, alpha, light );
	}

	/** Only the ground's ring (a shock across the floor and the water alike). */
	public static void groundRing( float x, float groundY, int fromW, int toW, float life, int color, float alpha, boolean light ){
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		if (floor != null) FxRing.ground( floor, x, groundY, fromW, toW, life, color, alpha, light );
	}

	/** Two colours mixed, 0xRRGGBB. */
	public static int mix( int a, int b, float k ){
		return xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp.lerp( a, b, k );
	}
}
