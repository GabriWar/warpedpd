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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;

/**
 * A tongue of fire: the effects kit's flame (effects/fx/fire/Flames) in the fire's ramp, small,
 * middling or tall, rising, swaying and cooling from near white through yellow and orange into a
 * dying red. Made in the light layer (Flames.host), so a bed of them glows over the night's tint,
 * and hidden where the hero does not see the spot it rose from.
 *
 * Everything that burns pours these: the fire blob's kin, torches, firebombs, the blazing
 * enchantment, elementals and the rest, all through FACTORY (the same object, net id "flame").
 */
public class FlameParticle extends FxParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			Flames.pour( emitter, FlameParticle.class, x, y, Element.FIRE );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};
}
