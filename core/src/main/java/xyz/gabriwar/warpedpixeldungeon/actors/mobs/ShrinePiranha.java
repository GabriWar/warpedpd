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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The pale thing in a lake shrine's pool (levels/overworld/CaveSites): an albino piranha grown
 * with its slice - a step per slice down, k = -altitude - instead of with Dungeon.depth, which
 * on the caves' slices is a hundred and more.
 */
public class ShrinePiranha extends AlbinoPiranha {

	public ShrinePiranha(){
		super();
		int k = k();
		HP = HT = 30 + 5 * k;
		defenseSkill = 10 + 2 * k;
	}

	//how deep the slice is (1 at -1), or 1 off the slices
	private static int k(){
		int a = WorldLayers.altitudeOf( Dungeon.depth );
		return a != Integer.MIN_VALUE && a < 0 ? -a : 1;
	}

	@Override
	public int damageRoll(){
		int k = k();
		return Random.NormalIntRange( 4 + k, 10 + 2 * k );
	}

	@Override
	public int attackSkill( Char target ){
		return 20 + 2 * k();
	}

	@Override
	public int drRoll(){
		return Random.NormalIntRange( 0, k() );
	}

	//the albino's restore sets its evasion from the floor's depth, which on a slice is a hundred
	//and more: the shrine's fish keeps its slice's
	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		defenseSkill = 10 + 2 * k();
	}
}
