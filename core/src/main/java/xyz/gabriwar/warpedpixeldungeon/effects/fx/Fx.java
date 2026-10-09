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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.effects.HearthLight;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;

import java.util.HashMap;

/**
 * The effects kit's common ground: which thread draws, handing work to it, letting a picture go,
 * the camera's shake as the player set it, where the hero sees, how strong a light is by day and
 * by night, the wind a particle drifts in, and the light layer's carriers for particles of the
 * existing factories (lightCarrier).
 */
public final class Fx {

	private Fx(){}

	// ------------------------------------------------------------------ threads

	//the thread the game's scene was made on: the render thread
	private static volatile Thread renderThread;

	/** The game's scene was made (GameScene.create): its thread draws, and nothing of the last
	 *  scene's carriers is left. */
	public static void sceneCreated(){
		renderThread = Thread.currentThread();
		carriers.clear();
		air = glowAir = null;
		frame();
	}

	/** Whether this is the render thread (true off any game scene: a test, a tool). */
	public static boolean onRenderThread(){
		Thread t = renderThread;
		return t == null || t == Thread.currentThread();
	}

	/** Runs it now on the render thread, else hands it there. */
	public static void post( Runnable r ){
		if (onRenderThread()){
			r.run();
		} else {
			Game.runOnRenderThread( r::run );
		}
	}

	/** Off its group and its GPU buffers freed, on the render thread (an erased picture keeps them
	 *  till the scene changes, and the surface is one long scene). */
	public static void dispose( final Gizmo g ){
		if (g == null) return;
		post( () -> {
			g.killAndErase();
			g.destroy();
		} );
	}

	// ------------------------------------------------------------------ the camera

	/** The most a shake may move the camera: a blizzard's vignette overhangs the screen by 2 px. */
	public static final float SHAKE_MAX = 4f, SHAKE_BLIZZARD = 2f;

	/**
	 * Shakes the camera as the player set it (PixelScene.shake(m / 2, d): the default setting looks
	 * as a raw shake of m always did, 0 shakes nothing), never more than SHAKE_MAX px, or
	 * SHAKE_BLIZZARD while a blizzard falls. Returns how far it shook, px.
	 */
	public static float shake( float magnitude, float duration ){
		float px = shakePx( magnitude, WPDSettings.screenShake(), blizzardFalling() );
		if (px > 0 && Camera.main != null) Camera.main.shake( px, duration );
		return px;
	}

	/** How far a shake of `magnitude` moves the camera at a setting, px. */
	public static float shakePx( float magnitude, int setting, boolean blizzard ){
		float px = magnitude * 0.5f * setting;
		return Math.max( 0f, Math.min( px, blizzard ? SHAKE_BLIZZARD : SHAKE_MAX ) );
	}

	private static boolean blizzardFalling(){
		return ClimateManager.localPrecipType() == PrecipType.BLIZZARD && ClimateManager.localPrecipRate() > 0f;
	}

	/** Whether a world point lies on the main camera's view, give or take `margin` px. */
	public static boolean onScreen( float x, float y, float margin ){
		Camera c = Camera.main;
		if (c == null) return true;
		return x >= c.scroll.x - margin && x <= c.scroll.x + c.width + margin
				&& y >= c.scroll.y - margin && y <= c.scroll.y + c.height + margin;
	}

	// ------------------------------------------------------------------ sight

	/** Whether the hero sees the world point (its cell in his field of view); true off a level. */
	public static boolean seen( float x, float y ){
		return WeatherSprites.visible( x, y );
	}

	/** How plainly the hero sees open ground: 1 in view, 0.8 explored, 0 never seen. */
	public static float groundSeen( int cell ){
		return HearthLight.groundSeen( cell );
	}

	/** The cell under a world point, -1 off the level. */
	public static int cellAt( float x, float y ){
		Level level = Dungeon.level;
		if (level == null || x < 0 || y < 0) return -1;
		int cx = (int)(x / DungeonTilemap.SIZE), cy = (int)(y / DungeonTilemap.SIZE);
		if (cx >= level.width() || cy >= level.height()) return -1;
		return cx + cy * level.width();
	}

	// ------------------------------------------------------------------ light

	/**
	 * How strong a light shows for the time of day: dayShare of it by day, all of it by night, as
	 * the star craters' glow (WorldEventDecor): dayShare + (1 - dayShare) x min(1, 0.3 + 1.4 x the
	 * night's tint). Persistent lights (fires, torches, auras) keep 0.35 by day, transient ones
	 * (impacts, strikes) 0.75.
	 */
	public static float nightMul( float dayShare ){
		return nightMul( dayShare, GameScene.nightTintAlpha() );
	}

	public static float nightMul( float dayShare, float nightTint ){
		return dayShare + (1f - dayShare) * Math.min( 1f, 0.3f + 1.4f * nightTint );
	}

	public static final float DAY_PERSISTENT = 0.35f, DAY_TRANSIENT = 0.75f;

	// ------------------------------------------------------------------ the frame's weather

	private static boolean openSky;
	private static float windX;

	/** Once a frame (GameScene.update): the wind and the sky looked up once for every particle. */
	public static void frame(){
		Level level = Dungeon.level;
		openSky = level != null && StormStrikes.underOpenSky( level );
		if (openSky){
			float dir = (float)Math.toRadians( ClimateManager.surfaceWindDir() );
			windX = (float)Math.sin( dir ) * ClimateManager.localWindSpeed() * 1.2f * (1f + 0.35f * WeatherSprites.gust());
		} else {
			windX = 0;
		}
	}

	/** Whether the sky is open over the level (the overworld's surface and its peaks). */
	public static boolean openSky(){
		return openSky;
	}

	/** The wind across the screen under an open sky, px/s (east positive); 0 under a roof. */
	public static float windX(){
		return windX;
	}

	// ------------------------------------------------------------------ holders

	private static FxEmitter air, glowAir;

	/**
	 * The scene's holder of loose matter among the effects: particles a fate or a recipe throws
	 * that belong to no emitter of their own (an ember's last speck, a droplet). It never emits
	 * nor dies. Null off the game's scene. Render thread.
	 */
	public static FxEmitter air(){
		if (air == null || !air.exists || air.parent == null) air = holder( GameScene.fxEmitter() );
		return air;
	}

	/** The same for loose light, in the light layer: drawn added, over the night's tint. */
	public static FxEmitter glowAir(){
		if (glowAir == null || !glowAir.exists || glowAir.parent == null){
			glowAir = holder( GameScene.lightEmitter() );
			if (glowAir != null) glowAir.holdLight();
		}
		return glowAir;
	}

	private static FxEmitter holder( FxEmitter e ){
		if (e == null) return null;
		e.on = false;
		e.autoKill = false;
		return e;
	}

	// ------------------------------------------------------------------ carriers

	//one per particle class, in the light layer
	private static final HashMap<Class<? extends Gizmo>, Emitter> carriers = new HashMap<>();

	/**
	 * A pooling emitter of the light layer for one particle class, made once a scene: an existing
	 * factory recycles its particle from here instead of from its caller's emitter, and that
	 * particle is drawn as light over the night's tint. Nothing else changes - the factory is the
	 * same object (its NetVisuals id too) and its caller's burst still records. Render thread; no
	 * allocation after the first call for a class. Null off the game's scene.
	 */
	public static Emitter lightCarrier( Class<? extends Gizmo> particleClass ){
		Emitter e = carriers.get( particleClass );
		if (e == null || !e.exists || e.parent == null){
			e = new Carrier();
			GameScene.light( e );
			if (e.parent == null) return null;
			carriers.put( particleClass, e );
		}
		return e;
	}

	//never emits nor dies: it only holds and draws the particles recycled from it, as light
	private static final class Carrier extends FxEmitter {
		Carrier(){
			lightMode = true;
			autoKill = false;
			on = false;
		}

		@Override
		public void revive(){
			super.revive();
			lightMode = true;
			autoKill = false;
		}
	}
}
