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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.Game;

/**
 * An enchantment's sting: the one sound its proc adds to a blow the hero sees, where its code
 * plays none of its own (a row's ownSound is heard only by co-op guests, the host's code playing
 * it). At most one a blow, the strongest asked for on it (they are gathered for a frame); the
 * same sample never twice within SAME_GAP s on one character, one enchantment at most once per
 * ID_GAP s; LOUDEST-QUIETEST by tier from the row's volume, its pitch the row's give or take 5%;
 * an echo's at ECHO_VOL of it and ECHO_PITCH lower. A blade's (CUT) only from STRONG. Every
 * sample is an existing Assets.Sounds one, through SpatialSound.play (from where the character
 * stands). Render thread.
 */
public final class Sting {

	private Sting(){}

	public static final float SAME_GAP = 0.12f, ID_GAP = 0.25f, ECHO_VOL = 0.3f, ECHO_PITCH = 0.85f,
			QUIETEST = 0.25f, LOUDEST = 0.6f, PITCH_SPREAD = 0.05f;
	/** The row's volume's share by tier. */
	static final float[] TIER_VOL = { 0.7f, 1f, 1.25f, 1.4f };

	/** Where a sting goes: SpatialSound in the game; a test's ear in a test. */
	interface Out {
		void play( String sample, Char at, float volume, float pitch );
	}

	static Out out = SpatialSound::play;

	//when each enchantment last stung
	private static final float[] stungAt = new float[EnchantLooks.COUNT];
	static {
		reset();
	}

	static void reset(){
		for (int i = 0; i < stungAt.length; i++) stungAt[i] = -1f;
	}

	/** A proc on a character's blow asks for its sting (the strongest of the frame plays). */
	static void ask( final EnchantState st, EnchantLook l, Char at, int tier, boolean echo, boolean guest ){
		if (l == null || l.sound == null) return;
		if (l.ownSound && !guest) return;
		if (l.fam == Family.CUT && tier < EnchantFx.STRONG && !l.glyph) return;
		//an echo is the weakest of all; else the stronger tier
		int key = echo ? -1 : tier;
		int had = st.stingLook == null ? -2 : (st.stingEcho ? -1 : st.stingTier);
		if (key > had){
			st.stingLook = l;
			st.stingTier = tier;
			st.stingEcho = echo;
			st.stingAt = at;
		}
		if (!st.stingWaiting){
			st.stingWaiting = true;
			EnchantFx.later( 0f, () -> play( st ) );
		}
	}

	//the frame's strongest, played if its gaps allow
	static void play( EnchantState st ){
		EnchantLook l = st.stingLook;
		Char at = st.stingAt;
		boolean echo = st.stingEcho;
		int tier = st.stingTier;
		st.stingLook = null;
		st.stingAt = null;
		st.stingWaiting = false;
		if (l == null) return;
		float now = Game.timeTotal;
		float last = stungAt[l.code];
		if (last >= 0 && now >= last && now - last < ID_GAP) return;
		if (l.sound.equals( st.lastSample ) && st.lastAt >= 0 && now >= st.lastAt && now - st.lastAt < SAME_GAP) return;
		stungAt[l.code] = now;
		st.lastSample = l.sound;
		st.lastAt = now;
		out.play( l.sound, at, volume( l, tier, echo ), pitch( l, echo ) );
	}

	/** A sting's volume: the row's, by tier, kept between QUIETEST and LOUDEST; an echo's ECHO_VOL of it. */
	public static float volume( EnchantLook l, int tier, boolean echo ){
		float v = Math.max( QUIETEST, Math.min( LOUDEST, l.vol * TIER_VOL[Math.max( 0, Math.min( 3, tier ) )] ) );
		return echo ? v * ECHO_VOL : v;
	}

	private static float pitch( EnchantLook l, boolean echo ){
		float p = l.pitch * FxRandom.Float( 1f - PITCH_SPREAD, 1f + PITCH_SPREAD );
		return echo ? p * ECHO_PITCH : p;
	}
}
