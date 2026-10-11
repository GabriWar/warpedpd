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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.gas;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherBlobFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.particles.Emitter;

/**
 * How one gas cloud looks (docs/fx/gases-cold-earth.md): its bodies' crown and underside, how
 * opaque it is at its thickest, whether it is drawn added (the magical ones), what detail it
 * sheds among its bodies and how often, whether it glows, tints or etches the floor under it, and
 * how fast its blob pours so a cell holds about `cap` pictures at once.
 *
 * A body is a GasPuff: GAS_S stepping to GAS_L at half its life (rolling PUFF_L to PUFF_XL for
 * smoke), its greys mapped from crown to underside (Shade), its alpha peak x (0.55 + 0.45 x
 * clamp(log10(cur) / 3, 0, 1)), 1.5-3 s, drifting down the cloud's volume (at most 3 px/s), curling
 * 1-2 px at 0.4 Hz, 0.15 of an open sky's wind. The details are WeatherBlobFX's.
 */
public final class GasStyle {

	/** Its name, as the gallery and the tests call it. */
	public final String name;
	/** Its bodies' crown and underside (0xRRGGBB). */
	public int crown, under;
	/** Its alpha at its thickest; for smoke the least of a range up to peakHi. */
	public float peak, peakHi;
	/** Drawn added (light), under the night's tint as every gas. */
	public boolean light;
	/** Rolling smoke: PUFF_M or PUFF_L bodies growing to PUFF_L or PUFF_XL, churning by a mirror flip every 0.6-1 s. */
	public boolean smoke;
	/** Hugging the floor: its bodies in a cell's lower half, sinking 1 px/s. */
	public boolean hug;
	/** Rising as steam does, px/s (0: drifting as gas). */
	public float rise;
	/** The colour its bodies turn toward after TURN_AT of their life (-1: none). */
	public int turn = -1;
	/** Its crown going round the confusion wheel (WHEEL, over WHEEL_SECONDS). */
	public boolean wheel;
	/** Its detail, every `every`th particle (index + cell: spread through the cloud). */
	public Emitter.Factory detail;
	public int every = 3;
	/** About how many of its pictures a cell holds at once, and the pour that gives it. */
	public int cap;
	public float pour;
	/** A glow of its own over the night's tint (GLOW_15 at glowLevel), 0 for none. */
	public int glow;
	public float glowLevel;
	/** A twinkle now and then: a white one-frame tick (static) or a glint, at most one per cell
	 *  every glintEvery s; 0 for none. */
	public float glintEvery;
	public boolean tick;
	/** Thick cells tint the floor under them; it etches the floor. */
	public boolean tint, etch;

	/** A body's life, s, and the share of it after which a turning gas turns. */
	public static final float LIFE_MIN = 1.5f, LIFE_MAX = 3f, SMOKE_LIFE_MIN = 1.6f, SMOKE_LIFE_MAX = 2.4f, TURN_AT = 0.6f;
	/** The confusion wheel and the time it takes to go round. */
	public static final int[] WHEEL = { 0xF080F0, 0x80C0FF, 0x80F0A0, 0xFFE080 };
	public static final float WHEEL_SECONDS = 6f;
	/** Volume at which a cell counts as thick (it tints the floor). */
	public static final int THICK = 40;

	private GasStyle( String name, int crown, int under, float peak, int cap, float pour ){
		this.name = name;
		this.crown = crown;
		this.under = under;
		this.peak = this.peakHi = peak;
		this.cap = cap;
		this.pour = pour;
	}

	private GasStyle detail( Emitter.Factory f, int every ){
		detail = f;
		this.every = every;
		return this;
	}

	private GasStyle glow( int color, float level ){
		glow = color;
		glowLevel = level;
		return this;
	}

	private GasStyle glints( float every, boolean tick ){
		glintEvery = every;
		this.tick = tick;
		return this;
	}

	private GasStyle light(){
		light = true;
		return this;
	}

	private GasStyle floor( boolean tint, boolean etch ){
		this.tint = tint;
		this.etch = etch;
		return this;
	}

	//toxic's colours, the ones its kin share
	private static final int TOXIC_CROWN = 0xA8F07C, TOXIC_UNDER = 0x3E9038;
	private static final int CORROSIVE_CROWN = 0xC8C4B8, CORROSIVE_UNDER = 0x6A665C;

	public static final GasStyle TOXIC = new GasStyle( "toxic gas", TOXIC_CROWN, TOXIC_UNDER, 0.5f, 6, 0.3f )
			.detail( WeatherBlobFX.bubbles( 0x90FF60 ), 3 ).floor( true, false );

	public static final GasStyle PARALYTIC = new GasStyle( "paralytic gas", 0xFFFFB0, 0xB8A830, 0.5f, 6, 0.3f )
			.detail( WeatherBlobFX.motes( 0xFFF080 ), 3 ).glow( 0xFFF080, 0.1f ).glints( 2f, true );

	public static final GasStyle CONFUSION = new GasStyle( "confusion gas", WHEEL[0], 0x5A3A6A, 0.45f, 6, 0.3f )
			.detail( WeatherBlobFX.SWIRL, 2 ).light();

	public static final GasStyle CORROSIVE = new GasStyle( "corrosive gas", CORROSIVE_CROWN, CORROSIVE_UNDER, 0.6f, 6, 0.3f )
			.detail( WeatherBlobFX.acidDrips( 0xD8F048 ), 3 ).floor( true, true );

	public static final GasStyle STENCH = new GasStyle( "stench", 0x8A8A4A, 0x3A3A1C, 0.55f, 5, 0.37f )
			.detail( WeatherBlobFX.FLIES, 3 );

	public static final GasStyle SMOKE = new GasStyle( "smoke", 0x2C2826, 0x141210, 0.75f, 8, 0.27f )
			.detail( WeatherBlobFX.SOOT, 5 );

	public static final GasStyle POISON = new GasStyle( "poison gas", TOXIC_CROWN, TOXIC_UNDER, 0.5f, 6, 0.3f )
			.detail( WeatherBlobFX.bubbles( 0xA0FF70 ), 3 );

	public static final GasStyle DIGESTING_ACID = new GasStyle( "digesting acid", TOXIC_CROWN, TOXIC_UNDER, 0.5f, 6, 0.3f )
			.detail( WeatherBlobFX.drips( 0xB0FF50 ), 3 );

	public static final GasStyle GRASS_SMOKE = new GasStyle( "grass smoke", TOXIC_CROWN, TOXIC_UNDER, 0.5f, 6, 0.3f )
			.detail( WeatherBlobFX.motes( 0xC8E860 ), 3 );

	public static final GasStyle WITHER = new GasStyle( "withering cloud", 0x9A8A58, 0x4A3E26, 0.6f, 6, 0.3f )
			.detail( WeatherBlobFX.LEAF_BITS, 3 );

	public static final GasStyle CORRUPT = new GasStyle( "corrupting gas", CORROSIVE_CROWN, CORROSIVE_UNDER, 0.6f, 4, 0.46f )
			.detail( WeatherBlobFX.acidDrips( 0xC04848 ), 3 ).floor( false, true );

	public static final GasStyle CLOUD_OF_CORRUPTION = new GasStyle( "cloud of corruption", 0x4A3050, 0x1D1017, 0.55f, 4, 0.46f )
			.detail( WeatherBlobFX.motes( 0x9050C0 ), 3 ).glow( 0x9050C0, 0.08f );

	public static final GasStyle DEPRESSANT = new GasStyle( "depressant", 0x9A8AE0, 0x5A4A9A, 0.5f, 5, 0.37f )
			.detail( WeatherBlobFX.drips( 0x7080A0 ), 3 );

	public static final GasStyle DIRT = new GasStyle( "dirt cloud", 0xB89060, 0x4A3018, 0.5f, 6, 0.31f )
			.detail( WeatherBlobFX.motes( 0xB89060 ), 2 );

	public static final GasStyle MIASMA = new GasStyle( "miasma", 0xF070E8, 0x9A1890, 0.5f, 5, 0.37f )
			.detail( WeatherBlobFX.motes( 0xF8A0F0 ), 3 ).glow( 0xEE29DC, 0.08f );

	public static final GasStyle SLOWNESS = new GasStyle( "slowness cloud", 0xB0F0E0, 0x5AA898, 0.45f, 5, 0.37f )
			.detail( WeatherBlobFX.motes( 0xB080F0 ), 3 ).glow( 0x89DFCB, 0.1f ).light();

	public static final GasStyle POP = new GasStyle( "pop gas", 0xE04040, 0x6A1010, 0.5f, 4, 0.4f )
			.detail( WeatherBlobFX.bubbles( 0xF0F0FF ), 2 );

	public static final GasStyle BALL = new GasStyle( "ball gas", 0xFFFFFF, 0xAEB8C2, 0.4f, 4, 0.46f )
			.detail( WeatherBlobFX.sparkles( 0xC0E0FF ), 3 );

	public static final GasStyle ICE_STORM = new GasStyle( "ice storm", 0xE8F4FF, 0x9AB4CC, 0.4f, 6, 0.27f )
			.detail( WeatherBlobFX.FROST, 2 ).glints( 1.5f, false );

	static {
		SMOKE.smoke = true;
		SMOKE.peakHi = 0.9f;
		STENCH.hug = true;
		BALL.rise = 8f;
		CORROSIVE.turn = 0xFF8800;
		CORRUPT.turn = 0xFF8800;
		CONFUSION.wheel = true;
	}

	/** Every style, in the gallery's order. */
	public static final GasStyle[] ALL = { TOXIC, PARALYTIC, CONFUSION, CORROSIVE, STENCH, SMOKE, POISON, DIGESTING_ACID,
			GRASS_SMOKE, WITHER, CORRUPT, CLOUD_OF_CORRUPTION, DEPRESSANT, DIRT, MIASMA, SLOWNESS, POP, BALL, ICE_STORM };

	/** Its body's alpha in a cell holding `cur`: peak x (0.55 + 0.45 x clamp(log10(cur) / 3, 0, 1)). */
	public static float thickness( float peak, int cur ){
		float k = cur <= 1 ? 0f : Math.max( 0f, Math.min( 1f, (float)Math.log10( cur ) / 3f ) );
		return peak * (0.55f + 0.45f * k);
	}

	/** The confusion wheel's colour at a time, s. */
	public static int wheel( float t ){
		float p = t / WHEEL_SECONDS;
		p -= (float)Math.floor( p );
		float f = p * WHEEL.length;
		int i = (int)f;
		return Ramp.lerp( WHEEL[i % WHEEL.length], WHEEL[(i + 1) % WHEEL.length], f - i );
	}

	private GasFactory factory;

	/** Its factory, the same one every time (made the first time it is asked for). */
	public synchronized GasFactory factory(){
		if (factory == null) factory = new GasFactory( this );
		return factory;
	}
}
