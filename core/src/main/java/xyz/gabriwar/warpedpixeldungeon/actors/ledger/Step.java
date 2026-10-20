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
 * One line of a breakdown: the running value before and after one rule. A chain of steps starts
 * with a BASE (before == after, the value it starts at); every later step's before is the previous
 * one's after. The op says how to read the change: MUL as a factor (after / before), ADD as an
 * amount, SET as a new value, ROUND as the rounding the game did there.
 */
public final class Step {

	public static final int BASE = 0, ADD = 1, MUL = 2, SET = 3, ROUND = 4;
	/** Whose rule it is: the one who struck, the one struck, or neither (the hour, the season). */
	public static final int ATTACKER = 0, DEFENDER = 1, WORLD = 2;
	public static final int NO_ICON = Integer.MIN_VALUE;

	public int op, side, icon = NO_ICON, tint;   //icon: the chip-code namespace (skill >= 1000, text cell >= 0, buff < 0)
	public String label;                          //resolved and localised when recorded
	public float before, after;

	public Step(){}

	public Step( int op, int side, String label, int icon, int tint, float before, float after ){
		this.op = op;
		this.side = side;
		this.label = label;
		this.icon = icon;
		this.tint = tint;
		this.before = before;
		this.after = after;
	}

	/** after - before */
	public float change(){
		return after - before;
	}

	/** after / before, NaN when before is 0 */
	public float factor(){
		return before == 0f ? Float.NaN : after / before;
	}

	@Override
	public String toString(){
		return label + " [" + op + "] " + before + " -> " + after;
	}
}
