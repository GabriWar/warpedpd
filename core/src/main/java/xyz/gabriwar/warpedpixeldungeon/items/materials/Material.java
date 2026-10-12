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

package xyz.gabriwar.warpedpixeldungeon.items.materials;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;

import java.util.ArrayList;

/**
 * A building material of the homesteads: logs, planks, stone, clay, bricks, glass, thatch, rope,
 * hides and the rest. One class per material, so the pack stacks each on its own and a save needs
 * nothing but the class and the count. They ride in the Builder's Satchel (BuildersSatchel) and
 * sell for a little: gathering never pays better than the dungeon.
 *
 * {@link #count} and {@link #spend} are how anything that costs materials (the build pieces,
 * recipes) reads and takes them from a hero: the satchel first, then the rest of the pack.
 */
public abstract class Material extends Item {

	{
		stackable = true;
		bones = false;
	}

	/** Every material there is. */
	@SuppressWarnings("unchecked")
	public static final Class<? extends Material>[] ALL = new Class[]{
			Log.class, PineLog.class, Plank.class, Resin.class, RoughStone.class, CutStone.class,
			Clay.class, FiredBrick.class, Sand.class, GlassPane.class, Thatch.class, Rope.class,
			Hide.class, Leather.class, Charcoal.class, IronFittings.class
	};

	/** What one of it sells for. */
	public abstract int unitValue();

	@Override
	public int value(){
		return unitValue() * quantity;
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	/** How many of a material the hero carries, in the satchel and the pack alike. Gold is the
	 *  purse's (Dungeon.gold): a piece or a recipe may cost gold as well (BuildPiece.Cost.gold). */
	public static int count( Hero hero, Class<? extends Item> material ){
		if (material == Gold.class) return Dungeon.gold;
		int n = 0;
		for (Item i : stacks( hero, material )) n += i.quantity();
		return n;
	}

	/**
	 * Takes `amount` of a material from the hero, the satchel's stacks first: false, and nothing
	 * taken, when he carries fewer.
	 */
	public static boolean spend( Hero hero, Class<? extends Item> material, int amount ){
		if (amount <= 0) return true;
		if (material == Gold.class){
			if (Dungeon.gold < amount) return false;
			Dungeon.gold -= amount;
			return true;
		}
		ArrayList<Item> stacks = stacks( hero, material );
		int have = 0;
		for (Item i : stacks) have += i.quantity();
		if (have < amount) return false;
		for (Item i : stacks){
			if (amount <= 0) break;
			int take = Math.min( amount, i.quantity() );
			if (take == i.quantity()){
				i.detachAll( hero.belongings.backpack );
			} else {
				i.quantity( i.quantity() - take );
				i.updateQuickslot();
			}
			amount -= take;
		}
		return true;
	}

	//every stack of a material the hero carries: those in a builder's satchel first
	private static ArrayList<Item> stacks( Hero hero, Class<? extends Item> material ){
		ArrayList<Item> satchel = new ArrayList<>(), rest = new ArrayList<>();
		collect( hero.belongings.backpack, material, false, satchel, rest );
		satchel.addAll( rest );
		return satchel;
	}

	private static void collect( Bag bag, Class<? extends Item> material, boolean inSatchel,
			ArrayList<Item> satchel, ArrayList<Item> rest ){
		for (Item i : bag.items){
			if (i instanceof Bag){
				collect( (Bag) i, material, inSatchel || i instanceof BuildersSatchel, satchel, rest );
			} else if (i.getClass() == material){
				(inSatchel ? satchel : rest).add( i );
			}
		}
	}
}
