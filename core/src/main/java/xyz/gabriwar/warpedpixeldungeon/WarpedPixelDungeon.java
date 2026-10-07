/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon;

import xyz.gabriwar.warpedpixeldungeon.debug.ScreenshotTour;
import xyz.gabriwar.warpedpixeldungeon.debug.TrailerTour;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.WelcomeScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PlatformSupport;

public class WarpedPixelDungeon extends Game {

	//rankings from v1.2.3 and older use a different score formula, so this reference is kept
	public static final int v1_2_3 = 628;

	//savegames from versions older than v3.1.1 are no longer supported, and data from them is ignored
	public static final int v3_1_1 = 850;
	public static final int v3_2_1 = 861; //last version for Android 4.4- and Java 8
	public static final int v3_2_5 = 877;
	public static final int v3_3_0 = 883;

	//starting here we are doing 2 version codes per public update, so use code-1 to get both
	public static final int v4_0_0 = 911;
	
	public WarpedPixelDungeon( PlatformSupport platform ) {
		super( sceneClass == null ? WelcomeScene.class : sceneClass, platform );

		// Merge the old second disintegration wand, including imbued staves.
		com.watabou.utils.Bundle.addAlias(
				xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfDisintegration.class,
				"xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfDisintegration2" );

		//pre-v3.3.0
		com.watabou.utils.Bundle.addAlias(
				xyz.gabriwar.warpedpixeldungeon.items.keys.WornKey.class,
				"xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey" );

		//holy water was reworked from a melee weapon into a thrown vial (v4.x)
		com.watabou.utils.Bundle.addAlias(
				xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HolyWater.class,
				"xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HolyWater" );

	}
	
	@Override
	public void create() {
		super.create();

		updateSystemUI();
		WPDAction.loadBindings();
		
		Music.INSTANCE.enable( WPDSettings.music() );
		Music.INSTANCE.volume( WPDSettings.musicVol()*WPDSettings.musicVol()/100f );
		Sample.INSTANCE.enable( WPDSettings.soundFx() );
		Sample.INSTANCE.volume( WPDSettings.SFXVol()*WPDSettings.SFXVol()/100f );

		Sample.INSTANCE.load( Assets.Sounds.all );
		
	}

	@Override
	public void finish() {
		if (!DeviceCompat.isiOS()) {
			super.finish();
		} else {
			//can't exit on iOS (Apple guidelines), so just go to title screen
			switchScene(TitleScene.class);
		}
	}

	public static void switchNoFade(Class<? extends PixelScene> c){
		switchNoFade(c, null);
	}

	public static void switchNoFade(Class<? extends PixelScene> c, SceneChangeCallback callback) {
		PixelScene.noFade = true;
		switchScene( c, callback );
	}
	
	public static void seamlessResetScene(SceneChangeCallback callback) {
		if (scene() instanceof PixelScene){
			((PixelScene) scene()).saveWindows();
			switchNoFade((Class<? extends PixelScene>) sceneClass, callback );
		} else {
			resetScene();
		}
	}
	
	public static void seamlessResetScene(){
		seamlessResetScene(null);
	}
	
	@Override
	protected void switchScene() {
		super.switchScene();
		if (scene instanceof PixelScene){
			((PixelScene) scene).restoreWindows();
		}
	}
	
	@Override
	protected void update() {
		if (TrailerTour.instance != null) {
			TrailerTour.instance.frame(); //fixed-step stand-in for the whole update
			return;
		}
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		if (t == 0L && xyz.gabriwar.warpedpixeldungeon.debug.Profiler.running) t = System.nanoTime();
		super.update();
		if (t != 0L) xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.framePhase( true, (System.nanoTime() - t) / 1_000_000f );
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.frame();
		xyz.gabriwar.warpedpixeldungeon.debug.Profiler.frame();
		if (ScreenshotTour.instance != null) {
			ScreenshotTour.instance.update();
		}
	}

	@Override
	protected void draw() {
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		if (t == 0L && xyz.gabriwar.warpedpixeldungeon.debug.Profiler.running) t = System.nanoTime();
		super.draw();
		if (t != 0L) xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.framePhase( false, (System.nanoTime() - t) / 1_000_000f );
	}

	@Override
	public void resize( int width, int height ) {
		if (width == 0 || height == 0){
			return;
		}

		//the tour renders offscreen at its own size, whatever the window is
		if (ScreenshotTour.instance != null) {
			width = ScreenshotTour.instance.width;
			height = ScreenshotTour.instance.height;
		} else if (TrailerTour.instance != null) {
			width = TrailerTour.instance.width;
			height = TrailerTour.instance.height;
		}

		if (scene instanceof PixelScene &&
				(height != Game.height || width != Game.width)) {
			PixelScene.noFade = true;
			((PixelScene) scene).saveWindows();
		}

		super.resize( width, height );

		updateDisplaySize();

	}
	
	@Override
	public void destroy(){
		if (TrailerTour.instance != null) {
			TrailerTour.instance.finish();
		}
		super.destroy();
		GameScene.endActorThread();
		//closing the window is leaving the game: the other players get the goodbye
		//and their hero copies instead of a dead socket
		xyz.gabriwar.warpedpixeldungeon.net.NetManager.stop();
	}
	
	public void updateDisplaySize(){
		platform.updateDisplaySize();
	}

	public static void updateSystemUI() {
		platform.updateSystemUI();
	}
}