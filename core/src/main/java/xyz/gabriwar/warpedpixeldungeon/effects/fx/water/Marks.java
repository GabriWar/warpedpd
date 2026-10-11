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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Group;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * The marks the water's life lays on its surface (the scene's surface slot, under the shore's
 * tiles, the night's tint and the fog): glints of light, lap foam, whitecaps, wakes, bubbles,
 * crust, each kind of them under a cap of its own. A mark is the kit's particle (FxParticle) in
 * one of two holders of the surface slot - one drawing added (light: glints), one laid on (matter:
 * the rest) - set up by its maker after make(), and counted by its kind while it lives.
 *
 * Caps (a phone's; the kit's x1.5 is not taken: these are few and on the water alone): glints 8,
 * foam 8, whitecaps 6, wakes 12, bubbles (lava's and the swamp's together) 12, crust 16, heat rays 8
 * (the lava's, in the air: makeIn), mist 6, slush 16, pouring pipes' foam 8. Render thread.
 */
public final class Marks {

	private Marks(){}

	public static final int GLINT = 0, FOAM = 1, WHITECAP = 2, WAKE = 3, BUBBLE = 4, CRUST = 5, MIST = 6,
			SLUSH = 7, HEAT = 8, POUR = 9;
	static final int KINDS = 10;
	/** At most this many of each kind at once. */
	public static final int[] CAPS = { 8, 8, 6, 12, 12, 16, 6, 16, 8, 8 };

	private static final AtomicIntegerArray live = new AtomicIntegerArray( KINDS );

	/** How many of a kind are out. */
	public static int live( int kind ){
		return live.get( kind );
	}

	/** Room for one more of a kind. */
	public static boolean room( int kind ){
		return live.get( kind ) < CAPS[kind];
	}

	/** A new scene: none of the last one's. */
	public static void reset(){
		for (int i = 0; i < KINDS; i++) live.set( i, 0 );
		light = matter = null;
	}

	//the scene's two holders in the surface slot: what is added, what is laid on
	private static FxEmitter light, matter;

	/** The surface slot's holder of light, or of matter; null off the game's scene. */
	static FxEmitter holder( boolean isLight ){
		FxEmitter e = isLight ? light : matter;
		if (e == null || !e.exists || e.parent == null){
			Group surface = GameScene.layer( GameScene.Layer.SURFACE );
			if (surface == null) return null;
			e = new FxEmitter();
			e.on = false;
			e.autoKill = false;
			if (isLight) e.holdLight();
			surface.add( e );
			if (isLight) light = e;
			else matter = e;
		}
		return e;
	}

	/**
	 * A mark of a kind at (x, y), as light or matter: null with its kind at its cap or off the
	 * scene. Its maker sets it up (look, colour, life, alpha...), as an FxParticle after reset;
	 * it is not mirrored unless flip() says so.
	 */
	public static Mark make( int kind, boolean isLight, float x, float y ){
		return makeIn( kind, holder( isLight ), isLight, x, y );
	}

	/** The same in a holder of the maker's (the air's: Fx.air(), Fx.glowAir()), counted under its
	 *  kind's cap all the same: the heat rising off the lava. */
	public static Mark makeIn( int kind, FxEmitter e, boolean isLight, float x, float y ){
		if (!room( kind ) || e == null) return null;
		Mark m = (Mark) e.recycle( Mark.class );
		m.reset( x, y );
		m.light( isLight );
		m.flipHorizontal = m.flipVertical = false;
		m.count( kind );
		return m;
	}

	/** A mark of the water: an FxParticle counted under its kind's cap, mirrored as it is told. */
	public static class Mark extends FxParticle {

		private int kind = -1;

		void count( int kind ){
			if (this.kind >= 0) live.decrementAndGet( this.kind );
			this.kind = kind;
			live.incrementAndGet( kind );
		}

		/** Mirrored left to right and top to bottom (a wake heading west, or north). */
		public Mark flip( boolean x, boolean y ){
			flipHorizontal = x;
			flipVertical = y;
			updateFrame();
			return this;
		}

		@Override
		public void kill(){
			super.kill();
			if (kind >= 0){
				live.decrementAndGet( kind );
				kind = -1;
			}
		}
	}
}
