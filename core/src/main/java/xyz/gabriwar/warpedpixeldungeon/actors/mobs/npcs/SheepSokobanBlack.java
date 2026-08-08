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
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.SokobanBlackSheepSprite;
import com.watabou.utils.Random;

public class SheepSokobanBlack extends NPC {

	private static final String[] QUOTES = { "Baa!", "Baa?", "Baa.", "Baa..." };

	{
		spriteClass = SokobanBlackSheepSprite.class;
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

	private int countFleeceTraps( int start, int dist ) {
		int count = 0;
		for (int cell = 0; cell < Dungeon.level.length(); cell++) {
			if (Dungeon.level.avoid[cell]
					&& Actor.findChar( cell ) == null
					&& Dungeon.level.map[ cell ] == Terrain.FLEECING_TRAP
					&& Dungeon.level.distance( start, cell ) < dist) {
				count++;
			}
		}
		return count;
	}

	private int randomFleeceCell( int start, int dist ) {
		int cell;
		int count = 100;
		do {
			cell = Random.Int( Dungeon.level.length() );
			if (count-- <= 0) {
				return -1;
			}
		} while (!Dungeon.level.avoid[cell]
				|| Actor.findChar( cell ) != null
				|| Dungeon.level.map[ cell ] != Terrain.FLEECING_TRAP
				|| Dungeon.level.distance( start, cell ) > dist);
		return cell;
	}

	@Override
	public boolean interact( Char c ) {
		int traps = countFleeceTraps( pos, 5 );
		int newPos = -1;
		int curPos = pos;
		boolean moved = false;

		if (traps > 0) {
			newPos = randomFleeceCell( pos, 5 );
		}

		if (newPos == -1) {
			yell( Random.element( QUOTES ) );
			destroy();
			sprite.killAndErase();
			sprite.emitter().burst( ShadowParticle.UP, 5 );
			moved = true;
		} else {
			yell( "BAA!" );
			CellEmitter.get( pos ).start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );
			pos = newPos;
			move( pos );
			moved = true;
		}

		if (moved) {
			c.sprite.move( c.pos, curPos );
			c.move( curPos );
		}

		Dungeon.hero.spend( 1 / Dungeon.hero.speed() );
		Dungeon.hero.busy();

		return true;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
