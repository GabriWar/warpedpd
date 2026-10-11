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

import com.watabou.noosa.particles.Emitter;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.BurstWatch;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * Steam: the kit's wisp - a painted curl that thins into a drifting wisp - in near white
 * (EEF2F4), never more than 0.4 opaque, rising 12-20 px/s and slowing, a third of an open sky's
 * wind, 1.2-2 s. FACTORY (net id "steam") is every steam in the game: a spring's, a vent's, a
 * steam cloud's, a dewdrop boiling off, a blade quenched. A burst of QUENCH or more at once (a
 * quench: hot iron into water, and a co-op guest's replay of it) is a jet: JET more wisps spat up
 * fast one after another over JET_FOR s, and four large puffs lingering for 2 s; at most 20 in
 * all (its 12, the jet's 4, the puffs' 4).
 */
public class SteamParticle extends FxParticle {

	/** Its colour, alpha at most and how fast it rises, px/s. */
	public static final int COLOR = 0xEEF2F4;
	public static final float ALPHA = 0.4f, RISE_MIN = 12f, RISE_MAX = 20f;
	/** A burst this big is a quench: its jet's wisps, their speed, how long it spits, the puffs. */
	public static final int QUENCH = 12, JET = 4, LINGER = 4;
	public static final float JET_MIN = 30f, JET_MAX = 45f, JET_FOR = 0.3f, LINGER_LIFE = 2f, LINGER_ALPHA = 0.3f;

	//the wisp's thin look, once its curl has opened
	private static final int[][] WISPS = FxFrames.EDGE_WISP;

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			if (index == 0) BurstWatch.open( new Quench(), emitter, x, y );
			else BurstWatch.count( Quench.class, emitter, x, y );
			if (!emitter.visible || !FxBudget.keep( FxBudget.P2, x, y )) return;
			wisp( Flames.make( emitter, SteamParticle.class, x, y ) );
		}
	};

	/** Sets `p` up as a wisp of steam. Matter, P2. */
	public static FxParticle wisp( FxParticle p ){
		float life = FxRandom.Float( 1.2f, 2f );
		return p.look( FxFrames.STEAM_CURL, 4 ).then( 0.6f, WISPS ).paint( COLOR ).smooth().life( life )
				.alpha( ALPHA, life * 0.2f, life * 0.5f ).vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( RISE_MIN, RISE_MAX ) )
				.drag( 0.35f ).wind( 0.35f ).priority( FxBudget.P2 );
	}

	/** A wisp of a quench's jet: shot straight up at JET_MIN-JET_MAX px/s (within 20 degrees),
	 *  braking hard as it opens. Matter, P2. */
	public static final Emitter.Factory JET_WISP = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			float a = (float)(-Math.PI / 2) + FxRandom.Float( -0.35f, 0.35f );
			wisp( Flames.make( emitter, SteamParticle.class, x, y ) ).polar( a, FxRandom.Float( JET_MIN, JET_MAX ) ).drag( 2.5f );
		}
	};

	/** A burst read as a quench when it is big enough: the jet, then the lingering puffs. */
	public static class Quench extends BurstWatch {
		@Override
		protected void play( int n, float x, float y ){
			if (n < QUENCH) return;
			FxEmitter air = Fx.air();
			if (air == null) return;
			//the jet spits its wisps one after another over JET_FOR s (the effects' own emitter:
			//none of the game's random numbers)
			FxEmitter jet = GameScene.fxEmitter();
			if (jet != null){
				jet.pos( x - 2, y, 4, 0 );
				jet.start( JET_WISP, JET_FOR / JET, JET );
			}
			for (int i = 0; i < LINGER; i++){
				if (!FxBudget.keep( FxBudget.P2, x, y )) continue;
				float life = LINGER_LIFE * FxRandom.Float( 0.9f, 1.1f );
				Flames.make( air, SteamParticle.class, x + FxRandom.Float( -6f, 6f ), y - FxRandom.Float( 8f, 14f ) )
						.look( FxFrames.PUFF_L[i % 2] ).paint( COLOR ).smooth().life( life )
						.alpha( LINGER_ALPHA, JET_FOR, life * 0.6f ).vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 3f, 6f ) )
						.drag( 0.3f ).turbulence( 1.5f, 2f ).wind( 0.35f ).priority( FxBudget.P2 );
			}
		}
	}
}
