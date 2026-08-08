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
import xyz.gabriwar.warpedpixeldungeon.sprites.GnollVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** A gnoll of the peaceful overworld clans. */
public class GnollVillager extends NPC {

	{
		spriteClass = GnollVariantSprite.class;
	}

	public int tint = 0;
	public long homeSector = Long.MIN_VALUE;

	public static GnollVillager random( long homeSector ){
		GnollVillager v = new GnollVillager();
		v.tint = Random.Int( 8 );
		v.homeSector = homeSector;
		return v;
	}

	@Override
	public CharSprite sprite() {
		return new GnollVariantSprite( tint );
	}

	@Override
	protected boolean act() {
		throwItems();
		//cheap ambling: one free adjacent step now and then, zero pathfinding.
		//a metropolis can hold dozens of these - the default wandering AI runs
		//a pathfind per mob per turn and was a real source of turn lag
		//settlers keep to their beds at night
		if (sprite != null && !Shopkeeper.closedForNight() && Random.Int( 3 ) == 0){
			int step = pos + com.watabou.utils.PathFinder.NEIGHBOURS8[Random.Int( 8 )];
			if (step >= 0 && step < Dungeon.level.length()
					&& Dungeon.level.passable[step]
					&& !Dungeon.level.avoid[step]
					//tall grass tramples roll loot for ANY walker - a town of
					//amblers would slowly carpet itself in seeds
					&& Dungeon.level.map[step] != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.HIGH_GRASS
					&& xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( step ) == null){
				moveSprite( pos, step );
				move( step );
			}
		}
		spend( TICK );
		return true;
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
		if (Shopkeeper.closedForNight()){
			yell( Messages.get( this, "asleep" ) );
			return true;
		}
		yell( Messages.get( this, "line_" + Random.Int( 4 ) ) );
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
