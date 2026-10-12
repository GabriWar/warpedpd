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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

public class Compass extends Image {

	private static final float RAD_2_G	= 180f / 3.1415926f;
	private static final float RADIUS	= 12;
	
	private int cell;
	private PointF cellCenter;
	
	private PointF lastScroll = new PointF();
	
	public Compass( int cell ) {
		
		super();
		copy( Icons.COMPASS.get() );
		origin.set( width / 2, RADIUS );
		
		this.cell = cell;
		cellCenter = DungeonTilemap.tileCenterToWorld( cell );
		visible = false;
	}

	/** An arrow for one of the floor's ways out, in the colour of where it leads (colorOf). */
	public Compass( int cell, int rgb ) {
		this( cell );
		color( rgb );
	}

	/** Each kind of stairway its own colour: down gold, up sky blue, a branch's way in violet,
	 *  a branch's way back up green, the way out to the surface white. */
	public static int colorOf( LevelTransition.Type type ) {
		switch (type) {
			case REGULAR_EXIT:     return 0xFFD040;
			case REGULAR_ENTRANCE: return 0x66CCFF;
			case BRANCH_EXIT:      return 0xD070FF;
			case BRANCH_ENTRANCE:  return 0x70E070;
			case SURFACE: default: return 0xFFFFFF;
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (cell < 0 || cell >= Dungeon.level.length()){
			visible = false;
			return;
		}
		
		visible = Dungeon.level.visited[cell] || Dungeon.level.mapped[cell];
		
		if (visible) {
			PointF scroll = Camera.main.scroll;
			if (!scroll.equals( lastScroll )) {
				lastScroll.set( scroll );
				PointF center = Camera.main.center().offset( scroll );
				angle = (float)Math.atan2( cellCenter.x - center.x, center.y - cellCenter.y ) * RAD_2_G;
			}
		}
	}
}
