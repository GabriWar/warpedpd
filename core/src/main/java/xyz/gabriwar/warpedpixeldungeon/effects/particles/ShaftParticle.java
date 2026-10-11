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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.LitFrame;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;
import com.watabou.noosa.particles.Emitter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A shaft of light standing on the ground: a painted shaft (FxFrames.Light SHAFT_24, 32 or 40,
 * never a stretched texel) in the colour of its use, rising 6 px/s, up to its peak (0.45) over
 * 0.25 s and down over its last 0.5 s, with one or two motes rising faster inside it and, one
 * shaft in three, a faint pool of its light at its foot. Hidden until its life starts (a burst's
 * shafts come one after another, never drawn clear while they wait).
 *
 * Its factories send it to the light layer whatever emitter asks (it is light: over the night's
 * tint), keeping their identities: FACTORY (holy, NetVisuals' "shaft"), HEAL, SPIRIT, SUN, MOON.
 * At most CAP shafts (sunlight's too) stand at once, AMBIENT of them a blob's.
 */
public class ShaftParticle extends LitFrame {

	/** Its numbers: life, rise, peak, attack, release; the motes' rise. */
	public static final float LIFE = 1.2f, RISE = 6f, PEAK = 0.45f, ATTACK = 0.25f, RELEASE = 0.5f;
	public static final float MOTE_RISE_MIN = 8f, MOTE_RISE_MAX = 12f;
	/** A shaft in this many stands in a pool of its light (0.15). */
	public static final int POOL_EVERY = 3;
	public static final float POOL_ALPHA = 0.15f;
	/** The most a burst's shaft waits before its life starts. */
	public static final float MAX_DELAY = 0.5f;
	/** The most shafts standing at once (sunlight's counted with them), and the most a blob's
	 *  ambient light (P2 and P3: sunlight, foliage, a well, hallowed ground, a portal) may take:
	 *  the rest are kept for a cast's, a drink's or a wall of light's. */
	public static final int CAP = 40, AMBIENT = 28;

	private static final AtomicInteger living = new AtomicInteger();

	/** Whether another shaft may stand at (x, y): under CAP for a P1 one (an event's, a wall's),
	 *  under AMBIENT for the rest, and kept by the budget at `priority`. */
	public static boolean allowed( int priority, float x, float y ){
		return living.get() < (priority <= FxBudget.P1 ? CAP : AMBIENT) && FxBudget.keep( priority, x, y );
	}

	/** Whether a blob's ambient shaft may still stand (under AMBIENT), the budget asked apart. */
	public static boolean ambientRoom(){
		return living.get() < AMBIENT;
	}

	/** Shafts standing now (waiting ones too). */
	public static int alive(){
		return living.get();
	}

	private static final int[][] FRAMES = { FxFrames.Light.SHAFT_24, FxFrames.Light.SHAFT_32, FxFrames.Light.SHAFT_40 };

	/** Holy light (FFE9A8), a shaft of any of the three heights. */
	public static final Emitter.Factory FACTORY = factory( null, Nature.HOLY, PEAK );
	/** Healing (9CFFB0), a spirit (C8E0FF), the sun (FFFDE8, the tallest), the moon (C8D8FF). */
	public static final Emitter.Factory HEAL = factory( null, Nature.HEAL, PEAK ),
			SPIRIT = factory( FxFrames.Light.SHAFT_24, Nature.SPIRIT, PEAK ),
			SUN = factory( FxFrames.Light.SHAFT_40, Nature.SUN, 0.55f ),
			MOON = factory( FxFrames.Light.SHAFT_32, Nature.MOON, PEAK );

	/** A factory of shafts of one frame (null: any of the three) in a colour, up to a peak. */
	public static Emitter.Factory factory( final int[] frame, final int color, final float peak ){
		return new Emitter.Factory(){
			@Override
			public void emit( Emitter emitter, int index, float x, float y ){
				//an event's shafts (a cast, a drink, a plant): up to the whole cap
				if (living.get() >= CAP || !FxBudget.keep( FxBudget.P2, x, y )) return;
				Emitter carrier = Fx.lightCarrier( ShaftParticle.class );
				Emitter into = carrier != null ? carrier : emitter;
				((ShaftParticle) into.recycle( ShaftParticle.class ))
						.shaft( x, y, frame != null ? frame : FxRandom.element( FRAMES ), color, peak, LIFE )
						.delay( FxRandom.Float( MAX_DELAY ) );
			}

			@Override
			public boolean lightMode(){
				return true;
			}
		};
	}

	private int color;
	private float footX, footY;
	private boolean pooled, began, counted;

	@Override
	public void revive(){
		super.revive();
		if (!counted){
			counted = true;
			living.incrementAndGet();
		}
	}

	@Override
	public void kill(){
		super.kill();
		if (counted){
			counted = false;
			living.decrementAndGet();
		}
	}

	//gone with its scene (never killed): no longer counted
	@Override
	public void destroy(){
		super.destroy();
		if (counted){
			counted = false;
			living.decrementAndGet();
		}
	}

	/** A pale shaft standing at (x, y), at once: its old reset, for the callers that recycle one
	 *  themselves (the weather's moonbeams). */
	@Override
	public ShaftParticle reset( float x, float y ){
		return shaft( x, y, FxRandom.element( FRAMES ), Nature.SUN, PEAK, LIFE );
	}

	/** A shaft of `frame` standing with its foot at (x, y), in `color`, up to `peak`, for `life` s. */
	public ShaftParticle shaft( float x, float y, int[] frame, int color, float peak, float life ){
		super.reset( x, y );
		//(LitFrame's reset: not this class's own, which makes a whole pale shaft)
		this.color = color;
		footX = x;
		footY = y + 3;
		pooled = FxRandom.Int( POOL_EVERY ) == 0;
		began = false;
		look( frame ).paint( color ).light( true ).life( life ).alpha( peak, ATTACK, RELEASE )
				.at( x, footY - frame[3] / 2f ).vel( 0, -RISE ).snap().priority( FxBudget.P2 );
		return this;
	}

	/** Whether it stands in a pool of its light. */
	public ShaftParticle pool( boolean pool ){
		pooled = pool;
		return this;
	}

	@Override
	public void update(){
		//its life begun (after its wait, if it had one): its motes and its pool
		if (!began && !waiting() && alive){
			began = true;
			began();
		}
		super.update();
	}

	private void began(){
		Emitter e = parent instanceof Emitter ? (Emitter) parent : null;
		if (e == null) return;
		int motes = 1 + FxRandom.Int( 2 );
		for (int i = 0; i < motes; i++){
			if (!FxBudget.keep( FxBudget.P3, footX, footY )) continue;
			float l = FxRandom.Float( 0.6f, 1.1f );
			Nature.make( e, footX + FxRandom.Float( -2f, 2f ), footY - FxRandom.Float( 2f, 10f ) )
					.look( FxFrames.MOTE_1 ).paint( color ).light( true ).life( l ).alpha( 0.8f, 0.15f, 0.3f )
					.vel( 0, -FxRandom.Float( MOTE_RISE_MIN, MOTE_RISE_MAX ) ).snap().priority( FxBudget.P3 );
		}
		if (pooled && FxBudget.keep( FxBudget.P3, footX, footY )){
			Nature.lit( e, footX, footY ).look( FxFrames.Light.POOL_15 ).paint( color ).life( life )
					.alpha( POOL_ALPHA, ATTACK, RELEASE ).snap().priority( FxBudget.P3 );
		}
	}
}
