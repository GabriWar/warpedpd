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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;

import java.util.ArrayList;

/**
 * Everything the sky painters need to know, captured once. The painters never
 * touch the game's statics themselves, so the same art can be rendered by the
 * scene and by a headless preview from a hand-built context.
 *
 * Geometry is in camera units, one unit per game pixel. The sun rides a full
 * circle: above the horizon by day, below it by night, so the glow it throws on
 * the horizon keeps following it after it has set.
 */
public final class SkyContext {

	public final int W, H;
	public long seed = 1;

	/** one 0..1 clock over the whole cycle: dawn 0-.10, day .10-.60, dusk .60-.70, night .70-1 */
	public float f = 0.35f;

	public float sunX, sunY;
	/** sine of the sun's elevation, -1..1 */
	public float sunElev;
	public boolean sunUp;

	public float moonX, moonY;
	public boolean moonUp;
	/** how much light the moon gives for its phase, 0..1 */
	public float moonBright;
	public GameCalendar.MoonPhase moonPhase = GameCalendar.MoonPhase.FULL_MOON;
	/** where the moon is in its month, 0 new through 0.5 full and back to 1 */
	public float moonAge = 0.5f;
	/** how far in from the edges the sun rises and sets */
	public float edgeMargin = 12f;

	public boolean solarEclipse, lunarEclipse, aurora, rainbow, fog, storm;
	public float cloudCover, wind = 1f, windDir = 1f, precipRate, humidity = 0.5f, temp = 15f;
	public PrecipType precip = PrecipType.NONE;
	public ClimateManager.WeatherOverlayAmbient ambient = ClimateManager.WeatherOverlayAmbient.NONE;

	public GameCalendar.Season season = GameCalendar.Season.SUMMER;
	public int dayOfSeason = 40;
	public WorldModel.Biome biome = WorldModel.Biome.FOREST;
	public boolean town, underground, snow;

	// ---- layout, in camera units ----
	/** the astronomical horizon: where the sun's centre crosses when it rises and sets */
	public int skyHorizon;
	/** the land texture starts here, high enough for the ridges to stand against the sky */
	public int landTop;
	public int groundTop;

	// ---- derived by SkyDome.paint(), then read by everything painted in front ----
	public int zenith, horizonAvg, sunColor = 0xFFF4D6, moonColor = 0xC8D4F0;
	/** daylight, 0 at night to 1 under a high sun, already dimmed by cloud and eclipse */
	public float light;
	/** how orange the sun is, 1 on the horizon and 0 well above it */
	public float warmth;
	public float nightness;
	/** how much of the sun's disc shows through the cloud, 0..1 */
	public float sunDisc = 1f;
	/** the sky's own colour along the horizon, per column; what distant things fade into */
	public int[] horizonRow;

	// ---- things the land painter places that the life layer animates ----
	public final ArrayList<int[]> chimneys = new ArrayList<>();
	public int waterTop = -1, waterBottom = -1;

	public SkyContext( int W, int H ){
		this.W = W;
		this.H = H;
		skyHorizon = Math.round( H * 0.66f );
		landTop = skyHorizon - Math.round( H * 0.17f );
		groundTop = H - Math.max( 10, Math.round( H * 0.065f ) );
	}

	// ------------------------------------------------------------ capture

	/** the live game, as it is right now */
	public static SkyContext capture( int W, int H ){
		SkyContext c = new SkyContext( W, H );
		c.seed = Dungeon.seed;
		c.f = dayFraction();

		c.moonPhase = GameCalendar.moonPhase();
		c.moonAge = GameCalendar.moonProgress();
		c.solarEclipse = ClimateManager.ambientType() == ClimateManager.WeatherOverlayAmbient.CORONA
				&& DayNightCycle.phase() != DayNightCycle.Phase.NIGHT;
		c.lunarEclipse = ClimateManager.isLunarEclipse();
		c.aurora = ClimateManager.ambientType() == ClimateManager.WeatherOverlayAmbient.AURORA;
		c.rainbow = ClimateManager.ambientType() == ClimateManager.WeatherOverlayAmbient.RAINBOW;
		c.ambient = ClimateManager.ambientType();
		c.fog = ClimateManager.isFoggy() || c.ambient == ClimateManager.WeatherOverlayAmbient.MIST;
		c.storm = ClimateManager.isStorming();

		c.cloudCover = SkyPaint.clamp01( ClimateManager.cloudCover() );
		c.wind = Math.max( 1f, ClimateManager.localWindSpeed() );
		float wd = ClimateManager.surfaceWindDir();
		c.windDir = wd > 90 && wd < 270 ? -1f : 1f;
		c.precip = ClimateManager.precipType();
		c.precipRate = ClimateManager.localPrecipRate();
		c.humidity = SkyPaint.clamp01( ClimateManager.surfaceHumidity() );
		c.temp = ClimateManager.localTemp();

		c.season = GameCalendar.season();
		c.dayOfSeason = GameCalendar.dayOfSeason();

		if (Dungeon.level instanceof OverworldLevel && Dungeon.hero != null){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			c.biome = ow.biomeAtCell( Dungeon.hero.pos );
			c.town = ow.inTown( Dungeon.hero.pos );
			c.snow = ow.frozenAt( Dungeon.hero.pos ) || c.temp < 0f
					|| c.biome == WorldModel.Biome.SNOWFIELD;
		} else {
			//from the dungeon, you dream of mountains
			c.underground = true;
			c.biome = WorldModel.Biome.MOUNTAIN;
			c.snow = c.season == GameCalendar.Season.WINTER;
		}
		c.finish();
		return c;
	}

	/** places the sun and the moon; call after the fields above are set. Safe to
	 *  call again after moving the horizon or the margin */
	public void finish(){
		float m = edgeMargin;
		float theta;
		if (f < 0.695f){
			float t = f / 0.695f;
			theta = (float)(t * Math.PI);
			sunX = m + t * (W - 2 * m);
		} else {
			//the sun goes back east under the ground, so its glow ends the night
			//where the dawn will begin
			float t = (f - 0.695f) / 0.305f;
			theta = (float)(Math.PI + t * Math.PI);
			sunX = (W - m) - t * (W - 2 * m);
		}
		sunElev = (float)Math.sin( theta );
		sunY = skyHorizon - sunElev * H * 0.50f;
		sunUp = sunElev > -0.03f;

		float moonArc = f >= 0.60f ? (f - 0.60f) / 0.40f : (f < 0.10f ? 0.90f + f : -1f);
		moonUp = moonArc >= 0 && moonArc <= 1;
		if (moonUp){
			moonX = m + moonArc * (W - 2 * m);
			moonY = skyHorizon - (float)Math.sin( moonArc * Math.PI ) * H * 0.48f;
		}
		switch (moonPhase){
			case NEW_MOON:        moonBright = 0.02f; break;
			case WAXING_CRESCENT: case WANING_CRESCENT: moonBright = 0.28f; break;
			case FIRST_QUARTER:   case LAST_QUARTER:    moonBright = 0.55f; break;
			case WAXING_GIBBOUS:  case WANING_GIBBOUS:  moonBright = 0.8f; break;
			default:              moonBright = 1f;
		}
		if (lunarEclipse) moonBright *= 0.35f;
	}

	/** One continuous 0..1 clock over the whole cycle. Noon is exactly 0.35. */
	public static float dayFraction(){
		float p = DayNightCycle.phaseProgress();
		switch (DayNightCycle.phase()){
			case DAWN:  return 0.10f * p;
			case DAY:   return 0.10f + 0.50f * p;
			case DUSK:  return 0.60f + 0.10f * p;
			default:    return 0.70f + 0.30f * p;
		}
	}

	// ----------------------------------------------------------- lighting

	public boolean winter(){ return season == GameCalendar.Season.WINTER; }
	public boolean autumn(){ return season == GameCalendar.Season.AUTUMN; }
	public boolean spring(){ return season == GameCalendar.Season.SPRING; }
	public boolean summer(){ return season == GameCalendar.Season.SUMMER; }

	public boolean water(){
		return biome == WorldModel.Biome.OCEAN || biome == WorldModel.Biome.BEACH
				|| biome == WorldModel.Biome.RIVER || biome == WorldModel.Biome.SWAMP;
	}

	/**
	 * A surface colour under the current sky. exposure is how squarely it faces the
	 * sun: 1 for a lit face, 0 for one turned away. Shadows take the sky's colour
	 * rather than plain black, and at night everything sinks toward the zenith blue
	 * with a touch of moon on the faces that would catch it.
	 */
	public int lit( int base, float exposure ){
		float L = light;
		int amb = SkyPaint.mix( SkyPaint.scale( base, 0.40f + 0.42f * L ), zenith, 0.22f + 0.16f * nightness );
		int dir = SkyPaint.mix( SkyPaint.scale( base, 0.98f + 0.22f * L ), sunColor, 0.2f * warmth * L );
		int col = SkyPaint.mix( amb, dir, SkyPaint.clamp01( exposure ) * L );
		if (moonUp && nightness > 0.5f){
			col = SkyPaint.mix( col, moonColor, 0.10f * moonBright * SkyPaint.clamp01( exposure ) * nightness );
		}
		return col;
	}

	/** the outline colour for a lit sprite: darker than its shadow, tinted by the sky */
	public int rim( int base ){
		return SkyPaint.scale( SkyPaint.mix( base, zenith, 0.35f ), 0.45f + 0.1f * light );
	}

	/** distant things dissolve into the sky: how much of the horizon colour they take */
	public float haze( float depth ){
		float h = depth * (0.55f + 0.25f * humidity);
		if (fog) h = Math.min( 0.96f, h + 0.35f );
		return SkyPaint.clamp01( h );
	}

	public int horizonColorAt( int x ){
		if (horizonRow == null) return horizonAvg;
		return horizonRow[Math.max( 0, Math.min( W - 1, x ) )];
	}

	/** which side of the world the light comes from: +1 when the sun is to the right */
	public int litSide(){
		float lx = nightness > 0.6f && moonUp ? moonX : sunX;
		return lx >= W / 2f ? 1 : -1;
	}
}
