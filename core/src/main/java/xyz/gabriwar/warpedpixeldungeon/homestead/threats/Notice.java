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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.HeatSources;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadTiles;
import com.watabou.utils.PathFinder;

/**
 * How much a home draws the eye of those who would take from it: what it holds (the things in its
 * chests, its crops, how much is built) and its light by night, less its defences (stone and brick
 * walls, barred doors, a palisade round it, a watchtower, an alarm bell). Above the RAID_LINE
 * (HomeThreats) it draws bandits, and a wolf pack comes twice as often.
 *
 * Worked out from the home's ground (HomeThreats.AREA round its hearthstone) on its own slice, when
 * the hero comes into the home's window, at each dusk and every little while he is there
 * (ThreatSlice), and kept with the run (ThreatWorld) for the nights he is away and for the Ledger
 * window, which shows it part by part.
 */
public final class Notice {

	private Notice(){}

	/** The parts, in the Ledger window's order: what draws the eye, then what turns it away. */
	public enum Part {
		CHESTS, CROPS, BUILT, LIGHT,
		WALLS, BARRED, PALISADE, WATCHTOWER, BELL;

		/** Does this part add to the notice (what the home holds) or take from it (a defence)? */
		public boolean draws(){
			return ordinal() <= LIGHT.ordinal();
		}

		public String title(){
			return Messages.get( Notice.class, "part_" + name().toLowerCase( java.util.Locale.ENGLISH ) );
		}

		/** The most this part counts for, as a positive number. */
		public int cap(){
			switch (this){
				case CHESTS:     return CHEST_CAP;
				case CROPS:      return CROP_CAP;
				case BUILT:      return BUILT_CAP;
				case LIGHT:      return LIGHT_CAP;
				case WALLS:      return WALLS_MAX;
				case BARRED:     return BARRED_CAP;
				case PALISADE:   return PALISADE_MAX;
				case WATCHTOWER: return WATCHTOWER_OFF;
				default:         return BELL_OFF;
			}
		}
	}

	/** What the home holds: a point for every CHEST_VALUE of the things in its chests, at most CHEST_CAP. */
	public static final int CHEST_VALUE = 100, CHEST_CAP = 40;
	/** A point for every CROPS_EACH crops, at most CROP_CAP. */
	public static final int CROPS_EACH = 2, CROP_CAP = 15;
	/** A point for every BUILT_EACH built pieces, at most BUILT_CAP. */
	public static final int BUILT_EACH = 20, BUILT_CAP = 15;
	/** By night, LIGHT_EACH for every fire burning (a lit hearth, a kiln, a forge, a camp fire) and
	 *  every window of a heated room, at most LIGHT_CAP. */
	public static final int LIGHT_EACH = 5, LIGHT_CAP = 20;
	/** Less up to WALLS_MAX for the share of its walls, windows and doors that is stone or brick wall. */
	public static final int WALLS_MAX = 25;
	/** Less BARRED_EACH for every barred door, at most BARRED_CAP. */
	public static final int BARRED_EACH = 5, BARRED_CAP = 10;
	/** Less PALISADE_MAX for a palisade ring all round it, its share of that for a part of one. */
	public static final int PALISADE_MAX = 15;
	/** Less WATCHTOWER_OFF for a watchtower, BELL_OFF for an alarm bell on its ground. */
	public static final int WATCHTOWER_OFF = 10, BELL_OFF = 5;

	/** How many ways out from the hearthstone the palisade's ring is looked for along, and how far
	 *  (the larger of the two steps): out to where packs and bands come in, so a ring just round the
	 *  home's ground counts. */
	static final int RAYS = 64, RING_REACH = HomeThreats.EDGE_MAX;

	/** A home's notice, part by part (defences negative). */
	public static final class Score {
		final int[] parts = new int[Part.values().length];

		public int get( Part p ){
			return parts[p.ordinal()];
		}

		/** The notice: every part summed, never below nothing. */
		public int total(){
			int sum = 0;
			for (int p : parts) sum += p;
			return Math.max( 0, sum );
		}

		/** Above the raid line: it draws bandits. */
		public boolean aboveLine(){
			return total() > HomeThreats.RAID_LINE;
		}

		int[] toArray(){
			return parts.clone();
		}

		static Score of( int[] saved ){
			Score s = new Score();
			if (saved != null) System.arraycopy( saved, 0, s.parts, 0, Math.min( saved.length, s.parts.length ) );
			return s;
		}
	}

	/**
	 * A home's notice as last worked out (ThreatSlice keeps it current while its hearthstone is in
	 * the window), none before that. Reads nothing of the level: safe from the render thread (the
	 * Ledger window).
	 */
	public static Score of( HomesteadWorld.Home home ){
		ThreatWorld world = ThreatWorld.get();
		return world == null ? new Score() : world.notice( home );
	}

	/** Works a home's notice out on its slice (its light by night when it is dark now) and keeps it. */
	public static Score refresh( OverworldLevel level, HomesteadWorld.Home home ){
		DayNightCycle.Phase phase = DayNightCycle.phase();
		Score s = of( level, home, phase == DayNightCycle.Phase.NIGHT || phase == DayNightCycle.Phase.DUSK );
		ThreatWorld world = ThreatWorld.get();
		if (world != null) world.keepNotice( home, s );
		return s;
	}

	/** A home's notice worked out on its slice, with its light by night counted or not. */
	public static Score of( OverworldLevel level, HomesteadWorld.Home home, boolean night ){
		Score s = new Score();
		int hearth = HomeThreats.hearth( level, home );
		if (hearth == -1) return s;
		int w = level.width(), r = HomeThreats.AREA;
		int hx = hearth % w, hy = hearth / w;
		int x0 = Math.max( 1, hx - r ), x1 = Math.min( w - 2, hx + r );
		int y0 = Math.max( 1, hy - r ), y1 = Math.min( level.height() - 2, hy + r );

		//what stands on its ground
		long chestValue = 0;
		int crops = 0;
		boolean tower = false, bell = false;
		for (Mob m : level.mobs){
			if (!HomeThreats.inArea( level, home, m.pos )) continue;
			if (m instanceof StorageChest) chestValue += valueOf( (StorageChest) m );
			else if (m instanceof FarmCrop) crops++;
			else if (m instanceof Watchtower) tower = true;
			else if (m instanceof AlarmBell) bell = true;
		}
		s.parts[Part.CHESTS.ordinal()] = (int) Math.min( CHEST_CAP, chestValue / CHEST_VALUE );
		s.parts[Part.CROPS.ordinal()] = Math.min( CROP_CAP, crops / CROPS_EACH );

		//what is built on it, and its shell
		final int[] count = new int[4];   //built pieces, shell cells, stone or brick walls, barred doors
		level.homestead().forEachBuilt( home.wx - r, home.wy - r, home.wx + r + 1, home.wy + r + 1, ( x, y, b ) -> {
			int c = level.localCell( x, y );
			if (c == -1) return;
			count[0]++;
			int t = level.map[c];
			if (BuiltTerrain.wallLike( t ) || BuiltTerrain.door( t )) count[1]++;
			if (BuiltTerrain.wall( t ) && (BuiltTerrain.material( t ) == HomesteadTiles.STONE
					|| BuiltTerrain.material( t ) == HomesteadTiles.BRICK)) count[2]++;
			if (BuiltTerrain.barred( t )) count[3]++;
		} );
		s.parts[Part.BUILT.ordinal()] = Math.min( BUILT_CAP, count[0] / BUILT_EACH );
		if (count[1] > 0) s.parts[Part.WALLS.ordinal()] = -Math.round( WALLS_MAX * count[2] / (float) count[1] );
		s.parts[Part.BARRED.ordinal()] = -Math.min( BARRED_CAP, count[3] * BARRED_EACH );

		//its light by night: the fires burning on its ground, the windows of its heated rooms
		if (night){
			int lights = 0;
			for (int y = y0; y <= y1 && lights * LIGHT_EACH < LIGHT_CAP; y++){
				for (int x = x0; x <= x1 && lights * LIGHT_EACH < LIGHT_CAP; x++){
					int c = x + y * w;
					if (BuiltTerrain.window( level.map[c] ) ? litWindow( level, c ) : HeatSources.heatAt( level, c ) > 0) lights++;
				}
			}
			s.parts[Part.LIGHT.ordinal()] = Math.min( LIGHT_CAP, lights * LIGHT_EACH );
		}

		s.parts[Part.PALISADE.ordinal()] = -Math.round( PALISADE_MAX * palisadeShare( level, hearth ) );
		if (tower) s.parts[Part.WATCHTOWER.ordinal()] = -WATCHTOWER_OFF;
		if (bell) s.parts[Part.BELL.ordinal()] = -BELL_OFF;
		return s;
	}

	/** What the things in a chest are worth: their value, a coin for every gold piece. */
	public static long valueOf( StorageChest chest ){
		long sum = 0;
		for (Item i : chest.storage.items) sum += valueOf( i );
		return sum;
	}

	/** What one thing (a whole stack) is worth to a thief: its value, a coin a gold piece; a bag
	 *  its own and all it holds. */
	public static int valueOf( Item item ){
		if (item instanceof Bag){
			long sum = Math.max( 0, item.value() );
			for (Item i : ((Bag) item).items) sum += valueOf( i );
			return (int) Math.min( Integer.MAX_VALUE, sum );
		}
		return Math.max( 0, item instanceof Gold ? item.quantity() : item.value() );
	}

	//a window with a heated room's light behind it
	private static boolean litWindow( OverworldLevel level, int cell ){
		for (int n : PathFinder.NEIGHBOURS4){
			int m = cell + n;
			if (m < 0 || m >= level.length()) continue;
			Rooms.Room room = Rooms.at( level, m );
			if (room != null && room.heated()) return true;
		}
		return false;
	}

	/**
	 * The share of the ways out from the hearthstone a palisade (or a gate in it) stands across,
	 * within RING_REACH: RAYS lines out to the square of that reach (a ring is built square), each
	 * stepped cell by cell as a walker could slip through a diagonal gap. 1 for a closed ring.
	 */
	static float palisadeShare( OverworldLevel level, int hearth ){
		int w = level.width(), hx = hearth % w, hy = hearth / w;
		int guarded = 0;
		for (int k = 0; k < RAYS; k++){
			double a = 2 * Math.PI * k / RAYS;
			double cx = Math.cos( a ), cy = Math.sin( a ), out = RING_REACH / Math.max( Math.abs( cx ), Math.abs( cy ) );
			int tx = hx + (int) Math.round( cx * out ), ty = hy + (int) Math.round( cy * out );
			if (crossesPalisade( level, hx, hy, tx, ty )) guarded++;
		}
		return guarded / (float) RAYS;
	}

	//a line (Bresenham, 8-connected) from the hearthstone out to (tx, ty) meets a palisade or a gate
	private static boolean crossesPalisade( OverworldLevel level, int x, int y, int tx, int ty ){
		int w = level.width();
		int dx = Math.abs( tx - x ), dy = -Math.abs( ty - y ), sx = x < tx ? 1 : -1, sy = y < ty ? 1 : -1;
		int err = dx + dy;
		while (x != tx || y != ty){
			int e2 = 2 * err;
			if (e2 >= dy){ err += dy; x += sx; }
			if (e2 <= dx){ err += dx; y += sy; }
			if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) return false;
			int t = level.map[x + y * w];
			if (t == Terrain.PALISADE || BuiltTerrain.gate( t )) return true;
		}
		return false;
	}
}
