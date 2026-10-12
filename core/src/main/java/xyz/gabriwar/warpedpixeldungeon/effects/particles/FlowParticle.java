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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterFalls;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;

public class FlowParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((FlowParticle)emitter.recycle( FlowParticle.class )).reset( x, y );
		}
	};
	
	public FlowParticle() {
		super();
		
		lifespan = 0.6f;
		acc.set( 0, 32 );
		angularSpeed = FxRandom.Float( -360, +360 );
	}
	
	public void reset( float x, float y ) {
		revive();
		
		left = lifespan;
		
		this.x = x;
		this.y = y;
		
		am = 0;
		size( 0 );
		speed.set( 0 );
	}
	
	@Override
	public void update() {
		super.update();
		
		float p = left / lifespan;
		am = (p < 0.5f ? p : 1 - p) * 0.6f;
		size( (1 - p) * 4 );
	}

	/**
	 * The water spilling over a pit's lip from the water cell above or beside it (Level.addVisuals): its
	 * foam along the lip, streaks falling into the pit, a droplet thrown off now and then, a mist
	 * sinking below (effects/fx/water/WaterFalls). Shown while the hero sees its water.
	 */
	public static class Flow extends FxEmitter {
		
		private final int pos;
		private final WaterFalls.Fall fall;
		
		public Flow( int pos, int side ) {
			super();
			
			this.pos = pos;
			fall = WaterFalls.fall( Dungeon.level, pos, side );
			add( fall.lip() );
			autoKill = false;
		}
		
		@Override
		public void update() {
			boolean seen = pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos];
			if (visible = seen) {
				super.update();
			}
			WaterFalls.fall( fall, this, seen, isFrozen() ? 0f : Game.elapsed );
		}
	}
}