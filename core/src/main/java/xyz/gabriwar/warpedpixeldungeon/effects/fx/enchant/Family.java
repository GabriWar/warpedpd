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
 * The fifteen ways an enchantment or a glyph acts out its proc, each one verb with its own
 * silhouette (docs/fx/enchant.md; the art spec's families): what its core, follow-through,
 * linger and death accent are, how it gathers on a gained item and ends on its owner. A curse
 * plays its base family with the hex over it (HexOverlay, EnchantFx.CURSE). Each has its class
 * here (EmberFx, RimeFx, ...) with play, finisher, gather and finale; EnchantFx dispatches to it.
 */
public enum Family {
	/** Fire licks upward: tongues climbing the body, embers, a scorch. */
	EMBER,
	/** Frost locks, then cracks: ice shards, a mist, rime spreading on the floor. */
	RIME,
	/** Lightning forks: three legs of static, twigs to the ground, an etched mark. */
	STORM,
	/** Venom drips and eats the floor: a droplet arc, bubbles, an etch fizzing. */
	VENOM,
	/** Growth sprouts from below: blades shooting up, petals, a green pool rolling out. */
	VERDANT,
	/** Dread pours in and an eye opens: a closing ring, ink under the feet, wisps. */
	DREAD,
	/** Death falls: a downward scythe stroke, ash, a skull rising off the body. */
	REAPER,
	/** A blade opens the body: a wound, a crescent with a blood rim, droplets, a splat. */
	CUT,
	/** A drain draws home: beads of the defender's blood along a lifeline into the wielder. */
	DRAIN,
	/** Force rings out: a shock ring on the ground, dust, pebbles, a crack. */
	FORCE,
	/** A ward locks into runes: an arc on the threatened side, a bubble, runes snapping into a ring. */
	WARD,
	/** Radiance falls from above: a shaft of light, holy motes, a pool. */
	RADIANCE,
	/** Fortune glints and spins: a winking star, glints popping in an arc, coins. */
	FORTUNE,
	/** A mirage leaves ghosts: an afterimage, an upright ring (space folding), a ghost streak. */
	MIRAGE,
	/** A heart floats up: hearts off the target, pink motes. */
	HEART
}
