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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

import java.util.Locale;

/**
 * A great beast's trophy (levels/overworld/RoamerEvent): the pelt of a great wolf, the fang of an
 * elder yeti or the sting of a giant scorpion, taken from the named beast itself, which it keeps
 * the name of. Proof of the kill and worth a good price to any merchant. Each is one of a kind:
 * never stacked, so the beast it came from is never lost.
 */
public class RoamerTrophy extends Item {

	{
		image = ItemSpriteSheet.TROPHY_PELT;
		stackable = false;
	}

	//the beast's kind (RoamerEvent.Kind), its name among its kind's, and the roaming beast it was
	//taken from (WorldEvents id)
	public int kind = 0;
	public int name = 0;
	public long event = 0;

	public static RoamerTrophy of( RoamerEvent.Kind kind, int name, long event ){
		RoamerTrophy t = new RoamerTrophy();
		t.kind = kind.ordinal();
		t.name = name;
		t.event = event;
		t.image = kind.trophy;
		return t;
	}

	private RoamerEvent.Kind beast(){
		RoamerEvent.Kind[] all = RoamerEvent.Kind.values();
		return all[Math.floorMod( kind, all.length )];
	}

	@Override
	public int image(){
		return beast().trophy;
	}

	@Override
	public String name(){
		return Messages.get( this, "name_" + beast().name().toLowerCase( Locale.ENGLISH ), RoamerEvent.title( beast(), name ) );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc_" + beast().name().toLowerCase( Locale.ENGLISH ), RoamerEvent.title( beast(), name ) );
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	@Override
	public int value(){
		return 400 * quantity;
	}

	private static final String KIND  = "kind";
	private static final String NAME  = "beast_name";
	private static final String EVENT = "event";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( KIND, kind );
		bundle.put( NAME, name );
		bundle.put( EVENT, event );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		kind  = bundle.contains( KIND )  ? bundle.getInt( KIND )   : 0;
		name  = bundle.contains( NAME )  ? bundle.getInt( NAME )   : 0;
		event = bundle.contains( EVENT ) ? bundle.getLong( EVENT ) : 0;
		image = beast().trophy;
	}
}
