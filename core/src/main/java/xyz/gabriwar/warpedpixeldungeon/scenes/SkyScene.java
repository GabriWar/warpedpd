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

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyContext;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyDome;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyHorizon;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyLife;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.utils.Random;

/**
 * Look at the sky: a live skyscape painted from where you stand and what time
 * it is.
 *
 *  - the sky is lit per pixel from the sun's real position: its glow, the warm
 *    wedge under it, the pink belt and blue shadow opposite it at twilight, and
 *    moonlight after dark
 *  - the land stands in aerial perspective: ridges shaped by the biome, snow
 *    where it is cold, water mirroring the sky with the sun's path across it,
 *    trees of the biome's own species lit from the sun's side, the town's roofs
 *    with their windows lit after dark
 *  - clouds are lit by the sun and shaded by the sky, and drift with the wind
 *  - birds, bats, fireflies, leaves, petals, rain, snow, lightning, mist, smoke
 *    and shooting stars come and go with the season and the weather
 *
 * Every texture is drawn at one texel per game pixel and placed on the pixel
 * grid, so the whole thing stays pixel perfect. Tap anywhere (or back) to return.
 */
public class SkyScene extends PixelScene {

	private SkyContext c;
	private SkyLife life;

	private Image[] clouds;
	private float[] cloudX, cloudSpeed;
	private Image aurora;
	private float auroraT = 0;

	private SmartTexture tex( String name, int w, int h ){
		SmartTexture t = TextureCache.create( "sky-" + name + "-" + w + "x" + h, w, h );
		t.bitmap.setBlending( Pixmap.Blending.None );
		t.bitmap.setColor( 0 );
		t.bitmap.fill();
		t.filter( Texture.NEAREST, Texture.NEAREST );
		return t;
	}

	private Image place( SmartTexture t, int w, int h, float x, float y ){
		t.bitmap( t.bitmap );
		Image i = new Image( t );
		i.frame( 0, 0, w, h );
		i.x = (float)Math.floor( x );
		i.y = (float)Math.floor( y );
		add( i );
		return i;
	}

	@Override
	public void create() {
		super.create();

		int W = (int)Math.ceil( Camera.main.width );
		int H = (int)Math.ceil( Camera.main.height );
		c = SkyContext.capture( W, H );

		// ---- 1. the sky, lit from the sun ----
		SmartTexture domeT = tex( "dome", SkyPaint.nextPow2( W ), SkyPaint.nextPow2( H ) );
		SkyDome.paint( c, domeT.bitmap );
		place( domeT, W, H, 0, 0 );

		// ---- 2. stars, fading in through the twilight ----
		float starAlpha = SkyPaint.clamp01( (0.45f - c.light) / 0.35f );
		boolean stars = starAlpha > 0.05f && c.cloudCover < 0.85f;
		if (stars){
			SmartTexture starT = tex( "stars", SkyPaint.nextPow2( W ), SkyPaint.nextPow2( c.skyHorizon ) );
			SkyDome.paintStars( c, starT.bitmap );
			Image s = place( starT, W, c.skyHorizon, 0, 0 );
			s.am = starAlpha * (1f - 0.8f * c.cloudCover);
		}

		// ---- 3. aurora, behind the sun and moon ----
		if (!c.sunUp && c.aurora){
			int ah = Math.round( H * 0.5f );
			SmartTexture at = tex( "aurora", SkyPaint.nextPow2( W ), SkyPaint.nextPow2( ah ) );
			SkyDome.paintAurora( at.bitmap, W, ah, c.seed );
			aurora = place( at, W, ah, 0, Math.round( H * 0.03f ) );
			aurora.am = 0.55f;
		}

		// ---- 4. the sun, its glow, the moon ----
		if (c.sunElev > -0.14f){
			//one fixed texture, the glow painted at whatever size the day calls for
			int gs = Math.min( 127, Math.round( 44 + 44 * c.warmth + 24 * c.humidity ) | 1 );
			SmartTexture glowT = tex( "glow", 128, 128 );
			SkyDome.paintGlow( glowT.bitmap, gs, 130 );
			Image glow = place( glowT, gs, gs, c.sunX - gs / 2f, c.sunY - gs / 2f );
			glow.hardlight( c.sunColor );
			glow.am = (c.solarEclipse ? 0.12f : 0.22f + 0.45f * c.warmth) * SkyPaint.clamp01( (c.sunElev + 0.14f) / 0.14f ) * (0.3f + 0.7f * c.sunDisc);
		}
		if (c.sunUp){
			if (c.solarEclipse){
				SmartTexture e = tex( "eclipse", 40, 40 );
				SkyDome.paintEclipse( e.bitmap );
				place( e, 40, 40, c.sunX - 20, c.sunY - 20 );
			} else {
				SmartTexture s = tex( "sun", 24, 24 );
				SkyDome.paintSun( c, s.bitmap );
				place( s, 24, 24, c.sunX - 12, c.sunY - 12 ).am = c.sunDisc;
			}
		}
		if (c.moonUp){
			if (c.lunarEclipse && !c.sunUp){
				SmartTexture h = tex( "moonhalo", 49, 49 );
				SkyDome.paintGlow( h.bitmap, 49, 110 );
				Image halo = place( h, 49, 49, c.moonX - 24, c.moonY - 24 );
				halo.hardlight( 0xC03020 );
				halo.am = 0.35f;
			} else if (c.humidity > 0.7f && !c.sunUp && c.moonBright > 0.5f){
				//a ring round the moon on a damp night
				SmartTexture h = tex( "moonring", 41, 41 );
				SkyDome.paintGlow( h.bitmap, 41, 60 );
				Image ring = place( h, 41, 41, c.moonX - 20, c.moonY - 20 );
				ring.hardlight( c.moonColor );
				ring.am = 0.35f;
			}
			SmartTexture m = tex( "moon", 24, 24 );
			SkyDome.paintMoon( c, m.bitmap );
			Image moon = place( m, 24, 24, c.moonX - 12, c.moonY - 12 );
			if (c.sunUp) moon.am = 0.5f;   //a pale day moon
		}

		// ---- 5. rainbow, always opposite the sun ----
		if (c.sunUp && c.rainbow){
			SmartTexture rb = tex( "rainbow", SkyPaint.nextPow2( W ), SkyPaint.nextPow2( c.skyHorizon + 2 ) );
			SkyDome.paintRainbow( rb.bitmap, W, c.skyHorizon + 2, Math.round( W - c.sunX ), c.skyHorizon, Math.round( W * 0.42f ) );
			Image bow = place( rb, W, c.skyHorizon + 2, 0, 0 );
			bow.am = 0.7f;
		}

		// ---- 6. clouds: two bands lit by the sun, drifting with the wind ----
		float cover = c.cloudCover;
		int nFar = 4 + (int)(cover * 12), nNear = 2 + (int)(cover * 9);
		int nCirrus = (cover < 0.4f && c.light > 0.3f) ? 1 + Random.Int( 3 ) : 0;
		clouds = new Image[nFar + nNear + nCirrus];
		cloudX = new float[clouds.length];
		cloudSpeed = new float[clouds.length];
		Random.pushGenerator( (c.seed ^ 0xC10CDL) + xyz.gabriwar.warpedpixeldungeon.Dungeon.cycleTurn / 500 );
		int k = 0;
		for (int i = 0; i < nFar + nNear; i++, k++){
			boolean far = i < nFar;
			int w = far ? 22 + Random.Int( 20 ) : 40 + Random.Int( 34 );
			//fixed canvases keyed by slot, so a revisit repaints rather than allocates
			int tw = far ? 64 : 128, th = far ? 32 : 64;
			SmartTexture ct = tex( "cloud" + i, tw, th );
			float x = Random.Float( -40, W );
			SkyDome.paintCloud( c, ct.bitmap, w, far, x + w / 2f < c.sunX, c.seed ^ (i * 7331L) );
			float y = far ? Random.Float( 3, H * 0.3f ) : Random.Float( H * 0.1f, H * 0.46f );
			Image cl = place( ct, w, w / 2 + 2, x, y );
			cl.am = c.storm ? 0.92f : c.light < 0.25f ? (far ? 0.5f : 0.62f) : (far ? 0.7f : 0.88f);
			cloudX[k] = x;
			cloudSpeed[k] = c.windDir * c.wind * (far ? 0.15f + Random.Float( 0.1f ) : 0.35f + Random.Float( 0.2f ));
			clouds[k] = cl;
		}
		for (int i = 0; i < nCirrus; i++, k++){
			int w = W / 2 + Random.Int( W / 3 );
			SmartTexture ct = tex( "cirrus" + i, SkyPaint.nextPow2( W ), 8 );
			SkyDome.paintCirrus( c, ct.bitmap, w, c.seed ^ (i * 991L) );
			float x = Random.Float( -w / 2f, W - w / 2f ), y = Random.Float( 2, H * 0.18f );
			Image cl = place( ct, w, 8, x, y );
			cl.am = 0.8f;
			cloudX[k] = x;
			cloudSpeed[k] = c.windDir * c.wind * 0.1f;
			clouds[k] = cl;
		}
		Random.popGenerator();

		// ---- 7. the land ----
		int landH = H - c.landTop;
		SmartTexture landT = tex( "land", SkyPaint.nextPow2( W ), SkyPaint.nextPow2( landH ) );
		SkyHorizon.paint( c, domeT.bitmap, landT.bitmap );
		place( landT, W, landH, 0, c.landTop );

		// ---- 8. everything that moves ----
		life = new SkyLife( this, c );
		life.build( stars );

		// ---- caption + exit ----
		DayNightCycle.Phase phase = DayNightCycle.phase();
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

		if (clouds != null){
			for (int i = 0; i < clouds.length; i++){
				cloudX[i] += cloudSpeed[i] * el;
				float w = clouds[i].width();
				if (cloudX[i] > c.W + 10) cloudX[i] = -w - 5;
				if (cloudX[i] < -w - 10) cloudX[i] = c.W + 5;
				clouds[i].x = (float)Math.floor( cloudX[i] );
			}
		}

		if (aurora != null){
			auroraT += el;
			aurora.am = 0.4f + 0.25f * (float)Math.sin( auroraT * 0.7 );
		}

		if (life != null) life.update( el );
	}

	@Override
	protected void onBackPressed() {
		Game.switchScene( GameScene.class );
	}
}
