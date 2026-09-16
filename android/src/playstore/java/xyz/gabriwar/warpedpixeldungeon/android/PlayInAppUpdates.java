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

package xyz.gabriwar.warpedpixeldungeon.android;

import android.app.Activity;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.watabou.noosa.Game;

import xyz.gabriwar.warpedpixeldungeon.services.updates.AvailableUpdateData;
import xyz.gabriwar.warpedpixeldungeon.services.updates.PlayUpdates;
import xyz.gabriwar.warpedpixeldungeon.services.updates.Updates;

/**
 * Google Play's in-app updates, flexible flow: Play says whether the account's track
 * has a newer build, downloads it in the background once the player agrees, and the
 * game restarts into it when they press install. Testers on the internal, closed or
 * open tracks are offered their track's build, since Play answers for the account.
 */
public class PlayInAppUpdates extends PlayUpdates {

	private final Activity activity;
	private final AppUpdateManager manager;

	private volatile boolean downloaded;

	public PlayInAppUpdates(Activity activity) {
		this.activity = activity;
		manager = AppUpdateManagerFactory.create(activity);
		manager.registerListener(listener);
	}

	private final InstallStateUpdatedListener listener = state -> {
		if (state.installStatus() == InstallStatus.DOWNLOADED) {
			downloaded = true;
			//the player asked for this download, so the title tells them it is ready
			Updates.prompted = false;
		}
	};

	@Override
	public boolean supportsUpdatePrompts() {
		return true;
	}

	@Override
	public void checkForUpdate(boolean useMetered, boolean includeBetas, UpdateResultCallback callback) {
		if (!useMetered && !Game.platform.connectedToUnmeteredNetwork()) {
			callback.onConnectionFailed();
			return;
		}
		manager.getAppUpdateInfo().addOnSuccessListener(info -> {
			if (info.installStatus() == InstallStatus.DOWNLOADED) {
				//a download from an earlier session is still waiting for its restart
				downloaded = true;
				callback.onNoUpdateFound();
			} else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
					&& info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
				AvailableUpdateData update = new AvailableUpdateData();
				update.versionCode = info.availableVersionCode();
				//Play only knows the version code; testers can match it in the console
				update.versionName = "build " + info.availableVersionCode();
				update.URL = LISTING_URL;
				callback.onUpdateAvailable(update);
			} else {
				callback.onNoUpdateFound();
			}
		}).addOnFailureListener(e -> {
			//Play not installed, no account, or a sideloaded copy: nothing to offer
			callback.onConnectionFailed();
		});
	}

	@Override
	public void initializeUpdate(AvailableUpdateData update) {
		//an AppUpdateInfo starts a flow once, so a player who dismissed Play's sheet
		//and tapped again gets a fresh one; the listener above tracks the download
		manager.getAppUpdateInfo().addOnSuccessListener(info -> {
			if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
					|| info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
				try {
					manager.startUpdateFlow(info, activity,
							AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build());
				} catch (Exception e) {
					Game.reportException(e);
				}
			}
		});
	}

	@Override
	public boolean isInstallable() {
		return downloaded;
	}

	@Override
	public void initializeInstall() {
		downloaded = false;
		activity.runOnUiThread(manager::completeUpdate);
	}
}
