package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.items.BrokenSeal;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import com.watabou.noosa.audio.Sample;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Host-side application of blacksmith services for REMOTE heroes. The client shows the
 * menu + picks items from its own mirrored inventory, then sends typed actions; the host
 * runs the equivalent of {@code WndBlacksmith}'s logic here against the acting net hero's
 * belongings and per-hero {@link Blacksmith.Quest.HeroProgress}.
 *
 * <p>All methods run on the actor thread (called from {@code Hero.act()}), mirroring the
 * single-threaded local window path.
 */
public class BlacksmithService {

	private static Blacksmith.Quest.HeroProgress hp(Hero h) {
		return Blacksmith.Quest.progressFor(h.id());
	}

	private static Item findByName(Hero h, String name) {
		return findByName(h, name, null);
	}

	// Find an item by display name, optionally skipping one already-chosen object. Iterates
	// the FULL belongings (equipped + backpack + sub-bags, e.g. a holstered wand) via the
	// Belongings iterator. The `exclude` arg disambiguates two distinct items that share a
	// name (e.g. reforging two identical weapons) — the client guarantees it picked two
	// different objects, so the host resolves the 2nd name to a *different* item than the 1st.
	private static Item findByName(Hero h, String name, Item exclude) {
		if (name == null) return null;
		for (Item i : h.belongings) {
			if (i != exclude && i.name().equals(name)) return i;
		}
		return null;
	}

	private static void clearLandmarkIfDone() {
		if (!Blacksmith.Quest.rewardsAvailable()) {
			Notes.remove(Notes.Landmark.TROLL);
		}
	}

	// --- Menu (host -> client) -------------------------------------------------

	/** Build and push the service menu to a remote hero's client. */
	public static void sendMenu(Hero h) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null) return;
		try {
			JSONObject p = new JSONObject();
			p.put("favor", hp.favor);
			p.put("pickaxeAvail", hp.pickaxe != null);
			p.put("pickaxeCost", hp.freePickaxe ? 0 : 250);
			p.put("reforgeCost", 500 + 1000 * hp.reforges);
			p.put("hardenCost", 500 + 1000 * hp.hardens);
			p.put("upgradeCost", 1000 + 1000 * hp.upgrades);
			p.put("smithCost", 2000);
			if (hp.smithRewards != null && hp.smiths > 0) {
				// a smith job is paid for but not yet collected — re-offer the items
				JSONArray rewards = new JSONArray();
				for (Item i : hp.smithRewards) rewards.put(itemDisplay(i));
				p.put("smithPending", rewards);
			}
			NetDialogs.request(h, NetDialogs.KIND_BLACKSMITH, p, false);
		} catch (Exception e) {
			NetManager.log("[NET-HOST] blacksmith sendMenu failed: " + e.getMessage());
		}
	}

	private static JSONObject itemDisplay(Item item) throws Exception {
		JSONObject o = new JSONObject();
		o.put("img", item.image());
		o.put("name", item.name());
		o.put("lvl", item.level());
		try { o.put("desc", item.desc()); } catch (Exception ignored) {}
		return o;
	}

	// --- Service appliers (client -> host) -------------------------------------

	public static void pickaxe(Hero h) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null || hp.pickaxe == null) return;
		int cost = hp.freePickaxe ? 0 : 250;
		if (hp.favor < cost) return;
		Item pick = hp.pickaxe;
		if (pick.doPickUp(h)) {
			NetManager.heroLog(h, Messages.capitalize(Messages.get(h, "you_now_have", pick.name())));
		} else {
			Dungeon.level.drop(pick, h.pos).sprite.drop();
		}
		hp.favor -= cost;
		hp.pickaxe = null;
		clearLandmarkIfDone();
	}

	public static void cashout(Hero h) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null || hp.favor <= 0) return;
		new Gold(hp.favor).doPickUp(h, h.pos);
		hp.favor = 0;
		clearLandmarkIfDone();
	}

	public static void reforge(Hero h, String name1, String name2) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null) return;
		int cost = 500 + 1000 * hp.reforges;
		if (hp.favor < cost) return;

		Item a = findByName(h, name1);
		Item b = findByName(h, name2, a); // skip `a` so two same-name items resolve distinctly
		if (a == null || b == null || a == b) return;
		if (a.getClass() != b.getClass()) return;
		if (!a.isIdentified() || !b.isIdentified() || a.cursed || b.cursed
				|| !a.isUpgradable() || !b.isUpgradable()) return;

		Item first, second;
		if (a.trueLevel() >= b.trueLevel()) { first = a; second = b; }
		else { first = b; second = a; }

		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		ScrollOfUpgrade.upgrade(h);
		Item.evoke(h);

		if (second.isEquipped(h)) {
			((EquipableItem) second).doUnequip(h, false);
		}
		second.detachAll(h.belongings.backpack);

		if (second instanceof Armor) {
			BrokenSeal seal = ((Armor) second).checkSeal();
			if (seal != null) Dungeon.level.drop(seal, h.pos);
		} else if (second instanceof MissileWeapon) {
			Buff.affect(h, MissileWeapon.UpgradedSetTracker.class)
					.levelThresholds.put(((MissileWeapon) second).setID, Integer.MAX_VALUE);
		}

		if (first instanceof Weapon && ((Weapon) first).hasGoodEnchant()) {
			((Weapon) first).upgrade(true);
		} else if (first instanceof Armor && ((Armor) first).hasGoodGlyph()) {
			((Armor) first).upgrade(true);
		} else {
			first.upgrade();
		}
		Badges.validateItemLevelAquired(first);
		Item.updateQuickslot();

		hp.favor -= cost;
		hp.reforges++;
		clearLandmarkIfDone();
	}

	public static void harden(Hero h, String name) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null) return;
		int cost = 500 + 1000 * hp.hardens;
		if (hp.favor < cost) return;

		Item item = findByName(h, name);
		if (item == null || !item.isUpgradable() || !item.isIdentified() || item.cursed) return;
		if (item instanceof Weapon && !((Weapon) item).enchantHardened) {
			((Weapon) item).enchantHardened = true;
		} else if (item instanceof Armor && !((Armor) item).glyphHardened) {
			((Armor) item).glyphHardened = true;
		} else {
			return;
		}

		hp.favor -= cost;
		hp.hardens++;
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		Item.evoke(h);
		clearLandmarkIfDone();
	}

	public static void upgrade(Hero h, String name) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null) return;
		int cost = 1000 + 1000 * hp.upgrades;
		if (hp.favor < cost) return;

		Item item = findByName(h, name);
		if (item == null || !item.isUpgradable() || !item.isIdentified() || item.cursed || item.level() >= 2) return;

		item.upgrade();
		hp.favor -= cost;
		hp.upgrades++;
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		ScrollOfUpgrade.upgrade(h);
		Item.evoke(h);
		Badges.validateItemLevelAquired(item);
		Catalog.countUse(item.getClass());
		clearLandmarkIfDone();
	}

	/** Pay for and open a smith job — charges favor, re-pushes the menu with the reward pool. */
	public static void smithOpen(Hero h) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null || hp.favor < 2000) return;
		hp.favor -= 2000;
		hp.smiths++;
		if (hp.smithRewards == null || hp.smithRewards.isEmpty()) {
			Blacksmith.Quest.generateRewards(hp, false);
		}
		sendMenu(h);
	}

	/** Collect a smith reward by index. */
	public static void smith(Hero h, int index) {
		Blacksmith.Quest.HeroProgress hp = hp(h);
		if (hp == null || hp.smithRewards == null || index < 0 || index >= hp.smithRewards.size()) return;
		Item item = hp.smithRewards.get(index);

		if (item instanceof Weapon && hp.smithEnchant != null) {
			((Weapon) item).enchant(hp.smithEnchant);
		} else if (item instanceof Armor && hp.smithGlyph != null) {
			((Armor) item).inscribe(hp.smithGlyph);
		}
		item.identify(false);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		Item.evoke(h);
		if (item.doPickUp(h)) {
			NetManager.heroLog(h, Messages.capitalize(Messages.get(h, "you_now_have", item.name())));
		} else {
			Dungeon.level.drop(item, h.pos).sprite.drop();
		}
		hp.smithRewards = null;
		clearLandmarkIfDone();
	}
}
