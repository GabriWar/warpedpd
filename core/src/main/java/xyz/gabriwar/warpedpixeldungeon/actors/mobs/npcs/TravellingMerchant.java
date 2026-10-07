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
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ExoticScroll;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ExoticMerchantSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RelicMerchantSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.TravellingMerchantSprite;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A trader of the travelling market (levels/overworld/WorldEvents): one of the two or three
 * stalls pitched round a human village's well for three days (OverworldLevel.openMarket). What
 * the villages' own vendors never carry, from far off and priced for the miles: curios (rings
 * and wands), relics (artifacts and rings) or exotic brews and scrolls. The stall is stocked
 * once - the wagon carries what it carries - and struck, shelf and all, when the market ends
 * or when he is chased off, which ends the market for every stall of it.
 */
public class TravellingMerchant extends Shopkeeper {

	{
		spriteClass = TravellingMerchantSprite.class;
	}

	public static final int CURIOS = 0, RELICS = 1, EXOTICS = 2;
	public static final int SPECIALITIES = 3;

	//the wagon's mark-up: goods carried from far off
	public static final float MARK_UP = 1.25f;

	public int speciality = CURIOS;
	//the market (WorldEvents id) this stall belongs to
	public long eventId = 0;
	//the world turn the wagon rolls on: OverworldLevel strikes the stall then
	public int endTurn = 0;
	//the world cell the stall was pitched on: the shelf that comes down with him is the one
	//round it, wherever the window has since set him down
	public int stallX = Integer.MIN_VALUE, stallY = Integer.MIN_VALUE;

	private boolean firstStock = false;
	//the one fill of the shelf has been laid out
	private boolean stocked = true;

	public static TravellingMerchant of( int speciality, long eventId, int endTurn ){
		TravellingMerchant m = new TravellingMerchant();
		m.speciality = speciality;
		m.eventId = eventId;
		m.endTurn = endTurn;
		m.applySpeciality();
		return m;
	}

	//one sprite class per speciality: a co-op guest rebuilds a sprite from its class alone
	private void applySpeciality(){
		speciality = Math.floorMod( speciality, SPECIALITIES );
		switch (speciality){
			case RELICS:  spriteClass = RelicMerchantSprite.class; break;
			case EXOTICS: spriteClass = ExoticMerchantSprite.class; break;
			default:      spriteClass = TravellingMerchantSprite.class; break;
		}
	}

	@Override
	public String name(){
		return Messages.get( this, "name_" + speciality );
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( this, "talk_" + speciality );
	}

	@Override
	public float priceFactor(){
		return MARK_UP;
	}

	//bandits in the streets of the market's village (RaidEvent): the stall is shut like the
	//village's own...
	@Override
	public String tradeBlock(){
		return RaidEvent.ongoing( village() ) ? Messages.get( RaidEvent.class, "shut_raid" ) : null;
	}

	//...and the trader ducks behind it: a blast meant for a raider is not an attack on him
	@Override
	public void processHarm(){
		if (!RaidEvent.ongoing( village() )) super.processHarm();
	}

	//the market's village (packed sector): the stall stands within twelve cells of its well, and
	//a well is never nearer than a quarter sector to its sector's edge (WorldStructures.siteX)
	private long village(){
		if (stallX == Integer.MIN_VALUE) return Long.MIN_VALUE;
		return WorldStructures.sectorOf( Math.floorDiv( stallX, WorldStructures.SECTOR ), Math.floorDiv( stallY, WorldStructures.SECTOR ) );
	}

	/** Requests the shelf's one fill, on his first act - the overworld can pitch the stall
	 *  while Dungeon.level is still being built. */
	public void openShop(){
		firstStock = true;
		stocked = false;
	}

	@Override
	protected boolean act(){
		if (firstStock){
			firstStock = false;
			//the stall is where he stands as the goods go out: the shelf his strike takes down
			if (Dungeon.level instanceof OverworldLevel){
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				stallX = ow.worldX() + pos % ow.width();
				stallY = ow.worldY() + pos / ow.width();
			}
			lastRestock = restockClock();
			restock();
		}
		return super.act();
	}

	//stocked once: a market that runs over the week's turn does not restock, so no second roll
	//of rare goods is made only to be thrown away for want of shelf room
	@Override
	public void restock(){
		if (stocked) return;
		stocked = true;
		super.restock();
	}

	//four wares, the shelf's four places: nothing generated is lost
	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		for (int i = 0; i < 4; i++){
			Item it;
			if (speciality == RELICS){
				//once the run's artifacts are all out, the generator hands a ring instead
				it = Generator.random( i < 2 ? Generator.Category.ARTIFACT : Generator.Category.RING );
			} else if (speciality == EXOTICS){
				boolean potion = i % 2 == 0;
				Item base = Generator.random( potion ? Generator.Category.POTION : Generator.Category.SCROLL );
				Class<? extends Item> exotic = potion ? ExoticPotion.regToExo.get( base.getClass() )
						: ExoticScroll.regToExo.get( base.getClass() );
				//a base already exotic stays as it came
				it = exotic != null ? Generator.random( exotic ) : base;
			} else {
				it = Generator.random( i % 2 == 0 ? Generator.Category.RING : Generator.Category.WAND );
			}
			if (it == null) continue;
			//the shoproom treatment: no cursed surprises, known goods
			it.cursed = false;
			it.identify( false );
			stock.add( it );
		}
		return stock;
	}

	//chased off, he does not run alone: the whole troupe packs up and the market is over
	//(OverworldLevel.merchantFled), each stall taking only its own shelf - a shopkeeper's flight
	//would otherwise reach every keeper's goods on the level
	@Override
	public void flee(){
		if (Dungeon.level instanceof OverworldLevel) ((OverworldLevel) Dungeon.level).merchantFled( this );
		else super.flee();
	}

	@Override
	public void destroy(){
		if (Dungeon.level instanceof OverworldLevel) ((OverworldLevel) Dungeon.level).merchantFled( this );
		else super.destroy();
	}

	private static final String FIRST_STOCK = "first_stock";
	private static final String SPECIALITY  = "speciality";
	private static final String EVENT_ID    = "event_id";
	private static final String END_TURN    = "end_turn";
	private static final String STOCKED     = "stocked";
	private static final String STALL_X     = "stall_x";
	private static final String STALL_Y     = "stall_y";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( FIRST_STOCK, firstStock );
		bundle.put( SPECIALITY, speciality );
		bundle.put( EVENT_ID, eventId );
		bundle.put( END_TURN, endTurn );
		bundle.put( STOCKED, stocked );
		bundle.put( STALL_X, stallX );
		bundle.put( STALL_Y, stallY );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		firstStock = bundle.contains( FIRST_STOCK ) && bundle.getBoolean( FIRST_STOCK );
		speciality = bundle.contains( SPECIALITY ) ? bundle.getInt( SPECIALITY ) : CURIOS;
		applySpeciality();
		eventId = bundle.contains( EVENT_ID ) ? bundle.getLong( EVENT_ID ) : 0;
		//no known end: struck at the next expiry pass
		endTurn = bundle.contains( END_TURN ) ? bundle.getInt( END_TURN ) : 0;
		stocked = !bundle.contains( STOCKED ) || bundle.getBoolean( STOCKED );
		stallX = bundle.contains( STALL_X ) ? bundle.getInt( STALL_X ) : Integer.MIN_VALUE;
		stallY = bundle.contains( STALL_Y ) ? bundle.getInt( STALL_Y ) : Integer.MIN_VALUE;
	}
}
