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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

//a cell searched: a blue square in the cell shrinking in whole steps (16, 12, 8, 4 px) as it fades
//from START to nothing, the farther cells a little later
public class CheckedCell extends Image {

	public static final float START = 0.5f, FOR = 0.8f;

	private float alpha;
	private float delay;
	private float cx, cy;

	public CheckedCell() {
		super( TextureCache.createSolid( 0xFF55AAFF ) );
	}

	public void reset( int pos, int visSource ){
		revive();
		origin.set( 0 );

		cx = DungeonTilemap.tileToWorld( pos ).x + DungeonTilemap.SIZE / 2f;
		cy = DungeonTilemap.tileToWorld( pos ).y + DungeonTilemap.SIZE / 2f;

		alpha = START;
		size( START );

		delay = (Dungeon.level.trueDistance(pos, visSource)-1f);
		//delay steadily accelerates as distance increases
		if (delay > 0) {
			delay = (float)Math.pow(delay, 0.67f)/10f;
			alpha( 0 );
		}
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordCheckedCell(pos, visSource);
	}

	/** Its side in px with `a` of its alpha left: 16, 12, 8, then 4. */
	public static int side( float a ){
		float k = a / START;
		return k > 0.75f ? 16 : k > 0.5f ? 12 : k > 0.25f ? 8 : 4;
	}

	//its square, whole pixels, about the cell's middle
	private void size( float a ){
		int s = side( a );
		scale.set( s );
		x = Math.round( cx - s / 2f );
		y = Math.round( cy - s / 2f );
	}

	@Override
	public void update() {
		if ((delay -= Game.elapsed) > 0){
			alpha( 0 );
		} else if ((alpha -= Game.elapsed * START / FOR) > 0) {
			alpha( alpha );
			size( alpha );
		} else {
			kill();
		}
	}
}
