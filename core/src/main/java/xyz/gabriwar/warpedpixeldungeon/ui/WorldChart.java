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
 * <h3>One texture, painted rarely</h3>
 *
 * The world is a pure function of (seed, x, y), so any slab of it can be regenerated at
 * any time - but sampling a million cells is not free (a quarter of a second on a desktop,
 * more on a phone), so the slab is painted once into a pixmap, handed to the GPU as a
 * single image, and kept. It is only repainted when the hero has walked out of the middle
 * half of it, which is a walk of hundreds of cells.
 */
public class WorldChart extends Image {

	//the chart is SIZE x SIZE pixels covering SPAN x SPAN world cells
	public static final int SIZE   = 256;
	public static final int STRIDE = 4;
	public static final int SPAN   = SIZE * STRIDE;

	private static SmartTexture painted;
	private static long paintedSeed;
	private static int paintedOX, paintedOY;

	/** World coordinate of the chart's top-left pixel. */
	public final int originX, originY;

	public WorldChart( long seed, int centreX, int centreY ){
		super();

		//keep the painted slab while the hero is still well inside it - repainting on
		//every open would stutter the map button for no new information
		if (painted == null || paintedSeed != seed
				|| centreX < paintedOX + SPAN / 4 || centreX >= paintedOX + SPAN * 3 / 4
				|| centreY < paintedOY + SPAN / 4 || centreY >= paintedOY + SPAN * 3 / 4){
			paint( seed, centreX - SPAN / 2, centreY - SPAN / 2 );
		}

		originX = paintedOX;
		originY = paintedOY;

		texture( painted );
		frame( 0, 0, SIZE, SIZE );
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

	private static synchronized void paint( long seed, int ox, int oy ){

		Pixmap pm = new Pixmap( SIZE, SIZE, Pixmap.Format.RGBA8888 );
		pm.setBlending( Pixmap.Blending.None );

		WorldModel.Sample smp = new WorldModel.Sample();
		for (int py = 0; py < SIZE; py++){
			for (int px = 0; px < SIZE; px++){
				WorldModel.sample( seed, ox + px * STRIDE, oy + py * STRIDE, smp );
				int rgb;
				switch (smp.biome){
					case OCEAN:     rgb = 0x1b4d7a; break;
					case RIVER:     rgb = 0x2e6fa8; break;
					case BEACH:     rgb = 0xd8c48e; break;
					case PLAINS:    rgb = 0x8fae5a; break;
					case MEADOW:    rgb = 0x5f9b46; break;
					case FOREST:    rgb = 0x2f6b2f; break;
					case SWAMP:     rgb = 0x4a6d4f; break;
					case DESERT:    rgb = 0xd9c06a; break;
					case TUNDRA:    rgb = 0xb8c4c8; break;
					case SNOWFIELD: rgb = 0xeef4f8; break;
					case FOOTHILLS: rgb = 0x8a7f6b; break;
					case MOUNTAIN:  rgb = 0x6e6e72; break;
					default:        rgb = 0x000000;
				}
				pm.drawPixel( px, py, (rgb << 8) | 0xFF );
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
				int px = (WorldStructures.siteX( seed, sx, sy ) - ox) / STRIDE;
				int py = (WorldStructures.siteY( seed, sx, sy ) - oy) / STRIDE;
				if (px < 1 || py < 1 || px >= SIZE - 1 || py >= SIZE - 1) continue;
				int rgb = site == WorldStructures.Site.VILLAGE ? 0xffb347
						: site == WorldStructures.Site.RUIN ? 0xcc66ff : 0xff4444;
				pm.setColor( (rgb << 8) | 0xFF );
				pm.fillRectangle( px - 1, py - 1, 3, 3 );
			}
		}

		if (painted != null) painted.delete();
		painted = new SmartTexture( pm );
		paintedSeed = seed;
		paintedOX = ox;
		paintedOY = oy;
	}

	/** Forgets the painted slab, so the next chart repaints it. */
	public static synchronized void invalidate(){
		if (painted != null) painted.delete();
		painted = null;
	}
}
