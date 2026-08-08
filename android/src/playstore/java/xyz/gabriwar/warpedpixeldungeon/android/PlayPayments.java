package xyz.gabriwar.warpedpixeldungeon.android;

import android.app.Activity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.services.payments.PaymentService;

//Google Play Billing donations. The tiers are monthly auto-renewing SUBSCRIPTIONS, so
//supporting is an ongoing pledge, not a one-off gift. Product ids must exist in the Play
//Console as subscriptions with matching ids and a base plan each.
public class PlayPayments extends PaymentService implements PurchasesUpdatedListener {

	//subscription product ids in the Play Console, cheapest first. Each is a monthly
	//auto-renewing sub; the supporter flag tracks whether ANY of them is currently active
	//and is revoked once the subscription lapses (Play stops returning expired subs).
	private static final List<String> PRODUCT_IDS = Arrays.asList(
			"wpd_supporter_monthly_1",
			"wpd_supporter_monthly_2",
			"wpd_supporter_monthly_3"
	);

	private final Activity activity;
	private final BillingClient client;

	private final HashMap<String, ProductDetails> products = new HashMap<>();
	//the offer token a subscription purchase must be launched with, per product
	private final HashMap<String, String> offerTokens = new HashMap<>();
	private final ArrayList<Tier> tiers = new ArrayList<>();

	private DonateResult pending;
	//the verified subscription currently active, if any: switching tiers must replace
	//it rather than start a second parallel subscription
	private Purchase activePurchase;

	public PlayPayments(Activity activity) {
		this.activity = activity;
		client = BillingClient.newBuilder(activity)
				.setListener(this)
				.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
				//Play may not be ready at launch (offline, service busy); the library
				//then reconnects by itself instead of leaving the tiers empty for good
				.enableAutoServiceReconnection()
				.build();
	}

	@Override
	public void connect(Runnable onReady) {
		client.startConnection(new BillingClientStateListener() {
			@Override
			public void onBillingSetupFinished(BillingResult result) {
				if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
					queryProducts(onReady);
					//re-check on every launch so an expired subscription revokes supporter
					refreshSupporterStatus();
				}
			}

			@Override
			public void onBillingServiceDisconnected() {
				//auto-reconnection is on; status is left untouched so a transient
				//disconnect never falsely revokes an active supporter
			}
		});
	}

	@Override
	public void refresh() {
		if (client.isReady()) {
			refreshSupporterStatus();
		}
	}

	@Override
	public String manageSubscriptionsLink() {
		return "https://play.google.com/store/account/subscriptions?package=" + activity.getPackageName();
	}

	private void queryProducts(Runnable onReady) {
		ArrayList<QueryProductDetailsParams.Product> query = new ArrayList<>();
		for (String id : PRODUCT_IDS) {
			query.add(QueryProductDetailsParams.Product.newBuilder()
					.setProductId(id)
					.setProductType(BillingClient.ProductType.SUBS)
					.build());
		}
		client.queryProductDetailsAsync(
				QueryProductDetailsParams.newBuilder().setProductList(query).build(),
				(result, productDetailsResult) -> {
					if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
					//PBL 8: callback now returns QueryProductDetailsResult; the fetched
					//list is pulled from it (unfetched products carry status codes we ignore)
					List<ProductDetails> details = productDetailsResult.getProductDetailsList();
					products.clear();
					offerTokens.clear();
					tiers.clear();
					for (ProductDetails d : details) {
						products.put(d.getProductId(), d);
					}
					//keep the declared order, cheapest first
					for (String id : PRODUCT_IDS) {
						ProductDetails d = products.get(id);
						if (d == null) continue;
						List<ProductDetails.SubscriptionOfferDetails> offers = d.getSubscriptionOfferDetails();
						if (offers == null || offers.isEmpty()) continue;
						//the first offer is the base plan; its last pricing phase is the
						//recurring monthly price (any intro/trial phases come before it)
						ProductDetails.SubscriptionOfferDetails offer = offers.get(0);
						List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
						if (phases.isEmpty()) continue;
						String price = phases.get(phases.size() - 1).getFormattedPrice();
						offerTokens.put(id, offer.getOfferToken());
						tiers.add(new Tier(id, d.getName(), price));
					}
					if (onReady != null) activity.runOnUiThread(onReady);
				});
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
		return "Google Play";
	}

	@Override
	public void donate(String tierId, DonateResult callback) {
		ProductDetails details = products.get(tierId);
		String offerToken = offerTokens.get(tierId);
		if (details == null || offerToken == null) {
			callback.onResult(false, "product unavailable");
			return;
		}
		pending = callback;
		BillingFlowParams.Builder params = BillingFlowParams.newBuilder()
				.setProductDetailsParamsList(Arrays.asList(
						BillingFlowParams.ProductDetailsParams.newBuilder()
								.setProductDetails(details)
								.setOfferToken(offerToken)
								.build()));
		//already subscribed to another tier: replace it (prorated) instead of billing
		//two subscriptions side by side
		if (activePurchase != null && !activePurchase.getProducts().contains(tierId)) {
			params.setSubscriptionUpdateParams(BillingFlowParams.SubscriptionUpdateParams.newBuilder()
					.setOldPurchaseToken(activePurchase.getPurchaseToken())
					.setSubscriptionReplacementMode(
							BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION)
					.build());
		}
		BillingResult launched = client.launchBillingFlow(activity, params.build());
		if (launched.getResponseCode() != BillingClient.BillingResponseCode.OK) {
			//the sheet never opened, so onPurchasesUpdated never fires for this call
			pending = null;
			boolean owned = launched.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
			callback.onResult(false, owned ? null : launched.getDebugMessage());
		}
	}

	//a verified subscription purchase: acknowledged (never consumed) so Play keeps it, and
	//the supporter flag goes on. Ack must happen within 3 days or Play auto-refunds.
	private void honor(Purchase p) {
		if (!p.isAcknowledged()) {
			client.acknowledgePurchase(
					AcknowledgePurchaseParams.newBuilder()
							.setPurchaseToken(p.getPurchaseToken()).build(),
					result -> { /* subscription stays on the account */ });
		}
		activePurchase = p;
		WPDSettings.supporter(true);
	}

	//launch-time reconciliation: supporter is true iff a verified subscription is still
	//active. Play only returns non-expired subs here, so a lapsed pledge revokes the flag.
	//Skipped entirely when verification isn't configured - we can't trust anything then,
	//and must not falsely revoke.
	private void refreshSupporterStatus() {
		if (!PurchaseVerifier.configured()) return;
		client.queryPurchasesAsync(
				QueryPurchasesParams.newBuilder()
						.setProductType(BillingClient.ProductType.SUBS).build(),
				(result, purchases) -> {
					if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
					boolean active = false;
					for (Purchase p : purchases) {
						if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED
								&& PurchaseVerifier.verify(p.getOriginalJson(), p.getSignature())) {
							honor(p);
							active = true;
						}
					}
					if (!active) activePurchase = null;
					final boolean supporter = active;
					activity.runOnUiThread(() -> WPDSettings.supporter(supporter));
				});
	}

	@Override
	public void restore(DonateResult callback) {
		client.queryPurchasesAsync(
				QueryPurchasesParams.newBuilder()
						.setProductType(BillingClient.ProductType.SUBS).build(),
				(result, purchases) -> {
					boolean found = false;
					if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
						for (Purchase p : purchases) {
							//same signature verification as a fresh purchase - a spoofed
							//"restore" fails exactly like a spoofed purchase does
							if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED
									&& PurchaseVerifier.verify(p.getOriginalJson(), p.getSignature())) {
								honor(p);
								found = true;
							}
						}
					}
					if (!found) activePurchase = null;
					final boolean ok = found;
					//nothing active also means the flag should be off, not just "not found"
					activity.runOnUiThread(() -> {
						WPDSettings.supporter(ok);
						callback.onResult(ok, ok ? null : "no active subscription found");
					});
				});
	}

	@Override
	public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
		if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
			for (Purchase p : purchases) {
				if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;

				//spoofed-billing resilience (Lucky Patcher et al.): a real purchase is
				//RSA-signed by Play with the app's license key; fabricated ones aren't.
				//Only a verified purchase is honored. If no key is configured yet the
				//check cannot pass, and we fail closed rather than open.
				if (!PurchaseVerifier.verify(p.getOriginalJson(), p.getSignature())) {
					if (pending != null) {
						DonateResult cb = pending;
						pending = null;
						cb.onResult(false, "purchase could not be verified");
					}
					continue;
				}

				honor(p);
				if (pending != null) {
					DonateResult cb = pending;
					pending = null;
					cb.onResult(true, null);
				}
			}
		} else if (pending != null) {
			DonateResult cb = pending;
			pending = null;
			boolean cancelled = result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED;
			cb.onResult(false, cancelled ? null : result.getDebugMessage());
		}
	}
}
