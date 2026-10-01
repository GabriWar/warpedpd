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
 * How strong an item's inherent perk is: gamma is the weakest, alpha the strongest and
 * the only one that also unlocks the perk's alpha twist. Rolled on generation, raised
 * with a {@link TypeShifter}, never lowered.
 */
public enum ItemType {

	GAMMA( 1, 0xC9884A, 70, 40 ),
	BETA ( 2, 0xC8D2E0, 25, 40 ),
	ALPHA( 3, 0xF0C040,  5, 20 );

	/** perk multiplier: x1 / x2 / x3 */
	public final int rank;
	public final int color;
	private final float weightShallow, weightDeep;

	ItemType( int rank, int color, float weightShallow, float weightDeep ){
		this.rank = rank;
		this.color = color;
		this.weightShallow = weightShallow;
		this.weightDeep = weightDeep;
	}

	public String title(){
		return Messages.get( this, name().toLowerCase() );
	}

	/** icon cell in {@code sprites/rarity_icons.png}; the 7px badge sheet uses ordinal() */
	public int icon(){
		return 6 + ordinal();
	}

	public boolean alpha(){
		return this == ALPHA;
	}

	public ItemType next(){
		return this == ALPHA ? ALPHA : values()[ordinal() + 1];
	}

	/** shifters needed to reach the next type: gamma->beta 1, beta->alpha 2 */
	public int shiftCost(){
		return this == GAMMA ? 1 : 2;
	}

	public float weight( int depth ){
		float t = Math.max( 0f, Math.min( 1f, (depth - 1) / 24f ) );
		return weightShallow + (weightDeep - weightShallow) * t;
	}

	public static ItemType roll( int depth ){
		ItemType[] all = values();
		float[] weights = new float[all.length];
		for (int i = 0; i < all.length; i++) weights[i] = all[i].weight( depth );
		int pick = Random.chances( weights );
		return pick < 0 ? GAMMA : all[pick];
	}
}
