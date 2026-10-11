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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.badlogic.gdx.utils.IntMap;
import com.watabou.noosa.particles.Emitter;

import java.util.EnumMap;

/**
 * The particles characters wear while a state lasts: a burning body's tongues, its embers and
 * smoke, heat rising, frost falling and rising, shadow wisps, motes rising off a body. Cached
 * factories (the same object every time), FxRandom only, each asking FxBudget first.
 */
final class CombatFactories {

	private CombatFactories(){}

	static FxParticle make( Emitter e, float x, float y ){
		return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y );
	}

	// ------------------------------------------------------------------ a body on fire

	/** A body's tongues: small and middling, 70/30. */
	static final float TONGUE_SMALL = 0.7f;
	/** Their lives, s: a small one, a middling one (shrinking to small at 60%). */
	static final float LIFE_S = 0.36f, LIFE_M = 0.46f;

	private static final EnumMap<Element, Ramp> tongueRamps = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> tongues = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> bigTongues = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> embers = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> smokes = new EnumMap<>( Element.class );

	//the element's five hottest stops keyed as a tongue flows down them (as the kit's flame)
	static synchronized Ramp tongueRamp( Element e ){
		Ramp r = tongueRamps.get( e );
		if (r == null){
			r = e.ramp.size() >= 5 ? e.ramp.keyed( 0, .18f, .45f, .7f, 1 ) : e.ramp;
			tongueRamps.put( e, r );
		}
		return r;
	}

	/** A tongue licking up a burning body: TONGUE_S or TONGUE_M, rising and swaying. Light, P1. */
	static synchronized Emitter.Factory tongue( final Element el ){
		Emitter.Factory f = tongues.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P1, x, y )) return;
					FxParticle p = make( e, x, y );
					float life;
					if (FxRandom.Float() < TONGUE_SMALL){
						p.look( FxFrames.TONGUE_S, 12 + FxRandom.Float( -2f, 2f ) );
						life = LIFE_S;
					} else {
						p.look( FxFrames.TONGUE_M, 12 + FxRandom.Float( -2f, 2f ) ).then( 0.6f, FxFrames.TONGUE_S );
						life = LIFE_M;
					}
					life *= FxRandom.Float( 0.85f, 1.15f );
					p.ramp( tongueRamp( el ), true ).life( life ).alpha( 1f, 0, life * 0.3f )
							.vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -12f, -5f ) )
							.gravity( FxRandom.Float( -30f, -20f ) ).sway( FxRandom.Float( 3f, 7f ), FxRandom.Float( 5f, 9f ) )
							.wind( 0.15f ).priority( FxBudget.P1 );
				}

				@Override
				public boolean lightMode(){
					return true;
				}
			};
			tongues.put( el, f );
		}
		return f;
	}

	/** The catching: a middling or large tongue leaping up (the first 0.15 s of a fire). Light, P0. */
	static synchronized Emitter.Factory ignition( final Element el ){
		Emitter.Factory f = bigTongues.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					FxBudget.keep( FxBudget.P0 );
					FxParticle p = make( e, x, y );
					float life;
					if (FxRandom.chance( 0.5f )){
						p.look( FxFrames.TONGUE_L, 13 ).then( 0.55f, FxFrames.TONGUE_M ).then( 0.8f, FxFrames.TONGUE_S );
						life = 0.55f;
					} else {
						p.look( FxFrames.TONGUE_M, 13 ).then( 0.6f, FxFrames.TONGUE_S );
						life = 0.45f;
					}
					p.ramp( tongueRamp( el ), true ).life( life ).alpha( 1f, 0, life * 0.35f )
							.vel( FxRandom.Float( -6f, 6f ), FxRandom.Float( -26f, -16f ) ).gravity( -24f )
							.sway( FxRandom.Float( 3f, 6f ), FxRandom.Float( 6f, 9f ) ).priority( FxBudget.P0 );
				}

				@Override
				public boolean lightMode(){
					return true;
				}
			};
			bigTongues.put( el, f );
		}
		return f;
	}

	/** An ember off a burning body: a dot down the element's ember ramp, rising and drifting,
	 *  0.5-0.9 s (shorter than a fire's: a body's fire is small); its end the kit's. Light, P2. */
	static synchronized Emitter.Factory ember( final Element el ){
		Emitter.Factory f = embers.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					Ramp r = el.ember != null ? el.ember : FxFactories.EMBER;
					make( e, x, y ).look( FxFrames.EMBER_1 ).ramp( r, true ).life( FxRandom.Float( 0.5f, 0.9f ) )
							.alpha( 1f, 0.05f, 0.3f ).vel( FxRandom.Float( -5f, 5f ), FxRandom.Float( -26f, -14f ) )
							.gravity( -6f ).drag( 0.8f ).sway( FxRandom.Float( 3f, 5f ), FxRandom.Float( 2f, 4f ) )
							.wind( 0.4f ).fate( FxFactories.EMBER_END ).priority( FxBudget.P2 );
				}

				@Override
				public boolean lightMode(){
					return true;
				}
			};
			embers.put( el, f );
		}
		return f;
	}

	/** A small puff of the element's smoke off a burning head: PUFF_S stepping down its smoke ramp,
	 *  rising 10-14 px/s, at most 0.4 (it is over a character), 0.9-1.2 s. Matter, P2. */
	static synchronized Emitter.Factory smoke( final Element el ){
		Emitter.Factory f = smokes.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					Ramp r = el.smoke != null ? el.smoke : Element.SMOKE.smoke;
					float life = FxRandom.Float( 0.9f, 1.2f );
					make( e, x, y ).look( FxFrames.PUFF_S[FxRandom.Int( FxFrames.PUFF_S.length )] ).ramp( r, false ).smooth()
							.life( life ).alpha( 0.4f, life * 0.2f, life * 0.5f )
							.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 10f, 14f ) ).drag( 0.07f )
							.turbulence( FxRandom.Float( 1f, 2f ), FxRandom.Float( 2f, 4f ) ).wind( 0.35f ).priority( FxBudget.P2 );
				}
			};
			smokes.put( el, f );
		}
		return f;
	}

	// ------------------------------------------------------------------ heat

	/** The heat's shimmer off a body in heatstroke. */
	static final int HEAT = 0xFFE0C0;

	/** A heat ray: a faint 1x3 shimmer rising off the body and wavering, 0.8-1.2 s. Light, P3. */
	static final Emitter.Factory HEAT_RAY = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P3, x, y )) return;
			float life = FxRandom.Float( 0.8f, 1.2f );
			make( e, x, y ).look( FxFrames.SPARK_3 ).paint( HEAT ).light( true ).life( life ).alpha( 0.35f, 0.2f, 0.4f )
					.vel( 0, -FxRandom.Float( 6f, 10f ) ).sway( 2f, FxRandom.Float( 5f, 8f ) ).snap().priority( FxBudget.P3 );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	// ------------------------------------------------------------------ cold

	/** Frost's white. */
	static final int FROST = 0xE8F6FF;

	/** A flake falling over a chilled body (or, rising, off one in hypothermia): a pixel of frost
	 *  drifting 4-8 px/s and swaying, 0.8-1.2 s. Matter, P3. */
	static final Emitter.Factory FLAKE = flake( false ), RISING_FLAKE = flake( true );

	private static Emitter.Factory flake( final boolean rising ){
		return new Emitter.Factory(){
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				if (!FxBudget.keep( FxBudget.P3, x, y )) return;
				float v = FxRandom.Float( 4f, 8f );
				make( e, x, y ).look( FxFrames.MOTE_1 ).paint( FROST ).life( FxRandom.Float( 0.8f, 1.2f ) )
						.alpha( 0.9f, 0.1f, 0.3f ).vel( 0, rising ? -v - 2f : v ).sway( FxRandom.Float( 2f, 4f ), FxRandom.Float( 3f, 6f ) )
						.snap().priority( FxBudget.P3 );
			}
		};
	}

	// ------------------------------------------------------------------ shadow, motes

	/** A wisp of shadow rising off a body (marked, darkened): INK_WISP stepping down the shadow's
	 *  matter, rising 6-10 px/s and swaying, at most 0.5. Matter, P2. */
	static final Emitter.Factory INK_WISP = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			float life = FxRandom.Float( 1.3f, 1.6f );
			make( e, x, y ).look( FxFrames.INK_WISP, 4 ).ramp( Element.SHADOW.smoke, false ).smooth().life( life )
					.alpha( 0.5f, life * 0.2f, life * 0.5f ).vel( FxRandom.Float( -1.5f, 1.5f ), -FxRandom.Float( 6f, 10f ) )
					.sway( FxRandom.Float( 1.5f, 3f ), FxRandom.Float( 2f, 4f ) ).priority( FxBudget.P2 );
		}
	};

	private static final IntMap<Emitter.Factory> risers = new IntMap<>();

	/** A mote rising off a body: a dot of a colour, as light or as matter, rising 6-12 px/s and
	 *  slowing, 0.8-1.2 s, on whole pixels. P2. */
	static synchronized Emitter.Factory riser( final int color, final boolean light ){
		int key = ((color & 0xFFFFFF) << 1) | (light ? 1 : 0);
		Emitter.Factory f = risers.get( key );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					make( e, x, y ).look( FxRandom.chance( 0.7f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( color ).light( light )
							.life( FxRandom.Float( 0.8f, 1.2f ) ).alpha( 0.9f, 0.15f, 0.35f )
							.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 6f, 12f ) ).drag( 0.6f ).snap().priority( FxBudget.P2 );
				}

				@Override
				public boolean lightMode(){
					return light;
				}
			};
			risers.put( key, f );
		}
		return f;
	}

	/** A torch's spark: an ember dot hopping up off the flame, 0.4-0.7 s. Light, P3. */
	static final Emitter.Factory TORCH_SPARK = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P3, x, y )) return;
			make( e, x, y ).look( FxFrames.EMBER_1 ).ramp( Element.FIRE.ember, true ).life( FxRandom.Float( 0.4f, 0.7f ) )
					.alpha( 1f, 0, 0.2f ).vel( FxRandom.Float( -6f, 6f ), -FxRandom.Float( 12f, 22f ) ).gravity( -8f ).drag( 1f )
					.wind( 0.4f ).priority( FxBudget.P3 );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};
}
