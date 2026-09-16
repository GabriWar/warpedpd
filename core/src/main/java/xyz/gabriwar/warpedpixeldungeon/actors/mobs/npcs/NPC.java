/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownCommute;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInnLevel;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SnowedIn;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public abstract class NPC extends Mob {

	{
		HP = HT = 1;
		EXP = 0;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		//the climate never freezes, cooks, soaks or buries NPCs - cold/heat tick
		//damage reads as an attack (fleeing shopkeeper, frozen way-portal, etc)
		immunities.add( Hypothermia.class );
		immunities.add( Heatstroke.class );
		immunities.add( SnowedIn.class );
		immunities.add( Chill.class );
		immunities.add( Frost.class );
		immunities.add( Drenched.class );
	}

	//the town's people sleep at the inn (TownCommute); everyone else says no
	public boolean sleepsAtInn() { return false; }

	//where a sleeper stands by day, and which bed at the inn is theirs at night;
	//-1 until TownCommute hands them one. Slot keys, not cells: see TownCommute.cellOf
	public int home = -1;
	public int bed = -1;

	//how long after nightfall this one heads for bed, in turns: the pious turn in
	//first, the drunk stumble in last, so the street empties over the evening
	//rather than all at once. A little personal lateness on top, rolled once
	protected int bedtime() { return 0; }
	private int lateness = Random.Int( 40 );

	public boolean bedtimeNow() {
		return DayNightCycle.isNight() && DayNightCycle.turnsIntoPhase() >= bedtime() + lateness;
	}

	@Override
	protected boolean act() {
		if (Dungeon.level.heroFOV[pos]){
			Bestiary.setSeen(getClass());
		}

		if (sleepsAtInn()) {
			//the walk needs a field of view for pathing round people; Char.act would
			//build it, but the commute decides before the ordinary turn runs
			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
				fieldOfView = new boolean[Dungeon.level.length()];
			}
			Dungeon.level.updateFieldOfView( this, fieldOfView );
			if (commute()) return true;
		}

		return super.act();
	}

	//the daily round: at night, to bed if this is the inn and out of the door if it
	//is not; by day, out of the inn if home is elsewhere, else to the home spot.
	//Returns true when the turn was spent on it
	private boolean commute() {
		boolean night = DayNightCycle.isNight();
		boolean inn = Dungeon.level instanceof TownInnLevel;
		int dest;
		if (night) {
			if (!bedtimeNow()) return false;   //still up
			if (inn) {
				dest = TownCommute.cellOf( Dungeon.level, bed );
				if (dest == -1) return false;
				if (pos == dest) {
					state = SLEEPING;
					spend( TICK );
					return true;
				}
			} else {
				dest = TownCommute.door( Dungeon.level );
				if (dest == -1) return false;
				if (pos == dest) {
					leave();
					return true;
				}
			}
		} else {
			if (state == SLEEPING) state = PASSIVE;
			if (inn && home == -1) {
				dest = TownCommute.door( Dungeon.level );
				if (dest == -1) return false;
				if (pos == dest) {
					leave();
					return true;
				}
			} else {
				dest = TownCommute.cellOf( Dungeon.level, home );
				if (dest == -1 || pos == dest) return false;
			}
		}
		int oldPos = pos;
		if (getCloser( dest )) {
			spend( 1 / speed() );
			return moveSprite( oldPos, pos );
		}
		spend( TICK );
		return true;
	}

	//out of the door and gone: the level they are walking to will make a new one
	private void leave() {
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	private static final String HOME     = "home";
	private static final String BED      = "bed";
	private static final String LATENESS = "lateness";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		if (sleepsAtInn()) {
			bundle.put( HOME, home );
			bundle.put( BED, bed );
			bundle.put( LATENESS, lateness );
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		home = bundle.contains( HOME ) ? bundle.getInt( HOME ) : -1;
		bed  = bundle.contains( BED )  ? bundle.getInt( BED )  : -1;
		if (bundle.contains( LATENESS )) lateness = bundle.getInt( LATENESS );
	}

	@Override
	public void beckon( int cell ) {
	}
	
}