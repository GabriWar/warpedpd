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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import com.watabou.noosa.Game;

/**
 * A beast of the surface's plains, desert, swamps and rivers (tools/ow_fauna_gen.py: the bison, the
 * camel, the vulture, the alligator, the pike). It takes the look its ground gives it (idle) as it
 * is set down, and its run plays while it moves and, a moment after it stops, it settles back into
 * that look: the alligator lurks in the water and basks on land, the vulture tears at carrion it
 * stands on. The look is read from what every machine sees - the cell the beast stands on, the heap
 * there - so a co-op guest's copy, built from the sprite's class alone, settles into the same one. A
 * beast moving every turn never settles between its steps: the next one comes long before the
 * moment is up.
 */
public abstract class WildSprite extends MobSprite {

	//how long it stands still before it settles
	private static final float SETTLE = 0.3f;
	private float still;

	//set down (a spawn, a load, a window coming back, a guest's copy): the look of its ground at once,
	//not the constructor's (an alligator in its pool shows only its eyes from the first frame)
	@Override
	public void link( Char ch ){
		super.link( ch );
		idle();
	}

	@Override
	public void update(){
		super.update();
		if (ch == null || isMoving || curAnim == null || curAnim != run){
			still = 0;
			return;
		}
		still += Game.elapsed;
		if (still >= SETTLE){
			still = 0;
			idle();
		}
	}

	/** Whether the cell it stands on is open water (the level that is up now; false off it). */
	protected boolean inWater(){
		Level level = Dungeon.level;
		return ch != null && level != null && level.water != null && ch.pos >= 0 && ch.pos < level.length()
				&& level.water[ch.pos];
	}
}
