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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;

/**
 * The picture over a branch's stairway, so it never looks like the floor's own way down: the spider
 * nest's webbed stairs in the prison, the temple's marble and gold in the caves, the frozen halls'
 * ice in the halls. One tile of environment/custom_tiles/branch_stairs.png each, laid on the
 * entrance cell by its room (as the mines' and the imp's vault's entrances already were).
 */
public abstract class BranchStairs extends CustomTilemap {

	{
		texture = Assets.Environment.BRANCH_STAIRS;
		tileW = tileH = 1;
	}

	/** This stairway's tile in the sheet. */
	protected abstract int frame();

	@Override
	public Tilemap create(){
		Tilemap v = super.create();
		v.map( new int[]{ frame() }, 1 );
		return v;
	}

	@Override
	public String name( int tileX, int tileY ){
		return Messages.get( this, "name" );
	}

	@Override
	public String desc( int tileX, int tileY ){
		return Messages.get( this, "desc" );
	}

	public static class Spider extends BranchStairs {
		@Override
		protected int frame(){ return 0; }
	}

	public static class Temple extends BranchStairs {
		@Override
		protected int frame(){ return 1; }
	}

	public static class Frozen extends BranchStairs {
		@Override
		protected int frame(){ return 2; }
	}
}
