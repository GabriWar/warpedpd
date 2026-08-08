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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

/**
 * The deterministic world generator. Terrain at any world coordinate is a pure
 * function of (worldSeed, wx, wy, seasonal shift), so any window of the
 * infinite world can be regenerated identically at any time - and re-derived
 * for another point of the year when the seasons turn (see seasonShift).
 *
 * The landscape pipeline, per cell:
 *  - gradient-noise fBm for the base fields (organic detail at every scale)
 *  - DOMAIN WARPING bends ridgelines and coastlines into eroded shapes
 *  - RIDGED MULTIFRACTAL mountains chain peaks into ranges
 *  - RIVERS carve the near-zero set of a warped low-frequency field
 *  - climate: altitude lapse rate, river-corridor lushness, rain shadow
 *
 * PERFORMANCE: everything is computed in ONE pass per cell via sample() -
 * the warp vector, continent, ridge and river fields are each evaluated once
 * and shared by elevation, moisture, temperature and the biome bands. The
 * previous layered implementation re-derived them up to four times per cell,
 * which made window regeneration the main source of rebase lag.
 */
public class WorldModel {

	// ------------------------------------------------------------ hashing

	private static long hash( long seed, long x, long y ){
		long h = seed;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= y * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	// ------------------------------------------------------- gradient noise

	private static final float[] GRAD_X = new float[16];
	private static final float[] GRAD_Y = new float[16];
	static {
		for (int i = 0; i < 16; i++){
			double a = i * Math.PI / 8;
			GRAD_X[i] = (float)Math.cos( a );
			GRAD_Y[i] = (float)Math.sin( a );
		}
	}

	private static float fade( float t ){
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

	/** Perlin-style gradient noise, roughly in [-1, 1]. */
	private static float grad( long seed, float x, float y ){
		int gx = (int)Math.floor( x ), gy = (int)Math.floor( y );
		float fx = x - gx, fy = y - gy;

		int h00 = (int)(hash( seed, gx,   gy   ) & 15);
		int h10 = (int)(hash( seed, gx+1, gy   ) & 15);
		int h01 = (int)(hash( seed, gx,   gy+1 ) & 15);
		int h11 = (int)(hash( seed, gx+1, gy+1 ) & 15);

		float d00 = GRAD_X[h00]*fx     + GRAD_Y[h00]*fy;
		float d10 = GRAD_X[h10]*(fx-1) + GRAD_Y[h10]*fy;
		float d01 = GRAD_X[h01]*fx     + GRAD_Y[h01]*(fy-1);
		float d11 = GRAD_X[h11]*(fx-1) + GRAD_Y[h11]*(fy-1);

		float u = fade( fx ), v = fade( fy );
		float a = d00 + (d10-d00)*u;
		float b = d01 + (d11-d01)*u;
		return (a + (b-a)*v) * 1.41f;
	}

	/** fractal Brownian motion of gradient noise, in [0, 1]. */
	private static float fbm( long seed, float x, float y, int octaves ){
		float sum = 0, amp = 0.5f, tot = 0;
		for (int o = 0; o < octaves; o++){
			sum += amp * grad( seed + o * 0x9E37L, x, y );
			tot += amp;
			amp *= 0.5f;
			x *= 2.03f; y *= 2.03f;
		}
		return clamp01( 0.5f + 0.5f * sum / tot );
	}

	/** ridged multifractal: folded noise whose maxima chain into ridges, [0,1]. */
	private static float ridged( long seed, float x, float y, int octaves ){
		float sum = 0, amp = 0.5f, tot = 0;
		for (int o = 0; o < octaves; o++){
			float n = 1f - Math.abs( grad( seed + o * 0x51DEL, x, y ) );
			sum += amp * n * n;
			tot += amp;
			amp *= 0.5f;
			x *= 2.11f; y *= 2.11f;
		}
		return clamp01( sum / tot );
	}

	private static float clamp01( float v ){
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}

	private static float smooth( float t ){
		t = clamp01( t );
		return t * t * (3 - 2 * t);
	}

	// ----------------------------------------------------- the one-pass core

	public enum Biome {
		OCEAN, RIVER, BEACH, PLAINS, MEADOW, FOREST, SWAMP, DESERT,
		TUNDRA, SNOWFIELD, FOOTHILLS, MOUNTAIN
	}

	//global feature scale: every biome/continent/river field is this many
	//times wider than the first-cut world (whose regions were pocket-sized)
	public static final float S = 5f;

	//below this temperature the world freezes over
	public static final float FREEZE = 0.30f;

	// ----------------------------------------------------------- seasons

	//the SEASONAL TEMPERATURE OFFSET in effect for every sample() call: added to
	//the 0..1 temperature field, so the snow line creeps south in winter and
	//back north in summer, lakes freeze over and thaw, and the biome bands
	//that hang off temperature follow. sample() must stay a pure function of
	//(seed, wx, wy, shift): this is a SNAPSHOT the game sets from the main
	//thread (OverworldLevel, on season-stamp changes) and the worker-thread
	//pre-generator only reads - it never calls into the calendar or the
	//climate itself. generatePristine reads it ONCE per window so a window is
	//never half-winter, half-summer
	private static volatile float seasonShift = 0f;

	//a NETWORK MIRROR generates the world from the HOST's snapshot, which the
	//client's own calendar knows nothing about: while one is held, the local
	//calendar path (setSeasonShift, below) is ignored, or a client's window,
	//biome names and dressing would drift out of the host's point of the year
	private static volatile boolean shiftHeld = false;

	/** The seasonal offset sample() is using right now (the snapshot). */
	public static float seasonShift(){ return seasonShift; }

	/** Is a packet-driven snapshot in force? */
	public static boolean seasonShiftHeld(){ return shiftHeld; }

	/** The local calendar's snapshot. Ignored while a network one is held. */
	public static void setSeasonShift( float shift ){
		if (shiftHeld) return;
		seasonShift = shift;
	}

	/**
	 * Holds a host's snapshot in force. MUST be called before every network
	 * (re)generation: the value is what the whole window - terrain, the snow
	 * line, biome names, the dressing - is derived for.
	 */
	public static void holdSeasonShift( float shift ){
		seasonShift = shift;
		shiftHeld = true;
	}

	/** The local calendar takes the wheel back (OverworldLevel.refreshSeasonShift). */
	public static void releaseSeasonShift(){ shiftHeld = false; }

	//the calendar's contribution: a smooth sinusoid over the year, 0 at the
	//spring and autumn midpoints, SUMMER_SHIFT at midsummer, WINTER_SHIFT at
	//midwinter (the contrast-stretched field makes 0.18 a wide snow creep)
	public static final float SUMMER_SHIFT = 0.10f, WINTER_SHIFT = -0.18f;
	//the weather's contribution: a cold snap pushes the line one way, a heat
	//wave the other (OverworldLevel bands the surface temperature into it)
	public static final float CLIMATE_SHIFT = 0.04f;

	/** Season + day -> the calendar part of the shift. MAIN THREAD: reads the calendar. */
	public static float calendarShift(){
		return calendarShift( xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.season(),
				(xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.dayOfSeason() - 0.5f)
						/ Math.max( 1, xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.daysInCurrentSeason() ) );
	}

	/** The calendar part of the shift for a season and a progress 0..1 through it. */
	public static float calendarShift( xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season season, float t ){
		//year phase: 0 at the first day of spring, each season a quarter turn.
		//sin peaks at midsummer (0.375) and troughs at midwinter (0.875)
		float p = (season.ordinal() + clamp01( t )) * 0.25f;
		float s = (float)Math.sin( 2 * Math.PI * (p - 0.125f) );
		return s > 0 ? s * SUMMER_SHIFT : -s * WINTER_SHIFT;
	}

	/**
	 * The surface temperature (deg C) the climate expects at this point of the
	 * year - the same curve as ClimateManager.seasonalBaselineTemp, which is
	 * private there. The world bands the live surface temperature's deviation
	 * from it into the CLIMATE_SHIFT term.
	 */
	public static float seasonNormC(){
		float t = (float) xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.dayOfSeason()
				/ Math.max( 1, xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.daysInCurrentSeason() );
		switch (xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.season()){
			case SPRING: return 5f + 13f * t;
			case SUMMER: return 18f + 14f * (float)Math.sin( Math.PI * t );
			case AUTUMN: return 18f - 13f * t;
			case WINTER: return 5f - 12f * (float)Math.sin( Math.PI * t );
			default:     return 12f;
		}
	}

	//the town's snow core holds year-round but its field breathes with the
	//shift: the perennial-snow radius is TOWN_COLD times this (0.55 at
	//midsummer, 0.8 at the equinoxes, 1.15 at midwinter, in between otherwise)
	static float townColdFactor( float shift ){
		return shift > 0 ? 0.8f - 2.5f * shift : 0.8f + 1.944f * shift;
	}

	//the town stands at world origin on ground that is always flat, dry land
	//(within TOWN_IN cells, fading out by TOWN_LAND) in the middle of its own
	//snowfield (a wider cold spot, fading out by TOWN_COLD)
	public static final int TOWN_IN = 40, TOWN_LAND = 150, TOWN_COLD = 450;
	//rivers fade out over this many cells before the dry TOWN_IN radius
	public static final int RIVER_DRY = 16;

	/** 1 at the town, 0 past `out`; the blend weight of the town's override. */
	public static float townInfluence( long seed, int wx, int wy, int out ){
		float d = (float)Math.sqrt( (float)wx*wx + (float)wy*wy );
		if (d <= TOWN_IN) return 1f;
		if (d >= out * 1.35f) return 0f;
		//a ragged rim rather than a compass circle: the radius wanders with
		//the bearing, so the snowfield's edge looks like any other biome's
		float ang = (float)Math.atan2( wy, wx );
		float wob = 0.7f + 0.65f * fbm( seed ^ 0x70B0L, (float)Math.cos( ang ) * 3f + 5f, (float)Math.sin( ang ) * 3f + 5f, 3 );
		float edge = TOWN_IN + (out - TOWN_IN) * wob;
		if (d >= edge) return 0f;
		return smooth( 1f - (d - TOWN_IN) / (edge - TOWN_IN) );
	}

	private static final float SEA = 0.335f;

	/** Everything the generator knows about one world cell. */
	public static class Sample {
		public float elev, elevRaw, moisture, temperature, river, open;
		public Biome biome;
	}

	//how strongly a point sits inside a river channel, [0,1] - warped coords in
	private static float riverness( long seed, float x, float y ){
		float n = fbm( seed ^ 0x21E45L, x/(380f*S), y/(380f*S), 4 );
		float d = Math.abs( n - 0.5f );
		float w = (0.008f + 0.007f * fbm( seed ^ 0x77AA1L, x/(240f*S), y/(240f*S), 2 )) / S;
		if (d >= w * 2.2f) return 0;
		return smooth( 1f - d / (w * 2.2f) );
	}

	/**
	 * The full generator state of one cell in a single pass. The warp vector,
	 * continent, ridge and river fields are computed once and shared by every
	 * derived value.
	 */
	public static Sample sample( long seed, int wx, int wy, Sample out ){
		return sample( seed, wx, wy, seasonShift, out );
	}

	/** ...with an explicit seasonal shift: the pure core. */
	public static Sample sample( long seed, int wx, int wy, float shift, Sample out ){
		if (out == null) out = new Sample();

		//domain warp, shared by every warped field
		float wvx = 80f * S * (fbm( seed ^ 0xAAF1L, wx/(140f*S), wy/(140f*S), 3 ) - 0.5f);
		float wvy = 80f * S * (fbm( seed ^ 0xBB2EL, wx/(140f*S), wy/(140f*S), 3 ) - 0.5f);
		float x = wx + wvx, y = wy + wvy;

		float cont  = fbm( seed ^ 0xC047L, x/(640f*S), y/(640f*S), 4 );
		float hills = fbm( seed ^ 0x8177L, x/(90f*S),  y/(90f*S),  5 );
		float ridge = ridged( seed ^ 0x51D6E5L, x/(320f*S), y/(320f*S), 4 );

		float mountainMask = smooth( (cont - 0.38f) * 3.2f );
		float eRaw = 0.52f * cont + 0.48f * ridge * mountainMask;
		float elev = 0.50f * cont + 0.20f * hills + 0.48f * ridge * mountainMask;

		float rv = riverness( seed, x, y );
		float town = townInfluence( seed, wx, wy, TOWN_LAND );
		float cold = townInfluence( seed, wx, wy, Math.round( TOWN_COLD * townColdFactor( shift ) ) );
		if (town > 0){
			//the town patch: level and above the sea. rivers run through the
			//outskirts UNCHANGED (the roads bridge them) instead of dying in a
			//point at the land rim - only the footprint's own apron, the inner
			//TOWN_IN radius, stays dry: a river that reaches it fades out over
			//the last RIVER_DRY cells of its approach
			float d = (float)Math.sqrt( (float)wx*wx + (float)wy*wy );
			if (d < TOWN_IN + RIVER_DRY){
				rv = rv * smooth( (d - TOWN_IN) / RIVER_DRY );
			}
			elev = elev + town * (0.47f - elev);
			eRaw = eRaw + town * (0.47f - eRaw);
		}
		if (rv > 0){
			elev = elev - rv * (elev - 0.30f);
		}
		out.elev = clamp01( elev );
		out.elevRaw = clamp01( eRaw );
		out.river = rv;

		//moisture shares the warp and the river field (rivers are lush at the
		//exact cells they carve - the old separate warp sampled them offset)
		float m = 0.7f * fbm( seed ^ 0x5EEDF00DL, x/(260f*S), y/(260f*S), 4 )
				+ 0.3f * fbm( seed ^ 0xDA771E57L, x/(60f*S), y/(60f*S), 3 )
				+ 0.25f * rv;
		//rain shadow: continent+ridge sampled upwind along the warped axis -
		//no second warp evaluation needed at this offset scale
		float upCont  = fbm( seed ^ 0xC047L, (x-40*S)/(640f*S), y/(640f*S), 4 );
		float upRidge = ridged( seed ^ 0x51D6E5L, (x-40*S)/(320f*S), y/(320f*S), 4 );
		float upRaw = clamp01( 0.52f * upCont
				+ 0.48f * upRidge * smooth( (upCont - 0.38f) * 3.2f ) );
		m -= 0.18f * smooth( (upRaw - 0.62f) * 4f );
		if (cold > 0) m = m + cold * (0.70f - m);   //snowfield, not bare tundra
		out.moisture = clamp01( m );

		//temperature: region noise minus the altitude lapse (eRaw is free now)
		float t = 0.8f * fbm( seed ^ 0x7E3197EAL, wx/(520f*S), wy/(520f*S), 3 )
				+ 0.2f * fbm( seed ^ 0x31B0B0B0L, wx/(130f*S), wy/(130f*S), 2 );
		//stretch the contrast: fbm hugs 0.5, which left the frozen band a rarity
		t = 0.5f + (t - 0.5f) * 2.2f;
		t -= 0.45f * smooth( (eRaw - 0.55f) * 3f );
		//the season: the whole field slides with the calendar (and the
		//weather), which is what moves the snow line and freezes the lakes
		t += shift;
		if (cold > 0) t = t + cold * (0.10f - t);   //the town is always snowed under
		out.temperature = clamp01( t );

		//openness: rolling clean fields and forest glades where it runs high
		out.open = fbm( seed ^ 0x0BE7L, wx/(180f*S), wy/(180f*S), 2 );

		//biome bands with edge dither
		float dither = 0.015f * (fbm( seed ^ 0xD17E4L, wx/7f, wy/7f, 2 ) - 0.5f);
		float e = out.elev + dither;

		if (e < SEA){
			out.biome = (rv > 0.35f && eRaw > SEA + 0.03f) ? Biome.RIVER : Biome.OCEAN;
		} else if (e < SEA + 0.014f){
			out.biome = Biome.BEACH;
		} else if (e > 0.700f){
			out.biome = Biome.MOUNTAIN;
		} else if (e > 0.625f){
			out.biome = Biome.FOOTHILLS;
		} else {
			float moist = out.moisture + dither * 2f;
			float temp  = out.temperature;
			if (temp < FREEZE){
				out.biome = moist > 0.52f ? Biome.SNOWFIELD : Biome.TUNDRA;
			} else if (temp > 0.58f && moist < 0.40f){
				out.biome = Biome.DESERT;
			} else if (moist > 0.74f && temp > 0.40f){
				out.biome = Biome.SWAMP;
			} else if (moist > 0.60f){
				out.biome = Biome.FOREST;
			} else if (moist > 0.42f){
				out.biome = Biome.MEADOW;
			} else {
				out.biome = Biome.PLAINS;
			}
		}
		return out;
	}

	// ------------------------------------------- public field views (compat)

	public static Biome biomeAt( long seed, int wx, int wy ){
		return sample( seed, wx, wy, null ).biome;
	}

	/** The biome at the annual MEAN (shift 0): what the land IS, season aside.
	 *  Anything structural (where a village may stand) must key off this, or
	 *  the answer would depend on when it was first asked. */
	public static Biome baseBiomeAt( long seed, int wx, int wy ){
		return sample( seed, wx, wy, 0f, null ).biome;
	}

	public static float elevation( long seed, int wx, int wy ){
		return sample( seed, wx, wy, null ).elev;
	}

	public static float moisture( long seed, int wx, int wy ){
		return sample( seed, wx, wy, null ).moisture;
	}

	public static float temperature( long seed, int wx, int wy ){
		return sample( seed, wx, wy, null ).temperature;
	}

	//small-scale wobble field for footpaths (axis 0/1)
	static float pathWobble( long seed, int wx, int wy, int axis ){
		return fbm( seed ^ (axis == 0 ? 0xF007L : 0xF008L), wx/9f, wy/9f, 2 );
	}

	// ------------------------------------------------------------ terrain

	/** Wilderness terrain from an already-computed sample. */
	public static int wildTerrain( long seed, int wx, int wy, Sample s ){

		int scatter = (int)(hash( seed ^ 0x7C377E12L, wx, wy ) >>> 40);

		switch (s.biome){
			case OCEAN:
			case RIVER:
				//cold waters freeze into walkable (slippery) ice
				return s.temperature < FREEZE ? Terrain.FROZEN_WATER : Terrain.WATER;
			case BEACH:
				//sand, with the odd shrub clinging on; snowy shores up north
				if (s.temperature < FREEZE) return Terrain.SNOW;
				return scatter % 43 == 0 ? Terrain.SHRUB : Terrain.EMPTY_SP;
			case MOUNTAIN:
				return Terrain.WALL;
			case FOOTHILLS:
				if (scatter % 13 == 0) return Terrain.WALL;
				if (scatter % 9 == 0)  return Terrain.BOULDER;
				return Terrain.EMPTY;
			case DESERT:
				if (scatter % 37 == 0) return Terrain.SHRUB;
				if (scatter % 89 == 0) return Terrain.WALL;
				return Terrain.EMPTY_SP;
			case SWAMP:
				if (scatter % 5 == 0)  return Terrain.WATER;
				if (scatter % 4 == 0)  return Terrain.HIGH_GRASS;
				if (scatter % 19 == 0) return Terrain.MUSHROOM_PATCH;
				if (scatter % 31 == 0) return Terrain.TREE_OAK;
				return Terrain.GRASS;
			case FOREST:
				//glades: where the openness field runs high the wood thins
				//into a clean grassy clearing
				if (s.open > 0.72f) return Terrain.GRASS;
				//a real woodland: pines and oaks between the undergrowth
				if (scatter % 6 == 0)  return Terrain.TREE_PINE;
				if (scatter % 11 == 0) return Terrain.TREE_OAK;
				if (scatter % 7 == 0)  return Terrain.SHRUB;
				if (scatter % 3 == 0)  return Terrain.HIGH_GRASS;
				return Terrain.GRASS;
			case MEADOW:
				//open rolling fields: the highest band is perfectly bare grass,
				//below it grass with wildflowers only
				if (s.open > 0.72f) return Terrain.GRASS;
				if (s.open > 0.60f){
					return scatter % 17 == 0 ? Terrain.FLOWER_PATCH : Terrain.GRASS;
				}
				if (scatter % 23 == 0) return Terrain.SHRUB;
				if (scatter % 13 == 0) return Terrain.FLOWER_PATCH;
				if (scatter % 47 == 0) return Terrain.TREE_OAK;
				if (scatter % 7 == 0)  return Terrain.HIGH_GRASS;
				return Terrain.GRASS;
			case TUNDRA:
				if (scatter % 17 == 0) return Terrain.FROZEN_WATER;
				if (scatter % 29 == 0) return Terrain.BOULDER;
				if (scatter % 9 == 0)  return Terrain.GRASS;
				return Terrain.EMPTY;
			case SNOWFIELD:
				if (scatter % 13 == 0
						&& townInfluence( seed, wx, wy, TOWN_LAND ) == 0) return Terrain.FROZEN_WATER;
				if (scatter % 37 == 0) return Terrain.TREE_PINE;
				if (scatter % 41 == 0) return Terrain.SHRUB;
				return Terrain.SNOW;
			case PLAINS: default:
				//flat open country where the openness field runs high -
				//the highest band is utterly bare, below it the odd wildflower
				if (s.open > 0.72f){
					return scatter % 9 == 0 ? Terrain.GRASS : Terrain.EMPTY;
				}
				if (s.open > 0.60f){
					if (scatter % 37 == 0) return Terrain.FLOWER_PATCH;
					return scatter % 9 == 0 ? Terrain.GRASS : Terrain.EMPTY;
				}
				if (scatter % 31 == 0) return Terrain.BOULDER;
				if (scatter % 43 == 0) return Terrain.FLOWER_PATCH;
				if (scatter % 11 == 0) return Terrain.GRASS;
				return Terrain.EMPTY;
		}
	}

	/** The wilderness terrain only - what the land looks like untouched. */
	public static int wildTerrainAt( long seed, int wx, int wy ){
		return wildTerrain( seed, wx, wy, sample( seed, wx, wy, null ) );
	}

	/** The terrain of the untouched world at a world coordinate, including
	 *  the civilization layer (villages, roads, ruins). */
	public static int terrainAt( long seed, int wx, int wy ){
		int wild = wildTerrainAt( seed, wx, wy );
		int structure = WorldStructures.terrainAt( seed, wx, wy, wild );
		return structure != -1 ? structure : wild;
	}
}
