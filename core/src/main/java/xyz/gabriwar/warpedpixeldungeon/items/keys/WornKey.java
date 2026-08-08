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

package xyz.gabriwar.warpedpixeldungeon.items.keys;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.windows.WndSupportPrompt;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

import java.io.IOException;

public class WornKey extends Key {
	
	{
		image = ItemSpriteSheet.WORN_KEY;
	}
	
	public WornKey() {
		this( 0 );
	}
	
	public WornKey( int depth ) {
		super();
		this.depth = depth;
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		if(!WPDSettings.supportNagged()){
			try {
				Dungeon.saveAll();
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						WarpedPixelDungeon.scene().add(new WndSupportPrompt());
					}
				});
			} catch (IOException e) {
				WarpedPixelDungeon.reportException(e);
			}
			
		}
		
		return super.doPickUp(hero, pos);
	}

}
