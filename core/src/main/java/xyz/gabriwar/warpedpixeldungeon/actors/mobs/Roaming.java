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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import com.watabou.utils.Bundle;

/**
 * The mark of a roaming beast (levels/overworld/RoamerEvent) on its body: the event it is, its
 * name among its kind's (RoamerEvent.title) and the turn its days abroad end. The beast's own
 * class (GreatWolf, ElderYeti, GiantScorpio) reads it for its name and its haunt, and its death
 * for the reward; it keeps it out of the parked store (the event puts it back where it should be).
 */
public class Roaming extends Buff {

	{
		type = buffType.NEUTRAL;
	}

	public long event = Long.MIN_VALUE;
	public int name = 0;
	public int endTurn = 0;

	private static final String EVENT    = "event";
	private static final String NAME     = "name";
	private static final String END_TURN = "end_turn";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( EVENT, event );
		bundle.put( NAME, name );
		bundle.put( END_TURN, endTurn );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		event   = bundle.contains( EVENT )    ? bundle.getLong( EVENT )  : Long.MIN_VALUE;
		name    = bundle.contains( NAME )     ? bundle.getInt( NAME )    : 0;
		endTurn = bundle.contains( END_TURN ) ? bundle.getInt( END_TURN ) : 0;
	}
}
