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
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.sprites.BunnyVariantSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** A meadow bunny: hops about, bothers no one, drops dinner. */
public class Bunny extends Mob {

	{
		spriteClass = BunnyVariantSprite.class;

		HP = HT = 8;
		defenseSkill = 12;

		maxLvl = -1;   //no xp: it is fauna, not a foe

		alignment = Alignment.NEUTRAL;
		state = WANDERING;

	}

	public int tint = Random.Int( 6 );

	@Override
	public CharSprite sprite() {
		return new BunnyVariantSprite( tint );
	}

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		//fauna always yields dinner - rollToDropLoot is gated by hero level,
		//which a high-level hero fails on purpose for real enemies
		if (com.watabou.utils.Random.Int( 2 ) == 0){
			xyz.gabriwar.warpedpixeldungeon.Dungeon.level.drop( new MysteryMeat(), pos ).sprite.drop();
		}
	}

	@Override
	public int attackSkill( Char target ) {
		return 0;
	}

	private static final String TINT = "tint";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TINT, tint );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		tint = bundle.getInt( TINT );
	}
}
