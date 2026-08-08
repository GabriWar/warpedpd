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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.utils.Random;

/**
 * Look at the sky: a layered, procedural, fully live skyscape.
 *
 *  - the gradient follows the exact time of day (phase + progress)
 *  - REAL constellations (Orion, the Big Dipper, Cassiopeia, Scorpius,
 *    Cygnus, Lyra, Canis Major) drift with the seasons; stars twinkle
 *  - the sun arcs by daylight; the moon rises at night showing its TRUE
 *    current phase, lit side and all
 *  - clouds are procedurally-built sprites in two parallax bands, their
 *    number from the live cloud cover and their drift from the live wind
 *  - auroras ripple on the right nights, rain and snow fall when they fall
 *  - the horizon silhouette matches where you stand (pines, peaks, dunes,
 *    rooftops)
 *
 * Tap anywhere (or back) to return to the game.
 */
public class SkyScene extends PixelScene {

	// ------------------------------------------------------ real star data
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

	private int W, H;

	private Image gradient;
	private Image starLayer;
	private Image celestial;      //sun or moon
	private Image aurora;
	private Image rainbow;
	private Image horizon;

	private Image[] cloudsFar, cloudsNear;
	private float[] cloudSpeedFar, cloudSpeedNear;

	private Image[] precip;
	private float[] precipSpeed;

	private Image[] twinkles;
	private float[] twinklePhase;

	private float auroraT = 0;

	@Override
	public void create() {
		super.create();

		W = (int)Math.ceil( Camera.main.width );
		H = (int)Math.ceil( Camera.main.height );

		DayNightCycle.Phase phase = DayNightCycle.phase();
		float f = dayFraction();
		boolean eclipse = ClimateManager.ambientType() == ClimateManager.WeatherOverlayAmbient.CORONA
				&& phase != DayNightCycle.Phase.NIGHT;
		boolean lunarEcl = ClimateManager.isLunarEclipse();
		boolean darkish = f > 0.655f || f < 0.06f || eclipse;

		// ---- 1. sky gradient: true keyframed degrade ----
		gradient = new Image( gradientTex( eclipse, lunarEcl && darkish ) );
		gradient.scale.set( W / 4f, H / 64f );
		add( gradient );

		// ---- 2. stars + constellations (night skies only) ----
		if (darkish){
			starLayer = new Image( starTex() );
			starLayer.scale.set( W / 256f, H * 0.75f / 256f );
			//deep night is fully starry; twilight fades them
			starLayer.am = phase == DayNightCycle.Phase.NIGHT ? 1f : 0.45f;
			add( starLayer );

			//a handful of bright stars twinkle on top
			twinkles = new Image[14];
			twinklePhase = new float[twinkles.length];
			Random.pushGenerator( Dungeon.seed ^ 0x57A25L );
			for (int i = 0; i < twinkles.length; i++){
				Image t = new Image( TextureCache.createSolid( 0xFFFFFFFF ) );
				t.scale.set( 1 );
				t.x = Random.Int( W );
				t.y = Random.Int( (int)(H * 0.6f) );
				twinklePhase[i] = Random.Float( 6.28f );
				twinkles[i] = t;
				add( t );
			}
			Random.popGenerator();
		}

		// ---- 3. sun AND moon on true astronomical arcs ----
		//sun rides 0.00 -> 0.70 of the clock (dawn to end of dusk); the moon
		//rides the night 0.60 -> 1.00 and lingers into early dawn. at the
		//transitions BOTH hang in the sky - sunset with a rising moon
		boolean sunUp = f < 0.695f;
		float sunArc  = f / 0.695f;
		float moonArc = f >= 0.60f ? (f - 0.60f) / 0.40f
				: (f < 0.10f ? 0.90f + f : -1f);   //pre-dawn setting moon

		if (sunUp){
			float arc = sunArc;
			//low sun glows warm across the horizon
			float lowness = 1f - (float)Math.sin( arc * Math.PI );
			Image glow = new Image( glowTex() );
			float gx = 8 + arc * (W - 40) - 24;
			float gy = H * 0.62f - (float)Math.sin( arc * Math.PI ) * H * 0.45f - 24;
			glow.x = gx; glow.y = gy;
			glow.am = eclipse ? 0.15f : 0.25f + 0.5f * lowness;
			glow.hardlight( lowness > 0.5f ? 0xFF9040 : 0xFFE0A0 );
			add( glow );

			celestial = new Image( eclipse ? eclipseTex() : sunTex() );
			celestial.x = 8 + arc * (W - 40);
			celestial.y = H * 0.62f - (float)Math.sin( arc * Math.PI ) * H * 0.45f;
			add( celestial );
		}
		if (moonArc >= 0 && moonArc <= 1){
			float mx = 8 + moonArc * (W - 40);
			float my = H * 0.62f - (float)Math.sin( moonArc * Math.PI ) * H * 0.45f;
			if (lunarEcl && !sunUp){
				//LUNAR ECLIPSE: the earth's shadow turns the moon to copper
				Image halo = new Image( glowTex() );
				halo.x = mx - 24; halo.y = my - 24;
				halo.am = 0.3f;
				halo.hardlight( 0xC03020 );
				add( halo );
			}
			Image moon = new Image( lunarEcl && !sunUp ? bloodMoonTex() : moonTex( GameCalendar.moonPhase() ) );
			moon.x = mx;
			moon.y = my;
			if (sunUp) moon.am = 0.55f;   //a pale day moon
			add( moon );
			if (celestial == null) celestial = moon;
		}

		// ---- 4. aurora (cold clear nights when the climate says so) ----
		if (!sunUp && ClimateManager.ambientType() == ClimateManager.WeatherOverlayAmbient.AURORA){
			aurora = new Image( auroraTex() );
			aurora.scale.set( Math.max( 1f, W / 256f ), Math.max( 1f, H * 0.55f / 128f ) );
			aurora.x = 0;
			aurora.y = H * 0.04f;
			aurora.am = 0.55f;
			add( aurora );
		}

		// ---- 5. rainbow ----
		if (sunUp && ClimateManager.ambientType() == ClimateManager.WeatherOverlayAmbient.RAINBOW){
			rainbow = new Image( rainbowTex() );
			float rs = W * 0.85f / 256f;
			rainbow.scale.set( rs );
			rainbow.x = (W - 256 * rs) / 2f;
			rainbow.y = H * 0.72f - 128 * rs;
			rainbow.am = 0.65f;
			add( rainbow );
		}

		// ---- 6. clouds: two parallax bands, live cover + live wind ----
		float cover = ClimateManager.cloudCover();
		float wind = Math.max( 1f, ClimateManager.localWindSpeed() );
		float dir = ClimateManager.surfaceWindDir() > 90 && ClimateManager.surfaceWindDir() < 270 ? -1f : 1f;
		boolean storm = ClimateManager.precipType() != PrecipType.NONE && cover > 0.6f;

		int nFar  = 6 + (int)(cover * 14);
		int nNear = 3 + (int)(cover * 10);
		cloudsFar = new Image[nFar];
		cloudsNear = new Image[nNear];
		cloudSpeedFar = new float[nFar];
		cloudSpeedNear = new float[nNear];
		Random.pushGenerator( (Dungeon.seed ^ 0xC10CDL) + Dungeon.cycleTurn / 500 );
		for (int i = 0; i < nFar; i++){
			Image c = new Image( cloudTex( 26 + Random.Int( 22 ), storm, true ) );
			c.x = Random.Float( -30, W );
			c.y = Random.Float( 4, H * 0.35f );
			c.am = storm ? 0.85f : 0.55f;
			cloudSpeedFar[i] = dir * wind * (0.15f + Random.Float( 0.1f ));
			cloudsFar[i] = c;
			add( c );
		}
		for (int i = 0; i < nNear; i++){
			Image c = new Image( cloudTex( 44 + Random.Int( 34 ), storm, false ) );
			c.x = Random.Float( -60, W );
			c.y = Random.Float( H * 0.12f, H * 0.5f );
			c.am = storm ? 0.95f : 0.8f;
			cloudSpeedNear[i] = dir * wind * (0.35f + Random.Float( 0.2f ));
			cloudsNear[i] = c;
			add( c );
		}
		Random.popGenerator();

		// ---- 7. falling precipitation ----
		PrecipType pt = ClimateManager.precipType();
		if (pt != PrecipType.NONE && ClimateManager.localPrecipRate() > 0.05f){
			boolean snow = pt == PrecipType.SNOW;
			int n = 30 + (int)(ClimateManager.localPrecipRate() * 50);
			precip = new Image[n];
			precipSpeed = new float[n];
			for (int i = 0; i < n; i++){
				Image d = new Image( TextureCache.createSolid( snow ? 0xFFEFF4FA : 0x99A8C8E8 ) );
				d.scale.set( 1, snow ? 1 : 3 );
				d.x = Random.Float( W );
				d.y = Random.Float( H );
				precipSpeed[i] = snow ? Random.Float( 12, 22 ) : Random.Float( 70, 110 );
				precip[i] = d;
				add( d );
			}
		}

		// ---- 8. horizon silhouette matching where you stand ----
		horizon = new Image( horizonTex() );
		//stretch across the full screen width, bottom edge ON the bottom -
		//the silhouette fills its 32px texture, so anchoring at H-32*scale
		//keeps every peak whole
		float hs = Math.max( 1f, W / 256f );
		horizon.scale.set( hs );
		horizon.x = 0;
		horizon.y = H - 32 * hs;
		add( horizon );

		// ---- caption + exit ----
		String caption = GameCalendar.season().name().charAt(0)
				+ GameCalendar.season().name().substring(1).toLowerCase()
				+ " " + GameCalendar.dayOfSeason()
				+ "  ·  " + Messages.get( this, "phase_" + phase.name().toLowerCase() )
				+ "  ·  " + (int)ClimateManager.localTemp() + "°";
		RenderedTextBlock txt = PixelScene.renderTextBlock( caption, 7 );
		txt.hardlight( 0xCCDDEE );
		txt.setPos( (W - txt.width()) / 2f, H - 12 );
		align( txt );
		add( txt );

		RenderedTextBlock hint = PixelScene.renderTextBlock( Messages.get( this, "hint" ), 5 );
		hint.hardlight( 0x667788 );
		hint.setPos( (W - hint.width()) / 2f, 3 );
		align( hint );
		add( hint );

		PointerArea leave = new PointerArea( 0, 0, W, H ){
			@Override
			protected void onClick( PointerEvent event ){
				onBackPressed();
			}
		};
		add( leave );

		fadeIn();
	}

	@Override
	public void update() {
		super.update();

		float el = Game.elapsed;

		if (cloudsFar != null){
			for (int i = 0; i < cloudsFar.length; i++){
				cloudsFar[i].x += cloudSpeedFar[i] * el;
				if (cloudsFar[i].x > W + 10) cloudsFar[i].x = -60;
				if (cloudsFar[i].x < -70) cloudsFar[i].x = W + 5;
			}
			for (int i = 0; i < cloudsNear.length; i++){
				cloudsNear[i].x += cloudSpeedNear[i] * el;
				if (cloudsNear[i].x > W + 10) cloudsNear[i].x = -90;
				if (cloudsNear[i].x < -100) cloudsNear[i].x = W + 5;
			}
		}

		if (twinkles != null){
			for (int i = 0; i < twinkles.length; i++){
				twinklePhase[i] += el * (1.5f + i * 0.13f);
				twinkles[i].am = 0.35f + 0.65f * (float)Math.abs( Math.sin( twinklePhase[i] ) );
			}
		}

		if (aurora != null){
			auroraT += el;
			aurora.am = 0.4f + 0.25f * (float)Math.sin( auroraT * 0.7 );
			aurora.x = 6f * (float)Math.sin( auroraT * 0.23 );
		}

		if (precip != null){
			for (int i = 0; i < precip.length; i++){
				precip[i].y += precipSpeed[i] * el;
				precip[i].x += (precip[i].scale.y > 1 ? -20 : 8) * el;
				if (precip[i].y > H){
					precip[i].y = -4;
					precip[i].x = Random.Float( W );
				}
			}
		}
	}

	@Override
	protected void onBackPressed() {
		Game.switchScene( GameScene.class );
	}

	// ------------------------------------------------------- texture bakery

	private SmartTexture tex( String key, int w, int h ){
		SmartTexture t = TextureCache.create( "sky-" + key, w, h );
		t.bitmap.setBlending( Pixmap.Blending.None );
		t.bitmap.setColor( 0x00000000 );
		t.bitmap.fill();
		return t;
	}

	private static int rgba( int rgb, int a ){
		return (rgb << 8) | (a & 0xFF);
	}

	/** One continuous 0..1 clock over the whole cycle: dawn 0-.10, day
	 *  .10-.60, dusk .60-.70, night .70-1. Noon is exactly 0.35. */
	private static float dayFraction(){
		float p = DayNightCycle.phaseProgress();
		switch (DayNightCycle.phase()){
			case DAWN:  return 0.10f * p;
			case DAY:   return 0.10f + 0.50f * p;
			case DUSK:  return 0.60f + 0.10f * p;
			default:    return 0.70f + 0.30f * p;
		}
	}

	//keyframed palettes across the full day: {fraction, top, middle, horizon}
	//a TRUE degrade: every moment interpolates between its neighbours
	private static final float[]  KEY_T = {
			0.00f,     0.045f,    0.10f,     0.35f,     0.55f,     0.615f,    0.66f,     0.70f,     0.85f,     1.00f };
	private static final int[][] KEY_C = {
			{0x241a3e, 0x5e3a58, 0xa05848},  //pre-dawn purple
			{0x35406e, 0xd07038, 0xffc070},  //sunrise blaze
			{0x4a86cc, 0x82b0e0, 0xbfd8ee},  //morning
			{0x2f6ec8, 0x66a2e0, 0xa8ccee},  //noon zenith
			{0x3d78c0, 0x7aa8d8, 0xc8b890},  //late afternoon gold creeping in
			{0x51447e, 0xd06a40, 0xffb050},  //golden hour
			{0x352050, 0xb04858, 0xe07848},  //sunset ember
			{0x141230, 0x2a2050, 0x51325a},  //dusk fading
			{0x040810, 0x0a1122, 0x141e38},  //deep night
			{0x241a3e, 0x5e3a58, 0xa05848},  //wrap to pre-dawn
	};

	private SmartTexture gradientTex( boolean eclipse, boolean bloodMoon ){
		SmartTexture t = tex( "grad", 4, 64 );
		float f = dayFraction();
		int k = 0;
		while (k < KEY_T.length - 2 && f > KEY_T[k+1]) k++;
		float span = KEY_T[k+1] - KEY_T[k];
		float blend = span <= 0 ? 0 : (f - KEY_T[k]) / span;
		int top = mix( KEY_C[k][0], KEY_C[k+1][0], blend );
		int mid = mix( KEY_C[k][1], KEY_C[k+1][1], blend );
		int hor = mix( KEY_C[k][2], KEY_C[k+1][2], blend );
		if (eclipse){
			//the world dims under the corona
			top = mix( top, 0x0a0a18, 0.65f );
			mid = mix( mid, 0x141428, 0.65f );
			hor = mix( hor, 0x282038, 0.55f );
		}
		if (bloodMoon){
			//the whole night blushes rust under an eclipsed moon
			top = mix( top, 0x1c0806, 0.45f );
			mid = mix( mid, 0x30100c, 0.45f );
			hor = mix( hor, 0x481812, 0.40f );
		}
		for (int y = 0; y < 64; y++){
			float fy = y / 63f;
			int rgb = fy < 0.55f ? mix( top, mid, fy / 0.55f ) : mix( mid, hor, (fy - 0.55f) / 0.45f );
			t.bitmap.setColor( rgba( rgb, 0xFF ) );
			t.bitmap.fillRectangle( 0, y, 4, 1 );
		}
		t.bitmap( t.bitmap );
		return t;
	}

	private static int mix( int a, int b, float f ){
		f = Math.max( 0, Math.min( 1, f ) );
		int r = (int)(((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
		int g = (int)(((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
		int bl = (int)((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
		return (r << 16) | (g << 8) | bl;
	}

	private SmartTexture starTex(){
		SmartTexture t = tex( "stars", 256, 256 );
		Pixmap pm = t.bitmap;

		//the background star field: seeded by the run, so it's YOUR sky
		Random.pushGenerator( Dungeon.seed ^ 0x57A55L );
		for (int i = 0; i < 240; i++){
			int x = Random.Int( 256 ), y = Random.Int( 200 );
			int b = 90 + Random.Int( 120 );
			pm.setColor( rgba( (b << 16) | (b << 8) | Math.min( 255, b + 20 ), 0xFF ) );
			pm.drawPixel( x, y );
		}

		//constellations drift with the season: a third of the catalog is up
		//on any given night, sliding westward through the year
		int yearDay = GameCalendar.season().ordinal() * 90 + GameCalendar.dayOfSeason();
		for (int c = 0; c < CONSTELLATIONS.length; c++){
			float slot = ((c * 97 + yearDay) % 360) / 360f;
			if (slot > 0.62f) continue;   //below the horizon tonight
			int ox = (int)(10 + slot * 200);
			int oy = 12 + ((c * 53) % 90);

			float[][] stars = CONSTELLATIONS[c];
			//faint connecting lines first
			pm.setColor( rgba( 0x30405c, 0xFF ) );
			for (int[] line : CONST_LINES[c]){
				drawLine( pm, ox + (int)(stars[line[0]][0] * 1.6f), oy + (int)(stars[line[0]][1] * 1.6f),
						ox + (int)(stars[line[1]][0] * 1.6f), oy + (int)(stars[line[1]][1] * 1.6f) );
			}
			//then the stars, sized by magnitude
			for (float[] st : stars){
				int x = ox + (int)(st[0] * 1.6f), y = oy + (int)(st[1] * 1.6f);
				int size = st[2] >= 2.2f ? 2 : 1;
				pm.setColor( rgba( 0xF2F6FF, 0xFF ) );
				pm.fillRectangle( x, y, size, size );
				if (st[2] >= 1.8f){
					pm.setColor( rgba( 0x8090c0, 0xFF ) );
					pm.drawPixel( x - 1, y ); pm.drawPixel( x + size, y );
					pm.drawPixel( x, y - 1 ); pm.drawPixel( x, y + size );
				}
			}
		}
		Random.popGenerator();

		t.bitmap( pm );
		return t;
	}

	private SmartTexture glowTex(){
		SmartTexture t = tex( "glow", 80, 80 );
		Pixmap pm = t.bitmap;
		for (int y = 0; y < 80; y++){
			for (int x = 0; x < 80; x++){
				double d = Math.hypot( x - 39.5, y - 39.5 );
				if (d < 38){
					int a = (int)(120 * (1 - d / 38) * (1 - d / 38));
					pm.setColor( rgba( 0xFFFFFF, a ) );
					pm.drawPixel( x, y );
				}
			}
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture eclipseTex(){
		SmartTexture t = tex( "eclipse", 40, 40 );
		Pixmap pm = t.bitmap;
		for (int y = 0; y < 40; y++){
			for (int x = 0; x < 40; x++){
				double d = Math.hypot( x - 19.5, y - 19.5 );
				if (d < 9){
					pm.setColor( rgba( 0x060608, 0xFF ) );          //the black disc
				} else if (d < 10.5){
					pm.setColor( rgba( 0xFFF6D8, 0xFF ) );          //the ring of fire
				} else if (d < 13){
					pm.setColor( rgba( 0xFFE8B0, (int)(160 * (13 - d) / 2.5) ) );
				} else if (d < 18 && ((int)(Math.atan2( y - 19.5, x - 19.5 ) * 8 / Math.PI) & 1) == 0){
					pm.setColor( rgba( 0xD8D8F0, (int)(90 * (18 - d) / 5) ) );  //corona streamers
				} else continue;
				pm.drawPixel( x, y );
			}
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture sunTex(){
		SmartTexture t = tex( "sun", 32, 32 );
		Pixmap pm = t.bitmap;
		for (int y = 0; y < 32; y++){
			for (int x = 0; x < 32; x++){
				double d = Math.hypot( x - 15.5, y - 15.5 );
				if (d < 8) pm.setColor( rgba( 0xFFF4C0, 0xFF ) );
				else if (d < 9.5) pm.setColor( rgba( 0xFFD870, 0xFF ) );
				else if (d < 12) pm.setColor( rgba( 0xFFC050, 0x55 ) );
				else continue;
				pm.drawPixel( x, y );
			}
		}
		//rays
		pm.setColor( rgba( 0xFFD870, 0xAA ) );
		for (int i = 0; i < 8; i++){
			double a = i * Math.PI / 4;
			int x1 = 16 + (int)(Math.cos( a ) * 11), y1 = 16 + (int)(Math.sin( a ) * 11);
			int x2 = 16 + (int)(Math.cos( a ) * 15), y2 = 16 + (int)(Math.sin( a ) * 15);
			drawLine( pm, x1, y1, x2, y2 );
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture moonTex( GameCalendar.MoonPhase mp ){
		SmartTexture t = tex( "moon", 32, 32 );
		Pixmap pm = t.bitmap;
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
		for (int y = 0; y < 32; y++){
			for (int x = 0; x < 32; x++){
				double d = Math.hypot( x - 15.5, y - 15.5 );
				if (d >= 9) continue;
				boolean lit;
				if (off == -99) lit = true;
				else if (off == 99) lit = false;
				else {
					double ds = Math.hypot( x - 15.5 - off, y - 15.5 );
					lit = ds >= 9;
				}
				if (lit){
					int c = ((x * 7 + y * 13) % 11 == 0) ? 0xC8CCD8 : 0xE8ECF4;
					pm.setColor( rgba( c, 0xFF ) );
				} else {
					pm.setColor( rgba( 0x202838, 0xFF ) );
				}
				pm.drawPixel( x, y );
			}
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture bloodMoonTex(){
		SmartTexture t = tex( "bloodmoon", 32, 32 );
		Pixmap pm = t.bitmap;
		for (int y = 0; y < 32; y++){
			for (int x = 0; x < 32; x++){
				double d = Math.hypot( x - 15.5, y - 15.5 );
				if (d >= 9) continue;
				//copper disc, mottled maria, darker toward the umbral edge
				int c = ((x * 7 + y * 13) % 11 == 0) ? 0x702018 : 0xB03828;
				if (d > 7) c = mix( c, 0x501410, (float)((d - 7) / 2) );
				pm.setColor( rgba( c, 0xFF ) );
				pm.drawPixel( x, y );
			}
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture cloudTex( int w, boolean storm, boolean far ){
		int h = w / 2;
		//texture is pow2-padded; ALL drawing stays radius-inside the w x h
		//canvas so no blob ever gets guillotined at the edge
		int tw = Integer.highestOneBit( w - 1 ) * 2, th = Integer.highestOneBit( h - 1 ) * 2;
		SmartTexture t = tex( "cloud" + w + (storm ? "s" : "") + (far ? "f" : "n") + Random.Int( 100000 ),
				tw, th );
		Pixmap pm = t.bitmap;
		int base   = storm ? 0x4a5568 : 0xF2F5FA;
		int shade  = storm ? 0x39424f : 0xC8D2E2;
		int lite   = storm ? 0x5d6a80 : 0xFFFFFF;

		int keel = h * 2 / 3;   //the flat cloud base line
		//a row of puffs along the keel, each fully inside the canvas
		int nPuff = 3 + Random.Int( 4 );
		for (int b = 0; b < nPuff; b++){
			int br = h / 5 + Random.Int( Math.max( 1, h / 4 ) );
			int minX = br + 1, maxX = w - br - 2;
			if (maxX <= minX) { br = h / 5; minX = br + 1; maxX = Math.max( minX + 1, w - br - 2 ); }
			int bx = minX + Random.Int( Math.max( 1, maxX - minX ) );
			int by = keel - br / 2 - Random.Int( Math.max( 1, br / 2 ) );
			for (int y = Math.max( 0, by - br ); y <= Math.min( keel, by + br ); y++){
				for (int x = Math.max( 0, bx - br ); x <= Math.min( w - 1, bx + br ); x++){
					double d = Math.hypot( x - bx, (y - by) * 1.35 );
					if (d < br){
						int c = y < by - br / 3 ? lite : (y > by + br / 4 ? shade : base);
						pm.setColor( rgba( c, 0xFF ) );
						pm.drawPixel( x, y );
					}
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
				pm.setColor( rgba( shade, 0xFF ) );
				pm.drawPixel( x, keel );
				pm.setColor( rgba( shade, 0x99 ) );
				pm.drawPixel( x, keel + 1 );
			}
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture auroraTex(){
		SmartTexture t = tex( "aurora", 256, 128 );
		Pixmap pm = t.bitmap;
		//four hanging curtains, green fading through teal to violet
		int[] cols = { 0x40E080, 0x30C8A0, 0x50A0E0, 0x9060D8 };
		for (int band = 0; band < 4; band++){
			for (int x = 0; x < 256; x++){
				double wave = Math.sin( x * 0.045 + band * 2.1 ) * 10 + Math.sin( x * 0.013 + band ) * 8;
				int yTop = (int)(8 + band * 14 + wave);
				//long curtains, with bright vertical rays where the folds bunch
				double fold = Math.sin( x * 0.08 + band * 1.7 );
				int len = 26 + (int)(fold * 12);
				boolean ray = fold > 0.82;
				for (int y = 0; y < len; y++){
					int a = 190 - y * 190 / Math.max( 1, len );
					if (ray) a = Math.min( 255, a + 55 );
					int c = y > len * 2 / 3 ? mix( cols[band], 0xE060A0, 0.4f ) : cols[band];
					pm.setColor( rgba( c, Math.max( 0, a ) ) );
					pm.drawPixel( x, yTop + y );
				}
			}
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture rainbowTex(){
		SmartTexture t = tex( "rainbow", 256, 128 );
		Pixmap pm = t.bitmap;
		int[] cols = { 0xE04040, 0xE09040, 0xE0D040, 0x50C050, 0x5080D0, 0x8050C0 };
		//primary bow, plus a fainter secondary above with the colors reversed
		for (int bow = 0; bow < 2; bow++){
			for (int i = 0; i < cols.length; i++){
				int col = bow == 0 ? cols[i] : cols[cols.length - 1 - i];
				int r = (bow == 0 ? 108 : 122) - i * 3;
				for (double a = Math.PI; a <= Math.PI * 2; a += 0.004){
					//the ends melt into the horizon haze
					double end = Math.min( a - Math.PI, Math.PI * 2 - a );
					int alpha = (int)((bow == 0 ? 0xB0 : 0x48) * Math.min( 1, end / 0.5 ));
					pm.setColor( rgba( col, alpha ) );
					int x = 128 + (int)(Math.cos( a ) * r);
					int y = 124 + (int)(Math.sin( a ) * r);
					if (x >= 0 && x < 256 && y >= 0 && y < 128){
						pm.drawPixel( x, y );
						pm.drawPixel( x, y + 1 );
						pm.drawPixel( x, y + 2 );
					}
				}
			}
		}
		t.bitmap( pm );
		return t;
	}

	private SmartTexture horizonTex(){
		SmartTexture t = tex( "horizon", 256, 32 );
		Pixmap pm = t.bitmap;
		pm.setColor( rgba( 0x080c16, 0xFF ) );

		//what silhouette? where you stand decides
		int kind = 0;   //0 rolling, 1 pines, 2 peaks, 3 dunes, 4 rooftops
		if (Dungeon.level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			switch (ow.biomeAtCell( Dungeon.hero.pos )){
				case FOREST: case SNOWFIELD: kind = 1; break;
				case MOUNTAIN: case FOOTHILLS: kind = 2; break;
				case DESERT: case BEACH: kind = 3; break;
				default: kind = 0;
			}
		} else if (Dungeon.depth == 55){
			kind = 4;
		} else {
			kind = 2;   //from the dungeon, you dream of mountains
		}

		Random.pushGenerator( Dungeon.seed ^ 0x0812L + kind );
		switch (kind){
			case 1:   //pines
				for (int x = 0; x < 256; x += 7 + Random.Int( 6 )){
					int ph = 10 + Random.Int( 14 );
					for (int y = 0; y < ph; y++){
						int half = 1 + (y * 3 / Math.max( 1, ph ));
						pm.fillRectangle( x - half, 31 - ph + y, half * 2 + 1, 1 );
					}
				}
				break;
			case 2:   //peaks
				int y0 = 20;
				for (int x = 0; x < 256; x++){
					y0 += Random.Int( 3 ) - 1;
					y0 = Math.max( 6, Math.min( 26, y0 ) );
					pm.fillRectangle( x, y0, 1, 32 - y0 );
				}
				break;
			case 3:   //dunes
				for (int x = 0; x < 256; x++){
					int y = 24 + (int)(Math.sin( x * 0.05 ) * 4 + Math.sin( x * 0.013 ) * 3);
					pm.fillRectangle( x, y, 1, 32 - y );
				}
				break;
			case 4:   //rooftops
				for (int x = 0; x < 256; ){
					int wdt = 14 + Random.Int( 18 ), hgt = 8 + Random.Int( 12 );
					pm.fillRectangle( x, 31 - hgt, wdt, hgt );
					if (Random.Int( 3 ) == 0) pm.fillRectangle( x + wdt/2 - 1, 27 - hgt, 2, 4 );
					x += wdt + 2 + Random.Int( 5 );
				}
				break;
			default:  //rolling hills
				for (int x = 0; x < 256; x++){
					int y = 22 + (int)(Math.sin( x * 0.03 ) * 5 + Math.sin( x * 0.011 + 2 ) * 3);
					pm.fillRectangle( x, y, 1, 32 - y );
				}
		}
		Random.popGenerator();

		t.bitmap( pm );
		return t;
	}

	private static void drawLine( Pixmap pm, int x1, int y1, int x2, int y2 ){
		pm.drawLine( x1, y1, x2, y2 );
	}
}
