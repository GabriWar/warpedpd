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

//a store-backed donation provider (Google Play Billing, StoreKit, ...). Installed by the
//platform launcher, same pattern as UpdateService/NewsService. When none is installed the
//supporter page falls back to the Ko-fi link.
public abstract class PaymentService {

	public static class Tier {
		public String id;
		public String name;
		public String price;   //localized, straight from the store

		public Tier(String id, String name, String price) {
			this.id = id;
			this.name = name;
			this.price = price;
		}
	}

	public interface DonateResult {
		void onResult(boolean success, String message);
	}

	/**
	 * The store's signed proof that a subscription exists, passed to the relay so online
	 * play can be limited to supporters. It is the store's bytes verbatim: the relay
	 * checks the signature, so anything we changed on the way would stop verifying.
	 */
	public static class Receipt {
		/** Which store signed it: the two are checked in completely different ways. */
		public final String store;
		public final String payload;
		/** Google's detached signature. Empty for Apple, whose blob signs itself. */
		public final String signature;

		public Receipt(String store, String payload, String signature) {
			this.store = store;
			this.payload = payload;
			this.signature = signature;
		}
	}

	public static final String STORE_PLAY = "play";
	public static final String STORE_APPSTORE = "appstore";

	//kick off the store connection + product query; onReady fires once tiers() has data
	public abstract void connect(Runnable onReady);

	public abstract boolean available();

	public abstract ArrayList<Tier> tiers();

	public abstract void subscribe(String tierId, DonateResult callback);

	/** Null when there is nothing to prove: no subscription, or a store we cannot verify. */
	public Receipt receipt() {
		return null;
	}

	//re-checks the store for a previously bought supporter badge (non-consumable),
	//re-verifying its signature - the secure cross-device restore path
	public abstract void restore(DonateResult callback);

	public abstract String storeName();

	//re-query the store for the current subscription state; called when the game
	//comes back to the foreground. Providers that don't need it leave this alone.
	public void refresh() {}

	//where the player manages or cancels the subscription (store account page)
	public abstract String manageSubscriptionsLink();
}
