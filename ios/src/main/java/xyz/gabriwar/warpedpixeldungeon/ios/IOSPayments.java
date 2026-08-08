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

package xyz.gabriwar.warpedpixeldungeon.ios;

import com.watabou.noosa.Game;

import org.json.JSONArray;
import org.json.JSONObject;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSNumberFormatter;
import org.robovm.apple.foundation.NSNumberFormatterStyle;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.storekit.SKErrorCode;
import org.robovm.apple.storekit.SKPayment;
import org.robovm.apple.storekit.SKPaymentQueue;
import org.robovm.apple.storekit.SKPaymentTransaction;
import org.robovm.apple.storekit.SKPaymentTransactionObserver;
import org.robovm.apple.storekit.SKPaymentTransactionObserverAdapter;
import org.robovm.apple.storekit.SKProduct;
import org.robovm.apple.storekit.SKProductsRequest;
import org.robovm.apple.storekit.SKProductsRequestDelegate;
import org.robovm.apple.storekit.SKProductsRequestDelegateAdapter;
import org.robovm.apple.storekit.SKProductsResponse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.services.payments.PaymentService;

//StoreKit donations, the iOS counterpart to PlayPayments. The tiers are monthly
//auto-renewing SUBSCRIPTIONS, so supporting is an ongoing pledge. Apple policy forbids
//linking an external donation page (Ko-fi) from an App Store build, so on iOS the
//supporter page only ever offers these in-app products. Product ids must exist in App
//Store Connect as auto-renewable subscriptions (one group) with matching ids.
//
//The supporter flag is reconciled against the App Store receipt (Apple's verifyReceipt),
//so it is revoked once the subscription lapses. That check needs the app's shared secret;
//while RECEIPT_SHARED_SECRET is empty expiry can't be confirmed and the flag is never
//revoked (fresh purchases are still honored).
public class IOSPayments extends PaymentService {

	//subscription product ids in App Store Connect, cheapest first
	private static final String[] PRODUCT_IDS = {
			"wpd_supporter_monthly_1",
			"wpd_supporter_monthly_2",
			"wpd_supporter_monthly_3"
	};
	private static final Set<String> PRODUCT_ID_SET = new HashSet<>(Arrays.asList(PRODUCT_IDS));

	//App Store Connect -> App Information -> App-Specific Shared Secret. Required to verify
	//subscription expiry; leave empty only if you don't need lapse-based revocation.
	private static final String RECEIPT_SHARED_SECRET = "";

	private static final String VERIFY_PROD    = "https://buy.itunes.apple.com/verifyReceipt";
	private static final String VERIFY_SANDBOX = "https://sandbox.itunes.apple.com/verifyReceipt";

	//receipt status: subscription currently active / definitively lapsed / couldn't tell
	private static final int ACTIVE = 1, LAPSED = 0, UNKNOWN = -1;

	private final HashMap<String, SKProduct> products = new HashMap<>();
	private final ArrayList<Tier> tiers = new ArrayList<>();

	//the render-thread callback SupporterScene handed us for the in-flight purchase
	private DonateResult pending;
	private DonateResult restoreCb;

	//fires once tiers() has data, mirroring the Android path
	private Runnable onReady;

	//strong ref so the async products request isn't collected mid-flight
	private SKProductsRequest productsRequest;

	private final SKProductsRequestDelegate productsDelegate = new SKProductsRequestDelegateAdapter() {
		@Override
		public void didReceiveResponse(SKProductsRequest request, SKProductsResponse response) {
			products.clear();
			tiers.clear();
			for (SKProduct p : response.getProducts()) {
				products.put(p.getProductIdentifier(), p);
			}
			//keep the declared order, cheapest first
			for (String id : PRODUCT_IDS) {
				SKProduct p = products.get(id);
				if (p != null) {
					tiers.add(new Tier(id, p.getLocalizedTitle(), formatPrice(p)));
				}
			}
			if (onReady != null) {
				final Runnable r = onReady;
				onReady = null;
				Game.runOnRenderThread(r::run);
			}
		}
	};

	private final SKPaymentTransactionObserver observer = new SKPaymentTransactionObserverAdapter() {
		@Override
		public void updatedTransactions(SKPaymentQueue queue, NSArray<SKPaymentTransaction> transactions) {
			for (SKPaymentTransaction t : transactions) {
				switch (t.getTransactionState()) {
					case Purchased:
						//just bought, so it's active now; a receipt re-check happens on
						//later launches to catch the eventual lapse
						queue.finishTransaction(t);
						Game.runOnRenderThread(() -> WPDSettings.supporter(true));
						completePurchase(true, null);
						break;
					case Restored:
						queue.finishTransaction(t);
						break;
					case Failed:
						queue.finishTransaction(t);
						completePurchase(false, failureMessage(t));
						break;
					default:
						//Purchasing / Deferred: keep waiting, StoreKit will call again
						break;
				}
			}
		}

		@Override
		public void restoreCompletedTransactionsFinished(SKPaymentQueue queue) {
			//the restored subs are on the receipt now; let verification decide if any is live
			validateReceipt(restoreCb);
			restoreCb = null;
		}

		@Override
		public void restoreCompletedTransactionsFailed(SKPaymentQueue queue, NSError error) {
			final DonateResult cb = restoreCb;
			restoreCb = null;
			if (cb != null) Game.runOnRenderThread(() -> cb.onResult(false, "restore failed"));
		}
	};

	@Override
	public void connect(Runnable onReady) {
		SKPaymentQueue.getDefaultQueue().addTransactionObserver(observer);
		//launch-time reconciliation: revoke supporter if the subscription has lapsed
		validateReceipt(null);
		//parental controls / MDM can forbid purchases; the page then shows "unavailable"
		if (!SKPaymentQueue.canMakePayments()) {
			if (onReady != null) Game.runOnRenderThread(onReady::run);
			return;
		}
		this.onReady = onReady;
		productsRequest = new SKProductsRequest(new HashSet<>(Arrays.asList(PRODUCT_IDS)));
		productsRequest.setDelegate(productsDelegate);
		productsRequest.start();
	}

	@Override
	public boolean available() {
		return !tiers.isEmpty();
	}

	@Override
	public ArrayList<Tier> tiers() {
		return tiers;
	}

	@Override
	public String storeName() {
		return "App Store";
	}

	@Override
	public String manageSubscriptionsLink() {
		return "https://apps.apple.com/account/subscriptions";
	}

	@Override
	public void donate(String tierId, DonateResult callback) {
		SKProduct product = products.get(tierId);
		if (product == null) {
			callback.onResult(false, "product unavailable");
			return;
		}
		pending = callback;
		SKPaymentQueue.getDefaultQueue().addPayment(new SKPayment(product));
	}

	@Override
	public void restore(DonateResult callback) {
		restoreCb = callback;
		SKPaymentQueue.getDefaultQueue().restoreCompletedTransactions();
	}

	private void completePurchase(boolean success, String message) {
		if (pending != null) {
			final DonateResult cb = pending;
			pending = null;
			Game.runOnRenderThread(() -> cb.onResult(success, message));
		}
	}

	//a user-cancelled purchase carries a null message so SupporterScene stays quiet;
	//any other failure surfaces a warning
	private static String failureMessage(SKPaymentTransaction t) {
		NSError err = t.getError();
		if (err != null && err.getCode() == SKErrorCode.PaymentCancelled.value()) {
			return null;
		}
		return "purchase failed";
	}

	private static String formatPrice(SKProduct product) {
		//localized currency string, straight from the store, in the buyer's locale
		NSNumberFormatter fmt = new NSNumberFormatter();
		fmt.setNumberStyle(NSNumberFormatterStyle.Currency);
		fmt.setLocale(product.getPriceLocale());
		return fmt.format(product.getPrice());
	}

	//Verifies the App Store receipt off the main thread and reconciles the supporter flag:
	//ACTIVE -> on, LAPSED -> off (the revocation), UNKNOWN -> left untouched so a network
	//blip or missing secret never falsely revokes. An optional callback (from Restore)
	//reports the outcome.
	private void validateReceipt(final DonateResult callback) {
		if (RECEIPT_SHARED_SECRET.isEmpty()) {
			if (callback != null) {
				Game.runOnRenderThread(() -> callback.onResult(false, "restore unavailable"));
			}
			return;
		}
		new Thread(() -> {
			final int status = receiptStatus();
			Game.runOnRenderThread(() -> {
				if (status == ACTIVE)      WPDSettings.supporter(true);
				else if (status == LAPSED) WPDSettings.supporter(false);
				if (callback != null) {
					callback.onResult(status == ACTIVE,
							status == ACTIVE ? null
									: status == LAPSED ? "no active subscription found"
									: "couldn't reach the App Store");
				}
			});
		}, "IAP-Receipt-Validate").start();
	}

	private int receiptStatus() {
		try {
			NSURL url = NSBundle.getMainBundle().getAppStoreReceiptURL();
			if (url == null) return UNKNOWN;
			File f = new File(url.getPath());
			if (!f.exists()) return UNKNOWN;   //no receipt yet (nothing ever bought)
			String base64 = java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath()));

			JSONObject resp = postVerify(VERIFY_PROD, base64);
			//21007: a sandbox receipt sent to production - retry against sandbox
			if (resp != null && resp.optInt("status", -1) == 21007) {
				resp = postVerify(VERIFY_SANDBOX, base64);
			}
			if (resp == null) return UNKNOWN;              //network trouble
			if (resp.optInt("status", -1) != 0) return UNKNOWN; //malformed/unauth: don't revoke

			return latestExpiryMs(resp) > System.currentTimeMillis() ? ACTIVE : LAPSED;
		} catch (Exception e) {
			return UNKNOWN;
		}
	}

	private static JSONObject postVerify(String endpoint, String base64) {
		try {
			JSONObject body = new JSONObject();
			body.put("receipt-data", base64);
			body.put("password", RECEIPT_SHARED_SECRET);
			body.put("exclude-old-transactions", false);

			HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(8000);
			conn.setReadTimeout(8000);
			conn.setRequestProperty("Content-Type", "application/json");
			try (OutputStream os = conn.getOutputStream()) {
				os.write(body.toString().getBytes(StandardCharsets.UTF_8));
			}
			int code = conn.getResponseCode();
			InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
			if (in == null) return null;
			return new JSONObject(readAll(in));
		} catch (Exception e) {
			return null;
		}
	}

	//latest expiry across our subscription products, in ms since epoch (0 if none)
	private static long latestExpiryMs(JSONObject resp) {
		JSONArray info = resp.optJSONArray("latest_receipt_info");
		if (info == null) {
			JSONObject receipt = resp.optJSONObject("receipt");
			if (receipt != null) info = receipt.optJSONArray("in_app");
		}
		long max = 0;
		if (info != null) {
			for (int i = 0; i < info.length(); i++) {
				JSONObject t = info.optJSONObject(i);
				if (t == null || !PRODUCT_ID_SET.contains(t.optString("product_id", ""))) continue;
				try {
					long exp = Long.parseLong(t.optString("expires_date_ms", "0"));
					if (exp > max) max = exp;
				} catch (NumberFormatException ignored) {}
			}
		}
		return max;
	}

	private static String readAll(InputStream in) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int n;
		while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
		return out.toString("UTF-8");
	}
}
