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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuardVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** A watchful settlement guard. All bark, no bite - for now. */
public class OverworldGuard extends NPC {

	{
		spriteClass = GuardVariantSprite.class;
		properties.add( Property.IMMOVABLE );
	}

	public int tint = 0;
	public long homeSector = Long.MIN_VALUE;

	public static OverworldGuard random( long homeSector ){
		OverworldGuard g = new OverworldGuard();
		g.tint = Random.Int( 6 );
		g.homeSector = homeSector;
		return g;
	}

	@Override
	public CharSprite sprite() {
		return new GuardVariantSprite( tint );
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	protected Char chooseEnemy() {
		return null;
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
		sprite.turnTo( pos, c.pos );
		if (c != Dungeon.hero) return true;
		yell( Messages.get( this, "line_" + Random.Int( 3 ) ) );
		return true;
	}

	private static final String TINT = "tint";
	private static final String HOME = "home_sector";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TINT, tint );
		bundle.put( HOME, homeSector );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		tint = bundle.getInt( TINT );
		homeSector = bundle.getLong( HOME );
	}
}
