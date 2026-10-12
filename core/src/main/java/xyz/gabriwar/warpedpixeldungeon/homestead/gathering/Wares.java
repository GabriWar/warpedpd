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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.levels.TownShopLevel;
import com.watabou.utils.Reflection;

import java.util.Set;

/**
 * The homesteads' goods on the shelves. The town's general store (TownShopLevel) keeps up to
 * TOWN_SPOTS of its nine spots for a builder, the rest for its usual goods: first a builder's tool
 * and a builder's satchel for a hero who has none, then its tool rack - a woodcutter's axe, a
 * mason's hammer, a shovel and a sickle, each while the hero does not carry one, then rope, which
 * he can always use. Whatever does not fit comes up as soon as a spot is free: once the tool and
 * the satchel are bought the whole rack is on the counter. Its own prices, scaled as the town's
 * services are by the deepest floor reached (TownLedger.scaled): the tool 150, the axe and the
 * hammer 200, the satchel 120, rope 12 a coil; a shovel and a sickle at a weapon's shelf price.
 *
 * Any building material on any shelf costs twice its worth (MATERIAL_MARKUP): the village
 * carpenter's goods (Carpentry), and a trader buys it back at its worth.
 */
public final class Wares {

	private Wares(){}

	/** The town store's prices before the town's scaling. */
	public static final int BUILDERS_TOOL = 150, WOODCUTTERS_AXE = 200, MASONS_HAMMER = 200,
			BUILDERS_SATCHEL = 120, ROPE = 12;
	/** How many coils of rope the town store's rack holds. */
	public static final int ROPE_COILS = 5;
	/** How many of the town store's spots the builder's wares take at most: the other goods keep
	 *  the rest (its plant food and ammunition and at least three more). */
	public static final int TOWN_SPOTS = 4;
	/** A material on a shelf asks this many times its worth. */
	public static final float MATERIAL_MARKUP = 2f;

	//the builder's wares in the order the counter takes them: the tool, the satchel, the rack
	private static final Class<?>[] WARES = { BuildersTool.class, BuildersSatchel.class,
			WoodcuttersAxe.class, MasonsHammer.class, Shovel.class, Sickle.class, Rope.class };

	/**
	 * What a shelf asks for a homestead ware before the buyer's perks and the keeper's mood
	 * (Shopkeeper.basePrice), -1 for any other item. Keyed off the level the shelf is on, so a
	 * co-op guest is quoted what the host charges.
	 */
	public static int shelfPrice( Item item ){
		if (Dungeon.level instanceof TownShopLevel){
			int base = townBase( item );
			if (base > 0) return TownLedger.scaled( base ) * (item.stackable ? item.quantity() : 1);
		}
		if (item instanceof Material) return Math.round( item.value() * MATERIAL_MARKUP );
		return -1;
	}

	//the town store's own price for one of a ware, -1 for none it prices
	static int townBase( Item item ){
		if (item instanceof BuildersTool) return BUILDERS_TOOL;
		if (item instanceof WoodcuttersAxe) return WOODCUTTERS_AXE;
		if (item instanceof MasonsHammer) return MASONS_HAMMER;
		if (item instanceof BuildersSatchel) return BUILDERS_SATCHEL;
		if (item instanceof Rope) return ROPE;
		return -1;
	}

	/**
	 * The town store's next homestead ware for an empty spot, null when the builder's share of the
	 * counter (TOWN_SPOTS) is full or he has every ware on it: the first in order (WARES) that is
	 * not on the counter and the hero does not carry (rope he can always use).
	 */
	public static Item townStock( Set<Class<?>> stocked ){
		Hero hero = Dungeon.hero;
		int shown = 0;
		for (Class<?> c : WARES) if (stocked.contains( c )) shown++;
		if (shown >= TOWN_SPOTS) return null;
		for (Class<?> c : WARES){
			if (stocked.contains( c )) continue;
			@SuppressWarnings("unchecked") Class<? extends Item> ic = (Class<? extends Item>) c;
			if (c != Rope.class && hero != null && hero.belongings.getItem( ic ) != null) continue;
			return ware( ic );
		}
		return null;
	}

	//one of a ware, as the counter shows it: rope by the coil, a tool known and uncursed
	private static Item ware( Class<? extends Item> c ){
		Item it = Reflection.newInstance( c );
		if (it instanceof Rope){
			it.quantity( ROPE_COILS );
		} else {
			it.cursed = false;
			it.identify( false );
		}
		return it;
	}
}
