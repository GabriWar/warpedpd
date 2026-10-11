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

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.BurstWatch;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.LightFrame;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * The sacrificial fire's blue flame: the kit's tongue (effects/fx/fire/Flames) down the
 * sacrificial ramp, made in the light layer and hidden out of the hero's sight; one in WISP_EVERY
 * sheds a dark blue wisp of its smoke, one in SPARK_EVERY a cold spark. A burst of SACRIFICE or
 * more at once (a worthy sacrifice: its 20, and every co-op guest's replay of it) draws six motes
 * spiralling into the flame over 0.3 s and then a column of blue light standing up out of it for
 * 0.4 s. FACTORY keeps its object and net id ("sacrificial").
 */
public class SacrificialParticle extends FxParticle {

	/** The wisp's colour, and one wisp, one spark in this many flames. */
	public static final int WISP = 0x2050B0, WISP_EVERY = 5, SPARK_EVERY = 8;
	/** A burst this big is a sacrifice. */
	public static final int SACRIFICE = 20;
	/** The motes' spiral in, s, and the column's light. */
	public static final float SPIRAL = 0.3f;
	public static final int COLUMN = 0x6AA8FF;

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (index == 0) BurstWatch.open( new Sacrifice(), emitter, x, y );
			else BurstWatch.count( Sacrifice.class, emitter, x, y );
			FxParticle p = Flames.pour( emitter, SacrificialParticle.class, x, y - 4, Element.SACRIFICIAL );
			if (p == null) return;
			if (FxRandom.Int( WISP_EVERY ) == 0){
				FxEmitter air = Fx.air();
				if (air != null) FxFactories.wisp( WISP ).emit( air, index, x, y - 6 );
			}
			if (FxRandom.Int( SPARK_EVERY ) == 0){
				FxFactories.spark( Element.SACRIFICIAL ).emit( Flames.host( emitter, SacrificialParticle.class ), index, x, y - 2 );
			}
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** A burst read as a sacrifice when it is big enough. */
	public static class Sacrifice extends BurstWatch {
		@Override
		protected void play( int n, float x, float y ){
			if (n < SACRIFICE) return;
			FxEmitter glow = Fx.glowAir();
			if (glow == null) return;
			for (int i = 0; i < 6; i++){
				((Spiral) glow.recycle( Spiral.class )).spiral( x, y - 6, (float)(2 * Math.PI * i / 6) + FxRandom.Float( -0.3f, 0.3f ) );
			}
			Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
			if (lights == null) return;
			LightFrame column = new LightFrame();
			column.setup( FxFrames.Light.SHAFT_40, x, y - 16, COLUMN, 0.8f ).life( 0.05f, 0.1f, 0.25f ).delay( SPIRAL ).seenAt( x, y );
			lights.add( column );
		}
	}

	/** A mote spiralling into a point over SPIRAL s: steered onto its spiral every frame. */
	public static class Spiral extends FxParticle {
		private float cx, cy, a0;

		void spiral( float x, float y, float angle ){
			cx = x;
			cy = y;
			a0 = angle;
			reset( x + (float)Math.cos( angle ) * 14f, y + (float)Math.sin( angle ) * 7f )
					.look( FxFrames.MOTE_2 ).paint( 0xB0D4FF ).light( true ).life( SPIRAL ).alpha( 1f, 0.05f, 0.05f )
					.priority( FxBudget.P2 );
		}

		@Override
		public void update(){
			float dt = Game.elapsed;
			if (dt > 0){
				//where it should be next: the radius closing in as it turns half a circle
				float t = Math.min( 1f, (age + dt) / life );
				float r = 14f * (1f - t), a = a0 + (float)Math.PI * t;
				vx = (cx + (float)Math.cos( a ) * r - px) / dt;
				vy = (cy + (float)Math.sin( a ) * r * 0.5f - py) / dt;
			}
			super.update();
		}
	}
}
