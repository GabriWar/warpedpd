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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.Camera;
import com.watabou.noosa.particles.Emitter;

import java.util.EnumMap;

/**
 * Fire's one flame, built on the kit's flame model (FxFactories.tongue, lick, ember) with what
 * the fire area needs on top of it: a chosen size, a source the flame is hidden away from (a fire
 * out of the hero's sight never shows through the explored dark), and a home in the light layer
 * for the old factories that every burning thing pours (host).
 *
 * The tongue: TONGUE_S, M or L flipbooks at 12 fps (+-2, from a random frame), mixed 50/35/15 when
 * no size is asked for, living 0.40, 0.58 and 0.80 s (x 0.85-1.15), a large one shrinking to a
 * medium at 55% of its life and a small at 80%, a medium to a small at 60%; flowing down the
 * element's five hottest stops (held at 0, .18, .45, .7, 1), full until its last 30%; off at
 * (+-4, -14..-6) px/s, rising harder (-22..-34 px/s^2), swaying 3-9 px/s at 5-9 rad/s, 0.15 of an
 * open sky's wind. The lick: LICK at 12 fps, 0.2-0.35 s, quicker and lower. The ember: a dot or
 * two down the element's ember ramp, buoyant, 0.4 of the wind, 0.8-2.2 s, ending as the kit's
 * embers end (a hiss on water, a pop, a grey speck).
 */
public final class Flames {

	private Flames(){}

	/** A tongue's size: small, medium, large, or the model's mix. */
	public static final int S = 0, M = 1, L = 2, MIX = -1;
	/** The tongues' lives by size, s (x 0.85-1.15). */
	public static final float[] LIFE = { 0.40f, 0.58f, 0.80f };

	private static final EnumMap<Element, Ramp> RAMPS = new EnumMap<>( Element.class );

	/** The element's five hottest stops as a tongue flows down them. */
	public static synchronized Ramp ramp( Element e ){
		Ramp r = RAMPS.get( e );
		if (r == null){
			r = e.ramp.size() >= 5 ? e.ramp.keyed( 0, .18f, .45f, .7f, 1 ) : e.ramp;
			RAMPS.put( e, r );
		}
		return r;
	}

	/** The element's embers' ramp (the kit's where it has none). */
	public static Ramp emberRamp( Element e ){
		return e.ember != null ? e.ember : FxFactories.EMBER;
	}

	/**
	 * Where a flame poured from `caller` is made: in the light layer's carrier for its class
	 * (Fx.lightCarrier), so it draws over the night's tint, when the caller lies in the world (its
	 * camera the main one); in the caller itself on a window, a menu or off the game's scene.
	 */
	public static Emitter host( Emitter caller, Class<? extends FxParticle> cls ){
		Camera c = caller.camera();
		if (c != null && c == Camera.main){
			Emitter carrier = Fx.lightCarrier( cls );
			if (carrier != null) return carrier;
		}
		return caller;
	}

	/** A particle of a class made in an emitter, at (x, y). */
	public static FxParticle make( Emitter e, Class<? extends FxParticle> cls, float x, float y ){
		return ((FxParticle) e.recycle( cls )).reset( x, y );
	}

	/** Sets `p` up as a tongue of the element's flame of a size (S, M, L or MIX). Light, P1. */
	public static FxParticle tongue( FxParticle p, Element el, int size ){
		return tongue( p, ramp( el ), size );
	}

	public static FxParticle tongue( FxParticle p, Ramp ramp, int size ){
		if (size == MIX){
			float r = FxRandom.Float();
			size = r < 0.5f ? S : (r < 0.85f ? M : L);
		}
		float fps = 12 + FxRandom.Float( -2f, 2f );
		switch (size){
			case S:
				p.look( FxFrames.TONGUE_S, fps );
				break;
			case M:
				p.look( FxFrames.TONGUE_M, fps ).then( 0.6f, FxFrames.TONGUE_S );
				break;
			default:
				p.look( FxFrames.TONGUE_L, fps ).then( 0.55f, FxFrames.TONGUE_M ).then( 0.8f, FxFrames.TONGUE_S );
		}
		float life = LIFE[size] * FxRandom.Float( 0.85f, 1.15f );
		return p.ramp( ramp, true ).life( life ).alpha( 1f, 0, life * 0.3f )
				.vel( FxRandom.Float( -4f, 4f ), FxRandom.Float( -14f, -6f ) )
				.gravity( FxRandom.Float( -34f, -22f ) ).sway( FxRandom.Float( 3f, 9f ), FxRandom.Float( 5f, 9f ) )
				.wind( 0.15f ).priority( FxBudget.P1 );
	}

	/** Sets `p` up as a lick of the element's flame: small, quick, low. Light, P2. */
	public static FxParticle lick( FxParticle p, Element el ){
		return lick( p, ramp( el ) );
	}

	public static FxParticle lick( FxParticle p, Ramp ramp ){
		float life = FxRandom.Float( 0.2f, 0.35f );
		return p.look( FxFrames.LICK, 12 + FxRandom.Float( -2f, 2f ) ).ramp( ramp, true )
				.life( life ).alpha( 1f, 0, life * 0.4f )
				.vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -18f, -10f ) ).gravity( -20f )
				.sway( FxRandom.Float( 2f, 5f ), FxRandom.Float( 6f, 10f ) ).wind( 0.15f ).priority( FxBudget.P2 );
	}

	/** Sets `p` up as an ember rising off a fire, down a ramp of embers. Light, P2. */
	public static FxParticle ember( FxParticle p, Ramp ramp ){
		return p.look( FxRandom.chance( 0.8f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 )
				.ramp( ramp, true ).life( FxRandom.Float( 0.8f, 2.2f ) ).alpha( 1f, 0.05f, 0.4f )
				.vel( FxRandom.Float( -6f, 6f ), FxRandom.Float( -30f, -14f ) ).gravity( -6f ).drag( 0.8f )
				.sway( FxRandom.Float( 3f, 5f ), FxRandom.Float( 2f, 4f ) ).wind( 0.4f )
				.fate( FxFactories.EMBER_END ).priority( FxBudget.P2 );
	}

	/**
	 * A flame of the element poured by one of the old factories (FlameParticle and its kin): a
	 * tongue of the model's mix made in the caller's light-layer home, shown only where the hero
	 * sees the spot it rose from. Nothing when the caller is hidden (a burning body out of sight)
	 * or the budget says no.
	 */
	public static FxParticle pour( Emitter caller, Class<? extends FxParticle> cls, float x, float y, Element el ){
		if (!caller.visible || !FxBudget.keep( FxBudget.P1, x, y )) return null;
		return tongue( make( host( caller, cls ), cls, x, y ), el, MIX ).source( x, y );
	}
}
