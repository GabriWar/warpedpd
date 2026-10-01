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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

/**
 * How good an item's numbers are. Rolled once when the item is generated; the tier sets
 * how many bonus lines the item carries and how high they roll. Masterwork is not a
 * tier: it is an upgrade state on top of any rarity, see {@link Quality#masterwork}.
 */
public enum Rarity {

	COMMON   ( 0xB8BEC8, 1, 0.00f, 0.30f, 60, 35 ),
	UNCOMMON ( 0x5FB33A, 1, 0.35f, 0.65f, 28, 30 ),
	RARE     ( 0x3F8FDC, 2, 0.55f, 0.85f, 10, 22 ),
	LEGENDARY( 0xB04CFF, 2, 0.70f, 1.00f,  2, 10 ),
	EXOTIC   ( 0xFFB020, 2, 0.85f, 1.00f,  0,  3 );

	public final int color;
	public final int lines;
	public final float minRoll, maxRoll;
	//drop weight at depth 1 and at depth 25, interpolated between
	private final float weightShallow, weightDeep;

	//the white-gold of a fully masterworked item's frame and icon
	public static final int MASTERWORK_COLOR = 0xFFF1B0;

	Rarity( int color, int lines, float minRoll, float maxRoll, float weightShallow, float weightDeep ){
		this.color = color;
		this.lines = lines;
		this.minRoll = minRoll;
		this.maxRoll = maxRoll;
		this.weightShallow = weightShallow;
		this.weightDeep = weightDeep;
	}

	public String title(){
		return Messages.get( this, name().toLowerCase() );
	}

	/** icon cell in {@code sprites/rarity_icons.png} */
	public int icon(){
		return ordinal();
	}

	/**
	 * The tier's own grant: a share of the item's own damage, block or zap power, on top of
	 * whatever its rolled lines give. This is what makes a tier worth finding even before
	 * its lines are read, and it is a fraction, so it grows with every upgrade the item takes.
	 */
	public float potency(){
		switch (this){
			case COMMON:    return 0.05f;
			case UNCOMMON:  return 0.10f;
			case RARE:      return 0.15f;
			case LEGENDARY: return 0.22f;
			default:        return 0.30f;
		}
	}

	/** how much of the gap to a line's maximum one masterwork step closes */
	public float closure(){
		return ordinal() >= LEGENDARY.ordinal() ? 1.0f : this == RARE ? 0.8f : 0.6f;
	}

	public float weight( int depth ){
		if (this == LEGENDARY && depth < 5) return 0;
		if (this == EXOTIC && depth < 15) return 0;
		float t = Math.max( 0f, Math.min( 1f, (depth - 1) / 24f ) );
		return weightShallow + (weightDeep - weightShallow) * t;
	}

	public static Rarity roll( int depth ){
		Rarity[] all = values();
		float[] weights = new float[all.length];
		for (int i = 0; i < all.length; i++) weights[i] = all[i].weight( depth );
		int pick = Random.chances( weights );
		return pick < 0 ? COMMON : all[pick];
	}

	/** the most cores a reroll at the smith can stake */
	public static final int MAX_STAKE = 10;
	/** pockets on the smith's wheel, as on a European roulette */
	public static final int POCKETS = 37;
	/** exotic pockets at the full stake: nine of 37, just under one in four */
	public static final int MAX_EXOTIC_POCKETS = 9;

	/**
	 * How many of the wheel's {@link #POCKETS} pockets each rarity owns for a reroll
	 * staking {@code cores} (1 to {@link #MAX_STAKE}) at this depth. Every pocket is
	 * equally likely, so these counts ARE the odds. One core cuts the wheel like a fresh
	 * drop would; each core past it shrinks common and widens the rarer tiers, and ten
	 * give exotic {@link #MAX_EXOTIC_POCKETS} pockets whatever the depth. A tier with
	 * any chance at all keeps at least one pocket.
	 */
	public static int[] rerollPockets( int depth, int cores ){
		cores = Math.max( 1, Math.min( MAX_STAKE, cores ) );
		float tilt = (cores - 1) / (float)(MAX_STAKE - 1);
		Rarity[] all = values();
		float[] want = new float[all.length];
		float base = 0;
		for (Rarity r : all) base += r.weight( depth );
		float exotic = EXOTIC.weight( depth ) / base;
		exotic += (MAX_EXOTIC_POCKETS / (float)POCKETS - exotic) * tilt;
		//the tilt ignored the depth gate, so a deep stake bought exotic pockets on floor 5
		if (EXOTIC.weight( depth ) <= 0) exotic = 0;
		float rest = 0;
		for (int i = 0; i < all.length; i++){
			if (all[i] == EXOTIC) continue;
			want[i] = all[i].weight( depth ) * (i == 0 ? 1f - 0.7f * tilt : 1f + 1.5f * tilt * i);
			rest += want[i];
		}
		for (int i = 0; i < all.length; i++){
			want[i] = (all[i] == EXOTIC ? exotic : want[i] / rest * (1f - exotic)) * POCKETS;
		}
		//largest remainder, with a pocket kept for every tier that has any chance
		int[] out = new int[all.length];
		int used = 0;
		for (int i = 0; i < all.length; i++){
			out[i] = want[i] > 0 ? Math.max( 1, (int)Math.floor( want[i] ) ) : 0;
			used += out[i];
		}
		while (used < POCKETS){
			int best = -1; float bestRem = -1;
			for (int i = 0; i < all.length; i++){
				float rem = want[i] - out[i];
				if (want[i] > 0 && rem > bestRem){ bestRem = rem; best = i; }
			}
			out[best]++; used++;
		}
		while (used > POCKETS){
			//over by the kept pockets: take from the most crowded tier
			int most = 0;
			for (int i = 1; i < all.length; i++) if (out[i] > out[most]) most = i;
			out[most]--; used--;
		}
		return out;
	}

	/**
	 * The wheel itself: which rarity sits in each of the {@link #POCKETS} pockets, the
	 * rarer tiers spread evenly round it (not bunched), so the ball's run past them reads.
	 */
	public static Rarity[] rerollWheel( int depth, int cores ){
		int[] counts = rerollPockets( depth, cores );
		Rarity[] all = values();
		Rarity[] wheel = new Rarity[POCKETS];
		//rarest first, each tier stepping round the wheel at its own even spacing
		for (int t = all.length - 1; t >= 0; t--){
			int n = counts[t];
			for (int k = 0; k < n; k++){
				int at = Math.round( (k + 0.5f) * POCKETS / (float)n + t * 3 ) % POCKETS;
				while (wheel[at] != null) at = (at + 1) % POCKETS;
				wheel[at] = all[t];
			}
		}
		return wheel;
	}

	public float rollLine(){
		return minRoll >= maxRoll ? minRoll : Random.Float( minRoll, maxRoll );
	}
}
