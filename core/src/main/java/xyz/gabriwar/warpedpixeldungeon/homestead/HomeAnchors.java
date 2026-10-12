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


package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;

import java.util.function.IntConsumer;

/**
 * What makes a place a HOME, as the wild sees it: every hearthstone (a HomesteadWorld home on the
 * slice) and whatever the providers name - a lit hearth - is an ANCHOR, and no wild animal is set
 * down within RADIUS cells of one (OverworldLevel.populateFauna and randomRespawnCell, through
 * Shelter.wildlifeBarred). The radius is a square's (the larger of the two steps), as the
 * settlements' berths are.
 *
 * The hearthstones are built in. The rest come from PROVIDERS, looked up by class name (a missing
 * one is skipped), each a public class with a public no-argument constructor implementing Provider
 * that hands over the window cells of its anchors; one made in code (a test, a debug scene) is
 * added with register. The anchors of a window are worked out at most once a turn (and again when
 * a home or a built piece comes or goes).
 *
 * <pre>
 * public class StationAnchors implements HomeAnchors.Provider {
 *     public void anchors( OverworldLevel level, IntConsumer cells ){
 *         for (Mob m : level.mobs) if (m instanceof Hearth && ((Hearth) m).lit()) cells.accept( m.pos );
 *     }
 * }
 * </pre>
 */
public final class HomeAnchors {

	private HomeAnchors(){}

	/** How far round an anchor no wild animal is set down, in cells. */
	public static final int RADIUS = 10;

	/** A part of the homesteads whose things anchor a home. */
	public interface Provider {
		/** Hands over the window cells of this slice's window that anchor a home now. */
		void anchors( OverworldLevel level, IntConsumer cells );
	}

	private static final String PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/** The providers, by class name. */
	public static final String[] PROVIDERS = {
			PKG + "stations.StationAnchors",   //stations: a lit hearth
			PKG + "torches.TorchAnchors"       //a standing torch
	};

	private static final Providers<Provider> providers = new Providers<>( Provider.class, PROVIDERS );

	/** Adds a provider made in code (a test, a debug scene). */
	public static void register( Provider p ){
		providers.add( p );
		synchronized (HomeAnchors.class){
			cachedFor = null;
		}
	}

	public static void unregister( Provider p ){
		providers.remove( p );
		synchronized (HomeAnchors.class){
			cachedFor = null;
		}
	}

	//the last window's anchors, and what they were worked out for
	private static OverworldLevel cachedFor;
	private static int cachedWindow, cachedHomes, cachedStore;
	private static float cachedTurn;
	private static int[] cached = new int[0];

	/** The window cells anchoring a home on this slice's window now: its hearthstones in the window
	 *  and every provider's anchors. Kept for the moment of the global clock (FarmCrop.clock, not
	 *  the level's own Actor.now): an anchor may end by that clock alone (a hearth's fire burning
	 *  out, StationSlice), and the level's clock is pulled back (Actor.fixTime) and cleared with each
	 *  level. */
	public static synchronized int[] cells( OverworldLevel level ){
		float now = FarmCrop.clock();
		if (level == cachedFor && level.windowVersion() == cachedWindow && now == cachedTurn
				&& HomesteadWorld.version() == cachedHomes && level.homestead().version() == cachedStore){
			return cached;
		}
		final int[][] out = { new int[8] };
		final int[] n = { 0 };
		IntConsumer add = c -> {
			if (c < 0 || c >= level.length()) return;
			if (n[0] == out[0].length) out[0] = java.util.Arrays.copyOf( out[0], n[0] * 2 );
			out[0][n[0]++] = c;
		};
		int depth = WorldLayers.depthOf( level.altitude() );
		for (HomesteadWorld.Home h : HomesteadWorld.homes()){
			if (h.depth != depth) continue;
			int c = level.localCell( h.wx, h.wy );
			if (c != -1) add.accept( c );
		}
		for (Provider p : providers.all()) p.anchors( level, add );
		cached = java.util.Arrays.copyOf( out[0], n[0] );
		cachedFor = level;
		cachedWindow = level.windowVersion();
		cachedTurn = now;
		cachedHomes = HomesteadWorld.version();
		cachedStore = level.homestead().version();
		return cached;
	}

	/** Is this window cell within RADIUS of an anchor? */
	public static boolean near( OverworldLevel level, int cell ){
		int w = level.width(), x = cell % w, y = cell / w;
		for (int a : cells( level )){
			if (Math.abs( a % w - x ) <= RADIUS && Math.abs( a / w - y ) <= RADIUS) return true;
		}
		return false;
	}
}
