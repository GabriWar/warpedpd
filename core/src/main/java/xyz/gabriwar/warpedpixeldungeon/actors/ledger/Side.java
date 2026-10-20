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
 * One side of a hit as it was just before it: plain values only, never a Char, a Buff or a picture,
 * so a record outlives everything it names.
 */
public final class Side {

	public String name;                          //as shown then (title case)
	public String cls;                           //Class.getName() of the character, null for the hero
	public boolean hero;
	public int heroClass = -1, heroTier;         //the hero's HeroClass ordinal and armour tier (his look)
	public int hp, ht, shield;                   //before the hit
	public int[] buffIcons = {}, buffTints = {}; //BuffIndicator ids (not chip codes) and their icon tints
	public String[] buffNames = {}, buffClasses = {};   //buffs with an icon only
	public String weapon, armor;                 //the hero's: names as shown (enchantment, level), null otherwise
	public String weaponRarity, armorRarity;     //their rarity's name, null for none
	public int weaponColor, armorColor;          //their rarity's colour, 0 for none
}
