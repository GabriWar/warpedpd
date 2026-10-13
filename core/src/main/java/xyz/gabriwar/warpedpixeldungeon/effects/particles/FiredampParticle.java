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


package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;

/**
 * Firedamp's haze (levels/overworld/HazardWatch): sulphur-yellow wisps that hang in the foul air
 * of a deep cave, drifting and drawing out as they go, laid on every gas cell about the hero
 * each turn he is in it, so he can see where it lies. Yellower than toxic gas's green, warmer
 * than the grey of mist, and soft and moving where the caves' moss is hard spots on the floor.
 *
 * The kit's painted gas (effects/fx): two in three a thin wisp drawn out sideways (EDGE_WISP), one
 * in three a body of it growing from small to large halfway through (GAS_S to GAS_L: a bigger
 * painted frame, never a stretch), stepping from the fresh yellow to a dull olive as it thins,
 * never more than ALPHA opaque, drifting 1-4 px/s and wavering, 0.15 of an open sky's wind (a cave
 * has none), 2.5-4 s; hidden where the hero does not see its cell. Matter, P1: it shows the player
 * where the gas lies. FACTORY keeps its object and its net id ("firedamp").
 */
public class FiredampParticle extends FxParticle {

	/** Its colours as it thins, and its alpha at most. */
	public static final Ramp RAMP = new Ramp( 0xD4D06C, 0xBCC45C, 0x9A9C4A );
	public static final float ALPHA = 0.45f;

	//a body's two sizes, one frame each (a matter frame grows by stepping to a bigger one)
	private static final int[][][] SMALL = { { FxFrames.GAS_S[0] }, { FxFrames.GAS_S[1] } };
	private static final int[][][] LARGE = { { FxFrames.GAS_L[0] }, { FxFrames.GAS_L[1] } };

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			if (!emitter.visible || !FxBudget.keep( FxBudget.P1, x, y )) return;
			haze( Flames.make( emitter, FiredampParticle.class, x, y ) ).wind( 0.15f ).source( x, y );
		}
	};

	/** Sets `p` up as a wisp or a body of the haze, on no level: no wind, seen wherever it is (the
	 *  Descent Guide's). FACTORY adds the cave's draught and hides it where the hero does not see. */
	public static FxParticle haze( FxParticle p ){
		float life = FxRandom.Float( 2.5f, 4f );
		if (FxRandom.Int( 3 ) == 0){
			int v = FxRandom.Int( 2 );
			p.look( SMALL[v], 0 ).then( 0.5f, LARGE[v] );
		} else {
			p.look( FxFrames.EDGE_WISP[FxRandom.Int( FxFrames.EDGE_WISP.length )] );
		}
		return p.ramp( RAMP, false ).smooth().life( life ).alpha( ALPHA, life * 0.3f, life * 0.4f )
				.vel( FxRandom.Float( -4f, 4f ), -FxRandom.Float( 1f, 3f ) ).drag( 0.1f )
				.turbulence( FxRandom.Float( 1f, 2f ), FxRandom.Float( 1.5f, 3f ) ).priority( FxBudget.P1 );
	}
}
