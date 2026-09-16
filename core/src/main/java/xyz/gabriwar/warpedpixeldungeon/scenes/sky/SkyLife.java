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

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

import java.util.ArrayList;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * Everything in the sky that moves: birds by day and bats by night, fireflies on
 * warm evenings, leaves and petals in their seasons, rain leaning with the wind,
 * snow, lightning, mist over the ground, smoke from the town's chimneys, the
 * glitter on the water and the shooting stars.
 *
 * Every sprite keeps a float position for its motion and is drawn at the rounded
 * one, so nothing ever sits between two pixels. Slanted things, rain and the
 * meteors, have their slant painted into the texture rather than rotated.
 */
public final class SkyLife {

	private final SkyContext c;
	private final Group scene;
	private final ArrayList<Mote> motes = new ArrayList<>();
	private final String key;

	private interface Mote {
		void update( float el );
	}

	public SkyLife( Group scene, SkyContext c ){
		this.scene = scene;
		this.c = c;
		this.key = "skylife-" + c.W + "x" + c.H + "-";
	}

	public void update( float el ){
		for (Mote m : motes) m.update( el );
	}

	// -------------------------------------------------------------- textures

	private SmartTexture tex( String name, int w, int h ){
		SmartTexture t = TextureCache.create( key + name, w, h );
		t.bitmap.setBlending( Pixmap.Blending.None );
		t.bitmap.setColor( 0 );
		t.bitmap.fill();
		t.filter( Texture.NEAREST, Texture.NEAREST );
		return t;
	}

	private static Image snap( Image i, float x, float y ){
		i.x = (float)Math.floor( x );
		i.y = (float)Math.floor( y );
		return i;
	}

	// ---------------------------------------------------------------- build

	public void build( boolean stars ){
		boolean raining = c.precip == PrecipType.RAIN || c.precip == PrecipType.SLEET || c.precip == PrecipType.HAIL;
		boolean snowing = c.precip == PrecipType.SNOW || c.precip == PrecipType.BLIZZARD;
		boolean dark = c.light < 0.35f;

		if (stars) twinkles();
		if (dark && c.cloudCover < 0.5f && !c.fog) meteors();
		if (!c.storm && c.light > 0.3f && c.precipRate < 0.3f) birds();
		if (dark && !raining && !c.storm && !c.fog && c.temp > 4f) bats();
		if ((dark || c.f > 0.6f) && !raining && !snowing && c.temp >= 12f && fireflyLand()) fireflies();
		if (c.autumn() && leafyLand() && !snowing && c.precipRate < 0.5f) leaves( false );
		if ((c.spring() && leafyLand() && c.biome != Biome.PLAINS) || c.ambient == ClimateManager.WeatherOverlayAmbient.SPRING_PETALS) leaves( true );
		if (raining && c.precipRate > 0.05f) rain();
		if (snowing && c.precipRate > 0.05f) snow();
		if (c.storm) lightning();
		if (c.fog || c.ambient == ClimateManager.WeatherOverlayAmbient.MIST || (c.humidity > 0.85f && c.light < 0.5f && !raining && !snowing)) mist();
		if (c.town) smoke();
		if (c.water()) glitter();
	}

	private boolean fireflyLand(){
		return c.ambient == ClimateManager.WeatherOverlayAmbient.FIREFLIES
				|| (!c.town && (c.biome == Biome.FOREST || c.biome == Biome.MEADOW || c.biome == Biome.SWAMP
				|| c.biome == Biome.RIVER || c.biome == Biome.PLAINS) && (c.summer() || c.spring()));
	}

	private boolean leafyLand(){
		return !c.town && (c.biome == Biome.FOREST || c.biome == Biome.MEADOW || c.biome == Biome.RIVER
				|| c.biome == Biome.PLAINS || c.biome == Biome.FOOTHILLS);
	}

	// ------------------------------------------------------------- twinkles

	private void twinkles(){
		SmartTexture t = tex( "twinkle", 1, 1 );
		px( t.bitmap, 0, 0, 0xFFFFFF );
		t.bitmap( t.bitmap );
		Random.pushGenerator( c.seed ^ 0x57A25L );
		int n = Math.max( 8, c.W / 14 );
		for (int i = 0; i < n; i++){
			final Image s = new Image( t );
			snap( s, Random.Int( c.W ), Random.Int( (int)(c.skyHorizon * 0.8f) ) );
			final float phase = Random.Float( 6.28f ), rate = 1.2f + Random.Float( 1.6f );
			scene.add( s );
			motes.add( new Mote(){
				float tt = phase;
				public void update( float el ){
					tt += el * rate;
					s.am = 0.25f + 0.75f * (float)Math.abs( Math.sin( tt ) ) * c.nightness;
				}
			} );
		}
		Random.popGenerator();
	}

	// -------------------------------------------------------------- meteors

	private void meteors(){
		//two slants, one for each direction of travel
		final SmartTexture[] streak = new SmartTexture[2];
		for (int d = 0; d < 2; d++){
			SmartTexture t = tex( "meteor" + d, 14, 6 );
			for (int i = 0; i < 14; i++){
				int x = d == 0 ? i : 13 - i;
				int y = i * 5 / 13;
				int a = 40 + (215 * i / 13);
				blend( t.bitmap, x, y, 0xFFFFFF, a );
				if (i > 9) blend( t.bitmap, x, y + 1, 0xE8F0FF, a / 3 );
			}
			t.bitmap( t.bitmap );
			streak[d] = t;
		}
		for (int k = 0; k < 2; k++){
			final Image s = new Image( streak[0] );
			final int slot = k;
			s.visible = false;
			scene.add( s );
			motes.add( new Mote(){
				float wait = 3f + Random.Float( 9f ) + slot * 5f, life = -1f, px, py, vx, vy;
				public void update( float el ){
					if (life < 0){
						wait -= el;
						if (wait > 0) return;
						int dir = Random.Int( 2 );
						s.texture( streak[dir] );
						px = Random.Float( c.W * 0.1f, c.W * 0.9f );
						py = Random.Float( c.H * 0.04f, c.skyHorizon * 0.5f );
						float sp = 200f + Random.Float( 120f );
						vx = (dir == 0 ? 1 : -1) * sp * 0.93f;
						vy = sp * 0.36f;
						life = 0.5f + Random.Float( 0.25f );
						s.visible = true;
						wait = 5f + Random.Float( 11f );
						return;
					}
					life -= el;
					px += vx * el; py += vy * el;
					snap( s, px, py );
					s.am = Math.min( 1f, life * 3f );
					if (life <= 0){ life = -1f; s.visible = false; }
				}
			} );
		}
	}

	// ---------------------------------------------------------------- birds

	private void birds(){
		boolean gulls = c.biome == Biome.BEACH || c.biome == Biome.OCEAN;
		int col = gulls ? mix( 0xF4F6F8, c.horizonAvg, 0.15f ) : mix( 0x1a1c24, c.horizonAvg, 0.25f );
		//two frames: wings down and wings up
		final SmartTexture down = tex( "bird0", 7, 3 ), up = tex( "bird1", 7, 3 );
		px( down.bitmap, 0, 0, col ); px( down.bitmap, 6, 0, col ); px( down.bitmap, 1, 1, col ); px( down.bitmap, 5, 1, col );
		px( down.bitmap, 2, 2, col ); px( down.bitmap, 3, 2, col ); px( down.bitmap, 4, 2, col );
		px( up.bitmap, 0, 2, col ); px( up.bitmap, 6, 2, col ); px( up.bitmap, 1, 1, col ); px( up.bitmap, 5, 1, col );
		px( up.bitmap, 2, 0, col ); px( up.bitmap, 3, 0, col ); px( up.bitmap, 4, 0, col );
		down.bitmap( down.bitmap ); up.bitmap( up.bitmap );

		int flocks = c.autumn() ? 2 : 1;
		for (int fl = 0; fl < flocks; fl++){
			final int n = 4 + Random.Int( c.autumn() ? 6 : 4 );
			final float dir = Random.Int( 2 ) == 0 ? 1 : -1;
			final float speed = 11f + Random.Float( 8f );
			final float baseY = c.H * (0.12f + Random.Float( 0.28f ));
			final Image[] bs = new Image[n];
			final float[] ox = new float[n], oy = new float[n], flap = new float[n];
			for (int i = 0; i < n; i++){
				bs[i] = new Image( down );
				//a loose V behind the leader
				ox[i] = -i * 6f * dir + (Random.Float() - 0.5f) * 3f;
				oy[i] = Math.abs( i % 2 == 0 ? i : -i ) * 2.2f + (Random.Float() - 0.5f) * 2f;
				flap[i] = Random.Float( 1f );
				scene.add( bs[i] );
			}
			motes.add( new Mote(){
				float lead = dir > 0 ? -n * 7f - 10 : c.W + n * 7f + 10, t = Random.Float( 6f );
				float rest = 0;
				public void update( float el ){
					if (rest > 0){ rest -= el; return; }
					t += el;
					lead += dir * speed * el;
					for (int i = 0; i < n; i++){
						flap[i] += el * 5.5f;
						bs[i].texture( ((int)flap[i] & 1) == 0 ? down : up );
						float bob = (float)Math.sin( t * 1.3f + i ) * 1.5f;
						snap( bs[i], lead + ox[i], baseY + oy[i] + bob );
					}
					if ((dir > 0 && lead - n * 7f > c.W + 8) || (dir < 0 && lead + n * 7f < -8)){
						lead = dir > 0 ? -n * 7f - 10 : c.W + n * 7f + 10;
						rest = 4f + Random.Float( 8f );
					}
				}
			} );
		}
	}

	private void bats(){
		int col = mix( 0x0c0e16, c.zenith, 0.2f );
		final SmartTexture a = tex( "bat0", 5, 3 ), b = tex( "bat1", 5, 2 );
		px( a.bitmap, 0, 0, col ); px( a.bitmap, 4, 0, col ); px( a.bitmap, 1, 1, col ); px( a.bitmap, 3, 1, col ); px( a.bitmap, 2, 2, col ); px( a.bitmap, 2, 1, col );
		px( b.bitmap, 0, 1, col ); px( b.bitmap, 1, 1, col ); px( b.bitmap, 2, 0, col ); px( b.bitmap, 3, 1, col ); px( b.bitmap, 4, 1, col ); px( b.bitmap, 2, 1, col );
		a.bitmap( a.bitmap ); b.bitmap( b.bitmap );
		int n = 2 + Random.Int( 4 );
		for (int i = 0; i < n; i++){
			final Image s = new Image( a );
			scene.add( s );
			motes.add( new Mote(){
				float px = Random.Float( c.W ), py = c.H * (0.3f + Random.Float( 0.3f ));
				float vx = 0, vy = 0, turn = 0, flap = Random.Float( 3f );
				public void update( float el ){
					turn -= el;
					if (turn <= 0){
						//bats do not fly straight for long
						vx = (Random.Float() - 0.5f) * 60f; vy = (Random.Float() - 0.5f) * 30f;
						turn = 0.25f + Random.Float( 0.5f );
					}
					px += vx * el; py += vy * el;
					if (px < -6) px = c.W + 4; if (px > c.W + 6) px = -4;
					float lo = c.H * 0.2f, hi = c.landTop + 6;
					if (py < lo) vy = Math.abs( vy ); if (py > hi) vy = -Math.abs( vy );
					flap += el * 9f;
					s.texture( ((int)flap & 1) == 0 ? a : b );
					snap( s, px, py );
				}
			} );
		}
	}

	// ------------------------------------------------------------ fireflies

	private void fireflies(){
		SmartTexture t = tex( "firefly", 3, 3 );
		int glow = 0xd8ff70;
		px( t.bitmap, 1, 1, 0xf4ffb0 );
		blend( t.bitmap, 0, 1, glow, 110 ); blend( t.bitmap, 2, 1, glow, 110 );
		blend( t.bitmap, 1, 0, glow, 110 ); blend( t.bitmap, 1, 2, glow, 110 );
		t.bitmap( t.bitmap );
		int n = 8 + Math.max( 4, c.W / 20 );
		for (int i = 0; i < n; i++){
			final Image s = new Image( t );
			scene.add( s );
			motes.add( new Mote(){
				float px = Random.Float( c.W ), py = c.groundTop - 4 - Random.Float( 26f );
				float ph = Random.Float( 6.28f ), sp = 0.4f + Random.Float( 0.8f ), t2 = Random.Float( 9f );
				public void update( float el ){
					t2 += el;
					px += (float)Math.sin( t2 * 0.7f + ph ) * 6f * el;
					py += (float)Math.cos( t2 * 0.9f + ph * 1.3f ) * 4f * el;
					if (px < -2) px = c.W + 1; if (px > c.W + 2) px = -1;
					snap( s, px, py );
					//a slow pulse, off for a while between glows
					float k = (float)Math.sin( t2 * sp * 2.2f + ph );
					s.am = k > 0.2f ? (k - 0.2f) / 0.8f : 0f;
				}
			} );
		}
	}

	// --------------------------------------------------------------- leaves

	private void leaves( boolean petals ){
		int[] cols = petals ? new int[]{ 0xf0a8c8, 0xf8c8d8, 0xe890b8 } : new int[]{ 0xc86a2a, 0xd8a030, 0xb03c2c, 0xe08838 };
		final SmartTexture[] ts = new SmartTexture[cols.length];
		for (int i = 0; i < cols.length; i++){
			ts[i] = tex( (petals ? "petal" : "leaf") + i, 2, 2 );
			int col = c.lit( cols[i], 0.8f );
			px( ts[i].bitmap, 0, 0, col ); px( ts[i].bitmap, 1, 0, mix( col, 0xFFFFFF, 0.2f ) ); px( ts[i].bitmap, 0, 1, mix( col, 0, 0.2f ) );
			ts[i].bitmap( ts[i].bitmap );
		}
		int n = petals ? 8 : 12;
		for (int i = 0; i < n; i++){
			final Image s = new Image( ts[Random.Int( ts.length )] );
			scene.add( s );
			motes.add( new Mote(){
				float px = Random.Float( c.W ), py = Random.Float( c.H ), ph = Random.Float( 6.28f );
				float fall = (petals ? 5f : 8f) + Random.Float( 6f ), t2 = 0;
				public void update( float el ){
					t2 += el;
					py += fall * el;
					px += ((float)Math.sin( t2 * 2.4f + ph ) * 9f + c.windDir * c.wind * 2.5f) * el;
					if (py > c.H){ py = -3; px = Random.Float( c.W ); }
					if (px < -2) px = c.W + 1; if (px > c.W + 2) px = -1;
					snap( s, px, py );
				}
			} );
		}
	}

	// --------------------------------------------------------------- weather

	private void rain(){
		//the streak leans with the wind, and the lean is drawn, not rotated
		float lean = c.windDir * Math.min( 3f, c.wind * 0.35f );
		int len = 4;
		int w = 1 + (int)Math.ceil( Math.abs( lean ) );
		SmartTexture t = tex( "rain", w, len );
		int col = mix( 0x9ab0d0, c.horizonAvg, 0.4f );
		for (int y = 0; y < len; y++){
			int x = lean >= 0 ? Math.round( lean * (len - 1 - y) / (len - 1f) ) : Math.round( -lean * y / (len - 1f) );
			blend( t.bitmap, x, y, col, 170 );
		}
		t.bitmap( t.bitmap );
		int n = 30 + (int)(c.precipRate * 70) + c.W / 8;
		final float windX = c.windDir * c.wind * 9f;
		for (int i = 0; i < n; i++){
			final Image s = new Image( t );
			scene.add( s );
			motes.add( new Mote(){
				float px = Random.Float( c.W ), py = Random.Float( c.H ), sp = 95f + Random.Float( 50f );
				public void update( float el ){
					py += sp * el; px += windX * el;
					if (py > c.H){ py = -len; px = Random.Float( -20, c.W + 20 ); }
					snap( s, px, py );
				}
			} );
		}
	}

	private void snow(){
		final SmartTexture small = tex( "snow1", 1, 1 ), big = tex( "snow2", 2, 2 );
		px( small.bitmap, 0, 0, 0xEEF3FA ); small.bitmap( small.bitmap );
		px( big.bitmap, 0, 0, 0xF6F8FC ); px( big.bitmap, 1, 0, 0xE0E8F2 ); px( big.bitmap, 0, 1, 0xE0E8F2 ); px( big.bitmap, 1, 1, 0xC8D4E4 ); big.bitmap( big.bitmap );
		boolean blizzard = c.precip == PrecipType.BLIZZARD;
		int n = 35 + (int)(c.precipRate * 60) + c.W / 6;
		final float windX = c.windDir * c.wind * (blizzard ? 14f : 3f);
		for (int i = 0; i < n; i++){
			final boolean near = Random.Int( 3 ) == 0;
			final Image s = new Image( near ? big : small );
			scene.add( s );
			motes.add( new Mote(){
				float px = Random.Float( c.W ), py = Random.Float( c.H ), ph = Random.Float( 6.28f ), t2 = 0;
				float sp = (near ? 16f : 10f) + Random.Float( 8f ) + (blizzard ? 20f : 0);
				public void update( float el ){
					t2 += el;
					py += sp * el;
					px += ((float)Math.sin( t2 * 1.5f + ph ) * 5f + windX) * el;
					if (py > c.H){ py = -2; px = Random.Float( -20, c.W + 20 ); }
					if (px < -3) px = c.W + 2; if (px > c.W + 3) px = -2;
					snap( s, px, py );
				}
			} );
		}
	}

	private void lightning(){
		final ColorBlock flash = new ColorBlock( c.W, c.H, 0xFFFFFFFF );
		flash.am = 0;
		scene.add( flash );
		motes.add( new Mote(){
			float wait = 3f + Random.Float( 8f ), a = 0, second = -1;
			public void update( float el ){
				if (a > 0){
					a = Math.max( 0, a - el * 4.5f );
				} else if (second > 0){
					second -= el;
					if (second <= 0){ a = 0.45f; second = -1; }
				} else {
					wait -= el;
					if (wait <= 0){ a = 0.8f; second = 0.15f; wait = 6f + Random.Float( 14f ); }
				}
				flash.am = a * a;
			}
		} );
	}

	private void mist(){
		int bw = nextPow2( Math.max( 64, c.W ) );
		final int bands = 3;
		for (int i = 0; i < bands; i++){
			SmartTexture t = tex( "mist" + i, bw, 14 );
			int col = mix( 0xe6eaf0, c.horizonAvg, 0.35f );
			long s = c.seed ^ (0x0157L * (i + 1));
			for (int x = 0; x < bw; x++){
				float body = (float)Math.pow( Math.sin( x / (float)bw * Math.PI ), 0.7 );
				body *= 0.55f + 0.45f * smooth1( s, x, 23f );
				for (int y = 0; y < 14; y++){
					float bell = (float)Math.exp( -Math.pow( (y - 7) / 4.2f, 2 ) );
					int a = (int)(150 * body * bell);
					if (a > 0) blend( t.bitmap, x, y, col, a );
				}
			}
			t.bitmap( t.bitmap );
			final Image band = new Image( t );
			final float y0 = c.groundTop - 16 + i * 6;
			final float drift = (i % 2 == 0 ? 1 : -1) * (2f + i) * (c.wind > 2 ? 1.5f : 1f);
			final float alpha = (c.fog ? 0.45f : 0.28f) - i * 0.05f;
			final int bi = i;
			band.am = alpha;
			scene.add( band );
			motes.add( new Mote(){
				float px = -bw / 2f + bi * 30, t2 = Random.Float( 9f );
				public void update( float el ){
					t2 += el;
					px += drift * el;
					if (px > 0) px -= bw / 2f; if (px < -bw) px += bw / 2f;
					snap( band, px, y0 + (float)Math.sin( t2 * 0.4f ) * 1.5f );
				}
			} );
		}
	}

	private void smoke(){
		SmartTexture t = tex( "smoke", 3, 3 );
		int col = mix( 0xc8c8d0, c.horizonAvg, 0.5f );
		px( t.bitmap, 1, 1, col );
		blend( t.bitmap, 0, 1, col, 120 ); blend( t.bitmap, 2, 1, col, 120 ); blend( t.bitmap, 1, 0, col, 120 ); blend( t.bitmap, 1, 2, col, 120 );
		t.bitmap( t.bitmap );
		SmartTexture halo = tex( "lampglow", 9, 9 );
		SkyDome.paintGlow( halo.bitmap, 9, 120 );
		halo.bitmap( halo.bitmap );
		for (int[] ch : c.chimneys){
			if (ch[2] == 2) continue;   //a villager's lantern: painted, not animated
			if (ch[2] == 1){
				//a lamp: a warm glow that breathes
				final Image g = new Image( halo );
				g.hardlight( 0xffc860 );
				snap( g, ch[0] - 4, ch[1] - 4 );
				scene.add( g );
				motes.add( new Mote(){
					float t2 = Random.Float( 6f );
					public void update( float el ){ t2 += el; g.am = 0.5f + 0.15f * (float)Math.sin( t2 * 3f ) + 0.08f * (float)Math.sin( t2 * 11f ); }
				} );
				continue;
			}
			final int cx = ch[0], cy = ch[1];
			for (int i = 0; i < 4; i++){
				final Image s = new Image( t );
				scene.add( s );
				final float phase = i / 4f;
				final int pi = i;
				motes.add( new Mote(){
					float life = phase;
					public void update( float el ){
						life += el * 0.28f;
						if (life >= 1f) life -= 1f;
						float rise = life * 18f;
						float x = cx + (float)Math.sin( life * 7f + pi ) * 1.5f + c.windDir * life * life * 5f * Math.min( 2f, c.wind * 0.5f );
						snap( s, x, cy - rise );
						s.am = 0.55f * (1f - life) * (0.4f + 0.6f * c.light + 0.3f * c.nightness);
					}
				} );
			}
		}
	}

	private void glitter(){
		boolean sunPath = c.sunUp && c.light > 0.08f;
		if (!sunPath && !(c.moonUp && c.moonBright > 0.2f)) return;
		SmartTexture t = tex( "glint", 1, 1 );
		px( t.bitmap, 0, 0, sunPath ? mix( 0xFFFFFF, c.sunColor, 0.3f ) : mix( 0xFFFFFF, c.moonColor, 0.4f ) );
		t.bitmap( t.bitmap );
		float pathX = sunPath ? c.sunX : c.moonX;
		int n = Math.max( 10, c.W / 12 );
		for (int i = 0; i < n; i++){
			final Image s = new Image( t );
			scene.add( s );
			final float ph = Random.Float( 6.28f ), rate = 3f + Random.Float( 4f );
			float span = Math.max( 1, c.waterBottom - c.waterTop );
			float ty = Random.Float();
			float halfW = c.W * (0.045f + 0.06f * (sunPath ? c.warmth : 0.3f)) * (0.7f + 0.9f * ty);
			snap( s, pathX + (Random.Float() - 0.5f) * 2 * halfW, c.waterTop + ty * span );
			motes.add( new Mote(){
				float t2 = ph;
				public void update( float el ){
					t2 += el * rate;
					float k = (float)Math.sin( t2 );
					s.am = k > 0.55f ? (k - 0.55f) / 0.45f : 0f;
				}
			} );
		}
	}
}
