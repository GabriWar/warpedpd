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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * Earth, as pebbles and dust (matter, drawn normally; the effects' own randomness, thinned by
 * their budget). FACTORY ("earth"): pebbles in the earth's colours touched by the level's own (a
 * quarter of the way) thrown up a little, turning a quarter at a time, falling at 200 px/s^2 to bounce
 * once where they rose; the burst's first raises a puff of dust (PUFF_S, 0.35). FALLING
 * ("earth_falling"): pebbles dropping from rest 20 px up, a burst's one about every 0.3 s (each
 * waits, unseen, its turn; a pour's, a frame or more apart, fall at once), each landing in a little
 * plume of dust (at most 8 a burst). SMALL (a door, a geomancer's armour): its
 * one emit, three specks of dust drifting off the frame at 0.5.
 */
public class EarthParticle extends FxParticle {

	/** Dust: its colour, a puff's alpha and a speck's. */
	public static final int DUST = 0xB89C6C;
	public static final float PUFF_ALPHA = 0.35f, SPECK_ALPHA = 0.5f;
	/** Falling pebbles start this high, a burst's land this far apart in time, at most this many plumes. */
	public static final float FALL_FROM = 20f, TRICKLE = 0.3f;
	public static final int PLUMES = 8;
	private static final float FALL_G = 160f, THROW_G = 200f;
	private static final int[][] PEBBLES = { FxFrames.PEBBLE_2, FxFrames.PEBBLE_3, FxFrames.PEBBLE_3V };

	//a falling pebble's wait for its turn, s
	private float wait;

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (index == 0) dust( emitter, x, y, PUFF_ALPHA );
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((EarthParticle)emitter.recycle( EarthParticle.class )).resetPebble( x, y );
		}
	};

	public static final Emitter.Factory SMALL = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			for (int i = 0; i < 3; i++) {
				if (!FxBudget.keep( i == 0 ? FxBudget.P2 : FxBudget.P3, x, y )) return;
				((EarthParticle)emitter.recycle( EarthParticle.class )).resetSpeck( x, y );
			}
		}
	};

	public static final Emitter.Factory FALLING = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P1, x, y )) return;
			((EarthParticle)emitter.recycle( EarthParticle.class )).resetFalling( x, y, index, inTurn( emitter ) );
		}
	};

	//the emitter and frame of the last falling pebble, and how many it had that frame: a burst's
	//come in one frame and take turns, a pour's come a frame or more apart (render thread)
	private static int turnOf;
	private static float turnAt = -1;
	private static int turns;

	private static int inTurn( Emitter e ) {
		int id = System.identityHashCode( e );
		if (id == turnOf && Game.timeTotal == turnAt) return ++turns;
		turnOf = id;
		turnAt = Game.timeTotal;
		return turns = 0;
	}

	//a falling pebble's landing: a plume of dust
	private static final Fate PLUME = ( p, landed ) -> {
		if (landed && p.parent instanceof Emitter) dust( (Emitter)p.parent, p.px, p.py, PUFF_ALPHA );
	};

	/** A pebble's colour: the earth's, a quarter of the way to the level's own colours (those are
	 *  its moss and leaves: half of them turned a sewer's pebbles green). */
	public static final float LEVEL_SHARE = 0.25f;

	public static int pebbleColor() {
		int earth = Element.EARTH.ramp.stop( FxRandom.Int( 2 ) + 1 );
		if (Dungeon.level == null) return earth;
		int own = FxRandom.chance( 0.5f ) ? Dungeon.level.color1 : Dungeon.level.color2;
		return Ramp.lerp( earth, own, LEVEL_SHARE );
	}

	//a puff of dust rising off where the earth moved
	static void dust( Emitter e, float x, float y, float alpha ) {
		if (!FxBudget.keep( FxBudget.P3, x, y )) return;
		float life = FxRandom.Float( 0.6f, 0.9f );
		((FxParticle)e.recycle( FxParticle.class )).reset( x, y - 2 ).look( FxFrames.PUFF_S[FxRandom.Int( 2 )] ).paint( DUST )
				.smooth().life( life ).alpha( alpha, life * 0.15f, life * 0.6f ).vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 4f, 8f ) )
				.drag( 0.5f ).priority( FxBudget.P3 );
	}

	public void resetPebble( float x, float y ) {
		reset( x, y );
		look( FxRandom.element( PEBBLES ) ).paint( pebbleColor() ).life( FxRandom.Float( 0.7f, 1f ) ).alpha( 1f, 0, 0.25f )
				.polar( -FxRandom.Float( 0.2f, 0.8f ) * (float)Math.PI, FxRandom.Float( 30f, 60f ) ).gravity( THROW_G )
				.ground( y + FxRandom.Float( 1f, 3f ), BOUNCE, 0.3f ).spin( 0.12f ).priority( FxBudget.P2 );
	}

	/** A pebble falling from rest FALL_FROM px over (x, y), after the `turn`th of its burst's
	 *  TRICKLE s turns (hidden till then): the burst lands one every TRICKLE s. */
	public void resetFalling( float x, float y, int index, int turn ) {
		reset( x, y - FALL_FROM );
		look( FxRandom.element( PEBBLES ) ).paint( pebbleColor() ).life( 2.5f ).alpha( 1f, 0.05f, 0.1f )
				.vel( FxRandom.Float( -4f, 4f ), 0 ).gravity( FALL_G ).ground( y, DIE, 0 ).spin( 0.15f ).priority( FxBudget.P1 );
		if (index < PLUMES || index % 4 == 0) fate( PLUME );
		wait = (turn % 5) * TRICKLE;
		visible = wait <= 0;
	}

	/** How long it still waits its turn, s. */
	public float waiting() {
		return wait;
	}

	@Override
	public FxParticle reset( float x, float y ) {
		wait = 0;
		return super.reset( x, y );
	}

	@Override
	public void update() {
		if (wait > 0) {
			wait -= Game.elapsed;
			visible = false;
			return;
		}
		super.update();
	}

	public void resetSpeck( float x, float y ) {
		reset( x, y );
		float life = FxRandom.Float( 0.6f, 0.9f );
		look( FxRandom.chance( 0.7f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( Element.EARTH.ramp.stop( FxRandom.Int( 3 ) ) )
				.life( life ).alpha( SPECK_ALPHA, 0.05f, life * 0.5f ).polar( FxRandom.angle(), FxRandom.Float( 4f, 8f ) ).drag( 1.5f )
				.gravity( 6f ).snap().priority( FxBudget.P2 );
	}
}
