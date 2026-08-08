package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import com.watabou.utils.Bundlable;

/**
 * Per-hero quest progress record. Each NPC quest (Ghost/Imp/Wandmaker/Blacksmith) keeps
 * one of these per hero in a {@link PerHeroStore}. Implementations bundle their own fields;
 * {@link #heroId()} lets the store re-key the map after a save is restored.
 */
public interface PerHeroProgress extends Bundlable {
	int heroId();
}
