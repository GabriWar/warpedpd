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
