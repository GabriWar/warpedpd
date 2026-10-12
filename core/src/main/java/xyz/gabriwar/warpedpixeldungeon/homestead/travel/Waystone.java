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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.WaystoneSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/**
 * A WAYSTONE: a tall slate with a rune, standing by the well of the town and of every big settlement
 * (Waystones, put down by TravelSlice). The hero's touch ATTUNES it for the rest of the run
 * (TravelWorld): from then on it is a place to travel to, and from it he travels (WndTravel) to any
 * of his homes and attuned waystones. Unattuned, its rune is dormant; attuned, it glows.
 *
 * It is the world's, not the hero's: never hurt, never moved, never touched by a buff; an NPC, so
 * the surface keeps it for good, parked with the window and put back like the settlements' people.
 * A co-op guest's touch attunes it too (the host owns the record), but only the host travels.
 */
public class Waystone extends NPC {

	{
		spriteClass = WaystoneSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	/** Its settlement's key (Waystones: the sector, or Waystones.TOWN). */
	public long key;
	/** The world cell it was put down on: what it is travelled to and known by. */
	public int wx, wy;
	/** Its settlement's name. */
	public String place = "";

	/** Has the hero attuned it? */
	public boolean attuned(){
		TravelWorld tw = TravelWorld.get();
		return tw != null && tw.isAttuned( key );
	}

	@Override
	public String name(){
		return Messages.get( this, attuned() ? "name_attuned" : "name" );
	}

	@Override
	public String description(){
		return Messages.get( this, attuned() ? "desc_attuned" : "desc", place );
	}

	@Override
	protected boolean act(){
		//once the hero has laid eyes on it the map pins it, dim until he touches it
		if (Dungeon.level != null && Dungeon.level.heroFOV != null && pos >= 0 && pos < Dungeon.level.heroFOV.length
				&& Dungeon.level.heroFOV[pos]){
			TravelWorld tw = TravelWorld.get();
			if (tw != null) tw.see( key, wx, wy, place );
		}
		spend( TICK );
		return true;
	}

	@Override
	public void damage( int dmg, Object src ){
		//slate older than the village round it: nothing chips it
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public void beckon( int cell ){
	}

	@Override
	public boolean interact( Char c ){
		if (!(c instanceof Hero)) return true;
		Hero hero = (Hero) c;
		TravelWorld tw = TravelWorld.get();
		boolean woke = tw != null && tw.attune( key, wx, wy, place );
		if (woke) Travel.runeFlash( pos );
		if (hero.isRemote){
			//a guest's touch attunes it for the party; the travel itself is the host's to make
			NetDialogs.handleNetHero( hero, (woke ? Messages.get( this, "attuned", place ) + "\n\n" : "")
					+ Messages.get( this, "guest" ) );
			return true;
		}
		if (hero != Dungeon.hero) return true;
		if (woke){
			GLog.p( Messages.get( this, "attuned", place ) );
			xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_STONES );
		}
		final Travel.Place from = Travel.Place.of( this );
		Game.runOnRenderThread( () -> GameScene.show( new WndTravel( from ) ) );
		return true;
	}

	private static final String KEY = "key", WX = "wx", WY = "wy", PLACE = "place";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( KEY, key );
		bundle.put( WX, wx );
		bundle.put( WY, wy );
		bundle.put( PLACE, place == null ? "" : place );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		key = bundle.getLong( KEY );
		wx = bundle.getInt( WX );
		wy = bundle.getInt( WY );
		place = bundle.getString( PLACE );
	}
}
