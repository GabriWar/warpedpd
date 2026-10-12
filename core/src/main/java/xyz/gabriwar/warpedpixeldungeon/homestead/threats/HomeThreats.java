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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.AwaySettlers;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a home is to the things that come for it: its AREA (the cells within AREA of its
 * hearthstone, the larger of the two steps, on its own slice), and the nights a wolf pack or a
 * bandit band comes - pure functions of the world's seed, the home and the night (WorldClock.night),
 * so a night rolls the same live, after a reload and when it is settled on the hero's return
 * (ThreatSettler).
 *
 * A WOLF SIEGE falls on a winter night (GameCalendar.seasonOfDay) on a home of the forest, the
 * snowfields or the tundra under the open sky, one night in SIEGE_ODDS (one in SIEGE_ODDS_NOTICED
 * with its notice above the RAID_LINE). A BANDIT RAID falls only on a home whose notice is above
 * the line, one night in RAID_ODDS (one in RAID_ODDS_BANDITS with an outlaw village within
 * BANDIT_REACH sectors).
 */
public final class HomeThreats {

	private HomeThreats(){}

	/** A home's ground: every cell within this many of its hearthstone (the larger of the two steps). */
	public static final int AREA = 15;
	/** A home whose notice (Notice) is above this draws bandits. */
	public static final int RAID_LINE = 30;

	/** A siege on a winter night in wolf country: one night in this many... */
	public static final int SIEGE_ODDS = 4;
	/** ...one in this many with the home's notice above the line. */
	public static final int SIEGE_ODDS_NOTICED = 2;
	/** A raid on a night with the notice above the line: one night in this many... */
	public static final int RAID_ODDS = 6;
	/** ...one in this many with an outlaw village within BANDIT_REACH sectors. */
	public static final int RAID_ODDS_BANDITS = 3;
	/** How near an outlaw village makes a home a bandits' target, in sectors (the larger step). */
	public static final int BANDIT_REACH = 3;

	/** A pack's wolves, a band's raiders. */
	public static final int PACK_MIN = 3, PACK_MAX = 5, BAND_MIN = 3, BAND_MAX = 6;
	/** Where a pack or a band comes in: a ring this many cells round the hearthstone, just past
	 *  the home's ground. */
	public static final int EDGE_MIN = AREA + 1, EDGE_MAX = AREA + 5;

	private static final long SALT_SIEGE = 0x51E6E0F1L, SALT_RAID = 0x4A1D0F1EL, SALT_SIZE = 0x5112E5L;
	/** The salt of when in its night a siege or a raid wakes a sleeper (AlarmBell.sleepFor). */
	static final long SALT_WAKE = 0x3A4EB311L;

	// ----------------------------------------------------------------- homes

	/** A home's key in the threats' own records: its slice and world cell. */
	public static String key( HomesteadWorld.Home home ){
		return home.depth + ":" + home.wx + ":" + home.wy;
	}

	/** The home of a key (key), or null when that hearthstone is gone. */
	public static HomesteadWorld.Home home( String key ){
		for (HomesteadWorld.Home h : HomesteadWorld.homes()) if (key( h ).equals( key )) return h;
		return null;
	}

	/** The homes of a slice, oldest first. */
	public static List<HomesteadWorld.Home> homesOn( OverworldLevel level ){
		int depth = WorldLayers.depthOf( level.altitude() );
		ArrayList<HomesteadWorld.Home> out = new ArrayList<>();
		for (HomesteadWorld.Home h : HomesteadWorld.homes()) if (h.depth == depth) out.add( h );
		return out;
	}

	/** Is this home on that slice? */
	public static boolean on( OverworldLevel level, HomesteadWorld.Home home ){
		return home.depth == WorldLayers.depthOf( level.altitude() );
	}

	/** The window cell of a home's hearthstone, -1 when it is off the window (or another slice's). */
	public static int hearth( OverworldLevel level, HomesteadWorld.Home home ){
		return on( level, home ) ? level.localCell( home.wx, home.wy ) : -1;
	}

	/** How far a window cell is from a home's hearthstone, the larger of the two steps. */
	public static int distance( OverworldLevel level, HomesteadWorld.Home home, int cell ){
		int w = level.width();
		return Math.max( Math.abs( level.worldX + cell % w - home.wx ), Math.abs( level.worldY + cell / w - home.wy ) );
	}

	/** Is a window cell on this home's ground? */
	public static boolean inArea( OverworldLevel level, HomesteadWorld.Home home, int cell ){
		return on( level, home ) && cell >= 0 && cell < level.length() && distance( level, home, cell ) <= AREA;
	}

	/** Is a window cell AT a home, as the homesteads count a visit: within AwaySettlers.REACH of its
	 *  hearthstone (the larger of the two steps), on its slice? */
	public static boolean atHome( OverworldLevel level, HomesteadWorld.Home home, int cell ){
		return on( level, home ) && cell >= 0 && cell < level.length() && distance( level, home, cell ) <= AwaySettlers.REACH;
	}

	/** Is the whole of a home's ground inside the window (every chest on it among the level's mobs)? */
	public static boolean wholeInWindow( OverworldLevel level, HomesteadWorld.Home home ){
		int c = hearth( level, home );
		if (c == -1) return false;
		int w = level.width(), x = c % w, y = c / w;
		return x >= AREA && y >= AREA && x < w - AREA && y < level.height() - AREA;
	}

	/** The home whose ground holds a window cell (the oldest, where two meet), or null. */
	public static HomesteadWorld.Home homeAt( OverworldLevel level, int cell ){
		for (HomesteadWorld.Home h : homesOn( level )) if (inArea( level, h, cell )) return h;
		return null;
	}

	/** The world's seed, as the slices are generated from it. */
	public static long worldSeed(){
		return OverworldLevel.worldSeedOf( Dungeon.seed );
	}

	// ----------------------------------------------------------------- the nights (pure)

	//a night's roll for a home, uniform in [0, 1)
	static double roll( long seed, HomesteadWorld.Home home, int night, long salt ){
		long h = WorldEvents.mix( seed ^ salt ^ (home.depth * 0x9E3779B97F4A7C15L),
				OverworldLevel.worldKey( home.wx, home.wy ), night );
		return (h >>> 11) * 0x1.0p-53;
	}

	/** Does wolf country lie under this home: the forest, the snowfields or the tundra, under the
	 *  open sky? */
	public static boolean wolfCountry( long seed, HomesteadWorld.Home home ){
		if (!WorldLayers.openSky( WorldLayers.altitudeOf( home.depth ) )) return false;
		WorldModel.Biome b = WorldModel.biomeAt( seed, home.wx, home.wy );
		return b == WorldModel.Biome.FOREST || b == WorldModel.Biome.SNOWFIELD || b == WorldModel.Biome.TUNDRA;
	}

	/** The wolves of a home's country: the forest's brown, the snow's grey. */
	public static boolean greyWolves( long seed, HomesteadWorld.Home home ){
		return WorldModel.biomeAt( seed, home.wx, home.wy ) != WorldModel.Biome.FOREST;
	}

	/** Does a wolf pack besiege this home on this night (the night's index, WorldClock.night)? */
	public static boolean siegeNight( long seed, HomesteadWorld.Home home, int night, int notice ){
		if (GameCalendar.seasonOfDay( night ) != GameCalendar.Season.WINTER || !wolfCountry( seed, home )) return false;
		int odds = notice > RAID_LINE ? SIEGE_ODDS_NOTICED : SIEGE_ODDS;
		return roll( seed, home, night, SALT_SIEGE ) < 1.0 / odds;
	}

	/** Does a bandit band raid this home on this night? Only a home above the raid line draws one. */
	public static boolean raidNight( long seed, HomesteadWorld.Home home, int night, int notice ){
		if (notice <= RAID_LINE) return false;
		int odds = nearBandits( seed, home.wx, home.wy ) ? RAID_ODDS_BANDITS : RAID_ODDS;
		return roll( seed, home, night, SALT_RAID ) < 1.0 / odds;
	}

	/** How many wolves a siege brings, how many raiders a raid. */
	public static int packSize( long seed, HomesteadWorld.Home home, int night ){
		return PACK_MIN + (int)(roll( seed, home, night, SALT_SIZE ) * (PACK_MAX - PACK_MIN + 1));
	}

	public static int bandSize( long seed, HomesteadWorld.Home home, int night ){
		return BAND_MIN + (int)(roll( seed, home, night, SALT_SIZE ^ SALT_RAID ) * (BAND_MAX - BAND_MIN + 1));
	}

	//an outlaw village within BANDIT_REACH sectors of a world cell's sector: the sites are pure,
	//so the answer is kept per seed and sector
	private static volatile long banditSeed = Long.MIN_VALUE;
	private static final ConcurrentHashMap<Long, Boolean> banditCache = new ConcurrentHashMap<>();

	/** An outlaw village (a BANDIT faction's) within BANDIT_REACH sectors of this world cell. */
	public static boolean nearBandits( long seed, int wx, int wy ){
		if (seed != banditSeed){
			synchronized (banditCache){
				if (seed != banditSeed){
					banditCache.clear();
					banditSeed = seed;
				}
			}
		}
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		long key = WorldStructures.sectorOf( sx, sy );
		Boolean known = banditCache.get( key );
		if (known != null) return known;
		boolean near = false;
		for (int dy = -BANDIT_REACH; dy <= BANDIT_REACH && !near; dy++){
			for (int dx = -BANDIT_REACH; dx <= BANDIT_REACH && !near; dx++){
				near = WorldStructures.siteType( seed, sx + dx, sy + dy ) == WorldStructures.Site.VILLAGE
						&& WorldStructures.faction( seed, sx + dx, sy + dy ) == WorldStructures.Faction.BANDIT;
			}
		}
		banditCache.put( key, near );
		return near;
	}

	/** The way something (dx, dy) cells off lies, as a message: "north", "south-west"... (north is
	 *  up the map). Unlike WorldEvents.direction it never answers "here": a pack or a band seen
	 *  coming always comes from somewhere. */
	public static String bearing( int dx, int dy ){
		int octant = (int) Math.floorMod( Math.round( Math.atan2( -dy, dx ) / (Math.PI / 4) ), 8L );
		return Messages.get( WorldEvents.class, "dir_" + OCTANTS[octant] );
	}

	//WorldEvents' winds, in the same order (counter-clockwise from east)
	private static final String[] OCTANTS = { "e", "ne", "n", "nw", "w", "sw", "s", "se" };

	// ----------------------------------------------------------------- the edge

	/**
	 * Where one of a pack or a band comes in: a free cell of open ground on the ring EDGE_MIN..EDGE_MAX
	 * round the hearthstone, none built on, none in a hero's sight and none inside what the home has
	 * built round itself (a palisade ring, a fence) where one can be had, nearest the bearing `turn`
	 * (0..1 of a full circle) - so a band spreads out round the home. -1 for none.
	 */
	static int edgeCell( OverworldLevel level, int hearth, float turn ){
		int w = level.width(), hx = hearth % w, hy = hearth / w;
		int best = -1, bestScore = Integer.MAX_VALUE;
		double ax = Math.cos( turn * 2 * Math.PI ), ay = Math.sin( turn * 2 * Math.PI );
		//the ground walkable from the hearthstone without crossing anything shut: inside the home's
		//own ring, where a palisade or a fence stands round it
		PathFinder.buildDistanceMap( hearth, level.passable, 2 * EDGE_MAX );
		int[] walk = PathFinder.distance;
		for (int dy = -EDGE_MAX; dy <= EDGE_MAX; dy++){
			for (int dx = -EDGE_MAX; dx <= EDGE_MAX; dx++){
				int r = Math.max( Math.abs( dx ), Math.abs( dy ) );
				if (r < EDGE_MIN) continue;
				int x = hx + dx, y = hy + dy;
				if (x <= 1 || y <= 1 || x >= w - 2 || y >= level.height() - 2) continue;
				int c = x + y * w;
				if (!level.passable[c] || level.pit[c] || Terrain.homestead( level.map[c] )
						|| (Terrain.flags[level.map[c]] & Terrain.LIQUID) != 0 || Actor.findChar( c ) != null) continue;
				double len = Math.sqrt( dx * dx + dy * dy );
				//off the bearing, in tenths of a turn's arc, and a heavy price for being seen
				int score = (int)((1 - (dx * ax + dy * ay) / len) * 50);
				if (level.heroFOV != null && level.heroFOV[c]) score += 1000;
				if (walk[c] != Integer.MAX_VALUE) score += 500;
				if (score < bestScore){
					bestScore = score;
					best = c;
				}
			}
		}
		return best;
	}

	/** Puts a mob of a threat on the level: on the scene when it is the one shown, else straight
	 *  into its mobs (a test, a level being set up). */
	static void add( OverworldLevel level, Mob mob, int cell ){
		mob.pos = cell;
		if (live( level )) GameScene.add( mob );
		else level.mobs.add( mob );
	}

	/** Is this level the one on screen? */
	static boolean live( OverworldLevel level ){
		return level == Dungeon.level && Game.scene() instanceof GameScene;
	}
}
