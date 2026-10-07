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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

/**
 * The settlements' day: who does what (a trade rolled from the family hash), where they
 * should be at this hour and in this weather, and where on the live window that is. The
 * schedule is a pure function of (world seed, settlement, house, member, clock), so it is
 * stable across rebases, reloads and multiplayer; the level side only resolves the spot to
 * a free window cell (Settler walks or snaps there).
 */
public final class VillageRoutine {

	private VillageRoutine(){}

	//within this of any hero a settler walks; further off it simply is where it should be.
	//the surface sees 20 cells, so nothing past this is ever on screen in play
	public static final int NEAR = 30;
	//a gnoll hunter walks the fence a few cells at a time - HUNT_STRIDE cells on every
	//HUNT_LEG turns - so each leg runs along it rather than across the camp. PathFinder steps
	//straight before it steps aslant, so a longer leg round a corner would cut inside the band
	public static final int HUNT_LEG = 25, HUNT_STRIDE = 5;
	//a gatherer moves FORAGE_STRIDE cells along the brush every FORAGE_LEG turns
	public static final int FORAGE_LEG = 240, FORAGE_STRIDE = 5;
	//how much of the day a break at the well takes
	public static final float BREAK_LENGTH = 0.08f;
	//a change in the sky has to hold this many turns before the villages believe it
	public static final int WEATHER_HOLD = 20;
	//paths worked out per turn, across every settler: a slot change that strikes a whole
	//village on one turn is spread over the next few instead
	public static final int PATHS_PER_TURN = 3;

	//the guards' posts by the well: populateSettlement's NEIGHBOURS8[(g*2)%8]*2
	static final int[][] GUARD_POSTS = { {-2,-2}, {2,-2}, {2,0}, {0,2} };
	//which row of the house a member of the family keeps to: the middle, then either side
	static final int[] MEMBER_ROW = { 0, -1, 1 };
	//what a gatherer picks through (a farmer stoops over the village's own fields: besideField)
	static final int[] BRUSHY = { Terrain.HIGH_GRASS, Terrain.SHRUB, Terrain.TREE_OAK, Terrain.TREE_PINE,
			Terrain.FLOWER_PATCH, Terrain.MUSHROOM_PATCH };

	//ORDINALS ARE SAVED (Settler's bundle): append only, never reorder
	public enum Role { FARMER, FISHER, CRAFTER, ELDER, LOAFER, HUNTER, GATHERER }
	//ORDINALS ARE SAVED (Settler's bundle): append only, never reorder
	public enum Slot { BED, INDOORS, WORK, BREAK, GATHER }

	public enum Weather { FAIR, RAIN, SNOW, STORM }
	//what a spot is: where a settler stands for it, and what it does there
	public enum Kind { HOME, STREET, FIELD, SHORE, WELL, RING, BRUSH }

	// ------------------------------------------------------------ hashes

	//a splitmix-style finaliser over the family hash and one member
	static long mix( long h, int member ){
		long x = h ^ ((member + 1) * 0x9E3779B97F4A7C15L);
		x ^= x >>> 31; x *= 0xBF58476D1CE4E5B9L;
		x ^= x >>> 29; x *= 0x94D049BB133111EBL;
		x ^= x >>> 32;
		return x;
	}

	/** The hash a house's family is rolled from: the one populateSettlement gives them their look by. */
	public static long familyHash( int sx, int sy, int house ){
		return OverworldLevel.familyHash( sx, sy, house );
	}

	/** A member's trade: a gnoll hunts or gathers, half and half; a human farms (35%), fishes
	 *  (15%), keeps a craft (15%), is an elder (10%) or loafs about the well (25%). */
	public static Role roleOf( long family, int member, boolean gnoll ){
		long h = mix( family, member );
		if (gnoll) return (h & 1L) == 0 ? Role.HUNTER : Role.GATHERER;
		int roll = (int) Math.floorMod( h >>> 1, 20L );
		if (roll < 7)  return Role.FARMER;
		if (roll < 10) return Role.FISHER;
		if (roll < 13) return Role.CRAFTER;
		if (roll < 15) return Role.ELDER;
		return Role.LOAFER;
	}

	/** Is this a trade of that people? (an older save's role of the wrong kind is rolled again) */
	public static boolean belongs( Role r, boolean gnoll ){
		return gnoll == (r == Role.HUNTER || r == Role.GATHERER);
	}

	/** A member's own timing, so a village does not move as one:
	 *  bits 0-7 when it wakes, 8-15 how long it lingers at the well after dark, 16-17 whether it
	 *  takes a midday break (when both are 0), 20-27 how early it quits at dusk, 28-31 when its
	 *  break starts; 24-28 how many turns it takes to react to a change in the weather (shared
	 *  with the others: a staggering, not a trait). */
	public static int personal( long family, int member ){
		return (int)(mix( family ^ 0x7E25L, member ) >>> 16);
	}

	/** Which of its trade's spots a member takes (a field cell, a bank, a place at the well). */
	public static long spotHash( long family, int member ){
		return mix( family ^ 0x5B07L, member );
	}

	// ------------------------------------------------------------ the clock

	/** The hour and the sky, as every settler of the window reads them this turn. */
	public static final class Clock {
		public final DayNightCycle.Phase phase;
		public final float progress;
		public final boolean winter;
		//WorldClock.day(): the places at the well turn over with it
		public final int day;
		//the game turn this reading is for: the gnoll rounds move on with it (beatOf)
		public final int turn;
		//the sky as the villages have taken it in (Latch), what it was before its last change
		//and before the one ahead of that, and the game turns of those two changes. a change
		//holds WEATHER_HOLD turns before the next can come, so no member's delay (under 32
		//turns) reaches back past them
		public final Weather weather, before, earlier;
		public final int changed, prevChanged;

		public Clock( DayNightCycle.Phase phase, float progress, Weather weather, boolean winter, int day, int turn ){
			this( phase, progress, weather, weather, weather, 0, 0, turn, winter, day );
		}

		public Clock( DayNightCycle.Phase phase, float progress, Weather weather, Weather before, Weather earlier,
				int changed, int prevChanged, int turn, boolean winter, int day ){
			this.phase = phase;
			this.progress = progress;
			this.weather = weather;
			this.before = before;
			this.earlier = earlier;
			this.changed = changed;
			this.prevChanged = prevChanged;
			this.turn = turn;
			this.winter = winter;
			this.day = day;
		}

		/** The weather a settler acts on: the sky as the villages believed it its own few turns
		 *  ago (weatherDelay), so a shower does not empty the fields in a single turn, and each
		 *  member lives every change in order - a short shower too, as long as the rest. */
		public Weather weatherFor( int p ){
			int at = turn - weatherDelay( p );
			return at >= changed ? weather : at >= prevChanged ? before : earlier;
		}
	}

	/** The sky as the villages believe it: a new reading has to hold WEATHER_HOLD turns before
	 *  it replaces the last, so a front hovering at the edge of rain (ClimateManager drops a
	 *  rate under 0.01 to nothing) does not send a village in and out every few turns. */
	static final class Latch {
		Weather weather, before, earlier, pending;
		int changed, prevChanged, pendingSince, last;

		void read( Weather raw, int turn ){
			if (weather == null || turn < last || turn - last > WEATHER_HOLD){
				//the first reading, or time jumped (a level switch, a long rest, a debug clock
				//walk): whatever the sky does now is what everyone already knows
				weather = before = earlier = raw;
				changed = turn - 32;   //past every settler's delay (weatherDelay < 32)
				prevChanged = changed - 32;
				pending = null;
			} else if (raw == weather){
				pending = null;
			} else if (raw != pending){
				pending = raw;
				pendingSince = turn;
			} else if (turn - pendingSince >= WEATHER_HOLD){
				earlier = before;
				before = weather;
				prevChanged = changed;
				weather = raw;
				changed = turn;
				pending = null;
			}
			last = turn;
		}
	}

	private static final Latch sky = new Latch();
	private static Clock now;
	private static int nowCycle = Integer.MIN_VALUE, nowTurn = Integer.MIN_VALUE;

	//the game's own turn: Statistics.duration only takes in the time that passed when
	//fixTime runs, the rest sits in Actor.now() (FarmCrop.clock). on a real-clock run this,
	//not Dungeon.cycleTurn, is what moves
	static int gameTurn(){
		return (int)(Statistics.duration + Actor.now());
	}

	/** The hour and the sky, worked out once a turn for every settler in the window. */
	public static Clock clock(){
		int cycle = Dungeon.cycleTurn, turn = gameTurn();
		if (now == null || cycle != nowCycle || turn != nowTurn){
			sky.read( weather(), turn );
			now = new Clock( DayNightCycle.phase(), DayNightCycle.phaseProgress(), sky.weather, sky.before, sky.earlier,
					sky.changed, sky.prevChanged, turn, GameCalendar.season() == GameCalendar.Season.WINTER, WorldClock.day() );
			nowCycle = cycle;
			nowTurn = turn;
		}
		return now;
	}

	/** The weather over the hero, as the schedule sorts it: a storm with nothing falling is fair. */
	public static Weather weather(){
		if (ClimateManager.localPrecipRate() <= 0f) return Weather.FAIR;
		PrecipType t = ClimateManager.localPrecipType();
		if (t == PrecipType.NONE) return Weather.FAIR;
		if (ClimateManager.isStorming() || t == PrecipType.BLIZZARD || t == PrecipType.HAIL
				|| (t == PrecipType.SNOW && ClimateManager.localWindSpeed() > 12f)) return Weather.STORM;
		if (t == PrecipType.SNOW) return Weather.SNOW;
		return Weather.RAIN;   //rain, sleet
	}

	private static int pathCycle = Integer.MIN_VALUE, pathTurn = Integer.MIN_VALUE, paths;

	/** One of this turn's few paths (PATHS_PER_TURN), or false once they are spent: the
	 *  settler waits a turn and asks again. */
	public static boolean takePath(){
		int cycle = Dungeon.cycleTurn, turn = gameTurn();
		if (cycle != pathCycle || turn != pathTurn){
			pathCycle = cycle;
			pathTurn = turn;
			paths = 0;
		}
		if (paths >= PATHS_PER_TURN) return false;
		paths++;
		return true;
	}

	// ------------------------------------------------------------ the schedule

	static float unit( int p, int shift ){ return ((p >>> shift) & 0xFF) / 255f; }

	//when in the dawn a member is up and out
	static float wake( Role r, int p ){
		float u = unit( p, 0 );
		switch (r){
			case FISHER: case HUNTER:  return 0.05f + 0.25f * u;   //first out, before the light
			case FARMER:               return 0.10f + 0.30f * u;
			case ELDER: case GATHERER: return 0.20f + 0.30f * u;
			case CRAFTER:              return 0.30f + 0.30f * u;
			default:                   return 0.55f + 0.35f * u;   //a loafer sleeps in
		}
	}

	//how far into the night a member stays at the well (the old folk are long abed)
	static float linger( Role r, int p ){
		if (r == Role.ELDER) return 0f;
		return (r == Role.LOAFER ? 0.25f : 0.12f) * unit( p, 8 );
	}

	static boolean takesBreak( Role r, int p ){
		return (r == Role.FARMER || r == Role.FISHER || r == Role.CRAFTER || r == Role.GATHERER)
				&& ((p >>> 16) & 3) == 0;
	}

	static float breakStart( int p ){ return 0.42f + 0.06f * (((p >>> 28) & 15) / 15f); }

	//how early in the dusk a member quits work for the well
	static float quit( int p ){ return 0.20f * unit( p, 20 ); }

	/** How many turns a member takes to act on a change in the weather. */
	public static int weatherDelay( int p ){ return (p >>> 24) & 31; }

	/**
	 * Where in its day a member is. Night: abed, after a while at the well for some. Dawn:
	 * abed until its own waking point, then at work. Day: at work, with a break at the well
	 * for a quarter of the workers. Dusk: work wound up at a staggered point, then the well -
	 * the fishers and hunters stay out for the evening, the elders go in halfway through.
	 * Rain or a storm sends everyone indoors; falling snow keeps the farmers and gatherers in.
	 */
	public static Slot slotFor( Role r, Clock c, int p ){
		Slot s;
		float t = c.progress;
		switch (c.phase){
			case NIGHT:
				s = t < linger( r, p ) ? Slot.GATHER : Slot.BED;
				break;
			case DAWN:
				s = t < wake( r, p ) ? Slot.BED : Slot.WORK;
				break;
			case DAY: {
				float b = breakStart( p );
				s = takesBreak( r, p ) && t >= b && t < b + BREAK_LENGTH ? Slot.BREAK : Slot.WORK;
				break;
			}
			default: {   //DUSK
				float q = quit( p );
				if (r == Role.ELDER && t >= 0.5f) s = Slot.BED;                                  //old folk turn in early
				else if ((r == Role.FISHER || r == Role.HUNTER) && t < 0.25f + q) s = Slot.WORK; //the evening rise, the last round
				else if (t < q) s = Slot.WORK;                                                   //finishing up
				else s = Slot.GATHER;
			}
		}
		if (s == Slot.BED) return s;
		switch (c.weatherFor( p )){
			case STORM: case RAIN:
				return Slot.INDOORS;
			case SNOW:
				return (r == Role.FARMER || r == Role.GATHERER) && (s == Slot.WORK || s == Slot.BREAK) ? Slot.INDOORS : s;
			default:
				return s;
		}
	}

	/** Where a member's day goes while a travelling market is in its settlement (WorldEvents):
	 *  the well is where everything is. The idle, the old and the craftsmen leave their work for
	 *  the stalls, and a break is spent among them; the fields, the banks, the evening and the
	 *  night keep their own. */
	public static Slot marketSlot( Role r, Slot s ){
		if (s == Slot.BREAK) return Slot.GATHER;
		if (s == Slot.WORK && (r == Role.LOAFER || r == Role.ELDER || r == Role.CRAFTER)) return Slot.GATHER;
		return s;
	}

	/** The leg of its round a member's working spot was set for: a hunter's moves on along
	 *  the fence every HUNT_LEG turns and a gatherer's along the brush every FORAGE_LEG, each
	 *  on a turn of its own (spot), so a clan never sets off as one; every other spot holds
	 *  for the whole slot. */
	public static int beatOf( Role r, Slot s, Clock c, long spot ){
		if (s != Slot.WORK) return 0;
		int leg = r == Role.HUNTER ? HUNT_LEG : r == Role.GATHERER ? FORAGE_LEG : 0;
		if (leg == 0) return 0;
		return Math.floorDiv( c.turn + (int) Math.floorMod( spot >>> 40, (long) leg ), leg );
	}

	// ------------------------------------------------------------ settlements

	/** One human or gnoll settlement, as the schedule reads it. */
	public static final class Settlement {
		public final long key;
		public final int sx, sy, cx, cy, radius;
		public final boolean gnoll;
		final int[] layout;
		//its fields (WorldStructures.fieldPlots): none for a gnoll clan, or where the land takes no plough
		final int[] plots;

		Settlement( long key, int sx, int sy, int cx, int cy, boolean gnoll, int[] layout, int[] plots ){
			this.key = key;
			this.sx = sx;
			this.sy = sy;
			this.cx = cx;
			this.cy = cy;
			this.radius = layout[0];
			this.gnoll = gnoll;
			this.layout = layout;
			this.plots = plots;
		}

		/** Does the place have fields to work? */
		public boolean farms(){ return plots.length > 0; }

		public int houses(){ return (layout.length - 1) / 2; }
		public int houseX( int h ){ return cx + layout[1 + 2*h]; }
		public int houseY( int h ){ return cy + layout[2 + 2*h]; }
		//the families live in the lived-in houses past the vendors' (populateSettlement)
		public int firstFamily(){ return gnoll ? 0 : vendorHouses( houses() ); }
		public int lastFamily(){ return WorldStructures.populatedHouses( houses() ); }   //exclusive
	}

	/** The settlement of a packed sector key, or null: no village there, or a bandit camp
	 *  (nobody keeps a day there), or no key at all (a villager spawned by hand). */
	public static Settlement settlement( long seed, long key ){
		if (key == Long.MIN_VALUE) return null;
		int sx = (int)(key >> 32), sy = (int) key;
		if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE) return null;
		WorldStructures.Faction f = WorldStructures.faction( seed, sx, sy );
		if (f == WorldStructures.Faction.BANDIT) return null;
		return new Settlement( key, sx, sy, WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ),
				f == WorldStructures.Faction.GNOLL, WorldStructures.settlementLayout( seed, sx, sy ),
				WorldStructures.fieldPlots( seed, sx, sy ) );
	}

	/** How many of a human settlement's first houses are shops: village 1, town 2, city 3,
	 *  metropolis 4 (populateSettlement). */
	public static int vendorHouses( int houses ){
		return houses >= 45 ? 4 : houses >= 18 ? 3 : houses >= 9 ? 2 : houses >= 5 ? 1 : 0;
	}

	/** The family house nearest a world cell (Chebyshev to its centre, ties to the lower index),
	 *  passing over any house a vendor keeps shop in when the level is given. */
	public static int nearestHouse( Settlement s, int wx, int wy, OverworldLevel ow ){
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int h = s.firstFamily(); h < s.lastFamily(); h++){
			if (ow != null && ow.shopAt( s.key, s.houseX( h ), s.houseY( h ) )) continue;
			int d = Math.max( Math.abs( wx - s.houseX( h ) ), Math.abs( wy - s.houseY( h ) ) );
			if (d < bestD){
				bestD = d;
				best = h;
			}
		}
		return best != -1 ? best : s.firstFamily();
	}

	/** The guard post by the well nearest a world cell (ties to the lower post). */
	public static int nearestPost( Settlement s, int wx, int wy ){
		int best = 0, bestD = Integer.MAX_VALUE;
		for (int g = 0; g < GUARD_POSTS.length; g++){
			int d = Math.max( Math.abs( wx - s.cx - GUARD_POSTS[g][0] ), Math.abs( wy - s.cy - GUARD_POSTS[g][1] ) );
			if (d < bestD){
				bestD = d;
				best = g;
			}
		}
		return best;
	}

	/** The k-th of the 8r cells of the Chebyshev ring r, clockwise from its north-west corner:
	 *  every cell once for k in [0, 8r), neighbouring k neighbouring cells. */
	public static int ringX( int r, int k ){
		int side = k / (2*r), t = k % (2*r) - r;
		switch (side){ case 0: return t; case 1: return r; case 2: return -t; default: return -r; }
	}

	public static int ringY( int r, int k ){
		int side = k / (2*r), t = k % (2*r) - r;
		switch (side){ case 0: return -r; case 1: return t; case 2: return r; default: return -t; }
	}

	//a house's door, and the step from it into the house: with I the step and H the house's
	//centre, the door is H - 2I, the cell just inside it H - I, the middle row H, the back
	//row H + I, and the threshold outside H - 3I
	public static int doorX( Settlement s, int homeX, int homeY ){ return s.cx + WorldStructures.houseDoorDX( homeX - s.cx, homeY - s.cy ); }
	public static int doorY( Settlement s, int homeX, int homeY ){ return s.cy + WorldStructures.houseDoorDY( homeX - s.cx, homeY - s.cy ); }
	public static int inX( Settlement s, int homeX, int homeY ){ return Integer.signum( homeX - doorX( s, homeX, homeY ) ); }
	public static int inY( Settlement s, int homeX, int homeY ){ return Integer.signum( homeY - doorY( s, homeX, homeY ) ); }

	// ------------------------------------------------------------ spots

	/** Where a member wants to be: a world cell, what kind of place it is, how far round it the
	 *  level may look for a free cell (search) and how far the member wanders there (amble). */
	public static final class Target {
		public final int wx, wy;
		public final Kind kind;
		public final int search, amble;
		public final int ringMin, ringMax;  //the band of Chebyshev rings from the settlement centre it keeps to, -1 for none
		public final int homeX, homeY;      //HOME: the house
		public final int beat;              //the round it was set for (beatOf)

		Target( int wx, int wy, Kind kind, int search, int amble, int ringMin, int ringMax, int homeX, int homeY, int beat ){
			this.wx = wx;
			this.wy = wy;
			this.kind = kind;
			this.search = search;
			this.amble = amble;
			this.ringMin = ringMin;
			this.ringMax = ringMax;
			this.homeX = homeX;
			this.homeY = homeY;
			this.beat = beat;
		}
	}

	/**
	 * The spot of a member of the family in house `house` (centred on homeX, homeY) for a
	 * part of the day. Abed: the back row of their own house, one bed each. Indoors: its
	 * middle row. At the well: a place on its third ring that turns over every day, the
	 * family's first two side by side to talk. At work: a farmer's place at the edge of one of
	 * the village's fields, outside the fence (in winter, or in a village with no fields, the
	 * doorstep and the yard), a fisher's bank among the nearest
	 * (`shores`, nearest first; none: the fields), a crafter beside the threshold, an elder on
	 * the bench a step further out, a loafer about the well, a gnoll hunter's stretch of the
	 * fence and a gatherer's of the brush (each moving on with its own legs, beatOf).
	 */
	public static Target target( Settlement s, int house, int homeX, int homeY, int member, Role r, Slot slot,
			Clock c, long[] shores ){
		long family = familyHash( s.sx, s.sy, house );
		long spot = spotHash( family, member );
		int ix = inX( s, homeX, homeY ), iy = inY( s, homeX, homeY );
		int px = iy, py = ix;   //along the wall
		int row = MEMBER_ROW[Math.floorMod( member, 3 )];
		int side = Math.floorMod( member, 2 ) == 0 ? 1 : -1;
		int dx = doorX( s, homeX, homeY ), dy = doorY( s, homeX, homeY );
		switch (slot){
			case BED:     return new Target( homeX + ix + px*row, homeY + iy + py*row, Kind.HOME, 1, 0, -1, -1, homeX, homeY, 0 );
			case INDOORS: return new Target( homeX + px*row, homeY + py*row, Kind.HOME, 1, 0, -1, -1, homeX, homeY, 0 );
			case BREAK: case GATHER: return well( s, c, member, spot, spotHash( family, 0 ), 0 );
			default: break;
		}
		switch (r){
			case LOAFER:  return well( s, c, member, spot, spotHash( family, 0 ), 1 );
			case CRAFTER: return street( dx - ix + px*side, dy - iy + py*side, 1 );
			case ELDER:   return street( dx - 2*ix + px*side, dy - 2*iy + py*side, 0 );
			case FARMER:  return c.winter || !s.farms() ? street( dx - ix + px*side, dy - iy + py*side, 1 ) : field( s, spot );
			case FISHER:
				if (shores.length == 0) return target( s, house, homeX, homeY, member, Role.FARMER, slot, c, shores );
				return shore( shores[(int) Math.floorMod( spot >>> 12, (long) Math.min( 6, shores.length ) )] );
			case HUNTER:  return ring( s, spot, beatOf( r, slot, c, spot ) );
			default:      return brush( s, spot, beatOf( r, slot, c, spot ) );   //GATHERER
		}
	}

	static Target street( int wx, int wy, int amble ){
		return new Target( wx, wy, Kind.STREET, 2, amble, -1, -1, 0, 0, 0 );
	}

	//a place on the well's third ring, a new one every day. the family's first two take a
	//pair of cells side by side on its north or south row, so they stand face to face
	//(a sprite only turns left and right)
	static Target well( Settlement s, Clock c, int member, long spot, long spot0, int amble ){
		int k;
		if (member == 0 || member == 1){
			int pair = (int) Math.floorMod( mix( spot0, c.day ), 6L );
			k = (pair < 3 ? 0 : 12) + 2 * (pair % 3) + member;
		} else {
			k = (int) Math.floorMod( mix( spot, c.day ), 24L );
		}
		return new Target( s.cx + ringX( 3, k ), s.cy + ringY( 3, k ), Kind.WELL, 2, amble, 1, 5, 0, 0, 0 );
	}

	//a cell at the edge of one of the village's fields, the ring of cells round its plot: the
	//lane between the fence and the plots and the ground about them is the band it keeps to
	static Target field( Settlement s, long spot ){
		int n = s.plots.length / 4;
		int p = 4 * (int) Math.floorMod( spot >>> 12, (long) n );
		int x0 = s.plots[p] - 1, y0 = s.plots[p+1] - 1, x1 = s.plots[p+2] + 1, y1 = s.plots[p+3] + 1;
		int w = x1 - x0, h = y1 - y0;
		int k = (int) Math.floorMod( spot >>> 24, 2L * (w + h) );
		int wx, wy;
		if (k < w){              wx = x0 + k;           wy = y0; }
		else if (k < w + h){     wx = x1;               wy = y0 + (k - w); }
		else if (k < 2*w + h){   wx = x1 - (k - w - h); wy = y1; }
		else {                   wx = x0;               wy = y1 - (k - 2*w - h); }
		return new Target( wx, wy, Kind.FIELD, 3, 2, s.radius + WorldStructures.FIELD_RING0 - 1,
				s.radius + WorldStructures.FIELD_REACH + 1, 0, 0, 0 );
	}

	static Target shore( long key ){
		return new Target( (int)(key & 0xFFFFFFFFL), (int)(key >> 32), Kind.SHORE, 1, 1, -1, -1, 0, 0, 0 );
	}

	//just inside the fence, HUNT_STRIDE cells further round it every leg: short enough that
	//the shortest way there keeps to the fence's band, a corner included
	static Target ring( Settlement s, long spot, int leg ){
		int R = s.radius + 1;
		int k = (int) Math.floorMod( (spot >>> 12) + (long) leg * HUNT_STRIDE, 8L * R );
		return new Target( s.cx + ringX( R, k ), s.cy + ringY( R, k ), Kind.RING, 2, 1, s.radius, s.radius + 1, 0, 0, leg );
	}

	//the brush of the outer ring, FORAGE_STRIDE cells further along every leg
	static Target brush( Settlement s, long spot, int leg ){
		int R = s.radius;
		int k = (int) Math.floorMod( (spot >>> 12) + (long) leg * FORAGE_STRIDE, 8L * R );
		return new Target( s.cx + ringX( R, k ), s.cy + ringY( R, k ), Kind.BRUSH, 4, 1, R - 2, R + 1, 0, 0, leg );
	}

	// ------------------------------------------------------------ chatter

	/** The line set a member speaks from: the weather and the break have their own, a farmer's
	 *  winter work too and his yard in a village with no fields (`farms` false), a loafer by
	 *  day keeps the old idle lines (line_N), and the rest are trade by hour (farmer_dawn,
	 *  hunter_night...). */
	public static String chatStem( Role r, Slot s, Clock c, boolean farms ){
		if (s == Slot.INDOORS) return "indoors";
		if (s == Slot.BREAK) return "break";
		if (r == Role.FARMER && s == Slot.WORK && c.winter) return "farmer_winter";
		if (r == Role.FARMER && s == Slot.WORK && !farms) return "farmer_garden";
		if (r == Role.LOAFER && c.phase == DayNightCycle.Phase.DAY) return "line";
		return r.name().toLowerCase( Locale.ENGLISH ) + "_" + c.phase.name().toLowerCase( Locale.ENGLISH );
	}

	/** How many lines a stem has: the villager's six idle lines, three for everything else. */
	public static int chatLines( String stem ){
		return "line".equals( stem ) ? 6 : 3;
	}

	// ------------------------------------------------------------ the window

	//what is cached per window: a settlement's banks and gates. keyed by the level's identity
	//(a number: no stale level is kept alive) and its window version
	private static int cacheLevel = 0, cacheVersion = -1;
	private static final HashMap<Long, long[]> shoreCache = new HashMap<>(), gateCache = new HashMap<>();

	private static void checkCache( OverworldLevel ow ){
		int id = System.identityHashCode( ow );
		if (id != cacheLevel || ow.windowVersion() != cacheVersion){
			cacheLevel = id;
			cacheVersion = ow.windowVersion();
			shoreCache.clear();
			gateCache.clear();
		}
	}

	/** The window cell of a world cell a settler may stand on: inside the window and clear of
	 *  its two outer rings, where addMob puts nobody; -1 otherwise. */
	public static int spotCell( OverworldLevel ow, int wx, int wy ){
		int x = wx - ow.worldX(), y = wy - ow.worldY();
		if (x <= 1 || y <= 1 || x >= ow.width() - 2 || y >= ow.height() - 2) return -1;
		return ow.localCell( wx, wy );
	}

	//what a step tramples: the grass a walker flattens (and rolls loot from) and the wheat
	static boolean trample( int t ){
		return t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS || t == Terrain.SOIL_CORNWHEAT
				|| t == Terrain.SOIL_GREENWHEAT || t == Terrain.SOIL_STRAWWHEAT || t == Terrain.SOIL_WATERWHEAT;
	}

	/** Ground a settler walks over: dry and safe, nothing it would trample, nothing growing or
	 *  lying on it (a villager never wades, never treads a crop flat, never kicks a heap). */
	public static boolean walkable( Level l, int c ){
		return l.passable[c] && !l.avoid[c] && !l.water[c] && !l.pit[c]
				&& !trample( l.map[c] ) && l.plants.get( c ) == null && l.heaps.get( c ) == null;
	}

	/** ...and ground it stands on: never a doorway, a plank crossing or a way between slices. */
	public static boolean standable( Level l, int c ){
		int t = l.map[c];
		return t != Terrain.DOOR && t != Terrain.OPEN_DOOR && t != Terrain.BRIDGE
				&& t != Terrain.ENTRANCE && t != Terrain.EXIT && walkable( l, c );
	}

	//the first choice of a standing spot: off the roads and footpaths (the road through a
	//village is the travellers'), and never where something is drawn over the cell - the
	//chimney stack SettlementLights stands above a house's back wall, an oak's crown
	static boolean openSpot( Level l, int c ){
		int below = l.map[c + l.width()];
		return l.map[c] != Terrain.DIRT_PATH && below != Terrain.WALL && below != Terrain.TREE_OAK;
	}

	//nobody else stands there: in a turn, by the scheduler; while the window is being peopled
	//(a spawn, an unpark, a load), by the level's own list as well - the scheduler does not
	//hold its mobs yet there (OverworldLevel.occupied)
	static boolean free( OverworldLevel ow, int c, Char self, boolean arrival ){
		if (arrival) return !ow.occupied( c );
		Char o = Actor.findChar( c );
		return o == null || o == self;
	}

	static boolean waterBeside( Level l, int c ){
		int w = l.width();
		return l.water[c - 1] || l.water[c + 1] || l.water[c - w] || l.water[c + w];
	}

	/** The first neighbour of a cell (rows top to bottom) whose terrain is one of these, or -1. */
	public static int besideCell( Level l, int c, int[] terrains ){
		int w = l.width();
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (dx == 0 && dy == 0) continue;
				int n = c + dx + dy * w;
				int t = l.map[n];
				for (int k : terrains){
					if (t == k) return n;
				}
			}
		}
		return -1;
	}

	/** The first neighbour of a cell (rows top to bottom) that is a village's field, standing
	 *  (OverworldLevel.fieldAt), or -1. */
	public static int besideField( Level l, int c ){
		if (!(l instanceof OverworldLevel)) return -1;
		OverworldLevel ow = (OverworldLevel) l;
		int w = l.width();
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (dx == 0 && dy == 0) continue;
				int n = c + dx + dy * w;
				if (ow.fieldAt( n )) return n;
			}
		}
		return -1;
	}

	/** The cell a member at work turns to: the water a fisher watches, the crop a farmer
	 *  stoops over, the brush a gatherer picks through. -1 when there is none beside it (the
	 *  water froze, the field burned or was trodden flat): the work goes on unseen. */
	public static int flourishCell( Level l, int pos, Kind k ){
		switch (k){
			case SHORE: {
				int w = l.width();
				if (l.water[pos - w]) return pos - w;
				if (l.water[pos - 1]) return pos - 1;
				if (l.water[pos + 1]) return pos + 1;
				if (l.water[pos + w]) return pos + w;
				return -1;
			}
			case FIELD: return besideField( l, pos );
			case BRUSH: return besideCell( l, pos, BRUSHY );
			default:    return -1;
		}
	}

	//is this cell the kind of place the spot is? strict: with something to work beside it
	static boolean suits( Level l, int c, Kind k, boolean strict ){
		switch (k){
			case SHORE: return waterBeside( l, c );
			case FIELD: return l.map[c] != Terrain.DIRT_PATH && (!strict || besideField( l, c ) != -1);
			case BRUSH: return l.map[c] != Terrain.DIRT_PATH && (!strict || besideCell( l, c, BRUSHY ) != -1);
			default:    return true;
		}
	}

	/** Where a member at its spot may wander to: inside the spot's band, on the bank, beside
	 *  the crop or the brush it works. */
	public static boolean keeps( OverworldLevel ow, Settlement s, Target t, int c ){
		if (t.ringMin >= 0){
			int ring = Math.max( Math.abs( ow.worldX() + c % ow.width() - s.cx ), Math.abs( ow.worldY() + c / ow.width() - s.cy ) );
			if (ring < t.ringMin || ring > t.ringMax) return false;
		}
		return t.kind == Kind.HOME || t.kind == Kind.STREET || t.kind == Kind.WELL || t.kind == Kind.RING
				|| suits( ow, c, t.kind, true );
	}

	/** May a settler stand on this cell of the window as its first choice (openSpot), and at all? */
	public static boolean restsOn( OverworldLevel ow, int c ){
		return standable( ow, c ) && openSpot( ow, c );
	}

	private static boolean[] mask;
	//the walk mask cut into the areas a settler can walk within, numbered from 1 (0: off it)
	private static int[] areas, areaQueue;
	private static int maskLevel, maskVersion = -1;

	/** The ground a settler's path may cross in the current window: built once per window,
	 *  with its areas (areas). Shared - a caller that blocks a cell for one search puts it
	 *  back. Plants, heaps and whatever changed since are not in it: the walk checks each step
	 *  (walkable). */
	public static boolean[] walkMask( OverworldLevel ow ){
		int id = System.identityHashCode( ow );
		if (mask == null || mask.length != ow.length() || id != maskLevel || ow.windowVersion() != maskVersion){
			long t0 = LagMonitor.begin();
			if (mask == null || mask.length != ow.length()){
				mask = new boolean[ow.length()];
				areas = new int[ow.length()];
				areaQueue = new int[ow.length()];
			}
			for (int c = 0; c < mask.length; c++){
				mask[c] = ow.passable[c] && !ow.avoid[c] && !ow.water[c] && !ow.pit[c] && !trample( ow.map[c] );
			}
			labelAreas( ow.width() );
			maskLevel = id;
			maskVersion = ow.windowVersion();
			LagMonitor.end( "VillageRoutine walk mask", t0 );
		}
		return mask;
	}

	//flood fill of the mask, neighbours as PathFinder steps (eight ways, none across the
	//window's sides)
	private static void labelAreas( int w ){
		java.util.Arrays.fill( areas, 0 );
		int n = mask.length, area = 0;
		for (int start = 0; start < n; start++){
			if (!mask[start] || areas[start] != 0) continue;
			area++;
			int head = 0, tail = 0;
			areaQueue[tail++] = start;
			areas[start] = area;
			while (head < tail){
				int c = areaQueue[head++];
				int x = c % w;
				for (int dy = -w; dy <= w; dy += w){
					for (int dx = -1; dx <= 1; dx++){
						int m = c + dy + dx;
						if (m < 0 || m >= n || x + dx < 0 || x + dx >= w || !mask[m] || areas[m] != 0) continue;
						areas[m] = area;
						areaQueue[tail++] = m;
					}
				}
			}
		}
	}

	/** The areas of the window's walking ground (walkMask): two cells of the same area have a
	 *  way between them, two of different areas none. */
	public static int[] areas( OverworldLevel ow ){
		walkMask( ow );
		return areas;
	}

	/** The area of a house's doorway, the one its family walks out into (0: the door is off
	 *  the window, or nothing can stand there). */
	public static int homeArea( OverworldLevel ow, Settlement s, int homeX, int homeY ){
		int door = ow.localCell( doorX( s, homeX, homeY ), doorY( s, homeX, homeY ) );
		return door == -1 ? 0 : areas( ow )[door];
	}

	/** Is there a way over the window's ground from one cell to another - is a path search
	 *  worth running at all? Exactly when PathFinder.find over walkMask finds one: it takes
	 *  the two ends as they are (a settler may stand on what since grew or was dropped there)
	 *  and steps on the mask between them. A lookup, never a search. */
	public static boolean reachable( OverworldLevel ow, int a, int b ){
		if (a == b || ow.adjacent( a, b )) return true;
		int[] l = areas( ow );
		if (l[a] != 0 && l[b] != 0) return l[a] == l[b];
		int w = ow.width(), n = l.length;
		for (int i = 0; i < 9; i++){
			int na = l[a] != 0 ? (i == 0 ? a : -1) : around( a, i, w, n );
			if (na == -1 || l[na] == 0) continue;
			for (int j = 0; j < 9; j++){
				int nb = l[b] != 0 ? (j == 0 ? b : -1) : around( b, j, w, n );
				if (nb != -1 && l[nb] == l[na]) return true;
			}
		}
		return false;
	}

	//the i-th cell of the 3x3 block round c (i in [0, 9)), or -1 off the window
	private static int around( int c, int i, int w, int n ){
		int dx = i % 3 - 1, x = c % w + dx;
		int m = c + dx + (i / 3 - 1) * w;
		return x < 0 || x >= w || m < 0 || m >= n ? -1 : m;
	}

	/** Could a player see this cell? The host's hero by his field of view; another player's,
	 *  whose view the host does not hold, by being within NEAR of it. */
	public static boolean inSight( OverworldLevel ow, int cell ){
		return (Dungeon.level == ow && ow.heroFOV[cell]) || OverworldLevel.remoteHeroDistance( ow, cell ) <= NEAR;
	}

	/**
	 * A free window cell for a spot, or -1: spiralling out from it as far as the spot allows,
	 * inside its band, of its kind - first off the roads and with work beside it, then
	 * anywhere it may stand at all. Only ever in `area`, the member's home area, when that is
	 * known (not 0): a spot it can walk to and home from - a stretch of fence or brush cut off
	 * from the place (by thicket, reeds, water) is no spot of its. `arrival` while the window
	 * is being peopled (see free).
	 */
	public static int resolve( OverworldLevel ow, Settlement s, Target t, Char self, boolean arrival, int area ){
		if (t.kind == Kind.HOME) return resolveHome( ow, s, t, self, arrival );
		int[] l = area != 0 ? areas( ow ) : null;
		for (int pass = 0; pass < 2; pass++){
			for (int r = 0; r <= t.search; r++){
				for (int dy = -r; dy <= r; dy++){
					for (int dx = -r; dx <= r; dx++){
						if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
						int wx = t.wx + dx, wy = t.wy + dy;
						if (t.ringMin >= 0){
							int ring = Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) );
							if (ring < t.ringMin || ring > t.ringMax) continue;
						}
						int c = spotCell( ow, wx, wy );
						if (c == -1 || (l != null && l[c] != area) || !standable( ow, c ) || (pass == 0 && !openSpot( ow, c ))
								|| !suits( ow, c, t.kind, pass == 0 ) || !free( ow, c, self, arrival )) continue;
						return c;
					}
				}
			}
		}
		return -1;
	}

	//a cell of the house's 3x3 inside, the nearest to the spot asked for that is free - and
	//while there is any other, never the one just inside the door, kept clear for coming and going
	private static int resolveHome( OverworldLevel ow, Settlement s, Target t, Char self, boolean arrival ){
		int ix = inX( s, t.homeX, t.homeY ), iy = inY( s, t.homeX, t.homeY );
		for (int pass = 0; pass < 2; pass++){
			for (int r = 0; r <= 2; r++){
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int wx = t.homeX + dx, wy = t.homeY + dy;
						if (Math.max( Math.abs( wx - t.wx ), Math.abs( wy - t.wy ) ) != r) continue;
						if (pass == 0 && dx == -ix && dy == -iy) continue;
						int c = spotCell( ow, wx, wy );
						if (c == -1 || !standable( ow, c ) || !free( ow, c, self, arrival )) continue;
						return c;
					}
				}
			}
		}
		return -1;
	}

	/** The banks a settlement's fishers work: dry cells beside open water within its berth and
	 *  a few cells more, as world keys, nearest the well first (ties by key). Scanned once per
	 *  window: a frozen river has none, and a bank outside the window counts only once the
	 *  window holds it. */
	public static long[] shores( OverworldLevel ow, Settlement s ){
		checkCache( ow );
		long[] hit = shoreCache.get( s.key );
		if (hit != null) return hit;
		long t0 = LagMonitor.begin();
		int reach = s.radius + 6;   //the berth (radius + 3, OverworldFauna.nearSettlement) and a few cells more
		ArrayList<long[]> found = new ArrayList<>();
		for (int wy = s.cy - reach; wy <= s.cy + reach; wy++){
			for (int wx = s.cx - reach; wx <= s.cx + reach; wx++){
				int c = spotCell( ow, wx, wy );
				if (c == -1 || !standable( ow, c ) || !waterBeside( ow, c )) continue;
				found.add( new long[]{ Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ), OverworldLevel.worldKey( wx, wy ) } );
			}
		}
		Collections.sort( found, (a, b) -> a[0] != b[0] ? Long.compare( a[0], b[0] ) : Long.compare( a[1], b[1] ) );
		long[] out = new long[found.size()];
		for (int i = 0; i < out.length; i++) out[i] = found.get( i )[1];
		shoreCache.put( s.key, out );
		LagMonitor.end( "VillageRoutine shores", t0 );
		return out;
	}

	/** ...those of them a member can walk to from home: in its home area (homeArea), the
	 *  same order. A bank cut off from the village (over the water, past the reeds) is no
	 *  fisher's; area 0 (home unknown) keeps them all. */
	public static long[] shores( OverworldLevel ow, Settlement s, int area ){
		long[] all = shores( ow, s );
		if (area == 0) return all;
		int[] l = areas( ow );
		int n = 0;
		for (long k : all) if (l[ow.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) )] == area) n++;
		if (n == all.length) return all;
		long[] out = new long[n];
		n = 0;
		for (long k : all) if (l[ow.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) )] == area) out[n++] = k;
		return out;
	}

	//where a road runs through a settlement's fence: one world key per gap (the middle of each
	//run of road cells on the fence ring), in order round the ring. once per window
	static long[] gates( OverworldLevel ow, Settlement s ){
		checkCache( ow );
		long[] hit = gateCache.get( s.key );
		if (hit != null) return hit;
		int G = s.radius + 2, n = 8 * G;
		boolean[] gate = new boolean[n];
		int start = -1;
		for (int k = 0; k < n; k++){
			int c = ow.localCell( s.cx + ringX( G, k ), s.cy + ringY( G, k ) );
			gate[k] = c != -1 && (ow.map[c] == Terrain.DIRT_PATH || ow.map[c] == Terrain.BRIDGE);
			if (!gate[k] && start == -1) start = k;
		}
		ArrayList<Long> runs = new ArrayList<>();
		if (start != -1){
			for (int i = 1; i <= n; i++){
				int k = (start + i) % n;
				if (!gate[k] || gate[(k - 1 + n) % n]) continue;   //only the first cell of a run
				int len = 0;
				while (gate[(k + len) % n]) len++;
				int rep = (k + len / 2) % n;
				runs.add( OverworldLevel.worldKey( s.cx + ringX( G, rep ), s.cy + ringY( G, rep ) ) );
			}
		}
		long[] out = new long[runs.size()];
		for (int i = 0; i < out.length; i++) out[i] = runs.get( i );
		gateCache.put( s.key, out );
		return out;
	}

	/** A guard's place by the well by day: his post of the four, or a free cell beside it. */
	public static int dayPost( OverworldLevel ow, Settlement s, int post, Char self, boolean arrival ){
		int[] o = GUARD_POSTS[Math.floorMod( post, GUARD_POSTS.length )];
		for (int r = 0; r <= 1; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = spotCell( ow, s.cx + o[0] + dx, s.cy + o[1] + dy );
					if (c != -1 && standable( ow, c ) && free( ow, c, self, arrival )) return c;
				}
			}
		}
		return -1;
	}

	/** ...and by night: at a gate, a step inside the fence beside the road (the second guard
	 *  of a gate on its other side); a settlement with no road in has its guards stay by the well. */
	public static int gatePost( OverworldLevel ow, Settlement s, int post, Char self, boolean arrival ){
		long[] g = gates( ow, s );
		if (g.length > 0){
			long k = g[Math.floorMod( post, g.length )];
			int gx = (int)(k & 0xFFFFFFFFL), gy = (int)(k >> 32);
			//one step inside the fence, toward the well, along the larger axis
			int ax = Math.abs( gx - s.cx ) >= Math.abs( gy - s.cy ) ? -Integer.signum( gx - s.cx ) : 0;
			int ay = ax == 0 ? -Integer.signum( gy - s.cy ) : 0;
			for (int pass = 0; pass < 2; pass++){
				for (int r = 0; r <= 2; r++){
					for (int dy = -r; dy <= r; dy++){
						for (int dx = -r; dx <= r; dx++){
							if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
							int wx = gx + ax + dx, wy = gy + ay + dy;
							if (Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ) > s.radius + 1) continue;
							int c = spotCell( ow, wx, wy );
							if (c == -1 || !standable( ow, c ) || (pass == 0 && ow.map[c] == Terrain.DIRT_PATH)
									|| !free( ow, c, self, arrival )) continue;
							return c;
						}
					}
				}
			}
		}
		return dayPost( ow, s, post, self, arrival );
	}

	/** Where one of a settlement's people (a Settler or an OverworldGuard) stands on coming
	 *  into the window - spawned, unparked, loaded - having stood at world (wx, wy): straight
	 *  at its part of the day, before any sprite or turn. -1 when that spot cannot be had,
	 *  and the caller sets it down where it always did. */
	public static int arrivalCell( OverworldLevel ow, Mob m, int wx, int wy ){
		if (m instanceof Settler) return ((Settler) m).arrive( ow, wx, wy );
		if (m instanceof OverworldGuard) return ((OverworldGuard) m).arrive( ow, wx, wy );
		return -1;
	}

	/** Debug scenes: open water, by the wild terrain alone, in the band round a settlement its
	 *  fishers would work - a pond or a river, not a puddle (four of every second cell of
	 *  rings radius-4 to radius+6, some sixteen cells; ice is no water). */
	public static boolean shoreNear( long seed, int sx, int sy ){
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		int R = WorldStructures.settlementLayout( seed, sx, sy )[0];
		int water = 0;
		for (int dy = -(R + 6); dy <= R + 6; dy += 2){
			for (int dx = -(R + 6); dx <= R + 6; dx += 2){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) < R - 4) continue;
				int t = WorldModel.wildTerrainAt( seed, cx + dx, cy + dy );
				if ((t == Terrain.WATER || t == Terrain.DEEP_WATER) && ++water >= 4) return true;
			}
		}
		return false;
	}
}
