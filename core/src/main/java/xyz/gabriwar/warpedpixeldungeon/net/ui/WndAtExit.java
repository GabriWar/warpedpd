package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/**
 * Modal shown when the local hero is parked on a transition cell waiting for
 * the rest of the party. Lists each party member with their class portrait,
 * name, and a "ready" check (✓) or "missing" mark.
 *
 * Modal — back/escape disabled. The two ways out are the Leave Exit button
 * and the host triggering the group descent (which clears atExit on every
 * hero and we get force-dismissed via WndAtExit.dismiss()).
 */
public class WndAtExit extends Window {

	private static final int WIDTH        = 152;
	private static final int MARGIN       = 4;
	private static final int BTN_HEIGHT   = 18;
	private static final int ROW_HEIGHT   = 18;

	private static final int COL_READY    = 0x44FF44;
	private static final int COL_WAITING  = 0xFFAA00;
	private static final int COL_MUTED    = 0x888888;

	private static WndAtExit current;

	private RenderedTextBlock summary;
	private ColorBlock progressBg;
	private ColorBlock progressFill;
	private RedButton btnLeave;
	private final ArrayList<RowVisuals> rows = new ArrayList<>();

	public WndAtExit() {
		super();

		float pos = MARGIN;

		// ---- Title ----
		RenderedTextBlock title = PixelScene.renderTextBlock("Standing on the stairs", 9);
		title.hardlight(COL_READY);
		title.maxWidth(WIDTH - MARGIN * 2);
		title.setPos((WIDTH - title.width()) / 2, pos);
		add(title);
		pos = title.bottom() + MARGIN;

		// ---- Body ----
		RenderedTextBlock body = PixelScene.renderTextBlock(
				"Waiting for the rest of the party to reach an exit. " +
				"You won't take any turns while you're here.",
				6);
		body.hardlight(0xCCCCCC);
		body.maxWidth(WIDTH - MARGIN * 2);
		body.setPos(MARGIN, pos);
		add(body);
		pos = body.bottom() + MARGIN * 2;

		// ---- Summary + progress bar ----
		summary = PixelScene.renderTextBlock("", 7);
		summary.hardlight(COL_WAITING);
		summary.setPos(MARGIN, pos);
		add(summary);
		pos = summary.bottom() + 2;

		progressBg = new ColorBlock(WIDTH - MARGIN * 2, 3, 0xFF222222);
		progressBg.x = MARGIN;
		progressBg.y = pos;
		add(progressBg);

		progressFill = new ColorBlock(0, 3, 0xFF44FF44);
		progressFill.x = MARGIN;
		progressFill.y = pos;
		add(progressFill);
		pos += 3 + MARGIN;

		// ---- Roster (built dynamically on first refresh) ----
		rosterStartY = pos;

		// ---- Leave button ----
		btnLeave = new RedButton("Leave the exit") {
			@Override
			protected void onClick() {
				hide();
				if (NetManager.isHost()) {
					NetManager.handleLeaveExit(Dungeon.hero);
				} else {
					NetManager.sendLeaveExit();
				}
			}
		};
		btnLeave.icon(Icons.get(Icons.CLOSE));
		// Initial pos — refresh() bumps it down once the roster is sized.
		btnLeave.setRect(MARGIN, rosterStartY, WIDTH - MARGIN * 2, BTN_HEIGHT);
		add(btnLeave);

		// First sizing pass — drives roster + repositions Leave button + resize().
		refreshLayout();
	}

	private long lastRefresh = 0;
	private String lastSig = "";

	@Override
	public void update() {
		super.update();
		long now = System.currentTimeMillis();
		if (now - lastRefresh < 250) return;
		lastRefresh = now;
		// Build a cheap signature so we skip the full row rebuild when nothing changed.
		StringBuilder sb = new StringBuilder();
		for (Member m : collect()) {
			sb.append(m.key).append(m.atExit ? '+' : '-').append('|');
		}
		String sig = sb.toString();
		if (sig.equals(lastSig)) return;
		lastSig = sig;
		refreshLayout();
	}

	private static class RowVisuals {
		Image portrait;
		Image checkIcon;
		RenderedTextBlock name;
		float baseY;
		String key;
	}

	private float rosterStartY;

	/** Rebuild the roster, reposition the Leave button, and resize the window
	 *  to fit. Safe to call repeatedly — diffs against existing rows by key. */
	private void refreshLayout() {
		ArrayList<Member> members = collect();

		int ready = 0;
		for (Member m : members) if (m.atExit) ready++;
		int total = members.size();

		summary.text("Party ready: " + ready + " / " + total);
		if (total > 0 && ready == total) {
			summary.hardlight(COL_READY);
			progressFill.hardlight(0xFF44FF44);
		} else {
			summary.hardlight(COL_WAITING);
			progressFill.hardlight(0xFFFFAA00);
		}
		progressFill.size(
				total == 0 ? 0 : (WIDTH - MARGIN * 2) * (ready / (float) total),
				3);

		// Reuse existing visual rows when keys match — avoids flicker.
		float y = rosterStartY;
		ArrayList<RowVisuals> next = new ArrayList<>();
		for (Member m : members) {
			RowVisuals row = findRow(m);
			if (row == null) row = newRow(m);
			row.baseY = y;
			placeRow(row, m, y);
			next.add(row);
			y += ROW_HEIGHT;
		}
		// Drop stale rows whose member is no longer present.
		for (RowVisuals stale : rows) {
			if (!next.contains(stale)) destroyRow(stale);
		}
		rows.clear();
		rows.addAll(next);

		// Leave button + window grow to fit the actual roster height.
		float btnY = y + MARGIN;
		btnLeave.setRect(MARGIN, btnY, WIDTH - MARGIN * 2, BTN_HEIGHT);
		resize(WIDTH, (int)(btnY + BTN_HEIGHT + MARGIN));
	}

	private RowVisuals findRow(Member m) {
		for (RowVisuals r : rows) if (m.key.equals(r.key)) return r;
		return null;
	}

	private RowVisuals newRow(Member m) {
		RowVisuals r = new RowVisuals();
		r.key = m.key;
		if (m.cls != null) {
			r.portrait = new Image(m.cls.spritesheet(), 0, 90, 12, 15);
			add(r.portrait);
		}
		r.name = PixelScene.renderTextBlock("", 7);
		add(r.name);
		r.checkIcon = Icons.get(Icons.UNCHECKED);
		add(r.checkIcon);
		return r;
	}

	private void placeRow(RowVisuals r, Member m, float y) {
		if (r.portrait != null) {
			r.portrait.x = MARGIN + 1;
			r.portrait.y = y + (ROW_HEIGHT - r.portrait.height()) / 2f;
			PixelScene.align(r.portrait);
		}
		String label = m.name;
		if (m.isYou) label += " (you)";
		r.name.text(label);
		r.name.hardlight(m.atExit ? COL_READY : COL_MUTED);
		r.name.setPos(MARGIN + 16, y + (ROW_HEIGHT - r.name.height()) / 2f);
		PixelScene.align(r.name);

		// swap CHECKED/UNCHECKED icon based on ready state
		Image want = Icons.get(m.atExit ? Icons.CHECKED : Icons.UNCHECKED);
		if (r.checkIcon != null) r.checkIcon.copy(want);
		if (r.checkIcon != null) {
			r.checkIcon.x = WIDTH - MARGIN - r.checkIcon.width();
			r.checkIcon.y = y + (ROW_HEIGHT - r.checkIcon.height()) / 2f;
			PixelScene.align(r.checkIcon);
		}
	}

	private void destroyRow(RowVisuals r) {
		if (r.portrait  != null) r.portrait.killAndErase();
		if (r.name      != null) r.name.killAndErase();
		if (r.checkIcon != null) r.checkIcon.killAndErase();
	}

	private static class Member {
		final String key;
		final String name;
		final HeroClass cls;
		final boolean atExit;
		final boolean isYou;

		Member(String key, String name, HeroClass cls, boolean atExit, boolean isYou) {
			this.key = key; this.name = name; this.cls = cls;
			this.atExit = atExit; this.isYou = isYou;
		}
	}

	private ArrayList<Member> collect() {
		ArrayList<Member> out = new ArrayList<>();
		// Local hero (you).
		if (Dungeon.hero != null && Dungeon.hero.isAlive()) {
			String myName;
			if (NetManager.isNetClient()) {
				myName = WPDSettings.multiplayerName();
				if (myName == null || myName.isEmpty()) myName = "You";
			} else {
				myName = WPDSettings.multiplayerName();
				if (myName == null || myName.isEmpty()) myName = "Host";
			}
			out.add(new Member("self", myName, Dungeon.hero.heroClass,
					Dungeon.hero.atExit, true));
		}
		if (NetManager.isHost()) {
			for (Hero h : NetManager.getNetHeroes()) {
				String n = (h.netOwnerName != null && !h.netOwnerName.isEmpty())
						? h.netOwnerName : ("Player " + h.id());
				out.add(new Member("nh-" + h.id(), n, h.heroClass, h.atExit, false));
			}
		} else if (Dungeon.level != null) {
			for (Object b : new ArrayList<>(Dungeon.level.mobs)) {
				if (b instanceof SpectatorReceiver.NetHeroMob) {
					SpectatorReceiver.NetHeroMob nhm = (SpectatorReceiver.NetHeroMob) b;
					String n = (nhm.ownerName != null && !nhm.ownerName.isEmpty())
							? nhm.ownerName
							: (nhm.netName != null && !nhm.netName.isEmpty()
									? nhm.netName : "Player " + nhm.hostId);
					out.add(new Member("nh-" + nhm.hostId, n, nhm.heroClass, nhm.atExit, false));
				}
			}
		}
		return out;
	}

	@Override
	public void onBackPressed() {
		// Modal — escape doesn't dismiss. Use Leave or wait for descent.
	}

	@Override
	public void hide() {
		if (current == this) current = null;
		super.hide();
	}

	@Override
	public void destroy() {
		// GameScene teardown (disconnect → switchScene(TitleScene)) cascades destroy()
		// down to child gizmos without calling hide(). Without this override `current`
		// stays pointing at the dead window, and the next MP session's show() guard
		// `if (current != null) return` blocks forever — the hero parks atExit with no UI.
		if (current == this) current = null;
		super.destroy();
	}

	/** Show the wnd (no-op if already up). Safe to call from any thread. */
	public static void show() {
		Game.runOnRenderThread(() -> {
			// `parent == null` covers any path where destroy() ran but `current` was
			// somehow left set (defensive — the destroy() override above is the primary fix).
			if (current != null && current.parent != null) return;
			current = null;
			if (!(Game.scene() instanceof GameScene)) return;
			current = new WndAtExit();
			Game.scene().addToFront(current);
		});
	}

	/** Force-close the wnd if open. Static name differs from Window.hide() —
	 *  Java forbids a static method with the same signature as an inherited
	 *  instance method. */
	public static void dismiss() {
		Game.runOnRenderThread(() -> {
			if (current != null) current.hide();
		});
	}
}
