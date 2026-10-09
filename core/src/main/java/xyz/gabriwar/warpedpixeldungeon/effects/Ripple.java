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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * A ring on the water (WaterFX): the painted ring frames spreading on the water's surface,
 * mirrored so their far crest catches the sky, in the level's liquid's foam (added, on lava),
 * masked by the shore's tiles. Its y is still a caller's to nudge after it is made (the sewers'
 * pipes ring a cell up). Pooled by the game's scene; WaterFX.ring is the way to make one.
 */
public class Ripple extends FxRing {

	public Ripple(){
		super();
	}

	/** A middling ring (WaterFX.M) on the middle of a cell. */
	public void reset( int p ){
		int w = Dungeon.level.width();
		reset( (p % w + 0.5f) * DungeonTilemap.SIZE, (p / w + 0.5f) * DungeonTilemap.SIZE, WaterFX.M );
	}

	/** A ring of a size (WaterFX.S, M or L) on a point, the liquid's own. */
	public void reset( float x, float y, int size ){
		Element.Liquid l = WaterFX.liquid();
		reset( x, y, size, l.foam, WaterFX.alphaOf( size ), l.ringAdditive );
	}

	/** A ring of a size on a point in a colour and alpha of its own, as light or as matter. */
	public void reset( float x, float y, int size, int color, float alpha, boolean light ){
		int s = WaterFX.sizeOf( size );
		setup( x, y, WaterFX.FROM[s], WaterFX.TO[s], WaterFX.LIFE[s], color, alpha, light );
		mirrorY();
	}

	/** The second ring after a middling or large one: a size of ring smaller, a little later. */
	public void second( Ripple first, int size, int color, float alpha, boolean light ){
		int s = WaterFX.sizeOf( size );
		setup( first.centerX(), first.centerY(), WaterFX.SECOND_FROM[s], WaterFX.SECOND_TO[s], WaterFX.LIFE[s], color, alpha, light );
		mirrorY();
		delay( WaterFX.SECOND_AFTER );
		follow( first );
	}
}
