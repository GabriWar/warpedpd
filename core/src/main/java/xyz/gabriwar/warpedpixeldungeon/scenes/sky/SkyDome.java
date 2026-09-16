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

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;

import com.badlogic.gdx.graphics.Pixmap;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The sky itself and the things that hang in it: the dome, the stars, the sun,
 * the moon, the clouds, the aurora and the rainbow.
 *
 * The dome is not a stack of keyframes on a clock. Every pixel is lit from where
 * the sun actually is: a Rayleigh-like gradient whose colours follow the sun's
 * elevation, a Mie glow around the disc that widens and reddens as the sun drops,
 * a warm wedge on the horizon under it, and on the far side at twilight the pink
 * band and the blue earth-shadow that a real dusk has. Sunrise glows on the left,
 * sunset on the right, and the pre-dawn light gathers in the east because the sun
 * is really down there.
 */
public final class SkyDome {

	private SkyDome(){}

	//zenith and horizon colours by the sun's elevation (its sine), well below the
	//horizon to straight overhead. the horizon entries are the sky AWAY from the
	//sun; the side under the sun gets its warmth from the glow terms
	private static final float[] E  = { -1.00f,  -0.35f,  -0.20f,  -0.10f,   0.00f,   0.06f,   0.15f,   0.35f,   1.00f };
	private static final int[]   ZEN = { 0x02050c, 0x040816, 0x0a1230, 0x152458, 0x274a8c, 0x2f5fae, 0x3272c6, 0x2e6fc8, 0x2a67c2 };
	private static final int[]   HOR = { 0x080e1c, 0x0e1730, 0x242a54, 0x4c4470, 0x8a7aa0, 0xb0a8c0, 0xbcd0e8, 0xb0d2ee, 0xbcd8f0 };

	private static int key( int[] table, float e ){
		int k = 0;
		while (k < E.length - 2 && e > E[k + 1]) k++;
		float span = E[k + 1] - E[k];
		return mix( table[k], table[k + 1], span <= 0 ? 0 : (e - E[k]) / span );
	}

	/** paints the W x H sky into pm and fills in the context's lighting */
	public static void paint( SkyContext c, Pixmap pm ){
		float e = c.sunElev;

		// ---- the day's numbers, shared with everything painted later ----
		c.light = clamp01( (e + 0.10f) / 0.35f );
		c.light *= 1f - 0.45f * c.cloudCover * c.cloudCover;
		if (c.solarEclipse) c.light *= 0.25f;
		c.nightness = 1f - c.light;
		c.warmth = c.sunUp ? clamp01( 1f - e / 0.35f ) : 1f;
		c.sunColor = mix( 0xFFF4D6, 0xFF8A3C, (float)Math.pow( c.warmth, 1.3 ) );
		if (e < 0.02f) c.sunColor = mix( c.sunColor, 0xE8502A, clamp01( (0.02f - e) / 0.08f ) );
		c.moonColor = c.lunarEclipse ? 0xC05038 : 0xC8D4F0;
		c.sunDisc = clamp01( (0.9f - c.cloudCover) / 0.35f );

		int zen = key( ZEN, e ), hor = key( HOR, e );

		//the moon lifts the whole night a little
		if (c.moonUp && c.nightness > 0.4f){
			float k = 0.22f * c.moonBright * c.nightness;
			zen = mix( zen, 0x18244a, k );
			hor = mix( hor, 0x263258, k );
		}
		//cloud takes the colour out of the sky before the rain arrives
		float cover = c.cloudCover * c.cloudCover;
		if (cover > 0.02f){
			int greyTop = mix( 0x7c8494, 0x262c36, c.storm ? 0.8f : 0.3f + 0.5f * c.nightness );
			int greyLow = mix( 0xa8aeb8, 0x363c46, c.storm ? 0.75f : 0.25f + 0.5f * c.nightness );
			zen = mix( zen, scale( greyTop, 0.35f + 0.65f * c.light ), cover * 0.85f );
			hor = mix( hor, scale( greyLow, 0.35f + 0.65f * c.light ), cover * 0.85f );
		}
		if (c.fog){
			hor = mix( hor, scale( 0xd8dce4, 0.3f + 0.7f * c.light ), 0.6f );
			zen = mix( zen, scale( 0xb8c0cc, 0.3f + 0.7f * c.light ), 0.3f );
		}
		if (c.solarEclipse){
			zen = mix( zen, 0x0a0a18, 0.65f );
			hor = mix( hor, 0x282038, 0.55f );
		}
		if (c.lunarEclipse && c.nightness > 0.6f){
			zen = mix( zen, 0x1c0806, 0.4f );
			hor = mix( hor, 0x481812, 0.35f );
		}
		c.zenith = zen;

		// ---- the glow terms, sized from the sun ----
		float glowGate = 1f - 0.7f * cover;
		//how far the warm wedge on the horizon reaches: from twilight up to a high sun
		float wedge = clamp01( (e + 0.25f) / 0.35f ) * (0.30f + 0.70f * c.warmth) * glowGate;
		int wedgeCol = mix( 0xffd080, 0xff6a30, c.warmth );
		if (e < 0f) wedgeCol = mix( wedgeCol, 0xc03a3a, clamp01( -e / 0.2f ) );
		//the halo round the disc: wide and orange near the horizon, tight and pale on high
		float sigma = 0.09f + 0.24f * c.warmth + 0.12f * c.humidity;
		float halo = (0.60f * c.light + 0.35f * clamp01( (e + 0.12f) / 0.22f )) * glowGate;
		int haloCol = mix( 0xfff2c8, 0xffa050, c.warmth );
		//twilight on the far side: the belt of venus over the earth's shadow
		float twilight = clamp01( 1f - Math.abs( e ) / 0.14f ) * glowGate;
		float antiX = c.W - c.sunX;
		//moonlight
		float moonGlow = c.moonUp ? 0.4f * c.moonBright * c.nightness : 0f;

		int horizonY = c.skyHorizon;
		for (int y = 0; y < c.H; y++){
			//v: 0 on the horizon line, 1 at the top of the screen; below the horizon it
			//just keeps the horizon colour, the land covers it anyway
			float v = clamp01( (horizonY - y) / (float)horizonY );
			float rr, gg, bb;
			{
				int base = mix( hor, zen, (float)Math.pow( v, 0.55 ) );
				rr = r( base ); gg = g( base ); bb = b( base );
			}
			float dy = (y - c.sunY) / c.H;
			float dyMoon = c.moonUp ? (y - c.moonY) / c.H : 9f;
			for (int x = 0; x < c.W; x++){
				float R = rr, G = gg, B = bb;

				float dx = (x - c.sunX) / c.W;
				//the wedge: strongest right under the sun, dying off sideways and upward
				if (wedge > 0.01f){
					float az = (float)Math.exp( -Math.abs( dx ) / 0.42 );
					float k = wedge * az * (float)Math.pow( 1f - v, 2.4 );
					R += (r( wedgeCol ) - R) * k; G += (g( wedgeCol ) - G) * k; B += (b( wedgeCol ) - B) * k;
				}
				//the halo
				if (halo > 0.01f){
					float d = (float)Math.hypot( dx * 1.15f, dy );
					float k = halo * (float)Math.exp( -d / sigma );
					k = Math.min( 0.9f, k );
					R += (r( haloCol ) - R) * k; G += (g( haloCol ) - G) * k; B += (b( haloCol ) - B) * k;
				}
				//twilight opposite the sun
				if (twilight > 0.01f){
					float az = (float)Math.exp( -Math.abs( x - antiX ) / (c.W * 0.55f) );
					float bell = (float)Math.exp( -Math.pow( (v - 0.11f) / 0.07f, 2 ) );
					float k = twilight * az * bell * 0.55f;
					R += (0xe8 - R) * k; G += (0x9a - G) * k; B += (0xb0 - B) * k;
					if (v < 0.07f){
						float k2 = twilight * az * (1f - v / 0.07f) * 0.5f;
						R += (0x5a - R) * k2; G += (0x6a - G) * k2; B += (0x9a - B) * k2;
					}
				}
				//moonlight
				if (moonGlow > 0.01f){
					float d = (float)Math.hypot( (x - c.moonX) / c.W * 1.15f, dyMoon );
					float k = moonGlow * (float)Math.exp( -d / 0.16 );
					R += (0xb4 - R) * k; G += (0xc0 - G) * k; B += (0xe4 - B) * k;
				}
				pm.drawPixel( x, y, rgba( dither( x, y, R, G, B, 26 ), 0xFF ) );
			}
		}

		//what the far ridges will fade into
		c.horizonRow = new int[c.W];
		long sum = 0;
		int row = Math.max( 0, horizonY - 2 );
		for (int x = 0; x < c.W; x++){
			int col = pm.getPixel( x, row ) >>> 8;
			c.horizonRow[x] = col;
			sum += col;
		}
		//an average that means something: average the channels, not the packed ints
		int ar = 0, ag = 0, ab = 0;
		for (int x = 0; x < c.W; x++){ ar += r( c.horizonRow[x] ); ag += g( c.horizonRow[x] ); ab += b( c.horizonRow[x] ); }
		c.horizonAvg = rgb( ar / c.W, ag / c.W, ab / c.W );
	}

	// ------------------------------------------------------------- stars

	//normalized star positions (x,y in a 0..40 box) + magnitudes + join lines
	private static final float[][][] CONSTELLATIONS = {
		//Orion: shoulders, belt of three, feet
		{ {6,4,2}, {20,2,1.6f}, {2,16,1.4f}, {24,18,2.2f}, {11,10,1.8f}, {13,11,1.8f}, {15,12,1.8f} },
		//Big Dipper
		{ {0,6,1.7f}, {6,4,1.5f}, {12,5,1.5f}, {17,8,1.6f}, {24,7,1.5f}, {26,13,1.5f}, {19,15,1.7f} },
		//Cassiopeia W
		{ {0,8,1.6f}, {6,2,1.7f}, {12,7,1.5f}, {18,1,1.7f}, {24,6,1.6f} },
		//Scorpius hook
		{ {2,2,2.1f}, {6,6,1.5f}, {9,11,1.5f}, {10,17,1.5f}, {14,21,1.6f}, {20,22,1.6f}, {24,19,1.7f} },
		//Cygnus cross
		{ {12,0,1.9f}, {12,8,1.5f}, {12,16,1.6f}, {4,8,1.5f}, {20,8,1.5f} },
		//Lyra with Vega
		{ {4,2,2.4f}, {8,6,1.3f}, {12,10,1.4f}, {6,10,1.3f} },
		//Canis Major with Sirius
		{ {6,2,2.8f}, {2,8,1.4f}, {10,10,1.5f}, {6,16,1.5f}, {12,18,1.4f} },
	};
	private static final int[][][] CONST_LINES = {
		{ {0,4},{4,5},{5,6},{6,1},{0,2},{1,3},{2,4},{6,3} },
		{ {0,1},{1,2},{2,3},{3,4},{4,5},{5,6},{6,3} },
		{ {0,1},{1,2},{2,3},{3,4} },
		{ {0,1},{1,2},{2,3},{3,4},{4,5},{5,6} },
		{ {0,1},{1,2},{3,1},{1,4} },
		{ {0,1},{1,2},{2,3},{3,1} },
		{ {0,1},{0,2},{2,3},{2,4} },
	};

	/** the night sky at native resolution: a field seeded by the run, the milky
	 *  way on the darkest nights, and the season's constellations */
	public static void paintStars( SkyContext c, Pixmap pm ){
		int W = c.W, H = c.skyHorizon;
		long s = c.seed ^ 0x57A55L;
		float milky = c.moonBright < 0.6f && c.cloudCover < 0.3f ? 1f - c.moonBright : 0f;

		//the milky way: a broad soft band, tilted, made of hundreds of faint pixels
		if (milky > 0){
			float tilt = 0.55f + 0.3f * noise( s, 3, 3 );
			float off = H * (0.2f + 0.4f * noise( s, 4, 4 ));
			int n = W * H / 14;
			for (int i = 0; i < n; i++){
				int x = hash( s, i, 11 ) % W, y = hash( s, i, 12 ) % H;
				float dist = Math.abs( y - (off + (x - W / 2f) * tilt) ) / (H * 0.13f);
				float wgt = (float)Math.exp( -dist * dist ) * milky;
				if (noise( s, i, 13 ) > wgt) continue;
				int a = 30 + (int)(80 * wgt * noise( s, i, 14 ));
				blend( pm, x, y, noise( s, i, 15 ) > 0.8f ? 0xd8c8f0 : 0xb8c4e8, a );
			}
		}

		//the field: three sizes, a few tinted
		int n = W * H / 110;
		for (int i = 0; i < n; i++){
			int x = hash( s, i, 1 ) % W, y = hash( s, i, 2 ) % H;
			float m = noise( s, i, 3 );
			int col;
			float tint = noise( s, i, 4 );
			if (tint > 0.94f) col = 0xffd8a8;          //an orange giant
			else if (tint > 0.86f) col = 0xb8c8ff;     //a hot blue one
			else col = 0xeef2ff;
			int a = 80 + (int)(175 * m * m);
			//fewer stars low on the horizon, where the air is thick
			a = Math.round( a * (0.45f + 0.55f * (1f - y / (float)H)) );
			blend( pm, x, y, col, a );
			if (m > 0.93f){
				blend( pm, x + 1, y, col, a / 2 ); blend( pm, x - 1, y, col, a / 2 );
				blend( pm, x, y + 1, col, a / 2 ); blend( pm, x, y - 1, col, a / 2 );
			}
		}

		//constellations drift with the season: a third of the catalog is up
		//on any given night, sliding westward through the year
		int yearDay = c.season.ordinal() * 90 + c.dayOfSeason;
		float sc = Math.max( 1f, W / 160f );
		for (int k = 0; k < CONSTELLATIONS.length; k++){
			float slot = ((k * 97 + yearDay) % 360) / 360f;
			if (slot > 0.62f) continue;   //below the horizon tonight
			int ox = (int)(6 * sc + slot * (W - 50 * sc));
			int oy = (int)((6 + ((k * 53) % 40)) * sc * 0.6f);
			float[][] stars = CONSTELLATIONS[k];
			for (int[] ln : CONST_LINES[k]){
				lineBlend( pm, ox + Math.round( stars[ln[0]][0] * sc ), oy + Math.round( stars[ln[0]][1] * sc ),
						ox + Math.round( stars[ln[1]][0] * sc ), oy + Math.round( stars[ln[1]][1] * sc ), 0x40507a, 110 );
			}
			for (float[] st : stars){
				int x = ox + Math.round( st[0] * sc ), y = oy + Math.round( st[1] * sc );
				int size = st[2] >= 2.2f ? 2 : 1;
				for (int yy = 0; yy < size; yy++) for (int xx = 0; xx < size; xx++) px( pm, x + xx, y + yy, 0xf6f8ff );
				if (st[2] >= 1.8f){
					blend( pm, x - 1, y, 0xa0b0e0, 160 ); blend( pm, x + size, y, 0xa0b0e0, 160 );
					blend( pm, x, y - 1, 0xa0b0e0, 160 ); blend( pm, x, y + size, 0xa0b0e0, 160 );
				}
			}
		}
	}

	private static void lineBlend( Pixmap pm, int x0, int y0, int x1, int y1, int rgb, int a ){
		int dx = Math.abs( x1 - x0 ), dy = -Math.abs( y1 - y0 );
		int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		while (true){
			blend( pm, x0, y0, rgb, a );
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy){ err += dy; x0 += sx; }
			if (e2 <= dx){ err += dx; y0 += sy; }
		}
	}

	// -------------------------------------------------------- sun and moon

	/** a soft radial glow, white, to be tinted by whoever draws it */
	public static void paintGlow( Pixmap pm, int size, int peakAlpha ){
		float c = (size - 1) / 2f, rad = size / 2f;
		for (int y = 0; y < size; y++){
			for (int x = 0; x < size; x++){
				double d = Math.hypot( x - c, y - c ) / rad;
				if (d < 1){
					double k = (1 - d) * (1 - d);
					pm.drawPixel( x, y, rgba( 0xFFFFFF, (int)(peakAlpha * k) ) );
				}
			}
		}
	}

	/** the disc, limb-darkened, orange when low; 24 x 24 */
	public static void paintSun( SkyContext c, Pixmap pm ){
		float cx = 11.5f, cy = 11.5f;
		int core = mix( 0xFFFAE4, 0xFFB050, c.warmth );
		int limb = mix( 0xFFD880, 0xFF6A30, c.warmth );
		if (c.sunElev < 0.03f){
			core = mix( core, 0xFF7A3A, 0.5f ); limb = mix( limb, 0xE04828, 0.5f );
		}
		for (int y = 0; y < 24; y++){
			for (int x = 0; x < 24; x++){
				double d = Math.hypot( x - cx, y - cy );
				if (d < 8.6){
					float k = (float)Math.pow( d / 8.6, 2.2 );
					px( pm, x, y, mix( core, limb, k ) );
				} else if (d < 9.6){
					blend( pm, x, y, limb, (int)(255 * (9.6 - d)) );
				}
			}
		}
		//a faint ring of rays, short, so the disc still reads as a disc
		int ray = mix( 0xFFE8A0, 0xFF9050, c.warmth );
		for (int i = 0; i < 12; i++){
			double a = i * Math.PI / 6 + 0.26;
			int x1 = 12 + (int)Math.round( Math.cos( a ) * 10.2 ), y1 = 12 + (int)Math.round( Math.sin( a ) * 10.2 );
			int x2 = 12 + (int)Math.round( Math.cos( a ) * (i % 2 == 0 ? 11.6 : 11.0) );
			int y2 = 12 + (int)Math.round( Math.sin( a ) * (i % 2 == 0 ? 11.6 : 11.0) );
			lineBlend( pm, x1, y1, x2, y2, ray, 150 );
		}
	}

	/** the black disc and the ring of fire; 40 x 40 */
	public static void paintEclipse( Pixmap pm ){
		for (int y = 0; y < 40; y++){
			for (int x = 0; x < 40; x++){
				double d = Math.hypot( x - 19.5, y - 19.5 );
				if (d < 9) px( pm, x, y, 0x060608 );
				else if (d < 10.5) px( pm, x, y, 0xFFF6D8 );
				else if (d < 13) blend( pm, x, y, 0xFFE8B0, (int)(160 * (13 - d) / 2.5) );
				else if (d < 18 && ((int)(Math.atan2( y - 19.5, x - 19.5 ) * 8 / Math.PI) & 1) == 0){
					blend( pm, x, y, 0xD8D8F0, (int)(90 * (18 - d) / 5) );
				}
			}
		}
	}

	/** the moon in its true phase, with its seas, its craters and the faint
	 *  earthshine on the dark side; 24 x 24 */
	public static void paintMoon( SkyContext c, Pixmap pm ){
		GameCalendar.MoonPhase mp = c.moonPhase;
		//shadow-disc offset renders the true lit shape: negative = waxing
		//(lit on the right), positive = waning, +-99 = full/new sentinels
		int off;
		switch (mp){
			case NEW_MOON:        off =  99; break;
			case WAXING_CRESCENT: off = -11; break;
			case FIRST_QUARTER:   off =  -7; break;
			case WAXING_GIBBOUS:  off =  -4; break;
			case FULL_MOON:       off = -99; break;
			case WANING_GIBBOUS:  off =   4; break;
			case LAST_QUARTER:    off =   7; break;
			default:              off =  11; break;
		}
		float cx = 11.5f, cy = 11.5f, R = 9f;
		boolean blood = c.lunarEclipse && !c.sunUp;
		int litA = blood ? 0xB84030 : 0xEEF1F8, litB = blood ? 0x8A2C20 : 0xC8CDDA;
		int sea = blood ? 0x6E241C : 0xAEB5C6;
		int dark = blood ? 0x3A1410 : 0x151c2c;
		long s = c.seed ^ 0x300FL;
		//the seas: a few broad dark patches
		float[][] maria = { {8.5f, 8f, 3.2f}, {13.5f, 9.5f, 2.6f}, {10f, 13.5f, 2.9f}, {14.5f, 14f, 1.8f} };
		//craters: small rings
		float[][] craters = { {7f, 13f, 1.3f}, {15.5f, 7f, 1.1f}, {12f, 16.5f, 1.0f}, {9f, 6f, 0.9f} };
		for (int y = 0; y < 24; y++){
			for (int x = 0; x < 24; x++){
				double d = Math.hypot( x - cx, y - cy );
				if (d >= R + 0.6) continue;
				boolean lit;
				if (off == -99) lit = true;
				else if (off == 99) lit = false;
				else lit = Math.hypot( x - cx - off, y - cy ) >= R;
				int col;
				if (lit){
					col = ((hash( s, x, y ) % 7) == 0) ? litB : litA;
					for (float[] m : maria){
						if (Math.hypot( x - m[0], (y - m[1]) * 1.2 ) < m[2]) col = mix( col, sea, 0.7f );
					}
					for (float[] cr : craters){
						double dc = Math.hypot( x - cr[0], y - cr[1] );
						if (dc < cr[2]) col = mix( col, litB, 0.6f );
						else if (dc < cr[2] + 1) col = mix( col, 0xFFFFFF, 0.25f );
					}
					//limb: the edge falls off a touch
					if (d > R - 1.2) col = mix( col, litB, 0.5f );
				} else {
					col = dark;
				}
				if (d > R) blend( pm, x, y, col, (int)(255 * (R + 0.6 - d) / 0.6) );
				else px( pm, x, y, col );
			}
		}
	}

	// -------------------------------------------------------------- clouds

	/**
	 * A cloud w x (w/2), a row of puffs on a flat keel, lit from one side by the
	 * sun's colour and shaded underneath by the sky's. tw x th is the texture it
	 * lives in; everything stays radius-inside w x h so nothing is cut off.
	 */
	public static void paintCloud( SkyContext c, Pixmap pm, int w, boolean far, boolean litFromLeft, long key ){
		int h = w / 2;
		int lite, base, shade;
		if (c.storm){
			lite = mix( 0x6a7488, c.sunColor, 0.12f * c.light );
			base = 0x4a5468; shade = 0x353d4e;
		} else if (c.light < 0.25f){
			//night clouds: a shade lighter than the sky behind them, softly shaded, and
			//rimmed by the moon; anything harder reads as rocks hanging in the air
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
		if (far){
			//distant clouds sit in the haze
			float hz = c.haze( 0.5f );
			lite = mix( lite, c.horizonAvg, hz * 0.5f ); base = mix( base, c.horizonAvg, hz * 0.5f ); shade = mix( shade, c.horizonAvg, hz * 0.5f );
		}
		long s = key;
		int keel = h * 2 / 3;
		int nPuff = 3 + hash( s, 1, 1 ) % 4;
		for (int p = 0; p < nPuff; p++){
			int br = h / 5 + hash( s, p, 2 ) % Math.max( 1, h / 4 );
			int minX = br + 1, maxX = w - br - 2;
			if (maxX <= minX){ br = h / 5; minX = br + 1; maxX = Math.max( minX + 1, w - br - 2 ); }
			int bx = minX + hash( s, p, 3 ) % Math.max( 1, maxX - minX );
			int by = keel - br / 2 - hash( s, p, 4 ) % Math.max( 1, br / 2 );
			for (int y = Math.max( 0, by - br ); y <= Math.min( keel, by + br ); y++){
				for (int x = Math.max( 0, bx - br ); x <= Math.min( w - 1, bx + br ); x++){
					double nx = (x - bx) / (double)br, ny = (y - by) * 1.35 / br;
					double d = Math.hypot( nx, ny );
					if (d >= 1) continue;
					//a ragged rim, so the puffs stop being perfect circles
					if (d > 0.82 && (hash( s, x + 3, y + 5 ) % 5) < 2) continue;
					//lit from the sun's side and from above, shaded below, in three steps
					//with a dithered band between each pair so the rounding reads
					float ex = (float)((litFromLeft ? -nx : nx) * 0.55 - ny * 0.95);
					boolean odd = ((x + y) & 1) == 0;
					int col;
					if (ex > 0.45f) col = lite;
					else if (ex > 0.2f) col = odd ? lite : base;
					else if (ex > -0.3f) col = base;
					else if (ex > -0.5f) col = odd ? base : shade;
					else col = shade;
					px( pm, x, y, col );
				}
			}
		}
		//the keel itself: a soft flat base under the puffs
		for (int x = 0; x < w; x++){
			boolean covered = false;
			for (int y = 0; y <= keel; y++){
				if ((pm.getPixel( x, y ) & 0xFF) != 0){ covered = true; break; }
			}
			if (covered){
				px( pm, x, keel, shade );
				blend( pm, x, keel + 1, shade, 0x99 );
			}
		}
	}

	/** a long high wisp for clear days, frayed along its length; w x 8 */
	public static void paintCirrus( SkyContext c, Pixmap pm, int w, long key ){
		int col = mix( 0xF4F8FF, c.sunColor, 0.4f * c.warmth );
		for (int x = 0; x < w; x++){
			float t = x / (float)w;
			float body = (float)Math.pow( Math.sin( t * Math.PI ), 0.7 );
			int yc = 3 + Math.round( (smooth1( key, x, 41f ) - 0.5f) * 5f );
			float fray = 0.3f + 0.7f * smooth1( key ^ 7, x, 6f );
			int a = (int)(95 * body * fray);
			blend( pm, x, yc, col, a );
			blend( pm, x, yc + 1, col, a * 3 / 5 );
			blend( pm, x, yc - 1, col, a * 3 / 5 );
			blend( pm, x, yc + 2, col, a / 4 );
			blend( pm, x, yc - 2, col, a / 4 );
		}
	}

	// ----------------------------------------------------- aurora, rainbow

	/** hanging curtains, green through teal to violet, painted at native size so
	 *  every pixel of them is a real pixel */
	public static void paintAurora( Pixmap pm, int w, int h, long seed ){
		int[] cols = { 0x40E080, 0x30C8A0, 0x50A0E0, 0x9060D8 };
		float sx = w / 256f, sy = h / 128f;
		for (int band = 0; band < 4; band++){
			for (int x = 0; x < w; x++){
				float u = x / sx;
				double wave = Math.sin( u * 0.045 + band * 2.1 ) * 10 + Math.sin( u * 0.013 + band ) * 8;
				int yTop = Math.round( (float)((8 + band * 14 + wave) * sy) );
				double fold = Math.sin( u * 0.08 + band * 1.7 + noise( seed, band, 0 ) * 3 );
				int len = Math.round( (float)((26 + fold * 12) * sy) );
				boolean ray = fold > 0.82;
				for (int y = 0; y < len; y++){
					int a = 190 - y * 190 / Math.max( 1, len );
					if (ray) a = Math.min( 255, a + 55 );
					int c = y > len * 2 / 3 ? mix( cols[band], 0xE060A0, 0.4f ) : cols[band];
					blend( pm, x, yTop + y, c, Math.max( 0, a ) );
				}
			}
		}
	}

	/** the bow and its fainter reversed twin, centred where the anti-solar point
	 *  is, so it always stands opposite the sun */
	public static void paintRainbow( Pixmap pm, int w, int h, int cx, int baseY, int radius ){
		int[] cols = { 0xE04040, 0xE09040, 0xE0D040, 0x50C050, 0x5080D0, 0x8050C0 };
		float step = 1f / Math.max( 8, radius * 3 );
		for (int bow = 0; bow < 2; bow++){
			for (int i = 0; i < cols.length; i++){
				int col = bow == 0 ? cols[i] : cols[cols.length - 1 - i];
				float rad = (bow == 0 ? radius : radius * 1.13f) - i * Math.max( 1f, radius / 36f );
				for (double a = Math.PI; a <= Math.PI * 2; a += step){
					double end = Math.min( a - Math.PI, Math.PI * 2 - a );
					int alpha = (int)((bow == 0 ? 0xA8 : 0x40) * Math.min( 1, end / 0.5 ));
					int x = cx + (int)Math.round( Math.cos( a ) * rad ), y = baseY + (int)Math.round( Math.sin( a ) * rad );
					if (x >= 0 && x < w && y >= 0 && y < h){
						blend( pm, x, y, col, alpha );
						blend( pm, x, y + 1, col, alpha );
					}
				}
			}
		}
	}
}
