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
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.sea.SeaChart;
import xyz.gabriwar.warpedpixeldungeon.items.sea.SeaLantern;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SeaSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.LighthouseKeeperSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * The keeper of a lighthouse (levels/overworld/SeaSites, SeaWorld): he stands before its door and
 * sells what a sailor needs from him - a chart of the isles round his light (items/sea/SeaChart) and
 * his storm lanterns (items/sea/SeaLantern). Kept forever in the parked store as every NPC is; driven
 * off, he never comes back, and the lamp burns on without him.
 */
public class LighthouseKeeper extends Shopkeeper {

	{
		spriteClass = LighthouseKeeperSprite.class;
	}

	//the light he keeps: its key and its base's world cell
	public long lightKey = 0;
	public int lightX = Integer.MIN_VALUE, lightY = Integer.MIN_VALUE;
	private boolean firstStock = false;

	public static LighthouseKeeper of( long lightKey, int lightX, int lightY ){
		LighthouseKeeper k = new LighthouseKeeper();
		k.lightKey = lightKey;
		k.lightX = lightX;
		k.lightY = lightY;
		return k;
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
		String name = "";
		if (lightX != Integer.MIN_VALUE && Dungeon.level instanceof OverworldLevel){
			long seed = ((OverworldLevel) Dungeon.level).worldSeed();
			name = SeaSites.lightName( seed, Math.floorDiv( lightX, WorldStructures.SECTOR ), Math.floorDiv( lightY, WorldStructures.SECTOR ) );
		}
		stock.add( SeaChart.of( lightX, lightY, name ) );
		stock.add( new SeaLantern().quantity( 2 ) );
		return stock;
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( LighthouseKeeper.class, "talk_" + Math.floorMod( WorldClock.day() + (int) lightKey, 4 ) );
	}

	@Override
	public String chatText(){
		return chatTextFor( null );
	}

	//driven off: his shelf comes down with him and the light has no keeper again
	@Override
	public void flee(){
		destroy();
		Notes.remove( landmark() );
		GLog.newLine();
		GLog.n( Messages.get( this, "flee" ) );
		if (sprite != null){
			sprite.killAndErase();
			CellEmitter.get( pos ).burst( ElmoParticle.FACTORY, 6 );
		}
		if (Dungeon.level instanceof OverworldLevel && lightKey != 0){
			((OverworldLevel) Dungeon.level).markSiteCleared( lightKey );
		}
	}

	private static final String LIGHT = "light_key";
	private static final String LIGHT_X = "light_x";
	private static final String LIGHT_Y = "light_y";
	private static final String FIRST_STOCK = "first_stock";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LIGHT, lightKey );
		bundle.put( LIGHT_X, lightX );
		bundle.put( LIGHT_Y, lightY );
		bundle.put( FIRST_STOCK, firstStock );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		lightKey = bundle.getLong( LIGHT );
		lightX = bundle.contains( LIGHT_X ) ? bundle.getInt( LIGHT_X ) : Integer.MIN_VALUE;
		lightY = bundle.contains( LIGHT_Y ) ? bundle.getInt( LIGHT_Y ) : Integer.MIN_VALUE;
		firstStock = bundle.getBoolean( FIRST_STOCK );
	}
}
