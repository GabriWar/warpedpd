package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import com.watabou.noosa.Game;

import org.json.JSONObject;

import com.watabou.noosa.particles.Emitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class NetManager {

	public static final int PORT = 38471;

	/** Per-event chatty logging from the net layer (Actor pick, hero act, deltas, sprite link).
	 *  Debug builds only: in a release it is pure noise on stdout and frame time. */
	public static final boolean NET_DEBUG = com.watabou.utils.DeviceCompat.isDebug();

	public static void log(String msg) {
		if (NET_DEBUG) System.out.println(msg);
	}

	public enum NetMode { OFF, HOST, SPECTATOR, PLAYER }

	private static volatile NetMode mode = NetMode.OFF;
	private static HostServer server;
	private static SpectatorClient client;
	private static SpectatorReceiver receiver;
	private static String hostHeroName = "";
	private static boolean hostInGame = false;

	// --- N-Player Hero Management (HOST side) ---
	// Maps each player client to their Hero instance on the host
	private static final ConcurrentHashMap<HostServer.ClientConnection, Hero> netHeroes = new ConcurrentHashMap<>();

	// Heroes whose owner is currently disconnected, keyed by netOwnerName.
	// Populated by (a) onClientDisconnected when a player drops, (b) Level.restoreFromBundle
	// when a save is loaded with persisted netHeroes. Drained by handleJoinAsPlayer when
	// a player reconnects with a matching name.
	private static final ConcurrentHashMap<String, Hero> unclaimedHeroes = new ConcurrentHashMap<>();

	// Session tokens — keyed by netOwnerName. Issued on first fresh spawn; required on reclaim.
	// Prevents any LAN peer who knows a disconnected player's display name from hijacking their hero.
	private static final ConcurrentHashMap<String, String> sessionTokens = new ConcurrentHashMap<>();

	// Client-side session token received from host. Included in JOIN_AS_PLAYER on reconnect.
	private static volatile String clientSessionToken = null;
	// ...and the same tokens kept per (host, name), because stop() clears the live
	// one: going back to the lobby and joining again is a manual reconnect, and
	// without the token the host answers token_mismatch on the player's own hero
	private static final ConcurrentHashMap<String, String> clientTokens = new ConcurrentHashMap<>();
	private static volatile String currentHostAddress = null;

	private static String tokenKey(String host, String name) {
		return (host == null ? "" : host) + "/" + (name == null ? "" : name);
	}

	public static void storeClientSessionToken(String token) {
		clientSessionToken = token;
		if (client != null) client.setSessionToken(token);
		if (token != null && !token.isEmpty()) {
			clientTokens.put( tokenKey( currentHostAddress,
					xyz.gabriwar.warpedpixeldungeon.WPDSettings.multiplayerName() ), token );
		}
	}

	// When non-(-1), overrides the entrance as the relocation anchor for netHeroes on
	// the next depth change. Used by fast-travel portals so party members land next to
	// the destination gate instead of the floor's normal entrance. Cleared after use.
	private static volatile int pendingRelocationAnchor = -1;
	public static void setPendingRelocationAnchor(int cell) { pendingRelocationAnchor = cell; }

	// Serializes the claim+put compound op so two reconnects with the same name can't
	// both pass through unclaimedHeroes.remove() and double-bind the same Hero.
	private static final Object CLAIM_LOCK = new Object();

	// --- Client side: my hero's host actor ID ---
	private static int myNetHeroId = -1;
	private static volatile boolean myTurn = false;

	public static NetMode getMode() { return mode; }
	public static boolean isHost() { return mode == NetMode.HOST; }
	public static boolean isSpectator() { return mode == NetMode.SPECTATOR; }
	public static boolean isPlayer() { return mode == NetMode.PLAYER; }
	public static boolean isNetClient() { return mode == NetMode.SPECTATOR || mode == NetMode.PLAYER; }
	public static boolean isActive() { return mode != NetMode.OFF; }

	// --- Host: start server ---

	public static void startHost() throws IOException {
		stop();
		server = new HostServer(PORT);

		server.setOnClientConnected(c -> {
			if (hostInGame) {
				// If we're between floors (InterlevelScene), Dungeon.level / Dungeon.hero
				// may still reference the dying floor. Sending FULL_STATE now would land
				// stale map/mob data on the new client; the post-transition broadcast
				// (broadcastLevelChange) re-FULL_STATEs every connected client anyway,
				// so just send lobby info and let the natural transition end catch them up.
				if (!(Game.scene() instanceof GameScene)
						|| Dungeon.level == null
						|| Dungeon.hero == null) {
					try {
						JSONObject lobbyData = new JSONObject();
						lobbyData.put("hostName", xyz.gabriwar.warpedpixeldungeon.WPDSettings.multiplayerName());
						lobbyData.put("transitioning", true);
						c.sendMessage(Protocol.LOBBY_INFO, lobbyData);
					} catch (Exception ignored) {}
					Game.runOnRenderThread(() -> GLog.p("A client connected during transition; will sync shortly."));
					return;
				}
				JSONObject state = StateSerializer.serializeFullState();
				if (state != null) {
					try {
						String heroName = Dungeon.hero != null ? Dungeon.hero.name() : "Hero";
						JSONObject nameData = new JSONObject();
						nameData.put("name", heroName);
						c.sendMessage(Protocol.HERO_NAME, nameData);
					} catch (Exception ignored) {}
					c.sendMessage(Protocol.FULL_STATE, state);
				}
			} else {
				try {
					JSONObject lobbyData = new JSONObject();
					lobbyData.put("hostName", xyz.gabriwar.warpedpixeldungeon.WPDSettings.multiplayerName());
					c.sendMessage(Protocol.LOBBY_INFO, lobbyData);
				} catch (Exception ignored) {}
			}
			Game.runOnRenderThread(() -> GLog.p("A client connected! (" + getClientCount() + " connected)"));
		});

		server.setOnClientDisconnected(c -> {
			Hero removed = netHeroes.remove(c);
			if (removed != null) {
				// Demote to unclaimed instead of destroying. Hero stays on the level
				// (saved with the dungeon bundle) so reconnect can claim it back —
				// items, lvl, exp, pos, buffs all preserved. Killing the sprite here
				// keeps the host scene tidy; act() will lazy-readd on reconnect.
				if (removed.netOwnerName != null && !removed.netOwnerName.isEmpty()) {
					unclaimedHeroes.put(removed.netOwnerName, removed);
				}
				// Clear any pending input — owner won't be sending any more — and reset
				// the wait flag so re-entry into act() sees a clean state. Hold the same
				// lock the actor thread uses to snapshot the tuple, so it can't read a
				// half-cleared (type, item, extra, cell) mix.
				synchronized (removed) {
					removed.pendingActionCell = -1;
					removed.pendingActionType = null;
					removed.pendingActionItem = null;
					removed.pendingActionItemAction = null;
					removed.pendingActionExtra = -1;
					removed.pendingDialogId = -1;
					removed.pendingDialogChoice = null;
					removed.remoteWaiting = false;
				}
				NetDialogs.forgetHero(removed.id());
				Game.runOnRenderThread(() -> {
					if (removed.sprite != null) removed.sprite.killAndErase();
					GLog.w("A player disconnected (hero stashed for rejoin).");
				});
				// Wake the actor thread — if this hero was paused waiting for input,
				// the next act() will see !claimed and NPC-skip via spendAndNext(TICK),
				// unblocking the host's turn.
				wakeActorThread();
				// At-exit gate: a disconnect can change the ready/total ratio. If the
				// remaining online heroes are all parked at an exit, fire the descent.
				if (countAtExit() >= countOnlineClaimedHeroes() && countOnlineClaimedHeroes() > 0) {
					triggerGroupDescent();
				}
			} else {
				Game.runOnRenderThread(() -> GLog.w("A client disconnected. (" + getClientCount() + " connected)"));
			}
		});

		// Route messages from clients
		server.setOnClientMessage((c, msg) -> {
			switch (msg.type) {
				case Protocol.JOIN:
					c.connectionRole = HostServer.ClientConnection.ROLE_SPECTATOR;
					break;
				case Protocol.JOIN_AS_PLAYER:
					if (c.connectionRole == HostServer.ClientConnection.ROLE_SPECTATOR) {
						try {
							JSONObject err = new JSONObject();
							err.put("error", "already_spectator");
							c.sendMessage(Protocol.LOBBY_INFO, err);
						} catch (Exception ignored) {}
						break;
					}
					handleJoinAsPlayer(c, msg.data);
					break;
				case Protocol.PLAYER_ACTION:
					handlePlayerAction(c, msg.data);
					break;
				case Protocol.DIALOG_CHOICE:
					handleDialogChoice(c, msg.data);
					break;
				case Protocol.PEEK_CHARACTER:
					handlePeekCharacter(c, msg.data);
					break;
			}
		});

		server.start();
		mode = NetMode.HOST;
		StateSerializer.resetDeltaTracking();

		Emitter.netListener = new Emitter.NetEmitterListener() {
			@Override
			public void onBurst(int cell, String emType, Emitter.Factory factory, int count) {
				NetVisuals.recordCellBurst(cell, emType, factory, count);
			}
			@Override
			public void onStart(int cell, String emType, Emitter.Factory factory, float interval, int count) {
				NetVisuals.recordCellStart(cell, emType, factory, interval, count);
			}
		};

		Discovery.startBroadcasting();
	}

	// --- Host: handle JOIN_AS_PLAYER ---

	private static void handleJoinAsPlayer(HostServer.ClientConnection client, JSONObject data) {
		if (!hostInGame || Dungeon.level == null || Dungeon.hero == null) {
			// Can't join yet — send lobby info
			try {
				JSONObject lobbyData = new JSONObject();
				lobbyData.put("hostName", xyz.gabriwar.warpedpixeldungeon.WPDSettings.multiplayerName());
				client.sendMessage(Protocol.LOBBY_INFO, lobbyData);
			} catch (Exception ignored) {}
			return;
		}

		Game.runOnRenderThread(() -> {
			try {
				int clsOrdRaw = data.optInt("cls", 0);
				// Bounds-clamp the heroClass ordinal — a malformed/malicious client could otherwise
				// crash the host with AIOOBE on HeroClass.values()[clsOrd].
				HeroClass[] hcAll = HeroClass.values();
				int clsOrd = (clsOrdRaw < 0 || clsOrdRaw >= hcAll.length) ? 0 : clsOrdRaw;
				HeroClass hc = hcAll[clsOrd];
				String playerName = data.optString("name", "").trim();
				if (playerName.isEmpty()) {
					//a nameless hero can never be stashed (the disconnect handler keys
					//by name) nor authenticated: refuse rather than mint an orphan
					try {
						JSONObject err = new JSONObject();
						err.put("error", "name_required");
						client.sendMessage(Protocol.LOBBY_INFO, err);
					} catch (Exception ignored) {}
					log("[NET-HOST] Refused JOIN_AS_PLAYER with an empty name");
					return;
				}

				Hero netHero = null;
				boolean reclaimed = false;

				// Claim + spawn under CLAIM_LOCK so two reconnects racing on the same name
				// can't both pass the unclaimedHeroes.remove() and double-bind one Hero.
				// Also rejects spoofing: if another client is currently playing under this
				// name (active in netHeroes), reject the claim and force a different identity.
				String sessionToken = null;
				synchronized (CLAIM_LOCK) {
					if (!playerName.isEmpty()) {
						// Reject if name is already actively in use by another live client.
						for (java.util.Map.Entry<HostServer.ClientConnection, Hero> e : netHeroes.entrySet()) {
							if (e.getKey() != client
									&& playerName.equals(e.getValue().netOwnerName)) {
								xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
										"[NET-HOST] Rejecting claim of in-use name=" + playerName
												+ " — another client already owns it");
								try {
									JSONObject err = new JSONObject();
									err.put("error", "name_in_use");
									client.sendMessage(Protocol.LOBBY_INFO, err);
								} catch (Exception ignored) {}
								return;
							}
						}

						netHero = unclaimedHeroes.remove(playerName);
						if (netHero != null) {
							// Verify session token — prevents any LAN peer from claiming a
							// disconnected player's hero by guessing their display name.
							//the live session map dies with the process; the hero's own
							//token is the one that survived the save
							String storedToken = sessionTokens.get(playerName);
							if (storedToken == null && netHero.netSessionToken != null
									&& !netHero.netSessionToken.isEmpty()) {
								storedToken = netHero.netSessionToken;
							}
							if (storedToken != null) {
								String providedToken = data.optString("token", "");
								if (!storedToken.equals(providedToken)) {
									xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
											"[NET-HOST] Token mismatch for name=" + playerName + " — rejecting claim");
									unclaimedHeroes.put(playerName, netHero); // put back
									try {
										JSONObject err = new JSONObject();
										err.put("error", "token_mismatch");
										client.sendMessage(Protocol.LOBBY_INFO, err);
									} catch (Exception ignored) {}
									return;
								}
							}
							if (storedToken == null) {
								//a hero from before tokens existed: this first claim
								//is taken on trust, and mints the secret every later
								//one has to prove
								storedToken = java.util.UUID.randomUUID().toString();
								log("[NET-HOST] Tokenless hero name=" + playerName
										+ " - issuing a token on first claim");
							}
							sessionToken = storedToken; // carry forward for ack
							sessionTokens.put(playerName, sessionToken);
							netHero.netSessionToken = sessionToken;
							reclaimed = true;
							netHero.isRemote = true;
							netHero.remoteWaiting = false;
							netHero.pendingActionCell = -1;
							xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] Reclaimed netHero name=" + playerName
									+ " id=" + netHero.id() + " cls=" + netHero.heroClass.title()
									+ " pos=" + netHero.pos
									+ " items=" + netHero.belongings.backpack.items.size());
						}
					}

				if (netHero == null) {
					// Fresh spawn — ignore all client-supplied stats; host is authoritative.
					netHero = new Hero();
					netHero.heroClass = hc;
					// HP=HT=20, STR=10 set by Hero() constructor; lvl=1, exp=0 by default fields.
					netHero.isRemote = true;
					netHero.netOwnerName = playerName;
					// Issue a session token so reconnects can prove identity.
					sessionToken = java.util.UUID.randomUUID().toString();
					sessionTokens.put(playerName, sessionToken);
					//saved with the hero, so a host reload cannot strip its lock
					netHero.netSessionToken = sessionToken;

					// Class init — gives armor, food, pouch, waterskin, scroll, class kit, talents.
					// HeroClass.initHero() and the class-specific initWarrior/initRogue/etc reach
					// for Dungeon.hero internally via Item.collect() and quickslot calls. Temp-swap
					// so those resolve to the netHero. Also save/restore the host quickslots since
					// initHero may modify the global Dungeon.quickslot.
					Hero savedDungeonHero = Dungeon.hero;
					Item[] savedQs = new Item[xyz.gabriwar.warpedpixeldungeon.QuickSlot.SIZE];
					for (int s = 0; s < savedQs.length; s++) savedQs[s] = Dungeon.quickslot.getItem(s);
					try {
						Dungeon.hero = netHero;
						Dungeon.quickslot.reset();
						hc.initHero(netHero);
					} finally {
						Dungeon.hero = savedDungeonHero;
						Dungeon.quickslot.reset();
						for (int s = 0; s < savedQs.length; s++) {
							if (savedQs[s] != null) Dungeon.quickslot.setSlot(s, savedQs[s]);
						}
					}

					// Spawn beside the host, never via Level.entrance(). On the overworld
					// entrance() -> getTransition(null) has side effects (it eats the pending
					// arrival stamp and can re-centre the streaming window), so a join landing
					// mid-travel would steal the host's arrival cell. Next to the party is also
					// where a joining player wants to be — the entrance can be a whole window away.
					netHero.pos = findEmptyCell(Dungeon.hero.pos);
					Dungeon.level.occupyCell(netHero);
					Actor.add(netHero);
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] Spawned new netHero id=" + netHero.id()
							+ " name=" + playerName + " cls=" + hc.title()
							+ " pos=" + netHero.pos
							+ " items=" + netHero.belongings.backpack.items.size());
				}

					netHeroes.put(client, netHero);
				client.connectionRole = HostServer.ClientConnection.ROLE_PLAYER;
				} // end synchronized (CLAIM_LOCK)

				if (Game.scene() instanceof GameScene) {
					GameScene.addNetHero(netHero);
				}

				// Ack w/ the hero's actor ID so client knows which is theirs.
				// Include session token so client can authenticate future reconnects.
				JSONObject ack = new JSONObject();
				ack.put("heroId", netHero.id());
				ack.put("ack", true);
				if (sessionToken != null) ack.put("token", sessionToken);
				client.sendMessage(Protocol.YOUR_TURN, ack);

				broadcastLevelChange();
				GLog.p((reclaimed ? "Player reconnected as " : "Player joined as ")
						+ netHero.heroClass.title() + "!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/** Called from Hero.act() to check if a remote hero has an active client owner. */
	public static boolean isClaimedNetHero(Hero h) {
		return h != null && netHeroes.containsValue(h);
	}

	/**
	 * Called from Level.restoreFromBundle when a save with persisted netHeroes is loaded.
	 * Re-marks them as remote and stashes them under their owner name so reconnecting
	 * players can claim back their hero.
	 */
	public static void registerLoadedNetHero(Hero h) {
		if (h == null) return;
		h.isRemote = true;
		h.remoteWaiting = false;
		h.pendingActionCell = -1;
		// atExit is `transient`, so a save taken mid-transition has already lost the flag —
		// the field will already be false here. Reset explicitly anyway in case any deeper
		// path (custom save format, future serializer) ever leaves stale state.
		h.atExit = false;
		h.atExitCell = -1;
		if (h.netOwnerName != null && !h.netOwnerName.isEmpty()) {
			//the token it was saved with goes back into the live map, so the claim
			//check has something to verify against after a host restart
			if (h.netSessionToken != null && !h.netSessionToken.isEmpty()) {
				sessionTokens.putIfAbsent(h.netOwnerName, h.netSessionToken);
			}
			synchronized (CLAIM_LOCK) {
				// Don't silently overwrite a hero that's already been claimed by an
				// active connection or stashed earlier under the same name.
				if (netHeroes.values().stream().anyMatch(nh -> h.netOwnerName.equals(nh.netOwnerName))) {
					log("[NET-HOST] Loaded netHero collides with active claim name=" + h.netOwnerName
							+ " — dropping the loaded copy");
					return;
				}
				Hero existing = unclaimedHeroes.putIfAbsent(h.netOwnerName, h);
				if (existing != null && existing != h) {
					log("[NET-HOST] Loaded netHero collides with stashed name=" + h.netOwnerName
							+ " existingId=" + existing.id() + " newId=" + h.id()
							+ " — keeping existing");
					return;
				}
			}
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] Loaded netHero from save name=" + h.netOwnerName
					+ " id=" + h.id() + " pos=" + h.pos);
		}
	}

	/** Heroes (claimed + unclaimed) for level save serialization. */
	public static java.util.Collection<Hero> allKnownNetHeroes() {
		java.util.HashSet<Hero> all = new java.util.HashSet<>(netHeroes.values());
		all.addAll(unclaimedHeroes.values());
		return all;
	}

	// --- Host: non-blocking remote turn signaling ---

	/** Called from Hero.act() on host when remote hero's turn starts. Sends YOUR_TURN once. */
	public static void signalRemoteTurn(Hero hero) {
		HostServer.ClientConnection owner = null;
		for (var entry : netHeroes.entrySet()) {
			if (entry.getValue() == hero) { owner = entry.getKey(); break; }
		}
		if (owner == null) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] signalRemoteTurn: no owner for hero " + hero.id());
			return;
		}
		try {
			JSONObject turnData = new JSONObject();
			turnData.put("heroId", hero.id());
			turnData.put("pos", hero.pos);
			owner.sendMessage(Protocol.YOUR_TURN, turnData);
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] YOUR_TURN sent for hero " + hero.id() + " pos=" + hero.pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Ensure client has latest state
		broadcastGameState();
	}

	/** Lobby preview: client asks "do you have a stashed hero for this name?".
	 *  Host responds with a summary so the lobby can show "Continue as <class> Lvl X"
	 *  vs "Fresh start". No claim happens here — just a peek. */
	private static void handlePeekCharacter(HostServer.ClientConnection client, JSONObject data) {
		String name = data.optString("name", "");
		Hero h = null;
		if (!name.isEmpty()) {
			h = unclaimedHeroes.get(name);
			if (h == null) {
				// Already-active hero (player is rejoining w/ same name as their live session)
				for (Hero nh : netHeroes.values()) {
					if (name.equals(nh.netOwnerName)) { h = nh; break; }
				}
			}
		}
		try {
			JSONObject resp = new JSONObject();
			if (h != null) {
				resp.put("found", true);
				resp.put("cls", h.heroClass.ordinal());
				resp.put("lvl", h.lvl);
				//the rest is the character sheet, and a peek is unauthenticated:
				//any peer on the network can ask. only the owner - who can show
				//the token this hero was issued - gets the details
				String expect = sessionTokens.get(name);
				if (expect == null && h.netSessionToken != null && !h.netSessionToken.isEmpty()) {
					expect = h.netSessionToken;
				}
				if (expect != null && expect.equals(data.optString("token", ""))) {
					resp.put("hp", h.HP);
					resp.put("ht", h.HT);
					resp.put("pos", h.pos);
					resp.put("depth", Dungeon.depth);
					int itemCount = 0;
					if (h.belongings != null && h.belongings.backpack != null) {
						itemCount += h.belongings.backpack.items.size();
					}
					resp.put("items", itemCount);
				}
			} else {
				resp.put("found", false);
			}
			client.sendMessage(Protocol.PEEK_CHARACTER_RESPONSE, resp);
			log("[NET-HOST] PEEK_CHARACTER name=" + name + " found=" + (h != null));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Called from HostServer read thread when PLAYER_ACTION arrives */
	private static void handlePlayerAction(HostServer.ClientConnection client, JSONObject data) {
		int cell = data.optInt("cell", -1);
		String type = data.optString("type", null);
		String item = data.optString("item", null);
		String useAction = data.optString("useAction", null);
		int extra = data.optInt("extra", -1);
		Hero hero = netHeroes.get(client);
		if (hero == null) {
			log("[NET-HOST] PLAYER_ACTION from unknown client cell=" + cell + " type=" + type);
			return;
		}
		// Bounds-guard the cell BEFORE it ever touches map[]. -1 is sentinel "no cell";
		// anything else must be a valid level index. Snapshot the reference once to avoid
		// a TOCTOU race if the game thread replaces Dungeon.level mid-check.
		xyz.gabriwar.warpedpixeldungeon.levels.Level lvl = Dungeon.level;
		if (cell != -1 && (lvl == null || cell < 0 || cell >= lvl.length())) {
			log("[NET-HOST] PLAYER_ACTION rejected — out-of-range cell=" + cell
					+ " (levelLen=" + (lvl == null ? -1 : lvl.length()) + ")");
			return;
		}
		log("[NET-HOST] recv PLAYER_ACTION hero=" + hero.id() + " cell=" + cell
				+ " type=" + type + " item=" + item + " useAction=" + useAction
				+ " heroPos=" + hero.pos + " prevPending=" + hero.pendingActionCell
				+ " remoteWaiting=" + hero.remoteWaiting);
		// leave_exit is a turn-independent signal — process unconditionally so a client
		// can always back out of the at-exit park even on someone else's turn.
		if ("leave_exit".equals(type)) {
			handleLeaveExit(hero);
			return;
		}
		// Portal pay/travel/open don't consume a turn and are fully validated host-side
		// (atGate + gold/state checks). Accept them turn-independently so a tap during
		// the brief re-grant window after portal_open isn't silently dropped.
		boolean turnIndependent = "portal_pay".equals(type)
				|| "portal_travel".equals(type)
				|| "portal_open".equals(type)
				|| (type != null && (type.startsWith("blacksmith_") || type.startsWith("shop_")));
		// Reject everything else if it's not this hero's turn. Prevents pre-queueing moves
		// while another player is acting, and avoids stale fields being clobbered between
		// turn ticks.
		if (!hero.remoteWaiting && !turnIndependent) {
			log("[NET-HOST] PLAYER_ACTION dropped — not this hero's turn (heroId=" + hero.id() + ")");
			return;
		}
		// Stash compound action atomically — the actor thread reads the same fields and
		// must not see a half-written (type, item, useAction, cell) tuple.
		synchronized (hero) {
			if (type != null && !type.isEmpty() && !"null".equals(type)) {
				hero.pendingActionType = type;
				hero.pendingActionItem = (item != null && !"null".equals(item)) ? item : null;
				hero.pendingActionItemAction = (useAction != null && !"null".equals(useAction)) ? useAction : null;
				hero.pendingActionExtra = extra;
				if ("throw".equals(type)) hero.pendingActionCell = cell;
			} else {
				hero.pendingActionCell = cell;
			}
		}
		wakeActorThread();
	}

	/** Host: find the connection owning a given net hero (reverse of netHeroes). */
	private static HostServer.ClientConnection clientForHero(Hero hero) {
		for (java.util.Map.Entry<HostServer.ClientConnection, Hero> e : netHeroes.entrySet()) {
			if (e.getValue() == hero) return e.getKey();
		}
		return null;
	}

	/** Host→client: push an interaction dialog to a remote hero's owner. Returns
	 *  false if the hero has no connected client. Called via {@link NetDialogs}. */
	public static boolean sendDialogToHero(Hero hero, int dialogId, String kind, JSONObject payload) {
		HostServer.ClientConnection c = clientForHero(hero);
		if (c == null) {
			log("[NET-HOST] sendDialogToHero: no client for hero=" + hero.id());
			return false;
		}
		try {
			JSONObject data = new JSONObject();
			data.put("dialogId", dialogId);
			data.put("kind", kind);
			data.put("payload", payload != null ? payload : new JSONObject());
			c.sendMessage(Protocol.SHOW_DIALOG, data);
			log("[NET-HOST] sent SHOW_DIALOG hero=" + hero.id() + " id=" + dialogId + " kind=" + kind);
			return true;
		} catch (Exception e) {
			log("[NET-HOST] sendDialogToHero failed: " + e.getMessage());
			return false;
		}
	}

	/** Host: client resolved an interaction dialog. Turn-independent — a reward can
	 *  be claimed after the interacting turn already ended — so we don't gate on
	 *  remoteWaiting. Stash for the actor thread to apply single-threaded. */
	private static void handleDialogChoice(HostServer.ClientConnection client, JSONObject data) {
		Hero hero = netHeroes.get(client);
		if (hero == null) return;
		int dialogId = data.optInt("dialogId", -1);
		String choice = data.optString("choice", null);
		if (dialogId < 0 || !NetDialogs.isOwnedBy(dialogId, hero)) {
			log("[NET-HOST] DIALOG_CHOICE rejected — id=" + dialogId + " hero=" + hero.id());
			return;
		}
		synchronized (hero) {
			hero.pendingDialogId = dialogId;
			hero.pendingDialogChoice = choice;
		}
		log("[NET-HOST] recv DIALOG_CHOICE hero=" + hero.id() + " id=" + dialogId + " choice=" + choice);
		wakeActorThread();
	}

	// Cached reflective handle to GameScene.actorThread (private static field).
	// Looked up once on first wake; the field reference is stable for the JVM lifetime.
	private static volatile java.lang.reflect.Field cachedActorThreadField;

	public static void wakeActorThread() {
		try {
			if (cachedActorThreadField == null) {
				cachedActorThreadField = xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.class
						.getDeclaredField("actorThread");
				cachedActorThreadField.setAccessible(true);
			}
			Object t = cachedActorThreadField.get(null);
			if (t instanceof Thread) {
				synchronized (t) { t.notifyAll(); }
				log("[NET-HOST] Actor thread notified");
			}
		} catch (Exception e) {
			log("[NET-HOST] wakeActorThread failed: " + e.getMessage());
		}
	}

	public static java.util.Collection<Hero> getNetHeroes() {
		return netHeroes.values();
	}

	// --- Client: start as player ---

	public static void startPlayer(String host, JSONObject heroData) {
		//stop() wipes the live token, so recover the one this (host, name) was
		//issued before opening the new connection - otherwise the player cannot
		//reclaim their own hero after a visit to the lobby
		String name = xyz.gabriwar.warpedpixeldungeon.WPDSettings.multiplayerName();
		String keep = clientTokens.get( tokenKey( host, name ) );
		stop();
		currentHostAddress = host;
		client = new SpectatorClient(host, PORT);
		receiver = new SpectatorReceiver();
		client.setMessageHandler(receiver);
		if (keep != null && !keep.isEmpty()) {
			clientSessionToken = keep;
			client.setSessionToken(keep);
		}
		client.connectAsPlayer(heroData);
		mode = NetMode.PLAYER;
	}

	// --- Client: start as spectator ---

	public static void startSpectator(String host) {
		stop();
		client = new SpectatorClient(host, PORT);
		receiver = new SpectatorReceiver();
		client.setMessageHandler(receiver);
		client.connect();
		mode = NetMode.SPECTATOR;
	}

	// --- Client: send action ---

	public static void sendPlayerAction(int cell) {
		if (client == null) return;
		try {
			JSONObject data = new JSONObject();
			data.put("cell", cell);
			client.sendMessage(Protocol.PLAYER_ACTION, data);
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] sent PLAYER_ACTION cell=" + cell + " (myHeroId=" + myNetHeroId + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
		myTurn = false;
	}

	/** Client→host: report a resolved interaction dialog (e.g. picked a quest reward). */
	public static void sendDialogChoice(int dialogId, String choice) {
		if (client == null) return;
		try {
			JSONObject data = new JSONObject();
			data.put("dialogId", dialogId);
			data.put("choice", choice != null ? choice : "");
			client.sendMessage(Protocol.DIALOG_CHOICE, data);
			log("[NET-CLI] sent DIALOG_CHOICE id=" + dialogId + " choice=" + choice);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Send a typed action (search, etc) that doesn't map to a target cell. Host
	 *  invokes the corresponding method on the netHero. */
	public static void sendPlayerActionTyped(String type) {
		if (client == null) return;
		try {
			JSONObject data = new JSONObject();
			data.put("cell", -1);
			data.put("type", type);
			client.sendMessage(Protocol.PLAYER_ACTION, data);
			log("[NET-CLI] sent PLAYER_ACTION type=" + type + " (myHeroId=" + myNetHeroId + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
		myTurn = false;
	}

	/** Typed action carrying one extra int payload (e.g. portal travel destination depth). */
	public static void sendPlayerActionTyped(String type, int extra) {
		if (client == null) return;
		try {
			JSONObject data = new JSONObject();
			data.put("cell", -1);
			data.put("type", type);
			data.put("extra", extra);
			client.sendMessage(Protocol.PLAYER_ACTION, data);
			log("[NET-CLI] sent PLAYER_ACTION type=" + type + " extra=" + extra + " (myHeroId=" + myNetHeroId + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
		myTurn = false;
	}

	/** Client-side: tell the host we want our hero un-parked from the exit
	 *  (e.g. user wants to keep playing instead of waiting for the rest). */
	public static void sendLeaveExit() {
		sendPlayerActionTyped("leave_exit");
	}

	/** Lobby-side peek. Opens a short-lived socket to the host, asks for the
	 *  player's stashed hero summary by name, fires the callback (on a worker
	 *  thread) with the response or null on error / not-found. */
	public interface PeekCallback {
		void onResult(JSONObject info);
	}

	public static void peekCharacter(String host, String playerName, PeekCallback cb) {
		new Thread(() -> {
			java.net.Socket sock = null;
			try {
				sock = new java.net.Socket();
				sock.connect(new java.net.InetSocketAddress(host, PORT), 4000);
				sock.setSoTimeout(4000);
				JSONObject req = new JSONObject();
				req.put("name", playerName);
				String known = clientTokens.get( tokenKey( host, playerName ) );
				if (known != null) req.put("token", known);
				Protocol.writeMessage(sock.getOutputStream(), Protocol.PEEK_CHARACTER, req);
				Protocol.Message resp = Protocol.readMessage(sock.getInputStream());
				if (resp.type == Protocol.PEEK_CHARACTER_RESPONSE) {
					cb.onResult(resp.data);
				} else {
					cb.onResult(null);
				}
			} catch (Exception e) {
				log("[NET-CLI] peek failed: " + e.getMessage());
				cb.onResult(null);
			} finally {
				if (sock != null) try { sock.close(); } catch (Exception ignored) {}
			}
		}, "Net-Peek").start();
	}

	/** Item-class-specific action (drink/eat/read/equip/...) routed to host.
	 *  Host runs `item.execute(netHero, useAction)` against the item resolved by name. */
	public static void sendItemUse(String itemName, String useAction) {
		if (client == null) return;
		try {
			JSONObject data = new JSONObject();
			data.put("cell", -1);
			data.put("type", "use");
			data.put("item", itemName);
			data.put("useAction", useAction);
			client.sendMessage(Protocol.PLAYER_ACTION, data);
			log("[NET-CLI] sent PLAYER_ACTION type=use item=" + itemName
					+ " useAction=" + useAction + " (myHeroId=" + myNetHeroId + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
		myTurn = false;
	}

	/**
	 * Inventory mutation (drop/throw/use). Host looks up the item by name on the
	 * netHero's belongings and runs the corresponding game-side action.
	 * targetCell only used for "throw"; pass -1 otherwise.
	 */
	public static void sendInventoryAction(String type, String itemName, int targetCell) {
		if (client == null) return;
		try {
			JSONObject data = new JSONObject();
			data.put("cell", targetCell);
			data.put("type", type);
			data.put("item", itemName);
			client.sendMessage(Protocol.PLAYER_ACTION, data);
			log("[NET-CLI] sent PLAYER_ACTION type=" + type + " item=" + itemName
					+ " cell=" + targetCell + " (myHeroId=" + myNetHeroId + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
		myTurn = false;
	}

	public static void setMyTurn(boolean turn) { myTurn = turn; }
	public static boolean isMyTurn() { return myTurn; }

	public static void setMyNetHeroId(int id) { myNetHeroId = id; }
	public static int getMyNetHeroId() { return myNetHeroId; }

	/** Log a line addressed to one hero's screen, not the whole party. Single-player or
	 *  the host's own hero -> shown locally (host's own line never broadcasts to clients);
	 *  a remote hero -> queued targeted so only that client shows it. */
	public static void heroLog(Hero h, String text) {
		if (h == null) return;
		if (!isHost()) {
			xyz.gabriwar.warpedpixeldungeon.utils.GLog.i(text);
		} else if (h == Dungeon.hero) {
			xyz.gabriwar.warpedpixeldungeon.utils.GLog.update.dispatch(text);
		} else {
			StateSerializer.recordLogMessageForHero(h.id(), text);
		}
	}

	// --- Common ---

	public static void stop() {
		if (NET_DEBUG) {
			log("[NET] NetManager.stop() called from:");
			new Throwable().printStackTrace();
		}
		Discovery.stopAll();
		if (server != null) {
			server.stop();
			server = null;
		}
		if (client != null) {
			client.disconnect();
			client = null;
		}
		receiver = null;
		hostHeroName = "";
		hostInGame = false;
		mode = NetMode.OFF;
		netHeroes.clear();
		unclaimedHeroes.clear();
		sessionTokens.clear();
		clientSessionToken = null;
		myNetHeroId = -1;
		myTurn = false;
		Emitter.netListener = null;
		NetVisuals.clear();
		xyz.gabriwar.warpedpixeldungeon.effects.WeatherOverlay.netStormingOverride = null;
	}

	public static void broadcastGameState() {
		if (!isHost() || server == null) return;
		JSONObject delta = StateSerializer.serializeDelta();
		if (delta != null) {
			server.broadcast(Protocol.DELTA, delta);
		}
	}

	// Last (depth, branch) NetManager observed during a transition. Lets onLevelTransition()
	// tell "actual floor change" (DESCEND/ASCEND/FALL/RETURN/PALANTIR/PORT*) apart
	// from "transition that returns to the same floor" (JOURNAL, RESURRECT-in-place,
	// RESET that regens current depth). Only the former needs netHero relocation.
	//
	// The branch half is load-bearing since the overworld landed: a village house is
	// branch 7 with a *hashed* depth (1..16384), so a door whose hash lands on 97 would
	// look like "same floor" as the overworld itself and skip relocation — leaving every
	// netHero holding an overworld cell (up to 30975) on a 72-cell cottage, which blows
	// up in updateFieldOfView on the next act().
	private static int lastBroadcastDepth = -1;
	private static int lastBroadcastBranch = -1;

	/** Called on actual floor change (InterlevelScene completion). Compares
	 *  (Dungeon.depth, Dungeon.branch) against the pair recorded by the previous
	 *  broadcast and only relocates netHeroes when the floor actually changed.
	 *  JOURNAL / same-floor modes still broadcast the new state but leave hero
	 *  positions alone. */
	public static void onLevelDescent() {
		if (!isHost()) return;
		int curDepth = Dungeon.depth;
		int curBranch = Dungeon.branch;
		if (curDepth != lastBroadcastDepth || curBranch != lastBroadcastBranch) {
			relocateNetHeroesToCurrentFloor();
		}
		lastBroadcastDepth = curDepth;
		lastBroadcastBranch = curBranch;
		broadcastLevelChange();
	}

	/** Reset the floor tracker — call when the host quits or starts fresh. */
	public static void resetLevelTracking() {
		lastBroadcastDepth = -1;
		lastBroadcastBranch = -1;
	}

	private static void relocateNetHeroesToCurrentFloor() {
		if (Dungeon.level == null) return;
		int anchor = pendingRelocationAnchor;
		pendingRelocationAnchor = -1; // consume — one-shot
		if (anchor < 0 || anchor >= Dungeon.level.length()) {
			// Prefer the host's own arrival cell over Level.entrance(). On the overworld
			// entrance() -> getTransition(null) is side-effecting: it consumes the pending
			// arrival stamp and can re-centre the streaming window. The host has already
			// landed by the time this runs, so their cell is both safe to read and exactly
			// where the party should gather.
			anchor = (Dungeon.hero != null
					&& Dungeon.hero.pos >= 0 && Dungeon.hero.pos < Dungeon.level.length())
					? Dungeon.hero.pos
					: Dungeon.level.entrance();
		}
		final int entrance = anchor;
		java.util.HashSet<Hero> moved = new java.util.HashSet<>();
		moved.addAll(netHeroes.values());
		moved.addAll(unclaimedHeroes.values());
		for (Hero nh : moved) {
			int dst = entrance;
			if (xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar(dst) != null) {
				// entrance occupied by host or another netHero — pick a free neighbor.
				// Each hero's pos is assigned before the next iteration runs, so findChar
				// already sees the ones placed above and they don't stack.
				for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) {
					int c = entrance + n;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					if (xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar(c) == null
							&& Dungeon.level.passable[c]) {
						dst = c;
						break;
					}
				}
			}
			nh.pos = dst;
			nh.atExit = false;
			nh.atExitCell = -1;
			nh.curAction = null;
			nh.lastAction = null;
			nh.fieldOfView = null; // re-allocated to new level size on next act()
			if (nh.sprite != null) {
				nh.sprite.killAndErase();
				nh.sprite = null;
			}
			try {
				xyz.gabriwar.warpedpixeldungeon.actors.Actor.add(nh);
			} catch (Throwable ignored) {}
			log("[NET-HOST] Relocated netHero " + nh.id() + " to floor " + Dungeon.depth + " pos=" + dst);
		}
	}

	public static void broadcastLevelChange() {
		if (!isHost() || server == null) return;

		hostInGame = true;
		// Seed floor tracking — onLevelDescent() compares against this on the
		// next transition to decide whether netHeroes need relocating.
		lastBroadcastDepth = Dungeon.depth;
		lastBroadcastBranch = Dungeon.branch;
		NetVisuals.clear();

		if (Dungeon.level != null && Dungeon.hero != null) {
			Dungeon.level.updateFieldOfView(Dungeon.hero, Dungeon.level.heroFOV);
		}

		int clients = getClientCount();
		server.broadcast(Protocol.LEVEL_CHANGE, new JSONObject());

		JSONObject state = StateSerializer.serializeFullState();
		if (state != null) {
			try {
				String heroName = Dungeon.hero != null ? Dungeon.hero.name() : "Hero";
				JSONObject nameData = new JSONObject();
				nameData.put("name", heroName);
				server.broadcast(Protocol.HERO_NAME, nameData);
			} catch (Exception ignored) {}
			server.broadcast(Protocol.FULL_STATE, state);
			if (clients > 0) {
				Game.runOnRenderThread(() -> GLog.p("Game state sent to " + clients + " client(s)"));
			}
		}
		StateSerializer.resetDeltaTracking();
	}

	public static int getClientCount() {
		return server != null ? server.getClientCount() : 0;
	}

	/** # of connected clients that have claimed a netHero (players, not spectators). */
	public static int getPlayerCount() { return netHeroes.size(); }

	/** # of connected clients without a netHero (spectators only). */
	public static int getSpectatorCount() {
		return Math.max(0, getClientCount() - getPlayerCount());
	}

	// Net MP turn telemetry — set on host by StateSerializer (whoever has lowest
	// cooldown gets to act next), mirrored on clients from delta. UI reads this
	// to highlight whose turn is "now". -1 = unknown.
	public static volatile int activeHeroId = -1;

	public static boolean isClientConnected() {
		return client != null && client.isConnected();
	}

	public static long getPingMs() {
		return client != null ? client.getPingMs() : -1;
	}

	public static String getHostHeroName() { return hostHeroName; }
	public static void setHostHeroName(String name) { hostHeroName = name; }

	// --- Group-descent (at-exit) gate ---
	//
	// In net mode the actual level transition doesn't happen until every online
	// claimed hero (host + active clients) is parked on a transition cell. Each
	// hero that walks onto an exit calls onHeroReachedExit; the host re-checks
	// readiness, broadcasts a chat line, and triggers the group descent when
	// everyone's accounted for.

	private static String lastExitChatSig = "";

	public static void onHeroReachedExit(Hero h) {
		if (!isHost()) return;
		String name = heroDisplayName(h);
		int ready = countAtExit();
		int total = countOnlineClaimedHeroes();
		String sig = name + "|" + ready + "/" + total;
		if (!sig.equals(lastExitChatSig)) {
			lastExitChatSig = sig;
			Game.runOnRenderThread(() -> GLog.p(name + " is at the exit (" + ready + "/" + total + ")"));
		}
		log("[NET-HOST] " + name + " atExit (" + ready + "/" + total + ")");
		if (ready >= total && total > 0) {
			triggerGroupDescent();
		}
	}

	public static void onHeroLeftExit(Hero h) {
		if (!isHost()) return;
		h.atExit = false;
		h.atExitCell = -1;
		if (h.sprite != null) h.sprite.visible = true;
		if (h == Dungeon.hero) {
			xyz.gabriwar.warpedpixeldungeon.net.ui.WndAtExit.dismiss();
		}
		String name = heroDisplayName(h);
		int ready = countAtExit();
		int total = countOnlineClaimedHeroes();
		lastExitChatSig = ""; // re-arm so the next reach broadcasts again
		lastSplitSig = "";
		Game.runOnRenderThread(() -> GLog.w(name + " left the exit (" + ready + "/" + total + ")"));
		log("[NET-HOST] " + name + " leftExit (" + ready + "/" + total + ")");
		wakeActorThread();
	}

	private static String heroDisplayName(Hero h) {
		if (h == Dungeon.hero) {
			String n = xyz.gabriwar.warpedpixeldungeon.WPDSettings.multiplayerName();
			return (n != null && !n.isEmpty()) ? n : "Host";
		}
		return (h.netOwnerName != null && !h.netOwnerName.isEmpty()) ? h.netOwnerName : ("Player " + h.id());
	}

	private static int countAtExit() {
		int n = 0;
		if (Dungeon.hero != null && Dungeon.hero.atExit) n++;
		// Snapshot before iterating — a concurrent onClientDisconnected can
		// remove from the live map and yield a stale or skipped iteration.
		for (Hero h : new java.util.ArrayList<>(netHeroes.values())) {
			if (h.atExit) n++;
		}
		return n;
	}

	private static int countOnlineClaimedHeroes() {
		// host counts as 1 if alive, plus every claimed remote hero (unclaimed
		// stashed ones don't count — they'll just teleport along).
		int n = (Dungeon.hero != null && Dungeon.hero.isAlive()) ? 1 : 0;
		n += netHeroes.size();
		return n;
	}

	/** The transition a parked hero is standing on, or null if their cell no
	 *  longer resolves to one (the window slid, the level changed under them). */
	private static xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition transitionAt(Hero h) {
		if (h == null || Dungeon.level == null) return null;
		int cell = (h.atExit && h.atExitCell != -1) ? h.atExitCell : h.pos;
		if (cell < 0 || cell >= Dungeon.level.length()) return null;
		return Dungeon.level.getTransition(cell);
	}

	private static boolean sameDestination(
			xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition a,
			xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition b) {
		return a.destDepth == b.destDepth
				&& a.destBranch == b.destBranch
				&& a.destType == b.destType;
	}

	// Deduped "you're at different exits" chat — the readiness check re-runs on every
	// park, and without this it would repeat the same line on every turn of the standoff.
	private static String lastSplitSig = "";

	/** Un-park everyone and say why. Used when the party cannot actually travel —
	 *  otherwise the clients sit in the modal WndAtExit panel forever on a level
	 *  that is never going to change. */
	private static void releaseParty(String reason) {
		if (Dungeon.hero != null && Dungeon.hero.atExit) onHeroLeftExit(Dungeon.hero);
		for (Hero nh : new java.util.ArrayList<>(netHeroes.values())) {
			if (nh.atExit) onHeroLeftExit(nh);
		}
		lastExitChatSig = "";
		lastSplitSig = "";
		xyz.gabriwar.warpedpixeldungeon.net.ui.WndAtExit.dismiss();
		Game.runOnRenderThread(() -> GLog.w(reason));
	}

	private static void triggerGroupDescent() {
		log("[NET-HOST] Group descent triggered");
		Game.runOnRenderThread(() -> {
			if (Dungeon.hero == null || Dungeon.level == null) return;

			// Gather everyone who is parked. The surface exposes many exits at once
			// (the mine gate, the plaza stairs, six town doorways, every village door
			// in the window), so "everyone is at *an* exit" is no longer the same
			// question as "everyone is going to the same place".
			java.util.ArrayList<Hero> parked = new java.util.ArrayList<>();
			if (Dungeon.hero.atExit) parked.add(Dungeon.hero);
			for (Hero nh : new java.util.ArrayList<>(netHeroes.values())) {
				if (nh.atExit) parked.add(nh);
			}

			xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition dest = null;
			Hero pilot = null;
			for (Hero p : parked) {
				xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition tr = transitionAt(p);
				if (tr == null) continue;
				if (dest == null) {
					dest = tr;
					pilot = p;
				} else if (!sameDestination(dest, tr)) {
					String sig = heroDisplayName(pilot) + "|" + heroDisplayName(p);
					if (!sig.equals(lastSplitSig)) {
						lastSplitSig = sig;
						final String a = heroDisplayName(pilot), b = heroDisplayName(p);
						GLog.w(xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(
								NetManager.class, "exit_split", a, b));
					}
					log("[NET-HOST] triggerGroupDescent: party split across exits, holding");
					return;
				}
			}

			if (dest == null) {
				// Nobody is standing on a transition any more.
				releaseParty(xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(
						NetManager.class, "exit_no_transition"));
				return;
			}

			Hero h = Dungeon.hero;
			if (!h.isAlive()) {
				// The host is out of the game, so nobody can drive InterlevelScene.
				// Let the clients back out instead of stranding them in the panel.
				releaseParty(xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(
						NetManager.class, "exit_host_down"));
				return;
			}

			// Make sure the host is positioned on the transition cell so
			// activateTransition's post-step picks the right destination.
			h.pos = pilot.atExitCell != -1 ? pilot.atExitCell : pilot.pos;
			// Clear at-exit flags so heroes resume normal scheduling on the next
			// floor. They'll be moved next to the host during the level swap.
			h.atExit = false;
			h.atExitCell = -1;
			if (h.sprite != null) h.sprite.visible = true;
			for (Hero nh : netHeroes.values()) {
				nh.atExit = false;
				nh.atExitCell = -1;
				if (nh.sprite != null) nh.sprite.visible = true;
			}
			lastExitChatSig = ""; // ready for the next floor
			lastSplitSig = "";
			xyz.gabriwar.warpedpixeldungeon.net.ui.WndAtExit.dismiss();
			Dungeon.level.activateTransition(h, dest);
		});
	}

	/** Called from PLAYER_ACTION dispatcher when a client clicks the
	 *  "Leave exit" button on the WndAtExit panel. */
	public static void handleLeaveExit(Hero h) {
		if (h == null || !h.atExit) return;
		onHeroLeftExit(h);
	}

	// --- Party-travel items ---
	//
	// The town return beacon and Otiluke's journal don't walk to a transition — they
	// drive InterlevelScene straight from execute(), which means they skip the at-exit
	// gate entirely and take the WHOLE party with them (relocateNetHeroesToCurrentFloor
	// drags every netHero along). That's tolerable when the host does it: the host is
	// the one running the level, everybody lands together, nobody is stranded. It is
	// not tolerable from a client — one player would yank four others across the world
	// with no warning, and the items read Dungeon.hero (the *host's* buffs and cell)
	// while doing it. So they stay host-only in net mode.

	/** True if this item action moves the entire party across floors without the
	 *  at-exit gate, and therefore may only be run by the host. */
	public static boolean isPartyTravelAction(Item it, String action) {
		if (it == null || action == null) return false;
		if (it instanceof xyz.gabriwar.warpedpixeldungeon.items.TownReturnBeacon) {
			return xyz.gabriwar.warpedpixeldungeon.items.TownReturnBeacon.AC_RETURN.equals(action)
					|| xyz.gabriwar.warpedpixeldungeon.items.TownReturnBeacon.AC_RETURNTOWN.equals(action);
		}
		if (it instanceof xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal) {
			return xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal.AC_RETURN.equals(action)
					|| xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal.AC_PORT.equals(action);
		}
		return false;
	}

	/** Tell one player their party-travel attempt was refused. */
	public static void refusePartyTravel(Hero h) {
		String text = xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(
				NetManager.class, "party_travel_host_only");
		if (!NetDialogs.handleNetHero(h, text)) heroLog(h, text);
	}

	/** True if any online hero — the host or a claimed remote player — can see the
	 *  cell right now. netHeroes keep their own FOV array; the host's heroFOV alone
	 *  would silently hide events happening in front of a client's face. */
	public static boolean anyHeroSees(int cell) {
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return false;
		if (Dungeon.level.heroFOV != null
				&& Dungeon.level.heroFOV.length > cell
				&& Dungeon.level.heroFOV[cell]) {
			return true;
		}
		if (!isHost()) return false;
		for (Hero h : netHeroes.values()) {
			boolean[] fov = h.fieldOfView;
			if (fov != null && fov.length > cell && fov[cell]) return true;
		}
		return false;
	}

	// --- Surface window slides ---
	//
	// The overworld is a 176x176 window onto an infinite world, and it slides under the
	// whole party whenever the host walks far enough from its centre. A slide relabels
	// EVERY cell index on the level, so anything the net layer holds keyed by a cell —
	// a client's queued tap, a hero's at-exit park, a cached field of view — is pointing
	// somewhere else afterwards. Origin is the signal, not the window version: the season
	// re-derives the same window in place (terrain changes, indices don't).
	private static int lastWindowX = Integer.MIN_VALUE;
	private static int lastWindowY = Integer.MIN_VALUE;

	/**
	 * Host: notice a window slide and drop everything the net layer holds keyed by a cell
	 * index. Idempotent and cheap — called from every hero's act(), does nothing until the
	 * origin actually moves. Hero <em>positions</em> are the window's own business; this
	 * only clears the stale references the net layer owns.
	 */
	public static void checkWorldWindow() {
		if (!isHost()) return;
		if (!(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)) {
			lastWindowX = lastWindowY = Integer.MIN_VALUE;
			return;
		}
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow =
				(xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
		int wx = ow.worldX(), wy = ow.worldY();
		if (wx == lastWindowX && wy == lastWindowY) return;
		boolean slid = lastWindowX != Integer.MIN_VALUE;
		lastWindowX = wx;
		lastWindowY = wy;
		if (!slid) return; // first sight of this window, nothing to invalidate

		for (Hero h : allKnownNetHeroes()) {
			synchronized (h) {
				h.pendingActionCell = -1;
				h.pendingActionType = null;
				h.pendingActionItem = null;
				h.pendingActionItemAction = null;
				h.pendingActionExtra = -1;
				h.remoteWaiting = false;
			}
			// A queued walk aims at a cell that is now somewhere else entirely.
			h.curAction = null;
			h.lastAction = null;
			// Re-derived against the new window on the next act().
			h.fieldOfView = null;
			// The exit they were parked on may not even be in the window any more,
			// and their atExitCell would drive the party to the wrong destination.
			if (h.atExit) onHeroLeftExit(h);
		}
		log("[NET-HOST] world window slid to " + wx + "," + wy + " — dropped queued remote input");
		wakeActorThread();
	}


	public static void handleDisconnect(String reason) {
		mode = NetMode.OFF;
		Game.runOnRenderThread(() -> {
			Game.scene().addToFront(new xyz.gabriwar.warpedpixeldungeon.net.ui.WndNetError(
					"Disconnected",
					reason != null ? reason : "Connection lost",
					() -> Game.switchScene(TitleScene.class)
			));
		});
	}

	public static void handleReconnecting(int attempt, int max) {
		Game.runOnRenderThread(() -> GLog.w("Reconnecting... (" + attempt + "/" + max + ")"));
	}

	// --- Utility ---

	private static int findEmptyCell(int near) {
		int[] offsets = {-1, 1, -Dungeon.level.width(), Dungeon.level.width(),
				-Dungeon.level.width()-1, -Dungeon.level.width()+1,
				Dungeon.level.width()-1, Dungeon.level.width()+1};
		for (int off : offsets) {
			int cell = near + off;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.passable[cell]
					&& Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return near; // fallback
	}
}
