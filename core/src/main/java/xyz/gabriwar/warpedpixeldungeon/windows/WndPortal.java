/*
 * Warped Pixel Dungeon
 *
 * Portal gate UI. Dispatches by PortalGate state:
 *   LOCKED → pay flow
 *   ACTIVE → travel destination list
 *   DEAD   → broken message
 */

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

import java.util.List;

public class WndPortal extends WndOptions {

	private static final float TELEPORT_HUNGER = 150f;

	private final PortalGate portal;

	public WndPortal(PortalGate portal) {
		super(portalIcon(),
				title(portal),
				message(portal),
				options(portal));
		this.portal = portal;
	}

	private static Image portalIcon() {
		return new Image(Assets.Interfaces.PORTAL_ICON);
	}

	private static String title(PortalGate p) {
		switch (p.state) {
			case ACTIVE: return "Portal Gate";
			case DEAD:   return "Broken Portal";
			case LOCKED:
			default:     return "Inactive Portal";
		}
	}

	private static String message(PortalGate p) {
		switch (p.state) {
			case ACTIVE:
				List<Integer> dests = Portals.travelable(Dungeon.depth);
				if (dests.isEmpty()) {
					return "This gate is humming with energy, but no other portals are active. Discover and unlock more to travel between them.\n\nEach trip costs 150 hunger.";
				}
				return "Channel the gate to travel to another active portal.\n\nEach trip costs 150 hunger.";
			case DEAD:
				return "The keeper is gone. The gate is dark and silent — its magic will not answer.";
			case LOCKED:
			default:
				int cost = Portals.costForDepth(Dungeon.depth);
				return "The shopkeeper eyes the gate behind them. \"Activate this gate for " + cost + " gold and it'll carry you between every gate you've lit. Once paid, the magic stays with you.\"";
		}
	}

	private static String[] options(PortalGate p) {
		switch (p.state) {
			case ACTIVE: {
				List<Integer> dests = Portals.travelable(Dungeon.depth);
				if (dests.isEmpty()) {
					return new String[]{ "Leave" };
				}
				String[] out = new String[dests.size() + 1];
				for (int i = 0; i < dests.size(); i++) {
					out[i] = depthLabel(dests.get(i));
				}
				out[dests.size()] = "Leave";
				return out;
			}
			case DEAD:
				return new String[]{ "Leave" };
			case LOCKED:
			default: {
				int cost = Portals.costForDepth(Dungeon.depth);
				boolean canAfford = Dungeon.gold >= cost;
				return new String[]{
						canAfford ? ("Pay " + cost + " gold") : ("Pay " + cost + " gold (not enough)"),
						"Leave"
				};
			}
		}
	}

	private static String depthLabel(int depth) {
		String region;
		switch ((depth - 1) / 5) {
			case 0: region = "Sewers";  break;
			case 1: region = "Prison";  break;
			case 2: region = "Caves";   break;
			case 3: region = "City";    break;
			case 4: region = "Halls";   break;
			default: region = "Floor";  break;
		}
		return region + " — Floor " + depth;
	}

	// Read state from registry rather than this.portal — these callbacks fire
	// inside WndOptions super-ctor before the instance field assignment runs.
	private static PortalGate.State currentState() {
		return Portals.getState(Dungeon.depth);
	}

	@Override
	protected boolean hasIcon(int index) {
		return currentState() == PortalGate.State.ACTIVE
				&& index < Portals.travelable(Dungeon.depth).size();
	}

	@Override
	protected Image getIcon(int index) {
		return new Image(Assets.Interfaces.PORTAL_ICON);
	}

	@Override
	protected boolean enabled(int index) {
		if (currentState() == PortalGate.State.LOCKED && index == 0) {
			return Dungeon.gold >= Portals.costForDepth(Dungeon.depth);
		}
		return true;
	}

	@Override
	protected void onSelect(int index) {
		switch (portal.state) {
			case LOCKED: handlePay(index); break;
			case ACTIVE: handleTravel(index); break;
			default:     /* close */     break;
		}
	}

	private void handlePay(int index) {
		if (index != 0) return; // "Leave"
		// Client: route to host, which owns gold + portal state and broadcasts back.
		if (NetManager.isPlayer()) {
			NetManager.sendPlayerActionTyped("portal_pay");
			return;
		}
		int cost = Portals.costForDepth(Dungeon.depth);
		if (Dungeon.gold < cost) return;
		Dungeon.gold -= cost;
		Portals.discover(Dungeon.depth);
		portal.setState(PortalGate.State.ACTIVE);
	}

	private void handleTravel(int index) {
		List<Integer> dests = Portals.travelable(Dungeon.depth);
		if (index >= dests.size()) return; // "Leave"
		final int destDepth = dests.get(index);

		// Client: host applies hunger/starve + relocates the party. Skip the local
		// starve prompt (it'd read the host hero's hunger, not the client's).
		if (NetManager.isPlayer()) {
			doTravel(destDepth);
			return;
		}

		Hunger h = Dungeon.hero.buff(Hunger.class);
		boolean wouldStarve = h != null && (h.hunger() + TELEPORT_HUNGER >= Hunger.STARVING);
		if (wouldStarve) {
			GameScene.show(new WndOptions(
					"Channel anyway?",
					"You are too weak — channeling the portal now will leave you starving.",
					"Travel anyway",
					"Cancel"
			) {
				@Override
				protected void onSelect(int index) {
					if (index == 0) doTravel(destDepth);
				}
			});
			return;
		}
		doTravel(destDepth);
	}

	private void doTravel(int destDepth) {
		// Client: ask the host to relocate the whole party.
		if (NetManager.isPlayer()) {
			NetManager.sendPlayerActionTyped("portal_travel", destDepth);
			return;
		}
		travel(destDepth, Dungeon.hero);
	}

	// Shared host/single-player travel: applies hunger to the traveling hero, then
	// transitions the floor (whole party — depth is shared in MP).
	private static void travel(int destDepth, Hero traveler) {
		Portals.Record r = Portals.get(destDepth);
		if (r == null || r.state != PortalGate.State.ACTIVE) {
			return; // sanity
		}

		Hunger h = traveler.buff(Hunger.class);
		if (h != null) h.affectHunger(-TELEPORT_HUNGER, true);

		// MP: hint NetManager to land netHeroes adjacent to the destination gate
		// instead of the floor's regular entrance.
		NetManager.setPendingRelocationAnchor(r.cell);

		Game.runOnRenderThread(() -> {
			Level.beforeTransition();
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = destDepth;
			InterlevelScene.returnBranch = 0;
			InterlevelScene.returnPos = r.cell;
			Game.switchScene(InterlevelScene.class);
		});
	}

	// --- Host-side handlers for client-routed portal actions (run on actor thread) ---

	private static PortalGate findGate() {
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs) {
			if (m instanceof PortalGate) return (PortalGate) m;
		}
		return null;
	}

	/** A client opened the gate UI — register discovery so it shows in travel lists. */
	public static void hostOpen(Hero h) {
		Portals.discover(Dungeon.depth);
	}

	// Anti-cheese: a routed pay/travel is only honored if the hero is standing next to
	// (or on) the gate, mirroring how the local UI only opens via adjacent interaction.
	private static boolean atGate(Hero h) {
		Portals.Record r = Portals.get(Dungeon.depth);
		if (r == null || r.cell < 0 || Dungeon.level == null) return false;
		return h.pos == r.cell || Dungeon.level.adjacent(h.pos, r.cell);
	}

	/** A client paid to activate the gate at their current (shared) depth. */
	public static void hostPay(Hero h) {
		if (!atGate(h)) return;
		int cost = Portals.costForDepth(Dungeon.depth);
		if (Dungeon.gold < cost) return;
		if (Portals.getState(Dungeon.depth) != PortalGate.State.LOCKED) return;
		Dungeon.gold -= cost;
		Portals.discover(Dungeon.depth);
		Portals.setState(Dungeon.depth, PortalGate.State.ACTIVE);
		// flip the live gate NPC (and its sprite) on the render thread
		final PortalGate gate = findGate();
		if (gate != null) {
			Game.runOnRenderThread(() -> gate.setState(PortalGate.State.ACTIVE));
		}
	}

	/** A client chose a travel destination — relocate the whole party. */
	public static void hostTravel(Hero h, int destDepth) {
		if (!atGate(h)) return;
		travel(destDepth, h);
	}
}
