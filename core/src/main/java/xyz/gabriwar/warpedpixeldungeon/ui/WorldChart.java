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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.noosa.Image;

/**
 * A slab of the world, drawn one pixel per STRIDE cells.
 *
 * A view of 96 cells onto an infinite world shows the player almost none of it at a time.
 * Without a chart that is not exploration, it is a wide corridor: there is no way to form
 * a picture of the place, so there is nothing to navigate by and nowhere to decide to go.
 *
 * <h3>One texture, painted rarely, off the main thread</h3>
 *
 * The world is a pure function of (seed, x, y), so any slab of it can be regenerated at
 * any time - but sampling a million cells is not free (a quarter of a second on a desktop,
 * more on a phone), so the slab is painted once by a worker thread into a pixel array,
 * handed to the GPU as a single image the moment it is done, and kept. It is only
 * repainted when the hero has walked out of the middle half of it, which is a walk of
 * hundreds of cells. A chart asked for before its slab is done draws nothing until the
 * slab arrives (update), and a scene can ask for the slab ahead of the map ({@link #prepare}).
 *
 * <h3>The mountains are drawn as what they are: slices</h3>
 *
 * Above the mountain line the surface sample only says MOUNTAIN, but the ground there is
 * a stack of slices ({@link WorldLayers#band}), each its own level with its own climate:
 * the chart paints every pixel of a peak as the ground of its own band - snow once the
 * band is frozen, alpine meadow below the snow line, a tarn in the hollows - a shade
 * lighter per band up, and darkens the pixel just south of a higher band, where that
 * band's scarp face stands in the game. The contours of a peak read on the chart as they
 * are climbed.
 */
public class WorldChart extends Image {

	//the chart is SIZE x SIZE pixels covering SPAN x SPAN world cells
	public static final int SIZE   = 1024;
	public static final int STRIDE = 2;
	public static final int SPAN   = SIZE * STRIDE;

	//the slab on the GPU
	private static SmartTexture painted;
	private static long paintedSeed;
	private static int paintedOX, paintedOY;

	//the slab the worker is painting, or has painted and the GL thread has not yet uploaded
	private static final Object LOCK = new Object();
	private static long wantedSeed;
	private static int wantedOX, wantedOY;
	private static boolean wanted;
	private static int[] done;
	private static long doneSeed;
	private static int doneOX, doneOY;
	private static Thread worker;

	/** World coordinate of the chart's top-left pixel. */
	public final int originX, originY;

	private final long seed;

	//the origin of the slab on show: the chart's own, or - while that one is still being
	//painted - the last one painted for this seed, drawn shifted into place
	private int shownOX, shownOY;

	public WorldChart( long seed, int centreX, int centreY ){
		super();
		this.seed = seed;

		int[] origin = prepare( seed, centreX, centreY );
		originX = origin[0];
		originY = origin[1];

		adopt();
	}

	/**
	 * Has the slab around this point painted, unless one that still holds the point in its
	 * middle half is on the GPU or on its way. Returns the origin of the slab the chart
	 * will show: the one kept, or the one being painted.
	 */
	public static int[] prepare( long seed, int centreX, int centreY ){
		synchronized (LOCK){
			if (painted != null && paintedSeed == seed && inMiddle( paintedOX, paintedOY, centreX, centreY )){
				return new int[]{ paintedOX, paintedOY };
			}
			if (done != null && doneSeed == seed && inMiddle( doneOX, doneOY, centreX, centreY )){
				return new int[]{ doneOX, doneOY };
			}
			if (wanted && wantedSeed == seed && inMiddle( wantedOX, wantedOY, centreX, centreY )){
				return new int[]{ wantedOX, wantedOY };
			}
			wanted = true;
			wantedSeed = seed;
			wantedOX = centreX - SPAN / 2;
			wantedOY = centreY - SPAN / 2;
			if (worker == null || !worker.isAlive()){
				worker = new Thread( WorldChart::work, "world-chart" );
				worker.setDaemon( true );
				worker.start();
			}
			return new int[]{ wantedOX, wantedOY };
		}
	}

	private static boolean inMiddle( int ox, int oy, int cx, int cy ){
		return cx >= ox + SPAN / 4 && cx < ox + SPAN * 3 / 4
				&& cy >= oy + SPAN / 4 && cy < oy + SPAN * 3 / 4;
	}

	//paints whatever slab is wanted until none is: a slab asked for while another paints
	//is painted right after it
	private static void work(){
		while (true){
			long seed;
			int ox, oy;
			synchronized (LOCK){
				if (!wanted) return;
				seed = wantedSeed;
				ox = wantedOX;
				oy = wantedOY;
			}
			int[] pixels = pixels( seed, ox, oy );
			synchronized (LOCK){
				done = pixels;
				doneSeed = seed;
				doneOX = ox;
				doneOY = oy;
				if (wantedSeed == seed && wantedOX == ox && wantedOY == oy) wanted = false;
			}
		}
	}

	//takes the painted slab if it is this chart's; uploads a finished one first
	private void adopt(){
		synchronized (LOCK){
			if (done != null){
				Pixmap pm = new Pixmap( SIZE, SIZE, Pixmap.Format.RGBA8888 );
				pm.setBlending( Pixmap.Blending.None );
				for (int y = 0; y < SIZE; y++){
					for (int x = 0; x < SIZE; x++){
						pm.drawPixel( x, y, done[y * SIZE + x] );
					}
				}
				if (painted != null) painted.delete();
				painted = new SmartTexture( pm );
				paintedSeed = doneSeed;
				paintedOX = doneOX;
				paintedOY = doneOY;
				done = null;
			}
			if (painted != null && paintedSeed == seed
					&& (texture == null || !ready())
					&& (texture != painted || paintedOX != shownOX || paintedOY != shownOY)){
				texture( painted );
				frame( 0, 0, SIZE, SIZE );
				shownOX = paintedOX;
				shownOY = paintedOY;
			}
		}
	}

	@Override
	public void update(){
		super.update();
		if (!ready()) adopt();
	}

	/** Has this chart's own slab arrived? Until it has the chart draws the last one, shifted. */
	public boolean ready(){
		return texture != null && shownOX == originX && shownOY == originY;
	}

	/** Where the slab on show sits in the page, at this scale: (0, 0) once it is the chart's own. */
	public float shownX( float scale ){
		return (shownOX - originX) / (float)STRIDE * scale;
	}

	public float shownY( float scale ){
		return (shownOY - originY) / (float)STRIDE * scale;
	}

	/** Chart pixel of a world coordinate; may fall outside [0, SIZE). */
	public float chartX( int wx ){
		return (wx - originX) / (float)STRIDE;
	}

	public float chartY( int wy ){
		return (wy - originY) / (float)STRIDE;
	}

	/** World coordinate of a chart pixel. */
	public int worldX( float px ){
		return originX + (int)(px * STRIDE);
	}

	public int worldY( float py ){
		return originY + (int)(py * STRIDE);
	}

	//the colours of the surface's biomes
	static int biomeColour( WorldModel.Biome biome ){
		switch (biome){
			case OCEAN:     return 0x1b4d7a;
			case RIVER:     return 0x2e6fa8;
			case BEACH:     return 0xd8c48e;
			case PLAINS:    return 0x8fae5a;
			case MEADOW:    return 0x5f9b46;
			case FOREST:    return 0x2f6b2f;
			case SWAMP:     return 0x4a6d4f;
			case DESERT:    return 0xd9c06a;
			case TUNDRA:    return 0xb8c4c8;
			case SNOWFIELD: return 0xeef4f8;
			case FOOTHILLS: return 0x8a7f6b;
			case MOUNTAIN:  return 0x6e6e72;
			default:        return 0x000000;
		}
	}

	//the ground of a mountain band: bare alpine ground at the first band, snow once the
	//band is frozen, a tarn where the slice holds one; each a step paler per band up
	private static final int ROCK = 0x6e6e72, ROCK_TOP = 0xa8a6a4;
	private static final int SNOW = 0xd4dde6, SNOW_TOP = 0xffffff;
	private static final int MEADOW = 0x7d9a5c, MEADOW_TOP = 0xb4c69a;
	private static final int TARN = 0x3a7fb8, TARN_ICE = 0xbfe3f0;
	//the face of a scarp, drawn on the pixel south of a higher band
	private static final int SCARP = 0x2c2c30;

	static int bandColour( long seed, int wx, int wy, int band, WorldModel.Sample s ){
		float up = (band - 1) / (float)(WorldLayers.MAX_ABOVE - 1);
		boolean frozen = WorldModel.alpineTemperature( s, band ) < WorldModel.FREEZE;
		if (WorldModel.tarnAt( seed, wx, wy, band )){
			return frozen ? TARN_ICE : TARN;
		}
		if (frozen) return mix( SNOW, SNOW_TOP, up );
		//the meadow thins to bare ground up the band, as the snow line nears
		float warmth = (WorldModel.alpineTemperature( s, band ) - WorldModel.FREEZE) / 0.12f;
		int ground = mix( ROCK, ROCK_TOP, up );
		return mix( ground, mix( MEADOW, MEADOW_TOP, up ), Math.min( 1f, warmth ) );
	}

	static int mix( int a, int b, float t ){
		int r = Math.round( ((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t );
		int g = Math.round( ((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t );
		int bl = Math.round( (a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t );
		return (r << 16) | (g << 8) | bl;
	}

	/** The slab's pixels (RGBA, row-major), as the worker paints them: pure, for any thread. */
	public static int[] pixels( long seed, int ox, int oy ){

		int[] px = new int[SIZE * SIZE];
		byte[] band = new byte[SIZE * SIZE];

		WorldModel.Sample smp = new WorldModel.Sample();
		for (int py = 0; py < SIZE; py++){
			for (int px0 = 0; px0 < SIZE; px0++){
				int wx = ox + px0 * STRIDE, wy = oy + py * STRIDE;
				WorldModel.sample( seed, wx, wy, smp );
				int b = WorldLayers.band( smp.elev );
				band[py * SIZE + px0] = (byte)b;
				int rgb = b == 0 ? biomeColour( smp.biome ) : bandColour( seed, wx, wy, b, smp );
				px[py * SIZE + px0] = (rgb << 8) | 0xFF;
			}
		}

		//the scarps: a higher band to the north stands as a face over this pixel
		for (int py = 1; py < SIZE; py++){
			for (int px0 = 0; px0 < SIZE; px0++){
				int i = py * SIZE + px0;
				if (band[i - SIZE] > band[i]){
					px[i] = (mix( px[i] >>> 8, SCARP, 0.7f ) << 8) | 0xFF;
				}
			}
		}

		//sites: villages (amber), ruins (violet), dragon lairs (red)
		int s0x = Math.floorDiv( ox, WorldStructures.SECTOR ) - 1;
		int s0y = Math.floorDiv( oy, WorldStructures.SECTOR ) - 1;
		int span = SPAN / WorldStructures.SECTOR + 2;
		for (int sy = s0y; sy <= s0y + span; sy++){
			for (int sx = s0x; sx <= s0x + span; sx++){
				WorldStructures.Site site = WorldStructures.siteType( seed, sx, sy );
				if (site == WorldStructures.Site.NONE) continue;
				int cx = (WorldStructures.siteX( seed, sx, sy ) - ox) / STRIDE;
				int cy = (WorldStructures.siteY( seed, sx, sy ) - oy) / STRIDE;
				if (cx < 1 || cy < 1 || cx >= SIZE - 1 || cy >= SIZE - 1) continue;
				int rgb = site == WorldStructures.Site.VILLAGE ? 0xffb347
						: site == WorldStructures.Site.RUIN ? 0xcc66ff : 0xff4444;
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						px[(cy + dy) * SIZE + cx + dx] = (rgb << 8) | 0xFF;
					}
				}
			}
		}

		return px;
	}

	/** Forgets the painted slab, so the next chart repaints it. */
	public static void invalidate(){
		synchronized (LOCK){
			if (painted != null) painted.delete();
			painted = null;
			done = null;
		}
	}
}
