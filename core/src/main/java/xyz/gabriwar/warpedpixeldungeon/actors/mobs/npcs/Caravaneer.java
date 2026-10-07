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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ShopkeeperVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The road between two villages, walked for a living: a travelling stall that
 * stands a day's march further along its segment every weekday and is gone by
 * the next morning, stock and all (OverworldLevel.placeCaravans). What he
 * carries is a bit of everything. Now and then the outlaws lie in wait for
 * him (CaravanAmbush): saved, he marks his prices down for the rest of the day.
 */
public class Caravaneer extends Shopkeeper {

	{
		spriteClass = ShopkeeperVariantSprite.class;
	}

	//the weekday this stall was pitched for; a different one and he has moved on
	public int day = -1;

	//the day's trouble at this stall (CaravanAmbush): none; outlaws waiting for the hero to come
	//in sight; under attack; and how it ended - saved by the hero, robbed while he walked on,
	//or spared by somebody else
	public static final int AMBUSH_NONE = 0, AMBUSH_PENDING = 1, AMBUSH_BESIEGED = 2,
			AMBUSH_SAVED = 3, AMBUSH_ROBBED = 4, AMBUSH_SPARED = 5;
	public int ambush = AMBUSH_NONE;
	//the day (WorldClock.day) the stall went up; -1 on stalls from older saves
	public int pitched = -1;
	//this stall's mark on the outlaws sent against it (OverworldBandit.ambushOf): one per cell and day
	public long stall = OverworldBandit.NO_AMBUSH;
	//the road it stands on (RoadTraffic's key for it), by which a fought-out ambush is remembered
	//for the rest of the day; Long.MIN_VALUE on stalls from older saves, which are never fallen on
	public long road = Long.MIN_VALUE;
	//a rescued caravaneer's prices for the rest of that day
	public static final float SAVED_DISCOUNT = 0.7f;
	//what he carries: a bit of everything
	private static final Generator.Category[] WARES = {
			Generator.Category.POTION, Generator.Category.SCROLL, Generator.Category.FOOD,
			Generator.Category.MISSILE, Generator.Category.SEED, Generator.Category.STONE };

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
		for (Generator.Category cat : WARES){
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
		switch (ambush){
			case AMBUSH_SAVED:
				if (priceFactor() < 1f) return Messages.get( this, "talk_saved" );
				break;
			case AMBUSH_ROBBED:
				return Messages.get( this, "talk_robbed" );
			case AMBUSH_SPARED:
				return Messages.get( this, "talk_spared" );
		}
		return Messages.get( this, "talk_" + GameCalendar.weekday().name().toLowerCase() );
	}

	@Override
	public float priceFactor(){
		return ambush == AMBUSH_SAVED && pitched == WorldClock.day() ? SAVED_DISCOUNT : 1f;
	}

	@Override
	public String tradeBlock(){
		return ambush == AMBUSH_BESIEGED ? Messages.get( this, "talk_besieged" ) : null;
	}

	/** What a rescued caravaneer presses on his rescuer: one of his own wares, or a purse. */
	public Item gift(){
		if (Random.Int( 3 ) == 0) return new Gold( Random.IntRange( 60, 160 ) );
		Item it = Generator.random( Random.element( WARES ) );
		if (it == null) return new Gold( Random.IntRange( 60, 160 ) );
		it.cursed = false;
		it.identify( false );
		return it;
	}

	@Override
	public int defenseSkill( Char enemy ){
		//he ducks behind his wares: no outlaw's blow ever lands on him
		return enemy instanceof OverworldBandit ? INFINITE_EVASION : super.defenseSkill( enemy );
	}

	@Override
	public void damage( int dmg, Object src ){
		//under siege he keeps his head down: nothing flying about in the fight sends him packing
		if (ambush == AMBUSH_BESIEGED || src instanceof OverworldBandit) return;
		super.damage( dmg, src );
	}

	@Override
	public boolean add( Buff buff ){
		if (ambush == AMBUSH_BESIEGED) return false;
		return super.add( buff );
	}

	@Override
	public void flee(){
		//nor does anything else chase him off his stall (a thief's lift gone wrong, the ascent):
		//it would leave the siege, and his outlaws, with nothing to settle on
		if (ambush == AMBUSH_BESIEGED) return;
		super.flee();
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
		//the stall's trouble goes on on its own turn, not only on the host hero's steps: the outlaws
		//break cover as any hero comes in sight (a co-op guest walking up while the host rests),
		//and a siege settles however it ended (the last outlaw cut down by an arrow, or by the watch)
		if (Dungeon.level instanceof OverworldLevel){
			if (ambush == AMBUSH_PENDING) ((OverworldLevel) Dungeon.level).springIfDue( this );
			else if (ambush == AMBUSH_BESIEGED) ((OverworldLevel) Dungeon.level).settleSiege( this );
		}
		return super.act();
	}

	private static final String FIRST_STOCK = "first_stock";
	private static final String DAY = "day";
	private static final String AMBUSH = "ambush";
	private static final String PITCHED = "pitched";
	private static final String STALL = "stall";
	private static final String ROAD = "road";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FIRST_STOCK, firstStock );
		bundle.put( DAY, day );
		bundle.put( AMBUSH, ambush );
		bundle.put( PITCHED, pitched );
		bundle.put( STALL, stall );
		bundle.put( ROAD, road );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		firstStock = bundle.getBoolean( FIRST_STOCK );
		day = bundle.getInt( DAY );
		ambush = bundle.contains( AMBUSH ) ? bundle.getInt( AMBUSH ) : AMBUSH_NONE;
		pitched = bundle.contains( PITCHED ) ? bundle.getInt( PITCHED ) : -1;
		stall = bundle.contains( STALL ) ? bundle.getLong( STALL ) : OverworldBandit.NO_AMBUSH;
		road = bundle.contains( ROAD ) ? bundle.getLong( ROAD ) : Long.MIN_VALUE;
	}
}
