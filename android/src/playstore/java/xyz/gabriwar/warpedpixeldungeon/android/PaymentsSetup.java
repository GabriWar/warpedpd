package xyz.gabriwar.warpedpixeldungeon.android;

import android.app.Activity;

import xyz.gabriwar.warpedpixeldungeon.services.payments.Payments;

//playstore flavor: installs the Google Play Billing donation provider
public class PaymentsSetup {
	public static void setup(Activity activity) {
		PlayPayments payments = new PlayPayments(activity);
		Payments.service = payments;
		payments.connect(null);
	}
}
