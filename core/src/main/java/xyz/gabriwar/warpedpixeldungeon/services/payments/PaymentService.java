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

	//kick off the store connection + product query; onReady fires once tiers() has data
	public abstract void connect(Runnable onReady);

	public abstract boolean available();

	public abstract ArrayList<Tier> tiers();

	public abstract void donate(String tierId, DonateResult callback);

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
