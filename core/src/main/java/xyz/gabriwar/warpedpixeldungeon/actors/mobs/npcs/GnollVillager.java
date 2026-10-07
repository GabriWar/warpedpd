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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GnollVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** A gnoll of the peaceful overworld clans. */
public class GnollVillager extends Settler {

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
	public long settlementKey(){
		return homeSector;
	}

	@Override
	protected boolean gnoll(){
		return true;
	}

	@Override
	protected int idleLines(){
		return 4;
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
