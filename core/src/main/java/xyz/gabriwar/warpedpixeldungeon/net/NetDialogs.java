package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Ghost;

import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Host-side registry for interaction dialogs routed to a specific client.
 *
 * <p>The host runs the authoritative game; remote players can't be shown a
 * {@code GameScene.show(...)} window directly. Instead the host registers a
 * pending dialog here and pushes a {@link Protocol#SHOW_DIALOG} message to the
 * owning client. The client builds the window locally and, when the player
 * resolves it, replies with {@link Protocol#DIALOG_CHOICE}. The choice is
 * stashed on the hero and consumed on the actor thread, where {@link #resolve}
 * applies the result single-threaded.
 *
 * <p>Dialog "kinds" are simple strings; the only kind that needs server-side
 * resolution is a reward pick. Informational dialogs (quest text) carry no
 * registry entry and their choice is ignored.
 */
public class NetDialogs {

	public static final String KIND_INFO = "info";            // text-only, no resolution
	public static final String KIND_GHOST_REWARD = "ghost_reward";
	public static final String KIND_PORTAL = "portal";        // shopkeeper-guarded fast-travel gate
	public static final String KIND_SHOPKEEPER = "shopkeeper";// shop main menu (sell/talk/buyback)
	public static final String KIND_SHOP_BUY = "shop_buy";    // FOR_SALE heap purchase confirm
	public static final String KIND_IMP_REWARD = "imp_reward";// imp quest: hand in tokens for a ring
	public static final String KIND_WANDMAKER_REWARD = "wandmaker_reward"; // wandmaker: pick 1 of 2 wands
	public static final String KIND_BLACKSMITH = "blacksmith";// blacksmith service menu

	private static final AtomicInteger nextId = new AtomicInteger(1);

	private static class Pending {
		final int heroId;
		final String kind;
		Pending(int heroId, String kind) { this.heroId = heroId; this.kind = kind; }
	}

	// dialogId -> pending resolution. Only kinds that mutate state on choice are stored.
	private static final ConcurrentHashMap<Integer, Pending> pending = new ConcurrentHashMap<>();

	/**
	 * Push an interaction dialog to the hero's owning client.
	 *
	 * @param needsResolve true if the choice mutates game state and must be
	 *                     resolved on the host (reward picks); false for purely
	 *                     informational dialogs (quest text).
	 * @return the assigned dialogId, or -1 if the hero has no connected client.
	 */
	public static int request(Hero hero, String kind, JSONObject payload, boolean needsResolve) {
		int id = nextId.getAndIncrement();
		if (needsResolve) {
			// Bound the map: a player re-opening the same dialog (e.g. re-clicking the
			// ghost without choosing) replaces its prior outstanding entry instead of
			// leaking one per click. Stale ids are harmlessly rejected by resolve().
			pending.values().removeIf(p -> p.heroId == hero.id() && p.kind.equals(kind));
			pending.put(id, new Pending(hero.id(), kind));
		}
		if (!NetManager.sendDialogToHero(hero, id, kind, payload)) {
			pending.remove(id);
			return -1;
		}
		return id;
	}

	/** True if this dialogId is registered to the given hero (anti-spoof guard). */
	public static boolean isOwnedBy(int dialogId, Hero hero) {
		Pending p = pending.get(dialogId);
		return p != null && p.heroId == hero.id();
	}

	/**
	 * Apply a resolved dialog choice. Runs on the actor thread (called from
	 * {@code Hero.act()}), so it may freely mutate game state.
	 */
	public static void resolve(Hero hero, int dialogId, String choice) {
		Pending p = pending.remove(dialogId);
		if (p == null || p.heroId != hero.id()) {
			NetManager.log("[NET-HOST] dialog resolve ignored — stale/unknown id=" + dialogId
					+ " hero=" + hero.id());
			return;
		}
		switch (p.kind) {
			case KIND_GHOST_REWARD:
				Ghost.Quest.claimReward(hero, "weapon".equals(choice));
				break;
			case KIND_PORTAL:
				resolvePortal(hero, choice);
				break;
			case KIND_IMP_REWARD:
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Imp.Quest.claimReward(hero);
				break;
			case KIND_WANDMAKER_REWARD:
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker.Quest.claimReward(hero, "wand1".equals(choice));
				break;
			default:
				NetManager.log("[NET-HOST] dialog resolve: unknown kind=" + p.kind);
				break;
		}
	}

	private static void resolvePortal(Hero hero, String choice) {
		if (choice == null || choice.equals("leave")) return;

		// Find the portal NPC on the hero's current level (the one they clicked on).
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate portal = null;
		if (xyz.gabriwar.warpedpixeldungeon.Dungeon.level != null) {
			for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob m :
					xyz.gabriwar.warpedpixeldungeon.Dungeon.level.mobs.toArray(
							new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0])) {
				if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate) {
					portal = (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate) m;
					break;
				}
			}
		}
		if (portal == null) {
			NetManager.log("[NET-HOST] resolvePortal: no portal on current level — choice=" + choice);
			return;
		}

		if ("pay".equals(choice)) {
			int cost = xyz.gabriwar.warpedpixeldungeon.Portals.costForDepth(
					xyz.gabriwar.warpedpixeldungeon.Dungeon.depth);
			if (portal.state != xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate.State.LOCKED) {
				NetManager.log("[NET-HOST] resolvePortal pay: portal not LOCKED, ignored");
				return;
			}
			if (xyz.gabriwar.warpedpixeldungeon.Dungeon.gold < cost) {
				NetManager.log("[NET-HOST] resolvePortal pay: insufficient gold (" +
						xyz.gabriwar.warpedpixeldungeon.Dungeon.gold + " < " + cost + ")");
				return;
			}
			// NOTE: Dungeon.gold is shared across all players in this build — per-player
			// purses would need a netHero-side gold field that picks up from heap drops.
			// Until that exists, the cost comes out of the global purse the netHero sees
			// in their HUD as `gold`.
			xyz.gabriwar.warpedpixeldungeon.Dungeon.gold -= cost;
			portal.setState(xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate.State.ACTIVE);
			return;
		}

		if (choice.startsWith("travel:")) {
			final int destDepth;
			try {
				destDepth = Integer.parseInt(choice.substring("travel:".length()));
			} catch (NumberFormatException e) {
				NetManager.log("[NET-HOST] resolvePortal travel: bad depth in choice=" + choice);
				return;
			}
			xyz.gabriwar.warpedpixeldungeon.Portals.Record r =
					xyz.gabriwar.warpedpixeldungeon.Portals.get(destDepth);
			if (r == null
					|| r.state != xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate.State.ACTIVE) {
				NetManager.log("[NET-HOST] resolvePortal travel: invalid dest=" + destDepth);
				return;
			}
			// Hunger toll on the interacting hero specifically.
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger hg =
					hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger.class);
			if (hg != null) hg.affectHunger(-150f, true);

			final int destCell = r.cell;
			com.watabou.noosa.Game.runOnRenderThread(new com.watabou.utils.Callback() {
				@Override
				public void call() {
					NetManager.setPendingRelocationAnchor(destCell);
					xyz.gabriwar.warpedpixeldungeon.levels.Level.beforeTransition();
					xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.mode =
							xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.Mode.RETURN;
					xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnDepth = destDepth;
					xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnBranch = 0;
					xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnPos = destCell;
					com.watabou.noosa.Game.switchScene(
							xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.class);
				}
			});
		}
	}

	/** Drop any registered dialogs for a hero (disconnect / new game). */
	public static void forgetHero(int heroId) {
		pending.values().removeIf(p -> p.heroId == heroId);
	}

	public static void reset() {
		pending.clear();
	}

	/**
	 * Fallback for any NPC.interact() that doesn't yet have a dedicated MP routing.
	 * When called on a remote (client-owned) hero, ships the supplied text as an
	 * info dialog to that player and returns true so the caller can early-return
	 * from interact() — instead of silently bailing on the `c != Dungeon.hero`
	 * gate (which produces "click does nothing" in MP). Returns false for the
	 * host hero so the existing local dialog path runs unchanged.
	 */
	public static boolean handleNetHero(xyz.gabriwar.warpedpixeldungeon.actors.Char c, String text) {
		if (!(c instanceof Hero)) return false;
		Hero h = (Hero) c;
		if (!h.isRemote) return false;
		try {
			JSONObject payload = new JSONObject();
			payload.put("text", text != null && !text.isEmpty()
					? text
					: "(This interaction isn't fully wired for multiplayer yet — ask the host to do it for you.)");
			request(h, KIND_INFO, payload, false);
		} catch (Exception ignored) {}
		return true;
	}
}
