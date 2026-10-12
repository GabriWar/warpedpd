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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomeActions;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.Locale;

/**
 * The name a new home is given (the hero can rename it at its hearthstone): after the nearest place
 * with a name within NEAR cells - the town, or a settlement that is not a bandit camp - as
 * "Homestead by Millbrook", "Cave home under Millbrook" below it or "Peak home above Millbrook" over
 * it; with no place near, after the land: "Forest homestead" on the surface, "Cave home" or "Peak
 * home". A name already taken gets a number ("Homestead by Millbrook 2"). A name - its number and
 * all - longer than a home may be called (HomeActions.NAME_MAX, as Rename keeps it) falls back to the
 * short "Millbrook home", and one still too long is cut short before its number.
 */
public final class HomeNames {

	private HomeNames(){}

	/** How far from a place (its well, the town's middle) a home is named after it, in cells. */
	public static final int NEAR = 160;

	/** A new home's name, from where it stands; never one another home has. */
	public static String forNewHome( long seed, int altitude, int wx, int wy ){
		String near = nearestPlace( seed, wx, wy );
		if (near != null){
			String key = altitude < 0 ? "cave_near" : altitude > 0 ? "peak_near" : "surface_near";
			String full = numbered( Messages.get( HomeNames.class, key, near ) );
			if (full.length() <= HomeActions.NAME_MAX) return full;
			return fitted( Messages.get( HomeNames.class, "short_near", near ) );
		}
		if (altitude < 0) return fitted( Messages.get( HomeNames.class, "cave" ) );
		if (altitude > 0) return fitted( Messages.get( HomeNames.class, "peak" ) );
		String biome = Messages.get( OverworldLevel.class,
				"biome_" + WorldModel.biomeAt( seed, wx, wy ).name().toLowerCase( Locale.ENGLISH ) );
		return fitted( Messages.get( HomeNames.class, "surface", biome ) );
	}

	/** The name of the nearest named place within NEAR cells of a world cell (the town, or a
	 *  settlement that is no bandit camp), or null. */
	public static String nearestPlace( long seed, int wx, int wy ){
		String best = null;
		long bestD = (long)NEAR * NEAR + 1;
		long town = (long)wx * wx + (long)wy * wy;
		if (town < bestD){
			best = Messages.get( Waystones.class, "town" );
			bestD = town;
		}
		for (int[] s : WorldStructures.settlementsNear( seed, wx, wy, null, 0, 2 )){
			if (WorldStructures.faction( seed, s[0], s[1] ) == WorldStructures.Faction.BANDIT) continue;
			long d = (long)(s[2] - wx) * (s[2] - wx) + (long)(s[3] - wy) * (s[3] - wy);
			if (d < bestD){
				bestD = d;
				best = WorldStructures.villageName( seed, s[0], s[1] );
			}
		}
		return best;
	}

	//the name, or the name with the first number no home has with it
	private static String numbered( String name ){
		if (!taken( name )) return name;
		for (int n = 2; ; n++){
			String numbered = name + " " + n;
			if (!taken( numbered )) return numbered;
		}
	}

	//as numbered, never longer than HomeActions.NAME_MAX: the name is cut short to leave its number room
	private static String fitted( String name ){
		for (int n = 1; ; n++){
			String number = n == 1 ? "" : " " + n;
			String base = name.length() + number.length() <= HomeActions.NAME_MAX ? name
					: name.substring( 0, HomeActions.NAME_MAX - number.length() ).trim();
			if (!taken( base + number )) return base + number;
		}
	}

	private static boolean taken( String name ){
		for (HomesteadWorld.Home h : HomesteadWorld.homes()) if (name.equals( h.name )) return true;
		return false;
	}
}
