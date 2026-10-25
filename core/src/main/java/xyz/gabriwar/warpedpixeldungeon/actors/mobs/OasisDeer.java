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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;

/**
 * A deer come down to an oasis' water at dusk (levels/overworld/DrylandsLife): the meadows' deer in
 * every way - shy, quick, its hide and its meat for whoever brings it down - but the oasis' own
 * guest, not the land's wildlife: the hour's cull leaves it be (it is no DIURNAL kind), and it goes
 * back into the desert of itself once the dusk is over, out of everyone's sight. One the hero has
 * roped or penned (homestead/animals) is his own and stays, as the cull spares a kept animal.
 */
public class OasisDeer extends Deer {

	//how near a hero may be when it slips away: never under his nose
	private static final int AWAY = 8;

	@Override
	protected boolean act(){
		if (!homesteadKept() && DayNightCycle.phase() != DayNightCycle.Phase.DUSK
				&& OverworldLevel.heroDistance( Dungeon.level, pos ) >= AWAY && !NetManager.anyHeroSees( pos )){
			spend( TICK );
			OverworldLevel.vanish( Dungeon.level, this );
			return true;
		}
		return super.act();
	}
}
