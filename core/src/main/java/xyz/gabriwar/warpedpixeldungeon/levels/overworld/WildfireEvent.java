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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A fire in the woods or the meadows (WorldEvents.Type.WILDFIRE), in a hot dry spell.
 *
 * HOW IT STARTS. Fire weather is a dry spell - no rain for DRY_TURNS (half a day: the climate's
 * own memory, ClimateManager.lastRainTurn, wherever the hero was) - with the air at FIRE_C or more
 * where the fire would start: summer, as a rule. Then a careless camp
 * can set the land alight: every sector of the wilds has a fixed spot for it (a hashed cell of
 * woodland or meadow well inside the sector, clear of the town, every site's berth and the caves'
 * mouths), and one day in ODDS (60) it is that sector's turn, at a hashed hour of the day. Within
 * HEAR (2) sectors of a hero of the party the host looks at the weather when that hour comes and
 * for WINDOW turns after: fire weather, and it catches; not, and that day's chance is gone. A storm
 * also leaves fires behind: one tree in HOLD_ODDS a bolt strikes (StormStrikes) in warm weather
 * (HOLD_C) is left smouldering, and once HOLD_DRY turns without rain have gone by in fire weather
 * within HOLD_TURNS it flares up into a wildfire of its own. Either way the fire is written into the log
 * (WorldEventLog.forced), so a save, a far trip and a co-op guest all find the same one.
 *
 * HOW IT SPREADS. Cell by cell, out from where it caught, over the land's own fuel (the annual
 * mean's ground: grass, flowers, tall grass, brush, mushrooms and trees), inside a box RADIUS
 * (12-18) cells each way. Each step takes STEP turns times the fuel's own pace (tall grass quickest,
 * trees slowest), a per-cell jitter and the fire's own wind - quick downwind, slow against it - so a
 * fire burns out a ragged, wind-stretched scar. It keeps catching for SPREAD_TURNS (a day and a half
 * to two days) and then dies down. Each cell burns FLAME_* turns (a tree longest). Nothing else
 * carries it: roads, water, bare ground, rock and snow are firebreaks, and so is every village's
 * berth, the town and every site. Where it stands is a pure function of the fire, the cells kept
 * from it and the rain that put it out - worked out again (Blaze) whenever one of those changes, and
 * only ever forward: a cell kept from it from a turn on (doused, built on, dug) changes nothing
 * that came before that turn.
 *
 * WHAT STOPS IT. Rain (ClimateManager: the first rain after it caught, ClimateManager.
 * firstRainAfter, or the rain falling on the host's hero now) puts the whole fire out. Water poured
 * or splashed on a burning cell (PotionOfWater, a frost) puts that cell out and keeps it and its
 * four sides from the fire (douse). Whatever the hero made of the land - a built piece, a floor, a
 * field, a dug channel, a puddle - is no fuel: every such edit in its box is written down as a
 * firebreak from the turn the fire caught (or from the turn it was made, while the fire burns).
 *
 * WHAT IT LEAVES. A tree burnt down is a stump (STUMP_*), a sapling from SAPLING_TURNS after its
 * flames died and the tree again at TREE_TURNS; ground burnt lies black (EMBERS) for ASH_TURNS. All
 * of it is the world's overlay (OverworldLevel's scorch: never the player's edit, taken back by the
 * world), so the burnt land is bounded by the fire's box and gone with it. A wooden built piece
 * beside a cell as it catches takes SCORCH damage, down to one health and never past it, a crop
 * beside one withers: a homestead is never burnt whole, but it is warned once (WARN cells) and can
 * be saved with water.
 *
 * AROUND IT. Wild beasts within FLEE cells of the flames run from them, and none is set down near
 * it; the critters scatter. A hero resting within ROUSE cells of them is roused (a fire's burn on
 * the surface is fierce), and told once. Its smoke rises over the fog in thick black columns (effects/
 * NatureDecor), seen from afar; the flames are the blob Wildfire's (a fire's look, burning whoever
 * stands in them). Every player hears of it the way it lies when it catches, the world map pins
 * it while it burns, and its end - put out or burnt out - is said.
 */
public final class WildfireEvent {

	private WildfireEvent(){}

	private static final int FC = WorldEvents.FC;

	/** One day in ODDS a sector's spot is the one a camp sets alight. */
	public static final int ODDS = 60;
	/** Sectors: a spot this near a hero's sector can catch; the hero hears of a fire this near. */
	public static final int HEAR = 2;
	/** A spot is looked for in this many hashed cells of a sector's middle. */
	static final int SPOT_TRIES = 6;
	/** Turns after its hour that a spot still catches when the weather turns. */
	public static final int WINDOW = FC / 4;

	/** Fire weather: the air this warm at the spot, and no rain for this long (summer's rain comes
	 *  often: half a day without it is a dry spell one summer day in seven, NatureEventsTest). */
	public static final float FIRE_C = 25f;
	public static final int DRY_TURNS = FC / 2;

	/** A fire's box: RADIUS (MIN_RADIUS + up to RADIUS_SPAN - 1) cells each way of where it caught. */
	public static final int MIN_RADIUS = 12, RADIUS_SPAN = 7;
	/** Turns a step of the flames takes over ground of pace 1. */
	public static final int STEP = 150;
	/** How long it keeps catching: SPREAD_TURNS and up to SPREAD_SPAN more. */
	public static final int SPREAD_TURNS = 3 * FC / 2, SPREAD_SPAN = FC / 2;
	/** How long a cell burns: grass, brush, a tree. */
	public static final int FLAME_GROUND = 160, FLAME_BRUSH = 220, FLAME_TREE = 320;

	/** The scar: burnt ground black this long; a burnt tree a stump, then a sapling from
	 *  SAPLING_TURNS, the tree again at TREE_TURNS (all from its flames' end). */
	public static final int ASH_TURNS = 4 * FC, SAPLING_TURNS = 6 * FC, TREE_TURNS = 18 * FC;
	public static final int SCAR_TURNS = TREE_TURNS;

	/** One tree in HOLD_ODDS a bolt strikes in air this warm (HOLD_C) is left smouldering; it flares
	 *  within HOLD_TURNS once HOLD_DRY turns have gone by dry in fire weather. At most HOLDOVERS
	 *  smoulder at once. */
	public static final float HOLD_C = 15f;
	public static final int HOLD_ODDS = 3, HOLD_TURNS = 3 * FC, HOLD_DRY = FC / 2, HOLDOVERS = 4;

	/** Cells: a homestead this near the fire's box is warned; wildlife this near the flames runs; a
	 *  hero resting this near them is roused. */
	public static final int WARN = 10, FLEE = 8, ROUSE = 2;
	/** What a wooden built piece takes as a cell beside it catches (never its last health). */
	public static final int SCORCH = 10;
	/** One tree in CHARCOAL burnt down in the window leaves charcoal in its ashes. */
	public static final int CHARCOAL = 3;

	/** No turn: a cell that never catches. */
	static final int NEVER = Integer.MAX_VALUE;

	//a cell and its four sides: what water poured on one puts out
	private static final int[][] CROSS = { { 0, 0 }, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

	//fuel kinds
	static final byte NONE = 0, GROUND = 1, TALL = 2, BRUSH = 3, OAK = 4, PINE = 5;

	private static final long SALT_SPOT = 0x5F1D5B07L, SALT_DAY = 0x5F1DDA7EL, SALT_FIRE = 0x5F1DF12EL, SALT_HOLD = 0x5F1D401DL;

	// ------------------------------------------------------------ fuel (pure)

	/** The fuel a terrain is, NONE for a firebreak. */
	static byte fuel( int t ){
		switch (t){
			case Terrain.GRASS: case Terrain.FLOWER_PATCH: return GROUND;
			case Terrain.HIGH_GRASS: return TALL;
			case Terrain.SHRUB: case Terrain.MUSHROOM_PATCH: return BRUSH;
			case Terrain.TREE_OAK: return OAK;
			case Terrain.TREE_PINE: return PINE;
			default: return NONE;
		}
	}

	/** Does this terrain carry the flames? */
	public static boolean burns( int t ){
		return fuel( t ) != NONE;
	}

	//a step's pace onto fuel of this kind (tall grass goes up quickest, trees slowest)
	private static float pace( byte k ){
		switch (k){
			case TALL: return 0.8f;
			case BRUSH: return 1.2f;
			case OAK: case PINE: return 1.35f;
			default: return 1f;
		}
	}

	/** How long fuel of this kind burns, in turns. */
	static int flame( byte k ){
		switch (k){
			case BRUSH: return FLAME_BRUSH;
			case OAK: case PINE: return FLAME_TREE;
			default: return FLAME_GROUND;
		}
	}

	/** May a fire burn on this world cell at all: out of the town's reach, every site's berth and the
	 *  caves' mouths. */
	static boolean inLand( long seed, int wx, int wy ){
		if (Math.max( Math.abs( wx ), Math.abs( wy ) ) <= WorldEvents.TOWN_CLEAR) return false;
		return !WorldEvents.nearSite( seed, wx, wy ) && !WindowGenerator.mouthAt( seed, wx, wy );
	}

	// ------------------------------------------------------------ a fire's numbers (pure, by its id)

	/** Its box's reach each way. */
	public static int radiusOf( long id ){
		return MIN_RADIUS + (int) Math.floorMod( WorldEvents.mix( id, SALT_FIRE, 2 ), (long) RADIUS_SPAN );
	}

	/** How long it keeps catching. */
	public static int spreadOf( long id ){
		return SPREAD_TURNS + (int) Math.floorMod( WorldEvents.mix( id, SALT_FIRE, 1 ), (long) SPREAD_SPAN );
	}

	//its wind: the way it runs quickest, an angle
	private static double windOf( long id ){
		return (WorldEvents.mix( id, SALT_FIRE, 3 ) >>> 11) * 0x1.0p-53 * 2 * Math.PI;
	}

	/** A wildfire's event: catching at `start` on world cell (wx, wy), its sector's, with this id. */
	public static WorldEvents.Event fire( int wx, int wy, int start, long id ){
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		return new WorldEvents.Event( WorldEvents.Type.WILDFIRE, sx, sy, WorldEvents.day( start ), start,
				start + spreadOf( id ) + FLAME_TREE, wx, wy, id );
	}

	// ------------------------------------------------------------ the schedule (pure)

	//each sector's spot, or none: the sites are pure, so the answer is kept per seed
	private static volatile long spotSeed = Long.MIN_VALUE;
	private static final ConcurrentHashMap<Long, int[]> spotCache = new ConcurrentHashMap<>();
	private static final int[] NO_SPOT = new int[0];

	/** Sector (sx, sy)'s spot a camp's fire starts at: the first of SPOT_TRIES hashed cells of its
	 *  middle on woodland or meadow fuel in the land a fire may burn; null when there is none. */
	public static int[] spot( long seed, int sx, int sy ){
		if (seed != spotSeed){
			synchronized (spotCache){
				if (seed != spotSeed){
					spotCache.clear();
					spotSeed = seed;
				}
			}
		}
		long key = WorldStructures.sectorOf( sx, sy );
		int[] known = spotCache.get( key );
		if (known == null){
			known = NO_SPOT;
			for (int k = 0; k < SPOT_TRIES; k++){
				long h = WorldEvents.mix( seed ^ SALT_SPOT, key, k );
				int wx = sx * WorldStructures.SECTOR + 12 + (int) Math.floorMod( h, (long)(WorldStructures.SECTOR - 24) );
				int wy = sy * WorldStructures.SECTOR + 12 + (int) Math.floorMod( h >>> 24, (long)(WorldStructures.SECTOR - 24) );
				WorldModel.Biome b = WorldModel.baseBiomeAt( seed, wx, wy );
				if (b != WorldModel.Biome.FOREST && b != WorldModel.Biome.MEADOW) continue;
				if (!burns( WorldEvents.baseTerrain( seed, wx, wy ) ) || !inLand( seed, wx, wy )) continue;
				known = new int[]{ wx, wy };
				break;
			}
			spotCache.put( key, known );
		}
		return known.length == 0 ? null : known;
	}

	/** The fire a camp may start in sector (sx, sy) on day d, or null: one day in ODDS, at a hashed
	 *  turn of it, on the sector's spot. Whether it catches is the weather's (fireWeather). */
	public static WorldEvents.Event candidate( long seed, int sx, int sy, int d ){
		long h = WorldEvents.mix( seed ^ SALT_DAY, WorldStructures.sectorOf( sx, sy ), d );
		if (Math.floorMod( h, (long) ODDS ) != 0) return null;
		int[] at = spot( seed, sx, sy );
		if (at == null) return null;
		int start = d * FC + (int) Math.floorMod( h >>> 20, (long) FC );
		return fire( at[0], at[1], start, WorldEvents.idOf( WorldEvents.Type.WILDFIRE, sx, sy, d ) );
	}

	/** Fire weather at a world cell now: no rain falling, none for DRY_TURNS, the air there at FIRE_C. */
	public static boolean fireWeather( long seed, int wx, int wy, int turn ){
		return dryFor( turn ) >= DRY_TURNS && ClimateManager.surfaceTempIn( WorldModel.baseBiomeAt( seed, wx, wy ) ) >= FIRE_C;
	}

	//turns since the rain last fell (as good as forever when it never has), none while it falls
	static int dryFor( int turn ){
		if (ClimateManager.wetNow() || ClimateManager.isRaining()) return 0;
		int last = ClimateManager.lastRainTurn();
		return last == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.max( 0, turn - last );
	}

	// ------------------------------------------------------------ how it burns (pure, given the log)

	/**
	 * A fire as it burns, worked out over its box: where each cell catches and when its flames die,
	 * from its event, the cells kept from it and the rain that put it out.
	 */
	public static final class Blaze {
		public final long id;
		/** Its box: the world cell of its first corner and its width (2 * radius + 1). */
		public final int x0, y0, side;
		final byte[] kind;
		/** Turn each box cell catches (NEVER where it never does), and its flames die. */
		final int[] caught, out;
		/** The box cells that catch, by the turn they do. */
		final int[] order;
		/** The turns anything it lays on the land changes, sorted (the scar's stages). */
		final int[] changes;
		//what it was worked out for
		final int logId, barsVersion, quench;

		Blaze( long id, int x0, int y0, int side, byte[] kind, int[] caught, int[] out, int[] order, int[] changes,
				int logId, int barsVersion, int quench ){
			this.id = id;
			this.x0 = x0;
			this.y0 = y0;
			this.side = side;
			this.kind = kind;
			this.caught = caught;
			this.out = out;
			this.order = order;
			this.changes = changes;
			this.logId = logId;
			this.barsVersion = barsVersion;
			this.quench = quench;
		}

		/** Is a box cell (its index) alight at this turn? */
		public boolean burningAt( int i, int turn ){
			return caught[i] <= turn && turn < out[i];
		}

		/** How many box cells catch in all. */
		public int extent(){
			return order.length;
		}

		/** The world cell of a box index. */
		public int wx( int i ){ return x0 + i % side; }
		public int wy( int i ){ return y0 + i / side; }

		/** The box index of a world cell, -1 outside it. */
		public int index( int wx, int wy ){
			int x = wx - x0, y = wy - y0;
			return x < 0 || y < 0 || x >= side || y >= side ? -1 : x + y * side;
		}

		/** How many of its scar's changes have come by this turn: a new one is something new to lay. */
		public int stage( int turn ){
			int i = Arrays.binarySearch( changes, turn );
			if (i < 0) return -i - 1;
			while (i + 1 < changes.length && changes[i + 1] == turn) i++;
			return i + 1;
		}

		/** What the fire has left on a box cell by this turn, -1 nothing (unburnt, alight, grown back). */
		public int scar( int i, int turn ){
			if (caught[i] == NEVER || turn < out[i]) return -1;
			int age = turn - out[i];
			byte k = kind[i];
			if (k == OAK || k == PINE){
				if (age < SAPLING_TURNS) return k == OAK ? Terrain.STUMP_OAK : Terrain.STUMP_PINE;
				if (age < TREE_TURNS) return k == OAK ? Terrain.SAPLING_OAK : Terrain.SAPLING_PINE;
				return -1;
			}
			return age < ASH_TURNS ? Terrain.EMBERS : -1;
		}

		/** How fiercely a cell burns at this turn, the blob's volume there (0 not alight): a tree's
		 *  crown tall, then lower as it burns down; grass and brush lower all through. */
		public int heat( int i, int turn ){
			if (!burningAt( i, turn )) return 0;
			float f = (turn - caught[i]) / (float)Math.max( 1, out[i] - caught[i] );
			byte k = kind[i];
			if (k == OAK || k == PINE) return f < 0.34f ? 8 : f < 0.7f ? 6 : 2;
			if (k == BRUSH) return f < 0.5f ? 6 : 3;
			return f < 0.5f ? 5 : 2;
		}
	}

	//the box's land: fuel and the base terrain, by the fire alone (pure), kept while the fire is
	private static final class Land {
		final byte[] kind;
		Land( byte[] kind ){
			this.kind = kind;
		}
	}

	private static final int CACHE = 24;
	private static long cacheSeed = Long.MIN_VALUE;
	private static final LinkedHashMap<Long, Land> lands = new LinkedHashMap<>( 16, 0.75f, true );
	private static final LinkedHashMap<Long, Blaze> blazes = new LinkedHashMap<>( 16, 0.75f, true );

	private static Land land( long seed, WorldEvents.Event e ){
		if (seed != cacheSeed){
			lands.clear();
			blazes.clear();
			cacheSeed = seed;
		}
		Land l = lands.get( e.id );
		if (l != null) return l;
		int r = radiusOf( e.id ), side = 2 * r + 1;
		byte[] kind = new byte[side * side];
		for (int y = 0; y < side; y++){
			for (int x = 0; x < side; x++){
				int wx = e.wx - r + x, wy = e.wy - r + y;
				byte k = fuel( WorldEvents.baseTerrain( seed, wx, wy ) );
				if (k != NONE && !inLand( seed, wx, wy )) k = NONE;
				kind[x + y * side] = k;
			}
		}
		l = new Land( kind );
		lands.put( e.id, l );
		while (lands.size() > CACHE) lands.remove( lands.keySet().iterator().next() );
		return l;
	}

	/** The turn rain put a fire out, NEVER while none has. */
	static int quenchOf( WorldEventLog log, long id ){
		Integer q = log.quench.get( id );
		return q == null ? NEVER : q;
	}

	/** A fire as it burns now (worked out again only when what keeps cells from it, or the rain,
	 *  changed). Actor thread (and the tests). */
	public static synchronized Blaze blaze( long seed, WorldEvents.Event e, WorldEventLog log ){
		int logId = System.identityHashCode( log ), quench = quenchOf( log, e.id );
		if (seed == cacheSeed){
			Blaze b = blazes.get( e.id );
			if (b != null && b.logId == logId && b.barsVersion == log.barsVersion() && b.quench == quench) return b;
		}
		Land l = land( seed, e );
		Blaze b = compute( e, l.kind, log, logId, quench );
		blazes.put( e.id, b );
		while (blazes.size() > CACHE) blazes.remove( blazes.keySet().iterator().next() );
		return b;
	}

	//the time-ordered spread over the box: each cell catches at the earliest turn the flames reach
	//it while they still spread, unless it was kept from them by then; a cell kept from them (or
	//put out) spreads no further from that turn on
	private static Blaze compute( WorldEvents.Event e, byte[] kind, WorldEventLog log, int logId, int quench ){
		int r = radiusOf( e.id ), side = 2 * r + 1, n = side * side;
		int x0 = e.wx - r, y0 = e.wy - r;
		int stop = Math.min( quench, e.startTurn + spreadOf( e.id ) );
		int[] caught = new int[n], out = new int[n];
		Arrays.fill( caught, NEVER );
		Arrays.fill( out, NEVER );
		//the turns each box cell is kept from the fire, NEVER where it is not
		int[] barred = new int[n];
		Arrays.fill( barred, NEVER );
		for (Map.Entry<Long, int[]> b : log.bars.entrySet()){
			long k = b.getKey();
			int wx = (int)(k & 0xFFFFFFFFL), wy = (int)(k >> 32);
			int x = wx - x0, y = wy - y0;
			if (x >= 0 && y >= 0 && x < side && y < side) barred[x + y * side] = b.getValue()[0];
		}
		double wind = windOf( e.id );
		float cw = (float) Math.cos( wind ), sw = (float) Math.sin( wind );
		long seedJ = e.id ^ SALT_FIRE;
		PriorityQueue<long[]> pq = new PriorityQueue<>( 64, ( a, b ) -> a[0] != b[0] ? Long.compare( a[0], b[0] ) : Long.compare( a[1], b[1] ) );
		int origin = r + r * side;
		ArrayList<Integer> caughtCells = new ArrayList<>();
		if (kind[origin] != NONE && e.startTurn < stop && barred[origin] > e.startTurn){
			caught[origin] = e.startTurn;
			pq.add( new long[]{ e.startTurn, origin } );
		}
		while (!pq.isEmpty()){
			long[] top = pq.poll();
			int i = (int) top[1], t = (int) top[0];
			if (t != caught[i]) continue;
			if (t >= stop || barred[i] <= t){
				caught[i] = NEVER;
				continue;
			}
			caughtCells.add( i );
			int x = i % side, y = i / side;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					if (dx == 0 && dy == 0) continue;
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= side || ny >= side) continue;
					int j = nx + ny * side;
					if (kind[j] == NONE || caught[j] <= t) continue;
					//the jitter of the cell it runs into: 0.7 to 1.6
					float jit = 0.7f + 0.9f * (float)((WorldEvents.mix( seedJ, x0 + nx, y0 + ny ) >>> 11) * 0x1.0p-53);
					float len = dx != 0 && dy != 0 ? 1.4142f : 1f;
					//downwind 0.6, across 1.15, against 1.7
					float along = (dx * cw + dy * sw) / len;
					float step = STEP * pace( kind[j] ) * jit * (1.15f - 0.55f * along) * len;
					int tj = t + Math.max( 1, Math.round( step ) );
					//the flames on this cell spread no further once it is kept from them
					if (barred[i] <= tj) continue;
					if (tj < caught[j]){
						caught[j] = tj;
						pq.add( new long[]{ tj, j } );
					}
				}
			}
		}
		int[] order = new int[caughtCells.size()];
		int[] changes = new int[caughtCells.size() * 3];
		int c = 0;
		for (int k = 0; k < order.length; k++){
			int i = caughtCells.get( k );
			order[k] = i;
			int end = caught[i] + flame( kind[i] );
			end = Math.min( end, quench );
			if (barred[i] > caught[i]) end = Math.min( end, barred[i] );
			out[i] = Math.max( caught[i] + 1, end );
			changes[c++] = out[i];
			if (kind[i] == OAK || kind[i] == PINE){
				changes[c++] = out[i] + SAPLING_TURNS;
				changes[c++] = out[i] + TREE_TURNS;
			} else {
				changes[c++] = out[i] + ASH_TURNS;
			}
		}
		changes = Arrays.copyOf( changes, c );
		Arrays.sort( changes );
		return new Blaze( e.id, x0, y0, side, kind, caught, out, order, changes, logId, log.barsVersion(), quench );
	}

	// ------------------------------------------------------------ the land, as the window shows it

	/** The wildfires about a level: those the log holds whose scar is not yet gone. */
	static ArrayList<WorldEvents.Event> fires( WorldEventLog log, int turn ){
		ArrayList<WorldEvents.Event> out = new ArrayList<>();
		for (WorldEvents.Event e : log.forced){
			if (e.type == WorldEvents.Type.WILDFIRE && e.startTurn <= turn && turn < e.lingersUntil()) out.add( e );
		}
		return out;
	}

	//does a fire's box reach into the window at (ox, oy)?
	private static boolean reaches( WorldEvents.Event e, int ox, int oy ){
		int r = radiusOf( e.id );
		return e.wx + r >= ox && e.wy + r >= oy && e.wx - r < ox + OverworldLevel.WIDTH && e.wy - r < oy + OverworldLevel.HEIGHT;
	}

	/** The wildfires' scars on the window at (ox, oy) at a turn, into `out` (world key -> terrain):
	 *  stumps, saplings and burnt ground, the window's interior only (OverworldLevel.scorchFor). */
	public static void scars( long seed, WorldEventLog log, int ox, int oy, int turn, HashMap<Long, Integer> out ){
		for (WorldEvents.Event e : fires( log, turn )){
			if (!reaches( e, ox, oy )) continue;
			Blaze b = blaze( seed, e, log );
			for (int i : b.order){
				int t = b.scar( i, turn );
				if (t == -1) continue;
				int wx = b.wx( i ), wy = b.wy( i );
				int x = wx - ox, y = wy - oy;
				if (x <= 0 || y <= 0 || x >= OverworldLevel.WIDTH - 1 || y >= OverworldLevel.HEIGHT - 1) continue;
				out.put( OverworldLevel.worldKey( wx, wy ), t );
			}
		}
	}

	/** The scars' stage about the window, as one number: it changes when anything they lay does. */
	public static long sig( long seed, WorldEventLog log, int ox, int oy, int turn ){
		long sig = 0;
		for (WorldEvents.Event e : fires( log, turn )){
			if (!reaches( e, ox, oy )) continue;
			Blaze b = blaze( seed, e, log );
			sig = sig * 0x9E3779B97F4A7C15L + e.id * 31 + b.stage( turn ) + 7L * b.barsVersion + b.quench;
		}
		return sig;
	}

	/** The cells of a window alight now, as pairs {window cell, the blob's volume there} (Wildfire's):
	 *  what burns, and how fiercely; empty for none. Never the window's outer two rings (a fire's turn
	 *  reads a cell past it). Actor thread. */
	public static int[] burning( OverworldLevel ow, int turn ){
		int[] out = new int[0];
		int n = 0;
		WorldEventLog log = ow.eventLog();
		int w = ow.width(), h = ow.height();
		for (WorldEvents.Event e : fires( log, turn )){
			if (turn >= e.endTurn || !reaches( e, ow.worldX, ow.worldY )) continue;
			Blaze b = blaze( ow.worldSeed, e, log );
			for (int i : b.order){
				int v = b.heat( i, turn );
				if (v == 0) continue;
				int x = b.wx( i ) - ow.worldX, y = b.wy( i ) - ow.worldY;
				if (x <= 1 || y <= 1 || x >= w - 2 || y >= h - 2) continue;
				int c = x + y * w;
				//no flames where the land is not what burns: built over, dug, under water
				if (!burns( ow.map[c] )) continue;
				if (2 * n + 2 > out.length) out = Arrays.copyOf( out, Math.max( 32, out.length * 2 ) );
				out[2 * n] = c;
				out[2 * n + 1] = v;
				n++;
			}
		}
		return Arrays.copyOf( out, 2 * n );
	}

	/** Is this window cell too near a fire for a beast to be set down there: inside a burning
	 *  fire's box, or FLEE cells round it. */
	public static boolean scares( OverworldLevel ow, int cell ){
		if (ow.altitude() != 0 || cell < 0 || cell >= ow.length()) return false;
		int turn = ow.eventTurn();
		int wx = ow.worldX + cell % ow.width(), wy = ow.worldY + cell / ow.width();
		for (WorldEvents.Event e : fires( ow.eventLog(), turn )){
			if (turn >= e.endTurn || ow.eventResolved( e.id )) continue;
			int reach = radiusOf( e.id ) + FLEE;
			if (Math.abs( wx - e.wx ) <= reach && Math.abs( wy - e.wy ) <= reach) return true;
		}
		return false;
	}

	// ------------------------------------------------------------ the host's step

	//per fire: the turn its cells were last looked at for what they scorch, said once each (not
	//bundled: a reload looks from then on, and may say a warning again, harmlessly)
	private static final HashMap<Long, Integer> scorchedTo = new HashMap<>();
	private static final HashSet<Long> warned = new HashSet<>(), ended = new HashSet<>(), roused = new HashSet<>();
	private static int fledAt = Integer.MIN_VALUE;

	/** The host hero's step on the surface, or a turn he stood through (OverworldLevel.tickEvents):
	 *  fires caught where the weather allows, every fire near tended (put out by the rain, burnt
	 *  out, what the hero made of the land kept from it, what stands beside it scorched, the beasts
	 *  driven off, the homesteads warned), and the window's flames kept alight. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		long t0 = LagMonitor.begin();
		WorldEventLog log = level.eventLog();
		int turn = level.eventTurn();
		ignite( level, log, turn );
		for (WorldEvents.Event e : fires( log, turn )) tend( level, log, e, turn );
		keepAlight( level, turn );
		LagMonitor.end( "OW wildfire", t0 );
	}

	//every candidate within HEAR sectors of a hero at its hour, and every smouldering tree: caught in
	//fire weather, its chance gone otherwise
	private static void ignite( OverworldLevel level, WorldEventLog log, int turn ){
		long seed = level.worldSeed;
		HashSet<Long> sectors = new HashSet<>();
		int w = level.width();
		for (Hero h : OverworldLevel.heroesOn( level )){
			int hsx = Math.floorDiv( level.worldX + h.pos % w, WorldStructures.SECTOR );
			int hsy = Math.floorDiv( level.worldY + h.pos / w, WorldStructures.SECTOR );
			for (int sy = hsy - HEAR; sy <= hsy + HEAR; sy++){
				for (int sx = hsx - HEAR; sx <= hsx + HEAR; sx++) sectors.add( WorldStructures.sectorOf( sx, sy ) );
			}
		}
		int day = WorldEvents.day( turn );
		for (long key : sectors){
			int sx = (int)(key >> 32), sy = (int) key;
			for (int d = day - 1; d <= day; d++){
				WorldEvents.Event c = candidate( seed, sx, sy, d );
				if (c == null || turn < c.startTurn || turn >= c.startTurn + WINDOW) continue;
				if (known( log, c.id ) || level.eventResolved( c.id )) continue;
				if (fireWeather( seed, c.wx, c.wy, turn )) born( level, c, "camp" );
				//not today: the chance is gone
				else level.resolveEvent( c.id, c.startTurn + WINDOW );
			}
		}
		for (int[] hold : new ArrayList<>( log.holdovers )){
			if (turn - hold[2] >= HOLD_TURNS){
				log.holdovers.remove( hold );
				continue;
			}
			if (turn - hold[2] < HOLD_DRY || dryFor( turn ) < HOLD_DRY
					|| ClimateManager.surfaceTempIn( WorldModel.baseBiomeAt( seed, hold[0], hold[1] ) ) < FIRE_C) continue;
			log.holdovers.remove( hold );
			long id = lightningId( hold[0], hold[1], day );
			if (known( log, id )) continue;
			born( level, fire( hold[0], hold[1], turn, id ), "lightning" );
		}
	}

	/** A struck tree's fire flaring on a day: an id of its own, never its sector's camp chance of that
	 *  day (candidate), which may be settled already - a fire under a settled id would be put out by
	 *  nothing, tended by nothing and pinned nowhere. */
	static long lightningId( int wx, int wy, int day ){
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		return WorldEvents.mix( WorldEvents.idOf( WorldEvents.Type.WILDFIRE, sx, sy, day ) ^ SALT_HOLD, wx, wy );
	}

	/** Is this event in the log already? */
	static boolean known( WorldEventLog log, long id ){
		for (WorldEvents.Event e : log.forced) if (e.id == id) return true;
		return false;
	}

	//the fire written into the log, the hero's own work in its box kept from it, and heard of; one
	//with nothing to catch (its heart built over) is over at once, never heard of
	static void born( OverworldLevel level, WorldEvents.Event e, String cause ){
		level.forceEvent( e );
		barEdits( level, e, e.startTurn );
		if (blaze( level.worldSeed, e, level.eventLog() ).extent() == 0){
			level.resolveEvent( e.id, e.endTurn );
			return;
		}
		level.markAnnounced( e );
		level.tellParty( ( h, wx, wy ) -> {
			String dir = WorldEvents.direction( e.wx - wx, e.wy - wy );
			if (dir.equals( "here" )) return GLog.WARNING + Messages.get( WildfireEvent.class, "starts_here_" + cause );
			return GLog.WARNING + Messages.get( WildfireEvent.class, "starts_" + cause, Messages.get( WorldEvents.class, "dir_" + dir ) );
		} );
		xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_WORLD_EVENT );
		if (level.liveScene()) SpatialSound.playPanned( Assets.Sounds.BURNING, 0f, 0.5f, 1f, level.panTowards( e.wx ) );
	}

	//every edit of the land in a fire's box that is no fuel - a built piece, a floor, a field, a
	//channel, a puddle - kept from it from this turn on (the diff store: the whole world's)
	private static void barEdits( OverworldLevel level, WorldEvents.Event e, int from ){
		int r = radiusOf( e.id );
		WorldEventLog log = level.eventLog();
		level.forEachEdit( e.wx - r, e.wy - r, e.wx + r + 1, e.wy + r + 1, ( x, y, t ) -> {
			//its heart is where it smoulders: a struck tree burnt down is the fire's own start
			if (x == e.wx && y == e.wy) return;
			if (!burns( t )) log.bar( OverworldLevel.worldKey( x, y ), from, e.lingersUntil() );
		} );
	}

	//one fire near: the rain's word on it, its end, what the hero did to its land since, and what it
	//does to what stands round its flames
	private static void tend( OverworldLevel level, WorldEventLog log, WorldEvents.Event e, int turn ){
		boolean over = level.eventResolved( e.id ) || turn >= e.endTurn;
		if (!over){
			int rain = ClimateManager.firstRainAfter( e.startTurn );
			if (rain <= turn) quench( level, e, Math.max( rain, e.startTurn + 1 ), turn );
			else if (level.openSky() && ClimateManager.isRaining()) quench( level, e, turn, turn );
		}
		if (!level.eventResolved( e.id ) && turn >= e.endTurn){
			level.resolveEvent( e.id, e.endTurn );
			if (ended.add( e.id ) && level.eventAnnounced( e.id )){
				level.tellParty( ( h, wx, wy ) -> GLog.POSITIVE + Messages.get( WildfireEvent.class, "burnt_out" ) );
			}
		}
		if (level.eventResolved( e.id ) || turn >= e.endTurn || !reaches( e, level.worldX, level.worldY )) return;
		//the hero's work in its box since it caught: kept from it from now
		int r = radiusOf( e.id ), w = level.width();
		for (int y = e.wy - r; y <= e.wy + r; y++){
			for (int x = e.wx - r; x <= e.wx + r; x++){
				int c = level.localCell( x, y );
				if (c == -1 || (x == e.wx && y == e.wy)) continue;
				int now = level.map[c];
				if (now == level.pristineAt( c ) || burns( now ) || level.laidAt( c ) == now) continue;
				long key = OverworldLevel.worldKey( x, y );
				if (!log.barred( key, turn )) log.bar( key, turn, e.lingersUntil() );
			}
		}
		Blaze b = blaze( level.worldSeed, e, log );
		scorch( level, b, turn );
		warn( level, e, b, turn );
		if (turn - fledAt >= 5 || turn < fledAt){
			fledAt = turn;
			flee( level, b, turn );
		}
		rouse( level, b, turn );
	}

	//a hero resting with the flames ROUSE cells off is roused, and told once a fire: on the surface a
	//fire's burn is fierce (Burning by its depth), and the front comes on slowly enough to step away
	private static void rouse( OverworldLevel level, Blaze b, int turn ){
		Hero h = Dungeon.hero;
		if (h == null || !h.isAlive() || Dungeon.level != level || h.pos < 0 || h.pos >= level.length()) return;
		int w = level.width(), hx = level.worldX + h.pos % w, hy = level.worldY + h.pos / w;
		if (Math.abs( hx - (b.x0 + b.side / 2) ) > b.side / 2 + ROUSE || Math.abs( hy - (b.y0 + b.side / 2) ) > b.side / 2 + ROUSE) return;
		for (int i : b.order){
			if (!b.burningAt( i, turn ) || Math.max( Math.abs( b.wx( i ) - hx ), Math.abs( b.wy( i ) - hy ) ) > ROUSE) continue;
			if (h.resting) h.interrupt();
			if (roused.add( b.id )) GLog.w( Messages.get( WildfireEvent.class, "close" ) );
			return;
		}
	}

	//rain on the fire: out for good from that turn, said once where it was heard of
	private static void quench( OverworldLevel level, WorldEvents.Event e, int at, int turn ){
		WorldEventLog log = level.eventLog();
		Blaze before = blaze( level.worldSeed, e, log );
		log.quench.put( e.id, at );
		level.resolveEvent( e.id, e.endTurn );
		ended.add( e.id );
		if (level.eventAnnounced( e.id )){
			level.tellParty( ( h, wx, wy ) -> GLog.POSITIVE + Messages.get( WildfireEvent.class, "quenched" ) );
		}
		//the flames hiss out in steam where they are seen
		if (level.liveScene()){
			int w = level.width();
			for (int i : before.order){
				if (!before.burningAt( i, turn )) continue;
				int c = level.localCell( before.wx( i ), before.wy( i ) );
				if (c == -1 || !level.heroFOV[c]) continue;
				PointF p = DungeonTilemap.tileCenterToWorld( c );
				FxRecipes.steam( p.x, p.y, 3 );
			}
			SpatialSound.playPanned( Assets.Sounds.BURNING, 0f, 0.3f, 0.6f, level.panTowards( e.wx ) );
		}
	}

	//what stands beside a cell as it catches: a wooden piece scorched, a crop withered; and a tree
	//burnt down before the hero's eyes leaves charcoal in its ashes now and then (Gathering.charred)
	private static void scorch( OverworldLevel level, Blaze b, int turn ){
		Integer last = scorchedTo.get( b.id );
		scorchedTo.put( b.id, turn );
		if (last == null || last >= turn) return;
		HashSet<Integer> hit = new HashSet<>();
		for (int i : b.order){
			if ((b.kind[i] == OAK || b.kind[i] == PINE) && b.out[i] > last && b.out[i] <= turn){
				int c = level.localCell( b.wx( i ), b.wy( i ) );
				//on open ground by the trunk: the stump is laid a few turns later
				int at = c == -1 ? -1 : HuntEvent.openGround( level, c, 1 );
				if (at != -1 && com.watabou.utils.Random.Int( CHARCOAL ) == 0){
					xyz.gabriwar.warpedpixeldungeon.items.Heap h = level.drop( new xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal()
							.quantity( com.watabou.utils.Random.IntRange( 1, 2 ) ), at );
					if (h.sprite != null) h.sprite.drop();
				}
			}
			if (b.caught[i] <= last || b.caught[i] > turn) continue;
			int c = level.localCell( b.wx( i ), b.wy( i ) );
			if (c == -1) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int m = c + n;
				if (m < 0 || m >= level.length() || !hit.add( m )) continue;
				if (Homestead.isBuilt( level, m ) && (Terrain.flags[level.map[m]] & Terrain.FLAMABLE) != 0){
					//scorched, never wrecked: a fire passing leaves every piece standing on one health at least
					xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadStore.Built built =
							level.homestead().builtAt( level.worldX + m % level.width(), level.worldY + m / level.width() );
					if (built != null && built.hp > 1) Homestead.damage( level, m, Math.min( SCORCH, built.hp - 1 ), WildfireEvent.class );
				}
				xyz.gabriwar.warpedpixeldungeon.homestead.farming.Crop crop = Farming.cropAt( level, m );
				if (crop != null) Farming.dies( level, m, crop, Farming.Outcome.SCORCHED, true );
			}
		}
	}

	//once a fire: every player with a homestead within WARN of its box hears it is coming - from
	//which way, or that it is right by the homestead - and how to stop it
	private static void warn( OverworldLevel level, WorldEvents.Event e, Blaze b, int turn ){
		if (warned.contains( e.id )) return;
		int r = radiusOf( e.id ) + WARN;
		final int[] near = { Integer.MAX_VALUE, 0, 0 };
		level.homestead().forEachBuilt( e.wx - r, e.wy - r, e.wx + r + 1, e.wy + r + 1, ( x, y, built ) -> {
			int d = Math.max( Math.abs( x - e.wx ), Math.abs( y - e.wy ) );
			if (d < near[0]){
				near[0] = d;
				near[1] = x;
				near[2] = y;
			}
		} );
		if (near[0] == Integer.MAX_VALUE) return;
		warned.add( e.id );
		//the way from the homestead to where the fire caught
		final String dir = WorldEvents.direction( e.wx - near[1], e.wy - near[2] );
		level.tellParty( ( h, wx, wy ) -> GLog.NEGATIVE + (dir.equals( "here" )
				? Messages.get( WildfireEvent.class, "warn_here" )
				: Messages.get( WildfireEvent.class, "warn", Messages.get( WorldEvents.class, "dir_" + dir ) )) );
	}

	//the wild beasts within FLEE of the flames run from them: off out of sight, away in plain view;
	//the critters scatter
	private static void flee( OverworldLevel level, Blaze b, int turn ){
		int w = level.width();
		ArrayList<Integer> alight = new ArrayList<>();
		for (int i : b.order){
			if (!b.burningAt( i, turn )) continue;
			int c = level.localCell( b.wx( i ), b.wy( i ) );
			if (c != -1) alight.add( c );
		}
		if (alight.isEmpty()) return;
		OverworldCritters.noise( alight.get( alight.size() / 2 ) );
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!OverworldFauna.isFauna( m ) || m.buff( HuntPack.class ) != null) continue;
			if (m instanceof HomesteadActor && ((HomesteadActor) m).homesteadKept()) continue;
			//the beasts of the water (an alligator in its pool, the rivers' pike) have nothing to fear of it
			OverworldFauna.Habitat home = OverworldFauna.home( m.getClass() );
			if (home == OverworldFauna.Habitat.SHALLOWS || home == OverworldFauna.Habitat.POOL) continue;
			int best = Integer.MAX_VALUE, from = -1;
			for (int c : alight){
				int d = level.distance( c, m.pos );
				if (d < best){
					best = d;
					from = c;
				}
			}
			if (best > FLEE || m.state == m.HUNTING) continue;
			//out of sight it is simply gone, but for a world event's own (a migrating herd's, a roaming
			//great beast): those the event keeps, so they only run
			boolean events = MigrationEvent.onTheMove( m ) || m.buff( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Roaming.class ) != null;
			if (!events && OverworldLevel.heroDistance( level, m.pos ) > HuntEvent.UNSEEN && !xyz.gabriwar.warpedpixeldungeon.net.NetManager.anyHeroSees( m.pos )){
				OverworldLevel.vanish( level, m );
				continue;
			}
			int dx = m.pos % w - from % w, dy = m.pos / w - from / w;
			float len = Math.max( 1f, (float) Math.sqrt( dx * dx + dy * dy ) );
			int tx = Math.max( 2, Math.min( w - 3, m.pos % w + Math.round( dx / len * 12 ) ) );
			int ty = Math.max( 2, Math.min( level.height() - 3, m.pos / w + Math.round( dy / len * 12 ) ) );
			m.beckon( tx + ty * w );
		}
	}

	//the window's flames: the blob alight wherever a fire burns in it, so it burns on its own turns
	private static void keepAlight( OverworldLevel level, int turn ){
		xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob have = level.blobs.get( xyz.gabriwar.warpedpixeldungeon.actors.blobs.Wildfire.class );
		if (have != null && have.volume > 0) return;
		int[] alight = burning( level, turn );
		if (alight.length == 0) return;
		xyz.gabriwar.warpedpixeldungeon.actors.blobs.Wildfire f = xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob.seed(
				alight[0], alight[1], xyz.gabriwar.warpedpixeldungeon.actors.blobs.Wildfire.class, level );
		if (level.liveScene()) GameScene.add( f );
		else Actor.add( f );
	}

	// ------------------------------------------------------------ what the hero and the storms do

	/**
	 * Water on a cell of the shown level (PotionOfWater poured or splashed, a frost): a wildfire
	 * burning there or on a side of it is put out on the cell and its four sides, which no wildfire
	 * reaches from now on. True when one burned there.
	 */
	public static boolean doused( Level level, int cell ){
		if (!(level instanceof OverworldLevel) || ((OverworldLevel) level).isMirror() || cell < 0 || cell >= level.length()) return false;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.altitude() != 0) return false;
		int turn = ow.eventTurn();
		WorldEventLog log = ow.eventLog();
		int w = ow.width(), wx = ow.worldX + cell % w, wy = ow.worldY + cell / w;
		boolean any = false;
		for (WorldEvents.Event e : fires( log, turn )){
			if (turn >= e.endTurn || ow.eventResolved( e.id )) continue;
			Blaze b = blaze( ow.worldSeed, e, log );
			int i = b.index( wx, wy );
			if (i == -1) continue;
			boolean alight = false;
			for (int[] d : CROSS){
				int k = b.index( wx + d[0], wy + d[1] );
				if (k != -1 && b.burningAt( k, turn )) alight = true;
			}
			if (!alight) continue;
			any = true;
			for (int[] d : CROSS){
				log.bar( OverworldLevel.worldKey( wx + d[0], wy + d[1] ), turn, e.lingersUntil() );
			}
		}
		return any;
	}

	/**
	 * A storm's bolt struck a tree on a cell of the shown level (StormStrikes.strike): in warm weather
	 * (HOLD_C), on the surface's woodland or meadow a fire may burn in, one in HOLD_ODDS is left
	 * smouldering - at most HOLDOVERS at once, the oldest let go - and flares into a wildfire once
	 * HOLD_DRY turns go by dry in fire weather.
	 */
	public static void lightning( Level level, int cell ){
		if (!(level instanceof OverworldLevel) || ((OverworldLevel) level).isMirror()) return;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.altitude() != 0 || cell < 0 || cell >= ow.length()) return;
		int wx = ow.worldX + cell % ow.width(), wy = ow.worldY + cell / ow.width();
		WorldModel.Biome b = WorldModel.baseBiomeAt( ow.worldSeed, wx, wy );
		if (b != WorldModel.Biome.FOREST && b != WorldModel.Biome.MEADOW) return;
		if (ClimateManager.surfaceTempIn( b ) < HOLD_C || !inLand( ow.worldSeed, wx, wy )) return;
		if (com.watabou.utils.Random.Int( HOLD_ODDS ) != 0) return;
		WorldEventLog log = ow.eventLog();
		while (log.holdovers.size() >= HOLDOVERS) log.holdovers.remove( 0 );
		log.holdovers.add( new int[]{ wx, wy, ow.eventTurn() } );
	}

	// ------------------------------------------------------------ debug

	/** Debug (the wildfire scene): a fire forced on world cell (wx, wy), caught `ago` turns before now
	 *  (so it is already well alight), through the world's own registry; its id one of its own. */
	public static WorldEvents.Event force( OverworldLevel level, int wx, int wy, int ago ){
		int turn = level.eventTurn();
		int start = turn - Math.max( 0, ago );
		long id = WorldEvents.mix( WorldEvents.idOf( WorldEvents.Type.WILDFIRE, Math.floorDiv( wx, WorldStructures.SECTOR ),
				Math.floorDiv( wy, WorldStructures.SECTOR ), WorldEvents.day( start ) ) ^ 0xF0BCEDL, start, ((long) wx << 32) ^ (wy & 0xFFFFFFFFL) );
		WorldEvents.Event e = fire( wx, wy, start, id );
		born( level, e, "camp" );
		return e;
	}

	/** Debug: the world cell nearest (wx, wy) a fire could burn out from - woodland or meadow fuel
	 *  in the land a fire may burn, with fuel all round it - searched outwards to `reach`; null for none. */
	public static int[] findSpot( long seed, int wx, int wy, int reach ){
		for (int r = 0; r <= reach; r += 2){
			for (int dy = -r; dy <= r; dy += 2){
				for (int dx = -r; dx <= r; dx += 2){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = wx + dx, y = wy + dy;
					WorldModel.Biome b = WorldModel.baseBiomeAt( seed, x, y );
					if (b != WorldModel.Biome.FOREST && b != WorldModel.Biome.MEADOW) continue;
					boolean ok = inLand( seed, x, y );
					for (int k = 0; k < 9 && ok; k++){
						ok = burns( WorldEvents.baseTerrain( seed, x + k % 3 - 1, y + k / 3 - 1 ) );
					}
					if (ok) return new int[]{ x, y };
				}
			}
		}
		return null;
	}
}
