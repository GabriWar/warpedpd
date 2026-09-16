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

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe map of per-hero quest progress, keyed by {@code hero.id()}. Concurrent because
 * a host-hero reward claim runs on the render thread while remote claims / give / process run
 * on the actor thread. Handles the Bundlable collection save/load (with a guard so pre-per-hero
 * saves don't throw).
 */
public class PerHeroStore<P extends PerHeroProgress> {

	private final ConcurrentHashMap<Integer, P> map = new ConcurrentHashMap<>();

	public P get(int heroId) { return map.get(heroId); }
	public void put(P p) { map.put(p.heroId(), p); }
	public void clear() { map.clear(); }
	public Collection<P> values() { return map.values(); }

	public void store(Bundle node, String key) {
		node.put(key, map.values());
	}

	@SuppressWarnings("unchecked")
	public void restore(Bundle node, String key) {
		map.clear();
		if (node.contains(key)) {
			for (Bundlable o : node.getCollection(key)) {
				P p = (P) o;
				map.put(p.heroId(), p);
			}
		}
	}
}
