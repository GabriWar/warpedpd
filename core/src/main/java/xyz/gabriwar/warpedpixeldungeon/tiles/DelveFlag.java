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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.noosa.Tilemap;

/**
 * The flag planted beside a cleared barrow's stairs (levels/Delves): a 1x1 picture on the
 * overworld, laid by OverworldLevel over every cleared barrow its window holds. Never bundled -
 * the registry of cleared barrows is the truth, the flags are re-laid from it.
 */
public class DelveFlag extends CustomTilemap {

	{
		texture = "environment/custom_tiles/delve_flag.png";
		tileW = tileH = 1;
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map( new int[]{ 0 }, 1 );
		return v;
	}

	/** Takes the flag off the screen (its barrow left the window). */
	public void remove() {
		if (vis != null){
			if (vis.alive) vis.killAndErase();
			vis.destroy();
			vis = null;
		}
	}

	@Override
	public String name( int tileX, int tileY ) {
		return Messages.get( DelveFlag.class, "name" );
	}

	@Override
	public String desc( int tileX, int tileY ) {
		return Messages.get( DelveFlag.class, "desc" );
	}
}
