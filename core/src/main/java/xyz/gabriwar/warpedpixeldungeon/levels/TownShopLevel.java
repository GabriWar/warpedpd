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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.EnergyCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.Fertilizer;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.OrbOfZot;
import xyz.gabriwar.warpedpixeldungeon.items.SeekingClusterBombItem;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.plants.Blindweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Dewcatcher;
import xyz.gabriwar.warpedpixeldungeon.plants.Dreamfoil;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Flytrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift;
import xyz.gabriwar.warpedpixeldungeon.plants.Sorrowmoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Starflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Stormvine;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TownInteriors;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.HashSet;

//Remixed PD's shop interior, reached from the town square. Warped's two town
//shops (the scroll shop and the general store, formerly market stalls on the
//plaza) live in here: keepers behind the counter, wares laid out on it.
public class TownShopLevel extends TownInteriorLevel {

	//the two keepers stand in the nooks north of the counter
	private static final int SCROLL_KEEPER = 67;
	private static final int STORE_KEEPER  = 75;

	//the counter top: scroll shop on the north row, general store on the south row
	private static final int[] SCROLL_SPOTS = { 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108 };
	private static final int[] STORE_SPOTS  = { 115, 116, 117, 118, 119, 120, 121, 122, 123 };

	@Override protected int[] terrain()      { return TownInteriors.shopMap(); }
	@Override protected int mapWidth()       { return TownInteriors.SHOP_W; }
	@Override protected int mapHeight()      { return TownInteriors.SHOP_H; }
	@Override protected int entranceCell()   { return TownInteriors.SHOP_ENTRANCE; }

	@Override
	protected CustomTilemap[] groundLayers() {
		return new CustomTilemap[]{
				new TownInteriors.ShopBase(),
				new TownInteriors.ShopDeco(),
				new TownInteriors.ShopDeco2() };
	}

	@Override
	protected CustomTilemap roofLayer() {
		return new TownInteriors.ShopRoof();
	}

	@Override
	protected void spawnFolk() {
		//the keepers are seeded by storeStock() along with their wares
	}

	@Override
	protected void createItems() {
		storeStock();
	}

	@Override
	public void occupyCell(Char ch) {
		super.occupyCell(ch);
		//every visit refills what was bought; also stocks a shop interior that was
		//generated before the shops moved in here
		if (ch == Dungeon.hero) {
			storeStock();
		}
	}

	private Shopkeeper keeperAt(int cell) {
		for (Mob m : mobs) {
			if (m instanceof Shopkeeper && m.pos == cell) return (Shopkeeper) m;
		}
		return null;
	}

	private void seedKeeper(int cell) {
		if (keeperAt(cell) != null || Actor.findChar(cell) != null) return;
		Mob keeper = new Shopkeeper();
		keeper.pos = cell;
		if (Dungeon.level == this && com.watabou.noosa.Game.scene() instanceof GameScene) {
			GameScene.add(keeper);
		} else {
			mobs.add(keeper);
		}
	}

	protected void storeStock() {
		seedKeeper(SCROLL_KEEPER);
		seedKeeper(STORE_KEEPER);

		//every empty spot refills: the store restocks as soon as something is
		//bought. within each town shop no item type repeats - the scroll shop
		//always keeps exactly one scroll of upgrade in the mix
		HashSet<Class<?>> stocked = new HashSet<>();
		for (int i : SCROLL_SPOTS) {
			if (heaps.get(i) != null) stocked.add(heaps.get(i).peek().getClass());
		}
		for (int i : SCROLL_SPOTS) {
			if (heaps.get(i) == null) {
				Item it;
				if (!stocked.contains(ScrollOfUpgrade.class)) {
					it = new ScrollOfUpgrade();
				} else {
					it = distinctItem(stocked, false);
				}
				stocked.add(it.getClass());
				drop(it, i).type = Heap.Type.FOR_SALE;
				restockFx(i);
			}
		}

		stocked.clear();
		for (int i : STORE_SPOTS) {
			if (heaps.get(i) != null) stocked.add(heaps.get(i).peek().getClass());
		}
		for (int i : STORE_SPOTS) {
			if (heaps.get(i) == null) {
				Item it;
				//the town store always keeps plant food and gun ammo in stock
				if (!stocked.contains(Fertilizer.class)) {
					it = new Fertilizer();
					//planting season: the guaranteed stack is twice as deep
					it.quantity(GameCalendar.season() == GameCalendar.Season.SPRING ? 8 : 4);
				} else if (!stocked.contains(xyz.gabriwar.warpedpixeldungeon.items.BulletBelt.class)) {
					it = new xyz.gabriwar.warpedpixeldungeon.items.BulletBelt();
				} else {
					it = distinctItem(stocked, true);
				}
				stocked.add(it.getClass());
				drop(it, i).type = Heap.Type.FOR_SALE;
				restockFx(i);
			}
		}
	}

	//the same puff for wares vanishing and appearing. only when the shop is
	//the live level - stocking also happens during level creation, pre-scene
	private void restockFx(int cell){
		if (Dungeon.level == this && com.watabou.noosa.Game.scene() instanceof GameScene){
			CellEmitter.get(cell).burst(Speck.factory(Speck.WOOL), 4);
		}
	}

	//rolls until the item class isn't already on this shop's shelves
	private Item distinctItem(HashSet<Class<?>> stocked, boolean store){
		Item it = store ? storeItem() : scrollItem();
		for (int tries = 20; tries > 0 && stocked.contains(it.getClass()); tries--){
			it = store ? storeItem() : scrollItem();
		}
		return it;
	}

	protected Item scrollItem() {
		return Generator.randomUsingDefaults(Generator.Category.SCROLL);
	}

	//Sprouted's shop drew from a SEEDRICH pool, not the ordinary seed table. Rolling the
	//default SEED category here buries the rare seeds - they sit at weight 1 of 76, so Flytrap
	//fell from 22% to 1.3% - and this is the only reliable seed vendor in the postgame.
	private static final Class<?>[] RICH_SEEDS = {
			Firebloom.Seed.class, Icecap.Seed.class, Sorrowmoss.Seed.class, Blindweed.Seed.class,
			Sungrass.Seed.class, Earthroot.Seed.class, Fadeleaf.Seed.class, Dreamfoil.Seed.class,
			Stormvine.Seed.class, Starflower.Seed.class, Phaseshift.Seed.class,
			Flytrap.Seed.class, Dewcatcher.Seed.class };
	private static final float[] RICH_SEED_PROBS = {
			1, 1, 1, 1, 2, 1, 1, 1, 1, 4, 4, 8, 6 };

	private Item richSeed() {
		return (Item) Reflection.newInstance(RICH_SEEDS[Random.chances(RICH_SEED_PROBS)]);
	}

	//full rotation: every unsold item in both shops is rerolled. a stocked
	//orb of zot survives rotations - only buying it retires it for good
	public void rotateStock(){
		for (int i : SCROLL_SPOTS){
			Heap h = heaps.get(i);
			if (h != null && h.type == Heap.Type.FOR_SALE){
				restockFx(i);
				h.destroy();
			}
		}
		for (int i : STORE_SPOTS){
			Heap h = heaps.get(i);
			if (h != null && h.type == Heap.Type.FOR_SALE
					&& !(h.peek() instanceof OrbOfZot)){
				restockFx(i);
				h.destroy();
			}
		}
		storeStock();
	}

	private boolean orbStocked(){
		for (int i : STORE_SPOTS){
			Heap h = heaps.get(i);
			if (h != null && h.peek() instanceof OrbOfZot) return true;
		}
		return false;
	}

	protected Item storeItem() {
		//low-odds lottery for a second orb of zot. it can keep appearing
		//until bought once - after that, never again
		if (!Dungeon.orbofzotshopsold && !orbStocked() && Random.Int(100) == 0){
			return new OrbOfZot();
		}
		//two extra rolls in every twenty-one go to the season's shelf
		int roll = Random.Int(21);
		if (roll >= 19) {
			return seasonalItem(roll == 19);
		}
		Item it;
		switch (roll) {
			case 18:
				//loose ammo alongside the guaranteed belt
				return (Random.Int(2) == 0
						? new xyz.gabriwar.warpedpixeldungeon.items.BulletItem()
						: new xyz.gabriwar.warpedpixeldungeon.items.ArrowItem())
						.quantity(Random.IntRange(25, 50));
			case 16:
				return new xyz.gabriwar.warpedpixeldungeon.items.Coffee();
			//the unique ranged weapons only appear here
			case 17:
				it = (Item) Reflection.newInstance( Random.oneOf(
						xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Boomerang.class,
						xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ElfBow.class,
						xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.TaurcenBow.class,
						xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun.class,
						xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun.class,
						xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MegaCannon.class ));
				break;
			case 0: case 1:
				return new PotionOfHealing();
			case 2: case 3:
				return Generator.randomUsingDefaults(Generator.Category.POTION);
			case 4: case 5:
				return richSeed();
			case 6:
				return (Random.Int(10) < 3) ? new ActiveMrDestructo() : new InactiveMrDestructo();
			case 7:
				return new SeekingClusterBombItem();
			case 8:
				return Generator.randomUsingDefaults(Generator.Category.FOOD);
			case 9:
				return Generator.randomUsingDefaults(Generator.Category.STONE);
			//the rare shelf: identified, uncursed, priced by value()
			case 10:
				it = Generator.randomUsingDefaults(Generator.Category.WAND);
				break;
			case 11:
				it = Generator.randomUsingDefaults(Generator.Category.RING);
				break;
			case 12:
				it = Generator.randomUsingDefaults(Generator.Category.ARTIFACT);
				break;
			case 13:
				it = Generator.random(Generator.wepTiers[3 + Random.Int(2)]);
				break;
			case 14:
				it = Generator.randomUsingDefaults(Generator.Category.ARMOR);
				break;
			case 15:
				return new EnergyCrystal(Random.IntRange(15, 30));
			default:
				return new PotionOfHealing();
		}
		if (it == null){
			//artifact deck can run dry
			return new PotionOfHealing();
		}
		it.cursed = false;
		it.identify(false);
		return it;
	}

	//what the season calls for: spring seeds (on top of the deeper fertilizer
	//stack), summer frost, autumn harvest food, winter coffee and warming flame
	private Item seasonalItem(boolean first) {
		switch (GameCalendar.season()) {
			case SPRING:
				return richSeed();
			case SUMMER:
				return new PotionOfFrost();
			case AUTUMN:
				return Generator.randomUsingDefaults(Generator.Category.FOOD);
			case WINTER: default:
				return first ? new xyz.gabriwar.warpedpixeldungeon.items.Coffee() : new PotionOfLiquidFlame();
		}
	}
}
