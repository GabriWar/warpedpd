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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The roads' own life: who walks the road between two neighbouring villages
 * and when, the big towns' watch walking its beat, where each road's caravan
 * stands today and which of them the outlaws fall on. Like the rest of the
 * world every answer is a pure function of (seed, road, day, hour of the
 * day): a walker is wherever the clock says he has got to by now, so nobody
 * on the road is ever stored (OverworldLevel.park drops them) and every
 * window, reload and machine agrees on who is out there.
 *
 * Positions are world cells along a route: a polyline x0,y0,x1,y1,... from a
 * house door, past the wells, to a house door (for the watch: out of a door,
 * past its own well and along the road to the neighbour's well).
 * OverworldLevel.trafficCell finds the cell a point of it stands for.
 */
public final class RoadTraffic {

	private RoadTraffic(){}

	// ------------------------------------------------------------------ hashing

	private static final long ROAD_SALT = 0x40AD7AFF1CL, AMBUSH_SALT = 0xA3B05C0DEL, BEAT_SALT = 0xB3A7BEA7L,
			CARAVAN_SALT = 0xCA7A7A115L;

	static long mix( long a, long b, long c ){
		long h = a ^ 0x5DEECE66DL;
		h ^= b * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 29 );
		h ^= c * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	/** The road between two villages as one number, whichever end is named first: what its
	 *  walkers and its caravan's trouble are rolled from, and what OverworldLevel remembers an
	 *  ambush already fought out by. */
	static long roadHash( long seed, int sx, int sy, int nx, int ny ){
		long a = WorldStructures.sectorOf( sx, sy ), b = WorldStructures.sectorOf( nx, ny );
		return mix( seed ^ ROAD_SALT, Math.min( a, b ), Math.max( a, b ) );
	}

	// ---------------------------------------------------------------- the clock

	/** Traffic turns per real second on a real-clock run: about the hero's walking cadence. */
	public static final int TURNS_PER_SECOND = 2;
	private static final int REAL_CYCLE = 86_400 * TURNS_PER_SECOND;

	private static boolean realClock(){
		return Dungeon.isChallenged( Challenges.REAL_CLOCK );
	}

	/** Traffic time, in turns: the run's own clock, or on a real-clock run - whose turn clock
	 *  stands still - the local wall clock, TURNS_PER_SECOND to the second. */
	public static long now(){
		if (realClock()){
			long ms = System.currentTimeMillis();
			return realNow( ms + java.util.TimeZone.getDefault().getOffset( ms ) );
		}
		return Dungeon.cycleTurn;
	}

	//a real-clock run's traffic time at a local wall-clock time (the epoch plus the zone's offset)
	static long realNow( long localMs ){
		return Math.floorDiv( localMs, 1000L ) * TURNS_PER_SECOND;
	}

	//...and its day: the local calendar day, as WorldClock.day counts it
	static int realDay( long localMs ){
		return (int) Math.floorDiv( realNow( localMs ), (long) REAL_CYCLE );
	}

	/** Traffic turns in a day: the day and night cycle's, or a real day's. */
	public static int cycle(){
		return realClock() ? REAL_CYCLE : DayNightCycle.FULL_CYCLE;
	}

	/** The day a traffic time falls on: always WorldClock.day()'s day. */
	public static int day( long t ){
		return (int) Math.floorDiv( t, (long) cycle() );
	}

	public static int turnOfDay( long t ){
		return (int) Math.floorMod( t, (long) cycle() );
	}

	/** When walkers set out: late in the dawn. */
	public static int setOut(){
		if (realClock()) return Math.round( (GameCalendar.sunriseSunset()[0] - 0.5f) / 24f * REAL_CYCLE );
		return DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN ) - 100;
	}

	/** When every walker is indoors: just before the last of the dusk. */
	public static int indoors(){
		if (realClock()) return Math.round( (GameCalendar.sunriseSunset()[1] + 0.5f) / 24f * REAL_CYCLE );
		return DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN )
				+ DayNightCycle.phaseDuration( DayNightCycle.Phase.DAY )
				+ DayNightCycle.phaseDuration( DayNightCycle.Phase.DUSK ) - 50;
	}

	/** The debug menu has pinned the hour to night: everybody is indoors, whatever the clock says. */
	public static boolean nightForced(){
		return DayNightCycle.debugPhaseOverride == DayNightCycle.Phase.NIGHT;
	}

	// ----------------------------------------------- roads, caravans, ambushes

	/** The roads among sectors [sx0..sx1]x[sy0..sy1], as {sx, sy, nx, ny}: a village and
	 *  its road neighbour, spoken for by the pair's lower sector - the pairs the caravans
	 *  have always walked. */
	public static ArrayList<int[]> roads( long seed, int sx0, int sy0, int sx1, int sy1 ){
		ArrayList<int[]> out = new ArrayList<>();
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				long nv = WorldStructures.roadNeighbour( seed, sx, sy );
				if (nv == Long.MIN_VALUE) continue;
				//a pair of villages is one road: only its lower sector speaks
				if (WorldStructures.sectorOf( sx, sy ) > nv) continue;
				out.add( new int[]{ sx, sy, (int)(nv >> 32), (int) nv } );
			}
		}
		return out;
	}

	/** Every road drawn from a village among sectors [sx0..sx1]x[sy0..sy1], as {sx, sy, nx, ny}
	 *  lower sector first: the caravans' roads, and also the ones a village runs to a neighbour
	 *  whose own nearest village is another (a road the caravans never took, but people walk). */
	public static ArrayList<int[]> travelPairs( long seed, int sx0, int sy0, int sx1, int sy1 ){
		ArrayList<int[]> out = new ArrayList<>();
		HashSet<Long> seen = new HashSet<>();
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				long nv = WorldStructures.roadNeighbour( seed, sx, sy );
				if (nv == Long.MIN_VALUE) continue;
				long self = WorldStructures.sectorOf( sx, sy );
				//one road, from whichever end it is found
				if (!seen.add( mix( 0x7A1BL, Math.min( self, nv ), Math.max( self, nv ) ) )) continue;
				int nx = (int)(nv >> 32), ny = (int) nv;
				out.add( self <= nv ? new int[]{ sx, sy, nx, ny } : new int[]{ nx, ny, sx, sy } );
			}
		}
		return out;
	}

	/** Is the road walked at all? Nobody sets out to or from an outlaw village, nor from or to a
	 *  place with no lived-in house (a settlement can roll none): a walk goes door to door. */
	public static boolean travelled( long seed, int sx, int sy, int nx, int ny ){
		return WorldStructures.faction( seed, sx, sy ) != WorldStructures.Faction.BANDIT
				&& WorldStructures.faction( seed, nx, ny ) != WorldStructures.Faction.BANDIT
				&& populatedHouses( seed, sx, sy ) > 0 && populatedHouses( seed, nx, ny ) > 0;
	}

	/** Where the road's caravan stands today (world cell), or null when it carries none:
	 *  about half the roads have a stall on a given day, which half turning over with the
	 *  day of the season; it stands a seventh further along each weekday, and every pair
	 *  walks its road one fixed way. (OverworldLevel.placeCaravans pitches it there.) */
	public static int[] caravanSpot( long seed, int sx, int sy, int nx, int ny, int weekday, int dayOfSeason ){
		long ph = caravanHash( sx, sy, nx, ny );
		//about half the roads carry a stall on any given day, and which
		//half turns over from one day of the season to the next
		if ((((ph >>> 3) ^ dayOfSeason) & 1L) != 0) return null;
		float t = caravanAlong( ph, weekday );
		int ax = WorldStructures.siteX( seed, sx, sy );
		int ay = WorldStructures.siteY( seed, sx, sy );
		int bx = WorldStructures.siteX( seed, nx, ny );
		int by = WorldStructures.siteY( seed, nx, ny );
		return new int[]{ Math.round( ax + (bx - ax) * t ), Math.round( ay + (by - ay) * t ) };
	}

	//a pair's own roll for its caravan: which days it is out, and which way it walks
	private static long caravanHash( int sx, int sy, int nx, int ny ){
		return OverworldLevel.structHash( sx * 7919L + nx, sy * 7919L + ny );
	}

	//how far along the road from (sx,sy)'s well to (nx,ny)'s the stall stands on a weekday: an
	//eighth further each day, from the end the pair is walked from. Weekday -1 is that well
	private static float caravanAlong( long ph, int weekday ){
		float t = (weekday + 1) / 8f;
		if ((ph & 1L) != 0) t = 1f - t;    //this pair is walked the other way
		return t;
	}

	/** Cells per turn the caravan's cart makes: a mule in the shafts, a laden cart behind it. */
	public static final float CARAVAN_PACE = 0.35f;
	//a road's cart sets out within this many turns of setting-out time
	private static final int CARAVAN_SPREAD = 150;

	/** Today's caravan of a road on its morning leg (actors/mobs/npcs/CaravanTrain), a Trip of
	 *  kind CARAVAN: from where yesterday's stall would have stood (on the week's first day, the
	 *  well it sets out from) to today's caravanSpot, setting out within CARAVAN_SPREAD turns of
	 *  w0 at CARAVAN_PACE. Its from/to are the villages it is walked from and toward. Null when
	 *  the road carries no caravan today. The stall goes up at its arrive, never before
	 *  (OverworldLevel.placeCaravans). Pure: (seed, road, weekday, day of season, day, w0). */
	public static Trip caravanLeg( long seed, int sx, int sy, int nx, int ny, int weekday, int dayOfSeason, int day, int w0 ){
		int[] spot = caravanSpot( seed, sx, sy, nx, ny, weekday, dayOfSeason );
		if (spot == null) return null;
		long ph = caravanHash( sx, sy, nx, ny );
		float t = caravanAlong( ph, weekday - 1 );
		int ax = WorldStructures.siteX( seed, sx, sy );
		int ay = WorldStructures.siteY( seed, sx, sy );
		int bx = WorldStructures.siteX( seed, nx, ny );
		int by = WorldStructures.siteY( seed, nx, ny );
		float[] route = { ax + (bx - ax) * t, ay + (by - ay) * t, spot[0], spot[1] };
		long h = mix( roadHash( seed, sx, sy, nx, ny ) ^ CARAVAN_SALT, day, 0 );
		int depart = w0 + (int) Math.floorMod( h, (long)(CARAVAN_SPREAD + 1) );
		int turns = Math.max( 1, (int) Math.ceil( length( route ) / CARAVAN_PACE ) );
		boolean forward = (ph & 1L) == 0;
		return new Trip( h, CARAVAN, forward ? sx : nx, forward ? sy : ny, forward ? nx : sx, forward ? ny : sy,
				day, depart, depart + turns, CARAVAN_PACE, route );
	}

	/** Debug scenes: today's caravan of a road on its leg toward a stall `ahead` cells down the
	 *  road from (wx, wy), `behind` cells short of it now - whether or not the road carries one
	 *  today. Its from/to are as the pair is walked. */
	public static Trip forcedCaravanLeg( long seed, int sx, int sy, int nx, int ny, int day, int tod,
			float wx, float wy, float ahead, float behind ){
		long ph = caravanHash( sx, sy, nx, ny );
		boolean forward = (ph & 1L) == 0;
		float[] wells = { WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ),
				WorldStructures.siteX( seed, nx, ny ), WorldStructures.siteY( seed, nx, ny ) };
		if (!forward) wells = new float[]{ wells[2], wells[3], wells[0], wells[1] };
		float len = length( wells );
		float at = project( wells, wx, wy );
		float[] end = point( wells, Math.min( len, at + ahead ) );
		float[] start = point( wells, Math.max( 0f, at + ahead - Math.max( behind + 4f, len / 8f ) ) );
		float[] route = { start[0], start[1], Math.round( end[0] ), Math.round( end[1] ) };
		float leg = length( route );
		int depart = tod - Math.round( Math.max( 0f, leg - behind ) / CARAVAN_PACE );
		return new Trip( Long.MIN_VALUE + 7, CARAVAN, forward ? sx : nx, forward ? sy : ny, forward ? nx : sx, forward ? ny : sy,
				day, depart, depart + Math.max( 1, (int) Math.ceil( leg / CARAVAN_PACE ) ), CARAVAN_PACE, route );
	}

	/** Do the outlaws lie in wait for this road's caravan on this day? One stall in six. */
	public static boolean ambushed( long seed, int sx, int sy, int nx, int ny, int day ){
		return Math.floorMod( mix( roadHash( seed, sx, sy, nx, ny ) ^ AMBUSH_SALT, day, 0 ), 6L ) == 0;
	}

	// ----------------------------------------------------------- the routes

	//the houses with folk in them: the most central ones (OverworldLevel.populateSettlement)
	static int populatedHouses( long seed, int sx, int sy ){
		return WorldStructures.populatedHouses( (WorldStructures.settlementLayout( seed, sx, sy ).length - 1) / 2 );
	}

	//the world cell of the door of house i of the settlement in this sector
	static int doorX( long seed, int sx, int sy, int house ){
		int[] layout = WorldStructures.settlementLayout( seed, sx, sy );
		return WorldStructures.siteX( seed, sx, sy ) + WorldStructures.houseDoorDX( layout[1 + house*2], layout[2 + house*2] );
	}

	static int doorY( long seed, int sx, int sy, int house ){
		int[] layout = WorldStructures.settlementLayout( seed, sx, sy );
		return WorldStructures.siteY( seed, sx, sy ) + WorldStructures.houseDoorDY( layout[1 + house*2], layout[2 + house*2] );
	}

	/** A traveller's way: out of a house door, to the well, along the road to the other
	 *  village's well and in at one of its doors. */
	public static float[] travellerRoute( long seed, int fromSx, int fromSy, int toSx, int toSy, int houseFrom, int houseTo ){
		return new float[]{
				doorX( seed, fromSx, fromSy, houseFrom ), doorY( seed, fromSx, fromSy, houseFrom ),
				WorldStructures.siteX( seed, fromSx, fromSy ), WorldStructures.siteY( seed, fromSx, fromSy ),
				WorldStructures.siteX( seed, toSx, toSy ), WorldStructures.siteY( seed, toSx, toSy ),
				doorX( seed, toSx, toSy, houseTo ), doorY( seed, toSx, toSy, houseTo ) };
	}

	public static float length( float[] route ){
		float len = 0;
		for (int i = 0; i + 3 < route.length; i += 2){
			float dx = route[i+2] - route[i], dy = route[i+3] - route[i+1];
			len += (float) Math.sqrt( dx*dx + dy*dy );
		}
		return len;
	}

	/** The world point s cells along the route (clamped to its ends). */
	public static float[] point( float[] route, float s ){
		for (int i = 0; i + 3 < route.length; i += 2){
			float vx = route[i+2] - route[i], vy = route[i+3] - route[i+1];
			float seg = (float) Math.sqrt( vx*vx + vy*vy );
			if (s <= seg || i + 4 >= route.length){
				float t = seg == 0 ? 0 : Math.max( 0f, Math.min( 1f, s / seg ) );
				return new float[]{ route[i] + vx * t, route[i+1] + vy * t };
			}
			s -= seg;
		}
		return new float[]{ route[0], route[1] };
	}

	/** How far along the route lies its nearest point to (wx, wy) (the first, on a tie). */
	public static float project( float[] route, float wx, float wy ){
		float best = Float.MAX_VALUE, at = 0, acc = 0;
		for (int i = 0; i + 3 < route.length; i += 2){
			float ax = route[i], ay = route[i+1];
			float vx = route[i+2] - ax, vy = route[i+3] - ay;
			float len2 = vx*vx + vy*vy;
			float seg = (float) Math.sqrt( len2 );
			float t = len2 == 0 ? 0 : Math.max( 0f, Math.min( 1f, ((wx - ax)*vx + (wy - ay)*vy) / len2 ) );
			float dx = wx - (ax + vx*t), dy = wy - (ay + vy*t);
			float d = dx*dx + dy*dy;
			if (d < best){
				best = d;
				at = acc + seg * t;
			}
			acc += seg;
		}
		return at;
	}

	// ------------------------------------------------------------ travellers

	public static final int PILGRIM = 0, PEDLAR = 1, MESSENGER = 2, FARMER = 3, KINDS = 4;
	//not a traveller at all: a Trip of this kind is a road's caravan on its morning leg (caravanLeg)
	public static final int CARAVAN = KINDS;
	//cells per turn: the pilgrim plods, the messenger all but runs
	public static final float[] PACE = { 0.40f, 0.45f, 0.90f, 0.50f };
	//walks a lane can hold in a day, at most: a real-clock day holds some two hundred
	private static final int MAX_WALKS = 512;
	//a lane's first walker sets out within this many turns of setting-out time
	private static final int FIRST_SPREAD = 240;
	//turns between one arrival and the next setting out
	private static final int REST_MIN = 30, REST_SPAN = 120;

	/** 1 or 2 lanes (people walking the road one after another) per road, half the roads each. */
	public static int lanes( long seed, int sx, int sy, int nx, int ny ){
		return 1 + (int)((roadHash( seed, sx, sy, nx, ny ) >>> 5) & 1L);
	}

	private static int kindOf( long h ){
		int r = (int) Math.floorMod( h >>> 28, 11L );   //weights 3:3:2:3
		return r < 3 ? PILGRIM : r < 6 ? PEDLAR : r < 8 ? MESSENGER : FARMER;
	}

	public static final class Trip {
		public final long key;                       //who: road, day, lane, which walk of the lane
		public final int kind;
		public final int fromSx, fromSy, toSx, toSy; //the villages it is from and bound for
		public final int day, depart, arrive;        //traffic day; turns of it he sets out / is in
		public final float pace;
		public final float[] route;
		private final float length;

		public Trip( long key, int kind, int fromSx, int fromSy, int toSx, int toSy,
				int day, int depart, int arrive, float pace, float[] route ){
			this.key = key;
			this.kind = kind;
			this.fromSx = fromSx;
			this.fromSy = fromSy;
			this.toSx = toSx;
			this.toSy = toSy;
			this.day = day;
			this.depart = depart;
			this.arrive = arrive;
			this.pace = pace;
			this.route = route;
			length = RoadTraffic.length( route );
		}

		/** How far along the route the walker has got at this turn of the day. */
		public float distanceAt( int tod ){
			return Math.max( 0f, Math.min( length, (tod - depart) * pace ) );
		}
	}

	/** Who is out on this lane of the road at this turn of the day, or null. A lane is one
	 *  walker after another: the first sets out within FIRST_SPREAD turns of w0 in a hashed
	 *  direction; each walks door to door at his kind's pace, the next sets out from where
	 *  he arrived after a rest, the other way; a walk that could not be finished by w1 is
	 *  never started. Pure: (seed, road, lane, day, tod, w0, w1). Only for a travelled() road:
	 *  each end needs a house to walk out of and into. */
	public static Trip tripAt( long seed, int sx, int sy, int nx, int ny, int lane, int day, int tod, int w0, int w1 ){
		if (tod < w0 || tod >= w1) return null;
		long lh = mix( roadHash( seed, sx, sy, nx, ny ), day, lane );
		int t = w0 + (int) Math.floorMod( lh, (long) FIRST_SPREAD );
		boolean forward = ((lh >>> 9) & 1L) == 0;
		for (int walk = 0; walk < MAX_WALKS && t < w1; walk++){
			long h = mix( lh, walk, 0x7EA1L );
			int kind = kindOf( h );
			int fx = forward ? sx : nx, fy = forward ? sy : ny, tx = forward ? nx : sx, ty = forward ? ny : sy;
			int houseFrom = (int) Math.floorMod( h >>> 12, (long) populatedHouses( seed, fx, fy ) );
			int houseTo   = (int) Math.floorMod( h >>> 20, (long) populatedHouses( seed, tx, ty ) );
			float[] route = travellerRoute( seed, fx, fy, tx, ty, houseFrom, houseTo );
			int turns = (int) Math.ceil( length( route ) / PACE[kind] );
			if (t + turns > w1) return null;     //too late to set out: the lane is done till tomorrow
			if (tod < t) return null;            //the next walker has not set out yet
			if (tod < t + turns) return new Trip( h, kind, fx, fy, tx, ty, day, t, t + turns, PACE[kind], route );
			t += turns + REST_MIN + (int) Math.floorMod( h >>> 36, (long)(REST_SPAN + 1) );
			forward = !forward;
		}
		return null;
	}

	/** Debug scenes: a walk of this kind that has got to `ahead` cells past the route's
	 *  nearest point to (wx, wy) right now. */
	public static Trip forcedTrip( long seed, int fromSx, int fromSy, int toSx, int toSy, int kind,
			int day, int tod, float wx, float wy, float ahead, long key ){
		float[] route = travellerRoute( seed, fromSx, fromSy, toSx, toSy, 0, 0 );
		float len = length( route );
		float s = Math.max( 0f, Math.min( len, project( route, wx, wy ) + ahead ) );
		int depart = tod - Math.round( s / PACE[kind] );
		return new Trip( key, kind, fromSx, fromSy, toSx, toSy, day, depart,
				depart + (int) Math.ceil( len / PACE[kind] ), PACE[kind], route );
	}

	// -------------------------------------------------------- the town watch

	/** Does this settlement keep a watch that walks its road? Human towns of nine houses or
	 *  more (the ones with guards at the well: a smaller village has nobody to send) that have
	 *  a road - and not one that runs to an outlaw village: the watch keeps off it, as the
	 *  travellers do. The village at the other end, big or small, sees the watch come by. */
	public static boolean patrolTown( long seed, int sx, int sy ){
		if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE
				|| WorldStructures.faction( seed, sx, sy ) != WorldStructures.Faction.HUMAN
				|| (WorldStructures.settlementLayout( seed, sx, sy ).length - 1) / 2 < 9) return false;
		long nv = WorldStructures.roadNeighbour( seed, sx, sy );
		return nv != Long.MIN_VALUE
				&& WorldStructures.faction( seed, (int)(nv >> 32), (int) nv ) != WorldStructures.Faction.BANDIT;
	}

	/** The watch's beat: out of the door of the lived-in house nearest the road out, past the
	 *  well, and the whole road along to the neighbouring village's well, where it turns round
	 *  (Beat.PAUSE) and walks home. Null when the place keeps no watch. */
	public static float[] beatRoute( long seed, int sx, int sy ){
		if (!patrolTown( seed, sx, sy )) return null;
		long nv = WorldStructures.roadNeighbour( seed, sx, sy );
		int nx = (int)(nv >> 32), ny = (int) nv;
		float ax = WorldStructures.siteX( seed, sx, sy ), ay = WorldStructures.siteY( seed, sx, sy );
		float bx = WorldStructures.siteX( seed, nx, ny ), by = WorldStructures.siteY( seed, nx, ny );
		float len = (float) Math.sqrt( (bx-ax)*(bx-ax) + (by-ay)*(by-ay) );
		if (len < 1f) return null;
		float ux = (bx - ax) / len, uy = (by - ay) / len;
		int radius = WorldStructures.settlementLayout( seed, sx, sy )[0];
		float ex = ax + ux * radius, ey = ay + uy * radius;
		int house = 0;
		float best = Float.MAX_VALUE;
		for (int i = 0; i < populatedHouses( seed, sx, sy ); i++){
			float dx = doorX( seed, sx, sy, i ) - ex, dy = doorY( seed, sx, sy, i ) - ey;
			if (dx*dx + dy*dy < best){
				best = dx*dx + dy*dy;
				house = i;
			}
		}
		return new float[]{ doorX( seed, sx, sy, house ), doorY( seed, sx, sy, house ), ax, ay, bx, by };
	}

	public static final class Beat {
		public static final float PACE = 0.5f;
		public static final int PAUSE = 40;   //turns the watch stands at the neighbour's well looking about
		public static final int REST  = 160;  //turns indoors between two beats
		public final float[] route;
		public final int[] starts;            //turns of the day each beat sets out
		public final int day;
		private final float length;
		private final int walk;

		public Beat( float[] route, int[] starts, int day ){
			this.route = route;
			this.starts = starts;
			this.day = day;
			length = RoadTraffic.length( route );
			walk = (int) Math.ceil( length / PACE );
		}

		public int cycle(){
			return 2 * walk + PAUSE;
		}

		//turns into the beat under way at this turn of the day, or -1 while the watch is indoors
		private int into( int tod ){
			for (int t : starts){
				if (tod < t) return -1;
				if (tod - t < cycle()) return tod - t;
			}
			return -1;
		}

		/** How far along the route (from the door) the guard of this rank is at this turn of
		 *  the day, or -1 while the watch is indoors. The second walks a cell and a half behind. */
		public float distanceAt( int tod, int rank ){
			int u = into( tod );
			if (u < 0) return -1f;
			float s;
			if (u < walk) s = u * PACE - rank * 1.5f;                       //out
			else if (u < walk + PAUSE) s = length - rank * 1.5f;           //at the end of the beat
			else s = length - (u - walk - PAUSE) * PACE + rank * 1.5f;     //back
			return Math.max( 0f, Math.min( length, s ) );
		}

		/** Which way along the route the watch is heading at this turn of the day: 1 out, -1
		 *  back, 0 standing at the end of the beat (or indoors). */
		public int heading( int tod ){
			int u = into( tod );
			if (u < 0) return 0;
			return u < walk ? 1 : u < walk + PAUSE ? 0 : -1;
		}
	}

	/** The watch's day: beats from w0+40 + 0..119 turns, one after another with REST indoors
	 *  between, while a whole beat still fits before w1 - two on a long summer's day, one in
	 *  winter, so the pair is always home by dusk. Null when no watch or no beat fits (a road
	 *  too long to walk there and back in a short day keeps the watch at home that day). */
	public static Beat beat( long seed, int sx, int sy, int day, int w0, int w1 ){
		float[] route = beatRoute( seed, sx, sy );
		if (route == null) return null;
		int cycle = new Beat( route, new int[0], day ).cycle();
		long h = mix( seed ^ BEAT_SALT, WorldStructures.sectorOf( sx, sy ), day );
		ArrayList<Integer> list = new ArrayList<>();
		for (int t = w0 + 40 + (int) Math.floorMod( h, 120L ); t + cycle <= w1; t += cycle + Beat.REST) list.add( t );
		if (list.isEmpty()) return null;
		int[] starts = new int[list.size()];
		for (int i = 0; i < starts.length; i++) starts[i] = list.get( i );
		return new Beat( route, starts, day );
	}

	/** Debug scenes: the watch of this town out on its beat `ahead` cells past the beat's
	 *  nearest point to (wx, wy), walking out. */
	public static Beat forcedBeat( long seed, int sx, int sy, int day, int tod, float wx, float wy, float ahead ){
		float[] route = beatRoute( seed, sx, sy );
		if (route == null) return null;
		float s = Math.max( 0f, Math.min( length( route ), project( route, wx, wy ) + ahead ) );
		return new Beat( route, new int[]{ tod - Math.round( s / Beat.PACE ) }, day );
	}

	// --------------------------------------------------------------- rumours

	public static final int RUMOUR_DRAGON = 0, RUMOUR_RUIN = 1, RUMOUR_OUTLAWS = 2, RUMOUR_CARAVAN = 3, RUMOUR_WATCHED = 4;
	private static final int RUMOUR_NEAR = 24, RUMOUR_FAR = 320;

	/** Something real a walker at (wx, wy) has heard of: {kind, x, y} of the nearest live
	 *  dragon lair, ruin, outlaw village or today's caravan (WATCHED when its outlaws lie in
	 *  wait today) within three sectors, 24-320 cells off; pick chooses among the kinds on
	 *  offer. A caravan counts only where its stall can go up, on a road. Null when there is
	 *  nothing to tell. */
	public static int[] rumour( long seed, int wx, int wy, long pick, int day, int weekday, int dayOfSeason, Set<Long> cleared ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		int[][] best = new int[RUMOUR_CARAVAN + 1][];
		long[] bestD = new long[RUMOUR_CARAVAN + 1];
		Arrays.fill( bestD, Long.MAX_VALUE );
		for (int sy = sy0 - 3; sy <= sy0 + 3; sy++){
			for (int sx = sx0 - 3; sx <= sx0 + 3; sx++){
				WorldStructures.Site t = WorldStructures.siteType( seed, sx, sy );
				int kind;
				if (t == WorldStructures.Site.DRAGON && !cleared.contains( WorldStructures.sectorOf( sx, sy ) )) kind = RUMOUR_DRAGON;
				else if (t == WorldStructures.Site.RUIN) kind = RUMOUR_RUIN;
				else if (t == WorldStructures.Site.VILLAGE
						&& WorldStructures.faction( seed, sx, sy ) == WorldStructures.Faction.BANDIT) kind = RUMOUR_OUTLAWS;
				else continue;
				heard( best, bestD, kind, WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ), wx, wy, 0 );
			}
		}
		//today's stalls, nearest first, until one stands where a stall can stand
		ArrayList<int[]> stalls = new ArrayList<>();
		for (int[] r : roads( seed, sx0 - 3, sy0 - 3, sx0 + 3, sy0 + 3 )){
			int[] spot = caravanSpot( seed, r[0], r[1], r[2], r[3], weekday, dayOfSeason );
			if (spot == null) continue;
			long dx = spot[0] - wx, dy = spot[1] - wy, d = dx*dx + dy*dy;
			if (d < (long) RUMOUR_NEAR * RUMOUR_NEAR || d > (long) RUMOUR_FAR * RUMOUR_FAR) continue;
			stalls.add( new int[]{ spot[0], spot[1], ambushed( seed, r[0], r[1], r[2], r[3], day ) ? 1 : 0 } );
		}
		java.util.Collections.sort( stalls, (a, b) -> Long.compare(
				(long)(a[0] - wx) * (a[0] - wx) + (long)(a[1] - wy) * (a[1] - wy),
				(long)(b[0] - wx) * (b[0] - wx) + (long)(b[1] - wy) * (b[1] - wy) ) );
		for (int[] s : stalls){
			if (!roadNear( seed, s[0], s[1] )) continue;
			heard( best, bestD, RUMOUR_CARAVAN, s[0], s[1], wx, wy, s[2] );
			break;
		}
		ArrayList<int[]> heard = new ArrayList<>();
		for (int[] b : best) if (b != null) heard.add( b );
		if (heard.isEmpty()) return null;
		int[] h = heard.get( (int) Math.floorMod( pick >>> 7, (long) heard.size() ) );
		return new int[]{ h[0] == RUMOUR_CARAVAN && h[3] == 1 ? RUMOUR_WATCHED : h[0], h[1], h[2] };
	}

	private static void heard( int[][] best, long[] bestD, int kind, int x, int y, int wx, int wy, int flag ){
		long dx = x - wx, dy = y - wy, d = dx*dx + dy*dy;
		if (d < (long) RUMOUR_NEAR * RUMOUR_NEAR || d > (long) RUMOUR_FAR * RUMOUR_FAR || d >= bestD[kind]) return;
		bestD[kind] = d;
		best[kind] = new int[]{ kind, x, y, flag };
	}

	/** Is there road within four cells of a world cell? Where there is none (rock, ice), a
	 *  caravan has nowhere to pitch its stall (OverworldLevel.roadSpotNear). */
	public static boolean roadNear( long seed, int x, int y ){
		for (int r = 0; r <= 4; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (WorldStructures.terrainAt( seed, x + dx, y + dy ) == Terrain.DIRT_PATH) return true;
				}
			}
		}
		return false;
	}

	/** 8-way compass of a world offset (y grows south): 0 east, 1 north-east, 2 north ... 7 south-east. */
	public static int compass( int dx, int dy ){
		return (int) Math.floorMod( Math.round( Math.atan2( -dy, dx ) / (Math.PI / 4) ), 8L );
	}
}
