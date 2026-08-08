package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherOverlay;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import com.watabou.noosa.Game;
import com.watabou.utils.Reflection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorReceiver implements SpectatorClient.MessageHandler {

	private final ConcurrentHashMap<Integer, Mob> trackedMobs = new ConcurrentHashMap<>();

	// Security allowlist for the FULL_STATE-derived `armorAbility` class name field.
	// Anything outside this list is rejected; never Class.forName(network-supplied string).
	private static final HashMap<String,
			Class<? extends xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility>>
			ARMOR_ABILITY_ALLOWLIST = new HashMap<>();
	static {
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.Ratmogrify.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.Ratmogrify.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.HeroicLeap.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.HeroicLeap.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Shockwave.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Shockwave.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Endure.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Endure.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.ElementalBlast.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.ElementalBlast.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.WarpBeacon.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.WarpBeacon.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.WildMagic.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.WildMagic.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.DeathMark.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.DeathMark.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.ShadowClone.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.ShadowClone.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpectralBlades.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpectralBlades.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.NaturesPower.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.NaturesPower.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Challenge.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Challenge.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.ElementalStrike.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.ElementalStrike.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Feint.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Feint.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.Trinity.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.Trinity.class);
		ARMOR_ABILITY_ALLOWLIST.put(
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.AscendedForm.class.getName(),
				xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.AscendedForm.class);
	}
	// Remote heroes tracked by host actor ID (includes P1's hero and other players)
	private final ConcurrentHashMap<Integer, SpectatorMob> trackedNetHeroes = new ConcurrentHashMap<>();

	@Override
	public void onMessage(Protocol.Message message) {
		try {
			switch (message.type) {
			case Protocol.FULL_STATE:
				handleFullState(message.data);
				break;
			case Protocol.DELTA:
				handleDelta(message.data);
				break;
			case Protocol.LEVEL_CHANGE:
				break;
			case Protocol.HERO_NAME:
				String name = message.data.optString("name", "Hero");
				NetManager.setHostHeroName(name);
				break;
			case Protocol.LOBBY_INFO:
				//a refused claim comes back on this channel: say why instead of
				//dropping the player into the spectator lobby with no explanation
				String lobbyError = message.data.optString("error", "");
				if (!lobbyError.isEmpty()) {
					final String reason;
					switch (lobbyError) {
						case "name_in_use":
							reason = "Someone is already playing as that name on this host."; break;
						case "token_mismatch":
							reason = "That hero belongs to another player's session. "
									+ "Rejoin from the device you created it on."; break;
						case "name_required":
							reason = "Choose a player name before joining."; break;
						case "already_spectator":
							reason = "You joined as a spectator - reconnect to play."; break;
						default:
							reason = lobbyError;
					}
					NetManager.log("[NET-CLI] lobby refused the join: " + lobbyError);
					Game.runOnRenderThread(() -> {
						if (Game.scene() != null) {
							Game.scene().addToFront(new xyz.gabriwar.warpedpixeldungeon.net.ui.WndNetError(
									"Could not join", reason, null));
						}
					});
					break;
				}
				String hostName = message.data.optString("hostName", "Host");
				Game.runOnRenderThread(() ->
					Game.switchScene(xyz.gabriwar.warpedpixeldungeon.net.ui.SpectatorLobbyScene.class)
				);
				xyz.gabriwar.warpedpixeldungeon.net.ui.SpectatorLobbyScene.pendingHostName = hostName;
				break;
			case Protocol.YOUR_TURN:
				handleYourTurn(message.data);
				break;
			case Protocol.SHOW_DIALOG:
				handleShowDialog(message.data);
				break;
			}
		} catch (Throwable t) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] onMessage threw on type=" + message.type
					+ ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
			t.printStackTrace();
		}
	}

	private void handleShowDialog(JSONObject data) {
		final int dialogId = data.optInt("dialogId", -1);
		final String kind = data.optString("kind", "");
		final JSONObject payload = data.optJSONObject("payload") != null
				? data.optJSONObject("payload") : new JSONObject();
		xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] recv SHOW_DIALOG id=" + dialogId + " kind=" + kind);

		// Shopkeeper: build the SAME window the host shows (Shopkeeper.showShopMenu) from
		// the payload, rather than a separate client reimplementation.
		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_SHOPKEEPER.equals(kind)) {
			final String title = payload.optString("title", "Shopkeeper");
			final String desc = payload.optString("desc", "");
			final String chat = payload.optString("chat", "");
			final int gold = payload.optInt("gold", 0);
			final java.util.ArrayList<Item> buybacks = new java.util.ArrayList<>();
			JSONArray arr = payload.optJSONArray("buybacks");
			if (arr != null) {
				for (int i = 0; i < arr.length(); i++) {
					JSONObject jb = arr.optJSONObject(i);
					if (jb == null) continue;
					final int img = jb.optInt("image", 0);
					final String nm = jb.optString("name", "item");
					final int val = jb.optInt("value", 0);
					final int lvl = jb.optInt("lvl", 0);
					buybacks.add(new Item() {
						{ image = img; levelKnown = true; }
						@Override public int value() { return val; }
						@Override public String title() { return nm; }
						@Override public String name() { return nm; }
						@Override public int level() { return lvl; }
						@Override public boolean isIdentified() { return true; }
					});
				}
			}
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper.showShopMenu(
					title, desc, chat, buybacks, gold, true);
			return;
		}

		// Reward dialogs: show the SAME original window the host uses, in remote mode
		// (it ships the choice instead of applying). No client reimplementation.
		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_GHOST_REWARD.equals(kind)) {
			final int type = payload.optInt("type", 1);
			final Item w = xyz.gabriwar.warpedpixeldungeon.windows.WndNetDialog.displayItem(payload.optJSONObject("weapon"));
			final Item a = xyz.gabriwar.warpedpixeldungeon.windows.WndNetDialog.displayItem(payload.optJSONObject("armor"));
			Game.runOnRenderThread(() -> {
				if (Game.scene() instanceof GameScene)
					GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndSadGhost(dialogId, type, w, a));
			});
			return;
		}
		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_WANDMAKER_REWARD.equals(kind)) {
			final int type = payload.optInt("type", 1);
			final Item w1 = xyz.gabriwar.warpedpixeldungeon.windows.WndNetDialog.displayItem(payload.optJSONObject("wand1"));
			final Item w2 = xyz.gabriwar.warpedpixeldungeon.windows.WndNetDialog.displayItem(payload.optJSONObject("wand2"));
			Game.runOnRenderThread(() -> {
				if (Game.scene() instanceof GameScene)
					GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndWandmaker(dialogId, type, w1, w2));
			});
			return;
		}
		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_IMP_REWARD.equals(kind)) {
			Game.runOnRenderThread(() -> {
				if (Game.scene() instanceof GameScene)
					GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndImp(dialogId));
			});
			return;
		}
		// NPC intro/reminder text: rebuild the SAME WndQuest (titled message + NPC sprite)
		// the host shows, from the sprite class + name in the payload.
		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_INFO.equals(kind) && payload.has("title")) {
			final String title = payload.optString("title", "");
			final String text = payload.optString("text", "");
			final String spriteName = payload.optString("sprite", null);
			Game.runOnRenderThread(() -> {
				if (!(Game.scene() instanceof GameScene)) return;
				com.watabou.noosa.Image icon = null;
				// SECURITY: spriteName is network-supplied. Never instantiate an arbitrary
				// class from a peer — restrict to the sprites package (and reject inner
				// classes / traversal) so a malicious host can't trigger a side-effecting
				// constructor (RCE). Anything else falls back to the plain info box.
				if (spriteName != null
						&& spriteName.startsWith("xyz.gabriwar.warpedpixeldungeon.sprites.")
						&& spriteName.indexOf('$') == -1
						&& !spriteName.contains("..")) {
					try {
						Class<?> cls = com.watabou.utils.Reflection.forName(spriteName);
						Object s = cls == null ? null : com.watabou.utils.Reflection.newInstance(cls);
						if (s instanceof com.watabou.noosa.Image) icon = (com.watabou.noosa.Image) s;
					} catch (Exception ignored) {}
				}
				if (icon != null) {
					GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage(
							icon, title, text));
				} else {
					// sprite didn't resolve — fall back to the plain info box
					GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndNetDialog(
							dialogId, kind, payload));
				}
			});
			return;
		}

		Game.runOnRenderThread(() -> {
			if (Game.scene() instanceof GameScene) {
				GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndNetDialog(
						dialogId, kind, payload));
			}
		});
	}

	private void handleYourTurn(JSONObject data) {
		int heroId = data.optInt("heroId", -1);
		int pos = data.optInt("pos", -1);
		boolean ack = data.optBoolean("ack", false);
		xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] recv YOUR_TURN heroId=" + heroId + " pos=" + pos + " ack=" + ack);
		if (heroId != -1) {
			NetManager.setMyNetHeroId(heroId);
		}
		// Store session token for reconnect authentication
		String token = data.optString("token", null);
		if (token != null && !token.isEmpty()) {
			NetManager.storeClientSessionToken(token);
		}
		// Enable input on the render thread
		Game.runOnRenderThread(() -> {
			NetManager.setMyTurn(true);
			if (Game.scene() instanceof GameScene) {
				GameScene.enablePlayerInput();
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] enabledPlayerInput (scene ready)");
			} else {
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] YOUR_TURN before scene ready, input deferred");
			}
		});
	}

	// Mirror the host's shared portal registry so the client's WndPortal renders the
	// correct state + travel destinations. Format matches StateSerializer.serializePortals.
	private void applyPortals(JSONArray portals) {
		if (portals == null) return;
		for (int i = 0; i < portals.length(); i++) {
			JSONObject p = portals.optJSONObject(i);
			if (p == null) continue;
			int depth = p.optInt("depth", -1);
			if (depth <= 0) continue;
			int cell = p.optInt("cell", -1);
			boolean discovered = p.optBoolean("discovered", false);
			PortalGate.State state;
			try {
				state = PortalGate.State.valueOf(p.optString("state", "LOCKED"));
			} catch (IllegalArgumentException ex) {
				state = PortalGate.State.LOCKED;
			}
			Portals.registerNet(depth, state, cell, discovered);
		}
	}

	@Override
	public void onDisconnected(String reason) {
		xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] onDisconnected reason=" + reason);
		NetManager.handleDisconnect(reason);
	}

	@Override
	public void onConnected() {}

	@Override
	public void onReconnecting(int attempt, int max) {
		NetManager.handleReconnecting(attempt, max);
	}

	// --- Full State ---

	/**
	 * The run-wide values that ride a level packet's identity block. Applied before
	 * anything derives from them: the whole surface — biomes, village names, the snow
	 * line — comes out of the seed, and both GameCalendar and DayNightCycle branch on
	 * the challenge mask (REAL_CLOCK), so a client without the host's mask reads a
	 * different time of day and, through it, different shop hours. Neither is zero on
	 * a client that has played locally before — they hold that game's values.
	 */
	private static void applyIdentityGlobals(JSONObject identity) {
		if (identity == null) return;
		if (identity.has("seed")) Dungeon.seed = identity.optLong("seed", Dungeon.seed);
		if (identity.has("chal")) Dungeon.challenges = identity.optInt("chal", Dungeon.challenges);
	}

	private void handleFullState(JSONObject data) {
		try {
			int depth = data.getInt("depth");
			int branch = data.optInt("branch", 0);
			int w = data.getInt("w");
			int h = data.getInt("h");
			if (w <= 0 || w > 512 || h <= 0 || h > 512)
				throw new java.io.IOException("implausible level dimensions: " + w + "x" + h);

			JSONArray mapArr = data.getJSONArray("map");
			int[] mapData = new int[mapArr.length()];
			for (int i = 0; i < mapArr.length(); i++) {
				mapData[i] = mapArr.getInt(i);
			}

			String tilesTex = data.optString("tilesTex", null);
			String waterTex = data.optString("waterTex", null);

			JSONObject identity = data.optJSONObject("level");
			// The whole surface — biomes, village names, the snow line — is derived
			// from the seed, and GameCalendar's season from cycleTurn + the calendar
			// start day. Both must be the host's BEFORE the level is built: the
			// weather block that normally carries the calendar is applied later, on
			// the render thread, so a mirror built first would be dressed for the
			// previous floor's slice of the year.
			applyIdentityGlobals(identity);
			JSONObject weatherData = data.optJSONObject("weather");
			if (weatherData != null) {
				if (weatherData.has("cycleTurn")) Dungeon.cycleTurn = weatherData.getInt("cycleTurn");
				if (weatherData.has("calStart")) Dungeon.calendarStartDay = weatherData.getInt("calStart");
			}

			xyz.gabriwar.warpedpixeldungeon.levels.Level level =
					NetLevels.create(identity, branch, mapData, w, h, tilesTex, waterTex);
			level.color1 = data.optInt("color1", 0x004400);
			level.color2 = data.optInt("color2", 0x88CC44);

			String visitedRLE = data.optString("visited", "");
			if (!visitedRLE.isEmpty()) {
				level.visited = StateSerializer.decodeBooleanRLE(visitedRLE, w * h);
			}
			String fovRLE = data.optString("fov", "");
			if (!fovRLE.isEmpty()) {
				level.heroFOV = StateSerializer.decodeBooleanRLE(fovRLE, w * h);
			}
			String mappedRLE = data.optString("mapped", "");
			if (!mappedRLE.isEmpty()) {
				level.mapped = StateSerializer.decodeBooleanRLE(mappedRLE, w * h);
			}

			// Pick perspective hero data:
			// - spectator (myNetHeroId == -1): use host's `hero` field
			// - player: find my own netHero in netHeroes array, use that
			// The hero we don't pick gets rendered as a remote SpectatorMob below.
			JSONObject hostHeroData = data.getJSONObject("hero");
			JSONArray netHeroesArr = data.optJSONArray("netHeroes");
			int myId = NetManager.getMyNetHeroId();
			JSONObject heroData = hostHeroData;
			if (myId != -1 && netHeroesArr != null) {
				for (int i = 0; i < netHeroesArr.length(); i++) {
					JSONObject nh = netHeroesArr.getJSONObject(i);
					if (nh.optInt("id", -1) == myId) {
						heroData = nh;
						break;
					}
				}
			}
			final boolean isPlayerPerspective = (heroData != hostHeroData);

			// PLAYER mode has its own FoV — discard the host's. Arrays start empty
			// and get filled by Dungeon.observe() once the scene is up.
			if (isPlayerPerspective) {
				int len = w * h;
				level.heroFOV = new boolean[len];
				level.visited = new boolean[len];
				level.mapped  = new boolean[len];
			}
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] handleFullState myId=" + myId
					+ " hostHeroId=" + hostHeroData.optInt("id", -1)
					+ " netHeroesCount=" + (netHeroesArr == null ? 0 : netHeroesArr.length())
					+ " perspective=" + (isPlayerPerspective ? "PLAYER" : "SPECTATOR")
					+ " mainHeroId=" + heroData.optInt("id", -1)
					+ " mainHeroPos=" + heroData.optInt("pos", -1));

			final int netTier = heroData.optInt("tier", 0);
			final int netShield = heroData.optInt("shield", 0);
			Hero hero = new Hero() {
				private int spectatorTier = netTier;
				private int spectatorShield = netShield;
				@Override
				public int tier() { return spectatorTier; }
				@Override
				public int shielding() { return spectatorShield; }
				public void setNetTier(int t) { spectatorTier = t; }
				public void setNetShield(int s) { spectatorShield = s; }
				@Override
				public void onMotionComplete() {
					// Centralized in NetVisuals.onSpriteMotionComplete (CharSprite hook).
				}
				@Override
				public void onAttackComplete() {
					if (sprite != null) sprite.idle();
				}
				@Override
				public void onOperateComplete() {
					if (sprite != null) sprite.idle();
				}
			};
			hero.pos = heroData.getInt("pos");
			int clsOrd = heroData.optInt("cls", 0);
			HeroClass[] hcAll = HeroClass.values();
			hero.heroClass = (clsOrd >= 0 && clsOrd < hcAll.length) ? hcAll[clsOrd] : HeroClass.WARRIOR;
			hero.HP = heroData.getInt("hp");
			hero.HT = heroData.getInt("ht");
			hero.lvl = heroData.optInt("lvl", 1);
			hero.exp = heroData.optInt("exp", 0);
			hero.STR = heroData.optInt("str", 10);
			hero.ready = true;

			// Apply hero buffs
			applyBuffsToChar(hero, heroData.optJSONArray("buffs"));

			// Set Dungeon.hero early so applyInventory can access belongings
			Dungeon.hero = hero;
			applyInventory(heroData.optJSONObject("inv"));
			applyQuickslots(heroData.optJSONArray("qs"));

			int hostHeroId = heroData.optInt("id", -1);
			NetVisuals.setHostHeroId(hostHeroId);

			// Parse mobs
			trackedMobs.clear();
			JSONArray mobsArr = data.optJSONArray("mobs");
			if (mobsArr != null) {
				for (int i = 0; i < mobsArr.length(); i++) {
					JSONObject mobData = mobsArr.getJSONObject(i);
					SpectatorMob mob = new SpectatorMob();
					int mobId = mobData.getInt("id");
					mob.pos = mobData.getInt("pos");
					mob.HP = mobData.getInt("hp");
					mob.HT = mobData.getInt("ht");
					mob.netName = mobData.optString("name", "");
					mob.hostId = mobId;

					String spriteName = mobData.optString("sprite", "MobSprite");
					mob.spriteClass = resolveSpriteClass(spriteName);

					applyBuffsToChar(mob, mobData.optJSONArray("buffs"));

					level.mobs.add(mob);
					trackedMobs.put(mobId, mob);
				}
			}
			NetVisuals.setSpectatorMobs(trackedMobs);

			// Parse remote player heroes (everyone except whoever is "me" — see perspective swap above).
			// Uses netHeroesArr declared above. Includes the host hero too when I'm a player.
			trackedNetHeroes.clear();
			if (netHeroesArr != null) {
				for (int i = 0; i < netHeroesArr.length(); i++) {
					JSONObject nhData = netHeroesArr.getJSONObject(i);
					int nhId = nhData.getInt("id");
					// Skip whoever is me (I see myself via Dungeon.hero)
					if (nhId == NetManager.getMyNetHeroId()) continue;
					NetHeroMob nhMob = makeNetHeroMob(nhId, nhData, "Player");
					level.mobs.add(nhMob);
					trackedNetHeroes.put(nhId, nhMob);
					trackedMobs.put(nhId, nhMob); // also track for VFX
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] FS added netHero id=" + nhId
							+ " cls=" + nhMob.heroClass.title() + " pos=" + nhMob.pos);
				}
			}

			// Player perspective: also render the host hero as a remote sprite
			// (we adopted a netHero as Dungeon.hero, so the host becomes "the other guy")
			if (isPlayerPerspective) {
				int hostId = hostHeroData.optInt("id", -1);
				if (hostId != -1 && trackedNetHeroes.get(hostId) == null) {
					NetHeroMob hostMob = makeNetHeroMob(hostId, hostHeroData,
							NetManager.getHostHeroName());
					level.mobs.add(hostMob);
					trackedNetHeroes.put(hostId, hostMob);
					trackedMobs.put(hostId, hostMob);
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] FS added host as NetHeroMob id=" + hostId
							+ " cls=" + hostMob.heroClass.title()
							+ " pos=" + hostMob.pos);
				}
			}

			// Parse heaps
			applyHeaps(level, data.optJSONArray("heaps"));

			// Parse plants
			applyPlants(level, data.optJSONArray("plants"));

			// Parse traps
			applyTraps(level, data.optJSONArray("traps"));

			// Set Dungeon globals early
			Dungeon.level = level;
			Dungeon.hero = hero;
			Dungeon.depth = depth;
			Dungeon.branch = branch;
			Dungeon.gold = data.optInt("gold", 0);

			// Portals applied on the render thread below — Portals.records is a plain
			// HashMap also read by the render thread (WndPortal / cell-tap), so mutating
			// it here on the network read thread would race -> ConcurrentModificationException.
			final JSONArray portalsArr = data.optJSONArray("portals");

			// Parse blobs
			JSONArray blobsArr = data.optJSONArray("blobs");
			if (blobsArr != null) {
				applyBlobs(level, blobsArr);
			}

			// Store weather state and boss ID for after scene switch
			if (weatherData != null) {
				pendingWeather = weatherData;
			}
			pendingBossId = data.optInt("bossId", -1);

			// Capture host's Statistics + Notes blobs (if present) for restoration
			// after the local reset on the render thread. Without this, joiners see
			// blank journal/death screens regardless of host's actual progress.
			final String statsBlob = data.optString("stats", null);
			final String notesBlob = data.optString("notes", null);

			// Switch scene
			Game.runOnRenderThread(() -> {
				xyz.gabriwar.warpedpixeldungeon.Statistics.reset();
				xyz.gabriwar.warpedpixeldungeon.journal.Notes.reset();
				applyPortals(portalsArr);
				Dungeon.quickslot.reset();
				xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton.reset();
				Dungeon.droppedItems = new com.watabou.utils.SparseArray<>();
				Dungeon.chapters = new java.util.HashSet<>();
				Dungeon.energy = 0;
				Dungeon.pars = new int[100];
				Dungeon.LimitedDrops.reset();
				xyz.gabriwar.warpedpixeldungeon.Badges.reset();
				// Restore host snapshots over the freshly-reset state.
				if (statsBlob != null && !statsBlob.isEmpty() && !"null".equals(statsBlob)) {
					try {
						com.watabou.utils.Bundle sb = com.watabou.utils.Bundle.read(
								new java.io.ByteArrayInputStream(statsBlob.getBytes()));
						xyz.gabriwar.warpedpixeldungeon.Statistics.restoreFromBundle(sb);
					} catch (Throwable t) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
								"[NET-CLI] stats restore failed: " + t);
					}
				}
				if (notesBlob != null && !notesBlob.isEmpty() && !"null".equals(notesBlob)) {
					try {
						com.watabou.utils.Bundle nb = com.watabou.utils.Bundle.read(
								new java.io.ByteArrayInputStream(notesBlob.getBytes()));
						xyz.gabriwar.warpedpixeldungeon.journal.Notes.restoreFromBundle(nb);
					} catch (Throwable t) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
								"[NET-CLI] notes restore failed: " + t);
					}
				}
				xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.mode =
						xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.Mode.CONTINUE;
				Game.switchScene(GameScene.class);
			});

		} catch (Exception e) {
			e.printStackTrace();
			Game.runOnRenderThread(() -> {
				if (Game.scene() != null) {
					Game.scene().addToFront(new xyz.gabriwar.warpedpixeldungeon.net.ui.WndNetError(
							"Sync Error", e.toString(), null));
				}
			});
		}
	}

	// --- Pending state for after scene switch ---

	private static JSONObject pendingWeather;
	private static int pendingBossId = -1;

	public static void applyPendingWeather() {
		if (pendingWeather != null) {
			applyWeatherState(pendingWeather);
			pendingWeather = null;
		}
	}

	public static void applyPendingBoss(java.util.Map<Integer, Mob> mobs) {
		if (pendingBossId != -1 && mobs != null) {
			Mob boss = mobs.get(pendingBossId);
			if (boss != null) {
				BossHealthBar.assignBoss(boss);
			}
			pendingBossId = -1;
		}
	}

	// --- Delta ---

	private void handleDelta(JSONObject data) {
		//THE WINDOW IS GENERATED HERE, ON THE NETWORK THREAD. A slid or
		//re-derived surface window is a full generator pass over 30,976 cells
		//(~100ms): paying it inside the render-thread block below stalled a
		//frame every time the host's window moved. Only the adoption - the map
		//copy, the art layers and the flag maps - has to be on the render
		//thread, so the pass is staged out here and handed in.
		final OverworldLevel.Window stagedWindow;
		JSONObject preIdentity = data.optJSONObject("level");
		if (preIdentity != null && data.has("map")
				&& Dungeon.level != null
				//a packet for a floor we have already left is dropped below;
				//don't spend a generator pass on it
				&& (!data.has("depth") || (data.optInt("depth", -1) == Dungeon.depth
						&& data.optInt("branch", 0) == Dungeon.branch))) {
			stagedWindow = NetLevels.stageWindow(Dungeon.level, preIdentity);
		} else {
			stagedWindow = null;
		}

		Game.runOnRenderThread(() -> {
			try {
				if (Dungeon.level == null || Dungeon.hero == null) return;

				// Drop stale deltas from the previous floor. The host stamps each delta
				// with the depth/branch it describes; if we've since received a
				// LEVEL_CHANGE for a different floor, ignore this packet rather than
				// apply mob/hero/pos data against the wrong map (which can corrupt
				// trackedMobs or AIOOBE on `Dungeon.level.map[pos]`).
				if (data.has("depth")) {
					int dDepth = data.optInt("depth", -1);
					int dBranch = data.optInt("branch", 0);
					if (dDepth != Dungeon.depth || dBranch != Dungeon.branch) {
						return;
					}
				}

				// The surface window slid under us (a rebase) or was re-derived in
				// place (the season turned). Both re-label every cell index, so the
				// window has to be adopted BEFORE anything else in this packet is
				// read: the hero, mob and heap positions below are already against
				// the new origin. We're on the render thread here, which the
				// surface's art layers require — they rebuild straight into the
				// live scene.
				boolean windowMoved = false;
				JSONObject identity = data.optJSONObject("level");
				if (identity != null && data.has("map")) {
					applyIdentityGlobals(identity);
					JSONArray winMap = data.getJSONArray("map");
					int[] newMap = new int[winMap.length()];
					for (int i = 0; i < newMap.length; i++) newMap[i] = winMap.getInt(i);
					windowMoved = NetLevels.applyWindow(Dungeon.level, identity, newMap, stagedWindow);
					if (windowMoved) {
						GameScene.updateMap();
						GameScene.updateFog();
					}
				}

				// Replay VFX FIRST — this queues move steps per character.
				// Skipped across a window move: those steps were recorded against
				// the old origin and would walk sprites to the wrong cells. Chains
				// still in flight from earlier deltas name the old origin too, so
				// they go with them — otherwise startMoveChains() below would pick
				// one up and march a sprite off to a cell that has moved.
				NetVisuals.clearMovedSet();
				if (windowMoved) {
					NetVisuals.clearMoveQueues();
				} else if (data.has("vfx")) {
					NetVisuals.replay(data.getJSONArray("vfx"));
				}

				// Active hero (whoever's up next on host's actor loop). UI uses
				// this to highlight the current actor in the turn indicator.
				if (data.has("activeHeroId")) {
					NetManager.activeHeroId = data.optInt("activeHeroId", -1);
				}

				// Hero updates — perspective-aware:
				// - spectator: `hero` field -> Dungeon.hero (host hero)
				// - player:   `hero` field -> host's SpectatorMob; my own entry in netHeroes -> Dungeon.hero
				JSONObject hostHeroDelta = data.optJSONObject("hero");
				JSONArray nhArr = data.optJSONArray("netHeroes");
				int myId = NetManager.getMyNetHeroId();
				boolean isPlayerPerspective = (myId != -1);

				JSONObject mainHeroDelta;
				if (isPlayerPerspective) {
					// PLAYER: only consume my own netHero entry. If the host's
					// optimization dropped the netHeroes array (no change since
					// last broadcast), DON'T fall back to hostHeroDelta — that
					// would clobber Dungeon.hero (us) with the host's pos/HP/etc.
					// Leave mainHeroDelta null so applyMainHeroDelta is skipped.
					mainHeroDelta = null;
					if (nhArr != null) {
						for (int i = 0; i < nhArr.length(); i++) {
							JSONObject nh = nhArr.getJSONObject(i);
							if (nh.optInt("id", -1) == myId) {
								mainHeroDelta = nh;
								break;
							}
						}
					}
				} else {
					// SPECTATOR: follow the host hero.
					mainHeroDelta = hostHeroDelta;
				}

				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] handleDelta myId=" + myId
						+ " perspective=" + (isPlayerPerspective ? "PLAYER" : "SPECTATOR")
						+ " hasHostDelta=" + (hostHeroDelta != null)
						+ " hasNH=" + (nhArr != null ? nhArr.length() : 0)
						+ " mainPos=" + (mainHeroDelta == null ? -1 : mainHeroDelta.optInt("pos", -1))
						+ " mainHp=" + (mainHeroDelta == null ? -1 : mainHeroDelta.optInt("hp", -1)));

				int prevMyPos = (Dungeon.hero != null) ? Dungeon.hero.pos : -1;
				if (mainHeroDelta != null) {
					applyMainHeroDelta(mainHeroDelta);
				}
				boolean myPosChanged = (Dungeon.hero != null && Dungeon.hero.pos != prevMyPos);

				// In player mode, host hero is now a remote — push its delta to its SpectatorMob
				if (isPlayerPerspective && hostHeroDelta != null) {
					int hostId = hostHeroDelta.optInt("id", -1);
					if (hostId != -1) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] routing host delta to SpectatorMob id=" + hostId
								+ " pos=" + hostHeroDelta.optInt("pos", -1));
						applyRemoteHeroDelta(hostId, hostHeroDelta);
					}
				}

				// Gold
				if (data.has("gold")) {
					Dungeon.gold = data.getInt("gold");
				}

				// Portal registry
				if (data.has("portals")) {
					applyPortals(data.optJSONArray("portals"));
				}

				// Mob updates
				if (data.has("mobs")) {
					JSONArray mobs = data.getJSONArray("mobs");
					int levelLen = Dungeon.level.length();
					for (int i = 0; i < mobs.length(); i++) {
						JSONObject mobData = mobs.getJSONObject(i);
						int id = mobData.getInt("id");
						Mob mob = trackedMobs.get(id);

						if (mob != null) {
							int newPos = mobData.getInt("pos");
							if (newPos < 0 || newPos >= levelLen) continue; // skip malformed/mismatched delta entry
							mob.HP = mobData.getInt("hp");
							mob.HT = mobData.getInt("ht");

							if (mobData.has("buffs")) {
								applyBuffsToChar(mob, mobData.getJSONArray("buffs"));
							}

							mob.pos = newPos;
							if (!NetVisuals.wasMovedByVfx(id)
									&& !NetVisuals.hasPendingMoves(id)) {
								if (mob.sprite != null) {
									mob.sprite.place(newPos);
								}
							}
						} else {
							int newMobPos = mobData.getInt("pos");
							if (newMobPos < 0 || newMobPos >= levelLen) continue;
							SpectatorMob newMob = new SpectatorMob();
							newMob.pos = newMobPos;
							newMob.HP = mobData.getInt("hp");
							newMob.HT = mobData.getInt("ht");
							newMob.netName = mobData.optString("name", "");
							newMob.hostId = id;
							String spriteName = mobData.optString("sprite", "MobSprite");
							newMob.spriteClass = resolveSpriteClass(spriteName);
							if (mobData.has("buffs")) {
								applyBuffsToChar(newMob, mobData.getJSONArray("buffs"));
							}
							Dungeon.level.mobs.add(newMob);
							trackedMobs.put(id, newMob);
							GameScene.addSprite(newMob);
						}
					}
				}

				// Removed mobs
				if (data.has("removedMobs")) {
					JSONArray removed = data.getJSONArray("removedMobs");
					for (int i = 0; i < removed.length(); i++) {
						int id = removed.getInt(i);
						Mob mob = trackedMobs.remove(id);
						if (mob != null) {
							Dungeon.level.mobs.remove(mob);
							if (mob.sprite != null) {
								// Across a window move these aren't kills: they're the
								// villagers and wildlife the host parked when they
								// scrolled out. No death animation for them.
								if (windowMoved) mob.sprite.killAndErase();
								else mob.sprite.die();
							}
						}
					}
				}

				// Update remote player heroes (everyone except me — my data went to Dungeon.hero above)
				if (nhArr != null) {
					for (int i = 0; i < nhArr.length(); i++) {
						JSONObject nhData = nhArr.getJSONObject(i);
						int nhId = nhData.getInt("id");
						if (nhId == myId) continue;
						applyRemoteHeroDelta(nhId, nhData);
					}
				}

				// Start queued move chains AFTER positions are set
				NetVisuals.startMoveChains();

				// FOV update.
				// PLAYER mode: ignore the host's FoV/visited/mapped — each player
				// has their own perspective. Recompute locally from our hero's pos.
				// SPECTATOR mode: trust the host's broadcast (we follow host POV).
				boolean fovChanged = false;
				if (isPlayerPerspective) {
					// Only re-observe when our hero actually moved (or the window
					// moved under it, which re-derives every wall). Otherwise we'd
					// recompute FoV every delta tick (60+/s under broadcast spam),
					// which is expensive and produces no visual change.
					if (myPosChanged || windowMoved) {
						try {
							Dungeon.observe();
							fovChanged = true;
						} catch (Throwable t) {
							xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] local observe failed: " + t);
						}
					}
				} else {
					if (data.has("fov")) {
						Dungeon.level.heroFOV = StateSerializer.decodeBooleanRLE(
								data.getString("fov"), Dungeon.level.length());
						fovChanged = true;
					}
					if (data.has("visited")) {
						Dungeon.level.visited = StateSerializer.decodeBooleanRLE(
								data.getString("visited"), Dungeon.level.length());
						fovChanged = true;
					}
					if (data.has("mapped")) {
						Dungeon.level.mapped = StateSerializer.decodeBooleanRLE(
								data.getString("mapped"), Dungeon.level.length());
						fovChanged = true;
					}
				}
				if (fovChanged) {
					GameScene.updateFog();
					GameScene.afterObserve();
				}

				// Heaps
				if (data.has("heaps")) {
					Dungeon.level.heaps.clear();
					applyHeaps(Dungeon.level, data.getJSONArray("heaps"));
					GameScene.refreshNetHeaps();
				}

				// Plants
				if (data.has("plants")) {
					Dungeon.level.plants.clear();
					applyPlants(Dungeon.level, data.getJSONArray("plants"));
					for (Plant p : Dungeon.level.plants.valueList()) {
						GameScene.updateMap(p.pos);
					}
				}

				// Traps
				if (data.has("traps")) {
					for (Trap t : Dungeon.level.traps.valueList()) {
						GameScene.updateMap(t.pos);
					}
					Dungeon.level.traps.clear();
					applyTraps(Dungeon.level, data.getJSONArray("traps"));
					for (Trap t : Dungeon.level.traps.valueList()) {
						GameScene.updateMap(t.pos);
					}
				}

				// Cells
				if (data.has("cells")) {
					JSONArray cells = data.getJSONArray("cells");
					int cellMapLen = Dungeon.level.length();
					for (int i = 0; i < cells.length(); i++) {
						JSONObject cell = cells.getJSONObject(i);
						int idx = cell.getInt("i");
						if (idx < 0 || idx >= cellMapLen) continue;
						Dungeon.level.map[idx] = cell.getInt("v");
						GameScene.updateMap(idx);
					}
				}

				// Blobs
				if (data.has("blobs")) {
					applyBlobs(Dungeon.level, data.getJSONArray("blobs"));
				}

				// Weather
				if (data.has("weather")) {
					applyWeatherState(data.getJSONObject("weather"));
				}

				// Boss
				if (data.has("bossId")) {
					int bossId = data.getInt("bossId");
					if (bossId == -1) {
						BossHealthBar.assignBoss(null);
					} else {
						Mob boss = trackedMobs.get(bossId);
						if (boss != null) {
							BossHealthBar.assignBoss(boss);
						}
					}
				}

				// Log messages
				if (data.has("log")) {
					JSONArray logArr = data.getJSONArray("log");
					for (int i = 0; i < logArr.length(); i++) {
						GLog.update.dispatch(logArr.getString(i));
					}
				}

				// Per-hero log messages — show only the ones addressed to my hero.
				if (data.has("logFor")) {
					JSONArray arr = data.getJSONArray("logFor");
					int myLogId = NetManager.getMyNetHeroId();
					for (int i = 0; i < arr.length(); i++) {
						JSONObject o = arr.optJSONObject(i);
						if (o != null && o.optInt("h", -1) == myLogId) {
							GLog.update.dispatch(o.optString("t", ""));
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Apply a hero JSON block (from `hero` field for spectators, or own netHero entry
	 * for players) onto the local Dungeon.hero. Drives HP/HT/lvl/exp/buffs/inv/qs/pos.
	 */
	private void applyMainHeroDelta(JSONObject heroData) {
		try {
			int newPos = heroData.getInt("pos");
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] applyMainHeroDelta id=" + heroData.optInt("id", -1)
					+ " oldPos=" + Dungeon.hero.pos + " newPos=" + newPos
					+ " heroHostId=" + NetVisuals.getHostHeroId()
					+ " wasMovedByVfx=" + NetVisuals.wasMovedByVfx(NetVisuals.getHostHeroId())
					+ " hasPendingMoves=" + NetVisuals.hasPendingMoves(NetVisuals.getHostHeroId()));
			Dungeon.hero.HP = heroData.getInt("hp");
			Dungeon.hero.HT = heroData.getInt("ht");
			Dungeon.hero.lvl = heroData.optInt("lvl", Dungeon.hero.lvl);
			Dungeon.hero.exp = heroData.optInt("exp", Dungeon.hero.exp);
			Dungeon.hero.turnsTaken = heroData.optInt("turns", Dungeon.hero.turnsTaken);
			Dungeon.hero.queuedSteps = heroData.optInt("queued", 0);
			boolean newAtExit = heroData.optBoolean("atExit", false);
			if (newAtExit != Dungeon.hero.atExit) {
				Dungeon.hero.atExit = newAtExit;
				if (Dungeon.hero.sprite != null) Dungeon.hero.sprite.visible = !newAtExit;
				// Only show WndAtExit if THIS client owns the hero (player mode).
				// In spectator mode Dungeon.hero is the host's hero and the host
				// already has its own UI for the at-exit state.
				if (NetManager.getMyNetHeroId() != -1) {
					Game.runOnRenderThread(() -> {
						if (newAtExit) xyz.gabriwar.warpedpixeldungeon.net.ui.WndAtExit.show();
						else            xyz.gabriwar.warpedpixeldungeon.net.ui.WndAtExit.dismiss();
					});
				}
			}
			String owner = heroData.optString("owner", "");
			if (!owner.isEmpty()) Dungeon.hero.netOwnerName = owner;

			int shield = heroData.optInt("shield", 0);
			try {
				Dungeon.hero.getClass().getMethod("setNetShield", int.class)
						.invoke(Dungeon.hero, shield);
			} catch (Exception ignored) {}
			int tier = heroData.optInt("tier", 0);
			try {
				Dungeon.hero.getClass().getMethod("setNetTier", int.class)
						.invoke(Dungeon.hero, tier);
			} catch (Exception ignored) {}

			if (heroData.has("buffs")) {
				applyBuffsToChar(Dungeon.hero, heroData.getJSONArray("buffs"));
			}
			// Mirror host's hunger meter — buff-icon only sync would let the
			// client tick independently and starve while host is fine.
			if (heroData.has("hunger")) {
				int v = heroData.getInt("hunger");
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger hg =
						Dungeon.hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger.class);
				if (hg != null) hg.setLevel(v);
			}
			if (heroData.has("inv")) {
				applyInventory(heroData.getJSONObject("inv"));
			}
			// Mirror host's talents (tree progression + banked points). Without this
			// WndHero on the client renders empty trees regardless of host progress.
			if (heroData.has("talents")) {
				String talentsBlob = heroData.optString("talents", "");
				if (!talentsBlob.isEmpty() && !"null".equals(talentsBlob)) {
					try {
						com.watabou.utils.Bundle tb = com.watabou.utils.Bundle.read(
								new java.io.ByteArrayInputStream(talentsBlob.getBytes()));
						xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent.restoreTalentsFromBundle(
								tb, Dungeon.hero);
					} catch (Throwable t) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
								"[NET-CLI] talents restore failed: " + t);
					}
				}
			}
			if (heroData.has("armorAbility")) {
				String abilityCls = heroData.optString("armorAbility", "");
				if (!abilityCls.isEmpty() && (Dungeon.hero.armorAbility == null
						|| !abilityCls.equals(Dungeon.hero.armorAbility.getClass().getName()))) {
					// Allowlist — never Class.forName on network input. A malicious host
					// could otherwise trigger arbitrary class loading + run constructor
					// side effects before the instanceof gate.
					Class<? extends xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility>
							allowed = ARMOR_ABILITY_ALLOWLIST.get(abilityCls);
					if (allowed != null) {
						try {
							Dungeon.hero.armorAbility = allowed.getDeclaredConstructor().newInstance();
						} catch (Throwable t) {
							xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
									"[NET-CLI] armorAbility instantiate failed: " + t);
						}
					} else {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
								"[NET-CLI] armorAbility rejected (not in allowlist): " + abilityCls);
					}
				}
			}
			if (heroData.has("qs")) {
				applyQuickslots(heroData.getJSONArray("qs"));
			}

			Dungeon.hero.pos = newPos;
			int heroHostId = NetVisuals.getHostHeroId();
			if (!NetVisuals.wasMovedByVfx(heroHostId)
					&& !NetVisuals.hasPendingMoves(heroHostId)) {
				if (Dungeon.hero.sprite != null) {
					Dungeon.hero.sprite.place(newPos);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Apply a hero JSON block onto a tracked SpectatorMob (host hero in player perspective,
	 * or any other player's hero in either perspective). Creates the SpectatorMob lazily.
	 */
	private void applyRemoteHeroDelta(int id, JSONObject nhData) {
		try {
			SpectatorMob nh = trackedNetHeroes.get(id);
			if (nh != null) {
				int newPos = nhData.getInt("pos");
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] applyRemoteHeroDelta UPDATE id=" + id
						+ " oldPos=" + nh.pos + " newPos=" + newPos
						+ " spriteNull=" + (nh.sprite == null)
						+ " wasMovedByVfx=" + NetVisuals.wasMovedByVfx(id)
						+ " hasPendingMoves=" + NetVisuals.hasPendingMoves(id));
				if (newPos != nh.pos && nh.sprite != null
						&& !NetVisuals.wasMovedByVfx(id)
						&& !NetVisuals.hasPendingMoves(id)) {
					nh.sprite.place(newPos);
				}
				// No motion activity for this hero in this delta and the chain queue is
				// empty → snap sprite to idle. Belt-and-suspenders against deltas that
				// arrive after a chain naturally ended but the onMotionComplete path
				// raced (e.g. sprite anim still showing 'run' frames mid-transition).
				if (nh.sprite != null
						&& !NetVisuals.wasMovedByVfx(id)
						&& !NetVisuals.hasPendingMoves(id)
						&& !nh.sprite.isMoving
						&& nh.sprite.looping()) {
					// Don't force-idle mid-tween — that would snap the sprite to idle
					// pose while it visually keeps sliding to the dest. Only fire when
					// no motion is in flight (chain naturally ended but anim stuck).
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] force idle id=" + id
							+ " (delta has no motion this tick + queue empty)");
					nh.sprite.idle();
				}
				nh.pos = newPos;
				nh.HP = nhData.getInt("hp");
				nh.HT = nhData.getInt("ht");
				if (nh instanceof NetHeroMob) {
					NetHeroMob nhm = (NetHeroMob) nh;
					nhm.turnsTaken = nhData.optInt("turns", nhm.turnsTaken);
					nhm.queuedSteps = nhData.optInt("queued", 0);
					boolean atExit = nhData.optBoolean("atExit", false);
					nhm.atExit = atExit;
					if (nhm.sprite != null) nhm.sprite.visible = !atExit;
					String owner = nhData.optString("owner", "");
					if (!owner.isEmpty()) {
						nhm.ownerName = owner;
						nhm.netName = owner;
					}
				}
			} else {
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] applyRemoteHeroDelta CREATE id=" + id
						+ " pos=" + nhData.optInt("pos", -1));
				NetHeroMob newNh = makeNetHeroMob(id, nhData, "Player");
				Dungeon.level.mobs.add(newNh);
				trackedNetHeroes.put(id, newNh);
				trackedMobs.put(id, newNh);
				GameScene.addSprite(newNh);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/** Build a NetHeroMob from network data. Sets sprite class to NetHeroSprite so it
	 *  paints with the right hero spritesheet via NetHeroSprite.link(). */
	private NetHeroMob makeNetHeroMob(int id, JSONObject d, String name) {
		NetHeroMob m = new NetHeroMob();
		m.pos = d.optInt("pos", 0);
		m.HP = d.optInt("hp", 1);
		m.HT = d.optInt("ht", 1);
		m.hostId = id;
		int nhClsOrd = d.optInt("cls", 0);
		xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass[] nhHcAll =
				xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass.values();
		m.heroClass = (nhClsOrd >= 0 && nhClsOrd < nhHcAll.length)
				? nhHcAll[nhClsOrd]
				: xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass.WARRIOR;
		m.tier = d.optInt("tier", 0);
		m.turnsTaken = d.optInt("turns", 0);
		m.queuedSteps = d.optInt("queued", 0);
		// Prefer the player's own multiplayerName (sent in `owner`) for the indicator
		// label; fall back to the caller-supplied default ("Player" or hostName).
		m.ownerName = d.optString("owner", "");
		m.netName = (m.ownerName != null && !m.ownerName.isEmpty()) ? m.ownerName : name;
		m.spriteClass = NetHeroSprite.class;
		return m;
	}

	// --- Inventory Application ---

	private static void applyInventory(org.json.JSONObject inv) {
		if (inv == null || Dungeon.hero == null) return;
		try {
			Dungeon.hero.belongings.backpack.items.clear();

			// Add equipped items first (marked with [E] prefix)
			String[] slots = {"weapon", "armor", "artifact", "misc", "ring"};
			for (String slot : slots) {
				if (inv.has(slot)) {
					Item item = deserializeItem(inv.getJSONObject(slot), true);
					if (item != null) {
						Dungeon.hero.belongings.backpack.items.add(item);
					}
				}
			}

			// Backpack items
			JSONArray items = inv.optJSONArray("items");
			if (items != null) {
				for (int i = 0; i < items.length(); i++) {
					Item item = deserializeItem(items.getJSONObject(i), false);
					if (item != null) {
						Dungeon.hero.belongings.backpack.items.add(item);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Item deserializeItem(JSONObject data, boolean equipped) {
		try {
			final int img = data.optInt("img", 0);
			final String itemName = data.optString("name", "???");
			final String itemDesc = data.optString("desc", "");
			final boolean identified = data.optBoolean("id", false);
			Item item = new Item() {
				{ image = img; }
				@Override public String name() { return itemName; }
				@Override public String desc() { return itemDesc; }
				@Override public boolean isIdentified() { return identified; }
			};
			item.quantity(data.optInt("qty", 1));
			item.cursed = data.optBoolean("cursed", false);
			item.cursedKnown = data.optBoolean("cursedK", false);
			return item;
		} catch (Exception e) {
			return null;
		}
	}

	// --- Quickslot Application ---

	private static void applyQuickslots(JSONArray qsArr) {
		if (qsArr == null) return;
		try {
			Dungeon.quickslot.reset();
			for (int i = 0; i < qsArr.length(); i++) {
				JSONObject slot = qsArr.getJSONObject(i);
				int slotIdx = slot.getInt("s");
				if (slotIdx < 0 || slotIdx >= xyz.gabriwar.warpedpixeldungeon.QuickSlot.SIZE) continue;
				final int img = slot.optInt("img", 0);
				final String itemName = slot.optString("name", "");
				final int qty = slot.optInt("qty", 1);
				Item item = new Item() {
					{ image = img; }
					@Override public String name() { return itemName; }
				};
				item.quantity(qty);
				Dungeon.quickslot.setSlot(slotIdx, item);
			}
			xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton.refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --- Buff Application ---

	private static void applyBuffsToChar(xyz.gabriwar.warpedpixeldungeon.actors.Char ch, JSONArray buffsArr) {
		if (buffsArr == null) return;
		try {
			// Collect existing spectator buffs
			java.util.ArrayList<SpectatorBuff> existing = new java.util.ArrayList<>();
			for (Buff b : ch.buffs()) {
				if (b instanceof SpectatorBuff) existing.add((SpectatorBuff) b);
			}

			// Check if icon set matches — can update in-place
			boolean canUpdate = existing.size() == buffsArr.length();
			if (canUpdate) {
				for (int i = 0; i < buffsArr.length(); i++) {
					if (existing.get(i).getIconId() != buffsArr.getJSONObject(i).getInt("icon")) {
						canUpdate = false;
						break;
					}
				}
			}

			if (canUpdate) {
				// Same icon set — just update fade/text in-place
				for (int i = 0; i < buffsArr.length(); i++) {
					JSONObject b = buffsArr.getJSONObject(i);
					existing.get(i).updateVisuals(
							b.getInt("icon"), b.getInt("type"),
							(float) b.getDouble("fade"), b.optString("txt", ""));
				}
			} else {
				// Icon set changed — full rebuild
				for (SpectatorBuff sb : existing) {
					sb.detach();
				}
				for (int i = 0; i < buffsArr.length(); i++) {
					JSONObject b = buffsArr.getJSONObject(i);
					SpectatorBuff sb = new SpectatorBuff(
							b.getInt("icon"), b.getInt("type"),
							(float) b.getDouble("fade"), b.optString("txt", ""));
					sb.attachTo(ch);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --- Heap Application ---

	private static void applyHeaps(xyz.gabriwar.warpedpixeldungeon.levels.Level level, JSONArray heapsArr) {
		if (heapsArr == null) return;
		try {
			for (int i = 0; i < heapsArr.length(); i++) {
				JSONObject heapData = heapsArr.getJSONObject(i);
				Heap heap = new Heap();
				heap.pos = heapData.getInt("pos");
				int heapTypeOrd = heapData.optInt("type", 0);
			Heap.Type[] heapTypes = Heap.Type.values();
			heap.type = (heapTypeOrd >= 0 && heapTypeOrd < heapTypes.length)
					? heapTypes[heapTypeOrd] : Heap.Type.HEAP;
				Item placeholder = new Item();
				placeholder.image = heapData.optInt("sprite", 0);
				heap.drop(placeholder);
				heap.seen = true;
				level.heaps.put(heap.pos, heap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --- Plant Application ---

	private static void applyPlants(xyz.gabriwar.warpedpixeldungeon.levels.Level level, JSONArray plantsArr) {
		if (plantsArr == null) return;
		try {
			for (int i = 0; i < plantsArr.length(); i++) {
				JSONObject p = plantsArr.getJSONObject(i);
				SpectatorPlant plant = new SpectatorPlant();
				plant.pos = p.getInt("pos");
				plant.image = p.getInt("img");
				level.plants.put(plant.pos, plant);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --- Trap Application ---

	private static void applyTraps(xyz.gabriwar.warpedpixeldungeon.levels.Level level, JSONArray trapsArr) {
		if (trapsArr == null) return;
		try {
			for (int i = 0; i < trapsArr.length(); i++) {
				JSONObject t = trapsArr.getJSONObject(i);
				SpectatorTrap trap = new SpectatorTrap();
				trap.pos = t.getInt("pos");
				trap.color = t.getInt("col");
				trap.shape = t.getInt("shp");
				trap.visible = t.getBoolean("vis");
				trap.active = t.getBoolean("act");
				level.traps.put(trap.pos, trap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --- Blob Application ---

	@SuppressWarnings("unchecked")
	private static void applyBlobs(xyz.gabriwar.warpedpixeldungeon.levels.Level level, JSONArray blobsArr) {
		try {
			for (int i = 0; i < blobsArr.length(); i++) {
				JSONObject blobData = blobsArr.getJSONObject(i);
				String className = blobData.getString("cls");
				int volume = blobData.getInt("vol");

				// Allowlist: only load classes from the game's own blobs package.
				// Class.forName triggers static initializers before assignability can be checked,
				// so we guard by package prefix — mirroring the approach in resolveSpriteClass.
				if (!className.startsWith("xyz.gabriwar.warpedpixeldungeon.actors.blobs.")
						&& !className.startsWith("com.shatteredpixel.shatteredpixeldungeon.actors.blobs.")) {
					continue;
				}
				Class<?> cls;
				try {
					cls = Class.forName(className);
				} catch (ClassNotFoundException e) {
					continue;
				}

				if (!Blob.class.isAssignableFrom(cls)) continue;
				Class<? extends Blob> blobClass = (Class<? extends Blob>) cls;

				if (volume <= 0) {
					Blob existing = level.blobs.get(blobClass);
					if (existing != null) {
						existing.volume = 0;
						if (existing.cur != null) java.util.Arrays.fill(existing.cur, 0);
						if (existing.emitter != null) existing.emitter.on = false;
					}
					continue;
				}

				Blob blob = level.blobs.get(blobClass);
				if (blob == null) {
					blob = Reflection.newInstance(blobClass);
					if (blob == null) continue;
					level.blobs.put(blobClass, blob);
				}

				int len = blobData.getInt("len");
				if (len < 0 || len > level.length()) continue; // guard against OOM via oversized len
				if (blob.cur == null || blob.cur.length != len) {
					blob.cur = new int[len];
					blob.off = new int[len];
				} else {
					java.util.Arrays.fill(blob.cur, 0);
				}

				blob.volume = volume;
				blob.alwaysVisible = blobData.optBoolean("vis", false);

				JSONArray cells = blobData.getJSONArray("cells");
				JSONArray vals = blobData.getJSONArray("vals");
				for (int j = 0; j < cells.length(); j++) {
					int cidx = cells.getInt(j);
					if (cidx < 0 || cidx >= len) continue;
					blob.cur[cidx] = vals.getInt(j);
				}

				blob.area.setEmpty();
				blob.setupArea();
				GameScene.addNetBlobSprite(blob);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --- Weather Application ---

	private static void applyWeatherState(JSONObject weather) {
		try {
			// Apply visual overlay effects
			if (weather.has("precipType") && weather.has("precipRate")) {
				PrecipType pt = PrecipType.valueOf(weather.getString("precipType"));
				float rate = (float) weather.getDouble("precipRate");
				WeatherOverlay overlay = GameScene.getWeatherOverlay();
				if (overlay != null) {
					overlay.setPrecipitation(pt, rate);
				}
			}

			if (weather.has("storming")) {
				WeatherOverlay.netStormingOverride = weather.getBoolean("storming");
			}

			if (weather.has("ambientType")) {
				String ambName = weather.getString("ambientType");
				WeatherOverlay overlay = GameScene.getWeatherOverlay();
				if (overlay != null) {
					WeatherOverlay.AmbientType ambient;
					switch (ambName) {
						case "FIREFLIES":     ambient = WeatherOverlay.AmbientType.FIREFLIES; break;
						case "AUTUMN_LEAVES":  ambient = WeatherOverlay.AmbientType.AUTUMN_LEAVES; break;
						case "SPRING_PETALS":  ambient = WeatherOverlay.AmbientType.SPRING_PETALS; break;
						case "MIST":           ambient = WeatherOverlay.AmbientType.MIST; break;
						case "DUST":           ambient = WeatherOverlay.AmbientType.DUST; break;
						case "ASH":            ambient = WeatherOverlay.AmbientType.ASH; break;
						case "STEAM":          ambient = WeatherOverlay.AmbientType.STEAM; break;
						case "CORONA":         ambient = WeatherOverlay.AmbientType.CORONA; break;
						case "DRIP":           ambient = WeatherOverlay.AmbientType.DRIP; break;
						default:               ambient = WeatherOverlay.AmbientType.NONE; break;
					}
					overlay.setAmbient(ambient);
				}
				if ("AURORA".equals(ambName)) GameScene.showAurora();
				else GameScene.hideAurora();
				if ("RAINBOW".equals(ambName)) GameScene.showRainbow();
				else GameScene.hideRainbow();
			}

			if (weather.has("tint") && weather.has("brightness")) {
				JSONArray tintArr = weather.getJSONArray("tint");
				float[] tint = new float[tintArr.length()];
				for (int i = 0; i < tintArr.length(); i++) {
					tint[i] = (float) tintArr.getDouble(i);
				}
				float brightness = (float) weather.getDouble("brightness");
				GameScene.setDayNightTint(tint, brightness);
			}

			// Apply full climate state for SundialIndicator / info windows
			if (weather.has("surfTemp")) {
				xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.restoreFromNetwork(
						(float) weather.getDouble("surfTemp"),
						(float) weather.getDouble("surfHum"),
						(float) weather.getDouble("surfPres"),
						(float) weather.getDouble("windSpd"),
						(float) weather.getDouble("windDir"),
						(float) weather.getDouble("cloud"),
						(float) weather.getDouble("precipRate"),
						weather.getString("precipType"),
						weather.getString("wState"),
						weather.getBoolean("storming"),
						weather.getBoolean("aurora"),
						weather.getBoolean("rainbow"),
						weather.getBoolean("solarEcl"),
						weather.getBoolean("lunarEcl"));
			}

			// Sync Dungeon time counters for day/night cycle and calendar
			if (weather.has("cycleTurn")) {
				Dungeon.cycleTurn = weather.getInt("cycleTurn");
			}
			if (weather.has("calStart")) {
				Dungeon.calendarStartDay = weather.getInt("calStart");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --- Helpers ---

	@SuppressWarnings("unchecked")
	private Class<? extends CharSprite> resolveSpriteClass(String simpleName) {
		String fullName = "xyz.gabriwar.warpedpixeldungeon.sprites." + simpleName;
		try {
			Class<?> cls = Class.forName(fullName);
			if (CharSprite.class.isAssignableFrom(cls)) {
				return (Class<? extends CharSprite>) cls;
			}
		} catch (ClassNotFoundException ignored) {}
		return MobSprite.class;
	}

	// --- Spectator Data Types ---

	public static class SpectatorMob extends Mob {
		public String netName = "";
		public int hostId = -1;
		{
			state = PASSIVE;
		}
		@Override
		public String name() {
			return netName.isEmpty() ? super.name() : netName;
		}
		@Override
		public void onMotionComplete() {
			// Chain advancement + idle is handled centrally by
			// NetVisuals.onSpriteMotionComplete (called from CharSprite.onComplete)
			// for every net-driven sprite, so no per-class logic needed here.
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] NetHeroMob.onMotionComplete id=" + hostId
					+ " pos=" + pos);
		}
		@Override
		public void onAttackComplete() {
			if (sprite != null) sprite.idle();
		}
		@Override
		public void onOperateComplete() {
			if (sprite != null) sprite.idle();
		}
	}

	/** SpectatorMob variant that carries the heroClass + armor tier so NetHeroSprite
	 *  can paint it with the correct hero spritesheet on link(). */
	public static class NetHeroMob extends SpectatorMob {
		public xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass heroClass =
				xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass.WARRIOR;
		public int tier = 0;
		public int turnsTaken = 0;
		public int queuedSteps = 0;
		public String ownerName = "";
		public boolean atExit = false;
	}

	public static class SpectatorPlant extends Plant {
		@Override
		public void activate( xyz.gabriwar.warpedpixeldungeon.actors.Char ch ) {}
		@Override
		public void storeInBundle(com.watabou.utils.Bundle bundle) {}
		@Override
		public void restoreFromBundle(com.watabou.utils.Bundle bundle) {}
	}

	public static class SpectatorTrap extends Trap {
		@Override
		public void activate() {}
		@Override
		public void storeInBundle(com.watabou.utils.Bundle bundle) {}
		@Override
		public void restoreFromBundle(com.watabou.utils.Bundle bundle) {}
	}
}
