package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * Shared helpers for the per-hero MP quests — the bits every NPC quest repeated: the set of
 * heroes currently in the game, the "every present hero is done" despawn gate, and the
 * net-dialog payload builders.
 */
public class QuestSupport {

	/** Host hero + every connected remote hero. */
	public static ArrayList<Hero> presentHeroes() {
		ArrayList<Hero> all = new ArrayList<>();
		if (Dungeon.hero != null) all.add(Dungeon.hero);
		all.addAll(xyz.gabriwar.warpedpixeldungeon.net.NetManager.getNetHeroes());
		return all;
	}

	/** True once EVERY present hero has a progress record satisfying {@code done} — used to
	 *  decide when a quest NPC may finally leave (so a late joiner never loses their turn). */
	public static <P extends PerHeroProgress> boolean allPresentDone(PerHeroStore<P> store, Predicate<P> done) {
		for (Hero h : presentHeroes()) {
			P p = store.get(h.id());
			if (p == null || !done.test(p)) return false;
		}
		return true;
	}

	/** Route a plain text dialog (intro/reminder) to a remote hero's client. */
	public static void sendInfo(Hero h, String text) {
		sendInfo(null, h, text);
	}

	/** Route an NPC quest dialog to a remote hero's client. Carries the NPC's sprite class
	 *  + name so the client shows the SAME WndQuest (titled message + sprite) the host does. */
	public static void sendInfo(NPC npc, Hero h, String text) {
		try {
			JSONObject p = new JSONObject();
			p.put("text", text);
			if (npc != null) {
				p.put("title", npc.name());
				if (npc.spriteClass != null) p.put("sprite", npc.spriteClass.getName());
			}
			NetDialogs.request(h, NetDialogs.KIND_INFO, p, false);
		} catch (Exception ignored) {}
	}

	/** Render-only item descriptor (img/name/level/desc) for a dialog payload. */
	public static JSONObject itemDisplay(Item item) {
		JSONObject o = new JSONObject();
		try {
			o.put("img", item.image());
			o.put("name", item.name());
			o.put("lvl", item.level());
			try { o.put("desc", item.desc()); } catch (Exception ignored) {}
		} catch (Exception ignored) {}
		return o;
	}
}
