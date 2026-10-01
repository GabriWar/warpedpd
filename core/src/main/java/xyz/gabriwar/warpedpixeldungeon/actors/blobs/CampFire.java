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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

/**
 * The fire pit of a Wayfarer's Camp: a slow, low burn that one lighting keeps going for
 * most of a night. Warm enough to sit by and to dry out beside, never hot enough to work
 * metal or to be dangerous a step away.
 */
public class CampFire extends CoalBed {

	@Override
	protected int burnTurns(){ return 400; }

	@Override
	protected float heat(){ return 8f; }

	@Override
	protected float radiate(){ return 5f; }
}
