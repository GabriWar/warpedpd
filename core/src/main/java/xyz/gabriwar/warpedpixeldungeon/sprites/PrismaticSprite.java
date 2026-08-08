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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PrismaticImage;
import com.watabou.noosa.Game;

public class PrismaticSprite extends MirrorSprite {

	public PrismaticSprite(){
		super();

		float interval = (Game.timeTotal % 9 ) /3f;
		tint(interval > 2 ? interval - 2 : Math.max(0, 1 - interval),
				interval > 1 ? Math.max(0, 2-interval): interval,
				interval > 2 ? Math.max(0, 3-interval): interval-1, 0.5f);
	}

	@Override
	public void updateArmor() {
		//same trap MirrorSprite had: anything that borrows this sprite without
		//being a PrismaticImage would crash on link, so mirror the hero instead
		int tier = 0;
		if (ch instanceof PrismaticImage){
			tier = ((PrismaticImage)ch).armTier;
		} else if (Dungeon.hero != null){
			tier = Dungeon.hero.tier();
		}
		updateArmor( tier );
	}
	
	@Override
	public void update() {
		super.update();
		
		if (flashTime <= 0){
			float interval = (Game.timeTotal % 9 ) /3f;
			tint(interval > 2 ? interval - 2 : Math.max(0, 1 - interval),
					interval > 1 ? Math.max(0, 2-interval): interval,
					interval > 2 ? Math.max(0, 3-interval): interval-1, 0.5f);
		}
	}
	
}
