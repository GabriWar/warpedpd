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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.EmptyBottle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Dates;
import xyz.gabriwar.warpedpixeldungeon.items.misc.DesertScarf;
import xyz.gabriwar.warpedpixeldungeon.items.misc.SandGoggles;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Drylands;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoadTraffic;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.OasisTraderSprite;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * The trader who keeps his rug on an oasis' bank (levels/overworld/Drylands, DrylandsLife). He
 * sells what the desert calls for - sand goggles and a desert scarf against the sandstorms, dates
 * and empty bottles to fill at the pool - at a desert mark-up, buys like any keeper, and knows the
 * sands: he tells which way the nearest buried temple lies, if one lies within a few days' walk.
 * Kept for good, as every keeper; one driven off is replaced on the oasis' next peopling.
 */
public class OasisTrader extends Shopkeeper {

	{
		spriteClass = OasisTraderSprite.class;
	}

	/** What he asks over a shop's shelf price: everything here came across the sand. */
	public static final float MARK_UP = 1.25f;
	/** How far round his oasis he knows the temples, in sectors. */
	public static final int KNOWS = 4;

	//the oasis he keeps his rug at (sector), for his word on the temples
	public int oasisSx, oasisSy;
	private boolean firstStock = false;

	public static OasisTrader of( int sx, int sy ){
		OasisTrader t = new OasisTrader();
		t.oasisSx = sx;
		t.oasisSy = sy;
		return t;
	}

	/** Requests the first shelf fill, on his first act (the level is live by then). */
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
		stock.add( new SandGoggles() );
		stock.add( new DesertScarf() );
		stock.add( new Dates().quantity( 4 ) );
		stock.add( new EmptyBottle().quantity( 3 ) );
		return stock;
	}

	@Override
	public float priceFactor(){
		return MARK_UP;
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( OasisTrader.class, "talk" ) + "\n\n" + temples();
	}

	//which way the nearest buried temple lies from his oasis, and how far, or that he knows of none
	private String temples(){
		if (!(Dungeon.level instanceof OverworldLevel)) return Messages.get( OasisTrader.class, "no_temple" );
		long seed = ((OverworldLevel) Dungeon.level).worldSeed();
		int wx = WorldStructures.siteX( seed, oasisSx, oasisSy ), wy = WorldStructures.siteY( seed, oasisSx, oasisSy );
		ArrayList<Drylands.Place> near = Drylands.near( seed, wx, wy, WorldStructures.Site.BURIED_TEMPLE, KNOWS );
		if (near.isEmpty()) return Messages.get( OasisTrader.class, "no_temple" );
		Drylands.Place t = near.get( 0 );
		int dx = t.cx - wx, dy = t.cy - wy;
		double dist = Math.sqrt( (double) dx * dx + (double) dy * dy );
		String far = Messages.get( Traveller.class, dist < 70 ? "dist_near" : dist < 160 ? "dist_half" : "dist_far" );
		return Messages.get( OasisTrader.class, "temple", far, Messages.get( Traveller.class, "dir_" + RoadTraffic.compass( dx, dy ) ) );
	}

	private static final String FIRST_STOCK = "first_stock";
	private static final String OASIS_X = "oasis_sx", OASIS_Y = "oasis_sy";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( FIRST_STOCK, firstStock );
		bundle.put( OASIS_X, oasisSx );
		bundle.put( OASIS_Y, oasisSy );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		firstStock = bundle.contains( FIRST_STOCK ) && bundle.getBoolean( FIRST_STOCK );
		oasisSx = bundle.getInt( OASIS_X );
		oasisSy = bundle.getInt( OASIS_Y );
	}
}
