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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;

/**
 * The skill tree's schools, each with the colour its casts glow in (the cast silhouettes'
 * colours, SkillCastFX.color) and the element whose palette its particles take.
 */
public enum School {

	/** prayers, smites, wards of light */
	HOLY( 0xF1D99C, Element.HOLY ),
	/** stealth, curses, the dead */
	SHADOW( 0xB89CE4, Element.SHADOW ),
	/** the huntress's bow, traps, beasts */
	NATURE( 0xB3D699, Element.NATURE ),
	/** bombs, embers, blood and fury */
	FIRE( 0xDF9979, Element.FIRE ),
	/** frost novas and arrows */
	FROST( 0xA4E9FF, Element.FROST ),
	/** sparks, storms, the slipstream */
	STORM( 0xB9DEEE, Element.ELECTRIC ),
	/** the warrior's earth and shouts */
	MARTIAL( 0xD9AE75, Element.EARTH ),
	/** blades: the duelist's steel */
	STEEL( 0xB9DEEE, Element.STEEL ),
	/** wards, meteors, the rest of magic */
	ARCANE( 0xB89CE4, Element.ARCANE );

	/** The colour its casts glow in. */
	public final int color;
	/** The palette its particles take. */
	public final Element element;

	School( int color, Element element ){
		this.color = color;
		this.element = element;
	}

	/** The school of a cast silhouette (SkillCastFX.sprite's index). */
	public static School ofSilhouette( int sprite ){
		switch (sprite){
			case 8: case 9: case 17:            return HOLY;
			case 2: case 3: case 15: case 19:   return SHADOW;
			case 4: case 5: case 10: case 18: case 23: return NATURE;
			case 20: case 21:                   return FIRE;
			case 12:                            return FROST;
			case 14:                            return STORM;
			case 0: case 1: case 16:            return MARTIAL;
			case 6: case 7: case 22:            return STEEL;
			default:                            return ARCANE;
		}
	}
}
