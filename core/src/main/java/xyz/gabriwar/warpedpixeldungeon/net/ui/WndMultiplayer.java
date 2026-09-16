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

package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.Discovery;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollingListPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import xyz.gabriwar.warpedpixeldungeon.Chrome;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextInput;
import com.watabou.utils.DeviceCompat;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.net.relay.RelayJoin;
import xyz.gabriwar.warpedpixeldungeon.scenes.SupporterScene;
import xyz.gabriwar.warpedpixeldungeon.services.payments.Payments;

/**
 * Entry dialog for multiplayer, laid out like the game's other settings-style
 * windows (WndSettings): icon title, a name row, then Host and Join sections
 * split by separators. On landscape the two sections sit side by side.
 *
 *   Host — one button that opens the host lobby; the hint underneath carries
 *          your LAN IP for friends who need to type it manually.
 *   Join — auto-discovered LAN games (host class, floor, connection count)
 *          plus a manual address fallback.
 */
public class WndMultiplayer extends Window {

	private static final int WIDTH_P = 122;
	private static final int WIDTH_L = 223;

	private static final int BTN_HEIGHT = 18;
	private static final int GAP = 2;

	private final int wndWidth;

	private ScrollingListPane gameList;
	private RenderedTextBlock searchStatus;
	private float searchStatusX;
	private boolean listening;

	private RedButton btnName;

	// The libGDX-backed manual-IP field positions itself in absolute screen
	// space during layout(); it must be re-laid-out once resize() has settled
	// the window camera, or it lands away from its row. See end of constructor.
	private TextInput manualIpInput;
	private float ipInputX, ipInputY, ipInputW;

	private String lastGamesSig = "";
	private long lastListCheck = 0;
	private long animDot = 0;

	public WndMultiplayer() {
		super();

		wndWidth = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		float pos = 0;

		// ---- Title with mode badge when a session is already running ----
		IconTitle title = new IconTitle(Icons.CONTROLLER.get(), "Multiplayer");
		title.setRect(0, 0, wndWidth, 0);
		add(title);

		String statusText = null;
		int statusColor = NetUi.MUTED;
		if (NetManager.isHost())           { statusText = "HOSTING";    statusColor = NetUi.GREEN; }
		else if (NetManager.isPlayer())    { statusText = "PLAYING";    statusColor = NetUi.YELLOW; }
		else if (NetManager.isSpectator()) { statusText = "SPECTATING"; statusColor = NetUi.BLUE; }
		if (statusText != null) {
			RenderedTextBlock badge = PixelScene.renderTextBlock(statusText, 6);
			badge.hardlight(statusColor);
			badge.setPos(wndWidth - badge.width(), (title.bottom() - badge.height()) / 2f);
			add(badge);
		}
		pos = title.bottom() + GAP * 2;

		// ---- Name row (same pattern as the settings window) ----
		String currentName = WPDSettings.multiplayerName();
		btnName = new RedButton(currentName.isEmpty() ? "Set Name" : "Name: " + currentName, 8) {
			@Override
			protected void onClick() {
				WarpedPixelDungeon.scene().addToFront(
						new WndTextInput("Multiplayer Name",
								"Choose a name for other players to see.",
								WPDSettings.multiplayerName(),
								20, false, "OK", "Cancel") {
							@Override
							public void onSelect(boolean positive, String text) {
								if (positive && text != null && !text.trim().isEmpty()) {
									WPDSettings.multiplayerName(text.trim());
									btnName.text("Name: " + text.trim());
								}
							}
						}
				);
			}
		};
		btnName.icon(Icons.get(Icons.SCROLL_GREY));
		btnName.setRect(0, pos, wndWidth, 16);
		add(btnName);
		pos = btnName.bottom() + GAP * 2;

		pos = separator(pos);

		if (PixelScene.landscape()) {
			float colW = (wndWidth - 6) / 2f;
			float leftBottom  = buildHostSection(0, pos, colW);
			float rightBottom = buildJoinSection(colW + 6, pos, colW, 40);
			pos = Math.max(leftBottom, rightBottom);
		} else {
			pos = buildHostSection(0, pos, wndWidth);
			pos += GAP;
			pos = separator(pos);
			pos = buildJoinSection(0, pos, wndWidth, 46);
		}

		resize(wndWidth, (int) pos);

		// resize() re-centred the window camera; the manual-IP field cached its
		// on-screen position against the pre-resize camera, so place it again now.
		if (manualIpInput != null) {
			manualIpInput.setRect(ipInputX, ipInputY, ipInputW, 16);
		}

		if (NetManager.isHost()) {
			// Discovery is off while hosting — say so instead of "searching".
			searchStatus.text("You're hosting — stop hosting to join another game.");
		} else if (DeviceCompat.isiOS()) {
			// iOS can't receive LAN broadcasts, so games never appear here.
			// Joining is by direct address only.
			searchStatus.text("Type the host's address below to join.");
		} else {
			listening = true;
			Discovery.startListening();
		}
	}

	private float separator(float pos) {
		ColorBlock sep = new ColorBlock(wndWidth, 1, 0xFF000000);
		sep.y = pos;
		add(sep);
		return pos + 1 + GAP * 2;
	}

	private float buildHostSection(float x, float pos, float w) {
		RenderedTextBlock header = PixelScene.renderTextBlock("Host", 9);
		header.hardlight(TITLE_COLOR);
		header.setPos(x + (w - header.width()) / 2f, pos);
		add(header);
		pos = header.bottom() + GAP;

		RedButton btnHost = new RedButton("Host a Game", 9) {
			@Override
			protected void onClick() {
				hide();
				hostOnline = false;
				WarpedPixelDungeon.switchScene(LobbyScene.class);
			}
		};
		btnHost.icon(Icons.get(Icons.ENTER));
		btnHost.setRect(x, pos, w, BTN_HEIGHT);
		add(btnHost);
		pos = btnHost.bottom() + GAP;

		RedButton btnOnline = new RedButton("Host Online", 9) {
			@Override
			protected void onClick() {
				if (!canPlayOnline()) {
					offerSupporter();
					return;
				}
				hide();
				hostOnline = true;
				WarpedPixelDungeon.switchScene(LobbyScene.class);
			}
		};
		btnOnline.icon(Icons.get(Icons.CHALLENGE_COLOR));
		btnOnline.setRect(x, pos, w, BTN_HEIGHT);
		add(btnOnline);
		pos = btnOnline.bottom() + GAP;

		// Muted hint with the IP picked out in green — colors must be set
		// before text(), as hardlight() recolors highlighted words too.
		RenderedTextBlock hint = PixelScene.renderTextBlock(6);
		hint.setHightlighting(true, NetUi.GREEN);
		hint.hardlight(NetUi.MUTED);
		hint.maxWidth((int) w);
		if (DeviceCompat.isiOS()) {
			// An iOS host doesn't broadcast, so it's never auto-discovered — friends
			// must type this address in.
			hint.text("Share your address so friends can join: _"
					+ Discovery.getLocalIP() + "_. _Host Online_ works anywhere with a room code, for supporters.");
		} else {
			hint.text("Players on your LAN find your game automatically, or can type your address: _"
					+ Discovery.getLocalIP() + "_. _Host Online_ works anywhere with a room code, for supporters.");
		}
		hint.setPos(x + (w - hint.width()) / 2f, pos);
		add(hint);
		return hint.bottom() + GAP;
	}

	private float buildJoinSection(float x, float pos, float w, int listHeight) {
		RenderedTextBlock header = PixelScene.renderTextBlock("Join", 9);
		header.hardlight(TITLE_COLOR);
		header.setPos(x + (w - header.width()) / 2f, pos);
		add(header);
		pos = header.bottom() + GAP;

		searchStatus = PixelScene.renderTextBlock("Searching for LAN games", 6);
		searchStatus.hardlight(NetUi.MUTED);
		searchStatus.maxWidth((int) w);
		searchStatusX = x;
		searchStatus.setPos(x, pos);
		add(searchStatus);
		pos = searchStatus.bottom() + GAP;

		gameList = new ScrollingListPane();
		add(gameList);
		gameList.setRect(x, pos, w, listHeight);
		pos += listHeight + GAP;

		int textSize = (int) PixelScene.uiCamera.zoom * 9;
		manualIpInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, textSize);
		manualIpInput.setMaxLength(21);
		manualIpInput.setText("");
		ipInputX = x;
		ipInputY = pos;
		ipInputW = w - 42;
		manualIpInput.setRect(ipInputX, ipInputY, ipInputW, 16);
		add(manualIpInput);

		RedButton btnConnect = new RedButton("Join", 8) {
			@Override
			protected void onClick() {
				String input = manualIpInput.getText().trim();
				if (input.isEmpty()) return;
				//one field for both: six characters from the room alphabet is a code,
				//anything else is an address. A player should not have to know which
				//box their invitation belongs in.
				String code = RelayJoin.normalise(input);
				if (code != null) {
					connectOnline(code);
				} else {
					connectTo(input);
				}
			}
		};
		btnConnect.setRect(x + w - 40, pos, 40, 16);
		add(btnConnect);
		pos += 16 + GAP;

		RenderedTextBlock codeHint = PixelScene.renderTextBlock(
				"An address joins a game on this network, always free. A _six-character room code_ joins one anywhere, for supporters.", 6);
		codeHint.hardlight(NetUi.MUTED);
		codeHint.setHightlighting(true, NetUi.GREEN);
		codeHint.maxWidth((int) w);
		codeHint.setPos(x, pos);
		add(codeHint);
		return codeHint.bottom();
	}

	/**
	 * Online play runs through a relay the project pays for, so it is what a supporter
	 * subscription buys. Playing with someone on the same network never touches it and is
	 * always free — including in builds with no store at all.
	 */
	static boolean canPlayOnline() {
		//a build with no store cannot sell a subscription, so a developer testing online
		//play would be stopped by a door they can never open. The relay's own test secret
		//opens it instead - the relay is what actually checks it, and refuses anything
		//that is not the token it was started with.
		//
		//This branch cannot reach a released build: isDebug() is the -INDEV version
		//suffix, which only a debug build carries, and the token is empty in every
		//shipped copy anyway
		if (DeviceCompat.isDebug() && !WPDSettings.relayDevToken().isEmpty()) {
			return true;
		}
		return Payments.receipt() != null;
	}

	private void offerSupporter() {
		com.watabou.noosa.Scene current = Game.scene();
		if (current == null) return;
		String reason = Payments.service == null
				? "Connecting distant players runs through a server the game pays for, and this build has no store to subscribe through. Playing with someone on your own network works here and is always free."
				: "Playing with a distant friend runs through a server the game pays for every month, so it comes with being a supporter. Playing with someone on your own network is always free.";
		current.addToFront(new WndOptions(
				Icons.GOLD.get(),
				"Play with distant friends",
				reason,
				Payments.service == null ? "OK" : "See supporter tiers",
				"Not now"
		) {
			@Override
			protected void onSelect(int index) {
				if (index == 0 && Payments.service != null) {
					WndMultiplayer.this.hide();
					WarpedPixelDungeon.switchScene(SupporterScene.class);
				}
			}
		});
	}

	private void connectOnline(String code) {
		if (!canPlayOnline()) {
			offerSupporter();
			return;
		}
		hide();
		com.watabou.noosa.Scene current = Game.scene();
		if (current == null) return;
		current.addToFront(new WndOptions(
				Icons.CONTROLLER.get(),
				"Room " + code,
				"How do you want to join this game?",
				"Play (control your own hero)",
				"Spectate (watch only)"
		) {
			@Override
			protected void onSelect(int index) {
				Discovery.stopAll();
				if (index == 0) {
					pendingHostIP = null;
					pendingRoomCode = code;
					WarpedPixelDungeon.switchScene(PlayerLobbyScene.class);
				} else {
					NetManager.startSpectatorOnline(code);
				}
			}
		});
	}

	private void connectTo(String ip) {
		hide();
		com.watabou.noosa.Scene current = Game.scene();
		if (current != null) {
			current.addToFront(new WndOptions(
					Icons.CONTROLLER.get(),
					"Join " + ip,
					"How do you want to join this game?",
					"Play (control your own hero)",
					"Spectate (watch only)"
			) {
				@Override
				protected void onSelect(int index) {
					Discovery.stopAll();
					if (index == 0) {
						pendingHostIP = ip;
						pendingRoomCode = null;
						WarpedPixelDungeon.switchScene(PlayerLobbyScene.class);
					} else {
						NetManager.startSpectator(ip);
					}
				}
			});
		}
	}

	public static String pendingHostIP = null;
	/** Non-null when the next lobby should join through the relay instead of a LAN dial. */
	public static String pendingRoomCode = null;
	/** Set by "Host Online" so LobbyScene opens a relay room instead of a local port. */
	public static boolean hostOnline = false;

	@Override
	public void update() {
		super.update();
		if (!listening) return;

		long now = System.currentTimeMillis();

		// Animated search dots while the list is empty — visual confirmation
		// that discovery is still running (same idiom as the spectator lobby).
		if (lastGamesSig.isEmpty() && now - animDot > 400) {
			animDot = now;
			int dots = (int) ((now / 400) % 4);
			StringBuilder sb = new StringBuilder("Searching for LAN games");
			for (int i = 0; i < dots; i++) sb.append('.');
			searchStatus.text(sb.toString());
		}

		if (now - lastListCheck < 500) return;
		lastListCheck = now;

		// Rebuild only when a game appears/disappears or its metadata changes.
		StringBuilder sigB = new StringBuilder();
		for (Discovery.FoundGame g : Discovery.getFoundGames().values()) {
			sigB.append(g.ip).append('|').append(g.depth).append('|')
					.append(g.heroClass).append('|').append(g.spectators).append('#');
		}
		String sig = sigB.toString();
		if (!sig.equals(lastGamesSig)) {
			lastGamesSig = sig;
			rebuildGameList();
		}
	}

	private void rebuildGameList() {
		ArrayList<Discovery.FoundGame> games = new ArrayList<>(Discovery.getFoundGames().values());

		gameList.clear();

		if (games.isEmpty()) {
			searchStatus.text("Searching for LAN games");
			searchStatus.hardlight(NetUi.MUTED);
		} else {
			searchStatus.text(games.size() + (games.size() > 1 ? " games found" : " game found")
					+ " — tap to join");
			searchStatus.hardlight(NetUi.BLUE);
		}
		searchStatus.setPos(searchStatusX, searchStatus.top());

		for (Discovery.FoundGame game : games) {
			gameList.addItem(new GameRow(game));
		}
	}

	/** Two-line discovery row: address on top, floor · class · watchers under it. */
	private class GameRow extends ScrollingListPane.ListItem {

		private final String ip;
		private RenderedTextBlock sub;

		GameRow(Discovery.FoundGame game) {
			super(classIcon(game.heroClass), null, game.ip);

			ip = game.ip;

			StringBuilder s = new StringBuilder();
			if (game.depth > 0) {
				s.append("Floor ").append(game.depth);
				if (game.heroClass != null && !game.heroClass.equals("?")) {
					s.append(" · ").append(prettyClass(game.heroClass));
				}
			}
			if (game.spectators > 0) {
				if (s.length() > 0) s.append(" · ");
				s.append(game.spectators).append(" online");
			}
			if (s.length() == 0) s.append("In the lobby");

			sub = PixelScene.renderTextBlock(s.toString(), 6);
			sub.hardlight(NetUi.MUTED);
			add(sub);
		}

		@Override
		protected void layout() {
			super.layout();
			if (sub != null) {
				label.setPos(x + 17, y + 2);
				PixelScene.align(label);
				sub.maxWidth((int) (width - 17));
				sub.setPos(x + 17, label.bottom() + 1);
				PixelScene.align(sub);
			}
		}

		@Override
		public boolean onClick(float x, float y) {
			if (inside(x, y)) {
				connectTo(ip);
				return true;
			}
			return false;
		}
	}

	// Map a host's class string back to the right class icon. Falls back to
	// ENTER. Don't cache — each ListItem takes ownership of its Image instance.
	private static Image classIcon(String heroClass) {
		HeroClass hc = parseClass(heroClass);
		return hc == null ? Icons.get(Icons.ENTER) : Icons.get(hc);
	}

	private static HeroClass parseClass(String s) {
		if (s == null) return null;
		String upper = s.toUpperCase();
		for (HeroClass c : HeroClass.values()) {
			if (c.name().startsWith(upper) || upper.startsWith(c.name())) return c;
		}
		return null;
	}

	private static String prettyClass(String s) {
		HeroClass hc = parseClass(s);
		if (hc != null) return Messages.titleCase(hc.title());
		return s;
	}

	@Override
	public void destroy() {
		if (listening && !NetManager.isSpectator()) {
			Discovery.stopListening();
		}
		super.destroy();
	}
}
