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

package xyz.gabriwar.warpedpixeldungeon.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewConfiguration;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AsynchronousAndroidAudio;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.utils.GdxNativesLoader;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.services.news.News;
import xyz.gabriwar.warpedpixeldungeon.services.news.NewsImpl;
import xyz.gabriwar.warpedpixeldungeon.services.updates.UpdateImpl;
import xyz.gabriwar.warpedpixeldungeon.services.updates.Updates;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import xyz.gabriwar.warpedpixeldungeon.services.payments.Payments;

public class AndroidLauncher extends AndroidApplication {
	
	public static AndroidApplication instance;
	
	private static AndroidPlatformSupport support;
	
	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Install crash reporter — on uncaught exception, show CrashReportActivity
		final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			try {
				StringWriter sw = new StringWriter();
				throwable.printStackTrace(new PrintWriter(sw));
				String stackTrace = "Thread: " + thread.getName() + "\n\n" + sw.toString();

				Intent intent = new Intent(AndroidLauncher.this, CrashReportActivity.class);
				intent.putExtra(CrashReportActivity.EXTRA_CRASH_LOG, stackTrace);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
			} catch (Throwable ignored) {
				// our crash handler crashed; the default one below still runs
			}
			// always hand over to Android's own handler: it is what reports the crash
			// to Play Console vitals and tears the process down. Without it the game
			// keeps living behind the report screen and Play never hears of the crash.
			if (defaultHandler != null) {
				defaultHandler.uncaughtException(thread, throwable);
			} else {
				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(10);
			}
		});

		try {
			GdxNativesLoader.load();
			FreeType.initFreeType();
		} catch (Exception e){
			GdxNativesLoader.disableNativesLoading = true;
			AndroidMissingNativesHandler.error = e;
			Intent intent = new Intent(this, AndroidMissingNativesHandler.class);
			startActivity(intent);
			finish();
			//let initialization continue for a moment so that we can set up things libGDX expects to be set up
		}

		//there are some things we only need to set up on first launch
		if (instance == null) {

			instance = this;

			try {
				Game.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (PackageManager.NameNotFoundException e) {
				Game.version = "???";
			}
			try {
				Game.versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			} catch (PackageManager.NameNotFoundException e) {
				Game.versionCode = 0;
			}

			Gdx.app = this;
			if (UpdateImpl.supportsUpdates()) {
				Updates.service = UpdateImpl.getUpdateService();
			}
			if (NewsImpl.supportsNews()) {
				News.service = NewsImpl.getNewsService();
			}
			//flavor-specific: playstore installs Google Play Billing, github is a no-op
			PaymentsSetup.setup(this);
			//flavor-specific: playstore initializes Play Games Services (which signs
			//the player in on its own), github is a no-op
			PlayGamesSetup.setup(this);

			FileUtils.setDefaultFileProperties(Files.FileType.Local, "");

			// grab preferences directly using our instance first
			// so that we don't need to rely on Gdx.app, which isn't initialized yet.
			// Note that we use a different prefs name on android for legacy purposes,
			// this is the default prefs filename given to an android app (.xml is automatically added to it)
			WPDSettings.set(instance.getPreferences("WarpedPixelDungeon"));

		} else {
			instance = this;
		}

		//Shattered still overrides the back gesture behaviour, but we need to do it in a new way
		// (API added in Android 13, functionality enforced in Android 16)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, new OnBackInvokedCallback() {
				@Override
				public void onBackInvoked() {
					KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.BACK, true));
					KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.BACK, false));
				}
			});
		}

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.depth = 0;

		//we manage this ourselves
		config.useImmersiveMode = false;
		
		config.useCompass = false;
		config.useAccelerometer = false;
		
		if (support == null) support = new AndroidPlatformSupport();
		else                 support.reloadGenerators();
		
		support.updateSystemUI();

		Button.longClick = ViewConfiguration.getLongPressTimeout()/1000f;
		
		initialize(new WarpedPixelDungeon(support), config);
		
	}

	@Override
	public AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config) {
		return new AsynchronousAndroidAudio(context, config);
	}

	@Override
	protected void onResume() {
		//prevents weird rare cases where the app is running twice
		if (instance != this){
			finishAndRemoveTask();
		}
		super.onResume();
		//a subscription bought, renewed or cancelled in the Play Store app while the
		//game was in the background only shows up if we ask again on resume
		if (Payments.service != null) {
			Payments.service.refresh();
		}
	}

	@Override
	public void onBackPressed() {
		//do nothing, game should catch all back presses
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		support.updateSystemUI();
	}
	
	@Override
	public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
		super.onMultiWindowModeChanged(isInMultiWindowMode);
		support.updateSystemUI();
	}
}