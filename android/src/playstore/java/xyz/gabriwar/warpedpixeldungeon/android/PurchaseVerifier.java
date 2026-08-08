package xyz.gabriwar.warpedpixeldungeon.android;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

//Client-side purchase signature verification. Google signs every real purchase with the
//app's Play Console license key; tools like Lucky Patcher hand the app a fabricated
//purchase that carries no valid signature, so verifying it here filters those out.
//Not as strong as server-side verification (nothing beats that), but it defeats the
//spoofed-billing attacks, and R8 obfuscation on release builds makes patching this
//check out considerably more work.
public class PurchaseVerifier {

	//the base64 RSA public key from Play Console -> Monetization setup -> Licensing.
	//While this is empty, purchases cannot be verified and are NOT honored on release.
	private static final String PLAY_LICENSE_KEY =
			"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqYwMK6e+LI3Btw3GRp7po7uyYZp3uPm9qdH7WO8kj5Qxpii4492sgrMZcEECWjMUI4hWeRHh8bQF8mGa03Fn/B9nU9JcoDhzO4LpAhYuYC0bTSdvPmBMwXSTkisaOXI3rnj7E+SL5NLhU7L/36mpR9x+lKxVJTHf5v3zDYTnkoay2ve2BHPFjfcuu0m1hchQElymu8ni9N9WFDxHZj5d48LEzzcQx9QW/zIessQrUlueUqQhob68wP4DyBVVgrx9uyCzNm4nlvw01GVXoS9BLxs327DLB5myuxO+aGYDqJslAD4Bn69TmQrGykpWtW+C6M2hyvMgP0Ewa8AwT9CvjwIDAQAB";

	public static boolean configured() {
		return !PLAY_LICENSE_KEY.isEmpty();
	}

	public static boolean verify(String signedData, String signature) {
		if (!configured() || signedData == null || signature == null) {
			return false;
		}
		try {
			byte[] keyBytes = Base64.decode(PLAY_LICENSE_KEY, Base64.DEFAULT);
			PublicKey key = KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(keyBytes));

			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(key);
			sig.update(signedData.getBytes());
			return sig.verify(Base64.decode(signature, Base64.DEFAULT));
		} catch (Exception e) {
			return false;
		}
	}
}
