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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadStore;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Tilemap;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * The homesteads' roofs on screen (environment/roofs.png, RoofTiles): four window-sized layers
 * drawn over the walls and the mobs (the level's customWalls), the slopes and the ridge caps laid
 * over the ridge row, and the same two for the building the hero stands in, which fades to
 * {@link #FADE} over {@link #FADE_TIME} seconds as he steps in and comes back as he steps out, so
 * he sees himself and his room while those outside stay hidden under theirs.
 *
 * The building is the hero's room (homestead/Rooms): every roof over its floor and the roofs of
 * the same kinds on the ring of cells round it (its walls and their corners; a terraced
 * neighbour's roof over a shared wall stays). In a doorway it is the room the door closes (the one
 * already faded, between two). Under a roof that closes no room, it is every roof cell joined to
 * his (any kind).
 *
 * Built from the slice's HomesteadStore for the window's origin (world-keyed, so a rebase is a
 * fresh pass over the new window, nothing translated). A roof's look: per connected run of one
 * kind in each column, the ridge on its middle row ((height - 1) / 2 from its top), the shaded
 * slope north of it and the sunward slope from it south, each cell's edges by the 4-neighbour
 * mask of the same roof.
 */
public class RoofLayers {

	/** How much of a faded roof shows. */
	public static final float FADE = 0.15f;
	/** How long a roof takes to fade out or come back, in seconds. */
	public static final float FADE_TIME = 0.2f;

	/** One of the four layers. Public with a no-argument constructor: the level's bundle keeps
	 *  its custom tilemaps by class, and the overworld drops these on reading (they are rebuilt). */
	public static class Tiles extends CustomTilemap {

		{
			texture = RoofTiles.TEXTURE;
		}

		private int[] data;
		private float alpha = 1f;

		void setData( int[] data, int w, int h, boolean live ){
			this.data = data;
			setRect( 0, 0, w, h );
			if (live && vis != null && vis.alive && vis.parent != null) vis.refill( data, w );
		}

		void alpha( float alpha ){
			this.alpha = alpha;
			if (vis != null) vis.alpha( alpha );
		}

		@Override
		public Tilemap create(){
			Tilemap t = create( data != null ? data : new int[tileW * tileH], tileW );
			t.alpha( alpha );
			return t;
		}
	}

	public final Tiles slopes = new Tiles(), ridges = new Tiles(), faded = new Tiles(), fadedRidges = new Tiles();

	//the hero cell and store version the building to fade was last worked out for
	private int builtFor = Integer.MIN_VALUE, builtVersion = -1;
	//whether the window holds any roof, for the store version and origin it was counted for
	private boolean anyRoof;
	private int countedVersion = -1, countedX, countedY;
	//what the faded layers hold and how much of it shows
	private final Fade fade = new Fade();

	/** The four layers, bottom to top. */
	public Tiles[] layers(){
		return new Tiles[]{ slopes, ridges, faded, fadedRidges };
	}

	/** Rebuilds the layers for the window of a slice (its origin and size), fading the building
	 *  over the window cell heroCell (-1: none) at once, with no fade in. live: the tilemaps on
	 *  screen take it at once; otherwise only the data changes, and the next create() (the
	 *  window's art put on screen) shows it. */
	public synchronized void build( OverworldLevel level, HomesteadStore store, int ox, int oy, int w, int h, int heroCell, boolean live ){
		boolean[] hidden = hiddenFor( level, store, ox, oy, w, h, heroCell );
		fade.reset( hidden );
		walls = wallsOf( level, w, h );
		lay( store, ox, oy, w, h, hidden, live );
		builtFor = heroCell;
		builtVersion = store.version();
	}

	/**
	 * Every frame: when the hero has stepped onto another cell or the roofs changed, works out the
	 * building over him again. Stepping into one, it is moved to the faded layers at once (still
	 * showing in full) and fades out; stepping out, it comes back in full first and only then
	 * returns to the plain layers. Within the same building nothing fades again.
	 */
	public synchronized void follow( OverworldLevel level, HomesteadStore store, int ox, int oy, int w, int h, int heroCell ){
		if (heroCell != builtFor || store.version() != builtVersion){
			walls = wallsOf( level, w, h );
			builtFor = heroCell;
			builtVersion = store.version();
			//no roof in the window and none faded: nothing to work out (most steps on a slice)
			if (fade.shown != null || anyRoof( store, ox, oy, w, h )){
				boolean[] hidden = hiddenFor( level, store, ox, oy, w, h, heroCell, fade.shown );
				if (fade.want( hidden )) lay( store, ox, oy, w, h, fade.shown, true );
			}
		}
		if (fade.step( Game.elapsed )) lay( store, ox, oy, w, h, fade.shown, true );
		faded.alpha( fade.alpha );
		fadedRidges.alpha( fade.alpha );
	}

	private boolean anyRoof( HomesteadStore store, int ox, int oy, int w, int h ){
		if (store.version() != countedVersion || ox != countedX || oy != countedY){
			countedVersion = store.version();
			countedX = ox;
			countedY = oy;
			final boolean[] found = { false };
			store.forEachRoof( ox, oy, ox + w, oy + h, (wx, wy, roof) -> found[0] = true );
			anyRoof = found[0];
		}
		return anyRoof;
	}

	//the window's map, for leaving the built walls' tops bare (null: unknown, roofs drawn everywhere)
	private int[] walls;

	private static int[] wallsOf( OverworldLevel level, int w, int h ){
		return level != null && level.width() == w && level.height() == h ? level.map : null;
	}

	private void lay( HomesteadStore store, int ox, int oy, int w, int h, boolean[] hidden, boolean live ){
		int n = w * h;
		int[] s = new int[n], r = new int[n], f = new int[n], fr = new int[n];
		compute( store, ox, oy, w, h, hidden, walls, s, r, f, fr );
		slopes.setData( s, w, h, live );
		ridges.setData( r, w, h, live );
		faded.setData( f, w, h, live );
		fadedRidges.setData( fr, w, h, live );
		faded.alpha( fade.alpha );
		fadedRidges.alpha( fade.alpha );
	}

	/**
	 * The roof cells of the building over a window cell, null for none: in a room (Rooms.at),
	 * every roofed cell of its floor and the roofs of the same kinds on the ring round it (its
	 * walls and their corners); in the door or window of a room, that room (between two rooms the
	 * one `prefer` already holds, the building faded now; null: none); otherwise every roof cell
	 * joined to that one, any kind (connected).
	 */
	public static boolean[] hiddenFor( OverworldLevel level, HomesteadStore store, int ox, int oy, int w, int h, int heroCell, boolean[] prefer ){
		if (heroCell < 0 || heroCell >= w * h) return null;
		boolean rooms = level != null && level.width() == w && level.height() == h;
		Rooms.Room room = rooms ? Rooms.at( level, heroCell ) : null;
		if (room == null && rooms && !Float.isNaN( Rooms.insulationOf( level.map[heroCell] ) )) room = closedBy( level, heroCell, prefer );
		if (room != null){
			boolean[] out = roofOf( room, store, ox, oy, w, h );
			if (out != null) return out;
		}
		return connected( store, ox, oy, w, h, heroCell );
	}

	/** As hiddenFor, with no building faded before. */
	public static boolean[] hiddenFor( OverworldLevel level, HomesteadStore store, int ox, int oy, int w, int h, int heroCell ){
		return hiddenFor( level, store, ox, oy, w, h, heroCell, null );
	}

	//the room a door or window cell closes: the one beside it among its walls, the one already
	//faded when it closes two
	private static Rooms.Room closedBy( OverworldLevel level, int cell, boolean[] prefer ){
		int w = level.width();
		Rooms.Room first = null;
		for (int n : new int[]{ -w, 1, w, -1 }){
			int m = cell + n;
			if (m < 0 || m >= level.length() || (n == 1 && m % w == 0) || (n == -1 && cell % w == 0)) continue;
			Rooms.Room r = Rooms.at( level, m );
			if (r == null || Arrays.binarySearch( r.walls, cell ) < 0) continue;
			if (prefer != null && m < prefer.length && prefer[m]) return r;
			if (first == null) first = r;
		}
		return first;
	}

	//a room's roof: every roof over its floor, and those of the same kinds over the ring round it
	//(its walls and the corners where they meet); null when its floor has none
	private static boolean[] roofOf( Rooms.Room room, HomesteadStore store, int ox, int oy, int w, int h ){
		boolean[] out = new boolean[w * h];
		boolean[] floor = new boolean[w * h];
		java.util.HashSet<Integer> kinds = new java.util.HashSet<>();
		for (int c : room.cells){
			floor[c] = true;
			HomesteadStore.Roof r = store.roofAt( ox + c % w, oy + c / w );
			if (r != null){
				out[c] = true;
				kinds.add( r.kind );
			}
		}
		if (kinds.isEmpty()) return null;
		for (int c : room.cells){
			int x = c % w, y = c / w;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int m = nx + ny * w;
					if (out[m] || floor[m]) continue;
					HomesteadStore.Roof r = store.roofAt( ox + nx, oy + ny );
					if (r != null && kinds.contains( r.kind )) out[m] = true;
				}
			}
		}
		return out;
	}

	/** Every roof cell joined to the window cell heroCell through roofed 4-neighbours, any kind;
	 *  null when it has no roof. */
	public static boolean[] connected( HomesteadStore store, int ox, int oy, int w, int h, int heroCell ){
		int n = w * h;
		if (heroCell < 0 || heroCell >= n || store.roofAt( ox + heroCell % w, oy + heroCell / w ) == null) return null;
		final int[] kinds = kinds( store, ox, oy, w, h );
		boolean[] hidden = new boolean[n];
		ArrayDeque<Integer> open = new ArrayDeque<>();
		hidden[heroCell] = true;
		open.add( heroCell );
		while (!open.isEmpty()){
			int c = open.poll();
			int x = c % w;
			int[] next = { x > 0 ? c - 1 : -1, x < w - 1 ? c + 1 : -1, c - w, c + w };
			for (int m : next){
				if (m < 0 || m >= n || hidden[m] || kinds[m] == -1) continue;
				hidden[m] = true;
				open.add( m );
			}
		}
		return hidden;
	}

	//each window cell's roof kind, -1 for none
	private static int[] kinds( HomesteadStore store, int ox, int oy, int w, int h ){
		final int[] kinds = new int[w * h];
		Arrays.fill( kinds, -1 );
		store.forEachRoof( ox, oy, ox + w, oy + h, (wx, wy, roof) -> kinds[wx - ox + (wy - oy) * w] = roof.kind );
		return kinds;
	}

	/** The plain three-layer answer of the first roofs: the roofs joined to heroCell fade (any
	 *  kind), their ridge caps left out. */
	public static void compute( HomesteadStore store, int ox, int oy, int w, int h, int heroCell,
			int[] slopes, int[] ridges, int[] faded ){
		compute( store, ox, oy, w, h, connected( store, ox, oy, w, h, heroCell ), slopes, ridges, faded, new int[w * h] );
	}

	/**
	 * The four layers' tiles for a window (each -1 where it draws nothing): pure, so a test reads
	 * it. The roofs over the hidden cells (null: none) go to the faded layers, and neither of the
	 * other two draws there.
	 */
	public static void compute( HomesteadStore store, int ox, int oy, int w, int h, boolean[] hidden,
			int[] slopes, int[] ridges, int[] faded, int[] fadedRidges ){
		compute( store, ox, oy, w, h, hidden, null, slopes, ridges, faded, fadedRidges );
	}

	/**
	 * As above, with the window's map (null: none): a roof is not drawn over a built wall, window
	 * or door, so a house's roof sits inset within its walls, the walls' tops showing round it and
	 * its eaves against them. The roof is still there (it still roofs the room); only its picture
	 * stops at the walls.
	 */
	public static void compute( HomesteadStore store, int ox, int oy, int w, int h, boolean[] hidden, int[] map,
			int[] slopes, int[] ridges, int[] faded, int[] fadedRidges ){
		int n = w * h;
		Arrays.fill( slopes, -1 );
		Arrays.fill( ridges, -1 );
		Arrays.fill( faded, -1 );
		Arrays.fill( fadedRidges, -1 );
		final int[] kinds = kinds( store, ox, oy, w, h );
		if (map != null && map.length == n){
			for (int c = 0; c < n; c++){
				int t = map[c];
				if (kinds[c] != -1 && (BuiltTerrain.wall( t ) || BuiltTerrain.window( t ) || BuiltTerrain.door( t ))) kinds[c] = -1;
			}
		}
		if (hidden == null) hidden = new boolean[n];

		int[] ridgeRow = new int[n];
		for (int c = 0; c < n; c++){
			int k = kinds[c];
			if (k == -1) continue;
			int y = c / w;
			int top = y, bottom = y;
			while (top > 0 && kinds[c - (y - top + 1) * w] == k) top--;
			while (bottom < h - 1 && kinds[c + (bottom - y + 1) * w] == k) bottom++;
			ridgeRow[c] = top + (bottom - top) / 2;
		}
		for (int c = 0; c < n; c++){
			int k = kinds[c];
			if (k == -1) continue;
			int x = c % w, y = c / w;
			int mask = 0;
			if (y > 0 && kinds[c - w] == k) mask |= RoofTiles.N;
			if (x < w - 1 && kinds[c + 1] == k) mask |= RoofTiles.E;
			if (y < h - 1 && kinds[c + w] == k) mask |= RoofTiles.S;
			if (x > 0 && kinds[c - 1] == k) mask |= RoofTiles.W;
			int tile = RoofTiles.index( k, mask, y < ridgeRow[c] );
			(hidden[c] ? faded : slopes)[c] = tile;
			if (y == ridgeRow[c]){
				//a ridge stops where its row's roof stops, or where the faded part begins or ends
				int ends = 0;
				if (x == w - 1 || kinds[c + 1] != k || ridgeRow[c + 1] != y || hidden[c + 1] != hidden[c]) ends |= RoofTiles.RIDGE_OPEN_E;
				if (x == 0 || kinds[c - 1] != k || ridgeRow[c - 1] != y || hidden[c - 1] != hidden[c]) ends |= RoofTiles.RIDGE_OPEN_W;
				(hidden[c] ? fadedRidges : ridges)[c] = RoofTiles.ridge( k, ends );
			}
		}
	}

	/**
	 * Which building the faded layers hold and how much of it shows: pure, so a test steps it.
	 * A new building with nothing faded before is taken at once, showing in full, and fades out;
	 * the same building grown or shrunk (a roof put up or taken off over it) is taken at once
	 * as it is; anything else waits until what is faded now has come back in full.
	 */
	public static final class Fade {

		/** The faded layers' alpha. */
		public float alpha = 1f;
		/** The cells the faded layers hold (null: none). */
		public boolean[] shown;
		//what to show once the old building is back in full
		private boolean[] pending;
		private boolean waiting;

		/** Shows `hidden` at once, already faded (a new window: nothing to watch fade). */
		public void reset( boolean[] hidden ){
			shown = any( hidden ) ? hidden : null;
			alpha = shown != null ? FADE : 1f;
			pending = null;
			waiting = false;
		}

		/** The building over the hero is now `hidden`. True when the layers must be laid again
		 *  with {@link #shown} at once. */
		public boolean want( boolean[] hidden ){
			if (!any( hidden )) hidden = null;
			//the same building as shown (or none, and none shown): nothing to lay again
			if (!waiting && (shown == hidden || (shown != null && hidden != null && Arrays.equals( shown, hidden )))) return false;
			if (shown == null){
				shown = hidden;
				alpha = 1f;
				waiting = false;
				return true;
			}
			if (hidden != null && overlap( shown, hidden )){
				shown = hidden;
				waiting = false;
				return true;
			}
			pending = hidden;
			waiting = true;
			return false;
		}

		/** A frame of dt seconds. True when a swap came due: lay the layers again with shown. */
		public boolean step( float dt ){
			float rate = (1f - FADE) / FADE_TIME * dt;
			if (waiting){
				alpha = Math.min( 1f, alpha + rate );
				if (alpha < 1f) return false;
				shown = pending;
				pending = null;
				waiting = false;
				return true;
			}
			float target = shown != null ? FADE : 1f;
			alpha = alpha > target ? Math.max( target, alpha - rate ) : Math.min( target, alpha + rate );
			return false;
		}

		private static boolean any( boolean[] cells ){
			if (cells == null) return false;
			for (boolean b : cells) if (b) return true;
			return false;
		}

		private static boolean overlap( boolean[] a, boolean[] b ){
			for (int i = 0; i < Math.min( a.length, b.length ); i++) if (a[i] && b[i]) return true;
			return false;
		}
	}

	/** Keeps the roofs faded over wherever the local hero stands: a gizmo in the level's visuals
	 *  (OverworldLevel.addVisuals), on the host's screen and a co-op guest's alike. */
	public static class Watcher extends Gizmo {

		private final OverworldLevel level;

		public Watcher( OverworldLevel level ){
			this.level = level;
		}

		@Override
		public void update(){
			super.update();
			if (Dungeon.level == level && Dungeon.hero != null) level.followRoofs( Dungeon.hero.pos );
		}
	}
}
