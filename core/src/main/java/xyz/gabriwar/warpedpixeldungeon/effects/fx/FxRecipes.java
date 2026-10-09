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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.particles.Emitter;

/**
 * The effects' one vocabulary, ready made: an impact (a core, a ground ring, a burst and a pool of
 * light, motes lingering), a cast (motes gathering, a bloom), a local bloom standing in for a
 * screen flash, puffs, steam, a scorch smouldering, embers, sparks and dust. Every area builds
 * its own on these. Render thread; called from another, each hands itself there (Fx.post).
 */
public final class FxRecipes {

	private FxRecipes(){}

	/** An impact's numbers: its core's moment, its ring's widths and life, its burst at most, its
	 *  pool's life, its lingering motes' life. */
	public static final float CORE_FOR = 0.05f, RING_LIFE = 0.35f, POOL_FOR = 0.4f;
	public static final int RING_FROM = 9, RING_TO = 23, BURST = 12;

	/**
	 * Something strikes at (x, y): the brightest frame now (a flash core for 0.05 s, P0), a ring
	 * on the ground spreading 9 to 23 px (times `scale`) in the element's light, a burst of up to 12
	 * sparks (motes, for matter), a pool of its light fading over 0.4 s and two to four motes
	 * lingering 0.3-0.6 s.
	 */
	public static void impact( final float x, final float y, final Element e, final float scale ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> impact( x, y, e, scale ) );
			return;
		}
		FxEmitter glow = Fx.glowAir();
		if (glow != null){
			FxFactories.make( glow, x, y ).look( FxFrames.CORE_FLASH ).paint( e.glint ).light( true )
					.life( CORE_FOR * 2 ).alpha( 1f, 0, CORE_FOR ).snap().priority( FxBudget.P0 );
		}
		FxRing.ground( floor(), x, y + 2, RING_FROM, Math.round( RING_TO * scale ), RING_LIFE, e.light, 0.6f, true );
		Emitter burst = e.emissive ? GameScene.lightEmitter() : GameScene.fxEmitter();
		if (burst != null){
			burst.pos( x, y );
			burst.burst( e.emissive ? FxFactories.spark( e ) : FxFactories.mote( e.glint, false ),
					FxBudget.allow( Math.round( BURST * Math.min( 1f, scale ) ), FxBudget.P2 ) );
		}
		FxLight.pool( x, y + 4, 23, e.light, 1f ).life( 0, 0, POOL_FOR );
		linger( x, y, e, FxRandom.IntRange( 2, 4 ) );
	}

	//motes lingering where something struck
	private static void linger( float x, float y, Element e, int n ){
		FxEmitter air = e.emissive ? Fx.glowAir() : Fx.air();
		if (air == null) return;
		for (int i = 0; i < n; i++){
			if (!FxBudget.keep( FxBudget.P3, x, y )) continue;
			FxFactories.make( air, x + FxRandom.Float( -4f, 4f ), y + FxRandom.Float( -4f, 2f ) )
					.look( FxFrames.MOTE_1 ).paint( e.glint ).light( e.emissive ).life( FxRandom.Float( 0.3f, 0.6f ) )
					.alpha( 0.8f, 0, 0.2f ).vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -6f, -2f ) ).drag( 2f )
					.snap().priority( FxBudget.P3 );
		}
	}

	/** A cast: four motes gathering into (x, y) over 0.1 s, then a 15 px bloom of the element. */
	public static void castBloom( final float x, final float y, final Element e ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> castBloom( x, y, e ) );
			return;
		}
		FxEmitter glow = Fx.glowAir();
		if (glow != null){
			for (int i = 0; i < 4; i++){
				float a = FxRandom.angle(), d = FxRandom.Float( 6f, 9f );
				float sx = x + (float)Math.cos( a ) * d, sy = y + (float)Math.sin( a ) * d;
				FxFactories.make( glow, sx, sy ).look( FxFrames.MOTE_2 ).paint( e.glint ).light( true )
						.vel( (x - sx) / 0.1f, (y - sy) / 0.1f ).life( 0.1f ).alpha( 1f, 0.03f, 0.02f ).priority( FxBudget.P1 );
			}
		}
		FxLight.glow( x, y, 15, e.light, 1f ).life( 0.1f, 0.05f, 0.3f );
	}

	/**
	 * A bloom of light about (x, y), where a full-screen flash would once have been: a glow `size`
	 * px wide (past 63 a doubled one) up to `peak` over `life` s, a pool under it, and a ring of
	 * twelve motes thrown out.
	 */
	public static void bloom( final float x, final float y, final int color, final int size, final float peak, final float life ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> bloom( x, y, color, size, peak, life ) );
			return;
		}
		FxLight.glow( x, y, size, color, peak ).core().life( 0.05f, life * 0.2f, life * 0.8f );
		FxLight.pool( x, y + 6, Math.min( 63, size ), color, peak ).life( 0.05f, life * 0.2f, life * 0.8f );
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		int n = FxBudget.allow( 12, FxBudget.P2 );
		for (int i = 0; i < n; i++){
			float a = (float)(2 * Math.PI * i / 12) + FxRandom.Float( -0.2f, 0.2f );
			FxFactories.make( glow, x, y ).look( FxFrames.MOTE_2 ).paint( color ).light( true )
					.polar( a, FxRandom.Float( 30f, 50f ) ).drag( 3f ).life( FxRandom.Float( 0.4f, 0.7f ) )
					.alpha( 1f, 0, 0.3f ).priority( FxBudget.P2 );
		}
	}

	/** n puffs of the element's smoke rising from (x, y). */
	public static void puff( final float x, final float y, final Element e, final int n ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> puff( x, y, e, n ) );
			return;
		}
		Emitter em = GameScene.fxEmitter();
		if (em == null) return;
		em.pos( x - 4, y - 3, 8, 6 );
		em.burst( FxFactories.puff( e ), n );
	}

	/** n wisps of steam rising from (x, y). */
	public static void steam( final float x, final float y, final int n ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> steam( x, y, n ) );
			return;
		}
		Emitter em = GameScene.fxEmitter();
		if (em == null) return;
		em.pos( x - 4, y - 2, 8, 4 );
		em.burst( FxFactories.steam(), n );
	}

	/** Scorch decals by size: small, medium, large. */
	public static final int SMALL = 0, MEDIUM = 1, LARGE = 2;
	/** A scorch's colour and how long it lies. */
	public static final int SCORCH_COLOR = 0x2A1E16;
	public static final float SCORCH_HOLD = 6f, SCORCH_FADE = 3f;

	/** A scorch on the floor at (x, y) (SMALL, MEDIUM, LARGE) and two to four seconds of
	 *  smouldering specks rising off it. */
	public static void scorch( final float x, final float y, final int size ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> scorch( x, y, size ) );
			return;
		}
		int[] frame = size <= SMALL ? FxFrames.SCORCH_S : (size == MEDIUM ? FxFrames.SCORCH_M : FxFrames.SCORCH_L);
		FxDecal.at( x, y, frame, SCORCH_COLOR, 0.7f, SCORCH_HOLD, SCORCH_FADE, FxRandom.chance( 0.5f ) );
		Emitter em = GameScene.lightEmitter();
		if (em == null) return;
		em.pos( x - frame[2] / 3f, y - frame[3] / 3f, frame[2] * 2 / 3f, frame[3] * 2 / 3f );
		float smoulder = FxRandom.Float( 2f, 4f );
		em.start( FxFactories.ember( Element.FIRE ), 0.25f, Math.round( smoulder / 0.25f ) );
	}

	/** n embers of the element rising from (x, y). */
	public static void embers( final float x, final float y, final Element e, final int n ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> embers( x, y, e, n ) );
			return;
		}
		Emitter em = GameScene.lightEmitter();
		if (em == null) return;
		em.pos( x - 3, y - 3, 6, 6 );
		em.burst( FxFactories.ember( e ), n );
	}

	/** n sparks of the element thrown from (x, y). */
	public static void sparks( final float x, final float y, final Element e, final int n ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> sparks( x, y, e, n ) );
			return;
		}
		Emitter em = GameScene.lightEmitter();
		if (em == null) return;
		em.pos( x, y );
		em.burst( FxFactories.spark( e ), n );
	}

	/** Dust kicked up at (x, y): n earthen puffs and specks of grit. */
	public static void dust( final float x, final float y, final int n ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> dust( x, y, n ) );
			return;
		}
		Emitter em = GameScene.fxEmitter();
		if (em == null) return;
		em.pos( x - 6, y - 3, 12, 6 );
		em.burst( FxFactories.puff( Element.EARTH ), n );
		Emitter grit = GameScene.fxEmitter();
		if (grit == null) return;
		grit.pos( x - 4, y - 2, 8, 4 );
		grit.burst( FxFactories.debris( Element.EARTH.ramp.stop( 1 ), Element.EARTH.ramp.stop( 3 ) ), n );
	}

	//the floor's layer, for the ground rings
	private static com.watabou.noosa.Group floor(){
		return GameScene.layer( GameScene.Layer.FLOOR );
	}
}
