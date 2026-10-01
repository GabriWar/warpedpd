/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.sprites.AcidicSprite;
import com.watabou.utils.Random;

public class Acidic extends Scorpio {

	{
		spriteClass = AcidicSprite.class;

		properties.add(Property.ACIDIC);

		//built for the heat: the chill is what hurts
		thermal = Thermal.HEAT_DWELLER;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		Buff.affect( enemy, Ooze.class ).set( Ooze.DURATION );
		return super.attackProc( enemy, damage );
	}

	@Override
	public int defenseProc( Char enemy, int damage ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) {
			Buff.affect( enemy, Ooze.class ).set( Ooze.DURATION );
		}
		int dmg = Random.IntRange( 0, damage );
		if (dmg > 0) {
			enemy.damage( dmg, this );
		}
		return super.defenseProc( enemy, damage );
	}

}
