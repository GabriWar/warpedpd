package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;

import com.watabou.noosa.Camera;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.RectF;

/**
 * Holding scene for spectators while the host hasn't broadcast a level yet
 * (i.e. host is still in the lobby). One identity card carries the host name
 * and a live latency readout; once the host enters a game, SpectatorReceiver
 * swaps us into GameScene automatically.
 */
public class SpectatorLobbyScene extends PixelScene {

	/** Set by SpectatorReceiver before switching to this scene. */
	public static volatile String pendingHostName = "";

	private RenderedTextBlock pingText;
	private float pingRowY;
	private float cardX, cardW;
	private RenderedTextBlock waitText;
	private long lastPingShown = -1;
	private long animDot = 0;
	private String hostName;

	@Override
	public void create() {
		super.create();

		hostName = (pendingHostName == null || pendingHostName.isEmpty())
				? "Host" : pendingHostName;
		pendingHostName = "";

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		add(new TitleBackground(w, h));

		w -= (int)(insets.left + insets.right);
		h -= (int)(insets.top + insets.bottom);

		// Leaving this scene by any route must tear the spectator client down,
		// or SpectatorReceiver would keep running behind the title screen.
		ExitButton btnExit = new ExitButton() {
			@Override
			protected void onClick() {
				NetManager.stop();
				super.onClick();
			}
		};
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		IconTitle title = new IconTitle(Icons.COMPASS.get(), "Spectating");
		title.setSize(220, 0);
		title.setPos(insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f);
		align(title);
		add(title);

		cardW = Math.min(w - 8, 150);
		cardX = insets.left + (w - cardW) / 2f;
		float yPos = title.bottom() + 10;

		// ---- Host card: who we follow + live latency ----
		float cardH = 28;
		NinePatch card = Chrome.get(Chrome.Type.TOAST_TR);
		card.x = cardX;
		card.y = yPos;
		card.size(cardW, cardH);
		add(card);

		cardRow(cardX, yPos + 4, cardW, "Following", hostName, NetUi.GREEN);

		RenderedTextBlock pingLabel = PixelScene.renderTextBlock("Latency", 6);
		pingLabel.hardlight(NetUi.MUTED);
		pingLabel.setPos(cardX + 6, yPos + 16);
		align(pingLabel);
		add(pingLabel);

		pingRowY = yPos + 15;
		pingText = PixelScene.renderTextBlock("connecting…", 8);
		pingText.hardlight(NetUi.MUTED);
		pingText.setPos(cardX + cardW - pingText.width() - 6, pingRowY);
		align(pingText);
		add(pingText);

		yPos += cardH + 12;

		// ---- Status text (waits + dots animation) ----
		waitText = PixelScene.renderTextBlock("Waiting for host to start a game", 8);
		waitText.hardlight(NetUi.BLUE);
		waitText.setPos(insets.left + (w - waitText.width()) / 2f, yPos);
		align(waitText);
		add(waitText);
		yPos = waitText.bottom() + 6;

		RenderedTextBlock hint = PixelScene.renderTextBlock(
				"Read-only. The host's view will appear here as soon as they enter a level.",
				6);
		hint.hardlight(NetUi.MUTED);
		hint.maxWidth((int)cardW);
		hint.setPos(insets.left + (w - hint.width()) / 2f, yPos);
		align(hint);
		add(hint);
		yPos = hint.bottom() + 14;

		// ---- Disconnect button (anchored bottom) ----
		RedButton btnDisconnect = new RedButton("Disconnect", 9) {
			@Override
			protected void onClick() {
				NetManager.stop();
				WarpedPixelDungeon.switchNoFade(TitleScene.class);
			}
		};
		float bottomY = Math.max(yPos + 8, insets.top + h - 24);
		btnDisconnect.setRect(cardX, bottomY, cardW, 18);
		align(btnDisconnect);
		add(btnDisconnect);

		fadeIn();
	}

	/** WndGameInProgress-style stat row: muted label left, colored value right. */
	private void cardRow(float x, float y, float w, String label, String value, int valueColor) {
		RenderedTextBlock lbl = PixelScene.renderTextBlock(label, 6);
		lbl.hardlight(NetUi.MUTED);
		lbl.setPos(x + 6, y + 1);
		align(lbl);
		add(lbl);

		RenderedTextBlock val = PixelScene.renderTextBlock(value, 8);
		val.hardlight(valueColor);
		val.setPos(x + w - val.width() - 6, y);
		align(val);
		add(val);
	}

	@Override
	public void update() {
		super.update();

		// Animated dots on the wait text — visual confirmation we're alive.
		long now = System.currentTimeMillis();
		if (now - animDot > 400) {
			animDot = now;
			int dots = (int)((now / 400) % 4);
			// Pad to fixed width so the text doesn't drift left as dots accumulate.
			StringBuilder sb = new StringBuilder("Waiting for host to start a game");
			for (int i = 0; i < dots; i++) sb.append('.');
			for (int i = dots; i < 3; i++) sb.append(' ');
			waitText.text(sb.toString());
		}

		long ping = NetManager.getPingMs();
		if (ping == lastPingShown) return;
		lastPingShown = ping;

		if (ping < 0) {
			pingText.text("connecting…");
		} else {
			pingText.text(ping + " ms");
		}
		pingText.hardlight(NetUi.pingColor(ping));
		// Keep the value right-aligned in the card as its width changes.
		pingText.setPos(cardX + cardW - pingText.width() - 6, pingRowY);
		align(pingText);
	}

	@Override
	protected void onBackPressed() {
		NetManager.stop();
		WarpedPixelDungeon.switchNoFade(TitleScene.class);
	}
}
