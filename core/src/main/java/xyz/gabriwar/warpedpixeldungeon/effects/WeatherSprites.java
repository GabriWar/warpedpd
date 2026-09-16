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
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Game;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The weather's own sprite sheet, painted once at native pixel size: rain
 * streaks and their splashes, hail, four sizes of snowflake, blizzard streaks,
 * glows for fireflies and embers, leaves in three attitudes, petals, dust, ash,
 * wisps of mist and steam. Everything is drawn in white and grey with its own
 * alpha, so a particle tints it with color() and the shape stays crisp.
 *
 * Also holds the painters the sky overlays use: the rainbow and the aurora
 * curtains, drawn pixel by pixel instead of stretched from a gradient.
 */
public final class WeatherSprites {

	private WeatherSprites(){}

	private static final Object KEY = "weather-sprites";
	private static final int W = 64, H = 64;

	//frames: x, y, w, h
	public static final int[] RAIN_V   = { 0, 0, 1, 4 };
	public static final int[] RAIN_S1  = { 2, 0, 2, 4 };
	public static final int[] RAIN_S2  = { 5, 0, 3, 4 };
	public static final int[] SPLASH_0 = { 9, 0, 5, 3 };
	public static final int[] SPLASH_1 = { 15, 0, 5, 3 };
	public static final int[] SPLASH_2 = { 21, 0, 5, 3 };
	public static final int[] HAIL_2   = { 27, 0, 2, 2 };
	public static final int[] HAIL_1   = { 30, 0, 1, 1 };
	public static final int[] FLAKE_1  = { 32, 0, 1, 1 };
	public static final int[] FLAKE_2  = { 34, 0, 2, 2 };
	public static final int[] FLAKE_3  = { 37, 0, 3, 3 };
	public static final int[] FLAKE_5  = { 41, 0, 5, 5 };
	public static final int[] STREAK_3 = { 47, 0, 3, 1 };
	public static final int[] STREAK_4 = { 51, 0, 4, 1 };
	public static final int[] DROP     = { 56, 0, 1, 3 };
	public static final int[] GLOW_5   = { 0, 8, 5, 5 };
	public static final int[] GLOW_3   = { 6, 8, 3, 3 };
	public static final int[] LEAF_A   = { 10, 8, 3, 3 };
	public static final int[] LEAF_B   = { 14, 8, 3, 2 };
	public static final int[] LEAF_C   = { 18, 8, 2, 3 };
	public static final int[] PETAL    = { 21, 8, 2, 2 };
	public static final int[] SPECK_2  = { 24, 8, 2, 1 };
	public static final int[] SPECK_1  = { 27, 8, 1, 1 };
	public static final int[] ASH_2    = { 29, 8, 2, 2 };
	public static final int[] WISP_S   = { 32, 8, 8, 3 };
	public static final int[] WISP_L   = { 41, 8, 12, 4 };
	public static final int[] CORONA   = { 54, 8, 3, 3 };
	public static final int[] WISP_XL  = { 0, 16, 16, 5 };
	public static final int[] EMBER    = { 17, 16, 1, 1 };
	public static final int[] RIPPLE_0 = { 19, 16, 3, 2 };
	public static final int[] RIPPLE_1 = { 23, 16, 5, 3 };
	public static final int[] RIPPLE_2 = { 29, 16, 7, 3 };
	public static final int[] RAIN_VL  = { 38, 16, 1, 7 };
	public static final int[] RAIN_S1L = { 40, 16, 2, 7 };
	public static final int[] RAIN_S2L = { 43, 16, 3, 7 };
	//fire, heat and cloud: the flames burn in three sizes, the ray is the
	//shimmer that stands over baking ground, the puffs are cloud bodies
	public static final int[] FLAME_1  = { 0, 24, 3, 4 };
	public static final int[] FLAME_2  = { 4, 24, 5, 7 };
	public static final int[] FLAME_3  = { 10, 24, 7, 10 };
	public static final int[] SPARK_2  = { 18, 24, 1, 2 };
	public static final int[] HEAT_RAY = { 20, 24, 3, 14 };
	public static final int[] PUFF_S   = { 24, 24, 9, 6 };
	public static final int[] PUFF_L   = { 34, 24, 14, 8 };

	public static final int[][] FLAMES = { FLAME_1, FLAME_2, FLAME_3 };
	public static final int[][] PUFFS  = { PUFF_S, PUFF_L };

	public static final int[][] SPLASH = { SPLASH_0, SPLASH_1, SPLASH_2 };
	public static final int[][] RIPPLE = { RIPPLE_0, RIPPLE_1, RIPPLE_2 };
	public static final int[][] LEAVES = { LEAF_A, LEAF_B, LEAF_C };

	private static SmartTexture sheet;

	public static SmartTexture get(){
		if (sheet == null || !TextureCache.contains( KEY )){
			sheet = TextureCache.create( KEY, W, H );
			Pixmap pm = sheet.bitmap;
			pm.setBlending( Pixmap.Blending.None );
			pm.setColor( 0 );
			pm.fill();
			paint( pm );
			sheet.bitmap( pm );
			sheet.filter( Texture.NEAREST, Texture.NEAREST );
		}
		return sheet;
	}

	private static void a( Pixmap pm, int x, int y, int alpha ){
		pm.drawPixel( x, y, rgba( 0xFFFFFF, alpha ) );
	}

	private static void g( Pixmap pm, int x, int y, int grey ){
		pm.drawPixel( x, y, rgba( rgb( grey, grey, grey ), 0xFF ) );
	}

	private static void paint( Pixmap pm ){
		//rain: a streak that brightens toward its falling end
		a( pm, 0, 0, 70 ); a( pm, 0, 1, 150 ); a( pm, 0, 2, 220 ); a( pm, 0, 3, 255 );
		a( pm, 2, 0, 70 ); a( pm, 2, 1, 150 ); a( pm, 3, 2, 220 ); a( pm, 3, 3, 255 );
		a( pm, 5, 0, 70 ); a( pm, 6, 1, 150 ); a( pm, 6, 2, 220 ); a( pm, 7, 3, 255 );
		//splash: a crown that opens and thins
		a( pm, 11, 2, 255 ); a( pm, 10, 1, 150 ); a( pm, 12, 1, 150 );
		a( pm, 16, 1, 220 ); a( pm, 18, 1, 220 ); a( pm, 15, 2, 120 ); a( pm, 19, 2, 120 ); a( pm, 17, 0, 90 );
		a( pm, 21, 2, 90 ); a( pm, 25, 2, 90 ); a( pm, 22, 1, 50 ); a( pm, 24, 1, 50 );
		//hail: a ball with a shaded side
		g( pm, 27, 0, 250 ); g( pm, 28, 0, 215 ); g( pm, 27, 1, 215 ); g( pm, 28, 1, 165 );
		g( pm, 30, 0, 235 );
		//flakes: a dot, a pair, a cross, a crystal
		g( pm, 32, 0, 245 );
		g( pm, 34, 0, 250 ); g( pm, 35, 0, 215 ); g( pm, 34, 1, 215 ); g( pm, 35, 1, 190 );
		g( pm, 38, 0, 220 ); g( pm, 37, 1, 220 ); g( pm, 38, 1, 255 ); g( pm, 39, 1, 220 ); g( pm, 38, 2, 220 );
		for (int k = 0; k < 5; k++){ a( pm, 41 + k, 2, k == 2 ? 255 : (k == 0 || k == 4 ? 140 : 220) ); a( pm, 43, k, k == 2 ? 255 : (k == 0 || k == 4 ? 140 : 220) ); }
		a( pm, 42, 1, 110 ); a( pm, 44, 1, 110 ); a( pm, 42, 3, 110 ); a( pm, 44, 3, 110 );
		//blizzard streaks
		a( pm, 47, 0, 120 ); a( pm, 48, 0, 220 ); a( pm, 49, 0, 255 );
		a( pm, 51, 0, 90 ); a( pm, 52, 0, 160 ); a( pm, 53, 0, 230 ); a( pm, 54, 0, 255 );
		//a drip
		a( pm, 56, 0, 90 ); a( pm, 56, 1, 200 ); a( pm, 56, 2, 255 );

		//glows: a bright heart with a soft halo, additive when drawn
		glow( pm, 0, 8, 5 );
		glow( pm, 6, 8, 3 );
		//leaves in three attitudes, shaded so a tint keeps its body
		g( pm, 12, 8, 235 ); g( pm, 11, 9, 235 ); g( pm, 12, 9, 170 ); g( pm, 10, 10, 200 ); g( pm, 11, 10, 150 );
		g( pm, 14, 8, 200 ); g( pm, 15, 8, 240 ); g( pm, 16, 8, 200 ); g( pm, 15, 9, 150 );
		g( pm, 18, 8, 230 ); g( pm, 19, 9, 240 ); g( pm, 18, 9, 160 ); g( pm, 18, 10, 200 );
		//a petal
		g( pm, 21, 8, 250 ); g( pm, 22, 8, 235 ); g( pm, 21, 9, 235 ); g( pm, 22, 9, 175 );
		//specks
		g( pm, 24, 8, 220 ); g( pm, 25, 8, 170 ); g( pm, 27, 8, 220 );
		//ash: a flake with a hole in it
		g( pm, 29, 8, 200 ); g( pm, 30, 9, 200 ); g( pm, 30, 8, 120 ); g( pm, 29, 9, 120 );
		//wisps: soft, dithered, longer than tall
		wisp( pm, 32, 8, 8, 3, 1 );
		wisp( pm, 41, 8, 12, 4, 2 );
		wisp( pm, 0, 16, 16, 5, 3 );
		//corona mote
		glow( pm, 54, 8, 3 );
		//an ember
		g( pm, 17, 16, 255 );
		//long streaks: the same drops, drawn out by speed
		for (int k = 0; k < 7; k++){
			int al = 40 + k * 32;
			a( pm, 38, 16 + k, al );
			a( pm, 40 + (k >= 4 ? 1 : 0), 16 + k, al );
			a( pm, 43 + (k >= 5 ? 2 : (k >= 2 ? 1 : 0)), 16 + k, al );
		}
		//ripples: a ring that widens and thins where a drop meets water
		a( pm, 20, 16, 200 ); a( pm, 19, 17, 150 ); a( pm, 21, 17, 150 );
		a( pm, 24, 16, 140 ); a( pm, 26, 16, 140 ); a( pm, 23, 17, 160 ); a( pm, 27, 17, 160 ); a( pm, 25, 18, 120 );
		a( pm, 30, 16, 80 ); a( pm, 34, 16, 80 ); a( pm, 29, 17, 100 ); a( pm, 35, 17, 100 ); a( pm, 31, 18, 70 ); a( pm, 33, 18, 70 );

		//flames: drawn by hand, a pointed tip over a full body and a rounded base
		flame( pm, 0, 24, FLAME_ART_1 );
		flame( pm, 4, 24, FLAME_ART_2 );
		flame( pm, 10, 24, FLAME_ART_3 );
		//a spark: two pixels, the lower one hotter
		a( pm, 18, 24, 160 ); a( pm, 18, 25, 255 );
		//the heat ray: a thread of air that snakes as it rises
		ray( pm, 20, 24, 3, 14 );
		//cloud puffs: lumpy bodies, denser than a wisp and rounder
		puff( pm, 24, 24, 9, 6, 5 );
		puff( pm, 34, 24, 14, 8, 9 );
	}

	//the flames, a pixel at a time: '.' nothing, then dim, mid and hot
	private static final String[] FLAME_ART_1 = {
			".2.",
			"133",
			"233",
			".2.",
	};
	private static final String[] FLAME_ART_2 = {
			"..2..",
			"..3..",
			".133.",
			".233.",
			"13332",
			"23332",
			".232.",
	};
	private static final String[] FLAME_ART_3 = {
			"...2...",
			"...3...",
			"..133..",
			"..233..",
			".13332.",
			".23332.",
			"1333321",
			"2333332",
			".23332.",
			"..232..",
	};

	/** a hand-drawn flame: the digits are its three brightnesses */
	private static void flame( Pixmap pm, int x, int y, String[] art ){
		for (int yy = 0; yy < art.length; yy++){
			String row = art[yy];
			for (int xx = 0; xx < row.length(); xx++){
				int al;
				switch (row.charAt( xx )){
					case '1': al = 110; break;
					case '2': al = 185; break;
					case '3': al = 255; break;
					default: continue;
				}
				a( pm, x + xx, y + yy, al );
			}
		}
	}

	/** a rising thread of hot air: one or two pixels wide, snaking up the frame */
	private static void ray( Pixmap pm, int x, int y, int w, int h ){
		float half = (w - 1) / 2f;
		for (int yy = 0; yy < h; yy++){
			float t = (yy + 0.5f) / h;
			//it wanders left and right as it climbs, and thins out at both ends
			float off = (float)Math.sin( t * Math.PI * 2.6 ) * half;
			float fade = (float)Math.pow( Math.sin( t * Math.PI ), 0.7 );
			int cx = Math.round( half + off );
			int al = Math.round( 225 * fade );
			if (al < 25) continue;
			a( pm, x + cx, y + yy, Math.min( 255, al ) );
			//a fainter pixel trailing the bend gives it width where it turns
			int lean = off > 0.35f ? -1 : (off < -0.35f ? 1 : 0);
			if (lean != 0 && cx + lean >= 0 && cx + lean < w){
				a( pm, x + cx + lean, y + yy, Math.min( 255, Math.round( al * 0.5f ) ) );
			}
		}
	}

	/** a cloud body: overlapping lumps, solid in the middle, dithered at the rim */
	private static void puff( Pixmap pm, int x, int y, int w, int h, long seed ){
		//three lumps across the frame make a cumulus rather than an ellipse
		float[][] lumps = {
				{ w * 0.30f, h * 0.62f, h * 0.46f },
				{ w * 0.55f, h * 0.42f, h * 0.54f },
				{ w * 0.78f, h * 0.66f, h * 0.40f },
		};
		for (int yy = 0; yy < h; yy++){
			for (int xx = 0; xx < w; xx++){
				float best = 0;
				for (float[] l : lumps){
					float d = (float)Math.hypot( (xx + 0.5f - l[0]) / (l[2] * 1.35f), (yy + 0.5f - l[1]) / l[2] );
					best = Math.max( best, 1f - d );
				}
				if (best <= 0) continue;
				//flat and bright through the body, shaded along the underside
				float shade = 1f - 0.35f * ((yy + 0.5f) / h);
				int al = Math.round( 235 * Math.min( 1f, best * 2.2f ) * shade );
				//a dithered rim instead of a soft one
                if (best < 0.22f && noise( seed, xx, yy ) < 0.55f) continue;
				if (al < 30) continue;
				a( pm, x + xx, y + yy, Math.min( 255, al ) );
			}
		}
	}

	private static void glow( Pixmap pm, int x, int y, int size ){
		float c = (size - 1) / 2f, rad = size / 2f + 0.3f;
		for (int yy = 0; yy < size; yy++){
			for (int xx = 0; xx < size; xx++){
				double d = Math.hypot( xx - c, yy - c ) / rad;
				if (d >= 1) continue;
				double k = Math.pow( 1 - d, 1.6 );
				a( pm, x + xx, y + yy, (int)(255 * k) );
			}
		}
		a( pm, x + (size - 1) / 2, y + (size - 1) / 2, 255 );
	}

	private static void wisp( Pixmap pm, int x, int y, int w, int h, long seed ){
		float cx = (w - 1) / 2f, cy = (h - 1) / 2f;
		for (int yy = 0; yy < h; yy++){
			for (int xx = 0; xx < w; xx++){
				double d = Math.hypot( (xx - cx) / (w / 2f), (yy - cy) / (h / 2f + 0.3f) );
				if (d >= 1) continue;
				float n = noise( seed, xx, yy );
				int al = (int)(200 * Math.pow( 1 - d, 1.3 ) * (0.55f + 0.45f * n));
				if (al < 20) continue;
				a( pm, x + xx, y + yy, Math.min( 255, al ) );
			}
		}
	}

	// -------------------------------------------------------------- helpers

	/** whether the hero can see the tile under a world point; particles outside it
	 *  are hidden rather than killed, the fog above does the real masking */
	public static boolean visible( float x, float y ){
		if (Dungeon.level == null || Dungeon.level.heroFOV == null) return true;
		int cx = (int)(x / DungeonTilemap.SIZE), cy = (int)(y / DungeonTilemap.SIZE);
		if (x < 0 || y < 0 || cx >= Dungeon.level.width()) return false;
		int cell = cx + cy * Dungeon.level.width();
		return cell >= 0 && cell < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[cell];
	}

	/**
	 * The wind's gusting, -1..1 over a few seconds: slow swells with a quicker
	 * flutter on top. Rain leans harder and falls faster in a gust, snow and leaves
	 * are carried further, the fireflies hold on.
	 */
	public static float gust(){
		float t = Game.timeTotal;
		return (float)(0.6 * Math.sin( t * 0.37 ) + 0.3 * Math.sin( t * 1.13 + 1.7 ) + 0.1 * Math.sin( t * 3.1 + 0.4 ));
	}

	// -------------------------------------------------------------- rainbow

	/**
	 * The bow at native size: seven bands in pixel colours with a dithered seam
	 * between each, brighter sky inside the primary, two faint supernumerary
	 * fringes under it, and the wider, fainter, reversed secondary above. cx and
	 * baseY are the centre of the circle (the anti-solar point sits below the
	 * horizon, so baseY is under the texture); the bands fade toward the ends.
	 */
	public static void paintBow( Pixmap pm, int w, int h, int cx, int baseY, int radius ){
		int[] cols = { 0xE84848, 0xE89040, 0xE8D850, 0x58C860, 0x4CA8E8, 0x5468D8, 0x8C58C8 };
		int band = Math.max( 2, Math.round( radius / 26f ) );
		int total = band * cols.length;
		float rOuter = radius, rInner = radius - total;
		float r2Outer = radius * 1.17f, r2Inner = r2Outer - total * 1.25f;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				double d = Math.hypot( x + 0.5 - cx, y + 0.5 - baseY );
				if (y + 0.5 > baseY) continue;
				//how close to the ends of the arc: the bow fades where it meets the ground
				float ang = (float)Math.abs( Math.atan2( baseY - (y + 0.5), x + 0.5 - cx ) - Math.PI / 2 );
				float endFade = clamp01( (float)((Math.PI / 2 - ang) / 0.45) );
				if (endFade <= 0) continue;
				int col = -1; int alpha = 0;
				if (d < rOuter && d >= rInner){
					float t = (float)((rOuter - d) / band);
					int i = Math.min( cols.length - 1, (int)t );
					float f = t - i;
					col = cols[i];
					//a dithered seam into the next band
					if (f > 0.7f && i < cols.length - 1 && ((x + y) & 1) == 0) col = cols[i + 1];
					alpha = 0xA8;
				} else if (d < rInner && d >= rInner - band * 4){
					//supernumerary fringes: pale violet, then pale green, thin
					float t = (float)((rInner - d) / band);
					if (t < 1f){ col = 0xB090E0; alpha = 0x40; }
					else if (t < 2f){ col = 0x80D0A0; alpha = 0x30; }
					else if (t < 3f){ col = 0xC0A0E0; alpha = 0x1C; }
					else { col = 0x90D0B0; alpha = 0x14; }
				} else if (d < rInner - band * 4){
					//the sky inside the bow is brighter
					col = 0xFFFFFF;
					alpha = Math.round( 0x16 * clamp01( (float)((d - rInner * 0.55) / (rInner * 0.45)) ) );
				} else if (d < r2Outer && d >= r2Inner){
					float t = (float)((r2Outer - d) / (band * 1.25f));
					int i = Math.min( cols.length - 1, (int)t );
					col = cols[cols.length - 1 - i];
					alpha = 0x48;
				}
				if (col < 0 || alpha <= 0) continue;
				alpha = Math.round( alpha * endFade );
				if (alpha > 0) pm.drawPixel( x, y, rgba( col, alpha ) );
			}
		}
	}

	// --------------------------------------------------------------- aurora

	/**
	 * One curtain of aurora, w x h, at a phase of its slow folding: hanging rays of
	 * varying brightness, green at the bottom edge, teal through the body, violet
	 * and rose at the top, thinning to nothing along ragged lower fringes. Painted
	 * a few times at different phases and cycled, it flows.
	 */
	public static void paintCurtain( Pixmap pm, int ox, int w, int h, long seed, float phase ){
		int[] cols = { 0x40E888, 0x38D8B0, 0x48B8E0, 0x8070E0, 0xC060C0 };
		for (int x = 0; x < w; x++){
			float u = x / (float)w;
			//the fold: where the curtain hangs low and where it lifts
			double fold = Math.sin( u * 6.2 + phase ) * 0.5 + Math.sin( u * 13.1 + phase * 1.7 + noise( seed, 1, 1 ) * 6 ) * 0.25;
			//the rays: each column its own brightness, some blazing, some dark gaps.
			//they slide slowly with the phase, so one frame is a small step from the
			//last and the ring of frames closes on itself
			float slide = (float)(Math.sin( phase ) * 2.2);
			float rn = SkyPaint.smooth1( seed ^ 11, x + slide, 3f );
			double ray = 0.3 + 0.7 * rn;
			if (SkyPaint.smooth1( seed ^ 17, x * 0.5f + slide * 0.7f, 5f ) > 0.86) ray *= 1.35;
			if (rn < 0.12) continue;
			//ends of the curtain thin out
			float end = clamp01( (float)Math.min( u / 0.12, (1 - u) / 0.12 ) );
			int top = Math.round( (float)(h * (0.06 + 0.10 * (fold + 1))) );
			int len = Math.round( (float)(h * (0.50 + 0.30 * fold) * (0.6 + 0.4 * ray)) );
			if (len < 3) continue;
			for (int y = top; y < top + len && y < h; y++){
				float v = (y - top) / (float)len;
				//a rose rim at the very top, violet under it, then the green body that
				//carries most of the light, dimming down the ray
				int col;
				if (v < 0.10f) col = cols[4];
				else if (v < 0.24f) col = mix( cols[3], cols[1], (v - 0.10f) / 0.14f );
				else col = mix( cols[1], cols[0], Math.min( 1f, (v - 0.24f) / 0.5f ) );
				if (((x + y) & 1) == 0) col = mix( col, v < 0.24f ? cols[2] : 0x30C870, 0.35f );
				float fall = v < 0.35f ? 0.6f + 0.4f * (v / 0.35f) : (float)Math.pow( 1 - (v - 0.35f) / 0.65f, 1.3 );
				int alpha = Math.round( 200 * fall * (float)ray * end );
				//a bright rim along the top, flickering
				if (v < 0.08f) alpha = Math.min( 255, Math.round( alpha * (1.1f + 0.5f * noise( seed, x, y )) ) );
				//a dithered fade at the bottom instead of a hard end
				if (v > 0.75f && noise( seed ^ 5, x, y ) > (1 - v) / 0.25f) continue;
				if (alpha > 8) pm.drawPixel( ox + x, y, rgba( col, Math.min( 255, alpha ) ) );
			}
		}
	}
}
