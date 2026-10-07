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

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

public class TargetedCell extends Image implements Bundlable {

	public int pos;
	public float time;

	private float alpha;

	public static SparseArray<TargetedCell> cells = new SparseArray<>();

	public void reset( int pos, float time ){
		copy(Icons.get(Icons.TARGET));
		origin.set( width/2f );
		camera = null;

		this.pos = pos;
		point( DungeonTilemap.tileToWorld( pos ) );

		hardlight(0xFF0000);

		alpha = 1f;
		this.time = time;

		alpha(1f);
		scale.set(1f);

		synchronized (cells) {
			cells.put(pos, this);
		}
	}

	@Override
	public void update() {
		alpha -= Game.elapsed;
		if (time >= Actor.now()){
			alpha = Math.max(alpha, 0.6f);
		}
		if (alpha > 0) {
			alpha( alpha );
			scale.set( (float)Math.pow(alpha, 0.33f) );
		} else {
			time = 0;
			synchronized (cells) {
				cells.remove(pos);
			}
			killAndErase();
		}
	}

	/** A sliding-window level moved (OverworldLevel.rebase, render thread): every mark follows
	 *  its cell, and the ones that left the window go. */
	public static void shiftAll( int dx, int dy, int width, int height ){
		synchronized (cells){
			java.util.List<TargetedCell> all = cells.valueList();
			cells.clear();
			for (TargetedCell c : all){
				int x = c.pos % width - dx, y = c.pos / width - dy;
				if (x <= 0 || y <= 0 || x >= width - 1 || y >= height - 1){
					c.killAndErase();
					continue;
				}
				c.pos = x + y * width;
				c.point( DungeonTilemap.tileToWorld( c.pos ) );
				cells.put( c.pos, c );
			}
		}
	}

	public static void fixTime(float min){
		synchronized (cells){
			for (TargetedCell c : cells.valueList()){
				c.time -= min;
			}
		}
	}

	private static final String POS = "pos";
	private static final String COLOR = "color";
	private static final String TIME = "time";

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(POS, pos);
		bundle.put(TIME, time);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		pos = bundle.getInt(POS);
		time = bundle.getInt(TIME);
	}

}
