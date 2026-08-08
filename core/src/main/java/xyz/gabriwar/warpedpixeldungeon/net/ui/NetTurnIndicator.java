package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.HealthBar;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;

import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * The one in-game multiplayer overlay: a compact party pane.
 *
 *   ┌──────────────────────────────┐
 *   │ HOSTING            2 watching│   ← header: mode badge + ping / spectators
 *   │ ▸ [🧙] Gabriel  ▂▂▂▂▂▂  (2) │   ← one row per hero: turn marker, class
 *   │   [🗡] Ana      ▂▂▂░░░       │     sprite, name, native HP bar, queued
 *   └──────────────────────────────┘     turn count
 *
 * Replaces the old trio of loose elements (bottom-left status text, floating
 * spectator banner, fixed-position party list): mode, turn state, ping and
 * spectator count all live in the header now. Party rows are hidden while
 * you're the only hero (a solo host's HP is already on the status pane).
 * Rows re-render only when the underlying signature changes; the header ping
 * refreshes on the same tick.
 */
public class NetTurnIndicator extends Component {

	private static final long REFRESH_MS = 400;

	private static final int ROW_H        = 14;
	private static final int CHEVRON_COL  = 8;
	private static final int ICON_COL     = 14;
	private static final int HP_BAR_W     = 28;
	private static final int PADDING_X    = 4;
	private static final int PADDING_Y    = 3;
	private static final int HEADER_H     = 10;
	// Cap how wide the name column gets — keeps the panel from ballooning when
	// someone has a long display name and keeps HP bars aligned across rows.
	private static final int NAME_COL_MAX = 72;

	private static final int COL_OTHER   = 0xCCCCCC;
	private static final int COL_AT_EXIT = 0x88BBFF;

	private NinePatch bg;
	private RenderedTextBlock modeText;
	private RenderedTextBlock infoText;

	private final ArrayList<Gizmo> rowItems = new ArrayList<>();
	private long lastRefresh = 0;
	private String lastSig = "";
	private boolean lastMyTurn = false;
	private float builtX = Float.NaN, builtY = Float.NaN;

	// When set, the panel keeps itself horizontally centered on anchorCX (its
	// width is only known after a rebuild, so it can't be centered from outside).
	private float anchorCX = -1;
	private float anchorTop = 0;

	/** Pin the panel centered under {@code centerX}, its top edge at {@code top}. */
	public void anchorTopCenter(float centerX, float top) {
		anchorCX = centerX;
		anchorTop = top;
	}

	@Override
	protected void createChildren() {
		bg = Chrome.get(Chrome.Type.TOAST_TR);
		add(bg);

		modeText = PixelScene.renderTextBlock(7);
		add(modeText);

		infoText = PixelScene.renderTextBlock(6);
		add(infoText);
	}

	@Override
	public void update() {
		super.update();
		if (!NetManager.isActive()) {
			visible = false;
			active  = false;
			return;
		}
		visible = true;
		active  = true;

		// React to turn handoff immediately — that's the one state change the
		// player is actively waiting on.
		boolean myTurn = NetManager.isPlayer() && NetManager.isMyTurn();
		if (myTurn != lastMyTurn) {
			lastMyTurn = myTurn;
			lastSig = ""; // header text/width changes, rebuild layout
		}
		if (myTurn) {
			modeText.alpha(0.7f + 0.3f * (float) Math.sin(Game.timeTotal * 6));
		} else {
			modeText.alpha(1f);
		}

		long now = System.currentTimeMillis();
		if (now - lastRefresh > REFRESH_MS) {
			lastRefresh = now;
			refresh();
		}
	}

	// ---- Row data ----

	private static class Row {
		final int id;
		final String name;
		final HeroClass cls;
		final int hp, ht;
		final int queued;
		final boolean local;
		final boolean atExit;

		Row(int id, String name, HeroClass cls, int hp, int ht,
				int queued, boolean local, boolean atExit) {
			this.id = id; this.name = name; this.cls = cls;
			this.hp = hp; this.ht = ht; this.queued = queued;
			this.local = local; this.atExit = atExit;
		}
	}

	private void refresh() {
		ArrayList<Row> entries = collect();
		// A lone hero has no party to show — the header alone carries the
		// mode. Solo host HP is already on the status pane.
		boolean showRows = entries.size() >= 2;

		String headerMode;
		int headerModeColor;
		String headerInfo = "";
		int headerInfoColor = NetUi.MUTED;

		long ping = NetManager.getPingMs();
		if (NetManager.isHost()) {
			headerMode = "HOSTING";
			headerModeColor = NetUi.GREEN;
			int specs = NetManager.getSpectatorCount();
			if (specs > 0) {
				headerInfo = specs + " watching";
				headerInfoColor = NetUi.BLUE;
			}
		} else if (NetManager.isPlayer()) {
			if (NetManager.isMyTurn()) {
				headerMode = "YOUR TURN";
				headerModeColor = NetUi.GREEN;
			} else {
				headerMode = "PLAYING";
				headerModeColor = NetUi.YELLOW;
			}
			if (ping >= 0) {
				headerInfo = ping + " ms";
				headerInfoColor = NetUi.pingColor(ping);
			}
		} else {
			String host = NetManager.getHostHeroName();
			headerMode = (host == null || host.isEmpty())
					? "SPECTATING" : "SPECTATING " + host;
			headerModeColor = NetUi.BLUE;
			if (ping >= 0) {
				headerInfo = ping + " ms";
				headerInfoColor = NetUi.pingColor(ping);
			}
		}

		StringBuilder sigB = new StringBuilder();
		sigB.append(headerMode).append('#').append(headerInfo).append('#');
		if (showRows) {
			for (Row r : entries) sigB.append(r.id).append('=').append(r.hp)
					.append('/').append(r.ht).append('q').append(r.queued)
					.append(r.atExit ? 'X' : '_').append(r.name).append('|');
			sigB.append('@').append(NetManager.activeHeroId);
		}
		String sig = sigB.toString();
		if (sig.equals(lastSig)) return;
		lastSig = sig;

		// Tear down previous rows
		for (Gizmo g : rowItems) {
			if (g != null) g.killAndErase();
		}
		rowItems.clear();

		modeText.text(headerMode);
		modeText.hardlight(headerModeColor);
		infoText.text(headerInfo);
		infoText.hardlight(headerInfoColor);
		infoText.visible = !headerInfo.isEmpty();

		int activeId = NetManager.activeHeroId;

		// PASS 1 — build name blocks with maxWidth applied up-front so width()
		// returns the actually-rendered (wrapped if needed) width. Then track
		// the widest one across all rows so HP bars line up at a single barX
		// instead of zig-zagging.
		ArrayList<RenderedTextBlock> nameBlocks = new ArrayList<>();
		float maxNameW = 0;
		if (showRows) {
			for (Row r : entries) {
				boolean isActive = r.id == activeId;
				int nameColor = r.atExit ? COL_AT_EXIT
						: isActive ? NetUi.GREEN
						: r.local ? NetUi.YELLOW
						: COL_OTHER;
				RenderedTextBlock name = PixelScene.renderTextBlock(
						r.name + (r.atExit ? " *" : ""), 6);
				name.hardlight(nameColor);
				name.maxWidth(NAME_COL_MAX);
				nameBlocks.add(name);
				maxNameW = Math.max(maxNameW, name.width());
			}
		}

		// Size is known now (before placing children) — so a top-centered panel
		// can settle its own x first and lay the rows out against it.
		float headerW = modeText.width()
				+ (infoText.visible ? infoText.width() + 8 : 0);
		float rowsW = showRows
				? CHEVRON_COL + ICON_COL + 2 + maxNameW + 4 + HP_BAR_W + 14
				: 0;
		float w = PADDING_X * 2 + Math.max(headerW, rowsW);
		float h = PADDING_Y + HEADER_H + (showRows ? entries.size() : 0) * ROW_H + PADDING_Y;

		if (anchorCX >= 0) {
			x = (int) (anchorCX - w / 2f);
			y = anchorTop;
		}
		builtX = x;
		builtY = y;

		float yCursor = PADDING_Y + HEADER_H;
		float nameX = x + CHEVRON_COL + ICON_COL + 2;
		float barX  = nameX + maxNameW + 4;

		// PASS 2 — actually place everything.
		if (showRows) for (int i = 0; i < entries.size(); i++) {
			Row r = entries.get(i);
			boolean isActive = r.id == activeId;

			if (isActive) {
				Image chev = Icons.get(Icons.CHEVRON);
				chev.hardlight(0.27f, 1f, 0.27f);
				chev.x = x + 2;
				chev.y = y + yCursor + (ROW_H - chev.height()) / 2f;
				PixelScene.align(chev);
				add(chev);
				rowItems.add(chev);
			}

			// Class icon — natural 12x15, vertically centered in the row.
			if (r.cls != null) {
				Image cls = new Image(r.cls.spritesheet(), 0, 90, 12, 15);
				cls.x = x + CHEVRON_COL + 1;
				cls.y = y + yCursor + (ROW_H - cls.height()) / 2f;
				PixelScene.align(cls);
				add(cls);
				rowItems.add(cls);
			}

			RenderedTextBlock name = nameBlocks.get(i);
			name.setPos(nameX, y + yCursor + (ROW_H - name.height()) / 2f);
			PixelScene.align(name);
			add(name);
			rowItems.add(name);

			// HP bar — the game's native red/green bar, only if we know HT.
			if (r.ht > 0) {
				HealthBar hpBar = new HealthBar();
				hpBar.level(Math.max(0, Math.min(1, r.hp / (float) r.ht)));
				hpBar.setRect(barX, y + yCursor + (ROW_H - hpBar.height()) / 2f,
						HP_BAR_W, hpBar.height());
				add(hpBar);
				rowItems.add(hpBar);
			}

			// Queued counter — only > 0, suppressed for atExit heroes.
			if (r.queued > 0 && !r.atExit) {
				RenderedTextBlock q = PixelScene.renderTextBlock("(" + r.queued + ")", 6);
				q.hardlight(COL_OTHER);
				q.setPos(barX + HP_BAR_W + 3, y + yCursor + (ROW_H - q.height()) / 2f);
				PixelScene.align(q);
				add(q);
				rowItems.add(q);
			}

			yCursor += ROW_H;
		}

		setSize(w, h);
	}

	private ArrayList<Row> collect() {
		ArrayList<Row> out = new ArrayList<>();

		// Local hero row. For host + player that's you; for a spectator the
		// mirrored hero belongs to the HOST, so label it with their name.
		if (Dungeon.hero != null) {
			String selfName;
			if (NetManager.isSpectator()) {
				selfName = NetManager.getHostHeroName();
				if (selfName == null || selfName.isEmpty()) selfName = "Host";
			} else {
				selfName = WPDSettings.multiplayerName();
				if (selfName == null || selfName.isEmpty()) {
					selfName = NetManager.isHost() ? "Host" : "You";
				}
			}
			int q = NetManager.isNetClient()
					? Dungeon.hero.queuedSteps
					: liveQueuedFor(Dungeon.hero);
			out.add(new Row(localHeroId(), selfName, Dungeon.hero.heroClass,
					Dungeon.hero.HP, Dungeon.hero.HT, q, !NetManager.isSpectator(),
					Dungeon.hero.atExit));
		}

		if (NetManager.isHost()) {
			for (Hero h : NetManager.allKnownNetHeroes()) {
				String n = (h.netOwnerName != null && !h.netOwnerName.isEmpty())
						? h.netOwnerName : "Player " + h.id();
				out.add(new Row(h.id(), n, h.heroClass, h.HP, h.HT,
						liveQueuedFor(h), false, h.atExit));
			}
		} else if (Dungeon.level != null) {
			for (Object b : new ArrayList<>(Dungeon.level.mobs)) {
				if (b instanceof SpectatorReceiver.NetHeroMob) {
					SpectatorReceiver.NetHeroMob nhm = (SpectatorReceiver.NetHeroMob) b;
					String n = (nhm.ownerName != null && !nhm.ownerName.isEmpty())
							? nhm.ownerName
							: (nhm.netName != null && !nhm.netName.isEmpty()
									? nhm.netName : "Player " + nhm.hostId);
					out.add(new Row(nhm.hostId, n, nhm.heroClass, nhm.HP, nhm.HT,
							nhm.queuedSteps, false, nhm.atExit));
				}
			}
		}
		return out;
	}

	/** Live queued-turn estimate for a host-side Hero. Move = ceil(distance/speed),
	 *  Attack = ceil(attackDelay), other actions = 1, idle = 0. */
	private static int liveQueuedFor(Hero h) {
		if (h.curAction == null || Dungeon.level == null || h.sprite == null) return 0;
		if (h.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Move) {
			int dst = ((xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Move)
					h.curAction).dst;
			int dist = Dungeon.level.distance(h.pos, dst);
			float speed = Math.max(0.1f, h.speed());
			return Math.max(0, (int) Math.ceil(dist / speed));
		}
		if (h.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Attack) {
			return Math.max(1, (int) Math.ceil(h.attackDelay()));
		}
		return 1;
	}

	private static int localHeroId() {
		if (NetManager.isNetClient()) {
			int id = NetManager.getMyNetHeroId();
			if (id != -1) return id;
		}
		return Dungeon.hero != null ? Dungeon.hero.id() : -1;
	}

	@Override
	protected void layout() {
		if (bg != null) {
			bg.x = x;
			bg.y = y;
			bg.size(width, height);
		}
		if (modeText != null) {
			modeText.setPos(x + PADDING_X, y + PADDING_Y);
			PixelScene.align(modeText);
		}
		if (infoText != null) {
			infoText.setPos(x + width - PADDING_X - infoText.width(),
					y + PADDING_Y + (modeText.height() - infoText.height()) / 2f);
			PixelScene.align(infoText);
		}
		// Row children carry absolute positions — if the pane itself moved,
		// force a rebuild. (Pure resizes from refresh() keep x/y and skip this.)
		if (x != builtX || y != builtY) {
			lastSig = "";
		}
	}
}
