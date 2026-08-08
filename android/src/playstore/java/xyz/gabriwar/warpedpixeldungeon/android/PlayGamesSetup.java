package xyz.gabriwar.warpedpixeldungeon.android;

import android.app.Activity;

import com.google.android.gms.games.PlayGamesSdk;

//playstore flavor: initializes Play Games Services. The v2 SDK signs the player in by
//itself right after initialize() (and retries on later launches); forcing a manual
//signIn() on top of that re-prompts players who declined on every launch, which
//Google's sign-in guidance tells us not to do.
public class PlayGamesSetup {
	public static void setup(Activity activity) {
		PlayGamesSdk.initialize(activity);
	}
}
