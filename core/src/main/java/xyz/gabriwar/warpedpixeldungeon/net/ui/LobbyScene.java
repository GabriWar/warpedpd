package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.Discovery;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.HeroSelectScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.windows.WndGameInProgress;

import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.RectF;

import java.util.ArrayList;

/**
 * Host-side multiplayer lobby, styled after StartScene: title background,
 * icon title, then one centered column holding
 *
 *   - an identity card (host name + the LAN address players type),
 *   - the save-slot picker (same slots as the solo StartScene),
 *   - a live connections panel listing each joined player with their class
 *     sprite, plus a spectator counter.
 *
 * On scene-enter we silently start the host server if it isn't already up.
 */
public class LobbyScene extends PixelScene {

	private static final int SLOT_HEIGHT = 22;

	private RenderedTextBlock errorText;
	private RenderedTextBlock connSummary;
	private NinePatch connPanel;
	private float connPanelX, connPanelY, connPanelW;
	private float rosterY;
	private final ArrayList<Gizmo> rosterItems = new ArrayList<>();
	private String lastConnSig = null;
	private long animDot = 0;

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

		ExitButton btnExit = new ExitButton();
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		IconTitle title = new IconTitle(Icons.CONTROLLER.get(), "Hosting");
		title.setSize(220, 0);
		title.setPos(insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f);
		align(title);
		add(title);

		float yPos = title.bottom() + 6;
		float colW = Math.min(w - 8, 150);
		float colX = insets.left + (w - colW) / 2f;

		// Try to start hosting. Captures the failure message into the card area.
		String startErr = null;
		if (!NetManager.isHost()) {
			try {
				NetManager.startHost();
			} catch (Exception e) {
				startErr = e.getMessage();
			}
		}

		// ---- Identity card: name + address ----
		yPos = buildIdentityCard(colX, yPos, colW, startErr);
		yPos += 3;

		RenderedTextBlock hint = PixelScene.renderTextBlock(
				"Pick a save to play — friends can join or watch at any time.", 6);
		hint.hardlight(NetUi.MUTED);
		hint.maxWidth((int) colW);
		hint.setPos(colX + (colW - hint.width()) / 2f, yPos);
		align(hint);
		add(hint);
		yPos = hint.bottom() + 4;

		// ---- Save slot picker ----
		ArrayList<GamesInProgress.Info> games = GamesInProgress.checkAll();
		int slotGap = 4;
		for (GamesInProgress.Info game : games) {
			SaveSlotButton existing = new SaveSlotButton();
			existing.set(game.slot);
			existing.setRect(colX, yPos, colW, SLOT_HEIGHT);
			yPos += SLOT_HEIGHT + slotGap;
			align(existing);
			add(existing);
		}
		if (games.size() < GamesInProgress.MAX_SLOTS) {
			SaveSlotButton newGame = new SaveSlotButton();
			newGame.set(GamesInProgress.firstEmpty());
			newGame.setRect(colX, yPos, colW, SLOT_HEIGHT);
			yPos += SLOT_HEIGHT + slotGap;
			align(newGame);
			add(newGame);
		}
		GamesInProgress.curSlot = 0;

		yPos += 2;

		// ---- Connections panel ----
		yPos = buildConnectionsPanel(colX, yPos, colW);

		// ---- Stop hosting button — anchored to the bottom of the safe area ----
		RedButton btnStop = new RedButton("Stop Hosting", 9) {
			@Override
			protected void onClick() {
				NetManager.stop();
				WarpedPixelDungeon.switchNoFade(TitleScene.class);
			}
		};
		float stopY = Math.max(yPos + 6, insets.top + h - 22);
		btnStop.setRect(colX, stopY, colW, 18);
		add(btnStop);

		fadeIn();
	}

	private float buildIdentityCard(float x, float y, float w, String startErr) {
		float cardH = 28;
		NinePatch card = Chrome.get(Chrome.Type.TOAST_TR);
		card.x = x;
		card.y = y;
		card.size(w, cardH);
		add(card);

		String name = WPDSettings.multiplayerName();
		if (name == null || name.isEmpty()) name = "Host";

		cardRow(x, y + 4, w, "Hosting as", name, 0xFFFFFF);
		cardRow(x, y + 15, w, "LAN address",
				Discovery.getLocalIP() + ":" + NetManager.PORT, NetUi.GREEN);

		// If startHost() blew up, show the error in red beneath the card.
		if (startErr != null) {
			errorText = PixelScene.renderTextBlock("Error: " + startErr, 6);
			errorText.hardlight(NetUi.RED);
			errorText.maxWidth((int) w);
			errorText.setPos(x, y + cardH + 2);
			add(errorText);
			return y + cardH + errorText.height() + 2;
		}
		return y + cardH;
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

	private float buildConnectionsPanel(float x, float y, float w) {
		// Panel grows in update() based on roster line count.
		connPanel = Chrome.get(Chrome.Type.TOAST_TR);
		connPanelX = x;
		connPanelY = y;
		connPanelW = w;
		connPanel.x = x;
		connPanel.y = y;
		connPanel.size(w, 32);
		add(connPanel);

		RenderedTextBlock header = PixelScene.renderTextBlock("Connections", 7);
		header.hardlight(Window.TITLE_COLOR);
		header.setPos(x + 6, y + 4);
		align(header);
		add(header);

		connSummary = PixelScene.renderTextBlock("Waiting for players", 6);
		connSummary.hardlight(NetUi.MUTED);
		connSummary.maxWidth((int)(w - 12));
		connSummary.setPos(x + 6, header.bottom() + 3);
		add(connSummary);

		rosterY = connSummary.bottom() + 3;

		return y + 32;
	}

	@Override
	public void update() {
		super.update();

		int players = NetManager.getPlayerCount();
		int specs   = NetManager.getSpectatorCount();

		// Animated dots while empty — same idiom as the other waiting screens.
		if (players + specs == 0) {
			long now = System.currentTimeMillis();
			if (now - animDot > 400) {
				animDot = now;
				int dots = (int) ((now / 400) % 4);
				StringBuilder sb = new StringBuilder("Waiting for players");
				for (int i = 0; i < dots; i++) sb.append('.');
				connSummary.text(sb.toString());
			}
		}

		// Roster rebuild only when membership actually changes.
		StringBuilder sigB = new StringBuilder();
		sigB.append(players).append(',').append(specs);
		for (Hero h : NetManager.getNetHeroes()) {
			sigB.append('|').append(h.id()).append(':')
					.append(h.netOwnerName).append(':').append(h.heroClass);
		}
		String sig = sigB.toString();
		if (sig.equals(lastConnSig)) return;
		lastConnSig = sig;

		for (Gizmo g : rosterItems) {
			if (g != null) g.killAndErase();
		}
		rosterItems.clear();

		if (players + specs == 0) {
			connSummary.text("Waiting for players");
			connSummary.hardlight(NetUi.MUTED);
			connPanel.size(connPanelW, 32);
			return;
		}

		StringBuilder sb = new StringBuilder();
		if (players > 0) sb.append(players).append(players > 1 ? " players" : " player");
		if (specs > 0) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(specs).append(specs > 1 ? " spectators" : " spectator");
		}
		connSummary.text(sb.toString());
		connSummary.hardlight(0xCCCCCC);

		// Roster — one row per player: class sprite, name, class title.
		float ry = rosterY;
		for (Hero h : NetManager.getNetHeroes()) {
			Image spr = new Image(h.heroClass.spritesheet(), 0, 90, 12, 15);
			spr.x = connPanelX + 7;
			spr.y = ry;
			align(spr);
			add(spr);
			rosterItems.add(spr);

			String n = (h.netOwnerName != null && !h.netOwnerName.isEmpty())
					? h.netOwnerName : ("Player " + h.id());
			RenderedTextBlock name = PixelScene.renderTextBlock(n, 8);
			name.hardlight(NetUi.YELLOW);
			name.setPos(spr.x + 16, ry + (15 - name.height()) / 2f);
			align(name);
			add(name);
			rosterItems.add(name);

			RenderedTextBlock cls = PixelScene.renderTextBlock(
					Messages.titleCase(h.heroClass.title()), 6);
			cls.hardlight(NetUi.MUTED);
			cls.setPos(name.right() + 4, ry + (15 - cls.height()) / 2f);
			align(cls);
			add(cls);
			rosterItems.add(cls);

			ry += 16;
		}

		if (specs > 0) {
			RenderedTextBlock watching = PixelScene.renderTextBlock(
					"+ " + specs + " watching", 6);
			watching.hardlight(NetUi.BLUE);
			watching.setPos(connPanelX + 7, ry + 1);
			align(watching);
			add(watching);
			rosterItems.add(watching);
			ry += 10;
		}

		connPanel.size(connPanelW, Math.max(32, ry + 4 - connPanelY));
	}

	@Override
	protected void onBackPressed() {
		NetManager.stop();
		WarpedPixelDungeon.switchNoFade(TitleScene.class);
	}

	// ---- SaveSlotButton (unchanged from original — local class) ----
	private static class SaveSlotButton extends Button {

		private NinePatch bg;

		private Image hero;
		private RenderedTextBlock name;
		private RenderedTextBlock lastPlayed;

		private Image steps;
		private BitmapText depth;
		private Image classIcon;
		private BitmapText level;

		private int slot;
		private boolean newGame;

		@Override
		protected void createChildren() {
			super.createChildren();

			bg = Chrome.get(Chrome.Type.TOAST_TR);
			add(bg);

			name = PixelScene.renderTextBlock(9);
			add(name);

			lastPlayed = PixelScene.renderTextBlock(6);
			add(lastPlayed);
		}

		public void set(int slot) {
			this.slot = slot;
			GamesInProgress.Info info = GamesInProgress.check(slot);
			newGame = info == null;

			if (newGame) {
				name.text("New Game");

				if (hero != null) {
					remove(hero);   hero = null;
					remove(steps);  steps = null;
					remove(depth);  depth = null;
					remove(classIcon); classIcon = null;
					remove(level);  level = null;
				}
			} else {
				if (info.heroName != null && !info.heroName.isEmpty()) {
					name.text(info.heroName);
				} else if (info.subClass != HeroSubClass.NONE) {
					name.text(info.subClass.title());
				} else {
					name.text(info.heroClass.title());
				}

				if (hero == null) {
					hero = new Image(info.heroClass.spritesheet(), 0, 15 * info.armorTier, 12, 15);
					add(hero);

					steps = new Image(Icons.get(Icons.STAIRS));
					add(steps);
					depth = new BitmapText(PixelScene.pixelFont);
					add(depth);

					classIcon = new Image(Icons.get(info.heroClass));
					add(classIcon);
					level = new BitmapText(PixelScene.pixelFont);
					add(level);
				} else {
					hero.copy(new Image(info.heroClass.spritesheet(), 0, 15 * info.armorTier, 12, 15));
					classIcon.copy(Icons.get(info.heroClass));
				}

				long diff = Game.realTime - info.lastPlayed;
				if (diff < 60_000) {
					lastPlayed.text("just now");
				} else if (diff < 2 * 60 * 60_000) {
					lastPlayed.text((diff / 60_000) + "m ago");
				} else if (diff < 2 * 24 * 60 * 60_000) {
					lastPlayed.text((diff / (60 * 60_000)) + "h ago");
				} else {
					lastPlayed.text((diff / (24 * 60 * 60_000)) + "d ago");
				}

				depth.text(Integer.toString(info.depth));
				depth.measure();

				level.text(Integer.toString(info.level));
				level.measure();

				if (info.challenges > 0) {
					name.hardlight(Window.TITLE_COLOR);
					lastPlayed.hardlight(Window.TITLE_COLOR);
					depth.hardlight(Window.TITLE_COLOR);
					level.hardlight(Window.TITLE_COLOR);
				}
			}

			layout();
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x;
			bg.y = y;
			bg.size(width, height);

			if (hero != null) {
				hero.x = x + 8;
				hero.y = y + (height - hero.height()) / 2f;
				align(hero);

				name.setPos(
						hero.x + hero.width() + 6,
						y + (height - name.height() - lastPlayed.height() - 2) / 2f
				);
				align(name);

				lastPlayed.setPos(
						hero.x + hero.width() + 6,
						name.bottom() + 2
				);

				classIcon.x = x + width - 24 + (16 - classIcon.width()) / 2f;
				classIcon.y = y + (height - classIcon.height()) / 2f;
				align(classIcon);

				level.x = classIcon.x + (classIcon.width() - level.width()) / 2f;
				level.y = classIcon.y + (classIcon.height() - level.height()) / 2f + 1;
				align(level);

				steps.x = x + width - 40 + (16 - steps.width()) / 2f;
				steps.y = y + (height - steps.height()) / 2f;
				align(steps);

				depth.x = steps.x + (steps.width() - depth.width()) / 2f;
				depth.y = steps.y + (steps.height() - depth.height()) / 2f + 1;
				align(depth);

			} else {
				name.setPos(
						x + (width - name.width()) / 2f,
						y + (height - name.height()) / 2f
				);
				align(name);
			}
		}

		@Override
		protected void onClick() {
			if (newGame) {
				GamesInProgress.selectedClass = null;
				GamesInProgress.curSlot = slot;
				WarpedPixelDungeon.switchScene(HeroSelectScene.class);
			} else {
				GamesInProgress.curSlot = slot;
				WarpedPixelDungeon.scene().add(new WndGameInProgress(slot));
			}
		}
	}
}
