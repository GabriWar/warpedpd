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

import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Coffee;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ShopkeeperVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A settlement shop with a speciality: potions, scrolls, food, gear or seeds.
 * Restocks on the universal shop timer.
 */
public class OverworldShopkeeper extends Shopkeeper {

	public static final int POTIONS = 0, SCROLLS = 1, FOOD = 2, WEAPONS = 3,
			ARMOR = 4, MISSILES = 5, SEEDS = 6, WANDS = 7,
			RINGS = 8, STONES = 9, ARTIFACTS = 10;
	public static final int SPECIALITIES = 11;

	{
		spriteClass = ShopkeeperVariantSprite.class;
	}

	public int speciality = POTIONS;
	public int tint = 0;
	public long homeSector = Long.MIN_VALUE;

	public static OverworldShopkeeper of( int speciality, long homeSector ){
		OverworldShopkeeper k = new OverworldShopkeeper();
		k.speciality = speciality;
		k.tint = Random.Int( 6 );
		k.homeSector = homeSector;
		return k;
	}

	@Override
	public CharSprite sprite() {
		return new ShopkeeperVariantSprite( tint );
	}

	@Override
	public String name() {
		return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "name_" + speciality );
	}

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		//every settlement trader occasionally carries the magic world map,
		//and the very first shelf of a shop always includes one
		if (firstStock || Random.Int( 4 ) == 0){
			stock.add( new xyz.gabriwar.warpedpixeldungeon.items.MagicWorldMap() );
		}
		for (int i = 0; i < 6; i++){
			Item it;
			switch (speciality){
				case POTIONS: default:
					it = Generator.random( Generator.Category.POTION ); break;
				case SCROLLS:
					it = Generator.random( Generator.Category.SCROLL ); break;
				case FOOD:
					it = Random.Int( 4 ) == 0 ? new Coffee()
							: Random.Int( 2 ) == 0 ? new Pasty() : new Food(); break;
				case WEAPONS:
					it = Generator.random( Generator.Category.WEAPON ); break;
				case ARMOR:
					it = Generator.random( Generator.Category.ARMOR ); break;
				case MISSILES:
					//half the shelf is ammunition: belts, bullets and arrows
					if (i % 2 == 0){
						switch (Random.Int( 3 )){
							case 0: it = new xyz.gabriwar.warpedpixeldungeon.items.BulletBelt(); break;
							case 1: it = new xyz.gabriwar.warpedpixeldungeon.items.BulletItem()
									.quantity( Random.IntRange( 25, 50 ) ); break;
							default: it = new xyz.gabriwar.warpedpixeldungeon.items.ArrowItem()
									.quantity( Random.IntRange( 25, 50 ) ); break;
						}
					} else {
						it = Generator.random( Generator.Category.MISSILE );
					}
					break;
				case SEEDS:
					it = Generator.random( Generator.Category.SEED ); break;
				case WANDS:
					it = Generator.random( Generator.Category.WAND ); break;
				case RINGS:
					it = Generator.random( Generator.Category.RING ); break;
				case STONES:
					it = Generator.random( Generator.Category.STONE ); break;
				case ARTIFACTS:
					it = Generator.random( Generator.Category.ARTIFACT ); break;
			}
			if (it != null){
				//the shoproom treatment: no cursed surprises, known goods
				it.cursed = false;
				it.identify( false );
				stock.add( it );
			}
		}
		return stock;
	}

	private boolean firstStock = false;

	/** Requests the initial shelf fill. It happens on the keeper's first act,
	 *  when the level is guaranteed live - during the first overworld build
	 *  Dungeon.level is still null and an immediate restock would NPE. */
	public void openShop(){
		firstStock = true;
	}

	@Override
	protected boolean act(){
		if (firstStock){
			firstStock = false;
			lastRestock = restockClock();
			restock();
		}
		return super.act();
	}

	private static final String FIRST_STOCK = "first_stock";
	private static final String SPEC = "speciality";
	private static final String TINT = "tint";
	private static final String HOME = "home_sector";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FIRST_STOCK, firstStock );
		bundle.put( SPEC, speciality );
		bundle.put( TINT, tint );
		bundle.put( HOME, homeSector );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		firstStock = bundle.getBoolean( FIRST_STOCK );
		speciality = bundle.getInt( SPEC );
		tint = bundle.getInt( TINT );
		homeSector = bundle.getLong( HOME );
	}
}
