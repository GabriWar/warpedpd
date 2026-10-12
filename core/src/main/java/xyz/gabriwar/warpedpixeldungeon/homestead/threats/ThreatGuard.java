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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.SleepGuards;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import java.util.List;

/**
 * The alarm bell as a guard over a sleeper (homestead/SleepGuards, loaded by this name): a sleeper
 * found on the ground of a home with a bell is woken by its ringing, which tells him so itself,
 * and whoever found him is met at the edge of the home's ground (AlarmBell.wakeSleeper) instead of
 * beside the bed.
 */
public class ThreatGuard implements SleepGuards.Guard {

	@Override
	public SleepGuards.Roused rouse( Level level, int bed, Hero hero, List<Mob> found ){
		return AlarmBell.wakeSleeper( level, hero, found ) ? SleepGuards.Roused.placed( null ) : null;
	}
}
