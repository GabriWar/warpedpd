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

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Derives one window of one world slice: the terrain, what the window needs to
 * know about it (frozen ground, water tiers, the ways between slices) and the
 * dressing. Everything here is a PURE function of (seed, altitude, origin,
 * season snapshot, the player's edits): it never reads game state, so it runs
 * on the preparation worker as happily as on the actor thread, and two windows
 * that overlap always agree on every cell they share.
 *
 * A window is generated in three passes: the rows are sampled in parallel (each
 * cell is independent), then the ways between slices are resolved from the
 * sampled arrays (they need a cell's neighbours), then the water is tiered. The
 * outer ring of every window is solid: the whole engine assumes level borders
 * are impassable, and edge water would send the water stitcher out of bounds.
 */
public final class WindowGenerator {

	public static final int WIDTH  = OverworldLevel.WIDTH;
	public static final int HEIGHT = OverworldLevel.HEIGHT;

	//the ways between the slices, as the dressing draws them (Window.link)
	public static final byte LINK_NONE = 0;
	public static final byte LINK_MOUTH = 1;       //surface: a cave mouth, down to -1
	public static final byte LINK_CAVE_EXIT = 2;   //-1: the mouth seen from below, up to the surface
	public static final byte LINK_STAIR_UP = 3;    //surface and mountains: steps up the cliff face
	public static final byte LINK_STAIR_DOWN = 4;  //mountains: the same steps, from the top
	public static final byte LINK_LADDER_UP = 5;   //caves: a ladder up a shaft to the slice above
	public static final byte LINK_LADDER_DOWN = 6; //caves: the top of that shaft

	/**
	 * A generated window: the terrain plus, per cell, whether the ground is
	 * frozen (the tile art needs it for what grows there), the water's depth
	 * tier (1 = the wadeable shelf, 2+ = DEEP_WATER, shaded by tier) and the
	 * kind of way between slices standing there (LINK_*, for the dressing).
	 */
	public static class Window {
		public int altitude;
		public int[] terrain;
		public boolean[] frozen;
		public byte[] waterDepth;
		public byte[] link;
		//a human village's field (WorldStructures.fieldPlots) ploughed on this cell
		public boolean[] field;
		public float shift;   //the seasonal snapshot it was derived for
		//the slices only (null on the surface): the natural rock of each cell (Ores.naturalRock),
		//recorded by the row pass before anything is built or cut into it, and the ore veins in
		//it (Ores.veins: metal ordinal + 1, 0 for none), worked out here, off the render thread
		public boolean[] rock;
		public byte[] veins;
		//the places of the slice laid into this window (CaveSites.Site below the surface,
		//MountainSites.Site above it, in its own order), resolved once by the generator and never
		//changed: an immutable list, empty when there are none
		public List<Object> sites = java.util.Collections.emptyList();
		//what the slice below keeps sealed under this window (a tomb's box: x0, y0, x1, y1 each),
		//where nobody digs down (OverworldLevel.sealedBelow)
		public int[] sealedBelow = new int[0];
		//a mountain slice's: every cell's band (WorldLayers.band) and elevation
		public byte[] band;
		public float[] elev;
		//caves at LayerHazards.FIREDAMP_TOP and below: the firedamp pockets (LayerHazards.gassy), null elsewhere
		public boolean[] firedamp;
		//a mountain slice's view down (BelowView): per chasm cell, the tile of the ground it falls
		//to (-1 elsewhere) and how many bands down that ground lies; null on the other slices
		public int[] below;
		public byte[] belowDepth;
		//...and that ground's terrain and frost, for the dressing's blends over the view (Views)
		public int[] belowTerrain;
		public boolean[] belowFrozen;
		//the open-sky slices: per cell of rock above the slice, the tile its top shows (its own
		//band's ground, viewTile; a built wall's the slice's own ground; -1 elsewhere) and whether
		//its scarp is earth rather than stone (WorldModel.earthenScarp); null in the caves
		public int[] top;
		public boolean[] earth;
		//the top's terrain and frost, and the dressing's blends and corner roundings over the tops
		//(overworld_dress tiles, viewBlends), so a tarn's ice and the snow line stitch up there as
		//they do underfoot
		public int[] topTerrain;
		public boolean[] topFrozen;
		public int[] topBlend, topCorner;
	}

	/**
	 * A band's ground seen from another slice, as window-sized layers (-1: nothing): the ground's
	 * tiles (tiles_overworld), then the dressing's blends and corner roundings over them
	 * (overworld_dress), the view stitched as the ground underfoot is.
	 */
	public static final class View {
		public final int[] tiles, blends, corners;
		View( int n ){
			tiles = new int[n];
			blends = new int[n];
			corners = new int[n];
			java.util.Arrays.fill( tiles, -1 );
			java.util.Arrays.fill( blends, -1 );
			java.util.Arrays.fill( corners, -1 );
		}
	}

	/** The view down from a mountain slice: the bands below, nearest first (BELOW_LAYERS), and the
	 *  shade the drops' rims cast over them all. */
	public static final class Views {
		public final View[] depth;
		public final int[] shade;
		Views( int depths, int n ){
			depth = new View[depths];
			for (int i = 0; i < depths; i++) depth[i] = new View( n );
			shade = new int[n];
			java.util.Arrays.fill( shade, -1 );
		}
	}

	/** Everything a rebase needs that does not depend on live game state. */
	public static final class Prepared {
		public Window base;
		public int ox, oy;
		public GameCalendar.Season season;
		public int diffsVersion;
		//the fallen stars whose scorch is laid on map (OverworldLevel.scorchSig), 0 for none
		public long scorchSig;
		public int[] map;          //pristine plus the player's edits and the fallen stars' scorch
		public int[][] dress;      //ground, village ground, canopy, village canopy
		public byte[] variance;    //alt-art variance, anchored to world coordinates
		public Views below;        //a mountain slice's view down (belowLayers)
	}

	private WindowGenerator(){}

	// ------------------------------------------------------------ workers

	//the rows of a window are independent, so they are sampled on a small pool;
	//the generating thread joins them and runs the neighbour passes itself
	private static final int WORKERS = Math.max( 1, Math.min( 4, Runtime.getRuntime().availableProcessors() - 1 ) );
	private static final ExecutorService POOL = Executors.newFixedThreadPool( WORKERS, r -> {
		Thread t = new Thread( r, "ow-gen" );
		t.setDaemon( true );
		t.setPriority( Thread.MIN_PRIORITY );
		return t;
	} );

	private interface Rows {
		void run( int y0, int y1 );
	}

	private static void forRows( Rows work ){
		if (WORKERS == 1){
			work.run( 0, HEIGHT );
			return;
		}
		List<Future<?>> parts = new ArrayList<>( WORKERS );
		int band = (HEIGHT + WORKERS - 1) / WORKERS;
		for (int i = 0; i < WORKERS; i++){
			final int y0 = i * band, y1 = Math.min( HEIGHT, y0 + band );
			if (y0 >= y1) break;
			parts.add( POOL.submit( () -> work.run( y0, y1 ) ) );
		}
		try {
			for (Future<?> f : parts) f.get();
		} catch (Exception e){
			throw new RuntimeException( "window generation failed", e );
		}
	}

	// ----------------------------------------------------------- the passes

	/** One full generator pass for a slice and an origin, at the current seasonal snapshot. */
	public static Window generate( long seed, int altitude, int ox, int oy ){
		return generate( seed, altitude, ox, oy, WorldModel.seasonShift() );
	}

	/** ...at an explicit seasonal snapshot: the pure core. */
	public static Window generate( long seed, int altitude, int ox, int oy, float shift ){
		Window w = new Window();
		w.altitude = altitude;
		w.shift = shift;
		w.terrain = new int[WIDTH * HEIGHT];
		w.frozen = new boolean[WIDTH * HEIGHT];
		w.waterDepth = new byte[WIDTH * HEIGHT];
		w.link = new byte[WIDTH * HEIGHT];
		w.field = new boolean[WIDTH * HEIGHT];
		if (altitude != 0) w.rock = new boolean[WIDTH * HEIGHT];
		w.firedamp = altitude <= LayerHazards.FIREDAMP_TOP ? new boolean[WIDTH * HEIGHT] : null;
		if (altitude == 0){
			surface( seed, ox, oy, shift, w );
		} else if (altitude > 0){
			mountain( seed, altitude, ox, oy, shift, w );
		} else {
			caves( seed, altitude, ox, oy, w );
		}
		//the ring goes solid before the water is tiered: it is shore, so the
		//water along it stays the wadeable shelf
		for (int x = 0; x < WIDTH; x++){
			w.terrain[x] = w.terrain[x + (HEIGHT-1) * WIDTH] = Terrain.WALL;
		}
		for (int y = 0; y < HEIGHT; y++){
			w.terrain[y * WIDTH] = w.terrain[WIDTH-1 + y * WIDTH] = Terrain.WALL;
		}
		tierWater( w );
		if (altitude != 0){
			w.veins = new byte[WIDTH * HEIGHT];
			Ores.veins( seed, altitude, ox, oy, WIDTH, HEIGHT, w.rock, w.veins );
			//an ore vein runs in rock: its face is stone whatever lies over it
			if (w.earth != null){
				for (int c = 0; c < w.earth.length; c++) if (w.veins[c] != 0) w.earth[c] = false;
			}
		}
		return w;
	}

	//the surface: the biomes and the civilization layer, then the cave
	//mouths and the foot of every mountain stair
	private static void surface( long seed, int ox, int oy, float shift, Window w ){
		final byte[] band = new byte[WIDTH * HEIGHT];
		final boolean[] structured = new boolean[WIDTH * HEIGHT];
		final float[] soil = rockTops( w );
		final WorldStructures.SectorView view = WorldStructures.view( seed, ox, oy, ox + WIDTH, oy + HEIGHT );
		forRows( (y0, y1) -> {
			WorldModel.Sample smp = new WorldModel.Sample();
			for (int y = y0; y < y1; y++){
				for (int x = 0; x < WIDTH; x++){
					int cell = x + y * WIDTH;
					int wx = ox + x, wy = oy + y;
					WorldModel.sample( seed, wx, wy, shift, smp );
					//the ring is sampled too: the stair rule below reads its
					//neighbours' bands, and a cell must get the same answer
					//whichever window it is seen through
					band[cell] = (byte) WorldLayers.band( smp.elev );
					int wild = WorldModel.wildTerrain( seed, wx, wy, smp );
					int structure = WorldStructures.terrainAt( seed, wx, wy, wild, view );
					structured[cell] = structure != -1;
					int t = structure != -1 ? structure : wild;
					//the mountains are cut through by tunnels: a sheltered floor, never snowed
					boolean tunnel = structure == -1 && t == Terrain.WALL && WorldModel.tunnelAt( seed, wx, wy, 0 );
					if (tunnel) t = Terrain.DIRT_PATH;
					w.terrain[cell] = t;
					if (structure == -1 && t == Terrain.WALL && band[cell] >= 1){
						rockTop( seed, wx, wy, band[cell], smp, w, soil, cell );
					}
					w.field[cell] = structure == Terrain.FURROWED_GRASS;
					w.frozen[cell] = !tunnel && smp.temperature < WorldModel.FREEZE;
					if (t == Terrain.WATER) w.waterDepth[cell] = (byte) WorldModel.waterTier( smp.elev );
				}
			}
		} );
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int cell = x + y * WIDTH;
				int wx = ox + x, wy = oy + y;
				if (band[cell] == 1 && footOf( band, cell, 0 ) && stairHash( seed, wx, wy, 0 )){
					w.terrain[cell] = Terrain.ENTRANCE;
					w.link[cell] = LINK_STAIR_UP;
					clearFoot( w.terrain, band, cell, 0 );
				} else if (!structured[cell] && WorldStructures.townCell( wx, wy ) == -1
						&& mouthAt( seed, wx, wy )){
					w.terrain[cell] = Terrain.EXIT;
					w.link[cell] = LINK_MOUTH;
				}
			}
		}
		scarps( seed, ox, oy, 0, band, soil, w );
		topViews( seed, ox, oy, w );
		//a stair's foot cleared over a field leaves no field there
		for (int cell = 0; cell < w.field.length; cell++){
			if (w.field[cell] && w.terrain[cell] != Terrain.FURROWED_GRASS) w.field[cell] = false;
		}
	}

	//a mountain slice: the cells of its band are ground, the bands above it
	//rock, the bands below it open air; stairs where a cliff rises one band
	private static void mountain( long seed, int altitude, int ox, int oy, float shift, Window w ){
		final byte[] band = new byte[WIDTH * HEIGHT];
		final float[] elev = new float[WIDTH * HEIGHT];
		w.band = band;
		w.elev = elev;
		w.below = new int[WIDTH * HEIGHT];
		w.belowDepth = new byte[WIDTH * HEIGHT];
		w.belowTerrain = new int[WIDTH * HEIGHT];
		w.belowFrozen = new boolean[WIDTH * HEIGHT];
		java.util.Arrays.fill( w.below, -1 );
		final float[] soil = rockTops( w );
		forRows( (y0, y1) -> {
			WorldModel.Sample smp = new WorldModel.Sample();
			for (int y = y0; y < y1; y++){
				for (int x = 0; x < WIDTH; x++){
					int cell = x + y * WIDTH;
					int wx = ox + x, wy = oy + y;
					WorldModel.sample( seed, wx, wy, shift, smp );
					int b = WorldLayers.band( smp.elev );
					band[cell] = (byte) b;
					elev[cell] = smp.elev;
					w.frozen[cell] = WorldModel.alpineTemperature( smp, altitude ) < WorldModel.FREEZE;
					if (b < altitude){
						w.terrain[cell] = Terrain.CHASM;
						//the ground this air falls to, as it looks from up here: its own band's
						//ground (the surface's wilderness at band 0), frozen or not by that band
						boolean frost;
						int t;
						if (b >= 1){
							t = WorldModel.alpineTerrain( seed, wx, wy, b, smp );
							frost = WorldModel.alpineTemperature( smp, b ) < WorldModel.FREEZE;
						} else {
							t = WorldModel.wildTerrain( seed, wx, wy, smp );
							frost = smp.temperature < WorldModel.FREEZE;
						}
						w.below[cell] = viewTile( t, frost );
						w.belowDepth[cell] = (byte) Math.min( 127, altitude - b );
						w.belowTerrain[cell] = t;
						w.belowFrozen[cell] = frost;
					} else if (b > altitude && WorldModel.tunnelAt( seed, wx, wy, altitude )){
						//a tunnel through the rock of the higher bands: a sheltered floor
						w.terrain[cell] = Terrain.DIRT_PATH;
						w.frozen[cell] = false;
					} else if (b > altitude){
						w.terrain[cell] = Terrain.WALL;
						w.rock[cell] = true;
						rockTop( seed, wx, wy, b, smp, w, soil, cell );
					} else {
						int t = WorldModel.alpineTerrain( seed, wx, wy, altitude, smp );
						w.terrain[cell] = t;
						if (t == Terrain.WATER) w.waterDepth[cell] = (byte) WorldModel.tarnTier( seed, wx, wy, altitude );
					}
				}
			}
		} );
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int cell = x + y * WIDTH;
				int wx = ox + x, wy = oy + y;
				if (band[cell] == altitude + 1 && footOf( band, cell, altitude ) && stairHash( seed, wx, wy, altitude )){
					w.terrain[cell] = Terrain.ENTRANCE;
					w.link[cell] = LINK_STAIR_UP;
					clearFoot( w.terrain, band, cell, altitude );
				} else if (band[cell] == altitude && footOf( band, cell, altitude - 1 ) && stairHash( seed, wx, wy, altitude - 1 )){
					w.terrain[cell] = Terrain.EXIT;
					w.link[cell] = LINK_STAIR_DOWN;
					clearFoot( w.terrain, band, cell, altitude );
				}
			}
		}
		scarps( seed, ox, oy, altitude, band, soil, w );
		//the places on the mountains, on the slice's own ground clear of every way: last
		MountainSites.lay( seed, altitude, ox, oy, w );
		builtTops( seed, altitude, ox, oy, shift, w );
		topViews( seed, ox, oy, w );
	}

	//a cave slice: chambers, tunnels, pools and seams; then the ways up and
	//down where a chamber runs on into the slice above or below
	private static void caves( long seed, int altitude, int ox, int oy, Window w ){
		final byte[] bits = new byte[WIDTH * HEIGHT];   //1 open, 2 open above, 4 open below
		final boolean above = altitude + 1 < 0;
		final boolean below = WorldLayers.exists( altitude - 1 );
		forRows( (y0, y1) -> {
			WorldModel.CaveSample cs = new WorldModel.CaveSample();
			for (int y = y0; y < y1; y++){
				for (int x = 0; x < WIDTH; x++){
					int cell = x + y * WIDTH;
					int wx = ox + x, wy = oy + y;
					WorldModel.caveSample( seed, wx, wy, altitude, cs );
					w.rock[cell] = !cs.open;
					int t = WorldModel.caveTerrain( seed, wx, wy, altitude, cs );
					//the surface's lakes and seas reach down into the rock: a
					//water body of tier t on the surface floods the t-1 slices
					//under it (WorldModel.waterTier), whatever the cave field says
					int surfaceTier = -altitude < WorldModel.WATER_TIERS ? surfaceWaterTier( seed, wx, wy ) : 0;
					if (surfaceTier > -altitude) t = Terrain.WATER;
					w.terrain[cell] = t;
					if (w.firedamp != null && LayerHazards.gassy( seed, wx, wy, altitude, cs, t )) w.firedamp[cell] = true;
					if (t == Terrain.WATER){
						//how many slices the water goes on down: the pool's depth, and its shade
						int depth = 1;
						while (depth < WorldModel.WATER_TIERS && WorldLayers.exists( altitude - depth )
								&& (surfaceTier > depth - altitude
									|| WorldModel.caveWet( seed, wx, wy, altitude - depth ))){
							depth++;
						}
						w.waterDepth[cell] = (byte) depth;
					}
					if (!cs.open) continue;
					int b = 1;
					if (above && WorldModel.caveOpen( seed, wx, wy, altitude + 1 )) b |= 2;
					if (below && WorldModel.caveOpen( seed, wx, wy, altitude - 1 )) b |= 4;
					bits[cell] = (byte) b;
				}
			}
		} );
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int cell = x + y * WIDTH;
				if ((bits[cell] & 1) == 0) continue;
				int wx = ox + x, wy = oy + y;
				if (altitude == -1 && mouthAt( seed, wx, wy )){
					w.terrain[cell] = Terrain.ENTRANCE;
					w.link[cell] = LINK_CAVE_EXIT;
					continue;
				}
				boolean openAbove = (bits[cell] & 2) != 0, openBelow = (bits[cell] & 4) != 0;
				if (openBelow && caveStair( seed, wx, wy, altitude - 1, true, true )){
					w.terrain[cell] = Terrain.EXIT;
					w.link[cell] = LINK_LADDER_DOWN;
				} else if (openAbove && caveStair( seed, wx, wy, altitude, true, true )){
					w.terrain[cell] = Terrain.ENTRANCE;
					w.link[cell] = LINK_LADDER_UP;
				} else if (openBelow && pitHash( seed, wx, wy, altitude )){
					w.terrain[cell] = Terrain.CHASM;
				} else {
					continue;
				}
				if (w.firedamp != null) w.firedamp[cell] = false;
			}
		}
		//the places of the slice (an old mine, a grotto, a camp...), laid over the natural cave
		//after its ways, which they never touch; and the shafts the mines above sink into it
		w.sites = CaveSites.overlay( seed, altitude, ox, oy, w );
		//no gas about a way between slices or a place (LayerHazards.clearMask)
		LayerHazards.clearMask( w, seed, altitude, ox, oy );
	}

	/** The natural terrain of one cave cell, before the ways between slices and the places
	 *  laid over it: caves() for a single cell (the cave field, then the surface's water reaching
	 *  down), so the places can be judged against the world without a window. */
	static int naturalCaveTerrain( long seed, int altitude, int wx, int wy ){
		WorldModel.CaveSample cs = WorldModel.caveSample( seed, wx, wy, altitude, null );
		int t = WorldModel.caveTerrain( seed, wx, wy, altitude, cs );
		int surfaceTier = -altitude < WorldModel.WATER_TIERS ? surfaceWaterTier( seed, wx, wy ) : 0;
		return surfaceTier > -altitude ? Terrain.WATER : t;
	}

	/** Does caves() put a way between slices on this cell (a cave exit, a ladder up or down, a
	 *  pit)? The second pass of caves() for a single cell, the cheap hashes asked first. */
	static boolean wayAt( long seed, int altitude, int wx, int wy ){
		if (altitude >= 0 || !WorldLayers.exists( altitude )) return false;
		boolean below = WorldLayers.exists( altitude - 1 ), above = altitude + 1 < 0;
		boolean mouth = altitude == -1 && Math.floorMod( WorldModel.linkHash( seed ^ 0x300DL, wx, wy, 0 ), 900 ) == 0;
		boolean down = below && ladderHash( seed, wx, wy, altitude - 1 );
		boolean up = above && ladderHash( seed, wx, wy, altitude );
		boolean pit = below && pitHash( seed, wx, wy, altitude );
		if (!mouth && !down && !up && !pit) return false;
		if (!WorldModel.caveOpen( seed, wx, wy, altitude )) return false;
		if (mouth && mouthAt( seed, wx, wy )) return true;
		boolean openBelow = below && WorldModel.caveOpen( seed, wx, wy, altitude - 1 );
		boolean openAbove = above && WorldModel.caveOpen( seed, wx, wy, altitude + 1 );
		if (openBelow && caveStair( seed, wx, wy, altitude - 1, true, true )) return true;
		if (openAbove && caveStair( seed, wx, wy, altitude, true, true )) return true;
		return openBelow && pit;
	}

	/** Does a pit of the slice above open over this cell (so a fall lands on it)? */
	static boolean pitFromAbove( long seed, int altitude, int wx, int wy ){
		int up = altitude + 1;
		return up < 0 && pitHash( seed, wx, wy, up )
				&& WorldModel.caveOpen( seed, wx, wy, up ) && WorldModel.caveOpen( seed, wx, wy, altitude );
	}

	//the tier of the surface's open water above a cave column, 0 when the
	//surface is land (or ice) there: the annual mean decides, like the mouths
	static int surfaceWaterTier( long seed, int wx, int wy ){
		WorldModel.Sample s = WorldModel.sample( seed, wx, wy, 0f, null );
		if (s.elev >= WorldModel.SEA) return 0;
		int wild = WorldModel.wildTerrain( seed, wx, wy, s );
		if (wild != Terrain.WATER) return 0;
		if (WorldStructures.terrainAt( seed, wx, wy, wild ) != -1) return 0;
		return WorldModel.waterTier( s.elev );
	}

	// ------------------------------------------------------ the ways between

	//the stair on (wx, wy) between slice `lower` and the one above it. one in
	//fourteen cliff cells (a cell one band up from a 4-neighbour) carries one
	static boolean stairHash( long seed, int wx, int wy, int lower ){
		return Math.floorMod( WorldModel.linkHash( seed ^ 0x57A1BL, wx, wy, lower ), 14 ) == 0;
	}

	//is any 4-neighbour of this cell ground of the given band?
	private static boolean footOf( byte[] band, int cell, int b ){
		return band[cell-1] == b || band[cell+1] == b || band[cell-WIDTH] == b || band[cell+WIDTH] == b;
	}

	//the ground a stair is climbed from must be walkable: whatever grew or
	//lay on the 4-neighbours of its own band is cleared (never water)
	private static void clearFoot( int[] terrain, byte[] band, int cell, int b ){
		for (int n : new int[]{ cell-1, cell+1, cell-WIDTH, cell+WIDTH }){
			if (band[n] != b) continue;
			int t = terrain[n];
			if ((Terrain.flags[t] & Terrain.SOLID) != 0 && (Terrain.flags[t] & Terrain.LIQUID) == 0
					&& t != Terrain.DOOR && t != Terrain.TOWN_SOLID){
				terrain[n] = Terrain.EMPTY;
			}
		}
	}

	/**
	 * The cave stair on (wx, wy) between slice `lower` (negative) and the one
	 * above it exists when both are open there, the hash says so, and the same
	 * cell does not already carry the stair from the slice below (that one
	 * wins, so no cell is both a way up and a way down). `openHere` and
	 * `openAbove` are what the caller already knows; the slice below is asked
	 * only when the hash makes it matter.
	 */
	private static boolean caveStair( long seed, int wx, int wy, int lower, boolean openHere, boolean openAbove ){
		if (lower + 1 >= 0 || !WorldLayers.exists( lower )) return false;
		if (!ladderHash( seed, wx, wy, lower )) return false;
		if (!openHere || !openAbove) return false;
		int under = lower - 1;
		return !(WorldLayers.exists( under ) && ladderHash( seed, wx, wy, under )
				&& WorldModel.caveOpen( seed, wx, wy, under ));
	}

	//chambers overlap over whole areas, not along a line like a cliff: one
	//open-over-open cell in 220 carries a ladder
	static boolean ladderHash( long seed, int wx, int wy, int lower ){
		return Math.floorMod( WorldModel.linkHash( seed ^ 0x1ADDE4L, wx, wy, lower ), 220 ) == 0;
	}

	//a hole in a cave floor onto the slice below: one open-over-open cell in 500
	static boolean pitHash( long seed, int wx, int wy, int altitude ){
		return Math.floorMod( WorldModel.linkHash( seed ^ 0x9177L, wx, wy, altitude ), 500 ) == 0;
	}

	//the ground a cave mouth may open in: plain walkable land, no road, no water
	private static boolean plainGround( int t ){
		switch (t){
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: case Terrain.EMPTY_SP:
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.SNOW:
			case Terrain.FLOWER_PATCH: case Terrain.MUSHROOM_PATCH:
				return true;
			default:
				return false;
		}
	}

	/**
	 * A cave mouth on (wx, wy): one cell in 900 of plain ground with a chamber
	 * right under it. The ground is judged at the ANNUAL MEAN of the seasons and
	 * without the structures' say (roads and houses are checked by the surface
	 * pass), so the surface and the slice under it always agree on where the
	 * mouths are, whatever point of the year each was generated at.
	 */
	public static boolean mouthAt( long seed, int wx, int wy ){
		if (Math.floorMod( WorldModel.linkHash( seed ^ 0x300DL, wx, wy, 0 ), 900 ) != 0) return false;
		if (WorldModel.townInfluence( seed, wx, wy, WorldModel.TOWN_LAND ) > 0f) return false;
		if (!WorldModel.caveOpen( seed, wx, wy, -1 )) return false;
		int wild = WorldModel.wildTerrain( seed, wx, wy, WorldModel.sample( seed, wx, wy, 0f, null ) );
		if (!plainGround( wild )) return false;
		return WorldStructures.terrainAt( seed, wx, wy, wild ) == -1;
	}

	// --------------------------------------------------------------- water

	//the depth tiers came from the ground under the water; the first cell off
	//any shore is always the wadeable shelf, whatever the ground says (ice, a
	//bridge and every kind of land count as shore), and DEEP_WATER is tier 2+
	private static void tierWater( Window w ){
		int[] t = w.terrain;
		byte[] d = w.waterDepth;
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int c = x + y * WIDTH;
				if (t[c] != Terrain.WATER) continue;
				if (d[c] < 1) d[c] = 1;
				if (d[c] >= 2){
					for (int dy = -1; dy <= 1 && d[c] >= 2; dy++){
						for (int dx = -1; dx <= 1; dx++){
							int n = c + dx + dy * WIDTH;
							if (t[n] != Terrain.WATER){ d[c] = 1; break; }
						}
					}
				}
			}
		}
		for (int c = 0; c < t.length; c++){
			if (t[c] == Terrain.WATER && d[c] >= 2) t[c] = Terrain.DEEP_WATER;
		}
	}

	// ------------------------------------------------------------ the rock's tops

	//allocates a window's rock tops (none yet) and the scratch of the soil over them
	private static float[] rockTops( Window w ){
		int n = WIDTH * HEIGHT;
		w.top = new int[n];
		w.earth = new boolean[n];
		w.topTerrain = new int[n];
		w.topFrozen = new boolean[n];
		w.topBlend = new int[n];
		w.topCorner = new int[n];
		java.util.Arrays.fill( w.top, -1 );
		java.util.Arrays.fill( w.topTerrain, -1 );
		java.util.Arrays.fill( w.topBlend, -1 );
		java.util.Arrays.fill( w.topCorner, -1 );
		return new float[n];
	}

	//a cell of natural rock rising to band b: the ground its top shows (that band's own ground,
	//frozen or not by that band) and how deep the soil lies on it
	private static void rockTop( long seed, int wx, int wy, int b, WorldModel.Sample smp,
	                             Window w, float[] soil, int cell ){
		int t = WorldModel.alpineTerrain( seed, wx, wy, b, smp );
		boolean frost = WorldModel.alpineTemperature( smp, b ) < WorldModel.FREEZE;
		w.top[cell] = viewTile( t, frost );
		w.topTerrain[cell] = t;
		w.topFrozen[cell] = frost;
		soil[cell] = WorldModel.soilDepth( seed, wx, wy, b, t, smp );
	}

	//the walls a mountain's places build (MountainSites) stand under the open sky too: their
	//roof is the slice's own ground, snowed under or grown over like everything round them,
	//instead of the dungeon's black
	private static void builtTops( long seed, int altitude, int ox, int oy, float shift, Window w ){
		WorldModel.Sample smp = new WorldModel.Sample();
		for (int y = 1; y < HEIGHT - 1; y++){
			for (int x = 1; x < WIDTH - 1; x++){
				int cell = x + y * WIDTH;
				if (w.terrain[cell] != Terrain.WALL || w.top[cell] != -1) continue;
				int wx = ox + x, wy = oy + y;
				WorldModel.sample( seed, wx, wy, shift, smp );
				int t = WorldModel.alpineTerrain( seed, wx, wy, altitude, smp );
				w.top[cell] = viewTile( t, w.frozen[cell] );
				w.topTerrain[cell] = t;
				w.topFrozen[cell] = w.frozen[cell];
			}
		}
	}

	//the dressing over the tops that are drawn (rock with rock in front of it, the rest shows its
	//face): the blends between their grounds, the snow lapping onto a tarn's ice
	private static void topViews( long seed, int ox, int oy, Window w ){
		int n = WIDTH * HEIGHT;
		int[] vt = new int[n];
		java.util.Arrays.fill( vt, -1 );
		for (int c = 0; c + WIDTH < n; c++){
			if (w.top[c] != -1 && xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.wallStitcheable( w.terrain[c + WIDTH] )){
				vt[c] = w.topTerrain[c];
			}
		}
		viewBlends( seed, ox, oy, vt, w.topFrozen, w.topBlend, w.topCorner );
	}

	/**
	 * The dressing's blends over a view of another band (View): on every cell of `terrain` (-1
	 * where the view has none) the stronger grounds beside it lap over its edges, as dress() lays
	 * them underfoot - the same hash, so a cell's edge is the same piece seen from any slice.
	 */
	static void viewBlends( long seed, int ox, int oy, int[] terrain, boolean[] frozen, int[] blends, int[] corners ){
		for (int y = 1; y < HEIGHT - 1; y++){
			for (int x = 1; x < WIDTH - 1; x++){
				int cell = x + y * WIDTH;
				if (terrain[cell] < 0) continue;
				int f = familyOf( terrain, frozen, null, cell );
				if (f < 0) continue;
				blendCell( terrain, frozen, null, cell, f, edgeVersion( seed, ox + x, oy + y, OverworldDress.BIOME_VARIANTS ), blends, corners );
			}
		}
	}

	//what each rock cell's scarp is made of, once every band is known: its height is from its
	//top down to the slice's ground, or further where open air lies at its foot
	private static void scarps( long seed, int ox, int oy, int altitude, byte[] band, float[] soil, Window w ){
		for (int cell = 0; cell < WIDTH * HEIGHT; cell++){
			if (w.top[cell] == -1) continue;
			int foot = altitude;
			if (cell + WIDTH < WIDTH * HEIGHT) foot = Math.min( foot, band[cell + WIDTH] );
			w.earth[cell] = WorldModel.earthenScarp( seed, ox + cell % WIDTH, oy + cell / WIDTH,
					soil[cell], band[cell] - foot, false );
		}
	}

	// --------------------------------------------------------- the view down

	/** How many depth layers the view down from a mountain is drawn in (1 band down, 2, 3 and more):
	 *  each its own tilemap, hazed by its own depth (GameScene.addBelow). belowLayers hands one more
	 *  after them: the shade the rims cast into the drop, drawn over them all. */
	public static final int BELOW_LAYERS = 3;

	/** The overworld tile this terrain's ground shows from another band, above or below it: its
	 *  colour, not its detail. */
	static int viewTile( int terrain, boolean frozen ){
		switch (terrain){
			case Terrain.WATER: case Terrain.DEEP_WATER:
				return xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.BELOW_WATER;
			case Terrain.FROZEN_WATER:
				return xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.FROZEN_WATER;
			case Terrain.SNOW:
				return xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.SNOW_TILE;
			case Terrain.EMPTY_SP:
				return xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.FLOOR_SP;
			case Terrain.DIRT_PATH:
				return xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.DIRT_PATH_TILE;
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
			case Terrain.FLOWER_PATCH: case Terrain.SHRUB:
				return xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.GRASS;
			case Terrain.TREE_PINE: case Terrain.TREE_OAK: case Terrain.BOULDER:
				//the ground they stand on
				return frozen ? xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.SNOW_TILE
						: xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.GRASS;
			default:
				return frozen ? xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.SNOW_TILE
						: xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.FLOOR;
		}
	}

	//the eight neighbours in the order of DungeonTileSheet.belowShade's mask bits
	private static final int[] RIM_DX = { 0, 1, 1, 1, 0, -1, -1, -1 };
	private static final int[] RIM_DY = { -1, -1, 0, 1, 1, 1, 0, -1 };

	//which depth layer a drop's ground is drawn in
	private static int belowGroup( Window w, int cell ){
		return Math.min( BELOW_LAYERS, Math.max( 1, w.belowDepth[cell] ) ) - 1;
	}

	/**
	 * A mountain window's view down split into its depth layers (BELOW_LAYERS: one band down, two,
	 * three and more), each the ground's tiles with the dressing's blends over them (View), and
	 * the rims' shade; null off the mountains. Pure: the window worker runs it.
	 */
	public static Views belowLayers( long seed, int ox, int oy, Window w ){
		if (w == null || w.below == null) return null;
		int n = WIDTH * HEIGHT;
		Views out = new Views( BELOW_LAYERS, n );
		int[] shade = out.shade;
		for (int c = 0; c < n; c++){
			if (w.below[c] < 0) continue;
			int g = belowGroup( w, c );
			out.depth[g].tiles[c] = w.below[c];
			//the rims: the neighbours that stand higher than this drop - the slice's own ground,
			//or a nearer layer's ledge over a deeper one (the window's edge is neither)
			int x = c % WIDTH, y = c / WIDTH, mask = 0;
			for (int i = 0; i < 8; i++){
				int nx = x + RIM_DX[i], ny = y + RIM_DY[i];
				if (nx < 0 || ny < 0 || nx >= WIDTH || ny >= HEIGHT) continue;
				int nc = nx + ny * WIDTH;
				if (w.below[nc] < 0 || belowGroup( w, nc ) < g) mask |= 1 << i;
			}
			if (mask != 0) shade[c] = xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.belowShade( mask );
		}
		//each depth's blends over its own cells alone: a band's ground never laps over a drop
		int[] vt = new int[n];
		for (int g = 0; g < BELOW_LAYERS; g++){
			java.util.Arrays.fill( vt, -1 );
			for (int c = 0; c < n; c++){
				if (out.depth[g].tiles[c] != -1) vt[c] = w.belowTerrain[c];
			}
			viewBlends( seed, ox, oy, vt, w.belowFrozen, out.depth[g].blends, out.depth[g].corners );
		}
		return out;
	}

	// --------------------------------------------------------- preparation

	/** The terrain, the player's edits and the fallen stars' scorch over it, the dressing and the
	 *  variance for one origin. `scorch` is the stars' cells in this window (world key -> terrain,
	 *  or null) and `scorchSig` what they are known by. */
	public static Prepared prepare( long seed, int altitude, int ox, int oy, GameCalendar.Season season,
			HashMap<Long, Integer> edits, int version, HashMap<Long, Integer> scorch, long scorchSig ){
		Prepared p = new Prepared();
		p.base = generate( seed, altitude, ox, oy );
		p.ox = ox;
		p.oy = oy;
		p.season = season;
		p.diffsVersion = version;
		p.scorchSig = scorchSig;
		p.map = p.base.terrain.clone();
		overlayDiffs( p.map, edits, ox, oy );
		overlayScorch( p.map, p.base.terrain, p.base.link, scorch, edits, ox, oy );
		p.dress = dress( seed, ox, oy, p.map, p.base, season );
		p.below = belowLayers( seed, ox, oy, p.base );
		p.variance = variance( seed, ox, oy );
		return p;
	}

	/** The player's recorded edits laid over a window map: only the interior, the ring is always the window's own wall. */
	public static void overlayDiffs( int[] map, HashMap<Long, Integer> edits, int ox, int oy ){
		if (edits == null || edits.isEmpty()) return;
		for (HashMap.Entry<Long, Integer> e : edits.entrySet()){
			int wx = (int) (e.getKey() & 0xFFFFFFFFL);
			int wy = (int) (e.getKey() >> 32);
			int x = wx - ox, y = wy - oy;
			if (x > 0 && y > 0 && x < WIDTH - 1 && y < HEIGHT - 1){
				map[x + y * WIDTH] = e.getValue();
			}
		}
	}

	/**
	 * A fallen star's scorch (WorldEvents.craterCells) laid on a derived window: only on ground the
	 * generator really left open at this point of the year (a scorchable pristine - never water,
	 * ice, a tree or a road), never on a way between slices (link), never over a player's edit.
	 * The interior only, like the edits. The star's own pure guess at the ground is a pre-filter;
	 * this is the authority.
	 */
	public static void overlayScorch( int[] map, int[] pristine, byte[] link, HashMap<Long, Integer> scorch,
			HashMap<Long, Integer> edits, int ox, int oy ){
		if (scorch == null || scorch.isEmpty()) return;
		for (HashMap.Entry<Long, Integer> e : scorch.entrySet()){
			long key = e.getKey();
			int x = (int) (key & 0xFFFFFFFFL) - ox, y = (int) (key >> 32) - oy;
			if (x <= 0 || y <= 0 || x >= WIDTH - 1 || y >= HEIGHT - 1) continue;
			int c = x + y * WIDTH;
			if (edits != null && edits.containsKey( key )) continue;
			if (link != null && link[c] != LINK_NONE) continue;
			if (!WorldEvents.scorchable( pristine[c] )) continue;
			map[c] = e.getValue();
		}
	}

	/** Alt-art variance from WORLD coordinates: every world cell keeps the same variant forever, whatever window shows it. */
	public static byte[] variance( long seed, int ox, int oy ){
		byte[] v = new byte[WIDTH * HEIGHT];
		for (int y = 0; y < HEIGHT; y++){
			for (int x = 0; x < WIDTH; x++){
				v[x + y * WIDTH] = (byte) Math.floorMod( dressHash( seed, ox + x, oy + y, 0x7A81A7CEL ), 100 );
			}
		}
		return v;
	}

	// ------------------------------------------------------------ dressing

	public static long dressHash( long seed, int wx, int wy, long salt ){
		long h = seed ^ salt;
		h ^= wx * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= wy * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		return h ^ (h >>> 33);
	}

	private static int pick( int[] set, long h ){
		return set[(int)Math.floorMod( h, set.length )];
	}

	/**
	 * The ground a cell SHOWS, as a terrain: frozen ground under grass, bushes,
	 * trees, signposts and roads is drawn as snow (DungeonTerrainTilemap, the
	 * road's snow cap), and a
	 * boulder lies on whatever ground its neighbours show. The edge transitions
	 * blend what is drawn, not what the terrain is called.
	 */
	public static int visualGround( int[] map, boolean[] frozen, int cell ){
		int t = map[cell];
		if (t == Terrain.BOULDER) return rockGround( map, frozen, cell );
		return shownGround( map, frozen, cell );
	}

	//a non-rock cell's shown ground
	private static int shownGround( int[] map, boolean[] frozen, int cell ){
		int t = map[cell];
		boolean frozenHere = frozen != null && cell < frozen.length && frozen[cell];
		//(a frozen road wears a trodden snow cap - see dress() - so it reads as snow too)
		if (frozenHere && (t == Terrain.GRASS || t == Terrain.SHRUB || t == Terrain.TREE_PINE
				|| t == Terrain.TREE_OAK || t == Terrain.SIGN || t == Terrain.DIRT_PATH)){
			return Terrain.SNOW;
		}
		switch (t){
			case Terrain.TREE_PINE: case Terrain.TREE_OAK: case Terrain.SIGN:
				return Terrain.GRASS;
			default:
				return t;
		}
	}

	/**
	 * The ground under a boulder: the ground most of its eight neighbours show
	 * (stronger material wins a tie), so a rock sits in its field instead of on
	 * a square of its own. Bare ground when nothing around it is ground at all.
	 */
	public static int rockGround( int[] map, boolean[] frozen, int cell ){
		final int w = WIDTH;
		final int x = cell % w;
		int[] votes = new int[FAMILY_GROUND.length];
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx == 0 && dy == 0) || x + dx < 0 || x + dx >= w) continue;
				int n = cell + dx + dy * w;
				if (n < 0 || n >= map.length || map[n] == Terrain.BOULDER) continue;
				int f = blendFamily( shownGround( map, frozen, n ) );
				if (f >= 0) votes[f]++;
			}
		}
		int best = -1;
		for (int f = FAMILY_GROUND.length - 1; f >= 1; f--){
			if (votes[f] > 0 && (best == -1 || votes[f] > votes[best])) best = f;
		}
		return best == -1 ? Terrain.EMPTY : FAMILY_GROUND[best];
	}

	//the terrain a rock's ground takes for each family: ice, bare, grass, sand, snow
	private static final int[] FAMILY_GROUND = { Terrain.FROZEN_WATER, Terrain.EMPTY, Terrain.GRASS, Terrain.EMPTY_SP, Terrain.SNOW };

	//which version of an edge tile a world cell wears: fixed by the cell, so every window
	//agrees, and scattered, so neighbours rarely repeat
	private static int edgeVersion( long seed, int wx, int wy, int versions ){
		return (int)Math.floorMod( dressHash( seed, wx, wy, 0xED6EL ), versions );
	}

	//the deep-water shades' darkness, band by band (tools/dress_sheet.py paints them)
	private static final int[] SHADE_ALPHAS = { 60, 100, 135, 170 };

	//a water cell's shade band: -1 for shallow water (and anything that is not water),
	//0..3 for the deep tiers, darker further out
	private static int shadeBand( int[] map, Window base, int cell ){
		if (cell < 0 || cell >= map.length || map[cell] != Terrain.DEEP_WATER) return -1;
		int depth = base.waterDepth != null ? base.waterDepth[cell] : 2;
		return Math.max( 0, Math.min( OverworldDress.DEEP_SHADES.length - 1, depth - 2 ) );
	}

	/** A tall standing rock rather than a boulder: the stonier country's boulders under the sky, one in three. */
	public static boolean tallRock( long seed, int altitude, int wx, int wy ){
		if (!WorldLayers.openSky( altitude )) return false;
		WorldModel.Biome b = WorldModel.biomeAt( seed, wx, wy );
		if (b != WorldModel.Biome.FOOTHILLS && b != WorldModel.Biome.DESERT
				&& b != WorldModel.Biome.MOUNTAIN) return false;
		return Math.floorMod( dressHash( seed, wx, wy, 0x57A4DL ), 3 ) == 0;
	}

	/**
	 * Debug and tooling: the world cell at the heart of the nearest stretch where
	 * rocky foothills run up against a snowfield, so the ground transitions and
	 * the rocks can be looked at side by side (DebugScenes, DressDump).
	 */
	public static int[] findRockyEdge( long seed ){
		WorldModel.Sample s = new WorldModel.Sample();
		int[] best = null;
		int bestScore = 0;
		long bestDist = Long.MAX_VALUE;
		for (int cy = -4500; cy <= 4500; cy += 150){
			for (int cx = -4500; cx <= 4500; cx += 150){
				int hills = 0, snow = 0;
				for (int dy = -24; dy <= 24; dy += 6){
					for (int dx = -24; dx <= 24; dx += 6){
						WorldModel.sample( seed, cx + dx, cy + dy, s );
						if (s.biome == WorldModel.Biome.FOOTHILLS) hills++;
						else if (s.biome == WorldModel.Biome.SNOWFIELD) snow++;
					}
				}
				int score = Math.min( hills, snow );
				long dist = (long)cx * cx + (long)cy * cy;
				if (score >= 15 && (score > bestScore + 5 || (score >= bestScore - 5 && dist < bestDist))){
					bestScore = Math.max( bestScore, score );
					bestDist = dist;
					best = new int[]{ cx, cy };
				}
			}
		}
		return best;
	}

	//the blend family a cell of the window shows (blendFamily): a village's field takes none -
	//its crop is drawn whole, and no grass of it spills onto the bare ground beside it
	private static int familyAt( int[] map, Window base, int cell ){
		return familyOf( map, base.frozen, base.field, cell );
	}

	private static int familyOf( int[] map, boolean[] frozen, boolean[] field, int cell ){
		if (field != null && field[cell] && map[cell] == Terrain.FURROWED_GRASS) return -1;
		return blendFamily( visualGround( map, frozen, cell ) );
	}

	//the dressing's blends on one cell of family f (familyOf): the strongest neighbouring ground
	//material laps over its edges (ground), then a second, weaker one on the other sides or the
	//corner roundings where a stronger one meets only at a diagonal (edges). The same underfoot
	//(dress) and on a view of another band (viewBlends)
	private static void blendCell( int[] map, boolean[] frozen, boolean[] field, int cell, int f, int version, int[] ground, int[] edges ){
		final int w = WIDTH;
		int best = 0, mask = 0;
		for (int side = 0; side < 4; side++){
			int n = cell + (side == 0 ? -w : side == 1 ? 1 : side == 2 ? w : -1);
			int g = familyOf( map, frozen, field, n );
			if (g > f && g > 0){
				if (g > best){ best = g; mask = 0; }
				if (g == best) mask |= 1 << side;
			}
		}
		if (best > 0 && mask != 0){
			ground[cell] = OverworldDress.blend( BLEND_ROW[best], mask, version );
		}
		//the edges layer takes one more overlay: first a second, weaker
		//material on the other sides (ice between the snow and the grass) -
		//a whole side left square shows far more than a corner - otherwise
		//the corner roundings where a stronger material meets only at a
		//diagonal (NE needs neither N nor E covered, and so on round)
		int second = 0, mask2 = 0;
		if (best > 0){
			for (int side = 0; side < 4; side++){
				if ((mask & (1 << side)) != 0) continue;
				int n = cell + (side == 0 ? -w : side == 1 ? 1 : side == 2 ? w : -1);
				int g = familyOf( map, frozen, field, n );
				if (g > f && g > 0 && g < best){
					if (g > second){ second = g; mask2 = 0; }
					if (g == second) mask2 |= 1 << side;
				}
			}
		}
		if (second > 0){
			edges[cell] = OverworldDress.blend( BLEND_ROW[second], mask2, version );
		} else {
			int cBest = 0, cBits = 0;
			for (int d = 0; d < 4; d++){
				int sideA = d == 0 || d == 3 ? 1 : 4;    //N for NE/NW, S for SE/SW
				int sideB = d == 0 || d == 1 ? 2 : 8;    //E for NE/SE, W for SW/NW
				if ((mask & (sideA | sideB)) != 0) continue;
				int n = cell + (d == 0 ? -w+1 : d == 1 ? w+1 : d == 2 ? w-1 : -w-1);
				int g = familyOf( map, frozen, field, n );
				if (g > f && g > 0){
					if (g > cBest){ cBest = g; cBits = 0; }
					if (g == cBest) cBits |= 1 << d;
				}
			}
			if (cBest > 0 && cBits != 0){
				edges[cell] = OverworldDress.corner( CORNER_ROW[cBest], cBits, version );
			}
		}
	}

	//ground families for the edge-transition overlays, by visual strength:
	//stronger materials encroach on weaker ones. water is not here - the
	//water tiles stitch their own shores already
	private static int blendFamily( int t ){
		switch (t){
			case Terrain.SNOW: return 4;
			case Terrain.EMPTY_SP: return 3;                //sand
			case Terrain.GRASS: case Terrain.HIGH_GRASS:
			case Terrain.FURROWED_GRASS: case Terrain.SHRUB: return 2;
			case Terrain.EMPTY: case Terrain.EMPTY_DECO:
			case Terrain.DIRT_PATH: return 1;               //bare ground
			case Terrain.FROZEN_WATER: return 0;            //ice: every ground laps onto it
			default: return -1;                             //no blending on/from this
		}
	}

	//the overlay row each lapping family draws with: bare ground spills its green
	//over ice like the grass does, sand and snow have their own
	private static final int[] CORNER_ROW = { -1, OverworldDress.CORNER_GRASS, OverworldDress.CORNER_GRASS, OverworldDress.CORNER_SAND, OverworldDress.CORNER_SNOW };
	private static final int[] BLEND_ROW  = { -1, OverworldDress.BLEND_GRASS, OverworldDress.BLEND_GRASS, OverworldDress.BLEND_SAND, OverworldDress.BLEND_SNOW };

	//a crown may hang over open ground, never over something the player has to
	//see and use: a doorway, a staircase, a signpost, a shrine
	static boolean blocksSight( int terrain ){
		return terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR
				|| terrain == Terrain.LOCKED_DOOR || terrain == Terrain.CRYSTAL_DOOR
				|| terrain == Terrain.EXIT || terrain == Terrain.ENTRANCE
				|| terrain == Terrain.ENTRANCE_SP || terrain == Terrain.LOCKED_EXIT
				|| terrain == Terrain.SIGN || terrain == Terrain.PEDESTAL
				|| terrain == Terrain.ALCHEMY || terrain == Terrain.WELL;
	}

	/** A tree with leaves to lose: a standing (unfrozen) forest cell. */
	public static boolean forestAt( int[] map, boolean[] frozen, int cell ){
		if (cell < 0 || cell >= map.length) return false;
		int t = map[cell];
		return (t == Terrain.TREE_PINE || t == Terrain.TREE_OAK) && !(frozen != null && frozen[cell]);
	}

	//the dress tile of a way between slices: the natural ones by their kind,
	//a dug shaft (a way the generator did not make) by its direction
	private static int linkTile( int terrain, byte link ){
		switch (link){
			case LINK_MOUTH:       return OverworldDress.LINK_CAVE_MOUTH;
			case LINK_CAVE_EXIT:   return OverworldDress.LINK_CAVE_EXIT;
			case LINK_STAIR_UP:    return OverworldDress.LINK_STAIR_UP;
			case LINK_STAIR_DOWN:  return OverworldDress.LINK_STAIR_DOWN;
			case LINK_LADDER_UP:   return OverworldDress.LINK_SHAFT_UP;
			case LINK_LADDER_DOWN: return OverworldDress.LINK_SHAFT_DOWN;
			default:
				return terrain == Terrain.EXIT ? OverworldDress.LINK_SHAFT_DOWN : OverworldDress.LINK_SHAFT_UP;
		}
	}

	//a bridge tile from its neighbours: planks run along the road, so a span
	//is east-west when planks continue east or west (or, for a lone plank,
	//when the water lies north and south); the ends get their abutment
	private static int bridgeTile( int[] map, int cell, boolean frozen ){
		boolean bw = map[cell-1] == Terrain.BRIDGE, be = map[cell+1] == Terrain.BRIDGE;
		boolean bn = map[cell-WIDTH] == Terrain.BRIDGE, bs = map[cell+WIDTH] == Terrain.BRIDGE;
		boolean ew;
		if ((bw || be) != (bn || bs)){
			ew = bw || be;
		} else {
			ew = (Terrain.flags[map[cell-WIDTH]] & Terrain.LIQUID) != 0 || (Terrain.flags[map[cell+WIDTH]] & Terrain.LIQUID) != 0;
		}
		int[] set = ew ? (frozen ? OverworldDress.BRIDGE_SNOW_EW : OverworldDress.BRIDGE_EW)
				: (frozen ? OverworldDress.BRIDGE_SNOW_NS : OverworldDress.BRIDGE_NS);
		boolean capA = ew ? !bw : !bn, capB = ew ? !be : !bs;
		if (capA == capB) return set[0];
		return capA ? set[1] : set[2];
	}

	/**
	 * The window's dressing: three layers - ground and edges below the hero
	 * (the edges carry the corner roundings, so a cell can have a side
	 * transition and a corner at once), canopy above. The season is passed in,
	 * never read here: a network mirror is dressed for the HOST's point of the year.
	 */
	public static int[][] dress( long seed, int ox, int oy, int[] map, Window base, GameCalendar.Season season ){
		final int w = WIDTH, len = map.length;
		final boolean[] frozen = base.frozen;
		int[] ground = new int[len];
		int[] edges = new int[len];
		int[] canopy = new int[len];
		java.util.Arrays.fill( ground, -1 );
		java.util.Arrays.fill( edges, -1 );
		java.util.Arrays.fill( canopy, -1 );
		//the season picks the canopy: summer green, autumn orange-brown, and
		//in winter the forests standing on unfrozen ground carry snow (the
		//frozen ground has the winter pines). spring blossoms and autumn
		//leaves scatter on the ground by a world hash, so every window
		//agrees on where they lie
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int cell = x + y * w;
				int wx = ox + x, wy = oy + y;
				int t = map[cell];
				boolean frozenHere = frozen != null && frozen[cell];
				//trees (the town's own pines included - its art no longer
				//paints open ground, the world dresses it)
				//a tree or a standing rock: its body goes on the edges layer, over whatever
				//transition its own ground takes below (so it stands IN the snow line
				//rather than on a square cut out of it); a tree crown or rock peak goes
				//on the canopy
				int propBody = -1;
				if (t == Terrain.TREE_PINE || t == Terrain.TREE_OAK){
					long h = dressHash( seed, wx, wy, 0x7EEE5L );
					if (t == Terrain.TREE_OAK){
						int top, trunk;
						switch (season){
							case AUTUMN:
								top = OverworldDress.OAK_AUTUMN_TOP;
								trunk = OverworldDress.OAK_AUTUMN_TRUNK;
								break;
							case WINTER:
								top = OverworldDress.OAK_WINTER_TOP;
								trunk = OverworldDress.OAK_WINTER_TRUNK;
								break;
							default:
								top = OverworldDress.OAK_SUMMER_TOP;
								trunk = OverworldDress.OAK_SUMMER_TRUNK;
						}
						propBody = trunk;
						//the crown hangs over the cell above the trunk - but never
						//over a way in or out: a doorway with a canopy on it reads
						//as a bush growing in the gate
						if (y > 1 && WorldStructures.townCell( wx, wy-1 ) == -1
								&& !blocksSight( map[cell-w] )) canopy[cell-w] = top;
					} else {
						propBody = pick( frozenHere ? OverworldDress.WINTER_PINES : OverworldDress.SUMMER_PINES, h );
					}
				}
				if (t == Terrain.SIGN){
					ground[cell] = OverworldDress.SIGNPOST;
					continue;
				}
				if (t == Terrain.BOULDER && tallRock( seed, base.altitude, wx, wy )){
					//capped with snow where it stands in snow
					boolean snowy = rockGround( map, frozen, cell ) == Terrain.SNOW;
					long h = dressHash( seed, wx, wy, 0x80C4L );
					int v = (int)Math.floorMod( h, OverworldDress.ROCK_BODY.length );
					propBody = (snowy ? OverworldDress.ROCK_BODY_SNOW : OverworldDress.ROCK_BODY)[v];
					if (y > 1 && canopy[cell-w] == -1 && !blocksSight( map[cell-w] )){
						canopy[cell-w] = (snowy ? OverworldDress.ROCK_TOP_SNOW : OverworldDress.ROCK_TOP)[v];
					}
				}
				if (t == Terrain.DEEP_WATER || t == Terrain.WATER){
					int band = shadeBand( map, base, cell );
					if (band >= 0) ground[cell] = OverworldDress.DEEP_SHADES[band];
					//a deeper side spills its darkness over in loose pixels: the scatter
					//dark enough that this cell's own shade under it adds up to the
					//deepest neighbour's
					int mask = 0, deepest = band;
					for (int side = 0; side < 4; side++){
						int n = cell + (side == 0 ? -w : side == 1 ? 1 : side == 2 ? w : -1);
						int nb = shadeBand( map, base, n );
						if (nb > band){
							mask |= 1 << side;
							deepest = Math.max( deepest, nb );
						}
					}
					if (mask != 0){
						float own = band < 0 ? 0 : SHADE_ALPHAS[band] / 255f;
						float want = SHADE_ALPHAS[deepest] / 255f;
						float extra = 255f * (1f - (1f - want) / (1f - own));
						int pick = 0;
						for (int k = 1; k < SHADE_ALPHAS.length; k++){
							if (Math.abs( SHADE_ALPHAS[k] - extra ) < Math.abs( SHADE_ALPHAS[pick] - extra )) pick = k;
						}
						edges[cell] = OverworldDress.shadeEdge( pick, mask, edgeVersion( seed, wx, wy, OverworldDress.SHADE_VARIANTS ) );
					}
					continue;
				}
				//the ways between the slices draw over their stairs terrain -
				//outside the town, whose own staircase keeps its art
				if ((t == Terrain.EXIT || t == Terrain.ENTRANCE) && WorldStructures.townCell( wx, wy ) == -1){
					ground[cell] = linkTile( t, base.link != null ? base.link[cell] : LINK_NONE );
					continue;
				}
				if (t == Terrain.BRIDGE){
					ground[cell] = bridgeTile( map, cell, frozenHere );
					continue;
				}

				//biome edge transitions: the strongest neighbouring ground
				//material laps over this cell's edges (not inside the town -
				//its authored art owns those edges)
				if (WorldStructures.townCell( wx, wy ) != -1){
					if (propBody != -1) edges[cell] = propBody;
					continue;
				}

				//the roads across frozen ground lie under a trodden snow cap
				if (frozenHere && t == Terrain.DIRT_PATH){
					ground[cell] = pick( OverworldDress.ROAD_SNOW, dressHash( seed, wx, wy, 0x5A0BL ) );
					continue;
				}

				int f = familyAt( map, base, cell );
				if (f < 0){
					if (propBody != -1) edges[cell] = propBody;
					continue;
				}
				blendCell( map, frozen, base.field, cell, f, edgeVersion( seed, wx, wy, OverworldDress.BIOME_VARIANTS ), ground, edges );
				if (propBody != -1){
					edges[cell] = propBody;
					continue;
				}
				if (ground[cell] != -1 || edges[cell] != -1 || frozenHere || base.altitude != 0) continue;
				//spring: blossoms on the open grass of the meadows and plains
				//(about one cell in twelve); autumn: fallen leaves on the
				//ground beside the forests (about one in three)
				if (season == GameCalendar.Season.SPRING){
					if (t != Terrain.GRASS) continue;
					long h = dressHash( seed, wx, wy, 0xF10BEL );
					if (Math.floorMod( h, 12 ) != 0) continue;
					WorldModel.Biome b = WorldModel.biomeAt( seed, wx, wy );
					if (b == WorldModel.Biome.MEADOW || b == WorldModel.Biome.PLAINS){
						ground[cell] = pick( OverworldDress.SPRING_FLOWERS, h >> 4 );
					}
				} else if (season == GameCalendar.Season.AUTUMN){
					if (t != Terrain.GRASS && t != Terrain.EMPTY && t != Terrain.EMPTY_DECO
							&& t != Terrain.DIRT_PATH) continue;
					long h = dressHash( seed, wx, wy, 0x1EAFL );
					if (Math.floorMod( h, 3 ) != 0) continue;
					boolean byForest = forestAt( map, frozen, cell-w-1 ) || forestAt( map, frozen, cell-w ) || forestAt( map, frozen, cell-w+1 )
							|| forestAt( map, frozen, cell-1 ) || forestAt( map, frozen, cell+1 )
							|| forestAt( map, frozen, cell+w-1 ) || forestAt( map, frozen, cell+w ) || forestAt( map, frozen, cell+w+1 );
					if (!byForest) continue;
					WorldModel.Biome b = WorldModel.biomeAt( seed, wx, wy );
					if (b == WorldModel.Biome.FOREST || b == WorldModel.Biome.MEADOW){
						ground[cell] = pick( OverworldDress.FALLEN_LEAVES, h >> 2 );
					}
				}
			}
		}
		//the settlements' fittings: windows and a chimney on every human house, a fire ring
		//by every gnoll hut - the very cells SettlementAmbience lights at night
		if (base.altitude == 0) SettlementLights.dressFittings( seed, ox, oy, map, base.terrain, ground, edges, canopy );
		//the props of the caves' places (CaveSites): rails, timbers, the camp's furniture, giant
		//mushrooms, the tomb's dressed stone - before the ore, which never sits on them
		if (base.altitude < 0) CaveSites.dress( base.sites, ox, oy, map, ground, edges, canopy );
		//the places on the mountains: the hermit's windows and chimney, the waystation's, the nest, the pack
		if (base.altitude > 0) MountainSites.dressFittings( seed, ox, oy, map, base, ground, edges, canopy );
		//the rock of the slices above and below wears its ore where its face shows (Ores): last,
		//so it never sits on anything drawn there before it
		if (base.altitude != 0) Ores.dressVeins( seed, ox, oy, map, base.veins, ground, canopy );
		return new int[][]{ ground, edges, canopy };
	}
}
