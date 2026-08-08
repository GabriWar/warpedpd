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

import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DragonVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class OverworldDragon extends AdultDragonViolet {

	public int tint = Random.Int( 6 );
	//the lair sector that spawned this dragon; slaying it retires the lair
	public long homeSector = Long.MIN_VALUE;

	@Override
	public void die( Object cause ){
		super.die( cause );
		if (homeSector != Long.MIN_VALUE
				&& xyz.gabriwar.warpedpixeldungeon.Dungeon.level
						instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)
					xyz.gabriwar.warpedpixeldungeon.Dungeon.level).markSiteCleared( homeSector );
		}
	}

	@Override
	public CharSprite sprite() {
		return new DragonVariantSprite( tint );
	}

	private static final String TINT = "tint";
	private static final String HOME = "home_sector";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TINT, tint );
		bundle.put( HOME, homeSector );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		tint = bundle.getInt( TINT );
		homeSector = bundle.contains( HOME ) ? bundle.getLong( HOME ) : Long.MIN_VALUE;
	}
}
