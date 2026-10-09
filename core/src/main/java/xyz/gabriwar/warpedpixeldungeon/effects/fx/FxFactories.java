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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.LongMap;
import com.watabou.noosa.particles.Emitter;

import java.util.EnumMap;

/**
 * The kit's particle factories, one cached Emitter.Factory per kind and element or colour (the
 * same object every time: a caller may keep it, compare it, register it). Each asks FxBudget
 * whether to make its particle at all (and drops P2 and P3 with the distance from the hero) and
 * draws its randomness from FxRandom only, so emitting costs gameplay nothing.
 *
 * Light ones (tongue, lick, ember, spark, glint, a light mote) say so (lightMode) and belong in a
 * light emitter (GameScene.lightEmitter) to read over the night's tint; matter ones (puff, wisp,
 * steam, debris, droplet, shard, a matter mote) in an ordinary one.
 *
 * The flame: TONGUE_S, M and L flipbooks at 12 fps (+-2, from a random frame), mixed 50/35/15,
 * living 0.40, 0.58 and 0.80 s (x 0.85-1.15), a large one shrinking to medium at 55% of its life
 * and to small at 80%, a medium to small at 60%; flowing down the element's ramp (its five hottest
 * stops held at 0, .18, .45, .7 and 1), full until it fades over its last 30%; off at (+-4, -14..-6)
 * px/s, rising harder (-22..-34 px/s^2), swaying 3-9 px/s at 5-9 rad/s, 0.15 of an open sky's wind.
 */
public final class FxFactories {

	private FxFactories(){}

	//the five hottest stops of each element's ramp, keyed as a tongue flows down them
	private static final EnumMap<Element, Ramp> tongueRamps = new EnumMap<>( Element.class );
	/** An ember's ramp where its element has none of its own. */
	public static final Ramp EMBER = new Ramp( 0xFFF0A0, 0xFFB040, 0xF06020, 0xA02810, 0x3A1008 );
	/** A spark's last twinkle and an ember's last speck. */
	public static final int SPECK = 0x5E5850, STEAM = 0xEEF2F4;
	/** The glass's and the ice's shards. */
	public static final int GLASS = 0xCCD8E0, ICE = 0xE8F6FF;

	//a glint: a dot, a plus, a star, a plus, a dot
	static final int[][] GLINT = { FxFrames.MOTE_1, FxFrames.PLUS_3, FxFrames.STAR4_5, FxFrames.PLUS_3, FxFrames.MOTE_1 };
	//each puff's two looks, one frame each, as the stages a puff grows through
	private static final int[][][] PUFF_S = single( FxFrames.PUFF_S ), PUFF_M = single( FxFrames.PUFF_M ),
			PUFF_L = single( FxFrames.PUFF_L );
	private static final int[][] PEBBLES = { FxFrames.PEBBLE_2, FxFrames.PEBBLE_3, FxFrames.PEBBLE_3V };
	private static final int[][] DROPS = { FxFrames.DROP_2, FxFrames.DROP_3 };

	private static int[][][] single( int[][] frames ){
		int[][][] out = new int[frames.length][][];
		for (int i = 0; i < frames.length; i++) out[i] = new int[][]{ frames[i] };
		return out;
	}

	static synchronized Ramp tongueRamp( Element e ){
		Ramp r = tongueRamps.get( e );
		if (r == null){
			r = e.ramp.size() >= 5 ? e.ramp.keyed( 0, .18f, .45f, .7f, 1 ) : e.ramp;
			tongueRamps.put( e, r );
		}
		return r;
	}

	static Ramp emberRamp( Element e ){
		return e.ember != null ? e.ember : EMBER;
	}

	static Ramp sparkRamp( Element e ){
		return e.ember != null ? e.ember : e.ramp;
	}

	static Ramp smokeRamp( Element e ){
		return e.smoke != null ? e.smoke : Element.SMOKE.smoke;
	}

	//a spark's life by its element
	static float sparkLife( Element e ){
		switch (e){
			case ELECTRIC: return FxRandom.Float( 0.25f, 0.45f );
			case METAL:    return FxRandom.Float( 0.4f, 0.8f );
			case STEEL:    return FxRandom.Float( 0.35f, 0.7f );
			default:       return FxRandom.Float( 0.3f, 0.6f );
		}
	}

	static FxParticle make( Emitter e, float x, float y ){
		return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y );
	}

	// ------------------------------------------------------------------ fates

	/** An ember's end: over water a hiss of steam and a small ring; else a time in twelve it pops
	 *  into two sparks with a twinkle, and three times in ten it leaves a grey speck. */
	public static final FxParticle.Fate EMBER_END = ( p, landed ) -> {
		if (WaterFX.onWater( p.px, p.py )){
			steamSpeck( p.px, p.py );
			WaterFX.drop( p.px, p.py, 0.1f, Element.NONE );
			return;
		}
		Emitter e = (Emitter) p.parent;
		float r = FxRandom.Float();
		if (r < 0.08f && e != null){
			for (int i = 0; i < 2; i++){
				make( e, p.px, p.py ).look( FxFrames.SPARK_2 ).orient().paint( p.colorNow() ).light( true )
						.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 20f, 40f ) ).gravity( 120f )
						.drag( 1.2f ).life( FxRandom.Float( 0.15f, 0.3f ) ).alpha( 1f, 0, 0.1f ).priority( FxBudget.P3 );
			}
			make( e, p.px, p.py ).look( FxFrames.PLUS_3 ).paint( p.colorNow() ).light( true ).life( 1 / 30f )
					.alpha( 0.8f, 0, 0 ).snap().priority( FxBudget.P3 );
		} else if (r < 0.38f){
			FxEmitter air = Fx.air();
			if (air != null && FxBudget.keep( FxBudget.P3, p.px, p.py )){
				make( air, p.px, p.py ).look( FxFrames.MOTE_1 ).paint( SPECK ).vel( 0, -3f ).drag( 1f )
						.life( FxRandom.Float( 0.5f, 0.9f ) ).alpha( 0.7f, 0, 0.25f ).snap().priority( FxBudget.P3 );
			}
		}
	};

	/** A spark's end: a one-frame twinkle at 0.6. */
	public static final FxParticle.Fate SPARK_END = ( p, landed ) -> {
		if (landed) return;
		Emitter e = (Emitter) p.parent;
		if (e == null || !FxBudget.keep( FxBudget.P3 )) return;
		make( e, p.px, p.py ).look( FxFrames.PLUS_3 ).paint( p.colorNow() ).light( true ).life( 1 / 30f )
				.alpha( 0.6f, 0, 0 ).snap().priority( FxBudget.P3 );
	};

	//a hiss of steam where something hot met the water
	static void steamSpeck( float x, float y ){
		FxEmitter air = Fx.air();
		if (air == null || !FxBudget.keep( FxBudget.P3, x, y )) return;
		make( air, x, y - 1 ).look( FxFrames.MOTE_2 ).paint( STEAM ).vel( FxRandom.Float( -2f, 2f ), -10f ).drag( 0.5f )
				.life( 0.6f ).alpha( 0.45f, 0.05f, 0.3f ).smooth().priority( FxBudget.P3 );
	}

	// ------------------------------------------------------------------ fire

	private static final EnumMap<Element, Emitter.Factory> tongues = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> licks = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> embers = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> sparks = new EnumMap<>( Element.class );
	private static final EnumMap<Element, Emitter.Factory> puffs = new EnumMap<>( Element.class );

	/** A flame's tongue of the element (the flame model above): light, P1. */
	public static synchronized Emitter.Factory tongue( final Element el ){
		Emitter.Factory f = tongues.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P1, x, y )) return;
					float r = FxRandom.Float();
					float k = FxRandom.Float( 0.85f, 1.15f );
					FxParticle p = make( e, x, y );
					float life;
					if (r < 0.5f){
						p.look( FxFrames.TONGUE_S, 12 + FxRandom.Float( -2f, 2f ) );
						life = 0.40f;
					} else if (r < 0.85f){
						p.look( FxFrames.TONGUE_M, 12 + FxRandom.Float( -2f, 2f ) ).then( 0.6f, FxFrames.TONGUE_S );
						life = 0.58f;
					} else {
						p.look( FxFrames.TONGUE_L, 12 + FxRandom.Float( -2f, 2f ) )
								.then( 0.55f, FxFrames.TONGUE_M ).then( 0.8f, FxFrames.TONGUE_S );
						life = 0.80f;
					}
					life *= k;
					p.ramp( tongueRamp( el ), true ).life( life ).alpha( 1f, 0, life * 0.3f )
							.vel( FxRandom.Float( -4f, 4f ), FxRandom.Float( -14f, -6f ) )
							.gravity( FxRandom.Float( -34f, -22f ) ).sway( FxRandom.Float( 3f, 9f ), FxRandom.Float( 5f, 9f ) )
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

	/** A small lick of the element's flame: quick, low, light, P2. */
	public static synchronized Emitter.Factory lick( final Element el ){
		Emitter.Factory f = licks.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					float life = FxRandom.Float( 0.2f, 0.35f );
					make( e, x, y ).look( FxFrames.LICK, 12 + FxRandom.Float( -2f, 2f ) ).ramp( tongueRamp( el ), true )
							.life( life ).alpha( 1f, 0, life * 0.4f )
							.vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -18f, -10f ) ).gravity( -20f )
							.sway( FxRandom.Float( 2f, 5f ), FxRandom.Float( 6f, 10f ) ).wind( 0.15f ).priority( FxBudget.P2 );
				}

				@Override
				public boolean lightMode(){
					return true;
				}
			};
			licks.put( el, f );
		}
		return f;
	}

	/** An ember rising off the element's fire: a dot or two down its ember ramp, buoyant, drifting
	 *  with 0.4 of the wind, 0.8-2.2 s; its end EMBER_END. Light, P2. */
	public static synchronized Emitter.Factory ember( final Element el ){
		Emitter.Factory f = embers.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					make( e, x, y ).look( FxRandom.chance( 0.8f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 )
							.ramp( emberRamp( el ), true ).life( FxRandom.Float( 0.8f, 2.2f ) ).alpha( 1f, 0.05f, 0.4f )
							.vel( FxRandom.Float( -6f, 6f ), FxRandom.Float( -30f, -14f ) ).gravity( -6f ).drag( 0.8f )
							.sway( FxRandom.Float( 3f, 5f ), FxRandom.Float( 2f, 4f ) ).wind( 0.4f )
							.fate( EMBER_END ).priority( FxBudget.P2 );
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

	/** A spark of the element: a 1x2 streak turned to its way (a dot once under 15 px/s), off at
	 *  30-70 px/s into the upper half, falling at 120 with drag 1.2, bouncing once 4-8 px below
	 *  (keeping 0.35), its life the element's; its end SPARK_END. Light, P2. */
	public static synchronized Emitter.Factory spark( final Element el ){
		Emitter.Factory f = sparks.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					make( e, x, y ).look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 15f )
							.ramp( sparkRamp( el ), true ).life( sparkLife( el ) ).alpha( 1f, 0, 0.1f )
							.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 30f, 70f ) )
							.gravity( 120f ).drag( 1.2f ).ground( y + FxRandom.Float( 4f, 8f ), FxParticle.BOUNCE, 0.35f )
							.fate( SPARK_END ).priority( FxBudget.P2 );
				}

				@Override
				public boolean lightMode(){
					return true;
				}
			};
			sparks.put( el, f );
		}
		return f;
	}

	// ------------------------------------------------------------------ motes and glints

	private static final IntMap<Emitter.Factory> motes = new IntMap<>();
	private static final IntMap<Emitter.Factory> glints = new IntMap<>();
	private static final IntMap<Emitter.Factory> wisps = new IntMap<>();
	private static final IntMap<Emitter.Factory> droplets = new IntMap<>();
	private static final LongMap<Emitter.Factory> debris = new LongMap<>();

	/** A mote drifting 4-8 px/s and slowing (drag 2), on whole pixels: as light or as matter. P2. */
	public static synchronized Emitter.Factory mote( final int color, final boolean light ){
		int key = ((color & 0xFFFFFF) << 1) | (light ? 1 : 0);
		Emitter.Factory f = motes.get( key );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					make( e, x, y ).look( FxRandom.chance( 0.7f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 )
							.paint( color ).light( light ).life( FxRandom.Float( 0.6f, 1.4f ) ).alpha( 0.9f, 0.15f, 0.3f )
							.polar( FxRandom.angle(), FxRandom.Float( 4f, 8f ) ).drag( 2f ).snap().priority( FxBudget.P2 );
				}

				@Override
				public boolean lightMode(){
					return light;
				}
			};
			motes.put( key, f );
		}
		return f;
	}

	/** A glint: a dot, a plus, a star, a plus, a dot over 0.38 s, on whole pixels. Light, P3. */
	public static synchronized Emitter.Factory glint( final int color ){
		int key = color & 0xFFFFFF;
		Emitter.Factory f = glints.get( key );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P3, x, y )) return;
					make( e, x, y ).look( GLINT, 0 ).paint( color ).light( true ).life( 0.38f ).alpha( 1f, 0, 0.05f )
							.snap().priority( FxBudget.P3 );
				}

				@Override
				public boolean lightMode(){
					return true;
				}
			};
			glints.put( key, f );
		}
		return f;
	}

	// ------------------------------------------------------------------ matter

	/**
	 * A puff of the element's smoke (its matter: soot, ash, mist, fumes; plain smoke where it has
	 * none): growing from small to medium at 35% of its life and to large at 70%, stepping down its
	 * ramp, rising 10-16 px/s and slowing 8% a second, 0.35 of the wind, its alpha up to 0.55 and
	 * back, 1.4-2.4 s. Matter, P2.
	 */
	public static synchronized Emitter.Factory puff( final Element el ){
		Emitter.Factory f = puffs.get( el );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					int v = FxRandom.Int( 2 );
					float life = FxRandom.Float( 1.4f, 2.4f );
					make( e, x, y ).look( PUFF_S[v], 0 ).then( 0.35f, PUFF_M[v] ).then( 0.7f, PUFF_L[v] )
							.ramp( smokeRamp( el ), false ).smooth().life( life ).alpha( 0.55f, life * 0.25f, life * 0.45f )
							.vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 10f, 16f ) ).drag( 0.08f )
							.turbulence( FxRandom.Float( 1f, 3f ), FxRandom.Float( 2f, 5f ) ).wind( 0.35f )
							.priority( FxBudget.P2 );
				}
			};
			puffs.put( el, f );
		}
		return f;
	}

	/** A wisp of steam or vapour: a curl or a thin drift rising 12-20 px/s and slowing, its alpha
	 *  0.4 at most, 1.2-2 s. Matter, P2. */
	public static synchronized Emitter.Factory wisp( final int color ){
		int key = color & 0xFFFFFF;
		Emitter.Factory f = wisps.get( key );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					float life = FxRandom.Float( 1.2f, 2f );
					FxParticle p = make( e, x, y );
					if (FxRandom.chance( 0.6f )) p.look( FxFrames.STEAM_CURL, 4 );
					else p.look( FxFrames.EDGE_WISP[FxRandom.Int( FxFrames.EDGE_WISP.length )] );
					p.paint( color ).smooth().life( life ).alpha( 0.4f, life * 0.25f, life * 0.5f )
							.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 12f, 20f ) ).drag( 0.1f ).wind( 0.35f )
							.priority( FxBudget.P2 );
				}
			};
			wisps.put( key, f );
		}
		return f;
	}

	/** Steam: a white wisp. */
	public static Emitter.Factory steam(){
		return wisp( STEAM );
	}

	/** Debris: pebbles in two colours thrown up at 40-90 px/s, falling at 240, tumbling a
	 *  quarter turn at a time, bouncing once 6-10 px below, 0.6-1 s. Matter, P2. */
	public static synchronized Emitter.Factory debris( final int c1, final int c2 ){
		long key = ((long)(c1 & 0xFFFFFF) << 24) | (c2 & 0xFFFFFF);
		Emitter.Factory f = debris.get( key );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					make( e, x, y ).look( FxRandom.element( PEBBLES ) ).paint( FxRandom.chance( 0.5f ) ? c1 : c2 )
							.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 40f, 90f ) ).gravity( 240f )
							.ground( y + FxRandom.Float( 6f, 10f ), FxParticle.BOUNCE, 0.3f ).spin( 0.12f )
							.life( FxRandom.Float( 0.6f, 1f ) ).alpha( 1f, 0, 0.25f ).priority( FxBudget.P2 );
				}
			};
			debris.put( key, f );
		}
		return f;
	}

	/** A droplet thrown up within 50 degrees of straight up at 30-60 px/s, turned to its way,
	 *  falling at 240 to land within 4 px: a ring on water, a wet mark on the floor. Matter, P3. */
	public static synchronized Emitter.Factory droplet( final int color ){
		int key = color & 0xFFFFFF;
		Emitter.Factory f = droplets.get( key );
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P3, x, y )) return;
					float a = (float)(-Math.PI / 2) + FxRandom.Float( -0.87f, 0.87f );
					make( e, x, y ).look( FxRandom.element( DROPS ) ).orient().paint( color ).polar( a, FxRandom.Float( 30f, 60f ) )
							.gravity( 240f ).ground( y + FxRandom.Float( 0f, 4f ), FxParticle.RING, 0 ).landDry( FxParticle.WET )
							.strength( 0.1f ).life( 2f ).alpha( 1f, 0, 0.05f ).priority( FxBudget.P3 );
				}
			};
			droplets.put( key, f );
		}
		return f;
	}

	private static Emitter.Factory glassShards, iceShards;

	/** A shard of glass (CCD8E0) or ice (E8F6FF): tumbling a quarter turn every 0.06 s, falling
	 *  at 240, bouncing once, a glint as it breaks off a time in three, 0.4-0.6 s. Matter, P2. */
	public static synchronized Emitter.Factory shard( final boolean ice ){
		Emitter.Factory f = ice ? iceShards : glassShards;
		if (f == null){
			f = new Emitter.Factory(){
				@Override
				public void emit( Emitter e, int index, float x, float y ){
					if (!FxBudget.keep( FxBudget.P2, x, y )) return;
					int[][] frames = ice ? FxFrames.SHARD_ICE : FxFrames.SHARD_GLASS;
					make( e, x, y ).look( FxRandom.element( frames ) ).paint( ice ? ICE : GLASS )
							.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 40f, 80f ) ).gravity( 240f )
							.ground( y + FxRandom.Float( 4f, 8f ), FxParticle.BOUNCE, 0.3f ).spin( 0.06f )
							.life( FxRandom.Float( 0.4f, 0.6f ) ).alpha( 1f, 0, 0.15f ).priority( FxBudget.P2 );
					if (FxRandom.chance( 0.33f )){
						FxEmitter glow = Fx.glowAir();
						if (glow != null) glint( 0xFFFFFF ).emit( glow, index, x, y );
					}
				}
			};
			if (ice) iceShards = f;
			else glassShards = f;
		}
		return f;
	}
}
