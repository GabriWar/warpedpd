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
 * The thirteen ways an enchanted item shows its magic while it is only held, lying or flying
 * (docs/fx/enchant.md; the art spec's item motifs): in a slot or a window (ench-item-looks' UI
 * motes), on a heap, shed along a flight, at the weapon side in a hand, and as a swing's trail
 * (MotifFx draws the world's). Each has its life and the period between two of them at enchant
 * level 0; higher levels come denser: periodAt(level) = period / (1 + 0.3 level).
 */
public enum Motif {
	/** A mote leaving the top edge, rising 6-9 px/s with a sway, its ramp going hot to dark. */
	RISE( 0.9f, 0.6f ),
	/** A mote leaving the bottom edge, falling 3-5 px/s, twinkling. */
	FALL( 1.1f, 0.7f ),
	/** A bead forming at the tip, falling 6 px and popping. */
	DRIP( 1.0f, 1.0f ),
	/** A drip run backwards: a bead rising into the blade and gone. */
	SIP( 1.0f, 1.0f ),
	/** A bubble forming, rising 3 px and popping. */
	BUBBLE( 0.8f, 0.8f ),
	/** An ink wisp rising 4 px and curling, with a 1 px glint of the rim. */
	SEEP( 1.4f, 1.0f ),
	/** A 2-4 px jag of static between two edge points, never closer than 0.4 s to the last. */
	CRACKLE( 0.06f, 1.0f ),
	/** Two or three motes on a flat ellipse round it, the far half dimmer. */
	ORBIT( 1.0f, 1.0f ),
	/** A two-frame star sweeping three pixels of the edge. */
	GLINT( 0.3f, 1.2f ),
	/** A blade popping up at the base, swaying, wilting; a petal now and then. */
	SPROUT( 1.6f, 1.2f ),
	/** A flat ring under it, RING_5 to RING_9, on a beat. */
	THROB( 0.4f, 0.9f ),
	/** A copy of the item's frame at 0.3 in its rim colour, sliding 2-3 px. */
	GHOST( 1.6f, 1.6f ),
	/** The backlight walking a list of colours, 0.8-1.2 s each (no mote of its own). */
	CYCLE( 1.0f, 1.0f );

	/** How long one of it lives, s. */
	public final float life;
	/** The seconds between two of it at enchant level 0. */
	public final float period;

	Motif( float life, float period ){
		this.life = life;
		this.period = period;
	}

	/** The seconds between two of it at an enchant level: denser as it rises, bounded. */
	public float periodAt( int level ){
		return period / (1f + 0.3f * Math.max( 0, Math.min( 5, level ) ));
	}
}
