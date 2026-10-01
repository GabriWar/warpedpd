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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Standing in the wind of The Bellows (BellowsDraft, which lays this on the hero every
 * turn they are in it). It does nothing by itself: it is the room saying what it is. The
 * icon is there while the wind is, and its description is the one place the room's two
 * rules are written down - thrown things are carried by the wind, and so is anything
 * that hangs in the air.
 */
public class Draught extends FlavourBuff {

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	public static final float DURATION = 2f;

	/** which way the wind blows here: 0 east, 1 south, 2 west, 3 north */
	public int heading = 0;

	@Override
	public int icon(){
		return BuffIndicator.WINDSWEPT;
	}

	@Override
	public void tintIcon( Image icon ){
		icon.hardlight( 0.75f, 0.92f, 1f );
	}

	@Override
	public String toString(){
		return Messages.get( this, "name" );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", Messages.get( this, "dir_" + heading ) );
	}

	private static final String HEADING = "heading";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HEADING, heading );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		heading = bundle.getInt( HEADING );
	}
}
