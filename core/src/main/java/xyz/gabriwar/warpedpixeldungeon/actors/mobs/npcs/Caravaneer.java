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

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ShopkeeperVariantSprite;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * The road between two villages, walked for a living: a travelling stall that
 * stands a day's march further along its segment every weekday and is gone by
 * the next morning, stock and all (OverworldLevel.placeCaravans). What he
 * carries is a bit of everything.
 */
public class Caravaneer extends Shopkeeper {

	{
		spriteClass = ShopkeeperVariantSprite.class;
	}

	//the weekday this stall was pitched for; a different one and he has moved on
	public int day = -1;

	@Override
	public CharSprite sprite() {
		return new ShopkeeperVariantSprite( 3 );
	}

	@Override
	public String name() {
		return Messages.get( this, "name" );
	}

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		Generator.Category[] wares = {
				Generator.Category.POTION, Generator.Category.SCROLL, Generator.Category.FOOD,
				Generator.Category.MISSILE, Generator.Category.SEED, Generator.Category.STONE };
		for (Generator.Category cat : wares){
			Item it = Generator.random( cat );
			if (it == null) continue;
			it.cursed = false;
			it.identify( false );
			stock.add( it );
		}
		return stock;
	}

	@Override
	public String chatText(){
		return chatTextFor( null );
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( this, "talk_" + GameCalendar.weekday().name().toLowerCase() );
	}

	private boolean firstStock = false;

	/** Requests the initial shelf fill, on the caravaneer's first act - the
	 *  overworld can pitch him while Dungeon.level is still being built. */
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
	private static final String DAY = "day";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FIRST_STOCK, firstStock );
		bundle.put( DAY, day );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		firstStock = bundle.getBoolean( FIRST_STOCK );
		day = bundle.getInt( DAY );
	}
}
