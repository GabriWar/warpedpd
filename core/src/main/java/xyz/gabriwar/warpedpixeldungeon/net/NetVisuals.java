package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.CheckedCell;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.AmbientSnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.AshParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChallengeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CoronaParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CorrosionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DustParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EnergyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.HailParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.HalomethaneFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.MistParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PitfallParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoppolarExplosionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.RainParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.RainbowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SacrificialParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SleetParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowStormParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SpectralWallParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SteamParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SunlightParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WebParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WindParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WoolParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Records and replays visual effects (particles, animations, sprite states)
 * for network synchronization between host and spectator.
 */
public class NetVisuals {

	private static final ConcurrentLinkedQueue<JSONObject> pendingEvents = new ConcurrentLinkedQueue<>();

	// --- Factory Registry ---
	// Maps string IDs to particle factories for network serialization

	private static final HashMap<String, Emitter.Factory> factoryById = new HashMap<>();
	private static final HashMap<Emitter.Factory, String> idByFactory = new HashMap<>();

	static {
		// Core combat/effect particles
		reg("flame",            FlameParticle.FACTORY);
		reg("snow",             SnowParticle.FACTORY);
		reg("snow_rising",      SnowParticle.RISING_FACTORY);
		reg("leaf",             LeafParticle.GENERAL);
		reg("leaf_level",       LeafParticle.LEVEL_SPECIFIC);
		reg("shadow_missile",   ShadowParticle.MISSILE);
		reg("shadow_curse",     ShadowParticle.CURSE);
		reg("shadow_up",        ShadowParticle.UP);
		reg("energy",           EnergyParticle.FACTORY);
		reg("blast",            BlastParticle.FACTORY);
		reg("smoke",            SmokeParticle.FACTORY);
		reg("elmo",             ElmoParticle.FACTORY);
		reg("shaft",            ShaftParticle.FACTORY);
		reg("spark",            SparkParticle.FACTORY);
		reg("earth",            EarthParticle.FACTORY);
		reg("poison",           PoisonParticle.SPLASH);
		reg("corrosion",        CorrosionParticle.SPLASH);
		reg("wool",             WoolParticle.FACTORY);
		reg("blood",            BloodParticle.FACTORY);
		reg("blood_burst",      BloodParticle.BURST);
		reg("web",              WebParticle.FACTORY);
		reg("rainbow",          RainbowParticle.BURST);
		reg("purple",           PurpleParticle.BURST);
		reg("sacrificial",      SacrificialParticle.FACTORY);
		reg("halomethane",      HalomethaneFlameParticle.FACTORY);

		// Weather / ambient particles
		reg("snowstorm",        SnowStormParticle.FACTORY);
		reg("ambient_snow",     AmbientSnowParticle.FACTORY);
		reg("hail",             HailParticle.FACTORY);
		reg("sleet",            SleetParticle.FACTORY);
		reg("rain",             RainParticle.FACTORY);
		reg("mist",             MistParticle.FACTORY);
		reg("steam",            SteamParticle.FACTORY);
		reg("drip",             DripParticle.FACTORY);
		reg("ash",              AshParticle.FACTORY);
		reg("dust",             DustParticle.FACTORY);
		reg("sunlight",         SunlightParticle.FACTORY);
		reg("corona",           CoronaParticle.FACTORY);
		reg("firefly",          FireflyParticle.FACTORY);
		reg("wind",             WindParticle.FACTORY);

		// Terrain / trap particles
		reg("flow",             FlowParticle.FACTORY);
		reg("pitfall4",         PitfallParticle.FACTORY4);
		reg("pitfall8",         PitfallParticle.FACTORY8);
		reg("spectral_wall",    SpectralWallParticle.FACTORY);
		reg("challenge",        ChallengeParticle.FACTORY);
		reg("poppolar",         PoppolarExplosionParticle.FACTORY);
	}

	private static void reg(String id, Emitter.Factory factory) {
		factoryById.put(id, factory);
		idByFactory.put(factory, id);
	}

	// --- Speck factories (use int type IDs) ---

	private static String speckId(int speckType) {
		return "speck_" + speckType;
	}

	// --- Recording Methods (called on HOST side) ---

	/**
	 * Record a cell particle burst. Called from CellEmitter hooks.
	 */
	public static void recordCellBurst(int cell, String emitterType, Emitter.Factory factory, int count) {
		if (!NetManager.isHost()) return;
		String fid = idByFactory.get(factory);
		if (fid == null) return; // unregistered factory, skip

		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "cb");
			evt.put("c", cell);
			evt.put("e", emitterType); // "get", "center", "floor", "bottom"
			evt.put("f", fid);
			evt.put("n", count);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a cell particle start (sustained emission).
	 */
	public static void recordCellStart(int cell, String emitterType, Emitter.Factory factory, float interval, int count) {
		if (!NetManager.isHost()) return;
		String fid = idByFactory.get(factory);
		if (fid == null) return;

		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "cs");
			evt.put("c", cell);
			evt.put("e", emitterType);
			evt.put("f", fid);
			evt.put("i", interval);
			evt.put("n", count);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a Speck-based cell burst.
	 */
	public static void recordCellSpeckBurst(int cell, String emitterType, int speckType, int count) {
		if (!NetManager.isHost()) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "cb");
			evt.put("c", cell);
			evt.put("e", emitterType);
			evt.put("f", speckId(speckType));
			evt.put("n", count);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a Speck-based cell start.
	 */
	public static void recordCellSpeckStart(int cell, String emitterType, int speckType, float interval, int count) {
		if (!NetManager.isHost()) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "cs");
			evt.put("c", cell);
			evt.put("e", emitterType);
			evt.put("f", speckId(speckType));
			evt.put("i", interval);
			evt.put("n", count);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a sprite animation (attack, zap, operate, die).
	 */
	public static void recordSpriteAnim(Char ch, String animType, int targetCell) {
		if (!NetManager.isHost() || ch == null) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "sa");
			evt.put("id", ch.id());
			evt.put("a", animType);
			evt.put("tc", targetCell);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a sprite state change (burning, frozen, etc).
	 */
	public static void recordSpriteState(Char ch, CharSprite.State state, boolean added) {
		if (!NetManager.isHost() || ch == null) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "ss");
			evt.put("id", ch.id());
			evt.put("s", state.name());
			evt.put("on", added);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a search effect (CheckedCell at pos, radiating from source).
	 */
	public static void recordCheckedCell(int cell, int source) {
		if (!NetManager.isHost()) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "cc");
			evt.put("c", cell);
			evt.put("s", source);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a character movement (one step).
	 */
	public static void recordMove(Char ch, int from, int to) {
		if (!NetManager.isHost() || ch == null) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "mv");
			evt.put("id", ch.id());
			evt.put("fr", from);
			evt.put("to", to);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a missile sprite (thrown item, projectile).
	 */
	public static void recordMissile(float fx, float fy, float tx, float ty, int image,
								   float angularSpeed, float angle) {
		if (!NetManager.isHost()) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "ms");
			evt.put("fx", fx);
			evt.put("fy", fy);
			evt.put("tx", tx);
			evt.put("ty", ty);
			evt.put("img", image);
			evt.put("as", angularSpeed);
			evt.put("a", angle);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record floating text (damage numbers, status text) on a sprite.
	 */
	public static void recordFloatingText(Char ch, String text, int color, int icon) {
		if (!NetManager.isHost() || ch == null) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "ft");
			evt.put("id", ch.id());
			evt.put("txt", text);
			evt.put("col", color);
			evt.put("ic", icon);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a damage flash on a sprite.
	 */
	public static void recordFlash(Char ch) {
		if (!NetManager.isHost() || ch == null) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "sf");
			evt.put("id", ch.id());
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	/**
	 * Record a blood burst on a sprite.
	 */
	public static void recordBloodBurst(Char ch, int damage) {
		if (!NetManager.isHost() || ch == null) return;
		try {
			JSONObject evt = new JSONObject();
			evt.put("t", "sb");
			evt.put("id", ch.id());
			evt.put("d", damage);
			pendingEvents.add(evt);
		} catch (JSONException ignored) {}
	}

	// --- Serialization ---

	/**
	 * Drain pending events into a JSON array for inclusion in delta.
	 * Returns null if no events pending.
	 */
	public static JSONArray drainEvents() {
		if (pendingEvents.isEmpty()) return null;
		JSONArray arr = new JSONArray();
		JSONObject evt;
		while ((evt = pendingEvents.poll()) != null) {
			arr.put(evt);
		}
		return arr.length() > 0 ? arr : null;
	}

	/**
	 * Clear all pending events (e.g. on level change).
	 */
	public static void clear() {
		pendingEvents.clear();
	}

	// --- Replay Methods (called on SPECTATOR side) ---

	/**
	 * Replay a batch of visual events received from the host.
	 */
	public static void replay(JSONArray events) {
		if (events == null) return;
		for (int i = 0; i < events.length(); i++) {
			try {
				replayEvent(events.getJSONObject(i));
			} catch (JSONException ignored) {}
		}
	}

	// Track which characters were animated by VFX move events this frame
	private static final java.util.HashSet<Integer> movedByVfx = new java.util.HashSet<>();
	// Queued move steps per character (host ID → queue of [from, to])
	private static final HashMap<Integer, java.util.LinkedList<int[]>> moveQueues = new HashMap<>();

	public static void clearMovedSet() {
		movedByVfx.clear();
		// Don't clear moveQueues — let ongoing chains from previous deltas finish
	}
	/**
	 * Drop every queued move step. Called when the ground under the sprites has
	 * been re-labelled (the surface window slid): the cells those steps name are
	 * other places now, and walking a sprite to one would strand it there.
	 */
	public static void clearMoveQueues() {
		moveQueues.clear();
	}
	public static boolean wasMovedByVfx(int id) { return movedByVfx.contains(id); }
	public static boolean hasPendingMoves(int id) {
		java.util.LinkedList<int[]> q = moveQueues.get(id);
		return q != null && !q.isEmpty();
	}

	// Sprite -> hostId reverse map, populated when we drive a sprite through the
	// move chain. Used by CharSprite.onComplete to look up which host actor an
	// in-flight tween belongs to so the chain can self-advance even for sprites
	// whose Char doesn't know about NetVisuals (Dungeon.hero on the client).
	private static final java.util.WeakHashMap<CharSprite, Integer> spriteToHostId =
			new java.util.WeakHashMap<>();

	/** Start all queued move chains after VFX replay is done */
	public static void startMoveChains() {
		for (java.util.Map.Entry<Integer, java.util.LinkedList<int[]>> entry : moveQueues.entrySet()) {
			java.util.LinkedList<int[]> queue = entry.getValue();
			if (!queue.isEmpty()) {
				int[] step = queue.poll();
				CharSprite sprite = findSprite(entry.getKey());
				NetManager.log("[NET-CLI] startMoveChain id=" + entry.getKey()
						+ " step=" + step[0] + "->" + step[1]
						+ " remaining=" + queue.size()
						+ " spriteNull=" + (sprite == null));
				if (sprite != null) {
					spriteToHostId.put(sprite, entry.getKey());
					sprite.interruptMotion();
					sprite.place(step[0]);
					sprite.move(step[0], step[1]);
				}
			}
		}
	}

	/** Hook from CharSprite.onComplete(motion). Returns true if the sprite is
	 *  participating in a net move chain — caller should skip its default idle
	 *  logic since we either kicked off the next step or will idle the sprite.*/
	public static boolean onSpriteMotionComplete(CharSprite sprite) {
		Integer hostId = spriteToHostId.get(sprite);
		if (hostId == null) return false;
		java.util.LinkedList<int[]> queue = moveQueues.get(hostId);
		if (queue != null && !queue.isEmpty()) {
			int[] step = queue.poll();
			NetManager.log("[NET-CLI] onSpriteMotionComplete advancing id=" + hostId
					+ " step=" + step[0] + "->" + step[1] + " remaining=" + queue.size());
			sprite.move(step[0], step[1]);
		} else {
			NetManager.log("[NET-CLI] onSpriteMotionComplete chain end id=" + hostId
					+ " -> idle");
			spriteToHostId.remove(sprite);
			sprite.idle();
		}
		return true;
	}

	/** Called from spectator onMotionComplete to chain the next step */
	public static boolean advanceMoveQueue(int hostId) {
		java.util.LinkedList<int[]> queue = moveQueues.get(hostId);
		if (queue != null && !queue.isEmpty()) {
			int[] step = queue.poll();
			CharSprite sprite = findSprite(hostId);
			NetManager.log("[NET-CLI] advanceMoveQueue id=" + hostId
					+ " step=" + step[0] + "->" + step[1]
					+ " remaining=" + queue.size()
					+ " spriteNull=" + (sprite == null));
			if (sprite != null) {
				sprite.move(step[0], step[1]);
				return true;
			}
		}
		return false;
	}

	private static void replayEvent(JSONObject evt) throws JSONException {
		String type = evt.getString("t");
		switch (type) {
			case "mv": replayMove(evt); break;
			case "cb": replayCellBurst(evt); break;
			case "cs": replayCellStart(evt); break;
			case "sa": replaySpriteAnim(evt); break;
			case "ss": replaySpriteState(evt); break;
			case "sf": replayFlash(evt); break;
			case "sb": replayBloodBurst(evt); break;
			case "ft": replayFloatingText(evt); break;
			case "ms": replayMissile(evt); break;
			case "cc": replayCheckedCell(evt); break;
		}
	}

	private static void replayMove(JSONObject evt) throws JSONException {
		int id = evt.getInt("id");
		int from = evt.getInt("fr");
		int to = evt.getInt("to");
		int levelLen = Dungeon.level != null ? Dungeon.level.length() : 0;
		if (from < 0 || from >= levelLen || to < 0 || to >= levelLen) return;

		if (!movedByVfx.contains(id)) {
			// First move for this character in this delta — clear stale queue
			moveQueues.remove(id);
		}
		movedByVfx.add(id);
		moveQueues.computeIfAbsent(id, k -> new java.util.LinkedList<>()).add(new int[]{from, to});
	}

	private static void replayCellBurst(JSONObject evt) throws JSONException {
		int cell = evt.getInt("c");
		String emType = evt.getString("e");
		String fid = evt.getString("f");
		int count = evt.getInt("n");

		Emitter.Factory factory = resolveFactory(fid);
		if (factory == null) return;

		Emitter emitter = getEmitter(cell, emType);
		if (emitter != null) {
			emitter.burst(factory, count);
		}
	}

	private static void replayCellStart(JSONObject evt) throws JSONException {
		int cell = evt.getInt("c");
		String emType = evt.getString("e");
		String fid = evt.getString("f");
		float interval = (float) evt.getDouble("i");
		int count = evt.getInt("n");

		Emitter.Factory factory = resolveFactory(fid);
		if (factory == null) return;

		Emitter emitter = getEmitter(cell, emType);
		if (emitter != null) {
			emitter.start(factory, interval, count);
		}
	}

	private static void replaySpriteAnim(JSONObject evt) throws JSONException {
		int id = evt.getInt("id");
		String anim = evt.getString("a");
		int targetCell = evt.getInt("tc");

		CharSprite sprite = findSprite(id);
		if (sprite == null) return;

		switch (anim) {
			case "attack": sprite.attack(targetCell); break;
			case "zap":    sprite.zap(targetCell); break;
			case "operate":sprite.operate(targetCell); break;
			case "die":    sprite.die(); break;
		}
	}

	private static void replaySpriteState(JSONObject evt) throws JSONException {
		int id = evt.getInt("id");
		String stateName = evt.getString("s");
		boolean on = evt.getBoolean("on");

		CharSprite sprite = findSprite(id);
		if (sprite == null) return;

		try {
			CharSprite.State state = CharSprite.State.valueOf(stateName);
			if (on) {
				sprite.add(state);
			} else {
				sprite.remove(state);
			}
		} catch (IllegalArgumentException ignored) {}
	}

	private static void replayFlash(JSONObject evt) throws JSONException {
		int id = evt.getInt("id");
		CharSprite sprite = findSprite(id);
		if (sprite != null) {
			sprite.flash();
		}
	}

	private static void replayCheckedCell(JSONObject evt) throws JSONException {
		int cell = evt.getInt("c");
		int source = evt.getInt("s");
		GameScene.effectOverFog(new CheckedCell(cell, source));
	}

	private static void replayMissile(JSONObject evt) throws JSONException {
		float fx = (float) evt.getDouble("fx");
		float fy = (float) evt.getDouble("fy");
		float tx = (float) evt.getDouble("tx");
		float ty = (float) evt.getDouble("ty");
		int img = evt.getInt("img");
		float angSpeed = (float) evt.optDouble("as", 720);
		float angle = (float) evt.optDouble("a", 0);

		if (Dungeon.hero != null && Dungeon.hero.sprite != null && Dungeon.hero.sprite.parent != null) {
			Item dummy = new Item() { { image = img; } };
			MissileSprite ms = (MissileSprite) Dungeon.hero.sprite.parent.recycle(MissileSprite.class);
			ms.reset(new PointF(fx, fy), new PointF(tx, ty), dummy, null);
			ms.angularSpeed = angSpeed;
			ms.angle = angle;
		}
	}

	private static void replayFloatingText(JSONObject evt) throws JSONException {
		int id = evt.getInt("id");
		String text = evt.getString("txt");
		int color = evt.getInt("col");
		int icon = evt.getInt("ic");

		CharSprite sprite = findSprite(id);
		if (sprite != null) {
			PointF c = sprite.destinationCenter();
			float x = c.x;
			float y = c.y - sprite.height() / 2f;
			int pos = sprite.ch != null ? sprite.ch.pos : -1;
			FloatingText.show(x, y, pos, text, color, icon, true);
		}
	}

	private static void replayBloodBurst(JSONObject evt) throws JSONException {
		int id = evt.getInt("id");
		int damage = evt.getInt("d");

		CharSprite sprite = findSprite(id);
		if (sprite != null) {
			// Approximate blood burst from center
			PointF c = sprite.center();
			int n = Math.min(9, Math.max(1, (int)(9 * Math.sqrt((double)damage / 20))));
			Splash.at(c, 0xFFBB0000, n);
		}
	}

	// --- Helpers ---

	private static Emitter.Factory resolveFactory(String fid) {
		if (fid.startsWith("speck_")) {
			try {
				int speckType = Integer.parseInt(fid.substring(6));
				return Speck.factory(speckType);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return factoryById.get(fid);
	}

	private static Emitter getEmitter(int cell, String emitterType) {
		switch (emitterType) {
			case "center": return CellEmitter.center(cell);
			case "floor":  return CellEmitter.floor(cell);
			case "bottom": return CellEmitter.bottom(cell);
			default:       return CellEmitter.get(cell);
		}
	}

	// Spectator ID mapping — host IDs don't match local IDs
	private static int hostHeroId = -1;
	private static java.util.Map<Integer, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob> spectatorMobs;

	public static void setHostHeroId(int id) {
		hostHeroId = id;
	}

	public static int getHostHeroId() {
		return hostHeroId;
	}

	public static void setSpectatorMobs(java.util.Map<Integer, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob> mobs) {
		spectatorMobs = mobs;
	}

	public static java.util.Map<Integer, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob> getSpectatorMobs() {
		return spectatorMobs;
	}

	private static CharSprite findSprite(int charId) {
		if (Dungeon.level == null) return null;

		// Client (spectator OR player): VFX charId is a host-side actor id.
		// Dungeon.hero on the client represents `hostHeroId`; everyone else lives in spectatorMobs.
		// Comparing local Dungeon.hero.id() would collide w/ unrelated host ids.
		if (!NetManager.isHost()) {
			if (Dungeon.hero != null && charId == hostHeroId) {
				return Dungeon.hero.sprite;
			}
			if (spectatorMobs != null) {
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob = spectatorMobs.get(charId);
				if (mob != null && mob.sprite != null) {
					return mob.sprite;
				}
			}
			return null;
		}

		// Host mode: local IDs are authoritative
		if (Dungeon.hero != null && Dungeon.hero.id() == charId) {
			return Dungeon.hero.sprite;
		}
		for (Char ch : Dungeon.level.mobs) {
			if (ch.id() == charId && ch.sprite != null) {
				return ch.sprite;
			}
		}
		return null;
	}
}
