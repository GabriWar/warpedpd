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

package xyz.gabriwar.warpedpixeldungeon.effects;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.glwrap.Texture;

import java.util.HashMap;
import java.util.Random;

/**
 * The sea of clouds' deck, painted for the game's seed (tools/layer_life_gen.py painted one
 * such tile by hand once; the same procedure runs here, so no two games float over the same
 * clouds, and on a tile twice as wide, so it repeats half as often): a deck of cumulus seen from
 * above on a SIZE-pixel torus - every distance taken the short way round, so it tiles with no
 * seam. Low noise lays out the masses and the lanes of open air between them; billows (domes)
 * are piled where it is high, bigger the higher; their height field is lit from the north-west:
 * a white sunlit top, a light body, a blue-grey underside to the south-east, a dithered rim.
 * Painted off the render thread (get), and uploaded as two textures: the deck, and the deck with
 * every other texel cleared, from which CloudSea draws the frayed edge of a cloud the mountain
 * cuts.
 */
public final class CloudDeck {

	public static final int SIZE = 512;

	//RGBA
	private static final int TOP = rgba( 255, 255, 255, 250 ), BODY = rgba( 236, 240, 248, 235 );
	private static final int UNDER = rgba( 178, 190, 212, 215 ), RIM = rgba( 214, 222, 236, 120 );
	//the masses' noise lattices, points across the tile (64 and 32 pixels apart), and how much
	//of the tile is cloud, the billows' grid and their radii
	private static final int COARSE = SIZE / 64, FINE = SIZE / 32;
	private static final float COVER = 0.43f;
	private static final int BILLOW_STEP = 13, BILLOW_MIN = 9, BILLOW_MAX = 20;

	private CloudDeck(){}

	private static int rgba( int r, int g, int b, int a ){
		return (r << 24) | (g << 16) | (b << 8) | a;
	}

	private static final class Deck {
		final long seed;
		volatile int[] pixels;
		SmartTexture[] textures;
		Deck( long seed ){ this.seed = seed; }
	}

	private static final HashMap<Long, Deck> decks = new HashMap<>();

	/**
	 * The deck for this seed, {the deck, the frayed deck}, or null while it is still being
	 * painted (a worker paints it the first time it is asked for; ask again next frame).
	 */
	public static synchronized SmartTexture[] get( long seed ){
		Deck d = decks.get( seed );
		if (d == null){
			d = new Deck( seed );
			decks.put( seed, d );
			final Deck painting = d;
			Thread t = new Thread( () -> painting.pixels = paint( painting.seed, SIZE ), "cloud-deck" );
			t.setDaemon( true );
			t.setPriority( Thread.MIN_PRIORITY );
			t.start();
			return null;
		}
		if (d.textures != null) return d.textures;
		int[] px = d.pixels;
		if (px == null) return null;
		Pixmap deck = new Pixmap( SIZE, SIZE, Pixmap.Format.RGBA8888 );
		Pixmap rim = new Pixmap( SIZE, SIZE, Pixmap.Format.RGBA8888 );
		for (int y = 0; y < SIZE; y++){
			for (int x = 0; x < SIZE; x++){
				int c = px[x + y * SIZE];
				deck.drawPixel( x, y, c );
				rim.drawPixel( x, y, ((x + y) & 1) == 0 ? c : 0 );
			}
		}
		d.textures = new SmartTexture[]{
				new SmartTexture( deck, Texture.NEAREST, Texture.REPEAT, false ),
				new SmartTexture( rim, Texture.NEAREST, Texture.REPEAT, false ) };
		d.pixels = null;
		return d.textures;
	}

	/** The deck's pixels (RGBA, row by row), size x size: pure, so a test can look at it. */
	public static int[] paint( long seed, int size ){
		final int T = size;
		float[][] coarse = lattice( COARSE * T / SIZE, seed ), fine = lattice( FINE * T / SIZE, seed + 1 );
		Random rnd = new Random( seed + 7 );
		//the billows: on a jittered grid, where the masses' noise lies high, bigger the higher
		java.util.ArrayList<float[]> billows = new java.util.ArrayList<>();
		for (int gy = 0; gy < T; gy += BILLOW_STEP){
			for (int gx = 0; gx < T; gx += BILLOW_STEP){
				float x = gx + rnd.nextFloat() * BILLOW_STEP, y = gy + rnd.nextFloat() * BILLOW_STEP;
				float v = (noise( coarse, x, y, T ) + 0.45f * noise( fine, x, y, T )) / 1.45f;
				if (v < COVER) continue;
				float r = BILLOW_MIN + Math.min( 1f, (v - COVER) * 4f ) * (BILLOW_MAX - BILLOW_MIN) * (0.75f + 0.35f * rnd.nextFloat());
				billows.add( new float[]{ x, y, r } );
			}
		}
		//by 32-pixel bucket: no billow is wider than one, so the eight round a pixel's suffice
		final int B = 32, nb = T / B;
		@SuppressWarnings("unchecked")
		java.util.ArrayList<float[]>[] buckets = new java.util.ArrayList[nb * nb];
		for (float[] b : billows){
			int k = (((int) b[0] / B) % nb) + (((int) b[1] / B) % nb) * nb;
			if (buckets[k] == null) buckets[k] = new java.util.ArrayList<>();
			buckets[k].add( b );
		}
		//the domes' height field
		float[] h = new float[T * T];
		for (int y = 0; y < T; y++){
			for (int x = 0; x < T; x++){
				float best = 0f;
				int bx0 = x / B, by0 = y / B;
				for (int oy = -1; oy <= 1; oy++){
					for (int ox = -1; ox <= 1; ox++){
						java.util.ArrayList<float[]> list = buckets[Math.floorMod( bx0 + ox, nb ) + Math.floorMod( by0 + oy, nb ) * nb];
						if (list == null) continue;
						for (float[] b : list){
							float dx = wrap( x + 0.5f - b[0], T ), dy = wrap( y + 0.5f - b[1], T ), r = b[2];
							float q = 1f - (dx * dx + dy * dy) / (r * r);
							if (q > 0f) best = Math.max( best, r * (float) Math.sqrt( q ) );
						}
					}
				}
				h[x + y * T] = best;
			}
		}
		//lit from the north-west: a slope rising to the south-east faces the light, one falling
		//to it is in shade
		int[] px = new int[T * T];
		for (int y = 0; y < T; y++){
			for (int x = 0; x < T; x++){
				float v = h[x + y * T];
				if (v <= 0f) continue;
				float s = h[Math.floorMod( x + 1, T ) + Math.floorMod( y + 2, T ) * T]
						- h[Math.floorMod( x - 1, T ) + Math.floorMod( y - 2, T ) * T];
				int c;
				if (v < 2.2f){
					if (((x + y) & 1) != 0) continue;
					c = RIM;
				} else if (s < -3f){
					c = UNDER;
				} else if (s > 1.5f){
					c = TOP;
				} else {
					c = BODY;
				}
				px[x + y * T] = c;
			}
		}
		return px;
	}

	private static float wrap( float d, int T ){
		float half = T / 2f;
		d = (d + half) % T;
		if (d < 0) d += T;
		return d - half;
	}

	private static float[][] lattice( int period, long seed ){
		Random rnd = new Random( seed );
		float[][] g = new float[period][period];
		for (int y = 0; y < period; y++) for (int x = 0; x < period; x++) g[y][x] = rnd.nextFloat();
		return g;
	}

	//smooth noise on a lattice of g.length points across the tile, wrapping round it
	private static float noise( float[][] g, float x, float y, int T ){
		int period = g.length;
		float s = T / (float) period;
		float fx = x / s, fy = y / s;
		int ix = (int) Math.floor( fx ), iy = (int) Math.floor( fy );
		float tx = fx - ix, ty = fy - iy;
		tx = tx * tx * (3f - 2f * tx);
		ty = ty * ty * (3f - 2f * ty);
		int x0 = Math.floorMod( ix, period ), x1 = Math.floorMod( ix + 1, period );
		int y0 = Math.floorMod( iy, period ), y1 = Math.floorMod( iy + 1, period );
		float a = g[y0][x0], b = g[y0][x1], c = g[y1][x0], d = g[y1][x1];
		float top = a + (b - a) * tx, bottom = c + (d - c) * tx;
		return top + (bottom - top) * ty;
	}
}
