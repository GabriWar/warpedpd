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

import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

/**
 * DEV TOOL: a 1x1 signpost custom tile used by DevRoomsLevel. Renders a small
 * wooden sign; tapping the cell shows the label as the tile's name.
 */
public class DevSign extends CustomTilemap {

	{
		texture = "environment/custom_tiles/dev_sign.png";
		tileW = tileH = 1;
	}

	public String label = "";

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map( new int[]{ 0 }, 1 );
		return v;
	}

	@Override
	public String name(int tileX, int tileY) {
		return label;
	}

	@Override
	public String desc(int tileX, int tileY) {
		return "A dev marker naming the room beside it:\n\n_" + label + "_";
	}

	private static final String LABEL = "label";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(LABEL, label);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		label = bundle.getString(LABEL);
	}
}
