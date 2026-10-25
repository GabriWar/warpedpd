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
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Bread;
import xyz.gabriwar.warpedpixeldungeon.items.food.ChargrilledMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.MeatPie;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.items.food.SmallRation;
import xyz.gabriwar.warpedpixeldungeon.items.food.StewedMeat;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.FestivalCookSprite;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A food stall of a village's festival (levels/overworld/FestivalEvent): one of the two cooks who
 * set up round the well for the festival's day and night, the bakery's pies and bread or the
 * spit's roast and stew, every meal at half the shelf price. A travelling market's trader in all
 * but his wares: stocked once, struck when the festival ends, and chased off he takes the other
 * cook with him and the festival is over (OverworldLevel.merchantFled).
 */
public class FestivalCook extends TravellingMerchant {

	{
		spriteClass = FestivalCookSprite.class;
	}

	//festival fare: half the shelf price
	public static final float FESTIVAL_PRICE = 0.5f;

	public static final int BAKERY = 0, SPIT = 1;
	public static final int STALLS = 2;

	//which stall he keeps
	public int stall = BAKERY;

	public static FestivalCook of( int stall, long festival, int endTurn ){
		FestivalCook c = new FestivalCook();
		c.stall = Math.floorMod( stall, STALLS );
		c.eventId = festival;
		c.endTurn = endTurn;
		return c;
	}

	@Override
	public String name(){
		return Messages.get( this, "name" );
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( this, "talk_" + stall );
	}

	@Override
	public float priceFactor(){
		return FESTIVAL_PRICE;
	}

	//four meals for the shelf's four places: the bakery's pies, pasty and loaf, the spit's roast,
	//stew and a ration for the road
	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		if (stall == SPIT){
			stock.add( new ChargrilledMeat().quantity( 2 ) );
			stock.add( new StewedMeat().quantity( 2 ) );
			stock.add( new ChargrilledMeat() );
			stock.add( new Food() );
		} else {
			stock.add( new MeatPie() );
			stock.add( new Pasty() );
			stock.add( new Bread() );
			stock.add( new SmallRation().quantity( 2 ) );
		}
		for (Item it : stock) it.identify( false );
		return stock;
	}

	private static final String STALL = "festival_stall";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STALL, stall );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		stall = bundle.contains( STALL ) ? Math.floorMod( bundle.getInt( STALL ), STALLS ) : BAKERY;
		//the trader's own reading set a trader's sprite
		spriteClass = FestivalCookSprite.class;
	}
}
