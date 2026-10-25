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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OasisDeer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OasisTrader;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.Dates;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The life of the desert's places on the surface (Drylands), on the host alone - a network mirror
 * is handed what it makes:
 *   - a place is FOUND the first time its heart has been seen: a line to the heroes near it and
 *     its page of the guide;
 *   - an oasis keeps its TRADER on his rug whenever none is about (OverworldLevel.populateSites,
 *     never in front of the hero);
 *   - its palms drop DATES at their feet every DATES_DAYS world days, on a few of them, where
 *     none are lying yet;
 *   - at DUSK, while nobody is within SHY cells of its water, DRINKERS come down to drink - a few
 *     deer on its shore, out of everyone's sight as they arrive - once a day; they slip back into
 *     the desert when the dusk is over (OasisDeer).
 * Everything kept is the level's once- and periodic keys (claimHoard, dueOnce): a place's sector key
 * mixed with this class's salts.
 */
public final class DrylandsLife {

	private DrylandsLife(){}

	private static final long FOUND = 0x0F0D5EE7L, DATES = 0x0DA7E5L, DRINK = 0x0D121C4L;

	/** Dates fall every DATES_DAYS world days, under DATE_PALMS palms at most. */
	public static final int DATES_DAYS = 2, DATE_PALMS = 3;
	/** The dusk's drinkers keep away while a hero is within SHY cells of the water; how many come. */
	public static final int SHY = 12, DRINKERS_MIN = 2, DRINKERS_MAX = 3;
	//a place found is heard of by the heroes within this many cells of its heart
	private static final int FOUND_HEARD = 30;

	/** OverworldLevel.populateSites: the oasis in this sector, once it is wholly in the window - its
	 *  trader when none is about, and the dates when they are due. */
	static void populate( OverworldLevel level, int sx, int sy ){
		if (level.isMirror()) return;
		Drylands.Place p = new Drylands.Place( WorldStructures.Site.OASIS, sx, sy,
				WorldStructures.siteX( level.worldSeed(), sx, sy ), WorldStructures.siteY( level.worldSeed(), sx, sy ) );
		if (!whole( level, p )) return;
		trader( level, p );
		dates( level, p );
	}

	/** OverworldLevel.tickWorld, a host step on the surface: the places the window holds found,
	 *  their dates and their drinkers. */
	static void tick( OverworldLevel level, int heroPos ){
		if (level.isMirror()) return;
		for (Drylands.Place p : Drylands.places( level.worldSeed(), level.worldX(), level.worldY(),
				level.worldX() + level.width() - 1, level.worldY() + level.height() - 1 )){
			found( level, p );
			if (p.type != WorldStructures.Site.OASIS || !whole( level, p )) continue;
			dates( level, p );
			drinkers( level, p );
		}
	}

	//is the place, a cell round it and all, inside the window's peopled band (a half-seen place
	//never burns a once-key)?
	static boolean whole( OverworldLevel level, Drylands.Place p ){
		int r = Drylands.reach( p.type ) + 1;
		int x0 = p.cx - r - level.worldX(), y0 = p.cy - r - level.worldY();
		int x1 = p.cx + r - level.worldX(), y1 = p.cy + r - level.worldY();
		return x0 >= 2 && y0 >= 2 && x1 <= level.width() - 3 && y1 <= level.height() - 3;
	}

	// ------------------------------------------------------------------ found

	private static void found( OverworldLevel level, Drylands.Place p ){
		int c = level.localCell( p.cx, p.cy );
		if (c == -1 || level.visited == null || !level.visited[c]) return;
		if (!level.claimHoard( p.key() ^ FOUND )) return;
		boolean oasis = p.type == WorldStructures.Site.OASIS;
		String line = Messages.get( Drylands.class, oasis ? "found_oasis" : "found_temple" );
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (level.distance( h.pos, c ) <= FOUND_HEARD) NetManager.heroLog( h, GLog.HIGHLIGHT + line );
		}
		GuideGraph.reveal( oasis ? GuideGraph.TAG_OASIS : GuideGraph.TAG_TEMPLE );
	}

	// ------------------------------------------------------------------ the oasis

	private static void trader( OverworldLevel level, Drylands.Place p ){
		int r = Drylands.OASIS_REACH + 2;
		if (level.census( OasisTrader.class, p.cx - r, p.cy - r, p.cx + r, p.cy + r ) > 0) return;
		int[] rug = Drylands.rugCell( level.worldSeed(), p.sx, p.sy );
		int c = level.localCell( rug[0], rug[1] );
		if (c == -1 || level.map[c] != Terrain.CUSTOM_DECO_EMPTY) return;
		OasisTrader t = OasisTrader.of( p.sx, p.sy );
		t.openShop();
		level.addMob( t, c );
	}

	//ripe dates under a few palms, every DATES_DAYS days, where none lie already; a different few
	//each time (the day turns the choice)
	private static void dates( OverworldLevel level, Drylands.Place p ){
		if (!level.dueOnce( p.key() ^ DATES, DATES_DAYS )) return;
		long seed = level.worldSeed();
		int r = Drylands.OASIS_REACH;
		ArrayList<Integer> under = new ArrayList<>();
		for (int wy = p.cy - r; wy <= p.cy + r; wy++){
			for (int wx = p.cx - r; wx <= p.cx + r; wx++){
				if (Drylands.terrain( seed, p.sx, p.sy, WorldStructures.Site.OASIS, wx, wy ) != Terrain.CUSTOM_DECO) continue;
				int palm = level.localCell( wx, wy );
				int c = level.localCell( wx, wy + 1 );
				if (palm == -1 || c == -1 || level.map[palm] != Terrain.CUSTOM_DECO) continue;
				if (!level.passable[c] || level.water[c] || level.heaps.get( c ) != null || Actor.findChar( c ) != null) continue;
				under.add( c );
			}
		}
		Random.shuffle( under );
		for (int i = 0; i < Math.min( DATE_PALMS, under.size() ); i++){
			Heap h = level.drop( new Dates().quantity( Random.IntRange( 1, 2 ) ), under.get( i ) );
			if (h != null && h.sprite != null) h.sprite.drop();
		}
	}

	//a few deer on the shore at dusk, while nobody is at the water, out of everyone's sight
	private static void drinkers( OverworldLevel level, Drylands.Place p ){
		if (DayNightCycle.phase() != DayNightCycle.Phase.DUSK) return;
		int heart = level.localCell( p.cx, p.cy );
		if (heart == -1) return;
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (level.distance( h.pos, heart ) < SHY) return;
		}
		ArrayList<Integer> shore = shore( level, p );
		if (shore.size() < DRINKERS_MIN || !level.dueOnce( p.key() ^ DRINK, 1 )) return;
		Random.shuffle( shore );
		int n = Math.min( shore.size(), Random.IntRange( DRINKERS_MIN, DRINKERS_MAX ) );
		for (int i = 0; i < n; i++) level.addMob( new OasisDeer(), shore.get( i ) );
	}

	//the oasis' dry ground with its water beside it, free and out of every hero's sight
	static ArrayList<Integer> shore( OverworldLevel level, Drylands.Place p ){
		ArrayList<Integer> out = new ArrayList<>();
		int r = Drylands.OASIS_REACH;
		for (int wy = p.cy - r; wy <= p.cy + r; wy++){
			for (int wx = p.cx - r; wx <= p.cx + r; wx++){
				int c = level.localCell( wx, wy );
				if (c == -1 || !level.passable[c] || level.water[c] || level.occupied( c )) continue;
				if (Drylands.terrain( level.worldSeed(), p.sx, p.sy, WorldStructures.Site.OASIS, wx, wy ) == -1) continue;
				boolean wet = false;
				for (int n : PathFinder.NEIGHBOURS4){
					int k = c + n;
					if (k >= 0 && k < level.length() && level.water[k]) wet = true;
				}
				if (wet && !NetManager.anyHeroSees( c )) out.add( c );
			}
		}
		return out;
	}
}
