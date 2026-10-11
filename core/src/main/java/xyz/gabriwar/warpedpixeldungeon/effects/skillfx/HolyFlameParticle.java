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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;

/**
 * White fire: Scouring Flame's tongues, the kit's flame model in the holy palette (white at its
 * root, pale gold, honey at its tips), drawn as light over the night's tint whichever emitter
 * bursts it (its particles are made in a carrier of the light layer, Fx.lightCarrier). Registered
 * with NetVisuals as "holy_flame", so a guest sees the same bursts.
 */
public final class HolyFlameParticle {

	private HolyFlameParticle(){}

	//the light layer's carrier for these flames: a key of their own
	private static final class Carried extends Gizmo {}

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			Emitter host = Fx.lightCarrier( Carried.class );
			FxFactories.tongue( Element.HOLY ).emit( host != null ? host : emitter, index, x, y );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};
}
