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

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyContext;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyDome;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyHorizon;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;

import java.io.File;
import java.util.Random;

/**
 * Renders the sky scene's static art to PNG files without a window, a GL context
 * or a running game, so the painters can be judged and tuned from the desk.
 * The moving layer is not here; everything that stands still is.
 *
 *   java -cp core/build/classes/java/main:SPD-classes/build/classes/java/main:gdx.jar:gdx-natives.jar \
 *        xyz.gabriwar.warpedpixeldungeon.debug.SkyPreview /tmp/sky [WxH]
 */
public class SkyPreview {

	private interface Setup { void apply( SkyContext c ); }

	private static final Object[][] SCENES = {
		{ "01-dawn-forest",      (Setup) c -> { c.f = 0.04f; c.biome = Biome.FOREST; c.humidity = 0.7f; } },
		{ "02-sunrise-meadow",   (Setup) c -> { c.f = 0.085f; c.biome = Biome.MEADOW; c.season = GameCalendar.Season.SPRING; } },
		{ "03-noon-forest",      (Setup) c -> { c.f = 0.35f; c.biome = Biome.FOREST; c.cloudCover = 0.25f; } },
		{ "04-afternoon-river",  (Setup) c -> { c.f = 0.5f; c.biome = Biome.RIVER; c.season = GameCalendar.Season.AUTUMN; } },
		{ "05-golden-beach",     (Setup) c -> { c.f = 0.62f; c.biome = Biome.BEACH; c.humidity = 0.6f; } },
		{ "06-sunset-mountain",  (Setup) c -> { c.f = 0.675f; c.biome = Biome.MOUNTAIN; } },
		{ "07-dusk-town",        (Setup) c -> { c.f = 0.70f; c.biome = Biome.SNOWFIELD; c.town = true; c.snow = true; c.temp = -4; } },
		{ "08-night-snowfield",  (Setup) c -> { c.f = 0.85f; c.biome = Biome.SNOWFIELD; c.snow = true; c.temp = -12; c.season = GameCalendar.Season.WINTER; c.moonPhase = GameCalendar.MoonPhase.FULL_MOON; } },
		{ "09-night-desert",     (Setup) c -> { c.f = 0.80f; c.biome = Biome.DESERT; c.moonPhase = GameCalendar.MoonPhase.WAXING_CRESCENT; c.humidity = 0.1f; } },
		{ "10-aurora-tundra",    (Setup) c -> { c.f = 0.88f; c.biome = Biome.TUNDRA; c.aurora = true; c.snow = true; c.temp = -15; c.season = GameCalendar.Season.WINTER; c.moonPhase = GameCalendar.MoonPhase.NEW_MOON; } },
		{ "11-storm-plains",     (Setup) c -> { c.f = 0.4f; c.biome = Biome.PLAINS; c.cloudCover = 0.95f; c.storm = true; c.precip = PrecipType.RAIN; c.precipRate = 0.8f; } },
		{ "12-fog-swamp",        (Setup) c -> { c.f = 0.12f; c.biome = Biome.SWAMP; c.fog = true; c.humidity = 0.95f; c.cloudCover = 0.6f; } },
		{ "13-rainbow-meadow",   (Setup) c -> { c.f = 0.58f; c.biome = Biome.MEADOW; c.rainbow = true; c.cloudCover = 0.5f; } },
		{ "14-winter-oaks",      (Setup) c -> { c.f = 0.3f; c.biome = Biome.FOREST; c.season = GameCalendar.Season.WINTER; c.snow = true; c.temp = -3; c.precip = PrecipType.SNOW; c.precipRate = 0.5f; c.cloudCover = 0.7f; } },
		{ "15-predawn-foothills",(Setup) c -> { c.f = 0.975f; c.biome = Biome.FOOTHILLS; } },
		{ "16-bloodmoon-ocean",  (Setup) c -> { c.f = 0.82f; c.biome = Biome.OCEAN; c.lunarEclipse = true; c.moonPhase = GameCalendar.MoonPhase.FULL_MOON; } },
		{ "17-eclipse-town",     (Setup) c -> { c.f = 0.36f; c.biome = Biome.MEADOW; c.town = true; c.solarEclipse = true; } },
		{ "18-underground",      (Setup) c -> { c.f = 0.55f; c.underground = true; c.biome = Biome.MOUNTAIN; c.season = GameCalendar.Season.AUTUMN; } },
		{ "19-town-summer-day",  (Setup) c -> { c.f = 0.42f; c.biome = Biome.MEADOW; c.town = true; c.cloudCover = 0.3f; c.temp = 22; } },
		{ "20-town-autumn-eve",  (Setup) c -> { c.f = 0.655f; c.biome = Biome.FOREST; c.town = true; c.season = GameCalendar.Season.AUTUMN; c.humidity = 0.6f; } },
	};

	public static void main( String[] args ) throws Exception {
		GdxNativesLoader.load();
		File out = new File( args.length > 0 ? args[0] : "/tmp/sky" );
		out.mkdirs();
		int W = 200, H = 356;
		if (args.length > 1){
			String[] wh = args[1].split( "x" );
			W = Integer.parseInt( wh[0] ); H = Integer.parseInt( wh[1] );
		}
		for (Object[] sc : SCENES){
			String name = (String)sc[0];
			SkyContext c = new SkyContext( W, H );
			c.seed = 0xBEEF1234L;
			((Setup)sc[1]).apply( c );
			c.finish();
			Pixmap pm = render( c );
			PixmapIO.writePNG( new FileHandle( new File( out, name + ".png" ) ), pm );
			pm.dispose();
			System.out.println( name + "  light=" + f2( c.light ) + " warmth=" + f2( c.warmth ) + " elev=" + f2( c.sunElev ) );
		}
	}

	private static String f2( float v ){ return String.format( java.util.Locale.ROOT, "%.2f", v ); }

	private static Pixmap fresh( int w, int h ){
		Pixmap p = new Pixmap( w, h, Pixmap.Format.RGBA8888 );
		p.setBlending( Pixmap.Blending.None );
		p.setColor( 0 );
		p.fill();
		return p;
	}

	/** the same layers the scene stacks, composited by hand */
	public static Pixmap render( SkyContext c ){
		int W = c.W, H = c.H;
		Pixmap sky = fresh( W, H );
		SkyDome.paint( c, sky );

		float starAlpha = SkyPaint.clamp01( (0.45f - c.light) / 0.35f );
		if (starAlpha > 0.05f && c.cloudCover < 0.85f){
			Pixmap st = fresh( W, c.skyHorizon );
			SkyDome.paintStars( c, st );
			SkyPaint.blit( sky, st, 0, 0, starAlpha * (1f - 0.8f * c.cloudCover) );
			st.dispose();
		}
		if (!c.sunUp && c.aurora){
			int ah = Math.round( H * 0.5f );
			Pixmap a = fresh( W, ah );
			SkyDome.paintAurora( a, W, ah, c.seed );
			SkyPaint.blit( sky, a, 0, Math.round( H * 0.03f ), 0.55f );
			a.dispose();
		}
		if (c.sunElev > -0.14f){
			int gs = Math.round( 44 + 44 * c.warmth + 24 * c.humidity ) | 1;
			Pixmap g = fresh( gs, gs );
			SkyDome.paintGlow( g, gs, 130 );
			tint( g, c.sunColor );
			float a = (c.solarEclipse ? 0.12f : 0.22f + 0.45f * c.warmth) * SkyPaint.clamp01( (c.sunElev + 0.14f) / 0.14f ) * (0.3f + 0.7f * c.sunDisc);
			SkyPaint.blit( sky, g, Math.round( c.sunX - gs / 2f ), Math.round( c.sunY - gs / 2f ), a );
			g.dispose();
		}
		if (c.sunUp){
			if (c.solarEclipse){
				Pixmap e = fresh( 40, 40 ); SkyDome.paintEclipse( e );
				SkyPaint.blit( sky, e, Math.round( c.sunX - 20 ), Math.round( c.sunY - 20 ), 1f ); e.dispose();
			} else {
				Pixmap s = fresh( 24, 24 ); SkyDome.paintSun( c, s );
				SkyPaint.blit( sky, s, Math.round( c.sunX - 12 ), Math.round( c.sunY - 12 ), c.sunDisc ); s.dispose();
			}
		}
		if (c.moonUp){
			if (c.lunarEclipse && !c.sunUp){
				Pixmap h = fresh( 49, 49 ); SkyDome.paintGlow( h, 49, 110 ); tint( h, 0xC03020 );
				SkyPaint.blit( sky, h, Math.round( c.moonX - 24 ), Math.round( c.moonY - 24 ), 0.35f ); h.dispose();
			}
			Pixmap m = fresh( 24, 24 ); SkyDome.paintMoon( c, m );
			SkyPaint.blit( sky, m, Math.round( c.moonX - 12 ), Math.round( c.moonY - 12 ), c.sunUp ? 0.5f : 1f ); m.dispose();
		}
		if (c.sunUp && c.rainbow){
			Pixmap rb = fresh( W, c.skyHorizon + 2 );
			SkyDome.paintRainbow( rb, W, c.skyHorizon + 2, Math.round( W - c.sunX ), c.skyHorizon, Math.round( W * 0.42f ) );
			SkyPaint.blit( sky, rb, 0, 0, 0.7f ); rb.dispose();
		}
		//clouds, placed the way the scene places them
		Random rnd = new Random( c.seed ^ 0xC10CDL );
		float cover = c.cloudCover;
		int nFar = 4 + (int)(cover * 12), nNear = 2 + (int)(cover * 9);
		for (int i = 0; i < nFar + nNear; i++){
			boolean far = i < nFar;
			int w = far ? 22 + rnd.nextInt( 20 ) : 40 + rnd.nextInt( 34 );
			Pixmap ct = fresh( SkyPaint.nextPow2( w ), SkyPaint.nextPow2( w / 2 + 2 ) );
			float x = -40 + rnd.nextFloat() * (W + 40);
			SkyDome.paintCloud( c, ct, w, far, x + w / 2f < c.sunX, c.seed ^ (i * 7331L) );
			float y = far ? 3 + rnd.nextFloat() * (H * 0.3f - 3) : H * 0.1f + rnd.nextFloat() * H * 0.36f;
			SkyPaint.blit( sky, ct, Math.round( x ), Math.round( y ), c.storm ? 0.92f : c.light < 0.25f ? (far ? 0.5f : 0.62f) : (far ? 0.7f : 0.88f) );
			ct.dispose();
		}
		if (cover < 0.4f && c.light > 0.3f){
			int n = 1 + rnd.nextInt( 3 );
			for (int i = 0; i < n; i++){
				int w = W / 2 + rnd.nextInt( W / 3 );
				Pixmap ct = fresh( SkyPaint.nextPow2( w ), 8 );
				SkyDome.paintCirrus( c, ct, w, c.seed ^ (i * 991L) );
				SkyPaint.blit( sky, ct, Math.round( -w / 2f + rnd.nextFloat() * W ), Math.round( 2 + rnd.nextFloat() * H * 0.16f ), 0.8f );
				ct.dispose();
			}
		}
		//the land
		int landH = H - c.landTop;
		Pixmap land = fresh( W, landH );
		SkyHorizon.paint( c, sky, land );
		SkyPaint.blit( sky, land, 0, c.landTop, 1f );
		land.dispose();
		return sky;
	}

	/** what Image.hardlight does to a white glow */
	private static void tint( Pixmap pm, int rgb ){
		for (int y = 0; y < pm.getHeight(); y++){
			for (int x = 0; x < pm.getWidth(); x++){
				int p = pm.getPixel( x, y );
				int a = p & 0xFF;
				if (a == 0) continue;
				pm.setColor( SkyPaint.rgba( rgb, a ) );
				pm.drawPixel( x, y );
			}
		}
	}
}
