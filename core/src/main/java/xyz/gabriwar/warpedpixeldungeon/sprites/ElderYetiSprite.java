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


package xyz.gabriwar.warpedpixeldungeon.sprites;

/** An elder yeti roaming the cold lands (actors/mobs/ElderYeti, tools/assets/events_life_art.py): the
 *  yeti's frames in a frost-blue coat gone grey at the face, with pale glowing eyes, and a size up
 *  on its kin. */
public class ElderYetiSprite extends YetiSprite {

	public static final String TEXTURE = "sprites/elder_yeti.png";

	public ElderYetiSprite() {
		super();
		texture( TEXTURE );
		scale.set( 1.25f );
		play( idle, true );
	}
}
