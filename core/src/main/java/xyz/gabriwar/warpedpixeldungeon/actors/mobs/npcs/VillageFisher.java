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
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Fish;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Raft;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Sailboat;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.VillageFisherSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The fisher of a human village by the water (levels/overworld/SeaWorld.settleFisher): her stall
 * stands on the village's bank nearest deep water, and she sells the boats she builds between her
 * catches - a raft and a sailboat (items/sea) - with a little of the catch and a coil of rope. She
 * restocks on the shops' weekly round, keeps the village's hours and shuts during a raid on it.
 */
public class VillageFisher extends Shopkeeper {

	{
		spriteClass = VillageFisherSprite.class;
	}

	//the settlement she belongs to (its sector key)
	public long homeSector = Long.MIN_VALUE;
	private boolean firstStock = false;

	public static VillageFisher of( long homeSector ){
		VillageFisher f = new VillageFisher();
		f.homeSector = homeSector;
		return f;
	}

	/** Requests the first shelf fill, on her first act (the level is live by then). */
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

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		stock.add( new Raft() );
		stock.add( new Sailboat() );
		stock.add( new Fish().quantity( Random.IntRange( 1, 3 ) ) );
		stock.add( new Rope().quantity( Random.IntRange( 2, 3 ) ) );
		return stock;
	}

	//a raid on her village, or the shops being mended after one (RaidEvent)
	@Override
	public String tradeBlock(){
		return RaidEvent.shutReason( homeSector );
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( VillageFisher.class, "talk_" + GameCalendar.season().name().toLowerCase() );
	}

	@Override
	public String chatText(){
		return chatTextFor( null );
	}

	private static final String HOME = "home_sector";
	private static final String FIRST_STOCK = "first_stock";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HOME, homeSector );
		bundle.put( FIRST_STOCK, firstStock );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		homeSector = bundle.contains( HOME ) ? bundle.getLong( HOME ) : Long.MIN_VALUE;
		firstStock = bundle.getBoolean( FIRST_STOCK );
	}
}
