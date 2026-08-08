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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTerrainTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollingListPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndTileEdit extends Window {

	private static final int WIDTH = 120;
	private static final int TITLE_H = 14;
	private static final int MAX_H = 120;

	public interface Listener {
		void onSelect(int terrain);
	}

	public WndTileEdit(int cell, String title, int[] terrains, String[] labels, Listener listener) {
		super();

		int listH = Math.min(MAX_H, terrains.length * 18 + 2);
		resize(WIDTH, TITLE_H + listH);

		RenderedTextBlock titleBlock = PixelScene.renderTextBlock(title, 9);
		titleBlock.hardlight(TITLE_COLOR);
		titleBlock.setPos((WIDTH - titleBlock.width()) / 2f, 2);
		add(titleBlock);

		ScrollingListPane list = new ScrollingListPane();
		add(list);
		list.setRect(0, TITLE_H, WIDTH, listH);

		for (int i = 0; i < terrains.length; i++) {
			final int terrain = terrains[i];
			list.addItem(new ScrollingListPane.ListItem(
					DungeonTerrainTilemap.tile(cell, terrain), null, labels[i]
			) {
				@Override
				public boolean onClick(float x, float y) {
					if (inside(x, y)) {
						hide();
						listener.onSelect(terrain);
						return true;
					}
					return false;
				}
			});
		}
	}
}
