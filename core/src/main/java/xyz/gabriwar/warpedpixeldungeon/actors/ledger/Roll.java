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

package xyz.gabriwar.warpedpixeldungeon.actors.ledger;

/**
 * The to-hit roll of a blow or a zap, as Char.hit made it. The stats are the accuracy and evasion
 * it rolled against (after every rule that changed them: accSteps and defSteps end on them); the
 * rolls are the two random numbers after their own multipliers (accRollSteps and defRollSteps: a
 * BASE, the raw roll, then each multiplier). It hits when accRoll >= defRoll.
 */
public final class Roll {

	public float accStat, defStat, accRoll, defRoll;
	/** the product of the multipliers on each roll (1 when none): the chance reads them */
	public float accMul = 1f, defMul = 1f;
	public boolean hit;
	/** sure: accuracy was infinite (it could not miss); untouchable: evasion was (it could not hit) */
	public boolean sure, untouchable;
	public int reasonIcon = -1;                   //Char.hit's hitMissIcon, as given
	public Step[] accSteps = {}, defSteps = {};   //what made the two stats
	public Step[] accRollSteps = {}, defRollSteps = {};

	/** The chance it had to hit, from the stats and the multipliers on the rolls: two uniform
	 *  rolls, A = accStat * accMul against D = defStat * defMul. */
	public float chance(){
		if (untouchable) return 0f;
		if (sure) return 1f;
		return chance( accStat * accMul, defStat * defMul );
	}

	public static float chance( float a, float d ){
		if (a <= 0f) return d <= 0f ? 1f : 0f;
		if (d <= 0f) return 1f;
		return a >= d ? 1f - d / (2f * a) : a / (2f * d);
	}
}
