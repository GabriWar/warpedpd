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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.plants.Blindweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Dreamfoil;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sorrowmoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.sprites.SpiderExplodingSprite;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

//ported from Remixed PD's Spider Nest: a kamikaze spider that ruptures on contact,
//releasing one plant's effect at random. Remixed's plant list maps onto Warped's:
//Dreamweed->Dreamfoil, Moongrace->Blindweed, the rest by name.
public class SpiderExploding extends Mob {

	private static final Class<?>[] PLANTS = {
			Firebloom.class, Icecap.class, Sorrowmoss.class, Dreamfoil.class,
			Sungrass.class, Earthroot.class, Fadeleaf.class, Blindweed.class
	};

	{
		spriteClass = SpiderExplodingSprite.class;

		HP = HT = 5;
		defenseSkill = 1;

		baseSpeed = 2f;

		EXP = 3;
		maxLvl = 9;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 3, 6 );
	}

	@Override
	public int attackSkill( Char target ) {
		//it almost never misses; the point is the rupture, not the bite
		return 125;
	}

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		if (super.attack( enemy, dmgMulti, dmgBonus, accMulti )) {
			Plant plant = (Plant) Reflection.newInstance( PLANTS[ Random.Int( PLANTS.length ) ] );
			plant.pos = enemy.pos;
			plant.activate( enemy );
			die( this );
			return true;
		}
		return false;
	}
}
