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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Migrating;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A herd of deer crossing a region (WorldEvents.Type.MIGRATION), in spring and autumn: north to
 * the summer pastures in spring, south in autumn, wolves on its trail.
 *
 * WHEN AND WHERE. The world is dealt in regions of REGION x REGION (3x3) sectors. Every spring
 * and every autumn (EventSeasons) a region's herd sets off on a hashed morning of the season, so
 * its DAYS (4) days on the road never run into the next season, along a road LENGTH (240) cells
 * long through the region's middle - northward in spring, southward in autumn, swung up to
 * SWING either way and set up to half a sector across - when that road keeps to the deer's ground
 * all the way: woodland, meadow, plain, foothills, tundra or snowfield, never water, the peaks,
 * the town or a settlement's streets (a few tries, then no herd that season). FestivalEventTest's
 * neighbour, MigrationEventTest, measures how often a hero meets one.
 *
 * THE HERD. MIN_HERD to MAX_HERD (20 to 36) deer walk in a column COLUMN cells long, the widest
 * in its middle, and MIN_PACK to MAX_PACK (2 or 3) wolves keep TRAIL cells behind its last
 * stragglers. Its head sets off from the road's start at the herd's first turn and its last wolf
 * leaves the road's end at its last: every beast's place at any turn is a pure function of the
 * herd and the turn (placeAt), so the herd moves on whether anyone watches or not. In the window
 * a place within LIVE cells of a hero has its beast (spawned out of every hero's sight where it
 * can be, SPAWNS a step at most); the deer graze along toward their places (Deer.act) and bolt
 * from the hero as ever, the wolves wander at ease behind (Migrating.steer) and pay the hero no
 * mind unless he crowds or hurts one. After dark, once a night, the pack runs down the
 * rearmost deer in the window - a HuntPack of the herd's own (huntId), so the hunt ends, feeds and
 * breaks as a wolf hunt does (HuntEvent) - and goes back to trailing. A beast killed leaves its
 * place in the herd empty for good (fell), and so does a deer the hero ropes, which is his from
 * then on (leaveHerdIfRoped); one far from every hero, or strayed far from its
 * place where nobody sees, is gone until a hero comes near its place again; one that leaves the
 * window is never parked (leaving). When the herd's days are over its beasts in sight go on as
 * ordinary beasts and the rest slip away.
 *
 * A hero hears of the herd once it is on, within HEAR (3) sectors of its middle, and the world
 * map pins it where its middle is now (onMap).
 */
public final class MigrationEvent {

	private MigrationEvent(){}

	/** Sectors a side of a migration's region. */
	public static final int REGION = 3;
	/** Days a herd is on the road. */
	public static final int DAYS = 4;
	/** Cells of the herd's road. */
	public static final int LENGTH = 240;
	/** The herd: its deer, its wolves, and the cells from its head to its last deer. */
	public static final int MIN_HERD = 20, MAX_HERD = 36, MIN_PACK = 2, MAX_PACK = 3, COLUMN = 44;
	/** Cells the wolves keep behind the last deer. */
	public static final int TRAIL = 12;
	/** A wolf's place in the herd is WOLVES and on: the deer's are below. */
	public static final int WOLVES = 100;
	//how far the head travels: the road, and the column and its wolves after it
	static final int TRAVEL = LENGTH + COLUMN + TRAIL + 2 * MAX_PACK;
	//the turn of its first day the herd sets off: the morning on either clock
	static final int SET_OFF = WorldEvents.MARKET_OPEN;
	//days kept clear of either end of the season
	static final int EDGE = 2;
	//the road's swing off due north or south, either way, radians (35 degrees)
	static final double SWING = Math.toRadians( 35 );
	//roads tried before a region goes without a herd for the season
	static final int TRIES = 3;
	//cells across the road the herd spreads at its widest, either side
	static final int SPREAD = 5;
	//sectors: a herd's middle this near a hero of the party is heard of (a market's reach)
	static final int HEAR = 3;
	//cells: a place this near a hero has its beast; one this much further off loses it
	static final int LIVE = 30, LET_GO = 12;
	//cells: a beast this far from its place, out of sight, has strayed: gone until it is put back
	static final int STRAY = 30;
	//cells: how far from its place a beast is set down to be out of every hero's sight
	static final int HIDE = 6;
	//beasts put in the window on one step at most: a herd fills in over a few steps
	static final int SPAWNS = 6;
	//cells: a beast of a herd that is over slips away only this far from every hero, out of sight
	static final int UNSEEN = 21;

	private static final long SALT_ROAD = 0x41687A0DL, SALT_START = 0x5E770FFL, SALT_HERD = 0x4E2DL,
			SALT_SLOT = 0x5107L, SALT_HUNT = 0x5772A6L, SALT_FORCED = 0xF0BC1DL;

	// ------------------------------------------------------------ the road (pure)

	/** A herd's road: from (x0, y0) to (x1, y1), world cells. */
	public static final class Road {
		public final int x0, y0, x1, y1;
		Road( int x0, int y0, int x1, int y1 ){
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
		}
	}

	/** Ground a herd may cross (at the annual mean, so a road never comes and goes with the seasons). */
	static boolean deerGround( WorldModel.Biome b ){
		switch (b){
			case FOREST: case MEADOW: case PLAINS: case FOOTHILLS: case TUNDRA: case SNOWFIELD:
				return true;
			default:
				return false;
		}
	}

	/** Does the straight road from (x0, y0) to (x1, y1) keep to the deer's ground all the way, two
	 *  cells at a time: never water, the peaks, the town or a settlement's streets? */
	static boolean deerRoad( long seed, int x0, int y0, int x1, int y1 ){
		int n = Math.max( 1, Math.max( Math.abs( x1 - x0 ), Math.abs( y1 - y0 ) ) / 2 );
		for (int i = 0; i <= n; i++){
			int x = x0 + Math.round( (x1 - x0) * i / (float) n ), y = y0 + Math.round( (y1 - y0) * i / (float) n );
			if (Math.max( Math.abs( x ), Math.abs( y ) ) < WorldEvents.TOWN_CLEAR) return false;
			if (!deerGround( WorldModel.baseBiomeAt( seed, x, y ) ) || OverworldFauna.nearSettlement( seed, x, y )) return false;
		}
		return true;
	}

	//the way a herd goes in a season, radians on the map (+x east, +y south): north in spring,
	//south in autumn, east in any other (only a forced herd walks then)
	private static double due( GameCalendar.Season s ){
		return s == GameCalendar.Season.SPRING ? -Math.PI / 2 : s == GameCalendar.Season.AUTUMN ? Math.PI / 2 : 0;
	}

	//the roads worked out, by region and season: pure, so kept per seed (NONE: no herd that season)
	private static final Road NONE = new Road( 0, 0, 0, 0 );
	private static volatile long roadSeed = Long.MIN_VALUE;
	private static final ConcurrentHashMap<Long, Road> roads = new ConcurrentHashMap<>();

	/** The road of region (rx, ry)'s herd in a season, or null for none: through the region's
	 *  middle, swung and set across by the hash, the first of TRIES that is a deer road. */
	static Road road( long seed, int rx, int ry, EventSeasons.Span s ){
		if (seed != roadSeed){
			synchronized (roads){
				if (seed != roadSeed){
					roads.clear();
					roadSeed = seed;
				}
			}
		}
		long key = WorldEvents.mix( WorldStructures.sectorOf( rx, ry ), s.first, 0x60AD + s.season.ordinal() );
		Road known = roads.get( key );
		if (known != null) return known == NONE ? null : known;
		Road found = null;
		float cx = (rx * REGION + REGION / 2f) * WorldStructures.SECTOR, cy = (ry * REGION + REGION / 2f) * WorldStructures.SECTOR;
		for (int k = 0; k < TRIES && found == null; k++){
			long h = WorldEvents.mix( seed ^ SALT_ROAD, WorldStructures.sectorOf( rx, ry ), s.first * 4L + k );
			double ang = due( s.season ) + ((h & 0xFFFF) / 65535.0 - 0.5) * 2 * SWING;
			float across = (((h >>> 16) & 0xFFFF) / 65535f - 0.5f) * WorldStructures.SECTOR;
			float ux = (float) Math.cos( ang ), uy = (float) Math.sin( ang );
			float mx = cx - uy * across, my = cy + ux * across;
			int x0 = Math.round( mx - ux * LENGTH / 2f ), y0 = Math.round( my - uy * LENGTH / 2f );
			int x1 = Math.round( mx + ux * LENGTH / 2f ), y1 = Math.round( my + uy * LENGTH / 2f );
			if (deerRoad( seed, x0, y0, x1, y1 )) found = new Road( x0, y0, x1, y1 );
		}
		if (roads.size() > 4096) roads.clear();
		roads.put( key, found == null ? NONE : found );
		return found;
	}

	/** The day of a spring or an autumn region (rx, ry)'s herd sets off on: EDGE days or more
	 *  inside the season, its last day too. */
	static int startDay( long seed, int rx, int ry, EventSeasons.Span s ){
		int room = Math.max( 1, s.days - DAYS - 2 * EDGE );
		long h = WorldEvents.mix( seed ^ SALT_START, WorldStructures.sectorOf( rx, ry ), s.first );
		return s.first + EDGE + (int) Math.floorMod( h >>> 8, (long) room );
	}

	/** The herd of region (rx, ry) on the road on this day, or null: in spring and autumn only, for
	 *  its DAYS days, on a road that keeps to the deer's ground. Anchored on the region's middle
	 *  sector; its world cell is the road's start. */
	public static WorldEvents.Event migration( long seed, int rx, int ry, int day ){
		EventSeasons.Span s = EventSeasons.of( day );
		if (s.season != GameCalendar.Season.SPRING && s.season != GameCalendar.Season.AUTUMN) return null;
		int start = startDay( seed, rx, ry, s );
		if (day < start || day > start + DAYS) return null;
		Road r = road( seed, rx, ry, s );
		if (r == null) return null;
		int ax = rx * REGION + REGION / 2, ay = ry * REGION + REGION / 2;
		int from = start * WorldEvents.FC + SET_OFF;
		return new WorldEvents.Event( WorldEvents.Type.MIGRATION, ax, ay, start, from, from + DAYS * WorldEvents.FC,
				r.x0, r.y0, WorldEvents.idOf( WorldEvents.Type.MIGRATION, ax, ay, start ) );
	}

	/** A herd forced onto a road from world cell (wx, wy) at a turn (debug scenes): the way the
	 *  season of its day sends herds, for DAYS days. An id of its own. */
	public static WorldEvents.Event forcedMigration( int wx, int wy, int turn ){
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		int d = WorldEvents.day( turn );
		return new WorldEvents.Event( WorldEvents.Type.MIGRATION, sx, sy, d, turn, turn + DAYS * WorldEvents.FC, wx, wy,
				WorldEvents.mix( WorldEvents.idOf( WorldEvents.Type.MIGRATION, sx, sy, d ) ^ SALT_FORCED, turn, 0 ) );
	}

	/** The way a forced herd starting on day `day` goes, as a unit step {dx, dy} on the map. */
	public static float[] heading( int day ){
		double a = due( EventSeasons.of( day ).season );
		return new float[]{ (float) Math.cos( a ), (float) Math.sin( a ) };
	}

	/** The road of a herd, natural or forced, or null. Pure. */
	public static Road roadOf( long seed, WorldEvents.Event e ){
		if (e.id == WorldEvents.idOf( WorldEvents.Type.MIGRATION, e.sx, e.sy, e.startDay )){
			return road( seed, Math.floorDiv( e.sx, REGION ), Math.floorDiv( e.sy, REGION ), EventSeasons.of( e.startDay ) );
		}
		float[] u = heading( e.startDay );
		return new Road( e.wx, e.wy, e.wx + Math.round( u[0] * LENGTH ), e.wy + Math.round( u[1] * LENGTH ) );
	}

	// ------------------------------------------------------------ the herd (pure)

	/** How many deer the herd has. */
	public static int herdSize( long id ){
		return MIN_HERD + (int) Math.floorMod( WorldEvents.mix( id ^ SALT_HERD, 0, 0 ), (long)(MAX_HERD - MIN_HERD + 1) );
	}

	/** How many wolves trail it. */
	public static int packSize( long id ){
		return MIN_PACK + (int) Math.floorMod( WorldEvents.mix( id ^ SALT_HERD, 1, 0 ), (long)(MAX_PACK - MIN_PACK + 1) );
	}

	/** A place in the herd: {cells behind its head, cells right of its line}. The deer's spread down
	 *  the column, widest in its middle; the wolves' TRAIL behind the last, side by side. */
	public static int[] place( long id, int slot ){
		if (slot >= WOLVES){
			int j = slot - WOLVES;
			return new int[]{ COLUMN + TRAIL + 2 * j, 2 * j - packSize( id ) + 1 };
		}
		int n = herdSize( id );
		long h = WorldEvents.mix( id ^ SALT_SLOT, slot, 0 );
		int offset = 2 + slot * (COLUMN - 2) / n + (int) Math.floorMod( h, 3L ) - 1;
		int wide = 1 + Math.round( SPREAD * (float) Math.sin( Math.PI * (slot + 0.5) / n ) );
		int lateral = (int) Math.floorMod( h >>> 8, (long)(2 * wide + 1) ) - wide;
		return new int[]{ Math.max( 0, offset ), lateral };
	}

	//how far along its road the herd's head is at a turn, cells
	static float head( int startTurn, int endTurn, int turn ){
		return (turn - startTurn) * (float) TRAVEL / Math.max( 1, endTurn - startTurn );
	}

	/** The world cell {x, y} a place in a herd on the road (x0, y0)-(x1, y1) is on at a turn, or
	 *  null when it is not on the road (not on it yet, or past its end). Pure. */
	public static int[] placeAt( int x0, int y0, int x1, int y1, int startTurn, int endTurn, int offset, int lateral, int turn ){
		float len = (float) Math.hypot( x1 - x0, y1 - y0 );
		if (len < 1) return null;
		float p = head( startTurn, endTurn, turn ) - offset;
		if (p < 0 || p > len) return null;
		float ux = (x1 - x0) / len, uy = (y1 - y0) / len;
		return new int[]{ Math.round( x0 + ux * p - uy * lateral ), Math.round( y0 + uy * p + ux * lateral ) };
	}

	/** Where the herd's middle is on its road at a turn, world {x, y} (its start before it sets off). */
	static int[] middle( Road r, WorldEvents.Event e, int turn ){
		float len = (float) Math.hypot( r.x1 - r.x0, r.y1 - r.y0 );
		float p = Math.max( 0, Math.min( len, head( e.startTurn, e.endTurn, turn ) - COLUMN / 2f ) );
		float t = len < 1 ? 0 : p / len;
		return new int[]{ Math.round( r.x0 + (r.x1 - r.x0) * t ), Math.round( r.y0 + (r.y1 - r.y0) * t ) };
	}

	/** The herd as the world map pins it: where its middle is now (WorldEvents.onMap). */
	static WorldEvents.Event onMap( long seed, WorldEvents.Event e, int turn ){
		Road r = roadOf( seed, e );
		if (r == null) return e;
		int[] m = middle( r, e, turn );
		return new WorldEvents.Event( e.type, e.sx, e.sy, e.startDay, e.startTurn, e.endTurn, m[0], m[1], e.id );
	}

	/** The id a place in the herd is settled by once its beast is dead. */
	static long slotId( long herd, int slot ){
		return WorldEvents.mix( herd ^ SALT_SLOT, slot, 7 );
	}

	/** The id of the herd's wolves' hunt on a night (a HuntPack's, HuntEvent's to wind down). */
	static long huntId( long herd, int night ){
		return WorldEvents.mix( herd ^ SALT_HUNT, night, 0 );
	}

	// ------------------------------------------------------------ the hero's step

	/** The host hero's step on the surface, or a turn he stood through (OverworldLevel.tickEvents,
	 *  after the hunts): the herds on about the window heard of, their beasts put where they should
	 *  be, the night's straggler run down, and the beasts of herds now gone let go. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		long t0 = LagMonitor.begin();
		int turn = level.eventTurn();
		ArrayList<WorldEvents.Event> on = new ArrayList<>();
		for (WorldEvents.Event e : level.nearEvents()){
			if (e.type == WorldEvents.Type.MIGRATION && e.activeAt( turn ) && !level.eventResolved( e.id )) on.add( e );
		}
		for (Mob m : level.mobs) leaveHerdIfRoped( level, m );
		release( level, on );
		ArrayList<Hero> party = OverworldLevel.heroesOn( level );
		int[] spawns = { SPAWNS };
		for (WorldEvents.Event e : on){
			Road r = roadOf( level.worldSeed(), e );
			if (r == null) continue;
			hear( level, party, e, r, turn );
			keep( level, party, e, r, turn, spawns );
			huntAtNight( level, e );
		}
		LagMonitor.end( "OW migration", t0 );
	}

	//is a hero's sector within HEAR of the herd's middle's?
	private static boolean hears( OverworldLevel level, Hero h, int[] mid ){
		if (h == null || h.pos < 0 || h.pos >= level.length()) return false;
		int w = level.width();
		int hsx = Math.floorDiv( level.worldX() + h.pos % w, WorldStructures.SECTOR );
		int hsy = Math.floorDiv( level.worldY() + h.pos / w, WorldStructures.SECTOR );
		return Math.max( Math.abs( Math.floorDiv( mid[0], WorldStructures.SECTOR ) - hsx ),
				Math.abs( Math.floorDiv( mid[1], WorldStructures.SECTOR ) - hsy ) ) <= HEAR;
	}

	//heard of by the party once, each player in his own words - the way it lies and the way it
	//is going - and pinned on the map; the drumming of its hooves the way it lies
	private static void hear( OverworldLevel level, ArrayList<Hero> party, WorldEvents.Event e, Road r, int turn ){
		if (level.eventAnnounced( e.id )) return;
		final int[] mid = middle( r, e, turn );
		boolean heard = false;
		for (Hero h : party) heard |= hears( level, h, mid );
		if (!heard) return;
		level.markAnnounced( e );
		final String going = Messages.get( WorldEvents.class, "dir_" + WorldEvents.direction( (r.x1 - r.x0) * 8, (r.y1 - r.y0) * 8 ) );
		level.tellParty( ( h, wx, wy ) -> {
			if (!hears( level, h, mid )) return null;
			String dir = WorldEvents.direction( mid[0] - wx, mid[1] - wy );
			if (dir.equals( "here" )) return GLog.HIGHLIGHT + Messages.get( MigrationEvent.class, "heard_here", going );
			return GLog.HIGHLIGHT + Messages.get( MigrationEvent.class, "heard", Messages.get( WorldEvents.class, "dir_" + dir ), going );
		} );
		GuideGraph.reveal( GuideGraph.TAG_WORLD_EVENT );
		if (level.liveScene() && hears( level, Dungeon.hero, mid )){
			float pan = level.panTowards( mid[0] );
			for (int i = 0; i < 4; i++) SpatialSound.playPanned( Assets.Sounds.TRAMPLE, 0.12f * i, 0.35f, 0.7f + 0.05f * i, pan );
		}
	}

	//the herd's beasts in the window, by their place
	private static HashMap<Integer, Mob> beasts( OverworldLevel level, long id ){
		HashMap<Integer, Mob> out = new HashMap<>();
		for (Mob m : level.mobs){
			Migrating g = m.buff( Migrating.class );
			if (g != null && g.herd == id && m.isAlive()) out.put( g.slot, m );
		}
		return out;
	}

	//every place of the herd near a hero has its beast; a beast far from every hero, strayed far
	//from its place unseen, or past the road's end goes
	private static void keep( OverworldLevel level, ArrayList<Hero> party, WorldEvents.Event e, Road r, int turn, int[] spawns ){
		HashMap<Integer, Mob> have = beasts( level, e.id );
		int deer = herdSize( e.id ), wolves = packSize( e.id );
		for (int k = 0; k < deer + wolves; k++){
			int slot = k < deer ? k : WOLVES + k - deer;
			Mob m = have.get( slot );
			if (level.eventResolved( slotId( e.id, slot ) )){
				continue;
			}
			int[] p = place( e.id, slot );
			int[] at = placeAt( r.x0, r.y0, r.x1, r.y1, e.startTurn, e.endTurn, p[0], p[1], turn );
			int cell = at == null ? -1 : level.localCell( at[0], at[1] );
			if (m != null){
				boolean seen = NetManager.anyHeroSees( m.pos );
				if (at == null){
					//past the road's end, or the herd turned back to before its start: on its own
					letGo( level, m );
				} else if (!seen && (OverworldLevel.heroDistance( level, m.pos ) > LIVE + LET_GO
						|| (m.buff( HuntPack.class ) == null && (cell == -1 || level.distance( m.pos, cell ) > STRAY)))){
					OverworldLevel.vanish( level, m );
				}
				continue;
			}
			if (cell == -1 || spawns[0] <= 0 || OverworldLevel.heroDistance( level, cell ) > LIVE) continue;
			int w = level.width(), x = cell % w, y = cell / w;
			if (x < 3 || y < 3 || x > w - 4 || y > level.height() - 4) continue;
			if (spawn( level, e, r, slot, p, cell )) spawns[0]--;
		}
	}

	//a beast for a place of the herd, set down out of sight near it where it can be. false when
	//there is no open ground for it there
	private static boolean spawn( OverworldLevel level, WorldEvents.Event e, Road r, int slot, int[] p, int cell ){
		int at = HuntEvent.hiddenGround( level, cell, HIDE, 3 );
		if (at == -1) return false;
		Mob m;
		long h = WorldEvents.mix( e.id ^ SALT_HERD, slot, 3 );
		if (slot >= WOLVES){
			WorldModel.Biome b = level.biomeAtCell( at );
			//grey wolves in the cold, brown ones elsewhere
			boolean cold = level.frozenAt( at ) || b == WorldModel.Biome.TUNDRA || b == WorldModel.Biome.SNOWFIELD
					|| GameCalendar.season() == GameCalendar.Season.WINTER;
			m = cold ? new GrayWolf() : new BrownWolf();
			m.state = m.WANDERING;
		} else {
			Deer d = new Deer();
			d.setDoe( (h & 1L) == 0 );
			m = d;
		}
		Migrating g = Buff.affect( m, Migrating.class );
		g.herd = e.id;
		g.slot = slot;
		g.x0 = r.x0;
		g.y0 = r.y0;
		g.x1 = r.x1;
		g.y1 = r.y1;
		g.startTurn = e.startTurn;
		g.endTurn = e.endTurn;
		g.offset = p[0];
		g.lateral = p[1];
		return level.addMob( m, at );
	}

	//a beast of a herd let go: on its own as an ordinary beast where a hero can see it or nearly,
	//otherwise slipped away. never destroy(): a wolf's pays the hero for a kill
	private static void letGo( OverworldLevel level, Mob m ){
		if (NetManager.anyHeroSees( m.pos ) || OverworldLevel.heroDistance( level, m.pos ) <= UNSEEN){
			Migrating g = m.buff( Migrating.class );
			if (g != null) g.detach();
		} else {
			OverworldLevel.vanish( level, m );
		}
	}

	//the beasts of herds no longer on about the window (their days over, or settled) are let go
	private static void release( OverworldLevel level, List<WorldEvents.Event> on ){
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			Migrating g = m.buff( Migrating.class );
			if (g == null) continue;
			boolean live = false;
			for (WorldEvents.Event e : on) live |= e.id == g.herd;
			if (!live) letGo( level, m );
		}
	}

	//after dark, once a night: the pack runs down the herd's rearmost deer in the window. the
	//hunt is a HuntPack of the herd's own for the night, so HuntEvent winds it down as it does
	//any wolf hunt (its end, the pack broken, the kill and the feeding)
	private static void huntAtNight( OverworldLevel level, WorldEvents.Event e ){
		if (!HuntEvent.night()) return;
		int night = WorldClock.night();
		ArrayList<Mob> pack = new ArrayList<>();
		Mob quarry = null;
		int rearmost = -1;
		for (Mob m : level.mobs){
			Migrating g = m.buff( Migrating.class );
			if (g == null || g.herd != e.id || !m.isAlive()) continue;
			if (m.buff( HuntPack.class ) != null) return;   //a hunt of the herd's is on
			if (g.wolf()){
				if (g.defending || g.huntedNight == night || m.alignment != Char.Alignment.ENEMY) continue;
				pack.add( m );
			} else if (g.offset > rearmost){
				rearmost = g.offset;
				quarry = m;
			}
		}
		if (pack.isEmpty() || quarry == null) return;
		long id = huntId( e.id, night );
		int qx = level.worldX() + quarry.pos % level.width(), qy = level.worldY() + quarry.pos / level.width();
		tag( quarry, id, night, qx, qy );
		for (Mob wolf : pack){
			tag( wolf, id, night, qx, qy );
			wolf.buff( Migrating.class ).huntedNight = night;
			HuntPack.loose( wolf, quarry );
			if (wolf.sprite != null) wolf.sprite.showAlert();
		}
		//told to who sees it start, or is close by
		final int seen = quarry.pos;
		level.tellParty( ( h, wx, wy ) -> level.heroSees( h, seen )
				|| WorldEvents.direction( qx - wx, qy - wy ).equals( "here" )
				? GLog.WARNING + Messages.get( MigrationEvent.class, "hunt" ) : null );
		if (level.liveScene() && (level.heroSees( Dungeon.hero, seen )
				|| OverworldLevel.heroDistance( level, seen ) <= WorldEvents.STAR_NEAR)){
			SpatialSound.play( Assets.Sounds.HOWL, seen, 0.7f );
		}
	}

	private static void tag( Mob m, long id, int night, int wx, int wy ){
		HuntPack p = Buff.affect( m, HuntPack.class );
		p.hunt = id;
		p.day = night;
		p.ax = wx;
		p.ay = wy;
	}

	/** A beast of a herd is dead (Deer.die, GrayWolf, BrownWolf.die): its place stays empty. */
	public static void fell( Mob m ){
		Migrating g = m.buff( Migrating.class );
		if (g == null || !(Dungeon.level instanceof OverworldLevel)) return;
		((OverworldLevel) Dungeon.level).resolveEvent( slotId( g.herd, g.slot ), g.endTurn );
	}

	//a deer the hero roped (homestead/animals/Roping) is his now: out of the herd for good, its
	//place left empty so the herd does not hand out another
	private static boolean leaveHerdIfRoped( OverworldLevel level, Mob m ){
		Migrating g = m.buff( Migrating.class );
		if (g == null || !Roping.held( m )) return false;
		level.resolveEvent( slotId( g.herd, g.slot ), g.endTurn );
		g.detach();
		return true;
	}

	/** A mob leaving the window (OverworldLevel.park): a herd's beast is the herd's to put back,
	 *  never parked (one the hero roped is his, parked as any of his). True when it was one, and
	 *  it is gone. */
	static boolean leaving( OverworldLevel level, Mob m ){
		if (m.buff( Migrating.class ) == null || leaveHerdIfRoped( level, m )) return false;
		OverworldLevel.vanish( level, m );
		return true;
	}

	/** A herd on the move keeps its own hours: the hour's cull spares its deer by night and its
	 *  wolves by day (OverworldFauna.cull). */
	public static boolean onTheMove( Mob m ){
		return m.buff( Migrating.class ) != null;
	}

	// ------------------------------------------------------------ debug

	/** Debug (the migration scenes): the turn a herd has to have set off on for its middle to be
	 *  `lead` cells along its road at turn `now`. */
	public static int debugSetOff( int now, int lead ){
		return now - Math.round( (lead + COLUMN / 2f) * DAYS * WorldEvents.FC / (float) TRAVEL );
	}

	/** Debug: where a herd's middle is on its road at a turn, world {x, y}. */
	public static int[] middleNow( long seed, WorldEvents.Event e, int turn ){
		Road r = roadOf( seed, e );
		return r == null ? new int[]{ e.wx, e.wy } : middle( r, e, turn );
	}

	/** Debug (the migration scenes): a world cell for a forced herd's road to start on, so that its
	 *  head passes `ahead` cells off the hero after `lead` cells - the first of the eight ways
	 *  round the hero whose road keeps to the deer's ground. Null when none does. */
	public static int[] debugStart( long seed, int hwx, int hwy, int day, int lead, int ahead ){
		float[] u = heading( day );
		for (int k = 0; k < 8; k++){
			double a = k * Math.PI / 4;
			int sx = hwx + (int) Math.round( Math.cos( a ) * ahead ), sy = hwy + (int) Math.round( Math.sin( a ) * ahead );
			int x0 = sx - Math.round( u[0] * lead ), y0 = sy - Math.round( u[1] * lead );
			int x1 = x0 + Math.round( u[0] * LENGTH ), y1 = y0 + Math.round( u[1] * LENGTH );
			if (deerRoad( seed, x0, y0, x1, y1 )) return new int[]{ x0, y0 };
		}
		return null;
	}
}
