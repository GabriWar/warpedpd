package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StateSerializer {

	// Delta tracking state
	private static int prevHeroPos = -1;
	private static int prevHeroHP = -1;
	private static int prevHeroLvl = -1;
	private static int prevHeroExp = -1;
	private static boolean prevHeroAtExit = false;
	private static int prevGold = -1;
	private static int prevHeapCount = -1;
	private static int prevPlantCount = -1;
	private static int prevTrapHash = 0;
	private static int prevBossId = -1;
	private static String prevPortalSig = null;
	private static String prevHeroBuffHash = "";
	private static HashMap<Integer, String> prevMobBuffHashes = new HashMap<>();
	private static HashMap<Integer, MobState> prevMobStates = new HashMap<>();
	private static boolean[] prevFov;
	private static boolean[] prevVisited;
	private static boolean[] prevMapped;
	private static final Set<Integer> dirtyCells = new HashSet<>();

	// Overworld sliding window: bumped when the window slid (rebase/jump) or was
	// re-derived in place for a new slice of the year (reseason). -1 off the surface.
	private static int prevWindowVersion = -1;

	// Weather delta tracking
	private static String prevPrecipType = "";
	private static float prevPrecipRate = -1;
	private static String prevAmbientType = "";
	private static float[] prevTint = null;
	private static float prevBrightness = -1;

	// Blob delta tracking
	private static final HashMap<String, Integer> prevBlobHashes = new HashMap<>();

	// Log message queue (filled by GLog hook) — broadcast to every client.
	private static final ConcurrentLinkedQueue<String> pendingLogMessages = new ConcurrentLinkedQueue<>();

	// Per-hero log queue — delivered only to the owning client (e.g. quest reward lines).
	private static class HeroLogMsg {
		final int heroId; final String text;
		HeroLogMsg(int heroId, String text) { this.heroId = heroId; this.text = text; }
	}
	private static final ConcurrentLinkedQueue<HeroLogMsg> pendingHeroLogMessages = new ConcurrentLinkedQueue<>();

	/** Host: queue a log line addressed to a single hero's client (not broadcast to all). */
	public static void recordLogMessageForHero(int heroId, String text) {
		if (NetManager.isHost()) {
			pendingHeroLogMessages.add(new HeroLogMsg(heroId, text));
		}
	}

	private static class MobState {
		int pos, hp;
		MobState(int pos, int hp) { this.pos = pos; this.hp = hp; }
	}

	public static void resetDeltaTracking() {
		prevHeroPos = -1;
		prevHeroHP = -1;
		prevHeroLvl = -1;
		prevHeroExp = -1;
		prevHeroAtExit = false;
		prevGold = -1;
		prevPortalSig = null;
		// Not blanked: every caller of this pairs it with a FULL_STATE, which already
		// carries the current window. Blanking would make the very next delta re-ship
		// the whole 176x176 map for nothing.
		prevWindowVersion = Dungeon.level instanceof OverworldLevel
				? ((OverworldLevel) Dungeon.level).windowVersion() : -1;
		prevHeapCount = -1;
		prevPlantCount = -1;
		prevTrapHash = 0;
		prevBossId = -1;
		prevHeroBuffHash = "";
		prevMobBuffHashes.clear();
		prevMobStates.clear();
		prevFov = null;
		prevVisited = null;
		prevMapped = null;
		synchronized (dirtyCells) {
			dirtyCells.clear();
		}
		prevPrecipType = "";
		prevPrecipRate = -1;
		prevAmbientType = "";
		prevTint = null;
		prevBrightness = -1;
		prevBlobHashes.clear();
		pendingLogMessages.clear();
		pendingHeroLogMessages.clear();
	}

	public static void markCellDirty(int cell) {
		synchronized (dirtyCells) {
			dirtyCells.add(cell);
		}
	}

	/** Called from GLog.i() on the host to queue messages for spectators */
	public static void recordLogMessage(String text) {
		if (NetManager.isHost()) {
			pendingLogMessages.add(text);
		}
	}

	// --- Level identity ---

	/**
	 * Everything a client needs to build the real level class locally: the concrete
	 * class name, the game seed (the whole surface is derived from it), the fog and
	 * sight rules, and — on the surface — the window this level is a view of.
	 * A handful of scalars; it rides every level packet.
	 */
	private static JSONObject serializeLevelIdentity(Level level) throws JSONException {
		JSONObject id = new JSONObject();
		id.put("cls", level.getClass().getSimpleName());
		id.put("seed", Dungeon.seed);
		// The run's challenge mask. Not decoration: DayNightCycle.phase() and
		// GameCalendar both branch on REAL_CLOCK, so a client that doesn't have it
		// reads a different time of day than the host — and shop hours, which are
		// now derived from (depth, branch) precisely so both machines agree, would
		// disagree again. A client's own mask is whatever its last local game left.
		id.put("chal", Dungeon.challenges);
		id.put("noFog", level.noFogOfWar());
		id.put("viewDist", level.viewDistance);
		// The host's slice of the year, explicitly: the surface's dressing is keyed
		// to it, and a client's own calendar has usually not caught up when the
		// level packet lands (the weather block that carries it is applied later).
		id.put("season", GameCalendar.season().ordinal());
		if (level instanceof OverworldLevel) {
			OverworldLevel ow = (OverworldLevel) level;
			id.put("wseed", ow.worldSeed());
			id.put("wx", ow.worldX());
			id.put("wy", ow.worldY());
			id.put("shift", ow.windowShift());
			id.put("wver", ow.windowVersion());
		} else if (level instanceof VillageHouseLevel) {
			// which door in the world this cottage sits behind — the host stamps it
			// walking through; a client never does
			VillageHouseLevel vh = (VillageHouseLevel) level;
			id.put("dwx", vh.doorWX);
			id.put("dwy", vh.doorWY);
		}
		return id;
	}

	// --- Full State ---

	public static JSONObject serializeFullState() {
		Level level = Dungeon.level;
		Hero hero = Dungeon.hero;
		if (level == null || hero == null) return null;

		try {
			JSONObject state = new JSONObject();
			state.put("depth", Dungeon.depth);
			state.put("branch", Dungeon.branch);
			// Who this level actually is, so the client can mirror the real class
			// (art layers, fog rules, terrain names) instead of a bare SpectatorLevel.
			state.put("level", serializeLevelIdentity(level));
			state.put("w", level.width());
			state.put("h", level.height());
			state.put("gold", Dungeon.gold);

			// Statistics + Notes — without these, joiners' journal/death screen show
			// zeros instead of host's actual progress (boss kills, deepest floor, etc).
			// Serialize via the same Bundle format the save system uses, then ship as
			// a JSON string so the client can decode and restore.
			try {
				com.watabou.utils.Bundle sb = new com.watabou.utils.Bundle();
				xyz.gabriwar.warpedpixeldungeon.Statistics.storeInBundle(sb);
				state.put("stats", sb.toString());
			} catch (Throwable ignored) {}
			try {
				com.watabou.utils.Bundle nb = new com.watabou.utils.Bundle();
				xyz.gabriwar.warpedpixeldungeon.journal.Notes.storeInBundle(nb);
				state.put("notes", nb.toString());
			} catch (Throwable ignored) {}

			// Map tiles
			JSONArray mapArr = new JSONArray();
			for (int tile : level.map) mapArr.put(tile);
			state.put("map", mapArr);

			// Visited/FOV/Mapped as RLE
			state.put("visited", encodeBooleanRLE(level.visited));
			state.put("fov", encodeBooleanRLE(level.heroFOV));
			state.put("mapped", encodeBooleanRLE(level.mapped));

			// Textures
			state.put("tilesTex", level.tilesTex());
			state.put("waterTex", level.waterTex());

			// Colors
			state.put("color1", level.color1);
			state.put("color2", level.color2);

			// Hero (host's own — quickslots come from global Dungeon.quickslot)
			state.put("hero", serializeHeroFull(hero, true));

			// Mobs
			JSONArray mobsArr = new JSONArray();
			for (Mob mob : level.mobs) {
				JSONObject mobObj = new JSONObject();
				mobObj.put("id", mob.id());
				mobObj.put("pos", mob.pos);
				mobObj.put("sprite", mob.spriteClass != null ? mob.spriteClass.getSimpleName() : "MobSprite");
				mobObj.put("hp", mob.HP);
				mobObj.put("ht", mob.HT);
				mobObj.put("name", mob.name());
				mobObj.put("buffs", serializeBuffs(mob));
				mobsArr.put(mobObj);
			}
			state.put("mobs", mobsArr);

			// Remote player heroes
			state.put("netHeroes", serializeNetHeroes());

			// Heaps
			state.put("heaps", serializeHeaps(level));

			// Plants
			state.put("plants", serializePlants(level));

			// Traps
			state.put("traps", serializeTraps(level));

			// Blobs
			JSONArray blobsArr = serializeBlobs(level);
			if (blobsArr.length() > 0) {
				state.put("blobs", blobsArr);
			}

			// Boss
			Mob boss = getBossMob();
			if (boss != null) {
				state.put("bossId", boss.id());
			}

			// Weather & ambient state
			state.put("weather", serializeWeather());

			// Shopkeeper fast-travel portals (shared across all heroes)
			state.put("portals", serializePortals());
			prevPortalSig = portalSig();

			// Snapshot for delta tracking
			prevHeroPos = hero.pos;
			prevHeroHP = hero.HP;
			prevHeroLvl = hero.lvl;
			prevHeroExp = hero.exp;
			prevHeroAtExit = hero.atExit;
			prevGold = Dungeon.gold;
			prevHeroBuffHash = buffHash(hero);
			prevMobStates.clear();
			prevMobBuffHashes.clear();
			for (Mob mob : level.mobs) {
				prevMobStates.put(mob.id(), new MobState(mob.pos, mob.HP));
				prevMobBuffHashes.put(mob.id(), buffHash(mob));
			}
			prevNetHeroSig.clear();
			for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h : NetManager.getNetHeroes()) {
				prevNetHeroSig.put(h.id(), netHeroSig(h));
			}
			prevWindowVersion = level instanceof OverworldLevel
					? ((OverworldLevel) level).windowVersion() : -1;
			prevHeapCount = heapHash(level);
			prevPlantCount = level.plants.valueList().size();
			prevTrapHash = trapHash(level);
			prevBossId = boss != null ? boss.id() : -1;
			prevFov = level.heroFOV != null ? Arrays.copyOf(level.heroFOV, level.heroFOV.length) : null;
			prevVisited = level.visited != null ? Arrays.copyOf(level.visited, level.visited.length) : null;
			prevMapped = level.mapped != null ? Arrays.copyOf(level.mapped, level.mapped.length) : null;
			synchronized (dirtyCells) {
				dirtyCells.clear();
			}
			snapshotWeather();
			snapshotBlobHashes(level);
			pendingLogMessages.clear();
			pendingHeroLogMessages.clear();

			return state;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	// --- Delta ---

	public static JSONObject serializeDelta() {
		Level level = Dungeon.level;
		Hero hero = Dungeon.hero;
		if (level == null || hero == null) return null;

		try {
			JSONObject delta = new JSONObject();
			boolean hasChanges = false;

			// Stamp the floor this delta describes — client drops it if its current floor
			// doesn't match (e.g. mid-transition or LEVEL_CHANGE arrives out of order).
			delta.put("depth", Dungeon.depth);
			delta.put("branch", Dungeon.branch);

			// The surface window slid (rebase/jump) or was re-derived in place for a
			// new slice of the year (reseason). Both rewrite map[] wholesale without
			// ever going through Level.set(), so markCellDirty never fires and the
			// "cells" delta ships nothing — the client's terrain would go stale for
			// good while every position in this same packet already refers to the new
			// origin. Ship the identity and the whole map; the receiver adopts the
			// window before it reads a single cell index out of this packet.
			if (level instanceof OverworldLevel) {
				int wver = ((OverworldLevel) level).windowVersion();
				if (wver != prevWindowVersion) {
					prevWindowVersion = wver;
					delta.put("level", serializeLevelIdentity(level));
					delta.put("w", level.width());
					delta.put("h", level.height());
					JSONArray winMap = new JSONArray();
					for (int tile : level.map) winMap.put(tile);
					delta.put("map", winMap);
					// dirty cells were recorded against the old origin; the full map
					// supersedes them
					synchronized (dirtyCells) {
						dirtyCells.clear();
					}
					// plants are the one collection tracked by count alone — a slide
					// that preserves the count still moves every index
					prevPlantCount = -1;
					hasChanges = true;
				}
			} else {
				prevWindowVersion = -1;
			}

			// Hero — only send if something material changed (pos/HP/lvl/exp/buffs).
			// Was unconditional before, which combined with the actor thread's wait-loop
			// (broadcastGameState() fires every iteration when doNext=false) flooded
			// the wire with ~60 deltas/sec while the host idled waiting for client input.
			{
				String heroBH = buffHash(hero);
				boolean heroChanged = hero.pos != prevHeroPos
						|| hero.HP  != prevHeroHP
						|| hero.lvl != prevHeroLvl
						|| hero.exp != prevHeroExp
						|| hero.atExit != prevHeroAtExit
						|| !heroBH.equals(prevHeroBuffHash);
				if (heroChanged) {
					delta.put("hero", serializeHeroFull(hero, true));
					prevHeroPos = hero.pos;
					prevHeroHP = hero.HP;
					prevHeroLvl = hero.lvl;
					prevHeroExp = hero.exp;
					prevHeroAtExit = hero.atExit;
					prevHeroBuffHash = heroBH;
					hasChanges = true;
				}
			}

			// Remote player heroes — only send if any of them actually changed
			// (pos / HP / queued / turns). Otherwise we'd spam an unchanging array
			// every broadcast tick.
			JSONArray nh = serializeNetHeroesIfChanged();
			if (nh != null) {
				delta.put("netHeroes", nh);
				hasChanges = true;
			}

			// Active hero — whoever has lowest cooldown is "next to act". Lets
			// every client (and the host's own UI) highlight the right hero in
			// the turn indicator.
			int activeId = computeActiveHeroId();
			NetManager.activeHeroId = activeId;
			delta.put("activeHeroId", activeId);

			// Gold
			if (Dungeon.gold != prevGold) {
				delta.put("gold", Dungeon.gold);
				prevGold = Dungeon.gold;
				hasChanges = true;
			}

			// Mob changes
			JSONArray mobChanges = new JSONArray();
			HashMap<Integer, MobState> newStates = new HashMap<>();
			HashMap<Integer, String> newBuffHashes = new HashMap<>();
			for (Mob mob : level.mobs) {
				int id = mob.id();
				newStates.put(id, new MobState(mob.pos, mob.HP));
				String mbh = buffHash(mob);
				newBuffHashes.put(id, mbh);
				MobState prev = prevMobStates.get(id);
				String prevBH = prevMobBuffHashes.get(id);
				boolean posHpChanged = prev == null || prev.pos != mob.pos || prev.hp != mob.HP;
				boolean buffChanged = prevBH == null || !prevBH.equals(mbh);
				if (posHpChanged || buffChanged) {
					JSONObject mobObj = new JSONObject();
					mobObj.put("id", id);
					mobObj.put("pos", mob.pos);
					mobObj.put("hp", mob.HP);
					mobObj.put("ht", mob.HT);
					mobObj.put("sprite", mob.spriteClass != null ? mob.spriteClass.getSimpleName() : "MobSprite");
					mobObj.put("name", mob.name());
					if (buffChanged) {
						mobObj.put("buffs", serializeBuffs(mob));
					}
					mobChanges.put(mobObj);
					hasChanges = true;
				}
			}
			// Detect removed mobs
			JSONArray removedMobs = new JSONArray();
			for (int id : prevMobStates.keySet()) {
				if (!newStates.containsKey(id)) {
					removedMobs.put(id);
					hasChanges = true;
				}
			}
			if (mobChanges.length() > 0) delta.put("mobs", mobChanges);
			if (removedMobs.length() > 0) delta.put("removedMobs", removedMobs);
			prevMobStates = newStates;
			prevMobBuffHashes = newBuffHashes;

			// FOV changes
			if (level.heroFOV != null) {
				if (prevFov == null || !Arrays.equals(prevFov, level.heroFOV)) {
					delta.put("fov", encodeBooleanRLE(level.heroFOV));
					prevFov = Arrays.copyOf(level.heroFOV, level.heroFOV.length);
					hasChanges = true;
				}
			}

			// Visited changes
			if (level.visited != null) {
				if (prevVisited == null || !Arrays.equals(prevVisited, level.visited)) {
					delta.put("visited", encodeBooleanRLE(level.visited));
					prevVisited = Arrays.copyOf(level.visited, level.visited.length);
					hasChanges = true;
				}
			}

			// Mapped changes
			if (level.mapped != null) {
				if (prevMapped == null || !Arrays.equals(prevMapped, level.mapped)) {
					delta.put("mapped", encodeBooleanRLE(level.mapped));
					prevMapped = Arrays.copyOf(level.mapped, level.mapped.length);
					hasChanges = true;
				}
			}

			// Dirty cells
			Set<Integer> cells;
			synchronized (dirtyCells) {
				if (!dirtyCells.isEmpty()) {
					cells = new HashSet<>(dirtyCells);
					dirtyCells.clear();
				} else {
					cells = null;
				}
			}
			if (cells != null) {
				JSONArray cellArr = new JSONArray();
				for (int cell : cells) {
					JSONObject c = new JSONObject();
					c.put("i", cell);
					c.put("v", level.map[cell]);
					cellArr.put(c);
				}
				delta.put("cells", cellArr);
				hasChanges = true;
			}

			// Blob changes
			JSONArray blobDeltas = serializeBlobDeltas(level);
			if (blobDeltas != null && blobDeltas.length() > 0) {
				delta.put("blobs", blobDeltas);
				hasChanges = true;
			}

			// Weather changes
			JSONObject weatherDelta = serializeWeatherDelta();
			if (weatherDelta != null) {
				delta.put("weather", weatherDelta);
				hasChanges = true;
			}

			// Portal registry changes (state/discovery flips when a gate is paid/killed)
			String portalSig = portalSig();
			if (!portalSig.equals(prevPortalSig)) {
				delta.put("portals", serializePortals());
				prevPortalSig = portalSig;
				hasChanges = true;
			}

			// Heap changes — track content hash, not just count
			int hh = heapHash(level);
			if (hh != prevHeapCount) {
				delta.put("heaps", serializeHeaps(level));
				prevHeapCount = hh;
				hasChanges = true;
			}

			// Plant changes
			int plantCount = level.plants.valueList().size();
			if (plantCount != prevPlantCount) {
				delta.put("plants", serializePlants(level));
				prevPlantCount = plantCount;
				hasChanges = true;
			}

			// Trap changes
			int th = trapHash(level);
			if (th != prevTrapHash) {
				delta.put("traps", serializeTraps(level));
				prevTrapHash = th;
				hasChanges = true;
			}

			// Boss changes
			Mob boss = getBossMob();
			int bossId = boss != null ? boss.id() : -1;
			if (bossId != prevBossId) {
				delta.put("bossId", bossId);
				prevBossId = bossId;
				hasChanges = true;
			}

			// Log messages
			if (!pendingLogMessages.isEmpty()) {
				JSONArray logArr = new JSONArray();
				String msg;
				int count = 0;
				while ((msg = pendingLogMessages.poll()) != null && count < 30) {
					logArr.put(msg);
					count++;
				}
				if (logArr.length() > 0) {
					delta.put("log", logArr);
					hasChanges = true;
				}
			}

			// Per-hero log messages (e.g. quest reward lines) — addressed by hero id.
			if (!pendingHeroLogMessages.isEmpty()) {
				JSONArray arr = new JSONArray();
				HeroLogMsg m;
				int count = 0;
				while ((m = pendingHeroLogMessages.poll()) != null && count < 30) {
					JSONObject o = new JSONObject();
					o.put("h", m.heroId);
					o.put("t", m.text);
					arr.put(o);
					count++;
				}
				if (arr.length() > 0) {
					delta.put("logFor", arr);
					hasChanges = true;
				}
			}

			// Visual events (particles, animations, states)
			JSONArray vfx = NetVisuals.drainEvents();
			if (vfx != null) {
				delta.put("vfx", vfx);
				hasChanges = true;
			}

			return hasChanges ? delta : null;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	// --- Buff Serialization ---

	private static JSONArray serializeBuffs(xyz.gabriwar.warpedpixeldungeon.actors.Char ch) throws JSONException {
		JSONArray arr = new JSONArray();
		for (Buff buff : ch.buffs()) {
			if (buff.icon() != BuffIndicator.NONE) {
				JSONObject b = new JSONObject();
				b.put("icon", buff.icon());
				b.put("type", buff.type.ordinal());
				b.put("fade", buff.iconFadePercent());
				b.put("txt", buff.iconTextDisplay());
				arr.put(b);
			}
		}
		return arr;
	}

	private static String buffHash(xyz.gabriwar.warpedpixeldungeon.actors.Char ch) {
		StringBuilder sb = new StringBuilder();
		for (Buff buff : ch.buffs()) {
			if (buff.icon() != BuffIndicator.NONE) {
				sb.append(buff.icon()).append(':');
				sb.append(buff.type.ordinal()).append(':');
				sb.append(buff.iconTextDisplay()).append(';');
			}
		}
		return sb.toString();
	}

	// --- Plant/Trap Serialization ---

	private static JSONArray serializePlants(Level level) throws JSONException {
		JSONArray arr = new JSONArray();
		for (Plant plant : level.plants.valueList()) {
			JSONObject p = new JSONObject();
			p.put("pos", plant.pos);
			p.put("img", plant.image);
			arr.put(p);
		}
		return arr;
	}

	private static JSONArray serializeTraps(Level level) throws JSONException {
		JSONArray arr = new JSONArray();
		for (Trap trap : level.traps.valueList()) {
			JSONObject t = new JSONObject();
			t.put("pos", trap.pos);
			t.put("col", trap.color);
			t.put("shp", trap.shape);
			t.put("vis", trap.visible);
			t.put("act", trap.active);
			arr.put(t);
		}
		return arr;
	}

	private static int trapHash(Level level) {
		int hash = level.traps.valueList().size();
		for (Trap t : level.traps.valueList()) {
			hash = hash * 31 + t.pos;
			hash = hash * 31 + (t.visible ? 1 : 0);
			hash = hash * 31 + (t.active ? 1 : 0);
		}
		return hash;
	}

	// --- Inventory Serialization ---

	private static JSONObject serializeInventory(Hero hero) throws JSONException {
		JSONObject inv = new JSONObject();

		// Equipped items
		if (hero.belongings.weapon() != null) {
			inv.put("weapon", serializeItem(hero.belongings.weapon()));
		}
		if (hero.belongings.armor() != null) {
			inv.put("armor", serializeItem(hero.belongings.armor()));
		}
		if (hero.belongings.artifact() != null) {
			inv.put("artifact", serializeItem(hero.belongings.artifact()));
		}
		if (hero.belongings.misc() != null) {
			inv.put("misc", serializeItem(hero.belongings.misc()));
		}
		if (hero.belongings.ring() != null) {
			inv.put("ring", serializeItem(hero.belongings.ring()));
		}

		// Backpack items (flat list)
		JSONArray items = new JSONArray();
		for (Item item : hero.belongings.backpack.items) {
			items.put(serializeItem(item));
		}
		inv.put("items", items);

		return inv;
	}

	private static JSONObject serializeItem(Item item) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("img", item.image());
		obj.put("name", item.name());
		obj.put("qty", item.quantity());
		obj.put("lvl", item.level());
		obj.put("cursed", item.cursed);
		obj.put("cursedK", item.cursedKnown);
		obj.put("id", item.isIdentified());
		// Description for client item examine — base Messages.get(this, "desc") doesn't
		// resolve on the deserialized anon Item subclass, so ship it from the host.
		try {
			String d = item.desc();
			if (d != null) obj.put("desc", d);
		} catch (Exception ignored) {}
		return obj;
	}

	// --- Net Hero Serialization ---

	/** Estimate # of game-time turns remaining for this hero's queued action.
	 *  Move = ceil(distance/speed) so a 4-cell click on a slow hero can show 8.
	 *  Attack = ceil(attackDelay) so slow weapons read as 2 instead of 1.
	 *  Other actions = 1 (single act). 0 when idle. */
	private static int computeQueuedTurns(xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h) {
		// Hero.speed() and attackDelay() dereference sprite (HeroSprite.sprint
		// inside speed). During interlevel transitions / scene rebuild the sprite
		// is briefly null — bail out with 0 instead of crashing.
		if (h.curAction == null || Dungeon.level == null || h.sprite == null) return 0;
		if (h.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Move) {
			int dst = ((xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Move) h.curAction).dst;
			int dist = Dungeon.level.distance(h.pos, dst);
			float speed = Math.max(0.1f, h.speed());
			return Math.max(0, (int) Math.ceil(dist / speed));
		}
		if (h.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Attack) {
			return Math.max(1, (int) Math.ceil(h.attackDelay()));
		}
		return 1;
	}

	/** Pick the hero (host or any netHero) with the lowest cooldown — that's
	 *  who's "up next" in the actor loop. UI uses this to highlight active turn. */
	private static int computeActiveHeroId() {
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero best = Dungeon.hero;
		float bestCd = best != null ? best.cooldown() : Float.MAX_VALUE;
		for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h : NetManager.allKnownNetHeroes()) {
			float cd = h.cooldown();
			if (cd < bestCd) { bestCd = cd; best = h; }
		}
		return best != null ? best.id() : -1;
	}

	private static JSONArray serializeNetHeroes() throws JSONException {
		JSONArray arr = new JSONArray();
		for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h : NetManager.getNetHeroes()) {
			// Rich data so a player client can use their own netHero entry
			// as the basis for their local Dungeon.hero (perspective swap).
			// Quickslots stay host-global for now — players see no preset qs.
			arr.put(serializeHeroFull(h, false));
		}
		return arr;
	}

	// Snapshot of net-hero state from the last broadcast — used to skip
	// the netHeroes array when nothing has changed since.
	private static final java.util.HashMap<Integer, String> prevNetHeroSig = new java.util.HashMap<>();

	private static String netHeroSig(xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h) {
		return h.pos + "|" + h.HP + "|" + h.HT + "|" + h.queuedSteps
				+ "|" + h.turnsTaken + "|" + (h.atExit ? "X" : "_")
				+ "|" + buffHash(h);
	}

	private static JSONArray serializeNetHeroesIfChanged() throws JSONException {
		java.util.HashMap<Integer, String> nowSig = new java.util.HashMap<>();
		boolean anyChanged = false;
		java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero> heroes =
				new java.util.ArrayList<>();
		for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h : NetManager.getNetHeroes()) {
			heroes.add(h);
			String sig = netHeroSig(h);
			nowSig.put(h.id(), sig);
			if (!sig.equals(prevNetHeroSig.get(h.id()))) {
				anyChanged = true;
			}
		}
		// Removed (disconnected/demoted) net heroes also count as a change.
		if (!anyChanged && nowSig.size() != prevNetHeroSig.size()) {
			anyChanged = true;
		}
		if (!anyChanged) return null;

		prevNetHeroSig.clear();
		prevNetHeroSig.putAll(nowSig);

		JSONArray arr = new JSONArray();
		for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h : heroes) {
			arr.put(serializeHeroFull(h, false));
		}
		return arr;
	}

	/**
	 * Full hero serialization. Used for the host's `hero` field AND for each entry
	 * in the netHeroes array. Lets a player client adopt their own netHero data
	 * as their local Dungeon.hero.
	 */
	private static JSONObject serializeHeroFull(xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h,
												boolean includeQuickslots) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("id", h.id());
		obj.put("pos", h.pos);
		obj.put("cls", h.heroClass.ordinal());
		obj.put("tier", h.tier());
		obj.put("hp", h.HP);
		obj.put("ht", h.HT);
		obj.put("lvl", h.lvl);
		obj.put("exp", h.exp);
		obj.put("str", h.STR);
		obj.put("shield", h.shielding());
		obj.put("turns", h.turnsTaken);
		obj.put("queued", computeQueuedTurns(h));
		// Net MP at-exit gate — clients show WndAtExit when their own hero
		// reports atExit=true; remote heroes get their sprite hidden via this
		// flag in the SpectatorMob delta path.
		obj.put("atExit", h.atExit);
		// Optional owner name so the lobby/turn indicator on clients can render
		// "Player <name>" instead of just "Hero <id>".
		if (h.netOwnerName != null && !h.netOwnerName.isEmpty()) {
			obj.put("owner", h.netOwnerName);
		}
		obj.put("buffs", serializeBuffs(h));
		// Hunger level — buffs only carry the icon, not the underlying meter.
		// Without this clients tick their own Hunger locally and drift / starve solo.
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger hg =
				h.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger.class);
		if (hg != null) obj.put("hunger", hg.hunger());
		obj.put("inv", serializeInventory(h));
		// Talents — class tree progression + banked points. Without these, clients show
		// empty talent trees on the WndHero screen even when the host has spent dozens of
		// points. Serialize via the same Bundle path the save system uses.
		try {
			com.watabou.utils.Bundle tb = new com.watabou.utils.Bundle();
			xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent.storeTalentsInBundle(tb, h);
			obj.put("talents", tb.toString());
		} catch (Throwable ignored) {}
		// Armor ability — class-specific subclass instance; name() identifies it
		// well enough for the client to display the slot in WndHero.
		if (h.armorAbility != null) {
			obj.put("armorAbility", h.armorAbility.getClass().getName());
		}
		if (includeQuickslots) {
			obj.put("qs", serializeQuickslots());
		}
		return obj;
	}

	// --- Quickslot Serialization ---

	private static JSONArray serializeQuickslots() throws JSONException {
		JSONArray arr = new JSONArray();
		for (int i = 0; i < xyz.gabriwar.warpedpixeldungeon.QuickSlot.SIZE; i++) {
			Item item = Dungeon.quickslot.getItem(i);
			if (item != null) {
				JSONObject slot = new JSONObject();
				slot.put("s", i);
				slot.put("img", item.image());
				slot.put("name", item.name());
				slot.put("qty", item.quantity());
				arr.put(slot);
			}
		}
		return arr;
	}

	private static int heapHash(Level level) {
		int hash = level.heaps.valueList().size();
		for (Heap heap : level.heaps.valueList()) {
			hash = hash * 31 + heap.pos;
			hash = hash * 31 + heap.type.ordinal();
			hash = hash * 31 + (heap.peek() != null ? heap.peek().image : -1);
			hash = hash * 31 + (heap.items != null ? heap.items.size() : 0);
		}
		return hash;
	}

	// --- Heap Serialization ---

	private static JSONArray serializeHeaps(Level level) throws JSONException {
		JSONArray heapsArr = new JSONArray();
		for (Heap heap : level.heaps.valueList()) {
			JSONObject heapObj = new JSONObject();
			heapObj.put("pos", heap.pos);
			heapObj.put("type", heap.type.ordinal());
			if (heap.peek() != null) {
				heapObj.put("sprite", heap.peek().image);
			}
			heapsArr.put(heapObj);
		}
		return heapsArr;
	}

	// --- Boss ---

	private static Mob getBossMob() {
		try {
			java.lang.reflect.Field f = BossHealthBar.class.getDeclaredField("boss");
			f.setAccessible(true);
			return (Mob) f.get(null);
		} catch (Exception e) {
			return null;
		}
	}

	// --- Blob Serialization ---

	private static JSONArray serializeBlobs(Level level) throws JSONException {
		JSONArray arr = new JSONArray();
		for (Map.Entry<Class<? extends Blob>, Blob> entry : level.blobs.entrySet()) {
			Blob blob = entry.getValue();
			if (blob.volume <= 0 || blob.cur == null) continue;

			JSONObject blobObj = serializeSingleBlob(blob);
			if (blobObj != null) arr.put(blobObj);
		}
		return arr;
	}

	private static JSONObject serializeSingleBlob(Blob blob) throws JSONException {
		if (blob.cur == null || blob.volume <= 0) return null;

		JSONObject obj = new JSONObject();
		obj.put("cls", blob.getClass().getName());
		obj.put("vol", blob.volume);
		obj.put("vis", blob.alwaysVisible);

		JSONArray cells = new JSONArray();
		JSONArray vals = new JSONArray();
		for (int i = 0; i < blob.cur.length; i++) {
			if (blob.cur[i] > 0) {
				cells.put(i);
				vals.put(blob.cur[i]);
			}
		}
		obj.put("cells", cells);
		obj.put("vals", vals);
		obj.put("len", blob.cur.length);

		return obj;
	}

	private static JSONArray serializeBlobDeltas(Level level) throws JSONException {
		JSONArray arr = new JSONArray();
		Set<String> currentBlobs = new HashSet<>();

		for (Map.Entry<Class<? extends Blob>, Blob> entry : level.blobs.entrySet()) {
			Blob blob = entry.getValue();
			String cls = blob.getClass().getName();
			currentBlobs.add(cls);

			if (blob.cur == null) continue;

			int hash = Arrays.hashCode(blob.cur);
			Integer prevHash = prevBlobHashes.get(cls);

			if (prevHash == null || prevHash != hash) {
				if (blob.volume > 0) {
					JSONObject blobObj = serializeSingleBlob(blob);
					if (blobObj != null) arr.put(blobObj);
				} else {
					JSONObject removal = new JSONObject();
					removal.put("cls", cls);
					removal.put("vol", 0);
					arr.put(removal);
				}
				prevBlobHashes.put(cls, hash);
			}
		}

		Set<String> removedBlobs = new HashSet<>(prevBlobHashes.keySet());
		removedBlobs.removeAll(currentBlobs);
		for (String cls : removedBlobs) {
			JSONObject removal = new JSONObject();
			removal.put("cls", cls);
			removal.put("vol", 0);
			arr.put(removal);
			prevBlobHashes.remove(cls);
		}

		return arr.length() > 0 ? arr : null;
	}

	private static void snapshotBlobHashes(Level level) {
		prevBlobHashes.clear();
		for (Map.Entry<Class<? extends Blob>, Blob> entry : level.blobs.entrySet()) {
			Blob blob = entry.getValue();
			if (blob.cur != null) {
				prevBlobHashes.put(blob.getClass().getName(), Arrays.hashCode(blob.cur));
			}
		}
	}

	// --- Portal Serialization ---

	private static JSONArray serializePortals() throws JSONException {
		JSONArray arr = new JSONArray();
		for (java.util.Map.Entry<Integer, Portals.Record> e : Portals.entries()) {
			JSONObject p = new JSONObject();
			p.put("depth", e.getKey());
			p.put("state", e.getValue().state.name());
			p.put("cell", e.getValue().cell);
			p.put("discovered", e.getValue().discovered);
			arr.put(p);
		}
		return arr;
	}

	// Cheap change-signature for delta gating (only state + discovery matter visually).
	private static String portalSig() {
		StringBuilder sb = new StringBuilder();
		for (java.util.Map.Entry<Integer, Portals.Record> e : Portals.entries()) {
			sb.append(e.getKey()).append(':')
			  .append(e.getValue().state.ordinal()).append(':')
			  .append(e.getValue().discovered ? 1 : 0).append('|');
		}
		return sb.toString();
	}

	// --- Weather Serialization ---

	private static JSONObject serializeWeather() throws JSONException {
		JSONObject w = new JSONObject();

		// Visual overlay data
		PrecipType pt = ClimateManager.localPrecipType();
		w.put("precipType", pt.name());
		w.put("precipRate", ClimateManager.localPrecipRate());
		ClimateManager.WeatherOverlayAmbient amb = ClimateManager.ambientType();
		w.put("ambientType", amb.name());
		float[] tint = DayNightCycle.phaseTintSmooth();
		JSONArray tintArr = new JSONArray();
		for (float v : tint) tintArr.put(v);
		w.put("tint", tintArr);
		w.put("brightness", DayNightCycle.brightnessMult());
		w.put("storming", ClimateManager.isStorming());

		// Full climate state for SundialIndicator / info windows
		w.put("surfTemp", ClimateManager.surfaceTemp());
		w.put("surfHum", ClimateManager.surfaceHumidity());
		w.put("surfPres", ClimateManager.surfacePressure());
		w.put("windSpd", ClimateManager.localWindSpeed());
		w.put("windDir", ClimateManager.surfaceWindDir());
		w.put("cloud", ClimateManager.cloudCover());
		w.put("wState", ClimateManager.weatherState().name());
		w.put("aurora", ClimateManager.isAurora());
		w.put("rainbow", ClimateManager.isRainbow());
		w.put("solarEcl", ClimateManager.isSolarEclipse());
		w.put("lunarEcl", ClimateManager.isLunarEclipse());
		w.put("cycleTurn", Dungeon.cycleTurn);
		w.put("calStart", Dungeon.calendarStartDay);

		return w;
	}

	private static JSONObject serializeWeatherDelta() throws JSONException {
		// Always send full climate state — it's small and changes smoothly
		return serializeWeather();
	}

	private static boolean tintEquals(float[] a, float[] b) {
		if (a.length != b.length) return false;
		for (int i = 0; i < a.length; i++) {
			if (Math.abs(a[i] - b[i]) > 0.01f) return false;
		}
		return true;
	}

	private static void snapshotWeather() {
		prevPrecipType = ClimateManager.localPrecipType().name();
		prevPrecipRate = ClimateManager.localPrecipRate();
		prevAmbientType = ClimateManager.ambientType().name();
		prevTint = DayNightCycle.phaseTintSmooth().clone();
		prevBrightness = DayNightCycle.brightnessMult();
	}

	// --- RLE encoding/decoding ---

	public static String encodeBooleanRLE(boolean[] arr) {
		if (arr == null || arr.length == 0) return "";
		StringBuilder sb = new StringBuilder();
		boolean current = arr[0];
		int count = 1;
		for (int i = 1; i < arr.length; i++) {
			if (arr[i] == current) {
				count++;
			} else {
				sb.append(count).append(current ? 'T' : 'F');
				current = arr[i];
				count = 1;
			}
		}
		sb.append(count).append(current ? 'T' : 'F');
		return sb.toString();
	}

	public static boolean[] decodeBooleanRLE(String rle, int length) {
		boolean[] result = new boolean[length];
		int idx = 0;
		int numStart = 0;
		for (int i = 0; i < rle.length(); i++) {
			char c = rle.charAt(i);
			if (c == 'T' || c == 'F') {
				int count;
				try {
					count = Integer.parseInt(rle.substring(numStart, i));
				} catch (NumberFormatException ex) {
					// Malformed run-length — bail out with whatever we've decoded so far
					// rather than crashing the receive thread.
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
							"[NET-CLI] decodeBooleanRLE: malformed run at offset " + numStart);
					return result;
				}
				boolean val = (c == 'T');
				for (int j = 0; j < count && idx < length; j++) {
					result[idx++] = val;
				}
				numStart = i + 1;
			}
		}
		if (idx != length) {
			// Under- or over-counted runs (mismatched floor dimensions, partial packet).
			// Already truncated by the idx<length guard above; just log so the symptom
			// is visible if it ever happens in the wild.
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
					"[NET-CLI] decodeBooleanRLE: filled=" + idx + " expected=" + length);
		}
		return result;
	}
}
