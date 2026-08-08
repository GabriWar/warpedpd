/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.sprites.SokobanCornerSheepSprite;
import com.watabou.utils.Random;

public class SheepSokobanCorner extends NPC {

	private static final String[] QUOTES = { "Baa!", "Baa?", "Baa.", "Baa..." };

	{
		spriteClass = SokobanCornerSheepSprite.class;
	}

	@Override
	protected boolean act() {
		throwItems();
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean interact( Char c ) {
		int curPos = pos;
		int movPos = pos;
		int width = Dungeon.level.width();
		boolean moved = false;
		int posDif = c.pos - curPos;

		// Cardinal directions
		if (posDif == 1) {
			movPos = curPos - 1;
		} else if (posDif == -1) {
			movPos = curPos + 1;
		} else if (posDif == width) {
			movPos = curPos - width;
		} else if (posDif == -width) {
			movPos = curPos + width;
		}
		// Diagonal directions
		else if (posDif == -width + 1) {
			movPos = curPos + width - 1;
		} else if (posDif == -width - 1) {
			movPos = curPos + width + 1;
		} else if (posDif == width + 1) {
			movPos = curPos - (width + 1);
		} else if (posDif == width - 1) {
			movPos = curPos - (width - 1);
		}

		if (movPos != pos
				&& (Dungeon.level.passable[movPos] || Dungeon.level.avoid[movPos])
				&& Actor.findChar( movPos ) == null) {

			moveSprite( curPos, movPos );
			move( movPos );
			moved = true;
		}

		if (moved) {
			c.sprite.move( c.pos, curPos );
			c.move( curPos );
		}

		yell( Random.element( QUOTES ) );

		return true;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
