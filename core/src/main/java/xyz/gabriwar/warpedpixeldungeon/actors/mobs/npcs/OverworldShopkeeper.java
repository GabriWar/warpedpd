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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Coffee;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ShopkeeperVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A settlement shop with a speciality: potions, scrolls, food, gear or seeds; past them the
 * village carpenter and the stablehand, who lay out their own goods. Restocks on the universal
 * shop timer. A seed trader keeps a bug net among his stock, a food trader now and then
 * (items/catching/BugNet).
 */
public class OverworldShopkeeper extends Shopkeeper {

	public static final int POTIONS = 0, SCROLLS = 1, FOOD = 2, WEAPONS = 3,
			ARMOR = 4, MISSILES = 5, SEEDS = 6, WANDS = 7,
			RINGS = 8, STONES = 9, ARTIFACTS = 10;
	public static final int SPECIALITIES = 11;
	//the village carpenter, past the rotation above: every village or larger has one besides its
	//usual traders (homestead/gathering/Carpentry)
	public static final int CARPENTER = 11;
	//the village stablehand, past it: a horse for sale, saddles and hay (homestead/mounts/Stables)
	public static final int STABLE = 12;

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

	//a raid on the settlement, or the shop being mended after one (RaidEvent)
	@Override
	public String tradeBlock(){
		return RaidEvent.shutReason( homeSector );
	}

	//the price a saved settlement gives the hero who saved it (RaidEvent), and the better price his
	//standing there earns him with every rank (bounty/Bounties)
	@Override
	public float priceFactor(){
		return RaidEvent.priceFactor( homeSector )
				* xyz.gabriwar.warpedpixeldungeon.bounty.BountyActions.priceFactor( homeSector );
	}

	//in a raid the keeper is hiding behind the counter: a blast meant for a raider is not an attack on him
	@Override
	public void processHarm(){
		if (!RaidEvent.ongoing( homeSector )) super.processHarm();
	}

	//the carpenter's shelf is what a builder cannot gather, all of it every time and no world map
	//(homestead/gathering/Carpentry): not the few picked at random from a trader's stock
	@Override
	public void restock(){
		if (speciality == STABLE){
			//the stablehand's rail: a saddle, hay, and a horse when the last was sold
			if (xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Stables.lay( this ) > 0
					&& xyz.gabriwar.warpedpixeldungeon.net.NetManager.anyHeroSees( pos )){
				yell( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Shopkeeper.class, "restock" ) );
			}
			return;
		}
		if (speciality != CARPENTER){
			super.restock();
			return;
		}
		if (xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Carpentry.lay( this ) > 0
				&& xyz.gabriwar.warpedpixeldungeon.net.NetManager.anyHeroSees( pos )){
			yell( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Shopkeeper.class, "restock" ) );
		}
	}

	//the stablehand speaks of his horses
	@Override
	public String chatTextFor( Hero h ){
		if (speciality == STABLE) return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "talk_stable" );
		return super.chatTextFor( h );
	}

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		//every settlement trader occasionally carries the magic world map,
		//and the very first shelf of a shop always includes one
		if (firstStock || Random.Int( 4 ) == 0){
			stock.add( new xyz.gabriwar.warpedpixeldungeon.items.MagicWorldMap() );
		}
		//every shop keeps a stack of five empty bottles
		stock.add( new xyz.gabriwar.warpedpixeldungeon.items.EmptyBottle().quantity( 5 ) );
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
		//a bug net for the garden's butterflies on every seed trader's shelf, and now and then on a
		//food trader's (items/catching/BugNet)
		if (speciality == SEEDS || (speciality == FOOD && Random.Int( 3 ) == 0)){
			stock.add( new xyz.gabriwar.warpedpixeldungeon.items.catching.BugNet() );
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
