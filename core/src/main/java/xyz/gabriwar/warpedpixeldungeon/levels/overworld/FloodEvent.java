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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Crop;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.FarmSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.HashMap;

/**
 * The rivers over their banks (WorldEvents.Type.FLOOD). The ground soaks up the rain and drains in
 * the dry (ClimateManager.soak: every turn of rain adds its rate, every turn drains SOAK_DRAIN), and
 * in the wet season - spring and autumn - a long rain that soaks it to SOAK_FLOOD bursts every river
 * of the world over its banks (ClimateManager.floodStart), to MIN_PEAK levels and one more for every
 * SOAK_PER_LEVEL past it, LEVELS at the most; never twice within GAP_TURNS. It is the climate's, so
 * it comes about wherever the hero is, the dungeon too, and the surface finds it when he is back.
 *
 * THE WATER. A river's banks are dealt in levels by how far over the sea's line their ground lies
 * (LEVEL_STEP of elevation each: WindowGenerator's Window.bank, from the world's own sample, so a
 * window worked out on its worker knows its banks), only within the river's own corridor: a lake or
 * the sea never floods. The water climbs a level at a time over RISE_TURNS to its peak, stands there
 * HIGH_TURNS (two days), and drains away over FALL_TURNS. Open ground under its line is shallow water,
 * a road under it mud; from the moment it starts to fall every bank up to its peak lies under MUD
 * until MUD_TURNS after the water is gone. Mud is slow going (homestead/travel/Paths). Nothing the
 * hero built, dug or tilled is covered, nor a way between the slices, nor frozen ground.
 *
 * THE FIELDS. A crop on a bank under the water's line drowns; a field on higher ground ("raised
 * ground": a bank over the peak, or no bank at all) is spared. The flood leaves silt behind: a crop
 * on a bank it covered grows SILT times as fast until SILT_TURNS after the water is gone.
 *
 * THE EVENT. The first time the host's window holds a bank under the water, the flood is written
 * into the log anchored on the nearest such bank to his hero (WorldEventLog.forced), every player
 * hears of it the way it lies, and the world map pins it until the water is gone. Its going is said
 * once. What is laid on the window (water, mud) is the world's overlay (OverworldLevel's scorch),
 * worked out again from the bank levels whenever the water's or the mud's line moves.
 */
public final class FloodEvent {

	private FloodEvent(){}

	private static final int FC = WorldEvents.FC;

	/** How far a river climbs its banks, in levels of LEVEL_STEP elevation each (1 the lowest bank). */
	public static final int LEVELS = 6;
	public static final float LEVEL_STEP = 0.004f;
	public static final int MIN_PEAK = 3;

	/** The water climbs to its peak over RISE_TURNS, stands HIGH_TURNS, drains away over FALL_TURNS;
	 *  its mud lies MUD_TURNS more, its silt feeds the crops SILT_TURNS after the water is gone. */
	public static final int RISE_TURNS = 600, HIGH_TURNS = 2 * FC, FALL_TURNS = FC / 2;
	public static final int MUD_TURNS = 4 * FC, SILT_TURNS = 12 * FC;
	/** How much faster a crop grows on silt. */
	public static final float SILT = 1.5f;

	/** The ground's soaking (ClimateManager.soak): drained every turn, the flood's mark (the longest
	 *  rain of a wet season reaches it about once, NatureEventsTest), a level more of water past it for
	 *  every SOAK_PER_LEVEL, and the most it holds. */
	public static final float SOAK_DRAIN = 0.06f, SOAK_FLOOD = 270f, SOAK_PER_LEVEL = 60f, SOAK_CAP = 1500f;
	/** No flood within this many turns of the last one starting. */
	public static final int GAP_TURNS = 8 * FC;

	/** No flood on record (ClimateManager.floodStart). */
	public static final int NONE = Integer.MIN_VALUE;

	// ------------------------------------------------------------ the climate's word (pure)

	/** Is it the wet season: spring and autumn. */
	public static boolean wetSeason( GameCalendar.Season s ){
		return s == GameCalendar.Season.SPRING || s == GameCalendar.Season.AUTUMN;
	}

	/** Do the rivers burst their banks now: the wet season, the ground soaked to SOAK_FLOOD, above
	 *  freezing, and no flood begun within GAP_TURNS. */
	public static boolean rises( GameCalendar.Season season, float soak, float airC, int lastStart, int turn ){
		if (!wetSeason( season ) || soak < SOAK_FLOOD || airC <= 2f) return false;
		return lastStart == NONE || turn - lastStart >= GAP_TURNS;
	}

	/** The peak a flood the ground was soaked this far climbs to. */
	public static int peakFor( float soak ){
		return Math.max( MIN_PEAK, Math.min( LEVELS, MIN_PEAK + (int)((soak - SOAK_FLOOD) / SOAK_PER_LEVEL) ) );
	}

	/** The turn a flood that began at `start` is gone from the banks (its mud aside). */
	public static int endOf( int start ){
		return start + RISE_TURNS + HIGH_TURNS + FALL_TURNS;
	}

	/** The water's line at a turn of a flood: 0 none, else the highest bank level under it. */
	public static int waterLevel( int start, int peak, int turn ){
		if (start == NONE || peak <= 0) return 0;
		int dt = turn - start;
		if (dt < 0) return 0;
		if (dt < RISE_TURNS) return Math.min( peak, 1 + (peak - 1) * dt / RISE_TURNS );
		dt -= RISE_TURNS;
		if (dt < HIGH_TURNS) return peak;
		dt -= HIGH_TURNS;
		if (dt < FALL_TURNS) return Math.max( 0, peak - 1 - (peak - 1) * dt / FALL_TURNS );
		return 0;
	}

	/** The mud's line at a turn of a flood: from the moment the water starts to fall until MUD_TURNS
	 *  after it is gone, every bank up to the peak; 0 otherwise. */
	public static int mudLevel( int start, int peak, int turn ){
		if (start == NONE || peak <= 0) return 0;
		int dt = turn - start;
		return dt >= RISE_TURNS + HIGH_TURNS && turn < endOf( start ) + MUD_TURNS ? peak : 0;
	}

	/** Both lines packed in one number (0 for none): what a window is laid by. */
	public static int pack( int water, int mud ){
		return (water << 4) | mud;
	}

	public static int water( int packed ){
		return (packed >> 4) & 15;
	}

	public static int mud( int packed ){
		return packed & 15;
	}

	/** The lines now, packed, by the climate's flood on record. */
	public static int levels( int turn ){
		int start = ClimateManager.floodStart(), peak = ClimateManager.floodPeak();
		return pack( waterLevel( start, peak, turn ), mudLevel( start, peak, turn ) );
	}

	// ------------------------------------------------------------ the banks (pure)

	/**
	 * The bank level of a world cell from its sample (WindowGenerator.surface): 1..LEVELS for land in
	 * a river's corridor no more than LEVELS * LEVEL_STEP over the sea's line - the wilds or a road;
	 * 0 for anything else (water, higher ground, a lake's shore, a village's works).
	 */
	public static byte bankLevel( float river, float elev, int terrain, boolean wildOrRoad ){
		if (river <= 0f || !wildOrRoad) return 0;
		if (terrain == Terrain.WATER || terrain == Terrain.DEEP_WATER || terrain == Terrain.FROZEN_WATER) return 0;
		float h = elev - WorldModel.SEA;
		if (h >= LEVELS * LEVEL_STEP) return 0;
		return (byte) Math.max( 1, 1 + (int)(h / LEVEL_STEP) );
	}

	/** Open ground the water lies on: grass and the brush in it, bare earth, sand, flowers, embers. */
	public static boolean floodable( int t ){
		switch (t){
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: case Terrain.EMPTY_SP:
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
			case Terrain.FLOWER_PATCH: case Terrain.MUSHROOM_PATCH: case Terrain.SHRUB:
			case Terrain.EMBERS:
				return true;
			default:
				return false;
		}
	}

	/** What a flood at these lines lays on a cell of this bank level and pristine ground: WATER under
	 *  the water (MUD on a road), MUD under the mud's line, -1 nothing. */
	public static int laid( int pristine, int bank, boolean frozen, int packed ){
		if (bank <= 0 || frozen || packed == 0) return -1;
		boolean road = pristine == Terrain.DIRT_PATH;
		if (!road && !floodable( pristine )) return -1;
		if (bank <= water( packed )) return road ? Terrain.MUD : Terrain.WATER;
		if (bank <= mud( packed )) return Terrain.MUD;
		return -1;
	}

	/** Every cell of a window (its interior: the ring is the window's own wall) a flood at these lines
	 *  lays something on, never one an edit holds or a way between slices: world key -> terrain. */
	public static void want( WindowGenerator.Window base, int ox, int oy, int packed,
			java.util.Map<Long, Integer> edits, HashMap<Long, Integer> out ){
		if (packed == 0 || base == null || base.bank == null) return;
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		for (int y = 1; y < h - 1; y++){
			for (int x = 1; x < w - 1; x++){
				int c = x + y * w;
				int b = base.bank[c];
				if (b <= 0) continue;
				int t = laid( base.terrain[c], b, base.frozen != null && base.frozen[c], packed );
				if (t == -1) continue;
				if (base.link != null && base.link[c] != WindowGenerator.LINK_NONE) continue;
				long key = OverworldLevel.worldKey( ox + x, oy + y );
				if (edits != null && edits.containsKey( key )) continue;
				out.put( key, t );
			}
		}
	}

	// ------------------------------------------------------------ the land, as the hero finds it

	/** The silt's share of a crop's growth on a cell of the window: SILT on a bank the last flood
	 *  covered, from the water's fall until SILT_TURNS after it is gone; 1 everywhere else. */
	public static float siltFactor( OverworldLevel ow, int cell, int turn ){
		int start = ClimateManager.floodStart();
		if (start == NONE || ow.altitude() != 0) return 1f;
		if (turn < start + RISE_TURNS + HIGH_TURNS || turn >= endOf( start ) + SILT_TURNS) return 1f;
		int b = ow.bankAt( cell );
		return b > 0 && b <= ClimateManager.floodPeak() ? SILT : 1f;
	}

	//the flood whose going has been said (not bundled: a reload may say it again, harmlessly)
	private static int fallSaid = NONE;

	/** The host's step on the surface, or a turn he stood through (OverworldLevel.tickEvents): the
	 *  flood heard of and pinned the first time the window holds a bank under its water, the crops
	 *  on the low ground drowned, and its going said. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		int start = ClimateManager.floodStart();
		if (start == NONE) return;
		long t0 = LagMonitor.begin();
		int turn = level.eventTurn(), peak = ClimateManager.floodPeak();
		int water = waterLevel( start, peak, turn );
		WorldEvents.Event e = of( level.eventLog(), start );
		if (water > 0 && e == null){
			e = bear( level, start, water );
			if (e != null) announce( level, e );
		}
		if (water > 0) drown( level, water );
		if (e != null && turn >= start + RISE_TURNS + HIGH_TURNS && turn < endOf( start ) && fallSaid != start){
			fallSaid = start;
			level.tellParty( ( h, wx, wy ) -> GLog.POSITIVE + Messages.get( FloodEvent.class, "falls" ) );
		}
		LagMonitor.end( "OW flood", t0 );
	}

	/** The flood that began at `start` as written in the log, or null. */
	static WorldEvents.Event of( WorldEventLog log, int start ){
		for (WorldEvents.Event e : log.forced){
			if (e.type == WorldEvents.Type.FLOOD && e.startTurn == start) return e;
		}
		return null;
	}

	//written into the log on the bank under the water nearest the host's hero in the window; null
	//while the window holds none
	private static WorldEvents.Event bear( OverworldLevel level, int start, int water ){
		int[] at = nearestFlooded( level, xyz.gabriwar.warpedpixeldungeon.Dungeon.hero.pos, water );
		if (at == null) return null;
		int sx = Math.floorDiv( at[0], WorldStructures.SECTOR ), sy = Math.floorDiv( at[1], WorldStructures.SECTOR );
		int d = WorldEvents.day( start );
		WorldEvents.Event e = new WorldEvents.Event( WorldEvents.Type.FLOOD, sx, sy, d, start, endOf( start ),
				at[0], at[1], WorldEvents.idOf( WorldEvents.Type.FLOOD, sx, sy, d ) );
		level.forceEvent( e );
		return e;
	}

	/** The world cell of the window's bank under the water nearest a window cell (its own pristine
	 *  ground open to the water), or null. */
	static int[] nearestFlooded( OverworldLevel level, int from, int water ){
		int w = level.width(), fx = from % w, fy = from / w;
		int best = Integer.MAX_VALUE, bx = 0, by = 0;
		for (int y = 1; y < level.height() - 1; y++){
			for (int x = 1; x < w - 1; x++){
				int c = x + y * w;
				int b = level.bankAt( c );
				if (b <= 0 || b > water || level.frozenAt( c )) continue;
				int p = level.pristineAt( c );
				if (!floodable( p ) && p != Terrain.DIRT_PATH) continue;
				int d = Math.max( Math.abs( x - fx ), Math.abs( y - fy ) );
				if (d < best){
					best = d;
					bx = x;
					by = y;
				}
			}
		}
		return best == Integer.MAX_VALUE ? null : new int[]{ level.worldX + bx, level.worldY + by };
	}

	//every player hears of it the way it lies from his own hero, pinned on the map from now
	private static void announce( OverworldLevel level, WorldEvents.Event e ){
		level.markAnnounced( e );
		level.tellParty( ( h, wx, wy ) -> {
			String dir = WorldEvents.direction( e.wx - wx, e.wy - wy );
			if (dir.equals( "here" )) return GLog.WARNING + Messages.get( FloodEvent.class, "rises_here" );
			return GLog.WARNING + Messages.get( FloodEvent.class, "rises", Messages.get( WorldEvents.class, "dir_" + dir ) );
		} );
		xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_WORLD_EVENT );
		if (level.liveScene()) SpatialSound.playPanned( Assets.Sounds.WATER, 0f, 0.5f, 0.7f, level.panTowards( e.wx ) );
	}

	//every crop of the window on a bank under the water drowns
	private static void drown( OverworldLevel level, int water ){
		FarmSlice f = Farming.fields( level );
		if (f == null || f.size() == 0) return;
		for (Crop c : f.inWindow( level )){
			int cell = level.localCell( c.wx, c.wy );
			if (cell == -1) continue;
			int b = level.bankAt( cell );
			if (b > 0 && b <= water) Farming.dies( level, cell, c, Farming.Outcome.DROWNED, true );
		}
	}

	// ------------------------------------------------------------ debug

	/** Debug (the flood scenes): the rivers burst their banks at a turn (now, or earlier so the water
	 *  is already high or falling), to a peak. */
	public static void force( int start, int peak ){
		ClimateManager.debugFlood( start, Math.max( 1, Math.min( LEVELS, peak ) ) );
	}

	/** Debug (the flood scenes): the nearest world cell to (wx, wy) on a river's bank low enough for a
	 *  flood of `level` to cover, searched in rings of sample points out to `reach` cells; null for none. */
	public static int[] findBank( long seed, int wx, int wy, int level, int reach ){
		WorldModel.Sample s = new WorldModel.Sample();
		for (int r = 0; r <= reach; r += 2){
			for (int dy = -r; dy <= r; dy += 2){
				for (int dx = -r; dx <= r; dx += 2){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = wx + dx, y = wy + dy;
					WorldModel.sample( seed, x, y, 0f, s );
					if (s.biome == WorldModel.Biome.RIVER || s.biome == WorldModel.Biome.OCEAN) continue;
					int t = WorldModel.wildTerrain( seed, x, y, s );
					int st = WorldStructures.terrainAt( seed, x, y, t );
					boolean road = st == Terrain.DIRT_PATH;
					if (st != -1 && !road) continue;
					int b = bankLevel( s.river, s.elev, st != -1 ? st : t, true );
					if (b > 0 && b <= level && (road || floodable( t ))) return new int[]{ x, y };
				}
			}
		}
		return null;
	}
}
