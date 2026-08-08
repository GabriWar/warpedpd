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

package xyz.gabriwar.warpedpixeldungeon.services.payments;

import java.util.ArrayList;

public class Payments {

	public static PaymentService service;

	//the Ko-fi page, used whenever no store-backed provider is installed
	//(github builds, desktop) - and always reachable from the supporter page
	public static final String KOFI_LINK = "https://ko-fi.com/gabriwar";

	//App Store review requires a subscription's purchase screen to link both of these
	//from inside the app, not just from the store listing (guideline 3.1.2)
	public static final String PRIVACY_LINK = "https://gabriwar.github.io/warpedpd/privacy-policy.html";
	public static final String TERMS_LINK   = "https://gabriwar.github.io/warpedpd/terms-of-use.html";

	public static boolean supportsDonations() {
		return service != null && service.available();
	}

	public static ArrayList<PaymentService.Tier> tiers() {
		return supportsDonations() ? service.tiers() : new ArrayList<>();
	}

	public static void donate(String tierId, PaymentService.DonateResult callback) {
		if (supportsDonations()) {
			service.donate(tierId, callback);
		} else {
			callback.onResult(false, "no payment provider");
		}
	}

	public static void restore(PaymentService.DonateResult callback) {
		if (supportsDonations()) {
			service.restore(callback);
		} else {
			callback.onResult(false, "no payment provider");
		}
	}

	public static String storeName() {
		return supportsDonations() ? service.storeName() : "";
	}

	public static String manageSubscriptionsLink() {
		return supportsDonations() ? service.manageSubscriptionsLink() : null;
	}

	//a store provider whose first connection failed (offline, Play busy) never
	//retried on its own; the supporter page kicks it again on open
	public static void reconnect() {
		if (service != null && !service.available()) {
			service.connect(null);
		}
	}
}
