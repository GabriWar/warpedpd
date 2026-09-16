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

import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.Canvas;

import com.badlogic.gdx.graphics.Pixmap;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The land under the sky: ridges standing in aerial perspective, water that
 * mirrors the dome with the sun's path glittering across it, the ground, and the
 * trees, each species drawn as its own small sprite and lit from the sun's side.
 *
 * Everything is painted into one texture that starts at landTop and runs to the
 * bottom of the screen. Coordinates passed around here are screen coordinates;
 * the pixmap offset is applied at the last moment.
 */
public final class SkyHorizon {

	private SkyHorizon(){}

	private static Pixmap land;
	private static int top;
	private static SkyContext ctx;

	private static void put( int x, int y, int rgb ){ px( land, x, y - top, rgb ); }
	private static void mixIn( int x, int y, int rgb, int a ){ blend( land, x, y - top, rgb, a ); }
	private static int at( int x, int y ){ return get( land, x, y - top ); }

	public static void paint( SkyContext c, Pixmap sky, Pixmap landPm ){
		ctx = c; land = landPm; top = c.landTop;
		c.chimneys.clear();

		if (c.water()){
			c.waterTop = c.biome == Biome.RIVER || c.biome == Biome.SWAMP
					? c.groundTop - Math.max( 8, Math.round( c.H * 0.07f ) ) : c.skyHorizon + 1;
			c.waterBottom = c.groundTop - 1;
		} else {
			c.waterTop = c.waterBottom = -1;
		}

		paintField( c );
		paintRidges( c );
		if (c.water()) paintWater( c, sky );
		paintGround( c );
		if (c.town){
			//the town paints its own near band: houses at the edges, a tree or two between
			SkyTown.paint( c, land );
		} else {
			paintMidTrees( c );
			paintNearTrees( c );
		}
	}

	// -------------------------------------------------------------- ridges

	private static float profile( SkyContext c, int ridge, int x ){
		long s = c.seed ^ (0x51D6E00L + ridge * 7919L);
		float H = c.H;
		float amp, base;
		float v;
		switch (c.biome){
			case MOUNTAIN: case FOOTHILLS:
				amp = (ridge == 0 ? 0.15f : ridge == 1 ? 0.10f : 0.055f) * H;
				base = (ridge == 0 ? 0.02f : 0.01f) * H;
				//sharp peaks: folded noise, plus a little fine grit on the slopes
				float t1 = Math.abs( 2 * smooth1( s, x, 46 - ridge * 9 ) - 1 );
				float t2 = Math.abs( 2 * smooth1( s ^ 3, x, 17 ) - 1 );
				v = 0.7f * t1 + 0.3f * t2;
				if (c.biome == Biome.FOOTHILLS) v = 0.5f * v + 0.5f * smooth1( s ^ 5, x, 60 );
				break;
			case DESERT:
				amp = (ridge == 0 ? 0.08f : ridge == 1 ? 0.055f : 0.03f) * H;
				base = 0.005f * H;
				v = 0.6f * smooth1( s, x, 70 - ridge * 15 ) + 0.4f * smooth1( s ^ 3, x, 28 );
				break;
			case SWAMP: case PLAINS: case TUNDRA:
				amp = (ridge == 0 ? 0.045f : ridge == 1 ? 0.03f : 0.015f) * H;
				base = 0;
				v = 0.7f * smooth1( s, x, 90 ) + 0.3f * smooth1( s ^ 3, x, 30 );
				break;
			case OCEAN:
				return -1;
			case BEACH:
				//a headland at one end and open sea the rest of the way
				if (ridge > 0) return -1;
				float side = noise( c.seed, 77, 1 ) > 0.5f ? x / (float)c.W : 1f - x / (float)c.W;
				amp = 0.07f * H; base = 0;
				v = Math.max( 0, 1f - side * 3.2f ) * (0.6f + 0.4f * smooth1( s, x, 40 ));
				if (v <= 0.001f) return -1;
				break;
			default:
				amp = (ridge == 0 ? 0.085f : ridge == 1 ? 0.055f : 0.03f) * H;
				base = 0.01f * H;
				v = 0.65f * smooth1( s, x, 80 - ridge * 14 ) + 0.35f * smooth1( s ^ 3, x, 32 );
		}
		return base + amp * v;
	}

	private static int ridgeBase( SkyContext c, int ridge ){
		switch (c.biome){
			case MOUNTAIN: case FOOTHILLS: return ridge == 0 ? 0x6e7488 : ridge == 1 ? 0x545a6c : 0x3e4352;
			case DESERT:                   return ridge == 0 ? 0xd8b478 : ridge == 1 ? 0xc89c5c : 0xb08448;
			case SNOWFIELD: case TUNDRA:   return ridge == 0 ? 0xc4ccd8 : ridge == 1 ? 0xa8b2c2 : 0x8892a4;
			case SWAMP:                    return ridge == 0 ? 0x4a5a48 : ridge == 1 ? 0x38473a : 0x2a382c;
			case BEACH:                    return 0x4e6650;
			default:                       return ridge == 0 ? 0x486650 : ridge == 1 ? 0x36523e : 0x263c2c;
		}
	}

	private static void paintRidges( SkyContext c ){
		if (c.biome == Biome.OCEAN) return;
		//a ridge ends at its own foot; the field in front of it is painted separately,
		//so a mountain no longer drops a flat wall of one colour down to the grass
		int fillTo = c.water() && c.biome != Biome.RIVER && c.biome != Biome.SWAMP ? c.waterTop - 1 : c.skyHorizon + 3;
		boolean caps = c.snow || c.biome == Biome.SNOWFIELD || (c.biome == Biome.MOUNTAIN && !c.summer());
		int side = c.litSide();
		for (int ridge = 0; ridge < 3; ridge++){
			float depth = ridge == 0 ? 0.95f : ridge == 1 ? 0.6f : 0.3f;
			int base = ridgeBase( c, ridge );
			float hz = c.haze( depth );
			if (c.fog && ridge == 0 && hz > 0.9f) continue;   //the far line is gone in fog
			for (int x = 0; x < c.W; x++){
				float h = profile( c, ridge, x );
				if (h < 0) continue;
				float hl = profile( c, ridge, Math.max( 0, x - 2 ) ), hr = profile( c, ridge, Math.min( c.W - 1, x + 2 ) );
				//the face that leans toward the light is lit: a slope falling to the
				//right faces right
				float slope = (hr - hl) / 4f;
				float facing = clamp01( 0.5f - slope * side * 0.9f );
				int ridgeTop = Math.round( c.skyHorizon - h );
				int skyCol = c.horizonColorAt( x );
				for (int y = ridgeTop; y <= fillTo; y++){
					float down = clamp01( (y - ridgeTop) / Math.max( 6f, h ) );
					float exposure = facing * (1f - 0.35f * down);
					int col = c.lit( base, exposure );
					if (caps){
						float capLine = c.biome == Biome.MOUNTAIN ? 0.55f : 0.0f;   //how far up the snow starts
						float peakness = h / (0.15f * c.H);
						if (c.snow || c.biome == Biome.SNOWFIELD || (peakness > capLine && y - ridgeTop < h * 0.45f * (peakness - capLine) / 0.5f)){
							int snowCol = c.lit( 0xe4eaf2, 0.35f + 0.65f * facing );
							col = mix( col, snowCol, c.snow || c.biome == Biome.SNOWFIELD ? 0.85f : 0.9f );
						}
					}
					//a little grain in the rock face
					if (noise( c.seed ^ ridge, x >> 1, y >> 1 ) > 0.82f) col = mix( col, c.rim( base ), 0.25f );
					col = mix( col, skyCol, hz );
					//the crest catches the sky
					if (y == ridgeTop) col = mix( col, skyCol, 0.35f );
					put( x, y, col );
				}
			}
			//a dark tree line along the nearest ridge where trees grow
			if (ridge == 2 && (c.biome == Biome.FOREST || c.biome == Biome.FOOTHILLS || c.biome == Biome.RIVER
					|| c.biome == Biome.SNOWFIELD || c.biome == Biome.MEADOW)){
				long s = c.seed ^ 0x7EE11EL;
				int treeCol = mix( c.lit( c.biome == Biome.SNOWFIELD ? 0x2a4a3a : 0x1e3a28, 0.4f ), c.horizonColorAt( c.W / 2 ), hz * 0.8f );
				for (int x = 0; x < c.W; x += 1){
					float h = profile( c, ridge, x );
					if (h < 0) continue;
					int ridgeTop = Math.round( c.skyHorizon - h );
					int spike = 1 + hash( s, x >> 1, 1 ) % 3;
					if (hash( s, x >> 1, 2 ) % 4 == 0) spike += 1;
					for (int y = ridgeTop - spike; y <= ridgeTop + 1; y++) put( x, y, treeCol );
					if (c.snow || c.biome == Biome.SNOWFIELD) put( x, ridgeTop - spike, mix( 0xe0e8f0, treeCol, 0.4f ) );
				}
			}
		}
	}

	// --------------------------------------------------------------- field

	/** the open ground between the ridges' feet and the grass at your feet: it
	 *  recedes toward the horizon, hazed at the back and sharp at the front, with
	 *  whatever grows there getting bigger as it comes closer */
	private static void paintField( SkyContext c ){
		if (c.biome == Biome.OCEAN || c.biome == Biome.BEACH) return;
		int fieldTop = c.skyHorizon + 1;
		int fieldBottom = (c.biome == Biome.RIVER || c.biome == Biome.SWAMP) ? c.waterTop - 1 : c.groundTop;
		if (fieldBottom <= fieldTop) return;
		int base = groundBase( c );
		if (c.biome == Biome.MOUNTAIN || c.biome == Biome.FOOTHILLS) base = mix( base, 0x4a7a3c, c.snow ? 0f : 0.45f );
		long s = c.seed ^ 0xF1E1DL;
		int span = fieldBottom - fieldTop;
		for (int y = fieldTop; y <= fieldBottom; y++){
			float t = (y - fieldTop) / (float)span;
			float hz = c.haze( 0.9f * (float)Math.pow( 1f - t, 1.6 ) );
			int rowStep = Math.max( 3, Math.round( 3 + 7 * t ) );
			boolean furrow = !c.town && ((y - fieldTop) % rowStep) == 0;
			for (int x = 0; x < c.W; x++){
				int col = c.lit( base, 0.55f + 0.3f * t );
				float grain = (noise( s, x, y ) - 0.5f) * (6f + 10f * t);
				col = rgb( r( col ) + (int)grain, g( col ) + (int)grain, b( col ) + (int)grain );
				if (furrow) col = scale( col, 0.94f );
				if (c.town) col = cobble( c, s, x, y, t, col );
				col = mix( col, c.horizonColorAt( x ), hz );
				put( x, y, dither( x, y, r( col ), g( col ), b( col ), 30 ) );
			}
		}
		if (c.town) return;
		//what grows in the field, sized by how far away it stands
		int n = Math.max( 8, c.W / 6 );
		for (int i = 0; i < n; i++){
			float t = (float)Math.pow( noise( s, i, 20 ), 0.7 );
			int x = hash( s, i, 21 ) % c.W;
			int y = fieldTop + Math.round( t * (span - 1) );
			float hz = c.haze( 0.9f * (float)Math.pow( 1f - t, 1.6 ) );
			int kind = hash( s, i, 22 ) % 10;
			Canvas cv = null;
			if (c.snow){
				if (kind < 6){ int w = 2 + Math.round( 5 * t ); hline( land, x, x + w, y - top, mix( c.lit( 0xf2f6fa, 0.9f ), c.horizonColorAt( x ), hz ) ); }
				else if (kind < 8) cv = rock( c, s ^ i, 2 + Math.round( 3 * t ) );
			} else if (c.biome == Biome.DESERT){
				if (kind < 3) cv = shrub( c, s ^ i, 2 + Math.round( 3 * t ), 0x8a7a48 );
				else if (kind < 5) cv = rock( c, s ^ i, 2 + Math.round( 3 * t ) );
				else if (kind < 7 && t > 0.4f) cv = cactus( c, s ^ i, 6 + Math.round( 8 * t ), c.litSide() );
			} else if (c.biome == Biome.TUNDRA || c.biome == Biome.MOUNTAIN || c.biome == Biome.FOOTHILLS){
				if (kind < 4) cv = rock( c, s ^ i, 2 + Math.round( 4 * t ) );
				else if (kind < 7) cv = shrub( c, s ^ i, 2 + Math.round( 3 * t ), 0x5a7a44 );
				else tuft( c, x, y, 0x6a7a44 );
			} else if (c.biome == Biome.SWAMP){
				if (kind < 5) reeds( c, s ^ i, x, y );
				else if (kind < 7) cv = shrub( c, s ^ i, 2 + Math.round( 3 * t ), 0x4a6a3c );
			} else {
				if (kind < 3) cv = shrub( c, s ^ i, 2 + Math.round( 4 * t ), leafColor( c, s ^ i ) );
				else if (kind < 6) tuft( c, x, y, c.autumn() ? 0x9a8a3c : 0x5a9a44 );
				else if (kind < 8 && (c.spring() || c.summer()) && t > 0.3f) flower( c, s ^ i, x, y );
				else if (kind == 8 && (c.biome == Biome.MEADOW || c.biome == Biome.PLAINS) && (c.summer() || c.autumn()) && t > 0.35f) cv = haystack( c, s ^ i, 3 + Math.round( 5 * t ) );
			}
			if (cv != null){
				int skyCol = c.horizonColorAt( x );
				for (int k = 0; k < cv.px.length; k++) if (cv.px[k] != -1) cv.px[k] = mix( cv.px[k], skyCol, hz );
				cv.blitTo( land, x - cv.w / 2, y - cv.h - top );
			}
		}
	}

	// --------------------------------------------------------------- water

	private static void paintWater( SkyContext c, Pixmap sky ){
		int base;
		switch (c.biome){
			case OCEAN: case BEACH: base = mix( 0x1e4e78, c.horizonAvg, 0.3f ); break;
			case SWAMP:             base = 0x3a4a34; break;
			default:                base = 0x2a4e5c;
		}
		base = c.lit( base, 0.7f );
		long s = c.seed ^ 0x3A7E8L;
		int span = Math.max( 1, c.waterBottom - c.waterTop );
		boolean sunPath = c.sunUp && c.light > 0.08f;
		boolean moonPath = !sunPath && c.moonUp && c.moonBright > 0.2f && c.nightness > 0.5f;
		float pathX = sunPath ? c.sunX : c.moonX;
		int pathCol = sunPath ? mix( 0xffffff, c.sunColor, 0.45f ) : mix( 0xffffff, c.moonColor, 0.5f );
		float pathStrength = sunPath ? 0.75f + 0.25f * c.warmth : 0.6f * c.moonBright;
		for (int y = c.waterTop; y <= c.waterBottom; y++){
			float t = (y - c.waterTop) / (float)span;
			//the mirror: rows compress with distance, and ripple sideways more near us
			int srcY = Math.max( 0, Math.round( c.skyHorizon - 1 - (y - c.waterTop) * 1.7f ) );
			for (int x = 0; x < c.W; x++){
				int jx = Math.round( (float)Math.sin( y * 1.7 + x * 0.21 + noise( s, y, 1 ) * 6 ) * 1.6f * (0.4f + t) );
				int sx = Math.max( 0, Math.min( c.W - 1, x + jx ) );
				int sample = sky.getPixel( sx, srcY ) >>> 8;
				int col = mix( scale( sample, 0.78f ), base, 0.35f + 0.25f * t );
				//wave crests catch the light
				if (hash( s, x + jx, y ) % 19 == 0){
					col = mix( col, scale( sample, 1.15f ), 0.55f );
				}
				if (sunPath || moonPath){
					float halfW = c.W * (0.045f + 0.06f * (sunPath ? c.warmth : 0.3f)) * (0.7f + 0.9f * t);
					float dx = Math.abs( x - pathX );
					if (dx < halfW){
						float chance = (0.08f + 0.32f * (1f - dx / halfW)) * (0.5f + 0.5f * t);
						if (noise( s, x, y ) < chance) col = mix( col, pathCol, pathStrength );
					}
				}
				put( x, y, col );
			}
		}
		//the shore: a pale line where the water meets the land
		if (c.biome == Biome.OCEAN || c.biome == Biome.BEACH){
			int foam = c.lit( 0xe8eef2, 0.8f );
			for (int x = 0; x < c.W; x++){
				if (hash( s, x, 77 ) % 4 != 0) put( x, c.waterBottom, foam );
				if (hash( s, x, 78 ) % 7 == 0) put( x, c.waterBottom - 1, mix( foam, base, 0.5f ) );
			}
		}
	}

	// -------------------------------------------------------------- ground

	private static int groundBase( SkyContext c ){
		if (c.town) return 0x8c867c;
		if (c.snow) return 0xe0e8f2;
		switch (c.biome){
			case DESERT:            return 0xd6b068;
			case BEACH:             return 0xdcc890;
			case TUNDRA:            return 0x6e7460;
			case SWAMP:             return 0x4a5a34;
			case MOUNTAIN: case FOOTHILLS: return 0x6c7078;
			default:
				if (c.spring()) return 0x5a9a40;
				if (c.autumn()) return 0x8c8c3e;
				if (c.winter()) return 0x6a7a4c;
				return 0x4a7a3c;
		}
	}

	private static void paintGround( SkyContext c ){
		int base = groundBase( c );
		long s = c.seed ^ 0x6E0D5L;
		int span = Math.max( 1, c.H - c.groundTop );
		for (int y = c.groundTop; y < c.H; y++){
			float t = (y - c.groundTop) / (float)span;
			for (int x = 0; x < c.W; x++){
				int col = c.lit( base, 0.7f - 0.35f * t );
				col = mix( col, c.rim( base ), 0.4f * t );
				float grain = (noise( s, x, y ) - 0.5f) * 14f;
				col = rgb( r( col ) + (int)grain, g( col ) + (int)grain, b( col ) + (int)grain );
				if (c.town) col = cobble( c, s, x, y, 1f, col );
				put( x, y, dither( x, y, r( col ), g( col ), b( col ), 30 ) );
			}
		}
		//the edge of the ground against what stands behind it
		int edge = c.lit( base, 0.95f );
		for (int x = 0; x < c.W; x++) if (hash( s, x, 3 ) % 3 != 0) put( x, c.groundTop, edge );

		// ---- dressing ----
		int n = Math.max( 6, c.W / 7 );
		for (int i = 0; i < n; i++){
			int x = hash( s, i, 10 ) % c.W;
			int y = c.groundTop + 1 + hash( s, i, 11 ) % Math.max( 1, span - 2 );
			int kind = hash( s, i, 12 ) % 10;
			if (c.town){
				continue;
			} else if (c.snow){
				if (kind < 5){        //a drift, a little brighter on top
					int w = 3 + hash( s, i, 13 ) % 5;
					hline( land, x, x + w, y - top, c.lit( 0xf2f6fa, 0.9f ) );
					hline( land, x + 1, x + w - 1, y - 1 - top, c.lit( 0xf6f8fc, 1f ) );
				} else if (kind < 7){ //a stone poking through
					put( x, y, c.lit( 0x707480, 0.6f ) ); put( x + 1, y, c.lit( 0x707480, 0.3f ) );
				} else if (kind < 8 && c.biome != Biome.SNOWFIELD){ //a dead tuft
					put( x, y, c.lit( 0x8a7a50, 0.5f ) ); put( x, y - 1, c.lit( 0x8a7a50, 0.6f ) );
				}
			} else if (c.biome == Biome.DESERT){
				if (kind < 4){        //cracks
					line( land, x, y - top, x + 2 + hash( s, i, 13 ) % 3, y + 1 - top, c.rim( base ) );
				} else if (kind < 6){ //pebbles
					put( x, y, c.lit( 0x9a8060, 0.5f ) );
				} else if (kind < 7){ //a dry shrub
					shrub( c, s ^ i, 4 + hash( s, i, 14 ) % 3, 0x8a7a48 ).blitTo( land, x - 3, y - 5 - top );
				}
			} else if (c.biome == Biome.BEACH){
				if (kind < 3) put( x, y, c.lit( 0xf4e8e0, 0.9f ) );                        //a shell
				else if (kind < 5) { put( x, y, c.lit( 0xd8a8a0, 0.7f ) ); put( x + 1, y, c.lit( 0xf4e8e0, 0.8f ) ); }
				else if (kind < 6) put( x, y, c.lit( 0x6a6a70, 0.5f ) );                   //a stone
			} else if (c.biome == Biome.TUNDRA || c.biome == Biome.MOUNTAIN || c.biome == Biome.FOOTHILLS){
				if (kind < 4) rock( c, s ^ i, 3 + hash( s, i, 13 ) % 4 ).blitTo( land, x, y - 3 - top );
				else if (kind < 7) tuft( c, x, y, 0x6a7a44 );
				else if (kind < 8 && !c.winter()) flower( c, s ^ i, x, y );
			} else if (c.biome == Biome.SWAMP){
				if (kind < 5) reeds( c, s ^ i, x, y );
				else if (kind < 7 && c.precip != PrecipType.NONE) puddle( c, x, y );
				else if (kind < 8) put( x, y, c.lit( 0x3a2a1a, 0.4f ) );
			} else {
				//grassland and forest floor
				if (kind < 4) tuft( c, x, y, c.autumn() ? 0x9a8a3c : 0x5a9a44 );
				else if (kind < 6 && (c.spring() || c.summer())) flower( c, s ^ i, x, y );
				else if (kind < 7) put( x, y, c.lit( 0x707078, 0.5f ) );
				else if (kind < 8 && c.autumn() && c.biome == Biome.FOREST) mushroom( c, x, y );
				else if (kind < 9 && c.autumn()) put( x, y, c.lit( hash( s, i, 15 ) % 2 == 0 ? 0xc86a2a : 0xd8a030, 0.7f ) );
			}
			if (c.precipRate > 0.3f && c.precip == PrecipType.RAIN && kind == 9) puddle( c, x, y );
		}
	}

	/** rounded paving stones, each its own tone, in courses that flatten with
	 *  distance; far enough back the stones blur into grain */
	static int cobble( SkyContext c, long s, int x, int y, float t, int col ){
		//stones lie flat, so they are wider than they are tall, and the rows squash
		//together with distance until only their grain is left
		int ch = t < 0.3f ? 1 : 2, cw = t < 0.3f ? 3 : t < 0.65f ? 4 : 5;
		int row = y / ch;
		int shift = (row & 1) == 0 ? 0 : cw / 2;
		int cellX = Math.floorDiv( x + shift, cw ), cellY = row;
		int h = hash( s ^ 0xC0BBL, cellX, cellY );
		int tone = h % 9 - 4;
		int stone = rgb( r( col ) + tone * 4, g( col ) + tone * 4, b( col ) + tone * 3 );
		//worn stones, warm stones, and moss in some of the cracks
		if (h % 7 == 0) stone = mix( stone, 0xb0a494, 0.35f );
		if (h % 11 == 0) stone = mix( stone, 0x8a7a68, 0.3f );
		if (h % 19 == 0) stone = mix( stone, 0x5a7a3c, 0.4f );
		if (ch == 1) return stone;
		boolean joint = ((x + shift) % cw == 0) || (y % ch == 0 && ((x + shift) % cw) != 2);
		if (joint) return scale( stone, 0.82f );
		//a highlight on the near stones' tops
		if (t > 0.6f && y % ch == 1 && (x + shift) % cw == 2) return mix( stone, 0xffffff, 0.10f * c.light );
		return stone;
	}

	private static void tuft( SkyContext c, int x, int y, int col ){
		int a = c.lit( col, 0.8f ), b = c.lit( col, 0.45f );
		put( x, y, b ); put( x - 1, y - 1, a ); put( x + 1, y - 1, a ); put( x, y - 1, b ); put( x, y - 2, a );
	}

	private static void flower( SkyContext c, long s, int x, int y ){
		int[] heads = { 0xf05a6a, 0xf8d04a, 0xe890d8, 0xf6f6f6, 0x6aa0f0 };
		int head = c.lit( heads[hash( s, 1, 1 ) % heads.length], 0.9f );
		put( x, y, c.lit( 0x4a8a3a, 0.6f ) ); put( x, y - 1, c.lit( 0x4a8a3a, 0.7f ) );
		put( x, y - 2, head );
		if (hash( s, 2, 2 ) % 2 == 0){ put( x - 1, y - 2, head ); put( x + 1, y - 2, head ); put( x, y - 3, head ); put( x, y - 2, c.lit( 0xf8e060, 1f ) ); }
	}

	private static void mushroom( SkyContext c, int x, int y ){
		int cap = c.lit( 0xc04030, 0.8f );
		put( x, y, c.lit( 0xe8dcc8, 0.7f ) ); put( x, y - 1, c.lit( 0xe8dcc8, 0.8f ) );
		hline( land, x - 1, x + 1, y - 2 - top, cap ); put( x, y - 3, cap ); put( x - 1, y - 2, mix( cap, 0xffffff, 0.4f ) );
	}

	private static void puddle( SkyContext c, int x, int y ){
		int w = 3 + hash( c.seed, x, y ) % 4;
		int col = mix( c.horizonColorAt( x ), 0x304050, 0.5f );
		hline( land, x, x + w, y - top, col );
		hline( land, x + 1, x + w - 1, y + 1 - top, scale( col, 0.85f ) );
		put( x + 1, y, mix( col, 0xffffff, 0.35f * c.light ) );
	}

	private static void reeds( SkyContext c, long s, int x, int y ){
		int n = 3 + hash( s, 1, 1 ) % 4;
		for (int i = 0; i < n; i++){
			int h = 5 + hash( s, i, 2 ) % 6;
			int rx = x + i * 2 - n;
			vline( land, rx, y - h - top, y - top, c.lit( 0x7a8a48, 0.55f ) );
			put( rx, y - h, c.lit( 0x6a4a2a, 0.7f ) ); put( rx, y - h + 1, c.lit( 0x6a4a2a, 0.7f ) );
		}
	}

	// --------------------------------------------------------------- trees

	private enum Species { PINE, OAK, BIRCH, PALM, DEAD, CACTUS, SHRUB, ROCK, NONE }

	private static Species pick( SkyContext c, long s, int i ){
		int roll = hash( s, i, 100 ) % 100;
		switch (c.biome){
			case FOREST:     return roll < 55 ? Species.PINE : roll < 88 ? Species.OAK : Species.BIRCH;
			case SNOWFIELD:  return roll < 80 ? Species.PINE : roll < 90 ? Species.DEAD : Species.NONE;
			case TUNDRA:     return roll < 45 ? Species.SHRUB : roll < 70 ? Species.ROCK : roll < 82 ? Species.PINE : Species.NONE;
			case MEADOW:     return roll < 40 ? Species.OAK : roll < 55 ? Species.BIRCH : roll < 75 ? Species.SHRUB : Species.NONE;
			case PLAINS:     return roll < 30 ? Species.OAK : roll < 40 ? Species.DEAD : roll < 60 ? Species.SHRUB : Species.NONE;
			case SWAMP:      return roll < 60 ? Species.DEAD : roll < 80 ? Species.SHRUB : Species.NONE;
			case DESERT:     return roll < 55 ? Species.CACTUS : roll < 70 ? Species.DEAD : roll < 85 ? Species.ROCK : Species.NONE;
			case BEACH:      return roll < 70 ? Species.PALM : roll < 85 ? Species.ROCK : Species.NONE;
			case OCEAN:      return roll < 45 ? Species.PALM : Species.NONE;
			case RIVER:      return roll < 40 ? Species.OAK : roll < 65 ? Species.BIRCH : roll < 85 ? Species.SHRUB : Species.NONE;
			case FOOTHILLS:  return roll < 55 ? Species.PINE : roll < 75 ? Species.ROCK : roll < 90 ? Species.OAK : Species.NONE;
			default:         return roll < 50 ? Species.PINE : roll < 75 ? Species.ROCK : Species.NONE;   //mountain
		}
	}

	private static Canvas sprite( SkyContext c, Species sp, long s, int h, int lit ){
		switch (sp){
			case PINE:   return pine( c, s, h, c.snow || (c.winter() && c.temp < 3f), lit );
			case OAK:    return oak( c, s, h, lit );
			case BIRCH:  return birch( c, s, h, lit );
			case PALM:   return palm( c, s, h, lit );
			case DEAD:   return dead( c, s, h, lit, c.biome == Biome.SWAMP );
			case CACTUS: return cactus( c, s, h, lit );
			case SHRUB:  return shrub( c, s, Math.max( 4, h / 3 ), c.biome == Biome.DESERT || c.biome == Biome.TUNDRA ? 0x7a8a48 : leafColor( c, s ) );
			case ROCK:   return rock( c, s, Math.max( 4, h / 3 ) );
			default:     return null;
		}
	}

	private static void paintNearTrees( SkyContext c ){
		long s = c.seed ^ 0x7BEE5L;
		int lit = c.litSide();
		int n = Math.max( 4, c.W / 30 );
		int big = Math.max( 26, Math.round( c.H * 0.165f ) );
		//painted back to front, so the taller trees in the middle rows overlap the
		//ones behind them the right way round
		for (int i = 0; i < n; i++){
			Species sp = pick( c, s, i );
			if (sp == Species.NONE) continue;
			int h = Math.round( big * (0.5f + 0.5f * noise( s, i, 1 )) );
			if (sp == Species.SHRUB || sp == Species.ROCK) h = Math.round( big * 0.45f );
			Canvas cv = sprite( c, sp, s ^ (i * 131L), h, lit );
			if (cv == null) continue;
			int x = Math.round( (i + 0.5f) * c.W / n + (noise( s, i, 2 ) - 0.5f) * (c.W / n) * 0.9f ) - cv.w / 2;
			int baseY = c.groundTop + 2 + hash( s, i, 3 ) % Math.max( 1, (c.H - c.groundTop) / 3 );
			cv.blitTo( land, x, baseY - cv.h - top );
		}
	}

	private static void paintMidTrees( SkyContext c ){
		if (c.biome == Biome.OCEAN || c.biome == Biome.BEACH) return;
		boolean riverside = c.biome == Biome.RIVER || c.biome == Biome.SWAMP;
		//a far row along the ridge feet and, where there is land for it, a middle row
		//halfway to us: three depths of trees is what makes the ground read as deep
		midRow( c, 0x3D7EE5L, c.skyHorizon + 3, Math.max( 8, Math.round( c.H * 0.045f ) ), 0.55f, Math.max( 5, c.W / 14 ) );
		if (!riverside){
			int y = c.skyHorizon + (c.groundTop - c.skyHorizon) / 2;
			midRow( c, 0x3D7EE6L, y, Math.max( 12, Math.round( c.H * 0.08f ) ), 0.2f, Math.max( 3, c.W / 26 ) );
		}
	}

	private static void midRow( SkyContext c, long salt, int baseY, int size, float depth, int n ){
		long s = c.seed ^ salt;
		int lit = c.litSide();
		float hz = c.haze( depth );
		for (int i = 0; i < n; i++){
			Species sp = pick( c, s, i );
			if (sp == Species.NONE || sp == Species.ROCK) continue;
			int h = Math.round( size * (0.6f + 0.4f * noise( s, i, 1 )) );
			Canvas cv = sprite( c, sp, s ^ (i * 977L), h, lit );
			if (cv == null) continue;
			int x = Math.round( (i + 0.5f) * c.W / n + (noise( s, i, 2 ) - 0.5f) * (c.W / n) ) - cv.w / 2;
			//standing far off: faded into the sky like the ground they stand on
			int skyCol = c.horizonColorAt( x + cv.w / 2 );
			for (int k = 0; k < cv.px.length; k++) if (cv.px[k] != -1) cv.px[k] = mix( cv.px[k], skyCol, hz );
			cv.blitTo( land, x, baseY + hash( s, i, 3 ) % 3 - cv.h - top );
		}
	}

	private static int leafColor( SkyContext c, long s ){
		if (c.autumn()){
			int[] fall = { 0xc9702c, 0xd9a232, 0xb8402a, 0x9c7a2a, 0xe08838 };
			return fall[hash( s, 5, 5 ) % fall.length];
		}
		if (c.spring()) return 0x63a84a;
		if (c.winter()) return 0x4a7a44;
		return 0x3f7d36;
	}

	/** a conifer: tiers of foliage that flare and reset, lit on the sun's side */
	static Canvas pine( SkyContext c, long s, int h, boolean snowy, int lit ){
		int w = Math.max( 7, (int)(h * 0.62f) ) | 1;
		Canvas cv = new Canvas( w + 2, h + 2 );
		int cx = w / 2 + 1;
		int th = Math.max( 3, h / 6 ), tw = h >= 26 ? 3 : 2;
		int hf = h - th;
		int tierLen = Math.max( 4, hf / (3 + h / 12) );
		int base = c.winter() || snowy ? 0x2b5c3c : c.spring() ? 0x357c46 : 0x2f6b3f;
		int snowCol = 0xe6ecf4;
		for (int r = 0; r < hf; r++){
			float p = r / (float)hf;
			float maxHalf = 1 + p * (w / 2f - 1);
			float q = (r % tierLen) / (float)Math.max( 1, tierLen - 1 );
			int half = Math.round( 0.35f * maxHalf + 0.65f * maxHalf * q );
			int jagL = noise( s, r, 1 ) > 0.72f ? 1 : 0, jagR = noise( s, r, 2 ) > 0.72f ? 1 : 0;
			boolean tierTop = (r % tierLen) < 2, tierBottom = (r % tierLen) >= tierLen - 1 && r < hf - 1;
			for (int x = cx - half - jagL; x <= cx + half + jagR; x++){
				float rel = half == 0 ? 0 : (x - cx) / (float)half;
				float side = rel * lit;
				float exposure = 0.55f + 0.45f * side;
				if (tierBottom) exposure -= 0.4f;
				if (Math.abs( rel ) > 0.75f && side < 0) exposure -= 0.1f;
				int col;
				if (snowy && (tierTop || (noise( s, x, r ) > 0.86f && side > 0))){
					col = c.lit( snowCol, 0.6f + 0.4f * side );
				} else {
					col = c.lit( base, exposure );
					if (noise( s, x + 31, r ) > 0.9f) col = mix( col, c.lit( base, exposure + 0.3f ), 0.6f );
				}
				cv.set( x, r + 1, col );
			}
		}
		//trunk
		int trunk = 0x5a3a24;
		for (int y = hf; y < h + 1; y++){
			for (int x = cx - tw / 2; x <= cx - tw / 2 + tw - 1; x++){
				float side = tw == 1 ? 0 : ((x - (cx - tw / 2)) / (float)(tw - 1) * 2 - 1) * lit;
				cv.set( x, y, c.lit( trunk, 0.5f + 0.4f * side ) );
			}
		}
		cv.outline( c.rim( base ) );
		return cv;
	}

	/** a broadleaf: a lobed canopy on a short trunk; bare branches in winter */
	static Canvas oak( SkyContext c, long s, int h, int lit ){
		int w = Math.max( 9, (int)(h * 0.95f) ) | 1;
		Canvas cv = new Canvas( w + 2, h + 2 );
		int cx = w / 2 + 1;
		boolean bare = c.winter();
		int trunk = 0x5c3e28;
		int th = Math.max( 4, (int)(h * (bare ? 0.48f : 0.40f)) );
		int tw = h >= 20 ? 3 : 2;
		//trunk, tapering up
		for (int y = h + 1 - th; y <= h; y++){
			float up = (h - y) / (float)th;
			int ww = up > 0.6f ? Math.max( 1, tw - 1 ) : tw;
			for (int x = cx - ww / 2; x <= cx - ww / 2 + ww - 1; x++){
				float side = ww == 1 ? 0 : ((x - (cx - ww / 2)) / (float)(ww - 1) * 2 - 1) * lit;
				cv.set( x, y, c.lit( trunk, 0.5f + 0.4f * side ) );
			}
		}
		if (bare){
			branches( c, cv, s, cx, h + 1 - th + 1, (int)(h * 0.42f), trunk, lit, c.snow );
			cv.outline( c.rim( trunk ) );
			return cv;
		}
		int leaf = leafColor( c, s );
		float cy = 1 + h * 0.34f, rx = w / 2f - 1, ry = h * 0.31f;
		float[][] lobes = { { cx, cy, rx, ry }, { cx - rx * 0.5f, cy + ry * 0.35f, ry * 0.75f, ry * 0.7f },
				{ cx + rx * 0.5f, cy + ry * 0.35f, ry * 0.75f, ry * 0.7f }, { cx, cy - ry * 0.5f, ry * 0.7f, ry * 0.6f } };
		for (int y = 0; y < h + 2; y++){
			for (int x = 0; x < w + 2; x++){
				boolean in = false;
				for (float[] l : lobes){
					if (Math.pow( (x - l[0]) / l[2], 2 ) + Math.pow( (y - l[1]) / l[3], 2 ) <= 1){ in = true; break; }
				}
				if (!in) continue;
				float nx = (x - cx) / rx, ny = (y - cy) / ry;
				float exposure = clamp01( 0.5f + 0.5f * (nx * lit * 0.75f - ny * 0.85f) );
				float grain = noise( s, x, y );
				if (grain > 0.8f) exposure += 0.18f; else if (grain < 0.15f) exposure -= 0.18f;
				int col = c.lit( leaf, exposure );
				if (exposure > 0.92f && grain > 0.55f) col = mix( col, 0xfff0b0, 0.25f * c.light );
				//blossom in spring, a dusting of snow if it is cold and the leaves have not gone
				if (c.spring() && grain > 0.93f && ny < 0) col = mix( col, 0xf8b8d0, 0.8f );
				cv.set( x, y, col );
			}
		}
		cv.outline( c.rim( leaf ) );
		return cv;
	}

	private static void branches( SkyContext c, Canvas cv, long s, int x, int y, int len, int col, int lit, boolean snowy ){
		//four limbs fanning wide from the top of the trunk, each forking twice more
		float[] angles = { -2.55f, -2.0f, -1.2f, -0.6f, -1.6f };
		int n = 4 + hash( s, 9, 9 ) % 2;
		for (int i = 0; i < n; i++){
			float a = angles[i % angles.length] + (noise( s, i, 40 ) - 0.5f) * 0.4f;
			branch( c, cv, s ^ i, x, y - i / 2, a, len, 3, col, lit, snowy, 0 );
		}
	}

	private static void branch( SkyContext c, Canvas cv, long s, int x0, int y0, float a, int len, int depth, int col, int lit, boolean snowy, int gen ){
		int x1 = Math.round( x0 + (float)Math.cos( a ) * len ), y1 = Math.round( y0 + (float)Math.sin( a ) * len );
		int dx = Math.abs( x1 - x0 ), dy = -Math.abs( y1 - y0 ), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
		int cx = x0, cy = y0;
		while (true){
			cv.set( cx, cy, c.lit( col, 0.45f ) );
			if (gen == 0) cv.set( cx + 1, cy, c.lit( col, 0.55f ) );
			if (snowy && !cv.solid( cx, cy - 1 ) && noise( s, cx, cy ) > 0.3f) cv.set( cx, cy - 1, c.lit( 0xe6ecf4, 0.7f ) );
			if (cx == x1 && cy == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy){ err += dy; cx += sx; }
			if (e2 <= dx){ err += dx; cy += sy; }
		}
		if (depth > 0 && len > 2){
			branch( c, cv, s ^ 11, x1, y1, a - 0.55f - noise( s, 1, gen ) * 0.45f, len * 2 / 3, depth - 1, col, lit, snowy, gen + 1 );
			branch( c, cv, s ^ 13, x1, y1, a + 0.45f + noise( s, 2, gen ) * 0.45f, len * 2 / 3, depth - 1, col, lit, snowy, gen + 1 );
			if (gen == 0 && noise( s, 3, gen ) > 0.5f) branch( c, cv, s ^ 17, x1, y1, a + (noise( s, 4, gen ) - 0.5f) * 0.3f, len / 2, depth - 1, col, lit, snowy, gen + 1 );
		}
	}

	/** a birch: a pale ticked trunk under a small light crown */
	static Canvas birch( SkyContext c, long s, int h, int lit ){
		int w = Math.max( 7, (int)(h * 0.6f) ) | 1;
		Canvas cv = new Canvas( w + 2, h + 2 );
		int cx = w / 2 + 1;
		boolean bare = c.winter();
		int bark = 0xece9df;
		int th = (int)(h * (bare ? 0.6f : 0.55f));
		for (int y = h + 1 - th; y <= h; y++){
			cv.set( cx, y, c.lit( bark, 0.55f + 0.35f * lit ) );
			cv.set( cx - 1, y, c.lit( bark, 0.55f - 0.35f * lit ) );
			if (hash( s, y, 1 ) % 5 == 0) cv.set( lit > 0 ? cx - 1 : cx, y, c.lit( 0x2a2a2e, 0.5f ) );
		}
		if (bare){
			//a birch's twigs reach up rather than out
			float[] angles = { -2.1f, -1.57f, -1.05f };
			for (int i = 0; i < 3; i++){
				branch( c, cv, s ^ i, cx, h + 1 - th + 2 + i * 3, angles[i] + (noise( s, i, 40 ) - 0.5f) * 0.3f, (int)(h * 0.36f), 2, 0xd8d4c8, lit, c.snow, 0 );
			}
			cv.outline( c.rim( 0x8a8880 ) );
			return cv;
		}
		int leaf = c.autumn() ? 0xe8c040 : c.spring() ? 0x8cc860 : 0x72b04e;
		float cy = 1 + h * 0.26f, rx = w / 2f - 1, ry = h * 0.26f;
		for (int y = 0; y < h + 2; y++){
			for (int x = 0; x < w + 2; x++){
				float nx = (x - cx) / rx, ny = (y - cy) / ry;
				if (nx * nx + ny * ny > 1) continue;
				//an open crown: gaps let the sky through
				if (noise( s, x, y ) < 0.22f) continue;
				float exposure = clamp01( 0.5f + 0.5f * (nx * lit * 0.7f - ny * 0.8f) );
				cv.set( x, y, c.lit( leaf, exposure ) );
			}
		}
		cv.outline( c.rim( leaf ) );
		return cv;
	}

	/** a palm: a leaning ringed trunk with a crown of drooping fronds */
	static Canvas palm( SkyContext c, long s, int h, int lit ){
		int w = Math.max( 13, (int)(h * 0.9f) ) | 1;
		Canvas cv = new Canvas( w + 2, h + 2 );
		int lean = (hash( s, 1, 1 ) % 2 == 0 ? 1 : -1) * Math.max( 2, w / 6 );
		int x0 = w / 2 + 1 - lean / 2;
		int crownY = 1 + h / 4;
		int trunk = 0x8a6a44;
		int tx = x0, ty = crownY;
		for (float t = 0; t <= 1f; t += 0.02f){
			int x = Math.round( x0 + lean * t * t ), y = Math.round( h + 1 - t * (h - crownY) );
			cv.set( x, y, c.lit( trunk, 0.5f + 0.4f * lit ) );
			cv.set( x - 1, y, c.lit( trunk, 0.5f - 0.35f * lit ) );
			if (y % 3 == 0) cv.set( x - 1, y, c.lit( 0x5a4028, 0.5f ) );
			tx = x; ty = y;
		}
		int leaf = 0x3e8a4a;
		int fronds = 6 + hash( s, 2, 2 ) % 2;
		int len = Math.max( 5, w / 2 - 1 );
		for (int i = 0; i < fronds; i++){
			float a = (float)(-Math.PI * 0.92 + i * (Math.PI * 0.84) / (fronds - 1)) + (noise( s, i, 3 ) - 0.5f) * 0.3f;
			float droop = 0.55f + 0.5f * noise( s, i, 4 );
			for (float t = 0; t <= 1f; t += 0.05f){
				int x = Math.round( tx + (float)Math.cos( a ) * len * t );
				int y = Math.round( ty + (float)Math.sin( a ) * len * t * 0.55f + droop * len * t * t );
				float side = Math.signum( x - tx ) * lit;
				int col = c.lit( leaf, 0.45f + 0.35f * side + 0.2f * (1 - t) );
				cv.set( x, y, col );
				if (t < 0.7f) cv.set( x, y + 1, c.lit( leaf, 0.3f + 0.2f * side ) );
			}
		}
		for (int i = 0; i < 3; i++) cv.set( tx - 1 + i, ty + 1, c.lit( 0x6a4a2a, 0.6f ) );
		cv.outline( c.rim( leaf ) );
		return cv;
	}

	/** a dead tree, gnarled; in a swamp it drips moss */
	static Canvas dead( SkyContext c, long s, int h, int lit, boolean moss ){
		int w = Math.max( 9, (int)(h * 0.8f) ) | 1;
		Canvas cv = new Canvas( w + 2, h + 2 );
		int cx = w / 2 + 1;
		int bark = 0x3e3228;
		int th = (int)(h * 0.55f);
		for (int y = h + 1 - th; y <= h; y++){
			float up = (h - y) / (float)th;
			int ww = up > 0.5f ? 1 : 2;
			for (int x = cx; x < cx + ww; x++) cv.set( x, y, c.lit( bark, 0.5f + 0.3f * lit * (x == cx ? -1 : 1) ) );
		}
		float[] angles = { -2.5f, -1.9f, -1.2f, -0.7f };
		for (int i = 0; i < 3; i++){
			float a = angles[(i + hash( s, 1, 1 )) % angles.length] + (noise( s, i, 40 ) - 0.5f) * 0.6f;
			branch( c, cv, s ^ i, cx, h + 1 - th + 1 + i, a, (int)(h * 0.4f), 2, bark, lit, c.snow, 1 );
		}
		if (moss){
			int mossCol = 0x5a7a4a;
			for (int y = 0; y < h; y++){
				for (int x = 0; x < w + 2; x++){
					if (cv.solid( x, y ) && !cv.solid( x, y + 1 ) && hash( s, x, y ) % 3 == 0){
						int l = 2 + hash( s, x, y + 1 ) % 5;
						for (int k = 1; k <= l; k++) if (!cv.solid( x, y + k )) cv.set( x, y + k, c.lit( mossCol, 0.4f + 0.2f * (k % 2) ) );
					}
				}
			}
		}
		cv.outline( c.rim( bark ) );
		return cv;
	}

	/** a saguaro with two arms */
	static Canvas cactus( SkyContext c, long s, int h, int lit ){
		int w = 13;
		Canvas cv = new Canvas( w + 2, h + 2 );
		int cx = w / 2 + 1;
		int green = 0x4e8a48;
		int armL = h * 2 / 5, armR = h * 3 / 5;
		//the column
		for (int y = 2; y <= h; y++){
			for (int x = cx - 1; x <= cx + 2; x++){
				float side = ((x - cx - 0.5f) / 1.5f) * lit;
				int col = c.lit( green, 0.5f + 0.4f * side );
				if ((x - cx) % 2 == 0) col = c.lit( green, 0.35f + 0.3f * side );   //the ribs
				cv.set( x, y, col );
			}
		}
		cv.set( cx, 1, c.lit( green, 0.7f ) ); cv.set( cx + 1, 1, c.lit( green, 0.7f ) );
		//the arms: out, then up
		for (int k = 0; k < 3; k++){ cv.set( cx - 2 - k, h - armL + 2, c.lit( green, 0.45f ) ); cv.set( cx - 2 - k, h - armL + 3, c.lit( green, 0.35f ) ); }
		for (int y = h - armL - h / 4; y <= h - armL + 3; y++){ cv.set( cx - 5, y, c.lit( green, 0.45f - 0.2f * lit ) ); cv.set( cx - 4, y, c.lit( green, 0.6f - 0.2f * lit ) ); }
		for (int k = 0; k < 3; k++){ cv.set( cx + 3 + k, h - armR + 2, c.lit( green, 0.45f ) ); cv.set( cx + 3 + k, h - armR + 3, c.lit( green, 0.35f ) ); }
		for (int y = h - armR - h / 5; y <= h - armR + 3; y++){ cv.set( cx + 5, y, c.lit( green, 0.6f + 0.2f * lit ) ); cv.set( cx + 6, y, c.lit( green, 0.45f + 0.2f * lit ) ); }
		if (c.spring() && hash( s, 3, 3 ) % 2 == 0) cv.set( cx, 0, c.lit( 0xf070a0, 0.9f ) );
		cv.outline( c.rim( green ) );
		return cv;
	}

	/** a low bush: a few lumps grown together, ragged at the edge */
	static Canvas shrub( SkyContext c, long s, int h, int leaf ){
		int w = h * 2 + 1 + hash( s, 8, 8 ) % 3;
		Canvas cv = new Canvas( w + 2, h + 2 );
		float cx = w / 2f + 1, cy = h * 0.6f + 1, rx = w / 2f, ry = h * 0.6f;
		float[][] lumps = { { cx, cy, rx, ry }, { cx - rx * 0.45f, cy + ry * 0.2f, rx * 0.55f, ry * 0.7f },
				{ cx + rx * 0.4f, cy + ry * 0.15f, rx * 0.5f, ry * 0.75f }, { cx + (noise( s, 1, 1 ) - 0.5f) * rx, cy - ry * 0.4f, rx * 0.4f, ry * 0.5f } };
		int lit = c.litSide();
		for (int y = 0; y < h + 2; y++){
			for (int x = 0; x < w + 2; x++){
				boolean in = false; float edge = 9f;
				for (float[] l : lumps){
					float d = (float)(Math.pow( (x - l[0]) / l[2], 2 ) + Math.pow( (y - l[1]) / l[3], 2 ));
					if (d <= 1){ in = true; edge = Math.min( edge, d ); }
				}
				if (!in) continue;
				if (edge > 0.72f && noise( s, x, y ) > 0.6f) continue;   //ragged
				float nx = (x - cx) / rx, ny = (y - cy) / ry;
				float exposure = clamp01( 0.5f + 0.5f * (nx * lit * 0.7f - ny * 0.9f) );
				if (noise( s, x + 9, y ) > 0.85f) exposure += 0.15f;
				cv.set( x, y, c.lit( leaf, exposure ) );
			}
		}
		cv.outline( c.rim( leaf ) );
		return cv;
	}

	/** a haystack: a straw dome with a pole through it */
	static Canvas haystack( SkyContext c, long s, int h ){
		int w = h + h / 2;
		Canvas cv = new Canvas( w + 2, h + 3 );
		float cx = w / 2f + 1, cy = h + 1, rx = w / 2f, ry = h;
		int lit = c.litSide();
		int straw = c.autumn() ? 0xc8a040 : 0xd8b850;
		for (int y = 0; y < h + 2; y++){
			for (int x = 0; x < w + 2; x++){
				float nx = (x - cx) / rx, ny = (y - cy) / ry;
				if (ny > 0 || nx * nx + ny * ny > 1) continue;
				float exposure = clamp01( 0.5f + 0.5f * (nx * lit * 0.6f - ny * 0.8f) );
				//strokes of straw
				if ((x + y * 2) % 5 == 0) exposure -= 0.15f;
				cv.set( x, y, c.lit( straw, exposure ) );
			}
		}
		for (int y = -1; y < 3; y++) cv.set( (int)cx, y, c.lit( 0x5a3a24, 0.5f ) );
		cv.outline( c.rim( straw ) );
		return cv;
	}

	/** a boulder, lit on top */
	static Canvas rock( SkyContext c, long s, int h ){
		int w = h + 2 + hash( s, 1, 1 ) % 3;
		Canvas cv = new Canvas( w + 2, h + 2 );
		float cx = w / 2f + 1, cy = h * 0.6f + 1, rx = w / 2f, ry = h * 0.6f;
		int grey = c.biome == Biome.DESERT ? 0x9a7a58 : 0x707480;
		int lit = c.litSide();
		for (int y = 0; y < h + 2; y++){
			for (int x = 0; x < w + 2; x++){
				float nx = (x - cx) / rx, ny = (y - cy) / ry;
				if (nx * nx + ny * ny > 1) continue;
				float exposure = clamp01( 0.5f + 0.5f * (nx * lit * 0.5f - ny * 1.1f) );
				cv.set( x, y, c.lit( grey, exposure ) );
			}
		}
		//a crack
		int kx = (int)cx - 1 + hash( s, 2, 2 ) % 3;
		for (int y = (int)(cy - ry * 0.6f); y < cy + ry * 0.4f; y++){ cv.set( kx, y, c.lit( grey, 0.15f ) ); if (y % 2 == 0) kx += hash( s, y, 3 ) % 2 == 0 ? 1 : -1; }
		cv.outline( c.rim( grey ) );
		return cv;
	}
}
