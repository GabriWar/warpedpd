package xyz.gabriwar.warpedpixeldungeon.android;

import android.app.Activity;

import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

//playstore flavor: initializes Play Games Services. The v2 SDK signs the player in by
//itself right after initialize() (and retries on later launches); forcing a manual
//signIn() on top of that re-prompts players who declined on every launch, which
//Google's sign-in guidance tells us not to do.
public class PlayGamesSetup {
	public static void setup(Activity activity) {
		PlayGamesSdk.initialize(activity);
	}

	//every badge has an achievement of the same name on Play Games. Their ids live in
	//res/values/achievements.xml as achievement_<badge>, written by
	//tools/play_achievements.py; a badge with no id there is simply not reported.
	//The SDK drops the call when nobody is signed in, and that is fine
	public static void unlockAchievement(Activity activity, String badge) {
		int res = activity.getResources().getIdentifier(
				"achievement_" + badge.toLowerCase(), "string", activity.getPackageName());
		if (res == 0) return;
		PlayGames.getAchievementsClient(activity).unlock(activity.getString(res));
	}
}
