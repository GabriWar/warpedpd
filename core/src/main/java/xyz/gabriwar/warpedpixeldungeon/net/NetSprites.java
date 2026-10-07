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

package xyz.gabriwar.warpedpixeldungeon.net;

/**
 * How a mob's sprite class is named on the wire: its binary name inside the sprites package, so a
 * nested sprite - a wisp's colour (CrystalWispSprite$Blue), an elemental's (ElementalSprite$Fire) -
 * reaches a co-op guest whole, and SpectatorReceiver.resolveSpriteClass finds it again with
 * Class.forName. A top-level sprite's name is its simple name as it always was, so an older host
 * and a newer guest still understand each other.
 */
public final class NetSprites {

	private NetSprites(){}

	public static final String PACKAGE = "xyz.gabriwar.warpedpixeldungeon.sprites.";

	public static String wireName( Class<?> cls ){
		String n = cls.getName();
		return n.startsWith( PACKAGE ) ? n.substring( PACKAGE.length() ) : cls.getSimpleName();
	}
}
