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

package xyz.gabriwar.warpedpixeldungeon.scenes.sky;

import com.badlogic.gdx.graphics.Pixmap;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The sky scene shrunk to a HUD window. The same context and the same dome paint
 * the light; the horizon, the weather and the instruments under the window are
 * drawn small enough to read at a glance. Everything is native pixels. The sun is
 * the game's 9x9 icon, laid over the sky by whoever shows the window, so this
 * class only says where it goes and how faint it is; the moon is painted in its
 * true shape, and its phase icon sits in a badge in the corner.
 *
 * The window is two layers: the sky (gradient, stars, the moon, clouds, aurora,
 * rainbow) and, over the sun, the land with its weather (horizon, fog, rain,
 * snow, lightning, fireflies, leaves). That is what lets the sun set behind the
 * hills.
 */
public final class SkyMiniature {

	private SkyMiniature(){}

	/** the stone frame round the window, in pixels */
	public static final int FRAME = 2;

	/** rows of land under the horizon */
	public static final int LAND = 7;

	/** a live context for a window whose inside is w x h */
	public static SkyContext capture( int w, int h ){
		SkyContext c = SkyContext.capture( w, h );
		fit( c );
		return c;
	}

	/** re-aims a context at the miniature's proportions: a low horizon, the sun
	 *  rising and setting close to the frame, the moon where its age puts it */
	public static void fit( SkyContext c ){
		c.skyHorizon = c.H - LAND;
		c.edgeMargin = 5f;
		c.finish();
		placeMoon( c );
	}

	/** the moon is with the sun when new and opposite it when full, so it rises
	 *  later every night and, half the month, hangs pale in the day sky */
	public static void placeMoon( SkyContext c ){
		double ts = thetaOf( c.f );
		double tm = ts - c.moonAge * 2 * Math.PI;
		tm = ((tm % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
		float e = (float)Math.sin( tm );
		c.moonUp = e > 0.05f;
		if (c.moonUp){
			float m = c.edgeMargin;
			c.moonX = m + (float)(tm / Math.PI) * (c.W - 2 * m);
			c.moonY = c.skyHorizon - e * c.H * 0.5f;
		}
	}

	private static double thetaOf( float f ){
		if (f < 0.695f) return (f / 0.695f) * Math.PI;
		return Math.PI + ((f - 0.695f) / 0.305f) * Math.PI;
	}

	// ------------------------------------------------------------- icons

	/** top-left of a 9x9 icon centred on the sun, in window coordinates; null when
	 *  the sun is down or the cloud has swallowed it */
	public static int[] sunIcon( SkyContext c ){
		if (!c.sunUp || c.sunDisc < 0.12f) return null;
		return new int[]{ Math.round( c.sunX ) - 4, Math.round( c.sunY ) - 4 };
	}

	public static float sunAlpha( SkyContext c ){
		return clamp01( c.sunDisc ) * (c.fog ? 0.5f : 1f);
	}

	/** which frame of the 9x9 sun/moon strip shows tonight's phase: 1..24 round the
	 *  month, 26 for a moon in eclipse */
	public static int moonFrame( SkyContext c ){
		if (c.lunarEclipse) return 26;
		return 1 + Math.round( c.moonAge * 24f ) % 24;
	}

	/** the moon in the sky, in its true shape: the lit part only, so by day it is a
	 *  pale ghost and at night a lamp. Nothing is drawn near the sun or when new */
	private static void skyMoon( SkyContext c, Pixmap pm ){
		if (!c.moonUp) return;
		if (c.sunUp && Math.hypot( c.moonX - c.sunX, c.moonY - c.sunY ) < 14) return;
		float R = 3f;
		//the shadow disc slides across with the age: off the left when waxing, off
		//the right when waning, gone at the full
		float off = c.moonAge < 0.5f ? -R * (1.6f - 3.2f * c.moonAge) : R * (3.2f * c.moonAge - 1.6f);
		if (Math.abs( off ) > 1.5f * R) return;
		boolean full = Math.abs( off ) < 0.12f * R;
		float a = (0.30f + 0.70f * c.nightness) * (1f - 0.9f * c.cloudCover * c.cloudCover) * (c.fog ? 0.6f : 1f);
		if (a < 0.03f) return;
		boolean blood = c.lunarEclipse && c.nightness > 0.5f;
		int lit = blood ? 0xC05038 : mix( 0xEEF1F8, c.zenith, 0.35f * (1f - c.nightness) );
		int limb = blood ? 0x8A2C20 : mix( 0xC8CDDA, c.zenith, 0.35f * (1f - c.nightness) );
		int dark = mix( c.zenith, 0x151c2c, 0.5f );
		float cx = c.moonX, cy = c.moonY;
		for (int y = (int)Math.floor( cy - R ); y <= Math.ceil( cy + R ); y++){
			for (int x = (int)Math.floor( cx - R ); x <= Math.ceil( cx + R ); x++){
				double d = Math.hypot( x + 0.5 - cx, y + 0.5 - cy );
				if (d >= R + 0.3) continue;
				boolean on = full || Math.hypot( x + 0.5 - cx - off, y + 0.5 - cy ) >= R;
				if (on){
					int col = d > R - 1.1 ? limb : lit;
					blend( pm, x, y, col, Math.round( 255 * a * (d > R ? (float)(R + 0.3 - d) / 0.3f : 1f) ) );
				} else if (c.nightness > 0.6f){
					blend( pm, x, y, dark, Math.round( 120 * a ) );
				}
			}
		}
		//a soft glow round a bright moon
		if (c.nightness > 0.5f && !blood){
			for (int y = (int)Math.floor( cy - R - 2 ); y <= Math.ceil( cy + R + 2 ); y++){
				for (int x = (int)Math.floor( cx - R - 2 ); x <= Math.ceil( cx + R + 2 ); x++){
					double d = Math.hypot( x + 0.5 - cx, y + 0.5 - cy );
					if (d >= R + 0.3 && d < R + 2.5) blend( pm, x, y, 0xC8D4F0, Math.round( 55 * a * c.moonBright * (float)(1 - (d - R) / 2.5) ) );
				}
			}
		}
	}

	// -------------------------------------------------------------- frame

	/** a carved stone rim: black outline, a bevel that catches light on the top and
	 *  the left, rounded corners left open so the panel shows through */
	public static void paintFrame( Pixmap pm, int x, int y, int w, int h ){
		int dark = 0x0c0a0e, lit = 0x6e6878, mid = 0x3a3640, low = 0x1e1a22;
		//outer ring
		for (int i = 0; i < w; i++){ px( pm, x + i, y, dark ); px( pm, x + i, y + h - 1, dark ); }
		for (int i = 0; i < h; i++){ px( pm, x, y + i, dark ); px( pm, x + w - 1, y + i, dark ); }
		//bevel ring
		for (int i = 1; i < w - 1; i++){ px( pm, x + i, y + 1, lit ); px( pm, x + i, y + h - 2, low ); }
		for (int i = 1; i < h - 1; i++){ px( pm, x + 1, y + i, mid ); px( pm, x + w - 2, y + i, low ); }
		px( pm, x + 1, y + 1, lit ); px( pm, x + w - 2, y + 1, mid ); px( pm, x + 1, y + h - 2, mid );
		//open corners
		pm.drawPixel( x, y, 0 ); pm.drawPixel( x + w - 1, y, 0 );
		pm.drawPixel( x, y + h - 1, 0 ); pm.drawPixel( x + w - 1, y + h - 1, 0 );
	}

	/** the dark plates the weather caption (captionW x captionH of text) and the
	 *  moon badge sit on, inside a window whose inside is w wide at (ox, oy) */
	public static void paintPlates( Pixmap pm, int ox, int oy, int w, int captionW, int captionH ){
		plate( pm, ox + 1, oy + 1, captionW + 3, captionH + 2 );
		plate( pm, ox + w - 12, oy + 1, 11, 11 );
	}

	private static void plate( Pixmap pm, int x, int y, int w, int h ){
		for (int yy = 0; yy < h; yy++){
			for (int xx = 0; xx < w; xx++){
				boolean edge = xx == 0 || yy == 0 || xx == w - 1 || yy == h - 1;
				boolean corner = (xx == 0 || xx == w - 1) && (yy == 0 || yy == h - 1);
				if (corner) continue;
				blend( pm, x + xx, y + yy, edge ? 0x000000 : 0x06060a, edge ? 0xB0 : 0x80 );
			}
		}
	}

	// ---------------------------------------------------------------- sky

	/** the sky layer of a c.W x c.H window, painted into pm at (ox, oy) */
	public static void paintSky( SkyContext c, Pixmap pm, int ox, int oy, int anim ){
		Pixmap sky = fresh( c.W, c.H );
		SkyDome.paint( c, sky );
		stars( c, sky, anim );
		if (c.aurora && c.nightness > 0.5f){
			Pixmap au = fresh( c.W, c.skyHorizon );
			SkyDome.paintAurora( au, c.W, c.skyHorizon, c.seed );
			blit( sky, au, 0, 0, 0.75f * clamp01( (c.nightness - 0.5f) / 0.3f ) );
			au.dispose();
		}
		if (c.rainbow && c.sunUp && c.light > 0.3f && c.cloudCover < 0.85f){
			int antiX = Math.round( c.W - c.sunX );
			SkyDome.paintRainbow( sky, c.W, c.skyHorizon + 1, antiX, c.skyHorizon, Math.round( c.W * 0.32f ) );
		}
		skyMoon( c, sky );
		clouds( c, sky, anim );
		blit( pm, sky, ox, oy, 1f );
		sky.dispose();
	}

	private static Pixmap fresh( int w, int h ){
		Pixmap p = new Pixmap( w, h, Pixmap.Format.RGBA8888 );
		p.setBlending( Pixmap.Blending.None );
		p.setColor( 0 );
		p.fill();
		return p;
	}

	private static void stars( SkyContext c, Pixmap pm, int anim ){
		float a = clamp01( (0.45f - c.light) / 0.35f ) * (1f - c.cloudCover * c.cloudCover);
		if (a < 0.05f) return;
		long s = c.seed ^ 0x57A55L;
		int H = c.skyHorizon;
		int n = c.W * H / 34;
		for (int i = 0; i < n; i++){
			int x = hash( s, i, 1 ) % c.W, y = hash( s, i, 2 ) % H;
			float m = noise( s, i, 3 );
			int col = m > 0.9f ? 0xffe4b8 : (m > 0.82f ? 0xc0ccff : 0xeef2ff);
			//thin air up high, thick low down; the brightest ones twinkle
			float low = 0.35f + 0.65f * (1f - y / (float)H);
			int al = Math.round( (60 + 180 * m * m) * a * low );
			if (m > 0.93f && ((anim / 2 + i) % 5) == 0) al = al * 2 / 3;
			blend( pm, x, y, col, al );
			if (m > 0.94f){
				blend( pm, x + 1, y, col, al / 3 ); blend( pm, x - 1, y, col, al / 3 );
				blend( pm, x, y + 1, col, al / 3 ); blend( pm, x, y - 1, col, al / 3 );
			}
		}
	}

	private static void clouds( SkyContext c, Pixmap pm, int anim ){
		float cover = c.cloudCover;
		if (cover < 0.12f) return;
		int W = c.W, H = c.skyHorizon;
		long s = c.seed ^ 0xC10DL;

		int lite, base, shade;
		if (c.storm){
			lite = mix( 0x6a7488, c.sunColor, 0.12f * c.light );
			base = 0x4a5468; shade = 0x333a4a;
		} else if (c.light < 0.25f){
			float ml = c.moonUp ? c.moonBright : 0.2f;
			int nightBase = mix( c.zenith, 0x8a94b4, 0.30f + 0.10f * ml );
			lite = mix( nightBase, c.moonColor, 0.28f * ml );
			base = nightBase;
			shade = mix( nightBase, c.zenith, 0.45f );
		} else {
			lite = mix( 0xF8FAFE, c.sunColor, 0.55f * c.warmth );
			base = mix( 0xE4EAF4, mix( c.sunColor, 0xE0C8D8, 0.5f ), 0.35f * c.warmth );
			shade = mix( mix( 0xB4C0D6, c.horizonAvg, 0.45f ), 0x907090, 0.35f * c.warmth );
		}
		//the light comes from wherever the sun is, even after it has set
		boolean litLeft = c.litSide() < 0;

		//a deck: from three quarters cover the top of the sky is one cloud, with a
		//ragged underside and, until it closes, a few gaps of sky
		if (cover >= 0.72f){
			int deckH = 3 + Math.round( (cover - 0.72f) / 0.28f * 3f );
			for (int x = 0; x < W; x++){
				int bottom = deckH + Math.round( (smooth1( s, x + anim * 0.15f * c.wind * c.windDir, 7f ) - 0.5f) * 3f );
				boolean gap = cover < 0.92f && smooth1( s ^ 3, x, 11f ) > 0.78f;
				if (gap) bottom -= 3;
				for (int y = 0; y <= bottom && y < H - 2; y++){
					int col = y == 0 ? lite : (y >= bottom - 1 ? shade : base);
					if (c.storm && y == bottom) col = scale( shade, 0.8f );
					px( pm, x, y, col );
				}
				if (bottom >= 0 && bottom + 1 < H) blend( pm, x, bottom + 1, shade, 0x70 );
			}
		}

		//loose puffs, drifting on the wind
		int n = cover < 0.3f ? 1 : (cover < 0.55f ? 2 : 3);
		float drift = anim * (0.06f + 0.03f * c.wind) * c.windDir;
		for (int k = 0; k < n; k++){
			int cw = 8 + hash( s, k, 1 ) % 7;
			int ch = 3 + cw / 6;
			float span = W + cw + 6;
			float fx = ((hash( s, k, 2 ) % (int)span) + drift) % span;
			if (fx < 0) fx += span;
			int cx = (int)Math.floor( fx ) - cw - 3;
			int top = (cover >= 0.72f ? 4 : 1) + hash( s, k, 3 ) % Math.max( 1, H - ch - (cover >= 0.72f ? 6 : 4) );
			puff( pm, cx, top, cw, ch, lite, base, shade, litLeft, s ^ (k * 31L) );
		}
	}

	/** a few overlapping round puffs on a flat keel, three tones, lit from the sun's side */
	private static void puff( Pixmap pm, int x0, int y0, int w, int h, int lite, int base, int shade, boolean litLeft, long s ){
		int keel = y0 + h - 1;
		int nP = 2 + hash( s, 1, 1 ) % 2;
		for (int p = 0; p < nP; p++){
			int r = Math.max( 2, h - 1 - hash( s, p, 2 ) % 2 );
			int cx = x0 + r + hash( s, p, 3 ) % Math.max( 1, w - 2 * r );
			int cy = keel - r + 1;
			for (int y = cy - r; y <= keel; y++){
				for (int x = cx - r; x <= cx + r; x++){
					double nx = (x - cx) / (double)r, ny = (y - cy) * 1.3 / r;
					double d = Math.hypot( nx, ny );
					if (d >= 1) continue;
					if (d > 0.8 && (hash( s, x + 3, y + 5 ) % 4) == 0) continue;
					float ex = (float)((litLeft ? -nx : nx) * 0.5 - ny * 0.9);
					px( pm, x, y, ex > 0.35f ? lite : (ex > -0.3f ? base : shade) );
				}
			}
		}
		for (int x = x0; x < x0 + w; x++){
			if (get( pm, x, keel - 1 ) != -1) px( pm, x, keel, shade );
		}
	}

	// --------------------------------------------------------------- land

	/** the layer over the icons: horizon, weather in the air, and the small life */
	public static void paintOver( SkyContext c, Pixmap pm, int ox, int oy, int anim ){
		Pixmap ov = fresh( c.W, c.H );
		land( c, ov, anim );
		if (c.fog) fog( c, ov );
		precip( c, ov, anim );
		ambient( c, ov, anim );
		if (c.storm) lightning( c, ov, anim );
		blit( pm, ov, ox, oy, 1f );
		ov.dispose();
	}

	private static void land( SkyContext c, Pixmap pm, int anim ){
		int W = c.W, H = c.H, hz = c.skyHorizon;
		long s = c.seed ^ 0x1A4DL;
		Biome b = c.underground ? Biome.MOUNTAIN : c.biome;
		boolean snow = c.snow;

		int[] far = new int[W], near = new int[W];
		int farBase, nearBase, groundBase;
		switch (b){
			case MOUNTAIN:
				farBase = 0x5a6480; nearBase = 0x3a4256; groundBase = 0x2c3242;
				for (int x = 0; x < W; x++){
					float p = smooth1( s, x, 9f ), q = smooth1( s ^ 5, x, 4f );
					far[x] = hz - 3 - Math.round( 5f * Math.abs( p * 2 - 1 ) + 2f * q );
					float n = smooth1( s ^ 9, x, 6f );
					near[x] = hz - Math.round( 3.5f * Math.abs( n * 2 - 1 ) );
				}
				break;
			case FOOTHILLS:
				farBase = 0x6e7488; nearBase = 0x4e5a52; groundBase = 0x3a4438;
				for (int x = 0; x < W; x++){
					far[x] = hz - 2 - Math.round( 3f * smooth1( s, x, 13f ) );
					near[x] = hz - Math.round( 2f * smooth1( s ^ 7, x, 8f ) );
				}
				break;
			case DESERT:
				farBase = 0xd8b478; nearBase = 0xb88a50; groundBase = 0x9a7040;
				for (int x = 0; x < W; x++){
					far[x] = hz - 1 - Math.round( 3f * smooth1( s, x, 16f ) );
					near[x] = hz + 1 - Math.round( 2f * smooth1( s ^ 7, x, 9f ) );
				}
				break;
			case TUNDRA:
				farBase = 0xa8b2c2; nearBase = 0x8c96a6; groundBase = 0x707a88;
				for (int x = 0; x < W; x++){ far[x] = hz - 1 - Math.round( smooth1( s, x, 15f ) ); near[x] = hz; }
				break;
			case SNOWFIELD:
				farBase = 0xd8e2ec; nearBase = 0xc4d0dc; groundBase = 0xa8b6c4;
				for (int x = 0; x < W; x++){ far[x] = hz - 1 - Math.round( 1.5f * smooth1( s, x, 12f ) ); near[x] = hz; }
				break;
			case FOREST:
				farBase = c.autumn() ? 0x8a5a2e : (c.winter() ? 0x5c5450 : 0x3e7040);
				nearBase = c.autumn() ? 0x9a5a24 : (c.winter() ? 0x4a423e : (c.spring() ? 0x3c8038 : 0x2a6030));
				groundBase = c.winter() ? 0x3a3432 : 0x22401e;
				for (int x = 0; x < W; x++){
					far[x] = hz - 2 - Math.round( 2f * smooth1( s, x, 5f ) );
					near[x] = hz + 1;
				}
				break;
			case SWAMP:
				farBase = 0x4a5a48; nearBase = 0x2a3e34; groundBase = 0x22302a;
				for (int x = 0; x < W; x++){ far[x] = hz - 1 - Math.round( 2f * smooth1( s, x, 7f ) ); near[x] = hz + 1; }
				break;
			case OCEAN: case RIVER: case BEACH:
				farBase = 0x2e5e8a; nearBase = 0x1e4e78; groundBase = b == Biome.BEACH ? 0xd6b068 : 0x163a5a;
				for (int x = 0; x < W; x++){ far[x] = hz; near[x] = hz + 1; }
				break;
			default: //PLAINS, MEADOW
				farBase = c.autumn() ? 0xa08a48 : (c.winter() ? 0x8a8a80 : 0x6e9a4a);
				nearBase = c.autumn() ? 0x8a7038 : (c.winter() ? 0x6a6a62 : (c.spring() ? 0x5a9a44 : 0x4e8a3c));
				groundBase = c.winter() ? 0x4a4a44 : 0x385a2a;
				for (int x = 0; x < W; x++){
					far[x] = hz - 1 - Math.round( 1.5f * smooth1( s, x, 14f ) );
					near[x] = hz + 1 - Math.round( smooth1( s ^ 7, x, 9f ) );
				}
		}
		if (snow){
			farBase = mix( farBase, 0xdde6ee, 0.6f ); nearBase = mix( nearBase, 0xc8d4de, 0.55f ); groundBase = mix( groundBase, 0xb0bcc8, 0.5f );
		}
		boolean water = b == Biome.OCEAN || b == Biome.RIVER || b == Biome.BEACH || b == Biome.SWAMP;

		//far layer, dissolving into the sky
		float hzFar = c.haze( 0.7f );
		for (int x = 0; x < W; x++){
			int col = mix( c.lit( farBase, 0.55f ), c.horizonColorAt( x ), hzFar );
			for (int y = Math.max( 0, far[x] ); y < H; y++){
				int cc = col;
				if (b == Biome.MOUNTAIN && (snow || c.winter() || far[x] < hz - 6) && y <= far[x] + 1) cc = mix( col, 0xeef4fa, 0.65f );
				px( pm, x, y, cc );
			}
		}
		//near layer
		float hzNear = c.haze( 0.25f );
		for (int x = 0; x < W; x++){
			int col = mix( c.lit( nearBase, 0.5f ), c.horizonColorAt( x ), hzNear );
			int rim = mix( col, 0xffffff, 0.18f );
			for (int y = Math.max( 0, near[x] ); y < H; y++){
				int cc = y == near[x] ? rim : col;
				if (y >= hz + 4) cc = mix( cc, c.lit( groundBase, 0.4f ), 0.35f + 0.25f * (y - hz - 4) );
				if (b == Biome.MOUNTAIN && snow && y <= near[x] + 1) cc = mix( col, 0xe4ecf4, 0.5f );
				px( pm, x, y, cc );
			}
		}

		//the water: flat, mirroring the sky, with a glitter path under the light
		if (water){
			for (int y = hz + 1; y < H; y++){
				float depth = (y - hz) / (float)(H - hz);
				for (int x = 0; x < W; x++){
					int skyCol = c.horizonColorAt( x );
					int wc = mix( mix( c.lit( nearBase, 0.6f ), skyCol, 0.45f - 0.3f * depth ), c.zenith, 0.25f * depth );
					if (b == Biome.SWAMP) wc = mix( wc, 0x2a3e34, 0.6f );
					if (b == Biome.BEACH && y >= H - 2) wc = c.lit( groundBase, 0.6f );
					px( pm, x, y, wc );
				}
			}
			float lx = c.sunUp && c.light > 0.05f ? c.sunX : (c.moonUp ? c.moonX : -99);
			if (lx > -1){
				int gc = c.sunUp && c.light > 0.05f ? mix( 0xffffff, c.sunColor, 0.5f ) : mix( 0xffffff, c.moonColor, 0.5f );
				float str = c.sunUp && c.light > 0.05f ? (0.5f + 0.5f * c.sunDisc) : 0.6f * c.moonBright;
				for (int y = hz + 1; y < H - (b == Biome.BEACH ? 2 : 0); y++){
					int spread = 1 + (y - hz) / 2;
					for (int k = -spread; k <= spread; k++){
						if ((hash( s, k + 7, y + anim / 2 ) % 3) != 0) continue;
						blend( pm, Math.round( lx ) + k, y, gc, (int)(150 * str) );
					}
				}
			}
			if (b == Biome.SWAMP){
				//reeds
				for (int i = 0; i < W / 6; i++){
					int x = hash( s, i, 21 ) % W, hgt = 2 + hash( s, i, 22 ) % 3;
					vline( pm, x, hz + 2 - hgt, hz + 2, c.lit( 0x5a6a3a, 0.4f ) );
				}
			}
		}

		//trees on the forest's near edge: pines and round crowns
		if (b == Biome.FOREST){
			int crown = c.lit( nearBase, 0.6f ), dark = c.lit( scale( nearBase, 0.7f ), 0.3f );
			if (snow){ crown = mix( crown, 0xdde6ee, 0.35f ); }
			for (int x = 1; x < W - 1; x += 3 + hash( s, x, 30 ) % 3){
				boolean pine = (hash( s, x, 31 ) % 3) != 0;
				int hgt = 3 + hash( s, x, 32 ) % 3;
				int top = hz + 1 - hgt;
				if (pine){
					for (int y = 0; y < hgt; y++){
						int half = Math.min( 2, y / 2 + (y > 0 ? 1 : 0) );
						if (y == 0) half = 0;
						for (int k = -half; k <= half; k++) px( pm, x + k, top + y, k == -half && c.litSide() < 0 || k == half && c.litSide() > 0 ? crown : (k == 0 ? dark : crown) );
					}
					if (snow) px( pm, x, top, 0xeef4fa );
				} else {
					int r = 2;
					if (c.winter() && !snow){
						//bare: a trunk and a few twigs
						vline( pm, x, top + 1, hz + 1, dark ); px( pm, x - 1, top + 1, dark ); px( pm, x + 1, top, dark );
					} else {
						disc( pm, x, top + r, r, crown );
						px( pm, x + (c.litSide() > 0 ? 1 : -1), top + 1, mix( crown, 0xffffff, 0.25f ) );
						px( pm, x, top + 2 * r, dark );
						if (c.autumn()) px( pm, x - 1, top + 2, 0xd8983a );
						if (c.spring()) px( pm, x + 1, top + 2, 0xe8a0c0 );
					}
				}
			}
		}

		//grass and flowers on the open ground
		if (b == Biome.PLAINS || b == Biome.MEADOW){
			int tuft = c.lit( mix( nearBase, 0x203818, 0.4f ), 0.4f );
			for (int i = 0; i < W / 3; i++){
				int x = hash( s, i, 40 ) % W;
				vline( pm, x, near[x] - 1, near[x], tuft );
				if (c.spring() && !snow && (hash( s, i, 41 ) % 3) == 0){
					px( pm, x, near[x] - 1, (hash( s, i, 42 ) & 1) == 0 ? 0xe890b0 : 0xf0e060 );
				}
				if (c.summer() && !snow && (hash( s, i, 41 ) % 5) == 0) px( pm, x, near[x] - 1, 0xf0e060 );
			}
		}
		if (b == Biome.DESERT){
			//one cactus, and the dune crests catch the light
			int cx = W * 2 / 3;
			int cac = c.lit( 0x4a7a3a, 0.5f );
			vline( pm, cx, near[cx] - 4, near[cx], cac ); px( pm, cx - 1, near[cx] - 3, cac ); px( pm, cx - 1, near[cx] - 2, cac ); px( pm, cx + 1, near[cx] - 2, cac ); px( pm, cx + 1, near[cx] - 1, cac );
		}
		if (b == Biome.TUNDRA || b == Biome.SNOWFIELD){
			int rock = c.lit( 0x4a4e58, 0.5f );
			for (int i = 0; i < 3; i++){
				int x = 6 + hash( s, i, 50 ) % (W - 12);
				px( pm, x, hz, rock ); px( pm, x + 1, hz, rock ); px( pm, x, hz - 1, mix( rock, 0xffffff, 0.3f ) );
			}
		}

		//the town: roofs, a tower, and lamps in the windows after dark
		if (c.town && !c.underground) town( c, pm, anim );

		//the very bottom: the ground you stand on
		int gl = c.lit( scale( groundBase, 0.7f ), 0.3f );
		hline( pm, 0, W - 1, H - 1, gl );
	}

	private static void town( SkyContext c, Pixmap pm, int anim ){
		int W = c.W, H = c.H, hz = c.skyHorizon;
		long s = c.seed ^ 0x70A7L;
		boolean dark = c.light < 0.45f;
		int wall = c.lit( 0x7a6a5a, 0.5f ), wallShade = c.lit( 0x5a4c42, 0.25f );
		int roof = c.lit( 0x8a3c30, 0.55f ), roofShade = c.lit( 0x5a2820, 0.3f );
		int slate = c.lit( 0x4e5262, 0.55f );
		int lamp = 0xffd870;
		if (c.snow){ roof = mix( roof, 0xe4ecf4, 0.6f ); slate = mix( slate, 0xe4ecf4, 0.6f ); }
		int x = 2 + hash( s, 0, 1 ) % 3;
		int towerAt = W / 2 + hash( s, 0, 2 ) % 6 - 3;
		boolean towerDone = false;
		int litSide = c.litSide();
		while (x < W - 5){
			if (!towerDone && x >= towerAt - 2){
				//the tower: a shaft, a belfry line, a spire
				int tw = 3, th = 8;
				int base = hz + 1;
				for (int yy = base - th; yy <= base; yy++){
					for (int k = 0; k < tw; k++) px( pm, x + k, yy, (k == 0 && litSide < 0) || (k == tw - 1 && litSide > 0) ? wall : wallShade );
				}
				hline( pm, x, x + tw - 1, base - th, slate );
				px( pm, x + 1, base - th - 1, slate ); px( pm, x + 1, base - th - 2, slate ); px( pm, x + 1, base - th - 3, mix( slate, 0xffffff, 0.3f ) );
				if (dark) px( pm, x + 1, base - th + 2, lamp );
				x += tw + 1;
				towerDone = true;
				continue;
			}
			int hw = 4 + hash( s, x, 3 ) % 4;      //house width
			int hh = 2 + hash( s, x, 4 ) % 3;      //wall height
			int rh = hw / 2 + 1;                   //roof height
			int base = hz + 1;
			boolean tile = (hash( s, x, 5 ) % 3) != 0;
			int rc = tile ? roof : slate, rs = tile ? roofShade : scale( slate, 0.7f );
			for (int yy = base - hh; yy <= base; yy++){
				for (int k = 0; k < hw; k++) px( pm, x + k, yy, (k == 0 && litSide < 0) || (k == hw - 1 && litSide > 0) ? wall : wallShade );
			}
			for (int r = 0; r < rh; r++){
				int y = base - hh - 1 - r;
				int in = r;
				for (int k = in; k < hw - in; k++){
					boolean litFace = litSide < 0 ? k < hw / 2 : k >= (hw + 1) / 2;
					px( pm, x + k, y, litFace ? rc : rs );
				}
			}
			if (dark && (hash( s, x, 6 ) % 4) != 0) px( pm, x + 1 + hash( s, x, 7 ) % Math.max( 1, hw - 2 ), base - 1, lamp );
			//chimney smoke on cold nights
			if ((dark || c.temp < 5f) && (hash( s, x, 8 ) % 2) == 0){
				int cx = x + hw - 2;
				px( pm, cx, base - hh - 2, wallShade );
				int sm = mix( c.zenith, 0xc0c0c8, 0.5f );
				for (int k = 1; k <= 3; k++){
					int sx = cx + Math.round( k * 0.6f * c.windDir ) + ((anim / 2 + k) % 2 == 0 ? 0 : 1) - 1;
					blend( pm, sx, base - hh - 2 - k, sm, 120 - k * 30 );
				}
			}
			x += hw + 1 + hash( s, x, 9 ) % 2;
		}
	}

	private static void fog( SkyContext c, Pixmap pm ){
		int W = c.W, H = c.H, hz = c.skyHorizon;
		int col = mix( c.horizonAvg, 0xd8dce4, 0.4f );
		for (int y = hz - 4; y < H - 1; y++){
			float bell = (float)Math.exp( -Math.pow( (y - hz) / 3.2f, 2 ) );
			for (int x = 0; x < W; x++){
				float n = smooth1( c.seed ^ y, x, 9f );
				int a = Math.round( (40 + 110 * bell) * (0.6f + 0.4f * n) );
				blend( pm, x, y, col, a );
			}
		}
	}

	private static void precip( SkyContext c, Pixmap pm, int anim ){
		if (c.precip == PrecipType.NONE || c.precipRate <= 0.02f) return;
		int W = c.W, H = c.H;
		long s = c.seed ^ 0x9A1EL;
		float rate = clamp01( c.precipRate );
		int slant = Math.round( clamp01( c.wind / 12f ) * 2f ) * (c.windDir > 0 ? 1 : -1);
		boolean frozen = c.precip == PrecipType.SNOW || c.precip == PrecipType.BLIZZARD;
		int n = Math.round( W * H / 36f * rate * (c.precip == PrecipType.BLIZZARD ? 1.6f : 1f) );
		for (int i = 0; i < n; i++){
			int x = hash( s, i, 1 ) % W;
			boolean asRain = !frozen && (c.precip != PrecipType.SLEET || (i & 1) == 0);
			if (asRain){
				int y = (hash( s, i, 2 ) + anim * 5) % (H + 2);
				int col = c.precip == PrecipType.HAIL ? 0xe8ecf2 : 0xa8c4e8;
				int a = c.precip == PrecipType.HAIL ? 230 : 120 + Math.round( 80 * rate );
				if (c.precip == PrecipType.HAIL){
					blend( pm, x, y, col, a ); blend( pm, x, y + 1, 0x8090a8, a / 2 );
				} else {
					blend( pm, x, y, col, a ); blend( pm, x + (slant != 0 ? slant / Math.abs( slant ) : 0), y + 1, col, a * 3 / 4 ); blend( pm, x + slant, y + 2, col, a / 2 );
				}
			} else {
				int y = (hash( s, i, 2 ) + anim * (c.precip == PrecipType.BLIZZARD ? 4 : 2)) % (H + 1);
				int xx = x + ((anim / 3 + i) % 3) - 1 + (c.precip == PrecipType.BLIZZARD ? slant * 2 : 0);
				int a = 170 + Math.round( 70 * rate );
				blend( pm, xx, y, 0xf4f8ff, a );
				if ((hash( s, i, 3 ) % 4) == 0) blend( pm, xx + 1, y, 0xf4f8ff, a / 2 );
			}
		}
	}

	private static void ambient( SkyContext c, Pixmap pm, int anim ){
		int W = c.W, H = c.H, hz = c.skyHorizon;
		long s = c.seed ^ 0xA3B1L;
		switch (c.ambient){
			case FIREFLIES:
				if (c.nightness < 0.5f) return;
				for (int i = 0; i < 6; i++){
					if (((anim / 2 + i * 2) % 4) == 0) continue;
					int x = hash( s, i, 1 ) % W + ((anim + i) % 3) - 1;
					int y = hz - 3 + hash( s, i, 2 ) % 6 + ((anim / 2 + i) % 2);
					blend( pm, x, y, 0xc8ff60, 230 );
					blend( pm, x + 1, y, 0xc8ff60, 70 ); blend( pm, x - 1, y, 0xc8ff60, 70 );
				}
				break;
			case AUTUMN_LEAVES: specks( c, pm, anim, s, 7, new int[]{ 0xd8802a, 0xc84a2a, 0xe8b040 }, 2f, 1f, 200 ); break;
			case SPRING_PETALS: specks( c, pm, anim, s, 6, new int[]{ 0xf0a8c8, 0xfbe0ec, 0xe890b0 }, 1f, 0.5f, 200 ); break;
			case DUST:          specks( c, pm, anim, s, 8, new int[]{ 0xd8c090, 0xc0a878 }, 3f, 0.2f, 120 ); break;
			case ASH:           specks( c, pm, anim, s, 7, new int[]{ 0x9a9a9a, 0x6a6a70 }, 0.5f, 1f, 170 ); break;
			case STEAM:
				for (int i = 0; i < 5; i++){
					int x = hash( s, i, 1 ) % W;
					int y = H - 2 - ((hash( s, i, 2 ) + anim) % 6);
					blend( pm, x, y, 0xe8ecf0, 110 - (H - 2 - y) * 15 );
					blend( pm, x + 1, y, 0xe8ecf0, 60 );
				}
				break;
			default:
		}
	}

	private static void specks( SkyContext c, Pixmap pm, int anim, long s, int n, int[] cols, float vx, float vy, int alpha ){
		int W = c.W, H = c.H;
		for (int i = 0; i < n; i++){
			int x = Math.floorMod( hash( s, i, 1 ) + Math.round( anim * vx * c.windDir * (0.5f + 0.1f * c.wind) ), W );
			int y = Math.floorMod( hash( s, i, 2 ) + Math.round( anim * vy ) + ((anim + i) % 2), H );
			blend( pm, x, y, cols[i % cols.length], alpha );
		}
	}

	private static void lightning( SkyContext c, Pixmap pm, int anim ){
		if ((hash( c.seed, anim / 2, 77 ) % 100) >= 12) return;
		int W = c.W, hz = c.skyHorizon;
		long s = c.seed ^ anim;
		int x = 6 + hash( s, 1, 1 ) % (W - 12), y = 3;
		while (y < hz){
			int len = 2 + hash( s, y, 2 ) % 3;
			int nx = x + hash( s, y, 3 ) % 5 - 2;
			line( pm, x, y, nx, y + len, 0xfff6e0 );
			blend( pm, x + 1, y, 0xc8d8ff, 90 ); blend( pm, x - 1, y, 0xc8d8ff, 90 );
			x = nx; y += len;
		}
		//the flash on the land
		for (int yy = hz - 1; yy < c.H; yy++) for (int xx = 0; xx < W; xx++) blend( pm, xx, yy, 0xe8ecff, 45 );
	}

	// -------------------------------------------------------------- dials

	/**
	 * A strip of coloured segments with a marker. fr are the segment fractions, cur
	 * the lit one, pos 0..1 where the marker stands. h-1 rows of colour over a
	 * shadow row; the marker is a bright column with a dark edge either side.
	 */
	public static void paintStrip( Pixmap pm, int x, int y, int w, int h, float[] fr, int[] cols, int cur, float pos ){
		float acc = 0;
		for (int i = 0; i < fr.length; i++){
			int x0 = x + Math.round( acc * w );
			acc += fr[i];
			int x1 = x + Math.round( acc * w ) - 1;
			int col = i == cur ? cols[i] : mix( cols[i], 0x14141a, 0.62f );
			int top = i == cur ? mix( col, 0xffffff, 0.25f ) : col;
			for (int xx = x0; xx <= x1; xx++){
				for (int yy = 0; yy < h - 1; yy++) px( pm, xx, y + yy, yy == 0 ? top : col );
			}
			if (i < fr.length - 1) vline( pm, x1, y, y + h - 2, 0x0c0c10 );
		}
		hline( pm, x, x + w - 1, y + h - 1, 0x0a0a0e );
		int mx = x + Math.round( pos * (w - 1) );
		vline( pm, mx - 1, y, y + h - 1, 0x0a0a0e ); vline( pm, mx + 1, y, y + h - 1, 0x0a0a0e );
		vline( pm, mx, y, y + h - 1, 0xffffff );
	}

	/** a meter that fills left to right, with a notch where things start to matter */
	public static void paintBar( Pixmap pm, int x, int y, int w, int h, float frac, float notch, int fill ){
		for (int yy = 0; yy < h; yy++) hline( pm, x, x + w - 1, y + yy, yy == h - 1 ? 0x08080c : 0x16161c );
		int fw = Math.round( clamp01( frac ) * w );
		for (int xx = 0; xx < fw; xx++){
			for (int yy = 0; yy < h - 1; yy++) px( pm, x + xx, y + yy, yy == 0 ? mix( fill, 0xffffff, 0.25f ) : fill );
		}
		int nx = x + Math.round( notch * (w - 1) );
		vline( pm, nx, y, y + h - 1, frac * w > nx - x ? 0x3a3a44 : 0x6a6a74 );
	}

	/** 5 x 9: a thin glass tube with a round bulb under it, glass all the way round,
	 *  the mercury standing at the temperature. lo..hi is the whole scale */
	public static void paintThermometer( Pixmap pm, int x, int y, float temp, float lo, float hi, int mercury ){
		int rim = 0xd8dce4, rimShade = 0x9098a8, bore = 0x1e2028;
		int core = mercury, coreLit = mix( mercury, 0xffffff, 0.4f ), coreDark = scale( mercury, 0.7f );
		//the tube: a closed top, glass either side of a dark bore
		hline( pm, x + 1, x + 3, y, rim );
		for (int yy = 1; yy <= 5; yy++){
			px( pm, x + 1, y + yy, rim ); px( pm, x + 3, y + yy, rimShade ); px( pm, x + 2, y + yy, bore );
		}
		//the bulb: a ball of mercury, glass at its sides and under it
		for (int yy = 6; yy <= 7; yy++){
			px( pm, x, y + yy, rim ); px( pm, x + 4, y + yy, rimShade );
			px( pm, x + 1, y + yy, yy == 6 ? coreLit : core );
			px( pm, x + 2, y + yy, core );
			px( pm, x + 3, y + yy, yy == 6 ? core : coreDark );
		}
		px( pm, x + 1, y + 8, rim ); px( pm, x + 2, y + 8, rimShade ); px( pm, x + 3, y + 8, rimShade );
		//the column, rows 5 (at the bulb) up to 1
		float f = clamp01( (temp - lo) / (hi - lo) );
		int rows = Math.round( f * 5f );
		for (int k = 0; k < rows; k++) px( pm, x + 2, y + 5 - k, k == rows - 1 && rows > 1 ? coreLit : core );
	}

	/** 9 x 9: a compass rose whose needle points where the wind blows; no needle
	 *  in still air */
	public static void paintCompass( Pixmap pm, int x, int y, float dirDeg, boolean calm, int needle ){
		int ring = 0x7c8290, tick = 0xb8bec8, north = 0xffffff, hub = 0xa0a4b0;
		float cx = x + 4, cy = y + 4;
		for (int yy = 0; yy < 9; yy++){
			for (int xx = 0; xx < 9; xx++){
				double d = Math.hypot( xx - 4, yy - 4 );
				if (d <= 4.4) px( pm, x + xx, y + yy, d > 3.5 ? ring : 0x14161c );
			}
		}
		px( pm, x + 4, y, north ); px( pm, x + 4, y + 8, tick ); px( pm, x, y + 4, tick ); px( pm, x + 8, y + 4, tick );
		px( pm, x + 1, y + 1, 0x3a3e48 ); px( pm, x + 7, y + 1, 0x3a3e48 ); px( pm, x + 1, y + 7, 0x3a3e48 ); px( pm, x + 7, y + 7, 0x3a3e48 );
		if (!calm){
			double a = Math.toRadians( dirDeg );
			//the needle runs from the hub to the rim on the side the wind is going, no tail
			int tx = Math.round( (float)(cx + Math.sin( a ) * 3.2 ) ), ty = Math.round( (float)(cy - Math.cos( a ) * 3.2 ) );
			line( pm, (int)cx, (int)cy, tx, ty, needle );
			px( pm, tx, ty, 0xffffff );
		}
		px( pm, (int)cx, (int)cy, hub );
	}
}
