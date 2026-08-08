package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHeroInfo;
import xyz.gabriwar.warpedpixeldungeon.windows.WndKeyBindings;

import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.RectF;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Hero class selection for players joining a multiplayer game. Layout:
 *
 *   ┌──────────── Joining {host}'s game ─────────────┐
 *   │ host card  (IP · floor · class)                │
 *   ├────────────────────────────────────────────────┤
 *   │ hero card  (portrait · selected class info)    │
 *   ├────────────────────────────────────────────────┤
 *   │ class grid  (locked classes desaturated)       │
 *   ├────────────────────────────────────────────────┤
 *   │             [ Join Game ]                       │
 *   └────────────────────────────────────────────────┘
 *
 * Async PEEK to the host on open: if the host has a stashed netHero matching
 * our multiplayerName(), the hero card flips to "Continue as ..." mode and
 * the class grid becomes a read-only preview.
 */
public class PlayerLobbyScene extends PixelScene {

	private RenderedTextBlock subtitle;
	private RenderedTextBlock hostNameLabel;
	private RenderedTextBlock hostMetaLabel;

	private NinePatch heroPanelBg;
	private Image heroPortrait;
	private RenderedTextBlock heroName;
	private RenderedTextBlock heroDesc;

	private final ArrayList<HeroBtn> classButtons = new ArrayList<>();
	private IconButton btnInfo;
	private StyledButton btnJoin;
	private RenderedTextBlock joinHint;

	private HeroClass selectedClass = null;
	private volatile JSONObject stashedHero = null;

	@Override
	public void create() {
		super.create();
		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		add(new TitleBackground(w, h));

		w -= (int)(insets.left + insets.right);
		h -= (int)(insets.top + insets.bottom);

		ExitButton exit = new ExitButton();
		exit.setPos(insets.left + w - exit.width(), insets.top);
		add(exit);

		IconTitle title = new IconTitle(Icons.CONTROLLER.get(), "Join Game");
		title.setSize(220, 0);
		title.setPos(insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f);
		align(title);
		add(title);

		subtitle = PixelScene.renderTextBlock("Checking host…", 7);
		subtitle.hardlight(NetUi.MUTED);
		subtitle.setPos(insets.left + (w - subtitle.width()) / 2f, title.bottom() + 4);
		align(subtitle);
		add(subtitle);

		float cardW = Math.min(w - 16, 220);
		float cardX = insets.left + (w - cardW) / 2f;
		float yPos = subtitle.bottom() + 8;

		// ---- Host info card ----
		yPos = buildHostCard(cardX, yPos, cardW);
		yPos += 6;

		// ---- Hero card (portrait + class info) ----
		float heroCardH = 80;
		heroPanelBg = Chrome.get(Chrome.Type.TOAST_TR);
		heroPanelBg.x = cardX;
		heroPanelBg.y = yPos;
		heroPanelBg.size(cardW, heroCardH);
		add(heroPanelBg);

		heroName = PixelScene.renderTextBlock("", 9);
		heroName.hardlight(Window.TITLE_COLOR);
		add(heroName);

		heroDesc = PixelScene.renderTextBlock("", 6);
		heroDesc.hardlight(0xCCCCCC);
		heroDesc.maxWidth((int)(cardW - 60));
		add(heroDesc);

		btnInfo = new IconButton(Icons.get(Icons.INFO)) {
			@Override protected void onClick() {
				if (selectedClass != null) {
					WarpedPixelDungeon.scene().addToFront(new WndHeroInfo(selectedClass));
				}
			}
			@Override protected String hoverText() {
				return Messages.titleCase(Messages.get(WndKeyBindings.class, "hero_info"));
			}
		};
		btnInfo.setSize(20, 20);
		btnInfo.setPos(cardX + cardW - 22, yPos + 4);
		btnInfo.visible = btnInfo.active = false;
		add(btnInfo);

		yPos += heroCardH + 8;

		// ---- Class grid ----
		HeroClass[] classes = HeroClass.values();
		int btnW = 28;
		int btnH = 26;
		int gap = 2;
		int totalWBtn = classes.length * btnW + (classes.length - 1) * gap;
		float startX = insets.left + (w - totalWBtn) / 2f;
		for (HeroClass hc : classes) {
			HeroBtn btn = new HeroBtn(hc);
			btn.setRect(startX, yPos, btnW, btnH);
			align(btn);
			add(btn);
			classButtons.add(btn);
			startX += btnW + gap;
		}
		yPos += btnH + 12;

		// ---- Join button ----
		btnJoin = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Join Game", 9) {
			@Override protected void onClick() {
				if (stashedHero == null && selectedClass == null) return;
				joinAsPlayer();
			}
		};
		btnJoin.icon(Icons.get(Icons.ENTER));
		btnJoin.textColor(Window.TITLE_COLOR);
		float joinW = Math.max(120, btnJoin.reqWidth() + 16);
		btnJoin.setRect(insets.left + (w - joinW) / 2f, yPos, joinW, 21);
		align(btnJoin);
		add(btnJoin);
		btnJoin.enable(false);

		joinHint = PixelScene.renderTextBlock("Pick a class to join", 6);
		joinHint.hardlight(NetUi.MUTED);
		joinHint.setPos(insets.left + (w - joinHint.width()) / 2f, btnJoin.bottom() + 4);
		align(joinHint);
		add(joinHint);

		fadeIn();
		runPeek();
	}

	private float buildHostCard(float x, float y, float w) {
		float cardH = 28;
		NinePatch card = Chrome.get(Chrome.Type.TOAST_TR);
		card.x = x;
		card.y = y;
		card.size(w, cardH);
		add(card);

		String ip = WndMultiplayer.pendingHostIP;
		if (ip == null || ip.isEmpty()) ip = "?";

		String myName = WPDSettings.multiplayerName();
		if (myName == null || myName.isEmpty()) myName = "(no name set)";

		hostNameLabel = cardRow(x, y + 4, w, "Joining", ip, NetUi.GREEN);
		cardRow(x, y + 15, w, "Playing as", myName, NetUi.YELLOW);

		hostMetaLabel = PixelScene.renderTextBlock("", 6);
		hostMetaLabel.hardlight(NetUi.MUTED);
		hostMetaLabel.maxWidth((int)w);
		hostMetaLabel.setPos(x, y + cardH + 1);
		add(hostMetaLabel);

		return y + cardH + hostMetaLabel.height() + 2;
	}

	/** WndGameInProgress-style stat row: muted label left, colored value right. */
	private RenderedTextBlock cardRow(float x, float y, float w, String label, String value, int valueColor) {
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
		return val;
	}

	private void setSelected(HeroClass cls) {
		selectedClass = cls;
		for (HeroBtn b : classButtons) b.refresh();
		refreshHeroCard();
		btnInfo.visible = btnInfo.active = (cls != null);
		btnJoin.enable(cls != null);
	}

	private void refreshHeroCard() {
		float cardX = heroPanelBg.x;
		float cardY = heroPanelBg.y;
		float cardW = heroPanelBg.width();
		float cardH = heroPanelBg.height();

		if (heroPortrait != null) heroPortrait.killAndErase();
		heroPortrait = null;

		if (selectedClass == null) {
			heroName.text("Select a class");
			heroName.hardlight(NetUi.MUTED);
			heroName.setPos(cardX + (cardW - heroName.width()) / 2f,
					cardY + (cardH - heroName.height()) / 2f);
			align(heroName);
			heroDesc.text("");
			return;
		}

		heroPortrait = HeroSprite.avatar(selectedClass, 0);
		heroPortrait.scale.set(2.5f);
		heroPortrait.x = cardX + 8;
		heroPortrait.y = cardY + (cardH - heroPortrait.height() * heroPortrait.scale.y) / 2f;
		PixelScene.align(heroPortrait);
		add(heroPortrait);

		String displayName;
		String displayDesc;

		if (stashedHero != null) {
			int lvl = stashedHero.optInt("lvl", 1);
			int hp  = stashedHero.optInt("hp", 0);
			int ht  = stashedHero.optInt("ht", 0);
			int items = stashedHero.optInt("items", 0);
			int depth = stashedHero.optInt("depth", 0);
			displayName = "Continue as " + Messages.titleCase(selectedClass.title());
			StringBuilder sb = new StringBuilder();
			sb.append("Lvl ").append(lvl);
			if (ht > 0) sb.append(" · HP ").append(hp).append("/").append(ht);
			if (depth > 0) sb.append(" · Floor ").append(depth);
			sb.append(" · ").append(items).append(items == 1 ? " item" : " items");
			displayDesc = sb.toString();
			heroName.hardlight(NetUi.GREEN);
		} else {
			displayName = Messages.titleCase(selectedClass.title());
			try { displayDesc = Messages.get(selectedClass, "desc"); }
			catch (Exception e) { displayDesc = ""; }
			if (displayDesc == null || displayDesc.startsWith("!!!")) displayDesc = "";
			heroName.hardlight(Window.TITLE_COLOR);
		}

		heroName.text(displayName);
		heroName.setPos(cardX + 50, cardY + 8);
		align(heroName);

		heroDesc.text(displayDesc);
		heroDesc.maxWidth((int)(cardW - 58));
		heroDesc.setPos(cardX + 50, heroName.bottom() + 3);
		align(heroDesc);
	}

	/** Async lobby peek. Updates host card subtitle + hero card on response. */
	private void runPeek() {
		final String ip = WndMultiplayer.pendingHostIP;
		final String name = WPDSettings.multiplayerName();
		if (ip == null || ip.isEmpty()) {
			updateSubtitle("Pick a class to join", NetUi.MUTED);
			return;
		}
		NetManager.peekCharacter(ip, name, info -> Game.runOnRenderThread(() -> {
			if (subtitle == null) return;
			if (info != null && info.optBoolean("found", false)) {
				stashedHero = info;
				HeroClass hc = HeroClass.values()[info.optInt("cls", 0)];
				setSelected(hc);
				updateSubtitle("Saved hero found — your old character will be restored", NetUi.GREEN);
				updateJoinHint("Click Join to continue", NetUi.GREEN);
				int lvl = info.optInt("lvl", 1);
				int depth = info.optInt("depth", 0);
				if (hostMetaLabel != null) {
					String s = "Stashed hero: Lvl " + lvl
							+ (depth > 0 ? " · Floor " + depth : "");
					hostMetaLabel.text(s);
					float cardW = heroPanelBg.width();
					hostMetaLabel.setPos(heroPanelBg.x + (cardW - hostMetaLabel.width()) / 2f,
							hostMetaLabel.top());
					PixelScene.align(hostMetaLabel);
				}
				for (HeroBtn b : classButtons) b.lockedReclaim = true;
			} else {
				updateSubtitle("No saved hero — pick a class to start fresh", NetUi.YELLOW);
				if (hostMetaLabel != null) {
					hostMetaLabel.text("Fresh spawn at the host's current floor.");
					float cardW = heroPanelBg.width();
					hostMetaLabel.setPos(heroPanelBg.x + (cardW - hostMetaLabel.width()) / 2f,
							hostMetaLabel.top());
					PixelScene.align(hostMetaLabel);
				}
			}
		}));
	}

	private void updateSubtitle(String text, int color) {
		subtitle.text(text);
		subtitle.hardlight(color);
		RectF in = getCommonInsets();
		subtitle.setPos(in.left + (Camera.main.width - in.left - in.right - subtitle.width()) / 2f,
				subtitle.top());
		align(subtitle);
	}

	private void updateJoinHint(String text, int color) {
		if (joinHint == null) return;
		joinHint.text(text);
		joinHint.hardlight(color);
		RectF in = getCommonInsets();
		joinHint.setPos(in.left + (Camera.main.width - in.left - in.right - joinHint.width()) / 2f,
				joinHint.top());
		align(joinHint);
	}

	private void joinAsPlayer() {
		String ip = WndMultiplayer.pendingHostIP;
		if (ip == null || ip.isEmpty()) {
			WarpedPixelDungeon.switchNoFade(TitleScene.class);
			return;
		}
		try {
			JSONObject heroData = new JSONObject();
			heroData.put("cls", selectedClass.ordinal());
			heroData.put("hp", 20);
			heroData.put("ht", 20);
			heroData.put("str", 10);
			heroData.put("lvl", 1);
			heroData.put("exp", 0);
			heroData.put("name", WPDSettings.multiplayerName());
			NetManager.startPlayer(ip, heroData);
		} catch (Exception e) {
			e.printStackTrace();
			WarpedPixelDungeon.switchNoFade(TitleScene.class);
		}
	}

	@Override
	protected void onBackPressed() {
		WarpedPixelDungeon.switchNoFade(TitleScene.class);
	}

	private class HeroBtn extends StyledButton {
		private final HeroClass cls;
		boolean lockedReclaim = false;
		final boolean unlocked;

		HeroBtn(HeroClass cls) {
			super(Chrome.Type.GREY_BUTTON_TR, "");
			this.cls = cls;
			this.unlocked = cls.isUnlocked();
			icon(new Image(cls.spritesheet(), 0, 90, 12, 15));
		}

		private float targetBrightness() {
			if (!unlocked && !lockedReclaim) return 0.25f;
			return cls == selectedClass ? 1f : 0.55f;
		}

		void refresh() {
			if (icon == null) return;
			icon.brightness(targetBrightness());
		}

		@Override
		public void update() {
			super.update();
			if (icon != null) icon.brightness(targetBrightness());
		}

		@Override
		protected void onClick() {
			super.onClick();
			if (lockedReclaim) {
				WarpedPixelDungeon.scene().addToFront(new WndHeroInfo(cls));
				return;
			}
			if (!unlocked) {
				WarpedPixelDungeon.scene().addToFront(new WndHeroInfo(cls));
				return;
			}
			if (cls == selectedClass) {
				WarpedPixelDungeon.scene().addToFront(new WndHeroInfo(cls));
			} else {
				setSelected(cls);
			}
		}
	}
}
