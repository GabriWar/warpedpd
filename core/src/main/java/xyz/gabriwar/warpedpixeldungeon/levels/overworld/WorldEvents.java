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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The surface's timed events: something happening somewhere that a hero hears of, can travel
 * to and take part in. Where and when each one happens is a PURE function of the world seed,
 * the sector and the day - never of who looked first, so a reload, a window slide or a co-op
 * guest always finds the same events in the same places. What the players did about them (an
 * event heard of, a fragment picked up, a market chased off) lives in WorldEventLog, and the
 * world (OverworldLevel) lays each event down while it is on. Nothing here touches the game
 * state: the actor thread, the tests and the world map's render thread all call it.
 *
 * Times are WorldClock turns, FC to a day; day d runs from d*FC (dawn on a turn-counted run,
 * local midnight on a real-clock one) - as the world reads them through its log's clock, which
 * never runs back (WorldEventLog.clock). The season is deliberately not an input: on a
 * turn-counted run it is itself a function of the day, on a real-clock run it would come off
 * the wall clock and break the purity, and an event running across a season's turn would
 * flicker. The numbers:
 *
 * A FALLEN STAR. Every sector rolls 1 in STAR_ODDS (36) every day. A roll tries STAR_TRIES (6)
 * hashed cells in the sector's middle (12 cells clear of its edges) for open land: plain
 * ground on the cell and its four sides, at least TOWN_CLEAR (64) cells from the town, never
 * on the sea, a river or the peaks, never on a cave mouth, and clear of every site's berth
 * (a settlement's fence and the crater's own two cells besides, eight cells round any other
 * site's heart). Nearly nine rolls in ten find some on land, so a hero's own 3x3 sectors see
 * a star about every four and a half days (WorldEventsTest measures it). It comes down at
 * night: turn STAR_FALL_FROM (2200) to STAR_FALL_FROM + STAR_FALL_SPAN (2500) of its day,
 * inside the NIGHT of every season on both clocks - a turn-counted night starts at 500 + the
 * day's length (1900 at midsummer), a real-clock one 45 minutes after sunset (21:00 at the
 * latest, GameCalendar.sunriseSunset), and the window is 21:07 to midnight local. Its fragment
 * lies there STAR_DAYS (3) days, its crater smoulders STAR_HOT (a day) and stays scorched
 * STAR_SCAR_DAYS (9) days before the land takes it back. A hero hears of it within one sector,
 * or as soon as the impact is in his window.
 *
 * A TRAVELLING MARKET. The world is dealt in regions of MARKET_REGION x MARKET_REGION (3x3)
 * sectors and periods of MARKET_PERIOD (21) days. One period in two a region's market comes
 * round, to one of its human villages (hashed), opening on a hashed morning of the period
 * (turn MARKET_OPEN, 800, of its day) and rolling on the morning MARKET_DAYS (3) days after -
 * never straddling two periods. Morning on both clocks: turn 800 is inside a turn-counted DAY
 * in every season (it covers 250 to 850 at the least) and is 07:40 on a real-clock run, after
 * the night's end in every season (dawn comes at 07:30 at the latest), so the stalls never go
 * up shut for the night. A region with a human village so gets a market about every six weeks,
 * a village about once a season, and a hero sees a new one within three sectors about every
 * two weeks (one is on there a quarter of the time): more often than "once a season a region",
 * on purpose, so a run actually meets them. A hero hears of it within three sectors.
 *
 * A BANDIT RAID on a human village, for a whole day (RaidEvent has the numbers), and a WOLF
 * PACK'S HUNT, one night on a sector's hunting ground (HuntEvent). Both announce themselves
 * (announceSectors -1): the raid by the smoke a hero sees from afar, the hunt by the pack's howl
 * as night falls within a sector of a hero (HuntEvent.HEAR), long before it starts.
 *
 * An event's identity is idOf(type, anchor sector, start day); one a debug scene forces gets
 * an id of its own (forcedId), which never equals a natural one.
 */
public final class WorldEvents {

	private WorldEvents(){}

	/** Turns to a day: WorldClock turns, as every time here is. */
	public static final int FC = DayNightCycle.FULL_CYCLE;

	public static final int STAR_ODDS = 36;
	public static final int STAR_TRIES = 6;
	public static final int STAR_DAYS = 3;
	public static final int STAR_SCAR_DAYS = 9;
	public static final int STAR_FALL_FROM = 2200, STAR_FALL_SPAN = 300;
	/** How long a crater smokes after the fall. */
	public static final int STAR_HOT = FC;
	/** A hero who hears of a star within this many turns of the fall sees it come down. */
	public static final int STAR_FRESH = 60;
	/** Chebyshev cells: a star this close lands "close by"; further off, a direction is named. */
	public static final int STAR_NEAR = 16;
	public static final int TOWN_CLEAR = 64;

	public static final int MARKET_REGION = 3;
	public static final int MARKET_PERIOD = 21;
	public static final int MARKET_DAYS = 3;
	/** The turn of its day a market comes and, MARKET_DAYS on, goes: morning on either clock. */
	public static final int MARKET_OPEN = 800;

	private static final long STAR_SALT = 0x5747A2FA11L, MARKET_SALT = 0x3A2CE7B00L;
	private static final long CRATER_SALT = 0xC4A7E5L, BONUS_SALT = 0xB0B05L;
	private static final long FIND_SALT = 0xF1ADL, FORCED_SALT = 0xF0BCEDL;

	public enum Type {
		//a star comes down at night on open land and lies smoking in its crater for three days
		FALLEN_STAR( 0x57A2F411L, 0xFFFFE070, 1, "fallen_star" ),
		//a travelling market sets up round a human village's well for three days
		MARKET     ( 0x3A2CE7F1L, 0xFFFF5FC8, 3, "market" ),
		//bandits raid a human village for a day (RaidEvent)
		RAID       ( 0x7A1DE7E5L, 0xFFFF7F1E, -1, "raid" ),
		//a wolf pack runs down a deer on its hunting ground, one night (HuntEvent)
		HUNT       ( 0x40B7E4A5L, 0xFFB98A4E, -1, "hunt" );

		public final long salt;            //id salt: stable across enum reordering (ids are bundled)
		public final int pin;              //world map pin colour, ARGB
		public final int announceSectors;  //Chebyshev sector distance at which the hero hears of it; -1: it says itself
		public final String key;           //message suffix: map_<key>

		Type( long salt, int pin, int announceSectors, String key ){
			this.salt = salt;
			this.pin = pin;
			this.announceSectors = announceSectors;
			this.key = key;
		}
	}

	/** One event: where (its anchor sector and world cell), when (WorldClock turns) and who it is. */
	public static final class Event {
		public final Type type;
		public final int sx, sy;         //anchor sector (a star's impact sector, a market's village sector)
		public final int startDay;       //the day it belongs to
		public final int startTurn;      //first live turn: the star's fall, the market's opening morning
		public final int endTurn;        //first turn it is over: the star's loot taken back, the market gone
		public final int wx, wy;         //world cell: the impact, the village well
		public final long id;

		public Event( Type type, int sx, int sy, int startDay, int startTurn, int endTurn, int wx, int wy, long id ){
			this.type = type;
			this.sx = sx;
			this.sy = sy;
			this.startDay = startDay;
			this.startTurn = startTurn;
			this.endTurn = endTurn;
			this.wx = wx;
			this.wy = wy;
			this.id = id;
		}

		/** On at this turn: the star's fragment lies there, the market trades. */
		public boolean activeAt( int turn ){
			return turn >= startTurn && turn < endTurn;
		}

		/** The first turn nothing of it is left on the land: a star's crater outlasts its loot. */
		public int lingersUntil(){
			return type == Type.FALLEN_STAR ? startTurn + STAR_SCAR_DAYS * FC : endTurn;
		}

		/** Is a star's crater on the ground at this turn? */
		public boolean scarredAt( int turn ){
			return type == Type.FALLEN_STAR && turn >= startTurn && turn < lingersUntil();
		}

		/** Is anything of it on the land at some turn of this day? */
		public boolean overlapsDay( int day ){
			return startTurn < (day + 1) * FC && lingersUntil() > day * FC;
		}
	}

	// ------------------------------------------------------------ hashing

	/** The events' hash: the settlements' mix with a full avalanche. */
	public static long mix( long a, long b, long c ){
		long h = a ^ 0x57A7C7E5L;
		h ^= b * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= c * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	/** The day a world turn falls on. */
	public static int day( int turn ){
		return Math.floorDiv( turn, FC );
	}

	/** An event's identity: its kind, its anchor sector and the day it starts. */
	public static long idOf( Type type, int sx, int sy, int startDay ){
		return mix( type.salt, WorldStructures.sectorOf( sx, sy ), startDay );
	}

	//a forced event's identity: the natural one's, salted with where and when it was forced
	private static long forcedId( Type t, int sx, int sy, int startDay, int startTurn, int wx, int wy ){
		return mix( idOf( t, sx, sy, startDay ) ^ FORCED_SALT, startTurn, (((long) wx) << 32) ^ (wy & 0xFFFFFFFFL) );
	}

	// ------------------------------------------------------------ the ground

	/** What the land is on a world cell, season aside: the wilds and the settlements' works at the annual mean. */
	public static int baseTerrain( long seed, int wx, int wy ){
		WorldModel.Sample s = WorldModel.sample( seed, wx, wy, 0f, null );
		int wild = WorldModel.wildTerrain( seed, wx, wy, s );
		int st = WorldStructures.terrainAt( seed, wx, wy, wild );
		return st != -1 ? st : wild;
	}

	//plain ground a star can come down on
	static boolean openGround( int t ){
		switch (t){
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: case Terrain.EMPTY_SP:
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
			case Terrain.SNOW: case Terrain.FLOWER_PATCH: case Terrain.MUSHROOM_PATCH:
				return true;
			default:
				return false;
		}
	}

	/** Ground a fallen star scorches: open ground and the brush on it - never water, a road, a wall or a tree. */
	public static boolean scorchable( int t ){
		return openGround( t ) || t == Terrain.SHRUB;
	}

	//inside a settlement's berth (its fence, three out from its houses' reach) widened by the
	//crater's two cells and one more, or within eight of any other site's heart (a dragon's
	//lair is five across, then the crater and one)
	static boolean nearSite( long seed, int wx, int wy ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (int sy = sy0 - 1; sy <= sy0 + 1; sy++){
			for (int sx = sx0 - 1; sx <= sx0 + 1; sx++){
				WorldStructures.Site t = WorldStructures.siteType( seed, sx, sy );
				if (t == WorldStructures.Site.NONE) continue;
				int d = Math.max( Math.abs( WorldStructures.siteX( seed, sx, sy ) - wx ),
						Math.abs( WorldStructures.siteY( seed, sx, sy ) - wy ) );
				int reach = t == WorldStructures.Site.VILLAGE
						? WorldStructures.settlementLayout( seed, sx, sy )[0] + 3 + 3 : 8;
				if (d <= reach) return true;
			}
		}
		return false;
	}

	/** Can a star come down on this world cell: open land well away from the town, the water, the
	 *  peaks, the caves' mouths and every site. */
	public static boolean starGround( long seed, int wx, int wy ){
		if (Math.max( Math.abs( wx ), Math.abs( wy ) ) <= TOWN_CLEAR) return false;
		WorldModel.Biome b = WorldModel.baseBiomeAt( seed, wx, wy );
		if (b == WorldModel.Biome.OCEAN || b == WorldModel.Biome.RIVER || b == WorldModel.Biome.MOUNTAIN) return false;
		//the heart and its four sides must be open ground (a plus, so a woodland glade can still take one)
		for (int k = 0; k < 5; k++){
			int x = wx + (k == 1 ? 1 : k == 2 ? -1 : 0), y = wy + (k == 3 ? 1 : k == 4 ? -1 : 0);
			if (!openGround( baseTerrain( seed, x, y ) ) || WindowGenerator.mouthAt( seed, x, y )) return false;
		}
		return !nearSite( seed, wx, wy );
	}

	// ------------------------------------------------------------ fallen stars

	/** The star that falls in sector (sx, sy) on day d, or null: most days none does. */
	public static Event fallenStar( long seed, int sx, int sy, int d ){
		long h = mix( seed ^ STAR_SALT, WorldStructures.sectorOf( sx, sy ), d );
		if (Math.floorMod( h, (long) STAR_ODDS ) != 0) return null;
		int[] at = starSpot( seed, sx, sy, h );
		if (at == null) return null;
		int fall = d * FC + STAR_FALL_FROM + (int) Math.floorMod( h >>> 32, (long) STAR_FALL_SPAN );
		return new Event( Type.FALLEN_STAR, sx, sy, d, fall, fall + STAR_DAYS * FC, at[0], at[1],
				idOf( Type.FALLEN_STAR, sx, sy, d ) );
	}

	//the first of STAR_TRIES hashed cells in the sector's middle that a star can come down on
	static int[] starSpot( long seed, int sx, int sy, long h ){
		for (int k = 0; k < STAR_TRIES; k++){
			long hk = mix( h, k, 0x57A2L );
			int wx = sx * WorldStructures.SECTOR + 12 + (int) Math.floorMod( hk, (long)(WorldStructures.SECTOR - 24) );
			int wy = sy * WorldStructures.SECTOR + 12 + (int) Math.floorMod( hk >>> 24, (long)(WorldStructures.SECTOR - 24) );
			if (starGround( seed, wx, wy )) return new int[]{ wx, wy };
		}
		return null;
	}

	/**
	 * The cells a star scorches, as interleaved world x, y: a ragged round scar five across (the
	 * corners left, a third of the rim spared by the hash), on ground the land offers at the
	 * annual mean and never on a cave mouth. The window lays it only where its own derived ground
	 * agrees (WindowGenerator.overlayScorch).
	 */
	public static int[] craterCells( long seed, Event e ){
		int[] buf = new int[50];
		int n = 0;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int adx = Math.abs( dx ), ady = Math.abs( dy );
				if (adx == 2 && ady == 2) continue;
				if (Math.max( adx, ady ) == 2 && Math.floorMod( mix( e.id ^ CRATER_SALT, dx, dy ), 3L ) == 0) continue;
				int x = e.wx + dx, y = e.wy + dy;
				if (!scorchable( baseTerrain( seed, x, y ) ) || WindowGenerator.mouthAt( seed, x, y )) continue;
				buf[n++] = x;
				buf[n++] = y;
			}
		}
		return java.util.Arrays.copyOf( buf, n );
	}

	/** What else a star brought down, revealed when its fragment is picked up: -1 nothing (three
	 *  stars in five), 0 a ring, 1 a wand (15% each), 2 an artifact (10%). Fixed per star. */
	public static int starBonus( long id ){
		int roll = (int) Math.floorMod( mix( id, BONUS_SALT, 0 ), 20L );
		if (roll < 12) return -1;
		if (roll < 15) return 0;
		if (roll < 18) return 1;
		return 2;
	}

	// ------------------------------------------------------------ markets

	/** The market region (rx, ry) holds in a period, or null: one period in two it is elsewhere,
	 *  and a region without a human village never has one. */
	public static Event market( long seed, int rx, int ry, int period ){
		long h = mix( seed ^ MARKET_SALT, WorldStructures.sectorOf( rx, ry ), period );
		if ((h & 1L) != 0) return null;
		int[] vs = new int[2 * MARKET_REGION * MARKET_REGION];
		int count = 0;
		for (int sy = ry * MARKET_REGION; sy < ry * MARKET_REGION + MARKET_REGION; sy++){
			for (int sx = rx * MARKET_REGION; sx < rx * MARKET_REGION + MARKET_REGION; sx++){
				if (WorldStructures.siteType( seed, sx, sy ) == WorldStructures.Site.VILLAGE
						&& WorldStructures.faction( seed, sx, sy ) == WorldStructures.Faction.HUMAN){
					vs[2 * count] = sx;
					vs[2 * count + 1] = sy;
					count++;
				}
			}
		}
		if (count == 0) return null;
		int pick = (int) Math.floorMod( h >>> 24, (long) count );
		int sx = vs[2 * pick], sy = vs[2 * pick + 1];
		//it rolls on the morning of its fourth day, which is still the period's own
		int start = period * MARKET_PERIOD + (int) Math.floorMod( h >>> 8, (long)(MARKET_PERIOD - MARKET_DAYS) );
		return new Event( Type.MARKET, sx, sy, start, start * FC + MARKET_OPEN, (start + MARKET_DAYS) * FC + MARKET_OPEN,
				WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ),
				idOf( Type.MARKET, sx, sy, start ) );
	}

	// ------------------------------------------------------------ enumeration

	/**
	 * Every event anchored within Chebyshev `radius` sectors of (csx, csy) with anything of it
	 * on the land on some turn of `day` (a star's crater counts), the debug-forced ones among
	 * `forced` included, sorted by id. A pure function of its arguments.
	 */
	public static ArrayList<Event> eventsNear( long seed, int csx, int csy, int radius, int day, List<Event> forced ){
		ArrayList<Event> out = new ArrayList<>();
		for (int sy = csy - radius; sy <= csy + radius; sy++){
			for (int sx = csx - radius; sx <= csx + radius; sx++){
				//a star of up to STAR_SCAR_DAYS days ago may still scar the land today
				for (int d = day - STAR_SCAR_DAYS; d <= day; d++){
					Event e = fallenStar( seed, sx, sy, d );
					if (e != null && e.overlapsDay( day )) out.add( e );
				}
			}
		}
		//a raid lasts its day; a hunt's night begins on one day and runs into the next
		for (int sy = csy - radius; sy <= csy + radius; sy++){
			for (int sx = csx - radius; sx <= csx + radius; sx++){
				Event raid = RaidEvent.raidEvent( seed, sx, sy, day );
				if (raid != null) out.add( raid );
				for (int night = day - 1; night <= day; night++){
					Event hunt = HuntEvent.huntEvent( seed, sx, sy, night );
					if (hunt != null && hunt.overlapsDay( day )) out.add( hunt );
				}
			}
		}
		int period = Math.floorDiv( day, MARKET_PERIOD );
		for (int ry = Math.floorDiv( csy - radius, MARKET_REGION ); ry <= Math.floorDiv( csy + radius, MARKET_REGION ); ry++){
			for (int rx = Math.floorDiv( csx - radius, MARKET_REGION ); rx <= Math.floorDiv( csx + radius, MARKET_REGION ); rx++){
				Event e = market( seed, rx, ry, period );
				if (e != null && within( e, csx, csy, radius ) && e.overlapsDay( day )) out.add( e );
			}
		}
		if (forced != null){
			for (Event e : forced){
				if (!within( e, csx, csy, radius ) || !e.overlapsDay( day )) continue;
				boolean present = false;
				for (Event o : out) present |= o.id == e.id;
				if (!present) out.add( e );
			}
		}
		out.sort( (a, b) -> Long.compare( a.id, b.id ) );
		return out;
	}

	//is the event anchored within Chebyshev r sectors of (csx, csy)?
	static boolean within( Event e, int csx, int csy, int r ){
		return Math.max( Math.abs( e.sx - csx ), Math.abs( e.sy - csy ) ) <= r;
	}

	// ------------------------------------------------------------ what the hero sees

	/**
	 * The events to announce to a hero in sector (hsx, hsy) at a turn: on, neither heard of nor
	 * settled, and within the kind's announce radius - or, for a star, with its impact inside the
	 * window [ox, ox+w) x [oy, oy+h), so no crater lies in his window unheard of. In `near`'s order.
	 */
	public static ArrayList<Event> toAnnounce( List<Event> near, Map<Long, Integer> announced, Map<Long, Integer> resolved,
			int hsx, int hsy, int turn, int ox, int oy, int w, int h ){
		ArrayList<Event> out = new ArrayList<>();
		for (Event e : near){
			//a raid and a hunt say themselves (RaidEvent, HuntEvent)
			if (e.type.announceSectors < 0) continue;
			if (!e.activeAt( turn ) || announced.containsKey( e.id ) || resolved.containsKey( e.id )) continue;
			boolean heard = within( e, hsx, hsy, e.type.announceSectors );
			boolean inView = e.type == Type.FALLEN_STAR && e.wx >= ox && e.wy >= oy && e.wx < ox + w && e.wy < oy + h;
			if (heard || inView) out.add( e );
		}
		return out;
	}

	/** The events the world map pins: on, heard of, not settled, inside the chart's
	 *  [ox, ox+span) x [oy, oy+span). In `events`' order. */
	public static ArrayList<Event> mapMarkers( List<Event> events, Map<Long, Integer> announced, Map<Long, Integer> resolved,
			int turn, int ox, int oy, int span ){
		ArrayList<Event> out = new ArrayList<>();
		for (Event e : events){
			if (!e.activeAt( turn ) || !announced.containsKey( e.id ) || resolved.containsKey( e.id )) continue;
			if (e.wx < ox || e.wy < oy || e.wx >= ox + span || e.wy >= oy + span) continue;
			out.add( e );
		}
		return out;
	}

	private static final String[] OCTANTS = { "e", "ne", "n", "nw", "w", "sw", "s", "se" };

	/** The way from a hero to something (dx, dy) cells off, as a message suffix: "here" within
	 *  STAR_NEAR, otherwise one of the eight winds ("n", "ne" ...; north is up the map, -y). */
	public static String direction( int dx, int dy ){
		if (Math.max( Math.abs( dx ), Math.abs( dy ) ) <= STAR_NEAR) return "here";
		int octant = (int) Math.floorMod( Math.round( Math.atan2( -dy, dx ) / (Math.PI / 4) ), 8L );
		return OCTANTS[octant];
	}

	// ------------------------------------------------------------ debug overrides

	/** A star forced to fall on world cell (wx, wy) at a turn (debug scenes): it goes through the
	 *  same enumeration as every other (WorldEventLog.forced -> eventsNear). */
	public static Event forcedStar( int wx, int wy, int fallTurn ){
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		int d = day( fallTurn );
		return new Event( Type.FALLEN_STAR, sx, sy, d, fallTurn, fallTurn + STAR_DAYS * FC, wx, wy,
				forcedId( Type.FALLEN_STAR, sx, sy, d, fallTurn, wx, wy ) );
	}

	/** A market forced on the village of sector (sx, sy) from a turn until the morning a natural
	 *  one opened that day would go (debug scenes). */
	public static Event forcedMarket( long seed, int sx, int sy, int turn ){
		int d = day( turn );
		int wx = WorldStructures.siteX( seed, sx, sy ), wy = WorldStructures.siteY( seed, sx, sy );
		return new Event( Type.MARKET, sx, sy, d, turn, (d + MARKET_DAYS) * FC + MARKET_OPEN, wx, wy,
				forcedId( Type.MARKET, sx, sy, d, turn, wx, wy ) );
	}

	/** A raid forced on the village of sector (sx, sy) from a turn to the end of its day (debug
	 *  scenes). It keeps the natural raid's id - one raid a village a day - so the favour it earns
	 *  and the shops' prices read it like any other. */
	public static Event forcedRaid( long seed, int sx, int sy, int turn ){
		int d = day( turn );
		return new Event( Type.RAID, sx, sy, d, turn, (d + 1) * FC,
				WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ), idOf( Type.RAID, sx, sy, d ) );
	}

	/** A wolf hunt forced on world cell (wx, wy) from a turn until `endTurn`, the end of its night
	 *  (WorldClock.nightStart of the next; debug scenes): an id of its own. */
	public static Event forcedHunt( int wx, int wy, int turn, int endTurn ){
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		int d = day( turn );
		return new Event( Type.HUNT, sx, sy, d, turn, endTurn, wx, wy, forcedId( Type.HUNT, sx, sy, d, turn, wx, wy ) );
	}

	/** Debug: the nearest ground a star could come down on, by the same rules as the real ones,
	 *  searched sector ring by sector ring out to six from a world cell; null when there is none. */
	public static int[] findStarSpot( long seed, int nearWx, int nearWy ){
		int sx0 = Math.floorDiv( nearWx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( nearWy, WorldStructures.SECTOR );
		for (int r = 0; r <= 6; r++){
			for (int sy = sy0 - r; sy <= sy0 + r; sy++){
				for (int sx = sx0 - r; sx <= sx0 + r; sx++){
					if (Math.max( Math.abs( sx - sx0 ), Math.abs( sy - sy0 ) ) != r) continue;
					int[] at = starSpot( seed, sx, sy, mix( seed ^ FIND_SALT, WorldStructures.sectorOf( sx, sy ), 0 ) );
					if (at != null) return at;
				}
			}
		}
		return null;
	}
}
