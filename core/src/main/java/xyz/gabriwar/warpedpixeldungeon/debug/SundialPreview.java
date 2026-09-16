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

package xyz.gabriwar.warpedpixeldungeon.debug;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.WeatherOverlayAmbient;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.MoonPhase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyContext;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyMiniature;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;

import java.io.File;

/**
 * Headless mock of the sundial panel: the real painters for the window and the
 * dials, a stand-in 3x5 font for the labels, laid out exactly as the HUD does it.
 * Renders a contact sheet so the art can be judged without launching the game.
 *
 * java -cp ... xyz.gabriwar.warpedpixeldungeon.debug.SundialPreview <out.png> [scale]
 */
public class SundialPreview {

	interface Setup { void apply( SkyContext c ); }

	static final class Scene {
		final String name; final Setup setup;
		final String countdown, temp, wind, weekday, season, weather;
		final float rest, tempC, windSpeed, windDir, dayPos, yearPos;
		final int phase, seasonIdx;
		Scene( String name, Setup setup, String countdown, int phase, String weather, float tempC, float windSpeed, float windDir,
			   String weekday, int seasonIdx, String season, float rest, float dayPos, float yearPos ){
			this.name = name; this.setup = setup; this.countdown = countdown; this.phase = phase; this.weather = weather;
			this.tempC = tempC; this.windSpeed = windSpeed; this.windDir = windDir; this.weekday = weekday;
			this.seasonIdx = seasonIdx; this.season = season; this.rest = rest; this.dayPos = dayPos; this.yearPos = yearPos;
			this.temp = Math.round( tempC ) + "°";
			this.wind = windSpeed < 0.5f ? "CALM" : Math.round( windSpeed ) + " " + cardinal( windDir );
		}
	}

	private static final String[] CARD = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
	private static String cardinal( float deg ){ return CARD[Math.round( (((deg % 360f) + 360f) % 360f) / 45f ) % 8]; }

	private static final Scene[] SCENES = {
		new Scene( "noon forest summer", c -> { c.f = 0.35f; c.biome = Biome.FOREST; c.season = Season.SUMMER; c.cloudCover = 0.15f; c.moonAge = 0.25f; },
				"DUSK IN 620", 1, "CLEAR", 27, 3, 60, "STONESDAY", 1, "SUMMER 40", 0.2f, 0.35f, 0.36f ),
		new Scene( "dawn meadow spring", c -> { c.f = 0.05f; c.biome = Biome.MEADOW; c.season = Season.SPRING; c.cloudCover = 0.3f; c.ambient = WeatherOverlayAmbient.SPRING_PETALS; c.moonAge = 0.9f; },
				"DAY IN 120", 0, "FAIR", 12, 5, 200, "TIDEDAY", 0, "SPRING 22", 0.05f, 0.05f, 0.06f ),
		new Scene( "dusk mountain autumn", c -> { c.f = 0.67f; c.biome = Biome.MOUNTAIN; c.season = Season.AUTUMN; c.cloudCover = 0.45f; c.wind = 6; c.moonAge = 0.55f; },
				"NIGHT IN 80", 2, "CLOUDS", 9, 6, 300, "STORMDAY", 2, "AUTUMN 61", 0.55f, 0.68f, 0.67f ),
		new Scene( "night forest full moon", c -> { c.f = 0.85f; c.biome = Biome.FOREST; c.season = Season.SUMMER; c.cloudCover = 0.1f; c.moonAge = 0.5f; c.ambient = WeatherOverlayAmbient.FIREFLIES; },
				"DAWN IN 410", 3, "CLEAR", 16, 1, 90, "SHADOWDAY", 1, "SUMMER 71", 0.7f, 0.85f, 0.45f ),
		new Scene( "night town winter snow", c -> { c.f = 0.78f; c.biome = Biome.MEADOW; c.town = true; c.season = Season.WINTER; c.snow = true; c.temp = -6; c.cloudCover = 0.85f; c.precip = PrecipType.SNOW; c.precipRate = 0.6f; c.moonAge = 0.3f; c.wind = 3; },
				"DAWN IN 900", 3, "SNOW", -6, 3, 340, "HARVESTDAY", 3, "WINTER 12", 0.4f, 0.78f, 0.78f ),
		new Scene( "day plains rain windy", c -> { c.f = 0.45f; c.biome = Biome.PLAINS; c.season = Season.SPRING; c.cloudCover = 0.9f; c.precip = PrecipType.RAIN; c.precipRate = 0.8f; c.wind = 14; c.windDir = -1; c.humidity = 0.9f; c.moonAge = 0.1f; },
				"DUSK IN 300", 1, "DOWNPOUR", 11, 14, 250, "FORGEDAY", 0, "SPRING 80", 0.3f, 0.45f, 0.22f ),
		new Scene( "storm foothills", c -> { c.f = 0.5f; c.biome = Biome.FOOTHILLS; c.season = Season.SUMMER; c.cloudCover = 1f; c.storm = true; c.precip = PrecipType.RAIN; c.precipRate = 1f; c.wind = 20; c.moonAge = 0.7f; },
				"DUSK IN 210", 1, "STORM", 18, 20, 120, "LIGHTDAY", 1, "SUMMER 55", 0.85f, 0.5f, 0.4f ),
		new Scene( "fog swamp morning", c -> { c.f = 0.14f; c.biome = Biome.SWAMP; c.season = Season.AUTUMN; c.cloudCover = 0.5f; c.fog = true; c.humidity = 1f; c.moonAge = 0.6f; },
				"DUSK IN 810", 1, "FOG", 8, 0, 0, "STONESDAY", 2, "AUTUMN 10", 0.1f, 0.14f, 0.53f ),
		new Scene( "overcast tundra cold", c -> { c.f = 0.3f; c.biome = Biome.TUNDRA; c.season = Season.WINTER; c.cloudCover = 0.95f; c.temp = -14; c.snow = true; c.wind = 9; c.moonAge = 0.45f; },
				"DUSK IN 150", 1, "OVERCAST", -14, 9, 20, "TIDEDAY", 3, "WINTER 44", 0.5f, 0.3f, 0.87f ),
		new Scene( "desert noon dust", c -> { c.f = 0.36f; c.biome = Biome.DESERT; c.season = Season.SUMMER; c.cloudCover = 0.02f; c.temp = 41; c.humidity = 0.05f; c.wind = 11; c.ambient = WeatherOverlayAmbient.DUST; c.moonAge = 0.02f; },
				"DUSK IN 590", 1, "CLEAR", 41, 11, 100, "FORGEDAY", 1, "SUMMER 30", 0.35f, 0.36f, 0.33f ),
		new Scene( "ocean sunset", c -> { c.f = 0.665f; c.biome = Biome.OCEAN; c.season = Season.SUMMER; c.cloudCover = 0.35f; c.wind = 7; c.moonAge = 0.5f; },
				"NIGHT IN 110", 2, "FAIR", 22, 7, 270, "SHADOWDAY", 1, "SUMMER 88", 0.6f, 0.67f, 0.49f ),
		new Scene( "beach rainbow", c -> { c.f = 0.58f; c.biome = Biome.BEACH; c.season = Season.SPRING; c.cloudCover = 0.4f; c.rainbow = true; c.precip = PrecipType.RAIN; c.precipRate = 0.15f; c.moonAge = 0.35f; },
				"DUSK IN 90", 1, "CLEARING", 19, 4, 45, "LIGHTDAY", 0, "SPRING 60", 0.45f, 0.58f, 0.16f ),
		new Scene( "aurora snowfield", c -> { c.f = 0.88f; c.biome = Biome.SNOWFIELD; c.season = Season.WINTER; c.snow = true; c.temp = -20; c.cloudCover = 0.05f; c.aurora = true; c.moonAge = 0.15f; },
				"DAWN IN 260", 3, "CLEAR", -20, 2, 0, "HARVESTDAY", 3, "WINTER 70", 0.95f, 0.88f, 0.94f ),
		new Scene( "solar eclipse meadow", c -> { c.f = 0.36f; c.biome = Biome.MEADOW; c.season = Season.SUMMER; c.solarEclipse = true; c.cloudCover = 0.1f; c.moonAge = 0f; },
				"DUSK IN 600", 1, "CLEAR", 20, 2, 180, "STORMDAY", 1, "SUMMER 15", 0.15f, 0.36f, 0.29f ),
		new Scene( "underground night crescent", c -> { c.f = 0.8f; c.underground = true; c.biome = Biome.MOUNTAIN; c.season = Season.AUTUMN; c.cloudCover = 0.2f; c.moonAge = 0.12f; },
				"DAWN IN 700", 3, "CLEAR", 6, 4, 315, "TIDEDAY", 2, "AUTUMN 30", 0.65f, 0.8f, 0.58f ),
		new Scene( "hail river day", c -> { c.f = 0.4f; c.biome = Biome.RIVER; c.season = Season.SPRING; c.cloudCover = 0.8f; c.precip = PrecipType.HAIL; c.precipRate = 0.7f; c.wind = 8; c.moonAge = 0.8f; },
				"DUSK IN 480", 1, "HAIL", 4, 8, 135, "STONESDAY", 0, "SPRING 33", 0.25f, 0.4f, 0.09f ),
		new Scene( "autumn leaves forest windy", c -> { c.f = 0.55f; c.biome = Biome.FOREST; c.season = Season.AUTUMN; c.cloudCover = 0.5f; c.wind = 12; c.windDir = -1; c.ambient = WeatherOverlayAmbient.AUTUMN_LEAVES; c.moonAge = 0.4f; },
				"DUSK IN 200", 1, "CLOUDS", 13, 12, 230, "FORGEDAY", 2, "AUTUMN 75", 0.5f, 0.55f, 0.7f ),
		new Scene( "blood moon town", c -> { c.f = 0.83f; c.biome = Biome.PLAINS; c.town = true; c.season = Season.AUTUMN; c.cloudCover = 0.15f; c.lunarEclipse = true; c.moonAge = 0.5f; },
				"DAWN IN 500", 3, "CLEAR", 7, 2, 90, "SHADOWDAY", 2, "AUTUMN 45", 0.75f, 0.83f, 0.62f ),
		new Scene( "predawn foothills new moon", c -> { c.f = 0.975f; c.biome = Biome.FOOTHILLS; c.season = Season.SPRING; c.cloudCover = 0.25f; c.moonAge = 0.0f; },
				"DAWN IN 60", 3, "FAIR", 5, 1, 0, "LIGHTDAY", 0, "SPRING 5", 0.9f, 0.975f, 0.01f ),
		new Scene( "blizzard mountain", c -> { c.f = 0.42f; c.biome = Biome.MOUNTAIN; c.season = Season.WINTER; c.snow = true; c.temp = -12; c.cloudCover = 1f; c.precip = PrecipType.BLIZZARD; c.precipRate = 1f; c.wind = 22; c.storm = false; c.moonAge = 0.5f; },
				"DUSK IN 100", 1, "BLIZZARD", -12, 22, 290, "STORMDAY", 3, "WINTER 88", 0.3f, 0.42f, 0.99f ),
	};

	//the HUD's layout, in panel pixels
	static final int PAD = 3, W_IN = 68, H_IN = 22, WIN_W = W_IN + 2 * SkyMiniature.FRAME, WIN_H = H_IN + 2 * SkyMiniature.FRAME;
	static final int PANEL_W = PAD * 2 + WIN_W;
	static final int Y_WIN = PAD, Y_DAY = Y_WIN + WIN_H, Y_ROW1 = Y_DAY + 3 + 2, Y_ROW2 = Y_ROW1 + 9 + 1, Y_ROW3 = Y_ROW2 + 7 + 1, Y_SEASON = Y_ROW3 + 7 + 1;
	static final int PANEL_H = Y_SEASON + 3 + PAD;

	static final int[] PHASE_COLS  = { 0xff9a4a, 0xffd84a, 0xb070b8, 0x3a4a80 };
	static final int[] PHASE_TEXT  = { 0xffb070, 0xffe888, 0xd8a0d8, 0x9cb0f0 };
	static final int[] SEASON_COLS = { 0x4cb84c, 0xf0c832, 0xd06a24, 0x86b8de };
	static final int[] SEASON_TEXT = { 0x8ee08a, 0xffe070, 0xf0a060, 0xb8dcf8 };
	static final float[] TEMP_STOPS  = { -20f, 0f, 15f, 25f, 35f, 45f };
	static final int[]   TEMP_COLS   = { 0x4477ff, 0x55ccee, 0x44cc55, 0xeebb33, 0xff7722, 0xff3322 };
	static final float[] WIND_STOPS  = { 0f, 8f, 16f, 25f };
	static final int[]   WIND_COLS   = { 0x66bb66, 0xd8c43a, 0xee7733, 0xee3333 };

	public static void main( String[] args ) throws Exception {
		GdxNativesLoader.load();
		File out = new File( args.length > 0 ? args[0] : "/tmp/sundial.png" );
		int scale = args.length > 1 ? Integer.parseInt( args[1] ) : 4;
		Pixmap icons = new Pixmap( new FileHandle( new File( "core/src/main/assets/interfaces/sundial_toggle.png" ) ) );

		int cols = 4, rows = (SCENES.length + cols - 1) / cols;
		int cellW = PANEL_W + 8, cellH = PANEL_H + 8;
		Pixmap sheet = fresh( cellW * cols * scale, cellH * rows * scale );
		//a dungeon-ish backdrop so the translucent panel reads as it will in play
		for (int y = 0; y < sheet.getHeight(); y++) for (int x = 0; x < sheet.getWidth(); x++){
			int v = 0x3a3024 + (SkyPaint.hash( 7, x / scale, y / scale ) % 9) * 0x010101;
			if (((x / scale) % 16) == 0 || ((y / scale) % 16) == 0) v = 0x2a2218;
			sheet.drawPixel( x, y, SkyPaint.rgba( v, 0xFF ) );
		}

		for (int i = 0; i < SCENES.length; i++){
			Scene sc = SCENES[i];
			Pixmap panel = renderPanel( sc, icons, 3 );
			int ox = (i % cols) * cellW + 4, oy = (i / cols) * cellH + 4;
			for (int y = 0; y < panel.getHeight(); y++) for (int x = 0; x < panel.getWidth(); x++){
				int p = panel.getPixel( x, y );
				int a = p & 0xFF;
				if (a == 0) continue;
				for (int sy = 0; sy < scale; sy++) for (int sx = 0; sx < scale; sx++){
					SkyPaint.blend( sheet, (ox + x) * scale + sx, (oy + y) * scale + sy, p >>> 8, a );
				}
			}
			panel.dispose();
			System.out.println( sc.name );
		}
		PixmapIO.writePNG( new FileHandle( out ), sheet );
		System.out.println( "wrote " + out + "  panel " + PANEL_W + "x" + PANEL_H );
	}

	static Pixmap fresh( int w, int h ){
		Pixmap p = new Pixmap( w, h, Pixmap.Format.RGBA8888 );
		p.setBlending( Pixmap.Blending.None );
		p.setColor( 0 );
		p.fill();
		return p;
	}

	/** the whole chip, as the HUD stacks it */
	static Pixmap renderPanel( Scene sc, Pixmap icons, int anim ){
		Pixmap pm = fresh( PANEL_W, PANEL_H );
		//the toast panel: dark, a little see-through, 1px rounded rim
		for (int y = 0; y < PANEL_H; y++) for (int x = 0; x < PANEL_W; x++){
			boolean edge = x == 0 || y == 0 || x == PANEL_W - 1 || y == PANEL_H - 1;
			boolean corner = (x == 0 || x == PANEL_W - 1) && (y == 0 || y == PANEL_H - 1);
			if (corner) continue;
			pm.drawPixel( x, y, SkyPaint.rgba( edge ? 0x0a0a0c : 0x22232a, edge ? 0xFF : 0xE0 ) );
		}

		SkyContext c = new SkyContext( W_IN, H_IN );
		c.seed = 0xBEEF1234L;
		sc.setup.apply( c );
		SkyMiniature.fit( c );

		int wx = PAD + SkyMiniature.FRAME, wy = Y_WIN + SkyMiniature.FRAME;
		SkyMiniature.paintSky( c, pm, wx, wy, anim );
		int[] sun = SkyMiniature.sunIcon( c );
		if (sun != null) icon( pm, icons, c.solarEclipse ? 25 : 0, wx + sun[0], wy + sun[1], SkyMiniature.sunAlpha( c ), wx, wy );
		SkyMiniature.paintOver( c, pm, wx, wy, anim );
		SkyMiniature.paintFrame( pm, PAD, Y_WIN, WIN_W, WIN_H );

		//weather caption inside the window, the moon's phase badge opposite it
		SkyMiniature.paintPlates( pm, wx, wy, W_IN, textWidth( sc.weather ), 5 );
		text( pm, sc.weather, wx + 2, wy + 2, 0xf0f0f0 );
		icon( pm, icons, SkyMiniature.moonFrame( c ), wx + W_IN - 11, wy + 2, 1f, wx, wy );

		//the dial under the window: the day, proportioned to the season
		float dayT = sc.seasonIdx == 1 ? 1400 : sc.seasonIdx == 0 ? 1100 : sc.seasonIdx == 2 ? 900 : 600;
		float[] fr = { 250 / 2500f, dayT / 2500f, 250 / 2500f, (2000 - dayT) / 2500f };
		SkyMiniature.paintStrip( pm, PAD, Y_DAY, WIN_W, 3, fr, PHASE_COLS, sc.phase, sc.dayPos );

		//row 1: thermometer + temp, compass + wind
		int tCol = grad( TEMP_STOPS, TEMP_COLS, sc.tempC );
		SkyMiniature.paintThermometer( pm, PAD + 1, Y_ROW1, sc.tempC, -20, 45, tCol );
		text( pm, sc.temp, PAD + 8, Y_ROW1 + 2, tCol );
		int wCol = grad( WIND_STOPS, WIND_COLS, sc.windSpeed );
		int windTextW = textWidth( sc.wind );
		SkyMiniature.paintCompass( pm, PAD + WIN_W - windTextW - 12, Y_ROW1, sc.windDir, sc.windSpeed < 0.5f, wCol );
		text( pm, sc.wind, PAD + WIN_W - windTextW, Y_ROW1 + 2, sc.windSpeed < 0.5f ? 0x9a9aa4 : wCol );

		//row 2: countdown, rest
		text( pm, sc.countdown, PAD + 1, Y_ROW2 + 1, PHASE_TEXT[sc.phase] );
		int restCol = SkyPaint.mix( 0x4488ff, 0xdd3355, SkyPaint.clamp01( (sc.rest - 0.5f) / 0.5f ) );
		text( pm, "ZZ", PAD + WIN_W - 23, Y_ROW2 + 1, restCol );
		SkyMiniature.paintBar( pm, PAD + WIN_W - 14, Y_ROW2 + 2, 14, 3, sc.rest, 0.5f, restCol );

		//row 3: weekday, season + day; the weekday drops its "day" when the row is full
		String wd = sc.weekday;
		if (textWidth( wd ) + textWidth( sc.season ) + 4 > WIN_W) wd = wd.substring( 0, wd.length() - 3 );
		text( pm, wd, PAD + 1, Y_ROW3 + 1, 0xcacfc2 );
		text( pm, sc.season, PAD + WIN_W - textWidth( sc.season ), Y_ROW3 + 1, SEASON_TEXT[sc.seasonIdx] );

		//the year strip
		SkyMiniature.paintStrip( pm, PAD, Y_SEASON, WIN_W, 3, new float[]{ 0.25f, 0.25f, 0.25f, 0.25f }, SEASON_COLS, sc.seasonIdx, sc.yearPos );
		return pm;
	}

	/** a 9x9 frame of the sun/moon strip, clipped to the window's inside */
	static void icon( Pixmap dst, Pixmap strip, int frame, int x, int y, float alpha, int wx, int wy ){
		for (int yy = 0; yy < 9; yy++) for (int xx = 0; xx < 9; xx++){
			int p = strip.getPixel( frame * 9 + xx, yy );
			int a = Math.round( (p & 0xFF) * alpha );
			int dx = x + xx, dy = y + yy;
			if (dx < wx || dy < wy || dx >= wx + W_IN || dy >= wy + H_IN) continue;
			if (a > 0) SkyPaint.blend( dst, dx, dy, p >>> 8, a );
		}
	}

	static int grad( float[] stops, int[] cols, float v ){
		if (v <= stops[0]) return cols[0];
		for (int i = 1; i < stops.length; i++) if (v <= stops[i]) return SkyPaint.mix( cols[i - 1], cols[i], (v - stops[i - 1]) / (stops[i] - stops[i - 1]) );
		return cols[cols.length - 1];
	}

	// ------------------------------------------------ a stand-in 3x5 font

	static final String[] FONT_KEYS = { "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
			"0","1","2","3","4","5","6","7","8","9","°"," ","-","." };
	static final String[][] FONT = {
		{"010","101","111","101","101"},{"110","101","110","101","110"},{"011","100","100","100","011"},{"110","101","101","101","110"},
		{"111","100","110","100","111"},{"111","100","110","100","100"},{"011","100","101","101","011"},{"101","101","111","101","101"},
		{"111","010","010","010","111"},{"001","001","001","101","010"},{"101","110","100","110","101"},{"100","100","100","100","111"},
		{"101","111","111","101","101"},{"110","101","101","101","101"},{"010","101","101","101","010"},{"110","101","110","100","100"},
		{"010","101","101","111","011"},{"110","101","110","101","101"},{"011","100","010","001","110"},{"111","010","010","010","010"},
		{"101","101","101","101","011"},{"101","101","101","101","010"},{"101","101","111","111","101"},{"101","101","010","101","101"},
		{"101","101","010","010","010"},{"111","001","010","100","111"},
		{"010","101","101","101","010"},{"010","110","010","010","111"},{"110","001","010","100","111"},{"110","001","010","001","110"},
		{"101","101","111","001","001"},{"111","100","110","001","110"},{"011","100","110","101","010"},{"111","001","010","010","010"},
		{"010","101","010","101","010"},{"010","101","011","001","010"},{"010","101","010","000","000"},{"000","000","000","000","000"},
		{"000","000","111","000","000"},{"000","000","000","000","010"},
	};

	static int textWidth( String s ){ return s.length() * 4 - 1; }

	static void text( Pixmap pm, String s, int x, int y, int col ){
		for (int i = 0; i < s.length(); i++){
			String ch = s.substring( i, i + 1 );
			int k = -1;
			for (int j = 0; j < FONT_KEYS.length; j++) if (FONT_KEYS[j].equals( ch )){ k = j; break; }
			if (k >= 0){
				for (int r = 0; r < 5; r++) for (int cc = 0; cc < 3; cc++){
					if (FONT[k][r].charAt( cc ) == '1'){
						SkyPaint.px( pm, x + i * 4 + cc, y + r, col );
					}
				}
			}
		}
	}
}
