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
