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


package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.sprites.GreatWolfSprite;
import com.watabou.utils.Random;

/**
 * A great wolf with a name of its own, roaming a region for days (levels/overworld/RoamerEvent):
 * a grey wolf grown huge and old, four times as hardy, harder hitting, and its bite tears (one in
 * three draws blood for a while). Like any wolf it calls the pack when it wakes to a hero. Out of a
 * fight it keeps to its haunt of the hour; killed, it leaves its trophy, a purse and a good item.
 */
public class GreatWolf extends GrayWolf {

	{
		spriteClass = GreatWolfSprite.class;

		HP = HT = 440;
		defenseSkill = 32;

		EXP = 40;
		maxLvl = 36;

		state = WANDERING;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 26, 44 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 46;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 6 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (damage > 0 && Random.Int( 3 ) == 0) Buff.affect( enemy, Bleeding.class ).set( damage / 4f );
		return damage;
	}

	@Override
	public String name() {
		return RoamerEvent.title( this, super.name() );
	}

	//out of a fight it keeps to its haunt of the hour
	@Override
	protected boolean act() {
		if (state == WANDERING){
			int haunt = RoamerEvent.haunt( this );
			if (haunt != -1) target = haunt;
		}
		return super.act();
	}

	@Override
	public void die( Object cause ) {
		RoamerEvent.slain( this );
		super.die( cause );
	}
}
