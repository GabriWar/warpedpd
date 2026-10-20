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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

/**
 * How an enchanted item's glow lives (the art spec's glow profiles): value(t, phase) is the share
 * of the glow's colour on the item at t s (phase: the item's own offset, s), a pure function of
 * them both - no random numbers of either kind, the same on every frame, thread and machine.
 * ItemSprite (ench-item-looks) draws it; the numbers live here.
 *
 * The lasting ones (lasting) keep within MOST_SWING of their base, as every lasting light in
 * the kit does; the beating ones (PULSE, FLICKER, HEARTBEAT, CURSE) beat at 3 a second or
 * fewer. For INVERT and CURSE the value is how far the item darkens (its rim colour added at
 * RIM_SHARE of it), not a tint.
 */
public enum Profile {

	/** Today's glow: a triangle from 0 to 0.6 and back over two periods (pulse(t, period)). */
	PULSE( 0.3f, false ){
		@Override
		public float value( float t, float phase ){
			return pulse( t + phase, 1f );
		}
	},
	/** Fire: 0.30 swaying on two sines, and every 3-7 s a 0.4 s dip to 0.18. */
	SMOULDER( 0.30f, true ){
		@Override
		public float value( float t, float phase ){
			float x = t + phase;
			float v = base + 0.07f * (float)Math.sin( 4.4f * x ) + 0.05f * (float)Math.sin( 11.9f * x );
			int w = (int)Math.floor( x / DIP_WINDOW );
			float at = DIP_AFTER + DIP_SPREAD * hash( w, phase ), in = x - w * DIP_WINDOW - at;
			if (in >= 0 && in < DIP_FOR){
				float k = (float)Math.sin( Math.PI * in / DIP_FOR );
				v += (DIP_TO - v) * k;
			}
			return v;
		}
	},
	/** Cold, venom, growth: 0.30 breathing 0.12 either way at 0.35 Hz. */
	BREATH( 0.30f, true ){
		@Override
		public float value( float t, float phase ){
			return base + MOST_SWING * (float)Math.sin( 2.2f * (t + phase) );
		}
	},
	/** Static: 0.18, blipping to 0.45 for a frame or two at 0.3-1.2 s apart (never 3 a second). */
	FLICKER( 0.18f, false ){
		@Override
		public float value( float t, float phase ){
			float x = t + phase;
			int w = (int)Math.floor( x / BLIP_WINDOW );
			float at = BLIP_SPREAD * hash( w, phase + 7.3f );
			float len = BLIP_MIN + (BLIP_MAX - BLIP_MIN) * hash( w + 977, phase );
			float in = x - w * BLIP_WINDOW - at;
			return in >= 0 && in < len ? BLIP : base;
		}
	},
	/** Blood and drain: a lub (0.45, 0.08 s), a dub (0.38, 0.22 s later), a rest (0.15), 1.4 s. */
	HEARTBEAT( 0.15f, false ){
		@Override
		public float value( float t, float phase ){
			float k = mod( t + phase, BEAT );
			if (k < BEAT_FOR) return LUB;
			if (k >= DUB_AT && k < DUB_AT + BEAT_FOR) return DUB;
			return base;
		}
	},
	/** Metal, holy, fortune: 0.30, 0.06 either way at 3 Hz. */
	SHIMMER( 0.30f, true ){
		@Override
		public float value( float t, float phase ){
			return base + 0.06f * (float)Math.sin( 2 * Math.PI * 3f * (t + phase) );
		}
	},
	/** Wards and stillness: 0.30, never moving. */
	STEADY( 0.30f, true ){
		@Override
		public float value( float t, float phase ){
			return base;
		}
	},
	/** A steady 0.35 while its hue walks the look's colours (cycleColor). */
	CYCLE( 0.35f, true ){
		@Override
		public float value( float t, float phase ){
			return base;
		}
	},
	/** The dark ones: darkening by 0.15, 0.12 either way at 0.25 Hz, the rim colour showing. */
	INVERT( 0.15f, true ){
		@Override
		public float value( float t, float phase ){
			return base + MOST_SWING * (float)Math.sin( 1.6f * (t + phase) );
		}
	},
	/** A known curse: INVERT slowed to a 4 s beat, darkening from 0.05 to 0.45. */
	CURSE( 0.25f, false ){
		@Override
		public float value( float t, float phase ){
			return base + 0.2f * (float)Math.sin( 2 * Math.PI * 0.25f * (t + phase) );
		}
	};

	/** Where it rests (its middle, or the level it blips or beats from). */
	public final float base;
	/** Whether it is a lasting glow (within MOST_SWING of its base) rather than a beat. */
	public final boolean lasting;

	Profile( float base, boolean lasting ){
		this.base = base;
		this.lasting = lasting;
	}

	/** The share of the colour (or of the dark, INVERT and CURSE) at t s, the item `phase` s along. */
	public abstract float value( float t, float phase );

	/** How far a lasting glow may move off its base, either way; how much of the rim a dark one shows. */
	public static final float MOST_SWING = 0.12f, RIM_SHARE = 0.15f;
	//smoulder's dips: one in each DIP_WINDOW s, DIP_AFTER-DIP_AFTER+DIP_SPREAD s into it (so 3-7 s apart)
	static final float DIP_WINDOW = 5f, DIP_AFTER = 1f, DIP_SPREAD = 2f, DIP_FOR = 0.4f, DIP_TO = 0.18f;
	//flicker's blips: one in each BLIP_WINDOW s (so 0.3-1.2 s apart), a frame or two long
	static final float BLIP_WINDOW = 0.75f, BLIP_SPREAD = 0.45f, BLIP_MIN = 0.033f, BLIP_MAX = 0.05f, BLIP = 0.45f;
	//the heartbeat
	static final float BEAT = 1.4f, BEAT_FOR = 0.08f, DUB_AT = 0.22f, LUB = 0.45f, DUB = 0.38f;

	/** Today's glow (ItemSprite's triangle): 0 to 0.6 over `period` s, and back. */
	public static float pulse( float t, float period ){
		float p = Math.max( 0.01f, period );
		float k = mod( t, 2 * p ) / p;
		return 0.6f * (k <= 1f ? k : 2f - k);
	}

	/** CYCLE's colour at t s: each of `colors` for `each` s, the last 0.25 s of each blending into the next. */
	public static int cycleColor( int[] colors, float t, float each ){
		if (colors == null || colors.length == 0) return 0xFFFFFF;
		float e = Math.max( 0.1f, each );
		float x = mod( t, e * colors.length );
		int i = (int)(x / e) % colors.length;
		float into = x - i * e, blend = Math.min( 0.25f, e / 2f );
		int c = colors[i] & 0xFFFFFF;
		if (into > e - blend){
			c = xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp.lerp( c, colors[(i + 1) % colors.length] & 0xFFFFFF, (into - (e - blend)) / blend );
		}
		return c;
	}

	static float mod( float x, float m ){
		float r = x % m;
		return r < 0 ? r + m : r;
	}

	/** A number in [0, 1) from a whole number and a phase, the same every time: what stands in for
	 *  random in a pure glow. */
	static float hash( int k, float phase ){
		int h = k * 374761393 + Math.round( phase * 1000f ) * 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (float)0x1000000;
	}
}
