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
import xyz.gabriwar.warpedpixeldungeon.sprites.VillagerSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A settlement dweller: pure ambience. Wanders the village, chats when
 * bumped, can't be hurt. Look and colours are rolled at spawn.
 */
public class Villager extends Settler {

	{
		spriteClass = VillagerSprite.class;
	}

	public int look = VillagerSprite.THIEF;
	public int tint = 0;
	//sector key of the settlement that spawned this villager
	public long homeSector = Long.MIN_VALUE;

	public static Villager random( long homeSector ){
		Villager v = new Villager();
		v.look = Random.Int( 3 );
		v.tint = Random.Int( 8 );
		v.homeSector = homeSector;
		return v;
	}

	@Override
	public CharSprite sprite() {
		return new VillagerSprite( look, tint );
	}

	@Override
	public long settlementKey(){
		return homeSector;
	}

	@Override
	protected boolean gnoll(){
		return false;
	}

	@Override
	protected int idleLines(){
		return 6;
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

	private static final String LOOK = "look";
	private static final String TINT = "tint";
	private static final String HOME = "home_sector";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LOOK, look );
		bundle.put( TINT, tint );
		bundle.put( HOME, homeSector );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		look = bundle.getInt( LOOK );
		tint = bundle.getInt( TINT );
		homeSector = bundle.getLong( HOME );
	}
}
