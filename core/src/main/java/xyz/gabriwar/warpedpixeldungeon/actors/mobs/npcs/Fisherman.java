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
import xyz.gabriwar.warpedpixeldungeon.items.food.Fish;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.VillagerSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The man on the plank bridge. He works one river crossing for good, sells
 * what he pulls out of it (and a potion he traded off a passing peddler), and
 * restocks on the universal shop timer like every other trader.
 */
public class Fisherman extends Shopkeeper {

	{
		spriteClass = VillagerSprite.class;
	}

	@Override
	public CharSprite sprite() {
		return new VillagerSprite( VillagerSprite.THIEF, 4 );
	}

	@Override
	public String name() {
		return Messages.get( this, "name" );
	}

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		stock.add( new Fish().quantity( Random.IntRange( 2, 3 ) ) );
		Item potion = Generator.random( Generator.Category.POTION );
		if (potion != null){
			potion.cursed = false;
			potion.identify( false );
			stock.add( potion );
		}
		return stock;
	}

	@Override
	public String chatText(){
		return chatTextFor( null );
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( this, "talk_" + GameCalendar.season().name().toLowerCase() );
	}

	private boolean firstStock = false;

	/** Requests the initial shelf fill. It happens on the fisherman's first act,
	 *  when the level is guaranteed live - during the overworld's own build
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

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FIRST_STOCK, firstStock );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		firstStock = bundle.getBoolean( FIRST_STOCK );
	}
}
