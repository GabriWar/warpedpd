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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;

/**
 * A spark, painted (the effects kit's FxParticle): a 1x2 streak turned to its way (a dot once it
 * slows under 15 px/s), thrown 30-70 px/s into the upper half, falling at 120 px/s^2 with drag
 * 1.2 and bouncing once 4-8 px under where it left (keeping 0.35; on the water it fizzles into a
 * small ring instead), its colour flowing down a ramp and ending in a one-frame twinkle (P3).
 *
 * FACTORY (the struck sparks of 69 callers: steel, FFFFFF to 50607A over 0.35-0.7 s), STATIC (a
 * still crackle: a dot, a plus, a dot over 0.25-0.4 s in the lightning's blues), ELECTRIC
 * (lightning's sparks, FFFFFF to 2E50C0 over 0.25-0.45 s) and METAL (a grinder's, white-hot to
 * 5A2010 over 0.4-0.8 s). The factories are the same objects as ever (FACTORY's NetVisuals id
 * "spark" too) and draw only from FxRandom. A spark burst from an ordinary layer's emitter is
 * recycled in the light layer instead (Fx.lightCarrier), so it reads over the night's tint, and
 * shows only where the hero sees it.
 */
public class SparkParticle extends FxParticle {

	//the ramps: steel (the struck spark), the lightning's own sparks, and hot metal
	static final Ramp STEEL = Element.STEEL.ember, ELECTRIC_RAMP = Element.ELECTRIC.ember, METAL_RAMP = Element.METAL.ember;
	//a still crackle: a dot, a plus, a dot, stepped over its life
	static final int[][] CRACKLE = { FxFrames.MOTE_1, FxFrames.PLUS_3, FxFrames.MOTE_1 };
	/** How fast a spark is thrown, px/s; its fall, its drag, how far under where it left it bounces
	 *  (px) and what of its fall a bounce keeps. */
	public static final float SPEED_MIN = 30f, SPEED_MAX = 70f, GRAVITY = 120f, DRAG = 1.2f;
	public static final float BOUNCE_MIN = 4f, BOUNCE_MAX = 8f, BOUNCE_KEEPS = 0.35f;
	//below this it is drawn as a dot
	static final float SLOW = 15f;

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P1 )) return;
			((SparkParticle)host( emitter ).recycle( SparkParticle.class )).resetSteel( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	public static final Emitter.Factory STATIC = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((SparkParticle)host( emitter ).recycle( SparkParticle.class )).resetStatic( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** The lightning's sparks: where it strikes, where its run lands, off a storm's bolt. P1. */
	public static final Emitter.Factory ELECTRIC = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P1 )) return;
			((SparkParticle)host( emitter ).recycle( SparkParticle.class )).resetElectric( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** Hot metal's sparks: longer lived, orange to brown, bouncing. P2. */
	public static final Emitter.Factory METAL = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((SparkParticle)host( emitter ).recycle( SparkParticle.class )).resetMetal( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/**
	 * Where a spark burst from this emitter lives: the light layer's carrier (over the night's
	 * tint) for an emitter of an ordinary layer of the game's scene, else the emitter itself (one
	 * already drawn over the tint, in a window, or off the scene). Render thread.
	 */
	static Emitter host( Emitter emitter ){
		if (overTheTint( emitter )) return emitter;
		Camera cam = emitter.camera();
		if (cam != null && cam != Camera.main) return emitter;
		Emitter carrier = Fx.lightCarrier( SparkParticle.class );
		return carrier != null ? carrier : emitter;
	}

	//whether the emitter already lies in the light layer or the weather's (a few parents up)
	private static boolean overTheTint( Emitter emitter ){
		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		if (lights == null) return true;
		Gizmo g = emitter;
		for (int i = 0; i < 4 && g != null; i++){
			if (g == lights || g instanceof WeatherOverlay) return true;
			g = g.parent;
		}
		return false;
	}

	//thrown into the upper half, falling, bouncing once (fizzling on the water), twinkling out
	private SparkParticle thrown( float x, float y, Ramp ramp, float life, int priority ){
		reset( x, y );
		look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, SLOW ).ramp( ramp, true )
				.life( life ).alpha( 1f, 0, 0.1f )
				.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( SPEED_MIN, SPEED_MAX ) )
				.gravity( GRAVITY ).drag( DRAG )
				.ground( y + FxRandom.Float( BOUNCE_MIN, BOUNCE_MAX ), RING, BOUNCE_KEEPS ).landDry( BOUNCE )
				.strength( 0.1f ).fate( FxFactories.SPARK_END ).priority( priority );
		return this;
	}

	/** The struck spark (FACTORY): steel, 0.35-0.7 s. */
	public void resetSteel( float x, float y ){
		thrown( x, y, STEEL, FxRandom.Float( 0.35f, 0.7f ), FxBudget.P1 );
		sourceIfCarried( x, y );
	}

	/** Lightning's spark (ELECTRIC): its blues, 0.25-0.45 s. */
	public void resetElectric( float x, float y ){
		thrown( x, y, ELECTRIC_RAMP, FxRandom.Float( 0.25f, 0.45f ), FxBudget.P1 );
		sourceIfCarried( x, y );
	}

	/** Hot metal's spark (METAL): 0.4-0.8 s. */
	public void resetMetal( float x, float y ){
		thrown( x, y, METAL_RAMP, FxRandom.Float( 0.4f, 0.8f ), FxBudget.P2 );
		sourceIfCarried( x, y );
	}

	/** A still crackle (STATIC): a dot, a plus, a dot, over 0.25-0.4 s, in the lightning's blues. */
	public void resetStatic( float x, float y ){
		reset( x, y );
		look( CRACKLE, 0 ).ramp( ELECTRIC_RAMP, true ).life( FxRandom.Float( 0.25f, 0.4f ) )
				.alpha( 1f, 0, 0.08f ).snap().priority( FxBudget.P2 );
		sourceIfCarried( x, y );
	}

	/** A crackle drawn toward a visual's middle at 48 px/s, set back an eighth of its way so it
	 *  stays near its cell (CavesBossLevel's energy). */
	public void resetAttracting( float x, float y, Visual attracting ){
		reset( x, y );
		float dx = (attracting.x + attracting.width / 2f) - x, dy = (attracting.y + attracting.height / 2f) - y;
		float len = (float)Math.sqrt( dx * dx + dy * dy );
		float speed = DungeonTilemap.SIZE * 3f;
		float vx = len > 0 ? dx / len * speed : 0, vy = len > 0 ? dy / len * speed : 0;
		look( FxFrames.SPARK_2 ).orient().ramp( ELECTRIC_RAMP, true ).life( FxRandom.Float( 0.2f, 0.35f ) )
				.alpha( 1f, 0, 0.08f ).at( x - vx / 8f, y - vy / 8f ).vel( vx, vy ).priority( FxBudget.P2 );
		sourceIfCarried( x, y );
	}

	/** Its frame by the size it is given: a dot (1), a streak (2), a plus (3 and over). */
	public void setMaxSize( float value ){
		look( value <= 1f ? FxFrames.MOTE_1 : (value <= 2f ? FxFrames.SPARK_2 : FxFrames.PLUS_3) );
	}

	//one carried to the light layer is shown only where the hero sees it (its emitter would have
	//been hidden with its sprite or under the fog)
	private void sourceIfCarried( float x, float y ){
		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		if (lights != null && parent != null && parent.parent == lights) source( x, y );
	}
}
