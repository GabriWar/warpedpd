/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.special;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.ArrowBag;
import xyz.gabriwar.warpedpixeldungeon.items.BulletBelt;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import xyz.gabriwar.warpedpixeldungeon.items.Coffee;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfDead;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfLife;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfTranscendence;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Honeypot;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.Stylus;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.MailArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ScaleArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.AnkhChain;
import xyz.gabriwar.warpedpixeldungeon.items.bags.KeyRing;
import xyz.gabriwar.warpedpixeldungeon.items.bags.MagicalHolster;
import xyz.gabriwar.warpedpixeldungeon.items.bags.PotionBandolier;
import xyz.gabriwar.warpedpixeldungeon.items.bags.ScrollHolder;
import xyz.gabriwar.warpedpixeldungeon.items.bags.FoodPouch;
import xyz.gabriwar.warpedpixeldungeon.items.bags.VelvetPouch;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.food.SmallRation;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Alchemize;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAugmentation;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.TippedDart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;

public class ShopRoom extends SpecialRoom {

	protected ArrayList<Item> itemsToSpawn;

	protected int portalCell = -1;
	
	@Override
	public int minWidth() {
		return Math.max(7, (int)(Math.sqrt(spacesNeeded())+3));
	}
	
	@Override
	public int minHeight() {
		return Math.max(7, (int)(Math.sqrt(spacesNeeded())+3));
	}

	public int spacesNeeded(){
		if (itemsToSpawn == null) itemsToSpawn = generateItems();

		//sandbags spawn based on current level of an hourglass the player may be holding
		// so, to avoid rare cases of min sizes differing based on that, we ignore all sandbags
		// and then add 4 items in all cases, which is max number of sandbags that can be in the shop
		int spacesNeeded = itemsToSpawn.size();
		for (Item i : itemsToSpawn){
			if (i instanceof TimekeepersHourglass.sandBag){
				spacesNeeded--;
			}
		}
		spacesNeeded += 4;

		//we also add 1 more space, for the shopkeeper
		spacesNeeded++;
		return spacesNeeded;
	}
	
	public void paint( Level level ) {
		
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );

		placeShopkeeper( level );

		placeItems( level );
		
		for (Door door : connected.values()) {
			door.set( Door.Type.REGULAR );
		}

	}

	protected void placeShopkeeper( Level level ) {

		int pos = level.pointToCell(center());

		Mob shopkeeper = new Shopkeeper();
		shopkeeper.pos = pos;
		level.mobs.add( shopkeeper );

		placePortal( level, pos );
	}

	// Spawns a PortalGate adjacent to the shopkeeper.
	// Tries S, E, W, N in order; falls back to no portal if all blocked.
	protected void placePortal( Level level, int keeperPos ) {
		int[] candidates = new int[]{
				keeperPos + level.width(),  // south
				keeperPos + 1,              // east
				keeperPos - 1,              // west
				keeperPos - level.width()   // north
		};
		int chosen = -1;
		for (int c : candidates) {
			if (c < 0 || c >= level.length()) continue;
			if (level.map[c] != Terrain.EMPTY_SP && level.map[c] != Terrain.EMPTY) continue;
			if (level.findMob(c) != null) continue;
			chosen = c;
			break;
		}
		if (chosen == -1) return;

		PortalGate portal = new PortalGate();
		portal.pos = chosen;
		xyz.gabriwar.warpedpixeldungeon.Portals.register(Dungeon.depth, chosen);
		portal.syncFromRegistry();
		if (xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon.scene() instanceof GameScene) {
			GameScene.add(portal);
		} else {
			level.mobs.add(portal);
		}
		portalCell = chosen;
	}

	protected void placeItems( Level level ){

		if (itemsToSpawn == null){
			itemsToSpawn = generateItems();
		}

		Point entryInset = new Point(entrance());
		if (entryInset.y == top){
			entryInset.y++;
		} else if (entryInset.y == bottom) {
			entryInset.y--;
		} else if (entryInset.x == left){
			entryInset.x++;
		} else {
			entryInset.x--;
		}

		Point curItemPlace = entryInset.clone();

		int inset = 1;

		for (Item item : itemsToSpawn.toArray(new Item[0])) {

			//place items in a clockwise pattern
			if (curItemPlace.x == left+inset && curItemPlace.y != top+inset){
				curItemPlace.y--;
			} else if (curItemPlace.y == top+inset && curItemPlace.x != right-inset){
				curItemPlace.x++;
			} else if (curItemPlace.x == right-inset && curItemPlace.y != bottom-inset){
				curItemPlace.y++;
			} else {
				curItemPlace.x--;
			}

			//once we get to the inset from the entrance again, move another cell inward and loop
			if (curItemPlace.equals(entryInset)){

				if (entryInset.y == top+inset){
					entryInset.y++;
				} else if (entryInset.y == bottom-inset){
					entryInset.y--;
				}
				if (entryInset.x == left+inset){
					entryInset.x++;
				} else if (entryInset.x == right-inset){
					entryInset.x--;
				}
				inset++;

				if (inset > (Math.min(width(), height())-3)/2){
					break; //out of space!
				}

				curItemPlace = entryInset.clone();

				//make sure to step forward again
				if (curItemPlace.x == left+inset && curItemPlace.y != top+inset){
					curItemPlace.y--;
				} else if (curItemPlace.y == top+inset && curItemPlace.x != right-inset){
					curItemPlace.x++;
				} else if (curItemPlace.x == right-inset && curItemPlace.y != bottom-inset){
					curItemPlace.y++;
				} else {
					curItemPlace.x--;
				}
			}

			int cell = level.pointToCell(curItemPlace);
			//skip the portal tile so items don't bury it
			if (cell == portalCell){
				continue;
			}
			//prevents high grass from being trampled, potentially dropping dew/seeds onto shop items
			if (level.map[cell] == Terrain.HIGH_GRASS){
				Level.set(cell, Terrain.GRASS, level);
				GameScene.updateMap(cell);
			}
			level.drop( item, cell ).type = Heap.Type.FOR_SALE;
			itemsToSpawn.remove(item);
		}

		//we didn't have enough space to place everything neatly, so now just fill in anything left
		if (!itemsToSpawn.isEmpty()){
			for (Point p : getPoints()){
				int cell = level.pointToCell(p);
				if (cell == portalCell) continue;
				if ((level.map[cell] == Terrain.EMPTY_SP || level.map[cell] == Terrain.EMPTY)
						&& level.heaps.get(cell) == null && level.findMob(cell) == null){
					level.drop( itemsToSpawn.remove(0), level.pointToCell(p) ).type = Heap.Type.FOR_SALE;
				}
				if (itemsToSpawn.isEmpty()){
					break;
				}
			}
		}

		if (!itemsToSpawn.isEmpty()){
			WarpedPixelDungeon.reportException(new RuntimeException("failed to place all items in a shop!"));
		}

	}
	
	public static ArrayList<Item> generateItems() {

		ArrayList<Item> itemsToSpawn = new ArrayList<>();

		MeleeWeapon w;
		MissileWeapon m;
		switch (Dungeon.depth) {
		case 6: default:
			w = (MeleeWeapon) Generator.random(Generator.wepTiers[1]);
			m = (MissileWeapon) Generator.random(Generator.misTiers[1]);
			itemsToSpawn.add( new LeatherArmor().identify(false) );
			//the journal's canonical source. Without it the earliest you can get one is the
			//DM300 on 15, which pushes Sokoban, Town and the Vault two chapters back.
			//The LimitedDrop is only spent on pickup, not here: a journal you cannot afford
			//must not lock the boss fallbacks out.
			if (Dungeon.depth == 6 && !Dungeon.LimitedDrops.JOURNAL.dropped()) {
				itemsToSpawn.add( new OtilukesJournal() );
			}
			//first of the three portable storage chests
			itemsToSpawn.add( new PortableChest( PortableChest.WOOD ) );
			//the sickle: harvests seeds from safe-zone crops
			itemsToSpawn.add( new xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle() );
			break;
			
		case 11:
			w = (MeleeWeapon) Generator.random(Generator.wepTiers[2]);
			m = (MissileWeapon) Generator.random(Generator.misTiers[2]);
			itemsToSpawn.add( new MailArmor().identify(false) );
			break;
			
		case 16:
			w = (MeleeWeapon) Generator.random(Generator.wepTiers[3]);
			m = (MissileWeapon) Generator.random(Generator.misTiers[3]);
			itemsToSpawn.add( new ScaleArmor().identify(false) );
			itemsToSpawn.add( new PortableChest( PortableChest.GOLDEN ) );
			break;

		case 20: case 21:
			w = (MeleeWeapon) Generator.random(Generator.wepTiers[4]);
			m = (MissileWeapon) Generator.random(Generator.misTiers[4]);
			itemsToSpawn.add( new PlateArmor().identify(false) );
			itemsToSpawn.add( new Torch() );
			itemsToSpawn.add( new Torch() );
			itemsToSpawn.add( new Torch() );
			itemsToSpawn.add( new BookOfDead() );
			itemsToSpawn.add( new BookOfLife() );
			itemsToSpawn.add( new BookOfTranscendence() );
			itemsToSpawn.add( new PortableChest( PortableChest.CRYSTAL ) );
			break;
		}

		//every shop keeps a cup of coffee behind the counter
		itemsToSpawn.add( new Coffee() );

		w.enchant(null);
		w.cursed = false;
		w.level(0);
		w.identify(false);
		itemsToSpawn.add(w);

		m.enchant(null);
		m.cursed = false;
		m.level(0);
		m.identify(false);
		itemsToSpawn.add(m);
		
		itemsToSpawn.add( TippedDart.randomTipped(2) );

		itemsToSpawn.add( new Alchemize().quantity(Random.IntRange(2, 3)));

		for (int b = 0; b < 2; b++) {
			Bag bag = ChooseBag(Dungeon.hero.belongings);
			if (bag != null) {
				itemsToSpawn.add(bag);
			}
		}

		itemsToSpawn.add( new PotionOfHealing() );
		itemsToSpawn.add( Generator.randomUsingDefaults( Generator.Category.POTION ) );
		itemsToSpawn.add( Generator.randomUsingDefaults( Generator.Category.POTION ) );

		itemsToSpawn.add( new ScrollOfIdentify() );
		itemsToSpawn.add( new ScrollOfRemoveCurse() );
		itemsToSpawn.add( new ScrollOfMagicMapping() );

		for (int i=0; i < 2; i++)
			itemsToSpawn.add( Random.Int(2) == 0 ?
					Generator.randomUsingDefaults( Generator.Category.POTION ) :
					Generator.randomUsingDefaults( Generator.Category.SCROLL ) );


		itemsToSpawn.add( new SmallRation() );
		itemsToSpawn.add( new SmallRation() );
		
		switch (Random.Int(4)){
			case 0:
				itemsToSpawn.add( new Bomb() );
				break;
			case 1:
			case 2:
				itemsToSpawn.add( new Bomb.DoubleBomb() );
				break;
			case 3:
				itemsToSpawn.add( new Honeypot() );
				break;
		}

		itemsToSpawn.add( new Ankh() );
		itemsToSpawn.add( new StoneOfAugmentation() );

		TimekeepersHourglass hourglass = Dungeon.hero.belongings.getItem(TimekeepersHourglass.class);
		if (hourglass != null && hourglass.isIdentified() && !hourglass.cursed){
			int bags = 0;
			//creates the given float percent of the remaining bags to be dropped.
			//this way players who get the hourglass late can still max it, usually.
			switch (Dungeon.depth) {
				case 6:
					bags = (int)Math.ceil(( 5-hourglass.sandBags) * 0.20f ); break;
				case 11:
					bags = (int)Math.ceil(( 5-hourglass.sandBags) * 0.25f ); break;
				case 16:
					bags = (int)Math.ceil(( 5-hourglass.sandBags) * 0.50f ); break;
				case 20: case 21:
					bags = (int)Math.ceil(( 5-hourglass.sandBags) * 0.80f ); break;
			}

			for(int i = 1; i <= bags; i++){
				itemsToSpawn.add( new TimekeepersHourglass.sandBag());
				hourglass.sandBags ++;
			}
		}

		Item rare;
		switch (Random.Int(10)){
			case 0:
				rare = Generator.random( Generator.Category.WAND );
				rare.level( 0 );
				break;
			case 1:
				rare = Generator.random(Generator.Category.RING);
				rare.level( 0 );
				break;
			case 2:
				rare = Generator.random( Generator.Category.ARTIFACT );
				break;
			default:
				rare = new Stylus();
		}
		rare.cursed = false;
		rare.cursedKnown = true;
		itemsToSpawn.add( rare );

		//bulk ammo for the guns and bows, one belt per chapter of the run
		itemsToSpawn.add( new BulletBelt().quantity(Dungeon.depth/5) );

		//the quiver is one of a kind, so only restock it while the hero has none
		if (Dungeon.hero.belongings.getItem(ArrowBag.class) == null){
			itemsToSpawn.add( new ArrowBag() );
		}

		//use a new generator here to prevent items in shop stock affecting levelgen RNG (e.g. sandbags)
		//we can use a random long for the seed as it will be the same long every time
		Random.pushGenerator(Random.Long());
			Random.shuffle(itemsToSpawn);
		Random.popGenerator();

		return itemsToSpawn;
	}

	protected static Bag ChooseBag(Belongings pack){

		//generate a hashmap of all valid bags.
		HashMap<Bag, Integer> bags = new HashMap<>();
		if (!Dungeon.LimitedDrops.VELVET_POUCH.dropped()) bags.put(new VelvetPouch(), 1);
		if (!Dungeon.LimitedDrops.SCROLL_HOLDER.dropped()) bags.put(new ScrollHolder(), 0);
		if (!Dungeon.LimitedDrops.POTION_BANDOLIER.dropped()) bags.put(new PotionBandolier(), 0);
		if (!Dungeon.LimitedDrops.MAGICAL_HOLSTER.dropped()) bags.put(new MagicalHolster(), 0);
		if (!Dungeon.LimitedDrops.ANKH_CHAIN.dropped()) bags.put(new AnkhChain(), 0);
		//KeyRing is not stocked: keys were modernised into journal (Notes) records and
		//never enter the backpack, so the ring can hold nothing. Don't sell a dead bag.
		if (!Dungeon.LimitedDrops.FOOD_POUCH.dropped()) bags.put(new FoodPouch(), 0);

		if (bags.isEmpty()) return null;

		//count up items in the main bag
		for (Item item : pack.backpack.items) {
			for (Bag bag : bags.keySet()){
				if (bag.canHold(item)){
					bags.put(bag, bags.get(bag)+1);
				}
			}
		}

		//find which bag will result in most inventory savings, drop that.
		Bag bestBag = null;
		for (Bag bag : bags.keySet()){
			if (bestBag == null){
				bestBag = bag;
			} else if (bags.get(bag) > bags.get(bestBag)){
				bestBag = bag;
			}
		}

		if (bestBag instanceof VelvetPouch){
			Dungeon.LimitedDrops.VELVET_POUCH.drop();
		} else if (bestBag instanceof ScrollHolder){
			Dungeon.LimitedDrops.SCROLL_HOLDER.drop();
		} else if (bestBag instanceof PotionBandolier){
			Dungeon.LimitedDrops.POTION_BANDOLIER.drop();
		} else if (bestBag instanceof MagicalHolster){
			Dungeon.LimitedDrops.MAGICAL_HOLSTER.drop();
		} else if (bestBag instanceof AnkhChain){
			Dungeon.LimitedDrops.ANKH_CHAIN.drop();
		} else if (bestBag instanceof KeyRing){
			Dungeon.LimitedDrops.KEY_RING.drop();
		} else if (bestBag instanceof FoodPouch){
			Dungeon.LimitedDrops.FOOD_POUCH.drop();
		}

		return bestBag;

	}

}
