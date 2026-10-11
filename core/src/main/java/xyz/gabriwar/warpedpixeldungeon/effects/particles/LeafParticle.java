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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A leaf thrown up: a painted leaf (the weather sheet's three, the kit's LEAF_D and LEAF_E) lit
 * in its colour, popped up and out, falling slower after its apex as it flutters - swinging side
 * to side and turning over (mirrored, now and then its other face) every 0.12-0.3 s - to settle a
 * few px below where it started: on the floor it lies a moment and fades, on high grass it fades
 * into the blades at once, on water it floats a second, drifting, with a small ring that tells
 * the fish there is food. At 70% of its flight it ages a step (15% darker, toward C88A40 in an
 * autumn). Hidden where the hero does not see where it rose; at most CAP on screen.
 *
 * GENERAL and LEVEL_SPECIFIC are the same objects as ever (NetVisuals' "leaf" and "leaf_level").
 */
public class LeafParticle extends FxParticle {

	public static int color1;
	public static int color2;

	/** The most leaves on screen at once. */
	public static final int CAP = 150;
	/** The pop: px/s sideways either way, up between; its gravity, px/s^2. */
	public static final float POP_X = 10f, POP_UP_MIN = 16f, POP_UP_MAX = 24f, GRAVITY = 28f;
	/** The flutter after the apex: a swing of 6-10 px/s at 3-5 rad/s, turning over every
	 *  0.12-0.3 s, the sideways drag, the fastest it falls. */
	public static final float SWING_MIN = 6f, SWING_MAX = 10f, RATE_MIN = 3f, RATE_MAX = 5f,
			FLIP_MIN = 0.12f, FLIP_MAX = 0.3f, DRAG = 2.5f, FLUTTER_FALL = 16f;
	/** Where it settles, px below its start; how long it lies, floats; its fade. */
	public static final float SETTLE_MIN = 2f, SETTLE_MAX = 6f, LIE_MIN = 0.3f, LIE_MAX = 0.6f,
			FLOAT = 1f, FADE = 0.25f, GRASS_FADE = 0.15f;
	/** Its flight, s (where it ages: AGES_AT of it), the longest it may stay up before it fades
	 *  where it is (popped and falling at these numbers, a leaf lands 1.4-2 s after it rose);
	 *  its share of an open sky's wind. */
	public static final float FLIGHT_MIN = 1.2f, FLIGHT_MAX = 1.8f, AGES_AT = 0.7f, AIR_MOST = 2.4f, WIND = 0.6f;

	private static final int FLY = 0, LIE = 1, FLOAT_ON = 2, GONE = 3;
	private static final int[][] WEATHER_FACES = WeatherSprites.LEAVES;
	private static final AtomicInteger living = new AtomicInteger();

	/** The share of GENERAL's emits that make a leaf: a painted leaf is bigger than the square it
	 *  was and stays up longer, so a bolt's pour at its old rate covered twice the screen (the
	 *  previews: a regrowth cone buried its own growth). */
	public static final float GENERAL_SHARE = 0.6f;

	public static final Emitter.Factory GENERAL = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxRandom.chance( GENERAL_SHARE ) || !allowed( x, y )) return;
			((LeafParticle)emitter.recycle( LeafParticle.class )).leaf( x, y, Nature.generalLeaf() );
		}
	};

	public static final Emitter.Factory LEVEL_SPECIFIC = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!allowed( x, y )) return;
			((LeafParticle)emitter.recycle( LeafParticle.class )).leaf( x, y, Nature.levelLeaf() );
		}
	};

	/** Whether another leaf may be made at (x, y): under CAP and kept by the budget (P2). */
	public static boolean allowed( float x, float y ){
		return living.get() < CAP && FxBudget.keep( FxBudget.P2, x, y );
	}

	/** Leaves alive now. */
	public static int alive(){
		return living.get();
	}

	private boolean onWeatherSheet, counted;
	private int[][] faces;
	private int face;
	private int state;
	private int color;
	private boolean apex, aged;
	private float flight, flown, flipIn, settleY;

	public LeafParticle() {
		super();
	}

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

	/** A leaf of the level's colours popped up at (x, y). */
	@Override
	public LeafParticle reset( float x, float y ) {
		return leaf( x, y, Nature.levelLeaf() );
	}

	/** A leaf of `color` popped up at (x, y). */
	public LeafParticle leaf( float x, float y, int color ){
		super.reset( x, y );
		//three in five the weather's leaves, the rest the kit's: a face and the other it turns to
		boolean weather = FxRandom.chance( 0.6f );
		if (weather != onWeatherSheet){
			onWeatherSheet = weather;
			if (weather) texture( WeatherSprites.get() );
			else texture( Assets.Effects.FX_SHEET );
		}
		if (weather){
			faces = WEATHER_FACES;
		} else {
			faces = FxRandom.chance( 0.5f ) ? FxFrames.LEAF_D : FxFrames.LEAF_E;
		}
		face = FxRandom.Int( faces.length );
		look( faces[face] );
		flipHorizontal = FxRandom.chance( 0.5f );
		updateFrame();

		this.color = color;
		flight = FxRandom.Float( FLIGHT_MIN, FLIGHT_MAX );
		flown = 0;
		state = FLY;
		apex = aged = false;
		flipIn = FxRandom.Float( FLIP_MIN, FLIP_MAX );
		settleY = y + FxRandom.Float( SETTLE_MIN, SETTLE_MAX );
		//its life: the flight, and the longest it may lie after it (cut short as it lands)
		paint( color ).life( AIR_MOST + FLOAT + FADE ).alpha( 1f, 0.05f, FADE )
				.vel( FxRandom.Float( -POP_X, POP_X ), -FxRandom.Float( POP_UP_MIN, POP_UP_MAX ) )
				.gravity( GRAVITY ).wind( WIND ).source( x, y ).priority( FxBudget.P2 );
		return this;
	}

	@Override
	public void update() {
		float dt = Game.elapsed;
		if (state == FLY){
			flown += dt;
			if (!apex && vy >= 0){
				apex = true;
				sway( FxRandom.Float( SWING_MIN, SWING_MAX ), FxRandom.Float( RATE_MIN, RATE_MAX ) );
			}
			if (apex){
				vx *= Math.max( 0f, 1f - DRAG * dt );
				if (vy > FLUTTER_FALL) vy = FLUTTER_FALL;
				flutter( dt );
				if (py + vy * dt >= settleY){
					land();
				}
			}
			if (state == FLY && flown >= AIR_MOST){
				//out of its flight in the air: it fades where it is
				state = GONE;
				age = Math.max( age, life - FADE );
			}
			if (!aged && flown >= AGES_AT * flight){
				aged = true;
				paint( Nature.aged( color ) );
			}
		} else if (state == FLOAT_ON && (flipIn -= dt) <= 0){
			//it turns lazily on the water
			flipIn = FxRandom.Float( 0.5f, 0.9f );
			flipHorizontal = !flipHorizontal;
			updateFrame();
		}
		super.update();
	}

	//its turning over as it flutters: mirrored, now and then its other face
	private void flutter( float dt ){
		if ((flipIn -= dt) > 0) return;
		flipIn = FxRandom.Float( FLIP_MIN, FLIP_MAX );
		flipHorizontal = !flipHorizontal;
		if (faces.length > 1 && FxRandom.chance( 0.5f )){
			face = (face + 1 + FxRandom.Int( faces.length - 1 )) % faces.length;
			look( faces[face] );
		}
		updateFrame();
	}

	//on its ground: lying on the floor, fading into high grass, floating on water
	private void land(){
		py = settleY;
		vy = 0;
		gravity = 0;
		swayAmp = 0;
		windShare = 0;
		float left;
		Level level = Dungeon.level;
		int cell = Fx.cellAt( px, py );
		if (WaterFX.onWater( px, py )){
			state = FLOAT_ON;
			vx = FxRandom.Float( -2f, 2f );
			flipIn = FxRandom.Float( 0.3f, 0.6f );
			WaterFX.drop( px, py, 0.15f, Element.NONE );
			WaterFX.disturb( px, py, 24f, true );
			left = FLOAT + FADE;
		} else if (level != null && cell >= 0 && cell < level.length() && level.map[cell] == Terrain.HIGH_GRASS){
			state = GONE;
			vx = 0;
			fadeOut = GRASS_FADE;
			left = GRASS_FADE;
		} else {
			state = LIE;
			vx = 0;
			left = FxRandom.Float( LIE_MIN, LIE_MAX ) + FADE;
		}
		age = Math.max( age, life - left );
	}
}
