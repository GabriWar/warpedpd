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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.sprites.SokobanSheepSwitchSprite;
import com.watabou.utils.Random;

public class SheepSokobanSwitch extends NPC {

	private static final String[] QUOTES = { "Baa!", "Baa?", "Baa.", "Baa..." };

	{
		spriteClass = SokobanSheepSwitchSprite.class;
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

		yell( Random.element( QUOTES ) );
		moveSprite( pos, c.pos );
		move( c.pos );

		c.sprite.move( c.pos, curPos );
		c.move( curPos );

		Dungeon.hero.spend( 1 / Dungeon.hero.speed() );
		Dungeon.hero.busy();

		return true;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
