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

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * The homesteads' ROOMS. A room is a 4-connected area of open (not solid) cells of a world slice,
 * closed all round by walls - built walls, built windows, built doors and barred doors in any
 * state, and the land's own rock (WALL, WALL_DECO, a crystal seam), so a pocket dug out of a cave
 * and shut with a door is one - with at least one built piece among them, no more than MAX_CELLS
 * cells, and at least MIN_ROOFED of its floor under a roof. A fence, a gate, a palisade, a tree or
 * a rock in the ring is a gap (rooms need walls); rubble is open ground, so a wrecked wall lets the
 * room out. Furniture of the land's terrains standing inside (a statue, a bookshelf: furnishing) is
 * part of the room, not a gap in it. Every cell of a CAVE slice (OverworldLevel.openSky() false: altitude below 0,
 * WorldLayers.openSky) counts as roofed: the cave's own rock is overhead. A built room needs nothing
 * else - no claim: anything the hero closes in anywhere he may build is a room.
 *
 * Rooms.at(level, cell) finds the room a cell is in, or null. The answer is cached per slice
 * (HomesteadStore's Cache), for every cell the flood that found it reached - a room's cells, and
 * the cells a failed flood went through on its way out - and forgotten only round a cell that
 * changes: every terrain edit (OverworldLevel.updateCellFlags: building, wrecking, a door opening
 * or shutting, a pick in the rock) and every roof going on or coming off (Homestead) calls
 * changed(level, cell), which drops just the answers whose flood reached that cell or one beside
 * it. That is exact: a flood that failed is held to its failure by the cells it went through (its
 * way out, its first MAX_CELLS + 1 cells, the gap beside it), so only a change on or beside them
 * can turn it into a room. A flood stops at MAX_CELLS + 1 cells, so a missing wall never sends it
 * over the whole window, and none runs while the window holds nothing built. A new window (a
 * rebase, a jump, the season turning: OverworldLevel.windowVersion) forgets them all.
 *
 * A Room's cells are window cells of the window it was found in: ask Rooms.at again rather than
 * keeping one across turns. What changes without the terrain changing is read live: whether it is
 * sealed (doors shut, no wall broken) and its heat (HeatSources).
 *
 * Thread-safe: the climate (TileTemperature), the sounds (Earshot, on the render thread) and the
 * game ask it. Host and co-op mirror alike: it only reads the level.
 */
public final class Rooms {

	private Rooms(){}

	/** The most cells a room holds: a bigger space is a yard, not a room. */
	public static final int MAX_CELLS = 600;
	/** The share of a room's floor that must be under a roof. */
	public static final float MIN_ROOFED = 0.8f;
	/** The air a perfectly insulated room settles at (C), whatever the weather outside. */
	public static final float SHELTER_TEMP = 15f;
	/** A built piece in a room's walls with less than this share of its health left is broken: the
	 *  room is not sealed. */
	public static final float BROKEN_BELOW = 0.5f;

	/** How much of the outside air each kind of wall keeps out (insulation, 0..1): a room's is the
	 *  average over the cells of its walls. */
	public static final float LOG = 0.3f, PLANK = 0.4f, STONE = 0.5f, BRICK = 0.7f, ROCK = 0.6f,
			WINDOW = 0.2f, DOOR = 0.3f;
	//a built wall's, by its material (BuiltTerrain.material: HomesteadTiles.LOG..BRICK)
	private static final float[] WALL = { LOG, PLANK, STONE, BRICK };

	/** What a cell is to the rooms: in one, or why not. */
	public enum Verdict {
		/** Inside a room. */
		ROOM,
		/** A wall, a door or anything solid: no cell of a room. */
		SOLID,
		/** The space runs on to the edge of the window: open country. */
		OPEN,
		/** More than MAX_CELLS cells: no room, or not closed in. */
		TOO_BIG,
		/** Something in its ring is no wall: a fence, a gate, a palisade, a tree, a rock. */
		GAP,
		/** Closed in by the land alone: a room needs a built wall, window or door. */
		NO_BUILT_WALL,
		/** Less than MIN_ROOFED of its floor is under a roof. */
		UNROOFED;

		/** What the hero is told (homestead.properties: homestead.rooms.verdict_*). */
		public String message(){
			return Messages.get( Rooms.class, "verdict_" + name().toLowerCase( Locale.ENGLISH ) );
		}
	}

	/** A room: its cells and walls (window cells of the window it was found in) and what it is. */
	public static final class Room {
		/** The slice it is on. */
		public final OverworldLevel level;
		/** Its world key (OverworldLevel.worldKey) of its top-left cell (the first of `cells`):
		 *  the same room keeps it through window moves, as long as that cell stays. */
		public final long id;
		/** Its floor, and the furniture standing on it (furnishing), ascending. */
		public final int[] cells;
		/** Its walls, windows and doors: the cells beside its floor that close it, ascending. */
		public final int[] walls;
		/** The share of its floor under a roof (1 on a cave slice). */
		public final float roofFraction;
		/** The average insulation of its walls (LOG..DOOR), 0..1. */
		public final float insulation;

		Room( OverworldLevel level, int[] cells, int[] walls, float roofFraction, float insulation ){
			this.level = level;
			this.cells = cells;
			this.walls = walls;
			this.roofFraction = roofFraction;
			this.insulation = insulation;
			int w = level.width();
			this.id = OverworldLevel.worldKey( level.worldX + cells[0] % w, level.worldY + cells[0] / w );
		}

		public int size(){
			return cells.length;
		}

		public boolean contains( int cell ){
			return Arrays.binarySearch( cells, cell ) >= 0;
		}

		/** Every door in its walls shut, and no built piece of them broken (below BROKEN_BELOW of
		 *  its health): nothing gets in without breaking in. Read live. */
		public boolean sealed(){
			for (int b : walls){
				int t = level.map[b];
				if (Float.isNaN( insulationOf( t ) )) return false;
				if (BuiltTerrain.door( t ) && BuiltTerrain.open( t )) return false;
				BuildPiece p = Homestead.pieceAt( level, b );
				if (p != null && p.destructible() && Homestead.hpAt( level, b ) < p.maxHp * BROKEN_BELOW) return false;
			}
			return true;
		}

		/** The warmth its fires give it now (C, HeatSources): read live. */
		public float heat(){
			return HeatSources.roomHeat( level, this );
		}

		/** Is a fire warming it (heat above 0)? The hero is Comfy in a heated room. */
		public boolean heated(){
			return heat() > 0f;
		}

		/** The air inside it with the air outside at `outside` (C): drawn toward SHELTER_TEMP as
		 *  much as its walls insulate, plus its heat. */
		public float air( float outside ){
			return outside + (SHELTER_TEMP - outside) * insulation + heat();
		}
	}

	//a flood's outcome, held for every cell it reached: the room (ROOM), or why there is none
	private static final class Found {
		final Verdict verdict;
		final Room room;
		final int[] reached;

		Found( Verdict verdict, Room room, int[] reached ){
			this.verdict = verdict;
			this.room = room;
			this.reached = reached;
		}
	}

	private static final int[] NONE = new int[0];
	//the answers no flood is run for, never cached
	private static final Found SOLID_CELL = new Found( Verdict.SOLID, null, NONE );
	private static final Found NOTHING_BUILT = new Found( Verdict.NO_BUILT_WALL, null, NONE );

	/** One slice's answers (HomesteadStore keeps it; never saved). Guarded by itself. */
	static final class Cache {
		private int originX, originY, windowVersion = Integer.MIN_VALUE;
		private final HashMap<Integer, Found> byCell = new HashMap<>();
		//the flood's marks: a cell is reached (or a wall seen) in this flood when it holds `stamp`
		private int[] reached, walled;
		private int stamp;
		//whether the window holds anything built, as of the store's version
		private int builtVersion = Integer.MIN_VALUE;
		private boolean anyBuilt;

		//a new window: every answer was for the old one
		private void follow( OverworldLevel ow ){
			if (ow.worldX == originX && ow.worldY == originY && ow.windowVersion() == windowVersion) return;
			byCell.clear();
			originX = ow.worldX;
			originY = ow.worldY;
			windowVersion = ow.windowVersion();
			builtVersion = Integer.MIN_VALUE;
		}

		private boolean anyBuilt( OverworldLevel ow ){
			HomesteadStore store = ow.homestead();
			if (store.version() != builtVersion){
				builtVersion = store.version();
				final boolean[] any = { false };
				store.forEachBuilt( ow.worldX, ow.worldY, ow.worldX + ow.width(), ow.worldY + ow.height(),
						(x, y, b) -> any[0] = true );
				anyBuilt = any[0];
			}
			return anyBuilt;
		}

		synchronized void clear(){
			byCell.clear();
			windowVersion = Integer.MIN_VALUE;
			builtVersion = Integer.MIN_VALUE;
		}
	}

	// ----------------------------------------------------------------- asking

	/** The room a cell of a level is in, or null: off the world's slices, on a wall, or in no room. */
	public static Room at( Level level, int cell ){
		Found f = find( level, cell );
		return f == null ? null : f.room;
	}

	/** Whether a cell is in a room and why not (the build menu's inspect); null off the world's slices. */
	public static Verdict check( Level level, int cell ){
		Found f = find( level, cell );
		return f == null ? null : f.verdict;
	}

	/** Is the cell in a room whose doors are shut and walls whole (Room.sealed)? */
	public static boolean sealedAt( Level level, int cell ){
		Room r = at( level, cell );
		return r != null && r.sealed();
	}

	private static Found find( Level level, int cell ){
		if (!(level instanceof OverworldLevel) || cell < 0 || cell >= level.length()) return null;
		OverworldLevel ow = (OverworldLevel) level;
		Cache c = ow.homestead().roomCache;
		synchronized (c){
			c.follow( ow );
			Found f = c.byCell.get( cell );
			if (f != null) return f;
			if (!enterable( ow, cell )) return SOLID_CELL;
			if (!c.anyBuilt( ow )) return NOTHING_BUILT;
			f = flood( ow, c, cell );
			for (int r : f.reached) c.byCell.put( r, f );
			return f;
		}
	}

	/**
	 * A cell of a slice changed (its terrain, or the roof over it): the answers of every flood that
	 * reached it or a cell beside it are forgotten. OverworldLevel.updateCellFlags calls it for every
	 * terrain edit, the Homestead facade for every roof.
	 */
	public static void changed( Level level, int cell ){
		if (!(level instanceof OverworldLevel)) return;
		HomesteadStore store = ((OverworldLevel) level).homestead();
		//a terrain edit while the level is still being made
		if (store == null) return;
		Cache c = store.roomCache;
		synchronized (c){
			if (c.byCell.isEmpty()) return;
			int w = level.width(), x = cell % w, y = cell / w;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= w || ny >= level.height()) continue;
					Found f = c.byCell.get( nx + ny * w );
					if (f == null) continue;
					for (int r : f.reached) c.byCell.remove( r, f );
				}
			}
		}
	}

	/** Forgets every answer for a slice (a co-op mirror took over its host's roofs). */
	public static void forget( Level level ){
		if (level instanceof OverworldLevel) ((OverworldLevel) level).homestead().roomCache.clear();
	}

	// ----------------------------------------------------------------- the flood

	/** How well a terrain keeps a room's air as part of its walls (LOG..DOOR), NaN for anything
	 *  that is no wall of a room. */
	public static float insulationOf( int t ){
		if (BuiltTerrain.wall( t )) return WALL[BuiltTerrain.material( t )];
		if (BuiltTerrain.window( t )) return WINDOW;
		if (BuiltTerrain.door( t )) return DOOR;
		if (t == Terrain.WALL || t == Terrain.WALL_DECO || t == Terrain.MINE_CRYSTAL) return ROCK;
		return Float.NaN;
	}

	/** Furniture of the land's own terrain that stands IN a room rather than closing it: a statue,
	 *  a bookshelf, an alchemy pot. Solid, yet no gap in a room's walls - the space flows round it
	 *  (a room's cells hold it, and a roof may go over it). */
	public static boolean furnishing( int t ){
		return t == Terrain.STATUE || t == Terrain.STATUE_SP || t == Terrain.BOOKSHELF
				|| t == Terrain.EMPTY_BOOKSHELF || t == Terrain.ALCHEMY;
	}

	//the floor of a room may be here: inside the window's rim, open (or furnished), and no wall (an
	//open door is open ground but a wall of its room)
	private static boolean enterable( OverworldLevel ow, int cell ){
		int w = ow.width(), x = cell % w, y = cell / w;
		return x > 0 && y > 0 && x < w - 1 && y < ow.height() - 1
				&& (!ow.solid[cell] || furnishing( ow.map[cell] )) && Float.isNaN( insulationOf( ow.map[cell] ) );
	}

	private static final int[] DX = { 0, -1, 1, 0 }, DY = { -1, 0, 0, 1 };

	//the space round a cell, breadth first, until it is closed, runs out, grows past MAX_CELLS or
	//meets a gap
	private static Found flood( OverworldLevel ow, Cache c, int start ){
		int w = ow.width(), h = ow.height(), len = ow.length();
		if (c.reached == null || c.reached.length != len){
			c.reached = new int[len];
			c.walled = new int[len];
			c.stamp = 0;
		}
		int stamp = ++c.stamp;
		int[] queue = new int[MAX_CELLS + 1];
		int[] walls = new int[64];
		int head = 0, tail = 0, nWalls = 0, built = 0;
		float insulated = 0f;
		Verdict failed = null;
		queue[tail++] = start;
		c.reached[start] = stamp;
		flood:
		while (head < tail){
			int cell = queue[head++];
			int cx = cell % w, cy = cell / w;
			for (int k = 0; k < 4; k++){
				int nx = cx + DX[k], ny = cy + DY[k];
				if (nx <= 0 || ny <= 0 || nx >= w - 1 || ny >= h - 1){
					failed = Verdict.OPEN;
					break flood;
				}
				int n = nx + ny * w;
				if (c.reached[n] == stamp || c.walled[n] == stamp) continue;
				int t = ow.map[n];
				float ins = insulationOf( t );
				if (!Float.isNaN( ins )){
					c.walled[n] = stamp;
					if (nWalls == walls.length) walls = Arrays.copyOf( walls, nWalls * 2 );
					walls[nWalls++] = n;
					insulated += ins;
					if (Terrain.homestead( t )) built++;
					continue;
				}
				//furniture inside is part of the room; anything else solid is a gap in its walls
				if (ow.solid[n] && !furnishing( t )){
					failed = Verdict.GAP;
					break flood;
				}
				c.reached[n] = stamp;
				queue[tail++] = n;
				//one past the most a room holds, and kept: the cells it took to know
				if (tail > MAX_CELLS){
					failed = Verdict.TOO_BIG;
					break flood;
				}
			}
		}
		int[] cells = Arrays.copyOf( queue, tail );
		if (failed != null) return new Found( failed, null, cells );
		if (built == 0) return new Found( Verdict.NO_BUILT_WALL, null, cells );
		Arrays.sort( cells );
		float roofed = 1f;
		if (ow.openSky()){
			int n = 0;
			for (int cell : cells) if (Homestead.roofAt( ow, cell ) != -1) n++;
			roofed = n / (float) cells.length;
		}
		if (roofed < MIN_ROOFED) return new Found( Verdict.UNROOFED, null, cells );
		int[] ring = Arrays.copyOf( walls, nWalls );
		Arrays.sort( ring );
		return new Found( Verdict.ROOM, new Room( ow, cells, ring, roofed, insulated / nWalls ), cells );
	}

	// ----------------------------------------------------------------- telling

	/**
	 * What a look at a cell tells of its room, a line each: the room's name (RoomTraits) and size,
	 * how much is roofed, how well its walls keep the cold out, whether it is sealed, its heat and
	 * the air inside, and what its traits add - or why the cell is in no room. For the build menu's
	 * inspect. Empty off the world's slices.
	 */
	public static String describe( Level level, int cell ){
		Verdict v = check( level, cell );
		if (v == null) return "";
		if (v != Verdict.ROOM) return v.message();
		OverworldLevel ow = (OverworldLevel) level;
		Room room = at( ow, cell );
		RoomTraits.Traits traits = RoomTraits.of( ow, room );
		StringBuilder s = new StringBuilder();
		String name = traits.name();
		s.append( name != null ? Messages.get( Rooms.class, "named", name ) : Messages.get( Rooms.class, "unnamed" ) );
		s.append( '\n' ).append( Messages.get( Rooms.class, "size", room.size(), Math.round( room.roofFraction * 100 ) ) );
		s.append( '\n' ).append( Messages.get( Rooms.class, "insulation", Math.round( room.insulation * 100 ) ) );
		s.append( '\n' ).append( Messages.get( Rooms.class, room.sealed() ? "sealed" : "unsealed" ) );
		float heat = room.heat();
		if (heat > 0f) s.append( '\n' ).append( Messages.get( Rooms.class, "heat", Messages.decimalFormat( "#.#", heat ) ) );
		float outside = ClimateManager.airAt( ow, cell );
		s.append( '\n' ).append( Messages.get( Rooms.class, "air",
				Messages.decimalFormat( "#.#", room.air( outside ) ), Messages.decimalFormat( "#.#", outside ) ) );
		for (String line : traits.lines()) s.append( '\n' ).append( line );
		return s.toString();
	}
}
