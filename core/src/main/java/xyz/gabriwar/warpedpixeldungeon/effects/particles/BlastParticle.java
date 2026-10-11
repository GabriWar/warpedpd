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
import com.watabou.noosa.particles.Emitter.Factory;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Explosion;

/**
 * A blast. FACTORY (the same object, net id "blast") no longer throws squares: a burst of it is
 * read as one explosion sized by how many it asked for (effects/fx/fire/Explosion: a gun's
 * flash, a small blast, a bomb, DM-300's), so every blast that bursts it - and every co-op guest
 * replaying that burst - gets its core, ring, light, tongues, debris, smoke, embers and scorch.
 * This class is the explosion's own flame and flash.
 */
public class BlastParticle extends FxParticle {

	public static final Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			Explosion.burst( emitter, index, x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};
}
